package jdos.win.builtin.winmm;

import jdos.win.builtin.WinAPI;

public class MMIOINFO extends WinAPI {
    public static final int SIZE = 72;

    public MMIOINFO() {
    }
    public MMIOINFO(int address) {
        read(address);
    }
    public void write(int address) {
        writed(address, dwFlags); address+=4;
        writed(address, fccIOProc); address+=4;
        writed(address, pIOProc); address+=4;
        writed(address, wErrorRet); address+=4;
        writed(address, hTask); address+=4;
        writed(address, cchBuffer); address+=4;
        writed(address, pchBuffer); address+=4;
        writed(address, pchNext); address+=4;
        writed(address, pchEndRead); address+=4;
        writed(address, pchEndWrite); address+=4;
        writed(address, lBufOffset); address+=4;
        writed(address, lDiskOffset); address+=4;
        writed(address, adwInfo[0]); address+=4;
        writed(address, adwInfo[1]); address+=4;
        writed(address, adwInfo[2]); address+=4;
        writed(address, dwReserved1); address+=4;
        writed(address, dwReserved2); address+=4;
        writed(address, hmmio); address+=4;
    }

    public void read(int address) {
        dwFlags = readd(address);address+=4;
        fccIOProc = readd(address);address+=4;
        pIOProc = readd(address);address+=4;
        wErrorRet = readd(address);address+=4;
        hTask = readd(address);address+=4;
        cchBuffer = readd(address);address+=4;
        pchBuffer = readd(address);address+=4;
        pchNext = readd(address);address+=4;
        pchEndRead = readd(address);address+=4;
        pchEndWrite = readd(address);address+=4;
        lBufOffset = readd(address);address+=4;
        lDiskOffset = readd(address);address+=4;
        adwInfo[0] = readd(address);address+=4;
        adwInfo[1] = readd(address);address+=4;
        adwInfo[2] = readd(address);address+=4;
        dwReserved1 = readd(address);address+=4;
        dwReserved2 = readd(address);address+=4;
        hmmio = readd(address);address+=4;
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public int dwFlags;
    public int fccIOProc;
    public int pIOProc;
    public int wErrorRet;
    public int hTask;
    public int cchBuffer;
    public int pchBuffer;
    public int pchNext;
    public int pchEndRead;
    public int pchEndWrite;
    public int lBufOffset;
    public int lDiskOffset;
    public int[] adwInfo = new int[3];
    public int dwReserved1;
    public int dwReserved2;
    public int hmmio;
}
