package cpu

import cpu.testing.{CPUTestDriver, InstructionTests}

object singlestep {
  val helptext = "usage: singlestep <CPU type> <test directory> <test mem file>"

  val commands = """Help for the single stepper:
                   | Note: Registers print the value *stored* in that register. The wires print
                   | the *current* value on the wire for that cycle.
                   |
                   | Printing registers
                   | ------------------
                   | print reg <num>  : print the value in register
                   | print regs       : print values in all registers
                   | print pc         : print the address in the pc
                   | print inst [addr]: print the disassembly for the instruction at addr.
                   |                    If no addr provided then use the current pc.
                   |
                   | Printing module I/O (wires)
                   | ---------------------------
                   | dump all        : Show all modules and the values of their I/O
                   | dump list       : List the valid modules to dump
                   | dump [module]   : Show values of the I/O on a specific module
                   |
                   | Printing pipeline registers (pipelined CPU only)
                   | ------------------------------------------------
                   | print pipereg <name> : Print the values in the pipeline register with name <name>
                   | print piperegs       : Print the values in all of the pipeline registers
                   |
                   | Controlling the simulator
                   | -------------------------
                   | step [num]      : move forward this many cycles, default 1
                   |
                   | Other commands
                   | --------------
                   | ?               : print this help
                   | q               : quit
                   |""".stripMargin

  def main(args: Array[String]): Unit = {
    require(
      args.length >= 3,
      "Error: Expected at least three argument: cpu type, dir name and memFile name\n" + helptext
    )

    println(s"Running test ${args(1)}/${args(2)} on CPU design ${args(0)}")

    val test = InstructionTests.nameMap(args(1) + "/" + args(2))
    val cpuType = args(0)

    val driver = new CPUTestDriver(cpuType, args(1), args(2))
    driver.initRegs(test.initRegs)
    driver.initMemory(test.initMem)
    println(commands)
    var done = false
    while (!done) {
      val tokens = scala.io.StdIn.readLine("Single stepper> ").split(" ")
      if (tokens.length > 0) {
        tokens(0) match {
          case "?"       => println(commands)
          case "q" | "Q" => done = true
          case "step"    => if (!doStep(tokens, driver)) println(commands)
          case "print" => {
            if (tokens.length > 1) {
              if (!doPrint(tokens, driver)) println(commands)
            }
          }
          case "dump" => {
            if (tokens.length > 1) {
              if (!doDump(tokens, driver)) println(commands)
            }
          }
          case _ => println(commands)
        }
      }
    }
    if (driver.checkRegs(test.checkRegs) && driver.checkMemory(test.checkMem)) {
      println("Test passed!")
    } else {
      println("Test failed!")
    }
  }

  def doPrint(tokens: Array[String], driver: CPUTestDriver): Boolean = {
    tokens(1) match {
      case "reg" => {
        if (tokens.length == 3) {
          try {
            driver.printReg(tokens(2).toInt)
            true
          } catch {
            case e: NumberFormatException => false
          }
        } else {
          false
        }
      }
      case "regs" => {
        driver.printRegs()
        true
      }
      case "pipereg" => {
        driver.printPipeReg(tokens(2))
        true
      }
      case "piperegs" => {
        driver.printAllPipeRegs()
        true
      }
      case "pc" => {
        driver.printPC()
        true
      }
      case "inst" => {
        if (tokens.length == 2) {
          driver.printInst()
          true
        } else if (tokens.length == 3) {
          try {
            driver.printInst(tokens(2).toInt)
            true
          } catch {
            case e: NumberFormatException => false
          }
        } else {
          false
        }
      }
      case _ => false
    }
  }

  def doDump(tokens: Array[String], driver: CPUTestDriver): Boolean = {
    tokens(1) match {
      case "all" => {
        driver.dumpAllModules()
        true
      }
      case "list" => {
        driver.listModules()
        true
      }
      case _ => {
        driver.dumpModule(tokens(1))
        true
      }
    }
  }

  def doStep(tokens: Array[String], driver: CPUTestDriver): Boolean = {
    val cycles =
      try {
        if (tokens.length == 2) tokens(1).toInt else 1
      } catch {
        case e: NumberFormatException => 0
      }
    if (cycles > 0) {
      driver.step(cycles)
      true
    } else {
      false
    }
  }
}
