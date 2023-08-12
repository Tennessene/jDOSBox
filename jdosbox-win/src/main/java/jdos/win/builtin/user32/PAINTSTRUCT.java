package jdos.win.builtin.user32;

import jdos.win.system.WinRect;

public class PAINTSTRUCT {
    static public final int SIZE = 64;
    public int hdc;
    public int fErase;
    public WinRect rcPaint = new WinRect();
    public int fRestore;
    public int fIncUpdate;
    public byte[] rgbReserved = new byte[32];
}
