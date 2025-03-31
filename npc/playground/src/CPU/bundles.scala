package myCPU

import chisel3._
import chisel3.util._

class ifu_idu_io extends Bundle {

  val inst    = Output(UInt(32.W))
  val pcPlus4 = Output(UInt(32.W))
  val pc      = Output(UInt(32.W))
}

class idu_to_exu_io extends Bundle {
  val pc       = Output(UInt(32.W))
  val imm      = Output(UInt(32.W)) // also to wbu
  val rs1_data = Output(UInt(32.W))
  val rs2_data = Output(UInt(32.W))

  val srcASel = Output(SrcASelEnum())
  val srcBSel = Output(SrcBSelEnum())
  val aluCtrl = Output(AluCtrlEnum())
  val brType  = Output(BrTypeEnum())

  // to mmu
  val memValid  = Output(Bool())
  val storeCtrl = Output(StoreCtrlEnum())
  val loadCtrl  = Output(LoadCtrlEnum())
  val memWen    = Output(MemWenEnum())

  // to wbu and go back to idu
  val regWen = Output(Bool())
  val wbSel  = Output(WbSelEnum())

  // to ifu
  val pcSel = Output(PCSelEnum())

  // csr
  val csr_rdata = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))
  val csr_wen   = Output(Bool())

  // to wbu
  val pcPlus4 = Output(UInt(32.W))
}

class exu_to_mmu_io extends Bundle {
  // to wbu
  val pcPlus4 = Output(UInt(32.W))
  val wbSel   = Output(WbSelEnum())
  // to idu
  val regWen  = Output(Bool())

  // to mmu
  val memValid  = Output(Bool())
  val memWen    = Output(MemWenEnum())
  val loadCtrl  = Output(LoadCtrlEnum())
  val storeCtrl = Output(StoreCtrlEnum())
  val imm       = Output(UInt(32.W))
  val rs2_data  = Output(UInt(32.W))
  val aluResult = Output(UInt(32.W))

  // csr
  val csr_wdata = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))
  val csr_wen   = Output(Bool())
  val csr_rdata = Output(UInt(32.W))

}

class mmu_to_wbu_io extends Bundle {
  // to wbu
  val mem       = Output(UInt(32.W))
  val imm       = Output(UInt(32.W))
  val pcPlus4   = Output(UInt(32.W))
  val aluResult = Output(UInt(32.W))
  val wbSel     = Output(WbSelEnum())
  val regWen    = Output(Bool())
  // csr
  val csr_wdata = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))
  val csr_wen   = Output(Bool())
  val csr_rdata = Output(UInt(32.W))
}

class axi_lite extends Bundle {
  val ar = new Bundle {
    val addr  = Output(UInt(32.W))
    val valid = Output(Bool())
    val ready = Input(Bool())
  } // 读地址

  val r = new Bundle {
    val data  = Input(UInt(32.W))
    val valid = Input(Bool())
    val ready = Output(Bool())
    val resp  = Input(UInt(2.W))
  } // 读数据

  val aw = new Bundle {
    val addr  = Output(UInt(32.W))
    val valid = Output(Bool())
    val ready = Input(Bool())
  } // 写地址

  val w = new Bundle {
    val data  = Output(UInt(32.W))
    val valid = Output(Bool())
    val ready = Input(Bool())
    val strb  = Output(UInt(4.W))
  } // 写数据

  val b = new Bundle {
    val valid = Input(Bool())
    val ready = Output(Bool())
    val resp  = Input(UInt(2.W))
  }
}

class axi_full extends Bundle {
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awaddr  = Output(UInt(32.W))
  // 指定id
  val awid    = Output(UInt(4.W))
//一次读写长度 默认 00 Length = AxLEN + 1.
  val awlen   = Output(UInt(8.W))
  // 默认 8
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  val wready = Input(Bool())
  val wvalid = Output(Bool())
  val wdata  = Output(UInt(32.W))
  val wstrb  = Output(UInt(4.W))
  val wlast  = Output(Bool())

  val bready = Output(Bool())
  val bvalid = Input(Bool())
  val bresp  = Input(UInt(2.W))
  val bid    = Input(UInt(4.W))

  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(32.W))
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))

  val rready = Output(Bool())
  val rvalid = Input(Bool())
  val rresp  = Input(UInt(2.W))
  val rdata  = Input(UInt(32.W))
  val rlast  = Input(Bool())
  val rid    = Input(UInt(4.W))

}
