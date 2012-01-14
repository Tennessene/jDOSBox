package jdos.win.system;

public class WinMutex extends WaitObject {
    public WinMutex(int handle, String name) {
        super(handle, name);
    }
}
