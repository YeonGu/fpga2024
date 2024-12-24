package mip.units

import __global__.Params._
import chisel3._
import chisel3.util._
import mip.MipConfigs._
import mip.xilinx._
import rendering._
import _root_.mip.xilinx.VramConfigs.{VRAM_ADDRB_WIDTH, VRAM_BYTE_ADDR}

/* VRAM Unit
 *
 * Channel vram *4  -->  Vram Buffer (double buffer)
 */

class VramUnit extends Module {
    val io = IO(new Bundle {
        val calcResult = Vec(N_MIP_CHANNELS, Flipped(new calcResult()))
        val VramRead   = new brama_gen_port(64) // delay 6 clk
        val idle       = Output(Bool())
        val graphicUpdate = Input(Bool()) // called when previous calculation is done; update VRAM BUFFER
    })

    // VRAM Channels and VRAM buffer
    val vramChannels = Seq.fill(N_MIP_CHANNELS)(Module(new ChannelVram()))
    val vramBuffer   = Module(new VramBuffer())

    object VramStates extends ChiselEnum {
        val IDLE, UPDATE, CHANNELFLUSH = Value
    }
    val state = RegInit(VramStates.IDLE)
    io.idle := state === VramStates.IDLE

    val updateCnt = RegInit(0.U(32.W))
    val maxDelay  = 9

    when(state === VramStates.UPDATE) { updateCnt := updateCnt + 1.U }
        .otherwise { updateCnt := 0.U }

    switch(state) {
        is(VramStates.IDLE) {
            when(io.graphicUpdate) { state := VramStates.UPDATE }
        }
        is(VramStates.UPDATE) {
            when(vramBuffer.io.updateDone) { state := VramStates.CHANNELFLUSH }
        }
        is(VramStates.CHANNELFLUSH) { state := VramStates.IDLE }
    }

    vramChannels.zipWithIndex.foreach { case (channel, i) =>
        channel.vramFlush := state === VramStates.CHANNELFLUSH
        channel.channelResult <> io.calcResult(i)
    // channel.vramRead      := io.VramRead
    }
    vramBuffer.io.update := state === VramStates.UPDATE && updateCnt === 0.U
    vramBuffer.io.channelRd <> vramChannels.map(_.vramRead)
    io.VramRead <> vramBuffer.io.bufferRd
}

class VramBuffer extends Module {
    val io = IO(new Bundle {
        val update     = Input(Bool())
        val updateDone = Output(Bool())
        val channelRd  = Vec(N_MIP_CHANNELS, Flipped(new UramWordRead()))
        val bufferRd   = new brama_gen_port(64)
    })

    val buffers    = Seq.fill(2)(Module(new ultra_vram()))
    val dispBufSel = RegInit(0.U(1.W))

    object BufferStates extends ChiselEnum {
        val IDLE, UPDATE, SWITCH = Value
    }
    val state     = RegInit(BufferStates.IDLE)
    val updateCnt = RegInit(0.U(32.W))

    when(state === BufferStates.UPDATE) { updateCnt := updateCnt + 1.U }
        .otherwise { updateCnt := 0.U }

    switch(state) {
        is(BufferStates.IDLE) { when(io.update) { state := BufferStates.UPDATE } }
        is(BufferStates.UPDATE) {
            when(updateCnt === (SCREEN_H * SCREEN_V / 8 - 1).U) { state := BufferStates.SWITCH }
        }
        is(BufferStates.SWITCH) {
            state      := BufferStates.IDLE
            dispBufSel := ~dispBufSel
        }
    }

    io.updateDone := state === BufferStates.SWITCH

    // buffers(~dispBufSel)
    // Read from channel ram
    io.channelRd.zipWithIndex.foreach { case (rd, i) =>
        // buffers.zipWithIndex.foreach { case (buf, j) =>
        //     buf.io.addra := updateCnt
        //     buf.io.ena   := j.U =/= dispBufSel && state === BufferStates.UPDATE
        // }
        rd.rdWordAddr := updateCnt
        rd.rdWordEn   := state === BufferStates.UPDATE
    }
    val wrSel  = RegNext(RegNext(RegNext(RegNext(RegNext(~dispBufSel)))))
    val wrAddr = RegNext(RegNext(RegNext(RegNext(RegNext(updateCnt)))))
    val wrEn   = RegNext(RegNext(RegNext(RegNext(RegNext(state === BufferStates.UPDATE)))))

    val rdData = io.channelRd.map(_.rdWordData)
    val cmpRes = (0 until 8).map { i =>
        val cmp = Wire(UInt(8.W))
        cmp := rdData.map(_(i * 8 + 7, i * 8)).reduce((a, b) => Mux(a > b, a, b))
        cmp
    }.reduce(Cat(_, _))

    val wrData = RegNext(RegNext(cmpRes))

