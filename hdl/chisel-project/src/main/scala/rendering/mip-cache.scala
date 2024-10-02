package rendering

import chisel3._
import __global__.Params._

class MipCacheUnit extends Module {
    val MipResult = IO(Vec(MIP_CORES, Flipped(new MipOutputData())))

}

class mip_cache_ip extends BlackBox {}
