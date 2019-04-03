# CMakeLists for lua
PROJECT ( LUA )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name lua )
FILE ( GLOB_RECURSE	source_files src/*.c )

INCLUDE_DIRECTORIES     ( src )
ADD_LIBRARY             ( ${name} STATIC ${source_files} )
INSTALL (TARGETS ${name} DESTINATION lib)

TARGET_INCLUDE_DIRECTORIES  ( ${name} PUBLIC src etc )
