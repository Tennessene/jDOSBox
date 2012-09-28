package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;
import jdos.util.IntRef;

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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
    }

    final static public class Smsw_reg extends Op {
        Reg eard;

        public Smsw_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword=CPU.CPU_SMSW();
            return Constants.BR_Normal;
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

    final static public class Lmsw_reg extends Op {
        Reg eard;

        public Lmsw_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            if (CPU.CPU_LMSW(eard.dword)) return RUNEXCEPTION();
            return Constants.BR_Normal;
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
            IntRef value=new IntRef(rd.dword);
            CPU.CPU_LAR(earw.word(),value);
            rd.dword=value.value;
            return Constants.BR_Normal;
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
            IntRef value=new IntRef(rd.dword);
            CPU.CPU_LAR(Memory.mem_readw(eaa),value);
            rd.dword=value.value;
            return Constants.BR_Normal;
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
    }

    final static public class LslGdEd_reg extends Op {
        Reg earw;
        Reg rd;
        IntRef value = new IntRef(0);

        public LslGdEd_reg(int rm) {
            earw = Mod.ew(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            value.value = rd.dword;
            CPU.CPU_LSL(earw.word(),value);
            rd.dword=value.value;
            return Constants.BR_Normal;
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
    }

    final static public class LslGdEd_mem extends Op {
        EaaBase get_eaa;
        Reg rd;
        IntRef value=new IntRef(0);

        public LslGdEd_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rd = Mod.gd(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            value.value = rd.dword;
            CPU.CPU_LSL(Memory.mem_readw(eaa),value);
            rd.dword=value.value;
            return Constants.BR_Normal;
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
    }

static abstract public class JumpCond32_d extends Op {
        int offset;

        public JumpCond32_d() {
            offset = decode_fetchds();
        }
    
        final protected int jump(boolean COND, int off) {
            if (COND) {
                reg_eip+=off+eip_count;
                return Constants.BR_Link1;
            }
            reg_eip+=eip_count;
            return Constants.BR_Link2;
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
    }

    final static public class JumpCond32_d_o extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_O(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
    }

    final static public class JumpCond32_d_no extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NO(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.OF;
        }
    }

    final static public class JumpCond32_d_b extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_B(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
    }

    final static public class JumpCond32_d_nb extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NB(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF;
        }
    }

    final static public class JumpCond32_d_z extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_Z(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
    }

    final static public class JumpCond32_d_nz extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NZ(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.ZF;
        }
    }

    final static public class JumpCond32_d_be extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_BE(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
    }

    final static public class JumpCond32_d_nbe extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NBE(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.CF | CPU_Regs.ZF;
        }
    }

    final static public class JumpCond32_d_s extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_S(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
    }

    final static public class JumpCond32_d_ns extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NS(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF;
        }
    }

    final static public class JumpCond32_d_p extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_P(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
    }

    final static public class JumpCond32_d_np extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NP(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.PF;
        }
    }

    final static public class JumpCond32_d_l extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_L(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
    }

    final static public class JumpCond32_d_nl extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NL(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF;
        }
    }

    final static public class JumpCond32_d_le extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_LE(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
    }

    final static public class JumpCond32_d_nle extends JumpCond32_d {
        public int call() {
            return jump(Flags.TFLG_NLE(), offset);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.ZF;
        }
    }

    final static public class PushFS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU.Segs_FSval);
            return Constants.BR_Normal;
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

    final static public class PopFS extends Op {
        public int call() {
            if (CPU.CPU_PopSegFS(true)) return RUNEXCEPTION();
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
    }

    final static public class PushGS extends Op {
        public int call() {
            CPU.CPU_Push32(CPU.Segs_GSval);
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            Flags.FillFlags();
            Instructions.CMPD(eard.dword, reg_eax.dword);
            if (reg_eax.dword == eard.dword) {
                eard.dword=rd.dword;
                SETFLAGBIT(ZF,true);
            } else {
                reg_eax.dword=eard.dword;
                SETFLAGBIT(ZF,false);
            }
            return Constants.BR_Normal;
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
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int val = Memory.mem_readd(eaa);
            Instructions.CMPD(val, reg_eax.dword);
            if (reg_eax.dword == val) {
                Memory.mem_writed(eaa,rd.dword);
                SETFLAGBIT(ZF,true);
            } else {
                Memory.mem_writed(eaa,val);	// cmpxchg always issues a write
                reg_eax.dword=val;
                SETFLAGBIT(ZF,false);
            }
            return Constants.BR_Normal;
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
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
            Core.base_val_ds= CPU_Regs.ds;
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
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
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
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
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
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
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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
            return Constants.BR_Normal;
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

    final static public class Bswapd extends Op {
        Reg reg;

        public Bswapd(Reg reg) {
            this.reg = reg;
        }

        public int call() {
            reg.dword=Instructions.BSWAPD(reg.dword);
            return Constants.BR_Normal;
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
}
