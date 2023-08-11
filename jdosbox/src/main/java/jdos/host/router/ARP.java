package jdos.host.router;

public class ARP extends EtherUtil {
    static public final int LEN = 28;
    private void parse(byte[] buffer, int offset) {
        hardware = readWord(buffer, offset);offset+=2;
        protocol = readWord(buffer, offset);offset+=2;
        hlen = buffer[offset++] & 0xFF;
        plen = buffer[offset++] & 0xFF;
        op = readWord(buffer, offset); offset+=2;
        System.arraycopy(buffer, offset, senderMac, 0, 6);offset+=6;
        senderAddress = readDWord(buffer, offset);offset+=4;
        System.arraycopy(buffer, offset, targetMac, 0, 6);offset+=6;
        targetAddress = readDWord(buffer, offset);offset+=4;
    }
    public void handle(byte[] buffer, int offset, int len) {
        if (len<LEN)
            return;
        parse(buffer, offset);
        if (hardware == 1 && protocol == 0x800) {
            if (op == 1) {
                if (targetAddress == SERVER_ADDRESS) {
                    System.out.println("ARP");
                } else if (targetAddress == CLIENT_ADDRESS) {
                    System.out.println("ARP probe");
                }
            }
        }
    }

    int hardware;
    int protocol;
    int hlen;
    int plen;
    int op;
    byte[] senderMac = new byte[6];
    int senderAddress;
    byte[] targetMac = new byte[6];
    int targetAddress;
}
