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
        for (int i=0;i<objects.size();i++) {
            if (!objects.get(i).isReady())
                return false;
        }
        for (int i=0;i<objects.size();i++) {
            objects.get(i).get(this);
        }
        Scheduler.addThread(thread, false);
        return true;
    }

    public Vector<WaitObject> objects = new Vector<WaitObject>();
    public WinThread thread;
}
