package rendering

import chisel3._
import chisel3.util.log2Up
import __global__.Params._
import __global__._

/** MIP Core for Maximum/Minimum Intensity Projection.
  */

class MipInputData extends Bundle {
    val valid         = Input(Bool())
    val pipelineStall = Input(Bool())
    val density       = Input(UInt(DENS_DEPTH.W))
    val voxelPos      = Input(UInt(VOXEL_POS_XLEN.W))
    val mvpInfo       = Input(new Mat3x3())
}

class MipOutputData extends Bundle {
    val valid     = Output(Bool())
    val density   = Output(UInt(DENS_DEPTH.W))
    val screenPos = Output(new ScreenPos())
}

/** MIP/MinIP/AIP(?) Core.
  * @in
  * @out
  */
class IntensityProjectionCore extends Module {
    val in  = IO(new MipInputData())
    val out = IO(new MipOutputData())
}
