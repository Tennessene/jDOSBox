package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class CREATESTRUCT extends WinAPI {
    static public final int SIZE = 48;

    static public void write(int address, int lpCreateParams, int hInstance, int hMenu, int hwndParent, int cy, int cx, int y, int x, int style, int lpszName, int lpszClass, int dwExStyle) {
        writed(address, lpCreateParams); address+=4;
        writed(address, hInstance); address+=4;
        writed(address, hMenu); address+=4;
        writed(address, hwndParent); address+=4;
        writed(address, cy); address+=4;
        writed(address, cx); address+=4;
        writed(address, y); address+=4;
        writed(address, x); address+=4;
        writed(address, style); address+=4;
        writed(address, lpszName); address+=4;
        writed(address, lpszClass); address+=4;
        writed(address, dwExStyle);
    }

    public CREATESTRUCT(int address) {
        lpCreateParams = readd(address); address+=4;
        hInstance = readd(address); address+=4;
        hMenu = readd(address); address+=4;
        hwndParent = readd(address); address+=4;
        cy = readd(address); address+=4;
        cx = readd(address); address+=4;
        y = readd(address); address+=4;
        x = readd(address); address+=4;
        style = readd(address); address+=4;
        lpszName = readd(address); address+=4;
        lpszClass = readd(address); address+=4;
        dwExStyle = readd(address);
    }
    public final int lpCreateParams;
    public final int hInstance;
    public final int hMenu;
    public final int hwndParent;
    public final int cy;
    public final int cx;
    public final int y;
    public final int x;
    public final int style;
    public final int lpszName;
    public final int lpszClass;
    public final int dwExStyle;
}
