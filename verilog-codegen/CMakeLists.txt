add_library(verilog-codegen 
    module.cpp
    token.cpp
    tree.cpp
    module_items/data.cpp
)

target_include_directories(verilog-codegen INTERFACE
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/module_items"
)