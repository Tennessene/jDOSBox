package jdos.cpu;

import java.util.Vector;

public class Core_full {
    static final Vector state = new Vector();

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
    /*Bits*/
    public static final CPU.CPU_Decoder CPU_Core_Full_Run = () -> Core_normal.CPU_Core_Normal_Run.call();

    public static void CPU_Core_Full_Init() {

    }
}
