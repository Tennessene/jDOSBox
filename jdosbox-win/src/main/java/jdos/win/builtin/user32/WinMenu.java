package jdos.win.builtin.user32;

import jdos.win.system.StaticData;
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

    // DWORD WINAPI CheckMenuItem(HMENU hmenu, UINT uIDCheckItem, UINT uCheck)
    static public int CheckMenuItem(int hmenu, int uIDCheckItem, int uCheck) {
        return 0;
    }

    // HMENU WINAPI CreatePopupMenu(void)
    static public int CreatePopupMenu() {
        return create().handle;
    }

    // BOOL WINAPI DestroyMenu(HMENU hMenu)
    static public int DestroyMenu(int hMenu) {
        WinMenu menu = WinMenu.get(hMenu);
        if (menu == null)
            return FALSE;
        menu.close();
        return TRUE;
    }

    // BOOL WINAPI DrawMenuBar(HWND hWnd)
    static public int DrawMenuBar(int hWnd) {
        return FALSE;
    }

    // BOOL WINAPI EnableMenuItem(HMENU hMenu, UINT uIDEnableItem, UINT uEnable)
    static public int EnableMenuItem(int hMenu, int uIDEnableItem, int uEnable) {
        return FALSE;
    }

    // BOOL WINAPI EndMenu(void)
    static public int EndMenu() {
        return TRUE;
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

    // HMENU WINAPI GetSubMenu(HMENU hMenu, int nPos)
    static public int GetSubMenu(int hMenu, int nPos) {
        return 0;
    }

    // HMENU WINAPI GetSystemMenu(HWND hWnd, BOOL bRevert)
    static public int GetSystemMenu(int hWnd, int bRevert) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        if (bRevert != 0 && window.hSysMenu != 0) {
            DestroyMenu(window.hSysMenu);
            window.hSysMenu = 0;
        }
        if (window.hSysMenu==0 && (window.dwStyle & WS_SYSMENU)!=0) {
            // :TODO: MENU_GetSysMenu
            window.hSysMenu = create().handle;
        }
        int result = 0;
        if (window.hSysMenu != 0) {
            // :TODO: return the submenu
            result = window.hSysMenu;
        }
        return (bRevert==0)?result:0;
    }

    // HMENU WINAPI LoadMenu(HINSTANCE hInstance, LPCTSTR lpMenuName)
    static public int LoadMenuA(int hInstance, int lpMenuName) {
        return 0;
    }
    static public int LoadMenuW(int hInstance, int lpMenuName) {
        return 0;
    }

    // BOOL WINAPI ModifyMenu(HMENU hMnu, UINT uPosition, UINT uFlags, UINT_PTR uIDNewItem, LPCTSTR lpNewItem)
    static public int ModifyMenuA(int hMnu, int uPosition, int uFlags, int uIDNewItem, int lpNewItem) {
        return FALSE;
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

    static public int MENU_IsMenuActive() {
        return StaticData.top_popup;
    }
}
