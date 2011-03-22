package jdos.cpu;

import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Flags {
    public static LazyFlags lflags = new LazyFlags();

    public static short lf_var1b() {
        return (short)(lflags.var1 & 0xFF);
    }

    public static void lf_var1b(long b) {
        lflags.var1&=~0xFF;
        lflags.var1|=b & 0xFF;
    }

    public static short lf_var2b() {
        return (short)(lflags.var2 & 0xFF);
    }

    public static void lf_var2b(long b) {
        lflags.var2&=~0xFF;
        lflags.var2|=b & 0xFF;
    }

    public static short lf_resb() {
        return (short)(lflags.res & 0xFF);
    }

    public static void lf_resb(long b) {
        lflags.res&=~0xFF;
        lflags.res|=b & 0xFF;
    }

    public static int lf_var1w() {
        return (int)(lflags.var1 & 0xFFFF);
    }

    public static void lf_var1w(long s) {
        lflags.var1&=~0xFFFF;
        lflags.var1|=s & 0xFFFF;
    }

    public static int lf_var2w() {
        return (int)(lflags.var2 & 0xFFFF);
    }

    public static void lf_var2w(long s) {
        lflags.var2&=~0xFFFF;
        lflags.var2|=s & 0xFFFF;
    }

    public static int lf_resw() {
        return (int)(lflags.res & 0xFFFF);
    }

    public static void lf_resw(long s) {
        lflags.res&=~0xFFFF;
        lflags.res|=s & 0xFFFF;
    }

    public static long lf_var1d() {
        return lflags.var1;
    }

    public static void lf_var1d(long v) {
        lflags.var1=v & 0xFFFFFFFFl;
    }

    public static long lf_var2d() {
        return lflags.var2;
    }

    public static void lf_var2d(long v) {
        lflags.var2=v & 0xFFFFFFFFl;
    }

    public static long lf_resd() {
        return lflags.res;
    }

    public static void lf_resd(long v) {
        lflags.res=v & 0xFFFFFFFFl;
    }

    public static void SETFLAGSb(int FLAGB) {
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,get_OF());
        lflags.type=t_UNKNOWN;
        CPU.CPU_SetFlags(FLAGB,CPU_Regs.FMASK_NORMAL & 0xff);
    }

    static public void LoadCF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,get_CF());
    }
    static public void LoadZF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,get_ZF());
    }
    static public void LoadSF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF,get_SF());
    }
    static public void LoadOF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,get_OF());
    }
    static public void LoadAF() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.AF,get_AF());
    }

    /* CF     Carry Flag -- Set on high-order bit carry or borrow; cleared
          otherwise.
    */
    static public boolean get_CF() {
        switch (lflags.type) {
        case t_UNKNOWN:
        case t_INCb:
        case t_INCw:
        case t_INCd:
        case t_DECb:
        case t_DECw:
        case t_DECd:
        case t_MUL:
            return CPU_Regs.GETFLAG(CPU_Regs.CF)!=0;
        case t_ADDb:
            return (lf_resb()<lf_var1b());
        case t_ADDw:
            return (lf_resw()<lf_var1w());
        case t_ADDd:
            return (lf_resd()<lf_var1d());
        case t_ADCb:
            return (lf_resb() < lf_var1b()) || (lflags.oldcf && (lf_resb() == lf_var1b()));
        case t_ADCw:
            return (lf_resw() < lf_var1w()) || (lflags.oldcf && (lf_resw() == lf_var1w()));
        case t_ADCd:
            return (lf_resd() < lf_var1d()) || (lflags.oldcf && (lf_resd() == lf_var1d()));
        case t_SBBb:
            return (lf_var1b() < lf_resb()) || (lflags.oldcf && (lf_var2b()==0xff));
        case t_SBBw:
            return (lf_var1w() < lf_resw()) || (lflags.oldcf && (lf_var2w()==0xffff));
        case t_SBBd:
            return (lf_var1d() < lf_resd()) || (lflags.oldcf && (lf_var2d()==0xffffffffl));
        case t_SUBb:
        case t_CMPb:
            return (lf_var1b()<lf_var2b());
        case t_SUBw:
        case t_CMPw:
            return (lf_var1w()<lf_var2w());
        case t_SUBd:
        case t_CMPd:
            return (lf_var1d()<lf_var2d());
        case t_SHLb:
            if (lf_var2b()>8) return false;
            else return ((lf_var1b() >> (8-lf_var2b())) & 1)!=0;
        case t_SHLw:
            if (lf_var2b()>16) return false;
            else return ((lf_var1w()) >> (16-lf_var2b()) & 1)!=0;
        case t_SHLd:
        case t_DSHLw:	/* Hmm this is not correct for shift higher than 16 */
        case t_DSHLd:
            return ((lf_var1d() >>> (32 - lf_var2b())) & 1) != 0;
        case t_RCRb:
        case t_SHRb:
            return ((lf_var1b() >> (lf_var2b() - 1)) & 1) !=0;
        case t_RCRw:
        case t_SHRw:
            return ((lf_var1w() >> (lf_var2b() - 1)) & 1) !=0;
        case t_RCRd:
        case t_SHRd:
        case t_DSHRw:	/* Hmm this is not correct for shift higher than 16 */
        case t_DSHRd:
            return ((lf_var1d() >>> (lf_var2b() - 1)) & 1) != 0;
        case t_SARb:
            return (((lf_var1b()) >> (lf_var2b() - 1)) & 1) != 0;
        case t_SARw:
            return (((lf_var1w()) >> (lf_var2b() - 1)) & 1) != 0;
        case t_SARd:
            return (((lf_var1d()) >> (lf_var2b() - 1)) & 1) != 0;
        case t_NEGb:
            return lf_var1b() != 0;
        case t_NEGw:
            return lf_var1w() != 0;
        case t_NEGd:
            return lf_var1d() != 0;
        case t_ORb:
        case t_ORw:
        case t_ORd:
        case t_ANDb:
        case t_ANDw:
        case t_ANDd:
        case t_XORb:
        case t_XORw:
        case t_XORd:
        case t_TESTb:
        case t_TESTw:
        case t_TESTd:
            return false;	/* Set to false */
        case t_DIV:
            return false;	/* Unkown */
        default:
            Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"get_CF Unknown %d",lflags.type);
        }
        return false;
    }

    /* AF     Adjust flag -- Set on carry from or borrow to the low order
            four bits of   AL; cleared otherwise. Used for decimal
            arithmetic.
    */
    static public boolean get_AF() {
        switch (lflags.type) {
        case t_UNKNOWN:
            return CPU_Regs.GETFLAG(CPU_Regs.AF) != 0;
        case t_ADDb:
        case t_ADCb:
        case t_SBBb:
        case t_SUBb:
        case t_CMPb:
            return (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10) !=0;
        case t_ADDw:
        case t_ADCw:
        case t_SBBw:
        case t_SUBw:
        case t_CMPw:
            return (((lf_var1w() ^ lf_var2w()) ^ lf_resw()) & 0x10) != 0;
        case t_ADCd:
        case t_ADDd:
        case t_SBBd:
        case t_SUBd:
        case t_CMPd:
            return (((lf_var1d() ^ lf_var2d()) ^ lf_resd()) & 0x10) != 0;
        case t_INCb:
            return (lf_resb() & 0x0f) == 0;
        case t_INCw:
            return (lf_resw() & 0x0f) == 0;
        case t_INCd:
            return (lf_resd() & 0x0f) == 0;
        case t_DECb:
            return (lf_resb() & 0x0f) == 0x0f;
        case t_DECw:
            return (lf_resw() & 0x0f) == 0x0f;
        case t_DECd:
            return (lf_resd() & 0x0f) == 0x0f;
        case t_NEGb:
            return (lf_var1b() & 0x0f) != 0;
        case t_NEGw:
            return (lf_var1w() & 0x0f) != 0;
        case t_NEGd:
            return (lf_var1d() & 0x0f) != 0;
        case t_SHLb:
        case t_SHRb:
        case t_SARb:
            return (lf_var2b() & 0x1f) != 0;
        case t_SHLw:
        case t_SHRw:
        case t_SARw:
            return (lf_var2w() & 0x1f) != 0;
        case t_SHLd:
        case t_SHRd:
        case t_SARd:
            return (lf_var2d() & 0x1f) != 0;
        case t_ORb:
        case t_ORw:
        case t_ORd:
        case t_ANDb:
        case t_ANDw:
        case t_ANDd:
        case t_XORb:
        case t_XORw:
        case t_XORd:
        case t_TESTb:
        case t_TESTw:
        case t_TESTd:
        case t_DSHLw:
        case t_DSHLd:
        case t_DSHRw:
        case t_DSHRd:
        case t_DIV:
        case t_MUL:
            return false;			          /* Unkown */
        default:
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"get_AF Unknown %d",lflags.type);
        }
        return false;
    }

    // ZF     Zero Flag -- Set if result is zero; cleared otherwise.
    static public boolean get_ZF() {
        switch (lflags.type) {
        case t_UNKNOWN:
            return CPU_Regs.GETFLAG(CPU_Regs.ZF) != 0;
        case t_ADDb:
        case t_ORb:
        case t_ADCb:
        case t_SBBb:
        case t_ANDb:
        case t_XORb:
        case t_SUBb:
        case t_CMPb:
        case t_INCb:
        case t_DECb:
        case t_TESTb:
        case t_SHLb:
        case t_SHRb:
        case t_SARb:
        case t_NEGb:
            return (lf_resb()==0);
        case t_ADDw:
        case t_ORw:
        case t_ADCw:
        case t_SBBw:
        case t_ANDw:
        case t_XORw:
        case t_SUBw:
        case t_CMPw:
        case t_INCw:
        case t_DECw:
        case t_TESTw:
        case t_SHLw:
        case t_SHRw:
        case t_SARw:
        case t_DSHLw:
        case t_DSHRw:
        case t_NEGw:
            return (lf_resw()==0);
        case t_ADDd:
        case t_ORd:
        case t_ADCd:
        case t_SBBd:
        case t_ANDd:
        case t_XORd:
        case t_SUBd:
        case t_CMPd:
        case t_INCd:
        case t_DECd:
        case t_TESTd:
        case t_SHLd:
        case t_SHRd:
        case t_SARd:
        case t_DSHLd:
        case t_DSHRd:
        case t_NEGd:
            return (lf_resd()==0);
        case t_DIV:
        case t_MUL:
            return false;		/* Unkown */
        default:
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"get_ZF Unknown %d",lflags.type);
        }
        return false;
    }

    /* SF     Sign Flag -- Set equal to high-order bit of result (0 is
            positive, 1 if negative).
    */
    static public boolean get_SF() {
        switch (lflags.type) {
        case t_UNKNOWN:
            return CPU_Regs.GETFLAG(CPU_Regs.SF) != 0;
        case t_ADDb:
        case t_ORb:
        case t_ADCb:
        case t_SBBb:
        case t_ANDb:
        case t_XORb:
        case t_SUBb:
        case t_CMPb:
        case t_INCb:
        case t_DECb:
        case t_TESTb:
        case t_SHLb:
        case t_SHRb:
        case t_SARb:
        case t_NEGb:
            return	(lf_resb()&0x80)!= 0;
        case t_ADDw:
        case t_ORw:
        case t_ADCw:
        case t_SBBw:
        case t_ANDw:
        case t_XORw:
        case t_SUBw:
        case t_CMPw:
        case t_INCw:
        case t_DECw:
        case t_TESTw:
        case t_SHLw:
        case t_SHRw:
        case t_SARw:
        case t_DSHLw:
        case t_DSHRw:
        case t_NEGw:
            return	(lf_resw()&0x8000)!=0;
        case t_ADDd:
        case t_ORd:
        case t_ADCd:
        case t_SBBd:
        case t_ANDd:
        case t_XORd:
        case t_SUBd:
        case t_CMPd:
        case t_INCd:
        case t_DECd:
        case t_TESTd:
        case t_SHLd:
        case t_SHRd:
        case t_SARd:
        case t_DSHLd:
        case t_DSHRd:
        case t_NEGd:
            return	(lf_resd()&0x80000000l)!= 0;
        case t_DIV:
        case t_MUL:
            return false;	/* Unkown */
        default:
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"get_SF Unkown %d",lflags.type);
        }
        return false;
    }

    static public boolean get_OF() {
        switch (lflags.type) {
        case t_UNKNOWN:
        case t_MUL:
            return CPU_Regs.GETFLAG(CPU_Regs.OF) != 0;
        case t_ADDb:
        case t_ADCb:
            return (((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var2b())) & 0x80) != 0;
        case t_ADDw:
        case t_ADCw:
            return (((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var2w())) & 0x8000) != 0;
        case t_ADDd:
        case t_ADCd:
            return (((lf_var1d() ^ lf_var2d() ^ 0x80000000) & (lf_resd() ^ lf_var2d())) & 0x80000000l) != 0;
        case t_SBBb:
        case t_SUBb:
        case t_CMPb:
            return (((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb())) & 0x80) != 0;
        case t_SBBw:
        case t_SUBw:
        case t_CMPw:
            return (((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw())) & 0x8000) != 0;
        case t_SBBd:
        case t_SUBd:
        case t_CMPd:
            return (((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd())) & 0x80000000l) != 0;
        case t_INCb:
            return (lf_resb() == 0x80);
        case t_INCw:
            return (lf_resw() == 0x8000);
        case t_INCd:
            return (lf_resd() == 0x80000000l);
        case t_DECb:
            return (lf_resb() == 0x7f);
        case t_DECw:
            return (lf_resw() == 0x7fff);
        case t_DECd:
            return (lf_resd() == 0x7fffffff);
        case t_NEGb:
            return (lf_var1b() == 0x80);
        case t_NEGw:
            return (lf_var1w() == 0x8000);
        case t_NEGd:
            return (lf_var1d() == 0x80000000l);
        case t_SHLb:
            return ((lf_resb() ^ lf_var1b()) & 0x80) != 0;
        case t_SHLw:
        case t_DSHRw:
        case t_DSHLw:
            return ((lf_resw() ^ lf_var1w()) & 0x8000) != 0;
        case t_SHLd:
        case t_DSHRd:
        case t_DSHLd:
            return ((lf_resd() ^ lf_var1d()) & 0x80000000l) != 0;
        case t_SHRb:
            if ((lf_var2b()&0x1f)==1) return (lf_var1b() > 0x80);
            else return false;
        case t_SHRw:
            if ((lf_var2b()&0x1f)==1) return (lf_var1w() > 0x8000);
            else return false;
        case t_SHRd:
            if ((lf_var2b()&0x1f)==1) return (lf_var1d() > 0x80000000l);
            else return false;
        case t_ORb:
        case t_ORw:
        case t_ORd:
        case t_ANDb:
        case t_ANDw:
        case t_ANDd:
        case t_XORb:
        case t_XORw:
        case t_XORd:
        case t_TESTb:
        case t_TESTw:
        case t_TESTd:
        case t_SARb:
        case t_SARw:
        case t_SARd:
            return false;			/* Return false */
        case t_DIV:
            return false;		/* Unkown */
        default:
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"get_OF Unkown %d",lflags.type);
        }
        return false;
    }

    static short[] parity_lookup = new short[] {
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0,
        CPU_Regs.PF, 0, 0, CPU_Regs.PF, 0, CPU_Regs.PF, CPU_Regs.PF, 0, 0, CPU_Regs.PF, CPU_Regs.PF, 0, CPU_Regs.PF, 0, 0, CPU_Regs.PF
        };

    static public boolean get_PF() {
        switch (lflags.type) {
        case t_UNKNOWN:
            return CPU_Regs.GETFLAG(CPU_Regs.PF) != 0;
        default:
            return	(parity_lookup[lf_resb()]) != 0;
        }
    }

    static private void DOFLAG_PF() {
        CPU_Regs.flags=(CPU_Regs.flags & ~CPU_Regs.PF) | parity_lookup[lf_resb()];
    }

    static private void DOFLAG_AF() {
        CPU_Regs.flags=(CPU_Regs.flags & ~CPU_Regs.AF) | (((lf_var1b() ^ lf_var2b()) ^ lf_resb()) & 0x10);
    }

    static private void DOFLAG_ZFb() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,lf_resb()==0);
    }
    static private void DOFLAG_ZFw() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,lf_resw()==0);
    }
    static private void DOFLAG_ZFd() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,lf_resd()==0);
    }

    static private void DOFLAG_SFb() {
        CPU_Regs.flags=(CPU_Regs.flags & ~CPU_Regs.SF) | ((lf_resb() & 0x80));
    }
    static private void DOFLAG_SFw() {
        CPU_Regs.flags=(CPU_Regs.flags & ~CPU_Regs.SF) | ((lf_resw() & 0x8000) >> 8);
    }
    static private void DOFLAG_SFd() {
        CPU_Regs.flags=(int)((CPU_Regs.flags & ~CPU_Regs.SF) | ((lf_resd() & 0x80000000l) >>> 24));
    }

