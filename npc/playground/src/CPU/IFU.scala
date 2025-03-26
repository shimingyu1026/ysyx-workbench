package myCPU

import chisel3._
import chisel3.util._
object StateIFU extends ChiselEnum {
  val sIdle, sWaitReady = Value
}

object StateIFUAXI extends ChiselEnum {
  val sIdle, sWaitAR, sWaitR = Value
}

class IFUIO extends Bundle {

  // from idu
  val pcSel_i     = Input(PCSelEnum())
  val brSel_i     = Input(BrSelEnum())
  // from exu
  val pcBranchJ_i = Input(UInt(32.W))
  // from csr
  val csr_i       = Input(UInt(32.W))

  // axi master
  val axi    = (new axi_lite)
//from inst memory
  val inst_i = Input(UInt(32.W))

  val ifu_to_idu = Decoupled(new ifu_idu_io)
}

class IFU extends Module {
  val io       = IO(new IFUIO)
  // 状态机寄存器
  val state    = RegInit(StateIFU.sIdle)
  val stateAXI = RegInit(StateIFUAXI.sIdle)

//状态转移
  switch(stateAXI) {
    is(StateIFUAXI.sIdle) {
      when(state === StateIFU.sIdle) {
        stateAXI := StateIFUAXI.sWaitAR
      }
    }

    is(StateIFUAXI.sWaitAR) {
      when(io.axi.ar.ready) {
        stateAXI := StateIFUAXI.sWaitR
      }
    }

    is(StateIFUAXI.sWaitR) {
      when(io.axi.r.valid) {
        stateAXI := StateIFUAXI.sIdle
      }
    }
  }

  switch(state) {
    is(StateIFU.sIdle) {
      when(io.ifu_to_idu.valid) {
        state := StateIFU.sWaitReady
      }
    }
    is(StateIFU.sWaitReady) {
      when(io.ifu_to_idu.ready) {
        state := StateIFU.sIdle
      }
    }
  }
  val inst    = RegInit(0.U(32.W))
  val PC      = RegInit("h80000000".U(32.W))
  val pcPlus4 = PC + 4.U

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
  PC := PCNext
//--------------------------------------------------------------------------------------
//axi 信号
//写通道关闭
  io.axi.aw.addr  := 0.U
  io.axi.aw.valid := false.B

  io.axi.w.data  := 0.U
  io.axi.w.strb  := 0.U
  io.axi.w.valid := false.B

  io.axi.b.ready  := false.B
//读通道
  io.axi.ar.addr  := PC
  io.axi.ar.valid := stateAXI === StateIFUAXI.sWaitAR

  io.axi.r.ready      := stateAXI === StateIFUAXI.sWaitR // 可以是常1
//--------------------------------------------------------------------------------------
  inst                := Mux(
    (stateAXI === StateIFUAXI.sWaitR && io.axi.r.ready === true.B && io.axi.r.valid === true.B).asBool,
    io.axi.r.data,
    inst
  )
//--------------------------------------------------------------
  // 由instFetch影响
  io.ifu_to_idu.valid := true.B

  // 数据
  io.ifu_to_idu.bits.pc      := PC
  io.ifu_to_idu.bits.inst    := io.inst_i
  io.ifu_to_idu.bits.pcPlus4 := pcPlus4
  // ---------------------------------------------------------------
  // printf("io.pcBranchJ_i: %x\n", io.pcBranchJ_i)
  dontTouch(io)
}
