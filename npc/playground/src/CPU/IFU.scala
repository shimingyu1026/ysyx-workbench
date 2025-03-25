package myCPU

import chisel3._
import chisel3.util._
object StateIFU extends ChiselEnum {
  val sIdle, sWaitReady = Value
}

class IFUIO extends Bundle {

  // from idu
  val pcSel_i     = Input(PCSelEnum())
  val brSel_i     = Input(BrSelEnum())
  // from exu
  val pcBranchJ_i = Input(UInt(32.W))
  // from csr
  val csr_i       = Input(UInt(32.W))

//from inst memory
  val inst_i = Input(UInt(32.W))

  val ifu_to_idu = Decoupled(new ifu_idu_io)
}

class IFU extends Module {
  import StateIFU._
  val io      = IO(new IFUIO)
  // 状态机寄存器
  val state   = RegInit(sIdle)
  dontTouch(state)
//状态转移
  switch(state) {
    is(sIdle) {
      when(io.ifu_to_idu.valid) {
        state := sWaitReady
      }
    }
    is(sWaitReady) {
      when(io.ifu_to_idu.ready) {
        state := sIdle
      }
    }
  }
  val PC      = RegInit("h80000000".U(32.W))
  val pcPlus4 = PC + 4.U

  val jalPC  = io.pcBranchJ_i
  val jalrPC = io.pcBranchJ_i & "hfffffffe".U

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
      PCSelEnum.branch  -> brPC,
      PCSelEnum.csr     -> io.csr_i,
      PCSelEnum.none    -> PC
    )
  )
  PC := PCNext
//--------------------------------------------------------------
  // 由instFetch影响
  io.ifu_to_idu.valid := true.B

  // 数据
  io.ifu_to_idu.bits.pc      := PC
  io.ifu_to_idu.bits.inst    := io.inst_i
  io.ifu_to_idu.bits.pcPlus4 := pcPlus4
  // ---------------------------------------------------------------
  // printf("io.pcBranchJ_i: %x\n", io.pcBranchJ_i)
  dontTouch(io)
}
