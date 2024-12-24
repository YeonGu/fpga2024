package mip.units

import __global__.Params._
import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import mip.MipConfigs._
import mip.xilinx._

class FetcherCtrl extends Bundle {
    val start_valid = Input(Bool())
    val start_ready = Output(Bool())

    val dispatch_cnt = Output(UInt(32.W))
    val dispatch_end = Output(Bool())

    val cmd_send_cnt   = Output(UInt(32.W))
    val fetch_cnt      = Output(UInt(32.W))
    val cmd_send_state = Output(UInt(32.W))
    val stream_state   = Output(UInt(32.W))

    val dispatchFifoWrens    = Output(UInt(32.W))
    val dispatchFifoWracks   = Output(UInt(32.W))
    val dispatchFifoRdens    = Output(UInt(32.W))
    val dispatchFifoRdvalids = Output(UInt(32.W))
}

class MipDataFetcher extends Module {
    val io = IO(new Bundle {
        val ctrl = new FetcherCtrl() // fetcher control from mip system top
        /* Command & data <==> AXIS DataMover */
        val m_axis_mm2s_cmd = new datamover_m_axis_mm2s_cmd()
        val s_axis_mm2s     = new datamover_s_axis_mm2s()

        val mip_channels = Vec(N_MIP_CHANNELS, Flipped(new calcInput()))
    })

    /* Control FSM */
    /* cmd_sender and fetcher (system) state */
    // object FetcherState extends ChiselEnum {
    //     val IDLE, WORK = Value
    // }
    // object CmdSendState extends ChiselEnum {
    //     val IDLE, SEND, FINISH = Value
    // }
    // object StreamReceiverState extends ChiselEnum {
    //     val IDLE, RECV = Value
    // }

    object DataFetchState extends ChiselEnum {
        val IDLE, CMD, STREAM, WAIT = Value
    }

    // val fetcher_state    = RegInit(FetcherState.IDLE)
    // val cmd_sender_state  = RegInit(CmdSendState.IDLE)
    // val stream_recv_state = RegInit(StreamReceiverState.IDLE)

    val dispatch_fifo = Module(new mip_dispatch_fifo)

    // 512^3 takes 128MB
    // from 0x8_7800_0000 to 0x8_7FFF_FFFF
    val TEXTURE_ADDR_BASE     = "h8_7800_0000"
    val stream_cmd_start_addr = RegInit(TEXTURE_ADDR_BASE.U(49.W))

    val btt           = WireInit(8192.U(23.W))
    val TOTAL_CMD_CNT = VOXEL_COUNT / 8192

    // val VOXEL_STREAM_CNT = VOXEL_COUNT / (MIP_AXIS_MM2S_WIDTH / 8)
    val cmd_send_cnt          = RegInit(0.U(32.W))
    val total_stream_recv_cnt = RegInit(0.U(32.W))

    val datafetch_state = RegInit(DataFetchState.IDLE)

    switch(datafetch_state) {
        is(DataFetchState.IDLE) {
            stream_cmd_start_addr := TEXTURE_ADDR_BASE.U

            when(io.ctrl.start_valid) {
                total_stream_recv_cnt := 0.U
                cmd_send_cnt          := 0.U
                datafetch_state       := DataFetchState.CMD
            }
        }
        is(DataFetchState.CMD) {
            when(io.m_axis_mm2s_cmd.tvalid && io.m_axis_mm2s_cmd.tready) {
                datafetch_state       := DataFetchState.STREAM
                cmd_send_cnt          := cmd_send_cnt + 1.U
                stream_cmd_start_addr := stream_cmd_start_addr + btt
            }
        }
        is(DataFetchState.STREAM) {
            when(io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready) {
                total_stream_recv_cnt := total_stream_recv_cnt + 1.U
            }
            when(io.s_axis_mm2s.tlast && io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready) {
                datafetch_state := Mux(
                    cmd_send_cnt === TOTAL_CMD_CNT.U,
                    DataFetchState.IDLE,
                    DataFetchState.WAIT
                )
            }
        }
        is(DataFetchState.WAIT) {
            when(dispatch_fifo.io.rd_data_count < 512.U) {
                datafetch_state := DataFetchState.CMD
            }
        }
    }

    io.ctrl.start_ready   := datafetch_state === DataFetchState.IDLE
    io.s_axis_mm2s.tready := datafetch_state === DataFetchState.STREAM

    // val cmd_send_cnt = RegInit(0.U(32.W))
    io.ctrl.cmd_send_cnt := cmd_send_cnt
    io.ctrl.fetch_cnt    := total_stream_recv_cnt
    // io.ctrl.cmd_send_state := datafetch_state.asUInt
    io.ctrl.stream_state := datafetch_state.asUInt

