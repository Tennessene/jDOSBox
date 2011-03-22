package jdos.cpu.core_normal;

import jdos.cpu.*;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;
import jdos.util.LongRef;

public class Prefix_0f extends Prefix_none {
    static final private /*Bitu*/IntRef int_ref_1=new IntRef(0);
    static final private /*Bitu*/LongRef long_ref_1=new LongRef(0);
    static {
        /* GRP 6 Exxx */
        ops[0x100] = new OP() {
            final public int call() {
                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                /*Bit8u*/short rm=Fetchb.call();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:	/* SLDT */
                case 0x01:	/* STR */
                    {
                        /*Bitu*/int saveval;
                        if (which==0) saveval=CPU.CPU_SLDT();
                        else saveval=CPU.CPU_STR();
                        if (rm >= 0xc0) {Modrm.GetEArw[rm].word(saveval);}
                        else {/*PhysPt*/long eaa=ea_table[rm].call();Memory.mem_writew(eaa,saveval);}
                    }
                    break;
                case 0x02:case 0x03:case 0x04:case 0x05:
                    {
                        /*Bitu*/int loadval;
                        if (rm >= 0xc0 ) {loadval=Modrm.GetEArw[rm].word();}
                        else {/*PhysPt*/long eaa=ea_table[rm].call();loadval=Memory.mem_readw(eaa);}
                        switch (which) {
                        case 0x02:
                            if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                            if (CPU.CPU_LLDT(loadval)) return RUNEXCEPTION();
                            break;
                        case 0x03:
                            if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                            if (CPU.CPU_LTR(loadval)) return RUNEXCEPTION();
                            break;
                        case 0x04:
                            CPU.CPU_VERR(loadval);
                            break;
                        case 0x05:
                            CPU.CPU_VERW(loadval);
                            break;
                        }
                    }
                    break;
                default:
                    return ILLEGAL_OPCODE;
                }
                return HANDLED;
            }
        };

