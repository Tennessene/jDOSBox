package jdos.win.builtin.dsound;

import jdos.hardware.Memory;

public class DSBCaps {
    public DSBCaps(int address) {
        dwSize = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
        dwBufferBytes = Memory.mem_readd(address);address+=4;
        dwUnlockTransferRate = Memory.mem_readd(address);address+=4;
        dwPlayCpuOverhead = Memory.mem_readd(address);address+=4;
    }
    public int dwSize;
    public int dwFlags;
    public int dwBufferBytes;
    public int dwUnlockTransferRate;
    public int dwPlayCpuOverhead;
}
