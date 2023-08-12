package jdos.hardware.qemu;

/*
 * QEMU IDE Emulation -- internal header file
 * only files in hw/ide/ are supposed to include this file.
 * non-internal declarations are in hw/ide.h
 */

import jdos.hardware.Pic;

public class Internal {
    /* debug IDE devices */
    static public final boolean DEBUG_IDE = false;
    static public final boolean DEBUG_IDE_ATAPI = false;
    static public final boolean DEBUG_AIO = true;
    static public final boolean USE_DMA_CDROM = true;

    static public final String TYPE_IDE_BUS = "IDE";
    //static public final int IDE_BUS(obj) OBJECT_CHECK(IDEBus, (obj), TYPE_IDE_BUS)
    
    /* Bits of HD_STATUS */
    static public final int ERR_STAT =      0x01;
    static public final int INDEX_STAT =    0x02;
    static public final int ECC_STAT =      0x04;	/* Corrected error */
    static public final int DRQ_STAT =      0x08;
    static public final int SEEK_STAT =     0x10;
    static public final int SRV_STAT =      0x10;
    static public final int WRERR_STAT =    0x20;
    static public final int READY_STAT =    0x40;
    static public final int BUSY_STAT =     0x80;
    
    /* Bits for HD_ERROR */
    static public final int MARK_ERR =      0x01;	/* Bad address mark */
    static public final int TRK0_ERR =      0x02;	/* couldn't find track 0 */
    static public final int ABRT_ERR =      0x04;	/* Command aborted */
    static public final int MCR_ERR =       0x08;	/* media change request */
    static public final int ID_ERR =        0x10;	/* ID field not found */
    static public final int MC_ERR =        0x20;	/* media changed */
    static public final int ECC_ERR =       0x40;	/* Uncorrectable ECC error */
    static public final int BBD_ERR =       0x80;	/* pre-EIDE meaning:  block marked bad */
    static public final int ICRC_ERR =      0x80;	/* new meaning:  CRC error during transfer */
    
    /* Bits of HD_NSECTOR */
    static public final int CD =        0x01;
    static public final int IO =        0x02;
    static public final int REL =       0x04;
    static public final int TAG_MASK =  0xf8;
    
    static public final int IDE_CMD_RESET =         0x04;
    static public final int IDE_CMD_DISABLE_IRQ =   0x02;
    
