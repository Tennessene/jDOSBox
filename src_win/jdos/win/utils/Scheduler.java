package jdos.win.utils;

import jdos.cpu.CPU_Regs;
import jdos.gui.Main;
import jdos.win.Win;
import jdos.win.builtin.ddraw.IDirectDrawSurface;

import java.awt.image.BufferedImage;
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
    private long start = System.currentTimeMillis();

    // DirectX surface to force to the screen
    public int monitor;

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

    private int currentTickCount() {
        return (int)(System.currentTimeMillis()-start);
    }
    public void sleep(WinThread thread, int ms) {
        SchedulerItem item = (SchedulerItem)threadMap.get(thread);
        item.sleepUntil =currentTickCount() + ms + 1;
        tick();
    }

    public void yield(WinThread thread) {
        tick();
        if (WinSystem.getCurrentThread() == thread) {
            // :TODO: should wake up early if we get some sort of input, like mouse, keyboard, network, window timer, etc
            try {Thread.sleep(25);} catch (Exception e){}
        }
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
                        tick();
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
        if (currentThread != null) {
            currentThread.thread.saveCPU();
        }
        currentThread = thread;
        currentThread.thread.loadCPU();
    }

    public WinThread getCurrentThread() {
        if (currentThread == null)
            return null;
        return currentThread.thread;
    }

    // :TODO: run them in order of process to minimize page swapping
    public void tick() {
        if (monitor != 0) {
            BufferedImage src = IDirectDrawSurface.getImage(monitor, true);
            Main.drawImage(src);
        }
        SchedulerItem next = currentThread.next;
        SchedulerItem start = currentThread;
        int tickCount = currentTickCount();
        int sleepAmount = 0;
        if (threadMap.size() == 0) {
            Win.panic("DEADLOCK out threads are waiting on an object indefinitely");
        }
        while (true) {
            if (next == null) {
                next = first;
            }
            if (next.sleepUntil <= tickCount) {
                break;
            }
            sleepAmount = Math.max(sleepAmount, next.sleepUntil - tickCount);
            if (next == start) {
                try {Thread.sleep(sleepAmount);} catch (Exception e) {}
                tickCount = currentTickCount();
            }
            next = next.next;
        }
        if (next != currentThread) {
            System.out.println("Switching threads: "+currentThread.thread.getHandle()+"("+Integer.toString(CPU_Regs.reg_eip, 16)+") -> "+next.thread.getHandle()+"("+Integer.toString(next.thread.cpuState.eip, 16)+")");
            currentThread.thread.saveCPU();
            if (currentThread.thread.getProcess() != next.thread.getProcess()) {
                next.thread.getProcess().switchPageDirectory();
            }
            next.thread.loadCPU();
            currentThread = next;
        }
    }
}
