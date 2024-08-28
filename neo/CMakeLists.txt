cmake_minimum_required(VERSION 2.8.12 FATAL_ERROR) # 2.6
project(doom3sdk)

# -DCMAKE_TOOLCHAIN_FILE=D:/project/c/vcpkg/scripts/buildsystems/vcpkg.cmake
set(COMPILE_CURL "1")
set(CORE_DEFINES "-D__DOOM_DLL__ -D_SDK")
if(ANDROID)
	add_definitions(-D__ANDROID__)
	add_definitions(-D_NO_PTHREAD_CANCEL)
	set(CORE_DEFINES "${CORE_DEFINES} -DNO_ALSA -D_NO_GAMMA")
elseif(WIN32)
	find_package(CURL QUIET)
	if(CURL_FOUND)
		set(COMPILE_CURL "1")
		include_directories(${CURL_INCLUDE_DIR})
		message(STATUS "libcurl found and enabled")
	else()
		message(WARNING "libcurl not found, server downloads won't be available (apart from that doom3 will work)")
		set(CURL_LIBRARY "")
		set(COMPILE_CURL "0")
	endif()
	set(CORE_DEFINES "${CORE_DEFINES} -DNO_ALSA")
else()
	set(CORE_DEFINES "${CORE_DEFINES} -D_NO_GAMMA")
	# apt-get install libasound2-dev # sound alsa support
	# apt-get install libgles-dev # GLES2/GLES3/EGL headers
endif()

add_definitions(-D_K_CLANG) # TODO
add_definitions(-DID_ENABLE_CURL=${COMPILE_CURL})

set(CMAKE_VERBOSE_MAKEFILE ON)

# set(CMAKE_BUILD_TYPE "Debug")
# TODO

# osx: place game .dylib's in the bundle (next to the binary)
# osx: -weak_framework ?

# maybe add these as options:
# TARGET_MONO
# SETUP
# SDK				-D_D3SDK

# don't add these as options, but document them?
# IDNET_HOST		-DIDNET_HOST=\\"%s\\"' % IDNET_HOST
# DEBUG_MEMORY		-DID_DEBUG_MEMORY', '-DID_REDIRECT_NEWDELETE
# LIBC_MALLOC		-DUSE_LIBC_MALLOC=0
# ID_NOLANADDRESS	-DID_NOLANADDRESS

# fallback for cmake versions without add_compile_options
if(NOT COMMAND add_compile_options)
	function(add_compile_options)
		foreach(arg ${ARGN})
			set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${arg}" PARENT_SCOPE)
			set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${arg}" PARENT_SCOPE)
		endforeach()
	endfunction()
endif()

option(LINUX_RELEASE_BINS "Set RPATH to \$ORIGIN/libs/ for Linux binary releases" OFF)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/sys/cmake")

if(LINUX_RELEASE_BINS)
	message(STATUS "Setting RPATH to \$ORIGIN/libs/ so you can put dependencies in there")
	set(CMAKE_SKIP_RPATH OFF CACHE BOOL "Skip RPATH" FORCE)
	set(CMAKE_INSTALL_RPATH_USE_LINK_PATH FALSE)
	set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
	set(CMAKE_INSTALL_RPATH "$ORIGIN/libs")
	set(ldflags "${ldflags} -Wl,-z,origin") # not sure this is even needed, but shouldn't hurt
else()
	set(CMAKE_SKIP_RPATH ON CACHE BOOL "Skip RPATH" FORCE)
endif()

set(IDTECH4LIBRARY "idtech4")

include(CheckCXXCompilerFlag)
include(GNUInstallDirs OPTIONAL RESULT_VARIABLE GNUINSTALLDIRS)

option(DEDICATED							"Build the dedicated server" 												OFF)
option(ONATIVE								"Optimize for the host CPU" 												OFF)
option(REPRODUCIBLE_BUILD 					"Replace __DATE__ and __TIME__ by hardcoded values for reproducible builds" OFF)

# Extras engine extensions
if(ANDROID)
	option(OPENSLES							"OpenSLES support"															ON)
endif()
option(OPENGLES3							"OpenGLES3 support"															ON)
option(OPENAL								"OpenAL(soft, OpenSL as backend) support"									ON)
option(MULTITHREAD							"Multi-Thread support"														ON)
option(USE_STB_IMAGE						"Using stb_image"															ON)
option(SUPPORT_OBJ							"Support obj model"															ON)
option(SUPPORT_DAE							"Support dae model" 														ON)
option(SHADOW_MAPPING						"Shadow mapping support" 													ON)
option(NO_LIGHT								"r_noLight support" 														ON)
option(STENCIL_SHADOW_IMPROVE				"Improve stencil shadow(translucent, soft(Only OpenGLES3.1+))" 				ON)
option(EXTRAS_TOOLS							"Add extras commands" 														OFF)
option(USE_MINIZ							"use miniz and minizip" 													OFF)
option(USE_STB_OGG							"use stb_vorbis" 															OFF)

