cmake_minimum_required(VERSION 3.22.0)
project(TestBench)

cmake_policy(SET CMP0144 NEW)
find_package(Verilator HINTS $ENV{VERILATOR_ROOT})

if (NOT Verilator_FOUND)
    message(FATAL_ERROR "Verilator not found!")
endif()

set(CMAKE_CXX_STANDARD 11)
set(VERILOG_SRC ${CMAKE_CURRENT_SOURCE_DIR}/mipcore/tb_mipcore.v)
set(VERILATOR_FLAGS "-O3")
add_executable(Vtestbench ${CMAKE_CURRENT_SOURCE_DIR}/mipcore/main.cpp)

verilate(Vtestbench
    TRACE
    SOURCES ${VERILOG_SRC}
    VERILATOR_ARGS ${VERILATOR_FLAGS})



