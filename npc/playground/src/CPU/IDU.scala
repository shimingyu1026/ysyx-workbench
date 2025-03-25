package myCPU

import chisel3._
import chisel3.util._

object StateIDU extends ChiselEnum {
  val sIdle, sWaitValid, sWaitReady = Value
}
class IDUIO     extends Bundle     {

  val ifu_to_idu = Flipped(Decoupled(new ifu_idu_io))
  val idu_to_exu = Decoupled(new idu_to_exu_io)

//from wbu
  val wbdata_i = Input(UInt(32.W))
  val regWen_i = Input(Bool())

  val csr_wen_i   = Input(Bool())
  val csr_waddr_i = Input(UInt(12.W))
  val csr_wdata_i = Input(UInt(32.W))

  val csr_o = Output(UInt(32.W))

  val npcTrap = Output(Bool())

  // for verilator
  val regs = Output(Vec(32, UInt(32.W)))

}

class IDU extends Module {
  import StateIDU._
  val io = IO(new IDUIO)

  io.ifu_to_idu.ready := false.B
  io.idu_to_exu.valid := false.B

  val state = RegInit(sIdle)
  switch(state) {
    is(sIdle) {
      when(io.ifu_to_idu.ready) {
        state := sWaitValid
      }
    }
    is(sWaitValid) {
      when(io.ifu_to_idu.valid) {
        state := sWaitReady
      }
    }
    is(sWaitReady) {
      when(io.idu_to_exu.ready) {
        state := sIdle
      }
    }
  }

  val immExtend = Module(new immExtend)
  val regFile   = Module(new regFile)
  val csrFile   = Module(new csrFile)
  val decoder   = Module(new decoder)

  immExtend.io.inst    := io.ifu_to_idu.bits.inst
  immExtend.io.immType := decoder.io.immType

  val inst = io.ifu_to_idu.bits.inst

  val rs1_addr = inst(19, 15)
  val rs2_addr = inst(24, 20)
  val rd_addr  = inst(11, 7)
  val csr_addr = inst(31, 20)
  regFile.io.rs1_addr := rs1_addr
  regFile.io.rs2_addr := rs2_addr
  regFile.io.rd_addr  := rd_addr
  regFile.io.regWen   := io.regWen_i
  regFile.io.wdata    := io.wbdata_i

  csrFile.io.csr_raddr_i := csr_addr
  csrFile.io.csr_wen_i   := io.csr_wen_i
  csrFile.io.csr_wdata_i := io.csr_wdata_i
  csrFile.io.csr_waddr_i := io.csr_waddr_i
  csrFile.io.csr_ctrl_i  := decoder.io.csr_ctrl
  csrFile.io.pc_i        := io.ifu_to_idu.bits.pc

  decoder.io.inst := inst

  // ---------------------------------------------------------------------------------
  io.idu_to_exu.bits.pc       := io.ifu_to_idu.bits.pc
  io.idu_to_exu.bits.imm      := immExtend.io.imm
  io.idu_to_exu.bits.rs1_data := regFile.io.rs1_data
  io.idu_to_exu.bits.rs2_data := regFile.io.rs2_data

  io.idu_to_exu.bits.srcASel   := decoder.io.srcASel
  io.idu_to_exu.bits.srcBSel   := decoder.io.srcBSel
  io.idu_to_exu.bits.aluCtrl   := decoder.io.aluCtrl
  io.idu_to_exu.bits.brType    := decoder.io.brType
  io.idu_to_exu.bits.storeCtrl := decoder.io.storeCtrl
  io.idu_to_exu.bits.loadCtrl  := decoder.io.loadCtrl
  io.idu_to_exu.bits.memWen    := decoder.io.memWen
  io.idu_to_exu.bits.regWen    := decoder.io.regWen
  io.idu_to_exu.bits.wbSel     := decoder.io.wbSel
  io.idu_to_exu.bits.pcSel     := decoder.io.pcSel
  io.idu_to_exu.bits.csr_wen   := decoder.io.csr_wen

  io.idu_to_exu.bits.pcPlus4   := io.ifu_to_idu.bits.pcPlus4
  io.idu_to_exu.bits.csr_rdata := csrFile.io.csr_rdata_o
  io.idu_to_exu.bits.csr_waddr := csr_addr
  // ---------------------------------------------------------------------------------

  io.npcTrap := decoder.io.npcTrap

  io.regs := regFile.io.regs

  io.csr_o := csrFile.io.csr_rdata_o

  when(decoder.io.csr_ctrl === CSRCtrlEnum.ecall) {
    // printf("ecall\n")
  }

}
