package jdos.win.builtin;

import jdos.hardware.Memory;
import jdos.win.system.Scheduler;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;

public class WinAPI extends Error {
    static public boolean LOG = false;
    static public int NULL = 0;

    static public void log(String s) {
        System.out.println(s);
    }
    static public void warn(String s) {
        System.out.print(HandlerBase.currentHandler.getName()+": ");
        System.out.println(s);
    }
    static public void faked() {
        System.out.print(HandlerBase.currentHandler.getName()+" faked");
    }

    static public int MAKELONG(int low, int high) {
        return (low & 0xFFFF) | (high << 16);
    }
    static public int MAKEWPARAM(int low, int high) {
        return (low & 0xFFFF) | (high << 16);
    }
    static public void SetLastError(int error) {
        Scheduler.getCurrentThread().setLastError(error);
    }
    static public int RGB(int r, int g, int b) {
        return r | (g << 8) | (b << 16);
    }
    static public boolean IS_INTRESOURCE(int p) {
        return p<=0xFFFF;
    }
    static public int LOWORD(int w) {
        return w & 0xFFFF;
    }
    static public int HIWORD(int w) {
        return w >>> 16;
    }

    static final public int MAX_PATH = 260;
    static final public int TRUE = 1;
    static final public int FALSE = 0;

    static final public int WHITE_BRUSH =       0;
    static final public int LTGRAY_BRUSH =      1;
    static final public int GRAY_BRUSH =        2;
    static final public int DKGRAY_BRUSH =      3;
    static final public int BLACK_BRUSH =       4;
    static final public int NULL_BRUSH =        5;
    static final public int HOLLOW_BRUSH =      5;
    static final public int WHITE_PEN =         6;
    static final public int BLACK_PEN =         7;
    static final public int NULL_PEN =          8;
    static final public int OEM_FIXED_FONT =    10;
    static final public int ANSI_FIXED_FONT =   11;
    static final public int ANSI_VAR_FONT =     12;
    static final public int SYSTEM_FONT =       13;
    static final public int DEVICE_DEFAULT_FONT=14;
    static final public int DEFAULT_PALETTE =   15;
    static final public int SYSTEM_FIXED_FONT = 16;
    static final public int DEFAULT_GUI_FONT =  17;
    static final public int DC_BRUSH =          18;
    static final public int DC_PEN =            19;

    static final public int STOCK_LAST =        19;
    
      /* Brush styles */
    static final public int BS_SOLID =          0;
    static final public int BS_NULL =           1;
    static final public int BS_HOLLOW =         1;
    static final public int BS_HATCHED =        2;
    static final public int BS_PATTERN =        3;
    static final public int BS_INDEXED =        4;
    static final public int BS_DIBPATTERN =     5;
    static final public int BS_DIBPATTERNPT =   6;
    static final public int BS_PATTERN8X8 =     7;
    static final public int BS_DIBPATTERN8X8 =  8;
    static final public int BS_MONOPATTERN =    9;

      /* Hatch styles */
    static final public int HS_HORIZONTAL =     0;
    static final public int HS_VERTICAL =       1;
    static final public int HS_FDIAGONAL =      2;
    static final public int HS_BDIAGONAL =      3;
    static final public int HS_CROSS =          4;
    static final public int HS_DIAGCROSS =      5;
    
    static final public int PS_SOLID =        0x00000000;
    static final public int PS_DASH =         0x00000001;
    static final public int PS_DOT =          0x00000002;
    static final public int PS_DASHDOT =      0x00000003;
    static final public int PS_DASHDOTDOT =   0x00000004;
    static final public int PS_NULL =         0x00000005;
    static final public int PS_INSIDEFRAME =  0x00000006;
    static final public int PS_USERSTYLE =    0x00000007;
    static final public int PS_ALTERNATE =    0x00000008;
    static final public int PS_STYLE_MASK =   0x0000000f;
    
    static final public int PS_ENDCAP_ROUND = 0x00000000;
    static final public int PS_ENDCAP_SQUARE= 0x00000100;
    static final public int PS_ENDCAP_FLAT =  0x00000200;
    static final public int PS_ENDCAP_MASK =  0x00000f00;
    
    static final public int PS_JOIN_ROUND =   0x00000000;
    static final public int PS_JOIN_BEVEL =   0x00001000;
    static final public int PS_JOIN_MITER =   0x00002000;
    static final public int PS_JOIN_MASK =    0x0000f000;
    
    static final public int PS_COSMETIC =     0x00000000;
    static final public int PS_GEOMETRIC =    0x00010000;
    static final public int PS_TYPE_MASK =    0x000f0000;
    
      /* lfWeight values */
    static final public int FW_DONTCARE =       0;
    static final public int FW_THIN =           100;
    static final public int FW_EXTRALIGHT =     200;
    static final public int FW_ULTRALIGHT =     200;
    static final public int FW_LIGHT =          300;
    static final public int FW_NORMAL =         400;
    static final public int FW_REGULAR =        400;
    static final public int FW_MEDIUM =         500;
    static final public int FW_SEMIBOLD =       600;
    static final public int FW_DEMIBOLD =       600;
    static final public int FW_BOLD =           700;
    static final public int FW_EXTRABOLD =      800;
    static final public int FW_ULTRABOLD =      800;
    static final public int FW_HEAVY =          900;
    static final public int FW_BLACK =          900;
    
      /* lfCharSet values */
    static final public int ANSI_CHARSET =      0;   /* CP1252, ansi-0, iso8859-{1,15} */
    static final public int DEFAULT_CHARSET =   1;
    static final public int SYMBOL_CHARSET =    2;
    static final public int OEM_CHARSET =       255;

    static final public int FS_LATIN1 =         0x00000001;
    static final public int FS_LATIN2 =         0x00000002;
    static final public int FS_SYMBOL =         0x80000000;
    
      /* lfOutPrecision values */
    static final public int OUT_DEFAULT_PRECIS =    0;
    static final public int OUT_STRING_PRECIS =     1;
    static final public int OUT_CHARACTER_PRECIS =  2;
    static final public int OUT_STROKE_PRECIS =     3;
    static final public int OUT_TT_PRECIS =         4;
    static final public int OUT_DEVICE_PRECIS =     5;
    static final public int OUT_RASTER_PRECIS =     6;
    static final public int OUT_TT_ONLY_PRECIS =    7;
    static final public int OUT_OUTLINE_PRECIS =    8;
    
