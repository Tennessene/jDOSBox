package jdos.cpu;

public class Core_full {
    static Core_normal.State full_state = new Core_normal.State();
    static Core_normal.State save_state = new Core_normal.State();

    public static CPU.CPU_Decoder CPU_Core_Full_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            Core_normal.saveState(save_state);
            Core_normal.loadState(full_state);
            int result = Core_normal.CPU_Core_Normal_Run.call();
            Core_normal.saveState(full_state);
            Core_normal.loadState(save_state);
            return result;
        }
    };

    public static void CPU_Core_Full_Init() {

    }
}
