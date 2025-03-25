package myCPU

import chisel3._
import chisel3.util._

class coreIO extends Bundle {
  val pc_o   = Output(UInt(32.W))
  val inst_i = Input(UInt(32.W))

  val memRdata_i = Input(UInt(32.W))

  val memWdata_o = Output(UInt(32.W))
  val memRaddr_o = Output(UInt(32.W))
  val memWaddr_o = Output(UInt(32.W))
  val memWen_o   = Output(Bool())
  val mask_o     = Output(UInt(4.W))

  val npcTrap = Output(Bool())

  // for verilator
  val regs = Output(Vec(32, UInt(32.W)))
}

class core extends Module {
  val io  = IO(new coreIO)
  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val mmu = Module(new MMU)
  val wbu = Module(new WBU)

  ifu.io.pcSel_i     := exu.io.pcSel_o
  ifu.io.brSel_i     := exu.io.brSel_o
  ifu.io.pcBranchJ_i := exu.io.pcBranchJ_o
  ifu.io.inst_i      := io.inst_i
  ifu.io.csr_i       := idu.io.csr_o

  idu.io.pc_i        := ifu.io.pc_o
  idu.io.pcPlus4_i   := ifu.io.pcPlus4_o
  idu.io.inst_i      := ifu.io.inst_o
  idu.io.wbdata_i    := wbu.io.wbData_o
  idu.io.regWen_i    := wbu.io.regWen_o
  idu.io.csr_wen_i   := wbu.io.regWen_o
  idu.io.csr_waddr_i := wbu.io.csr_waddr_o
  idu.io.csr_wdata_i := wbu.io.csr_wdata_o

  exu.io.imm_i       := idu.io.imm_o
  exu.io.rs1_data_i  := idu.io.rs1_data_o
  exu.io.rs2_data_i  := idu.io.rs2_data_o
  exu.io.pc_i        := idu.io.pc_o
  exu.io.pcPlus4_i   := idu.io.pcPlus4_o
  exu.io.srcASel_i   := idu.io.srcASel_o
  exu.io.srcBSel_i   := idu.io.srcBSel_o
  exu.io.aluCtrl_i   := idu.io.aluCtrl_o
  exu.io.brType_i    := idu.io.brType_o
  exu.io.storeCtrl_i := idu.io.storeCtrl_o
  exu.io.loadCtrl_i  := idu.io.loadCtrl_o
  exu.io.memWen_i    := idu.io.memWen_o
  exu.io.regWen_i    := idu.io.regWen_o
  exu.io.wbSel_i     := idu.io.wbSel_o
  exu.io.pcSel_i     := idu.io.pcSel_o
  exu.io.csr_wen_i   := idu.io.csr_wen_o
  exu.io.csr_waddr_i := idu.io.csr_waddr_o
  exu.io.csr_rdata_i := idu.io.csr_rdata_o

  mmu.io.imm_i       := exu.io.imm_o
  mmu.io.aluResult_i := exu.io.aluResult_o
  mmu.io.pcPlus4_i   := exu.io.pcPlus4_o
  mmu.io.rs2_data_i  := exu.io.rs2_data_o
  mmu.io.storeCtrl_i := exu.io.storeCtrl_o
  mmu.io.loadCtrl_i  := exu.io.loadCtrl_o
  mmu.io.memWen_i    := exu.io.memWen_o
  mmu.io.regWen_i    := exu.io.regWen_o
  mmu.io.wbSel_i     := exu.io.wbSel_o
  mmu.io.memRdata_i  := io.memRdata_i
  mmu.io.csr_wen_i   := exu.io.csr_wen_o
  mmu.io.csr_waddr_i := exu.io.csr_waddr_o
  mmu.io.csr_wdata_i := exu.io.csr_wdata_o
  mmu.io.csr_rdata_i := exu.io.csr_rdata_o

  wbu.io.imm_i       := mmu.io.imm_o
  wbu.io.aluResult_i := mmu.io.aluResult_o
  wbu.io.pcPlus4_i   := mmu.io.pcPlus4_o
  wbu.io.mem_i       := mmu.io.mem_o
  wbu.io.regWen_i    := mmu.io.regWen_o
  wbu.io.wbSel_i     := mmu.io.wbSel_o
  wbu.io.csr_rdata_i := mmu.io.csr_rdata_o
  wbu.io.csr_waddr_i := mmu.io.csr_waddr_o
  wbu.io.csr_wdata_i := mmu.io.csr_wdata_o
  wbu.io.csr_wen_i   := mmu.io.csr_wen_o

  io.pc_o       := ifu.io.pc_o
  io.memWdata_o := mmu.io.memWdata_o
  io.memRaddr_o := mmu.io.memRaddr_o
  io.memWaddr_o := mmu.io.memWaddr_o
  io.memWen_o   := mmu.io.memWen_o
  io.mask_o     := mmu.io.mask_o

  io.npcTrap := idu.io.npcTrap

  io.regs := idu.io.regs

}