    /* ACS-2 T13/2015-D Table B.2 Command codes */
    static public final int WIN_NOP =                   0x00;
    /* reserved                                         0x01..0x02 */
    static public final int CFA_REQ_EXT_ERROR_CODE =    0x03; /* CFA Request Extended Error Code */
    /* reserved                                         0x04..0x05 */
    static public final int WIN_DSM =                   0x06;
    /* reserved                                         0x07 */
    static public final int WIN_DEVICE_RESET =          0x08;
    /* reserved                                         0x09..0x0a */
    /* REQUEST SENSE DATA EXT                           0x0B */
    /* reserved                                         0x0C..0x0F */
    static public final int WIN_RECAL =                 0x10; /* obsolete since ATA4 */
    /* obsolete since ATA3, retired in ATA4             0x11..0x1F */
    static public final int WIN_READ =                  0x20; /* 28-Bit */
    static public final int WIN_READ_ONCE =             0x21; /* 28-Bit w/o retries, obsolete since ATA5 */
    /* obsolete since ATA4                              0x22..0x23 */
    static public final int WIN_READ_EXT =              0x24; /* 48-Bit */
    static public final int WIN_READDMA_EXT =           0x25; /* 48-Bit */
    static public final int WIN_READDMA_QUEUED_EXT =    0x26; /* 48-Bit, obsolete since ACS2 */
    static public final int WIN_READ_NATIVE_MAX_EXT =   0x27; /* 48-Bit */
    /* reserved                                         0x28 */
    static public final int WIN_MULTREAD_EXT =          0x29; /* 48-Bit */
    /* READ STREAM DMA EXT                              0x2A */
    /* READ STREAM EXT                                  0x2B */
    /* reserved                                         0x2C..0x2E */
    /* READ LOG EXT                                     0x2F */
    static public final int WIN_WRITE =                 0x30; /* 28-Bit */
    static public final int WIN_WRITE_ONCE =            0x31; /* 28-Bit w/o retries, obsolete since ATA5 */
    /* obsolete since ATA4                              0x32..0x33 */
    static public final int WIN_WRITE_EXT =             0x34; /* 48-Bit */
    static public final int WIN_WRITEDMA_EXT =          0x35; /* 48-Bit */
    static public final int WIN_WRITEDMA_QUEUED_EXT =   0x36; /* 48-Bit */
    static public final int WIN_SET_MAX_EXT =           0x37; /* 48-Bit, obsolete since ACS2 */
    //static public final int WIN_SET_MAX_EXT	=           0x37; /* 48-Bit */
    static public final int CFA_WRITE_SECT_WO_ERASE =   0x38; /* CFA Write Sectors without erase */
    static public final int WIN_MULTWRITE_EXT =         0x39; /* 48-Bit */
    /* WRITE STREAM DMA EXT                             0x3A */
    /* WRITE STREAM EXT                                 0x3B */
    static public final int WIN_WRITE_VERIFY =          0x3C; /* 28-Bit, obsolete since ATA4 */
    /* WRITE DMA FUA EXT                                0x3D */
    /* obsolete since ACS2                              0x3E */
    /* WRITE LOG EXT                                    0x3F */
    static public final int WIN_VERIFY =                0x40; /* 28-Bit - Read Verify Sectors */
    static public final int WIN_VERIFY_ONCE =           0x41; /* 28-Bit - w/o retries, obsolete since ATA5 */
    static public final int WIN_VERIFY_EXT =            0x42; /* 48-Bit */
    /* reserved                                         0x43..0x44 */
    /* WRITE UNCORRECTABLE EXT                          0x45 */
    /* reserved                                         0x46 */
    /* READ LOG DMA EXT                                 0x47 */
    /* reserved                                         0x48..0x4F */
    /* obsolete since ATA4                              0x50 */
    /* CONFIGURE STREAM                                 0x51 */
    /* reserved                                         0x52..0x56 */
    /* WRITE LOG DMA EXT                                0x57 */
    /* reserved                                         0x58..0x5A */
    /* TRUSTED NON DATA                                 0x5B */
    /* TRUSTED RECEIVE                                  0x5C */
    /* TRUSTED RECEIVE DMA                              0x5D */
    /* TRUSTED SEND                                     0x5E */
    /* TRUSTED SEND DMA                                 0x5F */
    /* READ FPDMA QUEUED                                0x60 */
    /* WRITE FPDMA QUEUED                               0x61 */
    /* reserved                                         0x62->0x6F */
    static public final int WIN_SEEK =                  0x70; /* obsolete since ATA7 */
    /* reserved                                         0x71-0x7F */
    /* vendor specific                                  0x80-0x86 */
    static public final int CFA_TRANSLATE_SECTOR =      0x87; /* CFA Translate Sector */
    /* vendor specific                                  0x88-0x8F */
    static public final int WIN_DIAGNOSE =              0x90;
    static public final int WIN_SPECIFY =               0x91; /* set drive geometry translation, obsolete since ATA6 */
    static public final int WIN_DOWNLOAD_MICROCODE =    0x92;
    /* DOWNLOAD MICROCODE DMA                           0x93 */
    static public final int WIN_STANDBYNOW2 =           0x94; /* retired in ATA4 */
    static public final int WIN_IDLEIMMEDIATE2 =        0x95; /* force drive to become "ready", retired in ATA4 */
    static public final int WIN_STANDBY2 =              0x96; /* retired in ATA4 */
    static public final int WIN_SETIDLE2 =              0x97; /* retired in ATA4 */
    static public final int WIN_CHECKPOWERMODE2 =       0x98; /* retired in ATA4 */
    static public final int WIN_SLEEPNOW2 =             0x99; /* retired in ATA4 */
    /* vendor specific                                  0x9A */
    /* reserved                                         0x9B..0x9F */
    static public final int WIN_PACKETCMD =             0xA0; /* Send a packet command. */
    static public final int WIN_PIDENTIFY =             0xA1; /* identify ATAPI device	*/
    static public final int WIN_QUEUED_SERVICE =        0xA2; /* obsolete since ACS2 */
    /* reserved                                         0xA3..0xAF */
    static public final int WIN_SMART =                 0xB0; /* self-monitoring and reporting */
    /* Device Configuration Overlay                     0xB1 */
    /* reserved                                         0xB2..0xB3 */
    /* Sanitize Device                                  0xB4 */
    /* reserved                                         0xB5 */
    /* NV Cache                                         0xB6 */
    /* reserved for CFA                                 0xB7..0xBB */
    static public final int CFA_ACCESS_METADATA_STORAGE=0xB8;
    /* reserved                                         0xBC..0xBF */
    static public final int CFA_ERASE_SECTORS =         0xC0; /* microdrives implement as NOP */
    /* vendor specific                                  0xC1..0xC3 */
    static public final int WIN_MULTREAD =              0xC4; /* read sectors using multiple mode*/
    static public final int WIN_MULTWRITE =             0xC5; /* write sectors using multiple mode */
    static public final int WIN_SETMULT =               0xC6; /* enable/disable multiple mode */
    static public final int WIN_READDMA_QUEUED =        0xC7; /* read sectors using Queued DMA transfers, obsolete since ACS2 */
    static public final int WIN_READDMA =               0xC8; /* read sectors using DMA transfers */
    static public final int WIN_READDMA_ONCE =          0xC9; /* 28-Bit - w/o retries, obsolete since ATA5 */
    static public final int WIN_WRITEDMA =              0xCA; /* write sectors using DMA transfers */
    static public final int WIN_WRITEDMA_ONCE =         0xCB; /* 28-Bit - w/o retries, obsolete since ATA5 */
    static public final int WIN_WRITEDMA_QUEUED =       0xCC; /* write sectors using Queued DMA transfers, obsolete since ACS2 */
    static public final int CFA_WRITE_MULTI_WO_ERASE =  0xCD; /* CFA Write multiple without erase */
    /* WRITE MULTIPLE FUA EXT                           0xCE */
    /* reserved                                         0xCF..0xDO */
    /* CHECK MEDIA CARD TYPE                            0xD1 */
    /* reserved for media card pass through             0xD2..0xD4 */
    /* reserved                                         0xD5..0xD9 */
    static public final int WIN_GETMEDIASTATUS =        0xDA; /* obsolete since ATA8 */
    /* obsolete since ATA3, retired in ATA4             0xDB..0xDD */
    static public final int WIN_DOORLOCK =              0xDE; /* lock door on removable drives, obsolete since ATA8 */
    static public final int WIN_DOORUNLOCK =            0xDF; /* unlock door on removable drives, obsolete since ATA8 */
    static public final int WIN_STANDBYNOW1 =           0xE0;
    static public final int WIN_IDLEIMMEDIATE =         0xE1; /* force drive to become "ready" */
    static public final int WIN_STANDBY =               0xE2; /* Set device in Standby Mode */
    static public final int WIN_SETIDLE1 =              0xE3;
    static public final int WIN_READ_BUFFER =           0xE4; /* force read only 1 sector */
    static public final int WIN_CHECKPOWERMODE1 =       0xE5;
    static public final int WIN_SLEEPNOW1 =             0xE6;
    static public final int WIN_FLUSH_CACHE =           0xE7;
    static public final int WIN_WRITE_BUFFER =          0xE8; /* force write only 1 sector */
    /* READ BUFFER DMA                                  0xE9 */
    static public final int WIN_FLUSH_CACHE_EXT =       0xEA; /* 48-Bit */
    /* WRITE BUFFER DMA                                 0xEB */
    static public final int WIN_IDENTIFY =              0xEC; /* ask drive to identify itself	*/
    static public final int WIN_MEDIAEJECT =            0xED; /* obsolete since ATA8 */
    /* obsolete since ATA4                              0xEE */
    static public final int WIN_SETFEATURES =           0xEF; /* set special drive features */
    static public final int IBM_SENSE_CONDITION =       0xF0; /* measure disk temperature, vendor specific */
    static public final int WIN_SECURITY_SET_PASS =     0xF1;
    static public final int WIN_SECURITY_UNLOCK =       0xF2;
    static public final int WIN_SECURITY_ERASE_PREPARE= 0xF3;
    static public final int WIN_SECURITY_ERASE_UNIT =   0xF4;
    static public final int WIN_SECURITY_FREEZE_LOCK =  0xF5;
    static public final int CFA_WEAR_LEVEL =            0xF5; /* microdrives implement as NOP; not specified in T13! */
    static public final int WIN_SECURITY_DISABLE =      0xF6;
    /* vendor specific                                  0xF7 */
    static public final int WIN_READ_NATIVE_MAX =       0xF8; /* return the native maximum address */
    static public final int WIN_SET_MAX =               0xF9;
    /* vendor specific                                  0xFA..0xFF */
    
