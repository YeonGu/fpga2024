package __global__

import chisel3._

object Params {
    val DENS_DEPTH = 8
    val SCREEN_H   = 1920
    val SCREEN_V   = 1080

    val IMAGE_SIZE     = 1024
    val VOXEL_POS_XLEN = 16

    val MVP_MAT_BITDEPTH = 8

    val MIP_CORES = 16;
}
