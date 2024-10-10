package jdos.hardware.pci;

import jdos.cpu.Paging;
import jdos.hardware.Memory;

public class PCI_Memory_BAR extends PCI_BAR {
    PCI_PageHandler handler;

    public PCI_Memory_BAR(PCI_PageHandler handler) {
        this.handler = handler;
        Memory.MEM_AddPCIPageHandler(this.handler);
    }

    public int getBAR(int currentValue) {
        if (currentValue == -1)
            return 0xFF000000;
        return handler.start_page << 12;
    }

    public void setBAR(int newValue) {
        if (handler.start_page != (newValue >>> 12)) {
            handler.start_page = newValue >>> 12;
            Paging.PAGING_ClearTLB();
        }
    }
}
