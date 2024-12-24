import circt.stage.ChiselStage
import mip.sys.deserted.MipSystem
import rendering.IntensityProjectionCore
import mip.units.MipDataFetcher

/** Generate Verilog sources and save it in file
  */
object Main extends App {
    ChiselStage.emitSystemVerilogFile(
        new MipSystem(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build/mip-system")
    )
}
