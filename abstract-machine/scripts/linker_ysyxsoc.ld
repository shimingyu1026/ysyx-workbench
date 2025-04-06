ENTRY(_start)
PHDRS { fsbl PT_LOAD; ssbl PT_LOAD; text PT_LOAD; rodata PT_LOAD;data PT_LOAD; bss PT_LOAD;}

MEMORY {
  mrom : ORIGIN = 0x20000000,  LENGTH = 4K
  sram : ORIGIN = 0x0f000000,  LENGTH = 8K
  flash : ORIGIN = 0x30000000, LENGTH = 16M 
  psram : ORIGIN = 0x80000000, LENGTH = 4M
}

SECTIONS {
    . = ORIGIN(flash);

    .fsbl : {
      *(entry)
    } >flash AT> flash : fsbl

    .ssbl : {
      _ssbl_start = .;
      *(bootloader)
      _ssbl_end = .;
    } >psram AT> flash : ssbl
    _ssbl_lma = LOADADDR(.ssbl);

    .text : {
      _text_start = .;
      *(.text*)
      _text_end = .;
    }> psram AT> flash : text
    _text_lma = LOADADDR(.text);

  .rodata : {
    _rodata_start = .;
    *(.rodata*)
    *(.srodata*)
    _rodata_end = .;
  }> psram AT> flash : rodata
  _rodata_lma = LOADADDR(.rodata);

  .data :{
    _data_start = .;
    *(.data.*)
    *(.sdata*)
    _data_end = .;
  }> psram AT> flash : data
  _data_lma = LOADADDR(.data);    /* LMA: flash中的地址 */

  .bss :{
	_bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    _bss_end = .;
  }> psram AT> flash : bss


  _heap_start = ALIGN(0x10);
  _heap_end = ORIGIN(psram) + LENGTH(psram) - 0x100;

  . = ORIGIN(sram);
  _stack_top = ALIGN(0x10); 
  . = ORIGIN(sram) + LENGTH(sram) - 0x10;
  _stack_pointer = .;

  ASSERT(SIZEOF(.fsbl) + SIZEOF(.text) + SIZEOF(.rodata) + SIZEOF(.data)<= LENGTH(flash), "FLASH overflow!")
}