package mip.xilinx

import chisel3._
import chisel3.util._

object VramConfigs {
    // divide by 8. a word is 64bit wide.
    val VRAM_ADDRA_WIDTH = 32 // log2Ceil(32 * 1024 * 3 / 8) // 14 bits
    val VRAM_ADDRB_WIDTH = 32 // log2Ceil(32 * 1024 * 3 / 8)
    val VRAM_BYTE_ADDR   = 32 // log2Ceil(32 * 1024 * 3)     // 17 bits
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

        // read @ a
        val addra = Input(UInt(32.W))
        val ena   = Input(Bool())
        val douta = Output(UInt(64.W))

        // write @ b
        val addrb = Input(UInt(32.W))
        val enb   = Input(Bool())
        val web   = Input(UInt(8.W))
        val dinb  = Input(UInt(64.W))

        val wr_reset_busy = Output(Bool())
        val rd_reset_busy = Output(Bool())
    })
}
