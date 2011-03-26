package jdos.cpu.core_normal;

import jdos.cpu.*;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;

public class Prefix_66 extends Prefix_0f {
    static final private /*Bitu*/IntRef int_ref_1=new IntRef(0);
    
    static {
        /* ADD Ed,Gd */
        ops[0x201] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(ADDD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, ADDD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, ADDD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

         /* ADD Gd,Ed */
        ops[0x203] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(ADDD(Modrm.GetEArd[rm].dword(), r.dword()));
                } else {
                    r.dword(ADDD(Memory.mem_readd(getEaa(rm)), r.dword()));
                }
                return HANDLED;
            }
        };

         /* ADD EAX,Id */
        ops[0x205] = new OP() {
            final public int call() {
                reg_eax.dword(ADDD(Fetchd.call(), reg_eax.dword()));
                return HANDLED;
            }
        };

         /* PUSH ES */
        ops[0x206] = new OP() {
            final public int call() {
                CPU.CPU_Push32(CPU.Segs_ESval);
                return HANDLED;
            }
        };

         /* POP ES */
        ops[0x207] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegES(true)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

         /* OR Ed,Gd */
         ops[0x209] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(ORD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, ORD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, ORD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

         /* OR Gd,Ed */
        ops[0x20b] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(ORD(Modrm.GetEArd[rm].dword(), r.dword()));
                } else {
                    r.dword(ORD(Memory.mem_readd(getEaa(rm)), r.dword()));
                }
                return HANDLED;
            }
        };

        /* OR EAX,Id */
        ops[0x20d] = new OP() {
            final public int call() {
                reg_eax.dword(ORD(Fetchd.call(),reg_eax.dword()));
                return HANDLED;
            }
        };

         /* PUSH CS */
        ops[0x20e] = new OP() {
            final public int call() {
                CPU.CPU_Push32(CPU.Segs_CSval);
                return HANDLED;
            }
        };

        /* ADC Ed,Gd */
        ops[0x211] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(ADCD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, ADCD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, ADCD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };


        /* ADC Gd,Ed */
        ops[0x213] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(ADCD(Modrm.GetEArd[rm].dword(), r.dword()));
                } else {
                    r.dword(ADCD(Memory.mem_readd(getEaa(rm)), r.dword()));
                }                
                return HANDLED;
            }
        };

        /* ADC EAX,Id */
        ops[0x215] = new OP() {
            final public int call() {
                reg_eax.dword(ADCD(Fetchd.call(), reg_eax.dword()));
                return HANDLED;
            }
        };

        /* PUSH SS */
        ops[0x216] = new OP() {
            final public int call() {
                CPU.CPU_Push32(CPU.Segs_SSval);
                return HANDLED;
            }
        };

        /* POP SS */
        ops[0x217] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegSS(true)) return RUNEXCEPTION();
                CPU.CPU_Cycles++;
                return HANDLED;
            }
        };

        /* SBB Ed,Gd */
        ops[0x219] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(SBBD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, SBBD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, SBBD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* SBB Gd,Ed */
        ops[0x21b] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(SBBD(Modrm.GetEArd[rm].dword(),r.dword()));
                } else {
                    r.dword(SBBD(Memory.mem_readd(getEaa(rm)),r.dword()));
                }
                return HANDLED;
            }
        };

        /* SBB EAX,Id */
        ops[0x21d] = new OP() {
            final public int call() {
                reg_eax.dword(SBBD(Fetchd.call(), reg_eax.dword()));
                return HANDLED;
            }
        };

        /* PUSH DS */
        ops[0x21e] = new OP() {
            final public int call() {
                CPU.CPU_Push32(CPU.Segs_DSval);
                return HANDLED;
            }
        };

        /* POP DS */
        ops[0x21f] = new OP() {
            final public int call() {
                if (CPU.CPU_PopSegDS(true)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* AND Ed,Gd */
        ops[0x221] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(ANDD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, ANDD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, ANDD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* AND Gd,Ed */
        ops[0x223] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(ANDD(Modrm.GetEArd[rm].dword(), r.dword()));
                } else {
                    r.dword(ANDD(Memory.mem_readd(getEaa(rm)), r.dword()));
                }
                return HANDLED;
            }
        };

        /* AND EAX,Id */
        ops[0x225] = new OP() {
            final public int call() {
                reg_eax.dword(ANDD(Fetchd.call(), reg_eax.dword()));
                return HANDLED;
            }
        };

        /* SUB Ed,Gd */
        ops[0x229] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(SUBD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, SUBD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, SUBD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* SUB Gd,Ed */
        ops[0x22b] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(SUBD(Modrm.GetEArd[rm].dword(),r.dword()));
                } else {
                    r.dword(SUBD(Memory.mem_readd(getEaa(rm)),r.dword()));
                }
                return HANDLED;
            }
        };

        /* SUB EAX,Id */
        ops[0x22d] = new OP() {
            final public int call() {
                reg_eax.dword(SUBD(Fetchd.call(), reg_eax.dword()));
                return HANDLED;
            }
        };

        /* XOR Ed,Gd */
        ops[0x231] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Reg r = Modrm.GetEArd[rm];
                    r.dword(XORD(Modrm.Getrd[rm].dword(), r.dword()));
                }
                else {
                    long eaa = getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            Memory.host_writed(index, XORD(Modrm.Getrd[rm].dword(),Memory.host_readd(index)));
                            return HANDLED;
                        }
                    }
                    Memory.mem_writed(eaa, XORD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa)));
                }
                return HANDLED;
            }
        };

        /* XOR Gd,Ed */
        ops[0x233] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                Reg r = Modrm.Getrd[rm];
                if (rm >= 0xc0 ) {
                    r.dword(XORD(Modrm.GetEArd[rm].dword(), r.dword()));
                } else {
                    r.dword(XORD(Memory.mem_readd(getEaa(rm)), r.dword()));
                }
                return HANDLED;
            }
        };

        /* XOR EAX,Id */
        ops[0x235] = new OP() {
            final public int call() {
                reg_eax.dword(XORD(Fetchd.call(), reg_eax.dword()));
                return HANDLED;
            }
        };

        /* CMP Ed,Gd */
        ops[0x239] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    CMPD(Modrm.Getrd[rm].dword(), Modrm.GetEArd[rm].dword());
                }
                else {
                    long eaa = getEaa(rm);
                    CMPD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa));
                }
                return HANDLED;
            }
        };

        /* CMP Gd,Ed */
        ops[0x23b] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    CMPD(Modrm.GetEArd[rm].dword(), Modrm.Getrd[rm].dword());
                } else {
                    CMPD(Memory.mem_readd(getEaa(rm)), Modrm.Getrd[rm].dword());
                }
                return HANDLED;
            }
        };

        /* CMP EAX,Id */
        ops[0x23d] = new OP() {
            final public int call() {
                CMPD(Fetchd.call(), reg_eax.dword());
                return HANDLED;
            }
        };

        /* INC EAX */
        ops[0x240] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_eax.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_eax.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC ECX */
        ops[0x241] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_ecx.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_ecx.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC EDX */
        ops[0x242] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_edx.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_edx.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC EBX */
        ops[0x243] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_ebx.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_ebx.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC ESP */
        ops[0x244] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_esp.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_esp.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC EBP */
        ops[0x245] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_ebp.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_ebp.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC ESI */
        ops[0x246] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_esi.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_esi.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* INC EDI */
        ops[0x247] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_edi.dword();
                lflags.res = (lflags.var1+1) & 0xFFFFFFFFl;
                reg_edi.dword(lflags.res);
                lflags.type=t_INCd;
                return HANDLED;
            }
        };

        /* DEC EAX */
        ops[0x248] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_eax.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_eax.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC ECX */
        ops[0x249] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_ecx.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_ecx.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC EDX */
        ops[0x24a] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_edx.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_edx.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC EBX */
        ops[0x24b] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_ebx.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_ebx.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC ESP */
        ops[0x24c] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_esp.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_esp.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC EBP */
        ops[0x24d] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_ebp.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_ebp.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC ESI */
        ops[0x24e] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_esi.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_esi.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* DEC EDI */
        ops[0x24f] = new OP() {
            final public int call() {
                LoadCF();lflags.var1 = reg_edi.dword();
                lflags.res = (lflags.var1-1) & 0xFFFFFFFFl;
                reg_edi.dword(lflags.res);
                lflags.type=t_DECd;
                return HANDLED;
            }
        };

        /* PUSH EAX */
        ops[0x250] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_eax.dword());
                return HANDLED;
            }
        };

        /* PUSH ECX */
        ops[0x251] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_ecx.dword());
                return HANDLED;
            }
        };

        /* PUSH EDX */
        ops[0x252] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_edx.dword());
                return HANDLED;
            }
        };

        /* PUSH EBX */
        ops[0x253] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_ebx.dword());
                return HANDLED;
            }
        };

        /* PUSH ESP */
        ops[0x254] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_esp.dword());
                return HANDLED;
            }
        };

        /* PUSH EBP */
        ops[0x255] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_ebp.dword());
                return HANDLED;
            }
        };

        /* PUSH ESI */
        ops[0x256] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_esi.dword());
                return HANDLED;
            }
        };

        /* PUSH EDI */
        ops[0x257] = new OP() {
            final public int call() {
                CPU.CPU_Push32(reg_edi.dword());
                return HANDLED;
            }
        };

        /* POP EAX */
        ops[0x258] = new OP() {
            final public int call() {
                reg_eax.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP ECX */
        ops[0x259] = new OP() {
            final public int call() {
                reg_ecx.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP EDX */
        ops[0x25a] = new OP() {
            final public int call() {
                reg_edx.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP EBX */
        ops[0x25b] = new OP() {
            final public int call() {
                reg_ebx.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP ESP */
        ops[0x25c] = new OP() {
            final public int call() {
                reg_esp.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP EBP */
        ops[0x25d] = new OP() {
            final public int call() {
                reg_ebp.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP ESI */
        ops[0x25e] = new OP() {
            final public int call() {
                reg_esi.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* POP EDI */
        ops[0x25f] = new OP() {
            final public int call() {
                reg_edi.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* PUSHAD */
        ops[0x260] = new OP() {
            final public int call() {
                /*Bitu*/long tmpesp = reg_esp.dword();
                CPU.CPU_Push32(reg_eax.dword());CPU.CPU_Push32(reg_ecx.dword());CPU.CPU_Push32(reg_edx.dword());CPU.CPU_Push32(reg_ebx.dword());
                CPU.CPU_Push32(tmpesp);CPU.CPU_Push32(reg_ebp.dword());CPU.CPU_Push32(reg_esi.dword());CPU.CPU_Push32(reg_edi.dword());
                return HANDLED;
            }
        };

        /* POPAD */
        ops[0x261] = new OP() {
            final public int call() {
                reg_edi.dword(CPU.CPU_Pop32());
                reg_esi.dword(CPU.CPU_Pop32());
                reg_ebp.dword(CPU.CPU_Pop32());CPU.CPU_Pop32();//Don't save ESP
                reg_ebx.dword(CPU.CPU_Pop32());
                reg_edx.dword(CPU.CPU_Pop32());
                reg_ecx.dword(CPU.CPU_Pop32());
                reg_eax.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* BOUND Ed */
        ops[0x262] = new OP() {
            final public int call() {
                /*Bit32s*/int bound_min, bound_max;
                /*Bit8u*/short rm=Fetchb.call();/*PhysPt*/long eaa=getEaa(rm);
                bound_min=(int)Memory.mem_readd(eaa);
                bound_max=(int)Memory.mem_readd(eaa+4);
                int rmrd = (int)(Modrm.Getrd[rm].dword() & 0xFFFFFFFFl);
                if (rmrd < bound_min || rmrd > bound_max) {
                    return EXCEPTION(5);
                }
                return HANDLED;
            }
        };

        /* ARPL Ed,Rd */
        ops[0x263] = new OP() {
            final public int call() {
                if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return ILLEGAL_OPCODE;
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    int_ref_1.value = (int) Modrm.GetEArd[rm].dword();
                    CPU.CPU_ARPL(int_ref_1,Modrm.Getrw[rm].word());
                    Modrm.GetEArd[rm].dword(int_ref_1.value);
                } else {
                    /*PhysPt*/long eaa=getEaa(rm);int_ref_1.value = Memory.mem_readw(eaa);
                    CPU.CPU_ARPL(int_ref_1,Modrm.Getrw[rm].word());
                    Memory.mem_writed(eaa,int_ref_1.value);
                }
                return HANDLED;
            }
        };

        /* PUSH Id */
        ops[0x268] = new OP() {
            final public int call() {
                CPU.CPU_Push32(Fetchd.call());
                return HANDLED;
            }
        };

        /* IMUL Gd,Ed,Id */
        ops[0x269] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    long op3 = Fetchds.call();
                    Modrm.Getrd[rm].dword(DIMULD(Modrm.GetEArd[rm].dword(),op3));
                }
                else {
                    long eaa = getEaa(rm);
                    long op3 = Fetchds.call();
                    Modrm.Getrd[rm].dword(DIMULD(Memory.mem_readd(eaa),op3));
                }
                return HANDLED;
            }
        };

        /* PUSH Ib */
        ops[0x26a] = new OP() {
            final public int call() {
                CPU.CPU_Push32(Fetchbs.call());
                return HANDLED;
            }
        };

        /* IMUL Gd,Ed,Ib */
        ops[0x26b] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    long op3 = Fetchbs.call();
                    Modrm.Getrd[rm].dword(DIMULD(Modrm.GetEArd[rm].dword(),op3));
                }
                else {
                    long eaa = getEaa(rm);
                    long op3 = Fetchbs.call();
                    Modrm.Getrd[rm].dword(DIMULD(Memory.mem_readd(eaa),op3));
                }
                return HANDLED;
            }
        };

        /* INSD */
        ops[0x26d] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),4)) return RUNEXCEPTION();
                DoString(R_INSD);
                return HANDLED;
            }
        };

        /* OUTSD */
        ops[0x26f] = new OP() {
            final public int call() {
                if (CPU.CPU_IO_Exception(reg_edx.word(),4)) return RUNEXCEPTION();
                DoString(R_OUTSD);
                return HANDLED;
            }
        };

        /* JO */
        ops[0x270] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_O());
                return CONTINUE;
            }
        };

        /* JNO */
        ops[0x271] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NO());
                return CONTINUE;
            }
        };

        /* JB */
        ops[0x272] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_B());
                return CONTINUE;
            }
        };

        /* JNB */
        ops[0x273] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NB());
                return CONTINUE;
            }
        };

        /* JZ */
        ops[0x274] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_Z());
                return CONTINUE;
            }
        };

        /* JNZ */
        ops[0x275] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NZ());
                return CONTINUE;
            }
        };

        /* JBE */
        ops[0x276] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_BE());
                return CONTINUE;
            }
        };

        /* JNBE */
        ops[0x277] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NBE());
                return CONTINUE;
            }
        };

        /* JS */
        ops[0x278] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_S());
                return CONTINUE;
            }
        };

        /* JNS */
        ops[0x279] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NS());
                return CONTINUE;
            }
        };

        /* JP */
        ops[0x27a] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_P());
                return CONTINUE;
            }
        };

        /* JNP */
        ops[0x27b] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NP());
                return CONTINUE;
            }
        };

        /* JL */
        ops[0x27c] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_L());
                return CONTINUE;
            }
        };

        /* JNL */
        ops[0x27d] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NL());
                return CONTINUE;
            }
        };

        /* JLE */
        ops[0x27e] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_LE());
                return CONTINUE;
            }
        };

        /* JNLE */
        ops[0x27f] = new OP() {
            final public int call() {
                JumpCond32_b(Flags.TFLG_NLE());
                return CONTINUE;
            }
        };

        /* Grpl Ed,Id */
        ops[0x281] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();/*Bitu*/int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    Reg r = Modrm.GetEArd[rm];
                    /*Bit32u*/long id=Fetchd.call();
                    switch (which) {
                    case 0x00:
                        r.dword(ADDD(id, r.dword()));break;
                    case 0x01: r.dword(ORD(id, r.dword()));break;
                    case 0x02:
                        r.dword(ADCD(id,r.dword()));break;
                    case 0x03:
                        r.dword(SBBD(id,r.dword()));break;
                    case 0x04:
                        r.dword(ANDD(id,r.dword()));break;
                    case 0x05:
                        r.dword(SUBD(id,r.dword()));break;
                    case 0x06:
                        r.dword(XORD(id,r.dword()));break;
                    case 0x07:CMPD(id, r.dword());break;
                    }
                } else {
                    long eaa = getEaa(rm);
                    /*Bit32u*/long id=Fetchd.call();
                    if ((eaa & 0xFFF) < 0xFFD) {
                        int addr = Paging.getDirectIndex(eaa);
                        if (addr>=0) {
                            switch (which) {
                            case 0x00:Memory.host_writed(addr, ADDD(id,Memory.host_readd(addr)));break;
                            case 0x01: Memory.host_writed(addr, ORD(id,Memory.host_readd(addr)));break;
                            case 0x02:Memory.host_writed(addr, ADCD(id,Memory.host_readd(addr)));break;
                            case 0x03:Memory.host_writed(addr, SBBD(id,Memory.host_readd(addr)));break;
                            case 0x04:Memory.host_writed(addr, ANDD(id,Memory.host_readd(addr)));break;
                            case 0x05:Memory.host_writed(addr, SUBD(id,Memory.host_readd(addr)));break;
                            case 0x06:Memory.host_writed(addr, XORD(id,Memory.host_readd(addr)));break;
                            case 0x07:CMPD(id,Memory.host_readd(addr));break;
                            }
                            return HANDLED;
                        }
                    }
                    switch (which) {
                    case 0x00:Memory.mem_writed(eaa, ADDD(id,Memory.mem_readd(eaa)));break;
                    case 0x01: Memory.mem_writed(eaa, ORD(id,Memory.mem_readd(eaa)));break;
                    case 0x02:Memory.mem_writed(eaa, ADCD(id,Memory.mem_readd(eaa)));break;
                    case 0x03:Memory.mem_writed(eaa, SBBD(id,Memory.mem_readd(eaa)));break;
                    case 0x04:Memory.mem_writed(eaa, ANDD(id,Memory.mem_readd(eaa)));break;
                    case 0x05:Memory.mem_writed(eaa, SUBD(id,Memory.mem_readd(eaa)));break;
                    case 0x06:Memory.mem_writed(eaa, XORD(id,Memory.mem_readd(eaa)));break;
                    case 0x07:CMPD(id,Memory.mem_readd(eaa));break;
                    }
                }
                return HANDLED;
            }
        };

        /* Grpl Ed,Ix */
        ops[0x283] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                /*Bitu*/int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    /*Bit32u*/long id=((int)Fetchbs.call()) & 0xFFFFFFFFl;
                    Reg r = Modrm.GetEArd[rm];
                    switch (which) {
                        case 0x00: r.dword(ADDD(id, r.dword()));break;
                        case 0x01: r.dword(ORD(id, r.dword()));break;
                        case 0x02: r.dword(ADCD(id, r.dword()));break;
                        case 0x03: r.dword(SBBD(id, r.dword()));break;
                        case 0x04: r.dword(ANDD(id, r.dword()));break;
                        case 0x05: r.dword(SUBD(id, r.dword()));break;
                        case 0x06: r.dword(XORD(id, r.dword()));break;
                        case 0x07: CMPD(id, r.dword());break;
                    }
                } else {
                    long eaa = getEaa(rm);

                    /*Bit32u*/long id=((int)Fetchbs.call()) & 0xFFFFFFFFl;
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            switch (which) {
                            case 0x00:Memory.host_writed(index, ADDD(id,Memory.host_readd(index)));break;
                            case 0x01: Memory.host_writed(index, ORD(id,Memory.host_readd(index)));break;
                            case 0x02:Memory.host_writed(index, ADCD(id,Memory.host_readd(index)));break;
                            case 0x03:Memory.host_writed(index, SBBD(id,Memory.host_readd(index)));break;
                            case 0x04:Memory.host_writed(index, ANDD(id,Memory.host_readd(index)));break;
                            case 0x05:Memory.host_writed(index, SUBD(id,Memory.host_readd(index)));break;
                            case 0x06:Memory.host_writed(index, XORD(id,Memory.host_readd(index)));break;
                            case 0x07:CMPD(id,Memory.host_readd(index));break;
                            }
                            return HANDLED;
                        }
                    }
                    switch (which) {
                    case 0x00:Memory.mem_writed(eaa, ADDD(id,Memory.mem_readd(eaa)));break;
                    case 0x01: Memory.mem_writed(eaa, ORD(id,Memory.mem_readd(eaa)));break;
                    case 0x02:Memory.mem_writed(eaa, ADCD(id,Memory.mem_readd(eaa)));break;
                    case 0x03:Memory.mem_writed(eaa, SBBD(id,Memory.mem_readd(eaa)));break;
                    case 0x04:Memory.mem_writed(eaa, ANDD(id,Memory.mem_readd(eaa)));break;
                    case 0x05:Memory.mem_writed(eaa, SUBD(id,Memory.mem_readd(eaa)));break;
                    case 0x06:Memory.mem_writed(eaa, XORD(id,Memory.mem_readd(eaa)));break;
                    case 0x07:CMPD(id,Memory.mem_readd(eaa));break;
                    }
                }
                return HANDLED;
            }
        };

        /* TEST Ed,Gd */
        ops[0x285] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    TESTD(Modrm.Getrd[rm].dword(), Modrm.GetEArd[rm].dword());
                }
                else {
                    long eaa = getEaa(rm);
                    TESTD(Modrm.Getrd[rm].dword(),Memory.mem_readd(eaa));
                }
                return HANDLED;
            }
        };

        /* XCHG Ed,Gd */
        ops[0x287] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                Reg rd = Modrm.Getrd[rm];
                /*Bit32u*/long oldrmrd= rd.dword();
                if (rm >= 0xc0 ) {
                    Reg eard = Modrm.GetEArd[rm];
                    rd.dword(eard.dword());
                    eard.dword(oldrmrd);
                } else {
                    /*PhysPt*/long eaa=getEaa(rm);
                    if ((eaa & 0xFFF)<0xFFD) {
                        int index = Paging.getDirectIndex(eaa);
                        if (index>=0) {
                            rd.dword(Memory.host_readd(index));
                            Memory.host_writed(index,oldrmrd);
                            return HANDLED;
                        }
                    }
                    rd.dword(Memory.mem_readd(eaa));
                    Memory.mem_writed(eaa,oldrmrd);
                }
                return HANDLED;
            }
        };

        /* MOV Ed,Gd */
        ops[0x289] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArd[rm].dword(Modrm.Getrd[rm].dword());
                } else {
                    /*PhysPt*/long eaa=getEaa(rm);Memory.mem_writed(eaa, Modrm.Getrd[rm].dword());
                }
                return HANDLED;
            }
        };

        /* MOV Gd,Ed */
        ops[0x28b] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Modrm.Getrd[rm].dword(Modrm.GetEArd[rm].dword());
                } else {
                    /*PhysPt*/long eaa=getEaa(rm);
                    Modrm.Getrd[rm].dword(Memory.mem_readd(eaa));
                }
                return HANDLED;
            }
        };

        /* Mov Ew,Sw */
        ops[0x28c] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();/*Bit16u*/long val;/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:					/* MOV Ew,ES */
                    val=CPU.Segs_ESval;break;
                case 0x01:					/* MOV Ew,CS */
                    val=CPU.Segs_CSval;break;
                case 0x02:					/* MOV Ew,SS */
                    val=CPU.Segs_SSval;break;
                case 0x03:					/* MOV Ew,DS */
                    val=CPU.Segs_DSval;break;
                case 0x04:					/* MOV Ew,FS */
                    val=CPU.Segs_FSval;break;
                case 0x05:					/* MOV Ew,GS */
                    val=CPU.Segs_GSval;break;
                default:
                    Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"CPU:8c:Illegal RM Byte");
                    return ILLEGAL_OPCODE;
                }
                if (rm >= 0xc0 ) {
                    Modrm.GetEArd[rm].dword(val);}
                else {/*PhysPt*/long eaa=getEaa(rm);Memory.mem_writew(eaa,(int)val);}
                return HANDLED;
            }
        };

        /* LEA Gd */
        ops[0x28d] = new OP() {
            final public int call() {
                //Little hack to always use segprefixed version
                /*Bit8u*/short rm=Fetchb.call();
                base_ds=base_ss=0;
                if (TEST_PREFIX_ADDR()!=0) {
                    Modrm.Getrd[rm].dword(getEaa32(rm));
                } else {
                    Modrm.Getrd[rm].dword(getEaa16(rm));
                }
                return HANDLED;
            }
        };

        /* POP Ed */
        ops[0x28f] = new OP() {
            final public int call() {
                /*Bit32u*/long val=CPU.CPU_Pop32();
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0 ) {
                    Modrm.GetEArd[rm].dword(val);}
                else {/*PhysPt*/long eaa=getEaa(rm);Memory.mem_writed(eaa,val);}
                return HANDLED;
            }
        };

        /* XCHG ECX,EAX */
        ops[0x291] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_ecx.dword());
                reg_ecx.dword(temp);
                return HANDLED;
            }
        };

        /* XCHG EDX,EAX */
        ops[0x292] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_edx.dword());
                reg_edx.dword(temp);
                return HANDLED;
            }
        };

        /* XCHG EBX,EAX */
        ops[0x293] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_ebx.dword());
                reg_ebx.dword(temp);
                return HANDLED;
            }
        };

        /* XCHG ESP,EAX */
        ops[0x294] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_esp.dword());
                reg_esp.dword(temp);
                return HANDLED;
            }
        };

        /* XCHG EBP,EAX */
        ops[0x295] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_ebp.dword());
                reg_ebp.dword(temp);
                return HANDLED;
            }
        };

        /* XCHG ESI,EAX */
        ops[0x296] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_esi.dword());
                reg_esi.dword(temp);
                return HANDLED;
            }
        };

        /* XCHG EDI,EAX */
        ops[0x297] = new OP() {
            final public int call() {
                /*Bit32u*/long temp= reg_eax.dword();
                reg_eax.dword(reg_edi.dword());
                reg_edi.dword(temp);
                return HANDLED;
            }
        };

        /* CWDE */
        ops[0x298] = new OP() {
            final public int call() {
                reg_eax.dword((short)reg_eax.word());
                return HANDLED;
            }
        };

        /* CDQ */
        ops[0x299] = new OP() {
            final public int call() {
                if ((reg_eax.dword() & 0x80000000l)!=0) reg_edx.dword(0xffffffffl);
                else reg_edx.dword(0);
                return HANDLED;
            }
        };

        /* CALL FAR Ad */
        ops[0x29a] = new OP() {
            final public int call() {
                /*Bit32u*/long newip=Fetchd.call();/*Bit16u*/int newcs=Fetchw.call();
                FillFlags();
                CPU.CPU_CALL(true,newcs,newip,GETIP());
                if (CPU_TRAP_CHECK)
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return CBRET_NONE;
                    }
                return CONTINUE;
            }
        };

        /* PUSHFD */
        ops[0x29c] = new OP() {
            final public int call() {
                if (CPU.CPU_PUSHF(true)) return RUNEXCEPTION();
                return HANDLED;
            }
        };

        /* POPFD */
        ops[0x29d] = new OP() {
            final public int call() {
                if (CPU.CPU_POPF(true)) return RUNEXCEPTION();
                if (CPU_TRAP_CHECK)
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return DECODE_END;
                    }
                if (CPU_PIC_CHECK)
                    if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END;
                return HANDLED;
            }
        };

        /* MOV EAX,Od */
        ops[0x2a1] = new OP() {
            final public int call() {
                reg_eax.dword(Memory.mem_readd(GetEADirect()));
                return HANDLED;
            }
        };

        /* MOV Od,EAX */
        ops[0x2a3] = new OP() {
            final public int call() {
                Memory.mem_writed(GetEADirect(), reg_eax.dword());
                return HANDLED;
            }
        };

        /* MOVSD */
        ops[0x2a5] = new OP() {
            final public int call() {
                DoString(R_MOVSD);
                return HANDLED;
            }
        };

        /* CMPSD */
        ops[0x2a7] = new OP() {
            final public int call() {
                DoString(R_CMPSD);
                return HANDLED;
            }
        };

        /* TEST EAX,Id */
        ops[0x2a9] = new OP() {
            final public int call() {
                TESTD(Fetchd.call(),reg_eax.dword());
                return HANDLED;
            }
        };

        /* STOSD */
        ops[0x2ab] = new OP() {
            final public int call() {
                DoString(R_STOSD);
                return HANDLED;
            }
        };

        /* LODSD */
        ops[0x2ad] = new OP() {
            final public int call() {
                DoString(R_LODSD);
                return HANDLED;
            }
        };

        /* SCASD */
        ops[0x2af] = new OP() {
            final public int call() {
                DoString(R_SCASD);
                return HANDLED;
            }
        };

        /* MOV EAX,Id */
        ops[0x2b8] = new OP() {
            final public int call() {
                reg_eax.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV ECX,Id */
        ops[0x2b9] = new OP() {
            final public int call() {
                reg_ecx.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV EDX,Iw */
        ops[0x2ba] = new OP() {
            final public int call() {
                reg_edx.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV EBX,Id */
        ops[0x2bb] = new OP() {
            final public int call() {
                reg_ebx.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV ESP,Id */
        ops[0x2bc] = new OP() {
            final public int call() {
                reg_esp.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV EBP.Id */
        ops[0x2bd] = new OP() {
            final public int call() {
                reg_ebp.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV ESI,Id */
        ops[0x2be] = new OP() {
            final public int call() {
                reg_esi.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* MOV EDI,Id */
        ops[0x2bf] = new OP() {
            final public int call() {
                reg_edi.dword(Fetchd.call());
                return HANDLED;
            }
        };

        /* GRP2 Ed,Ib */
        ops[0x2c1] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                /*Bitu*/int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    int blah = Fetchb.call();
                    /*Bit8u*/int val=blah & 0x1f;
                    if (val == 0) return HANDLED;

                    Reg r = Modrm.GetEArd[rm];
                    switch (which)	{
                    case 0x00:
                        r.dword(ROLD(val, r.dword()));break;
                    case 0x01:
                        r.dword(RORD(val, r.dword()));break;
                    case 0x02:
                        r.dword(RCLD(val, r.dword()));break;
                    case 0x03:
                        r.dword(RCRD(val, r.dword()));break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:
                        r.dword(SHLD(val,r.dword()));break;
                    case 0x05:
                        r.dword(SHRD(val,r.dword()));break;
                    case 0x07:
                        r.dword(SARD(val,r.dword()));break;
                    }
                } else {
                    long eaa = getEaa(rm);
                    int blah = Fetchb.call();
                    /*Bit8u*/int val=blah & 0x1f;
                    if (val == 0) return HANDLED;
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
                            return HANDLED;
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
                return HANDLED;
            }
        };

        /* RETN Iw */
        ops[0x2c2] = new OP() {
            final public int call() {
                reg_eip(CPU.CPU_Pop32());
                reg_esp.dword(reg_esp.dword() +Fetchw.call());
                return CONTINUE;
            }
        };

        /* RETN */
        ops[0x2c3] = new OP() {
            final public int call() {
                reg_eip(CPU.CPU_Pop32());
                return CONTINUE;
            }
        };

        /* LES */
        ops[0x2c4] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/long eaa=getEaa(rm);
                if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
                Modrm.Getrd[rm].dword(Memory.mem_readd(eaa));
                return HANDLED;
            }
        };

        /* LDS */
        ops[0x2c5] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0) return ILLEGAL_OPCODE;
                /*PhysPt*/long eaa=getEaa(rm);
                if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();
                Modrm.Getrd[rm].dword(Memory.mem_readd(eaa));
                return HANDLED;
            }
        };

        /* MOV Ed,Id */
        ops[0x2c7] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();
                if (rm >= 0xc0) {
                    Modrm.GetEArd[rm].dword(Fetchd.call());}
                else {/*PhysPt*/long eaa=getEaa(rm);Memory.mem_writed(eaa,Fetchd.call());}
                return HANDLED;
            }
        };

        /* ENTER Iw,Ib */
        ops[0x2c8] = new OP() {
            final public int call() {
                /*Bitu*/int bytes=Fetchw.call();
                /*Bitu*/int level=Fetchb.call();
                CPU.CPU_ENTER(true,bytes,level);
                return HANDLED;
            }
        };

        /* LEAVE */
        ops[0x2c9] = new OP() {
            final public int call() {
                reg_esp.dword(reg_esp.dword() & CPU.cpu.stack.notmask);
                reg_esp.dword(reg_esp.dword() | (reg_ebp.dword() & CPU.cpu.stack.mask));
                reg_ebp.dword(CPU.CPU_Pop32());
                return HANDLED;
            }
        };

        /* RETF Iw */
        ops[0x2ca] = new OP() {
            final public int call() {
                /*Bitu*/int words=Fetchw.call();
                FillFlags();
                CPU.CPU_RET(true,words,GETIP());
                return CONTINUE;
            }
        };

        /* RETF */
        ops[0x2cb] = new OP() {
            final public int call() {
                FillFlags();
                CPU.CPU_RET(true,0,GETIP());
                return CONTINUE;
            }
        };

        /* IRET */
        ops[0x2cf] = new OP() {
            final public int call() {
                CPU.CPU_IRET(true,GETIP());
                if (CPU_TRAP_CHECK)
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return CBRET_NONE;
                    }
                if (CPU_PIC_CHECK)
                    if (GETFLAG(IF)!=0 && Pic.PIC_IRQCheck!=0) return CBRET_NONE;
                return CONTINUE;
            }
        };

        /* GRP2 Ed,1 */
        ops[0x2d1] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                GRP2D(rm, 1);
                return HANDLED;
            }
        };

        /* GRP2 Ed,CL */
        ops[0x2d3] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();
                GRP2D(rm, reg_ecx.low());
                return HANDLED;
            }
        };

        /* LOOPNZ */
        ops[0x2e0] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    reg_ecx.dword(reg_ecx.dword() -1);
                    JumpCond32_b(reg_ecx.dword() !=0 && !get_ZF());
                } else {
                    reg_ecx.word(reg_ecx.word()-1);
                    JumpCond32_b(reg_ecx.word()!=0 && !get_ZF());
                }
                return CONTINUE;
            }
        };

        /* LOOPZ */
        ops[0x2e1] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    reg_ecx.dword(reg_ecx.dword() -1);
                    JumpCond32_b(reg_ecx.dword() !=0 && get_ZF());
                } else {
                    reg_ecx.word(reg_ecx.word()-1);
                    JumpCond32_b(reg_ecx.word()!=0 && get_ZF());
                }
                return CONTINUE;
            }
        };

        /* LOOP */
        ops[0x2e2] = new OP() {
            final public int call() {
                if (TEST_PREFIX_ADDR()!=0) {
                    reg_ecx.dword(reg_ecx.dword() -1);
                    JumpCond32_b(reg_ecx.dword() !=0);
                } else {
                    reg_ecx.word(reg_ecx.word()-1);
                    JumpCond32_b(reg_ecx.word()!=0);
                }
                return CONTINUE;
            }
        };

        /* JCXZ */
        ops[0x2e3] = new OP() {
            final public int call() {
                JumpCond32_b((reg_ecx.dword() & AddrMaskTable[prefixes& PREFIX_ADDR])==0);
                return CONTINUE;
            }
        };

        /* IN EAX,Ib */
        ops[0x2e5] = new OP() {
            final public int call() {
                /*Bitu*/int port=Fetchb.call();
                if (CPU.CPU_IO_Exception(port,4)) return RUNEXCEPTION();
                reg_eax.dword(IO.IO_ReadD(port));
                return HANDLED;
            }
        };

        /* OUT Ib,EAX */
        ops[0x2e7] = new OP() {
            final public int call() {
                /*Bitu*/int port=Fetchb.call();
                if (CPU.CPU_IO_Exception(port,4)) return RUNEXCEPTION();
                IO.IO_WriteD(port, reg_eax.dword());
                return HANDLED;
            }
        };

        /* CALL Jd */
        ops[0x2e8] = new OP() {
            final public int call() {
                /*Bit32s*/int addip=Fetchds.call();
                SAVEIP();
                CPU.CPU_Push32(reg_eip());
                reg_eip(reg_eip()+addip);
                return CONTINUE;
            }
        };

        /* JMP Jd */
        ops[0x2e9] = new OP() {
            final public int call() {
                /*Bit32s*/int addip=Fetchds.call();
                SAVEIP();
                reg_eip(reg_eip()+addip);
                return CONTINUE;
            }
        };

        /* JMP Ad */
        ops[0x2ea] = new OP() {
            final public int call() {
                /*Bit32u*/long newip=Fetchd.call();
                /*Bit16u*/int newcs=Fetchw.call();
                FillFlags();
                CPU.CPU_JMP(true,newcs,(int)newip,GETIP());
                if (CPU_TRAP_CHECK)
                    if (GETFLAG(TF)!=0) {
                        CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                        return CBRET_NONE;
                    }
                return CONTINUE;
            }
        };

        /* JMP Jb */
        ops[0x2eb] = new OP() {
            final public int call() {
                /*Bit32s*/int addip=Fetchbs.call();
                SAVEIP();
                reg_eip(reg_eip()+addip);
                return CONTINUE;
            }
        };

        /* IN EAX,DX */
        ops[0x2ed] = new OP() {
            final public int call() {
                reg_eax.dword(IO.IO_ReadD(reg_edx.word()));
                return HANDLED;
            }
        };

        /* OUT DX,EAX */
        ops[0x2ef] = new OP() {
            final public int call() {
                IO.IO_WriteD(reg_edx.word(),reg_eax.dword());
                return HANDLED;
            }
        };

        /* GRP3 Ed(,Id) */
        ops[0x2f7] = new OP() {
            final public int call() {
                /*Bit8u*/final short rm=Fetchb.call();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:											/* TEST Ed,Id */
                case 0x01:											/* TEST Ed,Id Undocumented*/
                    {
                        if (rm >= 0xc0 ) {
                            TESTD(Fetchd.call(), Modrm.GetEArd[rm].dword());
                        }
                        else {
                            long eaa = getEaa(rm);
                            TESTD(Fetchd.call(),Memory.mem_readd(eaa));
                        }
                        break;
                    }
                case 0x02:											/* NOT Ed */
                    {
                        if (rm >= 0xc0 ) {
                            Reg r = Modrm.GetEArd[rm];
                            r.dword(~r.dword());
                        } else {
                            /*PhysPt*/long eaa=getEaa(rm);
                            if ((eaa & 0xFFF)<0xFFD) {
                                int index = Paging.getDirectIndex(eaa);
                                if (index>=0) {
                                    Memory.host_writed(index,~Memory.host_readd(index));
                                    break;
                                }
                            }
                            Memory.mem_writed(eaa,~Memory.mem_readd(eaa));
                        }
                        break;
                    }
                case 0x03:											/* NEG Ed */
                    {
                        lflags.type=t_NEGd;
                        if (rm >= 0xc0 ) {
                            Reg r = Modrm.GetEArd[rm];
                            Flags.lf_var1d(r.dword());Flags.lf_resd(0-Flags.lf_var1d());
                            r.dword(Flags.lf_resd());
                        } else {
                            /*PhysPt*/long eaa=getEaa(rm);
                            if ((eaa & 0xFFF)<0xFFD) {
                                int index = Paging.getDirectIndex(eaa);
                                if (index>=0) {
                                    Flags.lf_var1d(Memory.host_readd(index));
                                    Flags.lf_resd(0-Flags.lf_var1d());
                                    Memory.host_writed(index,Flags.lf_resd());
                                    break;
                                }
                            }
                            Flags.lf_var1d(Memory.mem_readd(eaa));
                            Flags.lf_resd(0-Flags.lf_var1d());
                            Memory.mem_writed(eaa,Flags.lf_resd());
                        }
                        break;
                    }
                case 0x04:											/* MUL EAX,Ed */
                    if (rm >= 0xc0 ) {
                        MULD(Modrm.GetEArd[rm].dword());
                    }
                    else {
                        long eaa = getEaa(rm);
                        MULD(Memory.mem_readd(eaa));
                    }
                    break;
                case 0x05:											/* IMUL EAX,Ed */
                    if (rm >= 0xc0 ) {
                        IMULD(Modrm.GetEArd[rm].dword());
                    }
                    else {
                        long eaa = getEaa(rm);
                        IMULD(Memory.mem_readd(eaa));
                    }
                    break;
                case 0x06:											/* DIV Ed */
                    if (rm >= 0xc0 ) {
                        DIVD(Modrm.GetEArd[rm].dword());
                    }
                    else {
                        long eaa = getEaa(rm);
                        DIVD(Memory.mem_readd(eaa));
                    }
                    break;
                case 0x07:											/* IDIV Ed */
                    if (rm >= 0xc0 ) {
                        IDIVD(Modrm.GetEArd[rm].dword());
                    }
                    else {
                        long eaa = getEaa(rm);
                        IDIVD(Memory.mem_readd(eaa));
                    }
                    break;
                }
                return HANDLED;
            }
        };

        /* GRP 5 Ed */
        ops[0x2ff] = new OP() {
            final public int call() {
                /*Bit8u*/short rm=Fetchb.call();/*Bitu*/int which=(rm>>3)&7;
                switch (which) {
                case 0x00:											/* INC Ed */
                    if (rm >= 0xc0 ) {
                        Reg r = Modrm.GetEArd[rm];
                        r.dword(INCD(r.dword()));
                    }
                    else {
                        long eaa = getEaa(rm);
                        if ((eaa & 0xFFF)<0xFFD) {
                            int index = Paging.getDirectIndex(eaa);
                            if (index>=0) {
                                Memory.host_writed(index, INCD(Memory.host_readd(index)));
                                break;
                            }
                        }
                        Memory.mem_writed(eaa, INCD(Memory.mem_readd(eaa)));
                    }
                    break;
                case 0x01:											/* DEC Ed */
                    if (rm >= 0xc0 ) {
                        Reg r = Modrm.GetEArd[rm];
                        r.dword(DECD(r.dword()));
                    }
                    else {
                        long eaa = getEaa(rm);
                        if ((eaa & 0xFFF)<0xFFD) {
                            int addr = Paging.getDirectIndex(eaa);
                            if (addr>=0) {
                                Memory.host_writed(addr, DECD(Memory.host_readd(addr)));
                                break;
                            }
                        }
                        Memory.mem_writed(eaa, DECD(Memory.mem_readd(eaa)));
                    }
                    break;
                case 0x02:											/* CALL NEAR Ed */
                    if (rm >= 0xc0 ) {reg_eip(Modrm.GetEArd[rm].dword());}
                    else {/*PhysPt*/long eaa=getEaa(rm);reg_eip(Memory.mem_readd(eaa));}
                    CPU.CPU_Push32(GETIP());
                    return CONTINUE;
                case 0x03:											/* CALL FAR Ed */
                    {
                        if (rm >= 0xc0) return ILLEGAL_OPCODE;
                        /*PhysPt*/long eaa=getEaa(rm);
                        /*Bit32u*/long newip=Memory.mem_readd(eaa);
                        /*Bit16u*/int newcs=Memory.mem_readw(eaa+4);
                        FillFlags();
                        CPU.CPU_CALL(true,newcs,newip,GETIP());
                        if (CPU_TRAP_CHECK)
                            if (GETFLAG(TF)!=0) {
                                CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                                return CBRET_NONE;
                            }
                        return CONTINUE;
                    }
                case 0x04:											/* JMP NEAR Ed */
                    if (rm >= 0xc0 ) {reg_eip(Modrm.GetEArd[rm].dword());}
                    else {/*PhysPt*/long eaa=getEaa(rm);reg_eip(Memory.mem_readd(eaa));}
                    return CONTINUE;
                case 0x05:											/* JMP FAR Ed */
                    {
                        if (rm >= 0xc0) return ILLEGAL_OPCODE;
                        /*PhysPt*/long eaa=getEaa(rm);
                        /*Bit32u*/long newip=Memory.mem_readd(eaa);
                        /*Bit16u*/int newcs=Memory.mem_readw(eaa+4);
                        FillFlags();
                        CPU.CPU_JMP(true,newcs,(int)newip,GETIP());
                        if (CPU_TRAP_CHECK)
                            if (GETFLAG(TF)!=0) {
                                CPU.cpudecoder=Core_normal.CPU_Core_Normal_Trap_Run;
                                return CBRET_NONE;
                            }
                        return CONTINUE;
                    }
                case 0x06:											/* Push Ed */
                    if (rm >= 0xc0 ) {CPU.CPU_Push32(Modrm.GetEArd[rm].dword());}
                    else {/*PhysPt*/long eaa=getEaa(rm);CPU.CPU_Push32(Memory.mem_readd(eaa));}
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"CPU:66:GRP5:Illegal call "+Integer.toString(which,16));
                    return ILLEGAL_OPCODE;
                }
                return HANDLED;
            }
        };
    }
}