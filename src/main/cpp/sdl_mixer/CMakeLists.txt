# CMakeLists for sdl_mixer
PROJECT ( SDL_MIXER )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name sdl_mixer )
FILE ( GLOB	source_files *.c )
INCLUDE_DIRECTORIES         (   ../
                                ../libmad-0.15.1b
                                ../sdl/include )
ADD_SUBDIRECTORY            (   ../tremor           ${CMAKE_BINARY_DIR}/tremor )
ADD_SUBDIRECTORY            (   ../libmad-0.15.1b   ${CMAKE_BINARY_DIR}/mad )

ADD_LIBRARY                 (   ${name} STATIC      ${source_files} )
SET_TARGET_PROPERTIES       (   ${name} PROPERTIES  COMPILE_FLAGS "-DWAV_MUSIC -DMP3_MAD_MUSIC \
                                                                   -DOGG_USE_TREMOR -DOGG_MUSIC")
TARGET_LINK_LIBRARIES       (   ${name} tremor mad )
INSTALL                     (   TARGETS ${name}     DESTINATION lib)
TARGET_INCLUDE_DIRECTORIES  (   ${name} PUBLIC      . )