    /* AXI Receiver Stream */

    /** AXI4 Stream Command ==> AXI DataMover MM2S
      *
      * | N+47	N+44 | N+43	N+40 | N+39	N+36 | N+35	N+32 | N+31	  32 | 31  | 30  | 29   24 | 23   | 22	0 |
      * |:----------|:----------|:----------|:----------|:----------|:----|:----|:--------|:-----|:-----|
      * | xUSER     | xCACHE    | RSVD      | TAG       | SADDR     | DRR | EOF | DSA     | Type | BTT  |
      */

    // datamover command. S2MM. Only need to send a bunch of commands to the datamover.
    // width: MM2S PS-AXIS 128bit
    // Receiver FIFO depth: 1024 (dispatch FIFO)
    // BTT: 512 * 16 = 8192
    // Enable read when FIFO count <= 512

    // val ADDR_INCREMENT = (MIP_AXIS_MM2S_WIDTH / 8).U

    val cmd = Wire(UInt(96.W))
    cmd := Cat(
        stream_cmd_start_addr(48, 0), // SADDR
        0.U(1.W),                     // DRR
        1.U(1.W),                     // EOF. assert tlast when BTT is reached
        0.U(6.W),                     // DSA
        1.U(1.W),                     // Type
        btt(22, 0)                    // BTT
    )
    io.m_axis_mm2s_cmd.tvalid := datafetch_state === DataFetchState.CMD
    io.m_axis_mm2s_cmd.tdata  := cmd

    // switch(cmd_sender_state) {
    //     is(CmdSendState.IDLE) { stream_cmd_start_addr := TEXTURE_ADDR_BASE.U }
    //     is(CmdSendState.SEND) {
    //         when(io.m_axis_mm2s_cmd.tvalid && io.m_axis_mm2s_cmd.tready) {
    //             stream_cmd_start_addr := stream_cmd_start_addr + btt
    //         }
    //     }
    // }
    // io.m_axis_mm2s_cmd.tvalid := cmd_sender_state === CmdSendState.SEND
    // io.m_axis_mm2s_cmd.tdata  := cmd

    /** AXI4 Status <== AXI DataMover S2MM
      */

    // AXIS DataMover MM2S
    // deassert tready when dispatch FIFO count > 512
    // deassert only happens when a command is completed in order to avoid BUS contention

    // val axis_ready = RegInit(false.B)
    // io.s_axis_mm2s.tready := axis_ready
    // switch(axis_ready) {
    //     is(true.B) {
    //         // Its okay to use 512 since btt once is 512 * 16 = 8192
    //         when(io.s_axis_mm2s.tlast && dispatch_fifo.io.wr_data_count > 512.U) {
    //             axis_ready := false.B
    //         }
    //     }
    //     is(false.B) {
    //         when(dispatch_fifo.io.wr_data_count <= 512.U) {
    //             axis_ready := true.B
    //         }
    //     }
    // }

    /** AXI DataMover MM2S ==> Data FIFO
      *
      * DDR MM2S data ==> dispatch FIFO (==> MIP Channels)
      */
    // ABOUT: dispatch FIFO
    // width: wr 128bit / rd 128bit
    // depth: 1024
    // read latency: 1

    dispatch_fifo.io.clk   := clock
    dispatch_fifo.io.srst  := reset.asBool || datafetch_state === DataFetchState.IDLE && io.ctrl.start_valid
    dispatch_fifo.io.wr_en := io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready
    dispatch_fifo.io.din   := io.s_axis_mm2s.tdata

    val dispFifoWracks   = RegInit(0.U(32.W))
    val dispFifoWrens    = RegInit(0.U(32.W))
    val dispFifoRdens    = RegInit(0.U(32.W))
    val dispFifoRdvalids = RegInit(0.U(32.W))

    io.ctrl.dispatchFifoWracks   := dispFifoWracks
    io.ctrl.dispatchFifoWrens    := dispFifoWrens
    io.ctrl.dispatchFifoRdens    := dispFifoRdens
    io.ctrl.dispatchFifoRdvalids := dispFifoRdvalids

    /** Data dispatch
      */
    val need_datas = io.mip_channels.map(_.need_data)
    val need       = need_datas.reduce(_ || _)

