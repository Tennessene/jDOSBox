package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.system.WinPoint;
import jdos.win.system.WinRect;

public class UiTools extends WinAPI {
    // BOOL CopyRect(LPRECT lprcDst, const RECT *lprcSrc)
    static public int CopyRect(int lprcDst, int lprcSrc) {
        if (lprcDst == 0 || lprcSrc==0)
            return FALSE;
        Memory.mem_memcpy(lprcDst, lprcSrc, WinRect.SIZE);
        return TRUE;
    }

    // BOOL DrawEdge(HDC hdc, LPRECT qrc, UINT edge, UINT grfFlags)
    static public int DrawEdge(int hdc, int qrc, int edge, int grfFlags) {
        if((grfFlags & BF_DIAGONAL)!=0)
            return UITOOLS95_DrawDiagEdge(hdc, qrc, edge, grfFlags);
        else
            return UITOOLS95_DrawRectEdge(hdc, qrc, edge, grfFlags);
    }

    // INT WINAPI FrameRect( HDC hdc, const RECT *rect, HBRUSH hbrush )
    static public int FrameRect(int hdc, int rect, int hbrush) {
        WinRect r = new WinRect(rect);

        if ( (r.right <= r.left) || (r.bottom <= r.top) ) return FALSE;
        int prevBrush = WinDC.SelectObject(hdc, hbrush);
        if (prevBrush==0) return FALSE;

        WinDC.PatBlt(hdc, r.left, r.top, 1, r.bottom - r.top, PATCOPY);
        WinDC.PatBlt(hdc, r.right - 1, r.top, 1, r.bottom - r.top, PATCOPY);
        WinDC.PatBlt(hdc, r.left, r.top, r.right - r.left, 1, PATCOPY);
        WinDC.PatBlt(hdc, r.left, r.bottom - 1, r.right - r.left, 1, PATCOPY);

        WinDC.SelectObject(hdc, prevBrush);
        return TRUE;
    }

    // BOOL IsRectEmpty(const RECT *lprc)
    static public int IsRectEmpty(int lprc) {
        if (lprc == 0)
            return TRUE;
        WinRect rect = new WinRect(lprc);
        return BOOL(rect.left>=rect.right || rect.left>=rect.bottom);
    }

    // BOOL OffsetRect(LPRECT lprc, int dx, int dy)
    static public int OffsetRect(int lprc, int dx, int dy) {
        WinRect rect = new WinRect(lprc);
        rect.left+=dx;
        rect.right+=dx;
        rect.top+=dy;
        rect.bottom+=dy;
        rect.write(lprc);
        return TRUE;
    }

    // BOOL SetRect(LPRECT lprc, int xLeft, int yTop, int xRight, int yBottom)
    static public int SetRect(int lprc, int xLeft, int yTop, int xRight, int yBottom) {
        WinRect.write(lprc, xLeft, yTop, xRight, yBottom);
        return TRUE;
    }

    /***********************************************************************
     *           set_control_clipping
     *
     * Set clipping for a builtin control that uses CS_PARENTDC.
     * Return the previous clip region if any.
     */
    static public int set_control_clipping(int hdc, int pRect) {
        int hrgn = WinRegion.CreateRectRgn(0, 0, 0, 0);

        if (Clipping.GetClipRgn(hdc, hrgn) != 1) {
            GdiObj.DeleteObject(hrgn);
            hrgn = 0;
        }
        // Mapping.DPtoLP( hdc, (POINT *)&rc, 2 );
        WinRect rc = new WinRect(pRect);
        Clipping.IntersectClipRect(hdc, rc.left, rc.top, rc.right, rc.bottom);
        return hrgn;
    }

    static final private int LTInnerNormal[] = {
        -1,           -1,                 -1,                 -1,
        -1,           COLOR_BTNHIGHLIGHT, COLOR_BTNHIGHLIGHT, -1,
        -1,           COLOR_3DDKSHADOW,   COLOR_3DDKSHADOW,   -1,
        -1,           -1,                 -1,                 -1
    };

