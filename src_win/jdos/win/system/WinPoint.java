package jdos.win.system;

public class WinPoint {
    public WinPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x;
    public int y;

    public WinPoint copy() {
        return new WinPoint(x, y);
    }
}
