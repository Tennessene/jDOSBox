package jdos.host.router;

public class Ether extends EtherUtil {
    static final public int IP = 0x0800;
    static final public int ARP = 0x0806;

    public void parse(byte[] bytes, int off) {
        System.arraycopy(bytes, off, dest, 0, 6);
        System.arraycopy(bytes, off+6, src, 0, 6);
        protocol = readWord(bytes, off + 12);
    }
    public static void output(byte[] data, int offset, byte[] src, byte[] dst) {
        System.arraycopy(dst, 0, data, offset, 6);
        System.arraycopy(src, 0, data, offset+6, 6);
        writeWord(data, offset + 12, 0x800); // IP
    }

    public void handle(byte[] buffer, int offset, int len) {
        parse(buffer, offset);
        if (protocol == Ether.IP) {
            ip.handle(buffer, offset+Ether.LEN, len-Ether.LEN);
        } else if (protocol == Ether.ARP) {
            arp.handle(buffer, offset+Ether.LEN, len-Ether.LEN);
        }
    }
    static public final int LEN = 14;

    // Protocols
    final static IP ip = new IP();
    final static ARP arp = new ARP();

    byte[] dest = new byte[6];
    byte[] src = new byte[6];
    int protocol;
}
