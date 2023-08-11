package jdos.hardware.qemu;

/*
 * QEMU Floppy disk emulator (Intel 82078)
 *
 * Copyright (c) 2003, 2007 Jocelyn Mayer
 * Copyright (c) 2008 Hervé Poussineau
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import jdos.hardware.IoHandler;
import jdos.hardware.Pic;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.FileIO;
import jdos.util.IntRef;
import jdos.util.Ptr;

public class Floppy {
    /********************************************************/
    /* Floppy drive emulation                               */

    static private final int MAX_FD = 2;

    static private final int FDRIVE_DRV_144  = 0x00;   /* 1.44 MB 3"5 drive      */
    static private final int FDRIVE_DRV_288  = 0x01;   /* 2.88 MB 3"5 drive      */
    static private final int FDRIVE_DRV_120  = 0x02;   /* 1.2  MB 5"25 drive     */
    static private final int FDRIVE_DRV_NONE = 0x03;   /* No drive connected     */

    
    static private final int FDRIVE_RATE_500K = 0x00;  /* 500 Kbps */
    static private final int FDRIVE_RATE_300K = 0x01;  /* 300 Kbps */
    static private final int FDRIVE_RATE_250K = 0x02;  /* 250 Kbps */
    static private final int FDRIVE_RATE_1M   = 0x03;  /*   1 Mbps */

    static private void FLOPPY_DPRINTF(String s) {
        //System.out.print("FLOPPY: ");
        //System.out.println(s);
    }
    
    static final private class FDFormat {
        public FDFormat(int drive, int last_sect, int max_track, int max_head, int rate) {
            this.drive = drive;
            this.last_sect = last_sect;
            this.max_track = max_track;
            this.max_head = max_head;
            this.rate = rate;
        }
        int drive;
        int last_sect;
        int max_track;
        int max_head;
        int rate;
    }

    static final private FDFormat[] fd_formats = new FDFormat[] {
        /* First entry is default format */
        /* 1.44 MB 3"1/2 floppy disks */
        new FDFormat( FDRIVE_DRV_144, 18, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 20, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 21, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 21, 82, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 21, 83, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 22, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 23, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_144, 24, 80, 1, FDRIVE_RATE_500K),
        /* 2.88 MB 3"1/2 floppy disks */
        new FDFormat( FDRIVE_DRV_288, 36, 80, 1, FDRIVE_RATE_1M),
        new FDFormat( FDRIVE_DRV_288, 39, 80, 1, FDRIVE_RATE_1M),
        new FDFormat( FDRIVE_DRV_288, 40, 80, 1, FDRIVE_RATE_1M),
        new FDFormat( FDRIVE_DRV_288, 44, 80, 1, FDRIVE_RATE_1M),
        new FDFormat( FDRIVE_DRV_288, 48, 80, 1, FDRIVE_RATE_1M),
        /* 720 kB 3"1/2 floppy disks */
        new FDFormat( FDRIVE_DRV_144,  9, 80, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_144, 10, 80, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_144, 10, 82, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_144, 10, 83, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_144, 13, 80, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_144, 14, 80, 1, FDRIVE_RATE_250K),
        /* 1.2 MB 5"1/4 floppy disks */
        new FDFormat( FDRIVE_DRV_120, 15, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_120, 18, 80, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_120, 18, 82, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_120, 18, 83, 1, FDRIVE_RATE_500K),
        new FDFormat( FDRIVE_DRV_120, 20, 80, 1, FDRIVE_RATE_500K),
        /* 720 kB 5"1/4 floppy disks */
        new FDFormat( FDRIVE_DRV_120,  9, 80, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_120, 11, 80, 1, FDRIVE_RATE_250K),
        /* 360 kB 5"1/4 floppy disks */
        new FDFormat( FDRIVE_DRV_120,  9, 40, 1, FDRIVE_RATE_300K),
        new FDFormat( FDRIVE_DRV_120,  9, 40, 0, FDRIVE_RATE_300K),
        new FDFormat( FDRIVE_DRV_120, 10, 41, 1, FDRIVE_RATE_300K),
        new FDFormat( FDRIVE_DRV_120, 10, 42, 1, FDRIVE_RATE_300K),
        /* 320 kB 5"1/4 floppy disks */
        new FDFormat( FDRIVE_DRV_120,  8, 40, 1, FDRIVE_RATE_250K),
        new FDFormat( FDRIVE_DRV_120,  8, 40, 0, FDRIVE_RATE_250K),
        /* 360 kB must match 5"1/4 better than 3"1/2... */
        new FDFormat( FDRIVE_DRV_144,  9, 80, 0, FDRIVE_RATE_250K),
        /* end */
        new FDFormat( FDRIVE_DRV_NONE, -1, -1, 0, 0)
    };

    static private void pick_geometry(Block.BlockDriverState bs, IntRef nb_heads, IntRef max_track, IntRef last_sect, int drive_in, IntRef drive, IntRef rate) {
        FDFormat parse;
        long nb_sectors, size;
        int i, first_match, match;

        nb_sectors = Block.bdrv_get_geometry(bs);
        match = -1;
        first_match = -1;
        for (i = 0; ; i++) {
            parse = fd_formats[i];
            if (parse.drive == FDRIVE_DRV_NONE) {
                break;
            }
            if (drive_in == parse.drive ||
                drive_in == FDRIVE_DRV_NONE) {
                size = (parse.max_head + 1) * parse.max_track * parse.last_sect;
                if (nb_sectors == size) {
                    match = i;
                    break;
                }
                if (first_match == -1) {
                    first_match = i;
                }
            }
        }
        if (match == -1) {
            if (first_match == -1) {
                match = 1;
            } else {
                match = first_match;
            }
            parse = fd_formats[match];
        }
        nb_heads.value = parse.max_head + 1;
        max_track.value = parse.max_track;
        last_sect.value = parse.last_sect;
        drive.value = parse.drive;
        rate.value = parse.rate;
    }

    static private int GET_CUR_DRV(FDCtrl fdctrl) {
        return fdctrl.cur_drv;
    }

    static private void SET_CUR_DRV(FDCtrl fdctrl, int drive) {
        fdctrl.cur_drv = drive;
    }
    
    /* Will always be a fixed parameter for us */
    static final private int FD_SECTOR_LEN =        512;
    static final private int FD_SECTOR_SC =         2;   /* Sector size code */
    static final private int FD_RESET_SENSEI_COUNT= 4;   /* Number of sense interrupts on RESET */


    /* Floppy disk drive emulation */
    static final private int FDISK_DBL_SIDES = 0x01;

    static final private class FDrive {
        FDCtrl fdctrl;
        Block.BlockDriverState bs;
        /* Drive status */
        int drive;
        int perpendicular;    /* 2.88 MB access mode    */
        /* Position */
        int head;
        int track;
        int sect;
        /* Media */
        int flags;
        int last_sect;        /* Nb sector per track    */
        int max_track;        /* Nb of tracks           */
        int bps;             /* Bytes per sector       */
        boolean ro;               /* Is read-only           */
        boolean media_changed;    /* Is media changed       */
        int media_rate;       /* Data rate of medium    */
    }

    static private void fd_init(FDrive drv) {
        /* Drive */
        drv.drive = FDRIVE_DRV_NONE;
        drv.perpendicular = 0;
        /* Disk */
        drv.last_sect = 0;
        drv.max_track = 0;
    }

    static private int NUM_SIDES(FDrive drv) {
        return (drv.flags & FDISK_DBL_SIDES)!=0 ? 2 : 1;
    }

    static private int fd_sector_calc(int head, int track, int sect, int last_sect, int num_sides) {
        return (((track * num_sides) + head) * last_sect) + sect - 1;
    }

    /* Returns current position, in sectors, for given drive */
    static private int fd_sector(FDrive drv) {
        return fd_sector_calc(drv.head, drv.track, drv.sect, drv.last_sect, NUM_SIDES(drv));
    }

    /* Seek to a new position:
     * returns 0 if already on right track
     * returns 1 if track changed
     * returns 2 if track is invalid
     * returns 3 if sector is invalid
     * returns 4 if seek is disabled
     */
    static private int fd_seek(FDrive drv, int head, int track, int sect, int enable_seek) {
        int sector;
        int ret;

        if (track > drv.max_track || (head != 0 && (drv.flags & FDISK_DBL_SIDES) == 0)) {
            FLOPPY_DPRINTF("try to read "+head+" "+Integer.toHexString(track)+" "+Integer.toHexString(sect)+" (max=1 "+((drv.flags & FDISK_DBL_SIDES) == 0 ? 0 : 1)+" "+Integer.toHexString(drv.max_track)+" "+Integer.toHexString(drv.last_sect));
            return 2;
        }
        if (sect > drv.last_sect) {
            FLOPPY_DPRINTF("try to read "+head+" "+Integer.toHexString(track)+" "+Integer.toHexString(sect)+" (max=1 "+((drv.flags & FDISK_DBL_SIDES) == 0 ? 0 : 1)+" "+Integer.toHexString(drv.max_track)+" "+Integer.toHexString(drv.last_sect));
            return 3;
        }
        sector = fd_sector_calc(head, track, sect, drv.last_sect, NUM_SIDES(drv));
        ret = 0;
        if (sector != fd_sector(drv)) {
//    #if 0
//            if (!enable_seek) {
//                FLOPPY_DPRINTF("error: no implicit seek %d %02x %02x"
//                               " (max=%d %02x %02x)\n",
//                               head, track, sect, 1, drv.max_track,
//                               drv.last_sect);
//                return 4;
//            }
//    #endif
            drv.head = head;
            if (drv.track != track) {
                if (drv.bs != null && Block.bdrv_is_inserted(drv.bs)) {
                    drv.media_changed = false;
                }
                ret = 1;
            }
            drv.track = track;
            drv.sect = sect;
        }

        if (drv.bs == null || !Block.bdrv_is_inserted(drv.bs)) {
            ret = 2;
        }

        return ret;
    }

    /* Set drive back to track 0 */
    static private void fd_recalibrate(FDrive drv) {
        FLOPPY_DPRINTF("recalibrate\n");
        fd_seek(drv, 0, 0, 1, 1);
    }

    /* Revalidate a disk drive after a disk change */
    static private void fd_revalidate(FDrive drv) {
        IntRef nb_heads=new IntRef(0), max_track = new IntRef(0), last_sect = new IntRef(0);
        boolean ro;
        IntRef drive = new IntRef(0);
        IntRef rate = new IntRef(0);

        FLOPPY_DPRINTF("revalidate\n");
        if (drv.bs != null) {
            ro = Block.bdrv_is_read_only(drv.bs);
            pick_geometry(drv.bs, nb_heads, max_track, last_sect, drv.drive, drive, rate);
            if (!Block.bdrv_is_inserted(drv.bs)) {
                FLOPPY_DPRINTF("No disk in drive");
            } else {
                FLOPPY_DPRINTF("Floppy disk ("+nb_heads.value+" h "+max_track.value+" t "+last_sect.value+" s) "+(ro ? "ro" : "rw"));
            }
            if (nb_heads.value == 1) {
                drv.flags &= ~FDISK_DBL_SIDES;
            } else {
                drv.flags |= FDISK_DBL_SIDES;
            }
            drv.max_track = max_track.value;
            drv.last_sect = last_sect.value;
            drv.ro = ro;
            drv.drive = drive.value;
            drv.media_rate = rate.value;
        } else {
            FLOPPY_DPRINTF("No drive connected");
            drv.last_sect = 0;
            drv.max_track = 0;
            drv.flags &= ~FDISK_DBL_SIDES;
        }
    }

    /********************************************************/
    /* Intel 82078 floppy disk controller emulation          */

    static private final int FD_DIR_WRITE   = 0;
    static private final int FD_DIR_READ    = 1;
    static private final int FD_DIR_SCANE   = 2;
    static private final int FD_DIR_SCANL   = 3;
    static private final int FD_DIR_SCANH   = 4;

    static private final int FD_STATE_MULTI  = 0x01;	/* multi track flag */
    static private final int FD_STATE_FORMAT = 0x02;	/* format flag */
    static private final int FD_STATE_SEEK   = 0x04;	/* seek flag */

    static private final int FD_REG_SRA = 0x00;
    static private final int FD_REG_SRB = 0x01;
    static private final int FD_REG_DOR = 0x02;
    static private final int FD_REG_TDR = 0x03;
    static private final int FD_REG_MSR = 0x04;
    static private final int FD_REG_DSR = 0x04;
    static private final int FD_REG_FIFO = 0x05;
    static private final int FD_REG_DIR = 0x07;
    static private final int FD_REG_CCR = 0x07;

    static private final int FD_CMD_READ_TRACK = 0x02;
    static private final int FD_CMD_SPECIFY = 0x03;
    static private final int FD_CMD_SENSE_DRIVE_STATUS = 0x04;
    static private final int FD_CMD_WRITE = 0x05;
    static private final int FD_CMD_READ = 0x06;
    static private final int FD_CMD_RECALIBRATE = 0x07;
    static private final int FD_CMD_SENSE_INTERRUPT_STATUS = 0x08;
    static private final int FD_CMD_WRITE_DELETED = 0x09;
    static private final int FD_CMD_READ_ID = 0x0a;
    static private final int FD_CMD_READ_DELETED = 0x0c;
    static private final int FD_CMD_FORMAT_TRACK = 0x0d;
    static private final int FD_CMD_DUMPREG = 0x0e;
    static private final int FD_CMD_SEEK = 0x0f;
    static private final int FD_CMD_VERSION = 0x10;
    static private final int FD_CMD_SCAN_EQUAL = 0x11;
    static private final int FD_CMD_PERPENDICULAR_MODE = 0x12;
    static private final int FD_CMD_CONFIGURE = 0x13;
    static private final int FD_CMD_LOCK = 0x14;
    static private final int FD_CMD_VERIFY = 0x16;
    static private final int FD_CMD_POWERDOWN_MODE = 0x17;
    static private final int FD_CMD_PART_ID = 0x18;
    static private final int FD_CMD_SCAN_LOW_OR_EQUAL = 0x19;
    static private final int FD_CMD_SCAN_HIGH_OR_EQUAL = 0x1d;
    static private final int FD_CMD_SAVE = 0x2e;
    static private final int FD_CMD_OPTION = 0x33;
    static private final int FD_CMD_RESTORE = 0x4e;
    static private final int FD_CMD_DRIVE_SPECIFICATION_COMMAND = 0x8e;
    static private final int FD_CMD_RELATIVE_SEEK_OUT = 0x8f;
    static private final int FD_CMD_FORMAT_AND_WRITE = 0xcd;
    static private final int FD_CMD_RELATIVE_SEEK_IN = 0xcf;

    static private final int FD_CONFIG_PRETRK = 0xff; /* Pre-compensation set to track 0 */
    static private final int FD_CONFIG_FIFOTHR = 0x0f; /* FIFO threshold set to 1 byte */
    static private final int FD_CONFIG_POLL  = 0x10; /* Poll enabled */
    static private final int FD_CONFIG_EFIFO = 0x20; /* FIFO disabled */
    static private final int FD_CONFIG_EIS   = 0x40; /* No implied seeks */

    static private final int FD_SR0_DS0      = 0x01;
    static private final int FD_SR0_DS1      = 0x02;
    static private final int FD_SR0_HEAD     = 0x04;
    static private final int FD_SR0_EQPMT    = 0x10;
    static private final int FD_SR0_SEEK     = 0x20;
    static private final int FD_SR0_ABNTERM  = 0x40;
    static private final int FD_SR0_INVCMD   = 0x80;
    static private final int FD_SR0_RDYCHG   = 0xc0;

    static private final int FD_SR1_MA       = 0x01; /* Missing address mark */
    static private final int FD_SR1_NW       = 0x02; /* Not writable */
    static private final int FD_SR1_EC       = 0x80; /* End of cylinder */

    static private final int FD_SR2_SNS      = 0x04; /* Scan not satisfied */
    static private final int FD_SR2_SEH      = 0x08; /* Scan equal hit */

    static private final int FD_SRA_DIR      = 0x01;
    static private final int FD_SRA_nWP      = 0x02;
    static private final int FD_SRA_nINDX    = 0x04;
    static private final int FD_SRA_HDSEL    = 0x08;
    static private final int FD_SRA_nTRK0    = 0x10;
    static private final int FD_SRA_STEP     = 0x20;
    static private final int FD_SRA_nDRV2    = 0x40;
    static private final int FD_SRA_INTPEND  = 0x80;

    static private final int FD_SRB_MTR0     = 0x01;
    static private final int FD_SRB_MTR1     = 0x02;
    static private final int FD_SRB_WGATE    = 0x04;
    static private final int FD_SRB_RDATA    = 0x08;
    static private final int FD_SRB_WDATA    = 0x10;
    static private final int FD_SRB_DR0      = 0x20;

    static private final int FD_DOR_SELMASK;
    static private final int FD_TDR_BOOTSEL;
    static {
        if (MAX_FD == 4) {
            FD_DOR_SELMASK  = 0x03;
            FD_TDR_BOOTSEL  = 0x0c;
        } else {
            FD_DOR_SELMASK  = 0x01;
            FD_TDR_BOOTSEL  = 0x04;
        }
    }

    static private final int FD_DOR_nRESET   = 0x04;
    static private final int FD_DOR_DMAEN    = 0x08;
    static private final int FD_DOR_MOTEN0   = 0x10;
    static private final int FD_DOR_MOTEN1   = 0x20;
    static private final int FD_DOR_MOTEN2   = 0x40;
    static private final int FD_DOR_MOTEN3   = 0x80;

    static private final int FD_DSR_DRATEMASK= 0x03;
    static private final int FD_DSR_PWRDOWN  = 0x40;
    static private final int FD_DSR_SWRESET  = 0x80;

    static private final int FD_MSR_DRV0BUSY = 0x01;
    static private final int FD_MSR_DRV1BUSY = 0x02;
    static private final int FD_MSR_DRV2BUSY = 0x04;
    static private final int FD_MSR_DRV3BUSY = 0x08;
    static private final int FD_MSR_CMDBUSY  = 0x10;
    static private final int FD_MSR_NONDMA   = 0x20;
    static private final int FD_MSR_DIO      = 0x40;
    static private final int FD_MSR_RQM      = 0x80;

    static private final int FD_DIR_DSKCHG   = 0x80;

    static private boolean FD_MULTI_TRACK(int state) {
        return (state & FD_STATE_MULTI)!=0;
    }
    static private boolean FD_DID_SEEK(int state) {
        return (state & FD_STATE_SEEK)!=0;
    }
    static private boolean FD_FORMAT_CMD(int state) {
        return (state & FD_STATE_FORMAT)!=0;
    }

    static private final class FDCtrl {
        public FDCtrl() {
            for (int i=0;i<drives.length;i++)
                drives[i] = new FDrive();
        }
        //MemoryRegion iomem;
        int irq;
        /* Controller state */
        //QEMUTimer *result_timer; // replaced with Pic.PIC_AddEvent
        int dma_chann;
        /* Controller's identification */
        int version;
        /* HW */
        int sra;
        int srb;
        int dor;
        int dor_vmstate; /* only used as temp during vmstate */
        int tdr;
        int dsr;
        int msr;
        int cur_drv;
        int status0;
        int status1;
        int status2;
        /* Command FIFO */
        byte[] fifo;
        int fifo_size;
        int data_pos;
        int data_len;
        int data_state;
        int data_dir;
        int eot; /* last wanted sector */
        /* States kept only to be returned back */
        /* precompensation */
        int precomp_trk;
        int config;
        int lock;
        /* Power down config (also with status regB access mode */
        int pwrd;
        /* Floppy drives */
        int num_floppies;
        /* Sun4m quirks? */
        boolean sun4m;
        FDrive[] drives = new FDrive[MAX_FD];
        int reset_sensei;
        boolean check_media_rate;
        /* Timers state */
        int timer0;
        int timer1;
    }

