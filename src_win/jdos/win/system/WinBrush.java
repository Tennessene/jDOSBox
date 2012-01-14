package jdos.win.system;

public class WinBrush extends WinObject {
    public int color;

    public WinBrush(int handle, int color) {
        super(handle);
        this.color = color;
    }
}
