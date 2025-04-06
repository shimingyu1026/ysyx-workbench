#include <common.h>
void init_monitor(int, char *[], CPU *cpu);
void sdb_mainloop(CPU *cpu, VerilatedFstC *tfp, VerilatedContext *contextp);
word_t vaddr_read(vaddr_t addr, int len);
void reset(CPU *cpu);

extern "C" word_t mem_read(vaddr_t addr, int len);
extern "C" void mem_write(vaddr_t addr, word_t data, char mask);
extern "C" void npcTrapHandler();
extern "C" void call_trace_diff();
extern "C" void uncall_trace_diff();
extern "C" void difftest_skip_ref();
// ysyx SoC
extern "C" void flash_read(int32_t addr, int32_t *data);
extern "C" void mrom_read(int32_t addr, int32_t *data);

int main(int argc, char **argv)
{
    VerilatedContext *contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    TOP *top = new TOP{contextp};

    VerilatedFstC *tfp = new VerilatedFstC;
    contextp->traceEverOn(true); // Trace enabled
    top->trace(tfp, 99);         // Trace signals
    tfp->open("waveform.fst");   // Output FST file

    //---------------------------------------------------------
    CPU *cpu = new CPU();
    cpu->top = top;
    cpu->pc = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__PC;
    cpu->inst = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__inst;

    cpu->regs = (vaddr_t *)&cpu->top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__regFile__DOT__regs_ext__DOT__Memory;
    cpu->mepc = &cpu->top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__csrFile__DOT__mepc;
    cpu->mstatus = &cpu->top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__csrFile__DOT__mstatus;
    cpu->macause = &cpu->top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__csrFile__DOT__mcause;
    cpu->mtvec = &cpu->top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__csrFile__DOT__mtvec;
    //------------------------------------------------------------------

    init_monitor(argc, argv, cpu);
    sdb_mainloop(cpu, tfp, contextp);

  

    tfp->close(); // Close the FST file at the end of simulation
    delete top;
    return 0;
}

