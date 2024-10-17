package __global__

import chisel3._
import chisel3.util.log2Up
import __global__.Params._
import rendering.FloatPoint

class ScreenPos extends Bundle {
    val x = UInt(log2Up(SCREEN_H).W)
    val y = UInt(log2Up(SCREEN_V).W)
}

class VoxelVector3 extends Bundle {
    val x = UInt(VOXEL_POS_XLEN.W)
    val y = UInt(VOXEL_POS_XLEN.W)
    val z = UInt(VOXEL_POS_XLEN.W)
}

/* 3x3 MVP Matrix with elements whose bitdepth is MVP_MAT_BITDEPTH */
class Mat3x4 extends Bundle {
    val mat = Vec(3, Vec(4, new FloatPoint()))
}

import chisel3._
import chisel3.util._

/** AXI4 interface definition using Decoupled */
class AXI4InterfaceM(addrWidth: Int, dataWidth: Int) extends Bundle {
    // Write address channel
    val awaddr  = Output(UInt(addrWidth.W))
    val awprot  = Output(UInt(3.W))
    val awvalid = Output(Bool())
    val awready = Input(Bool())

    // Write data channel
    val wdata  = Output(UInt(dataWidth.W))
    val wstrb  = Output(UInt((dataWidth / 8).W))
    val wvalid = Output(Bool())
    val wready = Input(Bool())

    // Write response channel
    val bresp  = Input(UInt(2.W))
    val bvalid = Input(Bool())
    val bready = Output(Bool())

    // Read address channel
    val araddr  = Output(UInt(addrWidth.W))
    val arprot  = Output(UInt(3.W))
    val arvalid = Output(Bool())
    val arready = Input(Bool())

    // Read data channel
    val rdata  = Input(UInt(dataWidth.W))
    val rresp  = Input(UInt(2.W))
    val rvalid = Input(Bool())
    val rready = Output(Bool())
}

/* Xilinx FIFO IP IO wrapping. From the perspective of FIFO IP. */
/* TODO: Check the ports. */
class XilinxFIFOIO(dataWidth: Int, depth: Int) extends Bundle {
    val din          = Input(UInt(dataWidth.W))
    val wr_en        = Input(Bool())
    val rd_en        = Input(Bool())
    val dout         = Output(UInt(dataWidth.W))
    val full         = Output(Bool())
    val empty        = Output(Bool())
    val almost_full  = Output(Bool())
    val almost_empty = Output(Bool())
    val wr_count     = Output(UInt(log2Up(depth + 1).W))
    val rd_count     = Output(UInt(log2Up(depth + 1).W))
}