      /* lfClipPrecision values */
    static final public int CLIP_DEFAULT_PRECIS =   0x00;
    static final public int CLIP_CHARACTER_PRECIS = 0x01;
    static final public int CLIP_STROKE_PRECIS =    0x02;
    static final public int CLIP_MASK =             0x0F;
    static final public int CLIP_LH_ANGLES =        0x10;
    static final public int CLIP_TT_ALWAYS =        0x20;
    static final public int CLIP_EMBEDDED =         0x80;
    
      /* lfQuality values */
    static final public int DEFAULT_QUALITY =           0;
    static final public int DRAFT_QUALITY =             1;
    static final public int PROOF_QUALITY =             2;
    static final public int NONANTIALIASED_QUALITY =    3;
    static final public int ANTIALIASED_QUALITY =       4;
    static final public int CLEARTYPE_QUALITY =         5;
    static final public int CLEARTYPE_NATURAL_QUALITY = 6;
    
      /* lfPitchAndFamily pitch values */
    static final public int DEFAULT_PITCH =     0x00;
    static final public int FIXED_PITCH =       0x01;
    static final public int VARIABLE_PITCH =    0x02;
    static final public int MONO_FONT =         0x08;
    
    static final public int FF_DONTCARE =       0x00;
    static final public int FF_ROMAN =          0x10;
    static final public int FF_SWISS =          0x20;
    static final public int FF_MODERN =         0x30;
    static final public int FF_SCRIPT =         0x40;
    static final public int FF_DECORATIVE =     0x50;
    
    static final public int CLR_INVALID = -1;

    static final public int INVALID_HANDLE_VALUE = -1;

    /* Unicode char type flags */
    static final public int	CT_CTYPE1 =	0x0001;	/* usual ctype */
    static final public int	CT_CTYPE2 =	0x0002;	/* bidirectional layout info */
    static final public int CT_CTYPE3 =	0x0004;	/* textprocessing info */

    static final public int GENERIC_READ    = 0x80000000;
    static final public int GENERIC_WRITE   = 0x40000000;
    static final public int GENERIC_EXECUTE = 0x20000000;
    static final public int GENERIC_ALL     = 0x10000000;

    static final public String SYSTEM32_PATH = "C:\\Windows\\System32\\";
    static final public String WIN32_PATH = "C:\\WINDOWS";
    static final public String TEMP_PATH = "C:\\WINDOWS\\TEMP\\";

    static public void writed(int address, int value) {
        Memory.mem_writed(address, value);
    }

    static public int readb(int address) {
        return Memory.mem_readb(address);
    }

    static public int readw(int address) {
        return Memory.mem_readw(address);
    }

    static public int readd(int address) {
        return Memory.mem_readd(address);
    }

    static public int Handle(WinObject object) {
        if (object == null)
            return 0;
        return object.handle;
    }

    static protected int BOOL(boolean b) {
        return b?WinAPI.TRUE:WinAPI.FALSE;
    }

    static protected int getTempBuffer(int size) {
        return WinSystem.getCurrentProcess().getTemp(size);
    }

    static public final int WM_CREATE =                      0x0001;
    static public final int WM_DESTROY =                     0x0002;
    static public final int WM_MOVE =                        0x0003;
    static public final int WM_SIZE =                        0x0005;
    static public final int WM_ACTIVATE =                    0x0006;
    static public final int WM_SETFOCUS =                    0x0007;
    static public final int WM_KILLFOCUS =                   0x0008;
    static public final int WM_ENABLE =                      0x000A;
    static public final int WM_SETREDRAW =                   0x000B;
    static public final int WM_SETTEXT =                     0x000C;
    static public final int WM_GETTEXT =                     0x000D;
    static public final int WM_GETTEXTLENGTH =               0x000E;
    static public final int WM_PAINT =                       0x000F;
    static public final int WM_CLOSE =                       0x0010;
    static public final int WM_QUERYENDSESSION =             0x0011;
    static public final int WM_QUIT =                        0x0012;
    static public final int WM_QUERYOPEN =                   0x0013;
    static public final int WM_ERASEBKGND =                  0x0014;
    static public final int WM_SYSCOLORCHANGE =              0x0015;
    static public final int WM_ENDSESSION =                  0x0016;
    static public final int WM_SHOWWINDOW =                  0x0018;
    static public final int WM_CTLCOLOR =                    0x0019;
    static public final int WM_ACTIVATEAPP =                 0x001C;
    static public final int WM_SETCURSOR =                   0x0020;
    static public final int WM_MOUSEACTIVATE =               0x0021;
    static public final int WM_CHILDACTIVATE =               0x0022;
    static public final int WM_QUEUESYNC =                   0x0023;
    static public final int WM_GETMINMAXINFO =               0x0024;

    static public final int WM_PAINTICON =                   0x0026;
    static public final int WM_ICONERASEBKGND =              0x0027;
    static public final int WM_NEXTDLGCTL =                  0x0028;
    static public final int WM_SPOOLERSTATUS =               0x002A;
    static public final int WM_DRAWITEM =                    0x002B;
    static public final int WM_MEASUREITEM =                 0x002C;
    static public final int WM_DELETEITEM =                  0x002D;
    static public final int WM_VKEYTOITEM =                  0x002E;
    static public final int WM_CHARTOITEM =                  0x002F;
    static public final int WM_SETFONT =                     0x0030;
    static public final int WM_GETFONT =                     0x0031;
    static public final int WM_SETHOTKEY =                   0x0032;
    static public final int WM_GETHOTKEY =                   0x0033;
    static public final int WM_QUERYDRAGICON =               0x0037;
    static public final int WM_COMPAREITEM =                 0x0039;


    static public final int WM_WINDOWPOSCHANGING =           0x0046;
    static public final int WM_WINDOWPOSCHANGED =            0x0047;

    static public final int WM_NCCREATE =                    0x0081;
    static public final int WM_NCDESTROY =                   0x0082;
    static public final int WM_NCCALCSIZE =                  0x0083;
    static public final int WM_NCHITTEST =                   0x0084;
    static public final int WM_NCPAINT =                     0x0085;
    static public final int WM_NCACTIVATE =                  0x0086;
    static public final int WM_GETDLGCODE =                  0x0087;
    static public final int WM_SYNCPAINT =                   0x0088;


    static public final int WM_NCMOUSEMOVE =                 0x00A0;
    static public final int WM_NCLBUTTONDOWN =               0x00A1;
    static public final int WM_NCLBUTTONUP =                 0x00A2;
    static public final int WM_NCLBUTTONDBLCLK =             0x00A3;
    static public final int WM_NCRBUTTONDOWN =               0x00A4;
    static public final int WM_NCRBUTTONUP =                 0x00A5;
    static public final int WM_NCRBUTTONDBLCLK =             0x00A6;
    static public final int WM_NCMBUTTONDOWN =               0x00A7;
    static public final int WM_NCMBUTTONUP =                 0x00A8;
    static public final int WM_NCMBUTTONDBLCLK =             0x00A9;


