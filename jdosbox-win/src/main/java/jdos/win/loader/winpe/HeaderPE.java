package jdos.win.loader.winpe;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinFile;

import java.io.IOException;
import java.io.OutputStream;

public class HeaderPE {
    public HeaderDOS dos = new HeaderDOS();
    public HeaderImageFile imageFile = new HeaderImageFile();
    public HeaderImageOptional imageOptional = new HeaderImageOptional();
    public HeaderImageSection[] imageSections = null;

    static public boolean fastCheckWinPE(WinFile file) {
        byte[] buffer = new byte[4];
        file.seek(HeaderDOS.SIZE - 4, WinAPI.SEEK_SET);
        file.read(buffer);
        int offset = (buffer[0] & 0xFF) | (buffer[1] & 0xFF) << 8 | (buffer[2] & 0xFF) << 16 | (buffer[3] & 0xFF) << 24;
        file.seek(offset, WinAPI.SEEK_SET);
        file.read(buffer);
        if (buffer[0]!=0x50 || buffer[1]!=0x45 || buffer[2]!=0 || buffer[3]!=0) {
            return false;
        }
        return true;
    }

    public boolean load(OutputStream os, WinFile fis) throws IOException {
        dos.load(os, fis);
        byte[] buffer = new byte[(int)dos.e_lfanew - HeaderDOS.SIZE];
        fis.read(buffer);
        os.write(buffer);
        buffer = new byte[4];
        fis.read(buffer);
        if (buffer[0]!=0x50 || buffer[1]!=0x45 || buffer[2]!=0 || buffer[3]!=0) {
            System.out.println("Not Windows EXE format");
            return false;
        }
        os.write(buffer);
        imageFile.load(os, fis);
        if (imageFile.Machine != 0x14C) { // Intel 80386
            System.out.println("Not Windows 80386 EXE");
            return false;
        }
        imageOptional.load(os, fis);
        int offset=imageFile.SizeOfOptionalHeader-HeaderImageOptional.SIZE;
        if (offset>0) {
            fis.seek(offset, WinAPI.SEEK_CUR);
            buffer = new byte[offset];
            os.write(buffer);
        }
        imageSections = new HeaderImageSection[imageFile.NumberOfSections];
        for (int i=0;i<imageSections.length;i++) {
            imageSections[i] = new HeaderImageSection();
            imageSections[i].load(os, fis);
        }
        return true;
    }
}
