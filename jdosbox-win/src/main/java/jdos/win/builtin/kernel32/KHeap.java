package jdos.win.builtin.kernel32;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinSystem;

public class KHeap extends WinAPI {
    // HLOCAL WINAPI LocalAlloc(UINT uFlags, SIZE_T uBytes)
    public static int LocalAlloc(int uFlags, int uBytes) {
        int result = WinSystem.getCurrentProcess().heap.alloc(uBytes+4, false);
        if ((uFlags & 0x0040)!=0)
            Memory.mem_zero(result, uBytes + 4);
        Memory.mem_writed(result, uBytes);
        return result+4;
    }

    // HLOCAL WINAPI LocalFree(HLOCAL hMem)
    public static int LocalFree(int hMem) {
        WinSystem.getCurrentProcess().heap.free(hMem-4);
        return 0;
    }

    // HLOCAL WINAPI LocalReAlloc(HLOCAL hMem, SIZE_T uBytes, UINT uFlags)
    public static int LocalReAlloc(int hMem, int uBytes, int uFlags) {
        int result = WinSystem.getCurrentProcess().heap.alloc(uBytes+4, false);
        if ((uFlags & 0x0040)!=0)
            Memory.mem_zero(result, uBytes + 4);
        writed(result, uBytes);
        int oldSize = readd(hMem);
        Memory.mem_memcpy(result+4, hMem+4, Math.min(uBytes, oldSize));
        return result;
    }
}
