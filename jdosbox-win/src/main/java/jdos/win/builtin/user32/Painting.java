package jdos.win.builtin.user32;

import jdos.gui.Main;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.system.StaticData;
import jdos.win.system.WinRect;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
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

        WinDC dc = win.getDC();
        if (win.invalidationRect == null) {
            new WinRect(0, 0, win.rectClient.width(), win.rectClient.height()).write(lps+8);
        } else {
            dc.clipX = win.invalidationRect.left;
            dc.clipY = win.invalidationRect.top;
            dc.clipCx = win.invalidationRect.width();
            dc.clipCy = win.invalidationRect.height();
            win.invalidationRect.write(lps+8);
        }
        int hdc = dc.handle;
        writed(lps, hdc);
        writed(lps+4, Message.SendMessageA(hwnd, WM_ERASEBKGND, hdc, 0));
        return readd(lps);
    }

    // BOOL Ellipse(HDC hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect)
    static public int Ellipse(int hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;

        if (nLeftRect == nRightRect || nTopRect == nBottomRect)
            return TRUE;
        WinPen pen = WinPen.get(dc.hPen);
        WinBrush brush = WinBrush.get(dc.hBrush);

        if (pen == null || brush == null)
            return FALSE;

        Graphics2D graphics = dc.getGraphics();
        Ellipse2D ellipse2D = new Ellipse2D.Float(dc.x+nLeftRect, dc.y+nTopRect, nRightRect-nLeftRect, nBottomRect-nTopRect);
        // inside
        if (brush.setPaint(graphics))
            graphics.fill(ellipse2D);
        // border
        if (pen.setStroke(dc, graphics))
            graphics.draw(ellipse2D);

        graphics.dispose();
        return TRUE;
    }

    // BOOL EndPaint(HWND hWnd, const PAINTSTRUCT *lpPaint)
    static public int EndPaint(int hWnd, int lpPaint) {
        WinDC dc = WinDC.get(readd(lpPaint));
        if (dc != null)
            dc.close();
        Caret.ShowCaret(hWnd);
        Main.drawImage(StaticData.screen.getImage());
        WinWindow.get(hWnd).validate();
        return TRUE;
    }

    // HDC WINAPI GetDC(HWND hwnd)
    static public int GetDC(int hwnd) {
        WinWindow win;
        if (hwnd == 0) {
            win = WinWindow.get(StaticData.desktopWindow);
        } else
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
            return FALSE; // :TODO: invalidate all windows
        WinRect rect = null;
        if (lpRect != 0) {
            rect = new WinRect(lpRect);
        }
        window.invalidate(rect);
        return TRUE;
    }

    // BOOL Pie(HDC hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect, int nXRadial1, int nYRadial1, int nXRadial2, int nYRadial2)
    static public int Pie(int hdc, int nLeftRect, int nTopRect, int nRightRect, int nBottomRect, int xstart, int ystart, int xend, int yend) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;

        if (nLeftRect == nRightRect || nTopRect == nBottomRect)
            return TRUE;
        WinPen pen = WinPen.get(dc.hPen);
        WinBrush brush = WinBrush.get(dc.hBrush);

        if (pen == null || brush == null)
            return FALSE;

        Graphics2D graphics = dc.getGraphics();
        int width = nRightRect-nLeftRect;
        int height = nBottomRect-nTopRect;

        int start = (int)(Math.atan2(height/2.0 - ystart, xstart-width/2.0)*180/Math.PI + 360) % 360;
        int end = (int)(Math.atan2(height/2.0 - yend, xend - width/2.0)*180/Math.PI + 360) % 360;
        int len;
        if (end<start)
            len = 360 - start + end;
        else
            len = end - start;
        Arc2D arc2D = new Arc2D.Double(dc.x+nLeftRect, dc.y+nTopRect, width, height,  start, len, Arc2D.PIE);
        // inside
        if (brush.setPaint(graphics))
            graphics.fill(arc2D);
        // border
        if (pen.setStroke(dc, graphics))
            graphics.draw(arc2D);

        graphics.dispose();

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
        if (dc.getImage() == StaticData.screen.getImage())
            Main.drawImage(dc.getImage());
        return TRUE;
    }

    // int ReleaseDC(HWND hWnd, HDC hDC)
    static public int ReleaseDC(int hWnd, int hDC) {
        WinDC dc = WinDC.get(hDC);
        if (dc == null)
            return 0;
        if (dc.isScreen()) {
            Main.drawImage(dc.getImage());
        }
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
