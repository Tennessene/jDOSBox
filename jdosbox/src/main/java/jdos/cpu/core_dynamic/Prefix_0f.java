package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.Instructions;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Prefix_0f extends Helper {
    static public void init(Decode[] ops) {
        /* GRP 6 Exxx */
        ops[0x100] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:	/* SLDT */
                    if (rm>=0xC0)
                        prev.next = new Inst2.Sldt_reg(rm);
                    else
                        prev.next = new Inst2.Sldt_mem(rm);
                    break;
                case 0x01:	/* STR */
                    if (rm>=0xC0)
                        prev.next = new Inst2.Str_reg(rm);
                    else
                        prev.next = new Inst2.Str_mem(rm);
                    break;
                case 0x02:
                    if (rm>=0xC0)
                        prev.next = new Inst2.Lldt_reg(rm);
                    else
                        prev.next = new Inst2.Lldt_mem(rm);
                    break;
                 case 0x03:
                    if (rm>=0xC0)
                        prev.next = new Inst2.Ltr_reg(rm);
                    else
                        prev.next = new Inst2.Ltr_mem(rm);
                    break;
                 case 0x04:
                    if (rm>=0xC0)
                        prev.next = new Inst2.Verr_reg(rm);
                    else
                        prev.next = new Inst2.Verr_mem(rm);
                    break;
                 case 0x05:
                    if (rm>=0xC0)
                        prev.next = new Inst2.Verw_reg(rm);
                    else
                        prev.next = new Inst2.Verw_mem(rm);
                    break;
                default:
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }
                return RESULT_HANDLED;
            }
        };

        /* Group 7 Ew */
        ops[0x101] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                int which=(rm>>3)&7;
                if (rm < 0xc0)	{
                    switch (which) {
                    case 0x00:										/* SGDT */
                        prev.next = new Inst2.Sgdt_mem(rm);
                        break;
                    case 0x01:										/* SIDT */
                        prev.next = new Inst2.Sidt_mem(rm);
                        break;
                    case 0x02:										/* LGDT */
                        prev.next = new Inst2.Lgdt_mem(rm);
                        break;
                    case 0x03:										/* LIDT */
                        prev.next = new Inst2.Lidt_mem(rm);
                        break;
                    case 0x04:										/* SMSW */
                        prev.next = new Inst2.Smsw_mem(rm);
                        break;
                    case 0x06:										/* LMSW */
                        prev.next = new Inst2.Lmsw_mem(rm);
                        break;
                    case 0x07:										/* INVLPG */
                        prev.next = new Inst2.Invlpg();
                        break;
                    }
                } else {
                    switch (which) {
                    case 0x02:										/* LGDT */
                        prev.next = new Inst2.Lgdt_reg();
                        break;
                    case 0x03:										/* LIDT */
                        prev.next = new Inst2.Lidt_reg();
                        break;
                    case 0x04:										/* SMSW */
                        prev.next = new Inst2.Smsw_reg(rm);
                        break;
                    case 0x06:										/* LMSW */
                        prev.next = new Inst2.Lmsw_reg(rm);
                        break;
                    default:
                        prev.next = new Inst1.Illegal("");
                        return RESULT_JUMP;
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* LAR Gw,Ew */
        ops[0x102] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.LarGwEw_reg(rm);
                } else {
                    prev.next = new Inst2.LarGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* LSL Gw,Ew */
        ops[0x103] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.LslGwEw_reg(rm);
                } else {
                    prev.next = new Inst2.LslGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CLTS */
        ops[0x106] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.Clts();
                return RESULT_HANDLED;
            }
        };
        ops[0x306] = ops[0x106];

        /* INVD */
        ops[0x108] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Invd();
                    return RESULT_HANDLED;
                }
            }
        };
        ops[0x308] = ops[0x108];

        /* WBINVD */
        ops[0x109] = ops[0x108];
        ops[0x309] = ops[0x108];

        /*
            0F0D NOP
            Intel 64 and IA-32 Architecture Software Developer's Manual Volume 2B: Instruction Set Reference, N-Z, Two-byte Opcode Map
            AMD architecture maps 3DNow! PREFETCH instructions here
        */
        ops[0x10D] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Noop();
                return RESULT_HANDLED;
            }
        };

        /* MOV Rd.CRx */
        ops[0x120] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm < 0xc0 ) {
                    int which=(rm >> 3) & 7;
                    rm |= 0xc0;
                    if (Log.level<= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,"MOV XXX,CR"+which+" with non-register");
                }
                prev.next = new Inst2.MovRdCr(rm);
                return RESULT_HANDLED;
            }
        };
        ops[0x320] = ops[0x120];

        /* MOV Rd,DRx */
        ops[0x121] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm < 0xc0 ) {
                    int which=(rm >> 3) & 7;
                    rm |= 0xc0;
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,DR"+which+" with non-register");
                }
                prev.next = new Inst2.MovRdDr(rm);
                return RESULT_HANDLED;
            }
        };
        ops[0x321] = ops[0x121];

        /* MOV CRx,Rd */
        ops[0x122] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm < 0xc0 ) {
                    int which=(rm >> 3) & 7;
                    rm |= 0xc0;
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,CR"+which+" with non-register");
                }
                prev.next = new Inst2.MovCrRd(rm);
                return RESULT_HANDLED;
            }
        };
        ops[0x322] = ops[0x122];

        /* MOV DRx,Rd */
        ops[0x123] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm < 0xc0 ) {
                    int which=(rm >> 3) & 7;
                    rm |= 0xc0;
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV DR"+which+",XXX with non-register");
                }
                prev.next = new Inst2.MovDrRd(rm);
                return RESULT_HANDLED;
            }
        };
        ops[0x323] = ops[0x123];

        /* MOV Rd,TRx */
        ops[0x124] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm < 0xc0 ) {
                    int which=(rm >> 3) & 7;
                    rm |= 0xc0;
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV XXX,TR"+which+" with non-register");
                }
                prev.next = new Inst2.MovRdTr(rm);
                return RESULT_HANDLED;
            }
        };
        ops[0x324] = ops[0x124];

        /* MOV TRx,Rd */
        ops[0x126] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm < 0xc0 ) {
                    int which=(rm >> 3) & 7;
                    rm |= 0xc0;
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"MOV TR"+which+",XXX with non-register");
                }
                prev.next = new Inst2.MovTrRd(rm);
                return RESULT_HANDLED;
            }
        };
        ops[0x326] = ops[0x126];

        /* RDTSC */
        ops[0x131] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Rdtsc();
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x331] = ops[0x131];

        /* CMOVO */
        ops[0x140] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_o_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_o_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNO */
        ops[0x141] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_no_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_no_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVB */
        ops[0x142] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_b_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_b_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNB */
        ops[0x143] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_nb_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_nb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVZ */
        ops[0x144] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_z_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_z_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNZ */
        ops[0x145] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_nz_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_nz_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVBE */
        ops[0x146] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_be_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_be_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNBE */
        ops[0x147] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_nbe_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_nbe_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVS */
        ops[0x148] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_s_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_s_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNS */
        ops[0x149] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_ns_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_ns_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVP */
        ops[0x14a] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_p_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_p_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNP */
        ops[0x14b] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_np_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_np_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVL */
        ops[0x14c] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_l_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_l_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNL */
        ops[0x14d] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_nl_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_nl_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVLE */
        ops[0x14e] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_le_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_le_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* CMOVNLE */
        ops[0x14f] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }

                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst2.ConditionalMov_nle_reg(rm);
                } else {
                    prev.next = new Inst2.ConditionalMov_nle_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* JO */
        ops[0x180] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_o();
                return RESULT_JUMP;
            }
        };

        /* JNO */
        ops[0x181] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_no();
                return RESULT_JUMP;
            }
        };

        /* JB */
        ops[0x182] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_b();
                return RESULT_JUMP;
            }
        };

        /* JNB */
        ops[0x183] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_nb();
                return RESULT_JUMP;
            }
        };

        /* JZ */
        ops[0x184] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_z();
                return RESULT_JUMP;
            }
        };

        /* JNZ */
        ops[0x185] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_nz();
                return RESULT_JUMP;
            }
        };

        /* JBE */
        ops[0x186] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_be();
                return RESULT_JUMP;
            }
        };

        /* JNBE */
        ops[0x187] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_nbe();
                return RESULT_JUMP;
            }
        };

        /* JS */
        ops[0x188] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_s();
                return RESULT_JUMP;
            }
        };

        /* JNS */
        ops[0x189] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_ns();
                return RESULT_JUMP;
            }
        };

        /* JP */
        ops[0x18a] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_p();
                return RESULT_JUMP;
            }
        };

        /* JNP */
        ops[0x18b] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_np();
                return RESULT_JUMP;
            }
        };

        /* JL */
        ops[0x18c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_l();
                return RESULT_JUMP;
            }
        };

        /* JNL */
        ops[0x18d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_nl();
                return RESULT_JUMP;
            }
        };

        /* JLE */
        ops[0x18e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_le();
                return RESULT_JUMP;
            }
        };

        /* JNLE */
        ops[0x18f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.JumpCond16_w_nle();
                return RESULT_JUMP;
            }
        };

        /* SETO */
        ops[0x190] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_o(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_o(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x390] = ops[0x190];

        /* SETNO */
        ops[0x191] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_no(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_no(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x391] = ops[0x191];

        /* SETB */
        ops[0x192] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_b(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_b(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x392] = ops[0x192];

        /* SETNB */
        ops[0x193] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_nb(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_nb(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x393] = ops[0x193];

        /* SETZ */
        ops[0x194] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_z(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_z(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x394] = ops[0x194];

        /* SETNZ */
        ops[0x195] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_nz(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_nz(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x395] = ops[0x195];

        /* SETBE */
        ops[0x196] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_be(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_be(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x396] = ops[0x196];

        /* SETNBE */
        ops[0x197] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_nbe(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_nbe(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x397] = ops[0x197];

        /* SETS */
        ops[0x198] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_s(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_s(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x398] = ops[0x198];

        /* SETNS */
        ops[0x199] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_ns(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_ns(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x399] = ops[0x199];

        /* SETP */
        ops[0x19a] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_p(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_p(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x39a] = ops[0x19a];

        /* SETNP */
        ops[0x19b] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_np(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_np(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x39b] = ops[0x19b];

        /* SETL */
        ops[0x19c] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_l(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_l(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x39c] = ops[0x19c];

        /* SETNL */
        ops[0x19d] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_nl(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_nl(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x39d] = ops[0x19d];

        /* SETLE */
        ops[0x19e] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_le(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_le(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x39e] = ops[0x19e];

        /* SETNLE */
        ops[0x19f] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.SETcc_reg_nle(rm);
                } else {
                    prev.next = new Inst2.SETcc_mem_nle(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x39f] = ops[0x19f];

        /* PUSH FS */
        ops[0x1a0] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.PushFS();
                return RESULT_HANDLED;
            }
        };

        /* POP FS */
        ops[0x1a1] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.PopFS();
                return RESULT_HANDLED;
            }
        };

        /* CPUID */
        ops[0x1a2] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486NEW) {
                    prev.next = new Inst1.Illegal("CPUID");
                    return RESULT_JUMP;
                }
                prev.next = new Inst2.CPUID();
                return RESULT_HANDLED;
            }
        };
        ops[0x3a2] = ops[0x1a2];

        /* BT Ew,Gw */
        ops[0x1a3] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.BtEwGw_reg(rm);
                } else {
                    prev.next = new Inst2.BtEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* SHLD Ew,Gw,Ib */
        ops[0x1a4] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    Reg earw = Mod.ew(rm);
                    int op3 = decode_fetchb();
                    if (Instructions.valid_DSHLW(op3))
                        prev.next = new Inst2.ShldEwGwIb_reg(rm, earw, op3);
                } else {
                    EaaBase get_eaa = Mod.getEaa(rm);
                    int op3 = decode_fetchb();
                    if (Instructions.valid_DSHLW(op3))
                        prev.next = new Inst2.ShldEwGwIb_mem(rm, get_eaa, op3);
                }
                return RESULT_HANDLED;
            }
        };

        /* SHLD Ew,Gw,CL */
        ops[0x1a5] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.ShldEwGwCl_reg(rm);
                } else {
                    prev.next = new Inst2.ShldEwGwCl_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* PUSH GS */
        ops[0x1a8] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.PushGS();
                return RESULT_HANDLED;
            }
        };

        /* POP GS */
        ops[0x1a9] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst2.PopGS();
                return RESULT_HANDLED;
            }
        };

        /* BTS Ew,Gw */
        ops[0x1ab] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.BtsEwGw_reg(rm);
                } else {
                    prev.next = new Inst2.BtsEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* SHRD Ew,Gw,Ib */
        ops[0x1ac] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    Reg earw = Mod.ew(rm);
                    int op3 = decode_fetchb();
                    if (Instructions.valid_DSHRW(op3))
                        prev.next = new Inst2.ShrdEwGwIb_reg(rm, earw, op3);
                } else {
                    EaaBase get_eaa = Mod.getEaa(rm);
                    int op3 = decode_fetchb();
                    if (Instructions.valid_DSHRW(op3))
                        prev.next = new Inst2.ShrdEwGwIb_mem(rm, get_eaa, op3);
                }
                return RESULT_HANDLED;
            }
        };

        /* SHRD Ew,Gw,CL */
        ops[0x1ad] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.ShrdEwGwCl_reg(rm);
                } else {
                    prev.next = new Inst2.ShrdEwGwCl_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* IMUL Gw,Ew */
        ops[0x1af] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.ImulGwEw_reg(rm);
                } else {
                    prev.next = new Inst2.ImulGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* cmpxchg Eb,Gb */
        ops[0x1b0] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    int rm=decode_fetchb();
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst2.CmpxchgEbGb_reg(rm);
                    } else {
                        prev.next = new Inst2.CmpxchgEbGb_mem(rm);
                    }
                    return RESULT_HANDLED;
                }
            }
        };
        ops[0x3b0] = ops[0x1b0];

        /* cmpxchg Ew,Gw */
        ops[0x1b1] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    int rm=decode_fetchb();
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst2.CmpxchgEwGw_reg(rm);
                    } else {
                        prev.next = new Inst2.CmpxchgEwGw_mem(rm);
                    }
                    return RESULT_HANDLED;
                }
            }
        };

        /* LSS Ew */
        ops[0x1b2] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst2.LssEw(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* BTR Ew,Gw */
        ops[0x1b3] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.BtrEwGw_reg(rm);
                } else {
                    prev.next = new Inst2.BtrEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* LFS Ew */
        ops[0x1b4] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst2.LfsEw(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* LGS Ew */
        ops[0x1b5] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst2.LgsEw(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOVZX Gw,Eb */
        ops[0x1b6] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.MovzxGwEb_reg(rm);
                } else {
                    prev.next = new Inst2.MovzxGwEb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOVZX Gw,Ew */
        ops[0x1b7] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.MovzxGwEw_reg(rm);
                } else {
                    prev.next = new Inst2.MovzxGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOVSX Gw,Ew */
        ops[0x1bf] = ops[0x1b7];

        /* GRP8 Ew,Ib */
        ops[0x1ba] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    switch (rm & 0x38) {
                    case 0x20:										/* BT */
                        prev.next = new Inst2.BtEwIb_reg(rm);
                        break;
                    case 0x28:										/* BTS */
                        prev.next = new Inst2.BtsEwIb_reg(rm);
                        break;
                    case 0x30:										/* BTR */
                        prev.next = new Inst2.BtrEwIb_reg(rm);
                        break;
                    case 0x38:										/* BTC */
                        prev.next = new Inst2.BtcEwIb_reg(rm);
                        break;
                    default:
                        Log.exit("CPU:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
                    }
                } else {
                    switch (rm & 0x38) {
                    case 0x20:										/* BT */
                        prev.next = new Inst2.BtEwIb_mem(rm);
                        break;
                    case 0x28:										/* BTS */
                        prev.next = new Inst2.BtsEwIb_mem(rm);
                        break;
                    case 0x30:										/* BTR */
                        prev.next = new Inst2.BtrEwIb_mem(rm);
                        break;
                    case 0x38:										/* BTC */
                        prev.next = new Inst2.BtcEwIb_mem(rm);
                        break;
                    default:
                        Log.exit("CPU:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* BTC Ew,Gw */
        ops[0x1bb] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.BtcEwGw_reg(rm);
                } else {
                    prev.next = new Inst2.BtcEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* BSF Gw,Ew */
        ops[0x1bc] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.BsfGwEw_reg(rm);
                } else {
                    prev.next = new Inst2.BsfGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* BSR Gw,Ew */
        ops[0x1bd] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.BsrGwEw_reg(rm);
                } else {
                    prev.next = new Inst2.BsrGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOVSX Gw,Eb */
        ops[0x1be] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst2.MovsxGwEb_reg(rm);
                } else {
                    prev.next = new Inst2.MovsxGwEb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* XADD Gb,Eb */
        ops[0x1c0] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    int rm=decode_fetchb();
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst2.XaddGbEb_reg(rm);
                    } else {
                        prev.next = new Inst2.XaddGbEb_mem(rm);
                    }
                    return RESULT_HANDLED;
                }
            }
        };
        ops[0x3c0] = ops[0x1c0];

        /* XADD Gw,Ew */
        ops[0x1c1] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    int rm=decode_fetchb();
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst2.XaddGwEw_reg(rm);
                    } else {
                        prev.next = new Inst2.XaddGwEw_mem(rm);
                    }
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP AX */
        ops[0x1c8] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_eax);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP CX */
        ops[0x1c9] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_ecx);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP DX */
        ops[0x1ca] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_edx);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP BX */
        ops[0x1cb] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_ebx);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP SP */
        ops[0x1cc] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_esp);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP BP */
        ops[0x1cd] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_ebp);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP SI */
        ops[0x1ce] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_esi);
                    return RESULT_HANDLED;
                }
            }
        };

        /* BSWAP DI */
        ops[0x1cf] = new Decode() {
            final public int call(Op prev) {
                if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                } else {
                    prev.next = new Inst2.Bswapw(reg_edi);
                    return RESULT_HANDLED;
                }
            }
        };
    }
}