# DOOM3 cores
option(BASE									"Build the base game code" 													ON)
option(D3XP									"Build the d3xp game code" 													ON)
option(D3_MOD_FULL_BODY_AWARENESS			"Build doom3 full body awareness mod support" 								ON)

# Print env
if(OPENSLES)
	add_definitions(-D_OPENSLES)
	message(STATUS "Build OpenSLES support")
endif()

if(MULTITHREAD)
	add_definitions(-D_MULTITHREAD)
	message(STATUS "Build Multi-Thread support")
endif()

if(OPENGLES3)
	add_definitions(-D_OPENGLES3)
	message(STATUS "Build OpenGLES3 support")
endif()

if(EXTRAS_TOOLS)
	add_definitions(-D_EXTRAS_TOOLS)
	message(STATUS "Build extras tools support")
endif()

# Using traditional C++98
set(CMAKE_CXX_STANDARD "98")

if(NOT CMAKE_SYSTEM_PROCESSOR)
	message(FATAL_ERROR "No target CPU architecture set")
endif()

if(NOT CMAKE_SYSTEM_NAME)
	message(FATAL_ERROR "No target OS set")
endif()

# target cpu
set(cpu ${CMAKE_SYSTEM_PROCESSOR})

# Originally, ${CMAKE_SYSTEM_PROCESSOR} was supposed to contain the *target* CPU, according to CMake's documentation.
# As far as I can tell this has always been broken (always returns host CPU) at least on Windows
# (see e.g. https://cmake.org/pipermail/cmake-developers/2014-September/011405.html) and wasn't reliable on
# other systems either, for example on Linux with 32bit userland but 64bit kernel it returned the kernel CPU type
# (e.g. x86_64 instead of i686). Instead of fixing this, CMake eventually updated their documentation in 3.20,
# now it's officially the same as CMAKE_HOST_SYSTEM_PROCESSOR except when cross-compiling (where it's explicitly set)
# So we gotta figure out the actual target CPU type ourselves.. (why am I sticking to this garbage buildsystem?)
if(NOT (CMAKE_SYSTEM_PROCESSOR STREQUAL CMAKE_HOST_SYSTEM_PROCESSOR))
	# special case: cross-compiling, here CMAKE_SYSTEM_PROCESSOR should be correct, hopefully
	# (just leave cpu at ${CMAKE_SYSTEM_PROCESSOR})
else() # not MSVC and not cross-compiling, assume GCC or clang (-compatible), seems to work for MinGW as well
	execute_process(COMMAND ${CMAKE_C_COMPILER} "-dumpmachine"
	                RESULT_VARIABLE cc_dumpmachine_res
	                OUTPUT_VARIABLE cc_dumpmachine_out)
	if(cc_dumpmachine_res EQUAL 0)
		string(STRIP ${cc_dumpmachine_out} cc_dumpmachine_out) # get rid of trailing newline
		message(STATUS "`${CMAKE_C_COMPILER} -dumpmachine` says: \"${cc_dumpmachine_out}\"")
		# gcc -dumpmachine and clang -dumpmachine seem to print something like "x86_64-linux-gnu" (gcc)
		# or "x64_64-pc-linux-gnu" (clang) or "i686-w64-mingw32" (32bit mingw-w64) i.e. starting with the CPU,
		# then "-" and then OS or whatever - so use everything up to first "-"
		string(REGEX MATCH "^[^-]+" cpu ${cc_dumpmachine_out})
		message(STATUS "  => CPU architecture extracted from that: \"${cpu}\"")
	else()
		message(WARNING "${CMAKE_C_COMPILER} -dumpmachine failed with error (code) ${cc_dumpmachine_res}")
		message(WARNING "will use the (sometimes incorrect) CMAKE_SYSTEM_PROCESSOR (${cpu}) to determine D3_ARCH")
	endif()
endif()

if(cpu STREQUAL "powerpc")
	set(cpu "ppc")
elseif(cpu STREQUAL "aarch64")
	# "arm64" is more obvious, and some operating systems (like macOS) use it instead of "aarch64"
	set(cpu "arm64")
elseif(cpu MATCHES "i.86")
	set(cpu "x86")
elseif(cpu MATCHES "[aA][mM][dD]64" OR cpu MATCHES "[xX]64")
	set(cpu "x86_64")
elseif(cpu MATCHES "[aA][rR][mM].*") # some kind of arm..
	# On 32bit Raspbian gcc -dumpmachine returns sth starting with "arm-",
	# while clang -dumpmachine says "arm6k-..." - try to unify that to "arm"
	if(CMAKE_SIZEOF_VOID_P EQUAL 8) # sizeof(void*) == 8 => must be arm64
		set(cpu "arm64")
	else() # should be 32bit arm then (probably "armv7l" "armv6k" or sth like that)
		set(cpu "arm")
	endif()
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "build/${CMAKE_BUILD_TYPE}/${CMAKE_ANDROID_ARCH_ABI}")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "build/${CMAKE_BUILD_TYPE}/${CMAKE_ANDROID_ARCH_ABI}")

