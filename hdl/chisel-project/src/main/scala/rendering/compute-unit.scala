package rendering

import __global__.Params._
import __global__._
import chisel3._
import chisel3.util._
// import fixedpoint._

object CUParams {
    val FRAC_WIDTH              = 6
    val TOTAL_WIDTH             = 16
    val INT_WIDTH               = TOTAL_WIDTH - FRAC_WIDTH - 1            // 不包括符号位
    val VOXEL_OFFSET_WIDTH      = 9
    val FRAC_INTERMEDIATE_WIDTH = 12
    val INTERMEDIATE_WIDTH      = INT_WIDTH + FRAC_INTERMEDIATE_WIDTH + 1 // 包括符号位
}

// import CUParams._
//
// class StageIO extends Bundle {
//     val valid   = Bool()
//     val density = UInt(DENS_DEPTH.W)
//     val data    = Vec(3, new FloatPoint())
// }
//
// // gridResolution, gridSize是电路常量, 暂时不支持软件更改密度场分辨率和大小
// // 计算的第一个stage. 将体素坐标转换为世界坐标.
// class WorldCoordStage(val gridResolution: Int, val gridSize: Int) extends Module {
//     val voxel_width = log2Up(gridResolution).toInt
//     val io = IO(new Bundle {
//         val in  = new MipInputData()
//         val out = Output(new StageIO())
//     })
//
//     val ratio = ((0x1000.S(INTERMEDIATE_WIDTH.W) >> log2Ceil(gridResolution)) << log2Ceil(gridSize)).asSInt
//     val voxel_offset = Wire(Vec(3, SInt(INTERMEDIATE_WIDTH.W)))
//     for (i <- 0 until 3) {
//         voxel_offset(i) := Cat(
//             0.U(1.W),
//             io.in.voxelPos(i),
//             0.U(FRAC_INTERMEDIATE_WIDTH.W)
//         ).asSInt
//     }
//
//     val world_offset_tmp = Wire(Vec(3, SInt((2 * INTERMEDIATE_WIDTH).W)))
//     for (i <- 0 until 3) {
//         world_offset_tmp(i) := voxel_offset(i) * ratio
//     }
//     val world_offset = Wire(Vec(3, SInt(INTERMEDIATE_WIDTH.W)))
//     for (i <- 0 until 3) {
//         world_offset(i) := world_offset_tmp(i)(
//             FRAC_INTERMEDIATE_WIDTH + INTERMEDIATE_WIDTH - 1,
//             FRAC_INTERMEDIATE_WIDTH
//         ).asSInt
//     }
//     val base_coord = Wire(Vec(3, SInt(INTERMEDIATE_WIDTH.W)))
//     for (i <- 0 until 3) {
//         base_coord(i) := Cat(
//             io.in.baseCoord(i),
//             0.U((INTERMEDIATE_WIDTH - BASE_POS_XLEN).W)
//         ).asSInt
//     }
//     val world_coord_tmp = Wire(Vec(3, SInt((1 + INTERMEDIATE_WIDTH).W)))
//     for (i <- 0 until 3) {
//         world_coord_tmp(i) := world_offset(i) + base_coord(i)
//     }
//
//     val out_reg = RegInit(0.U.asTypeOf(new StageIO()))
//     out_reg.valid   := RegNext(io.in.valid)
//     out_reg.density := io.in.density
//     out_reg.data(0) := Fixed2Float(world_coord_tmp(0)(INTERMEDIATE_WIDTH - 1, 0).asSInt, clock)
//     out_reg.data(1) := Fixed2Float(world_coord_tmp(1)(INTERMEDIATE_WIDTH - 1, 0).asSInt, clock)
//     out_reg.data(2) := Fixed2Float(world_coord_tmp(2)(INTERMEDIATE_WIDTH - 1, 0).asSInt, clock)
//
//     io.out := out_reg
// }
//
// // 计算的第二个stage. 将世界坐标转换为屏幕坐标. 这个转换通过MVP矩阵完成.
// // 在计算机图形学中, MVP矩阵即Model-View-Projection矩阵, 是一个4x4矩阵, 用于将世界坐标转换为规范化坐标.
// class MVPTransformStage(val gridResolution: Int, val gridSize: Int) extends Module {
//     val io = IO(new Bundle {
//         val in      = Input(new StageIO())
//         val mvpInfo = Input(new Mat3x4())
//         val out     = Output(new StageIO())
//     })
//
//     val valid       = ShiftRegister(io.in.valid, 7)
//     val density     = ShiftRegister(io.in.density, 7)
//     val world_coord = Wire(Vec(3, new FloatPoint))
//     for (i <- 0 until 3) {
//         world_coord(i) := io.in.data(i)
//     }
//
//     val transformed_coord = Wire(Vec(3, new FloatPoint()))
//     for (i <- 0 until 3) {
//         val val00     = FloatMul(world_coord(0), io.mvpInfo.mat(i)(0), clock)
//         val val01     = FloatMul(world_coord(1), io.mvpInfo.mat(i)(1), clock)
//         val val02     = FloatMul(world_coord(2), io.mvpInfo.mat(i)(2), clock)
//         val val00_reg = RegNext(val00)
//         val val01_reg = RegNext(val01)
//         val val02_reg = RegNext(val02)
//         val acc01     = FloatAdd(val00_reg, val01_reg, clock)
//         val acc02     = FloatAdd(val02_reg, io.mvpInfo.mat(i)(3), clock)
//         val acc       = FloatAdd(acc01, acc02, clock)
//         transformed_coord(i) := acc
//     }
//     val out_reg = RegInit(0.U.asTypeOf(new StageIO))
//     out_reg.valid   := valid
//     out_reg.density := density
//     out_reg.data(0) := transformed_coord(0)
//     out_reg.data(1) := transformed_coord(1)
//     out_reg.data(2) := transformed_coord(2)
//     io.out          := out_reg
// }
//
// class PerspectiveDivisionStage(val gridResolution: Int, val gridSize: Int) extends Module {
//     val io = IO(new Bundle {
//         val in  = Input(new StageIO())
//         val out = Output(new MipOutputData())
//     })
//
//     val valid   = ShiftRegister(io.in.valid, 12)
//     val density = ShiftRegister(io.in.density, 12)
//     // pre-defined constants
//     // val focal = FloatPoint(0.B, "b10000".U, "b100000000".U)
//     // val focal_aspect = FloatPoint(0.B, "b10000".U, "b1010101010".U)
//
//     val world_coord_x = io.in.data(0)
//     val world_coord_y = io.in.data(1)
//     val world_coord_z = io.in.data(2)
//     val half_screen   = (1.S << (FRAC_INTERMEDIATE_WIDTH - 1))
//     // val coord_x_focal = FloatMul(world_coord_x, focal, clock)
//     // val coord_y_focal = FloatMul(world_coord_y, focal_aspect, clock)
//
//     val world_coord_x_plus1 = FloatAdd(world_coord_x, FloatPoint(0.B, "b1111".U, 0.U), clock)
//     val world_coord_y_plus1 = FloatSub(FloatPoint(0.B, "b1111".U, 0.U), world_coord_y, clock)
//
//     // val x_div_z_plus_1 = FloatAdd(x_div_z, FloatPoint(0.B, "b1110".U, 0.U), clock)
//     // val y_div_z_plus_1 = FloatAdd(y_div_z, FloatPoint(0.B, "b1110".U, 0.U), clock)
//     val screen_coord_x = FloatDiv(world_coord_x_plus1, FloatPoint(0.B, "b10000".U, 0.U), clock)
//     val screen_coord_y = FloatDiv(world_coord_y_plus1, FloatPoint(0.B, "b10000".U, 0.U), clock)
//
//     val screen_pos_x = FloatMul(screen_coord_x, FloatPoint(0.B, "b11000".U, "b1110000000".U), clock)
//     val screen_pos_y = FloatMul(screen_coord_y, FloatPoint(0.B, "b11000".U, "b0110100000".U), clock)
//
//     val screen_idx_x = FloatRnd(screen_pos_x, clock)
//     val screen_idx_y = FloatRnd(screen_pos_y, clock)
//     val cmp00        = !screen_pos_x.sign
//     val cmp01        = screen_idx_x < SCREEN_H.U
//     val cmp10        = !screen_pos_y.sign
//     val cmp11        = screen_idx_y < SCREEN_V.U
//
//     val in_screen = cmp00 && cmp01 && cmp10 && cmp11
//     val out_reg   = RegInit(0.U.asTypeOf(new MipOutputData))
//     out_reg.valid       := valid && in_screen
//     out_reg.density     := density
//     out_reg.screenPos.x := screen_idx_x
//     out_reg.screenPos.y := screen_idx_y
//     io.out              := out_reg
// }
//
// class ComputeUnit(val gridResolution: Int, val gridSize: Int) extends Module {
//     val io = IO(new Bundle {
//         val in  = Input(new MipInputData())
//         val out = Output(new MipOutputData())
//     })
//
//     // stage 1. voxel coord -> world coord
//     val world_coord_stage = Module(new WorldCoordStage(gridResolution, gridSize))
//     // stage 2. world coord ---mvp matrix---> screen coord
//     val mvp_transform_stage = Module(new MVPTransformStage(gridResolution, gridSize))
//     val perspective_division_stage =
//         Module(new PerspectiveDivisionStage(gridResolution, gridSize))
//
//     // connections
//     world_coord_stage.io.in          := io.in
//     mvp_transform_stage.io.mvpInfo   := io.in.mvpInfo
//     mvp_transform_stage.io.in        := world_coord_stage.io.out
//     perspective_division_stage.io.in := mvp_transform_stage.io.out
//
//     io.out := perspective_division_stage.io.out
// }
//
// object CU extends App {
//     ChiselStage.emitSystemVerilogFile(new ComputeUnit(512, 64), Array("--target-dir", "generated"))
// }
