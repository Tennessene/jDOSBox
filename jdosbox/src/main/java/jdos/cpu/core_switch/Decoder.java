package jdos.cpu.core_switch;

import jdos.cpu.*;
import jdos.cpu.core_dynamic.*;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Decoder extends Helper {
    static protected boolean rep_zero=false;

    private static void ea(SwitchBlock block, int rm) {
        block.eaa16=EA16;
        if (EA16) {
            ea16(block, rm);
        } else {
            ea32(block, rm);
        }
        if (block.eaa_segPhys ==null) {
            block.eaa_segPhys = reg_dsPhys;
            block.eaa_segVal = reg_dsVal;
        }
    }

    private static void sib(SwitchBlock block, int mode) {
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
    private static void ea16(SwitchBlock block, int rm) {
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
    private static void ea32(SwitchBlock block, int rm) {
        if (rm<0x40) {
            switch (rm & 7) {
                case 0x00: block.eaa_r1 = CPU_Regs.reg_eax; break;
                case 0x01: block.eaa_r1 = CPU_Regs.reg_ecx; break;
                case 0x02: block.eaa_r1 = CPU_Regs.reg_edx; break;
                case 0x03: block.eaa_r1 = CPU_Regs.reg_ebx; break;
                case 0x04: sib(block, 0); break;
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
                case 0x04: sib(block, 1); block.eaa_const+=decode_fetchbs(); break;
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
                case 0x04: sib(block, 1); block.eaa_const+=decode_fetchds(); break;
                case 0x05: if (block.eaa_segPhys ==null) {block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal;} block.eaa_r1 = CPU_Regs.reg_ebp; block.eaa_const = decode_fetchds(); break;
                case 0x06: block.eaa_r1 = CPU_Regs.reg_esi; block.eaa_const = decode_fetchds(); break;
                case 0x07: block.eaa_r1 = CPU_Regs.reg_edi; block.eaa_const = decode_fetchds(); break;
            }
        }
    }
    private static int instruction8(SwitchBlock block, Inst r, Inst e) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = r;
            block.r1=Mod.eb(rm);
            block.r2=Mod.gb(rm);
        } else {
            block.instruction = e;
            block.r1=Mod.gb(rm);
            ea(block, rm);
        }
        return rm;
    }

    private static void instruction(SwitchBlock block, Inst i, Reg reg, int value) {
        block.instruction = i;
        block.r1 = reg;
        block.value = value;
    }
    private static void instruction(SwitchBlock block, Inst i, Reg reg) {
        block.instruction = i;
        block.r1 = reg;
    }
    private static void instruction8_r(SwitchBlock block, Inst r, Inst e) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = r;
            block.r1=Mod.gb(rm);
            block.r2=Mod.eb(rm);
        } else {
            block.instruction = e;
            block.r1=Mod.gb(rm);
            ea(block, rm);
        }
    }

    private static void doString(SwitchBlock block, Inst instruction, int type, int width) {
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

    private static void doFastString(SwitchBlock block, Inst i16, Inst i16r, Inst i32, Inst i32r) {
        if (block.eaa_segVal==null) {block.eaa_segVal=reg_dsVal;block.eaa_segPhys=reg_dsPhys;}
        if ((prefixes & Core.PREFIX_ADDR)==0) {
            if ((prefixes & Core.PREFIX_REP)==0) {
                block.instruction=i16;
            } else {
                block.instruction=i16r;
            }
        } else {
            if ((prefixes & Core.PREFIX_REP)==0) {
                block.instruction=i32;
            } else {
                block.instruction=i32r;
            }
        }
    }

    private static void instruction(SwitchBlock block, Inst r, Inst e) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = r;
            block.r1=Mod.ed(rm);
            block.r2=Mod.gd(rm);
        } else {
            block.instruction = e;
            block.r1=Mod.gd(rm);
            ea(block, rm);
        }
    }

    private static void instructionE32(SwitchBlock block, Inst r, Inst e) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = r;
            block.r1=Mod.ed(rm);
        } else {
            block.instruction = e;
            ea(block, rm);
        }
    }

    private static void instruction_r(SwitchBlock block, Inst r, Inst e) {
        int rm = decode_fetchb();
        if (rm >= 0xc0 ) {
            block.instruction = r;
            block.r1=Mod.gd(rm);
            block.r2=Mod.ed(rm);
        } else {
            block.instruction = e;
            block.r1=Mod.gd(rm);
            ea(block, rm);
        }
    }

    private static void group2b(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: // ROLB
                if ((block.value & 0x7) == 0) {
                    if ((block.value & 0x18) != 0) {
                        block.instruction = Inst.ROLB_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0x7;
                    block.instruction = Inst.ROLB_R8;
                }
                break;
            case 0x01: // RORB
                if ((block.value & 0x7) == 0) {
                    if ((block.value & 0x18) != 0) {
                        block.instruction = Inst.RORB_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0x7;
                    block.instruction = Inst.RORB_R8;
                }
                break;
            case 0x02: if (block.value % 9 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCLB_R8; break;
            case 0x03: if (block.value % 9 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCRB_R8; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHLB_R8; break;
            case 0x05: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHRB_R8; break;
            case 0x07: if (block.value == 0)block.instruction = Inst.NOP;else block.instruction = Inst.SARB_R8;break;
        }
    }

    private static void group2bEA(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: // ROLB
                if ((block.value & 0x7) == 0) {
                    if ((block.value & 0x18) != 0) {
                        block.instruction = Inst.ROLB_E8_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0x7;
                    block.instruction = Inst.ROLB_E8;
                }
                break;
            case 0x01: // RORB
                if ((block.value & 0x7) == 0) {
                    if ((block.value & 0x18) != 0) {
                        block.instruction = Inst.RORB_E8_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0x7;
                    block.instruction = Inst.RORB_E8;
                }
                break;
            case 0x02: if (block.value % 9 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCLB_E8; break;
            case 0x03: if (block.value % 9 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCRB_E8; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHLB_E8; break;
            case 0x05: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHRB_E8; break;
            case 0x07: if (block.value == 0)block.instruction = Inst.NOP;else block.instruction = Inst.SARB_E8;break;
        }
    }
    
    private static void group2w(SwitchBlock block , int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: // ROLW
                if ((block.value & 0xf)==0) {
                    if ((block.value & 0x10)!=0) {
                        block.instruction = Inst.ROLW_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0xf;
                    block.instruction = Inst.ROLW_R16;
                }
                break;
            case 0x01: // RORW
                if ((block.value & 0xf)==0) {
                    if ((block.value & 0x10)!=0) {
                        block.instruction = Inst.RORW_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0xf;
                    block.instruction = Inst.RORW_R16;
                }
                break;
            case 0x02: if (block.value % 17 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCLW_R16; break;
            case 0x03: if (block.value % 17 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCRW_R16; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHLW_R16; break;
            case 0x05: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHRW_R16; break;
            case 0x07: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SARW_R16; break;
        }
    }

    private static void group2wEA(SwitchBlock block , int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: // ROLW
                if ((block.value & 0xf)==0) {
                    if ((block.value & 0x10)!=0) {
                        block.instruction = Inst.ROLW_E16_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0xf;
                    block.instruction = Inst.ROLW_E16;
                }
                break;
            case 0x01: // RORW
                if ((block.value & 0xf)==0) {
                    if ((block.value & 0x10)!=0) {
                        block.instruction = Inst.RORW_E16_0_flags;
                    } else {
                        block.instruction = Inst.NOP;
                    }
                } else {
                    block.value &= 0xf;
                    block.instruction = Inst.RORW_E16;
                }
                break;
            case 0x02: if (block.value % 17 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCLW_E16; break;
            case 0x03: if (block.value % 17 == 0) block.instruction = Inst.NOP; else block.instruction = Inst.RCRW_E16; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHLW_E16; break;
            case 0x05: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SHRW_E16; break;
            case 0x07: if (block.value == 0) block.instruction = Inst.NOP; else block.instruction = Inst.SARW_E16; break;
        }
    }

    private static void group2bCL(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLB_R8_CL; break;
            case 0x01: block.instruction = Inst.RORB_R8_CL; break;
            case 0x02: block.instruction = Inst.RCLB_R8_CL; break;
            case 0x03: block.instruction = Inst.RCRB_R8_CL; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLB_R8_CL; break;
            case 0x05: block.instruction = Inst.SHRB_R8_CL; break;
            case 0x07: block.instruction = Inst.SARB_R8_CL;break;
        }
    }

    private static void group2bEACL(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLB_E8_CL; break;
            case 0x01: block.instruction = Inst.RORB_E8_CL; break;
            case 0x02: block.instruction = Inst.RCLB_E8_CL; break;
            case 0x03: block.instruction = Inst.RCRB_E8_CL; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLB_E8_CL; break;
            case 0x05: block.instruction = Inst.SHRB_E8_CL; break;
            case 0x07: block.instruction = Inst.SARB_E8_CL;break;
        }
    }

    private static void group2wCL(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLW_R16_CL; break;
            case 0x01: block.instruction = Inst.RORW_R16_CL; break;
            case 0x02: block.instruction = Inst.RCLW_R16_CL; break;
            case 0x03: block.instruction = Inst.RCRW_R16_CL; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLW_R16_CL; break;
            case 0x05: block.instruction = Inst.SHRW_R16_CL; break;
            case 0x07: block.instruction = Inst.SARW_R16_CL;break;
        }
    }

    private static void group2wEACL(SwitchBlock block, int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLW_E16_CL; break;
            case 0x01: block.instruction = Inst.RORW_E16_CL; break;
            case 0x02: block.instruction = Inst.RCLW_E16_CL; break;
            case 0x03: block.instruction = Inst.RCRW_E16_CL; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLW_E16_CL; break;
            case 0x05: block.instruction = Inst.SHRW_E16_CL; break;
            case 0x07: block.instruction = Inst.SARW_E16_CL;break;
        }
    }

    private static void group2dCL(SwitchBlock block , int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLD_R32_CL; break;
            case 0x01: block.instruction = Inst.RORD_R32_CL; break;
            case 0x02: block.instruction = Inst.RCLD_R32_CL; break;
            case 0x03: block.instruction = Inst.RCRD_R32_CL; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLD_R32_CL; break;
            case 0x05: block.instruction = Inst.SHRD_R32_CL; break;
            case 0x07: block.instruction = Inst.SARD_R32_CL; break;
        }
    }

    private static void group2dEACL(SwitchBlock block , int rm) {
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLD_E32_CL; break;
            case 0x01: block.instruction = Inst.RORD_E32_CL; break;
            case 0x02: block.instruction = Inst.RCLD_E32_CL; break;
            case 0x03: block.instruction = Inst.RCRD_E32_CL; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLD_E32_CL; break;
            case 0x05: block.instruction = Inst.SHRD_E32_CL; break;
            case 0x07: block.instruction = Inst.SARD_E32_CL; break;
        }
    }

    private static void group2d(SwitchBlock block , int rm) {
        block.value &= 0x1f;
        if (block.value==0) {
            block.instruction = Inst.NOP;
            return;
        }
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLD_R32; break;
            case 0x01: block.instruction = Inst.RORD_R32; break;
            case 0x02: block.instruction = Inst.RCLD_R32; break;
            case 0x03: block.instruction = Inst.RCRD_R32; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLD_R32; break;
            case 0x05: block.instruction = Inst.SHRD_R32; break;
            case 0x07: block.instruction = Inst.SARD_R32; break;
        }
    }

    private static void group2dEA(SwitchBlock block , int rm) {
        block.value &= 0x1f;
        if (block.value==0) {
            block.instruction = Inst.NOP;
            return;
        }
        switch ((rm >> 3) & 7) {
            case 0x00: block.instruction = Inst.ROLD_E32; break;
            case 0x01: block.instruction = Inst.RORD_E32; break;
            case 0x02: block.instruction = Inst.RCLD_E32; break;
            case 0x03: block.instruction = Inst.RCRD_E32; break;
            case 0x04:/* SHL and SAL are the same */
            case 0x06: block.instruction = Inst.SHLD_E32; break;
            case 0x05: block.instruction = Inst.SHRD_E32; break;
            case 0x07: block.instruction = Inst.SARD_E32; break;
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
                block = inst[currentInst];
                if (block==null) {
                    block = new SwitchBlock();
                    inst[currentInst] =  block;
                }
                opcode=opcode_index+decode_fetchb();
                block.opCode=opcode;
                switch (opcode) {
                    case 0x000:
                    case 0x200: /* ADD Eb,Gb */ 
                        instruction8(block, Inst.ADD_R8_R8, Inst.ADD_E8_R8); break;
                    case 0x001: /* ADD Ew,Gw */
                        instruction(block, Inst.ADD_R16_R16, Inst.ADD_E16_R16); break;
                    case 0x201: /* ADD Ed,Gd */
                        instruction(block, Inst.ADD_R32_R32, Inst.ADD_E32_R32); break;
                    case 0x002:
                    case 0x202: /* ADD Gb,Eb */
                        instruction8_r(block, Inst.ADD_R8_R8, Inst.ADD_R8_E8); break;
                    case 0x003: /* ADD Gw,Ew */
                        instruction_r(block, Inst.ADD_R16_R16, Inst.ADD_R16_E16); break;
                    case 0x203: /* ADD Gd,Ed */
                        instruction_r(block, Inst.ADD_R32_R32, Inst.ADD_R32_E32); break;
                    case 0x004:
                    case 0x204: /* ADD AL,Ib */
                        instruction(block, Inst.ADD_R8, reg_eax, decode_fetchb()); break;               
                    case 0x005: /* ADD AX,Iw */
                        instruction(block, Inst.ADD_R16, reg_eax, decode_fetchw()); break;              
                    case 0x205: /* ADD EAX,Id */
                        instruction(block, Inst.ADD_R32, reg_eax, decode_fetchd()); break;              
                    case 0x006: /* PUSH ES */
                        block.instruction = Inst.PUSH16_ES; break;                                      
                    case 0x007: /* POP ES */
                        block.instruction = Inst.POP16_ES; break;

                    case 0x008:
                    case 0x208: /* OR Eb,Gb */
                        instruction8(block, Inst.OR_R8_R8, Inst.OR_E8_R8); break;
                    case 0x009: /* OR Ew,Gw */
                        instruction(block, Inst.OR_R16_R16, Inst.OR_E16_R16); break;
                    case 0x209: /* OR Ed,Gd */
                        instruction(block, Inst.OR_R32_R32, Inst.OR_E32_R32); break;
                    case 0x00a:
                    case 0x20a: /* OR Gb,Eb */
                        instruction8_r(block, Inst.OR_R8_R8, Inst.OR_R8_E8); break;
                    case 0x00b: /* OR Gw,Ew */
                        instruction_r(block, Inst.OR_R16_R16, Inst.OR_R16_E16); break;
                    case 0x20b: /* OR Gd,Ed */
                        instruction_r(block, Inst.OR_R32_R32, Inst.OR_R32_E32); break;
                    case 0x00c:
                    case 0x20c: /* OR AL,Ib */
                        instruction(block, Inst.OR_R8, reg_eax, decode_fetchb()); break;
                    case 0x00d: /* OR AX,Iw */
                        instruction(block, Inst.OR_R16, reg_eax, decode_fetchw()); break;
                    case 0x20d: /* OR EAX,Id */
                        instruction(block, Inst.OR_R32, reg_eax, decode_fetchd()); break;
                    case 0x00e: /* PUSH CS */
                        block.instruction = Inst.PUSH16_CS; break;                                      
                    case 0x00f:
                    case 0x20f: /* 2 byte opcodes */
                        opcode_index += 0x100; continue;

                    case 0x010:
                    case 0x210: /* ADC Eb,Gb */
                        instruction8(block, Inst.ADC_R8_R8, Inst.ADC_E8_R8); break;
                    case 0x011: /* ADC Ew,Gw */
                        instruction(block, Inst.ADC_R16_R16, Inst.ADC_E16_R16); break;
                    case 0x211: /* ADC Ed,Gd */
                        instruction(block, Inst.ADC_R32_R32, Inst.ADC_E32_R32); break;
                    case 0x012:
                    case 0x212: /* ADC Gb,Eb */
                        instruction8_r(block, Inst.ADC_R8_R8, Inst.ADC_R8_E8); break;
                    case 0x013: /* ADC Gw,Ew */
                        instruction_r(block, Inst.ADC_R16_R16, Inst.ADC_R16_E16); break;
                    case 0x213: /* ADC Gd,Ed */
                        instruction_r(block, Inst.ADC_R32_R32, Inst.ADC_R32_E32); break;
                    case 0x014:
                    case 0x214: /* ADC AL,Ib */
                        instruction(block, Inst.ADC_R8, reg_eax, decode_fetchb()); break;
                    case 0x015: /* ADC AX,Iw */
                        instruction(block, Inst.ADC_R16, reg_eax, decode_fetchw()); break;
                    case 0x215: /* ADC EAX,Id */
                        instruction(block, Inst.ADC_R32, reg_eax, decode_fetchd()); break;
                    case 0x016: /* PUSH SS */
                        block.instruction = Inst.PUSH16_SS; break;                                      
                    case 0x017: /* POP SS */ //Always do another instruction
                        block.instruction = Inst.POP16_SS; doAnother = true; break;

                    case 0x018:
                    case 0x218: /* SBB Eb,Gb */
                        instruction8(block, Inst.SBB_R8_R8, Inst.SBB_E8_R8); break;
                    case 0x019: /* SBB Ew,Gw */
                        instruction(block, Inst.SBB_R16_R16, Inst.SBB_E16_R16); break;
                    case 0x219: /* SBB Ed,Gd */
                        instruction(block, Inst.SBB_R32_R32, Inst.SBB_E32_R32); break;
                    case 0x01a:
                    case 0x21a: /* SBB Gb,Eb */
                        instruction8_r(block, Inst.SBB_R8_R8, Inst.SBB_R8_E8); break;
                    case 0x01b: /* SBB Gw,Ew */
                        instruction_r(block, Inst.SBB_R16_R16, Inst.SBB_R16_E16); break;
                    case 0x21b: /* SBB Gd,Ed */
                        instruction_r(block, Inst.SBB_R32_R32, Inst.SBB_R32_E32); break;
                    case 0x01c:
                    case 0x21c: /* SBB AL,Ib */
                        instruction(block, Inst.SBB_R8, reg_eax, decode_fetchb()); break;
                    case 0x01d: /* SBB AX,Iw */
                        instruction(block, Inst.SBB_R16, reg_eax, decode_fetchw()); break;
                    case 0x21d: /* SBB EAX,Id */
                        instruction(block, Inst.SBB_R32, reg_eax, decode_fetchd()); break;
                    case 0x01e: /* PUSH DS */
                        block.instruction = Inst.PUSH16_DS; break;                                      
                    case 0x01f: /* POP DS */
                        block.instruction = Inst.POP16_DS; break;

                    case 0x020:
                    case 0x220: /* AND Eb,Gb */
                        instruction8(block, Inst.AND_R8_R8, Inst.AND_E8_R8); break;
                    case 0x021: /* AND Ew,Gw */
                        instruction(block, Inst.AND_R16_R16, Inst.AND_E16_R16); break;
                    case 0x221: /* AND Ed,Gd */
                        instruction(block, Inst.AND_R32_R32, Inst.AND_E32_R32); break;
                    case 0x022:
                    case 0x222: /* AND Gb,Eb */
                        instruction8_r(block, Inst.AND_R8_R8, Inst.AND_R8_E8); break;
                    case 0x023: /* AND Gw,Ew */
                        instruction_r(block, Inst.AND_R16_R16, Inst.AND_R16_E16); break;
                    case 0x223: /* AND Gd,Ed */
                        instruction_r(block, Inst.AND_R32_R32, Inst.AND_R32_E32); break;
                    case 0x024:
                    case 0x224: /* AND AL,Ib */
                        instruction(block, Inst.AND_R8, reg_eax, decode_fetchb()); break;
                    case 0x025: /* AND AX,Iw */
                        instruction(block, Inst.AND_R16, reg_eax, decode_fetchw()); break;
                    case 0x225: /* AND EAX,Id */
                        instruction(block, Inst.AND_R32, reg_eax, decode_fetchd()); break;
                    case 0x026: /* SEG ES: */
                        block.eaa_segPhys = reg_esPhys; block.eaa_segVal = CPU_Regs.reg_esVal; continue;
                    case 0x027: /* DAA */
                        block.instruction = Inst.DAA; break;

                    case 0x028:
                    case 0x228: /* SUB Eb,Gb */
                        instruction8(block, Inst.SUB_R8_R8, Inst.SUB_E8_R8); break;
                    case 0x029: /* SUB Ew,Gw */
                        instruction(block, Inst.SUB_R16_R16, Inst.SUB_E16_R16); break;
                    case 0x229: /* SUB Ed,Gd */
                        instruction(block, Inst.SUB_R32_R32, Inst.SUB_E32_R32); break;
                    case 0x02a:
                    case 0x22a: /* SUB Gb,Eb */
                        instruction8_r(block, Inst.SUB_R8_R8, Inst.SUB_R8_E8); break;
                    case 0x02b: /* SUB Gw,Ew */
                        instruction_r(block, Inst.SUB_R16_R16, Inst.SUB_R16_E16); break;
                    case 0x22b: /* SUB Gd,Ed */
                        instruction_r(block, Inst.SUB_R32_R32, Inst.SUB_R32_E32); break;
                    case 0x02c:
                    case 0x22c: /* SUB AL,Ib */
                        instruction(block, Inst.SUB_R8, reg_eax, decode_fetchb()); break;
                    case 0x02d: /* SUB AX,Iw */
                        instruction(block, Inst.SUB_R16, reg_eax, decode_fetchw()); break;
                    case 0x22d: /* SUB EAX,Id */
                        instruction(block, Inst.SUB_R32, reg_eax, decode_fetchd()); break;
                    case 0x02e: /* SEG CS: */
                        block.eaa_segPhys = reg_csPhys; block.eaa_segVal = CPU_Regs.reg_csVal; continue;
                    case 0x02f: /* DAS */
                        block.instruction = Inst.DAS; break;

                    case 0x030:
                    case 0x230: /* XOR Eb,Gb */
                        instruction8(block, Inst.XOR_R8_R8, Inst.XOR_E8_R8); break;
                    case 0x031: /* XOR Ew,Gw */
                        instruction(block, Inst.XOR_R16_R16, Inst.XOR_E16_R16); break;
                    case 0x231: /* XOR Ed,Gd */
                        instruction(block, Inst.XOR_R32_R32, Inst.XOR_E32_R32); break;
                    case 0x032:
                    case 0x232: /* XOR Gb,Eb */
                        instruction8_r(block, Inst.XOR_R8_R8, Inst.XOR_R8_E8); break;
                    case 0x033: /* XOR Gw,Ew */
                        instruction_r(block, Inst.XOR_R16_R16, Inst.XOR_R16_E16); break;
                    case 0x233: /* XOR Gd,Ed */
                        instruction_r(block, Inst.XOR_R32_R32, Inst.XOR_R32_E32); break;
                    case 0x034:
                    case 0x234: /* XOR AL,Ib */
                        instruction(block, Inst.XOR_R8, reg_eax, decode_fetchb()); break;
                    case 0x035: /* XOR AX,Iw */
                        instruction(block, Inst.XOR_R16, reg_eax, decode_fetchw()); break;
                    case 0x235: /* XOR EAX,Id */
                        instruction(block, Inst.XOR_R32, reg_eax, decode_fetchd()); break;
                    case 0x036: /* SEG SS: */
                        block.eaa_segPhys = reg_ssPhys; block.eaa_segVal = CPU_Regs.reg_ssVal; continue;
                    case 0x037: /* AAA */
                        block.instruction = Inst.AAA; break;

                    case 0x038:
                    case 0x238: /* CMP Eb,Gb */
                        instruction8(block, Inst.CMP_R8_R8, Inst.CMP_E8_R8); break;
                    case 0x039: /* CMP Ew,Gw */
                        instruction(block, Inst.CMP_R16_R16, Inst.CMP_E16_R16); break;
                    case 0x239: /* CMP Ed,Gd */
                        instruction(block, Inst.CMP_R32_R32, Inst.CMP_E32_R32); break;
                    case 0x03a:
                    case 0x23a: /* CMP Gb,Eb */
                        instruction8_r(block, Inst.CMP_R8_R8, Inst.CMP_R8_E8); break;
                    case 0x03b: /* CMP Gw,Ew */
                        instruction_r(block, Inst.CMP_R16_R16, Inst.CMP_R16_E16); break;
                    case 0x23b: /* CMP Gd,Ed */
                        instruction_r(block, Inst.CMP_R32_R32, Inst.CMP_R32_E32); break;
                    case 0x03c:
                    case 0x23c: /* CMP AL,Ib */
                        instruction(block, Inst.CMP_R8, reg_eax, decode_fetchb()); break;
                    case 0x03d: /* CMP AX,Iw */
                        instruction(block, Inst.CMP_R16, reg_eax, decode_fetchw()); break;
                    case 0x23d: /* CMP EAX,Id */
                        instruction(block, Inst.CMP_R32, reg_eax, decode_fetchd()); break;
                    case 0x03e: /* SEG DS: */
                        block.eaa_segPhys = reg_dsPhys; block.eaa_segVal = CPU_Regs.reg_dsVal; continue;
                    case 0x03f: /* AAS */
                        block.instruction = Inst.AAS; break;

                    case 0x040: /* INC AX */
                        instruction(block, Inst.INC_R16, reg_eax); break;
                    case 0x041: /* INC CX */
                        instruction(block, Inst.INC_R16, reg_ecx); break;
                    case 0x042: /* INC DX */
                        instruction(block, Inst.INC_R16, reg_edx); break;
                    case 0x043: /* INC BX */
                        instruction(block, Inst.INC_R16, reg_ebx); break;
                    case 0x044: /* INC SP */
                        instruction(block, Inst.INC_R16, reg_esp); break;
                    case 0x045: /* INC BP */
                        instruction(block, Inst.INC_R16, reg_ebp); break;
                    case 0x046: /* INC SI */
                        instruction(block, Inst.INC_R16, reg_esi); break;
                    case 0x047: /* INC DI */
                        instruction(block, Inst.INC_R16, reg_edi); break;
                    case 0x048: /* DEC AX */
                        instruction(block, Inst.DEC_R16, reg_eax); break;
                    case 0x049: /* DEC CX */
                        instruction(block, Inst.DEC_R16, reg_ecx); break;
                    case 0x04a: /* DEC DX */
                        instruction(block, Inst.DEC_R16, reg_edx); break;
                    case 0x04b: /* DEC BX */
                        instruction(block, Inst.DEC_R16, reg_ebx); break;
                    case 0x04c: /* DEC SP */
                        instruction(block, Inst.DEC_R16, reg_esp); break;
                    case 0x04d: /* DEC BP */
                        instruction(block, Inst.DEC_R16, reg_ebp); break;
                    case 0x04e: /* DEC SI */
                        instruction(block, Inst.DEC_R16, reg_esi); break;
                    case 0x04f: /* DEC DI */
                        instruction(block, Inst.DEC_R16, reg_edi); break;

                    case 0x050: /* PUSH AX */
                        instruction(block, Inst.PUSH16_R16, reg_eax); break;
                    case 0x051: /* PUSH CX */
                        instruction(block, Inst.PUSH16_R16, reg_ecx); break;
                    case 0x052: /* PUSH DX */
                        instruction(block, Inst.PUSH16_R16, reg_edx); break;
                    case 0x053: /* PUSH BX */
                        instruction(block, Inst.PUSH16_R16, reg_ebx); break;
                    case 0x054: /* PUSH SP */
                        instruction(block, Inst.PUSH16_R16, reg_esp); break;
                    case 0x055: /* PUSH BP */
                        instruction(block, Inst.PUSH16_R16, reg_ebp); break;
                    case 0x056: /* PUSH SI */
                        instruction(block, Inst.PUSH16_R16, reg_esi); break;
                    case 0x057: /* PUSH DI */
                        instruction(block, Inst.PUSH16_R16, reg_edi); break;
                    case 0x058: /* POP AX */
                        instruction(block, Inst.POP16_R16, reg_eax); break;
                    case 0x059: /* POP CX */
                        instruction(block, Inst.POP16_R16, reg_ecx); break;
                    case 0x05a: /* POP DX */
                        instruction(block, Inst.POP16_R16, reg_edx); break;
                    case 0x05b: /* POP BX */
                        instruction(block, Inst.POP16_R16, reg_ebx); break;
                    case 0x05c: /* POP SP */
                        instruction(block, Inst.POP16_R16, reg_esp); break;
                    case 0x05d: /* POP BP */
                        instruction(block, Inst.POP16_R16, reg_ebp); break;
                    case 0x05e: /* POP SI */
                        instruction(block, Inst.POP16_R16, reg_esi); break;
                    case 0x05f: /* POP DI */
                        instruction(block, Inst.POP16_R16, reg_edi); break;

                    case 0x060: /* PUSHA */
                        block.instruction = Inst.PUSH16A; break;
                    case 0x061: /* POPA */
                        block.instruction = Inst.POP16A; break;
                    case 0x062: /* BOUND */ {
                        int rm = decode_fetchb();
                        block.r1 = Mod.gw(rm);
                        ea(block, rm);
                        block.instruction = Inst.BOUND16;
                        break;
                    }
                    case 0x063: /* ARPL Ew,Rw */
                        instruction(block, Inst.ARPL_R16_R16, Inst.ARPL_R16_E16); break;
                    case 0x064: /* SEG FS: */
                        block.eaa_segPhys = reg_fsPhys; block.eaa_segVal = CPU_Regs.reg_fsVal; continue;
                    case 0x065: /* SEG GS: */
                        block.eaa_segPhys = reg_gsPhys; block.eaa_segVal = CPU_Regs.reg_gsVal; continue;
                    case 0x066: /* Operand Size Prefix */
                        opcode_index = (CPU.cpu.code.big ? 0 : 512); continue;
                    case 0x067: /* Address Size Prefix */
                        prefixes = (prefixes & ~Core.PREFIX_ADDR) | (CPU.cpu.code.big ? 0 : 1); EA16 = (prefixes & 1) == 0; continue;
                    case 0x068: /* PUSH Iw */
                        block.instruction = Inst.PUSH16; block.value = decode_fetchw(); break;
                    case 0x069: /* IMUL Gw,Ew,Iw */
                        instruction(block, Inst.IMUL_R16_R16, Inst.IMUL_R16_E16); block.value = decode_fetchws(); break;
                    case 0x06a: /* PUSH Ib */
                        block.instruction = Inst.PUSH16; block.value = decode_fetchbs(); break;
                    case 0x06b: /* IMUL Gw,Ew,Ib */
                        instruction(block, Inst.IMUL_R16_R16, Inst.IMUL_R16_E16); block.value = decode_fetchbs(); break;
                    case 0x06c: /* INSB */
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_INSB, 1); break;
                    case 0x06d: /* INSW */
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_INSW, 2); break;
                    case 0x06e: /* OUTSB */
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_OUTSB, 1); break;
                    case 0x06f: /* OUTSW */
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_OUTSW, 2); break;

                    case 0x070: /* JO */
                        block.instruction = Inst.JUMP16_JO; block.value = decode_fetchbs(); done = true; break;
                    case 0x071: /* NJO */
                        block.instruction = Inst.JUMP16_NJO; block.value = decode_fetchbs(); done = true; break;
                    case 0x072: /* JB */
                        block.instruction = Inst.JUMP16_B; block.value = decode_fetchbs(); done = true; break;
                    case 0x073: /* NB */
                        block.instruction = Inst.JUMP16_NB; block.value = decode_fetchbs(); done = true; break;
                    case 0x074: /* Z */
                        block.instruction = Inst.JUMP16_Z; block.value = decode_fetchbs(); done = true; break;
                    case 0x075: /* NZ */
                        block.instruction = Inst.JUMP16_NZ; block.value = decode_fetchbs(); done = true; break;
                    case 0x076: /* BE */
                        block.instruction = Inst.JUMP16_BE; block.value = decode_fetchbs(); done = true; break;
                    case 0x077: /* NBE */
                        block.instruction = Inst.JUMP16_NBE; block.value = decode_fetchbs(); done = true; break;
                    case 0x078: /* JS */
                        block.instruction = Inst.JUMP16_S; block.value = decode_fetchbs(); done = true; break;
                    case 0x079: /* NS */
                        block.instruction = Inst.JUMP16_NS; block.value = decode_fetchbs(); done = true; break;
                    case 0x07a: /* P */
                        block.instruction = Inst.JUMP16_P; block.value = decode_fetchbs(); done = true; break;
                    case 0x07b: /* NP */
                        block.instruction = Inst.JUMP16_NP; block.value = decode_fetchbs(); done = true; break;
                    case 0x07c: /* L */
                        block.instruction = Inst.JUMP16_L; block.value = decode_fetchbs(); done = true; break;
                    case 0x07d: /* NL */
                        block.instruction = Inst.JUMP16_NL; block.value = decode_fetchbs(); done = true; break;
                    case 0x07e: /* LE */
                        block.instruction = Inst.JUMP16_LE; block.value = decode_fetchbs(); done = true; break;
                    case 0x07f: /* NLE */
                        block.instruction = Inst.JUMP16_NLE; block.value = decode_fetchbs(); done = true; break;

                    case 0x080:
                    case 0x280:
                    case 0x082:
                    case 0x282: /* Grpl Eb,Ib */ {
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.eb(rm);
                            block.value = decode_fetchb();
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_R8; break;
                                case 0x01: block.instruction = Inst.OR_R8; break;
                                case 0x02: block.instruction = Inst.ADC_R8; break;
                                case 0x03: block.instruction = Inst.SBB_R8; break;
                                case 0x04: block.instruction = Inst.AND_R8; break;
                                case 0x05: block.instruction = Inst.SUB_R8; break;
                                case 0x06: block.instruction = Inst.XOR_R8; break;
                                case 0x07: block.instruction = Inst.CMP_R8; break;
                            }
                        } else {
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_E8; break;
                                case 0x01: block.instruction = Inst.OR_E8; break;
                                case 0x02: block.instruction = Inst.ADC_E8; break;
                                case 0x03: block.instruction = Inst.SBB_E8; break;
                                case 0x04: block.instruction = Inst.AND_E8; break;
                                case 0x05: block.instruction = Inst.SUB_E8; break;
                                case 0x06: block.instruction = Inst.XOR_E8; break;
                                case 0x07: block.instruction = Inst.CMP_E8; break;
                            }
                            ea(block, rm);
                            block.value = decode_fetchb();
                        }
                        break;
                    }
                    case 0x081: /* Grpl Ew,Iw */ {
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                            block.value = decode_fetchw();
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_R16; break;
                                case 0x01: block.instruction = Inst.OR_R16; break;
                                case 0x02: block.instruction = Inst.ADC_R16; break;
                                case 0x03: block.instruction = Inst.SBB_R16; break;
                                case 0x04: block.instruction = Inst.AND_R16; break;
                                case 0x05: block.instruction = Inst.SUB_R16; break;
                                case 0x06: block.instruction = Inst.XOR_R16; break;
                                case 0x07: block.instruction = Inst.CMP_R16; break;
                            }
                        } else {
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_E16; break;
                                case 0x01: block.instruction = Inst.OR_E16; break;
                                case 0x02: block.instruction = Inst.ADC_E16; break;
                                case 0x03: block.instruction = Inst.SBB_E16; break;
                                case 0x04: block.instruction = Inst.AND_E16; break;
                                case 0x05: block.instruction = Inst.SUB_E16; break;
                                case 0x06: block.instruction = Inst.XOR_E16; break;
                                case 0x07: block.instruction = Inst.CMP_E16; break;
                            }
                            ea(block, rm);
                            block.value = decode_fetchw();
                        }
                        break;
                    }
                    case 0x083: /* Grpl Ew,Ix */ {
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ew(rm);
                            block.value = (((short)decode_fetchbs()) & 0xFFFF);
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_R16; break;
                                case 0x01: block.instruction = Inst.OR_R16; break;
                                case 0x02: block.instruction = Inst.ADC_R16; break;
                                case 0x03: block.instruction = Inst.SBB_R16; break;
                                case 0x04: block.instruction = Inst.AND_R16; break;
                                case 0x05: block.instruction = Inst.SUB_R16; break;
                                case 0x06: block.instruction = Inst.XOR_R16; break;
                                case 0x07: block.instruction = Inst.CMP_R16; break;
                            }
                        } else {
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_E16; break;
                                case 0x01: block.instruction = Inst.OR_E16; break;
                                case 0x02: block.instruction = Inst.ADC_E16; break;
                                case 0x03: block.instruction = Inst.SBB_E16; break;
                                case 0x04: block.instruction = Inst.AND_E16; break;
                                case 0x05: block.instruction = Inst.SUB_E16; break;
                                case 0x06: block.instruction = Inst.XOR_E16; break;
                                case 0x07: block.instruction = Inst.CMP_E16; break;
                            }
                            ea(block, rm);
                            block.value = (((short)decode_fetchbs()) & 0xFFFF);
                        }
                        break;
                    }
                    case 0x084:
                    case 0x284: /* TEST Eb,Gb */
                        instruction8(block, Inst.TEST_R8_R8, Inst.TEST_E8_R8); break;
                    case 0x085: /* TEST Ew,Gw */
                        instruction(block, Inst.TEST_R16_R16, Inst.TEST_E16_R16); break;
                    case 0x086:
                    case 0x286: /* XCHG Eb,Gb */
                        instruction8(block, Inst.XCHG_R8_R8, Inst.XCHG_E8_R8); break;
                    case 0x087: /* XCHG Ew,Gw */
                        instruction(block, Inst.XCHG_R16_R16, Inst.XCHG_E16_R16); break;
                    case 0x088:
                    case 0x288: /* MOV Eb,Gb */
                        if (instruction8(block, Inst.MOV_R8_R8, Inst.MOV_E8_R8) == 5) block.instruction = Inst.MOV_E8_R8_RM5; break;
                    case 0x089: /* MOV Ew,Gw */
                        instruction(block, Inst.MOV_R16_R16, Inst.MOV_E16_R16);
                        break;
                    case 0x08a:
                    case 0x28a: /* MOV Gb,Eb */
                        instruction8_r(block, Inst.MOV_R8_R8, Inst.MOV_R8_E8);
                        break;
                    case 0x08b: /* MOV Gw,Ew */
                        instruction_r(block, Inst.MOV_R16_R16, Inst.MOV_R16_E16);
                        break;
                    case 0x08c: /* Mov Ew,Sw */ {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00:					/* MOV Ew,ES */
                                block.r2 = CPU_Regs.reg_esVal; break;
                            case 0x01:					/* MOV Ew,CS */
                                block.r2 = CPU_Regs.reg_csVal; break;
                            case 0x02:					/* MOV Ew,SS */
                                block.r2 = CPU_Regs.reg_ssVal; break;
                            case 0x03:					/* MOV Ew,DS */
                                block.r2 = CPU_Regs.reg_dsVal; break;
                            case 0x04:					/* MOV Ew,FS */
                                block.r2 = CPU_Regs.reg_fsVal; break;
                            case 0x05:					/* MOV Ew,GS */
                                block.r2 = CPU_Regs.reg_gsVal; break;
                            default:
                                block.instruction = Inst.ILLEGAL; done = true; break;
                        }
                        if (rm >= 0xc0) {
                            block.instruction = Inst.MOV_R16_R16;
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.instruction = Inst.MOV_E16_R16;
                            block.r1 = block.r2;
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x08d: /* LEA Gw */ {
                        int rm = decode_fetchb();
                        block.instruction = Inst.LEA_R16;
                        block.r1 = Mod.gw(rm);
                        ea(block, rm);
                        block.eaa_segPhys = reg_zero;
                        block.eaa_segVal = reg_zero;
                        break;
                    }
                    case 0x08e:
                    case 0x28e: /* MOV Sw,Ew */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xC0) {
                            switch ((rm >> 3) & 7) {
                                case 0: block.instruction = Inst.MOV_ES_R16; break;
                                case 2: block.instruction = Inst.MOV_SS_R16; doAnother = true; break;
                                case 3: block.instruction = Inst.MOV_DS_R16; break;
                                case 4: block.instruction = Inst.MOV_FS_R16; break;
                                case 5: block.instruction = Inst.MOV_GS_R16; break;
                                default: block.instruction = Inst.ILLEGAL; break;
                            }
                            block.r1 = Mod.ew(rm);
                        } else {
                            switch ((rm >> 3) & 7) {
                                case 0: block.instruction = Inst.MOV_ES_E16; break;
                                case 2: block.instruction = Inst.MOV_SS_E16; doAnother = true; break;
                                case 3: block.instruction = Inst.MOV_DS_E16; break;
                                case 4: block.instruction = Inst.MOV_FS_E16; break;
                                case 5: block.instruction = Inst.MOV_GS_E16; break;
                                default: block.instruction = Inst.ILLEGAL; break;
                            }
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x08f: /* POP Ew */
                        instruction(block, Inst.POP16_R16, Inst.POP16_E16); break;
                    case 0x090:
                    case 0x290: /* NOP */
                        block.instruction = Inst.NOP; break;
                    case 0x091: /* XCHG CX,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_ecx; break;
                    case 0x092: /* XCHG DX,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_edx; break;
                    case 0x093: /* XCHG BX,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_ebx; break;
                    case 0x094: /* XCHG SP,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_esp; break;
                    case 0x095: /* XCHG BP,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_ebp; break;
                    case 0x096: /* XCHG SI,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_esi; break;
                    case 0x097: /* XCHG DI,AX */
                        block.instruction = Inst.XCHG_R16_R16; block.r1 = reg_eax; block.r2 = reg_edi; break;
                    case 0x098: /* CBW */
                        block.instruction = Inst.CBW; break;
                    case 0x099: /* CWD */
                        block.instruction = Inst.CWD; break;
                    case 0x09a: /* CALL Ap */
                        block.instruction = Inst.CALL16_AP;
                        block.value = decode_fetchw();
                        block.eaa_const = decode_fetchw();
                        done=true;
                        break;
                    case 0x09b: /* WAIT */
                        block.instruction = Inst.NOP; break;
                    case 0x09c: /* PUSHF */
                        block.instruction = Inst.PUSHF; block.eaa16 = true; break;
                    case 0x09d: /* POPF */
                        block.instruction = Inst.POPF;  block.eaa16 = true; break;
                    case 0x09e:
                    case 0x29e: /* SAHF */
                        block.instruction = Inst.SAHF; break;
                    case 0x09f:
                    case 0x29f: /* LAHF */
                        block.instruction = Inst.LAHF; break;
                    case 0x0a0: /* MOV AL,Ob */
                        block.instruction = Inst.MOV_AL_0b;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;
                    case 0x0a1: /* MOV AX,Ow */
                        block.instruction = Inst.MOV_AX_0w;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;
                    case 0x0a2: /* MOV Ob,AL */
                        block.instruction = Inst.MOV_0b_AL;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;
                    case 0x0a3: /* MOV Ow,AX */
                        block.instruction = Inst.MOV_0w_AX;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;
                    case 0x0a4:
                    case 0x2a4: /* MOVSB */
                        doFastString(block, Inst.MOVSB16, Inst.MOVSB16r, Inst.MOVSB32, Inst.MOVSB32r); break;
                    case 0x0a5: /* MOVSW */
                        doFastString(block, Inst.MOVSW16, Inst.MOVSW16r, Inst.MOVSW32, Inst.MOVSW32r); break;
                    case 0x0a6:
                    case 0x2a6: /* CMPSB */
                        doString(block, Inst.STRING, StringOp.R_CMPSB, 1); break;
                    case 0x0a7: /* CMPSW */
                        doString(block, Inst.STRING, StringOp.R_CMPSW, 2); break;
                    case 0x0a8:
                    case 0x2a8: /* TEST AL,Ib */
                        block.instruction = Inst.TEST_R8; block.r1 = reg_eax; block.value = decode_fetchb(); break;
                    case 0x0a9: /* TEST AX,Iw */
                        block.instruction = Inst.TEST_R16; block.r1 = reg_eax; block.value = decode_fetchw(); break;
                    case 0x0aa:
                    case 0x2aa: /* STOSB */
                        doString(block, Inst.STRING, StringOp.R_STOSB, 1);
                        break;
                    case 0x0ab: /* STOSW */
                        doString(block, Inst.STRING, StringOp.R_STOSW, 2);
                        break;
                    case 0x0ac:
                    case 0x2ac: /* LODSB */
                        doString(block, Inst.STRING, StringOp.R_LODSB, 1); break;
                    case 0x0ad: /* LODSW */
                        doString(block, Inst.STRING, StringOp.R_LODSW, 2); break;
                    case 0x0ae:
                    case 0x2ae: /* SCASB */
                        doString(block, Inst.STRING, StringOp.R_SCASB, 1); break;
                    case 0x0af: /* SCASW */
                        doString(block, Inst.STRING, StringOp.R_SCASW, 2); break;
                    case 0x0b0:
                    case 0x2b0: /* MOV AL,Ib */
                        instruction(block, Inst.MOV_R8, reg_eax, decode_fetchb()); break;
                    case 0x0b1:
                    case 0x2b1: /* MOV CL,Ib */
                        instruction(block, Inst.MOV_R8, reg_ecx, decode_fetchb()); break;
                    case 0x0b2:
                    case 0x2b2: /* MOV DL,Ib */
                        instruction(block, Inst.MOV_R8, reg_edx, decode_fetchb()); break;
                    case 0x0b3:
                    case 0x2b3: /* MOV BL,Ib */
                        instruction(block, Inst.MOV_R8, reg_ebx, decode_fetchb()); break;
                    case 0x0b4:
                    case 0x2b4: /* MOV AH,Ib */
                        instruction(block, Inst.MOV_R8, reg_ah, decode_fetchb()); break;
                    case 0x0b5:
                    case 0x2b5: /* MOV CH,Ib */
                        instruction(block, Inst.MOV_R8, reg_ch, decode_fetchb()); break;
                    case 0x0b6:
                    case 0x2b6: /* MOV DH,Ib */
                        instruction(block, Inst.MOV_R8, reg_dh, decode_fetchb()); break;
                    case 0x0b7:
                    case 0x2b7: /* MOV BH,Ib */
                        instruction(block, Inst.MOV_R8, reg_bh, decode_fetchb()); break;
                    case 0x0b8: /* MOV AX,Iw */
                        instruction(block, Inst.MOV_R16, reg_eax, decode_fetchw()); break;
                    case 0x0b9: /* MOV CX,Iw */
                        instruction(block, Inst.MOV_R16, reg_ecx, decode_fetchw()); break;
                    case 0x0ba: /* MOV DX,Iw */
                        instruction(block, Inst.MOV_R16, reg_edx, decode_fetchw()); break;
                    case 0x0bb: /* MOV BX,Iw */
                        instruction(block, Inst.MOV_R16, reg_ebx, decode_fetchw()); break;
                    case 0x0bc: /* MOV SP,Iw */
                        instruction(block, Inst.MOV_R16, reg_esp, decode_fetchw()); break;
                    case 0x0bd: /* MOV BP,Iw */
                        instruction(block, Inst.MOV_R16, reg_ebp, decode_fetchw()); break;
                    case 0x0be: /* MOV SI,Iw */
                        instruction(block, Inst.MOV_R16, reg_esi, decode_fetchw()); break;
                    case 0x0bf: /* MOV DI,Iw */
                        instruction(block, Inst.MOV_R16, reg_edi, decode_fetchw()); break;
                    case 0x0c0:
                    case 0x2c0: /* GRP2 Eb,Ib */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.value = decode_fetchb() & 0x1f;
                            group2b(block, rm);
                            block.r1 = Mod.eb(rm);
                        } else {
                            ea(block, rm);
                            block.value = decode_fetchb() & 0x1f;
                            group2bEA(block, rm);
                        }
                        break;
                    }
                    case 0x0c1: /* GRP2 Ew,Ib */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.value = decode_fetchb() & 0x1f;
                            group2w(block, rm);
                            block.r1 = Mod.ew(rm);
                        } else {
                            ea(block, rm);
                            block.value = decode_fetchb() & 0x1f;
                            group2wEA(block, rm);
                        }
                        break;
                    }
                    case 0x0c2: /* RETN Iw */
                        block.instruction = Inst.RETN16_Iw; block.value = decode_fetchw(); done = true; break;
                    case 0x0c3: /* RETN */
                        block.instruction = Inst.RETN16; done = true; break;
                    case 0x0c4: /* LES */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.ILLEGAL;
                            break;
                        }
                        block.instruction = Inst.LES16;
                        block.r1 = Mod.gd(rm);
                        ea(block, rm);
                        break;
                    }
                    case 0x0c5: /* LDS */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.ILLEGAL;
                            break;
                        }
                        block.instruction = Inst.LDS16;
                        block.r1 = Mod.gd(rm);
                        ea(block, rm);
                        break;
                    }
                    case 0x0c6:
                    case 0x2c6: /* MOV Eb,Ib */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.MOV_R8;
                            block.r1 = Mod.eb(rm);
                        } else {
                            block.instruction = Inst.MOV_E8;
                            ea(block, rm);
                        }
                        block.value = decode_fetchb();
                        break;
                    }
                    case 0x0c7: /* MOV EW,Iw */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.MOV_R16;
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.instruction = Inst.MOV_E16;
                            ea(block, rm);
                        }
                        block.value = decode_fetchw();
                        break;
                    }
                    case 0x0c8: /* ENTER Iw,Ib */
                        block.instruction = Inst.ENTER;
                        block.value = decode_fetchw();
                        block.eaa_const = decode_fetchb();
                        block.eaa16 = true;
                        break;
                    case 0x0c9: /* LEAVE */
                        block.instruction = Inst.LEAVE16; break;
                    case 0x0ca: /* RETF Iw */
                        block.instruction = Inst.RETF_Iw; block.value = decode_fetchw(); block.eaa16 = true; done = true; break;
                    case 0x0cb: /* RETF */
                        block.instruction = Inst.RETF_Iw; block.value = 0; block.eaa16 = true; done = true; break;
                    case 0x0cc:
                    case 0x2cc: /* INT3 */
                        block.instruction = Inst.INT3; done = true; break;
                    case 0x0cd:
                    case 0x2cd: /* INT Ib */
                        block.instruction = Inst.INTIb; block.value = decode_fetchb(); done = true; break;
                    case 0x0ce:
                    case 0x2ce: /* INTO */
                        block.instruction = Inst.INTO; break;
                    case 0x0cf: /* IRET */
                        block.instruction = Inst.IRET; block.eaa16 = true; done = true; break;
                    case 0x0d0:
                    case 0x2d0: /* GRP2 Eb,1 */ {
                        int rm = decode_fetchb();
                        block.value = 1;
                        if (rm >= 0xc0) {
                            group2b(block, rm);
                            block.r1 = Mod.eb(rm);
                        } else {
                            group2bEA(block, rm);
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x0d1: /* GRP2 Ew,1 */ {
                        int rm = decode_fetchb();
                        block.value = 1;
                        if (rm >= 0xc0) {
                            group2w(block, rm);
                            block.r1 = Mod.ew(rm);
                        } else {
                            group2wEA(block, rm);
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x0d2:
                    case 0x2d2: /* GRP2 Eb,CL */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            group2bCL(block, rm);
                            block.r1 = Mod.eb(rm);
                        } else {
                            group2bEACL(block, rm);
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x0d3: /* GRP2 Ew,CL */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            group2wCL(block, rm);
                            block.r1 = Mod.ew(rm);
                        } else {
                            group2wEACL(block, rm);
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x0d4:
                    case 0x2d4: /* AAM Ib */
                        block.instruction = Inst.AAD; block.value = decode_fetchb(); break;
                    case 0x0d5:
                    case 0x2d5: /* AAD Ib */
                        block.instruction = Inst.AAD; block.value = decode_fetchb(); break;
                    case 0x0d6:
                    case 0x2d6: /* SALC */
                        block.instruction = Inst.SALC; break;
                    case 0x0d7:
                    case 0x2d7: /* XLAT */
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        if (EA16) block.instruction = Inst.XLAT16;
                        else block.instruction = Inst.XLAT32;
                        break;
                    case 0x0d8:
                    case 0x2d8:                                                                                     /* FPU ESC 0 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU0_normal; else {block.instruction = Inst.FPU0_ea; ea(block, block.value);} break;
                    case 0x0d9:
                    case 0x2d9:                                                                                     /* FPU ESC 1 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU1_normal; else {block.instruction = Inst.FPU1_ea; ea(block, block.value);} break;
                    case 0x0da:
                    case 0x2da:                                                                                     /* FPU ESC 2 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU2_normal; else {block.instruction = Inst.FPU2_ea; ea(block, block.value);} break;
                    case 0x0db:
                    case 0x2db:                                                                                     /* FPU ESC 3 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU3_normal; else {block.instruction = Inst.FPU3_ea; ea(block, block.value);} break;
                    case 0x0dc:
                    case 0x2dc:                                                                                     /* FPU ESC 4 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU4_normal; else {block.instruction = Inst.FPU4_ea; ea(block, block.value);} break;
                    case 0x0dd:
                    case 0x2dd:                                                                                     /* FPU ESC 5 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU5_normal; else {block.instruction = Inst.FPU5_ea; ea(block, block.value);} break;
                    case 0x0de:
                    case 0x2de:                                                                                     /* FPU ESC 6 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU6_normal; else {block.instruction = Inst.FPU6_ea; ea(block, block.value);} break;
                    case 0x0df:
                    case 0x2df:                                                                                     /* FPU ESC 7 */
                        block.value = decode_fetchb(); if (block.value >= 0xc0) block.instruction = Inst.FPU7_normal; else {block.instruction = Inst.FPU7_ea; ea(block, block.value);} break;
                    case 0x0e0: /* LOOPNZ */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOPNZ16_CX;
                        else block.instruction = Inst.LOOPNZ16_ECX;
                        done = true;
                        break;
                    case 0x0e1: /* LOOPZ */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOPZ16_CX;
                        else block.instruction = Inst.LOOPZ16_ECX;
                        done = true;
                        break;
                    case 0x0e2: /* LOOP */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOP16_CX;
                        else block.instruction = Inst.LOOP16_ECX;
                        done = true;
                        break;
                    case 0x0e3: /* JCXZ */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.JCXZ16_CX;
                        else block.instruction = Inst.JCXZ16_ECX;
                        done = true;
                        break;
                    case 0x0e4:
                    case 0x2e4: /* IN AL,Ib */
                        block.value = decode_fetchb(); block.instruction = Inst.IN_AL_Ib; break;
                    case 0x0e5: /* IN AX,Ib */
                        block.value = decode_fetchb(); block.instruction = Inst.IN_AX_Ib; break;
                    case 0x0e6:
                    case 0x2e6: /* OUT Ib,AL */
                        block.value = decode_fetchb(); block.instruction = Inst.OUT_Ib_AL; break;
                    case 0x0e7: /* OUT Ib,AX */
                        block.value = decode_fetchb(); block.instruction = Inst.OUT_Ib_AL; break;
                    case 0x0e8: /* CALL Jw */
                        block.value = decode_fetchws(); block.instruction = Inst.CALL16_Jw; done=true; break;
                    case 0x0e9: /* JMP Jw */
                        block.value = decode_fetchws(); block.instruction = Inst.JMP16; done = true; break;
                    case 0x0ea: /* JMP Ap */
                        block.value = decode_fetchw(); block.eaa_const = decode_fetchw(); block.instruction = Inst.JMP_AP; block.eaa16 = true; done = true; break;
                    case 0x0eb: /* JMP Jb */
                        block.value = decode_fetchbs(); block.instruction = Inst.JMP16; done = true; break;
                    case 0x0ec:
                    case 0x2ec: /* IN AL,DX */
                        block.instruction = Inst.IN_AL_DX; break;
                    case 0x0ed: /* IN AX,DX */
                        block.instruction = Inst.IN_AX_DX; break;
                    case 0x0ee:
                    case 0x2ee: /* OUT DX,AL */
                        block.instruction = Inst.OUT_DX_AL; break;
                    case 0x0ef: /* OUT DX,AX */
                        block.instruction = Inst.OUT_DX_AX; break;
                    case 0x0f0:
                    case 0x2f0: /* LOCK */
                        prefixes |= Core.PREFIX_LOCK; continue;
                    case 0x0f1:
                    case 0x2f1: /* ICEBP */
                        block.instruction = Inst.ICEBP; break;
                    case 0x0f2:
                    case 0x2f2: /* REPNZ */
                        prefixes |= Core.PREFIX_REP; rep_zero = false; continue;
                    case 0x0f3:
                    case 0x2f3: /* REPZ */
                        prefixes |= Core.PREFIX_REP; rep_zero = true; continue;
                    case 0x0f4:
                    case 0x2f4: /* HLT */
                        block.instruction = Inst.HLT; done = true; break;
                    case 0x0f5:
                    case 0x2f5: /* CMC */
                        block.instruction = Inst.CMC; break;
                    case 0x0f6:
                    case 0x2f6: /* GRP3 Eb(,Ib) */ {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00: /* TEST Eb,Ib */
                            case 0x01: /* TEST Eb,Ib Undocumented*/
                                if (rm >= 0xc0) {
                                    block.value = decode_fetchb();
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.TEST_R8;
                                } else {
                                    block.instruction = Inst.TEST_E8;
                                    ea(block, rm);
                                    block.value = decode_fetchb();
                                }
                                break;
                            case 0x02: /* NOT Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.NOT_R8;
                                } else {
                                    block.instruction = Inst.NOT_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x03: /* NEG Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.NEG_R8;
                                } else {
                                    block.instruction = Inst.NEG_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x04: /* MUL AL,Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.MUL_R8;
                                } else {
                                    block.instruction = Inst.IMUL_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x05: /* IMUL AL,Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.IMUL_R8;
                                } else {
                                    block.instruction = Inst.IMUL_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x06: /* DIV Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.DIV_R8;
                                } else {
                                    block.instruction = Inst.DIV_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x07: /* IDIV Eb */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.eb(rm);
                                    block.instruction = Inst.IDIV_R8;
                                } else {
                                    block.instruction = Inst.IDIV_E8;
                                    ea(block, rm);
                                }
                                break;
                        }
                        break;
                    }
                    case 0x0f7: /* GRP3 Ew(,Iw) */
                    {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00: /* TEST Ew,Iw */
                            case 0x01: /* TEST Ew,Iw Undocumented*/
                                if (rm >= 0xc0) {
                                    block.value = decode_fetchw();
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.TEST_R16;
                                } else {
                                    block.instruction = Inst.TEST_E16;
                                    ea(block, rm);
                                    block.value = decode_fetchw();
                                }
                                break;
                            case 0x02: /* NOT Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.NOT_R16;
                                } else {
                                    block.instruction = Inst.NOT_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x03: /* NEG Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.NEG_R16;
                                } else {
                                    block.instruction = Inst.NEG_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x04: /* MUL AX,Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.MUL_R16;
                                } else {
                                    block.instruction = Inst.IMUL_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x05: /* IMUL AX,Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.IMUL_R16;
                                } else {
                                    block.instruction = Inst.IMUL_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x06: /* DIV Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.DIV_R16;
                                } else {
                                    block.instruction = Inst.DIV_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x07: /* IDIV Ew */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ew(rm);
                                    block.instruction = Inst.IDIV_R16;
                                } else {
                                    block.instruction = Inst.IDIV_E16;
                                    ea(block, rm);
                                }
                                break;
                        }
                        break;
                    }
                    case 0x0f8:
                    case 0x2f8: /* CLC */
                        block.instruction=Inst.CLC;break;
                    case 0x0f9:
                    case 0x2f9: /* STC */
                        block.instruction=Inst.STC;break;
                    case 0x0fa:
                    case 0x2fa: /* CLI */
                        block.instruction=Inst.CLI;break;
                    case 0x0fb:
                    case 0x2fb: /* STI */
                        block.instruction=Inst.STI;break;
                    case 0x0fc:
                    case 0x2fc: /* CLD */
                        block.instruction=Inst.CLD;break;
                    case 0x0fd:
                    case 0x2fd: /* STD */
                        block.instruction=Inst.STD;break;
                    case 0x0fe:
                    case 0x2fe: /* GRP4 Eb */ {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00: /* INC Eb */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.INC_R8;
                                    block.r1 = Mod.eb(rm);
                                } else {
                                    block.instruction = Inst.INC_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x01: /* DEC Eb */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.DEC_R8;
                                    block.r1 = Mod.eb(rm);
                                } else {
                                    block.instruction = Inst.DEC_E8;
                                    ea(block, rm);
                                }
                                break;
                            case 0x07: /* CallBack */
                                block.value = decode_fetchw();
                                block.instruction = Inst.CALLBACK;
                                done=true;
                                break;
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
                            case 0x00: /* INC Ew */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.INC_R16;
                                    block.r1 = Mod.ew(rm);
                                } else {
                                    block.instruction = Inst.INC_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x01: /* DEC Ew */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.DEC_R16;
                                    block.r1 = Mod.ew(rm);
                                } else {
                                    block.instruction = Inst.DEC_E16;
                                    ea(block, rm);
                                }
                                break;
                            case 0x02: /* CALL Ev */
                                if (rm >= 0xc0 ) {
                                    block.instruction=Inst.CALL16_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.CALL16_E16;
                                    ea(block, rm);
                                }
                                done = true;
                                break;
                            case 0x03: /* CALL Ep */
                            {
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.ILLEGAL;
                                } else {
                                    block.instruction=Inst.CALL16_EP_E16;
                                    ea(block, rm);
                                }
                                done=true;
                                break;
                            }
                            case 0x04: /* JMP Ev */
                                if (rm >= 0xc0 ) {
                                    block.instruction=Inst.JMP16_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.JMP16_E16;
                                    ea(block, rm);
                                }
                                done = true;
                                break;
                            case 0x05: /* JMP Ep */
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.ILLEGAL;
                                } else {
                                    block.instruction=Inst.JMP16_EP;
                                    ea(block, rm);
                                }
                                done=true;
                                break;
                            case 0x06: /* PUSH Ev */
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.PUSH16_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.PUSH16_E16;
                                    ea(block, rm);
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
                    case 0x100:
                    case 0x300: /* GRP 6 Exxx */
                    {
                        int rm=decode_fetchb();
                        switch ((rm>>3)&7) {
                            case 0x00:	/* SLDT */
                                instructionE32(block, Inst.SLDT_R16, Inst.SLDT_E16); break;
                            case 0x01:	/* STR */
                                instructionE32(block, Inst.STR_R16, Inst.STR_E16); break;
                            case 0x02:
                                instructionE32(block, Inst.LLDT_R16, Inst.LLDT_E16); break;
                            case 0x03:
                                instructionE32(block, Inst.LTR_R16, Inst.LTR_E16); break;
                            case 0x04:
                                instructionE32(block, Inst.VERR_R16, Inst.VERR_E16); break;
                            case 0x05:
                                instructionE32(block, Inst.VERW_R16, Inst.VERW_E16); break;
                            default:
                                block.instruction = Inst.ILLEGAL; done=true; break;
                        }
                        break;
                    }
                    case 0x101: /* Group 7 Ew */
                    {
                        int rm=decode_fetchb();
                        int which=(rm>>3)&7;
                        if (rm < 0xc0)	{
                            ea(block, rm);
                            switch (which) {
                                case 0x00: block.instruction = Inst.SGDT; break;
                                case 0x01: block.instruction = Inst.SIDT; break;
                                case 0x02: block.instruction = Inst.LGDT16; break;
                                case 0x03: block.instruction = Inst.LIDT16; break;
                                case 0x04: block.instruction = Inst.SMSW_E16; break;
                                case 0x05: block.instruction = Inst.ILLEGAL; done = true; break;
                                case 0x06: block.instruction = Inst.LMSW_E16; break;
                                case 0x07: block.instruction = Inst.INVLPG; break;
                            }
                        } else {
                            block.r1=Mod.ew(rm);
                            switch (which) {
                                case 0x02: block.instruction = Inst.LGDT_R; break;
                                case 0x03: block.instruction = Inst.LIDT_R; break;
                                case 0x04: block.instruction = Inst.SMSW_R16; break;
                                case 0x06: block.instruction = Inst.LMSW_R16; break;
                                default: block.instruction = Inst.ILLEGAL; done = true; break;
                            }
                        }
                        break;
                    }
                    case 0x206: /* PUSH ES */
                        block.instruction = Inst.PUSH32_ES;
                        break;
                    case 0x207: /* POP ES */
                        block.instruction = Inst.POP32_ES;
                        break;
                    case 0x20e: /* PUSH CS */
                        block.instruction = Inst.PUSH32_CS;
                        break;
                    case 0x216: /* PUSH SS */
                        block.instruction = Inst.PUSH32_SS;
                        break;
                    case 0x217: /* POP SS */
                        block.instruction = Inst.POP32_SS;
                        break;
                    case 0x21e: /* PUSH DS */
                        block.instruction = Inst.PUSH32_DS;
                        break;
                    case 0x21f: /* POP DS */
                        block.instruction = Inst.POP32_DS;
                        break;
                    case 0x240: /* INC EAX */
                        instruction(block, Inst.INC_R32, reg_eax);
                        break;                                  
                    case 0x241: /* INC ECX */
                        instruction(block, Inst.INC_R32, reg_ecx);
                        break;                                  
                    case 0x242: /* INC EDX */
                        instruction(block, Inst.INC_R32, reg_edx);
                        break;                                  
                    case 0x243: /* INC EBX */
                        instruction(block, Inst.INC_R32, reg_ebx);
                        break;                                  
                    case 0x244: /* INC ESP */
                        instruction(block, Inst.INC_R32, reg_esp);
                        break;                                  
                    case 0x245: /* INC EBP */
                        instruction(block, Inst.INC_R32, reg_ebp);
                        break;                                  
                    case 0x246: /* INC ESI */
                        instruction(block, Inst.INC_R32, reg_esi);
                        break;                                  
                    case 0x247: /* INC EDI */
                        instruction(block, Inst.INC_R32, reg_edi);
                        break;                                  
                    case 0x248: /* DEC EAX */
                        instruction(block, Inst.DEC_R32, reg_eax);
                        break;                                  
                    case 0x249: /* DEC ECX */
                        instruction(block, Inst.DEC_R32, reg_ecx);
                        break;                                  
                    case 0x24a: /* DEC EDX */
                        instruction(block, Inst.DEC_R32, reg_edx);
                        break;                                  
                    case 0x24b: /* DEC EBX */
                        instruction(block, Inst.DEC_R32, reg_ebx);
                        break;                                  
                    case 0x24c: /* DEC ESP */
                        instruction(block, Inst.DEC_R32, reg_esp);
                        break;                                  
                    case 0x24d: /* DEC EBP */
                        instruction(block, Inst.DEC_R32, reg_ebp);
                        break;                                  
                    case 0x24e: /* DEC ESI */
                        instruction(block, Inst.DEC_R32, reg_esi);
                        break;                                  
                    case 0x24f: /* DEC EDI */
                        instruction(block, Inst.DEC_R32, reg_edi);
                        break;                                  

                    case 0x250: /* PUSH EAX */
                        instruction(block, Inst.PUSH32_R32, reg_eax);
                        break;
                    case 0x251: /* PUSH ECX */
                        instruction(block, Inst.PUSH32_R32, reg_ecx);
                        break;
                    case 0x252: /* PUSH EDX */
                        instruction(block, Inst.PUSH32_R32, reg_edx);
                        break;
                    case 0x253: /* PUSH EBX */
                        instruction(block, Inst.PUSH32_R32, reg_ebx);
                        break;
                    case 0x254: /* PUSH ESP */
                        instruction(block, Inst.PUSH32_R32, reg_esp);
                        break;
                    case 0x255: /* PUSH EBP */
                        instruction(block, Inst.PUSH32_R32, reg_ebp);
                        break;
                    case 0x256: /* PUSH ESI */
                        instruction(block, Inst.PUSH32_R32, reg_esi);
                        break;
                    case 0x257: /* PUSH EDI */
                        instruction(block, Inst.PUSH32_R32, reg_edi);
                        break;
                    case 0x258: /* POP EAX */
                        instruction(block, Inst.POP32_R32, reg_eax);
                        break;
                    case 0x259: /* POP ECX */
                        instruction(block, Inst.POP32_R32, reg_ecx);
                        break;
                    case 0x25a: /* POP EDX */
                        instruction(block, Inst.POP32_R32, reg_edx);
                        break;
                    case 0x25b: /* POP EBX */
                        instruction(block, Inst.POP32_R32, reg_ebx);
                        break;
                    case 0x25c: /* POP ESP */
                        instruction(block, Inst.POP32_R32, reg_esp);
                        break;
                    case 0x25d: /* POP EBP */
                        instruction(block, Inst.POP32_R32, reg_ebp);
                        break;
                    case 0x25e: /* POP SI */
                        instruction(block, Inst.POP32_R32, reg_esi);
                        break;
                    case 0x25f: /* POP DI */
                        instruction(block, Inst.POP32_R32, reg_edi);
                        break;
                    case 0x260: /* PUSHAD */
                        block.instruction = Inst.PUSH32A;
                        break;
                    case 0x261: /* POPAD */
                        block.instruction = Inst.POP32A;
                        break;
                    case 0x262: /* BOUND Ed */ {
                        int rm = decode_fetchb();
                        block.r1 = Mod.ed(rm);
                        ea(block, rm);
                        block.instruction = Inst.BOUND32;
                        break;                        
                    }
                    case 0x263: /* ARPL Ed,Rd */
                        instruction(block, Inst.ARPL_R32_R32, Inst.ARPL_R32_E32); break;
                    case 0x268: /* PUSH Id */
                        block.instruction = Inst.PUSH32; block.value = decode_fetchd(); break;
                    case 0x269: /* IMUL Gd,Ed,Id */
                        instruction(block, Inst.IMUL_R32_R32, Inst.IMUL_R32_E32); block.value = decode_fetchds(); break;
                    case 0x26a: /* PUSH Ib */
                        block.instruction = Inst.PUSH32; block.value = decode_fetchbs(); break;
                    case 0x26b: /* IMUL Gd,Ed,Ib */
                        instruction(block, Inst.IMUL_R32_R32, Inst.IMUL_R32_E32); block.value = decode_fetchbs(); break;
                    case 0x26d: /* INSD */
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_INSD, 4); break;
                    case 0x26f: /* OUTSD */
                        doString(block, Inst.STRING_EXCEPTION, StringOp.R_OUTSD, 4); break;
                    case 0x270: /* JO */
                        block.instruction = Inst.JUMP32_JO; block.value = decode_fetchbs(); done = true; break;
                    case 0x271: /* NJO */
                        block.instruction = Inst.JUMP32_NJO; block.value = decode_fetchbs(); done = true; break;
                    case 0x272: /* JB */
                        block.instruction = Inst.JUMP32_B; block.value = decode_fetchbs(); done = true; break;
                    case 0x273: /* NB */
                        block.instruction = Inst.JUMP32_NB; block.value = decode_fetchbs(); done = true; break;
                    case 0x274: /* Z */
                        block.instruction = Inst.JUMP32_Z; block.value = decode_fetchbs(); done = true; break;
                    case 0x275: /* NZ */
                        block.instruction = Inst.JUMP32_NZ; block.value = decode_fetchbs(); done = true; break;
                    case 0x276: /* BE */
                        block.instruction = Inst.JUMP32_BE; block.value = decode_fetchbs(); done = true; break;
                    case 0x277: /* NBE */
                        block.instruction = Inst.JUMP32_NBE; block.value = decode_fetchbs(); done = true; break;
                    case 0x278: /* JS */
                        block.instruction = Inst.JUMP32_S; block.value = decode_fetchbs(); done = true; break;
                    case 0x279: /* NS */
                        block.instruction = Inst.JUMP32_NS; block.value = decode_fetchbs(); done = true; break;
                    case 0x27a: /* P */
                        block.instruction = Inst.JUMP32_P; block.value = decode_fetchbs(); done = true; break;
                    case 0x27b: /* NP */
                        block.instruction = Inst.JUMP32_NP; block.value = decode_fetchbs(); done = true; break;
                    case 0x27c: /* L */
                        block.instruction = Inst.JUMP32_L; block.value = decode_fetchbs(); done = true; break;
                    case 0x27d: /* NL */
                        block.instruction = Inst.JUMP32_NL; block.value = decode_fetchbs(); done = true; break;
                    case 0x27e: /* LE */
                        block.instruction = Inst.JUMP32_LE; block.value = decode_fetchbs(); done = true; break;
                    case 0x27f: /* NLE */
                        block.instruction = Inst.JUMP32_NLE; block.value = decode_fetchbs(); done = true; break;
                    case 0x281: /* Grpl Ed,Id */
                    {
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ed(rm);
                            block.value = decode_fetchd();
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_R32; break;
                                case 0x01: block.instruction = Inst.OR_R32; break;
                                case 0x02: block.instruction = Inst.ADC_R32; break;
                                case 0x03: block.instruction = Inst.SBB_R32; break;
                                case 0x04: block.instruction = Inst.AND_R32; break;
                                case 0x05: block.instruction = Inst.SUB_R32; break;
                                case 0x06: block.instruction = Inst.XOR_R32; break;
                                case 0x07: block.instruction = Inst.CMP_R32; break;
                            }
                        } else {
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_E32; break;
                                case 0x01: block.instruction = Inst.OR_E32; break;
                                case 0x02: block.instruction = Inst.ADC_E32; break;
                                case 0x03: block.instruction = Inst.SBB_E32; break;
                                case 0x04: block.instruction = Inst.AND_E32; break;
                                case 0x05: block.instruction = Inst.SUB_E32; break;
                                case 0x06: block.instruction = Inst.XOR_E32; break;
                                case 0x07: block.instruction = Inst.CMP_E32;break;
                            }
                            ea(block, rm);
                            block.value = decode_fetchd();
                        }
                        break;
                    }
                    case 0x283: /* Grpl Ed,Ix */
                    {
                        int rm = decode_fetchb();
                        int which = (rm >> 3) & 7;
                        if (rm >= 0xc0) {
                            block.r1 = Mod.ed(rm);
                            block.value = decode_fetchbs();
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_R32; break;
                                case 0x01: block.instruction = Inst.OR_R32; break;
                                case 0x02: block.instruction = Inst.ADC_R32; break;
                                case 0x03: block.instruction = Inst.SBB_R32; break;
                                case 0x04: block.instruction = Inst.AND_R32; break;
                                case 0x05: block.instruction = Inst.SUB_R32; break;
                                case 0x06: block.instruction = Inst.XOR_R32; break;
                                case 0x07: block.instruction = Inst.CMP_R32; break;
                            }
                        } else {
                            switch (which) {
                                case 0x00: block.instruction = Inst.ADD_E32; break;
                                case 0x01: block.instruction = Inst.OR_E32; break;
                                case 0x02: block.instruction = Inst.ADC_E32; break;
                                case 0x03: block.instruction = Inst.SBB_E32; break;
                                case 0x04: block.instruction = Inst.AND_E32; break;
                                case 0x05: block.instruction = Inst.SUB_E32; break;
                                case 0x06: block.instruction = Inst.XOR_E32; break;
                                case 0x07: block.instruction = Inst.CMP_E32; break;
                            }
                            ea(block, rm);
                            block.value = decode_fetchbs();
                        }
                        break;
                    }
                    case 0x285: /* TEST Ed,Gd */
                        instruction(block, Inst.TEST_R32_R32, Inst.TEST_E32_R32);
                        break;
                    case 0x287: /* XCHG Ed,Gd */
                        instruction(block, Inst.XCHG_R32_R32, Inst.XCHG_E32_R32);
                        break;
                    case 0x289: /* MOV Ed,Gd */
                        instruction(block, Inst.MOV_R32_R32, Inst.MOV_E32_R32);
                        break;
                    case 0x28b: /* MOV Gd,Ed */
                        instruction_r(block, Inst.MOV_R32_R32, Inst.MOV_R32_E32);
                        break;
                    case 0x28c: /* Mov Ew,Sw */
                    {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00: block.r2 = CPU_Regs.reg_esVal; break;
                            case 0x01: block.r2 = CPU_Regs.reg_csVal; break;
                            case 0x02: block.r2 = CPU_Regs.reg_ssVal; break;
                            case 0x03: block.r2 = CPU_Regs.reg_dsVal; break;
                            case 0x04: block.r2 = CPU_Regs.reg_fsVal; break;
                            case 0x05: block.r2 = CPU_Regs.reg_gsVal; break;
                            default: block.instruction = Inst.ILLEGAL; done = true; break;
                        }
                        if (rm >= 0xc0) {
                            block.instruction = Inst.MOV_R32_R32;
                            block.r1 = Mod.ew(rm);
                        } else {
                            block.instruction = Inst.MOV_E32_R32;
                            block.r1 = block.r2;
                            ea(block, rm);
                        }
                        break;                    
                    }
                    case 0x28d: /* LEA Gd */ {
                        int rm = decode_fetchb();
                        block.instruction = Inst.LEA_R32;
                        block.r1 = Mod.gd(rm);
                        ea(block, rm);
                        block.eaa_segPhys = reg_zero;
                        block.eaa_segVal = reg_zero;
                        break;
                    }
                    case 0x28f: /* POP Ed */
                        instruction(block, Inst.POP32_R32, Inst.POP32_E32); break;
                    case 0x291: /* XCHG ECX,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_ecx; break;
                    case 0x292: /* XCHG EDX,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_edx; break;
                    case 0x293: /* XCHG EBX,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_ebx; break;
                    case 0x294: /* XCHG ESP,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_esp; break;
                    case 0x295: /* XCHG EBP,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_ebp; break;
                    case 0x296: /* XCHG ESI,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_esi; break;
                    case 0x297: /* XCHG EDI,EAX */
                        block.instruction = Inst.XCHG_R32_R32; block.r1 = reg_eax; block.r2 = reg_edi; break;
                    case 0x298: /* CWDE */
                        block.instruction = Inst.CBW; break;
                    case 0x299: /* CDQ */
                        block.instruction = Inst.CDQ; break;
                    case 0x29a: /* CALL FAR Ad */
                        block.instruction = Inst.CALL32_AP; block.value = decode_fetchd(); block.eaa_const = decode_fetchw(); done=true; break;
                    case 0x29c: /* PUSHFD */
                        block.instruction = Inst.PUSHF; block.eaa16 = false; break;
                    case 0x29d: /* POPFD */
                        block.instruction = Inst.POPF; block.eaa16 = false; break;
                    case 0x2a1: /* MOV EAX,Od */
                        block.instruction = Inst.MOV_EAX_0d;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                        break;
                    case 0x2a3: /* MOV Od,EAX */
                        block.instruction = Inst.MOV_0d_EAX;
                        block.value = (EA16 ? decode_fetchw() : decode_fetchd());
                        if (block.eaa_segPhys == null) {
                            block.eaa_segPhys = reg_dsPhys;
                            block.eaa_segVal = reg_dsVal;
                        }
                    case 0x2a5: /* MOVSD */
                        doFastString(block, Inst.MOVSD16, Inst.MOVSD16r, Inst.MOVSD32, Inst.MOVSD32r); break;
                    case 0x2a7: /* CMPSD */
                        doString(block, Inst.STRING, StringOp.R_CMPSD, 4); break;
                    case 0x2a9: /* TEST EAX,Id */
                        block.instruction = Inst.TEST_R32; block.r1 = reg_eax; block.value = decode_fetchd(); break;
                    case 0x2ab: /* STOSD */
                        doString(block, Inst.STRING, StringOp.R_STOSD, 4); break;
                    case 0x2ad: /* LODSD */
                        doString(block, Inst.STRING, StringOp.R_LODSD, 4); break;
                    case 0x2af: /* SCASD */
                        doString(block, Inst.STRING, StringOp.R_SCASD, 4); break;
                    case 0x2b8: /* MOV EAX,Id */
                        instruction(block, Inst.MOV_R32, reg_eax, decode_fetchd()); break;
                    case 0x2b9: /* MOV ECX,Id */
                        instruction(block, Inst.MOV_R32, reg_ecx, decode_fetchd()); break;
                    case 0x2ba: /* MOV EDX,Id */
                        instruction(block, Inst.MOV_R32, reg_edx, decode_fetchd()); break;
                    case 0x2bb: /* MOV EBX,Id */
                        instruction(block, Inst.MOV_R32, reg_ebx, decode_fetchd()); break;
                    case 0x2bc: /* MOV ESP,Id */
                        instruction(block, Inst.MOV_R32, reg_esp, decode_fetchd()); break;
                    case 0x2bd: /* MOV EBP,Id */
                        instruction(block, Inst.MOV_R32, reg_ebp, decode_fetchd()); break;
                    case 0x2be: /* MOV ESI,Id */
                        instruction(block, Inst.MOV_R32, reg_esi, decode_fetchd()); break;
                    case 0x2bf: /* MOV EDI,Id */
                        instruction(block, Inst.MOV_R32, reg_edi, decode_fetchd()); break;
                    case 0x2c1: /* GRP2 Ed,Ib */ {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.value = decode_fetchb() & 0x1f;
                            group2d(block, rm);
                            block.r1 = Mod.ed(rm);
                        } else {
                            ea(block, rm);
                            block.value = decode_fetchb() & 0x1f;
                            group2dEA(block, rm);
                        }
                        break;
                    }
                    case 0x2c2: /* RETN Iw */
                        block.instruction = Inst.RETN32_Iw; block.value = decode_fetchw(); done=true; break;
                    case 0x2c3: /* RETN */
                        block.instruction = Inst.RETN32; done=true; break;
                    case 0x2c4: /* LES */
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.ILLEGAL;
                            break;
                        }
                        block.instruction = Inst.LES32;
                        block.r1 = Mod.gd(rm);
                        ea(block, rm);
                        break;
                    }
                    case 0x2c5: /* LDS */
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.ILLEGAL;
                            break;
                        }
                        block.instruction = Inst.LDS32;
                        block.r1 = Mod.gd(rm);
                        ea(block, rm);
                        break;
                    }
                    case 0x2c7: /* MOV Ed,Id */
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            block.instruction = Inst.MOV_R32;
                            block.r1 = Mod.ed(rm);
                        } else {
                            block.instruction = Inst.MOV_E32;
                            ea(block, rm);
                        }
                        block.value = decode_fetchd();
                        break;
                    }
                    case 0x2c8: /* ENTER Iw,Ib */
                        block.instruction = Inst.ENTER;
                        block.value = decode_fetchw();
                        block.eaa_const = decode_fetchb();
                        block.eaa16 = false;
                        break;
                    case 0x2c9: /* LEAVE */
                        block.instruction = Inst.LEAVE32; break;
                    case 0x2ca: /* RETF Iw */
                        block.instruction = Inst.RETF_Iw; block.value = decode_fetchw(); block.eaa16 = false; done = true; break;
                    case 0x2cb: /* RETF */
                        block.instruction = Inst.RETF_Iw; block.value = 0; block.eaa16 = false; done = true; break;
                    case 0x2cf: /* IRET */
                        block.instruction = Inst.IRET; block.eaa16 = false; done = true; break;
                    case 0x2d1: /* GRP2 Ed,1 */
                    {
                        int rm = decode_fetchb();
                        block.value = 1;
                        if (rm >= 0xc0) {
                            group2d(block, rm);
                            block.r1 = Mod.ew(rm);
                        } else {
                            group2dEA(block, rm);
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x2d3: /* GRP2 Ed,CL */
                    {
                        int rm = decode_fetchb();
                        if (rm >= 0xc0) {
                            group2dCL(block, rm);
                            block.r1 = Mod.ew(rm);
                        } else {
                            group2dEACL(block, rm);
                            ea(block, rm);
                        }
                        break;
                    }
                    case 0x2e0: /* LOOPNZ */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOPNZ32_CX;
                        else block.instruction = Inst.LOOPNZ32_ECX;
                        done = true;
                        break;
                    case 0x2e1: /* LOOPZ */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOPZ32_CX;
                        else block.instruction = Inst.LOOPZ32_ECX;
                        done = true;
                        break;
                    case 0x2e2: /* LOOP */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.LOOP32_CX;
                        else block.instruction = Inst.LOOP32_ECX;
                        done = true;
                        break;
                    case 0x2e3: /* JCXZ */
                        block.value = decode_fetchbs();
                        if (EA16) block.instruction = Inst.JCXZ32_CX;
                        else block.instruction = Inst.JCXZ32_ECX;
                        done = true;
                        break;
                    case 0x2e5: /* IN EAX,Ib */
                        block.value = decode_fetchb(); block.instruction = Inst.IN_EAX_Ib; break;
                    case 0x2e7: /* OUT Ib,EAX */
                        block.value = decode_fetchb(); block.instruction = Inst.OUT_Ib_EAX; break;
                    case 0x2e8: /* CALL Jd */
                        block.value = decode_fetchds(); block.instruction = Inst.CALL32_Jd; done=true; break;
                    case 0x2e9: /* JMP Jd */
                        block.value = decode_fetchds(); block.instruction = Inst.JMP32; done = true; break;
                    case 0x2ea: /* JMP Ad */
                        block.value = decode_fetchd(); block.eaa_const = decode_fetchw(); block.instruction = Inst.JMP_AP; block.eaa16 = false; done = true; break;
                    case 0x2eb: /* JMP Jb */
                        block.value = decode_fetchbs(); block.instruction = Inst.JMP32; done = true; break;
                    case 0x2ed: /* IN EAX,DX */
                        block.instruction = Inst.IN_EAX_DX; break;
                    case 0x2ef: /* OUT DX,EAX */
                        block.instruction = Inst.OUT_DX_EAX; break;
                    case 0x2f7: /* GRP3 Ed(,Id) */
                    {
                        int rm = decode_fetchb();
                        switch ((rm >> 3) & 7) {
                            case 0x00: /* TEST Ed,Id */
                            case 0x01: /* TEST Ed,Id Undocumented*/
                                if (rm >= 0xc0) {
                                    block.value = decode_fetchd();
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.TEST_R32;
                                } else {
                                    block.instruction = Inst.TEST_E32;
                                    ea(block, rm);
                                    block.value = decode_fetchd();
                                }
                                break;
                            case 0x02: /* NOT Ed */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.NOT_R32;
                                } else {
                                    block.instruction = Inst.NOT_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x03: /* NEG Ed */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.NEG_R32;
                                } else {
                                    block.instruction = Inst.NEG_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x04: /* MUL EAX,Ed */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.MUL_R32;
                                } else {
                                    block.instruction = Inst.IMUL_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x05: /* IMUL EAX,Ed */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.IMUL_R32;
                                } else {
                                    block.instruction = Inst.IMUL_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x06: /* DIV Ed */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.DIV_R32;
                                } else {
                                    block.instruction = Inst.DIV_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x07: /* IDIV Ed */
                                if (rm >= 0xc0) {
                                    block.r1 = Mod.ed(rm);
                                    block.instruction = Inst.IDIV_R32;
                                } else {
                                    block.instruction = Inst.IDIV_E32;
                                    ea(block, rm);
                                }
                                break;
                        }
                        break;
                    }
                    case 0x2ff: /* GRP 5 Ed */
                    {
                        int rm=decode_fetchb();
                        switch ((rm>>3)&7) {
                            case 0x00: /* INC Ed */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.INC_R32;
                                    block.r1 = Mod.ed(rm);
                                } else {
                                    block.instruction = Inst.INC_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x01: /* DEC Ed */
                                if (rm >= 0xc0) {
                                    block.instruction = Inst.DEC_R32;
                                    block.r1 = Mod.ed(rm);
                                } else {
                                    block.instruction = Inst.DEC_E32;
                                    ea(block, rm);
                                }
                                break;
                            case 0x02: /* CALL NEAR Ed */
                                if (rm >= 0xc0 ) {
                                    block.instruction=Inst.CALL32_R16;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.CALL32_E16;
                                    ea(block, rm);
                                }
                                done = true;
                                break;
                            case 0x03: /* CALL FAR Ed */
                            {
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.ILLEGAL;
                                } else {
                                    block.instruction=Inst.CALL32_EP_E32;
                                    ea(block, rm);
                                }
                                done=true;
                                break;
                            }
                            case 0x04: /* JMP NEAR Ed */
                                if (rm >= 0xc0 ) {
                                    block.instruction=Inst.JMP32_R32;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.JMP32_E32;
                                    ea(block, rm);
                                }
                                done = true;
                                break;
                            case 0x05: /* JMP FAR Ed */
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.ILLEGAL;
                                } else {
                                    block.instruction=Inst.JMP32_EP;
                                    ea(block, rm);
                                }
                                done=true;
                                break;
                            case 0x06: /* Push Ed */
                                if (rm >= 0xc0) {
                                    block.instruction=Inst.PUSH32_R32;
                                    block.r1=Mod.ew(rm);
                                } else {
                                    block.instruction=Inst.PUSH32_E32;
                                    ea(block, rm);
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
                        Log.exit("Unknown instruction: 0x"+Integer.toHexString(opcode));
                }
                currentInst++;
                block.eipCount+=(decode.code - decode.op_start);

                if (decode.modifiedAlot) {
                    decode_putback(block.eipCount);
                    block.instruction = Inst.MODIFIED;
                    done = true;
                }

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
