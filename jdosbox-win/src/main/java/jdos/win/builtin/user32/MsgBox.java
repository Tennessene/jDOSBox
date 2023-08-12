package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.utils.StringUtil;

import javax.swing.*;

public class MsgBox extends WinAPI {
    // int WINAPI MessageBox(HWND hWnd, LPCTSTR lpText, LPCTSTR lpCaption, UINT uType)
    static public int MessageBoxA(int hWnd, int lpText, int lpCaption, int uType) {
        String text = StringUtil.getString(lpText);
        String caption = "";
        if (lpCaption != 0)
            caption = StringUtil.getString(lpCaption);
        int type = JOptionPane.INFORMATION_MESSAGE;
        if ((uType & 0x00000040)!=0) // MB_ICONINFORMATION
            type = JOptionPane.INFORMATION_MESSAGE;
        else if ((uType & 0x00000030)!=0) // MB_ICONWARNING
            type = JOptionPane.WARNING_MESSAGE;
        else if ((uType & 0x00000020)!=0) // MB_ICONQUESTION
            type = JOptionPane.QUESTION_MESSAGE;
        else if ((uType & 0x00000010)!=0) // MB_ICONERROR
            type = JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(null, text, caption, type);
        return IDOK;
    }
}
