package jdos.win.builtin.user32;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.kernel.WinCallback;
import jdos.win.system.WinRect;
import jdos.win.utils.Ptr;
import jdos.win.utils.StringUtil;

public class StaticWindow extends WinAPI {
    static public void registerClass(WinProcess process) {
        WinClass winClass = WinClass.create();
        winClass.className = "STATIC";
        winClass.style = CS_DBLCLKS | CS_PARENTDC;
        winClass.hCursor = WinCursor.LoadCursorA(0, IDC_ARROW);
        winClass.cbWndExtra = 8; // HFONT+HICON
        int cb = WinCallback.addCallback(static_proc);
        winClass.eip = process.loader.registerFunction(cb);
        process.classNames.put(winClass.className.toLowerCase(), winClass);
    }

    static private Callback.Handler static_proc = new HandlerBase() {
        public java.lang.String getName() {
            return "STATIC.proc";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int Msg = CPU.CPU_Pop32();
            int wParam = CPU.CPU_Pop32();
            int lParam = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = StaticWndProc_common(hWnd, Msg, wParam, lParam);
        }
    };

    static final private int HFONT_GWL_OFFSET = 0;
    static final private int HICON_GWL_OFFSET = 4;

    static private interface StaticPaint {
        public void paint(int hWnd, int hdc, int style);
    }

    /***********************************************************************
     *           STATIC_SetIcon
     *
     * Set the icon for an SS_ICON control.
     */
    static private int STATIC_SetIcon(int hwnd, int hicon, int style ) {
        int prevIcon;

        if ((style & SS_TYPEMASK) != SS_ICON) return 0;
        WinIcon icon = WinIcon.get(hicon);
        if (hicon!=0 && icon == null) {
            warn("hicon != 0, but invalid");
            return 0;
        }
        prevIcon = WinWindow.SetWindowLongA(hwnd, HICON_GWL_OFFSET, hicon);
        if (hicon!=0 && (style & SS_CENTERIMAGE)==0 && (style & SS_REALSIZECONTROL)==0) {
            /* Windows currently doesn't implement SS_RIGHTJUST */
            /*
            if ((style & SS_RIGHTJUST) != 0)
            {
                RECT wr;
                GetWindowRect(hwnd, &wr);
                SetWindowPos( hwnd, 0, wr.right - info->nWidth, wr.bottom - info->nHeight,
                              info->nWidth, info->nHeight, SWP_NOACTIVATE | SWP_NOZORDER );
            }
            else */
            {
                WinPos.SetWindowPos(hwnd, 0, 0, 0, icon.cx, icon.cy, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOZORDER);
            }
        }
        return prevIcon;
    }

    /***********************************************************************
     *           STATIC_SetBitmap
     *
     * Set the bitmap for an SS_BITMAP control.
     */
    static private int STATIC_SetBitmap(int hwnd, int hBitmap, int style) {
        int hOldBitmap;

        if ((style & SS_TYPEMASK) != SS_BITMAP) return 0;
        WinBitmap bitmap = WinBitmap.get(hBitmap);
        if (hBitmap!=0 && bitmap == null) {
            warn("hBitmap != 0, but it's not a bitmap");
            return 0;
        }
        hOldBitmap = WinWindow.SetWindowLongA(hwnd, HICON_GWL_OFFSET, hBitmap);
        if (hBitmap!=0 && (style & SS_CENTERIMAGE)==0 && (style & SS_REALSIZECONTROL)==0) {
            /* Windows currently doesn't implement SS_RIGHTJUST */
            /*
            if ((style & SS_RIGHTJUST) != 0)
            {
                RECT wr;
                GetWindowRect(hwnd, &wr);
                SetWindowPos( hwnd, 0, wr.right - bm.bmWidth, wr.bottom - bm.bmHeight,
                              bm.bmWidth, bm.bmHeight, SWP_NOACTIVATE | SWP_NOZORDER );
            }
            else */
            {
                WinPos.SetWindowPos(hwnd, 0, 0, 0, bitmap.getWidth(), bitmap.getHeight(), SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOZORDER);
            }

        }
        return hOldBitmap;
    }