//    typedef struct FDCtrlSysBus {
//        SysBusDevice busdev;
//        struct FDCtrl state;
//    } FDCtrlSysBus;
//
//    typedef struct FDCtrlISABus {
//        ISADevice busdev;
//        uint32_t iobase;
//        uint32_t irq;
//        uint32_t dma;
//        struct FDCtrl state;
//        int32_t bootindexA;
//        int32_t bootindexB;
//    } FDCtrlISABus;

    static private int fdctrl_read(Object opaque, int reg)
    {
        FDCtrl fdctrl = (FDCtrl)opaque;
        int retval;

        reg &= 7;
        switch (reg) {
        case FD_REG_SRA:
            retval = fdctrl_read_statusA(fdctrl);
            break;
        case FD_REG_SRB:
            retval = fdctrl_read_statusB(fdctrl);
            break;
        case FD_REG_DOR:
            retval = fdctrl_read_dor(fdctrl);
            break;
        case FD_REG_TDR:
            retval = fdctrl_read_tape(fdctrl);
            break;
        case FD_REG_MSR:
            retval = fdctrl_read_main_status(fdctrl);
            break;
        case FD_REG_FIFO:
            retval = fdctrl_read_data(fdctrl);
            break;
        case FD_REG_DIR:
            retval = fdctrl_read_dir(fdctrl);
            break;
        default:
            retval = -1;
            break;
        }
        FLOPPY_DPRINTF("read reg"+( reg & 7)+": 0x"+Integer.toHexString(retval));

        return retval;
    }

    static private void fdctrl_write(Object opaque, int reg, int value)
    {
        FDCtrl fdctrl = (FDCtrl)opaque;

        FLOPPY_DPRINTF("write reg"+(reg & 7)+": 0x"+Integer.toHexString(value));

        reg &= 7;
        switch (reg) {
        case FD_REG_DOR:
            fdctrl_write_dor(fdctrl, value);
            break;
        case FD_REG_TDR:
            fdctrl_write_tape(fdctrl, value);
            break;
        case FD_REG_DSR:
            fdctrl_write_rate(fdctrl, value);
            break;
        case FD_REG_FIFO:
            fdctrl_write_data(fdctrl, value);
            break;
        case FD_REG_CCR:
            fdctrl_write_ccr(fdctrl, value);
            break;
        default:
            break;
        }
    }

    static private int fdctrl_read_mem (Object opaque, int reg, int size)
    {
        return fdctrl_read(opaque, reg);
    }

    static private void fdctrl_write_mem (Object opaque, int reg, int value, int size)
    {
        fdctrl_write(opaque, reg, value);
    }

