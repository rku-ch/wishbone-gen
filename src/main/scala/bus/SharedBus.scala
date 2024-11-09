package ch.epfl.lap.wishbone_gen.bus

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus.arbiter.RRArbiter
import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import chisel3.util.MuxLookup
import chisel3.util.Counter
import chisel3.util.MuxCase
import chisel3.util.OHToUInt


class SharedBus(busDescription: Description) extends BusModule(busDescription) {  
  
  val arbiter = Module(new RRArbiter(masterDescriptions))
  
  arbiter.arbiterInputs.foreach({case (i, arbiterIn) => 
    arbiterIn := masterBundles(i).cyc_o
  })

  val masterSelect = arbiter.gntId

  val masters = masterBundles.toSeq
  // // Master to slave wires 
  val adr = MuxLookup(masterSelect, masterBundles.head._2.adr_o)(masters.map({ 
    case (i, master) => (i.asUInt, master.adr_o)
  })) 
  val dat_w = MuxLookup(masterSelect, masterBundles.head._2.dat_o)(masters.map({ 
    case (i, master) => (i.asUInt, master.dat_o)
  })) 
  val sel = MuxLookup(masterSelect, masterBundles.head._2.sel_o)(masters.map({ 
    case (i, master) => (i.asUInt, master.sel_o)
  })) 
  val we = MuxLookup(masterSelect, masterBundles.head._2.we_o)(masters.map({ 
    case (i, master) => (i.asUInt, master.we_o)
  })) 
  val stb = MuxLookup(masterSelect, masterBundles.head._2.stb_o)(masters.map({ 
    case (i, master) => (i.asUInt, master.stb_o)
  }))
  val cyc = arbiter.cyc_out

  // Slaves to master wires
  // TODO: Move address comparator to a module
  val acmp = slaveDescriptions.map({ case (i, description) => {
    val matchAdr = Wire(Bool()).suggestName(s"amcp_${i}")
    val endAdr = (description.startAddress.asUInt + description.size.asUInt)
    when((adr >= description.startAddress.asUInt) & (adr < endAdr)) {
      matchAdr := true.B
    }.otherwise {
      matchAdr := false.B
    }
    (i, matchAdr)
  }}).toMap

  invalidAddress := !acmp.foldLeft(false.B)({ case (acmpCurr, (_, acmpNext)) =>  
    acmpCurr | acmpNext
  })

  val dat_r = MuxCase(slaveBundles.head._2.dat_o,
      slaveBundles.map({ 
        case (i, slave) => {
          val description = slaveDescriptions(i)
          val endAdr = (description.startAddress.asUInt + description.size.asUInt)
          val lower_bound = (adr >= description.startAddress.asUInt)
            .suggestName(s"lower_bound_${i}")
          val upper_bound = (adr < endAdr).suggestName(s"upper_bound_${i}")
          val cond = lower_bound & (adr < endAdr)
          (cond) -> slave.dat_o
        }
      }).toSeq
    ) 

  
  val ack = slaveBundles.foldLeft(false.B)({
    case (or, (i, slave)) => or | slave.ack_o
  }) 
  val err = if (busDescription.useError) 
      Some(slaveBundles.foldLeft(false.B)({
        case (or, (i, slave)) => or | slave.err_o.get
      }))
    else 
      None
  val rty = if (busDescription.useRetry) 
      Some(slaveBundles.foldLeft(false.B)({ 
        case (or, (i, slave)) => or | slave.rty_o.get
      }))
    else 
      None
  
  masterBundles.foreach({ case (i, master) => {
    master.dat_i := dat_r
    master.ack_i := ack & arbiter.grants(i)
    if (busDescription.useError) { master.err_i.get := err.get & arbiter.grants(i) }
    if (busDescription.useRetry) { master.rty_i.get := rty.get & arbiter.grants(i) }
    if (busDescription.exposeGrants) {grants.get(i) := arbiter.grants(i)}
  }})

  slaveBundles.foreach({ case (i, slave) => {
    slave.adr_i := adr
    slave.dat_i := dat_w
    slave.sel_i := sel
    slave.we_i := we
    slave.cyc_i := cyc
    val cyc_validated_stb = stb & cyc
    slave.stb_i := cyc_validated_stb & acmp(i)
  }})

}