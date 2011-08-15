package jdos.cpu.instructions;

import jdos.fpu.FPU;
import jdos.hardware.Memory;
import jdos.util.MicroDouble;

public class testFPU extends InstructionsTestCase {
    protected void setUp() throws java.lang.Exception  {
        super.setUp();
        FPU.FPU_Init.call(null);
    }
    private void pushF(int f) {
        Memory.mem_writed(MEM_BASE_DS, f);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
    }
    private void pushD(long f) {
        Memory.mem_writed(MEM_BASE_DS, (int)f);
        Memory.mem_writed(MEM_BASE_DS+4, (int)(f>>>32));
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 8, 0xCDCDCDCD);
    }
    private byte rm(boolean ea, int group, int sub) {
        int result = (group & 7) << 3 | (sub & 7);
        if (!ea)
            result |= 0xC0;
        return (byte)result;
    }
    private boolean isC0() {
        return (FPU.fpu.sw & 0x0100) != 0;
    }
    private boolean isC1() {
        return (FPU.fpu.sw & 0x0200) != 0;
    }
    private boolean isC2() {
        return (FPU.fpu.sw & 0x0400) != 0;
    }
    private boolean isC3() {
        return (FPU.fpu.sw & 0x4000) != 0;
    }
    // 0xd8
    // FPU ESC 0

    // FADD ST,STi
    public void testFADD() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd8);
        pushIb(rm(false, 0, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x407629999B333333l); // 354.6000015258789
    }

    // FMUL ST,STi
    public void testFMUL() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd8);
        pushIb(rm(false, 1, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40D8EC6B8B799999l); // 25521.680387878416
    }

    // FCOM STi
    public void testFCOM() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd8);
        pushIb(rm(false, 2, 1));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC2());
        assertTrue(!isC3());
        assertTrue(FPU.fpu.top==6);

        fld64(0x406FC66666666666l); // 254.2
        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd8);
        pushIb(rm(false, 2, 1));
        decoder.call();
        assertTrue(isC0());
        assertTrue(!isC2());
        assertTrue(!isC3());

        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd8);
        pushIb(rm(false, 2, 0));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC2());
        assertTrue(isC3());
    }

    // FCOMP STi
    public void testFCOMP() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd8);
        pushIb(rm(false, 3, 1));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC2());
        assertTrue(!isC3());
        assertTrue(FPU.fpu.top==7);
    }

    // FSUB ST,STi
    public void testFSUB() {
        fld64(0x406FC66666666666l); // 254.2
        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd8);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0xC063399996666666l); // -153.79999847412108
    }

    // FSUBR ST,STi
    public void testFSUBR() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd8);
        pushIb(rm(false, 5, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0xC063399996666666l); // -153.79999847412108
    }

    // FDIV ST,STi
    public void testFDIV() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd8);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x400441465AB53884l); // 2.531872471480769
    }

    // FDIVR ST,STi
    public void testFDIVR() {
        fld64(0x406FC66666666666l); // 254.2
        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd8);
        pushIb(rm(false, 7, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x400441465AB53884l); // 2.531872471480769
    }

    // 0xd9
    // FPU ESC 1

    // FLD STi
    public void testFST() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd9);
        pushIb(rm(false, 0, 1));
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x406FC66666666666l);
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40591999A0000000l);
        assertTrue(FPU.fpu.regs[FPU.fpu.top+2].d == 0x40591999A0000000l);
    }

    // FXCH STi
    public void testFXCH() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd9);
        pushIb(rm(false, 1, 1));
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x406FC66666666666l);
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40591999A0000000l);
        assertTrue(FPU.fpu.regs[FPU.fpu.top+1].d == 0x406FC66666666666l);
    }

    // FNOP
    public void testFNOP() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd9);
        pushIb(rm(false, 2, 0));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top+1].d == 0x40591999A0000000l);
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x406FC66666666666l);
    }

    // FSTP STi
    public void testFSTP() {
        fld64(0x40591999A0000000l); // 100.4
        fld64(0x406FC66666666666l); // 254.2
        newInstruction(0xd9);
        pushIb(rm(false, 3, 2));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40591999A0000000l); // verify it popped the stack
        assertTrue(FPU.fpu.regs[(FPU.fpu.top+1)&7].d == 0x406FC66666666666l);
    }

    // FCHS
    public void testFCHS() {
        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd9);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0xC0591999A0000000l);
    }

    // FABS
    public void testFABS() {
        fld64(0xC0591999A0000000l); // 1100.4
        newInstruction(0xd9);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40591999A0000000l);
    }

    // FTST
    public void testFTST() {
        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC2());
        assertTrue(!isC3());

        fld64(MicroDouble.ZERO);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC2());
        assertTrue(isC3());
    }

    // FXAM
    public void testFXAM() {
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTrue(isC0());
        assertTrue(!isC1());
        assertTrue(isC3());

        fld64(0x40591999A0000000l); // 100.4
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC1());
        assertTrue(isC2());
        assertTrue(!isC3());

        fld64(0xC0591999A0000000l); // -100.4
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(isC1());
        assertTrue(isC2());
        assertTrue(!isC3());

        fld64(MicroDouble.ZERO); // -100.4
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTrue(!isC0());
        assertTrue(!isC1());
        assertTrue(!isC2());
        assertTrue(isC3());
    }

    // FLD1
    public void testFLD1() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 0));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.ONE);
    }

    // FLDL2T
    public void testFLDL2T() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == FPU.L2T);
    }

    // FLDL2E
    public void testFLDL2E() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 2));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.INV_LN2);
    }

    // FLDPI
    public void testFLDPI() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 3));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.PI);
    }

    // FLDLG2
    public void testFLDLG2() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == FPU.LG2);
    }

    // FLDLN2
    public void testFLDLN2() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 5));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.LN2);
    }

    // FLDZ
    public void testFLDZ() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 6));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.ZERO);
    }

    // F2XM1   2^X - 1
    public void testF2XM1() {
        fld64(MicroDouble.TEN);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x408FF80000000000l); // 1024
    }

    // FYL2X
    // ST1*log2(ST0)
    public void testFYL2X() {
        fld64(MicroDouble.TEN);
        fld64(0x4059400000000000l); // 101
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x4050A5415E8CAD34l); // 66.582114827517960
    }

    // FPTAN
    public void testFPTAN() {
        fld64(0x405639999999999Al); // 88.9 rads
        newInstruction(0xd9);
        pushIb(rm(false, 6, 2));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top+1].d == 0x3FF5B29F9BDEB657l); // 1.3561092461271949
    }

    // FPATAN
    public void testFPATAN() {
        fld64(0x405639999999999Al); // 88.9 rads
        fld64(MicroDouble.TWO);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 3));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x3FF8C5D94BE94153l); // 1.5483029332427420
    }

    // FXTRACT
    public void testFXTRACT() {
        // :TODO:
    }

    // FPREM1
    // REM = ST -ST(1) * Q where Q is the integer nearest ST/ST(1).
    public void testFPREM1() {
        fld64(MicroDouble.FIVE);
        fld64(MicroDouble.EIGHT);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 5));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.negate(MicroDouble.TWO));
    }

    // FDECSTP
    public void testFDECSTP() {
        newInstruction(0xd9);
        pushIb(rm(false, 6, 6));
        assertTrue(FPU.fpu.top==0);
        decoder.call();
        assertTrue(FPU.fpu.top==7);
    }

    // FINCSTP
    public void testFINCSTP() {
        newInstruction(0xd9);
        pushIb(rm(false, 6, 7));
        assertTrue(FPU.fpu.top==0);
        decoder.call();
        assertTrue(FPU.fpu.top==1);
    }

    // FPREM
    public void testFPREM() {
        fld64(MicroDouble.FIVE);
        fld64(MicroDouble.EIGHT);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 0));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.THREE);
    }

    // FYL2XP1
    // ST0 = ST1 * log2( ST0 + 1.0 )
    public void testFYL2XP1() {
        fld64(MicroDouble.FIVE);
        fld64(MicroDouble.EIGHT); // 5 * log2(8+1) = 5 * 3.169925
        newInstruction(0xd9);
        pushIb(rm(false, 7, 1));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x402FB3020C87ACC3l); // 15.849625007211563
    }

    // FSQRT
    public void testFSQRT() {
        fld64(MicroDouble.TEN);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 2));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40094C583ADA5B53l); // 3.1622776601683795
    }

    // FSINCOS
    public void testFSINCOS() {
        fld64(MicroDouble.ZERO);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 3));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.ONE);
        assertTrue(FPU.fpu.regs[FPU.fpu.top-1].d == MicroDouble.ZERO);
    }

    // FRNDINT
    public void testFRNDINT() {
        fld64(MicroDouble.TWO);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.TWO);

        FPU.fpu.round = FPU.ROUND_Down;
        fld64(0x400599999999999Al); // 2.7
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.TWO);

        FPU.fpu.round = FPU.ROUND_Down;
        fld64(0x400199999999999Al); // 2.2
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.TWO);

        FPU.fpu.round = FPU.ROUND_Up;
        fld64(0x400599999999999Al); // 2.7
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.THREE);

        FPU.fpu.round = FPU.ROUND_Up;
        fld64(0x400199999999999Al); // 2.2
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.THREE);

        FPU.fpu.round = FPU.ROUND_Chop;
        fld64(0x400199999999999Al); // 2.2
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.TWO);

        FPU.fpu.round = FPU.ROUND_Nearest;
        fld64(0x400599999999999Al); // 2.7
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.THREE);

        FPU.fpu.round = FPU.ROUND_Nearest;
        fld64(0x400199999999999Al); // 2.2
        newInstruction(0xd9);
        pushIb(rm(false, 7, 4));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == MicroDouble.TWO);
    }

    // FSCALE
    // ST(0)*2^ST(1) where x is chopped
    public void testFSCALE() {
        fld64(0x4016CCCCCCCCCCCDl); // 5.7
        fld64(MicroDouble.TWO);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 5));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x4050000000000000l); // 64.0
    }

    // FSIN
    public void testFSIN() {
        fld64(0x4050400000000000l); // 65
        newInstruction(0xd9);
        pushIb(rm(false, 7, 6));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x3FEA75616B39C16Al); // 0.8268286794901034
    }

    // FCOS
    public void testFCOS() {
        fld64(0x4050400000000000l); // 65
        newInstruction(0xd9);
        pushIb(rm(false, 7, 7));
        decoder.call();
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0xBFE1FF9F38127868l); // -0.562453851238172
    }

    // FLD float
    public void testFLD() {
        fld(0x42C8CCCD); // 100.4f
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40591999A0000000l || FPU.fpu.regs[FPU.fpu.top].d == 0x405919999999999Al);
    }

    // FST float
    public void testFSTfloat() {
        fld64(0x4050400000000000l); // 65
        newInstruction(0xd9);
        pushIb(rm(true, 2, 0));
        decoder.call();
        assertTrue(Memory.mem_readd(MEM_BASE_DS)==0x42820000);
        assertTrue(FPU.fpu.top==7);
    }

    // FSTP float
    public void testFSTPfloat() {
        fld64(0x4050400000000000l); // 65
        newInstruction(0xd9);
        pushIb(rm(true, 3, 0));
        decoder.call();
        assertTrue(Memory.mem_readd(MEM_BASE_DS)==0x42820000);
        assertTrue(FPU.fpu.top==0);
    }

    // FLDENV
    public void testFLDENV() {
        // :TODO:
    }

    // FLDCW
    public void testFLDCW() {
        // :TODO:
    }

    // FSTENV
    public void testFSTENV() {
        // :TODO:
    }

    // FNSTCW
    public void testFNSTCW() {
        // :TODO:
    }

    private void fld(int f) {
        newInstruction(0xd9);
        pushIb(rm(true, 0, 0));
        pushF(f);
        decoder.call();
    }

    private void fld64(long d) {
        newInstruction(0xdd);
        pushIb(rm(true, 0, 0));
        pushD(d);
        decoder.call();
    }

    // 0xdd
    // FPU ESC 5

    // FLD double real
    public void testFLD64() {
        fld64(0x40591999A0000000l); // 100.4
        assertTrue(FPU.fpu.regs[FPU.fpu.top].d == 0x40591999A0000000l);
    }
}
