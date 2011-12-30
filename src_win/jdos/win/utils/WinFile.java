package jdos.win.utils;

import java.io.RandomAccessFile;

public class WinFile extends WinObject {
    public static final int STD_OUT = 1;
    public static final int STD_IN = 2;
    public static final int STD_ERROR = 3;

    public static final int FILE_TYPE_DISK = 0x0001; // The specified file is a disk file.
    public static final int FILE_TYPE_CHAR = 0x0002; // The specified file is a character file, typically an LPT device or a console.
    public static final int FILE_TYPE_PIPE = 0x0003; // The specified file is a socket, a named pipe, or an anonymous pipe.
    public static final int FILE_TYPE_REMOTE = 0x8000; // Unused.

    public WinFile(int type, int handle) {
        super(handle);
        this.type = type;
    }
    public WinFile(int handle, RandomAccessFile file, int shareMode, int attributes) {
        super(handle);
        this.shareMode = shareMode;
        this.type = FILE_TYPE_DISK;
        this.file = file;
        this.attributes = attributes;
    }

    protected void onFree() {
        try {
            file.close();
        } catch (Exception e) {
        }
    }
    public RandomAccessFile file = null;
    public int shareMode;
    public int attributes;
    public int type;
}
