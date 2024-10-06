package cache.mip

import chisel3._
import chisel3.util._
import __global__._
import __global__.Params._
import __global__.GeneratedParams._

/** Cache block and cache group module. see waves in mip-cache-wave.json.
  *
  * This module holds several cache groups, with cache blocks inside.
  *
  * @CacheHit
  *   对每一个cache group, 在每个周期使用**优先编码**选择 [置位valid, group正确且cache hit的一个WB Channel],
  *   置位相应的ready信号, 并访问相应缓存项目, 返回读取的数据. For each cache group, in each cycle, use **priority
  *   encoding** to select a cache hit WB Channel, access the corresponding cache entry, and
  *   return the read data.
  *
  * @CacheMiss
  *   将cache miss的信息交给上层, 等待AXI返回数据, 并将数据写入cache. 注意. 在准备写入时将状态设置为 CACHE_REPLACING, 在此过程中该cache
  *   group不会处理任何请求. Pass the cache miss information to the upper layer, wait for the AXI to
  *   return data, and write the data into the cache. Note: Set the state to CACHE_REPLACING
  *   when preparing to write. During this process, the cache group will not handle any
  *   requests.
  */

class CacheReq extends Bundle {
    val screenAddr = Output(UInt(SCREEN_ADDR_WIDTH.W))
    val valid      = Output(Bool())
    val ready      = Input(Bool())
}
class CacheGroupIO extends Bundle {
    val wbLookupPort =
        Vec(MIP_WB_CHANNELS, Flipped(new WbCacheLookupPort())) /* From WB channels */
    val cacheReq = new CacheReq()
}

class CacheGroup(GROUP_INDEX: UInt) extends Module {
    val io = IO(new CacheGroupIO())

    val wbAddrDecoder = Seq.fill(MIP_WB_CHANNELS)(Module(new screenAddrDecode()))
    val blocks        = Seq.fill(MIP_CACHE_N_BLOCKS)(Module(new XilinxBramCache()))
    val blockTable = Reg(new Bundle {
        val valid      = VecInit(Seq.fill(MIP_CACHE_N_BLOCKS)(false.B))
        val tag        = VecInit(Seq.fill(MIP_CACHE_N_BLOCKS)(0.U(MIP_CACHE_TAG_WIDTH.W)))
        val lruCounter = VecInit(Seq.fill(MIP_CACHE_N_BLOCKS)(0.U(8.W)))
    })

    // TODO. what the fuck
    def delay[T <: Data](x: T, n: Int): T = (0 until n).foldLeft(x) { case (in, _) =>
        RegNext(in)
    }

    wbAddrDecoder.zipWithIndex.foreach { case (decoder, i) =>
        decoder.io.screenAddr := io.wbLookupPort(i).screenAddr
    }

    val hits = wbAddrDecoder.map {
        case channelwrinfo =>
            blockTable.tag.zip(blockTable.valid).map { case (blocktag, blockvalid) =>
                blocktag === channelwrinfo.io.tag && blockvalid === true.B && channelwrinfo.io.group === GROUP_INDEX
            }.reduce(_ || _)
    }
    val misses = wbAddrDecoder.map {
        case channelwrinfo =>
            blockTable.tag.zip(blockTable.valid).map { case (blocktag, blockvalid) =>
                blocktag === channelwrinfo.io.tag && blockvalid === true.B && channelwrinfo.io.group =/= GROUP_INDEX
            }.reduce(_ || _)
    }

    /* Select a hit channel if cache hits and status is not replacing.
     * send a READY signal to the channel, handshake with the channel.  */
    val hitSelChannel = PriorityEncoder(hits)
    io.wbLookupPort.zipWithIndex.foreach { case (wbport, i) =>
        wbport.wbReady :=
            hits(
                i
            ) && i.asUInt === hitSelChannel && cacheReqStatus =/= CacheReqStatus.replacing
    }

    val hitAddrInfo = wbAddrDecoder(hitSelChannel.litValue.toInt)

    /* BRAM Read Commands. With 1 clock delay. */
    val bramRdEns  = RegInit(VecInit(Seq.fill(MIP_WB_CHANNELS)(false.B)))
    val bramRdAddr = RegInit(0.U(log2Ceil(MIP_CACHE_BLOCK_SIZE).W))

    bramRdEns := bramRdEns.zipWithIndex.map {
        case (rdEn, blockindex) =>
            blockTable.tag(blockindex) === wbAddrDecoder(
                hitSelChannel.litValue.toInt
            ).io.tag &&
            io.wbLookupPort(hitSelChannel).wbValid &&
            io.wbLookupPort(hitSelChannel).wbReady
    }
    bramRdAddr := hitAddrInfo.io.blockOffset

    blocks.zipWithIndex.foreach { case (block, blkidx) =>
        block.io.rdEn   := bramRdEns(blkidx)
        block.io.rdAddr := bramRdAddr
    }

    /* BRAM read data comes out after 2 clocks. */
    val dhitSelChannel = delay(hitSelChannel, 3) /* this one needs 3 clocks */
    io.wbLookupPort.zipWithIndex.foreach { case (wbport, i) =>
        wbport.wbData := Mux(i.asUInt === dhitSelChannel, blocks(i).io.rdData, 0.U)
    }

    /* Request data and state machine */
    object CacheReqStatus extends ChiselEnum {
        val idle, waiting, replacing = Value
    }
    val cacheReqStatus = RegInit(CacheReqStatus.idle)

}

class XilinxBramCache extends BlackBox {
    val io = IO(new Bundle {
        val clk    = Input(Clock())
        val rst    = Input(Bool())
        val wrEn   = Input(Bool())
        val wrAddr = Input(UInt(log2Ceil(MIP_CACHE_BLOCK_SIZE).W))
        val wrData = Input(UInt(32.W))
        val rdEn   = Input(Bool())
        val rdAddr = Output(UInt(log2Ceil(MIP_CACHE_BLOCK_SIZE).W))
        val rdData = Input(UInt(32.W))
    })
}
