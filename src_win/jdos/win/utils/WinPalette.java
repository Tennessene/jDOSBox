package jdos.win.utils;

import jdos.hardware.Memory;

public class WinPalette extends WinObject {
    int[] palette;

    public WinPalette(int handle, int[] palette) {
        super(handle);
        this.palette = palette;
    }

    public int setEntries(int start, int count, int address) {
        for (int i=start;i<start+count;i++)
            palette[i] = Memory.mem_readd(address+i*4);
        return count;
    }
}
