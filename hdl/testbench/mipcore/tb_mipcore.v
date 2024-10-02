module testbench ();

  reg clock = 0;
  always #1 clock = ~clock;


endmodule

import "DPI-C" function void pmem_read(input bit re, input int addr, input int mask, output int rword);
import "DPI-C" function 