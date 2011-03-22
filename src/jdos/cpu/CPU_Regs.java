package jdos.cpu;

import jdos.hardware.Memory;

public class CPU_Regs extends Flags {
    public static final int CF=	0x00000001;
    public static final int PF=	0x00000004;
    public static final int AF=	0x00000010;
    public static final int ZF=	0x00000040;
    public static final int SF=	0x00000080;
    public static final int OF=	0x00000800;

    public static final int TF=	0x00000100;
    public static final int IF=	0x00000200;
    public static final int DF=	0x00000400;

    public static final int IOPL=	0x00003000;
    public static final int NT=	0x00004000;
    public static final int VM=	0x00020000;
    public static final int AC=	0x00040000;
    public static final int ID=	0x00200000;

    public static final int FMASK_TEST	=	(CF | PF | AF | ZF | SF | OF);
    public static final int FMASK_NORMAL=	(FMASK_TEST | DF | TF | IF | AC );
    public static final int FMASK_ALL	=	(FMASK_NORMAL | IOPL | NT);

    public static void SETFLAGBIT(int flag, boolean set) {
        if (set)
            CPU_Regs.flags |= flag;
        else
            CPU_Regs.flags &=~ flag;
    }

    public static int GETFLAG(int flag) {
        return CPU_Regs.flags & flag;
    }

    public static boolean GETFLAGBOOL(int flag) {
        return ((CPU_Regs.flags & flag) != 0);
    }

    public static int GETFLAG_IOPL() {
        return (CPU_Regs.flags & IOPL) >> 12;
    }

    // SegNames
    public static final int es=0;
    public static final int cs=1;
    public static final int ss=2;
    public static final int ds=3;
    public static final int fs=4;
    public static final int gs=5;

//    public static /*PhysPt*/long SegPhys(int index) {
//        return CPU.Segs.phys[index];
//    }
//
//    public static /*Bit16u*/int SegValue(int index) {
//        return (int)(CPU.Segs.val[index] & 0xFFFFl);
//    }
//
    public static /*RealPt*/long RealMakeSegDS(/*Bit16u*/int off) {
        return Memory.RealMake((int)CPU.Segs_DSval,off);
    }

    public static /*RealPt*/long RealMakeSegSS(/*Bit16u*/int off) {
        return Memory.RealMake((int)CPU.Segs_SSval,off);
    }

    public static void SegSet16ES(/*Bit16u*/int val) {
        CPU.Segs_ESval=val;
        CPU.Segs_ESphys=val << 4;
    }

    public static void SegSet16CS(/*Bit16u*/int val) {
        CPU.Segs_CSval=val;
        CPU.Segs_CSphys=val << 4;
    }

    public static void SegSet16SS(/*Bit16u*/int val) {
        CPU.Segs_SSval=val;
        CPU.Segs_SSphys=val << 4;
    }

    public static void SegSet16DS(/*Bit16u*/int val) {
        CPU.Segs_DSval=val;
        CPU.Segs_DSphys=val << 4;
    }

    public static void SegSet16FS(/*Bit16u*/int val) {
        CPU.Segs_FSval=val;
        CPU.Segs_FSphys=val << 4;
    }

    public static void SegSet16GS(/*Bit16u*/int val) {
        CPU.Segs_GSval=val;
        CPU.Segs_GSphys=val << 4;
    }  

    // IP
    public static int reg_ip() {
        return (int)(reg_eip & 0xFFFFl);
    }
    public static void reg_ip(int value) {
        reg_eip = value & 0xFFFF | (reg_eip & 0xFFFF0000l);
    }

    public static long reg_eip() {
        return (reg_eip & 0xFFFFFFFFl);
    }
    public static void reg_eip(long value) {
        reg_eip = value & 0xFFFFFFFFl;
    }
    public CPU_Regs() {
    }

    public static final class Reg {
        final public void dword(long l) {
            dword = l & 0xFFFFFFFFl;
        }
        final public long dword() {
            return dword;
        }
        final public int word() {
            return (int)(dword & 0xFFFFl);
        }
        final public void word(int value) {
            dword = value & 0xFFFF | (dword & 0xFFFF0000l);
        }

        final public short low() {
            return (short)(dword & 0xffl);
        }
        final public void low(int value) {
            dword = (value & 0xFF) | (dword & 0xFFFFFF00l);
        }

        final public short high() {
            return (short)((dword >>> 8) & 0xffl);
        }
        final public void high(int value) {
            dword = ((value & 0xFF) << 8) | (dword & 0xFFFF00FFl);
        }
        public long dword;
    }

    final static public Reg reg_eax = new Reg();
    final static public Reg reg_ebx = new Reg();
    final static public Reg reg_ecx = new Reg();
    final static public Reg reg_edx = new Reg();
    final static public Reg reg_esi = new Reg();
    final static public Reg reg_edi = new Reg();
    final static public Reg reg_esp = new Reg();
    final static public Reg reg_ebp = new Reg();
    static public long reg_eip;

    static public /*Bitu*/int flags;
}
