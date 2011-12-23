package jdos.win.utils;

import jdos.cpu.CPU_Regs;
import jdos.win.kernel.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class WinSystem {
    static private Scheduler scheduler;
    static private Hashtable processes;

    static private int nextProcessId;
    static private int nextThreadId;
    static private WinCallback callbacks;
    static private KernelMemory memory;
    static private DescriptorTables descriptorTables;
    static public Interrupts interrupts;
    static public Timer timer;

    static public void start() {
        nextProcessId = 0x2000;
        nextThreadId = 0x3000;
        scheduler = new Scheduler();
        processes = new Hashtable();

        memory = new KernelMemory();
        WinCallback.start(memory);
        interrupts = new Interrupts(memory);
        descriptorTables = new DescriptorTables(interrupts, memory);
        timer = new Timer(50); // 50MHz timer

        final int stackSize = 16*1024;
        int stackEnd = memory.kmalloc(stackSize);
        CPU_Regs.reg_esp.dword = stackEnd+stackSize;

        //memory.registerPageFault(interrupts);
        //memory.initialise_paging();
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
