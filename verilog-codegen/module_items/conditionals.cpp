#include "conditionals.hpp"

IfElse::IfElse(LineTree condition, Tree& ifBody, Tree& elseBody):
    condition(condition), ifBody(ifBody), elseBody(elseBody) {
}

std::string IfElse::to_string(int indent) {
    std::string str("");
    LineTree ifLine;
    ifLine += ifKw + ws + lPar + condition + rPar + ws + begin;
    LineTree elseLine;
    elseLine += end + ws + elseKw + ws + begin;
    str.append(ifLine.to_string(indent));
    str.append(ifBody.to_string(indent + 1));
    str.append(elseLine.to_string(indent));
    str.append(elseBody.to_string(indent + 1));
    str.append((LineTree() += end).to_string(indent));
    return str;
}

