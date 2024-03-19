#ifndef VERILOG_CODGEN_IDENTIFIER_INCLUDED
#define VERILOG_CODGEN_IDENTIFIER_INCLUDED

#include "token.hpp"


/**
 * Represent a Verilog Identifier
*/
class Identifier : public Token {
public:
    Identifier(std::string id): Token(id, false) { };
private:
};

#endif