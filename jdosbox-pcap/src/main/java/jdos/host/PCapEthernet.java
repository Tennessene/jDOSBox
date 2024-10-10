package jdos.host;

import jdos.misc.Log;
import jdos.misc.setup.Section_prop;
import jdos.util.Ptr;
import jdos.util.StringHelper;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.nio.JMemory;

import java.util.ArrayList;

public class PCapEthernet implements Ethernet{
    Pcap pcap;

    public void send(byte[] buffer, int offset, int len) {
        pcap.sendPacket(buffer, offset, len);
    }

    public void receive(RxFrame frame) {
        PcapHeader header = new PcapHeader(JMemory.POINTER);
        JBuffer buffer = new JBuffer(JMemory.POINTER);
        while (pcap.nextEx(header, buffer) == Pcap.NEXT_EX_OK) {
            byte[] data = buffer.getByteArray(0, header.hdr_len());
            if (!frame.rx_frame(new Ptr(data, 0),header.hdr_len()))
                return;
        }
    }

    public void close() {
        if (pcap!=null) {
            pcap.close();
            pcap = null;
        }
    }
    public boolean open(Section_prop section, byte[] mac) {
        pcap = open(section.Get_string("realnic"), true);
        return pcap!=null;
    }
    static public Pcap open(String realnicstring, boolean async) {
        try {
            ArrayList alldevs = new ArrayList(); // Will be filled with NICs
            StringBuilder errbuf = new StringBuilder(); // For any error msgs

            /***************************************************************************
             * First get a list of devices on this system
             **************************************************************************/
            int r = Pcap.findAllDevs(alldevs, errbuf);
            if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
                Log.log_msg("Cannot enumerate network interfaces: " + errbuf.toString());
                return null;
            }

            if (realnicstring.equalsIgnoreCase("list")) {
                int i = 0;
                Log.log_msg("\nNetwork Interface List \n-----------------------------------");
                for (i=0;i<alldevs.size();i++) {
                    PcapIf currentdev = (PcapIf)alldevs.get(i);
                    String desc = currentdev.getDescription();
                    if (desc == null || desc.length()==0)
                        desc = "no description";
                    i++;
                    Log.log_msg(StringHelper.sprintf("%2d. %s\n    (%s)\n", new Object[]{new Integer(i), currentdev.getName(), desc}));
                }
                Pcap.freeAllDevs(alldevs, errbuf);
                return null;
            }
            PcapIf dev = null;
            try {
                int index = Integer.parseInt(realnicstring);
                if (index>=0 && index<=alldevs.size()) {
                    dev = (PcapIf)alldevs.get(index);
                }
            } catch (Exception e) {
                for (int i=0;i<alldevs.size();i++) {
                    PcapIf currentdev = (PcapIf)alldevs.get(i);
                    if (currentdev.getName().contains(realnicstring)) {
                        dev = currentdev;
                        break;
                    } else if (currentdev.getDescription()!=null && currentdev.getDescription().contains(realnicstring)) {
                        dev = currentdev;
                        break;
                    }
                }
            }
            if (dev == null) {
                Log.log_msg("Unable to find network interface - check realnic parameter\n");
                Pcap.freeAllDevs(alldevs, errbuf);
                return null;
            }
            String desc = dev.getDescription();
            if (desc == null || desc.length() == 0)
                desc = "no description";
    		Log.log_msg("Using Network interface:\n" + dev.getName() + "\n(" + desc + ")\n");
            Pcap pcap = Pcap.openLive(dev.getName(), 65536, Pcap.MODE_PROMISCUOUS, -1, errbuf);
            if (pcap == null) {
                Log.log_msg("\\nUnable to open the interface: " + errbuf.toString());
                Pcap.freeAllDevs(alldevs, errbuf);
                return null;
            }
            if (async)
                pcap.setNonBlock(1, errbuf);
            return pcap;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
