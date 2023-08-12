package jdos.cpu;

import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public abstract class Core extends CPU_Regs {
    static public /*Bitu*/int opcode_index;
    static public /*PhysPt*/int cseip;
    static public int base_val_ds;
    static public boolean rep_zero;

    static public /*PhysPt*/int base_ds,base_ss;
    static public /*Bitu*/int prefixes;
    static public Table_ea.GetEAHandler[] ea_table;

    static public int Fetchb() {
        return Memory.mem_readb(cseip++);
    }

    static public int Fetchbs() {
        return (byte)Memory.mem_readb(cseip++);
    }

    static public int Fetchw() {
        int temp = Memory.mem_readw(cseip);
        cseip+=2;
        return temp;
    }

    static public int Fetchws() {
        int temp = (short)Memory.mem_readw(cseip);
        cseip+=2;
        return temp;
    }
    static public int Fetchd() {
        int temp = Memory.mem_readd(cseip);
        cseip+=4;
        return temp;
    }
    static public int Fetchds() {
        int temp = Memory.mem_readd(cseip);
        cseip+=4;
        return temp;
    }
    static public void DO_PREFIX_SEG_ES() {
        base_ds=CPU_Regs.reg_esPhys.dword;
        base_ss=CPU_Regs.reg_esPhys.dword;
        base_val_ds=CPU_Regs.es;
    }

    static public void DO_PREFIX_SEG_CS() {
        base_ds=CPU_Regs.reg_csPhys.dword;
        base_ss=CPU_Regs.reg_csPhys.dword;
        base_val_ds=CPU_Regs.cs;
    }

    static public void DO_PREFIX_SEG_SS() {
        base_ds=CPU_Regs.reg_ssPhys.dword;
        base_ss=CPU_Regs.reg_ssPhys.dword;
        base_val_ds=CPU_Regs.ss;
    }

    static public void DO_PREFIX_SEG_DS() {
        base_ds=CPU_Regs.reg_dsPhys.dword;
        base_ss=CPU_Regs.reg_dsPhys.dword;
        base_val_ds=CPU_Regs.ds;
    }

    static public void DO_PREFIX_SEG_FS() {
        base_ds=CPU_Regs.reg_fsPhys.dword;
        base_ss=CPU_Regs.reg_fsPhys.dword;
        base_val_ds=CPU_Regs.fs;
    }

    static public void DO_PREFIX_SEG_GS() {
        base_ds=CPU_Regs.reg_gsPhys.dword;
        base_ss=CPU_Regs.reg_gsPhys.dword;
        base_val_ds=CPU_Regs.gs;
    }

    static public boolean isInvalidLock(int op) {
        int b = op;
        int modrm;
        int mod;

        /* X={8,16,32,64}  Y={16,32,64} */
        switch (b)
        {
            /* /2: ADC reg/memX, immX */
            /* /0: ADD reg/memX, immX */
            /* /4: AND reg/memX, immX */
            /* /1: OR  reg/memX, immX */
            /* /3: SBB reg/memX, immX */
            /* /5: SUB reg/memX, immX */
            /* /6: XOR reg/memX, immX */
            case 0x80:
            case 0x81:
            case 0x83:
                modrm = Memory.mem_readb(cseip);
                op = (modrm >> 3) & 7;
                if (op == 7) /* /7: CMP */
                    break;
                mod = (modrm >> 6) & 3;
                if (mod == 3) /* register destination */
                    break;
                return false;

            case 0x10: /* /r: ADC reg/mem8, reg8 */
            case 0x11: /* /r: ADC reg/memX, regY */
            case 0x00: /* /r: ADD reg/mem8, reg8 */
            case 0x01: /* /r: ADD reg/memX, regY */
            case 0x20: /* /r: AND reg/mem8, reg8 */
            case 0x21: /* /r: AND reg/memY, regY */
            case 0x08: /* /r: OR  reg/mem8, reg8 */
            case 0x09: /* /r: OR  reg/memY, regY */
            case 0x18: /* /r: SBB reg/mem8, reg8 */
            case 0x19: /* /r: SBB reg/memY, regY */
            case 0x28: /* /r: SUB reg/mem8, reg8 */
            case 0x29: /* /r: SUB reg/memY, regY */
            case 0x86: /* /r: XCHG reg/mem8, reg8 or XCHG reg8, reg/mem8 */
            case 0x87: /* /r: XCHG reg/memY, regY or XCHG regY, reg/memY */
            case 0x30: /* /r: XOR reg/mem8, reg8 */
            case 0x31: /* /r: XOR reg/memY, regY */
                modrm = Memory.mem_readb(cseip);
                mod = (modrm >> 6) & 3;
                if (mod == 3) /* register destination */
                    break;
                return false;

            /* /1: DEC reg/memX */
            /* /0: INC reg/memX */
            case 0xfe:
            case 0xff:
                modrm = Memory.mem_readb(cseip);
                mod = (modrm >> 6) & 3;
                if (mod == 3) /* register destination */
                    break;
                return false;

            /* /3: NEG reg/memX */
            /* /2: NOT reg/memX */
            case 0xf6:
            case 0xf7:
                modrm = Memory.mem_readb(cseip);
                mod = (modrm >> 6) & 3;
                if (mod == 3) /* register destination */
                    break;
                return false;

            case 0x0f:
                b = Memory.mem_readb(cseip);
                switch (b)
                {
                    /* /7: BTC reg/memY, imm8 */
                    /* /6: BTR reg/memY, imm8 */
                    /* /5: BTS reg/memY, imm8 */
                    case 0xba:
                        modrm = Memory.mem_readb(cseip+1);
                        op = (modrm >> 3) & 7;
                        if (op < 5)
                            break;
                        mod = (modrm >> 6) & 3;
                        if (mod == 3) /* register destination */
                            break;
                        return false;

                    case 0xbb: /* /r: BTC reg/memY, regY */
                    case 0xb3: /* /r: BTR reg/memY, regY */
                    case 0xab: /* /r: BTS reg/memY, regY */
                    case 0xb0: /* /r: CMPXCHG reg/mem8, reg8 */
                    case 0xb1: /* /r: CMPXCHG reg/memY, regY */
                    case 0xc0: /* /r: XADD reg/mem8, reg8 */
                    case 0xc1: /* /r: XADD reg/memY, regY */
                        modrm = Memory.mem_readb(cseip+1);
                        mod = (modrm >> 6) & 3;
                        if (mod == 3) /* register destination */
                            break;
                        return false;

                    /* /1: CMPXCHG8B mem64 or CMPXCHG16B mem128 */
                    case 0xc7:
                        modrm = Memory.mem_readb(cseip+1);
                        op = (modrm >> 3) & 7;
                        if (op != 1)
                            break;
                        return false;
                }
                break;
        }

        if (Log.level<= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"illegal lock sequence: eip=0x"+CPU_Regs.reg_eip);
        return true;
    }
    final static public int PREFIX_ADDR = 0x1;
    final static public int PREFIX_REP = 0x2;
    final static public int PREFIX_LOCK = 0x4;
}
