package jdos.host;

import jdos.misc.Log;
import jdos.misc.setup.Section_prop;
import jdos.util.Ptr;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.nio.JMemory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FowardPCapEthernet implements Ethernet {
    Socket socket;
    DataOutputStream dos;
    DataInputStream dis;

    public void send(byte[] buffer, int offset, int len) {
        try {
            dos.writeInt(len);
            dos.write(buffer, offset, len);
        } catch (Exception e) {
        }
    }
    static byte[] buffer = new byte[4096];
    public void receive(RxFrame frame) {
        try {
            int len;

            do {
                if (dis.available()<4)
                    break;
                len = dis.readInt();
                if (len>buffer.length)
                    buffer = new byte[len];
                dis.readFully(buffer, 0, len);
                if (len>0) {
                    frame.rx_frame(new Ptr(buffer, 0), len);
                }
            } while (len>0);
        } catch (Exception e) {
        }
    }

    public boolean open(Section_prop section, byte[] mac) {
        try {
            socket = new Socket(section.Get_string("pcaphost"), section.Get_int("pcapport"));
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void close() {
        try {
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
        }
        socket = null;
        dis = null;
        dos = null;
    }

    static public void startServer(String nic, int port) {
        // Just make sure we are good to go
        Pcap pcaptmp = PCapEthernet.open(nic, false);
        if (pcaptmp==null) {
            return;
        }
        pcaptmp.close();
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Log.log_msg("Listening on port "+port+" for pcap forwarding.  Hit q [ENTER] to quit");
            while (true) {
                Thread exitThread = new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                char c = (char)System.in.read();
                                if (c == 'q') {
                                    System.exit(0);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                });
                exitThread.start();
                final Socket socket = serverSocket.accept();
                final String address = socket.getInetAddress().toString();
                Log.log_msg("  Accepted connection from "+address);
                final Pcap pcap = PCapEthernet.open(nic, true);
                Thread serviceIn = new Thread(new Runnable() {
                    public void run() {
                        try {
                            DataInputStream dis = new DataInputStream(socket.getInputStream());
                            byte[] buffer = new byte[4096];
                            while (true) {
                                int len = dis.readInt();
                                if (len<0) {
                                    return;
                                }
                                if (len>buffer.length) {
                                    buffer = new byte[len];
                                }
                                dis.readFully(buffer, 0, len);
                                synchronized (pcap) {
                                    pcap.sendPacket(buffer, 0, len);
                                }
                            }
                        } catch (Exception e) {
                            Log.log_msg("  Dropped connection from "+address);
                        } finally {
                            try {pcap.close();} catch (Exception e1){}
                        }
                    }
                });
                serviceIn.start();
                Thread serviceOut = new Thread(new Runnable() {
                    public void run() {
                        try {
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            while (true) {
                                PcapHeader header = new PcapHeader(JMemory.POINTER);
                                JBuffer buffer = new JBuffer(JMemory.POINTER);
                                synchronized (pcap) {
                                    while (pcap.nextEx(header, buffer) == Pcap.NEXT_EX_OK) {
                                        byte[] data = buffer.getByteArray(0, header.hdr_len());
                                        dos.writeInt(data.length);
                                        dos.write(data);
                                    }
                                }
                                Thread.sleep(10);
                            }
                        } catch (Exception e) {
                        } finally {
                            try {pcap.close();} catch (Exception e1){}
                        }
                    }
                });
                serviceOut.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
