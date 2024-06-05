# Wishbone Bus Generator

Generate a WISHBONE bus interconnect for a list of master(s) and slave(s) interfaces.

## Dependencies

You should be able to build and run this code with any scala build tool, but it's setup to build and run with [sbt](https://www.scala-sbt.org/download/).  

You will also need [Verilator](https://www.chisel-lang.org/docs/installation#verilator). It's required by Chisel to run simulations of the circuits.

## Build and run


Usage (with `sbt`):

``` 
sbt "run [options] <XML_description_file_path>"
    
By default, the bus is generated and every tests are run. If one or more 
of the options to generate or run tests is specified, only the specified 
generation and/or tests will be run. 

Options:

    --help, -h 
    Display this message.

    --bus, -b 
    Generate a Wishbone bus.

    --generic-test, -g
    Run a generic test on the bus.

    --test, -t
    Run test for the bus.

    --output-path, -o <path>
    Change the output path for all generated files.
```

The output Verilog file will have the same name as your description file.

You can find an example of an XML description in the `examples/shared_bus.xml` file.

## Add a new type of bus

You can add a new type of bus as follows:

 - Create a new class for your bus in the `bus` folder, it should extend the `BusModule` type
 - Also create a test class for your bus in the `test` folder
 - Add a new `BusType` at the top of the `Description.scala` file and map it to a string in the `Description` constructor
 - In the `Generators` class, add a matching to between your `BusType` and your bus class in the `generateBus` method
 - Also add a matching to your test class in the `executeTest` method