cmake_minimum_required(VERSION 3.10.0)
project(SDLApp)

find_package(SDL2 REQUIRED)
find_package(Eigen3 REQUIRED)

set(INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/inc)
include_directories(${INCLUDE_DIR})
file(GLOB SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp)
add_executable(SDLApp main.cpp ${SOURCES}
)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
target_link_libraries(SDLApp SDL2::SDL2 Eigen3::Eigen)

