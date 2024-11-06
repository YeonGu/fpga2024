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
        val s_axis_mm2s_sts = new datamover_s_axis_mm2s_sts()
        // control register port with AXI-BRAM-Control IP
        val ctrlreg_port = new brama_gen_port(32)
        val vram_port    = new brama_gen_port(64)
    })

    val ctrlReg = Module(new CtrlReg())

    val dataFetcher = Module(new MipDataFetcher())
    val calcUnit    = Seq.fill(N_MIP_CHANNELS)(Module(new MipCalcChannel()))
    val vram        = Module(new MipVram())

    // TODO: fetcher control ports
    io.m_axis_mm2s_cmd <> dataFetcher.io.m_axis_mm2s_cmd
    io.s_axis_mm2s <> dataFetcher.io.s_axis_mm2s

    // TODO: datamover status
    io.s_axis_mm2s_sts.tready := true.B

    val cmdStart = ctrlReg.io.start
    ctrlReg.io.dispatch_cnt := dataFetcher.io.ctrl.dispatch_cnt
    ctrlReg.io.ctrlreg_port <> io.ctrlreg_port

    dataFetcher.io.ctrl.start_valid := cmdStart

    // Datafetcher -> CUs
    dataFetcher.io.mip_channels.zip(calcUnit).foreach { case (df, cu) => df <> cu.in }

    // CU control
    calcUnit.foreach { _.mipCtrl.mvpInfo := ctrlReg.io.mvp_info }
    calcUnit.foreach { _.mipCtrl.baseCoord := ctrlReg.io.base_coord }
    calcUnit.foreach { _.mipCtrl.startCmd := cmdStart }
    // CU channels count
    ctrlReg.io.channels_cnt := calcUnit.map(_.mipCtrl.calcCount)

    // CUs -> VRAM
    calcUnit.zip(vram.io.calc_res).foreach { case (cu, vramin) => cu.out <> vramin }

    vram.io.en_minip := false.B
    vram.io.ram_port <> io.vram_port
    vram.io.ram_reset := cmdStart

    // TODO: lastFinished signal
    ctrlReg.io.last_finished :=
        dataFetcher.io.ctrl.dispatch_end &&
            calcUnit.map(_.mipCtrl.queueEmpty).reduce(_ && _)
}
