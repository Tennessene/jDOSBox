package jdos.cpu.instructions;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Flags;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.hardware.RAM;

public class testPrefix_none extends InstructionsTestCase{
    // 0x00
    // ADD Eb,Gb
    public void testAddEbGb() {  
        runRegsb((byte)0x00, 1, 2, false, 3, 2);
        runRegb((byte)0x00, 1, -2, false, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x00, 0x80, 0x80, false, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegb((byte)0x00, 0x80, 0x80, false, 0);
    }

    // 0x01
    // ADD Ew,Gw
    public void testAddEwGw() {  
        runRegsw((byte)0x01, 1001, 2, false, 1003, 2002);
        runRegw((byte)0x01, 1, -2, false, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x01, 0x8000, 0x8000, false, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegw((byte)0x01, 0x8000, 0x8000, false, 0);
    }

    // 0x02
    // ADD Gb,Eb
    public void testAddGbEb() {  
        runRegsb((byte)0x02, 1, 2, true, 3, 2);
        runRegb((byte)0x02, 1, -2, true, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x02, 0x80, 0x80, true, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegb((byte)0x02, 0x80, 0x80, true, 0);
    }

    // 0x03
    // ADD Gw,Ew
    public void testAddGwEw() {  
        runRegsw((byte)0x03, 1001, 2, true, 1003, 2002);
        runRegw((byte)0x03, 1, -2, true, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x03, 0x8000, 0x8000, true, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegw((byte)0x03, 0x8000, 0x8000, true, 0);
    }

    // 0x04
    // ADD AL,Ib
    public void testAddAlIb() {
        newInstruction(0x04);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==3);

        newInstruction(0x04);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)-2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==255);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x04);
        CPU_Regs.reg_eax.low(0x80);
        pushIb((byte)0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        newInstruction(0x04);
        CPU_Regs.reg_eax.low(0x80);
        pushIb((byte)0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);
    }

    // 0x05
    // ADD AX,Iw
     public void testAddAxIw() {
        newInstruction(0x05);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==3);

        newInstruction(0x05);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)-2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFFFF);
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF);
         if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x05);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);
        assertTrue(CPU_Regs.reg_eax.dword==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        newInstruction(0x05);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);
    }

    // 0x06
    //PUSH ES
    public void testPushES() {
        newInstruction(0x06);
        CPU_Regs.reg_esp.dword(0x100);
        CPU_Regs.reg_esVal.dword = 0x1ABCD;
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
    }

    // 0x07
    //POP ES
    public void testPopES() {
        newInstruction(0x07);
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x89EF);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(CPU_Regs.reg_esVal.dword==0x89EF);
    }

    // 0x08
    //OR Eb,Gb
    public void testOrEbGb() {
        runRegsb((byte)0x08, 1, 2, false, 3, 1);
        runRegb((byte)0x08, 0, 0, false, 0);
        runRegb((byte)0x08, 0xFF, 0, false, 0xFF);
        runRegb((byte)0x08, 0, 0xFF, false, 0xFF);
        runRegb((byte)0x08, 0xF0, 0x0F, false, 0xFF);
    }

    // 0x09
    //OR Ew,Gw
    public void testOrEwGw() {
        runRegsw((byte)0x09, 1, 2, false, 3, 1);
        runRegw((byte)0x09, 0, 0, false, 0);
        runRegw((byte)0x09, 0xFFFF, 0, false, 0xFFFF);
        runRegw((byte)0x09, 0, 0xFFFF, false, 0xFFFF);
        runRegw((byte)0x09, 0xFF00, 0x00FF, false, 0xFFFF);
    }

    // 0x0A
    //OR Gb,Eb
    public void testOrGbEb() {
        runRegsb((byte)0x0a, 1, 2, true, 3, 1);
        runRegb((byte)0x0a, 0, 0, true, 0);
        runRegb((byte)0x0a, 0xFF, 0, true, 0xFF);
        runRegb((byte)0x0a, 0, 0xFF, true, 0xFF);
        runRegb((byte)0x0a, 0xF0, 0x0F, true, 0xFF);
    }

    // 0x0B
    //OR Gw,Ew
    public void testOrGwEw() {
        runRegsw((byte)0x0b, 1, 2, true, 3, 1);
        runRegw((byte)0x0b, 0, 0, true, 0);
        runRegw((byte)0x0b, 0xFFFF, 0, true, 0xFFFF);
        runRegw((byte)0x0b, 0, 0xFFFF, true, 0xFFFF);
        runRegw((byte)0x0b, 0xFF00, 0x00FF, true, 0xFFFF);
    }

    // 0x0C
    //OR AL,Ib
    public void testOrdAlIb() {
        newInstruction(0x0C);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==3);

        newInstruction(0x0C);
        CPU_Regs.reg_eax.low(0);
        pushIb((byte)0);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);

        newInstruction(0x0c);
        CPU_Regs.reg_eax.low(0xF0);
        pushIb((byte)0x0F);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0xFF);
        assertTrue(CPU_Regs.reg_eax.high()==0);
    }

    // 0x0D
    //OR AX,Iw
    public void testOrAxIw() {
        newInstruction(0x0D);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==3);

        newInstruction(0x0D);
        CPU_Regs.reg_eax.word(0);
        pushIw((short)0);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);

        newInstruction(0x0D);
        CPU_Regs.reg_eax.word(0xF00F);
        pushIw((short)0x0FF0);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFFFF);
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF);
    }

    // 0x0E
    //PUSH CS
    public void testPushCS() {
        newInstruction(0x0E);
        CPU_Regs.reg_esp.dword(0x100);
        CPU_Regs.reg_csVal.dword = 0x1ABCD;
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
    }

    // 0x0F
    // 2 byte opcodes
    //
    // Tested with Prefix_0f

    // 0x10
    //ADC Eb,Gb
    public void testAdcEbGb() {
        runRegsb((byte)0x10, 1, 2, false, 3, 2);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x10, 192, 128, false, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegb((byte)0x10, 192, 128, false, 65);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x11
    //ADC Ew,Gw
    public void testAdcEwGw() {
        runRegsw((byte)0x11, 1, 2, false, 3, 2);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x11, 0xC000, 0x8000, false, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegw((byte)0x11, 0xC000, 0x8000, false, 0x4001);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x12
    //ADC Gb,Eb
    public void testAdcGbEb() {
        runRegsb((byte)0x12, 1, 2, true, 3, 2);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x12, 192, 128, true, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegb((byte)0x12, 192, 128, true, 65);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x13
    //ADC Gw,Ew
    public void testAdcGwEw() {
        runRegsw((byte)0x13, 1, 2, true, 3, 2);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x13, 0xC000, 0x8000, true, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegw((byte)0x13, 0xC000, 0x8000, true, 0x4001);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x14
    //ADC AL,Ib
    public void testAdcAlIb() {
        newInstruction(0x14);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==3);

        newInstruction(0x14);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)-2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==255);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x14);
        CPU_Regs.reg_eax.low(0x80);
        pushIb((byte)0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        newInstruction(0x14);
        CPU_Regs.reg_eax.low(0x80);
        pushIb((byte)0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==1);
        assertTrue(CPU_Regs.reg_eax.high()==0);
    }

    // 0x15
    //ADC AX,Iw
    public void testAdcAxIw() {
        newInstruction(0x15);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==3);

        newInstruction(0x15);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)-2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFFFF);
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x15);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);
        assertTrue(CPU_Regs.reg_eax.dword==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        newInstruction(0x15);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==1);
        assertTrue(CPU_Regs.reg_eax.dword==1);
    }

    // 0x16
    //PUSH SS
    public void testPushSS() {
        newInstruction(0x16);
        CPU_Regs.reg_esp.dword(0x100);
        CPU_Regs.reg_ssVal.dword = 0x1ABCD;
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        assertTrue(RAM.readd(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
    }

    // 0x17
    //POP SS
    public void testPopSS() {
        newInstruction(0x17);
        nop();
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x89EF);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(CPU_Regs.reg_ssVal.dword==0x89EF);
    }

    // 0x18
    //SBB Eb,Gb
    public void testSbbEbGb() {
        runRegsb((byte)0x18, 3, 2, false, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x18, 192, 128, false, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x18, 128, 192, false, -64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegb((byte)0x18, 128, 192, false, -65);  // test the -1 carry
    }

    // 0x19
    //SBB Ew,Gw
    public void testSbbEwGw() {
        runRegsw((byte)0x19, 3, 2, false, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x19, 0xC000, 0x8000, false, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x19, 0x8000, 0xC000, false, -0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegw((byte)0x19, 0x8000, 0xC000, false, -0x4001);  // test the -1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x1a
    //SBB Gb,Eb
    public void testSbbGbEb() {
        runRegsb((byte)0x1a, 2, 3, true, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x1a, 128, 192, true, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x1a, 192, 128, true, -64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegb((byte)0x1a, 192, 128, true, -65);  // test the -1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x1b
    //SBB Gw,Ew
    public void testSbbGwEw() {
        runRegsw((byte)0x1b, 2, 3, true, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x1b, 0x8000, 0xC000, true, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x1b, 0xC000, 0x8000, true, -0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegw((byte)0x1b, 0xC000, 0x8000, true, -0x4001);  // test the -1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x1c
    //SBB AL,Ib
    public void testSbbAlIb() {
        newInstruction(0x1c);
        CPU_Regs.reg_eax.low(3);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x1c);
        CPU_Regs.reg_eax.low(192);
        pushIb((byte)128);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==64);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x1c);
        CPU_Regs.reg_eax.low(128);
        pushIb((byte)192);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==192);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        newInstruction(0x1c);
        CPU_Regs.reg_eax.low(128);
        pushIb((byte)192);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==191); // test the -1 carry
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x1d
    //SBB AX,Iw
    public void testSbbAxIw() {
        newInstruction(0x1d);
        CPU_Regs.reg_eax.word(3);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x1d);
        CPU_Regs.reg_eax.word(0xC000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0x4000);
        assertTrue(CPU_Regs.reg_eax.dword==0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x1d);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0xC000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xC000);
        assertTrue(CPU_Regs.reg_eax.dword==0xC000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        newInstruction(0x1d);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0xC000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xBFFF);
        assertTrue(CPU_Regs.reg_eax.dword==0xBFFF);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x1E
    //PUSH DS
    public void testPushDS() {
        newInstruction(0x1E);
        CPU_Regs.reg_esp.dword(0x100);
        CPU_Regs.reg_dsVal.dword = 0x1ABCD;
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        assertTrue(RAM.readd(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
    }

    // 0x1F
    //POP DS
    public void testPopDS() {
        newInstruction(0x1F);
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x89EF);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(CPU_Regs.reg_dsVal.dword==0x89EF);
    }

    // 0x20
    // AND Eb,Gb
    public void testAndEbGb() {
        runRegsb((byte)0x20, 1, 2, false, 0, 1);
        runRegb((byte)0x20, 1, -1, false, 1);
        runRegb((byte)0x20, 0x0, 0x0, false, 0x0);
        runRegb((byte)0x20, 0x00, 0xFF, false, 0x0);
        runRegb((byte)0x20, 0xFF, 0xFF, false, 0xFF);
    }

    // 0x21
    // AND Ew,Gw
    public void testAndEwGw() {
        runRegsw((byte)0x21, 1, 2, false, 0, 1);
        runRegw((byte)0x21, 1, -1, false, 1);
        runRegw((byte)0x21, 0x0000, 0x0000, false, 0);
        runRegw((byte)0x21, 0x0000, 0xFFFF, false, 0);
        runRegw((byte)0x21, 0xFFFF, 0xFFFF, false, 0xFFFF);
    }

    // 0x22
    // AND Gb,Eb
    public void testAndGbEb() {
        runRegsb((byte)0x22, 1, 2, true, 0, 1);
        runRegb((byte)0x22, 1, -1, true, 1);
        runRegb((byte)0x22, 0x0, 0x0, true, 0x0);
        runRegb((byte)0x22, 0x00, 0xFF, true, 0x0);
        runRegb((byte)0x22, 0xFF, 0xFF, true, 0xFF);
    }

    // 0x23
    // AND Gw,Ew
    public void testAndGwEw() {
        runRegsw((byte)0x23, 1, 2, true, 0, 1);
        runRegw((byte)0x23, 1, -1, true, 1);
        runRegw((byte)0x23, 0x0000, 0x0000, true, 0);
        runRegw((byte)0x23, 0x0000, 0xFFFF, true, 0);
        runRegw((byte)0x23, 0xFFFF, 0xFFFF, true, 0xFFFF);
    }

    // 0x24
    // AND AL,Ib
    public void testAndAlIb() {
        newInstruction(0x24);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);

        newInstruction(0x24);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)-1);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==1);
        assertTrue(CPU_Regs.reg_eax.high()==0);

        newInstruction(0x24);
        CPU_Regs.reg_eax.low(0x0);
        pushIb((byte)0x0);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);
        assertTrue(CPU_Regs.reg_eax.high()==0);

        newInstruction(0x24);
        CPU_Regs.reg_eax.low(0x0);
        pushIb((byte)0xFF);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);

        newInstruction(0x24);
        CPU_Regs.reg_eax.low(0xFF);
        pushIb((byte)0xFF);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0xFF);
    }

    // 0x25
    // AND AX,Iw
     public void testAndAxIw() {
        newInstruction(0x25);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);

        newInstruction(0x25);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)-1);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0x0001);
        assertTrue(CPU_Regs.reg_eax.dword==0x0001);

        newInstruction(0x25);
        CPU_Regs.reg_eax.word(0x0000);
        pushIw((short)0x0000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);
        assertTrue(CPU_Regs.reg_eax.dword==0);

        newInstruction(0x25);
        CPU_Regs.reg_eax.word(0x0000);
        pushIw((short)0xFFFF);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);

        newInstruction(0x25);
        CPU_Regs.reg_eax.word(0xFFFF);
        pushIw((short)0xFFFF);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFFFF);
    }

    //0x26
    //SEG ES
    public void testSegES() {
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        newInstruction(0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS,(byte)0);

        newInstruction(0x26);
        pushIb((byte)0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        CPU_Regs.SegSet16ES(CPU_Regs.reg_ssVal.dword);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        assertTrue(RAM.readbs(MEM_BASE_SS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_SS, (byte)0);
    }

    //0x27
    //DAA
    public void testDaa() {
        newInstruction(0x27);
        CPU_Regs.reg_eax.dword(0x09);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x09);
        assertTrue(!Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x27);
        CPU_Regs.reg_eax.dword(0xC9);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x29);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x27);
        CPU_Regs.reg_eax.dword(0x0A);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x10);
        assertTrue(!Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x27);
        CPU_Regs.reg_eax.dword(0xBA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x20);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;
    }

    // 0x28
    //SUB Eb,Gb
    public void testSubEbGb() {
        runRegsb((byte)0x28, 3, 2, false, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x28, 192, 128, false, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x28, 128, 192, false, -64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegb((byte)0x28, 128, 192, false, -64); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x29
    //SUB Ew,Gw
    public void testSubEwGw() {
        runRegsw((byte)0x29, 3, 2, false, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x29, 0xC000, 0x8000, false, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x29, 0x8000, 0xC000, false, -0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegw((byte)0x29, 0x8000, 0xC000, false, -0x4000); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x2a
    //SUB Gb,Eb
    public void testSubGbEb() {
        runRegsb((byte)0x2a, 2, 3, true, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x2a, 128, 192, true, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegb((byte)0x2a, 192, 128, true, -64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegb((byte)0x2a, 192, 128, true, -64); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x2b
    //SUB Gw,Ew
    public void testSubGwEw() {
        runRegsw((byte)0x2b, 2, 3, true, 1, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x2b, 0x8000, 0xC000, true, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegw((byte)0x2b, 0xC000, 0x8000, true, -0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegw((byte)0x2b, 0xC000, 0x8000, true, -0x4000); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x2c
    //SUB AL,Ib
    public void testSubAlIb() {
        newInstruction(0x2c);
        CPU_Regs.reg_eax.low(3);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x2c);
        CPU_Regs.reg_eax.low(192);
        pushIb((byte)128);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==64);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x2c);
        CPU_Regs.reg_eax.low(128);
        pushIb((byte)192);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==192);
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        newInstruction(0x2c);
        CPU_Regs.reg_eax.low(128);
        pushIb((byte)192);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==192); // test no carry
        assertTrue(CPU_Regs.reg_eax.high()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    // 0x2d
    //SUB AX,Iw
    public void testSubAxIw() {
        newInstruction(0x2d);
        CPU_Regs.reg_eax.word(3);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x2d);
        CPU_Regs.reg_eax.word(0xC000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0x4000);
        assertTrue(CPU_Regs.reg_eax.dword==0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x2d);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0xC000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xC000);
        assertTrue(CPU_Regs.reg_eax.dword==0xC000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        newInstruction(0x2d);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0xC000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xC000);
        assertTrue(CPU_Regs.reg_eax.dword==0xC000); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
    }

    //0x2e
    //SEG CS
    public void testSegCS() {
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        newInstruction(0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS,(byte)0);

        newInstruction(0x2e);
        pushIb((byte)0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        CPU_Regs.reg_ebx.word(100);
        //CPU_Regs.reg_csPhys.dword = MEM_BASE_SS;
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        assertTrue(RAM.readbs(CPU_Regs.reg_csPhys.dword+100)==(byte)0xAB);
        RAM.writebs(CPU_Regs.reg_csPhys.dword+100, (byte)0);
    }

    // 0x2f
    //DAS
    public void testDas() {
        newInstruction(0x2f);
        CPU_Regs.reg_eax.dword(0x09);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x09);
        assertTrue(!Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x2f);
        CPU_Regs.reg_eax.dword(0x0A);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x04);
        assertTrue(!Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x2f);
        CPU_Regs.reg_eax.dword(0xBA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x54);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;
    }

     // 0x30
    // XOR Eb,Gb
    public void testXorEbGb() {  
        runRegsb((byte)0x30, 1, 2, false, 3, 0);
        runRegb((byte)0x30, 1, -1, false, 254);
        runRegb((byte)0x30, 0x80, 0x80, false, 0);
        runRegb((byte)0x30, 0xF0, 0x0F, false, 0xFF);
    }

    // 0x31
    // XOR Ew,Gw
    public void testXorEwGw() {
        runRegsw((byte)0x31, 1, 2, false, 3, 0);
        runRegw((byte)0x31, 1, -1, false, 0xFFFE);
        runRegw((byte)0x31, 0x8000, 0x8000, false, 0);
        runRegw((byte)0x31, 0xFF00, 0x00FF, false, 0xFFFF);
    }

    // 0x32
    // XOR Gb,Eb
    public void testXorGbEb() {
        runRegsb((byte)0x32, 1, 2, true, 3, 0);
        runRegb((byte)0x32, 1, -1, true, 254);
        runRegb((byte)0x32, 0x80, 0x80, true, 0);
        runRegb((byte)0x32, 0xF0, 0x0F, true, 0xFF);
    }

    // 0x33
    // XOR Gw,Ew
    public void testXorGwEw() {
        runRegsw((byte)0x33, 1, 2, true, 3, 0);
        runRegw((byte)0x33, 1, -1, true, 0xFFFE);
        runRegw((byte)0x33, 0x8000, 0x8000, true, 0);
        runRegw((byte)0x33, 0xFF00, 0x00FF, true, 0xFFFF);
    }

    // 0x34
    // XOR AL,Ib
    public void testXorAlIb() {
        newInstruction(0x34);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==3);

        newInstruction(0x34);
        CPU_Regs.reg_eax.low(1);
        pushIb((byte)-1);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==254);
        assertTrue(CPU_Regs.reg_eax.high()==0);

        newInstruction(0x34);
        CPU_Regs.reg_eax.low(0x80);
        pushIb((byte)0x80);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0);
        assertTrue(CPU_Regs.reg_eax.high()==0);

        newInstruction(0x34);
        CPU_Regs.reg_eax.low(0xF0);
        pushIb((byte)0x0F);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0xFF);
    }

    // 0x35
    // XOR AX,Iw
     public void testXorAxIw() {
        newInstruction(0x35);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==3);

        newInstruction(0x35);
        CPU_Regs.reg_eax.word(1);
        pushIw((short)-2);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFFFF);
        assertTrue(CPU_Regs.reg_eax.dword==0xFFFF);
        assertTrue(!Flags.get_CF());

        newInstruction(0x35);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);
        assertTrue(CPU_Regs.reg_eax.dword==0);
         if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x35);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0x8000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0);
    }

    //0x36
    //SEG SS
    public void testSegSS() {
        // base_ds point to DS
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        newInstruction(0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS,(byte)0);

        // base_ds changed to ss
        newInstruction(0x36);
        pushIb((byte)0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_SS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_SS, (byte)0);
    }

    // 0x37
    //AAA
    public void testAaa() {
        newInstruction(0x37);
        CPU_Regs.reg_eax.dword(0x09);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x09);
        assertTrue(!Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x37);
        CPU_Regs.reg_eax.dword(0x0A);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0x100);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x37);
        CPU_Regs.reg_eax.dword(0xBA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0x100);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;
    }

    // 0x38
    //CMP Eb,Gb
    public void testCmpEbGb() {
        runRegsbFlagsLess((byte)0x38, 2, 3);
        runRegbFlags((byte)0x38, 128, 192, -1);
        assertTrue(Flags.get_CF());
        runRegbFlags((byte)0x38, 192, 128, 1);
        assertTrue(!Flags.get_CF());
        runRegbFlags((byte)0x38, 1, 1, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x39
    //CMP Ew,Gw
    public void testCmpEwGw() {
        runRegswFlagsLess((byte)0x39, 2, 3);
        runRegwFlags((byte)0x39, 0x8000, 0xC000, -1);
        assertTrue(Flags.get_CF());
        runRegwFlags((byte)0x39, 0xC000, 0x8000, 1);
        assertTrue(!Flags.get_CF());
        runRegwFlags((byte)0x39, 0x8000, 0x8000, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x3a
    //CMP Gb,Eb
    public void testCmpGbEb() {
        runRegsbFlagsLess((byte)0x3a, 3, 2);
        runRegbFlags((byte)0x3a, 128, 192, 1);
        assertTrue(!Flags.get_CF());
        runRegbFlags((byte)0x3a, 192, 128, -1);
        assertTrue(Flags.get_CF());
        runRegbFlags((byte)0x3a, 1, 1, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x3b
    //CMP Gw,Ew
    public void testCmpGwEw() {
        runRegswFlagsLess((byte)0x3b, 3, 2);
        runRegwFlags((byte)0x3b, 0x8000, 0xC000, 1);
        assertTrue(!Flags.get_CF());
        runRegwFlags((byte)0x3b, 0xC000, 0x8000, -1);
        assertTrue(Flags.get_CF());
        runRegwFlags((byte)0x3b, 0x8000, 0x8000, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x3c
    //CMP AL,Ib
    public void testCmpAlIb() {
        newInstruction(0x3c);
        CPU_Regs.reg_eax.low(3);
        pushIb((byte)2);
        decoder.call();
        assertFlags(1);
        assertTrue(!Flags.get_CF());

        newInstruction(0x3c);
        CPU_Regs.reg_eax.low(192);
        pushIb((byte)128);
        decoder.call();
        assertFlags(1);
        assertTrue(!Flags.get_CF());

        newInstruction(0x3c);
        CPU_Regs.reg_eax.low(128);
        pushIb((byte)192);
        decoder.call();
        assertFlags(-1);
        assertTrue(Flags.get_CF());

        newInstruction(0x3c);
        CPU_Regs.reg_eax.low(255);
        pushIb((byte)255);
        decoder.call();
        assertFlags(0);
        assertTrue(!Flags.get_CF());
    }

    // 0x3d
    //CMP AX,Iw
    public void testCmpAxIw() {
        newInstruction(0x3d);
        CPU_Regs.reg_eax.word(3);
        pushIw((short)2);
        decoder.call();
        assertFlags(1);
        assertTrue(!Flags.get_CF());

        newInstruction(0x3d);
        CPU_Regs.reg_eax.word(0xC000);
        pushIw((short)0x8000);
        decoder.call();
        assertFlags(1);
        assertTrue(!Flags.get_CF());

        newInstruction(0x3d);
        CPU_Regs.reg_eax.word(0x8000);
        pushIw((short)0xC000);
        decoder.call();
        assertFlags(-1);
        assertTrue(Flags.get_CF());

        newInstruction(0x3d);
        CPU_Regs.reg_eax.word(0x0);
        pushIw((short)0x0);
        decoder.call();
        assertFlags(0);
        assertTrue(!Flags.get_CF());
    }

    //0x3E
    //SEG DS
    public void testSegDS() {
        // base_ds point to DS
        assertTrue(RAM.readbs(MEM_BASE_SS)==0);
        newInstruction(0x88);
        pushIb((byte)3);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_SS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_SS, (byte)0);

        // base_ss changed to ds
        newInstruction(0x3E);
        pushIb((byte)0x88);
        pushIb((byte)3);
        CPU_Regs.reg_eax.dword(0xAB);
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS, (byte)0);
    }

    // 0x3F
    //AAS
    public void testAas() {
        newInstruction(0x3F);
        CPU_Regs.reg_eax.dword(0x09);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.low()==0x09);
        assertTrue(!Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x3F);
        CPU_Regs.reg_eax.dword(0x0A);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFF04);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;

        newInstruction(0x3F);
        CPU_Regs.reg_eax.dword(0xBA);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==0xFF04);
        assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;
    }

    private void incWord(int instruction, CPU_Regs.Reg reg) {
        newInstruction(instruction);
        reg.word(0x00);
        decoder.call();
        assertTrue(reg.word()==0x01);
        newInstruction(instruction);
        reg.word(0x7FFF);
        decoder.call();
        assertTrue(reg.word()==0x8000);
        newInstruction(instruction);
        reg.word(0xFFFF);
        decoder.call();
        assertTrue(reg.word()==0x00);
    }

    // 0x40
    //INC AX
    public void testIncAx() {
        incWord(0x40, CPU_Regs.reg_eax);
    }

    // 0x41
    //INC CX
    public void testIncCx() {
        incWord(0x41, CPU_Regs.reg_ecx);
    }

    // 0x42
    //INC DX
    public void testIncDx() {
        incWord(0x42, CPU_Regs.reg_edx);
    }

    // 0x43
    //INC BX
    public void testIncBx() {
        incWord(0x43, CPU_Regs.reg_ebx);
    }

    // 0x44
    //INC SP
    public void testIncSp() {
        incWord(0x44, CPU_Regs.reg_esp);
    }

    // 0x45
    //INC BP
    public void testIncBp() {
        incWord(0x45, CPU_Regs.reg_ebp);
    }

    // 0x46
    //INC SI
    public void testIncSi() {
        incWord(0x46, CPU_Regs.reg_esi);
    }

    // 0x47
    //INC DI
    public void testIncDi() {
        incWord(0x47, CPU_Regs.reg_edi);
    }

    private void decWord(int instruction, CPU_Regs.Reg reg) {
        newInstruction(instruction);
        reg.word(0x02);
        decoder.call();
        assertTrue(reg.word()==0x01);
        newInstruction(instruction);
        reg.word(0x8000);
        decoder.call();
        assertTrue(reg.word()==0x7FFF);
        newInstruction(instruction);
        reg.word(0x0);
        decoder.call();
        assertTrue(reg.word()==0xFFFF);
    }

    // 0x48
    //DEC AX
    public void testDecAx() {
        decWord(0x48, CPU_Regs.reg_eax);
    }

    // 0x49
    //DEC CX
    public void testDecCx() {
        decWord(0x49, CPU_Regs.reg_ecx);
    }

    // 0x4A
    //DEC DX
    public void testDecDx() {
        decWord(0x4A, CPU_Regs.reg_edx);
    }

    // 0x4B
    //DEC BX
    public void testDecBx() {
        decWord(0x4B, CPU_Regs.reg_ebx);
    }

    // 0x4C
    //DEC SP
    public void testDecSp() {
        decWord(0x4C, CPU_Regs.reg_esp);
    }

    // 0x4D
    //DEC BP
    public void testDecBp() {
        decWord(0x4D, CPU_Regs.reg_ebp);
    }

    // 0x4E
    //DEC SI
    public void testDecSi() {
        decWord(0x4E, CPU_Regs.reg_esi);
    }

    // 0x4F
    //DEC DI
    public void testDecDi() {
        decWord(0x4F, CPU_Regs.reg_edi);
    }

    private void pushWord(int instruction, CPU_Regs.Reg reg) {
        newInstruction(instruction);
        CPU_Regs.reg_esp.dword(0x100);
        reg.dword(0x1ABCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        assertTrue(RAM.readd(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0);
    }

    // 0x50
    //PUSH AX
    public void testPushAx() {
        pushWord(0x50, CPU_Regs.reg_eax);
    }

    // 0x51
    //PUSH CX
    public void testPushCx() {
        pushWord(0x51, CPU_Regs.reg_ecx);
    }

    // 0x52
    //PUSH DX
    public void testPushDx() {
        pushWord(0x52, CPU_Regs.reg_edx);
    }

    // 0x53
    //PUSH BX
    public void testPushBx() {
        pushWord(0x53, CPU_Regs.reg_ebx);
    }

    // 0x54
    //PUSH SP
    public void testPushSp() {
        newInstruction(0x54);
        CPU_Regs.reg_esp.dword(0x100);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0x100);
        assertTrue(RAM.readd(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0x100);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0);
    }

    // 0x55
    //PUSH BP
    public void testPushBp() {
        pushWord(0x55, CPU_Regs.reg_ebp);
    }

    // 0x56
    //PUSH SI
    public void testPushSi() {
        pushWord(0x56, CPU_Regs.reg_esi);
    }

    // 0x57
    //PUSH DI
    public void testPushDi() {
        pushWord(0x57, CPU_Regs.reg_edi);
    }

    private void popWord(int instruction, CPU_Regs.Reg reg) {
        newInstruction(instruction);
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x89EF);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(reg.word()==0x89EF);
        assertTrue(reg.dword==0x89EF);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword-2, 0);
    }

    // 0x58
    //POP AX
    public void testPopAx() {
        popWord(0x58, CPU_Regs.reg_eax);
    }

    // 0x59
    //POP CX
    public void testPopCx() {
        popWord(0x59, CPU_Regs.reg_ecx);
    }

    // 0x5A
    //POP DX
    public void testPopDx() {
        popWord(0x5A, CPU_Regs.reg_edx);
    }

    // 0x5B
    //POP BX
    public void testPopBx() {
        popWord(0x5B, CPU_Regs.reg_ebx);
    }

    // 0x5C
    //POP SP
    public void testPopSp() {
        newInstruction(0x5c);
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x102);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x102);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFE, 0);
    }

    // 0x5D
    //POP BP
    public void testPopBp() {
        popWord(0x5D, CPU_Regs.reg_ebp);
    }

    // 0x5E
    //POP SI
    public void testPopSi() {
        popWord(0x5E, CPU_Regs.reg_esi);
    }

    // 0x5F
    //POP DI
    public void testPopDi() {
        popWord(0x5F, CPU_Regs.reg_edi);
    }

    // 0x60
    //PUSHA
    public void testPushA() {
        newInstruction(0x60);
        CPU_Regs.reg_esp.dword(0x100);
        CPU_Regs.reg_eax.dword(0x1ABC0);
        CPU_Regs.reg_ecx.dword(0x1ABC1);
        CPU_Regs.reg_edx.dword(0x1ABC2);
        CPU_Regs.reg_ebx.dword(0x1ABC3);
        //CPU_Regs.reg_esp.dword(0x1ABC4);
        CPU_Regs.reg_ebp.dword(0x1ABC5);        
        CPU_Regs.reg_esi.dword(0x1ABC6);
        CPU_Regs.reg_edi.dword(0x1ABC7);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xF0);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xFE)==0xABC0);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xFC)==0xABC1);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xFA)==0xABC2);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xF8)==0xABC3);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xF6)==0x0100);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xF4)==0xABC5);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xF2)==0xABC6);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+0xF0)==0xABC7);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFE, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFC, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFA, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF8, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF6, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF4, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF2, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF0, 0);
    }

    // 0x61
    //POPA
    public void testPopA() {
        newInstruction(0x61);
        CPU_Regs.reg_esp.dword(0xF0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFE, 0xABC7);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFC, 0xABC6);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFA, 0xABC5);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF8, 0xABC4);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF6, 0xABC3);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF4, 0xABC2);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF2, 0xABC1);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF0, 0xABC0);

        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xABC7);
        assertTrue(CPU_Regs.reg_ecx.dword==0xABC6);
        assertTrue(CPU_Regs.reg_edx.dword==0xABC5);
        assertTrue(CPU_Regs.reg_ebx.dword==0xABC4);
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(CPU_Regs.reg_ebp.dword==0xABC2);
        assertTrue(CPU_Regs.reg_esi.dword==0xABC1);
        assertTrue(CPU_Regs.reg_edi.dword==0xABC0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFE, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFC, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xFA, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF8, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF6, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF4, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF2, 0);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+0xF0, 0);
    }

    // 0x62
    //BOUND
    public void testBound() {
        newInstruction(0x62);
        pushIb((byte)0xC0);
        CPU_Regs.reg_eax.dword(0);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0);
        RAM.writew(CPU_Regs.reg_dsPhys.dword+2, 0);
        decoder.call();
        assertTrue(CPU.lastint == 0);

        newInstruction(0x62);
        pushIb((byte)0xC0);
        CPU_Regs.reg_eax.word(1);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0);
        RAM.writew(CPU_Regs.reg_dsPhys.dword+2, 0);
        decoder.call();
        assertTrue(CPU.lastint == 5);

        newInstruction(0x62);
        pushIb((byte)0xC0);
        CPU_Regs.reg_eax.word(0x7000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0x7000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword+2, 0x7000);
        decoder.call();
        assertTrue(CPU.lastint == 0);

        newInstruction(0x62);
        pushIb((byte)0xC0);
        CPU_Regs.reg_eax.word(0x7001);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0x7000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword+2, 0x7000);
        decoder.call();
        assertTrue(CPU.lastint == 5);

        newInstruction(0x62);
        pushIb((byte)0xC0);
        CPU_Regs.reg_eax.word(0xA000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0x9000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword+2, 0xB000);
        decoder.call();
        assertTrue(CPU.lastint == 0);

        newInstruction(0x62);
        pushIb((byte)0xC0);
        CPU_Regs.reg_eax.word(0x8000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0x9000);
        RAM.writew(CPU_Regs.reg_dsPhys.dword+2, 0);
        decoder.call();
        assertTrue(CPU.lastint == 5);
    }

    // 0x63
    //ARPL Ew,Rw
    public void testArplEwRw() {
        newInstruction(0x63);
        pushIb((byte)0xC1);
        decoder.call();
        assertTrue(CPU.lastint == 6);

        CPU.CPU_AutoDetermineMode = 0;
        CPU.CPU_SET_CRX(0, CPU.CR0_PROTECTION);
        newInstruction(0x63);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0);
        CPU_Regs.reg_ecx.dword(1);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0x63);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(2);
        CPU_Regs.reg_ecx.dword(1);
        decoder.call();
        assertTrue(Flags.get_ZF());
    }

    //0x64
    //SEG FS
    public void testSegFS() {
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        newInstruction(0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS,(byte)0);

        newInstruction(0x64);
        pushIb((byte)0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        CPU_Regs.SegSet16FS(MEM_BASE_SS>>4);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        assertTrue(RAM.readbs(MEM_BASE_SS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_SS, (byte)0);
    }

    //0x65
    //SEG GS
    public void testSegGS() {
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        newInstruction(0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS,(byte)0);

        newInstruction(0x65);
        pushIb((byte)0x88);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xAB);
        CPU_Regs.SegSet16GS(MEM_BASE_SS>>4);

        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS)==0);
        assertTrue(RAM.readbs(MEM_BASE_SS)==(byte)0xAB);
        RAM.writebs(MEM_BASE_SS, (byte)0);
    }

    //0x66
    //Operand Size Prefix
    // :TODO:

    // 0x67
    //Address Size Prefix
    public void testAddressSizePrefix() {
        assertTrue(RAM.readbs(MEM_BASE_DS+0x10000)==0);
        newInstruction(0x67);
        pushIb((byte)0x88);
        pushIb((byte)1);
        CPU_Regs.reg_ecx.dword(0x10000);
        CPU_Regs.reg_eax.dword(0xAB);
        decoder.call();
        assertTrue(RAM.readbs(MEM_BASE_DS+0x10000)==(byte)0xAB);
        RAM.writebs(MEM_BASE_DS+0x10000, (byte)0);
    }

    // 0x68
    //PUSH Iw
    public void testPushIw() {
        newInstruction(0x68);
        CPU_Regs.reg_esp.dword(0x100);
        pushIw((short)0xABCD);
        pushIw((short)0xEF01); // just make sure it doesn't read more
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue(RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        assertTrue(RAM.readd(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==0xABCD);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0);
    }

    // 0x69
    //IMUL Gw,Ew,Iw
    public void testImulGwEwIw() {
        newInstruction(0x69);
        pushIb((byte)0xC1);
        pushIw((short)1000);
        CPU_Regs.reg_ecx.word(9);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==9000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x69);
        pushIb((byte)0xC1);
        pushIw((short)-9);
        CPU_Regs.reg_ecx.word(1000);
        decoder.call();
        assertTrue((short)CPU_Regs.reg_eax.word()==-9000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x69);
        pushIb((byte)0xC1);
        pushIw((short)0x2);
        CPU_Regs.reg_ecx.word(0x8000);
        decoder.call();
        assertTrue((short)CPU_Regs.reg_eax.word()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        newInstruction(0x69);
        pushIb((byte)0);
        pushIw((short)-384);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 20);
        decoder.call();
        assertTrue((short)CPU_Regs.reg_eax.word()==-7680);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0);

    }

    // 0x6a
    //PUSH Ib
    public void testPushIb() {
        newInstruction(0x6a);
        CPU_Regs.reg_esp.dword(0x100);
        pushIb((byte)-71);
        pushIw((short)0xEF01); // just make sure it doesn't read more
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0xFE);
        assertTrue((short)RAM.readw(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword)==-71);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0);
    }

    // 0x6b
    //IMUL Gw,Ew,Ib
    public void testImulGwEwIb() {
        newInstruction(0x6b);
        pushIb((byte)0xC1);
        pushIb((byte)9);
        CPU_Regs.reg_ecx.word(1000);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.word()==9000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x6b);
        pushIb((byte)0xC1);
        pushIb((byte)-9);
        CPU_Regs.reg_ecx.word(1000);
        decoder.call();
        assertTrue((short)CPU_Regs.reg_eax.word()==-9000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());

        newInstruction(0x6b);
        pushIb((byte)0xC1);
        pushIb((byte)0x2);
        CPU_Regs.reg_ecx.word(0x8000);
        decoder.call();
        assertTrue((short)CPU_Regs.reg_eax.word()==0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        newInstruction(0x6b);
        pushIb((byte)0);
        pushIw((byte)-127);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, -20);
        decoder.call();
        assertTrue((short)CPU_Regs.reg_eax.word()==2540);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0);
    }

    // 0x6c
    //INSB
    public void testInsb() {
        newInstruction(0x6c);
        CPU_Regs.reg_edx.word(17000);
        CPU_Regs.reg_ecx.dword(1);
        CPU_Regs.reg_edi.dword(0);
        CPU_Regs.SegSet16ES(CPU_Regs.reg_dsVal.dword+0x100);
        IoHandler.IO_ReadHandler handler = new IoHandler.IO_ReadHandler() {public int call(int port, int iolen) {return 0xAB;}};
        IoHandler.IO_RegisterReadHandler(17000, handler, IoHandler.IO_MB);
        decoder.call();        
        assertTrue(RAM.readb(CPU_Regs.reg_esPhys.dword)==0xAB);
        IoHandler.IO_FreeReadHandler(17000, IoHandler.IO_MB);
        RAM.writeb(CPU_Regs.reg_esPhys.dword,(short)0);
    }

    // 0x6d
    //INSW
    public void testInsw() {
        newInstruction(0x6d);
        CPU_Regs.reg_edx.word(17000);
        CPU_Regs.reg_ecx.dword(1);
        CPU_Regs.reg_edi.dword(0);
        CPU_Regs.SegSet16ES(CPU_Regs.reg_dsVal.dword+0x100);
        IoHandler.IO_ReadHandler handler = new IoHandler.IO_ReadHandler() {public int call(int port, int iolen) {return 0xAB;}};
        IoHandler.IO_RegisterReadHandler(17000, handler, IoHandler.IO_MW);
        decoder.call();
        assertTrue(RAM.readw(CPU_Regs.reg_esPhys.dword)==0xAB);
        IoHandler.IO_FreeReadHandler(17000, IoHandler.IO_MW);
        RAM.writew(CPU_Regs.reg_esPhys.dword,0);
    }

    // 0x6e
    //OUTSB
    private static int result;
    public void testOutsb() {
        newInstruction(0x6e);
        CPU_Regs.reg_edx.word(17000);
        CPU_Regs.reg_ecx.dword(1);
        CPU_Regs.reg_esi.dword(0);
        result = 0;
        IoHandler.IO_WriteHandler handler = new IoHandler.IO_WriteHandler() {public void call(int port, int val, int iolen) {result=val;}};
        IoHandler.IO_RegisterWriteHandler(17000, handler, IoHandler.IO_MB);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0xABCD);
        decoder.call();
        assertTrue(result==0xCD);
        IoHandler.IO_FreeWriteHandler(17000, IoHandler.IO_MB);
        RAM.writew(CPU_Regs.reg_dsPhys.dword, 0);
    }

    // 0x6f
    //OUTSW
    public void testOutsw() {
        newInstruction(0x6f);
        CPU_Regs.reg_edx.word(17000);
        CPU_Regs.reg_ecx.dword(1);
        CPU_Regs.reg_esi.dword(0);
        result = 0;
        IoHandler.IO_WriteHandler handler = new IoHandler.IO_WriteHandler() {public void call(int port, int val, int iolen) {result=val;}};
        IoHandler.IO_RegisterWriteHandler(17000, handler, IoHandler.IO_MW);
        RAM.writed(CPU_Regs.reg_dsPhys.dword, 0xABCDEF01);
        decoder.call();
        assertTrue(result==0xEF01);
        IoHandler.IO_FreeWriteHandler(17000, IoHandler.IO_MW);
        RAM.writed(CPU_Regs.reg_dsPhys.dword, 0);
    }

    private void doJump(int instruction, int flag, boolean cond) {
        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag, !cond);
        int eip = CPU_Regs.reg_eip;
        decoder.call();
        assertTrue(CPU_Regs.reg_eip==eip+2);

        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag, cond);
        eip = CPU_Regs.reg_eip;
        decoder.call();
        assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));
    }

    private void doJumpOr(int instruction, int flag1, int flag2, boolean cond) {
        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag1, !cond);
        CPU_Regs.SETFLAGBIT(flag2, !cond);
        int eip = CPU_Regs.reg_eip;
        decoder.call();
        assertTrue(CPU_Regs.reg_eip==eip+2);

        if (cond) {
            newInstruction(instruction);
            pushIb((byte)-32);
            CPU_Regs.SETFLAGBIT(flag1, !cond);
            CPU_Regs.SETFLAGBIT(flag1, cond);
            eip = CPU_Regs.reg_eip;
            decoder.call();
            assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));

            newInstruction(instruction);
            pushIb((byte)-32);
            CPU_Regs.SETFLAGBIT(flag1, cond);
            CPU_Regs.SETFLAGBIT(flag2, !cond);
            eip = CPU_Regs.reg_eip;
            decoder.call();
            assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));
        } else {
            newInstruction(instruction);
            pushIb((byte)-32);
            CPU_Regs.SETFLAGBIT(flag1, !cond);
            CPU_Regs.SETFLAGBIT(flag1, cond);
            eip = CPU_Regs.reg_eip;
            decoder.call();
            assertTrue(CPU_Regs.reg_eip==eip+2);

            newInstruction(instruction);
            pushIb((byte)-32);
            CPU_Regs.SETFLAGBIT(flag1, cond);
            CPU_Regs.SETFLAGBIT(flag2, !cond);
            eip = CPU_Regs.reg_eip;
            decoder.call();
            assertTrue(CPU_Regs.reg_eip==eip+2);
        }

        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag1, cond);
        CPU_Regs.SETFLAGBIT(flag2, cond);
        eip = CPU_Regs.reg_eip;
        decoder.call();
        assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));
    }

    private void doJumpEqual(int instruction, int flag1, int flag2, boolean cond) {
        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag1, !cond);
        CPU_Regs.SETFLAGBIT(flag2, cond);
        int eip = CPU_Regs.reg_eip;
        decoder.call();
        if (cond)
            assertTrue(CPU_Regs.reg_eip==eip+2);
        else
            assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));

        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag1, cond);
        CPU_Regs.SETFLAGBIT(flag2, !cond);
        eip = CPU_Regs.reg_eip;
        decoder.call();
        if (cond)
            assertTrue(CPU_Regs.reg_eip==eip+2);
        else
            assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));

        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag1, cond);
        CPU_Regs.SETFLAGBIT(flag2, cond);
        eip = CPU_Regs.reg_eip;
        decoder.call();
        if (cond)
            assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));
        else
            assertTrue(CPU_Regs.reg_eip==eip+2);


        newInstruction(instruction);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(flag1, !cond);
        CPU_Regs.SETFLAGBIT(flag2, !cond);
        eip = CPU_Regs.reg_eip;
        decoder.call();
        if (cond)
            assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));
        else
            assertTrue(CPU_Regs.reg_eip==eip+2);
    }

    // 0x70
    //JO
    public void testJo() {
        CPU_Regs.flags = 0;
        doJump(0x70, CPU_Regs.OF, true);
    }

    // 0x71
    //JNO
    public void testJno() {
        CPU_Regs.flags = 0;
        doJump(0x71, CPU_Regs.OF, false);
    }

    // 0x72
    //JB
    public void testJb() {
        CPU_Regs.flags = 0;
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, true);
        doJump(0x72, CPU_Regs.CF, true);
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, false);
        doJump(0x72, CPU_Regs.CF, true);
    }

    // 0x73
    //JNB
    public void testJnb() {
        CPU_Regs.flags = 0;
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, true);
        doJump(0x73, CPU_Regs.CF, false);
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, false);
        doJump(0x73, CPU_Regs.CF, false);
    }

    // 0x74
    //JZ
    public void testJz() {
        CPU_Regs.flags = 0;
        doJump(0x74, CPU_Regs.ZF, true);
    }

    // 0x75
    //JNZ
    public void testJnz() {
        CPU_Regs.flags = 0;
        doJump(0x75, CPU_Regs.ZF, false);
    }

    // 0x76
    //JBE
    public void testJbe() {
        CPU_Regs.flags = 0;
        doJumpOr(0x76, CPU_Regs.CF, CPU_Regs.ZF, true);
    }

    // 0x77
    //JNBE
    public void testJnbe() {
        CPU_Regs.flags = 0;
        doJumpOr(0x77, CPU_Regs.CF, CPU_Regs.ZF, false);
    }

    // 0x78
    //JS
    public void testJs() {
        CPU_Regs.flags = 0;
        doJump(0x78, CPU_Regs.SF, true);
    }

    // 0x79
    //JNS
    public void testJns() {
        CPU_Regs.flags = 0;
        doJump(0x79, CPU_Regs.SF, false);
    }

    // 0x7a
    //JP
    public void testJp() {
        CPU_Regs.flags = 0;
        doJump(0x7a, CPU_Regs.PF, true);
    }

    // 0x7b
    //JNP
    public void testJnp() {
        CPU_Regs.flags = 0;
        doJump(0x7b, CPU_Regs.PF, false);
    }

    // 0x7c
    //JL
    public void testJl() {
        CPU_Regs.flags = 0;
        doJumpEqual(0x7c, CPU_Regs.SF, CPU_Regs.OF, false);
    }

    // 0x7d
    //JNL
    public void testJnl() {
        CPU_Regs.flags = 0;
        doJumpEqual(0x7d, CPU_Regs.SF, CPU_Regs.OF, true);
    }

    // 0x7e
    //JL
    public void testJle() {
        CPU_Regs.flags = 0;
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, false);
        doJumpEqual(0x7e, CPU_Regs.SF, CPU_Regs.OF, false);

        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, true);
        newInstruction(0x7e);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF, true);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF, true);
        int eip = CPU_Regs.reg_eip;
        decoder.call();
        assertTrue(CPU_Regs.reg_eip==((eip-32+2) & 0xFFFF));
    }

    // 0x7f
    //JNL
    public void testJnle() {
        CPU_Regs.flags = 0;
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, false);
        doJumpEqual(0x7f, CPU_Regs.SF, CPU_Regs.OF, true);

        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, true);
        newInstruction(0x7f);
        pushIb((byte)-32);
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF, true);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF, true);
        int eip = CPU_Regs.reg_eip;
        decoder.call();
        assertTrue(CPU_Regs.reg_eip==eip+2);
    }

    // 0x80
    //Grpl Eb,Ib
    public void testGroup1EbIb() {
        // ADDB
        runRegsbi((byte)0x80, (byte)0, 1, (byte)2, 3);
        runRegbi((byte)0x80, 0, 1, -2, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegbi((byte)0x80, 0, 0x80, 0x80, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegbi((byte)0x80, 0, 0x80, 0x80, 0);

        // ORB
        runRegsbi((byte)0x80, (byte)(1<<3), 1, (byte)2, 3);
        runRegbi((byte)0x80, 1<<3, 0, 0, 0);
        runRegbi((byte)0x80, 1<<3, 0xFF, 0, 0xFF);
        runRegbi((byte)0x80, 1<<3, 0, 0xCF, 0xCF);
        runRegbi((byte)0x80, 1<<3, 0xF0, 0x05, 0xF5);

        // ADCB
        runRegsbi((byte)0x80, (byte)(2<<3), 1, (byte)2, 3);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegbi((byte)0x80, 2<<3, 192, 128, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegbi((byte)0x80, 2<<3, 192, 128, 65);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;

        // SBBB
        runRegsbi((byte)0x80, (byte)(3<<3), 3, (byte)2, 1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegbi((byte)0x80, 3<<3, 192, 128, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegbi((byte)0x80, 3<<3, 128, 192, -64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegbi((byte)0x80, 3<<3, 128, 192, -65);  // test the -1 carry

        // ANDB
        runRegsbi((byte)0x80, (byte)(4<<3), 1, (byte)2, 0);
        runRegbi((byte)0x80, 4<<3, 1, -1, 1);
        runRegbi((byte)0x80, 4<<3, 0x0, 0x0, 0x0);
        runRegbi((byte)0x80, 4<<3, 0x00, 0xFF, 0x0);
        runRegbi((byte)0x80, 4<<3, 0xFF, 0xFF, 0xFF);

        // SUBB
        runRegsbi((byte)0x80, (byte)(5<<3), 3, (byte)2, 1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegbi((byte)0x80, 5<<3, 192, 128, 64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegbi((byte)0x80, 5<<3, 128, 192, -64);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegbi((byte)0x80, 5<<3, 128, 192, -64); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        // XOR
        runRegsbi((byte)0x80, (byte)(6<<3), 1, (byte)2, 3);
        runRegbi((byte)0x80, 6<<3, 1, -1, 254);
        runRegbi((byte)0x80, 6<<3, 0x80, 0x80, 0);
        runRegbi((byte)0x80, 6<<3, 0xF0, 0x0F, 0xFF);

        // CMP
        runRegsbFlagsLessi((byte)0x80, 7<<3, 2, 3);
        runRegbFlagsi((byte)0x80, 7<<3, 128, 192, -1);
        assertTrue(Flags.get_CF());
        runRegbFlagsi((byte)0x80, 7<<3, 192, 128, 1);
        assertTrue(!Flags.get_CF());
        runRegbFlagsi((byte)0x80, 7<<3, 1, 1, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x81
    //Grpl Ew,Iw
    public void testGroup1EwIw() {
        // ADD
        runRegswi((byte)0x81, (byte)0, 1001, 2, 1003);
        runRegwi((byte)0x81, (byte)0, 1, -2, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwi((byte)0x81, (byte)0, 0x8000, 0x8000, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegwi((byte)0x81, (byte)0, 0x8000, 0x8000, 0);

        // OR
        runRegswi((byte)0x81, 1<<3, 1, 2, 3);
        runRegwi((byte)0x81, 1<<3, 0, 0,  0);
        runRegwi((byte)0x81, 1<<3, 0xFFFF, 0, 0xFFFF);
        runRegwi((byte)0x81, 1<<3, 0, 0xFFFF, 0xFFFF);
        runRegwi((byte)0x81, 1<<3, 0xFF00, 0x00FF, 0xFFFF);

        // ADC
        runRegswi((byte)0x81, 2<<3, 1, 2, 3);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwi((byte)0x81, 2<<3, 0xC000, 0x8000, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegwi((byte)0x81, 2<<3, 0xC000, 0x8000, 0x4001);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;
        
        // SBB
        runRegswi((byte)0x81, 3<<3, 3, 2, 1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwi((byte)0x81, 3<<3, 0xC000, 0x8000, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwi((byte)0x81, 3<<3, 0x8000, 0xC000, -0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegwi((byte)0x81, 3<<3, 0x8000, 0xC000, -0x4001);  // test the -1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        // AND
        runRegswi((byte)0x81, 4<<3, 1, 2, 0);
        runRegwi((byte)0x81, 4<<3, 1, -1, 1);
        runRegwi((byte)0x81, 4<<3, 0x0000, 0x0000, 0);
        runRegwi((byte)0x81, 4<<3, 0x0000, 0xFFFF, 0);
        runRegwi((byte)0x81, 4<<3, 0xFFFF, 0xFFFF, 0xFFFF);

        // SUB
        runRegswi((byte)0x81, 5<<3, 3, 2, 1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwi((byte)0x81, 5<<3, 0xC000, 0x8000, 0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwi((byte)0x81, 5<<3, 0x8000, 0xC000, -0x4000);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegwi((byte)0x81, 5<<3, 0x8000, 0xC000, -0x4000); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        // XOR
        runRegswi((byte)0x81, 6<<3, 1, 2, 3);
        runRegwi((byte)0x81, 6<<3, 1, -1, 0xFFFE);
        runRegwi((byte)0x81, 6<<3, 0x8000, 0x8000, 0);
        runRegwi((byte)0x81, 6<<3, 0xFF00, 0x00FF, 0xFFFF);

        // CMP
        runRegswFlagsLessi((byte)0x81, 7<<3, 2, 3);
        runRegwFlagsi((byte)0x81, 7<<3, 0x8000, 0xC000, -1);
        assertTrue(Flags.get_CF());
        runRegwFlagsi((byte)0x81, 7<<3, 0xC000, 0x8000, 1);
        assertTrue(!Flags.get_CF());
        runRegwFlagsi((byte)0x81, 7<<3, 0x8000, 0x8000, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x83
    //Grpl Ew,Ix
    public void testGroup1EwIx() {
        // ADD
        runRegswix((byte)0x83, (byte)0, 1001, (byte)2, 1003);
        runRegwix((byte)0x83, (byte)0, 1, (byte)-2, -1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwix((byte)0x83, (byte)0, 0x80, (byte)0x80, 0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegwix((byte)0x83, (byte)0, 0x80, (byte)0x80, 0);

        // OR
        runRegswix((byte)0x83, 1<<3, 1, (byte)2, 3);
        runRegwix((byte)0x83, 1<<3, 0, (byte)0,  0);
        runRegwix((byte)0x83, 1<<3, 0xFFFF, (byte)0, 0xFFFF);
        runRegwix((byte)0x83, 1<<3, 0, (byte)0xFF, 0xFFFF);
        runRegwix((byte)0x83, 1<<3, 0xFF00, (byte)0xFF, 0xFFFF);

        // ADC
        runRegswix((byte)0x83, 2<<3, 1, (byte)2, 3);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwix((byte)0x83, 2<<3, 0x00C0, (byte)0x80, 0x0040); // 0x00C0+0xFF80
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegwix((byte)0x83, 2<<3, 0x00C0, (byte)0x80, 0x0041);  // test the +1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.flags = 0;

        // SBB
        runRegswix((byte)0x83, 3<<3, 3, (byte)2, 1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwix((byte)0x83, 3<<3, 0xFFC0, (byte)0x80, 0x0040);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwix((byte)0x83, 3<<3, 0x0080, (byte)0xC0, 0x00C0);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF, true);
        runRegwix((byte)0x83, 3<<3, 0x0080, (byte)0xC0, 0x00BF);  // test the -1 carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        // AND
        runRegswix((byte)0x83, 4<<3, 1, (byte)2, 0);
        runRegwix((byte)0x83, 4<<3, 1, (byte)-1, 1);
        runRegwix((byte)0x83, 4<<3, 0x0000, (byte)0x00, 0);
        runRegwix((byte)0x83, 4<<3, 0x0000, (byte)0xFF, 0);
        runRegwix((byte)0x83, 4<<3, 0xFFFF, (byte)0xFF, 0xFFFF);

        // SUB
        runRegswix((byte)0x83, 5<<3, 3, (byte)2, 1);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwix((byte)0x83, 5<<3, 0xC000, (byte)0x70, 0xBF90);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(!Flags.get_CF());
        runRegwix((byte)0x83, 5<<3, 0x8000, (byte)0xC0, 0x8040);
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());
        runRegwix((byte)0x83, 5<<3, 0x8000, (byte)0xC0, 0x8040); // test no carry
        if (!jdos.cpu.core_dynamic.Compiler.alwayUseFastVersion)
            assertTrue(Flags.get_CF());

        // XOR
        runRegswix((byte)0x83, 6<<3, 1, (byte)2, 3);
        runRegwix((byte)0x83, 6<<3, 1, (byte)-1, 0xFFFE);
        runRegwix((byte)0x83, 6<<3, 0xFF80, (byte)0x80, 0);
        runRegwix((byte)0x83, 6<<3, 0xFF00, (byte)0xFF, 0x00FF);

        // CMP
        runRegswFlagsLessix((byte)0x83, 7<<3, 2, (byte)3);
        runRegwFlagsix((byte)0x83, 7<<3, 0x8000, (byte)0xC0, -1);
        assertTrue(Flags.get_CF());
        runRegwFlagsix((byte)0x83, 7<<3, 0x7F00, (byte)0x7F, 1);
        assertTrue(!Flags.get_CF());
        runRegwFlagsix((byte)0x83, 7<<3, 0xFF80, (byte)0x80, 0);
        assertTrue(!Flags.get_CF());
    }

    // 0x84
    //TEST Eb,Gb
    public void testTestEbGb() {
        newInstruction(0x84);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0x12345600);
        CPU_Regs.reg_ecx.dword(0x789ABC00);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x84);
        pushIb((byte)0xC5);
        CPU_Regs.reg_eax.dword(0x12345600);
        CPU_Regs.reg_ecx.dword(0x789A00BC);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x84);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0xFF);
        CPU_Regs.reg_ecx.dword(0x01);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0x84);
        pushIb((byte)0xC5);
        CPU_Regs.reg_eax.dword(0xFF);
        CPU_Regs.reg_ecx.dword(0x0100);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0x84);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0xF0);
        CPU_Regs.reg_ecx.dword(0x0F);
        decoder.call();
        assertTrue(Flags.get_ZF());

        Memory.mem_writeb(MEM_BASE_DS, 0x0F);
        Memory.mem_writeb(MEM_BASE_DS-1,0xCD);
        Memory.mem_writeb(MEM_BASE_DS+1,0xCD);

        newInstruction(0x84);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xF0);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x84);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xF1);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS-1)==(byte)0xCD);
        assertTrue((byte)Memory.mem_readb(MEM_BASE_DS+1)==(byte)0xCD);
        Memory.mem_writeb(MEM_BASE_DS-1, 0);
        Memory.mem_writeb(MEM_BASE_DS, 0);
        Memory.mem_writeb(MEM_BASE_DS+1, 0);
    }

    // 0x85
    //TEST Ew,Gw
    public void testTestEwGw() {
        newInstruction(0x85);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0x12340000);
        CPU_Regs.reg_ecx.dword(0x789A0000);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x85);
        pushIb((byte)0xC5);
        CPU_Regs.reg_eax.dword(0x12340000);
        CPU_Regs.reg_ebp.dword(0x789A0000);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x85);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0xFFFF);
        CPU_Regs.reg_ecx.dword(0x01);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0x85);
        pushIb((byte)0xC5);
        CPU_Regs.reg_eax.dword(0xFFFF);
        CPU_Regs.reg_ebp.dword(0x0100);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        newInstruction(0x85);
        pushIb((byte)0xC1);
        CPU_Regs.reg_eax.dword(0xFF00);
        CPU_Regs.reg_ecx.dword(0x00FF);
        decoder.call();
        assertTrue(Flags.get_ZF());

        Memory.mem_writew(MEM_BASE_DS, 0x0F0F);
        Memory.mem_writew(MEM_BASE_DS-2,0xCDEF);
        Memory.mem_writew(MEM_BASE_DS+2,0xCDEF);

        newInstruction(0x85);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xF0F0);
        decoder.call();
        assertTrue(Flags.get_ZF());

        newInstruction(0x85);
        pushIb((byte)0);
        CPU_Regs.reg_eax.dword(0xF1);
        decoder.call();
        assertTrue(!Flags.get_ZF());

        assertTrue((short)Memory.mem_readw(MEM_BASE_DS-2)==(short)0xCDEF);
        assertTrue((short)Memory.mem_readw(MEM_BASE_DS+2)==(short)0xCDEF);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    // 0x86
    //XCHG Eb,Gb
    public void testXchgEbGb() {
        runRegsb((byte)0x86, 0xF0, 0x0F, false, 0x0F, 0xF0);
        runRegsb((byte)0x86, 0xF0, 0x0F, true, 0xF0, 0xF0);
        runRegb((byte)0x86, 1, 2, false, 2);
    }

    // 0x87
    //XCHG Ew,Gw
    public void testXchgEwGw() {
        runRegsw((byte)0x87, 0xFF00, 0x00FF, false, 0x00FF, 0xFF00);
        runRegsw((byte)0x87, 0xFF00, 0x00FF, true, 0xFF00, 0xFF00);
        runRegw((byte)0x87, 1, 2, false, 2);
    }

    // 0x88
    //MOV Eb,Gb
    public void testMovEbGb() {
        runRegsb((byte)0x88, 0x00, 0x0F, false, 0x0F, 0x00);
        runRegb((byte)0x88, 1, 2, false, 2);
    }

    // 0x89
    //MOV Ew,Gw
    public void testMovEwGw() {
        runRegsw((byte)0x89, 0x00, 0x0FFF, false, 0x0FFF, 0x00);
        runRegw((byte)0x89, 0x1AB, 0x2CE, false, 0x2CE);
    }

    // 0x8A
    //MOV Gb,Eb
    public void testMovGbEb() {
        runRegsb((byte)0x8a, 0x0F, 0x00, true, 0x0F, 0x0F);
        runRegb((byte)0x8a, 1, 2, true, 1);
    }

    // 0x8b
    //MOV Gw,Ew
    public void testMovGwEw() {
        runRegsw((byte)0x8b, 0x0FFF, 0x00, true, 0x0FFF, 0x0FFF);
        runRegw((byte)0x8b, 0x1AB, 0x2CD, true, 0x1AB);
    }

    // 0x8c
    //Mov Ew,Sw
    public void testMovEwSw() {
        newInstruction(0x8c);
        pushIb((byte)0xC0);
        CPU_Regs.SegSet16ES(0x1234);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x1234);

//        newInstruction(0x8c);
//        pushIb((byte)(0xC0 + (1<<3) + 1));
//        decoder.call();
//        assertTrue(CPU_Regs.reg_ecx.dword==0x1234);

        newInstruction(0x8c);
        pushIb((byte)(0xC0 + (2<<3) + 2));
        CPU_Regs.reg_ssVal.dword = 0x1234;
        decoder.call();
        assertTrue(CPU_Regs.reg_edx.dword==0x1234);

        newInstruction(0x8c);
        pushIb((byte)(0xC0 + (3<<3) + 3));
        CPU_Regs.reg_dsVal.dword = 0x1234;
        decoder.call();
        assertTrue(CPU_Regs.reg_ebx.dword==0x1234);

        newInstruction(0x8c);
        pushIb((byte)(0xC0 + (4<<3) + 4));
        CPU_Regs.reg_fsVal.dword = 0x1234;
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x1234);

        newInstruction(0x8c);
        pushIb((byte)(0xC0 + (5<<3) + 5));
        CPU_Regs.reg_gsVal.dword = 0x1234;
        decoder.call();
        assertTrue(CPU_Regs.reg_ebp.dword==0x1234);

        newInstruction(0x8c);
        pushIb((byte)0x0);
        Memory.mem_writew(MEM_BASE_DS-2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0xCDCD);
        CPU_Regs.SegSet16ES(0x1234);
        decoder.call();
        assertTrue(Memory.mem_readw(MEM_BASE_DS)==0x1234);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    // 0x8d
    //LEA Gw
    public void testLeaGw() {
        newInstruction(0x8d);
        CPU_Regs.reg_ebx.dword(0xFF);
        CPU_Regs.reg_esi.dword(0xFFF0);
        pushIb((byte)0x0);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xEF);

        newInstruction(0x67);
        pushIb((byte)0x8d);
        CPU_Regs.reg_ebx.dword(0xFF);
        CPU_Regs.reg_esi.dword(0xFFF0);
        CPU_Regs.reg_ecx.dword(0xABCDEF01);
        pushIb((byte)0x1);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xEF01);
    }

    // 0x8e
    //MOV Sw,Ew
    public void testMovSwEw() {
        newInstruction(0x8e);
        pushIb((byte)0xC1);
        CPU_Regs.reg_ecx.dword(0xFFABCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_esVal.dword == 0xABCD);

        newInstruction(0x8e);
        pushIb((byte)(0xC2 + (2<<3)));
        CPU_Regs.reg_edx.dword(0xFFABCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_ssVal.dword == 0xABCD);

        newInstruction(0x8e);
        pushIb((byte)(0xC3 + (3<<3)));
        CPU_Regs.reg_ebx.dword(0xFFABCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_dsVal.dword == 0xABCD);

        newInstruction(0x8e);
        pushIb((byte)(0xC4 + (4<<3)));
        CPU_Regs.reg_esp.dword(0xFFABCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_fsVal.dword == 0xABCD);

        newInstruction(0x8e);
        pushIb((byte)(0xC5 + (5<<3)));
        CPU_Regs.reg_ebp.dword(0xFFABCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_gsVal.dword == 0xABCD);

        newInstruction(0x8e);
        pushIb((byte)0x0);
        Memory.mem_writew(MEM_BASE_DS-2, 0xCDCD);
        Memory.mem_writew(MEM_BASE_DS, 0xABCD);
        Memory.mem_writew(MEM_BASE_DS+2, 0xCDCD);
        decoder.call();
        assertTrue(CPU_Regs.reg_esVal.dword == 0xABCD);
        Memory.mem_writew(MEM_BASE_DS-2, 0);
        Memory.mem_writew(MEM_BASE_DS, 0);
        Memory.mem_writew(MEM_BASE_DS+2, 0);
    }

    // 0x8f
    //POP Ew
    public void testPopEw() {
        newInstruction(0x8f);
        pushIb((byte)0xC3);
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x89EF);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(CPU_Regs.reg_ebx.dword==0x89EF);

        newInstruction(0x8f);
        pushIb((byte)0x0);
        CPU_Regs.reg_esp.dword(0xFE);
        RAM.writew(CPU_Regs.reg_ssPhys.dword+CPU_Regs.reg_esp.dword, 0x89EF);
        decoder.call();
        assertTrue(CPU_Regs.reg_esp.dword==0x100);
        assertTrue(Memory.mem_readw(MEM_BASE_DS)==0x89EF);
    }

    // 0x90
    //NOP

    // 0x91
    //XCHG CX,AX
    public void testXchgCxAx() {
        newInstruction(0x91);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_ecx.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_ecx.dword==0xBBBB1234);
    }

    // 0x92
    //XCHG DX,AX
    public void testXchgDxAx() {
        newInstruction(0x92);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_edx.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_edx.dword==0xBBBB1234);
    }

    // 0x93
    //XCHG BX,AX
    public void testXchgBxAx() {
        newInstruction(0x93);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_ebx.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_ebx.dword==0xBBBB1234);
    }

    // 0x94
    //XCHG SP,AX
    public void testXchgSpAx() {
        newInstruction(0x94);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_esp.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_esp.dword==0xBBBB1234);
    }

    // 0x95
    //XCHG BP,AX
    public void testXchgBpAx() {
        newInstruction(0x95);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_ebp.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_ebp.dword==0xBBBB1234);
    }

    // 0x96
    //XCHG SI,AX
    public void testXchgSiAx() {
        newInstruction(0x96);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_esi.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_esi.dword==0xBBBB1234);
    }

    // 0x97
    //XCHG DI,AX
    public void testXchgDiAx() {
        newInstruction(0x97);
        CPU_Regs.reg_eax.dword(0xAAAA1234);
        CPU_Regs.reg_edi.dword(0xBBBB5678);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0xAAAA5678);
        assertTrue(CPU_Regs.reg_edi.dword==0xBBBB1234);
    }

    // 0x98
    //CBW
    public void testCbw() {
        newInstruction(0x98);
        CPU_Regs.reg_eax.dword(0x4444AB01);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x44440001);

        newInstruction(0x98);
        CPU_Regs.reg_eax.dword(0x4444ABFE);
        decoder.call();
        assertTrue(CPU_Regs.reg_eax.dword==0x4444FFFE);
    }

    // 0x99
    //CWD
    public void testCwd() {
        newInstruction(0x99);
        CPU_Regs.reg_eax.dword(0xFFF);
        decoder.call();
        assertTrue(CPU_Regs.reg_edx.dword==0);

        newInstruction(0x99);
        CPU_Regs.reg_eax.dword(0xF000);
        decoder.call();
        assertTrue(CPU_Regs.reg_edx.dword==0xFFFF);
    }
}
