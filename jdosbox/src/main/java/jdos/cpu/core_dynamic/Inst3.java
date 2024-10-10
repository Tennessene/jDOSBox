package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Pic;

public class Inst3 extends Helper {
    final static public class Addd_reg extends Op {
        Reg e;
        Reg g;

        public Addd_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.ADDD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADD "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class AddEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public AddEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.ADDD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADD "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class AddGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public AddGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.ADDD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADD "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class AddEaxId extends Op {
        int i;

        public AddEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.ADDD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADD "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class Push32ES extends Op {
        public int call() {
            CPU.CPU_Push32(CPU_Regs.reg_esVal.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH ES";}
    }

    final static public class Pop32ES extends Op {
        public int call() {
            if (CPU.CPU_PopSegES(true)) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int setsSeg() {return ES|FROM_STACK;}
        public String description() {return "POP ES";}
    }

    final static public class Ord_reg extends Op {
        Reg e;
        Reg g;

        public Ord_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.ORD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OR "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ORD;}
    }

    final static public class OrEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public OrEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.ORD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OR "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ORD;}
    }

    final static public class OrGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public OrGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.ORD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OR "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_ORD;}
    }

    final static public class OrEaxId extends Op {
        int i;

        public OrEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.ORD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OR "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ORD;}
    }

    final static public class Push32CS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU_Regs.reg_csVal.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH CS";}
    }

    final static public class Adcd_reg extends Op {
        Reg e;
        Reg g;

        public Adcd_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.ADCD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADC "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ADCD;}
    }

