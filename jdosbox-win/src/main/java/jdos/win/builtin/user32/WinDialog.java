package jdos.win.builtin.user32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.GdiObj;
import jdos.win.builtin.gdi32.WinDC;
import jdos.win.builtin.gdi32.WinFont;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.system.*;
import jdos.win.utils.StringUtil;

import java.util.Iterator;

public class WinDialog extends WinAPI {
    // HWND WINAPI CreateDialogIndirectParam(HINSTANCE hInstance, LPCDLGTEMPLATE lpTemplate, HWND hWndParent, DLGPROC lpDialogFunc, LPARAM lParamInit)
    public static int CreateDialogIndirectParamA(int hInstance, int lpTemplate, int hWndParent, int lpDialogFunc, int lParamInit) {
        return DIALOG_CreateIndirect(hInstance, lpTemplate, hWndParent, lpDialogFunc, lParamInit, false, false);
    }

    // BOOL WINAPI EndDialog(HWND hDlg, INT_PTR nResult)
    public static int EndDialog(int hDlg, int nResult) {
        WinWindow dlg = WinWindow.get(hDlg);
        if (dlg == null || dlg.dlgInfo == null) {
            warn("EndDialog: invalid window handle");
            return FALSE;
        }
        DialogInfo dlgInfo = dlg.dlgInfo;
        dlgInfo.idResult = nResult;
        dlgInfo.flags |= DF_END;
        int wasEnabled = (dlgInfo.flags & DF_OWNERENABLED);

        int owner = WinWindow.GetWindow(hDlg, GW_OWNER);
        if (wasEnabled != 0 && owner != 0)
            DIALOG_EnableOwner(owner);

        /* Windows sets the focus to the dialog itself in EndDialog */

        if (WinWindow.IsChild(hDlg, Focus.GetFocus()) != 0)
            Focus.SetFocus(hDlg);

        /* Don't have to send a ShowWindow(SW_HIDE), just do
           SetWindowPos with SWP_HIDEWINDOW as done in Windows */

        WinPos.SetWindowPos(hDlg, NULL, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_HIDEWINDOW);

        if (hDlg == Focus.GetActiveWindow()) {
            /* If this dialog was given an owner then set the focus to that owner
               even when the owner is disabled (normally when a window closes any
               disabled windows cannot receive the focus). */
            if (owner != 0)
                Focus.SetForegroundWindow(owner);
            else
                WinPos.WINPOS_ActivateOtherWindow(hDlg);
        }

        /* unblock dialog loop */
        Message.PostMessageA(hDlg, WM_NULL, 0, 0);
        return TRUE;
    }

    // int WINAPI GetDlgCtrlID(HWND hwndCtl)
    public static int GetDlgCtrlID(int hwndCtl) {
        return WinWindow.GetWindowLongA(hwndCtl, GWL_ID);
    }

