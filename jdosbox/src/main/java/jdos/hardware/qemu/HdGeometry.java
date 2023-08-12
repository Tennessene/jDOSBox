package jdos.hardware.qemu;

import jdos.util.IntRef;

public class HdGeometry extends Internal {
//    struct partition {
//        uint8_t boot_ind;           /* 0x80 - active */
//        uint8_t head;               /* starting head */
//        uint8_t sector;             /* starting sector */
//        uint8_t cyl;                /* starting cylinder */
//        uint8_t sys_ind;            /* What partition type */
//        uint8_t end_head;           /* end head */
//        uint8_t end_sector;         /* end sector */
//        uint8_t end_cyl;            /* end cylinder */
//        uint32_t start_sect;        /* starting sector counting from 0 */
//        uint32_t nr_sects;          /* nr of sectors in partition */
//    }

    static private final int end_head_OFFSET = 5;
    static private final int end_sector_OFFSET = 6;
    static private final int end_cyl_OFFSET=7;
    static private final int nr_sects_OFFSET = 8;
    static private final int PARTITION_SIZE = 16;

    /* try to guess the disk logical geometry from the MSDOS partition table.
       Return 0 if OK, -1 if could not guess */
    static private  int guess_disk_lchs(Block.BlockDriverState bs, IntRef pcylinders, IntRef pheads, IntRef psectors) {
        byte[] buf = new byte[Block.BDRV_SECTOR_SIZE];
        int i, heads, sectors, cylinders;
        int nr_sects;
        long nb_sectors;
    
        nb_sectors = Block.bdrv_get_geometry(bs);
    
        /**
         * The function will be invoked during startup not only in sync I/O mode,
         * but also in async I/O mode. So the I/O throttling function has to
         * be disabled temporarily here, not permanently.
         */
        if (Block.bdrv_read_unthrottled(bs, 0, buf, 1) < 0) {
            return -1;
        }
        /* test msdos magic */
        if (buf[510] != 0x55 || buf[511] != (byte)0xaa) {
            return -1;
        }
        for (i = 0; i < 4; i++) {
            int p = 0x1be + i*PARTITION_SIZE;
            nr_sects = readd(buf, p+nr_sects_OFFSET);
            if (nr_sects!=0 && buf[p+end_cyl_OFFSET]!=0) {
                /* We make the assumption that the partition terminates on
                   a cylinder boundary */
                heads = (buf[p+end_head_OFFSET] & 0xFF) + 1;
                sectors = (buf[p+end_sector_OFFSET] & 0xFF) & 63;
                if (sectors == 0) {
                    continue;
                }
                cylinders = (int)(nb_sectors / (heads * sectors));
                if (cylinders < 1 || cylinders > 16383) {
                    continue;
                }
                pheads.value = heads;
                psectors.value = sectors;
                pcylinders.value = cylinders;
                //trace_hd_geometry_lchs_guess(bs, cylinders, heads, sectors);
                return 0;
            }
        }
        return -1;
    }
    
    static private void guess_chs_for_size(Block.BlockDriverState bs, IntRef pcyls, IntRef pheads, IntRef psecs)
    {
        long nb_sectors;
        int cylinders;
    
        nb_sectors = Block.bdrv_get_geometry(bs);
    
        cylinders = (int)(nb_sectors / (16 * 63));
        if (cylinders > 16383) {
            cylinders = 16383;
        } else if (cylinders < 2) {
            cylinders = 2;
        }
        pcyls.value = cylinders;
        pheads.value = 16;
        psecs.value = 63;
    }
    
    static public void hd_geometry_guess(Block.BlockDriverState bs, IntRef pcyls, IntRef pheads, IntRef psecs, IntRef ptrans) {
        IntRef cylinders=new IntRef(0), heads=new IntRef(0), secs=new IntRef(0);
        int translation;
    
        if (guess_disk_lchs(bs, cylinders, heads, secs) < 0) {
            /* no LCHS guess: use a standard physical disk geometry  */
            guess_chs_for_size(bs, pcyls, pheads, psecs);
            translation = hd_bios_chs_auto_trans(pcyls.value, pheads.value, psecs.value);
        } else if (heads.value > 16) {
            /* LCHS guess with heads > 16 means that a BIOS LBA
               translation was active, so a standard physical disk
               geometry is OK */
            guess_chs_for_size(bs, pcyls, pheads, psecs);
            translation = pcyls.value * pheads.value <= 131072
                ? Block.BIOS_ATA_TRANSLATION_LARGE
                : Block.BIOS_ATA_TRANSLATION_LBA;
        } else {
            /* LCHS guess with heads <= 16: use as physical geometry */
            pcyls.value = cylinders.value;
            pheads.value = heads.value;
            psecs.value = secs.value;
            /* disable any translation to be in sync with
               the logical geometry */
            translation = Block.BIOS_ATA_TRANSLATION_NONE;
        }
        if (ptrans!=null) {
            ptrans.value = translation;
        }
        //trace_hd_geometry_guess(bs, *pcyls, *pheads, *psecs, translation);
    }
    
    static private int hd_bios_chs_auto_trans(int cyls, int heads, int secs) {
        return cyls <= 1024 && heads <= 16 && secs <= 63
            ? Block.BIOS_ATA_TRANSLATION_NONE
            : Block.BIOS_ATA_TRANSLATION_LBA;
    }
}
