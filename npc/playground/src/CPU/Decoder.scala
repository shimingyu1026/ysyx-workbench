package myCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb

class decoderIO extends Bundle {
  val inst    = Input(UInt(32.W))
  val immType = Output(ImmTypesEnum())
  val regWen  = Output(Bool())
}

class decoder extends Module {
  val io = IO(new decoderIO)
  // Decoder
  val instTable: Iterable[rvdecoderdb.Instruction] =
    rvdecoderdb.instructions(
      os.pwd / "rvdecoderdb" / "rvdecoderdbtest" / "jvm" / "riscv-opcodes"
    )
  val rvTargetSets      = Set("rv_i", "rv32_i", "rv_m", "rv_system", "rv64_i")
  val rvzicsrTargetSets = Set("rv_zicsr")
  val rvInstlist        = instTable
    .filter(instr => rvTargetSets.contains(instr.instructionSet.name))
    .filter(_.pseudoFrom.isEmpty) // 去除伪指令
    .map(InstructionPattern(_))
    .toSeq
  val rvzicsrInstList = instTable
    .filter(instr => rvzicsrTargetSets.contains(instr.instructionSet.name)) // 筛选出属于rv_zicsr的指令
    .filter(_.pseudoFrom.isEmpty)                                           // 去除伪指令
    .map(InstructionPattern(_))
    .toSeq

  val instList = rvInstlist ++ rvzicsrInstList

  val allFields = Seq(ImmType, RegWen)

  val decodeTable  = new DecodeTable(instList, allFields)
  val decodeResult = decodeTable.decode(io.inst) // 解码

  io.immType := decodeResult(ImmType)
  io.regWen  := decodeResult(RegWen)
  // instList.foreach { instruction =>
  // println(instruction.inst.toString)
  // }

}

case class InstructionPattern(val inst: rvdecoderdb.Instruction) extends DecodePattern {
  override def bitPat: BitPat = BitPat("b" + inst.encoding.toString())
}

object RegWen extends DecodeField[InstructionPattern, Bool] {
  override def name = "reg_wen"

  override def chiselType = Bool()

  override def genTable(i: InstructionPattern): BitPat = {
    val regWen = i.inst.args
      .map(_.name match {
        case "rd" => true.B
        case _    => false.B
      })
      .filterNot(_ == false.B)
      .headOption
      .getOrElse(false.B)

    BitPat(regWen)
  }
}

object ImmType extends DecodeField[InstructionPattern, ImmTypesEnum.Type] {
  override def name = "imm_type"

  override def chiselType = ImmTypesEnum()

  override def genTable(i: InstructionPattern): BitPat = {
    val immType = i.inst.args
      .map(_.name match {
        case "imm12" | "shamtd"      => ImmTypesEnum.I
        case "imm12hi" | "imm12lo"   => ImmTypesEnum.S
        case "bimm12hi" | "bimm12lo" => ImmTypesEnum.B
        case "imm20"                 => ImmTypesEnum.U
        case "jimm20"                => ImmTypesEnum.J
        // case "shamtd"                => ImmTypeEnum.immShamtD
        case "shamtw"                => ImmTypesEnum.ShamtW
        case _                       => ImmTypesEnum.None
      })
      .filterNot(_ == ImmTypesEnum.None)
      .headOption // different ImmType will not appear in the Seq
      .getOrElse(ImmTypesEnum.None)
    // TODO: BitPat will accept ChiselEnum after #2327 has been merged
    BitPat(immType.litValue.U((immType.getWidth).W))
  }
}
