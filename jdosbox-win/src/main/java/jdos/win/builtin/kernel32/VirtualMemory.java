package jdos.win.builtin.kernel32;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.kernel.KernelMemory;
import jdos.win.system.WinSystem;

public class VirtualMemory {
    public VirtualMemory(long address, int size) {
        this.address = address;
        this.size = size;
    }
    public long address;
    public int size;

    public void commit(long p, int size) {
        if ((p & 0xFFF) != 0) {
            Win.panic("VirtualMemory.commit was expecting page aligned address");
        }
        int directory = WinSystem.getCurrentProcess().page_directory;
        for (long i=p;i<p+size;i+=0x1000) {
            int pagePtr = WinSystem.memory.get_page((int)i, true, directory);
            int page = Memory.mem_readd(pagePtr);
            if ((page & 0xFFFFF000)==0)
                KernelMemory.setPage(pagePtr, WinSystem.memory.getNextFrame(), false, true);
        }
        Memory.mem_zero((int)p, size);
    }

    public void decommit(long p, int size) {
        int directory = WinSystem.getCurrentProcess().page_directory;
        for (long i=p;i<p+size;i+=0x1000) {
            int pagePtr = WinSystem.memory.get_page((int)i, false, directory);
            if (pagePtr != 0)
                KernelMemory.clearPage(pagePtr);
        }
    }

    public void free() {
        decommit(address, size);
    }
}
