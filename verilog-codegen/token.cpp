#include "token.hpp"

std::unordered_set<std::string> Token::tokens;

Token::Token(std::string value, bool reserve): value(std::move(value)) {
    // The program should fail if we try to reuse a reseved token
    if(Token::tokens.find(value) != Token::tokens.end()) {
       throw std::runtime_error("Reusing a reserved token (keyword or operator) is not allowed");
    }
    if(reserve){
        Token::tokens.insert(this->value);
    }
}

Token::~Token(){
    if(Token::tokens.find(value) != Token::tokens.end()) {
        Token::tokens.erase(value);
    }
}

std::string Token::to_string() {
    return value;
}

/**
 * Instantiate reserved Token for (most) verilog keywords and operators
 * 
*/

// Shortcut to initialize keywords
#define KW(STR) Token STR(#STR, true);

// --- Keywords

KW(always)
KW(begin)
KW(end)
KW(endmodule)
KW(input)
KW(module)
KW(output)
KW(reg)
KW(wire)

// --- Operators

Token coma(",", true);
Token lPar("(", true);
Token rPar(")", true);
Token eol(";", true);