#include "obj_dir/Vtestbench.h"
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <cstdint>

#define VTOP Vtestbench
using vtop = Vtestbench;

void display_init();

uint32_t SIM_TIMES = -1; // -1 for infinite simulation

int main(int argc, char** argv)
{
    VerilatedContext* contextp = new VerilatedContext;

    contextp->commandArgs(argc, argv);
    contextp->traceEverOn(true);

    VTOP* dut = new VTOP{contextp};

    auto tfp = new VerilatedVcdC();
    dut->trace(tfp, 99);
    tfp->open("wave.vcd");

    display_init();

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

void display_init()
{
    // TODO. Initialize result display with specific resolution.
}