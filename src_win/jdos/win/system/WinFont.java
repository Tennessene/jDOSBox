package jdos.win.system;

import java.awt.*;

public class WinFont extends WinGDI {
    public Font font;

    public WinFont(int id, Font font) {
        super(id);
        this.font = font;
    }

    public String toString() {
        return "FONT "+font.getFontName()+" "+font.getSize()+"pt";
    }

    static public int JAVA_TO_WIN(int size) {
        return size * 96 / 72;
    }

    static public int WIN_TO_JAVA(int size) {
        return size * 72 / 96;
    }
}
