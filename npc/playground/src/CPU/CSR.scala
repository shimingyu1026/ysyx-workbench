package myCPU

import chisel3._
import chisel3.util._

class csrFileIO extends Bundle {
  val csr_raddr_i = Input(UInt(12.W))
  val csr_rdata_o = Output(UInt(32.W))

  val csr_wdata_i = Input(UInt(32.W))
  val csr_waddr_i = Input(UInt(12.W))
  val csr_wen_i   = Input(Bool())

  val csr_ctrl_i = Input(CSRCtrlEnum())
  val pc_i       = Input(UInt(32.W))

  val mepc_o = Output(UInt(32.W))
}

class csrFile extends Module {
  val io = IO(new csrFileIO)

  val mepc      = RegInit(0.U(32.W))
  val mtvec     = RegInit(0.U(32.W))
  val mcause    = RegInit(0.U(32.W))
  val mstatus   = RegInit("h1800".U(32.W))
  val mvendorid = RegInit("h79737978".U(32.W))
  val marchid   = RegInit("h16f4771".U(32.W))

  when(io.csr_ctrl_i === CSRCtrlEnum.mret) {
    io.csr_rdata_o := mepc
  }.elsewhen(io.csr_ctrl_i === CSRCtrlEnum.ecall) {
    io.csr_rdata_o := mtvec
  }.otherwise {
    io.csr_rdata_o := MuxLookup(io.csr_raddr_i, 0.U)(
      List(
        "h341".U -> mepc,
        "h305".U -> mtvec,
        "h342".U -> mcause,
        "h300".U -> mstatus,
        "hf11".U -> mvendorid,
        "hf12".U -> marchid
      )
    )
  }

  // printf("mcause: %x\n", mcause)
  when(io.csr_ctrl_i === CSRCtrlEnum.ecall & io.csr_wen_i) {
    mepc   := io.pc_i
    mcause := "hb".U
  }.otherwise {
    when(io.csr_wen_i) {
      when(io.csr_waddr_i === "h341".U) {
        mepc := io.csr_wdata_i
      }.elsewhen(io.csr_waddr_i === "h305".U) {
        mtvec := io.csr_wdata_i
      }.elsewhen(io.csr_waddr_i === "h342".U) {
        mcause := io.csr_wdata_i
      }.elsewhen(io.csr_waddr_i === "h300".U) {
        mstatus := io.csr_wdata_i
      }
    }
  }

  io.mepc_o := mepc
}
