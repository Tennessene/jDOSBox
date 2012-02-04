package jdos.win.builtin.user32;

import jdos.gui.Main;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.system.StaticData;
import jdos.win.system.WinRect;

import java.awt.*;
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

    // int GetUpdateRgn(HWND hWnd, HRGN hRgn, BOOL bErase)
    static public int GetUpdateRgn(int hWnd, int hRgn, int bErase) {
        WinWindow win = WinWindow.get(hWnd);
        WinRegion rgn = WinRegion.get(hRgn);
        if (win == null || rgn == null)
            return ERROR;
        if (win.needsPainting()) {
            if (bErase != 0) {
                rgn.rects.clear();
                rgn.rects.add(new WinRect(0, 0, win.rectWindow.width(), win.rectWindow.height()));
                Message.SendMessageA(hWnd, WM_NCPAINT, hRgn, 0 );
                int hdc = win.getDC().handle;
                Message.SendMessageA(hWnd, WM_ERASEBKGND, hdc, 0);
                ReleaseDC(hWnd, hdc);
            }
            rgn.rects.clear();
            rgn.rects.add(new WinRect(0, 0, win.rectClient.width(), win.rectClient.height()));
            return SIMPLEREGION;
        }
        return NULLREGION;
    }

    // BOOL InvalidateRect(HWND hWnd, const RECT *lpRect, BOOL bErase)
    static public int InvalidateRect(int hWnd, int lpRect, int bErase) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return FALSE;
        window.invalidate();
        return TRUE;
    }

    // BOOL Rectangle(HDC hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect)
    static public int Rectangle(int hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;
        if (nLeftRect == nRightRect || nTopRect == nBottomRect)
            return TRUE;
        WinPen pen = WinPen.get(dc.hPen);
        WinBrush brush = WinBrush.get(dc.hBrush);

        if (pen == null || brush == null)
            return FALSE;

        int width = nRightRect-nLeftRect-1;
        int height = nBottomRect-nTopRect-1;

        Graphics2D graphics = dc.getGraphics();
        Rectangle rectangle = new Rectangle(dc.x+nLeftRect, dc.y+nTopRect, width, height);
        // inside
        if (brush.setPaint(graphics))
            graphics.fill(rectangle);
        // border
        if (pen.setStroke(dc, graphics))
            graphics.draw(rectangle);

        graphics.dispose();
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

    // BOOL RedrawWindow(HWND hWnd, const RECT *lprcUpdate, HRGN hrgnUpdate, UINT flags)
    static public int RedrawWindow(int hWnd, int lprcUpdate, int hrgnUpdate, int flags) {
        log("RedrawWindow faked");
        if (hWnd == 0)
            hWnd = WinWindow.GetDesktopWindow();
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return FALSE;
        return TRUE;
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
