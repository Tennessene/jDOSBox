package jdos.win.loader.winpe;

import jdos.hardware.Memory;

import java.io.IOException;
import java.io.RandomAccessFile;

public class HeaderImageSection {
    public static final int SIZE=40;

    public static final int IMAGE_SIZEOF_SHORT_NAME = 8;

    public byte[] Name = new byte[IMAGE_SIZEOF_SHORT_NAME];
    public long PhysicalAddress_or_VirtualSize;
    public long VirtualAddress;
    public long SizeOfRawData;
    public long PointerToRawData;
    public long PointerToRelocations;
    public long PointerToLinenumbers;
    public int  NumberOfRelocations;
    public int  NumberOfLinenumbers;
    public long Characteristics;

    public void load(int address, RandomAccessFile fis) throws IOException {
        byte[] buffer = new byte[SIZE];
        fis.read(buffer);
        Memory.host_memcpy(address, buffer, 0, SIZE);
        LittleEndianFile is = new LittleEndianFile(address, SIZE);
        is.read(Name);
        PhysicalAddress_or_VirtualSize = is.readUnsignedInt();
        VirtualAddress = is.readUnsignedInt();
        SizeOfRawData = is.readUnsignedInt();
        PointerToRawData = is.readUnsignedInt();
        PointerToRelocations = is.readUnsignedInt();
        PointerToLinenumbers = is.readUnsignedInt();
        NumberOfRelocations = is.readUnsignedShort();
        NumberOfLinenumbers = is.readUnsignedShort();
        Characteristics = is.readUnsignedInt();
    }
}
