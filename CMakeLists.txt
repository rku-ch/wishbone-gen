cmake_minimum_required(VERSION 3.22.1)

project(wishbone-gen)

add_subdirectory(
    ext
    verilog-codegen
)

add_executable(wishbone-gen 
    src/main.cpp
)

target_link_libraries(wishbone-gen PUBLIC verilog-codegen pugixml)

target_include_directories(wishbone-gen PUBLIC 
    "${PROJECT_BINARY_DIR}"
)