package jdos.hardware.qemu;

import jdos.util.IntRef;

import java.io.File;
import java.util.Vector;

public class Block {
    static public final int BIOS_ATA_TRANSLATION_AUTO =  0;
    static public final int BIOS_ATA_TRANSLATION_NONE =  1;
    static public final int BIOS_ATA_TRANSLATION_LBA =   2;
    static public final int BIOS_ATA_TRANSLATION_LARGE = 3;
    static public final int BIOS_ATA_TRANSLATION_RECHS = 4;

    static private Vector<BlockDriver> bdrv_drivers = new Vector<BlockDriver>();
    static private Vector<BlockDriverState> bdrv_states = new Vector<BlockDriverState>();
    static private String[] whitelist = new String[0];
    static private boolean use_bdrv_whitelist = false;

    static private final int NOT_DONE = 0x7fffffff; /* used while emulated sync operation in progress */

    static private final int BLOCK_DEVICE_IO_STATUS_OK = 0;

    static public final int BDRV_O_RDWR =         0x0002;
    static private final int BDRV_O_SNAPSHOT =     0x0008; /* open the file read only and save writes in a snapshot */
    static private final int BDRV_O_NOCACHE =      0x0020; /* do not use the host page cache */
    static private final int BDRV_O_CACHE_WB =     0x0040; /* use write-back caching */
    static private final int BDRV_O_NATIVE_AIO =   0x0080; /* use native AIO instead of the thread pool */
    static private final int BDRV_O_NO_BACKING =   0x0100; /* don't open the backing file */
    static private final int BDRV_O_NO_FLUSH =     0x0200; /* disable flushing on this disk */
    static private final int BDRV_O_COPY_ON_READ = 0x0400; /* copy read backing sectors into image */
    static private final int BDRV_O_INCOMING =     0x0800;  /* consistency hint for incoming migration */
    static private final int BDRV_O_CHECK =        0x1000;  /* open solely for consistency check */
    
    static private final int BDRV_O_CACHE_MASK =(BDRV_O_NOCACHE | BDRV_O_CACHE_WB | BDRV_O_NO_FLUSH);
        
    static public final int BDRV_SECTOR_BITS = 9;
    static public final int BDRV_SECTOR_SIZE = (1 << BDRV_SECTOR_BITS);
    static public final int BDRV_SECTOR_MASK = ~(BDRV_SECTOR_SIZE - 1);

    public static final int BDRV_ACCT_READ = 0;
    public static final int BDRV_ACCT_WRITE = 1;
    public static final int BDRV_ACCT_FLUSH = 2;
    public static final int BDRV_MAX_IOTYPE = 3;

    public static enum BlockErrorAction {BLOCK_ERR_REPORT, BLOCK_ERR_IGNORE, BLOCK_ERR_STOP_ENOSPC,BLOCK_ERR_STOP_ANY}

    public static enum BlockQMPEventAction {BDRV_ACTION_REPORT, BDRV_ACTION_IGNORE, BDRV_ACTION_STOP}

    static long get_clock() {
        return System.currentTimeMillis()*1000;
    }

    static public class BlockAcctCookie {
        long bytes;
        long start_time_ns;
        int type;
    }

    public interface BlockDriverCompletionFunc {
        void call(Object opaque, int ret);
    }

    static public class BlockDriverAIOCB {

    }

    /*
     * Note: the function bdrv_append() copies and swaps contents of
     * BlockDriverStates, so if you add new fields to this struct, please
     * inspect bdrv_append() to determine if the new fields need to be
     * copied as well.
     */
    static public class BlockDriverState {
        public long total_sectors; /* if we are reading a disk image, give its
                                  size in sectors */
        public boolean read_only; /* if true, the media is read only */
        public boolean keep_read_only; /* if true, the media was requested to stay read only */
        public int open_flags; /* flags used to open the file, re-used for re-open */
        public boolean encrypted; /* if true, the media is encrypted */
        public boolean valid_key; /* if true, a valid encryption key has been set */
        public boolean sg;        /* if true, the device is a /dev/sg* */
        public int copy_on_read; /* if true, copy read backing sectors into image
                             note this is a reference count */

        public BlockDriver drv; /* NULL means no media */
        public Object opaque;

        Object dev;                  /* attached device model, if any */
        /* TODO change to DeviceState when all users are qdevified */
        public BlockDevOps dev_ops;
        public Object dev_opaque;

        public String filename;
        public String backing_file=""; /* if non zero, the image is a diff of this file image */
        public String backing_format; /* if non-zero and backing_file exists */
        public boolean is_temporary;

        public BlockDriverState backing_hd;
        public BlockDriverState file;

        /* number of in-flight copy-on-read requests */
        public int copy_on_read_in_flight;

        /* the time for latest disk I/O */
        public long slice_time;
        public long slice_start;
        public long slice_end;
        //BlockIOLimit io_limits;
        //BlockIOBaseValue  io_base;
        //CoQueue      throttled_reqs;
        //QEMUTimer    *block_timer;
        public boolean  io_limits_enabled;

        /* I/O stats (display with "info blockstats"). */
        public long[] nr_bytes = new long[BDRV_MAX_IOTYPE];
        public long[] nr_ops = new long[BDRV_MAX_IOTYPE];
        public long[] total_time_ns = new long[BDRV_MAX_IOTYPE];
        public long wr_highest_sector;

        /* Whether the disk can expand beyond total_sectors */
        public boolean growable;

        /* the memory alignment required for the buffers handled by this driver */
        public int buffer_alignment;

        /* do we need to tell the quest if we have a volatile write cache? */
        public boolean enable_write_cache;

        /* NOTE: the following infos are only hints for real hardware
           drivers. They are not used by the block driver */
        public BlockErrorAction on_read_error, on_write_error;
        public boolean iostatus_enabled;
        public int iostatus;
        public String device_name;
        public long[] dirty_bitmap;
        public long dirty_count;
        public int in_use; /* users other than guest access, eg. block migration */
        //QTAILQ_ENTRY(BlockDriverState) list;

        //QLIST_HEAD(, BdrvTrackedRequest) tracked_requests;

        /* long-running background operation */
        //public BlockJob *job;
    }

    static private class BlockDriverStateRef {
        BlockDriverState value;
    }

    /* Callbacks for block device models */
    static public interface BlockDevOps {
        /*
         * Runs when virtual media changed (monitor commands eject, change)
         * Argument load is true on load and false on eject.
         * Beware: doesn't run when a host device's physical media
         * changes.  Sure would be useful if it did.
         * Device models with removable media must implement this callback.
         */
        public void change_media_cb(Object opaque, boolean load);
        public boolean has_change_media_cb();
        /*
         * Runs when an eject request is issued from the monitor, the tray
         * is closed, and the medium is locked.
         * Device models that do not implement is_medium_locked will not need
         * this callback.  Device models that can lock the medium or tray might
         * want to implement the callback and unlock the tray when "force" is
         * true, even if they do not support eject requests.
         */
        public void eject_request_cb(Object opaque, boolean force);
        public boolean has_eject_request_cb();
        /*
         * Is the virtual tray open?
         * Device models implement this only when the device has a tray.
         */
        public boolean is_tray_open(Object opaque);
        public boolean has_is_tray_open();
        /*
         * Is the virtual medium locked into the device?
         * Device models implement this only when device has such a lock.
         */
        public boolean is_medium_locked(Object opaque);
        public boolean has_is_medium_locked();
        /*
         * Runs when the size changed (e.g. monitor command block_resize)
         */
        public void resize_cb(Object opaque);
        public boolean has_resize_cb();
    }

//    /* throttling disk I/O limits */
//    static private void bdrv_io_limits_disable(BlockDriverState bs) {
//        bs.io_limits_enabled = false;
//
//        while (qemu_co_queue_next(&bs.throttled_reqs));
//
//        if (bs.block_timer) {
//            qemu_del_timer(bs.block_timer);
//            qemu_free_timer(bs.block_timer);
//            bs.block_timer = NULL;
//        }
//
//        bs.slice_start = 0;
//        bs.slice_end   = 0;
//        bs.slice_time  = 0;
//        memset(&bs.io_base, 0, sizeof(bs.io_base));
//    }
//
//    static private void bdrv_block_timer(Object opaque) {
//        BlockDriverState bs = (BlockDriverState)opaque;
//
//        qemu_co_queue_next(&bs.throttled_reqs);
//    }
//
//    static private void bdrv_io_limits_enable(BlockDriverState bs) {
//        qemu_co_queue_init(&bs.throttled_reqs);
//        bs.block_timer = qemu_new_timer_ns(vm_clock, bdrv_block_timer, bs);
//        bs.slice_time  = 5 * BLOCK_IO_SLICE_TIME;
//        bs.slice_start = qemu_get_clock_ns(vm_clock);
//        bs.slice_end   = bs.slice_start + bs.slice_time;
//        memset(&bs.io_base, 0, sizeof(bs.io_base));
//        bs.io_limits_enabled = true;
//    }
//
//    static private boolean bdrv_io_limits_enabled(BlockDriverState bs)
//    {
//        BlockIOLimit *io_limits = &bs.io_limits;
//        return io_limits.bps[BLOCK_IO_LIMIT_READ]
//             || io_limits.bps[BLOCK_IO_LIMIT_WRITE]
//             || io_limits.bps[BLOCK_IO_LIMIT_TOTAL]
//             || io_limits.iops[BLOCK_IO_LIMIT_READ]
//             || io_limits.iops[BLOCK_IO_LIMIT_WRITE]
//             || io_limits.iops[BLOCK_IO_LIMIT_TOTAL];
//    }
//
//    static private void bdrv_io_limits_intercept(BlockDriverState bs, boolean is_write, int nb_sectors)
//    {
//        long wait_time = -1;
//
//        if (!qemu_co_queue_empty(bs.throttled_reqs)) {
//            qemu_co_queue_wait(bs.throttled_reqs);
//        }
//
//        /* In fact, we hope to keep each request's timing, in FIFO mode. The next
//         * throttled requests will not be dequeued until the current request is
//         * allowed to be serviced. So if the current request still exceeds the
//         * limits, it will be inserted to the head. All requests followed it will
//         * be still in throttled_reqs queue.
//         */
//
//        while (bdrv_exceed_io_limits(bs, nb_sectors, is_write, wait_time)) {
//            qemu_mod_timer(bs.block_timer, wait_time + qemu_get_clock_ns(vm_clock));
//            qemu_co_queue_wait_insert_head(bs.throttled_reqs);
//        }
//
//        qemu_co_queue_next(bs.throttled_reqs);
//    }

    static private boolean is_windows_drive_prefix(String filename) {
        char c = filename.charAt(0);
        return (filename.length()>0 && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) && filename.charAt(1) == ':');
    }

    static private boolean is_windows_drive(String filename) {
        if (is_windows_drive_prefix(filename) && filename.length()==2)
            return true;
        if (filename.startsWith("\\\\.\\") || filename.startsWith("//./"))
            return true;
        return false;
    }

    /* check if the path starts with "<protocol>:" */
    static private boolean path_has_protocol(String path) {
        if (is_windows_drive(path) || is_windows_drive_prefix(path)) {
            return false;
        }
        int pos1 = path.indexOf(":");
        if (pos1<0)
            return false;
        int pos2 = path.indexOf("/");
        if (pos2>=0 && pos2<pos1)
            return false;
        pos2 = path.indexOf("\\");
        if (pos2>=0 && pos2<pos1)
            return false;
        return true;
    }

    static private boolean path_is_absolute(String path) {
        /* specific case for names like: "\\.\d:" */
        if (is_windows_drive(path) || is_windows_drive_prefix(path)) {
            return true;
        }
        return (path.length()>0 && path.charAt(0)=='/' || path.charAt(0)=='\\');
    }

    /* if filename is absolute, just copy it to dest. Otherwise, build a
       path to it by considering it is relative to base_path. URL are
       supported. */
    static private String path_combine(String base_path, String filename) {
        if (path_is_absolute(filename)) {
            return filename;
        } else {
            int pos = base_path.lastIndexOf("/");
            int pos2 = base_path.lastIndexOf("\\");
            if (pos>=0 && pos2>=0)
                base_path = base_path.substring(0, Math.max(pos, pos2));
            else if (pos>=0)
                base_path = base_path.substring(0, pos);
            else if (pos2>=0)
                base_path = base_path.substring(0, pos2);
            if (!base_path.endsWith("\\") && !base_path.endsWith("/"))
                base_path+= File.pathSeparator;
            return base_path+filename;
        }
    }

    static private String bdrv_get_full_backing_filename(BlockDriverState bs) {
        if (bs.backing_file.length()==0 || path_has_protocol(bs.backing_file)) {
            return bs.backing_file;
        } else {
            return path_combine(bs.filename, bs.backing_file);
        }
    }

    private static void bdrv_register(BlockDriver bdrv) {
        /* Block drivers without coroutine functions need emulation */
//        if (bdrv.bdrv_co_readv == null) {
//            bdrv.bdrv_co_readv = bdrv_co_readv_em;
//            bdrv.bdrv_co_writev = bdrv_co_writev_em;
//
//            /* bdrv_co_readv_em()/brdv_co_writev_em() work in terms of aio, so if
//             * the block driver lacks aio we need to emulate that too.
//             */
//            if (bdrv.bdrv_aio_readv == null) {
//                /* add AIO emulation layer */
//                bdrv.bdrv_aio_readv = bdrv_aio_readv_em;
//                bdrv.bdrv_aio_writev = bdrv_aio_writev_em;
//            }
//        }
        bdrv_drivers.insertElementAt(bdrv, 0);
    }

    /* create a new block device (by default it is empty) */
    static public BlockDriverState bdrv_new(String device_name)
    {
        BlockDriverState bs = new BlockDriverState();

        bs.device_name = device_name;
        if (device_name.length()==0) {
            bdrv_states.add(bs);
        }
        bdrv_iostatus_disable(bs);
        return bs;
    }

    static private BlockDriver bdrv_find_format(String format_name)
    {
        for (BlockDriver drv1: bdrv_drivers) {
            if (drv1.format_name.equals(format_name))
                return drv1;
        }
        return null;
    }

    static private boolean bdrv_is_whitelisted(BlockDriver drv) {
        if (whitelist.length==0)
            return true;               /* no whitelist, anything goes */
        for (String p: whitelist) {
            if (p.equals(drv.format_name))
                return true;
        }
        return false;
    }
