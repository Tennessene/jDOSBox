package jdos.host.router;

import jdos.host.UserEthernet;

public class BootP extends EtherUtil {
    public void parse(byte[] buffer, int offset, int len) {
        int originalOffset = offset;
        op = buffer[offset] & 0xFF;
        htype = buffer[offset+1] & 0xFF;
        hlen = buffer[offset+2] & 0xFF;
        hops = buffer[offset+3] & 0xFF;
        xid = readDWord(buffer, offset + 4);
        secs = readWord(buffer, offset + 8);
        flags = readWord(buffer, offset + 10);
        ciaddr = readDWord(buffer, offset + 12);
        yiaddr = readDWord(buffer, offset + 16);
        siaddr = readDWord(buffer, offset + 20);
        giaddr = readDWord(buffer, offset + 24);
        offset+=28;
        System.arraycopy(buffer, offset, hwaddr, 0, hwaddr.length);
        offset+=hwaddr.length;
        System.arraycopy(buffer, offset, sname, 0, sname.length);
        offset+=sname.length;
        System.arraycopy(buffer, offset, file, 0, file.length);
        offset+=file.length;
        vend = new byte[Math.min(312, len-(offset-originalOffset))];
        System.arraycopy(buffer, offset, vend, 0, vend.length);
    }

    public void send(int vendorLen, int sourceAddress, int sourcePort, int destAddress, int destPort, int ttl) {
        if (vendorLen<72) {
            for (int i=vendorLen;i<72;i++)
                vend[i] = 0;
            vendorLen=72;
        }
        byte[] data = new byte[Ether.LEN+IP.LEN+UDP.LEN+236+vendorLen];
        int offset = Ether.LEN+IP.LEN+UDP.LEN;
        data[offset] = (byte)op;
        data[offset+1] = (byte)htype;
        data[offset+2] = (byte)hlen;
        data[offset+3] = (byte)hops;
        writeDWord(data, offset + 4, xid);
        writeWord(data, offset + 8, secs);
        writeWord(data, offset + 10, flags);
        writeDWord(data, offset + 12, ciaddr);
        writeDWord(data, offset + 16, yiaddr);
        writeDWord(data, offset + 20, siaddr);
        writeDWord(data, offset + 24, giaddr);
        offset+=28;
        System.arraycopy(hwaddr, 0, data, offset, hwaddr.length);
        offset+=hwaddr.length;
        System.arraycopy(sname, 0, data, offset, sname.length);
        offset+=sname.length;
        System.arraycopy(file, 0, data, offset, file.length);
        offset+=file.length;
        System.arraycopy(vend, 0, data, offset, vendorLen);
        UDP.output(sourceAddress, destAddress, data, Ether.LEN+IP.LEN, sourcePort, destPort, 236+vendorLen);
        IP.output(data, Ether.LEN, 17, sourceAddress, destAddress, 236+vendorLen+UDP.LEN+IP.LEN, IP.IPTOS_LOWDELAY, ttl);
        Ether.output(data, 0, UserEthernet.SERVER_MAC_ADDRESS, UserEthernet.ether.src);
        UserEthernet.frames.add(data);
    }

    private static final byte[] rfc1533_cookie = new byte[] {99, (byte)130, 83, 99};
    private static final int DHCPDISCOVER = 1;
    private static final int DHCPOFFER = 2;
    private static final int DHCPREQUEST = 3;
    private static final int DHCPACK = 5;
    private static final int DHCPNAK = 6;

    private static final int RFC1533_NETMASK = 1;
    private static final int RFC1533_GATEWAY = 3;
    private static final int RFC1533_DNS = 6;
    private static final int RFC1533_END = 255;

    private static final int RFC2132_REQ_ADDR = 50;
    private static final int RFC2132_LEASE_TIME = 51;
    private static final int RFC2132_MSG_TYPE = 53;
    private static final int RFC2132_SRV_ID = 54;
    private static final int RFC2132_MESSAGE = 56;

    public BootP() {
        bootp_response = new BootP(true);
    }

    public BootP(boolean isResponse) {
        bootp_response = null;
    }

    private final BootP bootp_response;

