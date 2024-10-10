package jdos.win.loader.winpe;

import jdos.win.system.WinFile;
import jdos.win.utils.LittleEndian;

import java.io.IOException;
import java.io.OutputStream;

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

    public void load(OutputStream os, WinFile fis) throws IOException {
        byte[] buffer = new byte[SIZE];
        fis.read(buffer);
        os.write(buffer);
        LittleEndian is = new LittleEndian(buffer);
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