//
//    static private BlockDriver bdrv_find_whitelisted_format(String format_name) {
//        BlockDriver drv = bdrv_find_format(format_name);
//        return drv!=null && bdrv_is_whitelisted(drv) ? drv : null;
//    }
//
//    static private class CreateCo {
//        public CreateCo(BlockDriver drv, String filename, Hashtable<String, String> options, int ret) {
//            this.drv = drv;
//            this.filename = filename;
//            this.options = options;
//            this.ret = ret;
//        }
//        BlockDriver drv;
//        String filename;
//        Hashtable<String, String> options = new Hashtable<String, String>();
//        int ret;
//    }
//
//    static private void bdrv_create_co_entry(Object opaque)
//    {
//        CreateCo cco = (CreateCo)opaque;
//        cco.ret = cco.drv.bdrv_create(cco.filename, cco.options);
//    }
//
//    static private int bdrv_create(BlockDriver drv, String filename, Hashtable<String, String> options) {
//        int ret;
//
//        Coroutine co;
//        CreateCo cco = new CreateCo(drv, filename, options, NOT_DONE);
//
//        if (drv.bdrv_create == null) {
//            return -Error.ENOTSUP;
//        }
//
//        if (qemu_in_coroutine()) {
//            /* Fast-path if already in coroutine context */
//            bdrv_create_co_entry(cco);
//        } else {
//            co = qemu_coroutine_create(bdrv_create_co_entry);
//            qemu_coroutine_enter(co, &cco);
//            while (cco.ret == NOT_DONE) {
//                qemu_aio_wait();
//            }
//        }
//
//        ret = cco.ret;
//        return ret;
//    }
//
//    static private int bdrv_create_file(String filename, Hashtable<String, String> options)
//    {
//        BlockDriver drv;
//
//        drv = bdrv_find_protocol(filename);
//        if (drv == null) {
//            return -Error.ENOENT;
//        }
//
//        return bdrv_create(drv, filename, options);
//    }
//
//    /*
//     * Create a uniquely-named empty temporary file.
//     * Return 0 upon success, otherwise a negative errno value.
//     */
//    int get_tmp_filename(char *filename, int size)
//    {
//    #ifdef _WIN32
//        char temp_dir[MAX_PATH];
//        /* GetTempFileName requires that its output buffer (4th param)
//           have length MAX_PATH or greater.  */
//        assert(size >= MAX_PATH);
//        return (GetTempPath(MAX_PATH, temp_dir)
//                && GetTempFileName(temp_dir, "qem", 0, filename)
//                ? 0 : -GetLastError());
//    #else
//        int fd;
//        const char *tmpdir;
//        tmpdir = getenv("TMPDIR");
//        if (!tmpdir)
//            tmpdir = "/tmp";
//        if (snprintf(filename, size, "%s/vl.XXXXXX", tmpdir) >= size) {
//            return -EOVERFLOW;
//        }
//        fd = mkstemp(filename);
//        if (fd < 0 || close(fd)) {
//            return -errno;
//        }
//        return 0;
//    #endif
//    }
//
//    /*
//     * Detect host devices. By convention, /dev/cdrom[N] is always
//     * recognized as a host CDROM.
//     */
//    static BlockDriver *find_hdev_driver(const char *filename)
//    {
//        int score_max = 0, score;
//        BlockDriver *drv = NULL, *d;
//
//        QLIST_FOREACH(d, &bdrv_drivers, list) {
//            if (d.bdrv_probe_device) {
//                score = d.bdrv_probe_device(filename);
//                if (score > score_max) {
//                    score_max = score;
//                    drv = d;
//                }
//            }
//        }
//
//        return drv;
//    }

    static private BlockDriver bdrv_find_protocol(String filename) {
        /* TODO Drivers without bdrv_file_open must be specified explicitly */

        /*
         * XXX(hch): we really should not let host device detection
         * override an explicit protocol specification, but moving this
         * later breaks access to device names with colons in them.
         * Thanks to the brain-dead persistent naming schemes on udev-
         * based Linux systems those actually are quite common.
         */
//        drv1 = find_hdev_driver(filename);
//        if (drv1) {
//            return drv1;
//        }

        if (!path_has_protocol(filename)) {
            return bdrv_find_format("file");
        }
        String protocol = filename.substring(0, filename.indexOf(":"));
        for (BlockDriver drv1: bdrv_drivers) {
            if (drv1.protocol_name.equals(protocol))
                return drv1;
        }
        return null;
    }

    static private class BlockDriverRef {
        BlockDriver value;
    }
    static private int find_image_format(String filename, BlockDriverRef pdrv)
    {
        int ret, score, score_max;
        BlockDriver drv;
        BlockDriverStateRef bs = new BlockDriverStateRef();

        ret = bdrv_file_open(bs, filename, 0);
        if (ret < 0) {
            pdrv.value = null;
            return ret;
        }

        /* Return the raw BlockDriver * to scsi-generic devices or empty drives */
        if (bs.value.sg || !bdrv_is_inserted(bs.value)) {
            bdrv_delete(bs.value);
            drv = bdrv_find_format("raw");
            if (drv == null) {
                ret = -Error.ENOENT;
            }
            pdrv.value = drv;
            return ret;
        }

        byte[] buf = new byte[2048];
        ret = bdrv_pread(bs.value, 0, buf, buf.length);
        bdrv_delete(bs.value);
        if (ret < 0) {
            pdrv.value = null;
            return ret;
        }

        score_max = 0;
        drv = null;
        for (BlockDriver drv1: bdrv_drivers) {
            if (drv1.has_bdrv_probe()) {
                score = drv1.bdrv_probe(buf, ret, filename);
                if (score > score_max) {
                    score_max = score;
                    drv = drv1;
                }
            }
        }
        if (drv==null) {
            ret = -Error.ENOENT;
        }
        pdrv.value = drv;
        return ret;
    }

    /**
     * Set the current 'total_sectors' value
     */
    static private int refresh_total_sectors(BlockDriverState bs, long hint) {
        BlockDriver drv = bs.drv;

        /* Do not attempt drv.bdrv_getlength() on scsi-generic devices */
        if (bs.sg)
            return 0;

        /* query actual device if possible, otherwise just trust the hint */
        if (drv.has_bdrv_getlength()) {
            long length = drv.bdrv_getlength(bs);
            if (length < 0) {
                return (int)length;
            }
            hint = length >> BDRV_SECTOR_BITS;
        }
        bs.total_sectors = hint;
        return 0;
    }

    /**
     * Set open flags for a given cache mode
     *
     * Return 0 on success, -1 if the cache mode was invalid.
     */
    static private int bdrv_parse_cache_flags(String mode, IntRef flags)
    {
        flags.value &= ~BDRV_O_CACHE_MASK;

        if (mode.equals("off") || mode.equals("none")) {
            flags.value |= BDRV_O_NOCACHE | BDRV_O_CACHE_WB;
        } else if (mode.equals("directsync")) {
            flags.value |= BDRV_O_NOCACHE;
        } else if (mode.equals("writeback")) {
            flags.value |= BDRV_O_CACHE_WB;
        } else if (mode.equals("unsafe")) {
            flags.value |= BDRV_O_CACHE_WB;
            flags.value |= BDRV_O_NO_FLUSH;
        } else if (mode.equals("writethrough")) {
            /* this is the default */
        } else {
            return -1;
        }
        return 0;
    }

    /**
     * The copy-on-read flag is actually a reference count so multiple users may
     * use the feature without worrying about clobbering its previous state.
     * Copy-on-read stays enabled until all users have called to disable it.
     */
    private static void bdrv_enable_copy_on_read(BlockDriverState bs) {
        bs.copy_on_read++;
    }

    private static void bdrv_disable_copy_on_read(BlockDriverState bs) {
        //assert(bs.copy_on_read > 0);
        bs.copy_on_read--;
    }

    /*
     * Common part for opening disk images and files
     */
    static private int bdrv_open_common(BlockDriverState bs, String filename, int flags, BlockDriver drv) {
        int ret, open_flags;

        //assert(drv != NULL);
        //assert(bs.file == NULL);

        //trace_bdrv_open_common(bs, filename, flags, drv.format_name);

        bs.open_flags = flags;
        bs.buffer_alignment = 512;

        //assert(bs.copy_on_read == 0); /* bdrv_new() and bdrv_close() make it so */
        if ((flags & BDRV_O_RDWR)!=0 && (flags & BDRV_O_COPY_ON_READ)!=0) {
            bdrv_enable_copy_on_read(bs);
        }

        bs.filename=filename;

        if (use_bdrv_whitelist && !bdrv_is_whitelisted(drv)) {
            return -Error.ENOTSUP;
        }

        bs.drv = drv;
        bs.opaque = drv.allocOpaque();

        bs.enable_write_cache = (flags & BDRV_O_CACHE_WB)!=0;
        open_flags = flags | BDRV_O_CACHE_WB;

        /*
         * Clear flags that are internal to the block layer before opening the
         * image.
         */
        open_flags &= ~(BDRV_O_SNAPSHOT | BDRV_O_NO_BACKING);

        /*
         * Snapshots should be writable.
         */
        if (bs.is_temporary) {
            open_flags |= BDRV_O_RDWR;
        }

        bs.keep_read_only = bs.read_only = (open_flags & BDRV_O_RDWR)==0;

        /* Open the image, either directly or using a protocol */
        if (drv.has_bdrv_file_open()) {
            ret = drv.bdrv_file_open(bs, filename, open_flags);
        } else {
            BlockDriverStateRef bds = new BlockDriverStateRef();
            ret = bdrv_file_open(bds, filename, open_flags);
            bs.file = bds.value;
            if (ret >= 0) {
                ret = drv.bdrv_open(bs, open_flags);
            }
        }

        if (ret>=0)
            ret = refresh_total_sectors(bs, bs.total_sectors);

        if (bs.is_temporary) {
            new File(filename).delete();
        }
        if (ret>=0)
            return 0;

        if (bs.file!=null) {
            bdrv_delete(bs.file);
            bs.file = null;
        }
        bs.opaque = null;
        bs.drv = null;
        return ret;
    }

    /*
     * Opens a file using a protocol (file, host_device, nbd, ...)
     */
    private static int bdrv_file_open(BlockDriverStateRef pbs, String filename, int flags)
    {
        BlockDriverState bs;
        BlockDriver drv;
        int ret;

        drv = bdrv_find_protocol(filename);
        if (drv == null) {
            return -Error.ENOENT;
        }

        bs = bdrv_new("");
        ret = bdrv_open_common(bs, filename, flags, drv);
        if (ret < 0) {
            bdrv_delete(bs);
            return ret;
        }
        bs.growable = true;
        pbs.value = bs;
        return 0;
    }

    /*
     * Opens a disk image (raw, qcow2, vmdk, ...)
     */
    static public int bdrv_open(BlockDriverState bs, String filename, int flags, BlockDriver drv) {
        int ret = 0;
        String tmp_filename;

//        if ((flags & BDRV_O_SNAPSHOT)!=0) {
//            BlockDriverState bs1;
//            long total_size;
//            boolean is_protocol = false;
//            BlockDriver bdrv_qcow2;
//            Hashtable<String, String> options;
//            String backing_filename;
//
//            /* if snapshot, we create a temporary backing file and open it
//               instead of opening 'filename' directly */
//
//            /* if there is a backing file, use it */
//            bs1 = bdrv_new("");
//            ret = bdrv_open(bs1, filename, 0, drv);
//            if (ret < 0) {
//                bdrv_delete(bs1);
//                return ret;
//            }
//            total_size = bdrv_getlength(bs1) & BDRV_SECTOR_MASK;
//
//            if (bs1.drv!=null && bs1.drv.protocol_name.length()>0)
//                is_protocol = true;
//
//            bdrv_delete(bs1);
//
//            ret = get_tmp_filename(tmp_filename, sizeof(tmp_filename));
//            if (ret < 0) {
//                return ret;
//            }
//
//            /* Real path is meaningless for protocols */
//            if (is_protocol)
//                backing_filename=filename;
//            else if (!realpath(filename, backing_filename))
//                return -errno;
//
//            bdrv_qcow2 = bdrv_find_format("qcow2");
//            options = parse_option_parameters("", bdrv_qcow2.create_options, NULL);
//
//            set_option_parameter_int(options, BLOCK_OPT_SIZE, total_size);
//            set_option_parameter(options, BLOCK_OPT_BACKING_FILE, backing_filename);
//            if (drv) {
//                set_option_parameter(options, BLOCK_OPT_BACKING_FMT,
//                    drv.format_name);
//            }
//
//            ret = bdrv_create(bdrv_qcow2, tmp_filename, options);
//            free_option_parameters(options);
//            if (ret < 0) {
//                return ret;
//            }
//
//            filename = tmp_filename;
//            drv = bdrv_qcow2;
//            bs.is_temporary = true;
//        }

        while (true) { // goto unlink_and_faile
        /* Find the right image format driver */
        if (drv==null) {
            BlockDriverRef bd = new BlockDriverRef();
            ret = find_image_format(filename, bd);
            drv = bd.value;
        }

        if (drv == null) {
            break;
        }

        /* Open the image */
        ret = bdrv_open_common(bs, filename, flags, drv);
        if (ret < 0) {
            break;
        }

        /* If there is a backing file, use it */
        if ((flags & BDRV_O_NO_BACKING) == 0 && bs.backing_file.length()>0) {
            String backing_filename;
            int back_flags;
            BlockDriver back_drv = null;

            bs.backing_hd = bdrv_new("");
            backing_filename = bdrv_get_full_backing_filename(bs);

            if (bs.backing_format.length()>0) {
                back_drv = bdrv_find_format(bs.backing_format);
            }

            /* backing files always opened read-only */
            back_flags =
                flags & ~(BDRV_O_RDWR | BDRV_O_SNAPSHOT | BDRV_O_NO_BACKING);

            ret = bdrv_open(bs.backing_hd, backing_filename, back_flags, back_drv);
            if (ret < 0) {
                bdrv_close(bs);
                return ret;
            }
            if (bs.is_temporary) {
                bs.backing_hd.keep_read_only = (flags & BDRV_O_RDWR)==0;
            } else {
                /* base image inherits from "parent" */
                bs.backing_hd.keep_read_only = bs.keep_read_only;
            }
        }

        //if (!bdrv_key_required(bs)) {
            bdrv_dev_change_media_cb(bs, true);
        //}

        /* throttling disk I/O limits */
        //if (bs.io_limits_enabled) {
        //    bdrv_io_limits_enable(bs);
        //}

        return 0;
        }
        // unlink_and_fail:
        if (bs.is_temporary) {
            new File(filename).delete();
        }
        return ret;
    }

    static private void bdrv_close(BlockDriverState bs)
    {}
//        bdrv_flush(bs);
//        if (bs.drv!=null) {
//            if (bs.job!=null) {
//                block_job_cancel_sync(bs.job);
//            }
//            bdrv_drain_all();
//
//            if (bs == bs_snapshots) {
//                bs_snapshots = NULL;
//            }
//            if (bs.backing_hd!=null) {
//                bdrv_delete(bs.backing_hd);
//                bs.backing_hd = null;
//            }
//            bs.drv.bdrv_close(bs);
//            if (bs.is_temporary) {
//                new File(bs.filename).delete();
//            }
//            bs.opaque = null;
//            bs.drv = null;
//            bs.copy_on_read = 0;
//            bs.backing_file = "";
//            bs.backing_format = "";
//            bs.total_sectors = 0;
//            bs.encrypted = false;
//            bs.valid_key = false;
//            bs.sg = false;
//            bs.growable = 0;
//
//            if (bs.file != null) {
//                bdrv_delete(bs.file);
//                bs.file = null;
//            }
//
//            bdrv_dev_change_media_cb(bs, false);
//        }
//
//        /*throttling disk I/O limits*/
//        if (bs.io_limits_enabled) {
//            bdrv_io_limits_disable(bs);
//        }
//    }
//
//    static private void bdrv_close_all() {
//        for (BlockDriverState bs: bdrv_states)
//            bdrv_close(bs);
//    }

    /*
     * Wait for pending requests to complete across all BlockDriverStates
     *
     * This function does not flush data to disk, use bdrv_flush_all() for that
     * after calling this function.
     *
     * Note that completion of an asynchronous I/O operation can trigger any
     * number of other I/O operations on other devices---for example a coroutine
     * can be arbitrarily complex and a constant flow of I/O can come until the
     * coroutine is complete.  Because of this, it is not possible to have a
     * function to drain a single device's I/O queue.
     */
//    static private void bdrv_drain_all() {
//        BlockDriverState bs;
//        boolean busy;
//
//        do {
//            busy = qemu_aio_wait();
//
//            /* FIXME: We do not have timer support here, so this is effectively
//             * a busy wait.
//             */
//            QTAILQ_FOREACH(bs, &bdrv_states, list) {
//                if (!qemu_co_queue_empty(&bs.throttled_reqs)) {
//                    qemu_co_queue_restart_all(&bs.throttled_reqs);
//                    busy = true;
//                }
//            }
//        } while (busy);
//
//        /* If requests are still pending there is a bug somewhere */
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            assert(QLIST_EMPTY(&bs.tracked_requests));
//            assert(qemu_co_queue_empty(&bs.throttled_reqs));
//        }
//    }

    /* make a BlockDriverState anonymous by removing from bdrv_state list.
       Also, NULL terminate the device_name to prevent double remove */
    static private void bdrv_make_anon(BlockDriverState bs) {
        if (bs.device_name.length()!=0) {
            bdrv_states.remove(bs);
        }
        bs.device_name="";
    }

//    static void bdrv_rebind(BlockDriverState bs)
//    {
//        if (bs.drv && bs.drv.bdrv_rebind) {
//            bs.drv.bdrv_rebind(bs);
//        }
//    }
//
//    static void bdrv_move_feature_fields(BlockDriverState bs_dest,
//                                         BlockDriverState bs_src)
//    {
//        /* move some fields that need to stay attached to the device */
//        bs_dest.open_flags         = bs_src.open_flags;
//
//        /* dev info */
//        bs_dest.dev_ops            = bs_src.dev_ops;
//        bs_dest.dev_opaque         = bs_src.dev_opaque;
//        bs_dest.dev                = bs_src.dev;
//        bs_dest.buffer_alignment   = bs_src.buffer_alignment;
//        bs_dest.copy_on_read       = bs_src.copy_on_read;
//
//        bs_dest.enable_write_cache = bs_src.enable_write_cache;
//
//        /* i/o timing parameters */
//        bs_dest.slice_time         = bs_src.slice_time;
//        bs_dest.slice_start        = bs_src.slice_start;
//        bs_dest.slice_end          = bs_src.slice_end;
//        bs_dest.io_limits          = bs_src.io_limits;
//        bs_dest.io_base            = bs_src.io_base;
//        bs_dest.throttled_reqs     = bs_src.throttled_reqs;
//        bs_dest.block_timer        = bs_src.block_timer;
//        bs_dest.io_limits_enabled  = bs_src.io_limits_enabled;
//
//        /* r/w error */
//        bs_dest.on_read_error      = bs_src.on_read_error;
//        bs_dest.on_write_error     = bs_src.on_write_error;
//
//        /* i/o status */
//        bs_dest.iostatus_enabled   = bs_src.iostatus_enabled;
//        bs_dest.iostatus           = bs_src.iostatus;
//
//        /* dirty bitmap */
//        bs_dest.dirty_count        = bs_src.dirty_count;
//        bs_dest.dirty_bitmap       = bs_src.dirty_bitmap;
//
//        /* job */
//        bs_dest.in_use             = bs_src.in_use;
//        bs_dest.job                = bs_src.job;
//
//        /* keep the same entry in bdrv_states */
//        bs_dest.device_name = bs_src.device_name;
//        bs_dest.list = bs_src.list;
//    }
//
//    /*
//     * Swap bs contents for two image chains while they are live,
//     * while keeping required fields on the BlockDriverState that is
//     * actually attached to a device.
//     *
//     * This will modify the BlockDriverState fields, and swap contents
//     * between bs_new and bs_old. Both bs_new and bs_old are modified.
//     *
//     * bs_new is required to be anonymous.
//     *
//     * This function does not create any image files.
//     */
//    void bdrv_swap(BlockDriverState bs_new, BlockDriverState bs_old)
//    {
//        BlockDriverState tmp;
//
//        /* bs_new must be anonymous and shouldn't have anything fancy enabled */
//        assert(bs_new.device_name[0] == '\0');
//        assert(bs_new.dirty_bitmap == NULL);
//        assert(bs_new.job == NULL);
//        assert(bs_new.dev == NULL);
//        assert(bs_new.in_use == 0);
//        assert(bs_new.io_limits_enabled == false);
//        assert(bs_new.block_timer == NULL);
//
//        tmp = *bs_new;
//        *bs_new = *bs_old;
//        *bs_old = tmp;
//
//        /* there are some fields that should not be swapped, move them back */
//        bdrv_move_feature_fields(&tmp, bs_old);
//        bdrv_move_feature_fields(bs_old, bs_new);
//        bdrv_move_feature_fields(bs_new, &tmp);
//
//        /* bs_new shouldn't be in bdrv_states even after the swap!  */
//        assert(bs_new.device_name[0] == '\0');
//
//        /* Check a few fields that should remain attached to the device */
//        assert(bs_new.dev == NULL);
//        assert(bs_new.job == NULL);
//        assert(bs_new.in_use == 0);
//        assert(bs_new.io_limits_enabled == false);
//        assert(bs_new.block_timer == NULL);
//
//        bdrv_rebind(bs_new);
//        bdrv_rebind(bs_old);
//    }
//
//    /*
//     * Add new bs contents at the top of an image chain while the chain is
//     * live, while keeping required fields on the top layer.
//     *
//     * This will modify the BlockDriverState fields, and swap contents
//     * between bs_new and bs_top. Both bs_new and bs_top are modified.
//     *
//     * bs_new is required to be anonymous.
//     *
//     * This function does not create any image files.
//     */
//    void bdrv_append(BlockDriverState bs_new, BlockDriverState bs_top)
//    {
//        bdrv_swap(bs_new, bs_top);
//
//        /* The contents of 'tmp' will become bs_top, as we are
//         * swapping bs_new and bs_top contents. */
//        bs_top.backing_hd = bs_new;
//        bs_top.open_flags &= ~BDRV_O_NO_BACKING;
//        pstrcpy(bs_top.backing_file, sizeof(bs_top.backing_file),
//                bs_new.filename);
//        pstrcpy(bs_top.backing_format, sizeof(bs_top.backing_format),
//                bs_new.drv ? bs_new.drv.format_name : "");
//    }

    static private void bdrv_delete(BlockDriverState bs)
    {
        //assert(!bs.dev);
        //assert(!bs.job);
        //assert(!bs.in_use);

        /* remove from list, if necessary */
        bdrv_make_anon(bs);

        bdrv_close(bs);

        //assert(bs != bs_snapshots);
        //g_free(bs);
    }
