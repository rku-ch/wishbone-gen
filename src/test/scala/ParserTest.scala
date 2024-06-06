import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import ch.epfl.lap.wishbone_gen.Description
import ch.epfl.lap.wishbone_gen.bus.SharedBus


class ParserTest extends AnyFlatSpec{
  behavior of "Bus Definition parsing"
  it should "fail when missing an optional signal" in {
    assertThrows[Exception]{
      new Description("tests/parser_missing_tag.xml")
    }
  } 
  it should "fail when some memory regions overlap" in {
    assertThrows[IllegalArgumentException]{
      new Description("tests/parser_overlaping_memory_regions.xml")
    }
  } 
}
