#include <common.h>
void init_monitor(int, char *[], CPU *cpu);
void sdb_mainloop(CPU *cpu);
word_t vaddr_read(vaddr_t addr, int len);
void reset(CPU *cpu);

extern "C" word_t mem_read(vaddr_t addr, int len);
extern "C" void mem_write(vaddr_t addr, word_t data, char mask);
extern "C" void npcTrapHandler();
extern "C" void call_trace_diff();
extern "C" void uncall_trace_diff();

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

    init_monitor(argc, argv, cpu);
    sdb_mainloop(cpu);

    return 0;
}

