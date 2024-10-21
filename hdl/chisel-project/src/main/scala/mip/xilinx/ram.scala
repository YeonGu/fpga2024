package mip.xilinx

import chisel3._
import chisel3.util._
import mip.xilinx.VramConfigs.{VRAM_ADDRA_WIDTH => VRAM_ADDRA_WIDTH}
import mip.xilinx.VramConfigs.{VRAM_ADDRB_WIDTH => VRAM_ADDRB_WIDTH}

object VramConfigs {
    val VRAM_ADDRA_WIDTH = log2Ceil(32 * 1024 * 3)
    val VRAM_ADDRB_WIDTH = log2Ceil(32 * 1024 * 3)
}

/** Build VRAM in Simple Dual Port Mode.
  *
  * Port A. Read (din), 64bit wide. OREG = true
  *
  * Port B. Write (dout), 8bit wide
  */
class ultra_vram extends BlackBox {
    val io = IO(new Bundle {
        // val wea = Input(Bool())
        val ena   = Input(Bool())
        val addra = Input(UInt(VRAM_ADDRA_WIDTH.W))
        val douta = Output(UInt(8.W))
        // val dina  = Input(UInt(8.W))

        // val enb   = Input(Bool())
        val web   = Input(Bool())
        val addrb = Input(UInt(VRAM_ADDRB_WIDTH.W))
        val dinb  = Input(UInt(8.W))
        // val doutb = Output(UInt(8.W))
    })
}
