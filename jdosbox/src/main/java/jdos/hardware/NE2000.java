package jdos.hardware;


/* Couldn't find a real spec for the NE2000 out there, hence this is adapted heavily from Bochs */

/////////////////////////////////////////////////////////////////////////
// $Id: ne2k.cc,v 1.56.2.1 2004/02/02 22:37:22 cbothamy Exp $
/////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2002  MandrakeSoft S.A.
//
//    MandrakeSoft S.A.
//    43, rue d'Aboukir
//    75002 Paris - France
//    http://www.linux-mandrake.com/
//    http://www.mandrakesoft.com/
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

import jdos.host.Ethernet;
import jdos.host.RxFrame;
import jdos.host.UserEthernet;
import jdos.misc.Log;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.Ptr;
import jdos.util.StringHelper;

// Peter Grehan (grehan@iprg.nokia.com) coded all of this
// NE2000/ether stuff.
public class NE2000 extends Module_base {
    //#define /*bx_bool*/int int
    //#define bx_param_c Bit8u


    //Never completely fill the ne2k ring so that we never
    // hit the unclear completely full buffer condition.
    static private final int BX_NE2K_NEVER_FULL_RING = 1;

    static final public boolean BX_DEBUG = false;
    static final public boolean BX_INFO = true;
    static final public boolean BX_PANIC = true;
    static final public boolean BX_ERROR = true;

    static private final int BX_RESET_HARDWARE = 0;
    static private final int BX_RESET_SOFTWARE = 1;

    static private bx_ne2k_c theNE2kDevice = null;

    //#  define BX_NE2K_SMF

    static private final int BX_NE2K_MEMSIZ = 32 * 1024;
    static private final int BX_NE2K_MEMSTART = 16 * 1024;
    static private final int BX_NE2K_MEMEND = (BX_NE2K_MEMSTART + BX_NE2K_MEMSIZ);

    private static final class bx_ne2k_t {
        //
        // ne2k register state

        //
        // Page 0
        //
        //  Command Register - 00h read/write
        public static final class CR_t {
            public int stop;        // STP - Software Reset command
            public int start;        // START - start the NIC
            public int tx_packet;    // TXP - initiate packet transmission
            public /*Bit8u*/ short rdma_cmd;      // RD0,RD1,RD2 - Remote DMA command
            public /*Bit8u*/ short pgsel;        // PS0,PS1 - Page select
        }

        public CR_t CR = new CR_t();

        // Interrupt Status Register - 07h read/write
        public static final class ISR_t {
            public int pkt_rx;           // PRX - packet received with no errors
            public int pkt_tx;           // PTX - packet transmitted with no errors
            public int rx_err;    // RXE - packet received with 1 or more errors
            public int tx_err;    // TXE - packet tx'd       "  " "    "    "
            public int overwrite;    // OVW - rx buffer resources exhausted
            public int cnt_oflow;     // CNT - network tally counter MSB's set
            public int rdma_done;     // RDC - remote DMA complete
            public int reset;        // RST - reset status
        }

        public ISR_t ISR = new ISR_t();

        // Interrupt Mask Register - 0fh write
        public static final class IMR_t {
            public int rx_inte;    // PRXE - packet rx interrupt enable
            public int tx_inte;    // PTXE - packet tx interrput enable
            public int rxerr_inte;    // RXEE - rx error interrupt enable
            public int txerr_inte;    // TXEE - tx error interrupt enable
            public int overw_inte;    // OVWE - overwrite warn int enable
            public int cofl_inte;    // CNTE - counter o'flow int enable
            public int rdma_inte;    // RDCE - remote DMA complete int enable
            public int reserved;    //  D7 - reserved
        }

        public IMR_t IMR = new IMR_t();

        // Data Configuration Register - 0eh write
        public static final class DCR_t {
            public int wdsize;    // WTS - 8/16-bit select
            public int endian;    // BOS - byte-order select
            public int longaddr;    // LAS - long-address select
            public int loop;        // LS  - loopback select
            public int auto_rx;    // AR  - auto-remove rx packets with remote DMA
            public /*Bit8u*/ short fifo_size;    // FT0,FT1 - fifo threshold
        }

        public DCR_t DCR = new DCR_t();

        // Transmit Configuration Register - 0dh write
        public static final class TCR_t {
            public int crc_disable;    // CRC - inhibit tx CRC
            public /*Bit8u*/ short loop_cntl;    // LB0,LB1 - loopback control
            public int ext_stoptx;    // ATD - allow tx disable by external mcast
            public int coll_prio;    // OFST - backoff algorithm select
            public /*Bit8u*/ short reserved;      //  D5,D6,D7 - reserved
        }

        public TCR_t TCR = new TCR_t();

        // Transmit Status Register - 04h read
        public static final class TSR_t {
            public int tx_ok;        // PTX - tx complete without error
            public int reserved;    //  D1 - reserved
            public int collided;    // COL - tx collided >= 1 times
            public int aborted;    // ABT - aborted due to excessive collisions
            public int no_carrier;    // CRS - carrier-sense lost
            public int fifo_ur;    // FU  - FIFO underrun
            public int cd_hbeat;    // CDH - no tx cd-heartbeat from transceiver
            public int ow_coll;    // OWC - out-of-window collision
        }

        public TSR_t TSR = new TSR_t();

        // Receive Configuration Register - 0ch write
        public static final class RCR_t {
            public int errors_ok;    // SEP - accept pkts with rx errors
            public int runts_ok;    // AR  - accept < 64-byte runts
            public int broadcast;    // AB  - accept eth broadcast address
            public int multicast;    // AM  - check mcast hash array
            public int promisc;    // PRO - accept all packets
            public int monitor;    // MON - check pkts, but don't rx
            public /*Bit8u*/ short reserved;    //  D6,D7 - reserved
        }

        public final RCR_t RCR = new RCR_t();

        // Receive Status Register - 0ch read
        public static final class RSR_t {
            public int rx_ok;        // PRX - rx complete without error
            public int bad_crc;    // CRC - Bad CRC detected
            public int bad_falign;    // FAE - frame alignment error
            public int fifo_or;    // FO  - FIFO overrun
            public int rx_missed;    // MPA - missed packet error
            public int rx_mbit;    // PHY - unicast or mcast/bcast address match
            public int rx_disabled;   // DIS - set when in monitor mode
            public int deferred;    // DFR - collision active
        }

        public RSR_t RSR = new RSR_t();

        public /*Bit16u*/ int local_dma;    // 01,02h read ; current local DMA addr
        public /*Bit8u*/ short page_start;  // 01h write ; page start register
        public /*Bit8u*/ short page_stop;   // 02h write ; page stop register
        public /*Bit8u*/ short bound_ptr;   // 03h read/write ; boundary pointer
        public /*Bit8u*/ short tx_page_start; // 04h write ; transmit page start register
        public /*Bit8u*/ short num_coll;    // 05h read  ; number-of-collisions register
        public /*Bit16u*/ int tx_bytes;    // 05,06h write ; transmit byte-count register
        public /*Bit8u*/ short fifo;    // 06h read  ; FIFO
        public /*Bit16u*/ int remote_dma;  // 08,09h read ; current remote DMA addr
        public /*Bit16u*/ int remote_start;  // 08,09h write ; remote start address register
        public /*Bit16u*/ int remote_bytes;  // 0a,0bh write ; remote byte-count register
        public /*Bit8u*/ short tallycnt_0;  // 0dh read  ; tally counter 0 (frame align errors)
        public /*Bit8u*/ short tallycnt_1;  // 0eh read  ; tally counter 1 (CRC errors)
        public /*Bit8u*/ short tallycnt_2;  // 0fh read  ; tally counter 2 (missed pkt errors)

