package jdos.win.builtin.kernel32;

import jdos.win.builtin.WinAPI;

public class TIB extends WinAPI {
    int address;
    WinProcess process;
    int tls;
    int tlsSize;

    public TIB(WinProcess process, int threadId, int stackStart, int stackStop) {
        this.process = process;
        address = process.heap.alloc(4096, true);
        this.tlsSize = 256;
        this.tls = address + 0xE10;
        writed(address+0x04, stackStop);
        writed(address+0x08, stackStart);
        writed(address+0x18, address);
        writed(address+0x20, process.handle);
        writed(address+0x24, threadId);
        writed(address+0x2C, tls);
        writed(address+0x6E8, process.handle);
        writed(address+0x6EC, threadId);
    }

    public void close() {
        process.heap.free(tls);
        process.heap.free(address);
    }
}
