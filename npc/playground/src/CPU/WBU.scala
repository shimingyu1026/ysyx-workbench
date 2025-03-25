package myCPU

import chisel3._
import chisel3.util._

class WBUIO extends Bundle {

  // from mmu
  val mem_i = Input(UInt(32.W))

  // from exu
  val aluResult_i = Input(UInt(32.W))

  // from ifu
  val pcPlus4_i = Input(UInt(32.W))

  // from idu
  val imm_i    = Input(UInt(32.W))
  val wbSel_i  = Input(WbSelEnum())
  val regWen_i = Input(Bool())

  // csr
  val csr_wen_i   = Input(Bool())
  val csr_waddr_i = Input(UInt(12.W))
  val csr_wdata_i = Input(UInt(32.W))
  val csr_rdata_i = Input(UInt(32.W))

  // to regFile/idu
  val regWen_o = Output(Bool())
  val wbData_o = Output(UInt(32.W))

  val csr_wen_o   = Output(Bool())
  val csr_waddr_o = Output(UInt(12.W))
  val csr_wdata_o = Output(UInt(32.W))

}

class WBU extends Module {
  val io = IO(new WBUIO)

  io.regWen_o := io.regWen_i

  val wbData = MuxLookup(io.wbSel_i, 0.U)(
    List(
      WbSelEnum.alu     -> io.aluResult_i,
      WbSelEnum.pcplus4 -> io.pcPlus4_i,
      WbSelEnum.mem     -> io.mem_i,
      WbSelEnum.imm     -> io.imm_i,
      WbSelEnum.csr     -> io.csr_rdata_i
    )
  )

  io.wbData_o := wbData

  io.csr_wen_o   := io.csr_wen_i
  io.csr_waddr_o := io.csr_waddr_i
  io.csr_wdata_o := io.csr_wdata_i
}
