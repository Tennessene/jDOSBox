package jdos.cpu;

import jdos.cpu.core_normal.Prefix_66_0f;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.debug.Debug;

public class Core_normal extends Prefix_66_0f {
    public static boolean log = false;
    public static int start = 0;

    public static class State {
        public int s_opcode_index;
        public long s_cseip;
        public int s_prefixes;
        public boolean EA16;
        public long s_base_ds;
        public long s_base_ss;
        public int s_base_val_ds;
        public boolean rep_zero;
    }

    public static void saveState(State state) {
        state.s_opcode_index = opcode_index;
        state.s_cseip = cseip;
        state.s_prefixes = prefixes;
        state.EA16 = EA16;
        state.s_base_ds = base_ds;
        state.s_base_ss = base_ss;
        state.s_base_val_ds = base_val_ds;
        state.rep_zero = rep_zero;
    }

    public static void loadState(State state) {
        opcode_index = state.s_opcode_index;
        cseip = state.s_cseip;
        prefixes = state.s_prefixes;
        EA16 = state.EA16;
        base_ds = state.s_base_ds;
        base_ss = state.s_base_ss;
        base_val_ds = state.s_base_val_ds;
        rep_zero = state.rep_zero;
    }

    public static CPU.CPU_Decoder CPU_Core_Normal_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            //System.out.println("CPU_Core_Normal_Run");
            while (CPU.CPU_Cycles-->0) {
                // inlined
                //LOADIP();
                cseip=CPU.Segs_CSphys+CPU_Regs.reg_eip;
                if (CPU.cpu.code.big) {
                    opcode_index=0x200;
                    prefixes=1;
                    EA16 = false;
                } else {
                    opcode_index=0;
                    prefixes=0;
                    EA16 = true;
                }
                base_ds=CPU.Segs_DSphys;
                base_ss=CPU.Segs_SSphys;
                base_val_ds=ds;
//                if (Config.C_DEBUG) {
//                    if (Config.C_HEAVY_DEBUG) {
//                        if (Debug.DEBUG_HeavyIsBreakpoint()) {
//                            Flags.FillFlags();
//                            return Debug.debugCallback;
//                        }
//                    }
//                    Debug.cycle_count++;
//                }
        //restart_opcode:
                while (true) {
                    int c = opcode_index+Fetchb();
//                    last = c;
                    if (Config.DEBUG_LOG)
                        Debug.start(Debug.TYPE_CPU, c);
                    try {
                    try {
                        int result = ops[c].call();
                        if (result != HANDLED) {
                            if (result == CONTINUE) {
                                break;
                            } else if (result == RETURN) {
                                return returnValue;
                            } else if (result == RESTART) {
                                continue;
                            } else if (result == CBRET_NONE) {
                                return Callback.CBRET_NONE;
                            } else if (result == DECODE_END) {
                                SAVEIP();
                                Flags.FillFlags();
                                return Callback.CBRET_NONE;
                            } else if (result == NOT_HANDLED || result == ILLEGAL_OPCODE) {
                                if (Config.C_DEBUG)
                                {
                                    /*Bitu*/int len=(int)((GETIP()-reg_eip()));
                                    LOADIP();
                                    if (len>16) len=16;
                                    StringBuffer tempcode=new StringBuffer();
                                    for (;len>0;len--) {
                                        tempcode.append(Integer.toHexString(Memory.mem_readb(cseip++)));
                                    }
                                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_NORMAL,"Illegal/Unhandled opcode "+tempcode);
                                }
                                CPU.CPU_Exception(6,0);
                                break;
                            }
                        }
                    // necessary for Prefix_helpers.EXCEPTION
                    } catch (ContinueException e) {
                        break;
                    }
                    } finally {
                        if (Config.DEBUG_LOG)
                            Debug.stop(Debug.TYPE_CPU, c);
                    }

                    // inlined
                    // SAVEIP();
                    CPU_Regs.reg_eip=cseip- CPU.Segs_CSphys;
                    break;
                }
            }
            Flags.FillFlags();
            return Callback.CBRET_NONE;
        }
    };

    public static CPU.CPU_Decoder CPU_Core_Normal_Trap_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            /*Bits*/int oldCycles = CPU.CPU_Cycles;
            CPU.CPU_Cycles = 1;
            CPU.cpu.trap_skip = false;

            /*Bits*/int ret=CPU_Core_Normal_Run.call();
            if (!CPU.cpu.trap_skip) CPU.CPU_HW_Interrupt(1);
            CPU.CPU_Cycles = oldCycles-1;
            CPU.cpudecoder = CPU_Core_Normal_Run;
            return ret;
        }
    };
    
    public static void CPU_Core_Normal_Init() {
    }
}