    /* set to 1 set disable mult support */
    static public final int MAX_MULT_SECTORS = 16;
    
    static public final int IDE_DMA_BUF_SECTORS = 256;
    
    /* feature values for Data Set Management */
    static public final int DSM_TRIM = 0x01;
    
    //#if (IDE_DMA_BUF_SECTORS < MAX_MULT_SECTORS)
    //#error "IDE_DMA_BUF_SECTORS must be bigger or equal to MAX_MULT_SECTORS"
    //#endif
    
    /* ATAPI defines */
    
    static public final int ATAPI_PACKET_SIZE = 12;
    
    /* The generic packet command opcodes for CD/DVD Logical Units,
     * From Table 57 of the SFF8090 Ver. 3 (Mt. Fuji) draft standard. */
    static public final int GPCMD_BLANK =                       0xa1;
    static public final int GPCMD_CLOSE_TRACK =                 0x5b;
    static public final int GPCMD_FLUSH_CACHE =                 0x35;
    static public final int GPCMD_FORMAT_UNIT =                 0x04;
    static public final int GPCMD_GET_CONFIGURATION =           0x46;
    static public final int GPCMD_GET_EVENT_STATUS_NOTIFICATION=0x4a;
    static public final int GPCMD_GET_PERFORMANCE =             0xac;
    static public final int GPCMD_INQUIRY =                     0x12;
    static public final int GPCMD_LOAD_UNLOAD =                 0xa6;
    static public final int GPCMD_MECHANISM_STATUS =            0xbd;
    static public final int GPCMD_MODE_SELECT_10 =              0x55;
    static public final int GPCMD_MODE_SENSE_10 =               0x5a;
    static public final int GPCMD_PAUSE_RESUME =                0x4b;
    static public final int GPCMD_PLAY_AUDIO_10 =               0x45;
    static public final int GPCMD_PLAY_AUDIO_MSF =              0x47;
    static public final int GPCMD_PLAY_AUDIO_TI =               0x48;
    static public final int GPCMD_PLAY_CD =                     0xbc;
    static public final int GPCMD_PREVENT_ALLOW_MEDIUM_REMOVAL =0x1e;
    static public final int GPCMD_READ_10 =                     0x28;
    static public final int GPCMD_READ_12 =                     0xa8;
    static public final int GPCMD_READ_CDVD_CAPACITY =          0x25;
    static public final int GPCMD_READ_CD =                     0xbe;
    static public final int GPCMD_READ_CD_MSF =                 0xb9;
    static public final int GPCMD_READ_DISC_INFO =              0x51;
    static public final int GPCMD_READ_DVD_STRUCTURE =          0xad;
    static public final int GPCMD_READ_FORMAT_CAPACITIES =      0x23;
    static public final int GPCMD_READ_HEADER =                 0x44;
    static public final int GPCMD_READ_TRACK_RZONE_INFO =       0x52;
    static public final int GPCMD_READ_SUBCHANNEL =             0x42;
    static public final int GPCMD_READ_TOC_PMA_ATIP =           0x43;
    static public final int GPCMD_REPAIR_RZONE_TRACK =          0x58;
    static public final int GPCMD_REPORT_KEY =                  0xa4;
    static public final int GPCMD_REQUEST_SENSE =               0x03;
    static public final int GPCMD_RESERVE_RZONE_TRACK =         0x53;
    static public final int GPCMD_SCAN =                        0xba;
    static public final int GPCMD_SEEK =                        0x2b;
    static public final int GPCMD_SEND_DVD_STRUCTURE =          0xad;
    static public final int GPCMD_SEND_EVENT =                  0xa2;
    static public final int GPCMD_SEND_KEY =                    0xa3;
    static public final int GPCMD_SEND_OPC =                    0x54;
    static public final int GPCMD_SET_READ_AHEAD =              0xa7;
    static public final int GPCMD_SET_STREAMING =               0xb6;
    static public final int GPCMD_START_STOP_UNIT =             0x1b;
    static public final int GPCMD_STOP_PLAY_SCAN =              0x4e;
    static public final int GPCMD_TEST_UNIT_READY =             0x00;
    static public final int GPCMD_VERIFY_10 =                   0x2f;
    static public final int GPCMD_WRITE_10 =                    0x2a;
    static public final int GPCMD_WRITE_AND_VERIFY_10 =         0x2e;
    /* This is listed as optional in ATAPI 2.6, but is (curiously)
     * missing from Mt. Fuji, Table 57.  It _is_ mentioned in Mt. Fuji
     * Table 377 as an MMC command for SCSi devices though...  Most ATAPI
     * drives support it. */
    static public final int GPCMD_SET_SPEED =                   0xbb;
    /* This seems to be a SCSI specific CD-ROM opcode
     * to play data at track/index */
    static public final int GPCMD_PLAYAUDIO_TI =                0x48;
    /*
     * From MS Media Status Notification Support Specification. For
     * older drives only.
     */
    static public final int GPCMD_GET_MEDIA_STATUS =            0xda;
    static public final int GPCMD_MODE_SENSE_6 =                0x1a;
    
