// See README.md for license details.

package cpu.cache

import chisel3._
import shared.{CircularShifter, PseudoLRUMRU, PseudoLRUTree}
//import chisel3.util.{log2Ceil, Cat}
import chisel3.util._
import cpu.common.NiseSramReadIO
import cpu.common.DefaultConfig._
import cpu.performance.CachePerformanceMonitorIO
import shared.AXIIO
import shared.Constants._

//TODO: will removing the fill buffer hurt performance?
//TODO: discuss propagating the signal
//TODO: change into a read only interface
//TODO: clear the naming of parameters
/**
  * icache with an AXI interface
  * @param setAmount how many sets there are in the i-cache
  * @param wayAmount how many ways there are in each set of i-cache
  * @param bankAmount how many banks there are in the i-cache
  * @param performanceMonitorEnable whether to enable the performance metrics
  */
class ICacheAXIWrap(
  setAmount:                Int = 64,
  wayAmount:                Int = 4,
  bankAmount:               Int = 16,
  performanceMonitorEnable: Boolean = false
) extends Module {
  val io = IO(new Bundle {
    val axi = AXIIO.master()
    val rInst = Flipped(new NiseSramReadIO)
    val performanceMonitorIO = if (performanceMonitorEnable) Some(new CachePerformanceMonitorIO) else None
  })

  /**
    * |   tag     |   index     |   bankOffset      | 0.U(2.W)  |
    * | `tagLen`  | `indexLen`  | `log2(bankAmount)`|     2     |
    */
  //----------------------------------------------------------------
  //------------------set up the cache parameters-------------------
  //----------------------------------------------------------------
  val indexLen = log2Ceil(setAmount) // index宽度
  val bankSize = 32 / 8 // 每bank字节数
  val blockSize = bankAmount * bankSize // 每块字节数
  val tagLen = 32 - indexLen - log2Ceil(blockSize) // tag宽度

  //------------------------------------------------------------------------------------
  //------------------check if the generator parameters meeet requirements--------------
  //------------------------------------------------------------------------------------
  require(bankAmount <= 16 && bankAmount >= 1, s"bank amount is $bankAmount! Need between 1 and 16")
  require(setAmount % 2 == 0, "setAmount of the cache must be a power of 2")
  require(blockSize % 4 == 0, "the block size of the cache ( in number of bytes ) must be 4 aligned")

  //-------------------------------------------------------------------------------
  //--------------------set up the states and register of FSM----------------------
  //-------------------------------------------------------------------------------
  val sIdle :: sWaitForAR :: sReFill :: Nil = Enum(3)
  val state = RegInit(sIdle)
  // cnt is a write mask to mark which to write to
  //  val cnt = RegInit(0.U(bankAmount.W))
  val writeMask = Module(new CircularShifter(bankAmount))

  // keep track of which way to write to
//  val replacedLineReg = Reg(UInt(1.W))
  val replacedLineReg = Reg(UInt(log2Ceil(wayAmount).W))
  // keep track of the index
  val indexReg = Reg(UInt(indexLen.W))
  // keep track of the tag
  val tagReg = Reg(UInt(tagLen.W))
  // keep track of the specific index of the bank
  val bankOffsetReg = Reg(UInt(log2Ceil(bankAmount).W))

  // registers for early restart
  val erBankOffsetReg = Reg(UInt(log2Ceil(bankAmount).W))

  // register for preserving the r data across rlast
  val rDataReg = RegNext(io.axi.r.bits.data)
  val rValidReg = RegInit(false.B)

  // records if the current set is full
  val isSetNotFull = WireDefault(false.B)
  val emptyPtr = WireDefault(0.U(log2Ceil(wayAmount).W))
  //-----------------------------------------------------------------------------
  //------------------assertions to check--------------------------------------
  //-----------------------------------------------------------------------------
  assert(!(io.rInst.valid && io.rInst.addr(1, 0).orR), "when address is not aligned, the valid signal must be false")

  //-------------------------------------------------------------------------------
  //-------------------- setup some constants to use----------------------------------
  //-------------------------------------------------------------------------------
  val addr = io.rInst.addr
  //TODO: check cached transaction
  val cachedTrans = true.B

  val tag = addr(dataLen - 1, dataLen - tagLen)
  val index = addr(dataLen - tagLen - 1, dataLen - tagLen - indexLen)
  val bankOffset = addr(log2Ceil(blockSize) - 1, log2Ceil(bankSize))

  //-------------------------------------------------------------------------------
  //--------------------set up the memory banks and wire them to their states------
  //-------------------------------------------------------------------------------
  /** valid(way)(index) */
  //TODO: change the order if it is not banked
  val valid = RegInit(VecInit(Seq.fill(wayAmount)(VecInit(Seq.fill(setAmount)(false.B)))))

  /** Write enable mask, we(way)(bank) */
  val we = Wire(Vec(wayAmount, Vec(bankAmount, Bool())))

  /** Tag write enable mask, tagWe(way) */
  val tagWe = Wire(Vec(wayAmount, Bool()))

  /** Data from every way, bankData(way)(bank) */
  val bankData = Wire(Vec(wayAmount, Vec(bankAmount, UInt(dataLen.W))))

  /** Data from every way tag, tagData(way) */
  val tagData = Wire(Vec(wayAmount, UInt(tagLen.W)))

  // LRU points to the least recently used item in each set
  // we can do this because it is only 2 way set associative now
  /** LRU(index) */
//  val LRU = RegInit(VecInit(Seq.fill(setAmount)(0.U(2.W))))
//TODO: is setAmount the number of sets
  val LRU = Module(new PseudoLRUTree(numOfWay = wayAmount, numOfSets = setAmount))
  val tagWire = Wire(UInt(tagLen.W))

  val indexWire = Wire(UInt(indexLen.W))
  //-----------------------------------------------------------------------------
  //------------------set up variables for this cycle----------------------------
  //-----------------------------------------------------------------------------
  tagWire := tag
  // check what state we are in
  //TODO: notice this does not precisely imply the time, as it state update happens
  // within the cycle
  val isIdle = state === sIdle
  val isWaitForAR = state === sWaitForAR
  val isReFill = state === sReFill

  // check if there is a hit and the line that got the hit if there is a hit
  // despite its name, this indicates a true hit
  val isHit = WireDefault(false.B)

  // check which is the lru line
  val lruLine = LRU.io.lruLine

  // check if there is a hit and determine the way of the hit
  val hitWay = Wire(UInt(log2Ceil(wayAmount).W))
  hitWay := 0.U

  // check across all ways in the desired set
  for (i <- 0 until wayAmount) {
    when(!valid(i)(index)) {
      isSetNotFull := true.B
      emptyPtr := i.U
    }
    when(valid(i)(index) && tagData(i) === tag) {
      hitWay := i.U
      isHit := true.B
    }
  }

  //-----------------------------------------------------------------------------
  //------------------initialize default IO--------------------------------
  //-----------------------------------------------------------------------------
  we := 0.U.asTypeOf(Vec(wayAmount, Vec(bankAmount, Bool())))
  tagWe := 0.U.asTypeOf(Vec(wayAmount, Bool()))
  // default io for write mask
  writeMask.io.initPosition.bits := 0.U
  writeMask.io.initPosition.valid := false.B
  writeMask.io.shiftEnable := false.B

  io.axi := DontCare

  io.axi.ar.bits.id := INST_ID
  io.axi.ar.bits.addr := Mux(
    cachedTrans,
    //    Cat(0.U(3.W), addr(28, 2 + log2Ceil(bankAmount)), 0.U((2 + log2Ceil(bankAmount)).W)),
    Cat(0.U(3.W), addr(28, 0)),
    virToPhy(addr)
  )
  io.axi.ar.bits.len := Mux(cachedTrans, (bankAmount - 1).U(4.W), 0.U(4.W)) // 16 or 1
  io.axi.ar.bits.size := "b010".U(3.W) // 4 Bytes
  io.axi.ar.bits.burst := "b10".U(2.W) // wrap burst

  /**
    * there was a design where if there is a miss, assert valid immediately
    * maybe it is not a good idea, need to know how this translate to verilog
    */
  io.axi.ar.valid := isWaitForAR

  //TODO: change the r ready signal
  //  io.axi.r.ready := state === sReFill
  io.axi.r.ready := true.B

  // these are hard wired as required by Loongson
  io.axi.ar.bits.lock := 0.U
  //TODO: can we utilize this?
  io.axi.ar.bits.cache := 0.U
  io.axi.ar.bits.prot := 0.U
  // hardcode r to always be ready
  //TODO: check in case of problem

  val instruction = Wire(UInt(dataLen.W))
  instruction := bankData(RegNext(hitWay))(RegNext(bankOffset))
  io.rInst.valid := cachedTrans && isIdle && isHit && io.rInst.enable
  io.rInst.data := instruction

  LRU.io.accessEnable := false.B
  LRU.io.accessWay := DontCare
  LRU.io.accessSet := index

  //-----------------------------------------------------------------------------
  //------------------transaction as functions-----------------------------------
  //-----------------------------------------------------------------------------
  def beginRTransaction: Unit = {
    state := sReFill
    writeMask.io.initPosition.valid := true.B
    writeMask.io.initPosition.bits := bankOffsetReg
    // the precise timing of this should happen at the first cycle of the r transaction
    // however, as it is put into the refill state ( because if you wait for R, then
    // you can't handle the first receive unless you put it into a reg, but that's kind
    // of ugly
    valid(replacedLineReg)(indexReg) := false.B
  }

  def beginARTransaction: Unit = {
    indexReg := index
    replacedLineReg := Mux(isSetNotFull, emptyPtr, lruLine)
    tagReg := tag
    bankOffsetReg := bankOffset
  }
  //-----------------------------------------------------------------------------
  //------------------fsm transformation-----------------------------------------
  //-----------------------------------------------------------------------------
  switch(state) {
    is(sIdle) {
      //TODO: a way to simplify nested when
      when(rValidReg) {
        rValidReg := false.B
        io.rInst.data := rDataReg
      }
      when(io.rInst.enable) {
        // enable already ensures that the address is aligned
        when(isHit) {
          LRU.io.accessSet := index
          LRU.io.accessEnable := true.B
          LRU.io.accessWay := hitWay
        }.otherwise {
          state := sWaitForAR
          beginARTransaction
          io.axi.ar.valid := true.B
          // if ar ready is asserted in the same cycle
          when(io.axi.ar.fire) {
            beginRTransaction
          }
        }
      }
    }
    is(sWaitForAR) {
      when(io.axi.ar.fire) {
        beginRTransaction
      }
    }
    is(sReFill) {
      assert(io.axi.r.bits.id === INST_ID, "r id is not supposed to be different from i-cache id")
      assert(io.axi.r.bits.resp === 0.U, "the response should always be okay")

      // with every successful transaction, increment the bank offset register to reflect the new value
      when(io.axi.r.fire) {
        writeMask.io.shiftEnable := true.B
        // write to each bank consequently
        we(replacedLineReg) := writeMask.io.vector.asBools()
        // TODO: this only works when the bank offset reg does not overflow, so it "happens" to work when there are 16 banks
        // TODO: implement a proper bank offset register
        bankOffsetReg := bankOffsetReg + 1.U

//        io.rInst.valid := (bankOffset === bankOffsetReg) && (tag === tagReg) && (index === indexReg) && io.rInst.enable
//        io.rInst.data := RegNext(io.axi.r.bits.data)

        // update the states of cache during last write
        when(io.axi.r.bits.last) {
          //        assert(writeMask.io.vector(15) === true.B, "the write mask's MSB should be 1 when the fill finishes")
          valid(replacedLineReg)(indexReg) := true.B
          state := sIdle
          for (i <- 0 until wayAmount) {
            tagWe(i.U) := (i.U === replacedLineReg)
          }
          tagWire := tagReg
        }
        val reFillHit = (bankOffset === bankOffsetReg) && (tag === tagReg) && (index === indexReg) && io.rInst.enable
        io.rInst.valid := reFillHit
        io.rInst.data := rDataReg
        rValidReg := reFillHit
      }
    }
  }

  indexWire := Mux(state === sIdle, index, indexReg)
  val instBanks = for {
    i <- 0 until wayAmount
    j <- 0 until bankAmount
  } yield {
    val bank = Module(new SinglePortBank(setAmount, dataLen, syncRead = true))
    bank.io.we := we(i)(j)
    bank.io.en.get := true.B
    bank.io.addr := indexWire
    bank.io.inData := io.axi.r.bits.data
    bankData(i)(j) := bank.io.outData
  }
  val tagBanks = for (i <- 0 until wayAmount) yield {
    val bank = Module(new SinglePortBank(setAmount, tagLen, syncRead = false))
    bank.io.we := tagWe(i)
    bank.io.addr := indexWire
    bank.io.inData := tagReg
    tagData(i) := bank.io.outData
  }

  //-----------------------------------------------------------------------------
  //------------------optional io for performance metrics--------------------------------------
  //-----------------------------------------------------------------------------
  if (performanceMonitorEnable) {
    // performance counter to count how many misses and how many hits are there
    val missCycleCounter = RegInit(0.U(32.W))
    val hitCycleCounter = RegInit(0.U(32.W))
    when(io.rInst.valid) {
      hitCycleCounter := hitCycleCounter + 1.U
    }.otherwise {
      missCycleCounter := missCycleCounter + 1.U
    }
    val performanceMonitorWire = Wire(new CachePerformanceMonitorIO)
    performanceMonitorWire.hitCycles := hitCycleCounter
    performanceMonitorWire.missCycles := missCycleCounter
    io.performanceMonitorIO.get := performanceMonitorWire
  }

  assert(io.axi.r.ready === true.B, "r ready signal should always be high")

  /** just erase high 3 bits */
  def virToPhy(addr: UInt): UInt = {
    require(addr.getWidth == addrLen)
    Cat(0.U(3.W), addr(28, 0))
  }
}
