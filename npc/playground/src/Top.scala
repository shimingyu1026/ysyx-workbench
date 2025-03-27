package myCPU

import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val npcTrap = Output(Bool())
    val inst_o  = Output(UInt(32.W))
    val pc_o    = Output(UInt(32.W))

    // for verilator
    val regs = Output(Vec(32, UInt(32.W)))
  })

  val core      = Module(new core)
  val trap      = Module(new trap)
  val instrSRAM = Module(new sram)
  val dataSRAM  = Module(new sram)

  instrSRAM.io.axi <> core.io.axi_1
  dataSRAM.io.axi <> core.io.axi_2

  io.npcTrap := core.io.npcTrap
  io.inst_o  := core.io.inst_o
  io.pc_o    := core.io.pc_o

  io.regs := core.io.regs

  trap.io.npcTrap := core.io.npcTrap
  trap.io.clock   := clock

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
