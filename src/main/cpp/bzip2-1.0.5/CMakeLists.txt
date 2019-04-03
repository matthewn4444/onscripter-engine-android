# CMakeLists for bzip2
PROJECT ( BZ2 )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name bz2 )
FILE ( GLOB_RECURSE	source_files *.c )

ADD_LIBRARY                 ( ${name} STATIC        ${source_files} )
SET_TARGET_PROPERTIES       ( ${name} PROPERTIES    COMPILE_FLAGS "-D_FILE_OFFSET_BITS=64")
INSTALL                     ( TARGETS ${name}       DESTINATION lib)
TARGET_INCLUDE_DIRECTORIES  ( ${name} PUBLIC        . )
