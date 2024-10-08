# filelist for testbench files and targets.

TB_VFILES := $(shell find $(TB_DIRS) ../../chisel-project/build -name "*.v" -or -name "*.sv")
TB_CFILES := $(shell find $(TB_DIRS) -name "*.c" -or -name "*.cpp")
FILES := $(TB_VFILES) $(TB_CFILES)

TARGET = obj_dir/V$(TOP)

VERIFLAGS := --cc --exe --build --trace -j 0 -Wall --timing\
			 -Wno-WIDTHEXPAND -Wno-DECLFILENAME -Wno-UNUSEDSIGNAL -Wno-VARHIDDEN
CUSTOMFLAG := -fPIE -Iobj_dir 

VERILATE := verilator $(VERIFLAGS) -CFLAGS "$(CUSTOMFLAG)" $(FILES) --top-module $(TOP) # -LDFLAGS "$(LIBS)"

all: $(TARGET)

$(TARGET): $(FILES)
	@echo "+ building target $@"
	@echo "+ building sources $^"
	$(VERILATE)