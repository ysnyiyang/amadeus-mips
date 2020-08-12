// See README.md for license details.

package cpu.core.pipeline

import chisel3._
import chisel3.util.Decoupled
import cpu.CPUConfig
import cpu.common.MemReqBundle
import cpu.core.Constants._
import cpu.core.bundles.TLBOpIO
import cpu.core.bundles.stages.{ExeMemBundle, Mem0Mem1Bundle}
import cpu.core.components.{ExceptionHandleBundle, TLBHandleBundle}

class Memory0Top(implicit conf: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val ins     = Input(Vec(conf.decodeWidth, new ExeMemBundle))
    val stalled = Input(Bool())

    val exceptionCP0 = Input(new ExceptionHandleBundle)
    val tlbCP0       = Input(new TLBHandleBundle(conf.tlbSize))

    val mem1Except = Input(Bool())

    // with ram
    val request  = Decoupled(new MemReqBundle)
    val uncached = Input(Bool())

    val iCacheInvalidate = Decoupled(UInt(conf.iCacheConf.indexLen.W))

    val dCacheInvalidate = Decoupled(UInt(0.W))

    // with mmu
    val tlb = new TLBOpIO(conf.tlbSize)

    val out      = Output(Vec(conf.decodeWidth, new Mem0Mem1Bundle))
    val stallReq = Output(Bool())
  })

  val control = Module(new cpu.core.memory.Control)
  val excepts = Seq.fill(2)(Module(new cpu.core.memory.Except))

  val memSlot   = Mux(io.ins(1).instType === INST_MEM, 0.U, 1.U)
  val hasExcept = VecInit(excepts.head.io.outExcept.reduce(_ || _), excepts.map(_.io.outExcept.reduce(_ || _)).reduce(_ || _))

  control.io.stalled   := io.stalled
  control.io.inMemData := io.ins(memSlot).memData
  control.io.operation := io.ins(memSlot).operation
  control.io.addr      := io.ins(memSlot).memAddr
  control.io.except    := hasExcept(memSlot) || io.mem1Except

  excepts.zip(io.ins).foreach {
    case (except, in) =>
      except.io.pc        := in.pc
      except.io.addr      := in.memAddr
      except.io.instValid := in.instValid
      except.io.op        := in.operation
      except.io.cp0Status := io.exceptionCP0.status
      except.io.cp0Cause  := io.exceptionCP0.cause
      except.io.inExcept  := in.except

      except.io.tlbExcept.refill   := io.tlb.except.data.refill
      except.io.tlbExcept.invalid  := io.tlb.except.data.invalid
      except.io.tlbExcept.modified := io.tlb.except.data.modified
  }

  io.out.zip(io.ins).zip(excepts).zipWithIndex.foreach {
    case (((out, in), except), i) =>
      out.addrL2             := in.memAddr(1, 0)
      out.op                 := in.operation
      out.write              := in.write
      out.write.enable       := in.write.enable && !hasExcept(i)
      out.pc                 := in.pc
      out.uncached           := io.uncached
      out.inDelaySlot        := in.inDelaySlot
      out.except             := except.io.outExcept
      out.badAddr            := except.io.badAddr
      out.tlbWrite           := io.tlb
      out.cp0Write           := in.cp0
      out.cp0Write.valid     := in.cp0.valid && !hasExcept(i)
      out.hiloWrite          := in.hilo
      out.hiloWrite.lo.valid := in.hilo.lo.valid && !hasExcept(i)
      out.hiloWrite.hi.valid := in.hilo.hi.valid && !hasExcept(i)
  }
  io.stallReq := control.io.stallReq || (io.dCacheInvalidate.valid && !io.dCacheInvalidate.ready) || (io.iCacheInvalidate.valid && !io.iCacheInvalidate.ready)

  io.request <> control.io.request

  io.dCacheInvalidate.bits  := DontCare
  io.dCacheInvalidate.valid := io.ins(0).cacheOp.target === TARGET_D && io.ins(0).cacheOp.valid && !hasExcept(0) && !io.mem1Except && io.ins(0).instValid

  val indexFrom = conf.iCacheConf.indexLen + conf.iCacheConf.bankIndexLen + conf.iCacheConf.bankOffsetLen - 1
  val indexTo   = conf.iCacheConf.bankIndexLen + conf.iCacheConf.bankOffsetLen
  io.iCacheInvalidate.bits  := io.ins(0).memAddr(indexFrom, indexTo)
  io.iCacheInvalidate.valid := io.ins(0).cacheOp.target === TARGET_I && io.ins(0).cacheOp.valid && !hasExcept(0) && !io.mem1Except && io.ins(0).instValid

  io.tlb.asid          := io.tlbCP0.entryHi.asid
  io.tlb.kseg0Uncached := false.B

  io.tlb.instrReq.writeEn := (io.ins(0).operation === TLB_WR || io.ins(0).operation === TLB_WI) && !hasExcept(0) && !io.mem1Except
  io.tlb.instrReq.TLBIndex := Mux(
    io.ins(0).operation === TLB_WR,
    io.tlbCP0.random.random,
    io.tlbCP0.index.index
  )

  io.tlb.instrReq.writeData.global   := io.tlbCP0.entryLo0.global & io.tlbCP0.entryLo1.global
  io.tlb.instrReq.writeData.asid     := io.tlbCP0.entryHi.asid
  io.tlb.instrReq.writeData.vpn2     := io.tlbCP0.entryHi.vpn2
  io.tlb.instrReq.writeData.pages(0) := io.tlbCP0.entryLo0
  io.tlb.instrReq.writeData.pages(1) := io.tlbCP0.entryLo1

  io.tlb.probeReq := io.tlbCP0.entryHi.vpn2
}
