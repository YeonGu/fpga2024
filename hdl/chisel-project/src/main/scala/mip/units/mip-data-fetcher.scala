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
    val done        = Output(Bool())
}

class MipDataFetcher extends Module {
    val io = IO(new Bundle {
        val ctrl = new FetcherCtrl() // fetcher control from mip system top
        /* Command & data <==> AXIS DataMover */
        val m_axis_mm2s_cmd = new datamover_m_axis_mm2s_cmd()
        val s_axis_mm2s     = new datamover_s_axis_mm2s()

        val mip_channels = Vec(N_MIP_CHANNELS, Flipped(new calcChannelIn()))
    })

    val BASE_ADDR      = 0
    val ADDR_INCREMENT = (MIP_AXIS_MM2S_WIDTH / 8).U
    val byte_to_read   = 1.U << 22; // max 23bits for BTT field in command

    /* Control FSM */
    val fetcher_state = RegInit(FetcherState.IDLE)
    object FetcherState extends ChiselEnum {
        val IDLE, WORK = Value
    }

    /* AXI Read */

    /** AXI4 Stream Command ==> AXI DataMover MM2S
      *
      * | N+47	N+44 | N+43	N+40 | N+39	N+36 | N+35	N+32 | N+31	  32 | 31  | 30  | 29 	24 | 23   | 22	0 |
      * |:----------|:----------|:----------|:----------|:----------|:----|:----|:-------|:-----|:-----|
      * | xUSER     | xCACHE    | RSVD      | TAG       | SADDR     | DRR | EOF | DSA    | Type | BTT  |
      */
    object CmdSendState extends ChiselEnum {
        val IDLE, SEND, FINISH = Value
    }
    val sender_state     = RegInit(CmdSendState.IDLE)
    val axi_transfer_cnt = RegInit(0.U(VOXEL_POS_XLEN.W))
    val start_address    = RegInit(BASE_ADDR.U(32.W))

    val cmd = Wire(UInt(72.W))
    cmd := Cat(
        start_address(32, 0), // SADDR
        0.U(1.W),             // DRR
        0.U(1.W),             // EOF
        0.U(6.W),             // DSA
        1.U(1.W),             // Type
        byte_to_read(22, 0)   // BTT
    )

    switch(sender_state) {
        is(CmdSendState.IDLE) {
            when(fetcher_state === FetcherState.WORK) { sender_state := CmdSendState.SEND }
        }
        is(CmdSendState.SEND) {
            when(
                (start_address === (VOXEL_COUNT.U - byte_to_read)) && io.m_axis_mm2s_cmd.tvalid && io.m_axis_mm2s_cmd.tready
            ) {
                sender_state := CmdSendState.FINISH
            }
        }
        is(CmdSendState.FINISH) {
            when(io.ctrl.done) { sender_state := CmdSendState.IDLE }
        }
    }

    start_address := Mux(
        sender_state === CmdSendState.SEND && io.m_axis_mm2s_cmd.tvalid && io.m_axis_mm2s_cmd.tready,
        start_address + ADDR_INCREMENT,
        start_address
    )
    io.m_axis_mm2s_cmd.tvalid := sender_state === CmdSendState.SEND
    io.m_axis_mm2s_cmd.tdata  := cmd

    /** AXI4 Status <== AXI DataMover S2MM
      */

    /** AXI4 Stream Data <=== DataMover S2MM
      *
      * data ==> dispatch FIFO (==> MIP Channels)
      */
    val dispatch_fifo = Module(new mip_dispatch_fifo())
    val axis_ready    = RegInit(false.B)
    when(dispatch_fifo.io.almost_full) { axis_ready := false.B }
    when(dispatch_fifo.io.almost_empty) { axis_ready := true.B }

    io.s_axis_mm2s.tready := axis_ready // (fetcher_state === FetcherState.WORK) && (!dispatch_fifo.io.almost_full)
    dispatch_fifo.io.wr_en   := io.s_axis_mm2s.tvalid && io.s_axis_mm2s.tready
    dispatch_fifo.io.wr_data := io.s_axis_mm2s.tdata

    /** Data dispatch
      */
    val need_datas = io.mip_channels.map(_.need_data)
    val need       = need_datas.reduce(_ || _)

    object DispatchStates extends ChiselEnum {
        val IDLE, WRITE_ADDR, DISPATCH = Value
    }
    val dispatch_state   = RegInit(DispatchStates.IDLE)
    val dispatch_count   = RegInit(0.U(log2Ceil(WORKSET_SIZE / PROC_QUEUE_WR_WIDTH - 1).W))
    val dispatch_channel = RegInit(0.U(log2Ceil(N_MIP_CHANNELS).W))
    val voxel_addr       = RegInit(0.U(VOXEL_POS_XLEN.W))

    switch(dispatch_state) {
        is(DispatchStates.IDLE) {
            when(
                need && dispatch_fifo.io.rd_data_count >= (WORKSET_SIZE / PROC_QUEUE_WR_WIDTH).U
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
            when(dispatch_count === (WORKSET_SIZE / PROC_QUEUE_WR_WIDTH - 1).U) {
                dispatch_state := DispatchStates.IDLE
                voxel_addr     := voxel_addr + WORKSET_SIZE.U
            }
        }
    }
    dispatch_count := Mux(
        dispatch_state === DispatchStates.DISPATCH,
        dispatch_count + 1.U,
        0.U
    )

    io.mip_channels.zipWithIndex.foreach { case (c, i) =>
        c.voxel_addr_reg_wren := (dispatch_state === DispatchStates.WRITE_ADDR) && (dispatch_channel === i.U)
    }
    io.mip_channels.foreach { _.voxel_addr_reg_wrdata := voxel_addr }

    // io.mip_channels.foreach(_.proc_queue_wren := dispatch_state === DispatchStates.DISPATCH)
    io.mip_channels.zipWithIndex.foreach { case (c, i) =>
        c.proc_queue_wren := (dispatch_state === DispatchStates.DISPATCH) && (dispatch_channel === i.U)
    }
    io.mip_channels.foreach(_.proc_queue_wrdata := dispatch_fifo.io.rd_data)
}