    static public final int WM_KEYFIRST =                    0x0100;
    static public final int WM_KEYDOWN =                     0x0100;
    static public final int WM_KEYUP =                       0x0101;
    static public final int WM_CHAR =                        0x0102;
    static public final int WM_DEADCHAR =                    0x0103;
    static public final int WM_SYSKEYDOWN =                  0x0104;
    static public final int WM_SYSKEYUP =                    0x0105;
    static public final int WM_SYSCHAR =                     0x0106;
    static public final int WM_SYSDEADCHAR =                 0x0107;
    static public final int WM_KEYLAST =                     0x0108;

    static public final int WM_INITDIALOG =                  0x0110;
    static public final int WM_COMMAND =                     0x0111;
    static public final int WM_SYSCOMMAND =                  0x0112;
    static public final int WM_TIMER =                       0x0113;
    static public final int WM_HSCROLL =                     0x0114;
    static public final int WM_VSCROLL =                     0x0115;
    static public final int WM_INITMENU =                    0x0116;
    static public final int WM_INITMENUPOPUP =               0x0117;
    static public final int WM_MENUSELECT =                  0x011F;
    static public final int WM_MENUCHAR =                    0x0120;
    static public final int WM_ENTERIDLE =                   0x0121;

    static public final int WM_CTLCOLORMSGBOX =              0x0132;
    static public final int WM_CTLCOLOREDIT =                0x0133;
    static public final int WM_CTLCOLORLISTBOX =             0x0134;
    static public final int WM_CTLCOLORBTN =                 0x0135;
    static public final int WM_CTLCOLORDLG =                 0x0136;
    static public final int WM_CTLCOLORSCROLLBAR =           0x0137;
    static public final int WM_CTLCOLORSTATIC =              0x0138;

    static public final int WM_MOUSEFIRST =                  0x0200;
    static public final int WM_MOUSEMOVE =                   0x0200;
    static public final int WM_LBUTTONDOWN =                 0x0201;
    static public final int WM_LBUTTONUP =                   0x0202;
    static public final int WM_LBUTTONDBLCLK =               0x0203;
    static public final int WM_RBUTTONDOWN =                 0x0204;
    static public final int WM_RBUTTONUP =                   0x0205;
    static public final int WM_RBUTTONDBLCLK =               0x0206;
    static public final int WM_MBUTTONDOWN =                 0x0207;
    static public final int WM_MBUTTONUP =                   0x0208;
    static public final int WM_MBUTTONDBLCLK =               0x0209;
    static public final int WM_MOUSEWHEEL =                  0x020A;

    static public final int WM_PARENTNOTIFY =                0x0210;
    static public final int WM_ENTERMENULOOP =               0x0211;
    static public final int WM_EXITMENULOOP =                0x0212;

    static public final int WM_DRAWCLIPBOARD =               0x0308;
    static public final int WM_PAINTCLIPBOARD =              0x0309;
    static public final int WM_VSCROLLCLIPBOARD =            0x030A;
    static public final int WM_SIZECLIPBOARD =               0x030B;
    static public final int WM_ASKCBFORMATNAME =             0x030C;
    static public final int WM_CHANGECBCHAIN =               0x030D;
    static public final int WM_HSCROLLCLIPBOARD =            0x030E;

    static public final int WM_QUERYNEWPALETTE =             0x030F;
    static public final int WM_PALETTEISCHANGING =           0x0310;
    static public final int WM_PALETTECHANGED =              0x0311;
    static public final int WM_HOTKEY =                      0x0312;

    static public final int WM_PRINT =                       0x0317;
    static public final int WM_PRINTCLIENT =                 0x0318;
    static public final int WM_APPCOMMAND =                  0x0319;
    static public final int WM_THEMECHANGED =                0x031A;
    static public final int WM_CLIPBOARDUPDATE =             0x031D;

    static public final int MM_MCINOTIFY =                   0x03B9;

    static public final int WM_USER =                        0x0400;

    static public final int WS_OVERLAPPED =     0x00000000;
    static public final int WS_POPUP =          0x80000000;
    static public final int WS_CHILD =          0x40000000;
    static public final int WS_MINIMIZE =       0x20000000;
    static public final int WS_VISIBLE =        0x10000000;
    static public final int WS_DISABLED =       0x08000000;
    static public final int WS_CLIPSIBLINGS =   0x04000000;
    static public final int WS_CLIPCHILDREN =   0x02000000;
    static public final int WS_MAXIMIZE =       0x01000000;
    static public final int WS_CAPTION =        0x00C00000;     /* WS_BORDER | WS_DLGFRAME  */
    static public final int WS_BORDER =         0x00800000;
    static public final int WS_DLGFRAME =       0x00400000;
    static public final int WS_VSCROLL =        0x00200000;
    static public final int WS_HSCROLL =        0x00100000;
    static public final int WS_SYSMENU =        0x00080000;
    static public final int WS_THICKFRAME =     0x00040000;
    static public final int WS_GROUP =          0x00020000;
    static public final int WS_TABSTOP =        0x00010000;
    static public final int WS_MINIMIZEBOX =    0x00020000;
    static public final int WS_MAXIMIZEBOX =    0x00010000;

    static public final int WS_EX_DLGMODALFRAME =    0x00000001;
    static public final int WS_EX_NOPARENTNOTIFY =   0x00000004;
    static public final int WS_EX_TOPMOST =          0x00000008;
    static public final int WS_EX_ACCEPTFILES =      0x00000010;
    static public final int WS_EX_TRANSPARENT =      0x00000020;
    static public final int WS_EX_MDICHILD =         0x00000040;
    static public final int WS_EX_TOOLWINDOW =       0x00000080;
    static public final int WS_EX_WINDOWEDGE =       0x00000100;
    static public final int WS_EX_CLIENTEDGE =       0x00000200;
    static public final int WS_EX_CONTEXTHELP =      0x00000400;
    static public final int WS_EX_RIGHT =            0x00001000;
    static public final int WS_EX_LEFT =             0x00000000;
    static public final int WS_EX_RTLREADING =       0x00002000;
    static public final int WS_EX_LTRREADING =       0x00000000;
    static public final int WS_EX_LEFTSCROLLBAR =    0x00004000;
    static public final int WS_EX_RIGHTSCROLLBAR =   0x00000000;
    static public final int WS_EX_CONTROLPARENT =    0x00010000;
    static public final int WS_EX_STATICEDGE =       0x00020000;
    static public final int WS_EX_APPWINDOW =        0x00040000;