//    static private void SETCF(int NEWBIT) {
//        CPU_Regs.flags=(CPU_Regs.flags & ~CPU_Regs.CF)|(NEWBIT);
//    }

    static public boolean TFLG_O() {
        return get_OF();
    }

    static public boolean TFLG_NO () {
        return !get_OF();
    }

    static public boolean TFLG_B() {
        return get_CF();
    }

    static public boolean TFLG_NB() {
        return !get_CF();
    }

    static public boolean TFLG_Z() {
        return get_ZF();
    }

    static public boolean TFLG_NZ() {
        return !get_ZF();
    }

    static public boolean TFLG_BE() {
        return (get_CF() || get_ZF());
    }

    static public boolean TFLG_NBE() {
        return (!get_CF() && !get_ZF());
    }

    static public boolean TFLG_S() {
        return get_SF();
    }

    static public boolean TFLG_NS() {
        return !get_SF();
    }

    static public boolean TFLG_P() {
        return get_PF();
    }

    static public boolean TFLG_NP() {
        return !get_PF();
    }

    static public boolean TFLG_L() {
        return ((get_SF()) != (get_OF()));
    }

    static public boolean TFLG_NL() {
        return ((get_SF()) == (get_OF()));
    }

    static public boolean TFLG_LE() {
        return (get_ZF()  || ((get_SF()) != (get_OF())));
    }

    static public boolean TFLG_NLE() {
        return (!get_ZF() && ((get_SF()) == (get_OF())));
    }

    public static final int t_UNKNOWN = 0;
	public static final int t_ADDb = 1;
    public static final int t_ADDw = 2;
    public static final int t_ADDd = 3;
	public static final int t_ORb = 4;
    public static final int t_ORw = 5;
    public static final int t_ORd = 6;
	public static final int t_ADCb = 7;
    public static final int t_ADCw = 8;
    public static final int t_ADCd = 9;
	public static final int t_SBBb = 10;
    public static final int t_SBBw = 11;
    public static final int t_SBBd = 12;
	public static final int t_ANDb = 13;
    public static final int t_ANDw = 14;
    public static final int t_ANDd = 15;
	public static final int t_SUBb = 16;
    public static final int t_SUBw = 17;
    public static final int t_SUBd = 18;
	public static final int t_XORb = 19;
    public static final int t_XORw = 20;
    public static final int t_XORd = 21;
	public static final int t_CMPb = 22;
    public static final int t_CMPw = 23;
    public static final int t_CMPd = 24;
	public static final int t_INCb = 25;
    public static final int t_INCw = 26;
    public static final int t_INCd = 27;
	public static final int t_DECb = 28;
    public static final int t_DECw = 29;
    public static final int t_DECd = 30;
	public static final int t_TESTb = 31;
    public static final int t_TESTw = 32;
    public static final int t_TESTd = 33;
	public static final int t_SHLb = 34;
    public static final int t_SHLw = 35;
    public static final int t_SHLd = 36;
	public static final int t_SHRb = 37;
    public static final int t_SHRw = 38;
    public static final int t_SHRd = 39;
	public static final int t_SARb = 40;
    public static final int t_SARw = 41;
    public static final int t_SARd = 42;
	public static final int t_ROLb = 43;
    public static final int t_ROLw = 44;
    public static final int t_ROLd = 45;
	public static final int t_RORb = 46;
    public static final int t_RORw = 47;
    public static final int t_RORd = 48;
	public static final int t_RCLb = 49;
    public static final int t_RCLw = 50;
    public static final int t_RCLd = 51;
	public static final int t_RCRb = 52;
    public static final int t_RCRw = 53;
    public static final int t_RCRd = 54;
	public static final int t_NEGb = 55;
    public static final int t_NEGw = 56;
    public static final int t_NEGd = 57;
	public static final int t_DSHLw = 58;
    public static final int t_DSHLd = 59;
	public static final int t_DSHRw = 60;
    public static final int t_DSHRd = 61;
	public static final int t_MUL = 62;
    public static final int t_DIV = 63;
	public static final int t_NOTDONE = 64;
	public static final int t_LASTFLAG = 65;

    static private void SET_FLAG(int flag, boolean set) {
        CPU_Regs.SETFLAGBIT(flag, set);
    }

    static public /*Bitu*/int FillFlags() {
        switch (lflags.type) {
        case t_UNKNOWN:
            break;
        case t_ADDb:
            SET_FLAG(CPU_Regs.CF,(lf_resb()<lf_var1b()));
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,(((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var1b())) & 0x80)!=0);
            DOFLAG_PF();
            break;
        case t_ADDw:
            SET_FLAG(CPU_Regs.CF,(lf_resw()<lf_var1w()));
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,(((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var1w())) & 0x8000)!=0);
            DOFLAG_PF();
            break;
        case t_ADDd:
            SET_FLAG(CPU_Regs.CF,(lf_resd()<lf_var1d()));
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,(((lf_var1d() ^ lf_var2d() ^ 0x80000000) & (lf_resd() ^ lf_var1d())) & 0x80000000l)!=0);
            DOFLAG_PF();
            break;
        case t_ADCb:
            SET_FLAG(CPU_Regs.CF,(lf_resb() < lf_var1b()) || (lflags.oldcf && (lf_resb() == lf_var1b())));
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,(((lf_var1b() ^ lf_var2b() ^ 0x80) & (lf_resb() ^ lf_var1b())) & 0x80)!=0);
            DOFLAG_PF();
            break;
        case t_ADCw:
            SET_FLAG(CPU_Regs.CF,(lf_resw() < lf_var1w()) || (lflags.oldcf && (lf_resw() == lf_var1w())));
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,(((lf_var1w() ^ lf_var2w() ^ 0x8000) & (lf_resw() ^ lf_var1w())) & 0x8000)!=0);
            DOFLAG_PF();
            break;
        case t_ADCd:
            SET_FLAG(CPU_Regs.CF,(lf_resd() < lf_var1d()) || (lflags.oldcf && (lf_resd() == lf_var1d())));
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,(((lf_var1d() ^ lf_var2d() ^ 0x80000000) & (lf_resd() ^ lf_var1d())) & 0x80000000l)!=0);
            DOFLAG_PF();
            break;


        case t_SBBb:
            SET_FLAG(CPU_Regs.CF,(lf_var1b() < lf_resb()) || (lflags.oldcf && (lf_var2b()==0xff)));
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb()) & 0x80)!=0);
            DOFLAG_PF();
            break;
        case t_SBBw:
            SET_FLAG(CPU_Regs.CF,(lf_var1w() < lf_resw()) || (lflags.oldcf && (lf_var2w()==0xffff)));
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw()) & 0x8000)!=0);
            DOFLAG_PF();
            break;
        case t_SBBd:
            SET_FLAG(CPU_Regs.CF,(lf_var1d() < lf_resd()) || (lflags.oldcf && (lf_var2d()==0xffffffffl)));
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd()) & 0x80000000l)!=0);
            DOFLAG_PF();
            break;


        case t_SUBb:
        case t_CMPb:
            SET_FLAG(CPU_Regs.CF,(lf_var1b()<lf_var2b()));
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,((lf_var1b() ^ lf_var2b()) & (lf_var1b() ^ lf_resb()) & 0x80)!=0);
            DOFLAG_PF();
            break;
        case t_SUBw:
        case t_CMPw:
            SET_FLAG(CPU_Regs.CF,(lf_var1w()<lf_var2w()));
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,((lf_var1w() ^ lf_var2w()) & (lf_var1w() ^ lf_resw()) & 0x8000)!=0);
            DOFLAG_PF();
            break;
        case t_SUBd:
        case t_CMPd:
            SET_FLAG(CPU_Regs.CF,(lf_var1d()<lf_var2d()));
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,((lf_var1d() ^ lf_var2d()) & (lf_var1d() ^ lf_resd()) & 0x80000000l)!=0);
            DOFLAG_PF();
            break;


        case t_ORb:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;
        case t_ORw:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;
        case t_ORd:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;


        case t_TESTb:
        case t_ANDb:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;
        case t_TESTw:
        case t_ANDw:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;
        case t_TESTd:
        case t_ANDd:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;


        case t_XORb:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;
        case t_XORw:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;
        case t_XORd:
            SET_FLAG(CPU_Regs.CF,false);
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            break;


        case t_SHLb:
            if (lf_var2b()>8) SET_FLAG(CPU_Regs.CF,false);
            else SET_FLAG(CPU_Regs.CF,((lf_var1b() >> (8-lf_var2b())) & 1)!=0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,((lf_resb() ^ lf_var1b()) & 0x80)!=0);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2b()&0x1f))!=0);
            break;
        case t_SHLw:
            if (lf_var2b()>16) SET_FLAG(CPU_Regs.CF,false);
            else SET_FLAG(CPU_Regs.CF,((lf_var1w() >> (16-lf_var2b())) & 1)!=0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,((lf_resw() ^ lf_var1w()) & 0x8000)!=0);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2w()&0x1f))!=0);
            break;
        case t_SHLd:
            SET_FLAG(CPU_Regs.CF,((lf_var1d() >>> (32 - lf_var2b())) & 1)!=0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,((lf_resd() ^ lf_var1d()) & 0x80000000l)!=0);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2d()&0x1f))!=0);
            break;


        case t_DSHLw:
            SET_FLAG(CPU_Regs.CF,((lf_var1d() >>> (32 - lf_var2b())) & 1)!=0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,((lf_resw() ^ lf_var1w()) & 0x8000)!=0);
            DOFLAG_PF();
            break;
        case t_DSHLd:
            SET_FLAG(CPU_Regs.CF,((lf_var1d() >>> (32 - lf_var2b())) & 1)!=0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,((lf_resd() ^ lf_var1d()) & 0x80000000l)!=0);
            DOFLAG_PF();
            break;


        case t_SHRb:
            SET_FLAG(CPU_Regs.CF,((lf_var1b() >> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            if ((lf_var2b()&0x1f)==1) SET_FLAG(CPU_Regs.OF,(lf_var1b() >= 0x80));
            else SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2b()&0x1f))!=0);
            break;
        case t_SHRw:
            SET_FLAG(CPU_Regs.CF,((lf_var1w() >> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            if ((lf_var2w()&0x1f)==1) SET_FLAG(CPU_Regs.OF,(lf_var1w() >= 0x8000));
            else SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2w()&0x1f))!=0);
            break;
        case t_SHRd:
            SET_FLAG(CPU_Regs.CF,((lf_var1d() >>> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            if ((lf_var2d()&0x1f)==1) SET_FLAG(CPU_Regs.OF,(lf_var1d() >= 0x80000000l));
            else SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2d()&0x1f))!=0);
            break;


        case t_DSHRw:	/* Hmm this is not correct for shift higher than 16 */
            SET_FLAG(CPU_Regs.CF,((lf_var1d() >>> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,((lf_resw() ^ lf_var1w()) & 0x8000)!=0);
            DOFLAG_PF();
            break;
        case t_DSHRd:
            SET_FLAG(CPU_Regs.CF,((lf_var1d() >>> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,((lf_resd() ^ lf_var1d()) & 0x80000000l)!=0);
            DOFLAG_PF();
            break;


        case t_SARb:
            SET_FLAG(CPU_Regs.CF,(((lf_var1b()) >> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2b()&0x1f))!=0);
            break;
        case t_SARw:
            SET_FLAG(CPU_Regs.CF,(((lf_var1w()) >> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2w()&0x1f))!=0);
            break;
        case t_SARd:
            SET_FLAG(CPU_Regs.CF,(((lf_var1d()) >>> (lf_var2b() - 1)) & 1)!=0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,false);
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2d()&0x1f))!=0);
            break;

        case t_INCb:
            SET_FLAG(CPU_Regs.AF,(lf_resb() & 0x0f) == 0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,(lf_resb() == 0x80));
            DOFLAG_PF();
            break;
        case t_INCw:
            SET_FLAG(CPU_Regs.AF,(lf_resw() & 0x0f) == 0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,(lf_resw() == 0x8000));
            DOFLAG_PF();
            break;
        case t_INCd:
            SET_FLAG(CPU_Regs.AF,(lf_resd() & 0x0f) == 0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,(lf_resd() == 0x80000000l));
            DOFLAG_PF();
            break;

        case t_DECb:
            SET_FLAG(CPU_Regs.AF,(lf_resb() & 0x0f) == 0x0f);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,(lf_resb() == 0x7f));
            DOFLAG_PF();
            break;
        case t_DECw:
            SET_FLAG(CPU_Regs.AF,(lf_resw() & 0x0f) == 0x0f);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,(lf_resw() == 0x7fff));
            DOFLAG_PF();
            break;
        case t_DECd:
            SET_FLAG(CPU_Regs.AF,(lf_resd() & 0x0f) == 0x0f);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,(lf_resd() == 0x7fffffff));
            DOFLAG_PF();
            break;

        case t_NEGb:
            SET_FLAG(CPU_Regs.CF,(lf_var1b()!=0));
            SET_FLAG(CPU_Regs.AF,(lf_resb() & 0x0f) != 0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            SET_FLAG(CPU_Regs.OF,(lf_var1b() == 0x80));
            DOFLAG_PF();
            break;
        case t_NEGw:
            SET_FLAG(CPU_Regs.CF,(lf_var1w()!=0));
            SET_FLAG(CPU_Regs.AF,(lf_resw() & 0x0f) != 0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            SET_FLAG(CPU_Regs.OF,(lf_var1w() == 0x8000));
            DOFLAG_PF();
            break;
        case t_NEGd:
            SET_FLAG(CPU_Regs.CF,(lf_var1d()!=0));
            SET_FLAG(CPU_Regs.AF,(lf_resd() & 0x0f) != 0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            SET_FLAG(CPU_Regs.OF,(lf_var1d() == 0x80000000l));
            DOFLAG_PF();
            break;


        case t_DIV:
        case t_MUL:
            break;

        default:
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled flag type %d",lflags.type);
            return 0;
        }
        lflags.type=t_UNKNOWN;
        return CPU_Regs.flags;
    }

    public static void FillFlagsNoCFOF() {
        switch (lflags.type) {
        case t_UNKNOWN:
            return;
        case t_ADDb:
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_ADDw:
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_ADDd:
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;
        case t_ADCb:
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_ADCw:
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_ADCd:
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_SBBb:
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_SBBw:
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_SBBd:
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_SUBb:
        case t_CMPb:
            DOFLAG_AF();
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_SUBw:
        case t_CMPw:
            DOFLAG_AF();
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_SUBd:
        case t_CMPd:
            DOFLAG_AF();
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_ORb:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_ORw:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_ORd:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_TESTb:
        case t_ANDb:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_TESTw:
        case t_ANDw:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_TESTd:
        case t_ANDd:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_XORb:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_XORw:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_XORd:
            SET_FLAG(CPU_Regs.AF,false);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_SHLb:
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2b()&0x1f))!=0);
            break;
        case t_SHLw:
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2w()&0x1f))!=0);
            break;
        case t_SHLd:
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2d()&0x1f))!=0);
            break;


        case t_DSHLw:
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_DSHLd:
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_SHRb:
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2b()&0x1f))!=0);
            break;
        case t_SHRw:
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2w()&0x1f))!=0);
            break;
        case t_SHRd:
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2d()&0x1f))!=0);
            break;


        case t_DSHRw:	/* Hmm this is not correct for shift higher than 16 */
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_DSHRd:
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_SARb:
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2b()&0x1f))!=0);
            break;
        case t_SARw:
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2w()&0x1f))!=0);
            break;
        case t_SARd:
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            SET_FLAG(CPU_Regs.AF,((lf_var2d()&0x1f))!=0);
            break;

        case t_INCb:
            SET_FLAG(CPU_Regs.AF,(lf_resb() & 0x0f) == 0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_INCw:
            SET_FLAG(CPU_Regs.AF,(lf_resw() & 0x0f) == 0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_INCd:
            SET_FLAG(CPU_Regs.AF,(lf_resd() & 0x0f) == 0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;

        case t_DECb:
            SET_FLAG(CPU_Regs.AF,(lf_resb() & 0x0f) == 0x0f);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_DECw:
            SET_FLAG(CPU_Regs.AF,(lf_resw() & 0x0f) == 0x0f);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_DECd:
            SET_FLAG(CPU_Regs.AF,(lf_resd() & 0x0f) == 0x0f);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;

        case t_NEGb:
            SET_FLAG(CPU_Regs.AF,(lf_resb() & 0x0f) != 0);
            DOFLAG_ZFb();
            DOFLAG_SFb();
            DOFLAG_PF();
            break;
        case t_NEGw:
            SET_FLAG(CPU_Regs.AF,(lf_resw() & 0x0f) != 0);
            DOFLAG_ZFw();
            DOFLAG_SFw();
            DOFLAG_PF();
            break;
        case t_NEGd:
            SET_FLAG(CPU_Regs.AF,(lf_resd() & 0x0f) != 0);
            DOFLAG_ZFd();
            DOFLAG_SFd();
            DOFLAG_PF();
            break;


        case t_DIV:
        case t_MUL:
            break;

        default:
            Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Unhandled flag type %d",lflags.type);
            break;
        }
        lflags.type=t_UNKNOWN;
    }

    static public void DestroyConditionFlags() {
        lflags.type=t_UNKNOWN;
    }

}
