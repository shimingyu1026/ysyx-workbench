package myCPU

import chisel3._
import chisel3.util._

class coreIO extends Bundle {

  val axi_1  = new axi_full()
  val axi_2  = new axi_full()
  val pc_o   = Output(UInt(32.W))
  val inst_o = Output(UInt(32.W))

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

  ifu.io.ifu_to_idu <> idu.io.ifu_to_idu
  idu.io.idu_to_exu <> exu.io.idu_to_exu
  exu.io.exu_to_mmu <> mmu.io.exu_to_mmu
  mmu.io.mmu_to_wbu <> wbu.io.mmu_to_wbu

  ifu.io.pcSel_i     := exu.io.pcSel_o
  ifu.io.brSel_i     := exu.io.brSel_o
  ifu.io.pcBranchJ_i := exu.io.pcBranchJ_o
  ifu.io.csr_i       := idu.io.csr_o
  ifu.io.pcUpdate    := wbu.io.pcUpdate
  ifu.io.wbFlag      := wbu.io.wbFlag

  idu.io.wbdata_i    := wbu.io.wbData_o
  idu.io.regWen_i    := wbu.io.regWen_o
  idu.io.csr_wen_i   := wbu.io.csr_wen_o
  idu.io.csr_waddr_i := wbu.io.csr_waddr_o
  idu.io.csr_wdata_i := wbu.io.csr_wdata_o

  io.pc_o   := ifu.io.ifu_to_idu.bits.pc
  io.inst_o := ifu.io.inst_o

  io.npcTrap := idu.io.npcTrap

  io.regs := idu.io.regs

  io.axi_1 <> ifu.io.axi
  io.axi_2 <> mmu.io.axi

  dontTouch(io.regs)
  dontTouch(io)

}
