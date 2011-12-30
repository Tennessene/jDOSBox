package jdos.win.loader.winpe;

import jdos.win.utils.LittleEndian;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

// From Wine project
public class HeaderImageFile {
    final static public int SIZE=20;

    public int  Machine;
    public int  NumberOfSections;
    public long TimeDateStamp;
    public long PointerToSymbolTable;
    public long NumberOfSymbols;
    public int  SizeOfOptionalHeader;
    public int  Characteristics;

    public void load(OutputStream os, RandomAccessFile fis) throws IOException {
        byte[] buffer = new byte[SIZE];
        fis.read(buffer);
        os.write(buffer);
        LittleEndian is = new LittleEndian(buffer);
        Machine = is.readUnsignedShort();
        NumberOfSections = is.readUnsignedShort();
        TimeDateStamp = is.readUnsignedInt();
        PointerToSymbolTable = is.readUnsignedInt();
        NumberOfSymbols = is.readUnsignedInt();
        SizeOfOptionalHeader = is.readUnsignedShort();
        Characteristics = is.readUnsignedShort();
    }
}
