package mip

import chisel3._
import chisel3.util._
import __global__.AXI4InterfaceM

class MipSystem extends Module {
    val io = IO(new AXI4InterfaceM(32, 32))

}
