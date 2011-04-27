package jdos.cpu.core_dynamic;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Instructions;
import jdos.cpu.Core_dynrec;
import jdos.cpu.Paging;
import jdos.hardware.Memory;

public class Grp2 extends Helper {
    static public class ROLB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public ROLB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_ROLB(earb.get8(), val))
                earb.set8(Instructions.do_ROLB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public RORB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_RORB(earb.get8(), val))
                earb.set8(Instructions.do_RORB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public RCLB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_RCLB(val))
                earb.set8(Instructions.do_RCLB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public RCRB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_RCRB(val))
                earb.set8(Instructions.do_RCRB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public SHLB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_SHLB(val))
                earb.set8(Instructions.do_SHLB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public SHRB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_SHRB(val))
                earb.set8(Instructions.do_SHRB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARB_reg extends Op {
        short val;
        CPU_Regs.Reg earb;

        public SARB_reg(int rm, short val) {
            this.val=val;
            earb = Mod.eb(rm);

        }
        public int call() {
            if (Instructions.valid_SARB(val))
                earb.set8(Instructions.do_SARB(val, earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public ROLB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_ROLB(eaa, val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_ROLB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_ROLB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public RORB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RORB(eaa, val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_RORB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_RORB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public RCLB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCLB(val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_RCLB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_RCLB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public RCRB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCRB(val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_RCRB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_RCRB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public SHLB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;

        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHLB(val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_SHLB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_SHLB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public SHRB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHRB(val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_SHRB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_SHRB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARB_mem extends Op {
        short val;
        EaaBase get_eaa;

        public SARB_mem(EaaBase get_eaa, short val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SARB(val)) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_SARB(val, Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_SARB(val, Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
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
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public ROLW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_ROLW(eaa, val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_ROLW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_ROLW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RORW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RORW(eaa, val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_RORW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_RORW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCLW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCLW(val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_RCLW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_RCLW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCRW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCRW(val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_RCRW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_RCRW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHLW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHLW(val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_SHLW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_SHLW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHRW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHRW(val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_SHRW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_SHRW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARW_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SARW_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SARW(val)) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_SARW(val, Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_SARW(val, Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public ROLB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_ROLB(earb.get8(), reg_ecx.low()))
                earb.set8(Instructions.do_ROLB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public RORB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_RORB(earb.get8(), reg_ecx.low()))
                earb.set8(Instructions.do_RORB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public RCLB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_RCLB(reg_ecx.low()))
                earb.set8(Instructions.do_RCLB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public RCRB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_RCRB(reg_ecx.low()))
                earb.set8(Instructions.do_RCRB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public SHLB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_SHLB(reg_ecx.low()))
                earb.set8(Instructions.do_SHLB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public SHRB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_SHRB(reg_ecx.low()))
                earb.set8(Instructions.do_SHRB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARB_reg_cl extends Op {
        CPU_Regs.Reg earb;

        public SARB_reg_cl(int rm) {
            earb = Mod.eb(rm);
        }
        public int call() {
            if (Instructions.valid_SARB(reg_ecx.low()))
                earb.set8(Instructions.do_SARB(reg_ecx.low(), earb.get8()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLB_mem_cl extends Op {
        EaaBase get_eaa;

        public ROLB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_ROLB(eaa, reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_ROLB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_ROLB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORB_mem_cl extends Op {
        EaaBase get_eaa;

        public RORB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RORB(eaa, reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_RORB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_RORB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLB_mem_cl extends Op {
        EaaBase get_eaa;

        public RCLB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCLB(reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_RCLB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_RCLB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRB_mem_cl extends Op {
        EaaBase get_eaa;

        public RCRB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCRB(reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_RCRB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_RCRB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLB_mem_cl extends Op {
        EaaBase get_eaa;

        public SHLB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHLB(reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_SHLB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_SHLB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRB_mem_cl extends Op {
        EaaBase get_eaa;

        public SHRB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHRB(reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_SHRB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_SHRB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARB_mem_cl extends Op {
        EaaBase get_eaa;

        public SARB_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SARB(reg_ecx.low())) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0)
                    Memory.host_writeb(index, Instructions.do_SARB(reg_ecx.low(), Memory.host_readb(index)));
                else
                    Memory.mem_writeb(eaa, Instructions.do_SARB(reg_ecx.low(), Memory.mem_readb(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public ROLW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            if (Instructions.valid_ROLW(earw.word(), reg_ecx.low()))
                earw.word(Instructions.do_ROLW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public RORW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            if (Instructions.valid_RORW(earw.word(), reg_ecx.low()))
                earw.word(Instructions.do_RORW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public RCLW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            if (Instructions.valid_RCLW(reg_ecx.low()))
                earw.word(Instructions.do_RCLW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public RCRW_reg_cl(int rm) {
            earw = Mod.ew(rm);

        }
        public int call() {
            if (Instructions.valid_RCRW(reg_ecx.low()))
                earw.word(Instructions.do_RCRW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public SHLW_reg_cl(int rm) {
            earw = Mod.ew(rm);

        }
        public int call() {
            if (Instructions.valid_SHLW(reg_ecx.low()))
                earw.word(Instructions.do_SHLW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public SHRW_reg_cl(int rm) {
            earw = Mod.ew(rm);

        }
        public int call() {
            if (Instructions.valid_SHRW(reg_ecx.low()))
                earw.word(Instructions.do_SHRW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARW_reg_cl extends Op {
        CPU_Regs.Reg earw;

        public SARW_reg_cl(int rm) {
            earw = Mod.ew(rm);
        }
        public int call() {
            if (Instructions.valid_SARW(reg_ecx.low()))
                earw.word(Instructions.do_SARW(reg_ecx.low(), earw.word()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLW_mem_cl extends Op {
        EaaBase get_eaa;

        public ROLW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_ROLW(eaa, reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_ROLW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_ROLW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORW_mem_cl extends Op {
        EaaBase get_eaa;

        public RORW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RORW(eaa, reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_RORW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_RORW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLW_mem_cl extends Op {
        EaaBase get_eaa;

        public RCLW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCLW(reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_RCLW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_RCLW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRW_mem_cl extends Op {
        EaaBase get_eaa;

        public RCRW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_RCRW(reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_RCRW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_RCRW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLW_mem_cl extends Op {
        EaaBase get_eaa;

        public SHLW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHLW(reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_SHLW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_SHLW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRW_mem_cl extends Op {
        EaaBase get_eaa;

        public SHRW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SHRW(reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_SHRW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_SHRW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARW_mem_cl extends Op {
        EaaBase get_eaa;

        public SARW_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if (Instructions.valid_SARW(reg_ecx.low())) {
                if ((eaa & 0xFFF)<0xFFF) {
                    int index = Paging.getDirectIndex(eaa);
                    if (index>=0) {
                        Memory.host_writew(index, Instructions.do_SARW(reg_ecx.low(), Memory.host_readw(index)));
                        return Core_dynrec.BR_Normal;
                    }
                }
                Memory.mem_writew(eaa, Instructions.do_SARW(reg_ecx.low(), Memory.mem_readw(eaa)));
            }
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public ROLD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.ROLD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public RORD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.RORD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public RCLD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.RCLD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public RCRD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.RCRD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public SHLD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.SHLD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public SHRD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.SHRD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARD_reg extends Op {
        int val;
        CPU_Regs.Reg eard;

        public SARD_reg(int rm, int val) {
            this.val = val;
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.SARD(val, eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public ROLD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.ROLD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.ROLD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RORD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.RORD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.RORD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCLD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.RCLD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.RCLD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public RCRD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.RCRD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.RCRD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHLD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.SHLD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.SHLD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SHRD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.SHRD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.SHRD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARD_mem extends Op {
        int val;
        EaaBase get_eaa;

        public SARD_mem(EaaBase get_eaa, int val) {
            this.get_eaa = get_eaa;
            this.val=val;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.SARD(val, Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.SARD(val, Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public ROLD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            eard.dword(Instructions.ROLD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public RORD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            eard.dword(Instructions.RORD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public RCLD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            eard.dword(Instructions.RCLD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public RCRD_reg_cl(int rm) {
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.RCRD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public SHLD_reg_cl(int rm) {
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.SHLD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public SHRD_reg_cl(int rm) {
            eard = Mod.ed(rm);

        }
        public int call() {
            eard.dword(Instructions.SHRD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARD_reg_cl extends Op {
        CPU_Regs.Reg eard;

        public SARD_reg_cl(int rm) {
            eard = Mod.ed(rm);
        }
        public int call() {
            eard.dword(Instructions.SARD(reg_ecx.low(), eard.dword()));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class ROLD_mem_cl extends Op {
        EaaBase get_eaa;

        public ROLD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.ROLD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.ROLD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RORD_mem_cl extends Op {
        EaaBase get_eaa;

        public RORD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.RORD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.RORD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCLD_mem_cl extends Op {
        EaaBase get_eaa;

        public RCLD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.RCLD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.RCLD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class RCRD_mem_cl extends Op {
        EaaBase get_eaa;

        public RCRD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.RCRD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.RCRD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHLD_mem_cl extends Op {
        EaaBase get_eaa;

        public SHLD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.SHLD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.SHLD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SHRD_mem_cl extends Op {
        EaaBase get_eaa;

        public SHRD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.SHRD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.SHRD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

    static public class SARD_mem_cl extends Op {
        EaaBase get_eaa;

        public SARD_mem_cl(EaaBase get_eaa) {
            this.get_eaa = get_eaa;
        }
        public int call() {
            long eaa = get_eaa.call();
            if ((eaa & 0xFFF)<0xFFD) {
                int index = Paging.getDirectIndex(eaa);
                if (index>=0) {
                    Memory.host_writed(index, Instructions.SARD(reg_ecx.low(), Memory.host_readd(index)));
                    return Core_dynrec.BR_Normal;
                }
            }
            Memory.mem_writed(eaa, Instructions.SARD(reg_ecx.low(), Memory.mem_readd(eaa)));
            return Core_dynrec.BR_Normal;
        }
    }

}