    static final private int LTOuterNormal[] = {
        -1,                 COLOR_3DLIGHT,     COLOR_BTNSHADOW, -1,
        COLOR_BTNHIGHLIGHT, COLOR_3DLIGHT,     COLOR_BTNSHADOW, -1,
        COLOR_3DDKSHADOW,   COLOR_3DLIGHT,     COLOR_BTNSHADOW, -1,
        -1,                 COLOR_3DLIGHT,     COLOR_BTNSHADOW, -1
    };

    static final private int RBInnerNormal[] = {
        -1,           -1,                -1,              -1,
        -1,           COLOR_BTNSHADOW,   COLOR_BTNSHADOW, -1,
        -1,           COLOR_3DLIGHT,     COLOR_3DLIGHT,   -1,
        -1,           -1,                -1,              -1
    };

    static final private int RBOuterNormal[] = {
        -1,              COLOR_3DDKSHADOW,  COLOR_BTNHIGHLIGHT, -1,
        COLOR_BTNSHADOW, COLOR_3DDKSHADOW,  COLOR_BTNHIGHLIGHT, -1,
        COLOR_3DLIGHT,   COLOR_3DDKSHADOW,  COLOR_BTNHIGHLIGHT, -1,
        -1,              COLOR_3DDKSHADOW,  COLOR_BTNHIGHLIGHT, -1
    };

    static final private int LTInnerSoft[] = {
        -1,                  -1,                -1,              -1,
        -1,                  COLOR_3DLIGHT,     COLOR_3DLIGHT,   -1,
        -1,                  COLOR_BTNSHADOW,   COLOR_BTNSHADOW, -1,
        -1,                  -1,                -1,              -1
    };

    static final private int LTOuterSoft[] = {
        -1,              COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1,
        COLOR_3DLIGHT,   COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1,
        COLOR_BTNSHADOW, COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1,
        -1,              COLOR_BTNHIGHLIGHT, COLOR_3DDKSHADOW, -1
    };

    static final private int[] RBInnerSoft = RBInnerNormal;   /* These are the same */
    static final private int[] RBOuterSoft = RBOuterNormal;

    static final private int LTRBOuterMono[] = {
        -1,           COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
        COLOR_WINDOW, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
        COLOR_WINDOW, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
        COLOR_WINDOW, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME, COLOR_WINDOWFRAME,
    };

    static final private int LTRBInnerMono[] = {
        -1, -1,           -1,           -1,
        -1, COLOR_WINDOW, COLOR_WINDOW, COLOR_WINDOW,
        -1, COLOR_WINDOW, COLOR_WINDOW, COLOR_WINDOW,
        -1, COLOR_WINDOW, COLOR_WINDOW, COLOR_WINDOW,
    };

    static final private int LTRBOuterFlat[] = {
        -1,                COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
        COLOR_BTNFACE,     COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
        COLOR_BTNFACE,     COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
        COLOR_BTNFACE,     COLOR_BTNSHADOW, COLOR_BTNSHADOW, COLOR_BTNSHADOW,
    };

    static final private int LTRBInnerFlat[] = {
        -1, -1,              -1,              -1,
        -1, COLOR_BTNFACE,     COLOR_BTNFACE,     COLOR_BTNFACE,
        -1, COLOR_BTNFACE,     COLOR_BTNFACE,     COLOR_BTNFACE,
        -1, COLOR_BTNFACE,     COLOR_BTNFACE,     COLOR_BTNFACE,
    };

