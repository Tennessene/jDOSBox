package jdos.win.builtin.user32;

public class Clipboard {
    // HWND WINAPI GetClipboardOwner(void);
    static public int GetClipboardOwner() {
        return 0;
    }

    static public boolean CLIPBOARD_ReleaseOwner() {
        return true;
    }
}
