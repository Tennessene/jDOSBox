package jdos.hardware.qemu;

public class Cdrom extends Internal {
    static public void lba_to_msf(byte[] buf, int offset, long lba) {
        lba += 150;
        buf[offset] = (byte)((lba / 75) / 60);
        buf[offset+1] = (byte)((lba / 75) % 60);
        buf[offset+2] = (byte)(lba % 75);
    }

    /* same toc as bochs. Return -1 if error or the toc length */
    /* XXX: check this */
    static public int cdrom_read_toc(long nb_sectors, byte[] buf, int msf, int start_track)
    {
        int q;
        int len;
    
        if (start_track > 1 && start_track != 0xaa)
            return -1;
        q = 2;
        buf[q++] = 1; /* first session */
        buf[q++] = 1; /* last session */
        if (start_track <= 1) {
            buf[q++] = 0; /* reserved */
            buf[q++] = 0x14; /* ADR, control */
            buf[q++] = 1;    /* track number */
            buf[q++] = 0; /* reserved */
            if (msf!=0) {
                buf[q++] = 0; /* reserved */
                lba_to_msf(buf, q, 0);
                q += 3;
            } else {
                /* sector 0 */
                writed(buf, q, 0);
                q += 4;
            }
        }
        /* lead out track */
        buf[q++] = 0; /* reserved */
        buf[q++] = 0x16; /* ADR, control */
        buf[q++] = (byte)0xaa; /* track number */
        buf[q++] = 0; /* reserved */
        if (msf!=0) {
            buf[q++] = 0; /* reserved */
            lba_to_msf(buf, q, nb_sectors);
            q += 3;
        } else {
            writed(buf, q, nb_sectors);
            q += 4;
        }
        len = q;
        writew(buf, 0, len - 2);
        return len;
    }
    
    /* mostly same info as PearPc */
    static public int cdrom_read_toc_raw(long nb_sectors, byte[] buf, int msf, int session_num)
    {
        int q;
        int len;
    
        q = 2;
        buf[q++] = 1; /* first session */
        buf[q++] = 1; /* last session */
    
        buf[q++] = 1; /* session number */
        buf[q++] = 0x14; /* data track */
        buf[q++] = 0; /* track number */
        buf[q++] = (byte)0xa0; /* lead-in */
        buf[q++] = 0; /* min */
        buf[q++] = 0; /* sec */
        buf[q++] = 0; /* frame */
        buf[q++] = 0;
        buf[q++] = 1; /* first track */
        buf[q++] = 0x00; /* disk type */
        buf[q++] = 0x00;
    
        buf[q++] = 1; /* session number */
        buf[q++] = 0x14; /* data track */
        buf[q++] = 0; /* track number */
        buf[q++] = (byte)0xa1;
        buf[q++] = 0; /* min */
        buf[q++] = 0; /* sec */
        buf[q++] = 0; /* frame */
        buf[q++] = 0;
        buf[q++] = 1; /* last track */
        buf[q++] = 0x00;
        buf[q++] = 0x00;
    
        buf[q++] = 1; /* session number */
        buf[q++] = 0x14; /* data track */
        buf[q++] = 0; /* track number */
        buf[q++] = (byte)0xa2; /* lead-out */
        buf[q++] = 0; /* min */
        buf[q++] = 0; /* sec */
        buf[q++] = 0; /* frame */
        if (msf!=0) {
            buf[q++] = 0; /* reserved */
            lba_to_msf(buf, q, nb_sectors);
            q += 3;
        } else {
            writed(buf, q, nb_sectors);
            q += 4;
        }
    
        buf[q++] = 1; /* session number */
        buf[q++] = 0x14; /* ADR, control */
        buf[q++] = 0;    /* track number */
        buf[q++] = 1;    /* point */
        buf[q++] = 0; /* min */
        buf[q++] = 0; /* sec */
        buf[q++] = 0; /* frame */
        if (msf!=0) {
            buf[q++] = 0;
            lba_to_msf(buf, q, 0);
            q += 3;
        } else {
            buf[q++] = 0;
            buf[q++] = 0;
            buf[q++] = 0;
            buf[q++] = 0;
        }
    
        len = q;
        writew(buf, 0, len - 2);
        return len;
    }
    
}
