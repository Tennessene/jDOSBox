package jdos.win.builtin.winmm;

import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class WAVEOUTCAPS extends WinAPI {
    public final static int SIZE = 52;
    public WAVEOUTCAPS() {
    }

    public void write(int address) {
        writew(address, wMid); address+=2;
        writew(address, wPid); address+=2;
        writew(address, vDriverVersion); address+=4;
        StringUtil.strncpy(address, szPname, 32); address+=32;
        writew(address, dwFormats); address+=4;
        writew(address, wChannels); address+=2;
        writew(address, wReserved1); address+=2;
        writew(address, dwSupport); address+=4;
    }

    public int allocTemp() {
        int p = getTempBuffer(SIZE);
        write(p);
        return p;
    }

    public int wMid;
    public int wPid;
    public int vDriverVersion;
    public String szPname="";
    public int dwFormats;
    public int wChannels;
    public int wReserved1;
    public int dwSupport;
}
