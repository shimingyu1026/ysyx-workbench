#include <dlfcn.h>
#include <common.h>
#include <capstone/capstone.h>

// 动态加载的cs_disasm函数指针，用于反汇编机器码。
static size_t (*cs_disasm_dl)(csh handle, const uint8_t *code,
                              size_t code_size, uint64_t address, size_t count, cs_insn **insn);
// 动态加载的cs_free函数指针，用于释放反汇编结果内存。
static void (*cs_free_dl)(cs_insn *insn, size_t count);
// Capstone库的句柄，用于配置和操作反汇编器。
static csh handle;

void init_disasm()
{
    void *dl_handle;
    dl_handle = dlopen("/home/smy/ysyx-workbench/npc/tools/capstone/repo/libcapstone.so.5", RTLD_LAZY);
    assert(dl_handle);

    cs_err (*cs_open_dl)(cs_arch arch, cs_mode mode, csh *handle) = NULL;
    cs_open_dl = (cs_err(*)(cs_arch, cs_mode, csh *))dlsym(dl_handle, "cs_open");
    assert(cs_open_dl);

    cs_disasm_dl = (size_t (*)(csh, const uint8_t *, size_t, uint64_t, size_t, cs_insn **))dlsym(dl_handle, "cs_disasm");
    assert(cs_disasm_dl);

    cs_free_dl = (void (*)(cs_insn *, size_t))dlsym(dl_handle, "cs_free");
    assert(cs_free_dl);

    cs_arch arch = CS_ARCH_RISCV;
    cs_mode mode = CS_MODE_RISCV32;
    int ret = cs_open_dl(arch, mode, &handle);
    assert(ret == CS_ERR_OK);
}

// 反汇编函数
// 调用cs_disasm反汇编机器码，结果存储在insn中。
void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte)
{
    cs_insn *insn;
    size_t count = cs_disasm_dl(handle, code, nbyte, pc, 0, &insn);
    assert(count == 1);
    int ret = snprintf(str, size, "%s", insn->mnemonic);
    if (insn->op_str[0] != '\0')
    {
        snprintf(str + ret, size - ret, "\t%s", insn->op_str);
    }
    cs_free_dl(insn, count);
}