add_definitions(-DD3_ARCH="${cpu}" -DD3_SIZEOFPTR=${CMAKE_SIZEOF_VOID_P})
# Stores in variable <var> either 1 or 0 indicating whether the target architecture is big or little endian.
# test_big_endian(big_endian)
if(CMAKE_CXX_BYTE_ORDER EQUAL BIG_ENDIAN)
	set(big_endian 1)
else()
	set(big_endian 0)
endif()
add_definitions(-DBUILD_IS_BIG_ENDIAN=${big_endian})

# target os
if(APPLE)
	set(os "macosx")
else()
	string(TOLOWER "${CMAKE_SYSTEM_NAME}" os)
endif()

add_definitions(-DD3_OSTYPE="${os}")

message(STATUS "Setting -DD3_ARCH=\"${cpu}\" -DD3_SIZEOFPTR=${CMAKE_SIZEOF_VOID_P} -DD3_OSTYPE=\"${os}\" ")

if(cpu MATCHES ".*64.*" AND NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
	# tough luck if some CPU architecture has "64" in its name but uses 32bit pointers
	message(SEND_ERROR "CMake thinks sizeof(void*) == 4, but the target CPU looks like a 64bit CPU!")
	message(FATAL_ERROR "If you're building in a 32bit chroot on a 64bit host, switch to it with 'linux32 chroot' or at least call cmake with linux32 (or your OSs equivalent)!")
endif()

# build type
if(NOT CMAKE_BUILD_TYPE)
	set(CMAKE_BUILD_TYPE "Release")
endif()

# precompiled libraries from the dhewm3-libs repo
#[[if(DHEWM3LIBS)
	if(CMAKE_CROSSCOMPILING)
		set(CMAKE_FIND_ROOT_PATH ${DHEWM3LIBS})
	else()
		set(ENV{CMAKE_PREFIX_PATH} ${DHEWM3LIBS})
	endif()

	# these are too stupid, give them a hint
	set(ENV{OPENALDIR} ${DHEWM3LIBS})
	set(ENV{SDLDIR} ${DHEWM3LIBS})
	set(ENV{SDL2DIR} ${DHEWM3LIBS})
endif()]]

if(CMAKE_MAJOR_VERSION LESS 3 OR ( CMAKE_MAJOR_VERSION EQUAL 3 AND CMAKE_MINOR_VERSION LESS 8 ))
	# cmake < 3.8 doesn't support source_group(TREE ...) so replace it with a dummy
	# (it's only cosmetical anyway, to make source files show up properly in Visual Studio)
	function(source_group)
	endfunction()
	message(STATUS "Using CMake < 3.8, doesn't support source_group(TREE ...), replacing it with a dummy")
	message(STATUS "  (this is only relevants for IDEs, doesn't matter for just compiling doom3)")
#else()
#	message(STATUS "Using CMake >= 3.8, supports source_group(TREE ...)")
endif()

# libs
find_package(ZLIB REQUIRED)
include_directories(${ZLIB_INCLUDE_DIRS})

# OpenAL using internal OpenAL-soft
#[[find_package(OpenAL REQUIRED)
include_directories(${OPENAL_INCLUDE_DIR})]]


if(WIN32)
	# skip SDL2main
	if(APPLE OR WIN32)
		set(SDL2_BUILDING_LIBRARY TRUE)
	endif()
	find_package(SDL2 REQUIRED)
	include_directories(${SDL2_INCLUDE_DIR})
	set(SDLx_LIBRARY ${SDL2_LIBRARIES})
	add_definitions(-D_SDL)
	include_directories("externlibs/include")
endif()

if(REPRODUCIBLE_BUILD)
	# don't use __DATE__ and __TIME__ macros so builds are reproducible
	add_definitions(-DID_REPRODUCIBLE_BUILD)
endif()


# cURL using internal
#[[find_package(CURL QUIET)
if(CURL_FOUND)
	set(ID_ENABLE_CURL ON)
	include_directories(${CURL_INCLUDE_DIR})
	message(STATUS "libcurl found and enabled")
else()
	message(WARNING "libcurl not found, server downloads won't be available (apart from that doom3 will work)")
	set(ID_ENABLE_CURL OFF)
	set(CURL_LIBRARY "")
endif()]]

