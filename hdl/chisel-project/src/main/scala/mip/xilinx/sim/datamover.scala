package mip.xilinx.sim

import chisel3._
import chisel3.util._
import mip.xilinx._
import _root_.circt.stage.ChiselStage

object DatamoverSim extends App {
    ChiselStage.emitSystemVerilogFile(
        new axi_datamover_sim(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}

class memory_read extends BlackBox {
    val io = IO(new Bundle {
        val addr = Input(UInt(32.W))
        val en   = Input(Bool())
        val data = Output(UInt(128.W))
    })
}

class axi_datamover_sim extends Module {
    val io = IO(new Bundle {
        val s_axis_mm2s_cmd = Flipped(new datamover_m_axis_mm2s_cmd())
        val m_axis_mm2s     = Flipped(new datamover_s_axis_mm2s())
    })

    val idle :: stream :: Nil = Enum(2)

    val state = RegInit(idle)
    val count = RegInit(0.U(21.W))

    val cmd         = io.s_axis_mm2s_cmd.tdata
    val cmd_rd_addr = cmd(63, 32)
    val cmd_btt     = cmd(22, 0)

    val rdaddr = RegInit(0.U(32.W))
    val btt    = RegInit(0.U(23.W))

    val mem = Module(new memory_read())
    mem.io.addr := rdaddr
    mem.io.en   := state === stream

    io.s_axis_mm2s_cmd.tready := state === idle

    io.m_axis_mm2s.tkeep  := "hff".U
    io.m_axis_mm2s.tvalid := state === stream
    io.m_axis_mm2s.tlast  := count === (btt / (128.U / 8.U)) - 1.U
    io.m_axis_mm2s.tdata  := mem.io.data

    when(state === stream && io.m_axis_mm2s.tready && io.m_axis_mm2s.tvalid) {
        count := count + 1.U
    }.elsewhen(state === idle) {
        count := 0.U
    }

    switch(state) {
        is(idle) {
            when(io.s_axis_mm2s_cmd.tvalid && io.s_axis_mm2s_cmd.tready) {
                state  := stream
                rdaddr := cmd_rd_addr
                btt    := cmd_btt
            }
        }
        is(stream) {
            when(count === (btt / (128.U / 8.U)) - 1.U) {
                state := idle
            }
            rdaddr := Mux(
                io.m_axis_mm2s.tready && io.m_axis_mm2s.tvalid,
                rdaddr + 16.U,
                rdaddr
            )
        }
    }

}
