// See README.md for license details.

package cpu.core.components

import chisel3._
import chisel3.experimental.chiselName
import chisel3.util._
import cpu.CPUConfig
import cpu.common._
import cpu.core.Constants._
import cpu.core.bundles.{CPBundle, TLBReadBundle}

class TLBHandleBundle(tlbSize: Int) extends Bundle {
  val entryHi  = new EntryHiBundle
  val pageMask = new PageMaskBundle
  val entryLo0 = new EntryLoBundle
  val entryLo1 = new EntryLoBundle
  val index    = new IndexBundle(tlbSize)
  val random   = new RandomBundle(tlbSize)

  override def cloneType: TLBHandleBundle.this.type = new TLBHandleBundle(tlbSize).asInstanceOf[this.type]
}

class ExceptionHandleBundle extends Bundle {
  val status = new StatusBundle
  val cause  = new CauseBundle
  val EPC    = UInt(dataLen.W)
  val ebase  = new EBaseBundle
}

@chiselName
class CP0(tlbSize: Int = 32)(implicit conf: CPUConfig) extends Module {
  val tlbWidth = log2Ceil(tlbSize)
  val io = IO(new Bundle {
    val intr = Input(UInt(intrLen.W))

    // cp0 write
    val cp0Write = Input(new CPBundle)
    val op       = Input(UInt(opLen.W))
    val tlb      = Input(new TLBReadBundle)

    // exception handler
    val except      = Input(Vec(exceptAmount, Bool()))
    val inDelaySlot = Input(Bool())
    val pc          = Input(UInt(addrLen.W))
    val badAddr     = Input(UInt(addrLen.W))
    val isWait      = Input(Bool())

    // read port
    val read = Vec(
      conf.decodeWidth,
      new Bundle {
        val addr = Input(UInt(regAddrLen.W))
        val sel  = Input(UInt(3.W))
        val data = Output(UInt(dataLen.W))
      }
    )

    val exceptionCP0 = Output(new ExceptionHandleBundle)

    val tlbCP0        = Output(new TLBHandleBundle(tlbSize))
    val kseg0Uncached = Output(Bool())

    val llSet   = Input(Bool())
    val llClear = Input(Bool())
    val llGet   = Output(Bool())
  })

  val index    = new IndexCP0(tlbSize)
  val random   = new RandomCP0(tlbSize)
  val entryLo0 = new EntryLoCP0(lo = 0)
  val entryLo1 = new EntryLoCP0(lo = 1)
  val context  = new ContextCP0
  val pageMask = new PageMaskCP0
  val wired    = new WiredCP0(tlbSize)
  val badVAddr = new BadVAddrCP0
  val count    = new CountCP0
  val entryHi  = new EntryHiCP0
  // TODO add interrupt support for [[compare]]
  val compare = new CompareCP0
  val status  = new StatusCP0
  val cause   = new CauseCP0
  val epc     = new EPCCP0
  val ebase   = new EBaseCP0
  val prid    = new PRIDCP0
  val config0 = new Config0CP0()
  val config1 = new Config1CP0()

  val cp0Seq = Seq(
    index,
    random,
    entryLo0,
    entryLo1,
    context,
    pageMask,
    wired,
    badVAddr,
    count,
    entryHi,
    compare,
    status,
    cause,
    epc,
    ebase,
    prid,
    config0,
    config1
  )

  val isKernelMode = !status.reg.um || status.reg.erl || status.reg.exl

  // soft write
  when(io.cp0Write.enable && io.cp0Write.valid) {
    val c = io.cp0Write
    cp0Seq.foreach(cp0 => {
      when(cp0.index.U === Cat(c.addr, c.sel)) {
        cp0.softWrite(c.data)
      }
    })
  }

  val tick = RegInit(false.B)
  tick := !tick

  val except = io.except.asUInt().orR()

  def compareWriteCP0(p: BaseCP0): Bool = {
    val c = io.cp0Write
    c.enable && c.valid && c.addr === p.addr.U && c.sel === p.sel.U
  }

