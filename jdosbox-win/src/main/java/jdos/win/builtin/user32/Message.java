package jdos.win.builtin.user32;

import jdos.cpu.CPU_Regs;
import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.directx.ddraw.IDirectDrawSurface;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.system.Scheduler;
import jdos.win.system.StaticData;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

public class Message extends WinAPI {
    // LRESULT WINAPI DispatchMessage(const MSG *lpmsg)
    static public int DispatchMessageA(int lpmsg) {
        int hWnd = readd(lpmsg);
        int msg = readd(lpmsg+4);
        int wParam = readd(lpmsg+8);
        int lParam = readd(lpmsg+12);
        int result = call_window_proc(hWnd, msg, wParam, lParam, false);
        if (msg == WinWindow.WM_PAINT) {
            if (Scheduler.monitor != 0) {
                Main.drawImage(IDirectDrawSurface.getImage(Scheduler.monitor, true).getImage());
            }
        }
        return result;
    }

    // BOOL WINAPI GetMessage(LPMSG lpMsg, HWND hWnd, UINT wMsgFilterMin, UINT wMsgFilterMax)
    static public int GetMessageA(int lpMsg, int hWnd, int wMsgFilterMin, int wMsgFilterMax) {
        return Scheduler.getCurrentThread().getNextMessage(lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax);
    }

    // DWORD WINAPI GetMessagePos(void);
    static public int GetMessagePos() {
        return Scheduler.getCurrentThread().currentGetMessagePos;
    }

    // LONG WINAPI GetMessageTime(void)
    static public int GetMessageTime() {
        return Scheduler.getCurrentThread().currentGetMessageTime;
    }

    // BOOL WINAPI MessageBeep(UINT uType)
    static public int MessageBeep(int uType) {
        return TRUE;
    }

    // BOOL WINAPI PeekMessage(LPMSG lpMsg, HWND hWnd, UINT wMsgFilterMin, UINT wMsgFilterMax, UINT wRemoveMsg)
    static public int PeekMessageA(int lpMsg, int hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg) {
        return Scheduler.getCurrentThread().peekMessage(lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax, wRemoveMsg);
    }

    // BOOL WINAPI PostMessage(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    static public int PostMessageA(int hWnd, int Msg, int wParam, int lParam) {
        if (WinWindow.get(hWnd) == null)
            return FALSE;
        if (hWnd == 0xFFFF)
            Win.panic("Broadcast PostMessage not implemented yet");
        Scheduler.getCurrentThread().postMessage(hWnd, Msg, wParam, lParam);
        return TRUE;
    }

    // If the message is successfully registered, the return value is a message identifier in the range 0xC000 through 0xFFFF
    // UINT WINAPI RegisterWindowMessage(LPCTSTR lpString)
    static public int RegisterWindowMessageA(int lpString) {
        String name = StringUtil.getString(lpString);
        Integer result = StaticData.registeredMessages.get(name);
        if (result != null)
            return result;
        int value = StaticData.nextRegisteredMessage++;
        StaticData.registeredMessages.put(name, value);
        return value;
    }

    // LRESULT WINAPI SendMessageA( HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam )
    static public int SendMessageA(int hWnd, int msg, int wParam, int lParam) {
        // :TODO: broadcast message
        return call_window_proc(hWnd, msg, wParam, lParam, true);
    }

    // UINT_PTR WINAPI SetTimer(HWND hWnd, UINT_PTR nIDEvent, UINT uElapse, TIMERPROC lpTimerFunc)
    static public int SetTimer(int hWnd, int nIDEvent, int uElapse, int lpTimerFunc) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        return window.timer.addTimer(uElapse, nIDEvent, lpTimerFunc);
    }

    // BOOL WINAPI TranslateMessage(const MSG *lpMsg)
    static public int TranslateMessage(int lpMsg) {
        int message = Memory.mem_readd(lpMsg+4);
        if (message == WinWindow.WM_KEYDOWN || message == WinWindow.WM_KEYUP) {
            if (message == WinWindow.WM_KEYDOWN) {
                int key = readd(lpMsg+8);
                if (key>=32 && key<=126) {
                    if (!Scheduler.getCurrentThread().getKeyState().get(VK_SHIFT))
                        key = StringUtil.tolowerW((char)key);
                    PostMessageA(readd(lpMsg), WM_CHAR, key, readd(lpMsg+12));
                }
            }
            return TRUE;
        } else if (message == WinWindow.WM_SYSKEYDOWN || message == WinWindow.WM_SYSKEYUP) {
            return TRUE;
        } else {
            return FALSE;
        }
    }

    // DWORD WINAPI WaitForInputIdle(HANDLE hProcess, DWORD dwMilliseconds)
    static public int WaitForInputIdle(int hProcess, int dwMilliseconds) {
        WinProcess process = WinProcess.get(hProcess);
        if (process == null)
            return WAIT_FAILED;
        return process.readyForInput.wait(Scheduler.getCurrentThread(), dwMilliseconds);
    }

    // BOOL WINAPI WaitMessage(void)
    static public int WaitMessage() {
        return Scheduler.getCurrentThread().waitMessage();
    }

//    typedef struct
//    {
//      LPARAM        lParam;
//      WPARAM      wParam;
//      UINT        message;
//      HWND        hwnd;
//    } CWPSTRUCT

//    typedef struct
//    {
//      LRESULT       lResult;
//      LPARAM        lParam;
//      WPARAM      wParam;
//      DWORD         message;
//      HWND        hwnd;
//    } CWPRETSTRUCT
    static private int call_window_proc(int hwnd, int msg, int wparam, int lparam, boolean same_thread) {
        /* first the WH_CALLWNDPROC hook */
        int cwp = getTempBuffer(16);
        writed(cwp, lparam);
        writed(cwp+4, wparam);
        writed(cwp+8, msg);
        writed(cwp + 12, hwnd);
        Hook.HOOK_CallHooks(WH_CALLWNDPROC, HC_ACTION, BOOL(same_thread), cwp);

        WinWindow wndPtr = WinWindow.get(hwnd);
        if (wndPtr == null)
            return 0;

        if (wndPtr.getThread() != Scheduler.getCurrentThread())
            Win.panic("Need to implement intra thread SendMessage");

        /* now call the window procedure */
        WinSystem.call(wndPtr.winproc, hwnd, msg, wparam, lparam);
        int result = CPU_Regs.reg_eax.dword;

        /* and finally the WH_CALLWNDPROCRET hook */
        int cwpret = getTempBuffer(20);
        writed(cwpret, result);
        writed(cwpret+4, lparam);
        writed(cwpret+8, wparam);
        writed(cwpret+12, msg);
        writed(cwpret + 16, hwnd);
        Hook.HOOK_CallHooks(WH_CALLWNDPROCRET, HC_ACTION, BOOL(same_thread), cwpret);
        return result;
    }
}
