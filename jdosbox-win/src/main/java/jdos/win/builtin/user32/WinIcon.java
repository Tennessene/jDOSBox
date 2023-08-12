package jdos.win.builtin.user32;

import jdos.win.system.WinObject;

public class WinIcon extends WinObject {
    static public WinIcon create(int instance, int name) {
        WinIcon icon = new WinIcon(nextObjectId());
        return icon;
    }

    static public WinIcon get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinIcon))
            return null;
        return (WinIcon)object;
    }

    // BOOL WINAPI DrawIcon(HDC hDC, int X, int Y, HICON hIcon)
    public static int DrawIcon(int hDC, int X, int Y, int hIcon) {
        return DrawIconEx(hDC, X, Y, hIcon, 0, 0, 0, 0, DI_NORMAL | DI_COMPAT | DI_DEFAULTSIZE );
    }

    // BOOL WINAPI DrawIconEx(HDC hdc, int xLeft, int yTop, HICON hIcon, int cxWidth, int cyWidth, UINT istepIfAniCur, HBRUSH hbrFlickerFreeDraw, UINT diFlags)
    public static int DrawIconEx(int hdc, int xLeft, int yTop, int hIcon, int cxWidth, int cyWidth, int istepIfAniCur, int hbrFlickerFreeDraw, int diFlags) {
        log("DrawIconEx faked");
        return TRUE;
    }

    // HICON WINAPI LoadIcon(HINSTANCE hInstance, LPCTSTR lpIconName)
    public static int LoadIconA(int hInstance, int lpIconName) {
        return create(hInstance, lpIconName).handle;
    }

    public WinIcon(int handle) {
        super(handle);
    }

    public int cx;
    public int cy;
}
