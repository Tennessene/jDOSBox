package jdos.cpu.core_dynamic;

import javassist.*;
import jdos.Dosbox;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Instructions;
import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.hardware.RAM;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;

public class Compiler extends Helper {
    public static int compiledMethods = 0;
    public static long compiledOps = 0;
    public static boolean saveClasses = false;
    public static int min_block_size = 1;
    public static boolean alwayUseFastVersion = false; // useful for unit test
    static public final boolean ENABLED = true;
    static final private boolean inlineFlags = true;
    static final private boolean cacheSegments = true;

    private static Thread[] compilerThread = null;
    private static final LinkedList compilerQueue = new LinkedList();

    // :TODO: update CMPXCHG to update flags like in normal core

    // Set to 0 during unit test
    public static int processorCount = 1; //Runtime.getRuntime().availableProcessors()-1; // :TODO: not sure if the compiler is thread safe
    public static boolean thowException = false; // true during unit test

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
                                Op result = do_compile(nextBlock);
                                if (result != null) {
                                    //nextBlock.op = nextBlock.next;
                                    // In Doom, bypassing the DecodeBlock call and instead having Core_dynamic call
                                    // the compiled code directly led to a nice increase in performance (10% at the time
                                    // of testing).  Keep in mind self modified code detection is not enabled within
                                    // the compiled code, hopefully by having the code run in the dynamic core 100-1000
                                    // times before being marked as needing compiling will weed out all the blocks
                                    // that modify themselves.
                                    //
                                    // Do not set nextBlock.parent.code on this thread, because depending on the timing
                                    // on the machine it is being run on, it may result in weird behavior.
                                    nextBlock.compiledOp = result;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("Compiler thread has exited");
                }
            });
            compilerThread[i].start();
        }
    }

    static private byte[] getOpCode(int start, int len) {
        byte[] opCode = new byte[len];
        int src = Paging.getDirectIndexRO(start);
        if (src>=0)
            RAM.memcpy(opCode, 0, src, len);
        else
            Memory.MEM_BlockRead(start, opCode, len);
        return opCode;
    }

    static public void compile(DecodeBlock block) {
        if (block == null) {
            for (int i = 0; i < compilerThread.length; i++)
            try {
                compilerThread[i].join();
            } catch (Exception e) {
            }
            return;
        }
        synchronized (compilerQueue) {
            if (processorCount > 0) {
                compilerQueue.add(block);
                compilerQueue.notify();
            } else {
                Op result = do_compile(block);
                if (result != null)
                    block.compiledOp = result;
            }
        }
    }

    static public void removeFromQueue(DecodeBlock block) {
        synchronized (compilerQueue) {
            compilerQueue.remove(block);
        }
    }
    static private int searchFlag(Op searchOp, int flag, int result) {
        while (searchOp!=null) {
            if ((searchOp.gets() & flag) != 0)
                return result | flag;
            if ((searchOp.sets() & flag) != 0 && (searchOp.sets() & CPU_Regs.MAYBE) == 0)
                return result;
            searchOp = searchOp.next;
        }
        // Always set the flag before a jump
        return result | flag;
    }

    static private boolean isDec(Op op) {
        return (op instanceof Inst3.Decd_reg || op instanceof Inst3.Decd_mem || op instanceof Inst1.Decw_reg || op instanceof Inst1.Decb_reg || op instanceof Inst1.Decb_mem || op instanceof Inst1.Decw_mem || op instanceof Inst1.Decw);
    }

    static final boolean combineEIP = true;  // about 3-4% improvement
    static final boolean combineMemoryAccessEIP = false; // less than 1% improvement

    static public class Seg {
        private StringBuilder method;

        private String ds;
        private boolean defaultDS;
        private int startDS;

        private String ss;
        private boolean defaultSS;
        private int startSS;

        public String val;
        public boolean wasSet;

        public Seg(StringBuilder method) {
            reset();
            this.method = method;
            this.startDS = method.length();
            this.startSS = method.length();
        }

        public void reset() {
            if (cacheSegments)
                ds = "ds";
            else
                ds = "CPU_Regs.reg_dsPhys.dword";

            if (cacheSegments)
                ss = "ss";
            else
                ss = "CPU_Regs.reg_ssPhys.dword";

            val = "CPU_Regs.ds";
            wasSet = false;
            defaultDS = true;
            defaultSS = true;
        }

        public void setES() {
            ds = "CPU_Regs.reg_esPhys.dword";
            ss = "CPU_Regs.reg_esPhys.dword";
            val = "CPU_Regs.es";
            wasSet = true;
            defaultDS = false;
            defaultSS = false;
        }

        public void setCS() {
            ds = "CPU_Regs.reg_csPhys.dword";
            ss = "CPU_Regs.reg_csPhys.dword";
            val = "CPU_Regs.cs";
            wasSet = true;
            defaultDS = false;
            defaultSS = false;
        }

        public void setDS() {
            if (cacheSegments)
                ds = "ds";
            else
                ds = "CPU_Regs.reg_dsPhys.dword";
            ss = "CPU_Regs.reg_dsPhys.dword";
            val = "CPU_Regs.ds";
            wasSet = true;
            defaultDS = true;
            defaultSS = false;
        }

        public void setFS() {
            ds = "CPU_Regs.reg_fsPhys.dword";
            ss = "CPU_Regs.reg_fsPhys.dword";
            val = "CPU_Regs.fs";
            wasSet = true;
            defaultDS = false;
            defaultSS = false;
        }

        public void setGS() {
            ds = "CPU_Regs.reg_gsPhys.dword";
            ss = "CPU_Regs.reg_gsPhys.dword";
            val = "CPU_Regs.gs";
            wasSet = true;
            defaultDS = false;
            defaultSS = false;
        }

        public void setSS() {
            ds = "CPU_Regs.reg_ssPhys.dword";
            if (cacheSegments)
                ss = "ss";
            else
                ss = "CPU_Regs.reg_ssPhys.dword";
            val = "CPU_Regs.ss";
            wasSet = true;
            defaultDS = false;
            defaultSS = true;
        }

        public String getDs() {
            if (cacheSegments && defaultDS && startDS>=0) {
                method.insert(startDS, "int ds = CPU_Regs.reg_dsPhys.dword;");
                startDS = -1;
            }
            return ds;
        }

        public String getSs() {
            if (cacheSegments && defaultSS && startSS>=0) {
                method.insert(startSS, "int ss = CPU_Regs.reg_ssPhys.dword;");
                startSS = -1;
            }
            return ss;
        }
    }
    static private Op opThatSetFlags = null;
    static public Op do_compile(Op op) {
        Op prev = op;
        op = op.next;
        StringBuilder method = new StringBuilder();
        int count = 0;
        Op start = prev;
        Seg seg = new Seg(method);

        int eaaPos = method.length();
        int loopPos = method.length();
        int eipTotal = 0;
        method.append("CPU.CPU_Cycles-=");
        method.append(op.cycle);
        method.append(";");
        int runningEipCount = 0;
        boolean loop = false;
        String loopCondition = null;
        eaaStarted = false;
        valStarted = false;
        val2Started = false;
        longValStarted = false;
        shortValStarted = false;
        opThatSetFlags = null;

        while (op != null) {
            boolean tryPageFault = false;
            if (true) {
                count++;
                if (start == null) {
                    start = prev;
                }
                if (combineEIP) {
                    if (loop || op.usesEip() || op.next==null || (!combineMemoryAccessEIP && op.accessesMemory())) {
                        if (runningEipCount > 0) {
                            method.append("CPU_Regs.reg_eip+=");
                            method.append(runningEipCount);
                            method.append(";\n  ");
                            runningEipCount = 0;
                        }
                    } else if (combineMemoryAccessEIP && op.accessesMemory()) {
                        method.append("try ");
                        tryPageFault = true;
                    }
                }
                if (testLocalVariableAccess)
                    method.append("{");
                method.append("/*" + Integer.toHexString(op.c) + "*/");
                if (op.gets()!=0)
                    method.append("/* Uses Flags */");
                int shouldSet = 0;
                if (op.sets()!=0) {
                    if ((op.sets() & CPU_Regs.CF)!=0)
                        shouldSet = searchFlag(op.next, CPU_Regs.CF, shouldSet);
                    if ((op.sets() & CPU_Regs.OF)!=0)
                        shouldSet = searchFlag(op.next, CPU_Regs.OF, shouldSet);
                    if ((op.sets() & CPU_Regs.SF)!=0)
                        shouldSet = searchFlag(op.next, CPU_Regs.SF, shouldSet);
                    if ((op.sets() & CPU_Regs.ZF)!=0)
                        shouldSet = searchFlag(op.next, CPU_Regs.ZF, shouldSet);
                    if ((op.sets() & CPU_Regs.AF)!=0)
                        shouldSet = searchFlag(op.next, CPU_Regs.AF, shouldSet);
                    if ((op.sets() & CPU_Regs.PF)!=0)
                        shouldSet = searchFlag(op.next, CPU_Regs.PF, shouldSet);
                }
                boolean loopClosed = false;
                if (loop) {
                    loop = false;

                    if (op instanceof Inst1.JumpCond16_b)
                        loop = compile((Inst1.JumpCond16_b)op, loopCondition, method, eipTotal);
                    else if (op instanceof Inst3.JumpCond32_b)
                        loop = compile((Inst2.JumpCond16_w)op, loopCondition, method, eipTotal);
                    else if (op instanceof Inst2.JumpCond16_w)
                        loop = compile((Inst3.JumpCond32_b)op, loopCondition, method, eipTotal);
                    else if (op instanceof Inst4.JumpCond32_d)
                        loop = compile((Inst4.JumpCond32_d)op, loopCondition, method, eipTotal);

                    if (loop) {
                        method.insert(loopPos, "while (true) {");
                        method.append("}");
                        loopClosed = true;
                    }
                }
                loopCondition = null;

                if (op.c == 0xe0) {
                    if (op instanceof Inst1.Loopnz32) {
                        Inst1.Loopnz32 l = (Inst1.Loopnz32)op;
                        if (eipTotal + op.eip_count == -l.offset) {
                            method.append("CPU_Regs.reg_ecx.dword--;");
                            method.append("if (CPU_Regs.reg_ecx.dword!=0 && !Flags.get_ZF()) {");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipTotal);
                            method.append(");if (CPU.CPU_Cycles<0) return Constants.BR_Link1; else continue;}");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
                            method.insert(loopPos, "while (true) {");
                            method.append("}");
                            loopClosed = true;
                        }
                    } else if (op instanceof Inst1.Loopnz16) {
                        Inst1.Loopnz16 l = (Inst1.Loopnz16)op;
                        if (eipTotal + op.eip_count == -l.offset) {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            method.append("if (CPU_Regs.reg_ecx.word()!=0 && !Flags.get_ZF()) {");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipTotal);
                            method.append(");if (CPU.CPU_Cycles<0) return Constants.BR_Link1; else continue;}");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
                            method.insert(loopPos, "while (true) {");
                            method.append("}");
                            loopClosed = true;
                        }
                    }
                } else if (op.c == 0xe1) {
                    if (op instanceof Inst1.Loopz32) {
                        Inst1.Loopz32 l = (Inst1.Loopz32)op;
                        if (eipTotal + op.eip_count == -l.offset) {
                            method.append("CPU_Regs.reg_ecx.dword--;");
                            method.append("if (CPU_Regs.reg_ecx.dword!=0 && Flags.get_ZF()) {");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipTotal);
                            method.append(");if (CPU.CPU_Cycles<0) return Constants.BR_Link1; else continue;}");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
                            method.insert(loopPos, "while (true) {");
                            method.append("}");
                            loopClosed = true;
                        }
                    } else if (op instanceof Inst1.Loopz16) {
                        Inst1.Loopz16 l = (Inst1.Loopz16)op;
                        if (eipTotal + op.eip_count == -l.offset) {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            method.append("if (CPU_Regs.reg_ecx.word()!=0 && Flags.get_ZF()) {");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipTotal);
                            method.append(");if (CPU.CPU_Cycles<0) return Constants.BR_Link1; else continue;}");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
                            method.insert(loopPos, "while (true) {");
                            method.append("}");
                            loopClosed = true;
                        }
                    }
                } else if (op.c == 0xe2) {
                    if (op instanceof Inst1.Loop32) {
                        Inst1.Loop32 l = (Inst1.Loop32)op;
                        if (eipTotal + op.eip_count == -l.offset) {
                            method.append("CPU_Regs.reg_ecx.dword--;");
                            method.append("if (CPU_Regs.reg_ecx.dword!=0) {");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipTotal);
                            method.append(");if (CPU.CPU_Cycles<0) return Constants.BR_Link1; else continue;}");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
                            method.insert(loopPos, "while (true) {");
                            method.append("}");
                            loopClosed = true;
                        }
                    } else if (op instanceof Inst1.Loop16) {
                        Inst1.Loop16 l = (Inst1.Loop16)op;
                        if (eipTotal + op.eip_count == -l.offset) {
                            method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                            method.append("if (CPU_Regs.reg_ecx.word()!=0) {");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipTotal);
                            method.append(");if (CPU.CPU_Cycles<0) return Constants.BR_Link1; else continue;}");
                            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
                            method.insert(loopPos, "while (true) {");
                            method.append("}");
                            loopClosed = true;
                        }
                    }
                    if (shouldSet!=0) {
                        method.append("/* Should set flags */");
                    }
                }
                boolean reset = seg.wasSet;
                if (shouldSet>0)
                    opThatSetFlags = op;
                if (!loopClosed && compile_op(op, alwayUseFastVersion?0:shouldSet, method, (runningEipCount>0)?"CPU_Regs.reg_eip+="+runningEipCount+";":"", seg)) {
                    if (combineEIP) {
                        if (testLocalVariableAccess)
                            method.append("}");
                        if (tryPageFault) {
                            method.append("catch (PageFaultException e) {");
                            if (runningEipCount>0) {
                                method.append("CPU_Regs.reg_eip+=");
                                method.append(runningEipCount);
                                method.append(";");
                            }
                            method.append("throw e;}");
                        }
                        method.append("\n");
                        runningEipCount+=op.eip_count;
                        eipTotal+=op.eip_count;
                    } else {
                        method.append("CPU_Regs.reg_eip+=");
                        method.append(op.eip_count);
                        method.append(";");
                        if (testLocalVariableAccess)
                            method.append("}");
                        method.append("\n");
                    }
                    if ((op.setsSeg() & 0xF)==Op.DS) {
                        if (seg.startDS>=0) {
                            method.append("int ");
                            seg.startDS = -1;
                        }
                        method.append("ds = CPU_Regs.reg_dsPhys.dword;");
                    }
                    if ((op.setsSeg() & 0xF)==Op.SS) {
                        if (seg.startSS>=0) {
                            method.append("int ");
                            seg.startSS = -1;
                        }
                        method.append("ss = CPU_Regs.reg_ssPhys.dword;");
                    }
                } else {
                    if (testLocalVariableAccess)
                        method.append("}");
                    if (tryPageFault) {
                        method.append("catch (PageFaultException e) {");
                        if (runningEipCount>0) {
                            method.append("CPU_Regs.reg_eip+=");
                            method.append(runningEipCount);
                            method.append(";");
                        }
                        method.append("throw e;}");
                    }
                    if (op.next != null) {
                        Log.exit("Instruction "+Integer.toHexString(op.c)+" jumped but there was another instruction after it: "+Integer.toHexString(op.next.c));
                    }
                }
                if (reset) {
                    seg.reset();
                }
                prev = op;
                op = op.next;
            }
        }
        if (count >= min_block_size) {
            if (eaaStarted) {
                method.insert(eaaPos, "int eaa;");
            }
            Op compiled = compileMethod(start, method, true);
            if (compiled != null) {
                // once this is assigned it is live
                start.next = compiled;
                compiledMethods++;
                compiledOps+=count;
                if ((compiledMethods % 250)==0) {
                    System.out.println("Compiled "+compiledMethods+" blocks ("+compilerQueue.size()+" in queue, ave ops/block: "+((float)compiledOps/compiledMethods));
                }
                return compiled;
            }
        }
        return null; // This will happen if the block is too short
    }

    static String nameGet8(Reg reg) {
        if (reg.getParent()==null)
            return "CPU_Regs.reg_"+reg.getName()+".low()";
        return "CPU_Regs.reg_"+reg.getParent().getName()+".high()";
    }

    static void nameGet8(Reg reg, StringBuilder method) {
        if (reg.getParent()==null && reg.getName()==null)
            method.append(String.valueOf(reg.dword));
        else {
            method.append("CPU_Regs.reg_");
            if (reg.getParent()==null) {
                method.append(reg.getName());
                method.append(".low()");
            } else {
                method.append(reg.getParent().getName());
                method.append(".high()");
            }
        }
    }

    static String nameGet16(Reg reg) {
        return "CPU_Regs.reg_"+reg.getName()+".word()";
    }

    static void nameGet16(Reg reg, StringBuilder method) {
        if (reg.getName()==null)
            method.append(String.valueOf(reg.dword));
        else {
            method.append("CPU_Regs.reg_");
            method.append(reg.getName());
            method.append(".word()");
        }
    }

    static String nameGet32(Reg reg) {
        if (reg.getName()==null) return String.valueOf(reg.dword);
        return "CPU_Regs.reg_"+reg.getName()+".dword";
    }

    static void nameGet32(Reg reg, StringBuilder method) {
        if (reg.getName() == null)
            method.append(String.valueOf(reg.dword));
        else {
            method.append("CPU_Regs.reg_");
            method.append(reg.getName());
            method.append(".dword");
        }
    }
    static String nameSet8(Reg reg, String value) {
        if (reg.getParent()==null)
            return "CPU_Regs.reg_"+reg.getName()+".low("+value+")";
        return "CPU_Regs.reg_"+reg.getParent().getName()+".high("+value+")";
    }

    static void nameSet8(Reg reg, StringBuilder method) {
        method.append("CPU_Regs.reg_");
        if (reg.getParent()==null) {
            method.append(reg.getName());
            method.append(".low(");
        } else {
            method.append(reg.getParent().getName());
            method.append(".high(");
        }
    }

    static void nameSet16(Reg reg, String value, StringBuilder method) {
        method.append("CPU_Regs.reg_");
        method.append(reg.getName());
        method.append(".word(");
        method.append(value);
        method.append(")");
    }

    static void nameSet16(Reg reg, String value, String value2, String value3, StringBuilder method) {
        method.append("CPU_Regs.reg_");
        method.append(reg.getName());
        method.append(".word(");
        method.append(value);
        method.append(value2);
        method.append(value3);
        method.append(")");
    }

    static void nameSet16(Reg reg, String value, String value2, String value3, String value4, String value5, StringBuilder method) {
        method.append("CPU_Regs.reg_");
        method.append(reg.getName());
        method.append(".word(");
        method.append(value);
        method.append(value2);
        method.append(value3);
        method.append(value4);
        method.append(value5);
        method.append(")");
    }

    static void nameSet16(Reg reg, String value, String value2, String value3, String value4, String value5, String value6, String value7, StringBuilder method) {
        method.append("CPU_Regs.reg_");
        method.append(reg.getName());
        method.append(".word(");
        method.append(value);
        method.append(value2);
        method.append(value3);
        method.append(value4);
        method.append(value5);
        method.append(value6);
        method.append(value7);
        method.append(")");
    }
    static void nameSet16(Reg reg, StringBuilder method) {
        method.append("CPU_Regs.reg_");
        method.append(reg.getName());
        method.append(".word(");
    }

    static String nameSet32(Reg reg) {
        return reg.getFullName32();
    }

    static void nameSet32(Reg reg, StringBuilder method) {
        method.append(reg.getFullName32());
    }

    static String nameRef(Reg reg) {
        return "CPU_Regs.reg_"+reg.getName();
    }

    static void toStringValue(EaaBase eaa, Seg seg, StringBuilder method) {
        toStringValue(eaa, seg, method, false);
    }
    static void checkForZero(boolean ds, Seg seg, boolean zero, StringBuilder method) {
        if (!zero) {
            if (ds)
                method.append(seg.getDs());
            else
                method.append(seg.getSs());
            method.append("+");
        }
    }
    static void toStringValue(EaaBase eaa, Seg seg, StringBuilder method, boolean zero) {
        if (eaa instanceof Eaa.EA_16_00_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_01_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_02_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+(short)CPU_Regs.reg_esi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_03_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+(short)CPU_Regs.reg_edi.word()) & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_04_n) {
            checkForZero(true, seg, zero, method);
            method.append("(CPU_Regs.reg_esi.word())");
        } else if (eaa instanceof Eaa.EA_16_05_n) {
            checkForZero(true, seg, zero, method);
            method.append("(CPU_Regs.reg_edi.word())");
        } else if (eaa instanceof Eaa.EA_16_06_n) {
            checkForZero(true, seg, zero, method);
            method.append(((Eaa.EA_16_06_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_16_07_n) {
            checkForZero(true, seg, zero, method);
            method.append("(CPU_Regs.reg_ebx.word())");
        } else if (eaa instanceof Eaa.EA_16_40_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_40_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_41_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_41_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_42_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_42_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_43_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_43_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_44_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_44_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_45_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_45_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_46_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+");method.append(((Eaa.EA_16_46_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_47_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+");method.append(((Eaa.EA_16_47_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_80_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_80_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_81_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_81_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_82_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+(short)CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_82_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_83_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+(short)CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_83_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_84_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_esi.word()+");method.append(((Eaa.EA_16_84_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_85_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_edi.word()+");method.append(((Eaa.EA_16_85_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_86_n) {
            checkForZero(false, seg, zero, method);
            method.append("((");nameGet16(CPU_Regs.reg_ebp,method);method.append("+");method.append(((Eaa.EA_16_86_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_16_87_n) {
            checkForZero(true, seg, zero, method);
            method.append("((CPU_Regs.reg_ebx.word()+");method.append(((Eaa.EA_16_87_n)eaa).i);method.append(") & 0xFFFF)");
        } else if (eaa instanceof Eaa.EA_32_00_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_eax.dword");
        } else if (eaa instanceof Eaa.EA_32_01_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_ecx.dword");
        } else if (eaa instanceof Eaa.EA_32_02_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_edx.dword");
        } else if (eaa instanceof Eaa.EA_32_03_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_ebx.dword");
        } else if (eaa instanceof Eaa.EA_32_04_n) {
            Eaa.EA_32_04_n o = (Eaa.EA_32_04_n)eaa;
            checkForZero(o.ds, seg, zero, method);
            method.append(nameGet32(o.reg));method.append("+(");method.append(nameGet32(o.reg2));
            if (o.sib>0) {
                method.append(" << ");
                method.append(o.sib);
            }
            method.append(")");
        } else if (eaa instanceof Eaa.EA_32_05_n) {
            checkForZero(true, seg, zero, method);
            method.append(((Eaa.EA_32_05_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_06_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_esi.dword");
        } else if (eaa instanceof Eaa.EA_32_07_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_edi.dword");
        } else if (eaa instanceof Eaa.EA_32_40_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_eax.dword+");method.append(((Eaa.EA_32_40_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_41_n) {
            checkForZero(true, seg, zero, method);
           method.append("CPU_Regs.reg_ecx.dword+");method.append(((Eaa.EA_32_41_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_42_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_edx.dword+");method.append(((Eaa.EA_32_42_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_43_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_ebx.dword+");method.append(((Eaa.EA_32_43_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_44_n) {
            Eaa.EA_32_44_n o = (Eaa.EA_32_44_n)eaa;
            checkForZero(o.ds, seg, zero, method);
            method.append(nameGet32(o.reg));method.append("+(");method.append(nameGet32(o.reg2));
            if (o.sib>0) {
                method.append(" << ");
                method.append(o.sib);
            }
            method.append(")+");method.append(o.i);
        } else if (eaa instanceof Eaa.EA_32_45_n) {
            checkForZero(false, seg, zero, method);
            nameGet32(CPU_Regs.reg_ebp,method);method.append("+");method.append(((Eaa.EA_32_45_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_46_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_esi.dword+");method.append(((Eaa.EA_32_46_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_47_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_edi.dword+");method.append(((Eaa.EA_32_47_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_80_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_eax.dword+");method.append(((Eaa.EA_32_80_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_81_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_ecx.dword+");method.append(((Eaa.EA_32_81_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_82_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_edx.dword+");method.append(((Eaa.EA_32_82_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_83_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_ebx.dword+");method.append(((Eaa.EA_32_83_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_84_n) {
            Eaa.EA_32_84_n o = (Eaa.EA_32_84_n)eaa;
            checkForZero(o.ds, seg, zero, method);
            method.append(nameGet32(o.reg));method.append("+(");method.append(nameGet32(o.reg2));
            if (o.sib>0) {
                method.append(" << ");
                method.append(o.sib);
            }
            method.append(")+");method.append(o.i);
        } else if (eaa instanceof Eaa.EA_32_85_n) {
            checkForZero(false, seg, zero, method);
            nameGet32(CPU_Regs.reg_ebp,method);method.append("+");method.append(((Eaa.EA_32_85_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_86_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_esi.dword+");method.append(((Eaa.EA_32_86_n)eaa).i);
        } else if (eaa instanceof Eaa.EA_32_87_n) {
            checkForZero(true, seg, zero, method);
            method.append("CPU_Regs.reg_edi.dword+");method.append(((Eaa.EA_32_87_n)eaa).i);
        }
    }
    static String getO() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB: return "Flags.lf_resb() == 0x80";
                case Op.FLAG_TYPE_INCW: return "Flags.lf_resw() == 0x8000";
                case Op.FLAG_TYPE_INCD: return "Flags.lf_resd() == 0x80000000";
                case Op.FLAG_TYPE_DECB: return "Flags.lf_resb() == 0x7f";
                case Op.FLAG_TYPE_DECW: return "Flags.lf_resw() == 0x7fff";
                case Op.FLAG_TYPE_DECD: return "Flags.lf_resd() == 0x7fffffff";
                case Op.FLAG_TYPE_ADDB:
                case Op.FLAG_TYPE_ADCB:
                    return "(((Flags.lf_var1b() ^ Flags.lf_var2b() ^ 0x80) & (Flags.lf_resb() ^ Flags.lf_var2b())) & 0x80) != 0";
                case Op.FLAG_TYPE_ADDW:
                case Op.FLAG_TYPE_ADCW:
                    return "(((Flags.lf_var1w() ^ Flags.lf_var2w() ^ 0x8000) & (Flags.lf_resw() ^ Flags.lf_var2w())) & 0x8000) != 0";
                case Op.FLAG_TYPE_ADDD:
                case Op.FLAG_TYPE_ADCD:
                    return "(((Flags.lf_var1d() ^ Flags.lf_var2d() ^ 0x80000000) & (Flags.lf_resd() ^ Flags.lf_var2d())) & 0x80000000) != 0";
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_XORD:
                    return "false";
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                case Op.FLAG_TYPE_SBBB:
                    return "(((Flags.lf_var1b() ^ Flags.lf_var2b()) & (Flags.lf_var1b() ^ Flags.lf_resb())) & 0x80) != 0";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                case Op.FLAG_TYPE_SBBW:
                    return "(((Flags.lf_var1w() ^ Flags.lf_var2w()) & (Flags.lf_var1w() ^ Flags.lf_resw())) & 0x8000) != 0";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                case Op.FLAG_TYPE_SBBD:
                    return "(((Flags.lf_var1d() ^ Flags.lf_var2d()) & (Flags.lf_var1d() ^ Flags.lf_resd())) & 0x80000000) != 0";
                case Op.FLAG_TYPE_SHLB:
                    return "((Flags.lf_resb() ^ Flags.lf_var1b()) & 0x80) != 0";
                case Op.FLAG_TYPE_SHLW:
                    return "((Flags.lf_resw() ^ Flags.lf_var1w()) & 0x8000) != 0";
                case Op.FLAG_TYPE_SHLD:
                    return "((Flags.lf_resd() ^ Flags.lf_var1d()) & 0x80000000) != 0";
                case Op.FLAG_TYPE_SHRB:
                    return "((Flags.lf_var2b()&0x1f)==1 && Flags.lf_var1b() > 0x80)";
                case Op.FLAG_TYPE_SHRW:
                    return "((Flags.lf_var2b()&0x1f)==1 && Flags.lf_var1w() > 0x8000)";
                case Op.FLAG_TYPE_SHRD:
                    return "((Flags.lf_var2b()&0x1f)==1 && (Flags.lf_var1d() & 0xFFFFFFFFl) > 0x80000000l)";
                case Op.FLAG_TYPE_SARB:
                case Op.FLAG_TYPE_SARW:
                case Op.FLAG_TYPE_SARD:
                    return "false";
                case Op.FLAG_TYPE_DSHLW:
                    return "((Flags.lf_resw() ^ Flags.lf_var1w()) & 0x8000) != 0";
                case Op.FLAG_TYPE_DSHLD:
                    return "((Flags.lf_resd() ^ Flags.lf_var1d()) & 0x80000000) != 0";
                case Op.FLAG_TYPE_DSHRW:
                    return "((Flags.lf_resw() ^ Flags.lf_var1w()) & 0x8000) != 0";
                case Op.FLAG_TYPE_DSHRD:
                    return "((Flags.lf_resd() ^ Flags.lf_var1d()) & 0x80000000) != 0";
                case Op.FLAG_TYPE_NEGB:
                    return "Flags.lf_var1b() == 0x80";
                case Op.FLAG_TYPE_NEGW:
                    return "Flags.lf_var1w() == 0x8000";
                case Op.FLAG_TYPE_NEGD:
                    return "Flags.lf_var1d() == 0x80000000";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.OF) != 0";
            }
        }
        return "Flags.TFLG_O()";
    }
    static String getNO() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB: return "Flags.lf_resb() != 0x80";
                case Op.FLAG_TYPE_INCW: return "Flags.lf_resw() != 0x8000";
                case Op.FLAG_TYPE_INCD: return "Flags.lf_resd() != 0x80000000";
                case Op.FLAG_TYPE_DECB: return "Flags.lf_resb() != 0x7f";
                case Op.FLAG_TYPE_DECW: return "Flags.lf_resw() != 0x7fff";
                case Op.FLAG_TYPE_DECD: return "Flags.lf_resd() != 0x7fffffff";
                case Op.FLAG_TYPE_ADDB:
                case Op.FLAG_TYPE_ADCB:
                    return "(((Flags.lf_var1b() ^ Flags.lf_var2b() ^ 0x80) & (Flags.lf_resb() ^ Flags.lf_var2b())) & 0x80) == 0";
                case Op.FLAG_TYPE_ADDW:
                case Op.FLAG_TYPE_ADCW:
                    return "(((Flags.lf_var1w() ^ Flags.lf_var2w() ^ 0x8000) & (Flags.lf_resw() ^ Flags.lf_var2w())) & 0x8000) == 0";
                case Op.FLAG_TYPE_ADDD:
                case Op.FLAG_TYPE_ADCD:
                    return "(((Flags.lf_var1d() ^ Flags.lf_var2d() ^ 0x80000000) & (Flags.lf_resd() ^ Flags.lf_var2d())) & 0x80000000) == 0";
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_XORD:
                    return "true";
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                case Op.FLAG_TYPE_SBBB:
                    return "(((Flags.lf_var1b() ^ Flags.lf_var2b()) & (Flags.lf_var1b() ^ Flags.lf_resb())) & 0x80) == 0";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                case Op.FLAG_TYPE_SBBW:
                    return "(((Flags.lf_var1w() ^ Flags.lf_var2w()) & (Flags.lf_var1w() ^ Flags.lf_resw())) & 0x8000) == 0";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                case Op.FLAG_TYPE_SBBD:
                    return "(((Flags.lf_var1d() ^ Flags.lf_var2d()) & (Flags.lf_var1d() ^ Flags.lf_resd())) & 0x80000000) == 0";
                case Op.FLAG_TYPE_SHLB:
                   return "((Flags.lf_resb() ^ Flags.lf_var1b()) & 0x80) == 0";
                case Op.FLAG_TYPE_SHLW:
                   return "((Flags.lf_resw() ^ Flags.lf_var1w()) & 0x8000) == 0";
                case Op.FLAG_TYPE_SHLD:
                   return "((Flags.lf_resd() ^ Flags.lf_var1d()) & 0x80000000) == 0";
                case Op.FLAG_TYPE_SHRB:
                   return "((Flags.lf_var2b()&0x1f)!=1 || Flags.lf_var1b() <= 0x80)";
                case Op.FLAG_TYPE_SHRW:
                   return "((Flags.lf_var2b()&0x1f)!=1 || Flags.lf_var1w() <= 0x8000)";
                case Op.FLAG_TYPE_SHRD:
                   return "((Flags.lf_var2b()&0x1f)!=1 || (Flags.lf_var1d() & 0xFFFFFFFFl) <= 0x80000000l)";
                case Op.FLAG_TYPE_SARB:
                case Op.FLAG_TYPE_SARW:
                case Op.FLAG_TYPE_SARD:
                   return "true";
                case Op.FLAG_TYPE_DSHLW:
                   return "((Flags.lf_resw() ^ Flags.lf_var1w()) & 0x8000) == 0";
                case Op.FLAG_TYPE_DSHLD:
                   return "((Flags.lf_resd() ^ Flags.lf_var1d()) & 0x80000000) == 0";
                case Op.FLAG_TYPE_DSHRW:
                   return "((Flags.lf_resw() ^ Flags.lf_var1w()) & 0x8000) == 0";
                case Op.FLAG_TYPE_DSHRD:
                   return "((Flags.lf_resd() ^ Flags.lf_var1d()) & 0x80000000) == 0";
                case Op.FLAG_TYPE_NEGB:
                   return "Flags.lf_var1b() != 0x80";
                case Op.FLAG_TYPE_NEGW:
                   return "Flags.lf_var1w() != 0x8000";
                case Op.FLAG_TYPE_NEGD:
                   return "Flags.lf_var1d() != 0x80000000";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.OF) == 0";
            }
        }
        return "Flags.TFLG_NO()";
    }
    static String getB() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB:
                case Op.FLAG_TYPE_INCW:
                case Op.FLAG_TYPE_INCD:
                case Op.FLAG_TYPE_DECB:
                case Op.FLAG_TYPE_DECW:
                case Op.FLAG_TYPE_DECD:
                    return "CPU_Regs.GETFLAG(CPU_Regs.CF)!=0";
                case Op.FLAG_TYPE_ADDB: return "Flags.lf_resb()<Flags.lf_var1b()";
                case Op.FLAG_TYPE_ADDW: return "Flags.lf_resw()<Flags.lf_var1w()";
                case Op.FLAG_TYPE_ADDD: return "(Flags.lf_resd() & 0xFFFFFFFFl)<(Flags.lf_var1d() & 0xFFFFFFFFl)";
                case Op.FLAG_TYPE_ADCB: return "(Flags.lf_resb()<Flags.lf_var1b() || (Flags.oldcf && (Flags.lf_resb() == Flags.lf_var1b())))";
                case Op.FLAG_TYPE_ADCW: return "(Flags.lf_resw()<Flags.lf_var1w() || (Flags.oldcf && (Flags.lf_resw() == Flags.lf_var1w())))";
                case Op.FLAG_TYPE_ADCD: return "((Flags.lf_resd() & 0xFFFFFFFFl)<(Flags.lf_var1d() & 0xFFFFFFFFl) || (Flags.oldcf && (Flags.lf_resd() == Flags.lf_var1d())))";
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_XORD:
                    return "false";
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "Flags.lf_var1b()<Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "Flags.lf_var1w()<Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "(Flags.lf_var1d() & 0xFFFFFFFFl)<(Flags.lf_var2d() & 0xFFFFFFFFl)";
                case Op.FLAG_TYPE_SBBB:
                    return "(Flags.lf_var1b() < Flags.lf_resb()) || (Flags.oldcf && (Flags.lf_var2b()==0xff))";
                case Op.FLAG_TYPE_SBBW:
                    return "(Flags.lf_var1w() < Flags.lf_resw()) || (Flags.oldcf && (Flags.lf_var2w()==0xffff))";
                case Op.FLAG_TYPE_SBBD:
                    return "((Flags.lf_var1d() & 0xFFFFFFFFl) < (Flags.lf_resd() & 0xFFFFFFFFl)) || (Flags.oldcf && (Flags.lf_var2d()==0xffffffff))";
                case Op.FLAG_TYPE_SHLB:
                    return "(Flags.lf_var2b()<=8 && ((Flags.lf_var1b() >> (8-Flags.lf_var2b())) & 1)!=0)";
                case Op.FLAG_TYPE_SHLW:
                    return "(Flags.lf_var2b()<=16 && ((Flags.lf_var1w()) >> (16-Flags.lf_var2b()) & 1)!=0)";
                case Op.FLAG_TYPE_SHLD:
                    return "((Flags.lf_var1d() >>> (32 - Flags.lf_var2b())) & 1) != 0";
                case Op.FLAG_TYPE_SHRB:
                    return "((Flags.lf_var1b() >> (Flags.lf_var2b() - 1)) & 1) !=0";
                case Op.FLAG_TYPE_SHRW:
                    return "((Flags.lf_var1w() >> (Flags.lf_var2b() - 1)) & 1) !=0";
                case Op.FLAG_TYPE_SHRD:
                    return "((Flags.lf_var1d() >>> (Flags.lf_var2b() - 1)) & 1) != 0";
                case Op.FLAG_TYPE_SARB:
                    return "(((Flags.lf_var1b()) >> (Flags.lf_var2b() - 1)) & 1) != 0";
                case Op.FLAG_TYPE_SARW:
                    return "(((Flags.lf_var1w()) >> (Flags.lf_var2b() - 1)) & 1) != 0";
                case Op.FLAG_TYPE_SARD:
                    return "(((Flags.lf_var1d()) >> (Flags.lf_var2b() - 1)) & 1) != 0";
                case Op.FLAG_TYPE_DSHLW:
                    return "((Flags.lf_var1d() >>> (32 - Flags.lf_var2b())) & 1) != 0";
                case Op.FLAG_TYPE_DSHLD:
                    return "((Flags.lf_var1d() >>> (32 - Flags.lf_var2b())) & 1) != 0";
                case Op.FLAG_TYPE_DSHRW:
                    return "((Flags.lf_var1d() >>> (Flags.lf_var2b() - 1)) & 1) != 0";
                case Op.FLAG_TYPE_DSHRD:
                    return "((Flags.lf_var1d() >>> (Flags.lf_var2b() - 1)) & 1) != 0";
                case Op.FLAG_TYPE_NEGB:
                    return "Flags.lf_var1b() != 0";
                case Op.FLAG_TYPE_NEGW:
                    return "Flags.lf_var1w() != 0";
                case Op.FLAG_TYPE_NEGD:
                    return "Flags.lf_var1d() != 0";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.CF)!=0";
            }
        }
        return "Flags.TFLG_B()";
    }
    static String getNB() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB:
                case Op.FLAG_TYPE_INCW:
                case Op.FLAG_TYPE_INCD:
                case Op.FLAG_TYPE_DECB:
                case Op.FLAG_TYPE_DECW:
                case Op.FLAG_TYPE_DECD:
                    return "CPU_Regs.GETFLAG(CPU_Regs.CF)==0";
                case Op.FLAG_TYPE_ADDB: return "Flags.lf_resb()>=Flags.lf_var1b()";
                case Op.FLAG_TYPE_ADDW: return "Flags.lf_resw()>=Flags.lf_var1w()";
                case Op.FLAG_TYPE_ADDD: return "(Flags.lf_resd() & 0xFFFFFFFFl)>=(Flags.lf_var1d() & 0xFFFFFFFFl)";
                case Op.FLAG_TYPE_ADCB: return "(Flags.lf_resb()>=Flags.lf_var1b() && (!Flags.oldcf || (Flags.lf_resb() != Flags.lf_var1b())))";
                case Op.FLAG_TYPE_ADCW: return "(Flags.lf_resw()>=Flags.lf_var1w() && (!Flags.oldcf || (Flags.lf_resw() != Flags.lf_var1w())))";
                case Op.FLAG_TYPE_ADCD: return "((Flags.lf_resd() & 0xFFFFFFFFl)>=(Flags.lf_var1d() & 0xFFFFFFFFl) && (!Flags.oldcf || (Flags.lf_resd() != Flags.lf_var1d())))";
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_XORD:
                    return "true";
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "Flags.lf_var1b()>=Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "Flags.lf_var1w()>=Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "((Flags.lf_var1d() & 0xFFFFFFFFl)>=(Flags.lf_var2d() & 0xFFFFFFFFl))";
                case Op.FLAG_TYPE_SBBB:
                    return "((Flags.lf_var1b() >= Flags.lf_resb()) && (!Flags.oldcf || (Flags.lf_var2b()!=0xff)))";
                case Op.FLAG_TYPE_SBBW:
                    return "((Flags.lf_var1w() >= Flags.lf_resw()) && (!Flags.oldcf || (Flags.lf_var2w()!=0xffff)))";
                case Op.FLAG_TYPE_SBBD:
                    return "(((Flags.lf_var1d() & 0xFFFFFFFFl) >= (Flags.lf_resd() & 0xFFFFFFFFl)) && (!Flags.oldcf || (Flags.lf_var2d()!=0xffffffff)))";
                case Op.FLAG_TYPE_SHLB:
                    return "(Flags.lf_var2b()>8 || ((Flags.lf_var1b() >> (8-Flags.lf_var2b())) & 1)==0)";
                case Op.FLAG_TYPE_SHLW:
                    return "(Flags.lf_var2b()>16 || ((Flags.lf_var1w()) >> (16-Flags.lf_var2b()) & 1)==0)";
                case Op.FLAG_TYPE_SHLD:
                    return "((Flags.lf_var1d() >>> (32 - Flags.lf_var2b())) & 1) == 0";
                case Op.FLAG_TYPE_SHRB:
                    return "((Flags.lf_var1b() >> (Flags.lf_var2b() - 1)) & 1) ==0";
                case Op.FLAG_TYPE_SHRW:
                    return "((Flags.lf_var1w() >> (Flags.lf_var2b() - 1)) & 1) ==0";
                case Op.FLAG_TYPE_SHRD:
                    return "((Flags.lf_var1d() >>> (Flags.lf_var2b() - 1)) & 1) == 0";
                case Op.FLAG_TYPE_SARB:
                    return "(((Flags.lf_var1b()) >> (Flags.lf_var2b() - 1)) & 1) == 0";
                case Op.FLAG_TYPE_SARW:
                    return "(((Flags.lf_var1w()) >> (Flags.lf_var2b() - 1)) & 1) == 0";
                case Op.FLAG_TYPE_SARD:
                    return "(((Flags.lf_var1d()) >> (Flags.lf_var2b() - 1)) & 1) == 0";
                case Op.FLAG_TYPE_DSHLW:
                    return "((Flags.lf_var1d() >>> (32 - Flags.lf_var2b())) & 1) == 0";
                case Op.FLAG_TYPE_DSHLD:
                    return "((Flags.lf_var1d() >>> (32 - Flags.lf_var2b())) & 1) == 0";
                case Op.FLAG_TYPE_DSHRW:
                    return "((Flags.lf_var1d() >>> (Flags.lf_var2b() - 1)) & 1) == 0";
                case Op.FLAG_TYPE_DSHRD:
                    return "((Flags.lf_var1d() >>> (Flags.lf_var2b() - 1)) & 1) == 0";
                case Op.FLAG_TYPE_NEGB:
                    return "Flags.lf_var1b() == 0";
                case Op.FLAG_TYPE_NEGW:
                    return "Flags.lf_var1w() == 0";
                case Op.FLAG_TYPE_NEGD:
                    return "Flags.lf_var1d() == 0";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.CF)==0";
            }
        }
        return "Flags.TFLG_NB()";
    }
    static String getZ() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB:
                case Op.FLAG_TYPE_DECB:
                case Op.FLAG_TYPE_ADDB:
                case Op.FLAG_TYPE_ADCB:
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                case Op.FLAG_TYPE_SBBB:
                case Op.FLAG_TYPE_SHLB:
                case Op.FLAG_TYPE_SHRB:
                case Op.FLAG_TYPE_SARB:
                case Op.FLAG_TYPE_NEGB:
                    return "(Flags.lf_resb()==0)";
                case Op.FLAG_TYPE_INCW:
                case Op.FLAG_TYPE_DECW:
                case Op.FLAG_TYPE_ADDW:
                case Op.FLAG_TYPE_ADCW:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                case Op.FLAG_TYPE_SBBW:
                case Op.FLAG_TYPE_SHLW:
                case Op.FLAG_TYPE_SHRW:
                case Op.FLAG_TYPE_SARW:
                case Op.FLAG_TYPE_DSHLW:
                case Op.FLAG_TYPE_DSHRW:
                case Op.FLAG_TYPE_NEGW:
                    return "(Flags.lf_resw()==0)";
                case Op.FLAG_TYPE_INCD:
                case Op.FLAG_TYPE_DECD:
                case Op.FLAG_TYPE_ADDD:
                case Op.FLAG_TYPE_ADCD:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORD:
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                case Op.FLAG_TYPE_SBBD:
                case Op.FLAG_TYPE_SHLD:
                case Op.FLAG_TYPE_SHRD:
                case Op.FLAG_TYPE_SARD:
                case Op.FLAG_TYPE_DSHLD:
                case Op.FLAG_TYPE_DSHRD:
                case Op.FLAG_TYPE_NEGD:
                    return "(Flags.lf_resd()==0)";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.ZF) != 0";
            }
        }
        return "Flags.TFLG_Z()";
    }
    static String getNZ() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB:
                case Op.FLAG_TYPE_DECB:
                case Op.FLAG_TYPE_ADDB:
                case Op.FLAG_TYPE_ADCB:
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                case Op.FLAG_TYPE_SBBB:
                case Op.FLAG_TYPE_SHLB:
                case Op.FLAG_TYPE_SHRB:
                case Op.FLAG_TYPE_SARB:
                case Op.FLAG_TYPE_NEGB:
                    return "(Flags.lf_resb()!=0)";
                case Op.FLAG_TYPE_INCW:
                case Op.FLAG_TYPE_DECW:
                case Op.FLAG_TYPE_ADDW:
                case Op.FLAG_TYPE_ADCW:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                case Op.FLAG_TYPE_SBBW:
                case Op.FLAG_TYPE_SHLW:
                case Op.FLAG_TYPE_SHRW:
                case Op.FLAG_TYPE_SARW:
                case Op.FLAG_TYPE_DSHLW:
                case Op.FLAG_TYPE_DSHRW:
                case Op.FLAG_TYPE_NEGW:
                    return "(Flags.lf_resw()!=0)";
                case Op.FLAG_TYPE_INCD:
                case Op.FLAG_TYPE_DECD:
                case Op.FLAG_TYPE_ADDD:
                case Op.FLAG_TYPE_ADCD:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORD:
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                case Op.FLAG_TYPE_SBBD:
                case Op.FLAG_TYPE_SHLD:
                case Op.FLAG_TYPE_SHRD:
                case Op.FLAG_TYPE_SARD:
                case Op.FLAG_TYPE_DSHLD:
                case Op.FLAG_TYPE_DSHRD:
                case Op.FLAG_TYPE_NEGD:
                    return "(Flags.lf_resd()!=0)";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.ZF) == 0";
            }
        }
        return "Flags.TFLG_NZ()";
    }

    static String getBE() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "Flags.lf_var1b()<=Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "Flags.lf_var1w()<=Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "(Flags.lf_var1d() & 0xFFFFFFFFl)<=(Flags.lf_var2d() & 0xFFFFFFFFl)";
            }
            return "(("+getB()+") || ("+getZ()+"))";
        }
        return "Flags.TFLG_BE()";
    }
    static String getNBE() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "Flags.lf_var1b()>Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "Flags.lf_var1w()>Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "(Flags.lf_var1d() & 0xFFFFFFFFl)>(Flags.lf_var2d() & 0xFFFFFFFFl)";
            }
            return "(("+getNB()+") && ("+getNZ()+"))";
        }
        return "Flags.TFLG_NBE()";
    }
    static String getS() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB:
                case Op.FLAG_TYPE_DECB:
                case Op.FLAG_TYPE_ADDB:
                case Op.FLAG_TYPE_ADCB:
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                case Op.FLAG_TYPE_SBBB:
                case Op.FLAG_TYPE_SHLB:
                case Op.FLAG_TYPE_SHRB:
                case Op.FLAG_TYPE_SARB:
                case Op.FLAG_TYPE_NEGB:
                    return "(Flags.lf_resb()&0x80)!=0";
                case Op.FLAG_TYPE_INCW:
                case Op.FLAG_TYPE_DECW:
                case Op.FLAG_TYPE_ADDW:
                case Op.FLAG_TYPE_ADCW:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                case Op.FLAG_TYPE_SBBW:
                case Op.FLAG_TYPE_SHLW:
                case Op.FLAG_TYPE_SHRW:
                case Op.FLAG_TYPE_SARW:
                case Op.FLAG_TYPE_DSHLW:
                case Op.FLAG_TYPE_DSHRW:
                case Op.FLAG_TYPE_NEGW:
                    return "(Flags.lf_resw()&0x8000)!=0";
                case Op.FLAG_TYPE_INCD:
                case Op.FLAG_TYPE_DECD:
                case Op.FLAG_TYPE_ADDD:
                case Op.FLAG_TYPE_ADCD:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORD:
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                case Op.FLAG_TYPE_SBBD:
                case Op.FLAG_TYPE_SHLD:
                case Op.FLAG_TYPE_SHRD:
                case Op.FLAG_TYPE_SARD:
                case Op.FLAG_TYPE_DSHLD:
                case Op.FLAG_TYPE_DSHRD:
                case Op.FLAG_TYPE_NEGD:
                    return "(Flags.lf_resd()&0x80000000)!=0";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.SF) != 0";
            }
        }
        return "Flags.TFLG_S()";
    }
    static String getNS() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_INCB:
                case Op.FLAG_TYPE_DECB:
                case Op.FLAG_TYPE_ADDB:
                case Op.FLAG_TYPE_ADCB:
                case Op.FLAG_TYPE_ORB:
                case Op.FLAG_TYPE_ANDB:
                case Op.FLAG_TYPE_TESTB:
                case Op.FLAG_TYPE_XORB:
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                case Op.FLAG_TYPE_SBBB:
                case Op.FLAG_TYPE_SHLB:
                case Op.FLAG_TYPE_SHRB:
                case Op.FLAG_TYPE_SARB:
                case Op.FLAG_TYPE_NEGB:
                    return "(Flags.lf_resb()&0x80)==0";
                case Op.FLAG_TYPE_INCW:
                case Op.FLAG_TYPE_DECW:
                case Op.FLAG_TYPE_ADDW:
                case Op.FLAG_TYPE_ADCW:
                case Op.FLAG_TYPE_ORW:
                case Op.FLAG_TYPE_ANDW:
                case Op.FLAG_TYPE_TESTW:
                case Op.FLAG_TYPE_XORW:
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                case Op.FLAG_TYPE_SBBW:
                case Op.FLAG_TYPE_SHLW:
                case Op.FLAG_TYPE_SHRW:
                case Op.FLAG_TYPE_SARW:
                case Op.FLAG_TYPE_DSHLW:
                case Op.FLAG_TYPE_DSHRW:
                case Op.FLAG_TYPE_NEGW:
                    return "(Flags.lf_resw()&0x8000)==0";
                case Op.FLAG_TYPE_INCD:
                case Op.FLAG_TYPE_DECD:
                case Op.FLAG_TYPE_ADDD:
                case Op.FLAG_TYPE_ORD:
                case Op.FLAG_TYPE_ANDD:
                case Op.FLAG_TYPE_ADCD:
                case Op.FLAG_TYPE_TESTD:
                case Op.FLAG_TYPE_XORD:
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                case Op.FLAG_TYPE_SBBD:
                case Op.FLAG_TYPE_SHLD:
                case Op.FLAG_TYPE_SHRD:
                case Op.FLAG_TYPE_SARD:
                case Op.FLAG_TYPE_DSHLD:
                case Op.FLAG_TYPE_DSHRD:
                case Op.FLAG_TYPE_NEGD:
                    return "(Flags.lf_resd()&0x80000000)==0";
                default:
                    return "CPU_Regs.GETFLAG(CPU_Regs.SF) == 0";
            }
        }
        return "Flags.TFLG_NS()";
    }
    static String getP() {
        if (inlineFlags && opThatSetFlags!=null) {
            if (opThatSetFlags.getFlagType()>0)
                return "Flags.parity_lookup[Flags.lf_resb() & 0xFF] != 0";
            else
                return "CPU_Regs.GETFLAG(CPU_Regs.PF) != 0";
        }
        return "Flags.TFLG_P()";
    }
    static String getNP() {
        if (inlineFlags && opThatSetFlags!=null) {
            if (opThatSetFlags.getFlagType()>0)
                return "Flags.parity_lookup[Flags.lf_resb() & 0xFF] == 0";
            else
                return "CPU_Regs.GETFLAG(CPU_Regs.PF) == 0";
        }
        return "Flags.TFLG_NP()";
    }
    static String getL() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "(byte)Flags.lf_var1b()<(byte)Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "(short)Flags.lf_var1w()<(short)Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "Flags.lf_var1d()<Flags.lf_var2d()";
            }
            return "("+getS()+") != ("+getO()+")";
        }
        return "Flags.TFLG_L()";
    }
    static String getNL() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "(byte)Flags.lf_var1b()>=(byte)Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "(short)Flags.lf_var1w()>=(short)Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "Flags.lf_var1d()>=Flags.lf_var2d()";
            }
            return "("+getS()+") == ("+getO()+")";
        }
        return "Flags.TFLG_NL()";
    }
    static String getLE() {
        if (inlineFlags && opThatSetFlags!=null){
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "(byte)Flags.lf_var1b()<=(byte)Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "(short)Flags.lf_var1w()<=(short)Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "Flags.lf_var1d()<=Flags.lf_var2d()";
            }
            return "(("+getZ()+") || ("+getL()+"))";
        }
        return "Flags.TFLG_LE()";
    }
    static String getNLE() {
        if (inlineFlags && opThatSetFlags!=null) {
            switch (opThatSetFlags.getFlagType()) {
                case Op.FLAG_TYPE_SUBB:
                case Op.FLAG_TYPE_CMPB:
                    return "(byte)Flags.lf_var1b()>(byte)Flags.lf_var2b()";
                case Op.FLAG_TYPE_SUBW:
                case Op.FLAG_TYPE_CMPW:
                    return "(short)Flags.lf_var1w()>(short)Flags.lf_var2w()";
                case Op.FLAG_TYPE_SUBD:
                case Op.FLAG_TYPE_CMPD:
                    return "Flags.lf_var1d()>Flags.lf_var2d()";
            }
            return "(("+getNZ()+") && ("+getNL()+"))";
        }
        return "Flags.TFLG_NLE()";
    }

    static void compile(Inst1.JumpCond16_b op, String cond, StringBuilder method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.offset+op.eip_count);method.append(");");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
    }
    static void compile(Inst2.JumpCond16_w op, String cond, StringBuilder method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.offset+op.eip_count);method.append(");");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
    }
    static void compile(Inst3.JumpCond32_b op, String cond, StringBuilder method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_eip+=");method.append(op.offset+op.eip_count);method.append(";");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_eip+=");method.append(op.eip_count);method.append(";return Constants.BR_Link2;");
    }
    static void compile(Inst4.JumpCond32_d op, String cond, StringBuilder method) {
        method.append("if (");method.append(cond);method.append(") {");
        method.append("CPU_Regs.reg_eip+=");method.append(op.offset+op.eip_count);method.append(";");
        method.append("return Constants.BR_Link1;}");
        method.append("CPU_Regs.reg_eip+=");method.append(op.eip_count);method.append(";return Constants.BR_Link2;");
    }

    static boolean compile(Inst1.JumpCond16_b op, String cond, StringBuilder method, int eipCount) {
        if (eipCount + op.eip_count == -op.offset) {
            method.append("if (");method.append(cond);method.append(") {");
            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipCount);
            method.append(");continue;}");
            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
            return true;
        }
        return false;
    }

    static boolean compile(Inst2.JumpCond16_w op, String cond, StringBuilder method, int eipCount) {
        if (eipCount + op.eip_count == -op.offset) {
            method.append("if (");method.append(cond);method.append(") {");
            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()-");method.append(eipCount);
            method.append(");continue;}");
            method.append("CPU_Regs.reg_ip(CPU_Regs.reg_ip()+");method.append(op.eip_count);method.append(");return Constants.BR_Link2;");
            return true;
        }
        return false;
    }

    static boolean compile(Inst3.JumpCond32_b op, String cond, StringBuilder method, int eipCount) {
        if (eipCount + op.eip_count == -op.offset) {
            method.append("if (");method.append(cond);method.append(") {");
            method.append("CPU_Regs.reg_eip-=");method.append(eipCount);
            method.append(";continue;}");
            method.append("CPU_Regs.reg_eip+=");method.append(op.eip_count);method.append(";return Constants.BR_Link2;");
            return true;
        }
        return false;
    }

    static boolean compile(Inst4.JumpCond32_d op, String cond, StringBuilder method, int eipCount) {
        if (eipCount + op.eip_count == -op.offset) {
            method.append("if (");method.append(cond);method.append(") {");
            method.append("CPU_Regs.reg_eip-=");method.append(eipCount);
            method.append(";continue;}");
            method.append("CPU_Regs.reg_eip+=");method.append(op.eip_count);method.append(";return Constants.BR_Link2;");
            return true;
        }
        return false;
    }

    static void memory_readb(EaaBase eaa, StringBuilder method) {

    }
    static void memory_readw(EaaBase eaa, StringBuilder method) {

    }
    static void memory_readd(EaaBase eaa, StringBuilder method) {

    }
    static void memory_writeb(EaaBase eaa, StringBuilder method) {

    }
    static void memory_writew(EaaBase eaa, StringBuilder method) {

    }
    static void memory_writed(EaaBase eaa, StringBuilder method) {

    }
    static boolean eaaStarted = false;
    static boolean valStarted = false;
    static boolean val2Started = false;
    static boolean longValStarted = false;
    static boolean shortValStarted = false;
    static private final boolean testLocalVariableAccess = false;
    static void declareLongVal(StringBuilder method) {
        if (!longValStarted || testLocalVariableAccess) {
            method.append("long ");
            longValStarted = true;
        }
    }
    static void declareShortVal(StringBuilder method) {
        if (!shortValStarted || testLocalVariableAccess) {
            method.append("short ");
            shortValStarted = true;
        }
    }
    static void declareVal(StringBuilder method) {
        if (!valStarted || testLocalVariableAccess) {
            method.append("int ");
            valStarted = true;
        }
    }
    static void declareVal2(StringBuilder method) {
        if (!val2Started || testLocalVariableAccess) {
            method.append("int ");
            val2Started = true;
        }
    }
    static void memory_start(EaaBase eaa, Seg seg, StringBuilder method) {
        if (!eaaStarted || testLocalVariableAccess) {
            //method.append("int ");
            eaaStarted = true;
        }
        method.append("eaa = ");
        toStringValue(eaa, seg, method);
        method.append(";");
    }

    static void memory_start(EaaBase eaa, Seg seg, StringBuilder method, boolean zero) {
        if (!eaaStarted || testLocalVariableAccess) {
            //method.append("int ");
            eaaStarted = true;
        }
        method.append("eaa = ");
        toStringValue(eaa, seg, method, zero);
        method.append(";");
    }
    static void memory_readb(StringBuilder method) {
        method.append("Memory.mem_readb(eaa)");
    }
    static void memory_readw(StringBuilder method) {
        method.append("Memory.mem_readw(eaa)");
    }
    static void memory_readd(StringBuilder method) {
        method.append("Memory.mem_readd(eaa)");
    }
    static void memory_writeb(StringBuilder method) {
        method.append("Memory.mem_writeb(eaa, ");
    }
    static void memory_writew(StringBuilder method) {
        method.append("Memory.mem_writew(eaa, ");
    }
    static void memory_writed(StringBuilder method) {
        method.append("Memory.mem_writed(eaa, ");
    }

    static void nameSet(Reg reg, int bits, StringBuilder method) {
        if (bits == 8)
            nameSet8(reg, method);
        else if (bits == 16)
            nameSet16(reg, method);
        else if (bits == 32) {
            nameSet32(reg, method);
            method.append("=(");
        }
    }

    static void nameGet(Reg reg, int bits, StringBuilder method) {
        if (bits == 8)
            nameGet8(reg, method);
        else if (bits == 16)
            nameGet16(reg, method);
        else if (bits == 32)
           nameGet32(reg, method);
    }

    static void instructionEG(boolean fast, int bits, Reg e, Reg g, String inst, String extraInst, String instCall, StringBuilder method) {
        nameSet(e, bits, method);
        if (fast) {
            nameGet(e, bits, method);
            method.append(" ");
            method.append(inst);
            method.append(" ");
            nameGet(g, bits, method);
            method.append(extraInst);
            method.append(");");
        } else {
            method.append("Instructions.");
            method.append(instCall);
            method.append("(");
            nameGet(g, bits, method);
            method.append(", ");
            nameGet(e, bits, method);
            method.append("));");
        }
    }
    static void instructionEG(boolean fast, int bits, EaaBase e, Reg g, String inst, String extraInst, String instCall, Seg seg, StringBuilder method) {
        memory_start(e, seg, method);
        if (bits == 8)
            memory_writeb(method);
        else if (bits == 16)
            memory_writew(method);
        else if (bits == 32)
            memory_writed(method);
        if (fast) {
            if (bits == 8)
                memory_readb(method);
            else if (bits == 16)
                memory_readw(method);
            else if (bits == 32)
                memory_readd(method);
            method.append(" ");
            method.append(inst);
            method.append(" ");
            nameGet(g, bits, method);
            method.append(extraInst);
            method.append(");");
        } else {
            method.append("Instructions.");
            method.append(instCall);
            method.append("(");
            nameGet(g, bits, method);
            method.append(", ");
            if (bits == 8)
                memory_readb(method);
            else if (bits == 16)
                memory_readw(method);
            else if (bits == 32)
                memory_readd(method);
            method.append("));");
        }
    }
    static void instructionGE(boolean fast, int bits, Reg e, EaaBase g, String inst, String extraInst, String instCall, Seg seg, StringBuilder method) {
        memory_start(g, seg, method);
        nameSet(e, bits, method);
        if (fast) {
            nameGet(e, bits, method);
            method.append(" ");
            method.append(inst);
            method.append(" ");
            if (bits == 8)
                memory_readb(method);
            else if (bits == 16)
                memory_readw(method);
            else if (bits == 32)
                memory_readd(method);
            method.append(extraInst);
            method.append(");");
        } else {
            method.append("Instructions.");
            method.append(instCall);
            method.append("(");
            if (bits == 8)
                memory_readb(method);
            else if (bits == 16)
                memory_readw(method);
            else if (bits == 32)
                memory_readd(method);
            method.append(", ");
            nameGet(e, bits, method);
            method.append("));");
        }
    }
    static void instructionAI(boolean fast, int bits, int i, String inst, String extraInst, String instCall, StringBuilder method) {
        nameSet(CPU_Regs.reg_eax, bits, method);
        if (fast) {
            nameGet(CPU_Regs.reg_eax, bits, method);
            method.append(inst);
            method.append(" ");
            method.append(i);
            method.append(" ");
            method.append(extraInst);
            method.append(");");
        } else {
            method.append("Instructions.");
            method.append(instCall);
            method.append("(");
            method.append(i);
            method.append(", ");
            nameGet(CPU_Regs.reg_eax, bits, method);
            method.append("));");
        }
    }
    static void inc(boolean fast, int bits, Reg r, StringBuilder method) {
        if (fast) {
            if (bits==32) {
                method.append(nameSet32(r));
                method.append("++;");
            } else {
                nameSet(r, bits, method);
                nameGet(r, bits, method);
                method.append("+1);");
            }
        } else {
            nameSet(r, bits, method);
            method.append("Instructions.INC");
            if (bits == 8)
                method.append("B");
            else if (bits == 16)
                method.append("W");
            else if (bits == 32)
                method.append("D");
            method.append("(");
            nameGet(r, bits, method);
            method.append("));");
        }
    }
    static void dec(boolean fast, int bits, Reg r, StringBuilder method) {
        if (fast) {
            if (bits==32) {
                method.append(nameSet32(r));
                method.append("--;");
            } else {
                nameSet(r, bits, method);
                nameGet(r, bits, method);
                method.append("-1);");
            }
        } else {
            nameSet(r, bits, method);
            method.append("Instructions.DEC");
            if (bits == 8)
                method.append("B");
            else if (bits == 16)
                method.append("W");
            else if (bits == 32)
                method.append("D");
            method.append("(");
            nameGet(r, bits, method);
            method.append("));");
        }
    }

    static private boolean compile_op(Op op, int setFlags, StringBuilder method, String preException, Seg seg) {
        switch (op.c) {
            case 0x00: // ADD Eb,Gb
            case 0x200:
                if (op instanceof Inst1.Addb_reg) {
                    Inst1.Addb_reg o = (Inst1.Addb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "+", "", "ADDB", method);
                    return true;
                }
                if (op instanceof Inst1.AddEbGb_mem) {
                    Inst1.AddEbGb_mem o = (Inst1.AddEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "+", "", "ADDB", seg, method);
                    return true;
                }
                break;
            case 0x01: // ADD Ew,Gw
                if (op instanceof Inst1.Addw_reg) {
                    Inst1.Addw_reg o = (Inst1.Addw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "+", "", "ADDW", method);
                    return true;
                }
                if (op instanceof Inst1.AddEwGw_mem) {
                    Inst1.AddEwGw_mem o = (Inst1.AddEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "+", "", "ADDW", seg, method);
                    return true;
                }
                break;
            case 0x02: // ADD Gb,Eb
            case 0x202:
                if (op instanceof Inst1.Addb_reg) {
                    Inst1.Addb_reg o = (Inst1.Addb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "+", "", "ADDB", method);
                    return true;
                }
                if (op instanceof Inst1.AddGbEb_mem) {
                    Inst1.AddGbEb_mem o = (Inst1.AddGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "+", "", "ADDB", seg, method);
                    return true;
                }
                break;
            case 0x03: // ADD Gw,Ew
                if (op instanceof Inst1.Addw_reg) {
                    Inst1.Addw_reg o = (Inst1.Addw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "+", "", "ADDW", method);
                    return true;
                }
                if (op instanceof Inst1.AddGwEw_mem) {
                    Inst1.AddGwEw_mem o = (Inst1.AddGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "+", "", "ADDW", seg, method);
                    return true;
                }
                break;
            case 0x04: // ADD AL,Ib
            case 0x204:
                if (op instanceof Inst1.AddAlIb) {
                    Inst1.AddAlIb o = (Inst1.AddAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "+", "", "ADDB", method);
                    return true;
                }
                break;
            case 0x05: // ADD AX,Iw
                if (op instanceof Inst1.AddAxIw) {
                    Inst1.AddAxIw o = (Inst1.AddAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "+", "", "ADDW", method);
                    return true;
                }
                break;
            case 0x06: // PUSH ES
                if (op instanceof Inst1.PushES) {
                    Inst1.PushES o = (Inst1.PushES) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_esVal.dword);");
                    return true;
                }
                break;
            case 0x07: // POP ES
                if (op instanceof Inst1.PopES) {
                    Inst1.PopES o = (Inst1.PopES) op;
                    method.append("if (CPU.CPU_PopSegES(false)) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x08: // OR Eb,Gb
            case 0x208:
                if (op instanceof Inst1.Orb_reg) {
                    Inst1.Orb_reg o = (Inst1.Orb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "|", "", "ORB", method);
                    return true;
                }
                if (op instanceof Inst1.OrEbGb_mem) {
                    Inst1.OrEbGb_mem o = (Inst1.OrEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "|", "", "ORB", seg, method);
                    return true;
                }
                break;
            case 0x09: // OR Ew,Gw
                if (op instanceof Inst1.Orw_reg) {
                    Inst1.Orw_reg o = (Inst1.Orw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "|", "", "ORW", method);
                    return true;
                }
                if (op instanceof Inst1.OrEwGw_mem) {
                    Inst1.OrEwGw_mem o = (Inst1.OrEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "|", "", "ORW", seg, method);
                    return true;
                }
                break;
            case 0x0a: // OR Gb,Eb
            case 0x20a:
                if (op instanceof Inst1.Orb_reg) {
                    Inst1.Orb_reg o = (Inst1.Orb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "|", "", "ORB", method);
                    return true;
                }
                if (op instanceof Inst1.OrGbEb_mem) {
                    Inst1.OrGbEb_mem o = (Inst1.OrGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "|", "", "ORB", seg, method);
                    return true;
                }
                break;
            case 0x0b: // OR Gw,Ew
                if (op instanceof Inst1.Orw_reg) {
                    Inst1.Orw_reg o = (Inst1.Orw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "|", "", "ORW", method);
                    return true;
                }
                if (op instanceof Inst1.OrGwEw_mem) {
                    Inst1.OrGwEw_mem o = (Inst1.OrGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "|", "", "ORW", seg, method);
                    return true;
                }
                break;
            case 0x0c: // OR AL,Ib
            case 0x20c:
                if (op instanceof Inst1.OrAlIb) {
                    Inst1.OrAlIb o = (Inst1.OrAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "|", "", "ORB", method);
                    return true;
                }
                break;
            case 0x0d: // OR AX,Iw
                if (op instanceof Inst1.OrAxIw) {
                    Inst1.OrAxIw o = (Inst1.OrAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "|", "", "ORW", method);
                    return true;
                }
                break;
            case 0x0e: // PUSH CS
                if (op instanceof Inst1.PushCS) {
                    Inst1.PushCS o = (Inst1.PushCS) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_csVal.dword);");
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
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCB", method);
                    return true;
                }
                if (op instanceof Inst1.AdcEbGb_mem) {
                    Inst1.AdcEbGb_mem o = (Inst1.AdcEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCB", seg, method);
                    return true;
                }
                break;
            case 0x11: // ADC Ew,Gw
                if (op instanceof Inst1.Adcw_reg) {
                    Inst1.Adcw_reg o = (Inst1.Adcw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCW", method);
                    return true;
                }
                if (op instanceof Inst1.AdcEwGw_mem) {
                    Inst1.AdcEwGw_mem o = (Inst1.AdcEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCW", seg, method);
                    return true;
                }
                break;
            case 0x12: // ADC Gb,Eb
            case 0x212:
                if (op instanceof Inst1.Adcb_reg) {
                    Inst1.Adcb_reg o = (Inst1.Adcb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCB", method);
                    return true;
                }
                if (op instanceof Inst1.AdcGbEb_mem) {
                    Inst1.AdcGbEb_mem o = (Inst1.AdcGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCB", seg, method);
                    return true;
                }
                break;
            case 0x13: // ADC Gw,Ew
                if (op instanceof Inst1.Adcw_reg) {
                    Inst1.Adcw_reg o = (Inst1.Adcw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCW", method);
                    return true;
                }
                if (op instanceof Inst1.AdcGwEw_mem) {
                    Inst1.AdcGwEw_mem o = (Inst1.AdcGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "+", "+(Flags.get_CF()?1:0)", "ADCW", seg, method);
                    return true;
                }
                break;
            case 0x14: // ADC AL,Ib
            case 0x214:
                if (op instanceof Inst1.AdcAlIb) {
                    Inst1.AdcAlIb o = (Inst1.AdcAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "+", "+(Flags.get_CF()?1:0)", "ADCB", method);
                    return true;
                }
                break;
            case 0x15: // ADC AX,Iw
                if (op instanceof Inst1.AdcAxIw) {
                    Inst1.AdcAxIw o = (Inst1.AdcAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "+", "+(Flags.get_CF()?1:0)", "ADCW", method);
                    return true;
                }
                break;
            case 0x16: // PUSH SS
                if (op instanceof Inst1.PushSS) {
                    Inst1.PushSS o = (Inst1.PushSS) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_ssVal.dword);");
                    return true;
                }
                break;
            case 0x17: // POP SS
                if (op instanceof Inst1.PopSS) {
                    Inst1.PopSS o = (Inst1.PopSS) op;
                    method.append("if (CPU.CPU_PopSegSS(false)) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x18: // SBB Eb,Gb
            case 0x218:
                if (op instanceof Inst1.Sbbb_reg) {
                    Inst1.Sbbb_reg o = (Inst1.Sbbb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBB", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.SbbEbGb_mem) {
                    Inst1.SbbEbGb_mem o = (Inst1.SbbEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBB", seg, method);
                    return true;
                }
                break;
            case 0x19: // SBB Ew,Gw
                if (op instanceof Inst1.Sbbw_reg) {
                    Inst1.Sbbw_reg o = (Inst1.Sbbw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBW", method);
                    return true;
                }
                if (op instanceof Inst1.SbbEwGw_mem) {
                    Inst1.SbbEwGw_mem o = (Inst1.SbbEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBW", seg, method);
                    return true;
                }
                break;
            case 0x1a: // SBB Gb,Eb
            case 0x21a:
                if (op instanceof Inst1.Sbbb_reg) {
                    Inst1.Sbbb_reg o = (Inst1.Sbbb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBB", method);
                    return true;
                }
                if (op instanceof Inst1.SbbGbEb_mem) {
                    Inst1.SbbGbEb_mem o = (Inst1.SbbGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBB", seg, method);
                    return true;
                }
                break;
            case 0x1b: // SBB Gw,Ew
                if (op instanceof Inst1.Sbbw_reg) {
                    Inst1.Sbbw_reg o = (Inst1.Sbbw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBW", method);
                    return true;
                }
                if (op instanceof Inst1.SbbGwEw_mem) {
                    Inst1.SbbGwEw_mem o = (Inst1.SbbGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "-", "-(Flags.get_CF()?1:0)", "SBBW", seg, method);
                    return true;
                }
                break;
            case 0x1c: // SBB AL,Ib
            case 0x21c:
                if (op instanceof Inst1.SbbAlIb) {
                    Inst1.SbbAlIb o = (Inst1.SbbAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "-", "-(Flags.get_CF()?1:0)", "SBBB", method);
                    return true;
                }
                break;
            case 0x1d: // SBB AX,Iw
                if (op instanceof Inst1.SbbAxIw) {
                    Inst1.SbbAxIw o = (Inst1.SbbAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "-", "-(Flags.get_CF()?1:0)", "SBBW", method);
                    return true;
                }
                break;
            case 0x1e: // PUSH DS
                if (op instanceof Inst1.PushDS) {
                    Inst1.PushDS o = (Inst1.PushDS) op;
                    method.append("CPU.CPU_Push16(CPU_Regs.reg_dsVal.dword);");
                    return true;
                }
                break;
            case 0x1f: // POP DS
                if (op instanceof Inst1.PopDS) {
                    Inst1.PopDS o = (Inst1.PopDS) op;
                    method.append("if (CPU.CPU_PopSegDS(false)) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x20: // AND Eb,Gb
            case 0x220:
                if (op instanceof Inst1.Andb_reg) {
                    Inst1.Andb_reg o = (Inst1.Andb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "&", "", "ANDB", method);
                    return true;
                }
                if (op instanceof Inst1.AndEbGb_mem) {
                    Inst1.AndEbGb_mem o = (Inst1.AndEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "&", "", "ANDB", seg, method);
                    return true;
                }
                break;
            case 0x21: // AND Ew,Gw
                if (op instanceof Inst1.Andw_reg) {
                    Inst1.Andw_reg o = (Inst1.Andw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "&", "", "ANDW", method);
                    return true;
                }
                if (op instanceof Inst1.AndEwGw_mem) {
                    Inst1.AndEwGw_mem o = (Inst1.AndEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "&", "", "ANDW", seg, method);
                    return true;
                }
                break;
            case 0x22: // AND Gb,Eb
            case 0x222:
                if (op instanceof Inst1.Andb_reg) {
                    Inst1.Andb_reg o = (Inst1.Andb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "&", "", "ANDB", method);
                    return true;
                }
                if (op instanceof Inst1.AndGbEb_mem) {
                    Inst1.AndGbEb_mem o = (Inst1.AndGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "&", "", "ANDB", seg, method);
                    return true;
                }
                break;
            case 0x23: // AND Gw,Ew
                if (op instanceof Inst1.Andw_reg) {
                    Inst1.Andw_reg o = (Inst1.Andw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "&", "", "ANDW", method);
                    return true;
                }
                if (op instanceof Inst1.AndGwEw_mem) {
                    Inst1.AndGwEw_mem o = (Inst1.AndGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "&", "", "ANDW", seg, method);
                    return true;
                }
                break;
            case 0x24: // AND AL,Ib
            case 0x224:
                if (op instanceof Inst1.AndAlIb) {
                    Inst1.AndAlIb o = (Inst1.AndAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "&", "", "ANDB", method);
                    return true;
                }
                break;
            case 0x25: // AND AX,Iw
                if (op instanceof Inst1.AndAxIw) {
                    Inst1.AndAxIw o = (Inst1.AndAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "&", "", "ANDW", method);
                    return true;
                }
                break;
            case 0x26: // SEG ES:
            case 0x226:
                if (op instanceof Inst1.SegES) {
                    seg.setES();
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
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "-", "", "SUBB", method);
                    return true;
                }
                if (op instanceof Inst1.SubEbGb_mem) {
                    Inst1.SubEbGb_mem o = (Inst1.SubEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "-", "", "SUBB", seg, method);
                    return true;
                }
                break;
            case 0x29: // SUB Ew,Gw
                if (op instanceof Inst1.Subw_reg) {
                    Inst1.Subw_reg o = (Inst1.Subw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "-", "", "SUBW", method);
                    return true;
                }
                if (op instanceof Inst1.SubEwGw_mem) {
                    Inst1.SubEwGw_mem o = (Inst1.SubEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "-", "", "SUBW", seg, method);
                    return true;
                }
                break;
            case 0x2a: // SUB Gb,Eb
            case 0x22a:
                if (op instanceof Inst1.Subb_reg) {
                    Inst1.Subb_reg o = (Inst1.Subb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "-", "", "SUBB", method);
                    return true;
                }
                if (op instanceof Inst1.SubGbEb_mem) {
                    Inst1.SubGbEb_mem o = (Inst1.SubGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "-", "", "SUBB", seg, method);
                    return true;
                }
                break;
            case 0x2b: // SUB Gw,Ew
                if (op instanceof Inst1.Subw_reg) {
                    Inst1.Subw_reg o = (Inst1.Subw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "-", "", "SUBW", method);
                    return true;
                }
                if (op instanceof Inst1.SubGwEw_mem) {
                    Inst1.SubGwEw_mem o = (Inst1.SubGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "-", "", "SUBW", seg, method);
                    return true;
                }
                break;
            case 0x2c: // SUB AL,Ib
            case 0x22c:
                if (op instanceof Inst1.SubAlIb) {
                    Inst1.SubAlIb o = (Inst1.SubAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "-", "", "SUBB", method);
                    return true;
                }
                break;
            case 0x2d: // SUB AX,Iw
                if (op instanceof Inst1.SubAxIw) {
                    Inst1.SubAxIw o = (Inst1.SubAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "-", "", "SUBW", method);
                    return true;
                }
                break;
            case 0x2e: // SEG CS:
            case 0x22e:
                if (op instanceof Inst1.SegCS) {
                    seg.setCS();
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
                    boolean fast = (setFlags & o.sets())==0;
                    if (fast && o.e == o.g) {
                        nameSet8(o.e, method);
                        method.append("0);");
                    } else {
                        instructionEG(fast, 8, o.e, o.g, "^", "", "XORB", method);
                    }
                    return true;
                }
                if (op instanceof Inst1.XorEbGb_mem) {
                    Inst1.XorEbGb_mem o = (Inst1.XorEbGb_mem) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "^", "", "XORB", seg, method);
                    return true;
                }
                break;
            case 0x31: // XOR Ew,Gw
                if (op instanceof Inst1.Xorw_reg) {
                    Inst1.Xorw_reg o = (Inst1.Xorw_reg) op;
                    boolean fast = (setFlags & o.sets())==0;
                    if (fast && o.e == o.g) {
                        nameSet16(o.e, method);
                        method.append("0);");
                    } else {
                        instructionEG(fast, 16, o.e, o.g, "^", "", "XORW", method);
                    }
                    return true;
                }
                if (op instanceof Inst1.XorEwGw_mem) {
                    Inst1.XorEwGw_mem o = (Inst1.XorEwGw_mem) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "^", "", "XORW", seg, method);
                    return true;
                }
                break;
            case 0x32: // XOR Gb,Eb
            case 0x232:
                if (op instanceof Inst1.Xorb_reg) {
                    Inst1.Xorb_reg o = (Inst1.Xorb_reg) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.e, o.g, "^", "", "XORB", method);
                    return true;
                }
                if (op instanceof Inst1.XorGbEb_mem) {
                    Inst1.XorGbEb_mem o = (Inst1.XorGbEb_mem) op;
                    instructionGE((setFlags & o.sets())==0, 8, o.e, o.g, "^", "", "XORB", seg, method);
                    return true;
                }
                break;
            case 0x33: // XOR Gw,Ew
                if (op instanceof Inst1.Xorw_reg) {
                    Inst1.Xorw_reg o = (Inst1.Xorw_reg) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.e, o.g, "^", "", "XORW", method);
                    return true;
                }
                if (op instanceof Inst1.XorGwEw_mem) {
                    Inst1.XorGwEw_mem o = (Inst1.XorGwEw_mem) op;
                    instructionGE((setFlags & o.sets())==0, 16, o.e, o.g, "^", "", "XORW", seg, method);
                    return true;
                }
                break;
            case 0x34: // XOR AL,Ib
            case 0x234:
                if (op instanceof Inst1.XorAlIb) {
                    Inst1.XorAlIb o = (Inst1.XorAlIb) op;
                    instructionAI((setFlags & o.sets())==0, 8, o.i, "^", "", "XORB", method);
                    return true;
                }
                break;
            case 0x35: // XOR AX,Iw
                if (op instanceof Inst1.XorAxIw) {
                    Inst1.XorAxIw o = (Inst1.XorAxIw) op;
                    instructionAI((setFlags & o.sets())==0, 16, o.i, "^", "", "XORW", method);
                    return true;
                }
                break;
            case 0x36: // SEG SS:
                case 0x236:
                    if (op instanceof Inst1.SegSS) {
                        seg.setSS();
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
                    memory_start(o.e, seg, method);
                    method.append("Instructions.CMPB(");
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
                    memory_start(o.e, seg, method);
                    method.append("Instructions.CMPW(");
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
                    memory_start(o.g, seg, method);
                    method.append("Instructions.CMPB(Memory.mem_readb(eaa), ");
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
                    memory_start(o.g, seg, method);
                    method.append("Instructions.CMPW(Memory.mem_readw(eaa), ");
                    method.append(nameGet16(o.e));
                    method.append(");");
                    return true;
                }
                break;
            case 0x3c: // CMP AL,Ib
            case 0x23c:
                if (op instanceof Inst1.CmpAlIb) {
                    Inst1.CmpAlIb o = (Inst1.CmpAlIb) op;
                    method.append("Instructions.CMPB(");
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
                    seg.setDS();
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
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x41: // INC CX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x42: // INC DX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x43: // INC BX
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x44: // INC SP
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x45: // INC BP
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x46: // INC SI
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x47: // INC DI
                if (op instanceof Inst1.Incw) {
                    Inst1.Incw o = (Inst1.Incw) op;
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x48: // DEC AX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x49: // DEC CX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x4a: // DEC DX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x4b: // DEC BX
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x4c: // DEC SP
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x4d: // DEC BP
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x4e: // DEC SI
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                break;
            case 0x4f: // DEC DI
                if (op instanceof Inst1.Decw) {
                    Inst1.Decw o = (Inst1.Decw) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
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
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x59: // POP CX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x5a: // POP DX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x5b: // POP BX
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x5c: // POP SP
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x5d: // POP BP
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x5e: // POP SI
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x5f: // POP DI
                if (op instanceof Inst1.Popw) {
                    Inst1.Popw o = (Inst1.Popw) op;
                    nameSet16(o.reg, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x60: // PUSHA
                if (op instanceof Inst1.Pusha) {
                    Inst1.Pusha o = (Inst1.Pusha) op;
                    declareVal2(method);
                    method.append("val2=CPU_Regs.reg_esp.word();");
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_esp.dword;val = CPU.CPU_Push16(val, CPU_Regs.reg_eax.word());val = CPU.CPU_Push16(val, CPU_Regs.reg_ecx.word());val = CPU.CPU_Push16(val, CPU_Regs.reg_edx.word());val = CPU.CPU_Push16(val, CPU_Regs.reg_ebx.word());val = CPU.CPU_Push16(val, val2);val = CPU.CPU_Push16(val, ");nameGet16(CPU_Regs.reg_ebp,method);method.append(");val = CPU.CPU_Push16(val, CPU_Regs.reg_esi.word());val = CPU.CPU_Push16(val, CPU_Regs.reg_edi.word());CPU_Regs.reg_esp.word(val);");
                    return true;
                }
                break;
            case 0x61: // POPA
                if (op instanceof Inst1.Popa) {
                    //Inst1.Popa o = (Inst1.Popa) op;
                    method.append("CPU_Regs.reg_edi.word(CPU.CPU_Pop16());CPU_Regs.reg_esi.word(CPU.CPU_Pop16());CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());CPU.CPU_Pop16();CPU_Regs.reg_ebx.word(CPU.CPU_Pop16());CPU_Regs.reg_edx.word(CPU.CPU_Pop16());CPU_Regs.reg_ecx.word(CPU.CPU_Pop16());CPU_Regs.reg_eax.word(CPU.CPU_Pop16());");
                    return true;
                }
                break;
            case 0x62: // BOUND
                if (op instanceof Inst1.Bound) {
                    Inst1.Bound o = (Inst1.Bound) op;
                    declareShortVal(method);
                    method.append("sval = (short)");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    memory_start(o.get_eaa, seg, method);
                    method.append(";{short bound_min, bound_max;");
                    method.append("bound_min=(short)Memory.mem_readw(eaa); bound_max=(short)Memory.mem_readw(eaa+2);if ( (sval < bound_min) || (sval > bound_max) ) {").append(preException).append("return EXCEPTION(5);}}");
                    return true;
                }
                break;
            case 0x63: // ARPL Ew,Rw
                if (op instanceof Inst1.ArplEwRw_reg) {
                    Inst1.ArplEwRw_reg o = (Inst1.ArplEwRw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    declareVal(method);
                    method.append("val=");
                    method.append(nameGet16(o.earw));
                    method.append(";val=CPU.CPU_ARPL(val,");
                    method.append(nameGet16(o.rw));
                    method.append(");");                    
                    nameSet16(o.earw, "val", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.ArplEwRw_mem) {
                    Inst1.ArplEwRw_mem o = (Inst1.ArplEwRw_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);val=CPU.CPU_ARPL(val,");
                    method.append(nameGet16(o.rw));
                    method.append(");Memory.mem_writew(eaa,val);");
                    return true;
                }
                break;
            case 0x64: // SEG FS:
            case 0x264:
                if (op instanceof Inst1.SegFS) {
                    seg.setFS();
                    return true;
                }
                break;
            case 0x65: // SEG GS:
            case 0x265:
                if (op instanceof Inst1.SegGS) {
                    seg.setGS();
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
                    nameSet16(o.rw, "Instructions.DIMULW(", nameGet16(o.earw), ", ", String.valueOf(o.op3), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.IMULGwEwIw_mem) {
                    Inst1.IMULGwEwIw_mem o = (Inst1.IMULGwEwIw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "Instructions.DIMULW(Memory.mem_readw(eaa),", String.valueOf(o.op3), ")", method);
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
                    nameSet16(o.rw, "Instructions.DIMULW(", nameGet16(o.earw), ", ", String.valueOf(o.op3), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.IMULGwEwIb_mem) {
                    Inst1.IMULGwEwIb_mem o = (Inst1.IMULGwEwIb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "Instructions.DIMULW(Memory.mem_readw(eaa),", String.valueOf(o.op3), ")", method);
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}Core.rep_zero = ");
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
            case 0x6d: // INSW
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}Core.rep_zero = ");
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
            case 0x6e: // OUTSB
            case 0x26e:
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}Core.rep_zero = ");
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
            case 0x6f: // OUTSW
                if (op instanceof Inst1.DoStringException) {
                    Inst1.DoStringException o = (Inst1.DoStringException) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),");
                    method.append(o.width);
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}Core.rep_zero = ");
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
            case 0x70: // JO
                if (op instanceof Inst1.JumpCond16_b_o) {
                    Inst1.JumpCond16_b_o o = (Inst1.JumpCond16_b_o) op;
                    compile(o, getO(), method);
                    return false;
                }
                break;
            case 0x71: // JNO
                if (op instanceof Inst1.JumpCond16_b_no) {
                    Inst1.JumpCond16_b_no o = (Inst1.JumpCond16_b_no) op;
                    compile(o, getNO(), method);
                    return false;
                }
                break;
            case 0x72: // JB
                if (op instanceof Inst1.JumpCond16_b_b) {
                    Inst1.JumpCond16_b_b o = (Inst1.JumpCond16_b_b) op;
                    compile(o, getB(), method);
                    return false;
                }
                break;
            case 0x73: // JNB
                if (op instanceof Inst1.JumpCond16_b_nb) {
                    Inst1.JumpCond16_b_nb o = (Inst1.JumpCond16_b_nb) op;
                    compile(o, getNB(), method);
                    return false;
                }
                break;
            case 0x74: // JZ
                if (op instanceof Inst1.JumpCond16_b_z) {
                    Inst1.JumpCond16_b_z o = (Inst1.JumpCond16_b_z) op;
                    compile(o, getZ(), method);
                    return false;
                }
                break;
            case 0x75: // JNZ
                if (op instanceof Inst1.JumpCond16_b_nz) {
                    Inst1.JumpCond16_b_nz o = (Inst1.JumpCond16_b_nz) op;
                    compile(o, getNZ(), method);
                    return false;
                }
                break;
            case 0x76: // JBE
                if (op instanceof Inst1.JumpCond16_b_be) {
                    Inst1.JumpCond16_b_be o = (Inst1.JumpCond16_b_be) op;
                    compile(o, getBE(), method);
                    return false;
                }
                break;
            case 0x77: // JNBE
                if (op instanceof Inst1.JumpCond16_b_nbe) {
                    Inst1.JumpCond16_b_nbe o = (Inst1.JumpCond16_b_nbe) op;
                    compile(o, getNBE(), method);
                    return false;
                }
                break;
            case 0x78: // JS
                if (op instanceof Inst1.JumpCond16_b_s) {
                    Inst1.JumpCond16_b_s o = (Inst1.JumpCond16_b_s) op;
                    compile(o, getS(), method);
                    return false;
                }
                break;
            case 0x79: // JNS
                if (op instanceof Inst1.JumpCond16_b_ns) {
                    Inst1.JumpCond16_b_ns o = (Inst1.JumpCond16_b_ns) op;
                    compile(o, getNS(), method);
                    return false;
                }
                break;
            case 0x7a: // JP
                if (op instanceof Inst1.JumpCond16_b_p) {
                    Inst1.JumpCond16_b_p o = (Inst1.JumpCond16_b_p) op;
                    compile(o, getP(), method);
                    return false;
                }
                break;
            case 0x7b: // JNP
                if (op instanceof Inst1.JumpCond16_b_np) {
                    Inst1.JumpCond16_b_np o = (Inst1.JumpCond16_b_np) op;
                    compile(o, getNP(), method);
                    return false;
                }
                break;
            case 0x7c: // JL
                if (op instanceof Inst1.JumpCond16_b_l) {
                    Inst1.JumpCond16_b_l o = (Inst1.JumpCond16_b_l) op;
                    compile(o, getL(), method);
                    return false;
                }
                break;
            case 0x7d: // JNL
                if (op instanceof Inst1.JumpCond16_b_nl) {
                    Inst1.JumpCond16_b_nl o = (Inst1.JumpCond16_b_nl) op;
                    compile(o, getNL(), method);
                    return false;
                }
                break;
            case 0x7e: // JLE
                if (op instanceof Inst1.JumpCond16_b_le) {
                    Inst1.JumpCond16_b_le o = (Inst1.JumpCond16_b_le) op;
                    compile(o, getLE(), method);
                    return false;
                }
                break;
            case 0x7f: // JNLE
                if (op instanceof Inst1.JumpCond16_b_nle) {
                    Inst1.JumpCond16_b_nle o = (Inst1.JumpCond16_b_nle) op;
                    compile(o, getNLE(), method);
                    return false;
                }
                break;
            case 0x80: // Grpl Eb,Ib
            case 0x280:
                if (op instanceof Inst1.GrplEbIb_reg_add) {
                    Inst1.GrplEbIb_reg_add o = (Inst1.GrplEbIb_reg_add) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "+", "", "ADDB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_or) {
                    Inst1.GrplEbIb_reg_or o = (Inst1.GrplEbIb_reg_or) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "|", "", "ORB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_adc) {
                    Inst1.GrplEbIb_reg_adc o = (Inst1.GrplEbIb_reg_adc) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "+", "+(Flags.get_CF()?1:0)", "ADCB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_sbb) {
                    Inst1.GrplEbIb_reg_sbb o = (Inst1.GrplEbIb_reg_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "-", "-(Flags.get_CF()?1:0)", "SBBB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_and) {
                    Inst1.GrplEbIb_reg_and o = (Inst1.GrplEbIb_reg_and) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "&", "", "ANDB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_sub) {
                    Inst1.GrplEbIb_reg_sub o = (Inst1.GrplEbIb_reg_sub) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "-", "", "SUBB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_xor) {
                    Inst1.GrplEbIb_reg_xor o = (Inst1.GrplEbIb_reg_xor) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.earb, new Reg(o.ib), "^", "", "XORB", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_reg_cmp) {
                    Inst1.GrplEbIb_reg_cmp o = (Inst1.GrplEbIb_reg_cmp) op;
                    method.append("Instructions.CMPB(");
                    method.append(o.ib);
                    method.append(",");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_add) {
                    Inst1.GrplEbIb_mem_add o = (Inst1.GrplEbIb_mem_add) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "+", "", "ADDB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_or) {
                    Inst1.GrplEbIb_mem_or o = (Inst1.GrplEbIb_mem_or) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "|", "", "ORB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_adc) {
                    Inst1.GrplEbIb_mem_adc o = (Inst1.GrplEbIb_mem_adc) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "+", "+(Flags.get_CF()?1:0)", "ADCB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_sbb) {
                    Inst1.GrplEbIb_mem_sbb o = (Inst1.GrplEbIb_mem_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "-", "-(Flags.get_CF()?1:0)", "SBBB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_and) {
                    Inst1.GrplEbIb_mem_and o = (Inst1.GrplEbIb_mem_and) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "&", "", "ANDB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_sub) {
                    Inst1.GrplEbIb_mem_sub o = (Inst1.GrplEbIb_mem_sub) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "-", "", "SUBB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_xor) {
                    Inst1.GrplEbIb_mem_xor o = (Inst1.GrplEbIb_mem_xor) op;
                    instructionEG((setFlags & o.sets())==0, 8, o.get_eaa, new Reg(o.ib), "^", "", "XORB", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEbIb_mem_cmp) {
                    Inst1.GrplEbIb_mem_cmp o = (Inst1.GrplEbIb_mem_cmp) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.CMPB(");
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
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "+", "", "ADDW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_or) {
                    Inst1.GrplEwIw_reg_or o = (Inst1.GrplEwIw_reg_or) op;
                    instructionEG((setFlags & o.sets())==16, 16, o.earw, new Reg(o.ib), "|", "", "ORW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_adc) {
                    Inst1.GrplEwIw_reg_adc o = (Inst1.GrplEwIw_reg_adc) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "+", "+(Flags.get_CF()?1:0)", "ADCW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sbb) {
                    Inst1.GrplEwIw_reg_sbb o = (Inst1.GrplEwIw_reg_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "-", "-(Flags.get_CF()?1:0)", "SBBW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_and) {
                    Inst1.GrplEwIw_reg_and o = (Inst1.GrplEwIw_reg_and) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "&", "", "ANDW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sub) {
                    Inst1.GrplEwIw_reg_sub o = (Inst1.GrplEwIw_reg_sub) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "-", "", "SUBW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_xor) {
                    Inst1.GrplEwIw_reg_xor o = (Inst1.GrplEwIw_reg_xor) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "^", "", "XORW", method);
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
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "+", "", "ADDW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_or) {
                    Inst1.GrplEwIw_mem_or o = (Inst1.GrplEwIw_mem_or) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "|", "", "ORW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_adc) {
                    Inst1.GrplEwIw_mem_adc o = (Inst1.GrplEwIw_mem_adc) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "+", "+(Flags.get_CF()?1:0)", "ADCW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sbb) {
                    Inst1.GrplEwIw_mem_sbb o = (Inst1.GrplEwIw_mem_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "-", "-(Flags.get_CF()?1:0)", "SBBW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_and) {
                    Inst1.GrplEwIw_mem_and o = (Inst1.GrplEwIw_mem_and) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "&", "", "ANDW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sub) {
                    Inst1.GrplEwIw_mem_sub o = (Inst1.GrplEwIw_mem_sub) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "-", "", "SUBW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_xor) {
                    Inst1.GrplEwIw_mem_xor o = (Inst1.GrplEwIw_mem_xor) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "^", "", "XORW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_cmp) {
                    Inst1.GrplEwIw_mem_cmp o = (Inst1.GrplEwIw_mem_cmp) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.CMPW(");
                    method.append(o.ib);
                    method.append(",Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x83: // Grpl Ew,Ix
                if (op instanceof Inst1.GrplEwIw_reg_add) {
                    Inst1.GrplEwIw_reg_add o = (Inst1.GrplEwIw_reg_add) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "+", "", "ADDW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_or) {
                    Inst1.GrplEwIw_reg_or o = (Inst1.GrplEwIw_reg_or) op;
                    instructionEG((setFlags & o.sets())==16, 16, o.earw, new Reg(o.ib), "|", "", "ORW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_adc) {
                    Inst1.GrplEwIw_reg_adc o = (Inst1.GrplEwIw_reg_adc) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "+", "+(Flags.get_CF()?1:0)", "ADCW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sbb) {
                    Inst1.GrplEwIw_reg_sbb o = (Inst1.GrplEwIw_reg_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "-", "-(Flags.get_CF()?1:0)", "SBBW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_and) {
                    Inst1.GrplEwIw_reg_and o = (Inst1.GrplEwIw_reg_and) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "&", "", "ANDW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_sub) {
                    Inst1.GrplEwIw_reg_sub o = (Inst1.GrplEwIw_reg_sub) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "-", "", "SUBW", method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_reg_xor) {
                    Inst1.GrplEwIw_reg_xor o = (Inst1.GrplEwIw_reg_xor) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.earw, new Reg(o.ib), "^", "", "XORW", method);
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
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "+", "", "ADDW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_or) {
                    Inst1.GrplEwIw_mem_or o = (Inst1.GrplEwIw_mem_or) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "|", "", "ORW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_adc) {
                    Inst1.GrplEwIw_mem_adc o = (Inst1.GrplEwIw_mem_adc) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "+", "+(Flags.get_CF()?1:0)", "ADCW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sbb) {
                    Inst1.GrplEwIw_mem_sbb o = (Inst1.GrplEwIw_mem_sbb) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "-", "-(Flags.get_CF()?1:0)", "SBBW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_and) {
                    Inst1.GrplEwIw_mem_and o = (Inst1.GrplEwIw_mem_and) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "&", "", "ANDW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_sub) {
                    Inst1.GrplEwIw_mem_sub o = (Inst1.GrplEwIw_mem_sub) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "-", "", "SUBW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_xor) {
                    Inst1.GrplEwIw_mem_xor o = (Inst1.GrplEwIw_mem_xor) op;
                    instructionEG((setFlags & o.sets())==0, 16, o.get_eaa, new Reg(o.ib), "^", "", "XORW", seg, method);
                    return true;
                }
                if (op instanceof Inst1.GrplEwIw_mem_cmp) {
                    Inst1.GrplEwIw_mem_cmp o = (Inst1.GrplEwIw_mem_cmp) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.CMPW(");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.TESTB(");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.TESTW(");
                    method.append(nameGet16(o.rw));
                    method.append(",Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x86: // XCHG Eb,Gb
            case 0x286:
                if (op instanceof Inst1.XchgEbGb_reg) {
                    Inst1.XchgEbGb_reg o = (Inst1.XchgEbGb_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet8(o.rb));
                    method.append(";");
                    method.append(nameSet8(o.rb, nameGet8(o.earb)));
                    method.append(";");
                    method.append(nameSet8(o.earb, "val"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XchgEbGb_mem) {
                    Inst1.XchgEbGb_mem o = (Inst1.XchgEbGb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readb(eaa);Memory.mem_writeb(eaa,");method.append(nameGet8(o.rb));method.append(");");
                    method.append(nameSet8(o.rb, "val"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x87: // XCHG Ew,Gw
                if (op instanceof Inst1.XchgEwGw_reg) {
                    Inst1.XchgEwGw_reg o = (Inst1.XchgEwGw_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet16(o.rw));
                    method.append(";");
                    nameSet16(o.rw, nameGet16(o.earw), method);
                    method.append(";");
                    nameSet16(o.earw, "val", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.XchgEwGw_mem) {
                    Inst1.XchgEwGw_mem o = (Inst1.XchgEwGw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readw(eaa);Memory.mem_writew(eaa,");method.append(nameGet16(o.rw));method.append(");");
                    nameSet16(o.rw, "val", method);
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
                    method.append("if (CPU.cpu.pmode && !CPU.cpu.code.big) {jdos.cpu.CPU.Descriptor desc=new jdos.cpu.CPU.Descriptor();CPU.cpu.gdt.GetDescriptor(CPU.seg_value(");method.append(seg.val);method.append("),desc);if ((desc.Type()==CPU.DESC_CODE_R_NC_A) || (desc.Type()==CPU.DESC_CODE_R_NC_NA)) {").append(preException).append("CPU.CPU_PrepareException(CPU.EXCEPTION_GP,CPU.seg_value(");method.append(seg.val);method.append(") & 0xfffc);return RUNEXCEPTION();}}");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa,");
                    method.append(nameGet8(o.rb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.MovEbGb_mem) {
                    Inst1.MovEbGb_mem o = (Inst1.MovEbGb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa,");
                    method.append(nameGet8(o.rb));
                    method.append(");");
                    return true;
                }
                break;
            case 0x89: // MOV Ew,Gw
                if (op instanceof Inst1.MovEwGw_reg) {
                    Inst1.MovEwGw_reg o = (Inst1.MovEwGw_reg) op;
                    nameSet16(o.earw, nameGet16(o.rw), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwGw_mem) {
                    Inst1.MovEwGw_mem o = (Inst1.MovEwGw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa,");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append(nameSet8(o.rb, "Memory.mem_readb(eaa)"));
                    method.append(";");
                    return true;
                }
                break;
            case 0x8b: // MOV Gw,Ew
                if (op instanceof Inst1.MovGwEw_reg) {
                    Inst1.MovGwEw_reg o = (Inst1.MovGwEw_reg) op;
                    nameSet16(o.rw, nameGet16(o.earw), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovGwEw_mem) {
                    Inst1.MovGwEw_mem o = (Inst1.MovGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "Memory.mem_readw(eaa)", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x8c: // Mov Ew,Sw
                if (op instanceof Inst1.MovEwEs_reg) {
                    Inst1.MovEwEs_reg o = (Inst1.MovEwEs_reg) op;
                    nameSet16(o.earw, "CPU_Regs.reg_esVal.dword", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwEs_mem) {
                    Inst1.MovEwEs_mem o = (Inst1.MovEwEs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_esVal.dword);");
                    return true;
                }
                if (op instanceof Inst1.MovEwCs_reg) {
                    Inst1.MovEwCs_reg o = (Inst1.MovEwCs_reg) op;
                    nameSet16(o.earw, "CPU_Regs.reg_csVal.dword", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwCs_mem) {
                    Inst1.MovEwCs_mem o = (Inst1.MovEwCs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_csVal.dword);");
                    return true;
                }
                if (op instanceof Inst1.MovEwSs_reg) {
                    Inst1.MovEwSs_reg o = (Inst1.MovEwSs_reg) op;
                    nameSet16(o.earw, "CPU_Regs.reg_ssVal.dword", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwSs_mem) {
                    Inst1.MovEwSs_mem o = (Inst1.MovEwSs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_ssVal.dword);");
                    return true;
                }
                if (op instanceof Inst1.MovEwDs_reg) {
                    Inst1.MovEwDs_reg o = (Inst1.MovEwDs_reg) op;
                    nameSet16(o.earw, "CPU_Regs.reg_dsVal.dword", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwDs_mem) {
                    Inst1.MovEwDs_mem o = (Inst1.MovEwDs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_dsVal.dword);");
                    return true;
                }
                if (op instanceof Inst1.MovEwFs_reg) {
                    Inst1.MovEwFs_reg o = (Inst1.MovEwFs_reg) op;
                    nameSet16(o.earw, "CPU_Regs.reg_fsVal.dword", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovEwFs_mem) {
                    Inst1.MovEwFs_mem o = (Inst1.MovEwFs_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", CPU_Regs.reg_fsVal.dword);");
                    return true;
                }
                if (op instanceof Inst1.MovEwGs_reg) {
                    Inst1.MovEwGs_reg o = (Inst1.MovEwGs_reg) op;
                    nameSet16(o.earw, "CPU_Regs.reg_gsVal.dword", method);
                    method.append(";");
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
            case 0x8d: // LEA Gw
                if (op instanceof Inst1.LeaGw_32) {
                    Inst1.LeaGw_32 o = (Inst1.LeaGw_32) op;
                    memory_start(o.get_eaa, seg, method, true);
                    nameSet16(o.rw, "eaa", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.LeaGw_16) {
                    Inst1.LeaGw_16 o = (Inst1.LeaGw_16) op;
                    memory_start(o.get_eaa, seg, method, true);
                    nameSet16(o.rw, "eaa", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x8e: // MOV Sw,Ew
            case 0x28e:
                if (op instanceof Inst1.MovEsEw_reg) {
                    Inst1.MovEsEw_reg o = (Inst1.MovEsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralES(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovEsEw_mem) {
                    Inst1.MovEsEw_mem o = (Inst1.MovEsEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovSsEw_reg) {
                    Inst1.MovSsEw_reg o = (Inst1.MovSsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralSS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovSsEw_mem) {
                    Inst1.MovSsEw_mem o = (Inst1.MovSsEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralSS(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovDsEw_reg) {
                    Inst1.MovDsEw_reg o = (Inst1.MovDsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralDS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovDsEw_mem) {
                    Inst1.MovDsEw_mem o = (Inst1.MovDsEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovFsEw_reg) {
                    Inst1.MovFsEw_reg o = (Inst1.MovFsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralFS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovFsEw_mem) {
                    Inst1.MovFsEw_mem o = (Inst1.MovFsEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralFS(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovGsEw_reg) {
                    Inst1.MovGsEw_reg o = (Inst1.MovGsEw_reg) op;
                    method.append("if (CPU.CPU_SetSegGeneralGS(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst1.MovGsEw_mem) {
                    Inst1.MovGsEw_mem o = (Inst1.MovGsEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_SetSegGeneralGS(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x8f: // POP Ew
                if (op instanceof Inst1.PopEw_reg) {
                    Inst1.PopEw_reg o = (Inst1.PopEw_reg) op;
                    nameSet16(o.earw, "CPU.CPU_Pop16()", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.PopEw_mem) {
                    Inst1.PopEw_mem o = (Inst1.PopEw_mem) op;
                    declareVal(method);
                    method.append("val = CPU.CPU_Pop16();");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, val);");
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
            case 0x92: // XCHG DX,AX
            case 0x93: // XCHG BX,AX
            case 0x94: // XCHG SP,AX
            case 0x95: // XCHG BP,AX
            case 0x96: // XCHG SI,AX
            case 0x97: // XCHG DI,AX
                if (op instanceof Inst1.XchgAx) {
                    Inst1.XchgAx o = (Inst1.XchgAx) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet16(o.reg));
                    method.append(";");
                    nameSet16(o.reg, "CPU_Regs.reg_eax.word()", method);
                    method.append(";CPU_Regs.reg_eax.word(val);");
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
                    method.append("if (CPU.CPU_PUSHF(false)) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x9d: // POPF
                if (op instanceof Inst1.PopF) {
                    Inst1.PopF o = (Inst1.PopF) op;
                    method.append("if (CPU.CPU_POPF(false)) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(");method.append(seg.getDs());method.append("+");
                    method.append(o.value);
                    method.append("));");
                    return true;
                }
                break;
            case 0xa1: // MOV AX,Ow
                if (op instanceof Inst1.MovAXOw) {
                    Inst1.MovAXOw o = (Inst1.MovAXOw) op;
                    method.append("CPU_Regs.reg_eax.word(Memory.mem_readw(");method.append(seg.getDs());method.append("+");
                    method.append(o.value);
                    method.append("));");
                    return true;
                }
                break;
            case 0xa2: // MOV Ob,AL
            case 0x2a2:
                if (op instanceof Inst1.MovObAL) {
                    Inst1.MovObAL o = (Inst1.MovObAL) op;
                    method.append("Memory.mem_writeb(");method.append(seg.getDs());method.append("+");
                    method.append(o.value);
                    method.append(", CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0xa3: // MOV Ow,AX
                if (op instanceof Inst1.MovOwAX) {
                    Inst1.MovOwAX o = (Inst1.MovOwAX) op;
                    method.append("Memory.mem_writew(");method.append(seg.getDs());method.append("+");
                    method.append(o.value);
                    method.append(", CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0xa4: // MOVSB
            case 0x2a4:
                method.append("Core.base_ds=");
                method.append(seg.getDs());
                method.append(";");
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
                method.append("Core.base_ds=");
                method.append(seg.getDs());
                method.append(";");
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
            case 0xa7: // CMPSW
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
            case 0xa8: // TEST AL,Ib
            case 0x2a8:
                if (op instanceof Inst1.TestAlIb) {
                    Inst1.TestAlIb o = (Inst1.TestAlIb) op;
                    method.append("Instructions.TESTB(");
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
            case 0xab: // STOSW
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
            case 0xac: // LODSB
            case 0x2ac:
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
            case 0xad: // LODSW
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
            case 0xae: // SCASB
            case 0x2ae:
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
            case 0xaf: // SCASW
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
            case 0xb0: // MOV AL,Ib
            case 0x2b0:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb1: // MOV CL,Ib
            case 0x2b1:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb2: // MOV DL,Ib
            case 0x2b2:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb3: // MOV BL,Ib
            case 0x2b3:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb4: // MOV AH,Ib
            case 0x2b4:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb5: // MOV CH,Ib
            case 0x2b5:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb6: // MOV DH,Ib
            case 0x2b6:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb7: // MOV BH,Ib
            case 0x2b7:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                break;
            case 0xb8: // MOV AX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xb9: // MOV CX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xba: // MOV DX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xbb: // MOV BX,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xbc: // MOV SP,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xbd: // MOV BP.Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xbe: // MOV SI,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xbf: // MOV DI,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xc0: // GRP2 Eb,Ib
            case 0x2c0:
                if (op instanceof Grp2.ROLB_reg) {
                    Grp2.ROLB_reg o = (Grp2.ROLB_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_ROLB(val, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_ROLB(" + o.val + ", val)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORB_reg) {
                    Grp2.RORB_reg o = (Grp2.RORB_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_RORB(val, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_RORB(" + o.val + ", val)"));
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_ROLB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_ROLB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORB_mem) {
                    Grp2.RORB_mem o = (Grp2.RORB_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_RORB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_RORB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLB_mem) {
                    Grp2.RCLB_mem o = (Grp2.RCLB_mem) op;
                    if (Instructions.valid_RCLB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_RCLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRB_mem) {
                    Grp2.RCRB_mem o = (Grp2.RCRB_mem) op;
                    if (Instructions.valid_RCRB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_RCRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLB_mem) {
                    Grp2.SHLB_mem o = (Grp2.SHLB_mem) op;
                    if (Instructions.valid_SHLB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_SHLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRB_mem) {
                    Grp2.SHRB_mem o = (Grp2.SHRB_mem) op;
                    if (Instructions.valid_SHRB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_SHRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARB_mem) {
                    Grp2.SARB_mem o = (Grp2.SARB_mem) op;
                    if (Instructions.valid_SARB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_SARB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                break;
            case 0xc1: // GRP2 Ew,Ib
                if (op instanceof Grp2.ROLW_reg) {
                    Grp2.ROLW_reg o = (Grp2.ROLW_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_ROLW(val, ");
                    method.append(o.val);
                    method.append(")) ");
                    nameSet16(o.earw, "Instructions.do_ROLW(", String.valueOf(o.val), ", val)", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORW_reg) {
                    Grp2.RORW_reg o = (Grp2.RORW_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_RORW(val, ");
                    method.append(o.val);
                    method.append(")) ");
                    nameSet16(o.earw, "Instructions.do_RORW(", String.valueOf(o.val), ", val)", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLW_reg) {
                    Grp2.RCLW_reg o = (Grp2.RCLW_reg) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_RCLW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_reg) {
                    Grp2.RCRW_reg o = (Grp2.RCRW_reg) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_RCRW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_reg) {
                    Grp2.SHLW_reg o = (Grp2.SHLW_reg) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_SHLW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_reg) {
                    Grp2.SHRW_reg o = (Grp2.SHRW_reg) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_SHRW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_reg) {
                    Grp2.SARW_reg o = (Grp2.SARW_reg) op;
                    if (Instructions.valid_SARW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_SARW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLW_mem) {
                    Grp2.ROLW_mem o = (Grp2.ROLW_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_ROLW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_ROLW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORW_mem) {
                    Grp2.RORW_mem o = (Grp2.RORW_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_RORW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_RORW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLW_mem) {
                    Grp2.RCLW_mem o = (Grp2.RCLW_mem) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_RCLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_mem) {
                    Grp2.RCRW_mem o = (Grp2.RCRW_mem) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_RCRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_mem) {
                    Grp2.SHLW_mem o = (Grp2.SHLW_mem) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_SHLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_mem) {
                    Grp2.SHRW_mem o = (Grp2.SHRW_mem) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_SHRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_mem) {
                    Grp2.SARW_mem o = (Grp2.SARW_mem) op;
                    if (Instructions.valid_SARW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_SARW(");
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
                    memory_start(o.get_eaa, seg, method);
                    // make sure all reads are done before writing something in case of a PF
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralES(Memory.mem_readw(eaa+2))) {").append(preException).append("return RUNEXCEPTION();}");
                    nameSet16(o.rw, "val", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xc5: // LDS
                if (op instanceof Inst1.Lds) {
                    Inst1.Lds o = (Inst1.Lds) op;
                    memory_start(o.get_eaa, seg, method);
                    // make sure all reads are done before writing something in case of a PF
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);if (CPU.CPU_SetSegGeneralDS(Memory.mem_readw(eaa+2))) {").append(preException).append("return RUNEXCEPTION();}");
                    nameSet16(o.rw, "val", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0xc6: // MOV Eb,Ib
            case 0x2c6:
                if (op instanceof Inst1.MovIb) {
                    Inst1.MovIb o = (Inst1.MovIb) op;
                    method.append(nameSet8(o.reg, String.valueOf(o.ib)));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovIb_mem) {
                    Inst1.MovIb_mem o = (Inst1.MovIb_mem) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", ");
                    method.append(o.ib);
                    method.append(");");
                    return true;
                }
                break;
            case 0xc7: // MOV EW,Iw
                if (op instanceof Inst1.MovIw) {
                    Inst1.MovIw o = (Inst1.MovIw) op;
                    nameSet16(o.reg, String.valueOf(o.ib), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst1.MovIw_mem) {
                    Inst1.MovIw_mem o = (Inst1.MovIw_mem) op;
                    method.append("Memory.mem_writew(");
                    toStringValue(o.get_eaa, seg, method);
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
                    // Inst1.Leave o = (Inst1.Leave) op;
                    method.append("CPU_Regs.reg_esp.dword&=CPU.cpu.stack.notmask;CPU_Regs.reg_esp.dword|=(");nameGet32(CPU_Regs.reg_ebp, method);method.append(" & CPU.cpu.stack.mask);CPU_Regs.reg_ebp.word(CPU.CPU_Pop16());");
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
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_ROLB(val, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_ROLB(" + o.val + ", val)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORB_reg) {
                    Grp2.RORB_reg o = (Grp2.RORB_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_RORB(val, ");
                    method.append(o.val);
                    method.append("))");
                    method.append(nameSet8(o.earb, "Instructions.do_RORB(" + o.val + ", val)"));
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_ROLB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_ROLB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORB_mem) {
                    Grp2.RORB_mem o = (Grp2.RORB_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_RORB(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writeb(eaa, Instructions.do_RORB(");
                    method.append(o.val);
                    method.append(", Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLB_mem) {
                    Grp2.RCLB_mem o = (Grp2.RCLB_mem) op;
                    if (Instructions.valid_RCLB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_RCLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRB_mem) {
                    Grp2.RCRB_mem o = (Grp2.RCRB_mem) op;
                    if (Instructions.valid_RCRB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_RCRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLB_mem) {
                    Grp2.SHLB_mem o = (Grp2.SHLB_mem) op;
                    if (Instructions.valid_SHLB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_SHLB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRB_mem) {
                    Grp2.SHRB_mem o = (Grp2.SHRB_mem) op;
                    if (Instructions.valid_SHRB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_SHRB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARB_mem) {
                    Grp2.SARB_mem o = (Grp2.SARB_mem) op;
                    if (Instructions.valid_SARB(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writeb(eaa, Instructions.do_SARB(");
                        method.append(o.val);
                        method.append(", Memory.mem_readb(eaa)));");
                    }
                    return true;
                }
                break;
            case 0xd1: // GRP2 Ew,1
                if (op instanceof Grp2.ROLW_reg) {
                    Grp2.ROLW_reg o = (Grp2.ROLW_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_ROLW(val, ");
                    method.append(o.val);
                    method.append(")) ");
                    nameSet16(o.earw, "Instructions.do_ROLW(", String.valueOf(o.val), ", val)", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORW_reg) {
                    Grp2.RORW_reg o = (Grp2.RORW_reg) op;
                    declareVal(method);
                    method.append("val = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_RORW(val, ");
                    method.append(o.val);
                    method.append(")) ");
                    nameSet16(o.earw, "Instructions.do_RORW(", String.valueOf(o.val), ", val)", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLW_reg) {
                    Grp2.RCLW_reg o = (Grp2.RCLW_reg) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_RCLW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_reg) {
                    Grp2.RCRW_reg o = (Grp2.RCRW_reg) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_RCRW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_reg) {
                    Grp2.SHLW_reg o = (Grp2.SHLW_reg) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_SHLW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_reg) {
                    Grp2.SHRW_reg o = (Grp2.SHRW_reg) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_SHRW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_reg) {
                    Grp2.SARW_reg o = (Grp2.SARW_reg) op;
                    if (Instructions.valid_SARW(o.val)) {
                        nameSet16(o.earw, "Instructions.do_SARW(", String.valueOf(o.val), ", ", nameGet16(o.earw), ")", method);
                        method.append(";");
                    }
                    return true;
                }
                if (op instanceof Grp2.ROLW_mem) {
                    Grp2.ROLW_mem o = (Grp2.ROLW_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_ROLW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_ROLW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORW_mem) {
                    Grp2.RORW_mem o = (Grp2.RORW_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (Instructions.valid_RORW(eaa, ");
                    method.append(o.val);
                    method.append(")) Memory.mem_writew(eaa, Instructions.do_RORW(");
                    method.append(o.val);
                    method.append(", Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLW_mem) {
                    Grp2.RCLW_mem o = (Grp2.RCLW_mem) op;
                    if (Instructions.valid_RCLW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_RCLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.RCRW_mem) {
                    Grp2.RCRW_mem o = (Grp2.RCRW_mem) op;
                    if (Instructions.valid_RCRW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_RCRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHLW_mem) {
                    Grp2.SHLW_mem o = (Grp2.SHLW_mem) op;
                    if (Instructions.valid_SHLW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_SHLW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SHRW_mem) {
                    Grp2.SHRW_mem o = (Grp2.SHRW_mem) op;
                    if (Instructions.valid_SHRW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_SHRW(");
                        method.append(o.val);
                        method.append(", Memory.mem_readw(eaa)));");
                    }
                    return true;
                }
                if (op instanceof Grp2.SARW_mem) {
                    Grp2.SARW_mem o = (Grp2.SARW_mem) op;
                    if (Instructions.valid_SARW(o.val)) {
                        memory_start(o.get_eaa, seg, method);
                        method.append("Memory.mem_writew(eaa, Instructions.do_SARW(");
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
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;");
                    declareVal2(method);
                    method.append("val2 = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_ROLB(val2, val))");
                    method.append(nameSet8(o.earb, "Instructions.do_ROLB(val, val2)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORB_reg_cl) {
                    Grp2.RORB_reg_cl o = (Grp2.RORB_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;");
                    declareVal2(method);
                    method.append("val2 = ");
                    method.append(nameGet8(o.earb));
                    method.append(";if (Instructions.valid_RORB(val2, val))");
                    method.append(nameSet8(o.earb, "Instructions.do_RORB(val, val2)"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLB_reg_cl) {
                    Grp2.RCLB_reg_cl o = (Grp2.RCLB_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCLB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_RCLB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCRB_reg_cl) {
                    Grp2.RCRB_reg_cl o = (Grp2.RCRB_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCRB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_RCRB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHLB_reg_cl) {
                    Grp2.SHLB_reg_cl o = (Grp2.SHLB_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHLB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_SHLB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHRB_reg_cl) {
                    Grp2.SHRB_reg_cl o = (Grp2.SHRB_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHRB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_SHRB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SARB_reg_cl) {
                    Grp2.SARB_reg_cl o = (Grp2.SARB_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SARB (val))");
                    method.append(nameSet8(o.earb, "Instructions.do_SARB(val, " + nameGet8(o.earb) + ")"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.ROLB_mem_cl) {
                    Grp2.ROLB_mem_cl o = (Grp2.ROLB_mem_cl) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_ROLB(eaa, val)) Memory.mem_writeb(eaa, Instructions.do_ROLB(val, Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORB_mem_cl) {
                    Grp2.RORB_mem_cl o = (Grp2.RORB_mem_cl) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RORB(eaa, val)) Memory.mem_writeb(eaa, Instructions.do_RORB(val, Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLB_mem_cl) {
                    Grp2.RCLB_mem_cl o = (Grp2.RCLB_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_RCLB (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.do_RCLB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCRB_mem_cl) {
                    Grp2.RCRB_mem_cl o = (Grp2.RCRB_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_RCRB (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.do_RCRB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHLB_mem_cl) {
                    Grp2.SHLB_mem_cl o = (Grp2.SHLB_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_SHLB (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.do_SHLB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHRB_mem_cl) {
                    Grp2.SHRB_mem_cl o = (Grp2.SHRB_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_SHRB (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.do_SHRB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SARB_mem_cl) {
                    Grp2.SARB_mem_cl o = (Grp2.SARB_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f; if (Instructions.valid_SARB (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.do_SARB(val, Memory.mem_readb(eaa)));}");
                    return true;
                }
                break;
            case 0xd3: // GRP2 Ew,CL
                if (op instanceof Grp2.ROLW_reg_cl) {
                    Grp2.ROLW_reg_cl o = (Grp2.ROLW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;");
                    declareVal2(method);
                    method.append("val2 = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_ROLW(val2, val))");
                    nameSet16(o.earw, "Instructions.do_ROLW(val, val2)", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RORW_reg_cl) {
                    Grp2.RORW_reg_cl o = (Grp2.RORW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;");
                    declareVal2(method);
                    method.append("val2 = ");
                    method.append(nameGet16(o.earw));
                    method.append(";if (Instructions.valid_RORW(val2, val))");
                    nameSet16(o.earw, "Instructions.do_RORW(val, val2)", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCLW_reg_cl) {
                    Grp2.RCLW_reg_cl o = (Grp2.RCLW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCLW (val))");
                    nameSet16(o.earw, "Instructions.do_RCLW(val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.RCRW_reg_cl) {
                    Grp2.RCRW_reg_cl o = (Grp2.RCRW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCRW (val))");
                    nameSet16(o.earw, "Instructions.do_RCRW(val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHLW_reg_cl) {
                    Grp2.SHLW_reg_cl o = (Grp2.SHLW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHLW (val))");
                    nameSet16(o.earw, "Instructions.do_SHLW(val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SHRW_reg_cl) {
                    Grp2.SHRW_reg_cl o = (Grp2.SHRW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHRW (val))");
                    nameSet16(o.earw, "Instructions.do_SHRW(val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.SARW_reg_cl) {
                    Grp2.SARW_reg_cl o = (Grp2.SARW_reg_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SARW (val))");
                    nameSet16(o.earw, "Instructions.do_SARW(val, ", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp2.ROLW_mem_cl) {
                    Grp2.ROLW_mem_cl o = (Grp2.ROLW_mem_cl) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_ROLW(eaa, val)) Memory.mem_writew(eaa, Instructions.do_ROLW(val, Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RORW_mem_cl) {
                    Grp2.RORW_mem_cl o = (Grp2.RORW_mem_cl) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RORW(eaa, val)) Memory.mem_writew(eaa, Instructions.do_RORW(val, Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Grp2.RCLW_mem_cl) {
                    Grp2.RCLW_mem_cl o = (Grp2.RCLW_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCLW (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.do_RCLW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.RCRW_mem_cl) {
                    Grp2.RCRW_mem_cl o = (Grp2.RCRW_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_RCRW (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.do_RCRW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHLW_mem_cl) {
                    Grp2.SHLW_mem_cl o = (Grp2.SHLW_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHLW (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.do_SHLW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SHRW_mem_cl) {
                    Grp2.SHRW_mem_cl o = (Grp2.SHRW_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SHRW (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.do_SHRW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                if (op instanceof Grp2.SARW_mem_cl) {
                    Grp2.SARW_mem_cl o = (Grp2.SARW_mem_cl) op;
                    declareVal(method);
                    method.append("val = CPU_Regs.reg_ecx.low() & 0x1f;if (Instructions.valid_SARW (val)) {");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.do_SARW(val, Memory.mem_readw(eaa)));}");
                    return true;
                }
                break;
            case 0xd4: // AAM o.ib
            case 0x2d4:
                if (op instanceof Inst1.AamIb) {
                    Inst1.AamIb o = (Inst1.AamIb) op;
                    if (o.ib == 0) {
                        method.append(preException).append("return EXCEPTION(0);");
                        return false;
                    }
                    method.append("Instructions.AAM(");
                    method.append(o.ib);
                    method.append(");");
                    return true;
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
                    method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(");method.append(seg.getDs());method.append("+CPU_Regs.reg_ebx.dword+CPU_Regs.reg_eax.low()));");
                    return true;
                }
                if (op instanceof Inst1.Xlat16) {
                    method.append("CPU_Regs.reg_eax.low(Memory.mem_readb(");method.append(seg.getDs());method.append("+((CPU_Regs.reg_ebx.word()+CPU_Regs.reg_eax.low()) & 0xFFFF)));");
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
                if (op instanceof InstFPU.FADD_ST0_STj) {
                    InstFPU.FADD_ST0_STj o = (InstFPU.FADD_ST0_STj) op;
                    method.append("FPU.FADD_ST0_STj(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FMUL_ST0_STj) {
                    InstFPU.FMUL_ST0_STj o = (InstFPU.FMUL_ST0_STj) op;
                    method.append("FPU.FMUL_ST0_STj(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOM_STi) {
                    InstFPU.FCOM_STi o = (InstFPU.FCOM_STi) op;
                    method.append("FPU.FCOM_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUB_ST0_STj) {
                    InstFPU.FSUB_ST0_STj o = (InstFPU.FSUB_ST0_STj) op;
                    method.append("FPU.FSUB_ST0_STj(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUBR_ST0_STj) {
                    InstFPU.FSUBR_ST0_STj o = (InstFPU.FSUBR_ST0_STj) op;
                    method.append("FPU.FSUBR_ST0_STj(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIV_ST0_STj) {
                    InstFPU.FDIV_ST0_STj o = (InstFPU.FDIV_ST0_STj) op;
                    method.append("FPU.FDIV_ST0_STj(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIVR_ST0_STj) {
                    InstFPU.FDIVR_ST0_STj o = (InstFPU.FDIVR_ST0_STj) op;
                    method.append("FPU.FDIVR_ST0_STj(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU0_ea) {
                    Inst1.FPU0_ea o = (Inst1.FPU0_ea) op;
                    method.append("FPU.FPU_ESC0_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FADD_SINGLE_REAL) {
                    InstFPU.FADD_SINGLE_REAL o = (InstFPU.FADD_SINGLE_REAL) op;
                    method.append("FPU.FADD_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FMUL_SINGLE_REAL) {
                    InstFPU.FMUL_SINGLE_REAL o = (InstFPU.FMUL_SINGLE_REAL) op;
                    method.append("FPU.FMUL_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOM_SINGLE_REAL) {
                    if (op instanceof InstFPU.FCOM_SINGLE_REAL) {
                        InstFPU.FCOM_SINGLE_REAL o = (InstFPU.FCOM_SINGLE_REAL) op;
                        method.append("FPU.FCOM_SINGLE_REAL(");
                        toStringValue(o.get_eaa, seg, method);
                        method.append(",");
                        method.append(o.pop);
                        method.append(");");
                        return true;
                    }
                }
                if (op instanceof InstFPU.FSUB_SINGLE_REAL) {
                    InstFPU.FSUB_SINGLE_REAL o = (InstFPU.FSUB_SINGLE_REAL) op;
                    method.append("FPU.FSUB_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUBR_SINGLE_REAL) {
                    InstFPU.FSUBR_SINGLE_REAL o = (InstFPU.FSUBR_SINGLE_REAL) op;
                    method.append("FPU.FSUBR_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIV_SINGLE_REAL) {
                    InstFPU.FDIV_SINGLE_REAL o = (InstFPU.FDIV_SINGLE_REAL) op;
                    method.append("FPU.FDIV_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIVR_SINGLE_REAL) {
                    InstFPU.FDIVR_SINGLE_REAL o = (InstFPU.FDIVR_SINGLE_REAL) op;
                    method.append("FPU.FDIVR_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FLD_STi) {
                    InstFPU.FLD_STi o = (InstFPU.FLD_STi) op;
                    method.append("FPU.FLD_STi(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FXCH_STi) {
                    InstFPU.FXCH_STi o = (InstFPU.FXCH_STi) op;
                    method.append("FPU.FXCH_STi(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FNOP) {
                    method.append("FPU.FNOP();");
                    return true;
                }
                if (op instanceof InstFPU.FST_STi) {
                    InstFPU.FST_STi o = (InstFPU.FST_STi) op;
                    method.append("FPU.FST_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCHS) {
                    method.append("FPU.FCHS();");
                    return true;
                }
                if (op instanceof InstFPU.FABS) {
                    method.append("FPU.FABS();");
                    return true;
                }
                if (op instanceof InstFPU.FTST) {
                    method.append("FPU.FTST();");
                    return true;
                }
                if (op instanceof InstFPU.FXAM) {
                    method.append("FPU.FXAM();");
                    return true;
                }
                if (op instanceof InstFPU.FLD1) {
                    method.append("FPU.FLD1();");
                    return true;
                }
                if (op instanceof InstFPU.FLDL2T) {
                    method.append("FPU.FLDL2T();");
                    return true;
                }
                if (op instanceof InstFPU.FLDL2E) {
                    method.append("FPU.FLDL2E();");
                    return true;
                }
                if (op instanceof InstFPU.FLDPI) {
                    method.append("FPU.FLDPI();");
                    return true;
                }
                if (op instanceof InstFPU.FLDLG2) {
                    method.append("FPU.FLDLG2();");
                    return true;
                }
                if (op instanceof InstFPU.FLDLN2) {
                    method.append("FPU.FLDLN2();");
                    return true;
                }
                if (op instanceof InstFPU.FLDZ) {
                    method.append("FPU.FLDZ();");
                    return true;
                }
                if (op instanceof InstFPU.F2XM1) {
                    method.append("FPU.F2XM1();");
                    return true;
                }
                if (op instanceof InstFPU.FYL2X) {
                    method.append("FPU.FYL2X();");
                    return true;
                }
                if (op instanceof InstFPU.FPTAN) {
                    method.append("FPU.FPTAN();");
                    return true;
                }
                if (op instanceof InstFPU.FPATAN) {
                    method.append("FPU.FPATAN();");
                    return true;
                }
                if (op instanceof InstFPU.FXTRACT) {
                    method.append("FPU.FXTRACT();");
                    return true;
                }
                if (op instanceof InstFPU.FPREM) {
                    InstFPU.FPREM o = (InstFPU.FPREM)op;
                    method.append("FPU.FPREM(");
                    method.append(o.bRoundNearest);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDECSTP) {
                    method.append("FPU.FDECSTP();");
                    return true;
                }
                if (op instanceof InstFPU.FINCSTP) {
                    method.append("FPU.FINCSTP();");
                    return true;
                }
                if (op instanceof InstFPU.FYL2XP1) {
                    method.append("FPU.FYL2XP1();");
                    return true;
                }
                if (op instanceof InstFPU.FSQRT) {
                    method.append("FPU.FSQRT();");
                    return true;
                }
                if (op instanceof InstFPU.FSINCOS) {
                    method.append("FPU.FSINCOS();");
                    return true;
                }
                if (op instanceof InstFPU.FRNDINT) {
                    method.append("FPU.FRNDINT();");
                    return true;
                }
                if (op instanceof InstFPU.FSCALE) {
                    method.append("FPU.FSCALE();");
                    return true;
                }
                if (op instanceof InstFPU.FSIN) {
                    method.append("FPU.FSIN();");
                    return true;
                }
                if (op instanceof InstFPU.FCOS) {
                    method.append("FPU.FCOS();");
                    return true;
                }
                if (op instanceof Inst1.FPU1_ea) {
                    Inst1.FPU1_ea o = (Inst1.FPU1_ea) op;
                    method.append("FPU.FPU_ESC1_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FLD_SINGLE_REAL) {
                    InstFPU.FLD_SINGLE_REAL o = (InstFPU.FLD_SINGLE_REAL) op;
                    method.append("FPU.FLD_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FST_SINGLE_REAL) {
                    InstFPU.FST_SINGLE_REAL o = (InstFPU.FST_SINGLE_REAL) op;
                    method.append("FPU.FST_SINGLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FLDENV) {
                    InstFPU.FLDENV o = (InstFPU.FLDENV) op;
                    method.append("FPU.FLDENV(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FLDCW) {
                    InstFPU.FLDCW o = (InstFPU.FLDCW) op;
                    method.append("FPU.FLDCW(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FNSTENV) {
                    InstFPU.FNSTENV o = (InstFPU.FNSTENV) op;
                    method.append("FPU.FNSTENV(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FNSTCW) {
                    InstFPU.FNSTCW o = (InstFPU.FNSTCW) op;
                    method.append("FPU.FNSTCW(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FCMOV_ST0_STj_CF) {
                    InstFPU.FCMOV_ST0_STj_CF o = (InstFPU.FCMOV_ST0_STj_CF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", Flags.get_CF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCMOV_ST0_STj_ZF) {
                    InstFPU.FCMOV_ST0_STj_ZF o = (InstFPU.FCMOV_ST0_STj_ZF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", Flags.get_ZF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCMOV_ST0_STj_CF_OR_ZF) {
                    InstFPU.FCMOV_ST0_STj_CF_OR_ZF o = (InstFPU.FCMOV_ST0_STj_CF_OR_ZF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", Flags.get_CF() || Flags.get_ZF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCMOV_ST0_STj_PF) {
                    InstFPU.FCMOV_ST0_STj_PF o = (InstFPU.FCMOV_ST0_STj_PF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", Flags.get_PF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FUCOMPP) {
                    method.append("FPU.FUCOMPP();");
                }
                if (op instanceof Inst1.FPU2_ea) {
                    Inst1.FPU2_ea o = (Inst1.FPU2_ea) op;
                    method.append("FPU.FPU_ESC2_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIADD_DWORD_INTEGER) {
                    InstFPU.FIADD_DWORD_INTEGER o = (InstFPU.FIADD_DWORD_INTEGER) op;
                    method.append("FPU.FIADD_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIMUL_DWORD_INTEGER) {
                    InstFPU.FIMUL_DWORD_INTEGER o = (InstFPU.FIMUL_DWORD_INTEGER) op;
                    method.append("FPU.FIMUL_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FICOM_DWORD_INTEGER) {
                    InstFPU.FICOM_DWORD_INTEGER o = (InstFPU.FICOM_DWORD_INTEGER) op;
                    method.append("FPU.FICOM_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISUB_DWORD_INTEGER) {
                    InstFPU.FISUB_DWORD_INTEGER o = (InstFPU.FISUB_DWORD_INTEGER) op;
                    method.append("FPU.FISUB_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISUBR_DWORD_INTEGER) {
                    InstFPU.FISUBR_DWORD_INTEGER o = (InstFPU.FISUBR_DWORD_INTEGER) op;
                    method.append("FPU.FISUBR_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIDIV_DWORD_INTEGER) {
                    InstFPU.FIDIV_DWORD_INTEGER o = (InstFPU.FIDIV_DWORD_INTEGER) op;
                    method.append("FPU.FIDIV_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIDIVR_DWORD_INTEGER) {
                    InstFPU.FIDIVR_DWORD_INTEGER o = (InstFPU.FIDIVR_DWORD_INTEGER) op;
                    method.append("FPU.FIDIVR_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FCMOV_ST0_STj_NCF) {
                    InstFPU.FCMOV_ST0_STj_NCF o = (InstFPU.FCMOV_ST0_STj_NCF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", !Flags.get_CF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCMOV_ST0_STj_NZF) {
                    InstFPU.FCMOV_ST0_STj_NZF o = (InstFPU.FCMOV_ST0_STj_NZF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", !Flags.get_ZF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCMOV_ST0_STj_NCF_AND_NZF) {
                    InstFPU.FCMOV_ST0_STj_NCF_AND_NZF o = (InstFPU.FCMOV_ST0_STj_NCF_AND_NZF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", !Flags.get_CF() && !Flags.get_ZF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCMOV_ST0_STj_NPF) {
                    InstFPU.FCMOV_ST0_STj_NPF o = (InstFPU.FCMOV_ST0_STj_NPF) op;
                    method.append("FPU.FCMOV_ST0_STj(");
                    method.append(o.rm);
                    method.append(", !Flags.get_PF()");
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FUCOMPP) {
                    method.append("FPU.FNCLEX();");
                }
                if (op instanceof InstFPU.FNINIT) {
                    method.append("FPU.FNINIT();");
                }
                if (op instanceof InstFPU.FUCOMI_ST0_STj) {
                    InstFPU.FUCOMI_ST0_STj o = (InstFPU.FUCOMI_ST0_STj) op;
                    method.append("FPU.FUCOMI_ST0_STj(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOMI_ST0_STj) {
                    InstFPU.FCOMI_ST0_STj o = (InstFPU.FCOMI_ST0_STj) op;
                    method.append("FPU.FCOMI_ST0_STj(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU3_ea) {
                    Inst1.FPU3_ea o = (Inst1.FPU3_ea) op;
                    method.append("FPU.FPU_ESC3_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FILD_DWORD_INTEGER) {
                    InstFPU.FILD_DWORD_INTEGER o = (InstFPU.FILD_DWORD_INTEGER) op;
                    method.append("FPU.FILD_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISTTP32) {
                    InstFPU.FISTTP32 o = (InstFPU.FISTTP32) op;
                    method.append("FPU.FISTTP32(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIST_DWORD_INTEGER) {
                    InstFPU.FIST_DWORD_INTEGER o = (InstFPU.FIST_DWORD_INTEGER) op;
                    method.append("FPU.FIST_DWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FLD_EXTENDED_REAL) {
                    InstFPU.FLD_EXTENDED_REAL o = (InstFPU.FLD_EXTENDED_REAL) op;
                    method.append("FPU.FLD_EXTENDED_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSTP_EXTENDED_REAL) {
                    InstFPU.FSTP_EXTENDED_REAL o = (InstFPU.FSTP_EXTENDED_REAL) op;
                    method.append("FPU.FSTP_EXTENDED_REAL(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FADD_STi_ST0) {
                    InstFPU.FADD_STi_ST0 o = (InstFPU.FADD_STi_ST0) op;
                    method.append("FPU.FADD_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FMUL_STi_ST0) {
                    InstFPU.FMUL_STi_ST0 o = (InstFPU.FMUL_STi_ST0) op;
                    method.append("FPU.FMUL_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOM_STi) {
                    InstFPU.FCOM_STi o = (InstFPU.FCOM_STi) op;
                    method.append("FPU.FCOM_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUBR_STi_ST0) {
                    InstFPU.FSUBR_STi_ST0 o = (InstFPU.FSUBR_STi_ST0) op;
                    method.append("FPU.FSUBR_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUB_STi_ST0) {
                    InstFPU.FSUB_STi_ST0 o = (InstFPU.FSUB_STi_ST0) op;
                    method.append("FPU.FSUB_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIVR_STi_ST0) {
                    InstFPU.FDIVR_STi_ST0 o = (InstFPU.FDIVR_STi_ST0) op;
                    method.append("FPU.FDIVR_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIV_STi_ST0) {
                    InstFPU.FDIV_STi_ST0 o = (InstFPU.FDIV_STi_ST0) op;
                    method.append("FPU.FDIV_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU4_ea) {
                    Inst1.FPU4_ea o = (Inst1.FPU4_ea) op;
                    method.append("FPU.FPU_ESC4_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FADD_DOUBLE_REAL) {
                    InstFPU.FADD_DOUBLE_REAL o = (InstFPU.FADD_DOUBLE_REAL) op;
                    method.append("FPU.FADD_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FMUL_DOUBLE_REAL) {
                    InstFPU.FMUL_DOUBLE_REAL o = (InstFPU.FMUL_DOUBLE_REAL) op;
                    method.append("FPU.FMUL_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOM_DOUBLE_REAL) {
                    InstFPU.FCOM_DOUBLE_REAL o = (InstFPU.FCOM_DOUBLE_REAL) op;
                    method.append("FPU.FCOM_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUB_DOUBLE_REAL) {
                    InstFPU.FSUB_DOUBLE_REAL o = (InstFPU.FSUB_DOUBLE_REAL) op;
                    method.append("FPU.FSUB_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUBR_DOUBLE_REAL) {
                    InstFPU.FSUBR_DOUBLE_REAL o = (InstFPU.FSUBR_DOUBLE_REAL) op;
                    method.append("FPU.FSUBR_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIV_DOUBLE_REAL) {
                    InstFPU.FDIV_DOUBLE_REAL o = (InstFPU.FDIV_DOUBLE_REAL) op;
                    method.append("FPU.FDIV_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIVR_DOUBLE_REAL) {
                    InstFPU.FDIVR_DOUBLE_REAL o = (InstFPU.FDIVR_DOUBLE_REAL) op;
                    method.append("FPU.FDIVR_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FFREE_STi) {
                    InstFPU.FFREE_STi o = (InstFPU.FFREE_STi) op;
                    method.append("FPU.FFREE_STi(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FXCH_STi) {
                    InstFPU.FXCH_STi o = (InstFPU.FXCH_STi) op;
                    method.append("FPU.FXCH_STi(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FST_STi) {
                    InstFPU.FST_STi o = (InstFPU.FST_STi) op;
                    method.append("FPU.FST_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FUCOM_STi) {
                    InstFPU.FUCOM_STi o = (InstFPU.FUCOM_STi) op;
                    method.append("FPU.FUCOM_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU5_ea) {
                    Inst1.FPU5_ea o = (Inst1.FPU5_ea) op;
                    method.append("FPU.FPU_ESC5_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FLD_DOUBLE_REAL) {
                    InstFPU.FLD_DOUBLE_REAL o = (InstFPU.FLD_DOUBLE_REAL) op;
                    method.append("FPU.FLD_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISTTP64) {
                    InstFPU.FISTTP64 o = (InstFPU.FISTTP64) op;
                    method.append("FPU.FISTTP64(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FST_DOUBLE_REAL) {
                    InstFPU.FST_DOUBLE_REAL o = (InstFPU.FST_DOUBLE_REAL) op;
                    method.append("FPU.FST_DOUBLE_REAL(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FRSTOR) {
                    InstFPU.FRSTOR o = (InstFPU.FRSTOR) op;
                    method.append("FPU.FRSTOR(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FNSAVE) {
                    InstFPU.FNSAVE o = (InstFPU.FNSAVE) op;
                    method.append("FPU.FNSAVE(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FNSTSW) {
                    InstFPU.FNSTSW o = (InstFPU.FNSTSW) op;
                    method.append("FPU.FNSTSW(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FADD_STi_ST0) {
                    InstFPU.FADD_STi_ST0 o = (InstFPU.FADD_STi_ST0) op;
                    method.append("FPU.FADD_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FMUL_STi_ST0) {
                    InstFPU.FMUL_STi_ST0 o = (InstFPU.FMUL_STi_ST0) op;
                    method.append("FPU.FMUL_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOM_STi) {
                    InstFPU.FCOM_STi o = (InstFPU.FCOM_STi) op;
                    method.append("FPU.FCOM_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOMPP) {
                    method.append("FPU.FCOMPP();");
                    return true;
                }
                if (op instanceof InstFPU.FSUBR_STi_ST0) {
                    InstFPU.FSUBR_STi_ST0 o = (InstFPU.FSUBR_STi_ST0) op;
                    method.append("FPU.FSUBR_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FSUB_STi_ST0) {
                    InstFPU.FSUB_STi_ST0 o = (InstFPU.FSUB_STi_ST0) op;
                    method.append("FPU.FSUB_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIVR_STi_ST0) {
                    InstFPU.FDIVR_STi_ST0 o = (InstFPU.FDIVR_STi_ST0) op;
                    method.append("FPU.FDIVR_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FDIV_STi_ST0) {
                    InstFPU.FDIV_STi_ST0 o = (InstFPU.FDIV_STi_ST0) op;
                    method.append("FPU.FDIV_STi_ST0(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof Inst1.FPU6_ea) {
                    Inst1.FPU6_ea o = (Inst1.FPU6_ea) op;
                    method.append("FPU.FPU_ESC6_EA(");
                    method.append(o.rm);
                    method.append(",");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIADD_WORD_INTEGER) {
                    InstFPU.FIADD_WORD_INTEGER o = (InstFPU.FIADD_WORD_INTEGER) op;
                    method.append("FPU.FIADD_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIMUL_WORD_INTEGER) {
                    InstFPU.FIMUL_WORD_INTEGER o = (InstFPU.FIMUL_WORD_INTEGER) op;
                    method.append("FPU.FIMUL_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FICOM_WORD_INTEGER) {
                    InstFPU.FICOM_WORD_INTEGER o = (InstFPU.FICOM_WORD_INTEGER) op;
                    method.append("FPU.FICOM_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISUB_WORD_INTEGER) {
                    InstFPU.FISUB_WORD_INTEGER o = (InstFPU.FISUB_WORD_INTEGER) op;
                    method.append("FPU.FISUB_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISUBR_WORD_INTEGER) {
                    InstFPU.FISUBR_WORD_INTEGER o = (InstFPU.FISUBR_WORD_INTEGER) op;
                    method.append("FPU.FISUBR_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIDIV_WORD_INTEGER) {
                    InstFPU.FIDIV_WORD_INTEGER o = (InstFPU.FIDIV_WORD_INTEGER) op;
                    method.append("FPU.FIDIV_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIDIVR_WORD_INTEGER) {
                    InstFPU.FIDIVR_WORD_INTEGER o = (InstFPU.FIDIVR_WORD_INTEGER) op;
                    method.append("FPU.FIDIVR_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
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
                if (op instanceof InstFPU.FFREEP_STi) {
                    InstFPU.FFREEP_STi o = (InstFPU.FFREEP_STi) op;
                    method.append("FPU.FFREEP_STi(");
                    method.append(o.rm);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FST_STi) {
                    InstFPU.FST_STi o = (InstFPU.FST_STi) op;
                    method.append("FPU.FST_STi(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FNSTSW_AX) {
                    method.append("FPU.FNSTSW_AX();");
                    return true;
                }
                if (op instanceof InstFPU.FUCOMI_ST0_STj) {
                    InstFPU.FUCOMI_ST0_STj o = (InstFPU.FUCOMI_ST0_STj) op;
                    method.append("FPU.FUCOMI_ST0_STj(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FCOMI_ST0_STj) {
                    InstFPU.FCOMI_ST0_STj o = (InstFPU.FCOMI_ST0_STj) op;
                    method.append("FPU.FCOMI_ST0_STj(");
                    method.append(o.rm);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FILD_WORD_INTEGER) {
                    InstFPU.FILD_WORD_INTEGER o = (InstFPU.FILD_WORD_INTEGER) op;
                    method.append("FPU.FILD_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISTTP16) {
                    InstFPU.FISTTP16 o = (InstFPU.FISTTP16) op;
                    method.append("FPU.FISTTP16(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FIST_WORD_INTEGER) {
                    InstFPU.FIST_WORD_INTEGER o = (InstFPU.FIST_WORD_INTEGER) op;
                    method.append("FPU.FIST_WORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(",");
                    method.append(o.pop);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FBLD_PACKED_BCD) {
                    InstFPU.FBLD_PACKED_BCD o = (InstFPU.FBLD_PACKED_BCD) op;
                    method.append("FPU.FBLD_PACKED_BCD(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FILD_QWORD_INTEGER) {
                    InstFPU.FILD_QWORD_INTEGER o = (InstFPU.FILD_QWORD_INTEGER) op;
                    method.append("FPU.FILD_QWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FBSTP_PACKED_BCD) {
                    InstFPU.FBSTP_PACKED_BCD o = (InstFPU.FBSTP_PACKED_BCD) op;
                    method.append("FPU.FBSTP_PACKED_BCD(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
                if (op instanceof InstFPU.FISTP_QWORD_INTEGER) {
                    InstFPU.FISTP_QWORD_INTEGER o = (InstFPU.FISTP_QWORD_INTEGER) op;
                    method.append("FPU.FISTP_QWORD_INTEGER(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(");");
                    return true;
                }
            case 0xe0: // LOOPNZ
                if (op instanceof Inst1.Loopnz32) {
                    Inst1.Loopnz32 o = (Inst1.Loopnz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && ("+getNZ()+")", method);
                    return false;
                }
                if (op instanceof Inst1.Loopnz16) {
                    Inst1.Loopnz16 o = (Inst1.Loopnz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && ("+getNZ()+")", method);
                    return false;
                }
                break;
            case 0xe1: // LOOPZ
                if (op instanceof Inst1.Loopz32) {
                    Inst1.Loopz32 o = (Inst1.Loopz32) op;
                    method.append("CPU_Regs.reg_ecx.dword--;");
                    compile(o, "CPU_Regs.reg_ecx.dword!=0 && ("+getZ()+")", method);
                    return false;
                }
                if (op instanceof Inst1.Loopz16) {
                    Inst1.Loopz16 o = (Inst1.Loopz16) op;
                    method.append("CPU_Regs.reg_ecx.word(CPU_Regs.reg_ecx.word()-1);");
                    compile(o, "CPU_Regs.reg_ecx.word()!=0 && ("+getZ()+")", method);
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
                    method.append(",1)) {").append(preException).append("return RUNEXCEPTION();}CPU_Regs.reg_eax.low(IO.IO_ReadB(");
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
                    method.append(",2)) {").append(preException).append("return RUNEXCEPTION();}CPU_Regs.reg_eax.word(IO.IO_ReadW(");
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
                    method.append(",1)) {").append(preException).append("return RUNEXCEPTION();}IO.IO_WriteB(");
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
                    method.append(",2)) {").append(preException).append("return RUNEXCEPTION();}IO.IO_WriteW(");
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
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) {").append(preException).append("return RUNEXCEPTION();}CPU_Regs.reg_eax.low(IO.IO_ReadB(CPU_Regs.reg_edx.word()));");
                    return true;
                }
                break;
            case 0xed: // IN AX,DX
                if (op instanceof Inst1.InAxDx) {
                    Inst1.InAxDx o = (Inst1.InAxDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) {").append(preException).append("return RUNEXCEPTION();}CPU_Regs.reg_eax.word(IO.IO_ReadW(CPU_Regs.reg_edx.word()));");
                    return true;
                }
                break;
            case 0xee: // OUT DX,AL
            case 0x2ee:
                if (op instanceof Inst1.OutAlDx) {
                    Inst1.OutAlDx o = (Inst1.OutAlDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),1)) {").append(preException).append("return RUNEXCEPTION();}IO.IO_WriteB(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.low());");
                    return true;
                }
                break;
            case 0xef: // OUT DX,AX
                if (op instanceof Inst1.OutAxDx) {
                    Inst1.OutAxDx o = (Inst1.OutAxDx) op;
                    method.append("if (CPU.CPU_IO_Exception(CPU_Regs.reg_edx.word(),2)) {").append(preException).append("return RUNEXCEPTION();}IO.IO_WriteW(CPU_Regs.reg_edx.word(),CPU_Regs.reg_eax.word());");
                    return true;
                }
                break;
            case 0xf0: // LOCK
            case 0x2f0:
                return true;
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
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}Flags.FillFlags();CPU.CPU_HLT(CPU_Regs.reg_eip+");
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
                    method.append("Instructions.TESTB(");
                    method.append(o.val);
                    method.append(",");
                    method.append(nameGet8(o.earb));
                    method.append(");");
                    return true;
                }
                if (op instanceof Grp3.Testb_mem) {
                    Grp3.Testb_mem o = (Grp3.Testb_mem) op;
                    method.append("Instructions.TESTB(");
                    method.append(o.val);
                    method.append(",Memory.mem_readb(");
                    toStringValue(o.get_eaa, seg, method);
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa,~Memory.mem_readb(eaa));");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.Negb(Memory.mem_readb(eaa)));");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.MULB(Memory.mem_readb(eaa));");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.IMULB(Memory.mem_readb(eaa));");
                    return true;
                }
                if (op instanceof Grp3.DivAlEb_reg) {
                    Grp3.DivAlEb_reg o = (Grp3.DivAlEb_reg) op;
                    method.append("if (!Instructions.DIVB(");
                    method.append(nameGet8(o.earb));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.DivAlEb_mem) {
                    Grp3.DivAlEb_mem o = (Grp3.DivAlEb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (!Instructions.DIVB(Memory.mem_readb(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.IDivAlEb_reg) {
                    Grp3.IDivAlEb_reg o = (Grp3.IDivAlEb_reg) op;
                    method.append("if (!Instructions.IDIVB(");
                    method.append(nameGet8(o.earb));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.IDivAlEb_mem) {
                    Grp3.IDivAlEb_mem o = (Grp3.IDivAlEb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (!Instructions.IDIVB(Memory.mem_readb(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
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
                    toStringValue(o.get_eaa, seg, method);
                    method.append("));");
                    return true;
                }
                if (op instanceof Grp3.NotEw_reg) {
                    Grp3.NotEw_reg o = (Grp3.NotEw_reg) op;
                    nameSet16(o.earw, "~" + nameGet16(o.earw), method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NotEw_mem) {
                    Grp3.NotEw_mem o = (Grp3.NotEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa,~Memory.mem_readw(eaa));");
                    return true;
                }
                if (op instanceof Grp3.NegEw_reg) {
                    Grp3.NegEw_reg o = (Grp3.NegEw_reg) op;
                    nameSet16(o.earw, "Instructions.Negw(", nameGet16(o.earw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Grp3.NegEw_mem) {
                    Grp3.NegEw_mem o = (Grp3.NegEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.Negw(Memory.mem_readw(eaa)));");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.MULW(Memory.mem_readw(eaa));");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("Instructions.IMULW(Memory.mem_readw(eaa));");
                    return true;
                }
                if (op instanceof Grp3.DivAxEw_reg) {
                    Grp3.DivAxEw_reg o = (Grp3.DivAxEw_reg) op;
                    method.append("if (!Instructions.DIVW(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.DivAxEw_mem) {
                    Grp3.DivAxEw_mem o = (Grp3.DivAxEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (!Instructions.DIVW(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEw_reg) {
                    Grp3.IDivAxEw_reg o = (Grp3.IDivAxEw_reg) op;
                    method.append("if (!Instructions.IDIVW(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Grp3.IDivAxEw_mem) {
                    Grp3.IDivAxEw_mem o = (Grp3.IDivAxEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (!Instructions.IDIVW(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append("if (CPU.CPU_CLI()) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0xfb: // STI
            case 0x2fb:
                if (op instanceof Inst1.Sti) {
                    Inst1.Sti o = (Inst1.Sti) op;
                    method.append("if (CPU.CPU_STI()) {").append(preException).append("return RUNEXCEPTION();}");
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
                    inc((setFlags & o.sets())==0, 8, o.reg, method);
                    return true;
                }
                if (op instanceof Inst1.Incb_mem) {
                    Inst1.Incb_mem o = (Inst1.Incb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.INCB(Memory.mem_readb(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.Decb_reg) {
                    Inst1.Decb_reg o = (Inst1.Decb_reg) op;
                    dec((setFlags & o.sets())==0, 8, o.reg, method);
                    return true;
                }
                if (op instanceof Inst1.Decb_mem) {
                    Inst1.Decb_mem o = (Inst1.Decb_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writeb(eaa, Instructions.DECB(Memory.mem_readb(eaa)));");
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
                    inc((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                if (op instanceof Inst1.Incw_mem) {
                    Inst1.Incw_mem o = (Inst1.Incw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.INCW(Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.Decw_reg) {
                    Inst1.Decw_reg o = (Inst1.Decw_reg) op;
                    dec((setFlags & o.sets())==0, 16, o.reg, method);
                    return true;
                }
                if (op instanceof Inst1.Decw_mem) {
                    Inst1.Decw_mem o = (Inst1.Decw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, Instructions.DECW(Memory.mem_readw(eaa)));");
                    return true;
                }
                if (op instanceof Inst1.CallEv_reg) {
                    Inst1.CallEv_reg o = (Inst1.CallEv_reg) op;
                    method.append(";CPU.CPU_Push16((CPU_Regs.reg_eip+");method.append(o.eip_count);method.append(") & 0xFFFF);");
                    method.append("CPU_Regs.reg_eip=");
                    method.append(nameGet16(o.earw));
                    method.append(";return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.CallEv_mem) {
                    Inst1.CallEv_mem o = (Inst1.CallEv_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val = Memory.mem_readw(eaa);CPU.CPU_Push16((CPU_Regs.reg_eip+");method.append(o.eip_count);method.append(") & 0xFFFF);CPU_Regs.reg_eip = val;return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.CallEp) {
                    Inst1.CallEp o = (Inst1.CallEp) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa+2);Flags.FillFlags();CPU.CPU_CALL(false,val2,val,(CPU_Regs.reg_eip+");
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
                    memory_start(o.get_eaa, seg, method);
                    method.append("CPU_Regs.reg_eip=Memory.mem_readw(eaa);return Constants.BR_Jump;");
                    return false;
                }
                if (op instanceof Inst1.JmpEp) {
                    Inst1.JmpEp o = (Inst1.JmpEp) op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readw(eaa);");
                    declareVal2(method);
                    method.append("val2=Memory.mem_readw(eaa+2);Flags.FillFlags();CPU.CPU_JMP(false,val2,val,(CPU_Regs.reg_eip+");
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
                    toStringValue(o.get_eaa, seg, method);
                    method.append("));");
                    return true;
                }
                break;
            case 0x100: // GRP 6 Exxx
            case 0x300:
                if (op instanceof Inst2.Sldt_reg) {
                    Inst2.Sldt_reg o = (Inst2.Sldt_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    nameSet16(o.earw, "CPU.CPU_SLDT()", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.Sldt_mem) {
                    Inst2.Sldt_mem o = (Inst2.Sldt_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, CPU.CPU_SLDT());");
                    return true;
                }
                if (op instanceof Inst2.Str_reg) {
                    Inst2.Str_reg o = (Inst2.Str_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    nameSet16(o.earw, "CPU.CPU_STR()", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.Str_mem) {
                    Inst2.Str_mem o = (Inst2.Str_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    method.append("Memory.mem_writew(eaa, CPU.CPU_STR());");
                    return true;
                }
                if (op instanceof Inst2.Lldt_reg) {
                    Inst2.Lldt_reg o = (Inst2.Lldt_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}if (CPU.CPU_LLDT(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst2.Lldt_mem) {
                    Inst2.Lldt_mem o = (Inst2.Lldt_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}");
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_LLDT(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst2.Ltr_reg) {
                    Inst2.Ltr_reg o = (Inst2.Ltr_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}if (CPU.CPU_LTR(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                if (op instanceof Inst2.Ltr_mem) {
                    Inst2.Ltr_mem o = (Inst2.Ltr_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;if (CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}");
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.CPU_LTR(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    method.append("CPU.CPU_VERR(Memory.mem_readw(eaa));");
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
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    method.append("CPU.CPU_VERW(Memory.mem_readw(eaa));");
                    return true;
                }
                break;
            case 0x101: // Group 7 Ew
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
                if (op instanceof Inst2.Lgdt_mem) {
                    Inst2.Lgdt_mem o = (Inst2.Lgdt_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}CPU.CPU_LGDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2) & 0xFFFFFF);");
                    return true;
                }
                if (op instanceof Inst2.Lidt_mem) {
                    Inst2.Lidt_mem o = (Inst2.Lidt_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}CPU.CPU_LIDT(Memory.mem_readw(eaa),Memory.mem_readd(eaa + 2) & 0xFFFFFF);");
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
                    method.append("if (CPU.CPU_LMSW(Memory.mem_readw(eaa))) {").append(preException).append("return RUNEXCEPTION();}");
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
                if (op instanceof Inst2.Smsw_reg) {
                    Inst2.Smsw_reg o = (Inst2.Smsw_reg) op;
                    nameSet16(o.earw, "CPU.CPU_SMSW()", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.Lmsw_reg) {
                    Inst2.Lmsw_reg o = (Inst2.Lmsw_reg) op;
                    method.append("if (CPU.CPU_LMSW(");
                    method.append(nameGet16(o.earw));
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x102: // LAR Gw,Ew
                if (op instanceof Inst2.LarGwEw_reg) {
                    Inst2.LarGwEw_reg o = (Inst2.LarGwEw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    nameSet16(o.rw, "CPU.CPU_LAR(", nameGet16(o.earw), ",", nameGet16(o.rw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.LarGwEw_mem) {
                    Inst2.LarGwEw_mem o = (Inst2.LarGwEw_mem) op;
                    memory_start(o.get_eaa, seg, method);
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    nameSet16(o.rw, "CPU.CPU_LAR(Memory.mem_readw(eaa),", nameGet16(o.rw), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x103: // LSL Gw,Ew
                if (op instanceof Inst2.LslGwEw_reg) {
                    Inst2.LslGwEw_reg o = (Inst2.LslGwEw_reg) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    nameSet16(o.rw, "CPU.CPU_LSL(", nameGet16(o.earw), ",", nameGet16(o.rw), ")", method);
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.LslGwEw_mem) {
                    Inst2.LslGwEw_mem o = (Inst2.LslGwEw_mem) op;
                    method.append("if ((CPU_Regs.flags & CPU_Regs.VM)!=0 || (!CPU.cpu.pmode)) return Constants.BR_Illegal;");
                    memory_start(o.get_eaa, seg, method);
                    nameSet16(o.rw, "CPU.CPU_LSL(Memory.mem_readw(eaa),", nameGet16(o.rw), ")", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x106: // CLTS
            case 0x306:
                if (op instanceof Inst2.Clts) {
                    Inst2.Clts o = (Inst2.Clts) op;
                    // :TODO: this is a bug in the compiler, ~ and a constant int does not work so I added the (int) cast which fixes it
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}CPU.cpu.cr0=CPU.cpu.cr0 & (~(int)CPU.CR0_TASKSWITCH);");
                    return true;
                }

                break;
            case 0x108: // INVD
            case 0x308:
                if (op instanceof Inst2.Invd) {
                    Inst2.Invd o = (Inst2.Invd) op;
                    method.append("if (CPU.cpu.pmode && CPU.cpu.cpl!=0) {").append(preException).append("return EXCEPTION(CPU.EXCEPTION_GP);}");
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
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
                    method.append(")) {").append(preException).append("return RUNEXCEPTION();}");
                    return true;
                }
                break;
            case 0x131: // RDTSC
            case 0x331:
                if (op instanceof Inst2.Rdtsc) {
                    Inst2.Rdtsc o = (Inst2.Rdtsc) op;
                    method.append("if (CPU.CPU_ArchitectureType<CPU.CPU_ARCHTYPE_PENTIUM) return Constants.BR_Illegal;");
                    declareLongVal(method);
                    method.append("lval=(long)(Pic.PIC_FullIndex()*(double) (CPU.CPU_CycleAutoAdjust?70000:CPU.CPU_CycleMax));CPU_Regs.reg_edx.dword=(int)(lval>>>32);CPU_Regs.reg_eax.dword=(int)lval;");
                    return true;
                }
                break;
            case 0x140: // CMOVO
            case 0x141: // CMOVNO
            case 0x142: // CMOVB
            case 0x143: // CMOVNB
            case 0x144: // CMOVZ
            case 0x145: // CMOVNZ
            case 0x146: // CMOVBE
            case 0x147: // CMOVNBE
            case 0x148: // CMOVS
            case 0x149: // CMOVNS
            case 0x14a: // CMOVP
            case 0x14b: // CMOVNP
            case 0x14c: // CMOVL
            case 0x14d: // CMOVNL
            case 0x14e: // CMOVLE
            case 0x14f: // CMOVNLE
                if (op instanceof Inst2.ConditionalMov_reg) {
                    if (op instanceof Inst2.ConditionalMov_o_reg)
                        method.append("if (Flags.TFLG_O()) ");
                    else if (op instanceof Inst2.ConditionalMov_no_reg)
                        method.append("if (Flags.TFLG_NO()) ");
                    else if (op instanceof Inst2.ConditionalMov_b_reg)
                        method.append("if (Flags.TFLG_B()) ");
                    else if (op instanceof Inst2.ConditionalMov_nb_reg)
                        method.append("if (Flags.TFLG_NB()) ");
                    else if (op instanceof Inst2.ConditionalMov_z_reg)
                        method.append("if (Flags.TFLG_Z()) ");
                    else if (op instanceof Inst2.ConditionalMov_nz_reg)
                        method.append("if (Flags.TFLG_NZ()) ");
                    else if (op instanceof Inst2.ConditionalMov_be_reg)
                        method.append("if (Flags.TFLG_BE()) ");
                    else if (op instanceof Inst2.ConditionalMov_nbe_reg)
                        method.append("if (Flags.TFLG_NBE()) ");
                    else if (op instanceof Inst2.ConditionalMov_s_reg)
                        method.append("if (Flags.TFLG_S()) ");
                    else if (op instanceof Inst2.ConditionalMov_ns_reg)
                        method.append("if (Flags.TFLG_NS()) ");
                    else if (op instanceof Inst2.ConditionalMov_p_reg)
                        method.append("if (Flags.TFLG_P()) ");
                    else if (op instanceof Inst2.ConditionalMov_np_reg)
                        method.append("if (Flags.TFLG_NP()) ");
                    else if (op instanceof Inst2.ConditionalMov_l_reg)
                        method.append("if (Flags.TFLG_L()) ");
                    else if (op instanceof Inst2.ConditionalMov_nl_reg)
                        method.append("if (Flags.TFLG_NL()) ");
                    else if (op instanceof Inst2.ConditionalMov_le_reg)
                        method.append("if (Flags.TFLG_LE()) ");
                    else if (op instanceof Inst2.ConditionalMov_nle_reg)
                        method.append("if (Flags.TFLG_NLE()) ");
                    Inst2.ConditionalMov_reg o = (Inst2.ConditionalMov_reg)op;
                    nameSet16(o.gw, nameGet16(o.ew), method);
                    method.append(";");
                    return true;
                } else if (op instanceof Inst2.ConditionalMov_mem) {
                    Inst2.ConditionalMov_mem o = (Inst2.ConditionalMov_mem)op;
                    memory_start(o.get_eaa, seg, method);
                    declareVal(method);
                    method.append("val=Memory.mem_readd(eaa);");
                    if (op instanceof Inst2.ConditionalMov_o_mem)
                        method.append("if (Flags.TFLG_O()) ");
                    else if (op instanceof Inst2.ConditionalMov_no_mem)
                        method.append("if (Flags.TFLG_NO()) ");
                    else if (op instanceof Inst2.ConditionalMov_b_mem)
                        method.append("if (Flags.TFLG_B()) ");
                    else if (op instanceof Inst2.ConditionalMov_nb_mem)
                        method.append("if (Flags.TFLG_NB()) ");
                    else if (op instanceof Inst2.ConditionalMov_z_mem)
                        method.append("if (Flags.TFLG_Z()) ");
                    else if (op instanceof Inst2.ConditionalMov_nz_mem)
                        method.append("if (Flags.TFLG_NZ()) ");
                    else if (op instanceof Inst2.ConditionalMov_be_mem)
                        method.append("if (Flags.TFLG_BE()) ");
                    else if (op instanceof Inst2.ConditionalMov_nbe_mem)
                        method.append("if (Flags.TFLG_NBE()) ");
                    else if (op instanceof Inst2.ConditionalMov_s_mem)
                        method.append("if (Flags.TFLG_S()) ");
                    else if (op instanceof Inst2.ConditionalMov_ns_mem)
                        method.append("if (Flags.TFLG_NS()) ");
                    else if (op instanceof Inst2.ConditionalMov_p_mem)
                        method.append("if (Flags.TFLG_P()) ");
                    else if (op instanceof Inst2.ConditionalMov_np_mem)
                        method.append("if (Flags.TFLG_NP()) ");
                    else if (op instanceof Inst2.ConditionalMov_l_mem)
                        method.append("if (Flags.TFLG_L()) ");
                    else if (op instanceof Inst2.ConditionalMov_nl_mem)
                        method.append("if (Flags.TFLG_NL()) ");
                    else if (op instanceof Inst2.ConditionalMov_le_mem)
                        method.append("if (Flags.TFLG_LE()) ");
                    else if (op instanceof Inst2.ConditionalMov_nle_mem)
                        method.append("if (Flags.TFLG_NLE()) ");
                    nameSet16(o.gw, "val", method);
                    method.append(";");
                    return true;
                }
                break;
            case 0x180: // JO
                if (op instanceof Inst2.JumpCond16_w_o) {
                    Inst2.JumpCond16_w_o o = (Inst2.JumpCond16_w_o) op;
                    compile(o, getO(), method);
                    return false;
                }
                break;
            case 0x181: // JNO
                if (op instanceof Inst2.JumpCond16_w_no) {
                    Inst2.JumpCond16_w_no o = (Inst2.JumpCond16_w_no) op;
                    compile(o, getNO(), method);
                    return false;
                }
                break;
            case 0x182: // JB
                if (op instanceof Inst2.JumpCond16_w_b) {
                    Inst2.JumpCond16_w_b o = (Inst2.JumpCond16_w_b) op;
                    compile(o, getB(), method);
                    return false;
                }
                break;
            case 0x183: // JNB
                if (op instanceof Inst2.JumpCond16_w_nb) {
                    Inst2.JumpCond16_w_nb o = (Inst2.JumpCond16_w_nb) op;
                    compile(o, getNB(), method);
                    return false;
                }
                break;
            case 0x184: // JZ
                if (op instanceof Inst2.JumpCond16_w_z) {
                    Inst2.JumpCond16_w_z o = (Inst2.JumpCond16_w_z) op;
                    compile(o, getZ(), method);
                    return false;
                }
                break;
            case 0x185: // JNZ
                if (op instanceof Inst2.JumpCond16_w_nz) {
                    Inst2.JumpCond16_w_nz o = (Inst2.JumpCond16_w_nz) op;
                    compile(o, getNZ(), method);
                    return false;
                }
                break;
            case 0x186: // JBE
                if (op instanceof Inst2.JumpCond16_w_be) {
                    Inst2.JumpCond16_w_be o = (Inst2.JumpCond16_w_be) op;
                    compile(o, getBE(), method);
                    return false;
                }
                break;
            case 0x187: // JNBE
                if (op instanceof Inst2.JumpCond16_w_nbe) {
                    Inst2.JumpCond16_w_nbe o = (Inst2.JumpCond16_w_nbe) op;
                    compile(o, getNBE(), method);
                    return false;
                }
                break;
            case 0x188: // JS
                if (op instanceof Inst2.JumpCond16_w_s) {
                    Inst2.JumpCond16_w_s o = (Inst2.JumpCond16_w_s) op;
                    compile(o, getS(), method);
                    return false;
                }
                break;
            case 0x189: // JNS
                if (op instanceof Inst2.JumpCond16_w_ns) {
                    Inst2.JumpCond16_w_ns o = (Inst2.JumpCond16_w_ns) op;
                    compile(o, getNS(), method);
                    return false;
                }
                break;
            case 0x18a: // JP
                if (op instanceof Inst2.JumpCond16_w_p) {
                    Inst2.JumpCond16_w_p o = (Inst2.JumpCond16_w_p) op;
                    compile(o, getP(), method);
                    return false;
                }
                break;
            case 0x18b: // JNP
                if (op instanceof Inst2.JumpCond16_w_np) {
                    Inst2.JumpCond16_w_np o = (Inst2.JumpCond16_w_np) op;
                    compile(o, getNP(), method);
                    return false;
                }
                break;
            case 0x18c: // JL
                if (op instanceof Inst2.JumpCond16_w_l) {
                    Inst2.JumpCond16_w_l o = (Inst2.JumpCond16_w_l) op;
                    compile(o, getL(), method);
                    return false;
                }
                break;
            case 0x18d: // JNL
                if (op instanceof Inst2.JumpCond16_w_nl) {
                    Inst2.JumpCond16_w_nl o = (Inst2.JumpCond16_w_nl) op;
                    compile(o, getNL(), method);
                    return false;
                }
                break;
            case 0x18e: // JLE
                if (op instanceof Inst2.JumpCond16_w_le) {
                    Inst2.JumpCond16_w_le o = (Inst2.JumpCond16_w_le) op;
                    compile(o, getLE(), method);
                    return false;
                }
                break;
            case 0x18f: // JNLE
                if (op instanceof Inst2.JumpCond16_w_nle) {
                    Inst2.JumpCond16_w_nle o = (Inst2.JumpCond16_w_nle) op;
                    compile(o, getNLE(), method);
                    return false;
                }
                break;
            case 0x190: // SETO
            case 0x390:
                if (op instanceof Inst2.SETcc_reg_o) {
                    Inst2.SETcc_reg_o o = (Inst2.SETcc_reg_o) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_O()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_o) {
                    Inst2.SETcc_mem_o o = (Inst2.SETcc_mem_o) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_O()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x191: // SETNO
            case 0x391:
                if (op instanceof Inst2.SETcc_reg_no) {
                    Inst2.SETcc_reg_no o = (Inst2.SETcc_reg_no) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NO()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_no) {
                    Inst2.SETcc_mem_no o = (Inst2.SETcc_mem_no) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NO()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x192: // SETB
            case 0x392:
                if (op instanceof Inst2.SETcc_reg_b) {
                    Inst2.SETcc_reg_b o = (Inst2.SETcc_reg_b) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_B()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_b) {
                    Inst2.SETcc_mem_b o = (Inst2.SETcc_mem_b) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_B()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x193: // SETNB
            case 0x393:
                if (op instanceof Inst2.SETcc_reg_nb) {
                    Inst2.SETcc_reg_nb o = (Inst2.SETcc_reg_nb) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NB()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nb) {
                    Inst2.SETcc_mem_nb o = (Inst2.SETcc_mem_nb) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NB()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x194: // SETZ
            case 0x394:
                if (op instanceof Inst2.SETcc_reg_z) {
                    Inst2.SETcc_reg_z o = (Inst2.SETcc_reg_z) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_Z()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_z) {
                    Inst2.SETcc_mem_z o = (Inst2.SETcc_mem_z) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_Z()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x195: // SETNZ
            case 0x395:
                if (op instanceof Inst2.SETcc_reg_nz) {
                    Inst2.SETcc_reg_nz o = (Inst2.SETcc_reg_nz) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NZ()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nz) {
                    Inst2.SETcc_mem_nz o = (Inst2.SETcc_mem_nz) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NZ()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x196: // SETBE
            case 0x396:
                if (op instanceof Inst2.SETcc_reg_be) {
                    Inst2.SETcc_reg_be o = (Inst2.SETcc_reg_be) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_BE()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_be) {
                    Inst2.SETcc_mem_be o = (Inst2.SETcc_mem_be) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_BE()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x197: // SETNBE
            case 0x397:
                if (op instanceof Inst2.SETcc_reg_nbe) {
                    Inst2.SETcc_reg_nbe o = (Inst2.SETcc_reg_nbe) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NBE()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_nbe) {
                    Inst2.SETcc_mem_nbe o = (Inst2.SETcc_mem_nbe) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NBE()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x198: // SETS
            case 0x398:
                if (op instanceof Inst2.SETcc_reg_s) {
                    Inst2.SETcc_reg_s o = (Inst2.SETcc_reg_s) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_S()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_s) {
                    Inst2.SETcc_mem_s o = (Inst2.SETcc_mem_s) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_S()) ? 1 : 0);");
                    return true;
                }
                break;
            case 0x199: // SETNS
            case 0x399:
                if (op instanceof Inst2.SETcc_reg_ns) {
                    Inst2.SETcc_reg_ns o = (Inst2.SETcc_reg_ns) op;
                    method.append(nameSet8(o.earb, "(Flags.TFLG_NS()) ? 1 : 0"));
                    method.append(";");
                    return true;
                }
                if (op instanceof Inst2.SETcc_mem_ns) {
                    Inst2.SETcc_mem_ns o = (Inst2.SETcc_mem_ns) op;
                    method.append("Memory.mem_writeb(");
                    toStringValue(o.get_eaa, seg, method);
                    method.append(", (Flags.TFLG_NS()) ? 1 : 0);");
                    return true;
                }
                break;
            default:
                return Compiler2.compile_op(op, setFlags, method, preException, seg);

        }
        return true;
    }

    static private ClassPool pool = ClassPool.getDefault();
    static java.security.MessageDigest md;

    static {
        pool.importPackage("jdos.cpu.core_dynamic");
        pool.importPackage("jdos.cpu");
        pool.importPackage("jdos.fpu");
        pool.importPackage("jdos.hardware");
        pool.importPackage("jdos.util");
        pool.importPackage("jdos.cpu.core_normal");
        pool.importPackage("jdos.cpu.core_share");
        pool.insertClassPath(new ClassPath() {
            public InputStream openClassfile(String s) throws NotFoundException {
                if (s.startsWith("jdos.")) {
                    s = "/" + s.replace('.', '/') + ".class";
                    return Dosbox.class.getResourceAsStream(s.substring(6));
                }
                return null;
            }

            public URL find(String s) {
                if (s.startsWith("jdos.")) {
                    s = "/" + s.replace('.', '/') + ".class";
                    return Dosbox.class.getResource(s.substring(6));
                }
                return null;
            }

            public void close() {
            }
        });
        try {
            md = java.security.MessageDigest.getInstance("MD5");
        } catch (Exception e) {

        }
    }

    static private int count = 0;

    static private Op compileMethod(Op op, StringBuilder method, boolean jump) {
        //System.out.println(method.toString());
        try {
            String className = "jdos.cpu.core_dynamic.CacheBlock" + (count++);
            // :TODO: research using a new pool for each block since the classes don't need to see each other
            CtClass codeBlock = pool.makeClass(className);
            codeBlock.setSuperclass(pool.getCtClass("jdos.cpu.core_dynamic.Op"));
            if (!jump)
                method.append("return Constants.BR_Normal;");
            method.append("}");
            CtMethod m = CtNewMethod.make("public int call() {" + method.toString(), codeBlock);
            codeBlock.addMethod(m);

            Op o = op;
            int cycle = 0;
            while (o!=null) {
                cycle = o.cycle;
                o = o.next;
            }
            if (cycle<1) {
                cycle=1;
            }
            CtConstructor c = CtNewConstructor.make("public "+codeBlock.getSimpleName()+"(){this.cycle="+cycle+";}", codeBlock);
            codeBlock.addConstructor(c);

            // Make the dynamic class belong to its own class loader so that when we
            // release the decoder block the class and class loader will be unloaded
            ClassLoader cl = new URLClassLoader(
                    new URL[]{ Compiler.class.getProtectionDomain().getCodeSource().getLocation()},
                    codeBlock.getClass().getClassLoader());

            // Create second copy of Compiler class inside custom classloader as "neighbour".
            // CtClass.toClass(neighbour) does not cause illegal reflective access in JDK17.
            Class clazz = codeBlock.toClass(cl.loadClass(Compiler.class.getName()));
            Op compiledCode = (Op) clazz.newInstance();
            codeBlock.detach();
            if (saveClasses) {
                if (op instanceof DecodeBlock) {
                    DecodeBlock block = (DecodeBlock)op;
                    String classBody =
                            "package " + codeBlock.getPackageName()  + ";\n\n" +
                            "import jdos.cpu.core_dynamic.*;\n" +
                            "import jdos.cpu.*;\n" +
                            "import jdos.fpu.*;\n" +
                            "import jdos.hardware.*;\n" +
                            "import jdos.util.*;\n" +
                            "import jdos.cpu.core_normal.*;\n" +
                            "import jdos.cpu.core_share.*;\n\n" +
                            "public final class " + codeBlock.getSimpleName() + " extends " + codeBlock.getSuperclass().getName() + " {\n" +
                            "   public int call() { " + method + "} \n" +
                            "}";
                    Loader.add(codeBlock.getName(), codeBlock.toBytecode(), block.codeStart, getOpCode(block.codeStart, block.codeLen), classBody);
                } else {
                    Log.exit("Tried to save an incomplete code block");
                }
            }
            return compiledCode;
        } catch (Exception e) {
            System.out.println(method.toString());
            e.printStackTrace();
            if (thowException)
                throw new RuntimeException("Failed to compile");
        }
        return null;
    }

    final public static Section.SectionFunction Compiler_Init = new Section.SectionFunction() {
        public void call(Section newconfig) {
            Section_prop section=(Section_prop)newconfig;
            DecodeBlock.compileThreshold = section.Get_int("threshold");
            min_block_size = section.Get_int("min_block_size");
        }
    };
}
