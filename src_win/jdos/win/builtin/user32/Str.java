package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class Str extends WinAPI {
    // LPTSTR WINAPI CharUpper(LPTSTR lpsz)
    static public int CharUpperA(int lpsz) {
        String value = StringUtil.getString(lpsz);
        StringUtil.strcpy(lpsz, value.toUpperCase());
        return lpsz;
    }
}
