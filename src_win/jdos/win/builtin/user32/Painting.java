package jdos.win.builtin.user32;

import jdos.gui.Main;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.GdiObj;
import jdos.win.builtin.gdi32.WinDC;
import jdos.win.builtin.gdi32.WinRegion;
import jdos.win.system.StaticData;
import jdos.win.system.WinRect;

import java.util.Iterator;

public class Painting extends WinAPI {
    // HDC WINAPI BeginPaint( HWND hwnd, PAINTSTRUCT *lps )
    static public int BeginPaint(int hwnd, int lps) {
        WinWindow win = WinWindow.get(hwnd);

        if (lps==0) return 0;

        Caret.HideCaret(hwnd);

        int rgn = WinRegion.CreateRectRgn(0, 0, win.rectWindow.width(), win.rectWindow.height());
        Message.SendMessageA(hwnd, WM_NCPAINT, rgn, 0 );
        GdiObj.DeleteObject(rgn);

        int hdc = win.getDC().handle;
        writed(lps, hdc);
        writed(lps+4, Message.SendMessageA(hwnd, WM_ERASEBKGND, hdc, 0));
        new WinRect(0, 0, win.rectClient.width(), win.rectClient.height()).write(lps+8);
        return readd(lps);
    }

    // BOOL EndPaint(HWND hWnd, const PAINTSTRUCT *lpPaint)
    static public int EndPaint(int hWnd, int lpPaint) {
        WinDC dc = WinDC.get(readd(lpPaint));
        if (dc != null)
            dc.close();
        Caret.ShowCaret(hWnd);
        Main.drawImage(StaticData.screen.getImage());
        return TRUE;
    }

    // HDC WINAPI GetDC(HWND hwnd)
    static public int GetDC(int hwnd) {
        WinWindow win;
        if (hwnd == 0)
            win = WinWindow.get(StaticData.desktopWindow);
        else
            win = WinWindow.get(hwnd);
        if (win == null)
            return 0;
        WinDC dc = win.getDC();
        dc.open();
        return dc.handle;
    }

    // BOOL InvalidateRect(HWND hWnd, const RECT *lpRect, BOOL bErase)
    static public int InvalidateRect(int hWnd, int lpRect, int bErase) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return FALSE;
        window.invalidate();
        return TRUE;
    }

    // int ReleaseDC(HWND hWnd, HDC hDC)
    static public int ReleaseDC(int hWnd, int hDC) {
        WinDC dc = WinDC.get(hDC);
        if (dc == null)
            return 0;
        dc.close();
        return 1;
    }

    // BOOL UpdateWindow(HWND hWnd)
    static public int UpdateWindow(int hWnd) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return FALSE;
        updateWindow(window);
        return TRUE;
    }

    // BOOL ValidateRect(HWND hWnd, const RECT *lpRect)
    static public int ValidateRect(int hWnd, int lpRect) {
        if (hWnd == 0)
            updateWindow(WinWindow.get(StaticData.desktopWindow));
        return TRUE;
    }

    static private void updateWindow(WinWindow window) {
        Message.SendMessageA(window.handle, WM_PAINT, 0, 0);
        Iterator<WinWindow> children = window.getChildren();
        while (children.hasNext()) {
            WinWindow child = children.next();
            updateWindow(child);
        }
    }
}
