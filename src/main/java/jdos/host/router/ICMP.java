package jdos.host.router;

public class ICMP extends EtherUtil {
    private void parse(byte[] buffer, int offset) {
        type = buffer[offset] & 0xFF;
        code = buffer[offset+1] & 0xFF;
        checksum = (short)readWord(buffer, offset+2);
    }
    public void handle(byte[] buffer, int offset, int len) {
        System.out.print("Received ICMP Packet ");
        parse(buffer, offset);
        if (type == 0) {
            System.out.println(" PING");
        } else {
            String strType = null;

            switch (type) {
                case 3:
                    strType = "Destination Unreachable";
                    break;
                case 4:
                    strType = "Source Quench";
                    break;
                case 5:
                    strType = "Redirect Message";
                    break;
                case 8:
                    strType = "Echo Request";
                    break;
                case 9:
                    strType = "Router Advertisement";
                    break;
                case 10:
                    strType = "Router Solicitation";
                    break;
                case 11:
                    strType = "Time Exceeded";
                    break;
                case 12:
                    strType = "Parameter Problem: Bad IP header";
                    break;
                case 13:
                    strType = "Timestamp";
                    break;
                case 14:
                    strType = "Timestamp Reply";
                    break;
                case 15:
                    strType = "Information Request";
                    break;
                case 16:
                    strType = "Information Reply";
                    break;
                case 17:
                    strType = "Address Mask Request";
                    break;
                case 18:
                    strType = "Address Mask Reply";
                    break;
                case 30:
                    strType = "Traceroute";
                    break;
            }
            System.out.print(" type="+type);
            if (strType != null)
                System.out.print("("+strType+")");
            System.out.println(" code="+code);
        }
    }

    public int type;
    public int code;
    public short checksum;
}
