package mip.xilinx

import chisel3._
import chisel3.util._
import mip.MipConfigs._

/** BlackBox wrapper for a Xilinx FIFO primitive implementing a processor queue.
  *
  * This FIFO has different read and write data widths, implementing width adaptation. It provides standard
  * FIFO control signals as well as read data count monitoring.
  *
  * Port descriptions:
  *   - din: Write data input (PROC_QUEUE_WR_WIDTH = 256 bits wide)
  *   - dout: Read data output (PROC_QUEUE_RD_WIDTH = 32 bits wide)
  *   - rd_data_count: Number of readable words available
  *
  * Configurations:
  *   - Write Depth: 512
  *   - Read Depth: 4096
  */
class proc_queue_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk  = Input(Clock())
        val srst = Input(Bool())

        val wr_en = Input(Bool())
        val din   = Input(UInt(PROC_QUEUE_WR_WIDTH.W))
        val full  = Output(Bool())
        val rd_en = Input(Bool())
        val dout  = Output(UInt(PROC_QUEUE_RD_WIDTH.W))
        val empty = Output(Bool())

        val rd_data_count = Output(UInt((log2Ceil(PROC_QUEUE_RD_DEPTH) + 1).W))
        val valid         = Output(Bool())

        val wr_rst_busy = Output(Bool())
        val rd_rst_busy = Output(Bool())
    })
}

class mip_dispatch_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk  = Input(Clock())
        val srst = Input(Bool())

        val wr_en = Input(Bool())
        val din   = Input(UInt(MIP_AXIS_MM2S_WIDTH.W)) // 128 from AXIS MM2S
        val full  = Output(Bool())

        val rd_en = Input(Bool())
        val dout  = Output(UInt(PROC_QUEUE_WR_WIDTH.W)) // 256 to PROC_QUEUE
        val empty = Output(Bool())

        val valid = Output(Bool())
        val wr_ack = Output(Bool())
        // val data_count = Output(UInt(10.W))
        val wr_data_count = Output(UInt(12.W))
        val rd_data_count = Output(UInt(11.W))

        val wr_rst_busy = Output(Bool())
        val rd_rst_busy = Output(Bool())
    })
}

/** @config.
  *   first word fall through, latency = 0
  */
class result_cache_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk  = Input(Clock())
        val srst = Input(Bool())

        val wr_en         = Input(Bool())
        val din           = Input(UInt((32 * N_MIP_CORES).W))                  // might be 128
        val full          = Output(Bool())
        val rd_en         = Input(Bool())
        val dout          = Output(UInt(32.W))
        val empty         = Output(Bool())
        val wr_data_count = Output(UInt((log2Ceil(RES_CACHE_WR_DEPTH) + 1).W)) // 512 bits

        val wr_rst_busy = Output(Bool())
        val rd_rst_busy = Output(Bool())
    })
}

class calc_test_fifo extends BlackBox {
    val io = IO(new Bundle {
        val clk  = Input(Clock())
        val srst = Input(Bool())

        val wr_en         = Input(Bool())
        val din           = Input(UInt((32 * N_MIP_CORES).W))                  // might be 128
        val full          = Output(Bool())
        val rd_en         = Input(Bool())
        val dout          = Output(UInt(32.W))
        val empty         = Output(Bool())
        val wr_data_count = Output(UInt((log2Ceil(RES_CACHE_WR_DEPTH) + 1).W)) // 512
        val rd_data_count = Output(UInt((log2Ceil(RES_CACHE_RD_DEPTH) + 1).W)) // 2048
        val valid         = Output(Bool())

        val wr_rst_busy = Output(Bool())
        val rd_rst_busy = Output(Bool())
    })
}
