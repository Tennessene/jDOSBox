package jdos.win.utils;

import jdos.hardware.Memory;

import java.io.RandomAccessFile;

public class WinFile extends WinObject {
    public static final int STD_OUT = 1;
    public static final int STD_IN = 2;
    public static final int STD_ERROR = 3;

    public static final int FILE_TYPE_DISK = 0x0001; // The specified file is a disk file.
    public static final int FILE_TYPE_CHAR = 0x0002; // The specified file is a character file, typically an LPT device or a console.
    public static final int FILE_TYPE_PIPE = 0x0003; // The specified file is a socket, a named pipe, or an anonymous pipe.
    public static final int FILE_TYPE_REMOTE = 0x8000; // Unused.

    public static final int FILE_ATTRIBUTE_DIRECTORY = 0x10;
    public static final int FILE_ATTRIBUTE_NORMAL = 0x80;

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

    public long size() {
        if (file == null) {
            return 0;
        }
        try {
            return file.length();
        } catch (Exception e) {
        }
        return 0;
    }

    public long seek(long pos, int from) {
        if (file == null)
            return -1;
        try {
            if (from == 0) // FILE_BEGIN
                file.seek(pos);
            else if (from == 1) //
                file.skipBytes((int)pos);
            else if (from == 2) // FILE_END
                file.seek(file.length()-pos);
            return file.getFilePointer();
        } catch (Exception e) {
            return -1;
        }
    }

    public int read(int buffer, int size) {
        try {
            byte[] buf = new byte[size];
            int result = file.read(buf);
            Memory.mem_memcpy(buffer, buf,  0, size);
            return result;
        } catch (Exception e) {
            return 0;
        }
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
