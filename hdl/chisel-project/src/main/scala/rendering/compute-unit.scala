package rendering

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import rendering.{MipInputData, MipOutputData}
import __global__._
import __global__.Params._
import fixedpoint._

object CUParams {
    val FRAC_WIDTH         = 6
    val TOTAL_WIDTH        = 16
    val INT_WIDTH          = TOTAL_WIDTH - FRAC_WIDTH
    val VOXEL_OFFSET_WIDTH = 9
    // val SCREEN_W           = 640 // 假设的屏幕宽度
    // val SCREEN_H           = 480 // 假设的屏幕高度
    // val DENS_DEPTH         = 8   // 假设的density位宽
}

import CUParams._

class StageIO extends Bundle {
    val inputValid = Bool()
    val valid      = Bool()
    val density    = UInt(DENS_DEPTH.W)
    val data       = Vec(3, SInt(TOTAL_WIDTH.W))
    val overflow   = Bool()
}

// assuming baseCoord is already fixed point
class WorldCoordStage(gridResolution: Int, gridSize: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(new MipInputData())
        val out = Output(new StageIO())
    })

    require(log2Up(gridResolution) >= FRAC_WIDTH)
    val voxel_offset_x =
        io.in.voxelPos(log2Up(gridResolution) - 1, log2Up(gridResolution) - FRAC_WIDTH)
    val voxel_offset_y =
        io.in.voxelPos(log2Up(gridResolution) + 9 - 1, log2Up(gridResolution) + 9 - FRAC_WIDTH)
    val voxel_offset_z =
        io.in.voxelPos(log2Up(gridResolution) + 9 + 9 - 1, log2Up(gridResolution) + 9 + 9 - FRAC_WIDTH)
    val world_offset_x = (voxel_offset_x * gridSize.U).asSInt
    val world_offset_y = (voxel_offset_y * gridSize.U).asSInt
    val world_offset_z = (voxel_offset_z * gridSize.U).asSInt

    val world_coord_tmp = Wire(Vec(3, SInt((1 + TOTAL_WIDTH).W)))
    world_coord_tmp(0) := io.in.baseCoord.asTypeOf(Vec(3, SInt(TOTAL_WIDTH.W)))(
        0
    ) +& world_offset_x
    world_coord_tmp(1) := io.in.baseCoord.asTypeOf(Vec(3, SInt(TOTAL_WIDTH.W)))(
        1
    ) +& world_offset_y
    world_coord_tmp(2) := io.in.baseCoord.asTypeOf(Vec(3, SInt(TOTAL_WIDTH.W)))(
        2
    ) +& world_offset_z

    val overflow = world_coord_tmp.map { coord =>
        coord(TOTAL_WIDTH) =/= coord(TOTAL_WIDTH - 1)
    }.reduce(_ || _)

    val world_coord = Wire(Vec(3, SInt(TOTAL_WIDTH.W)))
    world_coord.zipWithIndex.map { case (coord, idx) =>
        coord := Mux(
            overflow,
            Mux(
                world_coord_tmp(idx)(TOTAL_WIDTH),
                -(1 << (TOTAL_WIDTH - 1)).S,
                ((1 << (TOTAL_WIDTH - 1)) - 1).S
            ),
            world_coord_tmp(idx)(TOTAL_WIDTH - 1, 0).asSInt
        )
    }

    val outReg = RegInit(0.U.asTypeOf(new StageIO()))
    outReg.inputValid := io.in.valid && !io.in.pipelineStall
    outReg.valid      := !overflow
    outReg.density    := io.in.density
    outReg.data       := world_coord
    outReg.overflow   := overflow

    io.out := outReg
}

class MVPTransformStage(gridResolution: Int, gridSize: Int) extends Module {
    val io = IO(new Bundle {
        val in      = Input(new StageIO())
        val mvpInfo = Input(new Mat3x3())
        val out     = Output(new StageIO())
    })

    val transformedCoord = Wire(Vec(3, SInt((2 * TOTAL_WIDTH).W)))
    for (i <- 0 until 3) {
        transformedCoord(i) := (io.mvpInfo.mat(i)(0) * io.in.data(0) +
            io.mvpInfo.mat(i)(1) * io.in.data(1) +
            io.mvpInfo.mat(i)(2) * io.in.data(2)) >> FRAC_WIDTH
    }