    static public final int HTERROR =           -2;
    static public final int HTTRANSPARENT =     -1;
    static public final int HTNOWHERE =         0;
    static public final int HTCLIENT =          1;
    static public final int HTCAPTION =         2;
    static final public int HTSYSMENU =         3;
    static final public int HTSIZE =            4;
    static final public int HTGROWBOX =         HTSIZE;
    static final public int HTMENU =            5;
    static final public int HTHSCROLL =         6;
    static final public int HTVSCROLL =         7;
    static final public int HTMINBUTTON =       8;
    static final public int HTREDUCE =          HTMINBUTTON;
    static final public int HTMAXBUTTON =       9;
    static final public int HTZOOM =            HTMAXBUTTON;
    static final public int HTLEFT =            10;
    static final public int HTSIZEFIRST =       HTLEFT;
    static final public int HTRIGHT =           11;
    static final public int HTTOP =             12;
    static final public int HTTOPLEFT =         13;
    static final public int HTTOPRIGHT =        14;
    static final public int HTBOTTOM =          15;
    static final public int HTBOTTOMLEFT =      16;
    static final public int HTBOTTOMRIGHT =     17;
    static final public int HTSIZELAST =        HTBOTTOMRIGHT;
    static final public int HTBORDER =          18;
    static final public int HTOBJECT =          19;
    static final public int HTCLOSE =           20;
    static final public int HTHELP =            21;
    
    /* GetSystemMetrics() codes */
    static final public int SM_CXSCREEN =       0;
    static final public int SM_CYSCREEN =       1;
    static final public int SM_CXVSCROLL =      2;
    static final public int SM_CYHSCROLL =      3;
    static final public int SM_CYCAPTION =      4;
    static final public int SM_CXBORDER =       5;
    static final public int SM_CYBORDER =       6;
    static final public int SM_CXDLGFRAME =     7;
    static final public int SM_CYDLGFRAME =     8;
    static final public int SM_CYVTHUMB =       9;
    static final public int SM_CXHTHUMB =       10;
    static final public int SM_CXICON =         11;
    static final public int SM_CYICON =         12;
    static final public int SM_CXCURSOR =       13;
    static final public int SM_CYCURSOR =       14;
    static final public int SM_CYMENU =         15;
    static final public int SM_CXFULLSCREEN =   16;
    static final public int SM_CYFULLSCREEN =   17;
    static final public int SM_CYKANJIWINDOW =  18;
    static final public int SM_MOUSEPRESENT =   19;
    static final public int SM_CYVSCROLL =      20;
    static final public int SM_CXHSCROLL =      21;
    static final public int SM_DEBUG =          22;
    static final public int SM_SWAPBUTTON =     23;
    static final public int SM_RESERVED1 =      24;
    static final public int SM_RESERVED2 =      25;
    static final public int SM_RESERVED3 =      26;
    static final public int SM_RESERVED4 =      27;
    static final public int SM_CXMIN =          28;
    static final public int SM_CYMIN =          29;
    static final public int SM_CXSIZE =         30;
    static final public int SM_CYSIZE =         31;
    static final public int SM_CXFRAME =        32;
    static final public int SM_CYFRAME =        33;
    static final public int SM_CXMINTRACK =     34;
    static final public int SM_CYMINTRACK =     35;
    static final public int SM_CXDOUBLECLK =    36;
    static final public int SM_CYDOUBLECLK =    37;
    static final public int SM_CXICONSPACING =  38;
    static final public int SM_CYICONSPACING =  39;
    static final public int SM_MENUDROPALIGNMENT=40;
    static final public int SM_PENWINDOWS =     41;
    static final public int SM_DBCSENABLED =    42;
    static final public int SM_CMOUSEBUTTONS =  43;
    static final public int SM_CXFIXEDFRAME =   SM_CXDLGFRAME;
    static final public int SM_CYFIXEDFRAME =   SM_CYDLGFRAME;
    static final public int SM_CXSIZEFRAME =    SM_CXFRAME;
    static final public int SM_CYSIZEFRAME =    SM_CYFRAME;
    static final public int SM_SECURE =         44;
    static final public int SM_CXEDGE =         45;
    static final public int SM_CYEDGE =         46;
    static final public int SM_CXMINSPACING =   47;
    static final public int SM_CYMINSPACING =   48;
    static final public int SM_CXSMICON	=       49;
    static final public int SM_CYSMICON	=       50;
    static final public int SM_CYSMCAPTION =    51;
    static final public int SM_CXSMSIZE	=       52;
    static final public int SM_CYSMSIZE	=       53;
    static final public int SM_CXMENUSIZE =     54;
    static final public int SM_CYMENUSIZE =     55;
    static final public int SM_ARRANGE =        56;
    static final public int SM_CXMINIMIZED =    57;
    static final public int SM_CYMINIMIZED =    58;
    static final public int SM_CXMAXTRACK =     59;
    static final public int SM_CYMAXTRACK =     60;
    static final public int SM_CXMAXIMIZED =    61;
    static final public int SM_CYMAXIMIZED =    62;
    static final public int SM_NETWORK =        63;
    static final public int SM_CLEANBOOT =      67;
    static final public int SM_CXDRAG =         68;
    static final public int SM_CYDRAG =         69;
    static final public int SM_SHOWSOUNDS =     70;
    static final public int SM_CXMENUCHECK =    71;
    static final public int SM_CYMENUCHECK =    72;
    static final public int SM_SLOWMACHINE =    73;
    static final public int SM_MIDEASTENABLED = 74;
    static final public int SM_MOUSEWHEELPRESENT=75;
    static final public int SM_XVIRTUALSCREEN = 76;
    static final public int SM_YVIRTUALSCREEN = 77;
    static final public int SM_CXVIRTUALSCREEN =78;
    static final public int SM_CYVIRTUALSCREEN =79;
    static final public int SM_CMONITORS =      80;
    static final public int SM_SAMEDISPLAYFORMAT=81;
    static final public int SM_IMMENABLED =     82;
    static final public int SM_CXFOCUSBORDER =  83;
    static final public int SM_CYFOCUSBORDER =  84;
    static final public int SM_TABLETPC =       86;
    static final public int SM_MEDIACENTER =    87;
    static final public int SM_STARTER =        88;
    static final public int SM_SERVERR2 =       89;
    static final public int SM_CMETRICS =       90;
    static final public int SM_MOUSEHORIZONTALWHEELPRESENT = 91;

    static final public int SM_REMOTESESSION =      0x1000;
    static final public int SM_SHUTTINGDOWN =       0x2000;
    static final public int SM_REMOTECONTROL =      0x2001;
    static final public int SM_CARETBLINKINGENABLED=0x2002;

