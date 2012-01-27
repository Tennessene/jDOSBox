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

    // HICON WINAPI LoadIcon(HINSTANCE hInstance, LPCTSTR lpIconName)
    public static int LoadIconA(int hInstance, int lpIconName) {
        return create(hInstance, lpIconName).handle;
    }

    public WinIcon(int handle) {
        super(handle);
    }
}
