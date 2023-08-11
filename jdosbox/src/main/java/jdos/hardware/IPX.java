package jdos.hardware;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.Dos_misc;
import jdos.dos.Dos_system;
import jdos.dos.Dos_tables;
import jdos.dos.drives.Drive_virtual;
import jdos.gui.Main;
import jdos.misc.Log;
import jdos.misc.Program;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.IntRef;
import jdos.util.StringHelper;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class IPX extends Module_base {
    static final int SOCKETTABLESIZE = 150; // DOS IPX driver was limited to 150 open sockets
    static final int IPXBUFFERSIZE = 1424;

    // In Use Flag codes
    static private final int USEFLAG_AVAILABLE  = 0x00;
    static private final int USEFLAG_AESTEMP    = 0xe0;
    static private final int USEFLAG_IPXCRIT    = 0xf8;
    static private final int USEFLAG_SPXLISTEN  = 0xf9;
    static private final int USEFLAG_PROCESSING = 0xfa;
    static private final int USEFLAG_HOLDING    = 0xfb;
    static private final int USEFLAG_AESWAITING = 0xfc;
    static private final int USEFLAG_AESCOUNT   = 0xfd;
    static private final int USEFLAG_LISTENING  = 0xfe;
    static private final int USEFLAG_SENDING    = 0xff;

    // Completion codes
    static private final int COMP_SUCCESS         = 0x00;
    static private final int COMP_REMOTETERM      = 0xec;
    static private final int COMP_DISCONNECT      = 0xed;
    static private final int COMP_INVALIDID       = 0xee;
    static private final int COMP_SPXTABLEFULL    = 0xef;
    static private final int COMP_EVENTNOTCANCELED= 0xf9;
    static private final int COMP_NOCONNECTION    = 0xfa;
    static private final int COMP_CANCELLED       = 0xfc;
    static private final int COMP_MALFORMED       = 0xfd;
    static private final int COMP_UNDELIVERABLE   = 0xfe;
    static private final int COMP_HARDWAREERROR   = 0xff;

    static final class IPXAddress {
        InetAddress address;
        int host;
        int port;
    }

    static public final class packetBuffer {
        /*Bit8u*/byte[] buffer = new byte[1024];
        /*Bit16s*/short packetSize;  // Packet size remaining in read
        /*Bit16s*/short packetRead;  // Bytes read of total packet
        boolean inPacket;      // In packet reception flag
        boolean connected;		// Connected flag
        boolean waitsize;
    }

    static public final class nodeType {
	    public /*Uint8*/byte[] node = new byte[6];
    }

    static final class IPXHeader {
        /*Uint8*/short checkSum;
        /*Uint8*/short length = 30;
        /*Uint8*/short transControl; // Transport control
        /*Uint8*/short pType; // Packet type

        static public class transport {
            /*Uint8*/int network;
            static public class addrtype {
                //nodeType byNode = new nodeType();
                nodeType byNode = new nodeType();
                public void setHost(int host) {
                    byNode.node[0] = (byte)(host & 0xFF);
                    byNode.node[1] = (byte)((host >> 8) & 0xFF);
                    byNode.node[2] = (byte)((host >> 16) & 0xFF);
                    byNode.node[3] = (byte)((host >> 24) & 0xFF);
                }
                public void setPort(int port) {
                    byNode.node[4] = (byte)(port & 0xFF);
                    byNode.node[5] = (byte)((port >> 8) & 0xFF);
                }
                public int host() {
                    return (byNode.node[0] & 0xFF) | ((byNode.node[1] & 0xFF) << 8) | ((byNode.node[2] & 0xFF) << 16) | ((byNode.node[3] & 0xFF) << 24);
                }
                public String hostAsString() {
                    return (byNode.node[0] & 0xFF) +"."+ (byNode.node[1] & 0xFF) +"."+ (byNode.node[2] & 0xFF) +"."+ (byNode.node[3] & 0xFF);
                }
                public int port() {
                    return (byNode.node[4] & 0xFF) | ((byNode.node[5] & 0xFF) << 8);
                }
            }
            final addrtype addr = new addrtype();
            /*Uint8*/short socket;
        }
        final transport dest = new transport();
        final transport src = new transport();

        byte[] toByteArray() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                write16(bos, checkSum);
                write16(bos, length);
                bos.write(transControl);
                bos.write(pType);
                write32(bos, dest.network);
                bos.write(dest.addr.byNode.node);
                write16(bos, dest.socket);
                write32(bos, src.network);
                bos.write(src.addr.byNode.node);
                write16(bos, src.socket);
            } catch (Exception e) {
            }
            return bos.toByteArray();
        }

        void load(byte[] data) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            try {
                checkSum = read16(bis);
                length = read16(bis);
                transControl = (short)bis.read();
                pType = (short)bis.read();
                dest.network = read32(bis);
                bis.read(dest.addr.byNode.node);
                dest.socket = read16(bis);
                src.network = read32(bis);
                bis.read(src.addr.byNode.node);
                src.socket = read16(bis);
            } catch (Exception e) {

            }
        }
    }

    final static private class IPaddress {
        /*Uint32*/int host;            /* 32-bit IPv4 host address */
        /*Uint16*/int port;            /* 16-bit protocol port */
    }

    final static private class ipxnetaddr {
        /*Uint8*/byte[] netnum = new byte[4];   // Both are big endian
        /*Uint8*/byte[] netnode = new byte[6];

        public byte[] toByteArray() {
            byte[] result = new byte[10];
            System.arraycopy(netnum, 0, result, 0, 4);
            System.arraycopy(netnode, 0, result, 4, 6);
            return result;
        }

        public int netnum() {
            return (netnum[3] & 0xFF) | ((netnum[2] & 0xFF) << 8) | ((netnum[1] & 0xFF) << 16) | ((netnum[0] & 0xFF) << 24);
        }

        public void netnum(int num) {
            netnum[3] = (byte)(num & 0xFF);
            netnum[2] = (byte)((num >> 8)& 0xFF);
            netnum[1] = (byte)((num >> 16)& 0xFF);
            netnum[0] = (byte)((num >> 24)& 0xFF);
        }
    }

    final static private class fragmentDescriptor {
        /*Bit16u*/int offset;
        /*Bit16u*/int segment;
        /*Bit16u*/int size;
    }

    static private final ipxnetaddr localIpxAddr = new ipxnetaddr();

    static private/*Bit32u*/int udpPort;
    static private boolean isIpxServer;
    static private boolean isIpxConnected;
    static private InetAddress ipxServConnIp;			// IPAddress for client connection to server
    static private DatagramSocket ipxClientSocket;

    private static /*RealPt*/int ipx_callback;

    static private final packetBuffer incomingPacket = new packetBuffer();

    private static /*Bit16u*/int socketCount;
    private static /*Bit16u*/int[] opensockets = new int[SOCKETTABLESIZE];

    private static /*Bit16u*/int swapByte(/*Bit16u*/int sockNum) {
        return (((sockNum >>> 8) & 0xFF) | ((sockNum & 0xFF) << 8));
    }

    //#ifdef IPX_DEBUGMSG
    static private /*Bitu*/int ECBSerialNumber = 0;
    static private /*Bitu*/int ECBAmount = 0;
    //#endif

    static private ECBClass ECBList;  // Linked list of ECB's
    static private ECBClass ESRList;	// ECBs waiting to be ESR notified

    private static class ECBClass {
        public /*RealPt*/int ECBAddr;
        public boolean isInESRList;
        ECBClass prevECB;	// Linked List
        ECBClass nextECB;

        public /*Bit8u*/int iuflag;		// Need to save data since we are not always in
        public /*Bit16u*/int mysocket;	// real mode

        public /*Bit8u*/byte[] databuffer;	// received data is stored here until we get called
        public /*Bitu*/int buflen;		// by Interrupt

        public /*Bitu*/int SerialNumber;

        public ECBClass(/*Bit16u*/int segment, /*Bit16u*/int offset) {
            ECBAddr = Memory.RealMake(segment, offset);
            databuffer = null;

            if (Config.IPX_DEBUGMSG) {
                SerialNumber = ECBSerialNumber;
                ECBSerialNumber++;
                ECBAmount++;

                Log.log_msg(StringHelper.sprintf("ECB: SN%7d created.   Number of ECBs: %3d, ESR %4x:%4x, ECB %4x:%4x",
                    new Object[] {
                            new Integer(SerialNumber),
                            new Integer(ECBAmount),
                            new Integer(Memory.real_readw(Memory.RealSeg(ECBAddr),Memory.RealOff(ECBAddr)+6)),
                            new Integer(Memory.real_readw(Memory.RealSeg(ECBAddr),Memory.RealOff(ECBAddr)+4)),
                            new Integer(segment),new Integer(offset)}));
            }

            isInESRList = false;
            prevECB = null;
            nextECB = null;

            if (ECBList == null)
                ECBList = this;
            else {
                // Transverse the list until we hit the end
                ECBClass useECB = ECBList;

                while(useECB.nextECB != null)
                    useECB = useECB.nextECB;

                useECB.nextECB = this;
                this.prevECB = useECB;
            }

            iuflag = getInUseFlag();
            mysocket = getSocket();
        }

        public /*Bit16u*/int getSocket() {
            return swapByte(Memory.real_readw(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr) + 0xa));
        }

        public /*Bit8u*/byte getInUseFlag() {
            return (byte)Memory.real_readb(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr) + 0x8);
        }

        public void setInUseFlag(/*Bit8u*/int flagval) {
            iuflag = flagval;
            Memory.real_writeb(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr) + 0x8, flagval);
        }

        public void setCompletionFlag(/*Bit8u*/int flagval) {
            Memory.real_writeb(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr) + 0x9, flagval);
        }

        public /*Bit16u*/int getFragCount() {
            return Memory.real_readw(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr) + 34);
        }

        public boolean writeData() {
            /*Bitu*/int length=buflen;
            /*Bit8u*/byte[] buffer = databuffer;
            fragmentDescriptor tmpFrag = new fragmentDescriptor();
            setInUseFlag(USEFLAG_AVAILABLE);
            /*Bitu*/int fragCount = getFragCount();
            /*Bitu*/int bufoffset = 0;
            for(/*Bitu*/int i = 0;i < fragCount;i++) {
                getFragDesc(i,tmpFrag);
                for(/*Bitu*/int t = 0;t < tmpFrag.size;t++) {
                    Memory.real_writeb(tmpFrag.segment, tmpFrag.offset + t, buffer[bufoffset]);
                    bufoffset++;
                    if(bufoffset >= length) {
                        setCompletionFlag(COMP_SUCCESS);
                        setImmAddress(buffer, 22);  // Write in source node
                        return true;
                    }
                }
            }
            if(bufoffset < length) {
                setCompletionFlag(COMP_MALFORMED);
                return false;
            }
            return false;
        }

        public void writeDataBuffer(/*Bit8u*/byte[] buffer, /*Bit16u*/int length) {
            databuffer = new /*Bit8u*/byte[length];
            System.arraycopy(buffer, 0, databuffer, 0, length);
            buflen=length;
        }

        public void getFragDesc(/*Bit16u*/int descNum, fragmentDescriptor fragDesc) {
            /*Bit16u*/int memoff = Memory.RealOff(ECBAddr) + 30 + ((descNum+1) * 6);
            fragDesc.offset = Memory.real_readw(Memory.RealSeg(ECBAddr), memoff);
            memoff += 2;
            fragDesc.segment = Memory.real_readw(Memory.RealSeg(ECBAddr), memoff);
            memoff += 2;
            fragDesc.size = Memory.real_readw(Memory.RealSeg(ECBAddr), memoff);
        }

        public /*RealPt*/int getESRAddr() {
            return Memory.RealMake(Memory.real_readw(Memory.RealSeg(ECBAddr),
            Memory.RealOff(ECBAddr)+6),
            Memory.real_readw(Memory.RealSeg(ECBAddr),
            Memory.RealOff(ECBAddr)+4));
        }

        public void NotifyESR() {
            /*Bit32u*/long ESRval = Memory.real_readd(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr)+4);
            if(ESRval!=0 || databuffer!=null) { // databuffer: write data at realmode/v86 time
                // LOG_IPX("ECB: SN%7d to be notified.", SerialNumber);
                // take the ECB out of the current list
                if(prevECB == null) {	// was the first in the list
                    ECBList = nextECB;
                    if(ECBList != null) ECBList.prevECB = null;
                } else {		// not the first
                    prevECB.nextECB = nextECB;
                    if(nextECB != null) nextECB.prevECB = prevECB;
                }

                nextECB = null;
                // put it to the notification queue
                if(ESRList==null) {
                    ESRList = this;
                    prevECB = null;
                } else  {// put to end of ESR list
                    ECBClass useECB = ESRList;

                    while(useECB.nextECB != null)
                        useECB = useECB.nextECB;

                    useECB.nextECB = this;
                    prevECB = useECB;
                }
                isInESRList = true;
                Pic.PIC_ActivateIRQ(11);
            }
            // this one does not want to be notified, delete it right away
            else close();
        }

        public void setImmAddress(/*Bit8u*/byte[] immAddr, int off) {
            for(/*Bitu*/int i=0;i<6;i++)
                Memory.real_writeb(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr)+28+i, immAddr[i+off]);
        }
        public void getImmAddress(/*Bit8u*/byte[] immAddr) {
            for(/*Bitu*/int i=0;i<6;i++)
                immAddr[i] = (byte)Memory.real_readb(Memory.RealSeg(ECBAddr), Memory.RealOff(ECBAddr)+28+i);
        }

        public void close() {
            if (Config.IPX_DEBUGMSG) {
                ECBAmount--;
                Log.log_msg(StringHelper.sprintf("ECB: SN%7d destroyed. Remaining ECBs: %3d", new Object[] {new Integer(SerialNumber),new Integer(ECBAmount)}));
            }

            if(isInESRList) {
                // in ESR list, always the first element is deleted.
                ESRList=nextECB;
            } else {
                if(prevECB == null) {	// was the first in the list
                    ECBList = nextECB;
                    if(ECBList != null) ECBList.prevECB = null;
                } else {	// not the first
                    prevECB.nextECB = nextECB;
                    if(nextECB != null) nextECB.prevECB = prevECB;
                }
            }
        }
    }

    private static boolean sockInUse(/*Bit16u*/int sockNum) {
        for(/*Bitu*/int i=0;i<socketCount;i++) {
            if (opensockets[i] == sockNum) return true;
        }
        return false;
    }

    private static void OpenSocket() {
        /*Bit16u*/int sockNum, sockAlloc;
        sockNum = swapByte(CPU_Regs.reg_edx.word());

        if(socketCount >= SOCKETTABLESIZE) {
            CPU_Regs.reg_eax.low(0xfe); // Socket table full
            return;
        }

        if(sockNum == 0x0000) {
            // Dynamic socket allocation
            sockAlloc = 0x4002;
            while(sockInUse(sockAlloc) && (sockAlloc < 0x7fff)) sockAlloc++;
            if(sockAlloc > 0x7fff) {
                // I have no idea how this could happen if the IPX driver
                // is limited to 150 open sockets at a time
                Log.log_msg("IPX: Out of dynamic sockets");
            }
            sockNum = sockAlloc;
        } else {
            if(sockInUse(sockNum)) {
                CPU_Regs.reg_eax.low(0xff); // Socket already open
                return;
            }
        }

        opensockets[socketCount] = sockNum;
        socketCount++;

        CPU_Regs.reg_eax.low(0x00); // Success
        CPU_Regs.reg_edx.word(swapByte(sockNum));  // Convert back to big-endian
    }

    private static void CloseSocket() {
        /*Bit16u*/int sockNum, i;
        ECBClass tmpECB = ECBList;
        ECBClass tmp2ECB = ECBList;

        sockNum = swapByte(CPU_Regs.reg_edx.word());
        if(!sockInUse(sockNum)) return;

        for(i=0;i<socketCount-1;i++) {
            if (opensockets[i] == sockNum) {
                // Realign list of open sockets
                for (int j=i;j< SOCKETTABLESIZE -1;j++)
                    opensockets[j] = opensockets[j+1];
                break;
            }
        }
        --socketCount;

        // delete all ECBs of that socket
        while(tmpECB!=null) {
            tmp2ECB = tmpECB.nextECB;
            if(tmpECB.getSocket()==sockNum) {
                tmpECB.setCompletionFlag(COMP_CANCELLED);
                tmpECB.setInUseFlag(USEFLAG_AVAILABLE);
                tmpECB.close();
            }
            tmpECB = tmp2ECB;
        }
    }

    //static RealPt IPXVERpointer;

    private static final Dos_system.MultiplexHandler IPX_Multiplex = new Dos_system.MultiplexHandler() {
        public boolean call() {
            if(CPU_Regs.reg_eax.word() != 0x7a00) return false;
            CPU_Regs.reg_eax.low(0xff);
            CPU_Regs.SegSet16ES(Memory.RealSeg(ipx_callback));
            CPU_Regs.reg_edi.word(Memory.RealOff(ipx_callback));

            //CPU_Regs.reg_ebx.word(RealOff(IPXVERpointer));
            //CPU_Regs.reg_ecx.word(RealSeg(ipx_callback));
            return true;
        }
    };

    private static final Pic.PIC_EventHandler IPX_AES_EventHandler = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int param) {
            ECBClass tmpECB = ECBList;
            ECBClass tmp2ECB;
            while(tmpECB!=null) {
                tmp2ECB = tmpECB.nextECB;
                if(tmpECB.iuflag==USEFLAG_AESCOUNT && param==tmpECB.ECBAddr) {
                    tmpECB.setCompletionFlag(COMP_SUCCESS);
                    tmpECB.setInUseFlag(USEFLAG_AVAILABLE);
                    tmpECB.NotifyESR();
                    // LOG_IPX("AES Notification: ECB S/N %d",tmpECB.SerialNumber);
                    return;
                }
                tmpECB = tmp2ECB;
            }
            Log.log_msg("!!!! Rouge AES !!!!" );
        }
    };

    private static void handleIpxRequest() {
        ECBClass tmpECB;

        switch (CPU_Regs.reg_ebx.word()) {
            case 0x0000:	// Open socket
                OpenSocket();
                Log.log_msg(StringHelper.sprintf("IPX: Open socket %4x", new Object[]{new Integer(swapByte(CPU_Regs.reg_edx.word()))}));
                break;
            case 0x0001:	// Close socket
                Log.log_msg(StringHelper.sprintf("IPX: Close socket %4x", new Object[]{new Integer(swapByte(CPU_Regs.reg_edx.word()))}));
                CloseSocket();
                break;
            case 0x0002:	// get local target
                            // es:si
                            // Currently no support for multiple networks

                for(/*Bitu*/int i = 0; i < 6; i++)
                    Memory.real_writeb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()+i,Memory.real_readb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_esi.word()+i+4));

                CPU_Regs.reg_ecx.word(1);		// time ticks expected
                CPU_Regs.reg_eax.low(0x00);	//success
                break;

            case 0x0003:		// Send packet
                tmpECB = new ECBClass((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_esi.word());
                if(!incomingPacket.connected) {
                    tmpECB.setInUseFlag(USEFLAG_AVAILABLE);
                    tmpECB.setCompletionFlag(COMP_UNDELIVERABLE);
                    tmpECB.close();	// not notify?
                    CPU_Regs.reg_eax.low(0xff); // Failure
                } else {
                    tmpECB.setInUseFlag(USEFLAG_SENDING);
                    //LOG_IPX("IPX: Sending packet on %4x", tmpECB.getSocket());
                    CPU_Regs.reg_eax.low(0x00); // Success
                    sendPacket(tmpECB);
                }

                break;
            case 0x0004:  // Listen for packet
                tmpECB = new ECBClass((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_esi.word());
                // LOG_IPX("ECB: SN%7d RECEIVE.", tmpECB.SerialNumber);
                if(!sockInUse(tmpECB.getSocket())) {  // Socket is not open
                    CPU_Regs.reg_eax.low(0xff);
                    tmpECB.setInUseFlag(USEFLAG_AVAILABLE);
                    tmpECB.setCompletionFlag(COMP_HARDWAREERROR);
                    tmpECB.close();
                } else {
                    CPU_Regs.reg_eax.low(0x00);  // Success
                    tmpECB.setInUseFlag(USEFLAG_LISTENING);
                    /*LOG_IPX("IPX: Listen for packet on 0x%4x - ESR address %4x:%4x",
                        tmpECB.getSocket(),
                        RealSeg(tmpECB.getESRAddr()),
                        RealOff(tmpECB.getESRAddr()));*/
                }
                break;

            case 0x0005:	// SCHEDULE IPX EVENT
            case 0x0007:	// SCHEDULE SPECIAL IPX EVENT
            {
                tmpECB = new ECBClass((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_esi.word());
                // LOG_IPX("ECB: SN%7d AES. T=%fms.", tmpECB.SerialNumber,
                //	(1000.0f/(1193182.0f/65536.0f))*(float)CPU_Regs.reg_eax.word();
                Pic.PIC_AddEvent(IPX_AES_EventHandler, (1000.0f/(1193182.0f/65536.0f))*(float)CPU_Regs.reg_eax.word(),(int)tmpECB.ECBAddr);
                tmpECB.setInUseFlag(USEFLAG_AESCOUNT);
                break;
            }
            case 0x0006:	// cancel operation
            {
                /*RealPt*/int ecbaddress = Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_esi.word());
                tmpECB= ECBList;
                ECBClass tmp2ECB;
                while (tmpECB!=null) {
                    tmp2ECB=tmpECB.nextECB;
                    if(tmpECB.ECBAddr == ecbaddress) {
                        if(tmpECB.getInUseFlag()==USEFLAG_AESCOUNT)
                            Pic.PIC_RemoveSpecificEvents(IPX_AES_EventHandler,(int)ecbaddress);
                        tmpECB.setInUseFlag(USEFLAG_AVAILABLE);
                        tmpECB.setCompletionFlag(COMP_CANCELLED);
                        tmpECB.close();
                        CPU_Regs.reg_eax.low(0);	// Success
                        Log.log_msg("IPX: ECB canceled.");
                        return;
                    }
                    tmpECB=tmp2ECB;
                }
                CPU_Regs.reg_eax.low(0xff);	// Fail
                break;
            }
            case 0x0008:		// Get interval marker
                CPU_Regs.reg_eax.word(Memory.mem_readw(0x46c)); // BIOS_TIMER
                break;
            case 0x0009:		// Get internetwork address
            {
                Log.log_msg(StringHelper.sprintf("IPX: Get internetwork address %2x:%2x:%2x:%2x:%2x:%2x", new Object[] {
                    new Integer(localIpxAddr.netnode[5] & 0xFF), new Integer(localIpxAddr.netnode[4] & 0xFF),
                    new Integer(localIpxAddr.netnode[3] & 0xFF), new Integer(localIpxAddr.netnode[2] & 0xFF),
                    new Integer(localIpxAddr.netnode[1] & 0xFF), new Integer(localIpxAddr.netnode[0] & 0xFF)}));

                //Bit8u * addrptr = (Bit8u *)&localIpxAddr;
                byte[] addrptr = localIpxAddr.toByteArray();
                for(/*Bit16u*/int i=0;i<10;i++)
                    Memory.real_writeb((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_esi.word()+i,addrptr[i]);
                break;
            }
            case 0x000a:		// Relinquish control
                break;			// Idle thingy

            case 0x000b:		// Disconnect from Target
                break;			// We don't even connect

            case 0x000d:		// get packet size
                CPU_Regs.reg_ecx.word(0);		// retry count
                CPU_Regs.reg_eax.word(1024);	// real implementation returns 1024
                break;

            case 0x0010:		// SPX install check
                CPU_Regs.reg_eax.low(0);		// SPX not installed
                break;

            case 0x001a:		// get driver maximum packet size
                CPU_Regs.reg_ecx.word(0);		// retry count
                CPU_Regs.reg_eax.word(IPXBUFFERSIZE);	// max packet size: something near the
                                        // ethernet packet size
                break;

            default:
                Log.log_msg(StringHelper.sprintf("Unhandled IPX function: %4x", new Object[] {new Integer(CPU_Regs.reg_ebx.word())}));
                break;
        }
    }

    // Entrypoint handler
    final static private Callback.Handler IPX_Handler = new Callback.Handler() {
        public /*Bitu*/int call() {
            handleIpxRequest();
            return Callback.CBRET_NONE;
        }
        public String getName() {
            return "IPX";
        }
    };

    // INT 7A handler
    final static private Callback.Handler IPX_IntHandler = new Callback.Handler() {
        public /*Bitu*/int call() {
            handleIpxRequest();
            return Callback.CBRET_NONE;
        }
        public String getName() {
            return "IPX INT 7A";
        }
    };

    static private void write16(OutputStream os, int value) throws IOException {
        os.write((value >> 8 & 0xFF));
        os.write(value & 0xFF);
    }

    static private void write32(OutputStream os, int value) throws IOException {
        os.write((value >> 24 & 0xFF));
        os.write((value >> 16 & 0xFF));
        os.write((value >> 8 & 0xFF));
        os.write(value & 0xFF);
    }

    static private short read16(InputStream is) throws IOException {
        int b = is.read() & 0xFF;
        int a = is.read() & 0xFF;
        return (short)(a | (b << 8));
    }

    static private int read32(InputStream is) throws IOException {
        int d = is.read() & 0xFF;
        int c = is.read() & 0xFF;
        int b = is.read() & 0xFF;
        int a = is.read() & 0xFF;
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    private static void pingAck(IPaddress retAddr) {
        IPXHeader regHeader = new IPXHeader();

        regHeader.checkSum = (short)0xFFFF;
        regHeader.dest.network = 0;
        regHeader.dest.addr.setHost(retAddr.host);
        regHeader.dest.addr.setPort(retAddr.port);
        regHeader.dest.socket = 2;
        regHeader.src.network = 0;
        System.arraycopy(localIpxAddr.netnode, 0, regHeader.src.addr.byNode.node, 0, regHeader.src.addr.byNode.node.length);
        regHeader.src.socket=2;
        regHeader.transControl = 0;
        regHeader.pType = 0x0;

        byte[] buf = regHeader.toByteArray();

        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length, ipxServConnIp, udpPort);
            ipxClientSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pingSend() {
        IPXHeader regHeader = new IPXHeader();

        regHeader.checkSum = (short)0xFFFF;
        regHeader.dest.network = 0;
        regHeader.dest.addr.setHost(0xFFFFFFFF);
        regHeader.dest.addr.setPort(0xFFFF);
        regHeader.dest.socket = 2;
        regHeader.src.network = 0;
        System.arraycopy(localIpxAddr.netnode, 0, regHeader.src.addr.byNode.node, 0, regHeader.src.addr.byNode.node.length);
        regHeader.src.socket=2;
        regHeader.transControl = 0;
        regHeader.pType = 0x0;

        byte[] buf = regHeader.toByteArray();

        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length, ipxServConnIp, udpPort);
            ipxClientSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void receivePacket(/*Bit8u*/byte[] buffer, /*Bit16s*/int bufSize) {
        ECBClass useECB;
        ECBClass nextECB;
        IPXHeader tmpHeader = new IPXHeader();
        tmpHeader.load(buffer);
        /*Bit16u*/int useSocket = tmpHeader.dest.socket & 0xFFFF;

        // Check to see if ping packet
        if(useSocket == 0x2) {
            // Is this a broadcast?
            if((tmpHeader.dest.addr.host() == 0xffffffff) &&
                (tmpHeader.dest.addr.port() == 0xffff)) {
                // Yes.  We should return the ping back to the sender
                IPaddress tmpAddr=new IPaddress();
                tmpAddr.host = tmpHeader.src.addr.host();
                tmpAddr.port = tmpHeader.src.addr.port();
                pingAck(tmpAddr);
                return;
            }
        }

        useECB = ECBList;
        while(useECB != null)
        {
            nextECB = useECB.nextECB;
            if(useECB.iuflag == USEFLAG_LISTENING && useECB.mysocket == useSocket) {
                useECB.writeDataBuffer(buffer, bufSize);
                useECB.NotifyESR();
                return;
            }
            useECB = nextECB;
        }
        if (Config.IPX_DEBUGMSG)
            Log.log_msg("IPX: RX Packet loss!");
    }

    private static ReceiverThread receiverThread;
    static final class ReceiverThread extends Thread {
        boolean exit = false;
        Object signal = new Object();
        boolean ready = false;
        DatagramPacket receivePacket;
        byte[] tmpBuffer = new byte[IPXBUFFERSIZE];
        DatagramSocket socket;
        byte[] recvBuffer = new byte[IPXBUFFERSIZE];

        public ReceiverThread(DatagramSocket socket) {
            this.socket = socket;
        }
        public int next(IPXAddress address) {
            synchronized (signal) {
                if (ready) {
                    System.arraycopy(tmpBuffer, 0, recvBuffer, 0, IPXBUFFERSIZE);
                    ready = false;
                    int result = receivePacket.getLength();
                    if (address != null) {
                        address.address = receivePacket.getAddress();
                        address.port = receivePacket.getPort();
                    }
                    signal.notify();
                    return result;
                }
            }
            return 0;
        }
        public void run() {
            exit = false;
            while (!exit) {
                receivePacket = new DatagramPacket(tmpBuffer, IPXBUFFERSIZE);
                try {
                    socket.receive(receivePacket);
                    synchronized (signal) {
                        ready = true;
                        signal.wait();
                    }
                } catch (Exception e) {
                }
            }
        }
    }
    private static final Timer.TIMER_TickHandler IPX_ClientLoop = new Timer.TIMER_TickHandler() {
        public void call() {
            int length = receiverThread.next(null);
            if (length>0)
                receivePacket(receiverThread.recvBuffer, length);
        }
    };

    static private void DisconnectFromServer(boolean unexpected) {
        if(unexpected) Log.log_msg("IPX: Server disconnected unexpectedly");
        if(incomingPacket.connected) {
            incomingPacket.connected = false;
            Timer.TIMER_DelTickHandler(IPX_ClientLoop);
            ipxClientSocket.close();
        }
        receiverThread.exit = true;
        try {receiverThread.join();} catch (Exception e){}
    }

    private static void sendPacket(ECBClass sendecb) {
        /*Bit8u*/byte[] outbuffer = new byte[IPXBUFFERSIZE];
        fragmentDescriptor tmpFrag = new fragmentDescriptor();
        /*Bit16u*/int i, fragCount,t;
        /*Bit16s*/int packetsize;
        /*Bits*/int result;

        sendecb.setInUseFlag(USEFLAG_AVAILABLE);
        packetsize = 0;
        fragCount = sendecb.getFragCount();
        for(i=0;i<fragCount;i++) {
            sendecb.getFragDesc(i,tmpFrag);
            if(i==0) {
                // Fragment containing IPX header
                // Must put source address into header

                // source netnum
                for(/*Bit16u*/int m=0;m<4;m++) {
                    Memory.real_writeb(tmpFrag.segment,tmpFrag.offset+m+18,localIpxAddr.netnum[m]);
                }
                // source node number
                for(/*Bit16u*/int m=0;m<6;m++) {
                    Memory.real_writeb(tmpFrag.segment,tmpFrag.offset+m+22,localIpxAddr.netnode[m]);
                }
                // Source socket
                Memory.real_writew(tmpFrag.segment,tmpFrag.offset+28, swapByte(sendecb.getSocket()));

                // blank checksum
                Memory.real_writew(tmpFrag.segment,tmpFrag.offset, 0xffff);
            }

            for(t=0;t<tmpFrag.size;t++) {
                outbuffer[packetsize] = (byte)Memory.real_readb(tmpFrag.segment, tmpFrag.offset + t);
                packetsize++;
                if(packetsize>=IPXBUFFERSIZE) {
                    Log.log_msg("IPX: Packet size to be sent greater than "+IPXBUFFERSIZE+" bytes.");
                    sendecb.setCompletionFlag(COMP_UNDELIVERABLE);
                    sendecb.NotifyESR();
                    return;
                }
            }
        }

        // Add length and source socket to IPX header
        // Blank CRC
        //wordptr[0] = 0xffff;
        // Length
        outbuffer[3] = (byte)(packetsize & 0xFF);
        outbuffer[2] = (byte)((packetsize >> 8) & 0xFF);
        // Source socket
        //wordptr[14] = swapByte(sendecb.getSocket());

        sendecb.getFragDesc(0,tmpFrag);
        Memory.real_writew(tmpFrag.segment,tmpFrag.offset+2, swapByte(packetsize));


        /*Bit8u*/byte[] immedAddr = new byte[6];
        sendecb.getImmAddress(immedAddr);
        // filter out broadcasts and local loopbacks
        // Real implementation uses the ImmedAddr to check wether this is a broadcast

        boolean islocalbroadcast=true;
        boolean isloopback=true;

        for(/*Bitu*/int m=0;m<4;m++) {
            if(localIpxAddr.netnum[m]!=outbuffer[m+0x6])isloopback=false;
        }
        for(/*Bitu*/int m=0;m<6;m++) {
            if(localIpxAddr.netnode[m]!=outbuffer[m+0xa])isloopback=false;
            if(immedAddr[m]!=(byte)0xff) islocalbroadcast=false;
        }
        //LOG_IPX("SEND crc:%2x",packetCRC(&outbuffer[0], packetsize));
        if(!isloopback) {
            // Since we're using a channel, we won't send the IP address again
            DatagramPacket outPacket = new DatagramPacket(outbuffer, packetsize, ipxServConnIp, udpPort);
            try {
                ipxClientSocket.send(outPacket);
            } catch (Exception e) {
                e.printStackTrace();
                Log.log_msg("IPX: Could not send packet");
                sendecb.setCompletionFlag(COMP_HARDWAREERROR);
                sendecb.NotifyESR();
                DisconnectFromServer(true);
                return;
            }
            sendecb.setCompletionFlag(COMP_SUCCESS);
            if (Config.IPX_DEBUGMSG)
                Log.log_msg("Packet sent: size: "+packetsize);
        } else {
            sendecb.setCompletionFlag(COMP_SUCCESS);
        }

        if(isloopback||islocalbroadcast) {
            // Send packet back to ourselves.
            receivePacket(outbuffer,packetsize);
            if (Config.IPX_DEBUGMSG)
                Log.log_msg("Packet back: loopback:"+isloopback+", broadcast:"+islocalbroadcast);
        }
        sendecb.NotifyESR();
    }

    private static boolean pingCheck(IPXHeader outHeader) {
        int length = receiverThread.next(null);
        if (length>0) {
            byte[] buffer = new byte[1024];
            System.arraycopy(receiverThread.recvBuffer, 0, buffer, 0, length);
            outHeader.load(buffer);
            return true;
        }
        return false;
    }

    private static boolean ConnectToServer(String strAddr) {
        try {
            ipxServConnIp = InetAddress.getByName(strAddr);
            ipxClientSocket = new DatagramSocket();
            receiverThread = new ReceiverThread(ipxClientSocket);
            receiverThread.start();
            IPXHeader regHeader = new IPXHeader();
            regHeader.checkSum = (short)0xFFFF;
            regHeader.dest.network = 0;
            regHeader.dest.addr.setHost(0);
            regHeader.dest.addr.setPort(0);
            regHeader.dest.socket = 0x2;
            regHeader.src.network = 0;
            regHeader.src.addr.setHost(0);
            regHeader.src.addr.setPort(0);
            regHeader.src.socket = 0x2;
            regHeader.transControl = 0;
            byte[] outbuffer = regHeader.toByteArray();
            DatagramPacket outPacket = new DatagramPacket(outbuffer, outbuffer.length, ipxServConnIp, udpPort);
            try {
                ipxClientSocket.send(outPacket);
            } catch (Exception e) {
                e.printStackTrace();
                Log.log_msg("IPX: Unable to connect to server");
                try {ipxClientSocket.close();} catch (Exception e1) {}
                return false;
            }

            // Wait for return packet from server.
            // This will contain our IPX address and port num
            /*Bits*/int result;
            /*Bit32u*/long ticks, elapsed;
            ticks = Main.GetTicks();

            while(true) {
                elapsed = Main.GetTicks() - ticks;
                if(elapsed > 5000) {
                    Log.log_msg("Timeout connecting to server at "+strAddr);
                    try {ipxClientSocket.close();} catch (Exception e) {}
                    return false;
                }
                Callback.CALLBACK_Idle();

                int length = receiverThread.next(null);
                if (length>0) {
                    regHeader.load(receiverThread.recvBuffer);
                    System.arraycopy(regHeader.dest.addr.byNode.node, 0, localIpxAddr.netnode, 0, localIpxAddr.netnode.length);
                    localIpxAddr.netnum(regHeader.dest.network);
                    break;
                }
            }

            Log.log_msg(StringHelper.sprintf("IPX: Connected to server.  IPX address is %d:%d:%d:%d:%d:%d", new Object[]{new Integer(localIpxAddr.netnode[0] & 0xFF), new Integer(localIpxAddr.netnode[1] & 0xFF), new Integer(localIpxAddr.netnode[2] & 0xFF), new Integer(localIpxAddr.netnode[3] & 0xFF), new Integer(localIpxAddr.netnode[4] & 0xFF), new Integer(localIpxAddr.netnode[5] & 0xFF) } ));

            incomingPacket.connected = true;
            Timer.TIMER_AddTickHandler(IPX_ClientLoop);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static private void IPX_NetworkInit() {

        localIpxAddr.netnum[0] = 0x0;
        localIpxAddr.netnum[1] = 0x0;
        localIpxAddr.netnum[2] = 0x0;
        localIpxAddr.netnum[3] = 0x1;
        localIpxAddr.netnode[0] = 0x00;
        localIpxAddr.netnode[1] = 0x00;
        localIpxAddr.netnode[2] = 0x00;
        localIpxAddr.netnode[3] = 0x00;
        localIpxAddr.netnode[4] = 0x00;
        localIpxAddr.netnode[5] = 0x00;

        socketCount = 0;
        return;
    }

    private static class IPXNET extends Program {
        void HelpCommand(String helpStr) {
            // Help on connect command
            if("connect".equals(helpStr)) {
                WriteOut("IPXNET CONNECT opens a connection to an IPX tunneling server running on another\n");
                WriteOut("DosBox session.  The \"address\" parameter specifies the IP address or host name\n");
                WriteOut("of the server computer.  One can also specify the UDP port to use.  By default\n");
                WriteOut("IPXNET uses port 213, the assigned IANA port for IPX tunneling, for its\nconnection.\n\n");
                WriteOut("The syntax for IPXNET CONNECT is:\n\n");
                WriteOut("IPXNET CONNECT address <port>\n\n");
                return;
            }
            // Help on the disconnect command
            if("disconnect".equals(helpStr)) {
                WriteOut("IPXNET DISCONNECT closes the connection to the IPX tunneling server.\n\n");
                WriteOut("The syntax for IPXNET DISCONNECT is:\n\n");
                WriteOut("IPXNET DISCONNECT\n\n");
                return;
            }
            // Help on the startserver command
            if("startserver".equals(helpStr)) {
                WriteOut("IPXNET STARTSERVER starts and IPX tunneling server on this DosBox session.  By\n");
                WriteOut("default, the server will accept connections on UDP port 213, though this can be\n");
                WriteOut("changed.  Once the server is started, DosBox will automatically start a client\n");
                WriteOut("connection to the IPX tunneling server.\n\n");
                WriteOut("The syntax for IPXNET STARTSERVER is:\n\n");
                WriteOut("IPXNET STARTSERVER <port>\n\n");
                return;
            }
            // Help on the stop server command
            if("stopserver".equals(helpStr)) {
                WriteOut("IPXNET STOPSERVER stops the IPX tunneling server running on this DosBox\nsession.");
                WriteOut("  Care should be taken to ensure that all other connections have\nterminated ");
                WriteOut("as well sinnce stoping the server may cause lockups on other\nmachines still using ");
                WriteOut("the IPX tunneling server.\n\n");
                WriteOut("The syntax for IPXNET STOPSERVER is:\n\n");
                WriteOut("IPXNET STOPSERVER\n\n");
                return;
            }
            // Help on the ping command
            if("ping".equals(helpStr)) {
                WriteOut("IPXNET PING broadcasts a ping request through the IPX tunneled network.  In    \n");
                WriteOut("response, all other connected computers will respond to the ping and report\n");
                WriteOut("the time it took to receive and send the ping message.\n\n");
                WriteOut("The syntax for IPXNET PING is:\n\n");
                WriteOut("IPXNET PING\n\n");
                return;
            }
            // Help on the status command
            if("status".equals(helpStr)) {
                WriteOut("IPXNET STATUS reports the current state of this DosBox's sessions IPX tunneling\n");
                WriteOut("network.  For a list of the computers connected to the network use the IPXNET \n");
                WriteOut("PING command.\n\n");
                WriteOut("The syntax for IPXNET STATUS is:\n\n");
                WriteOut("IPXNET STATUS\n\n");
                return;
            }
        }

        public void Run()
        {
            WriteOut("IPX Tunneling utility for DosBox\n\n");
            if(cmd.GetCount()==0) {
                WriteOut("The syntax of this command is:\n\n");
                WriteOut("IPXNET [ CONNECT | DISCONNECT | STARTSERVER | STOPSERVER | PING | HELP |\n         STATUS ]\n\n");
                return;
            }

            if((temp_line=cmd.FindCommand(1))!=null) {
                temp_line = temp_line.toLowerCase();
                if("help".equals(temp_line)) {
                    if((temp_line=cmd.FindCommand(2))==null) {
                        WriteOut("The following are valid IPXNET commands:\n\n");
                        WriteOut("IPXNET CONNECT        IPXNET DISCONNECT       IPXNET STARTSERVER\n");
                        WriteOut("IPXNET STOPSERVER     IPXNET PING             IPXNET STATUS\n\n");
                        WriteOut("To get help on a specific command, type:\n\n");
                        WriteOut("IPXNET HELP command\n\n");

                    } else {
                        HelpCommand(temp_line);
                        return;
                    }
                    return;
                }
                if("startserver".equals(temp_line)) {
                    if(!isIpxServer) {
                        if(incomingPacket.connected) {
                            WriteOut("IPX Tunneling Client already connected to another server.  Disconnect first.\n");
                            return;
                        }
                        boolean startsuccess;
                        udpPort = 213;
                        if((temp_line=cmd.FindCommand(2))!=null) {
                            try {
                                udpPort = Integer.parseInt(temp_line);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        startsuccess = IPXServer.IPX_StartServer(udpPort);
                        if(startsuccess) {
                            WriteOut("IPX Tunneling Server started\n");
                            isIpxServer = true;
                            ConnectToServer("localhost");
                        } else {
                            WriteOut("IPX Tunneling Server failed to start.\n");
                            if(udpPort < 1024) WriteOut("Try a port number above 1024. See IPXNET HELP CONNECT on how to specify a port.\n");
                        }
                    } else {
                        WriteOut("IPX Tunneling Server already started\n");
                    }
                    return;
                }
                if("stopserver".equals(temp_line)) {
                    if(!isIpxServer) {
                        WriteOut("IPX Tunneling Server not running in this DosBox session.\n");
                    } else {
                        isIpxServer = false;
                        DisconnectFromServer(false);
                        IPXServer.IPX_StopServer();
                        WriteOut("IPX Tunneling Server stopped.");
                    }
                    return;
                }
                if("connect".equals(temp_line)) {
                    String strHost;
                    if(incomingPacket.connected) {
                        WriteOut("IPX Tunneling Client already connected.\n");
                        return;
                    }
                    if((temp_line=cmd.FindCommand(2))==null) {
                        WriteOut("IPX Server address not specified.\n");
                        return;
                    }
                    strHost = temp_line;

                    udpPort = 213;
                    if((temp_line=cmd.FindCommand(3))!=null) {
                        try {
                            udpPort = Integer.parseInt(temp_line);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if(ConnectToServer(strHost)) {
                        WriteOut("IPX Tunneling Client connected to server at "+strHost+".\n");
                    } else {
                        WriteOut("IPX Tunneling Client failed to connect to server at "+strHost+".\n");
                    }
                    return;
                }

                if("disconnect".equals(temp_line)) {
                    if(!incomingPacket.connected) {
                        WriteOut("IPX Tunneling Client not connected.\n");
                        return;
                    }
                    // TODO: Send a packet to the server notifying of disconnect
                    WriteOut("IPX Tunneling Client disconnected from server.\n");
                    DisconnectFromServer(false);
                    return;
                }

                if("status".equals(temp_line)) {
                    WriteOut("IPX Tunneling Status:\n\n");
                    WriteOut("Server status: ");
                    if(isIpxServer) WriteOut("ACTIVE\n"); else WriteOut("INACTIVE\n");
                    WriteOut("Client status: ");
                    if(incomingPacket.connected) {
                        WriteOut("CONNECTED -- Server at "+ipxServConnIp.getHostAddress()+" port "+udpPort+"\n");
                    } else {
                        WriteOut("DISCONNECTED\n");
                    }
                    if(isIpxServer) {
                        // :TODO:
                        WriteOut("List of active connections:\n\n");
                        int i;
                        for(i=0;i<SOCKETTABLESIZE;i++) {
                            IPXAddress addr = IPXServer.IPX_isConnectedToServer(i);
                            if(addr != null) {
                                WriteOut("     "+addr.address.getHostAddress()+" from port "+addr.port+"\n");
                            }
                        }
                        WriteOut("\n");
                    }
                    return;
                }

                if("ping".equals(temp_line)) {
                    /*Bit32u*/long ticks;
                    IPXHeader pingHead = new IPXHeader();

                    if(!incomingPacket.connected) {
                        WriteOut("IPX Tunneling Client not connected.\n");
                        return;
                    }
                    Timer.TIMER_DelTickHandler(IPX_ClientLoop);
                    WriteOut("Sending broadcast ping:\n\n");
                    pingSend();
                    ticks = Main.GetTicks();
                    while((Main.GetTicks() - ticks) < 1500) {
                        Callback.CALLBACK_Idle();
                        if(pingCheck(pingHead)) {
                            WriteOut("Response from "+pingHead.src.addr.hostAsString()+", port "+pingHead.src.addr.port()+" time="+String.valueOf(Main.GetTicks() - ticks)+"ms\n");
                        }
                    }
                    Timer.TIMER_AddTickHandler(IPX_ClientLoop);
                    return;
                }
            }
        }
    }

    static private Program.PROGRAMS_Main IPXNET_ProgramStart = new Program.PROGRAMS_Main() {
        public Program call() {
            return new IPXNET();
        }
    };

    private static final Callback.Handler IPX_ESRHandler = new Callback.Handler() {
        public /*Bitu*/int call() {
            if (Config.IPX_DEBUGMSG)
                Log.log_msg("ESR: >>>>>>>>>>>>>>>" );
            while(ESRList!=null) {
                // LOG_IPX("ECB: SN%7d notified.", ESRList.SerialNumber);
                if(ESRList.databuffer!=null) ESRList.writeData();
                if(ESRList.getESRAddr()!=0) {
                    // setup registers
                    CPU_Regs.SegSet16ES(Memory.RealSeg(ESRList.ECBAddr));
                    CPU_Regs.reg_esi.word(Memory.RealOff(ESRList.ECBAddr));
                    CPU_Regs.reg_eax.low(0xff);
                    Callback.CALLBACK_RunRealFar(Memory.RealSeg(ESRList.getESRAddr()),Memory.RealOff(ESRList.getESRAddr()));
                }
                ESRList.close();
            }	// while

            IO.IO_WriteB(0xa0,0x63);	//EOI11
            IO.IO_WriteB(0x20,0x62);	//EOI2
            if (Config.IPX_DEBUGMSG)
                Log.log_msg("ESR: <<<<<<<<<<<<<<<");
            return Callback.CBRET_NONE;
        }

        public String getName() {
            return "IPX ESR";
        }
    };


    private Callback callback_ipx = new Callback();
    private Callback callback_esr = new Callback();
    private Callback callback_ipxint = new Callback();
    private /*RealPt*/IntRef old_73_vector = new IntRef(0);
    private static /*Bit16u*/int dospage;

    private IPX(Section configuration) {
        super(configuration);
        Section_prop section=(Section_prop)configuration;
        if(!section.Get_bool("ipx")) return;

        ECBList = null;
        ESRList = null;
        isIpxServer = false;
        isIpxConnected = false;
        IPX_NetworkInit();

        Dos_misc.DOS_AddMultiplexHandler(IPX_Multiplex);

        callback_ipx.Install(IPX_Handler,Callback.CB_RETF,"IPX Handler");
        ipx_callback = callback_ipx.Get_RealPointer();

        callback_ipxint.Install(IPX_IntHandler,Callback.CB_IRET,"IPX (int 7a)");
        callback_ipxint.Set_RealVec(0x7a);

        callback_esr.Allocate(IPX_ESRHandler,"IPX_ESR");
        /*Bit16u*/int call_ipxesr1 = callback_esr.Get_callback();

        if(dospage==0) dospage = Dos_tables.DOS_GetMemory(2); // can not be freed yet

        /*PhysPt*/int phyDospage = Memory.PhysMake(dospage,0);

        if (Config.IPX_DEBUGMSG)
            Log.log_msg("ESR callback address: "+Long.toString(phyDospage, 16)+", HandlerID "+call_ipxesr1);

        //save registers
        Memory.phys_writeb(phyDospage+0,0xFA);    // CLI
        Memory.phys_writeb(phyDospage+1,0x60);    // PUSHA
        Memory.phys_writeb(phyDospage+2,0x1E);    // PUSH DS
        Memory.phys_writeb(phyDospage+3,0x06);    // PUSH ES
        Memory.phys_writew(phyDospage+4,0xA00F); // PUSH FS
        Memory.phys_writew(phyDospage+6,0xA80F); // PUSH GS

        // callback
        Memory.phys_writeb(phyDospage+8,0xFE);  // GRP 4
        Memory.phys_writeb(phyDospage+9,0x38);  // Extra Callback instruction
        Memory.phys_writew(phyDospage+10,call_ipxesr1);        // Callback identifier

        // register recreation
        Memory.phys_writew(phyDospage+12,0xA90F); // POP GS
        Memory.phys_writew(phyDospage+14,0xA10F); // POP FS
        Memory.phys_writeb(phyDospage+16,0x07);    // POP ES
        Memory.phys_writeb(phyDospage+17,0x1F);    // POP DS
        Memory.phys_writeb(phyDospage+18,0x61);    // POPA
        Memory.phys_writeb(phyDospage+19,0xCF);    // IRET: restores flags, CS, IP

        // IPX version 2.12
        //phys_writeb(phyDospage+27,(Bit8u)0x2);
        //phys_writeb(phyDospage+28,(Bit8u)0x12);
        //IPXVERpointer = RealMake(dospage,27);

        /*RealPt*/int ESRRoutineBase = Memory.RealMake(dospage, 0);

        // Interrupt enabling
        Memory.RealSetVec(0x73,ESRRoutineBase,old_73_vector);	// IRQ11
        IO.IO_WriteB(0xa1,IO.IO_ReadB(0xa1)&(~8));			// enable IRQ11

        Program.PROGRAMS_MakeFile("IPXNET.COM",IPXNET_ProgramStart);
    }

    private void close() {
        Section_prop section=(Section_prop)m_configuration;
        Pic.PIC_RemoveEvents(IPX_AES_EventHandler);
        if(!section.Get_bool("ipx")) return;

        if(isIpxServer) {
            isIpxServer = false;
            IPXServer.IPX_StopServer();
        }
        DisconnectFromServer(false);

        Dos_misc.DOS_DelMultiplexHandler(IPX_Multiplex);
        Memory.RealSetVec(0x73, old_73_vector.value);
        IO.IO_WriteB(0xa1, IO.IO_ReadB(0xa1) | 8);	// disable IRQ11

        /*PhysPt*/int phyDospage = Memory.PhysMake(dospage, 0);
        for(/*Bitu*/int i = 0;i < 32;i++)
            Memory.phys_writeb(phyDospage + i, 0x00);

        Drive_virtual.VFILE_Remove("IPXNET.COM");
    }

    private static IPX test;

    public static Section.SectionFunction IPX_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test.close();
        }
    };

    public static Section.SectionFunction IPX_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new IPX(section);
            section.AddDestroyFunction(IPX_ShutDown,true);
        }
    };
}
