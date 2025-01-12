package ch.epfl.lap.wishbone_gen.test

import ch.epfl.lap.wishbone_gen._
import ch.epfl.lap.wishbone_gen.bus._
import chisel3._
import chisel3.simulator.EphemeralSimulator._

/**
  * Test Shared buses
  */
object RoundRobinSharedBusTest {
  def apply(bus: => SharedBus) = {
    println(s"Starting test for Round Robin Shared Bus")
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
      // For every starting situation
      b.masterBundles.foreach({ case (i, startingMaster) => {
        // Test every permutations
        permutations.foreach(p => {
          // Reset cyc's
          b.masterBundles.foreach(_._2.cyc_o.poke(false))
          startingMaster.cyc_o.poke(true)
          b.clock.step()
          // Setup cyc's according to the current permutation
          for(j <- 0 until p.length){
            b.masterBundles(j).cyc_o.poke(p(j))
          }
          val start = i
          val numberOfSteps = p.count(wantsAccess => wantsAccess)
          // Compute the expected order of enabled master
          val expectedOrder = (0 until b.masterBundles.size).foldLeft(Nil:List[Int])({
            case (order, j) => {
              val current = (j + start)%b.masterBundles.size
              if(p(current)){
                current :: order
              } else {
                order
              }
            }}).reverse
          expectedOrder.foreach( { case (expected) => {
            b.clock.step()
            // Verify that the slave is in the expected state 
            slave0.dat_i.expect(expected.asUInt)
            // Verify that every master is in the expected state at every step
            b.masterBundles.foreach({ case (k, master) => {
              if(k == expected){
                master.ack_i.expect(true.B)
              } else {
                master.ack_i.expect(false.B)
              }
            }})
            // Reset the expected master to let the next one take its place
            b.masterBundles(expected).cyc_o.poke(false.B)
          }})

          // Additionally, verify that no master still get feedback if we do an
          // additional step
          b.clock.step()
          b.masterBundles.foreach({ case (_, master) => {
            master.ack_i.expect(false.B)
          }})
        })
      }})
    }
    println(s"Shard Bus test complete")
  }
}
