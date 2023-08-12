package jdos.win.builtin.directx.dinput;

import jdos.hardware.Memory;

public class DIObjectDataFormat {
    public static final int SIZE = 16;
    public DIObjectDataFormat(int address) {
        pGuid = Memory.mem_readd(address);address+=4;
        dwOfs = Memory.mem_readd(address);address+=4;
        dwType = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
    }
    public int pGuid;
    public int dwOfs;
    public int dwType;
    public int dwFlags;
}
