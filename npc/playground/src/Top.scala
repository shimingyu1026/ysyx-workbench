package myCPU

import chisel3._
import chisel3.util._

class ysyx_24070001 extends Module {
  val io = IO(new Bundle {
    val master    = new axi_full()
    val slave     = Flipped(new axi_full)
    val interrupt = Input(Bool())

    // val inst_o  = Output(UInt(32.W))
    // val pc_o    = Output(UInt(32.W))

    // for verilator
    // val regs = Output(Vec(32, UInt(32.W)))
  })

  val core    = Module(new core)
  val trap    = Module(new trap)
  val arbiter = Module(new Arb)

  core.io.axi_1 <> arbiter.io.axi_in_1
  core.io.axi_2 <> arbiter.io.axi_in_2
  io.master <> arbiter.io.axi_out

  // io.npcTrap := core.io.npcTrap
  // io.inst_o  := core.io.inst_o
  // io.pc_o    := core.io.pc_o

  // io.regs := core.io.regs

  trap.io.npcTrap := core.io.npcTrap
  trap.io.clock   := clock

//slave
  io.slave.awready := false.B
  io.slave.wready  := false.B
  io.slave.bvalid  := false.B
  io.slave.bresp   := 0.U
  io.slave.bid     := 0.U
  io.slave.arready := false.B
  io.slave.rvalid  := false.B
  io.slave.rresp   := 0.U
  io.slave.rdata   := 0.U
  io.slave.rlast   := false.B
  io.slave.rid     := 0.U

}

class trap extends BlackBox {
  val io = IO(new Bundle {
    val clock   = Input(Clock())
    val npcTrap = Input(Bool())
  })
  dontTouch(io)
}

class instrMen extends BlackBox {
  val io = IO(new Bundle {
    val clock  = Input(Clock())
    val pc_i   = Input(UInt(32.W))
    val inst_o = Output(UInt(32.W))
  })
  dontTouch(io.clock)
  dontTouch(io.pc_i)
  dontTouch(io.inst_o)
}
