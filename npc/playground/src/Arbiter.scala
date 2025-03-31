package myCPU

import chisel3._
import chisel3.util._

object StateArbiter extends ChiselEnum {
  val sIdle, sAXI1R, sAXI2R, sAXI2W = Value
  //   00     01      10      11
}

class ArbiterIO extends Bundle {
  val axi_in_1 = Flipped(new axi_full)
  val axi_in_2 = Flipped(new axi_full)

  val axi_out = new axi_full()
}

class Arb extends Module {
  val io    = IO(new ArbiterIO)
  val state = RegInit(StateArbiter.sIdle)
  switch(state) {
    is(StateArbiter.sIdle) {
      when(io.axi_in_1.arvalid) {
        state := StateArbiter.sAXI1R
      }

      when(io.axi_in_2.arvalid) {
        state := StateArbiter.sAXI2R
      }
      when(io.axi_in_2.awvalid) {
        state := StateArbiter.sAXI2W
      }
    }

    is(StateArbiter.sAXI1R) {
      when(io.axi_in_1.rready & io.axi_in_1.rvalid) {
        state := StateArbiter.sIdle
      }
    }

    is(StateArbiter.sAXI2R) {
      when(io.axi_in_2.rready & io.axi_in_2.rvalid) {
        state := StateArbiter.sIdle
      }
    }
    is(StateArbiter.sAXI2W) {
      when(io.axi_in_2.bready & io.axi_in_2.bvalid) {
        state := StateArbiter.sIdle
      }
    }
  }
  val stop  = Wire(new axi_full())
  stop.arready := false.B
  stop.rdata   := 0.U
  stop.rvalid  := false.B
  stop.rresp   := 0.U
  stop.rlast   := false.B
  stop.rid     := 9.U
  stop.awready := false.B
  stop.wready  := false.B
  stop.bvalid  := false.B
  stop.bresp   := 0.U
  stop.bid     := 9.U

  when(state === StateArbiter.sAXI2R | state === StateArbiter.sAXI2W) {
    // printf("arbiter select axi2\n")
    io.axi_in_2 <> io.axi_out
    io.axi_in_1 <> stop
  }.otherwise {
    // printf("arbiter select axi1\n")
    io.axi_in_1 <> io.axi_out
    io.axi_in_2 <> stop
  }
  dontTouch(io)
}
