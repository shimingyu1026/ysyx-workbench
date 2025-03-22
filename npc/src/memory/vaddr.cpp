#include <isa.h>
#include <memory/paddr.h>

word_t vaddr_read(vaddr_t addr, int len)
{
    return paddr_read(addr, len);
}

extern "C" word_t mem_read(vaddr_t addr, int len)
{
    return vaddr_read(addr, len);
}