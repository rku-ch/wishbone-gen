#ifndef VERILOG_CODGEN_VARIABLES_INCLUDED
#define VERILOG_CODGEN_VARIABLES_INCLUDED

#include "../tree_helper.hpp"
#include "../identifier.hpp"

/**
 * Helper for the common syntax for all types of "Data" in verilog
 * "Data" includes in/out ports, registers and wires
 * 
*/
class Data : public LineTree {
public:

    /**
     * Simplest syntax for a single bit of Data
    */
    Data(Token type, Identifier id);
    
    /**
     * Simplified syntax for defining Data with a range, add a width with the 
     * form [<range-1>: 0]
    */
    Data(Token type, Identifier id, int range);

    /**
     * Alternative syntax closer to the actual Verilog syntax, for completeness
    */
    Data(Token type, int msb, int lsb, Identifier id);

private:
};

class Input : public Data {
public:
    
    Input(Identifier id): Data(input, id) {};
    Input(Identifier id, int range): Data(input, id, range) {};
    Input(int msb, int lsb, Identifier id): Data(input, msb, lsb, id) {};

private:
};

class Output : public Data {
public:

    Output(Identifier id): Data(output, id) {};
    Output(Identifier id, int range): Data(output, id, range) {};
    Output(int msb, int lsb, Identifier id): Data(output, msb, lsb, id) {};

private:
};

class Inout : public Data {
public:

    Inout(Identifier id): Data(inout, id) {};
    Inout(Identifier id, int range): Data(inout, id, range) {};
    Inout(int msb, int lsb, Identifier id): Data(inout, msb, lsb, id) {};

private:
};


class Reg : public Data {
public:

    Reg(Identifier id): Data(reg, id) {};
    Reg(Identifier id, int range): Data(reg, id, range) {};
    Reg(int msb, int lsb, Identifier id): Data(reg, msb, lsb, id) {};
    
private:
};

class Wire : public Data {
public:

    Wire(Identifier id): Data(wire, id) {};
    Wire(Identifier id, int range): Data(wire, id, range) {};
    Wire(int msb, int lsb, Identifier id): Data(wire, msb, lsb, id) {};
    
private:
};

#endif