package mip.units

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import mip.MipConfigs._
import mip.xilinx._
import mip.xilinx.VramConfigs._
import __global__.Params._
import __global__.ScreenPos

class MipVram extends Module {
    val io = IO(new Bundle {
        val calc_res       = Vec(N_MIP_CHANNELS, Flipped(new calcResult()))
        val en_minip       = Input(Bool())
        val ram_reset      = Input(Bool())
        val ram_reset_busy = Output(Bool())
        val ram_port       = new brama_gen_port(64)

        val start_cmd = Input(Bool())
        val read_cnt  = Vec(VRAM_CHANNELS, Output(UInt(32.W)))
        val wr_cnt    = Vec(VRAM_CHANNELS, Output(UInt(32.W)))
        val wr_max    = Vec(VRAM_CHANNELS, Output(UInt(32.W)))
    })

    val vram_channels = Seq.fill(VRAM_CHANNELS)(Module(new MipVramChannel()))

    // Connect MIP_channels ==> VRAM_channels
    // projection_res <> mip channels
    // data -> vram_channels; rden <- |vram_channels
    vram_channels.zipWithIndex.foreach { case (channel, i) =>
        channel.io.channel_id := i.U
        channel.io.en_minip   := io.en_minip
        channel.io.start_cmd  := io.start_cmd
        channel.io.projection_res.zipWithIndex.foreach { case (res, j) =>
            res.data_valid := io.calc_res(j).data_valid
            res.density    := io.calc_res(j).density
            res.screen_pos := io.calc_res(j).screen_pos
        }
    }
    io.calc_res.zipWithIndex.foreach { case (res, i) =>
        res.rden := vram_channels.map(_.io.projection_res(i).rden).reduce(_ || _)
    }
    io.read_cnt := vram_channels.map(_.io.read_cnt)
    io.wr_cnt   := vram_channels.map(_.io.wr_cnt)
    io.wr_max   := vram_channels.map(_.io.wr_max)

    // another way to use the mipvram is bram_port.
    // PS --- AXI BRAM CTRL --- MIPVRAM
    // Concat all channels' data together -> 64bit width.
    // Now the bram_port command will overlay commands from calc_res. This is a "force read" command.
    vram_channels.foreach { channel =>
        channel.io.read_channel.rden      := io.ram_port.ena
        channel.io.read_channel.byte_addr := io.ram_port.addra >> 3.U
        channel.io.read_channel.ram_reset := io.ram_reset
    }
    io.ram_port.douta := vram_channels.map(_.io.read_channel.dout).reduce(Cat(_, _))
    io.ram_reset_busy := vram_channels.map(_.io.read_channel.ram_reset_busy).reduce(_ || _)
}

class RamPort extends Bundle {
    val rden           = Input(Bool())
    val byte_addr      = Input(UInt(VRAM_ADDRA_WIDTH.W))
    val dout           = Output(UInt(DENS_DEPTH.W))
    val ram_reset      = Input(Bool())
    val ram_reset_busy = Output(Bool())
}

/** @brief
  *   Video RAM module implementation. VRAM and MIP Compare pipeline
  */
class MipVramChannel extends Module {
    val io = IO(new Bundle {
        val channel_id     = Input(UInt(log2Ceil(VRAM_CHANNELS).W))
        val en_minip       = Input(Bool())
        val projection_res = Vec(N_MIP_CHANNELS, Flipped(new calcResult())) // fetch and response

        val start_cmd = Input(Bool())
        val read_cnt  = Output(UInt(32.W))
        val wr_cnt    = Output(UInt(32.W))
        val wr_max    = Output(UInt(32.W))

        val read_channel = new RamPort() // read command from PS --- AXI BRAM CTRL
    })

    // val vram = Module(new ultra_vram())
    val uram_ctrl = Module(new Uram64Ctrl())

    uram_ctrl.io.external_reset    := reset.asBool || io.read_channel.ram_reset
    io.read_channel.ram_reset_busy := false.B

    // ABOUT: MIP VRAM operation pipeline
    // | RESP/DF/DEC | READ COMMAND | #1 | #2 | READ DATA - CMP - WRITE CMD
    // |             |  ADDR CALC   | #1 | #2 |  READ CMD DELAYED
    // |             |  NOP?        | #1 | #2 |  NOP?

    // Response, fetch & decode
    // needs first-word-fall-through fifo for low latency cache
    // select a channel with priority encoder
    val addr_conflict = Wire(Bool())
    val stall         = Wire(Bool())

    val channel_req = io.projection_res.map { res =>
        res.data_valid &&
        res.screen_pos.x(log2Ceil(VRAM_CHANNELS) - 1, 0) === io.channel_id
    }
    val need        = channel_req.reduce(_ || _)
    val sel_channel = PriorityEncoder(channel_req)
    io.projection_res.zipWithIndex.foreach { case (res, i) =>
        res.rden := (i.U === sel_channel) && need && !stall
    }

    val df_result = io.projection_res(sel_channel) // fetched result

