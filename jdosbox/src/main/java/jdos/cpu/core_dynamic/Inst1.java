package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.Data;
import jdos.fpu.FPU;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Inst1 extends Helper {
    final static public class Addb_reg extends Op {
        Reg e;
        Reg g;

        public Addb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ADDB(g.get8(), e.get8()));
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
        public String description() {return "ADD "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ADDB;}
    }

    final static public class AddEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public AddEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.ADDB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "ADD "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ADDB;}
    }

    final static public class AddGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public AddGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ADDB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "ADD "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_ADDB;}
    }

    final static public class Addw_reg extends Op {
        Reg e;
        Reg g;

        public Addw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ADDW(g.word(), e.word()));
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
        public String description() {return "ADD "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ADDW;}
    }

    final static public class AddEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public AddEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.ADDW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "ADD "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ADDW;}
    }

    final static public class AddGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public AddGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ADDW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "ADD "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_ADDW;}
    }

    final static public class AddAlIb extends Op {
        int i;

        public AddAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ADDB(i, reg_eax.low()));
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
        public String description() {return "ADD "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ADDB;}
    }

    final static public class AddAxIw extends Op {
        int i;

        public AddAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ADDW(i, reg_eax.word()));
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
        public String description() {return "ADD "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ADDW;}
    }

    final static public class Orb_reg extends Op {
        Reg e;
        Reg g;

        public Orb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ORB(g.get8(), e.get8()));
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
        public String description() {return "OR "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ORB;}
    }

    final static public class OrEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public OrEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.ORB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "OR "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ORB;}
    }

    final static public class OrGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public OrGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ORB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "OR "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_ORB;}
    }

    final static public class Orw_reg extends Op {
        Reg e;
        Reg g;

        public Orw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ORW(g.word(), e.word()));
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
        public String description() {return "OR "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ORW;}
    }

    final static public class OrEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public OrEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.ORW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "OR "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ORW;}
    }

    final static public class OrGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public OrGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ORW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "OR "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_ORW;}
    }

    final static public class OrAlIb extends Op {
        int i;

        public OrAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ORB(i, reg_eax.low()));
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
        public String description() {return "OR "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ORB;}
    }

    final static public class OrAxIw extends Op {
        int i;

        public OrAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ORW(i, reg_eax.word()));
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
        public String description() {return "OR "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ORW;}
    }

    final static public class Adcb_reg extends Op {
        Reg e;
        Reg g;

        public Adcb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ADCB(g.get8(), e.get8()));
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
        public String description() {return "ADC "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ADCB;}
    }

    final static public class AdcEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public AdcEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.ADCB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "ADC "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ADCB;}
    }

    final static public class AdcGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public AdcGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ADCB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "ADC "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_ADCB;}
    }

    final static public class Adcw_reg extends Op {
        Reg e;
        Reg g;

        public Adcw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ADCW(g.word(), e.word()));
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
        public String description() {return "ADC "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ADCW;}
    }

    final static public class AdcEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public AdcEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.ADCW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "ADC "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ADCW;}
    }

    final static public class AdcGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public AdcGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ADCW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "ADC "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_ADCW;}
    }

    final static public class AdcAlIb extends Op {
        int i;

        public AdcAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ADCB(i, reg_eax.low()));
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
        public String description() {return "ADC "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ADCB;}
    }

    final static public class AdcAxIw extends Op {
        int i;

        public AdcAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ADCW(i, reg_eax.word()));
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
        public String description() {return "ADC "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ADCW;}
    }
    
    final static public class Sbbb_reg extends Op {
        Reg e;
        Reg g;

        public Sbbb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.SBBB(g.get8(), e.get8()));
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
        public String description() {return "SBB "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_SBBB;}
    }

    final static public class SbbEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public SbbEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.SBBB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "SBB "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_SBBB;}
    }

    final static public class SbbGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public SbbGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.SBBB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "SBB "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_SBBB;}
    }

    final static public class Sbbw_reg extends Op {
        Reg e;
        Reg g;

        public Sbbw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.SBBW(g.word(), e.word()));
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
        public String description() {return "SBB "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_SBBW;}
    }

    final static public class SbbEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public SbbEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.SBBW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "SBB "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_SBBW;}
    }

    final static public class SbbGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public SbbGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.SBBW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "SBB "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_SBBW;}
    }

    final static public class SbbAlIb extends Op {
        int i;

        public SbbAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.SBBB(i, reg_eax.low()));
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
        public String description() {return "SBB "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_SBBB;}
    }

    final static public class SbbAxIw extends Op {
        int i;

        public SbbAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.SBBW(i, reg_eax.word()));
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
        public String description() {return "SBB "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_SBBW;}
    }

    final static public class Andb_reg extends Op {
        Reg e;
        Reg g;

        public Andb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.ANDB(g.get8(), e.get8()));
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
        public String description() {return "AND "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ANDB;}
    }

    final static public class AndEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public AndEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.ANDB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "AND "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_ANDB;}
    }

    final static public class AndGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public AndGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.ANDB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "AND "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_ANDB;}
    }

    final static public class Andw_reg extends Op {
        Reg e;
        Reg g;

        public Andw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.ANDW(g.word(), e.word()));
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
        public String description() {return "AND "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ANDW;}
    }

    final static public class AndEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public AndEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.ANDW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "AND "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_ANDW;}
    }

    final static public class AndGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public AndGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.ANDW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "AND "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_ANDW;}
    }

    final static public class AndAlIb extends Op {
        int i;

        public AndAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.ANDB(i, reg_eax.low()));
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
        public String description() {return "AND "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ANDB;}
    }

    final static public class AndAxIw extends Op {
        int i;

        public AndAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.ANDW(i, reg_eax.word()));
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
        public String description() {return "AND "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_ANDW;}
    }

    final static public class Subb_reg extends Op {
        Reg e;
        Reg g;

        public Subb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.SUBB(g.get8(), e.get8()));
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
        public String description() {return "SUB "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_SUBB;}
    }

    final static public class SubEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public SubEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.SUBB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "SUB "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_SUBB;}
    }

    final static public class SubGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public SubGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.SUBB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "SUB "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_SUBB;}
    }

    final static public class Subw_reg extends Op {
        Reg e;
        Reg g;

        public Subw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.SUBW(g.word(), e.word()));
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
        public String description() {return "SUB "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_SUBW;}
    }

    final static public class SubEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public SubEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.SUBW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "SUB "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_SUBW;}
    }

    final static public class SubGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public SubGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.SUBW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "SUB "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_SUBW;}
    }

    final static public class SubAlIb extends Op {
        int i;

        public SubAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.SUBB(i, reg_eax.low()));
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
        public String description() {return "SUB "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_SUBB;}
    }

    final static public class SubAxIw extends Op {
        int i;

        public SubAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.SUBW(i, reg_eax.word()));
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
        public String description() {return "SUB "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_SUBW;}
    }

    final static public class Xorb_reg extends Op {
        Reg e;
        Reg g;

        public Xorb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.set8(Instructions.XORB(g.get8(), e.get8()));
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
        public String description() {return "XOR "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_XORB;}
    }

    final static public class XorEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public XorEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writeb(eaa, Instructions.XORB(g.get8(), Memory.mem_readb(eaa)));
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
        public String description() {return "XOR "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_XORB;}
    }

    final static public class XorGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public XorGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.set8(Instructions.XORB(Memory.mem_readb(eaa), e.get8()));
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
        public String description() {return "XOR "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_XORB;}
    }

    final static public class Xorw_reg extends Op {
        Reg e;
        Reg g;

        public Xorw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            e.word(Instructions.XORW(g.word(), e.word()));
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
        public String description() {return "XOR "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_XORW;}
    }

    final static public class XorEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public XorEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Memory.mem_writew(eaa, Instructions.XORW(g.word(), Memory.mem_readw(eaa)));
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
        public String description() {return "XOR "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_XORW;}
    }

    final static public class XorGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public XorGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            e.word(Instructions.XORW(Memory.mem_readw(eaa), e.word()));
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
        public String description() {return "XOR "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_XORW;}
    }

    final static public class XorAlIb extends Op {
        int i;

        public XorAlIb(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.low(Instructions.XORB(i, reg_eax.low()));
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
        public String description() {return "XOR "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_XORB;}
    }

    final static public class XorAxIw extends Op {
        int i;

        public XorAxIw(int i) {
            this.i = i;
        }
        public int call() {
            reg_eax.word(Instructions.XORW(i, reg_eax.word()));
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
        public String description() {return "XOR "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_XORW;}
    }

    final static public class Cmpb_reg extends Op {
        Reg e;
        Reg g;

        public Cmpb_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            Instructions.CMPB(g.get8(), e.get8());
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
        public String description() {return "CMP "+e.getName8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_CMPB;}
    }

    final static public class CmpEbGb_mem extends Op {
        EaaBase e;
        Reg g;

        public CmpEbGb_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Instructions.CMPB(g.get8(), Memory.mem_readb(eaa));
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
        public String description() {return "CMP "+e.description8()+", "+g.getName8();}
        public int getFlagType() {return FLAG_TYPE_CMPB;}
    }

    final static public class CmpGbEb_mem extends Op {
        EaaBase g;
        Reg e;

        public CmpGbEb_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            Instructions.CMPB(Memory.mem_readb(eaa), e.get8());
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
        public String description() {return "CMP "+e.getName8()+", "+g.description8();}
        public int getFlagType() {return FLAG_TYPE_CMPB;}
    }

    final static public class Cmpw_reg extends Op {
        Reg e;
        Reg g;

        public Cmpw_reg(Reg e, Reg g) {
            this.e = e;
            this.g = g;
        }

        public int call() {
            Instructions.CMPW(g.word(), e.word());
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
        public String description() {return "CMP "+e.getName16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_CMPW;}
    }

    final static public class CmpEwGw_mem extends Op {
        EaaBase e;
        Reg g;

        public CmpEwGw_mem(EaaBase e, Reg g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = e.call();
            Instructions.CMPW(g.word(), Memory.mem_readw(eaa));
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
        public String description() {return "CMP "+e.description16()+", "+g.getName16();}
        public int getFlagType() {return FLAG_TYPE_CMPW;}
    }

    final static public class CmpGwEw_mem extends Op {
        EaaBase g;
        Reg e;

        public CmpGwEw_mem(Reg e, EaaBase g) {
            this.e = e;
            this.g = g;
        }
        public int call() {
            int eaa = g.call();
            Instructions.CMPW(Memory.mem_readw(eaa), e.word());
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
        public String description() {return "CMP "+e.getName16()+", "+g.description16();}
        public int getFlagType() {return FLAG_TYPE_CMPW;}
    }

    final static public class CmpAlIb extends Op {
        int i;

        public CmpAlIb(int i) {
            this.i = i;
        }
        public int call() {
            Instructions.CMPB(i, reg_eax.low());
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
        public String description() {return "CMP "+CPU_Regs.reg_eax.getName8()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_CMPB;}
    }

    final static public class CmpAxIw extends Op {
        int i;

        public CmpAxIw(int i) {
            this.i = i;
        }
        public int call() {
            Instructions.CMPW(i, reg_eax.word());
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
        public String description() {return "CMP "+CPU_Regs.reg_eax.getName16()+", "+i;}
        public int getFlagType() {return FLAG_TYPE_CMPW;}
    }

    final static public class PushES extends Op {
        public int call() {
            CPU.CPU_Push16(CPU_Regs.reg_esVal.dword);
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

    final static public class PopES extends Op {
        public int call() {
            if (CPU.CPU_PopSegES(false)) return RUNEXCEPTION();
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

    final static public class PushCS extends Op {
        public int call() {
            CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
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

    final static public class PushSS extends Op {
        public int call() {
            CPU.CPU_Push16(CPU_Regs.reg_ssVal.dword);
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

    final static public class PopSS extends Op {
        public int call() {
            if (CPU.CPU_PopSegSS(false)) return RUNEXCEPTION();
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

    final static public class PushDS extends Op {
        public int call() {
            CPU.CPU_Push16(CPU_Regs.reg_dsVal.dword);
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

    final static public class PopDS extends Op {
        public int call() {
            if (CPU.CPU_PopSegDS(false)) return RUNEXCEPTION();
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

    final static public class SegES extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_ES();
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
        public String description() {return "SEG ES";}
    }

    final static public class SegCS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_CS();
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
        public String description() {return "SEG CS";}
    }

    final static public class SegSS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_SS();
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
        public String description() {return "SEG SS";}
    }

    final static public class SegDS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_DS();
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
        public String description() {return "SEG DS";}
    }

    final static public class SegFS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_FS();
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
        public String description() {return "SEG FS";}
    }

    final static public class SegGS extends Op {
        public int call() {
            Core.DO_PREFIX_SEG_GS();
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
        public String description() {return "SEG GS";}
    }

    final static public class Daa extends Op {
        public int call() {
            Instructions.DAA();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // OF is undefined
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.AF | CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "DAA";}
    }

    final static public class Das extends Op {
        public int call() {
            Instructions.DAS();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.AF | CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "DAS";}
    }

    final static public class Aaa extends Op {
        public int call() {
            Instructions.AAA();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.AF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AAA";}
    }

    final static public class Aas extends Op {
        public int call() {
            Instructions.AAS();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return CPU_Regs.AF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AAS";}
    }

    final static public class Incw extends Op {
        Reg reg;

        public Incw(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            LoadCF();
            lf_var1w(reg.word());
            lf_resw(lf_var1w()+1);
            reg.word(lf_resw());
            type=t_INCw;
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
        public String description() {return "INC "+reg.getName16();}
        public int getFlagType() {return FLAG_TYPE_INCW;}
    }

    final static public class Decw extends Op {
        Reg reg;

        public Decw(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            LoadCF();
            lf_var1w(reg.word());
            lf_resw(lf_var1w()-1);
            reg.word(lf_resw());
            type=t_DECw;
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
        public String description() {return "DEC "+reg.getName16();}
        public int getFlagType() {return FLAG_TYPE_DECW;}
    }

    final static public class Pushw extends Op {
        Reg reg;

        public Pushw(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            CPU.CPU_Push16(reg.word());
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
        public String description() {return "PUSH "+reg.getName16();}
    }

    final static public class Popw extends Op {
        Reg reg;

        public Popw(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            reg.word(CPU.CPU_Pop16());
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
        public String description() {return "POP "+reg.getName16();}
    }

    final static public class DoStringException extends Op {
        int type;
        int width;
        int prefixes;
        boolean rep_zero;

        public DoStringException(int type, int width) {
            this.type = type;
            this.width = width;
            this.prefixes = Helper.prefixes;
            rep_zero = Core.rep_zero;
        }

        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),width)) return RUNEXCEPTION();
            Core.rep_zero = rep_zero;
            StringOp.DoString(prefixes, type);
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
        public String description() {return StringOp.description(type)+" prefix=0x"+Integer.toHexString(prefixes);}
    }

    final static public class DoString extends Op {
        int type;
        int prefixes;
        boolean rep_zero;

        public DoString(int type) {
            this.prefixes = Helper.prefixes;
            this.type = type;
            this.rep_zero = Core.rep_zero;
        }
        public int call() {
            Core.rep_zero = rep_zero;
            StringOp.DoString(prefixes, type);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0; // :TODO: what about cmp
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return StringOp.description(type)+" prefix=0x"+Integer.toHexString(prefixes);}
    }

    final static public class Pusha extends Op {
        public int call() {
            /*Bit16u*/int old_sp=reg_esp.word();
            int esp = reg_esp.dword;
            esp = CPU.CPU_Push16(esp, reg_eax.word());
            esp = CPU.CPU_Push16(esp, reg_ecx.word());
            esp = CPU.CPU_Push16(esp, reg_edx.word());
            esp = CPU.CPU_Push16(esp, reg_ebx.word());
            esp = CPU.CPU_Push16(esp, old_sp);
            esp = CPU.CPU_Push16(esp, reg_ebp.word());
            esp = CPU.CPU_Push16(esp, reg_esi.word());
            esp = CPU.CPU_Push16(esp, reg_edi.word());
            // Don't store ESP until all the memory writes are done in case of a PF so that this op can be reentrant
            reg_esp.word(esp);
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

    final static public class Popa extends Op {
        public int call() {
            reg_edi.word(CPU.CPU_Pop16());reg_esi.word(CPU.CPU_Pop16());reg_ebp.word(CPU.CPU_Pop16());CPU.CPU_Pop16();//Don't save SP
            reg_ebx.word(CPU.CPU_Pop16());reg_edx.word(CPU.CPU_Pop16());reg_ecx.word(CPU.CPU_Pop16());reg_eax.word(CPU.CPU_Pop16());
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

    final static public class Bound extends Op {
        Reg reg;
        EaaBase get_eaa;
        public Bound() {
            int rm=decode_fetchb();
            reg = Mod.gw(rm);
            get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            short bound_min, bound_max;
            int eaa=get_eaa.call();
            bound_min=(short)Memory.mem_readw(eaa);
            bound_max=(short)Memory.mem_readw(eaa+2);
            if ( (((short)reg.word()) < bound_min) || (((short)reg.word()) > bound_max) ) {
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
        public String description() {return "BOUND "+reg.getName16()+", "+get_eaa.description16();}
    }

    final static public class ArplEwRw_reg extends Op {
        Reg earw;
        Reg rw;

        public ArplEwRw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int value = earw.word();
            value = CPU.CPU_ARPL(value, rw.word());
            earw.word(value);
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
        public String description() {return "ARPL "+earw.getName16()+", "+rw.getName16();}
    }

    final static public class ArplEwRw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public ArplEwRw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();
            int value = Memory.mem_readw(eaa);
            value = CPU.CPU_ARPL(value,rw.word());
            Memory.mem_writew(eaa,value);
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
        public String description() {return "ARPL "+get_eaa.description16()+", "+rw.getName16();}
    }

    final static public class Push16 extends Op {
        int value;

        public Push16(int value) {
            this.value = value;
        }

        public int call() {
            CPU.CPU_Push16(value);
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
        public String description() {return "PUSHw "+value;}
    }

    final static public class IMULGwEwIw_reg extends Op {
        int op3;
        Reg rw;
        Reg earw;

        public IMULGwEwIw_reg(int rm) {
            op3 = decode_fetchws();
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            rw.word(Instructions.DIMULW(earw.word(),op3));
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
        public String description() {return "IMUL "+rw.getName16()+", "+earw.getName16()+", "+op3;}
    }

    final static public class IMULGwEwIw_mem extends Op {
        int op3;
        Reg rw;
        EaaBase get_eaa;

        public IMULGwEwIw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            op3 = decode_fetchws();
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rw.word(Instructions.DIMULW(Memory.mem_readw(eaa),op3));
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
        public String description() {return "IMUL "+rw.getName16()+", "+get_eaa.description16()+", "+op3;}
    }

    final static public class IMULGwEwIb_reg extends Op {
        int op3;
        Reg rw;
        Reg earw;

        public IMULGwEwIb_reg(int rm) {
            op3 = decode_fetchbs();
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            rw.word(Instructions.DIMULW(earw.word(),op3));
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
        public String description() {return "IMUL "+rw.getName16()+", "+earw.getName16()+", "+op3;}
    }

    final static public class IMULGwEwIb_mem extends Op {
        int op3;
        Reg rw;
        EaaBase get_eaa;

        public IMULGwEwIb_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            op3 = decode_fetchbs();
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rw.word(Instructions.DIMULW(Memory.mem_readw(eaa),op3));
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
        public String description() {return "IMUL "+rw.getName16()+", "+get_eaa.description16()+", "+op3;}
    }

    static abstract public class JumpCond16_b extends Op {
        int offset;
        public JumpCond16_b() {
            offset = decode_fetchbs();
        }

        final protected int jump(boolean COND) {
            if (COND) {
                reg_ip(reg_ip()+offset+eip_count);
                return Constants.BR_Link1;
            }
            reg_ip(reg_ip()+eip_count);
            return Constants.BR_Link2;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
    }

    final static public class JumpCond16_b_o extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_O());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
        public String description() {return "JO";}
    }

    final static public class JumpCond16_b_no extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NO());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
        public String description() {return "JNO";}
    }

    final static public class JumpCond16_b_b extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_B());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
        public String description() {return "JB";}
    }

    final static public class JumpCond16_b_nb extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NB());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
        public String description() {return "JNB";}
    }

    final static public class JumpCond16_b_z extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_Z());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "JZ";}
    }

    final static public class JumpCond16_b_nz extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NZ());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "JNZ";}
    }

    final static public class JumpCond16_b_be extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_BE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
        public String description() {return "JBE";}
    }

    final static public class JumpCond16_b_nbe extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NBE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
        public String description() {return "JNBE";}
    }

    final static public class JumpCond16_b_s extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_S());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
        public String description() {return "JS";}
    }

    final static public class JumpCond16_b_ns extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NS());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
        public String description() {return "JNS";}
    }

    final static public class JumpCond16_b_p extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_P());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
        public String description() {return "JP";}
    }

    final static public class JumpCond16_b_np extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NP());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
        public String description() {return "JNP";}
    }

    final static public class JumpCond16_b_l extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_L());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
        public String description() {return "JL";}
    }

    final static public class JumpCond16_b_nl extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NL());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
        public String description() {return "JNL";}
    }

    final static public class JumpCond16_b_le extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_LE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
        public String description() {return "JLE";}
    }

    final static public class JumpCond16_b_nle extends JumpCond16_b {
        public int call() {
            return jump(Flags.TFLG_NLE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
        public String description() {return "JNLE";}
    }

    final static public class GrplEbIb_reg_add extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_add(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.ADDB(ib,earb.get8()));
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
        public String description() {return "ADD "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADDB;}
    }

    final static public class GrplEbIb_reg_or extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_or(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.ORB(ib,earb.get8()));
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
        public String description() {return "OR "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ORB;}
    }

    final static public class GrplEbIb_reg_adc extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_adc(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.ADCB(ib,earb.get8()));
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
        public String description() {return "ADC "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADCB;}
    }

    final static public class GrplEbIb_reg_sbb extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_sbb(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.SBBB(ib,earb.get8()));
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
        public String description() {return "SBB "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SBBB;}
    }

    final static public class GrplEbIb_reg_and extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_and(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.ANDB(ib,earb.get8()));
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
        public String description() {return "AND "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ANDB;}
    }

    final static public class GrplEbIb_reg_sub extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_sub(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.SUBB(ib,earb.get8()));
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
        public String description() {return "SUB "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SUBB;}
    }

    final static public class GrplEbIb_reg_xor extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_xor(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            earb.set8(Instructions.XORB(ib,earb.get8()));
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
        public String description() {return "XOR "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_XORB;}
    }

    final static public class GrplEbIb_reg_cmp extends Op {
        Reg earb;
        int ib;

        public GrplEbIb_reg_cmp(int rm) {
            earb = Mod.eb(rm);
            ib=decode_fetchb();
        }

        public int call() {
            Instructions.CMPB(ib,earb.get8());
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
        public String description() {return "CMP "+earb.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_CMPB;}
    }

    final static public class GrplEbIb_mem_add extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_add(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.ADDB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "ADD "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADDB;}
    }

    final static public class GrplEbIb_mem_or extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_or(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.ORB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "OR "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ORB;}
    }

    final static public class GrplEbIb_mem_adc extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_adc(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.ADCB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "ADC "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADCB;}
    }

    final static public class GrplEbIb_mem_sbb extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_sbb(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.SBBB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "SBB "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SBBB;}
    }

    final static public class GrplEbIb_mem_and extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_and(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.ANDB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "AND "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ANDB;}
    }

    final static public class GrplEbIb_mem_sub extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_sub(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.SUBB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "SUB "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SUBB;}
    }

    final static public class GrplEbIb_mem_xor extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_xor(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.XORB(ib,Memory.mem_readb(eaa)));
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
        public String description() {return "XOR "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_XORB;}
    }

    final static public class GrplEbIb_mem_cmp extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEbIb_mem_cmp(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.CMPB(ib,Memory.mem_readb(eaa));
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
        public String description() {return "CMP "+get_eaa.description8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_CMPB;}
    }

    final static public class GrplEwIw_reg_add extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_add(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.ADDW(ib,earw.word()));
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
        public String description() {return "ADD "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADDW;}
    }

    final static public class GrplEwIw_reg_or extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_or(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.ORW(ib,earw.word()));
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
        public String description() {return "OR "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ORW;}
    }

    final static public class GrplEwIw_reg_adc extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_adc(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.ADCW(ib,earw.word()));
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
        public String description() {return "ADC "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADCW;}
    }

    final static public class GrplEwIw_reg_sbb extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_sbb(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.SBBW(ib,earw.word()));
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
        public String description() {return "SBB "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SBBW;}
    }

    final static public class GrplEwIw_reg_and extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_and(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.ANDW(ib,earw.word()));
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
        public String description() {return "AND "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ANDW;}
    }

    final static public class GrplEwIw_reg_sub extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_sub(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.SUBW(ib,earw.word()));
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
        public String description() {return "SUB "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SUBW;}
    }

    final static public class GrplEwIw_reg_xor extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_xor(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            earw.word(Instructions.XORW(ib,earw.word()));
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
        public String description() {return "XOR "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_XORW;}
    }

    final static public class GrplEwIw_reg_cmp extends Op {
        Reg earw;
        int ib;

        public GrplEwIw_reg_cmp(int rm, boolean signed) {
            earw = Mod.ew(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            Instructions.CMPW(ib,earw.word());
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
        public String description() {return "CMP "+earw.getName16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_CMPW;}
    }

    final static public class GrplEwIw_mem_add extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_add(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.ADDW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "ADD "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADDW;}
    }

    final static public class GrplEwIw_mem_or extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_or(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.ORW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "OR "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ORW;}
    }

    final static public class GrplEwIw_mem_adc extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_adc(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.ADCW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "ADC "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ADCW;}
    }

    final static public class GrplEwIw_mem_sbb extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_sbb(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.SBBW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "SBB "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SBBW;}
    }

    final static public class GrplEwIw_mem_and extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_and(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.ANDW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "AND "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_ANDW;}
    }

    final static public class GrplEwIw_mem_sub extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_sub(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.SUBW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "SUB "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_SUBW;}
    }

    final static public class GrplEwIw_mem_xor extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_xor(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.XORW(ib,Memory.mem_readw(eaa)));
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
        public String description() {return "XOR "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_XORW;}
    }

    final static public class GrplEwIw_mem_cmp extends Op {
        int ib;
        EaaBase get_eaa;

        public GrplEwIw_mem_cmp(int rm, boolean signed) {
            get_eaa= Mod.getEaa(rm);
            if (signed)
                ib = (((short)decode_fetchbs()) & 0xFFFF);
            else
                ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.CMPW(ib,Memory.mem_readw(eaa));
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
        public String description() {return "CMP "+get_eaa.description16()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_CMPW;}
    }

    final static public class TestEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public TestEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.TESTB(rb.get8(),earb.get8());
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
        public String description() {return "TEST "+earb.getName8()+", "+rb.getName8();}
        public int getFlagType() {return FLAG_TYPE_TESTB;}
    }

    final static public class TestEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;

        public TestEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTB(rb.get8(),Memory.mem_readb(eaa));
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
        public String description() {return "TEST "+get_eaa.description8()+", "+rb.getName8();}
        public int getFlagType() {return FLAG_TYPE_TESTB;}
    }

    final static public class TestEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public TestEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.TESTW(rw.word(),earw.word());
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
        public String description() {return "TEST "+earw.getName16()+", "+rw.getName16();}
        public int getFlagType() {return FLAG_TYPE_TESTW;}
    }

    final static public class TestEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public TestEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTW(rw.word(),Memory.mem_readw(eaa));
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
        public String description() {return "TEST "+get_eaa.description16()+", "+rw.getName16();}
        public int getFlagType() {return FLAG_TYPE_TESTW;}
    }

    final static public class XchgEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public XchgEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            int oldrmrb=rb.get8();
            rb.set8(earb.get8());
            earb.set8(oldrmrb);
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
        public String description() {return "XCHG "+earb.getName8()+", "+rb.getName8();}
    }

    final static public class XchgEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;
        public XchgEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int oldrmrb=rb.get8();
            int eaa = get_eaa.call();
            int newrb = Memory.mem_readb(eaa);
            Memory.mem_writeb(eaa,oldrmrb);
            rb.set8(newrb);
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
        public String description() {return "XCHG "+get_eaa.description8()+", "+rb.getName8();}
    }

    final static public class XchgEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public XchgEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            int oldrmrw=rw.word();
            rw.word(earw.word());
            earw.word(oldrmrw);
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
        public String description() {return "XCHG "+earw.getName16()+", "+rw.getName16();}
    }

    final static public class XchgEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        public XchgEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int oldrmrw=rw.word();
            int eaa = get_eaa.call();
            int newrw = Memory.mem_readw(eaa);
            Memory.mem_writew(eaa,oldrmrw);
            rw.word(newrw);
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
        public String description() {return "XCHG "+get_eaa.description16()+", "+rw.getName16();}
    }

    final static public class MovEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public MovEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            earb.set8(rb.get8());
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
        public String description() {return "MOV "+earb.getName8()+", "+rb.getName8();}
    }

    final static public class MovEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;
        public MovEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa,rb.get8());
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
        public String description() {return "MOV "+get_eaa.description8()+", "+rb.getName8();}
    }

    static final CPU.Descriptor desc=new CPU.Descriptor();

    final static public class MovEbGb_mem_5 extends Op {
        Reg rb;
        EaaBase get_eaa;
        public MovEbGb_mem_5(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if (CPU.cpu.pmode && !CPU.cpu.code.big) {
                CPU.cpu.gdt.GetDescriptor(CPU.seg_value(Core.base_val_ds),desc);
                if ((desc.Type()==CPU.DESC_CODE_R_NC_A) || (desc.Type()==CPU.DESC_CODE_R_NC_NA)) {
                    CPU.CPU_Exception(CPU.EXCEPTION_GP,CPU.seg_value(Core.base_val_ds) & 0xfffc);
                    return Constants.BR_Jump;
                }
            }
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa,rb.get8());
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
        public String description() {return "MOV "+get_eaa.description8()+", "+rb.getName8();}
    }

    final static public class MovEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public MovEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(rw.word());
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
        public String description() {return "MOV "+earw.getName16()+", "+rw.getName16();}
    }

    final static public class MovEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        public MovEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,rw.word());
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
        public String description() {return "MOV "+get_eaa.description16()+", "+rw.getName16();}
    }

    final static public class MovGbEb_reg extends Op {
        Reg rb;
        Reg earb;

        public MovGbEb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            rb.set8(earb.get8());
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
        public String description() {return "MOV "+rb.getName8()+", "+earb.getName8();}
    }

    final static public class MovGbEb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;
        public MovGbEb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rb.set8(Memory.mem_readb(eaa));
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
        public String description() {return "MOV "+rb.getName8()+", "+get_eaa.description8();}
    }

    final static public class MovGwEw_reg extends Op {
        Reg rw;
        Reg earw;

        public MovGwEw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            rw.word(earw.word());
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
        public String description() {return "MOV "+rw.getName16()+", "+earw.getName16();}
    }

    final static public class MovGwEw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        public MovGwEw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rw.word(Memory.mem_readw(eaa));
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
        public String description() {return "MOV "+rw.getName16()+", "+get_eaa.description16();}
    }

    final static public class MovEwEs_reg extends Op {
        Reg earw;
        public MovEwEs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU_Regs.reg_esVal.dword);
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
        public String description() {return "MOV "+earw.getName16()+", ES";}
    }

    final static public class MovEwEs_mem extends Op {
        EaaBase get_eaa;
        public MovEwEs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,CPU_Regs.reg_esVal.dword);
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
        public String description() {return "MOV "+get_eaa.description16()+", ES";}
    }

    final static public class MovEwCs_reg extends Op {
        Reg earw;
        public MovEwCs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU_Regs.reg_csVal.dword);
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
        public String description() {return "MOV "+earw.getName16()+", CS";}
    }

    final static public class MovEwCs_mem extends Op {
        EaaBase get_eaa;
        public MovEwCs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,CPU_Regs.reg_csVal.dword);
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
        public String description() {return "MOV "+get_eaa.description16()+", CS";}
    }

    final static public class MovEwSs_reg extends Op {
        Reg earw;
        public MovEwSs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU_Regs.reg_ssVal.dword);
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
        public String description() {return "MOV "+earw.getName16()+", SS";}
    }

    final static public class MovEwSs_mem extends Op {
        EaaBase get_eaa;
        public MovEwSs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,CPU_Regs.reg_ssVal.dword);
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
        public String description() {return "MOV "+get_eaa.description16()+", SS";}
    }

    final static public class MovEwDs_reg extends Op {
        Reg earw;
        public MovEwDs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU_Regs.reg_dsVal.dword);
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
        public String description() {return "MOV "+earw.getName16()+", DS";}
    }

    final static public class MovEwDs_mem extends Op {
        EaaBase get_eaa;
        public MovEwDs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,CPU_Regs.reg_dsVal.dword);
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
        public String description() {return "MOV "+get_eaa.description16()+", DS";}
    }

    final static public class MovEwFs_reg extends Op {
        Reg earw;
        public MovEwFs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU_Regs.reg_fsVal.dword);
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
        public String description() {return "MOV "+earw.getName16()+", FS";}
    }

    final static public class MovEwFs_mem extends Op {
        EaaBase get_eaa;
        public MovEwFs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,CPU_Regs.reg_fsVal.dword);
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
        public String description() {return "MOV "+get_eaa.description16()+", FS";}
    }

    final static public class MovEwGs_reg extends Op {
        Reg earw;
        public MovEwGs_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU_Regs.reg_gsVal.dword);
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
        public String description() {return "MOV "+earw.getName16()+", GS";}
    }

    final static public class MovEwGs_mem extends Op {
        EaaBase get_eaa;
        public MovEwGs_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,CPU_Regs.reg_gsVal.dword);
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
        public String description() {return "MOV "+get_eaa.description16()+", GS";}
    }

    final static public class Illegal extends Op {
        String msg;
        public Illegal(String msg) {
            this.msg = msg;
            if (msg == null || msg.length()==0)
                this.msg = "Illegal Instruction";
        }

        public int call() {
            Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,msg);
            return Constants.BR_Illegal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean returnsIllegal() {return true;}
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "Illegal";}
    }

    final static public class LeaGw_16 extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LeaGw_16(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa16(rm);
        }

        public int call() {
            // :TODO: research if the base_ds is alway CPU_Regs.reg_dsPhys.dword etc.
            //Little hack to always use segprefixed version
            Core.base_ds=Core.base_ss=0;
            int eaa = get_eaa.call();
            rw.word(eaa);
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
        public String description() {return "LEA "+rw.getName16()+", "+get_eaa.descriptionZero();}
    }

    final static public class LeaGw_32 extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LeaGw_32(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa32(rm);
        }

        public int call() {
            // :TODO: research if the base_ds is alway CPU_Regs.reg_dsPhys.dword etc.
            //Little hack to always use segprefixed version
            Core.base_ds=Core.base_ss=0;
            int eaa = get_eaa.call();
            rw.word(eaa);
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
        public String description() {return "LEA "+rw.getName16()+", "+get_eaa.descriptionZero();}
    }

    final static public class MovEsEw_reg extends Op {
        Reg earw;
        public MovEsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralES(earw.word())) return RUNEXCEPTION();
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
        public int setsSeg() {return ES|FROM_REG;}
        public String description() {return "MOV ES, "+earw.getName16();}
    }

    final static public class MovEsEw_mem extends Op {
        EaaBase get_eaa;
        public MovEsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa))) return RUNEXCEPTION();
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
        public String description() {return "MOV ES, "+get_eaa.description16();}
    }

    final static public class MovSsEw_reg extends Op {
        Reg earw;
        public MovSsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralSS(earw.word())) return RUNEXCEPTION();
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
        public int setsSeg() {return SS|FROM_REG;}
        public String description() {return "MOV SS, "+earw.getName16();}
    }

    final static public class MovSsEw_mem extends Op {
        EaaBase get_eaa;
        public MovSsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
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
        public int setsSeg() {return SS|FROM_MEMORY;}
        public String description() {return "MOV SS, "+get_eaa.description16();}
    }

    final static public class MovDsEw_reg extends Op {
        Reg earw;
        public MovDsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralDS(earw.word())) return RUNEXCEPTION();
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
        public int setsSeg() {return DS|FROM_REG;}
        public String description() {return "MOV DS, "+earw.getName16();}
    }

    final static public class MovDsEw_mem extends Op {
        EaaBase get_eaa;
        public MovDsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
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
        public int setsSeg() {return DS|FROM_MEMORY;}
        public String description() {return "MOV DS, "+get_eaa.description16();}
    }

    final static public class MovFsEw_reg extends Op {
        Reg earw;
        public MovFsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralFS(earw.word())) return RUNEXCEPTION();
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
        public int setsSeg() {return FS|FROM_REG;}
        public String description() {return "MOV FS, "+earw.getName16();}
    }

    final static public class MovFsEw_mem extends Op {
        EaaBase get_eaa;
        public MovFsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
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
        public int setsSeg() {return FS|FROM_MEMORY;}
        public String description() {return "MOV FS, "+get_eaa.description16();}
    }

    final static public class MovGsEw_reg extends Op {
        Reg earw;
        public MovGsEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_SetSegGeneralGS(earw.word())) return RUNEXCEPTION();
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
        public int setsSeg() {return GS|FROM_REG;}
        public String description() {return "MOV GS, "+earw.getName16();}
    }

    final static public class MovGsEw_mem extends Op {
        EaaBase get_eaa;
        public MovGsEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa))) return RUNEXCEPTION();
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
        public int setsSeg() {return GS|FROM_MEMORY;}
        public String description() {return "MOV GS, "+get_eaa.description16();}
    }

    final static public class PopEw_reg extends Op {
        Reg earw;

        public PopEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(CPU.CPU_Pop16());
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
        public String description() {return "POP "+earw.getName16();}
    }

    final static public class PopEw_mem extends Op {
        EaaBase get_eaa;

        public PopEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int val = CPU.CPU_Pop16();
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, val);
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
        public String description() {return "POP "+get_eaa.description16();}
    }

    final static public class Noop extends Op {
        public int call() {
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
        public String description() {return "NOOP";}
    }

    final static public class XchgAx extends Op {
        Reg reg;

        public XchgAx(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            int old=reg.word();
            reg.word(reg_eax.word());
            reg_eax.word(old);
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
        public String description() {return "XCHG "+reg.getName16()+", "+CPU_Regs.reg_eax.getName16();}
    }

    final static public class CallAp extends Op {
        int newcs;
        int newip;

        public CallAp(int newcs, int newip) {
            this.newcs = newcs;
            this.newip = newip;
        }

        public int call() {
            Flags.FillFlags();
            CPU.CPU_CALL(false,newcs,newip,reg_eip+eip_count);
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
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CALL "+Integer.toHexString(newcs)+":"+Integer.toHexString(newip);}
    }

    final static public class PushF extends Op {
        public int call() {
            if (CPU.CPU_PUSHF(false)) return RUNEXCEPTION();
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

    final static public class PopF extends Op {
        public int call() {
            if (CPU.CPU_POPF(false)) return RUNEXCEPTION();
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

    final static public class Sahf extends Op {
        public int call() {
            Flags.SETFLAGSb(reg_eax.high());
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // OF is not included
        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAHF";}
    }

    final static public class Lahf extends Op {
        public int call() {
            Flags.FillFlags();
            reg_eax.high(CPU_Regs.flags & 0xff);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        // OF is not included
        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.PF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "LAHF";}
    }

    abstract static public class GetEADirect extends Op {
        int value;

        public GetEADirect() {
            if ((prefixes & Core.PREFIX_ADDR)!=0) {
                value = decode_fetchd();
            } else {
                value = decode_fetchw();
            }
        }
    }
    final static public class MovALOb extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            reg_eax.low(Memory.mem_readb(eaa));
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
        public String description() {return "MOV "+CPU_Regs.reg_eax.getName8()+", 0x"+Integer.toHexString(Core.base_ds+value);}
    }

    final static public class MovAXOw extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            reg_eax.word(Memory.mem_readw(eaa));
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
        public String description() {return "MOV "+CPU_Regs.reg_eax.getName16()+", 0x"+Integer.toHexString(Core.base_ds+value);}
    }

    final static public class MovObAL extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            Memory.mem_writeb(eaa, reg_eax.low());
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
        public String description() {return "MOV "+"0x"+Integer.toHexString(Core.base_ds+value)+", "+CPU_Regs.reg_eax.getName8();}
    }

    final static public class MovOwAX extends GetEADirect {
        public int call() {
            int eaa = (Core.base_ds+value);
            Memory.mem_writew(eaa, reg_eax.word());
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
        public String description() {return "MOV "+"0x"+Integer.toHexString(Core.base_ds+value)+", "+CPU_Regs.reg_eax.getName16();}
    }

    final static public class TestAlIb extends Op {
        int ib;
        public TestAlIb() {
            ib = decode_fetchb();
        }
        public int call() {
            Instructions.TESTB(ib,reg_eax.low());
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
        public String description() {return "TEST "+CPU_Regs.reg_eax.getName8()+", "+ib;}
        public int getFlagType() {return FLAG_TYPE_TESTB;}
    }

    final static public class TestAxIw extends Op {
        int iw;
        public TestAxIw() {
            iw = decode_fetchw();
        }
        public int call() {
            Instructions.TESTW(iw,reg_eax.word());
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
        public String description() {return "TEST "+CPU_Regs.reg_eax.getName16()+", "+iw;}
        public int getFlagType() {return FLAG_TYPE_TESTW;}
    }

    final static public class MovIb extends Op {
        int ib;
        Reg reg;
        public MovIb(Reg reg) {
            ib = decode_fetchb();
            this.reg = reg;
        }
        public int call() {
            reg.set8(ib);
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
        public String description() {return "MOV "+reg.getName8()+", "+ib;}
    }

    final static public class MovIb_mem extends Op {
        int ib;
        EaaBase get_eaa;

        public MovIb_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib = decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, ib);
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
        public String description() {return "MOV "+get_eaa.description8()+", "+ib;}
    }

    final static public class MovIw extends Op {
        int ib;
        Reg reg;

        public MovIw(Reg reg) {
            ib = decode_fetchw();
            this.reg = reg;
        }
        public int call() {
            reg.word(ib);
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
        public String description() {return "MOV "+reg.getName16()+", "+ib;}
    }

    final static public class MovIw_mem extends Op {
        int ib;
        EaaBase get_eaa;

        public MovIw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            ib = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, ib);
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
        public String description() {return "MOV "+get_eaa.description16()+", "+ib;}
    }

    final static public class RetnIw extends Op {
        int offset;

        public RetnIw() {
            offset = decode_fetchw();
        }

        public int call() {
            reg_eip=CPU.CPU_Pop16();
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

    final static public class Retn extends Op {
        public int call() {
            reg_eip=CPU.CPU_Pop16();
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

    final static public class Les extends Op {
        EaaBase get_eaa;
        Reg rw;

        public Les(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int val = Memory.mem_readw(eaa); // make sure all reads are done before writing something in case of a PF
            if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(val);
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
        public String description() {return "LES "+rw.getName16()+", "+get_eaa.description16();}
    }

    final static public class Lds extends Op {
        EaaBase get_eaa;
        Reg rw;

        public Lds(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int val = Memory.mem_readw(eaa); // make sure all reads are done before writing something in case of a PF
            if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(val);
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
        public int setsSeg() {return DS|FROM_MEMORY;}
        public String description() {return "LDS "+rw.getName16()+", "+get_eaa.description16();}
    }

    final static public class EnterIwIb extends Op {
        int bytes;
        int level;

        public EnterIwIb() {
            bytes=decode_fetchw();
            level=decode_fetchb();
        }

        public int call() {
            CPU.CPU_ENTER(false,bytes,level);
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

    final static public class Leave extends Op {
        public int call() {
            reg_esp.dword&=CPU.cpu.stack.notmask;
            reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);
            reg_ebp.word(CPU.CPU_Pop16());
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

    final static public class RetfIw extends Op {
        int words;

        public RetfIw() {
            words = decode_fetchw();
        }
        public int call() {
            Flags.FillFlags();
            CPU.CPU_RET(false,words,reg_eip+eip_count);
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
        public boolean usesEip() {return false;}
        public boolean setsEip() {return true;}
        public String description() {return "RETF "+words;}
    }

    final static public class Retf extends Op {
        public int call() {
            Flags.FillFlags();
            CPU.CPU_RET(false,0,reg_eip+eip_count);
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
        public boolean usesEip() {return false;}
        public boolean setsEip() {return true;}
        public String description() {return "RETF";}
    }

    final static public class Int3 extends Op {
        public int call() {
            CPU.CPU_SW_Interrupt_NoIOPLCheck(3,reg_eip+eip_count);
            if (CPU_TRAP_CHECK)
                CPU.cpu.trap_skip=true;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return false;}
        public String description() {return "INT3";}
    }

    final static public class IntIb extends Op {
        int num;

        public IntIb() {
            num = decode_fetchb();
        }

        public int call() {
            CPU.CPU_SW_Interrupt(num,reg_eip+eip_count);
            if (CPU_TRAP_CHECK)
                CPU.cpu.trap_skip=true;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return false;}
        public String description() {return "INT "+num;}
    }

    final static public class Int0 extends Op {
        public int call() {
            if (Flags.get_OF()) {
                CPU.CPU_SW_Interrupt(4,reg_eip+eip_count);
                if (CPU_TRAP_CHECK)
                    CPU.cpu.trap_skip=true;
                return Constants.BR_Jump;
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return false;}
        public String description() {return "INT0";}
    }

    final static public class IRet extends Op {
        public int call() {
            CPU.CPU_IRET(false, reg_eip+eip_count);
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

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return false;}
        public String description() {return "IRET";}
    }

    final static public class AamIb extends Op {
        int ib;
        public AamIb() {
            ib=decode_fetchb();
        }
        public int call() {
            if (!Instructions.AAM(ib)) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return ib==0;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "AAM "+ib;}
    }

    final static public class AadIb extends Op {
        int ib;

        public AadIb() {
            ib=decode_fetchb();
        }

        public int call() {
            Instructions.AAD(ib);
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
        public String description() {return "AAD "+ib;}
    }

    final static public class Salc extends Op {
        public int call() {
            reg_eax.low(Flags.get_CF() ? 0xFF : 0);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SALC";}
    }

    final static public class Xlat32 extends Op {
        public int call() {
            reg_eax.low(Memory.mem_readb(Core.base_ds+reg_ebx.dword+reg_eax.low()));
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
        public String description() {return "XLAT";}
    }

    final static public class Xlat16 extends Op {
        public int call() {
            reg_eax.low(Memory.mem_readb(Core.base_ds+((reg_ebx.word()+reg_eax.low()) & 0xFFFF)));
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
        public String description() {return "XLAT";}
    }

    final static public class FPU0_normal extends Op {
        int rm;

        public FPU0_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC0_Normal(rm);
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
        public String description() {return "FPU0";}
    }

    final static public class FPU0_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU0_ea(int rm) {
            this.rm = rm;
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC0_EA(rm,get_eaa.call());
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
        public String description() {return "FPU0";}
    }

    final static public class FPU1_normal extends Op {
        int rm;

        public FPU1_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC1_Normal(rm);
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
        public String description() {return "FPU1";}
    }

    final static public class FPU1_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU1_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC1_EA(rm,get_eaa.call());
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
        public String description() {return "FPU1";}
    }

    final static public class FPU2_normal extends Op {
        int rm;

        public FPU2_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC2_Normal(rm);
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
        public String description() {return "FPU2";}
    }

    final static public class FPU2_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU2_ea(int rm) {
            this.rm = rm;
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC2_EA(rm,get_eaa.call());
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
        public String description() {return "FPU2";}
    }

    final static public class FPU3_normal extends Op {
        int rm;

        public FPU3_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC3_Normal(rm);
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
        public String description() {return "FPU3";}
    }

    final static public class FPU3_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU3_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC3_EA(rm,get_eaa.call());
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean returnsIllegal() {return true;}
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "FPU3";}
    }

    final static public class FPU4_normal extends Op {
        int rm;

        public FPU4_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC4_Normal(rm);
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
        public String description() {return "FPU4";}
    }

    final static public class FPU4_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU4_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC4_EA(rm,get_eaa.call());
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
        public String description() {return "FPU4";}
    }

    final static public class FPU5_normal extends Op {
        int rm;

        public FPU5_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC5_Normal(rm);
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
        public String description() {return "FPU5";}
    }

    final static public class FPU5_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU5_ea(int rm) {
            this.rm = rm;
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC5_EA(rm,get_eaa.call());
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
        public String description() {return "FPU5";}
    }

    final static public class FPU6_normal extends Op {
        int rm;

        public FPU6_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC6_Normal(rm);
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
        public String description() {return "FPU6";}
    }

    final static public class FPU6_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU6_ea(int rm) {
            this.rm = rm;
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC6_EA(rm,get_eaa.call());
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
        public String description() {return "FPU6";}
    }

    final static public class FPU7_normal extends Op {
        int rm;

        public FPU7_normal(int rm) {
            this.rm = rm;
        }

        public int call() {
            FPU.FPU_ESC7_Normal(rm);
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
        public String description() {return "FPU7";}
    }

    final static public class FPU7_ea extends Op {
        int rm;
        EaaBase get_eaa;

        public FPU7_ea(int rm) {
            this.rm = rm;
                get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            FPU.FPU_ESC7_EA(rm,get_eaa.call());
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
        public String description() {return "FPU7";}
    }

    final static public class Loopnz32 extends JumpCond16_b {
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

    final static public class Loopnz16 extends JumpCond16_b {
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

    final static public class Loopz32 extends JumpCond16_b {
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

    final static public class Loopz16 extends JumpCond16_b {
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

    final static public class Loop32 extends JumpCond16_b {
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

    final static public class Loop16 extends JumpCond16_b {
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

    final static public class Jcxz extends JumpCond16_b {
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

    final static public class InAlIb extends Op {
        int port;

        public InAlIb() {
            port=decode_fetchb();
        }

        public int call() {
            if (CPU.CPU_IO_Exception(port,1)) return RUNEXCEPTION();
            reg_eax.low(IO.IO_ReadB(port));
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
        public String description() {return "IN "+CPU_Regs.reg_eax.getName8()+" 0x"+Integer.toHexString(port);}
    }

    final static public class InAxIb extends Op {
        int port;

        public InAxIb() {
            port=decode_fetchb();
        }

        public int call() {
            if (CPU.CPU_IO_Exception(port,2)) return RUNEXCEPTION();
            reg_eax.word(IO.IO_ReadW(port));
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
        public String description() {return "IN "+CPU_Regs.reg_eax.getName16()+" 0x"+Integer.toHexString(port);}
    }

    final static public class OutAlIb extends Op {
        int port;

        public OutAlIb() {
            port=decode_fetchb();
        }

        public int call() {
            if (CPU.CPU_IO_Exception(port,1)) return RUNEXCEPTION();
            IO.IO_WriteB(port,reg_eax.low());
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
        public String description() {return "OUT "+CPU_Regs.reg_eax.getName8()+" 0x"+Integer.toHexString(port);}
    }

    final static public class OutAxIb extends Op {
        int port;

        public OutAxIb() {
            port=decode_fetchb();
        }

        public int call() {
            if (CPU.CPU_IO_Exception(port,2)) return RUNEXCEPTION();
            IO.IO_WriteW(port,reg_eax.word());
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
        public String description() {return "IN "+CPU_Regs.reg_eax.getName16()+" 0x"+Integer.toHexString(port);}
    }

    final static public class CallJw extends Op {
        int addip;

        public CallJw() {
            addip=decode_fetchws();
        }

        public int call() {
            CPU.CPU_Push16(reg_eip+eip_count);
            reg_ip(reg_eip+eip_count+addip);
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

    final static public class JmpJw extends Op {
        int addip;

        public JmpJw() {
            addip=decode_fetchws();
        }

        public int call() {
            reg_ip(reg_eip+addip+eip_count);
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

    final static public class JmpAp extends Op {
        int newip;
        int newcs;

        public JmpAp() {
            newip=decode_fetchw();
            newcs=decode_fetchw();
        }

        public int call() {
            Flags.FillFlags();
            CPU.CPU_JMP(false,newcs,newip,reg_eip+eip_count);
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
            reg_ip(reg_eip+addip+eip_count);
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

    final static public class InAlDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
            reg_eax.low(IO.IO_ReadB(reg_edx.word()));
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
        public String description() {return "IN "+CPU_Regs.reg_eax.getName8()+", "+CPU_Regs.reg_edx.getName16();}
    }

    final static public class InAxDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
            reg_eax.word(IO.IO_ReadW(reg_edx.word()));
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
        public String description() {return "IN "+CPU_Regs.reg_eax.getName16()+", "+CPU_Regs.reg_edx.getName16();}
    }

    final static public class OutAlDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
            IO.IO_WriteB(reg_edx.word(),reg_eax.low());
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
        public String description() {return "OUT "+CPU_Regs.reg_eax.getName8()+", "+CPU_Regs.reg_edx.getName16();}
    }

    final static public class OutAxDx extends Op {
        public int call() {
            if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
            IO.IO_WriteW(reg_edx.word(),reg_eax.word());
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
        public String description() {return "OUT "+CPU_Regs.reg_eax.getName16()+", "+CPU_Regs.reg_edx.getName16();}
    }

    final static public class Icebp extends Op {
        public int call() {
            CPU.CPU_SW_Interrupt_NoIOPLCheck(1,reg_eip+eip_count);
            if (CPU_TRAP_CHECK)
                CPU.cpu.trap_skip=true;
            return Constants.BR_Jump;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return false;}
        public String description() {return "ICEBP";}
    }

    final static public class Hlt extends Op {
        public int call() {
             if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            Flags.FillFlags();
            CPU.CPU_HLT(reg_eip+eip_count);
            return CB_NONE();
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF; // :TODO: is this FillFlags necessary
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return false;}
        public String description() {return "HLT";}
    }

    final static public class Cmc extends Op {
        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(CPU_Regs.flags & CPU_Regs.CF)==0);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CMC";}
    }

    final static public class Clc extends Op {
        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,false);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "CLC";}
    }

    final static public class Stc extends Op {
        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,true);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "STC";}
    }

    final static public class Cli extends Op {
        public int call() {
            if (CPU.CPU_CLI()) return RUNEXCEPTION();
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
        public String description() {return "CLI";}
    }

    final static public class Sti extends Op {
        public int call() {
            if (CPU.CPU_STI()) return RUNEXCEPTION();
            if (CPU_PIC_CHECK)
                if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END(eip_count);
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
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "STI";}
    }

    final static public class Cld extends Op {
        public int call() {
            SETFLAGBIT(DF,false);
            CPU.cpu.direction=1;
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
        public String description() {return "CLD";}
    }

    final static public class Std extends Op {
        public int call() {
            SETFLAGBIT(DF,true);
            CPU.cpu.direction=-1;
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
        public String description() {return "STD";}
    }

    final static public class Incb_reg extends Op {
        Reg reg;

        public Incb_reg(int rm) {
            reg = Mod.eb(rm);
        }

        public int call() {
            reg.set8(Instructions.INCB(reg.get8()));
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
        public String description() {return "INC "+reg.getName8();}
        public int getFlagType() {return FLAG_TYPE_INCB;}
    }

    final static public class Incb_mem extends Op {
        EaaBase get_eaa;

        public Incb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));
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
        public String description() {return "INC "+get_eaa.description8();}
        public int getFlagType() {return FLAG_TYPE_INCB;}
    }

    final static public class Decb_reg extends Op {
        Reg reg;

        public Decb_reg(int rm) {
            reg = Mod.eb(rm);
        }

        public int call() {
            reg.set8(Instructions.DECB(reg.get8()));
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
        public String description() {return "DEC "+reg.getName8();}
        public int getFlagType() {return FLAG_TYPE_DECB;}
    }

    final static public class Decb_mem extends Op {
        EaaBase get_eaa;

        public Decb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));
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
        public String description() {return "DEC "+get_eaa.description8();}
        public int getFlagType() {return FLAG_TYPE_DECB;}
    }

    final static public class Callback extends Op {
        int val;

        public Callback(int val) {
            this.val = val;
        }

        public int call() {
            reg_eip+=eip_count;
            Data.callback = val;
            return Constants.BR_CallBack;
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
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
        public String description() {return "Callback "+val;}
    }

    final static public class Incw_reg extends Op {
        Reg reg;

        public Incw_reg(int rm) {
            reg = Mod.ew(rm);
        }

        public int call() {
            reg.word(Instructions.INCW(reg.word()));
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
        public String description() {return "INC "+reg.getName16();}
        public int getFlagType() {return FLAG_TYPE_INCW;}
    }

    final static public class Incw_mem extends Op {
        EaaBase get_eaa;

        public Incw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));
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
        public String description() {return "INC "+get_eaa.description16();}
        public int getFlagType() {return FLAG_TYPE_INCW;}
    }

    final static public class Decw_reg extends Op {
        Reg reg;

        public Decw_reg(int rm) {
            reg = Mod.ew(rm);
        }

        public int call() {
            reg.word(Instructions.DECW(reg.word()));
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
        public String description() {return "DEC "+reg.getName16();}
        public int getFlagType() {return FLAG_TYPE_DECW;}
    }

    final static public class Decw_mem extends Op {
        EaaBase get_eaa;

        public Decw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));
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
        public String description() {return "DEC "+get_eaa.description16();}
        public int getFlagType() {return FLAG_TYPE_DECW;}
    }

    final static public class CallEv_reg extends Op {
        Reg earw;

        public CallEv_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int old = reg_eip+eip_count;
            CPU.CPU_Push16(old & 0xFFFF);
            reg_eip=earw.word();
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
        public String description() {return "CALL "+earw.getName16();}
    }

    final static public class CallEv_mem extends Op {
        EaaBase get_eaa;
        public CallEv_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int old = reg_eip+eip_count;
            int eip = Memory.mem_readw(eaa);
            CPU.CPU_Push16(old & 0xFFFF);
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
        public String description() {return "CALL "+get_eaa.description16();}
    }

    final static public class CallEp extends Op {
        EaaBase get_eaa;
        public CallEp(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            int newip=Memory.mem_readw(eaa);
            int newcs=Memory.mem_readw(eaa+2);
            Flags.FillFlags();
            CPU.CPU_CALL(false,newcs,newip,(reg_eip+eip_count) & 0xFFFF);
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
        public String description() {return "CALL "+get_eaa.description16();}
    }

    final static public class JmpEv_reg extends Op {
        Reg earw;

        public JmpEv_reg(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            reg_eip=earw.word();
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
        public String description() {return "JMP "+earw.getName16();}
    }

    final static public class JmpEv_mem extends Op {
        EaaBase get_eaa;

        public JmpEv_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            reg_eip=Memory.mem_readw(eaa);
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
        public String description() {return "JMP "+get_eaa.description16();}
    }

    final static public class JmpEp extends Op {
        EaaBase get_eaa;

        public JmpEp(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            int newip=Memory.mem_readw(eaa);
            int newcs=Memory.mem_readw(eaa+2);
            Flags.FillFlags();
            CPU.CPU_JMP(false,newcs,newip,(reg_eip+eip_count) & 0xFFFF);
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
        public String description() {return "JMP "+get_eaa.description16();}
    }

    final static public class PushEv_reg extends Op {
        Reg earw;

        public PushEv_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            CPU.CPU_Push16(earw.word());
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
        public String description() {return "PUSH "+earw.getName16();}
    }

    final static public class PushEv_mem extends Op {
        EaaBase get_eaa;

        public PushEv_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            CPU.CPU_Push16(Memory.mem_readw(eaa));
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
        public String description() {return "PUSH "+get_eaa.description16();}
    }

    final static public class Cbw extends Op {
        public int call() {
            reg_eax.word((byte)reg_eax.low());
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
        public String description() {return "CBW";}
    }

    final static public class Cwd extends Op {
        public int call() {
            if ((reg_eax.word() & 0x8000)!=0) reg_edx.word(0xffff);else reg_edx.word(0);
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
        public String description() {return "CWD";}
    }

    final static public class Wait extends Op {
        public int call() {
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
        public String description() {return "WAIT";}
    }
}
