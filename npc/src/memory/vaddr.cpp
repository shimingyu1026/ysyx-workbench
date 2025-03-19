#include <isa.h>
#include <memory/paddr.h>

word_t vaddr_read(vaddr_t addr, int len)
{
    return paddr_read(addr, len);
}