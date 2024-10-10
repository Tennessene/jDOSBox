package jdos.win.builtin.gdi32;

import jdos.win.system.WinObject;
import jdos.win.utils.Pixel;
import jdos.win.utils.Ptr;

import java.awt.*;

public class WinPen extends WinGDI {
    static public WinPen create(int style, int width, int color) {
        return WinPen.get(CreatePen(style, width, color));
    }

    static public WinPen get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinPen))
            return null;
        return (WinPen) object;
    }

    // HPEN CreatePen(int fnPenStyle, int nWidth, COLORREF crColor)
    static public int CreatePen(int fnPenStyle, int nWidth, int crColor) {
        if (fnPenStyle == PS_NULL) {
            int hpen = GdiObj.GetStockObject(NULL_PEN);
            if (hpen != 0) return hpen;
        }

        WinPen penPtr = new WinPen(nextObjectId());

        if ((fnPenStyle & PS_JOIN_MASK) == 0)
            fnPenStyle|=PS_JOIN_MITER;
        if ((fnPenStyle & PS_ENDCAP_MASK) == 0)
            fnPenStyle|=PS_ENDCAP_FLAT;

        if (fnPenStyle == PS_USERSTYLE || fnPenStyle == PS_ALTERNATE)
            penPtr.logpen.elpPenStyle = PS_SOLID;
        else
            penPtr.logpen.elpPenStyle = fnPenStyle;
        if (fnPenStyle == PS_NULL) {
            penPtr.logpen.elpWidth = 1;
            penPtr.logpen.elpColor = RGB(0, 0, 0);
        } else {
            penPtr.logpen.elpWidth = Math.abs(nWidth);
            penPtr.logpen.elpColor = crColor;
        }
        penPtr.logpen.elpBrushStyle = BS_SOLID;
        penPtr.logpen.elpHatch = 0;
        penPtr.logpen.elpNumEntries = 0;
        penPtr.logpen.elpStyleEntry = new int[0];
        return penPtr.handle;
    }

    // HPEN ExtCreatePen(DWORD dwPenStyle,DWORD dwWidth, const LOGBRUSH *lplb, DWORD dwStyleCount, const DWORD *lpStyle)
    static public int ExtCreatePen(int style, int width, int lplb, int style_count, int lpStyle) {
        int hpen;

        if ((style & PS_STYLE_MASK) == PS_USERSTYLE) {
            if (style_count <= 0)
                return 0;

            if ((style_count > 16) || lpStyle == 0) {
                SetLastError(ERROR_INVALID_PARAMETER);
                return 0;
            }

            if ((style & PS_TYPE_MASK) == PS_GEOMETRIC) {
                int i;
                boolean has_neg = false, all_zero = true;

                for (i = 0; (i < style_count) && !has_neg; i++) {
                    int s = readd(lpStyle + i * 4);
                    has_neg = has_neg || (s < 0);
                    all_zero = all_zero && (s == 0);
                }

                if (all_zero || has_neg) {
                    SetLastError(ERROR_INVALID_PARAMETER);
                    return 0;
                }
            }
        } else {
            if (style_count != 0 || lpStyle != 0) {
                SetLastError(ERROR_INVALID_PARAMETER);
                return 0;
            }
        }

        LOGBRUSH brush = new LOGBRUSH(lplb);
        if ((style & PS_STYLE_MASK) == PS_NULL)
            return CreatePen(PS_NULL, 0, brush.lbColor);

        if ((style & PS_TYPE_MASK) == PS_GEOMETRIC) {
            /* PS_ALTERNATE is applicable only for cosmetic pens */
            if ((style & PS_STYLE_MASK) == PS_ALTERNATE) {
                SetLastError(ERROR_INVALID_PARAMETER);
                return 0;
            }

            if (brush.lbHatch != 0 && ((brush.lbStyle != BS_SOLID) && (brush.lbStyle != BS_HOLLOW))) {
                warn("ExtCreatePen Hatches not implemented");
            }
        } else {
            /* PS_INSIDEFRAME is applicable only for geometric pens */
            if ((style & PS_STYLE_MASK) == PS_INSIDEFRAME || width != 1) {
                SetLastError(ERROR_INVALID_PARAMETER);
                return 0;
            }
        }

        WinPen penPtr = new WinPen(nextObjectId());

        penPtr.logpen.elpPenStyle = style;
        penPtr.logpen.elpWidth = Math.abs(width);
        penPtr.logpen.elpBrushStyle = brush.lbStyle;
        penPtr.logpen.elpColor = brush.lbColor;
        penPtr.logpen.elpHatch = brush.lbHatch;
        penPtr.logpen.elpNumEntries = style_count;
        penPtr.logpen.elpStyleEntry = new int[style_count];
        for (int i = 0; i < style_count; i++)
            penPtr.logpen.elpStyleEntry[i] = readd(lpStyle + i * 4);
        return penPtr.handle;
    }

    public EXTLOGPEN logpen = new EXTLOGPEN();

    public WinPen(int id) {
        super(id);
    }

    public boolean setStroke(WinDC dc, Graphics2D g) {
        if ((logpen.elpPenStyle & PS_STYLE_MASK) == PS_NULL) {
            return false;
        }
        g.setPaint(new Color(0xFF000000 | Pixel.BGRtoRGB(logpen.elpColor)));
        int cap = 0;
        int join = 0;

        float[] dash;
        float dashSize=3.0f*logpen.elpWidth;
        float dotOnSize=1.0f*logpen.elpWidth;
        float dotOffSize=1.0f*logpen.elpWidth;

        if ((logpen.elpPenStyle & PS_ENDCAP_SQUARE)!=0) {
            cap = BasicStroke.CAP_SQUARE;
            dashSize-=logpen.elpWidth;
            dotOffSize+=logpen.elpWidth;
            dotOnSize-=logpen.elpWidth;
        } else if ((logpen.elpPenStyle & PS_ENDCAP_FLAT)!=0) {
            cap = BasicStroke.CAP_BUTT;
        } else { // PS_ENDCAP_ROUND
            cap = BasicStroke.CAP_ROUND;
            dashSize-=logpen.elpWidth;
            dotOffSize+=logpen.elpWidth;
            dotOnSize-=logpen.elpWidth;
        }

        if ((logpen.elpPenStyle & PS_JOIN_BEVEL)!=0) {
            join = BasicStroke.JOIN_BEVEL;
        } else if ((logpen.elpPenStyle & PS_JOIN_MITER)!=0) {
            join = BasicStroke.JOIN_MITER;
        } else { // PS_JOIN_ROUND
            join = BasicStroke.JOIN_ROUND;
        }

        switch (logpen.elpPenStyle & PS_STYLE_MASK) {
            case PS_DASH:
                dash = new float[] {dashSize, dotOffSize};
                break;
            case PS_DOT:
                dash = new float[] {dotOnSize, dotOffSize};
                break;
            case PS_DASHDOT:
                dash = new float[] {dashSize, dotOffSize, dotOnSize, dotOffSize};
                break;
            case PS_DASHDOTDOT:
                dash = new float[] {dashSize, dotOffSize, dotOnSize, dotOffSize, dotOnSize, dotOffSize};
                break;
            case PS_USERSTYLE:
                dash = new float[logpen.elpNumEntries];
                for (int i=0;i<dash.length;i++)
                    dash[i] = logpen.elpStyleEntry[i];
                break;
            default:
                g.setStroke(new BasicStroke(logpen.elpWidth, cap, join));
                return true;
        }
        g.setStroke(new BasicStroke(logpen.elpWidth, cap, join, dc.miterLimit, dash, 0.0f));
        return true;
    }

    public String toString() {
        return "PEN style=" + logpen.elpPenStyle + " width=" + logpen.elpWidth + " color=0x" + Ptr.toString(logpen.elpColor);
    }
}
