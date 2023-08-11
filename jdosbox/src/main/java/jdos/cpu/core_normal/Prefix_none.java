package jdos.cpu.core_normal;

import jdos.cpu.*;
import jdos.debug.Debug;
import jdos.fpu.FPU;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Prefix_none extends StringOp {
    static protected final boolean CPU_TRAP_CHECK = true;
    static protected final boolean CPU_PIC_CHECK = true;
    static public int returnValue = 0;

    static public OP[] ops = new OP[0x400];

    static final CPU.Descriptor desc=new CPU.Descriptor();

    static protected interface mov {
        public void call();
    }
    static protected interface call1 {
        public int call();
    }

    static {
        OP not_handled = new OP() {
            public int call() {
                return NOT_HANDLED;
            }
        };

        for (int i=0;i<ops.length;i++)
            ops[i] = not_handled;

        /* ADD Eb,Gb */
        ops[0x00] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    int value = 0;
                    switch ((rm >> 3) & 7) {
                        case 0: value = reg_eax.low(); break;
                        case 1: value = reg_ecx.low(); break;
                        case 2: value = reg_edx.low(); break;
                        case 3: value = reg_ebx.low(); break;
                        case 4: value = reg_eax.high(); break;
                        case 5: value = reg_ecx.high(); break;
                        case 6: value = reg_edx.high(); break;
                        case 7: value = reg_ebx.high(); break;
                    }
                    switch (rm & 7) {
                        case 0: reg_eax.low(ADDB(value, reg_eax.low())); break;
                        case 1: reg_ecx.low(ADDB(value, reg_ecx.low())); break;
                        case 2: reg_edx.low(ADDB(value, reg_edx.low())); break;
                        case 3: reg_ebx.low(ADDB(value, reg_ebx.low())); break;
                        case 4: reg_eax.high(ADDB(value, reg_eax.high())); break;
                        case 5: reg_ecx.high(ADDB(value, reg_ecx.high())); break;
                        case 6: reg_edx.high(ADDB(value, reg_edx.high())); break;
                        case 7: reg_ebx.high(ADDB(value, reg_ebx.high())); break;
                    }
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, ADDB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x200] = ops[0x00];

        /* ADD Ew,Gw */
        ops[0x01] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArw[rm].word(ADDW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, ADDW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* ADD Gb,Eb */
        ops[0x02] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(ADDB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(ADDB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x202] = ops[0x02];

        /* ADD Gw,Ew */
        ops[0x03] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrw[rm].word(ADDW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word()));
                }
                else {
                    Modrm.Getrw[rm].word(ADDW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word()));
                }
                return HANDLED;
            }
        };

        /* ADD AL,Ib */
        ops[0x04] = new OP() {
            final public int call() {
                reg_eax.low(ADDB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x204] = ops[0x04];

        /* ADD AX,Iw */
        ops[0x05] = new OP() {
            final public int call() {
                reg_eax.word(ADDW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* PUSH ES */
        ops[0x06] = new OP() {
            final public int call() {
                CPU.CPU_Push16(CPU_Regs.reg_esVal.dword);
                return HANDLED;
            }
        };

        /* POP ES */
        ops[0x07] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegES(false)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* OR Eb,Gb */
        ops[0x08] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArb[rm].set(ORB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, ORB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x208] = ops[0x08];

        /* OR Ew,Gw */
        ops[0x09] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    r = rm;
                    Modrm.GetEArw[rm].word(ORW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, ORW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* OR Gb,Eb */
        ops[0x0a] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(ORB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(ORB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x20a] = ops[0x0a];

        /* OR Gw,Ew */
        ops[0x0b] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                Reg r = Modrm.Getrw[rm];
                if (rm >= 0xc0 ) {
                    r.word(ORW(Modrm.GetEArw[rm].word(),r.word()));
                }
                else {
                    r.word(ORW(Memory.mem_readw(getEaa(rm)),r.word()));
                }
                return HANDLED;
            }
        };

        /* OR AL,Ib */
        ops[0x0c] = new OP() {
            final public int call() {
                reg_eax.low(ORB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x20c] = ops[0x0c];

        /* OR AX,Iw */
        ops[0x0d] = new OP() {
            final public int call() {
                reg_eax.word(ORW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* PUSH CS */
        ops[0x0e] = new OP() {
            final public int call() {
                CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);
                return HANDLED;
            }
        };

        /* 2 byte opcodes*/
        ops[0x0f] = new OP() {
            final public int call() {
                opcode_index|=OPCODE_0F;
                return RESTART;
            }
        };
        ops[0x20f] = ops[0x0f];

        /* ADC Eb,Gb */
        ops[0x10] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    r = rm;
                    Modrm.GetEArb[rm].set(ADCB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, ADCB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x210] = ops[0x10];

        /* ADC Ew,Gw */
        ops[0x11] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArw[rm].word(ADCW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, ADCW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* ADC Gb,Eb */
        ops[0x12] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(ADCB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(ADCB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x212] = ops[0x12];

        /* ADC Gw,Ew */
        ops[0x13] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrw[rm].word(ADCW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word()));
                }
                else {
                    Modrm.Getrw[rm].word(ADCW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word()));
                }
                return HANDLED;
            }
        };

        /* ADC AL,Ib */
        ops[0x14] = new OP() {
            final public int call() {
                reg_eax.low(ADCB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x214] = ops[0x14];

        /* ADC AX,Iw */
        ops[0x15] = new OP() {
            final public int call() {
                reg_eax.word(ADCW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* PUSH SS */
        ops[0x16] = new OP() {
            final public int call() {
                CPU.CPU_Push16(CPU_Regs.reg_ssVal.dword);
                return HANDLED;
            }
        };

        /* POP SS */
        ops[0x17] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegSS(false)) return RUNEXCEPTION();
                CPU.CPU_Cycles++; //Always do another instruction
                return HANDLED;
            }
        };

        /* SBB Eb,Gb */
        ops[0x18] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArb[rm].set(SBBB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, SBBB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x218] = ops[0x18];

        /* SBB Ew,Gw */
        ops[0x19] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    r = rm;
                    Modrm.GetEArw[rm].word(SBBW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, SBBW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* SBB Gb,Eb */
        ops[0x1a] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(SBBB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(SBBB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x21a] = ops[0x1a];

        /* SBB Gw,Ew */
        ops[0x1b] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                r = rm;
                if (rm >= 0xc0 ) {
                    Modrm.Getrw[rm].word(SBBW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word()));
                }
                else {
                    Modrm.Getrw[rm].word(SBBW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word()));
                }
                return HANDLED;
            }
        };

        /* SBB AL,Ib */
        ops[0x1c] = new OP() {
            final public int call() {
                reg_eax.low(SBBB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x21c] = ops[0x1c];

        /* SBB AX,Iw */
        ops[0x1d] = new OP() {
            final public int call() {
                reg_eax.word(SBBW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* PUSH DS */
        ops[0x1e] = new OP() {
            final public int call() {
                CPU.CPU_Push16(CPU_Regs.reg_dsVal.dword);
                return HANDLED;
            }
        };

        /* POP DS */
        ops[0x1f] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegDS(false)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* AND Eb,Gb */
        ops[0x20] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArb[rm].set(ANDB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, ANDB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x220] = ops[0x20];

        /* AND Ew,Gw */
        ops[0x21] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArw[rm].word(ANDW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, ANDW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* AND Gb,Eb */
        ops[0x22] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(ANDB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(ANDB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x222] = ops[0x22];

        /* AND Gw,Ew */
        ops[0x23] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                r = rm;
                if (rm >= 0xc0 ) {
                    Modrm.Getrw[rm].word(ANDW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word()));
                }
                else {
                    Modrm.Getrw[rm].word(ANDW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word()));
                }
                return HANDLED;
            }
        };

        /* AND AL,Ib */
        ops[0x24] = new OP() {
            final public int call() {
                reg_eax.low(ANDB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x224] = ops[0x24];

        /* AND AX,Iw */
        ops[0x25] = new OP() {
            final public int call() {
                reg_eax.word(ANDW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* SEG ES: */
        ops[0x26] = new OP() {
            final public int call() {
                DO_PREFIX_SEG_ES();
                return RESTART;
            }
        };
        ops[0x226] = ops[0x26];

        /* DAA */
        ops[0x27] = new OP() {
            final public int call() {
                Instructions.DAA();
                return HANDLED;
            }
        };
        ops[0x227] = ops[0x27];

        /* SUB Eb,Gb */
        ops[0x28] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArb[rm].set(SUBB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, SUBB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x228] = ops[0x28];

        /* SUB Ew,Gw */
        ops[0x29] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArw[rm].word(SUBW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, SUBW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* SUB Gb,Eb */
        ops[0x2a] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(SUBB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(SUBB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x22a] = ops[0x2a];

        /* SUB Gw,Ew */
        ops[0x2b] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                r = rm;
                if (rm >= 0xc0 ) {
                    Modrm.Getrw[rm].word(SUBW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word()));
                }
                else {
                    Modrm.Getrw[rm].word(SUBW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word()));
                }
                return HANDLED;
            }
        };

        /* SUB AL,Ib */
        ops[0x2c] = new OP() {
            final public int call() {
                reg_eax.low(SUBB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x22c] = ops[0x2c];

        /* SUB AX,Iw */
        ops[0x2d] = new OP() {
            final public int call() {
                reg_eax.word(SUBW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* SEG CS: */
        ops[0x2e] = new OP() {
            final public int call() {
                DO_PREFIX_SEG_CS();
                return RESTART;
            }
        };
        ops[0x22e] = ops[0x2e];

        /* DAS */
        ops[0x2f] = new OP() {
            final public int call() {
                Instructions.DAS();
                return HANDLED;
            }
        };
        ops[0x22f] = ops[0x2f];

        /* XOR Eb,Gb */
        ops[0x30] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArb[rm].set(XORB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writeb(eaa, XORB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa)));
                }
                return HANDLED;
            }
        };
        ops[0x230] = ops[0x30];

        /* XOR Ew,Gw */
        ops[0x31] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArw[rm].word(XORW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word()));
                }
                else {
                    int eaa = getEaa(rm);
                    Memory.mem_writew(eaa, XORW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa)));
                }
                return HANDLED;
            }
        };

        /* XOR Gb,Eb */
        ops[0x32] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb[rm].set(XORB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get()));
                }
                else {
                    Modrm.Getrb[rm].set(XORB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get()));
                }
                return HANDLED;
            }
        };
        ops[0x232] = ops[0x32];

        /* XOR Gw,Ew */
        ops[0x33] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Modrm.Getrw[rm].word(XORW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word()));
                }
                else {
                    Modrm.Getrw[rm].word(XORW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word()));
                }
                return HANDLED;
            }
        };

        /* XOR AL,Ib */
        ops[0x34] = new OP() {
            final public int call() {
                reg_eax.low(XORB(Fetchb(),reg_eax.low()));
                return HANDLED;
            }
        };
        ops[0x234] = ops[0x34];

        /* XOR AX,Iw */
        ops[0x35] = new OP() {
            final public int call() {
                reg_eax.word(XORW(Fetchw(),reg_eax.word()));
                return HANDLED;
            }
        };

        /* SEG SS: */
        ops[0x36] = new OP() {
            final public int call() {
                DO_PREFIX_SEG_SS();
                return RESTART;
            }
        };
        ops[0x236] = ops[0x36];

        /* AAA */
        ops[0x37] = new OP() {
            final public int call() {
                Instructions.AAA();
                return HANDLED;
            }
        };
        ops[0x237] = ops[0x37];

        /* CMP Eb,Gb */
        ops[0x38] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    CMPB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get());
                }
                else {
                    int eaa = getEaa(rm);
                    CMPB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa));
                }
                return HANDLED;
            }
        };
        ops[0x238] = ops[0x38];

        /* CMP Ew,Gw */
        ops[0x39] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    CMPW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word());
                }
                else {
                    int eaa = getEaa(rm);
                    CMPW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa));
                }
                return HANDLED;
            }
        };

        /* CMP Gb,Eb */
        ops[0x3a] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    CMPB(Modrm.GetEArb[rm].get(),Modrm.Getrb[rm].get());
                }
                else {
                    CMPB(Memory.mem_readb(getEaa(rm)),Modrm.Getrb[rm].get());
                }
                return HANDLED;
            }
        };
        ops[0x23a] = ops[0x3a];

        /* CMP Gw,Ew */
        ops[0x3b] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    CMPW(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word());
                }
                else {
                    CMPW(Memory.mem_readw(getEaa(rm)),Modrm.Getrw[rm].word());
                }
                return HANDLED;
            }
        };

        /* CMP AL,Ib */
        ops[0x3c] = new OP() {
            final public int call() {
                CMPB(Fetchb(),reg_eax.low());
                return HANDLED;
            }
        };
        ops[0x23c] = ops[0x3c];

        /* CMP AX,Iw */
        ops[0x3d] = new OP() {
            final public int call() {
                CMPW(Fetchw(),reg_eax.word());
                return HANDLED;
            }
        };

        /* SEG DS: */
        ops[0x3e] = new OP() {
            final public int call() {
                DO_PREFIX_SEG_DS();
                return RESTART;
            }
        };
        ops[0x23e] = ops[0x3e];

        /* AAS */
        ops[0x3f] = new OP() {
            final public int call() {
                Instructions.AAS();
                return HANDLED;
            }
        };
        ops[0x23f] = ops[0x3f];

        /* INC AX */
        ops[0x40] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_eax.word());
                lf_resw(lf_var1w()+1);
                reg_eax.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC CX */
        ops[0x41] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_ecx.word());
                lf_resw(lf_var1w()+1);
                reg_ecx.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC DX */
        ops[0x42] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_edx.word());
                lf_resw(lf_var1w()+1);
                reg_edx.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC BX */
        ops[0x43] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_ebx.word());
                lf_resw(lf_var1w()+1);
                reg_ebx.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC SP */
        ops[0x44] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_esp.word());
                lf_resw(lf_var1w()+1);
                reg_esp.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC BP */
        ops[0x45] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_ebp.word());
                lf_resw(lf_var1w()+1);
                reg_ebp.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC SI */
        ops[0x46] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_esi.word());
                lf_resw(lf_var1w()+1);
                reg_esi.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* INC DI */
        ops[0x47] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_edi.word());
                lf_resw(lf_var1w()+1);
                reg_edi.word(lf_resw());
                type=t_INCw;
                return HANDLED;
            }
        };

        /* DEC AX */
        ops[0x48] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_eax.word());
                lf_resw(lf_var1w()-1);
                reg_eax.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC CX */
        ops[0x49] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_ecx.word());
                lf_resw(lf_var1w()-1);
                reg_ecx.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC DX */
        ops[0x4a] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_edx.word());
                lf_resw(lf_var1w()-1);
                reg_edx.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC BX */
        ops[0x4b] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_ebx.word());
                lf_resw(lf_var1w()-1);
                reg_ebx.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC SP */
        ops[0x4c] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_esp.word());
                lf_resw(lf_var1w()-1);
                reg_esp.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC BP */
        ops[0x4d] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_ebp.word());
                lf_resw(lf_var1w()-1);
                reg_ebp.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC SI */
        ops[0x4e] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_esi.word());
                lf_resw(lf_var1w()-1);
                reg_esi.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* DEC DI */
        ops[0x4f] = new OP() {
            final public int call() {
                LoadCF();lf_var1w(reg_edi.word());
                lf_resw(lf_var1w()-1);
                reg_edi.word(lf_resw());
                type=t_DECw;
                return HANDLED;
            }
        };

        /* PUSH AX */
        ops[0x50] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_eax.word());
                return HANDLED;
            }
        };

        /* PUSH CX */
        ops[0x51] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_ecx.word());
                return HANDLED;
            }
        };

        /* PUSH DX */
        ops[0x52] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_edx.word());
                return HANDLED;
            }
        };

        /* PUSH BX */
        ops[0x53] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_ebx.word());
                return HANDLED;
            }
        };

        /* PUSH SP */
        ops[0x54] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_esp.word());
                return HANDLED;
            }
        };

        /* PUSH BP */
        ops[0x55] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_ebp.word());
                return HANDLED;
            }
        };

        /* PUSH SI */
        ops[0x56] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_esi.word());
                return HANDLED;
            }
        };

        /* PUSH DI */
        ops[0x57] = new OP() {
            final public int call() {
                CPU.CPU_Push16(reg_edi.word());
                return HANDLED;
            }
        };

        /* POP AX */
        ops[0x58] = new OP() {
            final public int call() {
                reg_eax.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP CX */
        ops[0x59] = new OP() {
            final public int call() {
                reg_ecx.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP DX */
        ops[0x5a] = new OP() {
            final public int call() {
                reg_edx.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP BX */
        ops[0x5b] = new OP() {
            final public int call() {
                reg_ebx.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP SP */
        ops[0x5c] = new OP() {
            final public int call() {
                reg_esp.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP BP */
        ops[0x5d] = new OP() {
            final public int call() {
                reg_ebp.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP SI */
        ops[0x5e] = new OP() {
            final public int call() {
                reg_esi.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* POP DI */
        ops[0x5f] = new OP() {
            final public int call() {
                reg_edi.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* PUSHA */
        ops[0x60] = new OP() {
            final public int call() {
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
                CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & CPU.cpu.stack.notmask)|(esp & CPU.cpu.stack.mask);
                return HANDLED;
            }
        };

        /* POPA */
        ops[0x61] = new OP() {
            final public int call() {
                reg_edi.word(CPU.CPU_Peek16(0));reg_esi.word(CPU.CPU_Peek16(1));reg_ebp.word(CPU.CPU_Peek16(2));//Don't save SP
                reg_ebx.word(CPU.CPU_Peek16(4));reg_edx.word(CPU.CPU_Peek16(5));reg_ecx.word(CPU.CPU_Peek16(6));reg_eax.word(CPU.CPU_Peek16(7));
                CPU_Regs.reg_esp.dword=(CPU_Regs.reg_esp.dword & CPU.cpu.stack.notmask)|((CPU_Regs.reg_esp.dword+16) & CPU.cpu.stack.mask);
                return HANDLED;
            }
        };

        /* BOUND */
        ops[0x62] = new OP() {
            final public int call() {
                /*Bit16s*/int bound_min, bound_max;
                /*Bit8u*/int rm=Fetchb();/*PhysPt*/int eaa=getEaa(rm);
                bound_min=(short)Memory.mem_readw(eaa);
                bound_max=(short)Memory.mem_readw(eaa+2);
                if ( (((short)Modrm.Getrw[rm].word()) < bound_min) || (((short)Modrm.Getrw[rm].word()) > bound_max) ) {
                    return EXCEPTION(5);
                }
                return HANDLED;
            }
        };

        /* ARPL Ew,Rw */
        ops[0x63] = new OP() {
            final public int call() {
                 if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    int value = CPU.CPU_ARPL(Modrm.GetEArw[rm].word(),Modrm.Getrw[rm].word());
                    Modrm.GetEArw[rm].word(value);
                } else {
                    /*PhysPt*/int eaa=getEaa(rm);
                    int value = Memory.mem_readw(eaa);
                    value = CPU.CPU_ARPL(value, Modrm.Getrw[rm].word());
                    Memory.mem_writew(eaa,value);
                }
                return HANDLED;
            }
        };

        /* SEG FS: */
        ops[0x64] = new OP() {
            final public int call() {
                DO_PREFIX_SEG_FS();
                return RESTART;
            }
        };
        ops[0x264] = ops[0x64];

        /* SEG GS: */
        ops[0x65] = new OP() {
            final public int call() {
                DO_PREFIX_SEG_GS();
                return RESTART;
            }
        };
        ops[0x265] = ops[0x65];

        /* Operand Size Prefix */
        ops[0x66] = new OP() {
            final public int call() {
                opcode_index=(CPU.cpu.code.big?0:512);
                return RESTART;
            }
        };
        ops[0x266] = ops[0x66];

        /* Address Size Prefix */
        ops[0x67] = new OP() {
            final public int call() {
                prefixes=(prefixes & ~PREFIX_ADDR) |(CPU.cpu.code.big?0:1);
                EA16 = (prefixes&1)==0;
                return RESTART;
            }
        };
        ops[0x267] = ops[0x67];

        /* PUSH Iw */
        ops[0x68] = new OP() {
            final public int call() {
                CPU.CPU_Push16(Fetchw());
                return HANDLED;
            }
        };

        /* IMUL Gw,Ew,Iw */
        ops[0x69] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                r = rm;
                if (rm >= 0xc0 ) {
                    int op3 = Fetchws();
                    Modrm.Getrw[rm].word(DIMULW(Modrm.GetEArw[rm].word(),op3));
                }
                else {
                    int eaa = getEaa(rm);
                    int op3 = Fetchws();
                    Modrm.Getrw[rm].word(DIMULW(Memory.mem_readw(eaa),op3));
                }
                return HANDLED;
            }
        };

        /* PUSH Ib */
        ops[0x6a] = new OP() {
            final public int call() {
                CPU.CPU_Push16(Fetchbs());
                return HANDLED;
            }
        };

        /* IMUL Gw,Ew,Ib */
        ops[0x6b] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                r = rm;
                if (rm >= 0xc0 ) {
                    int op3 = Fetchbs();
                    Modrm.Getrw[rm].word(DIMULW(Modrm.GetEArw[rm].word(),op3));
                }
                else {
                    int eaa = getEaa(rm);
                    int op3 = Fetchbs();
                    Modrm.Getrw[rm].word(DIMULW(Memory.mem_readw(eaa),op3));
                }
                return HANDLED;
            }
        };

        /* INSB */
        ops[0x6c] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
                DoString(R_INSB);
                return HANDLED;
            }
        };
        ops[0x26c] = ops[0x6c];

        /* INSW */
        ops[0x6d] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
                DoString(R_INSW);
                return HANDLED;
            }
        };

        /* OUTSB */
        ops[0x6e] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
                DoString(R_OUTSB);
                return HANDLED;
            }
        };
        ops[0x26e] = ops[0x6e];

        /* OUTSW */
        ops[0x6f] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
                DoString(R_OUTSW);
                return HANDLED;
            }
        };

        /* JO */
        ops[0x70] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_O());
                return CONTINUE;
            }
        };

        /* JNO */
        ops[0x71] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NO());
                return CONTINUE;
            }
        };

        /* JB */
        ops[0x72] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_B());
                return CONTINUE;
            }
        };

        /* JNB */
        ops[0x73] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NB());
                return CONTINUE;
            }
        };

        /* JZ */
        ops[0x74] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_Z());
                return CONTINUE;
            }
        };

        /* JNZ */
        ops[0x75] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NZ());
                return CONTINUE;
            }
        };

        /* JBE */
        ops[0x76] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_BE());
                return CONTINUE;
            }
        };

        /* JNBE */
        ops[0x77] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NBE());
                return CONTINUE;
            }
        };

        /* JS */
        ops[0x78] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_S());
                return CONTINUE;
            }
        };

        /* JNS */
        ops[0x79] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NS());
                return CONTINUE;
            }
        };

        /* JP */
        ops[0x7a] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_P());
                return CONTINUE;
            }
        };

        /* JNP */
        ops[0x7b] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NP());
                return CONTINUE;
            }
        };

        /* JL */
        ops[0x7c] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_L());
                return CONTINUE;
            }
        };

        /* JNL */
        ops[0x7d] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NL());
                return CONTINUE;
            }
        };

        /* JLE */
        ops[0x7e] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_LE());
                return CONTINUE;
            }
        };

        /* JNLE */
        ops[0x7f] = new OP() {
            final public int call() {
                JumpCond16_b(Flags.TFLG_NLE());
                return CONTINUE;
            }
        };

        /* Grpl Eb,Ib */
        ops[0x80] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                if (rm>= 0xc0) {
                    Modrm.Getrb_interface r = Modrm.GetEArb[rm];

                    /*Bit8u*/int ib=Fetchb();
                    switch (which) {
                    case 0x00:r.set(ADDB(ib,r.get()));break;
                    case 0x01: r.set(ORB(ib,r.get()));break;
                    case 0x02:r.set(ADCB(ib,r.get()));break;
                    case 0x03:r.set(SBBB(ib,r.get()));break;
                    case 0x04:r.set(ANDB(ib,r.get()));break;
                    case 0x05:r.set(SUBB(ib,r.get()));break;
                    case 0x06:r.set(XORB(ib,r.get()));break;
                    case 0x07:CMPB(ib,r.get());break;
                    }
                } else {
                    int eaa = m = getEaa(rm);

                    /*Bit8u*/int ib=Fetchb();
                    switch (which) {
                    case 0x00:Memory.mem_writeb(eaa, ADDB(ib,Memory.mem_readb(eaa)));break;
                    case 0x01: Memory.mem_writeb(eaa, ORB(ib,Memory.mem_readb(eaa)));break;
                    case 0x02:Memory.mem_writeb(eaa, ADCB(ib,Memory.mem_readb(eaa)));break;
                    case 0x03:Memory.mem_writeb(eaa, SBBB(ib,Memory.mem_readb(eaa)));break;
                    case 0x04:Memory.mem_writeb(eaa, ANDB(ib,Memory.mem_readb(eaa)));break;
                    case 0x05:Memory.mem_writeb(eaa, SUBB(ib,Memory.mem_readb(eaa)));break;
                    case 0x06:Memory.mem_writeb(eaa, XORB(ib,Memory.mem_readb(eaa)));break;
                    case 0x07:CMPB(ib,Memory.mem_readb(eaa));break;
                    }
                }
                return HANDLED;
            }
        };
        ops[0x280] = ops[0x80];

        /* Grpl Eb,Ib Mirror instruction*/
        ops[0x82] = ops[0x80];
        ops[0x282] = ops[0x80];

        /* Grpl Ew,Iw */
        ops[0x81] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                if (rm>= 0xc0) {
                    Reg r = Modrm.GetEArw[rm];

                    /*Bit16u*/int iw=Fetchw();
                    switch (which) {
                    case 0x00:r.word(ADDW(iw,r.word()));break;
                    case 0x01: r.word(ORW(iw,r.word()));break;
                    case 0x02:r.word(ADCW(iw,r.word()));break;
                    case 0x03:r.word(SBBW(iw,r.word()));break;
                    case 0x04:r.word(ANDW(iw,r.word()));break;
                    case 0x05:r.word(SUBW(iw,r.word()));break;
                    case 0x06:r.word(XORW(iw,r.word()));break;
                    case 0x07:CMPW(iw,r.word());break;
                    }
                } else {
                    int eaa = getEaa(rm);
                    /*Bit16u*/int iw=Fetchw();
                    switch (which) {
                    case 0x00:Memory.mem_writew(eaa, ADDW(iw,Memory.mem_readw(eaa)));break;
                    case 0x01: Memory.mem_writew(eaa, ORW(iw,Memory.mem_readw(eaa)));break;
                    case 0x02:Memory.mem_writew(eaa, ADCW(iw,Memory.mem_readw(eaa)));break;
                    case 0x03:Memory.mem_writew(eaa, SBBW(iw,Memory.mem_readw(eaa)));break;
                    case 0x04:Memory.mem_writew(eaa, ANDW(iw,Memory.mem_readw(eaa)));break;
                    case 0x05:Memory.mem_writew(eaa, SUBW(iw,Memory.mem_readw(eaa)));break;
                    case 0x06:Memory.mem_writew(eaa, XORW(iw,Memory.mem_readw(eaa)));break;
                    case 0x07:CMPW(iw,Memory.mem_readw(eaa));break;
                    }
                }
                return HANDLED;
            }
        };

        /* Grpl Ew,Ix */
        ops[0x83] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                if (rm>= 0xc0) {
                    // reading 240 as an unsigned byte will get returned as -16 and when that is cast to unsigned short it becomes 65520
                    Reg r = Modrm.GetEArw[rm];
                    /*Bit16u*/int iw=(((short)Fetchbs()) & 0xFFFF);
                    switch (which) {
                    case 0x00:r.word(ADDW(iw,r.word()));break;
                    case 0x01: r.word(ORW(iw,r.word()));break;
                    case 0x02:r.word(ADCW(iw,r.word()));break;
                    case 0x03:r.word(SBBW(iw,r.word()));break;
                    case 0x04:r.word(ANDW(iw,r.word()));break;
                    case 0x05:r.word(SUBW(iw,r.word()));break;
                    case 0x06:r.word(XORW(iw,r.word()));break;
                    case 0x07:CMPW(iw,r.word());break;
                    }
                } else {
                    int eaa = m = getEaa(rm);

                    /*Bit16u*/int iw=(((short)Fetchbs()) & 0xFFFF);
                    switch (which) {
                    case 0x00:Memory.mem_writew(eaa, ADDW(iw,Memory.mem_readw(eaa)));break;
                    case 0x01: Memory.mem_writew(eaa, ORW(iw,Memory.mem_readw(eaa)));break;
                    case 0x02:Memory.mem_writew(eaa, ADCW(iw,Memory.mem_readw(eaa)));break;
                    case 0x03:Memory.mem_writew(eaa, SBBW(iw,Memory.mem_readw(eaa)));break;
                    case 0x04:Memory.mem_writew(eaa, ANDW(iw,Memory.mem_readw(eaa)));break;
                    case 0x05:Memory.mem_writew(eaa, SUBW(iw,Memory.mem_readw(eaa)));break;
                    case 0x06:Memory.mem_writew(eaa, XORW(iw,Memory.mem_readw(eaa)));break;
                    case 0x07:CMPW(iw,Memory.mem_readw(eaa));break;
                    }
                }
                return HANDLED;
            }
        };

        /* TEST Eb,Gb */
        ops[0x84] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    TESTB(Modrm.Getrb[rm].get(),Modrm.GetEArb[rm].get());
                }
                else {
                    int eaa = getEaa(rm);
                    TESTB(Modrm.Getrb[rm].get(),Memory.mem_readb(eaa));
                }
                return HANDLED;
            }
        };
        ops[0x284] = ops[0x84];

        /* TEST Ew,Gw */
        ops[0x85] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    TESTW(Modrm.Getrw[rm].word(),Modrm.GetEArw[rm].word());
                }
                else {
                    int eaa = getEaa(rm);
                    TESTW(Modrm.Getrw[rm].word(),Memory.mem_readw(eaa));
                }
                return HANDLED;
            }
        };

        /* XCHG Eb,Gb */
        ops[0x86] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();/*Bit8u*/int oldrmrb=Modrm.Getrb[rm].get();
                if (rm >= 0xc0 ) {Modrm.Getrb[rm].set(Modrm.GetEArb[rm].get());Modrm.GetEArb[rm].set(oldrmrb);}
                else {
                    /*PhysPt*/int eaa=getEaa(rm);
                    int val = Memory.mem_readb(eaa);
                    Memory.mem_writeb(eaa,oldrmrb);
                    Modrm.Getrb[rm].set(val);
                }
                return HANDLED;
            }
        };
        ops[0x286] = ops[0x86];

        /* XCHG Ew,Gw */
        ops[0x87] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    Reg rw = Modrm.Getrw[rm];
                    Reg ea = Modrm.GetEArw[rm];
                    /*Bit16u*/int oldrmrw=rw.word();
                    rw.word(ea.word());ea.word(oldrmrw);
                } else {
                    /*Bit16u*/int oldrmrw=Modrm.Getrw[rm].word();
                    /*PhysPt*/int eaa=getEaa(rm);
                    int val = Memory.mem_readw(eaa);
                    Memory.mem_writew(eaa,oldrmrw);
                    Modrm.Getrw[rm].word(val);
                }
                return HANDLED;
            }
        };

        /* MOV Eb,Gb */
        ops[0x88] = new OP() {
            final public int call() {
                int rm = Fetchb();
                if (rm >= 0xc0) {
                    int value;
                    switch ((rm >> 3) & 7) {
                        case 0: value = reg_eax.low(); break;
                        case 1: value = reg_ecx.low(); break;
                        case 2: value = reg_edx.low(); break;
                        case 3: value = reg_ebx.low(); break;
                        case 4: value = reg_eax.high(); break;
                        case 5: value = reg_ecx.high(); break;
                        case 6: value = reg_edx.high(); break;
                        case 7: value = reg_ebx.high(); break;
                        default: value = 0; // shouldn't be possible
                    }
                    switch (rm & 7) {
                        case 0: reg_eax.low(value); break;
                        case 1: reg_ecx.low(value); break;
                        case 2: reg_edx.low(value); break;
                        case 3: reg_ebx.low(value); break;
                        case 4: reg_eax.high(value); break;
                        case 5: reg_ecx.high(value); break;
                        case 6: reg_edx.high(value); break;
                        case 7: reg_ebx.high(value); break;
                    }
                } else {
                    if (rm == 5) {
                        if (CPU.cpu.pmode && !CPU.cpu.code.big) {
                            CPU.cpu.gdt.GetDescriptor(CPU.seg_value(base_val_ds),desc);
                            if ((desc.Type()==CPU.DESC_CODE_R_NC_A) || (desc.Type()==CPU.DESC_CODE_R_NC_NA)) {
                                CPU.CPU_Exception(CPU.EXCEPTION_GP,CPU.seg_value(base_val_ds) & 0xfffc);
                                return CONTINUE;
                            }
                        }
                    }
                    int eaa = getEaa(rm);

                    switch ((rm >> 3) & 7) {
                        case 0: Memory.mem_writeb(eaa, reg_eax.low()); break;
                        case 1: Memory.mem_writeb(eaa, reg_ecx.low()); break;
                        case 2: Memory.mem_writeb(eaa, reg_edx.low()); break;
                        case 3: Memory.mem_writeb(eaa, reg_ebx.low()); break;
                        case 4: Memory.mem_writeb(eaa, reg_eax.high()); break;
                        case 5: Memory.mem_writeb(eaa, reg_ecx.high()); break;
                        case 6: Memory.mem_writeb(eaa, reg_edx.high()); break;
                        case 7: Memory.mem_writeb(eaa, reg_ebx.high()); break;
                    }
                }
                return HANDLED;
            }
        };
        ops[0x288] = ops[0x88];

        /* MOV Ew,Gw */
        ops[0x89] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0 ) {Modrm.GetEArw[rm].word(Modrm.Getrw[rm].word());}
                else {/*PhysPt*/int eaa=getEaa(rm);Memory.mem_writew(eaa,Modrm.Getrw[rm].word());}
                return HANDLED;
            }
        };

        /* MOV Gb,Eb */
        ops[0x8a] = new OP() {
            final public int call() {
                int rm = Fetchb();

                if (rm >= 0xc0) {
                    int value;
                    switch (rm & 7) {
                        case 0: value = reg_eax.low(); break;
                        case 1: value = reg_ecx.low(); break;
                        case 2: value = reg_edx.low(); break;
                        case 3: value = reg_ebx.low(); break;
                        case 4: value = reg_eax.high(); break;
                        case 5: value = reg_ecx.high(); break;
                        case 6: value = reg_edx.high(); break;
                        case 7: value = reg_ebx.high(); break;
                        default: value = 0; // shouldn't be possible
                    }
                    switch ((rm >> 3) & 7) {
                        case 0: reg_eax.low(value); break;
                        case 1: reg_ecx.low(value); break;
                        case 2: reg_edx.low(value); break;
                        case 3: reg_ebx.low(value); break;
                        case 4: reg_eax.high(value); break;
                        case 5: reg_ecx.high(value); break;
                        case 6: reg_edx.high(value); break;
                        case 7: reg_ebx.high(value); break;
                    }
                } else {
                    int eaa = getEaa(rm);
                    switch ((rm >> 3) & 7) {
                        case 0: reg_eax.low(Memory.mem_readb(eaa)); break;
                        case 1: reg_ecx.low(Memory.mem_readb(eaa)); break;
                        case 2: reg_edx.low(Memory.mem_readb(eaa)); break;
                        case 3: reg_ebx.low(Memory.mem_readb(eaa)); break;
                        case 4: reg_eax.high(Memory.mem_readb(eaa)); break;
                        case 5: reg_ecx.high(Memory.mem_readb(eaa)); break;
                        case 6: reg_edx.high(Memory.mem_readb(eaa)); break;
                        case 7: reg_ebx.high(Memory.mem_readb(eaa)); break;
                    }
                }
                return HANDLED;
            }
        };
        ops[0x28a] = ops[0x8a];

        /* MOV Gw,Ew */
        ops[0x8b] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0 )Modrm.Getrw[rm].word(Modrm.GetEArw[rm].word());
                else {/*PhysPt*/int eaa=getEaa(rm);Modrm.Getrw[rm].word(Memory.mem_readw(eaa));}
                return HANDLED;
            }
        };

        /* Mov Ew,Sw */
        ops[0x8c] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();/*Bit16u*/int val;/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:					/* MOV Ew,ES */
                    val=CPU_Regs.reg_esVal.dword;break;
                case 0x01:					/* MOV Ew,CS */
                    val=CPU_Regs.reg_csVal.dword;break;
                case 0x02:					/* MOV Ew,SS */
                    val=CPU_Regs.reg_ssVal.dword;break;
                case 0x03:					/* MOV Ew,DS */
                    val=CPU_Regs.reg_dsVal.dword;break;
                case 0x04:					/* MOV Ew,FS */
                    val=CPU_Regs.reg_fsVal.dword;break;
                case 0x05:					/* MOV Ew,GS */
                    val=CPU_Regs.reg_gsVal.dword;break;
                default:
                    Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"CPU:8c:Illegal RM Byte");
                    return ILLEGAL_OPCODE;
                }
                if (rm >= 0xc0 ) {Modrm.GetEArw[rm].word(val);}
                else {/*PhysPt*/int eaa=getEaa(rm);Memory.mem_writew(eaa,val);}
                return HANDLED;
            }
        };

        /* LEA Gw */
        ops[0x8d] = new OP() {
            final public int call() {
                //Little hack to always use segprefixed version
                base_ds=base_ss=0;
                /*Bit8u*/int rm=Fetchb();
                if (TEST_PREFIX_ADDR()!=0) {
                    Modrm.Getrw[rm].word((/*Bit16u*/int)(getEaa32(rm)));
                } else {
                    Modrm.Getrw[rm].word((/*Bit16u*/int)(getEaa16(rm)));
                }
                return HANDLED;
            }
        };

        /* MOV Sw,Ew */
        ops[0x8e] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();/*Bit16u*/int val;/*Bitu*/int which=(rm>>3)&7;
                if (rm >= 0xc0 ) {val=Modrm.GetEArw[rm].word();}
                else {/*PhysPt*/int eaa=getEaa(rm);val=Memory.mem_readw(eaa);}
                switch (which) {
                case 0x02:					/* MOV SS,Ew */
                    CPU.CPU_Cycles++; //Always do another instruction
                case 0x00:					/* MOV ES,Ew */
                case 0x03:					/* MOV DS,Ew */
                case 0x05:					/* MOV GS,Ew */
                case 0x04:					/* MOV FS,Ew */
                    if (CPU.CPU_SetSegGeneral_index(which,val)) return RUNEXCEPTION();
                    break;
                default:
                    return ILLEGAL_OPCODE;
                }
                return HANDLED;
            }
        };
        ops[0x28e] = ops[0x8e];

        /* POP Ew */
        ops[0x8f] = new OP() {
            final public int call() {
                /*Bit16u*/int val=CPU.CPU_Pop16();
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0 ) {Modrm.GetEArw[rm].word(val);}
                else {/*PhysPt*/int eaa=getEaa(rm);Memory.mem_writew(eaa,val);}
                return HANDLED;
            }
        };

        /* NOP */
        ops[0x90] = new OP() {
            final public int call() {
                return HANDLED;
            }
        };
        ops[0x290] = ops[0x90];

        /* XCHG CX,AX */
        ops[0x91] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_ecx.word());reg_ecx.word(temp);
                return HANDLED;
            }
        };

        /* XCHG DX,AX */
        ops[0x92] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_edx.word());reg_edx.word(temp);
                return HANDLED;
            }
        };

        /* XCHG BX,AX */
        ops[0x93] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_ebx.word());reg_ebx.word(temp);
                return HANDLED;
            }
        };

        /* XCHG SP,AX */
        ops[0x94] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_esp.word());reg_esp.word(temp);
                return HANDLED;
            }
        };

        /* XCHG BP,AX */
        ops[0x95] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_ebp.word());reg_ebp.word(temp);
                return HANDLED;
            }
        };

        /* XCHG SI,AX */
        ops[0x96] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_esi.word());reg_esi.word(temp);
                return HANDLED;
            }
        };

        /* XCHG DI,AX */
        ops[0x97] = new OP() {
            final public int call() {
                /*Bit16u*/int temp=reg_eax.word();reg_eax.word(reg_edi.word());reg_edi.word(temp);
                return HANDLED;
            }
        };

        /* CBW */
        ops[0x98] = new OP() {
            final public int call() {
                reg_eax.word((byte)reg_eax.low());
                return HANDLED;
            }
        };

        /* CWD */
        ops[0x99] = new OP() {
            final public int call() {
                 if ((reg_eax.word() & 0x8000)!=0) reg_edx.word(0xffff);else reg_edx.word(0);
                return HANDLED;
            }
        };

        /* CALL Ap */
        ops[0x9a] = new OP() {
            final public int call() {
                Flags.FillFlags();
                /*Bit16u*/int newip=Fetchw();/*Bit16u*/int newcs=Fetchw();
                CPU.CPU_CALL(false,newcs,newip,GETIP());
                if (CPU_TRAP_CHECK) {
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return CBRET_NONE;
                    }
                }
                return CONTINUE;
            }
        };

        /* WAIT */
        ops[0x9b] = new OP() {
            final public int call() {
                /* No waiting here */
                return HANDLED;
            }
        };
        ops[0x29b] = ops[0x9b];

        /* PUSHF */
        ops[0x9c] = new OP() {
            final public int call() {
                if (CPU.CPU_PUSHF(false)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* POPF */
        ops[0x9d] = new OP() {
            final public int call() {
                if (CPU.CPU_POPF(false)) return RUNEXCEPTION();
                if (CPU_TRAP_CHECK) {
                        if (GETFLAG(TF)!=0) {
                            CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                            return DECODE_END;
                        }
                }
                if (CPU_PIC_CHECK)
                    if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END;
                return HANDLED;
            }
        };

        /* SAHF */
        ops[0x9e] = new OP() {
            final public int call() {
                Flags.SETFLAGSb(reg_eax.high());
                return HANDLED;
            }
        };
        ops[0x29e] = ops[0x9e];

        /* LAHF */
        ops[0x9f] = new OP() {
            final public int call() {
                Flags.FillFlags();
                reg_eax.high(CPU_Regs.flags&0xff);
                return HANDLED;
            }
        };
        ops[0x29f] = ops[0x9f];

        /* MOV AL,Ob */
        ops[0xa0] = new OP() {
            final public int call() {
                reg_eax.low(Memory.mem_readb(GetEADirect()));
                return HANDLED;
            }
        };
        ops[0x2a0] = ops[0xa0];

        /* MOV AX,Ow */
        ops[0xa1] = new OP() {
            final public int call() {
                reg_eax.word(Memory.mem_readw(GetEADirect()));
                return HANDLED;
            }
        };

        /* MOV Ob,AL */
        ops[0xa2] = new OP() {
            final public int call() {
                Memory.mem_writeb(GetEADirect(),reg_eax.low());
                return HANDLED;
            }
        };
        ops[0x2a2] = ops[0xa2];

        /* MOV Ow,AX */
        ops[0xa3] = new OP() {
            final public int call() {
                Memory.mem_writew(GetEADirect(),reg_eax.word());
                return HANDLED;
            }
        };

        /* MOVSB */
        ops[0xa4] = new OP() {
            final public int call() {
                DoString(R_MOVSB);
                return HANDLED;
            }
        };
        ops[0x2a4] = ops[0xa4];

        /* MOVSW */
        ops[0xa5] = new OP() {
            final public int call() {
                DoString(R_MOVSW);
                return HANDLED;
            }
        };

        /* CMPSB */
        ops[0xa6] = new OP() {
            final public int call() {
                DoString(R_CMPSB);
                return HANDLED;
            }
        };
        ops[0x2a6] = ops[0xa6];

        /* CMPSW */
        ops[0xa7] = new OP() {
            final public int call() {
                DoString(R_CMPSW);
                return HANDLED;
            }
        };

        /* TEST AL,Ib */
        ops[0xa8] = new OP() {
            final public int call() {
                TESTB(Fetchb(),reg_eax.low());
                return HANDLED;
            }
        };
        ops[0x2a8] = ops[0xa8];

        /* TEST AX,Iw */
        ops[0xa9] = new OP() {
            final public int call() {
                TESTW(Fetchw(),reg_eax.word());
                return HANDLED;
            }
        };

        /* STOSB */
        ops[0xaa] = new OP() {
            final public int call() {
                DoString(R_STOSB);
                return HANDLED;
            }
        };
        ops[0x2aa] = ops[0xaa];

        /* STOSW */
        ops[0xab] = new OP() {
            final public int call() {
                DoString(R_STOSW);
                return HANDLED;
            }
        };

        /* LODSB */
        ops[0xac] = new OP() {
            final public int call() {
                DoString(R_LODSB);
                return HANDLED;
            }
        };
        ops[0x2ac] = ops[0xac];

        /* LODSW */
        ops[0xad] = new OP() {
            final public int call() {
                DoString(R_LODSW);
                return HANDLED;
            }
        };

        /* SCASB */
        ops[0xae] = new OP() {
            final public int call() {
                DoString(R_SCASB);
                return HANDLED;
            }
        };
        ops[0x2ae] = ops[0xae];

        /* SCASW */
        ops[0xaf] = new OP() {
            final public int call() {
                DoString(R_SCASW);
                return HANDLED;
            }
        };

        /* MOV AL,Ib */
        ops[0xb0] = new OP() {
            final public int call() {
                reg_eax.low(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b0] = ops[0xb0];

        /* MOV CL,Ib */
        ops[0xb1] = new OP() {
            final public int call() {
                reg_ecx.low(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b1] = ops[0xb1];

        /* MOV DL,Ib */
        ops[0xb2] = new OP() {
            final public int call() {
                reg_edx.low(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b2] = ops[0xb2];

        /* MOV BL,Ib */
        ops[0xb3] = new OP() {
            final public int call() {
                reg_ebx.low(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b3] = ops[0xb3];

        /* MOV AH,Ib */
        ops[0xb4] = new OP() {
            final public int call() {
                reg_eax.high(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b4] = ops[0xb4];

        /* MOV CH,Ib */
        ops[0xb5] = new OP() {
            final public int call() {
                reg_ecx.high(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b5] = ops[0xb5];

        /* MOV DH,Ib */
        ops[0xb6] = new OP() {
            final public int call() {
                reg_edx.high(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b6] = ops[0xb6];

        /* MOV BH,Ib */
        ops[0xb7] = new OP() {
            final public int call() {
                reg_ebx.high(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2b7] = ops[0xb7];

        /* MOV AX,Iw */
        ops[0xb8] = new OP() {
            final public int call() {
                reg_eax.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV CX,Iw */
        ops[0xb9] = new OP() {
            final public int call() {
                reg_ecx.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV DX,Iw */
        ops[0xba] = new OP() {
            final public int call() {
                reg_edx.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV BX,Iw */
        ops[0xbb] = new OP() {
            final public int call() {
                reg_ebx.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV SP,Iw */
        ops[0xbc] = new OP() {
            final public int call() {
                reg_esp.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV BP.Iw */
        ops[0xbd] = new OP() {
            final public int call() {
                reg_ebp.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV SI,Iw */
        ops[0xbe] = new OP() {
            final public int call() {
                reg_esi.word(Fetchw());
                return HANDLED;
            }
        };

        /* MOV DI,Iw */
        ops[0xbf] = new OP() {
            final public int call() {
                reg_edi.word(Fetchw());
                return HANDLED;
            }
        };

        /* GRP2 Eb,Ib */
        ops[0xc0] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                GRP2B_fetchb(rm);
                return HANDLED;
            }
        };
        ops[0x2c0] = ops[0xc0];

        /* GRP2 Ew,Ib */
        ops[0xc1] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                GRP2W_fetchb(rm);
                return HANDLED;
            }
        };

        /* RETN Iw */
        ops[0xc2] = new OP() {
            final public int call() {
                int offset = Fetchw();
                reg_eip=CPU.CPU_Pop16();
                reg_esp.dword+=offset;
                return CONTINUE;
            }
        };

        /* RETN */
        ops[0xc3] = new OP() {
            final public int call() {
                reg_eip=CPU.CPU_Pop16();
                return CONTINUE;
            }
        };

        /* LES */
        ops[0xc4] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/int eaa=getEaa(rm);
                int val = Memory.mem_readw(eaa); // make sure all reads are done before writing something in case of a PF
                if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
                Modrm.Getrw[rm].word(val);
                return HANDLED;
            }
        };

        /* LDS */
        ops[0xc5] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/int eaa=getEaa(rm);
                int val = Memory.mem_readw(eaa); // make sure all reads are done before writing something in case of a PF
                if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
                Modrm.Getrw[rm].word(val);
                return HANDLED;
            }
        };

        /* MOV Eb,Ib */
        ops[0xc6] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0) {Modrm.GetEArb[rm].set(Fetchb());}
                else {/*PhysPt*/int eaa=getEaa(rm);Memory.mem_writeb(eaa,Fetchb());}
                return HANDLED;
            }
        };
        ops[0x2c6] = ops[0xc6];

        /* MOV EW,Iw */
        ops[0xc7] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                if (rm >= 0xc0) {Modrm.GetEArw[rm].word(Fetchw());}
                else {/*PhysPt*/int eaa=getEaa(rm);Memory.mem_writew(eaa,Fetchw());}
                return HANDLED;
            }
        };

        /* ENTER Iw,Ib */
        ops[0xc8] = new OP() {
            final public int call() {
                /*Bitu*/int bytes=Fetchw();
                /*Bitu*/int level=Fetchb();
                CPU.CPU_ENTER(false,bytes,level);
                return HANDLED;
            }
        };

        /* LEAVE */
        ops[0xc9] = new OP() {
            final public int call() {
                reg_esp.dword&=CPU.cpu.stack.notmask;
                reg_esp.dword|=(reg_ebp.dword & CPU.cpu.stack.mask);
                reg_ebp.word(CPU.CPU_Pop16());
                return HANDLED;
            }
        };

        /* RETF Iw */
        ops[0xca] = new OP() {
            final public int call() {
                /*Bitu*/int words=Fetchw();
                Flags.FillFlags();
                CPU.CPU_RET(false,words,GETIP());
                return CONTINUE;
            }
        };

        /* RETF */
        ops[0xcb] = new OP() {
            final public int call() {
                Flags.FillFlags();
                CPU.CPU_RET(false,0,GETIP());
                return CONTINUE;
            }
        };

        /* INT3 */
        ops[0xcc] = new OP() {
            final public int call() {
                if (Config.C_DEBUG) {
                    Flags.FillFlags();
                    if (Debug.DEBUG_Breakpoint())
                        return Debug.debugCallback;
                }
                CPU.CPU_SW_Interrupt_NoIOPLCheck(3,GETIP());
                if (CPU_TRAP_CHECK)
                    CPU.cpu.trap_skip=true;
                return CONTINUE;
            }
        };
        ops[0x2cc] = ops[0xcc];

        /* INT Ib */
        ops[0xcd] = new OP() {
            final public int call() {
                /*Bit8u*/int num=Fetchb();
                if (Config.C_DEBUG) {
                    Flags.FillFlags();
                    if (Debug.DEBUG_IntBreakpoint(num)) {
                        returnValue = Debug.debugCallback;
                        return RETURN;
                    }
                }
                CPU.CPU_SW_Interrupt(num,GETIP());
                if (CPU_TRAP_CHECK)
                    CPU.cpu.trap_skip=true;
                return CONTINUE;
            }
        };
        ops[0x2cd] = ops[0xcd];

        /* INTO */
        ops[0xce] = new OP() {
            final public int call() {
                if (Flags.get_OF()) {
                    CPU.CPU_SW_Interrupt(4,GETIP());
                    if (CPU_TRAP_CHECK)
                        CPU.cpu.trap_skip=true;
                    return CONTINUE;
                }
                return HANDLED;
            }
        };
        ops[0x2ce] = ops[0xce];

        /* IRET */
        ops[0xcf] = new OP() {
            final public int call() {
                CPU.CPU_IRET(false,GETIP());
                if (CPU_TRAP_CHECK) {
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return CBRET_NONE;
                    }
                }
                if (CPU_PIC_CHECK)
                    if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return CBRET_NONE;
                return CONTINUE;
            }
        };

        /* GRP2 Eb,1 */
        ops[0xd0] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                GRP2B(rm,1);
                return HANDLED;
            }
        };
        ops[0x2d0] = ops[0xd0];

        /* GRP2 Ew,1 */
        ops[0xd1] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                GRP2W(rm, 1);
                return HANDLED;
            }
        };

        /* GRP2 Eb,CL */
        ops[0xd2] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                GRP2B(rm, reg_ecx.low());
                return HANDLED;
            }
        };
        ops[0x2d2] = ops[0xd2];

        /* GRP2 Ew,CL */
        ops[0xd3] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                GRP2W(rm, reg_ecx.low());
                return HANDLED;
            }
        };

        /* AAM Ib */
        ops[0xd4] = new OP() {
          final public int call() {
                if (!Instructions.AAM(Fetchb())) return RUNEXCEPTION();
                return HANDLED;
            }
        };
        ops[0x2d4] = ops[0xd4];

        /* AAD Ib */
        ops[0xd5] = new OP() {
            final public int call() {
                Instructions.AAD(Fetchb());
                return HANDLED;
            }
        };
        ops[0x2d5] = ops[0xd5];

        /* SALC */
        ops[0xd6] = new OP() {
            final public int call() {
                reg_eax.low(Flags.get_CF() ? 0xFF : 0);
                return HANDLED;
            }
        };
        ops[0x2d6] = ops[0xd6];

        /* XLAT */
        ops[0xd7] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    reg_eax.low(Memory.mem_readb(base_ds+(reg_ebx.dword+reg_eax.low())));
                } else {
                    reg_eax.low(Memory.mem_readb(base_ds+((reg_ebx.word()+reg_eax.low()) & 0xFFFF)));
                }
                return HANDLED;
            }
        };
        ops[0x2d7] = ops[0xd7];

        /* FPU ESC 0 */
        ops[0xd8] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC0_Normal(rm);
                    } else {
                        FPU.FPU_ESC0_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2d8] = ops[0xd8];

        /* FPU ESC 1 */
        ops[0xd9] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC1_Normal(rm);
                    } else {
                        FPU.FPU_ESC1_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2d9] = ops[0xd9];

        /* FPU ESC 2 */
        ops[0xda] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC2_Normal(rm);
                    } else {
                        FPU.FPU_ESC2_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2da] = ops[0xda];

        /* FPU ESC 3 */
        ops[0xdb] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC3_Normal(rm);
                    } else {
                        FPU.FPU_ESC3_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2db] = ops[0xdb];

        /* FPU ESC 4 */
        ops[0xdc] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC4_Normal(rm);
                    } else {
                        FPU.FPU_ESC4_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2dc] = ops[0xdc];

        /* FPU ESC 5 */
        ops[0xdd] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC5_Normal(rm);
                    } else {
                        FPU.FPU_ESC5_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2dd] = ops[0xdd];

        /* FPU ESC 6 */
        ops[0xde] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC6_Normal(rm);
                    } else {
                        FPU.FPU_ESC6_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2de] = ops[0xde];

        /* FPU ESC 7 */
        ops[0xdf] = new OP() {
            final public int call() {
                if (Config.C_FPU)
                {
                    /*Bit8u*/int rm=Fetchb();
                    if (rm >= 0xc0) {
                        FPU.FPU_ESC7_Normal(rm);
                    } else {
                        FPU.FPU_ESC7_EA(rm,getEaa(rm));
                    }
                } else {
                    /*Bit8u*/int rm=Fetchb();
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"FPU used");
                    if (rm<0xc0) getEaa(rm);
                    return NOT_HANDLED;
                }
                return HANDLED;
            }
        };
        ops[0x2df] = ops[0xdf];

        /* LOOPNZ */
        ops[0xe0] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    JumpCond16_b(reg_ecx.dword-1!=0 && !Flags.get_ZF());
                    reg_ecx.dword--;
                } else {
                    JumpCond16_b(reg_ecx.word()-1!=0 && !Flags.get_ZF());
                    reg_ecx.word(reg_ecx.word()-1);
                }
                return CONTINUE;
            }
        };

        /* LOOPZ */
        ops[0xe1] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    JumpCond16_b(reg_ecx.dword-1!=0 && Flags.get_ZF());
                    reg_ecx.dword--;
                } else {
                    JumpCond16_b(reg_ecx.word()-1!=0 && Flags.get_ZF());
                    reg_ecx.word(reg_ecx.word()-1);
                }
                return CONTINUE;
            }
        };

        /* LOOP */
        ops[0xe2] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    JumpCond16_b(reg_ecx.dword-1!=0);
                    reg_ecx.dword--;
                } else {
                    JumpCond16_b(reg_ecx.word()-1!=0);
                    reg_ecx.word(reg_ecx.word()-1);
                }
                return CONTINUE;
            }
        };

        /* JCXZ */
        ops[0xe3] = new OP() {
            final public int call() {
                JumpCond16_b((reg_ecx.dword & AddrMaskTable1[prefixes& PREFIX_ADDR])==0);
                return CONTINUE;
            }
        };

        /* IN AL,Ib */
        ops[0xe4] = new OP() {
            final public int call() {
                /*Bitu*/int port=Fetchb();
                if (CPU.CPU_IO_Exception(port,1)) return RUNEXCEPTION();
                reg_eax.low(IO.IO_ReadB(port));
                return HANDLED;
            }
        };
        ops[0x2e4] = ops[0xe4];

        /* IN AX,Ib */
        ops[0xe5] = new OP() {
            final public int call() {
                /*Bitu*/int port=Fetchb();
                if (CPU.CPU_IO_Exception(port,2)) return RUNEXCEPTION();
                reg_eax.word(IO.IO_ReadW(port));
                return HANDLED;
            }
        };

        /* OUT Ib,AL */
        ops[0xe6] = new OP() {
            final public int call() {
                /*Bitu*/int port=Fetchb();
                if (CPU.CPU_IO_Exception(port,1)) return RUNEXCEPTION();
                IO.IO_WriteB(port,reg_eax.low());
                return HANDLED;
            }
        };
        ops[0x2e6] = ops[0xe6];

        /* OUT Ib,AX */
        ops[0xe7] = new OP() {
            final public int call() {
                /*Bitu*/int port=Fetchb();
                if (CPU.CPU_IO_Exception(port,2)) return RUNEXCEPTION();
                IO.IO_WriteW(port,reg_eax.word());
                return HANDLED;
            }
        };

        /* CALL Jw */
        ops[0xe8] = new OP() {
            final public int call() {
                /*Bit16u*/int addip=Fetchws();
                CPU.CPU_Push16(GETIP() & 0xFFFF);
                SAVEIP();
                reg_eip=(reg_eip+addip) & 0xFFFF;
                return CONTINUE;
            }
        };

        /* JMP Jw */
        ops[0xe9] = new OP() {
            final public int call() {
                /*Bit16u*/int addip=Fetchws();
                SAVEIP();
                reg_eip=(reg_eip+addip) & 0xFFFF;
                return CONTINUE;
            }
        };

        /* JMP Ap */
        ops[0xea] = new OP() {
            final public int call() {
                /*Bit16u*/int newip=Fetchw();
                /*Bit16u*/int newcs=Fetchw();
                Flags.FillFlags();
                CPU.CPU_JMP(false,newcs,newip,GETIP());
                if (CPU_TRAP_CHECK) {
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return CBRET_NONE;
                    }
                }
                return CONTINUE;
            }
        };

        /* JMP Jb */
        ops[0xeb] = new OP() {
            final public int call() {
                /*Bit16s*/int addip=Fetchbs();
                SAVEIP();
                reg_eip=(reg_eip+addip) & 0xFFFF;
                return CONTINUE;
            }
        };

        /* IN AL,DX */
        ops[0xec] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
                reg_eax.low(IO.IO_ReadB(reg_edx.word()));
                return HANDLED;
            }
        };
        ops[0x2ec] = ops[0xec];

        /* IN AX,DX */
        ops[0xed] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
                reg_eax.word(IO.IO_ReadW(reg_edx.word()));
                return HANDLED;
            }
        };

        /* OUT DX,AL */
        ops[0xee] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),1)) return RUNEXCEPTION();
                IO.IO_WriteB(reg_edx.word(),reg_eax.low());
                return HANDLED;
            }
        };
        ops[0x2ee] = ops[0xee];

        /* OUT DX,AX */
        ops[0xef] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),2)) return RUNEXCEPTION();
                IO.IO_WriteW(reg_edx.word(),reg_eax.word());
                return HANDLED;
            }
        };

        /* LOCK */
        ops[0xf0] = new OP() {
            final public int call() {
                //if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_NORMAL,"CPU:LOCK"); /* FIXME: see case D_LOCK in core_full/new Instructions.load()h */
                prefixes|=PREFIX_LOCK;
                return RESTART;
            }
        };
        ops[0x2f0] = ops[0xf0];

        /* ICEBP */
        ops[0xf1] = new OP() {
            final public int call() {
                CPU.CPU_SW_Interrupt_NoIOPLCheck(1,GETIP());
                if (CPU_TRAP_CHECK)
                    CPU.cpu.trap_skip=true;
                return CONTINUE;
            }
        };
        ops[0x2f1] = ops[0xf1];

        /* REPNZ */
        ops[0xf2] = new OP() {
            final public int call() {
                prefixes|=PREFIX_REP;
                rep_zero=false;
                return RESTART;
            }
        };
        ops[0x2f2] = ops[0xf2];

        /* REPZ */
        ops[0xf3] = new OP() {
            final public int call() {
                prefixes|=PREFIX_REP;
                rep_zero=true;
                return RESTART;
            }
        };
        ops[0x2f3] = ops[0xf3];

        /* HLT */
        ops[0xf4] = new OP() {
            final public int call() {
                if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                Flags.FillFlags();
                CPU.CPU_HLT(GETIP());
                return CBRET_NONE;
            }
        };
        ops[0x2f4] = ops[0xf4];

        /* CMC */
        ops[0xf5] = new OP() {
            final public int call() {
                Flags.FillFlags();
                SETFLAGBIT(CF,(CPU_Regs.flags & CPU_Regs.CF)==0);
                return HANDLED;
            }
        };
        ops[0x2f5] = ops[0xf5];

        /* GRP3 Eb(,Ib) */
        ops[0xf6] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:											/* TEST Eb,Ib */
                case 0x01:											/* TEST Eb,Ib Undocumented*/
                    {
                        if (rm >= 0xc0 ) {
                            TESTB(Fetchb(),Modrm.GetEArb[rm].get());
                        }
                        else {
                            final int val = Memory.mem_readb(getEaa(rm));
                            TESTB(Fetchb(),val);
                        }
                        break;
                    }
                case 0x02:											/* NOT Eb */
                    {
                        if (rm >= 0xc0 ) {Modrm.GetEArb[rm].set(~Modrm.GetEArb[rm].get());}
                        else {
                            /*PhysPt*/int eaa=getEaa(rm);
                            Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));
                        }
                        break;
                    }
                case 0x03:											/* NEG Eb */
                    {
                        Flags.type=Flags.t_NEGb;
                        if (rm >= 0xc0 ) {
                            Flags.lf_var1b(Modrm.GetEArb[rm].get());Flags.lf_resb(0-Flags.lf_var1b());
                            Modrm.GetEArb[rm].set(Flags.lf_resb());
                        } else {
                            /*PhysPt*/int eaa=getEaa(rm);Flags.lf_var1b(Memory.mem_readb(eaa));Flags.lf_resb(0-Flags.lf_var1b());
                             Memory.mem_writeb(eaa,Flags.lf_resb());
                        }
                        break;
                    }
                case 0x04:											/* MUL AL,Eb */
                    if (rm >= 0xc0 ) {
                        MULB(Modrm.GetEArb[rm].get());
                    }
                    else {
                        int eaa = getEaa(rm);
                        MULB(Memory.mem_readb(eaa));
                    }
                    break;
                case 0x05:											/* IMUL AL,Eb */
                    if (rm >= 0xc0 ) {
                        r = rm;
                        IMULB(Modrm.GetEArb[rm].get());
                    }
                    else {
                        int eaa = getEaa(rm);
                        IMULB(Memory.mem_readb(eaa));
                    }
                    break;
                case 0x06:											/* DIV Eb */
                    if (rm >= 0xc0 ) {
                        if (!DIVB(Modrm.GetEArb[rm].get())) return RUNEXCEPTION();
                    }
                    else {
                        int eaa = getEaa(rm);
                        if (!DIVB(Memory.mem_readb(eaa))) return RUNEXCEPTION();
                    }
                    break;
                case 0x07:											/* IDIV Eb */
                    if (rm >= 0xc0 ) {
                        if (!IDIVB(Modrm.GetEArb[rm].get())) return RUNEXCEPTION();
                    }
                    else {
                        int eaa = getEaa(rm);
                        if (!IDIVB(Memory.mem_readb(eaa))) return RUNEXCEPTION();
                    }
                    break;
                }
                return HANDLED;
            }
        };
        ops[0x2f6] = ops[0xf6];

        /* GRP3 Ew(,Iw) */
        ops[0xf7] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:											/* TEST Ew,Iw */
                case 0x01:											/* TEST Ew,Iw Undocumented*/
                    {
                        if (rm >= 0xc0 ) {
                            int op = Fetchw();
                            TESTW(op,Modrm.GetEArw[rm].word());
                        }
                        else {
                            final int eaa = getEaa(rm);
                            TESTW(Fetchw(),Memory.mem_readw(eaa));
                        }
                        break;
                    }
                case 0x02:											/* NOT Ew */
                    {
                        if (rm >= 0xc0 ) {Modrm.GetEArw[rm].word(~Modrm.GetEArw[rm].word());}
                        else {
                            /*PhysPt*/int eaa=getEaa(rm);
                            Memory.mem_writew(eaa,~Memory.mem_readw(eaa));
                        }
                        break;
                    }
                case 0x03:											/* NEG Ew */
                    {
                        Flags.type=Flags.t_NEGw;
                        if (rm >= 0xc0 ) {
                            Flags.lf_var1w(Modrm.GetEArw[rm].word());Flags.lf_resw(0-Flags.lf_var1w());
                            Modrm.GetEArw[rm].word(Flags.lf_resw());
                        } else {
                            /*PhysPt*/int eaa=getEaa(rm);
                            Flags.lf_var1w(Memory.mem_readw(eaa));
                            Flags.lf_resw(0-Flags.lf_var1w());
                            Memory.mem_writew(eaa,Flags.lf_resw());
                        }
                        break;
                    }
                case 0x04:											/* MUL AX,Ew */
                    if (rm >= 0xc0 ) {
                        MULW(Modrm.GetEArw[rm].word());
                    }
                    else {
                        int eaa = getEaa(rm);
                        MULW(Memory.mem_readw(eaa));
                    }
                    break;
                case 0x05:											/* IMUL AX,Ew */
                    if (rm >= 0xc0 ) {
                        IMULW(Modrm.GetEArw[rm].word());
                    }
                    else {
                        int eaa = getEaa(rm);
                        IMULW(Memory.mem_readw(eaa));
                    }
                    break;
                case 0x06:											/* DIV Ew */
                    if (rm >= 0xc0 ) {
                        if (!DIVW(Modrm.GetEArw[rm].word())) return RUNEXCEPTION();
                    }
                    else {
                        int eaa = getEaa(rm);
                        if (!DIVW(Memory.mem_readw(eaa))) return RUNEXCEPTION();
                    }
                    break;
                case 0x07:											/* IDIV Ew */
                    if (rm >= 0xc0 ) {
                        if (!IDIVW(Modrm.GetEArw[rm].word())) return RUNEXCEPTION();
                    }
                    else {
                        int eaa = getEaa(rm);
                        if (!IDIVW(Memory.mem_readw(eaa))) return RUNEXCEPTION();
                    }
                    break;
                }
                return HANDLED;
            }
        };

        /* CLC */
        ops[0xf8] = new OP() {
            final public int call() {
                Flags.FillFlags();
                SETFLAGBIT(CF,false);
                return HANDLED;
            }
        };
        ops[0x2f8] = ops[0xf8];

        /* STC */
        ops[0xf9] = new OP() {
            final public int call() {
                Flags.FillFlags();
                SETFLAGBIT(CF,true);
                return HANDLED;
            }
        };
        ops[0x2f9] = ops[0xf9];

        /* CLI */
        ops[0xfa] = new OP() {
            final public int call() {
                if (CPU.CPU_CLI()) return RUNEXCEPTION();
                return HANDLED;
            }
        };
        ops[0x2fa] = ops[0xfa];

        /* STI */
        ops[0xfb] = new OP() {
            final public int call() {
                if (CPU.CPU_STI()) return RUNEXCEPTION();
                if (CPU_PIC_CHECK)
                    if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END;
                return HANDLED;
            }
        };
        ops[0x2fb] = ops[0xfb];

        /* CLD */
        ops[0xfc] = new OP() {
            final public int call() {
                SETFLAGBIT(DF,false);
                CPU.cpu.direction=1;
                return HANDLED;
            }
        };
        ops[0x2fc] = ops[0xfc];

        /* STD */
        ops[0xfd] = new OP() {
            final public int call() {
                SETFLAGBIT(DF,true);
                CPU.cpu.direction=-1;
                return HANDLED;
            }
        };
        ops[0x2fd] = ops[0xfd];

        /* GRP4 Eb */
        ops[0xfe] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();
                /*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:										/* INC Eb */
                    if (rm >= 0xc0 ) {
                        switch (rm & 0x7) {
                            case 0: reg_eax.low(INCB(reg_eax.low()));break;
                            case 1: reg_ecx.low(INCB(reg_ecx.low()));break;
                            case 2: reg_edx.low(INCB(reg_edx.low()));break;
                            case 3: reg_ebx.low(INCB(reg_ebx.low()));break;
                            case 4: reg_eax.high(INCB(reg_eax.high()));break;
                            case 5: reg_ecx.high(INCB(reg_ecx.high()));break;
                            case 6: reg_edx.high(INCB(reg_edx.high()));break;
                            case 7: reg_ebx.high(INCB(reg_ebx.high()));break;
                        }
                    }
                    else {
                        int eaa = getEaa(rm);
                        Memory.mem_writeb(eaa, INCB(Memory.mem_readb(eaa)));
                    }
                    break;
                case 0x01:										/* DEC Eb */
                    if (rm >= 0xc0 ) {
                        switch (rm & 0x7) {
                            case 0: reg_eax.low(DECB(reg_eax.low()));break;
                            case 1: reg_ecx.low(DECB(reg_ecx.low()));break;
                            case 2: reg_edx.low(DECB(reg_edx.low()));break;
                            case 3: reg_ebx.low(DECB(reg_ebx.low()));break;
                            case 4: reg_eax.high(DECB(reg_eax.high()));break;
                            case 5: reg_ecx.high(DECB(reg_ecx.high()));break;
                            case 6: reg_edx.high(DECB(reg_edx.high()));break;
                            case 7: reg_ebx.high(DECB(reg_ebx.high()));break;
                        }
                    }
                    else {
                        int eaa = getEaa(rm);
                        Memory.mem_writeb(eaa, DECB(Memory.mem_readb(eaa)));
                    }
                    break;
                case 0x07:										/* CallBack */
                    {
                        returnValue=Fetchw();
                        Flags.FillFlags();
                        SAVEIP();
                        return RETURN;
                    }
                default:
                    Log.exit("Illegal GRP4 Call "+((rm>>3) & 7));
                    break;
                }
                return HANDLED;
            }
        };
        ops[0x2fe] = ops[0xfe];

        /* GRP5 Ew */
        ops[0xff] = new OP() {
            final public int call() {
                /*Bit8u*/int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:										/* INC Ew */
                    if (rm >= 0xc0 ) {
                        //Modrm.GetEArw[rm].word(INCW(Modrm.GetEArw[rm].word()));
                        switch (rm & 0x7) {
                            case 0: reg_eax.word(INCW(reg_eax.word()));break;
                            case 1: reg_ecx.word(INCW(reg_ecx.word()));break;
                            case 2: reg_edx.word(INCW(reg_edx.word()));break;
                            case 3: reg_ebx.word(INCW(reg_ebx.word()));break;
                            case 4: reg_esp.word(INCW(reg_esp.word()));break;
                            case 5: reg_ebp.word(INCW(reg_ebp.word()));break;
                            case 6: reg_esi.word(INCW(reg_esi.word()));break;
                            case 7: reg_edi.word(INCW(reg_edi.word()));break;
                        }
                    }
                    else {
                        int eaa = getEaa(rm);
                        Memory.mem_writew(eaa, INCW(Memory.mem_readw(eaa)));
                    }
                    break;
                case 0x01:										/* DEC Ew */
                    if (rm >= 0xc0 ) {
                        Reg r = Modrm.GetEArw[rm];
                        r.word(DECW(r.word()));
                    }
                    else {
                        int eaa = getEaa(rm);
                        Memory.mem_writew(eaa, DECW(Memory.mem_readw(eaa)));
                    }
                    break;
                case 0x02:										/* CALL Ev */
                {
                    int eip;
                    if (rm >= 0xc0 ) {eip=Modrm.GetEArw[rm].word();}
                    else {/*PhysPt*/int eaa=getEaa(rm);eip=Memory.mem_readw(eaa);}
                    CPU.CPU_Push16(GETIP() & 0xFFFF);
                    reg_eip = eip;
                    return CONTINUE;
                }
                case 0x03:										/* CALL Ep */
                    {
                        if (rm >= 0xc0) return ILLEGAL_OPCODE;
                        /*PhysPt*/int eaa=getEaa(rm);
                        /*Bit16u*/int newip=Memory.mem_readw(eaa);
                        /*Bit16u*/int newcs=Memory.mem_readw(eaa+2);
                        Flags.FillFlags();
                        CPU.CPU_CALL(false,newcs,newip,GETIP());
                        if (CPU_TRAP_CHECK) {
                            if (GETFLAG(TF)!=0) {
                                CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                                return CBRET_NONE;
                            }
                        }
                        return CONTINUE;
                    }
                case 0x04:										/* JMP Ev */
                    if (rm >= 0xc0 ) {reg_eip=Modrm.GetEArw[rm].word();}
                    else {/*PhysPt*/int eaa=getEaa(rm);reg_eip=Memory.mem_readw(eaa);}
                    return CONTINUE;
                case 0x05:										/* JMP Ep */
                    {
                        if (rm >= 0xc0) return ILLEGAL_OPCODE;
                        /*PhysPt*/int eaa=getEaa(rm);
                        /*Bit16u*/int newip=Memory.mem_readw(eaa);
                        /*Bit16u*/int newcs=Memory.mem_readw(eaa+2);
                        Flags.FillFlags();
                        CPU.CPU_JMP(false,newcs,newip,GETIP());
                        if (CPU_TRAP_CHECK) {
                            if (GETFLAG(TF)!=0) {
                                CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                                return CBRET_NONE;
                            }
                        }
                        return CONTINUE;
                    }
                case 0x06:										/* PUSH Ev */
                    if (rm >= 0xc0 ) {CPU.CPU_Push16(Modrm.GetEArw[rm].word());}
                    else {/*PhysPt*/int eaa=getEaa(rm);CPU.CPU_Push16(Memory.mem_readw(eaa));}
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"CPU:GRP5:Illegal Call "+Integer.toString(which,16));
                    return ILLEGAL_OPCODE;
                }
                return HANDLED;
            }
        };
    }
}
