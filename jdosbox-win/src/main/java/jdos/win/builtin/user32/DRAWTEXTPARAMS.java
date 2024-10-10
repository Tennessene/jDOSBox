package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class DRAWTEXTPARAMS extends WinAPI {
    final public static int SIZE = 20;

    public DRAWTEXTPARAMS() {
        cbSize = SIZE;
    }

    public DRAWTEXTPARAMS(int address) {
        cbSize = readd(address);address+=4;
        iTabLength = readd(address);address+=4;
        iLeftMargin = readd(address);address+=4;
        iRightMargin = readd(address);address+=4;
        uiLengthDrawn = readd(address);address+=4;
    }

    public void write(int address) {
        writed(address, cbSize); address+=4;
        writed(address, iTabLength); address+=4;
        writed(address, iLeftMargin); address+=4;
        writed(address, iRightMargin); address+=4;
        writed(address, uiLengthDrawn); address+=4;
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public int cbSize;
    public int iTabLength;
    public int iLeftMargin;
    public int iRightMargin;
    public int uiLengthDrawn;
}
