package ch.epfl.lap.wishbone_gen.bus.arbiter

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._
import chisel3.util.switch
import chisel3.util.is
import chisel3.util.OHToUInt

object ArbiterState extends ChiselEnum{
  val DECIDE, SERVICE, COLLECT = Value
}

class StatefulArbiterModule(masterDescriptions: Map[Int, MasterComponent])
  extends BaseArbiterModule(masterDescriptions) {
  import ArbiterState._
  
  val state = RegInit(DECIDE)

  val grants = masterDescriptions.map( {case (i, master) => {
    i -> Wire(Bool()).suggestName(s"s_${master.name}_gnt")
  }})
  private val hasDecided = grants.foldLeft(false.B)({ case (or, (i, grant)) => or | grant})

  val grantsReg = masterDescriptions.map( {case (i, master) => {
    val grantReg = RegInit(false.B).suggestName(s"s_${master.name}_gntReg")
    grantsOut(i) := grantReg
    i -> grantReg
  }})
  private val transactionEnd = grantsReg.foldLeft(false.B)({ case (or, (i, grantReg)) => {
    (grantReg & ~arbiterInputs(i)) | or
  }})
    
  // private val delay = 0.U
  // private val delay_collect = RegInit(delay)

  // State machine
  switch (state) {
    is(DECIDE) {
      when (hasDecided) {
        state := SERVICE
      }
    }
    is(SERVICE) {
      when (transactionEnd) {
        state := DECIDE
      }
    }
    // is(COLLECT) {
    //   when (delay_collect === 0.U) {
    //     // TODO add option for delay (to let the master component start a new request before next round)
            // Note: this is probably not so useful since the existing processor waits for at least 5 cycle before requesting a data access again

    //     state := DECIDE
    //   }
    // }
  }
  
  // Outputs logic
  private val is_deciding = (state === DECIDE)
  private val is_servicing = (state === SERVICE)
  grantsReg.foreach({ case (i, grantReg) => 
      when (is_deciding) {
        grantReg := grants(i)
      }.elsewhen (is_servicing) {
        grantReg := grantReg
        // delay_collect := delay
      }
      // .otherwise {
      //   when (!(delay_collect === 0.U)) {
      //     delay_collect := delay_collect-1.U
      //   }.otherwise {
      //     delay_collect := delay_collect
      //   }
      //   grantReg := false.B
      // }
    })

  private val grantsVec = Wire(Vec(grantsOut.size, Bool()))
  grantsOut.foreach({case (i, grantOut) => grantsVec(i) := grantOut})
  gntId := OHToUInt(grantsVec)

  cyc_out := arbiterInputs.foldLeft(false.B)({ case (or, (_, cyc)) => or | cyc})
}