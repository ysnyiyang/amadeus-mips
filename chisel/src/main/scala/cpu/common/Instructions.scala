// See README.md for license details.

package cpu.common

import chisel3._
import chisel3.util.BitPat

trait Instructions {
  // @formatter:off
  //                        |      |    |    |    |    |     |
  def ADD         = BitPat("b000000???????????????00000100000")
  def ADDI        = BitPat("b001000??????????????????????????")
  def ADDIU       = BitPat("b001001??????????????????????????")
  def ADDU        = BitPat("b000000???????????????00000100001")
  def AND         = BitPat("b000000???????????????00000100100")
  def ANDI        = BitPat("b001100??????????????????????????")
  def BEQ         = BitPat("b000100??????????????????????????")
  def BGEZ        = BitPat("b000001?????00001????????????????")
  def BGEZAL      = BitPat("b000001?????10001????????????????")
  def BGTZ        = BitPat("b000111?????00000????????????????")
  def BLEZ        = BitPat("b000110?????00000????????????????")
  def BLTZ        = BitPat("b000001?????00000????????????????")
  def BLTZAL      = BitPat("b000001?????10000????????????????")
  def BNE         = BitPat("b000101??????????????????????????")
  def BREAK       = BitPat("b000000????????????????????001101")
  def DIV         = BitPat("b000000??????????0000000000011010") // Divide Word
  def DIVU        = BitPat("b000000??????????0000000000011011") // Divide Unsigned Word
  def ERET        = BitPat("b01000010000000000000000000011000") // Exception Return
  def J           = BitPat("b000010??????????????????????????") // Jump
  def JAL         = BitPat("b000011??????????????????????????") // Jump and Link
  def JALR        = BitPat("b000000?????00000??????????001001") // Jump and Link Register
  def JR          = BitPat("b000000?????0000000000?????001000") // Jump Register
  def LB          = BitPat("b100000??????????????????????????") // Load Byte
  def LBU         = BitPat("b100100??????????????????????????") // Load Byte Unsigned
  def LH          = BitPat("b100001??????????????????????????") // Load Halfword
  def LHU         = BitPat("b100101??????????????????????????") // Load Halfword Unsigned
  def LUI         = BitPat("b00111100000?????????????????????") // Load Upper Immediate
  def LW          = BitPat("b100011??????????????????????????") // Load Word
  def MFC0        = BitPat("b01000000000??????????00000000???") // Move from Coprocessor 0
  def MFHI        = BitPat("b0000000000000000?????00000010000") // Move From HI Register
  def MFLO        = BitPat("b0000000000000000?????00000010010") // Move From LO Register
  def MOVN        = BitPat("b000000???????????????00000001011") // Move Conditional on Not Zero
  def MOVZ        = BitPat("b000000???????????????00000001010") // Move Conditional on Zero
  def MTC0        = BitPat("b01000000100??????????00000000???") // Move to Coprocessor 0
  def MTHI        = BitPat("b000000?????000000000000000010001") // Move to HI Register
  def MTLO        = BitPat("b000000?????000000000000000010011") // Move to LO Register
  def MUL         = BitPat("b011100???????????????00000000010") // Multiply Word to GPR
  def MULT        = BitPat("b000000??????????0000000000011000") // Multiply Word
  def MULTU       = BitPat("b000000??????????0000000000011001") // Multiply Unsigned Word
  def NOP         = BitPat("b00000000000000000000000000000000") // No Operation
  def NOR         = BitPat("b000000???????????????00000100111") // Not Or
  def OR          = BitPat("b000000???????????????00000100101") // Or
  def ORI         = BitPat("b001101??????????????????????????") // Or Immediate
  def SB          = BitPat("b101000??????????????????????????") // Store Byte
  def SH          = BitPat("b101001??????????????????????????") // Store Halfword
  def SLL         = BitPat("b00000000000???????????????000000") // Shift Word Left Logical
  def SLLV        = BitPat("b000000???????????????00000000100") // Shift Word Left Logical Variable
  def SLT         = BitPat("b000000???????????????00000101010") // Set on Less Than
  def SLTI        = BitPat("b001010??????????????????????????") // Set on Less Than Immediate
  def SLTIU       = BitPat("b001011??????????????????????????") // Set on less Than Immediate Unsigned
  def SLTU        = BitPat("b000000???????????????00000101011") // Set on less Than Unsigned
  def SRA         = BitPat("b00000000000???????????????000011") // Shift Word Right Arithmetic
  def SRAV        = BitPat("b000000???????????????00000000111") // Shift Word Right Arithmetic Variable
  def SRL         = BitPat("b00000000000???????????????000010") // Shift Word Right Logical
  def SRLV        = BitPat("b000000???????????????00000000110") // Shift Word Right Logical Variable
  def SUB         = BitPat("b000000???????????????00000100010") // Subtract Word
  def SUBU        = BitPat("b000000???????????????00000100011") // Subtract Unsigned Word
  def SW          = BitPat("b101011??????????????????????????") // Store Word
  def SYSCALL     = BitPat("b000000????????????????????001100") // System Call
  def TLBP        = BitPat("b01000010000000000000000000001000") // Probe TLB for Matching Entry
  def TLBR        = BitPat("b01000010000000000000000000000001") // Read Indexed TLB Entry
  def TLBWI       = BitPat("b01000010000000000000000000000010") // Write Indexed TLB Entry
  def TLBWR       = BitPat("b01000010000000000000000000000110") // Write Random TLB Entry
  def XOR         = BitPat("b000000???????????????00000100110") // Exclusive OR
  def XORI        = BitPat("b001110??????????????????????????") // Exclusive OR Immediate
  // @formatter:on

