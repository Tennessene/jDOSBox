package jdos.win.builtin.gdi32;

import jdos.win.system.WinObject;
import jdos.win.utils.Ptr;

public class WinBrush extends WinGDI {
    static public WinBrush create(int style, int color, int hatch) {
        return new WinBrush(nextObjectId(), style, color, hatch);
    }

    static public WinBrush get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinBrush))
            return null;
        return (WinBrush)object;
    }

    // HBRUSH CreateSolidBrush(COLORREF crColor)
    static public int CreateSolidBrush(int crColor) {
        return create(BS_SOLID, crColor, 0).handle;
    }

    public int color;
    public int style;
    public int hatch;

    public WinBrush(int handle, int style, int color, int hatch) {
        super(handle);
        this.color = color;
        this.style = style;
        this.hatch = hatch;
    }

    public String toString() {
        return "BRUSH color=0x"+Ptr.toString(color)+" style=0x"+Ptr.toString(style)+" hatch=0x"+Ptr.toString(hatch);
    }
}
