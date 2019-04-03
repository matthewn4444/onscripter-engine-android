# CMakeLists for sdlimage
PROJECT ( SDL_IMAGE )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name sdl_image )
FILE ( GLOB_RECURSE	source_files *.c )
INCLUDE_DIRECTORIES (   ../sdl/include )
ADD_SUBDIRECTORY    (   ../jpeg         ${CMAKE_BINARY_DIR}/jpeg )
ADD_SUBDIRECTORY    (   ../png          ${CMAKE_BINARY_DIR}/png )

ADD_LIBRARY     ( ${name} STATIC ${source_files} )
SET_TARGET_PROPERTIES   ( ${name} PROPERTIES COMPILE_FLAGS "-DLOAD_PNG -DLOAD_JPG -DLOAD_GIF -DLOAD_BMP")

TARGET_LINK_LIBRARIES(${name} png jpeg)

INSTALL (TARGETS ${name} DESTINATION lib)

TARGET_INCLUDE_DIRECTORIES ( ${name} PUBLIC . )
