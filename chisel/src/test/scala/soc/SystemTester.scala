package soc

import chisel3.iotesters.PeekPokeTester

class SystemTester(c: SocUpTop)(implicit cfg: SystemTestConfig) extends PeekPokeTester(c) {
  val nioWrapper: NIOWrapper = new SocketWrapper(9654)

  var pc:    BigInt = peek(c.io.debug(0).wbPC)
  var wen:   BigInt = peek(c.io.debug(0).wbRegFileWEn)
  var wnum:  BigInt = peek(c.io.debug(0).wbRegFileWNum)
  var wdata: BigInt = peek(c.io.debug(0).wbRegFileWData)

  def updateDebug(n: Int = 0) = {
    pc    = peek(c.io.debug(n).wbPC)
    wen   = peek(c.io.debug(n).wbRegFileWEn)
    wnum  = peek(c.io.debug(n).wbRegFileWNum)
    wdata = peek(c.io.debug(n).wbRegFileWData)
  }

  // start run
  var iCount = 0
  var cCount = 1 // avoid divide by 0
  var lastCycle = cCount
  var lastBlockingCycle = cCount
  var lastDebugCycle = cCount
  var lastDebugInfo = ""

  init()
  run()

  def init(): Unit = {
    updateDebug()
    iCount = 0
    cCount = 1
    lastCycle = cCount
    lastBlockingCycle = cCount
    lastDebugCycle = cCount
    lastDebugInfo = ""
  }

  def run(): Unit = {
    while(cCount < cfg.cycleLimit){
      for(i <- 0 until 2) {
        if (pc != 0) {
          iCount += 1
          if (cfg.needBlockingIO) if (!BlockingIO()) return
          lastDebugInfo = debugInfo
          lastDebugCycle = cCount
        }
        updateDebug(1)
      }
      cCount += 1
      if(cCount - lastDebugCycle > 500){
        log(lastDebugInfo)
        return
      }
      if(cCount - lastCycle >= 50000){
        log(lastDebugInfo)
        lastCycle = cCount
      }
      update(1)
    }
  }

  def BlockingIO(): Boolean = {
    if(cCount -  lastBlockingCycle >= cfg.blockingIOCycle) {
      log(lastDebugInfo)
      val t = scala.io.StdIn.readInt()
      if(t == -1) return false
      lastBlockingCycle = cCount
    }
    true
  }

  def update(n: Int): Unit = {
    updateDebug()
    writeToUartSimu()
    ReadFromUartSimu()
    step(1)
  }

  def ReadFromUartSimu(): Unit = {
    if(peek(c.io.uart.out.valid) != 0) {
      val ch = peek(c.io.uart.out.bits)
      log(s"read ${ch.toChar.toString} from uart")
      nioWrapper.send(Seq(ch.toByte))
    }
  }

  def writeToUartSimu(): Unit = {
    if(peek(c.io.uart.in.ready) != 0) {
      nioWrapper.next match {
        case Some(b) =>
          poke(c.io.uart.in.valid, true)
          poke(c.io.uart.in.bits, b)
          log(s"write ${b.toChar.toString} to uart")
        case None =>
          poke(c.io.uart.in.valid, false)
      }
    } else {
      poke(c.io.uart.in.valid, false)
    }
  }

  def debugInfo = f"pc--0x$pc%08x, wen--${(wen != 0).toString}%5s, wnum--$wnum%02x, wdata--0x$wdata%08x, cycle-- $cCount%09d"

  def log(msg: String): Unit = {
    println(scala.Console.CYAN + "Log: " + msg + scala.Console.RESET)
  }
}
