package ch.epfl.lap.wishbone_gen

import scala.xml.Node
import java.lang
import ch.epfl.lap.wishbone_gen.NameUtils._

/**
  * Bus type enum
  */
sealed abstract class BusType 
object BusType {
  case object SharedBus extends BusType
  // New bus types should be added there and treated in Generators
}

/**
  * Arbiter type enum
  */
sealed abstract class ArbiterType 
object ArbiterType {
  case object RoundRobin extends ArbiterType
  case object FixedPriority extends ArbiterType
  // New arbiter types should be added there and treated in Generators
}

/**
  * Custom signal tag enum (see Wishbone specification v4 p.38)
  */
sealed abstract class TagType 
object TagType {
  // MASTER Tags
  case object TGA_O extends TagType
  case object TGC_O extends TagType
  // SLAVE Tags
  case object TGA_I extends TagType
  case object TGC_I extends TagType
  // Both
  case object TGD_I extends TagType
  case object TGD_O extends TagType

}

case class MasterComponent(name: String, priority: Int, customSignals: Seq[CustomSignal])
case class SlaveComponent(name: String, startAddress: Long, size: Long, customSignals: Seq[CustomSignal])
case class CustomSignal(tag: TagType, name: String, width: Int, isOutput: Boolean)

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
  private def parseMasterCustomSignals(signal: Node): CustomSignal = {
    val name = (signal \ "name").text.toLowerCase()
    val width = (signal \ "width").text.toInt
    (signal \ "tag").text match {
      case "TGA_O" => CustomSignal(TagType.TGA_O, name, width, true)
      case "TGD_I" => CustomSignal(TagType.TGD_I, name, width, false)
      case "TGD_O" => CustomSignal(TagType.TGD_O, name, width, true)
      case "TGC_O" => CustomSignal(TagType.TGC_O, name, width, true)
      case _ => 
        throw new IllegalArgumentException("Unexpected tag type for master component, valid tag types for master are: TGA_O, TGD_I, TGD_O, TGC_O.")
    }
  }

  private def parseMaster(masterComponent: Node): MasterComponent = {
    val name = (masterComponent \ "name").text.toLowerCase().replace(" ", "_")
    val priority = (masterComponent \ "priority").text.toInt
    val customSignals = (masterComponent \ "signal").map(parseMasterCustomSignals)
    MasterComponent(name, priority, customSignals)
  }

  val arbiter = xmlDescription \ "arbiter"
  val arbiterType = (arbiter \ "type").text match {
    case "round_robin" => ArbiterType.RoundRobin
    case "fixed_priority" => ArbiterType.FixedPriority
    case _ => 
      throw new IllegalArgumentException("Uknown or missing arbiter type")
  }
  val masterComponents = 
    (arbiter \ "master_components" \ "component")
    .map(parseMaster)
  
  // Parse memory map (slave components)
  private def parseSlaveCustomSignals(signal: Node): CustomSignal = {
    val name = (signal \ "name").text.toLowerCase()
    val width = (signal \ "width").text.toInt
    (signal \ "tag").text match {
      case "TGA_I" => CustomSignal(TagType.TGA_I, name, width, false)
      case "TGD_I" => CustomSignal(TagType.TGD_I, name, width, false)
      case "TGD_O" => CustomSignal(TagType.TGD_O, name, width, true)
      case "TGC_I" => CustomSignal(TagType.TGC_I, name, width, false)
      case _ => 
        throw new IllegalArgumentException("Unexpected tag type for slave component, valid tag types for slave are: TGA_I, TGD_I, TGD_O, TGC_I.")
    }
  }

  private def parseSlave(slaveComponent: Node): SlaveComponent = {
    val name = (slaveComponent \ "name").text.toLowerCase().replace(" ", "_")
    val startAddressText = (slaveComponent \ "start_address").text.substring(1)
    val startAddress = lang.Long.parseLong(startAddressText, 16)
    val size = (slaveComponent \ "size").text.toLong
    val customSignals = (slaveComponent \ "signal").map(parseSlaveCustomSignals)
    SlaveComponent(name, startAddress, size, customSignals)
  }

  val slaveComponents = 
    (xmlDescription \ "memorymap" \ "memory_regions" \ "region")
    .map(parseSlave)
  
  // ------ Validate SOME expected properties of the description ------

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

  // Verify that there is no unused custom signals and that all custom signals 
  // with the same names have the same width and equivalent tags
  // This is primarily to avoid unexpected result since the generation will not 
  // prevent ALL of these situation
  def customSignalErrorText(name: String, 
    componentRole1: String, componentRole2: String,
    tag1: TagType, tag2: TagType, 
    width1: Int, width2: Int): String = {
      s"Custom signals with the same name must have corresponding tags and the" +
      s" same width \n" +
      s"Name: ${name}\n" +
      s"Component roles: ${componentRole1} and ${componentRole2}\n" +
      s"Tags: ${tag1} and ${tag2}\n" +
      s"Widths: ${width1} and ${width2}"

  }

  val masterCustomSignals = masterComponents
    .foldLeft(Map.empty: Map[String, (TagType, Int)])({ case (knownSignals, component) =>
      knownSignals ++ 
      component.customSignals.foldLeft(knownSignals)({ case (newKnownSignals, s) =>
          if (!newKnownSignals.contains(s.name)) 
            newKnownSignals+(s.name-> (s.tag, s.width))
          else {
            val knownSignal = newKnownSignals(s.name)
            if (knownSignal._1 != s.tag || knownSignal._2 != s.width) { 
              throw new IllegalArgumentException(
                customSignalErrorText(s.name, 
                  "Master", "Master",
                  knownSignal._1, s.tag, 
                  knownSignal._2, s.width))

            } else newKnownSignals 
          }
      })
    })
  
  val slaveCustomSignals = slaveComponents
    .foldLeft(Map.empty: Map[String, (TagType, Int)])({ case (knownSignals, component) => 
      knownSignals ++ 
      component.customSignals.foldLeft(knownSignals)({ case (newKnownSignals, s) =>
        if(!masterCustomSignals.contains(s.name)) {
          throw new IllegalArgumentException(s"Slave custom signal ${s.name} " +
            s"has no corresponding signal in any master component!")
        } else {
          val knownSignal = masterCustomSignals(s.name)
          val tagsEquivalent = (knownSignal._1 == TagType.TGA_O && s.tag == TagType.TGA_I
            || knownSignal._1 == TagType.TGD_I && s.tag == TagType.TGD_O
            || knownSignal._1 == TagType.TGD_O && s.tag == TagType.TGD_I
            || knownSignal._1 == TagType.TGC_O && s.tag == TagType.TGC_I)
            
          if (!tagsEquivalent || knownSignal._2 != s.width) { 
            throw new IllegalArgumentException(
              customSignalErrorText(s.name, 
                "Master", "Slave",
                knownSignal._1, s.tag, 
                knownSignal._2, s.width)) 

          } else if (!newKnownSignals.contains(s.name)) {
            newKnownSignals+(s.name-> (s.tag, s.width))
          } else {
            val knownSignal = newKnownSignals(s.name)
            if (knownSignal._1 != s.tag || knownSignal._2 != s.width) { 
              throw new IllegalArgumentException(
                customSignalErrorText(s.name, 
                  "Slave", "Slave",
                  knownSignal._1, s.tag, 
                  knownSignal._2, s.width
                  ))
            } 
            else newKnownSignals 
          }
        }
      })
    })
    
  masterCustomSignals.removedAll(slaveCustomSignals.keySet)
    .foreach( s => {
      val name = s._1
      throw new IllegalArgumentException(s"Master custom signal ${name} has no" +
        s" corresponding signal in any slave component!") 
    }
    )

}
