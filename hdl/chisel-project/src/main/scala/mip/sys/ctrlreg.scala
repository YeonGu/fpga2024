package mip.sys

import chisel3._
import chisel3.util._
import __global__.Mat3x4
import mip.xilinx._
import __global__.Params._
import mip.MipConfigs.N_MIP_CHANNELS
import _root_.circt.stage.ChiselStage

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
        when(io.ctrlreg_port.addra === (i).U) {
            when(wren) {
                mvp_info_regs(i) := io.ctrlreg_port.dina
            }
        }
    }
    // Convert MVP registers to Mat3x4 type
    // io.mvp_info := mvp_info_regs.asTypeOf(new Mat3x4())
    io.mvp_info := VecInit(mvp_info_regs.map(_.asTypeOf(UInt(16.W)))).asTypeOf(new Mat3x4())

    // Base Coordinates registers and write logic
    val base_coord_regs = Reg(Vec(3, UInt(32.W)))
    for (i <- 0 until 3) {
        when(io.ctrlreg_port.addra === (0x30 / 4 + i).U) {
            when(wren) {
                base_coord_regs(i) := io.ctrlreg_port.dina
            }
        }
    }
    val bcr_delayed = RegNext(base_coord_regs)
    // Convert base coordinates to signed fixed-point format
    io.base_coord := bcr_delayed.map(_.asTypeOf(SInt(BASE_POS_XLEN.W)))

    // Start Control register and write logic
    io.start := io.ctrlreg_port.addra === (0x40 / 4).U && wren && io.ctrlreg_port.dina =/= 0.U

    // Read multiplexer logic for all registers. read latency is 1 cycle
    io.ctrlreg_port.douta := RegNext(MuxLookup(
        io.ctrlreg_port.addra,
        0.U // Default value when address not matched
    )(
        Seq(
            (0x50 / 4 + 4).U -> io.dispatch_cnt, // Status monitoring registers
            (0x40 / 4).U     -> io.last_finished.asUInt
        ) ++
            (0 until N_MIP_CHANNELS)
                .map(i => ((0x50 / 4 + i).U -> io.channels_cnt(i))) ++ // Channel counters
            (0 until 12)
                .map(i => (i.U -> mvp_info_regs(i))) ++ // MVP matrix registers
            (0 until 3)
                .map(i => ((0x30 / 4 + i).U -> base_coord_regs(i))) // Base coordinate registers
    ))
}
object ControlRegs extends App {
    ChiselStage.emitSystemVerilogFile(
        new CtrlReg(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
