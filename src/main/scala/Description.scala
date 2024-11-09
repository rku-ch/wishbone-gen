package ch.epfl.lap.wishbone_gen

import scala.xml.Node
import java.lang

/**
  * Bus type enum
  */
sealed abstract class BusType 
object BusType {
  case object SharedBus extends BusType
  // New bus types should be added there and treated in Generators
}

case class MasterComponent(name: String, priority: Int)
case class SlaveComponent(name: String, startAddress: Long, size: Long)

class Description (val filename: String) {

  private val xmlDescription = xml.XML.loadFile(filename)
  
  // Get all global configurations
  private val configuration = xmlDescription \ "configuration"
  val useError = (configuration \ "error").text.toBoolean
  val useRetry = (configuration \ "retry").text.toBoolean
  val exposeGrants = (configuration \ "expose_grants").text.toBoolean
  val busType: BusType = (configuration \ "bus_type").text match {
    case "shared" => BusType.SharedBus
    case _ => 
      throw new IllegalArgumentException("Uknown bus type")
  }
  
  // Parse arbiter (master components)
  
  private def parseMaster(masterComponent: Node): MasterComponent = {
    val name = (masterComponent \ "name").text.toLowerCase().replace(" ", "_")
    val priority = (masterComponent \ "priority").text.toInt
    MasterComponent(name, priority)
  }
  
  val masterComponents = 
    (xmlDescription \ "arbiter" \ "master_components" \ "component")
    .map(node => parseMaster(node))
  
  // Parse memory map (slave components)
  private def parseSlave(slaveComponent: Node): SlaveComponent = {
    val name = (slaveComponent \ "name").text.toLowerCase().replace(" ", "_")
    val startAddressText = (slaveComponent \ "start_address").text.substring(1)
    val startAddress = lang.Long.parseLong(startAddressText, 16)
    val size = (slaveComponent \ "size").text.toLong
    SlaveComponent(name, startAddress, size)
  }

  val slaveComponents = 
    (xmlDescription \ "memorymap" \ "memory_regions" \ "region")
    .map(node => parseSlave(node))
  
  // Verify that there is no overlap of memory regions in the memory map
  slaveComponents.foldLeft(Nil:List[SlaveComponent])({case (prevs, next) => {
    prevs.foreach(prev => {
      if(next.startAddress < (prev.startAddress + prev.size) 
        && (next.startAddress+next.size) >= prev.startAddress) {
        println(s"${next.name} memory region overlap with ${prev.name}")
        println(s"${prev.name} start at address ${prev.startAddress.toHexString}" +
          s" and end at ${(prev.startAddress+prev.size).toHexString}")
        println(s"${next.name} start at address ${next.startAddress.toHexString}" +
          s" and end at ${(next.startAddress+next.size).toHexString}")
        throw new IllegalArgumentException("Memory regions overlap")
      }
    })
    next::prevs
  }})
}
