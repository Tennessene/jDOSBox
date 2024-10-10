package jdos.win.loader.winpe;

import jdos.win.system.WinFile;
import jdos.win.utils.LittleEndian;

import java.io.IOException;
import java.io.OutputStream;

// From Wine project
public class HeaderDOS {
    public static final int SIZE=64;

    int  e_magic;               /* 00: MZ Header signature */
    int  e_cblp;                /* 02: Bytes on last page of file */
    int  e_cp;                  /* 04: Pages in file */
    int  e_crlc;                /* 06: Relocations */
    int  e_cparhdr;             /* 08: Size of header in paragraphs */
    int  e_minalloc;            /* 0a: Minimum extra paragraphs needed */
    int  e_maxalloc;            /* 0c: Maximum extra paragraphs needed */
    int  e_ss;                  /* 0e: Initial (relative) SS value */
    int  e_sp;                  /* 10: Initial SP value */
    int  e_csum;                /* 12: Checksum */
    int  e_ip;                  /* 14: Initial IP value */
    int  e_cs;                  /* 16: Initial (relative) CS value */
    int  e_lfarlc;              /* 18: File address of relocation table */
    int  e_ovno;                /* 1a: Overlay number */
    int[]  e_res = new int[4];  /* 1c: Reserved words */
    int  e_oemid;               /* 24: OEM identifier (for e_oeminfo) */
    int  e_oeminfo;             /* 26: OEM information; e_oemid specific */
    int[]  e_res2 = new int[10];/* 28: Reserved words */
    long e_lfanew;              /* 3c: Offset to extended header */

    public void load(OutputStream os, WinFile fis) throws IOException {
        byte[] buffer = new byte[SIZE];
        fis.read(buffer);
        os.write(buffer);
        LittleEndian is = new LittleEndian(buffer);
        e_magic = is.readUnsignedShort();
        e_cblp = is.readUnsignedShort();
        e_cp = is.readUnsignedShort();
        e_crlc = is.readUnsignedShort();
        e_cparhdr = is.readUnsignedShort();
        e_minalloc = is.readUnsignedShort();
        e_maxalloc = is.readUnsignedShort();
        e_ss = is.readUnsignedShort();
        e_sp = is.readUnsignedShort();
        e_csum = is.readUnsignedShort();
        e_ip = is.readUnsignedShort();
        e_cs = is.readUnsignedShort();
        e_lfarlc = is.readUnsignedShort();
        e_ovno = is.readUnsignedShort();
        for (int i=0;i<e_res.length;i++)
            e_res[i] = is.readUnsignedShort();
        e_oemid = is.readUnsignedShort();
        e_oeminfo = is.readUnsignedShort();
        for (int i=0;i<e_res2.length;i++)
            e_res2[i] = is.readUnsignedShort();
        e_lfanew = is.readUnsignedInt();
    }
}
