package ch.epfl.lap.wishbone_gen

import scala.annotation.meta.field

sealed trait CliOption
case class Help() extends CliOption
case class Test() extends CliOption
case class GenericTest() extends CliOption
case class Bus() extends CliOption
case class OutputPath(path: String) extends CliOption
case class SplitOutput() extends CliOption

object Cli extends App {
  val help = """Usage: [options] <XML_description_file_path>
    |
    |By default, the bus is generated and every tests are run. If one or more 
    |of the options to generate or run tests is specified, only the specified 
    |generation and/or tests will be run. 
    |
    |Options:
    |
    |  --help, -h 
    |    Display this message.
    |
    |  --bus, -b 
    |    Generate a Wishbone bus.
    |
    |  --generic-test, -g
    |    Run a generic test on the bus.
    |
    |  --test, -t
    |    Run test for the bus.
    |
    |  --output-path, -o 
    |    Change the output path for all generated files.
    |
    |  --split-output, -s
    |    Split the generated verilog files by modules.
    """.stripMargin

  def parseArgs(remaining: List[String], options: Set[CliOption], filename: Option[String]): (Set[CliOption], Option[String]) = {

    remaining match {
      case head :: tail =>
        // Allow using "=" instead of a space for option parameters
        val (fixedHead, fixedTail) = head.split("=", 2) match {
          case Array(opt, arg) => (opt, arg :: tail)
          case _: Array[_] => (head, tail)
        }
        fixedHead match {
          case "--help" | "-h" => 
            parseArgs(fixedTail, options + Help(), filename)
          case "--bus" | "-b" => 
            parseArgs(fixedTail, options + Bus(), filename)
          case "--generic-test" | "-g" => 
            parseArgs(fixedTail, options + GenericTest(), filename)
          case "--test" | "-t" => 
            parseArgs(fixedTail, options + Test(), filename)
          case "--output-path" | "-o" => 
            if(fixedTail.isEmpty) {
              println("output_path option needs a paramater")
              throw new IllegalArgumentException
            } else {
              parseArgs(fixedTail.tail, options + OutputPath(fixedTail.head), filename)
            }
          case "--split-output" | "-s" =>
            parseArgs(fixedTail, options + SplitOutput(), filename)
          case str => 
            // Assume any isolated non-option/option argument is the filename
            // Can be located anywhere in the arguments
            parseArgs(fixedTail, options, Option(str)) 

        } 
      case Nil  => (options, filename)
    }
  }
  val (options, filename) = parseArgs(args.toList, Set.empty, None)

  // Return an error if we expected a filename and did not receive one
  val nonGeneratingOption = options.exists(_ match {
    case Help() => true
    case _ => false
  })
  if(filename.isEmpty && !nonGeneratingOption) {
    println("Missing argument for description file!")
    println(help)
    throw new IllegalArgumentException
  }

  if(options.contains(Help()) && options.size == 1) {
    // Only diplay help if it's the only option used
    println(help)
  } else {
    // If no option restrict the output, output everything 
    val restrictingOptions = options.filter(opt => {
      opt match {
        case Bus() => true
        case Test() => true
        case GenericTest() => true
        case _ => false
      }
    })
    val finalOptions = if(restrictingOptions.isEmpty) 
        options + Bus() + Test() + GenericTest()
      else
        options

    // Try to load XML file, this may fail if the provided filename does not 
    // exist or the XML is not formatted properly
    val busDescription = new Description(filename.get)
    
    // Execute according to options
    if(finalOptions.contains(Help())) {
      println(help)
    }

    val outputPath = finalOptions
      .find(_ match {
        case OutputPath(_) => true
        case _ => false
      }).map(_ match {
        case OutputPath(path) => path
        case _ => throw new IllegalStateException
      })

    val gen = new Generators(busDescription, outputPath, finalOptions.contains(SplitOutput()))
    if(finalOptions.contains(Bus())){
      gen.outputBus()
    }
    if(finalOptions.contains(GenericTest())) {
      gen.executeGenericTest()
    }
    if(finalOptions.contains(Test())){
      gen.executeTest()
    }
  }  
}
