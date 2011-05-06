package jdos.cpu.instructions;

import jdos.cpu.CPU_Regs;
import jdos.hardware.Memory;

public class testGrp2 extends InstructionsTestCase {
    // 0xc0
    //GRP2 Eb,Ib
    // 0xc1
    //GRP2 Ew,Ib
    // 0xd0
    //GRP2 Eb,1
    // 0xd1
    //GRP2 Ew,1
    // 0xd2
    //GRP2 Eb,CL
    // 0xd3
    //GRP2 Ew,CL
    public void rolb(int op) {
        newInstruction((byte)op);
        pushIb((byte)0xc3);
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==2);

        newInstruction((byte)op);
        pushIb((byte)0xc3);
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==2); // make sure carry flag is ignored

        newInstruction((byte)op);
        pushIb((byte)0xc3);
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            CPU_Regs.reg_ebx.dword(0x8000);
        else
            CPU_Regs.reg_ebx.dword(0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);

        newInstruction((byte)op);
        pushIb((byte)0x0);
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x8000);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)1);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x80);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)1);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)0xc3);
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            if (op == 0xc1 || op==0xd3)
                CPU_Regs.reg_ebx.dword(0x8000);
            else
                CPU_Regs.reg_ebx.dword(0x80);
            decoder.call();
            assertTrue(CPU_Regs.reg_ebx.dword()==2);
            assertTrue(!CPU_Regs.get_CF());
        }
    }
    public void testEbIb_rol() {
        rolb(0xc0);
    }
    public void testEb1_rol() {
        rolb(0xd0);
    }
    public void testEbCl_rol() {
        rolb(0xd2);
    }
    public void testEwIb_rol() {
        rolb(0xc1);
    }
    public void testEw1_rol() {
        rolb(0xd1);
    }
    public void testEwCl_rol() {
        rolb(0xd3);
    }
    public void rorb(int op) {
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(1<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(1<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);
        assertTrue(!CPU_Regs.get_CF()); // make sure carry flag is ignored

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(1<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            assertTrue(CPU_Regs.reg_ebx.dword()==0x8000);
        else
            assertTrue(CPU_Regs.reg_ebx.dword()==0x80);
        assertTrue(CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(1<<3));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x01);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0x8000);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x01);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0x80);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)(0xc3+(1<<3)));
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            CPU_Regs.reg_ebx.dword(1);
            decoder.call();
            if (op == 0xc1 || op==0xd3)
                assertTrue(CPU_Regs.reg_ebx.dword()==0x4000);
            else
                assertTrue(CPU_Regs.reg_ebx.dword()==0x40);
            assertTrue(!CPU_Regs.get_CF());
        }
    }
    public void testEbIb_ror() {
        rorb(0xc0);
    }
    public void testEb1_ror() {
        rorb(0xd0);
    }
    public void testEbCl_ror() {
        rorb(0xd2);
    }
    public void testEwIb_ror() {
        rorb(0xc1);
    }
    public void testEw1_ror() {
        rorb(0xd1);
    }
    public void testEwCl_ror() {
        rorb(0xd2);
    }
    public void rclb(int op) {
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(2<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==2);
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(2<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==3);// make sure carry flag is used
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(2<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            CPU_Regs.reg_ebx.dword(0x8000);
        else
            CPU_Regs.reg_ebx.dword(0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==0);
        assertTrue(CPU_Regs.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
        newInstruction((byte)op);
        pushIb((byte)(2<<3));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x8000);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x80);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }
        assertTrue(CPU_Regs.get_CF());

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)(0xc3+(2<<3)));
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            if (op == 0xc1 || op==0xd3)
                CPU_Regs.reg_ebx.dword(0x8000);
            else
                CPU_Regs.reg_ebx.dword(0x80);
            decoder.call();
            assertTrue(CPU_Regs.reg_ebx.dword()==1);
            assertTrue(!CPU_Regs.get_CF());
        }
    }
    public void testEbIb_rcl() {
        rclb(0xc0);
    }
    public void testEb1_rcl() {
        rclb(0xd0);
    }
    public void testEbCl_rcl() {
        rclb(0xd2);
    }
    public void testEwIb_rcl() {
        rclb(0xc1);
    }
    public void testEw1_rcl() {
        rclb(0xd1);
    }
    public void testEwCl_rcl() {
        rclb(0xd3);
    }
    public void rcrb(int op) {
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(3<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(3<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            assertTrue(CPU_Regs.reg_ebx.dword()==0x8001);// make sure carry flag is used
        else
            assertTrue(CPU_Regs.reg_ebx.dword()==0x81);// make sure carry flag is used
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(3<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==0);
        assertTrue(CPU_Regs.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
        newInstruction((byte)op);
        pushIb((byte)(3<<3));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x01);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x01);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }
        assertTrue(CPU_Regs.get_CF());

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)(0xc3+(3<<3)));
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            CPU_Regs.reg_ebx.dword(1);
            decoder.call();
            if (op == 0xc1 || op==0xd3)
                assertTrue(CPU_Regs.reg_ebx.dword()==0x8000);
            else
                assertTrue(CPU_Regs.reg_ebx.dword()==0x80);
            assertTrue(!CPU_Regs.get_CF());
        }
    }
    public void testEbIb_rcr() {
        rcrb(0xc0);
    }
    public void testEb1_rcr() {
        rcrb(0xd0);
    }
    public void testEbCl_rcr() {
        rcrb(0xd2);
    }
    public void testEwIb_rcr() {
        rcrb(0xc1);
    }
    public void testEw1_rcr() {
        rcrb(0xd1);
    }
    public void testEwCl_rcr() {
        rcrb(0xd3);
    }
    public void shlb(int op) {
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(6<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==2);
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(6<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==2);// make sure carry flag is ignored
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(6<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            CPU_Regs.reg_ebx.dword(0x8000);
        else
            CPU_Regs.reg_ebx.dword(0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==0);
        assertTrue(CPU_Regs.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);  // just make sure this isn't used
        newInstruction((byte)op);
        pushIb((byte)(6<<3));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x8000);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x80);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }
        assertTrue(CPU_Regs.get_CF());

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)(0xc3+(6<<3)));
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            if (op == 0xc1 || op==0xd3)
                CPU_Regs.reg_ebx.dword(0x8000);
            else
                CPU_Regs.reg_ebx.dword(0x80);
            decoder.call();
            assertTrue(CPU_Regs.reg_ebx.dword()==0);
            assertTrue(!CPU_Regs.get_CF());
        }
    }
    public void testEbIb_shl() {
        shlb(0xc0);
    }
    public void testEb1_shl() {
        shlb(0xd0);
    }
    public void testEbCl_shl() {
        shlb(0xd2);
    }
    public void testEwIb_shl() {
        shlb(0xc1);
    }
    public void testEw1_shl() {
        shlb(0xd1);
    }
    public void testEwCl_shl() {
        shlb(0xd3);
    }
    public void shrb(int op) {
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(5<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(5<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);// make sure carry flag is ignored
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(5<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==0);
        assertTrue(CPU_Regs.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);  // just make sure this isn't used
        newInstruction((byte)op);
        pushIb((byte)(5<<3));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x01);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x01);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }
        assertTrue(CPU_Regs.get_CF());

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)(0xc3+(5<<3)));
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            CPU_Regs.reg_ebx.dword(1);
            decoder.call();
            assertTrue(CPU_Regs.reg_ebx.dword()==0);  // no wrap
            assertTrue(!CPU_Regs.get_CF());
        }

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(5<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            CPU_Regs.reg_ebx.dword(0x8000);
        else
            CPU_Regs.reg_ebx.dword(0x80);
        decoder.call();
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            assertTrue(CPU_Regs.reg_ebx.dword()==0x4000);  // no sign extension
        else
            assertTrue(CPU_Regs.reg_ebx.dword()==0x40);  // no sign extension
        assertTrue(!CPU_Regs.get_CF());
    }
    public void testEbIb_shr() {
        shrb(0xc0);
    }
    public void testEb1_shr() {
        shrb(0xd0);
    }
    public void testEbCl_shr() {
        shrb(0xd2);
    }
    public void testEwIb_shr() {
        shrb(0xc1);
    }
    public void testEw1_shr() {
        shrb(0xd1);
    }
    public void testEwCl_shr() {
        shrb(0xd3);
    }
    public void sarb(int op) {
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(7<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(7<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==1);// make sure carry flag is ignored
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(0xc3+(7<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        CPU_Regs.reg_ebx.dword(1);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword()==0);
        assertTrue(CPU_Regs.get_CF());

        if (op==0xc0 || op==0xc1 || op==0xd2 || op==0xd3) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
            newInstruction((byte)op);
            pushIb((byte)(0xc3+(7<<3)));
            if (op==0xc0 || op==0xc1)
                pushIb((byte)2);
            if (op==0xd2 || op==0xd3)
                CPU_Regs.reg_ecx.low(2);
            CPU_Regs.reg_ebx.dword(1);
            decoder.call();
            assertTrue(CPU_Regs.reg_ebx.dword()==0);  // no wrap
            assertTrue(!CPU_Regs.get_CF());
        }

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, false);
        newInstruction((byte)op);
        pushIb((byte)(0xc3+(7<<3)));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            CPU_Regs.reg_ebx.dword(0x8000);
        else
            CPU_Regs.reg_ebx.dword(0x80);
        decoder.call();
        if (op == 0xc1 || op==0xd1 || op==0xd3)
            assertTrue(CPU_Regs.reg_ebx.dword()==0xC000);  // sign extension
        else
            assertTrue(CPU_Regs.reg_ebx.dword()==0xC0);  // sign extension
        assertTrue(!CPU_Regs.get_CF());

        newInstruction((byte)op);
        pushIb((byte)(7<<3));
        if (op==0xc0 || op==0xc1)
            pushIb((byte)1);
        if (op==0xd2 || op==0xd3)
            CPU_Regs.reg_ecx.low(1);
        if (op == 0xc1 || op==0xd1 || op==0xd3) {
            Memory.mem_writew(MEM_BASE_DS, 0x8000);
            Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
            Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);
            decoder.call();
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0xC000);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
            assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
            Memory.mem_writew(MEM_BASE_DS-2, 0);
            Memory.mem_writew(MEM_BASE_DS, 0);
            Memory.mem_writew(MEM_BASE_DS+2, 0);
        } else {
            Memory.mem_writeb(MEM_BASE_DS, 0x80);
            Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
            Memory.mem_writeb(MEM_BASE_DS+1,0xCD);
            decoder.call();
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0xC0);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
            assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
            Memory.mem_writeb(MEM_BASE_DS-1, 0);
            Memory.mem_writeb(MEM_BASE_DS, 0);
            Memory.mem_writeb(MEM_BASE_DS+1, 0);
        }
        assertTrue(!CPU_Regs.get_CF());
    }
    public void testEbIb_sar() {
        sarb(0xc0);
    }
    public void testEb1_sar() {
        sarb(0xd0);
    }
    public void testEbCl_sar() {
        sarb(0xd2);
    }
    public void testEwIb_sar() {
        sarb(0xc1);
    }
    public void testEw1_sar() {
        sarb(0xd1);
    }
    public void testEwCl_sar() {
        sarb(0xd3);
    }
}
