# CMakeLists for jpeg
PROJECT ( JPEG )
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET ( name jpeg )
SET (source_files
    jcapimin.c jcapistd.c jccoefct.c jccolor.c jcdctmgr.c jchuff.c
	jcinit.c jcmainct.c jcmarker.c jcmaster.c jcomapi.c jcparam.c
	jcphuff.c jcprepct.c jcsample.c jctrans.c jdapimin.c jdapistd.c
	jdatadst.c jdatasrc.c jdcoefct.c jdcolor.c jddctmgr.c jdhuff.c
	jdinput.c jdmainct.c jdmarker.c jdmaster.c jdmerge.c jdphuff.c
	jdpostct.c jdsample.c jdtrans.c jerror.c jfdctflt.c jfdctfst.c
	jfdctint.c jidctflt.c jidctred.c jquant1.c jquant2.c jutils.c
	jmemmgr.c jmem-android.c jidctint.c jidctfst.c )

ADD_LIBRARY             ( ${name} STATIC ${source_files} )
SET_TARGET_PROPERTIES   ( ${name} PROPERTIES COMPILE_FLAGS "-DAVOID_TABLES -O3 -fstrict-aliasing")

INSTALL (TARGETS ${name} DESTINATION lib)

TARGET_INCLUDE_DIRECTORIES ( ${name} PUBLIC . )
