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

class calcChannelIn extends Bundle {
    val need_data = Output(Bool())

    // val x_reg_wrdata    = Input(UInt(log2Ceil(SCREEN_H).W))
    // val y_reg_wrdata    = Input(UInt(log2Ceil(SCREEN_V).W))
    // val screen_pos_wren = Input(Bool())
    val voxel_addr_reg_wren   = Input(Bool())
    val voxel_addr_reg_wrdata = Input(UInt(SCREEN_ADDR_WIDTH.W))

    val proc_queue_wren   = Input(Bool())
    val proc_queue_wrdata = Input(UInt(PROC_QUEUE_WR_WIDTH.W))
}
class calcChannelOut extends Bundle {
    val data_valid = Output(Bool())
    val rden       = Input(Bool())
    // val screen_pos = Output(UInt(SCREEN_ADDR_WIDTH.W))
    val screen_pos = Output(new ScreenPos())
    val density    = Output(UInt(DENS_DEPTH.W))
}

/** MIP calculation channel.
  *
  * [Data Fetcher]-----------> densityQueue =======> mipCore =======> resultCache ----------->
  * addrReg proc * 4
  */
class MipCalcChannel extends Module {
    val in  = IO(new calcChannelIn())
    val out = IO(new calcChannelOut())

    /* Modules */

    // PROCESS QUEUE
    // From data fetcher, store density data.
    // write: from data fetcher. 128bit width (TODO:)
    // read:  to mip core. [N_CORES * 8bit] width
    // read:  disabled when result_queue is almost full.
    val proc_queue = Module(new proc_queue_fifo())

    // RENDER CORE
    // val mip_core     = Module(new IntensityProjectionCore())
    val render_core = Module(new RenderCore())

    // RESULT QUEUE
    val result_queue = Module(new result_cache_fifo())

    // val addrCounter = RegInit(0.U(VOXEL_POS_XLEN.W))
    val addr_reg = RegInit(0.U(VOXEL_POS_XLEN.W))
    // val x_reg      = RegInit(0.U(log2Ceil(SCREEN_H).W))
    // val y_reg      = RegInit(0.U(log2Ceil(SCREEN_V).W))
    val proc_count = RegInit(0.U(log2Ceil(WORKSET_WR_DEPTH).W))

    // proc_queue.io.clk   := clock
    // proc_queue.io.rst   := reset
    // result_queue.io.clk := clock
    // result_queue.io.rst := reset

    /* FSM */
    object states extends ChiselEnum {
        val IDLE, FETCH, CALC = Value
    }
    val channel_state = RegInit(states.IDLE)

    /* Process addrReg and procCounter in this FSM logic */
    switch(channel_state) {
        is(states.IDLE) {
            proc_count := 0.U

            addr_reg := Mux(in.voxel_addr_reg_wren, in.voxel_addr_reg_wrdata, addr_reg)
            when(!proc_queue.io.empty) { channel_state := states.CALC }
        }
        // is(states.FETCH) {
        //     proc_count := 0.U
        //     when(in.need_data && in.addr_reg_wren) {
        //         status   := states.CALC
        //         addr_reg := in.addr_reg_wrdata
        //     }
        // }
        is(states.CALC) {
            /*  increse 1 if handshake is valid */
            proc_count := Mux(proc_queue.io.rd_data_count <= 1.U, proc_count + 1.U, proc_count)
            addr_reg := Mux(
                proc_queue.io.rd_data_count <= 1.U,
                addr_reg + CORENUMS.U,
                addr_reg
            )

            when(proc_count === (WORKSET_WR_DEPTH - 1).U) {
                channel_state := states.IDLE
            }
        }
    }

    /* IOs */
    def delay(x: UInt, n: Int): UInt = {
        if (n == 1) RegNext(x) else delay(RegNext(x), n - 1)
    }

    // val res_almost_full = result_queue.io.almost_full

    /* proc queue IO */
    in.need_data          := (channel_state === states.IDLE)
    proc_queue.io.wr_en   := in.proc_queue_wren
    proc_queue.io.wr_data := in.proc_queue_wrdata

    /* proc_queue ==> mip_core */
    proc_queue.io.rd_en  := (channel_state === states.CALC) && (!result_queue.io.almost_full)
    // mip_core.in.density  := proc_queue.io.rd_data
    // mip_core.in.voxelPos := addr_reg
    // mip_core.in.valid    := proc_queue.io.valid

    /* mip_core ==> result_queue */
    // TODO.
    val packed_result = Wire(UInt(32.W))
    // packed_result := Cat(
    //     mip_core.out.screenPos.x,
    //     mip_core.out.screenPos.y,
    //     mip_core.out.density
    // )

    /* result IO */
    val dec = Module(new ResultDecode())
    dec.io.code := result_queue.io.rd_data

    out.density           := dec.io.density
    out.screen_pos        := dec.io.screen_pos
    out.data_valid        := !result_queue.io.empty
    result_queue.io.rd_en := out.rden

    val decoder = Module(new ResultDecode())
    decoder.io.code := result_queue.io.rd_data
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
