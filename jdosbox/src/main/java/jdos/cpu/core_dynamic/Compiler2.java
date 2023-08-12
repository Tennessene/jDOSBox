package jdos.cpu.core_dynamic;

import jdos.cpu.CPU_Regs;
import jdos.misc.Log;

public class Compiler2 extends Compiler {
    static boolean compile_op(Op op, int setFlags, StringBuilder method, String preException, Seg seg) {
        switch (op.c) {
            case 0x19a: // SETP
            case 0x39a:
                if (op instanceof Inst2.SETcc_reg_p) {
                    Inst2.SETcc_reg_p o = (Inst2.SETcc_reg_p) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_P()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_p) {
                    Inst2.SETcc_mem_p o = (Inst2.SETcc_mem_p) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_P()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x19b: // SETNP
            case 0x39b:
                if (op instanceof Inst2.SETcc_reg_np) {
                    Inst2.SETcc_reg_np o = (Inst2.SETcc_reg_np) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NP()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_np) {
                    Inst2.SETcc_mem_np o = (Inst2.SETcc_mem_np) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NP()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x19c: // SETL
            case 0x39c:
                if (op instanceof Inst2.SETcc_reg_l) {
                    Inst2.SETcc_reg_l o = (Inst2.SETcc_reg_l) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_L()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_l) {
                    Inst2.SETcc_mem_l o = (Inst2.SETcc_mem_l) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_L()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x19d: // SETNL
            case 0x39d:
                if (op instanceof Inst2.SETcc_reg_nl) {
                    Inst2.SETcc_reg_nl o = (Inst2.SETcc_reg_nl) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NL()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nl) {
                    Inst2.SETcc_mem_nl o = (Inst2.SETcc_mem_nl) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NL()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x19e: // SETLE
            case 0x39e:
                if (op instanceof Inst2.SETcc_reg_le) {
                    Inst2.SETcc_reg_le o = (Inst2.SETcc_reg_le) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_LE()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_le) {
                    Inst2.SETcc_mem_le o = (Inst2.SETcc_mem_le) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_LE()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x19f: // SETNLE
            case 0x39f:
                if (op instanceof Inst2.SETcc_reg_nle) {
                    Inst2.SETcc_reg_nle o = (Inst2.SETcc_reg_nle) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NLE()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nle) {
                    Inst2.SETcc_mem_nle o = (Inst2.SETcc_mem_nle) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NLE()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x1a0: // PUSH FS
                if (op instanceof Inst2.PushFS) {
                    Inst2.PushFS o = (Inst2.PushFS) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_fsVal.dword);");
                    return true;
                }
                break;
            case 0x1a1: // POP FS
                if (op instanceof Inst2.PopFS) {
                    Inst2.PopFS o = (Inst2.PopFS) op;
                    method.append("if (CPU.CPU_PopSegFS(false)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x1a2: // CPUID
            case 0x3a2:
                if (op instanceof Inst2.CPUID) {
                    Inst2.CPUID o = (Inst2.CPUID) op;
                    method.append("CPU.CPU_CPUID();");
                    return true;
                }
                break;
            case 0x1a3: // BT Ew,Gw
                if (op instanceof Inst2.BtEwGw_reg) {
                    Inst2.BtEwGw_reg o = (Inst2.BtEwGw_reg) op;
                    method.append("Flags.FillFlags();");
                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & (1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15)))!=0);");
                    return true;
                }
                if (op instanceof Inst2.BtEwGw_mem) {
                    Inst2.BtEwGw_mem o = (Inst2.BtEwGw_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    method.append("eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;");
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & (1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15)))!=0);");
                    return true;
                }
                break;
            case 0x1a4: // SHLD Ew,Gw,Ib
                if (op instanceof Inst2.ShldEwGwIb_reg) {
                    Inst2.ShldEwGwIb_reg o = (Inst2.ShldEwGwIb_reg) op;
                    nameSet16(o.earw, "Instructions.do_DSHLW(", nameGet16(o.rw),  ", ", String.valueOf(o.op3), ", ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShldEwGwIb_mem) {
                    Inst2.ShldEwGwIb_mem o = (Inst2.ShldEwGwIb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writew(method);
                    method.append("Instructions.do_DSHLW(");
                    method.append(nameGet16(o.rw));
                    method.append(", ");
                    method.append(o.op3);
                    method.append(", ");
                    memory_readw(method);
                    method.append("));");
                    return true;
                }
                break;
            case 0x1a5: // SHLD Ew,Gw,CL
                if (op instanceof Inst2.ShldEwGwCl_reg) {
                    Inst2.ShldEwGwCl_reg o = (Inst2.ShldEwGwCl_reg) op;
                    declareVal(method);
                    method.append("val  = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHLW(val))");
                    nameSet16(o.earw, "Instructions.do_DSHLW(", nameGet16(o.rw), ", val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShldEwGwCl_mem) {
                    Inst2.ShldEwGwCl_mem o = (Inst2.ShldEwGwCl_mem) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHLW(val)) {");
                    memory_start(o.get_eaa, seg, method);
                    memory_writew(method);
                    method.append("Instructions.do_DSHLW(");
                    nameGet16(o.rw, method);
                    method.append(", val, ");
                    memory_readw(method);
                    method.append("));");
                    return true;
                }
                break;
            case 0x1a8: // PUSH GS
                if (op instanceof Inst2.PushGS) {
                    Inst2.PushGS o = (Inst2.PushGS) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_gsVal.dword);");
                    return true;
                }
                break;
            case 0x1a9: // POP GS
                if (op instanceof Inst2.PopGS) {
                    Inst2.PopGS o = (Inst2.PopGS) op;
                    method.append("if (CPU.CPU_PopSegGS(false)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x1ab: // BTS Ew,Gw
                if (op instanceof Inst2.BtsEwGw_reg) {
                    Inst2.BtsEwGw_reg o = (Inst2.BtsEwGw_reg) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & val)!=0);");
                    nameSet16(o.earw, nameGet16(o.earw) + " | val", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtsEwGw_mem) {
                    Inst2.BtsEwGw_mem o = (Inst2.BtsEwGw_mem) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);");
                    memory_start(o.get_eaa, seg, method);
                    method.append("eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val2 & val)!=0);Memory.mem_writew(eaa,val2 | val);");
                    return true;
                }
                break;
            case 0x1ac: // SHRD Ew,Gw,Ib
                if (op instanceof Inst2.ShrdEwGwIb_reg) {
                    Inst2.ShrdEwGwIb_reg o = (Inst2.ShrdEwGwIb_reg) op;
                    nameSet16(o.earw, "Instructions.do_DSHRW(", nameGet16(o.rw), ", ", String.valueOf(o.op3), ", ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShrdEwGwIb_mem) {
                    Inst2.ShrdEwGwIb_mem o = (Inst2.ShrdEwGwIb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    method.append("Instructions.do_DSHRW(");
                    nameGet16(o.rw, method);
                    method.append(", ");
                    method.append(o.op3);
                    method.append(", ");
                    memory_readw(method);
                    method.append("));");
                    return true;
                }
                break;
            case 0x1ad: // SHRD Ew,Gw,CL
                if (op instanceof Inst2.ShrdEwGwCl_reg) {
                    Inst2.ShrdEwGwCl_reg o = (Inst2.ShrdEwGwCl_reg) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHRW(val))");
                    nameSet16(o.earw, "Instructions.do_DSHRW(", nameGet16(o.rw), ", val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShrdEwGwCl_mem) {
                    Inst2.ShrdEwGwCl_mem o = (Inst2.ShrdEwGwCl_mem) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHRW(val)) {");
                    memory_start(o.get_eaa, seg, method);
                    memory_writew(method);
                    method.append("Instructions.do_DSHRW(");
                    nameGet16(o.rw, method);
                    method.append(", val, ");
                    memory_readw(method);
                    method.append("));}");
                    return true;
                }
                break;
            case 0x1af: // IMUL Gw,Ew
                if (op instanceof Inst2.ImulGwEw_reg) {
                    Inst2.ImulGwEw_reg o = (Inst2.ImulGwEw_reg) op;
                    nameSet16(o.rw, "Instructions.DIMULW(", nameGet16(o.earw), ",", nameGet16(o.rw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ImulGwEw_mem) {
                    Inst2.ImulGwEw_mem o = (Inst2.ImulGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "Instructions.DIMULW(Memory.mem_readw(eaa),", nameGet16(o.rw), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b0: // cmpxchg Eb,Gb
            case 0x3b0:
                if (op instanceof Inst2.CmpxchgEbGb_reg) {
                    Inst2.CmpxchgEbGb_reg o = (Inst2.CmpxchgEbGb_reg) op;
                    method.append("Flags.FillFlags();if (CPU_Regs.reg_eax.low() == ");
                    method.append(nameGet8(o.earb));
                    method.append(") {");
                    method.append(nameSet8(o.earb, nameGet8(o.rb)));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {CPU_Regs.reg_eax.low(");
                    method.append(nameGet8(o.earb));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                if (op instanceof Inst2.CmpxchgEbGb_mem) {
                    Inst2.CmpxchgEbGb_mem o = (Inst2.CmpxchgEbGb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readb(eaa);if (CPU_Regs.reg_eax.low() == val) {Memory.mem_writeb(eaa,");
                    method.append(nameGet8(o.rb));
                    method.append(");CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {Memory.mem_writeb(eaa,val);CPU_Regs.reg_eax.low(val);CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                break;
            case 0x1b1: // cmpxchg Ew,Gw
                if (op instanceof Inst2.CmpxchgEwGw_reg) {
                    Inst2.CmpxchgEwGw_reg o = (Inst2.CmpxchgEwGw_reg) op;
                    method.append("Flags.FillFlags();if (CPU_Regs.reg_eax.word() == ");
                    method.append(nameGet16(o.earw));
                    method.append(") {");
                    nameSet16(o.earw, nameGet16(o.rw), method);
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {CPU_Regs.reg_eax.word(");
                    method.append(nameGet16(o.earw));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                if (op instanceof Inst2.CmpxchgEwGw_mem) {
                    Inst2.CmpxchgEwGw_mem o = (Inst2.CmpxchgEwGw_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readw(eaa);if (CPU_Regs.reg_eax.word() == val) {Memory.mem_writew(eaa,");
                    method.append(nameGet16(o.rw));
                    method.append(");CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {Memory.mem_writew(eaa,val);CPU_Regs.reg_eax.word(val);CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                break;
            case 0x1b2: // LSS Ew
                if (op instanceof Inst2.LssEw) {
                    Inst2.LssEw o = (Inst2.LssEw) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+2))){").append(preException).append("return RUNEXCEPTION();}");
                    nameSet16(o.rw, "Memory.mem_readw(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b3: // BTR Ew,Gw
                if (op instanceof Inst2.BtrEwGw_reg) {
                    Inst2.BtrEwGw_reg o = (Inst2.BtrEwGw_reg) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & val)!=0);");
                    nameSet16(o.earw, nameGet16(o.earw) + " & ~val", method);
                    method.append(";");
                    return false;
                }
                if (op instanceof Inst2.BtrEwGw_mem) {
                    Inst2.BtrEwGw_mem o = (Inst2.BtrEwGw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val2 & val)!=0);Memory.mem_writew(eaa,val2 & ~val);");
                    return true;
                }
                break;
            case 0x1b4: // LFS Ew
                if (op instanceof Inst2.LfsEw) {
                    Inst2.LfsEw o = (Inst2.LfsEw) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+2))){").append(preException).append("return RUNEXCEPTION();}");
                    nameSet16(o.rw, "Memory.mem_readw(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b5: // LGS Ew
                if (op instanceof Inst2.LgsEw) {
                    Inst2.LgsEw o = (Inst2.LgsEw) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+2))){").append(preException).append("return RUNEXCEPTION();}");
                    nameSet16(o.rw, "Memory.mem_readw(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b6: // MOVZX Gw,Eb
                if (op instanceof Inst2.MovzxGwEb_reg) {
                    Inst2.MovzxGwEb_reg o = (Inst2.MovzxGwEb_reg) op;
                    nameSet16(o.rw, nameGet8(o.earb), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.MovzxGwEb_mem) {
                    Inst2.MovzxGwEb_mem o = (Inst2.MovzxGwEb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "Memory.mem_readb(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b7: // MOVZX Gw,Ew
            case 0x1bf:  // MOVSX Gw,Ew
                if (op instanceof Inst2.MovzxGwEw_reg) {
                    Inst2.MovzxGwEw_reg o = (Inst2.MovzxGwEw_reg) op;
                    nameSet16(o.rw, nameGet16(o.earw), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.MovzxGwEw_mem) {
                    Inst2.MovzxGwEw_mem o = (Inst2.MovzxGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "Memory.mem_readw(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1ba: // GRP8 Ew,Ib
                if (op instanceof Inst2.BtEwIb_reg) {
                    Inst2.BtEwIb_reg o = (Inst2.BtEwIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    return true;
                }
                if (op instanceof Inst2.BtsEwIb_reg) {
                    Inst2.BtsEwIb_reg o = (Inst2.BtsEwIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    nameSet16(o.earw, nameGet16(o.earw), " | ", String.valueOf(o.mask), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtrEwIb_reg) {
                    Inst2.BtrEwIb_reg o = (Inst2.BtrEwIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    nameSet16(o.earw, nameGet16(o.earw), " & ~", String.valueOf(o.mask), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtcEwIb_reg) {
                    Inst2.BtcEwIb_reg o = (Inst2.BtcEwIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    nameSet16(o.earw, nameGet16(o.earw), " ^ ", String.valueOf(o.mask), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtEwIb_mem) {
                    Inst2.BtEwIb_mem o = (Inst2.BtEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Memory.mem_readw(eaa) & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    return true;
                }
                if (op instanceof Inst2.BtsEwIb_mem) {
                    Inst2.BtsEwIb_mem o = (Inst2.BtsEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writew(eaa,val|");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst2.BtrEwIb_mem) {
                    Inst2.BtrEwIb_mem o = (Inst2.BtrEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writew(eaa,val & ~");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst2.BtcEwIb_mem) {
                    Inst2.BtcEwIb_mem o = (Inst2.BtcEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writew(eaa,val ^ ");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                break;
            case 0x1bb: // BTC Ew,Gw
                if (op instanceof Inst2.BtcEwGw_reg) {
                    Inst2.BtcEwGw_reg o = (Inst2.BtcEwGw_reg) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & val)!=0);");
                    nameSet16(o.earw, nameGet16(o.earw) + " ^ val", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtcEwGw_mem) {
                    Inst2.BtcEwGw_mem o = (Inst2.BtcEwGw_mem) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);");
                    memory_start(o.get_eaa, seg, method);
                    method.append("eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val2 & val)!=0);Memory.mem_writew(eaa,val2 ^ val);");
                    return true;
                }
                break;
            case 0x1bc: // BSF Gw,Ew
                if (op instanceof Inst2.BsfGwEw_reg) {
                    Inst2.BsfGwEw_reg o = (Inst2.BsfGwEw_reg) op;
                    declareVal(method);
                    method.append("val=");
                    method.append(nameGet16(o.earw));
                    method.append(";if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0; while ((val & 0x01)==0) { result++; val>>=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    nameSet16(o.rw, "result", method);
                    method.append(";}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst2.BsfGwEw_mem) {
                    Inst2.BsfGwEw_mem o = (Inst2.BsfGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0;while ((val & 0x01)==0) { result++; val>>=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    nameSet16(o.rw, "result", method);
                    method.append(";}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x1bd: // BSR Gw,Ew
                if (op instanceof Inst2.BsrGwEw_reg) {
                    Inst2.BsrGwEw_reg o = (Inst2.BsrGwEw_reg) op;
                    declareVal(method);
                    method.append("val=");
                    method.append(nameGet16(o.earw));
                    method.append(";if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 15;while ((val & 0x8000)==0) { result--; val<<=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    nameSet16(o.rw, "result", method);
                    method.append(";}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst2.BsrGwEw_mem) {
                    Inst2.BsrGwEw_mem o = (Inst2.BsrGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 15;while ((val & 0x8000)==0) { result--; val<<=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    nameSet16(o.rw, "result", method);
                    method.append(";}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x1be: // MOVSX Gw,Eb
                if (op instanceof Inst2.MovsxGwEb_reg) {
                    Inst2.MovsxGwEb_reg o = (Inst2.MovsxGwEb_reg) op;
                    nameSet16(o.rw, "(byte)" + nameGet8(o.earb), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.MovsxGwEb_mem) {
                    Inst2.MovsxGwEb_mem o = (Inst2.MovsxGwEb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "(byte)Memory.mem_readb(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c0: // XADD Gb,Eb
            case 0x3c0:
                if (op instanceof Inst2.XaddGbEb_reg) {
                    Inst2.XaddGbEb_reg o = (Inst2.XaddGbEb_reg) op;
                    if ((setFlags & o.sets())==0) {
                        declareVal(method);
                        method.append("val = ");
                        method.append(nameGet8(o.rb));
                        method.append(";");
                        method.append(nameSet8(o.rb, nameGet8(o.earb)));
                        method.append(";");
                        method.append(nameSet8(o.earb, nameGet8(o.earb) + "+val"));
                        method.append(";");
                    } else {
                        declareVal(method);
                        method.append("val=Instructions.ADDB(");
                        method.append(nameGet8(o.rb));
                        method.append(", ");
                        method.append(nameGet8(o.earb));
                        method.append(");");
                        method.append(nameSet8(o.rb, nameGet8(o.earb)));
                        method.append(";");
                        method.append(nameSet8(o.earb, "val"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Inst2.XaddGbEb_mem) {
                    Inst2.XaddGbEb_mem o = (Inst2.XaddGbEb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        declareVal(method);
                        method.append("val = Memory.mem_readb(eaa);Memory.mem_writeb(eaa,val+");method.append(nameGet8(o.rb));method.append(");");
                        method.append(nameSet8(o.rb, "val"));
                        method.append(";");
                    } else {
                        declareVal(method);
                        method.append("val = Memory.mem_readb(eaa);");
                        method.append("Memory.mem_writeb(eaa,");method.append("Instructions.ADDB(");method.append(nameGet8(o.rb));method.append(", val)");method.append(");");
                        method.append(nameSet8(o.rb, "val"));
                        method.append(";");
                    }
                    return true;
                }
                break;
            case 0x1c1: // XADD Gw,Ew
                if (op instanceof Inst2.XaddGwEw_reg) {
                    Inst2.XaddGwEw_reg o = (Inst2.XaddGwEw_reg) op;
                    if ((setFlags & o.sets())==0) {
                        declareVal(method);
                        method.append("val=");
                        method.append(nameGet16(o.rw));
                        method.append(";");
                        nameSet16(o.rw, nameGet16(o.earw), method);
                        method.append(";");
                        nameSet16(o.earw, nameGet16(o.earw) + "+val", method);
                        method.append(";");
                    } else {
                        declareVal(method);
                        method.append("val=Instructions.ADDW(");
                        method.append(nameGet16(o.rw));
                        method.append(", ");
                        method.append(nameGet16(o.earw));
                        method.append(");");
                        nameSet16(o.rw, nameGet16(o.earw), method);
                        method.append(";");
                        nameSet16(o.earw, "val", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Inst2.XaddGwEw_mem) {
                    Inst2.XaddGwEw_mem o = (Inst2.XaddGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        declareVal(method);
                        method.append("val = Memory.mem_readw(eaa);Memory.mem_writew(eaa,val+");method.append(nameGet16(o.rw));method.append(");");
                        nameSet16(o.rw, "val", method);
                        method.append(";");
                    } else {
                        declareVal(method);
                        method.append("val = Memory.mem_readw(eaa);");
                        declareVal2(method);
                        method.append("val2 = Instructions.ADDW(");
                        method.append(nameGet16(o.rw));
                        method.append(", val);Memory.mem_writew(eaa,val2);");
                        nameSet16(o.rw, "val", method);
                        method.append(";");
                    }
                    return true;
                }
                break;
            case 0x1c8: // BSWAP AX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c9: // BSWAP CX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1ca: // BSWAP DX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cb: // BSWAP BX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cc: // BSWAP SP
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cd: // BSWAP BP
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1ce: // BSWAP SI
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cf: // BSWAP DI
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    nameSet16(o.reg, "Instructions.BSWAPW(", nameGet16(o.reg), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x201: // ADD Ed,Gd
                if (op instanceof Inst3.Addd_reg) {
                    Inst3.Addd_reg o = (Inst3.Addd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "", "ADDD", method);
                    return true;
                }
                if (op instanceof Inst3.AddEdGd_mem) {
                    Inst3.AddEdGd_mem o = (Inst3.AddEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "", "ADDD", seg, method);
                    return true;
                }
                break;
            case 0x203: // ADD Gd,Ed
                if (op instanceof Inst3.Addd_reg) {
                    Inst3.Addd_reg o = (Inst3.Addd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "", "ADDD", method);
                    return true;
                }
                if (op instanceof Inst3.AddGdEd_mem) {
                    Inst3.AddGdEd_mem o = (Inst3.AddGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "", "ADDD", seg, method);
                    return true;
                }
                break;
            case 0x205: // ADD EAX,Id
                if (op instanceof Inst3.AddEaxId) {
                    Inst3.AddEaxId o = (Inst3.AddEaxId) op;
                    instructionAI((setFlags & o.sets()) == 0, 32, o.i, "+", "", "ADDD", method);
                    return true;
                }
                break;
            case 0x206: // PUSH ES
                if (op instanceof Inst3.Push32ES) {
                    Inst3.Push32ES o = (Inst3.Push32ES) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_esVal.dword);");
                    return true;
                }
                break;
            case 0x207: // POP ES
                if (op instanceof Inst3.Pop32ES) {
                    Inst3.Pop32ES o = (Inst3.Pop32ES) op;
                    method.append("if (CPU.CPU_PopSegES(true)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x209: // OR Ed,Gd
                if (op instanceof Inst3.Ord_reg) {
                    Inst3.Ord_reg o = (Inst3.Ord_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "|", "", "ORD", method);
                    return true;
                }
                if (op instanceof Inst3.OrEdGd_mem) {
                    Inst3.OrEdGd_mem o = (Inst3.OrEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "|", "", "ORD", seg, method);
                    return true;
                }
                break;
            case 0x20b: // OR Gd,Ed
                if (op instanceof Inst3.Ord_reg) {
                    Inst3.Ord_reg o = (Inst3.Ord_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "|", "", "ORD", method);
                    return true;
                }
                if (op instanceof Inst3.OrGdEd_mem) {
                    Inst3.OrGdEd_mem o = (Inst3.OrGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "|", "", "ORD", seg, method);
                    return true;
                }
                break;
            case 0x20d: // OR EAX,Id
                if (op instanceof Inst3.OrEaxId) {
                    Inst3.OrEaxId o = (Inst3.OrEaxId) op;
                    instructionAI((setFlags & o.sets()) == 0, 32, o.i, "|", "", "ORD", method);
                    return true;
                }
                break;
            case 0x20e: // PUSH CS
                if (op instanceof Inst3.Push32CS) {
                    Inst3.Push32CS o = (Inst3.Push32CS) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_csVal.dword);");
                    return true;
                }
                break;
            case 0x211: // ADC Ed,Gd
                if (op instanceof Inst3.Adcd_reg) {
                    Inst3.Adcd_reg o = (Inst3.Adcd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCD", method);
                    return true;
                }
                if (op instanceof Inst3.AdcEdGd_mem) {
                    Inst3.AdcEdGd_mem o = (Inst3.AdcEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "+(("+getB()+")?1:0)", "ADCD", seg, method);
                    return true;
                }
                break;
            case 0x213: // ADC Gd,Ed
                if (op instanceof Inst3.Adcd_reg) {
                    Inst3.Adcd_reg o = (Inst3.Adcd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "+(("+getB()+")?1:0)", "ADCD", method);
                    return true;
                }
                if (op instanceof Inst3.AdcGdEd_mem) {
                    Inst3.AdcGdEd_mem o = (Inst3.AdcGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "+", "+(("+getB()+")?1:0)", "ADCD", seg, method);
                    return true;
                }
                break;
            case 0x215: // ADC EAX,Id
                if (op instanceof Inst3.AdcEaxId) {
                    Inst3.AdcEaxId o = (Inst3.AdcEaxId) op;
                    instructionAI((setFlags & o.sets())==0, 32, o.i, "+", "+(("+getB()+")?1:0)", "ADCD", method);
                    return true;
                }
                break;
            case 0x216: // PUSH SS
                if (op instanceof Inst3.Push32SS) {
                    Inst3.Push32SS o = (Inst3.Push32SS) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_ssVal.dword);");
                    return true;
                }
                break;
            case 0x217: // POP SS
                if (op instanceof Inst3.Pop32SS) {
                    Inst3.Pop32SS o = (Inst3.Pop32SS) op;
                    method.append("if (CPU.CPU_PopSegSS(true)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x219: // SBB Ed,Gd
                if (op instanceof Inst3.Sbbd_reg) {
                    Inst3.Sbbd_reg o = (Inst3.Sbbd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "-(("+getB()+")?1:0)", "SBBD", method);
                    return true;
                }
                if (op instanceof Inst3.SbbEdGd_mem) {
                    Inst3.SbbEdGd_mem o = (Inst3.SbbEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "-(("+getB()+")?1:0)", "SBBD", seg, method);
                    return true;
                }
                break;
            case 0x21b: // SBB Gd,Ed
                if (op instanceof Inst3.Sbbd_reg) {
                    Inst3.Sbbd_reg o = (Inst3.Sbbd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "-(("+getB()+")?1:0)", "SBBD", method);
                    return true;
                }
                if (op instanceof Inst3.SbbGdEd_mem) {
                    Inst3.SbbGdEd_mem o = (Inst3.SbbGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "-(("+getB()+")?1:0)", "SBBD", seg, method);
                    return true;
                }
                break;
            case 0x21d: // SBB EAX,Id
                if (op instanceof Inst3.SbbEaxId) {
                    Inst3.SbbEaxId o = (Inst3.SbbEaxId) op;
                    instructionAI((setFlags & o.sets()) == 0, 32, o.i, "-", "-(("+getB()+")?1:0)", "SBBD", method);
                    return true;
                }
                break;
            case 0x21e: // PUSH DS
                if (op instanceof Inst3.Push32DS) {
                    Inst3.Push32DS o = (Inst3.Push32DS) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_dsVal.dword);");
                    return true;
                }
                break;
            case 0x21f: // POP DS
                if (op instanceof Inst3.Pop32DS) {
                    Inst3.Pop32DS o = (Inst3.Pop32DS) op;
                    method.append("if (CPU.CPU_PopSegDS(true)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x221: // AND Ed,Gd
                if (op instanceof Inst3.Andd_reg) {
                    Inst3.Andd_reg o = (Inst3.Andd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "&", "", "ANDD", method);
                    return true;
                }
                if (op instanceof Inst3.AndEdGd_mem) {
                    Inst3.AndEdGd_mem o = (Inst3.AndEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "&", "", "ANDD", seg, method);
                    return true;
                }
                break;
            case 0x223: // AND Gd,Ed
                if (op instanceof Inst3.Andd_reg) {
                    Inst3.Andd_reg o = (Inst3.Andd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "&", "", "ANDD", method);
                    return true;
                }
                if (op instanceof Inst3.AndGdEd_mem) {
                    Inst3.AndGdEd_mem o = (Inst3.AndGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "&", "", "ANDD", seg, method);
                    return true;
                }
                break;
            case 0x225: // AND EAX,Id
                if (op instanceof Inst3.AndEaxId) {
                    Inst3.AndEaxId o = (Inst3.AndEaxId) op;
                    instructionAI((setFlags & o.sets()) == 0, 32, o.i, "&", "", "ANDD", method);
                    return true;
                }
                break;
            case 0x229: // SUB Ed,Gd
                if (op instanceof Inst3.Subd_reg) {
                    Inst3.Subd_reg o = (Inst3.Subd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "", "SUBD", method);
                    return true;
                }
                if (op instanceof Inst3.SubEdGd_mem) {
                    Inst3.SubEdGd_mem o = (Inst3.SubEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "", "SUBD", seg, method);
                    return true;
                }
                break;
            case 0x22b: // SUB Gd,Ed
                if (op instanceof Inst3.Subd_reg) {
                    Inst3.Subd_reg o = (Inst3.Subd_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "", "SUBD", method);
                    return true;
                }
                if (op instanceof Inst3.SubGdEd_mem) {
                    Inst3.SubGdEd_mem o = (Inst3.SubGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "-", "", "SUBD", seg, method);
                    return true;
                }
                break;
            case 0x22d: // SUB EAX,Id
                if (op instanceof Inst3.SubEaxId) {
                    Inst3.SubEaxId o = (Inst3.SubEaxId) op;
                    instructionAI((setFlags & o.sets()) == 0, 32, o.i, "-", "", "SUBD", method);
                    return true;
                }
                break;
            case 0x231: // XOR Ed,Gd
                if (op instanceof Inst3.Xord_reg) {
                    Inst3.Xord_reg o = (Inst3.Xord_reg) op;
                    boolean fast = (setFlags & o.sets())==0;
                    if (fast && o.e == o.g) {
                        nameSet32(o.e, method);
                        method.append("=0;");
                    } else {
                        instructionEG(fast, 32, o.e, o.g, "^", "", "XORD", method);
                    }
                    return true;
                }
                if (op instanceof Inst3.XorEdGd_mem) {
                    Inst3.XorEdGd_mem o = (Inst3.XorEdGd_mem) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "^", "", "XORD", seg, method);
                    return true;
                }
                break;
            case 0x233: // XOR Gd,Ed
                if (op instanceof Inst3.Xord_reg) {
                    Inst3.Xord_reg o = (Inst3.Xord_reg) op;
                    instructionEG((setFlags & o.sets()) == 0, 32, o.e, o.g, "^", "", "XORD", method);
                    return true;
                }
                if (op instanceof Inst3.XorGdEd_mem) {
                    Inst3.XorGdEd_mem o = (Inst3.XorGdEd_mem) op;
                    instructionGE((setFlags & o.sets()) == 0, 32, o.e, o.g, "^", "", "XORD", seg, method);
                    return true;
                }
                break;
            case 0x235: // XOR EAX,Id
                if (op instanceof Inst3.XorEaxId) {
                    Inst3.XorEaxId o = (Inst3.XorEaxId) op;
                    instructionAI((setFlags & o.sets()) == 0, 32, o.i, "^", "", "XORD", method);
                    return true;
                }
                break;
            case 0x239: // CMP Ed,Gd
                if (op instanceof Inst3.Cmpd_reg) {
                    Inst3.Cmpd_reg o = (Inst3.Cmpd_reg) op;
                    method.append("Instructions.CMPD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.CmpEdGd_mem) {
                    Inst3.CmpEdGd_mem o = (Inst3.CmpEdGd_mem) op;
                    memory_start(o.e, seg, method);
                    method.append("Instructions.CMPD(");
                    method.append(nameGet32(o.g));
                    method.append(", Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x23b: // CMP Gd,Ed
                if (op instanceof Inst3.Cmpd_reg) {
                    Inst3.Cmpd_reg o = (Inst3.Cmpd_reg) op;
                    method.append("Instructions.CMPD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.CmpGdEd_mem) {
                    Inst3.CmpGdEd_mem o = (Inst3.CmpGdEd_mem) op;
                    memory_start(o.g, seg, method);
                    method.append("Instructions.CMPD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x23d: // CMP EAX,Id
                if (op instanceof Inst3.CmpEaxId) {
                    Inst3.CmpEaxId o = (Inst3.CmpEaxId) op;
                    method.append("Instructions.CMPD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x240: // INC EAX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x241: // INC ECX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x242: // INC EDX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x243: // INC EBX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x244: // INC ESP
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x245: // INC EBP
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x246: // INC ESI
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x247: // INC EDI
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x248: // DEC EAX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x249: // DEC ECX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x24a: // DEC EDX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x24b: // DEC EBX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x24c: // DEC ESP
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x24d: // DEC EBP
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x24e: // DEC ESI
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x24f: // DEC EDI
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                break;
            case 0x250: // PUSH EAX
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x251: // PUSH ECX
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x252: // PUSH EDX
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x253: // PUSH EBX
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x254: // PUSH ESP
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x255: // PUSH EBP
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x256: // PUSH ESI
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x257: // PUSH EDI
                if (op instanceof Inst3.Push32_reg) {
                    Inst3.Push32_reg o = (Inst3.Push32_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x258: // POP EAX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x259: // POP ECX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25a: // POP EDX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25b: // POP EBX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25c: // POP ESP
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25d: // POP EBP
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25e: // POP ESI
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25f: // POP EDI
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameSet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x260: // PUSHAD
                if (op instanceof Inst3.Pushad) {
                    Inst3.Pushad o = (Inst3.Pushad) op;
                    declareVal2(method);
                    method.append("val2 = CPU_Regs.reg_esp.dword;");
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_esp.dword;val = CPU.CPU_Push32(val, CPU_Regs.reg_eax.dword);val = CPU.CPU_Push32(val, CPU_Regs.reg_ecx.dword);val = CPU.CPU_Push32(val, CPU_Regs.reg_edx.dword);val = CPU.CPU_Push32(val, CPU_Regs.reg_ebx.dword);val = CPU.CPU_Push32(val, val2);val = CPU.CPU_Push32(val, ");nameGet32(CPU_Regs.reg_ebp, method);method.append(");val = CPU.CPU_Push32(val, CPU_Regs.reg_esi.dword);val = CPU.CPU_Push32(val, CPU_Regs.reg_edi.dword);CPU_Regs.reg_esp.dword=val;");
                    return true;
                }
                break;
            case 0x261: // POPAD
                if (op instanceof Inst3.Popad) {
                    Inst3.Popad o = (Inst3.Popad) op;
                    method.append("CPU_Regs.reg_edi.dword=CPU.CPU_Pop32();CPU_Regs.reg_esi.dword=CPU.CPU_Pop32();CPU_Regs.reg_ebp.dword=CPU.CPU_Pop32();CPU.CPU_Pop32();CPU_Regs.reg_ebx.dword=CPU.CPU_Pop32();CPU_Regs.reg_edx.dword=CPU.CPU_Pop32();CPU_Regs.reg_ecx.dword=CPU.CPU_Pop32();CPU_Regs.reg_eax.dword=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x262: // BOUND Ed
                if (op instanceof Inst3.BoundEd) {
                    Inst3.BoundEd o = (Inst3.BoundEd) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (");method.append(nameGet32(o.rd));method.append(" < Memory.mem_readd(eaa) || ");method.append(nameGet32(o.rd));method.append(" > Memory.mem_readd(eaa + 4) {return EXCEPTION(5);}");
                    return true;
                }
                break;
            case 0x263: // ARPL Ed,Rd
                if (op instanceof Inst3.ArplEdRd_reg) {
                    Inst3.ArplEdRd_reg o = (Inst3.ArplEdRd_reg) op;
                    method.append("if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    declareVal(method);
                    method.append("val=");
                    method.append(nameGet32(o.eard));
                    method.append(";val=CPU.CPU_ARPL(val, ");
                    method.append(nameGet16(o.rd));
                    method.append(");");
                    method.append(nameSet32(o.eard));
                    method.append("=val;");
                    return true;
                }
                if (op instanceof Inst3.ArplEdRd_mem) {
                    Inst3.ArplEdRd_mem o = (Inst3.ArplEdRd_mem) op;
                    method.append("if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);");
                    method.append("val=CPU.CPU_ARPL(val, ");
                    method.append(nameGet16(o.rd));
                    method.append(");Memory.mem_writed(eaa,val);");
                    return true;
                }
                break;
            case 0x268: // PUSH Id
                if (op instanceof Inst3.PushId) {
                    Inst3.PushId o = (Inst3.PushId) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(o.id);
                    method.append(");");
                    return true;
                }
                break;
            case 0x269: // IMUL Gd,Ed,Id
                if (op instanceof Inst3.ImulGdEdId_reg) {
                    Inst3.ImulGdEdId_reg o = (Inst3.ImulGdEdId_reg) op;
                    method.append(nameSet32(o.rd));
                    if ((setFlags & o.sets())==0) {
                        method.append("=");
                        method.append(o.op3);
                        method.append("*");
                        method.append(nameGet32(o.eard));
                        method.append(";");
                    } else {
                        method.append("=Instructions.DIMULD(");
                        method.append(nameGet32(o.eard));
                        method.append(", ");
                        method.append(o.op3);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Inst3.ImulGdEdId_mem) {
                    Inst3.ImulGdEdId_mem o = (Inst3.ImulGdEdId_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append(nameSet32(o.rd));
                    if ((setFlags & o.sets())==0) {
                        method.append("=");
                        method.append(o.op3);
                        method.append("*Memory.mem_readd(eaa);");
                    } else {
                        method.append("=Instructions.DIMULD(Memory.mem_readd(eaa),");
                        method.append(o.op3);
                        method.append(");");
                    }
                    return true;
                }
                break;
            case 0x26a: // PUSH Ib
                if (op instanceof Inst3.PushIb) {
                    Inst3.PushIb o = (Inst3.PushIb) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(o.id);
                    method.append(");");
                    return true;
                }
                break;
            case 0x26b: // IMUL Gd,Ed,Ib
                if (op instanceof Inst3.ImulGdEdIb_reg) {
                    Inst3.ImulGdEdIb_reg o = (Inst3.ImulGdEdIb_reg) op;
                    method.append(nameSet32(o.rd));
                    method.append("=Instructions.DIMULD(");
                    method.append(nameGet32(o.eard));
                    method.append(", ");
                    method.append(o.op3);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.ImulGdEdIb_mem) {
                    Inst3.ImulGdEdIb_mem o = (Inst3.ImulGdEdIb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append(nameSet32(o.rd));
                    method.append("=Instructions.DIMULD(Memory.mem_readd(eaa),");
                    method.append(o.op3);
                    method.append(");");
                    return true;
                }
                break;
            case 0x26d: // INSD
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")){").append(preException).append("return RUNEXCEPTION();}Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x26f: // OUTSD
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")){").append(preException).append("return RUNEXCEPTION();}Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x270: // JO
                if (op instanceof Inst3.JumpCond32_b_o) {
                    Inst3.JumpCond32_b_o o = (Inst3.JumpCond32_b_o) op;
                    compile(o, getO(), method);
                    return false;
                }
                break;
            case 0x271: // JNO
                if (op instanceof Inst3.JumpCond32_b_no) {
                    Inst3.JumpCond32_b_no o = (Inst3.JumpCond32_b_no) op;
                    compile(o, getNO(), method);
                    return false;
                }
                break;
            case 0x272: // JB
                if (op instanceof Inst3.JumpCond32_b_b) {
                    Inst3.JumpCond32_b_b o = (Inst3.JumpCond32_b_b) op;
                    compile(o, getB(), method);
                    return false;
                }
                break;
            case 0x273: // JNB
                if (op instanceof Inst3.JumpCond32_b_nb) {
                    Inst3.JumpCond32_b_nb o = (Inst3.JumpCond32_b_nb) op;
                    compile(o, getNB(), method);
                    return false;
                }
                break;
            case 0x274: // JZ
                if (op instanceof Inst3.JumpCond32_b_z) {
                    Inst3.JumpCond32_b_z o = (Inst3.JumpCond32_b_z) op;
                    compile(o, getZ(), method);
                    return false;
                }
                break;
            case 0x275: // JNZ
                if (op instanceof Inst3.JumpCond32_b_nz) {
                    Inst3.JumpCond32_b_nz o = (Inst3.JumpCond32_b_nz) op;
                    compile(o, getNZ(), method);
                    return false;
                }
                break;
            case 0x276: // JBE
                if (op instanceof Inst3.JumpCond32_b_be) {
                    Inst3.JumpCond32_b_be o = (Inst3.JumpCond32_b_be) op;
                    compile(o, getBE(), method);
                    return false;
                }
                break;
            case 0x277: // JNBE
                if (op instanceof Inst3.JumpCond32_b_nbe) {
                    Inst3.JumpCond32_b_nbe o = (Inst3.JumpCond32_b_nbe) op;
                    compile(o, getNBE(), method);
                    return false;
                }
                break;
            case 0x278: // JS
                if (op instanceof Inst3.JumpCond32_b_s) {
                    Inst3.JumpCond32_b_s o = (Inst3.JumpCond32_b_s) op;
                    compile(o, getS(), method);
                    return false;
                }
                break;
            case 0x279: // JNS
                if (op instanceof Inst3.JumpCond32_b_ns) {
                    Inst3.JumpCond32_b_ns o = (Inst3.JumpCond32_b_ns) op;
                    compile(o, getNS(), method);
                    return false;
                }
                break;
            case 0x27a: // JP
                if (op instanceof Inst3.JumpCond32_b_p) {
                    Inst3.JumpCond32_b_p o = (Inst3.JumpCond32_b_p) op;
                    compile(o, getP(), method);
                    return false;
                }
                break;
            case 0x27b: // JNP
                if (op instanceof Inst3.JumpCond32_b_np) {
                    Inst3.JumpCond32_b_np o = (Inst3.JumpCond32_b_np) op;
                    compile(o, getNP(), method);
                    return false;
                }
                break;
            case 0x27c: // JL
                if (op instanceof Inst3.JumpCond32_b_l) {
                    Inst3.JumpCond32_b_l o = (Inst3.JumpCond32_b_l) op;
                    compile(o, getL(), method);
                    return false;
                }
                break;
            case 0x27d: // JNL
                if (op instanceof Inst3.JumpCond32_b_nl) {
                    Inst3.JumpCond32_b_nl o = (Inst3.JumpCond32_b_nl) op;
                    compile(o, getNL(), method);
                    return false;
                }
                break;
            case 0x27e: // JLE
                if (op instanceof Inst3.JumpCond32_b_le) {
                    Inst3.JumpCond32_b_le o = (Inst3.JumpCond32_b_le) op;
                    compile(o, getLE(), method);
                    return false;
                }
                break;
            case 0x27f: // JNLE
                if (op instanceof Inst3.JumpCond32_b_nle) {
                    Inst3.JumpCond32_b_nle o = (Inst3.JumpCond32_b_nle) op;
                    compile(o, getNLE(), method);
                    return false;
                }
                break;
            case 0x281: // Grpl Ed,Id
                if (op instanceof Inst3.GrplEdId_reg_add) {
                    Inst3.GrplEdId_reg_add o = (Inst3.GrplEdId_reg_add) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "+", "", "ADDD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_or) {
                    Inst3.GrplEdId_reg_or o = (Inst3.GrplEdId_reg_or) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "|", "", "ORD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_adc) {
                    Inst3.GrplEdId_reg_adc o = (Inst3.GrplEdId_reg_adc) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "+", "+(("+getB()+")?1:0)", "ADCD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sbb) {
                    Inst3.GrplEdId_reg_sbb o = (Inst3.GrplEdId_reg_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "-", "-(("+getB()+")?1:0)", "SBBD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_and) {
                    Inst3.GrplEdId_reg_and o = (Inst3.GrplEdId_reg_and) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "&", "", "ANDD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sub) {
                    Inst3.GrplEdId_reg_sub o = (Inst3.GrplEdId_reg_sub) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "-", "", "SUBD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_xor) {
                    Inst3.GrplEdId_reg_xor o = (Inst3.GrplEdId_reg_xor) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "^", "", "XORD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_cmp) {
                    Inst3.GrplEdId_reg_cmp o = (Inst3.GrplEdId_reg_cmp) op;
                    method.append("Instructions.CMPD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_add) {
                    Inst3.GrplEdId_mem_add o = (Inst3.GrplEdId_mem_add) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "+", "", "ADDD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_or) {
                    Inst3.GrplEdId_mem_or o = (Inst3.GrplEdId_mem_or) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "|", "", "ORD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_adc) {
                    Inst3.GrplEdId_mem_adc o = (Inst3.GrplEdId_mem_adc) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "+", "+(("+getB()+")?1:0)", "ADCD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sbb) {
                    Inst3.GrplEdId_mem_sbb o = (Inst3.GrplEdId_mem_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "-", "-(("+getB()+")?1:0)", "SBBD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_and) {
                    Inst3.GrplEdId_mem_and o = (Inst3.GrplEdId_mem_and) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "&", "", "ANDD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sub) {
                    Inst3.GrplEdId_mem_sub o = (Inst3.GrplEdId_mem_sub) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "-", "", "SUBD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_xor) {
                    Inst3.GrplEdId_mem_xor o = (Inst3.GrplEdId_mem_xor) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "^", "", "XORD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_cmp) {
                    Inst3.GrplEdId_mem_cmp o = (Inst3.GrplEdId_mem_cmp) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.CMPD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x283: // Grpl Ed,Ix
                if (op instanceof Inst3.GrplEdId_reg_add) {
                    Inst3.GrplEdId_reg_add o = (Inst3.GrplEdId_reg_add) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "+", "", "ADDD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_or) {
                    Inst3.GrplEdId_reg_or o = (Inst3.GrplEdId_reg_or) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "|", "", "ORD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_adc) {
                    Inst3.GrplEdId_reg_adc o = (Inst3.GrplEdId_reg_adc) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "+", "+(("+getB()+")?1:0)", "ADCD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sbb) {
                    Inst3.GrplEdId_reg_sbb o = (Inst3.GrplEdId_reg_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "-", "-(("+getB()+")?1:0)", "SBBD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_and) {
                    Inst3.GrplEdId_reg_and o = (Inst3.GrplEdId_reg_and) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "&", "", "ANDD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sub) {
                    Inst3.GrplEdId_reg_sub o = (Inst3.GrplEdId_reg_sub) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "-", "", "SUBD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_xor) {
                    Inst3.GrplEdId_reg_xor o = (Inst3.GrplEdId_reg_xor) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.eard, new Reg(o.ib), "^", "", "XORD", method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_cmp) {
                    Inst3.GrplEdId_reg_cmp o = (Inst3.GrplEdId_reg_cmp) op;
                    method.append("Instructions.CMPD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_add) {
                    Inst3.GrplEdId_mem_add o = (Inst3.GrplEdId_mem_add) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "+", "", "ADDD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_or) {
                    Inst3.GrplEdId_mem_or o = (Inst3.GrplEdId_mem_or) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "|", "", "ORD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_adc) {
                    Inst3.GrplEdId_mem_adc o = (Inst3.GrplEdId_mem_adc) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "+", "+(("+getB()+")?1:0)", "ADCD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sbb) {
                    Inst3.GrplEdId_mem_sbb o = (Inst3.GrplEdId_mem_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "-", "-(("+getB()+")?1:0)", "SBBD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_and) {
                    Inst3.GrplEdId_mem_and o = (Inst3.GrplEdId_mem_and) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "&", "", "ANDD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sub) {
                    Inst3.GrplEdId_mem_sub o = (Inst3.GrplEdId_mem_sub) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "-", "", "SUBD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_xor) {
                    Inst3.GrplEdId_mem_xor o = (Inst3.GrplEdId_mem_xor) op;
                    instructionEG((setFlags & o.sets())==0, 32, o.get_eaa, new Reg(o.ib), "^", "", "XORD", seg, method);
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_cmp) {
                    Inst3.GrplEdId_mem_cmp o = (Inst3.GrplEdId_mem_cmp) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.CMPD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x285: // TEST Ed,Gd
                if (op instanceof Inst3.TestEdGd_reg) {
                    Inst3.TestEdGd_reg o = (Inst3.TestEdGd_reg) op;
                    method.append("Instructions.TESTD(");
                    method.append(nameGet32(o.rd));
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.TestEdGd_mem) {
                    Inst3.TestEdGd_mem o = (Inst3.TestEdGd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.TESTD(");
                    method.append(nameGet32(o.rd));
                    method.append(" ,Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x287: // XCHG Ed,Gd
                if (op instanceof Inst3.XchgEdGd_reg) {
                    Inst3.XchgEdGd_reg o = (Inst3.XchgEdGd_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet32(o.rd));
                    method.append(";");
                    method.append(nameSet32(o.rd));
                    method.append("=");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    method.append(nameSet32(o.eard));
                    method.append("=val;");
                    return true;
                }
                if (op instanceof Inst3.XchgEdGd_mem) {
                    Inst3.XchgEdGd_mem o = (Inst3.XchgEdGd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readd(eaa);Memory.mem_writed(eaa, ");method.append(nameGet32(o.rd));method.append(");");
                    method.append(nameSet32(o.rd));
                    method.append("=val;");
                    return true;
                }
                break;
            case 0x289: // MOV Ed,Gd
                if (op instanceof Inst3.MovEdGd_reg) {
                    Inst3.MovEdGd_reg o = (Inst3.MovEdGd_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    method.append(nameGet32(o.rd));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.MovEdGd_mem) {
                    Inst3.MovEdGd_mem o = (Inst3.MovEdGd_mem) op;
                    method.append("Memory.mem_writed(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", ");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                break;
            case 0x28b: // MOV Gd,Ed
                if (op instanceof Inst3.MovGdEd_reg) {
                    Inst3.MovGdEd_reg o = (Inst3.MovGdEd_reg) op;
                    method.append(nameSet32(o.rd));
                    method.append("=");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.MovGdEd_mem) {
                    Inst3.MovGdEd_mem o = (Inst3.MovGdEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append(nameSet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x28c: // Mov Ew,Sw
                if (op instanceof Inst3.MovEdEs_reg) {
                    Inst3.MovEdEs_reg o = (Inst3.MovEdEs_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU_Regs.reg_esVal.dword & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwEs_mem) {
                    Inst1.MovEwEs_mem o = (Inst1.MovEwEs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_esVal.dword);");
                    return true;
                }
                if (op instanceof Inst3.MovEdCs_reg) {
                    Inst3.MovEdCs_reg o = (Inst3.MovEdCs_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU_Regs.reg_csVal.dword & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwCs_mem) {
                    Inst1.MovEwCs_mem o = (Inst1.MovEwCs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_csVal.dword);");
                    return true;
                }
                if (op instanceof Inst3.MovEdSs_reg) {
                    Inst3.MovEdSs_reg o = (Inst3.MovEdSs_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU_Regs.reg_ssVal.dword & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwSs_mem) {
                    Inst1.MovEwSs_mem o = (Inst1.MovEwSs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_ssVal.dword);");
                    return true;
                }
                if (op instanceof Inst3.MovEdDs_reg) {
                    Inst3.MovEdDs_reg o = (Inst3.MovEdDs_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU_Regs.reg_dsVal.dword & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwDs_mem) {
                    Inst1.MovEwDs_mem o = (Inst1.MovEwDs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_dsVal.dword);");
                    return true;
                }
                if (op instanceof Inst3.MovEdFs_reg) {
                    Inst3.MovEdFs_reg o = (Inst3.MovEdFs_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU_Regs.reg_fsVal.dword & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwFs_mem) {
                    Inst1.MovEwFs_mem o = (Inst1.MovEwFs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_fsVal.dword);");
                    return true;
                }
                if (op instanceof Inst3.MovEdGs_reg) {
                    Inst3.MovEdGs_reg o = (Inst3.MovEdGs_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU_Regs.reg_gsVal.dword & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwGs_mem) {
                    Inst1.MovEwGs_mem o = (Inst1.MovEwGs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_gsVal.dword);");
                    return true;
                }
                break;
            case 0x28d: // LEA Gd
                if (op instanceof Inst3.LeaGd_32) {
                    Inst3.LeaGd_32 o = (Inst3.LeaGd_32) op;
                    method.append(nameSet32(o.rd));
                    method.append("=");
                    toStringValue(o.get_eaa, seg, method, true);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.LeaGd_16) {
                    Inst3.LeaGd_16 o = (Inst3.LeaGd_16) op;
                    method.append(nameSet32(o.rd));
                    method.append("=");
                    toStringValue(o.get_eaa, seg, method, true);
                    method.append(";");
                    return true;
                }
                break;
            case 0x28f: // POP Ed
                if (op instanceof Inst3.PopEd_reg) {
                    Inst3.PopEd_reg o = (Inst3.PopEd_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                if (op instanceof Inst3.PopEd_mem) {
                    Inst3.PopEd_mem o = (Inst3.PopEd_mem) op;
                    declareVal(method);
                    method.append("val = CPU.CPU_Pop32();");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, val);");
                    return true;
                }
                break;
            case 0x291: // XCHG ECX,EAX
            case 0x292: // XCHG EDX,EAX
            case 0x293: // XCHG EBX,EAX
            case 0x294: // XCHG ESP,EAX
            case 0x295: // XCHG EBP,EAX
            case 0x296: // XCHG ESI,EAX
            case 0x297: // XCHG EDI,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameSet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=val;");
                    return true;
                }
                break;
            case 0x298: // CWDE
                if (op instanceof Inst3.Cwde) {
                    Inst3.Cwde o = (Inst3.Cwde) op;
                    method.append("CPU_Regs.reg_eax.dword=(short)CPU_Regs.reg_eax.word();");
                    return true;
                }
                break;
            case 0x299: // CDQ
                if (op instanceof Inst3.Cdq) {
                    Inst3.Cdq o = (Inst3.Cdq) op;
                    method.append("if ((CPU_Regs.reg_eax.dword & 0x80000000)!=0) CPU_Regs.reg_edx.dword=0xffffffff;else CPU_Regs.reg_edx.dword=0;");
                    return true;
                }
                break;
            case 0x29a: // CALL FAR Ad
                if (op instanceof Inst3.CallFarAp) {
                    Inst3.CallFarAp o = (Inst3.CallFarAp) op;
                    method.append("Flags.FillFlags();CPU.CPU_CALL(true,");
                    method.append(o.newcs);
                    method.append(",");
                    method.append(o.newip);
                    method.append(", CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x29c: // PUSHFD
                if (op instanceof Inst3.Pushfd) {
                    Inst3.Pushfd o = (Inst3.Pushfd) op;
                    method.append("if (CPU.CPU_PUSHF(true)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x29d: // POPFD
                if (op instanceof Inst3.Popfd) {
                    Inst3.Popfd o = (Inst3.Popfd) op;
                    method.append("if (CPU.CPU_POPF(true)){").append(preException).append("return RUNEXCEPTION();}");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return DECODE_END(");
                        method.append(o.eip_count);
                        method.append(");}");
                    }
                    if (CPU_PIC_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END(");
                        method.append(o.eip_count);
                        method.append(");");
                    }
                    return true;
                }
                break;
            case 0x2a1: // MOV EAX,Od
                if (op instanceof Inst3.MovEaxOd) {
                    Inst3.MovEaxOd o = (Inst3.MovEaxOd) op;
                    method.append("CPU_Regs.reg_eax.dword=Memory.mem_readd(");method.append(seg.getDs());method.append("+");
                    method.append(o.value);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2a3: // MOV Od,EAX
                if (op instanceof Inst3.MovOdEax) {
                    Inst3.MovOdEax o = (Inst3.MovOdEax) op;
                    method.append(" Memory.mem_writed(");method.append(seg.getDs());method.append("+");
                    method.append(o.value);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x2a5: // MOVSD
                method.append("Core.base_ds=");
                method.append(seg.getDs());
                method.append(";");
                if (op instanceof Strings.Movsd16) {
                    method.append("Strings.Movsd16.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsd16r) {
                    method.append("Strings.Movsd16r.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsd32) {
                    method.append("Strings.Movsd32.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsd32r) {
                    method.append("Strings.Movsd32r.doString();");
                    return true;
                }
                break;
            case 0x2a7: // CMPSD
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2a9: // TEST EAX,Id
                if (op instanceof Inst3.TestEaxId) {
                    Inst3.TestEaxId o = (Inst3.TestEaxId) op;
                    method.append("Instructions.TESTD(");
                    method.append(o.id);
                    method.append(",CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x2ab: // STOSD
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2ad: // LODSD
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2af: // SCASD
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2b8: // MOV EAX,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2b9: // MOV ECX,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2ba: // MOV EDX,Iw
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bb: // MOV EBX,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bc: // MOV ESP,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bd: // MOV EBP,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2be: // MOV ESI,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bf: // MOV EDI,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2c1: // GRP2 Ed,Ib
                if (op instanceof Grp2.ROLD_reg) {
                    Grp2.ROLD_reg o = (Grp2.ROLD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // (Ed << Ib) | (Ed >>> (32-Ib)
                        method.append("(");
                        nameGet32(o.eard, method);
                        method.append(" << ");
                        method.append(o.val);
                        method.append(") | (");
                        nameGet32(o.eard, method);
                        method.append(" >>> ");
                        method.append(32 - o.val);
                        method.append(");");
                    } else {
                        method.append("Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_reg) {
                    Grp2.RORD_reg o = (Grp2.RORD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> Ib) | (Ed << (32-Ib))
                        method.append("(");
                        nameGet32(o.eard, method);
                        method.append(" >>> ");
                        method.append(o.val);
                        method.append(") | (");
                        nameGet32(o.eard, method);
                        method.append(" << ");
                        method.append(32 - o.val);
                        method.append(");");
                    } else {
                        method.append("Instructions.RORD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_reg) {
                    Grp2.RCLD_reg o = (Grp2.RCLD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        if (o.val==1) {
                            // (Ed << 1) | cf
                            method.append("(");
                            nameGet32(o.eard, method);
                            method.append(" << 1) | (Flags.get_CF()?1:0);");
                        } else {
                            // (Ed << Ib) |(cf << (Ib-1)) | (Ed >>> (33-Ib));
                            method.append("(");
                            nameGet32(o.eard, method);
                            method.append(" << ");
                            method.append(o.val);
                            method.append(") | ((Flags.get_CF()?1:0) << ");
                            method.append(o.val-1);
                            method.append(") | (");
                            nameGet32(o.eard, method);
                            method.append(" >>> ");
                            method.append(33-o.val);
                            method.append(");");
                        }
                    } else {
                        method.append("Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_reg) {
                    Grp2.RCRD_reg o = (Grp2.RCRD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        if (o.val==1) {
                            // (Ed >>> 1 | cf << 31)
                            method.append("(");
                            nameGet32(o.eard, method);
                            method.append(" >>> 1) | ((Flags.get_CF()?1:0) << 31);");
                        } else {
                            // (Ed >>> Ib) | (cf << (32-Ib)) | (Ed << (33-Ib))
                            method.append("(");
                            nameGet32(o.eard, method);
                            method.append(" >>> ");
                            method.append(o.val);
                            method.append(") | ((Flags.get_CF()?1:0) << ");
                            method.append(32-o.val);
                            method.append(") | (");
                            nameGet32(o.eard, method);
                            method.append(" << ");
                            method.append(33-o.val);
                            method.append(");");
                        }
                    } else {
                        method.append("Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_reg) {
                    Grp2.SHLD_reg o = (Grp2.SHLD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // Ed << Ib
                        nameGet32(o.eard, method);
                        method.append(" << ");
                        method.append(o.val);
                        method.append(";");
                    } else {
                        method.append("Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_reg) {
                    Grp2.SHRD_reg o = (Grp2.SHRD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // Ed >>> Ib
                        nameGet32(o.eard, method);
                        method.append(" >>> ");
                        method.append(o.val);
                        method.append(";");
                    } else {
                        method.append("Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_reg) {
                    Grp2.SARD_reg o = (Grp2.SARD_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // Ed >> Ib
                        nameGet32(o.eard, method);
                        method.append(" >> ");
                        method.append(o.val);
                        method.append(";");
                    } else {
                        method.append("Instructions.SARD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLD_mem) {
                    Grp2.ROLD_mem o = (Grp2.ROLD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        // (Ed << Ib) | (Ed >>> (32-Ib))
                        declareVal(method);
                        method.append("val = ");
                        memory_readd(method);
                        method.append(";");
                        memory_writed(method);
                        method.append("(val << ");
                        method.append(o.val);
                        method.append(") | (val >>> ");
                        method.append(32-o.val);
                        method.append("));");
                    } else {
                        memory_writed(method);
                        method.append("Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_mem) {
                    Grp2.RORD_mem o = (Grp2.RORD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> Ib) | (Ed << (32-Ib))
                        declareVal(method);
                        method.append("val = ");
                        memory_readd(method);
                        method.append(";");
                        memory_writed(method);
                        method.append("(val >>> ");
                        method.append(o.val);
                        method.append(") | (val << ");
                        method.append(32-o.val);
                        method.append("));");
                    } else {
                        memory_writed(method);
                        method.append("Instructions.RORD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_mem) {
                    Grp2.RCLD_mem o = (Grp2.RCLD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        // if (Ib==1)
                        //     (Ed << 1) | cf
                        // else
                        //     (Ed << Ib) |(cf << (Ib-1)) | (Ed >>> (33-Ib))
                        if (o.val == 1) {
                            memory_writed(method);
                            method.append("(");
                            memory_readd(method);
                            method.append(" << 1) | (Flags.get_CF()?1:0));");
                        } else {
                            declareVal(method);
                            method.append("val = ");
                            memory_readd(method);
                            method.append(";");
                            memory_writed(method);
                            method.append("(val << ");
                            method.append(o.val);
                            method.append(") | ((Flags.get_CF()?1:0) << ");
                            method.append(o.val-1);
                            method.append(") | (val >>> ");
                            method.append(33-o.val);
                            method.append("));");
                        }
                    } else {
                        memory_writed(method);
                        method.append("Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_mem) {
                    Grp2.RCRD_mem o = (Grp2.RCRD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        // if (val==1)
                        //     (Ed >>> 1 | cf << 31)
                        // else
                        //     (Ed >>> Ib) | (cf << (32-Ib)) | (Ed << (33-Ib))
                        if (o.val == 1) {
                            memory_writed(method);
                            method.append("(");
                            memory_readd(method);
                            method.append(" >>> 1) | ((Flags.get_CF()?1:0) << 31));");
                        } else {
                            declareVal(method);
                            method.append("val = ");
                            memory_readd(method);
                            method.append(";");
                            memory_writed(method);
                            method.append("(val >>> ");
                            method.append(o.val);
                            method.append(") | ((Flags.get_CF()?1:0) << ");
                            method.append(32-o.val);
                            method.append(") | (val << ");
                            method.append(33-o.val);
                            method.append("));");
                        }
                    } else {
                        memory_writed(method);
                        method.append("Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_mem) {
                    Grp2.SHLD_mem o = (Grp2.SHLD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // Ed << Ib
                        memory_readd(method);
                        method.append(" << ");
                        method.append(o.val);
                        method.append(");");
                    } else {
                        method.append("Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_mem) {
                    Grp2.SHRD_mem o = (Grp2.SHRD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // Ed >>> Ib
                        memory_readd(method);
                        method.append(" >>> ");
                        method.append(o.val);
                        method.append(");");
                    } else {
                        method.append("Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_mem) {
                    Grp2.SARD_mem o = (Grp2.SARD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // Ed >> Ib
                        memory_readd(method);
                        method.append(" >> ");
                        method.append(o.val);
                        method.append(");");
                    } else {
                        method.append("Instructions.SARD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                break;
            case 0x2c2: // RETN Iw
                if (op instanceof Inst3.Retn32Iw) {
                    Inst3.Retn32Iw o = (Inst3.Retn32Iw) op;
                    method.append("CPU_Regs.reg_eip=CPU.CPU_Pop32();CPU_Regs.reg_esp.dword=CPU_Regs.reg_esp.dword+");
                    method.append(o.offset);
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x2c3: // RETN
                if (op instanceof Inst3.Retn32) {
                    Inst3.Retn32 o = (Inst3.Retn32) op;
                    method.append("CPU_Regs.reg_eip=CPU.CPU_Pop32();return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x2c4: // LES
                if (op instanceof Inst3.Les32) {
                    Inst3.Les32 o = (Inst3.Les32) op;
                    memory_start(o.get_eaa, seg, method);
                    // make sure all reads are done before writing something in case of a PF
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+4))){").append(preException).append("return RUNEXCEPTION();}");
                    method.append(nameSet32(o.rd));
                    method.append("=val;");
                    return true;
                }
                break;
            case 0x2c5: // LDS
                if (op instanceof Inst3.Lds32) {
                    Inst3.Lds32 o = (Inst3.Lds32) op;
                    memory_start(o.get_eaa, seg, method);
                    // make sure all reads are done before writing something in case of a PF
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa); if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+4))){").append(preException).append("return RUNEXCEPTION();}");
                    method.append(nameSet32(o.rd));
                    method.append("=val;");
                    return true;
                }
                break;
            case 0x2c7: // MOV Ed,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameSet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.MovId_mem) {
                    Inst3.MovId_mem o = (Inst3.MovId_mem) op;
                    method.append("Memory.mem_writed(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", ");
                    method.append(o.id);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2c8: // ENTER Iw,Ib
                if (op instanceof Inst3.Enter32IwIb) {
                    Inst3.Enter32IwIb o = (Inst3.Enter32IwIb) op;
                    method.append("CPU.CPU_ENTER(true,");
                    method.append(o.bytes);
                    method.append(", ");
                    method.append(o.level);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2c9: // LEAVE
                if (op instanceof Inst3.Leave32) {
                    //Inst3.Leave32 o = (Inst3.Leave32) op;
                    method.append("CPU_Regs.reg_esp.dword&=CPU.cpu.stack.notmask;CPU_Regs.reg_esp.dword|=(");nameGet32(CPU_Regs.reg_ebp, method);method.append(" & CPU.cpu.stack.mask);CPU_Regs.reg_ebp.dword=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x2ca: // RETF Iw
                if (op instanceof Inst3.Retf32Iw) {
                    Inst3.Retf32Iw o = (Inst3.Retf32Iw) op;
                    method.append("Flags.FillFlags();CPU.CPU_RET(true,");
                    method.append(o.words);
                    method.append(", CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x2cb: // RETF
                if (op instanceof Inst3.Retf32) {
                    Inst3.Retf32 o = (Inst3.Retf32) op;
                    method.append("Flags.FillFlags();CPU.CPU_RET(true,0,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x2cf: // IRET
                if (op instanceof Inst3.IRet32) {
                    Inst3.IRet32 o = (Inst3.IRet32) op;
                    method.append("CPU.CPU_IRET(true, CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    if (CPU_PIC_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) return CB_NONE();");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x2d1: // GRP2 Ed,1
                if (op instanceof Grp2.ROLD_reg) {
                    Grp2.ROLD_reg o = (Grp2.ROLD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // (Ed << 1) | (Ed >>> (32-1))
                        nameGet32(o.eard, method);
                        method.append(" = (");
                        nameGet32(o.eard, method);
                        method.append(" << 1) | (");
                        nameGet32(o.eard, method);
                        method.append(" >>> 31);");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_reg) {
                    Grp2.RORD_reg o = (Grp2.RORD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> 1) | (Ed << (32-1))
                        nameGet32(o.eard, method);
                        method.append(" = (");
                        nameGet32(o.eard, method);
                        method.append(" >>> 1) | (");
                        nameGet32(o.eard, method);
                        method.append(" << 31);");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.RORD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_reg) {
                    Grp2.RCLD_reg o = (Grp2.RCLD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // (Ed << 1) | cf;
                        nameGet32(o.eard, method);
                        method.append(" = (");
                        nameGet32(o.eard, method);
                        method.append(" << 1) | (Flags.get_CF()?1:0);");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_reg) {
                    Grp2.RCRD_reg o = (Grp2.RCRD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> 1 | cf << 31)
                        nameGet32(o.eard, method);
                        method.append(" = (");
                        nameGet32(o.eard, method);
                        method.append(" >>> 1) | ((Flags.get_CF()?1:0) << 31);");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_reg) {
                    Grp2.SHLD_reg o = (Grp2.SHLD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // Ed << 1
                        nameGet32(o.eard, method);
                        method.append(" = ");
                        nameGet32(o.eard, method);
                        method.append(" << 1;");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_reg) {
                    Grp2.SHRD_reg o = (Grp2.SHRD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // Ed >>> 1
                        nameGet32(o.eard, method);
                        method.append(" = ");
                        nameGet32(o.eard, method);
                        method.append(" >>> 1;");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_reg) {
                    Grp2.SARD_reg o = (Grp2.SARD_reg) op;
                    if ((setFlags & o.sets())==0) {
                        // Ed >> 1
                        nameGet32(o.eard, method);
                        method.append(" = ");
                        nameGet32(o.eard, method);
                        method.append(" >> 1;");
                    } else {
                        nameGet32(o.eard, method);
                        method.append("=Instructions.SARD(");
                        method.append(o.val);
                        method.append(", ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLD_mem) {
                    Grp2.ROLD_mem o = (Grp2.ROLD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        // (Ed << 1) | (Ed >>> (32-1))
                        declareVal(method);
                        method.append("val = ");
                        memory_readd(method);
                        method.append(";");
                        memory_writed(method);
                        method.append("(val << 1) | (val >>> 31));");
                    } else {
                        memory_writed(method);
                        method.append("Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_mem) {
                    Grp2.RORD_mem o = (Grp2.RORD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> 1) | (Ed << (32-1))
                        declareVal(method);
                        method.append("val = ");
                        memory_readd(method);
                        method.append(";");
                        memory_writed(method);
                        method.append("(val >>> 1) | (val << 31));");
                    } else {
                        memory_writed(method);
                        method.append("Instructions.RORD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_mem) {
                    Grp2.RCLD_mem o = (Grp2.RCLD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // (Ed << 1) | cf;
                        method.append("(");
                        memory_readd(method);
                        method.append(" << 1) | (Flags.get_CF()?1:0));");
                    } else {
                        method.append("Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_mem) {
                    Grp2.RCRD_mem o = (Grp2.RCRD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> 1 | cf << 31)
                        method.append("(");
                        memory_readd(method);
                        method.append(" >>> 1) | ((Flags.get_CF()?1:0) << 31));");
                    } else {
                        method.append("Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_mem) {
                    Grp2.SHLD_mem o = (Grp2.SHLD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // Ed << 1
                        memory_readd(method);
                        method.append(" << 1);");
                    } else {
                        method.append("Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_mem) {
                    Grp2.SHRD_mem o = (Grp2.SHRD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // Ed >>> 1
                        memory_readd(method);
                        method.append(" >>> 1);");
                    } else {
                        method.append("Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_mem) {
                    Grp2.SARD_mem o = (Grp2.SARD_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    memory_writed(method);
                    if ((setFlags & o.sets())==0) {
                        // Ed >> 1
                        memory_readd(method);
                        method.append(" >> 1);");
                    } else {
                        method.append("Instructions.SARD(");
                        method.append(o.val);
                        method.append(", ");
                        memory_readd(method);
                        method.append("));");
                    }
                    return true;
                }
                break;
            case 0x2d3: // GRP2 Ed,CL
                if (op instanceof Grp2.ROLD_reg_cl) {
                    Grp2.ROLD_reg_cl o = (Grp2.ROLD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // (Ed << CL) | (Ed >>> (32-CL))
                        method.append("(");
                        nameGet32(o.eard, method);
                        method.append(" << val) | (");
                        nameGet32(o.eard, method);
                        method.append(" >>> (32-val));");
                    } else {
                        method.append("Instructions.ROLD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_reg_cl) {
                    Grp2.RORD_reg_cl o = (Grp2.RORD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> CL) | (Ed << (32-CL))
                        method.append("(");
                        nameGet32(o.eard, method);
                        method.append(" >>> val) | (");
                        nameGet32(o.eard, method);
                        method.append(" << (32-val));");
                    } else {
                        method.append("Instructions.RORD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_reg_cl) {
                    Grp2.RCLD_reg_cl o = (Grp2.RCLD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    nameGet32(o.eard, method);
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // if (val==1)
                        //     (Ed << 1) | cf
                        // else
                        //     (Ed << CL) |(cf << (CL-1)) | (Ed >>> (33-CL))
                        method.append("(");
                        nameGet32(o.eard, method);
                        method.append(" << val) | (val==1?(Flags.get_CF()?1:0):((Flags.get_CF()?1:0) << (val - 1)) | (");
                        nameGet32(o.eard, method);
                        method.append(" >>> (33-val)));");
                    } else {
                        method.append("Instructions.RCLD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_reg_cl) {
                    Grp2.RCRD_reg_cl o = (Grp2.RCRD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    nameGet32(o.eard, method);
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // if (val==1)
                        //     (Ed >>> 1 | cf << 31)
                        // else
                        //     (Ed >>> CL) | (cf << (32-CL)) | (Ed << (33-CL))
                        method.append("(");
                        nameGet32(o.eard, method);
                        method.append(" >>> val) | (val==1?((Flags.get_CF()?1:0) << 31):((Flags.get_CF()?1:0) << (32-val)) | (");
                        nameGet32(o.eard, method);
                        method.append(" << (33-val)));");
                    } else {
                        method.append("Instructions.RCRD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_reg_cl) {
                    Grp2.SHLD_reg_cl o = (Grp2.SHLD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    nameGet32(o.eard, method);
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // Ed << CL
                        nameGet32(o.eard, method);
                        method.append(" << val;");
                    } else {
                        method.append("Instructions.SHLD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_reg_cl) {
                    Grp2.SHRD_reg_cl o = (Grp2.SHRD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    nameGet32(o.eard, method);
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // Ed >>> CL
                        nameGet32(o.eard, method);
                        method.append(" >>> val;");
                    } else {
                        method.append("Instructions.SHRD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_reg_cl) {
                    Grp2.SARD_reg_cl o = (Grp2.SARD_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    nameGet32(o.eard, method);
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // Ed >> CL
                        nameGet32(o.eard, method);
                        method.append(" >> val;");
                    } else {
                        method.append("Instructions.SARD(val, ");
                        nameGet32(o.eard, method);
                        method.append(");");
                    }
                    return true;
                }
                // :TODO: implement fast versions
                if (op instanceof Grp2.ROLD_mem_cl) {
                    Grp2.ROLD_mem_cl o = (Grp2.ROLD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.ROLD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RORD_mem_cl) {
                    Grp2.RORD_mem_cl o = (Grp2.RORD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.RORD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCLD_mem_cl) {
                    Grp2.RCLD_mem_cl o = (Grp2.RCLD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.RCLD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCRD_mem_cl) {
                    Grp2.RCRD_mem_cl o = (Grp2.RCRD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.RCRD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHLD_mem_cl) {
                    Grp2.SHLD_mem_cl o = (Grp2.SHLD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.SHLD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHRD_mem_cl) {
                    Grp2.SHRD_mem_cl o = (Grp2.SHRD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.SHRD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SARD_mem_cl) {
                    Grp2.SARD_mem_cl o = (Grp2.SARD_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.SARD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                break;
            case 0x2e0: // LOOPNZ
                if (op instanceof Inst3.Loopnz32) {
                    Inst3.Loopnz32 o = (Inst3.Loopnz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && ("+getNZ()+")", method);
                    return false;
                }
                if (op instanceof Inst3.Loopnz16) {
                    Inst3.Loopnz16 o = (Inst3.Loopnz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && ("+getNZ()+")", method);
                    return false;
                }
                break;
            case 0x2e1: // LOOPZ
                if (op instanceof Inst3.Loopz32) {
                    Inst3.Loopz32 o = (Inst3.Loopz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && ("+getZ()+")", method);
                    return false;
                }
                if (op instanceof Inst3.Loopz16) {
                    Inst3.Loopz16 o = (Inst3.Loopz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && ("+getZ()+")", method);
                    return false;
                }
                break;
            case 0x2e2: // LOOP
                if (op instanceof Inst3.Loop32) {
                    Inst3.Loop32 o = (Inst3.Loop32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0", method);
                    return false;
                }
                if (op instanceof Inst3.Loop16) {
                    Inst3.Loop16 o = (Inst3.Loop16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0", method);
                    return false;
                }
                break;
            case 0x2e3: // JCXZ
                if (op instanceof Inst3.Jcxz) {
                    Inst3.Jcxz o = (Inst3.Jcxz) op;
                    compile(o, "(CPU_Regs.reg_ecx.dword & " + o.mask + ")==0", method);
                    return false;
                }
                break;
            case 0x2e5: // IN EAX,Ib
                if (op instanceof Inst3.InEaxIb) {
                    Inst3.InEaxIb o = (Inst3.InEaxIb) op;
                    method.append("if (CPU.CPU_IO_Exception(");
                    method.append(o.port);
                    method.append(",4)){").append(preException).append("return RUNEXCEPTION();}CPU_Regs.reg_eax.dword=IO.IO_ReadD(");
                    method.append(o.port);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2e7: // OUT Ib,EAX
                if (op instanceof Inst3.OutEaxIb) {
                    Inst3.OutEaxIb o = (Inst3.OutEaxIb) op;
                    method.append("if (CPU.CPU_IO_Exception(");
                    method.append(o.port);
                    method.append(",4)){").append(preException).append("return RUNEXCEPTION();}IO.IO_WriteD(");
                    method.append(o.port);
                    method.append(",CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x2e8: // CALL Jd
                if (op instanceof Inst3.CallJd) {
                    Inst3.CallJd o = (Inst3.CallJd) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");CPU_Regs.reg_eip+=");
                    method.append(o.addip + o.eip_count);
                    method.append(";return Constants.BR_Link1;");
                    return false;
                }
                break;
            case 0x2e9: // JMP Jd
                if (op instanceof Inst3.JmpJd) {
                    Inst3.JmpJd o = (Inst3.JmpJd) op;
                    method.append("CPU_Regs.reg_eip+=");
                    method.append(o.eip_count + o.addip);
                    method.append(";return Constants.BR_Link1;");
                    return false;
                }
                break;
            case 0x2ea: // JMP Ad
                if (op instanceof Inst3.JmpAd) {
                    Inst3.JmpAd o = (Inst3.JmpAd) op;
                    method.append("Flags.FillFlags();CPU.CPU_JMP(true,");
                    method.append(o.newcs);
                    method.append(", ");
                    method.append(o.newip);
                    method.append(", CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0x2eb: // JMP Jb
                if (op instanceof Inst3.JmpJb) {
                    Inst3.JmpJb o = (Inst3.JmpJb) op;
                    method.append("CPU_Regs.reg_eip+=");
                    method.append(o.eip_count + o.addip);
                    method.append(";return Constants.BR_Link1;");
                    return false;
                }
                break;
            case 0x2ed: // IN EAX,DX
                if (op instanceof Inst3.InEaxDx) {
                    Inst3.InEaxDx o = (Inst3.InEaxDx) op;
                    method.append("CPU_Regs.reg_eax.dword=IO.IO_ReadD(CPU_Regs.reg_edx.word());");
                    return true;
                }
                break;
            case 0x2ef: // OUT DX,EAX
                if (op instanceof Inst3.OutEaxDx) {
                    Inst3.OutEaxDx o = (Inst3.OutEaxDx) op;
                    method.append("IO.IO_WriteD(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x2f7: // GRP3 Ed(,Id)
                if (op instanceof Grp3.Testd_reg) {
                    Grp3.Testd_reg o = (Grp3.Testd_reg) op;
                    method.append("Instructions.TESTD(");
                    method.append(o.val);
                    method.append(",");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.Testd_mem) {
                    Grp3.Testd_mem o = (Grp3.Testd_mem) op;
                    method.append("Instructions.TESTD(");
                    method.append(o.val);
                    method.append(",Memory.mem_readd(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append("));");
                    return true;
                }
                if (op instanceof Grp3.NotEd_reg) {
                    Grp3.NotEd_reg o = (Grp3.NotEd_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=~");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NotEd_mem) {
                    Grp3.NotEd_mem o = (Grp3.NotEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa,~Memory.mem_readd(eaa));");
                    return true;
                }
                if (op instanceof Grp3.NegEd_reg) {
                    Grp3.NegEd_reg o = (Grp3.NegEd_reg) op;
                    method.append(nameSet32(o.eard));
                    if ((setFlags & o.sets())==0) {
                        method.append("=0-");
                        method.append(nameGet32(o.eard));
                        method.append(";");
                    } else {
                        method.append("=Instructions.Negd(");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp3.NegEd_mem) {
                    Grp3.NegEd_mem o = (Grp3.NegEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, ");
                    if ((setFlags & o.sets())==0) {
                        method.append("0-Memory.mem_readd(eaa));");
                    } else {
                        method.append("Instructions.Negd(Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp3.MulAxEd_reg) {
                    Grp3.MulAxEd_reg o = (Grp3.MulAxEd_reg) op;
                    if ((setFlags & o.sets())==0) {
                        declareLongVal(method);
                        method.append("lval=(CPU_Regs.reg_eax.dword & 0xFFFFFFFFl)*(");
                        method.append(nameGet32(o.eard));
                        method.append(" & 0xFFFFFFFFl);CPU_Regs.reg_eax.dword=(int)lval;CPU_Regs.reg_edx.dword=(int)(lval >> 32);");
                    } else {
                        method.append("Instructions.MULD(");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp3.MulAxEd_mem) {
                    Grp3.MulAxEd_mem o = (Grp3.MulAxEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        declareLongVal(method);
                        method.append("lval=(CPU_Regs.reg_eax.dword & 0xFFFFFFFFl)*(Memory.mem_readd(eaa) & 0xFFFFFFFFl);CPU_Regs.reg_eax.dword=(int)lval;CPU_Regs.reg_edx.dword=(int)(lval >> 32);");
                    } else {
                        method.append("Instructions.MULD(Memory.mem_readd(eaa));");
                    }
                    return true;
                }
                if (op instanceof Grp3.IMulAxEd_reg) {
                    Grp3.IMulAxEd_reg o = (Grp3.IMulAxEd_reg) op;
                    if ((setFlags & o.sets())==0) {
                        declareLongVal(method);
                        method.append("lval=(long)CPU_Regs.reg_eax.dword*");
                        method.append(nameGet32(o.eard));
                        method.append(";CPU_Regs.reg_eax.dword=(int)lval;CPU_Regs.reg_edx.dword=(int)(lval >> 32);");
                    } else {
                        method.append("Instructions.IMULD(");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp3.IMulAxEd_mem) {
                    Grp3.IMulAxEd_mem o = (Grp3.IMulAxEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        declareLongVal(method);
                        method.append("lval=(long)CPU_Regs.reg_eax.dword*Memory.mem_readd(eaa);CPU_Regs.reg_eax.dword=(int)lval;CPU_Regs.reg_edx.dword=(int)(lval >> 32);");
                    } else {
                        method.append("Instructions.IMULD(Memory.mem_readd(eaa));");
                    }
                    return true;
                }
                if (op instanceof Grp3.DivAxEd_reg) {
                    Grp3.DivAxEd_reg o = (Grp3.DivAxEd_reg) op;
                    method.append("if (!Instructions.DIVD(");
                    method.append(nameGet32(o.eard));
                    method.append(")){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.DivAxEd_mem) {
                    Grp3.DivAxEd_mem o = (Grp3.DivAxEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (!Instructions.DIVD(Memory.mem_readd(eaa))){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEd_reg) {
                    Grp3.IDivAxEd_reg o = (Grp3.IDivAxEd_reg) op;
                    method.append("if (!Instructions.IDIVD(");
                    method.append(nameGet32(o.eard));
                    method.append(")){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEd_mem) {
                    Grp3.IDivAxEd_mem o = (Grp3.IDivAxEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (!Instructions.IDIVD(Memory.mem_readd(eaa))){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x2ff: // GRP 5 Ed
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    inc((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                if (op instanceof Inst3.Incd_mem) {
                    Inst3.Incd_mem o = (Inst3.Incd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, ");
                    if ((setFlags & o.sets())==0) {
                        method.append("Memory.mem_readd(eaa)+1);");
                    } else {
                        method.append("Instructions.INCD(Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    dec((setFlags & o.sets())==0, 32, o.reg, method);
                    return true;
                }
                if (op instanceof Inst3.Decd_mem) {
                    Inst3.Decd_mem o = (Inst3.Decd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, ");
                    if ((setFlags & o.sets())==0) {
                        method.append("Memory.mem_readd(eaa)-1);");
                    } else {
                        method.append("Instructions.DECD(Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Inst3.CallNearEd_reg) {
                    Inst3.CallNearEd_reg o = (Inst3.CallNearEd_reg) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_eip+");method.append(o.eip_count);method.append(");CPU_Regs.reg_eip=");
                    method.append(nameGet32(o.eard));
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.CallNearEd_mem) {
                    Inst3.CallNearEd_mem o = (Inst3.CallNearEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readd(eaa); CPU.CPU_Push32(CPU_Regs.reg_eip+");method.append(o.eip_count);method.append(");CPU_Regs.reg_eip = val;return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.CallFarEd_mem) {
                    Inst3.CallFarEd_mem o = (Inst3.CallFarEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa+4);Flags.FillFlags();CPU.CPU_CALL(true,val2,val,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.JmpNearEd_reg) {
                    Inst3.JmpNearEd_reg o = (Inst3.JmpNearEd_reg) op;
                    method.append("CPU_Regs.reg_eip = ");
                    method.append(nameGet32(o.eard));
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.JmpNearEd_mem) {
                    Inst3.JmpNearEd_mem o = (Inst3.JmpNearEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("CPU_Regs.reg_eip=Memory.mem_readd(eaa);return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.JmpFarEd_mem) {
                    Inst3.JmpFarEd_mem o = (Inst3.JmpFarEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa+4);Flags.FillFlags();CPU.CPU_JMP(true,val2,val,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.PushEd_reg) {
                    Inst3.PushEd_reg o = (Inst3.PushEd_reg) op;
                    method.append("CPU.CPU_Push32(");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.PushEd_mem) {
                    Inst3.PushEd_mem o = (Inst3.PushEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("CPU.CPU_Push32(Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x301: // Group 7 Ed
                if (op instanceof Inst2.Sgdt_mem) {
                    Inst2.Sgdt_mem o = (Inst2.Sgdt_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());");
                    return true;
                }
                if (op instanceof Inst2.Sidt_mem) {
                    Inst2.Sidt_mem o = (Inst2.Sidt_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());");
                    return true;
                }
                if (op instanceof Inst4.Lgdt_mem) {
                    Inst4.Lgdt_mem o = (Inst4.Lgdt_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));");
                    return true;
                }
                if (op instanceof Inst4.Lidt_mem) {
                    Inst4.Lidt_mem o = (Inst4.Lidt_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}CPU.CPU_LIDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));");
                    return true;
                }
                if (op instanceof Inst2.Smsw_mem) {
                    Inst2.Smsw_mem o = (Inst2.Smsw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa,CPU.CPU_SMSW());");
                    return true;
                }
                if (op instanceof Inst2.Lmsw_mem) {
                    Inst2.Lmsw_mem o = (Inst2.Lmsw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_LMSW(Memory.mem_readw(eaa))){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst2.Invlpg) {
                    Inst2.Invlpg o = (Inst2.Invlpg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}Paging.PAGING_ClearTLB();");
                    return true;
                }
                if (op instanceof Inst2.Lgdt_reg) {
                    Inst2.Lgdt_reg o = (Inst2.Lgdt_reg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}return Constants.BR_Illegal;");
                    return false;
                }
                if (op instanceof Inst2.Lidt_reg) {
                    Inst2.Lidt_reg o = (Inst2.Lidt_reg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}return Constants.BR_Illegal;");
                    return false;
                }
                if (op instanceof Inst4.Smsw_reg) {
                    Inst4.Smsw_reg o = (Inst4.Smsw_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=CPU.CPU_SMSW();");
                    return true;
                }
                if (op instanceof Inst4.Lmsw_reg) {
                    Inst4.Lmsw_reg o = (Inst4.Lmsw_reg) op;
                    method.append("if (CPU.CPU_LMSW(");
                    method.append(nameGet32(o.eard));
                    method.append(")){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x302: // LAR Gd,Ed
                if (op instanceof Inst4.LarGdEd_reg) {
                    Inst4.LarGdEd_reg o = (Inst4.LarGdEd_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    method.append(nameSet32(o.rd));
                    method.append("=CPU.CPU_LAR(");
                    method.append(nameGet16(o.earw));
                    method.append(",");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.LarGdEd_mem) {
                    Inst4.LarGdEd_mem o = (Inst4.LarGdEd_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    method.append(nameSet32(o.rd));
                    method.append("=CPU.CPU_LAR(Memory.mem_readw(eaa),");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                break;
            case 0x303: // LSL Gd,Ew
                if (op instanceof Inst4.LslGdEd_reg) {
                    Inst4.LslGdEd_reg o = (Inst4.LslGdEd_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    method.append(nameSet32(o.rd));
                    method.append("=CPU.CPU_LSL(");
                    method.append(nameGet16(o.earw));
                    method.append(",");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.LslGdEd_mem) {
                    Inst4.LslGdEd_mem o = (Inst4.LslGdEd_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    method.append(nameSet32(o.rd));
                    method.append("=CPU.CPU_LSL(Memory.mem_readw(eaa),");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                break;
            case 0x330: // WRMSR
                if (op instanceof Inst4.WriteMSR) {
                    method.append("if (CPU.cpu.cpl != 0) return EXCEPTION(CPU.EXCEPTION_GP);");
                    method.append("CPU.writeMSR(CPU_Regs.reg_ecx.dword, ((CPU_Regs.reg_edx.dword & 0xFFFFFFFFl) << 32) | (CPU_Regs.reg_eax.dword & 0xFFFFFFFFl));");
                    return true;
                }
                break;
            case 0x332: // RDMSR
                if (op instanceof Inst4.ReadMSR) {
                    method.append("if (CPU.cpu.cpl != 0) return EXCEPTION(CPU.EXCEPTION_GP);");
                    method.append("long result = CPU.readMSR(CPU_Regs.reg_ecx.dword);");
                    method.append("CPU_Regs.reg_eax.dword = (int)result;");
                    method.append("CPU_Regs.reg_edx.dword = (int)(result >>> 32);");
                    return true;
                }
                break;
            case 0x340: // CMOVO
            case 0x341: // CMOVNO
            case 0x342: // CMOVB
            case 0x343: // CMOVNB
            case 0x344: // CMOVZ
            case 0x345: // CMOVNZ
            case 0x346: // CMOVBE
            case 0x347: // CMOVNBE
            case 0x348: // CMOVS
            case 0x349: // CMOVNS
            case 0x34a: // CMOVP
            case 0x34b: // CMOVNP
            case 0x34c: // CMOVL
            case 0x34d: // CMOVNL
            case 0x34e: // CMOVLE
            case 0x34f: // CMOVNLE
                if (op instanceof Inst4.ConditionalMov_reg) {
                    if (op instanceof Inst4.ConditionalMov_o_reg)
                        method.append("if (Flags.TFLG_O()) ");
                    else if (op instanceof Inst4.ConditionalMov_no_reg)
                        method.append("if (Flags.TFLG_NO()) ");
                    else if (op instanceof Inst4.ConditionalMov_b_reg)
                        method.append("if (Flags.TFLG_B()) ");
                    else if (op instanceof Inst4.ConditionalMov_nb_reg)
                        method.append("if (Flags.TFLG_NB()) ");
                    else if (op instanceof Inst4.ConditionalMov_z_reg)
                        method.append("if (Flags.TFLG_Z()) ");
                    else if (op instanceof Inst4.ConditionalMov_nz_reg)
                        method.append("if (Flags.TFLG_NZ()) ");
                    else if (op instanceof Inst4.ConditionalMov_be_reg)
                        method.append("if (Flags.TFLG_BE()) ");
                    else if (op instanceof Inst4.ConditionalMov_nbe_reg)
                        method.append("if (Flags.TFLG_NBE()) ");
                    else if (op instanceof Inst4.ConditionalMov_s_reg)
                        method.append("if (Flags.TFLG_S()) ");
                    else if (op instanceof Inst4.ConditionalMov_ns_reg)
                        method.append("if (Flags.TFLG_NS()) ");
                    else if (op instanceof Inst4.ConditionalMov_p_reg)
                        method.append("if (Flags.TFLG_P()) ");
                    else if (op instanceof Inst4.ConditionalMov_np_reg)
                        method.append("if (Flags.TFLG_NP()) ");
                    else if (op instanceof Inst4.ConditionalMov_l_reg)
                        method.append("if (Flags.TFLG_L()) ");
                    else if (op instanceof Inst4.ConditionalMov_nl_reg)
                        method.append("if (Flags.TFLG_NL()) ");
                    else if (op instanceof Inst4.ConditionalMov_le_reg)
                        method.append("if (Flags.TFLG_LE()) ");
                    else if (op instanceof Inst4.ConditionalMov_nle_reg)
                        method.append("if (Flags.TFLG_NLE()) ");
                    Inst4.ConditionalMov_reg o = (Inst4.ConditionalMov_reg)op;
                    method.append(nameSet32(o.gd));
                    method.append("=");
                    method.append(nameGet32(o.ed));
                    method.append(";");
                    return true;
                } else if (op instanceof Inst4.ConditionalMov_mem) {
                    Inst4.ConditionalMov_mem o = (Inst4.ConditionalMov_mem)op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);");
                    if (op instanceof Inst4.ConditionalMov_o_mem)
                        method.append("if (Flags.TFLG_O()) ");
                    else if (op instanceof Inst4.ConditionalMov_no_mem)
                        method.append("if (Flags.TFLG_NO()) ");
                    else if (op instanceof Inst4.ConditionalMov_b_mem)
                        method.append("if (Flags.TFLG_B()) ");
                    else if (op instanceof Inst4.ConditionalMov_nb_mem)
                        method.append("if (Flags.TFLG_NB()) ");
                    else if (op instanceof Inst4.ConditionalMov_z_mem)
                        method.append("if (Flags.TFLG_Z()) ");
                    else if (op instanceof Inst4.ConditionalMov_nz_mem)
                        method.append("if (Flags.TFLG_NZ()) ");
                    else if (op instanceof Inst4.ConditionalMov_be_mem)
                        method.append("if (Flags.TFLG_BE()) ");
                    else if (op instanceof Inst4.ConditionalMov_nbe_mem)
                        method.append("if (Flags.TFLG_NBE()) ");
                    else if (op instanceof Inst4.ConditionalMov_s_mem)
                        method.append("if (Flags.TFLG_S()) ");
                    else if (op instanceof Inst4.ConditionalMov_ns_mem)
                        method.append("if (Flags.TFLG_NS()) ");
                    else if (op instanceof Inst4.ConditionalMov_p_mem)
                        method.append("if (Flags.TFLG_P()) ");
                    else if (op instanceof Inst4.ConditionalMov_np_mem)
                        method.append("if (Flags.TFLG_NP()) ");
                    else if (op instanceof Inst4.ConditionalMov_l_mem)
                        method.append("if (Flags.TFLG_L()) ");
                    else if (op instanceof Inst4.ConditionalMov_nl_mem)
                        method.append("if (Flags.TFLG_NL()) ");
                    else if (op instanceof Inst4.ConditionalMov_le_mem)
                        method.append("if (Flags.TFLG_LE()) ");
                    else if (op instanceof Inst4.ConditionalMov_nle_mem)
                        method.append("if (Flags.TFLG_NLE()) ");
                    method.append(nameSet32(o.gd));
                    method.append("=val;");
                    return true;
                } else {
                    Log.exit("[Compiler] Unhandled op: " + op);
                }
                break;
            case 0x380: // JO
                if (op instanceof Inst4.JumpCond32_d_o) {
                    Inst4.JumpCond32_d_o o = (Inst4.JumpCond32_d_o) op;
                    compile(o, getO(), method);
                    return false;
                }
                break;
            case 0x381: // JNO
                if (op instanceof Inst4.JumpCond32_d_no) {
                    Inst4.JumpCond32_d_no o = (Inst4.JumpCond32_d_no) op;
                    compile(o, getNO(), method);
                    return false;
                }
                break;
            case 0x382: // JB
                if (op instanceof Inst4.JumpCond32_d_b) {
                    Inst4.JumpCond32_d_b o = (Inst4.JumpCond32_d_b) op;
                    compile(o, getB(), method);
                    return false;
                }
                break;
            case 0x383: // JNB
                if (op instanceof Inst4.JumpCond32_d_nb) {
                    Inst4.JumpCond32_d_nb o = (Inst4.JumpCond32_d_nb) op;
                    compile(o, getNB(), method);
                    return false;
                }
                break;
            case 0x384: // JZ
                if (op instanceof Inst4.JumpCond32_d_z) {
                    Inst4.JumpCond32_d_z o = (Inst4.JumpCond32_d_z) op;
                    compile(o, getZ(), method);
                    return false;
                }
                break;
            case 0x385: // JNZ
                if (op instanceof Inst4.JumpCond32_d_nz) {
                    Inst4.JumpCond32_d_nz o = (Inst4.JumpCond32_d_nz) op;
                    compile(o, getNZ(), method);
                    return false;
                }
                break;
            case 0x386: // JBE
                if (op instanceof Inst4.JumpCond32_d_be) {
                    Inst4.JumpCond32_d_be o = (Inst4.JumpCond32_d_be) op;
                    compile(o, getBE(), method);
                    return false;
                }
                break;
            case 0x387: // JNBE
                if (op instanceof Inst4.JumpCond32_d_nbe) {
                    Inst4.JumpCond32_d_nbe o = (Inst4.JumpCond32_d_nbe) op;
                    compile(o, getNBE(), method);
                    return false;
                }
                break;
            case 0x388: // JS
                if (op instanceof Inst4.JumpCond32_d_s) {
                    Inst4.JumpCond32_d_s o = (Inst4.JumpCond32_d_s) op;
                    compile(o, getS(), method);
                    return false;
                }
                break;
            case 0x389: // JNS
                if (op instanceof Inst4.JumpCond32_d_ns) {
                    Inst4.JumpCond32_d_ns o = (Inst4.JumpCond32_d_ns) op;
                    compile(o, getNS(), method);
                    return false;
                }
                break;
            case 0x38a: // JP
                if (op instanceof Inst4.JumpCond32_d_p) {
                    Inst4.JumpCond32_d_p o = (Inst4.JumpCond32_d_p) op;
                    compile(o, getP(), method);
                    return false;
                }
                break;
            case 0x38b: // JNP
                if (op instanceof Inst4.JumpCond32_d_np) {
                    Inst4.JumpCond32_d_np o = (Inst4.JumpCond32_d_np) op;
                    compile(o, getNP(), method);
                    return false;
                }
                break;
            case 0x38c: // JL
                if (op instanceof Inst4.JumpCond32_d_l) {
                    Inst4.JumpCond32_d_l o = (Inst4.JumpCond32_d_l) op;
                    compile(o, getL(), method);
                    return false;
                }
                break;
            case 0x38d: // JNL
                if (op instanceof Inst4.JumpCond32_d_nl) {
                    Inst4.JumpCond32_d_nl o = (Inst4.JumpCond32_d_nl) op;
                    compile(o, getNL(), method);
                    return false;
                }
                break;
            case 0x38e: // JLE
                if (op instanceof Inst4.JumpCond32_d_le) {
                    Inst4.JumpCond32_d_le o = (Inst4.JumpCond32_d_le) op;
                    compile(o, getLE(), method);
                    return false;
                }
                break;
            case 0x38f: // JNLE
                if (op instanceof Inst4.JumpCond32_d_nle) {
                    Inst4.JumpCond32_d_nle o = (Inst4.JumpCond32_d_nle) op;
                    compile(o, getNLE(), method);
                    return false;
                }
                break;
            case 0x3a0: // PUSH FS
                if (op instanceof Inst4.PushFS) {
                    Inst4.PushFS o = (Inst4.PushFS) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_fsVal.dword);");
                    return true;
                }
                break;
            case 0x3a1: // POP FS
                if (op instanceof Inst4.PopFS) {
                    Inst4.PopFS o = (Inst4.PopFS) op;
                    method.append("if (CPU.CPU_PopSegFS(true)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x3a3: // BT Ed,Gd
                if (op instanceof Inst4.BtEdGd_reg) {
                    Inst4.BtEdGd_reg o = (Inst4.BtEdGd_reg) op;
                    method.append("Flags.FillFlags();");
                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & (1 << (");method.append(nameGet32(o.rd));method.append(" & 31)))!=0);");
                    return true;
                }
                if (op instanceof Inst4.BtEdGd_mem) {
                    Inst4.BtEdGd_mem o = (Inst4.BtEdGd_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    method.append("eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Memory.mem_readd(eaa) & (1 << (");method.append(nameGet32(o.rd));method.append(" & 31)))!=0);");
                    return true;
                }
                break;
            case 0x3a4: // SHLD Ed,Gd,Ib
                if (op instanceof Inst4.ShldEdGdIb_reg) {
                    Inst4.ShldEdGdIb_reg o = (Inst4.ShldEdGdIb_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // (Ed << Ib) | (Gd >>> (32-Ib))
                        method.append("(");
                        method.append(nameGet32(o.eard));
                        method.append(" << ");
                        method.append(o.op3);
                        method.append(") | (");
                        method.append(nameGet32(o.rd));
                        method.append(" >>> ");
                        method.append(32-o.op3);
                        method.append(");");
                    } else {
                        method.append("Instructions.DSHLD(");
                        method.append(nameGet32(o.rd));
                        method.append(", ");
                        method.append(o.op3);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Inst4.ShldEdGdIb_mem) {
                    Inst4.ShldEdGdIb_mem o = (Inst4.ShldEdGdIb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, ");
                    if ((setFlags & o.sets())==0) {
                        method.append("(Memory.mem_readd(eaa) << ");
                        method.append(o.op3);
                        method.append(") | (");
                        method.append(nameGet32(o.rd));
                        method.append(" >>> ");
                        method.append(32-o.op3);
                        method.append("));");
                    } else {
                        method.append("Instructions.DSHLD(");
                        method.append(nameGet32(o.rd));
                        method.append(", ");
                        method.append(o.op3);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                break;
            case 0x3a5: // SHLD Ed,Gd,CL
                if (op instanceof Inst4.ShldEdGdCl_reg) {
                    Inst4.ShldEdGdCl_reg o = (Inst4.ShldEdGdCl_reg) op;
                    declareVal(method);
                    method.append("val=CPU_Regs.reg_ecx.low() & 0x1F;if (val!=0)");
                    method.append(nameSet32(o.eard));
                    method.append("=Instructions.DSHLD(");
                    method.append(nameGet32(o.rd));
                    method.append(", val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.ShldEdGdCl_mem) {
                    Inst4.ShldEdGdCl_mem o = (Inst4.ShldEdGdCl_mem) op;
                    declareVal(method);
                    method.append("val=CPU_Regs.reg_ecx.low() & 0x1F;if (val!=0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.DSHLD(");
                    method.append(nameGet32(o.rd));
                    method.append(", val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                break;
            case 0x3a8: // PUSH GS
                if (op instanceof Inst4.PushGS) {
                    Inst4.PushGS o = (Inst4.PushGS) op;
                    method.append("CPU.CPU_Push32(CPU_Regs.reg_gsVal.dword);");
                    return true;
                }
                break;
            case 0x3a9: // POP GS
                if (op instanceof Inst4.PopGS) {
                    Inst4.PopGS o = (Inst4.PopGS) op;
                    method.append("if (CPU.CPU_PopSegGS(true)){").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x3ab: // BTS Ed,Gd
                if (op instanceof Inst4.BtsEdGd_reg) {
                    Inst4.BtsEdGd_reg o = (Inst4.BtsEdGd_reg) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & val)!=0);");
                    method.append(nameSet32(o.eard));
                    method.append("|=val;");
                    return true;
                }
                if (op instanceof Inst4.BtsEdGd_mem) {
                    Inst4.BtsEdGd_mem o = (Inst4.BtsEdGd_mem) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);");
                    memory_start(o.get_eaa, seg, method);
                    method.append("eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val2 & val)!=0);Memory.mem_writed(eaa,val2 | val);");
                    return true;
                }
                break;
            case 0x3ac: // SHRD Ed,Gd,Ib
                if (op instanceof Inst4.ShrdEdGdIb_reg) {
                    Inst4.ShrdEdGdIb_reg o = (Inst4.ShrdEdGdIb_reg) op;
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    if ((setFlags & o.sets())==0) {
                        // (Ed >>> Ib) | (Gd << (32-Ib))
                        method.append("(");
                        method.append(nameGet32(o.eard));
                        method.append(" >>> ");
                        method.append(o.op3);
                        method.append(") | (");
                        method.append(nameGet32(o.rd));
                        method.append(" << ");
                        method.append(32-o.op3);
                        method.append(");");
                    } else {
                        method.append("Instructions.DSHRD(");
                        method.append(nameGet32(o.rd));
                        method.append(", ");
                        method.append(o.op3);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Inst4.ShrdEdGdIb_mem) {
                    Inst4.ShrdEdGdIb_mem o = (Inst4.ShrdEdGdIb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, ");
                    if ((setFlags & o.sets())==0) {
                        method.append("(Memory.mem_readd(eaa) >>> ");
                        method.append(o.op3);
                        method.append(") | (");
                        method.append(nameGet32(o.rd));
                        method.append(" << ");
                        method.append(32-o.op3);
                        method.append("));");
                    } else {
                        method.append("Instructions.DSHRD(");
                        method.append(nameGet32(o.rd));
                        method.append(", ");
                        method.append(o.op3);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                break;
            case 0x3ad: // SHRD Ed,Gd,CL
                if (op instanceof Inst4.ShrdEdGdCl_reg) {
                    Inst4.ShrdEdGdCl_reg o = (Inst4.ShrdEdGdCl_reg) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1F;if (val!=0)");
                    method.append(nameSet32(o.eard));
                    method.append("=Instructions.DSHRD(");
                    method.append(nameGet32(o.rd));
                    method.append(", val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.ShrdEdGdCl_mem) {
                    Inst4.ShrdEdGdCl_mem o = (Inst4.ShrdEdGdCl_mem) op;
                    declareVal(method);
                    method.append("val=CPU_Regs.reg_ecx.low() & 0x1F;if (val!=0) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writed(eaa, Instructions.DSHRD(");
                    method.append(nameGet32(o.rd));
                    method.append(", val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                break;
            case 0x3af: // IMUL Gd,Ed
                if (op instanceof Inst4.ImulGdEd_reg) {
                    Inst4.ImulGdEd_reg o = (Inst4.ImulGdEd_reg) op;
                    method.append(nameSet32(o.rd));
                    if ((setFlags & o.sets())==0) {
                        method.append("*=");
                        method.append(nameGet32(o.eard));
                        method.append(";");
                    } else {
                        method.append("=Instructions.DIMULD(");
                        method.append(nameGet32(o.eard));
                        method.append(",");
                        method.append(nameGet32(o.rd));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Inst4.ImulGdEd_mem) {
                    Inst4.ImulGdEd_mem o = (Inst4.ImulGdEd_mem) op;
                    method.append(nameSet32(o.rd));
                    if ((setFlags & o.sets())==0) {
                        method.append("*=");
                        method.append("Memory.mem_readd(");
                        toStringValue(o.get_eaa, seg, method);
                        method.append(");");
                    } else {
                        method.append("=Instructions.DIMULD(Memory.mem_readd(");
                        toStringValue(o.get_eaa, seg, method);
                        method.append("),");
                        method.append(nameGet32(o.rd));
                        method.append(");");
                    }
                    return true;
                }
                break;
            case 0x3b1: // CMPXCHG Ed,Gd
                if (op instanceof Inst4.CmpxchgEdGd_reg) {
                    Inst4.CmpxchgEdGd_reg o = (Inst4.CmpxchgEdGd_reg) op;
                    method.append("Flags.FillFlags();if (CPU_Regs.reg_eax.dword == ");
                    method.append(nameGet32(o.eard));
                    method.append(") {");
                    method.append(nameSet32(o.eard));
                    method.append("=");
                    method.append(nameGet32(o.rd));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {CPU_Regs.reg_eax.dword=");
                    method.append(nameGet32(o.eard));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                if (op instanceof Inst4.CmpxchgEdGd_mem) {
                    Inst4.CmpxchgEdGd_mem o = (Inst4.CmpxchgEdGd_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readd(eaa);if (CPU_Regs.reg_eax.dword == val) {Memory.mem_writed(eaa,");
                    method.append(nameGet32(o.rd));
                    method.append(");CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {Memory.mem_writed(eaa,val);CPU_Regs.reg_eax.dword=val;CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                break;
            case 0x3b2: // LSS Ed
                if (op instanceof Inst4.LssEd) {
                    Inst4.LssEd o = (Inst4.LssEd) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+4))){").append(preException).append("return RUNEXCEPTION();}");
                    method.append(nameSet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x3b3: // BTR Ed,Gd
                if (op instanceof Inst4.BtrEdGd_reg) {
                    Inst4.BtrEdGd_reg o = (Inst4.BtrEdGd_reg) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & val)!=0);");
                    method.append(nameSet32(o.eard));
                    method.append("&=~val;");
                    return true;
                }
                if (op instanceof Inst4.BtrEdGd_mem) {
                    Inst4.BtrEdGd_mem o = (Inst4.BtrEdGd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val2 & val)!=0);Memory.mem_writed(eaa,val2 & ~val);");
                    return true;
                }
                break;
            case 0x3b4: // LFS Ed
                if (op instanceof Inst4.LfsEd) {
                    Inst4.LfsEd o = (Inst4.LfsEd) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+4))){").append(preException).append("return RUNEXCEPTION();}");
                    method.append(nameSet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x3b5: // LGS Ed
                if (op instanceof Inst4.LgsEd) {
                    Inst4.LgsEd o = (Inst4.LgsEd) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+4))){").append(preException).append("return RUNEXCEPTION();}");
                    method.append(nameSet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x3b6: // MOVZX Gd,Eb
                if (op instanceof Inst4.MovzxGdEb_reg) {
                    Inst4.MovzxGdEb_reg o = (Inst4.MovzxGdEb_reg) op;
                    method.append(nameSet32(o.rd));
                    method.append("=");
                    method.append(nameGet8(o.earb));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovzxGdEb_mem) {
                    Inst4.MovzxGdEb_mem o = (Inst4.MovzxGdEb_mem) op;
                    method.append(nameSet32(o.rd));
                    method.append("=Memory.mem_readb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3b7: // MOVXZ Gd,Ew
                if (op instanceof Inst4.MovzxGdEw_reg) {
                    Inst4.MovzxGdEw_reg o = (Inst4.MovzxGdEw_reg) op;
                    method.append(nameSet32(o.rd));
                    method.append("=");
                    method.append(nameGet16(o.earw));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovzxGdEw_mem) {
                    Inst4.MovzxGdEw_mem o = (Inst4.MovzxGdEw_mem) op;
                    method.append(nameSet32(o.rd));
                    method.append("=Memory.mem_readw(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3ba: // GRP8 Ed,Ib
                if (op instanceof Inst4.BtEdIb_reg) {
                    Inst4.BtEdIb_reg o = (Inst4.BtEdIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    return true;
                }
                if (op instanceof Inst4.BtsEdIb_reg) {
                    Inst4.BtsEdIb_reg o = (Inst4.BtsEdIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    method.append(nameSet32(o.eard));
                    method.append("|=");
                    method.append(o.mask);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.BtrEdIb_reg) {
                    Inst4.BtrEdIb_reg o = (Inst4.BtrEdIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    method.append(nameSet32(o.eard));
                    method.append("&=~");
                    method.append(o.mask);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.BtcEdIb_reg) {
                    Inst4.BtcEdIb_reg o = (Inst4.BtcEdIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    method.append("if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0) ");
                    method.append(nameSet32(o.eard));
                    method.append("&=~");
                    method.append(o.mask);
                    method.append(";else ");
                    method.append(nameSet32(o.eard));
                    method.append("|=");
                    method.append(o.mask);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.BtEdIb_mem) {
                    Inst4.BtEdIb_mem o = (Inst4.BtEdIb_mem) op;
                    method.append("Flags.FillFlags();");
                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(Memory.mem_readd(");toStringValue(o.get_eaa, seg, method);method.append(") & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    return true;
                }
                if (op instanceof Inst4.BtsEdIb_mem) {
                    Inst4.BtsEdIb_mem o = (Inst4.BtsEdIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writed(eaa,val|");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.BtrEdIb_mem) {
                    Inst4.BtrEdIb_mem o = (Inst4.BtrEdIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writed(eaa,val & ~");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.BtcEdIb_mem) {
                    Inst4.BtcEdIb_mem o = (Inst4.BtcEdIb_mem) op;
                    method.append("Flags.FillFlags();");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val & ");
                    method.append(o.mask);
                    method.append(")!=0);if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0) val&=~");
                    method.append(o.mask);
                    method.append(";else val|=");
                    method.append(o.mask);
                    method.append(";Memory.mem_writed(eaa,val);");
                    return true;
                }
                break;
            case 0x3bb: // BTC Ed,Gd
                if (op instanceof Inst4.BtcEdGd_reg) {
                    Inst4.BtcEdGd_reg o = (Inst4.BtcEdGd_reg) op;
                    method.append("Flags.FillFlags();");
                    method.append("val=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & val)!=0);");
                    method.append(nameSet32(o.eard));
                    method.append("^=val;");
                    return true;
                }
                if (op instanceof Inst4.BtcEdGd_mem) {
                    Inst4.BtcEdGd_mem o = (Inst4.BtcEdGd_mem) op;
                    method.append("Flags.FillFlags();");
                    declareVal(method);
                    method.append("val=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);");
                    memory_start(o.get_eaa, seg, method);
                    method.append("eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(val2 & val)!=0);Memory.mem_writed(eaa,val2 ^ val);");
                    return true;
                }
                break;
            case 0x3bc: // BSF Gd,Ed
                if (op instanceof Inst4.BsfGdEd_reg) {
                    Inst4.BsfGdEd_reg o = (Inst4.BsfGdEd_reg) op;
                    declareVal(method);
                    method.append("val=");
                    method.append(nameGet32(o.eard));
                    method.append(";if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0;while ((val & 0x01)==0) { result++; val>>>=1; } CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet32(o.rd));
                    method.append("=result;}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst4.BsfGdEd_mem) {
                    Inst4.BsfGdEd_mem o = (Inst4.BsfGdEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);");
                    method.append("if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0;while ((val & 0x01)==0) { result++; val>>>=1; } CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet32(o.rd));
                    method.append("=result;}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x3bd: // BSR Gd,Ed
                if (op instanceof Inst4.BsrGdEd_reg) {
                    Inst4.BsrGdEd_reg o = (Inst4.BsrGdEd_reg) op;
                    declareVal(method);
                    method.append("val=");
                    method.append(nameGet32(o.eard));
                    method.append(";if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 31;while ((val & 0x80000000)==0) { result--; val<<=1; } CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet32(o.rd));
                    method.append("=result;} Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst4.BsrGdEd_mem) {
                    Inst4.BsrGdEd_mem o = (Inst4.BsrGdEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);if (val==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 31;while ((val & 0x80000000)==0) { result--; val<<=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet32(o.rd));
                    method.append("=result;}Flags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x3be: // MOVSX Gd,Eb
                if (op instanceof Inst4.MovsxGdEb_reg) {
                    Inst4.MovsxGdEb_reg o = (Inst4.MovsxGdEb_reg) op;
                    method.append(nameSet32(o.rd));
                    method.append("=(byte)");
                    method.append(nameGet8(o.earb));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovsxGdEb_mem) {
                    Inst4.MovsxGdEb_mem o = (Inst4.MovsxGdEb_mem) op;
                    method.append(nameSet32(o.rd));
                    method.append("=(byte)Memory.mem_readb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3bf: // MOVSX Gd,Ew
                if (op instanceof Inst4.MovsxGdEw_reg) {
                    Inst4.MovsxGdEw_reg o = (Inst4.MovsxGdEw_reg) op;
                    method.append(nameSet32(o.rd));
                    method.append("=(short)");
                    method.append(nameGet16(o.earw));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovsxGdEw_mem) {
                    Inst4.MovsxGdEw_mem o = (Inst4.MovsxGdEw_mem) op;
                    method.append(nameSet32(o.rd));
                    method.append("=(short)Memory.mem_readw(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3c1: // XADD Gd,Ed
                if (op instanceof Inst4.XaddGdEd_reg) {
                    Inst4.XaddGdEd_reg o = (Inst4.XaddGdEd_reg) op;
                    if ((setFlags & o.sets())==0) {
                        declareVal(method);
                        method.append("val=");
                        method.append(nameGet32(o.rd));
                        method.append(";");
                        method.append(nameSet32(o.rd));
                        method.append("=");
                        method.append(nameGet32(o.eard));
                        method.append(";");
                        method.append(nameSet32(o.eard));
                        method.append("+=val;");
                    } else {
                        declareVal(method);
                        method.append("val=Instructions.ADDD(");
                        method.append(nameGet32(o.rd));
                        method.append(",");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                        method.append(nameSet32(o.rd));
                        method.append("=");
                        method.append(nameGet32(o.eard));
                        method.append(";");
                        method.append(nameSet32(o.eard));
                        method.append("=val;");
                    }
                    return true;
                }
                if (op instanceof Inst4.XaddGdEd_mem) {
                    Inst4.XaddGdEd_mem o = (Inst4.XaddGdEd_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    if ((setFlags & o.sets())==0) {
                        declareVal(method);
                        method.append("val = Memory.mem_readd(eaa);Memory.mem_writed(eaa,val+");method.append(nameGet32(o.rd));method.append(");");
                        method.append(nameSet32(o.rd));
                        method.append("=val;");
                    } else {
                        declareVal(method);
                        method.append("val = Memory.mem_readd(eaa);");
                        declareVal2(method);
                        method.append("val2 = Instructions.ADDD(");
                        method.append(nameGet32(o.rd));
                        method.append(",val);Memory.mem_writed(eaa,val2);");
                        method.append(nameSet32(o.rd));
                        method.append("=val;");
                    }
                    return true;
                }
                break;
            case 0x3c7: // CMPXCHG8B
                if (op instanceof Inst4.CompareExchange8B) {
                    Inst4.CompareExchange8B o = (Inst4.CompareExchange8B) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("{long value1 = ((CPU_Regs.reg_edx.dword & 0xffffffffL) << 32) | (CPU_Regs.reg_eax.dword & 0xffffffffL);long value2 = (Memory.mem_readd(eaa) & 0xffffffffl) | ((Memory.mem_readd(eaa+4) & 0xffffffffl) << 32);Flags.FillFlags();if (value1==value2) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, true);Memory.mem_writed(eaa, CPU_Regs.reg_ebx.dword);Memory.mem_writed(eaa+4, CPU_Regs.reg_ecx.dword);} else {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF, false);CPU_Regs.reg_edx.dword = (int)(value2 >>> 32);CPU_Regs.reg_eax.dword = (int)value2;}}");
                    return true;
                }
                break;
            case 0x3c8: // BSWAP EAX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3c9: // BSWAP ECX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3ca: // BSWAP EDX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cb: // BSWAP EBX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cc: // BSWAP ESP
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cd: // BSWAP EBP
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3ce: // BSWAP ESI
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cf: // BSWAP EDI
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameSet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            default:
                if (op instanceof Inst1.Illegal) {
                    Inst1.Illegal o = (Inst1.Illegal) op;
                    method.append("Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR,");
                    method.append(o.msg);
                    method.append(");return Constants.BR_Illegal;");
                    return false;
                } else if (op instanceof Decoder.HandledSegChange) {
                    return true;
                } else if (op instanceof Decoder.HandledDecode) {
                    method.append("return Constants.BR_Jump;");
                    return false; // compile block
                } else if (op instanceof Decoder.ModifiedDecodeOp) {
                    method.append("Core.base_ds=");
                    method.append(seg.getDs());
                    method.append(";Core.base_ss=");
                    method.append(seg.getSs());
                    method.append(";Core.base_val_ds=");
                    method.append(seg.val);
                    method.append(";return ModifiedDecode.call();");
                    return false;
                } else {
                    Log.exit("[Compiler] Unhandled op: " + op);
                }
        }
        return true;
    }
}
