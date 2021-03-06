// See README.md for license details.

package cpu.core.bundles.stages

import chisel3._
import cpu.core.Constants._
import cpu.core.bundles.{CPBundle, CacheOp, HILOValidBundle, WriteBundle}

class ExeMemBundle extends Bundle {
  val write       = new WriteBundle
  val instType    = UInt(instTypeLen.W)
  val operation   = UInt(opLen.W)
  val cp0         = new CPBundle
  val hilo        = new HILOValidBundle
  val inDelaySlot = Bool()
  val except      = Vec(exceptAmount, Bool())
  val pc          = UInt(addrLen.W)
  val memAddr     = UInt(addrLen.W)
  val memData     = UInt(dataLen.W)
  val cacheOp     = new CacheOp
  val instValid   = Bool()
}
