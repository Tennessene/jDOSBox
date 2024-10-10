package jdos.win.builtin;

import jdos.hardware.Memory;
import jdos.win.loader.BuiltinModule;
import jdos.win.system.Scheduler;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;

public class WinAPI extends Error {
    final static public boolean LOG = false;
    final static public boolean LOG_GDI = LOG;
    final static public boolean LOG_MM = false;
    final static public boolean MSGLOG = false;
    static public int NULL = 0;

    static public void log(String s) {
        if (BuiltinModule.inPre) {
            BuiltinModule.inPre = false;
        }
        if (BuiltinModule.indent>0)
            System.out.println();
        for (int i=0;i<BuiltinModule.indent;i++)
            System.out.print("    ");
        System.out.println(s);
    }

    static public void warn(String s) {
        log(HandlerBase.currentHandler.getName()+": "+s);
    }
    static public void faked() {
        log(HandlerBase.currentHandler.getName()+" faked");
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
        return b | (g << 8) | (r << 16);
    }
    static public boolean IS_INTRESOURCE(int p) {
        return (p & 0xFFFF0000) == 0;
    }
    static public int LOWORD(int w) {
        return w & 0xFFFF;
    }
    static public int HIWORD(int w) {
        return w >>> 16;
    }
    static public int LOBYTE(int b) {
        return b & 0xFF;
    }

    static final public int MAX_PATH = 260;
    static final public int HFILE_ERROR = -1;

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

    static public void writew(int address, int value) {
        Memory.mem_writew(address, value);
    }

