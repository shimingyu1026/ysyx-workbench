package myCPU

import chisel3._
import chisel3.util._
import scala.collection.immutable.ArraySeq
import scala.annotation.switch

object StateEXU extends ChiselEnum {
  val sIdle, sWaitValid, sWaitReady = Value
}

class EXUIO extends Bundle {

  val idu_to_exu = Flipped(Decoupled(new idu_to_exu_io))
  val exu_to_mmu = Decoupled(new exu_to_mmu_io)

  // to ifu
  val brSel_o     = Output(BrSelEnum())
  val pcBranchJ_o = Output(UInt(32.W))
  val pcSel_o     = Output(PCSelEnum())

}

class EXU extends Module {
  import StateEXU._
  val io = IO(new EXUIO)

  io.idu_to_exu.ready := false.B
  io.exu_to_mmu.valid := false.B
  val state = RegInit(sIdle)
  switch(state) {
    is(sIdle) {
      when(io.idu_to_exu.ready) {
        state := sWaitValid
      }
    }
    is(sWaitValid) {
      when(io.idu_to_exu.valid) {
        state := sWaitReady
      }
    }
    is(sWaitReady) {
      when(io.exu_to_mmu.ready) {
        state := sIdle
      }
    }
  }

  val alu = Module(new ALU)

  val rs1_data  = io.idu_to_exu.bits.rs1_data
  val rs2_data  = io.idu_to_exu.bits.rs2_data
  val csr_rdata = io.idu_to_exu.bits.csr_rdata
  val srcA      = MuxLookup(io.idu_to_exu.bits.srcASel, 0.U)(
    List(
      SrcASelEnum.pc  -> io.idu_to_exu.bits.pc,
      SrcASelEnum.rs1 -> rs1_data,
      SrcASelEnum.csr -> csr_rdata
    )
  )

  val srcB = MuxLookup(io.idu_to_exu.bits.srcBSel, 0.U)(
    List(
      SrcBSelEnum.imm  -> io.idu_to_exu.bits.imm,
      SrcBSelEnum.rs2  -> rs2_data,
      SrcBSelEnum.zero -> 0.U,
      SrcBSelEnum.csr  -> csr_rdata
    )
  )
  alu.io.srcA := srcA
  alu.io.srcB    := srcB
  alu.io.aluCtrl := io.idu_to_exu.bits.aluCtrl

  val brType = io.idu_to_exu.bits.brType
  val brSel  = MuxCase(
    BrSelEnum.pcplus4,
    ArraySeq(
      (brType === BrTypeEnum.beq && rs1_data === rs2_data)              -> BrSelEnum.alu,
      (brType === BrTypeEnum.bge && rs1_data.asSInt >= rs2_data.asSInt) -> BrSelEnum.alu,
      (brType === BrTypeEnum.bgeu && rs1_data >= rs2_data)              -> BrSelEnum.alu,
      (brType === BrTypeEnum.blt && rs1_data.asSInt < rs2_data.asSInt)  -> BrSelEnum.alu,
      (brType === BrTypeEnum.bltu && rs1_data < rs2_data)               -> BrSelEnum.alu,
      (brType === BrTypeEnum.bne && rs1_data =/= rs2_data)              -> BrSelEnum.alu
    )
  )
//-----------------------------------------------------------------------------------
  io.exu_to_mmu.bits.pcPlus4 := io.idu_to_exu.bits.pcPlus4
  io.exu_to_mmu.bits.wbSel     := io.idu_to_exu.bits.wbSel
  io.exu_to_mmu.bits.regWen    := io.idu_to_exu.bits.regWen
  io.exu_to_mmu.bits.memWen    := io.idu_to_exu.bits.memWen
  io.exu_to_mmu.bits.loadCtrl  := io.idu_to_exu.bits.loadCtrl
  io.exu_to_mmu.bits.storeCtrl := io.idu_to_exu.bits.storeCtrl
  io.exu_to_mmu.bits.imm       := io.idu_to_exu.bits.imm
  io.exu_to_mmu.bits.rs2_data  := rs2_data
  io.exu_to_mmu.bits.aluResult := alu.io.aluResult

  io.exu_to_mmu.bits.csr_wdata := alu.io.aluResult
  io.exu_to_mmu.bits.csr_wen   := io.idu_to_exu.bits.csr_wen
  io.exu_to_mmu.bits.csr_rdata := io.idu_to_exu.bits.csr_rdata
  io.exu_to_mmu.bits.csr_waddr := io.idu_to_exu.bits.csr_waddr
//-----------------------------------------------------------------------------------
  io.brSel_o                   := brSel
  io.pcBranchJ_o               := alu.io.aluResult
  io.pcSel_o                   := io.idu_to_exu.bits.pcSel

}
