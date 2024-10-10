package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.GdiObj;
import jdos.win.builtin.gdi32.WinDC;
import jdos.win.system.StaticData;
import jdos.win.system.WinRect;
import jdos.win.utils.StringUtil;

public class DefWnd extends WinAPI {
    // LRESULT WINAPI DefWindowProc(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    static public int DefWindowProcA(int hWnd, int Msg, int wParam, int lParam) {
        int result = 0;

        switch(Msg)
        {
        case WM_NCCREATE:
            if (lParam!=0) {
                CREATESTRUCT cs = new CREATESTRUCT(lParam);
                /* check for string, as static icons, bitmaps (SS_ICON, SS_BITMAP)
                 * may have child window IDs instead of window name */
                if (!IS_INTRESOURCE(cs.lpszName)) {
                    WinWindow window = WinWindow.get(hWnd);
                    if (window != null)
                        window.text = StringUtil.getString(cs.lpszName);
                }
                result = 1;
            }
            break;
        case WM_GETTEXTLENGTH:
            {
                WinWindow window = WinWindow.get(hWnd);
                if (window != null)
                    result = window.text.length();
            }
            break;
        case WM_GETTEXT:
            if (wParam!=0) {
                WinWindow window = WinWindow.get(hWnd);
                if (window != null)
                    result = StringUtil.strncpy(lParam, window.text, wParam);
            }
            break;
        case WM_SETTEXT:
            {
                WinWindow window = WinWindow.get(hWnd);
                if (window != null) {
                    window.text = StringUtil.getString(lParam);
                    if ((window.dwStyle & WS_CAPTION) == WS_CAPTION)
                        NonClient.NC_HandleNCPaint(hWnd, 1); /* Repaint caption */
                }
                result = 1; /* success. FIXME: check text length */
            }
            break;
        /* fall through */
        default:
            result = DEFWND_DefWinProc(hWnd, Msg, wParam, lParam);
            break;
        }
        return result;
    }

    /***********************************************************************
     *           DEFWND_DefWinProc
     *
     * Default window procedure for messages that are the same in Ansi and Unicode.
     */
    private static int DEFWND_DefWinProc(int hwnd, int msg, int wParam, int lParam ) {
        switch(msg)
        {
//        case WM_NCPAINT:
//            return NC_HandleNCPaint( hwnd, (HRGN)wParam );
//
        case WM_NCHITTEST:
            return HTCLIENT;
//            {
//                POINT pt;
//                pt.x = (short)LOWORD(lParam);
//                pt.y = (short)HIWORD(lParam);
//                return NC_HandleNCHitTest( hwnd, pt );
//            }
//
//        case WM_NCCALCSIZE:
//            return NC_HandleNCCalcSize( hwnd, wParam, (RECT *)lParam );
//
//        case WM_WINDOWPOSCHANGING:
//            return WINPOS_HandleWindowPosChanging( hwnd, (WINDOWPOS *)lParam );
//
//        case WM_WINDOWPOSCHANGED:
//            DEFWND_HandleWindowPosChanged( hwnd, (const WINDOWPOS *)lParam );
//            break;
//
//        case WM_LBUTTONDOWN:
//        case WM_RBUTTONDOWN:
//        case WM_MBUTTONDOWN:
//            iF10Key = iMenuSysKey = 0;
//            break;
//
//        case WM_NCLBUTTONDOWN:
//            return NC_HandleNCLButtonDown( hwnd, wParam, lParam );
//
//        case WM_LBUTTONDBLCLK:
//            return NC_HandleNCLButtonDblClk( hwnd, HTCLIENT, lParam );
//
//        case WM_NCLBUTTONDBLCLK:
//            return NC_HandleNCLButtonDblClk( hwnd, wParam, lParam );
//
//        case WM_NCRBUTTONDOWN:
//            /* in Windows, capture is taken when right-clicking on the caption bar */
//            if (wParam==HTCAPTION)
//            {
//                SetCapture(hwnd);
//            }
//            break;
//
//        case WM_RBUTTONUP:
//            {
//                POINT pt;
//
//                if (hwnd == GetCapture())
//                    /* release capture if we took it on WM_NCRBUTTONDOWN */
//                    ReleaseCapture();
//
//                pt.x = (short)LOWORD(lParam);
//                pt.y = (short)HIWORD(lParam);
//                ClientToScreen(hwnd, &pt);
//                SendMessageW( hwnd, WM_CONTEXTMENU, (WPARAM)hwnd, MAKELPARAM(pt.x, pt.y) );
//            }
//            break;
//
//        case WM_NCRBUTTONUP:
//            /*
//             * FIXME : we must NOT send WM_CONTEXTMENU on a WM_NCRBUTTONUP (checked
//             * in Windows), but what _should_ we do? According to MSDN :
//             * "If it is appropriate to do so, the system sends the WM_SYSCOMMAND
//             * message to the window". When is it appropriate?
//             */
//            break;
//
//        case WM_CONTEXTMENU:
//            if (GetWindowLongW( hwnd, GWL_STYLE ) & WS_CHILD)
//                SendMessageW( GetParent(hwnd), msg, wParam, lParam );
//            else
//            {
//                LONG hitcode;
//                POINT pt;
//                WND *wndPtr = WIN_GetPtr( hwnd );
//                HMENU hMenu = wndPtr->hSysMenu;
//                WIN_ReleasePtr( wndPtr );
//                if (!hMenu) return 0;
//                pt.x = (short)LOWORD(lParam);
//                pt.y = (short)HIWORD(lParam);
//                hitcode = NC_HandleNCHitTest(hwnd, pt);
//
//                /* Track system popup if click was in the caption area. */
//                if (hitcode==HTCAPTION || hitcode==HTSYSMENU)
//                   TrackPopupMenu(GetSystemMenu(hwnd, FALSE),
//                                   TPM_LEFTBUTTON | TPM_RIGHTBUTTON,
//                                   pt.x, pt.y, 0, hwnd, NULL);
//            }
//            break;
//
//        case WM_POPUPSYSTEMMENU:
//            {
//                /* This is an undocumented message used by the windows taskbar to
//                   display the system menu of windows that belong to other processes. */
//                HMENU menu = GetSystemMenu(hwnd, FALSE);
//
//                if (menu)
//                    TrackPopupMenu(menu, TPM_LEFTBUTTON|TPM_RIGHTBUTTON,
//                                   LOWORD(lParam), HIWORD(lParam), 0, hwnd, NULL);
//                return 0;
//            }
//
//        case WM_NCACTIVATE:
//            return NC_HandleNCActivate( hwnd, wParam, lParam );
//
//        case WM_NCDESTROY:
//            {
//                WND *wndPtr = WIN_GetPtr( hwnd );
//                if (!wndPtr) return 0;
//                HeapFree( GetProcessHeap(), 0, wndPtr->text );
//                wndPtr->text = NULL;
//                HeapFree( GetProcessHeap(), 0, wndPtr->pScroll );
//                wndPtr->pScroll = NULL;
//                WIN_ReleasePtr( wndPtr );
//                return 0;
//            }
//
//        case WM_PRINT:
//            DEFWND_Print(hwnd, (HDC)wParam, lParam);
//            return 0;
//
//        case WM_PAINTICON:
        case WM_PAINT:
            {
                int ps = getTempBuffer(PAINTSTRUCT.SIZE);
                int hdc = Painting.BeginPaint(hwnd, ps);
                if (hdc!=0) {
//                  HICON hIcon;
//                  if (IsIconic(hwnd) && ((hIcon = WinClass.GetClassLongA( hwnd, GCLP_HICON))) )
//                  {
//                      RECT rc;
//                      int x, y;
//
//                      GetClientRect( hwnd, &rc );
//                      x = (rc.right - rc.left - GetSystemMetrics(SM_CXICON))/2;
//                      y = (rc.bottom - rc.top - GetSystemMetrics(SM_CYICON))/2;
//                      TRACE("Painting class icon: vis rect=(%s)\n",
//                            wine_dbgstr_rect(&ps.rcPaint));
//                      DrawIcon( hdc, x, y, hIcon );
//                  }
                    Painting.EndPaint(hwnd, ps);
                }
                return 0;
            }

        case WM_SYNCPAINT:
            Painting.RedrawWindow (hwnd, NULL, 0, RDW_ERASENOW | RDW_ERASE | RDW_ALLCHILDREN);
            return 0;

//        case WM_SETREDRAW:
//            if (wParam!=0) WIN_SetStyle( hwnd, WS_VISIBLE, 0 );
//            else
//            {
//                Painting.RedrawWindow( hwnd, NULL, 0, RDW_ALLCHILDREN | RDW_VALIDATE );
//                WIN_SetStyle( hwnd, 0, WS_VISIBLE );
//            }
//            return 0;

        case WM_CLOSE:
            WinWindow.DestroyWindow( hwnd );
            return 0;

//        case WM_MOUSEACTIVATE:
//            if (GetWindowLongW( hwnd, GWL_STYLE ) & WS_CHILD)
//            {
//                LONG ret = SendMessageW( GetParent(hwnd), WM_MOUSEACTIVATE, wParam, lParam );
//                if (ret) return ret;
//            }
//
//            /* Caption clicks are handled by NC_HandleNCLButtonDown() */
//            return MA_ACTIVATE;

        case WM_ACTIVATE:
            /* The default action in Windows is to set the keyboard focus to
             * the window, if it's being activated and not minimized */
            if (LOWORD(wParam) != WA_INACTIVE) {
                if (WinPos.IsIconic(hwnd)==0) Focus.SetFocus(hwnd);
            }
            break;

//        case WM_MOUSEWHEEL:
//            if (GetWindowLongW( hwnd, GWL_STYLE ) & WS_CHILD)
//                return SendMessageW( GetParent(hwnd), WM_MOUSEWHEEL, wParam, lParam );
//            break;

        case WM_ERASEBKGND:
        case WM_ICONERASEBKGND:
            {
                WinWindow window = WinWindow.get(hwnd);
                if (window == null)
                    return 0;
                int hdc = wParam;
                int hbr = WinClass.GetClassLongA(hwnd, GCLP_HBRBACKGROUND);
                if (hbr==0) return 0;
                int rect = getTempBuffer(WinRect.SIZE);
                if ((WinClass.GetClassLongA( hwnd, GCL_STYLE ) & CS_PARENTDC)!=0) {
                    /* can't use GetClipBox with a parent DC or we fill the whole parent */
                    window.rectClient.write(rect);
                } else {
                    WinDC.GetClipBox(hdc, rect);
                }
                WinDC.FillRect(hdc, rect, hbr);
                return 1;
            }

        case WM_GETDLGCODE:
            return 0;

        case WM_CTLCOLORMSGBOX:
        case WM_CTLCOLOREDIT:
        case WM_CTLCOLORLISTBOX:
        case WM_CTLCOLORBTN:
        case WM_CTLCOLORDLG:
        case WM_CTLCOLORSTATIC:
        case WM_CTLCOLORSCROLLBAR:
            return DEFWND_ControlColor(wParam, msg - WM_CTLCOLORMSGBOX );

        case WM_CTLCOLOR:
            return DEFWND_ControlColor(wParam, HIWORD(lParam) );

        case WM_SETCURSOR:
            if ((WinWindow.GetWindowLongA(hwnd, GWL_STYLE) & WS_CHILD) != 0) {
                /* with the exception of the border around a resizable wnd,
                 * give the parent first chance to set the cursor */
                if ((LOWORD(lParam) < HTSIZEFIRST) || (LOWORD(lParam) > HTSIZELAST))
                {
                    int parent = WinWindow.GetParent(hwnd);
                    if (parent != 0) {
                        if (Message.SendMessageA(parent, WM_SETCURSOR, wParam, lParam)!=0)
                            return TRUE;
                    }
                }
            }
            NonClient.NC_HandleSetCursor(hwnd, wParam, lParam);
            break;

//        case WM_SYSCOMMAND:
//            return NC_HandleSysCommand( hwnd, wParam, lParam );
//
//        case WM_KEYDOWN:
//            if(wParam == VK_F10) iF10Key = VK_F10;
//            break;
//
//        case WM_SYSKEYDOWN:
//            if( HIWORD(lParam) & KEYDATA_ALT )
//            {
//                /* if( HIWORD(lParam) & ~KEYDATA_PREVSTATE ) */
//                  if ( (wParam == VK_MENU || wParam == VK_LMENU
//                        || wParam == VK_RMENU) && !iMenuSysKey )
//                    iMenuSysKey = 1;
//                  else
//                    iMenuSysKey = 0;
//
//                iF10Key = 0;
//
//                if( wParam == VK_F4 )       /* try to close the window */
//                {
//                    HWND top = GetAncestor( hwnd, GA_ROOT );
//                    if (!(GetClassLongW( top, GCL_STYLE ) & CS_NOCLOSE))
//                        PostMessageW( top, WM_SYSCOMMAND, SC_CLOSE, 0 );
//                }
//            }
//            else if( wParam == VK_F10 )
//            {
//                if (GetKeyState(VK_SHIFT) & 0x8000)
//                    SendMessageW( hwnd, WM_CONTEXTMENU, (WPARAM)hwnd, -1 );
//                iF10Key = 1;
//            }
//            else if( wParam == VK_ESCAPE && (GetKeyState(VK_SHIFT) & 0x8000))
//                SendMessageW( hwnd, WM_SYSCOMMAND, SC_KEYMENU, ' ' );
//            break;
//
//        case WM_KEYUP:
//        case WM_SYSKEYUP:
//            /* Press and release F10 or ALT */
//            if (((wParam == VK_MENU || wParam == VK_LMENU || wParam == VK_RMENU)
//                 && iMenuSysKey) || ((wParam == VK_F10) && iF10Key))
//                  SendMessageW( GetAncestor( hwnd, GA_ROOT ), WM_SYSCOMMAND, SC_KEYMENU, 0L );
//            iMenuSysKey = iF10Key = 0;
//            break;
//
//        case WM_SYSCHAR:
//        {
//            iMenuSysKey = 0;
//            if (wParam == '\r' && IsIconic(hwnd))
//            {
//                PostMessageW( hwnd, WM_SYSCOMMAND, SC_RESTORE, 0L );
//                break;
//            }
//            if ((HIWORD(lParam) & KEYDATA_ALT) && wParam)
//            {
//                if (wParam == '\t' || wParam == '\x1b') break;
//                if (wParam == ' ' && (GetWindowLongW( hwnd, GWL_STYLE ) & WS_CHILD))
//                    SendMessageW( GetParent(hwnd), msg, wParam, lParam );
//                else
//                    SendMessageW( hwnd, WM_SYSCOMMAND, SC_KEYMENU, wParam );
//            }
//            else /* check for Ctrl-Esc */
//                if (wParam != '\x1b') MessageBeep(0);
//            break;
//        }
//
//        case WM_SHOWWINDOW:
//            {
//                LONG style = GetWindowLongW( hwnd, GWL_STYLE );
//                WND *pWnd;
//                if (!lParam) return 0; /* sent from ShowWindow */
//                if ((style & WS_VISIBLE) && wParam) return 0;
//                if (!(style & WS_VISIBLE) && !wParam) return 0;
//                if (!GetWindow( hwnd, GW_OWNER )) return 0;
//                if (!(pWnd = WIN_GetPtr( hwnd ))) return 0;
//                if (pWnd == WND_OTHER_PROCESS) return 0;
//                if (wParam)
//                {
//                    if (!(pWnd->flags & WIN_NEEDS_SHOW_OWNEDPOPUP))
//                    {
//                        WIN_ReleasePtr( pWnd );
//                        return 0;
//                    }
//                    pWnd->flags &= ~WIN_NEEDS_SHOW_OWNEDPOPUP;
//                }
//                else pWnd->flags |= WIN_NEEDS_SHOW_OWNEDPOPUP;
//                WIN_ReleasePtr( pWnd );
//                ShowWindow( hwnd, wParam ? SW_SHOWNOACTIVATE : SW_HIDE );
//                break;
//            }
//
//        case WM_CANCELMODE:
//            iMenuSysKey = 0;
//            MENU_EndMenu( hwnd );
//            if (GetCapture() == hwnd) ReleaseCapture();
//            break;
//
//        case WM_VKEYTOITEM:
//        case WM_CHARTOITEM:
//            return -1;
//
//        case WM_DROPOBJECT:
//            return DRAG_FILE;
//
//        case WM_QUERYDROPOBJECT:
//            return (GetWindowLongA( hwnd, GWL_EXSTYLE ) & WS_EX_ACCEPTFILES) != 0;
//
//        case WM_QUERYDRAGICON:
//            {
//                UINT len;
//
//                HICON hIcon = (HICON)GetClassLongPtrW( hwnd, GCLP_HICON );
//                HINSTANCE instance = (HINSTANCE)GetWindowLongPtrW( hwnd, GWLP_HINSTANCE );
//                if (hIcon) return (LRESULT)hIcon;
//                for(len=1; len<64; len++)
//                    if((hIcon = LoadIconW(instance, MAKEINTRESOURCEW(len))))
//                        return (LRESULT)hIcon;
//                return (LRESULT)LoadIconW(0, (LPWSTR)IDI_APPLICATION);
//            }
//            break;
//
//        case WM_ISACTIVEICON:
//            {
//                WND *wndPtr = WIN_GetPtr( hwnd );
//                BOOL ret = (wndPtr->flags & WIN_NCACTIVATED) != 0;
//                WIN_ReleasePtr( wndPtr );
//                return ret;
//            }
//
//        case WM_NOTIFYFORMAT:
//          if (IsWindowUnicode(hwnd)) return NFR_UNICODE;
//          else return NFR_ANSI;
//
//        case WM_QUERYOPEN:
//        case WM_QUERYENDSESSION:
//            return 1;
//
//        case WM_SETICON:
//            {
//                HICON ret;
//                WND *wndPtr = WIN_GetPtr( hwnd );
//
//                switch(wParam)
//                {
//                case ICON_SMALL:
//                    ret = wndPtr->hIconSmall;
//                    wndPtr->hIconSmall = (HICON)lParam;
//                    break;
//                case ICON_BIG:
//                    ret = wndPtr->hIcon;
//                    wndPtr->hIcon = (HICON)lParam;
//                    break;
//                default:
//                    ret = 0;
//                    break;
//                }
//                WIN_ReleasePtr( wndPtr );
//
//                USER_Driver->pSetWindowIcon( hwnd, wParam, (HICON)lParam );
//
//                if( (GetWindowLongW( hwnd, GWL_STYLE ) & WS_CAPTION) == WS_CAPTION )
//                    NC_HandleNCPaint( hwnd , (HRGN)1 );  /* Repaint caption */
//
//                return (LRESULT)ret;
//            }
//
//        case WM_GETICON:
//            {
//                HICON ret;
//                WND *wndPtr = WIN_GetPtr( hwnd );
//
//                switch(wParam)
//                {
//                case ICON_SMALL:
//                    ret = wndPtr->hIconSmall;
//                    break;
//                case ICON_BIG:
//                    ret = wndPtr->hIcon;
//                    break;
//                case ICON_SMALL2:
//                    ret = wndPtr->hIconSmall;
//                    if (!ret) ret = (HICON)GetClassLongPtrW( hwnd, GCLP_HICONSM );
//                    /* FIXME: should have a default here if class icon is null */
//                    break;
//                default:
//                    ret = 0;
//                    break;
//                }
//                WIN_ReleasePtr( wndPtr );
//                return (LRESULT)ret;
//            }
//
//        case WM_HELP:
//            SendMessageW( GetParent(hwnd), msg, wParam, lParam );
//            break;
//
//        case WM_APPCOMMAND:
//            {
//                HWND parent = GetParent(hwnd);
//                if(!parent)
//                    HOOK_CallHooks(WH_SHELL, HSHELL_APPCOMMAND, wParam, lParam, TRUE);
//                else
//                    SendMessageW( parent, msg, wParam, lParam );
//                break;
//            }
//        case WM_KEYF1:
//            {
//                HELPINFO hi;
//
//                hi.cbSize = sizeof(HELPINFO);
//                GetCursorPos( &hi.MousePos );
//                if (MENU_IsMenuActive())
//                {
//                    hi.iContextType = HELPINFO_MENUITEM;
//                    hi.hItemHandle = MENU_IsMenuActive();
//                    hi.iCtrlId = MenuItemFromPoint( hwnd, hi.hItemHandle, hi.MousePos );
//                    hi.dwContextId = GetMenuContextHelpId( hi.hItemHandle );
//                }
//                else
//                {
//                    hi.iContextType = HELPINFO_WINDOW;
//                    hi.hItemHandle = hwnd;
//                    hi.iCtrlId = GetWindowLongPtrA( hwnd, GWLP_ID );
//                    hi.dwContextId = GetWindowContextHelpId( hwnd );
//                }
//                SendMessageW( hwnd, WM_HELP, 0, (LPARAM)&hi );
//                break;
//            }
//
//        case WM_INPUTLANGCHANGEREQUEST:
//            ActivateKeyboardLayout( (HKL)lParam, 0 );
//            break;
//
//        case WM_INPUTLANGCHANGE:
//            {
//                int count = 0;
//                HWND *win_array = WIN_ListChildren( hwnd );
//
//                if (!win_array)
//                    break;
//                while (win_array[count])
//                    SendMessageW( win_array[count++], WM_INPUTLANGCHANGE, wParam, lParam);
//                HeapFree(GetProcessHeap(),0,win_array);
//                break;
//            }
//
        }

        return 0;
    }

    static private int DEFWND_ControlColor(int hDC, int ctlType) {
        if( ctlType == CTLCOLOR_SCROLLBAR)
        {
            int hb = SysParams.GetSysColorBrush(COLOR_SCROLLBAR);
            int bk = SysParams.GetSysColor(COLOR_3DHILIGHT);
            WinDC.SetTextColor(hDC, SysParams.GetSysColor(COLOR_3DFACE));
            WinDC.SetBkColor(hDC, bk);

            /* if COLOR_WINDOW happens to be the same as COLOR_3DHILIGHT
             * we better use 0x55aa bitmap brush to make scrollbar's background
             * look different from the window background.
             */
            if (bk == SysParams.GetSysColor(COLOR_WINDOW))
                return StaticData.SYSCOLOR_55AABrush;

            GdiObj.UnrealizeObject(hb);
            return hb;
        }

        WinDC.SetTextColor(hDC, SysParams.GetSysColor(COLOR_WINDOWTEXT));

        if ((ctlType == CTLCOLOR_EDIT) || (ctlType == CTLCOLOR_LISTBOX)) {
            WinDC.SetBkColor(hDC, SysParams.GetSysColor(COLOR_WINDOW));
            return SysParams.GetSysColorBrush(COLOR_WINDOW);
        }
        WinDC.SetBkColor( hDC, SysParams.GetSysColor(COLOR_3DFACE) );
        return SysParams.GetSysColorBrush(COLOR_3DFACE);
    }
}