    buffers.zipWithIndex.foreach { case (buf, i) =>
        buf.io.addrb := wrAddr
        buf.io.enb   := wrEn && i.U === wrSel
        buf.io.web   := Mux(wrEn && i.U === wrSel, "hff".U, 0.U)
        buf.io.dinb  := wrData

        buf.io.clk := clock
        buf.io.rst := reset.asBool
    }
    // Read from buffer porta
    buffers.zipWithIndex.foreach { case (buf, i) =>
        buf.io.addra := io.bufferRd.addra
        buf.io.ena   := (i.U === dispBufSel) && io.bufferRd.ena
    }
    // io.bufferRd.douta := buffers((!dispBufSel).asUInt).io.douta
    io.bufferRd.douta := Mux(dispBufSel.asBool, buffers(1).io.douta, buffers(0).io.douta)
}

class ChannelVram extends Module {
    val vramFlush     = IO(Input(Bool()))
    val channelResult = IO(Flipped(new calcResult()))
    val vramRead      = IO(new UramWordRead())

    val uramInst = Module(new UramControl())
    uramInst.io.external_reset := vramFlush

    channelResult.rden := true.B

    val calcRes = WireInit(0.U.asTypeOf(new MipOutputData()))
    calcRes.valid     := channelResult.data_valid
    calcRes.density   := channelResult.density
    calcRes.screenPos := channelResult.screen_pos

    // for conflict resolution
    val resDelay = RegNext(calcRes)
    val conflict = resDelay.valid && calcRes.valid && resDelay.screenPos === calcRes.screenPos

    val resRdCmd = RegInit(0.U.asTypeOf(new MipOutputData()))
    resRdCmd.valid     := calcRes.valid && !conflict
    resRdCmd.density   := calcRes.density
    resRdCmd.screenPos := calcRes.screenPos

    uramInst.io.rdByteAddr := resRdCmd.screenPos.x + resRdCmd.screenPos.y * SCREEN_H.U
    uramInst.io.rden       := resRdCmd.valid

    // compare
    val cmpRes      = RegNext(RegNext(RegNext(resRdCmd)))
    val prevDensity = uramInst.io.rddata

    // writeback
    val resWr = RegInit(0.U.asTypeOf(new MipOutputData()))
    resWr.valid     := prevDensity < cmpRes.density && cmpRes.valid
    resWr.density   := cmpRes.density
    resWr.screenPos := cmpRes.screenPos

    uramInst.io.wrByteAddr := resWr.screenPos.x + resWr.screenPos.y * SCREEN_H.U
    uramInst.io.wren       := resWr.valid
    uramInst.io.wrdata     := resWr.density

    uramInst.io.wordRead <> vramRead
}

class UramWordRead extends Bundle {
    val rdWordAddr = Input(UInt(32.W))
    val rdWordEn   = Input(Bool())
    val rdWordData = Output(UInt(64.W))
}

class UramControl extends Module {
    val io = IO(new Bundle {
        val external_reset = Input(Bool())

        val rdByteAddr = Input(UInt(VRAM_BYTE_ADDR.W))
        val rden       = Input(Bool())
        val rddata     = Output(UInt(8.W)) // After 3 clk latency

        val wrByteAddr = Input(UInt(VRAM_ADDRB_WIDTH.W))
        val wren       = Input(Bool())
        val wrdata     = Input(UInt(8.W))

        val wordRead = new UramWordRead()
    })
    val uram = Module(new ultra_vram())

    uram.io.clk := clock
    uram.io.rst := reset.asBool || io.external_reset

    // read @ port A
    //    uram.io.addra := io.rdByteAddr >> 3 // 64/8 = 8
    uram.io.addra := Mux(io.wordRead.rdWordEn, io.wordRead.rdWordAddr, io.rdByteAddr >> 3)
    uram.io.ena   := io.rden || io.wordRead.rdWordEn

    io.wordRead.rdWordData := uram.io.douta

    // 3 clk latency for byte read
    val byte_offset = RegNext(RegNext(RegNext(io.rdByteAddr(2, 0))))
    io.rddata := (uram.io.douta >> (byte_offset * 8.U))(7, 0)

    // write @ port B
    uram.io.addrb := io.wrByteAddr >> 3
    uram.io.enb   := io.wren
    uram.io.dinb  := io.wrdata << (io.wrByteAddr(2, 0) * 8.U)

    val wr_byte_offset = io.wrByteAddr(2, 0)
    val wrEn = ((0 until 8).reverse.map { i =>
        val wrBit = Wire(UInt(1.W))
        val mask  = (wr_byte_offset === i.U)
        wrBit := Mux((mask && io.wren), 1.U, 0.U)
        wrBit
    }.reduce(Cat(_, _)))
    uram.io.web := wrEn
}
