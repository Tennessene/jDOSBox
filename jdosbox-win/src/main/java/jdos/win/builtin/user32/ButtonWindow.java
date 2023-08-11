package jdos.win.builtin.user32;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.kernel.WinCallback;
import jdos.win.system.StaticData;
import jdos.win.system.WinPoint;
import jdos.win.system.WinRect;
import jdos.win.utils.StringUtil;

public class ButtonWindow extends WinAPI {
    static public void registerClass(WinProcess process) {
        WinClass winClass = WinClass.create();
        winClass.className = "BUTTON";
        winClass.style = CS_DBLCLKS | CS_VREDRAW | CS_HREDRAW | CS_PARENTDC;
        winClass.hCursor = WinCursor.LoadCursorA(0, IDC_ARROW);
        winClass.cbWndExtra = NB_EXTRA_BYTES;
        int cb = WinCallback.addCallback(button_proc);
        winClass.eip = process.loader.registerFunction(cb);
        process.classNames.put(winClass.className.toLowerCase(), winClass);
    }

    static private Callback.Handler button_proc = new HandlerBase() {
        public java.lang.String getName() {
            return "BUTTON.proc";
        }

        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int Msg = CPU.CPU_Pop32();
            int wParam = CPU.CPU_Pop32();
            int lParam = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = ButtonWndProc_common(hWnd, Msg, wParam, lParam);
        }
    };

    /* GetWindowLong offsets for window extra information */
    static final private int STATE_GWL_OFFSET = 0;
    static final private int HFONT_GWL_OFFSET = 4;
    static final private int HIMAGE_GWL_OFFSET = 8;
    static final private int NB_EXTRA_BYTES = 12;

    /* undocumented flags */
    static final private int BUTTON_NSTATES = 0x0F;
    static final private int BUTTON_BTNPRESSED = 0x40;
    static final private int BUTTON_UNKNOWN2 = 0x20;
    static final private int BUTTON_UNKNOWN3 = 0x10;

    static private void BUTTON_NOTIFY_PARENT(int hWnd, int code) {
        /* Notify parent which has created this button control */
        Message.SendMessageA(WinWindow.GetParent(hWnd), WM_COMMAND, MAKEWPARAM(WinWindow.GetWindowLongA((hWnd), GWLP_ID), (code)), hWnd);
    }

    static final private int MAX_BTN_TYPE = 16;

    static final private int[] maxCheckState = new int[]
            {
                    BST_UNCHECKED,      /* BS_PUSHBUTTON */
                    BST_UNCHECKED,      /* BS_DEFPUSHBUTTON */
                    BST_CHECKED,        /* BS_CHECKBOX */
                    BST_CHECKED,        /* BS_AUTOCHECKBOX */
                    BST_CHECKED,        /* BS_RADIOBUTTON */
                    BST_INDETERMINATE,  /* BS_3STATE */
                    BST_INDETERMINATE,  /* BS_AUTO3STATE */
                    BST_UNCHECKED,      /* BS_GROUPBOX */
                    BST_UNCHECKED,      /* BS_USERBUTTON */
                    BST_CHECKED,        /* BS_AUTORADIOBUTTON */
                    BST_UNCHECKED,      /* BS_PUSHBOX */
                    BST_UNCHECKED       /* BS_OWNERDRAW */
            };

    static private interface ButtonPaint {
        public void paint(int hWnd, int hdc, int action);
    }

    static private int checkBoxWidth = 0, checkBoxHeight = 0;

    static private int get_button_state(int hwnd) {
        return WinWindow.GetWindowLongA(hwnd, STATE_GWL_OFFSET);
    }

    static private void set_button_state(int hwnd, int state) {
        WinWindow.SetWindowLongA(hwnd, STATE_GWL_OFFSET, state);
    }

    static private int get_button_font(int hwnd) {
        return WinWindow.GetWindowLongA(hwnd, HFONT_GWL_OFFSET);
    }

    static private void set_button_font(int hwnd, int font) {
        WinWindow.SetWindowLongA(hwnd, HFONT_GWL_OFFSET, font);
    }

    static private int get_button_type(int window_style) {
        return (window_style & BS_TYPEMASK);
    }

    /* paint a button of any type */
    static private void paint_button(int hwnd, int style, int action) {
        if (btnPaintFunc[style] != null && WinWindow.IsWindowVisible(hwnd) != 0) {
            int hdc = Painting.GetDC(hwnd);
            btnPaintFunc[style].paint(hwnd, hdc, action);
            Painting.ReleaseDC(hwnd, hdc);
        }
    }

    /* retrieve the button text; returned buffer must be freed by caller */
    static String get_button_text(int hwnd) {
        return WinWindow.get(hwnd).text;
    }

    /**
     * ********************************************************************
     * ButtonWndProc_common
     */
    static private int ButtonWndProc_common(int hWnd, int uMsg, int wParam, int lParam) {
        int style = WinWindow.GetWindowLongA(hWnd, GWL_STYLE);
        int btn_type = get_button_type(style);
        WinWindow window = WinWindow.get(hWnd);
        if (window == null) return 0;

        switch (uMsg) {
            case WM_GETDLGCODE:
                switch (btn_type) {
                    case BS_USERBUTTON:
                    case BS_PUSHBUTTON:
                        return DLGC_BUTTON | DLGC_UNDEFPUSHBUTTON;
                    case BS_DEFPUSHBUTTON:
                        return DLGC_BUTTON | DLGC_DEFPUSHBUTTON;
                    case BS_RADIOBUTTON:
                    case BS_AUTORADIOBUTTON:
                        return DLGC_BUTTON | DLGC_RADIOBUTTON;
                    case BS_GROUPBOX:
                        return DLGC_STATIC;
                    default:
                        return DLGC_BUTTON;
                }

            case WM_ENABLE:
                paint_button(hWnd, btn_type, ODA_DRAWENTIRE);
                break;

            case WM_CREATE:
                if (StaticData.hbitmapCheckBoxes == 0) {
                    StaticData.hbitmapCheckBoxes = WinBitmap.LoadBitmapA(0, OBM_CHECKBOXES);
                    WinBitmap bm = WinBitmap.get(StaticData.hbitmapCheckBoxes);
                    checkBoxWidth = bm.getWidth() / 4;
                    checkBoxHeight = bm.getHeight() / 3;
                }
                if (btn_type >= MAX_BTN_TYPE)
                    return -1; /* abort */

                /* XP turns a BS_USERBUTTON into BS_PUSHBUTTON */
                if (btn_type == BS_USERBUTTON) {
                    style = (style & ~BS_TYPEMASK) | BS_PUSHBUTTON;
                    window.dwStyle &= ~BS_TYPEMASK;
                    window.dwStyle |= BS_PUSHBUTTON;
                }
                set_button_state(hWnd, BST_UNCHECKED);
                return 0;

            case WM_ERASEBKGND:
                if (btn_type == BS_OWNERDRAW) {
                    int hdc = wParam;
                    int parent = WinWindow.GetParent(hWnd);
                    if (parent == 0) parent = hWnd;
                    int hBrush = Message.SendMessageA(parent, WM_CTLCOLORBTN, hdc, hWnd);
                    if (hBrush == 0) /* did the app forget to call defwindowproc ? */
                        hBrush = DefWnd.DefWindowProcA(parent, WM_CTLCOLORBTN, hdc, hWnd);
                    int rc = getTempBuffer(WinRect.SIZE);
                    WinPos.GetClientRect(hWnd, rc);
                    WinDC.FillRect(hdc, rc, hBrush);
                }
                return 1;

            case WM_PRINTCLIENT:
            case WM_PAINT:
                if (btnPaintFunc[btn_type] != null) {
                    int ps = 0;
                    int hdc;
                    if (wParam != 0)
                        hdc = wParam;
                    else {
                        ps = getTempBuffer(PAINTSTRUCT.SIZE);
                        hdc = Painting.BeginPaint(hWnd, ps);
                    }
                    int nOldMode = WinDC.SetBkMode(hdc, TRANSPARENT); // :TODO: why is this OPAQUE on WINE
                    btnPaintFunc[btn_type].paint(hWnd, hdc, ODA_DRAWENTIRE);
                    WinDC.SetBkMode(hdc, nOldMode); /*  reset painting mode */
                    if (ps != 0)
                        Painting.EndPaint(hWnd, ps);
                }
                break;

            case WM_KEYDOWN:
                if (wParam == VK_SPACE) {
                    Message.SendMessageA(hWnd, BM_SETSTATE, TRUE, 0);
                    set_button_state(hWnd, get_button_state(hWnd) | BUTTON_BTNPRESSED);
                    Input.SetCapture(hWnd);
                }
                break;

            case WM_LBUTTONDBLCLK:
                if ((style & BS_NOTIFY) != 0 || btn_type == BS_RADIOBUTTON || btn_type == BS_USERBUTTON || btn_type == BS_OWNERDRAW) {
                    BUTTON_NOTIFY_PARENT(hWnd, BN_DOUBLECLICKED);
                    break;
                }
                /* fall through */
            case WM_LBUTTONDOWN:
                Input.SetCapture(hWnd);
                Focus.SetFocus(hWnd);
                set_button_state(hWnd, get_button_state(hWnd) | BUTTON_BTNPRESSED);
                Message.SendMessageA(hWnd, BM_SETSTATE, TRUE, 0);
                break;

            case WM_KEYUP:
                if (wParam != VK_SPACE)
                    break;
                /* fall through */
            case WM_LBUTTONUP: {
                int state = get_button_state(hWnd);
                if ((state & BUTTON_BTNPRESSED) == 0) break;
                state &= BUTTON_NSTATES;
                set_button_state(hWnd, state);
                if ((state & BST_PUSHED) == 0) {
                    Input.ReleaseCapture();
                    break;
                }
                Message.SendMessageA(hWnd, BM_SETSTATE, FALSE, 0);
                Input.ReleaseCapture();
                WinRect rect = new WinRect();
                WinPos.WIN_GetClientRect(hWnd, rect);
                WinPoint pt = new WinPoint();
                pt.x = (short) LOWORD(lParam);
                pt.y = (short) HIWORD(lParam);

                if (uMsg == WM_KEYUP || rect.contains(pt)) {
                    state = get_button_state(hWnd);
                    switch (btn_type) {
                        case BS_AUTOCHECKBOX:
                            Message.SendMessageA(hWnd, BM_SETCHECK, BOOL((state & BST_CHECKED) == 0), 0);
                            break;
                        case BS_AUTORADIOBUTTON:
                            Message.SendMessageA(hWnd, BM_SETCHECK, TRUE, 0);
                            break;
                        case BS_AUTO3STATE:
                            Message.SendMessageA(hWnd, BM_SETCHECK, (state & BST_INDETERMINATE) != 0 ? 0 : ((state & 3) + 1), 0);
                            break;
                    }
                    BUTTON_NOTIFY_PARENT(hWnd, BN_CLICKED);
                }
                break;
            }
            case WM_CAPTURECHANGED: {
                int state = get_button_state(hWnd);
                if ((state & BUTTON_BTNPRESSED) != 0) {
                    state &= BUTTON_NSTATES;
                    set_button_state(hWnd, state);
                    if ((state & BST_PUSHED) != 0) Message.SendMessageA(hWnd, BM_SETSTATE, FALSE, 0);
                }
                break;
            }
            case WM_MOUSEMOVE:
                if ((wParam & MK_LBUTTON) != 0 && Input.GetCapture() == hWnd) {
                    WinRect rect = new WinRect();
                    WinPoint pt = new WinPoint();
                    WinPos.WIN_GetClientRect(hWnd, rect);
                    pt.x = (short) LOWORD(lParam);
                    pt.y = (short) HIWORD(lParam);

                    Message.SendMessageA(hWnd, BM_SETSTATE, BOOL(rect.contains(pt)), 0);
                }
                break;

            case WM_SETTEXT: {
                /* Clear an old text here as Windows does */
                int hdc = Painting.GetDC(hWnd);
                int parent = WinWindow.GetParent(hWnd);

                if (parent == 0) parent = hWnd;
                int hbrush = Message.SendMessageA(parent, WM_CTLCOLORSTATIC, hdc, hWnd);
                if (hbrush == 0) /* did the app forget to call DefWindowProc ? */
                    hbrush = DefWnd.DefWindowProcA(parent, WM_CTLCOLORSTATIC, hdc, hWnd);

                WinRect client = new WinRect();
                WinPos.WIN_GetClientRect(hWnd, client);
                WinRect rc = new WinRect(client);
                BUTTON_CalcLabelRect(hWnd, hdc, rc);
                /* Clip by client rect bounds */
                if (rc.right > client.right) rc.right = client.right;
                if (rc.bottom > client.bottom) rc.bottom = client.bottom;
                WinDC.FillRect(hdc, rc.allocTemp(), hbrush);
                Painting.ReleaseDC(hWnd, hdc);

                DefWnd.DefWindowProcA(hWnd, WM_SETTEXT, wParam, lParam);
                if (btn_type == BS_GROUPBOX) /* Yes, only for BS_GROUPBOX */
                    Painting.InvalidateRect(hWnd, NULL, TRUE);
                else
                    paint_button(hWnd, btn_type, ODA_DRAWENTIRE);
                return 1; /* success. FIXME: check text length */
            }

            case WM_SETFONT:
                set_button_font(hWnd, wParam);
                if (lParam != 0) Painting.InvalidateRect(hWnd, NULL, TRUE);
                break;

            case WM_GETFONT:
                return get_button_font(hWnd);

            case WM_SETFOCUS:
                set_button_state(hWnd, get_button_state(hWnd) | BST_FOCUS);
                paint_button(hWnd, btn_type, ODA_FOCUS);
                if ((style & BS_NOTIFY) != 0)
                    BUTTON_NOTIFY_PARENT(hWnd, BN_SETFOCUS);
                break;

            case WM_KILLFOCUS: {
                int state = get_button_state(hWnd);
                set_button_state(hWnd, state & ~BST_FOCUS);
                paint_button(hWnd, btn_type, ODA_FOCUS);

                if ((state & BUTTON_BTNPRESSED) != 0 && Input.GetCapture() == hWnd)
                    Input.ReleaseCapture();
                if ((style & BS_NOTIFY) != 0)
                    BUTTON_NOTIFY_PARENT(hWnd, BN_KILLFOCUS);

                Painting.InvalidateRect(hWnd, NULL, FALSE);
                break;
            }
            case WM_SYSCOLORCHANGE:
                Painting.InvalidateRect(hWnd, NULL, FALSE);
                break;

            case BM_SETSTYLE:
                if ((wParam & BS_TYPEMASK) >= MAX_BTN_TYPE) break;
                btn_type = wParam & BS_TYPEMASK;
                window.dwStyle = (style & ~BS_TYPEMASK) | btn_type;
                /* Only redraw if lParam flag is set.*/
                if (lParam != 0)
                    Painting.InvalidateRect(hWnd, NULL, TRUE);

                break;

            case BM_CLICK:
                Message.SendMessageA(hWnd, WM_LBUTTONDOWN, 0, 0);
                Message.SendMessageA(hWnd, WM_LBUTTONUP, 0, 0);
                break;

            case BM_SETIMAGE:
                /* Check that image format matches button style */
                switch (style & (BS_BITMAP | BS_ICON)) {
                    case BS_BITMAP:
                        if (wParam != IMAGE_BITMAP) return 0;
                        break;
                    case BS_ICON:
                        if (wParam != IMAGE_ICON) return 0;
                        break;
                    default:
                        return 0;
                }
                int oldHbitmap = WinWindow.SetWindowLongA(hWnd, HIMAGE_GWL_OFFSET, lParam);
                Painting.InvalidateRect(hWnd, NULL, FALSE);
                return oldHbitmap;

            case BM_GETIMAGE:
                return WinWindow.GetWindowLongA(hWnd, HIMAGE_GWL_OFFSET);

            case BM_GETCHECK:
                return get_button_state(hWnd) & 3;

            case BM_SETCHECK:
                if (wParam > maxCheckState[btn_type]) wParam = maxCheckState[btn_type];
                int state = get_button_state(hWnd);
                if ((btn_type == BS_RADIOBUTTON) || (btn_type == BS_AUTORADIOBUTTON)) {
                    if (wParam != 0) window.dwStyle |= WS_TABSTOP;
                    else window.dwStyle &= ~WS_TABSTOP;
                }
                if ((state & 3) != wParam) {
                    set_button_state(hWnd, (state & ~3) | wParam);
                    paint_button(hWnd, btn_type, ODA_SELECT);
                }
                if ((btn_type == BS_AUTORADIOBUTTON) && (wParam == BST_CHECKED) && (style & WS_CHILD) != 0)
                    BUTTON_CheckAutoRadioButton(hWnd);
                break;

            case BM_GETSTATE:
                return get_button_state(hWnd);

            case BM_SETSTATE:
                state = get_button_state(hWnd);
                if (wParam != 0)
                    set_button_state(hWnd, state | BST_PUSHED);
                else
                    set_button_state(hWnd, state & ~BST_PUSHED);

                paint_button(hWnd, btn_type, ODA_SELECT);
                break;

            case WM_NCHITTEST:
                if (btn_type == BS_GROUPBOX) return HTTRANSPARENT;
                /* fall through */
            default:
                return DefWnd.DefWindowProcA(hWnd, uMsg, wParam, lParam);
        }
        return 0;
    }

    /**
     * *******************************************************************
     * Convert button styles to flags used by DrawText.
     */
    static private int BUTTON_BStoDT(int style, int ex_style) {
        int dtStyle = DT_NOCLIP;  /* We use SelectClipRgn to limit output */

        /* "Convert" pushlike buttons to pushbuttons */
        if ((style & BS_PUSHLIKE) != 0)
            style &= ~BS_TYPEMASK;

        if ((style & BS_MULTILINE) == 0)
            dtStyle |= DT_SINGLELINE;
        else
            dtStyle |= DT_WORDBREAK;

        switch (style & BS_CENTER) {
            case BS_LEFT:   /* DT_LEFT is 0 */
                break;
            case BS_RIGHT:
                dtStyle |= DT_RIGHT;
                break;
            case BS_CENTER:
                dtStyle |= DT_CENTER;
                break;
            default:
                /* Pushbutton's text is centered by default */
                if (get_button_type(style) <= BS_DEFPUSHBUTTON) dtStyle |= DT_CENTER;
                /* all other flavours have left aligned text */
        }

        if ((ex_style & WS_EX_RIGHT) != 0) dtStyle = DT_RIGHT | (dtStyle & ~(DT_LEFT | DT_CENTER));

        /* DrawText ignores vertical alignment for multiline text,
        * but we use these flags to align label manually.
        */
        if (get_button_type(style) != BS_GROUPBOX) {
            switch (style & BS_VCENTER) {
                case BS_TOP:     /* DT_TOP is 0 */
                    break;
                case BS_BOTTOM:
                    dtStyle |= DT_BOTTOM;
                    break;
                case BS_VCENTER: /* fall through */
                default:
                    dtStyle |= DT_VCENTER;
                    break;
            }
        } else
            /* GroupBox's text is always single line and is top aligned. */
            dtStyle |= DT_SINGLELINE;

        return dtStyle;
    }

    /**
     * *******************************************************************
     * BUTTON_CalcLabelRect
     * <p/>
     * Calculates label's rectangle depending on button style.
     * <p/>
     * Returns flags to be passed to DrawText.
     * Calculated rectangle doesn't take into account button state
     * (pushed, etc.). If there is nothing to draw (no text/image) output
     * rectangle is empty, and return value is (UINT)-1.
     */
    static private int BUTTON_CalcLabelRect(int hwnd, int hdc, WinRect rc) {
        int style = WinWindow.GetWindowLongA(hwnd, GWL_STYLE);
        int ex_style = WinWindow.GetWindowLongA(hwnd, GWL_EXSTYLE);
        int dtStyle = BUTTON_BStoDT(style, ex_style);
        WinRect r = new WinRect(rc);

        /* Calculate label rectangle according to label type */
        switch (style & (BS_ICON | BS_BITMAP)) {
            case BS_TEXT:
                String text = get_button_text(hwnd);
                if (text == null || text.length() == 0) {
                    rc.right = rc.left;
                    rc.bottom = rc.top;
                    return -1;
                }
                int lpRect = r.allocTemp();
                WinText.DrawTextA(hdc, StringUtil.allocateA(text), -1, lpRect, dtStyle | DT_CALCRECT);
                r.copy(lpRect);
                break;

            case BS_ICON:
                WinIcon icon = WinIcon.get(WinWindow.GetWindowLongA(hwnd, HIMAGE_GWL_OFFSET));
                if (icon == null) {
                    rc.right = rc.left;
                    rc.bottom = rc.top;
                    return -1;
                }
                r.right = r.left + icon.cx;
                r.bottom = r.top + icon.cy;
                break;

            case BS_BITMAP:
                WinBitmap bitmap = WinBitmap.get(WinWindow.GetWindowLongA(hwnd, HIMAGE_GWL_OFFSET));
                if (bitmap == null) {
                    rc.right = rc.left;
                    rc.bottom = rc.top;
                    return -1;
                }
                r.right = r.left + bitmap.getWidth();
                r.bottom = r.top + bitmap.getHeight();
                break;

            default:
                rc.right = rc.left;
                rc.bottom = rc.top;
                return -1;
        }

        /* Position label inside bounding rectangle according to
        * alignment flags. (calculated rect is always left-top aligned).
        * If label is aligned to any side - shift label in opposite
        * direction to leave extra space for focus rectangle.
        */
        int n;
        switch (dtStyle & (DT_CENTER | DT_RIGHT)) {
            case DT_LEFT:
                r.left++;
                r.right++;
                break;
            case DT_CENTER:
                n = r.right - r.left;
                r.left = rc.left + ((rc.right - rc.left) - n) / 2;
                r.right = r.left + n;
                break;
            case DT_RIGHT:
                n = r.right - r.left;
                r.right = rc.right - 1;
                r.left = r.right - n;
                break;
        }

        switch (dtStyle & (DT_VCENTER | DT_BOTTOM)) {
            case DT_TOP:
                r.top++;
                r.bottom++;
                break;
            case DT_VCENTER:
                n = r.bottom - r.top;
                r.top = rc.top + ((rc.bottom - rc.top) - n) / 2;
                r.bottom = r.top + n;
                break;
            case DT_BOTTOM:
                n = r.bottom - r.top;
                r.bottom = rc.bottom - 1;
                r.top = r.bottom - n;
                break;
        }

        rc.copy(r);
        return dtStyle;
    }


    /**
     * *******************************************************************
     * BUTTON_DrawTextCallback
     * <p/>
     * Callback function used by DrawStateW function.
     */
    static public int BUTTON_DrawTextCallback(int hdc, int lp, int wp, int cx, int cy) {
        WinRect rc = new WinRect(0, 0, cx, cy);
        WinText.DrawTextA(hdc, lp, -1, rc.allocTemp(), wp);
        return TRUE;
    }


    /**
     * *******************************************************************
     * BUTTON_DrawLabel
     * <p/>
     * Common function for drawing button label.
     */
    static void BUTTON_DrawLabel(int hwnd, int hdc, int dtFlags, WinRect rc) {
        int lpOutputProc = NULL;
        int lp;
        int wp = 0;
        int hbr = 0;
        int flags = WinWindow.IsWindowEnabled(hwnd) != 0 ? DSS_NORMAL : DSS_DISABLED;
        int state = get_button_state(hwnd);
        int style = WinWindow.GetWindowLongA(hwnd, GWL_STYLE);

        /* FIXME: To draw disabled label in Win31 look-and-feel, we probably
        * must use DSS_MONO flag and COLOR_GRAYTEXT brush (or maybe DSS_UNION).
        * I don't have Win31 on hand to verify that, so I leave it as is.
        */

        if ((style & BS_PUSHLIKE) != 0 && (state & BST_INDETERMINATE) != 0) {
            hbr = SysParams.GetSysColorBrush(COLOR_GRAYTEXT);
            flags |= DSS_MONO;
        }

        switch (style & (BS_ICON | BS_BITMAP)) {
            case BS_TEXT:
                /* DST_COMPLEX -- is 0 */
                lpOutputProc = -1; // special value, will call BUTTON_DrawTextCallback
                String text = get_button_text(hwnd);
                if (text == null || text.length() == 0)
                    return;
                lp = StringUtil.allocateA(text);
                wp = dtFlags;
                break;

            case BS_ICON:
                flags |= DST_ICON;
                lp = WinWindow.GetWindowLongA(hwnd, HIMAGE_GWL_OFFSET);
                break;

            case BS_BITMAP:
                flags |= DST_BITMAP;
                lp = WinWindow.GetWindowLongA(hwnd, HIMAGE_GWL_OFFSET);
                break;

            default:
                return;
        }

        UiTools.DrawStateA(hdc, hbr, lpOutputProc, lp, wp, rc.left, rc.top, rc.width(), rc.height(), flags);
    }

    /**
     * *******************************************************************
     * Push Button Functions
     */
    static private ButtonPaint PB_Paint = new ButtonPaint() {
        public void paint(int hWnd, int hdc, int action) {
            WinRect rc = new WinRect();
            int state = get_button_state(hWnd);
            int style = WinWindow.GetWindowLongA(hWnd, GWL_STYLE);
            boolean pushedState = (state & BST_PUSHED) != 0;

            WinPos.WIN_GetClientRect(hWnd, rc);

            /* Send WM_CTLCOLOR to allow changing the font (the colors are fixed) */
            int hFont = get_button_font(hWnd);
            if (hFont != 0)
                WinDC.SelectObject(hdc, hFont);
            int parent = WinWindow.GetParent(hWnd);
            if (parent == 0) parent = hWnd;
            Message.SendMessageA(parent, WM_CTLCOLORBTN, hdc, hWnd);

            int hrgn = UiTools.set_control_clipping(hdc, rc);

            int hOldPen = WinDC.SelectObject(hdc, SysParams.GetSysColorPen(COLOR_WINDOWFRAME));
            int hOldBrush = WinDC.SelectObject(hdc, SysParams.GetSysColorBrush(COLOR_BTNFACE));
            int oldBkMode = WinDC.SetBkMode(hdc, TRANSPARENT);

            if (get_button_type(style) == BS_DEFPUSHBUTTON) {
                if (action != ODA_FOCUS)
                    Painting.Rectangle(hdc, rc.left, rc.top, rc.right, rc.bottom);
                rc.inflate(-1, -1);
            }
            try {
                /* completely skip the drawing if only focus has changed */
                if (action != ODA_FOCUS) {
                    int uState = DFCS_BUTTONPUSH;

                    if ((style & BS_FLAT) != 0)
                        uState |= DFCS_MONO;
                    else if (pushedState) {
                        if (get_button_type(style) == BS_DEFPUSHBUTTON)
                            uState |= DFCS_FLAT;
                        else
                            uState |= DFCS_PUSHED;
                    }

                    if ((state & (BST_CHECKED | BST_INDETERMINATE)) != 0)
                        uState |= DFCS_CHECKED;

                    UiTools.DrawFrameControl(hdc, rc.allocTemp(), DFC_BUTTON, uState);

                    /* draw button label */
                    WinRect r = new WinRect(rc);
                    int dtFlags = BUTTON_CalcLabelRect(hWnd, hdc, r);

                    if (dtFlags == -1)
                        return;

                    if (pushedState)
                        r.offset(1, 1);

                    int oldTxtColor = WinDC.SetTextColor(hdc, SysParams.GetSysColor(COLOR_BTNTEXT));
                    BUTTON_DrawLabel(hWnd, hdc, dtFlags, r);
                    WinDC.SetTextColor(hdc, oldTxtColor);
                }
                // draw_focus:
                if (action == ODA_FOCUS || (state & BST_FOCUS) != 0) {
                    rc.inflate(-2, -2);
                    UiTools.DrawFocusRect(hdc, rc.allocTemp());
                }
            } finally {
                WinDC.SelectObject(hdc, hOldPen);
                WinDC.SelectObject(hdc, hOldBrush);
                WinDC.SetBkMode(hdc, oldBkMode);
                WinDC.SelectClipRgn(hdc, hrgn);
                if (hrgn != 0) GdiObj.DeleteObject(hrgn);
            }
        }
    };

    /**
     * *******************************************************************
     * Check Box & Radio Button Functions
     */
    static private ButtonPaint CB_Paint = new ButtonPaint() {
        public void paint(int hWnd, int hdc, int action) {
            int state = get_button_state(hWnd);
            int style = WinWindow.GetWindowLongA(hWnd, GWL_STYLE);

            if ((style & BS_PUSHLIKE) != 0) {
                PB_Paint.paint(hWnd, hdc, action);
                return;
            }

            WinRect client = new WinRect();
            WinPos.WIN_GetClientRect(hWnd, client);
            WinRect rbox = new WinRect(client);
            WinRect rtext = new WinRect(client);
            int hFont = get_button_font(hWnd);

            if (hFont != 0)
                WinDC.SelectObject(hdc, hFont);

            int parent = WinWindow.GetParent(hWnd);
            if (parent == 0)
                parent = hWnd;
            int hBrush = Message.SendMessageA(parent, WM_CTLCOLORSTATIC, hdc, hWnd);
            if (hBrush == 0) /* did the app forget to call defwindowproc ? */
                hBrush = DefWnd.DefWindowProcA(parent, WM_CTLCOLORSTATIC, hdc, hWnd);
            int hrgn = UiTools.set_control_clipping(hdc, client);

            if ((style & BS_LEFTTEXT) != 0) {
                /* magic +4 is what CTL3D expects */
                rtext.right -= checkBoxWidth + 4;
                rbox.left = rbox.right - checkBoxWidth;
            } else {
                rtext.left += checkBoxWidth + 4;
                rbox.right = checkBoxWidth;
            }

            /* Since WM_ERASEBKGND does nothing, first prepare background */
            if (action == ODA_SELECT)
                WinDC.FillRect(hdc, rbox.allocTemp(), hBrush);
            if (action == ODA_DRAWENTIRE)
                WinDC.FillRect(hdc, client.allocTemp(), hBrush);

            /* Draw label */
            client.copy(rtext);
            int dtFlags = BUTTON_CalcLabelRect(hWnd, hdc, rtext);

            /* Only adjust rbox when rtext is valid */
            if (dtFlags != -1) {
                rbox.top = rtext.top;
                rbox.bottom = rtext.bottom;
            }

            /* Draw the check-box bitmap */
            if (action == ODA_DRAWENTIRE || action == ODA_SELECT) {
                int flags;

                if ((get_button_type(style) == BS_RADIOBUTTON) || (get_button_type(style) == BS_AUTORADIOBUTTON))
                    flags = DFCS_BUTTONRADIO;
                else if ((state & BST_INDETERMINATE) != 0)
                    flags = DFCS_BUTTON3STATE;
                else
                    flags = DFCS_BUTTONCHECK;

                if ((state & (BST_CHECKED | BST_INDETERMINATE)) != 0)
                    flags |= DFCS_CHECKED;
                if ((state & BST_PUSHED) != 0)
                    flags |= DFCS_PUSHED;

                if ((style & WS_DISABLED) != 0)
                    flags |= DFCS_INACTIVE;

                /* rbox must have the correct height */
                int delta = rbox.bottom - rbox.top - checkBoxHeight;

                if ((style & BS_TOP) != 0) {
                    if (delta > 0) {
                        rbox.bottom = rbox.top + checkBoxHeight;
                    } else {
                        rbox.top -= -delta / 2 + 1;
                        rbox.bottom = rbox.top + checkBoxHeight;
                    }
                } else if ((style & BS_BOTTOM) != 0) {
                    if (delta > 0) {
                        rbox.top = rbox.bottom - checkBoxHeight;
                    } else {
                        rbox.bottom += -delta / 2 + 1;
                        rbox.top = rbox.bottom - checkBoxHeight;
                    }
                } else { /* Default */
                    if (delta > 0) {
                        int ofs = (delta / 2);
                        rbox.bottom -= ofs + 1;
                        rbox.top = rbox.bottom - checkBoxHeight;
                    } else if (delta < 0) {
                        int ofs = (-delta / 2);
                        rbox.top -= ofs + 1;
                        rbox.bottom = rbox.top + checkBoxHeight;
                    }
                }

                UiTools.DrawFrameControl(hdc, rbox.allocTemp(), DFC_BUTTON, flags);
            }

            if (dtFlags == -1L) /* Noting to draw */
                return;

            if (action == ODA_DRAWENTIRE)
                BUTTON_DrawLabel(hWnd, hdc, dtFlags, rtext);

            /* ... and focus */
            if (action == ODA_FOCUS || (state & BST_FOCUS) != 0) {
                rtext.left--;
                rtext.right++;
                rtext.intersect(rtext, client);
                UiTools.DrawFocusRect(hdc, rtext.allocTemp());
            }
            WinDC.SelectClipRgn(hdc, hrgn);
            if (hrgn != 0)
                GdiObj.DeleteObject(hrgn);
        }
    };

    /**
     * *******************************************************************
     * BUTTON_CheckAutoRadioButton
     * <p/>
     * hwnd is checked, uncheck every other auto radio button in group
     */
    static private void BUTTON_CheckAutoRadioButton(int hwnd) {
        int parent = WinWindow.GetParent(hwnd);
        /* make sure that starting control is not disabled or invisible */
        int start = WinDialog.GetNextDlgGroupItem(parent, hwnd, TRUE);
        int sibling = start;
        do {
            if (sibling == 0) break;
            if ((hwnd != sibling) && ((WinWindow.GetWindowLongA(sibling, GWL_STYLE) & BS_TYPEMASK) == BS_AUTORADIOBUTTON))
                Message.SendMessageA(sibling, BM_SETCHECK, BST_UNCHECKED, 0);
            sibling = WinDialog.GetNextDlgGroupItem(parent, sibling, FALSE);
        } while (sibling != start);
    }


    /**
     * *******************************************************************
     * Group Box Functions
     */

    static private ButtonPaint GB_Paint = new ButtonPaint() {
        public void paint(int hWnd, int hdc, int action) {
            int style = WinWindow.GetWindowLongA(hWnd, GWL_STYLE);

            int hFont = get_button_font(hWnd);
            if (hFont != 0)
                WinDC.SelectObject(hdc, hFont);
            /* GroupBox acts like static control, so it sends CTLCOLORSTATIC */
            int parent = WinWindow.GetParent(hWnd);
            if (parent == 0)
                parent = hWnd;
            int hbr = Message.SendMessageA(parent, WM_CTLCOLORSTATIC, hdc, hWnd);
            if (hbr == 0) /* did the app forget to call defwindowproc ? */
                hbr = DefWnd.DefWindowProcA(parent, WM_CTLCOLORSTATIC, hdc, hWnd);
            WinRect rc = new WinRect();
            WinPos.WIN_GetClientRect(hWnd, rc);
            WinRect rcFrame = new WinRect(rc);
            int hrgn = UiTools.set_control_clipping(hdc, rc);

            int pTm = getTempBuffer(TEXTMETRIC.SIZE);
            WinFont.GetTextMetricsA(hdc, pTm);
            TEXTMETRIC tm = new TEXTMETRIC(pTm);

            rcFrame.top += (tm.tmHeight / 2) - 1;
            UiTools.DrawEdge(hdc, rcFrame.allocTemp(), EDGE_ETCHED, BF_RECT | ((style & BS_FLAT) != 0 ? BF_FLAT : 0));

            rc.inflate(-7, 1);
            int dtFlags = BUTTON_CalcLabelRect(hWnd, hdc, rc);

            if (dtFlags != -1) {
                /* Because buttons have CS_PARENTDC class style, there is a chance
                 * that label will be drawn out of client rect.
                 * But Windows doesn't clip label's rect, so do I.
                 */

                /* There is 1-pixel margin at the left, right, and bottom */
                rc.left--;
                rc.right++;
                rc.bottom++;
                WinDC.FillRect(hdc, rc.allocTemp(), hbr);
                rc.left++;
                rc.right--;
                rc.bottom--;

                BUTTON_DrawLabel(hWnd, hdc, dtFlags, rc);
            }
            WinDC.SelectClipRgn(hdc, hrgn);
            if (hrgn != 0)
                GdiObj.DeleteObject(hrgn);
        }
    };


    /**
     * *******************************************************************
     * User Button Functions
     */

    static private ButtonPaint UB_Paint = new ButtonPaint() {
        public void paint(int hWnd, int hdc, int action) {
            int state = get_button_state(hWnd);

            WinRect rc = new WinRect();
            WinPos.WIN_GetClientRect(hWnd, rc);

            int hFont = get_button_font(hWnd);
            if (hFont != 0)
                WinDC.SelectObject(hdc, hFont);

            int parent = WinWindow.GetParent(hWnd);
            if (parent == 0)
                parent = hWnd;
            int hBrush = Message.SendMessageA(parent, WM_CTLCOLORBTN, hdc, hWnd);
            if (hBrush == 0) /* did the app forget to call defwindowproc ? */
                hBrush = DefWnd.DefWindowProcA(parent, WM_CTLCOLORBTN, hdc, hWnd);

            WinDC.FillRect(hdc, rc.allocTemp(), hBrush);
            if (action == ODA_FOCUS || (state & BST_FOCUS) != 0)
                UiTools.DrawFocusRect(hdc, rc.allocTemp());

            switch (action) {
                case ODA_FOCUS:
                    BUTTON_NOTIFY_PARENT(hWnd, (state & BST_FOCUS) != 0 ? BN_SETFOCUS : BN_KILLFOCUS);
                    break;

                case ODA_SELECT:
                    BUTTON_NOTIFY_PARENT(hWnd, (state & BST_PUSHED) != 0 ? BN_HILITE : BN_UNHILITE);
                    break;

                default:
                    BUTTON_NOTIFY_PARENT(hWnd, BN_PAINT);
                    break;
            }
        }
    };


    /**
     * *******************************************************************
     * Ownerdrawn Button Functions
     */
    static private ButtonPaint OB_Paint = new ButtonPaint() {
        public void paint(int hWnd, int hdc, int action) {
            int state = get_button_state(hWnd);
            DRAWITEMSTRUCT dis = new DRAWITEMSTRUCT();
            int id = WinWindow.GetWindowLongA(hWnd, GWLP_ID);

            dis.CtlType = ODT_BUTTON;
            dis.CtlID = id;
            dis.itemID = 0;
            dis.itemAction = action;
            dis.itemState = ((state & BST_FOCUS) != 0 ? ODS_FOCUS : 0) | ((state & BST_PUSHED) != 0 ? ODS_SELECTED : 0) | (WinWindow.IsWindowEnabled(hWnd) != 0 ? 0 : ODS_DISABLED);
            dis.hwndItem = hWnd;
            dis.hDC = hdc;
            dis.itemData = 0;
            WinPos.WIN_GetClientRect(hWnd, dis.rcItem);

            int hFont = get_button_font(hWnd);
            int hPrevFont = 0;
            if (hFont != 0)
                hPrevFont = WinDC.SelectObject(hdc, hFont);
            int parent = WinWindow.GetParent(hWnd);
            if (parent == 0)
                parent = hWnd;
            Message.SendMessageA(parent, WM_CTLCOLORBTN, hdc, hWnd);

            int hrgn = UiTools.set_control_clipping(hdc, dis.rcItem);

            Message.SendMessageA(WinWindow.GetParent(hWnd), WM_DRAWITEM, id, dis.allocTemp());
            if (hPrevFont != 0)
                WinDC.SelectObject(hdc, hPrevFont);
            WinDC.SelectClipRgn(hdc, hrgn);
            if (hrgn != 0)
                GdiObj.DeleteObject(hrgn);
        }
    };

    static private final ButtonPaint[] btnPaintFunc = new ButtonPaint[]
    {
            PB_Paint,    /* BS_PUSHBUTTON */
            PB_Paint,    /* BS_DEFPUSHBUTTON */
            CB_Paint,    /* BS_CHECKBOX */
            CB_Paint,    /* BS_AUTOCHECKBOX */
            CB_Paint,    /* BS_RADIOBUTTON */
            CB_Paint,    /* BS_3STATE */
            CB_Paint,    /* BS_AUTO3STATE */
            GB_Paint,    /* BS_GROUPBOX */
            UB_Paint,    /* BS_USERBUTTON */
            CB_Paint,    /* BS_AUTORADIOBUTTON */
            null,        /* BS_PUSHBOX */
            OB_Paint     /* BS_OWNERDRAW */
    };
}
