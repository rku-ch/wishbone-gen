package ch.epfl.lap.wishbone_gen.bus.arbiter

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._
import chisel3.util.OHToUInt

/**
  * A fixed priority arbiter
  * If two or more master have the same priority their turn a decided by a Round Robin arbiter
  */
class FixedPriorityArbiter(masterDescriptions: Map[Int, MasterComponent]) 
  extends ArbiterModule(masterDescriptions) {

  masterDescriptions
    .groupBy(_._2.priority)
    .toSeq
    .sortBy(_._1)
    .reverse
    .foldLeft(0.U)({ case (or, (priority, (masters))) =>
      if (masters.size == 1) {
        grants(masters.head._1) := arbiterInputs(masters.head._1) & ~or
        arbiterInputs(masters.head._1) | or
      } else {
        // If two have the same priority, fallback to Round Robin
        val mastersRRmapping = masters.zipWithIndex.map({ case ((i, master), rr_i) => 
            i -> (rr_i -> master)
          }).toMap
        val rrMastersMapping = mastersRRmapping.map(e => (e._2._1 -> e._1))
        val rr = Module(new RRArbiter(mastersRRmapping.map(_._2)))
        rr.cycs.foreach({case (rr_i, in) => 
            in := arbiterInputs(rrMastersMapping(rr_i))
          })
        masters.foreach( {case (i, master) => 
            grants(i) :=  rr.grantsOut(mastersRRmapping(i)._1) & ~or
          })
        masters.foldLeft(0.U)({case (or, (i, _)) => arbiterInputs(i) | or}) | or
      }
    })
}