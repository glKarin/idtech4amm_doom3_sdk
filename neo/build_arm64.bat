@echo off

rem setup your build type: Release | Debug
set BUILD_TYPE=Release
rem setup your ninja program path
set MAKE=ninja
rem setup your cmake program path
set CMAKE=cmake
rem setup your Android-NDK path
set NDK=E:\dev\android_sdk\android-ndk-r23c
rem setup build target arch
set ABI=arm64-v8a
rem setup target cpu
set CPU=arm64

set TOOLCHAIN=%NDK%/toolchains
set TOOLCHAIN_FILE=%NDK%\build\cmake\android.toolchain.cmake
set ANDROID_API=21

echo Build %ABI%-%BUILD_TYPE%

echo Configure......
%CMAKE% -DCMAKE_TOOLCHAIN_FILE=%TOOLCHAIN_FILE% -DANDROID_NDK=%NDK% -DCMAKE_MAKE_PROGRAM=%MAKE% -GNinja -DCMAKE_ANDROID_API=%ANDROID_API% -DANDROID_PLATFORM=%ANDROID_API% -DCMAKE_SYSTEM_NAME=Android -DCMAKE_SYSTEM_PROCESSOR=%CPU% -DCMAKE_ANDROID_ARCH_ABI=%ABI% -DANDROID_ABI=%ABI% -DCMAKE_BUILD_TYPE=%BUILD_TYPE% CMakeLists.txt

echo Building......
cmake --build .

echo Build done!

echo Opening build target folder(build\%BUILD_TYPE%\%ABI%)......
start "" build\%BUILD_TYPE%\%ABI%

pause