package jdos.cpu;

public class Core_dynrec {
    public static void CPU_Core_Dynrec_Init() {
    }

    static public void CPU_Core_Dynrec_Cache_Init(boolean enable_cache) {

    }

    static public void CPU_Core_Dynrec_Cache_Close() {

    }
    public static CPU.CPU_Decoder CPU_Core_Dynrec_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            return 0;
        }
    };
}
