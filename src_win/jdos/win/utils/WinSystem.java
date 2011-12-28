package jdos.win.utils;

import jdos.cpu.CPU_Regs;
import jdos.win.kernel.*;

import java.util.Hashtable;
import java.util.Vector;

public class WinSystem {
    static public Scheduler scheduler;
    static private Hashtable processes;

    static private int nextObjectId;
    static private WinCallback callbacks;
    static public KernelMemory memory;
    static private DescriptorTables descriptorTables;
    static public Interrupts interrupts;
    static public Timer timer;

    static private Hashtable namedObjects = new Hashtable();
    static private Hashtable objects = new Hashtable();

    static public void start() {
        nextObjectId = 0x2000;
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
        memory.initialise_paging();
    }

    static public WinObject getNamedObject(String name) {
        return (WinObject)namedObjects.get(name);
    }

    static public WinObject getObject(int handle) {
        return (WinObject)objects.get(new Integer(handle));
    }

    static public FileMapping createFileMapping(int hFile, String name) {
        FileMapping mapping = new FileMapping(hFile, name, nextObjectId++);
        if (name != null)
                WinSystem.namedObjects.put(name, mapping);
        WinSystem.objects.put(new Integer(mapping.handle), mapping);
        return mapping;
    }

    static public WinProcess getCurrentProcess() {
        return scheduler.getCurrentThread().getProcess();
    }

    static public WinThread getCurrentThread() {
        return scheduler.getCurrentThread();
    }

    static public WinThread createThread(WinProcess process, long startAddress, int stackSizeCommit, int stackSizeReserve) {
        WinThread thread = new WinThread(nextObjectId++, process, startAddress, stackSizeCommit, stackSizeCommit);
        scheduler.addThread(thread);
        objects.put(new Integer(thread.handle), thread);
        return thread;
    }

    static public int createProcess(String path, String[] args, Vector paths) {
        WinProcess process = new WinProcess(nextObjectId++, memory);
        process.switchPageDirectory();
        if (!process.load(path, args, paths))
            return 0;
        objects.put(new Integer(process.handle), process);
        processes.put(new Integer(process.getHandle()), process);
        return process.getHandle();
    }
}
