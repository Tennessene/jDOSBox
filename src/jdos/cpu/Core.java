package jdos.cpu;

import jdos.hardware.Memory;

public abstract class Core extends CPU_Regs {
    static public /*Bitu*/int opcode_index;
    static public /*PhysPt*/int cseip;
    static public int base_val_ds;
    static public boolean rep_zero;

    static public /*PhysPt*/int base_ds,base_ss;
    static public /*Bitu*/int prefixes;
    static public Table_ea.GetEAHandler[] ea_table;

    static public short Fetchb() {
        return Memory.mem_readb(cseip++);
    }

    static public byte Fetchbs() {
        return (byte)Memory.mem_readb(cseip++);
    }

    static public int Fetchw() {
        int temp = Memory.mem_readw(cseip);
        cseip+=2;
        return temp;
    }

    static public short Fetchws() {
        short temp = (short)Memory.mem_readw(cseip);
        cseip+=2;
        return temp;
    }
    static public int Fetchd() {
        int temp = Memory.mem_readd(cseip);
        cseip+=4;
        return temp;
    }
    static public int Fetchds() {
        int temp = Memory.mem_readd(cseip);
        cseip+=4;
        return temp;
    }
    static public void DO_PREFIX_SEG_ES() {
        base_ds=CPU.Segs_ESphys;
        base_ss=CPU.Segs_ESphys;
        base_val_ds=CPU_Regs.es;
    }

    static public void DO_PREFIX_SEG_CS() {
        base_ds=CPU.Segs_CSphys;
        base_ss=CPU.Segs_CSphys;
        base_val_ds=CPU_Regs.cs;
    }

    static public void DO_PREFIX_SEG_SS() {
        base_ds=CPU.Segs_SSphys;
        base_ss=CPU.Segs_SSphys;
        base_val_ds=CPU_Regs.ss;
    }

    static public void DO_PREFIX_SEG_DS() {
        base_ds=CPU.Segs_DSphys;
        base_ss=CPU.Segs_DSphys;
        base_val_ds=CPU_Regs.ds;
    }

    static public void DO_PREFIX_SEG_FS() {
        base_ds=CPU.Segs_FSphys;
        base_ss=CPU.Segs_FSphys;
        base_val_ds=CPU_Regs.fs;
    }

    static public void DO_PREFIX_SEG_GS() {
        base_ds=CPU.Segs_GSphys;
        base_ss=CPU.Segs_GSphys;
        base_val_ds=CPU_Regs.gs;
    }

    final static public int PREFIX_ADDR = 0x1;
    final static public int PREFIX_REP = 0x2;
}
