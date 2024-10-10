package jdos.win.builtin.gdi32;

import jdos.win.system.WinObject;

public class WinEnhancedMetaFile extends WinObject {
    static public WinEnhancedMetaFile create(int style, int color, int hatch) {
        return new WinEnhancedMetaFile(nextObjectId());
    }

    static public WinEnhancedMetaFile get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinEnhancedMetaFile))
            return null;
        return (WinEnhancedMetaFile)object;
    }

    public WinEnhancedMetaFile(int id) {
        super(id);
    }
}
