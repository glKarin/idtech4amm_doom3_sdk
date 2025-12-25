cmake_minimum_required(VERSION 2.8.12 FATAL_ERROR) # 2.6

project(doom3sdk)

option(IS_DHEWM3    	"If is Dhewm3 mod"      	ON)

set(IDTECH4AMM_ENGINE_PATCH 70)
set(IDTECH4AMM_ENGINE_VERSION "1.1.0harmattan${IDTECH4AMM_ENGINE_PATCH}")
if(CMAKE_BUILD_TYPE STREQUAL "Release")
string(TIMESTAMP IDTECH4AMM_BUILDTIME "%Y-%m-%d %H:%M:%S")
else()
file(TIMESTAMP ${CMAKE_CURRENT_SOURCE_DIR}/CMakeLists.txt IDTECH4AMM_BUILDTIME "%Y-%m-%d %H:%M:%S")
endif()
message(STATUS "idTech4A++")
message(STATUS "Build version: ${IDTECH4AMM_ENGINE_VERSION}")
message(STATUS "Build time: ${IDTECH4AMM_BUILDTIME}")

# -DCMAKE_TOOLCHAIN_FILE=D:/project/c/vcpkg/scripts/buildsystems/vcpkg.cmake

#set(CMAKE_VERBOSE_MAKEFILE ON)

# build type
if(NOT CMAKE_BUILD_TYPE)
	set(CMAKE_BUILD_TYPE "Release")
endif()
#set(CMAKE_BUILD_TYPE "RelWithDebInfo")

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "build/${CMAKE_BUILD_TYPE}/${CMAKE_ANDROID_ARCH_ABI}")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "build/${CMAKE_BUILD_TYPE}/${CMAKE_ANDROID_ARCH_ABI}")

option(USE_SYSTEM_OGGVORBIS    	"use system ogg/vorbis library"      	OFF)
option(USE_SYSTEM_CURL        	"use system curl library"      			OFF)
option(USE_SYSTEM_FREETYPE    	"use system freetype2 library"      	OFF)

set(CORE_DEFINES "-D__DOOM_DLL__ -D_SDK")
if(IS_DHEWM3)
	set(CORE_DEFINES "${CORE_DEFINES} -D_DHEWM3")
endif()

add_definitions(-D_IDTECH4AMM_PATCH=${IDTECH4AMM_ENGINE_PATCH})
add_definitions(-D_IDTECH4AMM_VERSION="${IDTECH4AMM_ENGINE_VERSION}")
add_definitions(-D_IDTECH4AMM_BUILD="${IDTECH4AMM_BUILDTIME}")
if(ANDROID)
	add_definitions(-D__ANDROID__)
	add_definitions(-D_NO_PTHREAD_CANCEL)
	set(CORE_DEFINES "${CORE_DEFINES} -DNO_ALSA -D_NO_GAMMA")
elseif(WIN32)
	set(CORE_DEFINES "${CORE_DEFINES} -DNO_ALSA")
else()
	set(CORE_DEFINES "${CORE_DEFINES} -D_NO_GAMMA")
	# apt-get install libasound2-dev # sound alsa support
	# apt-get install libgles-dev # GLES2/GLES3/EGL headers
endif()

add_definitions(-D_K_CLANG) # TODO
add_definitions(-DID_ENABLE_CURL=1)

set(COMPILE_OGGVORBIS       TRUE)
set(COMPILE_CURL        	TRUE)
set(COMPILE_FREETYPE    	FALSE)

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

include(CheckCXXCompilerFlag)
include(GNUInstallDirs OPTIONAL RESULT_VARIABLE GNUINSTALLDIRS)

if(MSVC)
	option(TOOLS							"Build the tools game code (Visual Studio+SDL2 only)" 						OFF)
endif()
option(DEDICATED							"Build the dedicated server" 												OFF)
option(ONATIVE								"Optimize for the host CPU" 												OFF)
option(REPRODUCIBLE_BUILD 					"Replace __DATE__ and __TIME__ by hardcoded values for reproducible builds" OFF)
option(CONSOLE								"Run on console" 															OFF)
if(LINUX AND NOT ANDROID)
	option(USE_X11							"Use X11"																	OFF) # else SDL2
	option(USE_LOCAL_SDL					"Use local SDL library"														OFF) # else system