      /* Offsets for GetWindowLongPtr() and SetWindowLongPtr() */
    static public final int GWLP_USERDATA =     -21;
    static public final int GWLP_ID =           -12;
    static public final int GWLP_HWNDPARENT =   -8;
    static public final int GWLP_HINSTANCE =    -6;
    static public final int GWLP_WNDPROC =      -4;
    static public final int DWLP_MSGRESULT =     0;
    static public final int DWLP_DLGPROC =       DWLP_MSGRESULT + 4;
    static public final int DWLP_USER =          DWLP_DLGPROC + 4;

    static public final int GWL_WNDPROC =       -4;
    static public final int GWL_HINSTANCE =     -6;
    static public final int GWL_HWNDPARENT =    -8;
    static public final int GWL_STYLE =         -16;
    static public final int GWL_EXSTYLE =       -20;
    static public final int GWL_USERDATA =      -21;
    static public final int GWL_ID =            -12;

    static public final int GW_HWNDFIRST =       0;
    static public final int GW_HWNDLAST =        1;
    static public final int GW_HWNDNEXT =        2;
    static public final int GW_HWNDPREV =        3;
    static public final int GW_OWNER =           4;
    static public final int GW_CHILD =           5;

    static public final int GA_MIC =         1;
    static public final int GA_PARENT =      1;
    static public final int GA_ROOT =        2;
    static public final int GA_ROOTOWNER =   3;
    static public final int GA_MAC =         4;

    static public final int WA_INACTIVE =          0;
    static public final int WA_ACTIVE =            1;
    static public final int WA_CLICKACTIVE =       2;

    static public final int WH_MIN =            -1;
    static public final int WH_MSGFILTER =      -1;
    static public final int WH_JOURNALRECORD =   0;
    static public final int WH_JOURNALPLAYBACK = 1;
    static public final int WH_KEYBOARD	=        2;
    static public final int WH_GETMESSAGE =      3;
    static public final int WH_CALLWNDPROC =     4;
    static public final int WH_CBT =             5;
    static public final int WH_SYSMSGFILTER =    6;
    static public final int WH_MOUSE =           7;
    static public final int WH_HARDWARE =        8;
    static public final int WH_DEBUG =           9;
    static public final int WH_SHELL =           10;
    static public final int WH_FOREGROUNDIDLE =  11;
    static public final int WH_CALLWNDPROCRET =  12;
    static public final int WH_KEYBOARD_LL =     13;
    static public final int WH_MOUSE_LL =        14;
    static public final int WH_MAX =             14;

    static public final int HCBT_MOVESIZE =      0;
    static public final int HCBT_MINMAX =        1;
    static public final int HCBT_QS =            2;
    static public final int HCBT_CREATEWND =     3;
    static public final int HCBT_DESTROYWND =    4;
    static public final int HCBT_ACTIVATE =      5;
    static public final int HCBT_CLICKSKIPPED =  6;
    static public final int HCBT_KEYSKIPPED =    7;
    static public final int HCBT_SYSCOMMAND =    8;
    static public final int HCBT_SETFOCUS =      9;

    static public final int HC_ACTION =          0;
    static public final int HC_GETNEXT =         1;
    static public final int HC_SKIP =            2;
    static public final int HC_NOREMOVE =        3;
    static public final int HC_NOREM =           HC_NOREMOVE;
    static public final int HC_SYSMODALON =      4;
    static public final int HC_SYSMODALOFF =     5;

    static public final int HWND_DESKTOP =       0;
    static public final int HWND_BROADCAST =     0xffff;

    /* SetWindowPos() and WINDOWPOS flags */
    static public final int SWP_NOSIZE =         0x0001;
    static public final int SWP_NOMOVE =         0x0002;
    static public final int SWP_NOZORDER =       0x0004;
    static public final int SWP_NOREDRAW =       0x0008;
    static public final int SWP_NOACTIVATE =     0x0010;
    static public final int SWP_FRAMECHANGED =   0x0020;  /* The frame changed: send WM_NCCALCSIZE */
    static public final int SWP_SHOWWINDOW =     0x0040;
    static public final int SWP_HIDEWINDOW =     0x0080;
    static public final int SWP_NOCOPYBITS =     0x0100;
    static public final int SWP_NOOWNERZORDER =  0x0200;  /* Don't do owner Z ordering */
    
    static public final int SWP_DRAWFRAME =      SWP_FRAMECHANGED;
    static public final int SWP_NOREPOSITION =   SWP_NOOWNERZORDER;
    
    static public final int SWP_NOSENDCHANGING = 0x0400;
    static public final int SWP_DEFERERASE =     0x2000;
    static public final int SWP_ASYNCWINDOWPOS = 0x4000;
    
    /* undocumented SWP flags - from SDK 3.1 */
    static public final int SWP_NOCLIENTSIZE =   0x0800;
    static public final int SWP_NOCLIENTMOVE =   0x1000;
    static public final int SWP_STATECHANGED =   0x8000;
    
    /* SetWindowPos() hwndInsertAfter field values */
    static public final int HWND_TOP =           0;
    static public final int HWND_BOTTOM =        1;
    static public final int HWND_TOPMOST =       -1;
    static public final int HWND_NOTOPMOST =     -2;
    static public final int HWND_MESSAGE =       -3;

    /* WM_SIZE message wParam values */
    static public final int SIZE_RESTORED =         0;
    static public final int SIZE_MINIMIZED =        1;
    static public final int SIZE_MAXIMIZED =        2;
    static public final int SIZE_MAXSHOW =          3;
    static public final int SIZE_MAXHIDE =          4;
    static public final int SIZENORMAL =            SIZE_RESTORED;
    static public final int SIZEICONIC =            SIZE_MINIMIZED;
    static public final int SIZEFULLSCREEN =        SIZE_MAXIMIZED;
    static public final int SIZEZOOMSHOW =          SIZE_MAXSHOW;
    static public final int SIZEZOOMHIDE =          SIZE_MAXHIDE;
    
    /* Shell hook values */
    static public final int HSHELL_WINDOWCREATED =      1;
    static public final int HSHELL_WINDOWDESTROYED =    2;
    static public final int HSHELL_ACTIVATESHELLWINDOW= 3;
    static public final int HSHELL_WINDOWACTIVATED =    4;
    static public final int HSHELL_GETMINRECT =         5;
    static public final int HSHELL_REDRAW =             6;
    static public final int HSHELL_TASKMAN =            7;
    static public final int HSHELL_LANGUAGE =           8;
    static public final int HSHELL_SYSMENU =            9;
    static public final int HSHELL_ENDTASK =            10;
    static public final int HSHELL_ACCESSIBILITYSTATE = 11;
    static public final int HSHELL_APPCOMMAND =         12;
    static public final int HSHELL_WINDOWREPLACED =     13;
    static public final int HSHELL_WINDOWREPLACING =    14;
    
