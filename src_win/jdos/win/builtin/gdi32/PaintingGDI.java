package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinPoint;

import java.awt.*;

public class PaintingGDI extends WinAPI {
    // BOOL LineTo(HDC hdc, int nXEnd, int nYEnd)
    static public int LineTo(int hdc, int nXEnd, int nYEnd) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return FALSE;
        }

        Graphics g = dc.getGraphics();
        WinPen pen = WinPen.get(dc.hPen);
        if ((pen.style & PS_STYLE_MASK) != PS_SOLID)
            warn("LineTo only solid pens are supported");
        if (pen.width != 1)
            warn("LineTo only 1 pixel wide pens are supported");
        g.setColor(new Color(0xFF000000|pen.color));
        g.drawLine(dc.x+dc.CursPosX, dc.y+dc.CursPosY, dc.x+nXEnd, dc.y+nYEnd);
        dc.CursPosX = nXEnd;
        dc.CursPosY = nYEnd;
        g.dispose();
        return TRUE;
    }

    // BOOL MoveToEx(HDC hdc, int X, int Y, LPPOINT lpPoint)
    static public int MoveToEx(int hdc, int X, int Y, int lpPoint) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return FALSE;
        }
        if (lpPoint!=0) {
            WinPoint p = new WinPoint(dc.CursPosX, dc.CursPosY);
            p.write(lpPoint);
        }
        dc.CursPosX = X;
        dc.CursPosY = Y;
        return TRUE;
    }

    // BOOL Polygon(HDC hdc, const POINT* lpPoints, int nCount)
    static public int Polygon(int hdc, int lpPoints, int nCount) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return FALSE;
        }

        Graphics g = dc.getGraphics();
        WinBrush brush = WinBrush.get(dc.hBrush);
        if (brush.style!=BS_SOLID)
            warn("Polygon only supports solid brushes");
        g.setColor(new Color(0xFF000000|brush.color));

        int[] x = new int[nCount];
        int[] y = new int[nCount];
        for (int i=0;i<nCount;i++) {
            x[i] = readd(lpPoints+i*8);
            y[i] = readd(lpPoints+i*12);
        }
        g.fillPolygon(x, y, nCount);

        WinPen pen = WinPen.get(dc.hPen);
        if ((pen.style & PS_STYLE_MASK) != PS_SOLID)
            warn("Polygon only solid pens are supported");
        if (pen.width != 1)
            warn("Polygon only 1 pixel wide pens are supported");
        g.setColor(new Color(0xFF000000|pen.color));
        g.drawPolygon(x, y, nCount);
        g.dispose();
        return TRUE;
    }
}
