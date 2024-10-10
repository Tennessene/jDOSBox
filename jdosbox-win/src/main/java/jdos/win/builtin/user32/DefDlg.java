package jdos.win.builtin.user32;

import jdos.cpu.CPU_Regs;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.WinDC;
import jdos.win.system.WinRect;
import jdos.win.system.WinSystem;

public class DefDlg extends WinAPI {
    //static public final int DWLP_MSGRESULT = 0;
    //static public final int DWLP_DLGPROC = 4;

    static public final int DM_GETDEFID = WM_USER;
    static public final int DM_SETDEFID = WM_USER + 1;

    static public final int DC_HASDEFID = 0x534B;

    //LRESULT WINAPI DefDlgProc(HWinWindow hDlg, UINT Msg, WPARAM wParam, LPARAM lParam)
    public static int DefDlgProcA(int hDlg, int msg, int wParam, int lParam) {
        WinWindow window = WinWindow.get(hDlg);
        if (window == null)
            return 0;

        if (window.dlgInfo == null) {
            window.dlgInfo = new DialogInfo();
        }
        WinWindow.SetWindowLongA(hDlg, DWLP_MSGRESULT, 0);
        int dlgEip = WinWindow.GetWindowLongA(hDlg, DWLP_DLGPROC);
        int result = 0;

        if (dlgEip != 0) {
            WinSystem.call(dlgEip, window.handle, msg, wParam, lParam);
            result = CPU_Regs.reg_eax.dword;
        }
        if (result == 0) {
            switch (msg) {
                case WM_ERASEBKGND:
                case WM_SHOWWINDOW:
                case WM_ACTIVATE:
                case WM_SETFOCUS:
                case DM_SETDEFID:
                case DM_GETDEFID:
                case WM_NEXTDLGCTL:
                case WM_GETFONT:
                case WM_CLOSE:
                case WM_NCDESTROY:
                case WM_ENTERMENULOOP:
                case WM_LBUTTONDOWN:
                case WM_NCLBUTTONDOWN:
                    return DEFDLG_Proc(hDlg, msg, wParam, lParam);
                case WM_INITDIALOG:
                case WM_VKEYTOITEM:
                case WM_COMPAREITEM:
                case WM_CHARTOITEM:
                    break;

                default:
                    return DefWnd.DefWindowProcA(hDlg, msg, wParam, lParam);
            }
        }
        if ((msg >= WM_CTLCOLORMSGBOX && msg <= WM_CTLCOLORSTATIC) ||
                msg == WM_CTLCOLOR || msg == WM_COMPAREITEM ||
                msg == WM_VKEYTOITEM || msg == WM_CHARTOITEM ||
                msg == WM_QUERYDRAGICON || msg == WM_INITDIALOG) {
            return result;
        }
        return WinWindow.GetWindowLongA(hDlg, DWLP_MSGRESULT);
    }

