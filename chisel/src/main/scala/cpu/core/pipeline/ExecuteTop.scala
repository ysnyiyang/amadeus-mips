// See README.md for license details.

package cpu.core.pipeline

import chisel3._
import chisel3.util.ValidIO
import cpu.CPUConfig
import cpu.core.Constants._
import cpu.core.bundles._
import cpu.core.bundles.stages.{ExeMemBundle, IdExeBundle}
import cpu.core.components.{BrPrUpdateBundle, Div, Mult}
import cpu.core.execute.components._
import shared.ValidBundle

class ExecuteTop(implicit conf: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val in = Input(new IdExeBundle)

    /** For multi-cycle multiplication and division. */
    val flush = Input(Bool())

    val rawHILO = Input(new HILOBundle)
    val memHILO = Input(new HILOValidBundle)
    val wbHILO  = Input(new HILOValidBundle)

    val cp0Data = Input(UInt(dataLen.W)) // from cp0
    val memCP0  = Input(new CPBundle)
    val wbCP0   = Input(new CPBundle)

    val out        = Output(new ExeMemBundle)
    val branch     = Output(new ValidBundle) // back to `Fetch`
    val predUpdate = ValidIO(new BrPrUpdateBundle)
    val stallReq   = Output(Bool())
  })

  val alu        = Module(new ALU)
  val move       = Module(new Move)
  val writeOther = Module(new WriteOther)
  val memory     = Module(new Memory)

  val mult = Module(new Mult)
  val div  = Module(new Div)

  val branch = Module(new Branch)

  /** Only used in `move` module */
  val forward = Module(new cpu.core.execute.Forward)
  val control = Module(new cpu.core.execute.Control)

  forward.io.rawHILO   := io.rawHILO
  forward.io.fwHILO(0) := io.memHILO
  forward.io.fwHILO(1) := io.wbHILO

  // cp0 come from decode, data come from CP0 regfile
  forward.io.rawCP0      := io.in.cp0
  forward.io.rawCP0.data := io.cp0Data
  forward.io.fwCP0(0)    := io.memCP0
  forward.io.fwCP0(1)    := io.wbCP0

  alu.io.op1       := io.in.op1
  alu.io.op2       := io.in.op2
  alu.io.operation := io.in.operation

  move.io.operation := io.in.operation
  move.io.hilo      := forward.io.outHILO
  move.io.cp0Data   := forward.io.outCP0

  writeOther.io.op1       := io.in.op1
  writeOther.io.op2       := io.in.op2
  writeOther.io.operation := io.in.operation
  writeOther.io.flush     := io.flush
  writeOther.io.inCP0     := io.in.cp0
  writeOther.io.mult      <> mult.io
  writeOther.io.div       <> div.io

  memory.io.op1       := io.in.op1
  memory.io.op2       := io.in.op2
  memory.io.imm16     := io.in.imm26(15, 0)
  memory.io.operation := io.in.operation

  branch.io.op1       := io.in.op1
  branch.io.op2       := io.in.op2
  branch.io.operation := io.in.operation
  branch.io.imm26     := io.in.imm26
  branch.io.pc        := io.in.pc

  control.io.instType := io.in.instType
  control.io.inWrite  := io.in.write
  control.io.pc       := io.in.pc
  control.io.inExcept := io.in.except

  control.io.aluResult   := alu.io.result
  control.io.aluOverflow := alu.io.overflow
  control.io.exceptLoad  := memory.io.exceptLoad
  control.io.exceptSave  := memory.io.exceptSave
  control.io.moveResult  := move.io.result

  io.out.write       := control.io.outWrite
  io.out.operation   := io.in.operation
  io.out.cp0         := writeOther.io.outCP0
  io.out.hilo        := writeOther.io.outHILO
  io.out.inDelaySlot := io.in.inDelaySlot
  io.out.except      := control.io.outExcept
  io.out.pc          := io.in.pc
  io.out.memAddr     := memory.io.memAddr
  io.out.memData     := io.in.op2

  val brPrFail =
    (branch.io.branch.valid ^ io.in.brPredict.valid) || (branch.io.branch.valid && branch.io.branch.bits =/= io.in.brPredict.bits)

  io.branch.bits  := Mux(branch.io.branch.valid, branch.io.branch.bits, io.in.pc + 8.U)
  io.branch.valid := brPrFail

  io.predUpdate.valid       := brPrFail
  io.predUpdate.bits.pc     := io.in.pc
  io.predUpdate.bits.target := branch.io.branch.bits
  io.predUpdate.bits.taken  := branch.io.branch.valid

  io.stallReq := writeOther.io.stallReq

  // performance
  val brPrTotal = RegInit(0.U.asTypeOf(new BrPrPerfBundle))
  val brPrJ     = RegInit(0.U.asTypeOf(new BrPrPerfBundle))
  val brPrB     = RegInit(0.U.asTypeOf(new BrPrPerfBundle))

  val pcReg = RegInit(0.U(addrLen.W))
  when(io.in.instType === INST_BR && pcReg =/= io.in.pc) {
    val isJ = VecInit(Seq(BR_JR, BR_JALR, BR_J, BR_JAL)).contains(io.in.operation)
    pcReg := io.in.pc
    when(io.branch.valid) {
      brPrTotal.fail := brPrTotal.fail + 1.U
      when(isJ) {
        brPrJ.fail := brPrJ.fail + 1.U
      }.otherwise {
        brPrB.fail := brPrB.fail + 1.U
      }
    }.otherwise {
      brPrTotal.success := brPrTotal.success + 1.U
      when(isJ) {
        brPrJ.success := brPrJ.success + 1.U
      }.otherwise {
        brPrB.success := brPrB.success + 1.U
      }
    }
  }
}

class BrPrPerfBundle extends Bundle {
  val success = UInt(64.W)
  val fail    = UInt(64.W)
}
