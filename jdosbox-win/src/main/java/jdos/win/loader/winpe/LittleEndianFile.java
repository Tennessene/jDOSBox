package jdos.win.loader.winpe;

import jdos.hardware.Memory;
import jdos.win.Win;

public class LittleEndianFile {
    private final byte[] w;
    private final int address;
    private final int len;
    private int pos;

    public LittleEndianFile(int address) {
        this(address, 0xFFFFFFF);
        if (address == 0) {
            System.out.println("Tried to access a NULL pointer\n");
            Win.exit();
        }
    }

    public LittleEndianFile(int address, int len) {
        this.address = address;
        this.len = len;
        this.pos = 0;
        w = new byte[8];
    }

    public String readCString() {
        StringBuilder result = new StringBuilder();
        while (pos+1<len) {
            char c = (char)readByte(); // :TODO: need to research converting according to 1252
            if (c == 0)
                break;
            result.append(c);
        }
        return result.toString();
    }

    public static void writeCString(int address, String s) {
        byte[] b = s.getBytes();
        Memory.mem_memcpy(address, b, 0, b.length);
        Memory.mem_writeb(address+b.length, 0);
    }

    public String readCString(int len) {
        StringBuilder result = new StringBuilder();
        for (int i=0;i<len && pos+1<=this.len;i++) {
            char c = (char)readByte();
            result.append(c);
        }
        return result.toString();
    }

    public String readCStringW() {
        StringBuilder result = new StringBuilder();
        while (true) {
            char c = (char)readShort();
            if (c == 0)
                break;
            result.append(c);
        }
        return result.toString();
    }

    public String readCStringW(int len) {
        StringBuilder result = new StringBuilder();
        int i;
        for (i=0;i<len && pos+2<=this.len;i++) {
            char c = (char)readShort();
            result.append(c);
        }
        return result.toString();
    }

    public void seek(long value) {
        if (value>len)
            value = len;
        pos = (int)value;
    }

    public int available() {
        return len - pos;
    }

    public final short readShort() {
        short result = (short)Memory.mem_readw(address + pos);
        pos+=2;
        return result;
    }

    public final int readUnsignedShort() {
        int result = Memory.mem_readw(address + pos);
        pos+=2;
        return result;
    }

    public final int readInt() {
        int result = Memory.mem_readd(address + pos);
        pos+=4;
        return result;
    }

    public final long readUnsignedInt() {
        int result = Memory.mem_readd(address + pos);
        pos+=4;
        return result & 0xFFFFFFFFL;
    }

    public final void read(byte[] b, int off, int len) {
        if (len>available())
            len=available();
        Memory.mem_memcpy(b, off, address + pos, len);
        pos+=len;
    }

    public final void read(byte[] b) {
        read(b, 0, b.length);
    }

    public final int skipBytes(int n) {
        if (n>available())
            n = available();
        pos+=n;
        return n;
    }

    public final byte readByte() {
        byte result = (byte)Memory.mem_readb(address + pos);
        pos+=1;
        return result;
    }

    public final short readUnsignedByte() {
        int result = Memory.mem_readb(address + pos);
        pos+=1;
        return (short)result;
    }
}