    /***********************************************************************
     *           STATIC_SetEnhMetaFile
     *
     * Set the enhanced metafile for an SS_ENHMETAFILE control.
     */
    static private int STATIC_SetEnhMetaFile(int hwnd, int hEnhMetaFile, int style) {
        if ((style & SS_TYPEMASK) != SS_ENHMETAFILE) return 0;
        WinEnhancedMetaFile metaFile = WinEnhancedMetaFile.get(hEnhMetaFile);
        if (hEnhMetaFile!=0 && metaFile == null) {
            warn("hEnhMetaFile != 0, but it's not an enhanced metafile");
            return 0;
        }
        return WinWindow.SetWindowLongA(hwnd, HICON_GWL_OFFSET, hEnhMetaFile);
    }

    /***********************************************************************
     *           STATIC_GetImage
     *
     * Gets the bitmap for an SS_BITMAP control, the icon/cursor for an
     * SS_ICON control or the enhanced metafile for an SS_ENHMETAFILE control.
     */
    static int STATIC_GetImage(int hwnd, int wParam, int style) {
        switch(style & SS_TYPEMASK)
        {
            case SS_ICON:
                if ((wParam != IMAGE_ICON) &&
                    (wParam != IMAGE_CURSOR)) return NULL;
                break;
            case SS_BITMAP:
                if (wParam != IMAGE_BITMAP) return NULL;
                break;
            case SS_ENHMETAFILE:
                if (wParam != IMAGE_ENHMETAFILE) return NULL;
                break;
            default:
                return NULL;
        }
        return WinWindow.GetWindowLongA( hwnd, HICON_GWL_OFFSET );
    }

    /***********************************************************************
     *           STATIC_LoadIconA
     *
     * Load the icon for an SS_ICON control.
     */
    static int STATIC_LoadIconA(int hInstance, int name, int style) {
        int hicon = 0;

        if (hInstance!=0) {
            if ((style & SS_REALSIZEIMAGE) != 0)
                hicon = Resource.LoadImageA(hInstance, name, IMAGE_ICON, 0, 0, LR_SHARED);
            else
            {
                hicon = WinIcon.LoadIconA( hInstance, name );
                if (hicon==0) hicon = WinCursor.LoadCursorA(hInstance, name);
            }
        }
        if (hicon==0) hicon = WinIcon.LoadIconA(0, name);
        /* Windows doesn't try to load a standard cursor,
           probably because most IDs for standard cursors conflict
           with the IDs for standard icons anyway */
        return hicon;
    }

//    /***********************************************************************
//     *           STATIC_LoadIconW
//     *
//     * Load the icon for an SS_ICON control.
//     */
//    static HICON STATIC_LoadIconW( HINSTANCE hInstance, LPCWSTR name, DWORD style )
//    {
//        HICON hicon = 0;
//
//        if (hInstance && ((ULONG_PTR)hInstance >> 16))
//        {
//            if ((style & SS_REALSIZEIMAGE) != 0)
//                hicon = LoadImageW(hInstance, name, IMAGE_ICON, 0, 0, LR_SHARED);
//            else
//            {
//                hicon = LoadIconW( hInstance, name );
//                if (!hicon) hicon = LoadCursorW( hInstance, name );
//            }
//        }
//        if (!hicon) hicon = LoadIconW( 0, name );
//        /* Windows doesn't try to load a standard cursor,
//           probably because most IDs for standard cursors conflict
//           with the IDs for standard icons anyway */
//        return hicon;
//    }

    /***********************************************************************
     *           STATIC_TryPaintFcn
     *
     * Try to immediately paint the control.
     */
    static private void STATIC_TryPaintFcn(int hwnd, int full_style) {
        int style = full_style & SS_TYPEMASK;
        int rc = getTempBuffer(WinRect.SIZE);

        WinPos.GetClientRect(hwnd, rc);
        if (UiTools.IsRectEmpty(rc)!=0 && WinWindow.IsWindowVisible(hwnd)!=0 && staticPaintFunc[style]!=null) {
            int hdc = Painting.GetDC(hwnd);
            int hrgn = UiTools.set_control_clipping(hdc, rc);
            staticPaintFunc[style].paint(hwnd, hdc, full_style);
            WinDC.SelectClipRgn(hdc, hrgn);
            if (hrgn!=0)
                GdiObj.DeleteObject(hrgn);
            Painting.ReleaseDC(hwnd, hdc);
        }
    }

