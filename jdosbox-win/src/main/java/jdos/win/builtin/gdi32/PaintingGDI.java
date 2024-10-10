package jdos.win.builtin.gdi32;

import jdos.gui.Main;
import jdos.win.builtin.WinAPI;
import jdos.win.system.StaticData;
import jdos.win.system.WinPoint;

import java.awt.*;
import java.awt.geom.Line2D;

public class PaintingGDI extends WinAPI {
    // BOOL LineTo(HDC hdc, int nXEnd, int nYEnd)
    static public int LineTo(int hdc, int nXEnd, int nYEnd) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null) {
            return FALSE;
        }

        Graphics2D g = dc.getGraphics();
        WinPen pen = WinPen.get(dc.hPen);
        if (pen.setStroke(dc, g))
            g.draw(new Line2D.Float(dc.x + dc.CursPosX, dc.y + dc.CursPosY, dc.x + nXEnd, dc.y + nYEnd));
        dc.CursPosX = nXEnd;
        dc.CursPosY = nYEnd;
        g.dispose();
        if (dc.getImage() == StaticData.screen.getImage())
            Main.drawImage(dc.getImage());
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
        WinBrush brush = WinBrush.get(dc.hBrush);
        WinPen pen = WinPen.get(dc.hPen);

        if (dc == null || brush == null || pen == null) {
            return FALSE;
        }

        int[] x = new int[nCount];
        int[] y = new int[nCount];
        for (int i=0;i<nCount;i++) {
            x[i] = readd(lpPoints+i*8);
            y[i] = readd(lpPoints+i*12);
        }
        Polygon polygon = new Polygon(x, y, nCount);

        Graphics2D g = dc.getGraphics();

        // inside
        if (brush.setPaint(g))
            g.fill(polygon);

        // border
        if (pen.setStroke(dc, g))
            g.draw(polygon);

        g.dispose();
        if (dc.getImage() == StaticData.screen.getImage())
            Main.drawImage(dc.getImage());
        return TRUE;
    }
}
