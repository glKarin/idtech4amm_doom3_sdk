# idTech4A++ DOOM3 SDK

------------

DOOM3 SDK for Android idTech4A++  
Building or porting your mod and make it run on Android idTech4A++

------------

> Build apk
#### 1-1: Build all
```gradlew assembleRelease```
#### 1-2: Build arm64 only
```gradlew assembleRelease -Pabifilters=arm64-v8a```
#### 1-3: Build arm32 only
```gradlew assembleRelease -Pabifilters=armeabi-v7a```
#### 2: target libraries on `doom3/build/outputs/apk/release`

------------

> Build library only
#### 1. enter source folder
```cd neo```
#### 2-1. Build arm64
```build_arm64.bat```
#### 2-2. Build arm32
```build_arm32.bat```
#### 3. target libraries on `neo/build`

------------
