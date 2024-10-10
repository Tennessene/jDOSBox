package jdos.win.builtin.kernel32;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.user32.GuiThreadInfo;
import jdos.win.builtin.user32.Input;
import jdos.win.builtin.user32.WinWindow;
import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.WinCallback;
import jdos.win.system.*;
import jdos.win.utils.Error;

import java.util.*;

public class WinThread extends WaitObject {
    static public WinThread create(WinProcess process, long startAddress, int stackSizeCommit, int stackSizeReserve, boolean primary) {
        return new WinThread(nextObjectId(), process, startAddress, stackSizeCommit, stackSizeReserve, primary);
    }

    static public WinThread get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinThread))
            return null;
        return (WinThread)object;
    }

    // HANDLE WINAPI GetCurrentThread(void);
    static public int GetCurrentThread() {
        return Scheduler.getCurrentThread().handle;
    }

    // DWORD WINAPI GetCurrentThreadId(void)
    static public int GetCurrentThreadId() {
        return Scheduler.getCurrentThread().handle;
    }

    // DWORD WINAPI GetLastError(void)
    static public int GetLastError() {
        return Scheduler.getCurrentThread().getLastError();
    }

    static public WinThread current() {
        return Scheduler.getCurrentThread();
    }

    static public final int THREAD_PRIORITY_IDLE = -15;
    static public final int THREAD_PRIORITY_LOWEST = -2;
    static public final int THREAD_PRIORITY_BELOW_NORMAL = -1;
    static public final int THREAD_PRIORITY_NORMAL = 0;
    static public final int THREAD_PRIORITY_ABOVE_NORMAL = 1;
    static public final int THREAD_PRIORITY_HIGHEST = 2;
    static public final int THREAD_PRIORITY_TIME_CRITICAL = 15;

    private WinProcess process;
    private int lastError = jdos.win.utils.Error.ERROR_SUCCESS;
    public CpuState cpuState = new CpuState();
    private KernelHeap stack;
    private int stackAddress;
    private int startAddress;
    private List<WinMsg> msgQueue = Collections.synchronizedList(new ArrayList<WinMsg>()); // synchronized since the keyboard will post message from another thread
    private List sendMsgQueue = new ArrayList();
    public Vector<WinWindow> windows = new Vector<WinWindow>();
    private Vector<Integer> paintList = new Vector<Integer>();
    private boolean quit = false;
    private WinTimer timer = new WinTimer(0);
    public int priority = THREAD_PRIORITY_NORMAL;
    public BitSet keyState;
    public int msg_window;
    final private WinEvent msgReady = WinEvent.create(null, true, true);
    private GuiThreadInfo guiInfo = new GuiThreadInfo();
    public int waitTime;
    public int waitTimeStart;
    public int currentGetMessageTime = 0;
    public int currentGetMessagePos = 0;
    public TIB tib;

    public GuiThreadInfo GetGUIThreadInfo() {
        return guiInfo;
    }

    private Callback.Handler startUp = new HandlerBase() {
        public String getName() {
            return "WinThread.start";
        }
        public void onCall() {
            process.loader.attachThread();
            CPU_Regs.reg_esp.dword = cpuState.esp;
            CPU_Regs.reg_eip = startAddress;
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
        stackSizeReserve=(stackSizeReserve+0xFFF) & ~0xFFF;
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
        this.startAddress = (int)startAddress;
        this.tib = new TIB(process, handle, this.cpuState.esp-stackSizeCommit, this.cpuState.esp);
        this.cpuState.fsPhys = this.tib.address;

        if (primary) {
            this.cpuState.eip = (int)startAddress;
        } else {
            // :TODO: this will leak
            int cb = WinCallback.addCallback(startUp);
            threadStarup = process.loader.registerFunction(cb);
            this.cpuState.eip = threadStarup;
        }
        Scheduler.addThread(this, false);
    }

    static public void setMessage(int address, int hWnd, int message, int wParam, int lParam, int time, int curX, int curY) {
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
        if (remove) {
            msg = msgQueue.remove(msgIndex);
        } else
            msg = msgQueue.get(msgIndex);
        if (msg.keyState != null)
            keyState = msg.keyState;
        setMessage(msgAddress, msg.hwnd, msg.message, msg.wParam, msg.lParam, msg.time, msg.x, msg.y);
        currentGetMessageTime = msg.time;
        currentGetMessagePos = msg.x | (msg.y << 16);
        return WinAPI.TRUE;
    }

    public BitSet getKeyState() {
        if (keyState == null)
            return WinKeyboard.keyState;
        return keyState;
    }

    public void setPriority(int nPriority) {

    }
    public void postMessage(int hWnd, int message, int wParam, int lParam) {
        msgQueue.add(new WinMsg(hWnd, message, wParam, lParam));
        synchronized(msgReady) {
            msgReady.set();
        }
    }

    public void postMessage(int hWnd, int message, int wParam, int lParam, BitSet keyState) {
        msgQueue.add(new WinMsg(hWnd, message, wParam, lParam, keyState));
        synchronized(msgReady) {
            msgReady.set();
        }
    }

    public boolean isCurrent() {
        return Scheduler.getCurrentThread()==this;
    }

    public int waitMessage() {
        Input.processInput();
        if (peekMessage(0, 0, 0, 0, 0) == WinAPI.FALSE) {
            synchronized(msgReady) {
                msgReady.reset();
                int time = timeUntilNextTimer();
                if (time == Integer.MAX_VALUE)
                    time = -1;
                else
                    time = time - WinSystem.getTickCount();
                return msgReady.wait(this, time);
            }
        }
        return WinAPI.TRUE;
    }

    public void paintReady() {
        msgReady.set();
    }

    private int timeUntilNextTimer() {
        int time = timer.getNextTimerTime();

        for (int i=0;i<windows.size();i++) {
            WinWindow window = windows.elementAt(i);
            int t = window.timer.getNextTimerTime();
            if (t<time)
                time = t;
        }
        return time;
    }

    private static void buildBackToFrontWindowList(WinWindow window, Vector<Integer> list) {
        list.add(window.handle);
        for (int j=window.children.size()-1;j>=0;j--) {
            WinWindow child = window.children.get(j);
            if ((child.dwStyle & WS_VISIBLE)==0)
                continue;
            WinRect intersection = new WinRect();
            WinRect childRect = child.rectWindow.copy();
            childRect.offset(window.rectWindow.left, window.rectWindow.top);
            if (intersection.intersect(window.invalidationRect, childRect))
                buildBackToFrontWindowList(child, list);
        }
    }

    public int peekMessage(int msgAddress, int hWnd, int minMsg, int maxMsg, int wRemoveMsg) {
        Input.processInput();
        if (quit)  {
            setMessage(msgAddress, 0, WinWindow.WM_QUIT, 0, 0, WinSystem.getTickCount(), StaticData.currentPos.x, StaticData.currentPos.y);
            return WinAPI.TRUE;
        }
        boolean remove = (wRemoveMsg & 0x0001)!= 0;
        for (int i=0;i<msgQueue.size();i++) {
            WinMsg msg = msgQueue.get(i);
            if (hWnd == 0 || msg.hwnd == hWnd) {
                if (minMsg == 0 && maxMsg == 0)
                    return getMessage(msgAddress, i, remove);
                if (msg.message>=minMsg && msg.message<=maxMsg)
                    return getMessage(msgAddress, i, remove);
            }
        }
        while (paintList.size() != 0) {
            int h;
            if (remove)
                h = paintList.remove(0);
            else
                h = paintList.firstElement();
            WinWindow window = WinWindow.get(h);
            if (window != null) {
                if (remove) {
                    window.validate();
                }
                setMessage(msgAddress, window.getHandle(), WinWindow.WM_PAINT, 0, 0, WinSystem.getTickCount(), StaticData.currentPos.x, StaticData.currentPos.y);
                return WinAPI.TRUE;
            }
        }
        for (int i=0;i<windows.size();i++) {
            WinWindow window = windows.elementAt(i);
            if (window.needsPainting()) {
                WinWindow parent = window.parent();
                while (parent != null) {
                    if (parent.needsPainting())
                        window = parent;
                    parent = parent.getParent();
                }
                if (remove) {
                    buildBackToFrontWindowList(window, paintList);
                }
                setMessage(msgAddress, window.getHandle(), WinWindow.WM_PAINT, 0, 0, WinSystem.getTickCount(), StaticData.currentPos.x, StaticData.currentPos.y);
                return WinAPI.TRUE;
            }
        }
        int time = WinSystem.getTickCount();
        if (timer.getNextTimerMsg(msgAddress, time, remove))
            return WinAPI.TRUE;

        for (int i=0;i<windows.size();i++) {
            WinWindow window = windows.elementAt(i);
            if (window.timer.getNextTimerMsg(msgAddress, time, remove))
                return WinAPI.TRUE;
        }
        return WinAPI.FALSE;
    }

    public int getNextMessage(int msgAddress, int hWnd, int minMsg, int maxMsg) {
        Input.processInput();
        if (quit) {
            return WinAPI.FALSE;
        }
        while (true) {
            if (peekMessage(msgAddress, hWnd, minMsg, maxMsg, 0x0001)==WinAPI.TRUE) {
                return WinAPI.TRUE;
            }
            if (waitMessage() == WAIT_SWITCH)
                return WAIT_SWITCH; // not looked at
        }
    }

    public WinMsg getLastMessage() {
        synchronized (msgQueue) {
            if (msgQueue.size()!=0)
                return msgQueue.get(msgQueue.size()-1);
        }
        return null;
    }

    public void sleep(int ms) {
        Scheduler.sleep(this, ms);
    }

    public void pushStack32(int value) {
        cpuState.esp-=4;
        Memory.mem_writed(cpuState.esp, value);
    }

    public void exit(int exitCode) {
        release();
        stack.deallocate();
        close();
        getProcess().freeAddress(stackAddress);
        Scheduler.removeThread(this);
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

    public int tlsAlloc() {
        WinProcess process = WinSystem.getCurrentProcess();
        if (process.freeTLS.size()>0)
            return process.freeTLS.remove(0);
        return process.tlsSize++;
    }

    public int tlsFree(int index) {
        int size = WinSystem.getCurrentProcess().tlsSize;
        if (index>=0 && index<size) {
            WinSystem.getCurrentProcess().freeTLS.add(index);
            return WinAPI.TRUE;
        }
        lastError = Error.ERROR_INVALID_PARAMETER;
        return WinAPI.FALSE;
    }

    public int tlsSetValue(int index, int value) {
        int size = WinSystem.getCurrentProcess().tlsSize;
        if (index>=0 && index<size) {
            if (index*4>=tib.tlsSize) {
                Win.panic("Need to implement growing TLS");
            }
            writed(tib.tls+index*4, value);
            return WinAPI.TRUE;
        }
        lastError = Error.ERROR_INVALID_PARAMETER;
        return WinAPI.FALSE;
    }

    public int tlsGetValue(int index) {
        int size = WinSystem.getCurrentProcess().tlsSize;
        if (index>=0 && index<size) {
            if (index*4>=tib.tlsSize) {
                Win.panic("Need to implement growing TLS");
            }
            return readd(tib.tls+index*4);
        }
        lastError = Error.ERROR_INVALID_PARAMETER;
        return WinAPI.FALSE;
    }
}
