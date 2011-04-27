package jdos.cpu;

import jdos.cpu.core_normal.Prefix_helpers;
import jdos.util.LongHelper;
import jdos.hardware.Memory;

public class Instructions extends Table_ea {
    static public interface loadb {
        public short call();
    }
    static public interface saveb {
        public void call(short value);
    }

    static public interface loadw {
        public int call();
    }
    static public interface savew {
        public void call(int value);
    }

    static public interface loadd {
        public long call();
    }
    static public interface saved {
        public void call(long value);
    }

    static public interface Instruction2w {
        public void call(int op2, int op3, loadw l, savew s);
    }
    static public interface Instruction2d {
        public void call(long op2, long op3, loadd l, saved s);
    }
    static public interface Instruction1w {
        public void call(int op2, loadw l, savew s);
    }
    static public interface Instruction1d {
        public void call(long op2, loadd l, saved s);
    }
    static public interface Instruction1n {
        public void call(long op1);
    }
    static public interface Instruction0b {
        public void call(loadb l, saveb s);
    }
    static public interface Instruction0w {
        public void call(loadw l, savew s);
    }
    static public interface Instruction0d {
        public void call(loadd l, saved s);
    }
    static public short ADDB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l+op2) & 0xFF;
        lflags.type=t_ADDb;
        return (short)lflags.res;
    }

    static public short ADCB(short op2, short l) {
        lflags.oldcf=get_CF();
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l+op2+(lflags.oldcf?1:0)) & 0xFF;
        lflags.type=t_ADCb;
        return (short)lflags.res;
    }

    static public short SBBB(short op2, short l) {
        lflags.oldcf=get_CF();
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l-(op2+(lflags.oldcf?1:0))) & 0xFF;
        lflags.type=t_SBBb;
        return (short)lflags.res;
    }

    static public short SUBB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l-op2) & 0xFF;
        lflags.type=t_SUBb;
        return (short)lflags.res;
    }

    static public short ORB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l | op2) & 0xFF;
        lflags.type=t_ORb;
        return (short)lflags.res;
    }

    static public short XORB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l ^ op2) & 0xFF;
        lflags.type=t_XORb;
        return (short)lflags.res;
    }

    static public short ANDB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l & op2) & 0xFF;
        lflags.type=t_ANDb;
        return (short)lflags.res;
    }

    static public void CMPB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l-op2) & 0xFF;
        lflags.type=t_CMPb;
    }

    static public void TESTB(short op2, short l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l & op2) & 0xFF;
        lflags.type=t_TESTb;
    }

    /* All Word General instructions */

    static public int ADDW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l+op2) & 0xFFFF;
        lflags.type=t_ADDw;
        return (int)lflags.res;
    }

    static public int ADCW(int op2, int l) {
        lflags.oldcf=get_CF();
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l+op2+(lflags.oldcf?1:0)) & 0xFFFF;
        lflags.type=t_ADCw;
        return (int)lflags.res;
    }

    static public int SBBW(int op2, int l) {
        lflags.oldcf=get_CF();
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l-(op2+(lflags.oldcf?1:0))) & 0xFFFF;
        lflags.type=t_SBBw;
        return (int)lflags.res;
    }

    static public int SUBW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l-op2) & 0xFFFF;
        lflags.type=t_SUBw;
        return (int)lflags.res;
    }

    static public int ORW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l | op2) & 0xFFFF;
        lflags.type=t_ORw;
        return (int)lflags.res;
    }

    static public int XORW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l ^ op2) & 0xFFFF;
        lflags.type=t_XORw;
        return (int)lflags.res;
    }

    static public int ANDW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l & op2) & 0xFFFF;
        lflags.type=t_ANDw;
        return (int)lflags.res;
    }

    static public void CMPW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l-op2) & 0xFFFF;
        lflags.type=t_CMPw;
    }

    static public void TESTW(int op2, int l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res=(l & op2) & 0xFFFF;
        lflags.type=t_TESTw;
    }


    /* All DWORD General Instructions */

    static public long ADDD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l+op2) & 0xFFFFFFFFl;
        lflags.type=t_ADDd;
        return lflags.res;
    }

    static public long ADCD(long op2, long l) {
        lflags.oldcf=get_CF();
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l + op2+(lflags.oldcf?1:0)) & 0xFFFFFFFFl;
        lflags.type=t_ADCd;
        return lflags.res;
    }

    static public long SBBD(long op2, long l) {
        lflags.oldcf=get_CF();
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l - (op2+(lflags.oldcf?1:0))) & 0xFFFFFFFFl;
        lflags.type=t_SBBd;
        return lflags.res;
    }

    static public long SUBD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l-op2) & 0xFFFFFFFFl;
        lflags.type=t_SUBd;
        return lflags.res;
    }

    static public long ORD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l | op2) & 0xFFFFFFFFl;
        lflags.type=t_ORd;
        return lflags.res;
    }

    static public long XORD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l ^ op2) & 0xFFFFFFFFl;
        lflags.type=t_XORd;
        return lflags.res;
    }

    static public long ANDD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l & op2) & 0xFFFFFFFFl;
        lflags.type=t_ANDd;
        return lflags.res;
    }

    static public void CMPD(long op2, long op1) {
        lflags.var1=op1;lflags.var2=op2;
        lflags.res = (op1-op2) & 0xFFFFFFFFl;
        lflags.type=t_CMPd;
    }

    static public void TESTD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l & op2) & 0xFFFFFFFFl;
        lflags.type=t_TESTd;
    }


    static public short INCB(short l) {
        LoadCF();lf_var1b(l);
        lf_resb(lf_var1b()+1);
        lflags.type=t_INCb;
        return lf_resb();
    }

    static public int INCW(int l) {
        LoadCF();lf_var1w(l);
        lf_resw(lf_var1w()+1);
        lflags.type=t_INCw;
        return lf_resw();
    }

    static public long INCD(long l) {
        LoadCF();lf_var1d(l);
        lf_resd(lf_var1d()+1);
        lflags.type=t_INCd;
        return lf_resd();
    }

    static public short DECB(short l) {
        LoadCF();lflags.var1=l;
        lflags.res = (l-1) & 0xFF;
        lflags.type=t_DECb;
        return (short)lflags.res;
    }

    static public int DECW(int l) {
        LoadCF();lflags.var1 = l;
        lflags.res = (l-1) & 0xFFFF;
        lflags.type=t_DECw;
        return (int)lflags.res;
    }

    static public long DECD(long l) {
        LoadCF();lflags.var1 = l;
        lflags.res = (l-1) & 0xFFFFFFFFl;
        lflags.type=t_DECd;
        return lflags.res;
    }

    static public boolean valid_ROLB(long op1, short op2) {
        if ((op2&0x7)==0) {
            if ((op2&0x18)!=0) {
                FillFlagsNoCFOF();
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(op1 & 1)!=0);
                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((op1 & 1) ^ (op1 >> 7))!=0);
            }
            return false;
        }
        return true;
    }
    static public short do_ROLB(short op2, short l) {
        FillFlagsNoCFOF();
        lf_var1b(l);
        lf_var2b(op2&0x07);
        lf_resb((lf_var1b() << lf_var2b()) |
                (lf_var1b() >> (8-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(lf_resb() & 1)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lf_resb() & 1) ^ (lf_resb() >> 7))!=0);
        return lf_resb();
    }
    static public void ROLB(long op1, short op2, loadb l, saveb s) {
        if (valid_ROLB(op1, op2)) {
            s.call(do_ROLB(op2, l.call()));
        }
    }
    static public boolean valid_ROLW(long op1, int op2) {
        if ((op2&0xf)==0) {
            if ((op2&0x10)!=0) {
                FillFlagsNoCFOF();
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(op1 & 1)!=0);
                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((op1 & 1) ^ (op1 >> 15))!=0);
            }
            return false;
        }
        return true;
    }
    static public int do_ROLW(int op2, int l) {
        FillFlagsNoCFOF();
        lf_var1w(l);
        lf_var2b(op2&0xf);
        lf_resw((lf_var1w() << lf_var2b()) |
                (lf_var1w() >> (16-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(lf_resw() & 1)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lf_resw() & 1) ^ (lf_resw() >> 15))!=0);
        return lf_resw();
    }
    static public void ROLW(long op1, int op2, loadw l, savew s) {
        if (valid_ROLW(op1, op2)) {
            s.call(do_ROLW(op2, l.call()));
        }
    }

    static public long ROLD(long op2, long l) {
        FillFlagsNoCFOF();
        lflags.var1=l;
        lflags.var2=op2;
        lflags.res=((l << op2) | (l >> (32-op2))) & 0xFFFFFFFFl;
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(lflags.res & 1)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lflags.res & 1) ^ (lflags.res >> 31))!=0);
        return lflags.res;
    }

    static public boolean valid_RORB(long op1, short op2) {
        if ((op2&0x7)==0) {
            if ((op2&0x18)!=0) {
                FillFlagsNoCFOF();
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(op1>>7)!=0);
                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((op1>>7) ^ ((op1>>6) & 1))!=0);
            }
            return false;
        }
        return true;
    }
    static public short do_RORB(short op2, short l) {

        FillFlagsNoCFOF();
        lf_var1b(l);
        lf_var2b(op2&0x07);
        lf_resb((lf_var1b() >> lf_var2b()) |
                (lf_var1b() << (8-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(lf_resb() & 0x80)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lf_resb() ^ (lf_resb()<<1)) & 0x80)!=0);
        return lf_resb();
    }

    static public void RORB(long op1, short op2, loadb l, saveb s) {
        if (valid_RORB(op1, op2)) {
            s.call(do_RORB(op2, l.call()));
        }
    }
    static public boolean valid_RORW(long op1, int op2) {
        if ((op2&0xf)==0) {
            if ((op2&0x10)!=0) {
                FillFlagsNoCFOF();
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(op1>>15)!=0);
                CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((op1>>15) ^ ((op1>>14) & 1))!=0);
            }
            return false;
        }
        return true;
    }
    static public int do_RORW(int op2, int l) {
        FillFlagsNoCFOF();
        lf_var1w(l);
        lf_var2b(op2&0xf);
        lf_resw((lf_var1w() >> lf_var2b()) |
                (lf_var1w() << (16-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(lf_resw() & 0x8000)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lf_resw() ^ (lf_resw()<<1)) & 0x8000)!=0);
        return lf_resw();
    }
    static public void RORW(long op1, int op2, loadw l, savew s) {
        if (valid_RORW(op1, op2))
            s.call(do_RORW(op2, l.call()));
    }

    static public long RORD(long op2, long l) {
        FillFlagsNoCFOF();
        lflags.var1=l;
        lflags.var2=op2;
        lflags.res=((l >> op2) | (l << (32-op2))) & 0xFFFFFFFFl;
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(lflags.res & 0x80000000l)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lflags.res ^ (lflags.res<<1)) & 0x80000000l)!=0);
        return lflags.res;
    }

    static public boolean valid_RCLB(short op2) {
        return (op2%9)!=0;
    }

    static public short do_RCLB(short op2, short l) {
        /*Bit8u*/int cf=(/*Bit8u*/int)FillFlags()&0x1;
        lf_var1b(l);
        lf_var2b(op2%9);
        lf_resb((lf_var1b() << lf_var2b()) |
                (cf << (lf_var2b()-1)) |
                (lf_var1b() >> (9-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((lf_var1b() >> (8-lf_var2b())) & 1))!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (lf_resb() >> 7))!=0);
        return lf_resb();
    }

    static public void RCLB(short op2, loadb l, saveb s) {
        if (valid_RCLB(op2)) {
            s.call(do_RCLB(op2, l.call()));
        }
    }
    static public boolean valid_RCLW(int op2) {
        return (op2%17)!=0;
    }
    static public int do_RCLW(int op2, int l) {
        /*Bit16u*/int cf=(/*Bit16u*/int)FillFlags()&0x1;
        lf_var1w(l);
        lf_var2b(op2%17);
        lf_resw((lf_var1w() << lf_var2b()) |
                (cf << (lf_var2b()-1)) |
                (lf_var1w() >> (17-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((lf_var1w() >> (16-lf_var2b())) & 1))!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (lf_resw() >> 15))!=0);
        return lf_resw();
    }
    static public void RCLW(int op2, loadw l, savew s) {
        if (valid_RCLW(op2)) {
            s.call(do_RCLW(op2, l.call()));
        }
    }

    static public long RCLD(long op2, long l) {
        /*Bit32u*/int cf=FillFlags()&0x1;
        lflags.var1=l;
        lflags.var2=op2;
        if (op2==1)	{
            lflags.res = ((l << 1) | cf) & 0xFFFFFFFFl;
        } else 	{
            lflags.res = ((l << op2) |(cf << (op2-1)) | (l >> (33-op2))) & 0xFFFFFFFFl;
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(((l >> (32-op2)) & 1))!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.flags & 1) ^ (lflags.res >> 31))!=0);
        return lflags.res;
    }

    static public boolean valid_RCRB(short op2) {
        return (op2%9)!=0;
    }
    static public short do_RCRB(short op2, short l) {
        /*Bit8u*/int cf=FillFlags()&0x1;
        lf_var1b(l);
        lf_var2b(op2%9);
        lf_resb((lf_var1b() >> lf_var2b()) |
                (cf << (8-lf_var2b())) |
                (lf_var1b() << (9-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,((lf_var1b() >> (lf_var2b() - 1)) & 1)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lf_resb() ^ (lf_resb()<<1)) & 0x80)!=0);
        return lf_resb();
    }
    static public void RCRB(short op2, loadb l, saveb s) {
        if (valid_RCRB(op2))
            s.call(do_RCRB(op2, l.call()));
    }

    static public boolean valid_RCRW(int op2) {
        return (op2%17)!=0;
    }
    static public int do_RCRW(int op2, int l) {
        /*Bit16u*/int cf=FillFlags()&0x1;
        lf_var1w(l);
        lf_var2b(op2%17);
        lf_resw((lf_var1w() >> lf_var2b()) |
                (cf << (16-lf_var2b())) |
                (lf_var1w() << (17-lf_var2b())));
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,((lf_var1w() >> (lf_var2b() - 1)) & 1)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lf_resw() ^ (lf_resw()<<1)) & 0x8000)!=0);
        return lf_resw();
    }
    static public void RCRW(int op2, loadw l, savew s) {
        if (valid_RCRW(op2))
            s.call(do_RCRW(op2, l.call()));
    }

    static public long RCRD(long op2, long l) {
        /*Bit32u*/int cf=FillFlags()&0x1;
        lflags.var1 = l;
        lflags.var2 = op2;
        if (op2==1) {
            lflags.res = (l >> 1 | cf << 31) & 0xFFFFFFFFl;
        } else {
            lflags.res = ((l >> op2) | (cf << (32-op2)) | (l << (33-op2))) & 0xFFFFFFFFl;
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,((l >> (op2 - 1)) & 1)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((lflags.res ^ (lflags.res<<1)) & 0x80000000)!=0);
        return lflags.res;
    }

    static public boolean valid_SHLB(short op2) {
        return op2!=0;
    }

    static public short do_SHLB(short op2, short l) {
        lf_var1b(l);lf_var2b(op2);
        lf_resb(lf_var1b() << lf_var2b());
        lflags.type=t_SHLb;
        return lf_resb();
    }

    static public void SHLB(short op2, loadb l, saveb s) {
        if (valid_SHLB(op2))
            s.call(do_SHLB(op2, l.call()));
    }
    static public boolean valid_SHLW(int op2) {
        return op2!=0;
    }
    static public int do_SHLW(int op2, int l) {
        lf_var1w(l);lf_var2b(op2);
        lf_resw(lf_var1w() << lf_var2b());
        lflags.type=t_SHLw;
        return lf_resw();
    }
    static public void SHLW(int op2, loadw l, savew s) {
        if (valid_SHLW(op2))
            s.call(do_SHLW(op2, l.call()));
    }

    static public long SHLD(long op2, long l) {
        lflags.var1=l;lflags.var2=op2;
        lflags.res = (l << op2) & 0xFFFFFFFFl;
        lflags.type=t_SHLd;
        return lflags.res;
    }

    static public boolean valid_SHRB(short op2) {
        return op2!=0;
    }

    static public short do_SHRB(short op2, short l) {
        lf_var1b(l);lf_var2b(op2);
        lf_resb(lf_var1b() >> lf_var2b());
        lflags.type=t_SHRb;
        return lf_resb();
    }

    static public void SHRB(short op2, loadb l, saveb s) {
        if (valid_SHRB(op2))
            s.call(do_SHRB(op2, l.call()));
    }
    static public boolean valid_SHRW(int op2) {
        return op2!=0;
    }
    static public int do_SHRW(int op2, int l) {
        lf_var1w(l);lf_var2b(op2);
        lf_resw(lf_var1w() >> lf_var2b());
        lflags.type=t_SHRw;
        return lf_resw();
    }
    static public void SHRW(int op2, loadw l, savew s) {
        if (valid_SHRW(op2))
            s.call(do_SHRW(op2, l.call()));
    }

    static public long SHRD(long op2, long l) {
        lflags.var1 = l;lflags.var2 = op2;
        lflags.res = (l >> op2);
        lflags.type=t_SHRd;
        return lflags.res;
    }

    static public boolean valid_SARB(short op2) {
        return op2!=0;
    }

    static public short do_SARB(short op2, short l) {
        lf_var1b(l);lf_var2b(op2);
        if (lf_var2b()>8) lf_var2b(8);
        if ((lf_var1b() & 0x80)!=0) {
            lf_resb((lf_var1b() >> lf_var2b())|
            (0xff << (8 - lf_var2b())));
        } else {
            lf_resb(lf_var1b() >> lf_var2b());
        }
        lflags.type=t_SARb;
        return lf_resb();
    }

    static public void SARB(short op2, loadb l, saveb s) {
        if (valid_SARB(op2))
            s.call(do_SARB(op2, l.call()));        
    }
    static public boolean valid_SARW(int op2) {
        return op2!=0;
    }
    static public int do_SARW(int op2, int l) {
        lf_var1w(l);lf_var2b(op2);
        if (lf_var2b()>16) lf_var2b(16);
        if ((lf_var1w() & 0x8000)!=0) {
            lf_resw((lf_var1w() >> lf_var2b())|
            (0xffff << (16 - lf_var2b())));
        } else {
            lf_resw(lf_var1w() >> lf_var2b());
        }
        lflags.type=t_SARw;
        return lf_resw();
    }
    static public void SARW(int op2, loadw l, savew s) {
        if (valid_SARW(op2))
            s.call(do_SARW(op2, l.call()));
    }

    static public long SARD(long op2, long l) {
        lflags.var2=op2;lflags.var1=l;
        lflags.res = (int)l >> op2;
        lflags.type=t_SARd;
        return lflags.res;
    }

    static public void DAA() {
        if (((CPU_Regs.reg_eax.low() & 0x0F)>0x09) || get_AF()) {
            if ((CPU_Regs.reg_eax.low() > 0x99) || get_CF()) {
                CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()+0x60);
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            } else {
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
            }
            CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()+0x06);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,true);
        } else {
            if ((CPU_Regs.reg_eax.low() > 0x99) || get_CF()) {
                CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()+0x60);
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            } else {
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
            }
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,false);
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF,(CPU_Regs.reg_eax.low()&0x80)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,(CPU_Regs.reg_eax.low()==0));
        CPU_Regs.SETFLAGBIT(CPU_Regs.PF,parity_lookup[CPU_Regs.reg_eax.low()]!=0);
        lflags.type=t_UNKNOWN;
    }

    static public void DAS() {
        /*Bit8u*/int osigned=CPU_Regs.reg_eax.low() & 0x80;
        if (((CPU_Regs.reg_eax.low() & 0x0f) > 9) || get_AF()) {
            if ((CPU_Regs.reg_eax.low()>0x99) || get_CF()) {
                CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()-0x60);
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            } else {
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(CPU_Regs.reg_eax.low()<=0x05));
            }
            CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()-6);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,true);
        } else {
            if ((CPU_Regs.reg_eax.low()>0x99) || get_CF()) {
                CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low()-0x60);
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            } else {
                CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
            }
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,false);
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,osigned!=0 && ((CPU_Regs.reg_eax.low()&0x80)==0));
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF,(CPU_Regs.reg_eax.low()&0x80)!=0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,(CPU_Regs.reg_eax.low()==0));
        CPU_Regs.SETFLAGBIT(CPU_Regs.PF,parity_lookup[CPU_Regs.reg_eax.low()]!=0);
        lflags.type=t_UNKNOWN;
    }


    static public void AAA() {
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF,((CPU_Regs.reg_eax.low()>=0x7a) && (CPU_Regs.reg_eax.low()<=0xf9)));
        if ((CPU_Regs.reg_eax.low() & 0xf) > 9) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,(CPU_Regs.reg_eax.low()&0xf0)==0x70);
            CPU_Regs.reg_eax.word(CPU_Regs.reg_eax.word()+0x106);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,(CPU_Regs.reg_eax.low() == 0));
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,true);
        } else if (get_AF()) {
            CPU_Regs.reg_eax.word(CPU_Regs.reg_eax.word() + 0x106);
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,true);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,(CPU_Regs.reg_eax.low() == 0));
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,false);
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.PF,parity_lookup[CPU_Regs.reg_eax.low()]!=0);
        CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low() & 0x0F);
        lflags.type=t_UNKNOWN;
    }

    static public void AAS() {
        if ((CPU_Regs.reg_eax.low() & 0x0f)>9) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.SF,(CPU_Regs.reg_eax.low()>0x85));
            CPU_Regs.reg_eax.word(CPU_Regs.reg_eax.word() - 0x106);
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,true);
        } else if (get_AF()) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,((CPU_Regs.reg_eax.low()>=0x80) && (CPU_Regs.reg_eax.low()<=0x85)));
            CPU_Regs.SETFLAGBIT(CPU_Regs.SF,(CPU_Regs.reg_eax.low()<0x06) || (CPU_Regs.reg_eax.low()>0x85));
            CPU_Regs.reg_eax.word(CPU_Regs.reg_eax.word() - 0x106);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,true);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.SF,(CPU_Regs.reg_eax.low()>=0x80));
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,false);
        }
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,(CPU_Regs.reg_eax.low() == 0));
        CPU_Regs.SETFLAGBIT(CPU_Regs.PF,parity_lookup[CPU_Regs.reg_eax.low()]!=0);
        CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low() & 0x0F);
        lflags.type=t_UNKNOWN;
    }

    static public void AAM(int op1) {
        if (op1!=0) {
            CPU_Regs.reg_eax.high(CPU_Regs.reg_eax.low() / op1);
            CPU_Regs.reg_eax.low(CPU_Regs.reg_eax.low() % op1);
            CPU_Regs.SETFLAGBIT(CPU_Regs.SF,(CPU_Regs.reg_eax.low() & 0x80)!=0);
            CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,(CPU_Regs.reg_eax.low() == 0));
            CPU_Regs.SETFLAGBIT(CPU_Regs.PF,parity_lookup[CPU_Regs.reg_eax.low()]!=0);
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
            CPU_Regs.SETFLAGBIT(CPU_Regs.AF,false);
            lflags.type=t_UNKNOWN;
        } else {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
    }


    //Took this from bochs, i seriously hate these weird bcd opcodes
    static public void AAD(int op1) {
        /*Bit16u*/int ax1 = CPU_Regs.reg_eax.high() * op1;
        /*Bit16u*/int ax2 = ax1 + CPU_Regs.reg_eax.low();
        CPU_Regs.reg_eax.low(ax2);
        CPU_Regs.reg_eax.high(0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);
        CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        CPU_Regs.SETFLAGBIT(CPU_Regs.AF,false);
        CPU_Regs.SETFLAGBIT(CPU_Regs.SF,CPU_Regs.reg_eax.low() >= 0x80);
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,CPU_Regs.reg_eax.low() == 0);
        CPU_Regs.SETFLAGBIT(CPU_Regs.PF,parity_lookup[CPU_Regs.reg_eax.low()]!=0);
        lflags.type=t_UNKNOWN;
    }

    static public void MULB(short l) {
        CPU_Regs.reg_eax.word(CPU_Regs.reg_eax.low()*l);
        FillFlagsNoCFOF();
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,CPU_Regs.reg_eax.low() == 0);
        if ((CPU_Regs.reg_eax.word() & 0xff00)!=0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        }
    }

    static public void MULW(int l) {
        /*Bitu*/int tempu=CPU_Regs.reg_eax.word()*l;
        CPU_Regs.reg_eax.word(tempu);
        CPU_Regs.reg_edx.word(tempu >>> 16);
        FillFlagsNoCFOF();
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,CPU_Regs.reg_eax.word() == 0);
        if (CPU_Regs.reg_edx.word()!=0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        }
    }

    static public void MULD(long l) {
        /*Bit64u*/long tempu=CPU_Regs.reg_eax.dword()*(l);
        CPU_Regs.reg_eax.dword((tempu));
        CPU_Regs.reg_edx.dword((tempu >> 32));
        FillFlagsNoCFOF();
        CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,CPU_Regs.reg_eax.dword() == 0);
        if (CPU_Regs.reg_edx.dword()!=0) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        }
    }

    static public void DIVB(short l) {
        /*Bitu*/int val=l;
        if (val==0)	{
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        /*Bitu*/int quo=CPU_Regs.reg_eax.word() / val;
        /*Bit8u*/int rem=(CPU_Regs.reg_eax.word() % val);
        /*Bit8u*/int quo8=(quo&0xff);
        if (quo>0xff) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        CPU_Regs.reg_eax.high(rem);
        CPU_Regs.reg_eax.low(quo8);
    }

    static public void DIVW(int val) {
        if (val==0)	{
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        /*Bitu*/long num=((/*Bit32u*/long)CPU_Regs.reg_edx.word()<<16)|CPU_Regs.reg_eax.word();
        /*Bitu*/long quo=num/val;
        /*intBit16u*/int rem=(int)((num % val));
        /*intBit16u*/int quo16=(int)(quo&0xffff);
        if (quo!=quo16) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        CPU_Regs.reg_edx.word(rem);
        CPU_Regs.reg_eax.word(quo16);
    }

    static public void DIVD(long val) {
        if (val==0) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        /*Bit64u*/long num=((CPU_Regs.reg_edx.dword())<<32)|CPU_Regs.reg_eax.dword();
        /*Bit64u*/long quo= LongHelper.divideLongByInt(num,(int)val);
        CPU_Regs.reg_edx.dword(((quo >> 32) & 0xFFFFFFFFl));
        CPU_Regs.reg_eax.dword((quo & 0xFFFFFFFFl));
    }

    static public void IDIVB(short l) {
        /*Bits*/int val=(/*Bit8s*/byte)(l);
        if (val==0)	{
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        /*Bits*/int quo=((/*Bit16s*/short)CPU_Regs.reg_eax.word()) / val;
        /*Bit8s*/byte rem=(/*Bit8s*/byte)((/*Bit16s*/short)CPU_Regs.reg_eax.word() % val);
        /*Bit8s*/byte quo8s=(/*Bit8s*/byte)(quo&0xff);
        if (quo!=(/*Bit16s*/short)quo8s) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        CPU_Regs.reg_eax.high(rem);
        CPU_Regs.reg_eax.low(quo8s);
    }

    static public void IDIVW(int l) {
        /*Bits*/int val=(/*Bit16s*/short)(l);
        if (val==0) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        /*Bits*/int num=((CPU_Regs.reg_edx.word()<<16)|CPU_Regs.reg_eax.word());
        /*Bits*/int quo=num/val;
        /*Bit16s*/short rem=(/*Bit16s*/short)(num % val);
        /*Bit16s*/short quo16s=(/*Bit16s*/short)quo;
        if (quo!=(/*Bit32s*/int)quo16s) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        CPU_Regs.reg_edx.word(rem);
        CPU_Regs.reg_eax.word(quo16s);
    }

    static public void IDIVD(long l) {
        /*Bits*/int val=(/*Bit32s*/int)(l);
        if (val==0) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        /*Bit64s*/long num=((CPU_Regs.reg_edx.dword())<<32)|CPU_Regs.reg_eax.dword();
        /*Bit64s*/long quo=num/val;
        /*Bit32s*/int rem=(/*Bit32s*/int)(num % val);
        /*Bit32s*/int quo32s=(/*Bit32s*/int)(quo&0xffffffffl);
        if (quo!=(/*Bit64s*/long)quo32s) {
            Prefix_helpers.EXCEPTION(0);
            throw new Prefix_helpers.ContinueException();
        }
        CPU_Regs.reg_edx.dword(rem);
        CPU_Regs.reg_eax.dword(quo32s);
    }

    static public void IMULB(short l) {
        CPU_Regs.reg_eax.word(((/*Bit8s*/byte)CPU_Regs.reg_eax.low()) * ((/*Bit8s*/byte)(l)));
        FillFlagsNoCFOF();
        if ((CPU_Regs.reg_eax.word() & 0xff80)==0xff80 ||
            (CPU_Regs.reg_eax.word() & 0xff80)==0x0000) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        }
    }

    static public void IMULW(int l) {
        /*Bits*/int temps=((/*Bit16s*/short)CPU_Regs.reg_eax.word())*((/*Bit16s*/short)(l));
        CPU_Regs.reg_eax.word((/*Bit16s*/short)(temps));
        CPU_Regs.reg_edx.word((/*Bit16s*/short)(temps >> 16));
        FillFlagsNoCFOF();
        if (((temps & 0xffff8000)==0xffff8000 ||
            (temps & 0xffff8000)==0x0000)) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        }
    }

    static public void IMULD(long l) {
        /*Bit64s*/long temps=((/*Bit64s*/long)((/*Bit32s*/int)CPU_Regs.reg_eax.dword()))*
                     ((/*Bit64s*/long)((/*Bit32s*/int)(l)));
        CPU_Regs.reg_eax.dword((temps));
        CPU_Regs.reg_edx.dword((temps >> 32));
        FillFlagsNoCFOF();
        if ((CPU_Regs.reg_edx.dword()==0xffffffffl) &&
            (CPU_Regs.reg_eax.dword() & 0x80000000l)!=0 ) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        } else if ( (CPU_Regs.reg_edx.dword()==0x00000000) &&
                    (CPU_Regs.reg_eax.dword()< 0x80000000l) ) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        }
    }

    static public int DIMULW(int op2,int op3) {
        /*Bits*/int res=((/*Bit16s*/short)op2) * ((/*Bit16s*/short)op3);
        FillFlagsNoCFOF();
        if ((res> -32768)  && (res<32767)) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        }
        return (res & 0xffff);
    }

    static public long DIMULD(long op2,long op3) {
        /*Bit64s*/long res=((/*Bit64s*/long)((/*Bit32s*/int)op2))*((/*Bit64s*/long)((/*Bit32s*/int)op3));
        FillFlagsNoCFOF();
        if ((res>-((/*Bit64s*/long)(2147483647)+1)) &&
            (res<(/*Bit64s*/long)2147483647)) {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,false);
        } else {
            CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);CPU_Regs.SETFLAGBIT(CPU_Regs.OF,true);
        }
        return res & 0xFFFFFFFFl;
    }

    static public long DSHLD(long op2,long val, long l) {
        lf_var2b(val);lf_var1d(l);
        lflags.res = ((l << val) | (op2 >> (32-val))) & 0xFFFFFFFFl;
        lflags.type=t_DSHLd;
        return lflags.res;
    }

    static public long DSHRD(long op2,long val, long l) {
        lf_var2b(val);lf_var1d(l);
        lflags.res = ((l >> val) | (op2 << (32-val))) & 0xFFFFFFFFl;
        lflags.type=t_DSHRd;
        return lflags.res;
    }

    /* let's hope bochs has it correct with the higher than 16 shifts */
    /* double-precision shift left has low Bits in second argument */
    static public boolean valid_DSHLW(int op3) {
        return ((op3 & 0x1f)!=0);
    }
    static public int do_DSHLW(int op2,int op3, int l) {
        int val=op3 & 0x1F;
        lf_var2b(val);
        lf_var1d((l<<16)|op2);
        long tempd=lf_var1d() << lf_var2b();
        if (lf_var2b()>16) tempd |= (op2 << (lf_var2b() - 16));
        lf_resw((int)(tempd >>> 16));
        lflags.type=t_DSHLw;
        return lf_resw();
    }

    static public void DSHLW(int op2,int op3, loadw l, savew s) {
        if (valid_DSHLW(op3))
            s.call(do_DSHLW(op2, op3, l.call()));
    }

    /* double-precision shift right has high Bits in second argument */
    static public boolean valid_DSHRW(int op3) {
        return ((op3 & 0x1f)!=0);
    }

    static public int do_DSHRW(int op2,int op3, int l) {
        /*Bit8u*/short val=(short)(op3 & 0x1F);
        lf_var2b(val);lf_var1d((op2<<16)|l);
        /*Bit32u*/long tempd=lf_var1d() >>> lf_var2b();
        if (lf_var2b()>16) tempd |= (op2 << (32-lf_var2b() ));
        lf_resw((/*Bit16u*/int)(tempd));
        lflags.type=t_DSHRw;
        return lf_resw();
    }
    static public void DSHRW(int op2,int op3, loadw l, savew s) {
        if (valid_DSHRW(op3))
            s.call(do_DSHRW(op2, op3, l.call()));
    }

    public static int BSWAPW(int op1) {
	    return 0;
    }

    static public long BSWAPD(long op1) {
        return (op1>>24)|((op1>>8)&0xFF00)|((op1<<8)&0xFF0000)|((op1<<24)&0xFF000000l);
    }
}
