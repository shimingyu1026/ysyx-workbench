package myCPU

import chisel3._
import chisel3.util._

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
}

class MMU extends Module {
  val io = IO(new MMUIO)

  val raddr    = io.aluResult_i
  val waddr    = io.aluResult_i
  val rs2_data = io.rs2_data_i

  val wdata = MuxLookup(io.storeCtrl_i, 0.U)(
    List(
      StoreCtrlEnum.sb -> Cat(Fill(24, 0.U), rs2_data(7, 0)),
      StoreCtrlEnum.sh -> Cat(Fill(16, 0.U), rs2_data(15, 0)),
      StoreCtrlEnum.sw -> rs2_data
    )
  )

  val rdata = MuxLookup(io.loadCtrl_i, 0.U)(
    List(
      LoadCtrlEnum.lb  -> Cat(Fill(24, io.memRdata_i(7)), io.memRdata_i(7, 0)),
      LoadCtrlEnum.lbu -> Cat(Fill(24, 0.U), io.memRdata_i(7, 0)),
      LoadCtrlEnum.lhu -> Cat(Fill(16, 0.U), io.memRdata_i(15, 0)),
      LoadCtrlEnum.lh  -> Cat(Fill(16, io.memRdata_i(15)), io.memRdata_i(15, 0)),
      LoadCtrlEnum.lw  -> io.memRdata_i
    )
  )

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

}
