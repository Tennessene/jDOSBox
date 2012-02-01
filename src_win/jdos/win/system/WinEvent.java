package jdos.win.system;

import jdos.cpu.CPU_Regs;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinThread;

public class WinEvent extends WaitObject {
    static public WinEvent create(String name, boolean manual, boolean set) {
        return new WinEvent(nextObjectId(), name, manual, set);
    }

    static public WinEvent get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinEvent))
            return null;
        return (WinEvent)object;
    }

    public WinEvent(int handle, String name, boolean manual, boolean set) {
        super(handle);
        this.name = name;
        this.manual = set;
        this.set = set;
    }

    public int set() {
        set = true;
        release();
        return WinAPI.TRUE;
    }

    public void reset() {
        set = false;
    }

    public int wait(WinThread thread, int timeout) {
        if (set) {
            CPU_Regs.reg_eax.dword = WaitObject.WAIT_OBJECT_0;
            if (!manual)
                set = false;
            return 0;
        }
        CPU_Regs.reg_eax.dword = WAIT_TIMEOUT;
        if (timeout !=0) {
            return internalWait(thread, timeout);
        }
        return 0;
    }

    public void release() {
        while(waiting.size()>0) {
            WinThread thread = waiting.remove(0);
            Scheduler.addThread(thread, false);
            if (!manual) {
                break; // only one
            }
        }
    }

    public boolean manual;
    public boolean set;
}