    static private int DEFDLG_Proc(int hwnd, int msg, int wParam, int lParam) {
        WinWindow window = WinWindow.get(hwnd);
        switch (msg) {
            case WM_ERASEBKGND: {
                int brush = Message.SendMessageA(hwnd, WM_CTLCOLORDLG, wParam, hwnd);
                if (brush == 0) brush = DefWnd.DefWindowProcA(hwnd, WM_CTLCOLORDLG, wParam, hwnd);
                if (brush != 0) {
                    int rect = getTempBuffer(WinRect.SIZE);
                    WinRect.write(rect, 0, 0, window.rectClient.width(), window.rectClient.height());
                    // Mapping.DPtoLP(dc, rect);
                    WinDC.FillRect(wParam, rect, brush);
                }
                return 1;
            }
            case WM_NCDESTROY:
                if (window.dlgInfo != null)
                    window.dlgInfo.close();
                return DefWnd.DefWindowProcA(hwnd, msg, wParam, lParam);
            case WM_SHOWWINDOW:
                if (wParam == 0) DEFDLG_SaveFocus(hwnd);
                return DefWnd.DefWindowProcA(hwnd, msg, wParam, lParam);
            case WM_ACTIVATE:
                if (wParam != 0) DEFDLG_RestoreFocus(hwnd);
                else DEFDLG_SaveFocus(hwnd);
                return 0;
            case WM_SETFOCUS:
                DEFDLG_RestoreFocus(hwnd);
                return 0;

            case DM_SETDEFID:
                if (window.dlgInfo != null && !window.dlgInfo.endDialogCalled)
                    DEFDLG_SetDefId(hwnd, wParam);
                return 1;

            case DM_GETDEFID:
                if (window.dlgInfo != null && !window.dlgInfo.endDialogCalled) {
                    if (window.dlgInfo.idResult != 0)
                        return WinAPI.MAKELONG(window.dlgInfo.idResult, DC_HASDEFID);
                    int windowDef = DEFDLG_FindDefButton(hwnd);
                    if (windowDef != 0)
                        return MAKELONG(WinDialog.GetDlgCtrlID(windowDef), DC_HASDEFID);
                }
                return 0;

            case WM_NEXTDLGCTL:
                if (window.dlgInfo != null) {
                    int hwndDest = wParam;
                    if (lParam == 0)
                        hwndDest = WinDialog.GetNextDlgTabItem(hwnd, Focus.GetFocus(), wParam);
                    if (hwndDest != 0) DEFDLG_SetFocus(hwnd, hwndDest);
                    DEFDLG_SetDefButton(hwnd, hwndDest);
                }
                return 0;

            case WM_ENTERMENULOOP:
            case WM_LBUTTONDOWN:
            case WM_NCLBUTTONDOWN: {
                int hwndFocus = Focus.GetFocus();
                if (hwndFocus != 0) {
                    /* always make combo box hide its listbox control */
                    if (Message.SendMessageA(hwndFocus, CB_SHOWDROPDOWN, FALSE, 0) != 0)
                        Message.SendMessageA(WinWindow.GetParent(hwndFocus), CB_SHOWDROPDOWN, FALSE, 0);
                }
            }
            return DefWnd.DefWindowProcA(hwnd, msg, wParam, lParam);

            case WM_GETFONT:
                return window.dlgInfo != null ? window.dlgInfo.hUserFont : 0;

            case WM_CLOSE:
                Message.PostMessageA(hwnd, WM_COMMAND, WinAPI.MAKEWPARAM(IDCANCEL, BN_CLICKED), WinDialog.GetDlgItem(hwnd, IDCANCEL));
                return 0;
        }
        return 0;
    }

    static private void DEFDLG_SaveFocus(int hWnd) {
        int hwndFocus = Focus.GetFocus();

        if (hwndFocus == 0 || WinWindow.IsChild(hWnd, hwndFocus)==0) return;
        WinWindow window = WinWindow.get(hWnd);
        if (window.dlgInfo == null) return;
        window.dlgInfo.hwndFocus = hwndFocus;
    }

    static private void DEFDLG_RestoreFocus(int hWnd) {
        if (WinPos.IsIconic(hWnd)!=0) return;
        WinWindow window = WinWindow.get(hWnd);
        if (window.dlgInfo == null) return;
        /* Don't set the focus back to controls if EndDialog is already called.*/
        if (window.dlgInfo.endDialogCalled) return;
        if (window.dlgInfo.hwndFocus == 0 || window.dlgInfo.hwndFocus == hWnd) {
            /* If no saved focus control exists, set focus to the first visible,
               non-disabled, WS_TABSTOP control in the dialog */
            window.dlgInfo.hwndFocus = WinDialog.GetNextDlgTabItem(hWnd, 0, 0);
            if (window.dlgInfo.hwndFocus == 0) return;
        }
        DEFDLG_SetFocus(hWnd, window.dlgInfo.hwndFocus);

        /* This used to set infoPtr->hwndFocus to NULL for no apparent reason,
           sometimes losing focus when receiving WM_SETFOCUS messages. */
    }

    private static void DEFDLG_SetFocus(int hwndDlg, int hwndCtrl) {
        if ((Message.SendMessageA(hwndCtrl, WM_GETDLGCODE, 0, 0) & WinDialog.DLGC_HASSETSEL) != 0)
            Message.SendMessageA(hwndCtrl, EM_SETSEL, 0, -1);
        Focus.SetFocus(hwndCtrl);
    }

