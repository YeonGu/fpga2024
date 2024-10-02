# filelist for testbench files and targets.

MIPCORE_TB_DIRS := mipcore/
MIPCORE_TB_VFILES := $(shell find -name "*.v" -or "*.sv")
MIPCORE_TB_CFILES := $(shell find -name "*.c" -or "*.v")