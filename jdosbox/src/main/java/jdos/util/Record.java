package jdos.util;

import jdos.cpu.CPU_Regs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Record {
    public static DataOutputStream dos;
    public static DataInputStream ios;

    static {
        try {
            //dos = new DataOutputStream(new FileOutputStream("good.bin"));
            ios = new DataInputStream(new FileInputStream("good.bin"));
        } catch (Exception e) {

        }
    }

    static public void op(int c) {
        if (c==0x0f || c==0x20f || c==0x26 || c==0x226 || c==0x2e || c==0x22e || c==0x36 || c==0x236 || c==0x3e || c==0x23e || c==0x64 || c==0x264 || c==0x65 || c==0x265 || c==0x66 || c==0x266 || c==0x67 || c==0x267 || c==0xf0 || c==0x2f0 || c==0xf2 || c==0xf3 || c==0x2f2 || c==0x2f3)
            return;
        if (ios!=null) {
            try {
                int tmp=ios.readInt();
                if (tmp!=c) {
                    int ii=0;
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_csPhys.dword) {
                    int ii=0;
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_eip) {
                    int ii=0;
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_eax.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_ecx.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_edx.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_ebx.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_esp.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_ebp.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_esi.dword) {
                    throw new Exception();
                }
                tmp=ios.readInt();
                if (tmp!=CPU_Regs.reg_edi.dword) {
                    throw new Exception();
                }
            } catch (Exception e) {

            }
        }
        if (dos!=null) {
            try {
                dos.writeInt(c);
                dos.writeInt(CPU_Regs.reg_csPhys.dword);
                dos.writeInt(CPU_Regs.reg_eip);
                dos.writeInt(CPU_Regs.reg_eax.dword);
                dos.writeInt(CPU_Regs.reg_ecx.dword);
                dos.writeInt(CPU_Regs.reg_edx.dword);
                dos.writeInt(CPU_Regs.reg_ebx.dword);
                dos.writeInt(CPU_Regs.reg_esp.dword);
                dos.writeInt(CPU_Regs.reg_ebp.dword);
                dos.writeInt(CPU_Regs.reg_esi.dword);
                dos.writeInt(CPU_Regs.reg_edi.dword);
            } catch (Exception e) {

            }
        }
    }
}
