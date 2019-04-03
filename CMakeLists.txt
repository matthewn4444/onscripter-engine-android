# Builds the ONScripter Project and its dependencies
CMAKE_MINIMUM_REQUIRED(VERSION 3.4.1)

# Enable Languages
ADD_DEFINITIONS(-DENABLE_ENGLISH)
ADD_DEFINITIONS(-DENABLE_KOREAN)
ADD_DEFINITIONS(-DENABLE_CHINESE)

# Configure the paths
SET ( SDL_JAVA_PACKAGE_PATH com_onscripter )
SET ( SDL_CURDIR_PATH /sdcard/ons )
SET ( SDL_TRACKBALL_KEYUP_DELAY 1 )

# Import libraries
SET(CPP_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp)
ADD_SUBDIRECTORY    (   ${CPP_DIR}/lua              ${CMAKE_BINARY_DIR}/lua )
ADD_SUBDIRECTORY    (   ${CPP_DIR}/sdl              ${CMAKE_BINARY_DIR}/sdl )
ADD_SUBDIRECTORY    (   ${CPP_DIR}/bzip2-1.0.5      ${CMAKE_BINARY_DIR}/bz2 )
ADD_SUBDIRECTORY    (   ${CPP_DIR}/sdl_image        ${CMAKE_BINARY_DIR}/sdl_image )
ADD_SUBDIRECTORY    (   ${CPP_DIR}/sdl_ttf          ${CMAKE_BINARY_DIR}/sdl_ttf )
ADD_SUBDIRECTORY    (   ${CPP_DIR}/sdl_mixer        ${CMAKE_BINARY_DIR}/sdl_mixer )

# Add all the sources to this project
ADD_LIBRARY (   onscripter SHARED   ${CPP_DIR}/sdl_main/sdl_main.c

                                    ${CPP_DIR}/onscripter/gbk2utf.cpp
                                    ${CPP_DIR}/onscripter/kr2utf.cpp
                                    ${CPP_DIR}/onscripter/sjis2utf16.cpp

                                    ${CPP_DIR}/onscripter/onscripter_main.cpp
                                    ${CPP_DIR}/onscripter/ONScripter.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_animation.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_command.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_effect.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_effect_breakup.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_event.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_file.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_file2.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_image.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_lut.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_rmenu.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_sound.cpp
                                    ${CPP_DIR}/onscripter/ONScripter_text.cpp

                                    ${CPP_DIR}/onscripter/ScriptDecoder.cpp
                                    ${CPP_DIR}/onscripter/ScriptException.cpp
                                    ${CPP_DIR}/onscripter/ScriptHandler.cpp
                                    ${CPP_DIR}/onscripter/ScriptParser.cpp
                                    ${CPP_DIR}/onscripter/ScriptParser_command.cpp

                                    ${CPP_DIR}/onscripter/SarReader.cpp
                                    ${CPP_DIR}/onscripter/resize_image.cpp
                                    ${CPP_DIR}/onscripter/AnimationInfo.cpp
                                    ${CPP_DIR}/onscripter/backtrace.cpp
                                    ${CPP_DIR}/onscripter/conv_shared.cpp
                                    ${CPP_DIR}/onscripter/DirectReader.cpp
                                    ${CPP_DIR}/onscripter/DirtyRect.cpp
                                    ${CPP_DIR}/onscripter/FontInfo.cpp
                                    ${CPP_DIR}/onscripter/LUAHandler.cpp
                                    ${CPP_DIR}/onscripter/NsaReader.cpp )

SET_TARGET_PROPERTIES   (   onscripter PROPERTIES COMPILE_FLAGS "\
                                        -DSDL_JAVA_PACKAGE_PATH=${SDL_JAVA_PACKAGE_PATH} \
                                        -DLINUX -DMP3_MAD -DPDA_AUTOSIZE -DUSE_OGG_VORBIS \
                                        -DINTEGER_OGG_VORBIS -DUTF8_FILESYSTEM -DUSE_LUA")

TARGET_LINK_LIBRARIES   (   onscripter  PRIVATE
                            sdl
                            lua
                            bz2
                            sdl_image
                            sdl_ttf
                            sdl_mixer
                            z )
