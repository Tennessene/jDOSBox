package jdos.win.system;

import jdos.win.builtin.kernel32.WinThread;

import java.util.BitSet;

public class WinMsg {
    public WinMsg(int hWnd, int message, int wParam, int lParam, WinThread callingThread) {
        this(hWnd, message, wParam, lParam);
        this.callingThread = callingThread;
    }
    public WinMsg(int hWnd, int message, int wParam, int lParam, BitSet keyState) {
        this(hWnd, message, wParam, lParam);
        this.keyState = keyState;
    }
    public WinMsg(int hWnd, int message, int wParam, int lParam) {
        this.hwnd = hWnd;
        this.message = message;
        this.wParam = wParam;
        this.lParam = lParam;
        this.time = WinSystem.getTickCount();
        this.x = StaticData.currentPos.x;
        this.y = StaticData.currentPos.y;
    }
    public BitSet keyState;
    public WinThread callingThread;
    public int sendResult;
    public int hwnd;
    public int message;
    public int wParam;
    public int lParam;
    public int time;
    public int x;
    public int y;
}
