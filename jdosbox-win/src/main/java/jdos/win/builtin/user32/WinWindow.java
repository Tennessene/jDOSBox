package jdos.win.builtin.user32;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Core_normal;
import jdos.win.Win;
import jdos.win.builtin.gdi32.WinDC;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.system.*;
import jdos.win.utils.Error;
import jdos.win.utils.StringUtil;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

public class WinWindow extends WinObject {
    static public WinWindow create() {
        return new WinWindow(nextObjectId());
    }

    static public WinWindow get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinWindow))
            return null;
        return (WinWindow)object;
    }

    // HWND WINAPI CreateWindowEx(DWORD dwExStyle, LPCTSTR lpClassName, LPCTSTR lpWindowName, DWORD dwStyle, int x, int y, int nWidth, int nHeight, HWND hWndParent, HMENU hMenu, HINSTANCE hInstance, LPVOID lpParam)
    static public int CreateWindowExA(int dwExStyle, int lpClassName, int lpWindowName, int dwStyle, int x, int y, int nWidth, int nHeight, int hWndParent, int hMenu, int hInstance, int lpParam) {
        if ((dwExStyle & WS_EX_MDICHILD)!=0) {
            Win.panic("MDI not supported yet");
        }
        int hwndOwner = 0;

        if (x == 0x80000000) {
            x = 0;
            y = 0;
        }
        if (nWidth == 0x80000000) {
            nWidth = StaticData.screen.getWidth();
            nHeight = StaticData.screen.getHeight();
        }
        /* Find the parent window */
        if (hWndParent == HWND_MESSAGE) {

        } else if (hWndParent!=0) {
            if ((dwStyle & (WS_CHILD|WS_POPUP)) != WS_CHILD) {
                hwndOwner = hWndParent;
                hWndParent = 0;
            }
        } else {
            if ((dwStyle & (WS_CHILD|WS_POPUP)) == WS_CHILD) {
                warn("No parent for child window\n" );
                SetLastError(ERROR_TLW_WITH_WSCHILD);
                return 0;  /* WS_CHILD needs a parent, but WS_POPUP doesn't */
            }
        }

        if ((dwExStyle & WS_EX_DLGMODALFRAME)!=0 ||
            (((dwExStyle & WS_EX_STATICEDGE)==0) &&
              (dwStyle & (WS_DLGFRAME | WS_THICKFRAME))!=0))
            dwExStyle |= WS_EX_WINDOWEDGE;
        else
            dwExStyle &= ~WS_EX_WINDOWEDGE;

        WinClass winClass;
        if (lpClassName<=0xFFFF) {
            winClass = WinClass.get(lpClassName);
        } else {
            String className = StringUtil.getString(lpClassName);
            winClass = WinSystem.getCurrentProcess().classNames.get(className.toLowerCase());
        }
        if (winClass == null) {
            SetLastError(ERROR_CANNOT_FIND_WND_CLASS);
            return 0;
        }
        WinWindow wndPtr = WinWindow.create();
        int hwnd = wndPtr.getHandle();

        /* Fill the window structure */
        wndPtr.thread = Scheduler.getCurrentThread();
        wndPtr.hInstance = hInstance;
        wndPtr.winClass = winClass;
        wndPtr.cbWndExtra = winClass.cbWndExtra;
        wndPtr.winproc = winClass.eip;
        wndPtr.text           = null;
        wndPtr.dwStyle        = dwStyle & ~WS_VISIBLE;
        wndPtr.dwExStyle      = dwExStyle;
        wndPtr.wIDmenu        = 0;
        wndPtr.helpContext    = 0;
        //wndPtr->pScroll        = NULL;
        wndPtr.userdata       = 0;
        wndPtr.hIcon          = 0;
        wndPtr.hIconSmall     = 0;
        wndPtr.hSysMenu       = 0;
        wndPtr.parent         = hWndParent;
        wndPtr.owner          = hwndOwner;
        wndPtr.lastActivePopup = hwnd;

        //if ((dwStyle & WS_SYSMENU)!=0) SetSystemMenu(hwnd, 0);

        /*
         * Correct the window styles.
         *
         * It affects only the style loaded into the WIN structure.
         */

        if ((wndPtr.dwStyle & (WS_CHILD | WS_POPUP)) != WS_CHILD)
        {
            wndPtr.dwStyle |= WS_CLIPSIBLINGS;
            if ((wndPtr.dwStyle & WS_POPUP)==0)
                wndPtr.dwStyle |= WS_CAPTION;
        }

        /*
         * WS_EX_WINDOWEDGE appears to be enforced based on the other styles, so
         * why does the user get to set it?
         */

        if ((wndPtr.dwExStyle & WS_EX_DLGMODALFRAME)!=0 ||
              (wndPtr.dwStyle & (WS_DLGFRAME | WS_THICKFRAME))!=0)
            wndPtr.dwExStyle |= WS_EX_WINDOWEDGE;
        else
            wndPtr.dwExStyle &= ~WS_EX_WINDOWEDGE;

        if ((wndPtr.dwStyle & (WS_CHILD | WS_POPUP))==0) {
            wndPtr.flags |= WIN_NEED_SIZE;
        }

        /* Set the window menu */
        if ((wndPtr.dwStyle & (WS_CHILD | WS_POPUP)) != WS_CHILD) {
            if (hMenu!=0) {
                if (WinMenu.SetMenu(hwnd, hMenu)==0) {
                    wndPtr.close();
                    return 0;
                }
            } else if (winClass.pMenuName!=0) {
                hMenu = WinMenu.LoadMenuA(hInstance, winClass.pMenuName);
                if (hMenu!=0) WinMenu.SetMenu(hwnd, hMenu);
            }
        }
        wndPtr.wIDmenu = hMenu;

        /* call the WH_CBT hook */
        // CBT_CREATEWND
            // LPCREATESTRUCT lpcs;
            // HWND  hwndInsertAfter;

        int cbcs = getTempBuffer(CREATESTRUCT.SIZE);
        int cbtc = getTempBuffer(8);
        CREATESTRUCT.write(cbcs, lpParam, hInstance, hMenu, wndPtr.parent, nHeight, nWidth, y, x, dwStyle, lpWindowName, lpClassName, dwExStyle);
        writed(cbtc, cbcs);
        writed(cbtc+4, HWND_TOP);

        if (Hook.HOOK_CallHooks( WH_CBT, HCBT_CREATEWND, hwnd, cbtc)!=0) {
            wndPtr.close();
            return 0;
        }

        /* send the WM_GETMINMAXINFO message and fix the size if needed */
        CREATESTRUCT cs = new CREATESTRUCT(cbcs);

        if ((dwStyle & WS_THICKFRAME)!=0 || (dwStyle & (WS_POPUP | WS_CHILD))==0) {
            // :TODO: min/max stuff
        }

        wndPtr.rectWindow.set(cs.x, cs.y, +cs.x+cs.cx, cs.y+cs.cy);
        System.out.println(wndPtr.handle+" "+wndPtr.rectWindow);
        wndPtr.rectClient=wndPtr.rectWindow.copy();

        /* send WM_NCCREATE */
        if (Message.SendMessageA(wndPtr.handle, WM_NCCREATE, 0, cbcs)==0) {
            warn(hwnd+": aborted by WM_NCCREATE\n");
            wndPtr.close();
            return 0;
        }

        /* send WM_NCCALCSIZE */
        WinRect rect = new WinRect();
        if (WIN_GetRectangles( hwnd, COORDS_PARENT, rect, null)) {
            /* yes, even if the CBT hook was called with HWND_TOP */
            int insert_after = (GetWindowLongA(hwnd, GWL_STYLE ) & WS_CHILD)!=0 ? HWND_BOTTOM : HWND_TOP;
            WinRect client_rect = rect.copy();

            WinWindow parent = WinWindow.get(wndPtr.parent);
            /* the rectangle is in screen coords for WM_NCCALCSIZE when wparam is FALSE */
            if (parent != null)
                parent.windowToScreen(client_rect);
            int pRect = client_rect.allocTemp();
            Message.SendMessageA(wndPtr.handle, WM_NCCALCSIZE, FALSE, pRect);
            client_rect = new WinRect(pRect);
            if (parent != null)
                parent.screenToWindow(client_rect);
            WinPos.SetWindowPos(hwnd, insert_after, client_rect.left, client_rect.top, client_rect.width(), client_rect.height(), SWP_NOACTIVATE);
        }
        else {
            wndPtr.close();
            return 0;
        }

        /* send WM_CREATE */
        if (Message.SendMessageA(wndPtr.handle, WM_CREATE, 0, cbcs) == -1) {
            wndPtr.close();
            return 0;
        }

        /* send the size messages */
        Message.SendMessageA(wndPtr.handle, WM_SIZE, SIZE_RESTORED, MAKELONG(wndPtr.rectWindow.width(), wndPtr.rectWindow.height()));
        Message.SendMessageA(wndPtr.handle, WM_MOVE, 0, MAKELONG(wndPtr.rectWindow.left, wndPtr.rectWindow.top));

        Scheduler.getCurrentThread().windows.add(wndPtr);

        /* Notify the parent window only */
        wndPtr.parentNotify(WM_CREATE);
        if (IsWindow(hwnd)==FALSE)
            return 0;

        if ((dwStyle & WS_VISIBLE)!=0) {
            WinPos.ShowWindow(hwnd, SW_SHOW);
        }

        /* Call WH_SHELL hook */
        if ((wndPtr.dwStyle & WS_CHILD)==0 && wndPtr.owner == 0)
            Hook.HOOK_CallHooks(WH_SHELL, HSHELL_WINDOWCREATED, hwnd, 0);

        return hwnd;
    }

    // BOOL WINAPI DestroyWindow(HWND hWnd)
    public static int DestroyWindow(int hWnd) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null) {
            return FALSE;
        }
        if (hWnd == GetDesktopWindow() || window.getThread().getProcess() != WinSystem.getCurrentProcess()) {
            SetLastError(ERROR_ACCESS_DENIED);
            return FALSE;
        }
        if (Hook.HOOK_CallHooks(WH_CBT, HCBT_DESTROYWND, hWnd, 0)!=0) return FALSE;

        if (WinMenu.MENU_IsMenuActive() == hWnd)
            WinMenu.EndMenu();

        boolean is_child = (WinWindow.GetWindowLongA(hWnd, GWL_STYLE) & WS_CHILD) != 0;

        if (is_child) {
            //if (!USER_IsExitingThread( GetCurrentThreadId() ))
                window.parentNotify(WM_DESTROY );
        } else if (GetWindow(hWnd, GW_OWNER)==0) {
            Hook.HOOK_CallHooks(WH_SHELL, HSHELL_WINDOWDESTROYED, hWnd, 0);
            /* FIXME: clean up palette - see "Internals" p.352 */
        }

        if (IsWindow(hWnd)==0) return TRUE;

          /* Hide the window */
        if ((GetWindowLongA(hWnd, GWL_STYLE ) & WS_VISIBLE)!=0) {
            /* Only child windows receive WM_SHOWWINDOW in DestroyWindow() */
            if (is_child)
                WinPos.ShowWindow(hWnd, SW_HIDE);
            else
                WinPos.SetWindowPos(hWnd, 0, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_HIDEWINDOW );
        }

        if (IsWindow(hWnd)==0) return TRUE;

          /* Recursively destroy owned windows */
        if (!is_child)
        {
            for (;;)
            {
                boolean got_one = false;
                Iterator<WinWindow> children = WinWindow.get(GetDesktopWindow()).getChildren();
                while (children.hasNext()) {
                    WinWindow child = children.next();
                    if (child.owner != hWnd) continue;
                    if (child.getThread() == Scheduler.getCurrentThread()) {
                        DestroyWindow(child.handle);
                        got_one = true;
                        continue;
                    }
                    child.owner = 0;
                }
                if (!got_one) break;
            }
        }

          /* Send destroy messages */

        WIN_SendDestroyMsg(hWnd);
        if (IsWindow(hWnd)==0) return TRUE;

        if (Clipboard.GetClipboardOwner() == hWnd)
            Clipboard.CLIPBOARD_ReleaseOwner();

          /* Destroy the window storage */
        WIN_DestroyWindow(hWnd);
        return TRUE;
    }

    // BOOL WINAPI EnableWindow(HWND hWnd, BOOL bEnable)
    public static int EnableWindow(int hWnd, int bEnable) {
        if (hWnd == HWND_BROADCAST) {
            SetLastError(Error.ERROR_INVALID_PARAMETER);
            return FALSE;
        }
        boolean isDisabled = IsWindowEnabled(hWnd)==0;
        if (bEnable != 0 && isDisabled) {
            WinWindow window = WinWindow.get(hWnd);
            if (window != null) {
                window.dwStyle &=~ WS_DISABLED;
                Message.SendMessageA(hWnd, WM_ENABLE, TRUE, 0);
            }
        } else if (bEnable == 0 && !isDisabled) {
            WinWindow window = WinWindow.get(hWnd);
            if (window != null) {
                Message.SendMessageA(hWnd, WM_CANCELMODE, 0, 0);
                window.dwStyle|=WS_DISABLED;

                if (hWnd == Focus.GetFocus())
                    Focus.SetFocus(0);  /* A disabled window can't have the focus */

                int capture_wnd = Input.GetCapture();
                if (hWnd == capture_wnd || IsChild(hWnd, capture_wnd)!=0)
                    Input.ReleaseCapture();  /* A disabled window can't capture the mouse */

                Message.SendMessageA(hWnd, WM_ENABLE, FALSE, 0);
            }
        }
        return BOOL(isDisabled);
    }

    // BOOL WINAPI EnumWindows(WNDENUMPROC lpEnumFunc, LPARAM lParam)
    public static int EnumWindows(int lpEnumFunc, int lParam) {
        /* We have to build a list of all windows first, to avoid */
        /* unpleasant side-effects, for instance if the callback */
        /* function changes the Z-order of the windows.          */
        WinWindow desktop = WinWindow.get(GetDesktopWindow());
        int[] result = new int[desktop.children.size()];
        if (result.length>0) {
            Iterator<WinWindow> children = desktop.getChildren();
            int i=0;
            while (children.hasNext()) {
                result[i++] = children.next().handle;
            }
            for (i=0;i<result.length;i++) {
                WinSystem.call(lpEnumFunc, result[i], lParam);
                if (CPU_Regs.reg_eax.dword == 0)
                    return FALSE;
            }
        }
        return TRUE;
    }

    // HWND WINAPI FindWindow(LPCTSTR lpClassName, LPCTSTR lpWindowName)
    public static int FindWindowA(int lpClassName, int lpWindowName) {
        return FindWindowExA(0, 0, lpClassName, lpWindowName);
    }

    // HWND WINAPI FindWindowEx(HWND hwndParent, HWND hwndChildAfter, LPCTSTR lpszClass, LPCTSTR lpszWindow)
    public static int FindWindowExA(int hwndParent, int hwndChildAfter, int lpszClass, int lpszWindow) {
        WinClass winClass = null;
        if (lpszClass != 0) {
            if (lpszClass <= 0xFFFF)
                winClass = WinClass.get(lpszClass);
            else
                winClass = (WinClass)getNamedObject(StringUtil.getString(lpszClass));
        }
        String name = null;
        if (lpszWindow != 0)
            name = StringUtil.getString(lpszWindow);
        if (hwndParent == 0)
            hwndParent = StaticData.desktopWindow;
        if (hwndParent == HWND_MESSAGE)
            Win.panic("FindWindowEx HWND_MESSAGE not supported yet");
        WinWindow parent = WinWindow.get(hwndParent);
        if (parent == null)
            return 0;
        Iterator<WinWindow> children = parent.getChildren();
        int buffer = 0;
        int bufferLen = 0;
        if (name != null) {
            bufferLen = name.length()+2;
            buffer = getTempBuffer(bufferLen);
        }
        while (children.hasNext()) {
            WinWindow child = children.next();
            if (hwndChildAfter != 0) {
                if (child.handle == hwndChildAfter)
                    hwndChildAfter = 0;
            } else {
                if (winClass != null && child.winClass != winClass)
                    continue;
                if (name != null) {
                    int count = GetWindowTextA(child.handle, buffer, bufferLen);
                    if (count <0 || StringUtil.strncmp(buffer, name, bufferLen)!=0)
                        continue;
                }
            }
            return child.handle;
        }
        return 0;
    }

    // HWND WINAPI GetAncestor( HWND hwnd, UINT type )
    public static int GetAncestor(int hwnd, int type) {
        WinWindow win;

        if ((win = WinWindow.get(hwnd))==null) {
            SetLastError(ERROR_INVALID_WINDOW_HANDLE);
            return 0;
        }
        switch(type)
        {
        case GA_PARENT:
            return win.parent;
        case GA_ROOT:
            while (win.parent != 0) {
                win = WinWindow.get(win.parent);
            }
            return win.handle;
        case GA_ROOTOWNER:
            while (true) {
                int parent = GetParent(hwnd);
                if (parent != 0)
                    hwnd = parent;
                else
                    break;
            }
            return hwnd;
        }
        return 0;
    }


    // HWND WINAPI GetDesktopWindow(void)
    static public int GetDesktopWindow() {
        return StaticData.desktopWindow;
    }

    // HWND WINAPI GetLastActivePopup(HWND hWnd)
    static public int GetLastActivePopup(int hWnd) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        return window.lastActivePopup;
    }

    // HWND WINAPI GetParent( HWND hwnd )
    static public int GetParent(int hwnd) {
        WinWindow wndPtr;
        int retvalue = 0;

        if ((wndPtr = WinWindow.get(hwnd))==null) {
            SetLastError( ERROR_INVALID_WINDOW_HANDLE );
            return 0;
        }

        if ((wndPtr.dwStyle & WS_POPUP)!=0) retvalue = wndPtr.owner;
        else if ((wndPtr.dwStyle & WS_CHILD)!=0) retvalue = wndPtr.parent;
        return retvalue;
    }

    // HWND WINAPI GetTopWindow(HWND hWnd)
    static public int GetTopWindow(int hWnd) {
        if (hWnd == 0)
            return GetDesktopWindow();
        return GetWindow(hWnd, GW_CHILD);
    }

    // HWND WINAPI GetWindow( HWND hwnd, UINT rel )
    static public int GetWindow(int hwnd, int rel) {
        int retval = 0;

        WinWindow wndPtr = WinWindow.get(hwnd);
        if (wndPtr==null)
        {
            SetLastError( ERROR_INVALID_HANDLE );
            return 0;
        }
        switch(rel)
        {
        case GW_HWNDFIRST:
        {
            WinWindow parent = WinWindow.get(wndPtr.parent);
            if (parent == null)
                return 0;
            return parent.children.getFirst().handle;
        }
        case GW_HWNDLAST:
        {
            WinWindow parent = WinWindow.get(wndPtr.parent);
            if (parent == null)
                return 0;
            return parent.children.getLast().handle;
        }
        case GW_HWNDNEXT:
        {
            WinWindow parent = WinWindow.get(wndPtr.parent);
            if (parent == null)
                return 0;
            int index = parent.children.indexOf(wndPtr);
            if (index+1>=parent.children.size())
                return 0;
            return parent.children.get(index+1).handle;
        }
        case GW_HWNDPREV:
        {
            WinWindow parent = WinWindow.get(wndPtr.parent);
            if (parent == null)
                return 0;
            int index = parent.children.indexOf(wndPtr);
            if (index==0)
                return 0;
            return parent.children.get(index-1).handle;
        }
        case GW_OWNER:
            return wndPtr.owner;
        case GW_CHILD:
            if (wndPtr.children.size()==0)
                return 0;
            return wndPtr.children.getFirst().handle;
        }
        return 0;
    }

    // LONG WINAPI GetWindowLongA( HWND hwnd, INT offset );
    public static int GetWindowLongA(int hwnd, int offset) {
        return WIN_GetWindowLong( hwnd, offset, 4, FALSE );
    }

    // int WINAPI GetWindowText(HWND hWnd, LPTSTR lpString, int nMaxCount)
    public static int GetWindowTextA(int hWnd, int lpString, int nMaxCount) {
        return Message.SendMessageA(hWnd, WM_GETTEXT, nMaxCount, lpString);
    }

    // int WINAPI GetWindowTextLength(HWND hWnd)
    public static int GetWindowTextLengthA(int hWnd) {
        return Message.SendMessageA(hWnd, WM_GETTEXTLENGTH, 0, 0);
    }

    // DWORD WINAPI GetWindowThreadProcessId( HWND hwnd, LPDWORD process )
    static public int GetWindowThreadProcessId(int hwnd, int process) {
        if (hwnd == GetDesktopWindow()) {
            Core_normal.start = 1;
            return Scheduler.getCurrentThread().handle;
        }
        WinWindow ptr = WinWindow.get(hwnd);

        if (ptr == null) {
            SetLastError(ERROR_INVALID_WINDOW_HANDLE);
            return 0;
        }
        if (process != 0)
            writed(process, ptr.thread.getProcess().handle);
        return ptr.thread.handle;
    }

    // BOOL WINAPI IsChild(HWND hWndParent, HWND hWnd)
    static public int IsChild(int hWndParent, int hWnd) {
        WinWindow parent = WinWindow.get(hWndParent);
        WinWindow child = WinWindow.get(hWnd);
        if (parent == null)
            return FALSE;
        return BOOL(parent.children.contains(child));
    }

    // BOOL WINAPI IsWindow( HWND hwnd )
    public static int IsWindow(int hwnd) {
        return BOOL(WinWindow.get(hwnd)!=null);
    }

    // BOOL WINAPI IsWindowEnabled(HWND hWnd)
    public static int IsWindowEnabled(int hWnd) {
        return (GetWindowLongA( hWnd, GWL_STYLE ) & WS_DISABLED)==0?TRUE:FALSE;
    }

    // BOOL WINAPI IsWindowVisible(HWND hWnd)
    public static int IsWindowVisible(int hWnd) {
        WinWindow window = WinWindow.get(hWnd);

        while (window != null && (window.dwStyle & WS_VISIBLE) != 0) {
            if (window.parent == 0)
                return TRUE;
            window = window.parent();
        }
        return FALSE;
    }

    // HWND WINAPI SetParent(HWND hWndChild, HWND hWndNewParent)
    public static int SetParent(int hWndChild, int hWndNewParent) {
        if (hWndChild == HWND_BROADCAST || hWndNewParent==HWND_BROADCAST) {
            SetLastError(ERROR_INVALID_PARAMETER);
            return 0;
        }

        if (hWndNewParent==0) hWndNewParent = GetDesktopWindow();
        else if (hWndNewParent == HWND_MESSAGE) {
            Win.panic("SetParent to HWND_MESSAGE not implemented yet");
        }

        if (IsWindow(hWndNewParent)==0) {
            SetLastError( ERROR_INVALID_WINDOW_HANDLE );
            return 0;
        }

        /* Some applications try to set a child as a parent */
        if (IsChild(hWndChild, hWndNewParent)!=0) {
            SetLastError( ERROR_INVALID_PARAMETER );
            return 0;
        }

        /* Windows hides the window first, then shows it again
         * including the WM_SHOWWINDOW messages and all */
        int was_visible = WinPos.ShowWindow(hWndChild, SW_HIDE);

        WinWindow wndPtr = WinWindow.get(hWndChild);
        if (wndPtr == null || wndPtr.getThread().getProcess() != WinSystem.getCurrentProcess() || hWndChild == StaticData.desktopWindow) return 0;

        int old_parent = wndPtr.parent;
        if (wndPtr.getParent()!=null) {
            wndPtr.getParent().children.remove(wndPtr);
        }
        if (hWndNewParent == StaticData.desktopWindow)
            wndPtr.parent = 0;
        else
            wndPtr.parent = hWndNewParent;

        /* SetParent additionally needs to make hwnd the topmost window
           in the x-order and send the expected WM_WINDOWPOSCHANGING and
           WM_WINDOWPOSCHANGED notification messages.
        */
        WinPos.SetWindowPos(hWndChild, HWND_TOP, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE | (was_visible!=0 ? SWP_SHOWWINDOW : 0) );
        /* FIXME: a WM_MOVE is also generated (in the DefWindowProc handler
         * for WM_WINDOWPOSCHANGED) in Windows, should probably remove SWP_NOMOVE */

        return old_parent;
    }

    // LONG WINAPI SetWindowLong(HWND hWnd, int nIndex, LONG dwNewLong)
    static public int SetWindowLongA(int hWnd, int nIndex, int dwNewLong) {
        return WIN_SetWindowLong(hWnd, nIndex, 4, dwNewLong);
    }

    // BOOL WINAPI SetWindowText(HWND hWnd, LPCTSTR lpString)
    static public int SetWindowTextA(int hWnd, int lpString) {
        if (hWnd == HWND_BROADCAST) {
            SetLastError(Error.ERROR_INVALID_PARAMETER);
            return FALSE;
        }
        return Message.SendMessageA(hWnd, WM_SETTEXT, 0, lpString);
    }

    static private int WIN_GetWindowLong(int hwnd, int offset, int size, int unicode) {
        WinWindow wndPtr;

        if (offset == GWLP_HWNDPARENT) {
            int parent = GetAncestor( hwnd, GA_PARENT );
            if (parent == 0) parent = GetWindow( hwnd, GW_OWNER );
            return parent;
        }

        if ((wndPtr = WinWindow.get(hwnd))==null) {
            SetLastError( ERROR_INVALID_WINDOW_HANDLE );
            return 0;
        }

        if (offset == GWLP_WNDPROC && (!wndPtr.isInCurrentProcess() || wndPtr.isDesktop())) {
            SetLastError( ERROR_ACCESS_DENIED );
            return 0;
        }

        if (offset >= 0) {
            if (offset > wndPtr.cbWndExtra - size) {
                warn("Invalid offset "+offset);
                SetLastError( ERROR_INVALID_INDEX );
                return 0;
            }
            Integer result = wndPtr.extra.get(offset);
            if (result == null)
                return 0;
            return result;
        }

        switch(offset)
        {
            case GWLP_USERDATA:  return wndPtr.userdata;
            case GWL_STYLE:      return wndPtr.dwStyle;
            case GWL_EXSTYLE:    return wndPtr.dwExStyle;
            case GWLP_ID:        return wndPtr.wIDmenu;
            case GWLP_HINSTANCE: return wndPtr.hInstance;
            case GWLP_WNDPROC:   return wndPtr.winproc;
            default:
                warn("Unknown offset "+offset);
                SetLastError( ERROR_INVALID_INDEX );
                break;
        }
        return 0;
    }

    static private int WIN_SetWindowLong(int hwnd, int offset, int size, int newval) {
        if (hwnd==HWND_BROADCAST) {
            SetLastError( ERROR_INVALID_PARAMETER );
            return FALSE;
        }

        WinWindow wndPtr = WinWindow.get(hwnd);

        if (wndPtr == null) {
            SetLastError( ERROR_INVALID_WINDOW_HANDLE );
            return 0;
        }
        if (hwnd == StaticData.desktopWindow) {
            /* can't change anything on the desktop window */
            SetLastError( ERROR_ACCESS_DENIED );
            return 0;
        }

        switch( offset ) {
            case GWL_STYLE:
            {
                int result = wndPtr.dwStyle;
                int style = getTempBuffer(8); //
                writed(style, wndPtr.dwStyle);
                writed(style + 4, newval);
                Message.SendMessageA(hwnd, WM_STYLECHANGING, GWL_STYLE, style);
                wndPtr = WinWindow.get(hwnd);
                if (wndPtr == null)
                    return 0;
                newval = readd(style+4);
                /* WS_CLIPSIBLINGS can't be reset on top-level windows */
                if (wndPtr.parent == 0) newval |= WS_CLIPSIBLINGS;
                wndPtr.dwStyle = newval;
                Message.SendMessageA(hwnd, WM_STYLECHANGED, GWL_STYLE, style);
                return result;
            }
            case GWL_EXSTYLE:
            {
                int result = wndPtr.dwExStyle;
                int style = getTempBuffer(8); //
                writed(style, wndPtr.dwExStyle);
                writed(style+4, newval);
                Message.SendMessageA(hwnd, WM_STYLECHANGING, GWL_EXSTYLE, style);
                wndPtr = WinWindow.get(hwnd);
                if (wndPtr == null)
                    return 0;
                newval = readd(style+4);
                /* WS_EX_TOPMOST can only be changed through SetWindowPos */
                newval = (newval & ~WS_EX_TOPMOST) | (wndPtr.dwExStyle & WS_EX_TOPMOST);
                /* WS_EX_WINDOWEDGE depends on some other styles */
                if ((newval & WS_EX_DLGMODALFRAME)!=0 || (wndPtr.dwStyle & WS_THICKFRAME)!=0)
                    newval |= WS_EX_WINDOWEDGE;
                else if ((wndPtr.dwStyle & (WS_CHILD|WS_POPUP))!=0)
                    newval &= ~WS_EX_WINDOWEDGE;
                wndPtr.dwExStyle = newval;
                Message.SendMessageA(hwnd, WM_STYLECHANGED, GWL_EXSTYLE, style);
                return result;
            }
            case GWLP_HWNDPARENT:
                if (wndPtr.parent == 0) {
                    int result = wndPtr.owner;
                    wndPtr.owner = newval;
                    return result;
                } else {
                    return SetParent( hwnd, newval);
                }
            case GWLP_WNDPROC:
            {
                if (wndPtr.getThread().getProcess() != WinSystem.getCurrentProcess()) {
                    SetLastError( ERROR_ACCESS_DENIED );
                    return 0;
                }
                int result = wndPtr.winproc;
                wndPtr.winproc = newval;
                return result;
            }
            case GWLP_ID:
            {
                int result = wndPtr.id;
                wndPtr.id = newval;
                return result;
            }
            case GWLP_HINSTANCE:
            {
                int result = wndPtr.hInstance;
                wndPtr.hInstance = newval;
                return result;
            }
            case GWLP_USERDATA:
            {
                int result = wndPtr.userdata;
                wndPtr.userdata = newval;
                return result;
            }
            default:
                if (offset < 0 || offset > wndPtr.cbWndExtra - size) {
                    warn("SetWindowLong Invalid offset "+offset);
                    SetLastError( ERROR_INVALID_INDEX );
                    return 0;
                }
                Integer old = wndPtr.extra.get(offset);
                wndPtr.extra.put(offset, newval);
                if (old != null)
                    return old;
                return 0;
        }
    }


    public static boolean WIN_GetRectangles(int hwnd, int relative, WinRect rectWindow, WinRect rectClient ) {
        WinWindow win = WinWindow.get(hwnd);
        boolean ret = true;

        if (win == null) {
            SetLastError(ERROR_INVALID_WINDOW_HANDLE);
            return false;
        }
        if (win.isDesktop())
        {
            WinRect rect = new WinRect();
            rect.left = rect.top = 0;
            rect.right  = SysParams.GetSystemMetrics(SM_CXSCREEN);
            rect.bottom = SysParams.GetSystemMetrics(SM_CYSCREEN);
            if (rectWindow!=null) rectWindow.copy(rect);
            if (rectClient!=null) rectClient.copy(rect);
            return true;
        }

        WinRect window_rect = win.rectWindow.copy();
        WinRect client_rect = win.rectClient.copy();

        switch (relative)
        {
        case COORDS_CLIENT:
            window_rect.offset(-win.rectClient.left, -win.rectClient.top);
            client_rect.offset(-win.rectClient.left, -win.rectClient.top);
            break;
        case COORDS_WINDOW:
            window_rect.offset(-win.rectWindow.left, -win.rectWindow.top);
            client_rect.offset(-win.rectWindow.left, -win.rectWindow.top);
            break;
        case COORDS_PARENT:
            break;
        case COORDS_SCREEN:
            while (win.parent!=0) {
                win = win.parent();
                if (win.isDesktop()) break;
                if (win.parent!=0) {
                    window_rect.offset(win.rectClient.left, win.rectClient.top);
                    client_rect.offset(win.rectClient.left, win.rectClient.top );
                }
            }
            break;
        }
        if (rectWindow != null) rectWindow.copy(window_rect);
        if (rectClient!=null) rectClient.copy(client_rect);
        return true;
    }

    static private void WIN_SendDestroyMsg(int hwnd) {
        GuiThreadInfo info = Scheduler.getCurrentThread().GetGUIThreadInfo();

        if (hwnd == info.hwndCaret) Caret.DestroyCaret();
        if (hwnd == info.hwndActive) WinPos.WINPOS_ActivateOtherWindow(hwnd);

        /*
         * Send the WM_DESTROY to the window.
         */
        Message.SendMessageA(hwnd, WM_DESTROY, 0, 0);

        /*
         * This WM_DESTROY message can trigger re-entrant calls to DestroyWindow
         * make sure that the window still exists when we come back.
         */
        if (IsWindow(hwnd)!=0) {
            Iterator<WinWindow> children = WinWindow.get(hwnd).getChildren();
            while (children.hasNext())  {
                WinWindow child = children.next();
                if (IsWindow(child.handle)!=0) WIN_SendDestroyMsg(child.handle);
            }
        } else {
          warn("destroyed itself while in WM_DESTROY!\n");
        }
    }

    static public int WIN_DestroyWindow(int hwnd) {
        WinWindow window = WinWindow.get(hwnd);
        if (window == null)
            return 0;

        while (window.children.size()>0) {
            WinWindow child = window.children.get(0);
            WIN_DestroyWindow(child.handle);
        }

        WinWindow parent;
        if (window.parent != 0)
            parent = WinWindow.get(window.parent);
        else
            parent = WinWindow.get(GetDesktopWindow());
        parent.children.remove(window);

        /*
         * Send the WM_NCDESTROY to the window being destroyed.
         */
        Message.SendMessageA(hwnd, WM_NCDESTROY, 0, 0);

        /* FIXME: do we need to fake QS_MOUSEMOVE wakebit? */

        /* free resources associated with the window */
        if (IsWindow(hwnd)==0)
            return 0;
        if ((window.dwStyle & (WS_CHILD | WS_POPUP)) != WS_CHILD) {
            if (window.wIDmenu!=0)
                WinMenu.DestroyMenu(window.wIDmenu);
        }
        if (window.hSysMenu!=0) WinMenu.DestroyMenu(window.hSysMenu);
        window.close();
        return 0;
    }

    private WinWindow(int id) {
        super(id);
    }

    public WinTimer timer = new WinTimer(handle);

    public WinRect rectWindow = new WinRect();
    public WinRect rectClient = new WinRect();
    public int dwStyle;
    private int dwExStyle;
    private int cbWndExtra;
    private int id = 0;
    private int hInstance;
    int wIDmenu;
    public int parent;
    public int owner;
    private WinThread thread;
    protected int winproc;
    private int wExtra;
    private int userdata;
    public String text="";
    public int helpContext;
    private int hIcon;
    private int hIconSmall;
    public int hSysMenu;
    public int flags;
    public WinPoint min_pos = new WinPoint();
    public WinPoint max_pos = new WinPoint();
    public WinRect normal_rect = new WinRect(0, 0, 640, 480);
    public DialogInfo dlgInfo = null;
    private Hashtable<Integer, Integer> extra = new Hashtable<Integer, Integer>();
    public Hashtable<String, Integer> props = new Hashtable<String, Integer>();
    public int lastActivePopup;
    private WinDC dc;
    WinClass winClass;

    public boolean isActive = false;
    public WinRect invalidationRect = null;

    public LinkedList<WinWindow> children = new LinkedList<WinWindow>(); // first one is on top

    // Used by desktop
    public WinWindow(int id, WinClass winClass, String name) {
        super(id);
        this.winClass = winClass;
        this.name = name;
        this.rectWindow = new WinRect(0, 0, WinSystem.getScreenWidth(), WinSystem.getScreenHeight());
        this.rectClient = new WinRect(0, 0, this.rectWindow.width(), this.rectWindow.height());
    }

    public boolean isInCurrentProcess() {
        return thread.getProcess() == WinSystem.getCurrentProcess();
    }

    public boolean isDesktop() {
        return handle == StaticData.desktopWindow;
    }

    public WinWindow parent() {
        return WinWindow.get(parent);
    }

    public void invalidate(WinRect rect) {
        if (rect == null)
            rect = new WinRect(0, 0, rectClient.width(), rectClient.height());
        else
            rect = rect.copy();
        if (invalidationRect == null) {
            invalidationRect = rect;
            if (thread != null)
                thread.paintReady();
        } else {
            invalidationRect.merge(rect);
        }
    }

    public void validate() {
        invalidationRect = null;
    }

    public boolean needsPainting() {
        return invalidationRect != null && (dwStyle & WS_VISIBLE)!=0;
    }

    public WinWindow findWindowFromPoint(int x, int y) {
        Iterator<WinWindow> i = children.iterator();
        while (i.hasNext()) {
            WinWindow child = i.next();
            if ((child.dwStyle & WS_VISIBLE)!=0 &&  child.rectWindow.contains(x, y)) {
                return child.findWindowFromPoint(x-child.rectWindow.left, y-child.rectWindow.top);
            }
        }
        return this;
    }

    public int findWindow(String className, String windowName) {
        if (this.winClass.className.equals(className) || this.name.equals(windowName))
            return getHandle();
        Iterator<WinWindow> i = children.iterator();
        while (i.hasNext()) {
            WinWindow child = i.next();
            int result = child.findWindow(className, windowName);
            if (result != 0)
                return result;
        }
        return 0;
    }

    public void postMessage(int msg, int wParam, int lParam) {
        thread.postMessage(handle, msg, wParam, lParam);
    }

    public WinThread getThread() {
        return thread;
    }

    public WinWindow getParent() {
        if ((dwStyle & WS_POPUP)!=0) return get(owner);
        else if ((dwStyle & WS_CHILD)!=0) return get(parent);
        return null;
    }

    public Iterator<WinWindow> getChildren() {
        return children.iterator();
    }

    public WinDC getDC() {
        WinDC dc;

        dc = this.dc;
        if (dc == null) {
            dc = winClass.dc;
        }

        int class_style = winClass.style;
        if (dc==null) {
            if ((class_style & CS_CLASSDC)!=0) {
                dc = WinDC.create();
                winClass.dc = dc;
                dc.makePermanent();
            } else if ((class_style & CS_OWNDC)!=0) {
                dc = WinDC.create();
                this.dc = dc;
                dc.makePermanent();
            }
        }
        if (dc == null)
            dc = WinDC.create(StaticData.screen, false);
        WinPoint p = getScreenOffset();
        if ((class_style & CS_PARENTDC)!=0 && parent != 0) {
            WinWindow pParent = WinWindow.get(parent);
            WinPoint ptParent = pParent.getScreenOffset();
            dc.setOffset(p.x, p.y, ptParent.x-p.x, ptParent.y-p.y, pParent.rectClient.width(), pParent.rectClient.height());
        } else {
            dc.setOffset(p.x, p.y, 0, 0, rectClient.width(), rectClient.height());
        }
        return dc;
    }

    public WinPoint getScreenOffset() {
        WinPoint offset = new WinPoint();
        WinWindow window = this;

        while (window != null) {
            offset.x += window.rectClient.left;
            offset.y += window.rectClient.top;
            window = window.parent();
        }
        return offset;
    }

    public void windowToScreen(WinRect rect) {
        WinPoint p = getScreenOffset();
        rect.offset(p.x, p.y);
    }

    public void screenToWindow(WinRect rect) {
        WinPoint p = getScreenOffset();
        rect.offset(-p.x, -p.y);
    }

    public void windowToScreen(WinPoint pt) {
        WinPoint p = getScreenOffset();
        pt.offset(p.x, p.y);
    }

    public void screenToWindow(WinPoint pt) {
        WinPoint p = getScreenOffset();
        pt.offset(-p.x, -p.y);
    }

    public void parentNotify(int msg) {
        if ((dwStyle & (WS_CHILD | WS_POPUP)) == WS_CHILD && (dwExStyle & WS_EX_NOPARENTNOTIFY)==0) {
            Message.SendMessageA(GetParent(handle), WM_PARENTNOTIFY, MAKEWPARAM(msg, wIDmenu), handle);
        }
    }

    public String toString() {
        return "handle="+handle+" text="+text;
    }
}
