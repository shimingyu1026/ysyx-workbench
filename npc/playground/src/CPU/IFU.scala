package myCPU

import chisel3._
import chisel3.util._

class IFUIO extends Bundle {
  val pc_o = Output(UInt(32.W))

  // from idu
  val pcSel_i = Input(PCSelEnum())
  val brSel_i = Input(BrSelEnum())

  // from exu
  val pcBranchJ_i = Input(UInt(32.W))

//from inst memory
  val inst_i = Input(UInt(32.W))

  // to idu
  val inst_o = Output(UInt(32.W))
}

class IFU extends Module {
  val io = IO(new IFUIO)

  val PC = RegInit(0.U(32.W))

  val pcPlus4 = PC + 4.U

  val jalPC  = io.pcBranchJ_i
  val jalrPC = io.pcBranchJ_i & 0xfffffffe.U

  val branchPC = io.pcBranchJ_i
  val brPC     = MuxLookup(io.brSel_i, pcPlus4)(
    List(
      BrSelEnum.pcplus4 -> pcPlus4,
      BrSelEnum.alu     -> branchPC
    )
  )

  val PCNext = MuxLookup(io.pcSel_i, pcPlus4)(
    List(
      PCSelEnum.pcplus4 -> pcPlus4,
      PCSelEnum.jal     -> jalPC,
      PCSelEnum.jalr    -> jalrPC,
      PCSelEnum.branch  -> brPC
    )
  )
  PC := PCNext

  io.pc_o   := PC
  io.inst_o := io.inst_i

}
