package myCPU

import chisel3._
import chisel3.util._
import scala.annotation.switch

class MMUIO extends Bundle {
  // from ifu
  val pcPlus4_i = Input(UInt(32.W))

  // from idu
  val rs2_data_i = Input(UInt(32.W))
  val imm_i      = Input(UInt(32.W))

  val regWen_i    = Input(Bool())
  val storeCtrl_i = Input(StoreCtrlEnum())
  val loadCtrl_i  = Input(LoadCtrlEnum())
  val memWen_i    = Input(MemWenEnum())
  val wbSel_i     = Input(WbSelEnum())

  // from memory
  val memRdata_i = Input(UInt(32.W))

  // csr
  val csr_wen_i   = Input(Bool())
  val csr_waddr_i = Input(UInt(12.W))
  val csr_wdata_i = Input(UInt(32.W))
  val csr_rdata_i = Input(UInt(32.W))

  // from exu
  val aluResult_i = Input(UInt(32.W))

  // to wbu
  val mem_o       = Output(UInt(32.W))
  val imm_o       = Output(UInt(32.W))
  val pcPlus4_o   = Output(UInt(32.W))
  val aluResult_o = Output(UInt(32.W))
  val wbSel_o     = Output(WbSelEnum())

//to idu
  val regWen_o = Output(Bool())

  // to memory
  val memWdata_o = Output(UInt(32.W))
  val memRaddr_o = Output(UInt(32.W))
  val memWaddr_o = Output(UInt(32.W))
  val memWen_o   = Output(Bool())
  val mask_o     = Output(UInt(4.W))

  // csr
  val csr_wdata_o = Output(UInt(32.W))
  val csr_waddr_o = Output(UInt(12.W))
  val csr_wen_o   = Output(Bool())
  val csr_rdata_o = Output(UInt(32.W))
}

class MMU extends Module {
  val io = IO(new MMUIO)

  val raddr    = io.aluResult_i
  val waddr    = io.aluResult_i
  val rs2_data = io.rs2_data_i

  val wdata = Wire(UInt(32.W))
  val mask  = Wire(UInt(4.W))
  mask  := 0.U
  wdata := 0.U
  switch(io.storeCtrl_i) {

    is(StoreCtrlEnum.sb) {
      // printf("wdata: %x\n", rs2_data)
      wdata := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(24, 0.U), rs2_data(7, 0)),
          1.U -> Cat(Fill(16, 0.U), rs2_data(7, 0), Fill(8, 0.U)),
          2.U -> Cat(Fill(8, 0.U), rs2_data(7, 0), Fill(16, 0.U)),
          3.U -> Cat(rs2_data(7, 0), Fill(24, 0.U))
        )
      )
      mask  := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> "b0001".U,
          1.U -> "b0010".U,
          2.U -> "b0100".U,
          3.U -> "b1000".U
        )
      )
    }
    is(StoreCtrlEnum.sh) {
      wdata := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(16, 0.U), rs2_data(15, 0)),
          1.U -> Cat(Fill(8, 0.U), rs2_data(15, 0), Fill(8, 0.U)),
          2.U -> Cat(rs2_data(15, 0), Fill(16, 0.U))
        )
      )
      mask  := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> "b0011".U,
          1.U -> "b0110".U,
          2.U -> "b1100".U
        )
      )
    }
    is(StoreCtrlEnum.sw) {
      wdata := rs2_data
      mask  := "b1111".U
    }
  }

  val rdata = Wire(UInt(32.W))
  rdata := 0.U

  switch(io.loadCtrl_i) {
    is(LoadCtrlEnum.lb) {
      rdata := MuxLookup(raddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(24, io.memRdata_i(7)), io.memRdata_i(7, 0)),
          1.U -> Cat(Fill(24, io.memRdata_i(15)), io.memRdata_i(15, 8)),
          2.U -> Cat(Fill(24, io.memRdata_i(23)), io.memRdata_i(23, 16)),
          3.U -> Cat(Fill(24, io.memRdata_i(31)), io.memRdata_i(31, 24))
        )
      )
    }

    is(LoadCtrlEnum.lbu) {
      rdata := MuxLookup(raddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(24, 0.U), io.memRdata_i(7, 0)),
          1.U -> Cat(Fill(24, 0.U), io.memRdata_i(15, 8)),
          2.U -> Cat(Fill(24, 0.U), io.memRdata_i(23, 16)),
          3.U -> Cat(Fill(24, 0.U), io.memRdata_i(31, 24))
        )
      )
    }

    is(LoadCtrlEnum.lhu) {
      rdata := MuxLookup(raddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(16, 0.U), io.memRdata_i(15, 0)),
          1.U -> Cat(Fill(16, 0.U), io.memRdata_i(23, 8)),
          2.U -> Cat(Fill(16, 0.U), io.memRdata_i(31, 16))
        )
      )
    }

    is(LoadCtrlEnum.lh) {
      rdata := MuxLookup(raddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(16, io.memRdata_i(15)), io.memRdata_i(15, 0)),
          1.U -> Cat(Fill(16, io.memRdata_i(23)), io.memRdata_i(23, 8)),
          2.U -> Cat(Fill(16, io.memRdata_i(31)), io.memRdata_i(31, 16))
        )
      )
    }

    is(LoadCtrlEnum.lw) {
      rdata := io.memRdata_i
    }
  }

  io.mem_o       := rdata
  io.imm_o       := io.imm_i
  io.pcPlus4_o   := io.pcPlus4_i
  io.aluResult_o := io.aluResult_i
  io.regWen_o    := io.regWen_i

  io.memWdata_o := wdata
  io.memRaddr_o := raddr
  io.memWaddr_o := waddr
  io.memWen_o   := MuxLookup(io.memWen_i, 0.U)(
    List(
      MemWenEnum.wen  -> 1.U,
      MemWenEnum.none -> 0.U
    )
  )

  io.wbSel_o := io.wbSel_i
  io.mask_o  := mask

  io.csr_wdata_o := io.csr_wdata_i
  io.csr_waddr_o := io.csr_waddr_i
  io.csr_wen_o   := io.csr_wen_i
  io.csr_rdata_o := io.csr_rdata_i

  dontTouch(io.mask_o)
}
