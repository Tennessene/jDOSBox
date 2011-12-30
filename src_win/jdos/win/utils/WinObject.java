package jdos.win.utils;

public class WinObject {
    public WinObject(String name, int handle) {
        this.name = name;
        this.handle = handle;
        WinSystem.objects.put(new Integer(handle), this);
        open();
    }

    public WinObject(int handle) {
        this.handle = handle;
        this.name = null;
        WinSystem.objects.put(new Integer(handle), this);
        open();
    }

    public int getHandle() {
        return handle;
    }

    public void open() {
        refCount++;
    }
    public void close() {
        refCount--;
        if (refCount == 0) {
            WinSystem.removeObject(this);
            onFree();
        }
    }
    protected void onFree() {

    }

    public String name;
    public int handle;
    private int refCount = 0;
}
