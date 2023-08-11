package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;

public class WinRect extends WinAPI {
    public static final int SIZE = 16;

    public int left;
    public int top;
    public int right;
    public int bottom;

    public WinRect() {
        left = 0;
        top = 0;
        right = 0;
        bottom = 0;
    }

    public WinRect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public WinRect(int address) {
        copy(address);
    }

    public WinRect(WinRect rect) {
        copy(rect);
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public void inflate(int dx, int dy) {
        left-=dx;
        right+=dx;
        top-=dy;
        bottom+=dy;
    }

    public boolean intersect(WinRect src1, WinRect src2) {
        if (src1.isEmpty() || src2.isEmpty()) {
            setEmpty();
            return false;
        }
        left   = Math.max(src1.left, src2.left);
        right  = Math.min(src1.right,src2.right);
        top    = Math.max(src1.top, src2.top);
        bottom = Math.min(src1.bottom, src2.bottom);
        return !isEmpty();
    }

    public void merge(WinRect rect) {
        if (!rect.isEmpty()) {
            left = Math.min(left, rect.left);
            top = Math.min(top, rect.top);
            right = Math.max(right, rect.right);
            bottom = Math.max(bottom, rect.bottom);
        }
    }
    public boolean isEmpty() {
        return left>=right || top>=bottom;
    }

    public void setEmpty() {
        left = 0;
        right = 0;
        top = 0;
        bottom = 0;
    }
    public void set(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void offset(int x, int y) {
        left+=x;
        right+=x;
        top+=y;
        bottom+=y;
    }

    public void copy(int address) {
        left = Memory.mem_readd(address);
        top = Memory.mem_readd(address+4);
        right = Memory.mem_readd(address+8);
        bottom = Memory.mem_readd(address+12);
    }

    public void copy(WinRect rect) {
        this.left = rect.left;
        this.right = rect.right;
        this.top = rect.top;
        this.bottom = rect.bottom;
    }

    public boolean equals(WinRect rect) {
        return left==rect.left && top==rect.top && right==rect.right && bottom==rect.bottom;
    }

    public void write(int address) {
        write(address, left, top, right, bottom);
    }

    public static void write(int address, int left, int top, int right, int bottom) {
        Memory.mem_writed(address, left);
        Memory.mem_writed(address+4, top);
        Memory.mem_writed(address+8, right);
        Memory.mem_writed(address+12, bottom);
    }

    public boolean contains(int x, int y) {
        return (x>=left && x<=right && y>=top && y<=bottom);
    }

    public boolean contains(WinPoint p) {
        return (p.x>=left && p.x<=right && p.y>=top && p.y<=bottom);
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public String toString() {
        return "("+left+","+top+") - ("+right+","+bottom+")";
    }

    public WinRect copy() {
        return new WinRect(left, top, right, bottom);
    }
}
