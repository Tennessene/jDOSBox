package jdos.win.builtin.directx.ddraw;

import jdos.hardware.Memory;

public class DDColorKey {
    public final static int SIZE = 8;

    public int dwColorSpaceLowValue; // low boundary of color space that is to be treated as Color Key, inclusive
	public int dwColorSpaceHighValue; // high boundary of color space that is to be treated as Color Key, inclusive

    public DDColorKey(int address) {
        dwColorSpaceLowValue = Memory.mem_readd(address);
        dwColorSpaceHighValue = Memory.mem_readd(address+4);
    }
}
