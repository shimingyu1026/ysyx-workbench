package myCPU

import chisel3._
import chisel3.util._

class IDUIO extends Bundle {

//from ifu
  val inst_i    = Input(UInt(32.W))
  val pcPlus4_i = Input(UInt(32.W))
  val pc_i      = Input(UInt(32.W))

//from wbu
  val wbdata_i = Input(UInt(32.W))
  val regWen_i = Input(Bool())

  val csr_wen_i   = Input(Bool())
  val csr_waddr_i = Input(UInt(12.W))
  val csr_wdata_i = Input(UInt(32.W))

//to exu
  val pc_o       = Output(UInt(32.W))
  val imm_o      = Output(UInt(32.W)) // also to wbu
  val rs1_data_o = Output(UInt(32.W))
  val rs2_data_o = Output(UInt(32.W))

  val srcASel_o = Output(SrcASelEnum())
  val srcBSel_o = Output(SrcBSelEnum())
  val aluCtrl_o = Output(AluCtrlEnum())
  val brType_o  = Output(BrTypeEnum())

  // to mmu
  val storeCtrl_o = Output(StoreCtrlEnum())
  val loadCtrl_o  = Output(LoadCtrlEnum())
  val memWen_o    = Output(MemWenEnum())

  // to wbu
  val pcPlus4_o = Output(UInt(32.W))

  // to wbu and go back to idu
  val regWen_o = Output(Bool())
  val wbSel_o  = Output(WbSelEnum())

  // to ifu
  val pcSel_o = Output(PCSelEnum())
  val csr_o   = Output(UInt(32.W))

  val npcTrap = Output(Bool())

  // for verilator
  val regs = Output(Vec(32, UInt(32.W)))

  // csr
  val csr_rdata_o = Output(UInt(32.W))
  val csr_waddr_o = Output(UInt(12.W))
  val csr_wen_o   = Output(Bool())
}

class IDU extends Module {
  val io = IO(new IDUIO)

  val immExtend = Module(new immExtend)
  val regFile   = Module(new regFile)
  val csrFile   = Module(new csrFile)
  val decoder   = Module(new decoder)

  immExtend.io.inst    := io.inst_i
  immExtend.io.immType := decoder.io.immType

  val rs1_addr = io.inst_i(19, 15)
  val rs2_addr = io.inst_i(24, 20)
  val rd_addr  = io.inst_i(11, 7)
  val csr_addr = io.inst_i(31, 20)
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
  csrFile.io.pc_i        := io.pc_i

  decoder.io.inst := io.inst_i

  io.pc_o       := io.pc_i
  io.imm_o      := immExtend.io.imm
  io.rs1_data_o := regFile.io.rs1_data
  io.rs2_data_o := regFile.io.rs2_data

  io.srcASel_o := decoder.io.srcASel
  io.srcBSel_o := decoder.io.srcBSel
  io.aluCtrl_o := decoder.io.aluCtrl
  io.brType_o  := decoder.io.brType

  io.storeCtrl_o := decoder.io.storeCtrl
  io.loadCtrl_o  := decoder.io.loadCtrl
  io.memWen_o    := decoder.io.memWen

  io.pcPlus4_o := io.pcPlus4_i
  io.regWen_o  := decoder.io.regWen
  io.wbSel_o   := decoder.io.wbSel

  io.pcSel_o := decoder.io.pcSel

  io.npcTrap := decoder.io.npcTrap

  io.regs := regFile.io.regs

  io.csr_rdata_o := csrFile.io.csr_rdata_o
  io.csr_wen_o   := decoder.io.csr_wen
  io.csr_waddr_o := csr_addr

  io.csr_o := csrFile.io.csr_rdata_o

  when(decoder.io.csr_ctrl === CSRCtrlEnum.ecall) {
    // printf("ecall\n")
  }

}
