package cpu

import cpu.common.WriteMask

class CPUConfig(val build: Boolean, val memoryFile: String = "", val tlbSize: Int = 16, val compareRamDirectly: Boolean = false, val verification: Boolean = false) {
  WriteMask.tlbSize = tlbSize

  val branchPredictorAddrLen = 10
  val branchPredictorTableEntryNum = 64
//  require(build && memoryFile.isEmpty || !build && !memoryFile.isEmpty)
}

object CPUConfig{
  val Build = new CPUConfig(build = true, memoryFile = "", compareRamDirectly = false)
}
