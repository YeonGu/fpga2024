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
    val aw = new Bundle {
        val addr  = Output(UInt(addrWidth.W))
        val prot  = Output(UInt(3.W))
        val valid = Output(Bool())
        val ready = Input(Bool())
    }

    // Write data channel
    val w = new Bundle {
        val data  = Output(UInt(dataWidth.W))
        val strb  = Output(UInt((dataWidth / 8).W))
        val valid = Output(Bool())
        val ready = Input(Bool())
    }

    // Write response channel
    val b = new Bundle {
        val resp  = Input(UInt(2.W))
        val valid = Input(Bool())
        val ready = Output(Bool())
    }

    // Read address channel
    val ar = new Bundle {
        val addr  = Output(UInt(addrWidth.W))
        val prot  = Output(UInt(3.W))
        val valid = Output(Bool())
        val ready = Input(Bool())
    }

    // Read data channel
    val r = new Bundle {
        val data  = Input(UInt(dataWidth.W))
        val resp  = Input(UInt(2.W))
        val valid = Input(Bool())
        val ready = Output(Bool())
    }
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
