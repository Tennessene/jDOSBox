package jdos.win.builtin.directx.dsound;

import jdos.hardware.Memory;

public class DSBCaps {
    public static final int SIZE = 20;

    public DSBCaps(int address) {
        dwSize = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
        dwBufferBytes = Memory.mem_readd(address);address+=4;
        dwUnlockTransferRate = Memory.mem_readd(address);address+=4;
        dwPlayCpuOverhead = Memory.mem_readd(address);
    }

    public static void write(int address, int flags, int len, int rate, int overhead) {
        Memory.mem_writed(address, SIZE);
        Memory.mem_writed(address, flags);
        Memory.mem_writed(address, len);
        Memory.mem_writed(address, rate);
        Memory.mem_writed(address, overhead);
    }

    public final int dwSize;
    public final int dwFlags;
    public final int dwBufferBytes;
    public final int dwUnlockTransferRate;
    public final int dwPlayCpuOverhead;
}
