package jdos.win.builtin.directx.dsound;

import jdos.hardware.Memory;

public class WaveFormatEx {
    public static final int SIZE = 18;

    public WaveFormatEx(int address) {
        wFormatTag = Memory.mem_readw(address); address+=2;
        nChannels = Memory.mem_readw(address); address+=2;
        nSamplesPerSec = Memory.mem_readd(address); address+=4;
        nAvgBytesPerSec = Memory.mem_readd(address); address+=4;
        nBlockAlign = Memory.mem_readw(address); address+=2;
        wBitsPerSample = Memory.mem_readw(address); address+=2;
        cbSize = Memory.mem_readw(address); address+=2;
    }
    public int wFormatTag;
    public int nChannels;
    public int nSamplesPerSec;
    public int nAvgBytesPerSec;
    public int nBlockAlign;
    public int wBitsPerSample;
    public int cbSize;
}
