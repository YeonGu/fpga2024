package cache.mip

import chisel3._
import chisel3.util._

import __global__._
import __global__.GeneratedParams._
import __global__.Params.DENS_DEPTH
import __global__.Params.MIP_CACHE_BLOCK_SIZE

class CacheDecode extends Module {}

class WbCacheLookupPort extends Bundle {
    val screenAddr = Output(UInt(SCREEN_ADDR_WIDTH.W))
    val wbValid    = Output(Bool())
    val wbReady    = Input(Bool())
    val wbData     = Input(UInt(DENS_DEPTH.W)) /* A few clocks delayed data */
}

class WbWrCmd extends Bundle {
    val vramAddr = Output(UInt(SCREEN_ADDR_WIDTH.W))
    val density  = Output(UInt(DENS_DEPTH.W))
}

class screenAddrDecode extends Module {
    val io = IO(new Bundle {
        val screenAddr  = Input(UInt(SCREEN_ADDR_WIDTH.W))
        val tag         = Output(UInt(MIP_CACHE_TAG_WIDTH.W))
        val group       = Output(UInt(MIP_CACHE_GROUP_WIDTH.W))
        val blockOffset = Output(UInt(log2Ceil(MIP_CACHE_BLOCK_SIZE).W))
    })

    io.tag := io.screenAddr(
        SCREEN_ADDR_WIDTH - 1,
        MIP_CACHE_GROUP_WIDTH + log2Ceil(MIP_CACHE_BLOCK_SIZE)
    )
    io.group := io.screenAddr(
        SCREEN_ADDR_WIDTH - MIP_CACHE_GROUP_WIDTH - 1,
        log2Ceil(MIP_CACHE_BLOCK_SIZE)
    )
    io.blockOffset := io.screenAddr(log2Ceil(MIP_CACHE_BLOCK_SIZE) - 1, 0)
}