//
//    static private int bdrv_attach_dev(BlockDriverState bs, Object dev)
//    /* TODO change to DeviceState *dev when all users are qdevified */
//    {
//        if (bs.dev!=null) {
//            return -Error.EBUSY;
//        }
//        bs.dev = dev;
//        bdrv_iostatus_reset(bs);
//        return 0;
//    }
//
//    /* TODO qdevified devices don't use this, remove when devices are qdevified */
//    void bdrv_attach_dev_nofail(BlockDriverState bs, void *dev)
//    {
//        if (bdrv_attach_dev(bs, dev) < 0) {
//            abort();
//        }
//    }
//
//    static private void bdrv_detach_dev(BlockDriverState bs, Object dev)
//    /* TODO change to DeviceState *dev when all users are qdevified */
//    {
//        //assert(bs.dev == dev);
//        bs.dev = null;
//        bs.dev_ops = null;
//        bs.dev_opaque = null;
//        bs.buffer_alignment = 512;
//    }
//
//    /* TODO change to return DeviceState * when all users are qdevified */
//    static private Object bdrv_get_attached_dev(BlockDriverState bs)
//    {
//        return bs.dev;
//    }

    static public void bdrv_set_dev_ops(BlockDriverState bs, BlockDevOps ops, Object opaque)
    {
        bs.dev_ops = ops;
        bs.dev_opaque = opaque;
//        if (bdrv_dev_has_removable_media(bs) && bs == bs_snapshots) {
//            bs_snapshots = NULL;
//        }
    }

    static public void bdrv_emit_qmp_error_event(BlockDriverState bdrv, BlockQMPEventAction action, boolean is_read) {
//        QObject *data;
//        const char *action_str;
//
//        switch (action) {
//        case BDRV_ACTION_REPORT:
//            action_str = "report";
//            break;
//        case BDRV_ACTION_IGNORE:
//            action_str = "ignore";
//            break;
//        case BDRV_ACTION_STOP:
//            action_str = "stop";
//            break;
//        default:
//            abort();
//        }
//
//        data = qobject_from_jsonf("{ 'device': %s, 'action': %s, 'operation': %s }",
//                                  bdrv.device_name,
//                                  action_str,
//                                  is_read ? "read" : "write");
//        monitor_protocol_event(QEVENT_BLOCK_IO_ERROR, data);
//
//        qobject_decref(data);
    }

    static private void bdrv_emit_qmp_eject_event(BlockDriverState bs, boolean ejected)
    {
//        QObject *data;
//
//        data = qobject_from_jsonf("{ 'device': %s, 'tray-open': %i }",
//                                  bdrv_get_device_name(bs), ejected);
//        monitor_protocol_event(QEVENT_DEVICE_TRAY_MOVED, data);
//
//        qobject_decref(data);
    }

    static void bdrv_dev_change_media_cb(BlockDriverState bs, boolean load) {
        if (bs.dev_ops!=null && bs.dev_ops.has_change_media_cb()) {
            boolean tray_was_closed = !bdrv_dev_is_tray_open(bs);
            bs.dev_ops.change_media_cb(bs.dev_opaque, load);
            if (tray_was_closed) {
                /* tray open */
                bdrv_emit_qmp_eject_event(bs, true);
            }
            if (load) {
                /* tray close */
                bdrv_emit_qmp_eject_event(bs, false);
            }
        }
    }

    static private boolean bdrv_dev_has_removable_media(BlockDriverState bs)
    {
        return bs.dev==null || (bs.dev_ops!=null && bs.dev_ops.has_change_media_cb());
    }

    static private void bdrv_dev_eject_request(BlockDriverState bs, boolean force) {
        if (bs.dev_ops!=null && bs.dev_ops.has_eject_request_cb()) {
            bs.dev_ops.eject_request_cb(bs.dev_opaque, force);
        }
    }

    static private boolean bdrv_dev_is_tray_open(BlockDriverState bs)
    {
        if (bs.dev_ops!=null && bs.dev_ops.has_is_tray_open()) {
            return bs.dev_ops.is_tray_open(bs.dev_opaque);
        }
        return false;
    }

    static private void bdrv_dev_resize_cb(BlockDriverState bs) {
        if (bs.dev_ops!=null && bs.dev_ops.has_resize_cb()) {
            bs.dev_ops.resize_cb(bs.dev_opaque);
        }
    }

    static private boolean bdrv_dev_is_medium_locked(BlockDriverState bs)
    {
        if (bs.dev_ops!=null && bs.dev_ops.has_is_medium_locked()) {
            return bs.dev_ops.is_medium_locked(bs.dev_opaque);
        }
        return false;
    }

//    /*
//     * Run consistency checks on an image
//     *
//     * Returns 0 if the check could be completed (it doesn't mean that the image is
//     * free of errors) or -errno when an internal error occurred. The results of the
//     * check are stored in res.
//     */
//    int bdrv_check(BlockDriverState bs, BdrvCheckResult *res, BdrvCheckMode fix)
//    {
//        if (bs.drv.bdrv_check == NULL) {
//            return -ENOTSUP;
//        }
//
//        memset(res, 0, sizeof(*res));
//        return bs.drv.bdrv_check(bs, res, fix);
//    }
//
//    static private final int COMMIT_BUF_SECTORS 2048
//
//    /* commit COW file into the raw image */
//    int bdrv_commit(BlockDriverState bs)
//    {
//        BlockDriver *drv = bs.drv;
//        BlockDriver *backing_drv;
//        int64_t sector, total_sectors;
//        int n, ro, open_flags;
//        int ret = 0, rw_ret = 0;
//        uint8_t *buf;
//        char filename[1024];
//        BlockDriverState bs_rw, *bs_ro;
//
//        if (!drv)
//            return -ENOMEDIUM;
//
//        if (!bs.backing_hd) {
//            return -ENOTSUP;
//        }
//
//        if (bs.backing_hd.keep_read_only) {
//            return -EACCES;
//        }
//
//        if (bdrv_in_use(bs) || bdrv_in_use(bs.backing_hd)) {
//            return -EBUSY;
//        }
//
//        backing_drv = bs.backing_hd.drv;
//        ro = bs.backing_hd.read_only;
//        strncpy(filename, bs.backing_hd.filename, sizeof(filename));
//        open_flags =  bs.backing_hd.open_flags;
//
//        if (ro) {
//            /* re-open as RW */
//            bdrv_delete(bs.backing_hd);
//            bs.backing_hd = NULL;
//            bs_rw = bdrv_new("");
//            rw_ret = bdrv_open(bs_rw, filename, open_flags | BDRV_O_RDWR,
//                backing_drv);
//            if (rw_ret < 0) {
//                bdrv_delete(bs_rw);
//                /* try to re-open read-only */
//                bs_ro = bdrv_new("");
//                ret = bdrv_open(bs_ro, filename, open_flags & ~BDRV_O_RDWR,
//                    backing_drv);
//                if (ret < 0) {
//                    bdrv_delete(bs_ro);
//                    /* drive not functional anymore */
//                    bs.drv = NULL;
//                    return ret;
//                }
//                bs.backing_hd = bs_ro;
//                return rw_ret;
//            }
//            bs.backing_hd = bs_rw;
//        }
//
//        total_sectors = bdrv_getlength(bs) >> BDRV_SECTOR_BITS;
//        buf = g_malloc(COMMIT_BUF_SECTORS * BDRV_SECTOR_SIZE);
//
//        for (sector = 0; sector < total_sectors; sector += n) {
//            if (bdrv_is_allocated(bs, sector, COMMIT_BUF_SECTORS, &n)) {
//
//                if (bdrv_read(bs, sector, buf, n) != 0) {
//                    ret = -EIO;
//                    goto ro_cleanup;
//                }
//
//                if (bdrv_write(bs.backing_hd, sector, buf, n) != 0) {
//                    ret = -EIO;
//                    goto ro_cleanup;
//                }
//            }
//        }
//
//        if (drv.bdrv_make_empty) {
//            ret = drv.bdrv_make_empty(bs);
//            bdrv_flush(bs);
//        }
//
//        /*
//         * Make sure all data we wrote to the backing device is actually
//         * stable on disk.
//         */
//        if (bs.backing_hd)
//            bdrv_flush(bs.backing_hd);
//
//    ro_cleanup:
//        g_free(buf);
//
//        if (ro) {
//            /* re-open as RO */
//            bdrv_delete(bs.backing_hd);
//            bs.backing_hd = NULL;
//            bs_ro = bdrv_new("");
//            ret = bdrv_open(bs_ro, filename, open_flags & ~BDRV_O_RDWR,
//                backing_drv);
//            if (ret < 0) {
//                bdrv_delete(bs_ro);
//                /* drive not functional anymore */
//                bs.drv = NULL;
//                return ret;
//            }
//            bs.backing_hd = bs_ro;
//            bs.backing_hd.keep_read_only = 0;
//        }
//
//        return ret;
//    }
//
//    int bdrv_commit_all(void)
//    {
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            int ret = bdrv_commit(bs);
//            if (ret < 0) {
//                return ret;
//            }
//        }
//        return 0;
//    }
//
//    struct BdrvTrackedRequest {
//        BlockDriverState bs;
//        int64_t sector_num;
//        int nb_sectors;
//        bool is_write;
//        QLIST_ENTRY(BdrvTrackedRequest) list;
//        Coroutine *co; /* owner, used for deadlock detection */
//        CoQueue wait_queue; /* coroutines blocked on this request */
//    };
//
//    /**
//     * Remove an active request from the tracked requests list
//     *
//     * This function should be called when a tracked request is completing.
//     */
//    static void tracked_request_end(BdrvTrackedRequest *req)
//    {
//        QLIST_REMOVE(req, list);
//        qemu_co_queue_restart_all(&req.wait_queue);
//    }
//
//    /**
//     * Add an active request to the tracked requests list
//     */
//    static void tracked_request_begin(BdrvTrackedRequest *req,
//                                      BlockDriverState bs,
//                                      int64_t sector_num,
//                                      int nb_sectors, bool is_write)
//    {
//        *req = (BdrvTrackedRequest){
//            .bs = bs,
//            .sector_num = sector_num,
//            .nb_sectors = nb_sectors,
//            .is_write = is_write,
//            .co = qemu_coroutine_self(),
//        };
//
//        qemu_co_queue_init(&req.wait_queue);
//
//        QLIST_INSERT_HEAD(&bs.tracked_requests, req, list);
//    }
//
//    /**
//     * Round a region to cluster boundaries
//     */
//    static void round_to_clusters(BlockDriverState bs,
//                                  int64_t sector_num, int nb_sectors,
//                                  int64_t *cluster_sector_num,
//                                  int *cluster_nb_sectors)
//    {
//        BlockDriverInfo bdi;
//
//        if (bdrv_get_info(bs, &bdi) < 0 || bdi.cluster_size == 0) {
//            *cluster_sector_num = sector_num;
//            *cluster_nb_sectors = nb_sectors;
//        } else {
//            int64_t c = bdi.cluster_size / BDRV_SECTOR_SIZE;
//            *cluster_sector_num = QEMU_ALIGN_DOWN(sector_num, c);
//            *cluster_nb_sectors = QEMU_ALIGN_UP(sector_num - *cluster_sector_num +
//                                                nb_sectors, c);
//        }
//    }
//
//    static bool tracked_request_overlaps(BdrvTrackedRequest *req,
//                                         int64_t sector_num, int nb_sectors) {
//        /*        aaaa   bbbb */
//        if (sector_num >= req.sector_num + req.nb_sectors) {
//            return false;
//        }
//        /* bbbb   aaaa        */
//        if (req.sector_num >= sector_num + nb_sectors) {
//            return false;
//        }
//        return true;
//    }
//
//    static void coroutine_fn wait_for_overlapping_requests(BlockDriverState bs,
//            int64_t sector_num, int nb_sectors)
//    {
//        BdrvTrackedRequest *req;
//        int64_t cluster_sector_num;
//        int cluster_nb_sectors;
//        bool retry;
//
//        /* If we touch the same cluster it counts as an overlap.  This guarantees
//         * that allocating writes will be serialized and not race with each other
//         * for the same cluster.  For example, in copy-on-read it ensures that the
//         * CoR read and write operations are atomic and guest writes cannot
//         * interleave between them.
//         */
//        round_to_clusters(bs, sector_num, nb_sectors,
//                          &cluster_sector_num, &cluster_nb_sectors);
//
//        do {
//            retry = false;
//            QLIST_FOREACH(req, &bs.tracked_requests, list) {
//                if (tracked_request_overlaps(req, cluster_sector_num,
//                                             cluster_nb_sectors)) {
//                    /* Hitting this means there was a reentrant request, for
//                     * example, a block driver issuing nested requests.  This must
//                     * never happen since it means deadlock.
//                     */
//                    assert(qemu_coroutine_self() != req.co);
//
//                    qemu_co_queue_wait(&req.wait_queue);
//                    retry = true;
//                    break;
//                }
//            }
//        } while (retry);
//    }
//
//    /*
//     * Return values:
//     * 0        - success
//     * -EINVAL  - backing format specified, but no file
//     * -ENOSPC  - can't update the backing file because no space is left in the
//     *            image file header
//     * -ENOTSUP - format driver doesn't support changing the backing file
//     */
//    int bdrv_change_backing_file(BlockDriverState bs,
//        const char *backing_file, const char *backing_fmt)
//    {
//        BlockDriver *drv = bs.drv;
//        int ret;
//
//        /* Backing file format doesn't make sense without a backing file */
//        if (backing_fmt && !backing_file) {
//            return -EINVAL;
//        }
//
//        if (drv.bdrv_change_backing_file != NULL) {
//            ret = drv.bdrv_change_backing_file(bs, backing_file, backing_fmt);
//        } else {
//            ret = -ENOTSUP;
//        }
//
//        if (ret == 0) {
//            pstrcpy(bs.backing_file, sizeof(bs.backing_file), backing_file ?: "");
//            pstrcpy(bs.backing_format, sizeof(bs.backing_format), backing_fmt ?: "");
//        }
//        return ret;
//    }
//
//    static int bdrv_check_byte_request(BlockDriverState bs, int64_t offset,
//                                       size_t size)
//    {
//        int64_t len;
//
//        if (!bdrv_is_inserted(bs))
//            return -ENOMEDIUM;
//
//        if (bs.growable)
//            return 0;
//
//        len = bdrv_getlength(bs);
//
//        if (offset < 0)
//            return -EIO;
//
//        if ((offset > len) || (len - offset < size))
//            return -EIO;
//
//        return 0;
//    }
//
//    static int bdrv_check_request(BlockDriverState bs, int64_t sector_num,
//                                  int nb_sectors)
//    {
//        return bdrv_check_byte_request(bs, sector_num * BDRV_SECTOR_SIZE,
//                                       nb_sectors * BDRV_SECTOR_SIZE);
//    }
//
//    typedef struct RwCo {
//        BlockDriverState bs;
//        int64_t sector_num;
//        int nb_sectors;
//        QEMUIOVector *qiov;
//        bool is_write;
//        int ret;
//    } RwCo;
//
//    static void coroutine_fn bdrv_rw_co_entry(void *opaque)
//    {
//        RwCo *rwco = opaque;
//
//        if (!rwco.is_write) {
//            rwco.ret = bdrv_co_do_readv(rwco.bs, rwco.sector_num,
//                                         rwco.nb_sectors, rwco.qiov, 0);
//        } else {
//            rwco.ret = bdrv_co_do_writev(rwco.bs, rwco.sector_num,
//                                          rwco.nb_sectors, rwco.qiov, 0);
//        }
//    }

