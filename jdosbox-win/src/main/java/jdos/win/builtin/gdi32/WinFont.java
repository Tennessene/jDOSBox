package jdos.win.builtin.gdi32;

import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinObject;
import jdos.win.system.WinSize;
import jdos.win.utils.StringUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.Arrays;

public class WinFont extends WinGDI {
    static public WinFont create(Font font) {
        return new WinFont(nextObjectId(), font);
    }

    static public WinFont get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinFont))
            return null;
        return (WinFont)object;
    }

    // HFONT CreateFont(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, DWORD fdwItalic, DWORD fdwUnderline, DWORD fdwStrikeOut, DWORD fdwCharSet, DWORD fdwOutputPrecision, DWORD fdwClipPrecision, DWORD fdwQuality, DWORD fdwPitchAndFamily, LPCTSTR lpszFace)
    public static int CreateFontA(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, int fdwItalic, int fdwUnderline, int fdwStrikeOut, int fdwCharSet, int fdwOutputPrecision, int fdwClipPrecision, int fdwQuality, int fdwPitchAndFamily, int lpszFace) {
        String fontName = null;

        if (lpszFace != 0) {
            fontName = StringUtil.getString(lpszFace);
        }
        return CreateFont(nHeight, nWidth, nEscapement, nOrientation, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwCharSet, fdwOutputPrecision, fdwClipPrecision, fdwQuality, fdwPitchAndFamily, fontName);
    }
    public static int CreateFontW(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, int fdwItalic, int fdwUnderline, int fdwStrikeOut, int fdwCharSet, int fdwOutputPrecision, int fdwClipPrecision, int fdwQuality, int fdwPitchAndFamily, int lpszFace) {
        String fontName = null;

        if (lpszFace != 0) {
            fontName = StringUtil.getStringW(lpszFace);
        }
        return CreateFont(nHeight, nWidth, nEscapement, nOrientation, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwCharSet, fdwOutputPrecision, fdwClipPrecision, fdwQuality, fdwPitchAndFamily, fontName);
    }

    public static int CreateFont(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, int fdwItalic, int fdwUnderline, int fdwStrikeOut, int fdwCharSet, int fdwOutputPrecision, int fdwClipPrecision, int fdwQuality, int fdwPitchAndFamily, String fontName) {
        int style = Font.PLAIN;
        if (fnWeight >= 600) // FW_SEMIBOLD
            style |= Font.BOLD;
        if (fdwItalic != 0)
            style |= Font.ITALIC;
        if (fdwUnderline != 0)
            System.out.println("Underline fonts not supported yet");
        if (fdwStrikeOut != 0)
            System.out.println("Strikeout fonts not supported yet");

        int size = 12;
        if (nHeight != 0)
            size = Math.abs(nHeight);
        size = WinFont.WIN_TO_JAVA(size);
        Font font = null;
        if (fontName != null) {
            font = new Font(fontName, style, size);
        }
        if (font == null) {
            if (fdwPitchAndFamily == 1) { // FIXED_PITCH
                fontName = "Monospaced";
            } else {
                fontName = null;
            }
            font = new Font(fontName, style, size);
        }
        return WinFont.create(font).getHandle();
    }

    // HFONT CreateFontIndirect(const LOGFONT *lplf)
    public static int CreateFontIndirectA(int lplf) {
        int nHeight = readd(lplf);lplf+=4;
        int nWidth = readd(lplf);lplf+=4;
        int nEscapement = readd(lplf);lplf+=4;
        int nOrientation = readd(lplf);lplf+=4;
        int fnWeight = readd(lplf);lplf+=4;
        int fdwItalic = readb(lplf);lplf+=1;
        int fdwUnderline = readb(lplf);lplf+=1;
        int fdwStrikeOut = readb(lplf);lplf+=1;
        int fdwCharSet = readb(lplf);lplf+=1;
        int fdwOutputPrecision = readb(lplf);lplf+=1;
        int fdwClipPrecision = readb(lplf);lplf+=1;
        int fdwQuality = readb(lplf);lplf+=1;
        int fdwPitchAndFamily = readb(lplf);lplf+=1;
        int lpszFace = lplf;
        return CreateFontA(nHeight, nWidth, nEscapement, nOrientation, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwCharSet, fdwOutputPrecision, fdwClipPrecision, fdwQuality, fdwPitchAndFamily, lpszFace);
    }

    // BOOL GetTextExtentExPoint(HDC hdc, LPCTSTR lpszStr, int cchString, int nMaxExtent, LPINT lpnFit, LPINT alpDx, LPSIZE lpSize)
    public static int GetTextExtentExPointA(int hdc, int lpszStr, int cchString, int nMaxExtent, int lpnFit, int alpDx, int lpSize) {
        String text = StringUtil.getString(lpszStr, cchString);
        WinSize size = GetTextExtentExPoint(hdc, text, nMaxExtent, lpnFit, alpDx);
        if (size == null)
            return FALSE;
        Memory.mem_writed(lpSize, size.cx);
        Memory.mem_writed(lpSize+4, size.cy);
        return TRUE;
    }
    public static WinSize GetTextExtentExPoint(int hdc, String text, int nMaxExtent, int lpnFit, int alpDx) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return null;
        Graphics2D g = dc.getGraphics();
        FontRenderContext frc = g.getFontRenderContext();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);

        if (alpDx != 0 || lpnFit != 0) {
            int[] dx = new int[text.length()];
            for (int i=0;i<text.length();i++) {
                dx[i] = (int)(font.getStringBounds(text.substring(0, i), frc).getWidth()+0.95);
            }

            if (alpDx != 0) {
                for (int i=0;i<text.length();i++) {
                    writed(alpDx+i*4, dx[i]);
                }
            }
            if (lpnFit != 0) {
                writed(lpnFit, text.length());
                for (int i=0;i<dx.length;i++) {
                    if (dx[i]>nMaxExtent) {
                        writed(lpnFit, i);
                    }
                }
            }
        }
        int sw = (int)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        int sh = (int)(lm.getAscent() + lm.getDescent());
        g.dispose();
        return new WinSize(sw, sh);
    }

    // BOOL GetTextExtentPoint32(HDC hdc, LPCTSTR lpString, int c, LPSIZE lpSize)
    static public int GetTextExtentPoint32A(int hdc, int lpString, int cbString, int lpSize) {
        return GetTextExtentPointA(hdc, lpString, cbString, lpSize);
    }
    static public int GetTextExtentPoint32W(int hdc, int lpString, int cbString, int lpSize) {
        return GetTextExtentPointW(hdc, lpString, cbString, lpSize);
    }

    // BOOL GetTextExtentPoint(HDC hdc, LPCTSTR lpString, int cbString, LPSIZE lpSize)
    static public int GetTextExtentPointA(int hdc, int lpString, int cbString, int lpSize) {
        String text = StringUtil.getString(lpString, cbString);
        WinSize size = GetTextExtentPoint(hdc, text);
        if (size == null)
            return FALSE;
        Memory.mem_writed(lpSize, size.cx);
        Memory.mem_writed(lpSize+4, size.cy);
        return TRUE;
    }
    static public int GetTextExtentPointW(int hdc, int lpString, int cbString, int lpSize) {
        String text = StringUtil.getStringW(lpString, cbString);
        WinSize size = GetTextExtentPoint(hdc, text);
        if (size == null)
            return FALSE;
        Memory.mem_writed(lpSize, size.cx);
        Memory.mem_writed(lpSize+4, size.cy);
        return TRUE;
    }

    static public WinSize GetTextExtentPoint(int hdc, String text) {
        return GetTextExtentExPoint(hdc, text, 0, NULL, NULL);
    }

    // BOOL GetTextMetrics(HDC hdc, LPTEXTMETRIC lptm)
    static public int GetTextMetricsA(int hdc, int lptm) {
        WinDC dc = WinDC.get(hdc);
        if (dc == null)
            return FALSE;
        Graphics2D g = dc.getGraphics();
        Font font = WinFont.get(dc.hFont).font;
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.dispose();
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(font.getSize()));lptm+=4; // tmHeight
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(metrics.getAscent()));lptm+=4; // tmAscent
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(metrics.getDescent()));lptm+=4; // tmDescent
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(metrics.getLeading()));lptm+=4; // tmInternalLeading
        Memory.mem_writed(lptm, 0);lptm+=4; // tmExternalLeading
        int[] width = metrics.getWidths();
        Arrays.sort(width);
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(width[200])/2);lptm+=4; // tmAveCharWidth
        Memory.mem_writed(lptm, WinFont.JAVA_TO_WIN(width[255]));lptm+=4; // tmMaxCharWidth
        Memory.mem_writed(lptm, font.isBold()?700:400);lptm+=4; // tmWeight FW_NORMAL=400 FW_BOLD=700
        Memory.mem_writed(lptm, 0);lptm+=4; // tmOverhang
        Memory.mem_writed(lptm, 96);lptm+=4; // tmDigitizedAspectX
        Memory.mem_writed(lptm, 96);lptm+=4; // tmDigitizedAspectY
        Memory.mem_writeb(lptm, 32);lptm+=1; // tmFirstChar
        Memory.mem_writeb(lptm, 256);lptm+=1; // tmLastChar
        Memory.mem_writeb(lptm, 32);lptm+=1; // tmDefaultChar
        Memory.mem_writeb(lptm, 32);lptm+=1; // tmBreakChar
        Memory.mem_writeb(lptm, font.isItalic() ? 1 : 0);lptm+=1; // tmItalic
        Memory.mem_writeb(lptm, 0);lptm+=1; // tmUnderlined
        Memory.mem_writeb(lptm, 0);lptm+=1; // tmStruckOut
        Memory.mem_writeb(lptm, 0x06);lptm+=1; // tmPitchAndFamily TMPF_FIXED_PITCH=0x01 TMPF_VECTOR=0x02 TMPF_DEVICE=0x08 TMPF_TRUETYPE=0x04
        Memory.mem_writeb(lptm, 0);lptm+=1; // tmCharSet 0=ANSI_CHARSET
        return WinAPI.TRUE;
    }

    public Font font;

    public WinFont(int id, Font font) {
        super(id);
        this.font = font;
    }

    public String toString() {
        return "FONT "+font.getFontName()+" "+font.getSize()+"pt";
    }

    static public int JAVA_TO_WIN(int size) {
        return size * 9 / 10;// * 96 / 72;
    }

    static public int WIN_TO_JAVA(int size) {
        return size * 10 / 9;// * 72 / 96;
    }

    /***********************************************************************
     *           GdiGetCharDimensions    (GDI32.@)
     *
     * Gets the average width of the characters in the English alphabet.
     *
     * PARAMS
     *  hdc    [I] Handle to the device context to measure on.
     *
     * RETURNS
     *  The average width of characters in the English alphabet.
     *
     * NOTES
     *  This function is used by the dialog manager to get the size of a dialog
     *  unit. It should also be used by other pieces of code that need to know
     *  the size of a dialog unit in logical units without having access to the
     *  window handle of the dialog.
     *  Windows caches the font metrics from this function, but we don't and
     *  there doesn't appear to be an immediate advantage to do so.
     *
     * SEE ALSO
     *  GetTextExtentPointW, GetTextMetricsW, MapDialogRect.
     */
    static public WinSize GdiGetCharDimensions(int hdc, int lptm) {
        if(lptm!=0 && GetTextMetricsA(hdc, lptm)==0) return null;

        WinSize size = GetTextExtentPoint(hdc, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (size == null)
            return null;
        size.cx = (size.cx / 26 + 1) / 2;
        return size;
    }
}