endif()
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
option(USE_MINIZ							"use miniz and minizip" 													ON)
option(USE_STB_OGG							"use stb_vorbis" 															OFF) #! some ogg file can not be load
option(D3BFG_OCCLUSION_CULLING				"DOOM3-BFG occlusion culling support" 										ON)
option(WIDE_CHARACTER_LANGUAGE				"Wide-character language font support" 										ON)
option(D3BFG_NEW_FONT						"DOOM3-BFG new font support"												ON)
option(GLSL_PROGRAM							"GLSL program on new material stage support"								ON)
option(GLOBAL_ILLUMINATION					"Render global illumination before draw interactions"						ON)
option(RETRO_POSTPROCESS					"Add retro postprocess rendering support"								   	ON)
option(IMGUI								"Add imGUI support"								   							ON)
option(GL_INT_INDEX							"Use 4 bytes index on OpenGL"								   				ON)
option(MP3_SUPPORT							"Add mp3 sound support"								   						ON)
option(SUPPORT_MD5V6						"Add 2002 E3 demo md5mesh/md5anim v6 version animation model converter"		ON)
option(SUPPORT_PSK							"Support Unreal engine psk/psa animation/static model"						ON)
option(SUPPORT_IQM							"Support iqm animation/static model"										ON)
option(SUPPORT_SMD							"Support Source engine smd animation/static model"							ON)
option(SUPPORT_GLTF							"Support Khronos gltf/glb animation/static model"							ON)
option(SUPPORT_FBX							"Support Autodesk fbx animation/static model"								ON)

# DOOM3 cores
option(BASE									"Build the base game code" 													ON)
option(D3XP									"Build the d3xp game code" 													OFF)
option(D3_MOD_FULL_BODY_AWARENESS			"Build doom3 full body awareness mod support" 								ON)
option(D3_MOD_BOTS							"Build doom3 bots mod support" 												ON)

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
	message(STATUS "Build extras engine tools support")
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
	if(MSVC)
		if(CMAKE_SIZEOF_VOID_P EQUAL 8) # sizeof(void*) == 8 => must be amd64
	set(cpu "x86_64")
		else() # should be i386
			set(cpu "x86")
		endif()
	else()
	set(cpu "x86_64")
	endif()
elseif(cpu MATCHES "[aA][rR][mM].*") # some kind of arm..
	# On 32bit Raspbian gcc -dumpmachine returns sth starting with "arm-",
	# while clang -dumpmachine says "arm6k-..." - try to unify that to "arm"
	if(CMAKE_SIZEOF_VOID_P EQUAL 8) # sizeof(void*) == 8 => must be arm64
		set(cpu "arm64")
	else() # should be 32bit arm then (probably "armv7l" "armv6k" or sth like that)
		set(cpu "arm")
	endif()
endif()

# target os
if(APPLE)
	set(os "macosx")
else()
	string(TOLOWER "${CMAKE_SYSTEM_NAME}" os)
endif()

# check char is singed or unsigned
if(cpu MATCHES "arm.*")
	message(STATUS "Target char is unsigned")
	set(char_is_singed 0)
	set(char_is_unsigned 1)
else()
	message(STATUS "Target char is signed")
	set(char_is_singed 1)
	set(char_is_unsigned 0)
endif()

if(CMAKE_SIZEOF_VOID_P EQUAL 8)
	message(STATUS "Target is 64 bits")
	set(is_64 1)
	set(is_32 0)
else()
	set(is_64 0)
	set(is_32 1)
	message(STATUS "Target is 32 bits")
endif()

add_definitions(-DD3_OSTYPE="${os}" -DD3_SIZEOFPTR=${CMAKE_SIZEOF_VOID_P})
add_definitions(-DD3_ARCH="${cpu}")
# Stores in variable <var> either 1 or 0 indicating whether the target architecture is big or little endian.
# test_big_endian(is_big_endian)
if(CMAKE_CXX_BYTE_ORDER EQUAL BIG_ENDIAN)
	message(STATUS "Target is Big endian")
	set(is_big_endian 1)
	set(is_little_endian 0)
else()
	message(STATUS "Target is Little endian")
	set(is_big_endian 0)
	set(is_little_endian 1)
