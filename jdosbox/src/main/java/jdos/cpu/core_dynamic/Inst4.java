package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;

public class Inst4 extends Helper {
    final static public class Lgdt_mem extends Op {
        EaaBase get_eaa;

        public Lgdt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            int v1 = Memory.mem_readd(eaa + 2);
            int v0 = Memory.mem_readw(eaa);
            CPU.CPU_LGDT(v0,v1);
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
        public String description() {return "LGDT "+get_eaa.description32();}
    }

    final static public class Lidt_mem extends Op {
        EaaBase get_eaa;

        public Lidt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            int v1 = Memory.mem_readd(eaa + 2);
            int v0 = Memory.mem_readw(eaa);
            CPU.CPU_LIDT(v0,v1);
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
        public String description() {return "LIDT "+get_eaa.description32();}
    }

    final static public class Smsw_reg extends Op {
        Reg eard;

        public Smsw_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU.CPU_SMSW();
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
        public String description() {return "SMSW "+eard.getName();}
    }

    final static public class Lmsw_reg extends Op {
        Reg eard;

        public Lmsw_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            if (CPU.CPU_LMSW(eard.dword)) return RUNEXCEPTION();
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
        public String description() {return "LMSW "+eard.getName();}
    }

    final static public class LarGdEd_reg extends Op {
        Reg earw;
        Reg rd;

        public LarGdEd_reg(int rm) {
            earw = Mod.ew(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            rd.dword = CPU.CPU_LAR(earw.word(),rd.dword);
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
        public String description() {return "LAR "+rd.getName() + ", "+earw.getName16();}
    }

    final static public class LarGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public LarGdEd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            rd.dword = CPU.CPU_LAR(Memory.mem_readw(eaa),rd.dword);
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
        public String description() {return "LAR "+rd.getName() + ", "+get_eaa.description16();}
    }

    final static public class LslGdEd_reg extends Op {
        Reg earw;
        Reg rd;

        public LslGdEd_reg(int rm) {
            earw = Mod.ew(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            rd.dword = CPU.CPU_LSL(earw.word(),rd.dword);
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
        public String description() {return "LSL "+rd.getName() + ", "+earw.getName16();}
    }

    final static public class LslGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public LslGdEd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            rd.dword = CPU.CPU_LSL(Memory.mem_readw(eaa),rd.dword);
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
        public String description() {return "LAR "+rd.getName() + ", "+get_eaa.description16();}
    }

static abstract public class JumpCond32_d extends Op {
        int offset;

        public JumpCond32_d() {
            offset = decode_fetchds();
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

    final static public class JumpCond32_d_o extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_O());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
        public String description() {return "JO "+super.description();}
    }

    final static public class JumpCond32_d_no extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NO());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
        public String description() {return "JNO "+super.description();}
    }

