package jdos.win.builtin.user32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

public class Property extends WinAPI {
    // HANDLE WINAPI GetProp(HWND hWnd, LPCTSTR lpString)
    static public int GetPropA(int hWnd, int lpString) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        if (IS_INTRESOURCE(lpString))
            Win.panic("GetPropA does not support atoms yet");
        String name = StringUtil.getString(lpString);
        Integer value = window.props.get(name);
        if (value != null)
            return value;
        return 0;
    }

    // HANDLE WINAPI RemoveProp(HWND hWnd, LPCTSTR lpString)
    static public int RemovePropA(int hWnd, int lpString) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        if (IS_INTRESOURCE(lpString))
            Win.panic("RemoveProp with atom not supported yet");
        String name = StringUtil.getString(lpString);
        Integer result = window.props.remove(name);
        if (result == null)
            return 0;
        return result;
    }

    //BOOL WINAPI SetProp(HWND hWnd, LPCTSTR lpString, HANDLE hData)
    static public int SetPropA(int hWnd, int lpString, int hData) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return FALSE;
        if (IS_INTRESOURCE(lpString))
            Win.panic("SetProp with atom not supported yet");
        String name = StringUtil.getString(lpString);
        window.props.put(name, hData);
        return TRUE;
    }
}
