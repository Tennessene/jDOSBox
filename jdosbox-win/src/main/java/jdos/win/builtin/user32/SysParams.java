package jdos.win.builtin.user32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.WinBrush;
import jdos.win.builtin.gdi32.WinPen;
import jdos.win.system.StaticData;
import jdos.win.system.WinRect;
import jdos.win.utils.Ptr;

public class SysParams extends WinAPI {
    // DWORD WINAPI GetSysColor(int nIndex)
    public static int GetSysColor(int nIndex) {
        if (0 <= nIndex && nIndex < NUM_SYS_COLORS) {
            return StaticData.SysColors[nIndex];
        }
        warn("Unknown index("+nIndex+")");
        return 0;
    }

    // HBRUSH GetSysColorBrush(int nIndex)
    public static int GetSysColorBrush(int nIndex) {
        if (0 <= nIndex && nIndex < NUM_SYS_COLORS) {
            int result = StaticData.SysColorBrushes[nIndex];
            if (result == 0) {
                WinBrush brush = WinBrush.create(BS_SOLID, DefSysColors[nIndex].color, 0);
                brush.makePermanent();
                StaticData.SysColorBrushes[nIndex] = brush.handle;
                result = brush.handle;
            }
            return result;
        }
        warn("Unknown index("+nIndex+")");
        return 0;
    }

    public static int GetSysColorPen(int nIndex) {
        if (0 <= nIndex && nIndex < NUM_SYS_COLORS) {
            int result = StaticData.SysColorPens[nIndex];
            if (result == 0) {
                WinPen pen = WinPen.create(PS_SOLID, 1, DefSysColors[nIndex].color);
                pen.makePermanent();
                StaticData.SysColorPens[nIndex] = pen.handle;
                result = pen.handle;
            }
            return result;
        }
        warn("Unknown index("+nIndex+")");
        return 0;
    }

