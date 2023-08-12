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
        writed(address, dwExStyle); address+=4;
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
        dwExStyle = readd(address); address+=4;
    }
    public int lpCreateParams;
    public int hInstance;
    public int hMenu;
    public int hwndParent;
    public int cy;
    public int cx;
    public int y;
    public int x;
    public int style;
    public int lpszName;
    public int lpszClass;
    public int dwExStyle;
}
