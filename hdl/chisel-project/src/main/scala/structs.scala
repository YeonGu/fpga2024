package __global__

import chisel3._
import chisel3.util.log2Up
import __global__.Params._

class ScreenPos extends Bundle {
    val x = UInt(log2Up(SCREEN_H).W)
    val y = UInt(log2Up(SCREEN_V).W)
}

class VoxelVector3 extends Bundle {
    val x = UInt(VOXEL_POS_XLEN.W)
    val y = UInt(VOXEL_POS_XLEN.W)
    val z = UInt(VOXEL_POS_XLEN.W)
}

class MvpMat extends Bundle{
    
}