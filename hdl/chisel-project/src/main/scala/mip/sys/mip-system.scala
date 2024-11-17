package mip.sys

import chisel3._
import chisel3.util._
import __global__.AXI4InterfaceM
import mip.xilinx._
import mip.units._
import mip.MipConfigs.N_MIP_CHANNELS
import scribe.ANSI.ctrl

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
    val sts_reg = RegInit(0.U(8.W))
    sts_reg := Mux(io.s_axis_mm2s_sts.tvalid, io.s_axis_mm2s_sts.tdata, sts_reg)

    val cmdStart = ctrlReg.io.start
    ctrlReg.io.dispatch_cnt := dataFetcher.io.ctrl.dispatch_cnt
    ctrlReg.io.ctrlreg_port <> io.ctrlreg_port

    dataFetcher.io.ctrl.start_valid := cmdStart
    ctrlReg.io.cmd_send_cnt         := dataFetcher.io.ctrl.cmd_send_cnt
    ctrlReg.io.fetch_cnt            := dataFetcher.io.ctrl.fetch_cnt
    ctrlReg.io.cmd_send_state       := dataFetcher.io.ctrl.cmd_send_state
    ctrlReg.io.stream_state         := dataFetcher.io.ctrl.stream_state
    ctrlReg.io.last_sts             := sts_reg

    // Datafetcher -> CUs
    dataFetcher.io.mip_channels.zip(calcUnit).foreach { case (df, cu) => df <> cu.in }

    // CU control
    calcUnit.foreach { _.mipCtrl.mvpInfo := ctrlReg.io.mvp_info }
    calcUnit.foreach { _.mipCtrl.baseCoord := ctrlReg.io.base_coord }
    calcUnit.foreach { _.mipCtrl.startCmd := cmdStart }
    // CU channels count
    ctrlReg.io.channels_cnt := calcUnit.map(_.mipCtrl.calcCount)

    calcUnit.zipWithIndex.foreach { case (cu, i) => ctrlReg.io.calc_cnt(i) := cu.mipCtrl.calcCount }
    calcUnit.zipWithIndex.foreach { case (cu, i) => ctrlReg.io.calc_res_valid_cnt(i) := cu.mipCtrl.validCnt }

    // CUs -> VRAM
    calcUnit.zip(vram.io.calc_res).foreach { case (cu, vramin) => cu.out <> vramin }

    vram.io.en_minip := false.B
    vram.io.ram_port <> io.vram_port
    vram.io.ram_reset := cmdStart

    vram.io.start_cmd   := cmdStart
    ctrlReg.io.read_cnt := vram.io.read_cnt
    ctrlReg.io.wr_cnt   := vram.io.wr_cnt
    ctrlReg.io.wr_max   := vram.io.wr_max

    // TODO: lastFinished signal
    ctrlReg.io.last_finished :=
        dataFetcher.io.ctrl.dispatch_end &&
            calcUnit.map(_.mipCtrl.queueEmpty).reduce(_ && _)
}
