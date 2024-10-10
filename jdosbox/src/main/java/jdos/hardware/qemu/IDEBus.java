package jdos.hardware.qemu;

import jdos.hardware.IoHandler;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;

public class IDEBus extends Module_base {
    static private final int[] IDE_default_IRQs = new int[]{
            14,    /* primary */
            15,    /* secondary */
            11,    /* tertiary */
            10    /* quaternary */
    };

    static private final int[] IDE_default_bases = new int[]{
            0x1F0,    /* primary */
            0x170,    /* secondary */
            0x1E8,    /* tertiary */
            0x168    /* quaternary */
    };

    static private final int[] IDE_default_alts = new int[]{
            0x3F6,    /* primary */
            0x376,    /* secondary */
            0x3EE,    /* tertiary */
            0x36E    /* quaternary */
    };

    public IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[8];
    public IoHandler.IO_ReadHandleObject[] ReadHandlerW = new IoHandler.IO_ReadHandleObject[2];
    public IoHandler.IO_ReadHandleObject[] ReadHandlerD = new IoHandler.IO_ReadHandleObject[4];
    public IoHandler.IO_ReadHandleObject ReadHandlerAlt = new IoHandler.IO_ReadHandleObject();

    public IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[8];
    public IoHandler.IO_WriteHandleObject[] WriteHandlerW = new IoHandler.IO_WriteHandleObject[2];
    public IoHandler.IO_WriteHandleObject[] WriteHandlerD = new IoHandler.IO_WriteHandleObject[4];
    public IoHandler.IO_WriteHandleObject WriteHandlerAlt = new IoHandler.IO_WriteHandleObject();

    //BusState qbus;
    Internal.IDEDevice master = new Internal.IDEDevice();
    Internal.IDEDevice slave = new Internal.IDEDevice();
    Internal.IDEState[] ifs = new Internal.IDEState[2];
    int bus_id;
    Internal.IDEDMA dma = new Internal.IDEDMA();
    int unit;
    int cmd;
    int irq;
    int base_io = 0;
    int alt_io = 0;

    int error_status;

    public IDEBus(Section configuration, int index) {
        super(configuration);
        Section_prop section = (Section_prop) configuration;
        base_io = 0;
        alt_io = 0;

        if (index < IDE_default_IRQs.length) {
            base_io = IDE_default_bases[index];
            alt_io = IDE_default_alts[index];
            irq = IDE_default_IRQs[index];
        }

        for (int i=0;i< ifs.length;i++)
            ifs[i] = new Internal.IDEState();

        IDE.ide_init2(this, irq);
    }

    public void initIO() {
        if (base_io != 0 || alt_io != 0 || irq >= 0)
            System.out.println("IDE: Adding IDE controller to port 0x" + Integer.toHexString(base_io) + "/%03x IRQ " + alt_io);

        for (int i = 0; i < WriteHandler.length; i++)
            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
        for (int i = 0; i < ReadHandler.length; i++)
            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
        for (int i = 0; i < WriteHandlerW.length; i++)
            WriteHandlerW[i] = new IoHandler.IO_WriteHandleObject();
        for (int i = 0; i < ReadHandlerW.length; i++)
            ReadHandlerW[i] = new IoHandler.IO_ReadHandleObject();
        for (int i = 0; i < WriteHandlerD.length; i++)
            WriteHandlerD[i] = new IoHandler.IO_WriteHandleObject();
        for (int i = 0; i < ReadHandlerD.length; i++)
            ReadHandlerD[i] = new IoHandler.IO_ReadHandleObject();

        if (base_io != 0) {
            for (int i = 0; i < 8; i++) {
                WriteHandler[i].Install(base_io + i, IDE.ide_ioport_write_handler, IoHandler.IO_MB);
                ReadHandler[i].Install(base_io + i, IDE.ide_ioport_read_handler, IoHandler.IO_MB);
            }
            for (int i = 0; i < 2; i++) {
                WriteHandlerW[i].Install(base_io + i, IDE.ide_data_writew_handler, IoHandler.IO_MW);
                ReadHandlerW[i].Install(base_io + i, IDE.ide_data_readw_handler, IoHandler.IO_MW);
            }
            for (int i = 0; i < 4; i++) {
                WriteHandlerD[i].Install(base_io + i, IDE.ide_data_writel_handler, IoHandler.IO_MD);
                ReadHandlerD[i].Install(base_io + i, IDE.ide_data_readl_handler, IoHandler.IO_MD);
            }
        }

        if (alt_io != 0) {
            WriteHandlerAlt.Install(alt_io, IDE.ide_cmd_write_handler, IoHandler.IO_MB);
            ReadHandlerAlt.Install(alt_io, IDE.ide_status_read_handler, IoHandler.IO_MB);
        }
    }
}