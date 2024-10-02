package __global__

import chisel3._
import chisel3.util.log2Up
import __global__.Params._

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
class Mat3x3 extends Bundle {
    val mat = Vec(3, Vec(3, UInt(MVP_MAT_BITDEPTH.W)))
}

import chisel3._
import chisel3.util._

/** AXI4 interface definition using Decoupled */
class AXI4Interface(addrWidth: Int, dataWidth: Int) extends Bundle {
    // Write address channel
    val writeAddr = Decoupled(new Bundle {
        val addr = UInt(addrWidth.W)
        val prot = UInt(3.W)
    })

    // Write data channel
    val writeData = Decoupled(new Bundle {
        val data = UInt(dataWidth.W)
        val strb = UInt((dataWidth / 8).W)
    })

    // Write response channel
    val writeResp = Flipped(Decoupled(new Bundle {
        val resp = UInt(2.W)
    }))

    // Read address channel
    val readAddr = Decoupled(new Bundle {
        val addr = UInt(addrWidth.W)
        val prot = UInt(3.W)
    })

    // Read data channel
    val readData = Flipped(Decoupled(new Bundle {
        val data = UInt(dataWidth.W)
        val resp = UInt(2.W)
    }))
}