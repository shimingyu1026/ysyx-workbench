
#ifndef __ISA_H__
#define __ISA_H__
#include <common.h>
word_t isa_reg_str2val(const char *s, bool *success);
void isa_reg_display(CPU *cpu);
#endif