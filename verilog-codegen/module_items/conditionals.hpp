#ifndef VERILOG_CODGEN_CONDITIONALS_INCLUDED
#define VERILOG_CODGEN_CONDITIONALS_INCLUDED

#include "../tree_helper.hpp"
#include "../identifier.hpp"

/**
 * Helper for the if else syntax
 * 
*/
class IfElse : public Tree {
public:

    /**
     * Syntax for an if-else statement
     * 
     * @param condition this should include ONLY the conditional statement
     * @param ifBody
     * @param elseBody
    */
    IfElse(LineTree condition, Tree& ifBody, Tree& elseBody);

    std::string to_string(int indent = 0);
private:
    LineTree condition;
    Tree& ifBody;
    Tree& elseBody;
};

#endif