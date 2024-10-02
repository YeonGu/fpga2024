package math

import chisel3._

/** 3x3 Matrix Multiplication.
  */

class MatMul3x3(BITDEPTH: Int) extends BlackBox {
    val io = IO(new Bundle {
        val a = Input(Vec(3, Vec(3, UInt(BITDEPTH.W))))
        val b = Input(Vec(3, Vec(3, UInt(BITDEPTH.W))))
        val c = Output(Vec(3, Vec(3, UInt(BITDEPTH.W))))
    })
}
