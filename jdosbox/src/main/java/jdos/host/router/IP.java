package jdos.host.router;

public class IP extends EtherUtil {
    static public final int LEN = 20;

    static public final int MAXTTL = 255;		/* maximum time to live (seconds) */
    static public final int IPDEFTTL = 64;		/* default ttl, from RFC 1340 */
    static public final int IPFRAGTTL = 60;		/* time to live for frags, slowhz */
    static public final int IPTTLDEC = 1;		/* subtracted when forwarding */

    public static final int IPTOS_LOWDELAY = 0x10;

    public static final int IP_DF = 0x4000;			/* don't fragment flag */
    public static final int IP_MF = 0x2000;			/* more fragments flag */
    public static final int IP_OFFMASK = 0x1fff;		/* mask for fragmenting bits */

    public static int id=0x1234;

    public void parse(byte[] bytes, int off) {
        version = bytes[off] >> 4;
        headerLen = (bytes[off] & 0xF) << 2;
        service = bytes[off+1];
        len = readWord(bytes, off + 2);
        ident = readWord(bytes, off + 4);
        frags = readWord(bytes, off + 6);
        ttl = bytes[off+8];
        pcol = bytes[off+9];
        check = readWord(bytes, off + 10);
        sourceIP = readDWord(bytes, off + 12);
        destIP = readDWord(bytes, off + 16);
    }

    static public void output(byte[] buffer, int offset, int protocol, int sourceIP, int destIP, int len, int iptos, int ttl) {
        int originalOffset = offset;
        buffer[offset++] = (byte)(4 << 4 | 5);  // version & headerLen
        buffer[offset++] = (byte)iptos;
        writeWord(buffer, offset, len);
        offset+=2;
        writeWord(buffer, offset, /*id++*/0);
        offset+=2;
        writeWord(buffer, offset, /*IP_DF*/0);
        offset+=2;
        buffer[offset++] = (byte)ttl;
        buffer[offset++] = (byte)protocol;
        writeWord(buffer, offset, 0);
        int sumOffset = offset;
        offset+=2;
        writeDWord(buffer, offset, sourceIP);
        offset+=4;
        writeDWord(buffer, offset, destIP);
        writeWord(buffer, sumOffset, csum(buffer, originalOffset, IP.LEN));
    }

    public void handle(byte[] buffer, int offset, int len) {
        if (len<LEN)
            return;
        parse(buffer, offset);
        if (version == 4 && headerLen<=len && csum(buffer, offset, headerLen)==0) {
            if (pcol ==  6) { // TCP
                System.out.println("Received TCP Packet");
            } else if (pcol == 17) { // UDP
                udp.handle(buffer, offset + this.headerLen, len - this.headerLen);
            } else if (pcol == 1) { // ICMP
                icmp.handle(buffer, offset + this.headerLen, len - this.headerLen);
            } else {
                System.out.println("IP packet "+pcol);
            }
        }
    }

    // Protocols
    final static UDP udp = new UDP();
    final static ICMP icmp = new ICMP();

    int version;
    int headerLen;
    short service;
    int len;
    int ident;
    int frags;
    short ttl;
    short pcol;
    int check;
    int sourceIP;
    int destIP;
}
