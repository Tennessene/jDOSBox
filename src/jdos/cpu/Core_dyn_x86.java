package jdos.cpu;

public class Core_dyn_x86 {   
    public static void CPU_Core_Dyn_X86_Init() {

    }
    public static void CPU_Core_Dyn_X86_Cache_Init(boolean enable_cache) {

    }

    public static void CPU_Core_Dyn_X86_SetFPUMode(boolean dh_fpu) {

    }

    public static void CPU_Core_Dyn_X86_Cache_Close() {

    }
    public static CPU.CPU_Decoder CPU_Core_Dyn_X86_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            return 0;
        }
    };
}
