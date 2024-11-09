package ch.epfl.lap.wishbone_gen

import ch.epfl.lap.wishbone_gen.bus._
import ch.epfl.lap.wishbone_gen.test._
import _root_.circt.stage.ChiselStage

/**
  * Given a bus description, generate the appropriate bus and/or execute the 
  * appropriate tests
  */
class Generators(busDescription: Description, outputPath: Option[String]) {
    private val args = outputPath match {
      case Some(path) => Array("--target-dir", path)
      case None => Array[String]()
    }

    // Creating a function instead of a value solve the exception : 
    // java.lang.IllegalArgumentException: requirement failed: must be inside Builder context
    // That happens when calling ChiselStage.emitSystemVerilogFile
    // (or EphemeralSimulator.simulate in the tests)
    // This is annoying since it produces a new version of the same bus every 
    // time but I couldn't figure out exactly what the exception meant
    private val generateBus = () => busDescription.busType match {
      case BusType.SharedBus => new SharedBus(busDescription)
    }

    def outputBus() = {
      println("Generating Bus")
      ChiselStage.emitSystemVerilogFile(
        generateBus(),
        args ++ Array("--split-verilog"),
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info",
          "-lowering-options=disallowLocalVariables")
      )
      println("Bus generated")
    }

    def executeGenericTest() = {
      GenericBusTest(generateBus())
    } 

    def executeTest() = {
      busDescription.busType match {
        case BusType.SharedBus => SharedBusTest(generateBus())
        case _ => throw new IllegalArgumentException("Every bus should have " +
          "a test associated to it")
      }
    }
}