        //
        // Page 1
        //
        //   Command Register 00h (repeated)
        //
        public /*Bit8u*/ byte[] physaddr = new byte[6];  // 01-06h read/write ; MAC address
        public /*Bit8u*/ short curr_page;    // 07h read/write ; current page register
        public /*Bit8u*/ byte[] mchash = new byte[8];    // 08-0fh read/write ; multicast hash array

        //
        // Page 2  - diagnostic use only
        //
        //   Command Register 00h (repeated)
        //
        //   Page Start Register 01h read  (repeated)
        //   Page Stop Register  02h read  (repeated)
        //   Current Local DMA Address 01,02h write (repeated)
        //   Transmit Page start address 04h read (repeated)
        //   Receive Configuration Register 0ch read (repeated)
        //   Transmit Configuration Register 0dh read (repeated)
        //   Data Configuration Register 0eh read (repeated)
        //   Interrupt Mask Register 0fh read (repeated)
        //
        public /*Bit8u*/ short rempkt_ptr;   // 03h read/write ; remote next-packet pointer
        public /*Bit8u*/ short localpkt_ptr; // 05h read/write ; local next-packet pointer
        public /*Bit16u*/ int address_cnt;  // 06,07h read/write ; address counter

        //
        // Page 3  - should never be modified.
        //

        // Novell ASIC state
        public /*Bit8u*/ byte[] macaddr = new byte[32];          // ASIC ROM'd MAC address, even bytes
        public /*Bit8u*/ Ptr mem = new Ptr(BX_NE2K_MEMSIZ);  // on-chip packet memory

        // ne2k internal state
        public /*Bit32u*/ long base_address;
        public int base_irq;
        public int tx_timer_index;
        public int tx_timer_active;
    }

    public static final class bx_ne2k_c implements RxFrame {
        bx_ne2k_t s = new bx_ne2k_t();

        public bx_ne2k_c() {
            s.tx_timer_index = 0;
        }

        public void init() {
            if (BX_INFO)
                Log.log_msg(StringHelper.sprintf("[NE2000] port 0x%x/32 irq %d mac %02x:%02x:%02x:%02x:%02x:%02x", new Object[]{
                        new Long(s.base_address),
                        new Integer(s.base_irq),
                        new Integer(s.physaddr[0]),
                        new Integer(s.physaddr[1]),
                        new Integer(s.physaddr[2]),
                        new Integer(s.physaddr[3]),
                        new Integer(s.physaddr[4]),
                        new Integer(s.physaddr[5])}));

            // Initialise the mac address area by doubling the physical address
            s.macaddr[0] = s.physaddr[0];
            s.macaddr[1] = s.physaddr[0];
            s.macaddr[2] = s.physaddr[1];
            s.macaddr[3] = s.physaddr[1];
            s.macaddr[4] = s.physaddr[2];
            s.macaddr[5] = s.physaddr[2];
            s.macaddr[6] = s.physaddr[3];
            s.macaddr[7] = s.physaddr[3];
            s.macaddr[8] = s.physaddr[4];
            s.macaddr[9] = s.physaddr[4];
            s.macaddr[10] = s.physaddr[5];
            s.macaddr[11] = s.physaddr[5];

            // ne2k signature
            for (/*Bitu*/int i = 12; i < 32; i++)
                s.macaddr[i] = 0x57;

            // Bring the register state into power-up state
            reset(BX_RESET_HARDWARE);
        }

        public void reset(int type) {
            if (BX_DEBUG) Log.log_msg("[NE2000] reset");
            // Zero out registers and memory
            s.CR = new bx_ne2k_t.CR_t();
            s.ISR = new bx_ne2k_t.ISR_t();
            s.IMR = new bx_ne2k_t.IMR_t();
            s.DCR = new bx_ne2k_t.DCR_t();
            s.TCR = new bx_ne2k_t.TCR_t();
            s.TSR = new bx_ne2k_t.TSR_t();
            //s.RCR = new bx_ne2k_t.RCR_t();
            s.RSR = new bx_ne2k_t.RSR_t();

            s.tx_timer_active = 0;
            s.local_dma = 0;
            s.page_start = 0;
            s.page_stop = 0;
            s.bound_ptr = 0;
            s.tx_page_start = 0;
            s.num_coll = 0;
            s.tx_bytes = 0;
            s.fifo = 0;
            s.remote_dma = 0;
            s.remote_start = 0;
            s.remote_bytes = 0;
            s.tallycnt_0 = 0;
            s.tallycnt_1 = 0;
            s.tallycnt_2 = 0;

            //memset( & s.physaddr, 0, sizeof(s.physaddr));
            //memset( & s.mchash, 0, sizeof(s.mchash));
            s.curr_page = 0;

            s.rempkt_ptr = 0;
            s.localpkt_ptr = 0;
            s.address_cnt = 0;

            s.mem.clear();

            // Set power-up conditions
            s.CR.stop = 1;
            s.CR.rdma_cmd = 4;
            s.ISR.reset = 1;
            s.DCR.longaddr = 1;
            Pic.PIC_DeActivateIRQ(s.base_irq);
            //DEV_pic_lower_irq(s.base_irq);
        }

        //
        // read_cr/write_cr - utility routines for handling reads/writes to
        // the Command Register
        //
        public /*Bit32u*/int read_cr() {
            /*Bit32u*/
            int val = (((s.CR.pgsel & 0x03) << 6) | ((s.CR.rdma_cmd & 0x07) << 3) | (s.CR.tx_packet << 2) | (s.CR.start << 1) | (s.CR.stop));
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] read CR returns 0x%08x", new Object[]{new Integer(val)}));
            return val;
        }

        public void write_cr(/*Bit32u*/int value) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] wrote 0x%02x to CR", new Object[]{new Integer(value)}));

            // Validate remote-DMA
            if ((value & 0x38) == 0x00) {
                if (BX_DEBUG)
                    Log.log_msg("[NE2000] CR write - invalid rDMA value 0");
                value |= 0x20; /* dma_cmd == 4 is a safe default */
                //value = 0x22; /* dma_cmd == 4 is a safe default */
            }

            // Check for s/w reset
            if ((value & 0x01) != 0) {
                s.ISR.reset = 1;
                s.CR.stop = 1;
            } else {
                s.CR.stop = 0;
            }

            s.CR.rdma_cmd = (short) ((value & 0x38) >> 3);

            // If start command issued, the RST bit in the ISR
            // must be cleared
            if ((value & 0x02) != 0 && s.CR.start == 0) {
                s.ISR.reset = 0;
            }

            s.CR.start = ((value & 0x02) == 0x02) ? 1 : 0;
            s.CR.pgsel = (short) ((value & 0xc0) >> 6);

            // Check for send-packet command
            if (s.CR.rdma_cmd == 3) {
                // Set up DMA read from receive ring
                s.remote_start = s.remote_dma =
                        s.bound_ptr * 256;
                s.remote_bytes = s.mem.readw(s.bound_ptr * 256 + 2 - BX_NE2K_MEMSTART);
                if (BX_INFO)
                    Log.log_msg("[NE2000] Sending buffer #x" + Integer.toString(s.remote_start, 16) + " length " + s.remote_bytes);
            }

            // Check for start-tx
            if ((value & 0x04) != 0 && s.TCR.loop_cntl != 0) {
                // loopback mode
                if (s.TCR.loop_cntl != 1) {
                    if (BX_INFO)
                        Log.log_msg("[NE2000] Loop mode " + s.TCR.loop_cntl + " not supported.");
                } else {
                    rx_frame(new Ptr(s.mem, s.tx_page_start * 256 - BX_NE2K_MEMSTART), s.tx_bytes);

                    // do a TX interrupt
                    // Generate an interrupt if not masked and not one in progress
                    if (s.IMR.tx_inte != 0 && s.ISR.pkt_tx == 0) {
                        //LOG_MSG("tx complete interrupt");
                        Pic.PIC_ActivateIRQ(s.base_irq);
                    }
                    s.ISR.pkt_tx = 1;
                }
            } else if ((value & 0x04) != 0) {
                // start-tx and no loopback
                if (s.CR.stop != 0 || s.CR.start == 0)
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] CR write - tx start, dev in reset");

                if (s.tx_bytes == 0)
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] CR write - tx start, tx bytes == 0");