    // INT WINAPI GetSystemMetrics( INT index )
    static public int GetSystemMetrics(int index) {
        /* some metrics are dynamic */
        switch (index)
        {
        case SM_CXSCREEN:
            return StaticData.screen.getWidth();
        case SM_CYSCREEN:
            return StaticData.screen.getHeight();
//        case SM_CXVSCROLL:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iScrollWidth;
        case SM_CYHSCROLL:
            return GetSystemMetrics(SM_CXVSCROLL);
//        case SM_CYCAPTION:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iCaptionHeight + 1;
        case SM_CXBORDER:
        case SM_CYBORDER:
            /* SM_C{X,Y}BORDER always returns 1 regardless of 'BorderWidth' value in registry */
            return 1;
        case SM_CXDLGFRAME:
        case SM_CYDLGFRAME:
            return 3;
//        case SM_CYVTHUMB:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iScrollHeight;
        case SM_CXHTHUMB:
            return GetSystemMetrics(SM_CYVTHUMB);
        case SM_CXICON:
            //if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
            //return icon_size.cx;
            return 32;
        case SM_CYICON:
            //if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
            //return icon_size.cy;
            return 32;
        case SM_CXCURSOR:
        case SM_CYCURSOR:
            return 32;
        case SM_CYMENU:
            return GetSystemMetrics(SM_CYMENUSIZE) + 1;
        case SM_CXFULLSCREEN:
            /* see the remark for SM_CXMAXIMIZED, at least this formulation is
             * correct */
            return GetSystemMetrics( SM_CXMAXIMIZED) - 2 * GetSystemMetrics( SM_CXFRAME);
        case SM_CYFULLSCREEN:
            /* see the remark for SM_CYMAXIMIZED, at least this formulation is
             * correct */
            return GetSystemMetrics( SM_CYMAXIMIZED) - GetSystemMetrics( SM_CYMIN);
        case SM_CYKANJIWINDOW:
            return 0;
        case SM_MOUSEPRESENT:
            return 1;
//        case SM_CYVSCROLL:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iScrollHeight;
//        case SM_CXHSCROLL:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iScrollHeight;
        case SM_DEBUG:
            return 0;
//        case SM_SWAPBUTTON:
//            get_bool_param( SPI_SETMOUSEBUTTONSWAP_IDX, SPI_SETMOUSEBUTTONSWAP_REGKEY,
//                            SPI_SETMOUSEBUTTONSWAP_VALNAME, &swap_buttons, (BOOL*)&ret );
//            return ret;
        case SM_RESERVED1:
        case SM_RESERVED2:
        case SM_RESERVED3:
        case SM_RESERVED4:
            return 0;
//        case SM_CXMIN:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return 3 * nonclient_metrics.iCaptionWidth + GetSystemMetrics( SM_CYSIZE) +
//                4 * CaptionFontAvCharWidth + 2 * GetSystemMetrics( SM_CXFRAME) + 4;
        case SM_CYMIN:
            return GetSystemMetrics( SM_CYCAPTION) + 2 * GetSystemMetrics( SM_CYFRAME);
//        case SM_CXSIZE:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iCaptionWidth;
//        case SM_CYSIZE:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iCaptionHeight;
//        case SM_CXFRAME:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return GetSystemMetrics(SM_CXDLGFRAME) + nonclient_metrics.iBorderWidth;
//        case SM_CYFRAME:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return GetSystemMetrics(SM_CYDLGFRAME) + nonclient_metrics.iBorderWidth;
        case SM_CXMINTRACK:
            return GetSystemMetrics(SM_CXMIN);
        case SM_CYMINTRACK:
            return GetSystemMetrics(SM_CYMIN);
//        case SM_CXDOUBLECLK:
//            get_uint_param( SPI_SETDOUBLECLKWIDTH_IDX, SPI_SETDOUBLECLKWIDTH_REGKEY1,
//                            SPI_SETDOUBLECLKWIDTH_VALNAME, &double_click_width, &ret );
//            return ret;
//        case SM_CYDOUBLECLK:
//            get_uint_param( SPI_SETDOUBLECLKHEIGHT_IDX, SPI_SETDOUBLECLKHEIGHT_REGKEY1,
//                            SPI_SETDOUBLECLKHEIGHT_VALNAME, &double_click_height, &ret );
//            return ret;
//        case SM_CXICONSPACING:
//            SystemParametersInfoW( SPI_ICONHORIZONTALSPACING, 0, &ret, 0 );
//            return ret;
//        case SM_CYICONSPACING:
//            SystemParametersInfoW( SPI_ICONVERTICALSPACING, 0, &ret, 0 );
//            return ret;
//        case SM_MENUDROPALIGNMENT:
//            SystemParametersInfoW( SPI_GETMENUDROPALIGNMENT, 0, &ret, 0 );
//            return ret;
        case SM_PENWINDOWS:
            return 0;
//        case SM_DBCSENABLED:
//        {
//            CPINFO cpinfo;
//            GetCPInfo( CP_ACP, &cpinfo );
//            return (cpinfo.MaxCharSize > 1);
//        }
        case SM_CMOUSEBUTTONS:
            return 3;
        case SM_SECURE:
            return 0;
        case SM_CXEDGE:
            return GetSystemMetrics(SM_CXBORDER) + 1;
        case SM_CYEDGE:
            return GetSystemMetrics(SM_CYBORDER) + 1;
//        case SM_CXMINSPACING:
//            if( spi_loaded[SPI_MINIMIZEDMETRICS_IDX]) load_minimized_metrics();
//            return GetSystemMetrics(SM_CXMINIMIZED) + minimized_metrics.iHorzGap;
//        case SM_CYMINSPACING:
//            if( spi_loaded[SPI_MINIMIZEDMETRICS_IDX]) load_minimized_metrics();
//            return GetSystemMetrics(SM_CYMINIMIZED) + minimized_metrics.iVertGap;
        case SM_CXSMICON:
        case SM_CYSMICON:
            return 16;
        case SM_CYSMCAPTION:
            return GetSystemMetrics(SM_CYSMSIZE) + 1;
//        case SM_CXSMSIZE:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iSmCaptionWidth;
//        case SM_CYSMSIZE:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iSmCaptionHeight;
//        case SM_CXMENUSIZE:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iMenuWidth;
//        case SM_CYMENUSIZE:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iMenuHeight;
//        case SM_ARRANGE:
//            if( spi_loaded[SPI_MINIMIZEDMETRICS_IDX]) load_minimized_metrics();
//            return minimized_metrics.iArrange;
//        case SM_CXMINIMIZED:
//            if( spi_loaded[SPI_MINIMIZEDMETRICS_IDX]) load_minimized_metrics();
//            return minimized_metrics.iWidth + 6;
//        case SM_CYMINIMIZED:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return nonclient_metrics.iCaptionHeight + 6;
        case SM_CXMAXTRACK:
            return GetSystemMetrics(SM_CXVIRTUALSCREEN) + 4 + 2 * GetSystemMetrics(SM_CXFRAME);
        case SM_CYMAXTRACK:
            return GetSystemMetrics(SM_CYVIRTUALSCREEN) + 4 + 2 * GetSystemMetrics(SM_CYFRAME);
        case SM_CXMAXIMIZED:
            /* FIXME: subtract the width of any vertical application toolbars*/
            return GetSystemMetrics(SM_CXSCREEN) + 2 * GetSystemMetrics(SM_CXFRAME);
        case SM_CYMAXIMIZED:
            /* FIXME: subtract the width of any horizontal application toolbars*/
            return GetSystemMetrics(SM_CYSCREEN) + 2 * GetSystemMetrics(SM_CYCAPTION);
        case SM_NETWORK:
            return 3;  /* FIXME */
        case SM_CLEANBOOT:
            return 0; /* 0 = ok, 1 = failsafe, 2 = failsafe + network */
        case SM_CXDRAG:
        case SM_CYDRAG:
            return 4;
//        case SM_SHOWSOUNDS:
//            SystemParametersInfoW( SPI_GETSHOWSOUNDS, 0, &ret, 0 );
//            return ret;
//        case SM_CXMENUCHECK:
//        case SM_CYMENUCHECK:
//            if (!spi_loaded[SPI_NONCLIENTMETRICS_IDX]) load_nonclient_metrics();
//            return tmMenuFont.tmHeight <= 0 ? 13 :
//            ((tmMenuFont.tmHeight + tmMenuFont.tmExternalLeading + 1) / 2) * 2 - 1;
        case SM_SLOWMACHINE:
            return 0;  /* Never true */
        case SM_MIDEASTENABLED:
            return 0;  /* FIXME */
        case SM_MOUSEWHEELPRESENT:
            return 1;
//        case SM_XVIRTUALSCREEN:
//        {
//            struct monitor_info info;
//            get_monitors_info( &info );
//            return info.virtual_rect.left;
//        }
//        case SM_YVIRTUALSCREEN:
//        {
//            struct monitor_info info;
//            get_monitors_info( &info );
//            return info.virtual_rect.top;
//        }
//        case SM_CXVIRTUALSCREEN:
//        {
//            struct monitor_info info;
//            get_monitors_info( &info );
//            return info.virtual_rect.right - info.virtual_rect.left;
//        }
//        case SM_CYVIRTUALSCREEN:
//        {
//            struct monitor_info info;
//            get_monitors_info( &info );
//            return info.virtual_rect.bottom - info.virtual_rect.top;
//        }
        case SM_CMONITORS:
            return 1;
        case SM_SAMEDISPLAYFORMAT:
            return 1;
        case SM_IMMENABLED:
            return 0;  /* FIXME */
        case SM_CXFOCUSBORDER:
        case SM_CYFOCUSBORDER:
            return 1;
        case SM_TABLETPC:
        case SM_MEDIACENTER:
            return 0;
        case SM_CMETRICS:
            return SM_CMETRICS;
        default:
            return 0;
        }
    }

