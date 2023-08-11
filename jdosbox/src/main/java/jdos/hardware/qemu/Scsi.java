package jdos.hardware.qemu;

public class Scsi {
    /* Event notification classes for GET EVENT STATUS NOTIFICATION */
    public static final int  GESN_NO_EVENTS =               0;
    public static final int  GESN_OPERATIONAL_CHANGE =      1;
    public static final int  GESN_POWER_MANAGEMENT =        2;
    public static final int  GESN_EXTERNAL_REQUEST =        3;
    public static final int  GESN_MEDIA =                   4;
    public static final int  GESN_MULTIPLE_HOSTS =          5;
    public static final int  GESN_DEVICE_BUSY =             6;
    
    /* Event codes for MEDIA event status notification */
    public static final int  MEC_NO_CHANGE =                0;
    public static final int  MEC_EJECT_REQUESTED =          1;
    public static final int  MEC_NEW_MEDIA =                2;
    public static final int  MEC_MEDIA_REMOVAL =            3; /* only for media changers */
    public static final int  MEC_MEDIA_CHANGED =            4; /* only for media changers */
    public static final int  MEC_BG_FORMAT_COMPLETED =      5; /* MRW or DVD+RW b/g format completed */
    public static final int  MEC_BG_FORMAT_RESTARTED =      6; /* MRW or DVD+RW b/g format restarted */
    
    public static final int  MS_TRAY_OPEN =                 1;
    public static final int  MS_MEDIA_PRESENT=              2;
    
    /*
    *  SENSE KEYS
    */
    static public final int NO_SENSE =            0x00;
    static public final int NOT_READY =           0x02;
    static public final int ILLEGAL_REQUEST =     0x05;
    static public final int UNIT_ATTENTION =      0x06;

    /* Some generally useful CD-ROM information */
    public static final int CD_MINS =                      80; /* max. minutes per CD */
    public static final int CD_SECS =                      60; /* seconds per minute */
    public static final int CD_FRAMES =                    75; /* frames per second */
    public static final int CD_FRAMESIZE =               2048; /* bytes per frame, "cooked" mode */
    public static final int CD_MAX_BYTES =      (CD_MINS * CD_SECS * CD_FRAMES * CD_FRAMESIZE);
    public static final int CD_MAX_SECTORS =    (CD_MAX_BYTES / 512);
    
    /* Mode page codes for mode sense/set */
    public static final int  MODE_PAGE_R_W_ERROR =                  0x01;
    public static final int  MODE_PAGE_HD_GEOMETRY =                0x04;
    public static final int  MODE_PAGE_FLEXIBLE_DISK_GEOMETRY =     0x05;
    public static final int  MODE_PAGE_CACHING =                    0x08;
    public static final int  MODE_PAGE_AUDIO_CTL =                  0x0e;
    public static final int  MODE_PAGE_POWER =                      0x1a;
    public static final int  MODE_PAGE_FAULT_FAIL =                 0x1c;
    public static final int  MODE_PAGE_TO_PROTECT =                 0x1d;
    public static final int  MODE_PAGE_CAPABILITIES =               0x2a;
    public static final int  MODE_PAGE_ALLS =                       0x3f;
    /* Not in Mt. Fuji, but in ATAPI 2.6 -- depricated now in favor
     * of MODE_PAGE_SENSE_POWER */
    public static final int  MODE_PAGE_CDROM =                      0x0d;
    
    /* Profile list from MMC-6 revision 1 table 91 */
    public static final int  MMC_PROFILE_NONE =               0x0000;
    public static final int  MMC_PROFILE_CD_ROM =             0x0008;
    public static final int  MMC_PROFILE_CD_R =               0x0009;
    public static final int  MMC_PROFILE_CD_RW =              0x000A;
    public static final int  MMC_PROFILE_DVD_ROM =            0x0010;
    public static final int  MMC_PROFILE_DVD_R_SR =           0x0011;
    public static final int  MMC_PROFILE_DVD_RAM =            0x0012;
    public static final int  MMC_PROFILE_DVD_RW_RO =          0x0013;
    public static final int  MMC_PROFILE_DVD_RW_SR =          0x0014;
    public static final int  MMC_PROFILE_DVD_R_DL_SR =        0x0015;
    public static final int  MMC_PROFILE_DVD_R_DL_JR =        0x0016;
    public static final int  MMC_PROFILE_DVD_RW_DL =          0x0017;
    public static final int  MMC_PROFILE_DVD_DDR =            0x0018;
    public static final int  MMC_PROFILE_DVD_PLUS_RW =        0x001A;
    public static final int  MMC_PROFILE_DVD_PLUS_R =         0x001B;
    public static final int  MMC_PROFILE_DVD_PLUS_RW_DL =     0x002A;
    public static final int  MMC_PROFILE_DVD_PLUS_R_DL =      0x002B;
    public static final int  MMC_PROFILE_BD_ROM =             0x0040;
    public static final int  MMC_PROFILE_BD_R_SRM =           0x0041;
    public static final int  MMC_PROFILE_BD_R_RRM =           0x0042;
    public static final int  MMC_PROFILE_BD_RE =              0x0043;
    public static final int  MMC_PROFILE_HDDVD_ROM =          0x0050;
    public static final int  MMC_PROFILE_HDDVD_R =            0x0051;
    public static final int  MMC_PROFILE_HDDVD_RAM =          0x0052;
    public static final int  MMC_PROFILE_HDDVD_RW =           0x0053;
    public static final int  MMC_PROFILE_HDDVD_R_DL =         0x0058;
    public static final int  MMC_PROFILE_HDDVD_RW_DL =        0x005A;
    public static final int  MMC_PROFILE_INVALID =            0xFFFF;
    
}
