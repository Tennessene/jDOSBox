package jdos.win.builtin.gdi32;

import jdos.hardware.Memory;
import jdos.win.system.WinObject;
import jdos.win.utils.Ptr;

public class WinPalette extends WinGDI {
    static public WinPalette create(int[] palette) {
        return new WinPalette(nextObjectId(), palette);
    }

    static public WinPalette get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinPalette))
            return null;
        return (WinPalette)object;
    }

    // HPALETTE CreatePalette(const LOGPALETTE *lplgpl)
    static public int CreatePalette(int lplgpl) {
        if (lplgpl == 0)
            return 0;
        int count = readw(lplgpl + 2);
        int[] palette = new int[count];
        for (int i=0;i<count;i++) {
            int address = lplgpl+4+4*i;
            palette[i] = readd(address) & 0xFFFFFF; // strip out the flag
        }
        return create(palette).handle;
    }

    // UINT GetPaletteEntries(HPALETTE hpal, UINT iStartIndex, UINT nEntries, LPPALETTEENTRY lppe)
    static public int GetPaletteEntries(int hpal, int iStartIndex, int nEntries, int lppe) {
        WinPalette palette = WinPalette.get(hpal);
        if (palette == null)
            return 0;

        if (lppe != 0) {
            for (int i=iStartIndex;i<iStartIndex+nEntries;i++, lppe+=4)
                writed(lppe, palette.palette[i]);
            return nEntries;
        }
        return palette.palette.length;
    }

    // UINT GetSystemPaletteUse(HDC hdc)
    static public int GetSystemPaletteUse(int hdc) {
        return SYSPAL_NOSTATIC;
    }

    // BOOL ResizePalette(HPALETTE hpal, UINT nEntries)
    static public int ResizePalette(int hpal, int nEntries) {
        WinPalette palette = WinPalette.get(hpal);
        if (palette == null)
            return FALSE;
        if (palette.palette.length==nEntries)
            return TRUE;
        if (palette.palette.length>nEntries) {
            log("ResizePalette faked");
        } else {
            log("ResizePalette faked");
        }
        return TRUE;
    }

    // UINT SetPaletteEntries(HPALETTE hpal, UINT iStart, UINT cEntries, const PALETTEENTRY *lppe)
    static public int SetPaletteEntries(int hpal, int iStart, int cEntries, int lppe) {
        WinPalette palette = WinPalette.get(hpal);
        if (palette == null)
            return 0;
        for (int i=iStart;i<iStart+cEntries;i++)
            palette.palette[i] = 0xFF000000|readd(lppe + i * 4);
        return cEntries;
    }

    // UINT SetSystemPaletteUse(HDC hdc, UINT uUsage)
    static public int SetSystemPaletteUse(int hdc, int uUsage) {
        return SYSPAL_NOSTATIC;
    }

    int[] palette;

    public WinPalette(int handle, int[] palette) {
        super(handle);
        this.palette = palette;
    }

    public int setEntries(int start, int count, int address) {
        for (int i=start;i<start+count;i++)
            palette[i] = Memory.mem_readd(address+i*4);
        return count;
    }

    public String toString() {
        return "PALETTE size="+palette.length+" 0=0x"+Ptr.toString(palette[0])+" 1=0x"+Ptr.toString(palette[1]);
    }
}
