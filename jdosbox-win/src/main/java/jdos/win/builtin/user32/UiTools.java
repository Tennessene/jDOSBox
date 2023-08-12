package jdos.win.builtin.user32;

import jdos.cpu.CPU_Regs;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.system.*;
import jdos.win.utils.Ptr;
import jdos.win.utils.StringUtil;

public class UiTools extends WinAPI {
    // BOOL CopyRect(LPRECT lprcDst, const RECT *lprcSrc)
    static public int CopyRect(int lprcDst, int lprcSrc) {
        if (lprcDst == 0 || lprcSrc == 0)
            return FALSE;
        Memory.mem_memcpy(lprcDst, lprcSrc, WinRect.SIZE);
        return TRUE;
    }

    // BOOL DrawEdge(HDC hdc, LPRECT qrc, UINT edge, UINT grfFlags)
    static public int DrawEdge(int hdc, int qrc, int edge, int grfFlags) {
        if ((grfFlags & BF_DIAGONAL) != 0)
            return UITOOLS95_DrawDiagEdge(hdc, qrc, edge, grfFlags);
        else
            return UITOOLS95_DrawRectEdge(hdc, qrc, edge, grfFlags);
    }

    // BOOL DrawFocusRect(HDC hDC, const RECT *lprc)
    static public int DrawFocusRect(int hDC, int lprc) {
        LOGBRUSH lb = new LOGBRUSH();

        int hOldBrush = WinDC.SelectObject(hDC, GdiObj.GetStockObject(NULL_BRUSH));
        lb.lbStyle = BS_SOLID;
        lb.lbColor = SysParams.GetSysColor(COLOR_WINDOWTEXT);
        int hNewPen = WinPen.ExtCreatePen(PS_COSMETIC | PS_ALTERNATE, 1, lb.allocTemp(), 0, NULL);
        int hOldPen = WinDC.SelectObject(hDC, hNewPen);
        int oldDrawMode = WinDC.SetROP2(hDC, R2_XORPEN);
        int oldBkMode = WinDC.SetBkMode(hDC, TRANSPARENT);

        WinRect rc = new WinRect(lprc);
        Painting.Rectangle(hDC, rc.left, rc.top, rc.right, rc.bottom);

        WinDC.SetBkMode(hDC, oldBkMode);
        WinDC.SetROP2(hDC, oldDrawMode);
        WinDC.SelectObject(hDC, hOldPen);
        GdiObj.DeleteObject(hNewPen);
        WinDC.SelectObject(hDC, hOldBrush);

        return TRUE;
    }

    // BOOL DrawState(HDC hdc, HBRUSH hbr, DRAWSTATEPROC lpOutputFunc, LPARAM lData, WPARAM wData, int x, int y, int cx, int cy, UINT fuFlags)
    static public int DrawStateA(int hdc, int hbr, int lpOutputFunc, int lData, int wData, int x, int y, int cx, int cy, int fuFlags) {
        return UITOOLS_DrawState(hdc, hbr, lpOutputFunc, lData, wData, x, y, cx, cy, fuFlags, false);
    }

    // BOOL DrawFrameControl(HDC hdc, LPRECT lprc, UINT uType, UINT uState)
    static public int DrawFrameControl(int hdc, int rc, int uType, int uState) {
        switch (uType) {
            case DFC_BUTTON:
                return UITOOLS95_DrawFrameButton(hdc, rc, uState);
            case DFC_CAPTION:
                //return UITOOLS95_DrawFrameCaption(hdc, rc, uState);
            case DFC_MENU:
                //return UITOOLS95_DrawFrameMenu(hdc, rc, uState);
                /*case DFC_POPUPMENU:
                FIXME("DFC_POPUPMENU: not implemented\n");
                break;*/
            case DFC_SCROLL:
                //return UITOOLS95_DrawFrameScroll(hdc, rc, uState);
            default:
                warn("DrawFrameControl: unknown type " + uType);
        }
        return FALSE;
    }

    // INT WINAPI FrameRect( HDC hdc, const RECT *rect, HBRUSH hbrush )
    static public int FrameRect(int hdc, int rect, int hbrush) {
        WinRect r = new WinRect(rect);

        if ((r.right <= r.left) || (r.bottom <= r.top)) return FALSE;
        int prevBrush = WinDC.SelectObject(hdc, hbrush);
        if (prevBrush == 0) return FALSE;

        WinDC.PatBlt(hdc, r.left, r.top, 1, r.bottom - r.top, PATCOPY);
        WinDC.PatBlt(hdc, r.right - 1, r.top, 1, r.bottom - r.top, PATCOPY);
        WinDC.PatBlt(hdc, r.left, r.top, r.right - r.left, 1, PATCOPY);
        WinDC.PatBlt(hdc, r.left, r.bottom - 1, r.right - r.left, 1, PATCOPY);

        WinDC.SelectObject(hdc, prevBrush);
        return TRUE;
    }

    // BOOL InflateRect(LPRECT lprc, int dx, int dy)
    static public int InflateRect(int lprc, int dx, int dy) {
        if (lprc == 0)
            return FALSE;
        WinRect rect = new WinRect(lprc);
        rect.inflate(dx, dy);
        rect.write(lprc);
        return TRUE;
    }

    // BOOL IntersectRect(LPRECT lprcDst, const RECT *lprcSrc1, const RECT *lprcSrc2)
    static public int IntersectRect(int lprcDst, int lprcSrc1, int lprcSrc2) {
        if (lprcDst == 0 || lprcSrc1 == 0 || lprcSrc2 == 0)
            return FALSE;
        WinRect rect = new WinRect(lprcSrc1);
        boolean result = rect.intersect(rect, new WinRect(lprcSrc2));
        rect.write(lprcDst);
        return BOOL(result);
    }

    // BOOL IsRectEmpty(const RECT *lprc)
    static public int IsRectEmpty(int lprc) {
        if (lprc == 0)
            return TRUE;
        WinRect rect = new WinRect(lprc);
        return BOOL(rect.left >= rect.right || rect.left >= rect.bottom);
    }

    // BOOL OffsetRect(LPRECT lprc, int dx, int dy)
    static public int OffsetRect(int lprc, int dx, int dy) {
        WinRect rect = new WinRect(lprc);
        rect.left += dx;
        rect.right += dx;
        rect.top += dy;
        rect.bottom += dy;
        rect.write(lprc);
        return TRUE;
    }

    // BOOL SetRect(LPRECT lprc, int xLeft, int yTop, int xRight, int yBottom)
    static public int SetRect(int lprc, int xLeft, int yTop, int xRight, int yBottom) {
        WinRect.write(lprc, xLeft, yTop, xRight, yBottom);
        return TRUE;
    }

