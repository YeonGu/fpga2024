package mip.sys

import chisel3._
import chisel3.util._
import __global__.Mat3x4
import mip.xilinx._
import __global__.Params._

// See spec in README.md
class CtrlReg extends Module {
    val io = IO(new Bundle {
        val ctrlreg_port = Flipped(new brama_gen_port(32))
        val mvp_info     = Output(new Mat3x4())
        val base_coord   = Output(Vec(3, SInt(BASE_POS_XLEN.W)))
        val start        = Output(Bool())
        // status monitor input
        val dispatch_cnt = Input(UInt(32.W))
        val channel_cnt  = Input(UInt(32.W))
    })
}
