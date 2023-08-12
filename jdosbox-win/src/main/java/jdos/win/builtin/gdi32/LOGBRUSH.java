package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;

public class LOGBRUSH extends WinAPI {
    public final static int SIZE = 12;

    public LOGBRUSH() {
    }

    public LOGBRUSH(int address) {
        lbStyle = readd(address); address+=4;
        lbColor = readd(address); address+=4;
        lbHatch = readd(address); address+=4;
    }

    public void write(int address) {
        writed(address, lbStyle);address+=4;
        writed(address, lbColor);address+=4;
        writed(address, lbHatch);address+=4;
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public int lbStyle;
    public int lbColor;
    public int lbHatch;
}