  // BitPat can't use VecInit and contains
  def isBranchInst(inst: UInt) = {
    require(inst.getWidth == 32)
    val bi = Seq(J, JAL, JR, JALR, BEQ, BNE, BGTZ, BLEZ, BGEZ, BGEZAL, BLTZ, BLTZAL)
    bi.foldLeft(false.B)((r, e) => r || (e === inst))
  }
}

trait unImpl {
  // @formatter:off
  // 未实现
  def WSBH        = BitPat("b01111100000??????????00010100000") // Word Swap Bytes Within Halfwords
  def WRPGPR      = BitPat("b01000001110??????????00000000000") // Write to GPR in Previous Shadow Set
  def WAIT        = BitPat("b0100001???????????????????100000") // Enter Standby Mode
  def TRUNC_W_fmt = BitPat("b010001?????00000??????????001101") // Floating Point Truncate to Word Fixed Point
  def TRUNC_L_fmt = BitPat("b010001?????00000??????????001001") // Floating Point Truncate to Long Fixed Point
  def TNEI        = BitPat("b000001?????01110????????????????") // Trap if Not Equal Immediate
  def TNE         = BitPat("b000000????????????????????110110") // Trap if Not Equal
  def TLTU        = BitPat("b000000????????????????????110011") // Trap if less Than Unsigned
  def TLTIU       = BitPat("b000001?????01011????????????????") // Trap if Less Than Immediate Unsigned
  def TLTI        = BitPat("b000001?????01010????????????????") // Trap if Less Than Immediate
  def TLT         = BitPat("b000000????????????????????110010") // Trap if Less Than
  def TGEU        = BitPat("b000000????????????????????110001") // Trap if Greater or Equal Unsigned
  def TGEIU       = BitPat("b000001?????01001????????????????") // Trap if Greater or Equal Immediate Unsigned
  def TGEI        = BitPat("b000001?????01000????????????????") // Trap if Greater or Equal Immediate
  def TGE         = BitPat("b000000????????????????????110000") // Trap if Greater or Equal
  def TEQI        = BitPat("b000001?????01100????????????????") // Trap if Equal Immediate
  def TEQ         = BitPat("b000000????????????????????110100") // Trap if Equal
  def SYNCI       = BitPat("b000001?????11111????????????????") // Synchronize Caches to Make Instruction Writes Effective
  def SYNC        = BitPat("b000000000000000000000?????001111") // To order loads and stores for shared memory
  def SWXC1       = BitPat("b010011???????????????00000001000") // Store Word Indexed from Floating Point
  def SWR         = BitPat("b10110???????????????????????????") // Store Word Right
  def SWL         = BitPat("b101010??????????????????????????") // Store Word Left
  def SWC2        = BitPat("b111010??????????????????????????") // Store Word from Coprocessor 2
  def SWC1        = BitPat("b111001??????????????????????????") // Store Word from Floating Point
  def SUXC1       = BitPat("b010011???????????????00000001101") // Store Doubleword Indexed Unaligned from Floating Point
  def SUB_fmt     = BitPat("b010001????????????????????000001") // Floating Point Subtract
  def SSNOP       = BitPat("b00000000000000000000000001000000") // Superscalar No Operation
  def SQRT_fmt    = BitPat("b010001?????00000??????????000100") // Floating Point Square Root
  def SEH         = BitPat("b01111100000??????????11000100000") // Sign-Extend Halfword
  def SEB         = BitPat("b01111100000??????????10000100000") // Sign-Extend Byte
  def SDXC1       = BitPat("b010011???????????????00000001001") // Store Doubleword Indexed from Floating Point
  def SDC2        = BitPat("b111110??????????????????????????") // Store Doubleword from Coprocessor 2
  def SDC1        = BitPat("b111101??????????????????????????") // Store Doubleword from Floating Point
  def SDBBP       = BitPat("b011100????????????????????111111") // Software Debug Breakpoint
  def SC          = BitPat("b111000??????????????????????????") // Store Conditional Word
  def RSQRT_fmt   = BitPat("b010001?????00000??????????010110") // Reciprocal Square Root Approximation
  def ROUND_W_fmt = BitPat("b010001?????00000??????????001100") // Floating Point Round to Word Fixed Point
  def ROUND_L_fmt = BitPat("b010001?????00000??????????001000") // Floating Point Round to Long Fixed Point
  def ROTRV       = BitPat("b000000???????????????00001000110") // Rotate Word Right Variable
  def ROTR        = BitPat("b00000000001???????????????000010") // Rotate Word Right
  def RECIP_fmt   = BitPat("b010001?????00000??????????010101") // Reciprocal Approximation
  def RDPGPR      = BitPat("b01000001010??????????00000000000") // Read GPR from Previous Shadow Set
  def RDHWR       = BitPat("b01111100000??????????00000111011") // Read Hardware Register
  def PUU_PS      = BitPat("b01000110110???????????????101111") // Pair Upper Upper
  def PUL_PS      = BitPat("b01000110110???????????????101110") // Pair Upper Lower
  def PREFX       = BitPat("b010011???????????????00000001111") // Prefetch Indexed
  def PREF        = BitPat("b110011??????????????????????????") // Prefetch
  def PLU_PS      = BitPat("b01000110110???????????????101101") // Pair Lower Upper
  def PLL_PS      = BitPat("b01000110110???????????????101100") // Pair Lower Lower
  def PAUSE       = BitPat("b00000000000000000000000101000000") // Wait for the LLBit to clear
  def NMSUB_fmt   = BitPat("b010011????????????????????111???") // Floating Point Negative Multiply Subtract
  def NMADD_fmt   = BitPat("b010011????????????????????110???") // Floating Point Negative Multiply Add
  def NEG_fmt     = BitPat("b010001?????00000??????????000111") // Floating Point Negate
  def MUL_fmt     = BitPat("b010001????????????????????000010") // Floating Point Multiply
  def MTHC2       = BitPat("b01001000111?????????????????????") // Move Word to High of Coprocessor 2 Register
  def MTHC1       = BitPat("b01000100111??????????00000000000") // Move Word to High Half of Floating Point Register
  def MTC2        = BitPat("b01001000100?????????????????????") // Move Word to Coprocessor 2
  def MTC1        = BitPat("b01000100100??????????00000000000") // Move Word to Floating point
  def MSUBU       = BitPat("b011100??????????0000000000000101") // Multiply and Subtract Unsigned Word to Hi, Lo
  def MSUB_fmt    = BitPat("b010011????????????????????101???") // Floating Point Multiply Subtract
  def MSUB        = BitPat("b011100??????????0000000000000100") // Multiply and Subtract Word to Hi, Lo
  def MOVZ_fmt    = BitPat("b010001????????????????????010010") // Floating Point Move Conditional on Zero
  def MOVT_fmt    = BitPat("b010001????????01??????????010001") // Floating Point Move Conditional on Floating Point True
  def MOVT        = BitPat("b000000????????01?????00000000001") // Move Conditional on Floating Point True
  def MOVN_fmt    = BitPat("b010001????????????????????010011") // Floating Point Move Conditional on Not Zero
  def MOVF_fmt    = BitPat("b010001????????00??????????010001") // Floating Point Move Conditional on Floating Point False
  def MOVF        = BitPat("b000000????????00?????00000000001") // Move Conditional on Floating Point False
  def MOV_fmt     = BitPat("b010001?????00000??????????000110") // Floating Point Move
  def MFHC2       = BitPat("b01001000011?????????????????????") // Move Word From High Half of Coprocessor 2 Register
  def MFHC1       = BitPat("b01000100011??????????00000000000") // Move Word From High Half of Floating Point Register
  def MFC2        = BitPat("b01001000000?????????????????????") // Move Word From Coprocessor 2
  def MFC1        = BitPat("b01000100000??????????00000000000") // Move Word from Floating Point
  def MADDU       = BitPat("b011100??????????0000000000000001") // Multiply and Add Unsigned Word to Hi, Lo
  def MADD_fmt    = BitPat("b010011????????????????????100???") // Floating Point Multiply Add
  def MADD        = BitPat("b011100??????????0000000000000000") // Multiply and Add Word to Hi, Lo
  def LWXC1       = BitPat("b010011??????????00000?????000000") // Load Word Indexed to Floating Point
  def LWR         = BitPat("b100110??????????????????????????") // Load Word Right
  def LWL         = BitPat("b100010??????????????????????????") // Load Word Left
  def LWC2        = BitPat("b110010??????????????????????????") // Load Word to Coprocessor 2
  def LWC1        = BitPat("b110001??????????????????????????") // Load Word to Floating Point
  def LUXC1       = BitPat("b010011??????????00000?????000101") // Load Doubleword Indexed Unaligned to Floating Point
  def LL          = BitPat("b110000??????????????????????????") // Load Linked Word
  def LDXC1       = BitPat("b010011??????????00000?????000001") // Load Doubleword Indexed to Floating Point
  def LDC2        = BitPat("b110110??????????????????????????") // Load Doubleword to Coprocessor 2
  def LDC1        = BitPat("b110101??????????????????????????") // Load Doubleword to Floating Point
  def JR_HB       = BitPat("b000000?????00000000001????001000") // Jump Register with Hazard Barrier
  def JALX        = BitPat("b011101??????????????????????????") // Jump and Link Exchange
  def JALR_HB     = BitPat("b000000?????00000?????1????001001") // Jump and Link Register with Hazard Barrier
  def INS         = BitPat("b011111????????????????????000100") // Insert Bit Field
  def FLOOR_W_fmt = BitPat("b010001?????00000??????????001111") // Floating Point Floor Convert to Word Fixed Point
  def FLOOR_L_fmt = BitPat("b010001?????00000??????????001011") // Floating Point Floor Convert to Long Fixed Point
  def EXT         = BitPat("b011111????????????????????000000") // Extract Bit Field
  def EI          = BitPat("b01000001011?????0110000000100000") // Enable Interrupts
  def EHB         = BitPat("b00000000000000000000000011000000") // Execution hazard Barrier
  def DIV_fmt     = BitPat("b010001????????????????????000011") // Floating Point Divide
  def DI          = BitPat("b01000001011?????0110000000000000") // Disable Interrupts
  def DERET       = BitPat("b01000010000000000000000000011111") // Debug Exception Return
  def CVT_W_fmt   = BitPat("b010001?????00000??????????100100") // Floating Point Convert to Word Fixed Point
  def CVT_S_PU    = BitPat("b0100011011000000??????????100000") // Floating Point Convert Pair Upper to Single Floating Point
  def CVT_S_PL    = BitPat("b0100011011000000??????????101000") // Floating Point Convert Pair Lower to Single Floating Point
  def CVT_S_fmt   = BitPat("b010001?????00000??????????100000") // Floating Point Convert to Single Floating Point
  def CVT_PS_S    = BitPat("b01000110000???????????????100110") // Floating Point Convert Pair to Paired Single
  def CVT_L_fmt   = BitPat("b010001?????00000??????????100101") // Floating Point Convert to Long Fixed Point
  def CVT_D_fmt   = BitPat("b010001?????00000??????????100001") // Floating Point Convert to Double Floating Point
  def CTC2        = BitPat("b01001000110?????????????????????") // Move Control Word to Coprocessor 2
  def CTC1        = BitPat("b01000100110??????????00000000000") // Move Control Word to Floating Point
  def COP2        = BitPat("b0100101?????????????????????????") // Coprocessor operation to Coprocessor 2
  def CLZ         = BitPat("b011100???????????????00000100000") // Count Leading Zeros in Word
  def CLO         = BitPat("b011100???????????????00000100001") // Count Leading Ones in Word
  def CFC2        = BitPat("b01001000010?????????????????????") // Move Control Word From Coprocessor 2
  def CFC1        = BitPat("b01000100010??????????00000000000") // Move Control Word From Floating Point
  def CEIL_W_fmt  = BitPat("b010001?????00000??????????001110") // Floating Point Ceiling Convert to Word Fixed Point
  def CEIL_L_fmt  = BitPat("b010001?????00000??????????001010") // Fixed Point Ceiling Convert to Long Fixed Point
  def CACHE       = BitPat("b101111??????????????????????????")
  def C_cond_fmt  = BitPat("b010001??????????????????0011????") // Floating Point Compare
  def BNEL        = BitPat("b010101??????????????????????????")
  def BLTZL       = BitPat("b000001?????00010????????????????")
  def BLTZALL     = BitPat("b000001?????10010????????????????")
  def BLEZL       = BitPat("b010110?????00000????????????????")
  def BGTZL       = BitPat("b010111?????00000????????????????")
  def BGEZL       = BitPat("b000001?????00011????????????????")
  def BGEZALL     = BitPat("b000001?????10011????????????????")
  def BEQL        = BitPat("b010100??????????????????????????")
  def BC2TL       = BitPat("b01001001000???11????????????????")
  def BC2T        = BitPat("b01001001000???01????????????????")
  def BC2FL       = BitPat("b01001001000???10????????????????")
  def BC2F        = BitPat("b01001001000???00????????????????")
  def BC1TL       = BitPat("b01000101000???11????????????????")
  def BC1T        = BitPat("b01000101000???01????????????????")
  def BC1FL       = BitPat("b01000101000???10????????????????")
  def BC1F        = BitPat("b01000101000???00????????????????")
  def ALNV_PS     = BitPat("b010011????????????????????011110")
  def ABS_fmt     = BitPat("b010001?????00000??????????000101")
  // @formatter:on
}
