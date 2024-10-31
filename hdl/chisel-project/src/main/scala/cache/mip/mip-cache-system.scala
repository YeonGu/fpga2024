package cache.mip

import chisel3._
import chisel3.util._
import __global__._
import __global__.Params._
import __global__.GeneratedParams.SCREEN_ADDR_WIDTH

/** MIP Cache System top module. This module is the top module of the MIP Cache System.
  * @in:
  *   mipResCacheFifo: Read from Result FIFOs
  * @out:
  *   AXI4 interface ==> Zynq PS AXI-HP-S port
  */
class MipCacheSystem extends Module {
    val io = IO(new Bundle {
        val mipResFifoPort =
            Vec(
                MIP_CORES,
                Flipped(new XilinxFIFOIO(MIP_DATA_WIDTH, MIP_RESULT_FIFO_DEPTH))
            ) /* Read from Result FIFO */
        val axi4Port =
            new AXI4InterfaceM(AXIS_ADDR_WIDTH, AXIS_DATA_WIDTH) /* AXI4 interface */
    })
}

/** MIP Writeback channel module.
  *
  * This module defines the writeback channel for the MIP (Memory Interface Protocol) cache
  * system. It contains an IO bundle that facilitates communication with the cache unit.
  *
  * @param cacheLookUp
  *   The interface for communicating with the cache unit. including
  *   - tag: The tag used to find the block in the cache group.
  *   - group: The group used to determine the cache group.
  *   - wbValid: The writeback valid signal.
  *   - wbReady(in): From the cache block. The writeback ready signal.
  * @param cacheRdData
  *   The data read from the cache. With specific delayed clock cycles.
  * @param cacheWr
  *   The write command for the cache. With specific delayed clock cycles.
  */

class WbChannel extends Bundle {
    val io = IO(new Bundle {
        val cacheLookUp = new WbCacheLookupPort() /* Communicate with cache unit */
        val cacheRdData = Input(UInt(DENS_DEPTH.W))
        val cacheWr     = new WbWrCmd()
    })
}
