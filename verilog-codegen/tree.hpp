#ifndef VERILOG_CODGEN_TREE_INCLUDED
#define VERILOG_CODGEN_TREE_INCLUDED

#include <vector>
#include <string>
#include <memory>


/**
 * Minimal interface for any part of a Verilog program 
*/
class Tree {
public:
    
    virtual ~Tree() {};

    /**
     * Convert the tree to a string 
     * Complex trees should manage proper spacing and indentation
     * 
     * @return a string representation of the Tree
    */
    virtual std::string to_string() = 0;
private:
};

#endif