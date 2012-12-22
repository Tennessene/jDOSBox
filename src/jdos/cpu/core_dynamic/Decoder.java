package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.ModifiedDecode;
import jdos.hardware.Memory;
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
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
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

    public static class SegEsOp extends SegOp {
        public int call() {
            Core.DO_PREFIX_SEG_ES();
            int result = op.call();
            reset();
            return result;
        }
        public String description() {return "ES: "+op.description();}
    }
    public static class SegCsOp extends SegOp {
        public int call() {
            Core.DO_PREFIX_SEG_CS();
            int result = op.call();
            reset();
            return result;
        }
        public String description() {return "CS: "+op.description();}
    }
    public static class SegSsOp extends SegOp {
        public int call() {
            Core.DO_PREFIX_SEG_SS();
            int result = op.call();
            reset();
            return result;
        }
        public String description() {return "SS: "+op.description();}
    }
    public static class SegDsOp extends SegOp {
        public int call() {
            Core.DO_PREFIX_SEG_DS();
            int result = op.call();
            reset();
            return result;
        }
        public String description() {return "DS: "+op.description();}
    }
    public static class SegFsOp extends SegOp {
        public int call() {
            Core.DO_PREFIX_SEG_FS();
            int result = op.call();
            reset();
            return result;
        }
        public String description() {return "FS: "+op.description();}
    }
    public static class SegGsOp extends SegOp {
        public int call() {
            Core.DO_PREFIX_SEG_GS();
            int result = op.call();
            reset();
            return result;
        }
        public String description() {return "GS: "+op.description();}
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
        int opcode = 0;
        int count = 0;
        int cycles = 0;

        opcode_seg = -1;
        try {
            while (max_opcodes-->0 && result==0) {
                decode.cycles++;
                decode.op_start=decode.code;
                decode.modifiedAlot = false;

                opcode=opcode_index+decode_fetchb();
                result = ops[opcode].call(op);
                //if (printNextOp && op.next!=null) {
                //    System.out.println(Integer.toHexString(opcode)+" "+op.next.getClass().getName());
                //}
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
                if (opcode_seg>=0) {
                    SegOp segOp = null;
                    switch (opcode_seg) {
                        case CPU_Regs.es: segOp = new SegEsOp(); break;
                        case CPU_Regs.cs: segOp = new SegCsOp(); break;
                        case CPU_Regs.ss: segOp = new SegSsOp(); break;
                        case CPU_Regs.ds: segOp = new SegDsOp(); break;
                        case CPU_Regs.fs: segOp = new SegFsOp(); break;
                        case CPU_Regs.gs: segOp = new SegGsOp(); break;
                    }
                    segOp.op = op.next;
                    segOp.op.eip_count = count;
                    segOp.op.c = opcode;
                    op.next = segOp;
                }
                op = op.next;
                op.c = opcode;
                ++cycles;

                begin_op = op;
                op.eip_count = count;
                count = 0;
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
                opcode_seg = -1;
            }
        } catch (PageFaultException e) {
            if (decode.code -decode.op_start + count == 0) {
                result = RESULT_HANDLED; // begining of op code started on next page
            } else {
                result = RESULT_ILLEGAL_INSTRUCTION; // op code spanned two pages, run with normal core in case of page fault
            }
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
            Memory.host_memcpy(decode.block.originalByteCode, 0, Paging.getDirectIndexRO(start), decode.block.originalByteCode.length);
        }
        start_op.next.cycle=cycles;
        decode.block.code = new DecodeBlock(decode.block, start_op.next, start, decode.block.page.end - decode.block.page.start + 1);
        return decode.block;
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
