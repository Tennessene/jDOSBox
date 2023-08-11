package jdos.win.builtin.user32;

import jdos.win.system.WinRect;

public class GuiThreadInfo {
    static final int SIZE = 48;
    int         flags;
    int         hwndActive;
    int         hwndFocus;
    int         hwndCapture;
    int         hwndMenuOwner;
    int         hwndMoveSize;
    int         hwndCaret;
    WinRect rcCaret;
}
