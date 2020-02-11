package cpu
import cpu.memory._
import cpu.components._
import chisel3._
import chisel3.util._
import java.io.File

import cpu.singleCycle.SingleCycleCPU
/**
  * This class configures all of the dinocpus. It takes parameters for the type of CPU model
  * (e.g., single-cycle, five-cycle, etc.), and the memories to hook up.
  */
class CPUConfig
{
  /** The type of CPU to elaborate */
  var cpuType = "single-cycle"

  /** The type of branch predictor to use */
//  var branchPredictor = "always-not-taken"
  /** Number of bits for the saturating counters */
//  var saturatingCounterBits = 2
  /** Number of entries in the branch predictor table */
//  var branchPredTableEntries = 32

  /** The memory file location */
  var memFile = "test"
  /** The noncombinational memory latency */
//  var memLatency = 5
  /** The port types **/
  var memPortType = "combinational-port"
  /** The backing memory type */
  var memType = "combinational"
  val config = "phoenix"
  /**
    * Returns the CPU that we will be elaborating
    *
    * @return A CPU to elaborate.
    */
  def getCPU(): BaseCPU = {
    implicit val conf = this
    cpuType match {
      case "single-cycle" => new SingleCycleCPU
//      case "pipelined" => new PipelinedCPU
//      case "pipelined-bp" => new PipelinedCPUBP
      case _ => throw new IllegalArgumentException("Must specify known CPU model")
    }
  }

  //TODO: branch predictor
//  def getBranchPredictor: BaseBranchPredictor = {
//    implicit val conf = this
//    branchPredictor match {
//      case "always-taken"     => new AlwaysTakenPredictor
//      case "always-not-taken" => new AlwaysNotTakenPredictor
//      case "local"            => new LocalPredictor
//      case "global"           => new GlobalHistoryPredictor
//      case _ => throw new IllegalArgumentException("Must specify known branch predictor")
//    }
//  }

  /**
    * Create a memory with data from a file
    *
    * @param minSize is the minimum size for the memory. If the binary file is
    *        smaller than this, create a memory that is this size.
    * @return [[BaseDualPortedMemory]] object
    */
  def getNewMem(minSize: Int = 1 << 16): BaseDualPortedMemory = {
    val f = new File(memFile)
    if (f.length == 0) {
      println("WARNING: No file will be loaded for data memory")
    }

    memType match {
      case "combinational"     => new DualPortedCombinMemory (minSize, memFile)
//      non-comb prohibited, as they are not possible on FPGAs
//      case "non-combinational" => new DualPortedNonCombinMemory (minSize, memFile, memLatency)
      case _ => throw new IllegalArgumentException("Must specify known backing memory type")
    }
  }

  /**
    * Create an instruction memory port
    *
    * @return [[BaseIMemPort]] object
    */
  def getIMemPort(): BaseIMemPort = {
    val f = new File(memFile)
    if (f.length == 0) {
      println("WARNING: No file will be loaded for data memory")
    }

    memPortType match {
      case "combinational-port"     => new ICombinMemPort
//      non-comb prohibited, as they are not possible on FPGAs
//      case "non-combinational-port" => new INonCombinMemPort
//       case "non-combinational-cache" => new ICache
      case _ => throw new IllegalArgumentException("Must specify known instruction memory port type")
    }
  }

  /**
    * Create a data memory port
    *
    * @return [[BaseDMemPort]] object
    */
  def getDMemPort(): BaseDMemPort = {
    val f = new File(memFile)
    if (f.length == 0) {
      println("WARNING: No file will be loaded for data memory")
    }

    memPortType match {
      case "combinational-port"     => new DCombinMemPort
//      case "non-combinational-port" => new DNonCombinMemPort
      // case "non-combinational-cache" => new DCache
      case _ => throw new IllegalArgumentException("Must specify known data memory port type")
    }
  }
}
