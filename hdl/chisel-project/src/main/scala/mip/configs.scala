package mip

object MipConfigs {
    val PROC_QUEUE_WR_WIDTH = 64
    val WORKSET_SIZE        = 128

    /* AXI configs */
    val MIP_AXIS_MM2S_WIDTH = 128
    val MIP_AXIS_S2MM_WIDTH = 64

    val N_MIP_CHANNELS = 4 // Number of MIP calculation channels
    val N_MIP_CORES    = 4 // Number of MIP cores in each MIP calculation channel

    /* VRAM configs */
    val VRAM_CHANNELS = 8

}
