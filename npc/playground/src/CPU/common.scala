package myCPU

import chisel3._
import chisel3.util._

object ImmTypesEnum extends ChiselEnum {
  val R, I, S, B, U, J, ShamtW, None = Value
}

object AluCrtlEnum extends ChiselEnum {
  val add, sub, and, or, xor, sll, srl, sra, slt, sltu = Value
}
