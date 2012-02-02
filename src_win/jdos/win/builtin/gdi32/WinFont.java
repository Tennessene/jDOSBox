package jdos.win.builtin.gdi32;

import jdos.win.system.WinObject;
import jdos.win.system.WinSize;
import jdos.win.utils.StringUtil;

import java.awt.*;

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
        if(lptm!=0 && WinDC.GetTextMetricsA(hdc, lptm)==0) return null;

        WinSize size = WinDC.GetTextExtentPoint(hdc, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (size == null)
            return null;
        size.cx = (size.cx / 26 + 1) / 2;
        return size;
    }
}
