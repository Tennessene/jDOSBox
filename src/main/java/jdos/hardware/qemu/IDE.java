package jdos.hardware.qemu;

/*
 * QEMU IDE disk and CD/DVD-ROM Emulator
 *
 * Copyright (c) 2003 Fabrice Bellard
 * Copyright (c) 2006 Openedhand Ltd.
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
import jdos.misc.setup.Config;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.BooleanRef;
import jdos.util.FileIO;
import jdos.util.IntRef;

public class IDE extends Internal {
    /* These values were based on a Seagate ST3500418AS but have been modified
       to make more sense in QEMU */
    static private final int[][] smart_attributes = new int[][] {
        /* id,  flags, hflags, val, wrst, raw (6 bytes), threshold */
        /* raw read error rate*/
        { 0x01, 0x03, 0x00, 0x64, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06},
        /* spin up */
        { 0x03, 0x03, 0x00, 0x64, 0x64, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        /* start stop count */
        { 0x04, 0x02, 0x00, 0x64, 0x64, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x14},
        /* remapped sectors */
        { 0x05, 0x03, 0x00, 0x64, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x24},
        /* power on hours */
        { 0x09, 0x03, 0x00, 0x64, 0x64, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        /* power cycle count */
        { 0x0c, 0x03, 0x00, 0x64, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
        /* airflow-temperature-celsius */
        { 190,  0x03, 0x00, 0x45, 0x45, 0x1f, 0x00, 0x1f, 0x1f, 0x00, 0x00, 0x32},
        /* end of list */
        { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}
    };

    static private void padstr(byte[] buffer, int offset, String src, int len)
    {
        int i, v;
        for (i = 0;i<src.length();i+=2) {
            if (src.length()>i+1) {
                 byte c = (byte)src.charAt(i+1);
                if (c==0)
                    c = ' ';
                buffer[offset*2+i]=c;
            } else {
                buffer[offset*2+i]=' ';
            }
            byte c = (byte)src.charAt(i);
            if (c==0)
                c = ' ';
            buffer[offset*2+i+1]=c;
        }
        //System.arraycopy(src.getBytes(), 0, buffer, offset*2, src.length());
        for(i = src.length(); i < len; i++) {
            buffer[i+offset*2] = ' ';
        }
    }

    static private void put_le16(byte[] b, int offset, long value) {
        b[offset*2]=(byte)(value);
	    b[offset*2+1]=(byte)((value >> 8));
    }

    static private void le16_to_cpu(byte[] b, int offset, int value) {
        b[offset*2]=(byte)(value);
	    b[offset*2+1]=(byte)((value >> 8));
    }

    static private void ide_identify(IDEState s)
    {
        IDEDevice dev = s.unit>0 ? s.bus.slave : s.bus.master;

        if (s.identify_set) {
            System.arraycopy(s.identify_data, 0, s.io_buffer, 0, s.identify_data.length);
            return;
        }

        java.util.Arrays.fill(s.io_buffer, (byte)0);
        byte[] p = s.io_buffer;
        put_le16(p, 0, 0x0040);
        put_le16(p, 1, s.cylinders);
        put_le16(p, 3, s.heads);
        put_le16(p, 4, 512 * s.sectors); /* XXX: retired, remove ? */
        put_le16(p, 5, 512); /* XXX: retired, remove ? */
        put_le16(p, 6, s.sectors);
        padstr(p, 10, s.drive_serial_str, 20); /* serial number */
        put_le16(p, 20, 3); /* XXX: retired, remove ? */
        put_le16(p, 21, 512); /* cache size in sectors */
        put_le16(p, 22, 4); /* ecc bytes */
        padstr(p, 23, s.version, 8); /* firmware version */
        padstr(p, 27, s.drive_model_str, 40); /* model */
        if (MAX_MULT_SECTORS > 1)
            put_le16(p, 47, 0x8000 | MAX_MULT_SECTORS);

        put_le16(p, 48, 1); /* dword I/O */
        put_le16(p, 49, (1 << 11) | (1 << 9) | (1 << 8)); /* DMA and LBA supported */
        put_le16(p, 51, 0x200); /* PIO transfer cycle */
        put_le16(p, 52, 0x200); /* DMA transfer cycle */
        put_le16(p, 53, 1 | (1 << 1) | (1 << 2)); /* words 54-58,64-70,88 are valid */
        put_le16(p, 54, s.cylinders);
        put_le16(p, 55, s.heads);
        put_le16(p, 56, s.sectors);
        int oldsize = s.cylinders * s.heads * s.sectors;
        put_le16(p, 57, oldsize);
        put_le16(p, 58, oldsize >>> 16);
        if (s.mult_sectors>0)
            put_le16(p, 59, 0x100 | s.mult_sectors);
        put_le16(p, 60, s.nb_sectors);
        put_le16(p, 61, s.nb_sectors >> 16);
        put_le16(p, 62, 0x07); /* single word dma0-2 supported */
        put_le16(p, 63, 0x07); /* mdma0-2 supported */
        put_le16(p, 64, 0x03); /* pio3-4 supported */
        put_le16(p, 65, 120);
        put_le16(p, 66, 120);
        put_le16(p, 67, 120);
        put_le16(p, 68, 120);
        if (dev!=null && dev.conf.discard_granularity!=0) {
            put_le16(p, 69, (1 << 14)); /* determinate TRIM behavior */
        }

        if (s.ncq_queues != 0) {
            put_le16(p, 75, s.ncq_queues - 1);
            /* NCQ supported */
            put_le16(p, 76, (1 << 8));
        }

        put_le16(p, 80, 0xf0); /* ata3 . ata6 supported */
        put_le16(p, 81, 0x16); /* conforms to ata5 */
        /* 14=NOP supported, 5=WCACHE supported, 0=SMART supported */
        put_le16(p, 82, (1 << 14) | (1 << 5) | 1);
        /* 13=flush_cache_ext,12=flush_cache,10=lba48 */
        put_le16(p, 83, (1 << 14) | (1 << 13) | (1 <<12) | (1 << 10));
        /* 14=set to 1, 8=has WWN, 1=SMART self test, 0=SMART error logging */
        if (s.wwn!=0) {
            put_le16(p, 84, (1 << 14) | (1 << 8) | 0);
        } else {
            put_le16(p, 84, (1 << 14) | 0);
        }
        /* 14 = NOP supported, 5=WCACHE enabled, 0=SMART feature set enabled */
        if (Block.bdrv_enable_write_cache(s.bs))
             put_le16(p, 85, (1 << 14) | (1 << 5) | 1);
        else
             put_le16(p, 85, (1 << 14) | 1);
        /* 13=flush_cache_ext,12=flush_cache,10=lba48 */
        put_le16(p, 86, (1 << 13) | (1 <<12) | (1 << 10));
        /* 14=set to 1, 8=has WWN, 1=SMART self test, 0=SMART error logging */
        if (s.wwn!=0) {
            put_le16(p, 87, (1 << 14) | (1 << 8) | 0);
        } else {
            put_le16(p, 87, (1 << 14) | 0);
        }
        put_le16(p, 88, 0x3f | (1 << 13)); /* udma5 set and supported */
        put_le16(p, 93, 1 | (1 << 14) | 0x2000);
        put_le16(p, 100, s.nb_sectors);
        put_le16(p, 101, s.nb_sectors >>> 16);
        put_le16(p, 102, s.nb_sectors >>> 32);
        put_le16(p, 104, s.nb_sectors >>> 48);

        if (dev!=null && dev.conf.physical_block_size!=0)
            put_le16(p, 106, 0x6000 | dev.conf.get_physical_block_exp());
        if (s.wwn!=0) {
            /* LE 16-bit words 111-108 contain 64-bit World Wide Name */
            put_le16(p, 108, s.wwn >>> 48);
            put_le16(p, 109, s.wwn >>> 32);
            put_le16(p, 110, s.wwn >>> 16);
            put_le16(p, 111, s.wwn);
        }
        if (dev!=null && dev.conf.discard_granularity!=0) {
            put_le16(p, 169, 1); /* TRIM support */
        }
        System.arraycopy(p, 0, s.identify_data, 0, s.identify_data.length);
        s.identify_set = true;
    }

    static private void ide_atapi_identify(IDEState s)
    {
        if (s.identify_set) {
            System.arraycopy(s.identify_data, 0, s.io_buffer, 0, s.identify_data.length);
            return;
        }

        java.util.Arrays.fill(s.io_buffer, (byte)0);
        byte[] p = s.io_buffer;
        /* Removable CDROM, 50us response, 12 byte packets */
        put_le16(p, 0, (2 << 14) | (5 << 8) | (1 << 7) | (2 << 5) | (0 << 0));
        padstr(p, 10, s.drive_serial_str, 20); /* serial number */
        put_le16(p, 20, 3); /* buffer type */
        put_le16(p, 21, 512); /* cache size in sectors */
        put_le16(p, 22, 4); /* ecc bytes */
        padstr(p, 23, s.version, 8); /* firmware version */
        padstr(p, 27, s.drive_model_str, 40); /* model */
        put_le16(p, 48, 1); /* dword I/O (XXX: should not be set on CDROM) */
    if (USE_DMA_CDROM) {
        put_le16(p, 49, 1 << 9 | 1 << 8); /* DMA and LBA supported */
        put_le16(p, 53, 7); /* words 64-70, 54-58, 88 valid */
        put_le16(p, 62, 7);  /* single word dma0-2 supported */
        put_le16(p, 63, 7);  /* mdma0-2 supported */
    } else {
        put_le16(p, 49, 1 << 9); /* LBA supported, no DMA */
        put_le16(p, 53, 3); /* words 64-70, 54-58 valid */
        put_le16(p, 63, 0x103); /* DMA modes XXX: may be incorrect */
    }
        put_le16(p, 64, 3); /* pio3-4 supported */
        put_le16(p, 65, 0xb4); /* minimum DMA multiword tx cycle time */
        put_le16(p, 66, 0xb4); /* recommended DMA multiword tx cycle time */
        put_le16(p, 67, 0x12c); /* minimum PIO cycle time without flow control */
        put_le16(p, 68, 0xb4); /* minimum PIO cycle time with IORDY flow control */

        put_le16(p, 71, 30); /* in ns */
        put_le16(p, 72, 30); /* in ns */

        if (s.ncq_queues!=0) {
            put_le16(p, 75, s.ncq_queues - 1);
            /* NCQ supported */
            put_le16(p, 76, (1 << 8));
        }

        put_le16(p, 80, 0x1e); /* support up to ATA/ATAPI-4 */
    if (USE_DMA_CDROM) {
        put_le16(p, 88, 0x3f | (1 << 13)); /* udma5 set and supported */
    }
        System.arraycopy(p, 0, s.identify_data, 0, s.identify_data.length);
        s.identify_set = true;
    }

    static private void ide_cfata_identify(IDEState s)
    {
        if (s.identify_set) {
            System.arraycopy(s.identify_data, 0, s.io_buffer, 0, s.identify_data.length);
            return;
        }

        java.util.Arrays.fill(s.io_buffer, (byte)0);
        byte[] p = s.io_buffer;

        long cur_sec = s.cylinders * s.heads * s.sectors;

        put_le16(p, 0, 0x848a);			/* CF Storage Card signature */
        put_le16(p, 1, s.cylinders);		/* Default cylinders */
        put_le16(p, 3, s.heads);			/* Default heads */
        put_le16(p, 6, s.sectors);		/* Default sectors per track */
        put_le16(p, 7, s.nb_sectors >> 16);	/* Sectors per card */
        put_le16(p, 8, s.nb_sectors);		/* Sectors per card */
        padstr(p, 10, s.drive_serial_str, 20); /* serial number */
        put_le16(p, 22, 0x0004);			/* ECC bytes */
        padstr(p, 23, s.version, 8);	/* Firmware Revision */
        padstr(p, 27, s.drive_model_str, 40);/* Model number */
    if (MAX_MULT_SECTORS > 1) {
        put_le16(p, 47, 0x8000 | MAX_MULT_SECTORS);
    } else {
        put_le16(p, 47, 0x0000);
    }
        put_le16(p, 49, 0x0f00);			/* Capabilities */
        put_le16(p, 51, 0x0002);			/* PIO cycle timing mode */
        put_le16(p, 52, 0x0001);			/* DMA cycle timing mode */
        put_le16(p, 53, 0x0003);			/* Translation params valid */
        put_le16(p, 54, s.cylinders);		/* Current cylinders */
        put_le16(p, 55, s.heads);			/* Current heads */
        put_le16(p, 56, s.sectors);		/* Current sectors */
        put_le16(p, 57, cur_sec);			/* Current capacity */
        put_le16(p, 58, cur_sec >>> 16);		/* Current capacity */
        if (s.mult_sectors != 0)			/* Multiple sector setting */
            put_le16(p, 59, 0x100 | s.mult_sectors);
        put_le16(p, 60, s.nb_sectors);		/* Total LBA sectors */
        put_le16(p, 61, s.nb_sectors >> 16);	/* Total LBA sectors */
        put_le16(p, 63, 0x0203);			/* Multiword DMA capability */
        put_le16(p, 64, 0x0001);			/* Flow Control PIO support */
        put_le16(p, 65, 0x0096);			/* Min. Multiword DMA cycle */
        put_le16(p, 66, 0x0096);			/* Rec. Multiword DMA cycle */
        put_le16(p, 68, 0x00b4);			/* Min. PIO cycle time */
        put_le16(p, 82, 0x400c);			/* Command Set supported */
        put_le16(p, 83, 0x7068);			/* Command Set supported */
        put_le16(p, 84, 0x4000);			/* Features supported */
        put_le16(p, 85, 0x000c);			/* Command Set enabled */
        put_le16(p, 86, 0x7044);			/* Command Set enabled */
        put_le16(p, 87, 0x4000);			/* Features enabled */
        put_le16(p, 91, 0x4060);			/* Current APM level */
        put_le16(p, 129, 0x0002);			/* Current features option */
        put_le16(p, 130, 0x0005);			/* Reassigned sectors */
        put_le16(p, 131, 0x0001);			/* Initial power mode */
        put_le16(p, 132, 0x0000);			/* User signature */
        put_le16(p, 160, 0x8100);			/* Power requirement */
        put_le16(p, 161, 0x8001);			/* CF command set */

        s.identify_set = true;
        System.arraycopy(p, 0, s.identify_data, 0, s.identify_data.length);
    }

    static private void ide_set_signature(IDEState s)
    {
        s.select &= 0xf0; /* clear head */
        /* put signature */
        s.nsector = 1;
        s.sector = 1;
        if (s.drive_kind == IDE_CD) {
            s.lcyl = 0x14;
            s.hcyl = 0xeb;
        } else if (s.bs!=null) {
            s.lcyl = 0;
            s.hcyl = 0;
        } else {
            s.lcyl = 0xff;
            s.hcyl = 0xff;
        }
    }


//    typedef struct TrimAIOCB {
//        BlockDriverAIOCB common;
//        QEMUBH *bh;
//        int ret;
//    } TrimAIOCB;
//
//    static void trim_aio_cancel(BlockDriverAIOCB *acb)
//    {
//        TrimAIOCB *iocb = container_of(acb, TrimAIOCB, common);
//
//        qemu_bh_delete(iocb.bh);
//        iocb.bh = NULL;
//        qemu_aio_release(iocb);
//    }
//
//    static AIOPool trim_aio_pool = {
//        .aiocb_size         = sizeof(TrimAIOCB),
//        .cancel             = trim_aio_cancel,
//    };
//
//    static void ide_trim_bh_cb(void *opaque)
//    {
//        TrimAIOCB *iocb = opaque;
//
//        iocb.common.cb(iocb.common.opaque, iocb.ret);
//
//        qemu_bh_delete(iocb.bh);
//        iocb.bh = NULL;
//
//        qemu_aio_release(iocb);
//    }
//
    static private DMA.DMAIOFunc ide_issue_trim = new DMA.DMAIOFunc() {
        public Block.BlockDriverAIOCB call(Block.BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector iov, int nb_sectors, Block.BlockDriverCompletionFunc cb, Object opaque) {
//        TrimAIOCB *iocb;
//        int i, j, ret;
//
//        iocb = qemu_aio_get(&trim_aio_pool, bs, cb, opaque);
//        iocb.bh = qemu_bh_new(ide_trim_bh_cb, iocb);
//        iocb.ret = 0;
//
//        for (j = 0; j < qiov.niov; j++) {
//            uint64_t *buffer = qiov.iov[j].iov_base;
//
//            for (i = 0; i < qiov.iov[j].iov_len / 8; i++) {
//                /* 6-byte LBA + 2-byte range per entry */
//                uint64_t entry = le64_to_cpu(buffer[i]);
//                uint64_t sector = entry & 0x0000ffffffffffffULL;
//                uint16_t count = entry >> 48;
//
//                if (count == 0) {
//                    break;
//                }
//
//                ret = bdrv_discard(bs, sector, count);
//                if (!iocb.ret) {
//                    iocb.ret = ret;
//                }
//            }
//        }
//
//        qemu_bh_schedule(iocb.bh);
//
//        return &iocb.common;
            return null;
        }
    };

    static private void ide_abort_command(IDEState s)
    {
        s.status = READY_STAT | ERR_STAT;
        s.error = ABRT_ERR;
    }

    /* prepare data transfer and tell what to do after */
    static public void ide_transfer_start(IDEState s, byte[] buf, int offset, int size, EndTransferFunc end_transfer_func)
    {
        s.end_transfer_func = end_transfer_func;
        s.data_ptr = buf;
        s.data_ptr_offset = offset;
        s.data_end = offset+size;
        if ((s.status & ERR_STAT)==0) {
            s.status |= DRQ_STAT;
        }
        s.bus.dma.ops.start_transfer.call(s.bus.dma);
    }

    private static final EndTransferFunc ide_transfer_stop = new EndTransferFunc() {
        public void call(IDEState s) {
            s.end_transfer_func = ide_transfer_stop;
            s.data_ptr = s.io_buffer;
            s.data_end = 0;
            s.status &= ~DRQ_STAT;
        }
    };

    static void ide_transfer_stop(IDEState s) {
        ide_transfer_stop.call(s);
    }

    static private long ide_get_sector(IDEState s)
    {
        long sector_num;
        if ((s.select & 0x40)!=0) {
            /* lba */
        if (s.lba48==0) {
            sector_num = ((s.select & 0x0f) << 24) | (s.hcyl << 16) |
            (s.lcyl << 8) | s.sector;
        } else {
            sector_num = ((long)s.hob_hcyl << 40) |
            ((long) s.hob_lcyl << 32) |
            ((long) s.hob_sector << 24) |
            ((long) s.hcyl << 16) |
            ((long) s.lcyl << 8) | s.sector;
        }
        } else {
            sector_num = ((s.hcyl << 8) | s.lcyl) * s.heads * s.sectors +
                (s.select & 0x0f) * s.sectors + (s.sector - 1);
        }
        return sector_num;
    }

    static private void ide_set_sector(IDEState s, long sector_num)
    {
        long cyl, r;
        if ((s.select & 0x40)!=0) {
            if (s.lba48==0) {
                s.select = (int)(((s.select & 0xf0) | (sector_num >>> 24)) & 0xFF);
                s.hcyl = (int)((sector_num >> 16) & 0xFF);
                s.lcyl = (int)((sector_num >> 8) & 0xFF);
                s.sector = (int)(sector_num & 0xFF);
        } else {
            s.sector = (int)(sector_num & 0xFF);
            s.lcyl = (int)((sector_num >> 8) & 0xFF);
            s.hcyl = (int)((sector_num >> 16) & 0xFF);
            s.hob_sector = (int)((sector_num >> 24) & 0xFF);
            s.hob_lcyl = (int)((sector_num >> 32) & 0xFF);
            s.hob_hcyl = (int)((sector_num >> 40) & 0xFF);
        }
        } else {
            cyl = sector_num / (s.heads * s.sectors);
            r = sector_num % (s.heads * s.sectors);
            s.hcyl = (int)((cyl >> 8) & 0xFF);
            s.lcyl = (int)(cyl & 0xFF);
            s.select = (int)((s.select & 0xf0) | ((r / s.sectors) & 0x0f) & 0xFF);
            s.sector = (int)(((r % s.sectors) + 1) & 0xFF);
        }
    }

    static private void ide_rw_error(IDEState s) {
        ide_abort_command(s);
        ide_set_irq(s.bus);
    }

    static final private Block.BlockDriverCompletionFunc ide_sector_read_cb = new Block.BlockDriverCompletionFunc() {
        public void call(Object opaque, int ret) {
            IDEState s = (IDEState)opaque;
            int n;

            s.pio_aiocb = null;
            s.status &= ~BUSY_STAT;

            Block.bdrv_acct_done(s.bs, s.acct);
            if (ret != 0) {
                if (ide_handle_rw_error(s, -ret, BM_STATUS_PIO_RETRY | BM_STATUS_RETRY_READ)) {
                    return;
                }
            }

            n = s.nsector;
            if (n > s.req_nb_sectors) {
                n = s.req_nb_sectors;
            }

            /* Allow the guest to read the io_buffer */
            ide_transfer_start(s, s.io_buffer, 0, n * Block.BDRV_SECTOR_SIZE, ide_sector_read);

            ide_set_irq(s.bus);

            ide_set_sector(s, ide_get_sector(s) + n);
            s.nsector -= n;
        }
    };

    private static final EndTransferFunc ide_sector_read = new EndTransferFunc() {
        public void call(IDEState s) {
            ide_sector_read(s);
        }
    };

    static private void ide_sector_read(IDEState s)
    {
        long sector_num;
        int n;

        s.status = READY_STAT | SEEK_STAT;
        s.error = 0; /* not needed by IDE spec, but needed by Windows */
        sector_num = ide_get_sector(s);
        n = s.nsector;

        if (n == 0) {
            ide_transfer_stop(s);
            return;
        }

        s.status |= BUSY_STAT;

        if (n > s.req_nb_sectors) {
            n = s.req_nb_sectors;
        }

        if (DEBUG_IDE) {
            System.out.println("read sector=" + sector_num);
        }

        s.iov.iov_base = s.io_buffer;
        s.iov.iov_len  = n * Block.BDRV_SECTOR_SIZE;
        QemuCommon.qemu_iovec_init_external(s.qiov, s.iov, 1);

        Block.bdrv_acct_start(s.bs, s.acct, n * Block.BDRV_SECTOR_SIZE, Block.BDRV_ACCT_READ);
        s.pio_aiocb = Block.bdrv_aio_readv(s.bs, sector_num, s.qiov, n, ide_sector_read_cb, s);
    }

    static private void dma_buf_commit(IDEState s)
    {
        DMAHelpers.qemu_sglist_destroy(s.sg);
    }

    static void ide_set_inactive(IDEState s)
    {
        s.bus.dma.aiocb = null;
        s.bus.dma.ops.set_inactive.call(s.bus.dma);
    }

    static private void ide_dma_error(IDEState s)
    {
        ide_transfer_stop(s);
        s.error = ABRT_ERR;
        s.status = READY_STAT | ERR_STAT;
        ide_set_inactive(s);
        ide_set_irq(s.bus);
    }

    static private boolean ide_handle_rw_error(IDEState s, int error, int op)
    {
        boolean is_read = (op & BM_STATUS_RETRY_READ)!=0;
        Block.BlockErrorAction action = Block.bdrv_get_on_error(s.bs, is_read);

        if (action == Block.BlockErrorAction.BLOCK_ERR_IGNORE) {
            Block.bdrv_emit_qmp_error_event(s.bs, Block.BlockQMPEventAction.BDRV_ACTION_IGNORE, is_read);
            return false;
        }

        if ((error == Error.ENOSPC && action == Block.BlockErrorAction.BLOCK_ERR_STOP_ENOSPC) || action == Block.BlockErrorAction.BLOCK_ERR_STOP_ANY) {
            s.bus.dma.ops.set_unit.call(s.bus.dma, s.unit);
            s.bus.error_status = op;
            Block.bdrv_emit_qmp_error_event(s.bs, Block.BlockQMPEventAction.BDRV_ACTION_STOP, is_read);
            Qemu.vm_stop(Runstate.RUN_STATE_IO_ERROR);
            Block.bdrv_iostatus_set_err(s.bs, error);
        } else {
            if ((op & BM_STATUS_DMA_RETRY)!=0) {
                dma_buf_commit(s);
                ide_dma_error(s);
            } else {
                ide_rw_error(s);
            }
            Block.bdrv_emit_qmp_error_event(s.bs, Block.BlockQMPEventAction.BDRV_ACTION_REPORT, is_read);
        }

        return true;
    }

    static private final Block.BlockDriverCompletionFunc ide_dma_cb = new Block.BlockDriverCompletionFunc() {
        public void call(Object opaque, int ret) {
            IDEState s = (IDEState)opaque;
            int n;
            long sector_num;

            while (true) { // eot goto
                if (ret < 0) {
                    int op = BM_STATUS_DMA_RETRY;

                    if (s.dma_cmd == ide_dma_cmd.IDE_DMA_READ)
                        op |= BM_STATUS_RETRY_READ;
                    else if (s.dma_cmd == ide_dma_cmd.IDE_DMA_TRIM)
                        op |= BM_STATUS_RETRY_TRIM;

                    if (ide_handle_rw_error(s, -ret, op)) {
                        return;
                    }
                }

                n = s.io_buffer_size >> 9;
                sector_num = ide_get_sector(s);
                if (n > 0) {
                    dma_buf_commit(s);
                    sector_num += n;
                    ide_set_sector(s, sector_num);
                    s.nsector -= n;
                }

                /* end of transfer ? */
                if (s.nsector == 0) {
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                }

                /* launch next transfer */
                n = s.nsector;
                s.io_buffer_index = 0;
                s.io_buffer_size = n * 512;
                if (s.bus.dma.ops.prepare_buf.call(s.bus.dma, s.ide_cmd_is_read()?1:0) == 0) { // :TODO: is this right? ahci and pci both lable this as isWrite
                    /* The PRDs were too short. Reset the Active bit, but don't raise an
                     * interrupt. */
                    break;
                }

                if (DEBUG_AIO) {
                    System.out.println("ide_dma_cb: sector_num="+sector_num+" n="+n+", cmd_cmd="+s.dma_cmd);
                }

                switch (s.dma_cmd) {
                case IDE_DMA_READ:
                    s.bus.dma.aiocb = DMAHelpers.dma_bdrv_read(s.bs, s.sg, sector_num, ide_dma_cb, s);
                    break;
                case IDE_DMA_WRITE:
                    s.bus.dma.aiocb = DMAHelpers.dma_bdrv_write(s.bs, s.sg, sector_num, ide_dma_cb, s);
                    break;
                case IDE_DMA_TRIM:
                    s.bus.dma.aiocb = DMAHelpers.dma_bdrv_io(s.bs, s.sg, sector_num, ide_issue_trim, ide_dma_cb, s, DMA.DMA_DIRECTION_TO_DEVICE);
                    break;
                }
                return;
            }
        //eot:
            if (s.dma_cmd == ide_dma_cmd.IDE_DMA_READ || s.dma_cmd == ide_dma_cmd.IDE_DMA_WRITE) {
                Block.bdrv_acct_done(s.bs, s.acct);
            }
            ide_set_inactive(s);
        }
    };

    static private void ide_sector_start_dma(IDEState s, ide_dma_cmd dma_cmd)
    {
        s.status = READY_STAT | SEEK_STAT | DRQ_STAT | BUSY_STAT;
        s.io_buffer_index = 0;
        s.io_buffer_size = 0;
        s.dma_cmd = dma_cmd;

        switch (dma_cmd) {
        case IDE_DMA_READ:
            Block.bdrv_acct_start(s.bs, s.acct, s.nsector * Block.BDRV_SECTOR_SIZE, Block.BDRV_ACCT_READ);
            break;
        case IDE_DMA_WRITE:
            Block.bdrv_acct_start(s.bs, s.acct, s.nsector * Block.BDRV_SECTOR_SIZE, Block.BDRV_ACCT_WRITE);
            break;
        default:
            break;
        }

        s.bus.dma.ops.start_dma.call(s.bus.dma, s, ide_dma_cb);
    }

    static private void ide_sector_write_timer_cb(Object opaque)
    {
        IDEState s = (IDEState)opaque;
        ide_set_irq(s.bus);
    }

    static private Block.BlockDriverCompletionFunc ide_sector_write_cb = new Block.BlockDriverCompletionFunc() {
        public void call(Object opaque, int ret)
        {
            IDEState s = (IDEState)opaque;
            int n;

            Block.bdrv_acct_done(s.bs, s.acct);

            s.pio_aiocb = null;
            s.status &= ~BUSY_STAT;

            if (ret != 0) {
                if (ide_handle_rw_error(s, -ret, BM_STATUS_PIO_RETRY)) {
                    return;
                }
            }

            n = s.nsector;
            if (n > s.req_nb_sectors) {
                n = s.req_nb_sectors;
            }
            s.nsector -= n;
            if (s.nsector == 0) {
                /* no more sectors to write */
                ide_transfer_stop(s);
            } else {
                int n1 = s.nsector;
                if (n1 > s.req_nb_sectors) {
                    n1 = s.req_nb_sectors;
                }
                ide_transfer_start(s, s.io_buffer, 0, n1 * Block.BDRV_SECTOR_SIZE, ide_sector_write);
            }
            ide_set_sector(s, ide_get_sector(s) + n);

//            if (win2k_install_hack && ((++s.irq_count % 16) == 0)) {
//                /* It seems there is a bug in the Windows 2000 installer HDD
//                   IDE driver which fills the disk with empty logs when the
//                   IDE write IRQ comes too early. This hack tries to correct
//                   that at the expense of slower write performances. Use this
//                   option _only_ to install Windows 2000. You must disable it
//                   for normal use. */
//                qemu_mod_timer(s.sector_write_timer, qemu_get_clock_ns(vm_clock) + (get_ticks_per_sec() / 1000));
//            } else {
                ide_set_irq(s.bus);
//            }
        }
    };

    static private EndTransferFunc ide_sector_write = new EndTransferFunc() {
        public void call(IDEState s) {
            long sector_num;
            int n;

            s.status = READY_STAT | SEEK_STAT | BUSY_STAT;
            sector_num = ide_get_sector(s);
            if (DEBUG_IDE)
                System.out.println("sector=" + sector_num);
            n = s.nsector;
            if (n > s.req_nb_sectors) {
                n = s.req_nb_sectors;
            }

            s.iov.iov_base = s.io_buffer;
            s.iov.iov_len  = n * Block.BDRV_SECTOR_SIZE;
            QemuCommon.qemu_iovec_init_external(s.qiov, s.iov, 1);

            Block.bdrv_acct_start(s.bs, s.acct, n * Block.BDRV_SECTOR_SIZE, Block.BDRV_ACCT_READ);
            s.pio_aiocb = Block.bdrv_aio_writev(s.bs, sector_num, s.qiov, n, ide_sector_write_cb, s);
        }
    };

    static private final Block.BlockDriverCompletionFunc ide_flush_cb = new Block.BlockDriverCompletionFunc() {
        public void call(Object opaque, int ret)
        {
            IDEState s = (IDEState)opaque;

            if (ret < 0) {
                /* XXX: What sector number to set here? */
                if (ide_handle_rw_error(s, -ret, BM_STATUS_RETRY_FLUSH)) {
                    return;
                }
            }

            Block.bdrv_acct_done(s.bs, s.acct);
            s.status = READY_STAT | SEEK_STAT;
            ide_set_irq(s.bus);
        }
    };

    static private void ide_flush_cache(IDEState s)
    {
        if (s.bs == null) {
            ide_flush_cb.call(s, 0);
            return;
        }

        Block.bdrv_acct_start(s.bs, s.acct, 0, Block.BDRV_ACCT_FLUSH);
        Block.bdrv_aio_flush(s.bs, ide_flush_cb, s);
    }

    static private void ide_cfata_metadata_inquiry(IDEState s) {
        java.util.Arrays.fill(s.io_buffer, (byte)0);
        byte[] p = s.io_buffer;

        long spd = ((s.mdata_size - 1) >> 9) + 1;

        put_le16(p, 0, 0x0001);			/* Data format revision */
        put_le16(p, 1, 0x0000);			/* Media property: silicon */
        put_le16(p, 2, s.media_changed);		/* Media status */
        put_le16(p, 3, s.mdata_size & 0xffff);	/* Capacity in bytes (low) */
        put_le16(p, 4, s.mdata_size >> 16);	/* Capacity in bytes (high) */
        put_le16(p, 5, spd & 0xffff);		/* Sectors per device (low) */
        put_le16(p, 6, spd >>> 16);			/* Sectors per device (high) */
    }

    static private void ide_cfata_metadata_read(IDEState s){
        if (((s.hcyl << 16) | s.lcyl) << 9 > s.mdata_size + 2) {
            s.status = ERR_STAT;
            s.error = ABRT_ERR;
            return;
        }

        java.util.Arrays.fill(s.io_buffer, (byte)0);
        byte[] p = s.io_buffer;

        put_le16(p, 0, s.media_changed);		/* Media status */

        System.arraycopy(s.mdata_storage, (((s.hcyl << 16) | s.lcyl) << 9), p, 1, (int)Math.min(Math.min(s.mdata_size - (((s.hcyl << 16) | s.lcyl) << 9), s.nsector << 9), 0x200 - 2));
    }

    static private void ide_cfata_metadata_write(IDEState s) {
        if (((s.hcyl << 16) | s.lcyl) << 9 > s.mdata_size + 2) {
            s.status = ERR_STAT;
            s.error = ABRT_ERR;
            return;
        }

        s.media_changed = 0;
        System.arraycopy(s.io_buffer, 2, s.mdata_storage, (((s.hcyl << 16) | s.lcyl) << 9), (int) Math.min(Math.min(s.mdata_size - (((s.hcyl << 16) | s.lcyl) << 9), s.nsector << 9), 0x200 - 2));
    }

    /* called when the inserted state of the media has changed */
    static private void ide_cd_change_cb(Object opaque, boolean load)
    {
        IDEState s = (IDEState)opaque;

        s.tray_open = !load;
        s.nb_sectors = Block.bdrv_get_geometry(s.bs);

        /*
         * First indicate to the guest that a CD has been removed.  That's
         * done on the next command the guest sends us.
         *
         * Then we set UNIT_ATTENTION, by which the guest will
         * detect a new CD in the drive.  See ide_atapi_cmd() for details.
         */
        s.cdrom_changed = true;
        s.events.new_media = true;
        s.events.eject_request = false;
        ide_set_irq(s.bus);
    }

    static private void ide_cd_eject_request_cb(Object opaque, boolean force)
    {
        IDEState s = (IDEState)opaque;

        s.events.eject_request = true;
        if (force) {
            s.tray_locked = false;
        }
        ide_set_irq(s.bus);
    }

    static private void ide_cmd_lba48_transform(IDEState s, int lba48) {
        s.lba48 = lba48;

        /* handle the 'magic' 0 nsector count conversion here. to avoid
         * fiddling with the rest of the read logic, we just store the
         * full sector count in .nsector and ignore .hob_nsector from now
         */
        if (s.lba48 == 0) {
            if (s.nsector == 0)
                s.nsector = 256;
        } else {
            if (s.nsector == 0 && s.hob_nsector == 0)
                s.nsector = 65536;
            else {
                int lo = s.nsector;
                int hi = s.hob_nsector;

                s.nsector = (hi << 8) | lo;
            }
        }
    }

    static private void ide_clear_hob(IDEBus bus) {
        /* any write clears HOB high bit of device control register */
        bus.ifs[0].select &= ~(1 << 7);
        bus.ifs[1].select &= ~(1 << 7);
    }

    static private void ide_ioport_write(Object opaque, int addr, int val) {
        IDEBus bus = (IDEBus)opaque;

        if (DEBUG_IDE)
            System.out.println("IDE: write addr=0x"+Integer.toHexString(addr)+" val=0x"+Integer.toHexString(val));

        addr &= 7;

        /* ignore writes to command block while busy with previous command */
        if (addr != 7 && (idebus_active_if(bus).status & (BUSY_STAT|DRQ_STAT))!=0)
            return;

        switch(addr) {
        case 0:
            break;
        case 1:
        ide_clear_hob(bus);
            /* NOTE: data is written to the two drives */
        bus.ifs[0].hob_feature = bus.ifs[0].feature;
        bus.ifs[1].hob_feature = bus.ifs[1].feature;
            bus.ifs[0].feature = val;
            bus.ifs[1].feature = val;
            break;
        case 2:
        ide_clear_hob(bus);
        bus.ifs[0].hob_nsector = bus.ifs[0].nsector;
        bus.ifs[1].hob_nsector = bus.ifs[1].nsector;
            bus.ifs[0].nsector = val;
            bus.ifs[1].nsector = val;
            break;
        case 3:
        ide_clear_hob(bus);
        bus.ifs[0].hob_sector = bus.ifs[0].sector;
        bus.ifs[1].hob_sector = bus.ifs[1].sector;
            bus.ifs[0].sector = val;
            bus.ifs[1].sector = val;
            break;
        case 4:
        ide_clear_hob(bus);
        bus.ifs[0].hob_lcyl = bus.ifs[0].lcyl;
        bus.ifs[1].hob_lcyl = bus.ifs[1].lcyl;
            bus.ifs[0].lcyl = val;
            bus.ifs[1].lcyl = val;
            break;
        case 5:
        ide_clear_hob(bus);
        bus.ifs[0].hob_hcyl = bus.ifs[0].hcyl;
        bus.ifs[1].hob_hcyl = bus.ifs[1].hcyl;
            bus.ifs[0].hcyl = val;
            bus.ifs[1].hcyl = val;
            break;
        case 6:
        /* FIXME: HOB readback uses bit 7 */
            bus.ifs[0].select = (val & ~0x10) | 0xa0;
            bus.ifs[1].select = (val | 0x10) | 0xa0;
            /* select drive */
            bus.unit = (val >> 4) & 1;
            break;
        default:
        case 7:
            /* command */
            ide_exec_cmd(bus, val);
            break;
        }
    }

    static final private byte HD_OK = (byte)(1 << IDE_HD);
    static final private byte CD_OK = (byte)(1 << IDE_CD);
    static final private byte CFA_OK = (byte)(1 << IDE_CFATA);
    static final private byte HD_CFA_OK = (byte)(HD_OK | CFA_OK);
    static final private byte ALL_OK = (byte)(HD_OK | CD_OK | CFA_OK);

    /* See ACS-2 T13/2015-D Table B.2 Command codes */
    static private byte[] ide_cmd_table = new byte[0x100];
    static {
        /* NOP not implemented, mandatory for CD */
        ide_cmd_table[CFA_REQ_EXT_ERROR_CODE]            = CFA_OK;
        ide_cmd_table[WIN_DSM]                           = ALL_OK;
        ide_cmd_table[WIN_DEVICE_RESET]                  = CD_OK;
        ide_cmd_table[WIN_RECAL]                         = HD_CFA_OK;
        ide_cmd_table[WIN_READ]                          = ALL_OK;
        ide_cmd_table[WIN_READ_ONCE]                     = ALL_OK;
        ide_cmd_table[WIN_READ_EXT]                      = HD_CFA_OK;
        ide_cmd_table[WIN_READDMA_EXT]                   = HD_CFA_OK;
        ide_cmd_table[WIN_READ_NATIVE_MAX_EXT]           = HD_CFA_OK;
        ide_cmd_table[WIN_MULTREAD_EXT]                  = HD_CFA_OK;
        ide_cmd_table[WIN_WRITE]                         = HD_CFA_OK;
        ide_cmd_table[WIN_WRITE_ONCE]                    = HD_CFA_OK;
        ide_cmd_table[WIN_WRITE_EXT]                     = HD_CFA_OK;
        ide_cmd_table[WIN_WRITEDMA_EXT]                  = HD_CFA_OK;
        ide_cmd_table[CFA_WRITE_SECT_WO_ERASE]           = CFA_OK;
        ide_cmd_table[WIN_MULTWRITE_EXT]                 = HD_CFA_OK;
        ide_cmd_table[WIN_WRITE_VERIFY]                  = HD_CFA_OK;
        ide_cmd_table[WIN_VERIFY]                        = HD_CFA_OK;
        ide_cmd_table[WIN_VERIFY_ONCE]                   = HD_CFA_OK;
        ide_cmd_table[WIN_VERIFY_EXT]                    = HD_CFA_OK;
        ide_cmd_table[WIN_SEEK]                          = HD_CFA_OK;
        ide_cmd_table[CFA_TRANSLATE_SECTOR]              = CFA_OK;
        ide_cmd_table[WIN_DIAGNOSE]                      = ALL_OK;
        ide_cmd_table[WIN_SPECIFY]                       = HD_CFA_OK;
        ide_cmd_table[WIN_STANDBYNOW2]                   = ALL_OK;
        ide_cmd_table[WIN_IDLEIMMEDIATE2]                = ALL_OK;
        ide_cmd_table[WIN_STANDBY2]                      = ALL_OK;
        ide_cmd_table[WIN_SETIDLE2]                      = ALL_OK;
        ide_cmd_table[WIN_CHECKPOWERMODE2]               = ALL_OK;
        ide_cmd_table[WIN_SLEEPNOW2]                     = ALL_OK;
        ide_cmd_table[WIN_PACKETCMD]                     = CD_OK;
        ide_cmd_table[WIN_PIDENTIFY]                     = CD_OK;
        ide_cmd_table[WIN_SMART]                         = HD_CFA_OK;
        ide_cmd_table[CFA_ACCESS_METADATA_STORAGE]       = CFA_OK;
        ide_cmd_table[CFA_ERASE_SECTORS]                 = CFA_OK;
        ide_cmd_table[WIN_MULTREAD]                      = HD_CFA_OK;
        ide_cmd_table[WIN_MULTWRITE]                     = HD_CFA_OK;
        ide_cmd_table[WIN_SETMULT]                       = HD_CFA_OK;
        ide_cmd_table[WIN_READDMA]                       = HD_CFA_OK;
        ide_cmd_table[WIN_READDMA_ONCE]                  = HD_CFA_OK;
        ide_cmd_table[WIN_WRITEDMA]                      = HD_CFA_OK;
        ide_cmd_table[WIN_WRITEDMA_ONCE]                 = HD_CFA_OK;
        ide_cmd_table[CFA_WRITE_MULTI_WO_ERASE]          = CFA_OK;
        ide_cmd_table[WIN_STANDBYNOW1]                   = ALL_OK;
        ide_cmd_table[WIN_IDLEIMMEDIATE]                 = ALL_OK;
        ide_cmd_table[WIN_STANDBY]                       = ALL_OK;
        ide_cmd_table[WIN_SETIDLE1]                      = ALL_OK;
        ide_cmd_table[WIN_CHECKPOWERMODE1]               = ALL_OK;
        ide_cmd_table[WIN_SLEEPNOW1]                     = ALL_OK;
        ide_cmd_table[WIN_FLUSH_CACHE]                   = ALL_OK;
        ide_cmd_table[WIN_FLUSH_CACHE_EXT]               = HD_CFA_OK;
        ide_cmd_table[WIN_IDENTIFY]                      = ALL_OK;
        ide_cmd_table[WIN_SETFEATURES]                   = ALL_OK;
        ide_cmd_table[IBM_SENSE_CONDITION]               = CFA_OK;
        ide_cmd_table[CFA_WEAR_LEVEL]                    = HD_CFA_OK;
        ide_cmd_table[WIN_READ_NATIVE_MAX]               = ALL_OK;
    }

    static private boolean ide_cmd_permitted(IDEState s, int cmd)
    {
        return cmd < ide_cmd_table.length && (ide_cmd_table[cmd] & (1 << s.drive_kind))!=0;
    }

    static private class AbortException extends Exception {
    }

    static private void ide_exec_cmd(IDEBus bus, int val) {
        IDEState s;
        int n;
        int lba48 = 0;

        if (DEBUG_IDE)
            System.out.println("ide: CMD=" + Integer.toHexString(val));

        s = idebus_active_if(bus);
        /* ignore commands to non existent slave */
        if (s != bus.ifs[0] && s.bs == null)
            return;

        /* Only DEVICE RESET is allowed while BSY or/and DRQ are set */
        if ((s.status & (BUSY_STAT | DRQ_STAT)) != 0 && val != WIN_DEVICE_RESET)
            return;

        try {
            if (!ide_cmd_permitted(s, val))
                throw new AbortException();
            switch (val) {
                case WIN_DSM:
                    switch (s.feature) {
                        case DSM_TRIM:
                            if (s.bs == null) {
                                throw new AbortException();
                            }
                            ide_sector_start_dma(s, ide_dma_cmd.IDE_DMA_TRIM);
                            break;
                        default:
                            throw new AbortException();
                    }
                    break;
                case WIN_IDENTIFY:
                    if (s.bs!=null && s.drive_kind != IDE_CD) {
                        if (s.drive_kind != IDE_CFATA)
                            ide_identify(s);
                        else
                            ide_cfata_identify(s);
                        s.status = READY_STAT | SEEK_STAT;
                        ide_transfer_start(s, s.io_buffer, 0, 512, ide_transfer_stop);
                    } else {
                        if (s.drive_kind == IDE_CD) {
                            ide_set_signature(s);
                        }
                        ide_abort_command(s);
                    }
                    ide_set_irq(s.bus);
                    break;
                case WIN_SPECIFY:
                case WIN_RECAL:
                    s.error = 0;
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                case WIN_SETMULT:
                    if (s.drive_kind == IDE_CFATA && s.nsector == 0) {
                        /* Disable Read and Write Multiple */
                        s.mult_sectors = 0;
                        s.status = READY_STAT | SEEK_STAT;
                    } else if ((s.nsector & 0xff) != 0 &&
                            ((s.nsector & 0xff) > MAX_MULT_SECTORS ||
                                    (s.nsector & (s.nsector - 1)) != 0)) {
                        ide_abort_command(s);
                    } else {
                        s.mult_sectors = s.nsector & 0xff;
                        s.status = READY_STAT | SEEK_STAT;
                    }
                    ide_set_irq(s.bus);
                    break;
                case WIN_VERIFY_EXT:
                    lba48 = 1;
                case WIN_VERIFY:
                case WIN_VERIFY_ONCE:
                    /* do sector number check ? */
                    ide_cmd_lba48_transform(s, lba48);
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                case WIN_READ_EXT:
                    lba48 = 1;
                case WIN_READ:
                case WIN_READ_ONCE:
                    if (s.drive_kind == IDE_CD) {
                        ide_set_signature(s); /* odd, but ATA4 8.27.5.2 requires it */
                        throw new AbortException();
                    }
                    if (s.bs == null) {
                        throw new AbortException();
                    }
                    ide_cmd_lba48_transform(s, lba48);
                    s.req_nb_sectors = 1;
                    ide_sector_read(s);
                    break;
                case WIN_WRITE_EXT:
                    lba48 = 1;
                case WIN_WRITE:
                case WIN_WRITE_ONCE:
                case CFA_WRITE_SECT_WO_ERASE:
                case WIN_WRITE_VERIFY:
                    if (s.bs == null) {
                        throw new AbortException();
                    }
                    ide_cmd_lba48_transform(s, lba48);
                    s.error = 0;
                    s.status = SEEK_STAT | READY_STAT;
                    s.req_nb_sectors = 1;
                    ide_transfer_start(s, s.io_buffer, 0, 512, ide_sector_write);
                    s.media_changed = 1;
                    break;
                case WIN_MULTREAD_EXT:
                    lba48 = 1;
                case WIN_MULTREAD:
                    if (s.bs == null) {
                        throw new AbortException();
                    }
                    if (s.mult_sectors == 0) {
                        throw new AbortException();
                    }
                    ide_cmd_lba48_transform(s, lba48);
                    s.req_nb_sectors = s.mult_sectors;
                    ide_sector_read(s);
                    break;
                case WIN_MULTWRITE_EXT:
                    lba48 = 1;
                case WIN_MULTWRITE:
                case CFA_WRITE_MULTI_WO_ERASE:
                    if (s.bs == null) {
                        throw new AbortException();
                    }
                    if (s.mult_sectors == 0) {
                        throw new AbortException();
                    }
                    ide_cmd_lba48_transform(s, lba48);
                    s.error = 0;
                    s.status = SEEK_STAT | READY_STAT;
                    s.req_nb_sectors = s.mult_sectors;
                    n = s.nsector;
                    if (n > s.req_nb_sectors)
                        n = s.req_nb_sectors;
                    ide_transfer_start(s, s.io_buffer, 0, 512 * n, ide_sector_write);
                    s.media_changed = 1;
                    break;
                case WIN_READDMA_EXT:
                    lba48 = 1;
                case WIN_READDMA:
                case WIN_READDMA_ONCE:
                    if (s.bs == null) {
                        throw new AbortException();
                    }
                    ide_cmd_lba48_transform(s, lba48);
                    ide_sector_start_dma(s, ide_dma_cmd.IDE_DMA_READ);
                    break;
                case WIN_WRITEDMA_EXT:
                    lba48 = 1;
                case WIN_WRITEDMA:
                case WIN_WRITEDMA_ONCE:
                    if (s.bs == null) {
                        throw new AbortException();
                    }
                    ide_cmd_lba48_transform(s, lba48);
                    ide_sector_start_dma(s, ide_dma_cmd.IDE_DMA_WRITE);
                    s.media_changed = 1;
                    break;
                case WIN_READ_NATIVE_MAX_EXT:
                    lba48 = 1;
                case WIN_READ_NATIVE_MAX:
                    ide_cmd_lba48_transform(s, lba48);
                    ide_set_sector(s, s.nb_sectors - 1);
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                case WIN_CHECKPOWERMODE1:
                case WIN_CHECKPOWERMODE2:
                    s.error = 0;
                    s.nsector = 0xff; /* device active or idle */
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                case WIN_SETFEATURES:
                    if (s.bs == null)
                        throw new AbortException();
                    /* XXX: valid for CDROM ? */
                    switch (s.feature) {
                        case 0x02: /* write cache enable */
                            Block.bdrv_set_enable_write_cache(s.bs, true);
                            put_le16(s.identify_data, 85, (1 << 14) | (1 << 5) | 1);
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        case 0x82: /* write cache disable */
                            Block.bdrv_set_enable_write_cache(s.bs, false);
                            put_le16(s.identify_data, 85, (1 << 14) | 1);
                            ide_flush_cache(s);
                            break;
                        case 0xcc: /* reverting to power-on defaults enable */
                        case 0x66: /* reverting to power-on defaults disable */
                        case 0xaa: /* read look-ahead enable */
                        case 0x55: /* read look-ahead disable */
                        case 0x05: /* set advanced power management mode */
                        case 0x85: /* disable advanced power management mode */
                        case 0x69: /* NOP */
                        case 0x67: /* NOP */
                        case 0x96: /* NOP */
                        case 0x9a: /* NOP */
                        case 0x42: /* enable Automatic Acoustic Mode */
                        case 0xc2: /* disable Automatic Acoustic Mode */
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        case 0x03: { /* set transfer mode */
                            {
                                int ns = s.nsector & 0x07;

                                switch (s.nsector >> 3) {
                                    case 0x00: /* pio default */
                                    case 0x01: /* pio mode */
                                        put_le16(s.identify_data, 62, 0x07);
                                        put_le16(s.identify_data, 63, 0x07);
                                        put_le16(s.identify_data, 88, 0x3f);
                                        break;
                                    case 0x02: /* sigle word dma mode*/
                                        put_le16(s.identify_data, 62, 0x07 | (1 << (ns + 8)));
                                        put_le16(s.identify_data, 63, 0x07);
                                        put_le16(s.identify_data, 88, 0x3f);
                                        break;
                                    case 0x04: /* mdma mode */
                                        put_le16(s.identify_data, 62, 0x07);
                                        put_le16(s.identify_data, 63, 0x07 | (1 << (ns + 8)));
                                        put_le16(s.identify_data, 88, 0x3f);
                                        break;
                                    case 0x08: /* udma mode */
                                        put_le16(s.identify_data, 62, 0x07);
                                        put_le16(s.identify_data, 63, 0x07);
                                        put_le16(s.identify_data, 88, 0x3f | (1 << (ns + 8)));
                                        break;
                                    default:
                                        throw new AbortException();
                                }
                            }
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        }
                        default:
                            throw new AbortException();
                    }
                    break;
                case WIN_FLUSH_CACHE:
                case WIN_FLUSH_CACHE_EXT:
                    ide_flush_cache(s);
                    break;
                case WIN_STANDBY:
                case WIN_STANDBY2:
                case WIN_STANDBYNOW1:
                case WIN_STANDBYNOW2:
                case WIN_IDLEIMMEDIATE:
                case WIN_IDLEIMMEDIATE2:
                case WIN_SETIDLE1:
                case WIN_SETIDLE2:
                case WIN_SLEEPNOW1:
                case WIN_SLEEPNOW2:
                    s.status = READY_STAT;
                    ide_set_irq(s.bus);
                    break;
                case WIN_SEEK:
                    /* XXX: Check that seek is within bounds */
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                /* ATAPI commands */
                case WIN_PIDENTIFY:
                    ide_atapi_identify(s);
                    s.status = READY_STAT | SEEK_STAT;
                    ide_transfer_start(s, s.io_buffer, 0, 512, ide_transfer_stop);
                    ide_set_irq(s.bus);
                    break;
                case WIN_DIAGNOSE:
                    ide_set_signature(s);
                    if (s.drive_kind == IDE_CD)
                        s.status = 0; /* ATAPI spec (v6) section 9.10 defines packet
                                    * devices to return a clear status register
                                    * with READY_STAT *not* set. */
                    else
                        s.status = READY_STAT | SEEK_STAT;
                    s.error = 0x01; /* Device 0 passed, Device 1 passed or not
                                  * present.
                                  */
                    ide_set_irq(s.bus);
                    break;
                case WIN_DEVICE_RESET:
                    ide_set_signature(s);
                    s.status = 0x00; /* NOTE: READY is _not_ set */
                    s.error = 0x01;
                    break;
                case WIN_PACKETCMD:
                    /* overlapping commands not supported */
                    if ((s.feature & 0x02)!=0)
                        throw new AbortException();
                    s.status = READY_STAT | SEEK_STAT;
                    s.atapi_dma = s.feature & 1;
                    s.nsector = 1;
                    ide_transfer_start(s, s.io_buffer, 0, ATAPI_PACKET_SIZE, Atapi.ide_atapi_cmd);
                    break;
                /* CF-ATA commands */
                case CFA_REQ_EXT_ERROR_CODE:
                    s.error = 0x09;    /* miscellaneous error */
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                case CFA_ERASE_SECTORS:
                case CFA_WEAR_LEVEL:
                    /* This one has the same ID as CFA_WEAR_LEVEL and is required for Windows 8 to work with AHCI */
                //case WIN_SECURITY_FREEZE_LOCK:
                    if (val == CFA_WEAR_LEVEL)
                        s.nsector = 0;
                    if (val == CFA_ERASE_SECTORS)
                        s.media_changed = 1;
                    s.error = 0x00;
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;
                case CFA_TRANSLATE_SECTOR:
                    s.error = 0x00;
                    s.status = READY_STAT | SEEK_STAT;
                    java.util.Arrays.fill(s.io_buffer, (byte)0);
                    s.io_buffer[0x00] = (byte)s.hcyl;            /* Cyl MSB */
                    s.io_buffer[0x01] = (byte)s.lcyl;            /* Cyl LSB */
                    s.io_buffer[0x02] = (byte)s.select;            /* Head */
                    s.io_buffer[0x03] = (byte)s.sector;            /* Sector */
                    s.io_buffer[0x04] = (byte)(ide_get_sector(s) >>> 16);    /* LBA MSB */
                    s.io_buffer[0x05] = (byte)(ide_get_sector(s) >>> 8);    /* LBA */
                    s.io_buffer[0x06] = (byte)(ide_get_sector(s) >> 0);    /* LBA LSB */
                    s.io_buffer[0x13] = 0x00;                /* Erase flag */
                    s.io_buffer[0x18] = 0x00;                /* Hot count */
                    s.io_buffer[0x19] = 0x00;                /* Hot count */
                    s.io_buffer[0x1a] = 0x01;                /* Hot count */
                    ide_transfer_start(s, s.io_buffer, 0, 0x200, ide_transfer_stop);
                    ide_set_irq(s.bus);
                    break;
                case CFA_ACCESS_METADATA_STORAGE:
                    switch (s.feature) {
                        case 0x02:    /* Inquiry Metadata Storage */
                            ide_cfata_metadata_inquiry(s);
                            break;
                        case 0x03:    /* Read Metadata Storage */
                            ide_cfata_metadata_read(s);
                            break;
                        case 0x04:    /* Write Metadata Storage */
                            ide_cfata_metadata_write(s);
                            break;
                        default:
                            throw new AbortException();
                    }
                    ide_transfer_start(s, s.io_buffer, 0, 0x200, ide_transfer_stop);
                    s.status = 0x00; /* NOTE: READY is _not_ set */
                    ide_set_irq(s.bus);
                    break;
                case IBM_SENSE_CONDITION:
                    switch (s.feature) {
                        case 0x01:  /* sense temperature in device */
                            s.nsector = 0x50;      /* +20 C */
                            break;
                        default:
                            throw new AbortException();
                    }
                    s.status = READY_STAT | SEEK_STAT;
                    ide_set_irq(s.bus);
                    break;

                case WIN_SMART:
                    if (s.hcyl != 0xc2 || s.lcyl != 0x4f)
                        throw new AbortException();
                    if (!s.smart_enabled && s.feature != SMART_ENABLE)
                        throw new AbortException();
                    switch (s.feature) {
                        case SMART_DISABLE:
                            s.smart_enabled = false;
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        case SMART_ENABLE:
                            s.smart_enabled = true;
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        case SMART_ATTR_AUTOSAVE:
                            switch (s.sector) {
                                case 0x00:
                                    s.smart_autosave = false;
                                    break;
                                case 0xf1:
                                    s.smart_autosave = true;
                                    break;
                                default:
                                    throw new AbortException();
                            }
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        case SMART_STATUS:
                            if (s.smart_errors==0) {
                                s.hcyl = 0xc2;
                                s.lcyl = 0x4f;
                            } else {
                                s.hcyl = 0x2c;
                                s.lcyl = 0xf4;
                            }
                            s.status = READY_STAT | SEEK_STAT;
                            ide_set_irq(s.bus);
                            break;
                        case SMART_READ_THRESH:
                            java.util.Arrays.fill(s.io_buffer, (byte)0);
                            s.io_buffer[0] = 0x01; /* smart struct version */
                            for (n = 0; n < 30; n++) {
                                if (smart_attributes[n][0] == 0)
                                    break;
                                s.io_buffer[2 + 0 + (n * 12)] = (byte)smart_attributes[n][0];
                                s.io_buffer[2 + 1 + (n * 12)] = (byte)smart_attributes[n][11];
                            }
                            for (n = 0; n < 511; n++) /* checksum */
                                s.io_buffer[511] += s.io_buffer[n];
                            s.io_buffer[511] = (byte)(0x100 - (s.io_buffer[511] & 0xFF));
                            s.status = READY_STAT | SEEK_STAT;
                            ide_transfer_start(s, s.io_buffer, 0, 0x200, ide_transfer_stop);
                            ide_set_irq(s.bus);
                            break;
                        case SMART_READ_DATA:
                            java.util.Arrays.fill(s.io_buffer, (byte)0);
                            s.io_buffer[0] = 0x01; /* smart struct version */
                            for (n = 0; n < 30; n++) {
                                if (smart_attributes[n][0] == 0) {
                                    break;
                                }
                                int i;
                                for (i = 0; i < 11; i++) {
                                    s.io_buffer[2 + i + (n * 12)] = (byte)smart_attributes[n][i];
                                }
                            }
                            s.io_buffer[362] = (byte)(0x02 | (s.smart_autosave ? 0x80 : 0x00));
                            if (s.smart_selftest_count == 0) {
                                s.io_buffer[363] = 0;
                            } else {
                                s.io_buffer[363] = s.smart_selftest_data[3 + (s.smart_selftest_count - 1) * 24];
                            }
                            s.io_buffer[364] = 0x20;
                            s.io_buffer[365] = 0x01;
                            /* offline data collection capacity: execute + self-test*/
                            s.io_buffer[367] = (1 << 4 | 1 << 3 | 1);
                            s.io_buffer[368] = 0x03; /* smart capability (1) */
                            s.io_buffer[369] = 0x00; /* smart capability (2) */
                            s.io_buffer[370] = 0x01; /* error logging supported */
                            s.io_buffer[372] = 0x02; /* minutes for poll short test */
                            s.io_buffer[373] = 0x36; /* minutes for poll ext test */
                            s.io_buffer[374] = 0x01; /* minutes for poll conveyance */

                            for (n = 0; n < 511; n++)
                                s.io_buffer[511] += s.io_buffer[n];
                            s.io_buffer[511] = (byte)(0x100 - (s.io_buffer[511] & 0xFF));
                            s.status = READY_STAT | SEEK_STAT;
                            ide_transfer_start(s, s.io_buffer, 0, 0x200, ide_transfer_stop);
                            ide_set_irq(s.bus);
                            break;
                        case SMART_READ_LOG:
                            switch (s.sector) {
                                case 0x01: /* summary smart error log */
                                    java.util.Arrays.fill(s.io_buffer, (byte)0);
                                    s.io_buffer[0] = 0x01;
                                    s.io_buffer[1] = 0x00; /* no error entries */
                                    s.io_buffer[452] = (byte)(s.smart_errors & 0xff);
                                    s.io_buffer[453] = (byte)((s.smart_errors & 0xff00) >> 8);

                                    for (n = 0; n < 511; n++)
                                        s.io_buffer[511] += s.io_buffer[n];
                                    s.io_buffer[511] = (byte)(0x100 - (s.io_buffer[511] & 0xFF));
                                    break;
                                case 0x06: /* smart self test log */
                                    java.util.Arrays.fill(s.io_buffer, (byte)0);
                                    s.io_buffer[0] = 0x01;
                                    if (s.smart_selftest_count == 0) {
                                        s.io_buffer[508] = 0;
                                    } else {
                                        s.io_buffer[508] = (byte)s.smart_selftest_count;
                                        for (n = 2; n < 506; n++)
                                            s.io_buffer[n] = s.smart_selftest_data[n];
                                    }
                                    for (n = 0; n < 511; n++)
                                        s.io_buffer[511] += s.io_buffer[n];
                                    s.io_buffer[511] = (byte)(0x100 - (s.io_buffer[511] & 0xFF));
                                    break;
                                default:
                                    throw new AbortException();
                            }
                            s.status = READY_STAT | SEEK_STAT;
                            ide_transfer_start(s, s.io_buffer, 0, 0x200, ide_transfer_stop);
                            ide_set_irq(s.bus);
                            break;
                        case SMART_EXECUTE_OFFLINE:
                            switch (s.sector) {
                                case 0: /* off-line routine */
                                case 1: /* short self test */
                                case 2: /* extended self test */
                                    s.smart_selftest_count++;
                                    if (s.smart_selftest_count > 21)
                                        s.smart_selftest_count = 0;
                                    n = 2 + (s.smart_selftest_count - 1) * 24;
                                    s.smart_selftest_data[n] = (byte)s.sector;
                                    s.smart_selftest_data[n + 1] = 0x00; /* OK and finished */
                                    s.smart_selftest_data[n + 2] = 0x34; /* hour count lsb */
                                    s.smart_selftest_data[n + 3] = 0x12; /* hour count msb */
                                    s.status = READY_STAT | SEEK_STAT;
                                    ide_set_irq(s.bus);
                                    break;
                                default:
                                    throw new AbortException();
                            }
                            break;
                        default:
                            throw new AbortException();
                    }
                    break;
                default:
                    /* should not be reachable */
                    throw new AbortException();
            }
        } catch (AbortException e) {
            ide_abort_command(s);
            ide_set_irq(s.bus);
        }
    }

    static private int ide_ioport_read(Object opaque, int addr1) {
        IDEBus bus = (IDEBus)opaque;
        IDEState s = idebus_active_if(bus);
        int addr;
        int ret, hob;

        addr = addr1 & 7;
        /* FIXME: HOB readback uses bit 7, but it's always set right now */
        //hob = s.select & (1 << 7);
        hob = 0;
        switch(addr) {
        case 0:
            ret = 0xff;
            break;
        case 1:
            if ((bus.ifs[0].bs==null && bus.ifs[1].bs==null) || (s != bus.ifs[0] && s.bs==null))
                ret = 0;
            else if (hob==0)
                ret = s.error;
        else
            ret = s.hob_feature;
            break;
        case 2:
            if (bus.ifs[0].bs == null && bus.ifs[1].bs==null)
                ret = 0;
            else if (hob==0)
                ret = s.nsector & 0xff;
        else
            ret = s.hob_nsector;
            break;
        case 3:
            if (bus.ifs[0].bs==null && bus.ifs[1].bs==null)
                ret = 0;
            else if (hob==0)
                ret = s.sector;
        else
            ret = s.hob_sector;
            break;
        case 4:
            if (bus.ifs[0].bs==null && bus.ifs[1].bs==null)
                ret = 0;
            else if (hob==0)
                ret = s.lcyl;
            else
                ret = s.hob_lcyl;
            break;
        case 5:
            if (bus.ifs[0].bs==null && bus.ifs[1].bs==null)
                ret = 0;
            else if (hob==0)
                ret = s.hcyl;
        else
            ret = s.hob_hcyl;
            break;
        case 6:
            if (bus.ifs[0].bs==null && bus.ifs[1].bs==null)
                ret = 0;
            else
                ret = s.select;
            break;
        default:
        case 7:
            if ((bus.ifs[0].bs==null && bus.ifs[1].bs==null) || (s != bus.ifs[0] && s.bs==null))
                ret = 0;
            else
                ret = s.status;
            Pic.PIC_DeActivateIRQ(bus.irq);
            break;
        }
        if (DEBUG_IDE)
            System.out.println("ide: read addr=0x"+Integer.toHexString(addr1)+" val="+Integer.toHexString(ret));
        return ret;
    }

    private static int ide_status_read(Object opaque, int addr) {
        IDEBus bus = (IDEBus)opaque;
        IDEState s = idebus_active_if(bus);
        int ret;

        if ((bus.ifs[0].bs==null && bus.ifs[1].bs==null) ||
            (s != bus.ifs[0] && s.bs==null))
            ret = 0;
        else
            ret = s.status;
        if (DEBUG_IDE)
            System.out.println("ide: read status addr=0x"+Integer.toHexString(addr)+" val="+Integer.toHexString(ret));
        return ret;
    }

    private static void ide_cmd_write(Object opaque, int addr, int val) {
        IDEBus bus = (IDEBus)opaque;
        IDEState s;
        int i;

        if (DEBUG_IDE)
            System.out.println("ide: write control addr=0x"+Integer.toHexString(addr)+" val="+Integer.toHexString(val));
        /* common for both drives */
        if ((bus.cmd & IDE_CMD_RESET)==0 && (val & IDE_CMD_RESET)!=0) {
            /* reset low to high */
            for(i = 0;i < 2; i++) {
                s = bus.ifs[i];
                s.status = BUSY_STAT | SEEK_STAT;
                s.error = 0x01;
            }
        } else if ((bus.cmd & IDE_CMD_RESET)!=0 && (val & IDE_CMD_RESET)==0) {
            /* high to low */
            for(i = 0;i < 2; i++) {
                s = bus.ifs[i];
                if (s.drive_kind == IDE_CD)
                    s.status = 0x00; /* NOTE: READY is _not_ set */
                else
                    s.status = READY_STAT | SEEK_STAT;
                ide_set_signature(s);
            }
        }
        bus.cmd = val;
    }

    /*
     * Returns true if the running PIO transfer is a PIO out (i.e. data is
     * transferred from the device to the guest), false if it's a PIO in
     */
    static private boolean ide_is_pio_out(IDEState s) {
        if (s.end_transfer_func == ide_sector_write ||
            s.end_transfer_func == Atapi.ide_atapi_cmd) {
            return false;
        } else if (s.end_transfer_func == ide_sector_read ||
                   s.end_transfer_func == ide_transfer_stop ||
                   s.end_transfer_func == Atapi.ide_atapi_cmd_reply_end ||
                   s.end_transfer_func == ide_dummy_transfer_stop) {
            return true;
        }
        Log.exit("Bad state in IDE.Core.ide_is_pio_out");
        return false;
    }

    private static void ide_data_writew(Object opaque, int addr, int val) {
        IDEBus bus = (IDEBus)opaque;
        IDEState s = idebus_active_if(bus);

        /* PIO data access allowed only when DRQ bit is set. The result of a write
         * during PIO out is indeterminate, just ignore it. */
        if ((s.status & DRQ_STAT)==0 || ide_is_pio_out(s)) {
            return;
        }

        writew(s.data_ptr, s.data_ptr_offset, val);
        s.data_ptr_offset+=2;
        if (s.data_ptr_offset >= s.data_end)
            s.end_transfer_func.call(s);
    }

    static private int ide_data_readw(Object opaque, int addr) {
        IDEBus bus = (IDEBus)opaque;
        IDEState s = idebus_active_if(bus);
        int ret;

        /* PIO data access allowed only when DRQ bit is set. The result of a read
         * during PIO in is indeterminate, return 0 and don't move forward. */
        if ((s.status & DRQ_STAT)==0 || !ide_is_pio_out(s)) {
            return 0;
        }

        ret = readw(s.data_ptr, s.data_ptr_offset);
        s.data_ptr_offset+=2;
        if (s.data_ptr_offset >= s.data_end)
            s.end_transfer_func.call(s);
        return ret;
    }

    static private void ide_data_writel(Object opaque, int addr, int val) {
        IDEBus bus = (IDEBus)opaque;
        IDEState s = idebus_active_if(bus);

        /* PIO data access allowed only when DRQ bit is set. The result of a write
         * during PIO out is indeterminate, just ignore it. */
        if ((s.status & DRQ_STAT)==0 || ide_is_pio_out(s)) {
            return;
        }

        writed(s.data_ptr, s.data_ptr_offset, val);
        s.data_ptr_offset+=4;
        if (s.data_ptr_offset >= s.data_end)
            s.end_transfer_func.call(s);
    }

    static private int ide_data_readl(Object opaque, int addr)
    {
        IDEBus bus = (IDEBus)opaque;
        IDEState s = idebus_active_if(bus);
        int ret;

        /* PIO data access allowed only when DRQ bit is set. The result of a read
         * during PIO in is indeterminate, return 0 and don't move forward. */
        if ((s.status & DRQ_STAT)==0 || !ide_is_pio_out(s)) {
            return 0;
        }

        ret = readd(s.data_ptr, s.data_ptr_offset);
        s.data_ptr_offset+=4;
        if (s.data_ptr_offset >= s.data_end)
            s.end_transfer_func.call(s);
        return ret;
    }

    final static private Internal.EndTransferFunc ide_dummy_transfer_stop = new Internal.EndTransferFunc() {
        public void call(Internal.IDEState s) {
            s.data_ptr = s.io_buffer;
            s.data_end = 0;
            s.io_buffer[0] = (byte)0xff;
            s.io_buffer[1] = (byte)0xff;
            s.io_buffer[2] = (byte)0xff;
            s.io_buffer[3] = (byte)0xff;
        }
    };

    static private void ide_reset(IDEState s)
    {
        if (DEBUG_IDE)
        System.out.println("ide: reset");

        if (s.pio_aiocb!=null) {
            Block.bdrv_aio_cancel(s.pio_aiocb);
            s.pio_aiocb = null;
        }

        if (s.drive_kind == IDE_CFATA)
            s.mult_sectors = 0;
        else
            s.mult_sectors = MAX_MULT_SECTORS;
        /* ide regs */
        s.feature = 0;
        s.error = 0;
        s.nsector = 0;
        s.sector = 0;
        s.lcyl = 0;
        s.hcyl = 0;

        /* lba48 */
        s.hob_feature = 0;
        s.hob_sector = 0;
        s.hob_nsector = 0;
        s.hob_lcyl = 0;
        s.hob_hcyl = 0;

        s.select = 0xa0;
        s.status = READY_STAT | SEEK_STAT;

        s.lba48 = 0;

        /* ATAPI specific */
        s.sense_key = 0;
        s.asc = 0;
        s.cdrom_changed = false;
        s.packet_transfer_size = 0;
        s.elementary_transfer_size = 0;
        s.io_buffer_index = 0;
        s.cd_sector_size = 0;
        s.atapi_dma = 0;
        /* ATA DMA state */
        s.io_buffer_size = 0;
        s.req_nb_sectors = 0;

        ide_set_signature(s);
        /* init the transfer handler so that 0xffff is returned on data
           accesses */
        s.end_transfer_func = ide_dummy_transfer_stop;
        ide_dummy_transfer_stop.call(s);
        s.media_changed = 0;
    }

    static private void ide_bus_reset(IDEBus bus)
    {
        bus.unit = 0;
        bus.cmd = 0;
        ide_reset(bus.ifs[0]);
        ide_reset(bus.ifs[1]);
        ide_clear_hob(bus);

        /* pending async DMA */
        if (bus.dma.aiocb != null) {
            if (DEBUG_AIO)
                System.out.println("aio_cancel");
            Block.bdrv_aio_cancel(bus.dma.aiocb);
            bus.dma.aiocb = null;
        }

        /* reset dma provider too */
        bus.dma.ops.reset.call(bus.dma);
    }

    static private boolean ide_cd_is_tray_open(Object opaque)
    {
        return ((IDEState)opaque).tray_open;
    }

    static private boolean ide_cd_is_medium_locked(Object opaque)
    {
        return ((IDEState)opaque).tray_locked;
    }

    static private final Block.BlockDevOps ide_cd_block_ops = new Block.BlockDevOps() {
        public void change_media_cb(Object opaque, boolean load) {
            ide_cd_change_cb(opaque, load);
        }

        public boolean has_change_media_cb() {
            return true;
        }

        public void eject_request_cb(Object opaque, boolean force) {
            ide_cd_eject_request_cb(opaque, force);
        }

        public boolean has_eject_request_cb() {
            return true;
        }

        public boolean is_tray_open(Object opaque) {
            return ide_cd_is_tray_open(opaque);
        }

        public boolean has_is_tray_open() {
            return true;
        }

        public boolean is_medium_locked(Object opaque) {
            return ide_cd_is_medium_locked(opaque);
        }

        public boolean has_is_medium_locked() {
            return true;
        }

        public void resize_cb(Object opaque) {
        }

        public boolean has_resize_cb() {
            return false;
        }
    };

    static private int ide_init_drive(IDEState s, Block.BlockDriverState bs, int kind,
                       String version, String serial, String model,
                       long wwn,
                       int cylinders, int heads, int secs,
                       int chs_trans)
    {
        long nb_sectors;

        s.bs = bs;
        s.drive_kind = kind;

        nb_sectors = Block.bdrv_get_geometry(bs);
        s.cylinders = cylinders;
        s.heads = heads;
        s.sectors = secs;
        s.chs_trans = chs_trans;
        s.nb_sectors = nb_sectors;
        s.wwn = wwn;
        /* The SMART values should be preserved across power cycles
           but they aren't.  */
        s.smart_enabled = true;
        s.smart_autosave = true;
        s.smart_errors = 0;
        s.smart_selftest_count = 0;
        if (kind == IDE_CD) {
            Block.bdrv_set_dev_ops(bs, ide_cd_block_ops, s);
            Block.bdrv_set_buffer_alignment(bs, 2048);
        } else {
            if (!Block.bdrv_is_inserted(s.bs)) {
                Log.log_msg("Device needs media, but drive is empty");
                return -1;
            }
            if (Block.bdrv_is_read_only(bs)) {
                Log.log_msg("Can't use a read-only drive");
                return -1;
            }
        }
        if (serial != null && serial.length()>0)
            s.drive_serial_str = serial;
        else
            s.drive_serial_str = String.valueOf(s.drive_serial);

        if (model != null && model.length()>0) {
            s.drive_model_str = model;
        } else {
            switch (kind) {
            case IDE_CD:
                s.drive_model_str = "QEMU DVD-ROM";
                break;
            case IDE_CFATA:
                s.drive_model_str = "QEMU MICRODRIVE";
                break;
            default:
                s.drive_model_str = "QEMU HARDDISK";
                break;
            }
        }

        if (version != null && version.length()>0) {
            s.version = version;
        } else {
            s.version = Config.VERSION;
        }

        ide_reset(s);
        Block.bdrv_iostatus_enable(bs);
        return 0;
    }

    static int drive_serial = 1;

    static private void ide_init1(IDEBus bus, int unit) {
        IDEState s = bus.ifs[unit];

        s.bus = bus;
        s.unit = unit;
        s.drive_serial = drive_serial++;
        /* we need at least 2k alignment for accessing CDROMs using O_DIRECT */
        s.io_buffer_total_len = IDE_DMA_BUF_SECTORS*512 + 4;
        s.io_buffer = new byte[s.io_buffer_total_len];

        s.smart_selftest_data = new byte[512];
        //s.sector_write_timer = qemu_new_timer_ns(vm_clock, ide_sector_write_timer_cb, s);
    }

    static private final class IDEDMANop extends IDEDMAOps {
        public IDEDMANop() {
            start_dma = new DMAStartFunc() {
                public void call(IDEDMA dma, IDEState s, Block.BlockDriverCompletionFunc cb) {
                }
            };
            start_transfer = new DMAFunc() {
                public int call(IDEDMA dma) {
                    return 0;
                }
            };
            prepare_buf    = new DMAIntFunc() {
                public int call(IDEDMA dma, int x) {
                    return 0;
                }
            };
            rw_buf         = prepare_buf;
            set_unit       = prepare_buf;
            add_status     = prepare_buf;
            set_inactive   = start_transfer;
            restart_cb     = new DMARestartFunc() {
                public void call(Object opaque, int x, int y) {
                }
            };
            reset          = start_transfer;
        }
    }
    static private final IDEDMAOps ide_dma_nop_ops = new IDEDMANop();
    static private final IDEDMA ide_dma_nop = new IDEDMA(ide_dma_nop_ops);

    public static void ide_init2(IDEBus bus, int irq) {
        int i;

        for(i = 0; i < 2; i++) {
            ide_init1(bus, i);
            ide_reset(bus.ifs[i]);
        }
        bus.irq = irq;
        bus.dma = ide_dma_nop;
    }

    static private final IDEBus[] idecontroller = new IDEBus[4];

    static public Block.BlockDriverState getFirstCdrom() {
        for (int i=0;i<idecontroller.length;i++) {
            IDEBus ide = idecontroller[i];
            if (ide != null) {
                if (ide.ifs[0].drive_kind == IDE_CD)
                    return ide.ifs[0].bs;
                if (ide.ifs[1].drive_kind == IDE_CD)
                    return ide.ifs[1].bs;
            }
        }
        return null;
    }

    static public IDEState getDrive(int drive) {
        if (drive == 0x80) {
            return getDrive(0, 0);
        } else if (drive == 0x81) {
            return getDrive(0, 1);
        }
        return null;
    }
    static public IDEState getDrive(int controller, int index) {
        IDEBus ide = idecontroller[controller];
        if (ide != null && ide.ifs[index].bs!=null) {
            return ide.ifs[index];
        }
        return null;
    }
    static public int getHDCount() {
        int count = 0;
        for (int i=0;i<idecontroller.length;i++) {
            IDEBus ide = idecontroller[i];
            if (ide == null) continue;
            if (ide.ifs[0].bs != null && ide.ifs[0].drive_kind == IDE_HD)
                count++;
            if (ide.ifs[1].bs != null && ide.ifs[1].drive_kind == IDE_HD)
                count++;
        }
        return count;
    }

    static private IDEBus match_ide_controller(int port) {
        int i;

        for (i=0;i < 4;i++) {
            IDEBus ide = idecontroller[i];
            if (ide == null) continue;
            if (ide.base_io != 0 && ide.base_io == (port&0xFFF8)) return ide;
            if (ide.alt_io != 0 && ide.alt_io == (port&0xFFFE)) return ide;
        }

        return null;
    }

    public final static IoHandler.IO_WriteHandler ide_ioport_write_handler  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            ide_ioport_write(ide, port, val);
        }
    };

    public final static IoHandler.IO_WriteHandler ide_data_writew_handler  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            ide_data_writew(ide, port, val);
        }
    };

    public final static IoHandler.IO_WriteHandler ide_data_writel_handler  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            ide_data_writel(ide, port, val);
        }
    };

    public static final IoHandler.IO_ReadHandler ide_ioport_read_handler = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            return ide_ioport_read(ide, port);
        }
    };

    public static final IoHandler.IO_ReadHandler ide_data_readw_handler = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            return ide_data_readw(ide, port);
        }
    };

    public static final IoHandler.IO_ReadHandler ide_data_readl_handler = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            return ide_data_readl(ide, port);
        }
    };

    public static final IoHandler.IO_ReadHandler ide_status_read_handler = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            return ide_status_read(ide, port);
        }
    };

    public final static IoHandler.IO_WriteHandler ide_cmd_write_handler  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            IDEBus ide = match_ide_controller(port);
            ide_cmd_write(ide, port, val);
        }
    };

    public static Section.SectionFunction IDE_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            /* TODO: Free each IDE object */
        }
    };

    static void IDE_Init(Section sec,int i,String tag) {
        Section_prop section=(Section_prop)sec;
        if (!section.Get_bool(tag))
            return;

        idecontroller[i] = new IDEBus(sec,i);
    }

    static public IDEBus getIDEController(int index) {
        return idecontroller[index];
    }

    static public void IDE_Auto(IntRef index,BooleanRef slave) {
        IDEBus c;
        int i;

        index.value = -1;
        slave.value = false;
        for (i=0;i < idecontroller.length;i++) {
            if ((c=idecontroller[i]) == null) continue;
            index.value = (i >> 1);

            if (c.ifs[0].bs == null) {
                slave.value = false;
                break;
            }
            if (c.ifs[1].bs == null) {
                slave.value = true;
                break;
            }
        }
    }

    static private int hd_bios_chs_auto_trans(long cyls, long heads, long secs) {
        return cyls <= 1024 && heads <= 16 && secs <= 63 ? Block.BIOS_ATA_TRANSLATION_NONE : Block.BIOS_ATA_TRANSLATION_LBA;
    }

    /* bios_disk_index = index into BIOS INT 13h disk array: imageDisk *imageDiskList[MAX_DISK_IMAGES]; */
    static public void IDE_Attach(boolean isCD, int index,boolean slave, FileIO file, int hintCylinders, int hintHeads, int hintSector) {
        if (index < 0 || index >= idecontroller.length) return;
        IDEBus c = idecontroller[index];
        if (c == null) return;

        if (c.ifs[slave?1:0].bs != null) {
            System.out.println("IDE: Controller "+index+" "+(slave?"slave":"master")+" already taken");
            return;
        }

        Block.BlockDriverState bs = Block.bdrv_new("image");
        RawBlockDriver drv = new RawBlockDriver(file);
        Block.bdrv_open(bs, "dummy", Block.BDRV_O_RDWR, drv);
        IntRef cylinders = new IntRef(hintCylinders);
        IntRef heads = new IntRef(hintHeads);
        IntRef sectors  = new IntRef(hintSector);
        IntRef trans = new IntRef(Block.BIOS_ATA_TRANSLATION_AUTO);
        //if (cylinders.value == 0 && heads.value == 0 && sectors.value==0) {
            HdGeometry.hd_geometry_guess(bs, cylinders, heads, sectors, trans);
        //} else if (trans.value == Block.BIOS_ATA_TRANSLATION_AUTO) {
        //    trans.value = hd_bios_chs_auto_trans(cylinders.value, heads.value, sectors.value);
        //}
        // :TODO: this isn't right yet for larger hard drives
        /*
        if (cylinders.value < 1 || cylinders.value > 65535) {
            Log.exit("cyls must be between 1 and 65535");
        }
        if (heads.value < 1 || heads.value > 16) {
            Log.exit("heads must be between 1 and 16");
        }
        if (sectors.value < 1 || sectors.value > 255) {
            Log.exit("secs must be between 1 and 255");
        }
        */
        ide_init_drive(c.ifs[slave ? 1 : 0], bs, isCD?IDE_CD:IDE_HD, "version", "serial", null, 0, cylinders.value, heads.value, sectors.value, trans.value);
    }

    public static Section.SectionFunction IDE_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            IDE_Init(sec, 0, "primary");
            IDE_Init(sec, 1, "secondary");
            IDE_Init(sec, 2, "tertiary");
            IDE_Init(sec,3, " quaternary");
        }
    };
}