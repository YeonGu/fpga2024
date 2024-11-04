MIP Chisel Project
=======================

# Specification

## Control Registers / PS-PL Interface

- MVP Info [w-r]
  - mat3x4, 16bit float point
  - 12 words. 
- base coord [w-r]
  - 3*16bit
  - 3 words
- start control (-> VRAM ) [w]
  - 1bit write
- status monitor [r]
  - dispatch count
  - channel count

- (interrupts)

``` plaintext
BASE=0xA000_0000
+ 0x0 MVP Info
  16bit float point * 12 elements
  each takes 32bit (one word), 12 words
  48 bytes = 0x30 bytes in total
  - 0xA000_002F

+ 0x30 base coord
  +0 [15:0]: base coord 0
  +4 [15:0]: base coord 1
  +8 [15:0]: base coord 2

+ 0x40 start control
  write non-zero value to 0x40 to start rendering
  like this:
    volatile uint32_t* start_reg = (uint32_t*)0xA000_0040;
    *start_reg = 0x1;
  
+ 0x50 status monitor
  +0x00: calc channel #0 count
  +0x04: calc channel #1 count
  +0x08: calc channel #2 count
  +0x0C: calc channel #3 count
  +0x10: dispatch count
```
