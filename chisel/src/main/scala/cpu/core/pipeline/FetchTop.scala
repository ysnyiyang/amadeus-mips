package cpu.core.pipeline

import chisel3._
import chisel3.util.ValidIO
import cpu.CPUConfig
import cpu.core.Constants._
import cpu.core.bundles.stages.IfIf1Bundle
import cpu.core.components.{BrPrUpdateBundle, TwoBitPredictor}
import shared.ValidBundle

class FetchTop(implicit conf: CPUConfig) extends Module {
  val io = IO(new Bundle {
    // from hazard module
    val stall   = Input(Bool())
    val flush   = Input(Bool())
    val flushPC = Input(UInt(addrLen.W))
    val lastDS  = Input(UInt(2.W))

    // from decode stage
    val inDelaySlot = Input(Bool())
    val predict     = Input(new ValidBundle)
    // from execute stage
    val branch     = Input(new ValidBundle)
    val predUpdate = Flipped(ValidIO(new BrPrUpdateBundle))

    // from ram
    val instValid = Input(Bool())

    // to IFID
    val out = Output(new IfIf1Bundle)
    // to ram
    val pcValid  = Output(Bool())
    val pcChange = Output(Bool())
    // to ctrl
    val stallReq = Output(Bool())
  })
  val branchPredictor = Module(new TwoBitPredictor())

  val hazard = Module(new cpu.core.fetch.Hazard)
  val pcMux  = Module(new cpu.core.fetch.PCMux(n = 4))

  val except = io.out.except.asUInt().orR()

  branchPredictor.io.update := io.predUpdate
  branchPredictor.io.pc     := pcMux.io.pc

  hazard.io.flush          := io.flush
  hazard.io.stall          := io.stall
  hazard.io.associateDS    := io.lastDS === 0.U
  hazard.io.in.inDelaySlot := io.inDelaySlot
  hazard.io.in.branch      := io.branch
  hazard.io.in.predict     := io.predict

  pcMux.io.ins(0) := ValidBundle(io.flush, io.flushPC)
  pcMux.io.ins(1) := io.branch
  pcMux.io.ins(2) := ValidBundle(io.stall, pcMux.io.pc)
  pcMux.io.ins(3) := hazard.io.out.predict

  io.out.pc          := pcMux.io.pc
  io.out.instValid   := io.instValid && !except
  io.out.inDelaySlot := hazard.io.out.inDelaySlot
  io.out.brPredict   := branchPredictor.io.prediction

  io.out.brPredict.valid := branchPredictor.io.prediction.valid && io.out.instValid

  io.out.except               := DontCare
  io.out.except(EXCEPT_FETCH) := pcMux.io.pcNotAligned

  io.pcValid  := !except && !(io.branch.valid || io.flush)
  io.pcChange := false.B // priority higher than stall pc
  io.stallReq := !io.instValid && !except
}
