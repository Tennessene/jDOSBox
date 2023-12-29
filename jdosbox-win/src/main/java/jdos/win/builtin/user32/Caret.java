package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class Caret extends WinAPI {
    // BOOL WINAPI DestroyCaret(void);
    static public void DestroyCaret() {
    }

    // BOOL WINAPI HideCaret( HWND hwnd )
    static public void HideCaret(int hwnd) {
    }

    // BOOL WINAPI ShowCaret(HWND hWnd)
    static public void ShowCaret(int hWnd) {
    }
}
