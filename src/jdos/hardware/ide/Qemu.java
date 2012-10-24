package jdos.hardware.ide;

import jdos.gui.Main;

public class Qemu {
    static public void vm_stop(int runstate) {
        throw new Main.KillException();
    }
}
