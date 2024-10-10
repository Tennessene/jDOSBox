package jdos.cpu.instructions;

import jdos.cpu.*;
import jdos.cpu.core_dynamic.Compiler;
import jdos.cpu.core_dynamic.DecodeBlock;
import jdos.cpu.core_dynamic.Mod;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.hardware.RAM;
import jdos.misc.setup.*;
import junit.framework.TestCase;

abstract public class InstructionsTestCase extends TestCase {
    // static CPU.CPU_Decoder decoder = Core_normal.CPU_Core_Normal_Run;
    //static CPU.CPU_Decoder decoder = Core_dynamic.CPU_Core_Dynamic_Run;
    static CPU.CPU_Decoder decoder = Core_switch.CPU_Core_Switch_Run;
    protected int cseip = 0x10000;
    protected final static int MEM_BASE_DS = 0x2000;
    protected final static int MEM_BASE_SS = 0x3000;
    protected void newInstruction(int op) {
        clearReg();
        CPU.lastint = 0;
        //cseip = 0x10000;
        CPU_Regs.reg_eip = cseip-0x10000;
        CPU.CPU_Cycles = 1;
        if (op>0x200) {
            RAM.writebs(cseip++, (byte) 0x66);
        }
        RAM.writebs(cseip++, (byte)op);
        CPU_Regs.SegSet16ES(0);
        CPU_Regs.SegSet16CS(0x1000);
        CPU_Regs.SegSet16SS(MEM_BASE_SS >> 4);
        CPU_Regs.SegSet16DS(MEM_BASE_DS >> 4);
        CPU_Regs.SegSet16FS(0);
        CPU_Regs.SegSet16GS(0);
    }
    protected void nop() {
        pushIb((byte)0x90);
    }
    protected void pushIb(byte ib) {
        RAM.writebs(cseip++, ib);
    }
    protected void pushIw(short iw) {
        RAM.writebs(cseip++, (byte)(iw));
	    RAM.writebs(cseip++, (byte)(iw >> 8));
    }
    protected void pushId(int id) {
        RAM.writebs(cseip++, (byte)id);
	    RAM.writebs(cseip++, (byte)(id >> 8));
        RAM.writebs(cseip++, (byte)(id >> 16));
        RAM.writebs(cseip++, (byte)(id >> 24));
    }
    protected void runReg(byte op, long ed, long gd, boolean gdResult, long result) {
        int rm = 0xC0;
        newInstruction(op);
        pushIb((byte)rm);
        Mod.gd(rm).dword(gd);
        Mod.ed(rm).dword(ed);
        decoder.call();
        if (gdResult)
            assertTrue(Mod.gd(rm).dword==(int)result);
        else
            assertTrue(Mod.ed(rm).dword==(int)result);
    }

    protected void runRegs(byte op, long ed, long gd, boolean gbResult, long result, long result2) {
        for (int rm=192;rm<256;rm++) {
            newInstruction(op);
            pushIb((byte)rm);
            Mod.gd(rm).dword(gd);
            Mod.ed(rm).dword(ed);
            decoder.call();
            if (Mod.gd(rm) == Mod.ed(rm)) {
                if (gbResult)
                    assertTrue("rm = "+rm, Mod.gd(rm).dword==(int)result2);
                else
                    assertTrue("rm = "+rm, Mod.ed(rm).dword==(int)result2);
            } else {
                if (gbResult)
                    assertTrue("rm = "+rm, Mod.gd(rm).dword==(int)result);
                else
                    assertTrue("rm = "+rm, Mod.ed(rm).dword==(int)result);
            }
        }
    }