    // BOOL SetRectEmpty(LPRECT lprc)
    static public int SetRectEmpty(int lprc) {
        if (lprc == 0)
            return FALSE;
        writed(lprc, 0);
        writed(lprc + 4, 0);
        writed(lprc + 8, 0);
        writed(lprc + 12, 0);
        return FALSE;
    }

    /**
     * ********************************************************************
     * set_control_clipping
     * <p/>
     * Set clipping for a builtin control that uses CS_PARENTDC.
     * Return the previous clip region if any.
     */
    static public int set_control_clipping(int hdc, int pRect) {
        return set_control_clipping(hdc, new WinRect(pRect));
    }

    static public int set_control_clipping(int hdc, WinRect rc) {
        int hrgn = WinRegion.CreateRectRgn(0, 0, 0, 0);

        if (Clipping.GetClipRgn(hdc, hrgn) != 1) {
            GdiObj.DeleteObject(hrgn);
            hrgn = 0;
        }
        // Mapping.DPtoLP( hdc, (POINT *)&rc, 2 );
        Clipping.IntersectClipRect(hdc, rc.left, rc.top, rc.right, rc.bottom);
        return hrgn;
    }

    static final private int LTInnerNormal[] = {
            -1, -1, -1, -1,
            -1, COLOR_BTNHIGHLIGHT, COLOR_BTNHIGHLIGHT, -1,
            -1, COLOR_3DDKSHADOW, COLOR_3DDKSHADOW, -1,
            -1, -1, -1, -1
    };

    static final private int LTOuterNormal[] = {
            -1, COLOR_3DLIGHT, COLOR_BTNSHADOW, -1,
            COLOR_BTNHIGHLIGHT, COLOR_3DLIGHT, COLOR_BTNSHADOW, -1,
            COLOR_3DDKSHADOW, COLOR_3DLIGHT, COLOR_BTNSHADOW, -1,
            -1, COLOR_3DLIGHT, COLOR_BTNSHADOW, -1
    };

    static final private int RBInnerNormal[] = {
            -1, -1, -1, -1,
            -1, COLOR_BTNSHADOW, COLOR_BTNSHADOW, -1,
            -1, COLOR_3DLIGHT, COLOR_3DLIGHT, -1,
            -1, -1, -1, -1
    };

    static final private int RBOuterNormal[] = {
            -1, COLOR_3DDKSHADOW, COLOR_BTNHIGHLIGHT, -1,
            COLOR_BTNSHADOW, COLOR_3DDKSHADOW, COLOR_BTNHIGHLIGHT, -1,
            COLOR_3DLIGHT, COLOR_3DDKSHADOW, COLOR_BTNHIGHLIGHT, -1,
            -1, COLOR_3DDKSHADOW, COLOR_BTNHIGHLIGHT, -1
    };

    static final private int LTInnerSoft[] = {
            -1, -1, -1, -1,
            -1, COLOR_3DLIGHT, COLOR_3DLIGHT, -1,
            -1, COLOR_BTNSHADOW, COLOR_BTNSHADOW, -1,
            -1, -1, -1, -1
    };

    static final private int LTOuterSoft[] = {
            -1, COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1,
            COLOR_3DLIGHT, COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1,
            COLOR_BTNSHADOW, COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1,
            -1, COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1
    };

    static final private int[] RBInnerSoft = RBInnerNormal;   /* These are the same */
    static final private int[] RBOuterSoft = RBOuterNormal;

    static final private int LTRBOuterMono[] = {
            -1, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
            COLOR_WINDOW, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
            COLOR_WINDOW, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
            COLOR_WINDOW, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
    };

    static final private int LTRBInnerMono[] = {
            -1, -1, -1, -1,
            -1, COLOR_WINDOW, COLOR_WINDOW, COLOR_WINDOW,
            -1, COLOR_WINDOW, COLOR_WINDOW, COLOR_WINDOW,
            -1, COLOR_WINDOW, COLOR_WINDOW, COLOR_WINDOW,
    };

    static final private int LTRBOuterFlat[] = {
            -1, COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
            COLOR_BTNFACE, COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
            COLOR_BTNFACE, COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
            COLOR_BTNFACE, COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
    };

    static final private int LTRBInnerFlat[] = {
            -1, -1, -1, -1,
            -1, COLOR_BTNFACE, COLOR_BTNFACE, COLOR_BTNFACE,
            -1, COLOR_BTNFACE, COLOR_BTNFACE, COLOR_BTNFACE,
            -1, COLOR_BTNFACE, COLOR_BTNFACE, COLOR_BTNFACE,
    };

