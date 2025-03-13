package myCPU

import chisel3._
import chisel3.util._

object ImmTypesEnum extends ChiselEnum {
  val R, I, S, B, U, J, ShamtW, None = Value
}

object AluCtrlEnum extends ChiselEnum {
  val add, sub, and, or, xor, sll, srl, sra, slt, sltu, none = Value
}

object SrcASelEnum extends ChiselEnum {
  val pc, rs1, none = Value
}

object SrcBSelEnum extends ChiselEnum {
  val imm, rs2, none = Value
}

object BrTypeEnum extends ChiselEnum {
  val beq, bge, bgeu, blt, bltu, bne, none = Value
}

object BrSelEnum extends ChiselEnum {
  val alu, pcplus4 = Value
}

object MemWenEnum extends ChiselEnum {
  val wen, none = Value
}

object LoadCtrlEnum  extends ChiselEnum {
  val lb, lh, lw, lbu, lhu, none = Value
}
object StoreCtrlEnum extends ChiselEnum {
  val sb, sh, sw, none = Value
}

object WbSelEnum extends ChiselEnum {
  val alu, pcplus4, mem, imm = Value
}
