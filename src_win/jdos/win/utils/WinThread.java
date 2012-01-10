package jdos.win.utils;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.WinCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class WinThread extends WaitObject {
    static public final int THREAD_PRIORITY_IDLE = -15;
    static public final int THREAD_PRIORITY_LOWEST = -2;
    static public final int THREAD_PRIORITY_BELOW_NORMAL = -1;
    static public final int THREAD_PRIORITY_NORMAL = 0;
    static public final int THREAD_PRIORITY_ABOVE_NORMAL = 1;
    static public final int THREAD_PRIORITY_HIGHEST = 2;
    static public final int THREAD_PRIORITY_TIME_CRITICAL = 15;

    private Vector tls = new Vector();
    private WinProcess process;
    private int lastError = Error.ERROR_SUCCESS;
    public CpuState cpuState = new CpuState();
    private KernelHeap stack;
    private int stackAddress;
    private List msgQueue = Collections.synchronizedList(new ArrayList()); // synchronized since the keyboard will post message from another thread
    Vector windows = new Vector();
    private boolean quit = false;
    private WinTimer timer = new WinTimer(0);
    public int priority = THREAD_PRIORITY_NORMAL;

    private Callback.Handler startUp = new HandlerBase() {
        public String getName() {
            return "WinThread.start";
        }
        public void onCall() {
            process.loader.attachThread();
            if (CPU_Regs.reg_esp.dword != cpuState.esp) {
                Win.panic("Wasn't expecting DllMain to mess up stack");
            }
            CPU_Regs.reg_eip = stackAddress;
        }
    };

    private int threadStarup = 0;

    public WinThread(int handle, WinProcess process, long startAddress, int stackSizeCommit, int stackSizeReserve, boolean primary) {
        super(handle);
        final int guard = 0x1000;

        if (stackSizeCommit <= 0)
            stackSizeCommit = stackSizeReserve;
        if (stackSizeCommit <= 0) {
            stackSizeCommit = (int)process.loader.main.header.imageOptional.SizeOfStackCommit;
            stackSizeReserve = (int)process.loader.main.header.imageOptional.SizeOfStackReserve;
            if (stackSizeCommit <= 0)
                stackSizeCommit = stackSizeReserve;
        }
        if (stackSizeReserve<stackSizeCommit)
            stackSizeReserve = stackSizeCommit;
        // :TODO: remove this line once we have a stack that can grow
        stackSizeCommit = stackSizeReserve;
        stackAddress = process.reserveStackAddress(stackSizeReserve+guard*2);
        int start = stackAddress;
        int end = start+stackSizeReserve+guard*2;

        this.cpuState.esp = end - guard;
        start = end-stackSizeCommit-guard*2;
        System.out.println("Creating Thread: stack size: "+stackSizeCommit+" ("+Integer.toHexString(start)+"-"+Integer.toHexString(end)+")");
        // :TODO: implement a page fault handler to grow stack as necessary
        // :TODO: need a stack heap that grows down
        this.stack = new KernelHeap(process.kernelMemory, process.page_directory, start, end, end, false, false);
        this.process = process;

        if (primary) {
            this.cpuState.eip = (int)startAddress;
        } else {
            // :TODO: this will leak
            int cb = WinCallback.addCallback(startUp);
            threadStarup = process.loader.registerFunction(cb);
            this.cpuState.eip = threadStarup;
        }
        WinSystem.scheduler.addThread(this, false);
    }

    static void setMessage(int address, int hWnd, int message, int wParam, int lParam, int time, int curX, int curY) {
        Memory.mem_writed(address, hWnd);
        Memory.mem_writed(address+4, message);
        Memory.mem_writed(address+8, wParam);
        Memory.mem_writed(address+12, lParam);
        Memory.mem_writed(address+16, time);
        Memory.mem_writed(address+20, curX);
        Memory.mem_writed(address+24, curY);
    }

    private int getMessage(int msgAddress, int msgIndex, boolean remove) {
        if (msgAddress == 0)
            return WinAPI.TRUE;
        WinMsg msg;
        if (remove)
            msg = (WinMsg)msgQueue.remove(msgIndex);
        else
            msg = (WinMsg)msgQueue.get(msgIndex);
        setMessage(msgAddress, msg.hwnd, msg.message, msg.wParam, msg.lParam, msg.time, msg.x, msg.y);
        return WinAPI.TRUE;
    }

    public void setPriority(int nPriority) {

    }
    public void postMessage(int hWnd, int message, int wParam, int lParam) {
        msgQueue.add(new WinMsg(hWnd, message, wParam, lParam));
    }

    public int waitMessage() {
        while (peekMessage(0, 0, 0, 0, 0) == WinAPI.FALSE) {
            // :TODO: put thread to sleep until new msg or timer or paint or quit
            try {Thread.sleep(25);} catch (Exception e) {}
        }
        return WinAPI.TRUE;
    }

    public int peekMessage(int msgAddress, int hWnd, int minMsg, int maxMsg, int wRemoveMsg) {
        if (quit)  {
            setMessage(msgAddress, 0, WinWindow.WM_QUIT, 0, 0, WinSystem.getTickCount(), WinSystem.getMouseX(), WinSystem.getMouseY());
            return WinAPI.TRUE;
        }
        boolean remove = (wRemoveMsg & 0x0001)!= 0;
        for (int i=0;i<msgQueue.size();i++) {
            WinMsg msg = (WinMsg)msgQueue.get(i);
            if (hWnd == 0 || msg.hwnd == hWnd) {
                if (minMsg == 0 && maxMsg == 0)
                    return getMessage(msgAddress, i, remove);
                if (msg.message>=minMsg && msg.message<=maxMsg)
                    return getMessage(msgAddress, i, remove);
            }
        }
        for (int i=0;i<windows.size();i++) {
            WinWindow window = (WinWindow)windows.elementAt(i);
            if (window.needsPainting) {
                if (remove)
                    window.needsPainting = false;
                setMessage(msgAddress, window.getHandle(), WinWindow.WM_PAINT, 0, 0, WinSystem.getTickCount(), WinSystem.getMouseX(), WinSystem.getMouseY());
                return WinAPI.TRUE;
            }
        }
        int time = WinSystem.getTickCount();
        if (timer.getNextTimerMsg(msgAddress, time, remove))
            return WinAPI.TRUE;

        for (int i=0;i<windows.size();i++) {
            WinWindow window = (WinWindow)windows.elementAt(i);
            if (window.timer.getNextTimerMsg(msgAddress, time, remove))
                return WinAPI.TRUE;
        }
        return WinAPI.FALSE;
    }

    public int getNextMessage(int msgAddress, int hWnd, int minMsg, int maxMsg) {
        while (true) {
            if (quit)
                return WinAPI.FALSE;
            if (peekMessage(msgAddress, hWnd, minMsg, maxMsg, 0x0001)==WinAPI.TRUE)
                return WinAPI.TRUE;
            // :TODO: put thread to sleep until new msg or timer or paint or quit
            try {Thread.sleep(25);} catch (Exception e) {}
        }
    }

    public void sleep(int ms) {
        WinSystem.scheduler.sleep(this, ms);
    }

    public void pushStack32(int value) {
        cpuState.esp-=4;
        Memory.mem_writed(cpuState.esp, value);
    }

    public void exit(int exitCode) {
        release();
        WinSystem.scheduler.removeThread(this, false);
        stack.deallocate();
        close();
        getProcess().freeAddress(stackAddress);
    }

    public void loadCPU() {
        cpuState.load();
    }

    public void saveCPU() {
        cpuState.save();
    }

    public int getLastError() {
        return lastError;
    }

    public void setLastError(int error) {
        lastError = error;
    }

    public WinProcess getProcess() {
        return process;
    }

    private static class TLS {
        public int add=0;
    }
    public int tlsAlloc() {
        for (int i=0;i<tls.size();i++) {
            if (tls.elementAt(i) == null)
                return i;
        }
        tls.add(new TLS());
        return tls.size()-1;
    }
    public int tlsFree(int index) {
        if (index>=0 && index<tls.size() && tls.elementAt(index)!=null) {
            tls.setElementAt(null, index);
            return WinAPI.TRUE;
        }
        lastError = Error.ERROR_INVALID_PARAMETER;
        return WinAPI.FALSE;
    }
    public int tlsSetValue(int index, int value) {
        if (index>=0 && index<tls.size() && tls.elementAt(index)!=null) {
            ((TLS)tls.elementAt(index)).add = value;
            return WinAPI.TRUE;
        }
        lastError = Error.ERROR_INVALID_PARAMETER;
        return WinAPI.FALSE;
    }
    public int tlsGetValue(int index) {
        if (index>=0 && index<tls.size() && tls.elementAt(index)!=null) {
            return ((TLS)tls.elementAt(index)).add;
        }
        lastError = Error.ERROR_INVALID_PARAMETER;
        return WinAPI.FALSE;
    }
}
