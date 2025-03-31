package myCPU

import chisel3._
import chisel3.util._
object StateIFU extends ChiselEnum {
  val sIdle, sWaitReady = Value
}

object StateIFUAXI extends ChiselEnum {
  val sIdle, sWaitAR, sWaitR, sNothing = Value
}

class IFUIO extends Bundle {

  // from idu
  val pcSel_i     = Input(PCSelEnum())
  val brSel_i     = Input(BrSelEnum())
  // from exu
  val pcBranchJ_i = Input(UInt(32.W))
  val pcUpdate    = Input(Bool())
  // from csr
  val csr_i       = Input(UInt(32.W))
//for sim
  val inst_o      = Output(UInt(32.W))

  val wbFlag = Input(Bool())

  // axi master
  val axi        = (new axi_full)
  val ifu_to_idu = Decoupled(new ifu_idu_io)
}

class IFU extends Module {
  val io = IO(new IFUIO)

  val resetValue = "h20000000".U(32.W)
  val inst       = RegInit(0.U(32.W))
  val PC         = RegInit(resetValue)
  val pcPlus4    = PC + 4.U
  // 状态机寄存器
  val state      = RegInit(StateIFU.sIdle)
  val stateAXI   = RegInit(StateIFUAXI.sIdle)
  switch(stateAXI) {
    is(StateIFUAXI.sIdle) {
      when(DEBUG.PRINTF) {
        printf("IFU AXI state: idle\n")
      }
      val flag = RegInit(true.B)

      when((state === StateIFU.sIdle & io.pcUpdate) | flag) {
        stateAXI := StateIFUAXI.sWaitAR
        flag     := false.B
      }
    }
    is(StateIFUAXI.sWaitAR) {
      when(DEBUG.PRINTF) {
        printf("IFU AXI state: wait ar\n")
      }

      when(io.axi.arready) {
        stateAXI := StateIFUAXI.sWaitR
      }
    }
    is(StateIFUAXI.sWaitR) {
      when(DEBUG.PRINTF) {
        printf("IFU AXI state: wait r\n")
      }

      when(io.axi.rvalid) {
        stateAXI := StateIFUAXI.sNothing
      }
    }
    is(StateIFUAXI.sNothing) {
      when(DEBUG.PRINTF) {
        printf("IFU AXI state: nothing\n")
      }

      when(io.ifu_to_idu.fire) // 握手成功
      {
        stateAXI := StateIFUAXI.sIdle
      }
    }
  }

  switch(state) {
    is(StateIFU.sIdle) {
      when(DEBUG.PRINTF) { printf("IFU state: idle\n") }

      when(io.ifu_to_idu.valid & ~(io.ifu_to_idu.ready)) {
        state := StateIFU.sWaitReady
      }.elsewhen(io.ifu_to_idu.fire) {
        state := StateIFU.sIdle

      }
    }
    is(StateIFU.sWaitReady) {
      when(DEBUG.PRINTF) {
        printf("IFU state: wait ready\n")
      }

      when(io.ifu_to_idu.fire) {
        state := StateIFU.sIdle
      }
    }
  }

  val jalPC  = io.pcBranchJ_i
  val jalrPC = io.pcBranchJ_i & "hfffffffe".U

  val branchPC = io.pcBranchJ_i
  val brPC     = MuxLookup(io.brSel_i, pcPlus4)(
    List(
      BrSelEnum.pcplus4 -> pcPlus4,
      BrSelEnum.alu     -> branchPC
    )
  )

  val PCNext = MuxLookup(io.pcSel_i, pcPlus4)(
    List(
      PCSelEnum.pcplus4 -> pcPlus4,
      PCSelEnum.jal     -> jalPC,
      PCSelEnum.jalr    -> jalrPC,
      PCSelEnum.branch  -> brPC,
      PCSelEnum.csr     -> io.csr_i,
      PCSelEnum.none    -> PC
    )
  )
  when(DEBUG.PRINTF) {
    printf("pc next: %x\n", PCNext)
    printf("inst: %x\n", inst)
    printf("rdata: %x\n", io.axi.rdata)
  }

  when(io.pcUpdate) {
    PC := PCNext
  }.otherwise {
    PC := PC
  }

//--------------------------------------------------------------------------------------
//axi 信号
//写通道关闭
  io.axi.awaddr  := 0.U
  io.axi.awvalid := false.B
  io.axi.awid    := 1.U // ifu
  io.axi.awlen   := 0.U
  io.axi.awsize  := 0.U
  io.axi.awburst := 0.U

  io.axi.wdata  := 0.U
  io.axi.wstrb  := 0.U
  io.axi.wvalid := false.B
  io.axi.wlast  := false.B

  io.axi.bready := false.B

//读通道
  io.axi.araddr       := PC
  io.axi.arvalid      := stateAXI === StateIFUAXI.sWaitAR
  io.axi.arid         := 1.U
  io.axi.rready       := stateAXI === StateIFUAXI.sWaitR // 可以是常1
  io.axi.arlen        := 0.U
  io.axi.arsize       := "b10".U
  io.axi.arburst      := "b00".U
//--------------------------------------------------------------------------------------
  // axi 信号影响
  // printf("IFU io.axi.rdata: %x\n", io.axi.rdata)
  // printf("IFU io.axi.rvalid: %x\n", io.axi.rvalid)
  // printf("IFU io.axi.rready: %x\n", io.axi.rready)
  when(stateAXI === StateIFUAXI.sWaitR & io.axi.rvalid & io.axi.rready) {
    inst := io.axi.rdata
  }.otherwise {
    inst := inst
  }
  io.ifu_to_idu.valid := stateAXI === StateIFUAXI.sNothing
//--------------------------------------------------------------

  // 数据
  io.ifu_to_idu.bits.pc      := PC
  io.ifu_to_idu.bits.inst    := inst
  io.ifu_to_idu.bits.pcPlus4 := pcPlus4
  // ---------------------------------------------------------------
  io.inst_o                  := inst

  val traceDiff = Module(new traceDiff)

  traceDiff.io.clock := clock
  traceDiff.io.call  := io.pcUpdate
}

class traceDiff extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val call  = Input(Bool())
  })
  dontTouch(io)
}
