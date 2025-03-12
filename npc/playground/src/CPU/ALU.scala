package myCPU

import chisel3._
import chisel3.util._

class ALUIO extends Bundle {
  val srcA = Input(UInt(32.W))
  val srcB = Input(UInt(32.W))
  val aluCrtl = Input(AluCrtlEnum())
  val aluResult = Output(UInt(32.W))
}

class ALU extends Module {
  val io = IO(new ALUIO)
  val shamt = io.srcB(4, 0).asUInt

  io.aluResult := MuxLookup(io.aluCrtl, 0.U)(
    List(
      AluCrtlEnum.add -> (io.srcA + io.srcB),
      AluCrtlEnum.sub -> (io.srcA - io.srcB),
      AluCrtlEnum.and -> (io.srcA & io.srcB),
      AluCrtlEnum.or -> (io.srcA | io.srcB),
      AluCrtlEnum.xor -> (io.srcA ^ io.srcB),
      AluCrtlEnum.sll -> (io.srcA << shamt)(31, 0),
      AluCrtlEnum.srl -> (io.srcA >> shamt).asUInt,
      AluCrtlEnum.sra -> (io.srcA.asSInt >> shamt).asUInt,
      AluCrtlEnum.slt -> (io.srcA.asSInt < io.srcB.asSInt).asUInt,
      AluCrtlEnum.sltu -> (io.srcA < io.srcB).asUInt
    )
  )
}
