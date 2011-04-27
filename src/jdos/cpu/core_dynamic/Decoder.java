package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core_dynrec;
import jdos.cpu.Core;
import jdos.debug.Debug;
import jdos.misc.setup.Config;

public class Decoder extends Inst1 {
    public static final Decode[] ops = new Decode[1024];

    private static class StartDecode extends Op {
        public int call() {
            return Core_dynrec.BR_Normal;
        }
    }

    private static class HandledDecode extends Op {
        long instructions;
        public HandledDecode(long instructions) {
            this.instructions = instructions;
        }
        public int call() {
            CPU_Regs.reg_eip+=instructions;
            return Core_dynrec.BR_Normal;
        }
    }

    private static class ModifiedDecode extends Op {
        long instructions;
        public ModifiedDecode(long instructions) {
            this.instructions = instructions;
        }
        public int call() {
            CPU_Regs.reg_eip+=instructions;
            return Core_dynrec.BR_Opcode;
        }
    }

    static private int count=0;
    private static class DecodeBlock extends DynamicClass {
        Op op;

        public DecodeBlock(Op op) {
            this.op = op;
        }
        public int call2() {
            Op o = op;
            int result = Core_dynrec.BR_Normal;
            Core.base_ds=CPU.Segs_DSphys;
            Core.base_ss=CPU.Segs_SSphys;
            Core.base_val_ds=ds;
            while (o != null && result == Core_dynrec.BR_Normal) {
                if (Config.DEBUG_LOG) {
                    if (o.c>=0) Debug.start(Debug.TYPE_CPU, o.c);
                    //System.out.println(count+":"+o.c);
                }
                result = o.call();
                CPU.CPU_Cycles--;
                if (Config.DEBUG_LOG)
                    if (o.c>=0) Debug.stop(Debug.TYPE_CPU, o.c);
                o = o.next;
                if (result == Core_dynrec.BR_Continue) {
                    result = Core_dynrec.BR_Normal;
                    continue;
                }
                Core.base_ds=CPU.Segs_DSphys;
                Core.base_ss=CPU.Segs_SSphys;
                Core.base_val_ds=ds;
            }
            return result;
        }
    }
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
        int callback = 0;

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

        while (max_opcodes-->0 && result==0) {
            // Init prefixes
            decode.big_addr=CPU.cpu.code.big;
            decode.big_op=CPU.cpu.code.big;
            decode.seg_prefix=0;
            decode.seg_prefix_used=false;
            decode.rep=Decoder_basic.REP_NONE;
            decode.cycles++;
            decode.op_start=decode.code;

            /*Bitu*/int opcode;
            if (decode.page.invmap==null) opcode=decode_fetchb();
            else {
                // some entries in the invalidation map, see if the next
                // instruction is known to be modified a lot
                if (decode.page.index<4096) {
                    if (decode.page.invmap.p[decode.page.index]>=4) {
                        result = RESULT_ILLEGAL_INSTRUCTION;
                        break;
                    }
                    opcode=decode_fetchb();
                } else {
                    // switch to the next page
                    opcode=decode_fetchb();
                    if (decode.page.invmap!=null && decode.page.invmap.p[decode.page.index-1]>=4) {
                        result = RESULT_ILLEGAL_INSTRUCTION;
                        break;
                    }
                }
            }
            opcode+=opcode_index;
            if (ops[opcode] == null) { // :TODO: after all codes are done then remove this
                System.out.println("Unknown op: "+opcode);
                System.exit(1);
                decode.cycles--;
                decode.code--;
                break;
            }
            result = ops[opcode].call(op);
            if (op.next != null) {
                op = op.next;
                op.c = opcode;
                //System.out.println(Integer.toHexString(opcode));
            } else {
                if (Config.DEBUG_LOG) {
                    op.next = new Op() {
                        final public int call() {
                            return Core_dynrec.BR_Continue;
                        }
                    };
                    op = op.next;
                    op.c = opcode;
                }
            }
            if (result == RESULT_CONTINUE) {
                result = RESULT_HANDLED;
                max_opcodes++;
                continue;
            } else if (result == RESULT_ANOTHER) {
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
        }
        Cache.cache_closeblock();
        switch (result) {
            case RESULT_CALLBACK:
            case RESULT_HANDLED:
                op.next = new HandledDecode(decode.code - decode.code_start);
                op = op.next;
                break;
            case RESULT_JUMP:
                break;
            case RESULT_ILLEGAL_INSTRUCTION:
                op.next = new ModifiedDecode(decode.code - decode.code_start);
                op = op.next;
                break;
        }
        decode.block.code = new DecodeBlock(start_op.next);
        decode.block.code.decode = decode;
        decode.active_block.page.end=--decode.page.index;
        return decode.block;
    }
}
