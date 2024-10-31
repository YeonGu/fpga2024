#include "obj_dir/Vtb_fetcher.h"
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <cstdint>
#include <cstdio>

#define VTOP Vtb_fetcher
using vtop = Vtb_fetcher;

uint32_t SIM_TIMES = 5000; // -1 for infinite simulation

int main(int argc, char** argv)
{
    VerilatedContext* contextp = new VerilatedContext;

    contextp->commandArgs(argc, argv);
    contextp->traceEverOn(true);

    VTOP* dut = new VTOP{contextp};

    auto tfp = new VerilatedVcdC();
    dut->trace(tfp, 99);
    tfp->open("wave.vcd");

    while(SIM_TIMES--)
    {
        dut->eval();
        tfp->dump(contextp->time());
        contextp->timeInc(1);
    }

    dut->final();
    tfp->close();
    delete dut;
    delete contextp;

    return 0;
}
// import "DPI-C" function void read_128bit_data(
//   input  [31:0] addr,
//   output [31:0] data_h,
//   output [31:0] data_h2,
//   output [31:0] data_h3,
//   output [31:0] data_l
// );
extern "C" void read_128bit_data(uint32_t* addr, uint32_t* data_h, uint32_t* data_h2,
                                 uint32_t* data_h3, uint32_t* data_l)
{
    // For demonstration purposes, we will just fill the data with some pattern
    // In a real scenario, this function would read from a memory or a register
    auto rdaddr = *addr;
    *data_h     = rdaddr;
    *data_h2    = rdaddr;
    *data_h3    = rdaddr;
    *data_l     = rdaddr;

    // Print the data for verification
    // printf("the addr is %d\n", *addr);
}
