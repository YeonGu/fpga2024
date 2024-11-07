cmake_minimum_required(VERSION 3.10)

# Set the project name
project(apptest)

find_package(Eigen3 REQUIRED)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

add_compile_options(-Werror -Wall -O3 -mfma -mavx2 -flto -funroll-loops -ffast-math)

# Find all source files in src directory
file(GLOB_RECURSE SOURCES "src/*.cpp" "src/*.c")

# Add the executable
add_executable(apptest ${SOURCES})

include_directories(${PROJECT_SOURCE_DIR}/src/include ${EIGEN3_INCLUDE_DIR})

target_link_libraries(apptest ${EIGEN3_LIBS})