    static public final int ATAPI_INT_REASON_CD =               0x01; /* 0 = data transfer */
    static public final int ATAPI_INT_REASON_IO =               0x02; /* 1 = transfer to the host */
    static public final int ATAPI_INT_REASON_REL =              0x04;
    static public final int ATAPI_INT_REASON_TAG =              0xf8;
    
    /* same constants as bochs */
    static public final int ASC_ILLEGAL_OPCODE =                0x20;
    static public final int ASC_LOGICAL_BLOCK_OOR =             0x21;
    static public final int ASC_INV_FIELD_IN_CMD_PACKET =       0x24;
    static public final int ASC_MEDIUM_MAY_HAVE_CHANGED =       0x28;
    static public final int ASC_INCOMPATIBLE_FORMAT =           0x30;
    static public final int ASC_MEDIUM_NOT_PRESENT =            0x3a;
    static public final int ASC_SAVING_PARAMETERS_NOT_SUPPORTED=0x39;
    static public final int ASC_MEDIA_REMOVAL_PREVENTED =       0x53;
    
    static public final int CFA_NO_ERROR =          0x00;
    static public final int CFA_MISC_ERROR =        0x09;
    static public final int CFA_INVALID_COMMAND =   0x20;
    static public final int CFA_INVALID_ADDRESS =   0x21;
    static public final int CFA_ADDRESS_OVERFLOW =  0x2f;
    