if(NOT MSVC) # not MSVC

	if(NOT WIN32 AND NOT ANDROID)
		# libbacktrace support - TODO: might work with MinGW? we don't have a crash handler for win32 though..
		include(CheckCSourceCompiles)
		set(CMAKE_REQUIRED_LIBRARIES backtrace)
		check_c_source_compiles( "#include <backtrace.h>
	int main() { backtrace_create_state(NULL, 0, NULL, NULL); return 0; }" HAVE_LIBBACKTRACE )
		unset(CMAKE_REQUIRED_LIBRARIES)

		if(HAVE_LIBBACKTRACE)
			set(sys_libs ${sys_libs} backtrace)
			add_definitions(-DD3_HAVE_LIBBACKTRACE)
			message(STATUS "Using libbacktrace")
		else()
			message(WARNING "libbacktrace wasn't found. It's not required but recommended, because it provides useful backtraces if doom3 crashes")
		endif()
	endif() # NOT WIN32

endif() # not MSVC

# compiler specific flags
if(CMAKE_COMPILER_IS_GNUCC OR CMAKE_C_COMPILER_ID STREQUAL "Clang")
	add_compile_options(-pipe)
	#k add_compile_options(-Wall)

	if(NOT CMAKE_CROSSCOMPILING AND ONATIVE)
		add_compile_options(-march=native)
	elseif(NOT APPLE AND cpu STREQUAL "x86")
		add_compile_options(-march=pentium3)
	endif()

	set(CMAKE_C_FLAGS_DEBUG "-g -ggdb -D_DEBUG -O0 -fPIC")
	set(CMAKE_C_FLAGS_DEBUGALL "-g -ggdb -D_DEBUG -fPIC")
	set(CMAKE_C_FLAGS_PROFILE "-g -ggdb -D_DEBUG -O1 -fno-omit-frame-pointer -fPIC")
	set(CMAKE_C_FLAGS_RELEASE "-O2 -fno-math-errno -fno-trapping-math -fomit-frame-pointer -fPIC")
	set(CMAKE_C_FLAGS_RELWITHDEBINFO "-g -ggdb -O2 -fno-math-errno -fno-trapping-math -fno-omit-frame-pointer -fPIC")
	set(CMAKE_C_FLAGS_MINSIZEREL "-Os -fno-math-errno -fno-trapping-math -fomit-frame-pointer -fPIC")

	set(CMAKE_CXX_FLAGS_DEBUGALL ${CMAKE_C_FLAGS_DEBUGALL})
	set(CMAKE_CXX_FLAGS_PROFILE ${CMAKE_C_FLAGS_PROFILE})

	add_compile_options(-fno-strict-aliasing)
	# dear idiot compilers, don't fuck up math code with useless FMA "optimizations"
	# (https://gcc.gnu.org/bugzilla/show_bug.cgi?id=100839)
	add_compile_options(-ffp-contract=off)

	if(NOT AROS)
		CHECK_CXX_COMPILER_FLAG("-fvisibility=hidden" cxx_has_fvisibility)
		if(NOT cxx_has_fvisibility)
			message(FATAL_ERROR "Compiler does not support -fvisibility")
		endif()
		add_compile_options(-fvisibility=hidden)
	endif()

	# TODO fix these warnings
	add_compile_options(-Wno-sign-compare)
	add_compile_options(-Wno-switch)
	add_compile_options(-Wno-format-security)

	#k ignore these
	add_compile_options(-Wno-unknown-pragmas)
	add_compile_options(-Wno-unused-private-field)
	add_compile_options(-Wno-unused-variable)
	#add_compile_options(-Wall)
	#add_compile_options(-w)
	add_compile_options(-Wno-local-type-template-args -Wno-parentheses-equality -Wno-unused-value)
	add_compile_options(-Wno-self-assign-field -Wno-logical-not-parentheses -Wno-overloaded-virtual -Wno-c++11-compat-deprecated-writable-strings)
	add_compile_options(-Wno-undefined-inline -Wno-null-conversion -Wno-extra-tokens -Wno-c++11-extensions)
	if(NOT ANDROID)
		CHECK_CXX_COMPILER_FLAG("-Woverloaded-virtual" cxx_has_Woverload_virtual)
		if(cxx_has_Woverload_virtual)
			add_compile_options(-Woverloaded-virtual)
		endif()
	endif()

	# shut up about using memcpy() on classes, in the cases doom3 uses it it seems to be fine
	CHECK_CXX_COMPILER_FLAG("-Wno-class-memaccess" cxx_has_Wno-class-memaccess)
	if(cxx_has_Wno-class-memaccess)
		add_compile_options(-Wno-class-memaccess)
	endif()

	if(AROS)
		set(CMAKE_SHARED_LIBRARY_SUFFIX ".aros-${cpu}")
		add_definitions(-DIOAPI_NO_64)
	elseif(APPLE)
		add_definitions(-DMACOS_X=1)

		if(cpu STREQUAL "x86_64")
			add_compile_options(-arch x86_64 -mmacosx-version-min=10.9)
			set(ldflags "${ldflags} -arch x86_64 -mmacosx-version-min=10.9")
		elseif(cpu STREQUAL "arm64")
			add_compile_options(-arch arm64 -mmacosx-version-min=11.0)
			set(ldflags "${ldflags} -arch arm64 -mmacosx-version-min=11.0")
		elseif(cpu STREQUAL "x86")
			CHECK_CXX_COMPILER_FLAG("-arch i386" cxx_has_arch_i386)
			if(cxx_has_arch_i386)
				add_compile_options(-arch i386)
				set(ldflags "${ldflags} -arch i386")
			endif()

			add_compile_options(-mmacosx-version-min=10.4)
			set(ldflags "${ldflags} -mmacosx-version-min=10.4")
		elseif(cpu STREQUAL "ppc")
			CHECK_CXX_COMPILER_FLAG("-arch ppc" cxx_has_arch_ppc)
			if(cxx_has_arch_ppc)
				add_compile_options(-arch ppc -mone-byte-bool)
				set(ldflags "${ldflags} -arch ppc -mone-byte-bool")
			endif()

			add_compile_options(-mmacosx-version-min=10.4)
			set(ldflags "${ldflags} -mmacosx-version-min=10.4")
		else()
			message(FATAL_ERROR "Unsupported CPU architecture for OSX")
		endif()

		set(sys_libs ${sys_libs} "-framework Carbon -framework Cocoa -framework IOKit")
	elseif(WIN32)
		set(ldflags "${ldflags} -static-libgcc -static-libstdc++")
	elseif(os STREQUAL "linux")
		set(sys_libs ${sys_libs} dl)
	elseif(os STREQUAL "freebsd")
		find_library(EXECINFO_LIBRARIES execinfo /usr/lib /usr/local/lib)
		set(sys_libs ${sys_libs} ${EXECINFO_LIBRARIES})
	endif()
elseif(MSVC)
	add_compile_options(/MP) # parallel build (use all cores, or as many as configured in VS)

	add_compile_options(/W3) # TODO: was /W4, caused trouble with VS2019 (and/or its integrated CMake? or only HarrieVG's setup?)
	add_compile_options(/we4840) # treat as error when passing a class to a vararg-function (probably printf-like)
	# treat several kinds of truncating int<->pointer conversions as errors (for more 64bit-safety)
	add_compile_options(/we4306 /we4311 /we4312 /we4302)
	# ignore the following warnings:
	add_compile_options(/wd4100) # unreferenced formal parameter
	add_compile_options(/wd4127) # conditional expression is constant
	add_compile_options(/wd4244) # possible loss of data
	add_compile_options(/wd4245) # signed/unsigned mismatch
	add_compile_options(/wd4267) # possible loss of data
	add_compile_options(/wd4714) # 'function' marked as __forceinline not inlined
	add_compile_options(/wd4996) # 'function': was declared deprecated
	add_compile_options(/wd4068) # unknown pragma
	add_compile_options(/wd4458) # declaration of 'variable' hides class member
	add_definitions(-D_ALLOW_KEYWORD_MACROS) # because of the "#define private public" and "#define protected public" in TypeInfo.cpp
	set(CMAKE_C_FLAGS_DEBUG "-D_DEBUG /Od /Zi /MDd")
	set(CMAKE_C_FLAGS_RELEASE "/Ox /Oy /MD")
	set(CMAKE_C_FLAGS_RELWITHDEBINFO "/Ox /Oy /Zi /MD")
	set(CMAKE_C_FLAGS_MINSIZEREL "/Ox /Oy /Os /MD")
else()
	message(FATAL_ERROR "Unsupported compiler")
endif()

set(CMAKE_CXX_FLAGS_DEBUG ${CMAKE_C_FLAGS_DEBUG})
set(CMAKE_CXX_FLAGS_RELWITHDEBINFO ${CMAKE_C_FLAGS_RELWITHDEBINFO})
# disable assertions in release builds
set(CMAKE_CXX_FLAGS_RELEASE "-DNDEBUG ${CMAKE_C_FLAGS_RELEASE}")
set(CMAKE_CXX_FLAGS_MINSIZEREL "-DNDEBUG ${CMAKE_C_FLAGS_MINSIZEREL}")

# mingw and msvc
if(WIN32)
	add_definitions(-DWINVER=0x0501)
	add_definitions(-D_WIN32_WINNT=0x0501)

	set(sys_libs ${sys_libs}
			winmm
			iphlpapi
			wsock32
			ole32
			dsound ksuser #k
			)
elseif(ANDROID)
	# set(sys_libs ${sys_libs} android dl EGL z)
else() #TODO linux
	set(sys_libs ${sys_libs} X11 dl EGL z pthread)
endif()

# fallback for cmake versions without GNUInstallDirs
if(GNUINSTALLDIRS MATCHES "NOTFOUND")
	set(CMAKE_INSTALL_BINDIR		"bin"
			CACHE PATH "user executables (bin)")
	set(CMAKE_INSTALL_LIBDIR		"lib${LIB_SUFFIX}"
			CACHE PATH "object code libraries (lib${LIB_SUFFIX})")
	set(CMAKE_INSTALL_DATAROOTDIR	"share"
			CACHE PATH "read-only architecture-independent data root (share)")
	set(CMAKE_INSTALL_DATADIR		"${CMAKE_INSTALL_DATAROOTDIR}"
			CACHE PATH "read-only architecture-independent data (DATAROOTDIR)")

	mark_as_advanced(CMAKE_INSTALL_BINDIR CMAKE_INSTALL_LIBDIR CMAKE_INSTALL_DATAROOTDIR CMAKE_INSTALL_DATADIR)
	foreach(dir BINDIR LIBDIR DATAROOTDIR DATADIR)
		if(NOT IS_ABSOLUTE ${CMAKE_INSTALL_${dir}})
			set(CMAKE_INSTALL_FULL_${dir} "${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_${dir}}")
		else()
			set(CMAKE_INSTALL_FULL_${dir} "${CMAKE_INSTALL_${dir}}")
		endif()
	endforeach()
endif()

set(bindir		"${CMAKE_INSTALL_FULL_BINDIR}")
set(libdir		"${CMAKE_INSTALL_FULL_LIBDIR}")
set(datadir		"${CMAKE_INSTALL_FULL_DATADIR}")

#[[configure_file(
		"${CMAKE_SOURCE_DIR}/config.h.in"
		"${CMAKE_BINARY_DIR}/config.h"
)]]