//    /*
//     * Process a synchronous request using coroutines
//     */
//    static int bdrv_rw_co(BlockDriverState bs, int64_t sector_num, uint8_t *buf, int nb_sectors, bool is_write)
//    {
//        QEMUIOVector qiov;
//        struct iovec iov = {
//            .iov_base = (void *)buf,
//            .iov_len = nb_sectors * BDRV_SECTOR_SIZE,
//        };
//        Coroutine *co;
//        RwCo rwco = {
//            .bs = bs,
//            .sector_num = sector_num,
//            .nb_sectors = nb_sectors,
//            .qiov = &qiov,
//            .is_write = is_write,
//            .ret = NOT_DONE,
//        };
//
//        qemu_iovec_init_external(&qiov, &iov, 1);
//
//        /**
//         * In sync call context, when the vcpu is blocked, this throttling timer
//         * will not fire; so the I/O throttling function has to be disabled here
//         * if it has been enabled.
//         */
//        if (bs.io_limits_enabled) {
//            fprintf(stderr, "Disabling I/O throttling on '%s' due "
//                            "to synchronous I/O.\n", bdrv_get_device_name(bs));
//            bdrv_io_limits_disable(bs);
//        }
//
//        if (qemu_in_coroutine()) {
//            /* Fast-path if already in coroutine context */
//            bdrv_rw_co_entry(&rwco);
//        } else {
//            co = qemu_coroutine_create(bdrv_rw_co_entry);
//            qemu_coroutine_enter(co, &rwco);
//            while (rwco.ret == NOT_DONE) {
//                qemu_aio_wait();
//            }
//        }
//        return rwco.ret;
//    }

    /* return < 0 if error. See bdrv_write() for the return codes */
    static public int bdrv_read(BlockDriverState bs, long sector_num, byte[] buf, int buffOffset, int nb_sectors)
    {
        return bs.drv.bdrv_read(bs, sector_num, buf, buffOffset, nb_sectors);
        //return bdrv_rw_co(bs, sector_num, buf, nb_sectors, false);
    }

    /* Just like bdrv_read(), but with I/O throttling temporarily disabled */
    static int bdrv_read_unthrottled(BlockDriverState bs, long sector_num, byte[] buf, int nb_sectors)
    {
        boolean enabled;
        int ret;

        enabled = bs.io_limits_enabled;
        bs.io_limits_enabled = false;
        ret = bdrv_read(bs, 0, buf, 0, 1);
        bs.io_limits_enabled = enabled;
        return ret;
    }

//    static private final int BITS_PER_LONG  (sizeof(unsigned long) * 8)
//
//    static void set_dirty_bitmap(BlockDriverState bs, int64_t sector_num,
//                                 int nb_sectors, int dirty)
//    {
//        int64_t start, end;
//        unsigned long val, idx, bit;
//
//        start = sector_num / BDRV_SECTORS_PER_DIRTY_CHUNK;
//        end = (sector_num + nb_sectors - 1) / BDRV_SECTORS_PER_DIRTY_CHUNK;
//
//        for (; start <= end; start++) {
//            idx = start / BITS_PER_LONG;
//            bit = start % BITS_PER_LONG;
//            val = bs.dirty_bitmap[idx];
//            if (dirty) {
//                if (!(val & (1UL << bit))) {
//                    bs.dirty_count++;
//                    val |= 1UL << bit;
//                }
//            } else {
//                if (val & (1UL << bit)) {
//                    bs.dirty_count--;
//                    val &= ~(1UL << bit);
//                }
//            }
//            bs.dirty_bitmap[idx] = val;
//        }
//    }
//
    /* Return < 0 if error. Important errors are:
      -EIO         generic I/O error (may happen for all errors)
      -ENOMEDIUM   No media inserted.
      -EINVAL      Invalid sector number or nb_sectors
      -EACCES      Trying to write a read-only device
    */
    static public int bdrv_write(BlockDriverState bs, long sector_num, byte[] buf, int offset, int nb_sectors)
    {
        return bs.drv.bdrv_write(bs, sector_num, buf, offset, nb_sectors);
        //return bdrv_rw_co(bs, sector_num, (uint8_t *)buf, nb_sectors, true);
    }

    static private int bdrv_pread(BlockDriverState bs, int offset, byte[] buf, int count1) {
        byte[] tmp_buf = new byte[BDRV_SECTOR_SIZE];
        int len, nb_sectors, count;
        long sector_num;
        int ret;

        count = count1;
        /* first read to align to sector start */
        len = ((BDRV_SECTOR_SIZE - offset) & (BDRV_SECTOR_SIZE - 1));
        if (len > count)
            len = count;
        sector_num = offset >> BDRV_SECTOR_BITS;
        int bufOffset = 0;
        if (len > 0) {
            if ((ret = bdrv_read(bs, sector_num, tmp_buf, 0, 1)) < 0)
                return ret;
            System.arraycopy(tmp_buf, (offset & (BDRV_SECTOR_SIZE - 1)) ,buf, 0, len);
            count -= len;
            if (count == 0)
                return count1;
            sector_num++;
            bufOffset = len;
        }

        /* read the sectors "in place" */
        nb_sectors = count >> BDRV_SECTOR_BITS;
        if (nb_sectors > 0) {
            if ((ret = bdrv_read(bs, sector_num, buf, bufOffset, nb_sectors)) < 0)
                return ret;
            sector_num += nb_sectors;
            len = nb_sectors << BDRV_SECTOR_BITS;
            bufOffset+=len;
            count -= len;
        }

        /* add data from the last sector */
        if (count > 0) {
            if ((ret = bdrv_read(bs, sector_num, tmp_buf, 0, 1)) < 0)
                return ret;
            System.arraycopy(tmp_buf, 0 ,buf, bufOffset, count);
        }
        return count1;
    }

//    int bdrv_pwrite(BlockDriverState bs, int64_t offset,
//                    const void *buf, int count1)
//    {
//        uint8_t tmp_buf[BDRV_SECTOR_SIZE];
//        int len, nb_sectors, count;
//        int64_t sector_num;
//        int ret;
//
//        count = count1;
//        /* first write to align to sector start */
//        len = (BDRV_SECTOR_SIZE - offset) & (BDRV_SECTOR_SIZE - 1);
//        if (len > count)
//            len = count;
//        sector_num = offset >> BDRV_SECTOR_BITS;
//        if (len > 0) {
//            if ((ret = bdrv_read(bs, sector_num, tmp_buf, 1)) < 0)
//                return ret;
//            memcpy(tmp_buf + (offset & (BDRV_SECTOR_SIZE - 1)), buf, len);
//            if ((ret = bdrv_write(bs, sector_num, tmp_buf, 1)) < 0)
//                return ret;
//            count -= len;
//            if (count == 0)
//                return count1;
//            sector_num++;
//            buf += len;
//        }
//
//        /* write the sectors "in place" */
//        nb_sectors = count >> BDRV_SECTOR_BITS;
//        if (nb_sectors > 0) {
//            if ((ret = bdrv_write(bs, sector_num, buf, nb_sectors)) < 0)
//                return ret;
//            sector_num += nb_sectors;
//            len = nb_sectors << BDRV_SECTOR_BITS;
//            buf += len;
//            count -= len;
//        }
//
//        /* add data from the last sector */
//        if (count > 0) {
//            if ((ret = bdrv_read(bs, sector_num, tmp_buf, 1)) < 0)
//                return ret;
//            memcpy(tmp_buf, buf, count);
//            if ((ret = bdrv_write(bs, sector_num, tmp_buf, 1)) < 0)
//                return ret;
//        }
//        return count1;
//    }
//
//    /*
//     * Writes to the file and ensures that no writes are reordered across this
//     * request (acts as a barrier)
//     *
//     * Returns 0 on success, -errno in error cases.
//     */
//    int bdrv_pwrite_sync(BlockDriverState bs, int64_t offset,
//        const void *buf, int count)
//    {
//        int ret;
//
//        ret = bdrv_pwrite(bs, offset, buf, count);
//        if (ret < 0) {
//            return ret;
//        }
//
//        /* No flush needed for cache modes that already do it */
//        if (bs.enable_write_cache) {
//            bdrv_flush(bs);
//        }
//
//        return 0;
//    }

//    static int coroutine_fn bdrv_co_do_copy_on_readv(BlockDriverState bs,
//            int64_t sector_num, int nb_sectors, QEMUIOVector *qiov)
//    {
//        /* Perform I/O through a temporary buffer so that users who scribble over
//         * their read buffer while the operation is in progress do not end up
//         * modifying the image file.  This is critical for zero-copy guest I/O
//         * where anything might happen inside guest memory.
//         */
//        void *bounce_buffer;
//
//        BlockDriver *drv = bs.drv;
//        struct iovec iov;
//        QEMUIOVector bounce_qiov;
//        int64_t cluster_sector_num;
//        int cluster_nb_sectors;
//        size_t skip_bytes;
//        int ret;
//
//        /* Cover entire cluster so no additional backing file I/O is required when
//         * allocating cluster in the image file.
//         */
//        round_to_clusters(bs, sector_num, nb_sectors,
//                          &cluster_sector_num, &cluster_nb_sectors);
//
//        trace_bdrv_co_do_copy_on_readv(bs, sector_num, nb_sectors,
//                                       cluster_sector_num, cluster_nb_sectors);
//
//        iov.iov_len = cluster_nb_sectors * BDRV_SECTOR_SIZE;
//        iov.iov_base = bounce_buffer = qemu_blockalign(bs, iov.iov_len);
//        qemu_iovec_init_external(&bounce_qiov, &iov, 1);
//
//        ret = drv.bdrv_co_readv(bs, cluster_sector_num, cluster_nb_sectors,
//                                 &bounce_qiov);
//        if (ret < 0) {
//            goto err;
//        }
//
//        if (drv.bdrv_co_write_zeroes &&
//            buffer_is_zero(bounce_buffer, iov.iov_len)) {
//            ret = bdrv_co_do_write_zeroes(bs, cluster_sector_num,
//                                          cluster_nb_sectors);
//        } else {
//            /* This does not change the data on the disk, it is not necessary
//             * to flush even in cache=writethrough mode.
//             */
//            ret = drv.bdrv_co_writev(bs, cluster_sector_num, cluster_nb_sectors,
//                                      &bounce_qiov);
//        }
//
//        if (ret < 0) {
//            /* It might be okay to ignore write errors for guest requests.  If this
//             * is a deliberate copy-on-read then we don't want to ignore the error.
//             * Simply report it in all cases.
//             */
//            goto err;
//        }
//
//        skip_bytes = (sector_num - cluster_sector_num) * BDRV_SECTOR_SIZE;
//        qemu_iovec_from_buf(qiov, 0, bounce_buffer + skip_bytes,
//                            nb_sectors * BDRV_SECTOR_SIZE);
//
//    err:
//        qemu_vfree(bounce_buffer);
//        return ret;
//    }
//
//    /*
//     * Handle a read request in coroutine context
//     */
//    static int coroutine_fn bdrv_co_do_readv(BlockDriverState bs,
//        int64_t sector_num, int nb_sectors, QEMUIOVector *qiov,
//        BdrvRequestFlags flags)
//    {
//        BlockDriver *drv = bs.drv;
//        BdrvTrackedRequest req;
//        int ret;
//
//        if (!drv) {
//            return -ENOMEDIUM;
//        }
//        if (bdrv_check_request(bs, sector_num, nb_sectors)) {
//            return -EIO;
//        }
//
//        /* throttling disk read I/O */
//        if (bs.io_limits_enabled) {
//            bdrv_io_limits_intercept(bs, false, nb_sectors);
//        }
//
//        if (bs.copy_on_read) {
//            flags |= BDRV_REQ_COPY_ON_READ;
//        }
//        if (flags & BDRV_REQ_COPY_ON_READ) {
//            bs.copy_on_read_in_flight++;
//        }
//
//        if (bs.copy_on_read_in_flight) {
//            wait_for_overlapping_requests(bs, sector_num, nb_sectors);
//        }
//
//        tracked_request_begin(&req, bs, sector_num, nb_sectors, false);
//
//        if (flags & BDRV_REQ_COPY_ON_READ) {
//            int pnum;
//
//            ret = bdrv_co_is_allocated(bs, sector_num, nb_sectors, &pnum);
//            if (ret < 0) {
//                goto out;
//            }
//
//            if (!ret || pnum != nb_sectors) {
//                ret = bdrv_co_do_copy_on_readv(bs, sector_num, nb_sectors, qiov);
//                goto out;
//            }
//        }
//
//        ret = drv.bdrv_co_readv(bs, sector_num, nb_sectors, qiov);
//
//    out:
//        tracked_request_end(&req);
//
//        if (flags & BDRV_REQ_COPY_ON_READ) {
//            bs.copy_on_read_in_flight--;
//        }
//
//        return ret;
//    }
//
//    int coroutine_fn bdrv_co_readv(BlockDriverState bs, int64_t sector_num,
//        int nb_sectors, QEMUIOVector *qiov)
//    {
//        trace_bdrv_co_readv(bs, sector_num, nb_sectors);
//
//        return bdrv_co_do_readv(bs, sector_num, nb_sectors, qiov, 0);
//    }
//
//    int coroutine_fn bdrv_co_copy_on_readv(BlockDriverState bs,
//        int64_t sector_num, int nb_sectors, QEMUIOVector *qiov)
//    {
//        trace_bdrv_co_copy_on_readv(bs, sector_num, nb_sectors);
//
//        return bdrv_co_do_readv(bs, sector_num, nb_sectors, qiov,
//                                BDRV_REQ_COPY_ON_READ);
//    }
//
//    static int coroutine_fn bdrv_co_do_write_zeroes(BlockDriverState bs,
//        int64_t sector_num, int nb_sectors)
//    {
//        BlockDriver *drv = bs.drv;
//        QEMUIOVector qiov;
//        struct iovec iov;
//        int ret;
//
//        /* TODO Emulate only part of misaligned requests instead of letting block
//         * drivers return -ENOTSUP and emulate everything */
//
//        /* First try the efficient write zeroes operation */
//        if (drv.bdrv_co_write_zeroes) {
//            ret = drv.bdrv_co_write_zeroes(bs, sector_num, nb_sectors);
//            if (ret != -ENOTSUP) {
//                return ret;
//            }
//        }
//
//        /* Fall back to bounce buffer if write zeroes is unsupported */
//        iov.iov_len  = nb_sectors * BDRV_SECTOR_SIZE;
//        iov.iov_base = qemu_blockalign(bs, iov.iov_len);
//        memset(iov.iov_base, 0, iov.iov_len);
//        qemu_iovec_init_external(&qiov, &iov, 1);
//
//        ret = drv.bdrv_co_writev(bs, sector_num, nb_sectors, &qiov);
//
//        qemu_vfree(iov.iov_base);
//        return ret;
//    }
//
//    /*
//     * Handle a write request in coroutine context
//     */
//    static int coroutine_fn bdrv_co_do_writev(BlockDriverState bs,
//        int64_t sector_num, int nb_sectors, QEMUIOVector *qiov,
//        BdrvRequestFlags flags)
//    {
//        BlockDriver *drv = bs.drv;
//        BdrvTrackedRequest req;
//        int ret;
//
//        if (!bs.drv) {
//            return -ENOMEDIUM;
//        }
//        if (bs.read_only) {
//            return -EACCES;
//        }
//        if (bdrv_check_request(bs, sector_num, nb_sectors)) {
//            return -EIO;
//        }
//
//        /* throttling disk write I/O */
//        if (bs.io_limits_enabled) {
//            bdrv_io_limits_intercept(bs, true, nb_sectors);
//        }
//
//        if (bs.copy_on_read_in_flight) {
//            wait_for_overlapping_requests(bs, sector_num, nb_sectors);
//        }
//
//        tracked_request_begin(&req, bs, sector_num, nb_sectors, true);
//
//        if (flags & BDRV_REQ_ZERO_WRITE) {
//            ret = bdrv_co_do_write_zeroes(bs, sector_num, nb_sectors);
//        } else {
//            ret = drv.bdrv_co_writev(bs, sector_num, nb_sectors, qiov);
//        }
//
//        if (ret == 0 && !bs.enable_write_cache) {
//            ret = bdrv_co_flush(bs);
//        }
//
//        if (bs.dirty_bitmap) {
//            set_dirty_bitmap(bs, sector_num, nb_sectors, 1);
//        }
//
//        if (bs.wr_highest_sector < sector_num + nb_sectors - 1) {
//            bs.wr_highest_sector = sector_num + nb_sectors - 1;
//        }
//
//        tracked_request_end(&req);
//
//        return ret;
//    }
//
//    int coroutine_fn bdrv_co_writev(BlockDriverState bs, int64_t sector_num,
//        int nb_sectors, QEMUIOVector *qiov)
//    {
//        trace_bdrv_co_writev(bs, sector_num, nb_sectors);
//
//        return bdrv_co_do_writev(bs, sector_num, nb_sectors, qiov, 0);
//    }
//
//    int coroutine_fn bdrv_co_write_zeroes(BlockDriverState bs,
//                                          int64_t sector_num, int nb_sectors)
//    {
//        trace_bdrv_co_write_zeroes(bs, sector_num, nb_sectors);
//
//        return bdrv_co_do_writev(bs, sector_num, nb_sectors, NULL,
//                                 BDRV_REQ_ZERO_WRITE);
//    }
//
//    /**
//     * Truncate file to 'offset' bytes (needed only for file protocols)
//     */
//    int bdrv_truncate(BlockDriverState bs, int64_t offset)
//    {
//        BlockDriver *drv = bs.drv;
//        int ret;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (!drv.bdrv_truncate)
//            return -ENOTSUP;
//        if (bs.read_only)
//            return -EACCES;
//        if (bdrv_in_use(bs))
//            return -EBUSY;
//        ret = drv.bdrv_truncate(bs, offset);
//        if (ret == 0) {
//            ret = refresh_total_sectors(bs, offset >> BDRV_SECTOR_BITS);
//            bdrv_dev_resize_cb(bs);
//        }
//        return ret;
//    }
//
//    /**
//     * Length of a allocated file in bytes. Sparse files are counted by actual
//     * allocated space. Return < 0 if error or unknown.
//     */
//    int64_t bdrv_get_allocated_file_size(BlockDriverState bs)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv) {
//            return -ENOMEDIUM;
//        }
//        if (drv.bdrv_get_allocated_file_size) {
//            return drv.bdrv_get_allocated_file_size(bs);
//        }
//        if (bs.file) {
//            return bdrv_get_allocated_file_size(bs.file);
//        }
//        return -ENOTSUP;
//    }

    /**
     * Length of a file in bytes. Return < 0 if error or unknown.
     */
    static private long bdrv_getlength(BlockDriverState bs) {
        BlockDriver drv = bs.drv;
        if (drv == null)
            return -Error.ENOMEDIUM;

        if (bs.growable || bdrv_dev_has_removable_media(bs)) {
            if (drv.has_bdrv_getlength()) {
                return drv.bdrv_getlength(bs);
            }
        }
        return bs.total_sectors * BDRV_SECTOR_SIZE;
    }

    /* return 0 as number of sectors if no device present or error */
    static public long bdrv_get_geometry(BlockDriverState bs)
    {
        long length = bdrv_getlength(bs);
        if (length < 0)
            length = 0;
        else
            length = length >> BDRV_SECTOR_BITS;
        return length;
    }

