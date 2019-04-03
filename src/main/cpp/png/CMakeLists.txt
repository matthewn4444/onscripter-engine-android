# CMakeLists for png
PROJECT ( PNG )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name png )
SET ( source_files  png.c
                    pngerror.c
                    pnggccrd.c
                    pngget.c
                    pngmem.c
                    pngpread.c
                    pngread.c
                    pngrio.c
                    pngrtran.c
                    pngrutil.c
                    pngset.c
                    pngtrans.c
                    pngvcrd.c
                    pngwio.c
                    pngwrite.c
                    pngwtran.c
                    pngwutil.c )

ADD_LIBRARY ( ${name} STATIC ${source_files} )

INSTALL (TARGETS ${name} DESTINATION lib)

TARGET_INCLUDE_DIRECTORIES ( ${name} PUBLIC . )