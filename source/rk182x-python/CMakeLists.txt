cmake_minimum_required(VERSION 3.16)

project(rk_mpp_pipeline LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(Python3 REQUIRED COMPONENTS Interpreter Development)
find_package(pybind11 CONFIG REQUIRED)
find_path(MPP_INCLUDE_DIR rk_mpi.h PATH_SUFFIXES rockchip REQUIRED)
find_library(MPP_LIBRARY NAMES rockchip_mpp REQUIRED)
find_path(RGA_INCLUDE_DIR im2d.hpp PATH_SUFFIXES rga REQUIRED)
find_library(RGA_LIBRARY NAMES rga REQUIRED)
find_path(FFMPEG_INCLUDE_DIR libavformat/avformat.h REQUIRED)

# The board ships FFmpeg headers and versioned shared runtime libraries, but no
# unversioned development symlinks. Linking the versioned .so files keeps this
# pybind11 module shared and avoids the non-PIC static archives selected by
# pkg-config in that environment.
set(FFMPEG_LIBRARY_DIRS /lib/aarch64-linux-gnu /usr/lib/aarch64-linux-gnu)
unset(AVFORMAT_LIBRARY CACHE)
unset(AVCODEC_LIBRARY CACHE)
unset(AVUTIL_LIBRARY CACHE)
find_file(AVFORMAT_LIBRARY NAMES libavformat.so.58 libavformat.so.59 libavformat.so.60 libavformat.so.61
  PATHS ${FFMPEG_LIBRARY_DIRS} REQUIRED)
find_file(AVCODEC_LIBRARY NAMES libavcodec.so.58 libavcodec.so.59 libavcodec.so.60 libavcodec.so.61
  PATHS ${FFMPEG_LIBRARY_DIRS} REQUIRED)
find_file(AVUTIL_LIBRARY NAMES libavutil.so.56 libavutil.so.57 libavutil.so.58 libavutil.so.59
  PATHS ${FFMPEG_LIBRARY_DIRS} REQUIRED)

pybind11_add_module(rk_mpp_pipeline src/rk_mpp_pipeline.cpp)
target_include_directories(rk_mpp_pipeline PRIVATE
  ${MPP_INCLUDE_DIR}
  ${RGA_INCLUDE_DIR}
  ${FFMPEG_INCLUDE_DIR}
)
target_link_libraries(rk_mpp_pipeline PRIVATE
  ${MPP_LIBRARY}
  ${RGA_LIBRARY}
  ${AVFORMAT_LIBRARY}
  ${AVCODEC_LIBRARY}
  ${AVUTIL_LIBRARY}
)
target_compile_options(rk_mpp_pipeline PRIVATE -Wall -Wextra -Wpedantic)

add_library(rknnrt_redirect SHARED src/rknnrt_redirect.c)
target_link_libraries(rknnrt_redirect PRIVATE dl)

install(TARGETS rk_mpp_pipeline LIBRARY DESTINATION .)
install(TARGETS rknnrt_redirect LIBRARY DESTINATION vendor/lib)
