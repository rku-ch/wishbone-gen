#include "tree_helper.hpp"

LineTree::LineTree(){}

// Operation on trees

LineTree operator+(const Token& t1,  const Token& t2) {
    LineTree l;
    l += t1;
    l += t2; 
    return l;
}

LineTree& LineTree::operator+=(const Token& token) {
    line.push_back(token);
    return *this;
}

LineTree& LineTree::operator+(const Token& token) {
    line.push_back(token);
    return *this;
}

LineTree& LineTree::operator+=(const LineTree& l2) {
    line.insert(line.end(), l2.line.begin(), l2.line.end());
    return *this;
}

LineTree& LineTree::operator+(const LineTree& l2) {
    *this += l2;
    return *this;
}

BlockTree& BlockTree::operator+=(const std::shared_ptr<Tree>& tree) {
    trees.push_back(tree);
    return *this;
}

BlockTree& BlockTree::operator+=(const BlockTree& block) {
    trees.insert(trees.end(), block.trees.begin(), block.trees.end());
    return *this;
}

std::string LineTree::to_string(int indent) {
    std::string str("");
    for(Token token : line) {
        str.append(token.to_string());
    }
    return INDENT_STR(indent) + str + "\n";
}

std::string BlockTree::to_string(int indent) {
    std::string str("");
    std::string indent_str(INDENT_SIZE*indent, ' ');
    for(auto tree : trees) {
        str.append(tree->to_string(indent));
    }
    return str;
}