    static public final int SMART_READ_DATA =       0xd0;
    static public final int SMART_READ_THRESH =     0xd1;
    static public final int SMART_ATTR_AUTOSAVE =   0xd2;
    static public final int SMART_SAVE_ATTR =       0xd3;
    static public final int SMART_EXECUTE_OFFLINE = 0xd4;
    static public final int SMART_READ_LOG =        0xd5;
    static public final int SMART_WRITE_LOG =       0xd6;
    static public final int SMART_ENABLE =          0xd8;
    static public final int SMART_DISABLE =         0xd9;
    static public final int SMART_STATUS =          0xda;
    
    static public final int IDE_HD=0;
    static public final int IDE_CD=1;
    static public final int IDE_CFATA=2;

    public interface EndTransferFunc {
        public void call(IDEState s);
    }

    public static interface DMAStartFunc {
        public void call(IDEDMA dma, IDEState s, Block.BlockDriverCompletionFunc cb);
    }

    public static interface DMAFunc {
        public int call(IDEDMA dma);
    }

    public static interface DMAIntFunc {
        public int call(IDEDMA dma, int x);
    }

    public static interface DMARestartFunc {
        public void call(Object opaque, int x, int y);
    }
    
    static public class unreported_events {
        public boolean eject_request;
        public boolean new_media;
    }
    
    static public enum ide_dma_cmd {
        IDE_DMA_READ,
        IDE_DMA_WRITE,
        IDE_DMA_TRIM,
    }

