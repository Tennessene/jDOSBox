package jdos.win.utils;

public class WinEvent extends WaitObject {
    public WinEvent(int handle, String name, boolean manual, boolean set) {
        super(handle);
        this.name = name;
        this.manual = set;
        if (set) {
            owner = WinSystem.getCurrentThread();
        } else {
            owner = null;
        }
    }

    public boolean manual;
}
