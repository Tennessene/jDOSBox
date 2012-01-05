package jdos.win.utils;

import jdos.win.Console;
import jdos.win.Win;

import java.util.Hashtable;

public class Scheduler {
    private static class SchedulerItem {
        WinThread thread;
        SchedulerItem next;
        SchedulerItem prev;
        int sleepUntil = 0;
    }
    private SchedulerItem currentThread = null;
    private SchedulerItem first;
    private Hashtable threadMap = new Hashtable();
    static public final int TICK_MS = 50;

    private int tickCount = 0;

    public void addThread(WinThread thread, boolean schedule) {
        SchedulerItem item = new SchedulerItem();
        item.thread = thread;
        if (first == null) {
            first = item;
        } else {
            item.next = first;
            first.prev = item;
            first = item;
        }
        if (currentThread == null || schedule) {
            scheduleThread(item);
        }
        threadMap.put(thread, item);
    }

    public void sleep(WinThread thread, int ms) {
        //SchedulerItem item = (SchedulerItem)threadMap.get(thread);
        //item.sleepUntil = tickCount+ms / TICK_MS + 1;
        //tick(false);
    }

    public void removeThread(WinThread thread, boolean canSwitchProcess) {
        threadMap.remove(thread);
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
                if (item == currentThread) {
                    if (canSwitchProcess) {
                        tick(false);
                    } else {
                        SchedulerItem i = first;
                        // move to next thread in the same process, we don't want to change page directories here
                        // if this is the last thread in the process then hopefully tick() will be called from somewhere
                        // else before the cpu resumes
                        while (i!=null) {
                            if (i!=item && i.thread.getProcess()==item.thread.getProcess()) {
                                currentThread.thread.saveCPU();
                                currentThread = i;
                                currentThread.thread.loadCPU();
                                break;
                            }
                            i = i.next;
                        }
                    }
                }
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
        if (currentThread == null)
            return null;
        return currentThread.thread;
    }

    // :TODO: run them in order of process to minimize page swapping
    public void tick(boolean incrementTickCount) {
        if (incrementTickCount) {
            tickCount++;
        }
        SchedulerItem next = currentThread.next;
        SchedulerItem start = currentThread;
        while (true) {
            if (next == null) {
                next = first;
            }
            if (next.sleepUntil <= tickCount) {
                break;
            }
            if (next == start) {
                // :TODO: all threads are alseep
                tick(false);
                Console.out("Can not handle all threads being asleep at the same time yet");
                Win.exit();
            }
            next = next.next;
        }
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
