package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

public class NonClient extends WinAPI {
    // BOOL WINAPI AdjustWindowRectEx(LPRECT lpRect, DWORD dwStyle, BOOL bMenu, DWORD dwExStyle)
    static public int AdjustWindowRectEx(int lpRect, int dwStyle, int bMenu, int dwExStyle) {
        log("Faked");
        return TRUE;
    }

    static public int NC_HandleNCPaint(int hwnd , int clip) {
        return 0;
    }

    static public int NC_HandleSetCursor(int hwnd, int wParam, int lParam) {
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
                    return TRUE;
                }
                return FALSE;
            }

        case HTLEFT:
        case HTRIGHT:
            return WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZEWE));

        case HTTOP:
        case HTBOTTOM:
            return WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZENS));

        case HTTOPLEFT:
        case HTBOTTOMRIGHT:
            return WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZENWSE));

        case HTTOPRIGHT:
        case HTBOTTOMLEFT:
            return WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_SIZENESW));
        }

        /* Default cursor: arrow */
        return WinCursor.SetCursor(WinCursor.LoadCursorA(0, IDC_ARROW));
    }
}
