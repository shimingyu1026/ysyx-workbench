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
  val StoreCtrl_i = Input(StoreCtrlEnum())
  val LoadCtrl_i  = Input(LoadCtrlEnum())
  val memWen_i    = Input(MemWenEnum())

  // from exu
  val aluResult_i = Input(UInt(32.W))

  // to wbu
  val mem_o       = Output(UInt(32.W))
  val imm_o       = Output(UInt(32.W))
  val pcPlus4_o   = Output(UInt(32.W))
  val aluResult_o = Output(UInt(32.W))

//to idu
  val regWen_o = Output(Bool())
}

class MMU extends Module {
  val io = IO(new MMUIO)

  val raddr    = io.aluResult_i
  val waddr    = io.rs2_data_i
  val rs2_data = io.rs2_data_i

  val wdata = MuxLookup(io.StoreCtrl_i, 0.U)(
    List(
      StoreCtrlEnum.sb -> Cat(Fill(24, 0.U), rs2_data(7, 0)),
      StoreCtrlEnum.sh -> Cat(Fill(16, 0.U), rs2_data(15, 0)),
      StoreCtrlEnum.sw -> rs2_data
    )
  )

}
