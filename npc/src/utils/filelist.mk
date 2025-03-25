LIBCAPSTONE = tools/capstone/repo/libcapstone.so.5
CFLAGS += -I/home/smy/ysyx-workbench/npc/tools/capstone/repo/include
src/utils/disasm.cc: $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(MAKE) -C tools/capstone