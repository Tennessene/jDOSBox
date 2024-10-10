package jdos.host.router;

import jdos.util.StringHelper;

import java.util.LinkedList;

public class EtherUtil {
    protected final static Ether ether = new Ether();
    protected static final int CLIENT_ADDRESS = 0xC0A89D07; // 192.168.157.7
    protected static final int SERVER_ADDRESS = 0xC0A89D01; // 192.168.157.1
    protected static final int SERVER_NETMASK = 0xFFFFFF00; // 255.255.255.0
    protected static final byte[] SERVER_MAC_ADDRESS = new byte[] {(byte)0xAC, (byte)0xDE, 0x48, (byte)0x88, (byte)0x99, (byte)0xAB};

    protected static LinkedList frames;

    static public int readWord(byte[] bytes, int off) {
        return bytes[off+1] & 0xFF | ((bytes[off] & 0xFF) << 8);
    }
    static public int readDWord(byte[] bytes, int off) {
        return bytes[off+3] & 0xFF | ((bytes[off+2] & 0xFF) << 8) | ((bytes[off+1] & 0xFF) << 16) | ((bytes[off] & 0xFF) << 24);
    }
    static public void writeDWord(byte[] bytes, int off, int value) {
        bytes[off+3] = (byte)value;
        bytes[off+2] = (byte)(value >> 8);
        bytes[off+1] = (byte)(value >> 16);
        bytes[off] = (byte)(value >> 24);
    }
    static public void writeWord(byte[] bytes, int off, int value) {
        bytes[off+1] = (byte)value;
        bytes[off] = (byte)(value >> 8);
    }

    static public short csum(byte[] buffer, int offset, int length) {
        int i = 0;

        long sum = 0;
        long data;

        while (length > 1) {
          data = (((buffer[i+offset] << 8) & 0xFF00) | ((buffer[i + 1 + offset]) & 0xFF));
          sum += data;
          if ((sum & 0xFFFF0000) > 0) {
            sum = sum & 0xFFFF;
            sum += 1;
          }
          i += 2;
          length -= 2;
        }

        if (length > 0) {
          sum += (buffer[i+offset] << 8 & 0xFF00);
          if ((sum & 0xFFFF0000) > 0) {
            sum = sum & 0xFFFF;
            sum += 1;
          }
        }

        sum = ~sum;
        return (short)(sum & 0xFFFF);
    }

    public static void printAddress(int address) {
        System.out.print(address >>> 24);
        System.out.print(".");
        System.out.print((address >> 16) & 0xFF);
        System.out.print(".");
        System.out.print((address >> 8) & 0xFF);
        System.out.print(".");
        System.out.print(address & 0xFF);
    }

    public static void dump(byte[] buffer, int offset, int len) {
        int col=0;
        int row=0;
        for (int i=offset;i<offset+len;i++) {
            if (col == 0) {
                System.out.print(StringHelper.sprintf("%08x", new Object[]{row * 8}));
            }
            System.out.print(StringHelper.sprintf(" %02x", new Object[] {(buffer[i] & 0xFF)}));
            col++;
            if (col==8) {
                System.out.println(" ........");
                row++;
                col = 0;
            }
        }
        System.out.println();
    }
}
