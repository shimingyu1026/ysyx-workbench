package myCPU

import chisel3._
import chisel3.util._

class immExtendIO extends Bundle {
  val inst    = Input(UInt(32.W))
  val immType = Input(ImmTypesEnum())
  val imm     = Output(UInt(32.W))
}

class immExtend extends Module {
  val io        = IO(new immExtendIO)
  val inst31    = io.inst(31)
  val inst30_25 = io.inst(30, 25)
  val inst24_21 = io.inst(24, 21)
  val inst20    = io.inst(20)
  val inst11_8  = io.inst(11, 8)
  val inst7     = io.inst(7)
  val inst30_20 = io.inst(30, 20)
  val inst19_12 = io.inst(19, 12)

  io.imm := MuxLookup(io.immType, 0.U)(
    List(
      ImmTypesEnum.R -> 0.U,
      ImmTypesEnum.I -> Cat(Fill(21, inst31), inst30_25, inst24_21, inst20),
      ImmTypesEnum.S -> Cat(Fill(21, inst31), inst30_25, inst11_8, inst7),
      ImmTypesEnum.B -> Cat(Fill(20, inst31), inst7, inst30_25, inst11_8, 0.U),
      ImmTypesEnum.U -> Cat(inst31, inst30_20, inst19_12, Fill(12, 0.U)),
      ImmTypesEnum.J -> Cat(Fill(12, inst31), inst19_12, inst20, inst30_25, inst24_21, 0.U)
    )
  )
}
