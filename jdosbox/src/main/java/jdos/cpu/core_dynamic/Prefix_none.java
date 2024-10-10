package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.Core;
import jdos.cpu.StringOp;
import jdos.fpu.FPU;

public class Prefix_none extends Helper {
    static public void init(Decode[] ops) {
        /* ADD Eb,Gb */
        ops[0x00] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Addb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.AddEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };

        ops[0x200] = ops[0x00];

        /* ADD Ew,Gw */
        ops[0x01] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Addw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.AddEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* ADD Gb,Eb */
        ops[0x02] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Addb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.AddGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x202] = ops[0x02];

        /* ADD Gw,Ew */
        ops[0x03] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Addw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.AddGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* ADD AL,Ib */
        ops[0x04] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AddAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x204] = ops[0x04];

        /* ADD AX,Iw */
        ops[0x05] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AddAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* PUSH ES */
        ops[0x06] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PushES();
                return RESULT_HANDLED;
            }
        };

        /* POP ES */
        ops[0x07] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PopES();
                return RESULT_HANDLED;
            }
        };

        /* OR Eb,Gb */
        ops[0x08] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Orb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.OrEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };

        ops[0x208] = ops[0x08];

        /* OR Ew,Gw */
        ops[0x09] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Orw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.OrEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* OR Gb,Eb */
        ops[0x0a] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Orb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.OrGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x20a] = ops[0x0a];

        /* OR Gw,Ew */
        ops[0x0b] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Orw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.OrGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* OR AL,Ib */
        ops[0x0c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.OrAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x20c] = ops[0x0c];

        /* OR AX,Iw */
        ops[0x0d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.OrAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* PUSH CS */
        ops[0x0e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PushCS();
                return RESULT_HANDLED;
            }
        };

        /* 2 byte opcodes*/
        ops[0x0f] = new Decode() {
            final public int call(Op prev) {
                opcode_index|=OPCODE_0F;
                return RESULT_CONTINUE;
            }
        };
        ops[0x20f] = ops[0x0f];

        /* ADC Eb,Gb */
        ops[0x10] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Adcb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.AdcEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };

        ops[0x210] = ops[0x10];

        /* ADC Ew,Gw */
        ops[0x11] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Adcw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.AdcEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* ADC Gb,Eb */
        ops[0x12] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Adcb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.AdcGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x212] = ops[0x12];

        /* ADC Gw,Ew */
        ops[0x13] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Adcw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.AdcGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* ADC AL,Ib */
        ops[0x14] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AdcAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x214] = ops[0x14];

        /* ADC AX,Iw */
        ops[0x15] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AdcAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* PUSH SS */
        ops[0x16] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PushSS();
                return RESULT_HANDLED;
            }
        };

        /* POP SS */
        ops[0x17] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PopSS();
                return RESULT_ANOTHER;
            }
        };

        /* SBB Eb,Gb */
        ops[0x18] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Sbbb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.SbbEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x218] = ops[0x18];

        /* SBB Ew,Gw */
        ops[0x19] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Sbbw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.SbbEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* SBB Gb,Eb */
        ops[0x1a] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Sbbb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.SbbGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x21a] = ops[0x1a];

        /* SBB Gw,Ew */
        ops[0x1b] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Sbbw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.SbbGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* SBB AL,Ib */
        ops[0x1c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SbbAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x21c] = ops[0x1c];

        /* SBB AX,Iw */
        ops[0x1d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SbbAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* PUSH DS */
        ops[0x1e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PushDS();
                return RESULT_HANDLED;
            }
        };

        /* POP DS */
        ops[0x1f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PopDS();
                return RESULT_HANDLED;
            }
        };

        /* AND Eb,Gb */
        ops[0x20] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Andb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.AndEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x220] = ops[0x20];

        /* AND Ew,Gw */
        ops[0x21] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Andw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.AndEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* AND Gb,Eb */
        ops[0x22] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Andb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.AndGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x222] = ops[0x22];

        /* AND Gw,Ew */
        ops[0x23] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Andw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.AndGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* AND AL,Ib */
        ops[0x24] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AndAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x224] = ops[0x24];

        /* AND AX,Iw */
        ops[0x25] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AndAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* SEG ES: */
        ops[0x26] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SegES();
                return RESULT_CONTINUE_SEG;
            }
        };
        ops[0x226] = ops[0x26];

        /* DAA */
        ops[0x27] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Daa();
                return RESULT_HANDLED;
            }
        };
        ops[0x227] = ops[0x27];

        /* SUB Eb,Gb */
        ops[0x28] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Subb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.SubEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x228] = ops[0x28];

        /* SUB Ew,Gw */
        ops[0x29] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Subw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.SubEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* SUB Gb,Eb */
        ops[0x2a] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Subb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.SubGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x22a] = ops[0x2a];

        /* SUB Gw,Ew */
        ops[0x2b] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Subw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.SubGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* SUB AL,Ib */
        ops[0x2c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SubAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x22c] = ops[0x2c];

        /* SUB AX,Iw */
        ops[0x2d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SubAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* SEG CS: */
        ops[0x2e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SegCS();
                return RESULT_CONTINUE_SEG;
            }
        };
        ops[0x22e] = ops[0x2e];

        /* DAS */
        ops[0x2f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Das();
                return RESULT_HANDLED;
            }
        };
        ops[0x22f] = ops[0x2f];

        /* XOR Eb,Gb */
        ops[0x30] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Xorb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.XorEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };

        ops[0x230] = ops[0x30];

        /* XOR Ew,Gw */
        ops[0x31] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Xorw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.XorEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* XOR Gb,Eb */
        ops[0x32] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Xorb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.XorGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x232] = ops[0x32];

        /* XOR Gw,Ew */
        ops[0x33] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Xorw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.XorGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* XOR AL,Ib */
        ops[0x34] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XorAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x234] = ops[0x34];

        /* XOR AX,Iw */
        ops[0x35] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XorAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* SEG SS: */
        ops[0x36] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SegSS();
                return RESULT_CONTINUE_SEG;
            }
        };
        ops[0x236] = ops[0x36];

        /* AAA */
        ops[0x37] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Aaa();
                return RESULT_HANDLED;
            }
        };
        ops[0x237] = ops[0x37];

        /* CMP Eb,Gb */
        ops[0x38] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Cmpb_reg(Mod.eb(rm), Mod.gb(rm));
                } else {
                    prev.next = new Inst1.CmpEbGb_mem(Mod.getEaa(rm), Mod.gb(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x238] = ops[0x38];

        /* CMP Ew,Gw */
        ops[0x39] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Cmpw_reg(Mod.ew(rm), Mod.gw(rm));
                } else {
                    prev.next = new Inst1.CmpEwGw_mem(Mod.getEaa(rm), Mod.gw(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* CMP Gb,Eb */
        ops[0x3a] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                 if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Cmpb_reg(Mod.gb(rm), Mod.eb(rm));
                } else {
                    prev.next = new Inst1.CmpGbEb_mem(Mod.gb(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x23a] = ops[0x3a];

        /* CMP Gw,Ew */
        ops[0x3b] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Cmpw_reg(Mod.gw(rm), Mod.ew(rm));
                } else {
                    prev.next = new Inst1.CmpGwEw_mem(Mod.gw(rm), Mod.getEaa(rm));
                }
                return RESULT_HANDLED;
            }
        };

        /* CMP AL,Ib */
        ops[0x3c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.CmpAlIb(decode_fetchb());
                return RESULT_HANDLED;
            }
        };
        ops[0x23c] = ops[0x3c];

        /* CMP AX,Iw */
        ops[0x3d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.CmpAxIw(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* SEG DS: */
        ops[0x3e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SegDS();
                return RESULT_CONTINUE_SEG;
            }
        };
        ops[0x23e] = ops[0x3e];

        /* AAS */
        ops[0x3f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Aas();
                return RESULT_HANDLED;
            }
        };
        ops[0x23f] = ops[0x3f];

        /* INC AX */
        ops[0x40] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_eax);
                return RESULT_HANDLED;
            }
        };

        /* INC CX */
        ops[0x41] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* INC DX */
        ops[0x42] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* INC BX */
        ops[0x43] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* INC SP */
        ops[0x44] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* INC BP */
        ops[0x45] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* INC SI */
        ops[0x46] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* INC DI */
        ops[0x47] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Incw(reg_edi);
                return RESULT_HANDLED;
            }
        };

        /* DEC AX */
        ops[0x48] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_eax);
                return RESULT_HANDLED;
            }
        };

        /* DEC CX */
        ops[0x49] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* DEC DX */
        ops[0x4a] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* DEC BX */
        ops[0x4b] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* DEC SP */
        ops[0x4c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* DEC BP */
        ops[0x4d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* DEC SI */
        ops[0x4e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* DEC DI */
        ops[0x4f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Decw(reg_edi);
                return RESULT_HANDLED;
            }
        };

        /* PUSH AX */
        ops[0x50] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_eax);
                return RESULT_HANDLED;
            }
        };

        /* PUSH CX */
        ops[0x51] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* PUSH DX */
        ops[0x52] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* PUSH BX */
        ops[0x53] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* PUSH SP */
        ops[0x54] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* PUSH BP */
        ops[0x55] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* PUSH SI */
        ops[0x56] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* PUSH DI */
        ops[0x57] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pushw(reg_edi);
                return RESULT_HANDLED;
            }
        };

        /* POP AX */
        ops[0x58] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_eax);
                return RESULT_HANDLED;
            }
        };

        /* POP CX */
        ops[0x59] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* POP DX */
        ops[0x5a] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* POP BX */
        ops[0x5b] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* POP SP */
        ops[0x5c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* POP BP */
        ops[0x5d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* POP SI */
        ops[0x5e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* POP DI */
        ops[0x5f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popw(reg_edi);
                return RESULT_HANDLED;
            }
        };

        /* PUSHA */
        ops[0x60] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Pusha();
                return RESULT_HANDLED;
            }
        };

        /* POPA */
        ops[0x61] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Popa();
                return RESULT_HANDLED;
            }
        };

        /* BOUND */
        ops[0x62] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Bound();
                return RESULT_HANDLED;
            }
        };

        /* ARPL Ew,Rw */
        ops[0x63] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.ArplEwRw_reg(rm);
                } else {
                    prev.next = new Inst1.ArplEwRw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* SEG FS: */
        ops[0x64] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SegFS();
                return RESULT_CONTINUE_SEG;
            }
        };
        ops[0x264] = ops[0x64];

        /* SEG GS: */
        ops[0x65] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.SegGS();
                return RESULT_CONTINUE_SEG;
            }
        };
        ops[0x265] = ops[0x65];

        /* Operand Size Prefix */
        ops[0x66] = new Decode() {
            final public int call(Op prev) {
                opcode_index=(CPU.cpu.code.big?0:512);
                return RESULT_CONTINUE;
            }
        };
        ops[0x266] = ops[0x66];

        /* Address Size Prefix */
        ops[0x67] = new Decode() {
            final public int call(Op prev) {
                prefixes=(prefixes & ~Core.PREFIX_ADDR) |(CPU.cpu.code.big?0:1);
                EA16 = (prefixes&1)==0;
                return RESULT_CONTINUE;
            }
        };
        ops[0x267] = ops[0x67];

        /* PUSH Iw */
        ops[0x68] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Push16(decode_fetchw());
                return RESULT_HANDLED;
            }
        };

        /* IMUL Gw,Ew,Iw */
        ops[0x69] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.IMULGwEwIw_reg(rm);
                }
                else {
                    prev.next = new Inst1.IMULGwEwIw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* PUSH Ib */
        ops[0x6a] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Push16(decode_fetchbs());
                return RESULT_HANDLED;
            }
        };

        /* IMUL Gw,Ew,Ib */
        ops[0x6b] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.IMULGwEwIb_reg(rm);
                }
                else {
                    prev.next = new Inst1.IMULGwEwIb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* INSB */
        ops[0x6c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoStringException(StringOp.R_INSB, 1);
                return RESULT_HANDLED;
            }
        };
        ops[0x26c] = ops[0x6c];

        /* INSW */
        ops[0x6d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoStringException(StringOp.R_INSW, 2);
                return RESULT_HANDLED;
            }
        };

        /* OUTSB */
        ops[0x6e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoStringException(StringOp.R_OUTSB, 1);
                return RESULT_HANDLED;
            }
        };
        ops[0x26e] = ops[0x6e];

        /* OUTSW */
        ops[0x6f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoStringException(StringOp.R_OUTSW, 2);
                return RESULT_HANDLED;
            }
        };

        /* JO */
        ops[0x70] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_o();
                return RESULT_JUMP;
            }
        };

        /* JNO */
        ops[0x71] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_no();
                return RESULT_JUMP;
            }
        };

        /* JB */
        ops[0x72] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_b();
                return RESULT_JUMP;
            }
        };

        /* JNB */
        ops[0x73] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_nb();
                return RESULT_JUMP;
            }
        };

        /* JZ */
        ops[0x74] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_z();
                return RESULT_JUMP;
            }
        };

        /* JNZ */
        ops[0x75] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_nz();
                return RESULT_JUMP;
            }
        };

        /* JBE */
        ops[0x76] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_be();
                return RESULT_JUMP;
            }
        };

        /* JNBE */
        ops[0x77] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_nbe();
                return RESULT_JUMP;
            }
        };

        /* JS */
        ops[0x78] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_s();
                return RESULT_JUMP;
            }
        };

        /* JNS */
        ops[0x79] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_ns();
                return RESULT_JUMP;
            }
        };

        /* JP */
        ops[0x7a] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_p();
                return RESULT_JUMP;
            }
        };

        /* JNP */
        ops[0x7b] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_np();
                return RESULT_JUMP;
            }
        };

        /* JL */
        ops[0x7c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_l();
                return RESULT_JUMP;
            }
        };

        /* JNL */
        ops[0x7d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_nl();
                return RESULT_JUMP;
            }
        };

        /* JLE */
        ops[0x7e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_le();
                return RESULT_JUMP;
            }
        };

        /* JNLE */
        ops[0x7f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JumpCond16_b_nle();
                return RESULT_JUMP;
            }
        };

        /* Grpl Eb,Ib */
        ops[0x80] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm>= 0xc0) {
                    switch (which) {
                    case 0x00:prev.next = new Inst1.GrplEbIb_reg_add(rm);break;
                    case 0x01:prev.next = new Inst1.GrplEbIb_reg_or(rm);break;
                    case 0x02:prev.next = new Inst1.GrplEbIb_reg_adc(rm);break;
                    case 0x03:prev.next = new Inst1.GrplEbIb_reg_sbb(rm);break;
                    case 0x04:prev.next = new Inst1.GrplEbIb_reg_and(rm);break;
                    case 0x05:prev.next = new Inst1.GrplEbIb_reg_sub(rm);break;
                    case 0x06:prev.next = new Inst1.GrplEbIb_reg_xor(rm);break;
                    case 0x07:prev.next = new Inst1.GrplEbIb_reg_cmp(rm);break;
                    }
                } else {
                    switch (which) {
                    case 0x00:prev.next = new Inst1.GrplEbIb_mem_add(rm);break;
                    case 0x01:prev.next = new Inst1.GrplEbIb_mem_or(rm);break;
                    case 0x02:prev.next = new Inst1.GrplEbIb_mem_adc(rm);break;
                    case 0x03:prev.next = new Inst1.GrplEbIb_mem_sbb(rm);break;
                    case 0x04:prev.next = new Inst1.GrplEbIb_mem_and(rm);break;
                    case 0x05:prev.next = new Inst1.GrplEbIb_mem_sub(rm);break;
                    case 0x06:prev.next = new Inst1.GrplEbIb_mem_xor(rm);break;
                    case 0x07:prev.next = new Inst1.GrplEbIb_mem_cmp(rm);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x280] = ops[0x80];

        /* Grpl Eb,Ib Mirror instruction*/
        ops[0x82] = ops[0x80];
        ops[0x282] = ops[0x80];

        /* Grpl Ew,Iw */
        ops[0x81] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm>= 0xc0) {
                    switch (which) {
                    case 0x00:prev.next = new Inst1.GrplEwIw_reg_add(rm, false);break;
                    case 0x01:prev.next = new Inst1.GrplEwIw_reg_or(rm, false);break;
                    case 0x02:prev.next = new Inst1.GrplEwIw_reg_adc(rm, false);break;
                    case 0x03:prev.next = new Inst1.GrplEwIw_reg_sbb(rm, false);break;
                    case 0x04:prev.next = new Inst1.GrplEwIw_reg_and(rm, false);break;
                    case 0x05:prev.next = new Inst1.GrplEwIw_reg_sub(rm, false);break;
                    case 0x06:prev.next = new Inst1.GrplEwIw_reg_xor(rm, false);break;
                    case 0x07:prev.next = new Inst1.GrplEwIw_reg_cmp(rm, false);break;
                    }
                } else {
                    switch (which) {
                    case 0x00:prev.next = new Inst1.GrplEwIw_mem_add(rm, false);break;
                    case 0x01:prev.next = new Inst1.GrplEwIw_mem_or(rm, false);break;
                    case 0x02:prev.next = new Inst1.GrplEwIw_mem_adc(rm, false);break;
                    case 0x03:prev.next = new Inst1.GrplEwIw_mem_sbb(rm, false);break;
                    case 0x04:prev.next = new Inst1.GrplEwIw_mem_and(rm, false);break;
                    case 0x05:prev.next = new Inst1.GrplEwIw_mem_sub(rm, false);break;
                    case 0x06:prev.next = new Inst1.GrplEwIw_mem_xor(rm, false);break;
                    case 0x07:prev.next = new Inst1.GrplEwIw_mem_cmp(rm, false);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* Grpl Ew,Ix */
        ops[0x83] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm>= 0xc0) {
                    switch (which) {
                    case 0x00:prev.next = new Inst1.GrplEwIw_reg_add(rm, true);break;
                    case 0x01:prev.next = new Inst1.GrplEwIw_reg_or(rm, true);break;
                    case 0x02:prev.next = new Inst1.GrplEwIw_reg_adc(rm, true);break;
                    case 0x03:prev.next = new Inst1.GrplEwIw_reg_sbb(rm, true);break;
                    case 0x04:prev.next = new Inst1.GrplEwIw_reg_and(rm, true);break;
                    case 0x05:prev.next = new Inst1.GrplEwIw_reg_sub(rm, true);break;
                    case 0x06:prev.next = new Inst1.GrplEwIw_reg_xor(rm, true);break;
                    case 0x07:prev.next = new Inst1.GrplEwIw_reg_cmp(rm, true);break;
                    }
                } else {
                    switch (which) {
                    case 0x00:prev.next = new Inst1.GrplEwIw_mem_add(rm, true);break;
                    case 0x01:prev.next = new Inst1.GrplEwIw_mem_or(rm, true);break;
                    case 0x02:prev.next = new Inst1.GrplEwIw_mem_adc(rm, true);break;
                    case 0x03:prev.next = new Inst1.GrplEwIw_mem_sbb(rm, true);break;
                    case 0x04:prev.next = new Inst1.GrplEwIw_mem_and(rm, true);break;
                    case 0x05:prev.next = new Inst1.GrplEwIw_mem_sub(rm, true);break;
                    case 0x06:prev.next = new Inst1.GrplEwIw_mem_xor(rm, true);break;
                    case 0x07:prev.next = new Inst1.GrplEwIw_mem_cmp(rm, true);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* TEST Eb,Gb */
        ops[0x84] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.TestEbGb_reg(rm);
                }
                else {
                    prev.next = new Inst1.TestEbGb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x284] = ops[0x84];

        /* TEST Ew,Gw */
        ops[0x85] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.TestEwGw_reg(rm);
                }
                else {
                    prev.next = new Inst1.TestEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* XCHG Eb,Gb */
        ops[0x86] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.XchgEbGb_reg(rm);
                } else {
                    prev.next = new Inst1.XchgEbGb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x286] = ops[0x86];

        /* XCHG Ew,Gw */
        ops[0x87] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.XchgEwGw_reg(rm);
                } else {
                    prev.next = new Inst1.XchgEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOV Eb,Gb */
        ops[0x88] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.MovEbGb_reg(rm);
                } else {
                    if (rm == 5)
                        prev.next = new Inst1.MovEbGb_mem_5(rm);
                    else
                        prev.next = new Inst1.MovEbGb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x288] = ops[0x88];

        /* MOV Ew,Gw */
        ops[0x89] = new Decode() {
            final public int call(Op prev) {
                 int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.MovEwGw_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwGw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOV Gb,Eb */
        ops[0x8a] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.MovGbEb_reg(rm);
                } else {
                    prev.next = new Inst1.MovGbEb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x28a] = ops[0x8a];

        /* MOV Gw,Ew */
        ops[0x8b] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.MovGwEw_reg(rm);
                } else {
                    prev.next = new Inst1.MovGwEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* Mov Ew,Sw */
        ops[0x8c] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:					/* MOV Ew,ES */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEwEs_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEwEs_mem(rm);
                    }
                    break;
                case 0x01:					/* MOV Ew,CS */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEwCs_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEwCs_mem(rm);
                    }
                    break;
                case 0x02:					/* MOV Ew,SS */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEwSs_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEwSs_mem(rm);
                    }
                    break;
                case 0x03:					/* MOV Ew,DS */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEwDs_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEwDs_mem(rm);
                    }
                    break;
                case 0x04:					/* MOV Ew,FS */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEwFs_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEwFs_mem(rm);
                    }
                    break;
                case 0x05:					/* MOV Ew,GS */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEwGs_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEwGs_mem(rm);
                    }
                    break;
                default:
                    prev.next = new Inst1.Illegal("CPU:8c:Illegal RM Byte");
                    return RESULT_JUMP;
                }
                return RESULT_HANDLED;
            }
        };

        /* LEA Gw */
        ops[0x8d] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if ((prefixes & Core.PREFIX_ADDR)!=0) {
                    prev.next = new Inst1.LeaGw_32(rm);
                } else {
                    prev.next = new Inst1.LeaGw_16(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOV Sw,Ew */
        ops[0x8e] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:					/* MOV ES,Ew */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovEsEw_reg(rm);
                    } else {
                        prev.next = new Inst1.MovEsEw_mem(rm);
                    }
                    break;
//                case 0x01:					/* MOV CS,Ew */
//                    if (rm >= 0xC0) {
//                        prev.next = new Decoder2_instructions.MovCsEw_reg(rm);
//                    } else {
//                        prev.next = new Decoder2_instructions.MovCsW_mem(rm);
//                    }
//                    break;
                case 0x02:					/* MOV SS,Ew */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovSsEw_reg(rm);
                    } else {
                        prev.next = new Inst1.MovSsEw_mem(rm);
                    }
                    return RESULT_ANOTHER; //Always do another instruction
                case 0x03:					/* MOV DS,Ew */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovDsEw_reg(rm);
                    } else {
                        prev.next = new Inst1.MovDsEw_mem(rm);
                    }
                    break;
                case 0x04:					/* MOV FS,Ew */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovFsEw_reg(rm);
                    } else {
                        prev.next = new Inst1.MovFsEw_mem(rm);
                    }
                    break;
                case 0x05:					/* MOV GS,Ew */
                    if (rm >= 0xC0) {
                        prev.next = new Inst1.MovGsEw_reg(rm);
                    } else {
                        prev.next = new Inst1.MovGsEw_mem(rm);
                    }
                    break;
                default:
                    prev.next = new Inst1.Illegal("CPU:8e:Illegal RM Byte");
                    return RESULT_JUMP;
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x28e] = ops[0x8e];

        /* POP Ew */
        ops[0x8f] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.PopEw_reg(rm);
                } else {
                    prev.next = new Inst1.PopEw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* NOP */
        ops[0x90] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Noop();
                return RESULT_HANDLED;
            }
        };
        ops[0x290] = ops[0x90];

        /* XCHG CX,AX */
        ops[0x91] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* XCHG DX,AX */
        ops[0x92] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* XCHG BX,AX */
        ops[0x93] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* XCHG SP,AX */
        ops[0x94] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* XCHG BP,AX */
        ops[0x95] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* XCHG SI,AX */
        ops[0x96] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* XCHG DI,AX */
        ops[0x97] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.XchgAx(reg_edi);
                return RESULT_HANDLED;
            }
        };

        /* CBW */
        ops[0x98] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Cbw();
                return RESULT_HANDLED;
            }
        };

        /* CWD */
        ops[0x99] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Cwd();
                return RESULT_HANDLED;
            }
        };

        /* CALL Ap */
        ops[0x9a] = new Decode() {
            final public int call(Op prev) {
                int newip=decode_fetchw();
                int newcs=decode_fetchw();
                prev.next = new Inst1.CallAp(newcs, newip);
                return RESULT_JUMP;
            }
        };

        /* WAIT */
        ops[0x9b] = new Decode() {
            final public int call(Op prev) {
                /* No waiting here */
                prev.next = new Inst1.Wait();
                return RESULT_HANDLED;
            }
        };
        ops[0x29b] = ops[0x9b];

        /* PUSHF */
        ops[0x9c] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PushF();
                return RESULT_HANDLED;
            }
        };

        /* POPF */
        ops[0x9d] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.PopF();
                return RESULT_HANDLED;
            }
        };

        /* SAHF */
        ops[0x9e] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Sahf();
                return RESULT_HANDLED;
            }
        };
        ops[0x29e] = ops[0x9e];

        /* LAHF */
        ops[0x9f] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Lahf();
                return RESULT_HANDLED;
            }
        };
        ops[0x29f] = ops[0x9f];

        /* MOV AL,Ob */
        ops[0xa0] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovALOb();
                return RESULT_HANDLED;
            }
        };
        ops[0x2a0] = ops[0xa0];

        /* MOV AX,Ow */
        ops[0xa1] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovAXOw();
                return RESULT_HANDLED;
            }
        };

        /* MOV Ob,AL */
        ops[0xa2] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovObAL();
                return RESULT_HANDLED;
            }
        };
        ops[0x2a2] = ops[0xa2];

        /* MOV Ow,AX */
        ops[0xa3] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovOwAX();
                return RESULT_HANDLED;
            }
        };

        /* MOVSB */
        ops[0xa4] = new Decode() {
            final public int call(Op prev) {
                 if ((prefixes & Core.PREFIX_ADDR)==0) {
                    if ((prefixes & Core.PREFIX_REP)==0) {
                        prev.next = new Strings.Movsb16();
                    } else {
                        prev.next = new Strings.Movsb16r();
                    }
                } else {
                    if ((prefixes & Core.PREFIX_REP)==0) {
                        prev.next = new Strings.Movsb32();
                    } else {
                        prev.next = new Strings.Movsb32r();
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2a4] = ops[0xa4];

        /* MOVSW */
        ops[0xa5] = new Decode() {
            final public int call(Op prev) {
                if ((prefixes & Core.PREFIX_ADDR)==0) {
                    if ((prefixes & Core.PREFIX_REP)==0) {
                        prev.next = new Strings.Movsw16();
                    } else {
                        prev.next = new Strings.Movsw16r();
                    }
                } else {
                    if ((prefixes & Core.PREFIX_REP)==0) {
                        prev.next = new Strings.Movsw32();
                    } else {
                        prev.next = new Strings.Movsw32r();
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* CMPSB */
        ops[0xa6] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_CMPSB);
                return RESULT_HANDLED;
            }
        };
        ops[0x2a6] = ops[0xa6];

        /* CMPSW */
        ops[0xa7] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_CMPSW);
                return RESULT_HANDLED;
            }
        };

        /* TEST AL,Ib */
        ops[0xa8] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.TestAlIb();
                return RESULT_HANDLED;
            }
        };
        ops[0x2a8] = ops[0xa8];

        /* TEST AX,Iw */
        ops[0xa9] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.TestAxIw();
                return RESULT_HANDLED;
            }
        };

        /* STOSB */
        ops[0xaa] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_STOSB);
                return RESULT_HANDLED;
            }
        };
        ops[0x2aa] = ops[0xaa];

        /* STOSW */
        ops[0xab] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_STOSW);
                return RESULT_HANDLED;
            }
        };

        /* LODSB */
        ops[0xac] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_LODSB);
                return RESULT_HANDLED;
            }
        };
        ops[0x2ac] = ops[0xac];

        /* LODSW */
        ops[0xad] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_LODSW);
                return RESULT_HANDLED;
            }
        };

        /* SCASB */
        ops[0xae] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_SCASB);
                return RESULT_HANDLED;
            }
        };
        ops[0x2ae] = ops[0xae];

        /* SCASW */
        ops[0xaf] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.DoString(StringOp.R_SCASW);
                return RESULT_HANDLED;
            }
        };

        /* MOV AL,Ib */
        ops[0xb0] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_eax);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b0] = ops[0xb0];

        /* MOV CL,Ib */
        ops[0xb1] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_ecx);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b1] = ops[0xb1];

        /* MOV DL,Ib */
        ops[0xb2] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_edx);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b2] = ops[0xb2];

        /* MOV BL,Ib */
        ops[0xb3] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_ebx);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b3] = ops[0xb3];

        /* MOV AH,Ib */
        ops[0xb4] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_ah);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b4] = ops[0xb4];

        /* MOV CH,Ib */
        ops[0xb5] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_ch);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b5] = ops[0xb5];

        /* MOV DH,Ib */
        ops[0xb6] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_dh);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b6] = ops[0xb6];

        /* MOV BH,Ib */
        ops[0xb7] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIb(reg_bh);
                return RESULT_HANDLED;
            }
        };
        ops[0x2b7] = ops[0xb7];

        /* MOV AX,Iw */
        ops[0xb8] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_eax);
                return RESULT_HANDLED;
            }
        };

        /* MOV CX,Iw */
        ops[0xb9] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_ecx);
                return RESULT_HANDLED;
            }
        };

        /* MOV DX,Iw */
        ops[0xba] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_edx);
                return RESULT_HANDLED;
            }
        };

        /* MOV BX,Iw */
        ops[0xbb] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_ebx);
                return RESULT_HANDLED;
            }
        };

        /* MOV SP,Iw */
        ops[0xbc] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_esp);
                return RESULT_HANDLED;
            }
        };

        /* MOV BP.Iw */
        ops[0xbd] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_ebp);
                return RESULT_HANDLED;
            }
        };

        /* MOV SI,Iw */
        ops[0xbe] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_esi);
                return RESULT_HANDLED;
            }
        };

        /* MOV DI,Iw */
        ops[0xbf] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.MovIw(reg_edi);
                return RESULT_HANDLED;
            }
        };

        /* GRP2 Eb,Ib */
        ops[0xc0] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    int val = decode_fetchb() & 0x1f;
                    switch (which)	{
                    case 0x00:prev.next = new Grp2.ROLB_reg(rm, val);break;
                    case 0x01:prev.next = new Grp2.RORB_reg(rm, val);break;
                    case 0x02:prev.next = new Grp2.RCLB_reg(rm, val);break;
                    case 0x03:prev.next = new Grp2.RCRB_reg(rm, val);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLB_reg(rm, val);break;
                    case 0x05:prev.next = new Grp2.SHRB_reg(rm, val);break;
                    case 0x07:prev.next = new Grp2.SARB_reg(rm, val);break;
                    }
                } else {
                    EaaBase get_eaa= Mod.getEaa(rm);
                    int val = decode_fetchb() & 0x1f;
                    switch (which) {
                    case 0x00:prev.next = new Grp2.ROLB_mem(get_eaa, val);break;
                    case 0x01:prev.next = new Grp2.RORB_mem(get_eaa, val);break;
                    case 0x02:prev.next = new Grp2.RCLB_mem(get_eaa, val);break;
                    case 0x03:prev.next = new Grp2.RCRB_mem(get_eaa, val);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLB_mem(get_eaa, val);break;
                    case 0x05:prev.next = new Grp2.SHRB_mem(get_eaa, val);break;
                    case 0x07:prev.next = new Grp2.SARB_mem(get_eaa, val);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2c0] = ops[0xc0];

        /* GRP2 Ew,Ib */
        ops[0xc1] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    int val = decode_fetchb() & 0x1f;
                    switch (which)	{
                    case 0x00:prev.next = new Grp2.ROLW_reg(rm, val);break;
                    case 0x01:prev.next = new Grp2.RORW_reg(rm, val);break;
                    case 0x02:prev.next = new Grp2.RCLW_reg(rm, val);break;
                    case 0x03:prev.next = new Grp2.RCRW_reg(rm, val);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLW_reg(rm, val);break;
                    case 0x05:prev.next = new Grp2.SHRW_reg(rm, val);break;
                    case 0x07:prev.next = new Grp2.SARW_reg(rm, val);break;
                    }
                } else {
                    EaaBase get_eaa= Mod.getEaa(rm);
                    int val = decode_fetchb() & 0x1f;
                    switch (which) {
                    case 0x00:prev.next = new Grp2.ROLW_mem(get_eaa, val);break;
                    case 0x01:prev.next = new Grp2.RORW_mem(get_eaa, val);break;
                    case 0x02:prev.next = new Grp2.RCLW_mem(get_eaa, val);break;
                    case 0x03:prev.next = new Grp2.RCRW_mem(get_eaa, val);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLW_mem(get_eaa, val);break;
                    case 0x05:prev.next = new Grp2.SHRW_mem(get_eaa, val);break;
                    case 0x07:prev.next = new Grp2.SARW_mem(get_eaa, val);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* RETN Iw */
        ops[0xc2] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.RetnIw();
                return RESULT_JUMP;
            }
        };

        /* RETN */
        ops[0xc3] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Retn();
                return RESULT_JUMP;
            }
        };

        /* LES */
        ops[0xc4] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst1.Les(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* LDS */
        ops[0xc5] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst1.Lds(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* MOV Eb,Ib */
        ops[0xc6] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst1.MovIb(Mod.eb(rm));
                } else {
                    prev.next = new Inst1.MovIb_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2c6] = ops[0xc6];

        /* MOV EW,Iw */
        ops[0xc7] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                if (rm >= 0xc0) {
                    prev.next = new Inst1.MovIw(Mod.ew(rm));
                } else {
                    prev.next = new Inst1.MovIw_mem(rm);
                }
                return RESULT_HANDLED;
            }
        };

        /* ENTER Iw,Ib */
        ops[0xc8] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.EnterIwIb();
                return RESULT_HANDLED;
            }
        };

        /* LEAVE */
        ops[0xc9] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Leave();
                return RESULT_HANDLED;
            }
        };

        /* RETF Iw */
        ops[0xca] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.RetfIw();
                return RESULT_JUMP;
            }
        };

        /* RETF */
        ops[0xcb] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Retf();
                return RESULT_JUMP;
            }
        };

        /* INT3 */
        ops[0xcc] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Int3();
                return RESULT_JUMP;
            }
        };
        ops[0x2cc] = ops[0xcc];

        /* INT Ib */
        ops[0xcd] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.IntIb();
                return RESULT_JUMP;
            }
        };
        ops[0x2cd] = ops[0xcd];

        /* INTO */
        ops[0xce] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Int0();
                return RESULT_HANDLED;
            }
        };
        ops[0x2ce] = ops[0xce];

        /* IRET */
        ops[0xcf] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.IRet();
                return RESULT_JUMP;
            }
        };

        /* GRP2 Eb,1 */
        ops[0xd0] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    switch (which)	{
                    case 0x00:prev.next = new Grp2.ROLB_reg(rm, 1);break;
                    case 0x01:prev.next = new Grp2.RORB_reg(rm, 1);break;
                    case 0x02:prev.next = new Grp2.RCLB_reg(rm, 1);break;
                    case 0x03:prev.next = new Grp2.RCRB_reg(rm, 1);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLB_reg(rm, 1);break;
                    case 0x05:prev.next = new Grp2.SHRB_reg(rm, 1);break;
                    case 0x07:prev.next = new Grp2.SARB_reg(rm, 1);break;
                    }
                } else {
                    EaaBase get_eaa= Mod.getEaa(rm);
                    switch (which) {
                    case 0x00:prev.next = new Grp2.ROLB_mem(get_eaa, 1);break;
                    case 0x01:prev.next = new Grp2.RORB_mem(get_eaa, 1);break;
                    case 0x02:prev.next = new Grp2.RCLB_mem(get_eaa, 1);break;
                    case 0x03:prev.next = new Grp2.RCRB_mem(get_eaa, 1);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLB_mem(get_eaa, 1);break;
                    case 0x05:prev.next = new Grp2.SHRB_mem(get_eaa, 1);break;
                    case 0x07:prev.next = new Grp2.SARB_mem(get_eaa, 1);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2d0] = ops[0xd0];

        /* GRP2 Ew,1 */
        ops[0xd1] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                int val = 1;
                if (rm >= 0xc0) {
                    switch (which)	{
                    case 0x00:prev.next = new Grp2.ROLW_reg(rm, val);break;
                    case 0x01:prev.next = new Grp2.RORW_reg(rm, val);break;
                    case 0x02:prev.next = new Grp2.RCLW_reg(rm, val);break;
                    case 0x03:prev.next = new Grp2.RCRW_reg(rm, val);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLW_reg(rm, val);break;
                    case 0x05:prev.next = new Grp2.SHRW_reg(rm, val);break;
                    case 0x07:prev.next = new Grp2.SARW_reg(rm, val);break;
                    }
                } else {
                    EaaBase get_eaa= Mod.getEaa(rm);
                    switch (which) {
                    case 0x00:prev.next = new Grp2.ROLW_mem(get_eaa, val);break;
                    case 0x01:prev.next = new Grp2.RORW_mem(get_eaa, val);break;
                    case 0x02:prev.next = new Grp2.RCLW_mem(get_eaa, val);break;
                    case 0x03:prev.next = new Grp2.RCRW_mem(get_eaa, val);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLW_mem(get_eaa, val);break;
                    case 0x05:prev.next = new Grp2.SHRW_mem(get_eaa, val);break;
                    case 0x07:prev.next = new Grp2.SARW_mem(get_eaa, val);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* GRP2 Eb,CL */
        ops[0xd2] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    switch (which)	{
                    case 0x00:prev.next = new Grp2.ROLB_reg_cl(rm);break;
                    case 0x01:prev.next = new Grp2.RORB_reg_cl(rm);break;
                    case 0x02:prev.next = new Grp2.RCLB_reg_cl(rm);break;
                    case 0x03:prev.next = new Grp2.RCRB_reg_cl(rm);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLB_reg_cl(rm);break;
                    case 0x05:prev.next = new Grp2.SHRB_reg_cl(rm);break;
                    case 0x07:prev.next = new Grp2.SARB_reg_cl(rm);break;
                    }
                } else {
                    EaaBase get_eaa= Mod.getEaa(rm);
                    switch (which) {
                    case 0x00:prev.next = new Grp2.ROLB_mem_cl(get_eaa);break;
                    case 0x01:prev.next = new Grp2.RORB_mem_cl(get_eaa);break;
                    case 0x02:prev.next = new Grp2.RCLB_mem_cl(get_eaa);break;
                    case 0x03:prev.next = new Grp2.RCRB_mem_cl(get_eaa);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLB_mem_cl(get_eaa);break;
                    case 0x05:prev.next = new Grp2.SHRB_mem_cl(get_eaa);break;
                    case 0x07:prev.next = new Grp2.SARB_mem_cl(get_eaa);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2d2] = ops[0xd2];

        /* GRP2 Ew,CL */
        ops[0xd3] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                if (rm >= 0xc0) {
                    switch (which)	{
                    case 0x00:prev.next = new Grp2.ROLW_reg_cl(rm);break;
                    case 0x01:prev.next = new Grp2.RORW_reg_cl(rm);break;
                    case 0x02:prev.next = new Grp2.RCLW_reg_cl(rm);break;
                    case 0x03:prev.next = new Grp2.RCRW_reg_cl(rm);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLW_reg_cl(rm);break;
                    case 0x05:prev.next = new Grp2.SHRW_reg_cl(rm);break;
                    case 0x07:prev.next = new Grp2.SARW_reg_cl(rm);break;
                    }
                } else {
                    EaaBase get_eaa= Mod.getEaa(rm);
                    switch (which) {
                    case 0x00:prev.next = new Grp2.ROLW_mem_cl(get_eaa);break;
                    case 0x01:prev.next = new Grp2.RORW_mem_cl(get_eaa);break;
                    case 0x02:prev.next = new Grp2.RCLW_mem_cl(get_eaa);break;
                    case 0x03:prev.next = new Grp2.RCRW_mem_cl(get_eaa);break;
                    case 0x04:/* SHL and SAL are the same */
                    case 0x06:prev.next = new Grp2.SHLW_mem_cl(get_eaa);break;
                    case 0x05:prev.next = new Grp2.SHRW_mem_cl(get_eaa);break;
                    case 0x07:prev.next = new Grp2.SARW_mem_cl(get_eaa);break;
                    }
                }
                return RESULT_HANDLED;
            }
        };

        /* AAM Ib */
        ops[0xd4] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AamIb();
                return RESULT_HANDLED;
            }
        };
        ops[0x2d4] = ops[0xd4];

        /* AAD Ib */
        ops[0xd5] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.AadIb();
                return RESULT_HANDLED;
            }
        };
        ops[0x2d5] = ops[0xd5];

        /* SALC */
        ops[0xd6] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Salc();
                return RESULT_HANDLED;
            }
        };
        ops[0x2d6] = ops[0xd6];

        /* XLAT */
        ops[0xd7] = new Decode() {
            final public int call(Op prev) {
                if ((prefixes & Core.PREFIX_ADDR)!=0) {
                    prev.next = new Inst1.Xlat32();
                } else {
                    prev.next = new Inst1.Xlat16();
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2d7] = ops[0xd7];

        /* FPU ESC 0 */
        ops[0xd8] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU0_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FADD_ST0_STj(rm); break;
                            case 1: prev.next = new InstFPU.FMUL_ST0_STj(rm); break;
                            case 2: prev.next = new InstFPU.FCOM_STi(rm, false); break;
                            case 3: prev.next = new InstFPU.FCOM_STi(rm, true); break;
                            case 4: prev.next = new InstFPU.FSUB_ST0_STj(rm); break;
                            case 5: prev.next = new InstFPU.FSUBR_ST0_STj(rm); break;
                            case 6: prev.next = new InstFPU.FDIV_ST0_STj(rm); break;
                            case 7: prev.next = new InstFPU.FDIVR_ST0_STj(rm); break;
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU0_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FADD_SINGLE_REAL(rm); break;
                            case 1: prev.next = new InstFPU.FMUL_SINGLE_REAL(rm); break;
                            case 2: prev.next = new InstFPU.FCOM_SINGLE_REAL(rm, false); break;
                            case 3: prev.next = new InstFPU.FCOM_SINGLE_REAL(rm, true); break;
                            case 4: prev.next = new InstFPU.FSUB_SINGLE_REAL(rm); break;
                            case 5: prev.next = new InstFPU.FSUBR_SINGLE_REAL(rm); break;
                            case 6: prev.next = new InstFPU.FDIV_SINGLE_REAL(rm); break;
                            case 7: prev.next = new InstFPU.FDIVR_SINGLE_REAL(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2d8] = ops[0xd8];

        /* FPU ESC 1 */
        ops[0xd9] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU1_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FLD_STi(rm); break;
                            case 1: prev.next = new InstFPU.FXCH_STi(rm); break;
                            case 2: prev.next = new InstFPU.FNOP(); break;
                            case 3: prev.next = new InstFPU.FST_STi(rm, true); break;
                            case 4:
                            {
                                switch (rm & 7) {
                                    case 0: prev.next = new InstFPU.FCHS(); break;
                                    case 1: prev.next = new InstFPU.FABS(); break;
                                    case 2: prev.next = new Inst1.FPU1_normal(rm); break;
                                    case 3: prev.next = new Inst1.FPU1_normal(rm); break;
                                    case 4: prev.next = new InstFPU.FTST(); break;
                                    case 5: prev.next = new InstFPU.FXAM(); break;
                                    case 6: prev.next = new Inst1.FPU1_normal(rm); break;
                                    case 7: prev.next = new Inst1.FPU1_normal(rm); break;
                                }
                                break;
                            }
                            case 5:
                            {
                                switch (rm & 7) {
                                    case 0: prev.next = new InstFPU.FLD1(); break;
                                    case 1: prev.next = new InstFPU.FLDL2T(); break;
                                    case 2: prev.next = new InstFPU.FLDL2E(); break;
                                    case 3: prev.next = new InstFPU.FLDPI(); break;
                                    case 4: prev.next = new InstFPU.FLDLG2(); break;
                                    case 5: prev.next = new InstFPU.FLDLN2(); break;
                                    case 6: prev.next = new InstFPU.FLDZ(); break;
                                    case 7: prev.next = new Inst1.FPU1_normal(rm); break;
                                }
                                break;
                            }
                            case 6:
                            {
                                switch (rm & 7) {
                                    case 0: prev.next = new InstFPU.F2XM1(); break;
                                    case 1: prev.next = new InstFPU.FYL2X(); break;
                                    case 2: prev.next = new InstFPU.FPTAN(); break;
                                    case 3: prev.next = new InstFPU.FPATAN(); break;
                                    case 4: prev.next = new InstFPU.FXTRACT(); break;
                                    case 5: prev.next = new InstFPU.FPREM(true); break;
                                    case 6: prev.next = new InstFPU.FDECSTP(); break;
                                    case 7: prev.next = new InstFPU.FINCSTP(); break;
                                }
                                break;
                            }
                            case 7:
                            {
                                switch (rm & 7) {
                                    case 0: prev.next = new InstFPU.FPREM(false); break;
                                    case 1: prev.next = new InstFPU.FYL2XP1(); break;
                                    case 2: prev.next = new InstFPU.FSQRT(); break;
                                    case 3: prev.next = new InstFPU.FSINCOS(); break;
                                    case 4: prev.next = new InstFPU.FRNDINT(); break;
                                    case 5: prev.next = new InstFPU.FSCALE(); break;
                                    case 6: prev.next = new InstFPU.FSIN(); break;
                                    case 7: prev.next = new InstFPU.FCOS(); break;
                                }
                                break;
                            }
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU1_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FLD_SINGLE_REAL(rm); break;
                            case 1: prev.next = new Inst1.FPU2_ea(rm); break;
                            case 2: prev.next = new InstFPU.FST_SINGLE_REAL(rm, false); break;
                            case 3: prev.next = new InstFPU.FST_SINGLE_REAL(rm, true); break;
                            case 4: prev.next = new InstFPU.FLDENV(rm); break;
                            case 5: prev.next = new InstFPU.FLDCW(rm); break;
                            case 6: prev.next = new InstFPU.FNSTENV(rm); break;
                            case 7: prev.next = new InstFPU.FNSTCW(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2d9] = ops[0xd9];

        /* FPU ESC 2 */
        ops[0xda] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU2_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FCMOV_ST0_STj_CF(rm); break;
                            case 1: prev.next = new InstFPU.FCMOV_ST0_STj_ZF(rm); break;
                            case 2: prev.next = new InstFPU.FCMOV_ST0_STj_CF_OR_ZF(rm); break;
                            case 3: prev.next = new InstFPU.FCMOV_ST0_STj_PF(rm); break;
                            case 5:
                            {
                                if ((rm & 7)==5) {
                                    prev.next = new InstFPU.FUCOMPP();
                                } else {
                                    prev.next = new Inst1.FPU2_normal(rm);
                                }
                            }
                            default:
                                prev.next = new Inst1.FPU2_normal(rm);
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU2_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FIADD_DWORD_INTEGER(rm); break;
                            case 1: prev.next = new InstFPU.FIMUL_DWORD_INTEGER(rm); break;
                            case 2: prev.next = new InstFPU.FICOM_DWORD_INTEGER(rm, false); break;
                            case 3: prev.next = new InstFPU.FICOM_DWORD_INTEGER(rm, true); break;
                            case 4: prev.next = new InstFPU.FISUB_DWORD_INTEGER(rm); break;
                            case 5: prev.next = new InstFPU.FISUBR_DWORD_INTEGER(rm); break;
                            case 6: prev.next = new InstFPU.FIDIV_DWORD_INTEGER(rm); break;
                            case 7: prev.next = new InstFPU.FIDIVR_DWORD_INTEGER(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2da] = ops[0xda];

        /* FPU ESC 3 */
        ops[0xdb] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU3_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FCMOV_ST0_STj_NCF(rm); break;
                            case 1: prev.next = new InstFPU.FCMOV_ST0_STj_NZF(rm); break;
                            case 2: prev.next = new InstFPU.FCMOV_ST0_STj_NCF_AND_NZF(rm); break;
                            case 3: prev.next = new InstFPU.FCMOV_ST0_STj_NPF(rm); break;
                            case 4:
                            {
                                switch (rm & 7) {
                                    case 2:prev.next = new InstFPU.FNCLEX(); break;
                                    case 3:prev.next = new InstFPU.FNINIT(); break;
                                    default:prev.next = new Inst1.FPU3_normal(rm);

                                }
                                break;
                            }
                            case 5: prev.next = new InstFPU.FUCOMI_ST0_STj(rm, false); break;
                            case 6: prev.next = new InstFPU.FCOMI_ST0_STj(rm, false); break;
                            default:
                                prev.next = new Inst1.FPU3_normal(rm);
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU3_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FILD_DWORD_INTEGER(rm); break;
                            case 1: prev.next = new InstFPU.FISTTP32(rm); break;
                            case 2: prev.next = new InstFPU.FIST_DWORD_INTEGER(rm, false); break;
                            case 3: prev.next = new InstFPU.FIST_DWORD_INTEGER(rm, true); break;
                            case 4: prev.next = new Inst1.FPU3_ea(rm); break;
                            case 5: prev.next = new InstFPU.FLD_EXTENDED_REAL(rm); break;
                            case 6: prev.next = new Inst1.FPU3_ea(rm); break;
                            case 7: prev.next = new InstFPU.FSTP_EXTENDED_REAL(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2db] = ops[0xdb];

        /* FPU ESC 4 */
        ops[0xdc] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU4_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FADD_STi_ST0(rm, false); break;
                            case 1: prev.next = new InstFPU.FMUL_STi_ST0(rm, false); break;
                            case 2: prev.next = new InstFPU.FCOM_STi(rm, false); break;
                            case 3: prev.next = new InstFPU.FCOM_STi(rm, true); break;
                            case 4: prev.next = new InstFPU.FSUBR_STi_ST0(rm, false); break;
                            case 5: prev.next = new InstFPU.FSUB_STi_ST0(rm, false); break;
                            case 6: prev.next = new InstFPU.FDIVR_STi_ST0(rm, false); break;
                            case 7: prev.next = new InstFPU.FDIV_STi_ST0(rm, false); break;
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU4_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FADD_DOUBLE_REAL(rm); break;
                            case 1: prev.next = new InstFPU.FMUL_DOUBLE_REAL(rm); break;
                            case 2: prev.next = new InstFPU.FCOM_DOUBLE_REAL(rm, false); break;
                            case 3: prev.next = new InstFPU.FCOM_DOUBLE_REAL(rm, true); break;
                            case 4: prev.next = new InstFPU.FSUB_DOUBLE_REAL(rm); break;
                            case 5: prev.next = new InstFPU.FSUBR_DOUBLE_REAL(rm); break;
                            case 6: prev.next = new InstFPU.FDIV_DOUBLE_REAL(rm); break;
                            case 7: prev.next = new InstFPU.FDIVR_DOUBLE_REAL(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2dc] = ops[0xdc];

        /* FPU ESC 5 */
        ops[0xdd] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU5_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FFREE_STi(rm); break;
                            case 1: prev.next = new InstFPU.FXCH_STi(rm); break;
                            case 2: prev.next = new InstFPU.FST_STi(rm, false); break;
                            case 3: prev.next = new InstFPU.FST_STi(rm, true); break;
                            case 4: prev.next = new InstFPU.FUCOM_STi(rm, false); break;
                            case 5: prev.next = new InstFPU.FUCOM_STi(rm, true); break;
                            default: prev.next = new Inst1.FPU5_normal(rm);
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU5_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FLD_DOUBLE_REAL(rm); break;
                            case 1: prev.next = new InstFPU.FISTTP64(rm); break;
                            case 2: prev.next = new InstFPU.FST_DOUBLE_REAL(rm, false); break;
                            case 3: prev.next = new InstFPU.FST_DOUBLE_REAL(rm, true); break;
                            case 4: prev.next = new InstFPU.FRSTOR(rm); break;
                            case 5: prev.next = new Inst1.FPU5_ea(rm); break;
                            case 6: prev.next = new InstFPU.FNSAVE(rm); break;
                            case 7: prev.next = new InstFPU.FNSTSW(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2dd] = ops[0xdd];

        /* FPU ESC 6 */
        ops[0xde] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU6_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FADD_STi_ST0(rm, true); break;
                            case 1: prev.next = new InstFPU.FMUL_STi_ST0(rm, true); break;
                            case 2: prev.next = new InstFPU.FCOM_STi(rm, true); break;
                            case 3:
                            {
                                if ((rm & 7) == 1)
                                    prev.next = new InstFPU.FCOMPP();
                                else
                                    prev.next = new Inst1.FPU6_normal(rm);
                            }
                            break;
                            case 4: prev.next = new InstFPU.FSUBR_STi_ST0(rm, true); break;
                            case 5: prev.next = new InstFPU.FSUB_STi_ST0(rm, true); break;
                            case 6: prev.next = new InstFPU.FDIVR_STi_ST0(rm, true); break;
                            case 7: prev.next = new InstFPU.FDIV_STi_ST0(rm, true); break;
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU6_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FIADD_WORD_INTEGER(rm); break;
                            case 1: prev.next = new InstFPU.FIMUL_WORD_INTEGER(rm); break;
                            case 2: prev.next = new InstFPU.FICOM_WORD_INTEGER(rm, false); break;
                            case 3: prev.next = new InstFPU.FICOM_WORD_INTEGER(rm, true); break;
                            case 4: prev.next = new InstFPU.FISUB_WORD_INTEGER(rm); break;
                            case 5: prev.next = new InstFPU.FISUBR_WORD_INTEGER(rm); break;
                            case 6: prev.next = new InstFPU.FIDIV_WORD_INTEGER(rm); break;
                            case 7: prev.next = new InstFPU.FIDIVR_WORD_INTEGER(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2de] = ops[0xde];

        /* FPU ESC 7 */
        ops[0xdf] = new Decode() {
            final public int call(Op prev) {
                int rm = decode_fetchb();
                if (rm >= 0xc0) {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU7_normal(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FFREEP_STi(rm); break;
                            case 1: prev.next = new InstFPU.FXCH_STi(rm); break;
                            case 2:
                            case 3: prev.next = new InstFPU.FST_STi(rm, true); break;
                            case 4:
                            {
                                if ((rm & 7)==0)
                                    prev.next = new InstFPU.FNSTSW_AX();
                                else
                                    prev.next = new Inst1.FPU7_normal(rm);
                            }
                            break;
                            case 5: prev.next = new InstFPU.FUCOMI_ST0_STj(rm, true); break;
                            case 6: prev.next = new InstFPU.FCOMI_ST0_STj(rm, true); break;
                            case 7: prev.next = new Inst1.FPU7_normal(rm); break;
                        }
                    }
                } else {
                    if (FPU.softFPU || !FPU.shouldInline) {
                        prev.next = new Inst1.FPU7_ea(rm);
                    } else {
                        switch ((rm >> 3) & 7) {
                            case 0: prev.next = new InstFPU.FILD_WORD_INTEGER(rm); break;
                            case 1: prev.next = new InstFPU.FISTTP16(rm); break;
                            case 2: prev.next = new InstFPU.FIST_WORD_INTEGER(rm, false); break;
                            case 3: prev.next = new InstFPU.FIST_WORD_INTEGER(rm, true); break;
                            case 4: prev.next = new InstFPU.FBLD_PACKED_BCD(rm); break;
                            case 5: prev.next = new InstFPU.FILD_QWORD_INTEGER(rm); break;
                            case 6: prev.next = new InstFPU.FBSTP_PACKED_BCD(rm); break;
                            case 7: prev.next = new InstFPU.FISTP_QWORD_INTEGER(rm); break;
                        }
                    }
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2df] = ops[0xdf];

        /* LOOPNZ */
        ops[0xe0] = new Decode() {
            final public int call(Op prev) {
                if ((prefixes & Core.PREFIX_ADDR)!=0) {
                    prev.next = new Inst1.Loopnz32();
                } else {
                    prev.next = new Inst1.Loopnz16();
                }
                return RESULT_JUMP;
            }
        };

        /* LOOPZ */
        ops[0xe1] = new Decode() {
            final public int call(Op prev) {
                if ((prefixes & Core.PREFIX_ADDR)!=0) {
                    prev.next = new Inst1.Loopz32();
                } else {
                    prev.next = new Inst1.Loopz16();
                }
                return RESULT_JUMP;
            }
        };

        /* LOOP */
        ops[0xe2] = new Decode() {
            final public int call(Op prev) {
                if ((prefixes & Core.PREFIX_ADDR)!=0) {
                    prev.next = new Inst1.Loop32();
                } else {
                    prev.next = new Inst1.Loop16();
                }
                return RESULT_JUMP;
            }
        };

        /* JCXZ */
        ops[0xe3] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Jcxz(AddrMaskTable1[prefixes & Core.PREFIX_ADDR]);
                return RESULT_JUMP;
            }
        };

        /* IN AL,Ib */
        ops[0xe4] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.InAlIb();
                return RESULT_HANDLED;
            }
        };
        ops[0x2e4] = ops[0xe4];

        /* IN AX,Ib */
        ops[0xe5] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.InAxIb();
                return RESULT_HANDLED;
            }
        };

        /* OUT Ib,AL */
        ops[0xe6] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.OutAlIb();
                return RESULT_HANDLED;
            }
        };
        ops[0x2e6] = ops[0xe6];

        /* OUT Ib,AX */
        ops[0xe7] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.OutAxIb();
                return RESULT_HANDLED;
            }
        };

        /* CALL Jw */
        ops[0xe8] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.CallJw();
                return RESULT_JUMP;
            }
        };

        /* JMP Jw */
        ops[0xe9] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JmpJw();
                return RESULT_JUMP;
            }
        };

        /* JMP Ap */
        ops[0xea] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JmpAp();
                return RESULT_JUMP;
            }
        };

        /* JMP Jb */
        ops[0xeb] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.JmpJb();
                return RESULT_JUMP;
            }
        };

        /* IN AL,DX */
        ops[0xec] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.InAlDx();
                return RESULT_HANDLED;
            }
        };
        ops[0x2ec] = ops[0xec];

        /* IN AX,DX */
        ops[0xed] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.InAxDx();
                return RESULT_HANDLED;
            }
        };

        /* OUT DX,AL */
        ops[0xee] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.OutAlDx();
                return RESULT_HANDLED;
            }
        };
        ops[0x2ee] = ops[0xee];

        /* OUT DX,AX */
        ops[0xef] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.OutAxDx();
                return RESULT_HANDLED;
            }
        };

        /* LOCK */
        ops[0xf0] = new Decode() {
            final public int call(Op prev) {
                // :TODO: check for illegal combinations like in the normal core
                return RESULT_CONTINUE;
            }
        };
        ops[0x2f0] = ops[0xf0];

        /* ICEBP */
        ops[0xf1] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Icebp();
                return RESULT_JUMP;
            }
        };
        ops[0x2f1] = ops[0xf1];

        /* REPNZ */
        ops[0xf2] = new Decode() {
            final public int call(Op prev) {
                prefixes|=Core.PREFIX_REP;
                Core.rep_zero = false;
                return RESULT_CONTINUE;
            }
        };
        ops[0x2f2] = ops[0xf2];

        /* REPZ */
        ops[0xf3] = new Decode() {
            final public int call(Op prev) {
                prefixes|=Core.PREFIX_REP;
                Core.rep_zero = true;
                return RESULT_CONTINUE;
            }
        };
        ops[0x2f3] = ops[0xf3];

        /* HLT */
        ops[0xf4] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Hlt();
                return RESULT_JUMP;
            }
        };
        ops[0x2f4] = ops[0xf4];

        /* CMC */
        ops[0xf5] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Cmc();
                return RESULT_HANDLED;
            }
        };
        ops[0x2f5] = ops[0xf5];

        /* GRP3 Eb(,Ib) */
        ops[0xf6] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:											/* TEST Eb,Ib */
                case 0x01:											/* TEST Eb,Ib Undocumented*/
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.Testb_reg(rm);
                    }
                    else {
                        prev.next = new Grp3.Testb_mem(rm);
                    }
                    break;
                case 0x02:											/* NOT Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.NotEb_reg(rm);
                    } else {
                        prev.next = new Grp3.NotEb_mem(rm);
                    }
                    break;
                case 0x03:                                          /* NEG Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.NegEb_reg(rm);
                    } else {
                        prev.next = new Grp3.NegEb_mem(rm);
                    }
                    break;
                case 0x04:											/* MUL AL,Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.MulAlEb_reg(rm);
                    } else {
                        prev.next = new Grp3.MulAlEb_mem(rm);
                    }
                    break;
                case 0x05:											/* IMUL AL,Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.IMulAlEb_reg(rm);
                    } else {
                        prev.next = new Grp3.IMulAlEb_mem(rm);
                    }
                    break;
                case 0x06:											/* DIV Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.DivAlEb_reg(rm);
                    } else {
                        prev.next = new Grp3.DivAlEb_mem(rm);
                    }
                    break;
                case 0x07:											/* IDIV Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.IDivAlEb_reg(rm);
                    } else {
                        prev.next = new Grp3.IDivAlEb_mem(rm);
                    }
                    break;
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2f6] = ops[0xf6];

        /* GRP3 Ew(,Iw) */
        ops[0xf7] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:											/* TEST Ew,Iw */
                case 0x01:											/* TEST Ew,Iw Undocumented*/
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.Testw_reg(rm);
                    }
                    else {
                        prev.next = new Grp3.Testw_mem(rm);
                    }
                    break;
                case 0x02:											/* NOT Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.NotEw_reg(rm);
                    } else {
                        prev.next = new Grp3.NotEw_mem(rm);
                    }
                    break;
                case 0x03:											/* NEG Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.NegEw_reg(rm);
                    } else {
                        prev.next = new Grp3.NegEw_mem(rm);
                    }
                    break;
                case 0x04:											/* MUL AX,Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.MulAxEw_reg(rm);
                    } else {
                        prev.next = new Grp3.MulAxEw_mem(rm);
                    }
                    break;
                case 0x05:											/* IMUL AX,Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.IMulAxEw_reg(rm);
                    } else {
                        prev.next = new Grp3.IMulAxEw_mem(rm);
                    }
                    break;
                case 0x06:											/* DIV Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.DivAxEw_reg(rm);
                    } else {
                        prev.next = new Grp3.DivAxEw_mem(rm);
                    }
                    break;
                case 0x07:											/* IDIV Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Grp3.IDivAxEw_reg(rm);
                    } else {
                        prev.next = new Grp3.IDivAxEw_mem(rm);
                    }
                    break;
                }
                return RESULT_HANDLED;
            }
        };

        /* CLC */
        ops[0xf8] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Clc();
                return RESULT_HANDLED;
            }
        };
        ops[0x2f8] = ops[0xf8];

        /* STC */
        ops[0xf9] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Stc();
                return RESULT_HANDLED;
            }
        };
        ops[0x2f9] = ops[0xf9];

        /* CLI */
        ops[0xfa] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Cli();
                return RESULT_HANDLED;
            }
        };
        ops[0x2fa] = ops[0xfa];

        /* STI */
        ops[0xfb] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Sti();
                return RESULT_HANDLED;
            }
        };
        ops[0x2fb] = ops[0xfb];

        /* CLD */
        ops[0xfc] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Cld();
                return RESULT_HANDLED;
            }
        };
        ops[0x2fc] = ops[0xfc];

        /* STD */
        ops[0xfd] = new Decode() {
            final public int call(Op prev) {
                prev.next = new Inst1.Std();
                return RESULT_HANDLED;
            }
        };
        ops[0x2fd] = ops[0xfd];

        /* GRP4 Eb */
        ops[0xfe] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:										/* INC Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst1.Incb_reg(rm);
                    }
                    else {
                        prev.next = new Inst1.Incb_mem(rm);
                    }
                    break;
                case 0x01:										/* DEC Eb */
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst1.Decb_reg(rm);
                    }
                    else {
                        prev.next = new Inst1.Decb_mem(rm);
                    }
                    break;
                case 0x07:										/* CallBack */
                    prev.next = new Inst1.Callback(decode_fetchw());
                    return RESULT_CALLBACK;
                default:
                    prev.next = new Inst1.Illegal("Illegal GRP4 Call "+((rm>>3) & 7));
                    return RESULT_JUMP;
                }
                return RESULT_HANDLED;
            }
        };
        ops[0x2fe] = ops[0xfe];

        /* GRP5 Ew */
        ops[0xff] = new Decode() {
            final public int call(Op prev) {
                int rm=decode_fetchb();
                int which=(rm>>3)&7;
                switch (which) {
                case 0x00:										/* INC Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst1.Incw_reg(rm);
                    }
                    else {
                        prev.next = new Inst1.Incw_mem(rm);
                    }
                    break;
                case 0x01:										/* DEC Ew */
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst1.Decw_reg(rm);
                    }
                    else {
                        prev.next = new Inst1.Decw_mem(rm);
                    }
                    break;
                case 0x02:										/* CALL Ev */
                    if (rm >= 0xc0 ) {
                        prev.next = new Inst1.CallEv_reg(rm);
                    }
                    else {
                        prev.next = new Inst1.CallEv_mem(rm);
                    }
                    return RESULT_JUMP;
                case 0x03:										/* CALL Ep */
                    if (rm >= 0xc0) {
                        prev.next = new Inst1.Illegal("");
                    } else {
                        prev.next = new Inst1.CallEp(rm);
                    }
                    return RESULT_JUMP;
                case 0x04:										/* JMP Ev */
                    if (rm >= 0xc0) {
                        prev.next = new Inst1.JmpEv_reg(rm);
                    } else {
                        prev.next = new Inst1.JmpEv_mem(rm);
                    }
                    return RESULT_JUMP;
                case 0x05:										/* JMP Ep */
                    if (rm >= 0xc0) {
                        prev.next = new Inst1.Illegal("");
                    } else {
                        prev.next = new Inst1.JmpEp(rm);
                    }
                    return RESULT_JUMP;
                case 0x06:										/* PUSH Ev */
                    if (rm >= 0xc0) {
                        prev.next = new Inst1.PushEv_reg(rm);
                    } else {
                        prev.next = new Inst1.PushEv_mem(rm);
                    }
                    break;
                default:
                    prev.next = new Inst1.Illegal("CPU:GRP5:Illegal Call "+Integer.toString(which,16));
                    return RESULT_JUMP;
                }
                return RESULT_HANDLED;
            }
        };
    }
}