//    /* throttling disk io limits */
//    void bdrv_set_io_limits(BlockDriverState bs,
//                            BlockIOLimit *io_limits)
//    {
//        bs.io_limits = *io_limits;
//        bs.io_limits_enabled = bdrv_io_limits_enabled(bs);
//    }
//
//    void bdrv_set_on_error(BlockDriverState bs, BlockErrorAction on_read_error,
//                           BlockErrorAction on_write_error)
//    {
//        bs.on_read_error = on_read_error;
//        bs.on_write_error = on_write_error;
//    }

    static public BlockErrorAction bdrv_get_on_error(BlockDriverState bs, boolean is_read) {
        return is_read ? bs.on_read_error : bs.on_write_error;
    }

    static public boolean bdrv_is_read_only(BlockDriverState bs) {
        return bs.read_only;
    }
//
//    int bdrv_is_sg(BlockDriverState bs)
//    {
//        return bs.sg;
//    }

    static public boolean bdrv_enable_write_cache(BlockDriverState bs) {
        return bs.enable_write_cache;
    }

    static public void bdrv_set_enable_write_cache(BlockDriverState bs, boolean wce) {
        bs.enable_write_cache = wce;
    }

//    int bdrv_is_encrypted(BlockDriverState bs)
//    {
//        if (bs.backing_hd && bs.backing_hd.encrypted)
//            return 1;
//        return bs.encrypted;
//    }
//
//    static private boolean bdrv_key_required(BlockDriverState bs) {
//        BlockDriverState backing_hd = bs.backing_hd;
//
//        if (backing_hd!=null && backing_hd.encrypted && !backing_hd.valid_key)
//            return true;
//        return (bs.encrypted && !bs.valid_key);
//    }
//
//    int bdrv_set_key(BlockDriverState bs, const char *key)
//    {
//        int ret;
//        if (bs.backing_hd && bs.backing_hd.encrypted) {
//            ret = bdrv_set_key(bs.backing_hd, key);
//            if (ret < 0)
//                return ret;
//            if (!bs.encrypted)
//                return 0;
//        }
//        if (!bs.encrypted) {
//            return -EINVAL;
//        } else if (!bs.drv || !bs.drv.bdrv_set_key) {
//            return -ENOMEDIUM;
//        }
//        ret = bs.drv.bdrv_set_key(bs, key);
//        if (ret < 0) {
//            bs.valid_key = 0;
//        } else if (!bs.valid_key) {
//            bs.valid_key = 1;
//            /* call the change callback now, we skipped it on open */
//            bdrv_dev_change_media_cb(bs, true);
//        }
//        return ret;
//    }
//
//    const char *bdrv_get_format_name(BlockDriverState bs)
//    {
//        return bs.drv ? bs.drv.format_name : NULL;
//    }
//
//    void bdrv_iterate_format(void (*it)(void *opaque, const char *name),
//                             void *opaque)
//    {
//        BlockDriver *drv;
//
//        QLIST_FOREACH(drv, &bdrv_drivers, list) {
//            it(opaque, drv.format_name);
//        }
//    }

    static private BlockDriverState bdrv_find(String name) {
        for (BlockDriverState bs: bdrv_states) {
            if (name.equals(bs.device_name))
                return bs;
        }
        return null;
    }

//    BlockDriverState *bdrv_next(BlockDriverState bs)
//    {
//        if (!bs) {
//            return QTAILQ_FIRST(&bdrv_states);
//        }
//        return QTAILQ_NEXT(bs, list);
//    }
//
//    void bdrv_iterate(void (*it)(void *opaque, BlockDriverState bs), void *opaque)
//    {
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            it(opaque, bs);
//        }
//    }
//
//    const char *bdrv_get_device_name(BlockDriverState bs)
//    {
//        return bs.device_name;
//    }
//
//    int bdrv_get_flags(BlockDriverState bs)
//    {
//        return bs.open_flags;
//    }
//
//    void bdrv_flush_all(void)
//    {
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            bdrv_flush(bs);
//        }
//    }
//
//    int bdrv_has_zero_init(BlockDriverState bs)
//    {
//        assert(bs.drv);
//
//        if (bs.drv.bdrv_has_zero_init) {
//            return bs.drv.bdrv_has_zero_init(bs);
//        }
//
//        return 1;
//    }
//
//    typedef struct BdrvCoIsAllocatedData {
//        BlockDriverState bs;
//        int64_t sector_num;
//        int nb_sectors;
//        int *pnum;
//        int ret;
//        bool done;
//    } BdrvCoIsAllocatedData;
//
//    /*
//     * Returns true iff the specified sector is present in the disk image. Drivers
//     * not implementing the functionality are assumed to not support backing files,
//     * hence all their sectors are reported as allocated.
//     *
//     * If 'sector_num' is beyond the end of the disk image the return value is 0
//     * and 'pnum' is set to 0.
//     *
//     * 'pnum' is set to the number of sectors (including and immediately following
//     * the specified sector) that are known to be in the same
//     * allocated/unallocated state.
//     *
//     * 'nb_sectors' is the max value 'pnum' should be set to.  If nb_sectors goes
//     * beyond the end of the disk image it will be clamped.
//     */
//    int coroutine_fn bdrv_co_is_allocated(BlockDriverState bs, int64_t sector_num,
//                                          int nb_sectors, int *pnum)
//    {
//        int64_t n;
//
//        if (sector_num >= bs.total_sectors) {
//            *pnum = 0;
//            return 0;
//        }
//
//        n = bs.total_sectors - sector_num;
//        if (n < nb_sectors) {
//            nb_sectors = n;
//        }
//
//        if (!bs.drv.bdrv_co_is_allocated) {
//            *pnum = nb_sectors;
//            return 1;
//        }
//
//        return bs.drv.bdrv_co_is_allocated(bs, sector_num, nb_sectors, pnum);
//    }
//
//    /* Coroutine wrapper for bdrv_is_allocated() */
//    static void coroutine_fn bdrv_is_allocated_co_entry(void *opaque)
//    {
//        BdrvCoIsAllocatedData *data = opaque;
//        BlockDriverState bs = data.bs;
//
//        data.ret = bdrv_co_is_allocated(bs, data.sector_num, data.nb_sectors,
//                                         data.pnum);
//        data.done = true;
//    }
//
//    /*
//     * Synchronous wrapper around bdrv_co_is_allocated().
//     *
//     * See bdrv_co_is_allocated() for details.
//     */
//    int bdrv_is_allocated(BlockDriverState bs, int64_t sector_num, int nb_sectors,
//                          int *pnum)
//    {
//        Coroutine *co;
//        BdrvCoIsAllocatedData data = {
//            .bs = bs,
//            .sector_num = sector_num,
//            .nb_sectors = nb_sectors,
//            .pnum = pnum,
//            .done = false,
//        };
//
//        co = qemu_coroutine_create(bdrv_is_allocated_co_entry);
//        qemu_coroutine_enter(co, &data);
//        while (!data.done) {
//            qemu_aio_wait();
//        }
//        return data.ret;
//    }
//
//    /*
//     * Given an image chain: ... . [BASE] . [INTER1] . [INTER2] . [TOP]
//     *
//     * Return true if the given sector is allocated in any image between
//     * BASE and TOP (inclusive).  BASE can be NULL to check if the given
//     * sector is allocated in any image of the chain.  Return false otherwise.
//     *
//     * 'pnum' is set to the number of sectors (including and immediately following
//     *  the specified sector) that are known to be in the same
//     *  allocated/unallocated state.
//     *
//     */
//    int coroutine_fn bdrv_co_is_allocated_above(BlockDriverState *top,
//                                                BlockDriverState *base,
//                                                int64_t sector_num,
//                                                int nb_sectors, int *pnum)
//    {
//        BlockDriverState *intermediate;
//        int ret, n = nb_sectors;
//
//        intermediate = top;
//        while (intermediate && intermediate != base) {
//            int pnum_inter;
//            ret = bdrv_co_is_allocated(intermediate, sector_num, nb_sectors,
//                                       &pnum_inter);
//            if (ret < 0) {
//                return ret;
//            } else if (ret) {
//                *pnum = pnum_inter;
//                return 1;
//            }
//
//            /*
//             * [sector_num, nb_sectors] is unallocated on top but intermediate
//             * might have
//             *
//             * [sector_num+x, nr_sectors] allocated.
//             */
//            if (n > pnum_inter) {
//                n = pnum_inter;
//            }
//
//            intermediate = intermediate.backing_hd;
//        }
//
//        *pnum = n;
//        return 0;
//    }
//
//    BlockInfoList *qmp_query_block(Error **errp)
//    {
//        BlockInfoList *head = NULL, *cur_item = NULL;
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            BlockInfoList *info = g_malloc0(sizeof(*info));
//
//            info.value = g_malloc0(sizeof(*info.value));
//            info.value.device = g_strdup(bs.device_name);
//            info.value.type = g_strdup("unknown");
//            info.value.locked = bdrv_dev_is_medium_locked(bs);
//            info.value.removable = bdrv_dev_has_removable_media(bs);
//
//            if (bdrv_dev_has_removable_media(bs)) {
//                info.value.has_tray_open = true;
//                info.value.tray_open = bdrv_dev_is_tray_open(bs);
//            }
//
//            if (bdrv_iostatus_is_enabled(bs)) {
//                info.value.has_io_status = true;
//                info.value.io_status = bs.iostatus;
//            }
//
//            if (bs.drv) {
//                info.value.has_inserted = true;
//                info.value.inserted = g_malloc0(sizeof(*info.value.inserted));
//                info.value.inserted.file = g_strdup(bs.filename);
//                info.value.inserted.ro = bs.read_only;
//                info.value.inserted.drv = g_strdup(bs.drv.format_name);
//                info.value.inserted.encrypted = bs.encrypted;
//                info.value.inserted.encryption_key_missing = bdrv_key_required(bs);
//                if (bs.backing_file[0]) {
//                    info.value.inserted.has_backing_file = true;
//                    info.value.inserted.backing_file = g_strdup(bs.backing_file);
//                }
//
//                info.value.inserted.backing_file_depth =
//                    bdrv_get_backing_file_depth(bs);
//
//                if (bs.io_limits_enabled) {
//                    info.value.inserted.bps =
//                                   bs.io_limits.bps[BLOCK_IO_LIMIT_TOTAL];
//                    info.value.inserted.bps_rd =
//                                   bs.io_limits.bps[BLOCK_IO_LIMIT_READ];
//                    info.value.inserted.bps_wr =
//                                   bs.io_limits.bps[BLOCK_IO_LIMIT_WRITE];
//                    info.value.inserted.iops =
//                                   bs.io_limits.iops[BLOCK_IO_LIMIT_TOTAL];
//                    info.value.inserted.iops_rd =
//                                   bs.io_limits.iops[BLOCK_IO_LIMIT_READ];
//                    info.value.inserted.iops_wr =
//                                   bs.io_limits.iops[BLOCK_IO_LIMIT_WRITE];
//                }
//            }
//
//            /* XXX: waiting for the qapi to support GSList */
//            if (!cur_item) {
//                head = cur_item = info;
//            } else {
//                cur_item.next = info;
//                cur_item = info;
//            }
//        }
//
//        return head;
//    }
//
//    /* Consider exposing this as a full fledged QMP command */
//    static BlockStats *qmp_query_blockstat(const BlockDriverState bs, Error **errp)
//    {
//        BlockStats *s;
//
//        s = g_malloc0(sizeof(*s));
//
//        if (bs.device_name[0]) {
//            s.has_device = true;
//            s.device = g_strdup(bs.device_name);
//        }
//
//        s.stats = g_malloc0(sizeof(*s.stats));
//        s.stats.rd_bytes = bs.nr_bytes[BDRV_ACCT_READ];
//        s.stats.wr_bytes = bs.nr_bytes[BDRV_ACCT_WRITE];
//        s.stats.rd_operations = bs.nr_ops[BDRV_ACCT_READ];
//        s.stats.wr_operations = bs.nr_ops[BDRV_ACCT_WRITE];
//        s.stats.wr_highest_offset = bs.wr_highest_sector * BDRV_SECTOR_SIZE;
//        s.stats.flush_operations = bs.nr_ops[BDRV_ACCT_FLUSH];
//        s.stats.wr_total_time_ns = bs.total_time_ns[BDRV_ACCT_WRITE];
//        s.stats.rd_total_time_ns = bs.total_time_ns[BDRV_ACCT_READ];
//        s.stats.flush_total_time_ns = bs.total_time_ns[BDRV_ACCT_FLUSH];
//
//        if (bs.file) {
//            s.has_parent = true;
//            s.parent = qmp_query_blockstat(bs.file, NULL);
//        }
//
//        return s;
//    }
//
//    BlockStatsList *qmp_query_blockstats(Error **errp)
//    {
//        BlockStatsList *head = NULL, *cur_item = NULL;
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            BlockStatsList *info = g_malloc0(sizeof(*info));
//            info.value = qmp_query_blockstat(bs, NULL);
//
//            /* XXX: waiting for the qapi to support GSList */
//            if (!cur_item) {
//                head = cur_item = info;
//            } else {
//                cur_item.next = info;
//                cur_item = info;
//            }
//        }
//
//        return head;
//    }
//
//    const char *bdrv_get_encrypted_filename(BlockDriverState bs)
//    {
//        if (bs.backing_hd && bs.backing_hd.encrypted)
//            return bs.backing_file;
//        else if (bs.encrypted)
//            return bs.filename;
//        else
//            return NULL;
//    }
//
//    void bdrv_get_backing_filename(BlockDriverState bs,
//                                   char *filename, int filename_size)
//    {
//        pstrcpy(filename, filename_size, bs.backing_file);
//    }
//
//    int bdrv_write_compressed(BlockDriverState bs, int64_t sector_num,
//                              const uint8_t *buf, int nb_sectors)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (!drv.bdrv_write_compressed)
//            return -ENOTSUP;
//        if (bdrv_check_request(bs, sector_num, nb_sectors))
//            return -EIO;
//
//        if (bs.dirty_bitmap) {
//            set_dirty_bitmap(bs, sector_num, nb_sectors, 1);
//        }
//
//        return drv.bdrv_write_compressed(bs, sector_num, buf, nb_sectors);
//    }
//
//    int bdrv_get_info(BlockDriverState bs, BlockDriverInfo *bdi)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (!drv.bdrv_get_info)
//            return -ENOTSUP;
//        memset(bdi, 0, sizeof(*bdi));
//        return drv.bdrv_get_info(bs, bdi);
//    }
//
//    int bdrv_save_vmstate(BlockDriverState bs, const uint8_t *buf,
//                          int64_t pos, int size)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (drv.bdrv_save_vmstate)
//            return drv.bdrv_save_vmstate(bs, buf, pos, size);
//        if (bs.file)
//            return bdrv_save_vmstate(bs.file, buf, pos, size);
//        return -ENOTSUP;
//    }
//
//    int bdrv_load_vmstate(BlockDriverState bs, uint8_t *buf,
//                          int64_t pos, int size)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (drv.bdrv_load_vmstate)
//            return drv.bdrv_load_vmstate(bs, buf, pos, size);
//        if (bs.file)
//            return bdrv_load_vmstate(bs.file, buf, pos, size);
//        return -ENOTSUP;
//    }
//
//    void bdrv_debug_event(BlockDriverState bs, BlkDebugEvent event)
//    {
//        BlockDriver *drv = bs.drv;
//
//        if (!drv || !drv.bdrv_debug_event) {
//            return;
//        }
//
//        drv.bdrv_debug_event(bs, event);
//
//    }
//
//    /**************************************************************/
//    /* handling of snapshots */
//
//    int bdrv_can_snapshot(BlockDriverState bs)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv || !bdrv_is_inserted(bs) || bdrv_is_read_only(bs)) {
//            return 0;
//        }
//
//        if (!drv.bdrv_snapshot_create) {
//            if (bs.file != NULL) {
//                return bdrv_can_snapshot(bs.file);
//            }
//            return 0;
//        }
//
//        return 1;
//    }
//
//    int bdrv_is_snapshot(BlockDriverState bs)
//    {
//        return !!(bs.open_flags & BDRV_O_SNAPSHOT);
//    }
//
//    BlockDriverState *bdrv_snapshots(void)
//    {
//        BlockDriverState bs;
//
//        if (bs_snapshots) {
//            return bs_snapshots;
//        }
//
//        bs = NULL;
//        while ((bs = bdrv_next(bs))) {
//            if (bdrv_can_snapshot(bs)) {
//                bs_snapshots = bs;
//                return bs;
//            }
//        }
//        return NULL;
//    }
//
//    int bdrv_snapshot_create(BlockDriverState bs,
//                             QEMUSnapshotInfo *sn_info)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (drv.bdrv_snapshot_create)
//            return drv.bdrv_snapshot_create(bs, sn_info);
//        if (bs.file)
//            return bdrv_snapshot_create(bs.file, sn_info);
//        return -ENOTSUP;
//    }
//
//    int bdrv_snapshot_goto(BlockDriverState bs,
//                           const char *snapshot_id)
//    {
//        BlockDriver *drv = bs.drv;
//        int ret, open_ret;
//
//        if (!drv)
//            return -ENOMEDIUM;
//        if (drv.bdrv_snapshot_goto)
//            return drv.bdrv_snapshot_goto(bs, snapshot_id);
//
//        if (bs.file) {
//            drv.bdrv_close(bs);
//            ret = bdrv_snapshot_goto(bs.file, snapshot_id);
//            open_ret = drv.bdrv_open(bs, bs.open_flags);
//            if (open_ret < 0) {
//                bdrv_delete(bs.file);
//                bs.drv = NULL;
//                return open_ret;
//            }
//            return ret;
//        }
//
//        return -ENOTSUP;
//    }
//
//    int bdrv_snapshot_delete(BlockDriverState bs, const char *snapshot_id)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (drv.bdrv_snapshot_delete)
//            return drv.bdrv_snapshot_delete(bs, snapshot_id);
//        if (bs.file)
//            return bdrv_snapshot_delete(bs.file, snapshot_id);
//        return -ENOTSUP;
//    }
//
//    int bdrv_snapshot_list(BlockDriverState bs,
//                           QEMUSnapshotInfo **psn_info)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv)
//            return -ENOMEDIUM;
//        if (drv.bdrv_snapshot_list)
//            return drv.bdrv_snapshot_list(bs, psn_info);
//        if (bs.file)
//            return bdrv_snapshot_list(bs.file, psn_info);
//        return -ENOTSUP;
//    }
//
//    int bdrv_snapshot_load_tmp(BlockDriverState bs,
//            const char *snapshot_name)
//    {
//        BlockDriver *drv = bs.drv;
//        if (!drv) {
//            return -ENOMEDIUM;
//        }
//        if (!bs.read_only) {
//            return -EINVAL;
//        }
//        if (drv.bdrv_snapshot_load_tmp) {
//            return drv.bdrv_snapshot_load_tmp(bs, snapshot_name);
//        }
//        return -ENOTSUP;
//    }
//
//    BlockDriverState *bdrv_find_backing_image(BlockDriverState bs,
//            const char *backing_file)
//    {
//        if (!bs.drv) {
//            return NULL;
//        }
//
//        if (bs.backing_hd) {
//            if (strcmp(bs.backing_file, backing_file) == 0) {
//                return bs.backing_hd;
//            } else {
//                return bdrv_find_backing_image(bs.backing_hd, backing_file);
//            }
//        }
//
//        return NULL;
//    }
//
//    int bdrv_get_backing_file_depth(BlockDriverState bs)
//    {
//        if (!bs.drv) {
//            return 0;
//        }
//
//        if (!bs.backing_hd) {
//            return 0;
//        }
//
//        return 1 + bdrv_get_backing_file_depth(bs.backing_hd);
//    }
//
//    static private final int NB_SUFFIXES 4
//
//    char *get_human_readable_size(char *buf, int buf_size, int64_t size)
//    {
//        static const char suffixes[NB_SUFFIXES] = "KMGT";
//        int64_t base;
//        int i;
//
//        if (size <= 999) {
//            snprintf(buf, buf_size, "%" PRId64, size);
//        } else {
//            base = 1024;
//            for(i = 0; i < NB_SUFFIXES; i++) {
//                if (size < (10 * base)) {
//                    snprintf(buf, buf_size, "%0.1f%c",
//                             (double)size / base,
//                             suffixes[i]);
//                    break;
//                } else if (size < (1000 * base) || i == (NB_SUFFIXES - 1)) {
//                    snprintf(buf, buf_size, "%" PRId64 "%c",
//                             ((size + (base >> 1)) / base),
//                             suffixes[i]);
//                    break;
//                }
//                base = base * 1024;
//            }
//        }
//        return buf;
//    }
//
//    char *bdrv_snapshot_dump(char *buf, int buf_size, QEMUSnapshotInfo *sn)
//    {
//        char buf1[128], date_buf[128], clock_buf[128];
//    #ifdef _WIN32
//        struct tm *ptm;
//    #else
//        struct tm tm;
//    #endif
//        time_t ti;
//        int64_t secs;
//
//        if (!sn) {
//            snprintf(buf, buf_size,
//                     "%-10s%-20s%7s%20s%15s",
//                     "ID", "TAG", "VM SIZE", "DATE", "VM CLOCK");
//        } else {
//            ti = sn.date_sec;
//    #ifdef _WIN32
//            ptm = localtime(&ti);
//            strftime(date_buf, sizeof(date_buf),
//                     "%Y-%m-%d %H:%M:%S", ptm);
//    #else
//            localtime_r(&ti, &tm);
//            strftime(date_buf, sizeof(date_buf),
//                     "%Y-%m-%d %H:%M:%S", &tm);
//    #endif
//            secs = sn.vm_clock_nsec / 1000000000;
//            snprintf(clock_buf, sizeof(clock_buf),
//                     "%02d:%02d:%02d.%03d",
//                     (int)(secs / 3600),
//                     (int)((secs / 60) % 60),
//                     (int)(secs % 60),
//                     (int)((sn.vm_clock_nsec / 1000000) % 1000));
//            snprintf(buf, buf_size,
//                     "%-10s%-20s%7s%20s%15s",
//                     sn.id_str, sn.name,
//                     get_human_readable_size(buf1, sizeof(buf1), sn.vm_state_size),
//                     date_buf,
//                     clock_buf);
//        }
//        return buf;
//    }

    /**************************************************************/
    /* async I/Os */

    static public BlockDriverAIOCB bdrv_aio_readv(BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector qiov, int nb_sectors, BlockDriverCompletionFunc cb, Object opaque) {
        //trace_bdrv_aio_readv(bs, sector_num, nb_sectors, opaque);
        Internal.IDEState s = (Internal.IDEState)opaque;
        bs.drv.bdrv_read(bs, sector_num, s.io_buffer, 0, nb_sectors);
        cb.call(opaque, 0);
        return null;
        //return bdrv_co_aio_rw_vector(bs, sector_num, qiov, nb_sectors, cb, opaque, false);
    }

    static public BlockDriverAIOCB bdrv_aio_writev(BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector qiov, int nb_sectors, BlockDriverCompletionFunc cb, Object opaque) {
        //trace_bdrv_aio_writev(bs, sector_num, nb_sectors, opaque);

        Internal.IDEState s = (Internal.IDEState)opaque;
        bs.drv.bdrv_write(bs, sector_num, s.io_buffer, 0, nb_sectors);
        cb.call(opaque, 0);
        return null;
        //return bdrv_co_aio_rw_vector(bs, sector_num, qiov, nb_sectors, cb, opaque, true);
    }


