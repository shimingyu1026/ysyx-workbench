#include "verilated.h"
#include "Vtop.h"
#include <common.h>
void init_monitor(int, char *[]);
void sdb_mainloop();
int main(int argc, char **argv)
{
    VerilatedContext* contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vtop* top = new Vtop{contextp};

    init_monitor(argc, argv);
    sdb_mainloop();
    return 0;
}