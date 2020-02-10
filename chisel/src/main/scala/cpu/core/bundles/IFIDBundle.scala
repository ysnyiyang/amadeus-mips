// See README.md for license details.

package cpu.core.bundles

import chisel3._
import cpu.core.Constants._

class IFIDBundle extends Bundle {
  val pc = UInt(addressLen.W)
  val inst = UInt(dataLen.W)
  val instFetchExcept = Bool()
}
