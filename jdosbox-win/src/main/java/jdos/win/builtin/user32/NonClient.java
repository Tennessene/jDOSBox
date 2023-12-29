package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class NonClient extends WinAPI {
    // BOOL WINAPI AdjustWindowRectEx(LPRECT lpRect, DWORD dwStyle, BOOL bMenu, DWORD dwExStyle)
    static public void AdjustWindowRectEx(int lpRect, int dwStyle, int bMenu, int dwExStyle) {
        log("Faked");
    }

    static public void NC_HandleNCPaint(int hwnd , int clip) {
    }

    static public void NC_HandleSetCursor(int hwnd, int wParam, int lParam) {
        switch((short)LOWORD(lParam))
        {
        case HTERROR:
            {
                int msg = HIWORD( lParam );
                if ((msg == WM_LBUTTONDOWN) || (msg == WM_MBUTTONDOWN) || (msg == WM_RBUTTONDOWN))
                    Message.MessageBeep(0);
            }
            break;
        case HTCLIENT:
            {
                int hCursor = WinClass.GetClassLongA(hwnd, GCL_HCURSOR);
                if (hCursor!=0) {
                    WinCursor.SetCursor(hCursor);
                    return;
                }
                return;
            }

        case HTLEFT:
        case HTRIGHT:
            WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZEWE));
            return;

        case HTTOP:
        case HTBOTTOM:
            WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZENS));
            return;

        case HTTOPLEFT:
        case HTBOTTOMRIGHT:
            WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZENWSE));
            return;

        case HTTOPRIGHT:
        case HTBOTTOMLEFT:
            WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZENESW));
            return;
        }

        /* Default cursor: arrow */
        WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_ARROW));
    }
}
