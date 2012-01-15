package jdos.win.system;

public class WinPen extends WinGDI {
    int style;
    int width;
    int color;

    public WinPen(int id, int style, int width, int color) {
        super(id);
        this.style = style;
        this.width = width;
        this.color = color;
    }
}