    /*** ShowWindow() codes ***/
    static public final int SW_HIDE =               0;
    static public final int SW_SHOWNORMAL =         1;
    static public final int SW_NORMAL =             SW_SHOWNORMAL;
    static public final int SW_SHOWMINIMIZED =      2;
    static public final int SW_SHOWMAXIMIZED =      3;
    static public final int SW_MAXIMIZE =           SW_SHOWMAXIMIZED;
    static public final int SW_SHOWNOACTIVATE =     4;
    static public final int SW_SHOW =               5;
    static public final int SW_MINIMIZE =           6;
    static public final int SW_SHOWMINNOACTIVE =    7;
    static public final int SW_SHOWNA =             8;
    static public final int SW_RESTORE =            9;
    static public final int SW_SHOWDEFAULT =        10;
    static public final int SW_FORCEMINIMIZE =      11;
    static public final int SW_MAX =                11;
    static public final int SW_NORMALNA =           0xCC; /* Undocumented. Flag in MinMaximize */
    
    public static final int IDOK =                  1;
    public static final int IDCANCEL =              2;
    public static final int IDABORT =               3;
    public static final int IDRETRY =               4;
    public static final int IDIGNORE =              5;
    public static final int IDYES =                 6;
    public static final int IDNO =                  7;
    public static final int IDCLOSE =               8;
    public static final int IDHELP =                9;
    public static final int IDTRYAGAIN =            10;
    public static final int IDCONTINUE =            11;

    public static final int DLGC_WANTARROWS =      0x0001;
    public static final int DLGC_WANTTAB =         0x0002;
    public static final int DLGC_WANTALLKEYS =     0x0004;
    public static final int DLGC_WANTMESSAGE =     0x0004;
    public static final int DLGC_HASSETSEL =       0x0008;
    public static final int DLGC_DEFPUSHBUTTON =   0x0010;
    public static final int DLGC_UNDEFPUSHBUTTON = 0x0020;
    public static final int DLGC_RADIOBUTTON =     0x0040;
    public static final int DLGC_WANTCHARS =       0x0080;
    public static final int DLGC_STATIC =          0x0100;
    public static final int DLGC_BUTTON =          0x2000;

    static public final int CB_GETEDITSEL =           0x0140;
    static public final int CB_LIMITTEXT =            0x0141;
    static public final int CB_SETEDITSEL =           0x0142;
    static public final int CB_ADDSTRING =            0x0143;
    static public final int CB_DELETESTRING =         0x0144;
    static public final int CB_DIR =                  0x0145;
    static public final int CB_GETCOUNT =             0x0146;
    static public final int CB_GETCURSEL =            0x0147;
    static public final int CB_GETLBTEXT =            0x0148;
    static public final int CB_GETLBTEXTLEN =         0x0149;
    static public final int CB_INSERTSTRING =         0x014a;
    static public final int CB_RESETCONTENT =         0x014b;
    static public final int CB_FINDSTRING =           0x014c;
    static public final int CB_SELECTSTRING =         0x014d;
    static public final int CB_SETCURSEL =            0x014e;
    static public final int CB_SHOWDROPDOWN =         0x014f;
    static public final int CB_GETITEMDATA =          0x0150;
    static public final int CB_SETITEMDATA =          0x0151;
    static public final int CB_GETDROPPEDCONTROLRECT =0x0152;
    static public final int CB_SETITEMHEIGHT =        0x0153;
    static public final int CB_GETITEMHEIGHT =        0x0154;
    static public final int CB_SETEXTENDEDUI =        0x0155;
    static public final int CB_GETEXTENDEDUI =        0x0156;
    static public final int CB_GETDROPPEDSTATE =      0x0157;
    static public final int CB_FINDSTRINGEXACT =      0x0158;
    static public final int CB_SETLOCALE =            0x0159;
    static public final int CB_GETLOCALE =            0x015a;
    static public final int CB_GETTOPINDEX =          0x015b;
    static public final int CB_SETTOPINDEX =          0x015c;
    static public final int CB_GETHORIZONTALEXTENT =  0x015d;
    static public final int CB_SETHORIZONTALEXTENT =  0x015e;
    static public final int CB_GETDROPPEDWIDTH =      0x015f;
    static public final int CB_SETDROPPEDWIDTH =      0x0160;
    static public final int CB_INITSTORAGE =          0x0161;
    static public final int CB_MULTIPLEADDSTRING =    0x0163;
    static public final int CB_GETCOMBOBOXINFO =      0x0164;
    static public final int CB_MSGMAX =               0x0165;

    /*** Button control styles ***/
    static public final int BS_PUSHBUTTON =         0x00000000;
    static public final int BS_DEFPUSHBUTTON =      0x00000001;
    static public final int BS_CHECKBOX =           0x00000002;
    static public final int BS_AUTOCHECKBOX =       0x00000003;
    static public final int BS_RADIOBUTTON =        0x00000004;
    static public final int BS_3STATE =             0x00000005;
    static public final int BS_AUTO3STATE =         0x00000006;
    static public final int BS_GROUPBOX =           0x00000007;
    static public final int BS_USERBUTTON =         0x00000008;
    static public final int BS_AUTORADIOBUTTON =    0x00000009;
    static public final int BS_PUSHBOX =            0x0000000A;
    static public final int BS_OWNERDRAW =          0x0000000B;
    static public final int BS_TYPEMASK =           0x0000000F;
    static public final int BS_LEFTTEXT =           0x00000020;
    static public final int BS_RIGHTBUTTON =        BS_LEFTTEXT;

    static public final int BS_TEXT =               0x00000000;
    static public final int BS_ICON =               0x00000040;
    static public final int BS_BITMAP =             0x00000080;
    static public final int BS_LEFT =               0x00000100;
    static public final int BS_RIGHT =              0x00000200;
    static public final int BS_CENTER =             0x00000300;
    static public final int BS_TOP =                0x00000400;
    static public final int BS_BOTTOM =             0x00000800;
    static public final int BS_VCENTER =            0x00000C00;
    static public final int BS_PUSHLIKE =           0x00001000;
    static public final int BS_MULTILINE =          0x00002000;
    static public final int BS_NOTIFY =             0x00004000;
    static public final int BS_FLAT =               0x00008000;


