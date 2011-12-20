package jdos.win.utils;

public class File {
    public static final int FILE_TYPE_DISK = 0x0001; // The specified file is a disk file.
    public static final int FILE_TYPE_CHAR = 0x0002; // The specified file is a character file, typically an LPT device or a console.
    public static final int FILE_TYPE_PIPE = 0x0003; // The specified file is a socket, a named pipe, or an anonymous pipe.
    public static final int FILE_TYPE_REMOTE = 0x8000; // Unused.

    public File(int type, int handle) {
        this.type = type;
        this.handle = handle;
    }

    public int type;
    public int handle;
}
