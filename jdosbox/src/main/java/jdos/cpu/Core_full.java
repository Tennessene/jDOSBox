package jdos.cpu;

import java.util.Vector;

public class Core_full {
    static Vector state = new Vector();

    public static void pushState() {
        Core_normal.State s = new Core_normal.State();
        Core_normal.saveState(s);
        state.addElement(s);
    }

    public static void removeState() {
        state.remove(state.size()-1);
    }
    public static void popState() {
        Core_normal.State s = (Core_normal.State)state.remove(state.size()-1);
        Core_normal.loadState(s);
    }
    public static CPU.CPU_Decoder CPU_Core_Full_Run = new CPU.CPU_Decoder() {
        public /*Bits*/int call() {
            return Core_normal.CPU_Core_Normal_Run.call();
        }
    };

    public static void CPU_Core_Full_Init() {

    }
}
