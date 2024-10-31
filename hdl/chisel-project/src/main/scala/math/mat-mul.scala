package math

import chisel3._
import circt.stage.ChiselStage
import chisel3.util.ShiftRegister

/** 3x3 Matrix Multiplication.
  */
class FixedPointMultiplier(matSize: Int, bitWidth: Int, signed: Boolean) extends Module {
    val io = IO(new Bundle {
        val a = Input(Vec(matSize, Vec(matSize, UInt(bitWidth.W))))
        val b = Input(Vec(matSize, Vec(matSize, UInt(bitWidth.W))))
        val c = Output(Vec(matSize, Vec(matSize, UInt((2 * bitWidth).W))))
    })

}

class FixedPointRounder(bitWidth: Int, fracWidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(Vec(3, Vec(3, UInt((2 * bitWidth).W))))
        val out = Output(Vec(3, Vec(3, UInt(bitWidth.W))))
    })

    for (i <- 0 until 3; j <- 0 until 3) {
        io.out(i)(j) := Mux(
            io.in(i)(j)(fracWidth - 1),
            (io.in(i)(j) + (1.U) << (fracWidth - 1))(bitWidth + fracWidth - 1, fracWidth),
            io.in(i)(j)(bitWidth + fracWidth - 1, fracWidth)
        )
    }
}

class MatMul(matSize: Int, bitWidth: Int, fracWidth: Int, pipelineStages: Int, signed: Boolean)
    extends Module {
    val io = IO(new Bundle {
        val a         = Input(Vec(3, Vec(3, UInt(bitWidth.W))))
        val b         = Input(Vec(3, Vec(3, UInt(bitWidth.W))))
        val c         = Output(Vec(3, Vec(3, UInt(bitWidth.W))))
        val in_valid  = Input(Bool())
        val out_valid = Output(Bool())
    })

    val multiplier = Module(new FixedPointMultiplier(3, bitWidth, signed))
    multiplier.io.a := io.a
    multiplier.io.b := io.b

    val mul_result = (if (pipelineStages > 3) RegNext(multiplier.io.c)
                      else multiplier.io.c)

    val rounder = Module(new FixedPointRounder(bitWidth, fracWidth))
    rounder.io.in := mul_result
    val round_result = RegNext(rounder.io.out)

    io.c := round_result

    val shift_reg = ShiftRegister(io.in_valid, pipelineStages)
    io.out_valid := shift_reg
}

// object Main extends App {
//     ChiselStage.emitSystemVerilogFile(new MatMul(3, 16, 4, 3, false))
// }