//    typedef struct MultiwriteCB {
//        int error;
//        int num_requests;
//        int num_callbacks;
//        struct {
//            BlockDriverCompletionFunc *cb;
//            void *opaque;
//            QEMUIOVector *free_qiov;
//        } callbacks[];
//    } MultiwriteCB;
//
//    static void multiwrite_user_cb(MultiwriteCB *mcb)
//    {
//        int i;
//
//        for (i = 0; i < mcb.num_callbacks; i++) {
//            mcb.callbacks[i].cb(mcb.callbacks[i].opaque, mcb.error);
//            if (mcb.callbacks[i].free_qiov) {
//                qemu_iovec_destroy(mcb.callbacks[i].free_qiov);
//            }
//            g_free(mcb.callbacks[i].free_qiov);
//        }
//    }
//
//    static void multiwrite_cb(void *opaque, int ret)
//    {
//        MultiwriteCB *mcb = opaque;
//
//        trace_multiwrite_cb(mcb, ret);
//
//        if (ret < 0 && !mcb.error) {
//            mcb.error = ret;
//        }
//
//        mcb.num_requests--;
//        if (mcb.num_requests == 0) {
//            multiwrite_user_cb(mcb);
//            g_free(mcb);
//        }
//    }
//
//    static int multiwrite_req_compare(const void *a, const void *b)
//    {
//        const BlockRequest *req1 = a, *req2 = b;
//
//        /*
//         * Note that we can't simply subtract req2.sector from req1.sector
//         * here as that could overflow the return value.
//         */
//        if (req1.sector > req2.sector) {
//            return 1;
//        } else if (req1.sector < req2.sector) {
//            return -1;
//        } else {
//            return 0;
//        }
//    }
//
//    /*
//     * Takes a bunch of requests and tries to merge them. Returns the number of
//     * requests that remain after merging.
//     */
//    static int multiwrite_merge(BlockDriverState bs, BlockRequest *reqs,
//        int num_reqs, MultiwriteCB *mcb)
//    {
//        int i, outidx;
//
//        // Sort requests by start sector
//        qsort(reqs, num_reqs, sizeof(*reqs), &multiwrite_req_compare);
//
//        // Check if adjacent requests touch the same clusters. If so, combine them,
//        // filling up gaps with zero sectors.
//        outidx = 0;
//        for (i = 1; i < num_reqs; i++) {
//            int merge = 0;
//            int64_t oldreq_last = reqs[outidx].sector + reqs[outidx].nb_sectors;
//
//            // Handle exactly sequential writes and overlapping writes.
//            if (reqs[i].sector <= oldreq_last) {
//                merge = 1;
//            }
//
//            if (reqs[outidx].qiov.niov + reqs[i].qiov.niov + 1 > IOV_MAX) {
//                merge = 0;
//            }
//
//            if (merge) {
//                size_t size;
//                QEMUIOVector *qiov = g_malloc0(sizeof(*qiov));
//                qemu_iovec_init(qiov,
//                    reqs[outidx].qiov.niov + reqs[i].qiov.niov + 1);
//
//                // Add the first request to the merged one. If the requests are
//                // overlapping, drop the last sectors of the first request.
//                size = (reqs[i].sector - reqs[outidx].sector) << 9;
//                qemu_iovec_concat(qiov, reqs[outidx].qiov, 0, size);
//
//                // We should need to add any zeros between the two requests
//                assert (reqs[i].sector <= oldreq_last);
//
//                // Add the second request
//                qemu_iovec_concat(qiov, reqs[i].qiov, 0, reqs[i].qiov.size);
//
//                reqs[outidx].nb_sectors = qiov.size >> 9;
//                reqs[outidx].qiov = qiov;
//
//                mcb.callbacks[i].free_qiov = reqs[outidx].qiov;
//            } else {
//                outidx++;
//                reqs[outidx].sector     = reqs[i].sector;
//                reqs[outidx].nb_sectors = reqs[i].nb_sectors;
//                reqs[outidx].qiov       = reqs[i].qiov;
//            }
//        }
//
//        return outidx + 1;
//    }
//
//    /*
//     * Submit multiple AIO write requests at once.
//     *
//     * On success, the function returns 0 and all requests in the reqs array have
//     * been submitted. In error case this function returns -1, and any of the
//     * requests may or may not be submitted yet. In particular, this means that the
//     * callback will be called for some of the requests, for others it won't. The
//     * caller must check the error field of the BlockRequest to wait for the right
//     * callbacks (if error != 0, no callback will be called).
//     *
//     * The implementation may modify the contents of the reqs array, e.g. to merge
//     * requests. However, the fields opaque and error are left unmodified as they
//     * are used to signal failure for a single request to the caller.
//     */
//    int bdrv_aio_multiwrite(BlockDriverState bs, BlockRequest *reqs, int num_reqs)
//    {
//        MultiwriteCB *mcb;
//        int i;
//
//        /* don't submit writes if we don't have a medium */
//        if (bs.drv == NULL) {
//            for (i = 0; i < num_reqs; i++) {
//                reqs[i].error = -ENOMEDIUM;
//            }
//            return -1;
//        }
//
//        if (num_reqs == 0) {
//            return 0;
//        }
//
//        // Create MultiwriteCB structure
//        mcb = g_malloc0(sizeof(*mcb) + num_reqs * sizeof(*mcb.callbacks));
//        mcb.num_requests = 0;
//        mcb.num_callbacks = num_reqs;
//
//        for (i = 0; i < num_reqs; i++) {
//            mcb.callbacks[i].cb = reqs[i].cb;
//            mcb.callbacks[i].opaque = reqs[i].opaque;
//        }
//
//        // Check for mergable requests
//        num_reqs = multiwrite_merge(bs, reqs, num_reqs, mcb);
//
//        trace_bdrv_aio_multiwrite(mcb, mcb.num_callbacks, num_reqs);
//
//        /* Run the aio requests. */
//        mcb.num_requests = num_reqs;
//        for (i = 0; i < num_reqs; i++) {
//            bdrv_aio_writev(bs, reqs[i].sector, reqs[i].qiov,
//                reqs[i].nb_sectors, multiwrite_cb, mcb);
//        }
//
//        return 0;
//    }

    static public void bdrv_aio_cancel(BlockDriverAIOCB acb) {
        //acb.pool.cancel(acb);
    }

