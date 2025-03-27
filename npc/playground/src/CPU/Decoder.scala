package myCPU

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb

class decoderIO extends Bundle {
  val inst = Input(UInt(32.W))

  // to idu
  val immType = Output(ImmTypesEnum())

  // to exu
  val brType  = Output(BrTypeEnum())
  val srcASel = Output(SrcASelEnum())
  val srcBSel = Output(SrcBSelEnum())
  val aluCtrl = Output(AluCtrlEnum())

  // to mmu
  val memValid  = Output(Bool())
  val memWen    = Output(MemWenEnum())
  val loadCtrl  = Output(LoadCtrlEnum())
  val storeCtrl = Output(StoreCtrlEnum())

  // to wbu and go back to idu
  val regWen = Output(Bool())
  val wbSel  = Output(WbSelEnum())

  // to ifu
  val pcSel = Output(PCSelEnum())

  // to sim
  val npcTrap = Output(Bool())

  val csr_wen  = Output(Bool())
  val csr_ctrl = Output(CSRCtrlEnum())

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

  val allFields =
    Seq(
      NPCTrap,
      PCSel,
      WbSel,
      CSRCtrl,
      CSRWen,
      ImmType,
      RegWen,
      ALUCtrl,
      SrcASel,
      SrcBSel,
      BrType,
      MemWen,
      LoadCtrl,
      StoreCtrl,
      MemValid
    )

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
  io.wbSel     := decodeResult(WbSel)
  io.pcSel     := decodeResult(PCSel)
  io.npcTrap   := decodeResult(NPCTrap)
  io.csr_wen   := decodeResult(CSRWen)
  io.csr_ctrl  := decodeResult(CSRCtrl)
  io.memValid  := decodeResult(MemValid)

  instList.foreach { instruction =>
    println(instruction.inst.toString)
  }

}

object CSRCtrl extends DecodeField[InstructionPattern, CSRCtrlEnum.Type] {
  override def name       = "csr_ctrl"
  override def chiselType = CSRCtrlEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val ctrl = i.inst.name match {
      case "mret"  => CSRCtrlEnum.mret
      case "ecall" => CSRCtrlEnum.ecall
      case _       => CSRCtrlEnum.none
    }
    return BitPat(ctrl.litValue.U((ctrl.getWidth).W))
  }
}

object CSRWen  extends DecodeField[InstructionPattern, Bool] {
  override def name       = "csr_wen"
  override def chiselType = Bool()
  override def genTable(i: InstructionPattern): BitPat = {
    val wen = i.inst.name match {
      case "csrrw" | "csrrs" | "csrrc" | "csrrwi" | "csrrsi" | "csrrci" => true.B
      case _                                                            => false.B
    }
    BitPat(wen.litValue.U((wen.getWidth).W))
  }
}
object NPCTrap extends DecodeField[InstructionPattern, Bool] {
  override def name       = "trap"
  override def chiselType = Bool()
  override def genTable(i: InstructionPattern): BitPat = {
    val trap = i.inst.name match {
      case "ebreak" => true.B
      case _        => false.B
    }
    BitPat(trap.litValue.U((trap.getWidth).W))
  }
}

object PCSel extends DecodeField[InstructionPattern, PCSelEnum.Type] {
  override def name       = "pc_sel"
  override def chiselType = PCSelEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val sel = i.inst.name match {
      case "jal"                                           => PCSelEnum.jal
      case "jalr"                                          => PCSelEnum.jalr
      case "beq" | "bge" | "bgeu" | "blt" | "bltu" | "bne" => PCSelEnum.branch
      case "ecall" | "mret"                                => PCSelEnum.csr
      case _                                               => PCSelEnum.pcplus4
    }
    BitPat(sel.litValue.U((sel.getWidth).W))
  }
}

