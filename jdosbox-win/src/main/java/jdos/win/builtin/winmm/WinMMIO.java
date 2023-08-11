package jdos.win.builtin.winmm;

import jdos.win.system.WinObject;

public class WinMMIO extends WinObject {
    static public WinMMIO create() {
        return new WinMMIO(nextObjectId());
    }

    static public WinMMIO get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinMMIO))
            return null;
        return (WinMMIO)object;
    }

    public WinMMIO(int id) {
        super(id);
    }

    protected void onFree() {
        super.onFree();
    }

    public MMIOINFO info = new MMIOINFO();
    public boolean bTmpIOProc;
    public Mmio.IOProc ioProc;
    public boolean bBufferLoaded;
    public int dwFileSize;
}