    /***********************************************************************
     *           UITOOLS_DrawDiagEdge
     *
     * Same as DrawEdge invoked with BF_DIAGONAL
     *
     * 03-Dec-1997: Changed by Bertho Stultiens
     *
     * See also comments with UITOOLS_DrawRectEdge()
     */
    static private int UITOOLS95_DrawDiagEdge(int hdc, int prc, int uType, int uFlags) {
        WinPoint[] Points = new WinPoint[4]; for (int i=0;i<Points.length;i++) Points[i] = new WinPoint();
        int InnerI, OuterI;
        int InnerPen, OuterPen;
        int SavePoint = getTempBuffer(WinPoint.SIZE);
        int SavePen;
        int spx, spy;
        int epx, epy;
        WinRect rc = new WinRect(prc);
        int Width = rc.right - rc.left;
        int Height= rc.bottom - rc.top;
        int SmallDiam = Width > Height ? Height : Width;
        int retval = BOOL(!(((uType & BDR_INNER) == BDR_INNER || (uType & BDR_OUTER) == BDR_OUTER) && (uFlags & (BF_FLAT|BF_MONO))==0 ));
        int add = (LTRBInnerMono[uType & (BDR_INNER|BDR_OUTER)] != -1 ? 1 : 0) + (LTRBOuterMono[uType & (BDR_INNER|BDR_OUTER)] != -1 ? 1 : 0);

        /* Init some vars */
        OuterPen = InnerPen = GdiObj.GetStockObject(NULL_PEN);
        SavePen = WinDC.SelectObject(hdc, InnerPen);
        spx = spy = epx = epy = 0; /* Satisfy the compiler... */

        /* Determine the colors of the edges */
        if ((uFlags & BF_MONO)!=0) {
            InnerI = LTRBInnerMono[uType & (BDR_INNER|BDR_OUTER)];
            OuterI = LTRBOuterMono[uType & (BDR_INNER|BDR_OUTER)];
        } else if ((uFlags & BF_FLAT)!=0) {
            InnerI = LTRBInnerFlat[uType & (BDR_INNER|BDR_OUTER)];
            OuterI = LTRBOuterFlat[uType & (BDR_INNER|BDR_OUTER)];
        } else if((uFlags & BF_SOFT)!=0) {
            if ((uFlags & BF_BOTTOM)!=0) {
                InnerI = RBInnerSoft[uType & (BDR_INNER|BDR_OUTER)];
                OuterI = RBOuterSoft[uType & (BDR_INNER|BDR_OUTER)];
            }
            else
            {
                InnerI = LTInnerSoft[uType & (BDR_INNER|BDR_OUTER)];
                OuterI = LTOuterSoft[uType & (BDR_INNER|BDR_OUTER)];
            }
        }
        else
        {
            if ((uFlags & BF_BOTTOM)!=0)
            {
                InnerI = RBInnerNormal[uType & (BDR_INNER|BDR_OUTER)];
                OuterI = RBOuterNormal[uType & (BDR_INNER|BDR_OUTER)];
            }
            else
            {
                InnerI = LTInnerNormal[uType & (BDR_INNER|BDR_OUTER)];
                OuterI = LTOuterNormal[uType & (BDR_INNER|BDR_OUTER)];
            }
        }

        if(InnerI != -1) InnerPen = SysParams.GetSysColorPen(InnerI);
        if(OuterI != -1) OuterPen = SysParams.GetSysColorPen(OuterI);

        PaintingGDI.MoveToEx(hdc, 0, 0, SavePoint);

        /* Don't ask me why, but this is what is visible... */
        /* This must be possible to do much simpler, but I fail to */
        /* see the logic in the MS implementation (sigh...). */
        /* So, this might look a bit brute force here (and it is), but */
        /* it gets the job done;) */

        switch(uFlags & BF_RECT)
        {
        case 0:
        case BF_LEFT:
        case BF_BOTTOM:
        case BF_BOTTOMLEFT:
            /* Left bottom endpoint */
            epx = rc.left-1;
            spx = epx + SmallDiam;
            epy = rc.bottom;
            spy = epy - SmallDiam;
            break;

        case BF_TOPLEFT:
        case BF_BOTTOMRIGHT:
            /* Left top endpoint */
            epx = rc.left-1;
            spx = epx + SmallDiam;
            epy = rc.top-1;
            spy = epy + SmallDiam;
            break;

        case BF_TOP:
        case BF_RIGHT:
        case BF_TOPRIGHT:
        case BF_RIGHT|BF_LEFT:
        case BF_RIGHT|BF_LEFT|BF_TOP:
        case BF_BOTTOM|BF_TOP:
        case BF_BOTTOM|BF_TOP|BF_LEFT:
        case BF_BOTTOMRIGHT|BF_LEFT:
        case BF_BOTTOMRIGHT|BF_TOP:
        case BF_RECT:
            /* Right top endpoint */
            spx = rc.left;
            epx = spx + SmallDiam;
            spy = rc.bottom-1;
            epy = spy - SmallDiam;
            break;
        }

        PaintingGDI.MoveToEx(hdc, spx, spy, NULL);
        WinDC.SelectObject(hdc, OuterPen);
        PaintingGDI.LineTo(hdc, epx, epy);

        WinDC.SelectObject(hdc, InnerPen);

        switch(uFlags & (BF_RECT|BF_DIAGONAL))
        {
        case BF_DIAGONAL_ENDBOTTOMLEFT:
        case (BF_DIAGONAL|BF_BOTTOM):
        case BF_DIAGONAL:
        case (BF_DIAGONAL|BF_LEFT):
            PaintingGDI.MoveToEx(hdc, spx - 1, spy, NULL);
            PaintingGDI.LineTo(hdc, epx, epy - 1);
            Points[0].x = spx-add;
            Points[0].y = spy;
            Points[1].x = rc.left;
            Points[1].y = rc.top;
            Points[2].x = epx+1;
            Points[2].y = epy-1-add;
            Points[3] = Points[2];
            break;

        case BF_DIAGONAL_ENDBOTTOMRIGHT:
            PaintingGDI.MoveToEx(hdc, spx - 1, spy, NULL);
            PaintingGDI.LineTo(hdc, epx, epy + 1);
            Points[0].x = spx-add;
            Points[0].y = spy;
            Points[1].x = rc.left;
            Points[1].y = rc.bottom-1;
            Points[2].x = epx+1;
            Points[2].y = epy+1+add;
            Points[3] = Points[2];
            break;

        case (BF_DIAGONAL|BF_BOTTOM|BF_RIGHT|BF_TOP):
        case (BF_DIAGONAL|BF_BOTTOM|BF_RIGHT|BF_TOP|BF_LEFT):
        case BF_DIAGONAL_ENDTOPRIGHT:
        case (BF_DIAGONAL|BF_RIGHT|BF_TOP|BF_LEFT):
            PaintingGDI.MoveToEx(hdc, spx + 1, spy, NULL);
            PaintingGDI.LineTo(hdc, epx, epy + 1);
            Points[0].x = epx-1;
            Points[0].y = epy+1+add;
            Points[1].x = rc.right-1;
            Points[1].y = rc.top+add;
            Points[2].x = rc.right-1;
            Points[2].y = rc.bottom-1;
            Points[3].x = spx+add;
            Points[3].y = spy;
            break;

        case BF_DIAGONAL_ENDTOPLEFT:
            PaintingGDI.MoveToEx(hdc, spx, spy - 1, NULL);
            PaintingGDI.LineTo(hdc, epx + 1, epy);
            Points[0].x = epx+1+add;
            Points[0].y = epy+1;
            Points[1].x = rc.right-1;
            Points[1].y = rc.top;
            Points[2].x = rc.right-1;
            Points[2].y = rc.bottom-1-add;
            Points[3].x = spx;
            Points[3].y = spy-add;
            break;

        case (BF_DIAGONAL|BF_TOP):
        case (BF_DIAGONAL|BF_BOTTOM|BF_TOP):
        case (BF_DIAGONAL|BF_BOTTOM|BF_TOP|BF_LEFT):
            PaintingGDI.MoveToEx(hdc, spx + 1, spy - 1, NULL);
            PaintingGDI.LineTo(hdc, epx, epy);
            Points[0].x = epx-1;
            Points[0].y = epy+1;
            Points[1].x = rc.right-1;
            Points[1].y = rc.top;
            Points[2].x = rc.right-1;
            Points[2].y = rc.bottom-1-add;
            Points[3].x = spx+add;
            Points[3].y = spy-add;
            break;

        case (BF_DIAGONAL|BF_RIGHT):
        case (BF_DIAGONAL|BF_RIGHT|BF_LEFT):
        case (BF_DIAGONAL|BF_RIGHT|BF_LEFT|BF_BOTTOM):
            PaintingGDI.MoveToEx(hdc, spx, spy, NULL);
            PaintingGDI.LineTo(hdc, epx - 1, epy + 1);
            Points[0].x = spx;
            Points[0].y = spy;
            Points[1].x = rc.left;
            Points[1].y = rc.top+add;
            Points[2].x = epx-1-add;
            Points[2].y = epy+1+add;
            Points[3] = Points[2];
            break;
        }

        /* Fill the interior if asked */
        if((uFlags & BF_MIDDLE)!=0 && retval!=0) {
            int  hb = SysParams.GetSysColorBrush((uFlags & BF_MONO) != 0 ? COLOR_WINDOW : COLOR_BTNFACE);
            int hp = SysParams.GetSysColorPen((uFlags & BF_MONO)!=0 ? COLOR_WINDOW : COLOR_BTNFACE);
            int hbsave = WinDC.SelectObject(hdc, hb);
            int hpsave = WinDC.SelectObject(hdc, hp);
            int p = getTempBuffer(WinPoint.SIZE*4);
            for (int i=0;i<Points.length;i++)
                Points[i].write(p+WinPoint.SIZE*i);
            PaintingGDI.Polygon(hdc, p, 4);
            WinDC.SelectObject(hdc, hbsave);
            WinDC.SelectObject(hdc, hpsave);
        }

        /* Adjust rectangle if asked */
        if((uFlags & BF_ADJUST)!=0) {
            if ((uFlags & BF_LEFT)!=0)   rc.left   += add;
            if ((uFlags & BF_RIGHT)!=0)  rc.right  -= add;
            if ((uFlags & BF_TOP)!=0)    rc.top    += add;
            if ((uFlags & BF_BOTTOM)!=0) rc.bottom -= add;
            rc.write(prc);
        }

        /* Cleanup */
        WinDC.SelectObject(hdc, SavePen);
        WinPoint p = new WinPoint(SavePoint);
        PaintingGDI.MoveToEx(hdc, p.x, p.y, NULL);

        return retval;
    }

