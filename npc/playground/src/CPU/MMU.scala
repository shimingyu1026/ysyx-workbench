package myCPU

import chisel3._
import chisel3.util._
import scala.annotation.switch
object StateMMU extends ChiselEnum {
  val sIdle, sWaitValid, sWaitReady = Value
}
class MMUIO     extends Bundle     {
  val exu_to_mmu = Flipped(Decoupled(new exu_to_mmu_io))
  val mmu_to_wbu = Decoupled(new mmu_to_wbu_io)

  // from memory
  val memRdata_i = Input(UInt(32.W))

  // to memory
  val memWdata_o = Output(UInt(32.W))
  val memRaddr_o = Output(UInt(32.W))
  val memWaddr_o = Output(UInt(32.W))
  val memWen_o   = Output(Bool())
  val mask_o     = Output(UInt(4.W))

}

class MMU extends Module {
  import StateMMU._
  val io = IO(new MMUIO)

  io.exu_to_mmu.ready := 0.U
  io.mmu_to_wbu.valid := 0.U

  val state = RegInit(sIdle)
  switch(state) {
    is(sIdle) {
      when(io.exu_to_mmu.ready) {
        state := sWaitValid
      }
    }
    is(sWaitValid) {
      when(io.exu_to_mmu.valid) {
        state := sWaitReady
      }
    }
    is(sWaitReady) {
      when(io.mmu_to_wbu.ready) {
        state := sIdle
      }
    }
  }

  val raddr    = io.exu_to_mmu.bits.aluResult
  val waddr    = io.exu_to_mmu.bits.aluResult
  val rs2_data = io.exu_to_mmu.bits.rs2_data

  val wdata = Wire(UInt(32.W))
  val mask  = Wire(UInt(4.W))
  mask  := 0.U
  wdata := 0.U
  switch(io.exu_to_mmu.bits.storeCtrl) {

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

  switch(io.exu_to_mmu.bits.loadCtrl) {
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

// -----------------------------------------------------------------------------------
  io.mmu_to_wbu.bits.mem       := rdata
  io.mmu_to_wbu.bits.imm       := io.exu_to_mmu.bits.imm
  io.mmu_to_wbu.bits.pcPlus4   := io.exu_to_mmu.bits.pcPlus4
  io.mmu_to_wbu.bits.aluResult := io.exu_to_mmu.bits.aluResult
  io.mmu_to_wbu.bits.regWen    := io.exu_to_mmu.bits.regWen
  io.mmu_to_wbu.bits.wbSel     := io.exu_to_mmu.bits.wbSel

  io.mmu_to_wbu.bits.csr_wdata := io.exu_to_mmu.bits.csr_wdata
  io.mmu_to_wbu.bits.csr_waddr := io.exu_to_mmu.bits.csr_waddr
  io.mmu_to_wbu.bits.csr_wen   := io.exu_to_mmu.bits.csr_wen
  io.mmu_to_wbu.bits.csr_rdata := io.exu_to_mmu.bits.csr_rdata

//-----------------------------------------------------------------------------------

  io.memWdata_o := wdata
  io.memRaddr_o := raddr
  io.memWaddr_o := waddr
  io.memWen_o   := MuxLookup(io.exu_to_mmu.bits.memWen, 0.U)(
    List(
      MemWenEnum.wen  -> 1.U,
      MemWenEnum.none -> 0.U
    )
  )
  io.mask_o     := mask

}
