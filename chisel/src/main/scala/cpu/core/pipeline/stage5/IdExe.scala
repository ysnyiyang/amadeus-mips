// See README.md for license details.

package cpu.core.pipeline.stage5

import chisel3._
import chisel3.util.MuxCase
import cpu.core.bundles.stage5.IdExeBundle
import cpu.core.components.Stage

class IdExe(stageId: Int = 2) extends Stage(stageId, new IdExeBundle) {
  val ioExt = IO(new Bundle {
    val nextInstInDelaySlot = Input(Bool())
    val inDelaySlot = Output(Bool()) // back to Decode
  })

  val inDelaySlot = RegInit(false.B)
  inDelaySlot := MuxCase(inDelaySlot,
    Array(
      io.flush -> false.B,
      stallEnd -> inDelaySlot,
      notStalled -> ioExt.nextInstInDelaySlot
    )
  )
  ioExt.inDelaySlot := inDelaySlot

}