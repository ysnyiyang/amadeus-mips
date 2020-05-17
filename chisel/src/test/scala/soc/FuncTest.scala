package soc

import chisel3.iotesters.{ChiselFlatSpec, Driver}

class FuncNoVcdTest extends ChiselFlatSpec {
  behavior.of("func_test")
  val funcFile = "./src/test/resources/loongson/func/inst_ram.coe"
  implicit val tcfg = new TestConfig(trace = true, performanceMonitorEnable = true)

  it should "use verilator without vcd file with performance metrics enabled" in {
    Driver.execute(
      Array("--backend-name", "verilator", "--generate-vcd-output", "off"),
      () => new SocLiteTop(simulation = false, memFile = funcFile, performanceMonitorEnable = true)
    ) { c =>
      new SocLiteTopUnitTester(c)
    } should be(true)
  }
}

class FuncWithVcdTest extends ChiselFlatSpec {
  behavior.of("func_test")
  val funcFile = "./src/test/resources/loongson/func/inst_ram.coe"
  implicit val tcfg = new TestConfig(trace = true, vcdOn = true)

  it should "use verilator to generate vcd file" in {
    Driver.execute(
      Array("--backend-name", "verilator"),
      () => new SocLiteTop(simulation = false, memFile = funcFile)
    ) { c =>
      new SocLiteTopUnitTester(c)
    } should be(true)
  }
}
