package myCPU

import chisel3._
import chisel3.util._
object StateWBU extends ChiselEnum {
  val sIdle, sWaitValid = Value
}
class WBUIO     extends Bundle     {
  val mmu_to_wbu = Flipped(Decoupled(new mmu_to_wbu_io))

  // to regFile/idu
  val regWen_o = Output(Bool())
  val wbData_o = Output(UInt(32.W))

  val csr_wen_o   = Output(Bool())
  val csr_waddr_o = Output(UInt(12.W))
  val csr_wdata_o = Output(UInt(32.W))

}

class WBU extends Module {
  import StateWBU._
  val io = IO(new WBUIO)

  io.mmu_to_wbu.ready := false.B
  val state = RegInit(sIdle)
  switch(state) {
    is(sIdle) {
      when(io.mmu_to_wbu.ready) {
        state := sWaitValid
      }
    }
    is(sWaitValid) {
      when(io.mmu_to_wbu.valid) {
        state := sIdle
      }
    }
  }

  io.regWen_o := io.mmu_to_wbu.bits.regWen

  val wbData = MuxLookup(io.mmu_to_wbu.bits.wbSel, 0.U)(
    List(
      WbSelEnum.alu     -> io.mmu_to_wbu.bits.aluResult,
      WbSelEnum.pcplus4 -> io.mmu_to_wbu.bits.pcPlus4,
      WbSelEnum.mem     -> io.mmu_to_wbu.bits.mem,
      WbSelEnum.imm     -> io.mmu_to_wbu.bits.imm,
      WbSelEnum.csr     -> io.mmu_to_wbu.bits.csr_rdata
    )
  )

  io.wbData_o := wbData

  io.csr_wen_o   := io.mmu_to_wbu.bits.csr_wen
  io.csr_waddr_o := io.mmu_to_wbu.bits.csr_waddr
  io.csr_wdata_o := io.mmu_to_wbu.bits.csr_wdata
}
