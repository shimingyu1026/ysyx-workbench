package myCPU

import chisel3._
import chisel3.util._

class IDUIO extends Bundle {
  val inst = Input(UInt(32.W))
  val pc_in = Input(UInt(32.W))
  val wdata = Input(UInt(32.W)) // from WBU
  val pc_out = Output(UInt(32.W))
  val imm = Output(UInt(32.W))
  val rs1_data = Output(UInt(32.W))
  val rs2_data = Output(UInt(32.W))
}

class IDU extends Module {
  val io = IO(new IDUIO)

  val immExtend = Module(new immExtend)
  val regFile = Module(new regFile)
  val decoder = Module(new decoder)

  immExtend.io.inst := io.inst
  immExtend.io.immType := decoder.io.immType
  io.imm := immExtend.io.imm

  val rs1_addr = io.inst(19, 15)
  val rs2_addr = io.inst(24, 20)
  val rd_addr = io.inst(11, 7)
  regFile.io.rs1_addr := rs1_addr
  regFile.io.rs2_addr := rs2_addr
  regFile.io.rd_addr := rd_addr
  regFile.io.regWen := decoder.io.regWen
  regFile.io.wdata := io.wdata
  io.rs1_data := regFile.io.rs1_data
  io.rs2_data := regFile.io.rs2_data

  decoder.io.inst := io.inst

  io.pc_out := io.pc_in

}
