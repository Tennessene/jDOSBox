package jdos.hardware;

import jdos.gui.Render;

import java.util.Arrays;

final public class RAM {
    public static int[] direct;

    static void alloc(int size) {
        direct = new int[size >> 2];
    }

    static void free() {
        direct = null;
    }

    public static byte readbs(/*HostPt*/int address) {
        return (byte) (direct[(address >> 2)] >>> ((address & 0x3) << 3));
    }

    public static /*Bit8u*/short readb(/*HostPt*/int address) {
        return (short) ((direct[(address >>> 2)] >>> ((address & 0x3) << 3)) & 0xFF);
    }

    public static /*Bit16u*/int readw(/*HostPt*/int address) {
        int rem = address & 0x3;
        int[] local = direct;
        int index = (address >>> 2);
        int val = local[index] >>> (rem << 3);
        if (rem == 3) {
            val |= local[index + 1] << 8;
        }
        return val & 0xFFFF;
    }

    public static /*Bit32u*/int readd(/*HostPt*/int address) {
        int rem = (address & 0x3);
        if (rem == 0) {
            return direct[address >>> 2];
        }
        int off = rem << 3;
        int[] local = direct;
        int index = (address >>> 2);
        return local[index] >>> off | local[index + 1] << (32 - off);
    }

    public static void writeb(/*HostPt*/int address,/*Bit8u*/ int value) {
        int off = (address & 0x3) << 3;
        int[] local = direct;
        int mask = ~(0xFF << off);
        int index = (address >>> 2);
        int val = local[index] & mask | (value & 0xFF) << off;
        local[index] = val;
    }

    public static void writebs(/*HostPt*/int address, byte value) {
        int off = (address & 0x3) << 3;
        int[] local = direct;
        int mask = ~(0xFF << off);
        int index = (address >>> 2);
        int val = local[index] & mask | (value & 0xFF) << off;
        local[index] = val;
    }

    public static void writew(/*HostPt*/int address,/*Bit16u*/int value) {
        int rem = (address & 0x3);
        int[] local = direct;
        int index = (address >>> 2);
        value &= 0xFFFF;
        if (rem == 3) {
            local[index] = (local[index] & 0xFFFFFF | value << 24);
            index++;
            local[index] = (local[index] & 0xFFFFFF00 | value >>> 8);
        } else {
            int off = rem << 3;
            int mask = ~(0xFFFF << off);
            local[index] = (local[index] & mask | value << off);
        }
    }

    public static void writed(/*HostPt*/int address,/*Bit32u*/int val) {
        int rem = (address & 0x3);
        if (rem == 0) {
            direct[address >>> 2] = val;
        } else {
            int index = (address >>> 2);
            int[] local = direct;
            int off = rem << 3;
            int mask = -1 << off;
            local[index] = (local[index] & ~mask) | (val << off);
            index++;
            local[index] = (local[index] & mask) | (val >>> (32 - off));
        }
    }

    static public void memcpy(int dest, byte[] src, int srcOffset, /*Bitu*/int size) {
        for (int i = 0; i < size; i++)
            writeb(dest++, src[srcOffset++]);
    }

    static public void memcpy(byte[] dest, int dest_offset, /*PhysPt*/int src,/*Bitu*/int size) {
        int begin = src & 3;
        int end = size & ~3;
        for (int i = 0; i < begin && i < size; i++)
            dest[i + dest_offset] = readbs(src + i);
        int off = dest_offset + begin;
        int index = (src + begin) >> 2;
        for (int i = begin; i < end && i + 3 < size; i += 4) {
            int v = direct[index++];
            dest[off++] = (byte) v;
            dest[off++] = (byte) (v >> 8);
            dest[off++] = (byte) (v >> 16);
            dest[off++] = (byte) (v >> 24);
        }
        for (int i = end; i < size; i++)
            dest[i + dest_offset] = readbs(src + i);
    }

    static public void memcpy(Render.Render_t.SRC dst, int offset, /*PhysPt*/int src,/*Bitu*/int amount) {
        if (dst.outWrite8 != null) {
            for (int i = 0; i < amount; i++)
                dst.outWrite8[i + offset] = readbs(src + i);
        } else if (dst.outWrite16 != null) {
            offset >>= 1;
            amount >>= 1;
            for (int i = 0; i < amount; i++) {
                dst.outWrite16[i + offset] = (short) readw(src);
                src += 2;
            }
        } else if (dst.outWrite32 != null) {
            offset >>= 2;
            amount >>= 2;
            for (int i = 0; i < amount; i++) {
                dst.outWrite32[i + offset] = readd(src);
                src += 4;
            }
        }
    }

    static public void memcpy(/*PhysPt*/int dst,/*PhysPt*/int src,/*Bitu*/int amount) {
        int src_align = src & 0x3;
        int dst_align = dst & 0x3;
        if (src_align == dst_align) {
            while ((src & 0x3) > 0 && amount > 0) {
                writeb(dst++, readb(src++));
                amount--;
            }
            int len = (amount >>> 2);
            if (len > 0)
                System.arraycopy(direct, src >>> 2, direct, dst >>> 2, len);
            len = len << 2;
            if (len == amount)
                return;
            dst += len;
            src += len;
            amount -= len;
        }

        for (int i = 0; i < amount; i++) {
            writeb(dst++, readb(src++));
        }
    }

    static public void zeroset(int dest, int size) {
        memset(dest, size, 0);
    }

    static public void memset(int dest, int size, int value) {
        if ((dest & 0x3) == 0) {
            int index = (dest >>> 2);
            int len = (size >>> 2);
            value &= 0xFF;
            Arrays.fill(direct, index, index + len, (value << 24) | (value << 16) | (value << 8) | value);
            size = (size & 0x3) << 3;
            dest += len << 2;
        }
        byte b = (byte) value;
        for (int i = 0; i < size; i++)
            writeb(dest++, b);
    }
}