message(STATUS "Building ${CMAKE_BUILD_TYPE} for ${os}-${cpu}")

if(NOT APPLE AND NOT WIN32 AND NOT ANDROID)
	message(STATUS "The install target will use the following directories:")
	message(STATUS "  Binary directory:  ${bindir}")
	message(STATUS "  Library directory: ${libdir}")
	message(STATUS "  Data directory:    ${datadir}")
endif()

# I'm a bit sloppy with headers and just glob them in..
# they're only handled in CMake at all so they turn up in Visual Studio solutions..

# globs all the headers from ${PATHPREFIX}/ and adds them to ${SRCLIST}
function(add_globbed_headers SRCLIST PATHPREFIX)
	file(GLOB_RECURSE tmp_hdrs RELATIVE "${CMAKE_CURRENT_SOURCE_DIR}" "${PATHPREFIX}/*.h")
	set(${SRCLIST} ${tmp_hdrs} ${${SRCLIST}})
endfunction()

if(COMPILE_CURL STREQUAL "1")
	message(STATUS "Build cURL")
endif()

if(USE_STB_IMAGE)
	set(CORE_DEFINES "${CORE_DEFINES} -D_USING_STB")
	message(STATUS "Using stb jpeg image")
endif()
if(USE_STB_OGG)
	set(CORE_DEFINES "${CORE_DEFINES} -D_USING_STB_OGG")
	message(STATUS "Using stb oggvorbis")
