cmake_minimum_required(VERSION 3.4.1)
add_library(kcats SHARED IMPORTED)

set_target_properties(kcats PROPERTIES IMPORTED_LOCATION
        "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libkcats.so")