    val outReg = RegInit(0.U.asTypeOf(new StageIO()))
    outReg.inputValid := io.in.inputValid
    outReg.valid      := io.in.valid
    outReg.density    := io.in.density
    outReg.data       := transformedCoord.map(_.asSInt)
    outReg.overflow   := io.in.overflow

    io.out := outReg
}

class PerspectiveDivisionStage(gridResolution: Int, gridSize: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(new StageIO())
        val out = Output(new MipOutputData())
    })

    val divByZero = io.in.data(2) === 0.S

    val focal = (0.4 * (1 << FRAC_WIDTH)).toInt.S(TOTAL_WIDTH.W)
    val aspectRatio =
        ((SCREEN_H.toFloat / SCREEN_V.toFloat) * (1 << FRAC_WIDTH)).toInt.S(TOTAL_WIDTH.W)
    val screenCoord = Wire(Vec(2, SInt(TOTAL_WIDTH.W)))
    def fixedPointDivision(num: SInt, den: SInt): SInt = {
        val shiftedNum = (num << FRAC_WIDTH).asSInt
        (shiftedNum / den).asSInt
    }

    when(!divByZero) {
        val xDivZ = fixedPointDivision(io.in.data(0), io.in.data(2))
        val yDivZ = fixedPointDivision(io.in.data(1), io.in.data(2))

        val xDivZF = fixedPointDivision(xDivZ, focal)
        val yDivZF = fixedPointDivision(yDivZ, focal)

        val yDivZFA = (yDivZF * aspectRatio) >> FRAC_WIDTH

        screenCoord(0) := (xDivZF + (1.S << FRAC_WIDTH)) >> 1
        screenCoord(1) := ((1.S << FRAC_WIDTH) - (yDivZFA + (1.S << FRAC_WIDTH)) >> 1)
    }.otherwise {
        screenCoord.foreach(_ := 0.S)
    }

    val screenX =
        (((screenCoord(0).asSInt * SCREEN_H.S) + (1.S << FRAC_WIDTH - 1)) >> FRAC_WIDTH).asUInt
    val screenY =
        ((SCREEN_V.S - ((screenCoord(
            1
        ).asSInt * SCREEN_V.S) + (1.S << FRAC_WIDTH - 1)) >> FRAC_WIDTH)).asUInt

    val inScreen = screenX < SCREEN_H.U && screenY < SCREEN_V.U
    val isValid  = io.in.inputValid && inScreen && !io.in.overflow && !divByZero

    val outReg = RegInit(0.U.asTypeOf(new MipOutputData()))
    outReg.valid       := isValid
    outReg.density     := Mux(isValid, io.in.density, 0.U)
    outReg.screenPos.x := screenX
    outReg.screenPos.y := screenY

    io.out := outReg
}

class ComputeUnit(gridResolution: Int, gridSize: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(new MipInputData())
        val out = Output(new MipOutputData())
    })

    val worldCoordStage          = Module(new WorldCoordStage(gridResolution, gridSize))
    val mvpTransformStage        = Module(new MVPTransformStage(gridResolution, gridSize))
    val perspectiveDivisionStage = Module(new PerspectiveDivisionStage(gridResolution, gridSize))

    worldCoordStage.io.in := io.in

    mvpTransformStage.io.in      := worldCoordStage.io.out
    mvpTransformStage.io.mvpInfo := io.in.mvpInfo

    perspectiveDivisionStage.io.in := mvpTransformStage.io.out

    val pipelineDepth = 3 // 假设总共有3个流水线阶段
    val delayCounter  = RegInit(0.U(log2Ceil(pipelineDepth + 1).W))

    when(io.in.valid && !io.in.pipelineStall) {
        when(delayCounter === pipelineDepth.U) {
            delayCounter := delayCounter
        }.otherwise {
            delayCounter := delayCounter + 1.U
        }
    }.elsewhen(io.in.pipelineStall) {
        delayCounter := 0.U
    }

    val outputReg = RegNext(perspectiveDivisionStage.io.out)
    outputReg.valid := outputReg.valid && (delayCounter === pipelineDepth.U)

    io.out := outputReg
}

object Main extends App {
    ChiselStage.emitSystemVerilogFile(new PerspectiveDivisionStage(128, 64))
}
