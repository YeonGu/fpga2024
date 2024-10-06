package utils

import chisel3._
import chisel3.util._

// class PipelineReg[T <: Data](gen: T) extends Module {
class PipelineReg(gen: Bundle) extends Module {
    val io = IO(new Bundle {
        val d     = Input(gen)
        val stall = Input(Bool())
        val q     = Output(gen)
    })

    val reg = Reg(gen)
    reg := Mux(io.stall, reg, io.d)
}
