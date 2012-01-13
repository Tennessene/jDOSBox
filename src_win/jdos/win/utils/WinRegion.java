package jdos.win.utils;

import java.util.Vector;

public class WinRegion extends WinGDI {
    Vector rects = new Vector();

    public WinRegion(int id, WinRect rect) {
        super(id);
        rects.add(rect);
    }

    public String toString() {
        return "REGION";
    }
}
