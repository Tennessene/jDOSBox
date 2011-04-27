package jdos.cpu.core_normal;

import jdos.cpu.*;
import jdos.hardware.Memory;

public class Prefix_helpers extends Instructions {
    static protected interface OP {
        public int call();
    }
    static protected final long[] AddrMaskTable={0x0000ffffl,0xffffffffl};

    protected static final int OPCODE_NONE=0x000;
    protected static final int OPCODE_0F=0x100;
    protected static final int OPCODE_SIZE=0x200;

    public static final int ILLEGAL_OPCODE = 1;
    public static final int NOT_HANDLED = 2;
    public static final int HANDLED = 3;
    public static final int RESTART = 4;
    public static final int CBRET_NONE = 5;
    public static final int CONTINUE = 6;
    public static final int RETURN = 7;
    public static final int DECODE_END = 8;

    static protected int TEST_PREFIX_ADDR() {
        return (prefixes & PREFIX_ADDR);
    }

    public static int RUNEXCEPTION() {
        CPU.CPU_Exception(CPU.cpu.exception.which,CPU.cpu.exception.error);
        return CONTINUE;
    }

    static public class ContinueException extends RuntimeException {}

    public static int EXCEPTION(int blah) {
        CPU.CPU_Exception(blah);
        return CONTINUE;
    }

    static protected long GETIP() {
        return (cseip- CPU.Segs_CSphys);
    }

    static protected void SAVEIP() {
        CPU_Regs.reg_eip(GETIP());
        //System.out.println("SAVEIP: "+CPU_Regs.reg_eip);
    }

    static protected void LOADIP() {
        cseip=CPU.Segs_CSphys+CPU_Regs.reg_eip();
        //System.out.println("LOADIP: "+cseip);
    }

    protected static void JumpCond16_b(boolean COND) {
        SAVEIP();
        if (COND) reg_ip(reg_ip()+Fetchbs.call());
        reg_ip(reg_ip()+1);
    }

    protected static void JumpCond16_w(boolean COND) {
        SAVEIP();
        if (COND) reg_ip(reg_ip()+Fetchws.call());
        reg_ip(reg_ip()+2);
    }

    protected static void JumpCond32_b(boolean COND) {
        SAVEIP();
        if (COND) reg_eip(reg_eip()+Fetchbs.call());
        reg_eip(reg_eip()+1);
    }

    protected static void JumpCond32_d(boolean COND) {
        SAVEIP();
        if (COND) reg_eip(reg_eip()+Fetchds.call());
        reg_eip(reg_eip()+4);
    }

    protected static void SETcc(boolean cc) {
        /*Bit8u*/short rm=Fetchb.call();
        if (rm >= 0xc0 ) {
            Modrm.GetEArb[rm].set((short)((cc) ? 1 : 0));}
        else {Memory.mem_writeb(getEaa(rm),(cc) ? 1 : 0);}
    }

//    #define RMGdEdOp3(inst,op3)													\
//	{																		\
//		GetRMrd;															\
//		if (rm >= 0xc0 ) {GetEArd;inst(*rmrd,*eard,op3,LoadRd,SaveRd);}		\
//		else {GetEAa;inst(*rmrd,LoadMd(eaa),op3,LoadRd,SaveRd);}			\
//	}

    static protected short r;
    static protected long m;

    static protected final Instructions.loadw rw_l = new Instructions.loadw() {
        final public int call() {
            return Modrm.Getrw[r].word();
        }
    };
    static protected final Instructions.savew rw_s = new Instructions.savew() {
        final public void call(int value) {
            Modrm.Getrw[r].word(value);
        }
    };
    static protected final Instructions.loadb earb_l = new Instructions.loadb() {
        final public short call() {
            return Modrm.GetEArb[r].get();
        }
    };
    static protected final Instructions.saveb earb_s = new Instructions.saveb() {
        final public void call(short value) {
            Modrm.GetEArb[r].set(value);
        }
    };
    static protected final Instructions.loadw earw_l = new Instructions.loadw() {
        final public int call() {
            return Modrm.GetEArw[r].word();
        }
    };
    static protected final Instructions.savew earw_s = new Instructions.savew() {
        final public void call(int value) {
            Modrm.GetEArw[r].word(value);
        }
    };