    // read command gen reg. screen_pos.x/y ==> addra; valid => valid;
    val rd_cmd = new Bundle {
        val valid      = Bool()
        val rd_addr    = UInt(VRAM_ADDRA_WIDTH.W)
        val density    = UInt(DENS_DEPTH.W)
        val screen_pos = new ScreenPos()
    }
    val rd_cmd_reg = RegInit(0.U.asTypeOf(rd_cmd))

    rd_cmd_reg.valid := io.projection_res.map(_.rden).reduce(_ || _)
    rd_cmd_reg.rd_addr := Mux(
        stall,
        rd_cmd_reg.rd_addr,
        (df_result.screen_pos.y * SCREEN_H.U + df_result.screen_pos.x) / VRAM_CHANNELS.U
    )
    rd_cmd_reg.screen_pos := Mux(stall, rd_cmd_reg.screen_pos, df_result.screen_pos)
    rd_cmd_reg.density    := Mux(stall, rd_cmd_reg.density, df_result.density)

    // PIPELINE CONFLICT RESOLVE
    // when RESP and READCMD address conflicts, RESP hold one cycle
    // val resp_addr  = (df_result.screen_pos.y * SCREEN_H.U + df_result.screen_pos.x) / VRAM_CHANNELS.U
    val rdcmd_addr = rd_cmd_reg.rd_addr
    addr_conflict := df_result.screen_pos === rd_cmd_reg.screen_pos
    // stall         := addr_conflict && need && rd_cmd_reg.valid
    stall := false.B

    val vram_rden = rd_cmd_reg.valid || io.read_channel.rden
    uram_ctrl.io.rd_byte_addr := Mux(io.read_channel.rden, io.read_channel.byte_addr, rd_cmd_reg.rd_addr)

    val read_cnt_reg = RegInit(0.U(32.W))
    val wr_cnt_reg   = RegInit(0.U(32.W))
    val wr_max_reg   = RegInit(0.U(32.W))
    read_cnt_reg := Mux(io.start_cmd, 0.U, read_cnt_reg + vram_rden)
    io.read_cnt  := read_cnt_reg
    io.wr_cnt    := wr_cnt_reg
    io.wr_max    := wr_max_reg

    // read delay & data & compare & write
    // due to the restriction of URAM, read delay should be no less than 3 cycles
    // also vram read port gives 64bits, which we only need 8bits. Wrap the address and select the important part.
    val rd_cmd_delay = RegNext(RegNext(RegNext(rd_cmd_reg)))
    val prev_density = uram_ctrl.io.rddata

    val wben   = Mux(io.en_minip, prev_density > rd_cmd_delay.density, prev_density < rd_cmd_delay.density)
    val wbaddr = rd_cmd_delay.rd_addr
    when(io.start_cmd) {
        wr_cnt_reg := 0.U
        wr_max_reg := 0.U
    }.elsewhen(wben && uram_ctrl.io.wren) {
        wr_cnt_reg := wr_cnt_reg + 1.U
        wr_max_reg := Mux(wr_max_reg > rd_cmd_delay.density, wr_max_reg, rd_cmd_delay.density)
    }

    uram_ctrl.io.wr_byte_addr := wbaddr
    uram_ctrl.io.wren         := wben && rd_cmd_delay.valid
    uram_ctrl.io.wrdata       := rd_cmd_delay.density

    // read
    uram_ctrl.io.rden    := vram_rden || io.read_channel.rden
    io.read_channel.dout := uram_ctrl.io.rddata
}

/** @brief
  *   URAM64 byte w/r control
  *
  * @write
  *   8bit data and byte-address.
  * @read
  *   8bit data and byte-address. latency=3
  */
class Uram64Ctrl extends Module {
    val io = IO(new Bundle {
        val external_reset = Input(Bool())

        // val rd_addr = Input(UInt(VRAM_ADDRA_WIDTH.W))
        val rd_byte_addr = Input(UInt(VRAM_BYTE_ADDR.W))
        val rden         = Input(Bool())
        val rddata       = Output(UInt(8.W))

        // val wr_addr = Input(UInt(VRAM_ADDRB_WIDTH.W))
        val wr_byte_addr = Input(UInt(VRAM_ADDRB_WIDTH.W))
        val wren         = Input(Bool())
        val wrdata       = Input(UInt(8.W))
    })
    val uram = Module(new ultra_vram())

    uram.io.clk := clock
    uram.io.rst := reset.asBool || io.external_reset

    // read @ port A
    uram.io.addra := io.rd_byte_addr >> 3 // 64/8 = 8
    uram.io.ena   := io.rden
    // 3 clk latency
    val byte_offset = RegNext(RegNext(RegNext(io.rd_byte_addr(2, 0))))
    io.rddata := (uram.io.douta >> (byte_offset * 8.U))(7, 0)

    // write @ port B
    uram.io.addrb := io.wr_byte_addr >> 3
    uram.io.enb   := io.wren
    uram.io.dinb  := io.wrdata << (io.wr_byte_addr(2, 0) * 8.U)

    val wr_byte_offset = io.wr_byte_addr(2, 0)
    uram.io.web := VecInit((0 until 8).map { i =>
        val mask = (wr_byte_offset === i.U)
        mask && io.wren
    }).asUInt
}

object Vram extends App {
    ChiselStage.emitSystemVerilogFile(
        new Uram64Ctrl(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