    private static boolean DEFDLG_SetDefId(int hwndDlg, int wParam) {
        int dlgcode = 0;
        int hwndOld;
        int hwndNew = WinDialog.GetDlgItem(hwndDlg, wParam);
        WinWindow window = WinWindow.get(hwndDlg);
        int old_id = window.dlgInfo.idResult;

        window.dlgInfo.idResult = wParam;
        if (hwndNew != 0 && ((dlgcode = Message.SendMessageA(hwndNew, WM_GETDLGCODE, 0, 0)) & (DLGC_UNDEFPUSHBUTTON | DLGC_BUTTON)) == 0)
            return false;  /* Destination is not a push button */

        /* Make sure the old default control is a valid push button ID */
        hwndOld = WinDialog.GetDlgItem(hwndDlg, old_id);
        if (hwndOld == 0 || (Message.SendMessageA(hwndDlg, WM_GETDLGCODE, 0, 0) & DLGC_DEFPUSHBUTTON) == 0)
            hwndOld = DEFDLG_FindDefButton(hwndDlg);
        if (hwndOld != 0 && hwndOld != hwndNew)
            Message.SendMessageA(hwndOld, BM_SETSTYLE, BS_PUSHBUTTON, TRUE);

        if (hwndNew != 0) {
            if ((dlgcode & DLGC_UNDEFPUSHBUTTON) != 0)
                Message.SendMessageA(hwndNew, BM_SETSTYLE, BS_DEFPUSHBUTTON, TRUE);
        }
        return true;
    }

    static public int DEFDLG_FindDefButton(int hwndDlg) {
        int hwndChild;
        int hwndTmp;

        hwndChild = WinWindow.GetWindow(hwndDlg, GW_CHILD);
        while (hwndChild != 0) {
            if ((Message.SendMessageA(hwndChild, WM_GETDLGCODE, 0, 0) & DLGC_DEFPUSHBUTTON) != 0)
                break;

            /* Recurse into WS_EX_CONTROLPARENT controls */
            if ((WinWindow.GetWindowLongA(hwndChild, GWL_EXSTYLE) & WS_EX_CONTROLPARENT) != 0) {
                int dsStyle = WinWindow.GetWindowLongA(hwndChild, GWL_STYLE);
                if ((dsStyle & WS_VISIBLE) != 0 && (dsStyle & WS_DISABLED) == 0 && (hwndTmp = DEFDLG_FindDefButton(hwndChild)) != 0)
                    return hwndTmp;
            }
            hwndChild = WinWindow.GetWindow(hwndChild, GW_HWNDNEXT);
        }
        return hwndChild;
    }

    private static boolean DEFDLG_SetDefButton(int hwndDlg, int hwndNew) {
        int dlgcode = 0;
        WinWindow window = WinWindow.get(hwndDlg);
        int hwndOld = WinDialog.GetDlgItem(hwndDlg, window.dlgInfo.idResult);

        if (hwndNew != 0 && ((dlgcode = Message.SendMessageA(hwndNew, WM_GETDLGCODE, 0, 0)) & (DLGC_UNDEFPUSHBUTTON | DLGC_DEFPUSHBUTTON)) == 0) {
            /**
             * Need to draw only default push button rectangle.
             * Since the next control is not a push button, need to draw the push
             * button rectangle for the default control.
             */
            hwndNew = hwndOld;
            dlgcode = Message.SendMessageA(hwndNew, WM_GETDLGCODE, 0, 0);
        }

        /* Make sure the old default control is a valid push button ID */
        if (hwndOld == 0 || (Message.SendMessageA(hwndOld, WM_GETDLGCODE, 0, 0) & DLGC_DEFPUSHBUTTON) == 0)
            hwndOld = DEFDLG_FindDefButton(hwndDlg);
        if (hwndOld != 0 && hwndOld != hwndNew)
            Message.SendMessageA(hwndOld, BM_SETSTYLE, BS_PUSHBUTTON, TRUE);

        if (hwndNew != 0) {
            if ((dlgcode & DLGC_UNDEFPUSHBUTTON) != 0)
                Message.SendMessageA(hwndNew, BM_SETSTYLE, BS_DEFPUSHBUTTON, TRUE);
        }
        return true;
    }
}