package jdos.cpu.core_dynamic;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Flags;
import jdos.cpu.Instructions;
import jdos.hardware.Memory;

public class Grp3 extends Helper {
    static public class Testb_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public Testb_reg(int rm) {
            this.val=decode_fetchb();
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.TESTB(val,earb.get8());
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
        public String description() {return "TEST "+earb.getName8()+", 0x"+Integer.toHexString(val);}
        public int getFlagType() {return FLAG_TYPE_TESTB;}
    }

    static public class Testb_mem extends Op {
        int val;
        EaaBase get_eaa;

        public Testb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val=decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTB(val,Memory.mem_readb(eaa));
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
        public String description() {return "TEST "+get_eaa.description8()+", 0x"+Integer.toHexString(val);}
        public int getFlagType() {return FLAG_TYPE_TESTB;}
    }

    static public class NotEb_reg extends Op {
        CPU_Regs.Reg earb;

        public NotEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            earb.set8((byte)~earb.get8());
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
        public String description() {return "NOT "+earb.getName8();}
    }

    static public class NotEb_mem extends Op {
        EaaBase get_eaa;

        public NotEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa,~Memory.mem_readb(eaa));
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
        public String description() {return "NOT "+get_eaa.description8();}
    }

    static public class NegEb_reg extends Op {
        CPU_Regs.Reg earb;

        public NegEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            Flags.type=Flags.t_NEGb;
            Flags.lf_var1b(earb.get8());
            Flags.lf_resb(0-Flags.lf_var1b());
            earb.set8(Flags.lf_resb());
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
        public String description() {return "NEG "+earb.getName8();}
        public int getFlagType() {return FLAG_TYPE_NEGB;}
    }

    static public class NegEb_mem extends Op {
        EaaBase get_eaa;

        public NegEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            Flags.type=Flags.t_NEGb;
            int eaa = get_eaa.call();
            Flags.lf_var1b(Memory.mem_readb(eaa));
            Flags.lf_resb(0-Flags.lf_var1b());
            Memory.mem_writeb(eaa,Flags.lf_resb());
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
        public String description() {return "NEG "+get_eaa.description8();}
        public int getFlagType() {return FLAG_TYPE_NEGB;}
    }

    static public class MulAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public MulAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.MULB(earb.get8());
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
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName8()+", "+earb.getName8();}
    }

    static public class MulAlEb_mem extends Op {
        EaaBase get_eaa;

        public MulAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.MULB(Memory.mem_readb(eaa));
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
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName8()+", "+get_eaa.description8();}
    }

    static public class IMulAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public IMulAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.IMULB(earb.get8());
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
        public String description() {return "IMUL "+CPU_Regs.reg_eax.getName8()+", "+earb.getName8();}
    }

    static public class IMulAlEb_mem extends Op {
        EaaBase get_eaa;

        public IMulAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.IMULB(Memory.mem_readb(eaa));
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
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName8()+", "+get_eaa.description8();}
    }

    static public class DivAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public DivAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            if (!Instructions.DIVB(earb.get8())) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "DIV "+CPU_Regs.reg_eax.getName8()+", "+earb.getName8();}
    }

    static public class DivAlEb_mem extends Op {
        EaaBase get_eaa;

        public DivAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (!Instructions.DIVB(Memory.mem_readb(eaa))) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "DIV "+CPU_Regs.reg_eax.getName8()+", "+get_eaa.description8();}
    }

    static public class IDivAlEb_reg extends Op {
        CPU_Regs.Reg earb;

        public IDivAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            if (!Instructions.IDIVB(earb.get8())) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "IDIV "+CPU_Regs.reg_eax.getName8()+", "+earb.getName8();}
    }

    static public class IDivAlEb_mem extends Op {
        EaaBase get_eaa;

        public IDivAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (!Instructions.IDIVB(Memory.mem_readb(eaa))) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "IDIV "+CPU_Regs.reg_eax.getName8()+", "+get_eaa.description8();}
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
        public String description() {return "TEST "+earw.getName16()+", 0x"+Integer.toHexString(val);}
        public int getFlagType() {return FLAG_TYPE_TESTW;}
    }

    static public class Testw_mem extends Op {
        int val;
        EaaBase get_eaa;

        public Testw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val=decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTW(val,Memory.mem_readw(eaa));
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
        public String description() {return "TEST "+get_eaa.description32()+", 0x"+Integer.toHexString(val);}
        public int getFlagType() {return FLAG_TYPE_TESTW;}
    }

    static public class NotEw_reg extends Op {
        CPU_Regs.Reg earw;

        public NotEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(~earw.word());
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
        public String description() {return "NOT "+earw.getName16();}
    }

    static public class NotEw_mem extends Op {
        EaaBase get_eaa;

        public NotEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa,~Memory.mem_readw(eaa));
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
        public String description() {return "NOT "+get_eaa.description16();}
    }

    static public class NegEw_reg extends Op {
        CPU_Regs.Reg earw;

        public NegEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            Flags.type=Flags.t_NEGw;
            Flags.lf_var1w(earw.word());
            Flags.lf_resw(0-Flags.lf_var1w());
            earw.word(Flags.lf_resw());
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
        public String description() {return "NEG "+earw.getName16();}
        public int getFlagType() {return FLAG_TYPE_NEGW;}
    }

    static public class NegEw_mem extends Op {
        EaaBase get_eaa;

        public NegEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            Flags.type=Flags.t_NEGw;
            int eaa = get_eaa.call();
            Flags.lf_var1w(Memory.mem_readw(eaa));
            Flags.lf_resw(0-Flags.lf_var1w());
            Memory.mem_writew(eaa,Flags.lf_resw());
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
        public String description() {return "NEG "+get_eaa.description16();}
        public int getFlagType() {return FLAG_TYPE_NEGW;}
    }

    static public class MulAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public MulAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.MULW(earw.word());
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
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName16()+", "+earw.getName16();}
    }

    static public class MulAxEw_mem extends Op {
        EaaBase get_eaa;

        public MulAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.MULW(Memory.mem_readw(eaa));
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
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName16()+", "+get_eaa.description16();}
    }

    static public class IMulAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public IMulAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.IMULW(earw.word());
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
        public String description() {return "IMUL "+CPU_Regs.reg_eax.getName16()+", "+earw.getName16();}
    }

    static public class IMulAxEw_mem extends Op {
        EaaBase get_eaa;

        public IMulAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.IMULW(Memory.mem_readw(eaa));
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
        public String description() {return "IMUL "+CPU_Regs.reg_eax.getName16()+", "+get_eaa.description16();}
    }

    static public class DivAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public DivAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (!Instructions.DIVW(earw.word())) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "DIV "+CPU_Regs.reg_eax.getName16()+", "+earw.getName16();}
    }

    static public class DivAxEw_mem extends Op {
        EaaBase get_eaa;

        public DivAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (!Instructions.DIVW(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "DIV "+CPU_Regs.reg_eax.getName16()+", "+get_eaa.description16();}
    }

    static public class IDivAxEw_reg extends Op {
        CPU_Regs.Reg earw;

        public IDivAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (!Instructions.IDIVW(earw.word())) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "IDIV "+CPU_Regs.reg_eax.getName16()+", "+earw.getName16();}
    }

    static public class IDivAxEw_mem extends Op {
        EaaBase get_eaa;

        public IDivAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (!Instructions.IDIVW(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "IDIV "+CPU_Regs.reg_eax.getName16()+", "+get_eaa.description16();}
    }

    static public class Testd_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public Testd_reg(int rm) {
            val= decode_fetchd();
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.TESTD(val,eard.dword);
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
        public String description() {return "TEST "+eard.getName()+", 0x"+Integer.toHexString(val);}
        public int getFlagType() {return FLAG_TYPE_TESTD;}
    }

    static public class Testd_mem extends Op {
        int val;
        EaaBase get_eaa;

        public Testd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val= decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTD(val,Memory.mem_readd(eaa));
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
        public String description() {return "TEST "+get_eaa.description32()+", 0x"+Integer.toHexString(val);}
        public int getFlagType() {return FLAG_TYPE_TESTD;}
    }

    static public class NotEd_reg extends Op {
        CPU_Regs.Reg eard;

        public NotEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=~eard.dword;
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
        public String description() {return "NOT "+eard.getName();}
    }

    static public class NotEd_mem extends Op {
        EaaBase get_eaa;

        public NotEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa,~Memory.mem_readd(eaa));
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
        public String description() {return "NOT "+get_eaa.description32();}
    }

    static public class NegEd_reg extends Op {
        CPU_Regs.Reg eard;

        public NegEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword = Instructions.Negd(eard.dword);
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
        public String description() {return "NEG "+eard.getName();}
        public int getFlagType() {return FLAG_TYPE_NEGD;}
    }

    static public class NegEd_mem extends Op {
        EaaBase get_eaa;

        public NegEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa,Instructions.Negd(Memory.mem_readd(eaa)));
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
        public String description() {return "NEG "+get_eaa.description32();}
        public int getFlagType() {return FLAG_TYPE_NEGD;}
    }

    static public class MulAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public MulAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.MULD(eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+eard.getName();}
    }

    static public class MulAxEd_mem extends Op {
        EaaBase get_eaa;

        public MulAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.MULD(Memory.mem_readd(eaa));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+get_eaa.description32();}
    }

    static public class IMulAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public IMulAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.IMULD(eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "IMUL "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+eard.getName();}
    }

    static public class IMulAxEd_mem extends Op {
        EaaBase get_eaa;

        public IMulAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.IMULD(Memory.mem_readd(eaa));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "MUL "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+get_eaa.description32();}
    }

    static public class DivAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public DivAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            if (!Instructions.DIVD(eard.dword)) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "DIV "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+eard.getName();}
    }

    static public class DivAxEd_mem extends Op {
        EaaBase get_eaa;

        public DivAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (!Instructions.DIVD(Memory.mem_readd(eaa))) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "DIV "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+get_eaa.description32();}
    }

    static public class IDivAxEd_reg extends Op {
        CPU_Regs.Reg eard;

        public IDivAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            if (!Instructions.IDIVD(eard.dword)) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "IDIV "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+eard.getName();}
    }

    static public class IDivAxEd_mem extends Op {
        EaaBase get_eaa;

        public IDivAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            if (!Instructions.IDIVD(Memory.mem_readd(eaa))) return RUNEXCEPTION();
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // Flags are undefined
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
        public String description() {return "IDIV "+CPU_Regs.reg_eax.getName()+" "+CPU_Regs.reg_edx.getName()+", "+get_eaa.description32();}
    }
}
