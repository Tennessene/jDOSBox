package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.kernel.KernelMemory;

public class WinFileMapping extends WinObject {
    static public WinFileMapping create(int hFile, String name, long size) {
        return new WinFileMapping(hFile, name, size, nextObjectId());
    }

    static public WinFileMapping get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinFileMapping))
            return null;
        return (WinFileMapping)object;
    }

    public WinFileMapping(int fileHandle, String name, long size, int handle) {
        super(name, handle);
        this.fileHandle = fileHandle;
        WinFile file = null;
        byte[] buffer = null;

        if (fileHandle>0) {
            file = WinFile.get(fileHandle);
            size = file.size();
            file.seek(0, 0);
            buffer = new byte[4096];
            fileName = file.name;
        }
        this.size = ((int)size + 0xFFF) & ~0xFFF;

        this.frames = new int[(this.size >> 12) + 1];
        for (int i=0;i<frames.length;i++) {
            frames[i] = WinSystem.memory.getNextFrame();
            if (fileHandle == -1) {
                Memory.phys_zero(frames[i] << 12, 0x1000);
            } else if (i>0) {
                int len = 0;
                len = file.read(buffer);
                Memory.phys_memcpy(frames[i] << 12, buffer, 0, len);
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
        int maxFrames = (((size + 0xFFF) & ~0xFFF) >>> 12) + offset;

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
        WinFileMapping mapping = WinFileMapping.get(handle);
        if (mapping != null) {
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
        if (WinAPI.LOG) {
            System.out.println("Freeing File Mapping: handle="+handle+" name="+name+" fileName="+fileName);
        }
        for (int i=0;i<frames.length;i++) {
            WinSystem.memory.freeFrame(frames[i]);
        }
    }

    public int getSize() {
        return size;
    }

    private String fileName;
    private int fileHandle;
    private int size;
    private int[] frames;
}
