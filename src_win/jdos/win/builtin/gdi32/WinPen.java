package jdos.win.builtin.gdi32;

import jdos.win.system.WinObject;
import jdos.win.utils.Ptr;

public class WinPen extends WinGDI {
    static public WinPen create(int style, int width, int color) {
        return new WinPen(nextObjectId(), style, width, color);
    }

    static public WinPen get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinPen))
            return null;
        return (WinPen)object;
    }

    // HPEN CreatePen(int fnPenStyle, int nWidth, COLORREF crColor)
    static public int CreatePen(int fnPenStyle, int nWidth, int crColor) {
        return create(fnPenStyle, nWidth, crColor).handle;
    }

    int style;
    int width;
    int color;

    public WinPen(int id, int style, int width, int color) {
        super(id);
        if (width==0)
            width=1;
        this.style = style;
        this.width = width;
        this.color = color;
    }

    public String toString() {
        return "PEN style="+style+" width="+width+" color=0x"+ Ptr.toString(color);
    }
}
