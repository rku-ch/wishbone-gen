package bus

import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import chisel3.util.MuxLookup
import chisel3.util.Counter
import chisel3.util.MuxCase
import chisel3.util.OHToUInt

class MasterBundle(description: MasterComponent, useError: Boolean, useRetry: Boolean) extends Bundle {
  println(description.name)
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
  val err_i = if (useError) Some(Output(Bool()).autoSeed(s"${description.name}_err_i")) else None
  val rty_i = if (useRetry) Some(Output(Bool()).autoSeed(s"${description.name}_rty_i")) else None
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


class SharedBus(val busDescription: Description) extends Module {

  // Both components descriptions and component IO bundles are stored in a map
  // in ordered to always be properly indexed. This insure that in every 
  // internal parts/modules (such as the arbiter and address comparator) the 
  // "result" indexed with e.g. 1 is always associated with the singals of the 
  // slave indexed 1 wothout needing to pass the complete IO bundle associated 
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

  // --- Internal wires ---
  // We use registers to determine which one was the last one accessed for 
  // fairness but other than that the RoundRobin arbiter is fully combinatorial,
  // outputs are always up to date
  // This is not what they propose in the spec so maybe I shouldn't do it like that?
  // The reason is: if we did that, the new master aquiring the bus may receive 
  // an ack (or err or rty) that was not meant for it
  // GNT = grant? I don't like that name I'll try to find one myself
  // val counter = new Counter(orderedMasters.length)
  // masterSelect := counter.value
  // orderedMasters.foreach({ case (master, _, index) => {
  //   when(counter.value === index.asUInt) {
  //     when(master.cyc_o === false.B) {
  //       counter.inc()   
  //     }
  //   }
  // }})

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
    i -> Reg(Bool()).suggestName(s"${master.name}_mask")
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
  // val maskedGrantVec = VecInit(maskedPriority.map(in => in._2).toSeq).asUInt
  // val unmaskedGrantVec = VecInit(simplePriority.map(in => in._2).toSeq).asUInt


  val masterGrants = masterDescriptions.map( {case (i, master) => {
    i -> Reg(Bool()).suggestName(s"${master.name}_grant")
  }})
  
  val cyc_out = Reg(Bool())
  cyc_out := (VecInit(arbiterInputs.map(in => in._2).toSeq).asUInt =/= 0.U)
  val masterSelect = Wire(UInt())
  masterSelect := 0.U

  when(maskedGrantVecUInt > 0.U) {
    masterSelect := OHToUInt(maskedGrantVec)
  }.elsewhen(unmaskedGrantVecUInt > 0.U) {
    masterSelect := OHToUInt(unmaskedGrantVec)
  }

  masterGrants.foreach( {case (i, masterGrant) => {
    when(maskedGrantVecUInt > 0.U) {
      masterGrant := maskedPriority(i)
    }.elsewhen(unmaskedGrantVecUInt > 0.U) {
      masterGrant := simplePriority(i)
    }.otherwise {
      masterGrant := masterGrants(i)
    }
  }})

  mask.foreach({ case (i, iMask) => {
    when(i.asUInt >= masterSelect){
      iMask := true.B
    }.otherwise {
      iMask := false.B
    }
  }})

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
    val matchAdr = Wire(Bool())
    val endAdr = (description.startAddress.asUInt + description.size.asUInt)
    when((adr >= description.startAddress.asUInt) & (adr < endAdr)) {
      matchAdr := true.B
    }.otherwise {
      matchAdr := false.B
    }
    (i, matchAdr)
  }}).toMap

  //TODO: probs incomplete/incorrect
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
        case (or, (i, slave)) => or | slave.err_o.get
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

/**
 * Generate Verilog sources and save it in file Bus.v
 */
object SharedBus extends App {
  val busDescription = new Description(args.head)

  ChiselStage.emitSystemVerilogFile(
    new SharedBus(busDescription),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-lowering-options=disallowLocalVariables")
  )
}