    object DispatchStates extends ChiselEnum {
        val IDLE, WRITE_ADDR, DISPATCH, DPT_DELAY, NEXT, END = Value
    }
    val dispatch_state   = RegInit(DispatchStates.IDLE)
    val dispatch_count   = RegInit(0.U(log2Ceil(WORKSET_WR_CNT).W))
    val dispatch_channel = RegInit(0.U(log2Ceil(N_MIP_CHANNELS).W))
    val voxel_addr       = RegInit(0.U(VOXEL_POS_XLEN.W))

    val total_dispatched    = RegInit(0.U(32.W))
    val single_dispatch_cnt = RegInit(0.U(32.W))

    val n_dispatch_total = VOXEL_COUNT / (PROC_QUEUE_WR_WIDTH / 8)

    io.ctrl.dispatch_cnt   := total_dispatched
    io.ctrl.cmd_send_state := datafetch_state.asUInt

    // note that dispatch fifo has 2 cycle read latency.
    // after dispatching, we need extra state DELAY #2 cycles to wait for dispatch completion.
    val dispatch_delay     = 2.U
    val dispatch_delay_cnt = RegInit(0.U(32.W))

    when(datafetch_state === DataFetchState.IDLE && io.ctrl.start_valid) {
        dispatch_state   := DispatchStates.IDLE
        voxel_addr       := 0.U
        dispFifoWracks   := 0.U
        dispFifoWrens    := 0.U
        dispFifoRdens    := 0.U
        dispFifoRdvalids := 0.U
    }.otherwise {
        switch(dispatch_state) {
            is(DispatchStates.IDLE) {
                when(need && dispatch_fifo.io.rd_data_count >= (WORKSET_WR_CNT).U) {
                    dispatch_state   := DispatchStates.WRITE_ADDR
                    dispatch_channel := PriorityEncoder(need_datas)
                }
                total_dispatched := 0.U
            }
            is(DispatchStates.WRITE_ADDR) {
                // set dispatching ios. io.dispatch(dispatch_channel).addr_reg := dispatch_count
                dispatch_state := DispatchStates.DISPATCH
            }
            is(DispatchStates.DISPATCH) {
                when(dispatch_count === (WORKSET_WR_CNT - 1).U) {
                    dispatch_state := DispatchStates.DPT_DELAY
                    voxel_addr     := voxel_addr + WORKSET_WR_CNT.U * (PROC_QUEUE_WR_WIDTH / 8).U
                }
                dispatch_delay_cnt := 0.U
            }
            is(DispatchStates.DPT_DELAY) {
                dispatch_delay_cnt := dispatch_delay_cnt + 1.U
                when(dispatch_delay_cnt === (dispatch_delay - 1.U)) {
                    dispatch_state := DispatchStates.NEXT
                }
            }
            is(DispatchStates.NEXT) {
                when(need && dispatch_fifo.io.rd_data_count >= (WORKSET_WR_CNT).U) {
                    dispatch_state   := DispatchStates.WRITE_ADDR
                    dispatch_channel := PriorityEncoder(need_datas)
                    total_dispatched := total_dispatched + 1.U
                }
                dispatch_delay_cnt := 0.U
            }
        }
        when(dispatch_fifo.io.wr_en) {
            dispFifoWrens := dispFifoWrens + 1.U
        }
        when(dispatch_fifo.io.wr_ack) {
            dispFifoWracks := dispFifoWracks + 1.U
        }
        when(dispatch_fifo.io.rd_en) {
            dispFifoRdens := dispFifoRdens + 1.U
        }
        when(dispatch_fifo.io.valid) {
            dispFifoRdvalids := dispFifoRdvalids + 1.U
        }
    }

    dispatch_fifo.io.rd_en := dispatch_state === DispatchStates.DISPATCH
    dispatch_count := Mux(
        dispatch_state === DispatchStates.DISPATCH,
        dispatch_count + 1.U,
        0.U
    )
    io.ctrl.dispatch_end := dispatch_state === DispatchStates.IDLE

    // addr reg wren
    io.mip_channels.zipWithIndex.foreach { case (c, i) =>
        c.voxel_addr_reg_wren := (dispatch_state === DispatchStates.WRITE_ADDR) && (dispatch_channel === i.U)
    }
    io.mip_channels.foreach { _.voxel_addr_reg_wrdata := voxel_addr }

    // proc queue
    io.mip_channels.zipWithIndex.foreach { case (c, i) =>
        c.proc_queue_wren := dispatch_fifo.io.valid && (dispatch_channel === i.U)
    }
    io.mip_channels.foreach(_.proc_queue_wrdata := dispatch_fifo.io.dout)
}

object Dfter extends App {
    ChiselStage.emitSystemVerilogFile(
        new MipDataFetcher(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build/mods")
    )
}
