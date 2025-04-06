.section entry, "ax"
.globl _start
.type _start, @function

_start:
  mv s0, zero
  la sp, _stack_pointer

  # 复制 .text 段内容（如果 .text 段有内容）
  la a0, _ssbl_lma        # a0 = _ssbl_lma 地址
  la a1, _ssbl_start      # a1 = _ssbl_start 地址
  la a2, _ssbl_end        # a2 = _ssbl_end 地址

copy_text:
  lw   t0, 0(a0)          # 从 _ssbl_lma 地址（a0）加载一个字节到 t0
  sw   t0, 0(a1)          # 将字节存储到 _ssbl_start 地址（a1）
  addi a0, a0, 4          # _ssbl_lma 地址递增 1（下一字节）
  addi a1, a1, 4          # _ssbl_start 地址递增 1（下一字节）
  bne  a1, a2, copy_text  # 如果 a1 没有到达 _ssbl_end，继续复制

  call _ssbl