//    /* block I/O throttling */
//    static bool bdrv_exceed_bps_limits(BlockDriverState bs, int nb_sectors,
//                     bool is_write, double elapsed_time, uint64_t *wait)
//    {
//        uint64_t bps_limit = 0;
//        double   bytes_limit, bytes_base, bytes_res;
//        double   slice_time, wait_time;
//
//        if (bs.io_limits.bps[BLOCK_IO_LIMIT_TOTAL]) {
//            bps_limit = bs.io_limits.bps[BLOCK_IO_LIMIT_TOTAL];
//        } else if (bs.io_limits.bps[is_write]) {
//            bps_limit = bs.io_limits.bps[is_write];
//        } else {
//            if (wait) {
//                *wait = 0;
//            }
//
//            return false;
//        }
//
//        slice_time = bs.slice_end - bs.slice_start;
//        slice_time /= (NANOSECONDS_PER_SECOND);
//        bytes_limit = bps_limit * slice_time;
//        bytes_base  = bs.nr_bytes[is_write] - bs.io_base.bytes[is_write];
//        if (bs.io_limits.bps[BLOCK_IO_LIMIT_TOTAL]) {
//            bytes_base += bs.nr_bytes[!is_write] - bs.io_base.bytes[!is_write];
//        }
//
//        /* bytes_base: the bytes of data which have been read/written; and
//         *             it is obtained from the history statistic info.
//         * bytes_res: the remaining bytes of data which need to be read/written.
//         * (bytes_base + bytes_res) / bps_limit: used to calcuate
//         *             the total time for completing reading/writting all data.
//         */
//        bytes_res   = (unsigned) nb_sectors * BDRV_SECTOR_SIZE;
//
//        if (bytes_base + bytes_res <= bytes_limit) {
//            if (wait) {
//                *wait = 0;
//            }
//
//            return false;
//        }
//
//        /* Calc approx time to dispatch */
//        wait_time = (bytes_base + bytes_res) / bps_limit - elapsed_time;
//
//        /* When the I/O rate at runtime exceeds the limits,
//         * bs.slice_end need to be extended in order that the current statistic
//         * info can be kept until the timer fire, so it is increased and tuned
//         * based on the result of experiment.
//         */
//        bs.slice_time = wait_time * BLOCK_IO_SLICE_TIME * 10;
//        bs.slice_end += bs.slice_time - 3 * BLOCK_IO_SLICE_TIME;
//        if (wait) {
//            *wait = wait_time * BLOCK_IO_SLICE_TIME * 10;
//        }
//
//        return true;
//    }
//
//    static bool bdrv_exceed_iops_limits(BlockDriverState bs, bool is_write,
//                                 double elapsed_time, uint64_t *wait)
//    {
//        uint64_t iops_limit = 0;
//        double   ios_limit, ios_base;
//        double   slice_time, wait_time;
//
//        if (bs.io_limits.iops[BLOCK_IO_LIMIT_TOTAL]) {
//            iops_limit = bs.io_limits.iops[BLOCK_IO_LIMIT_TOTAL];
//        } else if (bs.io_limits.iops[is_write]) {
//            iops_limit = bs.io_limits.iops[is_write];
//        } else {
//            if (wait) {
//                *wait = 0;
//            }
//
//            return false;
//        }
//
//        slice_time = bs.slice_end - bs.slice_start;
//        slice_time /= (NANOSECONDS_PER_SECOND);
//        ios_limit  = iops_limit * slice_time;
//        ios_base   = bs.nr_ops[is_write] - bs.io_base.ios[is_write];
//        if (bs.io_limits.iops[BLOCK_IO_LIMIT_TOTAL]) {
//            ios_base += bs.nr_ops[!is_write] - bs.io_base.ios[!is_write];
//        }
//
//        if (ios_base + 1 <= ios_limit) {
//            if (wait) {
//                *wait = 0;
//            }
//
//            return false;
//        }
//
//        /* Calc approx time to dispatch */
//        wait_time = (ios_base + 1) / iops_limit;
//        if (wait_time > elapsed_time) {
//            wait_time = wait_time - elapsed_time;
//        } else {
//            wait_time = 0;
//        }
//
//        bs.slice_time = wait_time * BLOCK_IO_SLICE_TIME * 10;
//        bs.slice_end += bs.slice_time - 3 * BLOCK_IO_SLICE_TIME;
//        if (wait) {
//            *wait = wait_time * BLOCK_IO_SLICE_TIME * 10;
//        }
//
//        return true;
//    }
//
//    static bool bdrv_exceed_io_limits(BlockDriverState bs, int nb_sectors,
//                               bool is_write, int64_t *wait)
//    {
//        int64_t  now, max_wait;
//        uint64_t bps_wait = 0, iops_wait = 0;
//        double   elapsed_time;
//        int      bps_ret, iops_ret;
//
//        now = qemu_get_clock_ns(vm_clock);
//        if ((bs.slice_start < now)
//            && (bs.slice_end > now)) {
//            bs.slice_end = now + bs.slice_time;
//        } else {
//            bs.slice_time  =  5 * BLOCK_IO_SLICE_TIME;
//            bs.slice_start = now;
//            bs.slice_end   = now + bs.slice_time;
//
//            bs.io_base.bytes[is_write]  = bs.nr_bytes[is_write];
//            bs.io_base.bytes[!is_write] = bs.nr_bytes[!is_write];
//
//            bs.io_base.ios[is_write]    = bs.nr_ops[is_write];
//            bs.io_base.ios[!is_write]   = bs.nr_ops[!is_write];
//        }
//
//        elapsed_time  = now - bs.slice_start;
//        elapsed_time  /= (NANOSECONDS_PER_SECOND);
//
//        bps_ret  = bdrv_exceed_bps_limits(bs, nb_sectors,
//                                          is_write, elapsed_time, &bps_wait);
//        iops_ret = bdrv_exceed_iops_limits(bs, is_write,
//                                          elapsed_time, &iops_wait);
//        if (bps_ret || iops_ret) {
//            max_wait = bps_wait > iops_wait ? bps_wait : iops_wait;
//            if (wait) {
//                *wait = max_wait;
//            }
//
//            now = qemu_get_clock_ns(vm_clock);
//            if (bs.slice_end < now + max_wait) {
//                bs.slice_end = now + max_wait;
//            }
//
//            return true;
//        }
//
//        if (wait) {
//            *wait = 0;
//        }
//
//        return false;
//    }
//
//    /**************************************************************/
//    /* async block device emulation */
//
//    typedef struct BlockDriverAIOCBSync {
//        BlockDriverAIOCB common;
//        QEMUBH *bh;
//        int ret;
//        /* vector translation state */
//        QEMUIOVector *qiov;
//        uint8_t *bounce;
//        int is_write;
//    } BlockDriverAIOCBSync;
//
//    static void bdrv_aio_cancel_em(BlockDriverAIOCB *blockacb)
//    {
//        BlockDriverAIOCBSync *acb =
//            container_of(blockacb, BlockDriverAIOCBSync, common);
//        qemu_bh_delete(acb.bh);
//        acb.bh = NULL;
//        qemu_aio_release(acb);
//    }
//
//    static AIOPool bdrv_em_aio_pool = {
//        .aiocb_size         = sizeof(BlockDriverAIOCBSync),
//        .cancel             = bdrv_aio_cancel_em,
//    };
//
//    static void bdrv_aio_bh_cb(void *opaque)
//    {
//        BlockDriverAIOCBSync *acb = opaque;
//
//        if (!acb.is_write)
//            qemu_iovec_from_buf(acb.qiov, 0, acb.bounce, acb.qiov.size);
//        qemu_vfree(acb.bounce);
//        acb.common.cb(acb.common.opaque, acb.ret);
//        qemu_bh_delete(acb.bh);
//        acb.bh = NULL;
//        qemu_aio_release(acb);
//    }
//
//    static BlockDriverAIOCB *bdrv_aio_rw_vector(BlockDriverState bs,
//                                                int64_t sector_num,
//                                                QEMUIOVector *qiov,
//                                                int nb_sectors,
//                                                BlockDriverCompletionFunc *cb,
//                                                void *opaque,
//                                                int is_write)
//
//    {
//        BlockDriverAIOCBSync *acb;
//
//        acb = qemu_aio_get(&bdrv_em_aio_pool, bs, cb, opaque);
//        acb.is_write = is_write;
//        acb.qiov = qiov;
//        acb.bounce = qemu_blockalign(bs, qiov.size);
//        acb.bh = qemu_bh_new(bdrv_aio_bh_cb, acb);
//
//        if (is_write) {
//            qemu_iovec_to_buf(acb.qiov, 0, acb.bounce, qiov.size);
//            acb.ret = bs.drv.bdrv_write(bs, sector_num, acb.bounce, nb_sectors);
//        } else {
//            acb.ret = bs.drv.bdrv_read(bs, sector_num, acb.bounce, nb_sectors);
//        }
//
//        qemu_bh_schedule(acb.bh);
//
//        return &acb.common;
//    }
//
//    static BlockDriverAIOCB *bdrv_aio_readv_em(BlockDriverState bs,
//            int64_t sector_num, QEMUIOVector *qiov, int nb_sectors,
//            BlockDriverCompletionFunc *cb, void *opaque)
//    {
//        return bdrv_aio_rw_vector(bs, sector_num, qiov, nb_sectors, cb, opaque, 0);
//    }
//
//    static BlockDriverAIOCB *bdrv_aio_writev_em(BlockDriverState bs,
//            int64_t sector_num, QEMUIOVector *qiov, int nb_sectors,
//            BlockDriverCompletionFunc *cb, void *opaque)
//    {
//        return bdrv_aio_rw_vector(bs, sector_num, qiov, nb_sectors, cb, opaque, 1);
//    }
//
//
//    typedef struct BlockDriverAIOCBCoroutine {
//        BlockDriverAIOCB common;
//        BlockRequest req;
//        bool is_write;
//        QEMUBH* bh;
//    } BlockDriverAIOCBCoroutine;
//
//    static void bdrv_aio_co_cancel_em(BlockDriverAIOCB *blockacb)
//    {
//        qemu_aio_flush();
//    }
//
//    static AIOPool bdrv_em_co_aio_pool = {
//        .aiocb_size         = sizeof(BlockDriverAIOCBCoroutine),
//        .cancel             = bdrv_aio_co_cancel_em,
//    };
//
//    static void bdrv_co_em_bh(void *opaque)
//    {
//        BlockDriverAIOCBCoroutine *acb = opaque;
//
//        acb.common.cb(acb.common.opaque, acb.req.error);
//        qemu_bh_delete(acb.bh);
//        qemu_aio_release(acb);
//    }
//
//    /* Invoke bdrv_co_do_readv/bdrv_co_do_writev */
//    static void coroutine_fn bdrv_co_do_rw(void *opaque)
//    {
//        BlockDriverAIOCBCoroutine *acb = opaque;
//        BlockDriverState bs = acb.common.bs;
//
//        if (!acb.is_write) {
//            acb.req.error = bdrv_co_do_readv(bs, acb.req.sector,
//                acb.req.nb_sectors, acb.req.qiov, 0);
//        } else {
//            acb.req.error = bdrv_co_do_writev(bs, acb.req.sector,
//                acb.req.nb_sectors, acb.req.qiov, 0);
//        }
//
//        acb.bh = qemu_bh_new(bdrv_co_em_bh, acb);
//        qemu_bh_schedule(acb.bh);
//    }
//
//    static private BlockDriverAIOCB *bdrv_co_aio_rw_vector(BlockDriverState bs, int64_t sector_num, QEMUIOVector qiov, long nb_sectors, BlockDriverCompletionFunc cb, Object opaque, boolean is_write) {
//        Coroutine *co;
//        BlockDriverAIOCBCoroutine *acb;
//
//        acb = qemu_aio_get(&bdrv_em_co_aio_pool, bs, cb, opaque);
//        acb.req.sector = sector_num;
//        acb.req.nb_sectors = nb_sectors;
//        acb.req.qiov = qiov;
//        acb.is_write = is_write;
//
//        co = qemu_coroutine_create(bdrv_co_do_rw);
//        qemu_coroutine_enter(co, acb);
//
//        return &acb.common;
//    }
//
//    static void coroutine_fn bdrv_aio_flush_co_entry(void *opaque)
//    {
//        BlockDriverAIOCBCoroutine *acb = opaque;
//        BlockDriverState bs = acb.common.bs;
//
//        acb.req.error = bdrv_co_flush(bs);
//        acb.bh = qemu_bh_new(bdrv_co_em_bh, acb);
//        qemu_bh_schedule(acb.bh);
//    }

    static public BlockDriverAIOCB bdrv_aio_flush(BlockDriverState bs, BlockDriverCompletionFunc cb, Object opaque) {
//        trace_bdrv_aio_flush(bs, opaque);
//
//        Coroutine *co;
//        BlockDriverAIOCBCoroutine *acb;
//
//        acb = qemu_aio_get(&bdrv_em_co_aio_pool, bs, cb, opaque);
//        co = qemu_coroutine_create(bdrv_aio_flush_co_entry);
//        qemu_coroutine_enter(co, acb);
//
//        return &acb.common;
        cb.call(opaque, 0);
        return null;
    }

//    static void coroutine_fn bdrv_aio_discard_co_entry(void *opaque)
//    {
//        BlockDriverAIOCBCoroutine *acb = opaque;
//        BlockDriverState bs = acb.common.bs;
//
//        acb.req.error = bdrv_co_discard(bs, acb.req.sector, acb.req.nb_sectors);
//        acb.bh = qemu_bh_new(bdrv_co_em_bh, acb);
//        qemu_bh_schedule(acb.bh);
//    }
//
//    BlockDriverAIOCB *bdrv_aio_discard(BlockDriverState bs,
//            int64_t sector_num, int nb_sectors,
//            BlockDriverCompletionFunc *cb, void *opaque)
//    {
//        Coroutine *co;
//        BlockDriverAIOCBCoroutine *acb;
//
//        trace_bdrv_aio_discard(bs, sector_num, nb_sectors, opaque);
//
//        acb = qemu_aio_get(&bdrv_em_co_aio_pool, bs, cb, opaque);
//        acb.req.sector = sector_num;
//        acb.req.nb_sectors = nb_sectors;
//        co = qemu_coroutine_create(bdrv_aio_discard_co_entry);
//        qemu_coroutine_enter(co, acb);
//
//        return &acb.common;
//    }

    public static void bdrv_init()
    {
        //module_call_init(MODULE_INIT_BLOCK);
    }

//    void bdrv_init_with_whitelist()
//    {
//        use_bdrv_whitelist = 1;
//        bdrv_init();
//    }
//
//    void *qemu_aio_get(AIOPool *pool, BlockDriverState bs,
//                       BlockDriverCompletionFunc *cb, void *opaque)
//    {
//        BlockDriverAIOCB *acb;
//
//        if (pool.free_aiocb) {
//            acb = pool.free_aiocb;
//            pool.free_aiocb = acb.next;
//        } else {
//            acb = g_malloc0(pool.aiocb_size);
//            acb.pool = pool;
//        }
//        acb.bs = bs;
//        acb.cb = cb;
//        acb.opaque = opaque;
//        return acb;
//    }
//
//    void qemu_aio_release(void *p)
//    {
//        BlockDriverAIOCB *acb = (BlockDriverAIOCB *)p;
//        AIOPool *pool = acb.pool;
//        acb.next = pool.free_aiocb;
//        pool.free_aiocb = acb;
//    }
//
//    /**************************************************************/
//    /* Coroutine block device emulation */
//
//    typedef struct CoroutineIOCompletion {
//        Coroutine *coroutine;
//        int ret;
//    } CoroutineIOCompletion;
//
//    static void bdrv_co_io_em_complete(void *opaque, int ret)
//    {
//        CoroutineIOCompletion *co = opaque;
//
//        co.ret = ret;
//        qemu_coroutine_enter(co.coroutine, NULL);
//    }
//
//    static int coroutine_fn bdrv_co_io_em(BlockDriverState bs, int64_t sector_num,
//                                          int nb_sectors, QEMUIOVector *iov,
//                                          bool is_write)
//    {
//        CoroutineIOCompletion co = {
//            .coroutine = qemu_coroutine_self(),
//        };
//        BlockDriverAIOCB *acb;
//
//        if (is_write) {
//            acb = bs.drv.bdrv_aio_writev(bs, sector_num, iov, nb_sectors,
//                                           bdrv_co_io_em_complete, &co);
//        } else {
//            acb = bs.drv.bdrv_aio_readv(bs, sector_num, iov, nb_sectors,
//                                          bdrv_co_io_em_complete, &co);
//        }
//
//        trace_bdrv_co_io_em(bs, sector_num, nb_sectors, is_write, acb);
//        if (!acb) {
//            return -EIO;
//        }
//        qemu_coroutine_yield();
//
//        return co.ret;
//    }
//
//    static int coroutine_fn bdrv_co_readv_em(BlockDriverState bs,
//                                             int64_t sector_num, int nb_sectors,
//                                             QEMUIOVector *iov)
//    {
//        return bdrv_co_io_em(bs, sector_num, nb_sectors, iov, false);
//    }
//
//    static int coroutine_fn bdrv_co_writev_em(BlockDriverState bs,
//                                             int64_t sector_num, int nb_sectors,
//                                             QEMUIOVector *iov)
//    {
//        return bdrv_co_io_em(bs, sector_num, nb_sectors, iov, true);
//    }
//
//    static void coroutine_fn bdrv_flush_co_entry(void *opaque)
//    {
//        RwCo *rwco = opaque;
//
//        rwco.ret = bdrv_co_flush(rwco.bs);
//    }
//
//    int coroutine_fn bdrv_co_flush(BlockDriverState bs)
//    {
//        int ret;
//
//        if (!bs || !bdrv_is_inserted(bs) || bdrv_is_read_only(bs)) {
//            return 0;
//        }
//
//        /* Write back cached data to the OS even with cache=unsafe */
//        if (bs.drv.bdrv_co_flush_to_os) {
//            ret = bs.drv.bdrv_co_flush_to_os(bs);
//            if (ret < 0) {
//                return ret;
//            }
//        }
//
//        /* But don't actually force it to the disk with cache=unsafe */
//        if (bs.open_flags & BDRV_O_NO_FLUSH) {
//            goto flush_parent;
//        }
//
//        if (bs.drv.bdrv_co_flush_to_disk) {
//            ret = bs.drv.bdrv_co_flush_to_disk(bs);
//        } else if (bs.drv.bdrv_aio_flush) {
//            BlockDriverAIOCB *acb;
//            CoroutineIOCompletion co = {
//                .coroutine = qemu_coroutine_self(),
//            };
//
//            acb = bs.drv.bdrv_aio_flush(bs, bdrv_co_io_em_complete, &co);
//            if (acb == NULL) {
//                ret = -EIO;
//            } else {
//                qemu_coroutine_yield();
//                ret = co.ret;
//            }
//        } else {
//            /*
//             * Some block drivers always operate in either writethrough or unsafe
//             * mode and don't support bdrv_flush therefore. Usually qemu doesn't
//             * know how the server works (because the behaviour is hardcoded or
//             * depends on server-side configuration), so we can't ensure that
//             * everything is safe on disk. Returning an error doesn't work because
//             * that would break guests even if the server operates in writethrough
//             * mode.
//             *
//             * Let's hope the user knows what he's doing.
//             */
//            ret = 0;
//        }
//        if (ret < 0) {
//            return ret;
//        }
//
//        /* Now flush the underlying protocol.  It will also have BDRV_O_NO_FLUSH
//         * in the case of cache=unsafe, so there are no useless flushes.
//         */
//    flush_parent:
//        return bdrv_co_flush(bs.file);
//    }
//
//    void bdrv_invalidate_cache(BlockDriverState bs)
//    {
//        if (bs.drv && bs.drv.bdrv_invalidate_cache) {
//            bs.drv.bdrv_invalidate_cache(bs);
//        }
//    }
//
//    void bdrv_invalidate_cache_all(void)
//    {
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            bdrv_invalidate_cache(bs);
//        }
//    }
//
//    void bdrv_clear_incoming_migration_all(void)
//    {
//        BlockDriverState bs;
//
//        QTAILQ_FOREACH(bs, &bdrv_states, list) {
//            bs.open_flags = bs.open_flags & ~(BDRV_O_INCOMING);
//        }
//    }
//
//    int bdrv_flush(BlockDriverState bs)
//    {
//        Coroutine *co;
//        RwCo rwco = {
//            .bs = bs,
//            .ret = NOT_DONE,
//        };
//
//        if (qemu_in_coroutine()) {
//            /* Fast-path if already in coroutine context */
//            bdrv_flush_co_entry(&rwco);
//        } else {
//            co = qemu_coroutine_create(bdrv_flush_co_entry);
//            qemu_coroutine_enter(co, &rwco);
//            while (rwco.ret == NOT_DONE) {
//                qemu_aio_wait();
//            }
//        }
//
//        return rwco.ret;
//    }
//
//    static void coroutine_fn bdrv_discard_co_entry(void *opaque)
//    {
//        RwCo *rwco = opaque;
//
//        rwco.ret = bdrv_co_discard(rwco.bs, rwco.sector_num, rwco.nb_sectors);
//    }
//
//    int coroutine_fn bdrv_co_discard(BlockDriverState bs, int64_t sector_num,
//                                     int nb_sectors)
//    {
//        if (!bs.drv) {
//            return -ENOMEDIUM;
//        } else if (bdrv_check_request(bs, sector_num, nb_sectors)) {
//            return -EIO;
//        } else if (bs.read_only) {
//            return -EROFS;
//        } else if (bs.drv.bdrv_co_discard) {
//            return bs.drv.bdrv_co_discard(bs, sector_num, nb_sectors);
//        } else if (bs.drv.bdrv_aio_discard) {
//            BlockDriverAIOCB *acb;
//            CoroutineIOCompletion co = {
//                .coroutine = qemu_coroutine_self(),
//            };
//
//            acb = bs.drv.bdrv_aio_discard(bs, sector_num, nb_sectors,
//                                            bdrv_co_io_em_complete, &co);
//            if (acb == NULL) {
//                return -EIO;
//            } else {
//                qemu_coroutine_yield();
//                return co.ret;
//            }
//        } else {
//            return 0;
//        }
//    }
//
//    int bdrv_discard(BlockDriverState bs, int64_t sector_num, int nb_sectors)
//    {
//        Coroutine *co;
//        RwCo rwco = {
//            .bs = bs,
//            .sector_num = sector_num,
//            .nb_sectors = nb_sectors,
//            .ret = NOT_DONE,
//        };
//
//        if (qemu_in_coroutine()) {
//            /* Fast-path if already in coroutine context */
//            bdrv_discard_co_entry(&rwco);
//        } else {
//            co = qemu_coroutine_create(bdrv_discard_co_entry);
//            qemu_coroutine_enter(co, &rwco);
//            while (rwco.ret == NOT_DONE) {
//                qemu_aio_wait();
//            }
//        }
//
//        return rwco.ret;
//    }

    /**************************************************************/
    /* removable device support */

    /**
     * Return TRUE if the media is present
     */
    static public boolean bdrv_is_inserted(BlockDriverState bs)
    {
        BlockDriver drv = bs.drv;

        if (drv==null)
            return false;
        if (!drv.has_bdrv_is_inserted())
            return true;
        return drv.bdrv_is_inserted(bs);
    }

    /**
     * Return whether the media changed since the last call to this
     * function, or -ENOTSUP if we don't know.  Most drivers don't know.
     */
    static private int bdrv_media_changed(BlockDriverState bs)
    {
        BlockDriver drv = bs.drv;

        if (drv!=null && drv.has_bdrv_media_changed()) {
            return drv.bdrv_media_changed(bs);
        }
        return -Error.ENOTSUP;
    }

    /**
     * If eject_flag is TRUE, eject the media. Otherwise, close the tray
     */
    static public void bdrv_eject(BlockDriverState bs, boolean eject_flag)
    {
        BlockDriver drv = bs.drv;

        if (drv!=null && drv.has_bdrv_eject()) {
            drv.bdrv_eject(bs, eject_flag);
        }

        if (bs.device_name.length()>0) {
            bdrv_emit_qmp_eject_event(bs, eject_flag);
        }
    }

    /**
     * Lock or unlock the media (if it is locked, the user won't be able
     * to eject it manually).
     */
    static public void bdrv_lock_medium(BlockDriverState bs, boolean locked)
    {
        BlockDriver drv = bs.drv;

        //trace_bdrv_lock_medium(bs, locked);

        if (drv!=null && drv.has_bdrv_lock_medium()) {
            drv.bdrv_lock_medium(bs, locked);
        }
    }

