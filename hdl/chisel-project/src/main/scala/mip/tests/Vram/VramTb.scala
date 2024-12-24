package mip.tests.Vram

import __global__._
import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import mip.units.VramUnit

class VramTb extends Module {
    val io = IO(new Bundle {
        val tdata = Output(UInt(64.W))
    })
    val vram = Module(new VramUnit())

    val wrCnt = RegInit(0.U(32.W))
    val wtCnt = RegInit(0.U(32.W))
    val rdCnt = RegInit(0.U(32.W))

    object TbStates extends ChiselEnum {
        val IDLE, WRITE, WAIT, READ, DONE = Value
    }
    val state = RegInit(TbStates.IDLE)
    switch(state) {
        is(TbStates.IDLE) { state := TbStates.WRITE }
        is(TbStates.WRITE) {
            wrCnt := wrCnt + 1.U
            when(wrCnt === 63.U) { state := TbStates.WAIT }
        }
        is(TbStates.WAIT) {
            wtCnt := wtCnt + 1.U
            when(vram.io.idle && wtCnt > 8.U) {
                state := TbStates.READ
            }
        }
        is(TbStates.READ) {
            rdCnt := rdCnt + 1.U
            when(rdCnt === 63.U) { state := TbStates.DONE }
        }
    }

    // write
    vram.io.calcResult.foreach { res =>
        res.data_valid := state === TbStates.WRITE
        // res.screen_pos := wrCnt.asTypeOf(new ScreenPos())
        res.screen_pos.y := 0.U
        res.screen_pos.x := wrCnt
    }
    vram.io.calcResult(0).density := wrCnt
    vram.io.calcResult(1).density := wrCnt - 3.U
    vram.io.calcResult(2).density := 64.U - wrCnt
    vram.io.calcResult(3).density := 36.U

    vram.io.graphicUpdate := state === TbStates.WAIT && wtCnt === 0.U

    vram.io.VramRead.ena   := state === TbStates.READ
    vram.io.VramRead.addra := rdCnt
    vram.io.VramRead.wea   := false.B
    vram.io.VramRead.dina  := 0.U
    io.tdata               := vram.io.VramRead.douta
}

object VramTestbenchGen extends App {
    ChiselStage.emitSystemVerilogFile(
        new VramTb(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build/cu-test")
    )
}
