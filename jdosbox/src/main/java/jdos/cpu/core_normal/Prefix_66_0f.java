package jdos.cpu.core_normal;

import jdos.cpu.*;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Prefix_66_0f extends Prefix_66 {
    static {
        /* GRP 6 Exxx */
        ops[0x300] = new OP() {
            final public int call() {
                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                /*Bit8u*/int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:	/* SLDT */
                case 0x01:	/* STR */
                    {
                        /*Bitu*/int saveval;
                        if (which==0) saveval=CPU.CPU_SLDT();
                        else saveval=CPU.CPU_STR();
                        if (rm >= 0xc0) {Modrm.GetEArw[rm].word(saveval);}
                        else {/*PhysPt*/int eaa=getEaa(rm);Memory.mem_writew(eaa,saveval);}
                    }
                    break;
                case 0x02:case 0x03:case 0x04:case 0x05:
                    {
                        /*Bitu*/int loadval;
                        if (rm >= 0xc0 ) {loadval=Modrm.GetEArw[rm].word();}
                        else {/*PhysPt*/int eaa=getEaa(rm);loadval=Memory.mem_readw(eaa);}
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
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"GRP6:Illegal call "+Integer.toString(which,16));
                    return ILLEGAL_OPCODE;
                }
                return HANDLED;
            }
        };

        /* Group 7 Ed */
        ops[0x301] = new OP() {
            final public int call() {
                int rm=Fetchb();/*Bitu*/int which=(rm>>3)&7;
                if (rm < 0xc0)	{ //First ones all use EA
                    /*PhysPt*/int eaa = getEaa(rm);/*Bitu*/int limit;
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
                        CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));
                        break;
                    case 0x03:										/* LIDT */
                        if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);
                        CPU.CPU_LIDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));
                        break;
                    case 0x04:										/* SMSW */
                        Memory.mem_writew(eaa,CPU.CPU_SMSW() & 0xFFFF);
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
                        Modrm.GetEArd[rm].dword=CPU.CPU_SMSW();
                        break;
                    case 0x06:										/* LMSW */
                        if (CPU.CPU_LMSW(Modrm.GetEArd[rm].dword)) return RUNEXCEPTION();
                        break;
                    default:
                        if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"Illegal group 7 RM subfunction "+which);
                        return ILLEGAL_OPCODE;
                    }

                }
                return HANDLED;
            }
        };

        /* LAR Gd,Ed */
        ops[0x302] = new OP() {
            final public int call() {
                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                int rm=Fetchb();
                if (rm >= 0xc0) {
                    Modrm.Getrd[rm].dword=CPU.CPU_LAR(Modrm.GetEArw[rm].word(),Modrm.Getrd[rm].dword);
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);
                    Modrm.Getrd[rm].dword=CPU.CPU_LAR(Memory.mem_readw(eaa),Modrm.Getrd[rm].dword);
                }
                return HANDLED;
            }
        };

        /* LSL Gd,Ew */
        ops[0x303] = new OP() {
            final public int call() {
                if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                int rm=Fetchb();
                /* Just load 16-bit values for selectors */
                if (rm >= 0xc0) {
                    Modrm.Getrd[rm].dword = CPU.CPU_LSL(Modrm.GetEArw[rm].word(),Modrm.Getrd[rm].dword);
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);
                    Modrm.Getrd[rm].dword = CPU.CPU_LSL(Memory.mem_readw(eaa),Modrm.Getrd[rm].dword);
                }
                return HANDLED;
            }
        };

        /* WRMSR */
        ops[0x330] = new OP() {
            final public int call() {
                if (CPU.cpu.cpl != 0) return EXCEPTION(CPU.EXCEPTION_GP);
                CPU.writeMSR(CPU_Regs.reg_ecx.dword, ((CPU_Regs.reg_edx.dword & 0xFFFFFFFFl) << 32) | (CPU_Regs.reg_eax.dword & 0xFFFFFFFFl));
                return HANDLED;
            }
        };

        /* RDMSR */
        ops[0x332] = new OP() {
            final public int call() {
                if (CPU.cpu.cpl != 0) return EXCEPTION(CPU.EXCEPTION_GP);
                long result = CPU.readMSR(CPU_Regs.reg_ecx.dword);
                CPU_Regs.reg_eax.dword = (int)result;
                CPU_Regs.reg_edx.dword = (int)(result >>> 32);
                return HANDLED;
            }
        };

        /* CMOVO */
        ops[0x340] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_O())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_O())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNO */
        ops[0x341] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NO())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NO())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVB */
        ops[0x342] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_B())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_B())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNB */
        ops[0x343] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NB())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NB())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVZ */
        ops[0x344] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_Z())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_Z())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNZ */
        ops[0x345] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NZ())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NZ())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVBE */
        ops[0x346] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_BE())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_BE())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNBE */
        ops[0x347] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NBE())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NBE())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVS */
        ops[0x348] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_S())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_S())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNS */
        ops[0x349] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NS())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NS())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVP */
        ops[0x34a] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_P())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_P())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNP */
        ops[0x34b] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NP())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NP())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVL */
        ops[0x34c] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_L())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_L())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNL */
        ops[0x34d] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NL())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NL())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVLE */
        ops[0x34e] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_LE())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_LE())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* CMOVNLE */
        ops[0x34f] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    return ILLEGAL_OPCODE;
                }

                int rm=Fetchb();
                if (rm >= 0xc0) {
                    if (Flags.TFLG_NLE())
                        Modrm.Getrd[rm].dword = Modrm.GetEArd[rm].dword;
                } else {
                    int eaa = getEaa(rm);
                    int temp = Memory.mem_readd(eaa); // must read before comparison so that it can throw errors
                    if (Flags.TFLG_NLE())
                        Modrm.Getrd[rm].dword=temp;
                }
                return HANDLED;
            }
        };

        /* JO */
        ops[0x380] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_O());
                return CONTINUE;
            }
        };

        /* JNO */
        ops[0x381] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NO());
                return CONTINUE;
            }
        };

        /* JB */
        ops[0x382] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_B());
                return CONTINUE;
            }
        };

        /* JNB */
        ops[0x383] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NB());
                return CONTINUE;
            }
        };

        /* JZ */
        ops[0x384] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_Z());
                return CONTINUE;
            }
        };

        /* JNZ */
        ops[0x385] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NZ());
                return CONTINUE;
            }
        };

        /* JBE */
        ops[0x386] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_BE());
                return CONTINUE;
            }
        };

        /* JNBE */
        ops[0x387] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NBE());
                return CONTINUE;
            }
        };

        /* JS */
        ops[0x388] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_S());
                return CONTINUE;
            }
        };

        /* JNS */
        ops[0x389] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NS());
                return CONTINUE;
            }
        };

        /* JP */
        ops[0x38a] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_P());
                return CONTINUE;
            }
        };

        /* JNP */
        ops[0x38b] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NP());
                return CONTINUE;
            }
        };

        /* JL */
        ops[0x38c] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_L());
                return CONTINUE;
            }
        };

        /* JNL */
        ops[0x38d] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NL());
                return CONTINUE;
            }
        };

        /* JLE */
        ops[0x38e] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_LE());
                return CONTINUE;
            }
        };

        /* JNLE */
        ops[0x38f] = new OP() {
            final public int call() {
                JumpCond32_d(Flags.TFLG_NLE());
                return CONTINUE;
            }
        };

        /* PUSH FS */
        ops[0x3a0] = new OP() {
            final public int call() {
                CPU.CPU_Push32(CPU_Regs.reg_fsVal.dword);
                return HANDLED;
            }
        };

        /* POP FS */
        ops[0x3a1] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegFS(true)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* BT Ed,Gd */
        ops[0x3a3] = new OP() {
            final public int call() {
                FillFlags();int rm=Fetchb();
                /*Bit32u*/int mask=1 << (Modrm.Getrd[rm].dword & 31);
                if (rm >= 0xc0 ) {
                    SETFLAGBIT(CF,(Modrm.GetEArd[rm].dword & mask)!=0);
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);eaa+=(Modrm.Getrd[rm].dword>>5)*4; // intentional signed shift
                    /*Bit32u*/int old=Memory.mem_readd(eaa);
                    SETFLAGBIT(CF,(old & mask)!=0);
                }
                return HANDLED;
            }
        };

        /* SHLD Ed,Gd,Ib */
        ops[0x3a4] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    int op3 = Fetchb() & 0x1F;
                    if (op3!=0) {
                        Reg r = Modrm.GetEArd[rm];
                        r.dword=DSHLD(Modrm.Getrd[rm].dword,op3,r.dword);
                    }
                }
                else {
                    int eaa = getEaa(rm);
                    int op3 = Fetchb() & 0x1F;
                    if (op3!=0)
                        Memory.mem_writed(eaa, DSHLD(Modrm.Getrd[rm].dword,op3,Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* SHLD Ed,Gd,CL */
        ops[0x3a5] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                int val = reg_ecx.dword & 0x1f;
                if (rm >= 0xc0 ) {
                    if (val != 0) {
                        Reg r = Modrm.GetEArd[rm];
                        r.dword=DSHLD(Modrm.Getrd[rm].dword,val,r.dword);
                    }
                }
                else {
                    int eaa = getEaa(rm);
                    if (val != 0)
                        Memory.mem_writed(eaa, DSHLD(Modrm.Getrd[rm].dword,val,Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* PUSH GS */
        ops[0x3a8] = new OP() {
            final public int call() {
                CPU.CPU_Push32(CPU_Regs.reg_gsVal.dword);
                return HANDLED;
            }
        };

        /* POP GS */
        ops[0x3a9] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegGS(true)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* BTS Ed,Gd */
        ops[0x3ab] = new OP() {
            final public int call() {
                FillFlags();int rm=Fetchb();
                Reg rd = Modrm.Getrd[rm];
                /*Bit32u*/int mask=1 << (rd.dword & 31);
                if (rm >= 0xc0 ) {
                    Reg eard = Modrm.GetEArd[rm];
                    SETFLAGBIT(CF,(eard.dword & mask)!=0);
                    eard.dword|=mask;
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);eaa+=(rd.dword>>5)*4; // intentional signed shift
                    /*Bit32u*/int old=Memory.mem_readd(eaa);
                    Memory.mem_writed(eaa,old | mask);
                    SETFLAGBIT(CF,(old & mask)!=0);
                }
                return HANDLED;
            }
        };

        /* SHRD Ed,Gd,Ib */
        ops[0x3ac] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    int op3 = Fetchb() & 0x1F;
                    if (op3!=0) {
                        Reg r = Modrm.GetEArd[rm];
                        r.dword=DSHRD(Modrm.Getrd[rm].dword,op3,r.dword);
                    }
                }
                else {
                    int eaa = getEaa(rm);
                    int op3 = Fetchb() & 0x1F;
                    if (op3!=0)
                        Memory.mem_writed(eaa, DSHRD(Modrm.Getrd[rm].dword,op3,Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* SHRD Ed,Gd,CL */
        ops[0x3ad] = new OP() {
            final public int call() {
                /*Bit8u*/final int rm=Fetchb();
                int val = reg_ecx.dword & 0x1f;
                if (rm >= 0xc0 ) {
                    if (val != 0) {
                        Reg r = Modrm.GetEArd[rm];
                        r.dword=DSHRD(Modrm.Getrd[rm].dword,val,r.dword);
                    }
                }
                else {
                    int eaa = getEaa(rm);
                    if (val != 0)
                        Memory.mem_writed(eaa, DSHRD(Modrm.Getrd[rm].dword,val,Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* IMUL Gd,Ed */
        ops[0x3af] = new OP() {
            final public int call() {
                // PERFORMANCE
                //
                // IMUL Gd,Ed TEST 1: 30
                // IMUL Gd,Ed TEST 2: 24
                /*Bit8u*/final int rm=Fetchb();
//                System.out.print("IMUL Gd,Ed TEST 1: ");
//                long start = System.currentTimeMillis();
//                for (int i=0;i<1000000;i++) {
//                    if (rm >= 0xc0 ) {
//                        Modrm.Getrd[rm].dword(DIMULD(Modrm.GetEArd[rm].dword(),Modrm.Getrd[rm].dword()));
//                    }
//                    else {
//                        Modrm.Getrd[rm].dword(DIMULD(Memory.mem_readd(getEaa(rm)),Modrm.Getrd[rm].dword()));
//                    }
//                }
//                System.out.println(System.currentTimeMillis()-start);
//                System.out.print("IMUL Gd,Ed TEST 2: ");
//                start = System.currentTimeMillis();
//                for (int i=0;i<1000000;i++) {
                    Reg r = Modrm.Getrd[rm];
                    if (rm >= 0xc0 ) {
                        r.dword=DIMULD(Modrm.GetEArd[rm].dword,r.dword);
                    }
                    else {
                        r.dword=DIMULD(Memory.mem_readd(getEaa(rm)),r.dword);
                    }
//                }
//                System.out.println(System.currentTimeMillis()-start);
                return HANDLED;
            }
        };

        /* CMPXCHG Ed,Gd */
        // :DOSBOX: this is different from dosbox
        ops[0x3b1] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486NEW) return ILLEGAL_OPCODE;
                int rm=Fetchb();
                if (rm >= 0xc0) {
                    Reg eard = Modrm.GetEArd[rm];
                    Instructions.CMPD(eard.dword, reg_eax.dword); // Sets the flags
                    FillFlags();
                    if (eard.dword==reg_eax.dword) {
                        eard.dword=Modrm.Getrd[rm].dword;
                        SETFLAGBIT(ZF,true);
                    } else {
                        reg_eax.dword=eard.dword;
                        SETFLAGBIT(ZF,false);
                    }
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);
                    /*Bit32u*/int val=Memory.mem_readd(eaa);
                    Instructions.CMPD(val, reg_eax.dword); // Sets the flags
                    FillFlags();
                    if (val==reg_eax.dword) {
                        Memory.mem_writed(eaa,Modrm.Getrd[rm].dword);
                        SETFLAGBIT(ZF,true);
                    } else {
                        Memory.mem_writed(eaa,val);	// cmpxchg always issues a write
                        reg_eax.dword=val;
                        SETFLAGBIT(ZF,false);
                    }
                }
                return HANDLED;
            }
        };

        /* LSS Ed */
        ops[0x3b2] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/int eaa = getEaa(rm);
                if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
                Modrm.Getrd[rm].dword=Memory.mem_readd(eaa);
                return HANDLED;
            }
        };

        /* BTR Ed,Gd */
        ops[0x3b3] = new OP() {
            final public int call() {
                FillFlags();int rm=Fetchb();
                Reg rd = Modrm.Getrd[rm];
                /*Bit32u*/int mask=1 << (rd.dword & 31);
                if (rm >= 0xc0 ) {
                    Reg eard = Modrm.GetEArd[rm];
                    SETFLAGBIT(CF,(eard.dword & mask)!=0);
                    eard.dword&=~mask;
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);eaa+=(rd.dword>>5)*4; // intentional signed shift
                    /*Bit32u*/int old=Memory.mem_readd(eaa);
                    Memory.mem_writed(eaa,old & ~mask);
                    SETFLAGBIT(CF,(old & mask)!=0);
                }
                return HANDLED;
            }
        };

        /* LFS Ed */
        ops[0x3b4] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/int eaa = getEaa(rm);
                if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
                Modrm.Getrd[rm].dword=Memory.mem_readd(eaa);
                return HANDLED;
            }
        };

        /* LGS Ed */
        ops[0x3b5] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/int eaa = getEaa(rm);
                if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
                Modrm.Getrd[rm].dword=Memory.mem_readd(eaa);
                return HANDLED;
            }
        };

        /* MOVZX Gd,Eb */
        ops[0x3b6] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0 ) {Modrm.Getrd[rm].dword=Modrm.GetEArb[rm].get();}
                else {/*PhysPt*/int eaa = getEaa(rm);Modrm.Getrd[rm].dword=Memory.mem_readb(eaa);}
                return HANDLED;
            }
        };

        /* MOVXZ Gd,Ew */
        ops[0x3b7] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0 ) {Modrm.Getrd[rm].dword=Modrm.GetEArw[rm].word();}
                else {/*PhysPt*/int eaa = getEaa(rm);Modrm.Getrd[rm].dword=Memory.mem_readw(eaa);}
                return HANDLED;
            }
        };

        /* GRP8 Ed,Ib */
        ops[0x3ba] = new OP() {
            final public int call() {
                FillFlags();int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    /*Bit32u*/int mask=1 << (Fetchb() & 31);
                    Reg eard = Modrm.GetEArd[rm];
                    SETFLAGBIT(CF,(eard.dword & mask)!=0);
                    switch (rm & 0x38) {
                    case 0x20:											/* BT */
                        break;
                    case 0x28:											/* BTS */
                        eard.dword|=mask;
                        break;
                    case 0x30:											/* BTR */
                        eard.dword&=~mask;
                        break;
                    case 0x38:											/* BTC */
                        if (GETFLAG(CF)!=0) eard.dword&=~mask;
                        else eard.dword|=mask;
                        break;
                    default:
                        Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
                    }
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);/*Bit32u*/int old=Memory.mem_readd(eaa);
                    /*Bit32u*/int mask=1 << (Fetchb() & 31);
                    switch (rm & 0x38) {
                    case 0x20:											/* BT */
                        break;
                    case 0x28:											/* BTS */
                        Memory.mem_writed(eaa,old|mask);
                        break;
                    case 0x30:											/* BTR */
                        Memory.mem_writed(eaa,old & ~mask);
                        break;
                    case 0x38:											/* BTC */
                        if (GETFLAG(CF)!=0) old&=~mask;
                        else old|=mask;
                        Memory.mem_writed(eaa,old);
                        break;
                    default:
                        Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
                    }
                    SETFLAGBIT(CF,(old & mask)!=0);
                }
                return HANDLED;
            }
        };

        /* BTC Ed,Gd */
        ops[0x3bb] = new OP() {
            final public int call() {
                FillFlags();int rm=Fetchb();
                /*Bit32u*/int mask=1 << (Modrm.Getrd[rm].dword & 31);
                if (rm >= 0xc0 ) {
                    Reg eard = Modrm.GetEArd[rm];
                    SETFLAGBIT(CF,(eard.dword & mask)!=0);
                    eard.dword^=mask;
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);eaa+=(Modrm.Getrd[rm].dword>>5)*4; // intentional signed shift
                    /*Bit32u*/int old=Memory.mem_readd(eaa);
                    Memory.mem_writed(eaa,old ^ mask);
                    SETFLAGBIT(CF,(old & mask)!=0);
                }
                return HANDLED;
            }
        };

        /* BSF Gd,Ed */
        ops[0x3bc] = new OP() {
            final public int call() {
                int rm=Fetchb();
                /*Bit32u*/int result,value;
                if (rm >= 0xc0) { value=Modrm.GetEArd[rm].dword; }
                else			{ /*PhysPt*/int eaa = getEaa(rm); value=Memory.mem_readd(eaa); }
                if (value==0) {
                    SETFLAGBIT(ZF,true);
                } else {
                    result = 0;
                    while ((value & 0x01)==0) { result++; value>>>=1; }
                    SETFLAGBIT(ZF,false);
                    Modrm.Getrd[rm].dword=result;
                }
                type=t_UNKNOWN;
                return HANDLED;
            }
        };

        /*  BSR Gd,Ed */
        ops[0x3bd] = new OP() {
            final public int call() {
                int rm=Fetchb();
                /*Bit32u*/int result,value;
                if (rm >= 0xc0) { value=Modrm.GetEArd[rm].dword; }
                else			{ /*PhysPt*/int eaa = getEaa(rm); value=Memory.mem_readd(eaa); }
                if (value==0) {
                    SETFLAGBIT(ZF,true);
                } else {
                    result = 31;	// Operandsize-1
                    while ((value & 0x80000000)==0) { result--; value<<=1; }
                    SETFLAGBIT(ZF,false);
                    Modrm.Getrd[rm].dword=result;
                }
                type=t_UNKNOWN;
                return HANDLED;
            }
        };

        /* MOVSX Gd,Eb */
        ops[0x3be] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0 ) {Modrm.Getrd[rm].dword=(byte)(Modrm.GetEArb[rm].get());}
                else {/*PhysPt*/int eaa = getEaa(rm);Modrm.Getrd[rm].dword=(byte)Memory.mem_readb(eaa);}
                return HANDLED;
            }
        };

        /* MOVSX Gd,Ew */
        ops[0x3bf] = new OP() {
            final public int call() {
                int rm=Fetchb();
                if (rm >= 0xc0 ) {Modrm.Getrd[rm].dword=(short)(Modrm.GetEArw[rm].word());}
                else {/*PhysPt*/int eaa = getEaa(rm);Modrm.Getrd[rm].dword=(short)Memory.mem_readw(eaa);} // Yes that short signed cast is intentional
                return HANDLED;
            }
        };

        /* XADD Gd,Ed */
        // :DOSBOX: this is different from dosbox
        ops[0x3c1] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
                int rm=Fetchb();
                if (rm >= 0xc0 ) {
                    int result = Instructions.ADDD(Modrm.Getrd[rm].dword, Modrm.GetEArd[rm].dword);
                    Modrm.Getrd[rm].dword=Modrm.GetEArd[rm].dword;
                    Modrm.GetEArd[rm].dword=result;
                } else {
                    /*PhysPt*/int eaa = getEaa(rm);
                    int value = Memory.mem_readd(eaa);
                    int result = Instructions.ADDD(Modrm.Getrd[rm].dword, value);
                    Memory.mem_writed(eaa,result);
                    Modrm.Getrd[rm].dword=value;
                }
                return HANDLED;
            }
        };

        ops[0x3c7] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_PENTIUM) return ILLEGAL_OPCODE;
                int rm=Fetchb();
                switch (rm & 0x38) {
                    case 8: // CMPXCHG8B
                    {
                        long value1 = ((CPU_Regs.reg_edx.dword & 0xffffffffL) << 32) | (CPU_Regs.reg_eax.dword & 0xffffffffL);
                        int eaa = getEaa(rm);
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
                    }
                        break;
                    default:
                        return ILLEGAL_OPCODE;
                }
                return HANDLED;
            }
        };

        /* BSWAP EAX */
        ops[0x3c8] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_eax.dword=BSWAPD(reg_eax.dword);
                return HANDLED;
            }
        };

        /* BSWAP ECX */
        ops[0x3c9] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_ecx.dword=BSWAPD(reg_ecx.dword);
                return HANDLED;
            }
        };

        /* BSWAP EDX */
        ops[0x3ca] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_edx.dword=BSWAPD(reg_edx.dword);
                return HANDLED;
            }
        };

        /* BSWAP EBX */
        ops[0x3cb] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_ebx.dword=BSWAPD(reg_ebx.dword);
                return HANDLED;
            }
        };

        /* BSWAP ESP */
        ops[0x3cc] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_esp.dword=BSWAPD(reg_esp.dword);
                return HANDLED;
            }
        };

        /* BSWAP EBP */
        ops[0x3cd] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_ebp.dword=BSWAPD(reg_ebp.dword);
                return HANDLED;
            }
        };

        /* BSWAP ESI */
        ops[0x3ce] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_esi.dword=BSWAPD(reg_esi.dword);
                return HANDLED;
            }
        };

        /* BSWAP EDI */
        ops[0x3cf] = new OP() {
            final public int call() {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) return ILLEGAL_OPCODE;
		        reg_edi.dword=BSWAPD(reg_edi.dword);
                return HANDLED;
            }
        };
    }
}