    /* NOTE: IDEState represents in fact one drive */
    static public final class IDEState {
        public boolean ide_cmd_is_read() {return dma_cmd==ide_dma_cmd.IDE_DMA_READ;}
        public IDEBus bus;
        public int unit;
        /* ide config */
        public int drive_kind;
        public int cylinders, heads, sectors, chs_trans;
        public long nb_sectors;
        public int mult_sectors;
        public boolean identify_set;
        public byte[] identify_data = new byte[512];
        public int drive_serial;
        public String drive_serial_str;
        public String drive_model_str;
        public long wwn;
        /* ide regs */
        public int feature;
        public int error;
        public int nsector;
        public int sector;
        public int lcyl;
        public int hcyl;
        /* other part of tf for lba48 support */
        public int hob_feature;
        public int hob_nsector;
        public int hob_sector;
        public int hob_lcyl;
        public int hob_hcyl;
    
        public int select;
        public int status;
    
        /* set for lba48 access */
        public int lba48;
        public Block.BlockDriverState bs;
        public String version;
        /* ATAPI specific */
        public unreported_events events = new unreported_events();
        public int sense_key;
        public int asc;
        public boolean tray_open;
        public boolean tray_locked;
        public boolean cdrom_changed;
        public int packet_transfer_size;
        public int elementary_transfer_size;
        public int io_buffer_index;
        public long lba;
        public int cd_sector_size;
        public int atapi_dma; /* true if dma is requested for the packet cmd */
        public Block.BlockAcctCookie acct = new Block.BlockAcctCookie();
        public Block.BlockDriverAIOCB pio_aiocb;
        public QemuCommon.iovec iov = new QemuCommon.iovec();
        public QemuCommon.QEMUIOVector qiov;
        /* ATA DMA state */
        public int io_buffer_offset;
        public int io_buffer_size;
        public DMA.QEMUSGList sg;
        /* PIO transfer handling */
        public int req_nb_sectors; /* number of sectors per interrupt */
        public EndTransferFunc end_transfer_func;
        public byte[] data_ptr;
        public int data_ptr_offset;
        public int data_end;
        public byte[] io_buffer;
        /* PIO save/restore */
        public int io_buffer_total_len;
        public int cur_io_buffer_offset;
        public int cur_io_buffer_len;
        public int end_transfer_fn_idx;
        //QEMUTimer *sector_write_timer; /* only used for win2k install hack */
        public long irq_count; /* counts IRQs when using win2k install hack */
        /* CF-ATA extended error */
        public int ext_error;
        /* CF-ATA metadata storage */
        public long mdata_size;
        public byte[] mdata_storage;
        public int media_changed;
        public ide_dma_cmd dma_cmd;
        /* SMART */
        public boolean smart_enabled;
        public boolean smart_autosave;
        public int smart_errors;
        public int smart_selftest_count;
        public byte[] smart_selftest_data;
        /* AHCI */
        public int ncq_queues;
    }

    static public class IDEDMAOps {
        DMAStartFunc start_dma;
        DMAFunc start_transfer;
        DMAIntFunc prepare_buf;
        DMAIntFunc rw_buf;
        DMAIntFunc set_unit;
        DMAIntFunc add_status;
        DMAFunc set_inactive;
        DMARestartFunc restart_cb;
        DMAFunc reset;
    }

    static public class IDEDMA {
        public IDEDMA() {
        }
        public IDEDMA(IDEDMAOps ops) {
            this.ops = ops;
        }
        IDEDMAOps ops;
        QemuCommon.iovec iov;
        QemuCommon.QEMUIOVector qiov;
        Block.BlockDriverAIOCB aiocb;
    }

    static public final String TYPE_IDE_DEVICE = "ide-device";
//    static public final int IDE_DEVICE(obj) \
//         OBJECT_CHECK(IDEDevice, (obj), TYPE_IDE_DEVICE)
//    static public final int IDE_DEVICE_CLASS(klass) \
//         OBJECT_CLASS_CHECK(IDEDeviceClass, (klass), TYPE_IDE_DEVICE)
//    static public final int IDE_DEVICE_GET_CLASS(obj) \
//         OBJECT_GET_CLASS(IDEDeviceClass, (obj), TYPE_IDE_DEVICE)
//
//    typedef struct IDEDeviceClass {
//        DeviceClass parent_class;
//        int (*init)(IDEDevice *dev);
//    } IDEDeviceClass;

