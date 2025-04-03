#include <am.h>
#include <klib-macros.h>
#include <stdio.h>
#define putstr(s) \
  ({ for (const char *p = s; *p; p++) putch(*p); })
extern char _heap_start;
extern char _heap_end;
int main(const char *args);

extern const uintptr_t _data_lma;
extern const uintptr_t _data_start;
extern const uintptr_t _data_end;

extern const uintptr_t _bss_start;
extern const uintptr_t _bss_end;
// #define PMEM_SIZE (128 * 1024 * 1024)
// #define PMEM_END ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, &_heap_end);
static const char mainargs[MAINARGS_MAX_LEN] = MAINARGS_PLACEHOLDER; // defined in CFLAGS

void putch(char ch)
{
  while (!(*(volatile uint8_t *)(0x10000000 + 5) & 0x1 << 5))
    ;
  *(volatile uint8_t *)(0x10000000) = ch;
}
void uart_set()
{
  // Line Control Register
  uint8_t reg_value;
  reg_value = *(volatile uint8_t *)(0x10000000 + 3);
  *(volatile uint8_t *)(0x10000000 + 3) = reg_value | (1 << 7);
  *(volatile uint8_t *)(0x10000000 + 0) = 25;
  *(volatile uint8_t *)(0x10000000 + 1) = 0x0;
  *(volatile uint8_t *)(0x10000000 + 3) = reg_value;
}

void halt(int code)
{
  asm volatile("mv a0, %0; ebreak" : : "r"(code));
  while (1)
    ;
}
void _printInfo();
void _trm_init()
{
  uart_set();
  //_printInfo();
  int ret = main(mainargs);
  halt(ret);
}
void _printInfo()
{
  uint32_t mvendorid;
  __asm__ volatile(
      "csrr %0, mvendorid" // Read mvendorid into output operand
      : "=r"(mvendorid)    // Output operand
      :                    // No input operands
      : /* No clobbers */  // No registers are clobbered
  );
  printf("ysyx ASCII: 0x%x\n", mvendorid);
  uint32_t marchid;
  __asm__ volatile(
      "csrr %0, marchid"  // Read mvendorid into output operand
      : "=r"(marchid)     // Output operand
      :                   // No input operands
      : /* No clobbers */ // No registers are clobbered
  );
  printf("ysyx_%d\n", marchid);
}