package jdos.win.kernel;

import jdos.win.utils.WinProcess;
import jdos.win.utils.WinSystem;

public class VideoMemory {
    static public int SIZE = 0; // MB

    static public int mapVideoRAM(int amount) {
        int frame = WinSystem.memory.frames.length*32;
        long address = WinSystem.getCurrentProcess().addressSpace.getNextAddress(WinProcess.ADDRESS_VIDEO_START, amount, true);
        WinSystem.getCurrentProcess().addressSpace.alloc(address, amount);
        int directory = WinSystem.getCurrentProcess().page_directory;
        for (long i=address;i<address+amount;i+=0x1000) {
            int pagePtr = WinSystem.memory.get_page((int)i, true, directory);
            KernelMemory.setPage(pagePtr, frame++, false, true);
        }
        return (int)address;
    }

    static public void unmapVideoRAM(int address) {

    }
}
