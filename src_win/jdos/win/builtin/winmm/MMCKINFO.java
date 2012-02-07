package jdos.win.builtin.winmm;

import jdos.win.builtin.WinAPI;

public class MMCKINFO extends WinAPI {
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

    public int ckid;
    public int cksize;
    public int fccType;
    public int dwDataOffset;
    public int dwFlags;
}
