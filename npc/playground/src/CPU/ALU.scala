package myCPU

import chisel3._
import chisel3.util._

class ALUIO extends Bundle {
  val srcA      = Input(UInt(32.W))
  val srcB      = Input(UInt(32.W))
  val aluCtrl   = Input(AluCtrlEnum())
  val aluResult = Output(UInt(32.W))
}

class ALU extends Module {
  val io    = IO(new ALUIO)
  val shamt = io.srcB(4, 0).asUInt

  io.aluResult := MuxLookup(io.aluCtrl, 0.U)(
    List(
      AluCtrlEnum.add  -> (io.srcA + io.srcB),
      AluCtrlEnum.sub  -> (io.srcA - io.srcB),
      AluCtrlEnum.and  -> (io.srcA & io.srcB),
      AluCtrlEnum.or   -> (io.srcA | io.srcB),
      AluCtrlEnum.xor  -> (io.srcA ^ io.srcB),
      AluCtrlEnum.sll  -> (io.srcA << shamt)(31, 0),
      AluCtrlEnum.srl  -> (io.srcA >> shamt).asUInt,
      AluCtrlEnum.sra  -> (io.srcA.asSInt >> shamt).asUInt,
      AluCtrlEnum.slt  -> (io.srcA.asSInt < io.srcB.asSInt).asUInt,
      AluCtrlEnum.sltu -> (io.srcA < io.srcB).asUInt
    )
  )
}