    /**
     * ********************************************************************
     * UITOOLS_DrawDiagEdge
     * <p/>
     * Same as DrawEdge invoked with BF_DIAGONAL
     * <p/>
     * 03-Dec-1997: Changed by Bertho Stultiens
     * <p/>
     * See also comments with UITOOLS_DrawRectEdge()
     */
    static private int UITOOLS95_DrawDiagEdge(int hdc, int prc, int uType, int uFlags) {
        WinPoint[] Points = new WinPoint[4];
        for (int i = 0; i < Points.length; i++) Points[i] = new WinPoint();
        int InnerI, OuterI;
        int InnerPen, OuterPen;
        int SavePoint = getTempBuffer(WinPoint.SIZE);
        int SavePen;
        int spx, spy;
        int epx, epy;
        WinRect rc = new WinRect(prc);
        int Width = rc.right - rc.left;
        int Height = rc.bottom - rc.top;
        int SmallDiam = Width > Height ? Height : Width;
        int retval = BOOL(!(((uType & BDR_INNER) == BDR_INNER || (uType & BDR_OUTER) == BDR_OUTER) && (uFlags & (BF_FLAT | BF_MONO)) == 0));
        int add = (LTRBInnerMono[uType & (BDR_INNER | BDR_OUTER)] != -1 ? 1 : 0) + (LTRBOuterMono[uType & (BDR_INNER | BDR_OUTER)] != -1 ? 1 : 0);

        /* Init some vars */
        OuterPen = InnerPen = GdiObj.GetStockObject(NULL_PEN);
        SavePen = WinDC.SelectObject(hdc, InnerPen);
        spx = spy = epx = epy = 0; /* Satisfy the compiler... */

        /* Determine the colors of the edges */
        if ((uFlags & BF_MONO) != 0) {
            InnerI = LTRBInnerMono[uType & (BDR_INNER | BDR_OUTER)];
            OuterI = LTRBOuterMono[uType & (BDR_INNER | BDR_OUTER)];
        } else if ((uFlags & BF_FLAT) != 0) {
            InnerI = LTRBInnerFlat[uType & (BDR_INNER | BDR_OUTER)];
            OuterI = LTRBOuterFlat[uType & (BDR_INNER | BDR_OUTER)];
        } else if ((uFlags & BF_SOFT) != 0) {
            if ((uFlags & BF_BOTTOM) != 0) {
                InnerI = RBInnerSoft[uType & (BDR_INNER | BDR_OUTER)];
                OuterI = RBOuterSoft[uType & (BDR_INNER | BDR_OUTER)];
            } else {
                InnerI = LTInnerSoft[uType & (BDR_INNER | BDR_OUTER)];
                OuterI = LTOuterSoft[uType & (BDR_INNER | BDR_OUTER)];
            }
        } else {
            if ((uFlags & BF_BOTTOM) != 0) {
                InnerI = RBInnerNormal[uType & (BDR_INNER | BDR_OUTER)];
                OuterI = RBOuterNormal[uType & (BDR_INNER | BDR_OUTER)];
            } else {
                InnerI = LTInnerNormal[uType & (BDR_INNER | BDR_OUTER)];
                OuterI = LTOuterNormal[uType & (BDR_INNER | BDR_OUTER)];
            }
        }

        if (InnerI != -1) InnerPen = SysParams.GetSysColorPen(InnerI);
        if (OuterI != -1) OuterPen = SysParams.GetSysColorPen(OuterI);

        PaintingGDI.MoveToEx(hdc, 0, 0, SavePoint);

        /* Don't ask me why, but this is what is visible... */
        /* This must be possible to do much simpler, but I fail to */
        /* see the logic in the MS implementation (sigh...). */
        /* So, this might look a bit brute force here (and it is), but */
        /* it gets the job done;) */

        switch (uFlags & BF_RECT) {
            case 0:
            case BF_LEFT:
            case BF_BOTTOM:
            case BF_BOTTOMLEFT:
                /* Left bottom endpoint */
                epx = rc.left - 1;
                spx = epx + SmallDiam;
                epy = rc.bottom;
                spy = epy - SmallDiam;
                break;

            case BF_TOPLEFT:
            case BF_BOTTOMRIGHT:
                /* Left top endpoint */
                epx = rc.left - 1;
                spx = epx + SmallDiam;
                epy = rc.top - 1;
                spy = epy + SmallDiam;
                break;

            case BF_TOP:
            case BF_RIGHT:
            case BF_TOPRIGHT:
            case BF_RIGHT | BF_LEFT:
            case BF_RIGHT | BF_LEFT | BF_TOP:
            case BF_BOTTOM | BF_TOP:
            case BF_BOTTOM | BF_TOP | BF_LEFT:
            case BF_BOTTOMRIGHT | BF_LEFT:
            case BF_BOTTOMRIGHT | BF_TOP:
            case BF_RECT:
                /* Right top endpoint */
                spx = rc.left;
                epx = spx + SmallDiam;
                spy = rc.bottom - 1;
                epy = spy - SmallDiam;
                break;
        }

        PaintingGDI.MoveToEx(hdc, spx, spy, NULL);
        WinDC.SelectObject(hdc, OuterPen);
        PaintingGDI.LineTo(hdc, epx, epy);

        WinDC.SelectObject(hdc, InnerPen);

        switch (uFlags & (BF_RECT | BF_DIAGONAL)) {
            case BF_DIAGONAL_ENDBOTTOMLEFT:
            case (BF_DIAGONAL | BF_BOTTOM):
            case BF_DIAGONAL:
            case (BF_DIAGONAL | BF_LEFT):
                PaintingGDI.MoveToEx(hdc, spx - 1, spy, NULL);
                PaintingGDI.LineTo(hdc, epx, epy - 1);
                Points[0].x = spx - add;
                Points[0].y = spy;
                Points[1].x = rc.left;
                Points[1].y = rc.top;
                Points[2].x = epx + 1;
                Points[2].y = epy - 1 - add;
                Points[3] = Points[2];
                break;

            case BF_DIAGONAL_ENDBOTTOMRIGHT:
                PaintingGDI.MoveToEx(hdc, spx - 1, spy, NULL);
                PaintingGDI.LineTo(hdc, epx, epy + 1);
                Points[0].x = spx - add;
                Points[0].y = spy;
                Points[1].x = rc.left;
                Points[1].y = rc.bottom - 1;
                Points[2].x = epx + 1;
                Points[2].y = epy + 1 + add;
                Points[3] = Points[2];
                break;

            case (BF_DIAGONAL | BF_BOTTOM | BF_RIGHT | BF_TOP):
            case (BF_DIAGONAL | BF_BOTTOM | BF_RIGHT | BF_TOP | BF_LEFT):
            case BF_DIAGONAL_ENDTOPRIGHT:
            case (BF_DIAGONAL | BF_RIGHT | BF_TOP | BF_LEFT):
                PaintingGDI.MoveToEx(hdc, spx + 1, spy, NULL);
                PaintingGDI.LineTo(hdc, epx, epy + 1);
                Points[0].x = epx - 1;
                Points[0].y = epy + 1 + add;
                Points[1].x = rc.right - 1;
                Points[1].y = rc.top + add;
                Points[2].x = rc.right - 1;
                Points[2].y = rc.bottom - 1;
                Points[3].x = spx + add;
                Points[3].y = spy;
                break;

            case BF_DIAGONAL_ENDTOPLEFT:
                PaintingGDI.MoveToEx(hdc, spx, spy - 1, NULL);
                PaintingGDI.LineTo(hdc, epx + 1, epy);
                Points[0].x = epx + 1 + add;
                Points[0].y = epy + 1;
                Points[1].x = rc.right - 1;
                Points[1].y = rc.top;
                Points[2].x = rc.right - 1;
                Points[2].y = rc.bottom - 1 - add;
                Points[3].x = spx;
                Points[3].y = spy - add;
                break;

            case (BF_DIAGONAL | BF_TOP):
            case (BF_DIAGONAL | BF_BOTTOM | BF_TOP):
            case (BF_DIAGONAL | BF_BOTTOM | BF_TOP | BF_LEFT):
                PaintingGDI.MoveToEx(hdc, spx + 1, spy - 1, NULL);
                PaintingGDI.LineTo(hdc, epx, epy);
                Points[0].x = epx - 1;
                Points[0].y = epy + 1;
                Points[1].x = rc.right - 1;
                Points[1].y = rc.top;
                Points[2].x = rc.right - 1;
                Points[2].y = rc.bottom - 1 - add;
                Points[3].x = spx + add;
                Points[3].y = spy - add;
                break;

            case (BF_DIAGONAL | BF_RIGHT):
            case (BF_DIAGONAL | BF_RIGHT | BF_LEFT):
            case (BF_DIAGONAL | BF_RIGHT | BF_LEFT | BF_BOTTOM):
                PaintingGDI.MoveToEx(hdc, spx, spy, NULL);
                PaintingGDI.LineTo(hdc, epx - 1, epy + 1);
                Points[0].x = spx;
                Points[0].y = spy;
                Points[1].x = rc.left;
                Points[1].y = rc.top + add;
                Points[2].x = epx - 1 - add;
                Points[2].y = epy + 1 + add;
                Points[3] = Points[2];
                break;
        }

        /* Fill the interior if asked */
        if ((uFlags & BF_MIDDLE) != 0 && retval != 0) {
            int hb = SysParams.GetSysColorBrush((uFlags & BF_MONO) != 0 ? COLOR_WINDOW : COLOR_BTNFACE);
            int hp = SysParams.GetSysColorPen((uFlags & BF_MONO) != 0 ? COLOR_WINDOW : COLOR_BTNFACE);
            int hbsave = WinDC.SelectObject(hdc, hb);
            int hpsave = WinDC.SelectObject(hdc, hp);
            int p = getTempBuffer(WinPoint.SIZE * 4);
            for (int i = 0; i < Points.length; i++)
                Points[i].write(p + WinPoint.SIZE * i);
            PaintingGDI.Polygon(hdc, p, 4);
            WinDC.SelectObject(hdc, hbsave);
            WinDC.SelectObject(hdc, hpsave);
        }

        /* Adjust rectangle if asked */
        if ((uFlags & BF_ADJUST) != 0) {
            if ((uFlags & BF_LEFT) != 0) rc.left += add;
            if ((uFlags & BF_RIGHT) != 0) rc.right -= add;
            if ((uFlags & BF_TOP) != 0) rc.top += add;
            if ((uFlags & BF_BOTTOM) != 0) rc.bottom -= add;
            rc.write(prc);
        }

        /* Cleanup */
        WinDC.SelectObject(hdc, SavePen);
        WinPoint p = new WinPoint(SavePoint);
        PaintingGDI.MoveToEx(hdc, p.x, p.y, NULL);

        return retval;
    }

