#ifndef VERILOG_CODGEN_MODULE_INCLUDED
#define VERILOG_CODGEN_MODULE_INCLUDED

#include "identifier.hpp"
#include "tree_helper.hpp"


class Module : public Tree {
public:
    Module(Identifier module_id, std::vector<Identifier> ports, BlockTree &content);

    /**
     * Output the complete module to a file with the given name
    */
    void output_to_file(std::string filename);

    std::string to_string(int indent = 0);
private:
    LineTree module_declaration;


    BlockTree &content;
};

#endif