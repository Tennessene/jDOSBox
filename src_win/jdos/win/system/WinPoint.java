package jdos.win.system;

import jdos.hardware.Memory;

public class WinPoint {
    static public final int SIZE = 8;

    public WinPoint() {
        x = 0;
        y = 0;
    }

    public WinPoint(int address) {
        this.x = Memory.mem_readd(address);
        this.y = Memory.mem_readd(address+4);
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

    public String toString() {
        return "("+x+","+y+")";
    }
}
