package jdos.win.utils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class WinSystem {
    static private Scheduler scheduler;
    static private Hashtable processes;

    static private int nextProcessId;
    static private int nextThreadId;

    static public void start() {
        nextProcessId = 0x2000;
        nextThreadId = 0x3000;
        scheduler = new Scheduler();
        processes = new Hashtable();
    }

    static public void exit() {
        Enumeration e = processes.elements();
        while (e.hasMoreElements()) {
            WinProcess process = (WinProcess)e.nextElement();
            process.exit();
        }
        scheduler = null;
        processes = null;
    }
    static public WinProcess getCurrentProcess() {
        return scheduler.getCurrentThread().getProcess();
    }

    static public WinThread getCurrentThread() {
        return scheduler.getCurrentThread();
    }

    static public WinThread createThread(WinProcess process, long startAddress) {
        WinThread thread = new WinThread(nextThreadId++, process, startAddress);
        scheduler.addThread(thread);
        return thread;
    }

    static public int createProcess(String path, String[] args, Vector paths) {
        WinProcess process = new WinProcess();
        if (!process.load(nextProcessId++, path, args, paths))
            return 0;

        processes.put(new Integer(process.getHandle()), process);
        return process.getHandle();
    }
}
