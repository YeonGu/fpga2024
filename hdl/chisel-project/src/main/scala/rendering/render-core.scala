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
    val valid    = Input(Bool())
    val density  = Input(Vec(CORENUMS, UInt(DENS_DEPTH.W)))
    val voxelPos = Input(UInt(VOXEL_POS_XLEN.W))
    // val voxelPos      = Input(Vec(CORENUMS, UInt((3 * VOXEL_POS_XLEN).W)))
    val mvpInfo   = Input(new Mat3x4())
    val baseCoord = Input(Vec(3, SInt(BASE_POS_XLEN.W)))
}

class RenderCoreOutputData extends Bundle {
    val res_len      = DENS_DEPTH + log2Up(SCREEN_V) + log2Up(SCREEN_H) + 4
    val valid        = Output(Bool())
    val packedResult = Output(UInt((CORENUMS * res_len).W))
}

class RenderCore(val gridResolution: Int, val gridSize: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(new RenderCoreInputData())
        val out = Output(new RenderCoreOutputData())
    })

    // Compute units
    val voxelPos          = io.in.voxelPos
    val voxelpos_len      = log2Ceil(IMAGE_SIZE)
    val voxelPos_x        = voxelPos(voxelpos_len - 1, 0)
    val voxelPos_x_plus_1 = voxelPos_x + 1.U
    val voxelPos_x_plus_2 = voxelPos_x + 2.U
    val voxelPos_x_plus_3 = voxelPos_x + 3.U

    val CUs = Seq.fill(CORENUMS)(Module(new ComputeUnit(gridResolution, gridSize)))
    CUs.zipWithIndex.foreach { case (cu, idx) =>
        cu.io.in.valid       := io.in.valid
        cu.io.in.density     := io.in.density(idx)
        cu.io.in.voxelPos(1) := voxelPos(2 * voxelpos_len - 1, voxelpos_len)
        cu.io.in.voxelPos(2) := voxelPos(3 * voxelpos_len - 1, 2 * voxelpos_len)
        cu.io.in.mvpInfo     := io.in.mvpInfo
        cu.io.in.baseCoord   := io.in.baseCoord
    }
    CUs(0).io.in.voxelPos(0) := voxelPos_x
    CUs(1).io.in.voxelPos(0) := voxelPos_x_plus_1
    CUs(2).io.in.voxelPos(0) := voxelPos_x_plus_2
    CUs(3).io.in.voxelPos(0) := voxelPos_x_plus_3

    // Output logic
    val allValid = CUs.map(_.io.out.valid).reduce(_ && _)

    val result = VecInit(CUs.zipWithIndex.map { case (cu, _) =>
        Mux(
            allValid,
            Cat(0.U(2.W), cu.io.out.screenPos.y, 0.U(2.W), cu.io.out.screenPos.x, cu.io.out.density),
            0.U
        )
    })

    io.out.valid := allValid

    val resultReg = RegNext(Cat(result))
    io.out.packedResult := resultReg
}

object Main extends App {
    ChiselStage.emitSystemVerilogFile(new RenderCore(512, 256), Array("--target-dir", "generated"))
}