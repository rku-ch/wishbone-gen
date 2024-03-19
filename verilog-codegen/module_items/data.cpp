#include "data.hpp"


Data::Data(Token type, Identifier id) {
    *this += type + ws + id + eol;

}

Data::Data(Token type, Identifier id, int range): Data(type, range-1, 0, id) {}

Data::Data(Token type, int msb, int lsb, Identifier id) {
    Token msb_expr(std::to_string(msb), false);
    Token lsb_expr(std::to_string(lsb), false);
    *this 
        += type + ws +  lBracket + msb_expr + colon + lsb_expr + rBracket + ws 
        + id + eol;
}