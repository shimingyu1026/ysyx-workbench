#ifndef __CPU_CPU_H__
#define __CPU_CPU_H__

#include <common.h>
void cpu_exec(uint64_t n, CPU *cpu, VerilatedFstC *tfp, VerilatedContext *contextp);
#endif