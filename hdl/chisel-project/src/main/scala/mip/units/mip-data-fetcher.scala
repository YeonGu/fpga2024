package mip.units

import chisel3._
import chisel3.util._
import __global__.AXI4InterfaceM
import __global__.Params._
import mip.MipConfigs._
import mip.xilinx._

class FetcherCtrl extends Bundle {
    val start_valid = Input(Bool())
    val start_ready = Output(Bool())
    // val done        = Output(Bool())
}

class MipDataFetcher extends Module {
    val io = IO(new Bundle {
        val ctrl = new FetcherCtrl() // fetcher control from mip system top
        /* Command & data <==> AXIS DataMover */
        val m_axis_mm2s_cmd = new datamover_m_axis_mm2s_cmd()
        val s_axis_mm2s     = new datamover_s_axis_mm2s()

        val mip_channels = Vec(N_MIP_CHANNELS, Flipped(new calcChannelIn()))
    })

    /* Control FSM */
    /* cmd_sender and fetcher (system) state */
    // object FetcherState extends ChiselEnum {
    //     val IDLE, WORK = Value
    // }
    object CmdSendState extends ChiselEnum {
        val IDLE, SEND, FINISH = Value
    }
    object StreamReceiverState extends ChiselEnum {
        val IDLE, RECV = Value
    }

    // val fetcher_state    = RegInit(FetcherState.IDLE)
    val cmd_sender_state  = RegInit(CmdSendState.IDLE)
    val stream_recv_state = RegInit(StreamReceiverState.IDLE)

    val TEXTURE_ADDR_BASE     = 0
    val stream_cmd_start_addr = RegInit(TEXTURE_ADDR_BASE.U(32.W))
    val btt                   = WireInit(8192.U(23.W))

    val VOXEL_STREAM_CNT = VOXEL_COUNT / (MIP_AXIS_MM2S_WIDTH / 8)
    val stream_recv_cnt  = RegInit(0.U(32.W))

    io.ctrl.start_ready := cmd_sender_state === CmdSendState.IDLE

    // switch(fetcher_state) {
    //     is(FetcherState.IDLE) {
    //         when(io.ctrl.start_valid && io.ctrl.start_ready) {
    //             fetcher_state := FetcherState.WORK
    //         }
    //     }
    //     is(FetcherState.WORK) {
    //         when(cmd_sender_state === CmdSendState.FINISH) {
    //             fetcher_state := FetcherState.IDLE
    //         }
    //     }
    // }

    switch(cmd_sender_state) {
        is(CmdSendState.IDLE) {
            when(io.ctrl.start_valid && io.ctrl.start_ready) { // FIXME: check other components
                cmd_sender_state := CmdSendState.SEND
            }
        }
        is(CmdSendState.SEND) {
            when(
                (stream_cmd_start_addr === (VOXEL_COUNT.U - btt)) && io.m_axis_mm2s_cmd.tvalid && io.m_axis_mm2s_cmd.tready
            ) {
                cmd_sender_state := CmdSendState.FINISH
            }
        }
        is(CmdSendState.FINISH) { cmd_sender_state := CmdSendState.IDLE }
    }
    switch(stream_recv_state) {
        is(StreamReceiverState.IDLE) {
            when(io.ctrl.start_valid && io.ctrl.start_ready) {
                stream_recv_state := StreamReceiverState.RECV
            }
            stream_recv_cnt := 0.U
        }
        is(StreamReceiverState.RECV) {
            when(
                io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready && (stream_recv_cnt === (VOXEL_STREAM_CNT - 1).U)
            ) {
                stream_recv_state := StreamReceiverState.IDLE
            }

            stream_recv_cnt := Mux(
                io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready,
                stream_recv_cnt + 1.U,
                stream_recv_cnt
            )
        }
    }

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

    val cmd = Wire(UInt(72.W))
    cmd := Cat(
        stream_cmd_start_addr(31, 0), // SADDR
        0.U(1.W),                     // DRR
        1.U(1.W),                     // EOF. assert tlast when BTT is reached
        0.U(6.W),                     // DSA
        1.U(1.W),                     // Type
        btt(22, 0)                    // BTT
    )

    switch(cmd_sender_state) {
        is(CmdSendState.IDLE) { stream_cmd_start_addr := TEXTURE_ADDR_BASE.U }
        is(CmdSendState.SEND) {
            when(io.m_axis_mm2s_cmd.tvalid && io.m_axis_mm2s_cmd.tready) {
                stream_cmd_start_addr := stream_cmd_start_addr + btt
            }
        }
    }
    io.m_axis_mm2s_cmd.tvalid := cmd_sender_state === CmdSendState.SEND
    io.m_axis_mm2s_cmd.tdata  := cmd

