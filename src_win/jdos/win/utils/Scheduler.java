package jdos.win.utils;

import jdos.win.Win;

public class Scheduler {
    private static class SchedulerItem {
        WinThread thread;
        SchedulerItem next;
        SchedulerItem prev;
    }
    private SchedulerItem currentThread = null;
    private SchedulerItem first;

    public void addThread(WinThread thread) {
        SchedulerItem item = new SchedulerItem();
        item.thread = thread;
        if (first == null) {
            first = item;
        } else {
            item.next = first;
            first.prev = item;
            first = item;
        }
        if (currentThread == null) {
            scheduleThread(item);
        }
    }

    public void removeThread(WinThread thread) {
        SchedulerItem item = first;
        while (item != null) {
            if (item.thread == thread) {
                if (item.next != null)
                    item.next.prev = item.prev;
                if (item.prev != null)
                    item.prev.next = item.next;
                else
                    first = item.next;
                if (first == null)
                    Win.exit();
                if (item == currentThread)
                    tick();
                break;
            }
            item = item.next;
        }
    }

    private void scheduleThread(SchedulerItem thread) {
        currentThread = thread;
        currentThread.thread.loadCPU();
    }

    public WinThread getCurrentThread() {
        return currentThread.thread;
    }

    // :TODO: run them in order of process to minimize page swapping
    public void tick() {
        SchedulerItem next = currentThread.next;
        if (next == null)
            next = first;
        if (next != currentThread) {
            currentThread.thread.saveCPU();
            if (currentThread.thread.getProcess() != next.thread.getProcess()) {
                next.thread.getProcess().switchPageDirectory();
            }
            next.thread.loadCPU();
            currentThread = next;
        }
    }
}
