/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>

__EXPORT void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
  uint32_t *p = (uint32_t *)buf;
  if (direction == DIFFTEST_TO_REF)
  {
    // DIFFTEST_TO_REF
    memcpy(guest_to_host(addr), p, n);
  }
  else
  {
    // REF_TO_DIFFTEST
    memcpy(p + (addr - 0x80000000) / 4, guest_to_host(addr), n);
  }
}

__EXPORT void difftest_regcpy(void *dut, bool direction) {
  CPU_state *ref_r = &cpu;
  CPU_state *dut_r = (CPU_state *)dut;

  if (direction == DIFFTEST_TO_REF)
  {
    // DIFFTEST_TO_REF
    for (int i = 0; i < 32; i++)
    {
      ref_r->gpr[i] = dut_r->gpr[i];
    }
    ref_r->pc = dut_r->pc;
    ref_r->csr.mstatus = dut_r->csr.mstatus;
    ref_r->csr.mepc = dut_r->csr.mepc;
    ref_r->csr.mtvec = dut_r->csr.mtvec;
    ref_r->csr.mepc = dut_r->csr.mepc;
  }
  else
  {
    // REF_TO_DIFFTEST
    for (int i = 0; i < 32; i++)
    {
      dut_r->gpr[i] = ref_r->gpr[i];
    }
    dut_r->pc = ref_r->pc;
    dut_r->csr.mstatus = ref_r->csr.mstatus;
    dut_r->csr.mepc = ref_r->csr.mepc;
    dut_r->csr.mtvec = ref_r->csr.mtvec;
    dut_r->csr.mepc = ref_r->csr.mepc;
  }
}

__EXPORT void difftest_exec(uint64_t n) {
  cpu_exec(n);
  /* printf("pc = %x\n", cpu.pc); */
  /* isa_reg_display(); */
}

__EXPORT void difftest_raise_intr(word_t NO) {
  assert(0);
}

__EXPORT void difftest_init(int port) {
  void init_mem();
  init_mem();
  /* Perform ISA dependent initialization. */
  init_isa();
}
