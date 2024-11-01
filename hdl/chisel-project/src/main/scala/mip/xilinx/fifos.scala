package mip.xilinx

import chisel3._
import chisel3.util._
import mip.MipConfigs.PROC_QUEUE_WR_WIDTH
import __global__.Params.DENS_DEPTH
import mip.MipConfigs.N_MIP_CORES
import mip.MipConfigs.MIP_AXIS_MM2S_WIDTH
import mip.MipConfigs.PROC_QUEUE_RD_WIDTH
import mip.MipConfigs.PROC_QUEUE_RD_DEPTH
import mip.MipConfigs.RES_CACHE_WR_DEPTH

class proc_queue_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk           = Input(Clock())
        val rst           = Input(Bool())
        val wr_en         = Input(Bool())
        val wr_data       = Input(UInt(PROC_QUEUE_WR_WIDTH.W))
        val rd_en         = Input(Bool())
        val rd_data       = Output(UInt(PROC_QUEUE_RD_WIDTH.W))
        val rd_data_count = Output(UInt(log2Ceil(PROC_QUEUE_RD_DEPTH).W))
        val valid         = Output(Bool())
        val full          = Output(Bool())
        val empty         = Output(Bool())
        val almost_empty  = Output(Bool())
    })
}

class mip_dispatch_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk  = Input(Clock())
        val srst = Input(Bool())

        val wr_en   = Input(Bool())
        val wr_data = Input(UInt(MIP_AXIS_MM2S_WIDTH.W)) // 128 from AXIS MM2S
        val full    = Output(Bool())

        val rd_en   = Input(Bool())
        val rd_data = Output(UInt(PROC_QUEUE_WR_WIDTH.W)) // 128 to PROC_QUEUE
        val empty   = Output(Bool())

        val data_count = Output(UInt(10.W))
    })
}

/** @config.
  *   first word fall through, latency = 0
  */
class result_cache_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk           = Input(Clock())
        val rst           = Input(Bool())
        val wr_en         = Input(Bool())
        val wr_data       = Input(UInt((32 * N_MIP_CORES).W)) // might be 128
        val wr_data_count = Output(UInt(log2Ceil(RES_CACHE_WR_DEPTH).W))
        val rd_en         = Input(Bool())
        val rd_data       = Output(UInt(32.W))
        val valid         = Output(Bool())
        val full          = Output(Bool())
        val empty         = Output(Bool())
        // val almost_empty = Output(Bool())
        val almost_full = Output(Bool())
    })
}