endif()
add_definitions(-DD3_IS_BIG_ENDIAN=${is_big_endian} -DD3_IS_LITTLE_ENDIAN=${is_little_endian})
add_definitions(-DD3_CHAR_IS_SIGNED=${char_is_singed} -DD3_CHAR_IS_UNSIGNED=${char_is_unsigned})
add_definitions(-DD3_IS_64=${is_64} -DD3_IS_32=${is_32})

message(STATUS "Setting -DD3_ARCH=\"${cpu}\" -DD3_SIZEOFPTR=${CMAKE_SIZEOF_VOID_P} -DD3_OSTYPE=\"${os}\" -DD3_IS_BIG_ENDIAN=${is_big_endian} -DD3_IS_LITTLE_ENDIAN=${is_little_endian} -DD3_CHAR_IS_SIGNED=${char_is_singed} -DD3_CHAR_IS_UNSIGNED=${char_is_unsigned} -DD3_IS_64=${is_64} -DD3_IS_32=${is_32}")

if(cpu MATCHES ".*64.*" AND NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
	# tough luck if some CPU architecture has "64" in its name but uses 32bit pointers
	message(SEND_ERROR "CMake thinks sizeof(void*) == 4, but the target CPU looks like a 64bit CPU!")
	message(FATAL_ERROR "If you're building in a 32bit chroot on a 64bit host, switch to it with 'linux32 chroot' or at least call cmake with linux32 (or your OSs equivalent)!")
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
if(USE_MINIZ)
	add_definitions(-D_MINIZ)
else()
find_package(ZLIB REQUIRED)
include_directories(${ZLIB_INCLUDE_DIRS})
endif()

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
	include_directories("${CMAKE_CURRENT_SOURCE_DIR}/externlibs/include")
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

if(MSVC)
	if(TOOLS)
		# This is required for tools on windows.
		find_package(MFC)
	
		if(TOOLS AND NOT MFC_FOUND)
			message(SEND_ERROR "MFC ('Microsoft Foundation Classes for C++') couldn't be found, but is needed for TOOLS!")
			message(FATAL_ERROR "If you're using VS2013, you'll also need the 'Multibyte MFC Library for Visual Studio 2013': https://www.microsoft.com/en-us/download/details.aspx?id=40770 (VS2015 and 2017 include that in the default MFC package)")
		endif()
	endif()
else() # not MSVC

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

	# check if this is some kind of clang (Clang, AppleClang, whatever)
	# (convert compiler ID to lowercase so we match Clang, clang, AppleClang etc, regardless of case)
	string(TOLOWER ${CMAKE_CXX_COMPILER_ID} compiler_id_lower)
	if(compiler_id_lower MATCHES ".*clang.*")
		message(STATUS "Compiler \"${CMAKE_CXX_COMPILER_ID}\" detected as some kind of clang")
		set(D3_COMPILER_IS_CLANG TRUE)
		set(D3_COMPILER_IS_GCC_OR_CLANG TRUE)
	elseif(CMAKE_COMPILER_IS_GNUCC)
		set(D3_COMPILER_IS_GCC_OR_CLANG TRUE)
	endif()
	unset(compiler_id_lower)

endif() # not MSVC

# compiler specific flags
if(D3_COMPILER_IS_GCC_OR_CLANG)
	add_compile_options(-pipe)
	#k add_compile_options(-Wall)

	if(NOT CMAKE_CROSSCOMPILING AND ONATIVE)
		add_compile_options(-march=native)
	elseif(NOT APPLE AND cpu STREQUAL "x86")
		add_compile_options(-march=pentium3)
	endif()

	if(cpu STREQUAL "e2k" AND CMAKE_COMPILER_IS_GNUCC)
		# O3 on E2K mcst-lcc approximately equal to O2 at X86/ARM gcc
		set(OPT_LEVEL "-O3")
	else()
		set(OPT_LEVEL "-O2")
	endif()
	#set(OPT_LEVEL "-O3")

	#karin: don't add `-ffinite-math-only`, because cause NaN <= 0.0 is true, example: calc NaN `fov_y` when drawRect.w == drawRect.h == 0, and call idRenderWorld::RenderScene on ui/idRenderWindow::Draw
	set(CMAKE_C_FLAGS_DEBUG "-g -ggdb -D_DEBUG -O0 -fPIC")
	set(CMAKE_C_FLAGS_DEBUGALL "-g -ggdb -D_DEBUG -fPIC")
	set(CMAKE_C_FLAGS_PROFILE "-g -ggdb -D_DEBUG -O1 -fno-omit-frame-pointer -fPIC")
	set(CMAKE_C_FLAGS_RELEASE "${OPT_LEVEL} -fno-math-errno -fno-trapping-math -fomit-frame-pointer -fPIC")
	set(CMAKE_C_FLAGS_RELWITHDEBINFO "-g -ggdb ${OPT_LEVEL} -fno-math-errno -fno-trapping-math -fno-omit-frame-pointer -fPIC")
	set(CMAKE_C_FLAGS_MINSIZEREL "-Os -fno-math-errno -fno-trapping-math -fomit-frame-pointer -fPIC")

	set(CMAKE_CXX_FLAGS_DEBUGALL ${CMAKE_C_FLAGS_DEBUGALL})
	set(CMAKE_CXX_FLAGS_PROFILE ${CMAKE_C_FLAGS_PROFILE})

	add_compile_options(-fno-strict-aliasing)
	#add_compile_options(-fsigned-char)
	# dear idiot compilers, don't fuck up math code with useless FMA "optimizations"
	# (https://gcc.gnu.org/bugzilla/show_bug.cgi?id=100839)
	CHECK_CXX_COMPILER_FLAG("-ffp-contract=off" cxx_has_fp-contract)
	if(cxx_has_fp-contract)
	add_compile_options(-ffp-contract=off)
	endif()

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
		# use 8MB stack instead of 1MB, so big models don't overflow it with _alloca()
		set(ldflags "${ldflags} -static-libgcc -static-libstdc++ -Wl,--stack,8388608")
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
	# use 8MB stack instead of 1MB, so big models don't overflow it with _alloca()
	set(ldflags "${ldflags} /STACK:8388608")
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
	set(DIRECTXSDK_INCLUDE_DIR)
elseif(ANDROID)
	set(sys_libs ${sys_libs} android dl EGL)
	if(NOT USE_MINIZ)
	    set(LINK_Z True)
	endif()
else() #TODO linux
	set(sys_libs ${sys_libs} dl pthread)
	if(USE_X11)
		set(LINUX_SDL FALSE)
		set(LINUX_X11 TRUE)
		message(STATUS "use X11 + EGL")
		set(sys_libs ${sys_libs} X11 EGL)
	else() # SDL2
	    if(USE_LOCAL_SDL)
	        add_subdirectory(externlibs/SDL2)
            set(LINUX_SDL TRUE)
			set(LINUX_X11 FALSE)
            message(STATUS "use local SDL2")
            include_directories(externlibs/SDL2/include)
            set(SDLx_LIBRARY "SDL2::SDL2")
            add_definitions(-D_SDL)
            set(sys_libs ${sys_libs} ${SDLx_LIBRARY})
	    else()
		find_package(SDL2)
		if(SDL2_FOUND)
			set(LINUX_SDL TRUE)
			set(LINUX_X11 FALSE)
                message(STATUS "use system SDL2")
                include_directories(${SDL2_INCLUDE_DIRS})
			set(SDLx_LIBRARY ${SDL2_LIBRARIES})
			add_definitions(-D_SDL)
			set(sys_libs ${sys_libs} ${SDLx_LIBRARY})
		else()
			set(LINUX_SDL FALSE)
			set(LINUX_X11 TRUE)
			message(STATUS "SDL2 not found, using X11 + EGL")
			set(sys_libs ${sys_libs} X11 EGL)
		endif()
	endif()
	endif()
	if(NOT USE_MINIZ)
	    set(LINK_Z True)
	endif()
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

set(REQUIRE_OGGVORBIS TRUE)

if(USE_STB_IMAGE)
	set(CORE_DEFINES "${CORE_DEFINES} -D_USING_STB")
	message(STATUS "Using stb jpeg image")
endif()
if(USE_STB_OGG)
	set(CORE_DEFINES "${CORE_DEFINES} -D_USING_STB_OGG")
	set(REQUIRE_OGGVORBIS FALSE)
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
if(D3BFG_OCCLUSION_CULLING)
	set(CORE_DEFINES "${CORE_DEFINES} -D_D3BFG_CULLING")
	message(STATUS "Add DOOM3-BFG occlusion culling support")
endif()
if(D3BFG_NEW_FONT)
	add_definitions(-D_D3BFG_FONT)
	message(STATUS "Add DOOM3-BFG new font support")
	set(WIDE_CHARACTER_LANGUAGE ON)
endif()
if(WIDE_CHARACTER_LANGUAGE)
	add_definitions(-D_WCHAR_LANG)
	set(CORE_DEFINES "${CORE_DEFINES} -D_NEW_FONT_TOOLS")
	set(REQUIRE_FREETYPE TRUE)
	message(STATUS "Add wide-character language font support")
endif()
if(GLSL_PROGRAM)
	add_definitions(-D_GLSL_PROGRAM)
	message(STATUS "Add GLSL program on new material stage support")
endif()
if(GLOBAL_ILLUMINATION)
	add_definitions(-D_GLOBAL_ILLUMINATION)
	message(STATUS "Add global illumination support")
endif()
if(RETRO_POSTPROCESS)
	add_definitions(-D_POSTPROCESS)
	message(STATUS "Add retro postprocess rendering support")
endif()
if(IMGUI AND NOT LINUX_X11)
	add_definitions(-D_IMGUI)
	message(STATUS "Add imGUI support")
endif()
if(GL_INT_INDEX)
	add_definitions(-D_GL_INT_INDEX)
	message(STATUS "Use 4 bytes index on OpenGL for large model")
endif()
if(MP3_SUPPORT)
	set(CORE_DEFINES "${CORE_DEFINES} -D_SND_MP3")
	message(STATUS "mp3 sound support")
endif()
if(SUPPORT_MD5V6)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_MD5V6")
	message(STATUS "Add 2002 E3 demo md5mesh/md5anim v6 version animation model converter")
endif()
if(SUPPORT_PSK)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_PSK")
	message(STATUS "Support Unreal engine psk/psa animation/static model")
endif()
if(SUPPORT_IQM)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_IQM")
	message(STATUS "Support iqm animation/static model")
endif()
if(SUPPORT_SMD)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_SMD")
	message(STATUS "Support Source engine smd animation/static model")
endif()
if(SUPPORT_GLTF)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_GLTF")
	message(STATUS "Support Khronos gltf/glb animation/static model")
endif()
if(SUPPORT_FBX)
	set(CORE_DEFINES "${CORE_DEFINES} -D_MODEL_FBX")
	message(STATUS "Support Autodesk fbx animation/static model")
    set(LINK_Z True)
endif()
if(CONSOLE AND WIN32)
	set(CORE_DEFINES "${CORE_DEFINES} -D_REDIRECT_OUTPUT")
	message(STATUS "Redirect output to stdout.txt")
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
		set(d3_compile_definitions "${d3_compile_definitions};_MOD_VIEW_BODY")
		set(d3_compile_definitions "${d3_compile_definitions};_MOD_VIEW_LIGHT")
		message(STATUS "Build doom3 full body awareness mod support")
	endif()
	if(D3_MOD_BOTS)
		set(d3_compile_definitions "${d3_compile_definitions};MOD_BOTS;_MOD_BOTS_ASSETS")
		message(STATUS "Build doom3 bots mod support")
	endif()
	set(d3_compile_definitions "${d3_compile_definitions};_DOOM3;_BLOODMOD")
	#set(d3_compile_definitions "${d3_compile_definitions};_MOD_ANIM_MODEL_ROTATION")
	set_target_properties(game PROPERTIES COMPILE_DEFINITIONS "${d3_compile_definitions}")
	target_include_directories(game PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/game")
	set_target_properties(game PROPERTIES LINK_FLAGS "${ldflags}")
	set_target_properties(game PROPERTIES INSTALL_NAME_DIR "@executable_path")
	target_link_libraries(game idlib)
	if(NOT MSVC)
		target_compile_options(game PRIVATE -Wno-format-security)
	endif()

	if(NOT APPLE AND NOT WIN32 AND NOT ANDROID)
		install(TARGETS game
				RUNTIME DESTINATION "${bindir}"
				LIBRARY DESTINATION "${libdir}"
				ARCHIVE DESTINATION "${libdir}"
				)
	endif()
endif()
