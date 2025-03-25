#ifndef __CPU_DIFFTEST_H__
#define __CPU_DIFFTEST_H__

#include <common.h>
#include <difftest-def.h>

#ifdef CONFIG_DIFFTEST
void difftest_skip_ref();
void difftest_skip_dut(int nr_ref, int nr_dut);
void difftest_step(vaddr_t pc, vaddr_t npc, CPU *cpu);
#endif

static inline bool difftest_check_reg(const char *name, vaddr_t pc, word_t ref, word_t dut)
{
    if (ref != dut)
    {
        Log("%s is different after executing instruction at pc = " FMT_WORD
            ", right = " FMT_WORD ", wrong = " FMT_WORD ", diff = " FMT_WORD,
            name, pc, ref, dut, ref ^ dut);
        return false;
    }
    return true;
}

#endif