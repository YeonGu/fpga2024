package mip.xilinx

import chisel3._
import chisel3.util._
import mip.MipConfigs._

/* AXI Datamover MM2S (Memory READ --> DATAMOVER) */

// AXI4S command channel
class datamover_m_axis_mm2s_cmd extends Bundle {
    val tdata  = Output(UInt(72.W))
    val tvalid = Output(Bool())
    val tready = Input(Bool())
}

// AXI4S data channel
class datamover_s_axis_mm2s extends Bundle {
    val tdata  = Input(UInt(MIP_AXIS_MM2S_WIDTH.W))
    val tkeep  = Input(UInt((MIP_AXIS_MM2S_WIDTH / 8).W))
    val tlast  = Input(Bool())
    val tvalid = Input(Bool())
    val tready = Output(Bool())
}

/* S2MM (AXIS DataMover ==> Memory Map) */

// AXI4S command channel
class datamover_m_axis_s2mm_cmd extends Bundle {
    val tdata  = Output(UInt(72.W))
    val tvalid = Output(Bool())
    val tready = Input(Bool())
}
class datamover_m_axis_s2mm extends Bundle {
    val tdata  = Output(UInt(MIP_AXIS_S2MM_WIDTH.W))
    val tkeep  = Output(UInt((MIP_AXIS_S2MM_WIDTH / 8).W))
    val tlast  = Output(Bool())
    val tvalid = Output(Bool())
    val tready = Input(Bool())
}

// BRAM PORT
class brama_gen_port(data_width: Int) extends Bundle {
    val addra = Input(UInt(32.W))
    val dina  = Input(UInt(32.W))
    val douta = Output(UInt(32.W))
    val ena   = Input(Bool())
    val wea   = Input(Bool())
}