    static private int STATIC_SendWmCtlColorStatic(int hwnd, int hdc) {
        int parent = WinWindow.GetParent(hwnd);

        if (parent==0) parent = hwnd;
        int hBrush = Message.SendMessageA(parent, WM_CTLCOLORSTATIC, hdc, hwnd);
        if (hBrush==0) { /* did the app forget to call DefWindowProc ? */
            /* FIXME: DefWindowProc should return different colors if a manifest is present */
            hBrush = DefWnd.DefWindowProcA(parent, WM_CTLCOLORSTATIC, hdc, hwnd);
        }
        return hBrush;
    }

    /***********************************************************************
     *           hasTextStyle
     *
     * Tests if the control displays text.
     */
    static private boolean hasTextStyle(int style) {
        switch(style & SS_TYPEMASK)
        {
            case SS_SIMPLE:
            case SS_LEFT:
            case SS_LEFTNOWORDWRAP:
            case SS_CENTER:
            case SS_RIGHT:
            case SS_OWNERDRAW:
                return true;
        }

        return false;
    }

    /***********************************************************************
     *           StaticWndProc_common
     */
    static private int StaticWndProc_common(int hwnd, int uMsg, int wParam, int lParam) {
        int full_style = WinWindow.GetWindowLongA(hwnd, GWL_STYLE);
        int style = full_style & SS_TYPEMASK;

        if (WinWindow.IsWindow(hwnd)==0) return 0;

        switch (uMsg)
        {
        case WM_CREATE:
            if (style < 0L || style > SS_TYPEMASK) {
                warn("Unknown style "+Ptr.toString(style));
                return -1;
            }
            break;
        case WM_NCDESTROY:
            if (style == SS_ICON) {
                /*
                 * FIXME
                 *           DestroyIcon32( STATIC_SetIcon( wndPtr, 0 ) );
                 *
                 * We don't want to do this yet because DestroyIcon32 is broken. If the icon
                 * had already been loaded by the application the last thing we want to do is
                 * GlobalFree16 the handle.
                 */
                break;
            } else {
                return DefWnd.DefWindowProcA(hwnd, uMsg, wParam, lParam);
            }

        case WM_ERASEBKGND:
            /* do all painting in WM_PAINT like Windows does */
            return 1;

        case WM_PRINTCLIENT:
        case WM_PAINT:
            {
                int ps = getTempBuffer(PAINTSTRUCT.SIZE);
                WinRect rect = new WinRect();
                int hdc = wParam!=0 ? wParam : Painting.BeginPaint(hwnd, ps);
                WinPos.WIN_GetClientRect(hwnd, rect);
                if (staticPaintFunc[style] != null) {
                    int hrgn = UiTools.set_control_clipping(hdc, rect);
                    staticPaintFunc[style].paint(hwnd, hdc, full_style);
                    WinDC.SelectClipRgn(hdc, hrgn);
                    if (hrgn!=0)
                        GdiObj.DeleteObject(hrgn);
                }
                if (wParam==0)
                    Painting.EndPaint(hwnd, ps);
            }
            break;

        case WM_ENABLE:
            STATIC_TryPaintFcn(hwnd, full_style);
            if ((full_style & SS_NOTIFY)!=0) {
                if (wParam!=0) {
                    Message.SendMessageA(WinWindow.GetParent(hwnd), WM_COMMAND, MAKEWPARAM(WinWindow.GetWindowLongA(hwnd,GWLP_ID), STN_ENABLE), hwnd);
                } else {
                    Message.SendMessageA(WinWindow.GetParent(hwnd), WM_COMMAND, MAKEWPARAM(WinWindow.GetWindowLongA(hwnd,GWLP_ID), STN_DISABLE), hwnd);
                }
            }
            break;

        case WM_SYSCOLORCHANGE:
            STATIC_TryPaintFcn(hwnd, full_style);
            break;

        case WM_NCCREATE:
            {
                CREATESTRUCT cs = new CREATESTRUCT(wParam);

                if ((full_style & SS_SUNKEN)!=0)
                    WinWindow.SetWindowLongA( hwnd, GWL_EXSTYLE, WinWindow.GetWindowLongA(hwnd, GWL_EXSTYLE) | WS_EX_STATICEDGE);

                switch (style) {
                case SS_ICON:
                    {
                        STATIC_SetIcon(hwnd, STATIC_LoadIconA(cs.hInstance, cs.lpszName, full_style), full_style);
                    }
                    break;
                case SS_BITMAP:
                    if (cs.hInstance != 0) {
                        STATIC_SetBitmap(hwnd, WinBitmap.LoadBitmapA(cs.hInstance, cs.lpszName), full_style);
                    }
                    break;
                }
                /* SS_ENHMETAFILE: Despite what MSDN says, Windows does not load
                   the enhanced metafile that was specified as the window text. */
            }
            return DefWnd.DefWindowProcA(hwnd, uMsg, wParam, lParam);

        case WM_SETTEXT:
            if (hasTextStyle( full_style )) {
                if (HIWORD(lParam)!=0) {
                    int result = DefWnd.DefWindowProcA(hwnd, uMsg, wParam, lParam);
                    STATIC_TryPaintFcn( hwnd, full_style );
                    return result;
                }
            }
            break;

        case WM_SETFONT:
            if (hasTextStyle( full_style )) {
                WinWindow.SetWindowLongA(hwnd, HFONT_GWL_OFFSET, wParam);
                if (LOWORD(lParam)!=0)
                    Painting.RedrawWindow(hwnd, NULL, 0, RDW_INVALIDATE | RDW_ERASE | RDW_UPDATENOW | RDW_ALLCHILDREN);
            }
            break;

        case WM_GETFONT:
            return WinWindow.GetWindowLongA(hwnd, HFONT_GWL_OFFSET);

        case WM_NCHITTEST:
            if ((full_style & SS_NOTIFY)!=0)
               return HTCLIENT;
            else
               return HTTRANSPARENT;

        case WM_GETDLGCODE:
            return DLGC_STATIC;

        case WM_LBUTTONDOWN:
        case WM_NCLBUTTONDOWN:
            if ((full_style & SS_NOTIFY)!=0)
                Message.SendMessageA(WinWindow.GetParent(hwnd), WM_COMMAND, MAKEWPARAM(WinWindow.GetWindowLongA(hwnd, GWLP_ID), STN_CLICKED), hwnd);
            return 0;

        case WM_LBUTTONDBLCLK:
        case WM_NCLBUTTONDBLCLK:
            if ((full_style & SS_NOTIFY)!=0)
                Message.SendMessageA(WinWindow.GetParent(hwnd), WM_COMMAND, MAKEWPARAM(WinWindow.GetWindowLongA(hwnd, GWLP_ID), STN_DBLCLK), hwnd);
            return 0;

        case STM_GETIMAGE:
            return STATIC_GetImage(hwnd, wParam, full_style);

        case STM_GETICON:
            return STATIC_GetImage(hwnd, IMAGE_ICON, full_style);

        case STM_SETIMAGE:
        {
            int lResult = 0;
            switch(wParam) {
            case IMAGE_BITMAP:
                lResult = STATIC_SetBitmap(hwnd, lParam, full_style);
                break;
            case IMAGE_ENHMETAFILE:
                lResult = STATIC_SetEnhMetaFile(hwnd, lParam, full_style);
                break;
            case IMAGE_ICON:
            case IMAGE_CURSOR:
                lResult = STATIC_SetIcon(hwnd, lParam, full_style);
                break;
            default:
                warn("STM_SETIMAGE: Unhandled type " + wParam);
                break;
            }
            STATIC_TryPaintFcn( hwnd, full_style );
            return lResult;
        }

        case STM_SETICON:
        {
            int lResult = STATIC_SetIcon(hwnd, wParam, full_style);
            STATIC_TryPaintFcn( hwnd, full_style );
            return lResult;
        }
        default:
            return DefWnd.DefWindowProcA(hwnd, uMsg, wParam, lParam);
        }
        return 0;
    }

