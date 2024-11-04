package mip.sys

import chisel3._
import chisel3.util._
import __global__.AXI4InterfaceM
import mip.xilinx._
import mip.units._
import mip.MipConfigs.N_MIP_CHANNELS

class MipSystem extends Module {
    val io = IO(new Bundle {
        // axi datamover command and axis data channel
        val m_axis_mm2s_cmd = new datamover_m_axis_mm2s_cmd()
        val s_axis_mm2s     = new datamover_s_axis_mm2s()
        // control register port with AXI-BRAM-Control IP
        val ctrlreg_port = Flipped(new brama_gen_port(32))
        val vram_port    = Flipped(new brama_gen_port(64))
    })

    val ctrlReg = Module(new CtrlReg())

    val dataFetcher = Module(new MipDataFetcher())
    val calcUnit    = Seq.fill(N_MIP_CHANNELS)(Module(new MipCalcChannel()))
    val vram        = Module(new MipVram())

}
