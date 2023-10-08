package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.misc.Log;

public class Prefix_66_0f extends Helper {
    static public void init(Decode[] ops) {
        // :TODO: double check that 0x300 is a copy of 0x100
        ops[0x300] = ops[0x100];
        /* Group 7 Ed */
        ops[0x301] = prev -> {
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
                    prev.next = new Inst4.Lgdt_mem(rm);
                    break;
                case 0x03:										/* LIDT */
                    prev.next = new Inst4.Lidt_mem(rm);
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
                    prev.next = new Inst4.Smsw_reg(rm);
                    break;
                case 0x06:										/* LMSW */
                    prev.next = new Inst4.Lmsw_reg(rm);
                    break;
                default:
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }
            }
            return RESULT_HANDLED;
        };

        /* LAR Gd,Ed */
        ops[0x302] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.LarGdEd_reg(rm);
            } else {
                prev.next = new Inst4.LarGdEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* LSL Gd,Ew */
        ops[0x303] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.LslGdEd_reg(rm);
            } else {
                prev.next = new Inst4.LslGdEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* WRMSR */
        ops[0x330] = prev -> {
            prev.next = new Inst4.WriteMSR();
            return RESULT_HANDLED;
        };

        /* RDMSR */
        ops[0x332] = prev -> {
            prev.next = new Inst4.ReadMSR();
            return RESULT_HANDLED;
        };

        /* CMOVO */
        ops[0x340] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_o_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_o_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNO */
        ops[0x341] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_no_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_no_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVB */
        ops[0x342] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_b_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_b_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNB */
        ops[0x343] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_nb_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_nb_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVZ */
        ops[0x344] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_z_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_z_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNZ */
        ops[0x345] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_nz_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_nz_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVBE */
        ops[0x346] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_be_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_be_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNBE */
        ops[0x347] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_nbe_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_nbe_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVS */
        ops[0x348] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_s_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_s_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNS */
        ops[0x349] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_ns_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_ns_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVP */
        ops[0x34a] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_p_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_p_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNP */
        ops[0x34b] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_np_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_np_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVL */
        ops[0x34c] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_l_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_l_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNL */
        ops[0x34d] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_nl_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_nl_mem(rm);
            }
            return RESULT_HANDLED;
        };
        
        /* CMOVLE */
        ops[0x34e] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_le_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_le_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMOVNLE */
        ops[0x34f] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM_PRO) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }

            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst4.ConditionalMov_nle_reg(rm);
            } else {
                prev.next = new Inst4.ConditionalMov_nle_mem(rm);
            }
            return RESULT_HANDLED;
        };
        
        /* JO */
        ops[0x380] = prev -> {
            prev.next = new Inst4.JumpCond32_d_o();
            return RESULT_JUMP;
        };

        /* JNO */
        ops[0x381] = prev -> {
            prev.next = new Inst4.JumpCond32_d_no();
            return RESULT_JUMP;
        };

        /* JB */
        ops[0x382] = prev -> {
            prev.next = new Inst4.JumpCond32_d_b();
            return RESULT_JUMP;
        };

        /* JNB */
        ops[0x383] = prev -> {
            prev.next = new Inst4.JumpCond32_d_nb();
            return RESULT_JUMP;
        };

        /* JZ */
        ops[0x384] = prev -> {
            prev.next = new Inst4.JumpCond32_d_z();
            return RESULT_JUMP;
        };

        /* JNZ */
        ops[0x385] = prev -> {
            prev.next = new Inst4.JumpCond32_d_nz();
            return RESULT_JUMP;
        };

        /* JBE */
        ops[0x386] = prev -> {
            prev.next = new Inst4.JumpCond32_d_be();
            return RESULT_JUMP;
        };

        /* JNBE */
        ops[0x387] = prev -> {
            prev.next = new Inst4.JumpCond32_d_nbe();
            return RESULT_JUMP;
        };

        /* JS */
        ops[0x388] = prev -> {
            prev.next = new Inst4.JumpCond32_d_s();
            return RESULT_JUMP;
        };

        /* JNS */
        ops[0x389] = prev -> {
            prev.next = new Inst4.JumpCond32_d_ns();
            return RESULT_JUMP;
        };

        /* JP */
        ops[0x38a] = prev -> {
            prev.next = new Inst4.JumpCond32_d_p();
            return RESULT_JUMP;
        };

        /* JNP */
        ops[0x38b] = prev -> {
            prev.next = new Inst4.JumpCond32_d_np();
            return RESULT_JUMP;
        };

        /* JL */
        ops[0x38c] = prev -> {
            prev.next = new Inst4.JumpCond32_d_l();
            return RESULT_JUMP;
        };

        /* JNL */
        ops[0x38d] = prev -> {
            prev.next = new Inst4.JumpCond32_d_nl();
            return RESULT_JUMP;
        };

        /* JLE */
        ops[0x38e] = prev -> {
            prev.next = new Inst4.JumpCond32_d_le();
            return RESULT_JUMP;
        };

        /* JNLE */
        ops[0x38f] = prev -> {
            prev.next = new Inst4.JumpCond32_d_nle();
            return RESULT_JUMP;
        };

        /* PUSH FS */
        ops[0x3a0] = prev -> {
            prev.next = new Inst4.PushFS();
            return RESULT_HANDLED;
        };

        /* POP FS */
        ops[0x3a1] = prev -> {
            prev.next = new Inst4.PopFS();
            return RESULT_HANDLED;
        };

        /* BT Ed,Gd */
        ops[0x3a3] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.BtEdGd_reg(rm);
            } else {
                prev.next = new Inst4.BtEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* SHLD Ed,Gd,Ib */
        ops[0x3a4] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                Inst4.ShldEdGdIb_reg tmp = new Inst4.ShldEdGdIb_reg(rm);
                if (tmp.op3==0)
                    prev.next = new Inst1.Noop();
                else
                    prev.next = tmp;
            } else {
                Inst4.ShldEdGdIb_mem tmp = new Inst4.ShldEdGdIb_mem(rm);
                if (tmp.op3==0)
                    prev.next = new Inst1.Noop();
                else
                    prev.next = tmp;
            }
            return RESULT_HANDLED;
        };

        /* SHLD Ed,Gd,CL */
        ops[0x3a5] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.ShldEdGdCl_reg(rm);
            } else {
                prev.next = new Inst4.ShldEdGdCl_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* PUSH GS */
        ops[0x3a8] = prev -> {
            prev.next = new Inst4.PushGS();
            return RESULT_HANDLED;
        };

        /* POP GS */
        ops[0x3a9] = prev -> {
            prev.next = new Inst4.PopGS();
            return RESULT_HANDLED;
        };

        /* BTS Ed,Gd */
        ops[0x3ab] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.BtsEdGd_reg(rm);
            } else {
                prev.next = new Inst4.BtsEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* SHRD Ed,Gd,Ib */
        ops[0x3ac] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                Inst4.ShrdEdGdIb_reg tmp = new Inst4.ShrdEdGdIb_reg(rm);
                if (tmp.op3 == 0)
                    prev.next = new Inst1.Noop();
                else
                    prev.next = tmp;
            } else {
                Inst4.ShrdEdGdIb_mem tmp = new Inst4.ShrdEdGdIb_mem(rm);
                if (tmp.op3 == 0)
                    prev.next = new Inst1.Noop();
                else
                    prev.next = tmp;
            }
            return RESULT_HANDLED;
        };

        /* SHRD Ed,Gd,CL */
        ops[0x3ad] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.ShrdEdGdCl_reg(rm);
            } else {
                prev.next = new Inst4.ShrdEdGdCl_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* IMUL Gd,Ed */
        ops[0x3af] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.ImulGdEd_reg(rm);
            } else {
                prev.next = new Inst4.ImulGdEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* CMPXCHG Ed,Gd */
        ops[0x3b1] = prev -> {
             if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst4.CmpxchgEdGd_reg(rm);
                } else {
                    prev.next = new Inst4.CmpxchgEdGd_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* LSS Ed */
        ops[0x3b2] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst1.Illegal("");
            } else {
                prev.next = new Inst4.LssEd(rm);
            }
            return RESULT_HANDLED;
        };

        /* BTR Ed,Gd */
        ops[0x3b3] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.BtrEdGd_reg(rm);
            } else {
                prev.next = new Inst4.BtrEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* LFS Ed */
        ops[0x3b4] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst1.Illegal("");
            } else {
                prev.next = new Inst4.LfsEd(rm);
            }
            return RESULT_HANDLED;
        };

        /* LGS Ed */
        ops[0x3b5] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst1.Illegal("");
            } else {
                prev.next = new Inst4.LgsEd(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOVZX Gd,Eb */
        ops[0x3b6] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.MovzxGdEb_reg(rm);
            } else {
                prev.next = new Inst4.MovzxGdEb_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOVXZ Gd,Ew */
        ops[0x3b7] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.MovzxGdEw_reg(rm);
            } else {
                prev.next = new Inst4.MovzxGdEw_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* GRP8 Ed,Ib */
        ops[0x3ba] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                switch (rm & 0x38) {
                case 0x20:										/* BT */
                    prev.next = new Inst4.BtEdIb_reg(rm);
                    break;
                case 0x28:										/* BTS */
                    prev.next = new Inst4.BtsEdIb_reg(rm);
                    break;
                case 0x30:										/* BTR */
                    prev.next = new Inst4.BtrEdIb_reg(rm);
                    break;
                case 0x38:										/* BTC */
                    prev.next = new Inst4.BtcEdIb_reg(rm);
                    break;
                default:
                    Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
                }
            } else {
                switch (rm & 0x38) {
                case 0x20:										/* BT */
                    prev.next = new Inst4.BtEdIb_mem(rm);
                    break;
                case 0x28:										/* BTS */
                    prev.next = new Inst4.BtsEdIb_mem(rm);
                    break;
                case 0x30:										/* BTR */
                    prev.next = new Inst4.BtrEdIb_mem(rm);
                    break;
                case 0x38:										/* BTC */
                    prev.next = new Inst4.BtcEdIb_mem(rm);
                    break;
                default:
                    Log.exit("CPU:66:0F:BA:Illegal subfunction "+Integer.toString(rm & 0x38,16));
                }
            }
            return RESULT_HANDLED;
        };

        /* BTC Ed,Gd */
        ops[0x3bb] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.BtcEdGd_reg(rm);
            } else {
                prev.next = new Inst4.BtcEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* BSF Gd,Ed */
        ops[0x3bc] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.BsfGdEd_reg(rm);
            } else {
                prev.next = new Inst4.BsfGdEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* BSR Gd,Ed */
        ops[0x3bd] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.BsrGdEd_reg(rm);
            } else {
                prev.next = new Inst4.BsrGdEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOVSX Gd,Eb */
        ops[0x3be] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.MovsxGdEb_reg(rm);
            } else {
                prev.next = new Inst4.MovsxGdEb_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOVSX Gd,Ew */
        ops[0x3bf] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst4.MovsxGdEw_reg(rm);
            } else {
                prev.next = new Inst4.MovsxGdEw_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* XADD Gd,Ed */
        ops[0x3c1] = prev -> {
            if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst4.XaddGdEd_reg(rm);
                } else {
                    prev.next = new Inst4.XaddGdEd_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        ops[0x3c7] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_PENTIUM) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                int rm=decode_fetchb();
                if ((rm & 0x38) == 8) {
                    prev.next = new Inst4.CompareExchange8B(rm);
                } else {
                    prev.next = new Inst1.Illegal("");
                    return RESULT_JUMP;
                }
                return RESULT_HANDLED;
            }
        };

        /* BSWAP EAX */
        ops[0x3c8] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_eax);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP ECX */
        ops[0x3c9] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP EDX */
        ops[0x3ca] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP EBX */
        ops[0x3cb] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP ESP */
        ops[0x3cc] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP EBP */
        ops[0x3cd] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP ESI */
        ops[0x3ce] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* BSWAP EDI */
        ops[0x3cf] = prev -> {
            if (CPU.CPU_ArchitectureType< CPU.CPU_ARCHTYPE_486OLD) {
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            } else {
                prev.next = new Inst4.Bswapd(reg_edi);
                return RESULT_HANDLED;
            }
        };
    }
}
