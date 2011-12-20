package jdos.win.loader.winpe;

import jdos.hardware.Memory;

import java.io.IOException;
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

    public void load(int address, RandomAccessFile fis) throws IOException {
        byte[] buffer = new byte[SIZE];
        fis.read(buffer);
        Memory.host_memcpy(address, buffer, 0, SIZE);
        LittleEndianFile is = new LittleEndianFile(address, SIZE);
        Machine = is.readUnsignedShort();
        NumberOfSections = is.readUnsignedShort();
        TimeDateStamp = is.readUnsignedInt();
        PointerToSymbolTable = is.readUnsignedInt();
        NumberOfSymbols = is.readUnsignedInt();
        SizeOfOptionalHeader = is.readUnsignedShort();
        Characteristics = is.readUnsignedShort();
    }
}
