ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }

MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
  flash : ORIGIN = 0x30000000, LENGTH = 256M
}

SECTIONS {
    . = ORIGIN(flash);
    .text : {
    *(entry)
    *(.text*)
  }> flash AT> flash : text

  etext = .;
  _etext = .;

  .rodata : ALIGN(4){
    *(.rodata*)
    *(.srodata*)
    . = ALIGN(4);
  }> flash AT> flash


  .data : ALIGN(4){
    _data_start = .;
    *(.data)
    *(.sdata*)
    . = ALIGN(4);
    _data_end = .;
  }> sram AT> flash : data
 _data_lma = LOADADDR(.data);    /* LMA: flash中的地址 */

  edata = .;
  _data = .;

  .bss : ALIGN(4){
	_bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(4);
    _bss_end = .;
  }> sram AT> flash
  _stack_top = ALIGN(0x10); 
  . = _stack_top + 0x800;
  _stack_pointer = .;
  end = .;
  _end = .;

  _heap_start = ALIGN(0x10);
  _heap_end = ORIGIN(sram) + LENGTH(sram) - 0x100;

  ASSERT(_heap_end <= ORIGIN(sram) + LENGTH(sram), "SRAM overflow!")
  ASSERT(SIZEOF(.text) + SIZEOF(.rodata) <= LENGTH(flash), "FLASH overflow!")
}