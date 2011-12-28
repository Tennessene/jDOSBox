package jdos.win.loader.winpe;

import jdos.hardware.Memory;

import java.io.IOException;
import java.io.RandomAccessFile;

public class HeaderPE {
    public HeaderDOS dos = new HeaderDOS();
    public HeaderImageFile imageFile = new HeaderImageFile();
    public HeaderImageOptional imageOptional = new HeaderImageOptional();
    public HeaderImageSection[] imageSections = null;
    public int baseAddress;

    static public boolean fastCheckWinPE(String path) {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(path, "r");
            byte[] buffer = new byte[4];
            file.seek(HeaderDOS.SIZE - 4);
            file.read(buffer);
            int offset = (buffer[0] & 0xFF) | (buffer[1] & 0xFF) << 8 | (buffer[2] & 0xFF) << 16 | (buffer[3] & 0xFF) << 24;
            file.seek(offset);
            file.read(buffer);
            if (buffer[0]!=0x50 || buffer[1]!=0x45 || buffer[2]!=0 || buffer[3]!=0) {
                return false;
            }
            return true;
        } catch (Exception e) {
        } finally {
            if (file != null) {
                try {file.close();} catch (Exception e) {}
            }
        }
        return false;
    }

    public int load(int address, RandomAccessFile fis) throws IOException {
        baseAddress = address;
        dos.load(address, fis);
        byte[] buffer = new byte[(int)dos.e_lfanew - HeaderDOS.SIZE];
        fis.read(buffer);
        Memory.mem_memcpy(address + HeaderDOS.SIZE, buffer, 0, buffer.length);
        address+=dos.e_lfanew;
        buffer = new byte[4];
        fis.read(buffer);
        if (buffer[0]!=0x50 || buffer[1]!=0x45 || buffer[2]!=0 || buffer[3]!=0) {
            System.out.println("Not Windows EXE format");
            return 0;
        }
        Memory.mem_memcpy(address, buffer, 0, buffer.length);
        address+=4;
        imageFile.load(address, fis);
        if (imageFile.Machine != 0x14C) { // Intel 80386
            System.out.println("Not Windows 80386 EXE");
            return 0;
        }
        address+=HeaderImageFile.SIZE;
        imageOptional.load(address, fis);
        int offset=imageFile.SizeOfOptionalHeader-HeaderImageOptional.SIZE;
        if (offset>0)
            fis.skipBytes(offset);
        address+=imageFile.SizeOfOptionalHeader;
        imageSections = new HeaderImageSection[imageFile.NumberOfSections];
        for (int i=0;i<imageSections.length;i++) {
            imageSections[i] = new HeaderImageSection();
            imageSections[i].load(address, fis);
            address+=HeaderImageSection.SIZE;
        }
        return address;
    }
}
