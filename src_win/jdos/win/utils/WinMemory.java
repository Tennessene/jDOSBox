package jdos.win.utils;

import jdos.hardware.Memory;

public class WinMemory {
    static public final int MEM_COMMIT = 0x1000;
    static public final int MEM_RESERVE = 0x2000;
    static public final int MEM_RESET = 0x80000;

    static public final int MEM_LARGE_PAGES = 0x20000000;
    static public final int MEM_PHYSICAL = 0x400000;
    static public final int MEM_TOP_DOWN = 0x100000;
    static public final int MEM_WRITE_WATCH = 0x200000;

    static public final int PAGE_NOACCESS = 0x01;
    static public final int PAGE_READONLY = 0x02;
    static public final int PAGE_READWRITE = 0x04;
    static public final int PAGE_WRITECOPY = 0x08;

    static public final int PAGE_EXECUTE = 0x10;
    static public final int PAGE_EXECUTE_READ = 0x20;
    static public final int PAGE_EXECUTE_READWRITE = 0x40;
    static public final int PAGE_EXECUTE_WRITECOPY = 0x80;

    static public final int PAGE_GUARD = 0x100;
    static public final int PAGE_NOCACHE = 0x200;
    static public final int PAGE_WRITECOMBINE = 0x400;

    private WinHeap heap;
    private int heapHandle;

    public WinMemory(WinHeap heap) {
        this.heap = heap;
        heapHandle = heap.createHeap(0, 0);
    }

    public int virtualAlloc(int address, int size, int flags, int protect) {
        // :TODO: could be implemented better with virtual memory
        if ((flags & MEM_RESERVE) != 0) {
            address = heap.allocateHeap(heapHandle, size);
        }
        if ((flags & MEM_COMMIT) != 0) {
            for (int i = 0; i < size; i++)
                Memory.mem_writeb(address + i, 0);
        }
        return address;
    }
}