    /*** Button notification codes ***/
    static final public int BN_CLICKED =         0;
    static final public int BN_PAINT =           1;
    static final public int BN_HILITE =          2;
    static final public int BN_UNHILITE =        3;
    static final public int BN_DISABLE =         4;
    static final public int BN_DOUBLECLICKED =   5;
    static final public int BN_PUSHED =          BN_HILITE;
    static final public int BN_UNPUSHED =        BN_UNHILITE;
    static final public int BN_DBLCLK =          BN_DOUBLECLICKED;
    static final public int BN_SETFOCUS =        6;
    static final public int BN_KILLFOCUS =       7;

    /*** Win32 button control messages ***/
    static public final int BM_GETCHECK =           0x00f0;
    static public final int BM_SETCHECK =           0x00f1;
    static public final int BM_GETSTATE =           0x00f2;
    static public final int BM_SETSTATE =           0x00f3;
    static public final int BM_SETSTYLE =           0x00f4;
    static public final int BM_CLICK =              0x00f5;
    static public final int BM_GETIMAGE =           0x00f6;
    static public final int BM_SETIMAGE =           0x00f7;
    static public final int BM_SETDONTCLICK =       0x00f8;

    /* Button states */
    static public final int BST_UNCHECKED =         0x0000;
    static public final int BST_CHECKED =           0x0001;
    static public final int BST_INDETERMINATE =     0x0002;
    static public final int BST_PUSHED =            0x0004;
    static public final int BST_FOCUS =             0x0008;

    static final public int EM_GETSEL =              0x00B0;
    static final public int EM_SETSEL =              0x00B1;
    static final public int EM_GETRECT =             0x00B2;
    static final public int EM_SETRECT =             0x00B3;
    static final public int EM_SETRECTNP =           0x00B4;
    static final public int EM_SCROLL =              0x00B5;
    static final public int EM_LINESCROLL =          0x00B6;
    static final public int EM_SCROLLCARET =         0x00B7;
    static final public int EM_GETMODIFY =           0x00B8;
    static final public int EM_SETMODIFY =           0x00B9;
    static final public int EM_GETLINECOUNT =        0x00BA;
    static final public int EM_LINEINDEX =           0x00BB;
    static final public int EM_SETHANDLE =           0x00BC;
    static final public int EM_GETHANDLE =           0x00BD;
    static final public int EM_GETTHUMB =            0x00BE;
    static final public int EM_LINELENGTH =          0x00C1;
    static final public int EM_REPLACESEL =          0x00C2;
    static final public int EM_GETLINE =             0x00C4;
    static final public int EM_LIMITTEXT =           0x00C5;
    static final public int EM_CANUNDO =             0x00C6;
    static final public int EM_UNDO =                0x00C7;
    static final public int EM_FMTLINES =            0x00C8;
    static final public int EM_LINEFROMCHAR =        0x00C9;
    static final public int EM_SETTABSTOPS =         0x00CB;
    static final public int EM_SETPASSWORDCHAR =     0x00CC;
    static final public int EM_EMPTYUNDOBUFFER =     0x00CD;
    static final public int EM_GETFIRSTVISIBLELINE = 0x00CE;
    static final public int EM_SETREADONLY =         0x00CF;
    static final public int EM_SETWORDBREAKPROC =    0x00D0;
    static final public int EM_GETWORDBREAKPROC =    0x00D1;
    static final public int EM_GETPASSWORDCHAR =     0x00D2;
    static final public int EM_SETMARGINS =          0x00D3;
    static final public int EM_GETMARGINS =          0x00D4;
    static final public int EM_SETLIMITTEXT =        EM_LIMITTEXT;
    static final public int EM_GETLIMITTEXT =        0x00D5;
    static final public int EM_POSFROMCHAR =         0x00D6;
    static final public int EM_CHARFROMPOS =         0x00D7;
    static final public int EM_SETIMESTATUS =        0x00D8;
    static final public int EM_GETIMESTATUS =        0x00D9;

    static final public int ERROR =            0;
    static final public int NULLREGION =       1;
    static final public int SIMPLEREGION =     2;
    static final public int COMPLEXREGION =    3;
    static final public int RGN_ERROR =        ERROR;

    static final public int RGN_AND =          1;
    static final public int RGN_OR =           2;
    static final public int RGN_XOR =          3;
    static final public int RGN_DIFF =         4;
    static final public int RGN_COPY =         5;
    static final public int RGN_MIN =          RGN_AND;
    static final public int RGN_MAX =          RGN_COPY;

    /* GetDCEx flags */
    static final public int DCX_WINDOW =          0x00000001;
    static final public int DCX_CACHE =           0x00000002;
    static final public int DCX_NORESETATTRS =    0x00000004;
    static final public int DCX_CLIPCHILDREN =    0x00000008;
    static final public int DCX_CLIPSIBLINGS =    0x00000010;
    static final public int DCX_PARENTCLIP =      0x00000020;
    static final public int DCX_EXCLUDERGN =      0x00000040;
    static final public int DCX_INTERSECTRGN =    0x00000080;
    static final public int DCX_EXCLUDEUPDATE =   0x00000100;
    static final public int DCX_INTERSECTUPDATE = 0x00000200;
    static final public int DCX_LOCKWINDOWUPDATE =0x00000400;
    static final public int DCX_USESTYLE =        0x00010000;
    static final public int DCX_NORECOMPUTE =     0x00100000;
    static final public int DCX_VALIDATE =        0x00200000;

    static final public int GCL_MENUNAME =      -8;
    static final public int GCL_HBRBACKGROUND = -10;
    static final public int GCL_HCURSOR =       -12;
    static final public int GCL_HICON =         -14;
    static final public int GCL_HMODULE =       -16;
    static final public int GCL_WNDPROC =       -24;
    static final public int GCL_HICONSM =       -34;

    static final public int GCL_CBWNDEXTRA =    -18;
    static final public int GCL_CBCLSEXTRA =    -20;
    static final public int GCL_STYLE =         -26;
    static final public int GCW_ATOM =          -32;

    static final public int GCLP_MENUNAME =      -8;
    static final public int GCLP_HBRBACKGROUND = -10;
    static final public int GCLP_HCURSOR =       -12;
    static final public int GCLP_HICON =         -14;
    static final public int GCLP_HMODULE =       -16;
    static final public int GCLP_WNDPROC =       -24;
    static final public int GCLP_HICONSM =       -34;

