package myCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb
import java.util.Base64.Decoder

class decoderIO extends Bundle {
  val inst    = Input(UInt(32.W))
  val immType = Output(ImmTypesEnum())
  val regWen  = Output(Bool())
  val aluCtrl = Output(AluCtrlEnum())
  val srcASel = Output(SrcASelEnum())
  val srcBSel = Output(SrcBSelEnum())
  val brType  = Output(BrTypeEnum())

  // to mmu
  val memWen    = Output(MemWenEnum())
  val loadCtrl  = Output(LoadCtrlEnum())
  val storeCtrl = Output(StoreCtrlEnum())

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

  val allFields = Seq(ImmType, RegWen, ALUCtrl, SrcASel, SrcBSel, BrType, MemWen, LoadCtrl, StoreCtrl)

  val decodeTable  = new DecodeTable(instList, allFields)
  val decodeResult = decodeTable.decode(io.inst) // 解码

  io.immType   := decodeResult(ImmType)
  io.regWen    := decodeResult(RegWen)
  io.aluCtrl   := decodeResult(ALUCtrl)
  io.srcASel   := decodeResult(SrcASel)
  io.srcBSel   := decodeResult(SrcBSel)
  io.brType    := decodeResult(BrType)
  io.memWen    := decodeResult(MemWen)
  io.loadCtrl  := decodeResult(LoadCtrl)
  io.storeCtrl := decodeResult(StoreCtrl)

  instList.foreach { instruction =>
    println(instruction.inst.toString)
  }

}

object MemWen extends DecodeField[InstructionPattern, MemWenEnum.Type] {
  override def name       = "mem_wen"
  override def chiselType = MemWenEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val wen = i.inst.name match {
      case "sb" | "sh" | "sw" => MemWenEnum.wen
      case _                  => MemWenEnum.none
    }
    BitPat(wen.litValue.U((wen.getWidth).W))
  }

}
object StoreCtrl extends DecodeField[InstructionPattern, StoreCtrlEnum.Type] {
  override def name       = "store_ctrl"
  override def chiselType = StoreCtrlEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val ctrl = i.inst.name match {
      case "sb" => StoreCtrlEnum.sb
      case "sh" => StoreCtrlEnum.sh
      case "sw" => StoreCtrlEnum.sw
      case _    => StoreCtrlEnum.none
    }
    BitPat(ctrl.litValue.U((ctrl.getWidth).W))
  }
}
object LoadCtrl  extends DecodeField[InstructionPattern, LoadCtrlEnum.Type]  {
  override def name       = "load_ctrl"
  override def chiselType = LoadCtrlEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val ctrl = i.inst.name match {
      case "lb"  => LoadCtrlEnum.lb
      case "lbu" => LoadCtrlEnum.lbu
      case "lh"  => LoadCtrlEnum.lh
      case "lhu" => LoadCtrlEnum.lhu
      case "lw"  => LoadCtrlEnum.lw
      case _     => LoadCtrlEnum.none
    }
    BitPat(ctrl.litValue.U((ctrl.getWidth).W))
  }
}
object BrType    extends DecodeField[InstructionPattern, BrTypeEnum.Type]    {
  override def name       = "br_type"
  override def chiselType = BrTypeEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val brtype = i.inst.name match {
      case "beq"  => BrTypeEnum.beq
      case "bge"  => BrTypeEnum.bge
      case "bgeu" => BrTypeEnum.bgeu
      case "blt"  => BrTypeEnum.blt
      case "bltu" => BrTypeEnum.bltu
      case "bne"  => BrTypeEnum.bne
      case _      => BrTypeEnum.none
    }
    BitPat(brtype.litValue.U((brtype.getWidth).W))
  }
}

object SrcBSel extends DecodeField[InstructionPattern, SrcBSelEnum.Type] {
  override def name       = "src_b_sel"
  override def chiselType = SrcBSelEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val sel = i.inst.args
      .map(_.name match {
        case "rs2"                                                           => SrcBSelEnum.rs2
        case "imm12" | "shamtd" | "imm12hi" | "imm12lo" | "imm20" | "jimm20" => SrcBSelEnum.imm
        case _                                                               => SrcBSelEnum.none
      })
      .filterNot(_ == SrcBSelEnum.none)
      .headOption // different ImmType will not appear in the Seq
      .getOrElse(SrcBSelEnum.none)

    BitPat(sel.litValue.U((sel.getWidth).W))

  }
}

object SrcASel extends DecodeField[InstructionPattern, SrcASelEnum.Type] {
  override def name       = "src_a_sel"
  override def chiselType = SrcASelEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val sel = i.inst.args
      .map(_.name match {
        case "rs1" => SrcASelEnum.rs1
        case _     => SrcASelEnum.none
      })
      .filterNot(_ == SrcASelEnum.none)
      .headOption // different ImmType will not appear in the Seq
      .getOrElse(SrcASelEnum.none)

    i.inst.name match {
      case "jal" | "auipc" => BitPat(SrcASelEnum.pc.litValue.U((SrcASelEnum.pc.getWidth).W))
      case _               => BitPat(sel.litValue.U((sel.getWidth).W))
    }
  }
}
object ALUCtrl extends DecodeField[InstructionPattern, AluCtrlEnum.Type] {
  override def name       = "alu_ctrl"
  override def chiselType = AluCtrlEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val ctrl = i.inst.name match {
      case "lw" | "lb" | "lh" | "lbu" | "lhu" | "sw" | "sb" | "sh" | "add" | "addi" | "jal" | "lui" | "auipc" =>
        AluCtrlEnum.add
      case "sub"                                                                                              => AluCtrlEnum.sub
      case "and" | "andi"                                                                                     => AluCtrlEnum.and
      case "or" | "ori"                                                                                       => AluCtrlEnum.or
      case "xor" | "xori"                                                                                     => AluCtrlEnum.xor
      case "sll" | "slli"                                                                                     => AluCtrlEnum.sll
      case "srl" | "srli"                                                                                     => AluCtrlEnum.srl
      case "sra" | "srai"                                                                                     => AluCtrlEnum.sra
      case "slt" | "slti"                                                                                     => AluCtrlEnum.slt
      case "sltu" | "sltiu"                                                                                   => AluCtrlEnum.sltu

      case _ =>
        AluCtrlEnum.none

    }
    return BitPat(ctrl.litValue.U((ctrl.getWidth).W))
  }
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

    BitPat(regWen.litValue.U((regWen.getWidth).W))
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

case class InstructionPattern(val inst: rvdecoderdb.Instruction) extends DecodePattern {
  override def bitPat: BitPat = BitPat("b" + inst.encoding.toString())
}
