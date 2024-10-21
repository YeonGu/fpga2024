package mip.units

import chisel3._
import chisel3.util._
import mip.MipConfigs._
import mip.xilinx._
import mip.xilinx.VramConfigs._
import __global__.Params._

class MipVram extends Module {
    val io = IO(new Bundle {
        val projection_res      = Vec(VRAM_CHANNELS, Flipped(new calcChannelOut()))
        val vram_start_transfer = Input(Bool())
        val m_axis_s2mm         = new datamover_m_axis_s2mm() // VRAM stream ==> Memory Map
    })

    val vram_channels = Seq.fill(VRAM_CHANNELS)(Module(new VramChannel()))

    // Connect MIP_channels ==> VRAM_channels
    vram_channels.zipWithIndex.foreach { case (channel, i) =>
        channel.io.channel_id := i.U

        channel.io.projection_res(i).data_valid := io.projection_res(i).data_valid
        channel.io.projection_res(i).density    := io.projection_res(i).density
        channel.io.projection_res(i).screen_pos := io.projection_res(i).screen_pos
    }
    io.projection_res.zipWithIndex.foreach { case (res, i) =>
        res.rden := vram_channels.map(_.io.projection_res(i).rden).reduce(_ || _)
    }

    /* When everything within a frame is processed, take the vram_start_transfer signal
     * and start streaming the data through AXIS_S2MM channel.
     * The channel is high-bits crossed coded. which means we need to concat the data
     * from all 8 channels to form a 64-bit data.
     * The read command is sent through ReadChannel.
     */

}

class ReadChannel extends Bundle {
    val rden = Input(Bool())
    val addr = Input(UInt(VRAM_ADDRA_WIDTH.W))
    val dout = Output(UInt(DENS_DEPTH.W))
}

class VramChannel extends Module {
    val io = IO(new Bundle {
        val channel_id     = Input(UInt(log2Ceil(VRAM_CHANNELS).W))
        val en_minip       = Input(Bool())
        val projection_res = Vec(VRAM_CHANNELS, Flipped(new calcChannelOut()))
        val read_channel   = new ReadChannel()
    })

    val vram = Module(new ultra_vram())

    /* Response. fetch & decode */
    val req_channels = io.projection_res.map { res =>
        res.data_valid &&
        res.screen_pos.x(log2Ceil(VRAM_CHANNELS) - 1, 0) === io.channel_id
    }
    val need        = req_channels.reduce(_ || _)
    val sel_channel = PriorityEncoder(req_channels)
    io.projection_res.zipWithIndex.foreach { case (res, i) =>
        res.rden := (i.U === sel_channel) && need
    }

    val df_result = io.projection_res(sel_channel)

    /* read command gen. screen_pos.x/y ==> addra; valid => valid;  */
    val rd_cmd_reg = RegInit(new Bundle {
        val valid       = false.B
        val rd_addr     = 0.U(VRAM_ADDRA_WIDTH.W)
        val density     = 0.U(DENS_DEPTH.W)
        val byte_offset = 0.U(3.W)
    })
    rd_cmd_reg.valid := need
    rd_cmd_reg.rd_addr := ((df_result.screen_pos.y * SCREEN_H.U + df_result.screen_pos.x) / VRAM_CHANNELS.U)
    rd_cmd_reg.density := df_result.density
    // rd_cmd_reg.byte_offset := ((df_result.screen_pos.y * SCREEN_H.U + df_result.screen_pos.x) / VRAM_CHANNELS.U) % 8.U

    vram.io.ena   := rd_cmd_reg.valid || io.read_channel.rden
    vram.io.addra := Mux(io.read_channel.rden, io.read_channel.addr, rd_cmd_reg.rd_addr)
    /* read delay */
    val rd_delay = RegNext(rd_cmd_reg)

    /* read data & compare */
    val prev_density = vram.io.douta
    // val density  = (mem_data >> (rd_delay.byte_offset * 8.U))(7, 0)
    val wben =
        Mux(io.en_minip, prev_density < rd_delay.density, prev_density > rd_delay.density)
    val wbaddr = (rd_delay.rd_addr << 3) ## (rd_delay.byte_offset)
    vram.io.web   := wben
    vram.io.addrb := wbaddr
    vram.io.dinb  := rd_delay.density

    /* Others */
    io.read_channel.dout := vram.io.douta
}