    static protected final Instructions.loadb b_l = new Instructions.loadb() {
        final public short call() {
            return Memory.mem_readb(m);
        }
    };
    static protected final Instructions.saveb b_s = new Instructions.saveb() {
        final public void call(short value) {
            Memory.mem_writeb(m, value);
        }
    };
    static protected final Instructions.loadw w_l = new Instructions.loadw() {
        final public int call() {
            return Memory.mem_readw(m);
        }
    };
    static protected final Instructions.savew w_s = new Instructions.savew() {
        final public void call(int value) {
            Memory.mem_writew(m, value);
        }
    };

    static protected void GRP2B(final short rm, int blah) {
        /*Bitu*/int which=(rm>>3)&7;
        if (rm >= 0xc0) {
            /*Bit8u*/short val=(short)(blah & 0x1f);
            r = rm;
            short earb = Modrm.GetEArb[rm].get();
            switch (which)	{
            case 0x00:ROLB(earb,val,earb_l,earb_s);break;
            case 0x01:RORB(earb,val,earb_l,earb_s);break;
            case 0x02:RCLB(val,earb_l,earb_s);break;
            case 0x03:RCRB(val,earb_l,earb_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(val,earb_l,earb_s);break;
            case 0x05:SHRB(val,earb_l,earb_s);break;
            case 0x07:SARB(val,earb_l,earb_s);break;
            }
        } else {
            m = getEaa(rm);
            /*Bit8u*/short val=(short)(blah & 0x1f);
            switch (which) {
            case 0x00:ROLB(m,val,b_l,b_s);break;
            case 0x01:RORB(m,val,b_l,b_s);break;
            case 0x02:RCLB(val,b_l,b_s);break;
            case 0x03:RCRB(val,b_l,b_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(val,b_l,b_s);break;
            case 0x05:SHRB(val,b_l,b_s);break;
            case 0x07:SARB(val,b_l,b_s);break;
            }
        }
    }

    static protected void GRP2B_fetchb(final short rm) {
        /*Bitu*/int which=(rm>>3)&7;
        if (rm >= 0xc0) {
            short blah = Fetchb.call();
            /*Bit8u*/short val=(short)(blah & 0x1f);
            r = rm;
            short earb = Modrm.GetEArb[rm].get();
            switch (which)	{
            case 0x00:ROLB(earb,val,earb_l,earb_s);break;
            case 0x01:RORB(earb,val,earb_l,earb_s);break;
            case 0x02:RCLB(val,earb_l,earb_s);break;
            case 0x03:RCRB(val,earb_l,earb_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(val,earb_l,earb_s);break;
            case 0x05:SHRB(val,earb_l,earb_s);break;
            case 0x07:SARB(val,earb_l,earb_s);break;
            }
        } else {
            m = getEaa(rm);
            short blah = Fetchb.call();
            /*Bit8u*/short val=(short)(blah & 0x1f);
            switch (which) {
            case 0x00:ROLB(m,val,b_l,b_s);break;
            case 0x01:RORB(m,val,b_l,b_s);break;
            case 0x02:RCLB(val,b_l,b_s);break;
            case 0x03:RCRB(val,b_l,b_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLB(val,b_l,b_s);break;
            case 0x05:SHRB(val,b_l,b_s);break;
            case 0x07:SARB(val,b_l,b_s);break;
            }
        }
    }

    static protected void GRP2W(final short rm, int blah) {
        /*Bitu*/int which=(rm>>3)&7;
        if (rm >= 0xc0) {
            /*Bit8u*/int val=blah & 0x1f;

            r = rm;
            int earw = Modrm.GetEArw[rm].word();
            switch (which)	{
            case 0x00:ROLW(earw,val,earw_l,earw_s);break;
            case 0x01:RORW(earw,val,earw_l,earw_s);break;
            case 0x02:RCLW(val,earw_l,earw_s);break;
            case 0x03:RCRW(val,earw_l,earw_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(val,earw_l,earw_s);break;
            case 0x05:SHRW(val,earw_l,earw_s);break;
            case 0x07:SARW(val,earw_l,earw_s);break;
            }
        } else {
            m = getEaa(rm);
            /*Bit8u*/int val=blah & 0x1f;
            switch (which) {
            case 0x00:ROLW(m,val,w_l,w_s);break;
            case 0x01:RORW(m,val,w_l,w_s);break;
            case 0x02:RCLW(val,w_l,w_s);break;
            case 0x03:RCRW(val,w_l,w_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(val,w_l,w_s);break;
            case 0x05:SHRW(val,w_l,w_s);break;
            case 0x07:SARW(val,w_l,w_s);break;
            }
        }
    }

    static protected void GRP2W_fetchb(final short rm) {
        /*Bitu*/int which=(rm>>3)&7;
        if (rm >= 0xc0) {
            int blah = Fetchb.call();
            /*Bit8u*/int val=blah & 0x1f;

            r = rm;
            int earw = Modrm.GetEArw[rm].word();
            switch (which)	{
            case 0x00:ROLW(earw,val,earw_l,earw_s);break;
            case 0x01:RORW(earw,val,earw_l,earw_s);break;
            case 0x02:RCLW(val,earw_l,earw_s);break;
            case 0x03:RCRW(val,earw_l,earw_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(val,earw_l,earw_s);break;
            case 0x05:SHRW(val,earw_l,earw_s);break;
            case 0x07:SARW(val,earw_l,earw_s);break;
            }
        } else {
            m = getEaa(rm);
            int blah = Fetchb.call();
            /*Bit8u*/int val=blah & 0x1f;
            switch (which) {
            case 0x00:ROLW(m,val,w_l,w_s);break;
            case 0x01:RORW(m,val,w_l,w_s);break;
            case 0x02:RCLW(val,w_l,w_s);break;
            case 0x03:RCRW(val,w_l,w_s);break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:SHLW(val,w_l,w_s);break;
            case 0x05:SHRW(val,w_l,w_s);break;
            case 0x07:SARW(val,w_l,w_s);break;
            }
        }
    }

    static protected void GRP2D(final short rm, int blah) {
        /*Bitu*/int which=(rm>>3)&7;
        if (rm >= 0xc0) {
            /*Bit8u*/int val=blah & 0x1f;
            if (val == 0) return;
            Reg r = Modrm.GetEArd[rm];
            switch (which)	{
            case 0x00:r.dword(ROLD(val,r.dword()));break;
            case 0x01:r.dword(RORD(val,r.dword()));break;
            case 0x02:r.dword(RCLD(val,r.dword()));break;
            case 0x03:r.dword(RCRD(val,r.dword()));break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:r.dword(SHLD(val,r.dword()));break;
            case 0x05:r.dword(SHRD(val,r.dword()));break;
            case 0x07:r.dword(SARD(val,r.dword()));break;
            }
        } else {
            long eaa = getEaa(rm);
            /*Bit8u*/int val=blah & 0x1f;

            if (val == 0) return;
            if ((eaa & 0xFFF)<0xFFD) {
                int addr = Paging.getDirectIndex(eaa);
                if (addr>=0) {
                    switch (which) {
                    case 0x00:Memory.host_writed(addr, ROLD(val,Memory.host_readd(addr)));break;
                    case 0x01:Memory.host_writed(addr, RORD(val,Memory.host_readd(addr)));break;
                    case 0x02:Memory.host_writed(addr, RCLD(val,Memory.host_readd(addr)));break;
                    case 0x03:Memory.host_writed(addr, RCRD(val,Memory.host_readd(addr)));break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:Memory.host_writed(addr, SHLD(val,Memory.host_readd(addr)));break;
                    case 0x05:Memory.host_writed(addr, SHRD(val,Memory.host_readd(addr)));break;
                    case 0x07:Memory.host_writed(addr, SARD(val,Memory.host_readd(addr)));break;
                    }
                    return;
                }
            }
            switch (which) {
            case 0x00:Memory.mem_writed(eaa, ROLD(val,Memory.mem_readd(eaa)));break;
            case 0x01:Memory.mem_writed(eaa, RORD(val,Memory.mem_readd(eaa)));break;
            case 0x02:Memory.mem_writed(eaa, RCLD(val,Memory.mem_readd(eaa)));break;
            case 0x03:Memory.mem_writed(eaa, RCRD(val,Memory.mem_readd(eaa)));break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:Memory.mem_writed(eaa, SHLD(val,Memory.mem_readd(eaa)));break;
            case 0x05:Memory.mem_writed(eaa, SHRD(val,Memory.mem_readd(eaa)));break;
            case 0x07:Memory.mem_writed(eaa, SARD(val,Memory.mem_readd(eaa)));break;
            }
        }
    }
}