    static private StaticPaint STATIC_PaintOwnerDrawfn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            DRAWITEMSTRUCT dis = new DRAWITEMSTRUCT();
            int id = WinWindow.GetWindowLongA(hWnd, GWLP_ID);

            dis.CtlType    = ODT_STATIC;
            dis.CtlID      = id;
            dis.itemID     = 0;
            dis.itemAction = ODA_DRAWENTIRE;
            dis.itemState  = WinWindow.IsWindowEnabled(hWnd)!=0 ? 0 : ODS_DISABLED;
            dis.hwndItem   = hWnd;
            dis.hDC        = hdc;
            dis.itemData   = 0;
            WinPos.WIN_GetClientRect(hWnd, dis.rcItem);

            int font = WinWindow.GetWindowLongA(hWnd, HFONT_GWL_OFFSET);
            int oldFont = 0;
            if (font!=0)
                oldFont = WinDC.SelectObject(hdc, font);
            Message.SendMessageA(WinWindow.GetParent(hWnd), WM_CTLCOLORSTATIC, hdc, hWnd);
            Message.SendMessageA(WinWindow.GetParent(hWnd), WM_DRAWITEM, id, dis.allocTemp());
            if (font!=0)
                WinDC.SelectObject(hdc, oldFont);
        }
    };

    static private StaticPaint STATIC_PaintTextfn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            int format;

            WinWindow window = WinWindow.get(hWnd);
            if (window == null)
                return;

            switch (style & SS_TYPEMASK)
            {
                case SS_LEFT:
                    format = DT_LEFT | DT_EXPANDTABS | DT_WORDBREAK;
                    break;
                case SS_CENTER:
                    format = DT_CENTER | DT_EXPANDTABS | DT_WORDBREAK;
                    break;
                case SS_RIGHT:
                    format = DT_RIGHT | DT_EXPANDTABS | DT_WORDBREAK;
                    break;
                case SS_SIMPLE:
                    format = DT_LEFT | DT_SINGLELINE;
                    break;
                case SS_LEFTNOWORDWRAP:
                    format = DT_LEFT | DT_EXPANDTABS;
                    break;
                default:
                    return;
            }

            int rc = getTempBuffer(WinRect.SIZE);
            WinPos.GetClientRect(hWnd, rc);

            if ((WinWindow.GetWindowLongA(hWnd, GWL_EXSTYLE) & WS_EX_RIGHT)!=0)
                format = DT_RIGHT | (format & ~(DT_LEFT | DT_CENTER));

            if ((style & SS_NOPREFIX)!=0)
                format |= DT_NOPREFIX;

            if ((style & SS_TYPEMASK) != SS_SIMPLE) {
                if ((style & SS_CENTERIMAGE)!=0)
                    format |= DT_SINGLELINE | DT_VCENTER;
                if ((style & SS_EDITCONTROL)!=0)
                    format |= DT_EDITCONTROL;
                if ((style & SS_ENDELLIPSIS)!=0)
                    format |= DT_SINGLELINE | DT_END_ELLIPSIS;
                if ((style & SS_PATHELLIPSIS)!=0)
                    format |= DT_SINGLELINE | DT_PATH_ELLIPSIS;
                if ((style & SS_WORDELLIPSIS)!=0)
                    format |= DT_SINGLELINE | DT_WORD_ELLIPSIS;
            }
            int hFont = WinWindow.GetWindowLongA(hWnd, HFONT_GWL_OFFSET);
            int hOldFont = 0;
            if (hFont != 0)
                hOldFont = WinDC.SelectObject(hdc, hFont);

            /* SS_SIMPLE controls: WM_CTLCOLORSTATIC is sent, but the returned
                                   brush is not used */
            int hBrush = STATIC_SendWmCtlColorStatic(hWnd, hdc);

            if ((style & SS_TYPEMASK) != SS_SIMPLE) {
                WinDC.FillRect(hdc, rc, hBrush);
                if (WinWindow.IsWindowEnabled(hWnd)==0)
                    WinDC.SetTextColor(hdc, SysParams.GetSysColor(COLOR_GRAYTEXT));
            }

            if (window.text.length()>0) {
                if (((style & SS_TYPEMASK) == SS_SIMPLE) && (style & SS_NOPREFIX)!=0) {
                    /* Windows uses the faster ExtTextOut() to draw the text and
                       to paint the whole client rectangle with the text background
                       color. Reference: "Static Controls" by Kyle Marsh, 1992 */
                    WinRect rect = new WinRect(rc);
                    WinDC.ExtTextOutA( hdc, rect.left, rect.top, ETO_CLIPPED | ETO_OPAQUE, rc, StringUtil.allocateTempA(window.text), window.text.length(), NULL );
                } else {
                    WinText.DrawTextA(hdc, StringUtil.allocateTempA(window.text), -1, rc, format);
                }
            }
            if (hFont!=0)
                WinDC.SelectObject(hdc, hOldFont);
        }
    };

    static private StaticPaint STATIC_PaintRectfn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            int rc = getTempBuffer(WinRect.SIZE);
            int hBrush;

            WinPos.GetClientRect(hWnd, rc);

            /* FIXME: send WM_CTLCOLORSTATIC */
            switch (style & SS_TYPEMASK)
            {
                case SS_BLACKRECT:
                    hBrush = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DDKSHADOW));
                    WinDC.FillRect(hdc, rc, hBrush);
                break;
                case SS_GRAYRECT:
                    hBrush = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DSHADOW));
                    WinDC.FillRect(hdc, rc, hBrush);
                break;
                case SS_WHITERECT:
                    hBrush = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DHIGHLIGHT));
                    WinDC.FillRect(hdc, rc, hBrush);
                break;
                case SS_BLACKFRAME:
                    hBrush = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DDKSHADOW));
                    UiTools.FrameRect(hdc, rc, hBrush);
                break;
                case SS_GRAYFRAME:
                    hBrush = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DSHADOW));
                    UiTools.FrameRect(hdc, rc, hBrush);
                break;
                case SS_WHITEFRAME:
                    hBrush = WinBrush.CreateSolidBrush(SysParams.GetSysColor(COLOR_3DHIGHLIGHT));
                    UiTools.FrameRect(hdc, rc, hBrush);
                break;
                default:
                    return;
            }
            GdiObj.DeleteObject(hBrush);
        }
    };

    static private StaticPaint STATIC_PaintIconfn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            int rc = getTempBuffer(WinRect.SIZE);
            WinPos.GetClientRect(hWnd, rc);
            int hbrush = STATIC_SendWmCtlColorStatic(hWnd, hdc);
            int hIcon = WinWindow.GetWindowLongA(hWnd, HICON_GWL_OFFSET);
            WinIcon icon = WinIcon.get(hIcon);
            WinDC.FillRect(hdc, rc, hbrush);
            if (icon != null) {
                WinRect iconRect = new WinRect();
                WinRect rect = new WinRect(rc);
                if ((style & SS_CENTERIMAGE)==0)
                    iconRect = rect;
                else {
                    iconRect.left = (rect.right - rect.left) / 2 - icon.cx / 2;
                    iconRect.top = (rect.bottom - rect.top) / 2 - icon.cy / 2;
                    iconRect.right = iconRect.left + icon.cx;
                    iconRect.bottom = iconRect.top + icon.cy;
                }
                WinIcon.DrawIconEx(hdc, iconRect.left, iconRect.top, hIcon, iconRect.right - iconRect.left, iconRect.bottom - iconRect.top, 0, NULL, DI_NORMAL);
            }
        }
    };

    static private StaticPaint STATIC_PaintBitmapfn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            /* message is still sent, even if the returned brush is not used */
            int hbrush = STATIC_SendWmCtlColorStatic(hWnd, hdc);
            int hBitmap = WinWindow.GetWindowLongA(hWnd, HICON_GWL_OFFSET);
            WinBitmap bitmap = WinBitmap.get(hBitmap);
            int hMemDC = 0;
            if (bitmap != null)
                hMemDC = WinDC.CreateCompatibleDC(hdc);

            if (hMemDC != 0) {
                int oldbitmap = WinDC.SelectObject(hMemDC, hBitmap);

                /* Set the background color for monochrome bitmaps
                   to the color of the background brush */
                WinBrush brush = WinBrush.get(hbrush);
                if (brush != null) {
                    if (brush.style == BS_SOLID)
                        WinDC.SetBkColor(hdc, brush.color);
                }
                WinRect rcClient = new WinRect();
                WinPos.WIN_GetClientRect(hWnd, rcClient);

                if ((style & SS_CENTERIMAGE)!=0) {
                    int x, y;
                    x = (rcClient.right - rcClient.left)/2 - bitmap.getWidth()/2;
                    y = (rcClient.bottom - rcClient.top)/2 - bitmap.getHeight()/2;
                    WinDC.FillRect(hdc, rcClient.allocTemp(), hbrush);
                    BitBlt.BitBlt(hdc, x, y, bitmap.getWidth(), bitmap.getHeight(), hMemDC, 0, 0, SRCCOPY);
                } else {
                    BitBlt.StretchBlt(hdc, 0, 0, rcClient.right - rcClient.left, rcClient.bottom - rcClient.top, hMemDC, 0, 0, bitmap.getWidth(), bitmap.getHeight(), SRCCOPY);
                }
                WinDC.SelectObject(hMemDC, oldbitmap);
                WinDC.DeleteDC(hMemDC);
            }
        }
    };

    static private StaticPaint STATIC_PaintEnhMetafn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            int rc = getTempBuffer(WinRect.SIZE);
            WinPos.GetClientRect(hWnd, rc);
            int hbrush = STATIC_SendWmCtlColorStatic(hWnd, hdc);
            WinDC.FillRect(hdc, rc, hbrush);
            int hEnhMetaFile = WinWindow.GetWindowLongA(hWnd, HICON_GWL_OFFSET);
            WinEnhancedMetaFile mf = WinEnhancedMetaFile.get((hEnhMetaFile));
            if (mf != null) {
                /* The control's current font is not selected into the
                   device context! */
                // :TODO:
                log("STATIC metafile faked");
                // PlayEnhMetaFile(hdc, hEnhMetaFile, rc);
            }
        }
    };

    static private StaticPaint STATIC_PaintEtchedfn = new StaticPaint()  {
        public void paint(int hWnd, int hdc, int style) {
            int rc = getTempBuffer(WinRect.SIZE);

            /* FIXME: sometimes (not always) sends WM_CTLCOLORSTATIC */
            WinPos.GetClientRect(hWnd, rc);
            switch (style & SS_TYPEMASK)
            {
            case SS_ETCHEDHORZ:
                UiTools.DrawEdge(hdc, rc, EDGE_ETCHED, BF_TOP | BF_BOTTOM);
                break;
            case SS_ETCHEDVERT:
                UiTools.DrawEdge(hdc, rc, EDGE_ETCHED, BF_LEFT | BF_RIGHT);
                break;
            case SS_ETCHEDFRAME:
                UiTools.DrawEdge(hdc, rc, EDGE_ETCHED, BF_RECT);
                break;
            }
        }
    };

    static final private StaticPaint[] staticPaintFunc = new StaticPaint[]  {
        STATIC_PaintTextfn,      /* SS_LEFT */
        STATIC_PaintTextfn,      /* SS_CENTER */
        STATIC_PaintTextfn,      /* SS_RIGHT */
        STATIC_PaintIconfn,      /* SS_ICON */
        STATIC_PaintRectfn,      /* SS_BLACKRECT */
        STATIC_PaintRectfn,      /* SS_GRAYRECT */
        STATIC_PaintRectfn,      /* SS_WHITERECT */
        STATIC_PaintRectfn,      /* SS_BLACKFRAME */
        STATIC_PaintRectfn,      /* SS_GRAYFRAME */
        STATIC_PaintRectfn,      /* SS_WHITEFRAME */
        null,                    /* SS_USERITEM */
        STATIC_PaintTextfn,      /* SS_SIMPLE */
        STATIC_PaintTextfn,      /* SS_LEFTNOWORDWRAP */
        STATIC_PaintOwnerDrawfn, /* SS_OWNERDRAW */
        STATIC_PaintBitmapfn,    /* SS_BITMAP */
        STATIC_PaintEnhMetafn,   /* SS_ENHMETAFILE */
        STATIC_PaintEtchedfn,    /* SS_ETCHEDHORZ */
        STATIC_PaintEtchedfn,    /* SS_ETCHEDVERT */
        STATIC_PaintEtchedfn,    /* SS_ETCHEDFRAME */
    };
}
