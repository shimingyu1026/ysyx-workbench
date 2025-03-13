package myCPU

import chisel3._
import chisel3.util._
import scala.collection.immutable.ArraySeq

class EXUIO extends Bundle {
  val rs1_data = Input(UInt(32.W))
  val rs2_data = Input(UInt(32.W))
  val pc       = Input(UInt(32.W))
  val imm      = Input(UInt(32.W))

  val aluCtrl = Input(AluCtrlEnum())
  val srcASel = Input(SrcASelEnum())
  val srcBSel = Input(SrcBSelEnum())
  val brType  = Input(BrTypeEnum())

  // to ifu
  val brSel    = Output(BrSelEnum())
  val pcBranch = Output(UInt(32.W))
}

class EXU extends Module {
  val io = IO(new EXUIO)

  val alu = Module(new ALU)

  val srcA = MuxLookup(io.srcASel, 0.U)(
    List(
      SrcASelEnum.pc  -> io.pc,
      SrcASelEnum.rs1 -> io.rs1_data
    )
  )

  val srcB = MuxLookup(io.srcBSel, 0.U)(
    List(
      SrcBSelEnum.imm -> io.imm,
      SrcBSelEnum.rs2 -> io.rs2_data
    )
  )
  alu.io.srcA := srcA
  alu.io.srcB    := srcB
  alu.io.aluCtrl := io.aluCtrl

  val brSel = MuxCase(
    BrSelEnum.pcplus4,
    ArraySeq(
      (io.brType === BrTypeEnum.beq && io.rs1_data === io.rs2_data)              -> BrSelEnum.alu,
      (io.brType === BrTypeEnum.bge && io.rs1_data.asSInt >= io.rs2_data.asSInt) -> BrSelEnum.alu,
      (io.brType === BrTypeEnum.bgeu && io.rs1_data >= io.rs2_data)              -> BrSelEnum.alu,
      (io.brType === BrTypeEnum.blt && io.rs1_data.asSInt < io.rs2_data.asSInt)  -> BrSelEnum.alu,
      (io.brType === BrTypeEnum.bltu && io.rs1_data < io.rs2_data)               -> BrSelEnum.alu,
      (io.brType === BrTypeEnum.bne && io.rs1_data =/= io.rs2_data)              -> BrSelEnum.alu
    )
  )

  io.brSel    := brSel
  io.pcBranch := alu.io.aluResult
}
