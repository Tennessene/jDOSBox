package jdos.hardware.qemu;

public abstract class BlockDriver {
    public String format_name;
    abstract public Object allocOpaque();
    abstract public int bdrv_probe(byte[] buf, int buf_size, String filename);
    abstract public boolean has_bdrv_probe();

//    abstract public int bdrv_probe_device(String filename);
    abstract public int bdrv_open(Block.BlockDriverState bs, int flags);

    abstract public int bdrv_file_open(Block.BlockDriverState bs, String filename, int flags);
    abstract public boolean has_bdrv_file_open();

    abstract public int bdrv_read(Block.BlockDriverState bs, long sector_num, byte[] buf, int bufferOffset, int nb_sectors);
    abstract public int bdrv_write(Block.BlockDriverState bs, long sector_num, byte[] buf, int bufferOffset, int nb_sectors);
//    abstract public void bdrv_close(Block.BlockDriverState bs);
//    abstract public void bdrv_rebind(Block.BlockDriverState bs);
//    abstract public int bdrv_create(String filename, Hashtable<String, String> options);
//    abstract public int bdrv_set_key(Block.BlockDriverState bs, String key);
//    abstract public int bdrv_make_empty(Block.BlockDriverState bs);
//    /* aio */
//    static public interface  bdrv_aio_read_write {
//        public Block.BlockDriverAIOCB call(Block.BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector qiov, int nb_sectors, Block.BlockDriverCompletionFunc cb, Object opaque);
//    }
//    public bdrv_aio_read_write bdrv_aio_readv;
//    public bdrv_aio_read_write bdrv_aio_writev;
//    abstract public Block.BlockDriverAIOCB bdrv_aio_flush(Block.BlockDriverState bs, Block.BlockDriverCompletionFunc cb, Object opaque);
//    abstract public Block.BlockDriverAIOCB bdrv_aio_discard(Block.BlockDriverState bs, long sector_num, int nb_sectors, Block.BlockDriverCompletionFunc cb, Object opaque);
//
//    static public interface bdrv_co_read_write {
//        public int call(Block.BlockDriverState bs, long sector_num, int nb_sectors, QemuCommon.QEMUIOVector qiov);
//    }
//    public bdrv_co_read_write bdrv_co_readv;
//    public bdrv_co_read_write bdrv_co_writev;
    /*
     * Efficiently zero a region of the disk image.  Typically an image format
     * would use a compact metadata representation to implement this.  This
     * function pointer may be NULL and .bdrv_co_writev() will be called
     * instead.
     */
//    abstract public int bdrv_co_write_zeroes(Block.BlockDriverState bs, long sector_num, int nb_sectors);
//    abstract public int bdrv_co_discard(Block.BlockDriverState bs, long sector_num, int nb_sectors);
//    abstract public int bdrv_co_is_allocated(Block.BlockDriverState bs, long sector_num, int nb_sectors, int *pnum);

    /*
     * Invalidate any cached meta-data.
     */
//    abstract public void bdrv_invalidate_cache(Block.BlockDriverState bs);

    /*
     * Flushes all data that was already written to the OS all the way down to
     * the disk (for example raw-posix calls fsync()).
     */
//    abstract public int bdrv_co_flush_to_disk(Block.BlockDriverState bs);

    /*
     * Flushes all internal caches to the OS. The data may still sit in a
     * writeback cache of the host OS, but it will survive a crash of the qemu
     * process.
     */
//    abstract public int bdrv_co_flush_to_os(Block.BlockDriverState bs);

    String protocol_name;
//    abstract public int bdrv_truncate(Block.BlockDriverState bs, long offset);

    abstract public long bdrv_getlength(Block.BlockDriverState bs);
    abstract public boolean has_bdrv_getlength();

//    abstract public long bdrv_get_allocated_file_size(Block.BlockDriverState bs);
//    abstract public int bdrv_write_compressed(Block.BlockDriverState bs, long sector_num, byte[] buf, int nb_sectors);

//    abstract public int bdrv_snapshot_create(Block.BlockDriverState bs, QEMUSnapshotInfo *sn_info);
//    abstract public int bdrv_snapshot_goto(Block.BlockDriverState bs, String snapshot_id);
//    abstract public int bdrv_snapshot_delete(Block.BlockDriverState bs, String snapshot_id);
//    abstract public int bdrv_snapshot_list(Block.BlockDriverState bs, QEMUSnapshotInfo **psn_info);
//    abstract public int bdrv_snapshot_load_tmp(Block.BlockDriverState bs, String snapshot_name);
//    abstract public int bdrv_get_info(Block.BlockDriverState bs, BlockDriverInfo *bdi);

//    abstract public int bdrv_save_vmstate(Block.BlockDriverState bs, const uint8_t *buf, long pos, int size);
//    abstract public int bdrv_load_vmstate(Block.BlockDriverState bs, uint8_t *buf, long pos, int size);

//    abstract public int bdrv_change_backing_file(Block.BlockDriverState bs, String backing_file, String backing_fmt);

    /* removable device specific */
    abstract public boolean bdrv_is_inserted(Block.BlockDriverState bs);
    abstract public boolean has_bdrv_is_inserted();

    abstract public int bdrv_media_changed(Block.BlockDriverState bs);
    abstract public boolean has_bdrv_media_changed();

    abstract public void bdrv_eject(Block.BlockDriverState bs, boolean eject_flag);
    abstract public boolean has_bdrv_eject();

    abstract public void bdrv_lock_medium(Block.BlockDriverState bs, boolean locked);
    abstract public boolean has_bdrv_lock_medium();

    /* to control generic scsi devices */
//    abstract public int bdrv_ioctl(Block.BlockDriverState bs, long req, byte[] buf);
//    abstract public Block.BlockDriverAIOCB bdrv_aio_ioctl(Block.BlockDriverState bs, long req, byte[] buf, Block.BlockDriverCompletionFunc cb, Object opaque);

    /* List of options for creating images, terminated by name == NULL */
//    QEMUOptionParameter *create_options;


    /*
     * Returns 0 for completed check, -errno for internal errors.
     * The check results are stored in result.
     */
//    abstract public int bdrv_check(Block.BlockDriverState bs, BdrvCheckResult *result, BdrvCheckMode fix);

//    abstract public void bdrv_debug_event(Block.BlockDriverState bs, BlkDebugEvent event);

    /*
     * Returns 1 if newly created images are guaranteed to contain only
     * zeros, 0 otherwise.
     */
//    abstract public int bdrv_has_zero_init(Block.BlockDriverState bs);

//    QLIST_ENTRY(BlockDriver) list;
}
