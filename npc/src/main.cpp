#include "verilated.h"
#include "Vtop.h"
#include <stdio.h>
#include <common.h>
int main(int argc, char **argv)
{
    VerilatedContext* contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vtop* top = new Vtop{contextp};
    return 0;

}