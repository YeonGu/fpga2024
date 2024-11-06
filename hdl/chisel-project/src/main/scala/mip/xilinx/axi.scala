package mip.xilinx

import chisel3._
import chisel3.util._
import mip.MipConfigs._
import dataclass.data

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

class datamover_s_axis_mm2s_sts extends Bundle {
    val tdata  = Input(UInt(8.W))
    val tvalid = Input(Bool())
    val tready = Output(Bool())
    val tkeep  = Input(UInt(1.W))
    val tlast  = Input(Bool())
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
    val addra = Input(UInt(data_width.W))
    val dina  = Input(UInt(data_width.W))
    val douta = Output(UInt(data_width.W))
    val ena   = Input(Bool())
    // val wea   = Input(Bool())
    val wea = Input(UInt((data_width / 8).W))
}

class xlnx_axi_bram_ctrl_port(BRAM_ADDR_WIDTH: Int, DATA_WIDTH: Int) extends Bundle {
    val bram_addr_a   = Output(UInt(BRAM_ADDR_WIDTH.W))
    val bram_clk_a    = Output(Clock())
    val bram_wrdata_a = Output(UInt(DATA_WIDTH.W))
    val bram_rddata_a = Input(UInt(DATA_WIDTH.W))
    val bram_en_a     = Output(Bool())
    val bram_we_a     = Output(UInt((DATA_WIDTH / 8).W))
    val bram_rst_a    = Output(Bool())
}