endif()
if(SUPPORT_OBJ)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_OBJ")
	message(STATUS "Support obj model")
endif()
if(SUPPORT_DAE)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_DAE")
	message(STATUS "Support dae model")
endif()
if(SHADOW_MAPPING)
	set(CORE_DEFINES "${CORE_DEFINES} -D_SHADOW_MAPPING")
	message(STATUS "Add shadow mapping support")
endif()
if(NO_LIGHT)
	set(CORE_DEFINES "${CORE_DEFINES} -D_NO_LIGHT")
	message(STATUS "Add r_noLight support")
endif()
if(STENCIL_SHADOW_IMPROVE)
	set(CORE_DEFINES "${CORE_DEFINES} -D_STENCIL_SHADOW_IMPROVE")
	set(stencil_shadow_improve_desc "combine translucent ")
	if(OPENGLES3)
		set(CORE_DEFINES "${CORE_DEFINES} -D_SOFT_STENCIL_SHADOW")
		set(stencil_shadow_improve_desc "${stencil_shadow_improve_desc}soft ")
		message(STATUS "Add soft stencil shadow support")
	endif()
	message(STATUS "Add stencil shadow support: ${stencil_shadow_improve_desc}")
endif()
if(USE_MINIZ)
	message(STATUS "Using minizip and miniz instead of zlib")
endif()

if(OPENAL)
	add_definitions(-D_OPENAL -D_OPENAL_EFX -D_OPENAL_SOFT)
	message(STATUS "Build OpenAL(soft) support")
endif()
add_definitions(${CORE_DEFINES})

