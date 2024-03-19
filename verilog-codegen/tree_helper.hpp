#ifndef VERILOG_CODGEN_TREE_HELPER_INCLUDED
#define VERILOG_CODGEN_TREE_HELPER_INCLUDED

#include "token.hpp"

/**
 * Helper subclass of a tree that sould allow to construct a single line 
 * instruction an convert it to a string properly
*/
class LineTree : public Tree {
public:

    /**
     * Append a token at the end of the line 
    */
    LineTree& operator+=(const Token& token);

    /**
     * Append a token at the end of the line
    */
    LineTree& operator+(const Token& token);

    /**
     * Concatenate two LineTrees
    */
    LineTree& operator+=(const LineTree& l2);

    std::string to_string();
private:
    std::vector<Token> line;
};


/**
 * Helper subclass of a tree that sould allow to construct a block of code 
 * where every line has the same indentation
*/
class BlockTree : public Tree {
public:
    
    
    // TODO:  overload "+" operator to add LineTrees and/or concatenate BlockTrees
    //   BlockTree + LineTree => BlockTree
    //   BlockTree + BlockTree => BlockTree
    BlockTree& operator+=(const LineTree& line);

    BlockTree& operator+=(const BlockTree& block);

    std::string to_string();


    /**
     * BlockTree additionally allows to pring with a indent for each line
    */
    std::string to_string(int indent);
private:
    std::vector<LineTree> lines;
};

LineTree operator+(const Token& t1,  const Token& t2);

#endif