    /**
     * ********************************************************************
     * GetNextDlgGroupItem (USER32.@)
     * <p/>
     * Corrections to MSDN documentation
     * <p/>
     * (Under Windows 2000 at least, where hwndDlg is not actually a dialog)
     * 1. hwndCtrl can be hwndDlg in which case it behaves as for NULL
     * 2. Prev of NULL or hwndDlg fails
     */
    // HWND WINAPI GetNextDlgGroupItem(HWND hDlg, HWND hCtl, BOOL bPrevious)
    public static int GetNextDlgGroupItem(int hDlg, int hCtl, int fPrevious) {
        boolean fLooped = false;
        boolean fSkipping = false;

        if (hDlg == hCtl)
            hCtl = NULL;
        if (hCtl == 0 && fPrevious != 0)
            return 0;

        if (hCtl != 0) {
            if (WinWindow.IsChild(hDlg, hCtl) == 0)
                return 0;
        } else {
            /* No ctrl specified -> start from the beginning */
            hCtl = WinWindow.GetWindow(hDlg, GW_CHILD);
            if (hCtl == 0)
                return 0;

            /* MSDN is wrong. fPrevious does not result in the last child */

            /* Maybe that first one is valid.  If so then we don't want to skip it*/
            if ((WinWindow.GetWindowLongA(hCtl, GWL_STYLE) & (WS_VISIBLE | WS_DISABLED)) == WS_VISIBLE) {
                return hCtl;
            }
        }

        /* Always go forward around the group and list of controls; for the
         * previous control keep track; for the next break when you find one
         */
        int retvalue = hCtl;
        int hwnd = hCtl;
        int hwndLastGroup = 0;

        while (true) {
            int hwndNext = WinWindow.GetWindow(hwnd, GW_HWNDNEXT);
            while (hwndNext == 0) {
                /* Climb out until there is a next sibling of the ancestor or we
                 * reach the top (in which case we loop back to the start)
                 */
                if (hDlg == WinWindow.GetParent(hwnd)) {
                    /* Wrap around to the beginning of the list, within the same
                     * group. (Once only)
                     */
                    if (fLooped) return hCtl;
                    fLooped = true;
                    hwndNext = WinWindow.GetWindow(hDlg, GW_CHILD);
                } else {
                    hwnd = WinWindow.GetParent(hwnd);
                    hwndNext = WinWindow.GetWindow(hwnd, GW_HWNDNEXT);
                }
            }
            hwnd = hwndNext;

            /* Wander down the leading edge of controlparents */
            while ((WinWindow.GetWindowLongA(hwnd, GWL_EXSTYLE) & WS_EX_CONTROLPARENT) != 0 && ((WinWindow.GetWindowLongA(hwnd, GWL_STYLE) & (WS_VISIBLE | WS_DISABLED)) == WS_VISIBLE) && (hwndNext = WinWindow.GetWindow(hwnd, GW_CHILD)) != 0) {
                hwnd = hwndNext;
            }
            /* Question.  If the control is a control parent but either has no
             * children or is not visible/enabled then if it has a WS_GROUP does
             * it count?  For that matter does it count anyway?
             * I believe it doesn't count.
             */

            if ((WinWindow.GetWindowLongA(hwnd, GWL_STYLE) & WS_GROUP) != 0) {
                hwndLastGroup = hwnd;
                if (!fSkipping) {
                    /* Look for the beginning of the group */
                    fSkipping = true;
                }
            }

            if (hwnd == hCtl) {
                if (!fSkipping) break;
                if (hwndLastGroup == hwnd) break;
                hwnd = hwndLastGroup;
                fSkipping = false;
                fLooped = false;
            }

            if (!fSkipping && (WinWindow.GetWindowLongA(hwnd, GWL_STYLE) & (WS_VISIBLE | WS_DISABLED)) == WS_VISIBLE) {
                retvalue = hwnd;
                if (fPrevious == 0)
                    break;
            }
        }
        return retvalue;
    }

    // BOOL WINAPI IsDialogMessage(HWND hDlg, LPMSG lpMsg)
    public static int IsDialogMessageA(int hDlg, int lpMsg) {
        return FALSE;
    }

    private static int units = 0;

    // LONG WINAPI GetDialogBaseUnits(void)
    public static int GetDialogBaseUnits() {
        if (units != 0)
            return units;
        int dc = Painting.GetDC(0);
        if (dc != 0) {
            WinSize size = WinFont.GdiGetCharDimensions(dc, NULL);
            Painting.ReleaseDC(0, dc);
            units = MAKELONG(size.cx, size.cy);
        }
        return units;
    }

    // HWND WINAPI GetNextDlgTabItem( HWND hDlg, HWND hCtl, BOOL bPrevious)
    public static int GetNextDlgTabItem(int hwndDlg, int hwndCtrl, int fPrevious) {
        /* Undocumented but tested under Win2000 and WinME */
        if (hwndDlg == hwndCtrl) hwndCtrl = 0;

        /* Contrary to MSDN documentation, tested under Win2000 and WinME
         * NB GetLastError returns whatever was set before the function was
         * called.
         */
        if (hwndCtrl == 0 && fPrevious != 0) return 0;

        return DIALOG_GetNextTabItem(hwndDlg, hwndDlg, hwndCtrl, fPrevious);
    }

    // LRESULT WINAPI SendDlgItemMessage(HWND hDlg, int nIDDlgItem, UINT Msg, WPARAM wParam, LPARAM lParam)
    static public int SendDlgItemMessageA(int hwnd, int id, int msg, int wParam, int lParam) {
        int hwndCtrl = GetDlgItem( hwnd, id );
        if (hwndCtrl!=0) return Message.SendMessageA(hwndCtrl, msg, wParam, lParam);
        else return 0;
    }

