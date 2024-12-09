package ch.epfl.lap.wishbone_gen.bus

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus.arbiter._
import ch.epfl.lap.wishbone_gen.ArbiterType._
import ch.epfl.lap.wishbone_gen.NameUtils._
import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import chisel3.util.MuxLookup
import chisel3.util.Counter
import chisel3.util.MuxCase
import chisel3.util.OHToUInt


class SharedBus(busDescription: Description) extends BusModule(busDescription) {  
  
  val arbiter = Module(
    busDescription.arbiterType match {
      case RoundRobin => new WeightedRRArbiter(masterDescriptions)
      case FixedPriority => new FixedPriorityArbiter(masterDescriptions)
    })
  
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

  val customMasterOutputsNames = masterBundles.flatMap({ case (_, master) => 
      master.custom_o.map(_._1) 
    }).toSet

  // Create muxes for master custom outputs
  val masterCustomOutputs = customMasterOutputsNames.map(cname => {
      val filteredMasters = masterBundles.filter(_._2.custom_o.contains(cname))
      cname -> 
      MuxLookup(masterSelect, filteredMasters.head._2.custom_o(cname))(filteredMasters.map({
          case (i, master) => 
            (i.asUInt, master.custom_o(cname))
        }).toSeq
      ).suggestName((cname))
    }).toMap
    
  val cyc = arbiter.cyc_out

  // Slaves to master wires
  // TODO: Move address comparator to a module
  val acmp = slaveDescriptions.map({ case (i, description) => {
    val matchAdr = Wire(Bool()).suggestName(s"amcp_${i}")
    val endAdr = (description.startAddress + description.size).asUInt
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
          acmp(i) -> slave.dat_o
        }
      }).toSeq
    ) 

  val customSlaveOutputsNames = slaveBundles.flatMap({ case (_, master) => 
      master.custom_o.map(_._1) 
    }).toSet 
  // Create muxes for slave custom outputs
  val slaveCustomOutputs = customSlaveOutputsNames.map(cname => {
      val filteredSlaves = slaveBundles.filter(_._2.custom_o.contains(cname))
      cname -> 
      MuxCase(filteredSlaves.head._2.custom_o(cname), 
        filteredSlaves.map({
          case (i, slave) => 
            acmp(i) -> slave.custom_o(cname)
        }).toSeq
      ).suggestName(cname+"_r")
    }).toMap
  
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
    // Custom signals
    master.custom_i.foreach({ case (cname, s) => 
        s := slaveCustomOutputs(cname)
      })
  }})

  slaveBundles.foreach({ case (i, slave) => {
    slave.adr_i := adr
    slave.dat_i := dat_w
    slave.sel_i := sel
    slave.we_i := we
    slave.cyc_i := cyc
    val cyc_validated_stb = stb & cyc
    slave.stb_i := cyc_validated_stb & acmp(i)
    // Custom signals
    slave.custom_i.foreach({ case (cname, s) => 
        s := masterCustomOutputs(cname)
      })
  }})

}