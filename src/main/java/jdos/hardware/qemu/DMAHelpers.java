package jdos.hardware.qemu;

public class DMAHelpers {
    static public void qemu_sglist_destroy(DMA.QEMUSGList qsg) {

    }

    static private DMA.DMAIOFunc bdrv_aio_readv = new DMA.DMAIOFunc() {
        public Block.BlockDriverAIOCB call(Block.BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector iov, int nb_sectors, Block.BlockDriverCompletionFunc cb, Object opaque) {
            return Block.bdrv_aio_readv(bs, sector_num, iov, nb_sectors, cb, opaque);
        }
    };

    static private DMA.DMAIOFunc bdrv_aio_writev = new DMA.DMAIOFunc() {
        public Block.BlockDriverAIOCB call(Block.BlockDriverState bs, long sector_num, QemuCommon.QEMUIOVector iov, int nb_sectors, Block.BlockDriverCompletionFunc cb, Object opaque) {
            return Block.bdrv_aio_writev(bs, sector_num, iov, nb_sectors, cb, opaque);
        }
    };

    static public Block.BlockDriverAIOCB dma_bdrv_io(Block.BlockDriverState bs, DMA.QEMUSGList sg, long sector_num, DMA.DMAIOFunc io_func, Block.BlockDriverCompletionFunc cb, Object opaque, int dir) {
        return io_func.call(bs, sector_num, null, 1, cb, opaque);
        /*
        DMAAIOCB *dbs = qemu_aio_get(&dma_aio_pool, bs, cb, opaque);

        trace_dma_bdrv_io(dbs, bs, sector_num, (dir == DMA_DIRECTION_TO_DEVICE));

        dbs->acb = NULL;
        dbs->bs = bs;
        dbs->sg = sg;
        dbs->sector_num = sector_num;
        dbs->sg_cur_index = 0;
        dbs->sg_cur_byte = 0;
        dbs->dir = dir;
        dbs->io_func = io_func;
        dbs->bh = NULL;
        qemu_iovec_init(&dbs->iov, sg->nsg);
        dma_bdrv_cb(dbs, 0);
        return &dbs->common;
        */
    }


    static public Block.BlockDriverAIOCB dma_bdrv_read(Block.BlockDriverState bs, DMA.QEMUSGList sg, long sector,  Block.BlockDriverCompletionFunc cb, Object opaque) {
        return dma_bdrv_io(bs, sg, sector, bdrv_aio_readv, cb, opaque, DMA.DMA_DIRECTION_FROM_DEVICE);
    }

    static public Block.BlockDriverAIOCB dma_bdrv_write(Block.BlockDriverState bs, DMA.QEMUSGList sg, long sector, Block.BlockDriverCompletionFunc cb, Object opaque) {
        return dma_bdrv_io(bs, sg, sector, bdrv_aio_writev, cb, opaque, DMA.DMA_DIRECTION_TO_DEVICE);
    }
}
