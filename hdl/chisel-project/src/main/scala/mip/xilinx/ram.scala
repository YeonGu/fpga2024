package mip.xilinx

import chisel3._
import chisel3.util._
import mip.xilinx.VramConfigs.{VRAM_ADDRA_WIDTH => VRAM_ADDRA_WIDTH}
import mip.xilinx.VramConfigs.{VRAM_ADDRB_WIDTH => VRAM_ADDRB_WIDTH}

object VramConfigs {
    // divide by 8. a word is 64bit wide.
    val VRAM_ADDRA_WIDTH = log2Ceil(32 * 1024 * 3 / 8)
    val VRAM_ADDRB_WIDTH = log2Ceil(32 * 1024 * 3 / 8)
}

/** Build VRAM in Dual Port Mode.
  *
  * @PortA.
  *   Read (din), 8bit (maybe 16?) wide. OREG = true, latency = 1clk
  *
  * @PortB.
  *   Write (dout), 8bit wide
  *
  * @configs.
  *   capacity = 32 * 1024 * 3 Bytes = 96KB
  */
class ultra_vram extends BlackBox {
    val io = IO(new Bundle {
        val clk = Input(Clock())
        val rst = Input(Bool())

        // val wea = Input(Bool())
        val addra = Input(UInt(VRAM_ADDRA_WIDTH.W))
        val ena   = Input(Bool())
        val douta = Output(UInt(64.W))
        // val dina  = Input(UInt(8.W))

        val addrb = Input(UInt(VRAM_ADDRB_WIDTH.W))
        val enb   = Input(Bool())
        val web   = Input(UInt(8.W))
        val dinb  = Input(UInt(8.W))
        // val doutb = Output(UInt(8.W))

        val wr_reset_busy = Output(Bool())
        val rd_reset_busy = Output(Bool())
    })
}
