package jdos.win.utils;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.kernel.*;

import java.awt.*;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Vector;

public class WinSystem {
    static public Scheduler scheduler;

    static private int nextObjectId;
    static private WinCallback callbacks;
    static public KernelMemory memory;
    static private DescriptorTables descriptorTables;
    static public Interrupts interrupts;
    static public Timer timer;

    static private Hashtable namedObjects;
    static Hashtable objects;

    static public int screenWidth = 640;
    static public int screenHeight = 480;
    static public int screenBpp = 32;
    static public int screenAddress;
    static public int screenPalette;

    static private long startTime = System.currentTimeMillis();

    static public void start() {
        nextObjectId = 0x2000;
        scheduler = new Scheduler();

        namedObjects = new Hashtable();
        objects = new Hashtable();

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

        new WinFile(WinFile.FILE_TYPE_CHAR, WinFile.STD_OUT);
        new WinFile(WinFile.FILE_TYPE_CHAR, WinFile.STD_IN);
        new WinFile(WinFile.FILE_TYPE_CHAR, WinFile.STD_ERROR);
        startTime = System.currentTimeMillis();
        Main.GFX_SetSize(screenWidth, screenHeight, false, false, false, 32);
    }

    static public int getScreenAddress() {
        if (screenAddress == 0)
            screenAddress = VideoMemory.mapVideoRAM(Pixel.getPitch(screenWidth, screenBpp)*screenWidth);
        return screenAddress;
    }

    static public int getMouseX() {
        return 0;
    }

    static public int getMouseY() {
        return 0;
    }
    static public int getTickCount() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    static public void removeObject(WinObject object) {
        namedObjects.remove(object);
        objects.remove(object);
    }

    static private Callback.Handler returnCallback = new Callback.Handler() {
        public String getName() {
            return "WinProc";
        }
        public int call() {
            return 1; // return from SendMessage
        }
    };

    private static int returnEip = 0;

    static public void call(int eip, int param1, int param2, int param3, int param4) {
        internalCall(eip, 4, param1, param2, param3, param4);
    }
    static public void call(int eip, int param1, int param2) {
        internalCall(eip, 2, param1, param2, 0, 0);
    }
    static private void internalCall(int eip, int paramCount, int param1, int param2, int param3, int param4) {
        if (returnEip == 0) {
            int callback = WinCallback.addCallback(returnCallback);
            returnEip =  WinSystem.getCurrentProcess().loader.registerFunction(callback);
        }
        int oldEsp = CPU_Regs.reg_esp.dword;
        if (paramCount>=4)
            CPU.CPU_Push32(param4);
        if (paramCount>=3)
            CPU.CPU_Push32(param3);
        if (paramCount>=2)
            CPU.CPU_Push32(param2);
        if (paramCount>=1)
            CPU.CPU_Push32(param1);
        CPU.CPU_Push32(returnEip);
        int saveEip = CPU_Regs.reg_eip;
        CPU_Regs.reg_eip = eip;
        Dosbox.DOSBOX_RunMachine();
        CPU_Regs.reg_eip = saveEip;
        CPU_Regs.reg_esp.dword = oldEsp;
    }

    static public WinObject getNamedObject(String name) {
        return (WinObject)namedObjects.get(name);
    }

    static public WinObject getObject(int handle) {
        return (WinObject)objects.get(new Integer(handle));
    }

    static public WinBrush createBrush(int color) {
        return new WinBrush(nextObjectId++, color);
    }

    static public FileMapping createFileMapping(int hFile, String name, long size) {
        FileMapping mapping = new FileMapping(hFile, name, size, nextObjectId++);
        if (name != null)
            WinSystem.namedObjects.put(name, mapping);
        return mapping;
    }

    static public WinEvent createEvent(String name, boolean manual, boolean set) {
        WinEvent event = new WinEvent(nextObjectId++, name, manual, set);
        if (name!=null)
            WinSystem.namedObjects.put(name, event);
        return event;
    }

    static public WinClass createClass() {
        return new WinClass(nextObjectId++);
    }

    static public WinBitmap createBitmap(int address) {
        return new WinBitmap(nextObjectId++, address);
    }

    static public WinObject createWinObject() {
        return new WinObject(nextObjectId++);
    }

    static public WaitObject createWaitObject() {
        return new WaitObject(nextObjectId++);
    }

    static int[] getScreenPalette() {
        if (screenPalette == 0)
            return null;
        int[] result = new int[256];
        for (int i=0;i<result.length;i++) {
            result[i] = Memory.mem_readd(screenPalette+i*4);
        }
        return result;
    }

    static public WinDC createDC(WinDC dc, int address, int width, int height, int[] palette) {
        int bpp = screenBpp;
        if (palette == null)
            palette = getScreenPalette();
        if (dc != null) {
            bpp = dc.bpp;
            palette = dc.palette;
        }
        return new WinDC(nextObjectId++, bpp, address, width, height, palette);
    }

    static public WinFont createFont(Font font) {
        return new WinFont(nextObjectId, font);
    }

    static public WinWindow createWindow(int dwExStyle, WinClass winClass, String name, int dwStyle, int x, int y, int cx, int cy, int hParent, int hMenu, int hInstance, int lpParam) {
        return new WinWindow(nextObjectId++, dwExStyle, winClass, name, dwStyle, x, y, cx, cy, hParent, hMenu, hInstance, lpParam);
    }

    static public WinPalette createPalette(int[] palette) {
        return new WinPalette(nextObjectId++, palette);
    }

    static public WinCursor loadCursor(int instance, int name) {
        WinCursor cursor = new WinCursor(nextObjectId++);
        return cursor;
    }

    static public WinIcon loadIcon(int instance, int name) {
        WinIcon icon = new WinIcon(nextObjectId++);
        return icon;
    }

    static public WinProcess getCurrentProcess() {
        if (scheduler != null) {
            WinThread currentThread = scheduler.getCurrentThread();
            if (currentThread!=null)
                return currentThread.getProcess();
        }
        return null;
    }

    static public WinThread getCurrentThread() {
        return scheduler.getCurrentThread();
    }

    static public WinThread createThread(WinProcess process, long startAddress, int stackSizeCommit, int stackSizeReserve, boolean primary) {
        return new WinThread(nextObjectId++, process, startAddress, stackSizeCommit, stackSizeReserve, primary);
    }

    static public WinMenu createMenu() {
        return new WinMenu(nextObjectId++);
    }

    static public WinProcess createProcess(String path, String commandLine, Vector paths, String workingDirectory) {
        WinProcess currentProcess = WinSystem.getCurrentProcess();
        WinProcess process = new WinProcess(nextObjectId++, memory, workingDirectory);
        process.switchPageDirectory();

        if (!process.load(path, commandLine, paths)) {
            process.close();
            return null;
        }
        if (currentProcess != null) {
            currentProcess.switchPageDirectory();
        }
        return process;
    }

    static public WinFile createFile(RandomAccessFile file, int shareMode, int attributes) {
        return new WinFile(nextObjectId++, file, shareMode, attributes);
    }
}
