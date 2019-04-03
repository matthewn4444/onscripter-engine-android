# CMakeLists for freetype
PROJECT ( FREETYPE )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET     ( name freetype )
FILE    ( GLOB_RECURSE	source_files src/*.c )

INCLUDE_DIRECTORIES     ( include )
ADD_LIBRARY             ( ${name} STATIC ${source_files} )
SET_TARGET_PROPERTIES   ( ${name} PROPERTIES COMPILE_FLAGS "-DFT2_BUILD_LIBRARY")

INSTALL (TARGETS ${name} DESTINATION lib)

TARGET_INCLUDE_DIRECTORIES ( ${name} PUBLIC include )
