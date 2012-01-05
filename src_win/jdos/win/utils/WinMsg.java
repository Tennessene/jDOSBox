package jdos.win.utils;

public class WinMsg {
    public WinMsg(int hWnd, int message, int wParam, int lParam) {
        this.hwnd = hWnd;
        this.message = message;
        this.wParam = wParam;
        this.lParam = lParam;
        this.time = WinSystem.getTickCount();
        this.x = WinSystem.getMouseX();
        this.y = WinSystem.getMouseY();
    }
    public int hwnd;
    public int message;
    public int wParam;
    public int lParam;
    public int time;
    public int x;
    public int y;
}
