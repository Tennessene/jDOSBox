package jdos.win.utils;

import java.awt.*;

public class WinFont extends WinObject {
    public Font font;

    public WinFont(int id, Font font) {
        super(id);
        this.font = font;
    }
}
