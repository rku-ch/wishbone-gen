
#include <iostream>
#include <string>
#include <vector>


#include <module.hpp>
#include <all_items.hpp>
#include <pugixml.hpp>


int main(int argc, char const *argv[]) {

    pugi::xml_document doc;
    pugi::xml_parse_result result = doc.load_file("../tests/memorymap.xml");

    std::cout << "Load result: " << result.description() << std::endl;

    std::cout << "Description: " << doc.child("memorymap").child("description").child_value() << std::endl;
    
    // Test module
    Identifier moduleName("testModule");
    Identifier rst("reset");
    Identifier mainReg("mainReg");
    Identifier connect("connect");
    LineTree emptyLine; // For swag points
    std::shared_ptr<LineTree> sharedEmptyLine = std::make_shared<LineTree>(emptyLine);
    std::vector<Identifier> ports;
    ports.push_back(rst);
    LineTree condition;
    condition += rst;
    BlockTree ifBody;
    ifBody += std::make_shared<LineTree>(condition);
    ifBody += sharedEmptyLine;
    BlockTree elseBody;
    elseBody += sharedEmptyLine;
    elseBody += std::make_shared<LineTree>(condition);
    IfElse ifElse(condition, ifBody, elseBody);
    BlockTree content;
    content += std::make_shared<LineTree>(Input(rst));
    content += sharedEmptyLine;
    content += std::make_shared<LineTree>(Reg(mainReg, 16));
    content += std::make_shared<LineTree>(Wire(connect));
    content += std::make_shared<IfElse>(ifElse);
    Module m(moduleName, ports, content);

    std::cout << m.to_string() << std::endl;

    return 0;
}