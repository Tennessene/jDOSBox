package jdos.cpu.core_dynamic;

import jdos.Dosbox;
import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.ModifiedDecode;
import jdos.hardware.RAM;
import jdos.misc.Log;
import jdos.misc.setup.Config;

public class Decoder extends Inst1 {
    public static final Decode[] ops = new Decode[1024];

    static {
        Decode not_handled = new Decode() {
            public int call(Op prev) {
                prev.next = new Op() {
                    public int call() {
                        CPU.CPU_Exception(6,0);
                        return Constants.BR_Jump;
                    }
                    public boolean throwsException() {return true;}
                    public boolean accessesMemory() {return false;}
                    public boolean usesEip() {return false;}
                    public boolean setsEip() {return false;}
                };
                return RESULT_JUMP;
            }
        };
        for (int i=0;i<ops.length;i++)
            ops[i] = not_handled;
    }

    abstract public static class SegOp extends Op {
        public Op op;
        public void reset() {
            Core.base_ds=CPU_Regs.reg_dsPhys.dword;
            Core.base_ss=CPU_Regs.reg_ssPhys.dword;
            Core.base_val_ds=ds;
        }

        public int sets() { return op.sets(); }
        public int gets() { return op.gets(); }

        public boolean returnsIllegal() {return op.returnsIllegal();}
        public int setsSeg() {return op.setsSeg();}
        public boolean throwsException() {return op.throwsException();}
        public boolean accessesMemory() {return op.accessesMemory();}
        public boolean usesEip() {return op.accessesMemory();}
        public boolean setsEip() {return op.setsEip();}
    }

