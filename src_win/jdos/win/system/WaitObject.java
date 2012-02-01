package jdos.win.system;

import jdos.cpu.CPU_Regs;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.kernel32.WinThread;

import java.util.Vector;

public class WaitObject extends WinObject {
    static public WaitObject create() {
        return new WaitObject(nextObjectId());
    }

    static public WaitObject getWait(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WaitObject))
            return null;
        return (WaitObject)object;
    }

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

    protected int internalWait(WinThread thread, int timeout) {
        if (waiting.contains(thread)) {
            waiting.remove(thread);
            return WAIT_TIMEOUT;
        }
        HandlerBase.currentHandler.wait = true;
        waiting.add(thread);
        thread.waitTime = timeout;
        return WAIT_SWITCH;
    }

    public int wait(WinThread thread, int timeout) {
        if (thread == owner || owner == null) {
            CPU_Regs.reg_eax.dword = WAIT_OBJECT_0;
            return 0;
        }
        if (timeout !=0) {
            return internalWait(thread, timeout);
        }
        return WAIT_TIMEOUT;
    }

    public void release() {
        for (int i=0;i<waiting.size();i++) {
            WinThread thread = waiting.elementAt(i);
            Scheduler.addThread(thread, false);
        }
        waiting.clear();
        owner = null;
    }

    public WinObject owner;
    public Vector<WinThread> waiting = new Vector<WinThread>();
}