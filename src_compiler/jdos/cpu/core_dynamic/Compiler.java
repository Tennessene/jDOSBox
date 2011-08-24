package jdos.cpu.core_dynamic;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Instructions;
import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.misc.Log;

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

public class Compiler extends Helper {
    public static int compiledMethods = 0;

    private static Thread[] compilerThread = null;
    private static LinkedList compilerQueue = new LinkedList();
    private static Hashtable cache = new Hashtable();

    // :TODO: not sure if the compiler is thread safe
    private static final int processorCount = 0;//Runtime.getRuntime().availableProcessors();

    static {
        compilerThread = new Thread[processorCount];
        for (int i = 0; i < compilerThread.length; i++) {
            compilerThread[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            DecodeBlock nextBlock = null;
                            synchronized (compilerQueue) {
                                if (compilerQueue.isEmpty())
                                    compilerQueue.wait();
                                if (!compilerQueue.isEmpty())
                                    nextBlock = (DecodeBlock) compilerQueue.pop();
                            }
                            if (nextBlock == null) {
                                break;
                            }
                            if (nextBlock.active) {
                                do_compile(nextBlock);
                                nextBlock.op = nextBlock.next;
                                Integer key = new Integer(nextBlock.codeStart);
                                synchronized (cache) {
                                    Vector ops = (Vector) cache.get(key);
                                    if (ops == null) {
                                        ops = new Vector();
                                        cache.put(key, ops);
                                    }
                                    ops.add(nextBlock);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });
            compilerThread[i].start();
        }
    }

    static public void compile(DecodeBlock block) {
        synchronized (compilerQueue) {
            int len = Helper.decode.active_block.page.end - Helper.decode.active_block.page.start + 1;
            block.byteCode = new byte[len];
            block.codeStart = Helper.decode.code_start;
            int src = Paging.getDirectIndexRO(Helper.decode.code_start);
            if (src>=0)
                Memory.host_memcpy(block.byteCode, 0, src, len);
            else
                Memory.MEM_BlockRead(Helper.decode.code_start, block.byteCode, len);
            Op op = null;
            Vector ops;
            synchronized (cache) {
                ops = (Vector) cache.get(new Integer(block.codeStart));
            }
            if (ops != null) {
                for (int i = 0; i < ops.size(); i++) {
                    if (Arrays.equals(block.byteCode, ((DecodeBlock) ops.elementAt(i)).byteCode)) {
                        //op = ((DecodeBlock)ops.elementAt(i)).next;
                        cacheCount++;
                        if ((cacheCount & 0x3FF) == 0)
                            System.out.println("Cached " + cacheCount);
                        break;
                    }
                }
            }
            if (op != null) {
                block.op = op;
            } else {
                if (processorCount > 0) {
                    compilerQueue.add(block);
                    compilerQueue.notify();
                } else {
                    do_compile(block);
                    block.op = block.next;
                    Integer key = new Integer(block.codeStart);
                    if (ops == null) {
                        ops = new Vector();
                        cache.put(key, ops);
                    }
                    ops.add(block);
                }
            }
        }
        if (block == null) {
            for (int i = 0; i < compilerThread.length; i++)
                try {
                    compilerThread[i].join();
                } catch (Exception e) {
                }
        }
    }

    static private int cacheCount = 0;

    static public void do_compile(Op op) {
        Op prev = op;
        op = op.next;
        StringBuffer method = new StringBuffer();
        int count = 0;
        Op start = prev;
        while (op != null) {
            try {
                boolean jump = false;
                if (op.c < 0x400) {
                    count++;
                    if (start == null) {
                        start = prev;
                    }
                    method.append("{");
                    //method.append("/*" + Integer.toHexString(op.c) + "*/");
                    if (compile_op(op, method)) {
                        method.append("CPU_Regs.reg_eip+=");
                        method.append(op.eip_count);
                        method.append(";}\n  ");
                        continue;
                    } else {
                        method.append("}");
                        jump = true;
                    }
                } else {
                    //System.out.println(Integer.toHexString(op.c));
                }
//                if (op.next!=null) {
//                    System.out.println(Integer.toHexString(op.c));
//                    System.exit(1);
//                }
                if (count > 0) {
                    Op compiled = compileMethod(method, jump);
                    if (compiled != null) {
                        // set it up first
                        compiled.next = op;
                        if (op.cycle >= 1)
                            compiled.cycle = op.cycle;
                        else
                            compiled.cycle = prev.cycle;
                        // once this is assigned it is live
                        start.next = compiled;
                        compiledMethods++;
                        if ((compiledMethods % 250)==0) {
                            System.out.println("Compiled "+compiledMethods+" blocks");
                        }
                    }
                }
                if (method.length() > 0)
                    method = new StringBuffer();
                start = null;
                count = 0;
            } finally {
                prev = op;
                op = op.next;
            }
        }
    }

    static private String nameGet8(CPU_Regs.Reg reg) {
        if (reg.getParent()==null)
            return "CPU_Regs.reg_"+reg.getName()+".low()";
        return "CPU_Regs.reg_"+reg.getParent().getName()+".high()";
    }

    static private String nameGet16(CPU_Regs.Reg reg) {
        return "CPU_Regs.reg_"+reg.getName()+".word()";
    }

    static private String nameGet32(CPU_Regs.Reg reg) {
        if (reg.getName()==null) return String.valueOf(reg.dword);
        return "CPU_Regs.reg_"+reg.getName()+".dword";
    }

    static private String nameSet8(CPU_Regs.Reg reg, String value) {
        if (reg.getParent()==null)
            return "CPU_Regs.reg_"+reg.getName()+".low("+value+")";
        return "CPU_Regs.reg_"+reg.getParent().getName()+".high("+value+")";
    }

    static private String nameSet16(CPU_Regs.Reg reg, String value) {
        return "CPU_Regs.reg_"+reg.getName()+".word("+value+")";
    }
    static private String nameSet32(CPU_Regs.Reg reg, String value) {
        return "CPU_Regs.reg_"+reg.getName()+".dword="+value;
    }
    static private String nameRef(CPU_Regs.Reg reg) {
        return "CPU_Regs.reg_"+reg.getName();
    }

    static private void toStringValue(EaaBase eaa, StringBuffer method) {
        if (eaa instanceof Eaa.EA_16_00_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_01_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_02_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_esi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_03_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_edi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_04_n) {
            method.append("Core.base_ds+(CPU_Regs.reg_esi.word())");
        } else if (eaa instanceof Eaa.EA_16_05_n) {
            method.append("Core.base_ds+(CPU_Regs.reg_edi.word())");
        } else if (eaa instanceof Eaa.EA_16_06_n) {
            method.append("Core.base_ds+");method.append(((Eaa.EA_16_06_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_16_07_n) {
            method.append("Core.base_ds+(CPU_Regs.reg_ebx.word())");
        } else if (eaa instanceof Eaa.EA_16_40_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_40_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_41_n) {
            method.append(" Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_41_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_42_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_42_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_43_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_43_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_44_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_44_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_45_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_45_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_46_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+");method.append(((Eaa.EA_16_46_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_47_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+");method.append(((Eaa.EA_16_47_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_80_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_80_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_81_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_81_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_82_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_82_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_83_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_83_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_84_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_84_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_85_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_85_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_86_n) {
            method.append("Core.base_ss+((CPU_Regs.reg_ebp.word()+");method.append(((Eaa.EA_16_86_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_87_n) {
            method.append("Core.base_ds+((CPU_Regs.reg_ebx.word()+");method.append(((Eaa.EA_16_87_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_32_00_n) {
            method.append("Core.base_ds+CPU_Regs.reg_eax.dword");
        } else if (eaa instanceof Eaa.EA_32_01_n) {
            method.append("Core.base_ds+CPU_Regs.reg_ecx.dword");
        } else if (eaa instanceof Eaa.EA_32_02_n) {
            method.append("Core.base_ds+CPU_Regs.reg_edx.dword");
        } else if (eaa instanceof Eaa.EA_32_03_n) {
            method.append("Core.base_ds+CPU_Regs.reg_ebx.dword");
        } else if (eaa instanceof Eaa.EA_32_04_n) {
            Eaa.EA_32_04_n o = (Eaa.EA_32_04_n)eaa;
            if (o.ds) {
                method.append("Core.base_ds+");
            } else {
                method.append("Core.base_ss+");
            }
            method.append(nameGet32(o.reg));method.append("+(");method.append(nameGet32(o.reg2));
            if (o.sib>0) {
                method.append(" << ");
                method.append(o.sib);
            }
            method.append(")");
        } else if (eaa instanceof Eaa.EA_32_05_n) {
            method.append("Core.base_ds+");method.append(((Eaa.EA_32_05_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_06_n) {
            method.append("Core.base_ds+CPU_Regs.reg_esi.dword");
        } else if (eaa instanceof Eaa.EA_32_07_n) {
            method.append("Core.base_ds+CPU_Regs.reg_edi.dword");
        } else if (eaa instanceof Eaa.EA_32_40_n) {
            method.append("Core.base_ds+CPU_Regs.reg_eax.dword+");method.append(((Eaa.EA_32_40_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_41_n) {
           method.append("Core.base_ds+CPU_Regs.reg_ecx.dword+");method.append(((Eaa.EA_32_41_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_42_n) {
            method.append("Core.base_ds+CPU_Regs.reg_edx.dword+");method.append(((Eaa.EA_32_42_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_43_n) {
            method.append("Core.base_ds+CPU_Regs.reg_ebx.dword+");method.append(((Eaa.EA_32_43_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_44_n) {
            Eaa.EA_32_44_n o = (Eaa.EA_32_44_n)eaa;
            if (o.ds)
                method.append("Core.base_ds+");
            else
                method.append("Core.base_ss+");
            method.append(nameGet32(o.reg));method.append("+(");method.append(nameGet32(o.reg2));
            if (o.sib>0) {
                method.append(" << ");
                method.append(o.sib);
            }
            method.append(")+");method.append(o.i);
        } else if (eaa instanceof Eaa.EA_32_45_n) {
            method.append("Core.base_ss+CPU_Regs.reg_ebp.dword+");method.append(((Eaa.EA_32_45_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_46_n) {
            method.append("Core.base_ds+CPU_Regs.reg_esi.dword+");method.append(((Eaa.EA_32_46_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_47_n) {
            method.append("Core.base_ds+CPU_Regs.reg_edi.dword+");method.append(((Eaa.EA_32_47_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_80_n) {
            method.append("Core.base_ds+CPU_Regs.reg_eax.dword+");method.append(((Eaa.EA_32_80_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_81_n) {
            method.append("Core.base_ds+CPU_Regs.reg_ecx.dword+");method.append(((Eaa.EA_32_81_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_82_n) {
            method.append("Core.base_ds+CPU_Regs.reg_edx.dword+");method.append(((Eaa.EA_32_82_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_83_n) {
            method.append("Core.base_ds+CPU_Regs.reg_ebx.dword+");method.append(((Eaa.EA_32_83_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_84_n) {
            Eaa.EA_32_84_n o = (Eaa.EA_32_84_n)eaa;
            if (o.ds)
                method.append("Core.base_ds+");
            else
                method.append("Core.base_ss+");
            method.append(nameGet32(o.reg));method.append("+(");method.append(nameGet32(o.reg2));
            if (o.sib>0) {
                method.append(" << ");
                method.append(o.sib);
            }
            method.append(")+");method.append(o.i);
        } else if (eaa instanceof Eaa.EA_32_85_n) {
            method.append("Core.base_ss+CPU_Regs.reg_ebp.dword+");method.append(((Eaa.EA_32_85_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_86_n) {
            method.append("Core.base_ds+CPU_Regs.reg_esi.dword+");method.append(((Eaa.EA_32_86_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_87_n) {
            method.append("Core.base_ds+CPU_Regs.reg_edi.dword+");method.append(((Eaa.EA_32_87_n)eaa).i);
        }
    }
    static private void compile(Inst1.JumpCond16_b op, String cond, StringBuffer method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.offset);method.append("+");method.append(op.eip_count);method.append(");");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
    }
    static private void compile(Inst2.JumpCond16_w op, String cond, StringBuffer method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.offset);method.append("+");method.append(op.eip_count);method.append(");");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
    }
    static private void compile(Inst3.JumpCond32_b op, String cond, StringBuffer method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_eip+=");method.append(op.offset);method.append("+");method.append(op.eip_count);method.append(";");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_eip+=");method.append(op.eip_count);method.append(";return Constants.BR_Link2;");
    }
    static private void compile(Inst4.JumpCond32_d op, String cond, StringBuffer method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_eip+=");method.append(op.offset);method.append("+");method.append(op.eip_count);method.append(";");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_eip+=");method.append(op.eip_count);method.append(";return Constants.BR_Link2;");
    }
    static private boolean compile_op(Op op, StringBuffer method) {
        switch (op.c) {
            case 0x00: // ADD Eb,Gb
            case 0x200:
                if (op instanceof Inst1.Addb_reg) {
                    Inst1.Addb_reg o = (Inst1.Addb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ADDB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AddEbGb_mem) {
                    Inst1.AddEbGb_mem o = (Inst1.AddEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ADDB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x01: // ADD Ew,Gw
                if (op instanceof Inst1.Addw_reg) {
                    Inst1.Addw_reg o = (Inst1.Addw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ADDW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AddEwGw_mem) {
                    Inst1.AddEwGw_mem o = (Inst1.AddEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ADDW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x02: // ADD Gb,Eb
            case 0x202:
                if (op instanceof Inst1.Addb_reg) {
                    Inst1.Addb_reg o = (Inst1.Addb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ADDB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AddGbEb_mem) {
                    Inst1.AddGbEb_mem o = (Inst1.AddGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.ADDB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x03: // ADD Gw,Ew
                if (op instanceof Inst1.Addw_reg) {
                    Inst1.Addw_reg o = (Inst1.Addw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ADDW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AddGwEw_mem) {
                    Inst1.AddGwEw_mem o = (Inst1.AddGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.ADDW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x04: // ADD AL,Ib
            case 0x204:
                if (op instanceof Inst1.AddAlIb) {
                    Inst1.AddAlIb o = (Inst1.AddAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.ADDB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x05: // ADD AX,Iw
                if (op instanceof Inst1.AddAxIw) {
                    Inst1.AddAxIw o = (Inst1.AddAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.ADDW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x06: // PUSH ES
                if (op instanceof Inst1.PushES) {
                    Inst1.PushES o = (Inst1.PushES) op;
                    method.append("CPU.CPU_Push16(CPU.Segs_ESval);");
                    return true;
                }
                break;
            case 0x07: // POP ES
                if (op instanceof Inst1.PopES) {
                    Inst1.PopES o = (Inst1.PopES) op;
                    method.append("if (CPU.CPU_PopSegES(false)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x08: // OR Eb,Gb
            case 0x208:
                if (op instanceof Inst1.Orb_reg) {
                    Inst1.Orb_reg o = (Inst1.Orb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ORB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.OrEbGb_mem) {
                    Inst1.OrEbGb_mem o = (Inst1.OrEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ORB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x09: // OR Ew,Gw
                if (op instanceof Inst1.Orw_reg) {
                    Inst1.Orw_reg o = (Inst1.Orw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ORW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.OrEwGw_mem) {
                    Inst1.OrEwGw_mem o = (Inst1.OrEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ORW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x0a: // OR Gb,Eb
            case 0x20a:
                if (op instanceof Inst1.Orb_reg) {
                    Inst1.Orb_reg o = (Inst1.Orb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ORB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.OrGbEb_mem) {
                    Inst1.OrGbEb_mem o = (Inst1.OrGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.ORB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x0b: // OR Gw,Ew
                if (op instanceof Inst1.Orw_reg) {
                    Inst1.Orw_reg o = (Inst1.Orw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ORW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.OrGwEw_mem) {
                    Inst1.OrGwEw_mem o = (Inst1.OrGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.ORW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x0c: // OR AL,Ib
            case 0x20c:
                if (op instanceof Inst1.OrAlIb) {
                    Inst1.OrAlIb o = (Inst1.OrAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.ORB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x0d: // OR AX,Iw
                if (op instanceof Inst1.OrAxIw) {
                    Inst1.OrAxIw o = (Inst1.OrAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.ORW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x0e: // PUSH CS
                if (op instanceof Inst1.PushCS) {
                    Inst1.PushCS o = (Inst1.PushCS) op;
                    method.append("CPU.CPU_Push16(CPU.Segs_CSval);");
                    return true;
                }
                break;
            case 0x0f: // 2 byte opcodes*/
            case 0x20f:
                break;
            case 0x10: // ADC Eb,Gb
            case 0x210:
                if (op instanceof Inst1.Adcb_reg) {
                    Inst1.Adcb_reg o = (Inst1.Adcb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ADCB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AdcEbGb_mem) {
                    Inst1.AdcEbGb_mem o = (Inst1.AdcEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ADCB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x11: // ADC Ew,Gw
                if (op instanceof Inst1.Adcw_reg) {
                    Inst1.Adcw_reg o = (Inst1.Adcw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ADCW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AdcEwGw_mem) {
                    Inst1.AdcEwGw_mem o = (Inst1.AdcEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ADCW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x12: // ADC Gb,Eb
            case 0x212:
                if (op instanceof Inst1.Adcb_reg) {
                    Inst1.Adcb_reg o = (Inst1.Adcb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ADCB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AdcGbEb_mem) {
                    Inst1.AdcGbEb_mem o = (Inst1.AdcGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.ADCB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x13: // ADC Gw,Ew
                if (op instanceof Inst1.Adcw_reg) {
                    Inst1.Adcw_reg o = (Inst1.Adcw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ADCW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AdcGwEw_mem) {
                    Inst1.AdcGwEw_mem o = (Inst1.AdcGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.ADCW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x14: // ADC AL,Ib
            case 0x214:
                if (op instanceof Inst1.AdcAlIb) {
                    Inst1.AdcAlIb o = (Inst1.AdcAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.ADCB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x15: // ADC AX,Iw
                if (op instanceof Inst1.AdcAxIw) {
                    Inst1.AdcAxIw o = (Inst1.AdcAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.ADCW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x16: // PUSH SS
                if (op instanceof Inst1.PushSS) {
                    Inst1.PushSS o = (Inst1.PushSS) op;
                    method.append("CPU.CPU_Push16(CPU.Segs_SSval);");
                    return true;
                }
                break;
            case 0x17: // POP SS
                if (op instanceof Inst1.PopSS) {
                    Inst1.PopSS o = (Inst1.PopSS) op;
                    method.append("if (CPU.CPU_PopSegSS(false)) return RUNEXCEPTION();Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                break;
            case 0x18: // SBB Eb,Gb
            case 0x218:
                if (op instanceof Inst1.Sbbb_reg) {
                    Inst1.Sbbb_reg o = (Inst1.Sbbb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.SBBB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SbbEbGb_mem) {
                    Inst1.SbbEbGb_mem o = (Inst1.SbbEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.SBBB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x19: // SBB Ew,Gw
                if (op instanceof Inst1.Sbbw_reg) {
                    Inst1.Sbbw_reg o = (Inst1.Sbbw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.SBBW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SbbEwGw_mem) {
                    Inst1.SbbEwGw_mem o = (Inst1.SbbEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.SBBW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x1a: // SBB Gb,Eb
            case 0x21a:
                if (op instanceof Inst1.Sbbb_reg) {
                    Inst1.Sbbb_reg o = (Inst1.Sbbb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.SBBB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SbbGbEb_mem) {
                    Inst1.SbbGbEb_mem o = (Inst1.SbbGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.SBBB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b: // SBB Gw,Ew
                if (op instanceof Inst1.Sbbw_reg) {
                    Inst1.Sbbw_reg o = (Inst1.Sbbw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.SBBW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SbbGwEw_mem) {
                    Inst1.SbbGwEw_mem o = (Inst1.SbbGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.SBBW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c: // SBB AL,Ib
            case 0x21c:
                if (op instanceof Inst1.SbbAlIb) {
                    Inst1.SbbAlIb o = (Inst1.SbbAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.SBBB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x1d: // SBB AX,Iw
                if (op instanceof Inst1.SbbAxIw) {
                    Inst1.SbbAxIw o = (Inst1.SbbAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.SBBW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x1e: // PUSH DS
                if (op instanceof Inst1.PushDS) {
                    Inst1.PushDS o = (Inst1.PushDS) op;
                    method.append("CPU.CPU_Push16(CPU.Segs_DSval);");
                    return true;
                }
                break;
            case 0x1f: // POP DS
                if (op instanceof Inst1.PopDS) {
                    Inst1.PopDS o = (Inst1.PopDS) op;
                    method.append("if (CPU.CPU_PopSegDS(false)) return RUNEXCEPTION();Core.base_ds=CPU.Segs_DSphys;Core.base_val_ds= CPU_Regs.ds;");
                    return true;
                }
                break;
            case 0x20: // AND Eb,Gb
            case 0x220:
                if (op instanceof Inst1.Andb_reg) {
                    Inst1.Andb_reg o = (Inst1.Andb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ANDB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AndEbGb_mem) {
                    Inst1.AndEbGb_mem o = (Inst1.AndEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ANDB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x21: // AND Ew,Gw
                if (op instanceof Inst1.Andw_reg) {
                    Inst1.Andw_reg o = (Inst1.Andw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ANDW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AndEwGw_mem) {
                    Inst1.AndEwGw_mem o = (Inst1.AndEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ANDW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x22: // AND Gb,Eb
            case 0x222:
                if (op instanceof Inst1.Andb_reg) {
                    Inst1.Andb_reg o = (Inst1.Andb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.ANDB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AndGbEb_mem) {
                    Inst1.AndGbEb_mem o = (Inst1.AndGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.ANDB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x23: // AND Gw,Ew
                if (op instanceof Inst1.Andw_reg) {
                    Inst1.Andw_reg o = (Inst1.Andw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.ANDW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.AndGwEw_mem) {
                    Inst1.AndGwEw_mem o = (Inst1.AndGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.ANDW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x24: // AND AL,Ib
            case 0x224:
                if (op instanceof Inst1.AndAlIb) {
                    Inst1.AndAlIb o = (Inst1.AndAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.ANDB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x25: // AND AX,Iw
                if (op instanceof Inst1.AndAxIw) {
                    Inst1.AndAxIw o = (Inst1.AndAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.ANDW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x26: // SEG ES:
            case 0x226:
                if (op instanceof Inst1.SegES) {
                    Inst1.SegES o = (Inst1.SegES) op;
                    method.append("Core.DO_PREFIX_SEG_ES();");
                    return true;
                }
                break;
            case 0x27: // DAA
            case 0x227:
                if (op instanceof Inst1.Daa) {
                    Inst1.Daa o = (Inst1.Daa) op;
                    method.append("Instructions.DAA();");
                    return true;
                }
                break;
            case 0x28: // SUB Eb,Gb
            case 0x228:
                if (op instanceof Inst1.Subb_reg) {
                    Inst1.Subb_reg o = (Inst1.Subb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.SUBB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SubEbGb_mem) {
                    Inst1.SubEbGb_mem o = (Inst1.SubEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.SUBB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x29: // SUB Ew,Gw
                if (op instanceof Inst1.Subw_reg) {
                    Inst1.Subw_reg o = (Inst1.Subw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.SUBW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SubEwGw_mem) {
                    Inst1.SubEwGw_mem o = (Inst1.SubEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.SUBW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x2a: // SUB Gb,Eb
            case 0x22a:
                if (op instanceof Inst1.Subb_reg) {
                    Inst1.Subb_reg o = (Inst1.Subb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.SUBB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SubGbEb_mem) {
                    Inst1.SubGbEb_mem o = (Inst1.SubGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.SUBB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x2b: // SUB Gw,Ew
                if (op instanceof Inst1.Subw_reg) {
                    Inst1.Subw_reg o = (Inst1.Subw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.SUBW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SubGwEw_mem) {
                    Inst1.SubGwEw_mem o = (Inst1.SubGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.SUBW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x2c: // SUB AL,Ib
            case 0x22c:
                if (op instanceof Inst1.SubAlIb) {
                    Inst1.SubAlIb o = (Inst1.SubAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.SUBB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x2d: // SUB AX,Iw
                if (op instanceof Inst1.SubAxIw) {
                    Inst1.SubAxIw o = (Inst1.SubAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.SUBW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x2e: // SEG CS:
            case 0x22e:
                if (op instanceof Inst1.SegCS) {
                    Inst1.SegCS o = (Inst1.SegCS) op;
                    method.append("Core.DO_PREFIX_SEG_CS();");
                    return true;
                }
                break;
            case 0x2f: // DAS
            case 0x22f:
                if (op instanceof Inst1.Das) {
                    Inst1.Das o = (Inst1.Das) op;
                    method.append("Instructions.DAS();");
                    return true;
                }
                break;
            case 0x30: // XOR Eb,Gb
            case 0x230:
                if (op instanceof Inst1.Xorb_reg) {
                    Inst1.Xorb_reg o = (Inst1.Xorb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.XORB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XorEbGb_mem) {
                    Inst1.XorEbGb_mem o = (Inst1.XorEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.XORB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                break;
            case 0x31: // XOR Ew,Gw
                if (op instanceof Inst1.Xorw_reg) {
                    Inst1.Xorw_reg o = (Inst1.Xorw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.XORW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XorEwGw_mem) {
                    Inst1.XorEwGw_mem o = (Inst1.XorEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.XORW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x32: // XOR Gb,Eb
            case 0x232:
                if (op instanceof Inst1.Xorb_reg) {
                    Inst1.Xorb_reg o = (Inst1.Xorb_reg) op;
                    method.append(nameSet8(o.e, "Instructions.XORB(" + nameGet8(o.g) + ", " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XorGbEb_mem) {
                    Inst1.XorGbEb_mem o = (Inst1.XorGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet8(o.e, "Instructions.XORB(Memory.mem_readb(eaa), " + nameGet8(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x33: // XOR Gw,Ew
                if (op instanceof Inst1.Xorw_reg) {
                    Inst1.Xorw_reg o = (Inst1.Xorw_reg) op;
                    method.append(nameSet16(o.e, "Instructions.XORW(" + nameGet16(o.g) + ", " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XorGwEw_mem) {
                    Inst1.XorGwEw_mem o = (Inst1.XorGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameSet16(o.e, "Instructions.XORW(Memory.mem_readw(eaa), " + nameGet16(o.e) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x34: // XOR AL,Ib
            case 0x234:
                if (op instanceof Inst1.XorAlIb) {
                    Inst1.XorAlIb o = (Inst1.XorAlIb) op;
                    method.append("CPU_Regs.reg_eax.low(Instructions.XORB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low()));");
                    return true;
                }
                break;
            case 0x35: // XOR AX,Iw
                if (op instanceof Inst1.XorAxIw) {
                    Inst1.XorAxIw o = (Inst1.XorAxIw) op;
                    method.append("CPU_Regs.reg_eax.word(Instructions.XORW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word()));");
                    return true;
                }
                break;
            case 0x36: // SEG SS:
            case 0x236:
                if (op instanceof Inst1.SegSS) {
                    Inst1.SegSS o = (Inst1.SegSS) op;
                    method.append("Core.DO_PREFIX_SEG_SS();");
                    return true;
                }
                break;
            case 0x37: // AAA
            case 0x237:
                if (op instanceof Inst1.Aaa) {
                    Inst1.Aaa o = (Inst1.Aaa) op;
                    method.append("Instructions.AAA();");
                    return true;
                }
                break;
            case 0x38: // CMP Eb,Gb
            case 0x238:
                if (op instanceof Inst1.Cmpb_reg) {
                    Inst1.Cmpb_reg o = (Inst1.Cmpb_reg) op;
                    method.append("Instructions.CMPB(");
                    method.append(nameGet8(o.g));
                    method.append(", ");
                    method.append(nameGet8(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.CmpEbGb_mem) {
                    Inst1.CmpEbGb_mem o = (Inst1.CmpEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Instructions.CMPB(");
                    method.append(nameGet8(o.g));
                    method.append(", Memory.mem_readb(eaa));");
                    return true;
                }
                break;
            case 0x39: // CMP Ew,Gw
                if (op instanceof Inst1.Cmpw_reg) {
                    Inst1.Cmpw_reg o = (Inst1.Cmpw_reg) op;
                    method.append("Instructions.CMPW(");
                    method.append(nameGet16(o.g));
                    method.append(", ");
                    method.append(nameGet16(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.CmpEwGw_mem) {
                    Inst1.CmpEwGw_mem o = (Inst1.CmpEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.e, method);
                    method.append(";Instructions.CMPW(");
                    method.append(nameGet16(o.g));
                    method.append(", Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x3a: // CMP Gb,Eb
            case 0x23a:
                if (op instanceof Inst1.Cmpb_reg) {
                    Inst1.Cmpb_reg o = (Inst1.Cmpb_reg) op;
                    method.append("Instructions.CMPB(");
                    method.append(nameGet8(o.g));
                    method.append(", ");
                    method.append(nameGet8(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.CmpGbEb_mem) {
                    Inst1.CmpGbEb_mem o = (Inst1.CmpGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";Instructions.CMPB(Memory.mem_readb(eaa), ");
                    method.append(nameGet8(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3b: // CMP Gw,Ew
                if (op instanceof Inst1.Cmpw_reg) {
                    Inst1.Cmpw_reg o = (Inst1.Cmpw_reg) op;
                    method.append("Instructions.CMPW(");
                    method.append(nameGet16(o.g));
                    method.append(", ");
                    method.append(nameGet16(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.CmpGwEw_mem) {
                    Inst1.CmpGwEw_mem o = (Inst1.CmpGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";Instructions.CMPW(Memory.mem_readw(eaa), ");
                    method.append(nameGet16(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3c: // CMP AL,Ib
            case 0x23c:
                if (op instanceof Inst1.CmpAlIb) {
                    Inst1.CmpAlIb o = (Inst1.CmpAlIb) op;
                    method.append("Instructions.CMPB((short)");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0x3d: // CMP AX,Iw
                if (op instanceof Inst1.CmpAxIw) {
                    Inst1.CmpAxIw o = (Inst1.CmpAxIw) op;
                    method.append("Instructions.CMPW(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0x3e: // SEG DS:
            case 0x23e:
                if (op instanceof Inst1.SegDS) {
                    Inst1.SegDS o = (Inst1.SegDS) op;
                    method.append("Core.DO_PREFIX_SEG_DS();");
                    return true;
                }
                break;
            case 0x3f: // AAS
            case 0x23f:
                if (op instanceof Inst1.Aas) {
                    Inst1.Aas o = (Inst1.Aas) op;
                    method.append("Instructions.AAS();");
                    return true;
                }
                break;
            case 0x40: // INC AX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x41: // INC CX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x42: // INC DX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x43: // INC BX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x44: // INC SP
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x45: // INC BP
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x46: // INC SI
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x47: // INC DI
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x48: // DEC AX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x49: // DEC CX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x4a: // DEC DX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x4b: // DEC BX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x4c: // DEC SP
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x4d: // DEC BP
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x4e: // DEC SI
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x4f: // DEC DI
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x50: // PUSH AX
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x51: // PUSH CX
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x52: // PUSH DX
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x53: // PUSH BX
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x54: // PUSH SP
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x55: // PUSH BP
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x56: // PUSH SI
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x57: // PUSH DI
                if (op instanceof Inst1.Pushw) {
                    Inst1.Pushw o = (Inst1.Pushw) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x58: // POP AX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x59: // POP CX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x5a: // POP DX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x5b: // POP BX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x5c: // POP SP
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x5d: // POP BP
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x5e: // POP SI
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x5f: // POP DI
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    method.append(nameSet16(o.reg, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x60: // PUSHA
                if (op instanceof Inst1.Pusha) {
                    Inst1.Pusha o = (Inst1.Pusha) op;
                    method.append("int old_sp=CPU_Regs.reg_esp.word();CPU.CPU_Push16(CPU_Regs.reg_eax.word());CPU.CPU_Push16(CPU_Regs.reg_ecx.word());CPU.CPU_Push16(CPU_Regs.reg_edx.word());CPU.CPU_Push16(CPU_Regs.reg_ebx.word());CPU.CPU_Push16(old_sp);CPU.CPU_Push16(CPU_Regs.reg_ebp.word());CPU.CPU_Push16(CPU_Regs.reg_esi.word());CPU.CPU_Push16(CPU_Regs.reg_edi.word());");
                    return true;
                }
                break;
            case 0x61: // POPA
                if (op instanceof Inst1.Popa) {
                    Inst1.Popa o = (Inst1.Popa) op;
                    method.append("CPU_Regs.reg_edi.word(CPU.CPU_Pop16());CPU_Regs.reg_esi.word(CPU.CPU_Pop16());CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());CPU.CPU_Pop16();CPU_Regs.reg_ebx.word(CPU.CPU_Pop16());CPU_Regs.reg_edx.word(CPU.CPU_Pop16());CPU_Regs.reg_ecx.word(CPU.CPU_Pop16());CPU_Regs.reg_eax.word(CPU.CPU_Pop16());");
                    return true;
                }
                break;
            case 0x62: // BOUND
                if (op instanceof Inst1.Bound) {
                    Inst1.Bound o = (Inst1.Bound) op;
                    method.append("short r = (short)");
                    method.append(nameGet16(o.reg));
                    method.append(";short bound_min, bound_max;int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";bound_min=(short)Memory.mem_readw(eaa); bound_max=(short)Memory.mem_readw(eaa+2);if ( (r < bound_min) || (r > bound_max) ) {return EXCEPTION(5);}");
                    return true;
                }
                break;
            case 0x63: // ARPL Ew,Rw
                if (op instanceof Inst1.ArplEwRw_reg) {
                    Inst1.ArplEwRw_reg o = (Inst1.ArplEwRw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;IntRef ref = new IntRef(");
                    method.append(nameGet16(o.earw));
                    method.append(");CPU.CPU_ARPL(ref,");
                    method.append(nameGet16(o.rw));
                    method.append(");");
                    method.append(nameSet16(o.earw, "ref.value"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.ArplEwRw_mem) {
                    Inst1.ArplEwRw_mem o = (Inst1.ArplEwRw_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";IntRef ref = new IntRef(Memory.mem_readw(eaa));CPU.CPU_ARPL(ref,");
                    method.append(nameGet16(o.rw));
                    method.append(");Memory.mem_writew(eaa,ref.value);");
                    return true;
                }
                break;
            case 0x64: // SEG FS:
            case 0x264:
                if (op instanceof Inst1.SegFS) {
                    Inst1.SegFS o = (Inst1.SegFS) op;
                    method.append("Core.DO_PREFIX_SEG_FS();");
                    return true;
                }
                break;
            case 0x65: // SEG GS:
            case 0x265:
                if (op instanceof Inst1.SegGS) {
                    Inst1.SegGS o = (Inst1.SegGS) op;
                    method.append("Core.DO_PREFIX_SEG_GS();");
                    return true;
                }
                break;
            case 0x66: // Operand Size Prefix
            case 0x266:
                break;
            case 0x67: // Address Size Prefix
            case 0x267:
                break;
            case 0x68: // PUSH Iw
                if (op instanceof Inst1.Push16) {
                    Inst1.Push16 o = (Inst1.Push16) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(o.value);
                    method.append(");");
                    return true;
                }
                break;
            case 0x69: // IMUL Gw,Ew,Iw
                if (op instanceof Inst1.IMULGwEwIw_reg) {
                    Inst1.IMULGwEwIw_reg o = (Inst1.IMULGwEwIw_reg) op;
                    method.append(nameSet16(o.rw, "Instructions.DIMULW(" + nameGet16(o.earw) + ", " + o.op3 + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.IMULGwEwIw_mem) {
                    Inst1.IMULGwEwIw_mem o = (Inst1.IMULGwEwIw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "Instructions.DIMULW(Memory.mem_readw(eaa)," + o.op3 + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x6a: // PUSH o.ib
                if (op instanceof Inst1.Push16) {
                    Inst1.Push16 o = (Inst1.Push16) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(o.value);
                    method.append(");");
                    return true;
                }
                break;
            case 0x6b: // IMUL Gw,Ew,Ib
                if (op instanceof Inst1.IMULGwEwIb_reg) {
                    Inst1.IMULGwEwIb_reg o = (Inst1.IMULGwEwIb_reg) op;
                    method.append(nameSet16(o.rw, "Instructions.DIMULW(" + nameGet16(o.earw) + ", " + o.op3 + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.IMULGwEwIb_mem) {
                    Inst1.IMULGwEwIb_mem o = (Inst1.IMULGwEwIb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "Instructions.DIMULW(Memory.mem_readw(eaa)," + o.op3 + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x6c: // INSB
            case 0x26c:
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) return RUNEXCEPTION();Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x6d: // INSW
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) return RUNEXCEPTION();Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x6e: // OUTSB
            case 0x26e:
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) return RUNEXCEPTION();Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x6f: // OUTSW
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) return RUNEXCEPTION();Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0x70: // JO
                if (op instanceof Inst1.JumpCond16_b_o) {
                    Inst1.JumpCond16_b_o o = (Inst1.JumpCond16_b_o) op;
                    compile(o, "Flags.TFLG_O()", method);
                    return false;
                }
                break;
            case 0x71: // JNO
                if (op instanceof Inst1.JumpCond16_b_no) {
                    Inst1.JumpCond16_b_no o = (Inst1.JumpCond16_b_no) op;
                    compile(o, "Flags.TFLG_NO()", method);
                    return false;
                }
                break;
            case 0x72: // JB
                if (op instanceof Inst1.JumpCond16_b_b) {
                    Inst1.JumpCond16_b_b o = (Inst1.JumpCond16_b_b) op;
                    compile(o, "Flags.TFLG_B()", method);
                    return false;
                }
                break;
            case 0x73: // JNB
                if (op instanceof Inst1.JumpCond16_b_nb) {
                    Inst1.JumpCond16_b_nb o = (Inst1.JumpCond16_b_nb) op;
                    compile(o, "Flags.TFLG_NB()", method);
                    return false;
                }
                break;
            case 0x74: // JZ
                if (op instanceof Inst1.JumpCond16_b_z) {
                    Inst1.JumpCond16_b_z o = (Inst1.JumpCond16_b_z) op;
                    compile(o, "Flags.TFLG_Z()", method);
                    return false;
                }
                break;
            case 0x75: // JNZ
                if (op instanceof Inst1.JumpCond16_b_nz) {
                    Inst1.JumpCond16_b_nz o = (Inst1.JumpCond16_b_nz) op;
                    compile(o, "Flags.TFLG_NZ()", method);
                    return false;
                }
                break;
            case 0x76: // JBE
                if (op instanceof Inst1.JumpCond16_b_be) {
                    Inst1.JumpCond16_b_be o = (Inst1.JumpCond16_b_be) op;
                    compile(o, "Flags.TFLG_BE()", method);
                    return false;
                }
                break;
            case 0x77: // JNBE
                if (op instanceof Inst1.JumpCond16_b_nbe) {
                    Inst1.JumpCond16_b_nbe o = (Inst1.JumpCond16_b_nbe) op;
                    compile(o, "Flags.TFLG_NBE()", method);
                    return false;
                }
                break;
            case 0x78: // JS
                if (op instanceof Inst1.JumpCond16_b_s) {
                    Inst1.JumpCond16_b_s o = (Inst1.JumpCond16_b_s) op;
                    compile(o, "Flags.TFLG_S()", method);
                    return false;
                }
                break;
            case 0x79: // JNS
                if (op instanceof Inst1.JumpCond16_b_ns) {
                    Inst1.JumpCond16_b_ns o = (Inst1.JumpCond16_b_ns) op;
                    compile(o, "Flags.TFLG_NS()", method);
                    return false;
                }
                break;
            case 0x7a: // JP
                if (op instanceof Inst1.JumpCond16_b_p) {
                    Inst1.JumpCond16_b_p o = (Inst1.JumpCond16_b_p) op;
                    compile(o, "Flags.TFLG_P()", method);
                    return false;
                }
                break;
            case 0x7b: // JNP
                if (op instanceof Inst1.JumpCond16_b_np) {
                    Inst1.JumpCond16_b_np o = (Inst1.JumpCond16_b_np) op;
                    compile(o, "Flags.TFLG_NP()", method);
                    return false;
                }
                break;
            case 0x7c: // JL
                if (op instanceof Inst1.JumpCond16_b_l) {
                    Inst1.JumpCond16_b_l o = (Inst1.JumpCond16_b_l) op;
                    compile(o, "Flags.TFLG_L()", method);
                    return false;
                }
                break;
            case 0x7d: // JNL
                if (op instanceof Inst1.JumpCond16_b_nl) {
                    Inst1.JumpCond16_b_nl o = (Inst1.JumpCond16_b_nl) op;
                    compile(o, "Flags.TFLG_NL()", method);
                    return false;
                }
                break;
            case 0x7e: // JLE
                if (op instanceof Inst1.JumpCond16_b_le) {
                    Inst1.JumpCond16_b_le o = (Inst1.JumpCond16_b_le) op;
                    compile(o, "Flags.TFLG_LE()", method);
                    return false;
                }
                break;
            case 0x7f: // JNLE
                if (op instanceof Inst1.JumpCond16_b_nle) {
                    Inst1.JumpCond16_b_nle o = (Inst1.JumpCond16_b_nle) op;
                    compile(o, "Flags.TFLG_NLE()", method);
                    return false;
                }
                break;
            case 0x80: // Grpl Eb,Ib
            case 0x280:
                if (op instanceof Inst1.GrplEbIb_reg_add) {
                    Inst1.GrplEbIb_reg_add o = (Inst1.GrplEbIb_reg_add) op;
                    method.append(nameSet8(o.earb, "Instructions.ADDB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_or) {
                    Inst1.GrplEbIb_reg_or o = (Inst1.GrplEbIb_reg_or) op;
                    method.append(nameSet8(o.earb, "Instructions.ORB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_adc) {
                    Inst1.GrplEbIb_reg_adc o = (Inst1.GrplEbIb_reg_adc) op;
                    method.append(nameSet8(o.earb, "Instructions.ADCB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_sbb) {
                    Inst1.GrplEbIb_reg_sbb o = (Inst1.GrplEbIb_reg_sbb) op;
                    method.append(nameSet8(o.earb, "Instructions.SBBB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_and) {
                    Inst1.GrplEbIb_reg_and o = (Inst1.GrplEbIb_reg_and) op;
                    method.append(nameSet8(o.earb, "Instructions.ANDB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_sub) {
                    Inst1.GrplEbIb_reg_sub o = (Inst1.GrplEbIb_reg_sub) op;
                    method.append(nameSet8(o.earb, "Instructions.SUBB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_xor) {
                    Inst1.GrplEbIb_reg_xor o = (Inst1.GrplEbIb_reg_xor) op;
                    method.append(nameSet8(o.earb, "Instructions.XORB((short)" + o.ib + "," + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_cmp) {
                    Inst1.GrplEbIb_reg_cmp o = (Inst1.GrplEbIb_reg_cmp) op;
                    method.append("Instructions.CMPB((short)");
                    method.append(o.ib);
                    method.append(",");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_add) {
                    Inst1.GrplEbIb_mem_add o = (Inst1.GrplEbIb_mem_add) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ADDB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_or) {
                    Inst1.GrplEbIb_mem_or o = (Inst1.GrplEbIb_mem_or) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ORB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_adc) {
                    Inst1.GrplEbIb_mem_adc o = (Inst1.GrplEbIb_mem_adc) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ADCB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_sbb) {
                    Inst1.GrplEbIb_mem_sbb o = (Inst1.GrplEbIb_mem_sbb) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.SBBB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_and) {
                    Inst1.GrplEbIb_mem_and o = (Inst1.GrplEbIb_mem_and) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.ANDB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_sub) {
                    Inst1.GrplEbIb_mem_sub o = (Inst1.GrplEbIb_mem_sub) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.SUBB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_xor) {
                    Inst1.GrplEbIb_mem_xor o = (Inst1.GrplEbIb_mem_xor) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.XORB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_cmp) {
                    Inst1.GrplEbIb_mem_cmp o = (Inst1.GrplEbIb_mem_cmp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.CMPB((short)");
                    method.append(o.ib);
                    method.append(",Memory.mem_readb(eaa));");
                    return true;
                }
                break;
            case 0x82: // Grpl Eb,Ib Mirror instruction*/
            case 0x282:
                break;
            case 0x81: // Grpl Ew,Iw
                if (op instanceof Inst1.GrplEwIw_reg_add) {
                    Inst1.GrplEwIw_reg_add o = (Inst1.GrplEwIw_reg_add) op;
                    method.append(nameSet16(o.earw, "Instructions.ADDW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_or) {
                    Inst1.GrplEwIw_reg_or o = (Inst1.GrplEwIw_reg_or) op;
                    method.append(nameSet16(o.earw, "Instructions.ORW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_adc) {
                    Inst1.GrplEwIw_reg_adc o = (Inst1.GrplEwIw_reg_adc) op;
                    method.append(nameSet16(o.earw, "Instructions.ADCW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sbb) {
                    Inst1.GrplEwIw_reg_sbb o = (Inst1.GrplEwIw_reg_sbb) op;
                    method.append(nameSet16(o.earw, "Instructions.SBBW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_and) {
                    Inst1.GrplEwIw_reg_and o = (Inst1.GrplEwIw_reg_and) op;
                    method.append(nameSet16(o.earw, "Instructions.ANDW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sub) {
                    Inst1.GrplEwIw_reg_sub o = (Inst1.GrplEwIw_reg_sub) op;
                    method.append(nameSet16(o.earw, "Instructions.SUBW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_xor) {
                    Inst1.GrplEwIw_reg_xor o = (Inst1.GrplEwIw_reg_xor) op;
                    method.append(nameSet16(o.earw, "Instructions.XORW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_cmp) {
                    Inst1.GrplEwIw_reg_cmp o = (Inst1.GrplEwIw_reg_cmp) op;
                    method.append("Instructions.CMPW(");
                    method.append(o.ib);
                    method.append(",");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_add) {
                    Inst1.GrplEwIw_mem_add o = (Inst1.GrplEwIw_mem_add) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ADDW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_or) {
                    Inst1.GrplEwIw_mem_or o = (Inst1.GrplEwIw_mem_or) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ORW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_adc) {
                    Inst1.GrplEwIw_mem_adc o = (Inst1.GrplEwIw_mem_adc) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ADCW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sbb) {
                    Inst1.GrplEwIw_mem_sbb o = (Inst1.GrplEwIw_mem_sbb) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.SBBW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_and) {
                    Inst1.GrplEwIw_mem_and o = (Inst1.GrplEwIw_mem_and) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ANDW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sub) {
                    Inst1.GrplEwIw_mem_sub o = (Inst1.GrplEwIw_mem_sub) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.SUBW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_xor) {
                    Inst1.GrplEwIw_mem_xor o = (Inst1.GrplEwIw_mem_xor) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.XORW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_cmp) {
                    Inst1.GrplEwIw_mem_cmp o = (Inst1.GrplEwIw_mem_cmp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.CMPW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x83: // Grpl Ew,Ix
                if (op instanceof Inst1.GrplEwIw_reg_add) {
                    Inst1.GrplEwIw_reg_add o = (Inst1.GrplEwIw_reg_add) op;
                    method.append(nameSet16(o.earw, "Instructions.ADDW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_or) {
                    Inst1.GrplEwIw_reg_or o = (Inst1.GrplEwIw_reg_or) op;
                    method.append(nameSet16(o.earw, "Instructions.ORW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_adc) {
                    Inst1.GrplEwIw_reg_adc o = (Inst1.GrplEwIw_reg_adc) op;
                    method.append(nameSet16(o.earw, "Instructions.ADCW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sbb) {
                    Inst1.GrplEwIw_reg_sbb o = (Inst1.GrplEwIw_reg_sbb) op;
                    method.append(nameSet16(o.earw, "Instructions.SBBW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_and) {
                    Inst1.GrplEwIw_reg_and o = (Inst1.GrplEwIw_reg_and) op;
                    method.append(nameSet16(o.earw, "Instructions.ANDW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sub) {
                    Inst1.GrplEwIw_reg_sub o = (Inst1.GrplEwIw_reg_sub) op;
                    method.append(nameSet16(o.earw, "Instructions.SUBW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_xor) {
                    Inst1.GrplEwIw_reg_xor o = (Inst1.GrplEwIw_reg_xor) op;
                    method.append(nameSet16(o.earw, "Instructions.XORW(" + o.ib + "," + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_cmp) {
                    Inst1.GrplEwIw_reg_cmp o = (Inst1.GrplEwIw_reg_cmp) op;
                    method.append("Instructions.CMPW(");
                    method.append(o.ib);
                    method.append(",");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_add) {
                    Inst1.GrplEwIw_mem_add o = (Inst1.GrplEwIw_mem_add) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ADDW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_or) {
                    Inst1.GrplEwIw_mem_or o = (Inst1.GrplEwIw_mem_or) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ORW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_adc) {
                    Inst1.GrplEwIw_mem_adc o = (Inst1.GrplEwIw_mem_adc) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ADCW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sbb) {
                    Inst1.GrplEwIw_mem_sbb o = (Inst1.GrplEwIw_mem_sbb) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.SBBW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_and) {
                    Inst1.GrplEwIw_mem_and o = (Inst1.GrplEwIw_mem_and) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.ANDW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sub) {
                    Inst1.GrplEwIw_mem_sub o = (Inst1.GrplEwIw_mem_sub) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.SUBW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_xor) {
                    Inst1.GrplEwIw_mem_xor o = (Inst1.GrplEwIw_mem_xor) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.XORW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_cmp) {
                    Inst1.GrplEwIw_mem_cmp o = (Inst1.GrplEwIw_mem_cmp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.CMPW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x84: // TEST Eb,Gb
            case 0x284:
                if (op instanceof Inst1.TestEbGb_reg) {
                    Inst1.TestEbGb_reg o = (Inst1.TestEbGb_reg) op;
                    method.append("Instructions.TESTB(");
                    method.append(nameGet8(o.rb));
                    method.append(",");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.TestEbGb_mem) {
                    Inst1.TestEbGb_mem o = (Inst1.TestEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.TESTB(");
                    method.append(nameGet8(o.rb));
                    method.append(",Memory.mem_readb(eaa));");
                    return true;
                }
                break;
            case 0x85: // TEST Ew,Gw
                if (op instanceof Inst1.TestEwGw_reg) {
                    Inst1.TestEwGw_reg o = (Inst1.TestEwGw_reg) op;
                    method.append("Instructions.TESTW(");
                    method.append(nameGet16(o.rw));
                    method.append(",");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.TestEwGw_mem) {
                    Inst1.TestEwGw_mem o = (Inst1.TestEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.TESTW(");
                    method.append(nameGet16(o.rw));
                    method.append(",Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x86: // XCHG Eb,Gb
            case 0x286:
                if (op instanceof Inst1.XchgEbGb_reg) {
                    Inst1.XchgEbGb_reg o = (Inst1.XchgEbGb_reg) op;
                    method.append("short oldrmrb = ");
                    method.append(nameGet8(o.rb));
                    method.append(";");
                    method.append(nameSet8(o.rb, nameGet8(o.earb)));
                    method.append(";");
                    method.append(nameSet8(o.earb, "oldrmrb"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XchgEbGb_mem) {
                    Inst1.XchgEbGb_mem o = (Inst1.XchgEbGb_mem) op;
                    method.append("short oldrmrb = ");
                    method.append(nameGet8(o.rb));
                    method.append(";int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";short newrb = Memory.mem_readb(eaa);Memory.mem_writeb(eaa,oldrmrb);");
                    method.append(nameSet8(o.rb, "newrb"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x87: // XCHG Ew,Gw
                if (op instanceof Inst1.XchgEwGw_reg) {
                    Inst1.XchgEwGw_reg o = (Inst1.XchgEwGw_reg) op;
                    method.append("int oldrmrw = ");
                    method.append(nameGet16(o.rw));
                    method.append(";");
                    method.append(nameSet16(o.rw, nameGet16(o.earw)));
                    method.append(";");
                    method.append(nameSet16(o.earw, "oldrmrw"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XchgEwGw_mem) {
                    Inst1.XchgEwGw_mem o = (Inst1.XchgEwGw_mem) op;
                    method.append("int oldrmrw = ");
                    method.append(nameGet16(o.rw));
                    method.append(";int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int newrw = Memory.mem_readw(eaa);Memory.mem_writew(eaa,oldrmrw);");
                    method.append(nameSet16(o.rw, "newrw"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x88: // MOV Eb,Gb
            case 0x288:
                if (op instanceof Inst1.MovEbGb_reg) {
                    Inst1.MovEbGb_reg o = (Inst1.MovEbGb_reg) op;
                    method.append(nameSet8(o.earb, nameGet8(o.rb)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEbGb_mem_5) {
                    Inst1.MovEbGb_mem_5 o = (Inst1.MovEbGb_mem_5) op;
                    method.append("if (CPU.cpu.pmode && !CPU.cpu.code.big) {jdos.cpu.CPU.Descriptor desc=new jdos.cpu.CPU.Descriptor();CPU.cpu.gdt.GetDescriptor(CPU.seg_value(Core.base_val_ds),desc);if ((desc.Type()==CPU.DESC_CODE_R_NC_A) || (desc.Type()==CPU.DESC_CODE_R_NC_NA)) {CPU.CPU_Exception(CPU.EXCEPTION_GP,CPU.seg_value(Core.base_val_ds) & 0xfffc);return Constants.BR_Jump;}}");
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa,");
                    method.append(nameGet8(o.rb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.MovEbGb_mem) {
                    Inst1.MovEbGb_mem o = (Inst1.MovEbGb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa,");
                    method.append(nameGet8(o.rb));
                    method.append(");");
                    return true;
                }
                break;
            case 0x89: // MOV Ew,Gw
                if (op instanceof Inst1.MovEwGw_reg) {
                    Inst1.MovEwGw_reg o = (Inst1.MovEwGw_reg) op;
                    method.append(nameSet16(o.earw, nameGet16(o.rw)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwGw_mem) {
                    Inst1.MovEwGw_mem o = (Inst1.MovEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa,");
                    method.append(nameGet16(o.rw));
                    method.append(");");
                    return true;
                }
                break;
            case 0x8a: // MOV Gb,Eb
            case 0x28a:
                if (op instanceof Inst1.MovGbEb_reg) {
                    Inst1.MovGbEb_reg o = (Inst1.MovGbEb_reg) op;
                    method.append(nameSet8(o.rb, nameGet8(o.earb)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovGbEb_mem) {
                    Inst1.MovGbEb_mem o = (Inst1.MovGbEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet8(o.rb, "Memory.mem_readb(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x8b: // MOV Gw,Ew
                if (op instanceof Inst1.MovGwEw_reg) {
                    Inst1.MovGwEw_reg o = (Inst1.MovGwEw_reg) op;
                    method.append(nameSet16(o.rw, nameGet16(o.earw)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovGwEw_mem) {
                    Inst1.MovGwEw_mem o = (Inst1.MovGwEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x8c: // Mov Ew,Sw
                if (op instanceof Inst1.MovEwEs_reg) {
                    Inst1.MovEwEs_reg o = (Inst1.MovEwEs_reg) op;
                    method.append(nameSet16(o.earw, "CPU.Segs_ESval"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwEs_mem) {
                    Inst1.MovEwEs_mem o = (Inst1.MovEwEs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_ESval);");
                    return true;
                }
                if (op instanceof Inst1.MovEwCs_reg) {
                    Inst1.MovEwCs_reg o = (Inst1.MovEwCs_reg) op;
                    method.append(nameSet16(o.earw, "CPU.Segs_CSval"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwCs_mem) {
                    Inst1.MovEwCs_mem o = (Inst1.MovEwCs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_CSval);");
                    return true;
                }
                if (op instanceof Inst1.MovEwSs_reg) {
                    Inst1.MovEwSs_reg o = (Inst1.MovEwSs_reg) op;
                    method.append(nameSet16(o.earw, "CPU.Segs_SSval"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwSs_mem) {
                    Inst1.MovEwSs_mem o = (Inst1.MovEwSs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_SSval);");
                    return true;
                }
                if (op instanceof Inst1.MovEwDs_reg) {
                    Inst1.MovEwDs_reg o = (Inst1.MovEwDs_reg) op;
                    method.append(nameSet16(o.earw, "CPU.Segs_DSval"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwDs_mem) {
                    Inst1.MovEwDs_mem o = (Inst1.MovEwDs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_DSval);");
                    return true;
                }
                if (op instanceof Inst1.MovEwFs_reg) {
                    Inst1.MovEwFs_reg o = (Inst1.MovEwFs_reg) op;
                    method.append(nameSet16(o.earw, "CPU.Segs_FSval"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwFs_mem) {
                    Inst1.MovEwFs_mem o = (Inst1.MovEwFs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_FSval);");
                    return true;
                }
                if (op instanceof Inst1.MovEwGs_reg) {
                    Inst1.MovEwGs_reg o = (Inst1.MovEwGs_reg) op;
                    method.append(nameSet16(o.earw, "CPU.Segs_GSval"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwGs_mem) {
                    Inst1.MovEwGs_mem o = (Inst1.MovEwGs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_GSval);");
                    return true;
                }
                break;
            case 0x8d: // LEA Gw
                if (op instanceof Inst1.LeaGw_32) {
                    Inst1.LeaGw_32 o = (Inst1.LeaGw_32) op;
                    method.append("Core.base_ds=Core.base_ss=0;int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "eaa"));
                    method.append(";Core.base_ds=CPU.Segs_DSphys;Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                if (op instanceof Inst1.LeaGw_16) {
                    Inst1.LeaGw_16 o = (Inst1.LeaGw_16) op;
                    method.append("Core.base_ds=Core.base_ss=0;int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "eaa"));
                    method.append(";Core.base_ds=CPU.Segs_DSphys;Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                break;
            case 0x8e: // MOV Sw,Ew
            case 0x28e:
                if (op instanceof Inst1.MovEsEw_reg) {
                    Inst1.MovEsEw_reg o = (Inst1.MovEsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralES(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst1.MovEsEw_mem) {
                    Inst1.MovEsEw_mem o = (Inst1.MovEsEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst1.MovSsEw_reg) {
                    Inst1.MovSsEw_reg o = (Inst1.MovSsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralSS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                if (op instanceof Inst1.MovSsEw_mem) {
                    Inst1.MovSsEw_mem o = (Inst1.MovSsEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa))) return RUNEXCEPTION();Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                if (op instanceof Inst1.MovDsEw_reg) {
                    Inst1.MovDsEw_reg o = (Inst1.MovDsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralDS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();Core.base_ds=CPU.Segs_DSphys;Core.base_val_ds= CPU_Regs.ds;");
                    return true;
                }
                if (op instanceof Inst1.MovDsEw_mem) {
                    Inst1.MovDsEw_mem o = (Inst1.MovDsEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa))) return RUNEXCEPTION();Core.base_ds=CPU.Segs_DSphys;Core.base_val_ds= CPU_Regs.ds;");
                    return true;
                }
                if (op instanceof Inst1.MovFsEw_reg) {
                    Inst1.MovFsEw_reg o = (Inst1.MovFsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralFS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst1.MovFsEw_mem) {
                    Inst1.MovFsEw_mem o = (Inst1.MovFsEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst1.MovGsEw_reg) {
                    Inst1.MovGsEw_reg o = (Inst1.MovGsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralGS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst1.MovGsEw_mem) {
                    Inst1.MovGsEw_mem o = (Inst1.MovGsEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x8f: // POP Ew
                if (op instanceof Inst1.PopEw_reg) {
                    Inst1.PopEw_reg o = (Inst1.PopEw_reg) op;
                    method.append(nameSet16(o.earw, "CPU.CPU_Pop16()"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.PopEw_mem) {
                    Inst1.PopEw_mem o = (Inst1.PopEw_mem) op;
                    method.append("int val = CPU.CPU_Pop16();int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, val);");
                    return true;
                }
                break;
            case 0x90: // NOP
            case 0x290:
                if (op instanceof Inst1.Noop) {
                    Inst1.Noop o = (Inst1.Noop) op;
                    return true;
                }
                break;
            case 0x91: // XCHG CX,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x92: // XCHG DX,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x93: // XCHG BX,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x94: // XCHG SP,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x95: // XCHG BP,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x96: // XCHG SI,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x97: // XCHG DI,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    method.append("int old = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    method.append(nameSet16(o.reg, "CPU_Regs.reg_eax.word()"));
                    method.append(";CPU_Regs.reg_eax.word(old);");
                    return true;
                }
                break;
            case 0x98: // CBW
                if (op instanceof Inst1.Cbw) {
                    Inst1.Cbw o = (Inst1.Cbw) op;
                    method.append("CPU_Regs.reg_eax.word((byte)CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0x99: // CWD
                if (op instanceof Inst1.Cwd) {
                    Inst1.Cwd o = (Inst1.Cwd) op;
                    method.append("if ((CPU_Regs.reg_eax.word() & 0x8000)!=0) CPU_Regs.reg_edx.word(0xffff);else CPU_Regs.reg_edx.word(0);");
                    return true;
                }
                break;
            case 0x9a: // CALL Ap
                if (op instanceof Inst1.CallAp) {
                    Inst1.CallAp o = (Inst1.CallAp) op;
                    method.append("Flags.FillFlags();CPU.CPU_CALL(false,");
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
            case 0x9b: // WAIT
                break;
            case 0x29b: // No waiting here
//                if (op instanceof Inst1.Wait) {
//                    Inst1.Wait o = (Inst1.Wait) op;
//                    return true;
//                }
                break;
            case 0x9c: // PUSHF
                if (op instanceof Inst1.PushF) {
                    Inst1.PushF o = (Inst1.PushF) op;
                    method.append("if (CPU.CPU_PUSHF(false)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x9d: // POPF
                if (op instanceof Inst1.PopF) {
                    Inst1.PopF o = (Inst1.PopF) op;
                    method.append("if (CPU.CPU_POPF(false)) return RUNEXCEPTION();");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return DECODE_END(");
                        method.append(o.eip_count);
                        method.append(");}");
                    }
                    if (CPU_PIC_CHECK) {
                        method.append(" if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END(");
                        method.append(o.eip_count);
                        method.append(");");
                    }
                    return true;
                }
                break;
            case 0x9e: // SAHF
            case 0x29e:
                if (op instanceof Inst1.Sahf) {
                    Inst1.Sahf o = (Inst1.Sahf) op;
                    method.append("Flags.SETFLAGSb(CPU_Regs.reg_eax.high());");
                    return true;
                }
                break;
            case 0x9f: // LAHF
            case 0x29f:
                if (op instanceof Inst1.Lahf) {
                    Inst1.Lahf o = (Inst1.Lahf) op;
                    method.append("Flags.FillFlags();CPU_Regs.reg_eax.high(CPU_Regs.flags & 0xff);");
                    return true;
                }
                break;
            case 0xa0: // MOV AL,Ob
            case 0x2a0:
                if (op instanceof Inst1.MovALOb) {
                    Inst1.MovALOb o = (Inst1.MovALOb) op;
                    method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(Core.base_ds+");
                    method.append(o.value);
                    method.append("));");
                    return true;
                }
                break;
            case 0xa1: // MOV AX,Ow
                if (op instanceof Inst1.MovAXOw) {
                    Inst1.MovAXOw o = (Inst1.MovAXOw) op;
                    method.append("CPU_Regs.reg_eax.word(Memory.mem_readw(Core.base_ds+");
                    method.append(o.value);
                    method.append("));");
                    return true;
                }
                break;
            case 0xa2: // MOV Ob,AL
            case 0x2a2:
                if (op instanceof Inst1.MovObAL) {
                    Inst1.MovObAL o = (Inst1.MovObAL) op;
                    method.append("Memory.mem_writeb(Core.base_ds+");
                    method.append(o.value);
                    method.append(", CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0xa3: // MOV Ow,AX
                if (op instanceof Inst1.MovOwAX) {
                    Inst1.MovOwAX o = (Inst1.MovOwAX) op;
                    method.append("Memory.mem_writew(Core.base_ds+");
                    method.append(o.value);
                    method.append(", CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0xa4: // MOVSB
            case 0x2a4:
                if (op instanceof Strings.Movsb16) {
                    method.append("Strings.Movsb16.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsb16r) {
                    method.append("Strings.Movsb16r.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsb32) {
                    method.append("Strings.Movsb32.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsb32r) {
                    method.append("Strings.Movsb32r.doString();");
                    return true;
                }
                break;
            case 0xa5: // MOVSW
                if (op instanceof Strings.Movsw16) {
                    method.append("Strings.Movsw16.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsw16r) {
                    method.append("Strings.Movsw16r.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsw32) {
                    method.append("Strings.Movsw32.doString();");
                    return true;
                }
                if (op instanceof Strings.Movsw32r) {
                    method.append("Strings.Movsw32r.doString();");
                    return true;
                }
                break;
            case 0xa6: // CMPSB
            case 0x2a6:
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xa7: // CMPSW
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xa8: // TEST AL,Ib
            case 0x2a8:
                if (op instanceof Inst1.TestAlIb) {
                    Inst1.TestAlIb o = (Inst1.TestAlIb) op;
                    method.append("Instructions.TESTB((short)");
                    method.append(o.ib);
                    method.append(",CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0xa9: // TEST AX,Iw
                if (op instanceof Inst1.TestAxIw) {
                    Inst1.TestAxIw o = (Inst1.TestAxIw) op;
                    method.append("Instructions.TESTW(");
                    method.append(o.iw);
                    method.append(",CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0xaa: // STOSB
            case 0x2aa:
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xab: // STOSW
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xac: // LODSB
            case 0x2ac:
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xad: // LODSW
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xae: // SCASB
            case 0x2ae:
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xaf: // SCASW
                if (op instanceof Inst1.DoString) {
                    Inst1.DoString o = (Inst1.DoString) op;
                    method.append("Core.rep_zero = ");
                    method.append(o.rep_zero);
                    method.append(";StringOp.DoString(");
                    method.append(o.prefixes);
                    method.append(", ");
                    method.append(o.type);
                    method.append(");");
                    return true;
                }
                break;
            case 0xb0: // MOV AL,Ib
            case 0x2b0:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb1: // MOV CL,Ib
            case 0x2b1:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb2: // MOV DL,Ib
            case 0x2b2:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb3: // MOV BL,Ib
            case 0x2b3:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb4: // MOV AH,Ib
            case 0x2b4:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb5: // MOV CH,Ib
            case 0x2b5:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb6: // MOV DH,Ib
            case 0x2b6:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb7: // MOV BH,Ib
            case 0x2b7:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb8: // MOV AX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb9: // MOV CX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xba: // MOV DX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xbb: // MOV BX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xbc: // MOV SP,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xbd: // MOV BP.Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xbe: // MOV SI,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xbf: // MOV DI,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xc0: // GRP2 Eb,Ib
            case 0x2c0:
                if (op instanceof Grp2.ROLB_reg) {
                    Grp2.ROLB_reg o = (Grp2.ROLB_reg) op;
                    method.append("short e = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_ROLB(e, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_ROLB(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORB_reg) {
                    Grp2.RORB_reg o = (Grp2.RORB_reg) op;
                    method.append("short e = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_RORB(e, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_RORB(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLB_reg) {
                    Grp2.RCLB_reg o = (Grp2.RCLB_reg) op;
                    if (Instructions.valid_RCLB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_RCLB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRB_reg) {
                    Grp2.RCRB_reg o = (Grp2.RCRB_reg) op;
                    if (Instructions.valid_RCRB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_RCRB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLB_reg) {
                    Grp2.SHLB_reg o = (Grp2.SHLB_reg) op;
                    if (Instructions.valid_SHLB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_SHLB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRB_reg) {
                    Grp2.SHRB_reg o = (Grp2.SHRB_reg) op;
                    if (Instructions.valid_SHRB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_SHRB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARB_reg) {
                    Grp2.SARB_reg o = (Grp2.SARB_reg) op;
                    if (Instructions.valid_SARB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_SARB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLB_mem) {
                    Grp2.ROLB_mem o = (Grp2.ROLB_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_ROLB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_ROLB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORB_mem) {
                    Grp2.RORB_mem o = (Grp2.RORB_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_RORB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_RORB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLB_mem) {
                    Grp2.RCLB_mem o = (Grp2.RCLB_mem) op;
                    if (Instructions.valid_RCLB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_RCLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRB_mem) {
                    Grp2.RCRB_mem o = (Grp2.RCRB_mem) op;
                    if (Instructions.valid_RCRB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_RCRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLB_mem) {
                    Grp2.SHLB_mem o = (Grp2.SHLB_mem) op;
                    if (Instructions.valid_SHLB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_SHLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRB_mem) {
                    Grp2.SHRB_mem o = (Grp2.SHRB_mem) op;
                    if (Instructions.valid_SHRB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_SHRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARB_mem) {
                    Grp2.SARB_mem o = (Grp2.SARB_mem) op;
                    if (Instructions.valid_SARB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_SARB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                break;
            case 0xc1: // GRP2 Ew,Ib
                if (op instanceof Grp2.ROLW_reg) {
                    Grp2.ROLW_reg o = (Grp2.ROLW_reg) op;
                    method.append("int e = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_ROLW(e, ");
                    method.append(o.val);
                    method.append(")) ");
                    method.append(nameSet16(o.earw, "Instructions.do_ROLW(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORW_reg) {
                    Grp2.RORW_reg o = (Grp2.RORW_reg) op;
                    method.append("int e = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_RORW(e, ");
                    method.append(o.val);
                    method.append(")) ");
                    method.append(nameSet16(o.earw, "Instructions.do_RORW(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLW_reg) {
                    Grp2.RCLW_reg o = (Grp2.RCLW_reg) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_RCLW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_reg) {
                    Grp2.RCRW_reg o = (Grp2.RCRW_reg) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_RCRW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_reg) {
                    Grp2.SHLW_reg o = (Grp2.SHLW_reg) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_SHLW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_reg) {
                    Grp2.SHRW_reg o = (Grp2.SHRW_reg) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_SHRW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_reg) {
                    Grp2.SARW_reg o = (Grp2.SARW_reg) op;
                    if (Instructions.valid_SARW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_SARW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLW_mem) {
                    Grp2.ROLW_mem o = (Grp2.ROLW_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_ROLW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_ROLW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORW_mem) {
                    Grp2.RORW_mem o = (Grp2.RORW_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_RORW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_RORW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLW_mem) {
                    Grp2.RCLW_mem o = (Grp2.RCLW_mem) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_RCLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_mem) {
                    Grp2.RCRW_mem o = (Grp2.RCRW_mem) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_RCRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_mem) {
                    Grp2.SHLW_mem o = (Grp2.SHLW_mem) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_SHLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_mem) {
                    Grp2.SHRW_mem o = (Grp2.SHRW_mem) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_SHRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_mem) {
                    Grp2.SARW_mem o = (Grp2.SARW_mem) op;
                    if (Instructions.valid_SARW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_SARW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                break;
            case 0xc2: // RETN Iw
                if (op instanceof Inst1.RetnIw) {
                    Inst1.RetnIw o = (Inst1.RetnIw) op;
                    method.append("CPU_Regs.reg_eip=CPU.CPU_Pop16();CPU_Regs.reg_esp.dword+=");
                    method.append(o.offset);
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xc3: // RETN
                if (op instanceof Inst1.Retn) {
                    Inst1.Retn o = (Inst1.Retn) op;
                    method.append("CPU_Regs.reg_eip=CPU.CPU_Pop16();return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xc4: // LES
                if (op instanceof Inst1.Les) {
                    Inst1.Les o = (Inst1.Les) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0xc5: // LDS
                if (op instanceof Inst1.Lds) {
                    Inst1.Lds o = (Inst1.Lds) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";Core.base_ds=CPU.Segs_DSphys;Core.base_val_ds= CPU_Regs.ds;");
                    return true;
                }
                break;
            case 0xc6: // MOV Eb,Ib
            case 0x2c6:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, "(short)" + o.ib));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovIb_mem) {
                    Inst1.MovIb_mem o = (Inst1.MovIb_mem) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", (short)");
                    method.append(o.ib);
                    method.append(");");
                    return true;
                }
                break;
            case 0xc7: // MOV EW,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    method.append(nameSet16(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovIw_mem) {
                    Inst1.MovIw_mem o = (Inst1.MovIw_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ");
                    method.append(o.ib);
                    method.append(");");
                    return true;
                }
                break;
            case 0xc8: // ENTER Iw,Ib
                if (op instanceof Inst1.EnterIwIb) {
                    Inst1.EnterIwIb o = (Inst1.EnterIwIb) op;
                    method.append("CPU.CPU_ENTER(false,");
                    method.append(o.bytes);
                    method.append(",");
                    method.append(o.level);
                    method.append(");");
                    return true;
                }
                break;
            case 0xc9: // LEAVE
                if (op instanceof Inst1.Leave) {
                    Inst1.Leave o = (Inst1.Leave) op;
                    method.append("CPU_Regs.reg_esp.dword&=CPU.cpu.stack.notmask;CPU_Regs.reg_esp.dword|=(CPU_Regs.reg_ebp.dword & CPU.cpu.stack.mask);CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());");
                    return true;
                }
                break;
            case 0xca: // RETF Iw
                if (op instanceof Inst1.RetfIw) {
                    Inst1.RetfIw o = (Inst1.RetfIw) op;
                    method.append("Flags.FillFlags();CPU.CPU_RET(false,");
                    method.append(o.words);
                    method.append(",CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xcb: // RETF
                if (op instanceof Inst1.Retf) {
                    Inst1.Retf o = (Inst1.Retf) op;
                    method.append("Flags.FillFlags();CPU.CPU_RET(false,0,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xcc: // INT3
            case 0x2cc:
                if (op instanceof Inst1.Int3) {
                    Inst1.Int3 o = (Inst1.Int3) op;
                    method.append("CPU.CPU_SW_Interrupt_NoIOPLCheck(3,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("CPU.cpu.trap_skip=true;");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xcd: // INT o.ib
            case 0x2cd:
                if (op instanceof Inst1.IntIb) {
                    Inst1.IntIb o = (Inst1.IntIb) op;
                    method.append("CPU.CPU_SW_Interrupt(");
                    method.append(o.num);
                    method.append(", CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("CPU.cpu.trap_skip=true;");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xce: // INTO
            case 0x2ce:
                if (op instanceof Inst1.Int0) {
                    Inst1.Int0 o = (Inst1.Int0) op;
                    method.append("if (Flags.get_OF()) {CPU.CPU_SW_Interrupt(4,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK) {
                        method.append("CPU.cpu.trap_skip=true;");
                    }
                    method.append("return Constants.BR_Jump;}");
                    return true;
                }
                break;
            case 0xcf: // IRET
                if (op instanceof Inst1.IRet) {
                    Inst1.IRet o = (Inst1.IRet) op;
                    method.append("CPU.CPU_IRET(false, CPU_Regs.reg_eip+");
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
            case 0xd0: // GRP2 Eb,1
            case 0x2d0:
                if (op instanceof Grp2.ROLB_reg) {
                    Grp2.ROLB_reg o = (Grp2.ROLB_reg) op;
                    method.append("short e = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_ROLB(e, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_ROLB(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORB_reg) {
                    Grp2.RORB_reg o = (Grp2.RORB_reg) op;
                    method.append("short e = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_RORB(e, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_RORB(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLB_reg) {
                    Grp2.RCLB_reg o = (Grp2.RCLB_reg) op;
                    if (Instructions.valid_RCLB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_RCLB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRB_reg) {
                    Grp2.RCRB_reg o = (Grp2.RCRB_reg) op;
                    if (Instructions.valid_RCRB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_RCRB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLB_reg) {
                    Grp2.SHLB_reg o = (Grp2.SHLB_reg) op;
                    if (Instructions.valid_SHLB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_SHLB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRB_reg) {
                    Grp2.SHRB_reg o = (Grp2.SHRB_reg) op;
                    if (Instructions.valid_SHRB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_SHRB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARB_reg) {
                    Grp2.SARB_reg o = (Grp2.SARB_reg) op;
                    if (Instructions.valid_SARB(o.val)) {
                        method.append(nameSet8(o.earb, "Instructions.do_SARB(" + o.val + ", " + nameGet8(o.earb) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLB_mem) {
                    Grp2.ROLB_mem o = (Grp2.ROLB_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_ROLB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_ROLB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORB_mem) {
                    Grp2.RORB_mem o = (Grp2.RORB_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_RORB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_RORB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLB_mem) {
                    Grp2.RCLB_mem o = (Grp2.RCLB_mem) op;
                    if (Instructions.valid_RCLB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_RCLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRB_mem) {
                    Grp2.RCRB_mem o = (Grp2.RCRB_mem) op;
                    if (Instructions.valid_RCRB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_RCRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLB_mem) {
                    Grp2.SHLB_mem o = (Grp2.SHLB_mem) op;
                    if (Instructions.valid_SHLB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_SHLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRB_mem) {
                    Grp2.SHRB_mem o = (Grp2.SHRB_mem) op;
                    if (Instructions.valid_SHRB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_SHRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARB_mem) {
                    Grp2.SARB_mem o = (Grp2.SARB_mem) op;
                    if (Instructions.valid_SARB(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writeb(eaa, Instructions.do_SARB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                break;
            case 0xd1: // GRP2 Ew,1
                if (op instanceof Grp2.ROLW_reg) {
                    Grp2.ROLW_reg o = (Grp2.ROLW_reg) op;
                    method.append("int e = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_ROLW(e, ");
                    method.append(o.val);
                    method.append(")) ");
                    method.append(nameSet16(o.earw, "Instructions.do_ROLW(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORW_reg) {
                    Grp2.RORW_reg o = (Grp2.RORW_reg) op;
                    method.append("int e = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_RORW(e, ");
                    method.append(o.val);
                    method.append(")) ");
                    method.append(nameSet16(o.earw, "Instructions.do_RORW(" + o.val + ", e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLW_reg) {
                    Grp2.RCLW_reg o = (Grp2.RCLW_reg) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_RCLW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_reg) {
                    Grp2.RCRW_reg o = (Grp2.RCRW_reg) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_RCRW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_reg) {
                    Grp2.SHLW_reg o = (Grp2.SHLW_reg) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_SHLW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_reg) {
                    Grp2.SHRW_reg o = (Grp2.SHRW_reg) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_SHRW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_reg) {
                    Grp2.SARW_reg o = (Grp2.SARW_reg) op;
                    if (Instructions.valid_SARW(o.val)) {
                        method.append(nameSet16(o.earw, "Instructions.do_SARW(" + o.val + ", " + nameGet16(o.earw) + ")"));
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLW_mem) {
                    Grp2.ROLW_mem o = (Grp2.ROLW_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_ROLW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_ROLW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORW_mem) {
                    Grp2.RORW_mem o = (Grp2.RORW_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (Instructions.valid_RORW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_RORW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLW_mem) {
                    Grp2.RCLW_mem o = (Grp2.RCLW_mem) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_RCLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_mem) {
                    Grp2.RCRW_mem o = (Grp2.RCRW_mem) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_RCRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_mem) {
                    Grp2.SHLW_mem o = (Grp2.SHLW_mem) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_SHLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_mem) {
                    Grp2.SHRW_mem o = (Grp2.SHRW_mem) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_SHRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_mem) {
                    Grp2.SARW_mem o = (Grp2.SARW_mem) op;
                    if (Instructions.valid_SARW(o.val)) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writew(eaa, Instructions.do_SARW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                break;
            case 0xd2: // GRP2 Eb,CL
            case 0x2d2:
                if (op instanceof Grp2.ROLB_reg_cl) {
                    Grp2.ROLB_reg_cl o = (Grp2.ROLB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; short e = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_ROLB(e, val))");
                    method.append(nameSet8(o.earb, "Instructions.do_ROLB(val, e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORB_reg_cl) {
                    Grp2.RORB_reg_cl o = (Grp2.RORB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; short e = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_RORB(e, val))");
                    method.append(nameSet8(o.earb, "Instructions.do_RORB(val, e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLB_reg_cl) {
                    Grp2.RCLB_reg_cl o = (Grp2.RCLB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCLB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_RCLB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCRB_reg_cl) {
                    Grp2.RCRB_reg_cl o = (Grp2.RCRB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCRB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_RCRB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHLB_reg_cl) {
                    Grp2.SHLB_reg_cl o = (Grp2.SHLB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHLB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_SHLB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHRB_reg_cl) {
                    Grp2.SHRB_reg_cl o = (Grp2.SHRB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHRB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_SHRB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SARB_reg_cl) {
                    Grp2.SARB_reg_cl o = (Grp2.SARB_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SARB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_SARB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.ROLB_mem_cl) {
                    Grp2.ROLB_mem_cl o = (Grp2.ROLB_mem_cl) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_ROLB(eaa, val)) Memory.mem_writeb(eaa, Instructions.do_ROLB(val, Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORB_mem_cl) {
                    Grp2.RORB_mem_cl o = (Grp2.RORB_mem_cl) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RORB(eaa, val)) Memory.mem_writeb(eaa, Instructions.do_RORB(val, Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLB_mem_cl) {
                    Grp2.RCLB_mem_cl o = (Grp2.RCLB_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_RCLB (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.do_RCLB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCRB_mem_cl) {
                    Grp2.RCRB_mem_cl o = (Grp2.RCRB_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_RCRB (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.do_RCRB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHLB_mem_cl) {
                    Grp2.SHLB_mem_cl o = (Grp2.SHLB_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_SHLB (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.do_SHLB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHRB_mem_cl) {
                    Grp2.SHRB_mem_cl o = (Grp2.SHRB_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_SHRB (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.do_SHRB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SARB_mem_cl) {
                    Grp2.SARB_mem_cl o = (Grp2.SARB_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_SARB (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.do_SARB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                break;
            case 0xd3: // GRP2 Ew,CL
                if (op instanceof Grp2.ROLW_reg_cl) {
                    Grp2.ROLW_reg_cl o = (Grp2.ROLW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;int e = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_ROLW(e, val))");
                    method.append(nameSet16(o.earw, "Instructions.do_ROLW(val, e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORW_reg_cl) {
                    Grp2.RORW_reg_cl o = (Grp2.RORW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;int e = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_RORW(e, val))");
                    method.append(nameSet16(o.earw, "Instructions.do_RORW(val, e)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLW_reg_cl) {
                    Grp2.RCLW_reg_cl o = (Grp2.RCLW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCLW (val))");
                    method.append(nameSet16(o.earw, "Instructions.do_RCLW(val, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCRW_reg_cl) {
                    Grp2.RCRW_reg_cl o = (Grp2.RCRW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCRW (val))");
                    method.append(nameSet16(o.earw, "Instructions.do_RCRW(val, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHLW_reg_cl) {
                    Grp2.SHLW_reg_cl o = (Grp2.SHLW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHLW (val))");
                    method.append(nameSet16(o.earw, "Instructions.do_SHLW(val, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHRW_reg_cl) {
                    Grp2.SHRW_reg_cl o = (Grp2.SHRW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHRW (val))");
                    method.append(nameSet16(o.earw, "Instructions.do_SHRW(val, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SARW_reg_cl) {
                    Grp2.SARW_reg_cl o = (Grp2.SARW_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SARW (val))");
                    method.append(nameSet16(o.earw, "Instructions.do_SARW(val, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.ROLW_mem_cl) {
                    Grp2.ROLW_mem_cl o = (Grp2.ROLW_mem_cl) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_ROLW(eaa, val)) Memory.mem_writew(eaa, Instructions.do_ROLW(val, Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORW_mem_cl) {
                    Grp2.RORW_mem_cl o = (Grp2.RORW_mem_cl) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RORW(eaa, val)) Memory.mem_writew(eaa, Instructions.do_RORW(val, Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLW_mem_cl) {
                    Grp2.RCLW_mem_cl o = (Grp2.RCLW_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCLW (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_RCLW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCRW_mem_cl) {
                    Grp2.RCRW_mem_cl o = (Grp2.RCRW_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCRW (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_RCRW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHLW_mem_cl) {
                    Grp2.SHLW_mem_cl o = (Grp2.SHLW_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHLW (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_SHLW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHRW_mem_cl) {
                    Grp2.SHRW_mem_cl o = (Grp2.SHRW_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHRW (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_SHRW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SARW_mem_cl) {
                    Grp2.SARW_mem_cl o = (Grp2.SARW_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SARW (val)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_SARW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                break;
            case 0xd4: // AAM o.ib
            case 0x2d4:
                if (op instanceof Inst1.AamIb) {
                    Inst1.AamIb o = (Inst1.AamIb) op;
                    if (o.ib == 0) {
                        method.append("return EXCEPTION(0);");
                        return false;
                    }
                }
                break;
            case 0xd5: // AAD o.ib
            case 0x2d5:
                if (op instanceof Inst1.AadIb) {
                    Inst1.AadIb o = (Inst1.AadIb) op;
                    method.append("Instructions.AAD(");
                    method.append(o.ib);
                    method.append(");");
                    return true;
                }
                break;
            case 0xd6: // SALC
            case 0x2d6:
                if (op instanceof Inst1.Salc) {
                    Inst1.Salc o = (Inst1.Salc) op;
                    method.append("CPU_Regs.reg_eax.low(Flags.get_CF() ? 0xFF : 0);");
                    return true;
                }
                break;
            case 0xd7: // XLAT
            case 0x2d7:
                if (op instanceof Inst1.Xlat32) {
                    Inst1.Xlat32 o = (Inst1.Xlat32) op;
                    method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(Core.base_ds+CPU_Regs.reg_ebx.dword+CPU_Regs.reg_eax.low()));");
                    return true;
                }
                if (op instanceof Inst1.Xlat16) {
                    Inst1.Xlat16 o = (Inst1.Xlat16) op;
                    method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(Core.base_ds+((CPU_Regs.reg_ebx.word()+CPU_Regs.reg_eax.low()) & 0xFFFF)));");
                    return true;
                }
                break;
            case 0xd8: // FPU ESC 0
            case 0x2d8:
                if (op instanceof Inst1.FPU0_normal) {
                    Inst1.FPU0_normal o = (Inst1.FPU0_normal) op;
                    method.append("FPU.FPU_ESC0_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU0_ea) {
                    Inst1.FPU0_ea o = (Inst1.FPU0_ea) op;
                    method.append("FPU.FPU_ESC0_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xd9: // FPU ESC 1
            case 0x2d9:
                if (op instanceof Inst1.FPU1_normal) {
                    Inst1.FPU1_normal o = (Inst1.FPU1_normal) op;
                    method.append("FPU.FPU_ESC1_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU1_ea) {
                    Inst1.FPU1_ea o = (Inst1.FPU1_ea) op;
                    method.append("FPU.FPU_ESC1_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xda: // FPU ESC 2
            case 0x2da:
                if (op instanceof Inst1.FPU2_normal) {
                    Inst1.FPU2_normal o = (Inst1.FPU2_normal) op;
                    method.append("FPU.FPU_ESC2_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU2_ea) {
                    Inst1.FPU2_ea o = (Inst1.FPU2_ea) op;
                    method.append("FPU.FPU_ESC2_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xdb: // FPU ESC 3
            case 0x2db:
                if (op instanceof Inst1.FPU3_normal) {
                    Inst1.FPU3_normal o = (Inst1.FPU3_normal) op;
                    method.append("FPU.FPU_ESC3_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU3_ea) {
                    Inst1.FPU3_ea o = (Inst1.FPU3_ea) op;
                    method.append("FPU.FPU_ESC3_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xdc: // FPU ESC 4
            case 0x2dc:
                if (op instanceof Inst1.FPU4_normal) {
                    Inst1.FPU4_normal o = (Inst1.FPU4_normal) op;
                    method.append("FPU.FPU_ESC4_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU4_ea) {
                    Inst1.FPU4_ea o = (Inst1.FPU4_ea) op;
                    method.append("FPU.FPU_ESC4_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xdd: // FPU ESC 5
            case 0x2dd:
                if (op instanceof Inst1.FPU5_normal) {
                    Inst1.FPU5_normal o = (Inst1.FPU5_normal) op;
                    method.append("FPU.FPU_ESC5_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU5_ea) {
                    Inst1.FPU5_ea o = (Inst1.FPU5_ea) op;
                    method.append("FPU.FPU_ESC5_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xde: // FPU ESC 6
            case 0x2de:
                if (op instanceof Inst1.FPU6_normal) {
                    Inst1.FPU6_normal o = (Inst1.FPU6_normal) op;
                    method.append("FPU.FPU_ESC6_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU6_ea) {
                    Inst1.FPU6_ea o = (Inst1.FPU6_ea) op;
                    method.append("FPU.FPU_ESC6_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xdf: // FPU ESC 7
            case 0x2df:
                if (op instanceof Inst1.FPU7_normal) {
                    Inst1.FPU7_normal o = (Inst1.FPU7_normal) op;
                    method.append("FPU.FPU_ESC7_Normal(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU7_ea) {
                    Inst1.FPU7_ea o = (Inst1.FPU7_ea) op;
                    method.append("FPU.FPU_ESC7_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0xe0: // LOOPNZ
                if (op instanceof Inst1.Loopnz32) {
                    Inst1.Loopnz32 o = (Inst1.Loopnz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && !Flags.get_ZF()", method);
                    return false;
                }
                if (op instanceof Inst1.Loopnz16) {
                    Inst1.Loopnz16 o = (Inst1.Loopnz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && !Flags.get_ZF()", method);
                    return false;
                }
                break;
            case 0xe1: // LOOPZ
                if (op instanceof Inst1.Loopz32) {
                    Inst1.Loopz32 o = (Inst1.Loopz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && Flags.get_ZF()", method);
                    return false;
                }
                if (op instanceof Inst1.Loopz16) {
                    Inst1.Loopz16 o = (Inst1.Loopz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && Flags.get_ZF()", method);
                    return false;
                }
                break;
            case 0xe2: // LOOP
                if (op instanceof Inst1.Loop32) {
                    Inst1.Loop32 o = (Inst1.Loop32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0", method);
                    return false;
                }
                if (op instanceof Inst1.Loop16) {
                    Inst1.Loop16 o = (Inst1.Loop16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0", method);
                    return false;
                }
                break;
            case 0xe3: // JCXZ
                if (op instanceof Inst1.Jcxz) {
                    Inst1.Jcxz o = (Inst1.Jcxz) op;
                    compile(o, "(CPU_Regs.reg_ecx.dword & " + o.mask + ")==0", method);
                    return false;
                }
                break;
            case 0xe4: // IN AL,Ib
            case 0x2e4:
                if (op instanceof Inst1.InAlIb) {
                    Inst1.InAlIb o = (Inst1.InAlIb) op;
                    method.append("if (CPU.CPU_IO_Exception(");
                    method.append(o.port);
                    method.append(",1)) return RUNEXCEPTION();CPU_Regs.reg_eax.low(IO.IO_ReadB(");
                    method.append(o.port);
                    method.append("));");
                    return true;
                }
                break;
            case 0xe5: // IN AX,Ib
                if (op instanceof Inst1.InAxIb) {
                    Inst1.InAxIb o = (Inst1.InAxIb) op;
                    method.append("if (CPU.CPU_IO_Exception(");
                    method.append(o.port);
                    method.append(",2)) return RUNEXCEPTION();CPU_Regs.reg_eax.word(IO.IO_ReadW(");
                    method.append(o.port);
                    method.append("));");
                    return true;
                }
                break;
            case 0xe6: // OUT o.ib,AL
            case 0x2e6:
                if (op instanceof Inst1.OutAlIb) {
                    Inst1.OutAlIb o = (Inst1.OutAlIb) op;
                    method.append("if (CPU.CPU_IO_Exception(");
                    method.append(o.port);
                    method.append(",1)) return RUNEXCEPTION();IO.IO_WriteB(");
                    method.append(o.port);
                    method.append(",CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0xe7: // OUT o.ib,AX
                if (op instanceof Inst1.OutAxIb) {
                    Inst1.OutAxIb o = (Inst1.OutAxIb) op;
                    method.append("if (CPU.CPU_IO_Exception(");
                    method.append(o.port);
                    method.append(",2)) return RUNEXCEPTION();IO.IO_WriteW(");
                    method.append(o.port);
                    method.append(",CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0xe8: // CALL Jw
                if (op instanceof Inst1.CallJw) {
                    Inst1.CallJw o = (Inst1.CallJw) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");CPU_Regs.reg_ip(CPU_Regs.reg_eip+");
                    method.append(o.eip_count + o.addip);
                    method.append(");return Constants.BR_Link1;");
                    return false;
                }
                break;
            case 0xe9: // JMP Jw
                if (op instanceof Inst1.JmpJw) {
                    Inst1.JmpJw o = (Inst1.JmpJw) op;
                    method.append("CPU_Regs.reg_ip(CPU_Regs.reg_eip+");
                    method.append(o.eip_count + o.addip);
                    method.append(");return Constants.BR_Link1;");
                    return false;
                }
                break;
            case 0xea: // JMP Ap
                if (op instanceof Inst1.JmpAp) {
                    Inst1.JmpAp o = (Inst1.JmpAp) op;
                    method.append("Flags.FillFlags();CPU.CPU_JMP(false,");
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
            case 0xeb: // JMP Jb
                if (op instanceof Inst1.JmpJb) {
                    Inst1.JmpJb o = (Inst1.JmpJb) op;
                    method.append("CPU_Regs.reg_ip(CPU_Regs.reg_eip+");
                    method.append(o.eip_count + o.addip);
                    method.append(");return Constants.BR_Link1;");
                    return false;
                }
                break;
            case 0xec: // IN AL,DX
            case 0x2ec:
                if (op instanceof Inst1.InAlDx) {
                    Inst1.InAlDx o = (Inst1.InAlDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) return RUNEXCEPTION();CPU_Regs.reg_eax.low(IO.IO_ReadB(CPU_Regs.reg_edx.word()));");
                    return true;
                }
                break;
            case 0xed: // IN AX,DX
                if (op instanceof Inst1.InAxDx) {
                    Inst1.InAxDx o = (Inst1.InAxDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) return RUNEXCEPTION();CPU_Regs.reg_eax.word(IO.IO_ReadW(CPU_Regs.reg_edx.word()));");
                    return true;
                }
                break;
            case 0xee: // OUT DX,AL
            case 0x2ee:
                if (op instanceof Inst1.OutAlDx) {
                    Inst1.OutAlDx o = (Inst1.OutAlDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) return RUNEXCEPTION();IO.IO_WriteB(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0xef: // OUT DX,AX
                if (op instanceof Inst1.OutAxDx) {
                    Inst1.OutAxDx o = (Inst1.OutAxDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) return RUNEXCEPTION();IO.IO_WriteW(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0xf0: // LOCK
            case 0x2f0:
                if (op instanceof Inst1.Lock) {
                    Inst1.Lock o = (Inst1.Lock) op;
                    return true;
                }
                break;
            case 0xf1: // ICEBP
            case 0x2f1:
                if (op instanceof Inst1.Icebp) {
                    Inst1.Icebp o = (Inst1.Icebp) op;
                    method.append("CPU.CPU_SW_Interrupt_NoIOPLCheck(1,CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");");
                    if (CPU_TRAP_CHECK)
                        method.append("CPU.cpu.trap_skip=true;");
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                break;
            case 0xf2: // REPNZ
            case 0x2f2:
                break;
            case 0xf3: // REPZ
            case 0x2f3:
                break;
            case 0xf4: // HLT
            case 0x2f4:
                if (op instanceof Inst1.Hlt) {
                    Inst1.Hlt o = (Inst1.Hlt) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);Flags.FillFlags();CPU.CPU_HLT(CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(");return CB_NONE();");
                    return false;
                }
                break;
            case 0xf5: // CMC
            case 0x2f5:
                if (op instanceof Inst1.Cmc) {
                    Inst1.Cmc o = (Inst1.Cmc) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(CPU_Regs.flags & CPU_Regs.CF)==0);");
                    return true;
                }
                break;
            case 0xf6: // GRP3 Eb(,Ib)
            case 0x2f6:
                if (op instanceof Grp3.Testb_reg) {
                    Grp3.Testb_reg o = (Grp3.Testb_reg) op;
                    method.append("Instructions.TESTB((short)");
                    method.append(o.val);
                    method.append(",");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.Testb_mem) {
                    Grp3.Testb_mem o = (Grp3.Testb_mem) op;
                    method.append("Instructions.TESTB((short)");
                    method.append(o.val);
                    method.append(",Memory.mem_readb(");
                    toStringValue(o.get_eaa, method);
                    method.append("));");
                    return true;
                }
                if (op instanceof Grp3.NotEb_reg) {
                    Grp3.NotEb_reg o = (Grp3.NotEb_reg) op;
                    method.append(nameSet8(o.earb, "(byte)~" + nameGet8(o.earb)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NotEb_mem) {
                    Grp3.NotEb_mem o = (Grp3.NotEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa,~Memory.mem_readb(eaa));");
                    return true;
                }
                if (op instanceof Grp3.NegEb_reg) {
                    Grp3.NegEb_reg o = (Grp3.NegEb_reg) op;
                    method.append(nameSet8(o.earb, "Instructions.Negb(" + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NegEb_mem) {
                    Grp3.NegEb_mem o = (Grp3.NegEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.Negb(Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp3.MulAlEb_reg) {
                    Grp3.MulAlEb_reg o = (Grp3.MulAlEb_reg) op;
                    method.append("Instructions.MULB(");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.MulAlEb_mem) {
                    Grp3.MulAlEb_mem o = (Grp3.MulAlEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.MULB(Memory.mem_readb(eaa));");
                    return true;
                }
                if (op instanceof Grp3.IMulAlEb_reg) {
                    Grp3.IMulAlEb_reg o = (Grp3.IMulAlEb_reg) op;
                    method.append("Instructions.IMULB(");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.IMulAlEb_mem) {
                    Grp3.IMulAlEb_mem o = (Grp3.IMulAlEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.IMULB(Memory.mem_readb(eaa));");
                    return true;
                }
                if (op instanceof Grp3.DivAlEb_reg) {
                    Grp3.DivAlEb_reg o = (Grp3.DivAlEb_reg) op;
                    method.append("int val = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (val==0)	{return EXCEPTION(0);} int quo=CPU_Regs.reg_eax.word() / val; int rem=(CPU_Regs.reg_eax.word() % val); int quo8=(quo&0xff); if (quo>0xff) {return EXCEPTION(0);}CPU_Regs.reg_eax.high(rem);CPU_Regs.reg_eax.low(quo8);");
                    return true;
                }
                if (op instanceof Grp3.DivAlEb_mem) {
                    Grp3.DivAlEb_mem o = (Grp3.DivAlEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = Memory.mem_readb(eaa);");
                    method.append("if (val==0)	{return EXCEPTION(0);} int quo=CPU_Regs.reg_eax.word() / val; int rem=(CPU_Regs.reg_eax.word() % val); int quo8=(quo&0xff); if (quo>0xff) {return EXCEPTION(0);}CPU_Regs.reg_eax.high(rem);CPU_Regs.reg_eax.low(quo8);");
                    return true;
                }
                if (op instanceof Grp3.IDivAlEb_reg) {
                    Grp3.IDivAlEb_reg o = (Grp3.IDivAlEb_reg) op;
                    method.append("int val = (byte)");
                    method.append(nameGet8(o.earb));
                    method.append(";if (val==0)	{return EXCEPTION(0);} int quo=CPU_Regs.reg_eax.word() / val; int rem=(CPU_Regs.reg_eax.word() % val); int quo8=(quo&0xff); if (quo>0xff) {return EXCEPTION(0);}CPU_Regs.reg_eax.high(rem);CPU_Regs.reg_eax.low(quo8);");
                    return true;
                }
                if (op instanceof Grp3.IDivAlEb_mem) {
                    Grp3.IDivAlEb_mem o = (Grp3.IDivAlEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = (byte)Memory.mem_readb(eaa);");
                    method.append("if (val==0)	{return EXCEPTION(0);} int quo=CPU_Regs.reg_eax.word() / val; int rem=(CPU_Regs.reg_eax.word() % val); int quo8=(quo&0xff); if (quo>0xff) {return EXCEPTION(0);}CPU_Regs.reg_eax.high(rem);CPU_Regs.reg_eax.low(quo8);");
                    return true;
                }
                break;
            case 0xf7: // GRP3 Ew(,Iw)
                if (op instanceof Grp3.Testw_reg) {
                    Grp3.Testw_reg o = (Grp3.Testw_reg) op;
                    method.append("Instructions.TESTW(");
                    method.append(o.val);
                    method.append(",");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.Testw_mem) {
                    Grp3.Testw_mem o = (Grp3.Testw_mem) op;
                    method.append("Instructions.TESTW(");
                    method.append(o.val);
                    method.append(",Memory.mem_readw(");
                    toStringValue(o.get_eaa, method);
                    method.append("));");
                    return true;
                }
                if (op instanceof Grp3.NotEw_reg) {
                    Grp3.NotEw_reg o = (Grp3.NotEw_reg) op;
                    method.append(nameSet16(o.earw, "~" + nameGet16(o.earw)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NotEw_mem) {
                    Grp3.NotEw_mem o = (Grp3.NotEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa,~Memory.mem_readw(eaa));");
                    return true;
                }
                if (op instanceof Grp3.NegEw_reg) {
                    Grp3.NegEw_reg o = (Grp3.NegEw_reg) op;
                    method.append(nameSet16(o.earw, "Instructions.Negw(" + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NegEw_mem) {
                    Grp3.NegEw_mem o = (Grp3.NegEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.Negw(Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp3.MulAxEw_reg) {
                    Grp3.MulAxEw_reg o = (Grp3.MulAxEw_reg) op;
                    method.append("Instructions.MULW(");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.MulAxEw_mem) {
                    Grp3.MulAxEw_mem o = (Grp3.MulAxEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.MULW(Memory.mem_readw(eaa));");
                    return true;
                }
                if (op instanceof Grp3.IMulAxEw_reg) {
                    Grp3.IMulAxEw_reg o = (Grp3.IMulAxEw_reg) op;
                    method.append("Instructions.IMULW(");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.IMulAxEw_mem) {
                    Grp3.IMulAxEw_mem o = (Grp3.IMulAxEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.IMULW(Memory.mem_readw(eaa));");
                    return true;
                }
                if (op instanceof Grp3.DivAxEw_reg) {
                    Grp3.DivAxEw_reg o = (Grp3.DivAxEw_reg) op;
                    method.append("int val = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (val==0)	{return EXCEPTION(0);}long num=((long)CPU_Regs.reg_edx.word()<<16)|CPU_Regs.reg_eax.word();long quo=num/val;int rem=(int)((num % val));int quo16=(int)(quo&0xffff);if (quo!=quo16) {return EXCEPTION(0);}CPU_Regs.reg_edx.word(rem);CPU_Regs.reg_eax.word(quo16);");
                    return true;
                }
                if (op instanceof Grp3.DivAxEw_mem) {
                    Grp3.DivAxEw_mem o = (Grp3.DivAxEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = Memory.mem_readw(eaa);");
                    method.append("if (val==0)	{return EXCEPTION(0);}long num=((long)CPU_Regs.reg_edx.word()<<16)|CPU_Regs.reg_eax.word();long quo=num/val;int rem=(int)((num % val));int quo16=(int)(quo&0xffff);if (quo!=quo16) {return EXCEPTION(0);}CPU_Regs.reg_edx.word(rem);CPU_Regs.reg_eax.word(quo16);");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEw_reg) {
                    Grp3.IDivAxEw_reg o = (Grp3.IDivAxEw_reg) op;
                    method.append("int val = (short)");
                    method.append(nameGet16(o.earw));
                    method.append(";if (val==0)	{return EXCEPTION(0);}int num=(CPU_Regs.reg_edx.word()<<16)|CPU_Regs.reg_eax.word();int quo=num/val;short rem=(short)((num % val));short quo16=(short)quo;if (quo!=(int)quo16) {return EXCEPTION(0);}CPU_Regs.reg_edx.word(rem);CPU_Regs.reg_eax.word(quo16);");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEw_mem) {
                    Grp3.IDivAxEw_mem o = (Grp3.IDivAxEw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = (short)Memory.mem_readw(eaa);");
                    method.append("if (val==0)	{return EXCEPTION(0);}int num=(CPU_Regs.reg_edx.word()<<16)|CPU_Regs.reg_eax.word();int quo=num/val;short rem=(short)((num % val));short quo16=(short)quo;if (quo!=(int)quo16) {return EXCEPTION(0);}CPU_Regs.reg_edx.word(rem);CPU_Regs.reg_eax.word(quo16);");
                    return true;
                }
                break;
            case 0xf8: // CLC
            case 0x2f8:
                if (op instanceof Inst1.Clc) {
                    Inst1.Clc o = (Inst1.Clc) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,false);");
                    return true;
                }
                break;
            case 0xf9: // STC
            case 0x2f9:
                if (op instanceof Inst1.Stc) {
                    Inst1.Stc o = (Inst1.Stc) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,true);");
                    return true;
                }
                break;
            case 0xfa: // CLI
            case 0x2fa:
                if (op instanceof Inst1.Cli) {
                    Inst1.Cli o = (Inst1.Cli) op;
                    method.append("if (CPU.CPU_CLI()) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0xfb: // STI
            case 0x2fb:
                if (op instanceof Inst1.Sti) {
                    Inst1.Sti o = (Inst1.Sti) op;
                    method.append("if (CPU.CPU_STI()) return RUNEXCEPTION();");
                    if (CPU_PIC_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.IF)!=0 && Pic.PIC_IRQCheck!=0) return DECODE_END(");
                        method.append(o.eip_count);
                        method.append(");");
                    }
                    return true;
                }
                break;
            case 0xfc: // CLD
            case 0x2fc:
                if (op instanceof Inst1.Cld) {
                    Inst1.Cld o = (Inst1.Cld) op;
                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.DF,false);CPU.cpu.direction=1;");
                    return true;
                }
                break;
            case 0xfd: // STD
            case 0x2fd:
                if (op instanceof Inst1.Std) {
                    Inst1.Std o = (Inst1.Std) op;
                    method.append("CPU_Regs.SETFLAGBIT(CPU_Regs.DF,true);CPU.cpu.direction=-1;");
                    return true;
                }
                break;
            case 0xfe: // GRP4 Eb
            case 0x2fe:
                if (op instanceof Inst1.Incb_reg) {
                    Inst1.Incb_reg o = (Inst1.Incb_reg) op;
                    method.append(nameSet8(o.reg, "Instructions.INCB(" + nameGet8(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.Incb_mem) {
                    Inst1.Incb_mem o = (Inst1.Incb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.Decb_reg) {
                    Inst1.Decb_reg o = (Inst1.Decb_reg) op;
                    method.append(nameSet8(o.reg, "Instructions.DECB(" + nameGet8(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.Decb_mem) {
                    Inst1.Decb_mem o = (Inst1.Decb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.Callback) {
                    Inst1.Callback o = (Inst1.Callback) op;
                    method.append("CPU_Regs.reg_eip+=");
                    method.append(o.eip_count);
                    method.append(";Data.callback = ");
                    method.append(o.val);
                    method.append(";return Constants.BR_CallBack;");
                    return false;
                }
                break;
            case 0xff: // GRP5 Ew
                if (op instanceof Inst1.Incw_reg) {
                    Inst1.Incw_reg o = (Inst1.Incw_reg) op;
                    method.append(nameSet16(o.reg, "Instructions.INCW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.Incw_mem) {
                    Inst1.Incw_mem o = (Inst1.Incw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.Decw_reg) {
                    Inst1.Decw_reg o = (Inst1.Decw_reg) op;
                    method.append(nameSet16(o.reg, "Instructions.DECW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.Decw_mem) {
                    Inst1.Decw_mem o = (Inst1.Decw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.CallEv_reg) {
                    Inst1.CallEv_reg o = (Inst1.CallEv_reg) op;
                    method.append("int old = CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(";CPU.CPU_Push16(old & 0xFFFF);");
                    method.append("CPU_Regs.reg_eip=");
                    method.append(nameGet16(o.earw));
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.CallEv_mem) {
                    Inst1.CallEv_mem o = (Inst1.CallEv_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old = CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(";int eip = Memory.mem_readw(eaa);CPU.CPU_Push16(old & 0xFFFF);CPU_Regs.reg_eip = eip;return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.CallEp) {
                    Inst1.CallEp o = (Inst1.CallEp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int newip=Memory.mem_readw(eaa);int newcs=Memory.mem_readw(eaa+2);Flags.FillFlags();CPU.CPU_CALL(false,newcs,newip,(CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(") & 0xFFFF);");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.JmpEv_reg) {
                    Inst1.JmpEv_reg o = (Inst1.JmpEv_reg) op;
                    method.append("CPU_Regs.reg_eip=");
                    method.append(nameGet16(o.earw));
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.JmpEv_mem) {
                    Inst1.JmpEv_mem o = (Inst1.JmpEv_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";CPU_Regs.reg_eip=Memory.mem_readw(eaa);return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.JmpEp) {
                    Inst1.JmpEp o = (Inst1.JmpEp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int newip=Memory.mem_readw(eaa);int newcs=Memory.mem_readw(eaa+2);Flags.FillFlags();CPU.CPU_JMP(false,newcs,newip,(CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(") & 0xFFFF);");
                    if (CPU_TRAP_CHECK) {
                        method.append("if (CPU_Regs.GETFLAG(CPU_Regs.TF)!=0) {CPU.cpudecoder= Core_dynamic.CPU_Core_Dynrec_Trap_Run;return CB_NONE();}");
                    }
                    method.append("return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.PushEv_reg) {
                    Inst1.PushEv_reg o = (Inst1.PushEv_reg) op;
                    method.append("CPU.CPU_Push16(");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.PushEv_mem) {
                    Inst1.PushEv_mem o = (Inst1.PushEv_mem) op;
                    method.append("CPU.CPU_Push16(Memory.mem_readw(");
                    toStringValue(o.get_eaa, method);
                    method.append("));");
                    return true;
                }
                break;
            case 0x100: // GRP 6 Exxx
                if (op instanceof Inst2.Sldt_reg) {
                    Inst2.Sldt_reg o = (Inst2.Sldt_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    method.append(nameSet16(o.earw, "CPU.CPU_SLDT()"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.Sldt_mem) {
                    Inst2.Sldt_mem o = (Inst2.Sldt_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, CPU.CPU_SLDT());");
                    return true;
                }
                if (op instanceof Inst2.Str_reg) {
                    Inst2.Str_reg o = (Inst2.Str_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    method.append(nameSet16(o.earw, "CPU.CPU_STR()"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.Str_mem) {
                    Inst2.Str_mem o = (Inst2.Str_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, CPU.CPU_STR());");
                    return true;
                }
                if (op instanceof Inst2.Lldt_reg) {
                    Inst2.Lldt_reg o = (Inst2.Lldt_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);if (CPU.CPU_LLDT(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst2.Lldt_mem) {
                    Inst2.Lldt_mem o = (Inst2.Lldt_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_LLDT(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst2.Ltr_reg) {
                    Inst2.Ltr_reg o = (Inst2.Ltr_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);if (CPU.CPU_LTR(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst2.Ltr_mem) {
                    Inst2.Ltr_mem o = (Inst2.Ltr_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_LTR(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst2.Verr_reg) {
                    Inst2.Verr_reg o = (Inst2.Verr_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;CPU.CPU_VERR(");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst2.Verr_mem) {
                    Inst2.Verr_mem o = (Inst2.Verr_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";CPU.CPU_VERR(Memory.mem_readw(eaa));");
                    return true;
                }
                if (op instanceof Inst2.Verw_reg) {
                    Inst2.Verw_reg o = (Inst2.Verw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;CPU.CPU_VERW(");
                    method.append(nameGet16(o.earw));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst2.Verw_mem) {
                    Inst2.Verw_mem o = (Inst2.Verw_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";CPU.CPU_VERW(Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x101: // Group 7 Ew
                if (op instanceof Inst2.Sgdt_mem) {
                    Inst2.Sgdt_mem o = (Inst2.Sgdt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());");
                    return true;
                }
                if (op instanceof Inst2.Sidt_mem) {
                    Inst2.Sidt_mem o = (Inst2.Sidt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());");
                    return true;
                }
                if (op instanceof Inst2.Lgdt_mem) {
                    Inst2.Lgdt_mem o = (Inst2.Lgdt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2) & 0xFFFFFF);");
                    return true;
                }
                if (op instanceof Inst2.Lidt_mem) {
                    Inst2.Lidt_mem o = (Inst2.Lidt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);CPU.CPU_LIDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2) & 0xFFFFFF);");
                    return true;
                }
                if (op instanceof Inst2.Smsw_mem) {
                    Inst2.Smsw_mem o = (Inst2.Smsw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writew(eaa,CPU.CPU_SMSW());");
                    return true;
                }
                if (op instanceof Inst2.Lmsw_mem) {
                    Inst2.Lmsw_mem o = (Inst2.Lmsw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_LMSW(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst2.Invlpg) {
                    Inst2.Invlpg o = (Inst2.Invlpg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);Paging.PAGING_ClearTLB();");
                    return true;
                }
                if (op instanceof Inst2.Lgdt_reg) {
                    Inst2.Lgdt_reg o = (Inst2.Lgdt_reg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);return Constants.BR_Illegal;");
                    return false;
                }
                if (op instanceof Inst2.Lidt_reg) {
                    Inst2.Lidt_reg o = (Inst2.Lidt_reg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);return Constants.BR_Illegal;");
                    return false;
                }
                if (op instanceof Inst2.Smsw_reg) {
                    Inst2.Smsw_reg o = (Inst2.Smsw_reg) op;
                    method.append(nameSet16(o.earw, "CPU.CPU_SMSW()"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.Lmsw_reg) {
                    Inst2.Lmsw_reg o = (Inst2.Lmsw_reg) op;
                    method.append("if (CPU.CPU_LMSW(");
                    method.append(nameGet16(o.earw));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x102: // LAR Gw,Ew
                if (op instanceof Inst2.LarGwEw_reg) {
                    Inst2.LarGwEw_reg o = (Inst2.LarGwEw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;IntRef value=new IntRef(");
                    method.append(nameGet16(o.rw));
                    method.append(");CPU.CPU_LAR(");
                    method.append(nameGet16(o.earw));
                    method.append(",value);");
                    method.append(nameSet16(o.rw, "value.value"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.LarGwEw_mem) {
                    Inst2.LarGwEw_mem o = (Inst2.LarGwEw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;IntRef value=new IntRef(");
                    method.append(nameGet16(o.rw));
                    method.append(");CPU.CPU_LAR(Memory.mem_readw(eaa),value);");
                    method.append(nameSet16(o.rw, "value.value"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x103: // LSL Gw,Ew
                if (op instanceof Inst2.LslGwEw_reg) {
                    Inst2.LslGwEw_reg o = (Inst2.LslGwEw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;IntRef value=new IntRef(");
                    method.append(nameGet16(o.rw));
                    method.append(");CPU.CPU_LSL(");
                    method.append(nameGet16(o.earw));
                    method.append(",value);");
                    method.append(nameSet16(o.rw, "value.value"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.LslGwEw_mem) {
                    Inst2.LslGwEw_mem o = (Inst2.LslGwEw_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";IntRef value = new IntRef(");
                    method.append(nameGet16(o.rw));
                    method.append(");CPU.CPU_LSL(Memory.mem_readw(eaa),value);");
                    method.append(nameSet16(o.rw, "value.value"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x106: // CLTS
            case 0x306:
                if (op instanceof Inst2.Clts) {
                    Inst2.Clts o = (Inst2.Clts) op;
                    // :TODO: this is a bug in the compiler, ~ and a constant int does not work so I added the (int) cast which fixes it
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);CPU.cpu.cr0=CPU.cpu.cr0 & (~(int)CPU.CR0_TASKSWITCH);");
                    return true;
                }

                break;
            case 0x108: // INVD
            case 0x308:
                if (op instanceof Inst2.Invd) {
                    Inst2.Invd o = (Inst2.Invd) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);");
                    return true;
                }
                break;
            case 0x109: // WBINVD
            case 0x309:
                break;
            case 0x120: // MOV Rd.CRx
            case 0x320:
                if (op instanceof Inst2.MovRdCr) {
                    Inst2.MovRdCr o = (Inst2.MovRdCr) op;
                    method.append("if (CPU.CPU_READ_CRX(");
                    method.append(o.which);
                    method.append(",");
                    method.append(nameRef(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x121: // MOV Rd,DRx
            case 0x321:
                if (op instanceof Inst2.MovRdDr) {
                    Inst2.MovRdDr o = (Inst2.MovRdDr) op;
                    method.append("if (CPU.CPU_READ_DRX(");
                    method.append(o.which);
                    method.append(",");
                    method.append(nameRef(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x122: // MOV CRx,Rd
            case 0x322:
                if (op instanceof Inst2.MovCrRd) {
                    Inst2.MovCrRd o = (Inst2.MovCrRd) op;
                    method.append("if (CPU.CPU_WRITE_CRX(");
                    method.append(o.which);
                    method.append(",");
                    method.append(nameGet32(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x123: // MOV DRx,Rd
            case 0x323:
                if (op instanceof Inst2.MovDrRd) {
                    Inst2.MovDrRd o = (Inst2.MovDrRd) op;
                    method.append("if (CPU.CPU_WRITE_DRX(");
                    method.append(o.which);
                    method.append(",");
                    method.append(nameGet32(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x124: // MOV Rd,TRx
            case 0x324:
                if (op instanceof Inst2.MovRdTr) {
                    Inst2.MovRdTr o = (Inst2.MovRdTr) op;
                    method.append("if (CPU.CPU_READ_TRX(");
                    method.append(o.which);
                    method.append(",");
                    method.append(nameRef(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x126: // MOV TRx,Rd
            case 0x326:
                if (op instanceof Inst2.MovTrRd) {
                    Inst2.MovTrRd o = (Inst2.MovTrRd) op;
                    method.append("if (CPU.CPU_WRITE_TRX(");
                    method.append(o.which);
                    method.append(",");
                    method.append(nameGet32(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x131: // RDTSC
            case 0x331:
                if (op instanceof Inst2.Rdtsc) {
                    Inst2.Rdtsc o = (Inst2.Rdtsc) op;
                    method.append("if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM) return Constants.BR_Illegal;long tsc=(long)(Pic.PIC_FullIndex()*(double)CPU.CPU_CycleMax);CPU_Regs.reg_edx.dword=(int)(tsc>>>32);CPU_Regs.reg_eax.dword=(int)tsc;");
                    return true;
                }
                break;
            case 0x180: // JO
                if (op instanceof Inst2.JumpCond16_w_o) {
                    Inst2.JumpCond16_w_o o = (Inst2.JumpCond16_w_o) op;
                    compile(o, "Flags.TFLG_O()", method);
                    return false;
                }
                break;
            case 0x181: // JNO
                if (op instanceof Inst2.JumpCond16_w_no) {
                    Inst2.JumpCond16_w_no o = (Inst2.JumpCond16_w_no) op;
                    compile(o, "Flags.TFLG_NO()", method);
                    return false;
                }
                break;
            case 0x182: // JB
                if (op instanceof Inst2.JumpCond16_w_b) {
                    Inst2.JumpCond16_w_b o = (Inst2.JumpCond16_w_b) op;
                    compile(o, "Flags.TFLG_B()", method);
                    return false;
                }
                break;
            case 0x183: // JNB
                if (op instanceof Inst2.JumpCond16_w_nb) {
                    Inst2.JumpCond16_w_nb o = (Inst2.JumpCond16_w_nb) op;
                    compile(o, "Flags.TFLG_NB()", method);
                    return false;
                }
                break;
            case 0x184: // JZ
                if (op instanceof Inst2.JumpCond16_w_z) {
                    Inst2.JumpCond16_w_z o = (Inst2.JumpCond16_w_z) op;
                    compile(o, "Flags.TFLG_Z()", method);
                    return false;
                }
                break;
            case 0x185: // JNZ
                if (op instanceof Inst2.JumpCond16_w_nz) {
                    Inst2.JumpCond16_w_nz o = (Inst2.JumpCond16_w_nz) op;
                    compile(o, "Flags.TFLG_NZ()", method);
                    return false;
                }
                break;
            case 0x186: // JBE
                if (op instanceof Inst2.JumpCond16_w_be) {
                    Inst2.JumpCond16_w_be o = (Inst2.JumpCond16_w_be) op;
                    compile(o, "Flags.TFLG_BE()", method);
                    return false;
                }
                break;
            case 0x187: // JNBE
                if (op instanceof Inst2.JumpCond16_w_nbe) {
                    Inst2.JumpCond16_w_nbe o = (Inst2.JumpCond16_w_nbe) op;
                    compile(o, "Flags.TFLG_NBE()", method);
                    return false;
                }
                break;
            case 0x188: // JS
                if (op instanceof Inst2.JumpCond16_w_s) {
                    Inst2.JumpCond16_w_s o = (Inst2.JumpCond16_w_s) op;
                    compile(o, "Flags.TFLG_S()", method);
                    return false;
                }
                break;
            case 0x189: // JNS
                if (op instanceof Inst2.JumpCond16_w_ns) {
                    Inst2.JumpCond16_w_ns o = (Inst2.JumpCond16_w_ns) op;
                    compile(o, "Flags.TFLG_NS()", method);
                    return false;
                }
                break;
            case 0x18a: // JP
                if (op instanceof Inst2.JumpCond16_w_p) {
                    Inst2.JumpCond16_w_p o = (Inst2.JumpCond16_w_p) op;
                    compile(o, "Flags.TFLG_P()", method);
                    return false;
                }
                break;
            case 0x18b: // JNP
                if (op instanceof Inst2.JumpCond16_w_np) {
                    Inst2.JumpCond16_w_np o = (Inst2.JumpCond16_w_np) op;
                    compile(o, "Flags.TFLG_NP()", method);
                    return false;
                }
                break;
            case 0x18c: // JL
                if (op instanceof Inst2.JumpCond16_w_l) {
                    Inst2.JumpCond16_w_l o = (Inst2.JumpCond16_w_l) op;
                    compile(o, "Flags.TFLG_L()", method);
                    return false;
                }
                break;
            case 0x18d: // JNL
                if (op instanceof Inst2.JumpCond16_w_nl) {
                    Inst2.JumpCond16_w_nl o = (Inst2.JumpCond16_w_nl) op;
                    compile(o, "Flags.TFLG_NL()", method);
                    return false;
                }
                break;
            case 0x18e: // JLE
                if (op instanceof Inst2.JumpCond16_w_le) {
                    Inst2.JumpCond16_w_le o = (Inst2.JumpCond16_w_le) op;
                    compile(o, "Flags.TFLG_LE()", method);
                    return false;
                }
                break;
            case 0x18f: // JNLE
                if (op instanceof Inst2.JumpCond16_w_nle) {
                    Inst2.JumpCond16_w_nle o = (Inst2.JumpCond16_w_nle) op;
                    compile(o, "Flags.TFLG_NLE()", method);
                    return false;
                }
                break;
            case 0x190: // SETO
            case 0x390:
                if (op instanceof Inst2.SETcc_reg_o) {
                    Inst2.SETcc_reg_o o = (Inst2.SETcc_reg_o) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_O()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_o) {
                    Inst2.SETcc_mem_o o = (Inst2.SETcc_mem_o) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_O()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x191: // SETNO
            case 0x391:
                if (op instanceof Inst2.SETcc_reg_no) {
                    Inst2.SETcc_reg_no o = (Inst2.SETcc_reg_no) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NO()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_no) {
                    Inst2.SETcc_mem_no o = (Inst2.SETcc_mem_no) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NO()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x192: // SETB
            case 0x392:
                if (op instanceof Inst2.SETcc_reg_b) {
                    Inst2.SETcc_reg_b o = (Inst2.SETcc_reg_b) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_B()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_b) {
                    Inst2.SETcc_mem_b o = (Inst2.SETcc_mem_b) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_B()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x193: // SETNB
            case 0x393:
                if (op instanceof Inst2.SETcc_reg_nb) {
                    Inst2.SETcc_reg_nb o = (Inst2.SETcc_reg_nb) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NB()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nb) {
                    Inst2.SETcc_mem_nb o = (Inst2.SETcc_mem_nb) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NB()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x194: // SETZ
            case 0x394:
                if (op instanceof Inst2.SETcc_reg_z) {
                    Inst2.SETcc_reg_z o = (Inst2.SETcc_reg_z) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_Z()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_z) {
                    Inst2.SETcc_mem_z o = (Inst2.SETcc_mem_z) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_Z()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x195: // SETNZ
            case 0x395:
                if (op instanceof Inst2.SETcc_reg_nz) {
                    Inst2.SETcc_reg_nz o = (Inst2.SETcc_reg_nz) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NZ()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nz) {
                    Inst2.SETcc_mem_nz o = (Inst2.SETcc_mem_nz) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NZ()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x196: // SETBE
            case 0x396:
                if (op instanceof Inst2.SETcc_reg_be) {
                    Inst2.SETcc_reg_be o = (Inst2.SETcc_reg_be) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_BE()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_be) {
                    Inst2.SETcc_mem_be o = (Inst2.SETcc_mem_be) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_BE()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x197: // SETNBE
            case 0x397:
                if (op instanceof Inst2.SETcc_reg_nbe) {
                    Inst2.SETcc_reg_nbe o = (Inst2.SETcc_reg_nbe) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NBE()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nbe) {
                    Inst2.SETcc_mem_nbe o = (Inst2.SETcc_mem_nbe) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NBE()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x198: // SETS
            case 0x398:
                if (op instanceof Inst2.SETcc_reg_s) {
                    Inst2.SETcc_reg_s o = (Inst2.SETcc_reg_s) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_S()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_s) {
                    Inst2.SETcc_mem_s o = (Inst2.SETcc_mem_s) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_S()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x199: // SETNS
            case 0x399:
                if (op instanceof Inst2.SETcc_reg_ns) {
                    Inst2.SETcc_reg_ns o = (Inst2.SETcc_reg_ns) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NS()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_ns) {
                    Inst2.SETcc_mem_ns o = (Inst2.SETcc_mem_ns) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NS()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x19a: // SETP
            case 0x39a:
                if (op instanceof Inst2.SETcc_reg_p) {
                    Inst2.SETcc_reg_p o = (Inst2.SETcc_reg_p) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_P()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_p) {
                    Inst2.SETcc_mem_p o = (Inst2.SETcc_mem_p) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_P()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x19b: // SETNP
            case 0x39b:
                if (op instanceof Inst2.SETcc_reg_np) {
                    Inst2.SETcc_reg_np o = (Inst2.SETcc_reg_np) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NP()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_np) {
                    Inst2.SETcc_mem_np o = (Inst2.SETcc_mem_np) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NP()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x19c: // SETL
            case 0x39c:
                if (op instanceof Inst2.SETcc_reg_l) {
                    Inst2.SETcc_reg_l o = (Inst2.SETcc_reg_l) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_L()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_l) {
                    Inst2.SETcc_mem_l o = (Inst2.SETcc_mem_l) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_L()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x19d: // SETNL
            case 0x39d:
                if (op instanceof Inst2.SETcc_reg_nl) {
                    Inst2.SETcc_reg_nl o = (Inst2.SETcc_reg_nl) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NL()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nl) {
                    Inst2.SETcc_mem_nl o = (Inst2.SETcc_mem_nl) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NL()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x19e: // SETLE
            case 0x39e:
                if (op instanceof Inst2.SETcc_reg_le) {
                    Inst2.SETcc_reg_le o = (Inst2.SETcc_reg_le) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_LE()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_le) {
                    Inst2.SETcc_mem_le o = (Inst2.SETcc_mem_le) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_LE()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x19f: // SETNLE
            case 0x39f:
                if (op instanceof Inst2.SETcc_reg_nle) {
                    Inst2.SETcc_reg_nle o = (Inst2.SETcc_reg_nle) op;
                    method.append(nameSet8(o.earb, "(short)((Flags.TFLG_NLE()) ? 1 : 0)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nle) {
                    Inst2.SETcc_mem_nle o = (Inst2.SETcc_mem_nle) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ((short)((Flags.TFLG_NLE()) ? 1 : 0)));");
                    return true;
                }
                break;
            case 0x1a0: // PUSH FS
                if (op instanceof Inst2.PushFS) {
                    Inst2.PushFS o = (Inst2.PushFS) op;
                    method.append("CPU.CPU_Push16(CPU.Segs_FSval);");
                    return true;
                }
                break;
            case 0x1a1: // POP FS
                if (op instanceof Inst2.PopFS) {
                    Inst2.PopFS o = (Inst2.PopFS) op;
                    method.append("if (CPU.CPU_PopSegFS(false)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x1a2: // CPUID
            case 0x3a2:
                if (op instanceof Inst2.CPUID) {
                    Inst2.CPUID o = (Inst2.CPUID) op;
                    method.append("if (!CPU.CPU_CPUID()) return Constants.BR_Illegal;");
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
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                    return true;
                }
                break;
            case 0x1a4: // SHLD Ew,Gw,Ib
                if (op instanceof Inst2.ShldEwGwIb_reg) {
                    Inst2.ShldEwGwIb_reg o = (Inst2.ShldEwGwIb_reg) op;
                    method.append(nameSet16(o.earw, "Instructions.do_DSHLW(" + nameGet16(o.rw) + ", " + o.op3 + ", " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShldEwGwIb_mem) {
                    Inst2.ShldEwGwIb_mem o = (Inst2.ShldEwGwIb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_DSHLW(");
                    method.append(nameGet16(o.rw));
                    method.append(", ");
                    method.append(o.op3);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x1a5: // SHLD Ew,Gw,CL
                if (op instanceof Inst2.ShldEwGwCl_reg) {
                    Inst2.ShldEwGwCl_reg o = (Inst2.ShldEwGwCl_reg) op;
                    method.append("short s = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHLW(s))");
                    method.append(nameSet16(o.earw, "Instructions.do_DSHLW(" + nameGet16(o.rw) + ", s, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShldEwGwCl_mem) {
                    Inst2.ShldEwGwCl_mem o = (Inst2.ShldEwGwCl_mem) op;
                    method.append("short s = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHLW(s)) {int eaa =");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_DSHLW(");
                    method.append(nameGet16(o.rw));
                    method.append(", s, Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x1a8: // PUSH GS
                if (op instanceof Inst2.PushGS) {
                    Inst2.PushGS o = (Inst2.PushGS) op;
                    method.append("CPU.CPU_Push16(CPU.Segs_GSval);");
                    return true;
                }
                break;
            case 0x1a9: // POP GS
                if (op instanceof Inst2.PopGS) {
                    Inst2.PopGS o = (Inst2.PopGS) op;
                    method.append("if (CPU.CPU_PopSegGS(false)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x1ab: // BTS Ew,Gw
                if (op instanceof Inst2.BtsEwGw_reg) {
                    Inst2.BtsEwGw_reg o = (Inst2.BtsEwGw_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & mask)!=0);");
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + " | mask"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtsEwGw_mem) {
                    Inst2.BtsEwGw_mem o = (Inst2.BtsEwGw_mem) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);Memory.mem_writew(eaa,old | mask);");
                    return true;
                }
                break;
            case 0x1ac: // SHRD Ew,Gw,Ib
                if (op instanceof Inst2.ShrdEwGwIb_reg) {
                    Inst2.ShrdEwGwIb_reg o = (Inst2.ShrdEwGwIb_reg) op;
                    method.append(nameSet16(o.earw, "Instructions.do_DSHRW(" + nameGet16(o.rw) + ", " + o.op3 + ", " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShrdEwGwIb_mem) {
                    Inst2.ShrdEwGwIb_mem o = (Inst2.ShrdEwGwIb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_DSHRW(");
                    method.append(nameGet16(o.rw));
                    method.append(", ");
                    method.append(o.op3);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                break;
            case 0x1ad: // SHRD Ew,Gw,CL
                if (op instanceof Inst2.ShrdEwGwCl_reg) {
                    Inst2.ShrdEwGwCl_reg o = (Inst2.ShrdEwGwCl_reg) op;
                    method.append("int s = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHRW(s))");
                    method.append(nameSet16(o.earw, "Instructions.do_DSHRW(" + nameGet16(o.rw) + ", s, " + nameGet16(o.earw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ShrdEwGwCl_mem) {
                    Inst2.ShrdEwGwCl_mem o = (Inst2.ShrdEwGwCl_mem) op;
                    method.append("int s = CPU_Regs.reg_ecx.low();if (Instructions.valid_DSHRW(s)) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa, Instructions.do_DSHRW(");
                    method.append(nameGet16(o.rw));
                    method.append(", s, Memory.mem_readw(eaa)));}");
                    return true;
                }
                break;
            case 0x1af: // IMUL Gw,Ew
                if (op instanceof Inst2.ImulGwEw_reg) {
                    Inst2.ImulGwEw_reg o = (Inst2.ImulGwEw_reg) op;
                    method.append(nameSet16(o.rw, "Instructions.DIMULW(" + nameGet16(o.earw) + "," + nameGet16(o.rw) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.ImulGwEw_mem) {
                    Inst2.ImulGwEw_mem o = (Inst2.ImulGwEw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "Instructions.DIMULW(Memory.mem_readw(eaa)," + nameGet16(o.rw) + ")"));
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
                    method.append("Flags.FillFlags();int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";short val = Memory.mem_readb(eaa);if (CPU_Regs.reg_eax.low() == val) {Memory.mem_writeb(eaa,");
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
                    method.append(nameSet16(o.earw, nameGet16(o.rw)));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {CPU_Regs.reg_eax.word(");
                    method.append(nameGet16(o.earw));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                if (op instanceof Inst2.CmpxchgEwGw_mem) {
                    Inst2.CmpxchgEwGw_mem o = (Inst2.CmpxchgEwGw_mem) op;
                    method.append("Flags.FillFlags();int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = Memory.mem_readw(eaa);if (CPU_Regs.reg_eax.word() == val) {Memory.mem_writew(eaa,");
                    method.append(nameGet16(o.rw));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {Memory.mem_writew(eaa,val);CPU_Regs.reg_eax.word(val);CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                break;
            case 0x1b2: // LSS Ew
                if (op instanceof Inst2.LssEw) {
                    Inst2.LssEw o = (Inst2.LssEw) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                break;
            case 0x1b3: // BTR Ew,Gw
                if (op instanceof Inst2.BtrEwGw_reg) {
                    Inst2.BtrEwGw_reg o = (Inst2.BtrEwGw_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & mask)!=0);");
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + " & ~mask"));
                    method.append(";");
                    return false;
                }
                if (op instanceof Inst2.BtrEwGw_mem) {
                    Inst2.BtrEwGw_mem o = (Inst2.BtrEwGw_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);Memory.mem_writew(eaa,old & ~mask);");
                    return true;
                }
                break;
            case 0x1b4: // LFS Ew
                if (op instanceof Inst2.LfsEw) {
                    Inst2.LfsEw o = (Inst2.LfsEw) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b5: // LGS Ew
                if (op instanceof Inst2.LgsEw) {
                    Inst2.LgsEw o = (Inst2.LgsEw) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+2))) return RUNEXCEPTION();");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b6: // MOVZX Gw,Eb
                if (op instanceof Inst2.MovzxGwEb_reg) {
                    Inst2.MovzxGwEb_reg o = (Inst2.MovzxGwEb_reg) op;
                    method.append(nameSet16(o.rw, nameGet8(o.earb)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.MovzxGwEb_mem) {
                    Inst2.MovzxGwEb_mem o = (Inst2.MovzxGwEb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "Memory.mem_readb(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1b7: // MOVZX Gw,Ew
                if (op instanceof Inst2.MovzxGwEw_reg) {
                    Inst2.MovzxGwEw_reg o = (Inst2.MovzxGwEw_reg) op;
                    method.append(nameSet16(o.rw, nameGet16(o.earw)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.MovzxGwEw_mem) {
                    Inst2.MovzxGwEw_mem o = (Inst2.MovzxGwEw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "Memory.mem_readw(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1bf: // MOVSX Gw,Ew
                break;
            case 0x1ba: // GRP8 Ew,Ib
                if (op instanceof Inst2.BtEwIb_reg) {
                    Inst2.BtEwIb_reg o = (Inst2.BtEwIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & mask)!=0);");
                    return true;
                }
                if (op instanceof Inst2.BtsEwIb_reg) {
                    Inst2.BtsEwIb_reg o = (Inst2.BtsEwIb_reg) op;
                    method.append("Flags.FillFlags();CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + " | " + o.mask));
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
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + " & ~" + o.mask));
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
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + " ^ " + o.mask));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtEwIb_mem) {
                    Inst2.BtEwIb_mem o = (Inst2.BtEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    return true;
                }
                if (op instanceof Inst2.BtsEwIb_mem) {
                    Inst2.BtsEwIb_mem o = (Inst2.BtsEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writew(eaa,old|");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst2.BtrEwIb_mem) {
                    Inst2.BtrEwIb_mem o = (Inst2.BtrEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writew(eaa,old & ~");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst2.BtcEwIb_mem) {
                    Inst2.BtcEwIb_mem o = (Inst2.BtcEwIb_mem) op;
                    method.append("Flags.FillFlags();");
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writew(eaa,old ^ ");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                break;
            case 0x1bb: // BTC Ew,Gw
                if (op instanceof Inst2.BtcEwGw_reg) {
                    Inst2.BtcEwGw_reg o = (Inst2.BtcEwGw_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet16(o.earw));
                    method.append(" & mask)!=0);");
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + " ^ mask"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.BtcEwGw_mem) {
                    Inst2.BtcEwGw_mem o = (Inst2.BtcEwGw_mem) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet16(o.rw));
                    method.append(" & 15);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";eaa+=(((short)");
                    method.append(nameGet16(o.rw));
                    method.append(")>>4)*2;int old=Memory.mem_readw(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);Memory.mem_writew(eaa,old ^ mask);");
                    return true;
                }
                break;
            case 0x1bc: // BSF Gw,Ew
                if (op instanceof Inst2.BsfGwEw_reg) {
                    Inst2.BsfGwEw_reg o = (Inst2.BsfGwEw_reg) op;
                    method.append("int value=");
                    method.append(nameGet16(o.earw));
                    method.append(";if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0; while ((value & 0x01)==0) { result++; value>>=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet16(o.rw, "result"));
                    method.append(";}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst2.BsfGwEw_mem) {
                    Inst2.BsfGwEw_mem o = (Inst2.BsfGwEw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int value=Memory.mem_readw(eaa);if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0;while ((value & 0x01)==0) { result++; value>>=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet16(o.rw, "result"));
                    method.append(";}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x1bd: // BSR Gw,Ew
                if (op instanceof Inst2.BsrGwEw_reg) {
                    Inst2.BsrGwEw_reg o = (Inst2.BsrGwEw_reg) op;
                    method.append("int value=");
                    method.append(nameGet16(o.earw));
                    method.append(";if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 15;while ((value & 0x8000)==0) { result--; value<<=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet16(o.rw, "result"));
                    method.append(";}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst2.BsrGwEw_mem) {
                    Inst2.BsrGwEw_mem o = (Inst2.BsrGwEw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int value=Memory.mem_readw(eaa);if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 15;while ((value & 0x8000)==0) { result--; value<<=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameSet16(o.rw, "result"));
                    method.append(";}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x1be: // MOVSX Gw,Eb
                if (op instanceof Inst2.MovsxGwEb_reg) {
                    Inst2.MovsxGwEb_reg o = (Inst2.MovsxGwEb_reg) op;
                    method.append(nameSet16(o.rw, "(byte)" + nameGet8(o.earb)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.MovsxGwEb_mem) {
                    Inst2.MovsxGwEb_mem o = (Inst2.MovsxGwEb_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameSet16(o.rw, "(byte)Memory.mem_readb(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c0: // XADD Gb,Eb
            case 0x3c0:
                if (op instanceof Inst2.XaddGbEb_reg) {
                    Inst2.XaddGbEb_reg o = (Inst2.XaddGbEb_reg) op;
                    method.append("short oldrmrb=");
                    method.append(nameGet8(o.rb));
                    method.append(nameSet8(o.rb, nameGet8(o.earb)));
                    method.append(";");
                    method.append(nameSet8(o.earb, "(short)(" + nameGet8(o.earb) + "+oldrmrb)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.XaddGbEb_mem) {
                    Inst2.XaddGbEb_mem o = (Inst2.XaddGbEb_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";short oldrmrb=");
                    method.append(nameGet8(o.rb));
                    method.append(";short val = Memory.mem_readb(eaa);Memory.mem_writeb(eaa,val+oldrmrb);");
                    method.append(nameSet8(o.rb, "val"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c1: // XADD Gw,Ew
                if (op instanceof Inst2.XaddGwEw_reg) {
                    Inst2.XaddGwEw_reg o = (Inst2.XaddGwEw_reg) op;
                    method.append("int oldrmrw=");
                    method.append(nameGet16(o.rw));
                    method.append(";");
                    method.append(nameSet16(o.rw, nameGet16(o.earw)));
                    method.append(";");
                    method.append(nameSet16(o.earw, nameGet16(o.earw) + "+oldrmrw"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.XaddGwEw_mem) {
                    Inst2.XaddGwEw_mem o = (Inst2.XaddGwEw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int oldrmrb=");
                    method.append(nameGet16(o.rw));
                    method.append(";int val = Memory.mem_readw(eaa);Memory.mem_writew(eaa,val+oldrmrb);");
                    method.append(nameSet16(o.rw, "val"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c8: // BSWAP AX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1c9: // BSWAP CX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1ca: // BSWAP DX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cb: // BSWAP BX
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cc: // BSWAP SP
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cd: // BSWAP BP
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1ce: // BSWAP SI
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x1cf: // BSWAP DI
                if (op instanceof Inst2.Bswapw) {
                    Inst2.Bswapw o = (Inst2.Bswapw) op;
                    method.append(nameSet16(o.reg, "Instructions.BSWAPW(" + nameGet16(o.reg) + ")"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x201: // ADD Ed,Gd
                if (op instanceof Inst3.Addd_reg) {
                    Inst3.Addd_reg o = (Inst3.Addd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ADDD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.AddEdGd_mem) {
                    Inst3.AddEdGd_mem o = (Inst3.AddEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.ADDD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x203: // ADD Gd,Ed
                if (op instanceof Inst3.Addd_reg) {
                    Inst3.Addd_reg o = (Inst3.Addd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ADDD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.AddGdEd_mem) {
                    Inst3.AddGdEd_mem o = (Inst3.AddGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ADDD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x205: // ADD EAX,Id
                if (op instanceof Inst3.AddEaxId) {
                    Inst3.AddEaxId o = (Inst3.AddEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.ADDD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x206: // PUSH ES
                if (op instanceof Inst3.Push32ES) {
                    Inst3.Push32ES o = (Inst3.Push32ES) op;
                    method.append("CPU.CPU_Push32(CPU.Segs_ESval);");
                    return true;
                }
                break;
            case 0x207: // POP ES
                if (op instanceof Inst3.Pop32ES) {
                    Inst3.Pop32ES o = (Inst3.Pop32ES) op;
                    method.append("if (CPU.CPU_PopSegES(true)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x209: // OR Ed,Gd
                if (op instanceof Inst3.Ord_reg) {
                    Inst3.Ord_reg o = (Inst3.Ord_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ORD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.OrEdGd_mem) {
                    Inst3.OrEdGd_mem o = (Inst3.OrEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.ORD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x20b: // OR Gd,Ed
                if (op instanceof Inst3.Ord_reg) {
                    Inst3.Ord_reg o = (Inst3.Ord_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ORD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.OrGdEd_mem) {
                    Inst3.OrGdEd_mem o = (Inst3.OrGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ORD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x20d: // OR EAX,Id
                if (op instanceof Inst3.OrEaxId) {
                    Inst3.OrEaxId o = (Inst3.OrEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.ORD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x20e: // PUSH CS
                if (op instanceof Inst3.Push32CS) {
                    Inst3.Push32CS o = (Inst3.Push32CS) op;
                    method.append("CPU.CPU_Push32(CPU.Segs_CSval);");
                    return true;
                }
                break;
            case 0x211: // ADC Ed,Gd
                if (op instanceof Inst3.Adcd_reg) {
                    Inst3.Adcd_reg o = (Inst3.Adcd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ADCD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.AdcEdGd_mem) {
                    Inst3.AdcEdGd_mem o = (Inst3.AdcEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.ADCD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x213: // ADC Gd,Ed
                if (op instanceof Inst3.Adcd_reg) {
                    Inst3.Adcd_reg o = (Inst3.Adcd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ADCD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.AdcGdEd_mem) {
                    Inst3.AdcGdEd_mem o = (Inst3.AdcGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ADCD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x215: // ADC EAX,Id
                if (op instanceof Inst3.AdcEaxId) {
                    Inst3.AdcEaxId o = (Inst3.AdcEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.ADCD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x216: // PUSH SS
                if (op instanceof Inst3.Push32SS) {
                    Inst3.Push32SS o = (Inst3.Push32SS) op;
                    method.append("CPU.CPU_Push32(CPU.Segs_SSval);");
                    return true;
                }
                break;
            case 0x217: // POP SS
                if (op instanceof Inst3.Pop32SS) {
                    Inst3.Pop32SS o = (Inst3.Pop32SS) op;
                    method.append("if (CPU.CPU_PopSegSS(true)) return RUNEXCEPTION();Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                break;
            case 0x219: // SBB Ed,Gd
                if (op instanceof Inst3.Sbbd_reg) {
                    Inst3.Sbbd_reg o = (Inst3.Sbbd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.SBBD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.SbbEdGd_mem) {
                    Inst3.SbbEdGd_mem o = (Inst3.SbbEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.SBBD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x21b: // SBB Gd,Ed
                if (op instanceof Inst3.Sbbd_reg) {
                    Inst3.Sbbd_reg o = (Inst3.Sbbd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.SBBD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.SbbGdEd_mem) {
                    Inst3.SbbGdEd_mem o = (Inst3.SbbGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.SBBD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x21d: // SBB EAX,Id
                if (op instanceof Inst3.SbbEaxId) {
                    Inst3.SbbEaxId o = (Inst3.SbbEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.SBBD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x21e: // PUSH DS
                if (op instanceof Inst3.Push32DS) {
                    Inst3.Push32DS o = (Inst3.Push32DS) op;
                    method.append("CPU.CPU_Push32(CPU.Segs_DSval);");
                    return true;
                }
                break;
            case 0x21f: // POP DS
                if (op instanceof Inst3.Pop32DS) {
                    Inst3.Pop32DS o = (Inst3.Pop32DS) op;
                    method.append("if (CPU.CPU_PopSegDS(true)) return RUNEXCEPTION();Core.base_ds=CPU.Segs_DSphys;Core.base_val_ds= CPU_Regs.ds;");
                    return true;
                }
                break;
            case 0x221: // AND Ed,Gd
                if (op instanceof Inst3.Andd_reg) {
                    Inst3.Andd_reg o = (Inst3.Andd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ANDD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.AndEdGd_mem) {
                    Inst3.AndEdGd_mem o = (Inst3.AndEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.ANDD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x223: // AND Gd,Ed
                if (op instanceof Inst3.Andd_reg) {
                    Inst3.Andd_reg o = (Inst3.Andd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ANDD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.AndGdEd_mem) {
                    Inst3.AndGdEd_mem o = (Inst3.AndGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.ANDD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x225: // AND EAX,Id
                if (op instanceof Inst3.AndEaxId) {
                    Inst3.AndEaxId o = (Inst3.AndEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.ANDD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x229: // SUB Ed,Gd
                if (op instanceof Inst3.Subd_reg) {
                    Inst3.Subd_reg o = (Inst3.Subd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.SUBD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.SubEdGd_mem) {
                    Inst3.SubEdGd_mem o = (Inst3.SubEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.SUBD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x22b: // SUB Gd,Ed
                if (op instanceof Inst3.Subd_reg) {
                    Inst3.Subd_reg o = (Inst3.Subd_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.SUBD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.SubGdEd_mem) {
                    Inst3.SubGdEd_mem o = (Inst3.SubGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.SUBD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x22d: // SUB EAX,Id
                if (op instanceof Inst3.SubEaxId) {
                    Inst3.SubEaxId o = (Inst3.SubEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.SUBD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x231: // XOR Ed,Gd
                if (op instanceof Inst3.Xord_reg) {
                    Inst3.Xord_reg o = (Inst3.Xord_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.XORD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.XorEdGd_mem) {
                    Inst3.XorEdGd_mem o = (Inst3.XorEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.XORD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append("Memory.mem_readd(eaa)));");
                    return true;
                }
                break;
            case 0x233: // XOR Gd,Ed
                if (op instanceof Inst3.Xord_reg) {
                    Inst3.Xord_reg o = (Inst3.Xord_reg) op;
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.XORD(");
                    method.append(nameGet32(o.g));
                    method.append(", ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.XorGdEd_mem) {
                    Inst3.XorGdEd_mem o = (Inst3.XorGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.g, method);
                    method.append(";");
                    method.append(nameGet32(o.e));
                    method.append("=Instructions.XORD(Memory.mem_readd(eaa), ");
                    method.append(nameGet32(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x235: // XOR EAX,Id
                if (op instanceof Inst3.XorEaxId) {
                    Inst3.XorEaxId o = (Inst3.XorEaxId) op;
                    method.append("CPU_Regs.reg_eax.dword=Instructions.XORD(");
                    method.append(o.i);
                    method.append(", CPU_Regs.reg_eax.dword);");
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
                    method.append("int eaa=");
                    toStringValue(o.e, method);
                    method.append(";Instructions.CMPD(");
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
                    method.append("int eaa = ");
                    toStringValue(o.g, method);
                    method.append(";Instructions.CMPD(Memory.mem_readd(eaa), ");
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
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x241: // INC ECX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x242: // INC EDX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x243: // INC EBX
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x244: // INC ESP
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x245: // INC EBP
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x246: // INC ESI
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x247: // INC EDI
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x248: // DEC EAX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x249: // DEC ECX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x24a: // DEC EDX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x24b: // DEC EBX
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x24c: // DEC ESP
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x24d: // DEC EBP
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x24e: // DEC ESI
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x24f: // DEC EDI
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
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
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x259: // POP ECX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25a: // POP EDX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25b: // POP EBX
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25c: // POP ESP
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25d: // POP EBP
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25e: // POP ESI
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x25f: // POP EDI
                if (op instanceof Inst3.Pop32_reg) {
                    Inst3.Pop32_reg o = (Inst3.Pop32_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                break;
            case 0x260: // PUSHAD
                if (op instanceof Inst3.Pushad) {
                    Inst3.Pushad o = (Inst3.Pushad) op;
                    method.append("int tmpesp = CPU_Regs.reg_esp.dword;CPU.CPU_Push32(CPU_Regs.reg_eax.dword);CPU.CPU_Push32(CPU_Regs.reg_ecx.dword);CPU.CPU_Push32(CPU_Regs.reg_edx.dword);CPU.CPU_Push32(CPU_Regs.reg_ebx.dword);CPU.CPU_Push32(tmpesp);CPU.CPU_Push32(CPU_Regs.reg_ebp.dword);CPU.CPU_Push32(CPU_Regs.reg_esi.dword);CPU.CPU_Push32(CPU_Regs.reg_edi.dword);");
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
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int bound_min=Memory.mem_readd(eaa);int bound_max=Memory.mem_readd(eaa + 4);int rmrd = rd.dword;if (rmrd < bound_min || rmrd > bound_max) {return EXCEPTION(5);}");
                    return true;
                }
                break;
            case 0x263: // ARPL Ed,Rd
                if (op instanceof Inst3.ArplEdRd_reg) {
                    Inst3.ArplEdRd_reg o = (Inst3.ArplEdRd_reg) op;
                    method.append("if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    method.append("IntRef ref = new IntRef(");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    method.append("CPU.CPU_ARPL(ref, ");
                    method.append(nameGet16(o.rd));
                    method.append(");");
                    method.append(nameGet32(o.eard));
                    method.append("=ref.value;");
                    return true;
                }
                if (op instanceof Inst3.ArplEdRd_mem) {
                    Inst3.ArplEdRd_mem o = (Inst3.ArplEdRd_mem) op;
                    method.append("if (((CPU.cpu.pmode) && (CPU_Regs.flags & CPU_Regs.VM)!=0) || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";IntRef ref = new IntRef(Memory.mem_readw(eaa));");
                    method.append("CPU.CPU_ARPL(ref, ");
                    method.append(nameGet16(o.rd));
                    method.append(");Memory.mem_writed(eaa,ref.value);");
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
                    method.append(nameGet32(o.rd));
                    method.append("=Instructions.DIMULD(");
                    method.append(nameGet32(o.eard));
                    method.append(", ");
                    method.append(o.op3);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.ImulGdEdId_mem) {
                    Inst3.ImulGdEdId_mem o = (Inst3.ImulGdEdId_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameGet32(o.rd));
                    method.append("=Instructions.DIMULD(Memory.mem_readd(eaa),");
                    method.append(o.op3);
                    method.append(");");
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
                    method.append(nameGet32(o.rd));
                    method.append("=Instructions.DIMULD(");
                    method.append(nameGet32(o.eard));
                    method.append(", ");
                    method.append(o.op3);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.ImulGdEdIb_mem) {
                    Inst3.ImulGdEdIb_mem o = (Inst3.ImulGdEdIb_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameGet32(o.rd));
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
                    method.append(")) return RUNEXCEPTION();Core.rep_zero = ");
                    method.append(o.rep_zero);
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
                    method.append(")) return RUNEXCEPTION();Core.rep_zero = ");
                    method.append(o.rep_zero);
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
                    compile(o, "Flags.TFLG_O()", method);
                    return false;
                }
                break;
            case 0x271: // JNO
                if (op instanceof Inst3.JumpCond32_b_no) {
                    Inst3.JumpCond32_b_no o = (Inst3.JumpCond32_b_no) op;
                    compile(o, "Flags.TFLG_NO()", method);
                    return false;
                }
                break;
            case 0x272: // JB
                if (op instanceof Inst3.JumpCond32_b_b) {
                    Inst3.JumpCond32_b_b o = (Inst3.JumpCond32_b_b) op;
                    compile(o, "Flags.TFLG_B()", method);
                    return false;
                }
                break;
            case 0x273: // JNB
                if (op instanceof Inst3.JumpCond32_b_nb) {
                    Inst3.JumpCond32_b_nb o = (Inst3.JumpCond32_b_nb) op;
                    compile(o, "Flags.TFLG_NB()", method);
                    return false;
                }
                break;
            case 0x274: // JZ
                if (op instanceof Inst3.JumpCond32_b_z) {
                    Inst3.JumpCond32_b_z o = (Inst3.JumpCond32_b_z) op;
                    compile(o, "Flags.TFLG_Z()", method);
                    return false;
                }
                break;
            case 0x275: // JNZ
                if (op instanceof Inst3.JumpCond32_b_nz) {
                    Inst3.JumpCond32_b_nz o = (Inst3.JumpCond32_b_nz) op;
                    compile(o, "Flags.TFLG_NZ()", method);
                    return false;
                }
                break;
            case 0x276: // JBE
                if (op instanceof Inst3.JumpCond32_b_be) {
                    Inst3.JumpCond32_b_be o = (Inst3.JumpCond32_b_be) op;
                    compile(o, "Flags.TFLG_BE()", method);
                    return false;
                }
                break;
            case 0x277: // JNBE
                if (op instanceof Inst3.JumpCond32_b_nbe) {
                    Inst3.JumpCond32_b_nbe o = (Inst3.JumpCond32_b_nbe) op;
                    compile(o, "Flags.TFLG_NBE()", method);
                    return false;
                }
                break;
            case 0x278: // JS
                if (op instanceof Inst3.JumpCond32_b_s) {
                    Inst3.JumpCond32_b_s o = (Inst3.JumpCond32_b_s) op;
                    compile(o, "Flags.TFLG_S()", method);
                    return false;
                }
                break;
            case 0x279: // JNS
                if (op instanceof Inst3.JumpCond32_b_ns) {
                    Inst3.JumpCond32_b_ns o = (Inst3.JumpCond32_b_ns) op;
                    compile(o, "Flags.TFLG_NS()", method);
                    return false;
                }
                break;
            case 0x27a: // JP
                if (op instanceof Inst3.JumpCond32_b_p) {
                    Inst3.JumpCond32_b_p o = (Inst3.JumpCond32_b_p) op;
                    compile(o, "Flags.TFLG_P()", method);
                    return false;
                }
                break;
            case 0x27b: // JNP
                if (op instanceof Inst3.JumpCond32_b_np) {
                    Inst3.JumpCond32_b_np o = (Inst3.JumpCond32_b_np) op;
                    compile(o, "Flags.TFLG_NP()", method);
                    return false;
                }
                break;
            case 0x27c: // JL
                if (op instanceof Inst3.JumpCond32_b_l) {
                    Inst3.JumpCond32_b_l o = (Inst3.JumpCond32_b_l) op;
                    compile(o, "Flags.TFLG_L()", method);
                    return false;
                }
                break;
            case 0x27d: // JNL
                if (op instanceof Inst3.JumpCond32_b_nl) {
                    Inst3.JumpCond32_b_nl o = (Inst3.JumpCond32_b_nl) op;
                    compile(o, "Flags.TFLG_NL()", method);
                    return false;
                }
                break;
            case 0x27e: // JLE
                if (op instanceof Inst3.JumpCond32_b_le) {
                    Inst3.JumpCond32_b_le o = (Inst3.JumpCond32_b_le) op;
                    compile(o, "Flags.TFLG_LE()", method);
                    return false;
                }
                break;
            case 0x27f: // JNLE
                if (op instanceof Inst3.JumpCond32_b_nle) {
                    Inst3.JumpCond32_b_nle o = (Inst3.JumpCond32_b_nle) op;
                    compile(o, "Flags.TFLG_NLE()", method);
                    return false;
                }
                break;
            case 0x281: // Grpl Ed,Id
                if (op instanceof Inst3.GrplEdId_reg_add) {
                    Inst3.GrplEdId_reg_add o = (Inst3.GrplEdId_reg_add) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ADDD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_or) {
                    Inst3.GrplEdId_reg_or o = (Inst3.GrplEdId_reg_or) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ORD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_adc) {
                    Inst3.GrplEdId_reg_adc o = (Inst3.GrplEdId_reg_adc) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ADCD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sbb) {
                    Inst3.GrplEdId_reg_sbb o = (Inst3.GrplEdId_reg_sbb) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SBBD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_and) {
                    Inst3.GrplEdId_reg_and o = (Inst3.GrplEdId_reg_and) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ANDD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sub) {
                    Inst3.GrplEdId_reg_sub o = (Inst3.GrplEdId_reg_sub) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SUBD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_xor) {
                    Inst3.GrplEdId_reg_xor o = (Inst3.GrplEdId_reg_xor) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.XORD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
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
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ADDD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_or) {
                    Inst3.GrplEdId_mem_or o = (Inst3.GrplEdId_mem_or) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ORD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_adc) {
                    Inst3.GrplEdId_mem_adc o = (Inst3.GrplEdId_mem_adc) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ADCD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sbb) {
                    Inst3.GrplEdId_mem_sbb o = (Inst3.GrplEdId_mem_sbb) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.SBBD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_and) {
                    Inst3.GrplEdId_mem_and o = (Inst3.GrplEdId_mem_and) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ANDD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sub) {
                    Inst3.GrplEdId_mem_sub o = (Inst3.GrplEdId_mem_sub) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.SUBD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_xor) {
                    Inst3.GrplEdId_mem_xor o = (Inst3.GrplEdId_mem_xor) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.XORD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_cmp) {
                    Inst3.GrplEdId_mem_cmp o = (Inst3.GrplEdId_mem_cmp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.CMPD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x283: // Grpl Ed,Ix
                if (op instanceof Inst3.GrplEdId_reg_add) {
                    Inst3.GrplEdId_reg_add o = (Inst3.GrplEdId_reg_add) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ADDD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_or) {
                    Inst3.GrplEdId_reg_or o = (Inst3.GrplEdId_reg_or) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ORD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_adc) {
                    Inst3.GrplEdId_reg_adc o = (Inst3.GrplEdId_reg_adc) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ADCD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sbb) {
                    Inst3.GrplEdId_reg_sbb o = (Inst3.GrplEdId_reg_sbb) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SBBD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_and) {
                    Inst3.GrplEdId_reg_and o = (Inst3.GrplEdId_reg_and) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ANDD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_sub) {
                    Inst3.GrplEdId_reg_sub o = (Inst3.GrplEdId_reg_sub) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SUBD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_reg_xor) {
                    Inst3.GrplEdId_reg_xor o = (Inst3.GrplEdId_reg_xor) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.XORD(");
                    method.append(o.ib);
                    method.append(", ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
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
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ADDD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_or) {
                    Inst3.GrplEdId_mem_or o = (Inst3.GrplEdId_mem_or) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ORD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_adc) {
                    Inst3.GrplEdId_mem_adc o = (Inst3.GrplEdId_mem_adc) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ADCD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sbb) {
                    Inst3.GrplEdId_mem_sbb o = (Inst3.GrplEdId_mem_sbb) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.SBBD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_and) {
                    Inst3.GrplEdId_mem_and o = (Inst3.GrplEdId_mem_and) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.ANDD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_sub) {
                    Inst3.GrplEdId_mem_sub o = (Inst3.GrplEdId_mem_sub) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.SUBD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_xor) {
                    Inst3.GrplEdId_mem_xor o = (Inst3.GrplEdId_mem_xor) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writed(eaa, Instructions.XORD(");
                    method.append(o.ib);
                    method.append(" ,Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.GrplEdId_mem_cmp) {
                    Inst3.GrplEdId_mem_cmp o = (Inst3.GrplEdId_mem_cmp) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.CMPD(");
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
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.TESTD(");
                    method.append(nameGet32(o.rd));
                    method.append(" ,Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x287: // XCHG Ed,Gd
                if (op instanceof Inst3.XchgEdGd_reg) {
                    Inst3.XchgEdGd_reg o = (Inst3.XchgEdGd_reg) op;
                    method.append("int oldrmrd = ");
                    method.append(nameGet32(o.rd));
                    method.append(";");
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    method.append(nameGet32(o.eard));
                    method.append("=oldrmrd;");
                    return true;
                }
                if (op instanceof Inst3.XchgEdGd_mem) {
                    Inst3.XchgEdGd_mem o = (Inst3.XchgEdGd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int oldrmrd = ");
                    method.append(nameGet32(o.rd));
                    method.append("; int tmp = Memory.mem_readd(eaa);Memory.mem_writed(eaa, oldrmrd);");
                    method.append(nameGet32(o.rd));
                    method.append("=tmp;");
                    return true;
                }
                break;
            case 0x289: // MOV Ed,Gd
                if (op instanceof Inst3.MovEdGd_reg) {
                    Inst3.MovEdGd_reg o = (Inst3.MovEdGd_reg) op;
                    method.append(nameSet32(o.eard, nameGet32(o.rd)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.MovEdGd_mem) {
                    Inst3.MovEdGd_mem o = (Inst3.MovEdGd_mem) op;
                    method.append("Memory.mem_writed(");
                    toStringValue(o.get_eaa, method);
                    method.append(", ");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                break;
            case 0x28b: // MOV Gd,Ed
                if (op instanceof Inst3.MovGdEd_reg) {
                    Inst3.MovGdEd_reg o = (Inst3.MovGdEd_reg) op;
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.MovGdEd_mem) {
                    Inst3.MovGdEd_mem o = (Inst3.MovGdEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";");
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x28c: // Mov Ew,Sw
                if (op instanceof Inst3.MovEdEs_reg) {
                    Inst3.MovEdEs_reg o = (Inst3.MovEdEs_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.Segs_ESval & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwEs_mem) {
                    Inst1.MovEwEs_mem o = (Inst1.MovEwEs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_ESval);");
                    return true;
                }
                if (op instanceof Inst3.MovEdCs_reg) {
                    Inst3.MovEdCs_reg o = (Inst3.MovEdCs_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.Segs_CSval & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwCs_mem) {
                    Inst1.MovEwCs_mem o = (Inst1.MovEwCs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_CSval);");
                    return true;
                }
                if (op instanceof Inst3.MovEdSs_reg) {
                    Inst3.MovEdSs_reg o = (Inst3.MovEdSs_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.Segs_SSval & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwSs_mem) {
                    Inst1.MovEwSs_mem o = (Inst1.MovEwSs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_SSval);");
                    return true;
                }
                if (op instanceof Inst3.MovEdDs_reg) {
                    Inst3.MovEdDs_reg o = (Inst3.MovEdDs_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.Segs_DSval & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwDs_mem) {
                    Inst1.MovEwDs_mem o = (Inst1.MovEwDs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_DSval);");
                    return true;
                }
                if (op instanceof Inst3.MovEdFs_reg) {
                    Inst3.MovEdFs_reg o = (Inst3.MovEdFs_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.Segs_FSval & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwFs_mem) {
                    Inst1.MovEwFs_mem o = (Inst1.MovEwFs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_FSval);");
                    return true;
                }
                if (op instanceof Inst3.MovEdGs_reg) {
                    Inst3.MovEdGs_reg o = (Inst3.MovEdGs_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.Segs_GSval & 0xFFFF;");
                    return true;
                }
                if (op instanceof Inst1.MovEwGs_mem) {
                    Inst1.MovEwGs_mem o = (Inst1.MovEwGs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, method);
                    method.append(", CPU.Segs_GSval);");
                    return true;
                }
                break;
            case 0x28d: // LEA Gd
                if (op instanceof Inst3.LeaGd_32) {
                    Inst3.LeaGd_32 o = (Inst3.LeaGd_32) op;
                    method.append("Core.base_ds=Core.base_ss=0;");
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Core.base_ds=CPU.Segs_DSphys;Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                if (op instanceof Inst3.LeaGd_16) {
                    Inst3.LeaGd_16 o = (Inst3.LeaGd_16) op;
                    method.append("Core.base_ds=Core.base_ss=0;");
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Core.base_ds=CPU.Segs_DSphys;Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                break;
            case 0x28f: // POP Ed
                if (op instanceof Inst3.PopEd_reg) {
                    Inst3.PopEd_reg o = (Inst3.PopEd_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.CPU_Pop32();");
                    return true;
                }
                if (op instanceof Inst3.PopEd_mem) {
                    Inst3.PopEd_mem o = (Inst3.PopEd_mem) op;
                    method.append("int val = CPU.CPU_Pop32();int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, val);");
                    return true;
                }
                break;
            case 0x291: // XCHG ECX,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
                    return true;
                }
                break;
            case 0x292: // XCHG EDX,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
                    return true;
                }
                break;
            case 0x293: // XCHG EBX,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
                    return true;
                }
                break;
            case 0x294: // XCHG ESP,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
                    return true;
                }
                break;
            case 0x295: // XCHG EBP,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
                    return true;
                }
                break;
            case 0x296: // XCHG ESI,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
                    return true;
                }
                break;
            case 0x297: // XCHG EDI,EAX
                if (op instanceof Inst3.XchgEax) {
                    Inst3.XchgEax o = (Inst3.XchgEax) op;
                    method.append("int old = ");
                    method.append(nameGet32(o.reg));
                    method.append(";");
                    method.append(nameGet32(o.reg));
                    method.append("=CPU_Regs.reg_eax.dword;CPU_Regs.reg_eax.dword=old;");
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
                    method.append("if (CPU.CPU_PUSHF(true)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x29d: // POPFD
                if (op instanceof Inst3.Popfd) {
                    Inst3.Popfd o = (Inst3.Popfd) op;
                    method.append("if (CPU.CPU_POPF(true)) return RUNEXCEPTION();");
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
                    method.append("CPU_Regs.reg_eax.dword=Memory.mem_readd(Core.base_ds+");
                    method.append(o.value);
                    method.append(");");
                    return true;
                }
                break;
            case 0x2a3: // MOV Od,EAX
                if (op instanceof Inst3.MovOdEax) {
                    Inst3.MovOdEax o = (Inst3.MovOdEax) op;
                    method.append(" Memory.mem_writed(Core.base_ds+");
                    method.append(o.value);
                    method.append(", CPU_Regs.reg_eax.dword);");
                    return true;
                }
                break;
            case 0x2a5: // MOVSD
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
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2b9: // MOV ECX,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2ba: // MOV EDX,Iw
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bb: // MOV EBX,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bc: // MOV ESP,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bd: // MOV EBP,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2be: // MOV ESI,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2bf: // MOV EDI,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                break;
            case 0x2c1: // GRP2 Ed,Ib
                if (op instanceof Grp2.ROLD_reg) {
                    Grp2.ROLD_reg o = (Grp2.ROLD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_reg) {
                    Grp2.RORD_reg o = (Grp2.RORD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.RORD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_reg) {
                    Grp2.RCLD_reg o = (Grp2.RCLD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_reg) {
                    Grp2.RCRD_reg o = (Grp2.RCRD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_reg) {
                    Grp2.SHLD_reg o = (Grp2.SHLD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_reg) {
                    Grp2.SHRD_reg o = (Grp2.SHRD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_reg) {
                    Grp2.SARD_reg o = (Grp2.SARD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.SARD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLD_mem) {
                    Grp2.ROLD_mem o = (Grp2.ROLD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_mem) {
                    Grp2.RORD_mem o = (Grp2.RORD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.RORD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_mem) {
                    Grp2.RCLD_mem o = (Grp2.RCLD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_mem) {
                    Grp2.RCRD_mem o = (Grp2.RCRD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_mem) {
                    Grp2.SHLD_mem o = (Grp2.SHLD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_mem) {
                    Grp2.SHRD_mem o = (Grp2.SHRD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_mem) {
                    Grp2.SARD_mem o = (Grp2.SARD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.SARD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
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
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();");
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x2c5: // LDS
                if (op instanceof Inst3.Lds32) {
                    Inst3.Lds32 o = (Inst3.Lds32) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append("; if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();");
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);Core.base_ds=CPU.Segs_DSphys;Core.base_val_ds= CPU_Regs.ds;");
                    return true;
                }
                break;
            case 0x2c7: // MOV Ed,Id
                if (op instanceof Inst3.MovId) {
                    Inst3.MovId o = (Inst3.MovId) op;
                    method.append(nameGet32(o.reg));
                    method.append("=");
                    method.append(o.id);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst3.MovId_mem) {
                    Inst3.MovId_mem o = (Inst3.MovId_mem) op;
                    method.append("Memory.mem_writed(");
                    toStringValue(o.get_eaa, method);
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
                    Inst3.Leave32 o = (Inst3.Leave32) op;
                    method.append("CPU_Regs.reg_esp.dword&=CPU.cpu.stack.notmask;CPU_Regs.reg_esp.dword|=(CPU_Regs.reg_ebp.dword & CPU.cpu.stack.mask);CPU_Regs.reg_ebp.dword=CPU.CPU_Pop32();");
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
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_reg) {
                    Grp2.RORD_reg o = (Grp2.RORD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.RORD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_reg) {
                    Grp2.RCLD_reg o = (Grp2.RCLD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_reg) {
                    Grp2.RCRD_reg o = (Grp2.RCRD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_reg) {
                    Grp2.SHLD_reg o = (Grp2.SHLD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_reg) {
                    Grp2.SHRD_reg o = (Grp2.SHRD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_reg) {
                    Grp2.SARD_reg o = (Grp2.SARD_reg) op;
                    if (o.val != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.SARD(");
                        method.append(o.val);
                        method.append(", ");
                        method.append(nameGet32(o.eard));
                        method.append(");");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLD_mem) {
                    Grp2.ROLD_mem o = (Grp2.ROLD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.ROLD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RORD_mem) {
                    Grp2.RORD_mem o = (Grp2.RORD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.RORD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCLD_mem) {
                    Grp2.RCLD_mem o = (Grp2.RCLD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.RCLD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRD_mem) {
                    Grp2.RCRD_mem o = (Grp2.RCRD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.RCRD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLD_mem) {
                    Grp2.SHLD_mem o = (Grp2.SHLD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.SHLD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRD_mem) {
                    Grp2.SHRD_mem o = (Grp2.SHRD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.SHRD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARD_mem) {
                    Grp2.SARD_mem o = (Grp2.SARD_mem) op;
                    if (o.val != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.SARD(");
                        method.append(o.val);
                        method.append(", Memory.mem_readd(eaa)));");
                    }
                    return true;
                }
                break;
            case 0x2d3: // GRP2 Ed,CL
                if (op instanceof Grp2.ROLD_reg_cl) {
                    Grp2.ROLD_reg_cl o = (Grp2.ROLD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.ROLD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.RORD_reg_cl) {
                    Grp2.RORD_reg_cl o = (Grp2.RORD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.RORD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.RCLD_reg_cl) {
                    Grp2.RCLD_reg_cl o = (Grp2.RCLD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.RCLD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.RCRD_reg_cl) {
                    Grp2.RCRD_reg_cl o = (Grp2.RCRD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.RCRD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.SHLD_reg_cl) {
                    Grp2.SHLD_reg_cl o = (Grp2.SHLD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SHLD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.SHRD_reg_cl) {
                    Grp2.SHRD_reg_cl o = (Grp2.SHRD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SHRD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.SARD_reg_cl) {
                    Grp2.SARD_reg_cl o = (Grp2.SARD_reg_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val!=0) ");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.SARD(val, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp2.ROLD_mem_cl) {
                    Grp2.ROLD_mem_cl o = (Grp2.ROLD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.ROLD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RORD_mem_cl) {
                    Grp2.RORD_mem_cl o = (Grp2.RORD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.RORD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCLD_mem_cl) {
                    Grp2.RCLD_mem_cl o = (Grp2.RCLD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.RCLD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCRD_mem_cl) {
                    Grp2.RCRD_mem_cl o = (Grp2.RCRD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.RCRD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHLD_mem_cl) {
                    Grp2.SHLD_mem_cl o = (Grp2.SHLD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.SHLD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHRD_mem_cl) {
                    Grp2.SHRD_mem_cl o = (Grp2.SHRD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.SHRD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SARD_mem_cl) {
                    Grp2.SARD_mem_cl o = (Grp2.SARD_mem_cl) op;
                    method.append("int val = CPU_Regs.reg_ecx.low() & 0x1f;if (val != 0) {int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.SARD(val, Memory.mem_readd(eaa)));}");
                    return true;
                }
                break;
            case 0x2e0: // LOOPNZ
                if (op instanceof Inst3.Loopnz32) {
                    Inst3.Loopnz32 o = (Inst3.Loopnz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && !Flags.get_ZF()", method);
                    return false;
                }
                if (op instanceof Inst3.Loopnz16) {
                    Inst3.Loopnz16 o = (Inst3.Loopnz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && !Flags.get_ZF()", method);
                    return false;
                }
                break;
            case 0x2e1: // LOOPZ
                if (op instanceof Inst3.Loopz32) {
                    Inst3.Loopz32 o = (Inst3.Loopz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && Flags.get_ZF()", method);
                    return false;
                }
                if (op instanceof Inst3.Loopz16) {
                    Inst3.Loopz16 o = (Inst3.Loopz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && Flags.get_ZF()", method);
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
                    method.append(",4)) return RUNEXCEPTION();CPU_Regs.reg_eax.dword=IO.IO_ReadD(");
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
                    method.append(",4)) return RUNEXCEPTION();IO.IO_WriteD(");
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
                    toStringValue(o.get_eaa, method);
                    method.append("));");
                    return true;
                }
                if (op instanceof Grp3.NotEd_reg) {
                    Grp3.NotEd_reg o = (Grp3.NotEd_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=~");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NotEd_mem) {
                    Grp3.NotEd_mem o = (Grp3.NotEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa,~Memory.mem_readd(eaa));");
                    return true;
                }
                if (op instanceof Grp3.NegEd_reg) {
                    Grp3.NegEd_reg o = (Grp3.NegEd_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.Negd(");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.NegEd_mem) {
                    Grp3.NegEd_mem o = (Grp3.NegEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.Negd(Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Grp3.MulAxEd_reg) {
                    Grp3.MulAxEd_reg o = (Grp3.MulAxEd_reg) op;
                    method.append("Instructions.MULD(");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.MulAxEd_mem) {
                    Grp3.MulAxEd_mem o = (Grp3.MulAxEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.MULD(Memory.mem_readd(eaa));");
                    return true;
                }
                if (op instanceof Grp3.IMulAxEd_reg) {
                    Grp3.IMulAxEd_reg o = (Grp3.IMulAxEd_reg) op;
                    method.append("Instructions.IMULD(");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.IMulAxEd_mem) {
                    Grp3.IMulAxEd_mem o = (Grp3.IMulAxEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Instructions.IMULD(Memory.mem_readd(eaa));");
                    return true;
                }
                if (op instanceof Grp3.DivAxEd_reg) {
                    Grp3.DivAxEd_reg o = (Grp3.DivAxEd_reg) op;
                    method.append("int val = ");
                    method.append(nameGet32(o.eard));
                    method.append(";if (Instructions.DIVDr(this, val)==Constants.BR_Jump) return Constants.BR_Jump;");
                    return true;
                }
                if (op instanceof Grp3.DivAxEd_mem) {
                    Grp3.DivAxEd_mem o = (Grp3.DivAxEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = Memory.mem_readd(eaa);");
                    method.append("if (Instructions.DIVDr(this, val)==Constants.BR_Jump) return Constants.BR_Jump;");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEd_reg) {
                    Grp3.IDivAxEd_reg o = (Grp3.IDivAxEd_reg) op;
                    method.append("int val = ");
                    method.append(nameGet32(o.eard));
                    method.append(";if (Instructions.IDIVDr(this, val)==Constants.BR_Jump) return Constants.BR_Jump;");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEd_mem) {
                    Grp3.IDivAxEd_mem o = (Grp3.IDivAxEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = Memory.mem_readd(eaa);");
                    method.append("if (Instructions.IDIVDr(this, val)==Constants.BR_Jump) return Constants.BR_Jump;");
                    return true;
                }
                break;
            case 0x2ff: // GRP 5 Ed
                if (op instanceof Inst3.Incd_reg) {
                    Inst3.Incd_reg o = (Inst3.Incd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.INCD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.Incd_mem) {
                    Inst3.Incd_mem o = (Inst3.Incd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.INCD(Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.Decd_reg) {
                    Inst3.Decd_reg o = (Inst3.Decd_reg) op;
                    method.append(nameGet32(o.reg));
                    method.append(" = Instructions.DECD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst3.Decd_mem) {
                    Inst3.Decd_mem o = (Inst3.Decd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.DECD(Memory.mem_readd(eaa)));");
                    return true;
                }
                if (op instanceof Inst3.CallNearEd_reg) {
                    Inst3.CallNearEd_reg o = (Inst3.CallNearEd_reg) op;
                    method.append("int old = CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(";CPU.CPU_Push32(old);CPU_Regs.reg_eip=");
                    method.append(nameGet32(o.eard));
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.CallNearEd_mem) {
                    Inst3.CallNearEd_mem o = (Inst3.CallNearEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old = CPU_Regs.reg_eip+");
                    method.append(o.eip_count);
                    method.append(";int eip = Memory.mem_readd(eaa); CPU.CPU_Push32(old);CPU_Regs.reg_eip = eip;return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.CallFarEd_mem) {
                    Inst3.CallFarEd_mem o = (Inst3.CallFarEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int newip=Memory.mem_readd(eaa);int newcs=Memory.mem_readw(eaa+4);Flags.FillFlags();CPU.CPU_CALL(true,newcs,newip,CPU_Regs.reg_eip+");
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
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";CPU_Regs.reg_eip=Memory.mem_readd(eaa);return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst3.JmpFarEd_mem) {
                    Inst3.JmpFarEd_mem o = (Inst3.JmpFarEd_mem) op;
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";int newip=Memory.mem_readd(eaa);int newcs=Memory.mem_readw(eaa+4);Flags.FillFlags();CPU.CPU_JMP(true,newcs,newip,CPU_Regs.reg_eip+");
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
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";CPU.CPU_Push32(Memory.mem_readd(eaa));");
                    return true;
                }
                break;
            case 0x301: // Group 7 Ed
                if (op instanceof Inst2.Sgdt_mem) {
                    Inst2.Sgdt_mem o = (Inst2.Sgdt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa,CPU.CPU_SGDT_limit());Memory.mem_writed(eaa+2,CPU.CPU_SGDT_base());");
                    return true;
                }
                if (op instanceof Inst2.Sidt_mem) {
                    Inst2.Sidt_mem o = (Inst2.Sidt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writew(eaa,CPU.CPU_SIDT_limit());Memory.mem_writed(eaa+2,CPU.CPU_SIDT_base());");
                    return true;
                }
                if (op instanceof Inst4.Lgdt_mem) {
                    Inst4.Lgdt_mem o = (Inst4.Lgdt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));");
                    return true;
                }
                if (op instanceof Inst4.Lidt_mem) {
                    Inst4.Lidt_mem o = (Inst4.Lidt_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);CPU.CPU_LIDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2));");
                    return true;
                }
                if (op instanceof Inst2.Smsw_mem) {
                    Inst2.Smsw_mem o = (Inst2.Smsw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append("; Memory.mem_writew(eaa,CPU.CPU_SMSW());");
                    return true;
                }
                if (op instanceof Inst2.Lmsw_mem) {
                    Inst2.Lmsw_mem o = (Inst2.Lmsw_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_LMSW(Memory.mem_readw(eaa))) return RUNEXCEPTION();");
                    return true;
                }
                if (op instanceof Inst2.Invlpg) {
                    Inst2.Invlpg o = (Inst2.Invlpg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);Paging.PAGING_ClearTLB();");
                    return true;
                }
                if (op instanceof Inst2.Lgdt_reg) {
                    Inst2.Lgdt_reg o = (Inst2.Lgdt_reg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);return Constants.BR_Illegal;");
                    return false;
                }
                if (op instanceof Inst2.Lidt_reg) {
                    Inst2.Lidt_reg o = (Inst2.Lidt_reg) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) return EXCEPTION(CPU.EXCEPTION_GP);return Constants.BR_Illegal;");
                    return false;
                }
                if (op instanceof Inst4.Smsw_reg) {
                    Inst4.Smsw_reg o = (Inst4.Smsw_reg) op;
                    method.append(nameGet32(o.eard));
                    method.append("=CPU.CPU_SMSW();");
                    return true;
                }
                if (op instanceof Inst4.Lmsw_reg) {
                    Inst4.Lmsw_reg o = (Inst4.Lmsw_reg) op;
                    method.append("if (CPU.CPU_LMSW(");
                    method.append(nameGet32(o.eard));
                    method.append(")) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x302: // LAR Gd,Ed
                if (op instanceof Inst4.LarGdEd_reg) {
                    Inst4.LarGdEd_reg o = (Inst4.LarGdEd_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;IntRef value=new IntRef(");
                    method.append(nameGet32(o.rd));
                    method.append(");CPU.CPU_LAR(");
                    method.append(nameGet16(o.earw));
                    method.append(",value);");
                    method.append(nameGet32(o.rd));
                    method.append("=value.value;");
                    return true;
                }
                if (op instanceof Inst4.LarGdEd_mem) {
                    Inst4.LarGdEd_mem o = (Inst4.LarGdEd_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";IntRef value=new IntRef(");
                    method.append(nameGet32(o.rd));
                    method.append(");CPU.CPU_LAR(Memory.mem_readw(eaa),value);");
                    method.append(nameGet32(o.rd));
                    method.append("=value.value;");
                    return true;
                }
                break;
            case 0x303: // LSL Gd,Ew
                if (op instanceof Inst4.LslGdEd_reg) {
                    Inst4.LslGdEd_reg o = (Inst4.LslGdEd_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;IntRef value=new IntRef(");
                    method.append(nameGet32(o.rd));
                    method.append(");CPU.CPU_LSL(");
                    method.append(nameGet16(o.earw));
                    method.append(",value);");
                    method.append(nameGet32(o.rd));
                    method.append("=value.value;");
                    return true;
                }
                if (op instanceof Inst4.LslGdEd_mem) {
                    Inst4.LslGdEd_mem o = (Inst4.LslGdEd_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";IntRef value=new IntRef(");
                    method.append(nameGet32(o.rd));
                    method.append(");CPU.CPU_LSL(Memory.mem_readw(eaa),value);");
                    method.append(nameGet32(o.rd));
                    method.append("=value.value;");
                    return true;
                }
                break;
            case 0x380: // JO
                if (op instanceof Inst4.JumpCond32_d_o) {
                    Inst4.JumpCond32_d_o o = (Inst4.JumpCond32_d_o) op;
                    compile(o, "Flags.TFLG_O()", method);
                    return false;
                }
                break;
            case 0x381: // JNO
                if (op instanceof Inst4.JumpCond32_d_no) {
                    Inst4.JumpCond32_d_no o = (Inst4.JumpCond32_d_no) op;
                    compile(o, "Flags.TFLG_NO()", method);
                    return false;
                }
                break;
            case 0x382: // JB
                if (op instanceof Inst4.JumpCond32_d_b) {
                    Inst4.JumpCond32_d_b o = (Inst4.JumpCond32_d_b) op;
                    compile(o, "Flags.TFLG_B()", method);
                    return false;
                }
                break;
            case 0x383: // JNB
                if (op instanceof Inst4.JumpCond32_d_nb) {
                    Inst4.JumpCond32_d_nb o = (Inst4.JumpCond32_d_nb) op;
                    compile(o, "Flags.TFLG_NB()", method);
                    return false;
                }
                break;
            case 0x384: // JZ
                if (op instanceof Inst4.JumpCond32_d_z) {
                    Inst4.JumpCond32_d_z o = (Inst4.JumpCond32_d_z) op;
                    compile(o, "Flags.TFLG_Z()", method);
                    return false;
                }
                break;
            case 0x385: // JNZ
                if (op instanceof Inst4.JumpCond32_d_nz) {
                    Inst4.JumpCond32_d_nz o = (Inst4.JumpCond32_d_nz) op;
                    compile(o, "Flags.TFLG_NZ()", method);
                    return false;
                }
                break;
            case 0x386: // JBE
                if (op instanceof Inst4.JumpCond32_d_be) {
                    Inst4.JumpCond32_d_be o = (Inst4.JumpCond32_d_be) op;
                    compile(o, "Flags.TFLG_BE()", method);
                    return false;
                }
                break;
            case 0x387: // JNBE
                if (op instanceof Inst4.JumpCond32_d_nbe) {
                    Inst4.JumpCond32_d_nbe o = (Inst4.JumpCond32_d_nbe) op;
                    compile(o, "Flags.TFLG_NBE()", method);
                    return false;
                }
                break;
            case 0x388: // JS
                if (op instanceof Inst4.JumpCond32_d_s) {
                    Inst4.JumpCond32_d_s o = (Inst4.JumpCond32_d_s) op;
                    compile(o, "Flags.TFLG_S()", method);
                    return false;
                }
                break;
            case 0x389: // JNS
                if (op instanceof Inst4.JumpCond32_d_ns) {
                    Inst4.JumpCond32_d_ns o = (Inst4.JumpCond32_d_ns) op;
                    compile(o, "Flags.TFLG_NS()", method);
                    return false;
                }
                break;
            case 0x38a: // JP
                if (op instanceof Inst4.JumpCond32_d_p) {
                    Inst4.JumpCond32_d_p o = (Inst4.JumpCond32_d_p) op;
                    compile(o, "Flags.TFLG_P()", method);
                    return false;
                }
                break;
            case 0x38b: // JNP
                if (op instanceof Inst4.JumpCond32_d_np) {
                    Inst4.JumpCond32_d_np o = (Inst4.JumpCond32_d_np) op;
                    compile(o, "Flags.TFLG_NP()", method);
                    return false;
                }
                break;
            case 0x38c: // JL
                if (op instanceof Inst4.JumpCond32_d_l) {
                    Inst4.JumpCond32_d_l o = (Inst4.JumpCond32_d_l) op;
                    compile(o, "Flags.TFLG_L()", method);
                    return false;
                }
                break;
            case 0x38d: // JNL
                if (op instanceof Inst4.JumpCond32_d_nl) {
                    Inst4.JumpCond32_d_nl o = (Inst4.JumpCond32_d_nl) op;
                    compile(o, "Flags.TFLG_NL()", method);
                    return false;
                }
                break;
            case 0x38e: // JLE
                if (op instanceof Inst4.JumpCond32_d_le) {
                    Inst4.JumpCond32_d_le o = (Inst4.JumpCond32_d_le) op;
                    compile(o, "Flags.TFLG_LE()", method);
                    return false;
                }
                break;
            case 0x38f: // JNLE
                if (op instanceof Inst4.JumpCond32_d_nle) {
                    Inst4.JumpCond32_d_nle o = (Inst4.JumpCond32_d_nle) op;
                    compile(o, "Flags.TFLG_NLE()", method);
                    return false;
                }
                break;
            case 0x3a0: // PUSH FS
                if (op instanceof Inst4.PushFS) {
                    Inst4.PushFS o = (Inst4.PushFS) op;
                    method.append("CPU.CPU_Push32(CPU.Segs_FSval);");
                    return true;
                }
                break;
            case 0x3a1: // POP FS
                if (op instanceof Inst4.PopFS) {
                    Inst4.PopFS o = (Inst4.PopFS) op;
                    method.append("if (CPU.CPU_PopSegFS(true)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x3a3: // BT Ed,Gd
                if (op instanceof Inst4.BtEdGd_reg) {
                    Inst4.BtEdGd_reg o = (Inst4.BtEdGd_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & mask)!=0);");
                    return true;
                }
                if (op instanceof Inst4.BtEdGd_mem) {
                    Inst4.BtEdGd_mem o = (Inst4.BtEdGd_mem) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);");
                    return true;
                }
                break;
            case 0x3a4: // SHLD Ed,Gd,Ib
                if (op instanceof Inst4.ShldEdGdIb_reg) {
                    Inst4.ShldEdGdIb_reg o = (Inst4.ShldEdGdIb_reg) op;
                    if (o.op3 != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.DSHLD(");
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
                    if (o.op3 != 0) {
                        method.append("int eaa = ");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.DSHLD(");
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
                    method.append("int op3=CPU_Regs.reg_ecx.low() & 0x1F;if (op3!=0)");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.DSHLD(");
                    method.append(nameGet32(o.rd));
                    method.append(", op3, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.ShldEdGdCl_mem) {
                    Inst4.ShldEdGdCl_mem o = (Inst4.ShldEdGdCl_mem) op;
                    method.append("int op3=CPU_Regs.reg_ecx.low() & 0x1F;if (op3!=0) {");
                    method.append("int eaa = ");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.DSHLD(");
                    method.append(nameGet32(o.rd));
                    method.append(", op3, Memory.mem_readd(eaa)));}");
                    return true;
                }
                break;
            case 0x3a8: // PUSH GS
                if (op instanceof Inst4.PushGS) {
                    Inst4.PushGS o = (Inst4.PushGS) op;
                    method.append("CPU.CPU_Push32(CPU.Segs_GSval);");
                    return true;
                }
                break;
            case 0x3a9: // POP GS
                if (op instanceof Inst4.PopGS) {
                    Inst4.PopGS o = (Inst4.PopGS) op;
                    method.append("if (CPU.CPU_PopSegGS(true)) return RUNEXCEPTION();");
                    return true;
                }
                break;
            case 0x3ab: // BTS Ed,Gd
                if (op instanceof Inst4.BtsEdGd_reg) {
                    Inst4.BtsEdGd_reg o = (Inst4.BtsEdGd_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & mask)!=0);");
                    method.append(nameGet32(o.eard));
                    method.append("|=mask;");
                    return true;
                }
                if (op instanceof Inst4.BtsEdGd_mem) {
                    Inst4.BtsEdGd_mem o = (Inst4.BtsEdGd_mem) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);Memory.mem_writed(eaa,old | mask);");
                    return true;
                }
                break;
            case 0x3ac: // SHRD Ed,Gd,Ib
                if (op instanceof Inst4.ShrdEdGdIb_reg) {
                    Inst4.ShrdEdGdIb_reg o = (Inst4.ShrdEdGdIb_reg) op;
                    if (o.op3 != 0) {
                        method.append(nameGet32(o.eard));
                        method.append("=Instructions.DSHRD(");
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
                    if (o.op3 != 0) {
                        method.append("int eaa=");
                        toStringValue(o.get_eaa, method);
                        method.append(";Memory.mem_writed(eaa, Instructions.DSHRD(");
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
                    method.append("int op3 = CPU_Regs.reg_ecx.low() & 0x1F;if (op3!=0)");
                    method.append(nameGet32(o.eard));
                    method.append("=Instructions.DSHRD(");
                    method.append(nameGet32(o.rd));
                    method.append(", op3, ");
                    method.append(nameGet32(o.eard));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.ShrdEdGdCl_mem) {
                    Inst4.ShrdEdGdCl_mem o = (Inst4.ShrdEdGdCl_mem) op;
                    method.append("int op3=CPU_Regs.reg_ecx.low() & 0x1F;if (op3!=0) {int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Memory.mem_writed(eaa, Instructions.DSHRD(");
                    method.append(nameGet32(o.rd));
                    method.append(", op3, Memory.mem_readd(eaa)));}");
                    return true;
                }
                break;
            case 0x3af: // IMUL Gd,Ed
                if (op instanceof Inst4.ImulGdEd_reg) {
                    Inst4.ImulGdEd_reg o = (Inst4.ImulGdEd_reg) op;
                    method.append(nameGet32(o.rd));
                    method.append("=Instructions.DIMULD(");
                    method.append(nameGet32(o.eard));
                    method.append(",");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.ImulGdEd_mem) {
                    Inst4.ImulGdEd_mem o = (Inst4.ImulGdEd_mem) op;
                    method.append(nameGet32(o.rd));
                    method.append("=Instructions.DIMULD(Memory.mem_readd(");
                    toStringValue(o.get_eaa, method);
                    method.append("),");
                    method.append(nameGet32(o.rd));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3b1: // CMPXCHG Ed,Gd
                if (op instanceof Inst4.CmpxchgEdGd_reg) {
                    Inst4.CmpxchgEdGd_reg o = (Inst4.CmpxchgEdGd_reg) op;
                    method.append("Flags.FillFlags();if (CPU_Regs.reg_eax.dword == ");
                    method.append(nameGet32(o.eard));
                    method.append(") {");
                    method.append(nameGet32(o.eard));
                    method.append("=");
                    method.append(nameGet32(o.rd));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {CPU_Regs.reg_eax.dword=");
                    method.append(nameGet32(o.eard));
                    method.append(";CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                if (op instanceof Inst4.CmpxchgEdGd_mem) {
                    Inst4.CmpxchgEdGd_mem o = (Inst4.CmpxchgEdGd_mem) op;
                    method.append("Flags.FillFlags();int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int val = Memory.mem_readd(eaa);if (CPU_Regs.reg_eax.dword == val) {Memory.mem_writed(eaa,");
                    method.append(nameGet32(o.rd));
                    method.append(");CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {Memory.mem_writed(eaa,val);CPU_Regs.reg_eax.dword=val;CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);}");
                    return true;
                }
                break;
            case 0x3b2: // LSS Ed
                if (op instanceof Inst4.LssEd) {
                    Inst4.LssEd o = (Inst4.LssEd) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();");
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);Core.base_ss=CPU.Segs_SSphys;");
                    return true;
                }
                break;
            case 0x3b3: // BTR Ed,Gd
                if (op instanceof Inst4.BtrEdGd_reg) {
                    Inst4.BtrEdGd_reg o = (Inst4.BtrEdGd_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & mask)!=0);");
                    method.append(nameGet32(o.eard));
                    method.append("&=~mask;");
                    return true;
                }
                if (op instanceof Inst4.BtrEdGd_mem) {
                    Inst4.BtrEdGd_mem o = (Inst4.BtrEdGd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);Memory.mem_writed(eaa,old & ~mask);");
                    return true;
                }
                break;
            case 0x3b4: // LFS Ed
                if (op instanceof Inst4.LfsEd) {
                    Inst4.LfsEd o = (Inst4.LfsEd) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();");
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x3b5: // LGS Ed
                if (op instanceof Inst4.LgsEd) {
                    Inst4.LgsEd o = (Inst4.LgsEd) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa+4))) return RUNEXCEPTION();");
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readd(eaa);");
                    return true;
                }
                break;
            case 0x3b6: // MOVZX Gd,Eb
                if (op instanceof Inst4.MovzxGdEb_reg) {
                    Inst4.MovzxGdEb_reg o = (Inst4.MovzxGdEb_reg) op;
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    method.append(nameGet8(o.earb));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovzxGdEb_mem) {
                    Inst4.MovzxGdEb_mem o = (Inst4.MovzxGdEb_mem) op;
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readb(");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3b7: // MOVXZ Gd,Ew
                if (op instanceof Inst4.MovzxGdEw_reg) {
                    Inst4.MovzxGdEw_reg o = (Inst4.MovzxGdEw_reg) op;
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    method.append(nameGet16(o.earw));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovzxGdEw_mem) {
                    Inst4.MovzxGdEw_mem o = (Inst4.MovzxGdEw_mem) op;
                    method.append(nameGet32(o.rd));
                    method.append("=Memory.mem_readw(");
                    toStringValue(o.get_eaa, method);
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
                    method.append(nameGet32(o.eard));
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
                    method.append(nameGet32(o.eard));
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
                    method.append(nameGet32(o.eard));
                    method.append("&=~");
                    method.append(o.mask);
                    method.append(";else ");
                    method.append(nameGet32(o.eard));
                    method.append("|=");
                    method.append(o.mask);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.BtEdIb_mem) {
                    Inst4.BtEdIb_mem o = (Inst4.BtEdIb_mem) op;
                    method.append("Flags.FillFlags();int old=Memory.mem_readd(");
                    toStringValue(o.get_eaa, method);
                    method.append(");CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);");
                    return true;
                }
                if (op instanceof Inst4.BtsEdIb_mem) {
                    Inst4.BtsEdIb_mem o = (Inst4.BtsEdIb_mem) op;
                    method.append("Flags.FillFlags();int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writed(eaa,old|");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.BtrEdIb_mem) {
                    Inst4.BtrEdIb_mem o = (Inst4.BtrEdIb_mem) op;
                    method.append("Flags.FillFlags();int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);Memory.mem_writed(eaa,old & ~");
                    method.append(o.mask);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst4.BtcEdIb_mem) {
                    Inst4.BtcEdIb_mem o = (Inst4.BtcEdIb_mem) op;
                    method.append("Flags.FillFlags();int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & ");
                    method.append(o.mask);
                    method.append(")!=0);if (CPU_Regs.GETFLAG(CPU_Regs.CF)!=0) old&=~");
                    method.append(o.mask);
                    method.append(";else old|=");
                    method.append(o.mask);
                    method.append(";Memory.mem_writed(eaa,old);");
                    return true;
                }
                break;
            case 0x3bb: // BTC Ed,Gd
                if (op instanceof Inst4.BtcEdGd_reg) {
                    Inst4.BtcEdGd_reg o = (Inst4.BtcEdGd_reg) op;
                    method.append("Flags.FillFlags();int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(");
                    method.append(nameGet32(o.eard));
                    method.append(" & mask)!=0);");
                    method.append(nameGet32(o.eard));
                    method.append("^=mask;");
                    return true;
                }
                if (op instanceof Inst4.BtcEdGd_mem) {
                    Inst4.BtcEdGd_mem o = (Inst4.BtcEdGd_mem) op;
                    method.append("Flags.FillFlags();");
                    method.append("int mask=1 << (");
                    method.append(nameGet32(o.rd));
                    method.append(" & 31);int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";eaa+=(");
                    method.append(nameGet32(o.rd));
                    method.append(">>5)*4;int old=Memory.mem_readd(eaa);CPU_Regs.SETFLAGBIT(CPU_Regs.CF,(old & mask)!=0);Memory.mem_writed(eaa,old ^ mask);");
                    return true;
                }
                break;
            case 0x3bc: // BSF Gd,Ed
                if (op instanceof Inst4.BsfGdEd_reg) {
                    Inst4.BsfGdEd_reg o = (Inst4.BsfGdEd_reg) op;
                    method.append("int value=");
                    method.append(nameGet32(o.eard));
                    method.append(";if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0;while ((value & 0x01)==0) { result++; value>>>=1; } CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameGet32(o.rd));
                    method.append("=result;}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst4.BsfGdEd_mem) {
                    Inst4.BsfGdEd_mem o = (Inst4.BsfGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int value=Memory.mem_readd(eaa);");
                    method.append("if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 0;while ((value & 0x01)==0) { result++; value>>>=1; } CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameGet32(o.rd));
                    method.append("=result;}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x3bd: // BSR Gd,Ed
                if (op instanceof Inst4.BsrGdEd_reg) {
                    Inst4.BsrGdEd_reg o = (Inst4.BsrGdEd_reg) op;
                    method.append("int value=");
                    method.append(nameGet32(o.eard));
                    method.append(";if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 31;while ((value & 0x80000000)==0) { result--; value<<=1; } CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameGet32(o.rd));
                    method.append("=result;} Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                if (op instanceof Inst4.BsrGdEd_mem) {
                    Inst4.BsrGdEd_mem o = (Inst4.BsrGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int value=Memory.mem_readd(eaa);if (value==0) {CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,true);} else {int result = 31;while ((value & 0x80000000)==0) { result--; value<<=1; }CPU_Regs.SETFLAGBIT(CPU_Regs.ZF,false);");
                    method.append(nameGet32(o.rd));
                    method.append("=result;}Flags.lflags.type=Flags.t_UNKNOWN;");
                    return true;
                }
                break;
            case 0x3be: // MOVSX Gd,Eb
                if (op instanceof Inst4.MovsxGdEb_reg) {
                    Inst4.MovsxGdEb_reg o = (Inst4.MovsxGdEb_reg) op;
                    method.append(nameGet32(o.rd));
                    method.append("=(byte)");
                    method.append(nameGet8(o.earb));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovsxGdEb_mem) {
                    Inst4.MovsxGdEb_mem o = (Inst4.MovsxGdEb_mem) op;
                    method.append(nameGet32(o.rd));
                    method.append("=(byte)Memory.mem_readb(");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3bf: // MOVSX Gd,Ew
                if (op instanceof Inst4.MovsxGdEw_reg) {
                    Inst4.MovsxGdEw_reg o = (Inst4.MovsxGdEw_reg) op;
                    method.append(nameGet32(o.rd));
                    method.append("=(short)");
                    method.append(nameGet16(o.earw));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst4.MovsxGdEw_mem) {
                    Inst4.MovsxGdEw_mem o = (Inst4.MovsxGdEw_mem) op;
                    method.append(nameGet32(o.rd));
                    method.append("=(short)Memory.mem_readw(");
                    toStringValue(o.get_eaa, method);
                    method.append(");");
                    return true;
                }
                break;
            case 0x3c1: // XADD Gd,Ed
                if (op instanceof Inst4.XaddGdEd_reg) {
                    Inst4.XaddGdEd_reg o = (Inst4.XaddGdEd_reg) op;
                    method.append("int oldrmrd=");
                    method.append(nameGet32(o.rd));
                    method.append(";");
                    method.append(nameGet32(o.rd));
                    method.append("=");
                    method.append(nameGet32(o.eard));
                    method.append(";");
                    method.append(nameGet32(o.eard));
                    method.append("+=oldrmrd;");
                    return true;
                }
                if (op instanceof Inst4.XaddGdEd_mem) {
                    Inst4.XaddGdEd_mem o = (Inst4.XaddGdEd_mem) op;
                    method.append("int eaa=");
                    toStringValue(o.get_eaa, method);
                    method.append(";int oldrmrd=");
                    method.append(nameGet32(o.rd));
                    method.append(";int val = Memory.mem_readd(eaa);Memory.mem_writed(eaa,val+oldrmrd);");
                    method.append(nameGet32(o.rd));
                    method.append("=val;");
                    return true;
                }
                break;
            case 0x3c8: // BSWAP EAX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3c9: // BSWAP ECX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3ca: // BSWAP EDX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cb: // BSWAP EBX
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cc: // BSWAP ESP
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cd: // BSWAP EBP
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3ce: // BSWAP ESI
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
                    method.append("=Instructions.BSWAPD(");
                    method.append(nameGet32(o.reg));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3cf: // BSWAP EDI
                if (op instanceof Inst4.Bswapd) {
                    Inst4.Bswapd o = (Inst4.Bswapd) op;
                    method.append(nameGet32(o.reg));
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
                    method.append("Core.base_ds= CPU.Segs_DSphys;Core.base_ss=CPU.Segs_SSphys;Core.base_val_ds=CPU_Regs.ds;");
                    return true;
                } else if (op instanceof Decoder.HandledDecode) {
                    return true;
                } else if (op instanceof Decoder.ModifiedDecodeOp) {
                    method.append("return ModifiedDecode.call();");
                    return false;
                } else {
                    Log.exit("[Compiler] Unhandled op: " + op);
                }

        }
        return true;
    }

    static private ClassPool pool = ClassPool.getDefault();
    static java.security.MessageDigest md;

    static {
        pool.importPackage("jdos.cpu.core_dynamic");
        pool.importPackage("jdos.cpu");
        pool.importPackage("jdos.debug");
        pool.importPackage("jdos.fpu");
        pool.importPackage("jdos.hardware");
        pool.importPackage("jdos.util");
        pool.importPackage("jdos.cpu.core_normal");
        pool.importPackage("jdos.cpu.core_share");
        try {
            md = java.security.MessageDigest.getInstance("MD5");
        } catch (Exception e) {

        }
    }

    static private int count = 0;

    static private Op compileMethod(StringBuffer method, boolean jump) {
        //System.out.println(method.toString());
        try {
            CtClass codeBlock = pool.makeClass("CacheBlock" + (count++));
            codeBlock.setSuperclass(pool.getCtClass("jdos.cpu.core_dynamic.Op"));
            if (!jump)
                method.append("return Constants.BR_Normal;");
            method.append("}");
            CtMethod m = CtNewMethod.make("public int call() {" + method.toString(), codeBlock);
            codeBlock.addMethod(m);
            // Make the dynamic class belong to its own class loader so that when we
            // release the decoder block the class and class loader will be unloaded
            URLClassLoader cl = (URLClassLoader) codeBlock.getClass().getClassLoader();
            cl = URLClassLoader.newInstance(cl.getURLs(), cl);
            Class clazz = codeBlock.toClass(cl, null);
            Op compiledCode = (Op) clazz.newInstance();
            codeBlock.detach();
            return compiledCode;
        } catch (Exception e) {
            System.out.println(method.toString());
            e.printStackTrace();
        }
        return null;
    }
}
