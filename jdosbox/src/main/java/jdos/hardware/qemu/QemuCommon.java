package jdos.hardware.qemu;

public class QemuCommon {
    public static class iovec {
        public byte[] iov_base;
        public int iov_base_offset;
        public int iov_len;
    }

    public static void qemu_iovec_init_external(QEMUIOVector qiov, iovec iov, int niov) {
    }

    public static class QEMUIOVector {
        public iovec iov;
        public int niov;
        public int nalloc;
        public int size;
    }
}
