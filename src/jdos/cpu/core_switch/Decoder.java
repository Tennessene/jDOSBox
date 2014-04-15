package jdos.cpu.core_switch;

import jdos.cpu.*;
import jdos.cpu.core_dynamic.*;
import jdos.fpu.FPU;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Decoder extends Helper {
    static protected boolean rep_zero=false;

    private static void fastBlock(SwitchBlock block, int rm, int inc) {
        if (EA16) {
            fastBlock16(block, rm, inc);
        } else {
            fastBlock32(block, rm, inc);
        }
        if (block.eaa_segPhys ==null) {
            block.eaa_segPhys = reg_dsPhys;
            block.eaa_segVal = reg_dsVal;
        }
    }

    private static void sib(SwitchBlock block, int mode, int inc) {
        block.instruction+=inc;
        int sib = decode_fetchb();
        switch (sib&7) {
            case 0:	block.eaa_r1 = reg_eax;break;
            case 1:	block.eaa_r1 = reg_ecx;break;
            case 2: block.eaa_r1 = reg_edx;break;
            case 3:	block.eaa_r1 = reg_ebx;break;
            case 4:	if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1 = reg_esp;break;
            case 5:	/* #1 Base */
                if (mode==0) {
                    block.eaa_const = decode_fetchd();
                } else {
                    if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;}
                    block.eaa_r1 = reg_ebp;
                }
                break;
            case 6:	block.eaa_r1 = reg_esi;break;
            case 7: block.eaa_r1 = reg_edi;break;
        }
        int index =(sib >> 3) & 7;
        block.eaa_sib = sib>>6;
        switch (index) {
            case 0: block.eaa_r2 = reg_eax; break;
            case 1: block.eaa_r2 = reg_ecx; break;
            case 2: block.eaa_r2 = reg_edx; break;
            case 3: block.eaa_r2 = reg_ebx; break;
            case 4: block.eaa_r2 = reg_zero; break;
            case 5: block.eaa_r2 = reg_ebp; break;
            case 6: block.eaa_r2 = reg_esi; break;
            case 7: block.eaa_r2 = reg_edi; break;
        }
    }
    private static void fastBlock16(SwitchBlock block, int rm, int inc) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: block.eaa_r1=reg_ebx;block.eaa_r2=reg_esi;break;
                case 0x01: block.eaa_r1=reg_ebx;block.eaa_r2=reg_edi;break;
                case 0x02: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_esi;break;
                case 0x03: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_edi;break;
                case 0x04: block.eaa_r1=reg_esi;block.eaa_r2=reg_zero;break;
                case 0x05: block.eaa_r1=reg_edi;block.eaa_r2=reg_zero;break;
                case 0x06: block.eaa_r1=reg_zero;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchw(); break;
                case 0x07: block.eaa_r1=reg_ebx;block.eaa_r2=reg_zero;break;
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: block.eaa_r1=reg_ebx;block.eaa_r2=reg_esi; block.eaa_const=decode_fetchbs();break;
                case 0x01: block.eaa_r1=reg_ebx;block.eaa_r2=reg_edi; block.eaa_const=decode_fetchbs();break;
                case 0x02: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_esi; block.eaa_const=decode_fetchbs();break;
                case 0x03: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_edi; block.eaa_const=decode_fetchbs();break;
                case 0x04: block.eaa_r1=reg_esi;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchbs();break;
                case 0x05: block.eaa_r1=reg_edi;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchbs();break;
                case 0x06: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchbs(); break;
                case 0x07: block.eaa_r1=reg_ebx;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchbs();break;
            }
        } else {
            switch (rm & 7) {
                case 0x00: block.eaa_r1=reg_ebx;block.eaa_r2=reg_esi; block.eaa_const=decode_fetchws();break;
                case 0x01: block.eaa_r1=reg_ebx;block.eaa_r2=reg_edi; block.eaa_const=decode_fetchws();break;
                case 0x02: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_esi; block.eaa_const=decode_fetchws();break;
                case 0x03: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_edi; block.eaa_const=decode_fetchws();break;
                case 0x04: block.eaa_r1=reg_esi;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchws();break;
                case 0x05: block.eaa_r1=reg_edi;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchws();break;
                case 0x06: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1=reg_ebp;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchws(); break;
                case 0x07: block.eaa_r1=reg_ebx;block.eaa_r2=reg_zero; block.eaa_const=decode_fetchws();break;
            }
        }
    }
    private static void fastBlock32(SwitchBlock block, int rm, int inc) {
        block.instruction+=inc;
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: block.eaa_r1 = CPU_Regs.reg_eax; break;
                case 0x01: block.eaa_r1 = CPU_Regs.reg_ecx; break;
                case 0x02: block.eaa_r1 = CPU_Regs.reg_edx; break;
                case 0x03: block.eaa_r1 = CPU_Regs.reg_ebx; break;
                case 0x04: sib(block, 0, inc); break;
                case 0x05: block.eaa_const = decode_fetchd();block.eaa_r1=CPU_Regs.reg_zero;break;
                case 0x06: block.eaa_r1 = CPU_Regs.reg_esi; break;
                case 0x07: block.eaa_r1 = CPU_Regs.reg_edi; break;
            }
        } else if (rm<0x80) {
            switch (rm & 7) {
                case 0x00: block.eaa_r1 = CPU_Regs.reg_eax; block.eaa_const = decode_fetchbs(); break;
                case 0x01: block.eaa_r1 = CPU_Regs.reg_ecx; block.eaa_const = decode_fetchbs(); break;
                case 0x02: block.eaa_r1 = CPU_Regs.reg_edx; block.eaa_const = decode_fetchbs(); break;
                case 0x03: block.eaa_r1 = CPU_Regs.reg_ebx; block.eaa_const = decode_fetchbs(); break;
                case 0x04: sib(block, 1, inc); block.eaa_const+=decode_fetchbs(); break;
                case 0x05: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1 = CPU_Regs.reg_ebp; block.eaa_const = decode_fetchbs(); break;
                case 0x06: block.eaa_r1 = CPU_Regs.reg_esi; block.eaa_const = decode_fetchbs(); break;
                case 0x07: block.eaa_r1 = CPU_Regs.reg_edi; block.eaa_const = decode_fetchbs(); break;
            }
        } else {
            switch (rm & 7) {
                case 0x00: block.eaa_r1 = CPU_Regs.reg_eax; block.eaa_const = decode_fetchds(); break;
                case 0x01: block.eaa_r1 = CPU_Regs.reg_ecx; block.eaa_const = decode_fetchds(); break;
                case 0x02: block.eaa_r1 = CPU_Regs.reg_edx; block.eaa_const = decode_fetchds(); break;
                case 0x03: block.eaa_r1 = CPU_Regs.reg_ebx; block.eaa_const = decode_fetchds(); break;
                case 0x04: sib(block, 1, inc); block.eaa_const+=decode_fetchds(); break;
                case 0x05: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1 = CPU_Regs.reg_ebp; block.eaa_const = decode_fetchds(); break;
                case 0x06: block.eaa_r1 = CPU_Regs.reg_esi; block.eaa_const = decode_fetchds(); break;
                case 0x07: block.eaa_r1 = CPU_Regs.reg_edi; block.eaa_const = decode_fetchds(); break;
            }
        }
    }
    private static void instruction8(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.eb(rm);
            block.r2=Mod.gb(rm);
        } else {
            block.instruction = i+2;
            block.r1=Mod.gb(rm);
            fastBlock(block, rm, 2);
        }
    }
    private static int instruction8_nofast(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.eb(rm);
            block.r2=Mod.gb(rm);
        } else {
            block.instruction = i+1;
            block.r1=Mod.gb(rm);
            fastBlock(block, rm, 1);
        }
        return rm;
    }
    private static void instruction(SwitchBlock block, int i, Reg reg, int value) {
        block.instruction = i;
        block.r1 = reg;
        block.value = value;
    }
    private static void instruction(SwitchBlock block, int i, Reg reg) {
        block.instruction = i;
        block.r1 = reg;
    }
    private static void instruction8_r(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.gb(rm);
            block.r2=Mod.eb(rm);
        } else {
            block.instruction = i+8;
            block.r1=Mod.gb(rm);
            fastBlock(block, rm, 2);
        }
    }
    private static void instruction8_r_nofast(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.gb(rm);
            block.r2=Mod.eb(rm);
        } else {
            block.instruction = i+4;
            block.r1=Mod.gb(rm);
            fastBlock(block, rm, 1);
        }
    }

    private static void doString(SwitchBlock block, int instruction, int type, int width) {
        block.instruction = instruction;
        block.value = type;
        block.eaa_const = prefixes;
        block.zero = rep_zero;
        block.eaa_sib = width;
        block.eaa16 = EA16;
        if (block.eaa_segPhys==null) {
            block.eaa_segPhys = CPU_Regs.reg_dsPhys;
            block.eaa_segVal = CPU_Regs.reg_dsVal;
        }
    }

    private static void doFastString(SwitchBlock block, int instruction) {
        block.instruction = instruction;
        if (block.eaa_segVal==null) {block.eaa_segVal=reg_dsVal;block.eaa_segPhys=reg_dsPhys;}
        if ((prefixes & Core.PREFIX_ADDR)==0) {
            if ((prefixes & Core.PREFIX_REP)==0) {
                // block.instruction+=0;
            } else {
                block.instruction+=1;
            }
        } else {
            if ((prefixes & Core.PREFIX_REP)==0) {
                block.instruction+=2;
            } else {
                block.instruction+=3;
            }
        }
    }

    private static void instruction(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.ed(rm);
            block.r2=Mod.gd(rm);
        } else {
            block.instruction = i+2;
            block.r1=Mod.gd(rm);
            fastBlock(block, rm, 2);
        }
    }

    private static void instructionSlow(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.ed(rm);
            block.r2=Mod.gd(rm);
        } else {
            block.instruction = i+1;
            block.r1=Mod.gd(rm);
            fastBlock(block, rm, 2);
            block.eaa16 = EA16;
        }
    }

    private static void instruction_nofast(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.ed(rm);
            block.r2=Mod.gd(rm);
        } else {
            block.instruction = i+1;
            block.r1=Mod.gd(rm);
            fastBlock(block, rm, 1);
        }
    }

    private static void instruction_r(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.gd(rm);
            block.r2=Mod.ed(rm);
        } else {
            block.instruction = i+8;
            block.r1=Mod.gd(rm);
            fastBlock(block, rm, 2);
        }
    }

    private static void instruction_r_nofast(SwitchBlock block, int i) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = i;
            block.r1=Mod.gd(rm);
            block.r2=Mod.ed(rm);
        } else {
            block.instruction = i+4;
            block.r1=Mod.gd(rm);
            fastBlock(block, rm, 1);
        }
    }

    private static void group2b(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: // ROLB
                if ((block.value & 0x7) == 0) {
                    if ((block.value & 0x18) != 0) {
                        block.instruction /= 2; // flags doesn't contain fast versions
                        block.instruction += Inst.ROLB_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0x7;
                    block.instruction += Inst.ROLB;
                }
                break;
            case 0x01: // RORB
                if ((block.value & 0x7) == 0) {
                    if ((block.value & 0x18) != 0) {
                        block.instruction /= 2; // flags doesn't contain fast versions
                        block.instruction += Inst.RORB_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0x7;
                    block.instruction += Inst.RORB;
                }
                break;
            case 0x02: // RCLB
                if (block.value % 9 == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    // :TODO: if block.value==1 then make a more efficient fast version
                    block.instruction += Inst.RCLB;
                }
                break;
            case 0x03: // RCRB
                if (block.value % 9 == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.RCRB;
                }
                break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:
                if (block.value == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.SHLB;
                }
                break;
            case 0x05: // SHRB
                if (block.value == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.SHRB;
                }
                break;
            case 0x07: // SARB
                if (block.value == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.SARB;
                }
                break;
        }
    }

    private static void group2w(SwitchBlock block , int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: // ROLW
                if ((block.value & 0xf)==0) {
                    if ((block.value & 0x10)!=0) {
                        block.instruction /= 2; // flags doesn't contain fast versions
                        block.instruction += Inst.ROLW_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0xf;
                    block.instruction += Inst.ROLW;
                }
                break;
            case 0x01: // RORW
                if ((block.value & 0xf)==0) {
                    if ((block.value & 0x10)!=0) {
                        block.instruction /= 2; // flags doesn't contain fast versions
                        block.instruction += Inst.RORW_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0xf;
                    block.instruction += Inst.RORW;
                }
                break;
            case 0x02: // RCLW
                if (block.value % 17 == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    // :TODO: if block.value==1 then make a more efficient fast version
                    block.instruction += Inst.RCLW;
                }
                break;
            case 0x03: // RCRW
                if (block.value % 17 == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.RCRW;
                }
                break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06:
                if (block.value == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.SHLW;
                }
                break;
            case 0x05: // SHRW
                if (block.value == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.SHRW;
                }
                break;
            case 0x07: // SARW
                if (block.value == 0) {
                    block.instruction = Inst.NOP;
                } else {
                    block.instruction += Inst.SARW;
                }
                break;
        }
    }

    public static CacheBlockDynRec CreateCacheBlock(CodePageHandlerDynRec codepage,/*PhysPt*/int start,/*Bitu*/int max_opcodes) {
        // initialize a load of variables
        decode.code_start=start;
        decode.code=start;
        decode.page.code=codepage;
        decode.page.index=start & 4095;
        decode.page.wmap=codepage.write_map;
        decode.page.invmap=codepage.invalidation_map;
        decode.page.first=start >>> 12;
        decode.active_block=decode.block=Cache.cache_openblock();
        decode.block.page.start=decode.page.index;
        decode.setTLB(start);
        codepage.AddCacheBlock(decode.block);
        decode.cycles = 0;
        boolean done = false;
        SwitchBlock block=null;
        int opcode;
        SwitchBlock[] inst = new SwitchBlock[1024];
        int currentInst = 0;

        if (CPU.cpu.code.big) {
            opcode_index=0x200;
            prefixes=1;
            EA16 = false;
        } else {
            opcode_index=0;
            prefixes=0;
            EA16 = true;
        }


        decode.op_start=decode.code;
        try {
            while (!done) {
                boolean doAnother = false;
                decode.cycles++;                
                decode.modifiedAlot = false;
                if (inst[currentInst]==null) {
                    inst[currentInst] = new SwitchBlock();
                }
                block = inst[currentInst];
                opcode=opcode_index+decode_fetchb();
                block.opCode=opcode;
                switch (opcode) {
                    case 0x000:
                    case 0x200:
                        instruction8(block, Inst.ADD_R8_R8);
                        break;                                     /* ADD Eb,Gb */
                    case 0x001:
                        instruction(block, Inst.ADD_R16_R16);
                        break;                                    /* ADD Ew,Gw */
                    case 0x201:
                        instruction(block, Inst.ADD_R32_R32);
                        break;                                    /* ADD Ed,Gd */
                    case 0x002:
                    case 0x202:
                        instruction8_r(block, Inst.ADD_R8_R8);
                        break;                                   /* ADD Gb,Eb */
                    case 0x003:
                        instruction_r(block, Inst.ADD_R16_R16);
                        break;                                  /* ADD Gw,Ew */
                    case 0x203:
                        instruction_r(block, Inst.ADD_R32_R32);
                        break;                                  /* ADD Gd,Ed */
                    case 0x004:
                    case 0x204:
                        instruction(block, Inst.ADD_R8, reg_eax, decode_fetchb());
                        break;               /* ADD AL,Ib */
                    case 0x005:
                        instruction(block, Inst.ADD_R16, reg_eax, decode_fetchw());
                        break;              /* ADD AX,Iw */
                    case 0x205:
                        instruction(block, Inst.ADD_R32, reg_eax, decode_fetchd());
                        break;              /* ADD EAX,Id */
                    case 0x006:
                        block.instruction = Inst.PUSH16_ES;
                        break;                                      /* PUSH ES */
                    case 0x007:
                        block.instruction = Inst.POP16_ES;
                        break;                                       /* POP ES */

                    case 0x008:
                    case 0x208:
                        instruction8(block, Inst.OR_R8_R8);
                        break;                                      /* OR Eb,Gb */
                    case 0x009:
                        instruction(block, Inst.OR_R16_R16);
                        break;                                     /* OR Ew,Gw */
                    case 0x209:
                        instruction(block, Inst.OR_R32_R32);
                        break;                                     /* OR Ed,Gd */
                    case 0x00a:
                    case 0x20a:
                        instruction8_r(block, Inst.OR_R8_R8);
                        break;                                    /* OR Gb,Eb */
                    case 0x00b:
                        instruction_r(block, Inst.OR_R16_R16);
                        break;                                   /* OR Gw,Ew */
                    case 0x20b:
                        instruction_r(block, Inst.OR_R32_R32);
                        break;                                   /* OR Gd,Ed */
                    case 0x00c:
                    case 0x20c:
                        instruction(block, Inst.OR_R8, reg_eax, decode_fetchb());
                        break;                /* OR AL,Ib */
                    case 0x00d:
                        instruction(block, Inst.OR_R16, reg_eax, decode_fetchw());
                        break;               /* OR AX,Iw */
                    case 0x20d:
                        instruction(block, Inst.OR_R32, reg_eax, decode_fetchd());
                        break;               /* OR EAX,Id */
                    case 0x00e:
                        block.instruction = Inst.PUSH16_CS;
                        break;                                      /* PUSH CS */
                    case 0x00f:
                        opcode_index += 0x100;
                        continue;                                                  /* 2 byte opcodes */
                    case 0x010:
                    case 0x210:
                        instruction8(block, Inst.ADC_R8_R8);
                        break;                                     /* ADC Eb,Gb */
                    case 0x011:
                        instruction(block, Inst.ADC_R16_R16);
                        break;                                    /* ADC Ew,Gw */
                    case 0x211:
                        instruction(block, Inst.ADC_R32_R32);
                        break;                                    /* ADC Ed,Gd */
                    case 0x012:
                    case 0x212:
                        instruction8_r(block, Inst.ADC_R8_R8);
                        break;                                   /* ADC Gb,Eb */
                    case 0x013:
                        instruction_r(block, Inst.ADC_R16_R16);
                        break;                                  /* ADC Gw,Ew */
                    case 0x213:
                        instruction_r(block, Inst.ADC_R32_R32);
                        break;                                  /* ADC Gd,Ed */
                    case 0x014:
                    case 0x214:
                        instruction(block, Inst.ADC_R8, reg_eax, decode_fetchb());
                        break;               /* ADC AL,Ib */
                    case 0x015:
                        instruction(block, Inst.ADC_R16, reg_eax, decode_fetchw());
                        break;              /* ADC AX,Iw */
                    case 0x215:
                        instruction(block, Inst.ADC_R32, reg_eax, decode_fetchd());
                        break;              /* ADC EAX,Id */
                    case 0x016:
                        block.instruction = Inst.PUSH16_SS;
                        break;                                      /* PUSH SS */
                    case 0x017:
                        block.instruction = Inst.POP16_SS;
                        CPU.CPU_Cycles++;
                        doAnother = true;
                        break;      /* POP SS */ //Always do another instruction
                    case 0x018:
                    case 0x218:
                        instruction8(block, Inst.SBB_R8_R8);
                        break;                                     /* SBB Eb,Gb */
                    case 0x019:
                        instruction(block, Inst.SBB_R16_R16);
                        break;                                    /* SBB Ew,Gw */
                    case 0x219:
                        instruction(block, Inst.SBB_R32_R32);
                        break;                                    /* SBB Ed,Gd */
                    case 0x01a:
                    case 0x21a:
                        instruction8_r(block, Inst.SBB_R8_R8);
                        break;                                   /* SBB Gb,Eb */
                    case 0x01b:
                        instruction_r(block, Inst.SBB_R16_R16);
                        break;                                  /* SBB Gw,Ew */
                    case 0x21b:
                        instruction_r(block, Inst.SBB_R32_R32);
                        break;                                  /* SBB Gd,Ed */
                    case 0x01c:
                    case 0x21c:
                        instruction(block, Inst.SBB_R8, reg_eax, decode_fetchb());
                        break;               /* SBB AL,Ib */
                    case 0x01d:
                        instruction(block, Inst.SBB_R16, reg_eax, decode_fetchw());
                        break;              /* SBB AX,Iw */
                    case 0x21d:
                        instruction(block, Inst.SBB_R32, reg_eax, decode_fetchd());
                        break;              /* SBB EAX,Id */
                    case 0x01e:
                        block.instruction = Inst.PUSH16_DS;
                        break;                                      /* PUSH DS */
                    case 0x01f:
                        block.instruction = Inst.POP16_DS;
                        break;                                       /* POP DS */                                     /* 2 byte opcodes */
                    case 0x020:
                    case 0x220:
                        instruction8(block, Inst.AND_R8_R8);
                        break;                                     /* AND Eb,Gb */
                    case 0x021:
                        instruction(block, Inst.AND_R16_R16);
                        break;                                    /* AND Ew,Gw */
                    case 0x221:
                        instruction(block, Inst.AND_R32_R32);
                        break;                                    /* AND Ed,Gd */
                    case 0x022:
                    case 0x222:
                        instruction8_r(block, Inst.AND_R8_R8);
                        break;                                   /* AND Gb,Eb */
                    case 0x023:
                        instruction_r(block, Inst.AND_R16_R16);
                        break;                                  /* AND Gw,Ew */
                    case 0x223:
                        instruction_r(block, Inst.AND_R32_R32);
                        break;                                  /* AND Gd,Ed */
                    case 0x024:
                    case 0x224:
                        instruction(block, Inst.AND_R8, reg_eax, decode_fetchb());
                        break;               /* AND AL,Ib */
                    case 0x025:
                        instruction(block, Inst.AND_R16, reg_eax, decode_fetchw());
                        break;              /* AND AX,Iw */
                    case 0x225:
                        instruction(block, Inst.AND_R32, reg_eax, decode_fetchd());
                        break;              /* AND EAX,Id */
                    case 0x026:
                        block.eaa_segPhys = reg_esPhys;
                        block.eaa_segVal = CPU_Regs.reg_esVal;
                        continue;/* SEG ES: */
                    case 0x027:
                        block.instruction = Inst.DAA;
                        break;                                            /* DAA */
                    case 0x028:
                    case 0x228:
                        instruction8(block, Inst.SUB_R8_R8);
                        break;                                     /* SUB Eb,Gb */
                    case 0x029:
                        instruction(block, Inst.SUB_R16_R16);
                        break;                                    /* SUB Ew,Gw */
                    case 0x229:
                        instruction(block, Inst.SUB_R32_R32);
                        break;                                    /* SUB Ed,Gd */
                    case 0x02a:
                    case 0x22a:
                        instruction8_r(block, Inst.SUB_R8_R8);
                        break;                                   /* SUB Gb,Eb */
                    case 0x02b:
                        instruction_r(block, Inst.SUB_R16_R16);
                        break;                                  /* SUB Gw,Ew */
                    case 0x22b:
                        instruction_r(block, Inst.SUB_R32_R32);
                        break;                                  /* SUB Gd,Ed */
                    case 0x02c:
                    case 0x22c:
                        instruction(block, Inst.SUB_R8, reg_eax, decode_fetchb());
                        break;               /* SUB AL,Ib */
                    case 0x02d:
                        instruction(block, Inst.SUB_R16, reg_eax, decode_fetchw());
                        break;              /* SUB AX,Iw */
                    case 0x22d:
                        instruction(block, Inst.SUB_R32, reg_eax, decode_fetchd());
                        break;              /* SUB EAX,Id */
                    case 0x02e:
                        block.eaa_segPhys = reg_csPhys;
                        block.eaa_segVal = CPU_Regs.reg_csVal;
                        continue;/* SEG CS: */
                    case 0x02f:
                        block.instruction = Inst.DAS;
                        break;                                            /* DAS */
                    case 0x030:
                    case 0x230:
                        instruction8(block, Inst.XOR_R8_R8);
                        break;                                     /* XOR Eb,Gb */
                    case 0x031:
                        instruction(block, Inst.XOR_R16_R16);
                        break;                                    /* XOR Ew,Gw */
                    case 0x231:
                        instruction(block, Inst.XOR_R32_R32);
                        break;                                    /* XOR Ed,Gd */
                    case 0x032:
                    case 0x232:
                        instruction8_r(block, Inst.XOR_R8_R8);
                        break;                                   /* XOR Gb,Eb */
                    case 0x033:
                        instruction_r(block, Inst.XOR_R16_R16);
                        break;                                  /* XOR Gw,Ew */
                    case 0x233:
                        instruction_r(block, Inst.XOR_R32_R32);
                        break;                                  /* XOR Gd,Ed */
                    case 0x034:
                    case 0x234:
                        instruction(block, Inst.XOR_R8, reg_eax, decode_fetchb());
                        break;               /* XOR AL,Ib */
                    case 0x035:
                        instruction(block, Inst.XOR_R16, reg_eax, decode_fetchw());
                        break;              /* XOR AX,Iw */
                    case 0x235:
                        instruction(block, Inst.XOR_R32, reg_eax, decode_fetchd());
                        break;              /* XOR EAX,Id */
                    case 0x036:
                        block.eaa_segPhys = reg_ssPhys;
                        block.eaa_segVal = CPU_Regs.reg_ssVal;
                        continue;/* SEG SS: */
                    case 0x037:
                        block.instruction = Inst.AAA;
                        break;                                            /* AAA */
                    case 0x038:
                    case 0x238:
                        instruction8_nofast(block, Inst.CMP_R8_R8);
                        break;                              /* CMP Eb,Gb */
                    case 0x039:
                        instruction_nofast(block, Inst.CMP_R16_R16);
                        break;                             /* CMP Ew,Gw */
                    case 0x239:
                        instruction_nofast(block, Inst.CMP_R32_R32);
                        break;                             /* CMP Ed,Gd */
                    case 0x03a:
                    case 0x23a:
                        instruction8_r_nofast(block, Inst.CMP_R8_R8);
                        break;                            /* CMP Gb,Eb */
                    case 0x03b:
                        instruction_r_nofast(block, Inst.CMP_R16_R16);
                        break;                           /* CMP Gw,Ew */
                    case 0x23b:
                        instruction_r_nofast(block, Inst.CMP_R32_R32);
                        break;                           /* CMP Gd,Ed */
                    case 0x03c:
                    case 0x23c:
                        instruction(block, Inst.CMP_R8, reg_eax, decode_fetchb());
                        break;               /* CMP AL,Ib */
                    case 0x03d:
                        instruction(block, Inst.CMP_R16, reg_eax, decode_fetchw());
                        break;              /* CMP AX,Iw */
                    case 0x23d:
                        instruction(block, Inst.CMP_R32, reg_eax, decode_fetchd());
                        break;              /* CMP EAX,Id */
                    case 0x03e:
                        block.eaa_segPhys = reg_dsPhys;
                        block.eaa_segVal = CPU_Regs.reg_dsVal;
                        continue;/* SEG DS: */
                    case 0x03f:
                        block.instruction = Inst.AAS;
                        break;                                            /* AAS */
                    case 0x040:
                        instruction(block, Inst.INCW, reg_eax);
                        break;                                  /* INC AX */
                    case 0x041:
                        instruction(block, Inst.INCW, reg_ecx);
                        break;                                  /* INC CX */
                    case 0x042:
                        instruction(block, Inst.INCW, reg_edx);
                        break;                                  /* INC DX */
                    case 0x043:
                        instruction(block, Inst.INCW, reg_ebx);
                        break;                                  /* INC BX */
                    case 0x044:
                        instruction(block, Inst.INCW, reg_esp);
                        break;                                  /* INC SP */
                    case 0x045:
                        instruction(block, Inst.INCW, reg_ebp);
                        break;                                  /* INC BP */
                    case 0x046:
                        instruction(block, Inst.INCW, reg_esi);
                        break;                                  /* INC SI */
                    case 0x047:
                        instruction(block, Inst.INCW, reg_edi);
                        break;                                  /* INC DI */
                    case 0x048:
                        instruction(block, Inst.DECW, reg_eax);
                        break;                                  /* DEC AX */
                    case 0x049:
                        instruction(block, Inst.DECW, reg_ecx);
                        break;                                  /* DEC CX */
                    case 0x04a:
                        instruction(block, Inst.DECW, reg_edx);
                        break;                                  /* DEC DX */
                    case 0x04b:
                        instruction(block, Inst.DECW, reg_ebx);
                        break;                                  /* DEC BX */
                    case 0x04c:
                        instruction(block, Inst.DECW, reg_esp);
                        break;                                  /* DEC SP */
                    case 0x04d:
                        instruction(block, Inst.DECW, reg_ebp);
                        break;                                  /* DEC BP */
                    case 0x04e:
                        instruction(block, Inst.DECW, reg_esi);
                        break;                                  /* DEC SI */
                    case 0x04f:
                        instruction(block, Inst.DECW, reg_edi);
                        break;                                  /* DEC DI */

                    case 0x050:
                        instruction(block, Inst.PUSH16_R16, reg_eax);
                        break;                            /* PUSH AX */
                    case 0x051:
                        instruction(block, Inst.PUSH16_R16, reg_ecx);
                        break;                            /* PUSH CX */
                    case 0x052:
                        instruction(block, Inst.PUSH16_R16, reg_edx);
                        break;                            /* PUSH DX */
                    case 0x053:
                        instruction(block, Inst.PUSH16_R16, reg_ebx);
                        break;                            /* PUSH BX */
                    case 0x054:
                        instruction(block, Inst.PUSH16_R16, reg_esp);
                        break;                            /* PUSH SP */
                    case 0x055:
                        instruction(block, Inst.PUSH16_R16, reg_ebp);
                        break;                            /* PUSH BP */
                    case 0x056:
                        instruction(block, Inst.PUSH16_R16, reg_esi);
                        break;                            /* PUSH SI */
                    case 0x057:
                        instruction(block, Inst.PUSH16_R16, reg_edi);
                        break;                            /* PUSH DI */
                    case 0x058:
                        instruction(block, Inst.POP16_R16, reg_eax);
                        break;                             /* POP AX */
                    case 0x059:
                        instruction(block, Inst.POP16_R16, reg_ecx);
                        break;                             /* POP CX */
                    case 0x05a:
                        instruction(block, Inst.POP16_R16, reg_edx);
                        break;                             /* POP DX */
                    case 0x05b:
                        instruction(block, Inst.POP16_R16, reg_ebx);
                        break;                             /* POP BX */
                    case 0x05c:
                        instruction(block, Inst.POP16_R16, reg_esp);
                        break;                             /* POP SP */
                    case 0x05d:
                        instruction(block, Inst.POP16_R16, reg_ebp);
                        break;                             /* POP BP */
                    case 0x05e:
                        instruction(block, Inst.POP16_R16, reg_esi);
                        break;                             /* POP SI */
                    case 0x05f:
                        instruction(block, Inst.POP16_R16, reg_edi);
                        break;                             /* POP DI */
                    case 0x060:
                        block.instruction = Inst.PUSH16A;
                        break;                                          /* PUSHA */
                    case 0x061:
                        block.instruction = Inst.POP16A;
                        break;                                           /* POPA */
                    case 0x062: {                                                                               /* BOUND */
                        int rm = decode_fetchb();
                        block.r1 = Mod.gw(rm);
                        fastBlock(block, rm, 1);
                        block.instruction = Inst.BOUND16;
                        block.eaa16 = EA16;
                        break;
                    }
                    case 0x063:
                        instructionSlow(block, Inst.ARPL_R16_R16);
                        break;                               /* ARPL Ew,Rw */
                    case 0x064:
                        block.eaa_segPhys = reg_fsPhys;
                        block.eaa_segVal = CPU_Regs.reg_fsVal;
                        continue;/* SEG FS: */
                    case 0x065:
                        block.eaa_segPhys = reg_gsPhys;
                        block.eaa_segVal = CPU_Regs.reg_gsVal;
                        continue;/* SEG GS: */
                    case 0x066:
                        opcode_index = (CPU.cpu.code.big ? 0 : 512);
                        continue;                                /* Operand Size Prefix */
                    case 0x067:                                                                                 /* Address Size Prefix */
                        prefixes = (prefixes & ~Core.PREFIX_ADDR) | (CPU.cpu.code.big ? 0 : 1);
                        EA16 = (prefixes & 1) == 0;
                        continue;
                    case 0x068:
                        block.instruction = Inst.PUSH16;
                        block.value = decode_fetchw();
                        break;          /* PUSH Iw */
                    case 0x069:
                        instruction(block, Inst.IMUL_R16_R16);
                        block.value = decode_fetchws();
                        break;   /* IMUL Gw,Ew,Iw */
                    case 0x06a:
                        block.instruction = Inst.PUSH16;
                        block.value = decode_fetchbs();
                        break;         /* PUSH Ib */
                    case 0x06b:
                        instruction(block, Inst.IMUL_R16_R16);
                        block.value = decode_fetchbs();
                        break;   /* IMUL Gw,Ew,Ib */
                    case 0x06c:
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_INSB, 1);
                        break;              /* INSB */
                    case 0x06d:
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_INSW, 2);
                        break;              /* INSW */
                    case 0x06e:
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_OUTSB, 1);
                        break;             /* OUTSB */
                    case 0x06f:
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_OUTSW, 2);
                        break;             /* OUTSW */
                    case 0x070:
                        block.instruction = Inst.JUMP16_JO;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* JO */
                    case 0x071:
                        block.instruction = Inst.JUMP16_NJO;
                        block.value = decode_fetchbs();
                        done = true;
                        break;     /* NJO */
                    case 0x072:
                        block.instruction = Inst.JUMP16_B;
                        block.value = decode_fetchbs();
                        done = true;
                        break;       /* JB */
                    case 0x073:
                        block.instruction = Inst.JUMP16_NB;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* NB */
                    case 0x074:
                        block.instruction = Inst.JUMP16_Z;
                        block.value = decode_fetchbs();
                        done = true;
                        break;       /* Z */
                    case 0x075:
                        block.instruction = Inst.JUMP16_NZ;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* NZ */
                    case 0x076:
                        block.instruction = Inst.JUMP16_BE;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* BE */
                    case 0x077:
                        block.instruction = Inst.JUMP16_NBE;
                        block.value = decode_fetchbs();
                        done = true;
                        break;     /* NBE */
                    case 0x078:
                        block.instruction = Inst.JUMP16_S;
                        block.value = decode_fetchbs();
                        done = true;
                        break;       /* JS */
                    case 0x079:
                        block.instruction = Inst.JUMP16_NS;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* NS */
                    case 0x07a:
                        block.instruction = Inst.JUMP16_P;
                        block.value = decode_fetchbs();
                        done = true;
                        break;       /* P */
                    case 0x07b:
                        block.instruction = Inst.JUMP16_NP;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* NP */
                    case 0x07c:
                        block.instruction = Inst.JUMP16_L;
                        block.value = decode_fetchbs();
                        done = true;
                        break;       /* L */
                    case 0x07d:
                        block.instruction = Inst.JUMP16_NL;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* NL */
                    case 0x07e:
                        block.instruction = Inst.JUMP16_LE;
                        block.value = decode_fetchbs();
                        done = true;
                        break;      /* LE */
                    case 0x07f:
                        block.instruction = Inst.JUMP16_NLE;
                        block.value = decode_fetchbs();
                        done = true;
                        break;     /* NLE */

                    case 0x080:
                    case 0x280:
                    case 0x082:
                    case 0x282: {                                                                                            /* Grpl Eb,Ib */
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                            block.value = decode_fetchb();
                            switch (which) {
                                case 0x00:
                                    block.instruction = Inst.ADD_R8;
                                    break;
                                case 0x01:
                                    block.instruction = Inst.OR_R8;
                                    break;
                                case 0x02:
                                    block.instruction = Inst.ADC_R8;
                                    break;
                                case 0x03:
                                    block.instruction = Inst.SBB_R8;
                                    break;
                                case 0x04:
                                    block.instruction = Inst.AND_R8;
                                    break;
                                case 0x05:
                                    block.instruction = Inst.SUB_R8;
                                    break;
                                case 0x06:
                                    block.instruction = Inst.XOR_R8;
                                    break;
                                case 0x07:
                                    block.instruction = Inst.CMP_R8;
                                    break;
                            }
                        } else {
                            switch (which) {
                                case 0x00:
                                    block.instruction = Inst.ADD_E8_16;
                                    break;
                                case 0x01:
                                    block.instruction = Inst.OR_E8_16;
                                    break;
                                case 0x02:
                                    block.instruction = Inst.ADC_E8_16;
                                    break;
                                case 0x03:
                                    block.instruction = Inst.SBB_E8_16;
                                    break;
                                case 0x04:
                                    block.instruction = Inst.AND_E8_16;
                                    break;
                                case 0x05:
                                    block.instruction = Inst.SUB_E8_16;
                                    break;
                                case 0x06:
                                    block.instruction = Inst.XOR_E8_16;
                                    break;
                                case 0x07:
                                    block.instruction = Inst.CMP_E8_16;
                                    break;
                            }
                            fastBlock(block, rm, which == 7 ? 1 : 2);
                            block.value = decode_fetchb();
                        }
                        break;
                    }
                    case 0x081: {                                                                                            /* Grpl Ew,Iw */
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                            block.value = decode_fetchw();
                            switch (which) {
                                case 0x00:
                                    block.instruction = Inst.ADD_R16;
                                    break;
                                case 0x01:
                                    block.instruction = Inst.OR_R16;
                                    break;
                                case 0x02:
                                    block.instruction = Inst.ADC_R16;
                                    break;
                                case 0x03:
                                    block.instruction = Inst.SBB_R16;
                                    break;
                                case 0x04:
                                    block.instruction = Inst.AND_R16;
                                    break;
                                case 0x05:
                                    block.instruction = Inst.SUB_R16;
                                    break;
                                case 0x06:
                                    block.instruction = Inst.XOR_R16;
                                    break;
                                case 0x07:
                                    block.instruction = Inst.CMP_R16;
                                    break;
                            }
                        } else {
                            switch (which) {
                                case 0x00:
                                    block.instruction = Inst.ADD_E16_16;
                                    break;
                                case 0x01:
                                    block.instruction = Inst.OR_E16_16;
                                    break;
                                case 0x02:
                                    block.instruction = Inst.ADC_E16_16;
                                    break;
                                case 0x03:
                                    block.instruction = Inst.SBB_E16_16;
                                    break;
                                case 0x04:
                                    block.instruction = Inst.AND_E16_16;
                                    break;
                                case 0x05:
                                    block.instruction = Inst.SUB_E16_16;
                                    break;
                                case 0x06:
                                    block.instruction = Inst.XOR_E16_16;
                                    break;
                                case 0x07:
                                    block.instruction = Inst.CMP_E16_16;
                                    break;
                            }
                            fastBlock(block, rm, which == 7 ? 1 : 2);
                            block.value = decode_fetchw();
                        }
                        break;
                    }
                    case 0x083: {                                                                                            /* Grpl Ew,Ix */
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                            block.value = (((short) decode_fetchbs()) & 0xFFFF);
                            switch (which) {
                                case 0x00:
                                    block.instruction = Inst.ADD_R16;
                                    break;
                                case 0x01:
                                    block.instruction = Inst.OR_R16;
                                    break;
                                case 0x02:
                                    block.instruction = Inst.ADC_R16;
                                    break;
                                case 0x03:
                                    block.instruction = Inst.SBB_R16;
                                    break;
                                case 0x04:
                                    block.instruction = Inst.AND_R16;
                                    break;
                                case 0x05:
                                    block.instruction = Inst.SUB_R16;
                                    break;
                                case 0x06:
                                    block.instruction = Inst.XOR_R16;
                                    break;
                                case 0x07:
                                    block.instruction = Inst.CMP_R16;
                                    break;
                            }
                        } else {
                            switch (which) {
                                case 0x00:
                                    block.instruction = Inst.ADD_E16_16;
                                    break;
                                case 0x01:
                                    block.instruction = Inst.OR_E16_16;
                                    break;
                                case 0x02:
                                    block.instruction = Inst.ADC_E16_16;
                                    break;
                                case 0x03:
                                    block.instruction = Inst.SBB_E16_16;
                                    break;
                                case 0x04:
                                    block.instruction = Inst.AND_E16_16;
                                    break;
                                case 0x05:
                                    block.instruction = Inst.SUB_E16_16;
                                    break;
                                case 0x06:
                                    block.instruction = Inst.XOR_E16_16;
                                    break;
                                case 0x07:
                                    block.instruction = Inst.CMP_E16_16;
                                    break;
                            }
                            fastBlock(block, rm, which == 7 ? 1 : 2);
                            block.value = (((short) decode_fetchbs()) & 0xFFFF);
                        }
                        break;
                    }
                    case 0x084:
                    case 0x284:
                        instruction8_nofast(block, Inst.TEST_R8_R8);
                        break;                             /* TEST Eb,Gb */
                    case 0x085:
                        instruction_nofast(block, Inst.TEST_R16_R16);
                        break;                            /* TEST Ew,Gw */
                    case 0x086:
                    case 0x286:
                        instruction8_nofast(block, Inst.XCHG_R8_R8);
                        break;                             /* XCHG Eb,Gb */
                    case 0x087:
                        instruction_nofast(block, Inst.XCHG_R16_R16);
                        break;                            /* XCHG Ew,Gw */
                    case 0x088:
                    case 0x288:                                                                                 /* MOV Eb,Gb */
                        if (instruction8_nofast(block, Inst.MOV_R8_R8) == 5) {
                            block.instruction += 7;
                        }
                        break;
                    case 0x089:
                        instruction_nofast(block, Inst.MOV_R16_R16);
                        break;                             /* MOV Ew,Gw */
                    case 0x08a:
                    case 0x28a:
                        instruction8_r_nofast(block, Inst.MOV_R8_R8);
                        break;                            /* MOV Gb,Eb */
                    case 0x08b:
                        instruction_r_nofast(block, Inst.MOV_R16_R16);
                        break;                           /* MOV Gw,Ew */
                    case 0x08c: {                                                                               /* Mov Ew,Sw */
                        int rm = decode_fetchb();
                        block.instruction = Inst.MOV_R16_R16;
                        switch ((rm >> 3) & 7) {
                            case 0x00:					/* MOV Ew,ES */
                                block.r2 = CPU_Regs.reg_esVal;
                                break;
                            case 0x01:					/* MOV Ew,CS */
                                block.r2 = CPU_Regs.reg_csVal;
                                break;
                            case 0x02:					/* MOV Ew,SS */
                                block.r2 = CPU_Regs.reg_ssVal;
                                break;
                            case 0x03:					/* MOV Ew,DS */
                                block.r2 = CPU_Regs.reg_dsVal;
                                break;
                            case 0x04:					/* MOV Ew,FS */
                                block.r2 = CPU_Regs.reg_fsVal;
                                break;
                            case 0x05:					/* MOV Ew,GS */
                                block.r2 = CPU_Regs.reg_gsVal;
                                break;
                            default:
                                block.instruction = Inst.ILLEGAL;
                                done = true;
                                break;
                        }
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.r1 = block.r2;
                            block.instruction++;
                            fastBlock(block, rm, 1);
                        }
                        break;
                    }
                    case 0x08d: {                                                                               /* LEA Gw */
                        int rm = decode_fetchb();
                        block.instruction = Inst.LEA_R16_16;
                        block.r1 = Mod.gd(rm);
                        fastBlock(block, rm, 1);
                        block.eaa_segPhys = reg_zero;
                        block.eaa_segVal = reg_zero;
                        break;
                    }
                    case 0x08e:
                    case 0x28e: {                                                                               /* MOV Sw,Ew */
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0:
                                block.instruction = Inst.MOV_ES_R16;
                                break;
                            case 2:
                                block.instruction = Inst.MOV_SS_R16;
                                doAnother = true;
                                break;
                            case 3:
                                block.instruction = Inst.MOV_DS_R16;
                                break;
                            case 4:
                                block.instruction = Inst.MOV_FS_R16;
                                break;
                            case 5:
                                block.instruction = Inst.MOV_GS_R16;
                                break;
                            default:
                                block.instruction = Inst.ILLEGAL;
                                break;
                        }
                        if (rm >= 0xC0) {
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.instruction++;
                            fastBlock(block, rm, 1);
                        }
                        break;
                    }
                    case 0x08f:
                        instruction_nofast(block, Inst.POP16_R16);
                        break;                               /* POP Ew */
                    case 0x090:
                    case 0x290:
                        block.instruction = Inst.NOP;
                        break;                                            /* NOP */
                    case 0x091:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_ecx;
                        break;    /* XCHG CX,AX */
                    case 0x092:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_edx;
                        break;    /* XCHG DX,AX */
                    case 0x093:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_ebx;
                        break;    /* XCHG BX,AX */
                    case 0x094:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_esp;
                        break;    /* XCHG SP,AX */
                    case 0x095:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_ebp;
                        break;    /* XCHG BP,AX */
                    case 0x096:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_esi;
                        break;    /* XCHG SI,AX */
                    case 0x097:
                        block.instruction = Inst.XCHG_R16_R16;
                        block.r1 = reg_eax;
                        block.r2 = reg_edi;
                        break;    /* XCHG DI,AX */
                    case 0x098:
                        block.instruction = Inst.CBW;
                        break;                                              /* CBW */
                    case 0x099:
                        block.instruction = Inst.CWD;
                        break;                                              /* CWD */
                    case 0x09a:
                        block.instruction = Inst.CALL16_AP;
                        block.value = decode_fetchw();
                        block.eaa_const = decode_fetchw();
                        done=true;
                        break;/* CALL Ap */
                    case 0x09b:
                        block.instruction = Inst.NOP;
                        break;                                              /* WAIT */
                    case 0x09c:
                        block.instruction = Inst.PUSHF;
                        block.eaa16 = true;
                        break;                           /* PUSHF */
                    case 0x09d:
                        block.instruction = Inst.POPF;
                        block.eaa16 = true;
                        break;                            /* POPF */
                    case 0x09e:
                    case 0x29e:
                        block.instruction = Inst.SAHF;
                        break;                                              /* SAHF */
                    case 0x09f:
                    case 0x29f:
                        block.instruction = Inst.LAHF;
                        break;                                              /* LAHF */
                    case 0x0a0:
                        block.instruction = Inst.MOV_AL_0b;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;/* MOV AL,Ob */
                    case 0x0a1:
                        block.instruction = Inst.MOV_AX_0w;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;/* MOV AX,Ow */
                    case 0x0a2:
                        block.instruction = Inst.MOV_0b_AL;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;/* MOV Ob,AL */
                    case 0x0a3:
                        block.instruction = Inst.MOV_0w_AX;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;/* MOV Ow,AX */
                    case 0x0a4:
                    case 0x2a4:
                        doFastString(block, Inst.MOVSB16);
                        break;                                        /* MOVSB */
                    case 0x0a5:
                        doFastString(block, Inst.MOVSW16);
                        break;                                        /* MOVSW */
                    case 0x0a6:
                    case 0x2a6:
                        doString(block, Inst.STRING, StringOp.R_CMPSB, 1);
                        break;                        /* CMPSB */
                    case 0x0a7:
                        doString(block, Inst.STRING, StringOp.R_CMPSW, 2);
                        break;                        /* CMPSW */
                    case 0x0a8:
                    case 0x2a8:
                        block.instruction = Inst.TEST_R8;
                        block.r1 = reg_eax;
                        block.value = decode_fetchb();
                        break;/* TEST AL,Ib */
                    case 0x0a9:
                        block.instruction = Inst.TEST_R16;
                        block.r1 = reg_eax;
                        block.value = decode_fetchw();
                        break;/* TEST AX,Iw */
                    case 0x0aa:
                    case 0x2aa:
                        doString(block, Inst.STRING, StringOp.R_STOSB, 1);
                        break;                        /* STOSB */
                    case 0x0ab:
                        doString(block, Inst.STRING, StringOp.R_STOSW, 2);
                        break;                        /* STOSW */
                    case 0x0ac:
                    case 0x2ac:
                        doString(block, Inst.STRING, StringOp.R_LODSB, 1);
                        break;                        /* LODSB */
                    case 0x0ad:
                        doString(block, Inst.STRING, StringOp.R_LODSW, 2);
                        break;                        /* LODSW */
                    case 0x0ae:
                    case 0x2ae:
                        doString(block, Inst.STRING, StringOp.R_SCASB, 1);
                        break;                        /* SCASB */
                    case 0x0af:
                        doString(block, Inst.STRING, StringOp.R_SCASW, 2);
                        break;                        /* SCASW */
                    case 0x0b0:
                    case 0x2b0:
                        instruction(block, Inst.MOV_R8, reg_eax, decode_fetchb());
                        break;                /* MOV AL,Ib */
                    case 0x0b1:
                    case 0x2b1:
                        instruction(block, Inst.MOV_R8, reg_ecx, decode_fetchb());
                        break;                /* MOV CL,Ib */
                    case 0x0b2:
                    case 0x2b2:
                        instruction(block, Inst.MOV_R8, reg_edx, decode_fetchb());
                        break;                /* MOV DL,Ib */
                    case 0x0b3:
                    case 0x2b3:
                        instruction(block, Inst.MOV_R8, reg_ebx, decode_fetchb());
                        break;                /* MOV BL,Ib */
                    case 0x0b4:
                    case 0x2b4:
                        instruction(block, Inst.MOV_R8, reg_ah, decode_fetchb());
                        break;                 /* MOV AH,Ib */
                    case 0x0b5:
                    case 0x2b5:
                        instruction(block, Inst.MOV_R8, reg_ch, decode_fetchb());
                        break;                 /* MOV CH,Ib */
                    case 0x0b6:
                    case 0x2b6:
                        instruction(block, Inst.MOV_R8, reg_dh, decode_fetchb());
                        break;                 /* MOV DH,Ib */
                    case 0x0b7:
                    case 0x2b7:
                        instruction(block, Inst.MOV_R8, reg_bh, decode_fetchb());
                        break;                 /* MOV BH,Ib */

                    case 0x0b8:
                        instruction(block, Inst.MOV_R16, reg_eax, decode_fetchw());
                        break;                /* MOV AX,Iw */
                    case 0x0b9:
                        instruction(block, Inst.MOV_R16, reg_ecx, decode_fetchw());
                        break;                /* MOV CX,Iw */
                    case 0x0ba:
                        instruction(block, Inst.MOV_R16, reg_edx, decode_fetchw());
                        break;                /* MOV DX,Iw */
                    case 0x0bb:
                        instruction(block, Inst.MOV_R16, reg_ebx, decode_fetchw());
                        break;                /* MOV BX,Iw */
                    case 0x0bc:
                        instruction(block, Inst.MOV_R16, reg_esp, decode_fetchw());
                        break;                /* MOV SP,Iw */
                    case 0x0bd:
                        instruction(block, Inst.MOV_R16, reg_ebp, decode_fetchw());
                        break;                /* MOV BP,Iw */
                    case 0x0be:
                        instruction(block, Inst.MOV_R16, reg_esi, decode_fetchw());
                        break;                /* MOV SI,Iw */
                    case 0x0bf:
                        instruction(block, Inst.MOV_R16, reg_edi, decode_fetchw());
                        break;                /* MOV DI,Iw */
                    case 0x0c0:
                    case 0x2c0:                                                                                  /* GRP2 Eb,Ib */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction += 2;
                            fastBlock(block, rm, 2);
                        }
                        // :TODO: if fast then mask to 0x7
                        block.value = decode_fetchb() & 0x1f;
                        group2b(block, rm);
                        break;
                    }
                    case 0x0c1:                                                                                 /* GRP2 Ew,Ib */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction += 2;
                            fastBlock(block, rm, 2);
                        }
                        // :TODO: if fast then mask to 0xf
                        block.value = decode_fetchb() & 0x1f;
                        group2w(block, rm);
                        break;
                    }
                    case 0x0c2:
                        block.instruction = Inst.RETN16_Iw;
                        block.value = decode_fetchw();
                        break;                 /* RETN Iw */
                    case 0x0c3:
                        block.instruction = Inst.RETN16;
                        break;                                                /* RETN */
                    case 0x0c4:                                                                                     /* LES */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.ILLEGAL;
                            break;
                        }
                        block.instruction = Inst.LES16_16;
                        block.r1 = Mod.gd(rm);
                        fastBlock(block, rm, 1);
                        break;
                    }
                    case 0x0c5:                                                                                     /* LDS */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.ILLEGAL;
                            break;
                        }
                        block.instruction = Inst.LDS16_16;
                        block.r1 = Mod.gd(rm);
                        fastBlock(block, rm, 1);
                        break;
                    }
                    case 0x0c6:
                    case 0x2c6:                                                                                     /* MOV Eb,Ib */ {
                        int rm = decode_fetchb();
                        block.instruction = Inst.MOV_R8;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction = Inst.MOV_E8_16;
                            fastBlock(block, rm, 1);
                        }
                        block.value = decode_fetchb();
                        break;
                    }
                    case 0x0c7:                                                                                     /* MOV EW,Iw */ {
                        int rm = decode_fetchb();
                        block.instruction = Inst.MOV_R16;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction = Inst.MOV_E16_16;
                            fastBlock(block, rm, 1);
                        }
                        block.value = decode_fetchw();
                        break;
                    }
                    case 0x0c8:
                        block.instruction = Inst.ENTER16;
                        block.value = decode_fetchw();
                        block.eaa_const = decode_fetchb();
                        block.eaa16 = true;
                        break;/* ENTER Iw,Ib */
                    case 0x0c9:
                        block.instruction = Inst.LEAVE16;                                                     /* LEAVE */
                    case 0x0ca:
                        block.instruction = Inst.RETF16_Iw;
                        block.value = decode_fetchw();
                        done = true;
                        break;       /* RETF Iw */
                    case 0x0cb:
                        block.instruction = Inst.RETF16_Iw;
                        block.value = 0;
                        done = true;
                        break;                     /* RETF */
                    case 0x0cc:
                    case 0x2cc:
                        block.instruction = Inst.INT3;
                        done = true;
                        break;                                        /* INT3 */
                    case 0x0cd:
                    case 0x2cd:
                        block.instruction = Inst.INTIb;
                        block.value = decode_fetchb();
                        done = true;
                        break;           /* INT Ib */
                    case 0x0ce:
                    case 0x2ce:
                        block.instruction = Inst.INTO;
                        break;                                                  /* INTO */
                    case 0x0cf:
                        block.instruction = Inst.IRET16;
                        done = true;
                        break;                                      /* IRET */
                    case 0x0d0:
                    case 0x2d0:                                                                                     /* GRP2 Eb,1 */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction += 2;
                            fastBlock(block, rm, 2);
                        }
                        block.value = 1;
                        group2b(block, rm);
                        break;
                    }
                    case 0x0d1:                                                                                     /* GRP2 Ew,1 */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.instruction += 2;
                            fastBlock(block, rm, 2);
                        }
                        block.value = 1;
                        group2w(block, rm);
                        break;
                    }
                    case 0x0d2:
                    case 0x2d2:                                                                                     /* GRP2 Eb,CL */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction += 2;
                            fastBlock(block, rm, 2);
                        }
                        block.value = 1; // a hack so that group2b code can be re-used without triggering any 0 checks
                        group2b(block, rm);
                        block.instruction += 8;
                        break;
                    }
                    case 0x0d3:                                                                                     /* GRP2 Ew,CL */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.instruction += 2;
                            fastBlock(block, rm, 2);
                        }
                        block.value = 1; // a hack so that group2b code can be re-used without triggering any 0 checks
                        group2w(block, rm);
                        block.instruction += 8;
                        break;
                    }
                    case 0x0d4:
                    case 0x2d4:
                        block.instruction = Inst.AAD;
                        block.value = decode_fetchb();
                        break;                       /* AAM Ib */
                    case 0x0d5:
                    case 0x2d5:
                        block.instruction = Inst.AAD;
                        block.value = decode_fetchb();
                        break;                       /* AAD Ib */
                    case 0x0d6:
                    case 0x2d6:
                        block.instruction = Inst.SALC;
                        break;                                                  /* SALC */
                    case 0x0d7:
                    case 0x2d7:
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        if (EA16) block.instruction = Inst.XLAT16;
                        else block.instruction = Inst.XLAT32;
                        break;  /* XLAT */
                    case 0x0d8:
                    case 0x2d8:                                                                                     /* FPU ESC 0 */
                    case 0x0d9:
                    case 0x2d9:                                                                                     /* FPU ESC 1 */
                    case 0x0da:
                    case 0x2da:                                                                                     /* FPU ESC 2 */
                    case 0x0db:
                    case 0x2db:                                                                                     /* FPU ESC 3 */
                    case 0x0dc:
                    case 0x2dc:                                                                                     /* FPU ESC 4 */
                    case 0x0dd:
                    case 0x2dd:                                                                                     /* FPU ESC 5 */
                    case 0x0de:
                    case 0x2de:                                                                                     /* FPU ESC 6 */
                    case 0x0df:
                    case 0x2df:                                                                                     /* FPU ESC 7 */
                        // :TODO:
                        break;
                    case 0x0e0:
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOPNZ16_16;
                        else block.instruction = Inst.LOOPNZ16_32;
                        done = true;
                        break;/* LOOPNZ */
                    case 0x0e1:
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOPZ16_16;
                        else block.instruction = Inst.LOOPZ16_32;
                        done = true;
                        break;/* LOOPZ */
                    case 0x0e2:
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOP16_16;
                        else block.instruction = Inst.LOOP16_32;
                        done = true;
                        break;/* LOOP */
                    case 0x0e3:
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.JCXZ16_16;
                        else block.instruction = Inst.JCXZ16_32;
                        done = true;
                        break;/* JCXZ */
                    case 0x0e4:
                    case 0x2e4:
                        block.value = decode_fetchb();
                        block.instruction = Inst.IN_AL_Ib;
                        break;                  /* IN AL,Ib */
                    case 0x0e5:
                        block.value = decode_fetchb();
                        block.instruction = Inst.IN_AX_Ib;
                        break;                  /* IN AX,Ib */
                    case 0x0e6:
                    case 0x2e6:
                        block.value = decode_fetchb();
                        block.instruction = Inst.OUT_Ib_AL;
                        break;                 /* OUT Ib,AL */
                    case 0x0e7:
                        block.value = decode_fetchb();
                        block.instruction = Inst.OUT_Ib_AL;
                        break;                 /* OUT Ib,AX */
                    case 0x0e8:
                        block.value = decode_fetchws();
                        block.instruction = Inst.CALL16_Jw;
                        done=true;
                        break;                /* CALL Jw */
                    case 0x0e9:
                        block.value = decode_fetchws();
                        block.instruction = Inst.JMP16;
                        done = true;
                        break;          /* JMP Jw */
                    case 0x0ea:
                        block.value = decode_fetchw();
                        block.eaa_const = decode_fetchw();
                        block.instruction = Inst.JMP16_AP;
                        done = true;
                        break;/* JMP Ap */
                    case 0x0eb:
                        block.value = decode_fetchbs();
                        block.instruction = Inst.JMP16;
                        done = true;
                        break;          /* JMP Jb */
                    case 0x0ec:
                    case 0x2ec:
                        block.instruction = Inst.IN_AL_DX;
                        break;                                              /* IN AL,DX */
                    case 0x0ed:
                        block.instruction = Inst.IN_AX_DX;
                        break;                                              /* IN AX,DX */
                    case 0x0ee:
                    case 0x2ee:
                        block.instruction = Inst.OUT_DX_AL;
                        break;                                             /* OUT DX,AL */
                    case 0x0ef:
                        block.instruction = Inst.OUT_DX_AX;
                        break;                                             /* OUT DX,AX */
                    case 0x0f0:
                    case 0x2f0:
                        prefixes |= Core.PREFIX_LOCK;
                        continue;                                                /* LOCK */
                    case 0x0f1:
                    case 0x2f1:
                        block.instruction = Inst.ICEBP;
                        break;                                                 /* ICEBP */
                    case 0x0f2:
                    case 0x2f2:
                        prefixes |= Core.PREFIX_REP;
                        rep_zero = false;
                        continue;                                  /* REPNZ */
                    case 0x0f3:
                    case 0x2f3:
                        prefixes |= Core.PREFIX_REP;
                        rep_zero = true;
                        continue;                                  /* REPZ */
                    case 0x0f4:
                    case 0x2f4:
                        block.instruction = Inst.HLT;
                        done = true;
                        break;                                         /* HLT */
                    case 0x0f5:
                    case 0x2f5:
                        block.instruction = Inst.CMC;
                        break;                                                   /* CMC */
                    case 0x0f6:
                    case 0x2f6: /* GRP3 Eb(,Ib) */ {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00:											/* TEST Eb,Ib */
                            case 0x01:											/* TEST Eb,Ib Undocumented*/
                                if (rm >= 0xc0) {
                                    block.value = decode_fetchb();
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.TEST_R8;
                                } else {
                                    block.instruction = Inst.TEST_E8_16;
                                    fastBlock(block, rm, 1);
                                    block.value = decode_fetchb();
                                }
                                break;
                            case 0x02:											/* NOT Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.NOT_R8;
                                } else {
                                    block.instruction = Inst.NOT_E8_16;
                                    fastBlock(block, rm, 1);
                                }
                                break;
                            case 0x03:                                          /* NEG Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.NEG_R8;
                                } else {
                                    block.instruction = Inst.NEG_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x04:											/* MUL AL,Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.MUL_R8;
                                } else {
                                    block.instruction = Inst.IMUL_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x05:											/* IMUL AL,Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.IMUL_R8;
                                } else {
                                    block.instruction = Inst.IMUL_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x06:											/* DIV Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.DIV_R8;
                                } else {
                                    block.instruction = Inst.DIV_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x07:											/* IDIV Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.IDIV_R8;
                                } else {
                                    block.instruction = Inst.IDIV_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                        }
                        break;
                    }
                    case 0x0f7: /* GRP3 Ew(,Iw) */
                    {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00:											/* TEST Ew,Iw */
                            case 0x01:											/* TEST Ew,Iw Undocumented*/
                                if (rm >= 0xc0) {
                                    block.value = decode_fetchw();
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.TEST_R16;
                                } else {
                                    block.instruction = Inst.TEST_E16_16;
                                    fastBlock(block, rm, 1);
                                    block.value = decode_fetchw();
                                }
                                break;
                            case 0x02:											/* NOT Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.NOT_R16;
                                } else {
                                    block.instruction = Inst.NOT_E16_16;
                                    fastBlock(block, rm, 1);
                                }
                                break;
                            case 0x03:                                          /* NEG Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.NEG_R16;
                                } else {
                                    block.instruction = Inst.NEG_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x04:											/* MUL AX,Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.MUL_R16;
                                } else {
                                    block.instruction = Inst.IMUL_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x05:											/* IMUL AX,Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.IMUL_R16;
                                } else {
                                    block.instruction = Inst.IMUL_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x06:											/* DIV Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.DIV_R16;
                                } else {
                                    block.instruction = Inst.DIV_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x07:											/* IDIV Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.IDIV_R16;
                                } else {
                                    block.instruction = Inst.IDIV_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                        }
                        break;
                    }
                    case 0x0f8:
                    case 0x2f8:block.instruction=Inst.CLC;break;                                                    /* CLC */
                    case 0x0f9:
                    case 0x2f9:block.instruction=Inst.STC;break;                                                    /* STC */
                    case 0x0fa:
                    case 0x2fa:block.instruction=Inst.CLI;break;                                                    /* CLI */
                    case 0x0fb:
                    case 0x2fb:block.instruction=Inst.STI;break;                                                    /* STI */
                    case 0x0fc:
                    case 0x2fc:block.instruction=Inst.CLD;break;                                                    /* CLD */
                    case 0x0fd:
                    case 0x2fd:block.instruction=Inst.STD;break;                                                    /* STD */
                    case 0x0fe:
                    case 0x2fe: /* GRP4 Eb */ {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00:										/* INC Eb */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.INCB;
                                    block.r1 = Mod.eb(rm);
                                } else {
                                    block.instruction = Inst.INC_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x01:										/* DEC Eb */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.DECB;
                                    block.r1 = Mod.eb(rm);
                                } else {
                                    block.instruction = Inst.DEC_E8_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x07:										/* CallBack */ {
                                block.value = decode_fetchw();
                                block.instruction = Inst.CALLBACK;
                                done=true;
                                break;
                            }
                            default:
                                Log.exit("Illegal GRP4 Call " + ((rm >> 3) & 7));
                                break;
                        }
                        break;
                    }
                    case 0x0ff: /* GRP5 Ew */
                    {
                        int rm=decode_fetchb();
                        switch ((rm>>3)&7) {
                            case 0x00:										/* INC Ew */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.INCW;
                                    block.r1 = Mod.eb(rm);
                                } else {
                                    block.instruction = Inst.INC_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x01:										/* DEC Ew */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.DECW;
                                    block.r1 = Mod.eb(rm);
                                } else {
                                    block.instruction = Inst.DEC_E16_16;
                                    fastBlock(block, rm, 2);
                                }
                                break;
                            case 0x02:										/* CALL Ev */
                                if (rm >= 0xc0 ) {
                                    block.instruction=Inst.CALL16_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.CALL16_E16_16;
                                    fastBlock(block, rm, 1);
                                }
                                done = true;
                                break;
                            case 0x03:										/* CALL Ep */
                            {
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.ILLEGAL;
                                } else {
                                    block.instruction=Inst.CALL16_EP_E16;
                                    fastBlock(block, rm, 1);
                                }
                                done=true;
                                break;
                            }
                            case 0x04:										/* JMP Ev */
                                if (rm >= 0xc0 ) {
                                    block.instruction=Inst.JMP16_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.JMP16_E16_16;
                                    fastBlock(block, rm, 1);
                                }
                                done = true;
                                break;
                            case 0x05:										/* JMP Ep */
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.ILLEGAL;
                                } else {
                                    block.instruction=Inst.JMP16_EP_E16;
                                    fastBlock(block, rm, 1);
                                }
                                done=true;
                                break;
                            case 0x06:										/* PUSH Ev */
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.PUSH16_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.PUSH16_E16;
                                    fastBlock(block, rm, 1);
                                }
                                break;
                            default:
                                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"CPU:GRP5:Illegal Call "+Integer.toString((rm>>3)&7,16));
                                block.instruction = Inst.ILLEGAL;
                                done = true;
                                break;
                        }
                        break;

                    }
                    default:
                        Log.exit("Unknown instruction: "+opcode);
                }
                currentInst++;
                block.eipCount+=(decode.code - decode.op_start);

                if (CPU.cpu.code.big) {
                    opcode_index=0x200;
                    prefixes=1;
                    EA16 = false;
                } else {
                    opcode_index=0;
                    prefixes=0;
                    EA16 = true;
                }
                decode.op_start=decode.code;
                if (--max_opcodes<=0 && !doAnother) done = true;
            }
        } catch (PageFaultException e) {
            currentInst--; // don't include the instruction that caused the PF
            decode_putback(block.eipCount);
        }
        Cache.cache_closeblock();
        decode.active_block.page.end=--decode.page.index;
        decode.block.inst = new SwitchBlock[currentInst];
        System.arraycopy(inst, 0, decode.block.inst, 0, currentInst);
        return decode.block;
    }
}