    /** AXI4 Status <== AXI DataMover S2MM
      */

    // AXIS DataMover MM2S
    // deassert tready when dispatch FIFO count > 512
    // deassert only happens when a command is completed in order to avoid BUS contention
    val dispatch_fifo = Module(new mip_dispatch_fifo())

    val axis_ready = RegInit(false.B)
    io.s_axis_mm2s.tready := axis_ready
    switch(axis_ready) {
        is(true.B) {
            when(io.s_axis_mm2s.tlast && dispatch_fifo.io.data_count > 512.U) {
                axis_ready := false.B
            }
        }
        is(false.B) {
            when(dispatch_fifo.io.data_count <= 512.U) {
                axis_ready := true.B
            }
        }
    }

    // when(dispatch_fifo.io.almost_empty) { axis_ready := true.B }

    // io.s_axis_mm2s.tready := axis_ready // (fetcher_state === FetcherState.WORK) && (!dispatch_fifo.io.almost_full) FIXME:

    /** AXI DataMover MM2S ==> Data FIFO
      *
      * DDR MM2S data ==> dispatch FIFO (==> MIP Channels)
      */
    // ABOUT: dispatch FIFO
    // width: wr 128bit / rd 128bit
    // depth: 1024
    // read latency: 1

    dispatch_fifo.io.clk     := clock
    dispatch_fifo.io.srst    := reset
    dispatch_fifo.io.wr_en   := io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready
    dispatch_fifo.io.wr_data := io.s_axis_mm2s.tdata

    /** Data dispatch
      */
    val need_datas = io.mip_channels.map(_.need_data)
    val need       = need_datas.reduce(_ || _)

    object DispatchStates extends ChiselEnum {
        val IDLE, WRITE_ADDR, DISPATCH, NEXT, END = Value
    }
    val dispatch_state   = RegInit(DispatchStates.IDLE)
    val dispatch_count   = RegInit(0.U(log2Ceil(WORKSET_WR_DEPTH).W))
    val dispatch_channel = RegInit(0.U(log2Ceil(N_MIP_CHANNELS).W))
    val voxel_addr       = RegInit(0.U(VOXEL_POS_XLEN.W))
    val total_dispatched = RegInit(0.U(32.W))

    val n_dispatch_total = VOXEL_COUNT / (PROC_QUEUE_WR_WIDTH / 8)

    switch(dispatch_state) {
        is(DispatchStates.IDLE) {
            when(
                need && dispatch_fifo.io.data_count >= (WORKSET_WR_DEPTH).U // FIXME: parameterize this
            ) {
                dispatch_state   := DispatchStates.WRITE_ADDR
                dispatch_channel := PriorityEncoder(need_datas)
            }
        }
        is(DispatchStates.WRITE_ADDR) {
            // set dispatching ios. io.dispatch(dispatch_channel).addr_reg := dispatch_count
            dispatch_state := DispatchStates.DISPATCH
        }
        is(DispatchStates.DISPATCH) {
            when(dispatch_count === (WORKSET_WR_DEPTH - 1).U) { // FIXME: dispatch count -> 0
                // dispatch_state := DispatchStates.IDLE
                dispatch_state := Mux(
                    total_dispatched === (n_dispatch_total - 1).U,
                    DispatchStates.IDLE,
                    DispatchStates.NEXT
                )
                voxel_addr := voxel_addr + WORKSET_WR_DEPTH.U * (PROC_QUEUE_WR_WIDTH / 8).U
            }
        }
        is(DispatchStates.NEXT) {
            when(need && dispatch_fifo.io.data_count >= (WORKSET_WR_DEPTH).U) {
                dispatch_state   := DispatchStates.WRITE_ADDR
                dispatch_channel := PriorityEncoder(need_datas)
                total_dispatched := total_dispatched + 1.U
            }
        }
    }

    dispatch_fifo.io.rd_en := dispatch_state === DispatchStates.DISPATCH
    dispatch_count := Mux(
        dispatch_state === DispatchStates.DISPATCH,
        dispatch_count + 1.U,
        0.U
    )

    // addr reg wren
    io.mip_channels.zipWithIndex.foreach { case (c, i) =>
        c.voxel_addr_reg_wren := (dispatch_state === DispatchStates.WRITE_ADDR) && (dispatch_channel === i.U)
    }
    io.mip_channels.foreach { _.voxel_addr_reg_wrdata := voxel_addr }

    // proc queue
    io.mip_channels.zipWithIndex.foreach { case (c, i) =>
        c.proc_queue_wren := RegNext(
            (dispatch_state === DispatchStates.DISPATCH) && (dispatch_channel === i.U)
        )
    }
    io.mip_channels.foreach(_.proc_queue_wrdata := dispatch_fifo.io.rd_data)
}
