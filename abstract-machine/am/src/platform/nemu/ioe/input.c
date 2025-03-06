#include <am.h>
#include <nemu.h>

#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  size_t data = inw(KBD_ADDR);
  kbd->keydown = ((data >> 12) == 0x8) ? 1 : 0;
  kbd->keycode = data & 0xFFF;
}
