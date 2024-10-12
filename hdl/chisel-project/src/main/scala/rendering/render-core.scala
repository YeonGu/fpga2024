package rendering

import chisel3._
import circt.stage.ChiselStage
import chisel3.util._
import __global__._
import __global__.Params._
import rendering.ComputeUnit
import rendering.CUParams._

object CoreParams {
    val CORENUMS = 4
}

import CoreParams._

class RenderCoreInputData extends Bundle {
    val valid         = Input(Bool())
    val pipelineStall = Input(Bool())
    val density       = Input(Vec(CORENUMS, UInt(DENS_DEPTH.W)))
    val voxelPos      = Input(Vec(CORENUMS, UInt((3 * VOXEL_POS_XLEN).W)))
    val mvpInfo       = Input(new Mat3x3())
    val baseCoord     = Input(Vec(3, SInt(BASEPOS_XLEN.W)))
}

class RenderCoreOutputData extends Bundle {
    val valid = Output(Bool())
    val packedResult =
        Output(Vec(
            CORENUMS,
            UInt((DENS_DEPTH + log2Up(SCREEN_V).toInt + log2Up(SCREEN_H).toInt).W)
        ))
}

class RenderCore extends Module {
    val io = IO(new Bundle {
        val in  = Input(new RenderCoreInputData())
        val out = Output(new RenderCoreOutputData())
    })

    // Compute units
    val CUs = Seq.fill(CORENUMS)(Module(new ComputeUnit(128, 64)))
    CUs.zipWithIndex.foreach { case (cu, idx) =>
        cu.io.in.valid         := io.in.valid
        cu.io.in.pipelineStall := io.in.pipelineStall
        cu.io.in.density       := io.in.density(idx)
        cu.io.in.voxelPos      := io.in.voxelPos(idx)
        cu.io.in.mvpInfo       := io.in.mvpInfo
        cu.io.in.baseCoord     := io.in.baseCoord
    }

    // Result registers
    val resultRegs = RegInit(VecInit(Seq.fill(CORENUMS)(
        0.U((DENS_DEPTH + log2Up(SCREEN_V).toInt + log2Up(SCREEN_H).toInt).W)
    )))

    // Output logic
    val allValid = CUs.map(_.io.out.valid).reduce(_ && _)

    when(allValid) {
        io.out.valid := true.B
        resultRegs.zip(CUs).foreach { case (res, cu) =>
            res := Cat(
                cu.io.out.screenPos.y,
                cu.io.out.screenPos.x,
                cu.io.out.density
            )
        }
    }.otherwise {
        io.out.valid := false.B
        resultRegs   := resultRegs
    }

    io.out.packedResult := resultRegs
}


