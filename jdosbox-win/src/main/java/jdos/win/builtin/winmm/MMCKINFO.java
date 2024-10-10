package jdos.win.builtin.winmm;

import jdos.win.builtin.WinAPI;

public class MMCKINFO extends WinAPI {
    static final public int SIZE = 20;

    public MMCKINFO() {
    }

    public MMCKINFO(int address) {
        read(address);
    }

    public void write(int address) {
        writed(address, ckid); address+=4;
        writed(address, cksize); address+=4;
        writed(address, fccType); address+=4;
        writed(address, dwDataOffset); address+=4;
        writed(address, dwFlags); address+=4;
    }

    public void read(int address) {
        ckid = readd(address);address+=4;
        cksize = readd(address);address+=4;
        fccType = readd(address);address+=4;
        dwDataOffset = readd(address);address+=4;
        dwFlags = readd(address);address+=4;
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public int ckid;
    public int cksize;
    public int fccType;
    public int dwDataOffset;
    public int dwFlags;
}