    // BOOL WINAPI SetDlgItemText(HWND hDlg, int nIDDlgItem, LPCTSTR lpString)
    public static int SetDlgItemTextA(int hDlg, int nIDDlgItem, int lpString) {
        return SendDlgItemMessageA(hDlg, nIDDlgItem, WM_SETTEXT, 0, lpString);
    }

    static final int DIALOG_CLASS_ATOM = 32770;

    static public void registerClass(User32 dll, WinProcess process) {
        WinClass winClass = WinClass.create(DIALOG_CLASS_ATOM);
        winClass.className = "DIALOG_CLASS";
        winClass.cbWndExtra = 30;
        winClass.hCursor = IDC_ARROW;
        winClass.hbrBackground = 0;
        winClass.style = CS_SAVEBITS | CS_DBLCLKS;
        winClass.eip = dll.getProcAddress("DefDlgProcA", true);
        //process.classNames.put(winClass.className.toLowerCase(), winClass);
    }

    static private int DIALOG_GetNextTabItem(int hwndMain, int hwndDlg, int hwndCtrl, int fPrevious) {
        int dsStyle;
        int exStyle;
        int wndSearch = fPrevious != 0 ? GW_HWNDPREV : GW_HWNDNEXT;
        int retWnd = 0;
        int hChildFirst = 0;

        if (hwndCtrl == 0) {
            hChildFirst = WinWindow.GetWindow(hwndDlg, GW_CHILD);
            if (fPrevious != 0) hChildFirst = WinWindow.GetWindow(hChildFirst, GW_HWNDLAST);
        } else if (WinWindow.IsChild(hwndMain, hwndCtrl) != 0) {
            hChildFirst = WinWindow.GetWindow(hwndCtrl, wndSearch);
            if (hChildFirst == 0) {
                if (WinWindow.GetParent(hwndCtrl) != hwndMain)
                    /* i.e. if we are not at the top level of the recursion */
                    hChildFirst = WinWindow.GetWindow(WinWindow.GetParent(hwndCtrl), wndSearch);
                else
                    hChildFirst = WinWindow.GetWindow(hwndCtrl, fPrevious != 0 ? GW_HWNDLAST : GW_HWNDFIRST);
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
    public static int GetDlgItem(int hwndDlg, int id) {
        WinWindow wnd = WinWindow.get(hwndDlg);
        if (wnd == null)
            return 0;
        Iterator children = wnd.getChildren();
        while (children.hasNext()) {
            WinWindow child = (WinWindow) children.next();
            if (WinWindow.GetWindowLongA(child.handle, GWL_ID) == id)
                return child.handle;
        }
        return 0;
    }

    static private class DLG_TEMPLATE {
        int style;
        int exStyle;
        int helpId;
        int nbItems;
        int x;
        int y;
        int cx;
        int cy;
        int menuName;
        int className;
        int caption;
        int pointSize;
        int weight;
        boolean italic;
        int faceName;
        boolean dialogEx;
    }

    /**
     * ********************************************************************
     * DIALOG_ParseTemplate32
     * <p/>
     * Fill a DLG_TEMPLATE structure from the dialog template, and return
     * a pointer to the first control.
     */
    static public int DIALOG_ParseTemplate32(int template, DLG_TEMPLATE result) {
        int p = template;
        int signature;
        int dlgver;

        dlgver = readw(p);
        p += 2;
        signature = readw(p);
        p += 2;

        if (dlgver == 1 && signature == 0xffff) { /* DIALOGEX resource */
            result.dialogEx = true;
            result.helpId = readd(p);
            p += 4;
            result.exStyle = readd(p);
            p += 4;
            result.style = readd(p);
            p += 4;
        } else {
            result.style = readd(p - 4);
            result.dialogEx = false;
            result.helpId = 0;
            result.exStyle = readd(p);
            p += 4;
        }
        result.nbItems = readw(p);
        p += 2;
        result.x = readw(p);
        p += 2;
        result.y = readw(p);
        p += 2;
        result.cx = readw(p);
        p += 2;
        result.cy = readw(p);
        p += 2;

        /* Get the menu name */

        switch (readw(p)) {
            case 0x0000:
                result.menuName = 0;
                p += 2;
                break;
            case 0xffff:
                result.menuName = readw(p + 2);
                p += 4;
                break;
            default:
                result.menuName = p;
                p += (StringUtil.strlenW(result.menuName) + 1) * 2;
                break;
        }

        /* Get the class name */

        switch (readw(p)) {
            case 0x0000:
                result.className = DIALOG_CLASS_ATOM;
                p += 2;
                break;
            case 0xffff:
                result.className = readw(p + 2);
                p += 4;
                break;
            default:
                result.className = p;
                p += (StringUtil.strlenW(result.className) + 1) * 2;
                break;
        }

        /* Get the window caption */

        result.caption = p;
        p += (StringUtil.strlenW(result.caption) + 1) * 2;

        /* Get the font name */

        result.pointSize = 0;
        result.faceName = NULL;
        result.weight = FW_DONTCARE;
        result.italic = false;

        if ((result.style & DS_SETFONT) != 0) {
            result.pointSize = readw(p);
            p += 2;

            /* If pointSize is 0x7fff, it means that we need to use the font
             * in NONCLIENTMETRICSW.lfMessageFont, and NOT read the weight,
             * italic, and facename from the dialog template.
             */
            if (result.pointSize == 0x7fff) {
                /* We could call SystemParametersInfo here, but then we'd have
                 * to convert from pixel size to point size (which can be
                 * imprecise).
                 */
                // log(" FONT: Using message box font\n");
            } else {
                if (result.dialogEx) {
                    result.weight = readw(p);
                    p += 2;
                    result.italic = (readw(p) & 0xFF) != 0;
                    p += 2;
                }
                result.faceName = p;
                p += (StringUtil.strlenW(result.faceName) + 1) * 2;
            }
        }

        /* First control is on dword boundary */
        return (p + 3) & ~3;
    }

    /**
     * ********************************************************************
     * DIALOG_DisableOwner
     * <p/>
     * Helper function for modal dialogs to disable the
     * owner of the dialog box. Returns TRUE if owner was enabled.
     */
    static private boolean DIALOG_DisableOwner(int hOwner) {
        /* Owner must be a top-level window */
        if (hOwner != 0)
            hOwner = WinWindow.GetAncestor(hOwner, GA_ROOT);
        if (hOwner == 0) return false;
        if (WinWindow.IsWindowEnabled(hOwner) != 0) {
            WinWindow.EnableWindow(hOwner, FALSE);
            return true;
        } else
            return false;
    }

    static private class DialogControlInfo {
        int style;
        int exStyle;
        int helpId;
        int x;
        int y;
        int cx;
        int cy;
        int id;
        int className;
        int windowName;
        int data;
    }

    static private int DIALOG_GetControl32(int p, DialogControlInfo info, boolean dialogEx) {
        if (dialogEx) {
            info.helpId = readd(p);
            p += 4;
            info.exStyle = readd(p);
            p += 4;
            info.style = readd(p);
            p += 4;
        } else {
            info.helpId = 0;
            info.style = readd(p);
            p += 4;
            info.exStyle = readd(p);
            p += 4;
        }
        info.x = readw(p);
        p += 2;
        info.y = readw(p);
        p += 2;
        info.cx = readw(p);
        p += 2;
        info.cy = readw(p);
        p += 2;

        if (dialogEx) {
            /* id is a DWORD for DIALOGEX */
            info.id = readd(p);
            p += 4;
        } else {
            info.id = readw(p);
            p += 2;
        }

        if (readw(p) == 0xffff) {
            int id = readw(p + 2);
            /* Windows treats dialog control class ids 0-5 same way as 0x80-0x85 */
            if ((id >= 0x80) && (id <= 0x85)) id -= 0x80;
            if (id <= 5) {
                String name = null;
                switch (id) {
                    case 0:
                        name = "Button";
                        break;
                    case 1:
                        name = "Edit";
                        break;
                    case 2:
                        name = "Static";
                        break;
                    case 3:
                        name = "ListBox";
                        break;
                    case 4:
                        name = "ScrollBar";
                        break;
                    case 5:
                        name = "ComboBox";
                        break;
                }
                info.className = WinSystem.getCurrentProcess().classNames.get(name.toLowerCase()).handle;
            } else {
                info.className = NULL;
                Win.panic("Unknown built-in class id " + id);
            }
            p += 4;
        } else {
            info.className = p;
            p += (StringUtil.strlenW(info.className) + 1) * 2;
        }

        if (readw(p) == 0xffff) { /* Is it an integer id? */
            info.windowName = readw(p + 2);
            p += 4;
        } else {
            info.windowName = p;
            p += (StringUtil.strlenW(info.windowName) + 1) * 2;
        }

        int count = readw(p);
        p += 2;
        if (count == 0) {
            info.data = 0;
        } else {
            info.data = p;
            p += count;
        }

        /* Next control is on dword boundary */
        return (p + 3) & ~3;
    }

    /**
     * ********************************************************************
     * DIALOG_CreateControls32
     * <p/>
     * Create the control windows for a dialog.
     */
    static private boolean DIALOG_CreateControls32(int hwnd, int template, DLG_TEMPLATE dlgTemplate, int hInst, boolean unicode) {
        DialogInfo dlgInfo = WinWindow.get(hwnd).dlgInfo;
        DialogControlInfo info = new DialogControlInfo();
        int hwndCtrl = 0, hwndDefButton = 0;

        for (int i = 0; i < dlgTemplate.nbItems; i++) {
            template = DIALOG_GetControl32(template, info, dlgTemplate.dialogEx);
            info.style &= ~WS_POPUP;
            info.style |= WS_CHILD;

            if ((info.style & WS_BORDER) != 0) {
                info.style &= ~WS_BORDER;
                info.exStyle |= WS_EX_CLIENTEDGE;
            }
            if (unicode) {
                //hwndCtrl = CreateWindowExW( info.exStyle | WS_EX_NOPARENTNOTIFY, info.className, info.windowName, info.style | WS_CHILD,
                //        info.x * dlgInfo.xBaseUnit / 4, info.y * dlgInfo.yBaseUnit / 8, info.cx * dlgInfo.xBaseUnit / 4, info.cy * dlgInfo.yBaseUnit / 8, hwnd, info.id, hInst, info.data);
                Win.panic("DIALOG_CreateControls32 unicode not implemented yet");
            } else {
                int pClass = info.className;
                int caption = info.windowName;

                if (!IS_INTRESOURCE(pClass)) {
                    pClass = StringUtil.allocateTempA(StringUtil.getStringW(pClass));
                }
                if (!IS_INTRESOURCE(caption)) {
                    caption = StringUtil.allocateTempA(StringUtil.getStringW(caption));
                }
                hwndCtrl = WinWindow.CreateWindowExA(info.exStyle | WS_EX_NOPARENTNOTIFY, pClass, caption, info.style | WS_CHILD,
                        info.x * dlgInfo.xBaseUnit / 4, info.y * dlgInfo.yBaseUnit / 8, info.cx * dlgInfo.xBaseUnit / 4, info.cy * dlgInfo.yBaseUnit / 8, hwnd, info.id, hInst, info.data);
            }
            if (hwndCtrl == 0) {
                log("DIALOG_CreateControls32 control creation failed");
                if ((dlgTemplate.style & DS_NOFAILCREATE) != 0) continue;
                return false;
            }

            /* Send initialisation messages to the control */
            if (dlgInfo.hUserFont != 0)
                Message.SendMessageA(hwndCtrl, WM_SETFONT, dlgInfo.hUserFont, 0);
            if ((Message.SendMessageA(hwndCtrl, WM_GETDLGCODE, 0, 0) & DLGC_DEFPUSHBUTTON) != 0) {
                /* If there's already a default push-button, set it back */
                /* to normal and use this one instead. */
                if (hwndDefButton != 0)
                    Message.SendMessageA(hwndDefButton, BM_SETSTYLE, BS_PUSHBUTTON, FALSE);
                hwndDefButton = hwndCtrl;
                dlgInfo.idResult = WinWindow.GetWindowLongA(hwndCtrl, GWLP_ID);
            }
        }
        return true;
    }

    /**
     * ********************************************************************
     * DIALOG_EnableOwner
     * <p/>
     * Helper function for modal dialogs to enable again the
     * owner of the dialog box.
     */
    static private void DIALOG_EnableOwner(int hOwner) {
        /* Owner must be a top-level window */
        if (hOwner != 0)
            hOwner = WinWindow.GetAncestor(hOwner, GA_ROOT);
        if (hOwner == 0) return;
        WinWindow.EnableWindow(hOwner, TRUE);
    }

    static private int DIALOG_CreateIndirect(int hInst, int dlgTemplate, int owner, int dlgProc, int param, boolean modal, boolean unicode) {
        DLG_TEMPLATE template = new DLG_TEMPLATE();
        int units = GetDialogBaseUnits();
        int hMenu = 0;
        int hUserFont = 0;
        int xBaseUnit = LOWORD(units);
        int yBaseUnit = HIWORD(units);

        /* Parse dialog template */

        if (dlgTemplate == 0) return 0;
        dlgTemplate = DIALOG_ParseTemplate32(dlgTemplate, template);

        /* Load menu */

        if (template.menuName != 0) hMenu = WinMenu.LoadMenuW(hInst, template.menuName);

        /* Create custom font if needed */

        if ((template.style & DS_SETFONT) != 0) {
            int dc = Painting.GetDC(0);

            if (template.pointSize == 0x7fff) {
                warn("DIALOG_CreateIndirect msgbox font is not available yet");
//                /* We get the message font from the non-client metrics */
//                NONCLIENTMETRICSW ncMetrics;
//
//                ncMetrics.cbSize = sizeof(NONCLIENTMETRICSW);
//                if (SystemParametersInfoW(SPI_GETNONCLIENTMETRICS, sizeof(NONCLIENTMETRICSW), &ncMetrics, 0)) {
//                    hUserFont = CreateFontIndirectW( &ncMetrics.lfMessageFont );
//                }
            } else {
                /* We convert the size to pixels and then make it -ve.  This works
                 * for both +ve and -ve template.pointSize */
                int pixels = template.pointSize * WinDC.GetDeviceCaps(dc, LOGPIXELSY) / 72;
                hUserFont = WinFont.CreateFontW(-pixels, 0, 0, 0, template.weight, BOOL(template.italic), FALSE, FALSE, DEFAULT_CHARSET, 0, 0, PROOF_QUALITY, FF_DONTCARE, template.faceName);
            }

            if (hUserFont != 0) {
                int hOldFont = WinDC.SelectObject(dc, hUserFont);
                WinSize size = WinFont.GdiGetCharDimensions(dc, NULL);
                if (size != null) {
                    xBaseUnit = size.cx;
                    yBaseUnit = size.cy;
                }
                WinDC.SelectObject(dc, hOldFont);
            }
            Painting.ReleaseDC(0, dc);
        }

        /* Create dialog main window */

        WinRect rect = new WinRect();
        rect.left = rect.top = 0;
        rect.right = template.cx * xBaseUnit / 4;
        rect.bottom = template.cy * yBaseUnit / 8;
        if ((template.style & WS_CHILD) != 0)
            template.style &= ~(WS_CAPTION | WS_SYSMENU);
        if ((template.style & DS_MODALFRAME) != 0)
            template.exStyle |= WS_EX_DLGMODALFRAME;
        if ((template.style & DS_CONTROL) != 0)
            template.exStyle |= WS_EX_CONTROLPARENT;
        int lprc = rect.allocTemp();
        NonClient.AdjustWindowRectEx(lprc, template.style, (hMenu != 0) ? TRUE : FALSE, template.exStyle);
        WinPoint pos = new WinPoint(rect.left, rect.top);
        WinSize size = new WinSize(rect.width(), rect.height());
        if (template.x == 0x8000 /*CW_USEDEFAULT16*/) {
            pos.x = pos.y = CW_USEDEFAULT;
        } else {
            if ((template.style & DS_CENTER) != 0) {
                pos.x = (StaticData.screen.getWidth() - size.cx) / 2;
                pos.y = (StaticData.screen.getHeight() - size.cy) / 2;
            } else if ((template.style & DS_CENTERMOUSE) != 0) {
                pos.x = StaticData.currentPos.x - StaticData.screen.getWidth() / 2;
                pos.y = StaticData.currentPos.y - StaticData.screen.getHeight() / 2;
            } else {
                pos.x += template.x * xBaseUnit / 4;
                pos.y += template.y * yBaseUnit / 8;
                int lpt = pos.allocTemp();
                if ((template.style & (WS_CHILD | DS_ABSALIGN)) == 0) {
                    WinPos.ClientToScreen(owner, lpt);
                    pos.copy(lpt);
                }
            }
            if ((template.style & WS_CHILD) == 0) {
                int dX, dY;

                /* try to fit it into the desktop */
                if ((dX = pos.x + size.cx + SysParams.GetSystemMetrics(SM_CXDLGFRAME) - StaticData.screen.getWidth()) > 0)
                    pos.x -= dX;
                if ((dY = pos.y + size.cy + SysParams.GetSystemMetrics(SM_CYDLGFRAME) - StaticData.screen.getHeight()) > 0)
                    pos.y -= dY;
                if (pos.x < 0) pos.x = 0;
                if (pos.y < 0) pos.y = 0;
            }
        }

        boolean ownerEnabled = true;
        int flags = 0;

        if (modal) {
            ownerEnabled = DIALOG_DisableOwner(owner);
            if (ownerEnabled) flags |= DF_OWNERENABLED;
        }

        int hwnd = 0;
        if (unicode) {
            // hwnd = CreateWindowExW(template.exStyle, template.className, template.caption, template.style & ~WS_VISIBLE, pos.x, pos.y, size.cx, size.cy, owner, hMenu, hInst, NULL );
            Win.panic("DIALOG_CreateIndirect unicode not implemented yet");
        } else {
            int pClass = template.className;
            int caption = template.caption;

            if (!IS_INTRESOURCE(pClass)) {
                pClass = StringUtil.allocateTempA(StringUtil.getStringW(pClass));
            }
            if (!IS_INTRESOURCE(caption)) {
                caption = StringUtil.allocateTempA(StringUtil.getStringW(caption));
            }
            hwnd = WinWindow.CreateWindowExA(template.exStyle, pClass, caption, template.style & ~WS_VISIBLE, pos.x, pos.y, size.cx, size.cy, owner, hMenu, hInst, NULL);
        }

        WinWindow window = WinWindow.get(hwnd);
        if (hwnd == 0 || window == null) {
            if (hUserFont != 0) GdiObj.DeleteObject(hUserFont);
            if (hMenu != 0) WinMenu.DestroyMenu(hMenu);
            if (modal && (flags & DF_OWNERENABLED) != 0) DIALOG_EnableOwner(owner);
            return 0;
        }

        /* moved this from the top of the method to here as DIALOGINFO structure
        will be valid only after WM_CREATE message has been handled in DefDlgProc
        All the members of the structure get filled here using temp variables */
        if (WinWindow.get(hwnd).dlgInfo == null)
            WinWindow.get(hwnd).dlgInfo = new DialogInfo();
        DialogInfo dlgInfo = WinWindow.get(hwnd).dlgInfo;
        dlgInfo.hwndFocus = 0;
        dlgInfo.hUserFont = hUserFont;
        dlgInfo.hMenu = hMenu;
        dlgInfo.xBaseUnit = xBaseUnit;
        dlgInfo.yBaseUnit = yBaseUnit;
        dlgInfo.flags = flags;

        if (template.helpId != 0) window.helpContext = template.helpId;

        if (unicode) {
            // WinWindow.SetWindowLongW( hwnd, DWLP_DLGPROC, dlgProc );
            Win.panic("DIALOG_CreateIndirect unicode not implemented yet");
        } else {
            WinWindow.SetWindowLongA(hwnd, DWLP_DLGPROC, dlgProc);
        }

        if (dlgProc != 0 && dlgInfo.hUserFont != 0)
            Message.SendMessageA(hwnd, WM_SETFONT, dlgInfo.hUserFont, 0);

        /* Create controls */

        if (DIALOG_CreateControls32(hwnd, dlgTemplate, template, hInst, unicode)) {
            /* Send initialisation messages and set focus */

            if (dlgProc != 0) {
                int focus = GetNextDlgTabItem(hwnd, 0, FALSE);
                if (Message.SendMessageA(hwnd, WM_INITDIALOG, focus, param) != 0 && WinWindow.IsWindow(hwnd) != 0 && ((~template.style & DS_CONTROL) != 0 || (template.style & WS_VISIBLE) != 0)) {
                    /* By returning TRUE, app has requested a default focus assignment.
                     * WM_INITDIALOG may have changed the tab order, so find the first
                     * tabstop control again. */
                    dlgInfo.hwndFocus = GetNextDlgTabItem(hwnd, 0, FALSE);
                    if (dlgInfo.hwndFocus != 0)
                        Focus.SetFocus(dlgInfo.hwndFocus);
                }
            }

            if ((template.style & WS_VISIBLE) != 0 && (WinWindow.GetWindowLongA(hwnd, GWL_STYLE) & WS_VISIBLE) == 0) {
                WinPos.ShowWindow(hwnd, SW_SHOWNORMAL);   /* SW_SHOW doesn't always work */
            }
            return hwnd;
        }
        if (modal && ownerEnabled) DIALOG_EnableOwner(owner);
        if (WinWindow.IsWindow(hwnd) != 0) WinWindow.DestroyWindow(hwnd);
        return 0;
    }
}
