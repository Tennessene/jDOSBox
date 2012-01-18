package jdos.win.system;

import jdos.cpu.CPU_Regs;
import jdos.dos.Dos_programs;
import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.ddraw.IDirectDrawSurface;
import jdos.win.utils.StringUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

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
    static public final int WM_SETCURSOR =                   0x0020;

    static public final int WM_ACTIVATEAPP =                 0x001C;

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

    static public final int WM_TIMER =                       0x0113;

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

    static public final int MM_MCINOTIFY =                   0x03B9;

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

    static public final int HTERROR =           -2;
    static public final int HTTRANSPARENT =     -1;
    static public final int HTNOWHERE =         0;
    static public final int HTCLIENT =          1;
    static public final int HTCAPTION =         2;

    public WinTimer timer;
    public static int currentFocus;

    private WinRect rect;
    private WinRect clientRect;
    private int style;
    private int exStyle;
    private int id = 0;
    private int data = 0;
    private int hInstance;
    private int hMenu;
    private int hParent; // external parent
    private WinWindow parent; // will include desktop as top most parent
    private WinThread thread;

    private WinDC dc;
    private WinClass winClass;

    public boolean isActive = false;
    public boolean needsPainting = false;

    private Vector children = new Vector(); // first one is on bottom

    // Used by desktop
    public WinWindow(int id, WinClass winClass, String name) {
        super(id);
        this.winClass = winClass;
        this.name = name;
        this.rect = new WinRect(0, 0, WinSystem.getScreenWidth(), WinSystem.getScreenHeight());
        this.clientRect = new WinRect(0, 0, rect.width(), rect.height());
    }

    public WinWindow(int id, int dwExStyle, WinClass winClass, String name, int dwStyle, int x, int y, int cx, int cy, int hParent, int hMenu, int hInstance, int lpParam) {
        super(id);
        this.winClass = winClass;
        this.timer = new WinTimer(id);
        WinProcess process = WinSystem.getCurrentProcess();
        int createAddress = 0;process.heap.alloc(48, false);
        int winName = StringUtil.allocateA(name);
        int className = StringUtil.allocateA(winClass.className);

        if (hParent == 0) {
            x = 0;
            y = 0;
            cx = WinSystem.getScreenWidth();
            cy = WinSystem.getScreenHeight();
        }
        this.rect = new WinRect(x, y, x+cx, y+cy);
        this.clientRect = new WinRect(0, 0, rect.width(), rect.height());
        this.style = dwStyle;
        this.exStyle = dwExStyle;
        this.hInstance = hInstance;
        this.thread = WinSystem.getCurrentThread();
        this.hParent = hParent;
        this.name = name;
        if (hParent != 0)
            parent = (WinWindow)WinSystem.getObject(hParent);
        else {
            parent = WinSystem.desktop;
            if (rect.width()>WinSystem.getScreenWidth() || rect.height()>WinSystem.getScreenHeight()) {
                growDesktop();
            }
        }

        if (parent != null)
            parent.addChild(this);

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

    public int GetWindow(int uCmd) {
        switch (uCmd) {
            case 0: // GW_HWNDFIRST
                if ((style & WS_CHILD) != 0) {
                    Win.panic("GetWindow child relationships not implemented yet: "+uCmd);
                }
                return 0;
            case 1: // GW_HWNDLAST
                if ((style & WS_CHILD) != 0) {
                    Win.panic("GetWindow child relationships not implemented yet: "+uCmd);
                }
                return 0;
            case 2: // GW_HWNDNEXT
                if ((style & WS_CHILD) != 0) {
                    Win.panic("GetWindow child relationships not implemented yet: "+uCmd);
                }
                return 0;
            case 3: // GW_HWNDPREV
                if ((style & WS_CHILD) != 0) {
                    Win.panic("GetWindow child relationships not implemented yet: "+uCmd);
                }
                return 0;
            case 5: // GW_CHILD
                return (children.size()>0?((WinWindow)children.elementAt(children.size()-1)).handle:0);
            case 6: // GW_ENABLEDPOPUP
                return handle; // popups are not supported yet
            default:
                Win.panic("GetWindow "+uCmd+" not implemented yet");
                return 0;
        }
    }

    public WinWindow findWindowFromPoint(int x, int y) {
        for (int i=children.size()-1;i>=0;i--) {
            WinWindow child = (WinWindow)children.elementAt(i);
            if (child.isVisible() &&  child.rect.contains(x, y)) {
                return child.findWindowFromPoint(x-child.rect.left, y-child.rect.top);
            }
        }
        return this;
    }

    public WinThread getThread() {
        return thread;
    }

    public WinClass getWinClass() {
        return winClass;
    }

    private void addChild(WinWindow window) {
        children.add(window);
    }

    public int setFocus() {
        return handle;
    }

    public int findWindow(String className, String windowName) {
        if (this.winClass.className.equals(className) || this.name.equals(windowName))
            return getHandle();
        for (int i=0;i<children.size();i++) {
            int result = ((WinWindow)children.elementAt(i)).findWindow(className, windowName);
            if (result != 0)
                return result;
        }
        return 0;
    }

    public int getMenu() {
        return hMenu;
    }

    public int clientToScreen(int lpPoint) {
        return WinAPI.TRUE; // currently only one full screen window is supported so no conversion is necessary
    }

    public int beginPaint(int lpPaint) {
        int dc = getDC();
        if (lpPaint != 0) {  // :TODO: does windows allow this?
            int erase = WinAPI.TRUE;
            if (winClass.hbrBackground != 0) {
                WinObject object = WinSystem.getObject(winClass.hbrBackground);
                if (object != null && object instanceof WinBrush) {
                    WinDC winDC = (WinDC)WinSystem.getObject(dc);
                    Graphics g = winDC.getImage().getGraphics();
                    g.setColor(new Color(((WinBrush)object).color));
                    g.fillRect(0, 0, rect.width(), rect.height());
                    erase = WinAPI.FALSE;
                    g.dispose();
                }
            }
            Memory.mem_writed(lpPaint, dc);lpPaint+=4; // hdc
            Memory.mem_writed(lpPaint, erase);lpPaint+=4; // fErase
            WinRect.write(lpPaint, 0, 0, rect.width(), rect.height());lpPaint+=16; // rcPaint
            Memory.mem_writed(lpPaint, 0);lpPaint+=4; // fRestore - reserved
            Memory.mem_writed(lpPaint, 0);lpPaint+=4; // fIncUpdate - reserved
            Memory.mem_zero(lpPaint, 32); // rgbReserved - reserved
        }
        return dc;
    }

    public int endPaint(int lpPaint) {
        WinDC dc = (WinDC)WinSystem.getObject(Memory.mem_readd(lpPaint));
        Main.drawImage(dc.getImage());
        releaseDC(dc);
        return WinAPI.TRUE;
    }

    public int invalidateRect(int lpRect, int bErase) {
        needsPainting = true;
        return WinAPI.TRUE;
    }

    public int getDC() {
        return WinSystem.createDC(WinSystem.getScreen(), false).getHandle();
    }

    public int releaseDC(WinDC dc) {
        dc.close();
        return 1;
    }

    public boolean isVisible() {
        return (style & WS_VISIBLE) != 0;
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
                WinSystem.getCurrentThread().setLastError(jdos.win.utils.Error.ERROR_INVALID_PARAMETER);
                return 0;
        }
    }

    private void growDesktop() {
        int width = rect.width();
        int cx;
        int cy;
        if (width<=640) {
            cx = 640;
            cy = 480;
        } else if (width <= 800) {
            cx = 800;
            cy = 600;
        } else {
            cx = 1024;
            cy = 768;
        }
        WinSystem.setScreenSize(cx, cy, WinSystem.getScreenBpp());
    }

    public int setWindowPos(int hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags) {
        if ((uFlags & 0x0001)!=0) { // SWP_NOSIZE
            cx = rect.width();
            cy = rect.height();
        }
        if ((uFlags & 0x0002)!=0) { // SWP_NOMOVE
            X = rect.left;
            Y = rect.top;
        }
        boolean moved = rect.left != X || rect.top != Y;
        boolean sized = rect.width() != cx || rect.height() != cy;
        rect.left = X;
        rect.top = Y;
        rect.right = X+cx;
        rect.bottom = Y+cy;
        clientRect = new WinRect(0, 0, rect.width(), rect.height());
        if (hParent==0 && rect.width()>WinSystem.getScreenWidth() || rect.height()>WinSystem.getScreenHeight()) {
            growDesktop();
        }
//        if ((uFlags & 0x0040)!=0) { // SWP_SHOWWINDOW
//            showWindow(true);
//        } else if ((uFlags & 0x0080)!=0) { // SWP_HIDEWINDOW
//            showWindow(false);
//        }
        // :TODO: send move / size messages
        return WinAPI.TRUE;
    }

    public int validateRect(int lpRect) {
        if (lpRect == 0)
            needsPainting = false;
        return WinAPI.TRUE;
    }

    public int getClientRect(int address) {
        WinRect.write(address, 0, 0, clientRect.width(), clientRect.height());
        return WinAPI.TRUE;
    }

    public int getWindowRect(int lpRect) {
        int x = getScreenX(rect.left);
        int y = getScreenY(rect.top);
        WinRect screenRect = new WinRect(x, y, x+rect.width(), y+rect.height());
        screenRect.write(lpRect);
        return WinAPI.TRUE;
    }

    private int getScreenX(int x) {
        int result = this.rect.left+x;
        if (parent != null)
            return parent.getScreenX(result);
        return result;
    }

    private int getScreenY(int y) {
        int result = this.rect.top+y;
        if (parent != null)
            return parent.getScreenY(result);
        return result;
    }

    public int screenToWindow(WinPoint p) {
        int x = getScreenX(rect.left);
        int y = getScreenX(rect.top);
        p.x = p.x - x;
        p.y = p.y - y;
        return getHitTest(p.x, p.y);
    }

    public void screenToClient(WinPoint p) {
        int x = getScreenX(rect.left);
        int y = getScreenX(rect.top);
        p.x = p.x - x;
        p.y = p.y - y;
        p.x -= clientRect.left;
        p.y -= clientRect.top;
    }

    private int getTopMostParent(WinWindow window) {
        if (window.hParent == 0)
            return window.handle;
        return getTopMostParent(window.parent);
    }
    public int getTopMostParent() {
        return getTopMostParent(this);
    }

    public int getHitTest(int x, int y) {
        return HTCLIENT;
    }

    public void showWindow(boolean show) {
        if (show) {
            if (!isActive) {
                postMessage(WM_ACTIVATEAPP, WinAPI.TRUE, 0);
                isActive = true;
            }
            style|=WS_VISIBLE;
            postMessage(WM_SHOWWINDOW, WinAPI.TRUE, 0);
            postMessage(WM_ACTIVATE, 1 /* WA_ACTIVE */, 0);
            WinPoint p = WinMouse.currentPos.copy();
            int hitTest = screenToWindow(p);
            if (rect.contains(p))
                postMessage(WM_SETCURSOR, handle, hitTest);
            if (hParent == 0) {
                currentFocus = handle;
                postMessage(WM_SETFOCUS, 0, 0);
            }
            needsPainting = true;
        } else {
            style&=~WS_VISIBLE;
        }
    }
    public void postMessage(int msg, int wParam, int lParam) {
        WinSystem.getCurrentThread().postMessage(handle, msg, wParam, lParam);
    }

    public int sendMessage(int msg, int wParam, int lParam) {
        long start = System.currentTimeMillis();
        System.out.println("sendMessage 0x"+Integer.toHexString(msg)+" start");
        WinSystem.call(winClass.eip, handle, msg, wParam, lParam);
        System.out.println("sendMessage 0x"+Integer.toHexString(msg)+" "+(System.currentTimeMillis()-start)+"ms");
        if (msg == WinWindow.WM_PAINT) {
            if (WinSystem.scheduler.monitor != 0) {
                start = System.currentTimeMillis();
                BufferedImage src = IDirectDrawSurface.getImage(WinSystem.scheduler.monitor, true).getImage();
                Main.drawImage(src);
                System.out.println("Update screen "+(System.currentTimeMillis()-start)+"ms");
            }
        }
        return CPU_Regs.reg_eax.dword;
    }

    public int defWindowProc(int msg, int wParam, int lParam) {
        if (msg == WM_TIMER) {
            timer.execute(wParam);
        } else if (msg == WM_CLOSE) {
            throw new Dos_programs.RebootException();
        } else if (msg == WM_SETCURSOR) {
            if (hParent != 0) {
                if (parent.sendMessage(msg, wParam, lParam)==WinAPI.FALSE) {
                    if (wParam == handle) {
                        if (winClass.hCursor != 0) {
                            WinSystem.setCursor(winClass.hCursor);
                        }
                    }
                }
            }
        }
        return 0;
    }
}
