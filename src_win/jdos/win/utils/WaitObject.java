package jdos.win.utils;

import jdos.cpu.CPU_Regs;

import java.util.Vector;

public class WaitObject extends WinObject {
    static private final int WAIT_ABANDONED = 0x00000080;
    static private final int WAIT_OBJECT_0 = 0x00000000;
    static private final int WAIT_TIMEOUT = 0x00000102;

    public WaitObject(int handle) {
        super(handle);
        owner = this;
    }

    public void wait(WinThread thread, int timeout) {
        if (thread == owner || owner == null) {
            CPU_Regs.reg_eax.dword = WAIT_OBJECT_0;
            return;
        }
        CPU_Regs.reg_eax.dword = WAIT_TIMEOUT;
        if (timeout !=0) {
            waiting.add(thread);
            WinSystem.scheduler.removeThread(thread, true);
        }
    }

    public void release() {
        for (int i=0;i<waiting.size();i++) {
            WinThread thread = (WinThread)waiting.elementAt(i);
            WinSystem.scheduler.addThread(thread);
        }
    }

    public WinObject owner;
    public Vector waiting = new Vector();
}