//                #ifdef notdef
//                // XXX debug stuff
//                printf("packet tx (%d bytes):\t", s.tx_bytes);
//                for (int i = 0; i < s.tx_bytes; i++) {
//                    printf("%02x ", s.mem[s.tx_page_start * 256 -
//                            BX_NE2K_MEMSTART + i]);
//                    if (i && (((i + 1) % 16) == 0))
//                        printf("\t");
//                }
//                printf("");
//                #endif

                // Send the packet to the system driver
                /* TODO: Transmit packet */
                //ethdev->sendpkt(& s.mem[s.tx_page_start*256 - BX_NE2K_MEMSTART], s.tx_bytes);
                test.ethernet.send(s.mem.p, s.tx_page_start * 256 - BX_NE2K_MEMSTART, s.tx_bytes);
                // some more debug
                if (s.tx_timer_active != 0) {
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] CR write, tx timer still active");
                    Pic.PIC_RemoveEvents(NE2000_TX_Event);
                }
                //LOG_MSG("send packet command");
                //s.tx_timer_index = (64 + 96 + 4*8 + s.tx_bytes*8)/10;
                s.tx_timer_active = 1;
                Pic.PIC_AddEvent(NE2000_TX_Event, (float) ((64 + 96 + 4 * 8 + s.tx_bytes * 8) / 10000.0), 0);
                // Schedule a timer to trigger a tx-complete interrupt
                // The number of microseconds is the bit-time / 10.
                // The bit-time is the preamble+sfd (64 bits), the
                // inter-frame gap (96 bits), the CRC (4 bytes), and the
                // the number of bits in the frame (s.tx_bytes * 8).
                //

                /* TODO: Code transmit timer */
                /*
                bx_pc_system.activate_timer(s.tx_timer_index,
                            (64 + 96 + 4*8 + s.tx_bytes*8)/10,
                            0); // not continuous
                */
            } // end transmit-start branch

            // Linux probes for an interrupt by setting up a remote-DMA read
            // of 0 bytes with remote-DMA completion interrupts enabled.
            // Detect this here
            if (s.CR.rdma_cmd == 0x01 && s.CR.start != 0 && s.remote_bytes == 0) {
                s.ISR.rdma_done = 1;
                if (s.IMR.rdma_inte != 0) {
                    Pic.PIC_ActivateIRQ(s.base_irq);
                    //DEV_pic_raise_irq(s.base_irq);
                }
            }
        }

        //
        // chipmem_read/chipmem_write - access the 64K private RAM.
        // The ne2000 memory is accessed through the data port of
        // the asic (offset 0) after setting up a remote-DMA transfer.
        // Both byte and word accesses are allowed.
        // The first 16 bytes contains the MAC address at even locations,
        // and there is 16K of buffer memory starting at 16K
        //
        public /*Bit32u*/long chipmem_read(/*Bit32u*/int address, /*unsigned*/int io_len) {
            /*Bit32u*/
            long retval = 0;

            if ((io_len == 2) && (address & 0x1) != 0)
                if (BX_PANIC)
                    Log.log_msg("[NE2000] unaligned chipmem word read");

            // ROM'd MAC address
            if ((address >= 0) && (address <= 31)) {
                retval = s.macaddr[address];
                if ((io_len == 2) || (io_len == 4)) {
                    retval |= (s.macaddr[address + 1] << 8);
                    if (io_len == 4) {
                        retval |= (s.macaddr[address + 2] << 16);
                        retval |= (s.macaddr[address + 3] << 24);
                    }
                }
                return (retval);
            }
            if ((address >= BX_NE2K_MEMSTART) && (address < BX_NE2K_MEMEND)) {
                if (io_len == 1) {
                    return s.mem.readb(address - BX_NE2K_MEMSTART);
                } else if (io_len == 2) {
                    return s.mem.readw(address - BX_NE2K_MEMSTART);
                } else if (io_len == 4) {
                    return s.mem.readd(address - BX_NE2K_MEMSTART);
                }
            }
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("out-of-bounds chipmem read, %04X", new Object[]{new Integer(address)}));

            return (0xff);
        }

        public void chipmem_write(/*Bit32u*/int address, /*Bit32u*/long value, /*unsigned*/int io_len) {
            if ((io_len == 2) && (address & 0x1) != 0)
                if (BX_PANIC)
                    Log.log_msg("[NE2000] unaligned chipmem word write");

            if ((address >= BX_NE2K_MEMSTART) && (address < BX_NE2K_MEMEND)) {
                if (io_len == 1) {
                    s.mem.writeb(address - BX_NE2K_MEMSTART, (short) value);
                } else if (io_len == 2) {
                    s.mem.writew(address - BX_NE2K_MEMSTART, (int) value);
                } else if (io_len == 4) {
                    s.mem.writed(address - BX_NE2K_MEMSTART, value);
                }
            }
        }

        //
        // asic_read/asic_write - This is the high 16 bytes of i/o space
        // (the lower 16 bytes is for the DS8390). Only two locations
        // are used: offset 0, which is used for data transfer, and
        // offset 0xf, which is used to reset the device.
        // The data transfer port is used to as 'external' DMA to the
        // DS8390. The chip has to have the DMA registers set up, and
        // after that, insw/outsw instructions can be used to move
        // the appropriate number of bytes to/from the device.
        //
        public /*Bit32u*/long asic_read(/*Bit32u*/int offset, /*unsigned*/int io_len) {
            /*Bit32u*/
            long retval = 0;

            switch (offset) {
                case 0x0:  // Data register
                    //
                    // A read remote-DMA command must have been issued,
                    // and the source-address and length registers must
                    // have been initialised.
                    //
                    if (io_len > s.remote_bytes) {
                        if (BX_ERROR)
                            Log.log_msg("[NE2000] dma read underrun iolen=" + io_len + " remote_bytes=" + s.remote_bytes);
                        //return 0;
                    }

                    //BX_INFO(("ne2k read DMA: addr=%4x remote_bytes=%d",s.remote_dma,s.remote_bytes));
                    retval = chipmem_read(s.remote_dma, io_len);
                    //
                    // The 8390 bumps the address and decreases the byte count
                    // by the selected word size after every access, not by
                    // the amount of data requested by the host (io_len).
                    //
                    s.remote_dma += (s.DCR.wdsize + 1);
                    if (s.remote_dma == s.page_stop << 8) {
                        s.remote_dma = s.page_start << 8;
                    }
                    // keep s.remote_bytes from underflowing
                    if (s.remote_bytes > 1)
                        s.remote_bytes -= (s.DCR.wdsize + 1);
                    else
                        s.remote_bytes = 0;

                    // If all bytes have been written, signal remote-DMA complete
                    if (s.remote_bytes == 0) {
                        s.ISR.rdma_done = 1;
                        if (s.IMR.rdma_inte != 0) {
                            Pic.PIC_ActivateIRQ(s.base_irq);
                            //DEV_pic_raise_irq(s.base_irq);
                        }
                    }
                    break;
                case 0xf:  // Reset register
                    reset(BX_RESET_SOFTWARE);
                    //retval=0x1;
                    break;
                default:
                    if (BX_INFO)
                        Log.log_msg(StringHelper.sprintf("[NE2000] asic read invalid address %04x", new Object[]{new Integer(offset)}));
                    break;
            }
            return retval;
        }

        public void asic_write(/*Bit32u*/int offset, /*Bit32u*/long value, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] asic write addr=0x%02x, value=0x%04x", new Object[]{new Integer(offset), new Long(value)}));
            switch (offset) {
                case 0x0:  // Data register - see asic_read for a description

                    if ((io_len == 2) && (s.DCR.wdsize == 0)) {
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] dma write length 2 on byte mode operation");
                        break;
                    }

                    if (s.remote_bytes == 0)
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] dma write, byte count 0");

                    chipmem_write(s.remote_dma, value, io_len);
                    // is this right ??? asic_read uses DCR.wordsize
                    s.remote_dma += io_len;
                    if (s.remote_dma == s.page_stop << 8) {
                        s.remote_dma = s.page_start << 8;
                    }

                    s.remote_bytes -= io_len;
                    if (s.remote_bytes > BX_NE2K_MEMSIZ)
                        s.remote_bytes = 0;

                    // If all bytes have been written, signal remote-DMA complete
                    if (s.remote_bytes == 0) {
                        s.ISR.rdma_done = 1;
                        if (s.IMR.rdma_inte != 0) {
                            Pic.PIC_ActivateIRQ(s.base_irq);
                            //DEV_pic_raise_irq(s.base_irq);
                        }
                    }
                    break;

                case 0xf:  // Reset register
                    reset(BX_RESET_SOFTWARE);
                    break;

                default: // this is invalid, but happens under win95 device detection
                    if (BX_INFO)
                        Log.log_msg(StringHelper.sprintf("[NE2000] asic write invalid address %04x, ignoring", new Object[]{new Long(offset)}));
                    break;
            }
        }

        //
        // page0_read/page0_write - These routines handle reads/writes to
        // the 'zeroth' page of the DS8390 register file
        //
        public /*Bit32u*/long page0_read(/*Bit32u*/int offset, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] page 0 read from port %04x, len=%d", new Object[]{new Integer(offset), new Integer(io_len)}));
            if (io_len > 1) {
                if (BX_ERROR) {
                    /* encountered with win98 hardware probe */
                    Log.log_msg(StringHelper.sprintf("[NE2000] bad length! page 0 read from port %04x, len=%u", new Object[]{new Integer(offset), new Integer(io_len)}));
                }
                return 0;
            }


            switch (offset) {
                case 0x1:  // CLDA0
                    return (s.local_dma & 0xff);
                case 0x2:  // CLDA1
                    return (s.local_dma >> 8);
                case 0x3:  // BNRY
                    return (s.bound_ptr);
                case 0x4:  // TSR
                    return ((s.TSR.ow_coll << 7) |
                            (s.TSR.cd_hbeat << 6) |
                            (s.TSR.fifo_ur << 5) |
                            (s.TSR.no_carrier << 4) |
                            (s.TSR.aborted << 3) |
                            (s.TSR.collided << 2) |
                            (s.TSR.tx_ok));
                case 0x5:  // NCR
                    return (s.num_coll);
                case 0x6:  // FIFO
                    // reading FIFO is only valid in loopback mode
                    if (BX_ERROR)
                        Log.log_msg("[NE2000] reading FIFO not supported yet");
                    return (s.fifo);
                case 0x7:  // ISR
                    return ((s.ISR.reset << 7) |
                            (s.ISR.rdma_done << 6) |
                            (s.ISR.cnt_oflow << 5) |
                            (s.ISR.overwrite << 4) |
                            (s.ISR.tx_err << 3) |
                            (s.ISR.rx_err << 2) |
                            (s.ISR.pkt_tx << 1) |
                            (s.ISR.pkt_rx));
                case 0x8:  // CRDA0
                    return (s.remote_dma & 0xff);
                case 0x9:  // CRDA1
                    return (s.remote_dma >> 8);
                case 0xa:  // reserved
                    if (BX_INFO)
                        Log.log_msg("[NE2000] reserved read - page 0, 0xa");
                    return (0xff);
                case 0xb:  // reserved
                    if (BX_INFO)
                        Log.log_msg("[NE2000] reserved read - page 0, 0xb");
                    return (0xff);
                case 0xc:  // RSR
                    return ((s.RSR.deferred << 7) |
                            (s.RSR.rx_disabled << 6) |
                            (s.RSR.rx_mbit << 5) |
                            (s.RSR.rx_missed << 4) |
                            (s.RSR.fifo_or << 3) |
                            (s.RSR.bad_falign << 2) |
                            (s.RSR.bad_crc << 1) |
                            (s.RSR.rx_ok));
                case 0xd:  // CNTR0
                    return (s.tallycnt_0);
                case 0xe:  // CNTR1
                    return (s.tallycnt_1);
                case 0xf:  // CNTR2
                    return (s.tallycnt_2);
                default:
                    if (BX_PANIC)
                        Log.log_msg(StringHelper.sprintf("[NE2000] page 0 read offset %04x out of range", new Object[]{new Integer(offset)}));
            }
            return (0);
        }

        public void page0_write(/*Bit32u*/int offset, /*Bit32u*/long val, /*unsigned*/int io_len) {
            int value = (int) val;
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] page 0 write to port %04x, len=%d", new Object[]{new Integer(offset), new Integer(io_len)}));

            // It appears to be a common practice to use outw on page0 regs...

            // break up outw into two outb's
            if (io_len == 2) {
                page0_write(offset, (value & 0xff), 1);
                page0_write(offset + 1, ((value >> 8) & 0xff), 1);
                return;
            }

            switch (offset) {
                case 0x1:  // PSTART
                    s.page_start = (short) value;
                    break;

                case 0x2:  // PSTOP
                    // BX_INFO(("Writing to PSTOP: %02x", value));
                    s.page_stop = (short) value;
                    break;

                case 0x3:  // BNRY
                    s.bound_ptr = (short) value;
                    break;

                case 0x4:  // TPSR
                    s.tx_page_start = (short) value;
                    break;

                case 0x5:  // TBCR0
                    // Clear out low byte and re-insert
                    s.tx_bytes &= 0xff00;
                    s.tx_bytes |= (value & 0xff);
                    break;

                case 0x6:  // TBCR1
                    // Clear out high byte and re-insert
                    s.tx_bytes &= 0x00ff;
                    s.tx_bytes |= ((value & 0xff) << 8);
                    break;

                case 0x7:  // ISR
                    value &= 0x7f;  // clear RST bit - status-only bit
                    // All other values are cleared iff the ISR bit is 1
                    s.ISR.pkt_rx &= ~((/*bx_bool*/int) ((value & 0x01) == 0x01 ? 1 : 0));
                    s.ISR.pkt_tx &= ~((/*bx_bool*/int) ((value & 0x02) == 0x02 ? 1 : 0));
                    s.ISR.rx_err &= ~((/*bx_bool*/int) ((value & 0x04) == 0x04 ? 1 : 0));
                    s.ISR.tx_err &= ~((/*bx_bool*/int) ((value & 0x08) == 0x08 ? 1 : 0));
                    s.ISR.overwrite &= ~((/*bx_bool*/int) ((value & 0x10) == 0x10 ? 1 : 0));
                    s.ISR.cnt_oflow &= ~((/*bx_bool*/int) ((value & 0x20) == 0x20 ? 1 : 0));
                    s.ISR.rdma_done &= ~((/*bx_bool*/int) ((value & 0x40) == 0x40 ? 1 : 0));
                    value = ((s.ISR.rdma_done << 6) |
                            (s.ISR.cnt_oflow << 5) |
                            (s.ISR.overwrite << 4) |
                            (s.ISR.tx_err << 3) |
                            (s.ISR.rx_err << 2) |
                            (s.ISR.pkt_tx << 1) |
                            (s.ISR.pkt_rx));
                    value &= ((s.IMR.rdma_inte << 6) |
                            (s.IMR.cofl_inte << 5) |
                            (s.IMR.overw_inte << 4) |
                            (s.IMR.txerr_inte << 3) |
                            (s.IMR.rxerr_inte << 2) |
                            (s.IMR.tx_inte << 1) |
                            (s.IMR.rx_inte));
                    if (value == 0)
                        Pic.PIC_DeActivateIRQ(s.base_irq);
                    //DEV_pic_lower_irq(s.base_irq);
                    break;

                case 0x8:  // RSAR0
                    // Clear out low byte and re-insert
                    s.remote_start &= 0xff00;
                    s.remote_start |= (value & 0xff);
                    s.remote_dma = s.remote_start;
                    break;

                case 0x9:  // RSAR1
                    // Clear out high byte and re-insert
                    s.remote_start &= 0x00ff;
                    s.remote_start |= ((value & 0xff) << 8);
                    s.remote_dma = s.remote_start;
                    break;

                case 0xa:  // RBCR0
                    // Clear out low byte and re-insert
                    s.remote_bytes &= 0xff00;
                    s.remote_bytes |= (value & 0xff);
                    break;

                case 0xb:  // RBCR1
                    // Clear out high byte and re-insert
                    s.remote_bytes &= 0x00ff;
                    s.remote_bytes |= ((value & 0xff) << 8);
                    break;

                case 0xc:  // RCR
                    // Check if the reserved bits are set
                    if ((value & 0xc0) != 0)
                        if (BX_INFO)
                            Log.log_msg("[NE2000] RCR write, reserved bits set");

                    // Set all other bit-fields
                    s.RCR.errors_ok = ((value & 0x01) == 0x01) ? 1 : 0;
                    s.RCR.runts_ok = ((value & 0x02) == 0x02) ? 1 : 0;
                    s.RCR.broadcast = ((value & 0x04) == 0x04) ? 1 : 0;
                    s.RCR.multicast = ((value & 0x08) == 0x08) ? 1 : 0;
                    s.RCR.promisc = ((value & 0x10) == 0x10) ? 1 : 0;
                    s.RCR.monitor = ((value & 0x20) == 0x20) ? 1 : 0;

                    // Monitor bit is a little suspicious...
                    if ((value & 0x20) != 0)
                        if (BX_INFO)
                            Log.log_msg("[NE2000] RCR write, monitor bit set!");
                    break;

                case 0xd:  // TCR
                    // Check reserved bits
                    if ((value & 0xe0) != 0)
                        if (BX_ERROR)
                            Log.log_msg("[NE2000] TCR write, reserved bits set");

                    // Test loop mode (not supported)
                    if ((value & 0x06) != 0) {
                        s.TCR.loop_cntl = (short) ((value & 0x6) >> 1);
                        if (BX_INFO)
                            Log.log_msg("[NE2000] TCR write, loop mode " + s.TCR.loop_cntl + " not supported");
                    } else {
                        s.TCR.loop_cntl = 0;
                    }

                    // Inhibit-CRC not supported.
                    if ((value & 0x01) != 0)
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] TCR write, inhibit-CRC not supported");

                    // Auto-transmit disable very suspicious
                    if ((value & 0x08) != 0)
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] TCR write, auto transmit disable not supported");

                    // Allow collision-offset to be set, although not used
                    s.TCR.coll_prio = ((value & 0x08) == 0x08) ? 1 : 0;
                    break;

                case 0xe:  // DCR
                    // the loopback mode is not suppported yet
                    if ((value & 0x08) == 0) {
                        if (BX_ERROR)
                            Log.log_msg("[NE2000] DCR write, loopback mode selected");
                    }
                    // It is questionable to set longaddr and auto_rx, since they
                    // aren't supported on the ne2000. Print a warning and continue
                    if ((value & 0x04) != 0)
                        if (BX_INFO)
                            Log.log_msg("[NE2000] DCR write - LAS set ???");
                    if ((value & 0x10) != 0)
                        if (BX_INFO)
                            Log.log_msg("[NE2000] DCR write - AR set ???");

                    // Set other values.
                    s.DCR.wdsize = ((value & 0x01) == 0x01) ? 1 : 0;
                    s.DCR.endian = ((value & 0x02) == 0x02) ? 1 : 0;
                    s.DCR.longaddr = ((value & 0x04) == 0x04) ? 1 : 0; // illegal ?
                    s.DCR.loop = ((value & 0x08) == 0x08) ? 1 : 0;
                    s.DCR.auto_rx = ((value & 0x10) == 0x10) ? 1 : 0; // also illegal ?
                    s.DCR.fifo_size = (short) ((value & 0x50) >> 5);
                    break;

                case 0xf:  // IMR
                    // Check for reserved bit
                    if ((value & 0x80) != 0)
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] IMR write, reserved bit set");

                    // Set other values
                    s.IMR.rx_inte = ((value & 0x01) == 0x01) ? 1 : 0;
                    s.IMR.tx_inte = ((value & 0x02) == 0x02) ? 1 : 0;
                    s.IMR.rxerr_inte = ((value & 0x04) == 0x04) ? 1 : 0;
                    s.IMR.txerr_inte = ((value & 0x08) == 0x08) ? 1 : 0;
                    s.IMR.overw_inte = ((value & 0x10) == 0x10) ? 1 : 0;
                    s.IMR.cofl_inte = ((value & 0x20) == 0x20) ? 1 : 0;
                    s.IMR.rdma_inte = ((value & 0x40) == 0x40) ? 1 : 0;
                    if (s.ISR.pkt_tx != 0 && s.IMR.tx_inte != 0) {
                        Log.log_msg("[NE2000] tx irq retrigger");
                        Pic.PIC_ActivateIRQ(s.base_irq);
                    }
                    break;
                default:
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] page 0 write, bad offset " + Integer.toString(offset, 16));
            }
        }

        //
        // page1_read/page1_write - These routines handle reads/writes to
        // the first page of the DS8390 register file
        //
        public /*Bit32u*/long page1_read(/*Bit32u*/int offset, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] page 1 read from port %04x, len=%d", new Object[]{new Integer(offset), new Integer(io_len)}));
            if (io_len > 1) {
                if (BX_ERROR) {
                    /* encountered with win98 hardware probe */
                    Log.log_msg(StringHelper.sprintf("[NE2000] bad length! page 1 read from port %04x, len=%u", new Object[]{new Integer(offset), new Integer(io_len)}));
                }
                return 0;
            }

            switch (offset) {
                case 0x1:  // PAR0-5
                case 0x2:
                case 0x3:
                case 0x4:
                case 0x5:
                case 0x6:
                    return (s.physaddr[offset - 1]);

                case 0x7:  // CURR
                    if (BX_DEBUG)
                        Log.log_msg("[NE2000] returning current page: " + s.curr_page);
                    return (s.curr_page);

                case 0x8:  // MAR0-7
                case 0x9:
                case 0xa:
                case 0xb:
                case 0xc:
                case 0xd:
                case 0xe:
                case 0xf:
                    return (s.mchash[offset - 8]);

                default:
                    if (BX_PANIC)
                        Log.log_msg(StringHelper.sprintf("[NE2000] page 1 read offset %04x out of range", new Object[]{new Integer(offset)}));
            }
            return 0;
        }

        public void page1_write(/*Bit32u*/int offset, /*Bit32u*/long value, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] page 1 write to port %04x, len=%d", new Object[]{new Integer(offset), new Integer(io_len)}));

            switch (offset) {
                case 0x1:  // PAR0-5
                case 0x2:
                case 0x3:
                case 0x4:
                case 0x5:
                case 0x6:
                    s.physaddr[offset - 1] = (byte) value;
                    break;

                case 0x7:  // CURR
                    s.curr_page = (short) value;
                    break;

                case 0x8:  // MAR0-7
                case 0x9:
                case 0xa:
                case 0xb:
                case 0xc:
                case 0xd:
                case 0xe:
                case 0xf:
                    s.mchash[offset - 8] = (byte) value;
                    break;

                default:
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] page 1 write, bad offset " + Integer.toString(offset, 16));
            }
        }

        //
        // page2_read/page2_write - These routines handle reads/writes to
        // the second page of the DS8390 register file
        //
        public /*Bit32u*/long page2_read(/*Bit32u*/int offset, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] page 2 read from port %04x, len=%d", new Object[]{new Integer(offset), new Integer(io_len)}));
            if (io_len > 1) {
                if (BX_ERROR) {
                    /* encountered with win98 hardware probe */
                    Log.log_msg(StringHelper.sprintf("[NE2000] bad length! page 2 read from port %04x, len=%u", new Object[]{new Integer(offset), new Integer(io_len)}));
                }
                return 0;
            }

            switch (offset) {
                case 0x1:  // PSTART
                    return (s.page_start);
                case 0x2:  // PSTOP
                    return (s.page_stop);
                case 0x3:  // Remote Next-packet pointer
                    return (s.rempkt_ptr);
                case 0x4:  // TPSR
                    return (s.tx_page_start);
                case 0x5:  // Local Next-packet pointer
                    return (s.localpkt_ptr);
                case 0x6:  // Address counter (upper)
                    return (s.address_cnt >> 8);
                case 0x7:  // Address counter (lower)
                    return (s.address_cnt & 0xff);
                case 0x8:  // Reserved
                case 0x9:
                case 0xa:
                case 0xb:
                    if (BX_ERROR)
                        Log.log_msg("reserved read - page 2, 0x" + Integer.toString(offset, 16));
                    return (0xff);
                case 0xc:  // RCR
                    return ((s.RCR.monitor << 5) |
                            (s.RCR.promisc << 4) |
                            (s.RCR.multicast << 3) |
                            (s.RCR.broadcast << 2) |
                            (s.RCR.runts_ok << 1) |
                            (s.RCR.errors_ok));
                case 0xd:  // TCR
                    return ((s.TCR.coll_prio << 4) |
                            (s.TCR.ext_stoptx << 3) |
                            ((s.TCR.loop_cntl & 0x3) << 1) |
                            (s.TCR.crc_disable));
                case 0xe:  // DCR
                    return (((s.DCR.fifo_size & 0x3) << 5) |
                            (s.DCR.auto_rx << 4) |
                            (s.DCR.loop << 3) |
                            (s.DCR.longaddr << 2) |
                            (s.DCR.endian << 1) |
                            (s.DCR.wdsize));
                case 0xf:  // IMR
                    return ((s.IMR.rdma_inte << 6) |
                            (s.IMR.cofl_inte << 5) |
                            (s.IMR.overw_inte << 4) |
                            (s.IMR.txerr_inte << 3) |
                            (s.IMR.rxerr_inte << 2) |
                            (s.IMR.tx_inte << 1) |
                            (s.IMR.rx_inte));
                default:
                    if (BX_PANIC)
                        Log.log_msg(StringHelper.sprintf("[NE2000] page 2 read offset %04x out of range", new Object[]{new Integer(offset)}));
            }
            return 0;
        }

        public void page2_write(/*Bit32u*/int offset, /*Bit32u*/long value, /*unsigned*/int io_len) {
            // Maybe all writes here should be BX_PANIC()'d, since they
            // affect internal operation, but let them through for now
            // and print a warning.
            if (offset != 0)
                if (BX_ERROR)
                    Log.log_msg("[NE2000] page 2 write ?");

            switch (offset) {
                case 0x1:  // CLDA0
                    // Clear out low byte and re-insert
                    s.local_dma &= 0xff00;
                    s.local_dma |= (value & 0xff);
                    break;

                case 0x2:  // CLDA1
                    // Clear out high byte and re-insert
                    s.local_dma &= 0x00ff;
                    s.local_dma |= ((value & 0xff) << 8);
                    break;

                case 0x3:  // Remote Next-pkt pointer
                    s.rempkt_ptr = (short) value;
                    break;

                case 0x4:
                    if (BX_PANIC)
                        Log.log_msg("page 2 write to reserved offset 4");
                    break;

                case 0x5:  // Local Next-packet pointer
                    s.localpkt_ptr = (short) value;
                    break;

                case 0x6:  // Address counter (upper)
                    // Clear out high byte and re-insert
                    s.address_cnt &= 0x00ff;
                    s.address_cnt |= ((value & 0xff) << 8);
                    break;

                case 0x7:  // Address counter (lower)
                    // Clear out low byte and re-insert
                    s.address_cnt &= 0xff00;
                    s.address_cnt |= (value & 0xff);
                    break;

                case 0x8:
                case 0x9:
                case 0xa:
                case 0xb:
                case 0xc:
                case 0xd:
                case 0xe:
                case 0xf:
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] page 2 write to reserved offset " + Integer.toString(offset, 16));
                    break;

                default:
                    if (BX_PANIC)
                        Log.log_msg("[NE2000] page 2 write, illegal offset " + Integer.toString(offset));
                    break;
            }
        }

        //
        // page3_read/page3_write - writes to this page are illegal
        //
        public /*Bit32u*/long page3_read(/*Bit32u*/int offset, /*unsigned*/int io_len) {
            if (BX_PANIC)
                Log.log_msg("[NE2000] page 3 read attempted");
            return 0;
        }

        public void page3_write(/*Bit32u*/int address, /*Bit32u*/long value, /*unsigned*/int io_len) {
            if (BX_PANIC)
                Log.log_msg("[NE2000] page 3 write attempted");
        }

