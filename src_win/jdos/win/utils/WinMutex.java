package jdos.win.utils;

public class WinMutex extends WaitObject {
    public WinMutex(int handle, String name) {
        super(handle, name);
    }
}
