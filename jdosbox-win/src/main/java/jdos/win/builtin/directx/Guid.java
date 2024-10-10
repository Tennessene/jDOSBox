package jdos.win.builtin.directx;

import jdos.hardware.Memory;

public class Guid {
    int i1;
    int i2;
    int i3;
    int i4;
    int i5;
    int i6;
    int i7;
    int i8;
    int i9;
    int i10;
    int i11;

    public Guid(int address) {
        i1 = Memory.mem_readd(address);
        i2 = Memory.mem_readw(address+4);
        i3 = Memory.mem_readw(address+6);
        i4 = Memory.mem_readb(address+8);
        i5 = Memory.mem_readb(address+9);
        i6 = Memory.mem_readb(address+10);
        i7 = Memory.mem_readb(address+11);
        i8 = Memory.mem_readb(address+12);
        i9 = Memory.mem_readb(address+13);
        i10 = Memory.mem_readb(address+14);
        i11 = Memory.mem_readb(address+15);
    }

    public Guid(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11) {
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.i4 = i4;
        this.i5 = i5;
        this.i6 = i6;
        this.i7 = i7;
        this.i8 = i8;
        this.i9 = i9;
        this.i10 = i10;
        this.i11 = i11;
    }

    public boolean IsEqual(int address) {
        if (i1 == Memory.mem_readd(address) && i2 == Memory.mem_readw(address+4) && i3 == Memory.mem_readw(address+6) && i4 == Memory.mem_readb(address+8)
                 && i5 == Memory.mem_readb(address+9) && i6 == Memory.mem_readb(address+10) && i7 == Memory.mem_readb(address+11) && i8 == Memory.mem_readb(address+12)
                 && i9 == Memory.mem_readb(address+13) && i10 == Memory.mem_readb(address+14) && i11 == Memory.mem_readb(address+15)) {
            return true;
        }
        return false;
    }

    public boolean IsEqual(Guid guid) {
        return i1 == guid.i1 && i2 == guid.i2 && i3 == guid.i3 && i4 == guid.i4 && i5 == guid.i5 && i6 == guid.i6 && i7 == guid.i7 && i8 == guid.i8 && i9 == guid.i9 && i10 == guid.i10 && i11 == guid.i11;
    }
}
