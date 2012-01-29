package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;

import java.util.Iterator;

public class WinDialog extends WinAPI {
    // int WINAPI GetDlgCtrlID(HWND hwndCtl)
    public static int GetDlgCtrlID(int hwndCtl) {
        return WinWindow.GetWindowLongA(hwndCtl, GWL_ID);
    }

    // HWND WINAPI GetNextDlgTabItem( HWND hDlg, HWND hCtl, BOOL bPrevious)
    public static int GetNextDlgTabItem(int hwndDlg, int hwndCtrl, int fPrevious) {
        /* Undocumented but tested under Win2000 and WinME */
        if (hwndDlg == hwndCtrl) hwndCtrl = 0;

        /* Contrary to MSDN documentation, tested under Win2000 and WinME
         * NB GetLastError returns whatever was set before the function was
         * called.
         */
        if (hwndCtrl == 0 && fPrevious!=0) return 0;

        return DIALOG_GetNextTabItem(hwndDlg, hwndDlg, hwndCtrl, fPrevious);
    }

    static private int DIALOG_GetNextTabItem(int hwndMain, int hwndDlg, int hwndCtrl, int fPrevious) {
        int dsStyle;
        int exStyle;
        int wndSearch = fPrevious!=0 ? GW_HWNDPREV : GW_HWNDNEXT;
        int retWnd = 0;
        int hChildFirst = 0;

        if (hwndCtrl == 0) {
            hChildFirst = WinWindow.GetWindow(hwndDlg, GW_CHILD);
            if (fPrevious!=0) hChildFirst = WinWindow.GetWindow(hChildFirst, GW_HWNDLAST);
        } else if (WinWindow.IsChild(hwndMain, hwndCtrl)!=0) {
            hChildFirst = WinWindow.GetWindow(hwndCtrl, wndSearch);
            if (hChildFirst == 0) {
                if (WinWindow.GetParent(hwndCtrl) != hwndMain)
                    /* i.e. if we are not at the top level of the recursion */
                    hChildFirst = WinWindow.GetWindow(WinWindow.GetParent(hwndCtrl), wndSearch);
                else
                    hChildFirst = WinWindow.GetWindow(hwndCtrl, fPrevious!=0 ? GW_HWNDLAST : GW_HWNDFIRST);
            }
        }

        while (hChildFirst != 0) {
            dsStyle = WinWindow.GetWindowLongA(hChildFirst, GWL_STYLE);
            exStyle = WinWindow.GetWindowLongA(hChildFirst, GWL_EXSTYLE);
            if ((exStyle & WS_EX_CONTROLPARENT) != 0 && (dsStyle & WS_VISIBLE) != 0 && (dsStyle & WS_DISABLED) == 0) {
                int result = DIALOG_GetNextTabItem(hwndMain, hChildFirst, 0, fPrevious);
                if (result != 0) return result;
            } else if ((dsStyle & WS_TABSTOP) != 0 && (dsStyle & WS_VISIBLE) != 0 && (dsStyle & WS_DISABLED) == 0) {
                return hChildFirst;
            }
            hChildFirst = WinWindow.GetWindow(hChildFirst, wndSearch);
        }

        if (hwndCtrl != 0) {
            int hParent = WinWindow.GetParent(hwndCtrl);
            while (hParent != 0) {
                if (hParent == hwndMain) break;
                retWnd = DIALOG_GetNextTabItem(hwndMain, WinWindow.GetParent(hParent), hParent, fPrevious);
                if (retWnd != 0) break;
                hParent = WinWindow.GetParent(hParent);
            }
            if (retWnd == 0)
                retWnd = DIALOG_GetNextTabItem(hwndMain, hwndMain, 0, fPrevious);
        }
        return retWnd != 0 ? retWnd : hwndCtrl;
    }

    // HWND WINAPI GetDlgItem(HWND hDlg, int nIDDlgItem)
    public static int GetDlgItem(int hwndDlg, int id ) {
        WinWindow wnd = WinWindow.get(hwndDlg);
        if (wnd == null)
            return 0;
        Iterator children = wnd.getChildren();
        while (children.hasNext()) {
            WinWindow child = (WinWindow)children.next();
            if (WinWindow.GetWindowLongA(child.handle, GWL_ID) == id)
                return child.handle;
        }
        return 0;
    }
    
    static public int doModal(WinWindow dlg, int lpTemplateName, int lpDialogFunc, int dwInitParam) {
        return 1; // Should be the result of EndDialog
    }
}