    public void handle(byte[] buffer, int offset, int len) {
        parse(buffer, offset, len);
        if (op != 1) { // BOOTP_REQUEST
            System.out.print("  BOOTP op=0x"+Integer.toHexString(op));
            return;
        }
        if (vend[0]!=99 || vend[1]!=-126 || vend[2]!=83 || vend[3]!=99) {
            return;
        }
        System.out.print("  DHCP");
        int msg = 0;
        int address = 0;
        try {
            for (int i=4;i<vend.length-1;i++) {
                if (vend[i]==0)
                    continue;
                if (vend[i]==(byte)0xFF)
                    break;
                int tag = vend[i] & 0xFF;
                int tagLen = vend[i+1] & 0xFF;
                switch (tag) {
                    case RFC2132_REQ_ADDR:
                        if (tagLen>=4)
                            address = readDWord(vend, i+2);
                        break;
                    case RFC2132_MSG_TYPE:
                        if (tagLen>=1)
                            msg = vend[i+2] & 0xFF;
                        break;
                }
                i+=tagLen+1;
            }
        } catch (Exception e) {

        }
        if (msg == DHCPREQUEST) {
            System.out.print(" Request");
            if (address != 0) {
                System.out.print(" ");
                printAddress(address);
            }
        } else if (msg == DHCPDISCOVER) {
            System.out.print(" Discovery");
        } else {
            System.out.print(" msg="+msg);
        }
        if (msg == DHCPREQUEST && address == 0 && ciaddr != 0) {
            address = ciaddr;
        }
        System.out.println();
        System.out.println("    Transaction ID: 0x"+Integer.toHexString(xid));
        if (msg == 0)
            msg = DHCPREQUEST; // /* Force reply for old BOOTP clients */
        if (msg != DHCPDISCOVER && msg != DHCPREQUEST)
            return;
        if (msg == DHCPDISCOVER) {
            address = CLIENT_ADDRESS;
        } else if (address != 0) {
            if (address != CLIENT_ADDRESS)
                address = -1;
        } else {
            address = CLIENT_ADDRESS;
        }
        bootp_response.op = 2; // BOOTP_REPLY
        bootp_response.xid = xid;
        bootp_response.htype = 1;
        bootp_response.hlen = 6;
        System.arraycopy(hwaddr, 0, bootp_response.hwaddr, 0, hwaddr.length);
        bootp_response.yiaddr = address;
        bootp_response.siaddr = SERVER_ADDRESS;
        bootp_response.vend[0]=99;
        bootp_response.vend[1]=-126;
        bootp_response.vend[2]=83;
        bootp_response.vend[3]=99;
        int index=4;
        if (address != -1) {
            System.out.println("  DHCP Response "+((msg == DHCPDISCOVER)?"OFFER":"ACK"));
            System.out.println("    Transaction ID: 0x"+Integer.toHexString(bootp_response.xid));
            bootp_response.vend[index++] = RFC2132_MSG_TYPE;
            bootp_response.vend[index++] = 1;
            bootp_response.vend[index++] = (byte)((msg == DHCPDISCOVER)?DHCPOFFER:DHCPACK);

            System.out.print("    Client Address: ");printAddress(address);System.out.println();

            System.out.print("    Server ID: ");printAddress(SERVER_ADDRESS);System.out.println();
            bootp_response.vend[index++] = RFC2132_SRV_ID;
            bootp_response.vend[index++] = 4;
            writeDWord(bootp_response.vend, index, SERVER_ADDRESS);
            index+=4;

            System.out.print("    Netmask: ");printAddress(SERVER_NETMASK);System.out.println();
            bootp_response.vend[index++] = RFC1533_NETMASK;
            bootp_response.vend[index++] = 4;
            writeDWord(bootp_response.vend, index, SERVER_NETMASK);
            index+=4;

            System.out.print("    Gateway: ");printAddress(SERVER_ADDRESS);System.out.println();
            bootp_response.vend[index++] = RFC1533_GATEWAY;
            bootp_response.vend[index++] = 4;
            writeDWord(bootp_response.vend, index, SERVER_ADDRESS);
            index+=4;

            System.out.print("    DNS: ");printAddress(SERVER_ADDRESS);System.out.println();
            bootp_response.vend[index++] = RFC1533_DNS;
            bootp_response.vend[index++] = 4;
            writeDWord(bootp_response.vend, index, SERVER_ADDRESS);
            index+=4;

            System.out.println("    Lease Time : 1 day");
            bootp_response.vend[index++] = RFC2132_LEASE_TIME;
            bootp_response.vend[index++] = 4;
            writeDWord(bootp_response.vend, index, 3600*24);
            index+=4;
        } else {
            System.out.println("  DHCP Response Nak: requested address not available");
            System.out.println("    Transaction ID: 0x"+Integer.toHexString(bootp_response.xid));
            bootp_response.vend[index++] = RFC2132_MSG_TYPE;
            bootp_response.vend[index++] = 1;
            bootp_response.vend[index++] = DHCPNAK;

            String nak_msg = "requested address not available";
            bootp_response.vend[index++] = RFC2132_MESSAGE;
            bootp_response.vend[index++] = (byte)nak_msg.length();
            System.arraycopy(nak_msg.getBytes(), 0, bootp_response.vend, index, nak_msg.length());
            index+=nak_msg.length();
        }
        bootp_response.vend[index++] = (byte)RFC1533_END;
        bootp_response.send(index, SERVER_ADDRESS, 67, -1, 68, IP.IPDEFTTL);
    }

    int op;
    int htype;
    int hlen;
    int hops;
    int xid;
    int secs;
    int flags;
    int ciaddr; // client
    int yiaddr; // your
    int siaddr; // server
    int giaddr; // gateway
    byte[] hwaddr = new byte[16];
    byte[] sname = new byte[64];
    byte[] file = new byte[128];
    byte[] vend = new byte[312];
}
