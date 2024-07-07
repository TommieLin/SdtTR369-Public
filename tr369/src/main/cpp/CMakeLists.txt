# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.
project("tr369")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

aux_source_directory(openTr369/core SRC_TR369_CORE)
aux_source_directory(openTr369/vendor SRC_TR369_VENDOR)

add_library(
        # Sets the name of the library.
        sk_tr369_jni
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        skSource/sk_tr369_jni.cpp
        skSource/sk_jni_callback.cpp
        skSource/sk_jni_network.cpp
        ${SRC_TR369_CORE} ${SRC_TR369_VENDOR}
)

include_directories(
        openTr369/core
        openTr369/include
        openTr369/vendor
        skSource
        libs/armeabi-v7a/include
)

set(CMAKE_C_FLAGS
        "${CMAKE_C_FLAGS} \
        -DDISABLE_STOMP \
        -DENABLE_MQTT \
        -DE2ESESSION_EXPERIMENTAL_USP_V_1_2 \
        -DREMOVE_DEVICE_INFO \
        -DREMOVE_DEVICE_TIME \
        -DREMOVE_SELF_TEST_DIAG_EXAMPLE")

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
        # Specifies the target library.
        sk_tr369_jni
        log
        android
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_crypto.so
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_ssl.so
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_curl.so
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_mosquitto.so
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_zlib.so
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_iconv.so
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtms_xml2.so
)
