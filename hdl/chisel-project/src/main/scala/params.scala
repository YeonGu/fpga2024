package __global__

import chisel3._
import chisel3.util._

object Params {
    val DENS_DEPTH = 8
    val SCREEN_H   = 960
    val SCREEN_V   = 720

    val IMAGE_SIZE     = 512
    val VOXEL_COUNT    = IMAGE_SIZE * IMAGE_SIZE * IMAGE_SIZE
    val VOXEL_POS_XLEN = 3 * log2Ceil(IMAGE_SIZE)
    val BASE_POS_XLEN  = 32

    val AXIS_ADDR_WIDTH = 32
    val AXIS_DATA_WIDTH = 64

    val MVP_MAT_BITDEPTH = 16

    /* MIP */
    val MIP_CORES             = 16
    val MIP_DATA_WIDTH        = 16
    val MIP_RESULT_FIFO_DEPTH = 512

    /* MIP CACHING */
    val MIP_WB_CHANNELS      = 8
    val MIP_CACHE_GROUPS     = 4
    val MIP_CACHE_N_BLOCKS   = 16 /* Blocks in each group */
    val MIP_CACHE_BLOCK_SIZE = 512
}

object GeneratedParams {
    import Params._

    val SCREEN_SIZE       = SCREEN_H * SCREEN_V
    val SCREEN_ADDR_WIDTH = log2Ceil(SCREEN_H) + log2Ceil(SCREEN_V)

    /* example. The MIP Cache address.
    assume that the screen size is 1920x1080 < 2^21, and the cache block size is 512.
    The cache block size is 512, so the cache block address is 9 bits.
    The cache group size is 4, so the cache group address is 2 bits.
    The cache tag size is 21 - 9 - 2 = 10 bits.

    |20                                     0|
     T   TTTT    TTTT    TGGB    BBBB    BBBB

     T: Tag. Used to find the block in cache group. 缓存标签
     G: Group. Used to determine the cache group. 缓存寻组
     B: Block. Used to determine the actual position in the cache block. 缓存块内偏移
     */
    val MIP_CACHE_GROUP_WIDTH = log2Ceil(MIP_CACHE_GROUPS)
    val MIP_CACHE_TAG_WIDTH = log2Ceil(
        SCREEN_ADDR_WIDTH - MIP_CACHE_GROUP_WIDTH - log2Ceil(MIP_CACHE_BLOCK_SIZE)
    )
}
