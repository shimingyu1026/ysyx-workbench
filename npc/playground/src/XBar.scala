package myCPU

import chisel3._
import chisel3.util._
object StateXBar extends ChiselEnum {
  val sIdle, sCLINT, sOut = Value
  //   00     01      10      11
}

class XBarIO extends Bundle {
  val axi_in = Flipped(new axi_full)

  val axi_out_1 = new axi_full()
  val axi_out_2 = new axi_full()
}

class XBar extends Module {
  val io    = IO(new XBarIO)
  val state = RegInit(StateXBar.sIdle)
  // printf("state: %d\n", state.asUInt)

  switch(state) {
    is(StateXBar.sIdle) {
      when(io.axi_in.awvalid | io.axi_in.arvalid) {
        when(
          io.axi_in.araddr >= "h2000000".U & io.axi_in.araddr <= "h200ffff".U
        ) {
          state := StateXBar.sCLINT
        }.otherwise {
          state := StateXBar.sOut
        }
      }

    }
    is(StateXBar.sCLINT) {
      when(io.axi_in.rready & io.axi_in.rvalid) {
        // printf("go to idle\n")
        state := StateXBar.sIdle
      }
    }
    is(StateXBar.sOut) {
      when(
        (io.axi_in.rready & io.axi_in.rvalid) | (io.axi_in.bready & io.axi_in.bvalid)
      ) {
        state := StateXBar.sIdle
      }
    }
  }
  val stop = Wire(new axi_full())
  stop.awvalid := false.B
  stop.awaddr  := 0.U
  stop.awid    := 0.U
  stop.awlen   := 0.U
  stop.awsize  := 0.U
  stop.awburst := 0.U
  stop.wvalid  := false.B
  stop.wdata   := 0.U
  stop.wstrb   := 0.U
  stop.wlast   := false.B
  stop.bready  := false.B
  stop.arvalid := false.B
  stop.araddr  := 0.U
  stop.arid    := 0.U
  stop.arlen   := 0.U
  stop.arsize  := 0.U
  stop.arburst := 0.U
  stop.rready  := false.B

  when(state === StateXBar.sCLINT) {
    io.axi_out_1 <> stop
    io.axi_out_2 <> io.axi_in
  }.elsewhen(state === StateXBar.sOut) {
    io.axi_out_1 <> io.axi_in
    io.axi_out_2 <> stop
  }.otherwise {
    io.axi_out_1 <> stop
    io.axi_out_2 <> stop
    io.axi_in.arready := false.B
    io.axi_in.rdata   := 0.U
    io.axi_in.rvalid  := false.B
    io.axi_in.rresp   := 0.U
    io.axi_in.rlast   := false.B
    io.axi_in.rid     := 9.U
    io.axi_in.awready := false.B
    io.axi_in.wready  := false.B
    io.axi_in.bvalid  := false.B
    io.axi_in.bresp   := 0.U
    io.axi_in.bid     := 9.U
  }
  dontTouch(io)
}
