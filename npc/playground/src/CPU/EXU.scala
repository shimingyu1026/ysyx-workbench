package myCPU

import chisel3._
import chisel3.util._
import scala.collection.immutable.ArraySeq

class EXUIO extends Bundle {

  // from idu
  val rs1_data_i = Input(UInt(32.W))
  val rs2_data_i = Input(UInt(32.W))
  val imm_i      = Input(UInt(32.W))

  val srcASel_i   = Input(SrcASelEnum())
  val srcBSel_i   = Input(SrcBSelEnum())
  val brType_i    = Input(BrTypeEnum())
  val aluCtrl_i   = Input(AluCtrlEnum())
  val pcSel_i     = Input(PCSelEnum())
  val wbSel_i     = Input(WbSelEnum())
  val regWen_i    = Input(Bool())
  val memWen_i    = Input(MemWenEnum())
  val loadCtrl_i  = Input(LoadCtrlEnum())
  val storeCtrl_i = Input(StoreCtrlEnum())

  val csr_rdata_i = Input(UInt(32.W))
  val csr_wen_i   = Input(Bool())
  val csr_waddr_i = Input(UInt(12.W))

  // from ifu
  val pc_i      = Input(UInt(32.W))
  val pcPlus4_i = Input(UInt(32.W))

  // to wbu
  val pcPlus4_o = Output(UInt(32.W))
  val wbSel_o   = Output(WbSelEnum())

  // to ifu
  val brSel_o     = Output(BrSelEnum())
  val pcBranchJ_o = Output(UInt(32.W))
  val pcSel_o     = Output(PCSelEnum())

  // to idu
  val regWen_o = Output(Bool())

  // to mmu
  val memWen_o    = Output(MemWenEnum())
  val loadCtrl_o  = Output(LoadCtrlEnum())
  val storeCtrl_o = Output(StoreCtrlEnum())
  val imm_o       = Output(UInt(32.W))
  val rs2_data_o  = Output(UInt(32.W))
  val aluResult_o = Output(UInt(32.W))

  // csr
  val csr_wdata_o = Output(UInt(32.W))
  val csr_waddr_o = Output(UInt(12.W))
  val csr_wen_o   = Output(Bool())
  val csr_rdata_o = Output(UInt(32.W))
}

class EXU extends Module {
  val io = IO(new EXUIO)

  val alu = Module(new ALU)

  val srcA = MuxLookup(io.srcASel_i, 0.U)(
    List(
      SrcASelEnum.pc  -> io.pc_i,
      SrcASelEnum.rs1 -> io.rs1_data_i,
      SrcASelEnum.csr -> io.csr_rdata_i
    )
  )

  val srcB = MuxLookup(io.srcBSel_i, 0.U)(
    List(
      SrcBSelEnum.imm  -> io.imm_i,
      SrcBSelEnum.rs2  -> io.rs2_data_i,
      SrcBSelEnum.zero -> 0.U,
      SrcBSelEnum.csr  -> io.csr_rdata_i
    )
  )
  alu.io.srcA := srcA
  alu.io.srcB    := srcB
  alu.io.aluCtrl := io.aluCtrl_i

  val brSel = MuxCase(
    BrSelEnum.pcplus4,
    ArraySeq(
      (io.brType_i === BrTypeEnum.beq && io.rs1_data_i === io.rs2_data_i)              -> BrSelEnum.alu,
      (io.brType_i === BrTypeEnum.bge && io.rs1_data_i.asSInt >= io.rs2_data_i.asSInt) -> BrSelEnum.alu,
      (io.brType_i === BrTypeEnum.bgeu && io.rs1_data_i >= io.rs2_data_i)              -> BrSelEnum.alu,
      (io.brType_i === BrTypeEnum.blt && io.rs1_data_i.asSInt < io.rs2_data_i.asSInt)  -> BrSelEnum.alu,
      (io.brType_i === BrTypeEnum.bltu && io.rs1_data_i < io.rs2_data_i)               -> BrSelEnum.alu,
      (io.brType_i === BrTypeEnum.bne && io.rs1_data_i =/= io.rs2_data_i)              -> BrSelEnum.alu
    )
  )

  io.brSel_o     := brSel
  io.pcBranchJ_o := alu.io.aluResult

  io.pcPlus4_o   := io.pcPlus4_i
  io.pcSel_o     := io.pcSel_i
  io.wbSel_o     := io.wbSel_i
  io.regWen_o    := io.regWen_i
  io.memWen_o    := io.memWen_i
  io.loadCtrl_o  := io.loadCtrl_i
  io.storeCtrl_o := io.storeCtrl_i
  io.imm_o       := io.imm_i
  io.rs2_data_o  := io.rs2_data_i
  io.aluResult_o := alu.io.aluResult

  io.csr_wdata_o := alu.io.aluResult
  io.csr_wen_o   := io.csr_wen_i
  io.csr_rdata_o := io.csr_rdata_i
  io.csr_waddr_o := io.csr_waddr_i
}
