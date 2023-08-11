package jdos.hardware;

import jdos.misc.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class IPXServer {
    static private IPX.IPXAddress ipxServerIp;  // IPAddress for server's listening port
    static private DatagramSocket ipxServerSocket;  // Listening server socket

    static private final IPX.packetBuffer[] connBuffer = new IPX.packetBuffer[IPX.SOCKETTABLESIZE];

    static private final IPX.IPXAddress[] ipconn = new IPX.IPXAddress[IPX.SOCKETTABLESIZE];  // Active TCP/IP connection
    static private final DatagramSocket[] tcpconn = new DatagramSocket[IPX.SOCKETTABLESIZE];  // Active TCP/IP connections
    static private Timer.TIMER_TickHandler serverTimer;

    static private /*Bit8u*/byte packetCRC(/*Bit8u*/byte[] buffer, /*Bit16u*/int bufSize) {
        /*Bit8u*/byte tmpCRC = 0;
        /*Bit16u*/int i;
        for(i=0;i<bufSize;i++) {
            tmpCRC ^= buffer[i];
        }
        return tmpCRC;
    }

    /*
    static void closeSocket(Bit16u sockidx) {
        Bit32u host;

        host = ipconn[sockidx].host;
        LOG_MSG("IPXSERVER: %d.%d.%d.%d disconnected", CONVIP(host));

        SDLNet_TCP_DelSocket(serverSocketSet,tcpconn[sockidx]);
        SDLNet_TCP_Close(tcpconn[sockidx]);
        connBuffer[sockidx].connected = false;
        connBuffer[sockidx].waitsize = false;
    }
    */

    private static void sendIPXPacket(/*Bit8u*/byte[] buffer, /*Bit16s*/int bufSize) {
        /*Bit16u*/int srcport, destport;
        /*Bit32u*/int srchost, desthost;
        /*Bit16u*/int i;
        /*Bits*/int result;
        IPX.IPXHeader tmpHeader = new IPX.IPXHeader();
        tmpHeader.load(buffer);

        srchost = tmpHeader.src.addr.host();
        desthost = tmpHeader.dest.addr.host();

        srcport = tmpHeader.src.addr.port();
        destport = tmpHeader.dest.addr.port();


        if(desthost == 0xffffffff) {
            // Broadcast
            for(i=0;i<IPX.SOCKETTABLESIZE;i++) {
                if(connBuffer[i].connected && ((ipconn[i].host != srchost)||(ipconn[i].port!=srcport))) {
                    DatagramPacket outPacket = new DatagramPacket(buffer, bufSize, ipconn[i].address, ipconn[i].port);
                    try {
                        ipxServerSocket.send(outPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //LOG_MSG("IPXSERVER: Packet of %d bytes sent from %d.%d.%d.%d to %d.%d.%d.%d (BROADCAST) (%x CRC)", bufSize, CONVIP(srchost), CONVIP(ipconn[i].host), packetCRC(&buffer[30], bufSize-30));
                }
            }
        } else {
            // Specific address
            for(i=0;i<IPX.SOCKETTABLESIZE;i++) {
                if((connBuffer[i].connected) && (ipconn[i].host == desthost) && (ipconn[i].port == destport)) {
                    DatagramPacket outPacket = new DatagramPacket(buffer, bufSize, ipconn[i].address, ipconn[i].port);
                    try {
                        ipxServerSocket.send(outPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //LOG_MSG("IPXSERVER: Packet sent from %d.%d.%d.%d to %d.%d.%d.%d", CONVIP(srchost), CONVIP(desthost));
                }
            }
        }
    }

    static IPX.IPXAddress IPX_isConnectedToServer(/*Bits*/int tableNum) {
        if(tableNum >= IPX.SOCKETTABLESIZE) return null;
        if (!connBuffer[tableNum].connected)
            return null;
        return ipconn[tableNum];
    }

    private static void ackClient(IPX.IPXAddress clientAddr) {
        IPX.IPXHeader regHeader = new IPX.IPXHeader();

        regHeader.checkSum = (short)0xffff;
        regHeader.dest.network = 0;
        regHeader.dest.addr.setHost(clientAddr.host);
        regHeader.dest.addr.setPort(clientAddr.port);
        regHeader.dest.socket = 0x2;

        regHeader.src.network = 1;
        regHeader.src.addr.setHost(ipxServerIp.host);
        regHeader.src.addr.setPort(ipxServerIp.port);
        regHeader.src.socket = 0x2;
        regHeader.transControl = 0;

        byte[] outbuffer = regHeader.toByteArray();
        DatagramPacket outPacket = new DatagramPacket(outbuffer, outbuffer.length, clientAddr.address, clientAddr.port);
        try {
            ipxServerSocket.send(outPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static private final Thread serverThread = new Thread() {
        public void run() {
            byte[] tmpBuffer = new byte[IPX.IPXBUFFERSIZE];
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(tmpBuffer, tmpBuffer.length);
                    ipxServerSocket.receive(receivePacket);

                    // Check to see if incoming packet is a registration packet
                    // For this, I just spoofed the echo protocol packet designation 0x02
                    IPX.IPXHeader tmpHeader = new IPX.IPXHeader();
                    tmpHeader.load(tmpBuffer);

                    // Check to see if echo packet
                    if (tmpHeader.dest.socket == 0x2 && tmpHeader.dest.addr.host() == 0x0) {
                        // Null destination node means its a server registration packet
                        for(int i=0;i<IPX.SOCKETTABLESIZE;i++) {
                            if(!connBuffer[i].connected) {
                                // Use prefered host IP rather than the reported source IP
                                // It may be better to use the reported source
                                ipconn[i] = new IPX.IPXAddress();
                                ipconn[i].address = receivePacket.getAddress();
                                byte[] b = ipconn[i].address.getAddress();
                                ipconn[i].host = ((b[3] & 0xFF) << 24) | ((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | ((b[0] & 0xFF));
                                ipconn[i].port = receivePacket.getPort();

                                connBuffer[i].connected = true;
                                Log.log_msg("IPXSERVER: Connect from " + receivePacket.getAddress().getHostAddress());
                                ackClient(ipconn[i]);
                                break;
                            } else if((ipconn[i].host == tmpHeader.src.addr.host()) && (ipconn[i].port == tmpHeader.src.addr.port())) {
                                Log.log_msg("IPXSERVER: Reconnect from " + receivePacket.getAddress().getHostAddress());
                                // Update anonymous port number if changed
                                ipconn[i].port = receivePacket.getPort();
                                ackClient(ipconn[i]);
                                break;
                            }
                        }
                    } else {
                        // IPX packet is complete.  Now interpret IPX header and send to respective IP address
                        sendIPXPacket(tmpBuffer, receivePacket.getLength());
                    }
                } catch (Exception e) {
                    if (ipxServerSocket.isClosed()) break;
                    e.printStackTrace();
                }
            }
        }
    };

    static void IPX_StopServer() {
        ipxServerSocket.close();
        try {serverThread.join();} catch (Exception e) {}
    }

    static boolean IPX_StartServer(/*Bit16u*/int portnum) {
        try {
            ipxServerIp = new IPX.IPXAddress();
            ipxServerIp.address = InetAddress.getByName(null);
            ipxServerIp.port = portnum;
            ipxServerSocket = new DatagramSocket(portnum);
            serverThread.start();
        } catch (Exception e) {
            return false;
        }

        for(int i=0;i<IPX.SOCKETTABLESIZE;i++) {
            if (connBuffer[i] == null) connBuffer[i] = new IPX.packetBuffer();
            connBuffer[i].connected = false;
        }
        return true;
    }
}
