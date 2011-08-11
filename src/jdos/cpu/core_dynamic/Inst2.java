package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.util.IntRef;

public class Inst2 extends Helper {
    final static public class Sldt_reg extends Op {
        Reg earw;

        public Sldt_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            earw.word(CPU.CPU_SLDT());
            return Constants.BR_Normal;
        }
    }

    final static public class Sldt_mem extends Op {
        EaaBase get_eaa;

        public Sldt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();
            Memory.mem_writew(eaa, CPU.CPU_SLDT());
            return Constants.BR_Normal;
        }
    }

    final static public class Str_reg extends Op {
        Reg earw;

        public Str_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            earw.word(CPU.CPU_STR());
            return Constants.BR_Normal;
        }
    }

    final static public class Str_mem extends Op {
        EaaBase get_eaa;

        public Str_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();
            Memory.mem_writew(eaa, CPU.CPU_STR());
            return Constants.BR_Normal;
        }
    }

    final static public class Lldt_reg extends Op {
        Reg earw;

        public Lldt_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            if (CPU.CPU_LLDT(earw.word())) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Lldt_mem extends Op {
        EaaBase get_eaa;

        public Lldt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            int eaa=get_eaa.call();
            if (CPU.CPU_LLDT(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Ltr_reg extends Op {
        Reg earw;

        public Ltr_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            if (CPU.CPU_LTR(earw.word())) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Ltr_mem extends Op {
        EaaBase get_eaa;

        public Ltr_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            int eaa=get_eaa.call();
            if (CPU.CPU_LTR(Memory.mem_readw(eaa))) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Verr_reg extends Op {
        Reg earw;

        public Verr_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            CPU.CPU_VERR(earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class Verr_mem extends Op {
        EaaBase get_eaa;

        public Verr_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();
            CPU.CPU_VERR(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class Verw_reg extends Op {
        Reg earw;

        public Verw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            CPU.CPU_VERW(earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class Verw_mem extends Op {
        EaaBase get_eaa;

        public Verw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            int eaa=get_eaa.call();
            CPU.CPU_VERW(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class Sgdt_mem extends Op {
        EaaBase get_eaa;

        public Sgdt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());
            Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());
            return Constants.BR_Normal;
        }
    }

    final static public class Sidt_mem extends Op {
        EaaBase get_eaa;

        public Sidt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());
            Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());
            return Constants.BR_Normal;
        }
    }

    final static public class Lgdt_mem extends Op {
        EaaBase get_eaa;

        public Lgdt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            int v1 = (Memory.mem_readd(eaa + 2) & 0xFFFFFF);
            int v0 = Memory.mem_readw(eaa);
            CPU.CPU_LGDT(v0,v1);
            return Constants.BR_Normal;
        }
    }

    final static public class Lidt_mem extends Op {
        EaaBase get_eaa;

        public Lidt_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            int v1 = (Memory.mem_readd(eaa + 2) & 0xFFFFFF);
            int v0 = Memory.mem_readw(eaa);
            CPU.CPU_LIDT(v0,v1);
            return Constants.BR_Normal;
        }
    }

    final static public class Smsw_mem extends Op {
        EaaBase get_eaa;

        public Smsw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writew(eaa,(int)(CPU.CPU_SMSW() & 0xFFFFl));
            return Constants.BR_Normal;
        }
    }

    final static public class Lmsw_mem extends Op {
        EaaBase get_eaa;

        public Lmsw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int limit=Memory.mem_readw(eaa);
            if (CPU.CPU_LMSW(limit)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Invlpg extends Op {
        public int call() {
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            Paging.PAGING_ClearTLB();
            return Constants.BR_Normal;
        }
    }

    final static public class Lgdt_reg extends Op {
        public int call() {
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            return Constants.BR_Illegal;
        }
    }

    final static public class Lidt_reg extends Op {
        public int call() {
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            return Constants.BR_Illegal;
        }
    }

    final static public class Smsw_reg extends Op {
        Reg earw;

        public Smsw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word((int)(CPU.CPU_SMSW() & 0xFFFF));
            return Constants.BR_Normal;
        }
    }

    final static public class Lmsw_reg extends Op {
        Reg earw;

        public Lmsw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            if (CPU.CPU_LMSW(earw.word())) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class LarGwEw_reg extends Op {
        Reg earw;
        Reg rw;
        public LarGwEw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            IntRef value=new IntRef(rw.word());
            CPU.CPU_LAR(earw.word(),value);
            rw.word(value.value);
            return Constants.BR_Normal;
        }
    }

    final static public class LarGwEw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public LarGwEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            IntRef value=new IntRef(rw.word());
            CPU.CPU_LAR(Memory.mem_readw(eaa),value);
            rw.word(value.value);
            return Constants.BR_Normal;
        }
    }

    final static public class LslGwEw_reg extends Op {
        Reg earw;
        Reg rw;
        IntRef value = new IntRef(0);

        public LslGwEw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            value.value=rw.word();
            CPU.CPU_LSL(earw.word(),value);
            rw.word(value.value);
            return Constants.BR_Normal;
        }
    }

    final static public class LslGwEw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;
        IntRef value = new IntRef(0);

        public LslGwEw_mem(int rm) {
            get_eaa= Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;
            value.value=rw.word();
            CPU.CPU_LSL(Memory.mem_readw(eaa),value);
            rw.word(value.value);
            return Constants.BR_Normal;
        }
    }

    final static public class Clts extends Op {
        public int call() {
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            CPU.cpu.cr0&=(~CPU.CR0_TASKSWITCH);
            return Constants.BR_Normal;
        }
    }

    final static public class Invd extends Op {
        public int call() {
            if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
            return Constants.BR_Normal;
        }
    }

    final static public class MovRdCr extends Op {
        Reg eard;
        int which;

        public MovRdCr(int rm) {
            eard = Mod.ed(rm);
            which=(rm >> 3) & 7;
        }

        public int call() {
            if (CPU.CPU_READ_CRX(which,eard)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovRdDr extends Op {
        Reg eard;
        int which;

        public MovRdDr(int rm) {
            eard = Mod.ed(rm);
            which=(rm >> 3) & 7;
        }

        public int call() {
            if (CPU.CPU_READ_DRX(which,eard)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovCrRd extends Op {
        Reg eard;
        int which;

        public MovCrRd(int rm) {
            eard = Mod.ed(rm);
            which=(rm >> 3) & 7;
        }

        public int call() {
            if (CPU.CPU_WRITE_CRX(which,eard.dword)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovDrRd extends Op {
        Reg eard;
        int which;

        public MovDrRd(int rm) {
            eard = Mod.ed(rm);
            which=(rm >> 3) & 7;
        }

        public int call() {
            if (CPU.CPU_WRITE_DRX(which,eard.dword)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovRdTr extends Op {
        Reg eard;
        int which;

        public MovRdTr(int rm) {
            eard = Mod.ed(rm);
            which=(rm >> 3) & 7;
        }

        public int call() {
            if (CPU.CPU_READ_TRX(which,eard)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class MovTrRd extends Op {
        Reg eard;
        int which;

        public MovTrRd(int rm) {
            eard = Mod.ed(rm);
            which=(rm >> 3) & 7;
        }

        public int call() {
            if (CPU.CPU_WRITE_TRX(which,eard.dword)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class Rdtsc extends Op {
        public int call() {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM) return Constants.BR_Illegal;
            /*Bit64s*/long tsc=(/*Bit64s*/long)(Pic.PIC_FullIndex()*(double)CPU.CPU_CycleMax);
            reg_edx.dword((tsc>>>32));
            reg_eax.dword((tsc&0xffffffffl));
            return Constants.BR_Normal;
        }
    }

    static abstract public class JumpCond16_w extends Op {
        int offset;
        public JumpCond16_w() {
            offset = decode_fetchws();
        }
        final protected int jump(boolean COND, int off) {
            if (COND) {
                reg_ip(reg_ip()+off+(int)eip_count);
                return Constants.BR_Link1;
            }
            reg_ip(reg_ip()+(int)eip_count);
            return Constants.BR_Link2;
        }
    }

    final static public class JumpCond16_w_o extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_O(), offset);
        }
    }

    final static public class JumpCond16_w_no extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NO(), offset);
        }
    }

    final static public class JumpCond16_w_b extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_B(), offset);
        }
    }

    final static public class JumpCond16_w_nb extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NB(), offset);
        }
    }

    final static public class JumpCond16_w_z extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_Z(), offset);
        }
    }

    final static public class JumpCond16_w_nz extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NZ(), offset);
        }
    }

    final static public class JumpCond16_w_be extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_BE(), offset);
        }
    }

    final static public class JumpCond16_w_nbe extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NBE(), offset);
        }
    }

    final static public class JumpCond16_w_s extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_S(), offset);
        }
    }

    final static public class JumpCond16_w_ns extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NS(), offset);
        }
    }

    final static public class JumpCond16_w_p extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_P(), offset);
        }
    }

    final static public class JumpCond16_w_np extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NP(), offset);
        }
    }

    final static public class JumpCond16_w_l extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_L(), offset);
        }
    }

    final static public class JumpCond16_w_nl extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NL(), offset);
        }
    }

    final static public class JumpCond16_w_le extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_LE(), offset);
        }
    }

    final static public class JumpCond16_w_nle extends JumpCond16_w {
        public int call() {
            return jump(Flags.TFLG_NLE(), offset);
        }
    }

    final static public class SETcc_reg_o extends Op {
        Reg earb;

        public SETcc_reg_o(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_O()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_o extends Op {
        EaaBase get_eaa;

        public SETcc_mem_o(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_O()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_no extends Op {
        Reg earb;

        public SETcc_reg_no(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NO()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_no extends Op {
        EaaBase get_eaa;

        public SETcc_mem_no(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NO()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_b extends Op {
        Reg earb;

        public SETcc_reg_b(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_B()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_b extends Op {
        EaaBase get_eaa;

        public SETcc_mem_b(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_B()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_nb extends Op {
        Reg earb;

        public SETcc_reg_nb(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NB()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_nb extends Op {
        EaaBase get_eaa;

        public SETcc_mem_nb(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NB()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_z extends Op {
        Reg earb;

        public SETcc_reg_z(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_Z()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_z extends Op {
        EaaBase get_eaa;

        public SETcc_mem_z(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_Z()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_nz extends Op {
        Reg earb;

        public SETcc_reg_nz(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NZ()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_nz extends Op {
        EaaBase get_eaa;

        public SETcc_mem_nz(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NZ()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_be extends Op {
        Reg earb;

        public SETcc_reg_be(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_BE()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_be extends Op {
        EaaBase get_eaa;

        public SETcc_mem_be(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_BE()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_nbe extends Op {
        Reg earb;

        public SETcc_reg_nbe(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NBE()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_nbe extends Op {
        EaaBase get_eaa;

        public SETcc_mem_nbe(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NBE()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_s extends Op {
        Reg earb;

        public SETcc_reg_s(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_S()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_s extends Op {
        EaaBase get_eaa;

        public SETcc_mem_s(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_S()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_ns extends Op {
        Reg earb;

        public SETcc_reg_ns(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NS()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_ns extends Op {
        EaaBase get_eaa;

        public SETcc_mem_ns(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NS()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_p extends Op {
        Reg earb;

        public SETcc_reg_p(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_P()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_p extends Op {
        EaaBase get_eaa;

        public SETcc_mem_p(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_P()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_np extends Op {
        Reg earb;

        public SETcc_reg_np(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NP()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_np extends Op {
        EaaBase get_eaa;

        public SETcc_mem_np(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NP()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_l extends Op {
        Reg earb;

        public SETcc_reg_l(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_L()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_l extends Op {
        EaaBase get_eaa;

        public SETcc_mem_l(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_L()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_nl extends Op {
        Reg earb;

        public SETcc_reg_nl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NL()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_nl extends Op {
        EaaBase get_eaa;

        public SETcc_mem_nl(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NL()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_le extends Op {
        Reg earb;

        public SETcc_reg_le(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_LE()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_le extends Op {
        EaaBase get_eaa;

        public SETcc_mem_le(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_LE()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_reg_nle extends Op {
        Reg earb;

        public SETcc_reg_nle(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            earb.set8((short)((Flags.TFLG_NLE()) ? 1 : 0));
            return Constants.BR_Normal;
        }
    }

    final static public class SETcc_mem_nle extends Op {
        EaaBase get_eaa;

        public SETcc_mem_nle(int rm) {
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Memory.mem_writeb(eaa, ((short)((Flags.TFLG_NLE()) ? 1 : 0)));
            return Constants.BR_Normal;
        }
    }

    final static public class PushFS extends Op {
        public int call() {
            CPU.CPU_Push16((int) CPU.Segs_FSval);
            return Constants.BR_Normal;
        }
    }

    final static public class PopFS extends Op {
        public int call() {
            if (CPU.CPU_PopSegFS(false)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class CPUID extends Op {
        public int call() {
            if (!CPU.CPU_CPUID()) return Constants.BR_Illegal;
            return Constants.BR_Normal;
        }
    }

    final static public class BtEwGw_reg extends Op {
        int mask;
        Reg rw;
        Reg earw;

        public BtEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            Flags.FillFlags();
            mask=1 << (rw.word() & 15);
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            return Constants.BR_Normal;
        }
    }

    final static public class BtEwGw_mem extends Op {
        int mask;
        Reg rw;
        EaaBase get_eaa;

        public BtEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            Flags.FillFlags();
            mask=1 << (rw.word() & 15);
            int eaa=get_eaa.call();
            eaa+=(((/*Bit16s*/short)rw.word())>>4)*2;
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            return Constants.BR_Normal;
        }
    }

    final static public class ShldEwGwIb_reg extends Op {
        int op3;
        Reg rw;
        Reg earw;

        public ShldEwGwIb_reg(int rm, Reg earw, int op3) {
            rw = Mod.gw(rm);
            this.earw = earw;
            this.op3 = op3;
        }
        public int call() {
            earw.word(Instructions.do_DSHLW(rw.word(), op3, earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class ShldEwGwIb_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        int op3;

        public ShldEwGwIb_mem(int rm, EaaBase get_eaa, int op3) {
            rw = Mod.gw(rm);
            this.get_eaa = get_eaa;
            this.op3 = op3;
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.do_DSHLW(rw.word(), op3, Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.do_DSHLW(rw.word(), op3, Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class ShldEwGwCl_reg extends Op {
        Reg rw;
        Reg earw;

        public ShldEwGwCl_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            if (Instructions.valid_DSHLW(CPU_Regs.reg_ecx.low()))
                earw.word(Instructions.do_DSHLW(rw.word(), CPU_Regs.reg_ecx.low(), earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class ShldEwGwCl_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public ShldEwGwCl_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            if (Instructions.valid_DSHLW(CPU_Regs.reg_ecx.low())) {
                int eaa = get_eaa.call();
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_DSHLW(rw.word(), CPU_Regs.reg_ecx.low(), Memory.host_readw(index)));
                        return Constants.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_DSHLW(rw.word(), CPU_Regs.reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Constants.BR_Normal;
        }
    }

    final static public class PushGS extends Op {
        public int call() {
            CPU.CPU_Push16((int) CPU.Segs_GSval);
            return Constants.BR_Normal;
        }
    }

    final static public class PopGS extends Op {
        public int call() {
            if (CPU.CPU_PopSegGS(false)) return RUNEXCEPTION();
            return Constants.BR_Normal;
        }
    }

    final static public class BtsEwGw_reg extends Op {
        int mask;
        Reg rw;
        Reg earw;

        public BtsEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            Flags.FillFlags();
            mask=1 << (rw.word() & 15);
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            earw.word(earw.word() | mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtsEwGw_mem extends Op {
        int mask;
        Reg rw;
        EaaBase get_eaa;

        public BtsEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa= Mod.getEaa(rm);
        }
        public int call() {
            Flags.FillFlags();
            mask=1 << (rw.word() & 15);
            int eaa=get_eaa.call();
            eaa+=(((/*Bit16s*/short)rw.word())>>4)*2;

            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                     int old=Memory.host_readw(index);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.host_writew(index,old | mask);
                    return Constants.BR_Normal;
                }
            }
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writew(eaa,old | mask);
            return Constants.BR_Normal;
        }
    }

    final static public class ShrdEwGwIb_reg extends Op {
        int op3;
        Reg rw;
        Reg earw;

        public ShrdEwGwIb_reg(int rm, Reg earw, int op3) {
            rw = Mod.gw(rm);
            this.earw = earw;
            this.op3 = op3;
        }
        public int call() {
            earw.word(Instructions.do_DSHRW(rw.word(), op3, earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class ShrdEwGwIb_mem extends Op {
        Reg rw;
        EaaBase get_eaa;
        int op3;

        public ShrdEwGwIb_mem(int rm, EaaBase get_eaa, int op3) {
            rw = Mod.gw(rm);
            this.get_eaa = get_eaa;
            this.op3 = op3;
        }
        public int call() {
            int eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writew(index, Instructions.do_DSHRW(rw.word(), op3, Memory.host_readw(index)));
                    return Constants.BR_Normal;
                }
            }
            Memory.mem_writew(eaa, Instructions.do_DSHRW(rw.word(), op3, Memory.mem_readw(eaa)));
            return Constants.BR_Normal;
        }
    }

    final static public class ShrdEwGwCl_reg extends Op {
        Reg rw;
        Reg earw;

        public ShrdEwGwCl_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            if (Instructions.valid_DSHRW(CPU_Regs.reg_ecx.low()))
                earw.word(Instructions.do_DSHRW(rw.word(), CPU_Regs.reg_ecx.low(), earw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class ShrdEwGwCl_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public ShrdEwGwCl_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            if (Instructions.valid_DSHRW(CPU_Regs.reg_ecx.low())) {
                int eaa = get_eaa.call();
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_DSHRW(rw.word(), CPU_Regs.reg_ecx.low(), Memory.host_readw(index)));
                        return Constants.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_DSHRW(rw.word(), CPU_Regs.reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Constants.BR_Normal;
        }
    }

    final static public class ImulGwEw_reg extends Op {
        Reg rw;
        Reg earw;

        public ImulGwEw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            rw.word(Instructions.DIMULW(earw.word(),rw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class ImulGwEw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public ImulGwEw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa = get_eaa.call();
            rw.word(Instructions.DIMULW(Memory.mem_readw(eaa),rw.word()));
            return Constants.BR_Normal;
        }
    }

    final static public class CmpxchgEbGb_reg extends Op {
        Reg rb;
        Reg earb;

        public CmpxchgEbGb_reg(int rm) {
            rb = Mod.gb(rm);
            earb = Mod.eb(rm);
        }
        public int call() {
            Flags.FillFlags();
            if (reg_eax.low() == earb.get8()) {
                earb.set8(rb.get8());
                SETFLAGBIT(ZF,true);
            } else {
                reg_eax.low(earb.get8());
                SETFLAGBIT(ZF,false);
            }
            return Constants.BR_Normal;
        }
    }

    final static public class CmpxchgEbGb_mem extends Op {
        Reg rb;
        EaaBase get_eaa;

        public CmpxchgEbGb_mem(int rm) {
            rb = Mod.gb(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0) {
                short val = Memory.host_readb(index);
                if (reg_eax.low() == val) {
                    Memory.host_writeb(index,rb.get8());
                    SETFLAGBIT(ZF,true);
                } else {
                    Memory.host_writeb(index,val);	// cmpxchg always issues a write
                    reg_eax.low(val);
                    SETFLAGBIT(ZF,false);
                }
            } else {
                short val = Memory.mem_readb(eaa);
                if (reg_eax.low() == val) {
                    Memory.mem_writeb(eaa,rb.get8());
                    SETFLAGBIT(ZF,true);
                } else {
                    Memory.mem_writeb(eaa,val);	// cmpxchg always issues a write
                    reg_eax.low(val);
                    SETFLAGBIT(ZF,false);
                }
            }
            return Constants.BR_Normal;
        }
    }
    final static public class CmpxchgEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public CmpxchgEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            Flags.FillFlags();
            if (reg_eax.word() == earw.word()) {
                earw.word(rw.word());
                SETFLAGBIT(ZF,true);
            } else {
                reg_eax.word(earw.word());
                SETFLAGBIT(ZF,false);
            }
            return Constants.BR_Normal;
        }
    }

    final static public class CmpxchgEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public CmpxchgEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int val = Memory.host_readw(index);
                    if (reg_eax.word() == val) {
                        Memory.host_writew(index,rw.word());
                        SETFLAGBIT(ZF,true);
                    } else {
                        Memory.host_writew(index,val);	// cmpxchg always issues a write
                        reg_eax.word(val);
                        SETFLAGBIT(ZF,false);
                    }
                    return Constants.BR_Normal;
                }
            }
            int val = Memory.mem_readw(eaa);
            if (reg_eax.word() == val) {
                Memory.mem_writew(eaa,rw.word());
                SETFLAGBIT(ZF,true);
            } else {
                Memory.mem_writew(eaa,val);	// cmpxchg always issues a write
                reg_eax.word(val);
                SETFLAGBIT(ZF,false);
            }
            return Constants.BR_Normal;
        }
    }

    final static public class LssEw extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LssEw(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(Memory.mem_readw(eaa));
            Core.base_ss=CPU.Segs_SSphys;
            return Constants.BR_Normal;
        }
    }

    final static public class BtrEwGw_reg extends Op {
        Reg rw;
        Reg earw;

        public BtrEwGw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            Flags.FillFlags();
            int mask=1 << (rw.word() & 15);
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            earw.word(earw.word() & ~mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtrEwGw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public BtrEwGw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            Flags.FillFlags();
            int mask=1 << (rw.word() & 15);
            eaa+=(((/*Bit16s*/short)rw.word())>>4)*2;
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int old=Memory.host_readw(index);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.host_writew(index,old & ~mask);
                    return Constants.BR_Normal;
                }
            }
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writew(eaa,old & ~mask);
            return Constants.BR_Normal;
        }
    }

    final static public class LfsEw extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LfsEw(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class LgsEw extends Op {
        Reg rw;
        EaaBase get_eaa;

        public LgsEw(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
            rw.word(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class MovzxGwEb_reg extends Op {
        Reg rw;
        Reg earb;

        public MovzxGwEb_reg(int rm) {
            rw = Mod.gw(rm);
            earb = Mod.eb(rm);
        }
        public int call() {
            rw.word(earb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class MovzxGwEb_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public MovzxGwEb_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            rw.word(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class MovzxGwEw_reg extends Op {
        Reg rw;
        Reg earw;

        public MovzxGwEw_reg(int rm) {
            rw = Mod.gw(rm);
            earw = Mod.ew(rm);
        }
        public int call() {
            rw.word(earw.word());
            return Constants.BR_Normal;
        }
    }

    final static public class MovzxGwEw_mem extends Op {
        Reg rw;
        EaaBase get_eaa;

        public MovzxGwEw_mem(int rm) {
            rw = Mod.gw(rm);
            get_eaa =  Mod.getEaa(rm);
        }
        public int call() {
            int eaa=get_eaa.call();
            rw.word(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class BtEwIb_reg extends Op {
        Reg earw;
        int mask;
        public BtEwIb_reg(int rm) {
            earw = Mod.ew(rm);
            mask=1 << (decode_fetchb() & 15);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            return Constants.BR_Normal;
        }
    }

    final static public class BtsEwIb_reg extends Op {
        Reg earw;
        int mask;
        public BtsEwIb_reg(int rm) {
            earw = Mod.ew(rm);
            mask=1 << (decode_fetchb() & 15);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            earw.word(earw.word() | mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtrEwIb_reg extends Op {
        Reg earw;
        int mask;
        public BtrEwIb_reg(int rm) {
            earw = Mod.ew(rm);
            mask=1 << (decode_fetchb() & 15);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            earw.word(earw.word() & ~mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtcEwIb_reg extends Op {
        Reg earw;
        int mask;
        public BtcEwIb_reg(int rm) {
            earw = Mod.ew(rm);
            mask=1 << (decode_fetchb() & 15);
        }

        public int call() {
            Flags.FillFlags();
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            earw.word(earw.word() ^ mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtEwIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtEwIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 15);
        }
        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            return Constants.BR_Normal;
        }
    }

    final static public class BtsEwIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtsEwIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 15);
        }
        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int old=Memory.host_readw(index);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.host_writew(index,old|mask);
                    return Constants.BR_Normal;
                }
            }
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writew(eaa,old|mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtrEwIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtrEwIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 15);
        }
        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int old=Memory.host_readw(index);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.host_writew(index,old & ~mask);
                    return Constants.BR_Normal;
                }
            }
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writew(eaa,old & ~mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtcEwIb_mem extends Op {
        EaaBase get_eaa;
        int mask;

        public BtcEwIb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            mask=1 << (decode_fetchb() & 15);
        }
        public int call() {
            Flags.FillFlags();
            int eaa=get_eaa.call();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int old=Memory.host_readw(index);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.host_writew(index,old ^ mask);
                    return Constants.BR_Normal;
                }
            }
            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writew(eaa,old ^ mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtcEwGw_reg extends Op {
        Reg earw;
        Reg rw;

        public BtcEwGw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            Flags.FillFlags();
            int mask=1 << (rw.word() & 15);
            SETFLAGBIT(CF,(earw.word() & mask)!=0);
            earw.word(earw.word()^mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BtcEwGw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public BtcEwGw_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }
        public int call() {
            Flags.FillFlags();
            int mask=1 << (rw.word() & 15);
            int eaa=get_eaa.call();
            eaa+=(((/*Bit16s*/short)rw.word())>>4)*2;
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int old=Memory.host_readw(index);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.host_writew(index,old ^ mask);
                    return Constants.BR_Normal;
                }
            }

            int old=Memory.mem_readw(eaa);
            SETFLAGBIT(CF,(old & mask)!=0);
            Memory.mem_writew(eaa,old ^ mask);
            return Constants.BR_Normal;
        }
    }

    final static public class BsfGwEw_reg extends Op {
        Reg earw;
        Reg rw;

        public BsfGwEw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int value=earw.word();
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 0;
                while ((value & 0x01)==0) { result++; value>>=1; }
                SETFLAGBIT(ZF,false);
                rw.word(result);
            }
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
        }
    }

    final static public class BsfGwEw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public BsfGwEw_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int value=Memory.mem_readw(eaa);
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 0;
                while ((value & 0x01)==0) { result++; value>>=1; }
                SETFLAGBIT(ZF,false);
                rw.word(result);
            }
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
        }
    }

    final static public class BsrGwEw_reg extends Op {
        Reg earw;
        Reg rw;

        public BsrGwEw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int value=earw.word();
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 15;	// Operandsize-1
                while ((value & 0x8000)==0) { result--; value<<=1; }
                SETFLAGBIT(ZF,false);
                rw.word(result);
            }
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
        }
    }

    final static public class BsrGwEw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public BsrGwEw_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int value=Memory.mem_readw(eaa);
            if (value==0) {
                SETFLAGBIT(ZF,true);
            } else {
                int result = 15;	// Operandsize-1
                while ((value & 0x8000)==0) { result--; value<<=1; }
                SETFLAGBIT(ZF,false);
                rw.word(result);
            }
            Flags.lflags.type=Flags.t_UNKNOWN;
            return Constants.BR_Normal;
        }
    }

    final static public class MovsxGwEb_reg extends Op {
        Reg earb;
        Reg rw;

        public MovsxGwEb_reg(int rm) {
            earb = Mod.eb(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            rw.word((byte)earb.get8());
            return Constants.BR_Normal;
        }
    }

    final static public class MovsxGwEb_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public MovsxGwEb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            rw.word((byte)Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }
    }

    final static public class XaddGbEb_reg extends Op {
        Reg earb;
        Reg rb;

        public XaddGbEb_reg(int rm) {
            earb = Mod.eb(rm);
            rb = Mod.gb(rm);
        }

        public int call() {
            short oldrmrb=rb.get8();
            rb.set8(earb.get8());
            earb.set8((short)(earb.get8()+oldrmrb));
            return Constants.BR_Normal;
        }
    }

    final static public class XaddGbEb_mem extends Op {
        EaaBase get_eaa;
        Reg rb;

        public XaddGbEb_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rb = Mod.gb(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            short oldrmrb=rb.get8();
            int index = Paging.getDirectIndex(eaa);
            if (index>=0) {
                short val = Memory.host_readb(index);
                Memory.host_writeb(index,(short)(val+oldrmrb));
                rb.set8(val);
                return Constants.BR_Normal;
            }
            short val = Memory.mem_readb(eaa);
            Memory.mem_writeb(eaa,val+oldrmrb);
            rb.set8(val);
            return Constants.BR_Normal;
        }
    }

    final static public class XaddGwEw_reg extends Op {
        Reg earw;
        Reg rw;

        public XaddGwEw_reg(int rm) {
            earw = Mod.ew(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int oldrmrw=rw.word();
            rw.word(earw.word());
            earw.word(earw.word()+oldrmrw);
            return Constants.BR_Normal;
        }
    }

    final static public class XaddGwEw_mem extends Op {
        EaaBase get_eaa;
        Reg rw;

        public XaddGwEw_mem(int rm) {
            get_eaa =  Mod.getEaa(rm);
            rw = Mod.gw(rm);
        }

        public int call() {
            int eaa=get_eaa.call();
            int oldrmrb=rw.word();
            if ((eaa & 0xFFF)<0xFFF) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    int val = Memory.host_readw(index);
                    Memory.host_writew(index,val+oldrmrb);
                    rw.word(val);
                    return Constants.BR_Normal;
                }
            }
            int val = Memory.mem_readw(eaa);
            Memory.mem_writew(eaa,val+oldrmrb);
            rw.word(val);
            return Constants.BR_Normal;
        }
    }

    final static public class Bswapw extends Op {
        Reg reg;

        public Bswapw(Reg reg) {
        this.reg = reg;
        }

        public int call() {
            reg.word(Instructions.BSWAPW(reg.word()));
            return Constants.BR_Normal;
        }
    }
}
