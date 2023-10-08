package jdos.cpu.core_dynamic;

import jdos.cpu.Core;
import jdos.cpu.StringOp;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class Prefix_66 extends Helper {
    static public void init(Decode[] ops) {
        /* ADD Ed,Gd */
        ops[0x201] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Addd_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.AddEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* ADD Gd,Ed */
        ops[0x203] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Addd_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.AddGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* ADD EAX,Id */
        ops[0x205] = prev -> {
            prev.next = new Inst3.AddEaxId();
            return RESULT_HANDLED;
        };

        /* PUSH ES */
        ops[0x206] = prev -> {
            prev.next = new Inst3.Push32ES();
            return RESULT_HANDLED;
        };

         /* POP ES */
        ops[0x207] = prev -> {
            prev.next = new Inst3.Pop32ES();
            return RESULT_HANDLED;
        };

        /* OR Ed,Gd */
        ops[0x209] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Ord_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.OrEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* OR Gd,Ed */
        ops[0x20b] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Ord_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.OrGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* OR EAX,Id */
        ops[0x20d] = prev -> {
            prev.next = new Inst3.OrEaxId();
            return RESULT_HANDLED;
        };

        /* PUSH CS */
        ops[0x20e] = prev -> {
            prev.next = new Inst3.Push32CS();
            return RESULT_HANDLED;
        };

        /* ADC Ed,Gd */
        ops[0x211] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Adcd_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.AdcEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* ADC Gd,Ed */
        ops[0x213] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Adcd_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.AdcGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* ADC EAX,Id */
        ops[0x215] = prev -> {
            prev.next = new Inst3.AdcEaxId();
            return RESULT_HANDLED;
        };

        /* PUSH SS */
        ops[0x216] = prev -> {
            prev.next = new Inst3.Push32SS();
            return RESULT_HANDLED;
        };

         /* POP SS */
        ops[0x217] = prev -> {
            prev.next = new Inst3.Pop32SS();
            return RESULT_HANDLED;
        };

        /* SBB Ed,Gd */
        ops[0x219] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Sbbd_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.SbbEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* SBB Gd,Ed */
        ops[0x21b] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Sbbd_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.SbbGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* SBB EAX,Id */
        ops[0x21d] = prev -> {
            prev.next = new Inst3.SbbEaxId();
            return RESULT_HANDLED;
        };

        /* PUSH DS */
        ops[0x21e] = prev -> {
            prev.next = new Inst3.Push32DS();
            return RESULT_HANDLED;
        };

         /* POP DS */
        ops[0x21f] = prev -> {
            prev.next = new Inst3.Pop32DS();
            return RESULT_HANDLED;
        };

        /* AND Ed,Gd */
        ops[0x221] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Andd_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.AndEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* AND Gd,Ed */
        ops[0x223] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Andd_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.AndGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* AND EAX,Id */
        ops[0x225] = prev -> {
            prev.next = new Inst3.AndEaxId();
            return RESULT_HANDLED;
        };

        /* SUB Ed,Gd */
        ops[0x229] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Subd_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.SubEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* SUB Gd,Ed */
        ops[0x22b] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Subd_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.SubGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* SUB EAX,Id */
        ops[0x22d] = prev -> {
            prev.next = new Inst3.SubEaxId();
            return RESULT_HANDLED;
        };

        /* XOR Ed,Gd */
        ops[0x231] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Xord_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.XorEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* XOR Gd,Ed */
        ops[0x233] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Xord_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.XorGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* XOR EAX,Id */
        ops[0x235] = prev -> {
            prev.next = new Inst3.XorEaxId();
            return RESULT_HANDLED;
        };

        /* CMP Ed,Gd */
        ops[0x239] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Cmpd_reg(Mod.ed(rm), Mod.gd(rm));
            } else {
                prev.next = new Inst3.CmpEdGd_mem(Mod.getEaa(rm), Mod.gd(rm));
            }
            return RESULT_HANDLED;
        };

        /* CMP Gd,Ed */
        ops[0x23b] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.Cmpd_reg(Mod.gd(rm), Mod.ed(rm));
            } else {
                prev.next = new Inst3.CmpGdEd_mem(Mod.gd(rm), Mod.getEaa(rm));
            }
            return RESULT_HANDLED;
        };

        /* CMP EAX,Id */
        ops[0x23d] = prev -> {
            prev.next = new Inst3.CmpEaxId();
            return RESULT_HANDLED;
        };

        /* INC EAX */
        ops[0x240] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_eax);
            return RESULT_HANDLED;
        };

        /* INC ECX */
        ops[0x241] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_ecx);
            return RESULT_HANDLED;
        };

        /* INC EDX */
        ops[0x242] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_edx);
            return RESULT_HANDLED;
        };

        /* INC EBX */
        ops[0x243] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_ebx);
            return RESULT_HANDLED;
        };

        /* INC ESP */
        ops[0x244] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_esp);
            return RESULT_HANDLED;
        };

        /* INC EBP */
        ops[0x245] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_ebp);
            return RESULT_HANDLED;
        };

        /* INC ESI */
        ops[0x246] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_esi);
            return RESULT_HANDLED;
        };

        /* INC EDI */
        ops[0x247] = prev -> {
            prev.next = new Inst3.Incd_reg(reg_edi);
            return RESULT_HANDLED;
        };

        /* DEC EAX */
        ops[0x248] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_eax);
            return RESULT_HANDLED;
        };

        /* DEC ECX */
        ops[0x249] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_ecx);
            return RESULT_HANDLED;
        };

        /* DEC EDX */
        ops[0x24a] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_edx);
            return RESULT_HANDLED;
        };

        /* DEC EBX */
        ops[0x24b] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_ebx);
            return RESULT_HANDLED;
        };

        /* DEC ESP */
        ops[0x24c] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_esp);
            return RESULT_HANDLED;
        };

        /* DEC EBP */
        ops[0x24d] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_ebp);
            return RESULT_HANDLED;
        };

        /* DEC ESI */
        ops[0x24e] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_esi);
            return RESULT_HANDLED;
        };

        /* DEC EDI */
        ops[0x24f] = prev -> {
            prev.next = new Inst3.Decd_reg(reg_edi);
            return RESULT_HANDLED;
        };

        /* PUSH EAX */
        ops[0x250] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_eax);
            return RESULT_HANDLED;
        };

        /* PUSH ECX */
        ops[0x251] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_ecx);
            return RESULT_HANDLED;
        };

        /* PUSH EDX */
        ops[0x252] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_edx);
            return RESULT_HANDLED;
        };

        /* PUSH EBX */
        ops[0x253] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_ebx);
            return RESULT_HANDLED;
        };

        /* PUSH ESP */
        ops[0x254] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_esp);
            return RESULT_HANDLED;
        };

        /* PUSH EBP */
        ops[0x255] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_ebp);
            return RESULT_HANDLED;
        };

        /* PUSH ESI */
        ops[0x256] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_esi);
            return RESULT_HANDLED;
        };

        /* PUSH EDI */
        ops[0x257] = prev -> {
            prev.next = new Inst3.Push32_reg(reg_edi);
            return RESULT_HANDLED;
        };

        /* POP EAX */
        ops[0x258] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_eax);
            return RESULT_HANDLED;
        };

        /* POP ECX */
        ops[0x259] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_ecx);
            return RESULT_HANDLED;
        };

        /* POP EDX */
        ops[0x25a] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_edx);
            return RESULT_HANDLED;
        };

        /* POP EBX */
        ops[0x25b] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_ebx);
            return RESULT_HANDLED;
        };

        /* POP ESP */
        ops[0x25c] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_esp);
            return RESULT_HANDLED;
        };

        /* POP EBP */
        ops[0x25d] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_ebp);
            return RESULT_HANDLED;
        };

        /* POP ESI */
        ops[0x25e] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_esi);
            return RESULT_HANDLED;
        };

        /* POP EDI */
        ops[0x25f] = prev -> {
            prev.next = new Inst3.Pop32_reg(reg_edi);
            return RESULT_HANDLED;
        };

        /* PUSHAD */
        ops[0x260] = prev -> {
            prev.next = new Inst3.Pushad();
            return RESULT_HANDLED;
        };

        /* POPAD */
        ops[0x261] = prev -> {
            prev.next = new Inst3.Popad();
            return RESULT_HANDLED;
        };

        /* BOUND Ed */
        ops[0x262] = prev -> {
            prev.next = new Inst3.BoundEd();
            return RESULT_HANDLED;
        };

        /* ARPL Ed,Rd */
        ops[0x263] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.ArplEdRd_reg(rm);
            } else {
                prev.next = new Inst3.ArplEdRd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* PUSH Id */
        ops[0x268] = prev -> {
            prev.next = new Inst3.PushId();
            return RESULT_HANDLED;
        };

        /* IMUL Gd,Ed,Id */
        ops[0x269] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.ImulGdEdId_reg(rm);
            } else {
                prev.next = new Inst3.ImulGdEdId_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* PUSH Ib */
        ops[0x26a] = prev -> {
            prev.next = new Inst3.PushIb();
            return RESULT_HANDLED;
        };

        /* IMUL Gd,Ed,Ib */
        ops[0x26b] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.ImulGdEdIb_reg(rm);
            } else {
                prev.next = new Inst3.ImulGdEdIb_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* INSD */
        ops[0x26d] = prev -> {
            prev.next = new Inst1.DoStringException(StringOp.R_INSD, 4);
            return RESULT_HANDLED;
        };

        /* OUTSD */
        ops[0x26f] = prev -> {
            prev.next = new Inst1.DoStringException(StringOp.R_OUTSD, 4);
            return RESULT_HANDLED;
        };

        /* JO */
        ops[0x270] = prev -> {
            prev.next = new Inst3.JumpCond32_b_o();
            return RESULT_JUMP;
        };

        /* JNO */
        ops[0x271] = prev -> {
            prev.next = new Inst3.JumpCond32_b_no();
            return RESULT_JUMP;
        };

        /* JB */
        ops[0x272] = prev -> {
            prev.next = new Inst3.JumpCond32_b_b();
            return RESULT_JUMP;
        };

        /* JNB */
        ops[0x273] = prev -> {
            prev.next = new Inst3.JumpCond32_b_nb();
            return RESULT_JUMP;
        };

        /* JZ */
        ops[0x274] = prev -> {
            prev.next = new Inst3.JumpCond32_b_z();
            return RESULT_JUMP;
        };

        /* JNZ */
        ops[0x275] = prev -> {
            prev.next = new Inst3.JumpCond32_b_nz();
            return RESULT_JUMP;
        };

        /* JBE */
        ops[0x276] = prev -> {
            prev.next = new Inst3.JumpCond32_b_be();
            return RESULT_JUMP;
        };

        /* JNBE */
        ops[0x277] = prev -> {
            prev.next = new Inst3.JumpCond32_b_nbe();
            return RESULT_JUMP;
        };

        /* JS */
        ops[0x278] = prev -> {
            prev.next = new Inst3.JumpCond32_b_s();
            return RESULT_JUMP;
        };

        /* JNS */
        ops[0x279] = prev -> {
            prev.next = new Inst3.JumpCond32_b_ns();
            return RESULT_JUMP;
        };

        /* JP */
        ops[0x27a] = prev -> {
            prev.next = new Inst3.JumpCond32_b_p();
            return RESULT_JUMP;
        };

        /* JNP */
        ops[0x27b] = prev -> {
            prev.next = new Inst3.JumpCond32_b_np();
            return RESULT_JUMP;
        };

        /* JL */
        ops[0x27c] = prev -> {
            prev.next = new Inst3.JumpCond32_b_l();
            return RESULT_JUMP;
        };

        /* JNL */
        ops[0x27d] = prev -> {
            prev.next = new Inst3.JumpCond32_b_nl();
            return RESULT_JUMP;
        };

        /* JLE */
        ops[0x27e] = prev -> {
            prev.next = new Inst3.JumpCond32_b_le();
            return RESULT_JUMP;
        };

        /* JNLE */
        ops[0x27f] = prev -> {
            prev.next = new Inst3.JumpCond32_b_nle();
            return RESULT_JUMP;
        };

        /* Grpl Ed,Id */
        ops[0x281] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            if (rm>= 0xc0) {
                switch (which) {
                case 0x00:prev.next = new Inst3.GrplEdId_reg_add(rm, false);break;
                case 0x01:prev.next = new Inst3.GrplEdId_reg_or(rm, false);break;
                case 0x02:prev.next = new Inst3.GrplEdId_reg_adc(rm, false);break;
                case 0x03:prev.next = new Inst3.GrplEdId_reg_sbb(rm, false);break;
                case 0x04:prev.next = new Inst3.GrplEdId_reg_and(rm, false);break;
                case 0x05:prev.next = new Inst3.GrplEdId_reg_sub(rm, false);break;
                case 0x06:prev.next = new Inst3.GrplEdId_reg_xor(rm, false);break;
                case 0x07:prev.next = new Inst3.GrplEdId_reg_cmp(rm, false);break;
                }
            } else {
                switch (which) {
                case 0x00:prev.next = new Inst3.GrplEdId_mem_add(rm, false);break;
                case 0x01:prev.next = new Inst3.GrplEdId_mem_or(rm, false);break;
                case 0x02:prev.next = new Inst3.GrplEdId_mem_adc(rm, false);break;
                case 0x03:prev.next = new Inst3.GrplEdId_mem_sbb(rm, false);break;
                case 0x04:prev.next = new Inst3.GrplEdId_mem_and(rm, false);break;
                case 0x05:prev.next = new Inst3.GrplEdId_mem_sub(rm, false);break;
                case 0x06:prev.next = new Inst3.GrplEdId_mem_xor(rm, false);break;
                case 0x07:prev.next = new Inst3.GrplEdId_mem_cmp(rm, false);break;
                }
            }
            return RESULT_HANDLED;
        };

        /* Grpl Ed,Ix */
        ops[0x283] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            if (rm>= 0xc0) {
                switch (which) {
                case 0x00:prev.next = new Inst3.GrplEdId_reg_add(rm, true);break;
                case 0x01:prev.next = new Inst3.GrplEdId_reg_or(rm, true);break;
                case 0x02:prev.next = new Inst3.GrplEdId_reg_adc(rm, true);break;
                case 0x03:prev.next = new Inst3.GrplEdId_reg_sbb(rm, true);break;
                case 0x04:prev.next = new Inst3.GrplEdId_reg_and(rm, true);break;
                case 0x05:prev.next = new Inst3.GrplEdId_reg_sub(rm, true);break;
                case 0x06:prev.next = new Inst3.GrplEdId_reg_xor(rm, true);break;
                case 0x07:prev.next = new Inst3.GrplEdId_reg_cmp(rm, true);break;
                }
            } else {
                switch (which) {
                case 0x00:prev.next = new Inst3.GrplEdId_mem_add(rm, true);break;
                case 0x01:prev.next = new Inst3.GrplEdId_mem_or(rm, true);break;
                case 0x02:prev.next = new Inst3.GrplEdId_mem_adc(rm, true);break;
                case 0x03:prev.next = new Inst3.GrplEdId_mem_sbb(rm, true);break;
                case 0x04:prev.next = new Inst3.GrplEdId_mem_and(rm, true);break;
                case 0x05:prev.next = new Inst3.GrplEdId_mem_sub(rm, true);break;
                case 0x06:prev.next = new Inst3.GrplEdId_mem_xor(rm, true);break;
                case 0x07:prev.next = new Inst3.GrplEdId_mem_cmp(rm, true);break;
                }
            }
            return RESULT_HANDLED;
        };

        /* TEST Ed,Gd */
        ops[0x285] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.TestEdGd_reg(rm);
            } else {
                prev.next = new Inst3.TestEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* XCHG Ed,Gd */
        ops[0x287] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.XchgEdGd_reg(rm);
            } else {
                prev.next = new Inst3.XchgEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOV Ed,Gd */
        ops[0x289] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.MovEdGd_reg(rm);
            } else {
                prev.next = new Inst3.MovEdGd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOV Gd,Ed */
        ops[0x28b] = prev -> {
            int rm = decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.MovGdEd_reg(rm);
            } else {
                prev.next = new Inst3.MovGdEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* Mov Ew,Sw */
        ops[0x28c] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            switch (which) {
            case 0x00:					/* MOV Ew,ES */
                if (rm >= 0xC0) {
                    prev.next = new Inst3.MovEdEs_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwEs_mem(rm);
                }
                break;
            case 0x01:					/* MOV Ew,CS */
                if (rm >= 0xC0) {
                    prev.next = new Inst3.MovEdCs_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwCs_mem(rm);
                }
                break;
            case 0x02:					/* MOV Ew,SS */
                if (rm >= 0xC0) {
                    prev.next = new Inst3.MovEdSs_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwSs_mem(rm);
                }
                break;
            case 0x03:					/* MOV Ew,DS */
                if (rm >= 0xC0) {
                    prev.next = new Inst3.MovEdDs_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwDs_mem(rm);
                }
                break;
            case 0x04:					/* MOV Ew,FS */
                if (rm >= 0xC0) {
                    prev.next = new Inst3.MovEdFs_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwFs_mem(rm);
                }
                break;
            case 0x05:					/* MOV Ew,GS */
                if (rm >= 0xC0) {
                    prev.next = new Inst3.MovEdGs_reg(rm);
                } else {
                    prev.next = new Inst1.MovEwGs_mem(rm);
                }
                break;
            default:
                prev.next = new Inst1.Illegal("CPU:28c:Illegal RM Byte");
                return RESULT_JUMP;
            }
            return RESULT_HANDLED;
        };

        /* LEA Gd */
        ops[0x28d] = prev -> {
            int rm=decode_fetchb();
            if ((prefixes & Core.PREFIX_ADDR)!=0) {
                prev.next = new Inst3.LeaGd_32(rm);
            } else {
                prev.next = new Inst3.LeaGd_16(rm);
            }
            return RESULT_HANDLED;
        };

        /* POP Ed */
        ops[0x28f] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0 ) {
                prev.next = new Inst3.PopEd_reg(rm);
            } else {
                prev.next = new Inst3.PopEd_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* XCHG ECX,EAX */
        ops[0x291] = prev -> {
            prev.next = new Inst3.XchgEax(reg_ecx);
            return RESULT_HANDLED;
        };

        /* XCHG EDX,EAX */
        ops[0x292] = prev -> {
            prev.next = new Inst3.XchgEax(reg_edx);
            return RESULT_HANDLED;
        };

        /* XCHG EBX,EAX */
        ops[0x293] = prev -> {
            prev.next = new Inst3.XchgEax(reg_ebx);
            return RESULT_HANDLED;
        };

        /* XCHG ESP,EAX */
        ops[0x294] = prev -> {
            prev.next = new Inst3.XchgEax(reg_esp);
            return RESULT_HANDLED;
        };

        /* XCHG EBP,EAX */
        ops[0x295] = prev -> {
            prev.next = new Inst3.XchgEax(reg_ebp);
            return RESULT_HANDLED;
        };

        /* XCHG ESI,EAX */
        ops[0x296] = prev -> {
            prev.next = new Inst3.XchgEax(reg_esi);
            return RESULT_HANDLED;
        };

        /* XCHG EDI,EAX */
        ops[0x297] = prev -> {
            prev.next = new Inst3.XchgEax(reg_edi);
            return RESULT_HANDLED;
        };

        /* CWDE */
        ops[0x298] = prev -> {
            prev.next = new Inst3.Cwde();
            return RESULT_HANDLED;
        };

        /* CDQ */
        ops[0x299] = prev -> {
            prev.next = new Inst3.Cdq();
            return RESULT_HANDLED;
        };

        /* CALL FAR Ad */
        ops[0x29a] = prev -> {
            int newip= decode_fetchd();
            int newcs=decode_fetchw();
            prev.next = new Inst3.CallFarAp(newcs, newip);
            return RESULT_JUMP;
        };

        /* PUSHFD */
        ops[0x29c] = prev -> {
            prev.next = new Inst3.Pushfd();
            return RESULT_HANDLED;
        };

        /* POPFD */
        ops[0x29d] = prev -> {
            prev.next = new Inst3.Popfd();
            return RESULT_HANDLED;
        };

        /* MOV EAX,Od */
        ops[0x2a1] = prev -> {
            prev.next = new Inst3.MovEaxOd();
            return RESULT_HANDLED;
        };

        /* MOV Od,EAX */
        ops[0x2a3] = prev -> {
            prev.next = new Inst3.MovOdEax();
            return RESULT_HANDLED;
        };

        /* MOVSD */
        ops[0x2a5] = prev -> {
            if ((prefixes & Core.PREFIX_ADDR)==0) {
                if ((prefixes & Core.PREFIX_REP)==0) {
                    prev.next = new Strings.Movsd16();
                } else {
                    prev.next = new Strings.Movsd16r();
                }
            } else {
                if ((prefixes & Core.PREFIX_REP)==0) {
                    prev.next = new Strings.Movsd32();
                } else {
                    prev.next = new Strings.Movsd32r();
                }
            }
            return RESULT_HANDLED;
        };

        /* CMPSD */
        ops[0x2a7] = prev -> {
            prev.next = new Inst1.DoString(StringOp.R_CMPSD);
            return RESULT_HANDLED;
        };

        /* TEST EAX,Id */
        ops[0x2a9] = prev -> {
            prev.next = new Inst3.TestEaxId();
            return RESULT_HANDLED;
        };

        /* STOSD */
        ops[0x2ab] = prev -> {
            prev.next = new Inst1.DoString(StringOp.R_STOSD);
            return RESULT_HANDLED;
        };

        /* LODSD */
        ops[0x2ad] = prev -> {
            prev.next = new Inst1.DoString(StringOp.R_LODSD);
            return RESULT_HANDLED;
        };

        /* SCASD */
        ops[0x2af] = prev -> {
            prev.next = new Inst1.DoString(StringOp.R_SCASD);
            return RESULT_HANDLED;
        };

        /* MOV EAX,Id */
        ops[0x2b8] = prev -> {
            prev.next = new Inst3.MovId(reg_eax);
            return RESULT_HANDLED;
        };

        /* MOV ECX,Id */
        ops[0x2b9] = prev -> {
            prev.next = new Inst3.MovId(reg_ecx);
            return RESULT_HANDLED;
        };

        /* MOV EDX,Iw */
        ops[0x2ba] = prev -> {
            prev.next = new Inst3.MovId(reg_edx);
            return RESULT_HANDLED;
        };

        /* MOV EBX,Id */
        ops[0x2bb] = prev -> {
            prev.next = new Inst3.MovId(reg_ebx);
            return RESULT_HANDLED;
        };

        /* MOV ESP,Id */
        ops[0x2bc] = prev -> {
            prev.next = new Inst3.MovId(reg_esp);
            return RESULT_HANDLED;
        };

        /* MOV EBP,Id */
        ops[0x2bd] = prev -> {
            prev.next = new Inst3.MovId(reg_ebp);
            return RESULT_HANDLED;
        };

        /* MOV ESI,Id */
        ops[0x2be] = prev -> {
            prev.next = new Inst3.MovId(reg_esi);
            return RESULT_HANDLED;
        };

        /* MOV EDI,Id */
        ops[0x2bf] = prev -> {
            prev.next = new Inst3.MovId(reg_edi);
            return RESULT_HANDLED;
        };

        /* GRP2 Ed,Ib */
        ops[0x2c1] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            if (rm >= 0xc0) {
                int val = decode_fetchb() & 0x1f;
                if (val==0) {
                    prev.next = new Inst1.Noop();
                    return RESULT_HANDLED;
                }
                switch (which)	{
                case 0x00:prev.next = new Grp2.ROLD_reg(rm, val);break;
                case 0x01:prev.next = new Grp2.RORD_reg(rm, val);break;
                case 0x02:prev.next = new Grp2.RCLD_reg(rm, val);break;
                case 0x03:prev.next = new Grp2.RCRD_reg(rm, val);break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:prev.next = new Grp2.SHLD_reg(rm, val);break;
                case 0x05:prev.next = new Grp2.SHRD_reg(rm, val);break;
                case 0x07:prev.next = new Grp2.SARD_reg(rm, val);break;
                }
            } else {
                EaaBase get_eaa= Mod.getEaa(rm);
                int val = decode_fetchb() & 0x1f;
                if (val==0) {
                    prev.next = new Inst1.Noop();
                    return RESULT_HANDLED;
                }
                switch (which) {
                case 0x00:prev.next = new Grp2.ROLD_mem(get_eaa, val);break;
                case 0x01:prev.next = new Grp2.RORD_mem(get_eaa, val);break;
                case 0x02:prev.next = new Grp2.RCLD_mem(get_eaa, val);break;
                case 0x03:prev.next = new Grp2.RCRD_mem(get_eaa, val);break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:prev.next = new Grp2.SHLD_mem(get_eaa, val);break;
                case 0x05:prev.next = new Grp2.SHRD_mem(get_eaa, val);break;
                case 0x07:prev.next = new Grp2.SARD_mem(get_eaa, val);break;
                }
            }
            return RESULT_HANDLED;
        };

        /* RETN Iw */
        ops[0x2c2] = prev -> {
            prev.next = new Inst3.Retn32Iw();
            return RESULT_JUMP;
        };

        /* RETN */
        ops[0x2c3] = prev -> {
            prev.next = new Inst3.Retn32();
            return RESULT_JUMP;
        };

        /* LES */
        ops[0x2c4] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst1.Illegal("");
            } else {
                prev.next = new Inst3.Les32(rm);
            }
            return RESULT_HANDLED;
        };

        /* LDS */
        ops[0x2c5] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst1.Illegal("");
            } else {
                prev.next = new Inst3.Lds32(rm);
            }
            return RESULT_HANDLED;
        };

        /* MOV Ed,Id */
        ops[0x2c7] = prev -> {
            int rm=decode_fetchb();
            if (rm >= 0xc0) {
                prev.next = new Inst3.MovId(Mod.ed(rm));
            } else {
                prev.next = new Inst3.MovId_mem(rm);
            }
            return RESULT_HANDLED;
        };

        /* ENTER Iw,Ib */
        ops[0x2c8] = prev -> {
            prev.next = new Inst3.Enter32IwIb();
            return RESULT_HANDLED;
        };

        /* LEAVE */
        ops[0x2c9] = prev -> {
            prev.next = new Inst3.Leave32();
            return RESULT_HANDLED;
        };

        /* RETF Iw */
        ops[0x2ca] = prev -> {
            prev.next = new Inst3.Retf32Iw();
            return RESULT_JUMP;
        };

        /* RETF */
        ops[0x2cb] = prev -> {
            prev.next = new Inst3.Retf32();
            return RESULT_JUMP;
        };

        /* IRET */
        ops[0x2cf] = prev -> {
            prev.next = new Inst3.IRet32();
            return RESULT_JUMP;
        };

        /* GRP2 Ed,1 */
        ops[0x2d1] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            int val = 1;
            if (rm >= 0xc0) {
                switch (which)	{
                case 0x00:prev.next = new Grp2.ROLD_reg(rm, val);break;
                case 0x01:prev.next = new Grp2.RORD_reg(rm, val);break;
                case 0x02:prev.next = new Grp2.RCLD_reg(rm, val);break;
                case 0x03:prev.next = new Grp2.RCRD_reg(rm, val);break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:prev.next = new Grp2.SHLD_reg(rm, val);break;
                case 0x05:prev.next = new Grp2.SHRD_reg(rm, val);break;
                case 0x07:prev.next = new Grp2.SARD_reg(rm, val);break;
                }
            } else {
                EaaBase get_eaa= Mod.getEaa(rm);
                switch (which) {
                case 0x00:prev.next = new Grp2.ROLD_mem(get_eaa, val);break;
                case 0x01:prev.next = new Grp2.RORD_mem(get_eaa, val);break;
                case 0x02:prev.next = new Grp2.RCLD_mem(get_eaa, val);break;
                case 0x03:prev.next = new Grp2.RCRD_mem(get_eaa, val);break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:prev.next = new Grp2.SHLD_mem(get_eaa, val);break;
                case 0x05:prev.next = new Grp2.SHRD_mem(get_eaa, val);break;
                case 0x07:prev.next = new Grp2.SARD_mem(get_eaa, val);break;
                }
            }
            return RESULT_HANDLED;
        };

        /* GRP2 Ed,CL */
        ops[0x2d3] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            if (rm >= 0xc0) {
                switch (which)	{
                case 0x00:prev.next = new Grp2.ROLD_reg_cl(rm);break;
                case 0x01:prev.next = new Grp2.RORD_reg_cl(rm);break;
                case 0x02:prev.next = new Grp2.RCLD_reg_cl(rm);break;
                case 0x03:prev.next = new Grp2.RCRD_reg_cl(rm);break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:prev.next = new Grp2.SHLD_reg_cl(rm);break;
                case 0x05:prev.next = new Grp2.SHRD_reg_cl(rm);break;
                case 0x07:prev.next = new Grp2.SARD_reg_cl(rm);break;
                }
            } else {
                EaaBase get_eaa= Mod.getEaa(rm);
                switch (which) {
                case 0x00:prev.next = new Grp2.ROLD_mem_cl(get_eaa);break;
                case 0x01:prev.next = new Grp2.RORD_mem_cl(get_eaa);break;
                case 0x02:prev.next = new Grp2.RCLD_mem_cl(get_eaa);break;
                case 0x03:prev.next = new Grp2.RCRD_mem_cl(get_eaa);break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:prev.next = new Grp2.SHLD_mem_cl(get_eaa);break;
                case 0x05:prev.next = new Grp2.SHRD_mem_cl(get_eaa);break;
                case 0x07:prev.next = new Grp2.SARD_mem_cl(get_eaa);break;
                }
            }
            return RESULT_HANDLED;
        };

        /* LOOPNZ */
        ops[0x2e0] = prev -> {
             if ((prefixes & Core.PREFIX_ADDR)!=0) {
                prev.next = new Inst3.Loopnz32();
            } else {
                prev.next = new Inst3.Loopnz16();
            }
            return RESULT_JUMP;
        };

        /* LOOPZ */
        ops[0x2e1] = prev -> {
            if ((prefixes & Core.PREFIX_ADDR)!=0) {
                prev.next = new Inst3.Loopz32();
            } else {
                prev.next = new Inst3.Loopz16();
            }
            return RESULT_JUMP;
        };

        /* LOOP */
        ops[0x2e2] = prev -> {
            if ((prefixes & Core.PREFIX_ADDR)!=0) {
                prev.next = new Inst3.Loop32();
            } else {
                prev.next = new Inst3.Loop16();
            }
            return RESULT_JUMP;
        };

        /* JCXZ */
        ops[0x2e3] = prev -> {
            prev.next = new Inst3.Jcxz(AddrMaskTable1[prefixes & Core.PREFIX_ADDR]);
            return RESULT_JUMP;
        };

        /* IN EAX,Ib */
        ops[0x2e5] = prev -> {
            prev.next = new Inst3.InEaxIb();
            return RESULT_HANDLED;
        };

        /* OUT Ib,EAX */
        ops[0x2e7] = prev -> {
            prev.next = new Inst3.OutEaxIb();
            return RESULT_HANDLED;
        };

        /* CALL Jd */
        ops[0x2e8] = prev -> {
            prev.next = new Inst3.CallJd();
            return RESULT_JUMP;
        };

        /* JMP Jd */
        ops[0x2e9] = prev -> {
            prev.next = new Inst3.JmpJd();
            return RESULT_JUMP;
        };

        /* JMP Ad */
        ops[0x2ea] = prev -> {
            prev.next = new Inst3.JmpAd();
            return RESULT_JUMP;
        };

        /* JMP Jb */
        ops[0x2eb] = prev -> {
            prev.next = new Inst3.JmpJb();
            return RESULT_JUMP;
        };

        /* IN EAX,DX */
        ops[0x2ed] = prev -> {
            prev.next = new Inst3.InEaxDx();
            return RESULT_HANDLED;
        };

        /* OUT DX,EAX */
        ops[0x2ef] = prev -> {
            prev.next = new Inst3.OutEaxDx();
            return RESULT_HANDLED;
        };

        /* GRP3 Ed(,Id) */
        ops[0x2f7] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            switch (which) {
            case 0x00:											/* TEST Ed,Id */
            case 0x01:											/* TEST Ed,Id Undocumented*/
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.Testd_reg(rm);
                }
                else {
                    prev.next = new Grp3.Testd_mem(rm);
                }
                break;
            case 0x02:											/* NOT Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.NotEd_reg(rm);
                } else {
                    prev.next = new Grp3.NotEd_mem(rm);
                }
                break;
            case 0x03:											/* NEG Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.NegEd_reg(rm);
                } else {
                    prev.next = new Grp3.NegEd_mem(rm);
                }
                break;
            case 0x04:											/* MUL EAX,Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.MulAxEd_reg(rm);
                } else {
                    prev.next = new Grp3.MulAxEd_mem(rm);
                }
                break;
            case 0x05:											/* IMUL EAX,Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.IMulAxEd_reg(rm);
                } else {
                    prev.next = new Grp3.IMulAxEd_mem(rm);
                }
                break;
            case 0x06:											/* DIV Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.DivAxEd_reg(rm);
                } else {
                    prev.next = new Grp3.DivAxEd_mem(rm);
                }
                break;
            case 0x07:											/* IDIV Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Grp3.IDivAxEd_reg(rm);
                } else {
                    prev.next = new Grp3.IDivAxEd_mem(rm);
                }
                break;
            }
            return RESULT_HANDLED;
        };

        /* GRP 5 Ed */
        ops[0x2ff] = prev -> {
            int rm=decode_fetchb();
            int which=(rm>>3)&7;
            switch (which) {
            case 0x00:											/* INC Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Inst3.Incd_reg(Mod.ed(rm));
                } else {
                    prev.next = new Inst3.Incd_mem(rm);
                }
                break;
            case 0x01:											/* DEC Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Inst3.Decd_reg(Mod.ed(rm));
                } else {
                    prev.next = new Inst3.Decd_mem(rm);
                }
                break;
            case 0x02:											/* CALL NEAR Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Inst3.CallNearEd_reg(rm);
                } else {
                    prev.next = new Inst3.CallNearEd_mem(rm);
                }
                return RESULT_JUMP;
            case 0x03:											/* CALL FAR Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst3.CallFarEd_mem(rm);
                }
                return RESULT_JUMP;
            case 0x04:											/* JMP NEAR Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Inst3.JmpNearEd_reg(rm);
                } else {
                    prev.next = new Inst3.JmpNearEd_mem(rm);
                }
                return RESULT_JUMP;
            case 0x05:											/* JMP FAR Ed */
                if (rm >= 0xc0 ) {
                    prev.next = new Inst1.Illegal("");
                } else {
                    prev.next = new Inst3.JmpFarEd_mem(rm);
                }
                return RESULT_JUMP;
            case 0x06:											/* Push Ed */
                if (rm >= 0xc0) {
                    prev.next = new Inst3.PushEd_reg(rm);
                } else {
                    prev.next = new Inst3.PushEd_mem(rm);
                }
                break;
            default:
                if (Log.level<= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU,LogSeverities.LOG_ERROR,"CPU:66:GRP5:Illegal call "+Integer.toString(which,16));
                prev.next = new Inst1.Illegal("");
                return RESULT_JUMP;
            }
            return RESULT_HANDLED;
        };
    }
}
