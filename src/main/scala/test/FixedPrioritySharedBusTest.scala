package ch.epfl.lap.wishbone_gen.test

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._
import chisel3.simulator.EphemeralSimulator._

/**
  * Test Shared buses
  */
object FixedPrioritySharedBusTest {
  def apply(bus: => SharedBus) = {
    println(s"Starting test for Fixed Priority Shared Bus")
    simulate(bus){ b => 

      // Generate every permutations
      val permutations = for(
          n <- 1 until (2 << b.masterBundles.size)
        ) yield for (
            m <- 0 until b.masterBundles.size
          ) yield ((n >> m) & 1) == 1

      // Let ack ON for verification on master inputs 
      val slave0 = b.slaveBundles(0)
      // Send master id for validation on the slave side
      b.masterBundles.foreach({ case (i, master) => master.dat_o.poke(i)})
      slave0.ack_o.poke(true.B)
      // Test every permutations
      permutations.foreach(p => {
        // Reset cyc's
        b.masterBundles.foreach(_._2.cyc_o.poke(false))
        b.clock.step(0)
        // Setup cyc's according to the current permutation
        for(i <- 0 until p.length){
          b.masterBundles(i).cyc_o.poke(p(i))
        }
        val numberOfSteps = p.count(wantsAccess => wantsAccess)
        // Compute the expected order of enabled master
        val expectedOrder = b.masterDescriptions
          .filter({ case (index, desc) => {
            p.zipWithIndex(index)._1 // keep the one that are enabled
          }})
          .toList.map({ case (index, desc) => {
            desc.priority -> index
          }}).groupBy(_._1)
          .map({ case (prio, prioAndIndex) => 
            prio -> prioAndIndex.map(_._2).toSet
          })
          .toList.sortBy(_._1).reverse
        // println("permutation: " + p)
        // println("expected order: " + expectedOrder)
        expectedOrder.foreach( { case (_, expected) => {
          // Since we fallback to RR if multiple prios are the same, 
          // just make sure that they all get access at least once 
          val allExpectedWereGranted = expected.foldLeft(Set.empty:Set[Int])({ case (verified, e) => {
              b.clock.step(2) // 2 state because stateful!
              // Find which master the slave receives from 
              val masterIndex = slave0.dat_i.peek().litValue.toInt
              // Verify that it's one of the expected 
              if(expected.contains(masterIndex)){
                // Verify that every master is in the expected state at every step
                b.masterBundles.foreach({ case (k, master) => {
                  // println("Ack peek: " + master.ack_i.peek())
                  if(k == masterIndex){
                    master.ack_i.expect(true.B)
                  } else {
                    master.ack_i.expect(false.B)
                  }
                }})
                // Reset the expected master to let the next one take its place 
                b.masterBundles(masterIndex).cyc_o.poke(false.B)
                verified + e
              } else {
                // Force test to crash, might not be the best way
                throw new FailedExpectationException(masterIndex, s"any of ${expected}", "Observed value should be one of the expected value")
              }
            }})
            .equals(expected)
          // Verify that all the expected master were granted access
          if(!allExpectedWereGranted) {
            throw new Exception(s"All masters with similar priority should be granted access")
          }
        }})

        // Additionally, verify that no master still get feedback if we do TWO
        // additional step since the output is stateful
        b.clock.step(2)
        b.masterBundles.foreach({ case (_, master) => {
          // println("Ack post peek: " + master.ack_i.peek())
          master.ack_i.expect(false.B)
        }})
      })
    }
    println(s"Shard Bus test complete")
  }
}
