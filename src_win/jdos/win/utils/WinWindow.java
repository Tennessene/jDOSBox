package jdos.win.utils;

import jdos.cpu.CPU_Regs;
import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;

public class WinWindow extends WinObject {
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

    static public final int WM_TIMER =                       0x0113;


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

    public WinTimer timer;

    public boolean needsPainting = false;

    private int width;
    private int height;
    private int style;
    private int exStyle;
    private int id = 0;
    private int data = 0;
    private int hInstance;
    private WinDC dc;

    public WinWindow(int id, int dwExStyle, WinClass winClass, String name, int dwStyle, int x, int y, int cx, int cy, int hParent, int hMenu, int hInstance, int lpParam) {
        super(id);
        this.winClass = winClass;
        this.timer = new WinTimer(id);
        WinProcess process = WinSystem.getCurrentProcess();
        int createAddress = process.heap.alloc(48, false);
        int winName = StringUtil.allocateA(name);
        int className = StringUtil.allocateA(winClass.className);

        this.width = cx;
        this.height = cy;
        this.style = dwStyle;
        this.exStyle = dwExStyle;
        this.hInstance = hInstance;

        WinSystem.getCurrentThread().windows.add(this);

        Memory.mem_writed(createAddress, lpParam);
        Memory.mem_writed(createAddress+4, hInstance);
        Memory.mem_writed(createAddress+8, hMenu);
        Memory.mem_writed(createAddress+12, hParent);
        Memory.mem_writed(createAddress+16, cy);
        Memory.mem_writed(createAddress+20, cx);
        Memory.mem_writed(createAddress+24, y);
        Memory.mem_writed(createAddress+28, x);
        Memory.mem_writed(createAddress+32, dwStyle);
        Memory.mem_writed(createAddress+36, winName);
        Memory.mem_writed(createAddress+40, className);
        Memory.mem_writed(createAddress+44, dwExStyle);
        sendMessage(WM_CREATE, 0, createAddress);
        process.heap.free(createAddress);
        process.heap.free(winName);
        process.heap.free(className);

        if ((dwStyle & WS_VISIBLE) != 0) {
            showWindow(true);
        }
    }

    public int getDC() {
        return WinSystem.createDC(null, 0, width, height, null).getHandle();
    }

    public int releaseDC(WinDC dc) {
        dc.close();
        return 1;
    }

    public int isVisible() {
        return WinAPI.TRUE;
    }

    public int isIconic() {
        return WinAPI.FALSE;
    }

    public int getWindowLong(int index) {
        switch (index) {
            case -20: // GWL_EXSTYLE
                return exStyle;
            case -6: // GWL_HINSTANCE
                return hInstance;
            case -8: // GWL_HWNDPARENT
                return 0;
            case -12: // GWL_ID
                return id;
            case -16: // GWL_STYLE
                return style;
            case -21: // GWL_USERDATA
                return data;
            case -4: // GWL_WNDPROC
                return winClass.eip;
            default:
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return 0;
        }
    }
    public int getClientRect(int address) {
        WinRect.write(address, 0, 0, width, height);
        return WinAPI.TRUE;
    }

    public void showWindow(boolean show) {
        if (show) {
            postMessage(WM_SHOWWINDOW, WinAPI.TRUE, 0);
            postMessage(WM_ACTIVATE, 1 /* WA_ACTIVE */, 0);
            postMessage(WM_SETFOCUS, 0, 0);
            needsPainting = true;
        }
    }
    public void postMessage(int msg, int wParam, int lParam) {
        WinSystem.getCurrentThread().postMessage(handle, msg, wParam, lParam);
    }

    public int sendMessage(int msg, int wParam, int lParam) {
        WinSystem.call(winClass.eip, handle, msg, wParam, lParam);
        return CPU_Regs.reg_eax.dword;
    }

    public int defWindowProc(int msg, int wParam, int lParam) {
        if (msg == WM_TIMER) {
            timer.execute(wParam);
        }
        return 0;
    }

    private WinClass winClass;
}
