package ch.epfl.lap.wishbone_gen.bus.arbiter

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._

class BaseArbiterModule(masterDescriptions: Map[Int, MasterComponent])
  extends Module {
  
  val arbiterInputs = masterDescriptions.map({case (i, master) => 
    i -> IO(Input(Bool()).suggestName(s"${master.name}_cyc"))
  })

  val grantsOut = masterDescriptions.map( {case (i, master) => {
    i -> IO(Output(Bool()).suggestName(s"${master.name}_gnt"))
  }})

  val gntId = IO(Output(UInt()))
  val cyc_out = IO(Output(Bool()))
    
}
