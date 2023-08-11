package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class Str extends WinAPI {
    // LPTSTR WINAPI CharUpper(LPTSTR lpsz)
    static public int CharUpperA(int lpsz) {
        String value = StringUtil.getString(lpsz);
        StringUtil.strcpy(lpsz, value.toUpperCase());
        return lpsz;
    }

    // DWORD WINAPI CharUpperBuff(LPTSTR lpsz, DWORD cchLength)
    static public int CharUpperBuffA(int lpsz, int cchLength) {
        String value = StringUtil.getString(lpsz, cchLength);
        byte[] b = value.getBytes();
        if (b.length<cchLength)
            cchLength = b.length;
        Memory.mem_memcpy(lpsz, b, 0, cchLength);
        return cchLength;
    }
}
