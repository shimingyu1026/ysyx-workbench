package myCPU

import chisel3._
import chisel3.util._

class regFileIO extends Bundle {
  val rs1_addr = Input(UInt(5.W))
  val rs2_addr = Input(UInt(5.W))
  val rd_addr  = Input(UInt(5.W))
  val regWen   = Input(Bool())
  val wdata    = Input(UInt(32.W))
  val rs1_data = Output(UInt(32.W))
  val rs2_data = Output(UInt(32.W))
}

class regFile extends Module {
  val io   = IO(new regFileIO)
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  io.rs1_data := regs(io.rs1_addr)
  io.rs2_data := regs(io.rs2_addr)

  when(io.regWen && io.rd_addr =/= 0.U) {
    regs(io.rd_addr) := io.wdata
  }
}
