#include <common.h>
#include <isa.h>
#include <cpu/difftest.h>
#include <memory/paddr.h>

word_t vaddr_read(vaddr_t addr, int len)
{
    return paddr_read(addr, len);
}
void vaddr_write(paddr_t addr, int len, word_t data)
{
    paddr_write(addr, len, data);
}

extern "C" void mem_write(vaddr_t addr, word_t data, char mask)
{

    if (addr == (0xa0000000 + 0x3f8))
    {
        IFDEF(CONFIG_DIFFTEST, difftest_skip_ref();)
        printf("%c", (uint8_t)(data & 0xFF));
        return;
    }
    // printf("mem_write: addr = %x, data = %x, mask = %x\n", addr, data, mask);
    uint8_t data_bytes[4] = {
        (uint8_t)(data & 0xFF),         // 最低字节
        (uint8_t)((data >> 8) & 0xFF),  // 次低字节
        (uint8_t)((data >> 16) & 0xFF), // 次高字节
        (uint8_t)((data >> 24) & 0xFF)  // 最高字节
    };
    addr = addr & ~0x3u;
    if (mask & 0x1)
        vaddr_write(addr, 1, data_bytes[0]);
    if (mask & 0x2)
        vaddr_write(addr + 1, 1, data_bytes[1]);
    if (mask & 0x4)
        vaddr_write(addr + 2, 1, data_bytes[2]);
    if (mask & 0x8)
        vaddr_write(addr + 3, 1, data_bytes[3]);
}
extern "C" word_t mem_read(vaddr_t addr, int len)
{
    uint64_t us = get_time();
    if (addr == (0xa0000000 + 0x0000048) + 4)
    {
        IFDEF(CONFIG_DIFFTEST, difftest_skip_ref();)
        us = get_time();
        return us >> 32;
    }
    if (addr == (0xa0000000 + 0x0000048))
    {
        IFDEF(CONFIG_DIFFTEST, difftest_skip_ref();)
        return (uint32_t)(us);
    }

    // printf("mem_read: addr = %x, len = %d, data= %x\n", addr, len, vaddr_read(addr & ~0x3u, len));
    return vaddr_read(addr & ~0x3u, len);
}

extern "C" void mrom_read(int32_t addr, int32_t *data)
{
    // printf("\nmrom read: %08x at addr %08x\n", vaddr_read(addr, 4), addr);

    *data = vaddr_read(addr, 4);
}
extern "C" void flash_read(int32_t addr, int32_t *data)
{
    printf("\nsram read: %08x at addr %08x\n", sram_read(addr, 4), addr);

    *data = sram_read(addr, 4);
}