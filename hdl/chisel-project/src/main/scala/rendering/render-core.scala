package rendering

import __global__.Params._
import __global__._
import chisel3._
import circt.stage.ChiselStage
import chisel3.util._
import rendering.CUParams._

object CoreParams {
    val CORENUMS = 4
    val RES_LEN  = 32
}

import rendering.CoreParams._

class RenderCoreInputData extends Bundle {
    val valid    = Input(Bool())
    val density  = Input(Vec(CORENUMS, UInt(DENS_DEPTH.W)))
    val voxelPos = Input(UInt(VOXEL_POS_XLEN.W))
    // val voxelPos      = Input(Vec(CORENUMS, UInt((3 * VOXEL_POS_XLEN).W)))
    val mvpInfo   = Input(new Mat3x4())
    val baseCoord = Input(Vec(3, UInt(BASE_POS_XLEN.W)))
}

class RenderCoreOutputData extends Bundle {
    val valid        = Output(Bool())
    val packedResult = Output(UInt((CORENUMS * RES_LEN).W))
}

// class RenderCore(val gridResolution: Int, val gridSize: Int) extends Module {
//     val io = IO(new Bundle {
//         val in  = Input(new RenderCoreInputData())
//         val out = Output(new RenderCoreOutputData())
//     })
//
//     // Compute units
//     val voxelPos          = io.in.voxelPos
//     val voxelpos_len      = log2Ceil(IMAGE_SIZE)
//     val voxelPos_x        = voxelPos(voxelpos_len - 1, 0)
//     val voxelPos_x_plus_1 = voxelPos_x + 1.U
//     val voxelPos_x_plus_2 = voxelPos_x + 2.U
//     val voxelPos_x_plus_3 = voxelPos_x + 3.U
//
//     val CUs = Seq.fill(CORENUMS)(Module(new ComputeUnit(gridResolution, gridSize)))
//     CUs.zipWithIndex.foreach { case (cu, idx) =>
//         cu.io.in.valid       := io.in.valid
//         cu.io.in.density     := io.in.density(idx)
//         cu.io.in.voxelPos(1) := voxelPos(2 * voxelpos_len - 1, voxelpos_len)
//         cu.io.in.voxelPos(2) := voxelPos(3 * voxelpos_len - 1, 2 * voxelpos_len)
//         cu.io.in.mvpInfo     := io.in.mvpInfo
//         cu.io.in.baseCoord   := io.in.baseCoord
//     }
//     CUs(0).io.in.voxelPos(0) := voxelPos_x
//     CUs(1).io.in.voxelPos(0) := voxelPos_x_plus_1
//     CUs(2).io.in.voxelPos(0) := voxelPos_x_plus_2
//     CUs(3).io.in.voxelPos(0) := voxelPos_x_plus_3
//
//     // Output logic
//     /*
//     class MipOutputData extends Bundle {
//         val valid     = Output(Bool())
//         val density   = Output(UInt(DENS_DEPTH.W))
//         val screenPos = Output(new ScreenPos())
//     } */
//     val allValid = CUs.map(_.io.out.valid).reduce(_ && _)
//
//     val packedResult = Wire(UInt((CORENUMS * RES_LEN).W))
//     packedResult := CUs.zipWithIndex.map {
//         case (cu, idx) => {
//             val singleResult = Wire(UInt(RES_LEN.W))
//             singleResult := cu.io.out.asUInt
//             singleResult
//         }
//     }.reduce(Cat(_, _)).asUInt
//
//     // val resultReg = RegNext(Cat(result))
//     io.out.valid        := RegNext(allValid)
//     io.out.packedResult := RegNext(packedResult)
// }

class packedResultDecode extends Module {
    val io = IO(new Bundle {
        val packedResult   = Input(UInt(32.W))
        val decodedMipData = new MipOutputData()
    })
    val res = io.packedResult.asTypeOf(new MipOutputData())
    io.decodedMipData := res
}

// object Main extends App {
//     // ChiselStage.emitSystemVerilogFile(new packedResultDecode, Array("--target-dir", "build"))
//     ChiselStage.emitSystemVerilogFile(new RenderCore(512, 256), Array("--target-dir", "build"))
// }
//
class RenderCore extends BlackBox {
    val io = IO(new Bundle {
        val ap_clk        = Input(Clock())
        val ap_rst        = Input(Bool())
        val valid_in      = Input(Bool())
        val density_in    = Input(Vec(CORENUMS, UInt(DENS_DEPTH.W)))
        val mvp_mat_in    = Input(Vec(2, Vec(4, UInt(32.W))))
        val base_coord_in = Input(Vec(3, UInt(32.W)))
        val voxel_pos     = Input(UInt(VOXEL_POS_XLEN.W))
        val valid         = Output(Bool())
        val pixel_idx_out = Output(Vec(4, Vec(2, UInt(log2Up(SCREEN_V).W))))
        val density_out   = Output(Vec(4, UInt(DENS_DEPTH.W)))
    })
}

class RenderCoreWrapper extends Module {
    val io = IO(new Bundle {
        val in  = new RenderCoreInputData()
        val out = new RenderCoreOutputData()
    })

    val renderCore = Module(new RenderCore())
    val mvp_mat_in = Wire(Vec(2, Vec(4, UInt(32.W))))
    for (i <- 0 until 2; j <- 0 until 4) {
        mvp_mat_in(i)(j) := io.in.mvpInfo.mat(i)(j).asTypeOf(UInt(32.W))
    }
    renderCore.io.ap_clk     := clock
    renderCore.io.ap_rst     := reset
    renderCore.io.valid_in   := io.in.valid
    renderCore.io.density_in := io.in.density
    // TODO: mvp_mat_in: 2x4 matrix
    renderCore.io.mvp_mat_in    := mvp_mat_in
    renderCore.io.base_coord_in := io.in.baseCoord
    renderCore.io.voxel_pos     := io.in.voxelPos

    val valid_out     = renderCore.io.valid
    val pixel_idx_out = renderCore.io.pixel_idx_out
    val density_out   = renderCore.io.density_out
    val packed_result = Cat(pixel_idx_out.zip(density_out).map { case (pixel, dens) =>
        Cat(0.U(3.W), valid_out, dens, pixel(1), pixel(0))
    }.reverse)
    io.out.valid        := valid_out
    io.out.packedResult := packed_result
}

object RenderCoreWrapper extends App {
    ChiselStage.emitSystemVerilogFile(new RenderCoreWrapper, Array("--target-dir", "build"))
}
