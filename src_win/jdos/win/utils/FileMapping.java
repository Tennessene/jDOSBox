package jdos.win.utils;

import jdos.hardware.Memory;
import jdos.win.kernel.KernelMemory;

public class FileMapping extends WinObject {
    public FileMapping(int fileHandle, String name, long size, int handle) {
        super(name, handle);
        this.fileHandle = fileHandle;
        this.size = ((int)size + 0xFFF) & ~0xFFF;
        this.frames = new int[(this.size >> 12) + 1];
        for (int i=0;i<frames.length;i++) {
            frames[i] = WinSystem.memory.getNextFrame();
            if (fileHandle == -1) {
                Memory.phys_zero(frames[i] << 12, 0x1000);
            } else {
                // :TODO:
            }
        }
    }

    // Offset is guaranteed to be multiple of 0x1000
    public int map(int offset, int size,  boolean writable) {
        if (size == 0)
            size = this.size - offset;
        int address = WinSystem.getCurrentProcess().reserveAddress(size+0x1000, true);
        int directory = WinSystem.getCurrentProcess().page_directory;
        int p = address;
        offset >>>= 12;
        int maxFrames = frames.length - (((size + 0xFFF) & ~0xFFF) >>> 12) + offset;

        // always map the first frame since it contains metadata
        int page = WinSystem.memory.get_page(p, true, directory);
        KernelMemory.setPage(page, frames[0], false, writable);
        p+=0x1000;

        for (int i=offset;i<maxFrames;i++) {
            page = WinSystem.memory.get_page(p, true, directory);
            KernelMemory.setPage(page, frames[i+1], false, writable);
            p+=0x1000;
        }
        open();

        Memory.mem_writed(address, getHandle());
        Memory.mem_writed(address+4, maxFrames-offset+1);
        return address+0x1000;
    }

    static public boolean unmap(int address) {
        int handle = Memory.mem_readd(address-0x1000);
        int frameCount = Memory.mem_readd(address-0x1000+4);
        WinObject object = WinSystem.getObject(handle);
        if (object instanceof FileMapping) {
            FileMapping mapping = (FileMapping)object;
            int directory = WinSystem.getCurrentProcess().page_directory;
            for (int i=0;i<frameCount;i++) {
                int page = WinSystem.memory.get_page(address+i*0x1000, true, directory);
                KernelMemory.clearPage(page);
            }
            WinSystem.getCurrentProcess().freeAddress(address-0x1000);
            mapping.close();
            return true;
        }
        return false;
    }

    public void onFree() {
        for (int i=0;i<frames.length;i++) {
            WinSystem.memory.freeFrame(frames[i]);
        }
    }

    private int fileHandle;
    private int size;
    private int[] frames;
}