    public static class BlockConf {
        //BlockDriverState bs;
        long physical_block_size = 512;
        long logical_block_size = 512;
        long min_io_size;
        long opt_io_size;
        long bootindex;
        int discard_granularity = 0;
        /* geometry, not all devices use this */
        long cyls, heads, secs;

        public long get_physical_block_exp()
        {
            long exp = 0, size;

            for (size = physical_block_size; size > logical_block_size; size >>= 1) {
                exp++;
            }

            return exp;
        }
    }

    public static class IDEDevice {
        //DeviceState qdev;
        int unit;
        BlockConf conf = new BlockConf();
        int chs_trans;
        String version;
        String serial;
        String model;
        long wwn;
    }
    
    static public final int BM_STATUS_DMAING =  0x01;
    static public final int BM_STATUS_ERROR =   0x02;
    static public final int BM_STATUS_INT =     0x04;
    
    /* FIXME These are not status register bits */
    static public final int BM_STATUS_DMA_RETRY =   0x08;
    static public final int BM_STATUS_PIO_RETRY =   0x10;
    static public final int BM_STATUS_RETRY_READ =  0x20;
    static public final int BM_STATUS_RETRY_FLUSH = 0x40;
    static public final int BM_STATUS_RETRY_TRIM =  0x80;
    
    static public final int BM_MIGRATION_COMPAT_STATUS_BITS = (BM_STATUS_DMA_RETRY | BM_STATUS_PIO_RETRY | BM_STATUS_RETRY_READ | BM_STATUS_RETRY_FLUSH);
    
    static public final int BM_CMD_START =  0x01;
    static public final int BM_CMD_READ =   0x08;
    
    static public IDEState idebus_active_if(IDEBus bus)
    {
        return bus.ifs[bus.unit];
    }
    
    static public void ide_set_irq(IDEBus bus)
    {
        if ((bus.cmd & IDE_CMD_DISABLE_IRQ)==0) {
            Pic.PIC_ActivateIRQ(bus.irq);
        }
    }
    
    /* hw/ide/core.c */
//    extern const VMStateDescription vmstate_ide_bus;
//
//    static public final int VMSTATE_IDE_BUS(_field, _state)                          \
//        VMSTATE_STRUCT(_field, _state, 1, vmstate_ide_bus, IDEBus)
//
//    static public final int VMSTATE_IDE_BUS_ARRAY(_field, _state, _num)              \
//        VMSTATE_STRUCT_ARRAY(_field, _state, _num, 1, vmstate_ide_bus, IDEBus)
//
//    extern const VMStateDescription vmstate_ide_drive;
//
//    static public final int VMSTATE_IDE_DRIVES(_field, _state) \
//        VMSTATE_STRUCT_ARRAY(_field, _state, 2, 3, vmstate_ide_drive, IDEState)
    

    static public int readw(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset+1] & 0xFF) << 8);
    }
    static public int readd(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset+1] & 0xFF) << 8) | ((b[offset+2] & 0xFF) << 16) | ((b[offset+3] & 0xFF) << 24);
    }
    static void writew(byte[] b, int offset, int value) {
        b[offset]=(byte)(value);
	    b[offset+1]=(byte)((value >> 8));
    }
    static void writed(byte[] b, int offset, long value) {
        b[offset]=(byte)(value);
	    b[offset+1]=(byte)((value >> 8));
        b[offset+2]=(byte)((value >> 16));
        b[offset+3]=(byte)((value >>> 24));
    }

    static long be_readd(byte[] b, int offset) {
        return (b[offset+3] & 0xFF) | ((b[offset+2] & 0xFF) << 8) | ((b[offset+1] & 0xFF) << 16) | ((b[offset] & 0xFF) << 24);
    }
    static int be_readw(byte[] b, int offset) {
        return (b[offset+1] & 0xFF) | ((b[offset] & 0xFF) << 8);
    }
    static void be_writew(byte[] b, int offset, int value) {
        b[offset+1]=(byte)(value);
	    b[offset]=(byte)((value >> 8));
    }
    static void be_writed(byte[] b, int offset, long value) {
        b[offset+3]=(byte)(value);
	    b[offset+2]=(byte)((value >> 8));
        b[offset+1]=(byte)((value >> 16));
        b[offset]=(byte)((value >>> 24));
    }
}