//        public static void tx_timer_handler() {
//        }

        public void tx_timer() {
            if (BX_DEBUG)
                Log.log_msg("[NE2000] tx_timer");
            s.TSR.tx_ok = 1;
            // Generate an interrupt if not masked and not one in progress
            if (s.IMR.tx_inte != 0 && s.ISR.pkt_tx == 0) {
                //LOG_MSG("tx complete interrupt");
                Pic.PIC_ActivateIRQ(s.base_irq);
                //DEV_pic_raise_irq(s.base_irq);
            } //else 	  LOG_MSG("no tx complete interrupt");
            s.ISR.pkt_tx = 1;
            s.tx_timer_active = 0;
        }

//        static /*Bit32u*/long read_handler(Object this_ptr, /*Bit32u*/long address, /*unsigned*/int io_len) {
//        }

        public /*Bit32u*/long read(/*Bit32u*/long address, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg(StringHelper.sprintf("[NE2000] read addr %x, len %d", new Object[]{new Long(address), new Integer(io_len)}));
            /*Bit32u*/
            long retval = 0;
            int offset = (int) (address - s.base_address);

            if (offset >= 0x10) {
                retval = asic_read(offset - 0x10, io_len);
            } else if (offset == 0x00) {
                retval = read_cr();
            } else {
                switch (s.CR.pgsel) {
                    case 0x00:
                        retval = page0_read(offset, io_len);
                        break;

                    case 0x01:
                        retval = page1_read(offset, io_len);
                        break;

                    case 0x02:
                        retval = page2_read(offset, io_len);
                        break;

                    case 0x03:
                        retval = page3_read(offset, io_len);
                        break;

                    default:
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] unknown value of pgsel in read - " + s.CR.pgsel);
                }
            }
            return retval;
        }

        // static void write_handler(void*this_ptr, Bit32u address, Bit32u value, unsigned io_len);

        public void write(/*Bit32u*/long address, /*Bit32u*/long value, /*unsigned*/int io_len) {
            if (BX_DEBUG)
                Log.log_msg("[NE2000] write with length " + io_len);
            int offset = (int) (address - s.base_address);

            //
            // The high 16 bytes of i/o space are for the ne2000 asic -
            //  the low 16 bytes are for the DS8390, with the current
            //  page being selected by the PS0,PS1 registers in the
            //  command register
            //
            if (offset >= 0x10) {
                asic_write(offset - 0x10, value, io_len);
            } else if (offset == 0x00) {
                write_cr((int) value);
            } else {
                switch (s.CR.pgsel) {
                    case 0x00:
                        page0_write(offset, value, io_len);
                        break;

                    case 0x01:
                        page1_write(offset, value, io_len);
                        break;

                    case 0x02:
                        page2_write(offset, value, io_len);
                        break;

                    case 0x03:
                        page3_write(offset, value, io_len);
                        break;

                    default:
                        if (BX_PANIC)
                            Log.log_msg("[NE2000] unknown value of pgsel in write - " + s.CR.pgsel);
                }
            }
        }

        /*
         * mcast_index() - return the 6-bit index into the multicast
         * table. Stolen unashamedly from FreeBSD's if_ed.c
         */
        public /*unsigned*/int mcast_index(byte[] dst, int offset) {
            final int POLYNOMIAL = 0x04c11db6;
            int crc = 0xffffffff;
            int carry, i, j;
            byte b;

            for (i = 6; --i >= 0; ) {
                b = dst[offset++];
                for (j = 8; --j >= 0; ) {
                    carry = ((crc & 0x80000000L) != 0 ? 1 : 0) ^ (b & 0x01);
                    crc <<= 1;
                    b >>>= 1;
                    if (carry != 0)
                        crc = ((crc ^ POLYNOMIAL) | carry);
                }
            }
            return crc >>> 26;
        }

        //static void rx_handler(void*arg, const void*buf, unsigned len);

        /*
         * rx_frame() - called by the platform-specific code when an
         * ethernet frame has been received. The destination address
         * is tested to see if it should be accepted, and if the
         * rx ring has enough room, it is copied into it and
         * the receive process is updated
         */
        public boolean rx_frame(Ptr buf, /*unsigned*/int io_len) {
            if((s.DCR.loop == 0) || (s.TCR.loop_cntl != 0))
                return false;
            int pages;
            int avail;
            /*unsigned*/
            int idx;
            int wrapped;
            int nextpage;
            byte[] pkthdr = new byte[4];
            Ptr pktbuf = buf;
            Ptr startptr;
            final byte[] bcast_addr = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

            if (io_len != 60) {
                if (BX_DEBUG)
                    Log.log_msg("[NE2000] rx_frame with length " + io_len);
            }

            //LOG_MSG("stop=%d, pagestart=%x, dcr_loop=%x, tcr_loopcntl=%x",
            //	s.CR.stop, s.page_start,
            //	s.DCR.loop, s.TCR.loop_cntl);
            if ((s.CR.stop != 0) ||
                    (s.page_start == 0) /*||
                  ((s.DCR.loop == 0) &&
                   (s.TCR.loop_cntl != 0))*/) {
                return true;
            }

            // Add the pkt header + CRC to the length, and work
            // out how many 256-byte pages the frame would occupy
            pages = (io_len + 4 + 4 + 255) / 256;

            if (s.curr_page < s.bound_ptr) {
                avail = s.bound_ptr - s.curr_page;
            } else {
                avail = (s.page_stop - s.page_start) -
                        (s.curr_page - s.bound_ptr);
                wrapped = 1;
            }

            // Avoid getting into a buffer overflow condition by not attempting
            // to do partial receives. The emulation to handle this condition
            // seems particularly painful.
            if (BX_NE2K_NEVER_FULL_RING != 0) {
                if (avail <= pages) {
                    if (BX_DEBUG)
                        Log.log_msg("[NE2000] no space");
                    return true;
                }
            } else {
                if (avail < pages) {
                    if (BX_DEBUG)
                        Log.log_msg("[NE2000] no space");
                    return true;
                }

            }
            if ((io_len < 60) && s.RCR.runts_ok == 0) {
                if (BX_DEBUG)
                    Log.log_msg("[NE2000] rejected small packet, length " + io_len);
                return true;
            }
            // some computers don't care...
            //if (io_len < 60) io_len = 60;

            // Do address filtering if not in promiscuous mode
            if (s.RCR.promisc == 0) {
                if (Ptr.memcmp(buf, bcast_addr, 6) == 0) {
                    if (s.RCR.broadcast == 0) {
                        return true;
                    }
                } else if ((pktbuf.readb(0) & 0x01) != 0) {
                    if (s.RCR.multicast == 0) {
                        return true;
                    }
                    idx = mcast_index(buf.p, buf.off);
                    if ((s.mchash[idx >> 3] & (1 << (idx & 0x7))) == 0) {
                        return true;
                    }
                } else if (0 != Ptr.memcmp(buf, s.physaddr, 6)) {
                    return true;
                }
            } else {
                if (BX_DEBUG)
                    Log.log_msg("[NE2000] rx_frame promiscuous receive");
            }

//                BX_INFO("rx_frame %d to %x:%x:%x:%x:%x:%x from %x:%x:%x:%x:%x:%x",
//                     io_len,
//                     pktbuf[0], pktbuf[1], pktbuf[2], pktbuf[3], pktbuf[4], pktbuf[5],
//                     pktbuf[6], pktbuf[7], pktbuf[8], pktbuf[9], pktbuf[10], pktbuf[11]);

            nextpage = s.curr_page + pages;
            if (nextpage >= s.page_stop) {
                nextpage -= s.page_stop - s.page_start;
            }

            // Setup packet header
            pkthdr[0] = 0;    // rx status - old behavior
            pkthdr[0] = 1;        // Probably better to set it all the time
            // rather than set it to 0, which is clearly wrong.
            if ((pktbuf.readb(0) & 0x01) != 0) {
                pkthdr[0] |= 0x20;  // rx status += multicast packet
            }
            pkthdr[1] = (byte) nextpage;    // ptr to next packet
            pkthdr[2] = (byte) ((io_len + 4) & 0xff);    // length-low
            pkthdr[3] = (byte) ((io_len + 4) >> 8);    // length-hi

            // copy into buffer, update curpage, and signal interrupt if config'd
            startptr = new Ptr(s.mem, s.curr_page * 256 - BX_NE2K_MEMSTART);
            if ((nextpage > s.curr_page) || ((s.curr_page + pages) == s.page_stop)) {
                Ptr.memcpy(startptr, pkthdr, 4);
                startptr.inc(4);
                Ptr.memcpy(startptr, buf, io_len);
                s.curr_page = (short) nextpage;
            } else {
                int endbytes = (s.page_stop - s.curr_page) * 256;
                Ptr.memcpy(startptr, pkthdr, 4);
                startptr.inc(4);
                Ptr.memcpy(startptr, buf, endbytes - 4);
                startptr = new Ptr(s.mem, s.page_start * 256 - BX_NE2K_MEMSTART);
                Ptr.memcpy(startptr, new Ptr(pktbuf, endbytes - 4), io_len - endbytes + 4);
                s.curr_page = (short) nextpage;
            }

            s.RSR.rx_ok = 1;
            if ((pktbuf.readb(0) & 0x80) != 0) {
                s.RSR.rx_mbit = 1;
            }

            s.ISR.pkt_rx = 1;

            if (s.IMR.rx_inte != 0) {
                //LOG_MSG("packet rx interrupt");
                Pic.PIC_ActivateIRQ(s.base_irq);
                //DEV_pic_raise_irq(s.base_irq);
            } //else LOG_MSG("no packet rx interrupt");
            return true;
        }
    }

    static final private IoHandler.IO_ReadHandler dosbox_read = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return (int) theNE2kDevice.read(port, iolen);
        }
    };
    static final private IoHandler.IO_WriteHandler dosbox_write = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            theNE2kDevice.write(port, (long) (val & 0xFFFFFFFFl), iolen);
        }
    };

    static private final Pic.PIC_EventHandler NE2000_TX_Event = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            theNE2kDevice.tx_timer();
        }
    };

    static final private Timer.TIMER_TickHandler NE2000_Poller = new Timer.TIMER_TickHandler() {
        public void call() {
            test.ethernet.receive(theNE2kDevice);
        }
    };


    // Data
    IoHandler.IO_ReadHandleObject[] ReadHandler8 = new IoHandler.IO_ReadHandleObject[0x20];
    IoHandler.IO_WriteHandleObject[] WriteHandler8 = new IoHandler.IO_WriteHandleObject[0x20];

    boolean load_success;
    Ethernet ethernet;

    public NE2000(Section configuration) {
        super(configuration);
        Section_prop section = (Section_prop) (configuration);

        load_success = true;
        // enabled?
        String mode = section.Get_string("mode");
        if (mode == null || mode.length()==0 || mode.equalsIgnoreCase("false")) {
            load_success = false;
            return;
        }

        // get irq and base
        /*Bitu*/
        int irq = section.Get_int("nicirq");
        if (!(irq == 3 || irq == 4 || irq == 5 || irq == 6 || irq == 7 ||
                irq == 9 || irq == 10 || irq == 11 || irq == 12 || irq == 14 || irq == 15)) {
            irq = 3;
        }
        /*Bitu*/
        int base = section.Get_hex("nicbase").toInt();
        if (!(base == 0x260 || base == 0x280 || base == 0x300 || base == 0x320 || base == 0x340 || base == 0x380)) {
            base = 0x300;
        }

        // mac address
        String macstring = section.Get_string("macaddr");
        String[] parts = StringHelper.split(macstring, ":");
        /*Bit8u*/
        byte[] mac = new byte[6];
        try {
            for (int i = 0; i < parts.length; i++) {
                int d = Integer.parseInt(parts[i], 16);
                if (d > 0xFF)
                    throw new Exception("Invalid macaddr string: " + macstring);
                mac[i] = (byte) d;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mac[0] = (byte) 0xac;
            mac[1] = (byte) 0xde;
            mac[2] = (byte) 0x48;
            mac[3] = (byte) 0x88;
            mac[4] = (byte) 0xbb;
            mac[5] = (byte) 0xaa;
        }

        if (mode.equalsIgnoreCase("pcap")) {
            try {
                Class c = Class.forName("jdos.host.PCapEthernet");
                ethernet = (Ethernet)c.newInstance();
                if (!ethernet.open(section, mac)) {
                    ethernet = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }  else if (mode.equalsIgnoreCase("pcaphost")) {
            try {
                Class c = Class.forName("jdos.host.FowardPCapEthernet");
                ethernet = (Ethernet)c.newInstance();
                if (!ethernet.open(section, mac))
                    ethernet = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (mode.equalsIgnoreCase("user")) {
            ethernet = new UserEthernet();
            if (!ethernet.open(section, mac)) {
                ethernet = null;
            }
        }
        if (ethernet == null) {
            Log.log_msg("Network card disabled. mode="+mode+" not found.");
            load_success = false;
            return;
        }

        // create the bochs NIC class
        theNE2kDevice = new bx_ne2k_c();
        Ptr.memcpy(theNE2kDevice.s.physaddr, mac, 6);
        theNE2kDevice.init();

        theNE2kDevice.s.base_address = base;
        theNE2kDevice.s.base_irq = irq;

        // install I/O-handlers and timer
        for (/*Bitu*/int i = 0; i < 0x20; i++) {
            ReadHandler8[i] = new IoHandler.IO_ReadHandleObject();
            ReadHandler8[i].Install((int) (i + theNE2kDevice.s.base_address), dosbox_read, IoHandler.IO_MB | IoHandler.IO_MW);
            WriteHandler8[i] = new IoHandler.IO_WriteHandleObject();
            WriteHandler8[i].Install((int) (i + theNE2kDevice.s.base_address), dosbox_write, IoHandler.IO_MB | IoHandler.IO_MW);
        }
        Timer.TIMER_AddTickHandler(NE2000_Poller);
    }

    public void close() {
        if (ethernet!=null)
            ethernet.close();
        Timer.TIMER_DelTickHandler(NE2000_Poller);
        Pic.PIC_RemoveEvents(NE2000_TX_Event);
    }

    static private NE2000 test;

    private static Section.SectionFunction NE2000_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            test.close();
            test = null;
        }
    };

    public static Section.SectionFunction NE2000_Init = new Section.SectionFunction() {
        public void call(Section section) {
            test = new NE2000(section);
            section.AddDestroyFunction(NE2000_ShutDown, true);
        }
    };
}