set(src_idlib
	idlib/bv/Bounds.cpp
	idlib/bv/Frustum.cpp
	idlib/bv/Sphere.cpp
	idlib/bv/Box.cpp
	idlib/geometry/DrawVert.cpp
	idlib/geometry/Winding2D.cpp
	idlib/geometry/Surface_SweptSpline.cpp
	idlib/geometry/Winding.cpp
	idlib/geometry/Surface.cpp
	idlib/geometry/Surface_Patch.cpp
	idlib/geometry/TraceModel.cpp
	idlib/geometry/JointTransform.cpp
	idlib/hashing/CRC32.cpp
	idlib/hashing/MD4.cpp
	idlib/hashing/MD5.cpp
	idlib/math/Angles.cpp
	idlib/math/Lcp.cpp
	idlib/math/Math.cpp
	idlib/math/Matrix.cpp
	idlib/math/Ode.cpp
	idlib/math/Plane.cpp
	idlib/math/Pluecker.cpp
	idlib/math/Polynomial.cpp
	idlib/math/Quat.cpp
	idlib/math/Rotation.cpp
	idlib/math/Simd.cpp
	idlib/math/Simd_Generic.cpp
	idlib/math/Simd_AltiVec.cpp
	idlib/math/Simd_MMX.cpp
	idlib/math/Simd_3DNow.cpp
	idlib/math/Simd_SSE.cpp
	idlib/math/Simd_SSE2.cpp
	idlib/math/Simd_SSE3.cpp
	idlib/math/Vector.cpp
	idlib/BitMsg.cpp
	idlib/LangDict.cpp
	idlib/Lexer.cpp
	idlib/Lib.cpp
	idlib/containers/HashIndex.cpp
	idlib/Dict.cpp
	idlib/Str.cpp
	idlib/Parser.cpp
	idlib/MapFile.cpp
	idlib/CmdArgs.cpp
	idlib/Token.cpp
	idlib/Base64.cpp
	idlib/Timer.cpp
	idlib/Heap.cpp
	idlib/bv/Frustum_gcc.cpp
)

add_globbed_headers(src_idlib "idlib")

function(set_src_dir DSTLIST PATHPREFIX SRCLIST)
	set(list, "")
	math(EXPR last_index "${ARGC}-1")
	foreach(index RANGE 2 ${last_index})
		list(APPEND list "${PATHPREFIX}/${ARGV${index}}")
	endforeach()
	set(${DSTLIST} ${list} PARENT_SCOPE)
endfunction()

# TODO: add_globbed_headers(src_tools "tools/guied")

include_directories(${CMAKE_BINARY_DIR})
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Build idlib static
# idSoftware
add_library(idlib STATIC ${src_idlib})
if(NOT MSVC)
	set_target_properties(idlib PROPERTIES COMPILE_FLAGS "-fPIC")
endif()

source_group(TREE ${CMAKE_CURRENT_SOURCE_DIR} PREFIX neo FILES ${src_idlib})

if(BASE)
	message(STATUS "Build base")
endif()
if(D3XP)
	message(STATUS "Build d3xp")
endif()

message(STATUS "C++ STL: ${CMAKE_ANDROID_STL_TYPE}")
message(STATUS "arch ABI: ${CMAKE_ANDROID_ARCH_ABI}")
message(STATUS "API level: ${CMAKE_SYSTEM_VERSION}")
message(STATUS "arm mode: ${CMAKE_ANDROID_ARM_MODE}")
message(STATUS "armv7-neon: ${CMAKE_ANDROID_ARM_NEON}")
message(STATUS "build type: ${CMAKE_BUILD_TYPE}")

message(STATUS "pointer size: ${CMAKE_SIZEOF_VOID_P}")
message(STATUS "CORE_DEFINES: ${CORE_DEFINES}")
message(STATUS "ldflags: ${ldflags}")

#message(FATAL_ERROR "STOP")

