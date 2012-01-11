package jdos.win.utils;

import jdos.win.builtin.WinAPI;

public class WinMenu extends WinObject {
    public WinMenu(int id) {
        super(id);
    }

    public int getItemCount() {
        return 0;
    }

    public int append(int uFlags, int uIDNewItem, int lpNewItem) {
        return WinAPI.TRUE;
    }

    public int setItemInfo(int uItem, int fByPosition, int lpmii) {
        return WinAPI.TRUE;
    }
}