    protected void runRegwi(byte op, int rm, int eb, int value, int result) {
        rm+=0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        pushIw((short)value);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.ew(rm).word(eb);
        decoder.call();
        assertTrue((short) Mod.ew(rm).word()==(short)result);

        CPU_Regs.flags = flags;
        rm-=0xC1;
        newInstruction(op);
        pushIb((byte)rm);
        pushIw((short)value);
        Memory.mem_writew(MEM_BASE_DS, eb);
        Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
        Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
        decoder.call();
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)result);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    protected void runRegwix(byte op, int rm, int eb, byte value, int result) {
        rm+=0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb(value);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.ew(rm).word(eb);
        decoder.call();
        assertTrue((short) Mod.ew(rm).word()==(short)result);

        CPU_Regs.flags = flags;
        rm-=0xC1;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb(value);
        Memory.mem_writew(MEM_BASE_DS, eb);
        Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
        Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
        decoder.call();
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)result);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    protected void runRegw(byte op, int ed, int gd, boolean gdResult, int result) {
        int rm = 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.gd(rm).dword(0xABCDEF12);
        Mod.gd(rm).word(gd);
        Mod.ed(rm).word(ed);
        decoder.call();
        if (gdResult)
            assertTrue((short) Mod.gd(rm).word()==(short)result);
        else
            assertTrue((short) Mod.ed(rm).word()==(short)result);

        CPU_Regs.flags = flags;
        
        newInstruction(op);
        pushIb((byte)0x00);
        Mod.gd(rm).word(gd);
        Memory.mem_writew(MEM_BASE_DS-2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS, ed);
        Memory.mem_writew(MEM_BASE_DS+2, 0xCDCD);
        decoder.call();
        if (gdResult)
            assertTrue((short) Mod.gd(rm).word()==(short)result);
        else
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)result);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2) == (short)0xCDCD);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2) == (short)0xCDCD);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    protected void runRegd(int op, int ed, int gd, boolean gdResult, int result) {
        int rm = 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        Mod.gd(rm).dword=gd;
        Mod.ed(rm).dword=ed;
        decoder.call();
        if (gdResult)
            assertTrue(Mod.gd(rm).dword==result);
        else
            assertTrue(Mod.ed(rm).dword==result);

        CPU_Regs.flags = flags;

        newInstruction(op);
        pushIb((byte)0x00);
        Mod.gd(rm).dword=gd;
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS, ed);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
        decoder.call();
        if (gdResult)
            assertTrue(Mod.gd(rm).dword==result);
        else
            assertTrue(Memory.mem_readd(MEM_BASE_DS)==result);
        assertTrue(Memory.mem_readd(MEM_BASE_DS - 4) == 0xCDCDCDCD);
        assertTrue(Memory.mem_readd(MEM_BASE_DS + 4) == 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }

    protected void runRegwFlagsi(byte op, int rm, int eb, int value, int state) {
        rm += 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        pushIw((short) value);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.ew(rm).word(eb);
        decoder.call();
        assertFlags(state);
        CPU_Regs.flags = flags;
        rm-=0xC1;
        newInstruction(op);
        pushIb((byte)rm);
        pushIw((short) value);
        Memory.mem_writew(MEM_BASE_DS, eb);
        Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
        Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
        decoder.call();
        assertFlags(state);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)eb);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    protected void runRegwFlagsix(byte op, int rm, int eb, byte value, int state) {
        rm += 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb(value);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.ew(rm).word(eb);
        decoder.call();
        assertFlags(state);
        CPU_Regs.flags = flags;
        rm-=0xC1;
        newInstruction(op);
        pushIb((byte)rm);
        pushIw((short)value);
        Memory.mem_writew(MEM_BASE_DS, eb);
        Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
        Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
        decoder.call();
        assertFlags(state);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)eb);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    protected void runRegwFlags(byte op, int ed, int gd, int state) {
        int rm = 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.gd(rm).dword(0xABCDEF12);
        Mod.gd(rm).word(gd);
        Mod.ed(rm).word(ed);
        decoder.call();
        assertFlags(state);

        CPU_Regs.flags = flags;

        newInstruction(op);
        pushIb((byte) 0x00);
        Mod.gd(rm).word(gd);
        Memory.mem_writew(MEM_BASE_DS-2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS, ed);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCDCD);
        decoder.call();
        assertFlags(state);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2) == (short)0xCDCD);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2) == (short)0xCDCD);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    protected void runRegswi(byte op, int rm, int eb, int value, int result) {
        for (int i=0;i<8;i++) {
            newInstruction(op);
            pushIb((byte)(0xc0+rm+i));
            pushIw((short)value);
            Mod.ed(rm+i).dword(0xABCDEF12);
            Mod.ew(rm+i).word(eb);
            decoder.call();
            assertTrue("rm = "+(0xC0+rm+i), (short) Mod.ew(rm+i).word()==(short)result);
        }
    }

    protected void runRegswix(byte op, int rm, int eb, byte value, int result) {
        for (int i=0;i<8;i++) {
            newInstruction(op);
            pushIb((byte)(0xc0+rm+i));
            pushIb(value);
            Mod.ed(rm+i).dword(0xABCDEF12);
            Mod.ew(rm+i).word(eb);
            decoder.call();
            assertTrue("rm = "+(0xC0+rm+i), (short) Mod.ew(rm+i).word()==(short)result);
        }
    }

    protected void runRegsw(byte op, int ew, int gw, boolean gbResult, int result, int result2) {
        for (int rm=192;rm<256;rm++) {
            newInstruction(op);
            pushIb((byte)rm);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.gd(rm).dword(0xABCDEF12);
            Mod.gw(rm).word(gw);
            Mod.ew(rm).word(ew);
            decoder.call();
            if (Mod.gw(rm) == Mod.ew(rm)) {
                if (gbResult)
                    assertTrue("rm = "+rm, (short) Mod.gw(rm).word()==(short)result2);
                else
                    assertTrue("rm = "+rm, (short) Mod.ew(rm).word()==(short)result2);
            } else {
                if (gbResult)
                    assertTrue("rm = "+rm, (short) Mod.gw(rm).word()==(short)result);
                else
                    assertTrue("rm = "+rm, (short) Mod.ew(rm).word()==(short)result);
            }
        }
    }

    protected void runRegsd(int op, int ed, int gd, boolean gdResult, int result, int result2) {
        for (int rm=192;rm<256;rm++) {
            newInstruction(op);
            pushIb((byte) rm);
            Mod.gw(rm).dword=gd;
            Mod.ew(rm).dword=ed;
            decoder.call();
            if (Mod.gd(rm) == Mod.ed(rm)) {
                if (gdResult)
                    assertTrue("rm = "+rm, Mod.gd(rm).dword==result2);
                else
                    assertTrue("rm = "+rm, Mod.ed(rm).dword==result2);
            } else {
                if (gdResult)
                    assertTrue("rm = "+rm, Mod.gd(rm).dword==result);
                else
                    assertTrue("rm = "+rm, Mod.ed(rm).dword==result);
            }
        }
    }

    protected void runRegswFlagsLessi(byte op, int rm, int eb, int value) {
        for (int i=0;i<8;i++) {
            newInstruction(op);
            pushIb((byte)(0xC0+rm+i));
            pushIw((short)value);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.ew(rm).word(eb);
            decoder.call();
            assertTrue("rm = "+rm, Flags.TFLG_L());
        }
    }

    protected void runRegswFlagsLessix(byte op, int rm, int eb, byte value) {
        for (int i=0;i<8;i++) {
            newInstruction(op);
            pushIb((byte)(0xC0+rm+i));
            pushIb(value);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.ew(rm).word(eb);
            decoder.call();
            assertTrue("rm = "+rm, Flags.TFLG_L());
        }
    }

    protected void runRegswFlagsLess(byte op, int ew, int gw) {
        for (int rm=192;rm<256;rm++) {
            newInstruction(op);
            pushIb((byte)rm);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.gd(rm).dword(0xABCDEF12);
            Mod.gw(rm).word(gw);
            Mod.ew(rm).word(ew);
            decoder.call();
            if (Mod.gb(rm) == Mod.eb(rm)) {
                 assertTrue("rm = "+rm, Flags.TFLG_Z());
            } else {
                 assertTrue("rm = "+rm, Flags.TFLG_L());
            }
        }
    }

    protected void runRegb(byte op, int eb, int gb, boolean gbResult, int result) {
        int rm = 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.gd(rm).dword(0xABCDEF12);
        Mod.gb(rm).set8((short)gb);
        Mod.eb(rm).set8((short)eb);
        decoder.call();
        if (gbResult)
            assertTrue((byte) Mod.gb(rm).get8()==(byte)result);
        else
            assertTrue((byte) Mod.eb(rm).get8()==(byte)result);

        CPU_Regs.flags = flags;

        newInstruction(op);
        pushIb((byte)0x00);
        Mod.gb(rm).set8((short)gb);
        Memory.mem_writeb(MEM_BASE_DS, eb);
        Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
        Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
        decoder.call();
        if (gbResult)
            assertTrue((byte) Mod.gb(rm).get8()==(byte)result);
        else
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)result);            
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS+1, 0);
    }

     protected void runRegbi(byte op, int rm, int eb, int value, int result) {
        rm+=0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb((byte)value);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.eb(rm).set8((short)eb);
        decoder.call();
        assertTrue((byte) Mod.eb(rm).get8()==(byte)result);

        CPU_Regs.flags = flags;
        rm-=0xC1;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb((byte)value);
        Memory.mem_writeb(MEM_BASE_DS, eb);
        Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
        Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
        decoder.call();
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)result);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS+1, 0);
    }

    protected void assertFlags(int state) {
        if (state<0)
            assertTrue(Flags.TFLG_L());
        else if (state>0)
            assertTrue(Flags.TFLG_NLE());
         else
            assertTrue(Flags.TFLG_Z());
    }
    protected void runRegbFlags(byte op, int eb, int gb, int state) {
        int rm = 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        Mod.gb(rm).set8((short)gb);
        Mod.eb(rm).set8((short)eb);
        decoder.call();
        assertFlags(state);
        CPU_Regs.flags = flags;

        newInstruction(op);
        pushIb((byte)0x00);
        Mod.gd(rm).dword(0xABCDEF12);
        Mod.gb(rm).set8((short)gb);
        Memory.mem_writeb(MEM_BASE_DS, eb);
        Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
        Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
        decoder.call();
        assertFlags(state);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)eb);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS+1, 0);
    }

    protected void runRegbFlagsi(byte op, int rm, int eb, int value, int state) {
        rm += 0xC1;
        int flags = CPU_Regs.flags;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb((byte)value);
        Mod.ed(rm).dword(0xABCDEF12);
        Mod.eb(rm).set8((short)eb);
        decoder.call();
        assertFlags(state);
        CPU_Regs.flags = flags;
        rm-=0xC1;
        newInstruction(op);
        pushIb((byte)rm);
        pushIb((byte)value);
        Memory.mem_writeb(MEM_BASE_DS, eb);
        Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
        Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
        decoder.call();
        assertFlags(state);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)eb);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS+1, 0);
    }

    protected void runRegsb(byte op, int eb, int gb, boolean gbResult, int result, int result2) {
        for (int rm=192;rm<256;rm++) {
            newInstruction(op);
            pushIb((byte)rm);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.gd(rm).dword(0xABCDEF12);
            Mod.gb(rm).set8((short)gb);
            Mod.eb(rm).set8((short)eb);
            decoder.call();
            if (Mod.gb(rm) == Mod.eb(rm)) {
                if (gbResult)
                    assertTrue("rm = "+rm, (byte) Mod.gb(rm).get8()==(byte)result2);
                else
                    assertTrue("rm = "+rm, (byte) Mod.eb(rm).get8()==(byte)result2);
            } else {
                if (gbResult)
                    assertTrue("rm = "+rm, (byte) Mod.gb(rm).get8()==(byte)result);
                else
                    assertTrue("rm = "+rm, (byte) Mod.eb(rm).get8()==(byte)result);
            }
        }
    }

    protected void runRegsbi(byte op, byte rm, int eb, byte value, int result) {
        for (int i=0;i<8;i++) {
            newInstruction(op);
            pushIb((byte)(0xc0+rm+i));
            pushIb(value);
            Mod.ed(rm+i).dword(0xABCDEF12);
            Mod.eb(rm+i).set8((short)eb);
            decoder.call();
            assertTrue("rm = "+(0xC0+rm+i), (byte) Mod.eb(rm+i).get8()==(byte)result);
        }
    }

    protected void runRegsbFlagsLess(byte op, int eb, int gb) {
        for (int rm=192;rm<256;rm++) {
            newInstruction(op);
            pushIb((byte)rm);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.gd(rm).dword(0xABCDEF12);
            Mod.gb(rm).set8((short)gb);
            Mod.eb(rm).set8((short)eb);
            decoder.call();
            if (Mod.gb(rm) == Mod.eb(rm)) {
                 assertTrue("rm = "+rm, Flags.TFLG_Z());
            } else {
                 assertTrue("rm = "+rm, Flags.TFLG_L());
            }
        }
    }

    protected void runRegsbFlagsLessi(byte op, int rm, int eb, int value) {
        for (int i=0;i<8;i++) {
            newInstruction(op);
            pushIb((byte)(0xC0+rm+i));
            pushIb((byte)value);
            Mod.ed(rm).dword(0xABCDEF12);
            Mod.eb(rm).set8((short)eb);
            decoder.call();
            assertTrue("rm = "+rm, Flags.TFLG_L());
        }
    }

    protected void clearReg() {
        CPU_Regs.reg_eax.dword(0);
        CPU_Regs.reg_ecx.dword(0);
        CPU_Regs.reg_edx.dword(0);
        CPU_Regs.reg_ebx.dword(0);
        CPU_Regs.reg_esp.dword(0);
        CPU_Regs.reg_ebp.dword(0);
        CPU_Regs.reg_esi.dword(0);
        CPU_Regs.reg_edi.dword(0);
    }
    Section_prop dosbox_prop = new Section_prop("dosbox");
    Section_prop cpu_prop = new Section_prop("cpu");

    protected void setUp() throws java.lang.Exception  {
        super.setUp();

        CPU.initialize();

        Compiler.alwayUseFastVersion = true;
        Compiler.processorCount = 0;
        Compiler.thowException = true;
        //Compiler.ENABLED = true;

        Core_dynamic.instruction_count = 1;
        Core_switch.instruction_count = 1;
        Core_dynamic.CPU_Core_Dynamic_Cache_Init(true);

        String[] cores = new String[] { "auto", "dynamic", "normal", "simple"};
        Prop_string Pstring = cpu_prop.Add_string("core", Property.Changeable.WhenIdle,"auto");
        Pstring.Set_values(cores);
        Pstring.Set_help("CPU Core used in emulation. auto will switch to dynamic if available and appropriate.");

        String[] cputype_values = { "auto", "386", "386_slow", "486_slow", "pentium_slow", "386_prefetch"};
        Pstring = cpu_prop.Add_string("cputype",Property.Changeable.Always,"auto");
        Pstring.Set_values(cputype_values);

        Prop_multival_remain Pmulti_remain = cpu_prop.Add_multiremain("cycles",Property.Changeable.Always," ");
        String[] cyclest = { "auto","fixed","max","%u"};
        Pstring = Pmulti_remain.GetSection().Add_string("type",Property.Changeable.Always,"auto");
        Pmulti_remain.SetValue("auto");
        Pstring.Set_values(cyclest);

        Pstring = Pmulti_remain.GetSection().Add_string("parameters",Property.Changeable.Always,"");

        Prop_int Pint = cpu_prop.Add_int("cycleup",Property.Changeable.Always,10);
        Pint.SetMinMax(1,1000000);

        Pint = cpu_prop.Add_int("cycledown",Property.Changeable.Always,20);
        Pint.SetMinMax(1,1000000);

        dosbox_prop.Add_int("memsize", Property.Changeable.OnlyAtStart, 4);

        Paging.PAGING_Init.call(dosbox_prop);
        Memory.MEM_Init.call(dosbox_prop);
        CPU.CPU_Init.call(cpu_prop);
        Pic.PIC_Init.call(null);
        clearReg();
        CPU.cpu.code.big = false;
        CPU.cpu.pmode = false;
        CPU_Regs.flags = 0;
        DecodeBlock.compileThreshold = 1;
        Compiler.min_block_size = 1;
    }
    protected void tearDown() throws java.lang.Exception {
        dosbox_prop.ExecuteDestroy(true);
        cpu_prop.ExecuteDestroy(true);
        super.tearDown();
    }
}
