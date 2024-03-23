#ifndef VERILOG_CODGEN_TREE_INCLUDED
#define VERILOG_CODGEN_TREE_INCLUDED

#include <vector>
#include <string>
#include <memory>

#define INDENT_SIZE 4
#define INDENT_STR(INDENT) std::string(INDENT*INDENT_SIZE, ' ')

/**
 * Minimal interface for any part of a Verilog program 
*/
class Tree {
public:
    
    virtual ~Tree() {};

    /**
     * Convert the tree to a string 
     * Complex trees should manage proper spacing and indentation of 
     * their sub-trees
     *
     * @param indent add an indentation to the Tree before returning the string
     * @return a string representation of the Tree
    */
    virtual std::string to_string(int indent = 0) = 0;
private:
};

#endif