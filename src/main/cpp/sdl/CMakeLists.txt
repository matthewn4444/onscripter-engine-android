# CMakeLists for libsdl
PROJECT ( SDL )
CMAKE_MINIMUM_REQUIRED(VERSION 3.0)

SET ( name sdl )
FILE ( GLOB source_files    src/*.c
                            src/audio/*.c
                            src/cpuinfo/*.c
                            src/events/*.c
                            src/file/*.c
                            src/haptic/*.c
                            src/joystick/*.c
                            src/stdlib/*.c
                            src/thread/*.c
                            src/timer/*.c
                            src/video/*.c
                            src/main/*.c
                            src/power/*.c
                            src/thread/pthread/*.c
                            src/timer/unix/*.c
                            src/audio/android/*.c
                            src/video/android/*.c
                            src/joystick/dummy/*.c
                            src/haptic/dummy/*.c
                            src/loadso/dlopen/*.c
                            src/atomic/dummy/*.c
                            src/cdrom/*.c
                            src/cdrom/dummy/*.c)

INCLUDE_DIRECTORIES     ( include )
ADD_LIBRARY             ( ${name} SHARED ${source_files} )
SET_TARGET_PROPERTIES	( ${name} PROPERTIES OUTPUT_NAME "${name}" )
SET_TARGET_PROPERTIES   ( ${name} PROPERTIES COMPILE_FLAGS "\
                            -DSDL_JAVA_PACKAGE_PATH=${SDL_JAVA_PACKAGE_PATH} \
                            -DSDL_CURDIR_PATH=${SDL_CURDIR_PATH} \
                            -DSDL_TRACKBALL_KEYUP_DELAY=${SDL_TRACKBALL_KEYUP_DELAY}" )

TARGET_LINK_LIBRARIES(${name} log EGL GLESv1_CM )
INSTALL (   TARGETS ${name}
            LIBRARY DESTINATION lib
            RUNTIME DESTINATION bin )

TARGET_INCLUDE_DIRECTORIES ( ${name} PUBLIC include )
