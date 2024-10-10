package jdos.win.loader.winpe;

import jdos.win.system.WinFile;
import jdos.win.utils.LittleEndian;

import java.io.IOException;
import java.io.OutputStream;

// From Wine project
public class HeaderImageOptional {
    public final static int SIZE = 0xE0;
         
    public final static int IMAGE_DIRECTORY_ENTRY_EXPORT = 0;
    public final static int IMAGE_DIRECTORY_ENTRY_IMPORT = 1;
    public final static int IMAGE_DIRECTORY_ENTRY_RESOURCE = 2;
    public final static int IMAGE_DIRECTORY_ENTRY_EXCEPTION = 3;
    public final static int IMAGE_DIRECTORY_ENTRY_SECURITY = 4;
    public final static int IMAGE_DIRECTORY_ENTRY_BASERELOC = 5;
    public final static int IMAGE_DIRECTORY_ENTRY_DEBUG = 6;
    public final static int IMAGE_DIRECTORY_ENTRY_COPYRIGHT = 7;
    public final static int IMAGE_DIRECTORY_ENTRY_GLOBALPTR = 8;   /* (MIPS GP) */
    public final static int IMAGE_DIRECTORY_ENTRY_TLS = 9;
    public final static int IMAGE_DIRECTORY_ENTRY_LOAD_CONFIG = 10;
    public final static int IMAGE_DIRECTORY_ENTRY_BOUND_IMPORT = 11;
    public final static int IMAGE_DIRECTORY_ENTRY_IAT = 12;  /* Import Address Table */
    public final static int IMAGE_DIRECTORY_ENTRY_DELAY_IMPORT = 13;
    public final static int IMAGE_DIRECTORY_ENTRY_COM_DESCRIPTOR = 14;
    
    public static class ImageDataDirectory {
        public long VirtualAddress;
        public long Size;
    }

    /* Standard fields */
    public int Magic; /* 0x10b or 0x107 */    /* 0x00 */
    public short MajorLinkerVersion;
    public short MinorLinkerVersion;
    public long SizeOfCode;
    public long SizeOfInitializedData;
    public long SizeOfUninitializedData;
    public long AddressOfEntryPoint;            /* 0x10 */
    public long BaseOfCode;
    public long BaseOfData;

    /* NT additional fields */
    public long ImageBase;
    public long SectionAlignment;                /* 0x20 */
    public long FileAlignment;
    public int MajorOperatingSystemVersion;
    public int MinorOperatingSystemVersion;
    public int MajorImageVersion;
    public int MinorImageVersion;
    public int MajorSubsystemVersion;        /* 0x30 */
    public int MinorSubsystemVersion;
    public long Win32VersionValue;
    public long SizeOfImage;
    public long SizeOfHeaders;
    public long CheckSum;                        /* 0x40 */
    public int Subsystem;
    public int DllCharacteristics;
    public long SizeOfStackReserve;
    public long SizeOfStackCommit;
    public long SizeOfHeapReserve;            /* 0x50 */
    public long SizeOfHeapCommit;
    public long LoaderFlags;
    public long NumberOfRvaAndSizes;
    public ImageDataDirectory[] DataDirectory = new ImageDataDirectory[16]; /* 0x60 */
    /* 0xE0 */

    public void load(OutputStream os, WinFile fis) throws IOException {
        byte[] buffer = new byte[SIZE];
        fis.read(buffer);
        os.write(buffer);
        LittleEndian is = new LittleEndian(buffer);
        Magic = is.readUnsignedShort();
        MajorLinkerVersion = is.readUnsignedByte();
        MinorLinkerVersion = is.readUnsignedByte();
        SizeOfCode = is.readUnsignedInt();
        SizeOfInitializedData = is.readUnsignedInt();
        SizeOfUninitializedData = is.readUnsignedInt();
        AddressOfEntryPoint = is.readUnsignedInt();
        BaseOfCode = is.readUnsignedInt();
        BaseOfData = is.readUnsignedInt();
        ImageBase = is.readUnsignedInt();
        SectionAlignment = is.readUnsignedInt();
        FileAlignment = is.readUnsignedInt();
        MajorOperatingSystemVersion = is.readUnsignedShort();
        MinorOperatingSystemVersion = is.readUnsignedShort();
        MajorImageVersion = is.readUnsignedShort();
        MinorImageVersion = is.readUnsignedShort();
        MajorSubsystemVersion = is.readUnsignedShort();
        MinorSubsystemVersion = is.readUnsignedShort();
        Win32VersionValue = is.readUnsignedInt();
        SizeOfImage = is.readUnsignedInt();
        SizeOfHeaders = is.readUnsignedInt();
        CheckSum = is.readUnsignedInt();
        Subsystem = is.readUnsignedShort();
        DllCharacteristics = is.readUnsignedShort();
        SizeOfStackReserve = is.readUnsignedInt();
        SizeOfStackCommit = is.readUnsignedInt();
        SizeOfHeapReserve = is.readUnsignedInt();
        SizeOfHeapCommit = is.readUnsignedInt();
        LoaderFlags = is.readUnsignedInt();
        NumberOfRvaAndSizes = is.readUnsignedInt();
        for (int i=0;i<DataDirectory.length;i++) {
            DataDirectory[i] = new ImageDataDirectory();
            DataDirectory[i].VirtualAddress = is.readUnsignedInt();
            DataDirectory[i].Size = is.readUnsignedInt();
        }
    }
}
