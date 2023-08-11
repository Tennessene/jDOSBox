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
        tmCharSet = readb(address);address+=1;
    }

    public int tmHeight;
    public int tmAscent;
    public int tmDescent;
    public int tmInternalLeading;
    public int tmExternalLeading;
    public int tmAveCharWidth;
    public int tmMaxCharWidth;
    public int tmWeight;
    public int tmOverhang;
    public int tmDigitizedAspectX;
    public int tmDigitizedAspectY;
    public int tmFirstChar;
    public int tmLastChar;
    public int tmDefaultChar;
    public int tmBreakChar;
    public int tmItalic;
    public int tmUnderlined;
    public int tmStruckOut;
    public int tmPitchAndFamily;
    public int tmCharSet;
}
