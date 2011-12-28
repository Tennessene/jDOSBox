package jdos.win.utils;

public class WinObject {
    public WinObject(String name, int handle) {
        this.name = name;
        this.handle = handle;
    }

    public WinObject(int handle) {
        this.handle = handle;
        this.name = null;
    }

    public int getHandle() {
        return handle;
    }

    public String name;
    public int handle;
}
