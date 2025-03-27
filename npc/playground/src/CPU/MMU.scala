package myCPU

import chisel3._
import chisel3.util._
import scala.annotation.switch
object StateMMU    extends ChiselEnum {
  val sIdle, sWaitValid, sWaitReady = Value
}
object StateMMUAXI extends ChiselEnum {
  val sIdle, sWaitAR, sWaitR, sWaitAW, sWaitW, sWaitB, sNothing = Value
}
class MMUIO        extends Bundle     {
  val exu_to_mmu = Flipped(Decoupled(new exu_to_mmu_io))
  val mmu_to_wbu = Decoupled(new mmu_to_wbu_io)
  val axi        = new axi_lite()

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
  val io = IO(new MMUIO)

  io.exu_to_mmu.ready := true.B
  io.mmu_to_wbu.valid := 0.U

  val state    = RegInit(StateMMU.sIdle)
  val stateAXI = RegInit(StateMMUAXI.sIdle)

  switch(stateAXI) {
    is(StateMMUAXI.sIdle) {
      when(state === StateMMU.sWaitReady & io.exu_to_mmu.valid) {
        when(io.exu_to_mmu.bits.memValid === 0.U) {
          stateAXI := StateMMUAXI.sNothing
        }.elsewhen(io.exu_to_mmu.bits.memValid === 1.U & (io.exu_to_mmu.bits.memWen === MemWenEnum.none)) {
          stateAXI := StateMMUAXI.sWaitAR
        }.elsewhen(io.exu_to_mmu.bits.memValid === 1.U & (io.exu_to_mmu.bits.memWen === MemWenEnum.wen)) {
          stateAXI := StateMMUAXI.sWaitAW
        }
      }
    }

    is(StateMMUAXI.sWaitAW) {
      when(io.axi.aw.ready) {
        stateAXI := StateMMUAXI.sWaitW
      }
    }

    is(StateMMUAXI.sWaitW) {
      when(io.axi.w.ready) {
        stateAXI := StateMMUAXI.sWaitB
      }
    }

    is(StateMMUAXI.sWaitB) {
      when(io.axi.b.valid) {
        stateAXI := StateMMUAXI.sNothing
      }
    }

    is(StateMMUAXI.sWaitAR) {
      when(io.axi.ar.ready) {
        stateAXI := StateMMUAXI.sWaitR
      }
    }

    is(StateMMUAXI.sWaitR) {
      when(io.axi.r.valid) {
        stateAXI := StateMMUAXI.sNothing
      }
    }

    is(StateMMUAXI.sNothing) {
      when(io.mmu_to_wbu.ready & io.mmu_to_wbu.ready) {
        stateAXI := StateMMUAXI.sIdle
      }
    }
  }
  switch(state) {
    is(StateMMU.sIdle) {
      when(io.exu_to_mmu.ready) {
        state := StateMMU.sWaitValid
      }
    }
    is(StateMMU.sWaitValid) {
      when(io.exu_to_mmu.valid) {
        state := StateMMU.sWaitReady
      }
    }
    is(StateMMU.sWaitReady) {
      when(io.mmu_to_wbu.ready & io.mmu_to_wbu.valid) {
        state := StateMMU.sIdle
      }
    }
  }

  val raddr    = io.exu_to_mmu.bits.aluResult
  val waddr    = io.exu_to_mmu.bits.aluResult
  val rs2_data = io.exu_to_mmu.bits.rs2_data
  val axi_addr = RegInit(0.U(32.W))
  axi_addr := io.exu_to_mmu.bits.aluResult
  val axi_wdata = RegInit(0.U(32.W))
  val axi_rdata = RegInit(0.U(32.W))
  val axi_strb  = RegInit(0.U(4.W))
  val wdata     = Wire(UInt(32.W))
  val mask      = Wire(UInt(4.W))
  axi_wdata := wdata
  axi_strb  := mask
  mask      := 0.U
  wdata     := 0.U
  switch(io.exu_to_mmu.bits.storeCtrl) {

    is(StoreCtrlEnum.sb) {
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
//--------------------------------------------------------------------------
// axi 信号
  io.axi.ar.addr  := axi_addr
  io.axi.ar.valid := stateAXI === StateMMUAXI.sWaitAR

  io.axi.r.ready := stateAXI === StateMMUAXI.sWaitR

  io.axi.aw.addr  := axi_addr
  io.axi.aw.valid := stateAXI === StateMMUAXI.sWaitAW

  io.axi.w.data  := axi_wdata // TODO
  io.axi.w.strb  := axi_strb  // TODO
  io.axi.w.valid := stateAXI === StateMMUAXI.sWaitW

  io.axi.b.ready               := stateAXI === StateMMUAXI.sWaitB
//------------------------------------------------------------------------------------
// axi 影响的信号
  io.mmu_to_wbu.valid          := stateAXI === StateMMUAXI.sNothing
  when(stateAXI === StateMMUAXI.sWaitR & io.axi.r.valid & io.axi.r.ready) {
    axi_rdata := io.axi.r.data
  }.otherwise {
    axi_rdata := axi_rdata
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