object WbSel    extends DecodeField[InstructionPattern, WbSelEnum.Type]  {
  override def name       = "wb_sel"
  override def chiselType = WbSelEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val sel = i.inst.name match {
      case "jal" | "jalr"                     => WbSelEnum.pcplus4
      case "lb" | "lbu" | "lh" | "lhu" | "lw" => WbSelEnum.mem
      case "lui"                              => WbSelEnum.imm
      case "csrrw" | "csrrs"                  => WbSelEnum.csr
      case _                                  => WbSelEnum.alu
    }
    BitPat(sel.litValue.U((sel.getWidth).W))
  }
}
object MemValid extends DecodeField[InstructionPattern, Bool]            {
  override def name       = "mem_valid"
  override def chiselType = Bool()
  override def genTable(i: InstructionPattern): BitPat = {
    val valid = i.inst.name match {
      case "sb" | "sh" | "sw" | "lb" | "lbu" | "lh" | "lhu" | "lw" => true.B
      case _                                                       => false.B
    }
    BitPat(valid.litValue.U((valid.getWidth).W))
  }
}
object MemWen   extends DecodeField[InstructionPattern, MemWenEnum.Type] {
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
    val sela = i.inst.args
      .map(_.name match {
        case "rs2"                                                           => SrcBSelEnum.rs2
        case "imm12" | "shamtd" | "imm12hi" | "imm12lo" | "imm20" | "jimm20" => SrcBSelEnum.imm
        case _                                                               => SrcBSelEnum.none
      })
      .filterNot(_ == SrcBSelEnum.none)
      .headOption // different ImmType will not appear in the Seq
      .getOrElse(SrcBSelEnum.none)
    i.inst.name match {
      case "beq" | "bge" | "bgeu" | "blt" | "bltu" | "bne" =>
        BitPat(SrcBSelEnum.imm.litValue.U((SrcBSelEnum.imm.getWidth).W))
      case "csrrs"                                         => BitPat(SrcBSelEnum.csr.litValue.U((SrcBSelEnum.csr.getWidth).W))
      case "csrrw"                                         => BitPat(SrcBSelEnum.zero.litValue.U((SrcBSelEnum.zero.getWidth).W))
      case _                                               => BitPat(sela.litValue.U((sela.getWidth).W))
    }

  }
}

object SrcASel extends DecodeField[InstructionPattern, SrcASelEnum.Type] {
  override def name       = "src_a_sel"
  override def chiselType = SrcASelEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val selb = i.inst.args
      .map(_.name match {
        case "rs1" => SrcASelEnum.rs1
        case _     => SrcASelEnum.none
      })
      .filterNot(_ == SrcASelEnum.none)
      .headOption // different ImmType will not appear in the Seq
      .getOrElse(SrcASelEnum.none)

    i.inst.name match {
      case "jal" | "auipc" | "beq" | "bge" | "bgeu" | "blt" | "bltu" | "bne" =>
        BitPat(SrcASelEnum.pc.litValue.U((SrcASelEnum.pc.getWidth).W))
      case "csrrw" | "csrrs"                                                 => BitPat(SrcASelEnum.rs1.litValue.U((SrcASelEnum.rs1.getWidth).W))
      case _                                                                 => BitPat(selb.litValue.U((selb.getWidth).W))
    }
  }
}
object ALUCtrl extends DecodeField[InstructionPattern, AluCtrlEnum.Type] {
  override def name       = "alu_ctrl"
  override def chiselType = AluCtrlEnum()
  override def genTable(i: InstructionPattern): BitPat = {
    val ctrl = i.inst.name match {
      case "lw" | "lb" | "lh" | "lbu" | "lhu" | "sw" | "sb" | "sh" | "add" | "addi" | "jal" | "lui" | "auipc" | "jalr" |
          "bne" | "bltu" | "blt" | "bge" | "bgeu" | "beq" =>
        AluCtrlEnum.add
      case "sub"                  => AluCtrlEnum.sub
      case "and" | "andi"         => AluCtrlEnum.and
      case "or" | "ori" | "csrrs" => AluCtrlEnum.or
      case "xor" | "xori"         => AluCtrlEnum.xor
      case "sll" | "slli"         => AluCtrlEnum.sll
      case "srl" | "srli"         => AluCtrlEnum.srl
      case "sra" | "srai"         => AluCtrlEnum.sra
      case "slt" | "slti"         => AluCtrlEnum.slt
      case "sltu" | "sltiu"       => AluCtrlEnum.sltu

      case _ =>
        AluCtrlEnum.add

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
