package jdos.win.builtin.gdi32;

import jdos.win.system.WinObject;
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
        return CreateFontA_String(nHeight, nWidth, nEscapement, nOrientation, fnWeight, fdwItalic, fdwUnderline, fdwStrikeOut, fdwCharSet, fdwOutputPrecision, fdwClipPrecision, fdwQuality, fdwPitchAndFamily, fontName);
    }

    public static int CreateFontA_String(int nHeight, int nWidth, int nEscapement, int nOrientation, int fnWeight, int fdwItalic, int fdwUnderline, int fdwStrikeOut, int fdwCharSet, int fdwOutputPrecision, int fdwClipPrecision, int fdwQuality, int fdwPitchAndFamily, String fontName) {
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
        return size;// * 96 / 72;
    }

    static public int WIN_TO_JAVA(int size) {
        return size;// * 72 / 96;
    }
}