if(BASE)
	# base

	set(src_game
			AF.cpp
			AFEntity.cpp
			Actor.cpp
			Camera.cpp
			Entity.cpp
			BrittleFracture.cpp
			Fx.cpp
			GameEdit.cpp
			Game_local.cpp
			Game_network.cpp
			Item.cpp
			IK.cpp
			Light.cpp
			Misc.cpp
			Mover.cpp
			Moveable.cpp
			MultiplayerGame.cpp
			Player.cpp
			PlayerIcon.cpp
			PlayerView.cpp
			Projectile.cpp
			Pvs.cpp
			SecurityCamera.cpp
			SmokeParticles.cpp
			Sound.cpp
			Target.cpp
			Trigger.cpp
			Weapon.cpp
			WorldSpawn.cpp
			ai/AAS.cpp
			ai/AAS_debug.cpp
			ai/AAS_pathing.cpp
			ai/AAS_routing.cpp
			ai/AI.cpp
			ai/AI_events.cpp
			ai/AI_pathing.cpp
			ai/AI_Vagary.cpp
			gamesys/DebugGraph.cpp
			gamesys/Class.cpp
			gamesys/Event.cpp
			gamesys/SaveGame.cpp
			gamesys/SysCmds.cpp
			gamesys/SysCvar.cpp
			gamesys/TypeInfo.cpp
			anim/Anim.cpp
			anim/Anim_Blend.cpp
			anim/Anim_Import.cpp
			anim/Anim_Testmodel.cpp
			script/Script_Compiler.cpp
			script/Script_Interpreter.cpp
			script/Script_Program.cpp
			script/Script_Thread.cpp
			physics/Clip.cpp
			physics/Force.cpp
			physics/Force_Constant.cpp
			physics/Force_Drag.cpp
			physics/Force_Field.cpp
			physics/Force_Spring.cpp
			physics/Physics.cpp
			physics/Physics_AF.cpp
			physics/Physics_Actor.cpp
			physics/Physics_Base.cpp
			physics/Physics_Monster.cpp
			physics/Physics_Parametric.cpp
			physics/Physics_Player.cpp
			physics/Physics_RigidBody.cpp
			physics/Physics_Static.cpp
			physics/Physics_StaticMulti.cpp
			physics/Push.cpp
			)
	set_src_dir(src_game "game" ${src_game})
	add_globbed_headers(src_game "game")

	add_library(game SHARED ${src_game})
	
	source_group(TREE ${CMAKE_CURRENT_SOURCE_DIR} PREFIX neo FILES ${src_game})
	
	#set_target_properties(game PROPERTIES PREFIX "")
	set(d3_compile_definitions "GAME_DLL")
	if(D3_MOD_FULL_BODY_AWARENESS)
		set(d3_compile_definitions "${d3_compile_definitions};_MOD_FULL_BODY_AWARENESS")
		message(STATUS "Build doom3 full body awareness mod support")
	endif()
	set_target_properties(game PROPERTIES COMPILE_DEFINITIONS "${d3_compile_definitions}")
	target_include_directories(game PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/game")
	set_target_properties(game PROPERTIES LINK_FLAGS "${ldflags}")
	set_target_properties(game PROPERTIES INSTALL_NAME_DIR "@executable_path")
	target_link_libraries(game idlib)

	if(NOT APPLE AND NOT WIN32 AND NOT ANDROID)
		install(TARGETS game
				RUNTIME DESTINATION "${bindir}"
				LIBRARY DESTINATION "${libdir}"
				ARCHIVE DESTINATION "${libdir}"
				)
	endif()
endif()

if(D3XP)
	# d3xp
	set(src_d3xp
			AF.cpp
			AFEntity.cpp
			Actor.cpp
			Camera.cpp
			Entity.cpp
			BrittleFracture.cpp
			Fx.cpp
			GameEdit.cpp
			Game_local.cpp
			Game_network.cpp
			Item.cpp
			IK.cpp
			Light.cpp
			Misc.cpp
			Mover.cpp
			Moveable.cpp
			MultiplayerGame.cpp
			Player.cpp
			PlayerIcon.cpp
			PlayerView.cpp
			Projectile.cpp
			Pvs.cpp
			SecurityCamera.cpp
			SmokeParticles.cpp
			Sound.cpp
			Target.cpp
			Trigger.cpp
			Weapon.cpp
			WorldSpawn.cpp
			ai/AAS.cpp
			ai/AAS_debug.cpp
			ai/AAS_pathing.cpp
			ai/AAS_routing.cpp
			ai/AI.cpp
			ai/AI_events.cpp
			ai/AI_pathing.cpp
			ai/AI_Vagary.cpp
			gamesys/DebugGraph.cpp
			gamesys/Class.cpp
			gamesys/Event.cpp
			gamesys/SaveGame.cpp
			gamesys/SysCmds.cpp
			gamesys/SysCvar.cpp
			gamesys/TypeInfo.cpp
			anim/Anim.cpp
			anim/Anim_Blend.cpp
			anim/Anim_Import.cpp
			anim/Anim_Testmodel.cpp
			script/Script_Compiler.cpp
			script/Script_Interpreter.cpp
			script/Script_Program.cpp
			script/Script_Thread.cpp
			physics/Clip.cpp
			physics/Force.cpp
			physics/Force_Constant.cpp
			physics/Force_Drag.cpp
			physics/Force_Field.cpp
			physics/Force_Spring.cpp
			physics/Physics.cpp
			physics/Physics_AF.cpp
			physics/Physics_Actor.cpp
			physics/Physics_Base.cpp
			physics/Physics_Monster.cpp
			physics/Physics_Parametric.cpp
			physics/Physics_Player.cpp
			physics/Physics_RigidBody.cpp
			physics/Physics_Static.cpp
			physics/Physics_StaticMulti.cpp
			physics/Push.cpp
			Grabber.cpp
			physics/Force_Grab.cpp
			)
	set_src_dir(src_d3xp "d3xp" ${src_d3xp})
	add_globbed_headers(src_d3xp "d3xp")

	add_library(d3xp SHARED ${src_d3xp})
	source_group(TREE ${CMAKE_CURRENT_SOURCE_DIR} PREFIX neo FILES ${src_d3xp})
	#set_target_properties(d3xp PROPERTIES PREFIX "")
	set_target_properties(d3xp PROPERTIES COMPILE_DEFINITIONS "GAME_DLL;_D3XP;CTF")
	target_include_directories(d3xp PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/d3xp")
	set_target_properties(d3xp PROPERTIES LINK_FLAGS "${ldflags}")
	set_target_properties(d3xp PROPERTIES INSTALL_NAME_DIR "@executable_path")
	target_link_libraries(d3xp idlib)

	if(NOT APPLE AND NOT WIN32 AND NOT ANDROID)
		install(TARGETS d3xp
				RUNTIME DESTINATION "${bindir}"
				LIBRARY DESTINATION "${libdir}"
				ARCHIVE DESTINATION "${libdir}"
				)
	endif()
endif()
