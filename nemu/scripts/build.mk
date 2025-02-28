.DEFAULT_GOAL = app 
# 设置默认构建目标为 app。当直接运行 make 时，会自动构建 app 目标。

# Add necessary options if the target is a shared library
ifeq ($(SHARE),1)
SO = -so
CFLAGS  += -fPIC -fvisibility=hidden 
LDFLAGS += -shared -fPIC
endif

WORK_DIR  = $(shell pwd)
BUILD_DIR = $(WORK_DIR)/build

INC_PATH := $(WORK_DIR)/include $(INC_PATH)
OBJ_DIR  = $(BUILD_DIR)/obj-$(NAME)$(SO)
BINARY   = $(BUILD_DIR)/$(NAME)$(SO)

# Compilation flags
ifeq ($(CC),clang)
CXX := clang++
else
CXX := g++
endif
LD := $(CXX)
# 将 INC_PATH 中的路径添加 -I 前缀（如 -I./include）。
INCLUDES = $(addprefix -I, $(INC_PATH)) 
CFLAGS  := -O2 -MMD -Wall -Werror $(INCLUDES) $(CFLAGS)
LDFLAGS := -O2 $(LDFLAGS)

OBJS = $(SRCS:%.c=$(OBJ_DIR)/%.o) $(CXXSRC:%.cc=$(OBJ_DIR)/%.o)

# Compilation patterns
# 定义了一个模式规则，描述如何从 .c 文件生成对应的 .o 目标文件。
#% 是通配符，匹配文件名（不含扩展名）
$(OBJ_DIR)/%.o: %.c 
	@echo + CC $<  
	@mkdir -p $(dir $@) 
	@$(CC) $(CFLAGS) -c -o $@ $< 
	$(call call_fixdep, $(@:.o=.d), $@)

$(OBJ_DIR)/%.o: %.cc
	@echo + CXX $<
	@mkdir -p $(dir $@)
	@$(CXX) $(CFLAGS) $(CXXFLAGS) -c -o $@ $<
	$(call call_fixdep, $(@:.o=.d), $@)

# Depencies
# 包含所有 .d 依赖文件（由 -MMD 生成）。
-include $(OBJS:.o=.d) 

# Some convenient rules

.PHONY: app clean

app: $(BINARY)

# 双冒号规则：允许为同一目标定义多个规则。
$(BINARY):: $(OBJS) $(ARCHIVES) 
	@echo + LD $@
	@$(LD) -o $@ $(OBJS) $(LDFLAGS) $(ARCHIVES) $(LIBS)

clean:
	-rm -rf $(BUILD_DIR)
