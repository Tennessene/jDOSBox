package jdos.hardware.qemu;

import jdos.util.FileIO;

public class RawBlockDriver extends BlockDriver {
    FileIO file;
    int sector_size = 512;
    long current_fpos = -1;

    public RawBlockDriver(FileIO file) {
        this.file = file;
    }
    public Object allocOpaque() {
        return null;
    }

    public int bdrv_probe(byte[] buf, int buf_size, String filename) {
        return 0;
    }

    public boolean has_bdrv_probe() {
        return false;
    }

    public int bdrv_open(Block.BlockDriverState bs, int flags) {
        return 0;
    }

    public int bdrv_file_open(Block.BlockDriverState bs, String filename, int flags) {
        return 0;
    }

    public boolean has_bdrv_file_open() {
        return true;
    }

    public int bdrv_read(Block.BlockDriverState bs, long sector_num, byte[] buf, int bufferOffset, int nb_sectors) {
        /*Bit32u*/long bytenum;

        bytenum = sector_num * sector_size;

        try {
            if (bytenum!=current_fpos)
                file.seek(bytenum);
            current_fpos=bytenum;
            int read = sector_size*nb_sectors;
            int ret=file.read(buf, bufferOffset, read);
            if (ret != read) {
                return -Error.EIO;
            }
            current_fpos+=read;
        } catch (Exception e) {
            e.printStackTrace();
            return -Error.EIO;
        }
        return 0;
    }

    public int bdrv_write(Block.BlockDriverState bs, long sector_num, byte[] buf, int bufferOffset, int nb_sectors) {
        /*Bit32u*/long bytenum;

        bytenum = sector_num * sector_size;

        try {
            if (bytenum!=current_fpos)
                file.seek(bytenum);
            current_fpos=bytenum;

            for (int i=0;i<nb_sectors;i++) {
                file.write(buf, bufferOffset, sector_size);
                current_fpos+=sector_size;
                bufferOffset+=sector_size;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -Error.EIO;
        }
        return 0;
    }

    public long bdrv_getlength(Block.BlockDriverState bs) {
        try {
            return file.length();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean has_bdrv_getlength() {
        return true;
    }

    public boolean bdrv_is_inserted(Block.BlockDriverState bs) {
        return false;
    }

    public boolean has_bdrv_is_inserted() {
        return false;
    }

    public int bdrv_media_changed(Block.BlockDriverState bs) {
        return 0;
    }

    public boolean has_bdrv_media_changed() {
        return false;
    }

    public void bdrv_eject(Block.BlockDriverState bs, boolean eject_flag) {
    }

    public boolean has_bdrv_eject() {
        return false;
    }

    public void bdrv_lock_medium(Block.BlockDriverState bs, boolean locked) {
    }

    public boolean has_bdrv_lock_medium() {
        return false;
    }
}