        /* Group 7 Ew */
        ops[0x101] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();/*Bitu*/int which=(rm>>3)&7;
                if (rm < 0xc0)	{ //First ones all use EA
                    /*PhysPt*/long eaa=ea_table[rm].call();/*Bitu*/int limit;
                    switch (which) {
                    case 0x00:										/* SGDT */
                        Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());
                        Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());
                        break;
                    case 0x01:										/* SIDT */
                        Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());
                        Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());
                        break;
                    case 0x02:										/* LGDT */
                        if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                        // Read in the same order as in c so easier debugging
                    {
                        long v1 = (Memory.mem_readd(eaa+2) & 0xFFFFFFl);
                        int v0 = Memory.mem_readw(eaa);
                        CPU.CPU_LGDT(v0,v1);
                    }
                        break;
                    case 0x03:										/* LIDT */
                        if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                        // Read in the same order as in c so easier debugging
                    {
                        long v1 = (Memory.mem_readd(eaa+2) & 0xFFFFFFl);
                        int v0 = Memory.mem_readw(eaa);
                        CPU.CPU_LIDT(v0,v1);
                    }
                        break;
                    case 0x04:										/* SMSW */
                        Memory.mem_writew(eaa,(int)(CPU.CPU_SMSW() & 0xFFFFl));
                        break;
                    case 0x06:										/* LMSW */
                        limit=Memory.mem_readw(eaa);
                        if (CPU.CPU_LMSW(limit)) return RUNEXCEPTION();
                        break;
                    case 0x07:										/* INVLPG */
                        if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                        Paging.PAGING_ClearTLB();
                        break;
                    }
                } else {
                    switch (which) {
                    case 0x02:										/* LGDT */
                        if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                        return ILLEGAL_OPCODE;
                    case 0x03:										/* LIDT */
                        if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                        return ILLEGAL_OPCODE;
                    case 0x04:										/* SMSW */
                        Modrm.GetEArw[rm].word((int)(CPU.CPU_SMSW() & 0xFFFF));
                        break;
                    case 0x06:										/* LMSW */
                        if (CPU.CPU_LMSW(Modrm.GetEArw[rm].word())) return RUNEXCEPTION();
                        break;
                    default:
                        return ILLEGAL_OPCODE;
                    }
                }
                return HANDLED;
            }
        };

        /* LAR Gw,Ew */
        ops[0x102] = new OP() {
            final public int call() {
                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                /*Bit8u*/short rm=Fetchb.call();int_ref_1.value=Modrm.Getrw[rm].word();
                if (rm >= 0xc0) {
                    CPU.CPU_LAR(Modrm.GetEArw[rm].word(),int_ref_1);
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();CPU.CPU_LAR(Memory.mem_readw(eaa),int_ref_1);
                }
                Modrm.Getrw[rm].word(int_ref_1.value);
                return HANDLED;
            }
        };

        /* LSL Gw,Ew */
        ops[0x103] = new OP() {
            final public int call() {
                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                /*Bit8u*/short rm=Fetchb.call();int_ref_1.value=Modrm.Getrw[rm].word();
                if (rm >= 0xc0) {
                    CPU.CPU_LSL(Modrm.GetEArw[rm].word(),int_ref_1);
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();CPU.CPU_LSL(Memory.mem_readw(eaa),int_ref_1);
                }
                Modrm.Getrw[rm].word(int_ref_1.value);
                return HANDLED;
            }
        };

        /* CLTS */
        ops[0x106] = new OP() {
            final public int call() {
                if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                CPU.cpu.cr0&=(~CPU.CR0_TASKSWITCH);
                return HANDLED;
            }
        };
        ops[0x306] = ops[0x106];

        /* INVD */
        ops[0x108] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                return HANDLED;
            }
        };
        ops[0x308] = ops[0x108];

        /* WBINVD */
        ops[0x109] = ops[0x108];
        ops[0x309] = ops[0x108];

        /* MOV Rd.CRx */
        ops[0x120] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bitu*/int which=(rm >> 3) & 7;
                if (rm < 0xc0 ) {
                    rm |= 0xc0;
                    Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"MOV XXX,CR%u with non-register",which);
                }
                if (CPU.CPU_READ_CRX(which,long_ref_1)) return RUNEXCEPTION();
                Modrm.GetEArd[rm].dword(long_ref_1.value);
                return HANDLED;
            }
        };
        ops[0x320] = ops[0x120];

        /* MOV Rd,DRx */
        ops[0x121] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bitu*/int which=(rm >> 3) & 7;
                if (rm < 0xc0 ) {
                    rm |= 0xc0;
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,DR%u with non-register",which);
                }
                if (CPU.CPU_READ_DRX(which,long_ref_1)) return RUNEXCEPTION();
                Modrm.GetEArd[rm].dword(long_ref_1.value);
                return HANDLED;
            }
        };
        ops[0x321] = ops[0x121];

        /* MOV CRx,Rd */
        ops[0x122] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bitu*/int which=(rm >> 3) & 7;
                if (rm < 0xc0 ) {
                    rm |= 0xc0;
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,CR%u with non-register",which);
                }
                if (CPU.CPU_WRITE_CRX(which,Modrm.GetEArd[rm].dword())) return RUNEXCEPTION();
                return HANDLED;
            }
        };
        ops[0x322] = ops[0x122];

        /* MOV DRx,Rd */
        ops[0x123] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bitu*/int which=(rm >> 3) & 7;
                if (rm < 0xc0 ) {
                    rm |= 0xc0;
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV DR%u,XXX with non-register",which);
                }
                if (CPU.CPU_WRITE_DRX(which,Modrm.GetEArd[rm].dword())) return RUNEXCEPTION();
                return HANDLED;
            }
        };
        ops[0x323] = ops[0x123];

        /* MOV Rd,TRx */
        ops[0x124] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bitu*/int which=(rm >> 3) & 7;
                if (rm < 0xc0 ) {
                    rm |= 0xc0;
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,TR%u with non-register",which);
                }
                if (CPU.CPU_READ_TRX(which,long_ref_1)) return RUNEXCEPTION();
                Modrm.GetEArd[rm].dword(long_ref_1.value);
                return HANDLED;
            }
        };
        ops[0x324] = ops[0x124];

        /* MOV TRx,Rd */
        ops[0x126] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bitu*/int which=(rm >> 3) & 7;
                if (rm < 0xc0 ) {
                    rm |= 0xc0;
                    Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV TR%u,XXX with non-register",which);
                }
                if (CPU.CPU_WRITE_TRX(which,(int)Modrm.GetEArd[rm].dword())) return RUNEXCEPTION();
                return HANDLED;
            }
        };
        ops[0x326] = ops[0x126];

        /* RDTSC */
        ops[0x131] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUMSLOW) return ILLEGAL_OPCODE;
                /*Bit64s*/long tsc=(/*Bit64s*/long)(Pic.PIC_FullIndex()*(double)CPU.CPU_CycleMax);
                reg_edx.dword((tsc>>>32));
                reg_eax.dword((tsc&0xffffffffl));
                return HANDLED;
            }
        };
        ops[0x331] = ops[0x131];

        /* JO */
        ops[0x180] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_O());
                return CONTINUE;
            }
        };

        /* JNO */
        ops[0x181] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NO());
                return CONTINUE;
            }
        };

        /* JB */
        ops[0x182] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_B());
                return CONTINUE;
            }
        };

        /* JNB */
        ops[0x183] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NB());
                return CONTINUE;
            }
        };

        /* JZ */
        ops[0x184] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_Z());
                return CONTINUE;
            }
        };

        /* JNZ */
        ops[0x185] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NZ());
                return CONTINUE;
            }
        };

        /* JBE */
        ops[0x186] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_BE());
                return CONTINUE;
            }
        };

        /* JNBE */
        ops[0x187] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NBE());
                return CONTINUE;
            }
        };

        /* JS */
        ops[0x188] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_S());
                return CONTINUE;
            }
        };

        /* JNS */
        ops[0x189] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NS());
                return CONTINUE;
            }
        };

        /* JP */
        ops[0x18a] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_P());
                return CONTINUE;
            }
        };

        /* JNP */
        ops[0x18b] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NP());
                return CONTINUE;
            }
        };

        /* JL */
        ops[0x18c] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_L());
                return CONTINUE;
            }
        };

        /* JNL */
        ops[0x18d] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NL());
                return CONTINUE;
            }
        };

        /* JLE */
        ops[0x18e] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_LE());
                return CONTINUE;
            }
        };

        /* JNLE */
        ops[0x18f] = new OP() {
            final public int call() {
                JumpCond16_w(Flags.TFLG_NLE());
                return CONTINUE;
            }
        };

        /* SETO */
        ops[0x190] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_O());
                return HANDLED;
            }
        };
        ops[0x390] = ops[0x190];

        /* SETNO */
        ops[0x191] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NO());
                return HANDLED;
            }
        };
        ops[0x391] = ops[0x191];

        /* SETB */
        ops[0x192] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_B());
                return HANDLED;
            }
        };
        ops[0x392] = ops[0x192];

        /* SETNB */
        ops[0x193] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NB());
                return HANDLED;
            }
        };
        ops[0x393] = ops[0x193];

        /* SETZ */
        ops[0x194] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_Z());
                return HANDLED;
            }
        };
        ops[0x394] = ops[0x194];

        /* SETNZ */
        ops[0x195] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NZ());
                return HANDLED;
            }
        };
        ops[0x395] = ops[0x195];

        /* SETBE */
        ops[0x196] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_BE());
                return HANDLED;
            }
        };
        ops[0x396] = ops[0x196];

        /* SETNBE */
        ops[0x197] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NBE());
                return HANDLED;
            }
        };
        ops[0x397] = ops[0x197];

        /* SETS */
        ops[0x198] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_S());
                return HANDLED;
            }
        };
        ops[0x398] = ops[0x198];

        /* SETNS */
        ops[0x199] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NS());
                return HANDLED;
            }
        };
        ops[0x399] = ops[0x199];

        /* SETP */
        ops[0x19a] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_P());
                return HANDLED;
            }
        };
        ops[0x39a] = ops[0x19a];

        /* SETNP */
        ops[0x19b] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NP());
                return HANDLED;
            }
        };
        ops[0x39b] = ops[0x19b];

        /* SETL */
        ops[0x19c] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_L());
                return HANDLED;
            }
        };
        ops[0x39c] = ops[0x19c];

        /* SETNL */
        ops[0x19d] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NL());
                return HANDLED;
            }
        };
        ops[0x39d] = ops[0x19d];

        /* SETLE */
        ops[0x19e] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_LE());
                return HANDLED;
            }
        };
        ops[0x39e] = ops[0x19e];

        /* SETNLE */
        ops[0x19f] = new OP() {
            final public int call() {
                SETcc(Flags.TFLG_NLE());
                return HANDLED;
            }
        };
        ops[0x39f] = ops[0x19f];

        /* PUSH FS */
        ops[0x1a0] = new OP() {
            final public int call() {
                CPU.CPU_Push16((int)CPU.Segs_FSval);
                return HANDLED;
            }
        };

        /* POP FS */
        ops[0x1a1] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegFS(false)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* CPUID */
        ops[0x1a2] = new OP() {
            final public int call() {
                if (!CPU.CPU_CPUID()) return ILLEGAL_OPCODE;
                return HANDLED;
            }
        };
        ops[0x3a2] = ops[0x1a2];

        /* BT Ew,Gw */
        ops[0x1a3] = new OP() {
            final public int call() {
                Flags.FillFlags();/*Bit8u*/short rm=Fetchb.call();
                /* Bit16u*/int mask=1 << (Modrm.Getrw[rm].word() & 15);
                if (rm >= 0xc0 ) {
                    SETFLAGBIT(CF,(Modrm.GetEArw[rm].word() & mask)!=0?true:false);
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();eaa+=(((/*Bit16s*/short)Modrm.Getrw[rm].word())>>4)*2;
                    /*Bit16u*/int old=Memory.mem_readw(eaa);
                    SETFLAGBIT(CF,(old & mask)!=0?true:false);
                }
                return HANDLED;
            }
        };

        /* SHLD Ew,Gw,Ib */
        ops[0x1a4] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    r = rm;
                    int op3 = Fetchb.call();
                    DSHLW.call(Modrm.Getrw[rm].word(),op3,earw_l,earw_s);
                }
                else {
                    m = ea_table[rm].call();
                    int op3 = Fetchb.call();
                    DSHLW.call(Modrm.Getrw[rm].word(),op3,w_l,w_s);
                }
                return HANDLED;
            }
        };

        /* SHLD Ew,Gw,CL */
        ops[0x1a5] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    r = rm;
                    DSHLW.call(Modrm.Getrw[rm].word(),reg_ecx.low(),earw_l,earw_s);
                }
                else {
                    m = ea_table[rm].call();
                    DSHLW.call(Modrm.Getrw[rm].word(),reg_ecx.low(),w_l,w_s);
                }
                return HANDLED;
            }
        };

        /* PUSH GS */
        ops[0x1a8] = new OP() {
            final public int call() {
                CPU.CPU_Push16((int)CPU.Segs_GSval);
                return HANDLED;
            }
        };

        /* POP GS */
        ops[0x1a9] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegGS(false)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* BTS Ew,Gw */
        ops[0x1ab] = new OP() {
            final public int call() {
                Flags.FillFlags();/*Bit8u*/short rm=Fetchb.call();
                /*Bit16u*/int mask=1 << (Modrm.Getrw[rm].word() & 15);
                if (rm >= 0xc0 ) {
                    SETFLAGBIT(CF,(Modrm.GetEArw[rm].word() & mask)!=0);
                    Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word() | mask);
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();eaa+=(((/*Bit16s*/short)Modrm.Getrw[rm].word())>>4)*2;
                    /*Bit16u*/int old=Memory.mem_readw(eaa);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.mem_writew(eaa,old | mask);
                }
                return HANDLED;
            }
        };

        /* SHRD Ew,Gw,Ib */
        ops[0x1ac] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    r = rm;
                    int op3 = Fetchb.call();
                    DSHRW.call(Modrm.Getrw[rm].word(),op3,earw_l,earw_s);
                }
                else {
                    m = ea_table[rm].call();
                    int op3 = Fetchb.call();
                    DSHRW.call(Modrm.Getrw[rm].word(),op3,w_l,w_s);
                }
                return HANDLED;
            }
        };

        /* SHRD Ew,Gw,CL */
        ops[0x1ad] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    r = rm;
                    DSHRW.call(Modrm.Getrw[rm].word(),reg_ecx.low(),earw_l,earw_s);
                }
                else {
                    m = ea_table[rm].call();
                    DSHRW.call(Modrm.Getrw[rm].word(),reg_ecx.low(),w_l,w_s);
                }
                return HANDLED;
            }
        };

        /* IMUL Gw,Ew */
        ops[0x1af] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrw[rm];
                if (rm >= 0xc0 ) {
                    r.word(DIMULW(Modrm.GetEArw[rm].word(),r.word()));
                }
                else {
                    long eaa = ea_table[rm].call();
                    r.word(DIMULW(Memory.mem_readw(eaa),r.word()));
                }
                return HANDLED;
            }
        };

        /* cmpxchg Eb,Gb */
        ops[0x1b0] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                Flags.FillFlags();
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Modrm.Getrb_interface r = Modrm.GetEArb[rm];
                    if (reg_eax.low() == r.get()) {
                        r.set(Modrm.Getrb[rm].get());
                        SETFLAGBIT(ZF,true);
                    } else {
                        reg_eax.low(r.get());
                        SETFLAGBIT(ZF,false);
                    }
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();
                    /*Bit8u*/short val = Memory.mem_readb(eaa);
                    if (reg_eax.low() == val) {
                        Memory.mem_writeb(eaa,Modrm.Getrb[rm].get());
                        SETFLAGBIT(ZF,true);
                    } else {
                        Memory.mem_writeb(eaa,val);	// cmpxchg always issues a write
                        reg_eax.low(val);
                        SETFLAGBIT(ZF,false);
                    }
                }
                return HANDLED;
            }
        };
        ops[0x3b0] = ops[0x1b0];

        /* cmpxchg Ew,Gw */
        ops[0x1b1] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                Flags.FillFlags();
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    if(reg_eax.word() == Modrm.GetEArw[rm].word()) {
                        Modrm.GetEArw[rm].word(Modrm.Getrw[rm].word());
                        SETFLAGBIT(ZF,true);
                    } else {
                        reg_eax.word(Modrm.GetEArw[rm].word());
                        SETFLAGBIT(ZF,false);
                    }
                } else {
                       /*PhysPt*/long eaa=ea_table[rm].call();
                    /*Bit16u*/int val = Memory.mem_readw(eaa);
                    if(reg_eax.word() == val) {
                        Memory.mem_writew(eaa,Modrm.Getrw[rm].word());
                        SETFLAGBIT(ZF,true);
                    } else {
                        Memory.mem_writew(eaa,val);	// cmpxchg always issues a write
                        reg_eax.word(val);
                        SETFLAGBIT(ZF,false);
                    }
                }
                return HANDLED;
            }
        };

        /* LSS Ew */
        ops[0x1b2] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/long eaa=ea_table[rm].call();
                if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
                Modrm.Getrw[rm].word(Memory.mem_readw(eaa));
                return HANDLED;
            }
        };

        /* BTR Ew,Gw */
        ops[0x1b3] = new OP() {
            final public int call() {
                Flags.FillFlags();/*Bit8u*/short rm=Fetchb.call();
                /*Bit16u*/int mask=1 << (Modrm.Getrw[rm].word() & 15);
                if (rm >= 0xc0 ) {
                    SETFLAGBIT(CF,(Modrm.GetEArw[rm].word() & mask)!=0);
                    Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word() & ~mask);
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();eaa+=(((/*Bit16s*/short)Modrm.Getrw[rm].word())>>4)*2;
                    /*Bit16u*/int old=Memory.mem_readw(eaa);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.mem_writew(eaa,old & ~mask);
                }
                return HANDLED;
            }
        };

        /* LFS Ew */
        ops[0x1b4] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/long eaa=ea_table[rm].call();
                if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
                Modrm.Getrw[rm].word(Memory.mem_readw(eaa));
                return HANDLED;
            }
        };

        /* LGS Ew */
        ops[0x1b5] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/long eaa=ea_table[rm].call();
                if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();
                Modrm.Getrw[rm].word(Memory.mem_readw(eaa));
                return HANDLED;
            }
        };

        /* MOVZX Gw,Eb */
        ops[0x1b6] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {Modrm.Getrw[rm].word(Modrm.GetEArb[rm].get());}
                else {/*PhysPt*/long eaa=ea_table[rm].call();Modrm.Getrw[rm].word(Memory.mem_readb(eaa));}
                return HANDLED;
            }
        };

        /* MOVZX Gw,Ew */
        ops[0x1b7] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {Modrm.Getrw[rm].word(Modrm.GetEArw[rm].word());}
                else {/*PhysPt*/long eaa=ea_table[rm].call();Modrm.Getrw[rm].word(Memory.mem_readw(eaa));}
                return HANDLED;
            }
        };
        /* MOVSX Gw,Ew */
        ops[0x1bf] = ops[0x1b7];

        /* GRP8 Ew,Ib */
        ops[0x1ba] = new OP() {
            final public int call() {
                Flags.FillFlags();/*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    /*Bit16u*/int mask=1 << (Fetchb.call() & 15);
                    SETFLAGBIT(CF,(Modrm.GetEArw[rm].word() & mask)!=0);
                    switch (rm & 0x38) {
                    case 0x20:										/* BT */
                        break;
                    case 0x28:										/* BTS */
                        Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word() | mask);
                        break;
                    case 0x30:										/* BTR */
                        Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word() & ~mask);
                        break;
                    case 0x38:										/* BTC */
                        Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word() ^ mask);
                        break;
                    default:
                        Log.exit("CPU:0F:BA:Illegal subfunction %X",rm & 0x38);
                    }
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();/*Bit16u*/int old=Memory.mem_readw(eaa);
                    /*Bit16u*/int mask=1 << (Fetchb.call() & 15);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    switch (rm & 0x38) {
                    case 0x20:										/* BT */
                        break;
                    case 0x28:										/* BTS */
                        Memory.mem_writew(eaa,old|mask);
                        break;
                    case 0x30:										/* BTR */
                        Memory.mem_writew(eaa,old & ~mask);
                        break;
                    case 0x38:										/* BTC */
                        Memory.mem_writew(eaa,old ^ mask);
                        break;
                    default:
                        Log.exit("CPU:0F:BA:Illegal subfunction %X",rm & 0x38);
                    }
                }
                return HANDLED;
            }
        };

        /* BTC Ew,Gw */
        ops[0x1bb] = new OP() {
            final public int call() {
                Flags.FillFlags();/*Bit8u*/short rm=Fetchb.call();
                /*Bit16u*/int mask=1 << (Modrm.Getrw[rm].word() & 15);
                if (rm >= 0xc0 ) {
                    SETFLAGBIT(CF,(Modrm.GetEArw[rm].word() & mask)!=0);
                    Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word()^mask);
                } else {
                    /*PhysPt*/long eaa=ea_table[rm].call();eaa+=(((/*Bit16s*/short)Modrm.Getrw[rm].word())>>4)*2;
                    /*Bit16u*/int old=Memory.mem_readw(eaa);
                    SETFLAGBIT(CF,(old & mask)!=0);
                    Memory.mem_writew(eaa,old ^ mask);
                }
                return HANDLED;
            }
        };

        /* BSF Gw,Ew */
        ops[0x1bc] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bit16u*/int result,value;
                if (rm >= 0xc0) { value=Modrm.GetEArw[rm].word(); }
                else			{ /*PhysPt*/long eaa=ea_table[rm].call(); value=Memory.mem_readw(eaa); }
                if (value==0) {
                    SETFLAGBIT(ZF,true);
                } else {
                    result = 0;
                    while ((value & 0x01)==0) { result++; value>>=1; }
                    SETFLAGBIT(ZF,false);
                    Modrm.Getrw[rm].word(result);
                }
                Flags.lflags.type=Flags.t_UNKNOWN;
                return HANDLED;
            }
        };

        /* BSR Gw,Ew */
        ops[0x1bd] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                /*Bit16u*/int result,value;
                if (rm >= 0xc0) { value=Modrm.GetEArw[rm].word(); }
                else			{ /*PhysPt*/long eaa=ea_table[rm].call(); value=Memory.mem_readw(eaa); }
                if (value==0) {
                    SETFLAGBIT(ZF,true);
                } else {
                    result = 15;	// Operandsize-1
                    while ((value & 0x8000)==0) { result--; value<<=1; }
                    SETFLAGBIT(ZF,false);
                    Modrm.Getrw[rm].word(result);
                }
                Flags.lflags.type=Flags.t_UNKNOWN;
                return HANDLED;
            }
        };

        /* MOVSX Gw,Eb */
        ops[0x1be] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {Modrm.Getrw[rm].word(Modrm.GetEArb[rm].get());}
                else {/*PhysPt*/long eaa=ea_table[rm].call();Modrm.Getrw[rm].word((byte)Memory.mem_readb(eaa));}
                return HANDLED;
            }
        };

        /* XADD Gb,Eb */
        ops[0x1c0] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                /*Bit8u*/short rm=Fetchb.call();/*Bit8u*/short oldrmrb=Modrm.Getrb[rm].get();
                if (rm >= 0xc0 ) {Modrm.Getrb[rm].set(Modrm.GetEArb[rm].get());Modrm.GetEArb[rm].set((short)(Modrm.GetEArb[rm].get()+oldrmrb));}
                else {/*PhysPt*/long eaa=ea_table[rm].call();Modrm.Getrb[rm].set(Memory.mem_readb(eaa));Memory.mem_writeb(eaa,Memory.mem_readb(eaa)+oldrmrb);}
                return HANDLED;
            }
        };
        ops[0x3c0] = ops[0x1c0];

        /* XADD Gw,Ew */
        ops[0x1c1] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                /*Bit8u*/short rm=Fetchb.call();/*Bit16u*/int oldrmrw=Modrm.Getrw[rm].word();
                if (rm >= 0xc0 ) {Modrm.Getrw[rm].word(Modrm.GetEArw[rm].word());Modrm.GetEArw[rm].word(Modrm.GetEArw[rm].word()+oldrmrw);}
                else {/*PhysPt*/long eaa=ea_table[rm].call();Modrm.Getrw[rm].word(Memory.mem_readw(eaa));Memory.mem_writew(eaa,Memory.mem_readw(eaa)+oldrmrw);}
                return HANDLED;
            }
        };

        /* BSWAP AX */
        ops[0x1c8] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_eax.word(Instructions.BSWAPW(reg_eax.word()));
                return HANDLED;
            }
        };

        /* BSWAP CX */
        ops[0x1c9] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_ecx.word(Instructions.BSWAPW(reg_ecx.word()));
                return HANDLED;
            }
        };

        /* BSWAP DX */
        ops[0x1ca] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_edx.word(Instructions.BSWAPW(reg_edx.word()));
                return HANDLED;
            }
        };

        /* BSWAP BX */
        ops[0x1cb] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_ebx.word(Instructions.BSWAPW(reg_ebx.word()));
                return HANDLED;
            }
        };

        /* BSWAP SP */
        ops[0x1cc] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_esp.word(Instructions.BSWAPW(reg_esp.word()));
                return HANDLED;
            }
        };

        /* BSWAP BP */
        ops[0x1cd] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_ebp.word(Instructions.BSWAPW(reg_ebp.word()));
                return HANDLED;
            }
        };

        /* BSWAP SI */
        ops[0x1ce] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_esi.word(Instructions.BSWAPW(reg_esi.word()));
                return HANDLED;
            }
        };

        /* BSWAP DI */
        ops[0x1ce] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLDSLOW) return ILLEGAL_OPCODE;
                reg_edi.word(Instructions.BSWAPW(reg_edi.word()));
                return HANDLED;
            }
        };
    }
}