    static final public int CS_VREDRAW =            0x00000001;
    static final public int CS_HREDRAW =            0x00000002;
    static final public int CS_KEYCVTWINDOW =       0x00000004; /* DDK / Win16 */
    static final public int CS_DBLCLKS =            0x00000008;
    static final public int CS_OWNDC =              0x00000020;
    static final public int CS_CLASSDC =            0x00000040;
    static final public int CS_PARENTDC =           0x00000080;
    static final public int CS_NOKEYCVT =           0x00000100; /* DDK / Win16 */
    static final public int CS_NOCLOSE =            0x00000200;
    static final public int CS_SAVEBITS =           0x00000800;
    static final public int CS_BYTEALIGNCLIENT =    0x00001000;
    static final public int CS_BYTEALIGNWINDOW =    0x00002000;
    static final public int CS_GLOBALCLASS =        0x00004000;
    static final public int CS_IME =                0x00010000;
    static final public int CS_DROPSHADOW =         0x00020000;

    static final public int COLOR_SCROLLBAR	=       0;
    static final public int COLOR_BACKGROUND =      1;
    static final public int COLOR_ACTIVECAPTION =   2;
    static final public int COLOR_INACTIVECAPTION = 3;
    static final public int COLOR_MENU =            4;
    static final public int COLOR_WINDOW =          5;
    static final public int COLOR_WINDOWFRAME =     6;
    static final public int COLOR_MENUTEXT =        7;
    static final public int COLOR_WINDOWTEXT =      8;
    static final public int COLOR_CAPTIONTEXT =     9;
    static final public int COLOR_ACTIVEBORDER =    10;
    static final public int COLOR_INACTIVEBORDER =  11;
    static final public int COLOR_APPWORKSPACE =    12;
    static final public int COLOR_HIGHLIGHT =       13;
    static final public int COLOR_HIGHLIGHTTEXT =   14;
    static final public int COLOR_BTNFACE =         15;
    static final public int COLOR_BTNSHADOW =       16;
    static final public int COLOR_GRAYTEXT =        17;
    static final public int COLOR_BTNTEXT =         18;
    static final public int COLOR_INACTIVECAPTIONTEXT = 19;
    static final public int COLOR_BTNHIGHLIGHT =    20;
    /* win95 colors */
    static final public int COLOR_3DDKSHADOW =      21;
    static final public int COLOR_3DLIGHT =         22;
    static final public int COLOR_INFOTEXT =        23;
    static final public int COLOR_INFOBK =          24;
    static final public int COLOR_DESKTOP =         COLOR_BACKGROUND;
    static final public int COLOR_3DFACE =          COLOR_BTNFACE;
    static final public int COLOR_3DSHADOW =        COLOR_BTNSHADOW;
    static final public int COLOR_3DHIGHLIGHT =     COLOR_BTNHIGHLIGHT;
    static final public int COLOR_3DHILIGHT =       COLOR_BTNHIGHLIGHT;
    static final public int COLOR_BTNHILIGHT =      COLOR_BTNHIGHLIGHT;
    /* win98 colors */
    static final public int COLOR_ALTERNATEBTNFACE = 25;  /* undocumented, constant name unknown */
    static final public int COLOR_HOTLIGHT =        26;
    static final public int COLOR_GRADIENTACTIVECAPTION = 27;
    static final public int COLOR_GRADIENTINACTIVECAPTION = 28;
    /* win2k/xp colors */
    static final public int COLOR_MENUHILIGHT =     29;
    static final public int COLOR_MENUBAR =         30;
    static final public int COLOR_MAX =             COLOR_MENUBAR;

    static final public int SRCCOPY =        0xcc0020;
    static final public int SRCPAINT =       0xee0086;
    static final public int SRCAND =         0x8800c6;
    static final public int SRCINVERT =      0x660046;
    static final public int SRCERASE =       0x440328;
    static final public int NOTSRCCOPY =     0x330008;
    static final public int NOTSRCERASE =    0x1100a6;
    static final public int MERGECOPY =      0xc000ca;
    static final public int MERGEPAINT =     0xbb0226;
    static final public int PATCOPY =        0xf00021;
    static final public int PATPAINT =       0xfb0a09;
    static final public int PATINVERT =      0x5a0049;
    static final public int DSTINVERT =      0x550009;
    static final public int BLACKNESS =      0x000042;
    static final public int WHITENESS =      0xff0062;

    /* WINDOWPLACEMENT flags */
    static final public int WPF_SETMINPOSITION =     0x0001;
    static final public int WPF_RESTORETOMAXIMIZED = 0x0002;
    
    static final public int WAIT_FAILED =    0xffffffff;

    static final public int IDC_ARROW =         32512;
    static final public int IDC_IBEAM =         32513;
    static final public int IDC_WAIT =          32514;
    static final public int IDC_CROSS =         32515;
    static final public int IDC_UPARROW =       32516;
    static final public int IDC_SIZE =          32640;
    static final public int IDC_ICON =          32641;
    static final public int IDC_SIZENWSE =      32642;
    static final public int IDC_SIZENESW =      32643;
    static final public int IDC_SIZEWE =        32644;
    static final public int IDC_SIZENS =        32645;
    static final public int IDC_SIZEALL =       32646;
    static final public int IDC_NO =            32648;
    static final public int IDC_HAND =          32649;
    static final public int IDC_APPSTARTING =   32650;
    static final public int IDC_HELP =          32651;
        
    // ************
    // * Internal *
    // ************

    static final public int WAIT_SWITCH = 0xFFFF;
    static public final int NUM_SYS_COLORS = COLOR_MENUBAR+1;

    static final public int PAINT_INTERNAL =      0x01;  /* internal WM_PAINT pending */
    static final public int PAINT_ERASE =         0x02;  /* needs WM_ERASEBKGND */
    static final public int PAINT_NONCLIENT =     0x04;  /* needs WM_NCPAINT */
    static final public int PAINT_DELAYED_ERASE = 0x08;  /* still needs erase after WM_ERASEBKGND */

    static final public int COORDS_CLIENT = 0; /* relative to client area */
    static final public int COORDS_WINDOW = 1; /* relative to whole window area */
    static final public int COORDS_PARENT = 2; /* relative to parent's client area */
    static final public int COORDS_SCREEN = 3; /* relative to screen origin */
    
    /* WND flags values */
    static final public int WIN_RESTORE_MAX =          0x0001; /* Maximize when restoring */
    static final public int WIN_NEED_SIZE =            0x0002; /* Internal WM_SIZE is needed */
    static final public int WIN_NCACTIVATED =          0x0004; /* last WM_NCACTIVATE was positive */
    static final public int WIN_ISMDICLIENT =          0x0008; /* Window is an MDIClient */
    static final public int WIN_ISUNICODE =            0x0010; /* Window is Unicode */
    static final public int WIN_NEEDS_SHOW_OWNEDPOPUP= 0x0020; /* WM_SHOWWINDOW:SC_SHOW must be sent in the next ShowOwnedPopup call */
    static final public int WIN_CHILDREN_MOVED =       0x0040; /* children may have moved, ignore stored positions */
}