    final static public class AdcEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public AdcEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.ADCD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADC "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ADCD;}
    }

    final static public class AdcGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public AdcGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.ADCD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADC "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_ADCD;}
    }

    final static public class AdcEaxId extends Op {
        int i;

        public AdcEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.ADCD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADC "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ADCD;}
    }

    final static public class Push32SS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU_Regs.reg_ssVal.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH SS";}
    }

    final static public class Pop32SS extends Op {
        public int call() {
            if (CPU.CPU_PopSegSS(true)) return RUNEXCEPTION();
            Core.base_ss=CPU_Regs.reg_ssPhys.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int setsSeg() {return SS|FROM_STACK;}
        public String description() {return "POP SS";}
    }

    final static public class Sbbd_reg extends Op {
        Reg e;
        Reg g;

        public Sbbd_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.SBBD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SBB "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_SBBD;}
    }

    final static public class SbbEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public SbbEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.SBBD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SBB "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_SBBD;}
    }

    final static public class SbbGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public SbbGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.SBBD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SBB "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_SBBD;}
    }

    final static public class SbbEaxId extends Op {
        int i;

        public SbbEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.SBBD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SBB "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_SBBD;}
    }

    final static public class Push32DS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU_Regs.reg_dsVal.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH DS";}
    }

    final static public class Pop32DS extends Op {
        public int call() {
            if (CPU.CPU_PopSegDS(true)) return RUNEXCEPTION();
            Core.base_ds=CPU_Regs.reg_dsPhys.dword;
            Core.base_val_ds= CPU_Regs.ds;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int setsSeg() {return DS|FROM_STACK;}
        public String description() {return "POP DS";}
    }

    final static public class Andd_reg extends Op {
        Reg e;
        Reg g;

        public Andd_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.ANDD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AND "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ANDD;}
    }

    final static public class AndEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public AndEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.ANDD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AND "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_ANDD;}
    }

    final static public class AndGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public AndGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.ANDD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AND "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_ANDD;}
    }

    final static public class AndEaxId extends Op {
        int i;

        public AndEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.ANDD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AND "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ANDD;}
    }

    final static public class Subd_reg extends Op {
        Reg e;
        Reg g;

        public Subd_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.SUBD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SUB "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_SUBD;}
    }

    final static public class SubEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public SubEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.SUBD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SUB "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_SUBD;}
    }

    final static public class SubGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public SubGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.SUBD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SUB "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_SUBD;}
    }

    final static public class SubEaxId extends Op {
        int i;

        public SubEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.SUBD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SUB "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_SUBD;}
    }

    final static public class Xord_reg extends Op {
        Reg e;
        Reg g;

        public Xord_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.dword=Instructions.XORD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XOR "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_XORD;}
    }

    final static public class XorEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public XorEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Memory.mem_writed(eaa, Instructions.XORD(g.dword, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XOR "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_XORD;}
    }

    final static public class XorGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public XorGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            e.dword=Instructions.XORD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XOR "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_XORD;}
    }

    final static public class XorEaxId extends Op {
        int i;

        public XorEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            reg_eax.dword=Instructions.XORD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XOR "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_XORD;}
    }

    final static public class Cmpd_reg extends Op {
        Reg e;
        Reg g;

        public Cmpd_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            Instructions.CMPD(g.dword, e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMP "+e.getName()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_CMPD;}
    }

    final static public class CmpEdGd_mem extends Op {
        EaaBase e;
        Reg g;

        public CmpEdGd_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = e.call();
            Instructions.CMPD(g.dword, Memory.mem_readd(eaa));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMP "+e.description32()+", "+g.getName();}
        public int getFlagType() {return FLAG_TYPE_CMPD;}
    }

    final static public class CmpGdEd_mem extends Op {
        EaaBase g;
        Reg e;

        public CmpGdEd_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            int eaa = g.call();
            Instructions.CMPD(Memory.mem_readd(eaa), e.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMP "+e.getName()+", "+g.description32();}
        public int getFlagType() {return FLAG_TYPE_CMPD;}
    }

    final static public class CmpEaxId extends Op {
        int i;

        public CmpEaxId() {
            i = decode_fetchd();
        }

        public int call() {
            Instructions.CMPD(i, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMP "+CPU_Regs.reg_eax.getName()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_CMPD;}
    }

    final static public class Incd_reg extends Op {
        Reg reg;

        public Incd_reg(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            reg.dword = Instructions.INCD(reg.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF is preserved
        public int sets() {
            return CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "INC "+reg.getName();}
        public int getFlagType() {return FLAG_TYPE_INCD;}
    }

    final static public class Incd_mem extends Op {
        EaaBase get_eaa;

        public Incd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.INCD(Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF is preserved
        public int sets() {
            return CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "INC "+get_eaa.description32();}
        public int getFlagType() {return FLAG_TYPE_INCD;}
    }

    final static public class Decd_reg extends Op {
        Reg reg;

        public Decd_reg(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            reg.dword = Instructions.DECD(reg.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF is preserved
        public int sets() {
            return CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "DEC "+reg.getName();}
        public int getFlagType() {return FLAG_TYPE_DECD;}
    }

    final static public class Decd_mem extends Op {
        EaaBase get_eaa;

        public Decd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.DECD(Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF is preserved
        public int sets() {
            return CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "DEC "+get_eaa.description32();}
        public int getFlagType() {return FLAG_TYPE_DECD;}
    }

    final static public class Push32_reg extends Op {
        Reg reg;

        public Push32_reg(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            CPU.CPU_Push32(reg.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH "+reg.getName();}
    }

    final static public class Pop32_reg extends Op {
        Reg reg;

        public Pop32_reg(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            reg.dword=CPU.CPU_Pop32();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "POP "+reg.getName();}
    }

    final static public class Pushad extends Op {
        public int call() {
            /*Bitu*/int tmpesp = reg_esp.dword;
            int esp = reg_esp.dword;
            esp = CPU.CPU_Push32(esp, reg_eax.dword);
            esp = CPU.CPU_Push32(esp, reg_ecx.dword);
            esp = CPU.CPU_Push32(esp, reg_edx.dword);
            esp = CPU.CPU_Push32(esp, reg_ebx.dword);
            esp = CPU.CPU_Push32(esp, tmpesp);
            esp = CPU.CPU_Push32(esp, reg_ebp.dword);
            esp = CPU.CPU_Push32(esp, reg_esi.dword);
            esp = CPU.CPU_Push32(esp, reg_edi.dword);
            // Don't store ESP until all the memory writes are done in case of a PF so that this op can be reentrant
            reg_esp.dword=esp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSHA";}
    }

    final static public class Popad extends Op {
        public int call() {
            reg_edi.dword=CPU.CPU_Pop32();
            reg_esi.dword=CPU.CPU_Pop32();
            reg_ebp.dword=CPU.CPU_Pop32();CPU.CPU_Pop32();//Don't save ESP
            reg_ebx.dword=CPU.CPU_Pop32();
            reg_edx.dword=CPU.CPU_Pop32();
            reg_ecx.dword=CPU.CPU_Pop32();
            reg_eax.dword=CPU.CPU_Pop32();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "POPA";}
    }

    final static public class BoundEd extends Op {
        EaaBase get_eaa;
        Reg rd;

        public BoundEd() {
            int rm = decode_fetchb();
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int bound_min, bound_max;
            int eaa=get_eaa.call();
            bound_min=Memory.mem_readd(eaa);
            bound_max=Memory.mem_readd(eaa + 4);
            int rmrd = rd.dword;
            if (rmrd < bound_min || rmrd > bound_max) {
                return EXCEPTION(5);
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "BOUND "+rd.getName()+", "+get_eaa.description32();}
    }

    final static public class ArplEdRd_reg extends Op {
        Reg rd;
        Reg eard;

        public ArplEdRd_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            eard.dword = CPU.CPU_ARPL(eard.dword, rd.word());
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }
        public boolean returnsIllegal() {return true;}
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ARPL "+eard.getName()+", "+rd.getName();}
    }

    final static public class ArplEdRd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public ArplEdRd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }
        public int call() {
            if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();
            int value = Memory.mem_readw(eaa);
            value = CPU.CPU_ARPL(value, rd.word());
            // :TODO: if the value didn't change, should we issue a write?
            Memory.mem_writed(eaa, value);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }
        public boolean returnsIllegal() {return true;}
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ARPL "+get_eaa.description32()+", "+rd.getName();}
    }

    final static public class PushId extends Op {
        int id;

        public PushId() {
            id = decode_fetchd();
        }
        public int call() {
            CPU.CPU_Push32(id);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH "+id;}
    }

    final static public class ImulGdEdId_reg extends Op {
        Reg eard;
        Reg rd;
        int op3;

        public ImulGdEdId_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
            op3 = decode_fetchds();
        }

        public int call() {
            rd.dword=Instructions.DIMULD(eard.dword,op3);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IMUL "+rd.getName()+", "+eard.getName()+", "+op3;}
    }

    final static public class ImulGdEdId_mem extends Op {
        EaaBase get_eaa;
        Reg rd;
        int op3;

        public ImulGdEdId_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
            op3 = decode_fetchds();
        }

        public int call() {
            int eaa = get_eaa.call();
            rd.dword=Instructions.DIMULD(Memory.mem_readd(eaa),op3);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IMUL "+rd.getName()+", "+get_eaa.description32()+", "+op3;}
    }

    final static public class PushIb extends Op {
        int id;

        public PushIb() {
            id = decode_fetchbs();
        }
        public int call() {
            CPU.CPU_Push32(id);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH "+id;}
    }

    final static public class ImulGdEdIb_reg extends Op {
        Reg eard;
        Reg rd;
        int op3;

        public ImulGdEdIb_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
            op3 = decode_fetchbs();
        }

        public int call() {
            rd.dword=Instructions.DIMULD(eard.dword,op3);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IMUL "+rd.getName()+", "+eard.getName()+", "+op3;}
    }

    final static public class ImulGdEdIb_mem extends Op {
        EaaBase get_eaa;
        Reg rd;
        int op3;

        public ImulGdEdIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
            op3 = decode_fetchbs();
        }

        public int call() {
            int eaa = get_eaa.call();
            rd.dword=Instructions.DIMULD(Memory.mem_readd(eaa),op3);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IMUL "+rd.getName()+", "+get_eaa.description32()+", "+op3;}
    }

    static abstract public class JumpCond32_b extends Op {
        int offset;
        public JumpCond32_b() {
            offset = decode_fetchbs();
        }

        final protected int jump(boolean COND) {
            if (COND) {
                reg_eip+=offset+eip_count;
                return Constants.BR_Link1;
            }
            reg_eip+=eip_count;
            return Constants.BR_Link2;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return " off="+offset;}
    }

    final static public class JumpCond32_b_o extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_O());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
        public String description() {return "JO"+super.description();}
    }

    final static public class JumpCond32_b_no extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NO());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
        public String description() {return "JNO"+super.description();}
    }

    final static public class JumpCond32_b_b extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_B());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
        public String description() {return "JB"+super.description();}
    }

    final static public class JumpCond32_b_nb extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NB());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
        public String description() {return "JNB"+super.description();}
    }

    final static public class JumpCond32_b_z extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_Z());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "JZ"+super.description();}
    }

    final static public class JumpCond32_b_nz extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NZ());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "JNZ"+super.description();}
    }

    final static public class JumpCond32_b_be extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_BE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
        public String description() {return "JBE"+super.description();}
    }

    final static public class JumpCond32_b_nbe extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NBE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
        public String description() {return "JNBE"+super.description();}
    }

    final static public class JumpCond32_b_s extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_S());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
        public String description() {return "JS"+super.description();}
    }

    final static public class JumpCond32_b_ns extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NS());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
        public String description() {return "JNS"+super.description();}
    }

    final static public class JumpCond32_b_p extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_P());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
        public String description() {return "JP"+super.description();}
    }

    final static public class JumpCond32_b_np extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NP());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
        public String description() {return "JNP"+super.description();}
    }

    final static public class JumpCond32_b_l extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_L());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
        public String description() {return "JL"+super.description();}
    }

    final static public class JumpCond32_b_nl extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NL());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
        public String description() {return "JNL"+super.description();}
    }

    final static public class JumpCond32_b_le extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_LE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
        public String description() {return "JLE"+super.description();}
    }

    final static public class JumpCond32_b_nle extends JumpCond32_b {
        public int call() {
            return jump(Flags.TFLG_NLE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
        public String description() {return "JNLE"+super.description();}
    }

    final static public class GrplEdId_reg_add extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_add(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            eard.dword=Instructions.ADDD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADD "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class GrplEdId_reg_or extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_or(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            eard.dword=Instructions.ORD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OR "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ORD;}
    }

    final static public class GrplEdId_reg_adc extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_adc(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            eard.dword=Instructions.ADCD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADC "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADCD;}
    }

    final static public class GrplEdId_reg_sbb extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_sbb(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            eard.dword=Instructions.SBBD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SBB "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SBBD;}
    }

    final static public class GrplEdId_reg_and extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_and(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }
        public int call() {
            eard.dword=Instructions.ANDD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AND "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ANDD;}
    }

    final static public class GrplEdId_reg_sub extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_sub(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            eard.dword=Instructions.SUBD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SUB "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SUBD;}
    }

    final static public class GrplEdId_reg_xor extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_xor(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            eard.dword=Instructions.XORD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XOR "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_XORD;}
    }

    final static public class GrplEdId_reg_cmp extends Op {
        Reg eard;
        int ib;

        public GrplEdId_reg_cmp(int rm, boolean signed) {
            eard = Mod.ed(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            Instructions.CMPD(ib,eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMP "+eard.getName()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_CMPD;}
    }

    final static public class GrplEdId_mem_add extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_add(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.ADDD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADD "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class GrplEdId_mem_or extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_or(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }
        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.ORD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OR "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ORD;}
    }

    final static public class GrplEdId_mem_adc extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_adc(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.ADCD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ADC "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADCD;}
    }

    final static public class GrplEdId_mem_sbb extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_sbb(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.SBBD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SBB "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SBBD;}
    }

    final static public class GrplEdId_mem_and extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_and(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.ANDD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AND "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ANDD;}
    }

    final static public class GrplEdId_mem_sub extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_sub(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.SUBD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SUB "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SUBD;}
    }

    final static public class GrplEdId_mem_xor extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_xor(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.XORD(ib,Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XOR "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_XORD;}
    }

    final static public class GrplEdId_mem_cmp extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEdId_mem_cmp(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = decode_fetchbs();
            else
                ib = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.CMPD(ib,Memory.mem_readd(eaa));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMP "+get_eaa.description32()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_CMPD;}
    }

    final static public class TestEdGd_reg extends Op {
        Reg eard;
        Reg rd;

        public TestEdGd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            Instructions.TESTD(rd.dword, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "TEST "+eard.getName()+", "+rd.getName();}
        public int getFlagType() {return FLAG_TYPE_TESTD;}
    }

    final static public class TestEdGd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public TestEdGd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTD(rd.dword, Memory.mem_readd(eaa));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "TEST "+get_eaa.description32()+", "+rd.getName();}
        public int getFlagType() {return FLAG_TYPE_TESTD;}
    }

    final static public class XchgEdGd_reg extends Op {
        Reg eard;
        Reg rd;

        public XchgEdGd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int oldrmrd= rd.dword;
            rd.dword=eard.dword;
            eard.dword=oldrmrd;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XCHG "+eard.getName()+", "+rd.getName();}
    }

    final static public class XchgEdGd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public XchgEdGd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            int oldrmrd= rd.dword;
            int tmp = Memory.mem_readd(eaa);
            Memory.mem_writed(eaa,oldrmrd);
            rd.dword=tmp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XCHG "+get_eaa.description32()+", "+rd.getName();}
    }

    final static public class MovEdGd_reg extends Op {
        Reg eard;
        Reg rd;

        public MovEdGd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            eard.dword=rd.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", "+rd.getName();}
    }

    final static public class MovEdGd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public MovEdGd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, rd.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+get_eaa.description32()+", "+rd.getName();}
    }

    final static public class MovGdEd_reg extends Op {
        Reg eard;
        Reg rd;

        public MovGdEd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            rd.dword=eard.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+rd.getName()+", "+eard.getName();}
    }

    final static public class MovGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public MovGdEd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rd.dword=Memory.mem_readd(eaa);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+rd.getName()+", "+get_eaa.description32();}
    }

    final static public class MovEdEs_reg extends Op {
        Reg eard;

        public MovEdEs_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU_Regs.reg_esVal.dword & 0xFFFF; // this dword assignment is intentional
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", ES";}
    }

    final static public class MovEdCs_reg extends Op {
        Reg eard;

        public MovEdCs_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU_Regs.reg_csVal.dword & 0xFFFF; // this dword assignment is intentional
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", CS";}
    }

    final static public class MovEdSs_reg extends Op {
        Reg eard;

        public MovEdSs_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU_Regs.reg_ssVal.dword & 0xFFFF; // this dword assignment is intentional
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", SS";}
    }

    final static public class MovEdDs_reg extends Op {
        Reg eard;

        public MovEdDs_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU_Regs.reg_dsVal.dword & 0xFFFF; // this dword assignment is intentional
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", DS";}
    }

    final static public class MovEdFs_reg extends Op {
        Reg eard;

        public MovEdFs_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU_Regs.reg_fsVal.dword & 0xFFFF; // this dword assignment is intentional
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", FS";}
    }

    final static public class MovEdGs_reg extends Op {
        Reg eard;

        public MovEdGs_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU_Regs.reg_gsVal.dword & 0xFFFF; // this dword assignment is intentional
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+eard.getName()+", GS";}
    }

    final static public class LeaGd_16 extends Op {
        Reg rd;
        EaaBase get_eaa;

        public LeaGd_16(int rm) {
            rd = Mod.gd(rm);
            get_eaa= Mod.getEaa16(rm);
        }

        public int call() {
            //Little hack to always use segprefixed version
            Core.base_ds=Core.base_ss=0;
            rd.dword = get_eaa.call();
            Core.base_ds=CPU_Regs.reg_dsPhys.dword;
            Core.base_ss=CPU_Regs.reg_ssPhys.dword;
            Core.base_val_ds= CPU_Regs.ds;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "LEA "+rd.getName()+", "+get_eaa.descriptionZero();}
    }

    final static public class LeaGd_32 extends Op {
        Reg rd;
        EaaBase get_eaa;

        public LeaGd_32(int rm) {
            rd = Mod.gd(rm);
            get_eaa= Mod.getEaa32(rm);
        }

        public int call() {
            // :TODO: research if the base_ds is alway CPU_Regs.reg_dsPhys.dword etc.
            //Little hack to always use segprefixed version
            Core.base_ds=Core.base_ss=0;
            rd.dword = get_eaa.call();
            Core.base_ds=CPU_Regs.reg_dsPhys.dword;
            Core.base_ss=CPU_Regs.reg_ssPhys.dword;
            Core.base_val_ds= CPU_Regs.ds;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "LEA "+rd.getName()+", "+get_eaa.descriptionZero();}
    }

    final static public class PopEd_reg extends Op {
        Reg eard;

        public PopEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU.CPU_Pop32();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "POP "+eard.getName();}
    }

    final static public class PopEd_mem extends Op {
        EaaBase get_eaa;

        public PopEd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int val = CPU.CPU_Pop32();
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, val);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "POP "+get_eaa.description32();}
    }

    final static public class XchgEax extends Op {
        Reg reg;

        public XchgEax(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            int old=reg.dword;
            reg.dword=reg_eax.dword;
            reg_eax.dword=old;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "XCHG "+CPU_Regs.reg_eax.getName()+", "+reg.getName();}
    }

    final static public class Cwde extends Op {
        public int call() {
            reg_eax.dword=(short)reg_eax.word();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CWDE";}
    }

    final static public class Cdq extends Op {
        public int call() {
            if ((reg_eax.dword & 0x80000000)!=0) reg_edx.dword=0xffffffff;
            else reg_edx.dword=0;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CDQ";}
    }

    final static public class CallFarAp extends Op {
        int newcs;
        int newip;

        public CallFarAp(int newcs, int newip) {
            this.newcs = newcs;
            this.newip = newip;
        }

        public int call() {
            // :TODO: is this FillFlags necessary
            Flags.FillFlags();
            CPU.CPU_CALL(true,newcs,newip,reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "CALL FAR "+Integer.toHexString(newcs)+":"+Integer.toHexString(newip);}
    }

    final static public class Pushfd extends Op {
        public int call() {
            if (CPU.CPU_PUSHF(true)) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        // Pushes flags
        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSHF";}
    }

    final static public class Popfd extends Op {
        public int call() {
            if (CPU.CPU_POPF(true)) return RUNEXCEPTION();
            if (CPU_TRAP_CHECK) {
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                        return DECODE_END(eip_count);
                    }
            }
            if (CPU_PIC_CHECK)
                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END(eip_count);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Pops Flags
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "POPF";}
    }

     final static public class MovEaxOd extends Inst1.GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            reg_eax.dword=Memory.mem_readd(eaa);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

         public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+CPU_Regs.reg_eax.getName()+", @0x"+Integer.toHexString(Core.base_ds+value);}
    }

    final static public class MovOdEax extends Inst1.GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            Memory.mem_writed(eaa, reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV 0x"+Integer.toHexString(Core.base_ds+value)+", "+CPU_Regs.reg_eax.getName();}
    }

    final static public class TestEaxId extends Op {
        int id;

        public TestEaxId() {
            id = decode_fetchd();
        }

        public int call() {
            Instructions.TESTD(id,reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // CF, AF, OF are always 0
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "TEST "+CPU_Regs.reg_eax.getName()+", "+id;}
        public int getFlagType() {return FLAG_TYPE_TESTD;}
    }

    final static public class MovId extends Op {
        int id;
        Reg reg;

        public MovId(Reg reg) {
            id = decode_fetchd();
            this.reg = reg;
        }

        public int call() {
            reg.dword=id;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+reg.getName()+", "+id;}
    }

    final static public class MovId_mem extends Op {
        int id;
        EaaBase get_eaa;

        public MovId_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            id = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, id);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MOV "+get_eaa.description32()+", "+id;}
    }

    final static public class Retn32Iw extends Op {
        int offset;

        public Retn32Iw() {
            offset = decode_fetchw();
        }

        public int call() {
            reg_eip=CPU.CPU_Pop32();
            reg_esp.dword=reg_esp.dword+offset;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return true;}
        public String description() {return "RETN "+offset;}
    }

    final static public class Retn32 extends Op {
        public int call() {
            reg_eip=CPU.CPU_Pop32();
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return true;}
        public String description() {return "RETN";}
    }

    final static public class Les32 extends Op {
        EaaBase get_eaa;
        Reg rd;

        public Les32(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int val = Memory.mem_readd(eaa); // make sure all reads are done before writing something in case of a PF
            if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
            rd.dword=val;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int setsSeg() {return ES|FROM_MEMORY;}
        public String description() {return "LES "+rd.getName()+", "+get_eaa.description32();}
    }

    final static public class Lds32 extends Op {
        EaaBase get_eaa;
        Reg rd;

        public Lds32(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int val = Memory.mem_readd(eaa); // make sure all reads are done before writing something in case of a PF
            if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
            rd.dword=val;
            Core.base_ds=CPU_Regs.reg_dsPhys.dword;
            Core.base_ss=CPU_Regs.reg_ssPhys.dword;
            Core.base_val_ds= CPU_Regs.ds;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int setsSeg() {return DS|FROM_MEMORY;}
        public String description() {return "LDS "+rd.getName()+", "+get_eaa.description32();}
    }

    final static public class Enter32IwIb extends Op {
        int bytes;
        int level;

        public Enter32IwIb() {
            bytes=decode_fetchw();
            level=decode_fetchb();
        }

        public int call() {
            CPU.CPU_ENTER(true,bytes,level);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ENTER "+bytes+", "+level;}
    }

    final static public class Leave32 extends Op {
        public int call() {
            reg_esp.dword&=CPU.cpu.stack.notmask;
            reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);
            reg_ebp.dword=CPU.CPU_Pop32();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "LEAVE";}
    }

    final static public class Retf32Iw extends Op {
        int words;
        public Retf32Iw() {
            words = decode_fetchw();
        }
        public int call() {
            Flags.FillFlags();
            CPU.CPU_RET(true,words,reg_eip+eip_count);
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "RETF "+words;}
    }

    final static public class Retf32 extends Op {
        public int call() {
            Flags.FillFlags();
            CPU.CPU_RET(true,0,reg_eip+eip_count);
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "RETF";}
    }

    final static public class IRet32 extends Op {
        public int call() {
            CPU.CPU_IRET(true, reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            if (CPU_PIC_CHECK)
                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return CB_NONE();
            return Constants.BR_Jump;
        }

        // Pops flags
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "IRET";}
    }

    final static public class Loopnz32 extends JumpCond32_b {
        public int call() {
            reg_ecx.dword--;
            return jump(reg_ecx.dword!=0 && !Flags.get_ZF());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "LOOPNZ";}
    }

    final static public class Loopnz16 extends JumpCond32_b {
        public int call() {
            reg_ecx.word(reg_ecx.word()-1);
            return jump(reg_ecx.word()!=0 && !Flags.get_ZF());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "LOOPNZ";}
    }

    final static public class Loopz32 extends JumpCond32_b {
        public int call() {
            reg_ecx.dword--;
            return jump(reg_ecx.dword!=0 && Flags.get_ZF());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "LOOPZ";}
    }

    final static public class Loopz16 extends JumpCond32_b {
        public int call() {
            reg_ecx.word(reg_ecx.word()-1);
            return jump(reg_ecx.word()!=0 && Flags.get_ZF());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "LOOPZ";}
    }

    final static public class Loop32 extends JumpCond32_b {
        public int call() {
            reg_ecx.dword--;
            return jump(reg_ecx.dword!=0);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
        public String description() {return "LOOP";}
    }

    final static public class Loop16 extends JumpCond32_b {
        public int call() {
            reg_ecx.word(reg_ecx.word()-1);
            return jump(reg_ecx.word()!=0);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
        public String description() {return "LOOP";}
    }

    final static public class Jcxz extends JumpCond32_b {
        int mask;

        public Jcxz(int mask) {
            this.mask = mask;
        }

        public int call() {
            return jump((reg_ecx.dword & mask)==0);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
        public String description() {return "JCXZ";}
    }

    final static public class InEaxIb extends Op {
        int port;

        public InEaxIb() {
            port=decode_fetchb();
        }

        public int call() {
            if (CPU.CPU_IO_Exception(port,4)) return RUNEXCEPTION();
            reg_eax.dword=IO.IO_ReadD(port);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IN "+CPU_Regs.reg_eax.getName()+", 0x"+Integer.toHexString(port);}
    }

    final static public class OutEaxIb extends Op {
        int port;

        public OutEaxIb() {
            port=decode_fetchb();
        }

        public int call() {
            if (CPU.CPU_IO_Exception(port,4)) return RUNEXCEPTION();
            IO.IO_WriteD(port,reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OUT "+CPU_Regs.reg_eax.getName()+", 0x"+Integer.toHexString(port);}
    }

    final static public class CallJd extends Op {
        long addip;

        public CallJd() {
            addip=decode_fetchds();
        }

        public int call() {
            CPU.CPU_Push32(reg_eip+eip_count);
            reg_eip+=addip+eip_count;
            return Constants.BR_Link1;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "CALL "+addip;}
    }

    final static public class JmpJd extends Op {
        long addip;

        public JmpJd() {
            addip=decode_fetchds();
        }

        public int call() {
            reg_eip+=eip_count+addip;
            return Constants.BR_Link1;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "JMP "+addip;}
    }

    final static public class JmpAd extends Op {
        int newip;
        int newcs;
        public JmpAd() {
            newip= decode_fetchd();
            newcs=decode_fetchw();
        }
        public int call() {
            Flags.FillFlags();
            CPU.CPU_JMP(true,newcs,newip,reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "JMP "+Integer.toHexString(newcs)+":"+Integer.toHexString(newip);}
    }

    final static public class JmpJb extends Op {
        int addip;

        public JmpJb() {
            addip=decode_fetchbs();
        }

        public int call() {
            reg_eip+=eip_count+addip;
            return Constants.BR_Link1;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "JMP "+addip;}
    }

    final static public class InEaxDx extends Op {
        public int call() {
            reg_eax.dword=IO.IO_ReadD(reg_edx.word());
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IN "+CPU_Regs.reg_eax.getName()+", "+CPU_Regs.reg_edx.getName();}
    }

    final static public class OutEaxDx extends Op {
        public int call() {
            IO.IO_WriteD(reg_edx.word(),reg_eax.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "OUT "+CPU_Regs.reg_eax.getName()+", "+CPU_Regs.reg_edx.getName();}
    }

    final static public class CallNearEd_reg extends Op {
        Reg eard;

        public CallNearEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int old = reg_eip+eip_count;
            CPU.CPU_Push32(old);
            reg_eip=eard.dword;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "CALL NEAR "+eard.getName();}
    }

    final static public class CallNearEd_mem extends Op {
        EaaBase get_eaa;

        public CallNearEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            int old = reg_eip+eip_count;
            int eip = Memory.mem_readd(eaa);
            CPU.CPU_Push32(old);
            reg_eip = eip;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "CALL NEAR "+get_eaa.description32();}
    }

    final static public class CallFarEd_mem extends Op {
        EaaBase get_eaa;

        public CallFarEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            int newip=Memory.mem_readd(eaa);
            int newcs=Memory.mem_readw(eaa+4);
            FillFlags();
            CPU.CPU_CALL(true,newcs,newip,reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "CALL FAR "+get_eaa.description32();}
    }

    final static public class JmpNearEd_reg extends Op {
        Reg eard;

        public JmpNearEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            reg_eip=eard.dword;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return true;}
        public String description() {return "JMP NEAR "+eard.getName();}
    }

    final static public class JmpNearEd_mem extends Op {
        EaaBase get_eaa;

        public JmpNearEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            reg_eip=Memory.mem_readd(eaa);
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return true;}
        public String description() {return "JMP NEAR "+get_eaa.description32();}
    }

    final static public class JmpFarEd_mem extends Op {
        EaaBase get_eaa;

        public JmpFarEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int newip=Memory.mem_readd(eaa);
            int newcs=Memory.mem_readw(eaa+4);
            FillFlags();
            CPU.CPU_JMP(true,newcs,newip,reg_eip+eip_count);
            if (CPU_TRAP_CHECK) {
                if (GETFLAG(TF)!=0) {
                    CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;
                    return CB_NONE();
                }
            }
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "JMP FAR "+get_eaa.description32();}
    }

    final static public class PushEd_reg extends Op {
        Reg eard;

        public PushEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            CPU.CPU_Push32(eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH "+eard.getName();}
    }

    final static public class PushEd_mem extends Op {
        EaaBase get_eaa;

        public PushEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            CPU.CPU_Push32(Memory.mem_readd(eaa));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "PUSH "+get_eaa.description32();}
    }
}
