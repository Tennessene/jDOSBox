package jdos.cpu.core_dynamic;

import jdos.cpu.*;
import jdos.cpu.core_normal.Prefix_helpers;
import jdos.misc.setup.Config;
import jdos.misc.Log;

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
        public int prefixes;
        public boolean EA16;
        public int opcode_index;
        public ModifiedDecode(int opcode_index, int prefixes, boolean EA16, long instructions) {
            this.instructions = instructions;
            this.prefixes = prefixes;
            this.EA16 = EA16;
            this.opcode_index = opcode_index;
        }
        public int call() {
            CPU_Regs.reg_eip += instructions;
            Core.cseip = CPU.Segs_CSphys + CPU_Regs.reg_eip;
            Core.prefixes = prefixes;
            Table_ea.EA16 = EA16;
            while (true) {
                int c = opcode_index + Core.Fetchb.call();
//                    last = c;
//                    Debug.start(Debug.TYPE_CPU, c);
//                    try {
                try {
                    int result = jdos.cpu.core_normal.Prefix_none.ops[c].call();
                    if (result != Prefix_helpers.HANDLED) {
                        if (result == Prefix_helpers.CONTINUE) {
                            break;
                        } else if (result == Prefix_helpers.RETURN) {
                            Core_dynrec.core_dynrec.callback = jdos.cpu.core_normal.Prefix_none.returnValue;
                            return Core_dynrec.BR_CallBack;
                        } else if (result == Prefix_helpers.RESTART) {
                            continue;
                        } else if (result == Prefix_helpers.CBRET_NONE) {
                            return Core_dynrec.BR_CBRet_None;
                        } else if (result == Prefix_helpers.DECODE_END) {
                            Prefix_helpers.SAVEIP();
                            Flags.FillFlags();
                            return Core_dynrec.BR_CBRet_None;
                        } else if (result == Prefix_helpers.NOT_HANDLED || result == Prefix_helpers.ILLEGAL_OPCODE) {
                            CPU.CPU_Exception(6, 0);
                            break;
                        }
                    }
                    // necessary for Prefix_helpers.EXCEPTION
                } catch (Prefix_helpers.ContinueException e) {
                    break;
                }
//                    } finally {
//                        Debug.stop(Debug.TYPE_CPU, c);
//                    }

                // inlined
                // SAVEIP();
                CPU_Regs.reg_eip = Core.cseip - CPU.Segs_CSphys;
                break;
            }
            return Core_dynrec.BR_Normal;
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
            decode.modifiedAlot = false;

            int opcode=opcode_index+decode_fetchb();
            result = ops[opcode].call(op);
            if (decode.modifiedAlot) {
                result = RESULT_ILLEGAL_INSTRUCTION;
                break;
            }
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
                decode.page.index-= decode.code - decode.op_start;
                // :TODO: handle page change
                if (decode.page.index<0) {
                    Log.exit("Dynamic Core:  Self modifying code across page boundries not implemented yet");
                }
                op.next = new ModifiedDecode(opcode_index, prefixes, EA16, decode.op_start - decode.code_start);
                op = op.next;
                break;
        }
        decode.block.code = new DecodeBlock(start_op.next);
        decode.active_block.page.end=--decode.page.index;
        return decode.block;
    }
}
