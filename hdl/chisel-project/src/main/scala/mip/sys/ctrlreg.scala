package mip.sys

import chisel3._
import chisel3.util._
import __global__.Mat3x4
import mip.xilinx._
import __global__.Params._
import mip.MipConfigs.N_MIP_CHANNELS
import _root_.circt.stage.ChiselStage
import mip.MipConfigs.VRAM_CHANNELS

// See spec in README.md
class CtrlReg extends Module {
    // Main IO bundle definition
    val io = IO(new Bundle {
        val ctrlreg_port = new brama_gen_port(32)                // BRAM interface for control register access
        val mvp_info     = Output(new Mat3x4())                  // Model-View-Projection matrix output
        val base_coord   = Output(Vec(3, SInt(BASE_POS_XLEN.W))) // Base coordinates output
        val start        = Output(Bool())                        // Start signal output
        // Status monitoring inputs
        val dispatch_cnt  = Input(UInt(32.W))                      // Dispatch counter input
        val channels_cnt  = Input(Vec(N_MIP_CHANNELS, UInt(32.W))) // Channel counter input (4 channels)
        val last_finished = Input(Bool())                          // Last channel finished signal input

        val dispatchFifoWrens    = Input(UInt(32.W))
        val dispatchFifoWracks   = Input(UInt(32.W))
        val dispatchFifoRdens    = Input(UInt(32.W))
        val dispatchFifoRdvalids = Input(UInt(32.W))

        val cmd_send_cnt   = Input(UInt(32.W))
        val fetch_cnt      = Input(UInt(32.W))
        val cmd_send_state = Input(UInt(32.W))
        val stream_state   = Input(UInt(32.W))
        val last_sts       = Input(UInt(32.W))

        val calc_cnt           = Input(Vec(N_MIP_CHANNELS, UInt(32.W)))
        val calc_res_valid_cnt = Input(Vec(N_MIP_CHANNELS, UInt(32.W)))

        val read_cnt = Vec(VRAM_CHANNELS, Input(UInt(32.W)))
        val wr_cnt   = Vec(VRAM_CHANNELS, Input(UInt(32.W)))
        val wr_max   = Vec(VRAM_CHANNELS, Input(UInt(32.W)))
    })

    /* Memory Map:
    BASE=0xA000_0000
    + 0x0 MVP Info
    16bit float point * 12 elements
    each takes 32bit (one word), 12 words
    48 bytes = 0x30 bytes in total
    - 0xA000_002F

    + 0x30 base coord
    +0 [15:0]: base coord 0
    +4 [15:0]: base coord 1
    +8 [15:0]: base coord 2

    + 0x40 start control
    write non-zero value to 0x40 to start rendering
    like this:
        volatile uint32_t* start_reg = (uint32_t*)0xA000_0040;
        (*start_reg) = 0x1;

    + 0x50 status monitor
    +   0x00: calc channel #0 count
    +   0x04: calc channel #1 count
    +   0x08: calc channel #2 count
    +   0x0C: calc channel #3 count
    +   0x10: dispatch count
     */

    val wren = io.ctrlreg_port.wea.asBools.reduce(_ || _)

    // MVP Info registers and write logic
    val mvp_info_regs = Reg(Vec(12, UInt(32.W)))
    for (i <- 0 until 12) {
        when(io.ctrlreg_port.addra >> 2.U === (i).U) {
            when(wren) {
                mvp_info_regs(i) := io.ctrlreg_port.dina
            }
        }
    }
    // Convert MVP registers to Mat3x4 type
    // io.mvp_info := mvp_info_regs.asTypeOf(new Mat3x4())
    io.mvp_info := RegNext(VecInit(mvp_info_regs.map(_.asTypeOf(UInt(16.W)))).asTypeOf(new Mat3x4()))

    // Base Coordinates registers and write logic
    val base_coord_regs = Reg(Vec(3, UInt(32.W)))
    for (i <- 0 until 3) {
        when(io.ctrlreg_port.addra >> 2.U === (0x30 / 4 + i).U) {
            when(wren) {
                base_coord_regs(i) := io.ctrlreg_port.dina
            }
        }
    }
    val bcr_delayed = RegNext(base_coord_regs)
    // Convert base coordinates to signed fixed-point format
    io.base_coord := bcr_delayed.map(_.asTypeOf(SInt(BASE_POS_XLEN.W)))

    // Start Control register and write logic
    val started = RegInit(false.B)
    io.start := io.ctrlreg_port.addra >> 2.U === (0x40 / 4).U && wren && io.ctrlreg_port.dina =/= 0.U
    when(io.start) {
        started := true.B
    }

    // Read multiplexer logic for all registers. read latency is 1 cycle
    io.ctrlreg_port.douta := RegNext(MuxLookup(
        io.ctrlreg_port.addra >> 2.U,
        "hABCF".U // Default value when address not matched
    )(
        Seq(
            (0x50 / 4 + 4).U -> io.dispatch_cnt, // Status monitoring registers
            (0x40 / 4).U     -> io.last_finished.asUInt,
            30.U             -> io.cmd_send_cnt,
            31.U             -> io.fetch_cnt,
            32.U             -> io.cmd_send_state,
            33.U             -> io.stream_state,
            34.U             -> io.last_sts,
            35.U             -> started.asUInt,
            80.U             -> io.dispatchFifoWrens,
            81.U             -> io.dispatchFifoWracks,
            82.U             -> io.dispatchFifoRdens,
            83.U             -> io.dispatchFifoRdvalids
        ) ++
            (0 until 12)
                .map(i => (i.U -> mvp_info_regs(i))) ++ // MVP matrix registers
            (0 until N_MIP_CHANNELS)
                .map(i => ((0x50 / 4 + i).U -> io.channels_cnt(i))) ++ // Channel counters
            (0 until 3)
                .map(i => ((0x30 / 4 + i).U -> base_coord_regs(i))) ++ // Base coordinate registers

            (0 until N_MIP_CHANNELS)
                .map(i => ((36 + i).U -> io.calc_cnt(i))) ++
            (0 until N_MIP_CHANNELS)
                .map(i => ((40 + i).U -> io.calc_res_valid_cnt(i))) ++

            (0 until VRAM_CHANNELS)
                .map(i => ((44 + i).U -> io.read_cnt(i))) ++
            (0 until VRAM_CHANNELS)
                .map(i => ((60 + i).U -> io.wr_cnt(i))) ++
            (0 until VRAM_CHANNELS)
                .map(i => ((70 + i).U -> io.wr_max(i)))
    ))
}
object ControlRegs extends App {
    ChiselStage.emitSystemVerilogFile(
        new CtrlReg(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
