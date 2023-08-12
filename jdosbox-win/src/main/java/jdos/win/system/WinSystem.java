package jdos.win.system;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.gui.Main;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.kernel.*;
import jdos.win.utils.Pixel;

import java.awt.image.BufferedImage;

public class WinSystem {
    static private WinCallback callbacks;
    static public KernelMemory memory;
    static private DescriptorTables descriptorTables;
    static public Interrupts interrupts;
    static public Timer timer;

    static private long startTime = System.currentTimeMillis();

    static public WinRegistry registry;

    static public void start() {
        registry = new WinRegistry();

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
        setScreenSize(640, 480, 32);
        StaticData.init();

        new WinFile(WinFile.FILE_TYPE_CHAR, WinFile.STD_OUT);
        new WinFile(WinFile.FILE_TYPE_CHAR, WinFile.STD_IN);
        new WinFile(WinFile.FILE_TYPE_CHAR, WinFile.STD_ERROR);
        startTime = System.currentTimeMillis();
    }

    static public JavaBitmap getScreen() {
        return StaticData.screen;
    }

    static public int getScreenWidth() {
        return StaticData.screen.getWidth();
    }

    static public int getScreenHeight() {
        return StaticData.screen.getHeight();
    }

    static public int getScreenBpp() {
        return StaticData.screen.getBpp();
    }

    static public void setScreenSize(int dwWidth, int dwHeight, int dwBPP) {
        if (StaticData.screen == null ||  dwWidth != StaticData.screen.getWidth() || dwHeight != StaticData.screen.getHeight() || StaticData.screen.getBpp() != dwBPP) {
            int[] palette = null;

            if (StaticData.screen != null) {
                palette = StaticData.screen.getPalette();
                StaticData.screen.close();
            }
            if (palette == null) {
                palette = JavaBitmap.getDefaultPalette();
            }
            BufferedImage bi = Pixel.createImage(0, dwBPP,  palette, dwWidth, dwHeight, false);
            if (StaticData.screen == null)
                StaticData.screen = new JavaBitmap(bi, dwBPP, dwWidth, dwHeight, JavaBitmap.getDefaultPalette());
            else
                StaticData.screen.set(bi, dwBPP, dwWidth, dwHeight, JavaBitmap.getDefaultPalette()); // existing dc's will be point to screen, so update it instead of assigning a new one
            Main.GFX_SetSize(dwWidth, dwHeight, dwWidth, dwHeight, false, dwBPP);
        }
    }

    static public int getTickCount() {
        return (int)(System.currentTimeMillis() - startTime);
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

    static public void call(int eip, int param1, int param2, int param3, int param4, int param5) {
        internalCall(eip, 5, param1, param2, param3, param4, param5);
    }
    static public void call(int eip, int param1, int param2, int param3, int param4) {
        internalCall(eip, 4, param1, param2, param3, param4, 0);
    }
    static public void call(int eip, int param1, int param2, int param3) {
        internalCall(eip, 3, param1, param2, param3, 0, 0);
    }
    static public void call(int eip, int param1, int param2) {
        internalCall(eip, 2, param1, param2, 0, 0, 0);
    }
    static private void internalCall(int eip, int paramCount, int param1, int param2, int param3, int param4, int param5) {
        if (returnEip == 0) {
            int callback = WinCallback.addCallback(returnCallback);
            returnEip =  WinSystem.getCurrentProcess().loader.registerFunction(callback);
        }
        int oldEsp = CPU_Regs.reg_esp.dword;
        if (paramCount>=5)
            CPU.CPU_Push32(param5);
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

    static public WinProcess getCurrentProcess() {
        WinThread currentThread = Scheduler.getCurrentThread();
        if (currentThread!=null)
            return currentThread.getProcess();
        return null;
    }
}
