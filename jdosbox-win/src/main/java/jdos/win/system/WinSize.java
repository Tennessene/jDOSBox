package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;

public class WinSize extends WinAPI {
    public static final int SIZE = 8;

    public WinSize() {
    }

    public WinSize(int cx, int cy) {
        this.cx = cx;
        this.cy = cy;
    }

    public void write(int address) {
        Memory.mem_writed(address, cx);
        Memory.mem_writed(address+4, cy);
    }

    public void copy(int address) {
        this.cx = Memory.mem_readd(address);
        this.cy = Memory.mem_readd(address+4);
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public int cx;
    public int cy;
}
