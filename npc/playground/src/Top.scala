package myCPU

import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new immExtendIO())

  val immExtend = Module(new immExtend())
  immExtend.io <> io

  val decoder = Module(new decoder())
  decoder.io.inst := io.inst
  dontTouch(decoder.io.immType)
}
