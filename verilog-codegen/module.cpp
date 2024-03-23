#include "module.hpp"

#include <fstream>

Module::Module(Identifier module_id, std::vector<Identifier> ports, BlockTree &content): content(content)  {
    module_declaration += module_id;
    if(ports.size() > 0) {
        module_declaration += lPar;
        for(int i = 0; i < ports.size()-1; i++) {
            module_declaration += ports[i] + coma;
        }
        module_declaration += ports[ports.size()-1];
        module_declaration += rPar;
    }
    module_declaration += eol;    

    };

std::string Module::to_string(int indent) {
    std::string str("");
    str.append(module_declaration.to_string(indent));
    str.append(content.to_string(indent + 1));
    str.append(endmodule.to_string(indent));
    return str;
}

void Module::output_to_file(std::string filename) {
    std::ofstream out(filename); 
    
    out << this->to_string();

    if(out.is_open()) {
        out.close();
    }
}