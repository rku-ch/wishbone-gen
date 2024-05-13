package bus

import scala.xml.Node
import java.lang

case class MasterComponent(name: String, priority: Int)
case class SlaveComponent(name: String, startAddress: Long, size: Long)

class Description (val filename: String) {
  // TODO: give useful info if a field is missing (or add sensible defaults)  

  private val xmlDescription = xml.XML.loadFile(filename)
  
  // Get all options
  private val options = xmlDescription \ "options"
  val useError = (options \ "error").text.toBoolean
  val useRetry = (options \ "retry").text.toBoolean
  val busType = (options \ "bus_type").text
  
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

  // println(hasErrorSignal)
  // println(hasRetrySignal)
  // println(busType)
  // println(masterComponents)
  // println(slaveComponents)
}
