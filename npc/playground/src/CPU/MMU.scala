package myCPU

import chisel3._
import chisel3.util._
import scala.annotation.switch
object StateMMU    extends ChiselEnum {
  val sIdle, sWaitValid, sWaitReady = Value
}
object StateMMUAXI extends ChiselEnum {
  val sIdle, sWaitAR, sWaitR, sWaitAW, sWaitW, sWaitB, sNothing = Value
  //  000     001       010     011      100    101      110
}
class MMUIO        extends Bundle     {
  val exu_to_mmu = Flipped(Decoupled(new exu_to_mmu_io))
  val mmu_to_wbu = Decoupled(new mmu_to_wbu_io)
  val axi        = new axi_full()
}

class MMU extends Module {
  val io = IO(new MMUIO)

  io.exu_to_mmu.ready := true.B
  io.mmu_to_wbu.valid := 0.U

  val state    = RegInit(StateMMU.sIdle)
  val stateAXI = RegInit(StateMMUAXI.sIdle)

  switch(stateAXI) {
    is(StateMMUAXI.sIdle) {
      when(DEBUG.PRINTF) {
        printf("mmu AXI state: sIdle\n")
      }

      when(state === StateMMU.sWaitValid & io.exu_to_mmu.fire) {
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
      when(DEBUG.PRINTF) {
        printf("mmu AXI state: sWaitAW\n")
      }

      when(io.axi.awready) {
        stateAXI := StateMMUAXI.sWaitW
      }
    }

    is(StateMMUAXI.sWaitW) {
      when(DEBUG.PRINTF) {
        printf("mmu AXI state: sWaitW\n")
      }

      when(io.axi.wready) {
        stateAXI := StateMMUAXI.sWaitB
      }
    }

    is(StateMMUAXI.sWaitB) {
      when(DEBUG.PRINTF) {

        printf("mmu AXI state: sWaitB\n")
      }
      when(io.axi.bvalid) {
        stateAXI := StateMMUAXI.sNothing
      }
    }

    is(StateMMUAXI.sWaitAR) {
      when(DEBUG.PRINTF) {
        printf("mmu AXI state: sWaitAR\n")
      }

      when(io.axi.arready) {
        stateAXI := StateMMUAXI.sWaitR
      }
    }

    is(StateMMUAXI.sWaitR) {
      when(DEBUG.PRINTF) {
        printf("mmu AXI state: sWaitR\n")

      }

      when(io.axi.rvalid) {
        stateAXI := StateMMUAXI.sNothing
      }
    }

    is(StateMMUAXI.sNothing) {
      when(DEBUG.PRINTF) { printf("mmu AXI state: sNothing\n") }

      when(io.mmu_to_wbu.ready & io.mmu_to_wbu.ready) {
        stateAXI := StateMMUAXI.sIdle
      }
    }
  }
  switch(state) {
    is(StateMMU.sIdle) {
      when(DEBUG.PRINTF) {
        printf("mmu state: sIdle\n")
      }

      when(io.exu_to_mmu.ready) {
        state := StateMMU.sWaitValid
      }
    }
    is(StateMMU.sWaitValid) {
      when(DEBUG.PRINTF) {
        printf("mmu state: sWaitValid\n")
      }

      when(io.exu_to_mmu.valid) {
        state := StateMMU.sWaitReady
      }
    }
    is(StateMMU.sWaitReady) {
      when(DEBUG.PRINTF) {
        printf("mmu state: sWaitReady\n")
      }

      when(io.mmu_to_wbu.fire) {
        state := StateMMU.sIdle
      }
    }
  }
  // printf("io.axi.rready: %d\nio.axi.rvalid: %d\n", io.axi.rready, io.axi.rvalid)
  val raddr     = io.exu_to_mmu.bits.aluResult
  val waddr     = io.exu_to_mmu.bits.aluResult
  val rs2_data  = io.exu_to_mmu.bits.rs2_data
  val axi_rdata = RegInit(0.U(32.W))
  val wdata     = Wire(UInt(32.W))
  val mask      = Wire(UInt(4.W))
  val awsize    = Wire(UInt(3.W))
  mask   := 0.U
  wdata  := 0.U
  awsize := 0.U
  switch(io.exu_to_mmu.bits.storeCtrl) {

    is(StoreCtrlEnum.sb) {

      wdata  := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(24, 0.U), rs2_data(7, 0)),
          1.U -> Cat(Fill(16, 0.U), rs2_data(7, 0), Fill(8, 0.U)),
          2.U -> Cat(Fill(8, 0.U), rs2_data(7, 0), Fill(16, 0.U)),
          3.U -> Cat(rs2_data(7, 0), Fill(24, 0.U))
        )
      )
      mask   := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> "b0001".U,
          1.U -> "b0010".U,
          2.U -> "b0100".U,
          3.U -> "b1000".U
        )
      )
      awsize := "b000".U
    }
    is(StoreCtrlEnum.sh) {
      wdata  := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(16, 0.U), rs2_data(15, 0)),
          1.U -> Cat(Fill(8, 0.U), rs2_data(15, 0), Fill(8, 0.U)),
          2.U -> Cat(rs2_data(15, 0), Fill(16, 0.U))
        )
      )
      mask   := MuxLookup(waddr(1, 0), 0.U)(
        List(
          0.U -> "b0011".U,
          1.U -> "b0110".U,
          2.U -> "b1100".U
        )
      )
      awsize := "b001".U
    }
    is(StoreCtrlEnum.sw) {
      wdata  := rs2_data
      mask   := "b1111".U
      awsize := "b010".U
    }
  }

  val rdata  = Wire(UInt(32.W))
  val arsize = Wire(UInt(3.W))
  rdata          := 0.U
  arsize         := 0.U
  switch(io.exu_to_mmu.bits.loadCtrl) {
    is(LoadCtrlEnum.lb) {
      rdata  :=
        MuxLookup(raddr(1, 0), 0.U)(
          List(
            0.U -> Cat(Fill(24, axi_rdata(7)), axi_rdata(7, 0)),
            1.U -> Cat(Fill(24, axi_rdata(15)), axi_rdata(15, 8)),
            2.U -> Cat(Fill(24, axi_rdata(23)), axi_rdata(23, 16)),
            3.U -> Cat(Fill(24, axi_rdata(31)), axi_rdata(31, 24))
          )
        )
      arsize := "b000".U
    }

    is(LoadCtrlEnum.lbu) {
      rdata  := MuxLookup(raddr(1, 0), 0.U)(
        List(
          0.U -> Cat(Fill(24, 0.U), axi_rdata(7, 0)),
          1.U -> Cat(Fill(24, 0.U), axi_rdata(15, 8)),
          2.U -> Cat(Fill(24, 0.U), axi_rdata(23, 16)),
          3.U -> Cat(Fill(24, 0.U), axi_rdata(31, 24))
        )
      )
      arsize := "b000".U
    }

    is(LoadCtrlEnum.lhu) {
      rdata  :=
        MuxLookup(raddr(1, 0), 0.U)(
          List(
            0.U -> Cat(Fill(16, 0.U), axi_rdata(15, 0)),
            1.U -> Cat(Fill(16, 0.U), axi_rdata(23, 8)),
            2.U -> Cat(Fill(16, 0.U), axi_rdata(31, 16))
          )
        )
      arsize := "b001".U
    }

    is(LoadCtrlEnum.lh) {
      rdata  :=
        MuxLookup(raddr(1, 0), 0.U)(
          List(
            0.U -> Cat(Fill(16, axi_rdata(15)), axi_rdata(15, 0)),
            1.U -> Cat(Fill(16, axi_rdata(23)), axi_rdata(23, 8)),
            2.U -> Cat(Fill(16, axi_rdata(31)), axi_rdata(31, 16))
          )
        )
      arsize := "b001".U
    }

    is(LoadCtrlEnum.lw) {
      rdata  := axi_rdata
      arsize := "b010".U
    }
  }
