package jdos.win.builtin.kernel32;

import jdos.win.system.Scheduler;

import java.util.Vector;

public class WaitGroup {
    public WaitGroup(WinThread thread) {
        this.thread = thread;
    }

    public WaitGroup(WinThread thread, WaitObject waitObject) {
        this.thread = thread;
        this.objects.add(waitObject);
    }

    public boolean released() {
        for (WaitObject waitObject : objects) {
            if (!waitObject.isReady())
                return false;
        }
        for (WaitObject object : objects) {
            object.get(this);
        }
        Scheduler.addThread(thread, false);
        return true;
    }

    public final Vector<WaitObject> objects = new Vector<>();
    public final WinThread thread;
}
