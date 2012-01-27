package jdos.win.system;

import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.WinBrush;
import jdos.win.builtin.gdi32.WinFont;
import jdos.win.builtin.gdi32.WinGDI;
import jdos.win.builtin.gdi32.WinPen;
import jdos.win.builtin.user32.WinClass;
import jdos.win.builtin.user32.WinWindow;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class StaticData extends WinAPI {
    static public int desktopWindow;
    public static int showCursorCount;
    public static int hCursor; // HCURSOR
    public static int mouseCapture; // HWND
    public static int foregroundWindow; // HWND
    public static int nextObjectId = 8200;
    public static Hashtable<Integer, WinObject> objects = new Hashtable<Integer, WinObject>();
    public static Hashtable<String, WinObject> namedObjects = new Hashtable<String, WinObject>();
    public static WinPoint currentPos = new WinPoint(0, 0);

    public static int[] SysColors = new int[NUM_SYS_COLORS];
    public static int[] SysColorBrushes = new int[NUM_SYS_COLORS];
    public static int[] SysColorPens = new int[NUM_SYS_COLORS];

    public static int[] stockObjects;
    public static WinUser user;
    public static JavaBitmap screen;
    public static List inputQueue = Collections.synchronizedList(new LinkedList());
    public static final Object inputQueueMutex = new Object();

    static public void init() {
        stockObjects = new int[STOCK_LAST+1];
        stockObjects[WHITE_BRUSH] = WinBrush.CreateSolidBrush(RGB(255, 255, 255));
        stockObjects[LTGRAY_BRUSH] = WinBrush.CreateSolidBrush(RGB(192, 192, 192));
        stockObjects[GRAY_BRUSH] = WinBrush.CreateSolidBrush(RGB(128, 128, 128));
        stockObjects[DKGRAY_BRUSH] = WinBrush.CreateSolidBrush(RGB(64, 64, 64));
        stockObjects[BLACK_BRUSH] = WinBrush.CreateSolidBrush(RGB(0, 0, 0));
        stockObjects[NULL_BRUSH] = WinBrush.create(BS_NULL, 0, 0).handle;

        stockObjects[WHITE_PEN] = WinPen.CreatePen(PS_SOLID, 0, RGB(255, 255, 255));
        stockObjects[BLACK_PEN] = WinPen.CreatePen(PS_SOLID, 0, RGB(0, 0, 0));
        stockObjects[NULL_PEN] = WinPen.CreatePen(PS_NULL, 0, 0);

        stockObjects[DEFAULT_PALETTE] = WinPen.CreatePen(PS_NULL, 0, 0);

        stockObjects[OEM_FIXED_FONT]      = WinFont.CreateFontA(12, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, OEM_CHARSET, 0, 0, DEFAULT_QUALITY, FIXED_PITCH | FF_MODERN, 0);
        stockObjects[ANSI_FIXED_FONT]     = WinFont.CreateFontA_String(12, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, 0, 0, DEFAULT_QUALITY, FIXED_PITCH | FF_MODERN, "Courier");
        stockObjects[ANSI_VAR_FONT]       = WinFont.CreateFontA_String(12, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, 0, 0, DEFAULT_QUALITY, VARIABLE_PITCH | FF_SWISS, "MS Sans Serif");

        /* language-dependent stock fonts */
        stockObjects[SYSTEM_FONT]         = WinFont.CreateFontA_String(16, 7, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, 0, 0, DEFAULT_QUALITY, VARIABLE_PITCH | FF_SWISS, "System");
        stockObjects[DEVICE_DEFAULT_FONT] = WinFont.CreateFontA(16, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, 0, 0, DEFAULT_QUALITY, VARIABLE_PITCH | FF_SWISS, 0);
        stockObjects[SYSTEM_FIXED_FONT]   = WinFont.CreateFontA(16, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, OEM_CHARSET, 0, 0, DEFAULT_QUALITY, FIXED_PITCH | FF_MODERN, 0);

        stockObjects[DEFAULT_GUI_FONT]    = WinFont.CreateFontA_String(8, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, 0, 0, DEFAULT_QUALITY, VARIABLE_PITCH | FF_SWISS, "MS Shell Dlg");

        stockObjects[DC_BRUSH]     = WinBrush.CreateSolidBrush(RGB(255, 255, 255));
        stockObjects[DC_PEN]       = WinPen.CreatePen(PS_SOLID, 0, RGB(0, 0, 0));
        for (int i=0;i<stockObjects.length;i++) {
            if (stockObjects[i] != 0) {
                WinGDI gdi = WinGDI.getGDI(stockObjects[i]);
                gdi.makePermanent();
            }
        }
        user = WinUser.create();
        WinClass winClass = WinClass.create();
        winClass.className = "Desktop";
        desktopWindow = new WinWindow(nextObjectId++, winClass, "Desktop").handle;
    }
}
