package mip.units

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import mip.MipConfigs._
import mip.xilinx._
import mip.xilinx.VramConfigs._
import __global__.Params._

class MipVram extends Module {
    val io = IO(new Bundle {
        val calc_res = Vec(N_MIP_CHANNELS, Flipped(new calcChannelOut()))
        val en_minip = Input(Bool())
        val ram_port = new brama_gen_port(64)
    })

    val vram_channels = Seq.fill(VRAM_CHANNELS)(Module(new MipVramChannel()))

    // Connect MIP_channels ==> VRAM_channels
    // projection_res <> mip channels
    // data -> vram_channels; rden <- |vram_channels
    vram_channels.zipWithIndex.foreach { case (channel, i) =>
        channel.io.channel_id := i.U
        channel.io.en_minip   := io.en_minip
        channel.io.projection_res.zipWithIndex.foreach { case (res, j) =>
            res.data_valid := io.calc_res(j).data_valid
            res.density    := io.calc_res(j).density
            res.screen_pos := io.calc_res(j).screen_pos
        }
    }
    io.calc_res.zipWithIndex.foreach { case (res, i) =>
        res.rden := vram_channels.map(_.io.projection_res(i).rden).reduce(_ || _)
    }

    // another way to use the mipvram is bram_port.
    // PS --- AXI BRAM CTRL --- MIPVRAM
    // Concat all channels' data together -> 64bit width.
    // Now the bram_port command will overlay commands from calc_res. This is a "force read" command.
    vram_channels.foreach { channel =>
        channel.io.read_channel.rden := io.ram_port.ena
        channel.io.read_channel.addr := io.ram_port.addra >> log2Ceil(VRAM_CHANNELS)
    }
    io.ram_port.douta := vram_channels.map(_.io.read_channel.dout).reduce(Cat(_, _))
}

class RamRead extends Bundle {
    val rden = Input(Bool())
    val addr = Input(UInt(VRAM_ADDRA_WIDTH.W))
    val dout = Output(UInt(DENS_DEPTH.W))
}

/** @brief
  *   Video RAM module implementation. VRAM and MIP Compare pipeline
  */
class MipVramChannel extends Module {
    val io = IO(new Bundle {
        val channel_id     = Input(UInt(log2Ceil(VRAM_CHANNELS).W))
        val en_minip       = Input(Bool())
        val projection_res = Vec(N_MIP_CHANNELS, Flipped(new calcChannelOut())) // fetch and response

        val read_channel = new RamRead() // read command from PS --- AXI BRAM CTRL
    })

    val vram = Module(new ultra_vram())

    // ABOUT: MIP VRAM operation pipeline
    // | RESP/DF/DEC | READ COMMAND | READ DATA - CMP - WRITE CMD
    // |             |  ADDR CALC   |  READ CMD DELAYED

    // Response, fetch & decode
    // needs first-word-fall-through fifo for low latency cache
    // select a channel with priority encoder
    val channel_req = io.projection_res.map { res =>
        res.data_valid &&
        res.screen_pos.x(log2Ceil(VRAM_CHANNELS) - 1, 0) === io.channel_id
    }
    val need        = channel_req.reduce(_ || _)
    val sel_channel = PriorityEncoder(channel_req)
    io.projection_res.zipWithIndex.foreach { case (res, i) =>
        res.rden := (i.U === sel_channel) && need
    }

    val df_result = io.projection_res(sel_channel) // fetched result

    // read command gen. screen_pos.x/y ==> addra; valid => valid;
    val rd_cmd = new Bundle {
        val valid   = Bool()
        val rd_addr = UInt(VRAM_ADDRA_WIDTH.W)
        val density = UInt(DENS_DEPTH.W)
    }
    val rd_cmd_reg = RegInit(0.U.asTypeOf(rd_cmd))
    rd_cmd_reg.valid   := need
    rd_cmd_reg.rd_addr := ((df_result.screen_pos.y * SCREEN_H.U + df_result.screen_pos.x) / VRAM_CHANNELS.U)
    rd_cmd_reg.density := df_result.density

    val vram_rden = rd_cmd_reg.valid || io.read_channel.rden
    vram.io.addra := Mux(io.read_channel.rden, io.read_channel.addr, rd_cmd_reg.rd_addr)

    // read delay & data & compare & write
    val rd_cmd_delay = RegNext(rd_cmd_reg)
    val prev_density = vram.io.douta

    val wben   = Mux(io.en_minip, prev_density < rd_cmd_delay.density, prev_density > rd_cmd_delay.density)
    val wbaddr = rd_cmd_delay.rd_addr

    vram.io.web   := wben
    vram.io.addrb := wbaddr
    vram.io.dinb  := rd_cmd_delay.density

    // others
    io.read_channel.dout := vram.io.douta

    // read
    vram.io.ena          := vram_rden || io.read_channel.rden
    vram.io.addra        := Mux(io.read_channel.rden, io.read_channel.addr, rd_cmd_reg.rd_addr)
    io.read_channel.dout := vram.io.douta
}
object Vram extends App {
    ChiselStage.emitSystemVerilogFile(
        new MipVram(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
