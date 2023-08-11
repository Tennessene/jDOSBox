package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.system.Scheduler;
import jdos.win.system.StaticData;

import java.util.Iterator;

public class Focus extends WinAPI {
    // HWND WINAPI GetActiveWindow(void)
    public static int GetActiveWindow() {
        return Scheduler.getCurrentThread().GetGUIThreadInfo().hwndActive;
    }

    // HWND WINAPI GetFocus(void)
    public static int GetFocus() {
        return Scheduler.getCurrentThread().GetGUIThreadInfo().hwndFocus;
    }

    // HWND WINAPI GetForegroundWindow(void)
    public static int GetForegroundWindow() {
        return StaticData.foregroundWindow;
    }

    // HWND WINAPI SetActiveWindow(HWND hWnd)
    public static int SetActiveWindow(int hWnd) {
        if (hWnd!=0) {
            int style = WinWindow.GetWindowLongA(hWnd, GWL_STYLE);

            if ((style & (WS_POPUP|WS_CHILD)) == WS_CHILD)
                return GetActiveWindow();  /* Windows doesn't seem to return an error here */
        }
        IntRef previous = new IntRef(0);
        set_active_window(hWnd, previous, false, true);
        return previous.value;
    }

    // HWND WINAPI SetFocus( HWND hwnd )
    public static int SetFocus(int hwnd) {
        int hwndTop = hwnd;
        int previous = GetFocus();

        if (hwnd != 0) {
            /* Check if we can set the focus to this window */
            if (hwnd == previous) return previous;  /* nothing to do */
            for (; ; ) {
                int parent;
                int style = WinWindow.GetWindowLongA(hwndTop, GWL_STYLE);
                if ((style & (WS_MINIMIZE | WS_DISABLED)) != 0) return 0;
                parent = WinWindow.GetAncestor(hwndTop, GA_PARENT);
                if (parent == 0) {
                    if ((style & (WS_POPUP | WS_CHILD)) == WS_CHILD) return 0;
                    break;
                }
                if (parent == WinThread.current().msg_window) return 0;
                hwndTop = parent;
            }

            /* call hooks */
            if (Hook.HOOK_CallHooks(WH_CBT, HCBT_SETFOCUS, hwnd, previous) != 0) return 0;

            /* activate hwndTop if needed. */
            if (hwndTop != GetActiveWindow()) {
                if (!set_active_window(hwndTop, null, false, false)) return 0;
                if (WinWindow.IsWindow(hwnd)==0) return 0;  /* Abort if window destroyed */

                /* Do not change focus if the window is no longer active */
                if (hwndTop != GetActiveWindow()) return 0;
            }
        } else /* NULL hwnd passed in */ {
            if (previous == 0) return 0;  /* nothing to do */
            if (Hook.HOOK_CallHooks(WH_CBT, HCBT_SETFOCUS, 0, previous) != 0) return 0;
        }

        /* change focus and send messages */
        return set_focus_window(hwnd);
    }

    // BOOL WINAPI SetForegroundWindow(HWND hWnd)
    static public int SetForegroundWindow(int hWnd) {
        if (StaticData.foregroundWindow == hWnd)
            return TRUE;
        StaticData.foregroundWindow = hWnd;
        return BOOL(set_active_window(hWnd, null, false, true));
    }

    static private int set_focus_window(int hwnd) {
        int previous = GetFocus();
        if (previous == hwnd) return previous;
        Scheduler.getCurrentThread().GetGUIThreadInfo().hwndFocus = hwnd;
        if (previous != 0) {
            Message.SendMessageA(previous, WM_KILLFOCUS, hwnd, 0);
            if (GetFocus() != hwnd) return previous; /* changed by the message */
        }
        if (WinWindow.IsWindow(hwnd)!=0) {
            Message.SendMessageA(hwnd, WM_SETFOCUS, previous, 0);
        }
        return previous;
    }

    static private boolean set_active_window(int hwnd, IntRef prev, boolean mouse, boolean focus) {
        int previous = GetActiveWindow();
        boolean ret;

        if (previous == hwnd) {
            if (prev != null)
                prev.value = hwnd;
            return true;
        }

        int cbt = getTempBuffer(8);
        /* call CBT hook chain */
        Memory.mem_writed(cbt, BOOL(mouse));
        Memory.mem_writed(cbt + 4, BOOL(mouse));
        if (Hook.HOOK_CallHooks(WH_CBT, HCBT_ACTIVATE, hwnd, cbt) != 0) {
            return false;
        }
        if (WinWindow.IsWindow(previous)!=0) {
            Message.SendMessageA(previous, WM_NCACTIVATE, FALSE, hwnd);
            Message.SendMessageA(previous, WM_ACTIVATE, MAKEWPARAM(WA_INACTIVE, WinPos.IsIconic(previous)), hwnd);
        }

        Scheduler.getCurrentThread().GetGUIThreadInfo().hwndActive = hwnd;

        if (hwnd != 0) {
            /* send palette messages */
            if (Message.SendMessageA(hwnd, WM_QUERYNEWPALETTE, 0, 0) != 0)
                Message.SendMessageA(HWND_BROADCAST, WM_PALETTEISCHANGING, hwnd, 0);
            if (WinWindow.IsWindow(hwnd)==0) return false;
        }

        int old_thread = previous!=0 ? WinWindow.GetWindowThreadProcessId(previous, 0) : 0;
        int new_thread = hwnd!=0 ? WinWindow.GetWindowThreadProcessId(hwnd, 0) : 0;

        if (old_thread != new_thread) {
            Iterator<WinWindow> children = WinWindow.get(StaticData.desktopWindow).getChildren();

            while (children.hasNext()) {
                WinWindow child = children.next();
                if (child.getThread().handle == old_thread) {
                    Message.SendMessageA(child.handle, WM_ACTIVATEAPP, FALSE, new_thread);
                } else if (child.getThread().handle == new_thread) {
                    Message.SendMessageA(child.handle, WM_ACTIVATEAPP, TRUE, old_thread);
                }
            }
        }

        if (WinWindow.IsWindow(hwnd)!=0) {
            Message.SendMessageA(hwnd, WM_NCACTIVATE, BOOL(hwnd == GetForegroundWindow()), previous);
            Message.SendMessageA(hwnd, WM_ACTIVATE, MAKEWPARAM(mouse ? WA_CLICKACTIVE : WA_ACTIVE, WinPos.IsIconic(hwnd)), previous);
        }

        /* now change focus if necessary */
        if (focus) {
            GuiThreadInfo info = Scheduler.getCurrentThread().GetGUIThreadInfo();
            /* Do not change focus if the window is no more active */
            if (hwnd == info.hwndActive) {
                if (info.hwndFocus == 0 || hwnd == 0 || WinWindow.GetAncestor(info.hwndFocus, GA_ROOT) != hwnd)
                    set_focus_window(hwnd);
            }
        }
        WinWindow window = WinWindow.get(hwnd);
        while (window != null) {
            window.lastActivePopup = hwnd;
            if (window.owner == 0)
                break;
            window = WinWindow.get(window.owner);
        }
        return true;
    }
}
