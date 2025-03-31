#include <cpu/cpu.h>
#include <cpu/difftest.h>

bool npcTrap = false;
int axi_resp = 0;
static uint64_t g_timer = 0; // unit: us
bool traceDiff = false;
extern "C" void call_trace_diff()
{
    traceDiff = true;
}
extern "C" void uncall_trace_diff()
{
    traceDiff = false;
}
extern "C" void npcTrapHandler()
{
    npcTrap = true;
}

static void trace_and_difftest(CPU *_this, vaddr_t dnpc)
{
#ifdef CONFIG_ITRACE_COND
    if (ITRACE_COND)
    {
        log_write("%s\n", _this->logbuf);
    }
#endif

    IFDEF(CONFIG_ITRACE, puts(_this->logbuf));
    IFDEF(CONFIG_DIFFTEST, difftest_step(_this->lnpc, dnpc, _this));
}

static void exec_once(CPU *cpu, VerilatedVcdC *tfp, VerilatedContext *contextp)
{
    cpu->snpc = *(vaddr_t *)cpu->pc + 4;
    cpu->lnpc = *(vaddr_t *)cpu->pc;
    cpu->linst = *(vaddr_t *)cpu->inst;
    cpu->top->clock = 0;
    cpu->top->eval();
    tfp->dump(contextp->time()); // dump wave
    contextp->timeInc(1);        // 仿真时间推进
    cpu->top->clock = 1;
    cpu->top->eval();
    tfp->dump(contextp->time()); // dump wave
    contextp->timeInc(1);        // 仿真时间推进
#ifdef CONFIG_ITRACE
    if (traceDiff)
    {
        char *p = cpu->logbuf;
        p += snprintf(p, sizeof(cpu->logbuf), FMT_WORD ":", cpu->lnpc);
        int ilen = cpu->snpc - cpu->lnpc;
        int i;
        uint8_t *inst = (uint8_t *)&cpu->linst;
        for (i = ilen - 1; i >= 0; i--)
        {
            p += snprintf(p, 4, " %02x", inst[i]);
        }
        int ilen_max = 4;
        int space_len = ilen_max - ilen;
        if (space_len < 0)
            space_len = 0;
        space_len = space_len * 3 + 1;
        memset(p, ' ', space_len);
        p += space_len;
        void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
        disassemble(p, cpu->logbuf + sizeof(cpu->logbuf) - p,
                    cpu->lnpc, (uint8_t *)&cpu->linst, ilen);
    }
#endif
}
static void execute(uint64_t n, CPU *cpu, VerilatedVcdC *tfp, VerilatedContext *contextp)
{
    for (; n > 0; n--)
    {
        exec_once(cpu, tfp, contextp);
        // printf("mepc: %x\n", *(vaddr_t *)cpu->mepc);
        if (traceDiff)
        {

            trace_and_difftest(cpu, *(vaddr_t *)cpu->pc);
        }
        if (npcTrap)
        {
            break;
        }
    }
}
static void statistic()
{
    IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
    Log("host time spent = " NUMBERIC_FMT " us", g_timer);
    // Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
    // if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
    // else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

void cpu_exec(uint64_t n, CPU *cpu, VerilatedVcdC *tfp, VerilatedContext *contextp)
{
    if (npcTrap)
    {
        printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
        return;
    }
    uint64_t timer_start = get_time();
    execute(n, cpu, tfp, contextp);
    uint64_t timer_end = get_time();
    g_timer += timer_end - timer_start;
    // printf("timer_start: %ld, timer_end: %ld\n", timer_start, timer_end);
    // printf("NPC trap: %d\n", npcTrap);
    if (npcTrap)
    {
            Log("npc: %s at pc = " FMT_WORD,
                (
                    (cpu->regs[10] == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) : ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
                cpu->lnpc);
        statistic();
    }
}