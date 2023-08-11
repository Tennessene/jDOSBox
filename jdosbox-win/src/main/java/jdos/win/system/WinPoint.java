package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;

public class WinPoint extends WinAPI {
    static public final int SIZE = 8;

    public WinPoint() {
        x = 0;
        y = 0;
    }

    public WinPoint(int address) {
        copy(address);
    }

    public WinPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x;
    public int y;

    public WinPoint copy() {
        return new WinPoint(x, y);
    }

    public void copy(WinPoint pt) {
        this.x = pt.x;
        this.y = pt.y;
    }

    public void offset(int x, int y) {
        this.x+=x;
        this.y+=y;
    }

    public void write(int address) {
        Memory.mem_writed(address, x);
        Memory.mem_writed(address+4, y);
    }

    public void copy(int address) {
        this.x = Memory.mem_readd(address);
        this.y = Memory.mem_readd(address+4);
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public String toString() {
        return "("+x+","+y+")";
    }
}