    /***********************************************************************
     *           UITOOLS_DrawRectEdge
     *
     * Same as DrawEdge invoked without BF_DIAGONAL
     *
     * 23-Nov-1997: Changed by Bertho Stultiens
     *
     * Well, I started testing this and found out that there are a few things
     * that weren't quite as win95. The following rewrite should reproduce
     * win95 results completely.
     * The colorselection is table-driven to avoid awful if-statements.
     * The table below show the color settings.
     *
     * Pen selection table for uFlags = 0
     *
     * uType |  LTI  |  LTO  |  RBI  |  RBO
     * ------+-------+-------+-------+-------
     *  0000 |   x   |   x   |   x   |   x
     *  0001 |   x   |  22   |   x   |  21
     *  0010 |   x   |  16   |   x   |  20
     *  0011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     *  0100 |   x   |  20   |   x   |  16
     *  0101 |  20   |  22   |  16   |  21
     *  0110 |  20   |  16   |  16   |  20
     *  0111 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     *  1000 |   x   |  21   |   x   |  22
     *  1001 |  21   |  22   |  22   |  21
     *  1010 |  21   |  16   |  22   |  20
     *  1011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     *  1100 |   x   |   x   |   x   |   x
     *  1101 |   x   | x (22)|   x   | x (21)
     *  1110 |   x   | x (16)|   x   | x (20)
     *  1111 |   x   |   x   |   x   |   x
     *
     * Pen selection table for uFlags = BF_SOFT
     *
     * uType |  LTI  |  LTO  |  RBI  |  RBO
     * ------+-------+-------+-------+-------
     *  0000 |   x   |   x   |   x   |   x
     *  0001 |   x   |  20   |   x   |  21
     *  0010 |   x   |  21   |   x   |  20
     *  0011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     *  0100 |   x   |  22   |   x   |  16
     *  0101 |  22   |  20   |  16   |  21
     *  0110 |  22   |  21   |  16   |  20
     *  0111 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     *  1000 |   x   |  16   |   x   |  22
     *  1001 |  16   |  20   |  22   |  21
     *  1010 |  16   |  21   |  22   |  20
     *  1011 |   x   |   x   |   x   |   x
     * ------+-------+-------+-------+-------
     *  1100 |   x   |   x   |   x   |   x
     *  1101 |   x   | x (20)|   x   | x (21)
     *  1110 |   x   | x (21)|   x   | x (20)
     *  1111 |   x   |   x   |   x   |   x
     *
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
        int retval = BOOL(!(   ((uType & BDR_INNER) == BDR_INNER || (uType & BDR_OUTER) == BDR_OUTER) && (uFlags & (BF_FLAT|BF_MONO))==0 ));

        /* Init some vars */
        LTInnerPen = LTOuterPen = RBInnerPen = RBOuterPen = GdiObj.GetStockObject(NULL_PEN);
        int SavePen = WinDC.SelectObject(hdc, LTInnerPen);

