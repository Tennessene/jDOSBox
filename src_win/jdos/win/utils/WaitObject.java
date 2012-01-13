package jdos.win.utils;

import jdos.cpu.CPU_Regs;

import java.util.Vector;

public class WaitObject extends WinObject {
    static public final int WAIT_ABANDONED = 0x00000080;
    static public final int WAIT_OBJECT_0 = 0x00000000;
    static public final int WAIT_TIMEOUT = 0x00000102;

    public WaitObject(int handle) {
        super(handle);
        owner = this;
    }

    public WaitObject(int handle, String name) {
        super(name, handle);
        owner = this;
    }

    public int wait(WinThread thread, int timeout) {
        if (thread == owner || owner == null) {
            CPU_Regs.reg_eax.dword = WAIT_OBJECT_0;
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
        }
    }

    public WinObject owner;
    public Vector waiting = new Vector();
}
