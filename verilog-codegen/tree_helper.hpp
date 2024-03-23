#ifndef VERILOG_CODGEN_TREE_HELPER_INCLUDED
#define VERILOG_CODGEN_TREE_HELPER_INCLUDED

#include <memory>

#include "token.hpp"

/**
 * Helper subclass of Tree to construct a single line using Tokens
 * Calling to_string on a LineTree always end with a line break 
*/
class LineTree : public Tree {
public:

    LineTree();

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

    /**
     * Concatenate two LineTrees
    */
    LineTree& operator+(const LineTree& l2);

    std::string to_string(int indent = 0);
private:
    std::vector<Token> line;
};


/**
 * Helper subclass of Tree to construct blocks of code with sub Trees of 
 * arbitrary types
*/
class BlockTree : public Tree {
public:
    
    /**
     * Append any sub-tree after the last sub-tree in this block
    */
    BlockTree& operator+=(const std::shared_ptr<Tree>& tree);
    /**
     * Append all the sub-trees of another block after the last sub-tree of 
     * this block
    */
    BlockTree& operator+=(const BlockTree& block);

    std::string to_string(int indent);
private:
    std::vector<std::shared_ptr<Tree>> trees;
};

LineTree operator+(const Token& t1,  const Token& t2);

#endif