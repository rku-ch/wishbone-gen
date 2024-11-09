package ch.epfl.lap.wishbone_gen.bus

import ch.epfl.lap.wishbone_gen.Description
import chisel3._
import ch.epfl.lap.wishbone_gen.MasterComponent
import ch.epfl.lap.wishbone_gen.SlaveComponent


class MasterBundle(description: MasterComponent, useError: Boolean, useRetry: Boolean) extends Bundle {
  // Master Outputs
  val adr_o = Input(UInt(32.W)).autoSeed(s"${description.name}_adr_o")
  val dat_o = Input(UInt(32.W)).autoSeed(s"${description.name}_dat_o")
  val sel_o = Input(UInt(4.W)).autoSeed(s"${description.name}_sel_o")
  val we_o = Input(Bool()).autoSeed(s"${description.name}_we_o")
  val stb_o = Input(Bool()).autoSeed(s"${description.name}_stb_o")
  val cyc_o = Input(Bool()).autoSeed(s"${description.name}_cyc_o")
  
  // Master inputs
  val dat_i = Output(UInt(32.W)).autoSeed(s"${description.name}_dat_i")
  val ack_i = Output(Bool()).autoSeed(s"${description.name}_ack_i")
  val err_i = if (useError) 
      Some(Output(Bool()).autoSeed(s"${description.name}_err_i")) 
    else 
      None
  val rty_i = if (useRetry) 
      Some(Output(Bool()).autoSeed(s"${description.name}_rty_i")) 
    else 
      None
}

class SlaveBundle(description: SlaveComponent, useError: Boolean, useRetry: Boolean) extends Bundle {

  // Slave inputs
  val adr_i = Output(UInt(32.W)).autoSeed(s"${description.name}_adr_i")
  val dat_i = Output(UInt(32.W)).autoSeed(s"${description.name}_dat_i")
  val sel_i = Output(UInt(4.W)).autoSeed(s"${description.name}_sel_i")
  val we_i = Output(Bool()).autoSeed(s"${description.name}_we_i")
  val stb_i = Output(Bool()).autoSeed(s"${description.name}_stb_i")
  val cyc_i = Output(Bool()).autoSeed(s"${description.name}_cyc_i")
  
  // Slave outputs
  val dat_o = Input(UInt(32.W)).autoSeed(s"${description.name}_dat_o")
  val ack_o = Input(Bool()).autoSeed(s"${description.name}_ack_o")
  val err_o = if (useError) Some(Input(Bool()).autoSeed(s"${description.name}_err_o")) else None
  val rty_o = if (useRetry) Some(Input(Bool()).autoSeed(s"${description.name}_rty_o")) else None

}

/** 
  * Generate IO bundles for every master and slave bundles
  * Every bundle is indexed in a map to ensure consistent ordering
  * Additionally, description for each components are stored in another map
  * with the same indexes
  *
  * @param busDescription
  */
abstract class BusModule(busDescription: Description) extends Module {
  // Both components descriptions and component IO bundles are stored in a map
  // in ordered to always be properly indexed. This insure that in every 
  // internal parts/modules (such as the arbiter and address comparator) the 
  // "result" indexed with e.g. 1 is always associated with the signals of the 
  // slave indexed 1 without needing to pass the complete IO bundle associated 
  // with a given description

  val masterDescriptions = busDescription.masterComponents.zipWithIndex
    .map({case (master, i)  => {
      i -> master
    }}).toMap

  val masterBundles = masterDescriptions.map({ case (i, master) => {
    i -> IO(new MasterBundle(
      master, 
      busDescription.useError, 
      busDescription.useRetry)
    ).suggestName(master.name)
  }})

  val grants = if (busDescription.exposeGrants) {
    Some(
      masterDescriptions.map({ case (i, master) => {
        i -> IO(Output(Bool())).suggestName(s"${master.name}_gnt")
      }})
    )
  } else None

  val slaveDescriptions = busDescription.slaveComponents.zipWithIndex
    .map({case (slave, i)  => {
      i -> slave
    }}).toMap

  val slaveBundles = slaveDescriptions.map({ case (i, slave) => {
    i -> IO(new SlaveBundle(
      slave, 
      busDescription.useError, 
      busDescription.useRetry)
    ).suggestName(slave.name)
  }})


  val invalidAddress = IO(Output(Bool())).suggestName("invalid_address")
}
