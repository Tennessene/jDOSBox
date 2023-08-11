package jdos.win.kernel;

import jdos.win.Win;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.system.WinSystem;

public class VideoMemory {
    static public int SIZE = 0; // MB
    static private int size;

    static public int mapVideoRAM(int amount) {
        if (size != 0) {
            Win.panic("Video memory doesn't support more than one mapping");
        }
        int frame = WinSystem.memory.frames.length*32;
        long address = WinSystem.getCurrentProcess().addressSpace.getNextAddress(WinProcess.ADDRESS_VIDEO_START, amount, true);
        WinSystem.getCurrentProcess().addressSpace.alloc(address, amount);
        frame += (address- WinProcess.ADDRESS_VIDEO_START + 0xFFF) >>> 12;
        int directory = WinSystem.getCurrentProcess().page_directory;
        for (long i=address;i<address+amount;i+=0x1000) {
            int pagePtr = WinSystem.memory.get_page((int)i, true, directory);
            KernelMemory.setPage(pagePtr, frame++, false, true);
        }
        size = amount;
        return (int)address;
    }

    static public void unmapVideoRAM(int address) {
        int directory = WinSystem.getCurrentProcess().page_directory;
        for (long i=address;i<address+size;i+=0x1000) {
            int pagePtr = WinSystem.memory.get_page((int)i, true, directory);
            KernelMemory.clearPage(pagePtr);
        }
        WinSystem.getCurrentProcess().addressSpace.free(address);
        size = 0;
    }
}
