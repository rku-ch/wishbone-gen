package ch.epfl.lap.wishbone_gen.bus.arbiter

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._
import chisel3.util.OHToUInt


/**
  * A very simple round robin arbiter based on 
  * https://abdullahyildiz.github.io/files/Arbiters-Design_Ideas_and_Coding_Styles.pdf
  * It follows the "Mask" method since it's easier to understand and write and
  * (also easier to synthesize according to the paper)
  * It's also reasonable to write for arbitrary size 
  */
class RRArbiter(masterDescriptions: Map[Int, MasterComponent]) 
  extends ArbiterModule(masterDescriptions) {
  
  val masterGrants = grants.map({case (i, grantOut) => {
    val grant = RegInit(false.B).suggestName(s"s_gntReg${i}")
    grantOut := grant
    i -> grant
  }})

  val masterSelect = gntId
  val s_cyc = RegNext(cyc_out, false.B).suggestName(s"s_cycReg")
  cyc_out := (VecInit(arbiterInputs.map(in => in._2).toSeq).asUInt =/= 0.U)
  masterSelect := 0.U

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
}