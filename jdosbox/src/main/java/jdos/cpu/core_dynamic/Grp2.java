package jdos.cpu.core_dynamic;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Instructions;
import jdos.hardware.Memory;

public class Grp2 extends Helper {
    static public class ROLB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public ROLB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_ROLB(earb.get8(), val))
                earb.set8(Instructions.do_ROLB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+earb.getName8()+", "+val;}
    }

    static public class RORB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public RORB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_RORB(earb.get8(), val))
                earb.set8(Instructions.do_RORB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+earb.getName8()+", "+val;}
    }

    static public class RCLB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public RCLB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_RCLB(val))
                earb.set8(Instructions.do_RCLB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+earb.getName8()+", "+val;}
    }

    static public class RCRB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public RCRB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_RCRB(val))
                earb.set8(Instructions.do_RCRB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+earb.getName8()+", "+val;}
    }

    static public class SHLB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public SHLB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_SHLB(val))
                earb.set8(Instructions.do_SHLB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+earb.getName8()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHLB;}
    }

    static public class SHRB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public SHRB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_SHRB(val))
                earb.set8(Instructions.do_SHRB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+earb.getName8()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHRB;}
    }

    static public class SARB_reg extends Op {
        int val;
        CPU_Regs.Reg earb;

        public SARB_reg(int rm, int val) {
            this.val=val;
            earb = Mod.eb(rm);

        }

        public int call() {
            if (Instructions.valid_SARB(val))
                earb.set8(Instructions.do_SARB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+earb.getName8()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SARB;}
    }

    static public class ROLB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public ROLB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_ROLB(eaa, val)) {
                Memory.mem_writeb(eaa, Instructions.do_ROLB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+get_eaa.description8()+", "+val;}
    }

    static public class RORB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RORB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_RORB(eaa, val)) {
                Memory.mem_writeb(eaa, Instructions.do_RORB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+get_eaa.description8()+", "+val;}
    }

    static public class RCLB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCLB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_RCLB(val)) {
                Memory.mem_writeb(eaa, Instructions.do_RCLB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+get_eaa.description8()+", "+val;}
    }

    static public class RCRB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCRB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_RCRB(val)) {
                Memory.mem_writeb(eaa, Instructions.do_RCRB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+get_eaa.description8()+", "+val;}
    }

    static public class SHLB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHLB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }

        public int call() {
            if (Instructions.valid_SHLB(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writeb(eaa, Instructions.do_SHLB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+get_eaa.description8()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHLB;}
    }

    static public class SHRB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHRB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            if (Instructions.valid_SHRB(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writeb(eaa, Instructions.do_SHRB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+get_eaa.description8()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHRB;}
    }

    static public class SARB_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SARB_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            if (Instructions.valid_SARB(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writeb(eaa, Instructions.do_SARB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+get_eaa.description8()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SARB;}
    }

    static public class ROLW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public ROLW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_ROLW(earw.word(), val))
                earw.word(Instructions.do_ROLW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+earw.getName16()+", "+val;}
    }

    static public class RORW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public RORW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_RORW(earw.word(), val))
                earw.word(Instructions.do_RORW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+earw.getName16()+", "+val;}
    }

    static public class RCLW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public RCLW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_RCLW(val))
                earw.word(Instructions.do_RCLW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+earw.getName16()+", "+val;}
    }

    static public class RCRW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public RCRW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_RCRW(val))
                earw.word(Instructions.do_RCRW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+earw.getName16()+", "+val;}
    }

    static public class SHLW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public SHLW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_SHLW(val))
                earw.word(Instructions.do_SHLW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+earw.getName16()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHLW;}
    }

    static public class SHRW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public SHRW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_SHRW(val))
                earw.word(Instructions.do_SHRW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+earw.getName16()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHRW;}
    }

    static public class SARW_reg extends Op {
        int val;
        CPU_Regs.Reg earw;

        public SARW_reg(int rm, int val) {
            this.val = val;
            earw = Mod.ew(rm);

        }

        public int call() {
            if (Instructions.valid_SARW(val))
                earw.word(Instructions.do_SARW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+earw.getName16()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SARW;}
    }

    static public class ROLW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public ROLW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_ROLW(eaa, val)) {
                Memory.mem_writew(eaa, Instructions.do_ROLW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+get_eaa.description16()+", "+val;}
    }

    static public class RORW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RORW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_RORW(eaa, val)) {
                Memory.mem_writew(eaa, Instructions.do_RORW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+get_eaa.description16()+", "+val;}
    }

    static public class RCLW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCLW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            if (Instructions.valid_RCLW(val)) {
                Memory.mem_writew(eaa, Instructions.do_RCLW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+get_eaa.description16()+", "+val;}
    }

    static public class RCRW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCRW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            if (Instructions.valid_RCRW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_RCRW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+get_eaa.description16()+", "+val;}
    }

    static public class SHLW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHLW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            if (Instructions.valid_SHLW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_SHLW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+get_eaa.description16()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHLW;}
    }

    static public class SHRW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHRW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            if (Instructions.valid_SHRW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_SHRW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+get_eaa.description16()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHRW;}
    }

    static public class SARW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SARW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            if (Instructions.valid_SARW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_SARW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+get_eaa.description16()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SARW;}
    }

    static public class ROLB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public ROLB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_ROLB(earb.get8(), val))
                earb.set8(Instructions.do_ROLB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RORB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public RORB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RORB(earb.get8(), val))
                earb.set8(Instructions.do_RORB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCLB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public RCLB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCLB(val))
                earb.set8(Instructions.do_RCLB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCRB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public RCRB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCRB(val))
                earb.set8(Instructions.do_RCRB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class SHLB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public SHLB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHLB(val))
                earb.set8(Instructions.do_SHLB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHLB;}
    }

    static public class SHRB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public SHRB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHRB(val))
                earb.set8(Instructions.do_SHRB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHRB;}
    }

    static public class SARB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public SARB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SARB(val))
                earb.set8(Instructions.do_SARB(val, earb.get8()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+earb.getName8()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SARB;}
    }

    static public class ROLB_mem_cl extends Op {
        EaaBase get_eaa;

        public ROLB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_ROLB(eaa, val)) {
                Memory.mem_writeb(eaa, Instructions.do_ROLB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RORB_mem_cl extends Op {
        EaaBase get_eaa;

        public RORB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RORB(eaa, val)) {
                Memory.mem_writeb(eaa, Instructions.do_RORB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCLB_mem_cl extends Op {
        EaaBase get_eaa;

        public RCLB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCLB(val)) {
                Memory.mem_writeb(eaa, Instructions.do_RCLB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCRB_mem_cl extends Op {
        EaaBase get_eaa;

        public RCRB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCRB(val)) {
                Memory.mem_writeb(eaa, Instructions.do_RCRB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class SHLB_mem_cl extends Op {
        EaaBase get_eaa;

        public SHLB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHLB(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writeb(eaa, Instructions.do_SHLB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHLB;}
    }

    static public class SHRB_mem_cl extends Op {
        EaaBase get_eaa;

        public SHRB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHRB(reg_ecx.low())) {
                int eaa = get_eaa.call();
                Memory.mem_writeb(eaa, Instructions.do_SHRB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHRB;}
    }

    static public class SARB_mem_cl extends Op {
        EaaBase get_eaa;

        public SARB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SARB(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writeb(eaa, Instructions.do_SARB(val, Memory.mem_readb(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+get_eaa.description8()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SARB;}
    }

    static public class ROLW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public ROLW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_ROLW(earw.word(), val))
                earw.word(Instructions.do_ROLW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RORW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public RORW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RORW(earw.word(), val))
                earw.word(Instructions.do_RORW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCLW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public RCLW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCLW(val))
                earw.word(Instructions.do_RCLW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCRW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public RCRW_reg_cl(int rm) {
            earw = Mod.ew(rm);

        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCRW(val))
                earw.word(Instructions.do_RCRW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class SHLW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public SHLW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHLW(val))
                earw.word(Instructions.do_SHLW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHLW;}
    }

    static public class SHRW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public SHRW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHRW(val))
                earw.word(Instructions.do_SHRW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHRW;}
    }

    static public class SARW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public SARW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SARW(val))
                earw.word(Instructions.do_SARW(val, earw.word()));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+earw.getName16()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SARW;}
    }

    static public class ROLW_mem_cl extends Op {
        EaaBase get_eaa;

        public ROLW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_ROLW(eaa, val)) {
                Memory.mem_writew(eaa, Instructions.do_ROLW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RORW_mem_cl extends Op {
        EaaBase get_eaa;

        public RORW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RORW(eaa, val)) {
                Memory.mem_writew(eaa, Instructions.do_RORW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCLW_mem_cl extends Op {
        EaaBase get_eaa;

        public RCLW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int eaa = get_eaa.call();
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCLW(val)) {
                Memory.mem_writew(eaa, Instructions.do_RCLW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCRW_mem_cl extends Op {
        EaaBase get_eaa;

        public RCRW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_RCRW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_RCRW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class SHLW_mem_cl extends Op {
        EaaBase get_eaa;

        public SHLW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHLW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_SHLW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHLW;}
    }

    static public class SHRW_mem_cl extends Op {
        EaaBase get_eaa;

        public SHRW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SHRW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_SHRW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHRW;}
    }

    static public class SARW_mem_cl extends Op {
        EaaBase get_eaa;

        public SARW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (Instructions.valid_SARW(val)) {
                int eaa = get_eaa.call();
                Memory.mem_writew(eaa, Instructions.do_SARW(val, Memory.mem_readw(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+get_eaa.description16()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SARW;}
    }

    static public class ROLD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public ROLD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }

        public int call() {
            eard.dword=Instructions.ROLD(val, eard.dword);
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
        public String description() {return "ROL "+eard.getName()+", "+val;}
    }

    static public class RORD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public RORD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }

        public int call() {
            eard.dword=Instructions.RORD(val, eard.dword);
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
        public String description() {return "ROR "+eard.getName()+", "+val;}
    }

    static public class RCLD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public RCLD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }

        public int call() {
            eard.dword=Instructions.RCLD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+eard.getName()+", "+val;}
    }

    static public class RCRD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public RCRD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }

        public int call() {
            eard.dword=Instructions.RCRD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+eard.getName()+", "+val;}
    }

    static public class SHLD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public SHLD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=Instructions.SHLD(val, eard.dword);
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
        public String description() {return "SHL "+eard.getName()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHLD;}
    }

    static public class SHRD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public SHRD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=Instructions.SHRD(val, eard.dword);
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
        public String description() {return "SHR "+eard.getName()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHRD;}
    }

    static public class SARD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public SARD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=Instructions.SARD(val, eard.dword);
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
        public String description() {return "SAR "+eard.getName()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SARD;}
    }

    static public class ROLD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public ROLD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.ROLD(val, Memory.mem_readd(eaa)));
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
        public String description() {return "ROL "+get_eaa.description32()+", "+val;}
    }

    static public class RORD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RORD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.RORD(val, Memory.mem_readd(eaa)));
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
        public String description() {return "ROR "+get_eaa.description32()+", "+val;}
    }

    static public class RCLD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCLD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.RCLD(val, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+get_eaa.description32()+", "+val;}
    }

    static public class RCRD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCRD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.RCRD(val, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+get_eaa.description32()+", "+val;}
    }

    static public class SHLD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHLD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.SHLD(val, Memory.mem_readd(eaa)));
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
        public String description() {return "SHL "+get_eaa.description32()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHLD;}
    }

    static public class SHRD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHRD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.SHRD(val, Memory.mem_readd(eaa)));
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
        public String description() {return "SHR "+get_eaa.description32()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SHRD;}
    }

    static public class SARD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SARD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.SARD(val, Memory.mem_readd(eaa)));
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
        public String description() {return "SAR "+get_eaa.description32()+", "+val;}
        public int getFlagType() {return FLAG_TYPE_SARD;}
    }

    static public class ROLD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public ROLD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val!=0)
                eard.dword=Instructions.ROLD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RORD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public RORD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0)
                eard.dword=Instructions.RORD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCLD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public RCLD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0)
                eard.dword=Instructions.RCLD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCRD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public RCRD_reg_cl(int rm) {
            eard = Mod.ed(rm);

        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0)
                eard.dword=Instructions.RCRD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class SHLD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public SHLD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0)
                eard.dword=Instructions.SHLD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHLD;}
    }

    static public class SHRD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public SHRD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0)
                eard.dword=Instructions.SHRD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHRD;}
    }

    static public class SARD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public SARD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0)
                eard.dword=Instructions.SARD(val, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+eard.getName()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SARD;}
    }

    static public class ROLD_mem_cl extends Op {
        EaaBase get_eaa;

        public ROLD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.ROLD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROL "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RORD_mem_cl extends Op {
        EaaBase get_eaa;

        public RORD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.RORD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "ROR "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCLD_mem_cl extends Op {
        EaaBase get_eaa;

        public RCLD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.RCLD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCL "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class RCRD_mem_cl extends Op {
        EaaBase get_eaa;

        public RCRD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.RCRD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return CPU_Regs.CF;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "RCR "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
    }

    static public class SHLD_mem_cl extends Op {
        EaaBase get_eaa;

        public SHLD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.SHLD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHL "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHLD;}
    }

    static public class SHRD_mem_cl extends Op {
        EaaBase get_eaa;

        public SHRD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.SHRD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SHR "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SHRD;}
    }

    static public class SARD_mem_cl extends Op {
        EaaBase get_eaa;

        public SARD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }

        public int call() {
            int val = reg_ecx.low() & 0x1f;
            if (val != 0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.SARD(val, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF  | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF | CPU_Regs.MAYBE;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public String description() {return "SAR "+get_eaa.description32()+", "+CPU_Regs.reg_ecx.getName8();}
        public int getFlagType() {return FLAG_TYPE_SARD;}
    }
}
