# CMakeLists for tremor
PROJECT ( TREMOR )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name tremor )
FILE ( GLOB_RECURSE	source_files *.c )

ADD_LIBRARY             ( ${name} STATIC ${source_files} )
SET_TARGET_PROPERTIES   ( ${name} PROPERTIES COMPILE_FLAGS "-DHAVE_ALLOCA_H")

INSTALL (TARGETS ${name} DESTINATION lib)