//    static const MemoryRegionOps fdctrl_mem_ops = {
//        .read = fdctrl_read_mem,
//        .write = fdctrl_write_mem,
//        .endianness = DEVICE_NATIVE_ENDIAN,
//    };
//
//    static const MemoryRegionOps fdctrl_mem_strict_ops = {
//        .read = fdctrl_read_mem,
//        .write = fdctrl_write_mem,
//        .endianness = DEVICE_NATIVE_ENDIAN,
//        .valid = {
//            .min_access_size = 1,
//            .max_access_size = 1,
//        },
//    };

    static private boolean fdrive_media_changed_needed(Object opaque) {
        FDrive drive = (FDrive)opaque;

        return (drive.bs != null && !drive.media_changed);
    }

    static private boolean fdrive_media_rate_needed(Object opaque) {
        FDrive drive = (FDrive)opaque;

        return drive.fdctrl.check_media_rate;
    }

//    static void fdctrl_external_reset_sysbus(DeviceState *d)
//    {
//        FDCtrlSysBus *sys = container_of(d, FDCtrlSysBus, busdev.qdev);
//        FDCtrl *s = &sys.state;
//
//        fdctrl_reset(s, 0);
//    }
//
//    static void fdctrl_external_reset_isa(DeviceState *d)
//    {
//        FDCtrlISABus *isa = container_of(d, FDCtrlISABus, busdev.qdev);
//        FDCtrl *s = &isa.state;
//
//        fdctrl_reset(s, 0);
//    }
//
//    static private void fdctrl_handle_tc(Object opaque, int irq, int level) {
//        //FDCtrl *s = opaque;
//
//        if (level!=0) {
//            // XXX
//            FLOPPY_DPRINTF("TC pulsed");
//        }
//    }

    /* Change IRQ state */
    static private void fdctrl_reset_irq(FDCtrl fdctrl)
    {
        if ((fdctrl.sra & FD_SRA_INTPEND)==0)
            return;
        FLOPPY_DPRINTF("Reset interrupt");
        Pic.PIC_DeActivateIRQ(fdctrl.irq);
        fdctrl.sra &= ~FD_SRA_INTPEND;
    }

    static private void fdctrl_raise_irq(FDCtrl fdctrl, int status0)
    {
        /* Sparc mutation */
        if (fdctrl.sun4m && (fdctrl.msr & FD_MSR_CMDBUSY)!=0) {
            /* XXX: not sure */
            fdctrl.msr &= ~FD_MSR_CMDBUSY;
            fdctrl.msr |= FD_MSR_RQM | FD_MSR_DIO;
            fdctrl.status0 = status0;
            return;
        }
        if ((fdctrl.sra & FD_SRA_INTPEND)==0) {
            Pic.PIC_ActivateIRQ(fdctrl.irq);
            fdctrl.sra |= FD_SRA_INTPEND;
        }

        fdctrl.reset_sensei = 0;
        fdctrl.status0 = status0;
        FLOPPY_DPRINTF("Set interrupt status to 0x"+Integer.toHexString(fdctrl.status0));
    }

    /* Reset controller */
    static private void fdctrl_reset(FDCtrl fdctrl, boolean do_irq) {
        int i;

        FLOPPY_DPRINTF("reset controller");
        fdctrl_reset_irq(fdctrl);
        /* Initialise controller */
        fdctrl.sra = 0;
        fdctrl.srb = 0xc0;
        if (fdctrl.drives[1].bs == null)
            fdctrl.sra |= FD_SRA_nDRV2;
        fdctrl.cur_drv = 0;
        fdctrl.dor = FD_DOR_nRESET;
        fdctrl.dor |= (fdctrl.dma_chann != -1) ? FD_DOR_DMAEN : 0;
        fdctrl.msr = FD_MSR_RQM;
        /* FIFO state */
        fdctrl.data_pos = 0;
        fdctrl.data_len = 0;
        fdctrl.data_state = 0;
        fdctrl.data_dir = FD_DIR_WRITE;
        for (i = 0; i < MAX_FD; i++)
            fd_recalibrate(fdctrl.drives[i]);
        fdctrl_reset_fifo(fdctrl);
        if (do_irq) {
            fdctrl_raise_irq(fdctrl, FD_SR0_RDYCHG);
            fdctrl.reset_sensei = FD_RESET_SENSEI_COUNT;
        }
    }

    static private FDrive drv0(FDCtrl fdctrl) {
        return fdctrl.drives[(fdctrl.tdr & FD_TDR_BOOTSEL) >> 2];
    }

    static private FDrive drv1(FDCtrl fdctrl) {
        if ((fdctrl.tdr & FD_TDR_BOOTSEL) < (1 << 2))
            return fdctrl.drives[1];
        else
            return fdctrl.drives[0];
    }

    static private FDrive drv2(FDCtrl fdctrl)
    {
        if ((fdctrl.tdr & FD_TDR_BOOTSEL) < (2 << 2))
            return fdctrl.drives[2];
        else
            return fdctrl.drives[1];
    }

    static private FDrive drv3(FDCtrl fdctrl)
    {
        if ((fdctrl.tdr & FD_TDR_BOOTSEL) < (3 << 2))
            return fdctrl.drives[3];
        else
            return fdctrl.drives[2];
    }

    static private FDrive get_cur_drv(FDCtrl fdctrl)
    {
        switch (fdctrl.cur_drv) {
            case 0: return drv0(fdctrl);
            case 1: return drv1(fdctrl);
            case 2: if (MAX_FD==4) return drv2(fdctrl); else return null;
            case 3: if (MAX_FD==4) return drv3(fdctrl); else return null;
            default: return null;
        }
    }

    /* Status A register : 0x00 (read-only) */
    static private int fdctrl_read_statusA(FDCtrl fdctrl)
    {
        int retval = fdctrl.sra;

        FLOPPY_DPRINTF("status register A: 0x"+Integer.toHexString(retval));

        return retval;
    }

    /* Status B register : 0x01 (read-only) */
    static private int fdctrl_read_statusB(FDCtrl fdctrl)
    {
        int retval = fdctrl.srb;

        FLOPPY_DPRINTF("status register B: 0x"+Integer.toHexString(retval));

        return retval;
    }

    /* Digital output register : 0x02 */
    static private int fdctrl_read_dor(FDCtrl fdctrl)
    {
        int retval = fdctrl.dor;

        /* Selected drive */
        retval |= fdctrl.cur_drv;
        FLOPPY_DPRINTF("digital output register: 0x"+Integer.toHexString(retval));

        return retval;
    }

    static private void fdctrl_write_dor(FDCtrl fdctrl, int value)
    {
        FLOPPY_DPRINTF("digital output register set to 0x"+Integer.toHexString(value));

        /* Motors */
        if ((value & FD_DOR_MOTEN0)!=0)
            fdctrl.srb |= FD_SRB_MTR0;
        else
            fdctrl.srb &= ~FD_SRB_MTR0;
        if ((value & FD_DOR_MOTEN1)!=0)
            fdctrl.srb |= FD_SRB_MTR1;
        else
            fdctrl.srb &= ~FD_SRB_MTR1;

        /* Drive */
        if ((value & 1)!=0)
            fdctrl.srb |= FD_SRB_DR0;
        else
            fdctrl.srb &= ~FD_SRB_DR0;

        /* Reset */
        if ((value & FD_DOR_nRESET)==0) {
            if ((fdctrl.dor & FD_DOR_nRESET)!=0) {
                FLOPPY_DPRINTF("controller enter RESET state");
            }
        } else {
            if ((fdctrl.dor & FD_DOR_nRESET)==0) {
                FLOPPY_DPRINTF("controller out of RESET state\n");
                fdctrl_reset(fdctrl, true);
                fdctrl.dsr &= ~FD_DSR_PWRDOWN;
            }
        }
        /* Selected drive */
        fdctrl.cur_drv = value & FD_DOR_SELMASK;

        fdctrl.dor = value;
    }

    /* Tape drive register : 0x03 */
    static private int fdctrl_read_tape(FDCtrl fdctrl)
    {
        int retval = fdctrl.tdr;

        FLOPPY_DPRINTF("tape drive register: 0x"+Integer.toHexString(retval));

        return retval;
    }

    static private void fdctrl_write_tape(FDCtrl fdctrl, int value)
    {
        /* Reset mode */
        if ((fdctrl.dor & FD_DOR_nRESET)==0) {
            FLOPPY_DPRINTF("Floppy controller in RESET state !");
            return;
        }
        FLOPPY_DPRINTF("tape drive register set to 0x"+Integer.toHexString(value));
        /* Disk boot selection indicator */
        fdctrl.tdr = value & FD_TDR_BOOTSEL;
        /* Tape indicators: never allow */
    }

    /* Main status register : 0x04 (read) */
    static private int fdctrl_read_main_status(FDCtrl fdctrl)
    {
        int retval = fdctrl.msr;

        fdctrl.dsr &= ~FD_DSR_PWRDOWN;
        fdctrl.dor |= FD_DOR_nRESET;

        /* Sparc mutation */
        if (fdctrl.sun4m) {
            retval |= FD_MSR_DIO;
            fdctrl_reset_irq(fdctrl);
        }

        FLOPPY_DPRINTF("main status register: 0x"+Integer.toHexString(retval));

        return retval;
    }

    /* Data select rate register : 0x04 (write) */
    static private void fdctrl_write_rate(FDCtrl fdctrl, int value)
    {
        /* Reset mode */
        if ((fdctrl.dor & FD_DOR_nRESET)==0) {
            FLOPPY_DPRINTF("Floppy controller in RESET state !");
            return;
        }
        FLOPPY_DPRINTF("select rate register set to 0x"+Integer.toHexString(value));
        /* Reset: autoclear */
        if ((value & FD_DSR_SWRESET)!=0) {
            fdctrl.dor &= ~FD_DOR_nRESET;
            fdctrl_reset(fdctrl, true);
            fdctrl.dor |= FD_DOR_nRESET;
        }
        if ((value & FD_DSR_PWRDOWN)!=0) {
            fdctrl_reset(fdctrl, true);
        }
        fdctrl.dsr = value;
    }

    /* Configuration control register: 0x07 (write) */
    static private void fdctrl_write_ccr(FDCtrl fdctrl, int value)
    {
        /* Reset mode */
        if ((fdctrl.dor & FD_DOR_nRESET)==0) {
            FLOPPY_DPRINTF("Floppy controller in RESET state !");
            return;
        }
        FLOPPY_DPRINTF("configuration control register set to 0x"+Integer.toHexString(value));

        /* Only the rate selection bits used in AT mode, and we
         * store those in the DSR.
         */
        fdctrl.dsr = (fdctrl.dsr & ~FD_DSR_DRATEMASK) |
                      (value & FD_DSR_DRATEMASK);
    }

    static private boolean fdctrl_media_changed(FDrive drv)
    {
        return drv.media_changed;
    }

    /* Digital input register : 0x07 (read-only) */
    static private int fdctrl_read_dir(FDCtrl fdctrl)
    {
        int retval = 0;

        if (fdctrl_media_changed(get_cur_drv(fdctrl))) {
            retval |= FD_DIR_DSKCHG;
        }
        if (retval != 0) {
            FLOPPY_DPRINTF("Floppy digital input register: 0x"+Integer.toHexString(retval));
        }

        return retval;
    }

    /* FIFO state control */
    static private void fdctrl_reset_fifo(FDCtrl fdctrl)
    {
        fdctrl.data_dir = FD_DIR_WRITE;
        fdctrl.data_pos = 0;
        fdctrl.msr &= ~(FD_MSR_CMDBUSY | FD_MSR_DIO);
    }

    /* Set FIFO status for the host to read */
    static private void fdctrl_set_fifo(FDCtrl fdctrl, int fifo_len, int status0)
    {
        fdctrl.data_dir = FD_DIR_READ;
        fdctrl.data_len = fifo_len;
        fdctrl.data_pos = 0;
        fdctrl.msr |= FD_MSR_CMDBUSY | FD_MSR_RQM | FD_MSR_DIO;
        if (status0!=0) {
            fdctrl_raise_irq(fdctrl, status0);
        }
    }

    /* Set an error: unimplemented/unknown command */
    static private final HandlerCallback fdctrl_unimplemented  = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            if (Log.level<= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FLOPPY,LogSeverities.LOG_ERROR,"fdc: unimplemented command 0x"+Integer.toHexString(fdctrl.fifo[0] & 0xFF));
            fdctrl.fifo[0] = (byte)FD_SR0_INVCMD;
            fdctrl_set_fifo(fdctrl, 1, 0);
        }
    };
    
    /* Seek to next sector
     * returns 0 when end of track reached (for DBL_SIDES on head 1)
     * otherwise returns 1
     */
    static private int fdctrl_seek_to_next_sect(FDCtrl fdctrl, FDrive cur_drv)
    {
        FLOPPY_DPRINTF("seek to next sector ("+cur_drv.head+" "+Integer.toHexString(cur_drv.track)+" "+Integer.toHexString(cur_drv.sect)+" => "+fd_sector(cur_drv)+")");

        /* XXX: cur_drv.sect >= cur_drv.last_sect should be an
           error in fact */
        int new_head = cur_drv.head;
        int new_track = cur_drv.track;
        int new_sect = cur_drv.sect;

        int ret = 1;

        if (new_sect >= cur_drv.last_sect ||
            new_sect == fdctrl.eot) {
            new_sect = 1;
            if (FD_MULTI_TRACK(fdctrl.data_state)) {
                if (new_head == 0 &&
                    (cur_drv.flags & FDISK_DBL_SIDES) != 0) {
                    new_head = 1;
                } else {
                    new_head = 0;
                    new_track++;
                    if ((cur_drv.flags & FDISK_DBL_SIDES) == 0) {
                        ret = 0;
                    }
                }
            } else {
                new_track++;
                ret = 0;
            }
            if (ret == 1) {
                FLOPPY_DPRINTF("seek to next track ("+new_head+" "+Integer.toHexString(new_track)+" "+Integer.toHexString(new_sect)+" => "+fd_sector(cur_drv)+")");
            }
        } else {
            new_sect++;
        }
        fd_seek(cur_drv, new_head, new_track, new_sect, 1);
        return ret;
    }

    /* Callback for transfer end (stop or abort) */
    static private void fdctrl_stop_transfer(FDCtrl fdctrl, int status0,
                                     int status1, int status2)
    {
        FDrive cur_drv;

        cur_drv = get_cur_drv(fdctrl);
        fdctrl.status0 = status0 | FD_SR0_SEEK | (cur_drv.head << 2) |
                          GET_CUR_DRV(fdctrl);

        FLOPPY_DPRINTF("transfer status: "+Integer.toHexString(status0)+" "+Integer.toHexString(status1)+" "+Integer.toHexString(status2)+" ("+Integer.toHexString(fdctrl.status0)+")");
        fdctrl.fifo[0] = (byte)fdctrl.status0;
        fdctrl.fifo[1] = (byte)status1;
        fdctrl.fifo[2] = (byte)status2;
        fdctrl.fifo[3] = (byte)cur_drv.track;
        fdctrl.fifo[4] = (byte)cur_drv.head;
        fdctrl.fifo[5] = (byte)cur_drv.sect;
        fdctrl.fifo[6] = FD_SECTOR_SC;
        fdctrl.data_dir = FD_DIR_READ;
        if ((fdctrl.msr & FD_MSR_NONDMA)==0) {
            DMA.DMA_release_DREQ(fdctrl.dma_chann);
        }
        fdctrl.msr |= FD_MSR_RQM | FD_MSR_DIO;
        fdctrl.msr &= ~FD_MSR_NONDMA;
        fdctrl_set_fifo(fdctrl, 7, fdctrl.status0);
    }

    /* Prepare a data transfer (either DMA or FIFO) */
    static private final HandlerCallback fdctrl_start_transfer  = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;
            int kh, kt, ks;
            int did_seek = 0;
    
            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            kt = fdctrl.fifo[2];
            kh = fdctrl.fifo[3];
            ks = fdctrl.fifo[4];
            FLOPPY_DPRINTF("Start transfer at "+GET_CUR_DRV(fdctrl)+" "+kh+" "+Integer.toHexString(kt)+" "+Integer.toHexString(ks)+" ("+fd_sector_calc(kh, kt, ks, cur_drv.last_sect,NUM_SIDES(cur_drv))+")");
            switch (fd_seek(cur_drv, kh, kt, ks, fdctrl.config & FD_CONFIG_EIS)) {
            case 2:
                /* sect too big */
                fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, 0x00, 0x00);
                fdctrl.fifo[3] = (byte)kt;
                fdctrl.fifo[4] = (byte)kh;
                fdctrl.fifo[5] = (byte)ks;
                return;
            case 3:
                /* track too big */
                fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, FD_SR1_EC, 0x00);
                fdctrl.fifo[3] = (byte)kt;
                fdctrl.fifo[4] = (byte)kh;
                fdctrl.fifo[5] = (byte)ks;
                return;
            case 4:
                /* No seek enabled */
                fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, 0x00, 0x00);
                fdctrl.fifo[3] = (byte)kt;
                fdctrl.fifo[4] = (byte)kh;
                fdctrl.fifo[5] = (byte)ks;
                return;
            case 1:
                did_seek = 1;
                break;
            default:
                break;
            }
    
            /* Check the data rate. If the programmed data rate does not match
             * the currently inserted medium, the operation has to fail. */
            if (fdctrl.check_media_rate &&
                (fdctrl.dsr & FD_DSR_DRATEMASK) != cur_drv.media_rate) {
                FLOPPY_DPRINTF("data rate mismatch (fdc="+(fdctrl.dsr & FD_DSR_DRATEMASK)+", media="+cur_drv.media_rate+")");
                fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, FD_SR1_MA, 0x00);
                fdctrl.fifo[3] = (byte)kt;
                fdctrl.fifo[4] = (byte)kh;
                fdctrl.fifo[5] = (byte)ks;
                return;
            }
    
            /* Set the FIFO state */
            fdctrl.data_dir = direction;
            fdctrl.data_pos = 0;
            fdctrl.msr |= FD_MSR_CMDBUSY;
            if ((fdctrl.fifo[0] & 0x80)!=0)
                fdctrl.data_state |= FD_STATE_MULTI;
            else
                fdctrl.data_state &= ~FD_STATE_MULTI;
            if (did_seek!=0)
                fdctrl.data_state |= FD_STATE_SEEK;
            else
                fdctrl.data_state &= ~FD_STATE_SEEK;
            if (fdctrl.fifo[5] == 0) {
                fdctrl.data_len = fdctrl.fifo[8];
            } else {
                int tmp;
                fdctrl.data_len = 128 << (fdctrl.fifo[5] > 7 ? 7 : fdctrl.fifo[5]);
                tmp = (fdctrl.fifo[6] - ks + 1);
                if ((fdctrl.fifo[0] & 0x80)!=0)
                    tmp += fdctrl.fifo[6];
                fdctrl.data_len *= tmp;
            }
            fdctrl.eot = fdctrl.fifo[6];
            if ((fdctrl.dor & FD_DOR_DMAEN)!=0) {
                int dma_mode;
                /* DMA transfer are enabled. Check if DMA channel is well programmed */
                dma_mode = DMA.DMA_get_channel_mode(fdctrl.dma_chann);
                dma_mode = (dma_mode >> 2) & 3;
                FLOPPY_DPRINTF("dma_mode="+dma_mode+" direction="+direction+" ("+((128 << fdctrl.fifo[5]) * (cur_drv.last_sect - ks + 1))+" - "+fdctrl.data_len+")");
                if (((direction == FD_DIR_SCANE || direction == FD_DIR_SCANL ||
                      direction == FD_DIR_SCANH) && dma_mode == 0) ||
                    (direction == FD_DIR_WRITE && dma_mode == 2) ||
                    (direction == FD_DIR_READ && dma_mode == 1)) {
                    /* No access is allowed until DMA transfer has completed */
                    fdctrl.msr &= ~FD_MSR_RQM;
                    /* Now, we just have to wait for the DMA controller to
                     * recall us...
                     */
                    DMA.DMA_hold_DREQ(fdctrl.dma_chann);
                    DMA.DMA_schedule(fdctrl.dma_chann);
                    return;
                } else {
                    FLOPPY_DPRINTF("bad dma_mode="+dma_mode+" direction="+direction);
                }
            }
            FLOPPY_DPRINTF("start non-DMA transfer\n");
            fdctrl.msr |= FD_MSR_NONDMA;
            if (direction != FD_DIR_WRITE)
                fdctrl.msr |= FD_MSR_DIO;
            /* IO based transfer: calculate len */
            fdctrl_raise_irq(fdctrl, FD_SR0_SEEK);
        }
    };
    
    /* Prepare a transfer of deleted data */
    static private final HandlerCallback fdctrl_start_transfer_del  = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            if (Log.level<= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FLOPPY,LogSeverities.LOG_ERROR,"fdctrl_start_transfer_del() unimplemented");

            /* We don't handle deleted data,
             * so we don't return *ANYTHING*
             */
            fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM | FD_SR0_SEEK, 0x00, 0x00);
        }
    };

    /* handlers for DMA transfers */
    static private final jdos.hardware.DMA.DMA_CallBack fdctrl_transfer_handler = new jdos.hardware.DMA.DMA_CallBack() {
        public void call(jdos.hardware.DMA.DmaChannel chan, int event) {
            if (event != jdos.hardware.DMA.DMAEvent.DMA_UNMASKED) return;
            int nchan = chan.channum;
            int dma_pos = chan.curraddr - chan.pagebase;
            int dma_len = chan.currcnt+1;

            FDCtrl fdctrl;
            FDrive cur_drv;
            int len, start_pos, rel_pos;
            int status0 = 0x00, status1 = 0x00, status2 = 0x00;

            fdctrl = (FDCtrl)isa;
            if ((fdctrl.msr & FD_MSR_RQM)!=0) {
                FLOPPY_DPRINTF("Not in DMA transfer mode !");
                return;
            }
            cur_drv = get_cur_drv(fdctrl);
            if (fdctrl.data_dir == FD_DIR_SCANE || fdctrl.data_dir == FD_DIR_SCANL ||
                fdctrl.data_dir == FD_DIR_SCANH)
                status2 = FD_SR2_SNS;
            if (dma_len > fdctrl.data_len)
                dma_len = fdctrl.data_len;
            if (cur_drv.bs == null) {
                if (fdctrl.data_dir == FD_DIR_WRITE)
                    fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM | FD_SR0_SEEK, 0x00, 0x00);
                else
                    fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, 0x00, 0x00);
                return;
            }
            rel_pos = fdctrl.data_pos % FD_SECTOR_LEN;
            boolean exitToEndTransfer = false;
            for (start_pos = fdctrl.data_pos; fdctrl.data_pos < dma_len && !exitToEndTransfer;) {
                len = dma_len - fdctrl.data_pos;
                if (len + rel_pos > FD_SECTOR_LEN)
                    len = FD_SECTOR_LEN - rel_pos;
                FLOPPY_DPRINTF("copy "+len+" bytes ("+dma_len+" "+fdctrl.data_pos+" "+fdctrl.data_len+") "+GET_CUR_DRV(fdctrl)+" pos "+cur_drv.head+" 0x"+Integer.toHexString(cur_drv.track)+" ("+cur_drv.sect+"-0x"+Integer.toHexString(fd_sector(cur_drv))+" 0x"+Integer.toHexString(fd_sector(cur_drv) * FD_SECTOR_LEN)+")");
                if (fdctrl.data_dir != FD_DIR_WRITE ||
                    len < FD_SECTOR_LEN || rel_pos != 0) {
                    /* READ & SCAN commands and realign to a sector for WRITE */
                    if (Block.bdrv_read(cur_drv.bs, fd_sector(cur_drv), fdctrl.fifo, 0, 1) < 0) {
                        FLOPPY_DPRINTF("Floppy: error getting sector "+ fd_sector(cur_drv));
                        /* Sure, image size is too small... */
                        java.util.Arrays.fill(fdctrl.fifo, 0, FD_SECTOR_LEN, (byte)0);
                    }
                }
                switch (fdctrl.data_dir) {
                case FD_DIR_READ:
                    /* READ commands */
                    DMA.DMA_write_memory(nchan, fdctrl.fifo, rel_pos, fdctrl.data_pos, len);
                    break;
                case FD_DIR_WRITE:
                    /* WRITE commands */
                    if (cur_drv.ro) {
                        /* Handle readonly medium early, no need to do DMA, touch the
                         * LED or attempt any writes. A real floppy doesn't attempt
                         * to write to readonly media either. */
                        fdctrl_stop_transfer(fdctrl,
                                             FD_SR0_ABNTERM | FD_SR0_SEEK, FD_SR1_NW,
                                             0x00);
                        return;
                    }

                    DMA.DMA_read_memory (nchan, fdctrl.fifo, rel_pos, fdctrl.data_pos, len);
                    if (Block.bdrv_write(cur_drv.bs, fd_sector(cur_drv), fdctrl.fifo, 0, 1) < 0) {
                        FLOPPY_DPRINTF("error writing sector "+fd_sector(cur_drv));
                        fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM | FD_SR0_SEEK, 0x00, 0x00);
                        return;
                    }
                    break;
                default:
                    /* SCAN commands */
                    {
                        byte[] tmpbuf = new byte[FD_SECTOR_LEN];
                        int ret;
                        DMA.DMA_read_memory(nchan, tmpbuf, 0, fdctrl.data_pos, len);
                        ret = -Ptr.memcmp(new Ptr(fdctrl.fifo, rel_pos), tmpbuf, len);
                        if (ret == 0) {
                            status2 = FD_SR2_SEH;
                            exitToEndTransfer = true;
                            break;
                        }
                        if ((ret < 0 && fdctrl.data_dir == FD_DIR_SCANL) ||
                            (ret > 0 && fdctrl.data_dir == FD_DIR_SCANH)) {
                            status2 = 0x00;
                            exitToEndTransfer = true;
                            break;
                        }
                    }
                    break;
                }
                fdctrl.data_pos += len;
                rel_pos = fdctrl.data_pos % FD_SECTOR_LEN;
                if (rel_pos == 0) {
                    /* Seek to next sector */
                    if (fdctrl_seek_to_next_sect(fdctrl, cur_drv)==0)
                        break;
                }
            }
            // end_transfer:
            len = fdctrl.data_pos - start_pos;
            FLOPPY_DPRINTF("end transfer "+fdctrl.data_pos+" "+len+" "+fdctrl.data_len);
            if (fdctrl.data_dir == FD_DIR_SCANE ||
                fdctrl.data_dir == FD_DIR_SCANL ||
                fdctrl.data_dir == FD_DIR_SCANH)
                status2 = FD_SR2_SEH;
            if (FD_DID_SEEK(fdctrl.data_state))
                status0 |= FD_SR0_SEEK;
            fdctrl.data_len -= len;
            fdctrl_stop_transfer(fdctrl, status0, status1, status2);
        }
    };

    /* Data register : 0x05 */
    static private int fdctrl_read_data(FDCtrl fdctrl)
    {
        FDrive cur_drv;
        int retval = 0;
        int pos;

        cur_drv = get_cur_drv(fdctrl);
        fdctrl.dsr &= ~FD_DSR_PWRDOWN;
        if ((fdctrl.msr & FD_MSR_RQM)==0 || (fdctrl.msr & FD_MSR_DIO)==0) {
            FLOPPY_DPRINTF("error: controller not ready for reading");
            return 0;
        }
        pos = fdctrl.data_pos;
        if ((fdctrl.msr & FD_MSR_NONDMA)!=0) {
            pos %= FD_SECTOR_LEN;
            if (pos == 0) {
                if (fdctrl.data_pos != 0)
                    if (fdctrl_seek_to_next_sect(fdctrl, cur_drv)==0) {
                        FLOPPY_DPRINTF("error seeking to next sector "+fd_sector(cur_drv));
                        return 0;
                    }
                if (Block.bdrv_read(cur_drv.bs, fd_sector(cur_drv), fdctrl.fifo, 0, 1) < 0) {
                    FLOPPY_DPRINTF("error getting sector "+fd_sector(cur_drv));
                    /* Sure, image size is too small... */
                    java.util.Arrays.fill(fdctrl.fifo, 0, FD_SECTOR_LEN, (byte)0);
                }
            }
        }
        retval = fdctrl.fifo[pos];
        if (++fdctrl.data_pos == fdctrl.data_len) {
            fdctrl.data_pos = 0;
            /* Switch from transfer mode to status mode
             * then from status mode to command mode
             */
            if ((fdctrl.msr & FD_MSR_NONDMA)!=0) {
                fdctrl_stop_transfer(fdctrl, FD_SR0_SEEK, 0x00, 0x00);
            } else {
                fdctrl_reset_fifo(fdctrl);
                fdctrl_reset_irq(fdctrl);
            }
        }
        FLOPPY_DPRINTF("data register: 0x"+Integer.toHexString(retval));

        return retval;
    }

    static private void fdctrl_format_sector(FDCtrl fdctrl)
    {
        FDrive cur_drv;
        int kh, kt, ks;

        SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
        cur_drv = get_cur_drv(fdctrl);
        kt = fdctrl.fifo[6];
        kh = fdctrl.fifo[7];
        ks = fdctrl.fifo[8];
        FLOPPY_DPRINTF("format sector at "+GET_CUR_DRV(fdctrl)+" "+kh+" "+Integer.toHexString(kt)+" "+Integer.toHexString(ks)+" ("+fd_sector_calc(kh, kt, ks, cur_drv.last_sect,NUM_SIDES(cur_drv))+")");
        switch (fd_seek(cur_drv, kh, kt, ks, fdctrl.config & FD_CONFIG_EIS)) {
        case 2:
            /* sect too big */
            fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, 0x00, 0x00);
            fdctrl.fifo[3] = (byte)kt;
            fdctrl.fifo[4] = (byte)kh;
            fdctrl.fifo[5] = (byte)ks;
            return;
        case 3:
            /* track too big */
            fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, FD_SR1_EC, 0x00);
            fdctrl.fifo[3] = (byte)kt;
            fdctrl.fifo[4] = (byte)kh;
            fdctrl.fifo[5] = (byte)ks;
            return;
        case 4:
            /* No seek enabled */
            fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, 0x00, 0x00);
            fdctrl.fifo[3] = (byte)kt;
            fdctrl.fifo[4] = (byte)kh;
            fdctrl.fifo[5] = (byte)ks;
            return;
        case 1:
            fdctrl.data_state |= FD_STATE_SEEK;
            break;
        default:
            break;
        }
        java.util.Arrays.fill(fdctrl.fifo, 0, FD_SECTOR_LEN, (byte)0);
        if (cur_drv.bs == null ||
            Block.bdrv_write(cur_drv.bs, fd_sector(cur_drv), fdctrl.fifo, 0, 1) < 0) {
            FLOPPY_DPRINTF("error formatting sector "+fd_sector(cur_drv));
            fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM | FD_SR0_SEEK, 0x00, 0x00);
        } else {
            if (cur_drv.sect == cur_drv.last_sect) {
                fdctrl.data_state &= ~FD_STATE_FORMAT;
                /* Last sector done */
                if (FD_DID_SEEK(fdctrl.data_state))
                    fdctrl_stop_transfer(fdctrl, FD_SR0_SEEK, 0x00, 0x00);
                else
                    fdctrl_stop_transfer(fdctrl, 0x00, 0x00, 0x00);
            } else {
                /* More to do */
                fdctrl.data_pos = 0;
                fdctrl.data_len = 4;
            }
        }
    }

    static private final HandlerCallback fdctrl_handle_lock = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            fdctrl.lock = (fdctrl.fifo[0] & 0x80)!=0 ? 1 : 0;
            fdctrl.fifo[0] = (byte)(fdctrl.lock << 4);
            fdctrl_set_fifo(fdctrl, 1, 0);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_dumpreg = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);
    
            /* Drives position */
            fdctrl.fifo[0] = (byte)drv0(fdctrl).track;
            fdctrl.fifo[1] = (byte)drv1(fdctrl).track;
            if (MAX_FD == 4) {
                fdctrl.fifo[2] = (byte)drv2(fdctrl).track;
                fdctrl.fifo[3] = (byte)drv3(fdctrl).track;
            } else {
                fdctrl.fifo[2] = 0;
                fdctrl.fifo[3] = 0;
            }
            /* timers */
            fdctrl.fifo[4] = (byte)fdctrl.timer0;
            fdctrl.fifo[5] = (byte)((fdctrl.timer1 << 1) | ((fdctrl.dor & FD_DOR_DMAEN)!=0 ? 1 : 0));
            fdctrl.fifo[6] = (byte)cur_drv.last_sect;
            fdctrl.fifo[7] = (byte)((fdctrl.lock << 7) | (cur_drv.perpendicular << 2));
            fdctrl.fifo[8] = (byte)fdctrl.config;
            fdctrl.fifo[9] = (byte)fdctrl.precomp_trk;
            fdctrl_set_fifo(fdctrl, 10, 0);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_version = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            /* Controller's version */
            fdctrl.fifo[0] = (byte)fdctrl.version;
            fdctrl_set_fifo(fdctrl, 1, 0);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_partid = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            fdctrl.fifo[0] = 0x41; /* Stepping 1 */
            fdctrl_set_fifo(fdctrl, 1, 0);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_restore = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);
    
            /* Drives position */
            drv0(fdctrl).track = fdctrl.fifo[3];
            drv1(fdctrl).track = fdctrl.fifo[4];
            if (MAX_FD == 4) {
                drv2(fdctrl).track = fdctrl.fifo[5];
                drv3(fdctrl).track = fdctrl.fifo[6];
            }
            /* timers */
            fdctrl.timer0 = fdctrl.fifo[7];
            fdctrl.timer1 = fdctrl.fifo[8];
            cur_drv.last_sect = fdctrl.fifo[9];
            fdctrl.lock = fdctrl.fifo[10] >> 7;
            cur_drv.perpendicular = (fdctrl.fifo[10] >> 2) & 0xF;
            fdctrl.config = fdctrl.fifo[11];
            fdctrl.precomp_trk = fdctrl.fifo[12];
            fdctrl.pwrd = fdctrl.fifo[13];
            fdctrl_reset_fifo(fdctrl);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_save = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);
    
            fdctrl.fifo[0] = 0;
            fdctrl.fifo[1] = 0;
            /* Drives position */
            fdctrl.fifo[2] = (byte)drv0(fdctrl).track;
            fdctrl.fifo[3] = (byte)drv1(fdctrl).track;
            if (MAX_FD == 4) {
                fdctrl.fifo[4] = (byte)drv2(fdctrl).track;
                fdctrl.fifo[5] = (byte)drv3(fdctrl).track;
            } else {
                fdctrl.fifo[4] = 0;
                fdctrl.fifo[5] = 0;
            }
            /* timers */
            fdctrl.fifo[6] = (byte)fdctrl.timer0;
            fdctrl.fifo[7] = (byte)fdctrl.timer1;
            fdctrl.fifo[8] = (byte)cur_drv.last_sect;
            fdctrl.fifo[9] = (byte)((fdctrl.lock << 7) | (cur_drv.perpendicular << 2));
            fdctrl.fifo[10] = (byte)fdctrl.config;
            fdctrl.fifo[11] = (byte)fdctrl.precomp_trk;
            fdctrl.fifo[12] = (byte)fdctrl.pwrd;
            fdctrl.fifo[13] = 0;
            fdctrl.fifo[14] = 0;
            fdctrl_set_fifo(fdctrl, 15, 0);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_readid = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);
    
            cur_drv.head = (fdctrl.fifo[1] >> 2) & 1;
            Pic.PIC_AddEvent(fdctrl_result_timer, 20);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_format_track = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;
    
            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            fdctrl.data_state |= FD_STATE_FORMAT;
            if ((fdctrl.fifo[0] & 0x80)!=0)
                fdctrl.data_state |= FD_STATE_MULTI;
            else
                fdctrl.data_state &= ~FD_STATE_MULTI;
            fdctrl.data_state &= ~FD_STATE_SEEK;
            cur_drv.bps =
                (fdctrl.fifo[2] & 0xFF) > 7 ? 16384 : 128 << (fdctrl.fifo[2] & 0xFF);
    //    #if 0
    //        cur_drv.last_sect =
    //            cur_drv.flags & FDISK_DBL_SIDES ? fdctrl.fifo[3] :
    //            fdctrl.fifo[3] / 2;
    //    #else
            cur_drv.last_sect = fdctrl.fifo[3] & 0xFF;
    //    #endif
            /* TODO: implement format using DMA expected by the Bochs BIOS
             * and Linux fdformat (read 3 bytes per sector via DMA and fill
             * the sector with the specified fill byte
             */
            fdctrl.data_state &= ~FD_STATE_FORMAT;
            fdctrl_stop_transfer(fdctrl, 0x00, 0x00, 0x00);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_specify = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            fdctrl.timer0 = (fdctrl.fifo[1] >> 4) & 0xF;
            fdctrl.timer1 = fdctrl.fifo[2] >> 1;
            if ((fdctrl.fifo[2] & 1)!=0)
                fdctrl.dor &= ~FD_DOR_DMAEN;
            else
                fdctrl.dor |= FD_DOR_DMAEN;
            /* No result back */
            fdctrl_reset_fifo(fdctrl);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_sense_drive_status = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;
    
            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            cur_drv.head = (fdctrl.fifo[1] >> 2) & 1;
            /* 1 Byte status back */
            fdctrl.fifo[0] = (byte)(((cur_drv.ro?1:0) << 6) | (cur_drv.track == 0 ? 0x10 : 0x00) | (cur_drv.head << 2) | GET_CUR_DRV(fdctrl) | 0x28);
            fdctrl_set_fifo(fdctrl, 1, 0);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_recalibrate = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;
    
            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            fd_recalibrate(cur_drv);
            fdctrl_reset_fifo(fdctrl);
            /* Raise Interrupt */
            fdctrl_raise_irq(fdctrl, FD_SR0_SEEK);
        }
    };
    
    static private final HandlerCallback fdctrl_handle_sense_interrupt_status = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);

            if (fdctrl.reset_sensei > 0) {
                fdctrl.fifo[0] = (byte)(FD_SR0_RDYCHG + FD_RESET_SENSEI_COUNT - fdctrl.reset_sensei);
                fdctrl.reset_sensei--;
            } else if ((fdctrl.sra & FD_SRA_INTPEND)==0) {
                fdctrl.fifo[0] = (byte)FD_SR0_INVCMD;
                fdctrl_set_fifo(fdctrl, 1, 0);
                return;
            } else {
                fdctrl.fifo[0] = (byte)((fdctrl.status0 & ~(FD_SR0_HEAD | FD_SR0_DS1 | FD_SR0_DS0)) | GET_CUR_DRV(fdctrl));
            }

            fdctrl.fifo[1] = (byte)cur_drv.track;
            fdctrl_set_fifo(fdctrl, 2, 0);
            fdctrl_reset_irq(fdctrl);
            fdctrl.status0 = FD_SR0_RDYCHG;
        }
    };

    static private final HandlerCallback fdctrl_handle_seek = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;

            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            fdctrl_reset_fifo(fdctrl);
            /* The seek command just sends step pulses to the drive and doesn't care if
             * there is a medium inserted of if it's banging the head against the drive.
             */
            fd_seek(cur_drv, cur_drv.head, fdctrl.fifo[2] & 0xFF, cur_drv.sect, 1);
            /* Raise Interrupt */
            fdctrl_raise_irq(fdctrl, FD_SR0_SEEK);
        }
    };

    static private final HandlerCallback fdctrl_handle_perpendicular_mode = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);

            if ((fdctrl.fifo[1] & 0x80)!=0)
                cur_drv.perpendicular = fdctrl.fifo[1] & 0x7;
            /* No result back */
            fdctrl_reset_fifo(fdctrl);
        }
    };

    static private final HandlerCallback fdctrl_handle_configure = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            fdctrl.config = fdctrl.fifo[2] & 0xFF;
            fdctrl.precomp_trk =  fdctrl.fifo[3] & 0xFF;
            /* No result back */
            fdctrl_reset_fifo(fdctrl);
        }
    };

    static private final HandlerCallback fdctrl_handle_powerdown_mode = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            fdctrl.pwrd = fdctrl.fifo[1] & 0xFF;
            fdctrl.fifo[0] = fdctrl.fifo[1];
            fdctrl_set_fifo(fdctrl, 1, 0);
        }
    };

    static private final HandlerCallback fdctrl_handle_option = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            /* No result back */
            fdctrl_reset_fifo(fdctrl);
        }
    };

    static private final HandlerCallback fdctrl_handle_drive_specification_command = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv = get_cur_drv(fdctrl);

            if ((fdctrl.fifo[fdctrl.data_pos - 1] & 0x80)!=0) {
                /* Command parameters done */
                if ((fdctrl.fifo[fdctrl.data_pos - 1] & 0x40)!=0) {
                    fdctrl.fifo[0] = fdctrl.fifo[1];
                    fdctrl.fifo[2] = 0;
                    fdctrl.fifo[3] = 0;
                    fdctrl_set_fifo(fdctrl, 4, 0);
                } else {
                    fdctrl_reset_fifo(fdctrl);
                }
            } else if (fdctrl.data_len > 7) {
                /* ERROR */
                fdctrl.fifo[0] = (byte)(0x80 | (cur_drv.head << 2) | GET_CUR_DRV(fdctrl));
                fdctrl_set_fifo(fdctrl, 1, 0);
            }
        }
    };

    static private final HandlerCallback fdctrl_handle_relative_seek_in = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;

            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            if ((fdctrl.fifo[2] & 0xFF) + cur_drv.track >= cur_drv.max_track) {
                fd_seek(cur_drv, cur_drv.head, cur_drv.max_track - 1,
                        cur_drv.sect, 1);
            } else {
                fd_seek(cur_drv, cur_drv.head,
                        cur_drv.track + (fdctrl.fifo[2] & 0xFF), cur_drv.sect, 1);
            }
            fdctrl_reset_fifo(fdctrl);
            /* Raise Interrupt */
            fdctrl_raise_irq(fdctrl, FD_SR0_SEEK);
        }
    };

    static private final HandlerCallback fdctrl_handle_relative_seek_out = new HandlerCallback() {
        public void call(FDCtrl fdctrl, int direction) {
            FDrive cur_drv;

            SET_CUR_DRV(fdctrl, fdctrl.fifo[1] & FD_DOR_SELMASK);
            cur_drv = get_cur_drv(fdctrl);
            if ((fdctrl.fifo[2] & 0xFF) > cur_drv.track) {
                fd_seek(cur_drv, cur_drv.head, 0, cur_drv.sect, 1);
            } else {
                fd_seek(cur_drv, cur_drv.head,
                        cur_drv.track - (fdctrl.fifo[2] & 0xFF), cur_drv.sect, 1);
            }
            fdctrl_reset_fifo(fdctrl);
            /* Raise Interrupt */
            fdctrl_raise_irq(fdctrl, FD_SR0_SEEK);
        }
    };

    static private interface HandlerCallback {
        public void call (FDCtrl fdctrl, int direction);
    }

    static private final class Handler {
        public Handler(int value, int mask, String name, int parameters, HandlerCallback handler) {
            this.value = value;
            this.mask = mask;
            this.name = name;
            this.parameters = parameters;
            this.handler = handler;
        }
        public Handler(int value, int mask, String name, int parameters, HandlerCallback handler, int direction) {
            this.value = value;
            this.mask = mask;
            this.name = name;
            this.parameters = parameters;
            this.handler = handler;
            this.direction = direction;
        }
        int value;
        int mask;
        String name;
        int parameters;
        HandlerCallback handler;
        int direction;
    }

    static private final Handler[] handlers = new Handler [] {
        new Handler(FD_CMD_READ, 0x1f, "READ", 8, fdctrl_start_transfer, FD_DIR_READ),
        new Handler(FD_CMD_WRITE, 0x3f, "WRITE", 8, fdctrl_start_transfer, FD_DIR_WRITE),
        new Handler(FD_CMD_SEEK, 0xff, "SEEK", 2, fdctrl_handle_seek),
        new Handler(FD_CMD_SENSE_INTERRUPT_STATUS, 0xff, "SENSE INTERRUPT STATUS", 0, fdctrl_handle_sense_interrupt_status),
        new Handler(FD_CMD_RECALIBRATE, 0xff, "RECALIBRATE", 1, fdctrl_handle_recalibrate),
        new Handler(FD_CMD_FORMAT_TRACK, 0xbf, "FORMAT TRACK", 5, fdctrl_handle_format_track),
        new Handler(FD_CMD_READ_TRACK, 0xbf, "READ TRACK", 8, fdctrl_start_transfer, FD_DIR_READ),
        new Handler(FD_CMD_RESTORE, 0xff, "RESTORE", 17, fdctrl_handle_restore), /* part of READ DELETED DATA */
        new Handler(FD_CMD_SAVE, 0xff, "SAVE", 0, fdctrl_handle_save), /* part of READ DELETED DATA */
        new Handler(FD_CMD_READ_DELETED, 0x1f, "READ DELETED DATA", 8, fdctrl_start_transfer_del, FD_DIR_READ),
        new Handler(FD_CMD_SCAN_EQUAL, 0x1f, "SCAN EQUAL", 8, fdctrl_start_transfer, FD_DIR_SCANE),
        new Handler(FD_CMD_VERIFY, 0x1f, "VERIFY", 8, fdctrl_unimplemented),
        new Handler(FD_CMD_SCAN_LOW_OR_EQUAL, 0x1f, "SCAN LOW OR EQUAL", 8, fdctrl_start_transfer, FD_DIR_SCANL),
        new Handler(FD_CMD_SCAN_HIGH_OR_EQUAL, 0x1f, "SCAN HIGH OR EQUAL", 8, fdctrl_start_transfer, FD_DIR_SCANH),
        new Handler(FD_CMD_WRITE_DELETED, 0x3f, "WRITE DELETED DATA", 8, fdctrl_start_transfer_del, FD_DIR_WRITE),
        new Handler(FD_CMD_READ_ID, 0xbf, "READ ID", 1, fdctrl_handle_readid),
        new Handler(FD_CMD_SPECIFY, 0xff, "SPECIFY", 2, fdctrl_handle_specify),
        new Handler(FD_CMD_SENSE_DRIVE_STATUS, 0xff, "SENSE DRIVE STATUS", 1, fdctrl_handle_sense_drive_status),
        new Handler(FD_CMD_PERPENDICULAR_MODE, 0xff, "PERPENDICULAR MODE", 1, fdctrl_handle_perpendicular_mode),
        new Handler(FD_CMD_CONFIGURE, 0xff, "CONFIGURE", 3, fdctrl_handle_configure),
        new Handler(FD_CMD_POWERDOWN_MODE, 0xff, "POWERDOWN MODE", 2, fdctrl_handle_powerdown_mode),
        new Handler(FD_CMD_OPTION, 0xff, "OPTION", 1, fdctrl_handle_option),
        new Handler(FD_CMD_DRIVE_SPECIFICATION_COMMAND, 0xff, "DRIVE SPECIFICATION COMMAND", 5, fdctrl_handle_drive_specification_command),
        new Handler(FD_CMD_RELATIVE_SEEK_OUT, 0xff, "RELATIVE SEEK OUT", 2, fdctrl_handle_relative_seek_out),
        new Handler(FD_CMD_FORMAT_AND_WRITE, 0xff, "FORMAT AND WRITE", 10, fdctrl_unimplemented),
        new Handler(FD_CMD_RELATIVE_SEEK_IN, 0xff, "RELATIVE SEEK IN", 2, fdctrl_handle_relative_seek_in),
        new Handler(FD_CMD_LOCK, 0x7f, "LOCK", 0, fdctrl_handle_lock),
        new Handler(FD_CMD_DUMPREG, 0xff, "DUMPREG", 0, fdctrl_handle_dumpreg),
        new Handler(FD_CMD_VERSION, 0xff, "VERSION", 0, fdctrl_handle_version),
        new Handler(FD_CMD_PART_ID, 0xff, "PART ID", 0, fdctrl_handle_partid),
        new Handler(FD_CMD_WRITE, 0x1f, "WRITE (BeOS)", 8, fdctrl_start_transfer, FD_DIR_WRITE), /* not in specification ; BeOS 4.5 bug */
        new Handler(0, 0, "unknown", 0, fdctrl_unimplemented) /* default handler */
    };
    /* Associate command to an index in the 'handlers' array */
    static private final int[] command_to_handler = new int[256];

    static private void fdctrl_write_data(FDCtrl fdctrl, int value)
    {
        FDrive cur_drv;
        int pos;

        /* Reset mode */
        if ((fdctrl.dor & FD_DOR_nRESET)==0) {
            FLOPPY_DPRINTF("Floppy controller in RESET state !");
            return;
        }
        if ((fdctrl.msr & FD_MSR_RQM)==0 || (fdctrl.msr & FD_MSR_DIO)!=0) {
            FLOPPY_DPRINTF("error: controller not ready for writing");
            return;
        }
        fdctrl.dsr &= ~FD_DSR_PWRDOWN;
        /* Is it write command time ? */
        if ((fdctrl.msr & FD_MSR_NONDMA)!=0) {
            /* FIFO data write */
            pos = fdctrl.data_pos++;
            pos %= FD_SECTOR_LEN;
            fdctrl.fifo[pos] = (byte)value;
            if (pos == FD_SECTOR_LEN - 1 ||
                fdctrl.data_pos == fdctrl.data_len) {
                cur_drv = get_cur_drv(fdctrl);
                if (Block.bdrv_write(cur_drv.bs, fd_sector(cur_drv), fdctrl.fifo, 0, 1) < 0) {
                    FLOPPY_DPRINTF("error writing sector "+fd_sector(cur_drv));
                    return;
                }
                if (fdctrl_seek_to_next_sect(fdctrl, cur_drv)==0) {
                    FLOPPY_DPRINTF("error seeking to next sector %"+fd_sector(cur_drv));
                    return;
                }
            }
            /* Switch from transfer mode to status mode
             * then from status mode to command mode
             */
            if (fdctrl.data_pos == fdctrl.data_len)
                fdctrl_stop_transfer(fdctrl, FD_SR0_SEEK, 0x00, 0x00);
            return;
        }
        if (fdctrl.data_pos == 0) {
            /* Command */
            pos = command_to_handler[value & 0xff];
            FLOPPY_DPRINTF(handlers[pos].name+" command\n");
            fdctrl.data_len = handlers[pos].parameters + 1;
            fdctrl.msr |= FD_MSR_CMDBUSY;
        }

        FLOPPY_DPRINTF("fdctrl_write_data: 0x"+Integer.toHexString(value));
        fdctrl.fifo[fdctrl.data_pos++] = (byte)value;
        if (fdctrl.data_pos == fdctrl.data_len) {
            /* We now have all parameters
             * and will be able to treat the command
             */
            if ((fdctrl.data_state & FD_STATE_FORMAT)!=0) {
                fdctrl_format_sector(fdctrl);
                return;
            }

            pos = command_to_handler[fdctrl.fifo[0] & 0xff];
            FLOPPY_DPRINTF("treat "+handlers[pos].name+" command");
            handlers[pos].handler.call(fdctrl, handlers[pos].direction);
        }
    }

    static private final Pic.PIC_EventHandler fdctrl_result_timer = new Pic.PIC_EventHandler() {
        public void call(int val) {
            FDCtrl fdctrl = isa;
            FDrive cur_drv = get_cur_drv(fdctrl);

            /* Pretend we are spinning.
             * This is needed for Coherent, which uses READ ID to check for
             * sector interleaving.
             */
            if (cur_drv.last_sect != 0) {
                cur_drv.sect = (cur_drv.sect % cur_drv.last_sect) + 1;
            }
            /* READ_ID can't automatically succeed! */
            if (fdctrl.check_media_rate &&
                (fdctrl.dsr & FD_DSR_DRATEMASK) != cur_drv.media_rate) {
                FLOPPY_DPRINTF("read id rate mismatch (fdc="+(fdctrl.dsr & FD_DSR_DRATEMASK)+", media="+cur_drv.media_rate+")");
                fdctrl_stop_transfer(fdctrl, FD_SR0_ABNTERM, FD_SR1_MA, 0x00);
            } else {
                fdctrl_stop_transfer(fdctrl, 0x00, 0x00, 0x00);
            }
        }
    };

    static private void fdctrl_change_cb(FDrive drive) {
        drive.media_changed = true;
        fd_revalidate(drive);
    }

    static private final Block.BlockDevOps fdctrl_block_ops = new Block.BlockDevOps() {
        public void change_media_cb(Object opaque, boolean load) {
            fdctrl_change_cb((FDrive)opaque);
        }

        public boolean has_change_media_cb() {
            return true;
        }

        public void eject_request_cb(Object opaque, boolean force) {
        }

        public boolean has_eject_request_cb() {
            return false;
        }

        public boolean is_tray_open(Object opaque) {
            return false;
        }

        public boolean has_is_tray_open() {
            return false;
        }

        public boolean is_medium_locked(Object opaque) {
            return false;
        }

        public boolean has_is_medium_locked() {
            return false;
        }

        public void resize_cb(Object opaque) {
        }

        public boolean has_resize_cb() {
            return false;
        }
    };

    static private void connectDrive(FDrive drive) {
        if (drive.bs!=null) {
            if (Block.bdrv_get_on_error(drive.bs, false) != Block.BlockErrorAction.BLOCK_ERR_STOP_ENOSPC) {
                if (Log.level<= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FLOPPY,LogSeverities.LOG_ERROR,"fdc doesn't support drive option werror");
                return;
            }
            if (Block.bdrv_get_on_error(drive.bs, true) != Block.BlockErrorAction.BLOCK_ERR_REPORT) {
                if (Log.level<= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FLOPPY,LogSeverities.LOG_ERROR,"fdc doesn't support drive option rerror");
                return;
            }
        }

        fdctrl_change_cb(drive);
        if (drive.bs!=null) {
            Block.bdrv_set_dev_ops(drive.bs, fdctrl_block_ops, drive);
        }
    }

    /* Init functions */
    static private int fdctrl_connect_drives(FDCtrl fdctrl)
    {
        int i;
        FDrive drive;

        for (i = 0; i < MAX_FD; i++) {
            drive = fdctrl.drives[i];
            drive.fdctrl = fdctrl;
            fd_init(drive);
            connectDrive(drive);
        }
        return 0;
    }

    static public void Attach(int index, FileIO file) {
        if (isa != null && index>=0 && index<MAX_FD) {
            Block.BlockDriverState bs = Block.bdrv_new("image");
            bs.on_read_error = Block.BlockErrorAction.BLOCK_ERR_REPORT;
            bs.on_write_error = Block.BlockErrorAction.BLOCK_ERR_STOP_ENOSPC;
            bs.drv  = new RawBlockDriver(file);
            isa.drives[index].bs = bs;
            connectDrive(isa.drives[index]);
        }
    }

    static boolean command_tables_inited = false;
    static int fdctrl_init_common(FDCtrl fdctrl)
    {
        int i, j;

        /* Fill 'command_to_handler' lookup table */
        if (!command_tables_inited) {
            command_tables_inited = true;
            for (i = handlers.length - 1; i >= 0; i--) {
                for (j = 0; j < command_to_handler.length; j++) {
                    if ((j & handlers[i].mask) == handlers[i].value) {
                        command_to_handler[j] = i;
                    }
                }
            }
        }

        FLOPPY_DPRINTF("init controller");
        fdctrl.fifo = new byte[FD_SECTOR_LEN];
        fdctrl.fifo_size = 512;
        fdctrl.version = 0x90; /* Intel 82078 controller */
        fdctrl.config = FD_CONFIG_EIS | FD_CONFIG_EFIFO; /* Implicit seek, polling & FIFO enabled */
        fdctrl.num_floppies = MAX_FD;

        if (fdctrl.dma_chann != -1)
            //DMA_register_channel(fdctrl.dma_chann, fdctrl_transfer_handler, fdctrl);
            jdos.hardware.DMA.GetDMAChannel(fdctrl.dma_chann).Register_Callback(fdctrl_transfer_handler);
        int result = fdctrl_connect_drives(fdctrl);
        fdctrl_reset(fdctrl, false);
        return result;
    }

    static public boolean isDriveReady(int index) {
        return isa.drives[index].drive != FDRIVE_DRV_NONE;
    }
    static private FDCtrl isa;

    static private IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[6];
    static private IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[6];

    private static final IoHandler.IO_ReadHandler read_handler = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return fdctrl_read(isa, port);
        }
    };

    private final static IoHandler.IO_WriteHandler write_handler  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            fdctrl_write(isa, port, val);
        }
    };

    public static void initIO() {
        if (isa!=null) {
            int base_io = 0x3f0;
            for (int i = 0; i < ReadHandler.length; i++) {
                WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
                ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
                WriteHandler[i].Install(base_io + i, write_handler, IoHandler.IO_MB);
                ReadHandler[i].Install(base_io + i, read_handler, IoHandler.IO_MB);
            }
        }
    }

    public static Section.SectionFunction Flopyy_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            Section_prop section=(Section_prop)sec;
            if (!section.Get_bool("enable"))
                return;

            isa = new FDCtrl();
            isa.dma_chann = 2;
            isa.irq = 6;
            fdctrl_init_common(isa);
        }
    };
}