    static public void writeb(int address, int value) {
        Memory.mem_writeb(address, value);
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

    static public final int WM_NULL =                        0x0000;
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
    static public final int WM_CANCELMODE =                  0x001F;
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

    static final public int WM_CONTEXTMENU =                 0x007b;
    static final public int WM_STYLECHANGING =               0x007c;
    static final public int WM_STYLECHANGED =                0x007d;
    static final public int WM_DISPLAYCHANGE =               0x007e;
    static final public int WM_GETICON =                     0x007f;
    static final public int WM_SETICON =                     0x0080;
    
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

    /* Win32 4.0 messages */
    static final public int WM_SIZING =                      0x0214;
    static final public int WM_CAPTURECHANGED =              0x0215;
    static final public int WM_MOVING =                      0x0216;
    static final public int WM_POWERBROADCAST =              0x0218;
    static final public int WM_DEVICECHANGE =                0x0219;
    
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
    
    static final public int SYSPAL_ERROR =       0;
    static final public int SYSPAL_STATIC =      1;
    static final public int SYSPAL_NOSTATIC =    2;
    static final public int SYSPAL_NOSTATIC256 = 3;
     
    /*** Dialog styles ***/
    static final public int DS_ABSALIGN =           0x00000001;
    static final public int DS_SYSMODAL =           0x00000002;
    static final public int DS_3DLOOK =             0x00000004; /* win95 */
    static final public int DS_FIXEDSYS =           0x00000008; /* win95 */
    static final public int DS_NOFAILCREATE =       0x00000010; /* win95 */
    static final public int DS_LOCALEDIT =          0x00000020;
    static final public int DS_SETFONT =            0x00000040;
    static final public int DS_MODALFRAME =         0x00000080;
    static final public int DS_NOIDLEMSG =          0x00000100;
    static final public int DS_SETFOREGROUND =      0x00000200; /* win95 */
    static final public int DS_CONTROL =            0x00000400; /* win95 */
    static final public int DS_CENTER =             0x00000800; /* win95 */
    static final public int DS_CENTERMOUSE =        0x00001000; /* win95 */
    static final public int DS_CONTEXTHELP =        0x00002000; /* win95 */
    static final public int DS_USEPIXELS =          0x00008000;
    static final public int DS_SHELLFONT =          (DS_SETFONT | DS_FIXEDSYS);

    /*** Static Control Styles ***/
    static final public int SS_LEFT =               0x00000000;
    static final public int SS_CENTER =             0x00000001;
    static final public int SS_RIGHT =              0x00000002;
    static final public int SS_ICON =               0x00000003;
    static final public int SS_BLACKRECT =          0x00000004;
    static final public int SS_GRAYRECT =           0x00000005;
    static final public int SS_WHITERECT =          0x00000006;
    static final public int SS_BLACKFRAME =         0x00000007;
    static final public int SS_GRAYFRAME =          0x00000008;
    static final public int SS_WHITEFRAME =         0x00000009;
    static final public int SS_USERITEM =           0x0000000A;
    static final public int SS_SIMPLE =             0x0000000B;
    static final public int SS_LEFTNOWORDWRAP =     0x0000000C;
    static final public int SS_OWNERDRAW =          0x0000000D;
    static final public int SS_BITMAP =             0x0000000E;
    static final public int SS_ENHMETAFILE =        0x0000000F;
    static final public int SS_ETCHEDHORZ =         0x00000010;
    static final public int SS_ETCHEDVERT =         0x00000011;
    static final public int SS_ETCHEDFRAME =        0x00000012;
    static final public int SS_TYPEMASK =           0x0000001F;
    
    static final public int SS_REALSIZECONTROL =    0x00000040;
    static final public int SS_NOPREFIX =           0x00000080;
    static final public int SS_NOTIFY =             0x00000100;
    static final public int SS_CENTERIMAGE =        0x00000200;
    static final public int SS_RIGHTJUST =          0x00000400;
    static final public int SS_REALSIZEIMAGE =      0x00000800;
    static final public int SS_SUNKEN =             0x00001000;
    static final public int SS_EDITCONTROL =        0x00002000;
    static final public int SS_ENDELLIPSIS =        0x00004000;
    static final public int SS_PATHELLIPSIS =       0x00008000;
    static final public int SS_WORDELLIPSIS =       0x0000C000;
    static final public int SS_ELLIPSISMASK =       SS_WORDELLIPSIS;

    
    public static final int DRIVERVERSION =   0;
    public static final int TECHNOLOGY =      2;
    public static final int HORZSIZE =        4;
    public static final int VERTSIZE =        6;
    public static final int HORZRES =         8;
    public static final int VERTRES =         10;
    public static final int BITSPIXEL =       12;
    public static final int PLANES =          14;
    public static final int NUMBRUSHES =      16;
    public static final int NUMPENS =         18;
    public static final int NUMMARKERS =      20;
    public static final int NUMFONTS =        22;
    public static final int NUMCOLORS =       24;
    public static final int PDEVICESIZE =     26;
    public static final int CURVECAPS =       28;
    public static final int LINECAPS =        30;
    public static final int POLYGONALCAPS =   32;
    public static final int TEXTCAPS =        34;
    public static final int CLIPCAPS =        36;
    public static final int RASTERCAPS =      38;
    public static final int ASPECTX =         40;
    public static final int ASPECTY =         42;
    public static final int ASPECTXY =        44;
    public static final int LOGPIXELSX =      88;
    public static final int LOGPIXELSY =      90;
    public static final int CAPS1 =           94;
    public static final int SIZEPALETTE =     104;
    public static final int NUMRESERVED =     106;
    public static final int COLORRES =        108;

    public static final int CW_USEDEFAULT =   0x80000000;

    static final public int CTLCOLOR_MSGBOX =            0;
    static final public int CTLCOLOR_EDIT =              1;
    static final public int CTLCOLOR_LISTBOX =           2;
    static final public int CTLCOLOR_BTN =               3;
    static final public int CTLCOLOR_DLG =               4;
    static final public int CTLCOLOR_SCROLLBAR =         5;
    static final public int CTLCOLOR_STATIC =            6;
    
    /* types of LoadImage */
    static final public int IMAGE_BITMAP =      0;
    static final public int IMAGE_ICON =        1;
    static final public int IMAGE_CURSOR =      2;
    static final public int IMAGE_ENHMETAFILE = 3;

    static final public int LR_DEFAULTCOLOR =   0x0000;
    static final public int LR_MONOCHROME =     0x0001;
    static final public int LR_COLOR =          0x0002;
    static final public int LR_COPYRETURNORG =  0x0004;
    static final public int LR_COPYDELETEORG =  0x0008;
    static final public int LR_LOADFROMFILE =   0x0010;
    static final public int LR_LOADTRANSPARENT= 0x0020;
    static final public int LR_DEFAULTSIZE =    0x0040;
    static final public int LR_VGA_COLOR =      0x0080;
    static final public int LR_LOADMAP3DCOLORS= 0x1000;
    static final public int LR_CREATEDIBSECTION=0x2000;
    static final public int LR_COPYFROMRESOURCE=0x4000;
    static final public int LR_SHARED =         0x8000;
    
    /* Static Control Messages */
    static final public int STM_SETICON =        0x0170;
    static final public int STM_GETICON =        0x0171;
    static final public int STM_SETIMAGE =       0x0172;
    static final public int STM_GETIMAGE =       0x0173;
    static final public int STM_MSGMAX =         0x0174;
    
    static final public int STN_CLICKED =        0;
    static final public int STN_DBLCLK =         1;
    static final public int STN_ENABLE =         2;
    static final public int STN_DISABLE =        3;
    
      /* RedrawWindow() flags */
    static final public int RDW_INVALIDATE =      0x0001;
    static final public int RDW_INTERNALPAINT =   0x0002;
    static final public int RDW_ERASE =           0x0004;
    static final public int RDW_VALIDATE =        0x0008;
    static final public int RDW_NOINTERNALPAINT = 0x0010;
    static final public int RDW_NOERASE =         0x0020;
    static final public int RDW_NOCHILDREN =      0x0040;
    static final public int RDW_ALLCHILDREN =     0x0080;
    static final public int RDW_UPDATENOW =       0x0100;
    static final public int RDW_ERASENOW =        0x0200;
    static final public int RDW_FRAME =           0x0400;
    static final public int RDW_NOFRAME =         0x0800;
    
    /* Bit flags for DRAWITEMSTRUCT.CtlType */
    static final public int ODT_MENU =       1;
    static final public int ODT_LISTBOX =    2;
    static final public int ODT_COMBOBOX =   3;
    static final public int ODT_BUTTON =     4;
    static final public int ODT_STATIC =     5;
    
    /* Bit flags for DRAWITEMSTRUCT.itemAction */
    static final public int ODA_DRAWENTIRE= 0x1;
    static final public int ODA_SELECT =    0x2;
    static final public int ODA_FOCUS =     0x4;
    
    /* Bit flags for DRAWITEMSTRUCT.itemState */
    static final public int ODS_SELECTED =    0x0001; /* Selected */
    static final public int ODS_GRAYED =      0x0002; /* Grayed (Menus only) */
    static final public int ODS_DISABLED =    0x0004; /* Disabled */
    static final public int ODS_CHECKED =     0x0008; /* Checked (Menus only) */
    static final public int ODS_FOCUS =       0x0010; /* Has focus */
    static final public int ODS_DEFAULT =     0x0020; /* Default */
    static final public int ODS_HOTLIGHT =    0x0040; /* Highlighted when under mouse */
    static final public int ODS_INACTIVE =    0x0080; /* Inactive */
    static final public int ODS_NOACCEL =     0x0100; /* No keyboard accelerator */
    static final public int ODS_NOFOCUSRECT = 0x0200; /* No focus rectangle */
    static final public int ODS_COMBOBOXEDIT= 0x1000; /* Edit of a combo box */
    
    static final public int DT_TOP =             0x00000000;
    static final public int DT_LEFT =            0x00000000;
    static final public int DT_CENTER =          0x00000001;
    static final public int DT_RIGHT =           0x00000002;
    static final public int DT_VCENTER =         0x00000004;
    static final public int DT_BOTTOM =          0x00000008;
    static final public int DT_WORDBREAK =       0x00000010;
    static final public int DT_SINGLELINE =      0x00000020;
    static final public int DT_EXPANDTABS =      0x00000040;
    static final public int DT_TABSTOP =         0x00000080;
    static final public int DT_NOCLIP =          0x00000100;
    static final public int DT_EXTERNALLEADING = 0x00000200;
    static final public int DT_CALCRECT =        0x00000400;
    static final public int DT_NOPREFIX =        0x00000800;
    static final public int DT_INTERNAL =        0x00001000;
    static final public int DT_EDITCONTROL =     0x00002000;
    static final public int DT_PATH_ELLIPSIS =   0x00004000;
    static final public int DT_END_ELLIPSIS =    0x00008000;
    static final public int DT_MODIFYSTRING =    0x00010000;
    static final public int DT_RTLREADING =      0x00020000;
    static final public int DT_WORD_ELLIPSIS =   0x00040000;
    
      /* ExtTextOut() parameters */
    static final public int ETO_GRAYED =         0x0001;
    static final public int ETO_OPAQUE =         0x0002;
    static final public int ETO_CLIPPED =        0x0004;
    static final public int ETO_GLYPH_INDEX =    0x0010;
    static final public int ETO_RTLREADING =     0x0080;
    static final public int ETO_NUMERICSLOCAL =  0x0400;
    static final public int ETO_NUMERICSLATIN =  0x0800;
    static final public int ETO_IGNORELANGUAGE = 0x1000;
    static final public int ETO_PDY =            0x2000;
    
    /* Flags for DrawIconEx.  */
    static final public int DI_MASK =                0x0001;
    static final public int DI_IMAGE =               0x0002;
    static final public int DI_NORMAL =              (DI_MASK | DI_IMAGE);
    static final public int DI_COMPAT =              0x0004;
    static final public int DI_DEFAULTSIZE =         0x0008;
    static final public int DI_NOMIRROR =            0x0010;
    
    /* DrawEdge() flags */
    static final public int BDR_RAISEDOUTER =   0x0001;
    static final public int BDR_SUNKENOUTER =   0x0002;
    static final public int BDR_RAISEDINNER =   0x0004;
    static final public int BDR_SUNKENINNER =   0x0008;
    
    static final public int BDR_OUTER =         0x0003;
    static final public int BDR_INNER =         0x000c;
    static final public int BDR_RAISED =        0x0005;
    static final public int BDR_SUNKEN =        0x000a;
    
    static final public int EDGE_RAISED =       (BDR_RAISEDOUTER | BDR_RAISEDINNER);
    static final public int EDGE_SUNKEN =       (BDR_SUNKENOUTER | BDR_SUNKENINNER);
    static final public int EDGE_ETCHED =       (BDR_SUNKENOUTER | BDR_RAISEDINNER);
    static final public int EDGE_BUMP =         (BDR_RAISEDOUTER | BDR_SUNKENINNER);
    
    /* border flags */
    static final public int BF_LEFT =           0x0001;
    static final public int BF_TOP =            0x0002;
    static final public int BF_RIGHT =          0x0004;
    static final public int BF_BOTTOM =         0x0008;
    static final public int BF_DIAGONAL =       0x0010;
    static final public int BF_MIDDLE =         0x0800;  /* Fill in the middle */
    static final public int BF_SOFT =           0x1000;  /* For softer buttons */
    static final public int BF_ADJUST =         0x2000;  /* Calculate the space left over */
    static final public int BF_FLAT =           0x4000;  /* For flat rather than 3D borders */
    static final public int BF_MONO =           0x8000;  /* For monochrome borders */
    static final public int BF_TOPLEFT =        (BF_TOP | BF_LEFT);
    static final public int BF_TOPRIGHT =       (BF_TOP | BF_RIGHT);
    static final public int BF_BOTTOMLEFT =     (BF_BOTTOM | BF_LEFT);
    static final public int BF_BOTTOMRIGHT =    (BF_BOTTOM | BF_RIGHT);
    static final public int BF_RECT =           (BF_LEFT | BF_TOP | BF_RIGHT | BF_BOTTOM);
    static final public int BF_DIAGONAL_ENDTOPRIGHT =    (BF_DIAGONAL | BF_TOP | BF_RIGHT);
    static final public int BF_DIAGONAL_ENDTOPLEFT =     (BF_DIAGONAL | BF_TOP | BF_LEFT);
    static final public int BF_DIAGONAL_ENDBOTTOMLEFT =  (BF_DIAGONAL | BF_BOTTOM | BF_LEFT);
    static final public int BF_DIAGONAL_ENDBOTTOMRIGHT = (BF_DIAGONAL | BF_BOTTOM | BF_RIGHT);

        
    /*** OEM Resource Ordinal Numbers ***/
    static final public int OBM_RDRVERT =           32559;
    static final public int OBM_RDRHORZ =           32660;
    static final public int OBM_RDR2DIM =           32661;
    static final public int OBM_TRTYPE =            32732; /* FIXME: Wine-only */
    static final public int OBM_LFARROWI =          32734;
    static final public int OBM_RGARROWI =          32735;
    static final public int OBM_DNARROWI =          32736;
    static final public int OBM_UPARROWI =          32737;
    static final public int OBM_COMBO =             32738;
    static final public int OBM_MNARROW =           32739;
    static final public int OBM_LFARROWD =          32740;
    static final public int OBM_RGARROWD =          32741;
    static final public int OBM_DNARROWD =          32742;
    static final public int OBM_UPARROWD =          32743;
    static final public int OBM_RESTORED =          32744;
    static final public int OBM_ZOOMD =             32745;
    static final public int OBM_REDUCED =           32746;
    static final public int OBM_RESTORE =           32747;
    static final public int OBM_ZOOM =              32748;
    static final public int OBM_REDUCE =            32749;
    static final public int OBM_LFARROW =           32750;
    static final public int OBM_RGARROW =           32751;
    static final public int OBM_DNARROW =           32752;
    static final public int OBM_UPARROW =           32753;
    static final public int OBM_CLOSE =             32754;
    static final public int OBM_OLD_RESTORE =       32755;
    static final public int OBM_OLD_ZOOM =          32756;
    static final public int OBM_OLD_REDUCE =        32757;
    static final public int OBM_BTNCORNERS =        32758;
    static final public int OBM_CHECKBOXES =        32759;
    static final public int OBM_CHECK =             32760;
    static final public int OBM_BTSIZE =            32761;
    static final public int OBM_OLD_LFARROW =       32762;
    static final public int OBM_OLD_RGARROW =       32763;
    static final public int OBM_OLD_DNARROW =       32764;
    static final public int OBM_OLD_UPARROW =       32765;
    static final public int OBM_SIZE =              32766;
    static final public int OBM_OLD_CLOSE =         32767;
    
    static final public int OCR_NORMAL =            32512;
    static final public int OCR_IBEAM =             32513;
    static final public int OCR_WAIT =              32514;
    static final public int OCR_CROSS =             32515;
    static final public int OCR_UP =                32516;
    static final public int OCR_SIZE =              32640;
    static final public int OCR_ICON =              32641;
    static final public int OCR_SIZENWSE =          32642;
    static final public int OCR_SIZENESW =          32643;
    static final public int OCR_SIZEWE =            32644;
    static final public int OCR_SIZENS =            32645;
    static final public int OCR_SIZEALL =           32646;
    static final public int OCR_ICOCUR =            32647;
    static final public int OCR_NO =                32648;
    static final public int OCR_HAND =              32649;
    static final public int OCR_APPSTARTING =       32650;
    static final public int OCR_HELP =              32651; /* DDK / Win16 */
    static final public int OCR_RDRVERT =           32652; /* DDK / Win16 */
    static final public int OCR_RDRHORZ =           32653; /* DDK / Win16 */
    static final public int OCR_DRAGOBJECT =        OCR_RDRHORZ; /* FIXME: Wine-only */
    static final public int OCR_RDR2DIM =           32654; /* DDK / Win16 */
    static final public int OCR_RDRNORTH =          32655; /* DDK / Win16 */
    static final public int OCR_RDRSOUTH =          32656; /* DDK / Win16 */
    static final public int OCR_RDRWEST =           32657; /* DDK / Win16 */
    static final public int OCR_RDREAST =           32658; /* DDK / Win16 */
    static final public int OCR_RDRNORTHWEST =      32659; /* DDK / Win16 */
    static final public int OCR_RDRNORTHEAST =      32660; /* DDK / Win16 */
    static final public int OCR_RDRSOUTHWEST =      32661; /* DDK / Win16 */
    static final public int OCR_RDRSOUTHEAST =      32662; /* DDK / Win16 */
    
    static final public int OIC_SAMPLE =            32512;
    static final public int OIC_HAND =              32513;
    static final public int OIC_ERROR =             OIC_HAND;
    static final public int OIC_QUES =              32514;
    static final public int OIC_BANG =              32515;
    static final public int OIC_WARNING =           OIC_BANG;
    static final public int OIC_NOTE =              32516;
    static final public int OIC_INFORMATION =       OIC_NOTE;
    static final public int OIC_WINLOGO =           32517;
    static final public int OIC_SHIELD =            32518;

    static final public int TRANSPARENT = 1;
    static final public int OPAQUE = 2;

    /*** Virtual key codes ***/
    static final public int VK_LBUTTON =            0x01;
    static final public int VK_RBUTTON =            0x02;
    static final public int VK_CANCEL =             0x03;
    static final public int VK_MBUTTON =            0x04;
    static final public int VK_XBUTTON1 =           0x05;
    static final public int VK_XBUTTON2 =           0x06;
    /*                             0x07  Undefined */
    static final public int VK_BACK =               0x08;
    static final public int VK_TAB =                0x09;
    /*                             0x0A-0x0B  Undefined */
    static final public int VK_CLEAR =              0x0C;
    static final public int VK_RETURN =             0x0D;
    /*                             0x0E-0x0F  Undefined */
    static final public int VK_SHIFT =              0x10;
    static final public int VK_CONTROL =            0x11;
    static final public int VK_MENU =               0x12;
    static final public int VK_PAUSE =              0x13;
    static final public int VK_CAPITAL =            0x14;

    static final public int VK_KANA =               0x15;
    static final public int VK_HANGEUL =            VK_KANA;
    static final public int VK_HANGUL =             VK_KANA;
    static final public int VK_JUNJA =              0x17;
    static final public int VK_FINAL =              0x18;
    static final public int VK_HANJA =              0x19;
    static final public int VK_KANJI =              VK_HANJA;

    /*                             0x1A       Undefined */
    static final public int VK_ESCAPE =             0x1B;

    static final public int VK_CONVERT =            0x1C;
    static final public int VK_NONCONVERT =         0x1D;
    static final public int VK_ACCEPT =             0x1E;
    static final public int VK_MODECHANGE =         0x1F;

    static final public int VK_SPACE =              0x20;
    static final public int VK_PRIOR =              0x21;
    static final public int VK_NEXT =               0x22;
    static final public int VK_END =                0x23;
    static final public int VK_HOME =               0x24;
    static final public int VK_LEFT =               0x25;
    static final public int VK_UP =                 0x26;
    static final public int VK_RIGHT =              0x27;
    static final public int VK_DOWN =               0x28;
    static final public int VK_SELECT =             0x29;
    static final public int VK_PRINT =              0x2A; /* OEM specific in Windows 3.1 SDK */
    static final public int VK_EXECUTE =            0x2B;
    static final public int VK_SNAPSHOT =           0x2C;
    static final public int VK_INSERT =             0x2D;
    static final public int VK_DELETE =             0x2E;
    static final public int VK_HELP =               0x2F;
    /* VK_0 - VK-9                 0x30-0x39  Use ASCII instead */
    /*                             0x3A-0x40  Undefined */
    /* VK_A - VK_Z                 0x41-0x5A  Use ASCII instead */
    static final public int VK_LWIN =               0x5B;
    static final public int VK_RWIN =               0x5C;
    static final public int VK_APPS =               0x5D;
    /*                             0x5E Unassigned */
    static final public int VK_SLEEP =              0x5F;
    static final public int VK_NUMPAD0 =            0x60;
    static final public int VK_NUMPAD1 =            0x61;
    static final public int VK_NUMPAD2 =            0x62;
    static final public int VK_NUMPAD3 =            0x63;
    static final public int VK_NUMPAD4 =            0x64;
    static final public int VK_NUMPAD5 =            0x65;
    static final public int VK_NUMPAD6 =            0x66;
    static final public int VK_NUMPAD7 =            0x67;
    static final public int VK_NUMPAD8 =            0x68;
    static final public int VK_NUMPAD9 =            0x69;
    static final public int VK_MULTIPLY =           0x6A;
    static final public int VK_ADD =                0x6B;
    static final public int VK_SEPARATOR =          0x6C;
    static final public int VK_SUBTRACT =           0x6D;
    static final public int VK_DECIMAL =            0x6E;
    static final public int VK_DIVIDE =             0x6F;
    static final public int VK_F1 =                 0x70;
    static final public int VK_F2 =                 0x71;
    static final public int VK_F3 =                 0x72;
    static final public int VK_F4 =                 0x73;
    static final public int VK_F5 =                 0x74;
    static final public int VK_F6 =                 0x75;
    static final public int VK_F7 =                 0x76;
    static final public int VK_F8 =                 0x77;
    static final public int VK_F9 =                 0x78;
    static final public int VK_F10 =                0x79;
    static final public int VK_F11 =                0x7A;
    static final public int VK_F12 =                0x7B;
    static final public int VK_F13 =                0x7C;
    static final public int VK_F14 =                0x7D;
    static final public int VK_F15 =                0x7E;
    static final public int VK_F16 =                0x7F;
    static final public int VK_F17 =                0x80;
    static final public int VK_F18 =                0x81;
    static final public int VK_F19 =                0x82;
    static final public int VK_F20 =                0x83;
    static final public int VK_F21 =                0x84;
    static final public int VK_F22 =                0x85;
    static final public int VK_F23 =                0x86;
    static final public int VK_F24 =                0x87;
    /*                             0x88-0x8F  Unassigned */
    static final public int VK_NUMLOCK =            0x90;
    static final public int VK_SCROLL =             0x91;
    static final public int VK_OEM_NEC_EQUAL =      0x92;
    static final public int VK_OEM_FJ_JISHO =       0x92;
    static final public int VK_OEM_FJ_MASSHOU =     0x93;
    static final public int VK_OEM_FJ_TOUROKU =     0x94;
    static final public int VK_OEM_FJ_LOYA =        0x95;
    static final public int VK_OEM_FJ_ROYA =        0x96;
    /*                             0x97-0x9F  Unassigned */
    /*
     * differencing between right and left shift/control/alt key.
     * Used only by GetAsyncKeyState() and GetKeyState().
     */
    static final public int VK_LSHIFT =             0xA0;
    static final public int VK_RSHIFT =             0xA1;
    static final public int VK_LCONTROL =           0xA2;
    static final public int VK_RCONTROL =           0xA3;
    static final public int VK_LMENU =              0xA4;
    static final public int VK_RMENU =              0xA5;

    static final public int VK_BROWSER_BACK =       0xA6;
    static final public int VK_BROWSER_FORWARD =    0xA7;
    static final public int VK_BROWSER_REFRESH =    0xA8;
    static final public int VK_BROWSER_STOP =       0xA9;
    static final public int VK_BROWSER_SEARCH =     0xAA;
    static final public int VK_BROWSER_FAVORITES =  0xAB;
    static final public int VK_BROWSER_HOME =       0xAC;
    static final public int VK_VOLUME_MUTE =        0xAD;
    static final public int VK_VOLUME_DOWN =        0xAE;
    static final public int VK_VOLUME_UP =          0xAF;
    static final public int VK_MEDIA_NEXT_TRACK =   0xB0;
    static final public int VK_MEDIA_PREV_TRACK =   0xB1;
    static final public int VK_MEDIA_STOP =         0xB2;
    static final public int VK_MEDIA_PLAY_PAUSE =   0xB3;
    static final public int VK_LAUNCH_MAIL =        0xB4;
    static final public int VK_LAUNCH_MEDIA_SELECT= 0xB5;
    static final public int VK_LAUNCH_APP1 =        0xB6;
    static final public int VK_LAUNCH_APP2 =        0xB7;

    /*                             0xB8-0xB9  Unassigned */
    static final public int VK_OEM_1 =              0xBA;
    static final public int VK_OEM_PLUS =           0xBB;
    static final public int VK_OEM_COMMA =          0xBC;
    static final public int VK_OEM_MINUS =          0xBD;
    static final public int VK_OEM_PERIOD =         0xBE;
    static final public int VK_OEM_2 =              0xBF;
    static final public int VK_OEM_3 =              0xC0;
    /*                             0xC1-0xDA  Unassigned */
    static final public int VK_OEM_4 =              0xDB;
    static final public int VK_OEM_5 =              0xDC;
    static final public int VK_OEM_6 =              0xDD;
    static final public int VK_OEM_7 =              0xDE;
    static final public int VK_OEM_8 =              0xDF;
    /*                             0xE0       OEM specific */
    static final public int VK_OEM_AX =             0xE1;  /* "AX" key on Japanese AX keyboard */
    static final public int VK_OEM_102 =            0xE2;  /* "<>" or "\|" on RT 102-key keyboard */
    static final public int VK_ICO_HELP =           0xE3;  /* Help key on ICO */
    static final public int VK_ICO_00 =             0xE4;  /* 00 key on ICO */
    static final public int VK_PROCESSKEY =         0xE5;
    static final public int VK_ICO_CLEAR =          0xE6;

    static final public int VK_PACKET =             0xE7;
    /*                             0xE8       Unassigned */

    static final public int VK_OEM_RESET =          0xE9;
    static final public int VK_OEM_JUMP =           0xEA;
    static final public int VK_OEM_PA1 =            0xEB;
    static final public int VK_OEM_PA2 =            0xEC;
    static final public int VK_OEM_PA3 =            0xED;
    static final public int VK_OEM_WSCTRL =         0xEE;
    static final public int VK_OEM_CUSEL =          0xEF;
    static final public int VK_OEM_ATTN =           0xF0;
    static final public int VK_OEM_FINISH =         0xF1;
    static final public int VK_OEM_COPY =           0xF2;
    static final public int VK_OEM_AUTO =           0xF3;
    static final public int VK_OEM_ENLW =           0xF4;
    static final public int VK_OEM_BACKTAB =        0xF5;
    static final public int VK_ATTN =               0xF6;
    static final public int VK_CRSEL =              0xF7;
    static final public int VK_EXSEL =              0xF8;
    static final public int VK_EREOF =              0xF9;
    static final public int VK_PLAY =               0xFA;
    static final public int VK_ZOOM =               0xFB;
    static final public int VK_NONAME =             0xFC;
    static final public int VK_PA1 =                0xFD;
    static final public int VK_OEM_CLEAR =          0xFE;
    
    static final public int MK_LBUTTON =            0x0001;
    static final public int MK_RBUTTON =            0x0002;
    static final public int MK_SHIFT =              0x0004;
    static final public int MK_CONTROL =            0x0008;
    static final public int MK_MBUTTON =            0x0010;
    static final public int MK_XBUTTON1 =           0x0020;
    static final public int MK_XBUTTON2 =           0x0040;
    
    /* Image type */
    static final public int DST_COMPLEX =   0x0000;
    static final public int DST_TEXT =      0x0001;
    static final public int DST_PREFIXTEXT =0x0002;
    static final public int DST_ICON =      0x0003;
    static final public int DST_BITMAP =    0x0004;

    /* State type */
    static final public int DSS_NORMAL =    0x0000;
    static final public int DSS_UNION =     0x0010;  /* Gray string appearance */
    static final public int DSS_DISABLED =  0x0020;
    static final public int DSS_DEFAULT =   0x0040;  /* Make it bold */
    static final public int DSS_MONO =      0x0080;
    static final public int DSS_HIDEPREFIX =0x0200;
    static final public int DSS_PREFIXONLY =0x0400;
    static final public int DSS_RIGHT =     0x8000;
    
    /* Object Definitions for EnumObjects() */
    static final public int OBJ_PEN =            1;
    static final public int OBJ_BRUSH =          2;
    static final public int OBJ_DC =             3;
    static final public int OBJ_METADC =         4;
    static final public int OBJ_PAL =            5;
    static final public int OBJ_FONT =           6;
    static final public int OBJ_BITMAP =         7;
    static final public int OBJ_REGION =         8;
    static final public int OBJ_METAFILE =       9;
    static final public int OBJ_MEMDC =          10;
    static final public int OBJ_EXTPEN =         11;
    static final public int OBJ_ENHMETADC =      12;
    static final public int OBJ_ENHMETAFILE =    13;
    static final public int OBJ_COLORSPACE =     14;
    
    /* DrawFrameControl() uType's */
    
    static final public int DFC_CAPTION =            1;
    static final public int DFC_MENU =               2;
    static final public int DFC_SCROLL =             3;
    static final public int DFC_BUTTON =             4;
    static final public int DFC_POPUPMENU =          5;
    
    /* uState's */
    
    static final public int DFCS_CAPTIONCLOSE =      0x0000;
    static final public int DFCS_CAPTIONMIN =        0x0001;
    static final public int DFCS_CAPTIONMAX =        0x0002;
    static final public int DFCS_CAPTIONRESTORE =    0x0003;
    static final public int DFCS_CAPTIONHELP =       0x0004;		/* Windows 95 only */
    
    static final public int DFCS_MENUARROW =         0x0000;
    static final public int DFCS_MENUCHECK =         0x0001;
    static final public int DFCS_MENUBULLET =        0x0002;
    static final public int DFCS_MENUARROWRIGHT =    0x0004;
    
    static final public int DFCS_SCROLLUP =           0x0000;
    static final public int DFCS_SCROLLDOWN =         0x0001;
    static final public int DFCS_SCROLLLEFT =         0x0002;
    static final public int DFCS_SCROLLRIGHT =        0x0003;
    static final public int DFCS_SCROLLCOMBOBOX =     0x0005;
    static final public int DFCS_SCROLLSIZEGRIP =     0x0008;
    static final public int DFCS_SCROLLSIZEGRIPRIGHT= 0x0010;
    
    static final public int DFCS_BUTTONCHECK =       0x0000;
    static final public int DFCS_BUTTONRADIOIMAGE =  0x0001;
    static final public int DFCS_BUTTONRADIOMASK =   0x0002;		/* to draw nonsquare button */
    static final public int DFCS_BUTTONRADIO =       0x0004;
    static final public int DFCS_BUTTON3STATE =      0x0008;
    static final public int DFCS_BUTTONPUSH =        0x0010;
    
    /* additional state of the control */

    static final public int DFCS_INACTIVE =          0x0100;
    static final public int DFCS_PUSHED =            0x0200;
    static final public int DFCS_CHECKED =           0x0400;
    static final public int DFCS_TRANSPARENT =       0x0800;
    static final public int DFCS_HOT =               0x1000;
    static final public int DFCS_ADJUSTRECT =        0x2000;		/* exclude surrounding edge */
    static final public int DFCS_FLAT =              0x4000;
    static final public int DFCS_MONO =              0x8000;
    
      /* Raster operations */

    static final public int R2_BLACK =        1;
    static final public int R2_NOTMERGEPEN =  2;
    static final public int R2_MASKNOTPEN =   3;
    static final public int R2_NOTCOPYPEN =   4;
    static final public int R2_MASKPENNOT =   5;
    static final public int R2_NOT =          6;
    static final public int R2_XORPEN =       7;
    static final public int R2_NOTMASKPEN =   8;
    static final public int R2_MASKPEN =      9;
    static final public int R2_NOTXORPEN =   10;
    static final public int R2_NOP =         11;
    static final public int R2_MERGENOTPEN = 12;
    static final public int R2_COPYPEN =     13;
    static final public int R2_MERGEPENNOT = 14;
    static final public int R2_MERGEPEN =    15;
    static final public int R2_WHITE =       16;
    static final public int R2_LAST =        16;

    static public final int MEM_COMMIT = 0x1000;
    static public final int MEM_RESERVE = 0x2000;
    static public final int MEM_DECOMMIT = 0x4000;
    static public final int MEM_RELEASE = 0x8000;
    static public final int MEM_RESET = 0x80000;

    static public final int MEM_LARGE_PAGES = 0x20000000;
    static public final int MEM_PHYSICAL = 0x400000;
    static public final int MEM_TOP_DOWN = 0x100000;
    static public final int MEM_WRITE_WATCH = 0x200000;

    static public final int PAGE_NOACCESS = 0x01;
    static public final int PAGE_READONLY = 0x02;
    static public final int PAGE_READWRITE = 0x04;
    static public final int PAGE_WRITECOPY = 0x08;

    static public final int PAGE_EXECUTE = 0x10;
    static public final int PAGE_EXECUTE_READ = 0x20;
    static public final int PAGE_EXECUTE_READWRITE = 0x40;
    static public final int PAGE_EXECUTE_WRITECOPY = 0x80;

    static public final int PAGE_GUARD = 0x100;
    static public final int PAGE_NOCACHE = 0x200;
    static public final int PAGE_WRITECOMBINE = 0x400;

    /* MapVirtualKey translation types */
    static final public int MAPVK_VK_TO_VSC =    0;
    static final public int MAPVK_VSC_TO_VK =    1;
    static final public int MAPVK_VK_TO_CHAR =   2;
    static final public int MAPVK_VSC_TO_VK_EX = 3;
    static final public int MAPVK_VK_TO_VSC_EX = 4;

    static final public int CALLBACK_TYPEMASK = 0x00070000;     /* callback type mask */
    static final public int CALLBACK_NULL =     0x00000000;     /* no callback */
    static final public int CALLBACK_WINDOW =   0x00010000;     /* dwCallback is a HWND */
    static final public int CALLBACK_TASK =     0x00020000;     /* dwCallback is a HTASK */
    static final public int CALLBACK_THREAD =   CALLBACK_TASK;  /* dwCallback is a thread ID */
    static final public int CALLBACK_FUNCTION = 0x00030000;     /* dwCallback is a FARPROC */
    static final public int CALLBACK_EVENT =    0x00050000;     /* dwCallback is an EVENT Handler */

    static final public int MMSYSERR_BASE =         0;
    static final public int WAVERR_BASE =           32;
    static final public int MIDIERR_BASE =          64;
    static final public int TIMERR_BASE =           96;
    static final public int JOYERR_BASE =           160;
    static final public int MCIERR_BASE =           256;

    static final public int MCI_STRING_OFFSET =     512;
    static final public int MCI_VD_OFFSET =         1024;
    static final public int MCI_CD_OFFSET =         1088;
    static final public int MCI_WAVE_OFFSET =       1152;
    static final public int MCI_SEQ_OFFSET =        1216;

    static final public int MMSYSERR_NOERROR =     0;
    static final public int MMSYSERR_ERROR =        MMSYSERR_BASE + 1;
    static final public int MMSYSERR_BADDEVICEID =  MMSYSERR_BASE + 2;
    static final public int MMSYSERR_NOTENABLED =   MMSYSERR_BASE + 3;
    static final public int MMSYSERR_ALLOCATED =    MMSYSERR_BASE + 4;
    static final public int MMSYSERR_INVALHANDLE =  MMSYSERR_BASE + 5;
    static final public int MMSYSERR_NODRIVER =     MMSYSERR_BASE + 6;
    static final public int MMSYSERR_NOMEM =        MMSYSERR_BASE + 7;
    static final public int MMSYSERR_NOTSUPPORTED = MMSYSERR_BASE + 8;
    static final public int MMSYSERR_BADERRNUM =    MMSYSERR_BASE + 9;
    static final public int MMSYSERR_INVALFLAG =    MMSYSERR_BASE + 10;
    static final public int MMSYSERR_INVALPARAM =   MMSYSERR_BASE + 11;
    static final public int MMSYSERR_HANDLEBUSY =   MMSYSERR_BASE + 12;
    static final public int MMSYSERR_INVALIDALIAS = MMSYSERR_BASE + 13;
    static final public int MMSYSERR_BADDB =        MMSYSERR_BASE + 14;
    static final public int MMSYSERR_KEYNOTFOUND =  MMSYSERR_BASE + 15;
    static final public int MMSYSERR_READERROR =    MMSYSERR_BASE + 16;
    static final public int MMSYSERR_WRITEERROR =   MMSYSERR_BASE + 17;
    static final public int MMSYSERR_DELETEERROR =  MMSYSERR_BASE + 18;
    static final public int MMSYSERR_VALNOTFOUND =  MMSYSERR_BASE + 19;
    static final public int MMSYSERR_NODRIVERCB =   MMSYSERR_BASE + 20;
    static final public int MMSYSERR_MOREDATA =     MMSYSERR_BASE + 21;
    static final public int MMSYSERR_LASTERROR =    MMSYSERR_BASE + 21;

    static final public int  WAVE_FORMAT_QUERY =        0x0001;
    static final public int  WAVE_ALLOWSYNC =           0x0002;
    static final public int  WAVE_MAPPED =              0x0004;
    static final public int  WAVE_FORMAT_DIRECT =       0x0008;
    static final public int  WAVE_FORMAT_DIRECT_QUERY = (WAVE_FORMAT_QUERY | WAVE_FORMAT_DIRECT);

    static final public int WODM_OPEN = 5;
    static final public int WAVE_MAPPER = -1;

    static final public int WAVERR_BADFORMAT =     WAVERR_BASE + 0;    /* unsupported wave format */
    static final public int WAVERR_STILLPLAYING =  WAVERR_BASE + 1;    /* still something playing */
    static final public int WAVERR_UNPREPARED =    WAVERR_BASE + 2;    /* header not prepared */
    static final public int WAVERR_SYNC =          WAVERR_BASE + 3;    /* device is synchronous */
    static final public int WAVERR_LASTERROR =     WAVERR_BASE + 3;    /* last error in range */

    static final public int DIB_RGB_COLORS = 0;
    static final public int DIB_PAL_COLORS = 1;

    static final public int ACM_METRIC_COUNT_DRIVERS =           1;
    static final public int ACM_METRIC_COUNT_CODECS =            2;
    static final public int ACM_METRIC_COUNT_CONVERTERS =        3;
    static final public int ACM_METRIC_COUNT_FILTERS =           4;
    static final public int ACM_METRIC_COUNT_DISABLED =          5;
    static final public int ACM_METRIC_COUNT_HARDWARE =          6;
    static final public int ACM_METRIC_COUNT_LOCAL_DRIVERS =    20;
    static final public int ACM_METRIC_COUNT_LOCAL_CODECS =     21;
    static final public int ACM_METRIC_COUNT_LOCAL_CONVERTERS = 22;
    static final public int ACM_METRIC_COUNT_LOCAL_FILTERS =    23;
    static final public int ACM_METRIC_COUNT_LOCAL_DISABLED =   24;
    static final public int ACM_METRIC_HARDWARE_WAVE_INPUT =    30;
    static final public int ACM_METRIC_HARDWARE_WAVE_OUTPUT =   31;
    static final public int ACM_METRIC_MAX_SIZE_FORMAT =        50;
    static final public int ACM_METRIC_MAX_SIZE_FILTER =        51;
    static final public int ACM_METRIC_DRIVER_SUPPORT =        100;
    static final public int ACM_METRIC_DRIVER_PRIORITY =       101;
    
    static final public int MMIOERR_BASE =             256;
    static final public int MMIOERR_FILENOTFOUND =     MMIOERR_BASE + 1;  /* file not found */
    static final public int MMIOERR_OUTOFMEMORY =      MMIOERR_BASE + 2;  /* out of memory */
    static final public int MMIOERR_CANNOTOPEN =       MMIOERR_BASE + 3;  /* cannot open */
    static final public int MMIOERR_CANNOTCLOSE =      MMIOERR_BASE + 4;  /* cannot close */
    static final public int MMIOERR_CANNOTREAD =       MMIOERR_BASE + 5;  /* cannot read */
    static final public int MMIOERR_CANNOTWRITE =      MMIOERR_BASE + 6;  /* cannot write */
    static final public int MMIOERR_CANNOTSEEK =       MMIOERR_BASE + 7;  /* cannot seek */
    static final public int MMIOERR_CANNOTEXPAND =     MMIOERR_BASE + 8;  /* cannot expand file */
    static final public int MMIOERR_CHUNKNOTFOUND =    MMIOERR_BASE + 9;  /* chunk not found */
    static final public int MMIOERR_UNBUFFERED =       MMIOERR_BASE + 10; /* file is unbuffered */
    static final public int MMIOERR_PATHNOTFOUND =     MMIOERR_BASE + 11;
    static final public int MMIOERR_ACCESSDENIED =     MMIOERR_BASE + 12;
    static final public int MMIOERR_SHARINGVIOLATION = MMIOERR_BASE + 13;
    static final public int MMIOERR_NETWORKERROR =     MMIOERR_BASE + 14;
    static final public int MMIOERR_TOOMANYOPENFILES = MMIOERR_BASE + 15;
    static final public int MMIOERR_INVALIDFILE =      MMIOERR_BASE + 16;
        
    static final public int MMIO_RWMODE =    0x00000003;      /* open file for reading/writing/both */
    static final public int MMIO_SHAREMODE = 0x00000070;      /* file sharing mode number */
    
    static final public int MMIO_CREATE =    0x00001000;      /* create new file (or truncate file) */
    static final public int MMIO_PARSE =     0x00000100;      /* parse new file returning path */
    static final public int MMIO_DELETE =    0x00000200;      /* create new file (or truncate file) */
    static final public int MMIO_EXIST =     0x00004000;      /* checks for existence of file */
    static final public int MMIO_ALLOCBUF =  0x00010000;      /* mmioOpen() should allocate a buffer */
    static final public int MMIO_GETTEMP =   0x00020000;      /* mmioOpen() should retrieve temp name */
    
    static final public int MMIO_DIRTY =     0x10000000;      /* I/O buffer is dirty */
    
    static final public int MMIO_READ =      0x00000000;      /* open file for reading only */
    static final public int MMIO_WRITE =     0x00000001;      /* open file for writing only */
    static final public int MMIO_READWRITE = 0x00000002;      /* open file for reading and writing */
    
    static final public int MMIO_COMPAT =    0x00000000;      /* compatibility mode */
    static final public int MMIO_EXCLUSIVE = 0x00000010;      /* exclusive-access mode */
    static final public int MMIO_DENYWRITE = 0x00000020;      /* deny writing to other processes */
    static final public int MMIO_DENYREAD =  0x00000030;      /* deny reading to other processes */
    static final public int MMIO_DENYNONE =  0x00000040;      /* deny nothing to other processes */
    
    static final public int MMIO_FHOPEN =            0x0010;  /* mmioClose: keep file handle open */
    static final public int MMIO_EMPTYBUF =          0x0010;  /* mmioFlush: empty the I/O buffer */
    static final public int MMIO_TOUPPER =           0x0010;  /* mmioStringToFOURCC: to u-case */
    static final public int MMIO_INSTALLPROC =   0x00010000;  /* mmioInstallIOProc: install MMIOProc */
    static final public int MMIO_GLOBALPROC =    0x10000000;  /* mmioInstallIOProc: install globally */
    static final public int MMIO_REMOVEPROC =    0x00020000;  /* mmioInstallIOProc: remove MMIOProc */
    static final public int MMIO_FINDPROC =      0x00040000;  /* mmioInstallIOProc: find an MMIOProc */
    static final public int MMIO_FINDCHUNK =         0x0010;  /* mmioDescend: find a chunk by ID */
    static final public int MMIO_FINDRIFF =          0x0020;  /* mmioDescend: find a LIST chunk */
    static final public int MMIO_FINDLIST =          0x0040;  /* mmioDescend: find a RIFF chunk */
    static final public int MMIO_CREATERIFF =        0x0020;  /* mmioCreateChunk: make a LIST chunk */
    static final public int MMIO_CREATELIST =        0x0040;  /* mmioCreateChunk: make a RIFF chunk */
    
    static final public int mmioFOURCC(int ch0, int ch1, int ch2, int ch3) {
        return (ch0 & 0xFF) | ((ch1 & 0xFF) << 8) | ((ch2 & 0xFF) << 16) | ((ch3 & 0xFF) << 24);
    }
    static final public int FOURCC_RIFF = mmioFOURCC('R', 'I', 'F', 'F');
    static final public int FOURCC_LIST = mmioFOURCC('L', 'I', 'S', 'T');

    static final public int FOURCC_DOS = mmioFOURCC('D', 'O', 'S', ' ');
    static final public int FOURCC_MEM = mmioFOURCC('M', 'E', 'M', ' ');

    static final public int MMIO_DEFAULTBUFFER = 8192;    /* default buffer size */

    static final public int SEEK_SET = 0;
    static final public int SEEK_CUR = 1;
    static final public int SEEK_END = 2;
    
    static final public int SND_SYNC =           	0x0000;  /* play synchronously (default) */
    static final public int SND_ASYNC =          	0x0001;  /* play asynchronously */
    static final public int SND_NODEFAULT =      	0x0002;  /* don't use default sound */
    static final public int SND_MEMORY =         	0x0004;  /* lpszSoundName points to a memory file */
    static final public int SND_LOOP =           	0x0008;  /* loop the sound until next sndPlaySound */
    static final public int SND_NOSTOP =         	0x0010;  /* don't stop any currently playing sound */
    
    static final public int SND_NOWAIT =             0x00002000; /* don't wait if the driver is busy */
    static final public int SND_ALIAS =              0x00010000; /* name is a registry alias */
    static final public int SND_ALIAS_ID =           0x00110000; /* alias is a predefined ID */
    static final public int SND_FILENAME =           0x00020000; /* name is file name */
    static final public int SND_RESOURCE =           0x00040004; /* name is resource name or atom */
    static final public int SND_PURGE =              0x00000040; /* purge all sounds */
    static final public int SND_APPLICATION =        0x00000080; /* look for application specific association */

    static final public int SND_ALIAS_START =        0;

    static public int sndAlias(int ch0, int ch1) {
        return (SND_ALIAS_START + (ch0 & 0xFF) | ((ch1 & 0xFF) << 8));
    }
    
    static final public int SND_ALIAS_SYSTEMASTERISK =       sndAlias('S', '*');
    static final public int SND_ALIAS_SYSTEMQUESTION =       sndAlias('S', '?');
    static final public int SND_ALIAS_SYSTEMHAND =           sndAlias('S', 'H');
    static final public int SND_ALIAS_SYSTEMEXIT =           sndAlias('S', 'E');
    static final public int SND_ALIAS_SYSTEMSTART =          sndAlias('S', 'S');
    static final public int SND_ALIAS_SYSTEMWELCOME =        sndAlias('S', 'W');
    static final public int SND_ALIAS_SYSTEMEXCLAMATION =    sndAlias('S', '!');
    static final public int SND_ALIAS_SYSTEMDEFAULT =        sndAlias('S', 'D');
    
    /* STARTUPINFO.dwFlags */
    static final public int STARTF_USESHOWWINDOW =      0x00000001;
    static final public int STARTF_USESIZE =            0x00000002;
    static final public int STARTF_USEPOSITION =        0x00000004;
    static final public int STARTF_USECOUNTCHARS =      0x00000008;
    static final public int STARTF_USEFILLATTRIBUTE =   0x00000010;
    static final public int STARTF_RUNFULLSCREEN =      0x00000020;
    static final public int STARTF_FORCEONFEEDBACK =    0x00000040;
    static final public int STARTF_FORCEOFFFEEDBACK =   0x00000080;
    static final public int STARTF_USESTDHANDLES =      0x00000100;
    static final public int STARTF_USEHOTKEY =          0x00000200;

    static final public int OF_READ =              0x0000;
    static final public int OF_WRITE =             0x0001;
    static final public int OF_READWRITE =         0x0002;
    static final public int OF_SHARE_COMPAT =      0x0000;
    static final public int OF_SHARE_EXCLUSIVE =   0x0010;
    static final public int OF_SHARE_DENY_WRITE =  0x0020;
    static final public int OF_SHARE_DENY_READ =   0x0030;
    static final public int OF_SHARE_DENY_NONE =   0x0040;
    static final public int OF_PARSE =             0x0100;
    static final public int OF_DELETE =            0x0200;
    static final public int OF_VERIFY =            0x0400;   /* Used with OF_REOPEN */
    static final public int OF_SEARCH =            0x0400;   /* Used without OF_REOPEN */
    static final public int OF_CANCEL =            0x0800;
    static final public int OF_CREATE =            0x1000;
    static final public int OF_PROMPT =            0x2000;
    static final public int OF_EXIST =             0x4000;
    static final public int OF_REOPEN =            0x8000;
    
    /* WAVE form wFormatTag IDs */
    static final public int WAVE_FORMAT_UNKNOWN =           0x0000;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_ADPCM =             0x0002;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_IEEE_FLOAT =        0x0003;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_IBM_CVSD =          0x0005;	/*  IBM Corporation  */
    static final public int WAVE_FORMAT_ALAW =              0x0006;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_MULAW =             0x0007;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_OKI_ADPCM =         0x0010;	/*  OKI  */
    static final public int WAVE_FORMAT_DVI_ADPCM =         0x0011;	/*  Intel Corporation  */
    static final public int WAVE_FORMAT_IMA_ADPCM =         (WAVE_FORMAT_DVI_ADPCM);	/*  Intel Corporation  */
    static final public int WAVE_FORMAT_MEDIASPACE_ADPCM =  0x0012;	/*  Videologic  */
    static final public int WAVE_FORMAT_SIERRA_ADPCM =      0x0013;	/*  Sierra Semiconductor Corp  */
    static final public int WAVE_FORMAT_G723_ADPCM =        0x0014;	/*  Antex Electronics Corporation  */
    static final public int WAVE_FORMAT_DIGISTD =           0x0015;	/*  DSP Solutions, Inc.  */
    static final public int WAVE_FORMAT_DIGIFIX =           0x0016;	/*  DSP Solutions, Inc.  */
    static final public int WAVE_FORMAT_DIALOGIC_OKI_ADPCM= 0x0017;	/*  Dialogic Corporation  */
    static final public int WAVE_FORMAT_YAMAHA_ADPCM =      0x0020;	/*  Yamaha Corporation of America  */
    static final public int WAVE_FORMAT_SONARC =            0x0021;	/*  Speech Compression  */
    static final public int WAVE_FORMAT_DSPGROUP_TRUESPEECH=0x0022;	/*  DSP Group, Inc  */
    static final public int WAVE_FORMAT_ECHOSC1 =           0x0023;	/*  Echo Speech Corporation  */
    static final public int WAVE_FORMAT_AUDIOFILE_AF36 =    0x0024;	/*    */
    static final public int WAVE_FORMAT_APTX =              0x0025;	/*  Audio Processing Technology  */
    static final public int WAVE_FORMAT_AUDIOFILE_AF10 =    0x0026;	/*    */
    static final public int WAVE_FORMAT_DOLBY_AC2 =         0x0030;	/*  Dolby Laboratories  */
    static final public int WAVE_FORMAT_GSM610 =            0x0031;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_ANTEX_ADPCME =      0x0033;	/*  Antex Electronics Corporation  */
    static final public int WAVE_FORMAT_CONTROL_RES_VQLPC = 0x0034;	/*  Control Resources Limited  */
    static final public int WAVE_FORMAT_DIGIREAL =          0x0035;	/*  DSP Solutions, Inc.  */
    static final public int WAVE_FORMAT_DIGIADPCM =         0x0036;	/*  DSP Solutions, Inc.  */
    static final public int WAVE_FORMAT_CONTROL_RES_CR10 =  0x0037;	/*  Control Resources Limited  */
    static final public int WAVE_FORMAT_NMS_VBXADPCM =      0x0038;	/*  Natural MicroSystems  */
    static final public int WAVE_FORMAT_G721_ADPCM =        0x0040;	/*  Antex Electronics Corporation  */
    static final public int WAVE_FORMAT_MPEG =              0x0050;	/*  Microsoft Corporation  */
    static final public int WAVE_FORMAT_MPEGLAYER3 =        0x0055;
    static final public int WAVE_FORMAT_CREATIVE_ADPCM =    0x0200;	/*  Creative Labs, Inc  */
    static final public int WAVE_FORMAT_CREATIVE_FASTSPEECH8=0x0202;	/*  Creative Labs, Inc  */
    static final public int WAVE_FORMAT_CREATIVE_FASTSPEECH10=0x0203;	/*  Creative Labs, Inc  */
    static final public int WAVE_FORMAT_FM_TOWNS_SND =      0x0300;	/*  Fujitsu Corp.  */
    static final public int WAVE_FORMAT_OLIGSM =            0x1000;	/*  Ing C. Olivetti & C., S.p.A.  */
    static final public int WAVE_FORMAT_OLIADPCM =          0x1001;	/*  Ing C. Olivetti & C., S.p.A.  */
    static final public int WAVE_FORMAT_OLICELP =           0x1002;	/*  Ing C. Olivetti & C., S.p.A.  */
    static final public int WAVE_FORMAT_OLISBC =            0x1003;	/*  Ing C. Olivetti & C., S.p.A.  */
    static final public int WAVE_FORMAT_OLIOPR =            0x1004;	/*  Ing C. Olivetti & C., S.p.A.  */
    static final public int WAVE_FORMAT_EXTENSIBLE =        0xFFFE;  /* Microsoft */

    static final public int  DSBSIZE_MIN =                4;
    static final public int  DSBSIZE_MAX =                0xFFFFFFF;
    static final public int  DSBPAN_LEFT =                -10000;
    static final public int  DSBPAN_CENTER =              0;
    static final public int  DSBPAN_RIGHT =               10000;
    static final public int  DSBVOLUME_MAX =              0;
    static final public int  DSBVOLUME_MIN =              -10000;
    static final public int  DSBFREQUENCY_MIN =           100;
    static final public int  DSBFREQUENCY_MAX =           200000;
    static final public int  DSBFREQUENCY_ORIGINAL =      0;
    
    // ************
    // * Internal *
    // ************

    public static int DF_END =          0x0001;
    public static int DF_OWNERENABLED = 0x0002;

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

    static final public int DSOUND_FREQSHIFT = 20;
}

