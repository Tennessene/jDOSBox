package jdos.win.system;

import jdos.cpu.CPU_Regs;
import jdos.win.builtin.WinAPI;

public class WinEvent extends WaitObject {
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

    public int wait(WinThread thread, int timeout) {
        if (set) {
            CPU_Regs.reg_eax.dword = WaitObject.WAIT_OBJECT_0;
            if (!manual)
                set = false;
            return 0;
        }
        CPU_Regs.reg_eax.dword = WAIT_TIMEOUT;
        if (timeout !=0) {
            waiting.add(thread);
            return 2;
        }
        return 0;
    }

    public void release() {
        for (int i=0;i<waiting.size();i++) {
            WinThread thread = (WinThread)waiting.elementAt(i);
            WinSystem.scheduler.addThread(thread, false);
            if (!manual) {
                break; // only one
            }
        }
    }

    public boolean manual;
    public boolean set;
}
