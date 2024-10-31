import circt.stage.ChiselStage
import rendering.IntensityProjectionCore
import mip.units.MipDataFetcher

/** Generate Verilog sources and save it in file
  */
object GCD extends App {
    ChiselStage.emitSystemVerilogFile(
        new MipDataFetcher(),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
        args = Array("--target-dir", "build")
    )
}
