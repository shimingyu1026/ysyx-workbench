package myCPU

import chisel3._
import chisel3.util._

class CLINT extends Module {
  val io         = IO(new Bundle {
    val clint = Flipped(new axi_full)
  })
//计数器
  val clint_regs = RegInit(0.U(64.W))
  clint_regs       := clint_regs + 1.U
//赋初值
  io.clint.arready := true.B

  io.clint.rvalid := true.B
  io.clint.rdata  := 0.U
  io.clint.rresp  := 0.U
  io.clint.rlast  := true.B
  io.clint.rid    := 0.U

  io.clint.awready := false.B

  io.clint.wready := false.B

  io.clint.bvalid := false.B
  io.clint.bresp  := 0.U
  io.clint.bid    := 0.U
  when(io.clint.araddr === "h2000000".U) {
    // printf("clint_regs: %x\n", clint_regs)
    io.clint.rdata := clint_regs(31, 0).asUInt
  }.elsewhen(io.clint.araddr === "h2000004".U) {
    // printf("clint_regs: %x\n", clint_regs)
    io.clint.rdata := clint_regs(63, 32).asUInt
  }.otherwise {
    io.clint.rdata := 0.U
  }
  dontTouch(io)

}
