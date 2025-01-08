package ch.epfl.lap.wishbone_gen.test

import ch.epfl.lap.wishbone_gen.bus.BusModule
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import scribe.Logger.system
import java.util.concurrent.TimeUnit
import os.size

/**
  * Generic test that any bus should pass
  * This is a "sanity" check, it should verify the following properties:
  *  - If a single master needs the bus, it should get it at some point
  *    (arbiter sanity)
  *    (The test assume that any arbiter will never take more than 
  *    (number_of_masters + 1) cycles before the master gets access to the bus)
  *  - A single master can access every component 
  *   (address comparator correctness + signal routing correctness)
  *  - The address comparator outputs on the address_error signal if an address
  *    is not part of the address space
  *    (Invalid address signaling)
  */
object GenericBusTest {
  def apply[T <: BusModule](bus: => T)  = {
    println(s"Starting generic bus test (sanitiy check)")
    val startTime = System.nanoTime()
    simulate(bus){ b =>
      // Always make sure you wait long enough when changing the master that 
      // access the bus
      val arbiterMaxStep = b.masterBundles.size + 1 + 2// TODO Properly integrate delay in the test

      // Test every master input against an address associated to the first 
      // slave
      // (Arbiter sanity)
      val expectedSlave = b.slaveBundles(0)
      val testAddress = b.slaveDescriptions(0).startAddress
      b.masterBundles.foreach({ case (i, master) => {
        master.adr_o.poke(testAddress.asUInt)
        // Use the index as data so that it's evident that it comes from the right master 
        val expectedData = i.asUInt
        // These values give a little more confidence that they are indeed 
        // updated in between different master
        val expectedSel = (i%16).asUInt
        val expectedWe = (i%2).B 
        val expectedStb = true.B
        val expectedCyc = true.B
        master.dat_o.poke(expectedData) 
        master.sel_o.poke(expectedSel)
        master.we_o.poke(expectedWe)
        master.stb_o.poke(expectedStb)
        master.cyc_o.poke(expectedCyc)
        // With the RR Arbiter implemented, it should take 1 cycle to update
        // This particular test could be generic but we'd need to assume the 
        // arbiter could take longer something like 2x #Master or master
        b.clock.step(arbiterMaxStep)

        expectedSlave.adr_i.expect(testAddress.asUInt)
        expectedSlave.dat_i.expect(expectedData)
        expectedSlave.sel_i.expect(expectedSel)
        expectedSlave.we_i.expect(expectedWe)
        expectedSlave.stb_i.expect(expectedStb)
        expectedSlave.cyc_i.expect(expectedCyc)
        
        // Don't forget to free the bus once you're done 
        master.cyc_o.poke(false.B)
      }})

      // Test address comparator + master receiving response (ack/err/rty)
      // + selection of signals from master to slaves
      // (Signal routing correctness)
      b.masterBundles.foreach({ case (masterIndex, master) => {

        master.stb_o.poke(true.B)
        master.cyc_o.poke(true.B)
        b.clock.step(arbiterMaxStep)
        b.slaveBundles.foreach({ case (i, slave) => {
          val size = b.slaveDescriptions(i).size
          // Test for a few handpicked addresses
          val startAddress = b.slaveDescriptions(i).startAddress
          val endAddress = startAddress+size - 1
          val midAddress = startAddress+(size/2)
          
          master.adr_o.poke(startAddress)
          slave.stb_i.expect(true.B)
          // Also make sure that no other slave gets enabled (<-> receive stb)
          b.slaveBundles.foreach({ case (j, otherSlave) => {
            if(i != j) {
              otherSlave.stb_i.expect(false.B)
            }
          }})
          slave.dat_o.poke(endAddress)
          master.dat_i.expect(endAddress)
          b.invalidAddress.expect(false.B)

          master.adr_o.poke(midAddress)
          slave.stb_i.expect(true.B)
          b.slaveBundles.foreach({ case (j, otherSlave) => {
            if(i != j) {
              otherSlave.stb_i.expect(false.B)
            }
          }})
          slave.dat_o.poke(midAddress)
          master.dat_i.expect(midAddress)
          b.invalidAddress.expect(false.B)

          master.adr_o.poke(endAddress)
          slave.stb_i.expect(true.B)
          b.slaveBundles.foreach({ case (j, otherSlave) => {
            if(i != j) {
              otherSlave.stb_i.expect(false.B)
            }
          }})
          slave.dat_o.poke(startAddress)
          master.dat_i.expect(startAddress)
          b.invalidAddress.expect(false.B)


          // Also check that the master receive ack/err/rty
          slave.ack_o.poke(true.B)
          master.ack_i.expect(true.B)
          b.masterBundles.foreach({ case (j, otherMaster) => {
            if(masterIndex != j) {
              otherMaster.ack_i.expect(false.B)
            }
          }})
          slave.ack_o.poke(false.B)

          slave.err_o.foreach(_.poke(true.B))
          master.err_i.foreach(_.expect(true.B))
          b.masterBundles.foreach({ case (j, otherMaster) => {
            if(masterIndex != j) {
              otherMaster.err_i.foreach(_.expect(false.B))
            }
          }})
          slave.err_o.foreach(_.poke(false.B))

          slave.rty_o.foreach(_.poke(true.B))
          master.rty_i.foreach(_.expect(true.B))
          b.masterBundles.foreach({ case (j, otherMaster) => {
            if(masterIndex != j) {
              otherMaster.rty_i.foreach(_.expect(false.B))
            }
          }})
          slave.rty_o.foreach(_.poke(false.B))
          
        }})

        master.stb_o.poke(false.B)
        master.cyc_o.poke(false.B)
      }})

      // Reset all masters
      b.masterBundles.foreach({ case (_, master) => {
        master.adr_o.poke(0.U)
        master.dat_o.poke(0.U)
        master.sel_o.poke(0.U)
        master.we_o.poke(false.B)
        master.stb_o.poke(false.B)
        master.cyc_o.poke(false.B)
      }})
      // Enable a single master
      val masterIndex = 0
      val master0 = b.masterBundles(masterIndex)
      master0.cyc_o.poke(true.B)
      b.clock.step(arbiterMaxStep)

      val minAddr = 0L
      val maxAddr = 0x1_0000_0000L
      // Find some unused address
      // We use the edges of existing memory regions, it's not very exhaustive
      // but testing all addresses would be way too long
      val unused = b.slaveDescriptions.foldLeft(Nil: List[Long])({ 
        case (unused, (_, testedSlave)) => {
          val lower = testedSlave.startAddress - 1
          val higher = testedSlave.startAddress + testedSlave.size 
          val lowerIsUnused = lower >= minAddr && 
            b.slaveDescriptions.forall({ case (_, slave) => 
              lower < slave.startAddress ||
                lower >= (slave.startAddress + slave.size)
            })
          val higherIsUnused = higher < maxAddr && 
            b.slaveDescriptions.forall({ case (_, slave) => 
              higher < slave.startAddress ||
                higher >= (slave.startAddress + slave.size)
            })
          val withLower = if(lowerIsUnused) { lower::unused } else { unused }
          if (higherIsUnused) {higher :: withLower } else { withLower }
      }})
      // The test happens only if we found some unused addresses
      // <=> it does not happen if the whole address space is used
      unused.foreach(u => {
        master0.adr_o.poke(u.asUInt)
        b.invalidAddress.expect(true.B)
      })
    }
    println(s"Generic bus test completed")
    // println(s"Elapsed time: ${(System.nanoTime() - startTime).toDouble/1_000_000_000}")
  }
}