//    /* needed for generic scsi interface */
//
//    int bdrv_ioctl(BlockDriverState bs, unsigned long int req, void *buf)
//    {
//        BlockDriver *drv = bs.drv;
//
//        if (drv && drv.bdrv_ioctl)
//            return drv.bdrv_ioctl(bs, req, buf);
//        return -ENOTSUP;
//    }
//
//    BlockDriverAIOCB *bdrv_aio_ioctl(BlockDriverState bs,
//            unsigned long int req, void *buf,
//            BlockDriverCompletionFunc *cb, void *opaque)
//    {
//        BlockDriver *drv = bs.drv;
//
//        if (drv && drv.bdrv_aio_ioctl)
//            return drv.bdrv_aio_ioctl(bs, req, buf, cb, opaque);
//        return NULL;
//    }

    static public void bdrv_set_buffer_alignment(BlockDriverState bs, int align)
    {
        bs.buffer_alignment = align;
    }

//    void *qemu_blockalign(BlockDriverState bs, size_t size)
//    {
//        return qemu_memalign((bs && bs.buffer_alignment) ? bs.buffer_alignment : 512, size);
//    }
//
//    void bdrv_set_dirty_tracking(BlockDriverState bs, int enable)
//    {
//        int64_t bitmap_size;
//
//        bs.dirty_count = 0;
//        if (enable) {
//            if (!bs.dirty_bitmap) {
//                bitmap_size = (bdrv_getlength(bs) >> BDRV_SECTOR_BITS) +
//                        BDRV_SECTORS_PER_DIRTY_CHUNK * BITS_PER_LONG - 1;
//                bitmap_size /= BDRV_SECTORS_PER_DIRTY_CHUNK * BITS_PER_LONG;
//
//                bs.dirty_bitmap = g_new0(unsigned long, bitmap_size);
//            }
//        } else {
//            if (bs.dirty_bitmap) {
//                g_free(bs.dirty_bitmap);
//                bs.dirty_bitmap = NULL;
//            }
//        }
//    }
//
//    int bdrv_get_dirty(BlockDriverState bs, int64_t sector)
//    {
//        int64_t chunk = sector / (int64_t)BDRV_SECTORS_PER_DIRTY_CHUNK;
//
//        if (bs.dirty_bitmap &&
//            (sector << BDRV_SECTOR_BITS) < bdrv_getlength(bs)) {
//            return !!(bs.dirty_bitmap[chunk / (sizeof(unsigned long) * 8)] &
//                (1UL << (chunk % (sizeof(unsigned long) * 8))));
//        } else {
//            return 0;
//        }
//    }
//
//    void bdrv_reset_dirty(BlockDriverState bs, int64_t cur_sector,
//                          int nr_sectors)
//    {
//        set_dirty_bitmap(bs, cur_sector, nr_sectors, 0);
//    }
//
//    int64_t bdrv_get_dirty_count(BlockDriverState bs)
//    {
//        return bs.dirty_count;
//    }
//
//    void bdrv_set_in_use(BlockDriverState bs, int in_use)
//    {
//        assert(bs.in_use != in_use);
//        bs.in_use = in_use;
//    }
//
//    int bdrv_in_use(BlockDriverState bs)
//    {
//        return bs.in_use;
//    }

    static public void bdrv_iostatus_enable(BlockDriverState bs) {
        bs.iostatus_enabled = true;
        bs.iostatus = BLOCK_DEVICE_IO_STATUS_OK;
    }
//
//    /* The I/O status is only enabled if the drive explicitly
//     * enables it _and_ the VM is configured to stop on errors */
//    bool bdrv_iostatus_is_enabled(const BlockDriverState bs)
//    {
//        return (bs.iostatus_enabled &&
//               (bs.on_write_error == BLOCK_ERR_STOP_ENOSPC ||
//                bs.on_write_error == BLOCK_ERR_STOP_ANY    ||
//                bs.on_read_error == BLOCK_ERR_STOP_ANY));
//    }
//
    static private void bdrv_iostatus_disable(BlockDriverState bs)
    {
        bs.iostatus_enabled = false;
    }

//    static private void bdrv_iostatus_reset(BlockDriverState bs)
//    {
//        if (bdrv_iostatus_is_enabled(bs)) {
//            bs.iostatus = BLOCK_DEVICE_IO_STATUS_OK;
//        }
//    }

    /* XXX: Today this is set by device models because it makes the implementation
       quite simple. However, the block layer knows about the error, so it's
       possible to implement this without device models being involved */
    static public void bdrv_iostatus_set_err(BlockDriverState bs, int error) {
//        if (bdrv_iostatus_is_enabled(bs) &&
//            bs.iostatus == BLOCK_DEVICE_IO_STATUS_OK) {
//            assert(error >= 0);
//            bs.iostatus = error == ENOSPC ? BLOCK_DEVICE_IO_STATUS_NOSPACE :
//                                             BLOCK_DEVICE_IO_STATUS_FAILED;
//        }
    }

    static public void bdrv_acct_start(BlockDriverState bs, BlockAcctCookie cookie, long bytes, int type) {
        //assert(type < BDRV_MAX_IOTYPE);

        cookie.bytes = bytes;
        cookie.start_time_ns = get_clock();
        cookie.type = type;
    }

    static public void bdrv_acct_done(BlockDriverState bs, BlockAcctCookie cookie) {
        //assert(cookie.type < BDRV_MAX_IOTYPE);

        bs.nr_bytes[cookie.type] += cookie.bytes;
        bs.nr_ops[cookie.type]++;
        bs.total_time_ns[cookie.type] += get_clock() - cookie.start_time_ns;
    }
//
//    int bdrv_img_create(const char *filename, const char *fmt,
//                        const char *base_filename, const char *base_fmt,
//                        char *options, uint64_t img_size, int flags)
//    {
//        QEMUOptionParameter *param = NULL, *create_options = NULL;
//        QEMUOptionParameter *backing_fmt, *backing_file, *size;
//        BlockDriverState bs = NULL;
//        BlockDriver *drv, *proto_drv;
//        BlockDriver *backing_drv = NULL;
//        int ret = 0;
//
//        /* Find driver and parse its options */
//        drv = bdrv_find_format(fmt);
//        if (!drv) {
//            error_report("Unknown file format '%s'", fmt);
//            ret = -EINVAL;
//            goto out;
//        }
//
//        proto_drv = bdrv_find_protocol(filename);
//        if (!proto_drv) {
//            error_report("Unknown protocol '%s'", filename);
//            ret = -EINVAL;
//            goto out;
//        }
//
//        create_options = append_option_parameters(create_options,
//                                                  drv.create_options);
//        create_options = append_option_parameters(create_options,
//                                                  proto_drv.create_options);
//
//        /* Create parameter list with default values */
//        param = parse_option_parameters("", create_options, param);
//
//        set_option_parameter_int(param, BLOCK_OPT_SIZE, img_size);
//
//        /* Parse -o options */
//        if (options) {
//            param = parse_option_parameters(options, create_options, param);
//            if (param == NULL) {
//                error_report("Invalid options for file format '%s'.", fmt);
//                ret = -EINVAL;
//                goto out;
//            }
//        }
//
//        if (base_filename) {
//            if (set_option_parameter(param, BLOCK_OPT_BACKING_FILE,
//                                     base_filename)) {
//                error_report("Backing file not supported for file format '%s'",
//                             fmt);
//                ret = -EINVAL;
//                goto out;
//            }
//        }
//
//        if (base_fmt) {
//            if (set_option_parameter(param, BLOCK_OPT_BACKING_FMT, base_fmt)) {
//                error_report("Backing file format not supported for file "
//                             "format '%s'", fmt);
//                ret = -EINVAL;
//                goto out;
//            }
//        }
//
//        backing_file = get_option_parameter(param, BLOCK_OPT_BACKING_FILE);
//        if (backing_file && backing_file.value.s) {
//            if (!strcmp(filename, backing_file.value.s)) {
//                error_report("Error: Trying to create an image with the "
//                             "same filename as the backing file");
//                ret = -EINVAL;
//                goto out;
//            }
//        }
//
//        backing_fmt = get_option_parameter(param, BLOCK_OPT_BACKING_FMT);
//        if (backing_fmt && backing_fmt.value.s) {
//            backing_drv = bdrv_find_format(backing_fmt.value.s);
//            if (!backing_drv) {
//                error_report("Unknown backing file format '%s'",
//                             backing_fmt.value.s);
//                ret = -EINVAL;
//                goto out;
//            }
//        }
//
//        // The size for the image must always be specified, with one exception:
//        // If we are using a backing file, we can obtain the size from there
//        size = get_option_parameter(param, BLOCK_OPT_SIZE);
//        if (size && size.value.n == -1) {
//            if (backing_file && backing_file.value.s) {
//                uint64_t size;
//                char buf[32];
//                int back_flags;
//
//                /* backing files always opened read-only */
//                back_flags =
//                    flags & ~(BDRV_O_RDWR | BDRV_O_SNAPSHOT | BDRV_O_NO_BACKING);
//
//                bs = bdrv_new("");
//
//                ret = bdrv_open(bs, backing_file.value.s, back_flags, backing_drv);
//                if (ret < 0) {
//                    error_report("Could not open '%s'", backing_file.value.s);
//                    goto out;
//                }
//                bdrv_get_geometry(bs, &size);
//                size *= 512;
//
//                snprintf(buf, sizeof(buf), "%" PRId64, size);
//                set_option_parameter(param, BLOCK_OPT_SIZE, buf);
//            } else {
//                error_report("Image creation needs a size parameter");
//                ret = -EINVAL;
//                goto out;
//            }
//        }
//
//        printf("Formatting '%s', fmt=%s ", filename, fmt);
//        print_option_parameters(param);
//        puts("");
//
//        ret = bdrv_create(drv, filename, param);
//
//        if (ret < 0) {
//            if (ret == -ENOTSUP) {
//                error_report("Formatting or formatting option not supported for "
//                             "file format '%s'", fmt);
//            } else if (ret == -EFBIG) {
//                error_report("The image size is too large for file format '%s'",
//                             fmt);
//            } else {
//                error_report("%s: error while creating %s: %s", filename, fmt,
//                             strerror(-ret));
//            }
//        }
//
//    out:
//        free_option_parameters(create_options);
//        free_option_parameters(param);
//
//        if (bs) {
//            bdrv_delete(bs);
//        }
//
//        return ret;
//    }
//
//    void *block_job_create(const BlockJobType *job_type, BlockDriverState bs,
//                           int64_t speed, BlockDriverCompletionFunc *cb,
//                           void *opaque, Error **errp)
//    {
//        BlockJob *job;
//
//        if (bs.job || bdrv_in_use(bs)) {
//            error_set(errp, QERR_DEVICE_IN_USE, bdrv_get_device_name(bs));
//            return NULL;
//        }
//        bdrv_set_in_use(bs, 1);
//
//        job = g_malloc0(job_type.instance_size);
//        job.job_type      = job_type;
//        job.bs            = bs;
//        job.cb            = cb;
//        job.opaque        = opaque;
//        job.busy          = true;
//        bs.job = job;
//
//        /* Only set speed when necessary to avoid NotSupported error */
//        if (speed != 0) {
//            Error *local_err = NULL;
//
//            block_job_set_speed(job, speed, &local_err);
//            if (error_is_set(&local_err)) {
//                bs.job = NULL;
//                g_free(job);
//                bdrv_set_in_use(bs, 0);
//                error_propagate(errp, local_err);
//                return NULL;
//            }
//        }
//        return job;
//    }
//
//    void block_job_complete(BlockJob *job, int ret)
//    {
//        BlockDriverState bs = job.bs;
//
//        assert(bs.job == job);
//        job.cb(job.opaque, ret);
//        bs.job = NULL;
//        g_free(job);
//        bdrv_set_in_use(bs, 0);
//    }
//
//    void block_job_set_speed(BlockJob *job, int64_t speed, Error **errp)
//    {
//        Error *local_err = NULL;
//
//        if (!job.job_type.set_speed) {
//            error_set(errp, QERR_NOT_SUPPORTED);
//            return;
//        }
//        job.job_type.set_speed(job, speed, &local_err);
//        if (error_is_set(&local_err)) {
//            error_propagate(errp, local_err);
//            return;
//        }
//
//        job.speed = speed;
//    }
//
//    void block_job_cancel(BlockJob *job)
//    {
//        job.cancelled = true;
//        if (job.co && !job.busy) {
//            qemu_coroutine_enter(job.co, NULL);
//        }
//    }
//
//    bool block_job_is_cancelled(BlockJob *job)
//    {
//        return job.cancelled;
//    }
//
//    struct BlockCancelData {
//        BlockJob *job;
//        BlockDriverCompletionFunc *cb;
//        void *opaque;
//        bool cancelled;
//        int ret;
//    };
//
//    static void block_job_cancel_cb(void *opaque, int ret)
//    {
//        struct BlockCancelData *data = opaque;
//
//        data.cancelled = block_job_is_cancelled(data.job);
//        data.ret = ret;
//        data.cb(data.opaque, ret);
//    }
//
//    int block_job_cancel_sync(BlockJob *job)
//    {
//        struct BlockCancelData data;
//        BlockDriverState bs = job.bs;
//
//        assert(bs.job == job);
//
//        /* Set up our own callback to store the result and chain to
//         * the original callback.
//         */
//        data.job = job;
//        data.cb = job.cb;
//        data.opaque = job.opaque;
//        data.ret = -EINPROGRESS;
//        job.cb = block_job_cancel_cb;
//        job.opaque = &data;
//        block_job_cancel(job);
//        while (data.ret == -EINPROGRESS) {
//            qemu_aio_wait();
//        }
//        return (data.cancelled && data.ret == 0) ? -ECANCELED : data.ret;
//    }
//
//    void block_job_sleep_ns(BlockJob *job, QEMUClock *clock, int64_t ns)
//    {
//        /* Check cancellation *before* setting busy = false, too!  */
//        if (!block_job_is_cancelled(job)) {
//            job.busy = false;
//            co_sleep_ns(clock, ns);
//            job.busy = true;
//        }
//    }

}
