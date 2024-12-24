package mip

import rendering.CoreParams.CORENUMS

object MipConfigs {
    val DispatchFifoWrDepth = 1024
    val DispatchFifoRdDepth = 512
    val N_MIP_CHANNELS      = 4 // Number of MIP calculation channels
    val N_MIP_CORES         = 4 // Number of MIP cores in each MIP calculation channel

    // PROC QUEUE CONFIGS
    // data fetcher -> mip core processing queue fifo configs
    // write: 128bits wide, depth = 512 => 2*32K blockRAMs,
    // read:  8 * N_CORES = 32bits wide. depth = 2048
    val PROC_QUEUE_WR_WIDTH = 256 // 32 Bytes
    val WORKSET_WR_CNT      = 128 // 512 write depth. smaller to decrease latency
    // val PROC_QUEUE_WR_CNT   = WORKSET_SIZE / PROC_QUEUE_WR_WIDTH
    val PROC_QUEUE_RD_WIDTH = CORENUMS * 8 // 32
    val PROC_QUEUE_RD_DEPTH = 4096
    val WORKSET_RD_CNT      = WORKSET_WR_CNT * (PROC_QUEUE_WR_WIDTH / PROC_QUEUE_RD_WIDTH)

    val RES_CACHE_WR_DEPTH = 512 // 512 write depth. smaller to decrease latency
    val RES_CACHE_RD_DEPTH = RES_CACHE_WR_DEPTH * N_MIP_CORES

    /* AXI configs */
    val MIP_AXIS_MM2S_WIDTH = 128
    val MIP_AXIS_S2MM_WIDTH = 64

    /* VRAM configs */
    val VRAM_CHANNELS = 8
}