    /**
     * ********************************************************************
     * UITOOLS_DrawRectEdge
     * <p/>
     * Same as DrawEdge invoked without BF_DIAGONAL
     * <p/>
     * 23-Nov-1997: Changed by Bertho Stultiens
     * <p/>
     * Well, I started testing this and found out that there are a few things
     * that weren't quite as win95. The following rewrite should reproduce
     * win95 results completely.
     * The colorselection is table-driven to avoid awful if-statements.
     * The table below show the color settings.
     * <p/>
     * Pen selection table for uFlags = 0
     * <p/>
     * uType |  LTI  |  LTO  |  RBI  |  RBO
     * ------+-------+-------+-------+-------
     * 0000 |   x   |   x   |   x   |   x
     * 0001 |   x   |  22   |   x   |  21
     * 0010 |   x   |  16   |   x   |  20
     * 0011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     * 0100 |   x   |  20   |   x   |  16
     * 0101 |  20   |  22   |  16   |  21
     * 0110 |  20   |  16   |  16   |  20
     * 0111 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     * 1000 |   x   |  21   |   x   |  22
     * 1001 |  21   |  22   |  22   |  21
     * 1010 |  21   |  16   |  22   |  20
     * 1011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     * 1100 |   x   |   x   |   x   |   x
     * 1101 |   x   | x (22)|   x   | x (21)
     * 1110 |   x   | x (16)|   x   | x (20)
     * 1111 |   x   |   x   |   x   |   x
     * <p/>
     * Pen selection table for uFlags = BF_SOFT
     * <p/>
     * uType |  LTI  |  LTO  |  RBI  |  RBO
     * ------+-------+-------+-------+-------
     * 0000 |   x   |   x   |   x   |   x
     * 0001 |   x   |  20   |   x   |  21
     * 0010 |   x   |  21   |   x   |  20
     * 0011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     * 0100 |   x   |  22   |   x   |  16
     * 0101 |  22   |  20   |  16   |  21
     * 0110 |  22   |  21   |  16   |  20
     * 0111 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     * 1000 |   x   |  16   |   x   |  22
     * 1001 |  16   |  20   |  22   |  21
     * 1010 |  16   |  21   |  22   |  20
     * 1011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     * 1100 |   x   |   x   |   x   |   x
     * 1101 |   x   | x (20)|   x   | x (21)
     * 1110 |   x   | x (21)|   x   | x (20)
     * 1111 |   x   |   x   |   x   |   x
     * <p/>
     * x = don't care; (n) = is what win95 actually uses
     * LTI = left Top Inner line
     * LTO = left Top Outer line
     * RBI = Right Bottom Inner line
     * RBO = Right Bottom Outer line
     * 15 = COLOR_BTNFACE
     * 16 = COLOR_BTNSHADOW
     * 20 = COLOR_BTNHIGHLIGHT
     * 21 = COLOR_3DDKSHADOW
     * 22 = COLOR_3DLIGHT
     */


