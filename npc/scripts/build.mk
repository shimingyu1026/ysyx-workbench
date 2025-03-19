.DEFAULT_GOAL = app

WORK_DIR  = $(shell pwd)
BUILD_DIR = $(WORK_DIR)/build
VERILOG_DIR =$(abspath ./verilog)
TOPNAME = top

INC_PATH := $(WORK_DIR)/include $(INC_PATH)
OBJ_DIR  = $(BUILD_DIR)/obj-$(NAME)$(SO)
BINARY   = $(BUILD_DIR)/$(NAME)$(SO)

CXX := g++
LD := $(CXX)
INCLUDES = $(addprefix -I, $(INC_PATH))
CFLAGS  := -MMD -Wall -Werror $(INCLUDES) $(CFLAGS) \
			-DTOP_NAME="\"V$(TOPNAME)\"" \
			
CXXFLAGS := $(CXXFLAGS)
LDFLAGS := $(LDFLAGS) -lreadline -ldl -pie
VERILATOR_CFLAGS += --cc --trace \
					--x-assign fast \
					--x-initial fast \
					--noassert \
					--build --exe \
					--Mdir $(OBJ_DIR) \
					--top-module $(TOPNAME) \
					-o $(abspath $(BINARY)) 


.PHONY: app clean




