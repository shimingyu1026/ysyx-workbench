#include <common.h>
void init_monitor(int, char *[]);
void sdb_mainloop(CPU *cpu);
word_t vaddr_read(vaddr_t addr, int len);
void reset(CPU *cpu);

extern "C" word_t mem_read(vaddr_t addr, int len);
extern "C" void npcTrapHandler();

int main(int argc, char **argv)
{
    VerilatedContext *contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    TOP *top = new TOP{contextp};
    //---------------------------------------------------------
    CPU *cpu = new CPU();
    cpu->top = top;
    cpu->pc = &top->io_pc_o;
    cpu->inst = &top->io_inst_o;
    for (int i = 0; i < 32; i++)
    {
        cpu->regs = &cpu->top->io_regs_0;
    }
    //------------------------------------------------------------------

    init_monitor(argc, argv);
    reset(cpu);

    sdb_mainloop(cpu);

    return 0;
}

void reset(CPU *cpu)
{
    cpu->top->reset = 1;
    int n = 20;
    while (n-- > 0)
    {
        cpu->top->clock = 0;
        cpu->top->eval();
        cpu->top->clock = 1;
        cpu->top->eval();
    }
    cpu->top->reset = 0;
}