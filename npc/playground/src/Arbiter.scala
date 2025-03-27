package myCPU

import chisel3._
import chisel3.util._

object StateArbiter extends ChiselEnum {
  val sIdle, sAXI1, sAXI2 = Value
}

class ArbiterIO extends Bundle {
  val axi_in_1 = Flipped(new axi_lite)
  val axi_in_2 = Flipped(new axi_lite)

  val axi_out = new axi_lite()
}

class Arbiter extends Module {
  // val io    = IO(new ArbiterIO)
  // val state = RegInit(stateSRAM.sIdle)
}
