package mip.tests

import chisel3._
import chisel3.util._
import mip.MipConfigs.N_MIP_CHANNELS
import mip.units.calcResult
import mip.xilinx.brama_gen_port
import __global__.GeneratedParams.SCREEN_ADDR_WIDTH
import __global__.ScreenPos
import _root_.circt.stage.ChiselStage

class CalcGen extends Module {
    val io = IO(new Bundle {
        val res = new calcResult()
        val end = Output(Bool())
    })

    val send_cnt = RegInit(0.U(8.W))
    object states extends ChiselEnum {
        val IDLE, SEND, END = Value
    }
    val state = RegInit(states.IDLE)

    val endcnt = 32.U

    val addrgen = RegInit(0.U(SCREEN_ADDR_WIDTH.W))
    val datagen = RegInit(0.U(8.W))

    val randaddrgen = Module(new randgen())
    val randdatagen = Module(new randgen())

    randaddrgen.io.clk := clock
    randaddrgen.io.rst := reset
    randdatagen.io.clk := clock
    randdatagen.io.rst := reset

    switch(state) {
        is(states.IDLE) {
            when(send_cnt === 0.U) { state := states.SEND }
            send_cnt := 0.U
            addrgen  := randaddrgen.io.data
            datagen  := randdatagen.io.data
        }
        is(states.SEND) {
            when(send_cnt === endcnt - 1.U) { state := states.END }
            send_cnt := Mux(io.res.rden, send_cnt + 1.U, send_cnt)
            addrgen  := Mux(io.res.rden, randaddrgen.io.data, addrgen)
            datagen  := Mux(io.res.rden, randdatagen.io.data, datagen)
        }
        is(states.END) {
            state := states.END
        }
    }
    io.res.data_valid := state === states.SEND
    io.res.density    := datagen
    io.res.screen_pos := addrgen.asTypeOf(new ScreenPos())
    io.end            := state === states.END
}

class VramTestGen extends Module {
    val io = IO(new Bundle {
        val calc_res = Vec(N_MIP_CHANNELS, (new calcResult()))
        val en_minip = Output(Bool())
        val ram_port = Flipped(new brama_gen_port(64))
    })
    val calc_res_gen = Seq.fill(N_MIP_CHANNELS)(Module(new CalcGen()))
    val cnt = RegInit(0.U(32.W))

    io.en_minip := false.B
    io.calc_res.zipWithIndex.foreach { case (res, i) => res <> calc_res_gen(i).io.res }
    io.ram_port.ena   := false.B
    io.ram_port.wea   := false.B
    io.ram_port.addra := 0.U
    io.ram_port.dina  := 0.U

    cnt := cnt + 1.U
    io.ram_port.ena := cnt > 100.U
    val addr = RegInit(0.U(64.W))
    when(cnt > 100.U) {
        addr := addr + 8.U
    }
    io.ram_port.addra := addr
}

class randgen extends BlackBox {
    val io = IO(new Bundle {
        val clk  = Input(Clock())
        val rst  = Input(Reset())
        val data = Output(UInt(32.W))
    })
}

/** Generate Verilog sources and save it in file
  */
object VramTestGen extends App {
    ChiselStage.emitSystemVerilogFile(
        new VramTestGen(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
