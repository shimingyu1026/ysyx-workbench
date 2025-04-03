.section entry, "ax"
.globl _start
.type _start, @function

_start:
  mv s0, zero
  la sp, _stack_pointer

  # 复制 .text 段内容（如果 .text 段有内容）
  la a0, _text_lma        # a0 = _text_lma 地址
  la a1, _text_start      # a1 = _text_start 地址
  la a2, _text_end        # a2 = _text_end 地址

  # 如果 _text_start 等于 _text_end，说明 .text 段为空，跳过复制
  beq a1, a2, skip_copy_text

copy_text:
  lw   t0, 0(a0)          # 从 _text_lma 地址（a0）加载一个字节到 t0
  sw   t0, 0(a1)          # 将字节存储到 _text_start 地址（a1）
  addi a0, a0, 4          # _text_lma 地址递增 1（下一字节）
  addi a1, a1, 4          # _text_start 地址递增 1（下一字节）
  bne  a1, a2, copy_text  # 如果 a1 没有到达 _text_end，继续复制

skip_copy_text:

  # 复制 .rodata 段内容（如果 .rodata 段有内容）
  la a0, _rodata_lma      # a0 = _rodata_lma 地址
  la a1, _rodata_start    # a1 = _rodata_start 地址
  la a2, _rodata_end      # a2 = _rodata_end 地址

  # 如果 _rodata_start 等于 _rodata_end，说明 .rodata 段为空，跳过复制
  beq a1, a2, skip_copy_rodata

copy_rodata:
  lw   t0, 0(a0)          # 从 _rodata_lma 地址（a0）加载一个字节到 t0
  sw   t0, 0(a1)          # 将字节存储到 _rodata_start 地址（a1）
  addi a0, a0, 4          # _rodata_lma 地址递增 1（下一字节）
  addi a1, a1, 4          # _rodata_start 地址递增 1（下一字节）
  bne  a1, a2, copy_rodata # 如果 a1 没有到达 _rodata_end，继续复制

skip_copy_rodata:

  # 复制 .data 段内容（如果 .data 段有内容）
  la a0, _data_lma        # a0 = _data_lma 地址
  la a1, _data_start      # a1 = _data_start 地址
  la a2, _bss_start        # a2 = _data_end 地址

  # 如果 _data_start 等于 _data_end，说明 .data 段为空，跳过复制
  beq a1, a2, skip_copy_data

copy_data:
  lW   t0, 0(a0)          # 从 _data_lma 地址（a0）加载一个字节到 t0
  sw   t0, 0(a1)          # 将字节存储到 _data_start 地址（a1）
  addi a0, a0, 4          # _data_lma 地址递增 1（下一字节）
  addi a1, a1, 4          # _data_start 地址递增 1（下一字节）
  bne  a1, a2, copy_data  # 如果 a1 没有到达 _data_end，继续复制

skip_copy_data:
  # 清零 .bss 段（如果 .bss 段有内容）
  la   a0, _bss_start     # a0 = _bss_start 地址
  la   a2, _bss_end       # a2 = _bss_end 地址

# 如果 _bss_start 等于 _bss_end，说明 .bss 段为空，跳过清零
  beq a0, a2, skip_clear_bss
  li   t0, 0              # t0 = 0，用于清零数据

clear_bss:
  sw   t0, 0(a0)          # 将 0 存储到 _bss_start 地址（a0）
  addi a0, a0, 4          # _bss_start 地址递增 1（下一字节）
  bne  a0, a2, clear_bss  # 如果 a0 没有到达 _bss_end，继续清零

skip_clear_bss:

done:
  addi a0, x0, 0
  addi a1, x0, 0
  addi a2, x0, 0
  addi t0, x0, 0

  call _trm_init
