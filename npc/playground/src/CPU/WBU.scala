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

  // to regFile/idu
  val regWen_o = Output(Bool())
  val wbData_o = Output(UInt(32.W))
}

class WBU extends Module {
  val io = IO(new WBUIO)

  io.regWen_o := io.regWen_i

  val wbData = MuxLookup(io.wbSel_i, 0.U)(
    List(
      WbSelEnum.alu     -> io.aluResult_i,
      WbSelEnum.pcplus4 -> io.pcPlus4_i,
      WbSelEnum.mem     -> io.mem_i,
      WbSelEnum.imm     -> io.imm_i
    )
  )

  io.wbData_o := wbData

}
