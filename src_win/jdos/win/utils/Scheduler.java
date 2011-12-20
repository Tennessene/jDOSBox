package jdos.win.utils;

import java.util.Vector;

public class Scheduler {
    private Vector threads = new Vector();
    private WinThread currentThread = null;

    public void addThread(WinThread thread) {
        threads.add(thread);
        if (currentThread == null) {
            scheduleThread(thread);
        }
    }

    private void scheduleThread(WinThread thread) {
        currentThread = thread;
        currentThread.loadCPU();
    }

    public WinThread getCurrentThread() {
        return currentThread;
    }
}