    final static public class JumpCond32_d_b extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_B());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
        public String description() {return "JB "+super.description();}
    }

    final static public class JumpCond32_d_nb extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NB());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
        public String description() {return "JNB "+super.description();}
    }

    final static public class JumpCond32_d_z extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_Z());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "JZ "+super.description();}
    }

    final static public class JumpCond32_d_nz extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NZ());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
        public String description() {return "JNZ "+super.description();}
    }

    final static public class JumpCond32_d_be extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_BE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
        public String description() {return "JBE "+super.description();}
    }

    final static public class JumpCond32_d_nbe extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NBE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
        public String description() {return "JNBE "+super.description();}
    }

    final static public class JumpCond32_d_s extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_S());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
        public String description() {return "JS "+super.description();}
    }

    final static public class JumpCond32_d_ns extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NS());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
        public String description() {return "JNS "+super.description();}
    }

    final static public class JumpCond32_d_p extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_P());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
        public String description() {return "JP "+super.description();}
    }

    final static public class JumpCond32_d_np extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NP());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
        public String description() {return "JNP "+super.description();}
    }

    final static public class JumpCond32_d_l extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_L());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
        public String description() {return "JL "+super.description();}
    }

    final static public class JumpCond32_d_nl extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NL());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
        public String description() {return "JNL "+super.description();}
    }

    final static public class JumpCond32_d_le extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_LE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
        public String description() {return "JLE "+super.description();}
    }

    final static public class JumpCond32_d_nle extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NLE());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
        public String description() {return "JNLE "+super.description();}
    }

    final static public class PushFS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU_Regs.reg_fsVal.dword);
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
        public String description() {return "PUSH FS";}
    }

    final static public class PopFS extends Op {
        public int call() {
            if (CPU.CPU_PopSegFS(true)) return RUNEXCEPTION();
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
        public int setsSeg() {return FS|FROM_STACK;}
        public String description() {return "POP FS";}
    }

    final static public class BtEdGd_reg extends Op {
        int mask;
        Reg rd;
        Reg eard;

        public BtEdGd_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            Flags.FillFlags();
            mask=1 << (rd.dword & 31);
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
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
    }

    final static public class BtEdGd_mem extends Op {
        int mask;
        Reg rd;
        EaaBase get_eaa;

        public BtEdGd_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            Flags.FillFlags();
            mask=1 << (rd.dword & 31);
            int eaa=get_eaa.call();
            eaa+=(rd.dword>>5)*4; // intentional signed shift
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class ShldEdGdIb_reg extends Op {
        int op3;
        Reg rd;
        Reg eard;

        public ShldEdGdIb_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
            op3 = decode_fetchb() & 0x1F;
        }

        public int call() {
            eard.dword=Instructions.DSHLD(rd.dword, op3, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHLD;}
    }

    final static public class ShldEdGdIb_mem extends Op {
        Reg rd;
        EaaBase get_eaa;
        int op3;

        public ShldEdGdIb_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa = Mod.getEaa(rm);
            op3 = decode_fetchb() & 0x1F;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.DSHLD(rd.dword, op3, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHLD;}
    }

    final static public class ShldEdGdCl_reg extends Op {
        Reg rd;
        Reg eard;

        public ShldEdGdCl_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            int op3=CPU_Regs.reg_ecx.low() & 0x1F;
            if (op3!=0)
                eard.dword=Instructions.DSHLD(rd.dword, op3, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHLD;}
    }

    final static public class ShldEdGdCl_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public ShldEdGdCl_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int op3 = CPU_Regs.reg_ecx.low() & 0x1F;
            if (op3!=0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.DSHLD(rd.dword, op3, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHLD;}
    }

    final static public class PushGS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU_Regs.reg_gsVal.dword);
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
    }

    final static public class PopGS extends Op {
        public int call() {
            if (CPU.CPU_PopSegGS(true)) return RUNEXCEPTION();
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
        public int setsSeg() {return GS|FROM_STACK;}
    }

    final static public class BtsEdGd_reg extends Op {
        int mask;
        Reg rd;
        Reg eard;

        public BtsEdGd_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            Flags.FillFlags();
            mask=1 << (rd.dword & 31);
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
            eard.dword|=mask;
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
    }

    final static public class BtsEdGd_mem extends Op {
        int mask;
        Reg rd;
        EaaBase get_eaa;

        public BtsEdGd_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            Flags.FillFlags();
            mask=1 << (rd.dword & 31);
            int eaa=get_eaa.call();
            eaa+=(rd.dword>>5)*4; // intentional signed shift
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writed(eaa,old | mask);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class ShrdEdGdIb_reg extends Op {
        int op3;
        Reg rd;
        Reg eard;

        public ShrdEdGdIb_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
            op3 = decode_fetchb() & 0x1F;
        }

        public int call() {
            eard.dword=Instructions.DSHRD(rd.dword, op3, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHRD;}
    }

    final static public class ShrdEdGdIb_mem extends Op {
        Reg rd;
        EaaBase get_eaa;
        int op3;

        public ShrdEdGdIb_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa = Mod.getEaa(rm);
            op3 = decode_fetchb() & 0x1F;
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.DSHRD(rd.dword, op3, Memory.mem_readd(eaa)));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHRD;}
    }

    final static public class ShrdEdGdCl_reg extends Op {
        Reg rd;
        Reg eard;

        public ShrdEdGdCl_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            int op3 = CPU_Regs.reg_ecx.low() & 0x1F;
            if (op3!=0)
                eard.dword=Instructions.DSHRD(rd.dword, op3, eard.dword);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHRD;}
    }

    final static public class ShrdEdGdCl_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public ShrdEdGdCl_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int op3=CPU_Regs.reg_ecx.low() & 0x1F;
            if (op3!=0) {
                int eaa = get_eaa.call();
                Memory.mem_writed(eaa, Instructions.DSHRD(rd.dword, op3, Memory.mem_readd(eaa)));
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        // AF is always 0
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
        public int getFlagType() {return FLAG_TYPE_DSHRD;}
    }

    final static public class ImulGdEd_reg extends Op {
        Reg rd;
        Reg eard;

        public ImulGdEd_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            rd.dword=Instructions.DIMULD(eard.dword,rd.dword);
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
    }

    final static public class ImulGdEd_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public ImulGdEd_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            rd.dword=Instructions.DIMULD(Memory.mem_readd(eaa),rd.dword);
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
    }

    final static public class CmpxchgEdGd_reg extends Op {
        Reg rd;
        Reg eard;

        public CmpxchgEdGd_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.CMPD(eard.dword, reg_eax.dword);
            Flags.FillFlags();
            if (reg_eax.dword == eard.dword) {
                eard.dword=rd.dword;
                SETFLAGBIT(ZF,true);
            } else {
                reg_eax.dword=eard.dword;
                SETFLAGBIT(ZF,false);
            }
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
    }

    final static public class CmpxchgEdGd_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public CmpxchgEdGd_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int val = Memory.mem_readd(eaa);
            Instructions.CMPD(val, reg_eax.dword);
            Flags.FillFlags();
            if (reg_eax.dword == val) {
                Memory.mem_writed(eaa,rd.dword);
                SETFLAGBIT(ZF,true);
            } else {
                Memory.mem_writed(eaa,val);	// cmpxchg always issues a write
                reg_eax.dword=val;
                SETFLAGBIT(ZF,false);
            }
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
    }

    final static public class LssEd extends Op {
        Reg rd;
        EaaBase get_eaa;

        public LssEd(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
            rd.dword=Memory.mem_readd(eaa);
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
        public int setsSeg() {return SS|FROM_MEMORY;}
    }

    final static public class BtrEdGd_reg extends Op {
        Reg rd;
        Reg eard;

        public BtrEdGd_reg(int rm) {
            rd = Mod.gd(rm);
            eard = Mod.ed(rm);
        }

        public int call() {
            Flags.FillFlags();
            int mask=1 << (rd.dword & 31);
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
            eard.dword&=~mask;
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
    }

    final static public class BtrEdGd_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public BtrEdGd_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            Flags.FillFlags();
            int mask=1 << (rd.dword & 31);
            eaa+=(rd.dword>>5)*4; // intentional signed shift
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writed(eaa,old & ~mask);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class LfsEd extends Op {
        Reg rd;
        EaaBase get_eaa;

        public LfsEd(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
            rd.dword=Memory.mem_readd(eaa);
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
    }

    final static public class LgsEd extends Op {
        Reg rd;
        EaaBase get_eaa;

        public LgsEd(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
            rd.dword=Memory.mem_readd(eaa);
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
    }

    final static public class MovzxGdEb_reg extends Op {
        Reg rd;
        Reg earb;

        public MovzxGdEb_reg(int rm) {
            rd = Mod.gd(rm);
            earb = Mod.eb(rm);
        }

        public int call() {
            rd.dword=earb.get8();
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
    }

    final static public class MovzxGdEb_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public MovzxGdEb_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            rd.dword=Memory.mem_readb(eaa);
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
    }

    final static public class MovzxGdEw_reg extends Op {
        Reg rd;
        Reg earw;

        public MovzxGdEw_reg(int rm) {
            rd = Mod.gd(rm);
            earw = Mod.ew(rm);
        }

        public int call() {
            rd.dword=earw.word();
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
    }

    final static public class MovzxGdEw_mem extends Op {
        Reg rd;
        EaaBase get_eaa;

        public MovzxGdEw_mem(int rm) {
            rd = Mod.gd(rm);
            get_eaa =  Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            rd.dword=Memory.mem_readw(eaa);
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
    }

    final static public class BtEdIb_reg extends Op {
        Reg eard;
        int mask;

        public BtEdIb_reg(int rm) {
            eard = Mod.ed(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
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
    }

    final static public class BtsEdIb_reg extends Op {
        Reg eard;
        int mask;

        public BtsEdIb_reg(int rm) {
            eard = Mod.ed(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
            eard.dword|=mask;
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
    }

    final static public class BtrEdIb_reg extends Op {
        Reg eard;
        int mask;

        public BtrEdIb_reg(int rm) {
            eard = Mod.ed(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
            eard.dword&=~mask;
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
    }

    final static public class BtcEdIb_reg extends Op {
        Reg eard;
        int mask;

        public BtcEdIb_reg(int rm) {
            eard = Mod.ed(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
            if (GETFLAG(CF)!=0) eard.dword&=~mask;
            else eard.dword|=mask;
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
    }

    final static public class BtEdIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtEdIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BtsEdIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtsEdIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writed(eaa,old|mask);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BtrEdIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtrEdIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writed(eaa,old & ~mask);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BtcEdIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtcEdIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 31);
        }

        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            if (GETFLAG(CF)!=0) old&=~mask;
            else old|=mask;
            Memory.mem_writed(eaa,old);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BtcEdGd_reg extends Op {
        Reg eard;
        Reg rd;

        public BtcEdGd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            Flags.FillFlags();
            int mask=1 << (rd.dword & 31);
            SETFLAGBIT(CF,(eard.dword & mask)!=0);
            eard.dword^=mask;
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
    }

    final static public class BtcEdGd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public BtcEdGd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }
        public int call() {
            Flags.FillFlags();
            int mask=1 << (rd.dword & 31);
            int eaa=get_eaa.call();
            eaa+=(rd.dword>>5)*4; // intentional signed shift
            int old=Memory.mem_readd(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writed(eaa,old ^ mask);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.CF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BsfGdEd_reg extends Op {
        Reg eard;
        Reg rd;

        public BsfGdEd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int value=eard.dword;
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 0;
                while ((value & 0x01)==0) { result++; value>>>=1; }
                SETFLAGBIT(ZF,false);
                rd.dword=result;
            }
            Flags.type=Flags.t_UNKNOWN;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BsfGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public BsfGdEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int value=Memory.mem_readd(eaa);
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 0;
                while ((value & 0x01)==0) { result++; value>>>=1; }
                SETFLAGBIT(ZF,false);
                rd.dword=result;
            }
            Flags.type=Flags.t_UNKNOWN;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BsrGdEd_reg extends Op {
        Reg eard;
        Reg rd;

        public BsrGdEd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int value=eard.dword;
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 31;	// Operandsize-1
                while ((value & 0x80000000)==0) { result--; value<<=1; }
                SETFLAGBIT(ZF,false);
                rd.dword=result;
            }
            Flags.type=Flags.t_UNKNOWN;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class BsrGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public BsrGdEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int value=Memory.mem_readd(eaa);
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 31;	// Operandsize-1
                while ((value & 0x80000000)==0) { result--; value<<=1; }
                SETFLAGBIT(ZF,false);
                rd.dword=result;
            }
            Flags.type=Flags.t_UNKNOWN;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int sets() {
            return CPU_Regs.ZF;
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class MovsxGdEb_reg extends Op {
        Reg earb;
        Reg rd;

        public MovsxGdEb_reg(int rm) {
            earb = Mod.eb(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            rd.dword=(byte)earb.get8();
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
    }

    final static public class MovsxGdEb_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public MovsxGdEb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            rd.dword=(byte)Memory.mem_readb(eaa);
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
    }

    final static public class MovsxGdEw_reg extends Op {
        Reg earw;
        Reg rd;

        public MovsxGdEw_reg(int rm) {
            earw = Mod.ew(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            rd.dword=(short)earw.word();
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
    }

    final static public class MovsxGdEw_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public MovsxGdEw_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            rd.dword=(short)Memory.mem_readw(eaa);
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
    }

    final static public class XaddGdEd_reg extends Op {
        Reg eard;
        Reg rd;

        public XaddGdEd_reg(int rm) {
            eard = Mod.ed(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            // :DOSBOX: this is different from dosbox
            int result=Instructions.ADDD(rd.dword, eard.dword);
            rd.dword=eard.dword;
            eard.dword=result;
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
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class XaddGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;

        public XaddGdEd_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            // :DOSBOX: this is different from dosbox
            int eaa=get_eaa.call();
            int val = Memory.mem_readd(eaa);
            int result = Instructions.ADDD(rd.dword, val);
            Memory.mem_writed(eaa,result);
            rd.dword=val;
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
        public int getFlagType() {return FLAG_TYPE_ADDD;}
    }

    final static public class Bswapd extends Op {
        Reg reg;

        public Bswapd(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            reg.dword=Instructions.BSWAPD(reg.dword);
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
    }
    
    static abstract public class ConditionalMov_reg extends Op {
        Reg ed;
        Reg gd;

        public ConditionalMov_reg(int rm) {
            ed = Mod.ed(rm);
            gd = Mod.gd(rm);
        }

        public int sets() {return 0;}
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class ConditionalMov_o_reg extends ConditionalMov_reg {
        public ConditionalMov_o_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_O())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.OF;}
        public String description() {return "CMOV_O "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_no_reg extends ConditionalMov_reg {
        public ConditionalMov_no_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NO())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.OF;}
        public String description() {return "CMOV_NO "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_b_reg extends ConditionalMov_reg {
        public ConditionalMov_b_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_B())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF;}
        public String description() {return "CMOV_B "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_nb_reg extends ConditionalMov_reg {
        public ConditionalMov_nb_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NB())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF;}
        public String description() {return "CMOV_NB "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_z_reg extends ConditionalMov_reg {
        public ConditionalMov_z_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_Z())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.ZF;}
        public String description() {return "CMOV_Z "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_nz_reg extends ConditionalMov_reg {
        public ConditionalMov_nz_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NZ())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.ZF;}
        public String description() {return "CMOV_NZ "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_be_reg extends ConditionalMov_reg {
        public ConditionalMov_be_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_BE())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF | CPU_Regs.ZF;}
        public String description() {return "CMOV_BE "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_nbe_reg extends ConditionalMov_reg {
        public ConditionalMov_nbe_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NBE())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF | CPU_Regs.ZF;}
        public String description() {return "CMOV_NBE "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_s_reg extends ConditionalMov_reg {
        public ConditionalMov_s_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_S())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF;}
        public String description() {return "CMOV_S "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_ns_reg extends ConditionalMov_reg {
        public ConditionalMov_ns_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NS())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF;}
        public String description() {return "CMOV_NS "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_p_reg extends ConditionalMov_reg {
        public ConditionalMov_p_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_P())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.PF;}
        public String description() {return "CMOV_P "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_np_reg extends ConditionalMov_reg {
        public ConditionalMov_np_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NP())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF;}
        public String description() {return "CMOV_NP "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_l_reg extends ConditionalMov_reg {
        public ConditionalMov_l_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_L())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF;}
        public String description() {return "CMOV_L "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_nl_reg extends ConditionalMov_reg {
        public ConditionalMov_nl_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NL())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF;}
        public String description() {return "CMOV_NL "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_le_reg extends ConditionalMov_reg {
        public ConditionalMov_le_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_LE())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;}
        public String description() {return "CMOV_LE "+gd.getName()+", "+ed.getName();}
    }

    final static public class ConditionalMov_nle_reg extends ConditionalMov_reg {
        public ConditionalMov_nle_reg(int rm) {
            super(rm);
        }
        public int call() {
            if (Flags.TFLG_NLE())
                gd.dword = ed.dword;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;}
        public String description() {return "CMOV_NLE "+gd.getName()+", "+ed.getName();}
    }

     static abstract public class ConditionalMov_mem extends Op {
        EaaBase get_eaa;
        Reg gd;

        public ConditionalMov_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            gd = Mod.gd(rm);
        }

        public int sets() {return 0;}
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    final static public class ConditionalMov_o_mem extends ConditionalMov_mem {
        public ConditionalMov_o_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_O())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.OF;}
        public String description() {return "CMOV_O "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_no_mem extends ConditionalMov_mem {
        public ConditionalMov_no_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NO())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.OF;}
        public String description() {return "CMOV_NO "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_b_mem extends ConditionalMov_mem {
        public ConditionalMov_b_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_B())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF;}
        public String description() {return "CMOV_B "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_nb_mem extends ConditionalMov_mem {
        public ConditionalMov_nb_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NB())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF;}
        public String description() {return "CMOV_NB "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_z_mem extends ConditionalMov_mem {
        public ConditionalMov_z_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_Z())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.ZF;}
        public String description() {return "CMOV_Z "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_nz_mem extends ConditionalMov_mem {
        public ConditionalMov_nz_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NZ())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.ZF;}
        public String description() {return "CMOV_NZ "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_be_mem extends ConditionalMov_mem {
        public ConditionalMov_be_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_BE())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF | CPU_Regs.ZF;}
        public String description() {return "CMOV_BE "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_nbe_mem extends ConditionalMov_mem {
        public ConditionalMov_nbe_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NBE())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.CF | CPU_Regs.ZF;}
        public String description() {return "CMOV_NBE "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_s_mem extends ConditionalMov_mem {
        public ConditionalMov_s_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_S())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF;}
        public String description() {return "CMOV_S "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_ns_mem extends ConditionalMov_mem {
        public ConditionalMov_ns_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NS())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF;}
        public String description() {return "CMOV_NS "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_p_mem extends ConditionalMov_mem {
        public ConditionalMov_p_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_P())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.PF;}
        public String description() {return "CMOV_P "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_np_mem extends ConditionalMov_mem {
        public ConditionalMov_np_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NP())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF;}
        public String description() {return "CMOV_NP "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_l_mem extends ConditionalMov_mem {
        public ConditionalMov_l_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_L())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF;}
        public String description() {return "CMOV_L "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_nl_mem extends ConditionalMov_mem {
        public ConditionalMov_nl_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NL())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF;}
        public String description() {return "CMOV_NL "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_le_mem extends ConditionalMov_mem {
        public ConditionalMov_le_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_LE())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;}
        public String description() {return "CMOV_LE "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class ConditionalMov_nle_mem extends ConditionalMov_mem {
        public ConditionalMov_nle_mem(int rm) {
            super(rm);
        }
        public int call() {
            int temp = Memory.mem_readd(get_eaa.call()); // must read before comparison so that it can throw errors
            if (Flags.TFLG_NLE())
                gd.dword=temp;
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }
        public int gets() {return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;}
        public String description() {return "CMOV_NLE "+gd.getName()+", "+get_eaa.description32();}
    }

    final static public class CompareExchange8B extends Op {
        EaaBase get_eaa;

        public CompareExchange8B(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            long value1 = ((CPU_Regs.reg_edx.dword & 0xffffffffL) << 32) | (CPU_Regs.reg_eax.dword & 0xffffffffL);
            int eaa = get_eaa.call();
            long value2 = (Memory.mem_readd(eaa) & 0xffffffffl) | ((Memory.mem_readd(eaa+4) & 0xffffffffl) << 32);
            Flags.FillFlags();
            if (value1==value2) {
                CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, true);
                Memory.mem_writed(eaa, CPU_Regs.reg_ebx.dword);
                Memory.mem_writed(eaa+4, CPU_Regs.reg_ecx.dword);
            } else {
                CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, false);
                CPU_Regs.reg_edx.dword = (int)(value2 >>> 32);
                CPU_Regs.reg_eax.dword = (int)value2;
            }
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public int gets() {
            return 0;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int sets() {return CPU_Regs.ZF;}
        public String description() {return "CMPXCHG8B " + get_eaa.description32();}
    }

    final static public class ReadMSR extends Op {
        public int call() {
            if (CPU.cpu.cpl != 0) return EXCEPTION(CPU.EXCEPTION_GP);
            long result = CPU.readMSR(CPU_Regs.reg_ecx.dword);
            CPU_Regs.reg_eax.dword = (int)result;
            CPU_Regs.reg_edx.dword = (int)(result >>> 32);
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int sets() {return 0;}
        public int gets() {return 0;}
    }

    final static public class WriteMSR extends Op {
        public int call() {
            if (CPU.cpu.cpl != 0) return EXCEPTION(CPU.EXCEPTION_GP);
            CPU.writeMSR(CPU_Regs.reg_ecx.dword, ((CPU_Regs.reg_edx.dword & 0xFFFFFFFFl) << 32) | (CPU_Regs.reg_eax.dword & 0xFFFFFFFFl));
            CPU_Regs.reg_eip+=eip_count;return next.call();
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
        public int sets() {return 0;}
        public int gets() {return 0;}
    }
}
