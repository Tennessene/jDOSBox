package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.Error;
import jdos.win.utils.*;

public class User32 extends BuiltinModule {
    public User32(Loader loader, int handle) {
        super(loader, "user32.dll", handle);
        add(BeginPaint);
        add(CreateWindowExA);
        add(DefWindowProcA);
        add(DispatchMessageA);
        add(EndPaint);
        add(FindWindowA);
        add(GetClientRect);
        add(GetDC);
        add(GetForegroundWindow);
        add(GetMessageA);
        add(GetSysColor);
        add(GetSystemMetrics);
        add(GetWindowLongA);
        add(IsIconic);
        add(IsWindowVisible);
        add(LoadCursorA);
        add(LoadIconA);
        add(LoadImageA);
        add(PeekMessageA);
        add(RegisterClassA);
        add(RegisterClassExA);
        add(ReleaseDC);
        add(SetFocus);
        add(SetTimer);
        add(ShowCursor);
        add(ShowWindow);
        add(TranslateMessage);
        add(UpdateWindow);
        add(WaitForInputIdle);
        add(WaitMessage);
        add(wsprintfA);
    }

    // HDC BeginPaint(HWND hwnd, LPPAINTSTRUCT lpPaint)
    private Callback.Handler BeginPaint = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.BeginPaint";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpPaint = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                Log.exit("Weird state: BeginPaint couldn't find hWn");
            }
            WinWindow wnd = (WinWindow)object;
            CPU_Regs.reg_eax.dword = wnd.beginPaint(lpPaint);
        }
    };

    // HWND WINAPI CreateWindowEx(DWORD dwExStyle, LPCTSTR lpClassName, LPCTSTR lpWindowName, DWORD dwStyle, int x, int y, int nWidth, int nHeight, HWND hWndParent, HMENU hMenu, HINSTANCE hInstance, LPVOID lpParam)
    private Callback.Handler CreateWindowExA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.CreateWindowExA";
        }
        public void onCall() {
            int dwExStyle = CPU.CPU_Pop32();
            int lpClassName = CPU.CPU_Pop32();
            int lpWindowName = CPU.CPU_Pop32();
            int dwStyle = CPU.CPU_Pop32();
            int x = CPU.CPU_Pop32();
            int y = CPU.CPU_Pop32();
            int nWidth = CPU.CPU_Pop32();
            int nHeight = CPU.CPU_Pop32();
            int hWndParent = CPU.CPU_Pop32();
            int hMenu = CPU.CPU_Pop32();
            int hInstance = CPU.CPU_Pop32();
            int lpParam = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = 0;
            String name = null;
            WinClass winClass = null;
            if (lpWindowName != 0) {
                name = new LittleEndianFile(lpWindowName).readCString();
            }
            WinObject object = WinSystem.getObject(lpClassName);
            if (object != null && object instanceof WinClass) {
                winClass = (WinClass)object;
            } else {
                String className = new LittleEndianFile(lpClassName).readCString();
                winClass = (WinClass)WinSystem.getCurrentProcess().classNames.get(className);
            }
            if (winClass == null) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_CANNOT_FIND_WND_CLASS);
            }
            CPU_Regs.reg_eax.dword = WinSystem.createWindow(dwExStyle, winClass, name, dwStyle, x, y, nWidth, nHeight, hWndParent, hMenu, hInstance, lpParam).getHandle();
        }
    };

    // LRESULT WINAPI DefWindowProc(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    private Callback.Handler DefWindowProcA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.DefWindowProcA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int Msg = CPU.CPU_Pop32();
            int wParam = CPU.CPU_Pop32();
            int lParam = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                Log.exit("Weird state: DefWindowProc couldn't find hWn");
            }
            WinWindow wnd = (WinWindow)object;
            CPU_Regs.reg_eax.dword = wnd.defWindowProc(Msg, wParam, lParam);
        }
    };

    // LRESULT WINAPI DispatchMessage(const MSG *lpmsg)
    private Callback.Handler DispatchMessageA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.DispatchMessageA";
        }
        public void onCall() {
            int lpmsg = CPU.CPU_Pop32();
            int hWnd = Memory.mem_readd(lpmsg);
            if (hWnd != 0) {
                WinObject object = WinSystem.getObject(hWnd);
                if (object instanceof WinWindow) {
                    WinWindow window = (WinWindow)object;
                    CPU_Regs.reg_eax.dword = window.sendMessage(Memory.mem_readd(lpmsg+4), Memory.mem_readd(lpmsg+8), Memory.mem_readd(lpmsg+12));
                    return;
                }
            }
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // BOOL EndPaint(HWND hWnd, const PAINTSTRUCT *lpPaint)
    private Callback.Handler EndPaint = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.EndPaint";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpPaint = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                Log.exit("Weird state: EndPaint couldn't find hWn");
            }
            WinWindow wnd = (WinWindow)object;
            CPU_Regs.reg_eax.dword = wnd.endPaint(lpPaint);
        }
    };

    // HWND WINAPI FindWindow(LPCTSTR lpClassName, LPCTSTR lpWindowName)
    private Callback.Handler FindWindowA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.FindWindowA";
        }
        public void onCall() {
            int lpClassName = CPU.CPU_Pop32();
            int lpWindowName = CPU.CPU_Pop32();
            String className = null;
            String windowName = null;
            if (lpClassName != 0) {
                className = new LittleEndianFile(lpClassName).readCString();
            }
            if (lpWindowName != 0) {
                windowName = new LittleEndianFile(lpWindowName).readCString();
            }
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // BOOL WINAPI GetClientRect(HWND hWnd, LPRECT lpRect)
    private Callback.Handler GetClientRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetClientRect";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = ((WinWindow)object).getClientRect(lpRect);
            }
        }
    };

    // HDC GetDC(HWND hWnd)
    private Callback.Handler GetDC = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetDC";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                System.out.println(getName()+" faked");
                CPU_Regs.reg_eax.dword = ((WinWindow)object).getDC();
            }
        }
    };

    // HWND WINAPI GetForegroundWindow(void)
    private Callback.Handler GetForegroundWindow = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetForegroundWindow";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // BOOL WINAPI GetMessage(LPMSG lpMsg, HWND hWnd, UINT wMsgFilterMin, UINT wMsgFilterMax)
    private Callback.Handler GetMessageA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetMessageA";
        }
        public void onCall() {
            int lpMsg = CPU.CPU_Pop32();
            int hWnd = CPU.CPU_Pop32();
            int wMsgFilterMin = CPU.CPU_Pop32();
            int wMsgFilterMax = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentThread().getNextMessage(lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax);
        }
    };

    // DWORD WINAPI GetSysColor(int nIndex)
    private Callback.Handler GetSysColor = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetSysColor";
        }
        public void onCall() {
            int nIndex = CPU.CPU_Pop32();
            System.out.println(getName()+" "+nIndex+" faked");
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // int WINAPI GetSystemMetrics(int nIndex)
    private Callback.Handler GetSystemMetrics = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetSystemMetrics";
        }
        public void onCall() {
            int nIndex = CPU.CPU_Pop32();
            int result = 0;
            switch (nIndex) {
                case 0: // SM_CXSCREEN
                    result = WinSettings.SCREEN_CX;
                    break;
                case 1: // SM_CYSCREEN
                    result = WinSettings.SCREEN_CY;
                    break;
                default:
                    Console.out("GetSystemMetrics "+nIndex+" not implemented yet");
                    notImplemented();
            }
            CPU_Regs.reg_eax.dword = result;
        }
    };

    // BOOL WINAPI IsIconic(HWND hWnd)
    private Callback.Handler IsIconic = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.IsIconic";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                System.out.println(getName()+" faked");
                CPU_Regs.reg_eax.dword = ((WinWindow)object).isIconic();
            }
        }
    };

    // BOOL WINAPI IsWindowVisible(HWND hWnd)
    private Callback.Handler IsWindowVisible = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.IsWindowVisible";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                System.out.println(getName()+" faked");
                CPU_Regs.reg_eax.dword = ((WinWindow)object).isVisible();
            }
        }
    };

    // LONG WINAPI GetWindowLong(HWND hWnd, int nIndex)
    private Callback.Handler GetWindowLongA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetWindowLongA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int nIndex = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = ((WinWindow)object).getWindowLong(nIndex);
            }
        }
    };

    // HCURSOR WINAPI LoadCursor(HINSTANCE hInstance, LPCTSTR lpCursorName)
    private Callback.Handler LoadCursorA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.LoadCursorA";
        }
        public void onCall() {
            int hInstance = CPU.CPU_Pop32();
            int lpIconName = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.loadCursor(hInstance, lpIconName).getHandle();
        }
    };

    // HICON WINAPI LoadIcon(HINSTANCE hInstance, LPCTSTR lpIconName)
    private Callback.Handler LoadIconA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.LoadIconA";
        }
        public void onCall() {
            int hInstance = CPU.CPU_Pop32();
            int lpIconName = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.loadIcon(hInstance, lpIconName).getHandle();
        }
    };

    // HANDLE WINAPI LoadImage(HINSTANCE hinst, LPCTSTR lpszName, UINT uType, int cxDesired, int cyDesired, UINT fuLoad)
    private Callback.Handler LoadImageA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.LoadImageA";
        }
        public void onCall() {
            int hinst = CPU.CPU_Pop32();
            int lpszName = CPU.CPU_Pop32();
            int uType = CPU.CPU_Pop32();
            int cxDesired = CPU.CPU_Pop32();
            int cyDesired = CPU.CPU_Pop32();
            int fuLoad = CPU.CPU_Pop32();
            if (fuLoad != 0 && fuLoad != 0x2000) {
                Win.panic(getName()+" fuLoad flags are not currently supported: fuLoad = 0x"+Integer.toString(fuLoad, 16));
            }
            if (uType == 0) { // IMAGE_BITMAP
                Module m = WinSystem.getCurrentProcess().loader.getModuleByHandle(hinst);
                if (m instanceof NativeModule) {
                    NativeModule module = (NativeModule)m;
                    int bitmapAddress = module.getAddressOfResource(NativeModule.RT_BITMAP, lpszName);
                    if (bitmapAddress != 0) {
                        CPU_Regs.reg_eax.dword = WinSystem.createBitmap(bitmapAddress).getHandle();
                    } else {
                        // :TODO: what should the error be
                        CPU_Regs.reg_eax.dword = 0;
                    }
                    return;
                } else {
                    Win.panic(getName()+" currently does not support loading a image from a builtin module");
                }
            } else {
                Console.out("LoadImage type="+uType+" faked");
            }
            CPU_Regs.reg_eax.dword = 1;
        }
    };

    // BOOL WINAPI PeekMessage(LPMSG lpMsg, HWND hWnd, UINT wMsgFilterMin, UINT wMsgFilterMax, UINT wRemoveMsg)
    private Callback.Handler PeekMessageA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.PeekMessageA";
        }
        public void onCall() {
            int lpMsg = CPU.CPU_Pop32();
            int hWnd = CPU.CPU_Pop32();
            int wMsgFilterMin = CPU.CPU_Pop32();
            int wMsgFilterMax = CPU.CPU_Pop32();
            int wRemoveMsg = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentThread().peekMessage(lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax, wRemoveMsg);
        }
    };

    // ATOM WINAPI RegisterClass(const WNDCLASS *lpWndClass)
    private Callback.Handler RegisterClassA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.RegisterClassA";
        }
        public void onCall() {
            int lpWndClass = CPU.CPU_Pop32();
            WinClass c = WinSystem.createClass();
            if (!c.load(lpWndClass)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_CLASS_ALREADY_EXISTS);
            }
        }
    };

    // ATOM WINAPI RegisterClassEx(const WNDCLASSEX *lpwcx)
    private Callback.Handler RegisterClassExA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.RegisterClassExA";
        }
        public void onCall() {
            int lpwcx = CPU.CPU_Pop32();
            WinClass c = WinSystem.createClass();
            if (!c.loadEx(lpwcx)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_CLASS_ALREADY_EXISTS);
            }
        }
    };

    // int ReleaseDC(HWND hWnd, HDC hDC)
    private Callback.Handler ReleaseDC = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ReleaseDC";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int hDC = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return;
            }
            WinWindow window = (WinWindow)object;
            object = WinSystem.getObject(hDC);
            if (object == null || !(object instanceof WinDC)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return;
            }
            CPU_Regs.reg_eax.dword = window.releaseDC((WinDC)object);
        }
    };

    // HWND WINAPI SetFocus(HWND hWnd)
    private Callback.Handler SetFocus = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetFocus";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_WINDOW_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = hWnd; // No other window had focus, at least not yet
            }
        }
    };

    // UINT_PTR WINAPI SetTimer(HWND hWnd, UINT_PTR nIDEvent, UINT uElapse, TIMERPROC lpTimerFunc)
    private Callback.Handler SetTimer = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetTimer";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int nIDEvent = CPU.CPU_Pop32();
            int uElapse = CPU.CPU_Pop32();
            int lpTimerFunc = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                WinWindow window = (WinWindow)object;
                CPU_Regs.reg_eax.dword = window.timer.addTimer(uElapse, nIDEvent, lpTimerFunc);
            }
        }
    };

    // int WINAPI ShowCursor(BOOL bShow)
    private Callback.Handler ShowCursor = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ShowCursor";
        }
        public void onCall() {
            int bShow = CPU.CPU_Pop32();
            if (bShow != 0) {
                Console.out("ShowCursor(TRUE) not implemented yet");
                notImplemented();
            } else {
                CPU_Regs.reg_eax.dword = -1;
            }
        }
    };

    // BOOL WINAPI ShowWindow(HWND hWnd, int nCmdShow)
    private Callback.Handler ShowWindow = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ShowWindow";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int nCmdShow = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_WINDOW_HANDLE);
            } else {
                ((WinWindow)object).showWindow(nCmdShow != 0);
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // BOOL WINAPI TranslateMessage(const MSG *lpMsg)
    private Callback.Handler TranslateMessage = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.TranslateMessage";
        }
        public void onCall() {
            int lpMsg = CPU.CPU_Pop32();
            int message = Memory.mem_readd(lpMsg+4);
            if (message == WinWindow.WM_KEYDOWN || message == WinWindow.WM_KEYUP) {
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            } else if (message == WinWindow.WM_SYSKEYDOWN || message == WinWindow.WM_SYSKEYUP) {
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            } else {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
            }
        }
    };

    // BOOL UpdateWindow(HWND hWnd)
    private Callback.Handler UpdateWindow = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.UpdateWindow";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_WINDOW_HANDLE);
            } else {
                ((WinWindow)object).sendMessage(WinWindow.WM_PAINT, 0, 0);
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // DWORD WINAPI WaitForInputIdle(HANDLE hProcess, DWORD dwMilliseconds)
    private Callback.Handler WaitForInputIdle = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.WaitForInputIdle";
        }
        public void onCall() {
            int hProcess = CPU.CPU_Pop32();
            int dwMilliseconds = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = 0;
            System.out.println(getName()+" faked");
            //WinSystem.getCurrentThread().sleep(500); // fake it
        }
    };

    // BOOL WINAPI WaitMessage(void)
    private Callback.Handler WaitMessage = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.WaitMessage";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentThread().waitMessage();
        }
    };

    // int __cdecl wsprintf(LPTSTR lpOut, LPCTSTR lpFmt, ...)
    private Callback.Handler wsprintfA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.wsprintfA";
        }
        public void onCall() {
            int out = CPU.CPU_Peek32(0);
            int in = CPU.CPU_Peek32(1);
            String format = new LittleEndianFile(in).readCString();
            String result = StringUtil.format(format, false, 2);
            StringUtil.strcpy(out, result);
            CPU_Regs.reg_eax.dword = result.length();
        }
    };
}
