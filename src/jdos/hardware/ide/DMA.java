package jdos.hardware.ide;

import jdos.util.IntRef;

public class DMA {
    static public final int DMA_DIRECTION_TO_DEVICE = 0;
    static public final int DMA_DIRECTION_FROM_DEVICE = 1;

    static public interface DMATranslateFunc {
        public int call(DMAContext dma, int addr, IntRef paddr, IntRef len, int dir);
    }
    static public interface DMAMapFunc {
        public int call(DMAContext dma, int addr, IntRef len, int dir);
    }
    static public interface DMAUnmapFunc {
        public void call(DMAContext dma, int buffer, int len, int dir, int access_len);
    }

    static public interface DMAIOFunc {
        public Block.BlockDriverAIOCB call(Block.BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector iov, int nb_sectors, Block.BlockDriverCompletionFunc cb, Object opaque);
    }

    public static class DMAContext {
        DMATranslateFunc translate;
        DMAMapFunc map;
        DMAUnmapFunc unmap;
    }

    static public class ScatterGatherEntry {
        public int base;
        public int len;
    }
    public static class QEMUSGList {
        public ScatterGatherEntry sg;
        public int nsg;
        public int nalloc;
        public int size;
        DMAContext dma;
    }
}