        /* Determine the colors of the edges */
        if ((uFlags & BF_MONO)!=0) {
            LTInnerI = RBInnerI = LTRBInnerMono[uType & (BDR_INNER|BDR_OUTER)];
            LTOuterI = RBOuterI = LTRBOuterMono[uType & (BDR_INNER|BDR_OUTER)];
        }
        else if((uFlags & BF_FLAT)!=0)
        {
            LTInnerI = RBInnerI = LTRBInnerFlat[uType & (BDR_INNER|BDR_OUTER)];
            LTOuterI = RBOuterI = LTRBOuterFlat[uType & (BDR_INNER|BDR_OUTER)];

        if( LTInnerI != -1 ) LTInnerI = RBInnerI = COLOR_BTNFACE;
        }
        else if((uFlags & BF_SOFT)!=0)
        {
            LTInnerI = LTInnerSoft[uType & (BDR_INNER|BDR_OUTER)];
            LTOuterI = LTOuterSoft[uType & (BDR_INNER|BDR_OUTER)];
            RBInnerI = RBInnerSoft[uType & (BDR_INNER|BDR_OUTER)];
            RBOuterI = RBOuterSoft[uType & (BDR_INNER|BDR_OUTER)];
        }
        else
        {
            LTInnerI = LTInnerNormal[uType & (BDR_INNER|BDR_OUTER)];
            LTOuterI = LTOuterNormal[uType & (BDR_INNER|BDR_OUTER)];
            RBInnerI = RBInnerNormal[uType & (BDR_INNER|BDR_OUTER)];
            RBOuterI = RBOuterNormal[uType & (BDR_INNER|BDR_OUTER)];
        }

