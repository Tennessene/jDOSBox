package jdos.cpu;

import jdos.cpu.core_normal.Prefix_66_0f;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.Record;

public class Core_normal extends Prefix_66_0f {
    public static boolean log = false;
    public static int start = 0;

    public static class State {
        public int s_opcode_index;
        public int s_cseip;
        public int s_prefixes;
        public boolean EA16;
        public int s_base_ds;
        public int s_base_ss;
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

    static public final String[] desc = new String[] {
            "ADD",      "ADD",      "ADD",      "ADD",      "ADD",      "ADD",      "PUSH ES",  "POP ES",
            "OR",       "OR",       "OR",       "OR",       "OR",       "OR",       "PUSH CS",  "2 byte",
            "ADC",      "ADC",      "ADC",      "ADC",      "ADC",      "ADC",      "PUSH SS",  "POP SS",
            "SBB",      "SBB",      "SBB",      "SBB",      "SBB",      "SBB",      "PUSH DS",  "POP DS",
            "AND",      "AND",      "ABD",      "ABD",      "ABD",      "ABD",      "SEG ES",   "DAA",
            "SUB",      "SUB",      "SUB",      "SUB",      "SUB",      "SUB",      "SEG CS",   "DAS",
            "XOR",      "XOR",      "XOR",      "XOR",      "XOR",      "XOR",      "SEG SS",   "AAA",
            "CMP",      "CMP",      "CMP",      "CMP",      "CMP",      "CMP",      "SEG DS",   "AAS",
            "INC",      "INC",      "INC",      "INC",      "INC",      "INC",      "INC",      "INC",
            "DEC",      "DEC",      "DEC",      "DEC",      "DEC",      "DEC",      "DEC",      "DEC",
            "PUSH",     "PUSH",     "PUSH",     "PUSH",     "PUSH",     "PUSH",     "PUSH",     "PUSH",
            "POP",      "POP",      "POP",      "POP",      "POP",      "POP",      "POP",      "POP",
            "PUSHA",    "POPA",     "BOUND",    "ARPL",     "SEG FS",   "SEG GS",   "SIZE PREF","ADDR PREF",
            "PUSH",     "IMUL",     "PUSH",     "IMUL",     "INSB",     "INSW",     "OUTSB",    "OUTSW",
            "JO",       "JNO",      "JB",       "JNB",      "JZ",       "JNZ",      "JBE",      "JNBE",
            "JS",       "JNS",      "JP",       "JNP",      "JL",       "JNL",      "JLE",      "JNLE",
        };
    public static int count=1;

    public static CPU.CPU_Decoder CPU_Core_Normal_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            //System.out.println("CPU_Core_Normal_Run");
            while (CPU.CPU_Cycles-->0) {
                // inlined
                //LOADIP();
                cseip=CPU_Regs.reg_csPhys.dword+CPU_Regs.reg_eip;
                if (CPU.cpu.code.big) {
                    opcode_index=0x200;
                    prefixes=1;
                    EA16 = false;
                } else {
                    opcode_index=0;
                    prefixes=0;
                    EA16 = true;
                }
                base_ds=CPU_Regs.reg_dsPhys.dword;
                base_ss=CPU_Regs.reg_ssPhys.dword;
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
                    if ((prefixes & PREFIX_LOCK)!=0) {
                        if (Core.isInvalidLock(c & ~0x200)) {
                            CPU.CPU_Exception(6,0);
                        }
                        prefixes&=~PREFIX_LOCK;// only check the prefix once
                    }
//                    last = c;
//                    if (Config.DEBUG_LOG)
//                        Debug.start(Debug.TYPE_CPU, c);
//                    try {
//                    Record.op(c);
//                    if (count>0) {
//                        System.out.println(String.format("%d %06x:%08x %3s %-8s EAX=%08x ECX=%08x EDX=%08x EBX=%08x ESP=%08x EBP=%08x ESI=%08x EDI=%08x FLAGS=%04x", count, CPU_Regs.reg_csVal.dword, CPU_Regs.reg_eip, Integer.toHexString(c), (desc.length > c ? " " + desc[c] : ""), CPU_Regs.reg_eax.dword, CPU_Regs.reg_ecx.dword, CPU_Regs.reg_edx.dword, CPU_Regs.reg_ebx.dword, CPU_Regs.reg_esp.dword, CPU_Regs.reg_ebp.dword, CPU_Regs.reg_esi.dword, CPU_Regs.reg_edi.dword, CPU_Regs.flags));
//                        count++;
//                        if (count % 5000 == 0) {
//                            int ii = 0;
//                        }
//                    }
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
                                    /*Bitu*/int len=GETIP()-reg_eip;
                                    LOADIP();
                                    if (len>16) len=16;
                                    StringBuffer tempcode=new StringBuffer();
                                    for (;len>0;len--) {
                                        tempcode.append(Integer.toHexString(Memory.mem_readb(cseip++)));
                                    }
                                }
                                if (Log.level<=LogSeverities.LOG_NORMAL)
                                    Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_NORMAL,"Illegal/Unhandled opcode "+Integer.toHexString(c));
                                CPU.CPU_Exception(6,0);
                                break;
                            }
                        }
//                    } finally {
//                        if (Config.DEBUG_LOG)
//                            Debug.stop(Debug.TYPE_CPU, c);
//                    }

                    // inlined
                    // SAVEIP();
                    CPU_Regs.reg_eip=cseip- CPU_Regs.reg_csPhys.dword;
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
