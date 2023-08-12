package jdos.win.builtin.winmm;

import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class Mci extends WinAPI {
    // MCIERROR mciSendString(LPCTSTR lpszCommand, LPTSTR lpszReturnString, UINT cchReturn, HANDLE hwndCallback)
    static public int mciSendStringA(int lpszCommand, int lpszReturnString, int cchReturn, int hwndCallback) {
        String command = StringUtil.getString(lpszCommand);
        if (command.equalsIgnoreCase("open avivideo"))
            return 0;
        if (command.equalsIgnoreCase("close avivideo"))
            return 0;
        log("mciSendStringA "+command+" not supported yet");
        return 1;
    }
}
