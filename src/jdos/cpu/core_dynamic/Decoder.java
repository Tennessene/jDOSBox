package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.Core;
import jdos.cpu.Paging;
import jdos.cpu.core_share.Constants;
import jdos.cpu.core_share.ModifiedDecode;
import jdos.hardware.Memory;
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
                };
                return RESULT_JUMP;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        for (int i=0;i<ops.length;i++)
            ops[i] = not_handled;
    }
    private static class StartDecode extends Op {
        public int call() {
            return Constants.BR_Normal;
        }
    }

    private static class HandledDecode extends Op {
        public int call() {
            return Constants.BR_Jump;
        }
    }

    private static class HandledSegChange extends Op {
        public int call() {
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
            Core.base_val_ds=ds;
            return Constants.BR_Normal;
        }
    }

    static private int count=0;

    static {
        Prefix_none.init(ops);
        Prefix_0f.init(ops);
        Prefix_66.init(ops);
        Prefix_66_0f.init(ops);
    }

    public static CacheBlockDynRec CreateCacheBlock(CodePageHandlerDynRec codepage,/*PhysPt*/long start,/*Bitu*/int max_opcodes) {
        // initialize a load of variables
        decode.code_start=start;
        decode.code=start;
        decode.page.code=codepage;
        decode.page.index=(int)(start&4095);
        decode.page.wmap=codepage.write_map;
        decode.page.invmap=codepage.invalidation_map;
        decode.page.first=(int)(start >> 12);
        decode.active_block=decode.block=Cache.cache_openblock();
        decode.block.page.start=decode.page.index;
        codepage.AddCacheBlock(decode.block);

        Decoder_basic.InitFlagsOptimization();

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
        boolean seg_changed = false;
        int opcode = 0;
        int count = 0;
        int cycles = 0;

        try {
            while (max_opcodes-->0 && result==0) {
                decode.cycles++;
                decode.op_start=decode.code;
                decode.modifiedAlot = false;

                opcode=opcode_index+decode_fetchb();
                result = ops[opcode].call(op);
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
                op.cycle = ++cycles;
                if (result == RESULT_CONTINUE_SEG) {
                    result = RESULT_HANDLED;
                    max_opcodes++;
                    seg_changed = true;
                    continue;
                }
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
                if (seg_changed && result==0) {
                    seg_changed = false;
                    op.next = new HandledSegChange();
                    op = op.next;
                    op.c = -1;
                    op.cycle = cycles;
                    begin_op = op;
                }
            }
        } catch (ChangePageException e) {
            if (decode.code -decode.op_start + count == 0) {
                result = RESULT_HANDLED; // begining of op code started on next page
            } else {
                result = RESULT_ILLEGAL_INSTRUCTION; // op code spanned two pages, run with normal core in case of page fault
            }
        } catch (Paging.PageFaultException e) {
            Log.exit("Oops");
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
            System.arraycopy(Memory.direct, Paging.getDirectIndexRO(start), decode.block.originalByteCode, 0, decode.block.originalByteCode.length);
        }
        decode.block.code = new DecodeBlock(start_op.next);
        return decode.block;
    }

    static private class ModifiedDecodeOp extends Op {
        public int call() {
            return ModifiedDecode.call();
        }
    }
}
