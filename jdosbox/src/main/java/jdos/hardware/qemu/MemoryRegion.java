package jdos.hardware.qemu;

import jdos.cpu.Paging;
import jdos.hardware.Memory;

public class MemoryRegion {
    Paging.PageHandler handler;

    public MemoryRegion(int address, int offset, int size, String name, Paging.PageHandler handler) {
        this.address = address;
        this.offset = offset;
        this.size = size;
        this.name = name;
        this.handler = handler;
        Memory.MEM_SetPageHandler( address, 8, handler );
    }
    public void destroy() {
        Memory.MEM_ResetPageHandler( address, size );
    }

    int address;
    int size;
    int offset;
    String name;
}
