package soc

import axi.{AXIInterconnect, AXIInterconnectConfig}
import chisel3._
import confreg.Confreg
import cpu.{CPUConfig, CPUTop}
import ram.AXIRam
import shared.{DebugBundle, GPIO}
import uart.{MyUART, Uart}

class SocUpTop(implicit cfg: SocUpTopConfig) extends Module {
  val io = IO(new Bundle {
    val gp    = new GPIO
    val debug = Output(new DebugBundle)
//    val uart  = new UartIO
  })

  implicit val cpuConfig = new CPUConfig(build = false, memoryFile = cfg.memFile)

  val cpu = Module(new CPUTop())

  /** 1*5 */
  val axiInterconnect = Module(new AXIInterconnect(AXIInterconnectConfig.loongson_system()))

  val confreg = Module(new Confreg())
  val ddr3    = Module(new AXIRam(memFile = Some(cfg.memFile), addrLen = 32))
  val flash   = Module(new AXIRam(memFile = None, addrLen = 20))
  val uart    = Module(new MyUART())

  axiInterconnect.io.slaves(0)  <> cpu.io.axi
  axiInterconnect.io.masters(0) <> ddr3.io.axi
  axiInterconnect.io.masters(1) <> flash.io.axi
  axiInterconnect.io.masters(2) <> uart.io.axi
  axiInterconnect.io.masters(3) <> confreg.io.axi
  axiInterconnect.io.masters(4) := DontCare // MAC, unused
}
