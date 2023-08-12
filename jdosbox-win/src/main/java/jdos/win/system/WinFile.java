package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.utils.FilePath;

import java.io.File;
import java.io.FileFilter;

public class WinFile extends WinObject {
    static public WinFile create(FilePath file, boolean write, int shareMode, int attributes) {
        if (file.open(write))
            return new WinFile(nextObjectId(), file, write, shareMode, attributes);
        return null;
    }

    static public WinFile createNoHandle(FilePath file, boolean write, int shareMode, int attributes) {
        if (file.open(write))
            return new WinFile(0, file, write, shareMode, attributes);
        return null;
    }

    static public WinFile get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinFile))
            return null;
        return (WinFile)object;
    }

    public final static int FILE_SHARE_NONE = 0x0;
    public final static int FILE_SHARE_READ = 0x1;
    public final static int FILE_SHARE_WRITE = 0x2;
    public final static int FILE_SHARE_DELETE = 0x4;

    public static class WildCardFileFilter implements FileFilter {
        String begin;
        String end;

        public WildCardFileFilter(String filter) {
            if (filter.contains("?")) {
                Win.panic("WildCardFileFilter to not support ? yet");
            }
            int pos = filter.indexOf("*");
            if (pos>=0) {
                begin = filter.substring(0, pos);
                end = filter.substring(pos+1);
            } else {
                begin = filter;
                end = "";
            }
            begin = begin.toLowerCase();
            end = end.toLowerCase();
        }

        public boolean accept(File pathname) {
            String name = pathname.getName().toLowerCase();
            if (name.startsWith(begin) && name.endsWith(end))
                return true;
            return false;
        }
    }
    public static final int STD_OUT = 1;
    public static final int STD_IN = 2;
    public static final int STD_ERROR = 3;

    public static final int FILE_TYPE_DISK = 0x0001; // The specified file is a disk file.
    public static final int FILE_TYPE_CHAR = 0x0002; // The specified file is a character file, typically an LPT device or a console.
    public static final int FILE_TYPE_PIPE = 0x0003; // The specified file is a socket, a named pipe, or an anonymous pipe.
    public static final int FILE_TYPE_REMOTE = 0x8000; // Unused.

    public static final int FILE_ATTRIBUTE_DIRECTORY = 0x10;
    public static final int FILE_ATTRIBUTE_NORMAL = 0x80;

    private static final long FILETIME_EPOCH_DIFF = 11644473600000L;

    /** One millisecond expressed in units of 100s of nanoseconds. */
    private static final long FILETIME_ONE_MILLISECOND = 10 * 1000;

    public static long filetimeToMillis(final long filetime) {
        return (filetime / FILETIME_ONE_MILLISECOND) - FILETIME_EPOCH_DIFF;
    }

    public static long millisToFiletime(final long millis) {
        return (millis + FILETIME_EPOCH_DIFF) * FILETIME_ONE_MILLISECOND;
    }

    public static void writeFileTime(int address, long time) {
        int low = (int)time;
        int high = (int)(time >> 32);
        Memory.mem_writed(address, low);
        Memory.mem_writed(address+4, high);
    }

    public static long readFileTime(int address) {
        return (Memory.mem_readd(address) & 0xFFFFFFFFl) | ((Memory.mem_readd(address+4)  & 0xFFFFFFFFl) << 32);
    }

    public WinFile(int type, int handle) {
        super(handle);
        this.type = type;
    }
    public WinFile(int handle, FilePath file, boolean write, int shareMode, int attributes) {
        super(handle);
        this.name = file.getName();
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
            if (from == SEEK_SET)
                file.seek(pos);
            else if (from == SEEK_CUR)
                file.skipBytes((int)pos);
            else if (from == SEEK_END)
                file.seek(file.length()-pos);
            else
                Win.panic("WinFile.seek unknown from: "+from);
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

    public int read(byte[] buffer) {
        try {
            return file.read(buffer);
        } catch (Exception e) {
            return 0;
        }
    }

    public int readb() {
        try {
            byte[] buffer = new byte[1];
            file.read(buffer);
            return buffer[0] & 0xFF;
        } catch (Exception e) {
            return -1;
        }
    }

    public int readw() {
        try {
            byte[] buffer = new byte[2];
            file.read(buffer);
            return (buffer[0] & 0xFF) | ((buffer[1] & 0xFF) << 8);
        } catch (Exception e) {
            return -1;
        }
    }

    public int readd() {
        try {
            byte[] buffer = new byte[4];
            file.read(buffer);
            return (buffer[0] & 0xFF) | ((buffer[1] & 0xFF) << 8) | ((buffer[2] & 0xFF) << 16) | ((buffer[3] & 0xFF) << 24);
        } catch (Exception e) {
            return -1;
        }
    }

    public int write(int buffer, int size) {
        try {
            byte[] buf = new byte[size];
            Memory.mem_memcpy(buf, 0, buffer, size);
            file.write(buf);
            return size;
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

    private FilePath file = null;
    public int shareMode;
    public int attributes;
    public int type;
}