    // BOOL WINAPI SystemParametersInfo(UINT uiAction, UINT uiParam, PVOID pvParam, UINT fWinIni)
    static public int SystemParametersInfoA(int uiAction, int uiParam, int pvParam, int fWinIni) {
         switch (uiAction) {
            case 0x0010: // SPI_GETSCREENSAVEACTIVE
                writed(pvParam, FALSE);
                break;
            case 0x0030: // SPI_GETWORKAREA
                WinRect.write(pvParam, 0, 0, StaticData.screen.getWidth(), StaticData.screen.getHeight()); // :TODO: if we ever show a taskbar this will have to be adjusted
                break;
            case 0x0053: // SPI_GETLOWPOWERACTIVE
                writed(pvParam, FALSE);
                break;
            default:
                Win.panic("uiAction 0x" + Ptr.toString(uiAction) + " not supported yet");
        }
        return TRUE;
    }

    static public class DefColor {
        public DefColor(String name, int color) {
            this.name = name;
            this.color = color;
        }
        public String name;
        public int color;
    }

    static public final DefColor[] DefSysColors = new DefColor[] {
        new DefColor("Scrollbar", RGB(212, 208, 200)),              /* COLOR_SCROLLBAR */
        new DefColor("Background", RGB(58, 110, 165)),              /* COLOR_BACKGROUND */
        new DefColor("ActiveTitle", RGB(10, 36, 106)),              /* COLOR_ACTIVECAPTION */
        new DefColor("InactiveTitle", RGB(128, 128, 128)),          /* COLOR_INACTIVECAPTION */
        new DefColor("Menu", RGB(212, 208, 200)),                   /* COLOR_MENU */
        new DefColor("Window", RGB(255, 255, 255)),                 /* COLOR_WINDOW */
        new DefColor("WindowFrame", RGB(0, 0, 0)),                  /* COLOR_WINDOWFRAME */
        new DefColor("MenuText", RGB(0, 0, 0)),                     /* COLOR_MENUTEXT */
        new DefColor("WindowText", RGB(0, 0, 0)),                   /* COLOR_WINDOWTEXT */
        new DefColor("TitleText", RGB(255, 255, 255)),              /* COLOR_CAPTIONTEXT */
        new DefColor("ActiveBorder", RGB(212, 208, 200)),           /* COLOR_ACTIVEBORDER */
        new DefColor("InactiveBorder", RGB(212, 208, 200)),         /* COLOR_INACTIVEBORDER */
        new DefColor("AppWorkSpace", RGB(128, 128, 128)),           /* COLOR_APPWORKSPACE */
        new DefColor("Hilight", RGB(10, 36, 106)),                  /* COLOR_HIGHLIGHT */
        new DefColor("HilightText", RGB(255, 255, 255)),            /* COLOR_HIGHLIGHTTEXT */
        new DefColor("ButtonFace", RGB(212, 208, 200)),             /* COLOR_BTNFACE */
        new DefColor("ButtonShadow", RGB(128, 128, 128)),           /* COLOR_BTNSHADOW */
        new DefColor("GrayText", RGB(128, 128, 128)),               /* COLOR_GRAYTEXT */
        new DefColor("ButtonText", RGB(0, 0, 0)),                   /* COLOR_BTNTEXT */
        new DefColor("InactiveTitleText", RGB(212, 208, 200)),      /* COLOR_INACTIVECAPTIONTEXT */
        new DefColor("ButtonHilight", RGB(255, 255, 255)),          /* COLOR_BTNHIGHLIGHT */
        new DefColor("ButtonDkShadow", RGB(64, 64, 64)),            /* COLOR_3DDKSHADOW */
        new DefColor("ButtonLight", RGB(212, 208, 200)),            /* COLOR_3DLIGHT */
        new DefColor("InfoText", RGB(0, 0, 0)),                     /* COLOR_INFOTEXT */
        new DefColor("InfoWindow", RGB(255, 255, 225)),             /* COLOR_INFOBK */
        new DefColor("ButtonAlternateFace", RGB(181, 181, 181)),    /* COLOR_ALTERNATEBTNFACE */
        new DefColor("HotTrackingColor", RGB(0, 0, 200)),           /* COLOR_HOTLIGHT */
        new DefColor("GradientActiveTitle", RGB(166, 202, 240)),    /* COLOR_GRADIENTACTIVECAPTION */
        new DefColor("GradientInactiveTitle", RGB(192, 192, 192)),  /* COLOR_GRADIENTINACTIVECAPTION */
        new DefColor("MenuHilight", RGB(10, 36, 106)),              /* COLOR_MENUHILIGHT */
        new DefColor("MenuBar", RGB(212, 208, 200))                 /* COLOR_MENUBAR */
    };
}
