package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class WINDOWPOS extends WinAPI {
    public static final int SIZE = 28;

    public WINDOWPOS() {
    }

    public WINDOWPOS(int hwnd, int hwndInsertAfter, int x, int y, int cx, int cy, int flags) {
        this.hwnd = hwnd;
        this.hwndInsertAfter = hwndInsertAfter;
        this.x = x;
        this.y = y;
        this.cx = cx;
        this.cy = cy;
        this.flags = flags;
    }

    public void write(int address) {
        writed(address, hwnd); address+=4;
        writed(address, hwndInsertAfter); address+=4;
        writed(address, x); address+=4;
        writed(address, y); address+=4;
        writed(address, cx); address+=4;
        writed(address, cy); address+=4;
        writed(address, flags); address+=4;
    }

    public int allocTemp() {
        int result = WinAPI.getTempBuffer(SIZE);
        write(result);
        return result;
    }

    int hwnd;
    int hwndInsertAfter;
    int  x;
    int  y;
    int  cx;
    int  cy;
    int flags;
}
