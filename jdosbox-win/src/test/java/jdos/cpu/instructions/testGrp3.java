package jdos.cpu.instructions;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Flags;
import jdos.cpu.core_dynamic.Compiler;
import jdos.hardware.Memory;

public class testGrp3 extends InstructionsTestCase {
    // 0xf6
    //GRP3 Eb(,Ib)
    // 0xf7
    //GRP3 Ew(,Iw)
    public void testEbIb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3));
        pushIb((byte)2);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0xf6);
        pushIb((byte) (0xc3));
        pushIb((byte)1);
        CPU_Regs.reg_ebx.dword(2);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0xf6);
        pushIb((byte) (0xc3));
        pushIw((short)0xFF01);
        CPU_Regs.reg_ebx.dword(0xFF02);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0xf6);
        pushIb((byte) (0x0));
        pushIb((byte)1);
        Memory.mem_writeb(MEM_BASE_DS, 0x02);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xFE);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xFE);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0xf6);
        pushIb((byte) (0x0));
        pushIb((byte)1);
        Memory.mem_writeb(MEM_BASE_DS, 0x01);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xFE);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xFE);
        decoder.call();
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
        assertTrue(!Flags.get_ZF());
    }

    public void testNotEb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3+(2<<3)));
        CPU_Regs.reg_ebx.dword(0xFFF0);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0xFF0F);

        newInstruction(0xf6);
        pushIb((byte)(2<<3));
        Memory.mem_writeb(MEM_BASE_DS, 0xAA);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xCD);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xCD);
        decoder.call();
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0x55);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
    }

    public void testNegEb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3+(3<<3)));
        CPU_Regs.reg_ebx.dword(0xFFF0);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0xFF10);

        newInstruction(0xf6);
        pushIb((byte)(3<<3));
        Memory.mem_writeb(MEM_BASE_DS, 0x02);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xCD);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xCD);
        decoder.call();
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS)==(byte)0xFE);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
    }

    public void testMulAlEb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3+(4<<3)));
        CPU_Regs.reg_eax.dword(0xFF03);
        CPU_Regs.reg_ebx.dword(0xFF02);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x0006);
        assertTrue(!Flags.get_OF());

        newInstruction(0xf6);
        pushIb((byte)(4<<3));
        CPU_Regs.reg_eax.dword(0xFF10);
        Memory.mem_writeb(MEM_BASE_DS, 0x10);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xCD);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x0100);
        assertTrue(Flags.get_OF());
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
    }

    public void testIMulAlEb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3+(5<<3)));
        CPU_Regs.reg_eax.dword(0x44FA); // -6 x 2
        CPU_Regs.reg_ebx.dword(0xFF02);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFFF4);
        assertTrue(!Flags.get_OF());

        newInstruction(0xf6);
        pushIb((byte)(5<<3));
        CPU_Regs.reg_eax.dword(0xFF10); // -16 * -16
        Memory.mem_writeb(MEM_BASE_DS, 0xE0);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xCD);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFE00);
        assertTrue(Flags.get_OF());
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
    }

    public void testDivAlEb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3+(6<<3)));
        CPU_Regs.reg_eax.dword(0xFFFF0031); // 49/5
        CPU_Regs.reg_ebx.dword(0xFF05);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF0409);

        newInstruction(0xf6);
        pushIb((byte)(6<<3));
        CPU_Regs.reg_eax.dword(0xFFFFABCD);
        Memory.mem_writeb(MEM_BASE_DS, 0xEF);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xCD);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF05B8);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
    }

    public void testIDivAlEb() {
        newInstruction(0xf6);
        pushIb((byte) (0xc3+(7<<3)));
        CPU_Regs.reg_eax.dword(0xFFFF0031); // 49 / -6
        CPU_Regs.reg_ebx.dword(0xFFFA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF01F8); // -8 rem 1

        newInstruction(0xf6);
        pushIb((byte)(7<<3));
        CPU_Regs.reg_eax.dword(0xFFFF007F);
        Memory.mem_writeb(MEM_BASE_DS, 0xFF);
        Memory.mem_writeb(MEM_BASE_DS - 1, 0xCD);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0xCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF0081);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS + 1, 0);
    }

    public void testEwIw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3));
        pushIw((short) 0x200);
        CPU_Regs.reg_ebx.dword(0x200);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0xf7);
        pushIb((byte) (0xc3));
        pushIw((short)0x100);
        CPU_Regs.reg_ebx.dword(0x200);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0xf7);
        pushIb((byte) (0x0));
        pushIw((short) 0x0100);
        Memory.mem_writew(MEM_BASE_DS, 0x0200);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xFEFE);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xFEFE);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0xf7);
        pushIb((byte) (0x0));
        pushIw((short) 0x0100);
        Memory.mem_writew(MEM_BASE_DS, 0x0100);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xFEFE);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xFEFE);
        decoder.call();
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
        assertTrue(!Flags.get_ZF());
    }

    public void testNotEw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3+(2<<3)));
        CPU_Regs.reg_ebx.dword(0xF0F0);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0x0F0F);

        newInstruction(0xf7);
        pushIb((byte)(2<<3));
        Memory.mem_writew(MEM_BASE_DS, 0xAAAA);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCDCD);
        decoder.call();
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0x5555);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS - 2)==(short)0xCDCD);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS + 2)==(short)0xCDCD);
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
    }

    public void testNegEw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3+(3<<3)));
        CPU_Regs.reg_ebx.dword(0xF000);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0x1000);

        newInstruction(0xf7);
        pushIb((byte)(3<<3));
        Memory.mem_writew(MEM_BASE_DS, 0x0200);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCDCD);
        decoder.call();
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS)==(short)0xFE00);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS - 2)==(short)0xCDCD);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS + 2)==(short)0xCDCD);
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
    }

    public void testMulAxEw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3+(4<<3)));
        CPU_Regs.reg_eax.dword(0x0300);
        CPU_Regs.reg_ebx.dword(0x0003);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x0900);
        assertTrue(!Flags.get_OF());

        newInstruction(0xf7);
        pushIb((byte) (4 << 3));
        CPU_Regs.reg_eax.dword(0x100);
        Memory.mem_writew(MEM_BASE_DS, 0x100);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword == 0x0000);
        assertTrue(CPU_Regs.reg_edx.dword==0x0001);
        assertTrue(Flags.get_OF());
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
    }

    public void testIMulAxEw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3+(5<<3)));
        CPU_Regs.reg_eax.dword(0xFA00); // -1536 x 2
        CPU_Regs.reg_ebx.dword(0x2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xF400);
        assertTrue(!Flags.get_OF());

        newInstruction(0xf7);
        pushIb((byte) (5 << 3));
        CPU_Regs.reg_eax.dword(0xF100); // -3840 * 3584 = -13762560
        Memory.mem_writew(MEM_BASE_DS, 0xE00);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword == 0x0000);
        assertTrue(CPU_Regs.reg_edx.dword==0xFF2E);
        assertTrue(Flags.get_OF());
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
    }

    public void testDivAxEw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3+(6<<3)));
        CPU_Regs.reg_eax.dword(0x3100); // 12544/5
        CPU_Regs.reg_ebx.dword(0x05);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x09CC);
        assertTrue(CPU_Regs.reg_edx.dword==4);

        newInstruction(0xf7);
        pushIb((byte)(6<<3));
        CPU_Regs.reg_eax.dword(0x8000);  // 2129920 / 239
        CPU_Regs.reg_edx.dword(0x0020);
        Memory.mem_writew(MEM_BASE_DS, 0xEF);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xCD);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword == 0x22CF);
        assertTrue(CPU_Regs.reg_edx.dword==0xBF);
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
    }

    public void testIDivAxEw() {
        newInstruction(0xf7);
        pushIb((byte) (0xc3+(7<<3)));
        CPU_Regs.reg_eax.dword(0x3100); // 12544 / -6
        CPU_Regs.reg_ebx.dword(0xFFFA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xF7D6);
        assertTrue(CPU_Regs.reg_edx.dword==0x04);

        newInstruction(0xf7);
        pushIb((byte)(7<<3));
        CPU_Regs.reg_eax.dword(0x007F);
        CPU_Regs.reg_edx.dword(0xFFFF);
        Memory.mem_writew(MEM_BASE_DS, 0xFFFC);
        Memory.mem_writew(MEM_BASE_DS - 2, 0xCD);
        Memory.mem_writew(MEM_BASE_DS + 2, 0xCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword == 0x3FE0);
        assertTrue(CPU_Regs.reg_edx.dword==0xFFFF);
        Memory.mem_writew(MEM_BASE_DS - 2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS + 2, 0);
    }

    public void testEdId() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3));
        pushId(0x20000);
        CPU_Regs.reg_ebx.dword(0x20000);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0x2f7);
        pushIb((byte) (0xc3));
        pushId(0x10000);
        CPU_Regs.reg_ebx.dword(0x20000);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x2f7);
        pushIb((byte) (0x0));
        pushId(0x0100);
        Memory.mem_writed(MEM_BASE_DS, 0x02000000);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xFEFEFEFE);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xFEFEFEFE);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x2f7);
        pushIb((byte) (0x0));
        pushId(0x01000000);
        Memory.mem_writed(MEM_BASE_DS, 0x01000000);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xFEFEFEFE);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xFEFEFEFE);
        decoder.call();
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
        assertTrue(!Flags.get_ZF());
    }

    public void testNotEd() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3+(2<<3)));
        CPU_Regs.reg_ebx.dword(0xF0F0F0F0);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0x0F0F0F0F);

        newInstruction(0x2f7);
        pushIb((byte)(2<<3));
        Memory.mem_writed(MEM_BASE_DS, 0xAAAAAAAA);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
        decoder.call();
        assertTrue(Memory.mem_readd(MEM_BASE_DS)==0x55555555);
        assertTrue(Memory.mem_readd(MEM_BASE_DS - 4)==0xCDCDCDCD);
        assertTrue(Memory.mem_readd(MEM_BASE_DS + 4)==0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }

    public void testNegEd() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3+(3<<3)));
        CPU_Regs.reg_ebx.dword(0xF0000000);
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0x10000000);

        newInstruction(0x2f7);
        pushIb((byte)(3<<3));
        Memory.mem_writed(MEM_BASE_DS, 0x02000000);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
        decoder.call();
        assertTrue(Memory.mem_readd(MEM_BASE_DS)==0xFE000000);
        assertTrue(Memory.mem_readd(MEM_BASE_DS - 4)==0xCDCDCDCD);
        assertTrue(Memory.mem_readd(MEM_BASE_DS + 4)==0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }

    public void testMulEaxEd() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3+(4<<3)));
        CPU_Regs.reg_eax.dword(0x00030000);
        CPU_Regs.reg_ebx.dword(0x00000003);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x00090000);
        assertTrue(!Flags.get_OF());

        newInstruction(0x2f7);
        pushIb((byte)(4<<3));
        CPU_Regs.reg_eax.dword(0x10000);
        Memory.mem_writed(MEM_BASE_DS, 0x10000);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x0000);
        assertTrue(CPU_Regs.reg_edx.dword==0x0001);
        if (!Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_OF());
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }

    public void testIMulEaxEd() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3+(5<<3)));
        CPU_Regs.reg_eax.dword(0xFA000000);
        CPU_Regs.reg_ebx.dword(0x2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xF4000000);
        assertTrue(!Flags.get_OF());

        newInstruction(0x2f7);
        pushIb((byte)(5<<3));
        CPU_Regs.reg_eax.dword(0xF1000000);
        Memory.mem_writed(MEM_BASE_DS, 0xE000000);
        Memory.mem_writed(MEM_BASE_DS - 2, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 2, 0xCDCDCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFD000000);
        assertTrue(CPU_Regs.reg_edx.dword==0x02F0F0F0);
        if (!Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_OF());
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }

    public void testDivEaxEd() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3+(6<<3)));
        CPU_Regs.reg_eax.dword(0x31000000);
        CPU_Regs.reg_ebx.dword(0x05);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x09CCCCCC);
        assertTrue(CPU_Regs.reg_edx.dword==4);

        newInstruction(0x2f7);
        pushIb((byte)(6<<3));
        CPU_Regs.reg_eax.dword(0x80000000);
        CPU_Regs.reg_edx.dword(0x00000020);
        Memory.mem_writed(MEM_BASE_DS, 0xEF000);
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x22CFC);
        assertTrue(CPU_Regs.reg_edx.dword==0xBC000);
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }

    public void testIDivEaxEd() {
        newInstruction(0x2f7);
        pushIb((byte) (0xc3+(7<<3)));
        CPU_Regs.reg_eax.dword(0x31000); // 200704 / -6 =  -33450 r 4
        CPU_Regs.reg_ebx.dword(0xFFFFFFFA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF7D56);
        assertTrue(CPU_Regs.reg_edx.dword==0x04);

        newInstruction(0x2f7);
        pushIb((byte)(7<<3));
        // -4286578689
        CPU_Regs.reg_eax.dword(0x007FFFFF);
        CPU_Regs.reg_edx.dword(0xFFFFFFFF);
        Memory.mem_writed(MEM_BASE_DS, 0xFFFFFFFC); // -4
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x3FE00000);
        assertTrue(CPU_Regs.reg_edx.dword==0xFFFFFFFF);
        Memory.mem_writed(MEM_BASE_DS - 4, 0);
        Memory.mem_writed(MEM_BASE_DS, 0);
        Memory.mem_writed(MEM_BASE_DS + 4, 0);
    }
}
