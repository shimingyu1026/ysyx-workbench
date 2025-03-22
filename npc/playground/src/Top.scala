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

  val core = Module(new core)

  val instrMen = Module(new instrMen)

  val dataMem = Module(new dataMem)

  val trap = Module(new trap)

  instrMen.io.pc_i  := core.io.pc_o
  instrMen.io.clock := clock

  dataMem.io.clock      := clock
  dataMem.io.memWdata_i := core.io.memWdata_o
  dataMem.io.memRaddr_i := core.io.memRaddr_o
  dataMem.io.memWaddr_i := core.io.memWaddr_o
  dataMem.io.memWen_i   := core.io.memWen_o

  core.io.inst_i     := instrMen.io.inst_o
  core.io.memRdata_i := dataMem.io.memRdata_o

  io.npcTrap := core.io.npcTrap
  io.inst_o  := instrMen.io.inst_o
  io.pc_o    := core.io.pc_o

  io.regs := core.io.regs

  trap.io.npcTrap := core.io.npcTrap

}

class trap extends BlackBox {
  val io = IO(new Bundle {
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

class dataMem extends BlackBox {
  val io = IO(new Bundle {
    val clock      = Input(Clock())
    val memWdata_i = Input(UInt(32.W))
    val memRaddr_i = Input(UInt(32.W))
    val memWaddr_i = Input(UInt(32.W))
    val memWen_i   = Input(Bool())
    val memRdata_o = Output(UInt(32.W))
  })
  dontTouch(io)
}
