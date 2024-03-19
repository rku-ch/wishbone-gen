#include "tree_helper.hpp"

#define INDENT_SIZE 4


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

BlockTree& BlockTree::operator+=(const LineTree& line) {
    lines.push_back(line);
    return *this;
}

BlockTree& BlockTree::operator+=(const BlockTree& block) {
    lines.insert(lines.end(), block.lines.begin(), block.lines.end());
    return *this;
}

std::string LineTree::to_string() {
    std::string str("");
    for(Token token : line) {
        str.append(token.to_string());
    }
    return str;
}

std::string BlockTree::to_string() {
    std::string str("");
    for(LineTree line : lines) {
        str.append(line.to_string() + "\n");
    }
    return str;
}

std::string BlockTree::to_string(int indent) {
    std::string str("");
    std::string indent_str(INDENT_SIZE*indent, ' ');
    for(LineTree line : lines) {
        str.append(indent_str + line.to_string() + "\n");
    }
    return str;
}