package jdos.win.utils;

public class FileMapping extends WinObject {
    public FileMapping(int fileHandle, String name, int handle) {
        super(name, handle);
        this.fileHandle = fileHandle;
    }
    public int fileHandle;
    public int heapHandle = 0;
    public int heapSize = 0;
}
