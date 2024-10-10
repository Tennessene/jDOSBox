package jdos.win.system;

import jdos.win.builtin.kernel32.WaitObject;

public class WinMutex extends WaitObject {
    static public WinMutex create(String name) {
        return new WinMutex(nextObjectId(), name);
    }

    static public WinMutex get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinMutex))
            return null;
        return (WinMutex)object;
    }

    private WinMutex(int handle, String name) {
        super(handle, name);
    }
}
