package myCPU

import chisel3._
import chisel3.util._

class stateSRAM extends ChiselEnum {
  val sIdle, sWaitAW, sWaitW, sWaitB, sWaitAR, sWaitR, sNothing = Value
}

class sramIO extends Bundle {
  val axi = Flipped(new axi_lite)
}

class dataMem extends BlackBox {
  val io = IO(new Bundle {
    val clock      = Input(Clock())
    val memWdata_i = Input(UInt(32.W))
    val memRaddr_i = Input(UInt(32.W))
    val memWaddr_i = Input(UInt(32.W))
    val memWen_i   = Input(Bool())
    val mask_i     = Input(UInt(4.W))
    // val valid_i = Input(Bool())
    val memRdata_o = Output(UInt(32.W))
  })
  dontTouch(io)
}
class memory  extends BlackBox {
  val io = IO(new Bundle {
    val clock      = Input(Clock())
    val memWdata_i = Input(UInt(32.W))
    val memRaddr_i = Input(UInt(32.W))
    val memWaddr_i = Input(UInt(32.W))
    val memWen_i   = Input(Bool())
    val mask_i     = Input(UInt(4.W))
    val valid_i    = Input(Bool())
    val memRdata_o = Output(UInt(32.W))
  })
  dontTouch(io)
}

class sram extends Module {
  val io = IO(new Bundle {
    val axi = Flipped(new axi_lite)
  })

  val memory = Module(new memory)
  memory.io.clock      := clock
  memory.io.memWen_i   := io.axi.aw.valid
  memory.io.memRaddr_i := io.axi.ar.addr
  memory.io.memWaddr_i := io.axi.aw.addr
  memory.io.memWdata_i := io.axi.w.data
  memory.io.mask_i     := io.axi.w.strb
  memory.io.valid_i    := io.axi.ar.valid
  io.axi.r.data        := memory.io.memRdata_o
  io.axi.r.resp        := 0.U
  io.axi.b.resp        := 0.U
  io.axi.ar.ready      := false.B
  io.axi.r.valid       := false.B
  io.axi.aw.ready      := false.B
  io.axi.w.ready       := false.B
  io.axi.b.valid       := false.B

  val delayRegs = RegInit(VecInit(Seq.fill(100)(false.B)))
  for (i <- 0 until 100) {
    if (i == 0) {
      delayRegs(i) := false.B
    } else {
      delayRegs(i) := delayRegs(i - 1)
    }
  }
  val s_idle :: s_wait_data_read :: s_wait_r_fire :: s_wait_data_write :: s_wait_b_fire :: Nil =
    Enum(5)
  val state = RegInit(s_idle)
  state := MuxLookup(state, s_idle)(
    List(
      s_idle            -> Mux(
        io.axi.ar.valid,
        s_wait_data_read,
        Mux(io.axi.aw.valid, s_wait_data_write, s_idle)
      ),
      s_wait_data_read  -> Mux(delayRegs(1), s_wait_r_fire, s_wait_data_read),
      s_wait_r_fire     -> Mux(io.axi.r.ready, s_idle, s_wait_r_fire),
      s_wait_data_write -> Mux(
        io.axi.w.ready,
        s_wait_b_fire,
        s_wait_data_write
      ), // bug
      s_wait_b_fire -> Mux(io.axi.b.ready, s_idle, s_wait_b_fire)
    )
  )

  when(state === s_idle) {
    // printf("idle\n")
    io.axi.ar.ready := true.B
    io.axi.r.valid  := false.B
    io.axi.aw.ready := true.B
    io.axi.w.ready  := false.B
    io.axi.b.valid  := false.B
    delayRegs(0)    := 0.U
    when(io.axi.ar.valid) {
      delayRegs(0) := true.B
    }
  }

  when(state === s_wait_data_write) {
    // printf("s_wait_data_write\n")
    io.axi.ar.ready := false.B
    io.axi.r.valid  := false.B
    io.axi.aw.ready := true.B
    io.axi.w.ready  := false.B
    io.axi.b.valid  := false.B
    when(io.axi.w.valid) {
      delayRegs(0)   := true.B
      io.axi.w.ready := true.B
    }
  }.elsewhen(state === s_wait_b_fire) {
    // printf("s_wait_b_fire\n")
    io.axi.ar.ready := false.B
    io.axi.r.valid  := false.B
    io.axi.aw.ready := true.B
    io.axi.w.ready  := true.B
    io.axi.b.valid  := true.B
    delayRegs(0)    := false.B

  }

  when(state === s_wait_data_read) {
    // printf("s_wait_data_read\n")
    io.axi.ar.ready := true.B
    io.axi.r.valid  := false.B
    io.axi.aw.ready := false.B
    io.axi.w.ready  := false.B
    io.axi.b.valid  := false.B
    delayRegs(0)    := false.B
  }.elsewhen(state === s_wait_r_fire) {
    // printf("s_wait_r_fire\n")
    io.axi.ar.ready := true.B
    io.axi.r.valid  := true.B
    io.axi.aw.ready := false.B
    io.axi.w.ready  := false.B
    io.axi.b.valid  := false.B
    delayRegs(0)    := false.B
  }
}
