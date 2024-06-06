import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import ch.epfl.lap.wishbone_gen.Description
import ch.epfl.lap.wishbone_gen.bus.SharedBus
import ch.epfl.lap.wishbone_gen.Generators


class SharedBusTest extends AnyFlatSpec{
  behavior of "Shared Bus"
  it should "pass validation with more components and both optional signal" in {
    // Test with more componenet
    val busDescription = new Description("tests/shared_bus_more_components.xml")
    val tester = new Generators(busDescription, None)
    tester.executeGenericTest()
    tester.executeTest()
  } 
  it should "pass validation with less components and no optional signals" in {
    val busDescription = new Description("tests/shared_bus_less_components.xml")
    val tester = new Generators(busDescription, None)
    tester.executeGenericTest()
    tester.executeTest()
  } 
}
