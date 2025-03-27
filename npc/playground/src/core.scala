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

  ifu.io.ifu_to_idu <> idu.io.ifu_to_idu
  idu.io.idu_to_exu <> exu.io.idu_to_exu
  exu.io.exu_to_mmu <> mmu.io.exu_to_mmu
  mmu.io.mmu_to_wbu <> wbu.io.mmu_to_wbu

  ifu.io.pcSel_i     := exu.io.pcSel_o
  ifu.io.brSel_i     := exu.io.brSel_o
  ifu.io.pcBranchJ_i := exu.io.pcBranchJ_o
  ifu.io.inst_i      := io.inst_i
  ifu.io.csr_i       := idu.io.csr_o
  ifu.io.pcUpdate    := exu.io.pcUpdate

  idu.io.wbdata_i    := wbu.io.wbData_o
  idu.io.regWen_i    := wbu.io.regWen_o
  idu.io.csr_wen_i   := wbu.io.regWen_o
  idu.io.csr_waddr_i := wbu.io.csr_waddr_o
  idu.io.csr_wdata_i := wbu.io.csr_wdata_o

  mmu.io.memRdata_i := io.memRdata_i

  io.pc_o       := ifu.io.ifu_to_idu.bits.pc
  io.memWdata_o := mmu.io.memWdata_o
  io.memRaddr_o := mmu.io.memRaddr_o
  io.memWaddr_o := mmu.io.memWaddr_o
  io.memWen_o   := mmu.io.memWen_o
  io.mask_o     := mmu.io.mask_o

  io.npcTrap := idu.io.npcTrap

  io.regs := idu.io.regs

}
