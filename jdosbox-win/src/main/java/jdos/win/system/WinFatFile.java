package jdos.win.system;

import jdos.dos.Dos_files;
import jdos.dos.drives.Drive_fat;
import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.win.Win;


public class WinFatFile extends WinFile {
    static public WinFile create(String name, Drive_fat.fatFile file, int shareMode, int attributes) {
        return new WinFatFile(nextObjectId(), name, file, shareMode, attributes);
    }

    public WinFatFile(int handle, String name, Drive_fat.fatFile file, int shareMode, int attributes) {
        super(FILE_TYPE_DISK, handle);
        this.fatFile = file;
        this.name = name;
        this.shareMode = shareMode;
        this.attributes = attributes;
    }

    public long size() {
        if (fatFile == null) {
            return 0;
        }
        return fatFile.filelength;
    }

    public long seek(long pos, int from) {
        if (fatFile == null)
            return -1;

        LongRef pPos = new LongRef(pos);
        if (from == SEEK_SET)
            fatFile.Seek(pPos, Dos_files.DOS_SEEK_SET);
        else if (from == SEEK_CUR)
            fatFile.Seek(pPos, Dos_files.DOS_SEEK_CUR);
        else if (from == SEEK_END)
            fatFile.Seek(pPos, Dos_files.DOS_SEEK_END);
        else
            Win.panic("WinFile.seek unknown from: " + from);
        return pPos.value;
    }

    public int read(int buffer, int size) {
        byte[] buf = new byte[size];
        IntRef pSize = new IntRef(size);
        if (!fatFile.Read(buf, pSize))
            return 0;
        Memory.mem_memcpy(buffer, buf, 0, pSize.value);
        return pSize.value;
    }

    public int read(byte[] buffer) {
        IntRef pSize = new IntRef(buffer.length);
        if (!fatFile.Read(buffer, pSize))
            return 0;
        return pSize.value;
    }

    public int write(int buffer, int size) {
        byte[] buf = new byte[size];
        Memory.mem_memcpy(buf, 0, buffer, size);
        IntRef pSize = new IntRef(size);
        if (!fatFile.Write(buf, pSize))
            return 0;
        return pSize.value;
    }

    protected void onFree() {
        fatFile.Close();
    }

    private Drive_fat.fatFile fatFile = null;
}
