package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class Caret extends WinAPI {
    // BOOL WINAPI DestroyCaret(void);
    static public int DestroyCaret() {
        return TRUE;
    }

    // BOOL WINAPI HideCaret( HWND hwnd )
    static public int HideCaret(int hwnd) {
        return TRUE;
    }

    // BOOL WINAPI ShowCaret(HWND hWnd)
    static public int ShowCaret(int hWnd) {
        return TRUE;
    }
}
