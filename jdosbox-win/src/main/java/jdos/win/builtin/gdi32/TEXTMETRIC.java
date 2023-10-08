package jdos.win.builtin.gdi32;

import jdos.win.builtin.WinAPI;

public class TEXTMETRIC extends WinAPI {
    public static final int SIZE = 53;

    public TEXTMETRIC(int address) {
        tmHeight = readd(address);address+=4;
        tmAscent = readd(address);address+=4;
        tmDescent = readd(address);address+=4;
        tmInternalLeading = readd(address);address+=4;
        tmExternalLeading = readd(address);address+=4;
        tmAveCharWidth = readd(address);address+=4;
        tmMaxCharWidth = readd(address);address+=4;
        tmWeight = readd(address);address+=4;
        tmOverhang = readd(address);address+=4;
        tmDigitizedAspectX = readd(address);address+=4;
        tmDigitizedAspectY = readd(address);address+=4;
        tmFirstChar = readb(address);address+=1;
        tmLastChar = readb(address);address+=1;
        tmDefaultChar = readb(address);address+=1;
        tmBreakChar = readb(address);address+=1;
        tmItalic = readb(address);address+=1;
        tmUnderlined = readb(address);address+=1;
        tmStruckOut = readb(address);address+=1;
        tmPitchAndFamily = readb(address);address+=1;
        tmCharSet = readb(address);
    }

    public final int tmHeight;
    public final int tmAscent;
    public final int tmDescent;
    public final int tmInternalLeading;
    public final int tmExternalLeading;
    public final int tmAveCharWidth;
    public final int tmMaxCharWidth;
    public final int tmWeight;
    public final int tmOverhang;
    public final int tmDigitizedAspectX;
    public final int tmDigitizedAspectY;
    public final int tmFirstChar;
    public final int tmLastChar;
    public final int tmDefaultChar;
    public final int tmBreakChar;
    public final int tmItalic;
    public final int tmUnderlined;
    public final int tmStruckOut;
    public final int tmPitchAndFamily;
    public final int tmCharSet;
}