        if((uFlags & BF_BOTTOMLEFT) == BF_BOTTOMLEFT)   LBpenplus = 1;
        if((uFlags & BF_TOPRIGHT) == BF_TOPRIGHT)       RTpenplus = 1;
        if((uFlags & BF_BOTTOMRIGHT) == BF_BOTTOMRIGHT) RBpenplus = 1;
        if((uFlags & BF_TOPLEFT) == BF_TOPLEFT)         LTpenplus = 1;

        if(LTInnerI != -1) LTInnerPen = SysParams.GetSysColorPen(LTInnerI);
        if(LTOuterI != -1) LTOuterPen = SysParams.GetSysColorPen(LTOuterI);
        if(RBInnerI != -1) RBInnerPen = SysParams.GetSysColorPen(RBInnerI);
        if(RBOuterI != -1) RBOuterPen = SysParams.GetSysColorPen(RBOuterI);

        int SavePoint = getTempBuffer(WinPoint.SIZE);
        PaintingGDI.MoveToEx(hdc, 0, 0, SavePoint);

        /* Draw the outer edge */
        WinRect InnerRect = new WinRect(rc);
        WinDC.SelectObject(hdc, LTOuterPen);
        if ((uFlags & BF_TOP)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.left, InnerRect.top, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right, InnerRect.top);
        }
        if ((uFlags & BF_LEFT)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.left, InnerRect.top, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.left, InnerRect.bottom);
        }
        WinDC.SelectObject(hdc, RBOuterPen);
        if ((uFlags & BF_BOTTOM)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.left, InnerRect.bottom - 1, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 1, InnerRect.bottom - 1);
        }
        if ((uFlags & BF_RIGHT)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.right - 1, InnerRect.top, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 1, InnerRect.bottom);
        }

        /* Draw the inner edge */
        WinDC.SelectObject(hdc, LTInnerPen);
        if ((uFlags & BF_TOP)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.left + LTpenplus, InnerRect.top + 1, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - RTpenplus, InnerRect.top + 1);
        }
        if ((uFlags & BF_LEFT)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.left + 1, InnerRect.top + LTpenplus, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.left + 1, InnerRect.bottom - LBpenplus);
        }
        WinDC.SelectObject(hdc, RBInnerPen);
        if ((uFlags & BF_BOTTOM)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.left + LBpenplus, InnerRect.bottom - 2, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 1 - RBpenplus, InnerRect.bottom - 2);
        }
        if ((uFlags & BF_RIGHT)!=0)
        {
            PaintingGDI.MoveToEx(hdc, InnerRect.right - 2, InnerRect.top + 2 - RBpenplus, NULL);
            PaintingGDI.LineTo(hdc, InnerRect.right - 2, InnerRect.bottom - 2 + RTpenplus);
        }

        if( ((uFlags & BF_MIDDLE)!=0 && retval!=0) || (uFlags & BF_ADJUST)!=0 )
        {
            int add = (LTRBInnerMono[uType & (BDR_INNER|BDR_OUTER)] != -1 ? 1 : 0)
                    + (LTRBOuterMono[uType & (BDR_INNER|BDR_OUTER)] != -1 ? 1 : 0);

            if ((uFlags & BF_LEFT)!=0)   InnerRect.left   += add;
            if ((uFlags & BF_RIGHT)!=0)  InnerRect.right  -= add;
            if ((uFlags & BF_TOP)!=0)    InnerRect.top    += add;
            if ((uFlags & BF_BOTTOM)!=0) InnerRect.bottom -= add;

            if((uFlags & BF_MIDDLE)!=0 && retval!=0) {
                WinDC.FillRect(hdc, InnerRect.allocTemp(), SysParams.GetSysColorBrush((uFlags & BF_MONO) != 0 ? COLOR_WINDOW : COLOR_BTNFACE));
            }

        if ((uFlags & BF_ADJUST)!=0)
            InnerRect.write(rc);
        }

        /* Cleanup */
        WinDC.SelectObject(hdc, SavePen);
        WinPoint p = new WinPoint(SavePoint);
        PaintingGDI.MoveToEx(hdc, p.x, p.y, NULL);
        return retval;
    }
}
