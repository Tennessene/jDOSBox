package jdos.win.builtin.directx.dsound;

import jdos.hardware.Memory;

public class DSCaps {
    static final public int DSCAPS_PRIMARYMONO =        0x00000001;
    static final public int DSCAPS_PRIMARYSTEREO =      0x00000002;
    static final public int DSCAPS_PRIMARY8BIT =        0x00000004;
    static final public int DSCAPS_PRIMARY16BIT =       0x00000008;
    static final public int DSCAPS_CONTINUOUSRATE =     0x00000010;
    static final public int DSCAPS_EMULDRIVER =         0x00000020;
    static final public int DSCAPS_CERTIFIED =          0x00000040;
    static final public int DSCAPS_SECONDARYMONO =      0x00000100;
    static final public int DSCAPS_SECONDARYSTEREO =    0x00000200;
    static final public int DSCAPS_SECONDARY8BIT =      0x00000400;
    static final public int DSCAPS_SECONDARY16BIT =     0x00000800;
    
    public DSCaps() {
        
    }
    
    public DSCaps(int address) {
        dwSize = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
        dwMinSecondarySampleRate = Memory.mem_readd(address);address+=4;
        dwMaxSecondarySampleRate = Memory.mem_readd(address);address+=4;
        dwPrimaryBuffers = Memory.mem_readd(address);address+=4;
        dwMaxHwMixingAllBuffers = Memory.mem_readd(address);address+=4;
        dwMaxHwMixingStaticBuffers = Memory.mem_readd(address);address+=4;
        dwMaxHwMixingStreamingBuffers = Memory.mem_readd(address);address+=4;
        dwFreeHwMixingAllBuffers = Memory.mem_readd(address);address+=4;
        dwFreeHwMixingStaticBuffers = Memory.mem_readd(address);address+=4;
        dwFreeHwMixingStreamingBuffers = Memory.mem_readd(address);address+=4;
        dwMaxHw3DAllBuffers = Memory.mem_readd(address);address+=4;
        dwMaxHw3DStaticBuffers = Memory.mem_readd(address);address+=4;
        dwMaxHw3DStreamingBuffers = Memory.mem_readd(address);address+=4;
        dwFreeHw3DAllBuffers = Memory.mem_readd(address);address+=4;
        dwFreeHw3DStaticBuffers = Memory.mem_readd(address);address+=4;
        dwFreeHw3DStreamingBuffers = Memory.mem_readd(address);address+=4;
        dwTotalHwMemBytes = Memory.mem_readd(address);address+=4;
        dwFreeHwMemBytes = Memory.mem_readd(address);address+=4;
        dwMaxContigFreeHwMemBytes = Memory.mem_readd(address);address+=4;
        dwUnlockTransferRateHwBuffers = Memory.mem_readd(address);address+=4;
        dwPlayCpuOverheadSwBuffers = Memory.mem_readd(address);address+=4;
        dwReserved1 = Memory.mem_readd(address);address+=4;
        dwReserved2 = Memory.mem_readd(address);address+=4;
    }
    
    public void write(int address) {
         Memory.mem_writed(address, dwSize);address+=4;
         Memory.mem_writed(address, dwFlags);address+=4;
         Memory.mem_writed(address, dwMinSecondarySampleRate);address+=4;
         Memory.mem_writed(address, dwMaxSecondarySampleRate);address+=4;
         Memory.mem_writed(address, dwPrimaryBuffers);address+=4;
         Memory.mem_writed(address, dwMaxHwMixingAllBuffers);address+=4;
         Memory.mem_writed(address, dwMaxHwMixingStaticBuffers);address+=4;
         Memory.mem_writed(address, dwMaxHwMixingStreamingBuffers);address+=4;
         Memory.mem_writed(address, dwFreeHwMixingAllBuffers);address+=4;
         Memory.mem_writed(address, dwFreeHwMixingStaticBuffers);address+=4;
         Memory.mem_writed(address, dwFreeHwMixingStreamingBuffers);address+=4;
         Memory.mem_writed(address, dwMaxHw3DAllBuffers);address+=4;
         Memory.mem_writed(address, dwMaxHw3DStaticBuffers);address+=4;
         Memory.mem_writed(address, dwMaxHw3DStreamingBuffers);address+=4;
         Memory.mem_writed(address, dwFreeHw3DAllBuffers);address+=4;
         Memory.mem_writed(address, dwFreeHw3DStaticBuffers);address+=4;
         Memory.mem_writed(address, dwFreeHw3DStreamingBuffers);address+=4;
         Memory.mem_writed(address, dwTotalHwMemBytes);address+=4;
         Memory.mem_writed(address, dwFreeHwMemBytes);address+=4;
         Memory.mem_writed(address, dwMaxContigFreeHwMemBytes);address+=4;
         Memory.mem_writed(address, dwUnlockTransferRateHwBuffers);address+=4;
         Memory.mem_writed(address, dwPlayCpuOverheadSwBuffers);address+=4;
         Memory.mem_writed(address, dwReserved1);address+=4;
         Memory.mem_writed(address, dwReserved2);address+=4;
    }
    public int dwSize;
    public int dwFlags;
    public int dwMinSecondarySampleRate;
    public int dwMaxSecondarySampleRate;
    public int dwPrimaryBuffers;
    public int dwMaxHwMixingAllBuffers;
    public int dwMaxHwMixingStaticBuffers;
    public int dwMaxHwMixingStreamingBuffers;
    public int dwFreeHwMixingAllBuffers;
    public int dwFreeHwMixingStaticBuffers;
    public int dwFreeHwMixingStreamingBuffers;
    public int dwMaxHw3DAllBuffers;
    public int dwMaxHw3DStaticBuffers;
    public int dwMaxHw3DStreamingBuffers;
    public int dwFreeHw3DAllBuffers;
    public int dwFreeHw3DStaticBuffers;
    public int dwFreeHw3DStreamingBuffers;
    public int dwTotalHwMemBytes;
    public int dwFreeHwMemBytes;
    public int dwMaxContigFreeHwMemBytes;
    public int dwUnlockTransferRateHwBuffers;
    public int dwPlayCpuOverheadSwBuffers;
    public int dwReserved1;
    public int dwReserved2;
}
