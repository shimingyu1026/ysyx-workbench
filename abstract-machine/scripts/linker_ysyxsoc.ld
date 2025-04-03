ENTRY(_start)
PHDRS { bootloader PT_LOAD;text PT_LOAD; data PT_LOAD; }

MEMORY {
  mrom : ORIGIN = 0x20000000,  LENGTH = 4K
  sram : ORIGIN = 0x0f000000,  LENGTH = 8K
  flash : ORIGIN = 0x30000000, LENGTH = 16M 
  psram : ORIGIN = 0x80000000, LENGTH = 4M
}

SECTIONS {
    . = ORIGIN(flash);
    .bootloader : {
      *(entry)
    } >flash AT> flash : bootloader

    .text : {
      _text_start = .;
      *(.text*)
      _text_end = .;
    }> sram AT> flash : text
    _text_lma = LOADADDR(.text);

  etext = .;
  _etext = .;

  .rodata : {
    _rodata_start = .;
    *(.rodata*)
    *(.srodata*)
    _rodata_end = .;
  }> sram AT> flash
  _rodata_lma = LOADADDR(.rodata);

  .data :{
    _data_start = .;
    *(.data)
    *(.sdata*)
    _data_end = .;
  }> sram AT> flash : data
  _data_lma = LOADADDR(.data);    /* LMA: flash中的地址 */

  .bss :{
	_bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    _bss_end = .;
  }> sram AT> flash

  _stack_top = ALIGN(0x10); 
  . = _stack_top + 0x800;
  _stack_pointer = .;


  _heap_start = ALIGN(0x10);
  _heap_end = ORIGIN(sram) + LENGTH(sram) - 0x10;

  ASSERT(_heap_end <= ORIGIN(sram) + LENGTH(sram), "SRAM overflow!")
  ASSERT(_heap_start <= _heap_end, "Invalid heap range!")
  ASSERT(SIZEOF(.text) + SIZEOF(.rodata) + SIZEOF(.data)<= LENGTH(flash), "FLASH overflow!")
}