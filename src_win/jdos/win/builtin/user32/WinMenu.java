package jdos.win.builtin.user32;

import jdos.win.system.WinObject;

public class WinMenu extends WinObject {
    static public WinMenu create() {
        return new WinMenu(nextObjectId());
    }

    static public WinMenu get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinMenu))
            return null;
        return (WinMenu)object;
    }

    // BOOL WINAPI AppendMenu(HMENU hMenu, UINT uFlags, UINT_PTR uIDNewItem, LPCTSTR lpNewItem)
    static public int AppendMenuA(int hMenu, int uFlags, int uIDNewItem, int lpNewItem) {
        return TRUE;
    }

    // HMENU WINAPI CreatePopupMenu(void)
    static public int CreatePopupMenu() {
        return create().handle;
    }

    // HMENU WINAPI GetMenu(HWND hWnd)
    static public int GetMenu(int hWnd) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        return window.wIDmenu;
    }

    // int WINAPI GetMenuItemCount(HMENU hMenu)
    static public int GetMenuItemCount(int hMenu) {
        WinMenu menu = WinMenu.get(hMenu);
        if (menu == null)
            return -1;
        return 0;
    }

    // HMENU WINAPI LoadMenu(HINSTANCE hInstance, LPCTSTR lpMenuName)
    static public int LoadMenuA(int hInstance, int lpMenuName) {
        return 0;
    }

    // BOOL WINAPI SetMenu(HWND hWnd, HMENU hMenu)
    static public int SetMenu(int hWnd, int hMenu) {
        return TRUE;
    }

    // BOOL WINAPI SetMenuItemInfo(HMENU hMenu, UINT uItem, BOOL fByPosition, LPMENUITEMINFO lpmii)
    static public int SetMenuItemInfoA(int hMenu, int uItem, int fByPosition, int lpmii) {
        return TRUE;
    }

    // int WINAPI TranslateAccelerator(HWND hWnd, HACCEL hAccTable, LPMSG lpMsg)
    static public int TranslateAcceleratorA(int hWnd, int hAccTable, int lpMsg) {
        return 0;
    }

    public WinMenu(int id) {
        super(id);
    }
}