    static private int UITOOLS95_DrawRectEdge(int hdc, int rc, int uType, int uFlags) {
        int LTInnerI, LTOuterI;
        int RBInnerI, RBOuterI;
        int LTInnerPen, LTOuterPen, RBInnerPen, RBOuterPen;
        int LBpenplus = 0;
        int LTpenplus = 0;
        int RTpenplus = 0;
        int RBpenplus = 0;
        int retval = BOOL(!(((uType & BDR_INNER) == BDR_INNER || (uType & BDR_OUTER) == BDR_OUTER) && (uFlags & (BF_FLAT | BF_MONO)) == 0));

        /* Init some vars */
        LTInnerPen = LTOuterPen = RBInnerPen = RBOuterPen = GdiObj.GetStockObject(NULL_PEN);
        int SavePen = WinDC.SelectObject(hdc, LTInnerPen);

        /* Determine the colors of the edges */
        if ((uFlags & BF_MONO) != 0) {
            LTInnerI = RBInnerI = LTRBInnerMono[uType & (BDR_INNER | BDR_OUTER)];
            LTOuterI = RBOuterI = LTRBOuterMono[uType & (BDR_INNER | BDR_OUTER)];
        } else if ((uFlags & BF_FLAT) != 0) {
            LTInnerI = RBInnerI = LTRBInnerFlat[uType & (BDR_INNER | BDR_OUTER)];
            LTOuterI = RBOuterI = LTRBOuterFlat[uType & (BDR_INNER | BDR_OUTER)];

            if (LTInnerI != -1) LTInnerI = RBInnerI = COLOR_BTNFACE;
        } else if ((uFlags & BF_SOFT) != 0) {
            LTInnerI = LTInnerSoft[uType & (BDR_INNER | BDR_OUTER)];
            LTOuterI = LTOuterSoft[uType & (BDR_INNER | BDR_OUTER)];
            RBInnerI = RBInnerSoft[uType & (BDR_INNER | BDR_OUTER)];
            RBOuterI = RBOuterSoft[uType & (BDR_INNER | BDR_OUTER)];
        } else {
            LTInnerI = LTInnerNormal[uType & (BDR_INNER | BDR_OUTER)];
            LTOuterI = LTOuterNormal[uType & (BDR_INNER | BDR_OUTER)];
            RBInnerI = RBInnerNormal[uType & (BDR_INNER | BDR_OUTER)];
            RBOuterI = RBOuterNormal[uType & (BDR_INNER | BDR_OUTER)];
        }

        if ((uFlags & BF_BOTTOMLEFT) == BF_BOTTOMLEFT) LBpenplus = 1;
        if ((uFlags & BF_TOPRIGHT) == BF_TOPRIGHT) RTpenplus = 1;
        if ((uFlags & BF_BOTTOMRIGHT) == BF_BOTTOMRIGHT) RBpenplus = 1;
        if ((uFlags & BF_TOPLEFT) == BF_TOPLEFT) LTpenplus = 1;

        if (LTInnerI != -1) LTInnerPen = SysParams.GetSysColorPen(LTInnerI);
        if (LTOuterI != -1) LTOuterPen = SysParams.GetSysColorPen(LTOuterI);
        if (RBInnerI != -1) RBInnerPen = SysParams.GetSysColorPen(RBInnerI);
        if (RBOuterI != -1) RBOuterPen = SysParams.GetSysColorPen(RBOuterI);

        int SavePoint = getTempBuffer(WinPoint.SIZE);
        PaintingGDI.MoveToEx(hdc, 0, 0, SavePoint);

        /* Draw the outer edge */
        WinRect InnerRect = new WinRect(rc);
        WinDC.SelectObject(hdc, LTOuterPen);
        if ((uFlags & BF_TOP) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.left, InnerRect.top, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right, InnerRect.top);
        }
        if ((uFlags & BF_LEFT) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.left, InnerRect.top, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.left, InnerRect.bottom);
        }
        WinDC.SelectObject(hdc, RBOuterPen);
        if ((uFlags & BF_BOTTOM) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.left, InnerRect.bottom - 1, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 1, InnerRect.bottom - 1);
        }
        if ((uFlags & BF_RIGHT) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.right - 1, InnerRect.top, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 1, InnerRect.bottom);
        }

        /* Draw the inner edge */
        WinDC.SelectObject(hdc, LTInnerPen);
        if ((uFlags & BF_TOP) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.left + LTpenplus, InnerRect.top + 1, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - RTpenplus, InnerRect.top + 1);
        }
        if ((uFlags & BF_LEFT) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.left + 1, InnerRect.top + LTpenplus, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.left + 1, InnerRect.bottom - LBpenplus);
        }
        WinDC.SelectObject(hdc, RBInnerPen);
        if ((uFlags & BF_BOTTOM) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.left + LBpenplus, InnerRect.bottom - 2, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 1 - RBpenplus, InnerRect.bottom - 2);
        }
        if ((uFlags & BF_RIGHT) != 0) {
            PaintingGDI.MoveToEx(hdc, InnerRect.right - 2, InnerRect.top + 2 - RBpenplus, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 2, InnerRect.bottom - 2 + RTpenplus);
        }

        if (((uFlags & BF_MIDDLE) != 0 && retval != 0) || (uFlags & BF_ADJUST) != 0) {
            int add = (LTRBInnerMono[uType & (BDR_INNER | BDR_OUTER)] != -1 ? 1 : 0)
                    + (LTRBOuterMono[uType & (BDR_INNER | BDR_OUTER)] != -1 ? 1 : 0);

            if ((uFlags & BF_LEFT) != 0) InnerRect.left += add;
            if ((uFlags & BF_RIGHT) != 0) InnerRect.right -= add;
            if ((uFlags & BF_TOP) != 0) InnerRect.top += add;
            if ((uFlags & BF_BOTTOM) != 0) InnerRect.bottom -= add;

            if ((uFlags & BF_MIDDLE) != 0 && retval != 0) {
                WinDC.FillRect(hdc, InnerRect.allocTemp(), SysParams.GetSysColorBrush((uFlags & BF_MONO) != 0 ? COLOR_WINDOW : COLOR_BTNFACE));
            }

            if ((uFlags & BF_ADJUST) != 0)
                InnerRect.write(rc);
        }

        /* Cleanup */
        WinDC.SelectObject(hdc, SavePen);
        WinPoint p = new WinPoint(SavePoint);
        PaintingGDI.MoveToEx(hdc, p.x, p.y, NULL);
        return retval;
    }

    /**
     * *******************************************************************
     * UITOOLS_DrawStateJam
     * <p/>
     * Jams in the requested type in the dc
     */
    static private int UITOOLS_DrawStateJam(int hdc, int opcode, int func, int lp, int wp, WinRect rc, int dtflags, boolean unicode) {
        int cx = rc.width();
        int cy = rc.height();

        switch (opcode) {
            case DST_TEXT:
            case DST_PREFIXTEXT:
                if (unicode) {
                    Win.panic("UITOOLS_DrawStateJam unicode not implemented yet");
                    //return Text.DrawTextW(hdc, lp, wp, rc.allocTemp(), dtflags);
                } else
                    return WinText.DrawTextA(hdc, lp, wp, rc.allocTemp(), dtflags);

            case DST_ICON:
                return WinIcon.DrawIconEx(hdc, rc.left, rc.top, lp, 0, 0, 0, NULL, DI_NORMAL);

            case DST_BITMAP:
                int memdc = WinDC.CreateCompatibleDC(hdc);
                if (memdc == 0) return FALSE;
                int hbmsave = WinDC.SelectObject(memdc, lp);
                if (hbmsave == 0) {
                    WinDC.DeleteDC(memdc);
                    return FALSE;
                }
                int retval = BitBlt.BitBlt(hdc, rc.left, rc.top, cx, cy, memdc, 0, 0, SRCCOPY);
                WinDC.SelectObject(memdc, hbmsave);
                WinDC.DeleteDC(memdc);
                return retval;

            case DST_COMPLEX:
                if (func != 0) {
                    /* DRAWSTATEPROC assumes that it draws at the center of coordinates  */

                    Mapping.OffsetViewportOrgEx(hdc, rc.left, rc.top, NULL);
                    if (func == -1)
                        ButtonWindow.BUTTON_DrawTextCallback(hdc, lp, wp, cx, cy);
                    else
                        WinSystem.call(func, hdc, lp, wp, cx, cy);
                    int bRet = CPU_Regs.reg_eax.dword;
                    /* Restore origin */
                    Mapping.OffsetViewportOrgEx(hdc, -rc.left, -rc.top, NULL);
                    return bRet;
                } else
                    return FALSE;
        }
        return FALSE;
    }

    /**
     * *******************************************************************
     * UITOOLS_DrawState()
     */
    static private int UITOOLS_DrawState(int hdc, int hbr, int func, int lp, int wp, int x, int y, int cx, int cy, int flags, boolean unicode) {
        int dtflags = DT_NOCLIP;
        int opcode = flags & 0xf;
        int len = wp;

        if ((opcode == DST_TEXT || opcode == DST_PREFIXTEXT) && len == 0) {   /* The string is '\0' terminated */
            if (lp == 0) return FALSE;

            if (unicode)
                len = StringUtil.strlenW(lp);
            else
                len = StringUtil.strlenA(lp);
        }
        int retval = 0;
        /* Find out what size the image has if not given by caller */
        if (cx == 0 || cy == 0) {
            WinSize s = new WinSize();

            switch (opcode) {
                case DST_TEXT:
                case DST_PREFIXTEXT: {
                    int lpSize = getTempBuffer(WinSize.SIZE);
                    if (unicode)
                        retval = WinFont.GetTextExtentPoint32W(hdc, lp, len, lpSize);
                    else
                        retval = WinFont.GetTextExtentPoint32A(hdc, lp, len, lpSize);
                    if (retval == 0) return FALSE;
                    break;
                }
                case DST_ICON: {
                    WinIcon icon = WinIcon.get(lp);
                    if (icon == null)
                        return FALSE;
                    s.cx = icon.cx;
                    s.cy = icon.cy;
                    break;
                }
                case DST_BITMAP: {
                    WinBitmap bitmap = WinBitmap.get(lp);
                    if (bitmap == null)
                        return FALSE;
                    s.cx = bitmap.getWidth();
                    s.cy = bitmap.getHeight();
                    break;
                }
                case DST_COMPLEX: /* cx and cy must be set in this mode */
                    return FALSE;
            }

            if (cx == 0) cx = s.cx;
            if (cy == 0) cy = s.cy;
        }

        WinRect rc = new WinRect();
        rc.left = x;
        rc.top = y;
        rc.right = x + cx;
        rc.bottom = y + cy;

        if ((flags & DSS_RIGHT) != 0)    /* This one is not documented in the win32.hlp file */
            dtflags |= DT_RIGHT;
        if (opcode == DST_TEXT)
            dtflags |= DT_NOPREFIX;

        /* For DSS_NORMAL we just jam in the image and return */
        if ((flags & 0x7ff0) == DSS_NORMAL) {
            return UITOOLS_DrawStateJam(hdc, opcode, func, lp, len, rc, dtflags, unicode);
        }

        /* For all other states we need to convert the image to B/W in a local bitmap */
        /* before it is displayed */
        int fg = WinDC.SetTextColor(hdc, RGB(0, 0, 0));
        int bg = WinDC.SetBkColor(hdc, RGB(255, 255, 255));
        int hbm = NULL;
        int hbmsave = NULL;
        int hbrtmp = 0;
        int memdc = NULL;
        int hbsave = NULL;
        retval = FALSE; /* assume failure */

        /* From here on we must use "goto cleanup" when something goes wrong */
        hbm = WinBitmap.CreateBitmap(cx, cy, 1, 32, NULL);
        try {
            if (hbm == 0) return FALSE;
            memdc = WinDC.CreateCompatibleDC(hdc);
            if (memdc == 0) return FALSE;
            hbmsave = WinDC.SelectObject(memdc, hbm);
            if (hbmsave == 0) return FALSE;
            rc.left = rc.top = 0;
            rc.right = cx;
            rc.bottom = cy;
            if (WinDC.FillRect(memdc, rc.allocTemp(), GdiObj.GetStockObject(WHITE_BRUSH)) == 0) return FALSE;
            WinDC.SetBkColor(memdc, RGB(255, 255, 255));
            WinDC.SetTextColor(memdc, RGB(0, 0, 0));
            int hfsave = WinDC.SelectObject(memdc, GdiObj.GetCurrentObject(hdc, OBJ_FONT));

            /* DST_COMPLEX may draw text as well,
             * so we must be sure that correct font is selected
             */
            if (hfsave == 0 && (opcode <= DST_PREFIXTEXT)) return FALSE;
            int tmp = UITOOLS_DrawStateJam(memdc, opcode, func, lp, len, rc, dtflags, unicode);
            if (hfsave != 0) WinDC.SelectObject(memdc, hfsave);
            if (tmp == 0) return FALSE;

            /* This state cause the image to be dithered */
            if ((flags & DSS_UNION) != 0) {
                hbsave = WinDC.SelectObject(memdc, StaticData.SYSCOLOR_55AABrush);
                if (hbsave == 0) return FALSE;
                tmp = WinDC.PatBlt(memdc, 0, 0, cx, cy, 0x00FA0089);
                WinDC.SelectObject(memdc, hbsave);
                if (tmp == 0) return FALSE;
            }

            if ((flags & DSS_DISABLED) != 0)
                hbrtmp = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DHILIGHT));
            else if ((flags & DSS_DEFAULT) != 0)
                hbrtmp = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DSHADOW));

            /* Draw light or dark shadow */
            if ((flags & (DSS_DISABLED | DSS_DEFAULT)) != 0) {
                if (hbrtmp == 0) return FALSE;
                hbsave = WinDC.SelectObject(hdc, hbrtmp);
                if (hbsave == 0) return FALSE;
                if (BitBlt.BitBlt(hdc, x + 1, y + 1, cx, cy, memdc, 0, 0, 0x00B8074A) == 0) return FALSE;
                WinDC.SelectObject(hdc, hbsave);
                GdiObj.DeleteObject(hbrtmp);
                hbrtmp = 0;
            }

            if ((flags & DSS_DISABLED) != 0) {
                hbr = hbrtmp = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DSHADOW));
                if (hbrtmp == 0) return FALSE;
            } else if (hbr == 0) {
                hbr = GdiObj.GetStockObject(BLACK_BRUSH);
            }

            hbsave = WinDC.SelectObject(hdc, hbr);

            if (BitBlt.BitBlt(hdc, x, y, cx, cy, memdc, 0, 0, 0x00B8074A) == 0) return FALSE;

            return TRUE; /* We succeeded */
        } finally {
            WinDC.SetTextColor(hdc, fg);
            WinDC.SetBkColor(hdc, bg);

            if (hbsave != 0) WinDC.SelectObject(hdc, hbsave);
            if (hbmsave != 0) WinDC.SelectObject(memdc, hbmsave);
            if (hbrtmp != 0) GdiObj.DeleteObject(hbrtmp);
            if (hbm != 0) GdiObj.DeleteObject(hbm);
            if (memdc != 0) WinDC.DeleteDC(memdc);
        }
    }

    static int UITOOLS95_DrawFrameButton(int hdc, int rc, int uState) {
        switch (uState & 0xff) {
            case DFCS_BUTTONPUSH:
                return UITOOLS95_DFC_ButtonPush(hdc, rc, uState);

            case DFCS_BUTTONCHECK:
            case DFCS_BUTTON3STATE:
                return UITOOLS95_DFC_ButtonCheck(hdc, rc, uState);

            case DFCS_BUTTONRADIOIMAGE:
            case DFCS_BUTTONRADIOMASK:
            case DFCS_BUTTONRADIO:
                return UITOOLS95_DFC_ButtonRadio(hdc, rc, uState);

            default:
                log("Invalid button state=0x" + Ptr.toString(uState));
        }

        return FALSE;
    }

    /**
     * *********************************************************************
     * UITOOLS_DFC_ButtonPush
     * <p/>
     * Draw a push button coming from DrawFrameControl()
     * <p/>
     * Does a pretty good job in emulating MS behavior. Some quirks are
     * however there because MS uses a TrueType font (Marlett) to draw
     * the buttons.
     */
    static private int UITOOLS95_DFC_ButtonPush(int dc, int r, int uFlags) {
        int edge;

        if ((uFlags & (DFCS_PUSHED | DFCS_CHECKED | DFCS_FLAT)) != 0)
            edge = EDGE_SUNKEN;
        else
            edge = EDGE_RAISED;

        if ((uFlags & DFCS_CHECKED) != 0) {
            if ((uFlags & DFCS_MONO) != 0)
                UITOOLS95_DrawRectEdge(dc, r, edge, BF_MONO | BF_RECT | BF_ADJUST);
            else
                UITOOLS95_DrawRectEdge(dc, r, edge, (uFlags & DFCS_FLAT) | BF_RECT | BF_SOFT | BF_ADJUST);

            if ((uFlags & DFCS_TRANSPARENT) == 0)
                UITOOLS_DrawCheckedRect(dc, r);
        } else {
            if ((uFlags & DFCS_MONO) != 0) {
                UITOOLS95_DrawRectEdge(dc, r, edge, BF_MONO | BF_RECT | BF_ADJUST);
                if ((uFlags & DFCS_TRANSPARENT) == 0)
                    WinDC.FillRect(dc, r, SysParams.GetSysColorBrush(COLOR_BTNFACE));
            } else {
                UITOOLS95_DrawRectEdge(dc, r, edge, (uFlags & DFCS_FLAT) | ((uFlags & DFCS_TRANSPARENT) != 0 ? 0 : BF_MIDDLE) | BF_RECT | BF_SOFT);
            }
        }

        /* Adjust rectangle if asked */
        if ((uFlags & DFCS_ADJUSTRECT) != 0) {
            WinRect myr = new WinRect(r);

            myr.left += 2;
            myr.right -= 2;
            myr.top += 2;
            myr.bottom -= 2;
            myr.write(r);
        }
        return TRUE;
    }

    static void UITOOLS_DrawCheckedRect(int dc, int rect) {
        if (SysParams.GetSysColor(COLOR_BTNHIGHLIGHT) == RGB(255, 255, 255)) {
            int hbsave;
            int bg;

            WinDC.FillRect(dc, rect, SysParams.GetSysColorBrush(COLOR_BTNFACE));
            bg = WinDC.SetBkColor(dc, RGB(255, 255, 255));
            hbsave = WinDC.SelectObject(dc, StaticData.SYSCOLOR_55AABrush);
            WinRect rc = new WinRect(rect);
            WinDC.PatBlt(dc, rc.left, rc.top, rc.width(), rc.height(), 0x00FA0089);
            WinDC.SelectObject(dc, hbsave);
            WinDC.SetBkColor(dc, bg);
        } else {
            WinDC.FillRect(dc, rect, SysParams.GetSysColorBrush(COLOR_BTNHIGHLIGHT));
        }
    }

    /**
     * *********************************************************************
     * UITOOLS_DFC_ButtonChcek
     * <p/>
     * Draw a check/3state button coming from DrawFrameControl()
     * <p/>
     * Does a pretty good job in emulating MS behavior. Some quirks are
     * however there because MS uses a TrueType font (Marlett) to draw
     * the buttons.
     */

    static private int UITOOLS95_DFC_ButtonCheck(int dc, int r, int uFlags) {
        int flags = BF_RECT | BF_ADJUST;
        WinRect myr = new WinRect();

        UITOOLS_MakeSquareRect(r, myr);

        if ((uFlags & DFCS_FLAT) != 0)
            flags |= BF_FLAT;
        else if ((uFlags & DFCS_MONO) != 0)
            flags |= BF_MONO;

        int pMyRect = myr.allocTemp();
        UITOOLS95_DrawRectEdge(dc, pMyRect, EDGE_SUNKEN, flags);

        if ((uFlags & DFCS_TRANSPARENT) == 0) {
            if ((uFlags & (DFCS_INACTIVE | DFCS_PUSHED)) != 0)
                WinDC.FillRect(dc, pMyRect, SysParams.GetSysColorBrush(COLOR_BTNFACE));
            else if ((uFlags & DFCS_BUTTON3STATE) != 0 && (uFlags & DFCS_CHECKED) != 0)
                UITOOLS_DrawCheckedRect(dc, pMyRect);
            else
                WinDC.FillRect(dc, pMyRect, SysParams.GetSysColorBrush(COLOR_WINDOW));
        }

        if ((uFlags & DFCS_CHECKED) != 0) {
            int i, k;
            i = (uFlags & DFCS_INACTIVE) != 0 || (uFlags & 0xff) == DFCS_BUTTON3STATE ? COLOR_BTNSHADOW : COLOR_WINDOWTEXT;

            /* draw 7 bars, with h=3w to form the check */
            WinRect bar = new WinRect();
            bar.left = myr.left;
            bar.top = myr.top + 2;
            for (k = 0; k < 7; k++) {
                bar.left = bar.left + 1;
                bar.top = (k < 3) ? bar.top + 1 : bar.top - 1;
                bar.bottom = bar.top + 3;
                bar.right = bar.left + 1;
                WinDC.FillRect(dc, bar.allocTemp(), SysParams.GetSysColorBrush(i));
            }
        }
        return TRUE;
    }

    /**
     * *********************************************************************
     * UITOOLS_MakeSquareRect
     * <p/>
     * Utility to create a square rectangle and returning the width
     */
    static int UITOOLS_MakeSquareRect(int s, WinRect dst) {
        WinRect src = new WinRect(s);
        int Width = src.width();
        int Height = src.height();
        int SmallDiam = Width > Height ? Height : Width;

        dst.copy(src);

        /* Make it a square box */
        if (Width < Height)      /* SmallDiam == Width */ {
            dst.top += (Height - Width) / 2;
            dst.bottom = dst.top + SmallDiam;
        } else if (Width > Height) /* SmallDiam == Height */ {
            dst.left += (Width - Height) / 2;
            dst.right = dst.left + SmallDiam;
        }

        return SmallDiam;
    }

    /**
     * *********************************************************************
     * UITOOLS_DFC_ButtonRadio
     * <p/>
     * Draw a radio/radioimage/radiomask button coming from DrawFrameControl()
     * <p/>
     * Does a pretty good job in emulating MS behavior. Some quirks are
     * however there because MS uses a TrueType font (Marlett) to draw
     * the buttons.
     */
    static private int UITOOLS95_DFC_ButtonRadio(int dc, int r, int uFlags) {
        WinRect myr = new WinRect();
        int i;
        int SmallDiam = UITOOLS_MakeSquareRect(r, myr);
        int BorderShrink = SmallDiam / 16;
        int hpsave;
        int hbsave;
        int xc, yc;

        if (BorderShrink < 1) BorderShrink = 1;

        if ((uFlags & 0xff) == DFCS_BUTTONRADIOIMAGE)
            WinDC.FillRect(dc, r, GdiObj.GetStockObject(BLACK_BRUSH));
        else if ((uFlags & 0xff) == DFCS_BUTTONRADIOMASK)
            WinDC.FillRect(dc, r, GdiObj.GetStockObject(WHITE_BRUSH));

        xc = myr.left + SmallDiam - SmallDiam / 2;
        yc = myr.top + SmallDiam - SmallDiam / 2;

        /* Define bounding box */
        i = 14 * SmallDiam / 16;
        myr.left = xc - i + i / 2;
        myr.right = xc + i / 2;
        myr.top = yc - i + i / 2;
        myr.bottom = yc + i / 2;

        if ((uFlags & 0xff) == DFCS_BUTTONRADIOMASK) {
            hbsave = WinDC.SelectObject(dc, GdiObj.GetStockObject(BLACK_BRUSH));
            Painting.Ellipse(dc, myr.left, myr.top, myr.right, myr.bottom);
            WinDC.SelectObject(dc, hbsave);
        } else {
            if ((uFlags & (DFCS_FLAT | DFCS_MONO)) != 0) {
                hpsave = WinDC.SelectObject(dc, SysParams.GetSysColorPen(COLOR_WINDOWFRAME));
                hbsave = WinDC.SelectObject(dc, SysParams.GetSysColorBrush(COLOR_WINDOWFRAME));
                Painting.Ellipse(dc, myr.left, myr.top, myr.right, myr.bottom);
                WinDC.SelectObject(dc, hbsave);
                WinDC.SelectObject(dc, hpsave);
            } else {
                hpsave = WinDC.SelectObject(dc, SysParams.GetSysColorPen(COLOR_BTNHIGHLIGHT));
                hbsave = WinDC.SelectObject(dc, SysParams.GetSysColorBrush(COLOR_BTNHIGHLIGHT));
                Painting.Pie(dc, myr.left, myr.top, myr.right + 1, myr.bottom + 1, myr.left - 1, myr.bottom, myr.right + 1, myr.top);

                WinDC.SelectObject(dc, SysParams.GetSysColorPen(COLOR_BTNSHADOW));
                WinDC.SelectObject(dc, SysParams.GetSysColorBrush(COLOR_BTNSHADOW));
                Painting.Pie(dc, myr.left, myr.top, myr.right + 1, myr.bottom + 1, myr.right + 1, myr.top, myr.left - 1, myr.bottom);

                myr.left += BorderShrink;
                myr.right -= BorderShrink;
                myr.top += BorderShrink;
                myr.bottom -= BorderShrink;

                WinDC.SelectObject(dc, SysParams.GetSysColorPen(COLOR_3DLIGHT));
                WinDC.SelectObject(dc, SysParams.GetSysColorBrush(COLOR_3DLIGHT));
                Painting.Pie(dc, myr.left, myr.top, myr.right + 1, myr.bottom + 1, myr.left - 1, myr.bottom, myr.right + 1, myr.top);

                WinDC.SelectObject(dc, SysParams.GetSysColorPen(COLOR_3DDKSHADOW));
                WinDC.SelectObject(dc, SysParams.GetSysColorBrush(COLOR_3DDKSHADOW));
                Painting.Pie(dc, myr.left, myr.top, myr.right + 1, myr.bottom + 1, myr.right + 1, myr.top, myr.left - 1, myr.bottom);
                WinDC.SelectObject(dc, hbsave);
                WinDC.SelectObject(dc, hpsave);
            }

            i = 10 * SmallDiam / 16;
            myr.left = xc - i + i / 2;
            myr.right = xc + i / 2;
            myr.top = yc - i + i / 2;
            myr.bottom = yc + i / 2;
            i = (uFlags & (DFCS_INACTIVE | DFCS_PUSHED)) == 0 ? COLOR_WINDOW : COLOR_BTNFACE;
            hpsave = WinDC.SelectObject(dc, SysParams.GetSysColorPen(i));
            hbsave = WinDC.SelectObject(dc, SysParams.GetSysColorBrush(i));
            Painting.Ellipse(dc, myr.left, myr.top, myr.right, myr.bottom);
            WinDC.SelectObject(dc, hbsave);
            WinDC.SelectObject(dc, hpsave);
        }

        if ((uFlags & DFCS_CHECKED) != 0) {
            i = 6 * SmallDiam / 16;
            i = i < 1 ? 1 : i;
            myr.left = xc - i + i / 2;
            myr.right = xc + i / 2;
            myr.top = yc - i + i / 2;
            myr.bottom = yc + i / 2;

            i = (uFlags & DFCS_INACTIVE) != 0 ? COLOR_BTNSHADOW : COLOR_WINDOWTEXT;
            hbsave = WinDC.SelectObject(dc, SysParams.GetSysColorBrush(i));
            hpsave = WinDC.SelectObject(dc, SysParams.GetSysColorPen(i));
            Painting.Ellipse(dc, myr.left, myr.top, myr.right, myr.bottom);
            WinDC.SelectObject(dc, hpsave);
            WinDC.SelectObject(dc, hbsave);
        }

        /* FIXME: M$ has a polygon in the center at relative points: */
        /* 0.476, 0.476 (times SmallDiam, SmallDiam) */
        /* 0.476, 0.525 */
        /* 0.500, 0.500 */
        /* 0.500, 0.499 */
        /* when the button is unchecked. The reason for it is unknown. The */
        /* color is COLOR_BTNHIGHLIGHT, although the polygon gets painted at */
        /* least 3 times (it looks like a clip-region when you see it happen). */
        /* I do not really see a reason why this should be implemented. If you */
        /* have a good reason, let me know. Maybe this is a quirk in the Marlett */
        /* font. */

        return TRUE;
    }
}
