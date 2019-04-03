# CMakeLists for sdf_ttf
PROJECT ( SDL_TTF )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET                         ( name      sdl_ttf )
ADD_LIBRARY                 ( ${name}   STATIC      SDL_ttf.c )
ADD_SUBDIRECTORY            (   ../freetype         ${CMAKE_BINARY_DIR}/freetype )
INCLUDE_DIRECTORIES         ( ../sdl/include )
TARGET_LINK_LIBRARIES       ( ${name}   freetype )
INSTALL                     ( TARGETS   ${name}     DESTINATION lib)
TARGET_INCLUDE_DIRECTORIES  ( ${name}   PUBLIC . )
