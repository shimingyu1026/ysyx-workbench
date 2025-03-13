package myCPU

import chisel3._
import chisel3.util._

class IFUIO extends Bundle {
  val pc      = Output(UInt(32.W))
  val pcEn    = Input(Bool())
  val memInst = Input(UInt(32.W))
  val inst    = Output(UInt(32.W))
}

class IFU extends Module {
  val io = IO(new IFUIO)

  val PC = RegInit(0.U(32.W))

  val PCPlus4 = PC + 4.U

  when(io.pcEn) {
    PC := PCPlus4
  }.otherwise {
    PC := PC
  }

  io.pc := PC
}
