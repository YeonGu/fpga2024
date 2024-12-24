package utils

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

// class PipelineReg[T <: Data](gen: T) extends Module {
//class PipelineReg(gen: Bundle) extends Module {
//    val io = IO(new Bundle {
//        val d     = Input(gen)
//        val stall = Input(Bool())
//        val q     = Output(gen)
//    })
//
//    val reg = Reg(gen)
//    reg := Mux(io.stall, reg, io.d)
//}

class BramAddrReg extends Module {
    val io = IO(new Bundle {
        val bram_addr_a   = Input(UInt(13.W))
        val bram_wrdata_a = Input(UInt(32.W))
        val en_a          = Input(Bool())
        val wea           = Input(UInt(4.W))
        val bram_rddata_a = Output(UInt(32.W))
    })

    val regFile = Reg(Vec(32, UInt(32.W)))

    // write
    val extendWea = Wire(UInt(32.W))
    extendWea := io.wea.asBools.reverse.map(x => Mux(x, 0xff.U, 0.U)).reduce(Cat(_, _))

    // read
    //    when(io.en_a) {
    io.bram_rddata_a := RegNext(regFile(io.bram_addr_a >> 4))
    //    } otherwise {
    //        io.bram_rddata_a := RegNext(io.bram_addr_a)
    //    }

    when(io.en_a && io.wea.asBools.reduce(_ || _)) {
//        regFile(io.bram_addr_a >> 2) :=
//            (io.bram_wrdata_a & extendWea) | (regFile(io.bram_addr_a >> 2) & ~extendWea)
        regFile(io.bram_addr_a >> 4) := io.bram_wrdata_a
    }
}

object BramAddrTestGen extends App {
    ChiselStage.emitSystemVerilogFile(
        new BramAddrReg(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build/calc-test")
    )
}
