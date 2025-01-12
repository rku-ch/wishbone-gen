package ch.epfl.lap.wishbone_gen.bus.arbiter

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._
import chisel3.util.OHToUInt


/**
  * A weighted Round Robin arbiter, each master component appear as much as 
  * their <priority> value in a single round.
  */
class WeightedRRArbiter(masterDescriptions: Map[Int, MasterComponent]) 
  extends BaseArbiterModule(masterDescriptions) {
  
  // Duplicate 
  val duplicatedMasters = masterDescriptions.toList
    .foldLeft(Nil: List[((Int,Int), MasterComponent)])( {case (duplicated, (i, master)) => 
        duplicated ++: (for (
          p <- 0 until master.priority
        ) yield (p, i) -> MasterComponent(s"${master.name}_${p}", 1, Nil)).toList
    })
    // Avoid AAABBC -> get ABCABA instead
    .sortWith({case (((p1, i1), _), ((p2, i2), _)) => 
      if(p1 == p2) i1 < i2 else p1 < p2
    })
    .zipWithIndex.map( {case (duplicate, rr_i) =>
      rr_i -> duplicate
    }).toMap
    
  val asRRInput = duplicatedMasters.map( {case (rr_i, ((_, _), master)) =>
      rr_i -> master  
    }).toMap

  val rr = Module(new RRArbiter(asRRInput))

  rr.arbiterInputs.map({case (rr_i, cyc) => 
      cyc := arbiterInputs(duplicatedMasters(rr_i)._1._2)
    })
  grantsOut.map({case (i, grantOut) => 
      grantOut := 
        duplicatedMasters.filter({case (rr_i, ((_, i_d), _)) => i == i_d})
        .foldLeft(false.B)({case (out, (rr_i, ((_, _), _))) => out | rr.grantsOut(rr_i) })
    })

  val grantsVec = Wire(Vec(grantsOut.size, Bool()))
  grantsOut.foreach({case (i, grantOut) => grantsVec(i) := grantOut})
  gntId := OHToUInt(grantsVec)
  cyc_out := rr.cyc_out
}