package jdos.win.system;

import jdos.win.builtin.kernel32.WinThread;
import jdos.win.builtin.user32.Input;

import java.util.Hashtable;

public class Scheduler {
    private static class SchedulerItem {
        WinThread thread;
        SchedulerItem next;
        SchedulerItem prev;
        int sleepUntil = 0;
    }
    private static SchedulerItem currentThread = null;
    private static SchedulerItem first;
    private static Hashtable<WinThread, SchedulerItem> threadMap = new Hashtable<WinThread, SchedulerItem>();
    private static long start = System.currentTimeMillis();

    // DirectX surface to force to the screen
    public static int monitor;

    static public void addThread(WinThread thread, boolean schedule) {
        SchedulerItem item;
        item = threadMap.get(thread);
        if (item != null) {
            item.sleepUntil = 0;
            if (schedule)
                scheduleThread(item);
        } else {
            item = new SchedulerItem();
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
    }

    static private int currentTickCount() {
        return (int)(System.currentTimeMillis()-start);
    }

    static public void sleep(WinThread thread, int ms) {
        SchedulerItem item = threadMap.get(thread);
        if (item != null) {
            item.sleepUntil = currentTickCount() + ms + 1;
            tick();
        }
    }

    static public void wait(WinThread thread) {
        if (thread.waitTime==-1)
            removeThread(thread);
        else {
            SchedulerItem item = threadMap.get(thread);
            thread.waitTimeStart = currentTickCount();
            item.sleepUntil = thread.waitTimeStart + 1;
            tick();
        }
    }

    // After this call is make do not change any registers, the current process may have changed
    static public void removeThread(WinThread thread) {
        if (threadMap.remove(thread) == null)
            return;
        SchedulerItem item = first;
        while (item != null) {
            if (item.thread == thread) {
                if (item.next != null)
                    item.next.prev = item.prev;
                if (item.prev != null)
                    item.prev.next = item.next;
                else
                    first = item.next;
                while (first == null) {
                    synchronized (StaticData.inputQueueMutex) {
                        Input.processInput();
                        if (first != null)
                            break;
                        try {StaticData.inputQueueMutex.wait();} catch (Exception e){}
                    }
                }
                if (item == currentThread) {
                    tick();
                }
                break;
            }
            item = item.next;
        }
    }

    static private void scheduleThread(SchedulerItem thread) {
        if (currentThread != null) {
            currentThread.thread.saveCPU();
        }
        currentThread = thread;
        currentThread.thread.loadCPU();
    }

    static public WinThread getCurrentThread() {
        if (currentThread == null)
            return null;
        return currentThread.thread;
    }

    // :TODO: run them in order of process to minimize page swapping
    static public void tick() {
        if (threadMap.size() == 0) {
            return;
        }
        SchedulerItem next = currentThread.next;
        SchedulerItem start = currentThread;
        int tickCount = currentTickCount();
        while (true) {
            if (next == null) {
                next = first;
                tickCount = currentTickCount();
            }
            if (next.sleepUntil <= tickCount) {
                break;
            }
            if (next == start) {
                try {Thread.sleep(10);} catch (Exception e) {}
                tickCount = currentTickCount();
            }
            next = next.next;
        }
        if (next.thread != currentThread.thread) {
            //System.out.println("Switching threads: "+currentThread.thread.getHandle()+"("+ Ptr.toString(CPU_Regs.reg_eip)+") -> "+next.thread.getHandle()+"("+Ptr.toString(next.thread.cpuState.eip)+")");
            currentThread.thread.saveCPU();
            if (currentThread.thread.getProcess() != next.thread.getProcess()) {
                next.thread.getProcess().switchPageDirectory();
            }
            next.thread.loadCPU();
            currentThread = next;
        }
    }
}