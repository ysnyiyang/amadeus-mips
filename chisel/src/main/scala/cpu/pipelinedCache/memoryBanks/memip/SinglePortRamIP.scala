package cpu.pipelinedCache.memoryBanks.memip

import chisel3._
import chisel3.util.log2Ceil

/**
  * single port ram is conceptually like a `Reg` because of the read first write mode
  * i.e. the data out is the old value
  *
  * @param dataWidth       : the width of data in bits
  * @param byteWriteWidth  : how many bits to write each bit in write mask (wea), should default to 8
  * @param addrWidth       : the request of the width to request all locations
  * @param numberOfLines   : how wide is the request (to cover all lines)
  * @param memoryPrimitive : should I use auto, block ram or distributed ram
  */
class SinglePortRamIP(
  dataWidth:       Int    = 32,
  byteWriteWidth:  Int    = 8,
  addrWidth:       Int,
  numberOfLines:   Int,
  memoryPrimitive: String = "block",
  latency:         Int    = 1
) extends BlackBox(
      Map(
        "ADDR_WIDTH_A"       -> addrWidth,
        "WRITE_DATA_WIDTH_A" -> dataWidth,
        "READ_DATA_WIDTH_A"  -> dataWidth,
        "BYTE_WRITE_WIDTH_A" -> byteWriteWidth,
        "READ_LATENCY_A"     -> 1,
        "MEMORY_SIZE"        -> numberOfLines * dataWidth,
        "MEMORY_PRIMITIVE"   -> memoryPrimitive,
        "WRITE_MODE_A"       -> "read_first"
      )
    ) {
  require(
    (latency == 0 && (memoryPrimitive == "auto" || memoryPrimitive == "distributed")) || (latency == 1 && (memoryPrimitive == "auto" || memoryPrimitive == "block"))
  )

  override def desiredName: String = "xpm_memory_spram"

  val io = IO(new Bundle {
    require(
      dataWidth - (dataWidth / byteWriteWidth) * byteWriteWidth == 0,
      "data width should be a multiple of byte write width"
    )
    require(
      List("auto", "block", "distributed", "ultra").contains(memoryPrimitive),
      "memory primitive should be auto, block ram, dist ram or ultra ram"
    )
    require(addrWidth <= 20, "request width should be 1 to 20")
    require(addrWidth == log2Ceil(numberOfLines), "request width should be log 2 of number of lines to request all")
    // clock and reset
    val clka = Input(Clock())
    val rsta = Input(Reset())

    val addra  = Input(UInt(addrWidth.W))
    val dina   = Input(UInt(dataWidth.W))
    val ena    = Input(Bool())
    val regcea = Input(Bool())
    val wea    = Input(UInt((dataWidth / byteWriteWidth).W))
    val douta  = Output(UInt(dataWidth.W))

  })
}
