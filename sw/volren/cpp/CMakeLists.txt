cmake_minimum_required(VERSION 3.10)

# Set the project name
project(ct_volume_render)

find_package(OpenCV REQUIRED)
find_package(Eigen3 REQUIRED)
# Specify the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# Add compiler flags
add_compile_options(-Werror -Wall -O3 -mfma -mavx2 -flto -funroll-loops -fopt-info-vec -ffast-math)

# Add all source files in the src directory
file(GLOB_RECURSE SOURCES ${PROJECT_SOURCE_DIR}/src/*.cpp)

# Add the executable
add_executable(ct_volume_render ${SOURCES})

# Include directories
include_directories(${PROJECT_SOURCE_DIR}/include ${OpenCV_INCLUDE_DIRS} ${EIGEN3_INCLUDE_DIR})

# Link libraries (if any)
# target_link_libraries(ct_volume_render <library_name>)
target_link_libraries(ct_volume_render ${OpenCV_LIBS} ${EIGEN3_LIBS})