  val excCode =
    MuxCase(
      cause.reg.excCode,
      Seq(
        io.except(EXCEPT_INTR)                -> 0.U(5.W), // int
        io.except(EXCEPT_FETCH)               -> "h04".U(5.W), // AdEL
        io.except(EXCEPT_INST_TLB_REFILL)     -> "h02".U(5.W), // TLBL
        io.except(EXCEPT_INST_TLB_INVALID)    -> "h02".U(5.W), // TLBL
        io.except(EXCEPT_INST_INVALID)        -> "h0a".U(5.W), // RI
        io.except(EXCEPT_OVERFLOW)            -> "h0c".U(5.W), // Ov
        io.except(EXCEPT_SYSCALL)             -> "h08".U(5.W), // Sys
        io.except(EXCEPT_BREAK)               -> "h09".U(5.W), // Break
        io.except(EXCEPT_TRAP)                -> "h0d".U(5.W), // Tr
        io.except(EXCEPT_LOAD)                -> "h04".U(5.W), // AdEL
        io.except(EXCEPT_STORE)               -> "h05".U(5.W), // AdES
        io.except(EXCEPT_DATA_TLB_R_REFILL)   -> "h02".U(5.W), // TLBL
        io.except(EXCEPT_DATA_TLB_W_REFILL)   -> "h03".U(5.W), // TLBS
        io.except(EXCEPT_DATA_TLB_R_INVALID)  -> "h02".U(5.W), // TLBL
        io.except(EXCEPT_DATA_TLB_W_INVALID)  -> "h03".U(5.W), // TLBS
        io.except(EXCEPT_DATA_TLB_W_MODIFIED) -> "h01".U(5.W) // Mod
      )
    )

  val timeIntr = RegInit(false.B)
  when(compare.reg =/= 0.U && compare.reg === count.reg) {
    timeIntr := true.B
  }.elsewhen(compareWriteCP0(compare)) {
    timeIntr := false.B
  }

  // hard write
  when(io.op === TLB_R) {
    entryHi.reg.vpn2  := io.tlb.readResp.vpn2
    entryHi.reg.asid  := io.tlb.readResp.asid
    pageMask.reg.mask := 0.U
    Seq(entryLo0.reg, entryLo1.reg)
      .zip(io.tlb.readResp.pages)
      .foreach(e => {
        e._1.pfn          := e._2.pfn
        e._1.cacheControl := e._2.cacheControl
        e._1.valid        := e._2.valid
        e._1.dirty        := e._2.dirty
        e._1.global       := io.tlb.readResp.global
      })
  }.elsewhen(io.op === TLB_P) {
    index.reg.p     := io.tlb.probeResp(31)
    index.reg.index := io.tlb.probeResp(tlbWidth - 1, 0)
  }

  when(isTLBExcept(io.except)) {
    entryHi.reg.vpn2 := io.badAddr(31, 13)
  }

  when(compareWriteCP0(wired)) {
    random.reg.random := (tlbSize - 1).U
  }.elsewhen(io.op === TLB_WR) {
    random.reg.random := Mux(random.reg.random.andR(), wired.reg.wired, random.reg.random + 1.U)
  }

  when(io.except(EXCEPT_FETCH) || io.except(EXCEPT_LOAD) || io.except(EXCEPT_STORE) || isTLBExcept(io.except)) {
    badVAddr.reg := io.badAddr
  }

  /** increase 1 every two cycle */
  when(!compareWriteCP0(count) && tick) {
    count.reg := count.reg + 1.U
  }

  when(io.except(EXCEPT_ERET)) {
    status.reg.exl := false.B
  }.elsewhen(except) {
    status.reg.exl := true.B
  }

  when(except && !status.reg.exl) {
    cause.reg.bd      := io.inDelaySlot
  }
  cause.reg.ipHard := io.intr
  cause.reg.excCode := excCode

  when(except && !io.except(EXCEPT_ERET) && !status.reg.exl) {
    epc.reg := MuxCase(
      io.pc,
      Array(
        io.inDelaySlot -> (io.pc - 4.U),
        io.isWait      -> (io.pc + 4.U)
      )
    )
  }

  io.read.foreach(r => {
    r.data := MuxLookup(
      Cat(r.addr, r.sel),
      0.U,
      cp0Seq.map(e => e.index.U -> e.raw)
    )
  })

  val llbit = RegInit(false.B)

  when(except && !io.except(EXCEPT_ERET)) {
    llbit := false.B
  }.elsewhen(io.llSet) {
      llbit := true.B
    }
    .elsewhen(io.llClear) {
      llbit := false.B
    }
  io.llGet := llbit

  io.exceptionCP0.status := status.reg
  io.exceptionCP0.cause  := cause.reg
  io.exceptionCP0.ebase  := ebase.reg
  io.exceptionCP0.EPC    := epc.reg
  io.tlbCP0.index        := index.reg
  io.tlbCP0.random       := random.reg
  io.tlbCP0.pageMask     := pageMask.reg
  io.tlbCP0.entryHi      := entryHi.reg
  io.tlbCP0.entryLo0     := entryLo0.reg
  io.tlbCP0.entryLo1     := entryLo1.reg
  io.kseg0Uncached       := config0.reg.k0 === 2.U
}
