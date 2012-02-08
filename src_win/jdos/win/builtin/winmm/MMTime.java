package jdos.win.builtin.winmm;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinEvent;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.kernel.WinCallback;
import jdos.win.system.Scheduler;
import jdos.win.system.WinSystem;

import java.util.Vector;

public class MMTime extends WinAPI {
    static final public int MMSYSTIME_MININTERVAL = 1;
    static final public int MMSYSTIME_MAXINTERVAL = 65535;

    static final public int TIMERR_BASE =       96;
    static final public int TIMERR_NOERROR =    0;
    static final public int TIMERR_NOCANDO =    TIMERR_BASE+1;

    static final public int TIME_ONESHOT =              0x0000;	/* program timer for single event */
    static final public int TIME_PERIODIC =             0x0001;	/* program for continuous periodic event */
    static final public int TIME_CALLBACK_FUNCTION =    0x0000;	/* callback is function */
    static final public int TIME_CALLBACK_EVENT_SET =   0x0010;	/* callback is event - use SetEvent */
    static final public int TIME_CALLBACK_EVENT_PULSE = 0x0020;	/* callback is event - use PulseEvent */
    static final public int TIME_KILL_SYNCHRONOUS =     0x0100;

    static private Callback.Handler mmTimerThread = new HandlerBase() {
        public String getName() {
            return "mmTimerThread";
        }
        public void onCall() {
            int eip = CPU.CPU_Pop32();
            int esp = CPU.CPU_Pop32();
            int id = CPU.CPU_Pop32();
            int threadHandle = CPU.CPU_Pop32();
            int callback = CPU.CPU_Pop32();
            int dwUser = CPU.CPU_Pop32();
            int dwDelay = CPU.CPU_Pop32();

            WinThread thread = WinThread.get(threadHandle);

            WinSystem.call(callback, id, 0, dwUser, 0, 0);
            CPU_Regs.reg_eip = eip;
            CPU_Regs.reg_esp.dword = esp;
            if (dwDelay == 0) {
                Scheduler.removeThread(thread);
                timers.set(id, null);
            } else
                Scheduler.sleep(thread, dwDelay);
        }
    };

    static private class MMTimer extends Thread {
        int delay;
        int callback;
        int dwUser;
        int flags;
        int id;
        final WinThread thread;
        boolean bExit = false;

        public MMTimer(int id, int delay, int callback, int dwUser, int flags) {
            this.delay = delay;
            this.callback = callback;
            this.dwUser = dwUser;
            this.flags = flags;
            this.id = id;
            if ((flags & TIME_CALLBACK_EVENT_SET)==0 && (flags & TIME_CALLBACK_EVENT_PULSE)==0) {
                WinProcess process = WinSystem.getCurrentProcess();
                if (process.mmTimerThreadEIP == 0) {
                    int cb = WinCallback.addCallback(mmTimerThread);
                    process.mmTimerThreadEIP = process.loader.registerFunction(cb);
                }
                this.thread = WinThread.create(process, process.mmTimerThreadEIP,  4096, 4096, true); // primary=true so that we don't call dllmain's with this thread
                int esp = thread.cpuState.esp;
                thread.pushStack32((flags & TIME_PERIODIC)==0?delay:0);
                thread.pushStack32(dwUser);
                thread.pushStack32(callback);
                thread.pushStack32(thread.handle);
                thread.pushStack32(id);
                thread.pushStack32(esp);
                thread.pushStack32(thread.cpuState.eip);
                thread.pushStack32(0); // bogus callback return address
                Scheduler.addThread(thread, false);
                Scheduler.sleep(thread, delay);
            } else {
                this.thread = null;
                this.start();
            }
        }

        public void run() {
            while(!bExit) {
                try {sleep(delay);} catch (Exception e) {}
                if (!bExit) {
                    WinEvent event = WinEvent.get(callback);
                    if (event == null)
                        continue;
                    if ((flags & TIME_CALLBACK_EVENT_SET)!=0) {
                        event.set();
                    } else {
                        event.pulse();
                    }
                }
                if ((flags & TIME_PERIODIC)==0)
                    break;
            }
            timers.remove(this);
        }
    }

    static private Vector<MMTimer> timers = new Vector<MMTimer>();

    // MMRESULT timeBeginPeriod(UINT uPeriod)
    static public int timeBeginPeriod(int wPeriod) {
        if (wPeriod < MMSYSTIME_MININTERVAL || wPeriod > MMSYSTIME_MAXINTERVAL)
            return TIMERR_NOCANDO;

        if (wPeriod > MMSYSTIME_MININTERVAL) {
            log("Stub; we set our timer resolution at minimum\n");
        }

        return 0;
    }

    // DWORD timeGetTime(void)
    static public int timeGetTime() {
        return WinSystem.getTickCount();
    }

    // MMRESULT timeSetEvent(UINT uDelay, UINT uResolution, LPTIMECALLBACK lpTimeProc, DWORD_PTR dwUser, UINT fuEvent)
    static public int timeSetEvent(int uDelay, int uResolution, int lpTimeProc, int dwUser, int fuEvent) {
        if (uDelay < MMSYSTIME_MININTERVAL || uDelay > MMSYSTIME_MAXINTERVAL)
	        return 0;
        while (timers.size()>0 && timers.get(timers.size()-1)==null) {
            timers.remove(timers.size() -1);
        }
        MMTimer timer = new MMTimer(timers.size(), uDelay, lpTimeProc, dwUser, fuEvent);
        timers.add(timer);
        return timer.id;
    }
}
