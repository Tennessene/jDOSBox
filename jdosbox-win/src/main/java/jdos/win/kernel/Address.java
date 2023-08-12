package jdos.win.kernel;

import jdos.hardware.Memory;

public class Address {
    public int ptr;

    public void writed(int offset, int value) {
        Memory.mem_writed(ptr+offset, value);
    }
    public void writew(int offset, int value) {
        Memory.mem_writew(ptr+offset, value);
    }
    public void writeb(int offset, int value) {
        Memory.mem_writeb(ptr+offset, value);
    }
}
