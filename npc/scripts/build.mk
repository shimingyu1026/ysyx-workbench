.DEFAULT_GOAL = app

WORK_DIR  = $(shell pwd)
BUILD_DIR = $(WORK_DIR)/build
VERILOG_DIR =$(abspath ./verilog)
TOPNAME = ysyxSoCFull

INC_PATH := $(WORK_DIR)/include $(INC_PATH)
OBJ_DIR  = $(BUILD_DIR)/obj-$(NAME)$(SO)
BINARY   = $(BUILD_DIR)/$(NAME)$(SO)

CXX := g++
LD := $(CXX)
INCLUDES = $(addprefix -I, $(INC_PATH))
CFLAGS  := -MMD \
			$(INCLUDES) $(CFLAGS) \
			-DTOP_NAME=V$(TOPNAME) \
			-Wall -Werror
			
CXXFLAGS := $(CXXFLAGS)
LDFLAGS := $(LDFLAGS) -lreadline -ldl -pie
VERILATOR_CFLAGS += --cc --trace \
					--x-assign fast \
					--x-initial fast \
					--noassert \
					--build --exe \
					--autoflush \
					--timescale "1ns/1ns" \
					--no-timing \
					-y ../ysyxSoC/perip/uart16550/rtl \
					-y ../ysyxSoC/perip/spi/rtl \
					--Mdir $(OBJ_DIR) \
					--top-module $(TOPNAME) \
					-o $(abspath $(BINARY)) 


.PHONY: app clean




