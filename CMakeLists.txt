cmake_minimum_required(VERSION 3.22.1)

project(wishbone-gen)

add_subdirectory(verilog-codegen)

add_executable(wishbone-gen 
    src/main.cpp
)

target_link_libraries(wishbone-gen PUBLIC verilog-codegen)

target_include_directories(wishbone-gen PUBLIC 
    "${PROJECT_BINARY_DIR}"
)