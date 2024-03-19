#include "module.hpp"

#include <fstream>

Module::Module(Identifier module_id, std::vector<Identifier> ports, BlockTree &content): content(content)  {
    module_declaration += module_id;
    if(ports.size() > 0) {
        module_declaration += lPar;
        for(Identifier port : ports) {
            module_declaration += port + coma;
        }
        module_declaration += rPar;
    }
    module_declaration += eol;    

};

std::string Module::to_string() {
    std::string str("");
    str.append(module_declaration.to_string() + "\n");
    str.append(content.to_string(1) + "\n");
    str.append(endmodule.to_string());
    return str;
}

void Module::output_to_file(std::string filename) {
    std::ofstream out(filename); 
    
    out << this->to_string();

    if(out.is_open()) {
        out.close();
    }
}