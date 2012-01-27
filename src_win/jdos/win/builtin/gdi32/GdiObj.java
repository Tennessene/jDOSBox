package jdos.win.builtin.gdi32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.StaticData;

public class GdiObj extends WinAPI {
    // BOOL WINAPI DeleteObject( HGDIOBJ obj )
    static public int DeleteObject(int obj) {
        WinGDI gdi = WinGDI.getGDI(obj);
        if (gdi == null)
            return FALSE;
        gdi.close();
        return TRUE;
    }

    // DWORD GdiSetBatchLimit(DWORD dwLimit)
    static public int GdiSetBatchLimit(int dwLimit) {
        return 1;
    }

    // int GetObjectA(HGDIOBJ hgdiobj, int cbBuffer, LPVOID lpvObject)
    static public int GetObjectA(int hgdiobj, int cbBuffer, int lpvObject) {
        WinGDI gdi = WinGDI.getGDI(hgdiobj);
        if (gdi == null)
            return 0;
        if (gdi instanceof WinBitmap) {
            WinBitmap bitmap = (WinBitmap)gdi;
            return bitmap.get(lpvObject, cbBuffer);
        } else {
            Win.panic("GetObject not implemented yet for type " + gdi);
            return 0;
        }
    }

    // HGDIOBJ GetStockObject(int fnObject)
    static public int GetStockObject(int fnObject) {
        if (fnObject >= 0 && fnObject < StaticData.stockObjects.length)
            return StaticData.stockObjects[fnObject];
        return 0;
    }
}
