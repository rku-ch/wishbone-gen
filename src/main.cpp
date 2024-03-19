
#include <iostream>
#include <string>
#include <vector>

#include "module.hpp"

int main(int argc, char const *argv[]) {

    // Test module
    Identifier moduleName("testModule");
    std::vector<Identifier> noPorts;
    BlockTree noContent;
    Module m(moduleName, noPorts, noContent);

    std::cout << m.to_string() << std::endl;

    return 0;
}