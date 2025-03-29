package myCPU

import chisel3._
import chisel3.util._

object StateArbiter extends ChiselEnum {
  val sIdle, sAXI1R, sAXI2R, sAXI2W = Value
}

class ArbiterIO extends Bundle {
  val axi_in_1 = Flipped(new axi_lite)
  val axi_in_2 = Flipped(new axi_lite)

  val axi_out = new axi_lite()
}

class Arb extends Module {
  val io    = IO(new ArbiterIO)
  val state = RegInit(StateArbiter.sIdle)
  switch(state) {
    is(StateArbiter.sIdle) {
      when(io.axi_in_1.ar.valid) {
        state := StateArbiter.sAXI1R
      }

      when(io.axi_in_2.ar.valid) {
        state := StateArbiter.sAXI2R
      }
      when(io.axi_in_2.aw.valid) {
        state := StateArbiter.sAXI2W
      }
    }

    is(StateArbiter.sAXI1R) {
      when(io.axi_in_1.r.ready & io.axi_in_1.r.valid) {
        state := StateArbiter.sIdle
      }
    }

    is(StateArbiter.sAXI2R) {
      when(io.axi_in_2.r.ready & io.axi_in_2.r.valid) {
        state := StateArbiter.sIdle
      }
    }
    is(StateArbiter.sAXI2W) {
      when(io.axi_in_2.b.ready & io.axi_in_2.b.valid) {
        state := StateArbiter.sIdle
      }
    }
  }
  val stop  = Wire(new axi_lite())
  stop.ar.ready := false.B
  stop.r.data   := 0.U
  stop.r.valid  := false.B
  stop.r.resp   := 0.U
  stop.aw.ready := false.B
  stop.w.ready  := false.B
  stop.b.valid  := false.B
  stop.b.resp   := 0.U

  when(state === StateArbiter.sAXI2R | state === StateArbiter.sAXI2W) {
    io.axi_in_2 <> io.axi_out
    io.axi_in_1 <> stop
  }.otherwise {
    io.axi_in_1 <> io.axi_out
    io.axi_in_2 <> stop
  }
}
