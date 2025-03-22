include $(NPC_HOME)/scripts/build.mk

override ARGS ?= --log=$(BUILD_DIR)/npc-log.txt
override ARGS += $(ARGS_DIFF)

# Command to execute NEMU
IMG ?=
NPC_EXEC := $(BINARY) $(ARGS) $(IMG)

run-env: $(BINARY) $(DIFF_REF_SO)

#run: run-env
#	$(NPC_EXEC)

.PHONY: run  run-env 