//--------------------------------------------------------------------------
// axi 信号
  io.axi.araddr  := raddr
  io.axi.arvalid := stateAXI === StateMMUAXI.sWaitAR
  io.axi.arid    := 3.U
  io.axi.arlen   := 0.U
  io.axi.arburst := 0.U
  io.axi.arsize  := arsize

  io.axi.rready := stateAXI === StateMMUAXI.sWaitR

  io.axi.awaddr  := waddr
  io.axi.awvalid := stateAXI === StateMMUAXI.sWaitAW
  io.axi.awid    := 4.U
  io.axi.awlen   := 0.U
  io.axi.awburst := 0.U
  io.axi.awsize  := awsize

  io.axi.wdata  := wdata // TODO
  io.axi.wstrb  := mask  // TODO
  io.axi.wvalid := stateAXI === StateMMUAXI.sWaitW
  io.axi.wlast  := 1.U

  io.axi.bready                := stateAXI === StateMMUAXI.sWaitB
//------------------------------------------------------------------------------------
// axi 影响的信号
  io.mmu_to_wbu.valid          := stateAXI === StateMMUAXI.sNothing
  when(stateAXI === StateMMUAXI.sWaitR & io.axi.rvalid & io.axi.rready) {
    axi_rdata := io.axi.rdata
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
  dontTouch(io.axi)

  val skip_diff = Module(new skip_diff)
  skip_diff.io.addr  := raddr
  skip_diff.io.fire  := io.mmu_to_wbu.fire
  skip_diff.io.valid := io.exu_to_mmu.bits.memValid
}

class skip_diff extends BlackBox {
  val io = IO(new Bundle {
    val addr  = Input(UInt(32.W))
    val fire  = Input(Bool())
    val valid = Input(Bool())
  })
  dontTouch(io)
}
