#ifndef __COMMON_H__
#define __COMMON_H__

#include <stdint.h>
#include <inttypes.h>
#include <stdbool.h>
#include <string.h>

#include <generated/autoconf.h>
#include <macro.h>
#include "verilated.h"
#include "Vtop___024root.h"
#include "Vtop.h"

#include <assert.h>
#include <stdlib.h>

#define TOP Vtop

typedef MUXDEF(CONFIG_ISA64, uint64_t, uint32_t) word_t;
typedef MUXDEF(CONFIG_ISA64, int64_t, int32_t)  sword_t;
#define FMT_WORD MUXDEF(CONFIG_ISA64, "0x%016" PRIx64, "0x%08" PRIx32)

typedef word_t vaddr_t;
typedef MUXDEF(PMEM64, uint64_t, uint32_t) paddr_t;
#define FMT_PADDR MUXDEF(PMEM64, "0x%016" PRIx64, "0x%08" PRIx32)
typedef uint16_t ioaddr_t;

#include <debug.h>

class CPU
{
public:
    char logbuf[128];
    vaddr_t* regs;
    void *pc;

    vaddr_t lnpc;  // 当前周期pc
    vaddr_t snpc;  // 当前周期pc+4
    vaddr_t linst; // 当前周期执行的指令
                   // csr
    vaddr_t *macause;
    vaddr_t *mepc;
    vaddr_t *mstatus;
    vaddr_t *mtvec;
    TOP *top;
    void *inst;
};

#endif