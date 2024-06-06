package ch.epfl.lap.wishbone_gen.bus

import ch.epfl.lap.wishbone_gen._
import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import chisel3.util.MuxLookup
import chisel3.util.Counter
import chisel3.util.MuxCase
import chisel3.util.OHToUInt


class SharedBus(busDescription: Description) extends BusModule(busDescription) {  

  // TODO:  move to it's own file  
  // Arbiter :
  // Other people are more clever than me so I found a solution here:
  // https://abdullahyildiz.github.io/files/Arbiters-Design_Ideas_and_Coding_Styles.pdf
  // I followed the "Mask" method since it's easier to understand and write and
  // also apparently easier to synthesize 
  // And also reasonable to write for arbitrary size
  
  // Scala coolections are used everywhere, which produce ugly Verilog, but to 
  // modularity independently of the number of components, scala iterable feel 
  // quite much more practical 
  // Suggesting names make the Verilog code somewaht readable
  val arbiterInputs = masterBundles.map({case (i, master) => i -> master.cyc_o})
  
  val mask = masterDescriptions.map( {case (i, master) => {
    i -> RegInit(false.B).suggestName(s"${master.name}_mask")
  }})

  // Select the first bit with value 1 
  val simplePriority = arbiterInputs.map({ case (i, input) =>
      // This filter is an excess of precaution for ordering
    i -> (
      arbiterInputs
      .filter({case (j, _) =>  j < i}) 
      .foldLeft(true.B)({case (and, (_, value)) => and & ~value})
      & arbiterInputs(i))
      .suggestName(s"${masterDescriptions(i).name}_unmasked_grant")
  })
  val maskedInputs = arbiterInputs.map({ case (i, input) => {
    i -> (arbiterInputs(i) & mask(i))
    .suggestName(s"${masterDescriptions(i).name}_masked_input")
  }})
  val maskedPriority = arbiterInputs.map({ case (i, input) =>
      // This filter is an excess of precaution for ordering
    i -> (
      maskedInputs
      .filter({case (j, _) =>  j < i}) 
      .foldLeft(true.B)({case (and, (_, value)) => and & ~value}) 
      & maskedInputs(i))
      .suggestName(s"${masterDescriptions(i).name}_masked_grant")
  })

  val maskedGrantVec = Wire(Vec(simplePriority.size, Bool()))
    .suggestName("masked_grant")
  val maskedGrantVecUInt = maskedGrantVec.asUInt.suggestName("masked_grant_vec")
  maskedPriority.foreach({ case (i, value) => { maskedGrantVec(i) :=  value }})
  val unmaskedGrantVec = Wire(Vec(simplePriority.size, Bool()))
    .suggestName("unmasked_grant")
  val unmaskedGrantVecUInt = unmaskedGrantVec.asUInt.suggestName("unmasked_grant_vec")
  simplePriority.foreach({ case (i, value) => { unmaskedGrantVec(i) :=  value }})


  val masterGrants = masterDescriptions.map( {case (i, master) => {
    i -> RegInit(false.B).suggestName(s"${master.name}_grant")
  }})

  val cyc_out = RegInit(false.B)
  cyc_out := (VecInit(arbiterInputs.map(in => in._2).toSeq).asUInt =/= 0.U)
  val masterSelect = Wire(UInt())
  masterSelect := 0.U
  when(maskedGrantVecUInt > 0.U) {
    masterGrants.foreach({ case (i, masterGrant) => {
      masterGrant := maskedPriority(i)
    }})
    masterSelect := OHToUInt(maskedGrantVec)
    mask.foreach({ case (i, iMask) => {
      when(i.asUInt >= masterSelect){
        iMask := true.B
      }.otherwise {
        iMask := false.B
      }
    }})
  }.elsewhen(unmaskedGrantVecUInt > 0.U) {
    masterGrants.foreach({ case (i, masterGrant) => {
      masterGrant := simplePriority(i)
    }})
    masterSelect := OHToUInt(unmaskedGrantVec)
    mask.foreach({ case (i, iMask) => {
      when(i.asUInt >= masterSelect){
        iMask := true.B
      }.otherwise {
        iMask := false.B
      }
    }})
  }.otherwise {
    masterGrants.foreach({ case (i, masterGrant) => {
      masterGrant := false.B
    }})
    // We don't really care what master is selected by the muxes if no master is
    // enabled anyway
    // Mask should NOT be updated, otherwise we would bias toward low index masters
  }

  masterGrants.foreach( {case (i, masterGrant) => {
    when(maskedGrantVecUInt > 0.U) {
      masterGrant := maskedPriority(i)
      // memorizedGrants(i) := maskedPriority(i)
    }.elsewhen(unmaskedGrantVecUInt > 0.U) {
      masterGrant := simplePriority(i)
      // memorizedGrants(i) := simplePriority(i)
    }.otherwise {
      masterGrant := false.B
      // memorizedGrants(i) := memorizedGrants(i)
    }
  }})
  // We don't want to bias towards low index masters so we memorize the mask

  

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
  val cyc = cyc_out

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
    master.ack_i := ack & masterGrants(i)
    if (busDescription.useError) { master.err_i.get := err.get & masterGrants(i) }
    if (busDescription.useRetry) { master.rty_i.get := rty.get & masterGrants(i) }
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