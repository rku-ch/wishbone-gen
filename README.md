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
    
    --split-output, -s
    Split the generated verilog files by modules.
```

The output Verilog file will have the same name as your description file.

You can find an example of an XML description in the `examples/or1420RoundRobin_1_1_1_1.xml` file.

## Using custom signals

In your description, you can add custom signals to your bus architecture. For more information on these custom signals including tag types, refer to the Wishbone specification, notably section 3.1.6.

The custom signals are matched together by their <name> elements and to avoid most errors the following properties are verified when parsing the description: 

- All signals that have the same name must have the same width.
- If two components with the same role (both masters or both slaves) use a custom signal with the same name, their tags also have to be the same.
- If two components with opposite roles (one master, one slave) use a custom signal with the same name, their tags have to correspond. E.g., *TGA_O* on master and *TGA_I* on the slave.
- If a custom signal is present in a master component, it needs to be present in at least one slave component and vice versa.

## Add a new type of bus

You can add a new type of bus as follows:

 - Create a new class for your bus in the `bus` folder, it should extend the `BusModule` type
 - Also create a test class for your bus in the `test` folder
 - Add a new `BusType` at the top of the `Description.scala` file and map it to a string in the `Description` constructor
 - In the `Generators` class, add a matching between your `BusType` and your bus class in the `generateBus` method
 - Also add a matching to your test class in the `executeTest` method

## Add a new type of Arbiter

You can add a new type of arbiter as follows:

 - Create a new class for your arbiter in the `bus/arbiter` folder, it should extend the `BaseArbiterModule` type
 - Also create a test class for your new bus and arbiter pair in the `test` folder
 - Add a new `ArbiterType` at the top of the `Description.scala` file and map it to a string in the `Description` constructor
 - In the `SharedBus` class (or your new bus type), add a matching between your `ArbiterType` and your arbiter class when assigning the arbiter value
 - In the `Generators` class, adapt the matching for your test class in the `executeTest` method
