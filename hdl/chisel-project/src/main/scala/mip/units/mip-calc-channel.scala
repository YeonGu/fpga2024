package mip.units

import chisel3._
import chisel3.util._
import __global__.GeneratedParams.SCREEN_ADDR_WIDTH
import mip.MipConfigs._
// import xilinx.proc_queue
import rendering.IntensityProjectionCore
import __global__.Params.DENS_DEPTH
import __global__.Params.VOXEL_POS_XLEN
import mip.xilinx.result_cache_fifo
import __global__.Params._
import os.proc
import __global__.ScreenPos
import mip.xilinx.proc_queue_fifo
import rendering.RenderCore
import rendering.CoreParams.CORENUMS
import __global__.Mat3x4
import os.stat
import rendering.MipOutputData
import _root_.circt.stage.ChiselStage

class calcInput extends Bundle {
    val need_data = Output(Bool())

    // val x_reg_wrdata    = Input(UInt(log2Ceil(SCREEN_H).W))
    // val y_reg_wrdata    = Input(UInt(log2Ceil(SCREEN_V).W))
    // val screen_pos_wren = Input(Bool())
    val voxel_addr_reg_wren   = Input(Bool())
    val voxel_addr_reg_wrdata = Input(UInt(SCREEN_ADDR_WIDTH.W))

    val proc_queue_wren   = Input(Bool())
    val proc_queue_wrdata = Input(UInt(PROC_QUEUE_WR_WIDTH.W))
}
class calcResult extends Bundle {
    val data_valid = Output(Bool())
    val rden       = Input(Bool())
    // val screen_pos = Output(UInt(SCREEN_ADDR_WIDTH.W))
    val screen_pos = Output(new ScreenPos())
    val density    = Output(UInt(DENS_DEPTH.W))
}

/** MIP calculation channel.
  *
  * [Data Fetcher]-----------> densityQueue =======> mipCore =======> resultCache -----------> addrReg proc *
  * 4
  */
class MipCalcChannel extends Module {
    val in  = IO(new calcInput())
    val out = IO(new calcResult())
    val mipCtrl = IO(new Bundle {
        val mvpInfo   = Input(new Mat3x4())
        val baseCoord = Input(Vec(3, SInt(BASE_POS_XLEN.W)))
    })

    // PROCESS QUEUE & VOXEL ADDR REG
    // From data fetcher, store density data.
    // write: from data fetcher. 128bit width (TODO:)
    // read:  to mip core. [N_CORES * 8bit] width
    // read:  disabled when result_queue is almost full.
    val proc_queue = Module(new proc_queue_fifo())
    val voxel_addr = RegInit(0.U(VOXEL_POS_XLEN.W))

    proc_queue.io.clk  := clock
    proc_queue.io.srst := reset

    // RENDER CORE
    val render_core = Module(new RenderCore(128, 64))

    // RESULT QUEUE
    val result_queue = Module(new result_cache_fifo())

    result_queue.io.clk  := clock
    result_queue.io.srst := reset

    // FSM
    object states extends ChiselEnum { val IDLE, LOAD, CALC = Value }
    val channel_state = RegInit(states.IDLE)
    // val IDLE::FETCH::CALC

    // Process addrReg and procCounter in this FSM logic
    // In order to avoid conflicts, calculation starts when a whole workset
    // is loaded into the proc_queue.
    switch(channel_state) {
        is(states.IDLE) {
            // proc_count := 0.U
            voxel_addr := Mux(in.voxel_addr_reg_wren, in.voxel_addr_reg_wrdata, voxel_addr)

            when(~proc_queue.io.empty) { channel_state := states.LOAD }
        }
        is(states.LOAD) {
            when(proc_queue.io.rd_data_count >= WORKSET_RD_CNT.U) { channel_state := states.CALC }
        }
        is(states.CALC) {
            // increse 1 if remains data in proc_queue
            // it is correct to use rd_data_count > 1 as a valid signal. I dont fucking know why.
            // proc_count := Mux(proc_queue.io.rd_en, proc_count + 1.U, proc_count)
            voxel_addr := Mux(proc_queue.io.valid, voxel_addr + CORENUMS.U, voxel_addr)

            channel_state := Mux(proc_queue.io.empty, states.IDLE, states.CALC)
            // when(proc_count === (WORKSET_RD_CNT - 1).U) { channel_state := states.IDLE }
        }
    }

    // dispatch unit -> PROC QUEUE
    in.need_data        := (channel_state === states.IDLE)
    proc_queue.io.wr_en := in.proc_queue_wren
    proc_queue.io.din   := in.proc_queue_wrdata

    /* proc_queue ==> calc core */
    val toomuch_res = result_queue.io.wr_data_count >= (RES_CACHE_WR_DEPTH / 4 * 3).U
    proc_queue.io.rd_en := (channel_state === states.CALC) && (!toomuch_res)

    render_core.io.in.valid     := proc_queue.io.valid
    render_core.io.in.density   := proc_queue.io.dout.asTypeOf(Vec(CORENUMS, UInt(DENS_DEPTH.W)))
    render_core.io.in.voxelPos  := voxel_addr
    render_core.io.in.mvpInfo   := mipCtrl.mvpInfo
    render_core.io.in.baseCoord := mipCtrl.baseCoord
    // TODO: check density assignment in compiled verilog
    // render_core.io.in.density.zipWithIndex.foreach { case (d, idx) =>
    //     d := proc_queue.io.rd_data(8 * (idx + 1) - 1, 8 * idx)
    // }

    // RENDER CORE -> RES QUEUE

    /* mip_core ==> result_queue */
    val render_result = render_core.io.out // valid and packed results
    result_queue.io.wr_en := render_result.valid
    result_queue.io.din   := render_result.packedResult

    // Result queue => calc result (-> VRAM)
    val res_fw_dec = result_queue.io.rd_data.asTypeOf(new MipOutputData())

    result_queue.io.rd_en := out.rden || ~(res_fw_dec.valid)
    out.data_valid        := (!result_queue.io.empty) && res_fw_dec.valid
    out.density           := res_fw_dec.density
    out.screen_pos        := res_fw_dec.screenPos
}

class ResultEncode extends Module {
    val io = IO(new Bundle {
        val screen_pos = Input(new ScreenPos())
        val density    = Input(UInt(DENS_DEPTH.W))
        val result     = Output(UInt(32.W))
    })
    io.result := Cat(io.screen_pos.x, io.screen_pos.y, io.density)
}

class ResultDecode extends Module {
    val io = IO(new Bundle {
        val code       = Input(UInt(32.W))
        val screen_pos = Output(new ScreenPos())
        val density    = Output(UInt(DENS_DEPTH.W))
    })

    io.screen_pos.x := io.code(
        DENS_DEPTH + log2Ceil(SCREEN_V) + log2Ceil(SCREEN_H) - 1,
        DENS_DEPTH + log2Ceil(SCREEN_V)
    )
    io.screen_pos.y := io.code(DENS_DEPTH + log2Ceil(SCREEN_V) - 1, DENS_DEPTH)
    io.density      := io.code(DENS_DEPTH, 0)
}

object CalcChannel extends App {
    ChiselStage.emitSystemVerilogFile(
        new MipCalcChannel(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