    final public static class HandledSegChange extends Op {
        public int call() {
            Core.base_ds=CPU_Regs.reg_dsPhys.dword;
            Core.base_ss=CPU_Regs.reg_ssPhys.dword;
            Core.base_val_ds=ds;
            return next.call();
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }
    private static class StartDecode extends Op {
        public int call() {
            return Constants.BR_Normal;
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    public static boolean logit = false;
    private static class LogOp extends Op {
        public LogOp(Op op) {
            this.op = op;
            this.cycle = op.cycle;
        }
        Op op;
        public int call() {
            if (logit) {
                traceCount++;
                if ((traceCount % 1000)==0) {
                    int ii=0;
                }
                System.out.println(traceCount+" "+Integer.toHexString(CPU_Regs.reg_csPhys.dword) + ":" + Integer.toHexString(CPU_Regs.reg_eip) + " " + Integer.toHexString(op.c) + " " + op.description()+" (eax=0x"+Integer.toHexString(CPU_Regs.reg_eax.dword)+" ecx=0x"+Integer.toHexString(CPU_Regs.reg_ecx.dword)+" edx=0x"+Integer.toHexString(CPU_Regs.reg_edx.dword)+" ebx=0x"+Integer.toHexString(CPU_Regs.reg_ebx.dword)+" esp=0x"+Integer.toHexString(CPU_Regs.reg_esp.dword)+" ebp=0x"+Integer.toHexString(CPU_Regs.reg_ebp.dword)+" esi=0x"+Integer.toHexString(CPU_Regs.reg_esi.dword)+" edi=0x"+Integer.toHexString(CPU_Regs.reg_edi.dword)+")");
            }
            return op.call();
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    static public class JumpOp extends Op {
        int offset;

        public JumpOp(Op op) {
            next = op.next;
            c = op.c;
            eip_count = op.eip_count;
            if (op.c==0xe9) {
                Inst1.JmpJw jump = (Inst1.JmpJw)op;
                if (jump.addip>0) {
                    offset = jump.addip;
                } else {
                    Log.exit("Unexpected value");
                }
            } else if (op.c==0xeb) {
                Inst1.JmpJb jump = (Inst1.JmpJb)op;
                if (jump.addip>0) {
                    offset = jump.addip;
                } else  {
                    Log.exit("Unexpected value");
                }
            }
        }

        public String description() {return "JMP "+offset;}

        public int call() {
            CPU_Regs.reg_eip+=eip_count+offset;
            return next.call();
        }

        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
    }

    public static class HandledDecode extends Op {
        public int call() {
            return Constants.BR_Jump;
        }
        public boolean throwsException() {return false;}
        public boolean accessesMemory() {return false;}
        public boolean usesEip() {return false;}
        public boolean setsEip() {return false;}
    }

    static {
        Prefix_none.init(ops);
        Prefix_0f.init(ops);
        Prefix_66.init(ops);
        Prefix_66_0f.init(ops);
    }

    public static final boolean removeRedundantSegs = false;
    public static final boolean log = false;
    public static final boolean trace = false; // useful if the compiler isn't enabled
    public static int traceCount = 0;

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
        int result = 0;

        if (CPU.cpu.code.big) {
            opcode_index=0x200;
            prefixes=1;
            EA16 = false;
        } else {
            opcode_index=0;
            prefixes=0;
            EA16 = true;
        }

        Op op = new StartDecode();
        Op start_op = op;
        Op begin_op = start_op;
        int op_start = decode.code;
        boolean seg_changed = false;
        int opcode = 0;
        int count = 0;
        int cycles = 0;
        int previousSeg = -1;
        Op previousSegParent = null;

        try {
            while (max_opcodes-->0 && result==0) {
                decode.cycles++;
                decode.op_start=decode.code;
                decode.modifiedAlot = false;

                opcode=opcode_index+decode_fetchb();
                result = ops[opcode].call(op);
                if (log) {
                    if (op.next!=null)
                        System.out.print(Integer.toHexString(CPU_Regs.reg_csPhys.dword)+":"+Integer.toHexString(op_start)+" "+Integer.toHexString(opcode)+" "+op.next.getClass().getName());
                    else
                        System.out.print(Integer.toHexString(CPU_Regs.reg_csPhys.dword)+":"+Integer.toHexString(op_start)+" "+Integer.toHexString(opcode)+" ");
                }
                if (decode.modifiedAlot) {
                    result = RESULT_ILLEGAL_INSTRUCTION;
                    break;
                }
                count+=(decode.code - decode.op_start);
                if (result == RESULT_CONTINUE) {
                    result = RESULT_HANDLED;
                    max_opcodes++;
                    continue;
                }
                op = op.next;
                op.c = opcode;
                ++cycles;

                if (result == RESULT_CONTINUE_SEG) {
                    // This will remove redundant segment prefixes and remove the op that returns the base back to DS
                    //
                    // The following 2 instructions, each with a segment prefix used to become 6 oline
                    //
                    // Core.DO_PREFIX_SEG_ES();
                    // Memory.mem_writew(Core.base_ds+(CPU_Regs.reg_ebx.word()), 0);
                    // Core.base_ds= CPU_Regs.reg_dsPhys.dword;Core.base_ss=CPU_Regs.reg_ssPhys.dword;Core.base_val_ds=CPU_Regs.ds;
                    // Core.DO_PREFIX_SEG_ES();
                    // Memory.mem_writew(Core.base_ds+((CPU_Regs.reg_ebx.word()+2) & 0xFFFF), 0);
                    // Core.base_ds= CPU_Regs.reg_dsPhys.dword;Core.base_ss=CPU_Regs.reg_ssPhys.dword;Core.base_val_ds=CPU_Regs.ds;
                    //
                    // Now it will be 4 ops
                    //
                    // Core.DO_PREFIX_SEG_ES();
                    // Memory.mem_writew(Core.base_ds+(CPU_Regs.reg_ebx.word()), 0);
                    // Memory.mem_writew(Core.base_ds+((CPU_Regs.reg_ebx.word()+2) & 0xFFFF), 0);
                    // Core.base_ds= CPU_Regs.reg_dsPhys.dword;Core.base_ss=CPU_Regs.reg_ssPhys.dword;Core.base_val_ds=CPU_Regs.ds;
                    //
                    // or 5 ops if the recompiler is on
                    //
                    // Core.DO_PREFIX_SEG_ES();
                    // Memory.mem_writew(Core.base_ds+(CPU_Regs.reg_ebx.word()), 0);
                    // Core.DO_PREFIX_SEG_ES();
                    // Memory.mem_writew(Core.base_ds+((CPU_Regs.reg_ebx.word()+2) & 0xFFFF), 0);
                    // Core.base_ds= CPU_Regs.reg_dsPhys.dword;Core.base_ss=CPU_Regs.reg_ssPhys.dword;Core.base_val_ds=CPU_Regs.ds;
                    //
                    // This only works for instructions with prefixes that are back to back
                    if (removeRedundantSegs) {
                        if (previousSegParent != null && previousSeg == opcode) {

                            if (Dosbox.allPrivileges) {
                                // we can loose the HandleSegChange block, be we need this seg instruction for the recompiler
                                previousSegParent.next = op; // This will drop HandleSegChange
                                begin_op = previousSegParent.next;
                            } else {
                                op = previousSegParent;
                                begin_op = previousSegParent;
                            }
                            max_opcodes++;
                        }
                        previousSeg = opcode;
                    }
                    result = RESULT_HANDLED;
                    max_opcodes++;
                    seg_changed = true;
                    if (log)
                        System.out.println();
                    continue;
                }
                if (removeRedundantSegs)
                    previousSegParent = null;
                begin_op = op;
                op.eip_count = count;
                if (result == RESULT_ANOTHER) {
                    result = RESULT_HANDLED;
                    max_opcodes++;
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

                if (seg_changed && result==0) {
                    if (removeRedundantSegs) {
                        if ((op.setsSeg() & Op.FROM_MEMORY)==0) {
                            previousSegParent = op;
                        }
                    }
                    seg_changed = false;
                    op.next = new HandledSegChange();
                    op = op.next;
                    op.c = -1;
                    op.cycle = cycles;
                    begin_op = op;
                }
                count = 0;
                if (log)
                    System.out.println();
            }
        } catch (PageFaultException e) {
            if (decode.code -decode.op_start + count == 0) {
                result = RESULT_HANDLED; // begining of op code started on next page
            } else {
                result = RESULT_ILLEGAL_INSTRUCTION; // op code spanned two pages, run with normal core in case of page fault
            }
        }
        if (log) {
            print(start_op.next);
            System.out.println();
        }

        Cache.cache_closeblock();
        switch (result) {
            case RESULT_HANDLED:
                op.next = new HandledDecode();
                op.cycle = cycles;
                op = op.next;
                break;
            case RESULT_CALLBACK:
            case RESULT_JUMP:
                break;
            case RESULT_ILLEGAL_INSTRUCTION:
                decode_putback((int)(decode.code -decode.op_start + count));
                op = begin_op;
                op.next = new ModifiedDecodeOp();
                op.cycle = ++cycles;
                op = op.next;
                break;
        }
        decode.active_block.page.end=--decode.page.index;
        if (Config.DYNAMIC_CORE_VERIFY) {
            decode.block.originalByteCode = new byte[decode.block.page.end - decode.block.page.start + 1];
            RAM.memcpy(decode.block.originalByteCode, 0, Paging.getDirectIndexRO(start), decode.block.originalByteCode.length);
        }
        start_op.next.cycle=cycles;
        if (trace) {
            op = start_op;
            while (op != null && op.next != null) {
                Op o = op.next;
                op.next = new LogOp(o);
                op = o;
            }
        }
        decode.block.code = new DecodeBlock(decode.block, start_op.next, start, decode.block.page.end - decode.block.page.start + 1);
        return decode.block;
    }

    private static void print(Op op) {
        while (op != null) {
            System.out.println(Integer.toHexString(op.c)+" "+op.description());
            op = op.next;
        }
    }

    static public class ModifiedDecodeOp extends Op {
        public int call() {
            return ModifiedDecode.call();
        }

        public boolean throwsException() {return true;}
        public boolean accessesMemory() {return true;}
        public boolean usesEip() {return true;}
        public boolean setsEip() {return true;}
    }
}
