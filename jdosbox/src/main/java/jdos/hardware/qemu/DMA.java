package jdos.hardware.qemu;

import jdos.cpu.CPU;
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

    public static int DMA_get_channel_mode(int nchan)
    {
        jdos.hardware.DMA.DmaChannel channel = jdos.hardware.DMA.GetDMAChannel(nchan);
        return channel.mode;
    }

    public static void DMA_hold_DREQ(int nchan)
    {
        jdos.hardware.DMA.DmaChannel channel = jdos.hardware.DMA.GetDMAChannel(nchan);
        channel.callback.call(channel, jdos.hardware.DMA.DMAEvent.DMA_UNMASKED);
        channel.Raise_Request();
        // DMA_run();
    }

    public static void DMA_release_DREQ(int nchan) {
        jdos.hardware.DMA.DmaChannel channel = jdos.hardware.DMA.GetDMAChannel(nchan);
        channel.Clear_Request();
        // DMA_run();
    }

    public static int DMA_read_memory (int nchan, byte[] buf, int bufOffset, int pos, int len)
    {
        jdos.hardware.DMA.DmaChannel channel = jdos.hardware.DMA.GetDMAChannel(nchan);
        return channel.Read(len, buf, bufOffset);
    }

    public static int DMA_write_memory (int nchan, byte[] buf, int bufOffset, int pos, int len)
    {
        jdos.hardware.DMA.DmaChannel channel = jdos.hardware.DMA.GetDMAChannel(nchan);
        return channel.Write(len, buf, bufOffset);
    }

    /* request the emulator to transfer a new DMA memory block ASAP */
    public static void DMA_schedule(int nchan)
    {
        //struct dma_cont *d = &dma_controllers[nchan > 3];
        //qemu_irq_pulse(*d->cpu_request_exit);
        CPU.CPU_CycleLeft = CPU.CPU_Cycles;
        CPU.CPU_Cycles = 0;
    }
}
