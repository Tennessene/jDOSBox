package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;

public class Grp3 extends Helper {
    static public class Testb_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public Testb_reg(int rm) {
            this.val=decode_fetchb();
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.TESTB(val,earb.get8());
            return Constants.BR_Normal;
        }
    }

    static public class Testb_mem extends Op {
        short val;
        EaaBase get_eaa;

        public Testb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val=decode_fetchb();
        }

        public int call() {
            long eaa = get_eaa.call();
            Instructions.TESTB(val,Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class NotEb_reg extends Op {
        CPU_Regs.Reg earb;

        public NotEb_reg(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((byte)~earb.get8());
            return Constants.BR_Normal;
        }
    }

    static public class NotEb_mem extends Op {
        EaaBase get_eaa;

        public NotEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0)
                Memory.host_writeb(index,(byte)~Memory.host_readb(index));
            else
                Memory.mem_writeb(eaa,~Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class NegEb_reg extends Op {
        CPU_Regs.Reg earb;

        public NegEb_reg(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            Flags.lflags.type=Flags.t_NEGb;
            Flags.lf_var1b(earb.get8());
            Flags.lf_resb(0-Flags.lf_var1b());
            earb.set8(Flags.lf_resb());
            return Constants.BR_Normal;
        }
    }

    static public class NegEb_mem extends Op {
        EaaBase get_eaa;

        public NegEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            Flags.lflags.type=Flags.t_NEGb;
            long eaa = get_eaa.call();
            Flags.lf_var1b(Memory.mem_readb(eaa));
            Flags.lf_resb(0-Flags.lf_var1b());
            Memory.mem_writeb(eaa,Flags.lf_resb());
            return Constants.BR_Normal;
        }
    }

    static public class MulAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public MulAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            Instructions.MULB(earb.get8());
            return Constants.BR_Normal;
        }
    }

    static public class MulAlEb_mem extends Op {
        EaaBase get_eaa;

        public MulAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            Instructions.MULB(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class IMulAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public IMulAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            Instructions.IMULB(earb.get8());
            return Constants.BR_Normal;
        }
    }

    static public class IMulAlEb_mem extends Op {
        EaaBase get_eaa;

        public IMulAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            Instructions.IMULB(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class DivAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public DivAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            return Instructions.DIVBr(earb.get8());
        }
    }

    static public class DivAlEb_mem extends Op {
        EaaBase get_eaa;

        public DivAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            return Instructions.DIVBr(Memory.mem_readb(eaa));
        }
    }

    static public class IDivAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public IDivAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            return Instructions.IDIVBr(earb.get8());
        }
    }

    static public class IDivAlEb_mem extends Op {
        EaaBase get_eaa;

        public IDivAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            return Instructions.IDIVBr(Memory.mem_readb(eaa));
        }
    }

    static public class Testw_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public Testw_reg(int rm) {
            this.val=decode_fetchw();
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.TESTW(val,earw.word());
            return Constants.BR_Normal;
        }
    }

    static public class Testw_mem extends Op {
        int val;
        EaaBase get_eaa;

        public Testw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val=decode_fetchw();
        }

        public int call() {
            long eaa = get_eaa.call();
            Instructions.TESTW(val,Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class NotEw_reg extends Op {
        CPU_Regs.Reg earw;

        public NotEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            earw.word(~earw.word());
            return Constants.BR_Normal;
        }
    }

    static public class NotEw_mem extends Op {
        EaaBase get_eaa;

        public NotEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index,~Memory.host_readw(index));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa,~Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class NegEw_reg extends Op {
        CPU_Regs.Reg earw;

        public NegEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            Flags.lflags.type=Flags.t_NEGw;
            Flags.lf_var1w(earw.word());
            Flags.lf_resw(0-Flags.lf_var1w());
            earw.word(Flags.lf_resw());
            return Constants.BR_Normal;
        }
    }

    static public class NegEw_mem extends Op {
        EaaBase get_eaa;

        public NegEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            Flags.lflags.type=Flags.t_NEGw;
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Flags.lf_var1w(Memory.host_readw(index));
                    Flags.lf_resw(0-Flags.lf_var1w());
                    Memory.host_writew(index,Flags.lf_resw());
                    return Constants.BR_Normal;
                }
            }
            Flags.lf_var1w(Memory.mem_readw(eaa));
            Flags.lf_resw(0-Flags.lf_var1w());
            Memory.mem_writew(eaa,Flags.lf_resw());
            return Constants.BR_Normal;
        }
    }

    static public class MulAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public MulAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            Instructions.MULW(earw.word());
            return Constants.BR_Normal;
        }
    }

    static public class MulAxEw_mem extends Op {
        EaaBase get_eaa;

        public MulAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            Instructions.MULW(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class IMulAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public IMulAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            Instructions.IMULW(earw.word());
            return Constants.BR_Normal;
        }
    }

    static public class IMulAxEw_mem extends Op {
        EaaBase get_eaa;

        public IMulAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            Instructions.IMULW(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class DivAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public DivAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            return Instructions.DIVWr(earw.word());
        }
    }

    static public class DivAxEw_mem extends Op {
        EaaBase get_eaa;

        public DivAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            return Instructions.DIVWr(Memory.mem_readw(eaa));
        }
    }

    static public class IDivAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public IDivAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            return Instructions.IDIVWr(earw.word());
        }
    }

    static public class IDivAxEw_mem extends Op {
        EaaBase get_eaa;

        public IDivAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            return Instructions.IDIVWr(Memory.mem_readw(eaa));
        }
    }

    static public class Testd_reg extends Op {
        long val;
        CPU_Regs.Reg eard;

        public Testd_reg(int rm) {
            val=decode_fetchd();
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.TESTD(val,eard.dword());
            return Constants.BR_Normal;
        }
    }

    static public class Testd_mem extends Op {
        long val;
        EaaBase get_eaa;

        public Testd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val=decode_fetchd();
        }

        public int call() {
            long eaa = get_eaa.call();
            Instructions.TESTD(val,Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class NotEd_reg extends Op {
        CPU_Regs.Reg eard;

        public NotEd_reg(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            eard.dword(~eard.dword());
            return Constants.BR_Normal;
        }
    }

    static public class NotEd_mem extends Op {
        EaaBase get_eaa;

        public NotEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index,~Memory.host_readd(index));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writed(eaa,~Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class NegEd_reg extends Op {
        CPU_Regs.Reg eard;

        public NegEd_reg(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            Flags.lflags.type=Flags.t_NEGd;
            Flags.lf_var1d(eard.dword());
            Flags.lf_resd(0-Flags.lf_var1d());
            eard.dword(Flags.lf_resd());
            return Constants.BR_Normal;
        }
    }

    static public class NegEd_mem extends Op {
        EaaBase get_eaa;

        public NegEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            Flags.lflags.type=Flags.t_NEGd;
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Flags.lf_var1d(Memory.host_readd(index));
                    Flags.lf_resd(0-Flags.lf_var1d());
                    Memory.host_writed(index,Flags.lf_resd());
                    return Constants.BR_Normal;
                }
            }
            Flags.lf_var1d(Memory.mem_readd(eaa));
            Flags.lf_resd(0-Flags.lf_var1d());
            Memory.mem_writed(eaa,Flags.lf_resd());
            return Constants.BR_Normal;
        }
    }

    static public class MulAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public MulAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            Instructions.MULD(eard.dword());
            return Constants.BR_Normal;
        }
    }

    static public class MulAxEd_mem extends Op {
        EaaBase get_eaa;

        public MulAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            Instructions.MULD(Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class IMulAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public IMulAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            Instructions.IMULD(eard.dword());
            return Constants.BR_Normal;
        }
    }

    static public class IMulAxEd_mem extends Op {
        EaaBase get_eaa;

        public IMulAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            Instructions.IMULD(Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }
    }

    static public class DivAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public DivAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            return Instructions.DIVDr(eard.dword());
        }
    }

    static public class DivAxEd_mem extends Op {
        EaaBase get_eaa;

        public DivAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            return Instructions.DIVDr(Memory.mem_readd(eaa));
        }
    }

    static public class IDivAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public IDivAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            return Instructions.IDIVDr(eard.dword());
        }
    }

    static public class IDivAxEd_mem extends Op {
        EaaBase get_eaa;

        public IDivAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            long eaa = get_eaa.call();
            return Instructions.IDIVDr(Memory.mem_readd(eaa));
        }
    }
}
