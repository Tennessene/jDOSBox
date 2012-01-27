package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.system.*;
import jdos.win.utils.Ptr;

import java.util.BitSet;

public class Input extends WinAPI {
    // SHORT WINAPI GetAsyncKeyState(int nVirtKey)
    static public int GetAsyncKeyState(int nVirtKey) {
        if (WinKeyboard.keyState.get(nVirtKey))
            return 0x8000;
        else
            return 0;
    }

    // HWND WINAPI GetCapture(void)
    static public int GetCapture() {
        return StaticData.mouseCapture;
    }

    // BOOL WINAPI GetCursorPos(LPPOINT lpPoint)
    static public int GetCursorPos(int lpPoint) {
        writed(lpPoint, StaticData.currentPos.x);
        writed(lpPoint + 4, StaticData.currentPos.y);
        return TRUE;
    }

    // BOOL WINAPI GetKeyboardState(PBYTE lpKeyState)
    static public int GetKeyboardState(int lpKeyState) {
        for (int i=0;i<256;i++) {
            if (Scheduler.getCurrentThread().getKeyState().get(i))
                Memory.mem_writeb(lpKeyState + i, 0x80);
            else
                Memory.mem_writeb(lpKeyState+i, 0x0);
        }
        return WinAPI.TRUE;
    }

    // SHORT WINAPI GetKeyState(int nVirtKey)
    static public int GetKeyState(int nVirtKey) {
        if (Scheduler.getCurrentThread().getKeyState().get(nVirtKey))
            return 0x8000;
        else
            return 0;
    }

    // BOOL WINAPI ReleaseCapture(void)
    static public int ReleaseCapture() {
        StaticData.mouseCapture = 0;
        return TRUE;
    }

    // HWND WINAPI SetCapture(HWND hWnd)
    static public int SetCapture(int hWnd) {
        int result = StaticData.mouseCapture;
        StaticData.mouseCapture = hWnd;
        return result;
    }

    // BOOL WINAPI SetCursorPos(int X, int Y)
    static public int SetCursorPos(int X, int Y) {
        StaticData.currentPos.x = X;
        StaticData.currentPos.y = Y;
        return TRUE;
    }

    // called from the cpu thread
    static public void processInput() {
        while(StaticData.inputQueue.size()>0) {
            Object msg = StaticData.inputQueue.remove(0);
            if (msg instanceof MouseInput) {
                MouseInput mouseMsg = (MouseInput)msg;
                handeMouseInput(mouseMsg.msg, mouseMsg.pt, mouseMsg.wParam);
            } else if (msg instanceof KeyboardInput) {
                KeyboardInput keyboardMsg = (KeyboardInput)msg;
                handeKeyboardInput(keyboardMsg.msg, keyboardMsg.wParam, keyboardMsg.lParam, keyboardMsg.keyState);
            }
        }
    }

    static private class MouseInput {
        public MouseInput(int msg, WinPoint pt, int wParam) {
            this.msg = msg;
            this.pt = pt.copy();
            this.wParam = wParam;
        }
        int msg;
        WinPoint pt;
        int wParam;
    }

    static private class KeyboardInput {
        public KeyboardInput(int msg, int wParam, int lParam, BitSet keyState) {
            this.msg = msg;
            this.wParam = wParam;
            this.lParam = lParam;
            this.keyState = keyState;
        }
        int msg;
        int wParam;
        int lParam;
        BitSet keyState;
    }

    // called from java thread
    static public void addMouseMsg(int msg, WinPoint pt, int wParam) {
        synchronized(StaticData.inputQueueMutex) {
            StaticData.inputQueue.add(new MouseInput(msg, pt, wParam));
            StaticData.inputQueueMutex.notify();
        }
    }

    static public void addKeyboardMsg(int msg, int wParam, int lParam, BitSet keyState) {
         synchronized(StaticData.inputQueueMutex) {
            StaticData.inputQueue.add(new KeyboardInput(msg, wParam, lParam, keyState));
            StaticData.inputQueueMutex.notify();
        }
    }

    static private void handeKeyboardInput(int msg, int wParam, int lParam, BitSet keyState) {
        WinWindow window = WinWindow.get(StaticData.foregroundWindow);
        if (window != null)
            window.getThread().postMessage(window.getThread().GetGUIThreadInfo().hwndFocus, msg, wParam, lParam, keyState);
    }

    static private void handeMouseInput(int msg, WinPoint pt, int wParam) {
        WinWindow window = null;
        int hitTest = WinAPI.HTNOWHERE;
        WinPoint relWinPt = null;

        if (LOG) {
            System.out.println("\nMOUSE 0x"+Ptr.toString(msg)+" "+pt.toString());
        }
        if (StaticData.mouseCapture != 0) {
            window = WinWindow.get(StaticData.mouseCapture);
            hitTest = WinWindow.HTCLIENT;
        } else {
            window = WinWindow.get(StaticData.desktopWindow).findWindowFromPoint(pt.x, pt.y);
            if (window.handle == StaticData.desktopWindow) {
                return;
            }
        }
        relWinPt = pt.copy();
        window.screenToWindow(relWinPt);
        if (hitTest == WinAPI.HTNOWHERE) {
            hitTest = Message.SendMessageA(window.handle, WinAPI.WM_NCHITTEST, 0, WinAPI.MAKELONG(pt.x, pt.y));
        }
        if (msg != WinWindow.WM_MOUSEWHEEL) {
            if (hitTest != WinWindow.HTCLIENT)
                msg += WinWindow.WM_NCMOUSEMOVE - WinWindow.WM_MOUSEMOVE;
            else
                window.screenToWindow(pt);
        }
        // :TODO: double click?

        if (hitTest == WinWindow.HTERROR || hitTest == WinWindow.HTNOWHERE) {
            window.postMessage(WinWindow.WM_SETCURSOR, window.handle, hitTest | (msg >> 16));
            return;
        }

        if (StaticData.mouseCapture == 0) {
            // :TODO: WM_MOUSEACTIVATE
        }

        WinMsg m = window.getThread().getLastMessage();
        if (m != null && m.message == WinWindow.WM_MOUSEMOVE && m.message == msg) {
            m.wParam = wParam;
            m.lParam = (pt.x) | (pt.y << 16);
        } else {
            window.postMessage(WinWindow.WM_SETCURSOR, window.handle, hitTest | (msg >> 16));
            window.postMessage(msg, wParam, (pt.x) | (pt.y << 16));
        }
    }
}
