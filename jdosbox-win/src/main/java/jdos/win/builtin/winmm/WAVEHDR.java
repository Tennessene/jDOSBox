package jdos.win.builtin.winmm;

import jdos.hardware.Memory;

public class WAVEHDR {
    static final public int SIZE = 32;

    static final public int WHDR_DONE =      0x00000001;
    static final public int WHDR_PREPARED =  0x00000002;
    static final public int WHDR_BEGINLOOP = 0x00000004;
    static final public int WHDR_ENDLOOP =   0x00000008;
    static final public int WHDR_INQUEUE =   0x00000010;
    
    public WAVEHDR(int address) {
        lpData = Memory.mem_readd(address);address+=4;
        dwBufferLength = Memory.mem_readd(address);address+=4;
        dwBytesRecorded = Memory.mem_readd(address);address+=4;
        dwUser = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
        dwLoops = Memory.mem_readd(address);address+=4;
        lpNext = Memory.mem_readd(address);address+=4;
        reserved = Memory.mem_readd(address);address+=4;
    }

    public void writeFlags() {
        Memory.mem_writed(reserved+16, dwFlags);
    }
    public int lpData;
    public int dwBufferLength;
    public int dwBytesRecorded;
    public int dwUser;
    public int dwFlags;
    public int dwLoops;
    public int lpNext;
    public int reserved;

    public byte[] data;
}
