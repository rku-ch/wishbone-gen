add_library(pugixml STATIC 
    pugixml/src/pugixml.cpp
)

set_property(TARGET pugixml PROPERTY
   LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/pugixml")
set_property(TARGET pugixml PROPERTY FOLDER "dependencies")

target_include_directories(pugixml INTERFACE
  "${CMAKE_CURRENT_SOURCE_DIR}/pugixml/src"
)