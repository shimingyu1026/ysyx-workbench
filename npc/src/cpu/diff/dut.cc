#include <dlfcn.h>

#include <isa.h>
#include <cpu/cpu.h>
#include <memory/paddr.h>
#include <utils.h>
#include <difftest-def.h>
typedef struct
{
    word_t gpr[32];
    vaddr_t pc;
} CPU_state;
// 同步内存数据。
void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
// 同步寄存器状态。
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
// 让参考模拟器执行指定数量的指令。
void (*ref_difftest_exec)(uint64_t n) = NULL;
// 触发中断。
void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

#ifdef CONFIG_DIFFTEST

CPU_state dut_state;
static bool is_skip_ref = false; // 是否跳过参考模拟器的检查
static int skip_dut_nr_inst = 0; // DUT需要跳过的指令数
void cpu_copy(const CPU *cpu, CPU_state *state);
void difftest_skip_ref()
{
    is_skip_ref = true;
    // If such an instruction is one of the instruction packing in QEMU
    // (see below), we end the process of catching up with QEMU's pc to
    // keep the consistent behavior in our best.
    // Note that this is still not perfect: if the packed instructions
    // already write some memory, and the incoming instruction in NEMU
    // will load that memory, we will encounter false negative. But such
    // situation is infrequent.
    skip_dut_nr_inst = 0;
}

void difftest_skip_dut(int nr_ref, int nr_dut)
{
    skip_dut_nr_inst += nr_dut;

    while (nr_ref-- > 0)
    {
        ref_difftest_exec(1);
    }
}
void init_difftest(char *ref_so_file, long img_size, int port, CPU *cpu)
{
    assert(ref_so_file != NULL);

    void *diff_handle;
    diff_handle = dlopen(ref_so_file, RTLD_LAZY); // 加载共享库（如QEMU的实现）
    assert(diff_handle);

    // 动态绑定函数
    ref_difftest_memcpy = (void (*)(paddr_t, void *, size_t, bool))dlsym(diff_handle, "difftest_memcpy");
    assert(ref_difftest_memcpy);

    ref_difftest_regcpy = (void (*)(void *, bool))dlsym(diff_handle, "difftest_regcpy");
    assert(ref_difftest_regcpy);

    ref_difftest_exec = (void (*)(uint64_t))dlsym(diff_handle, "difftest_exec");
    assert(ref_difftest_exec);

    ref_difftest_raise_intr = (void (*)(uint64_t))dlsym(diff_handle, "difftest_raise_intr");
    assert(ref_difftest_raise_intr);

    void (*ref_difftest_init)(int) = (void (*)(int))dlsym(diff_handle, "difftest_init");
    assert(ref_difftest_init);

    Log("Differential testing: %s", ANSI_FMT("ON", ANSI_FG_GREEN));
    Log("The result of every instruction will be compared with %s. "
        "This will help you a lot for debugging, but also significantly reduce the performance. "
        "If it is not necessary, you can turn it off in menuconfig.",
        ref_so_file);
    // 初始化和状态同步
    ref_difftest_init(port); // 初始化参考模拟器
    // 同步内存
    ref_difftest_memcpy(RESET_VECTOR, guest_to_host(RESET_VECTOR), img_size, DIFFTEST_TO_REF);
    // 同步寄存器
    cpu_copy(cpu, &dut_state);
    ref_difftest_regcpy(&dut_state, DIFFTEST_TO_REF);
}

void cpu_copy(const CPU *cpu, CPU_state *state)
{
    for (int i = 0; i < 32; i++)
    {
        state->gpr[i] = cpu->regs[i];
    }
    state->pc = *(vaddr_t *)cpu->pc;
}
bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc)
{
    // TODO
    if (dut_state.pc != ref_r->pc)
    {
        return false;
    }

    for (int i = 0; i < 32; i++)
    {
        if (dut_state.gpr[i] != ref_r->gpr[i])
        {
            Log("%s reg is different after executing instruction at pc = " FMT_WORD
                ", regs = " FMT_WORD ", right = " FMT_WORD ", wrong = " FMT_WORD ", diff = " FMT_WORD,
                "npc", pc, i, ref_r->gpr[i], dut_state.gpr[i], ref_r->gpr[i] ^ dut_state.gpr[i]);
            return false;
        }
    }
    return true;
}
static void checkregs(CPU_state *ref, vaddr_t pc, CPU *cpu)
{
    if (!isa_difftest_checkregs(ref, pc))
    {
        isa_reg_display(cpu);
        Assert(false, "regs wrong");
    }
}

void difftest_step(vaddr_t pc, vaddr_t npc, CPU *cpu)
{
    CPU_state ref_r;
    cpu_copy(cpu, &dut_state);
    if (skip_dut_nr_inst > 0)
    {
        ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);
        if (ref_r.pc == npc)
        {
            skip_dut_nr_inst = 0;
            checkregs(&ref_r, npc, cpu);
            return;
        }
        skip_dut_nr_inst--;
        if (skip_dut_nr_inst == 0)
            panic("can not catch up with ref.pc = " FMT_WORD " at pc = " FMT_WORD, ref_r.pc, pc);
        return;
    }

    if (is_skip_ref)
    {
        // to skip the checking of an instruction, just copy the reg state to reference design
        ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
        is_skip_ref = false;
        return;
    }
    ref_difftest_exec(1);
    ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

    checkregs(&ref_r, pc, cpu);
}
#endif