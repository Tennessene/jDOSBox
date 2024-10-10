package jdos.win.builtin.winmm;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.directx.dsound.DSMixer;

public class WAVEFORMATEX extends WinAPI {
    public final static int SIZE = 18;
    public WAVEFORMATEX() {
        wFormatTag = WAVE_FORMAT_ADPCM;
        nChannels = DSMixer.DEVICE_CHANNELS;
        nSamplesPerSec = DSMixer.DEVICE_SAMPLE_RATE;
        wBitsPerSample = DSMixer.DEVICE_BITS_PER_SAMEPLE;
        nBlockAlign = DSMixer.DEVICE_BLOCK_ALIGN;
        cbSize = SIZE;
        nAvgBytesPerSec = nSamplesPerSec * wBitsPerSample / 8 * nChannels;
    }
    public WAVEFORMATEX(int address) {
        wFormatTag = Memory.mem_readw(address); address+=2;
        nChannels = Memory.mem_readw(address); address+=2;
        nSamplesPerSec = Memory.mem_readd(address); address+=4;
        nAvgBytesPerSec = Memory.mem_readd(address); address+=4;
        nBlockAlign = Memory.mem_readw(address); address+=2;
        wBitsPerSample = Memory.mem_readw(address); address+=2;
        cbSize = Memory.mem_readw(address); address+=2;
    }
    public void write(int address) {
        writew(address, wFormatTag);address+=2;
        writew(address, nChannels);address+=2;
        writed(address, nSamplesPerSec);address+=4;
        writed(address, nAvgBytesPerSec);address+=4;
        writew(address, nBlockAlign);address+=2;
        writew(address, wBitsPerSample);address+=2;
        writew(address, cbSize);address+=2;
    }
    public int  wFormatTag;
    public int  nChannels;
    public int nSamplesPerSec;
    public int nAvgBytesPerSec;
    public int  nBlockAlign;
    public int  wBitsPerSample;
    public int  cbSize;
}
