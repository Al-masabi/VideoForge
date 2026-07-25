cmake_minimum_required(VERSION 3.22.1)
project("videoforge-ffmpeg")

set(FFMPEG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/${ANDROID_ABI})

add_library(videoforge-ffmpeg SHARED ffmpeg_bridge.c)

foreach(lib avformat avcodec avutil swresample swscale)
    add_library(${lib} SHARED IMPORTED)
    set_target_properties(${lib} PROPERTIES IMPORTED_LOCATION ${FFMPEG_DIR}/lib${lib}.so)
endforeach()

target_include_directories(videoforge-ffmpeg PRIVATE ${FFMPEG_DIR}/include)

target_link_libraries(videoforge-ffmpeg
    avformat
    avcodec
    avutil
    swresample
    swscale
    log
)