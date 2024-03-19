
#include <iostream>
#include <string>
#include <vector>

#include "module.hpp"
#include "data.hpp"

int main(int argc, char const *argv[]) {

    // Test module
    Identifier moduleName("testModule");
    Identifier rst("reset");
    Identifier mainReg("mainReg");
    Identifier connect("connect");
    LineTree emptyLine; // For swag points
    std::vector<Identifier> ports;
    ports.push_back(rst);
    BlockTree content;
    content += Input(rst);
    content += emptyLine;
    content += Reg(mainReg, 16);
    content += Wire(connect);
    Module m(moduleName, ports, content);

    std::cout << m.to_string() << std::endl;

    return 0;
}