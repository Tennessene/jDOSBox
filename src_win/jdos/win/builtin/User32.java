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
import jdos.win.system.*;
import jdos.win.utils.Error;
import jdos.win.utils.StringUtil;

import javax.swing.*;

public class User32 extends BuiltinModule {
    public User32(Loader loader, int handle) {
        super(loader, "user32.dll", handle);
        add(AppendMenuA);
        add(BeginPaint);
        add(CharUpperA);
        add(ClientToScreen);
        add(CreatePopupMenu);
        add(CreateWindowExA);
        add(DefWindowProcA);
        add(DispatchMessageA);
        add(DrawTextA);
        add(EndPaint);
        add(FillRect);
        add(FindWindowA);
        add(GetCapture);
        add(GetClientRect);
        add(GetCursorPos);
        add(GetDC);
        add(GetForegroundWindow);
        add(GetKeyState);
        add(GetMenu);
        add(GetMenuItemCount);
        add(GetMessageA);
        add(GetSysColor);
        add(GetSystemMetrics);
        add(GetWindowLongA);
        add(GetWindowRect);
        add(InvalidateRect);
        add(IsIconic);
        add(IsWindowVisible);
        add(LoadAcceleratorsA);
        add(LoadCursorA);
        add(LoadIconA);
        add(LoadImageA);
        add(LoadStringA);
        add(MessageBoxA);
        add(OffsetRect);
        add(PeekMessageA);
        add(PostMessageA);
        add(RegisterClassA);
        add(RegisterClassExA);
        add(ReleaseCapture);
        add(ReleaseDC);
        add(SendMessageA);
        add(SetCapture);
        add(SetClassLongA);
        add(SetCursor);
        add(SetCursorPos);
        add(SetFocus);
        add(SetMenuItemInfoA);
        add(SetRect);
        add(SetTimer);
        add(SetWindowPos);
        add(ShowCursor);
        add(ShowWindow);
        add(SystemParametersInfoA);
        add(TranslateAcceleratorA);
        add(TranslateMessage);
        add(UpdateWindow);
        add(ValidateRect);
        add(WaitForInputIdle);
        add(WaitMessage);
        add(wsprintfA);
    }

    // BOOL WINAPI AppendMenu(HMENU hMenu, UINT uFlags, UINT_PTR uIDNewItem, LPCTSTR lpNewItem)
    private Callback.Handler AppendMenuA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.AppendMenuA";
        }
        public void onCall() {
            int hMenu = CPU.CPU_Pop32();
            int uFlags = CPU.CPU_Pop32();
            int uIDNewItem = CPU.CPU_Pop32();
            int lpNewItem = CPU.CPU_Pop32();
            if (hMenu == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                WinObject object = WinSystem.getObject(hMenu);
                if (object == null || !(object instanceof WinMenu)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                } else {
                    CPU_Regs.reg_eax.dword = ((WinMenu)object).append(uFlags, uIDNewItem, lpNewItem);
                }
            }
            if (LOG)
                log(" uFlags=0x"+Integer.toString(uFlags, 16)+" uIDNewItem="+uIDNewItem+" lpNewItem="+((lpNewItem==0 || uFlags!=0)?Integer.toString(lpNewItem):new LittleEndianFile(lpNewItem).readCString()+"@"+Integer.toString(lpNewItem, 16))+" result="+CPU_Regs.reg_eax.dword);
        }
    };

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

    // BOOL ClientToScreen(HWND hWnd, LPPOINT lpPoint)
    private Callback.Handler ClientToScreen = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ClientToScreen";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpPoint = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                Log.exit("Weird state: BeginPaint couldn't find hWn");
            }
            WinWindow wnd = (WinWindow)object;
            CPU_Regs.reg_eax.dword = wnd.clientToScreen(lpPoint);
        }
    };

    // LPTSTR WINAPI CharUpper(LPTSTR lpsz)
    private Callback.Handler CharUpperA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.CharUpperA";
        }
        public void onCall() {
            int lpsz = CPU.CPU_Pop32();
            if (lpsz<0)
                CPU_Regs.reg_eax.dword = StringUtil.toupperW((char)lpsz);
            else {
                String value = new LittleEndianFile(lpsz).readCString();
                StringUtil.strcpy(lpsz, value.toUpperCase());
                CPU_Regs.reg_eax.dword = lpsz;
            }
        }
    };

    // HMENU WINAPI CreatePopupMenu(void)
    private Callback.Handler CreatePopupMenu = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.CreatePopupMenu";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.createMenu().getHandle();
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
                if (winClass == null) {
                    Win.panic(getName()+" class "+className+" could not be found.");
                }
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
            defaultLog = false;
        }
    };

    // INT_PTR WINAPI DialogBoxParam(HINSTANCE hInstance, LPCTSTR lpTemplateName, HWND hWndParent, DLGPROC lpDialogFunc, LPARAM dwInitParam)
    private Callback.Handler DialogBoxParam = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.DialogBoxParam";
        }
        public void onCall() {
            int hInstance = CPU.CPU_Pop32();
            int lpTemplateName = CPU.CPU_Pop32();
            int hWndParent = CPU.CPU_Pop32();
            int lpDialogFunc = CPU.CPU_Pop32();
            int dwInitParam = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWndParent);
            WinWindow parent = null;
            if (object == null && !(object instanceof WinWindow)) {
                hWndParent  = 0;
            }
            WinDialog dlg = WinSystem.createDialog(hInstance, hWndParent);
            CPU_Regs.reg_eax.dword = dlg.doModal(lpTemplateName, lpDialogFunc, dwInitParam);
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
                    System.out.println(getName()+" msg="+Memory.mem_readd(lpmsg+4));
                    CPU_Regs.reg_eax.dword = window.sendMessage(Memory.mem_readd(lpmsg+4), Memory.mem_readd(lpmsg+8), Memory.mem_readd(lpmsg+12));
                    return;
                }
            }
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // int DrawText(HDC hDC, LPCTSTR lpchText, int nCount, LPRECT lpRect, UINT uFormat)
    private Callback.Handler DrawTextA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.DrawTextA";
        }
        public void onCall() {
            int hDC = CPU.CPU_Pop32();
            int lpchText = CPU.CPU_Pop32();
            int nCount = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            int uFormat = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hDC);
            if (object == null || !(object instanceof WinDC)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = 0;
            } else {
                CPU_Regs.reg_eax.dword = ((WinDC)object).drawText(lpchText, nCount, lpRect, uFormat);
            }
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

    // int FillRect(HDC hDC, const RECT *lprc, HBRUSH hbr)
    private Callback.Handler FillRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.FillRect";
        }
        public void onCall() {
            int hDC = CPU.CPU_Pop32();
            int lprc = CPU.CPU_Pop32();
            int hbr = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hDC);
            if (object == null || !(object instanceof WinDC)) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
            } else {
                CPU_Regs.reg_eax.dword = ((WinDC)object).fillRect(lprc, hbr);
            }
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
            CPU_Regs.reg_eax.dword = WinSystem.desktop.findWindow(className, windowName);
        }
    };

    // HWND WINAPI GetCapture(void)
    private Callback.Handler GetCapture = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetCapture";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCapture();
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

    // BOOL WINAPI GetCursorPos(LPPOINT lpPoint)
    private Callback.Handler GetCursorPos = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "User32.GetCursorPos";
        }
        public int processReturn() {
            int lpPoint = CPU.CPU_Pop32();
            Memory.mem_writed(lpPoint, WinMouse.currentPos.x);
            Memory.mem_writed(lpPoint+4, WinMouse.currentPos.y);
            return WinAPI.TRUE;
        }
    };

    // HDC GetDC(HWND hWnd)
    private Callback.Handler GetDC = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetDC";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            if (hWnd == 0) {
                CPU_Regs.reg_eax.dword = WinSystem.createDC(null, WinSystem.getScreenAddress(), WinSystem.getScreenWidth(), WinSystem.getScreenHeight(), null).getHandle();
            } else {
                WinObject object = WinSystem.getObject(hWnd);
                if (object == null || !(object instanceof WinWindow)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                } else {
                    CPU_Regs.reg_eax.dword = ((WinWindow)object).getDC();
                }
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

    // SHORT WINAPI GetKeyState(int nVirtKey)
    private Callback.Handler GetKeyState = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetKeyState";
        }
        public void onCall() {
            int nVirtKey = CPU.CPU_Pop32();
            if (WinSystem.getCurrentThread().getKeyState().get(nVirtKey))
                CPU_Regs.reg_eax.dword = 0x8000;
            else
                CPU_Regs.reg_eax.dword = 0;
        }
    };

    // HMENU WINAPI GetMenu(HWND hWnd)
     private Callback.Handler GetMenu = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetMenu";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            if (hWnd == 0) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                WinObject object = WinSystem.getObject(hWnd);
                if (object == null || !(object instanceof WinWindow)) {
                    CPU_Regs.reg_eax.dword = 0;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                } else {
                    CPU_Regs.reg_eax.dword = ((WinWindow)object).getMenu();
                }
            }
        }
    };

    // int WINAPI GetMenuItemCount(HMENU hMenu)
    private Callback.Handler GetMenuItemCount = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetMenuItemCount";
        }
        public void onCall() {
            int hMenu = CPU.CPU_Pop32();
            if (hMenu == 0) {
                CPU_Regs.reg_eax.dword = -1;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                WinObject object = WinSystem.getObject(hMenu);
                if (object == null || !(object instanceof WinMenu)) {
                    CPU_Regs.reg_eax.dword = -1;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                } else {
                    CPU_Regs.reg_eax.dword = ((WinMenu)object).getItemCount();
                }
            }
        }
    };

    // BOOL WINAPI GetMessage(LPMSG lpMsg, HWND hWnd, UINT wMsgFilterMin, UINT wMsgFilterMax)
    private Callback.Handler GetMessageA = new WaitHandlerBase() {
        public java.lang.String getName() {
            return "User32.GetMessageA";
        }
        public int onWait() {
            int lpMsg = CPU.CPU_Pop32();
            int hWnd = CPU.CPU_Pop32();
            int wMsgFilterMin = CPU.CPU_Pop32();
            int wMsgFilterMax = CPU.CPU_Pop32();
            int result = WinSystem.getCurrentThread().getNextMessage(lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax);
            defaultLog = false;
            if (result == -2) {
                // :TODO: figure a way to put the thread to sleep until we get input?
                return 1; // just yield, don't remove this thread from scheduling
            } else {
                CPU_Regs.reg_eax.dword = result;
                return 0;
            }
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

    // BOOL InvalidateRect(HWND hWnd, const RECT *lpRect, BOOL bErase)
    private Callback.Handler InvalidateRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.InvalidateRect";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            int bErase = CPU.CPU_Pop32();
            if (hWnd == 0) {
                Win.panic(getName()+" NULL hWnd not implemented yet");
            }
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = ((WinWindow)object).invalidateRect(lpRect, bErase);
            }
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
                CPU_Regs.reg_eax.dword = ((WinWindow)object).isVisible()?WinAPI.TRUE:WinAPI.FALSE;
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

    // BOOL WINAPI GetWindowRect(HWND hWnd, LPRECT lpRect)
    private Callback.Handler GetWindowRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.GetWindowRect";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = ((WinWindow)object).getWindowRect(lpRect);
            }
        }
    };

    // HACCEL WINAPI LoadAccelerators(HINSTANCE hInstance, LPCTSTR lpTableName)
    private Callback.Handler LoadAcceleratorsA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.LoadAcceleratorsA";
        }
        public void onCall() {
            int hInstance = CPU.CPU_Pop32();
            int lpTableName = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = 0xAAAA;
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

    // int WINAPI LoadString(HINSTANCE hInstance, UINT uID, LPTSTR lpBuffer, int nBufferMax)
    private Callback.Handler LoadStringA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.LoadStringA";
        }
        public void onCall() {
            int hInstance = CPU.CPU_Pop32();
            int uID = CPU.CPU_Pop32();
            int lpBuffer = CPU.CPU_Pop32();
            int nBufferMax = CPU.CPU_Pop32();
            Module m = WinSystem.getCurrentProcess().loader.getModuleByHandle(hInstance);
            if (m instanceof NativeModule) {
                NativeModule module = (NativeModule)m;
                int stringAddress = module.getAddressOfResource(NativeModule.RT_STRING, (uID >> 4)+1);
                if (stringAddress != 0) {
                    int index = uID & 0xf;
                    for (int i = 0; i < index; i++)
                        stringAddress += Memory.mem_readw(stringAddress)*2 + 2;
                    int len = Memory.mem_readw(stringAddress);
                    stringAddress+=2;
                    String result = new LittleEndianFile(stringAddress).readCStringW(len);
                    StringUtil.strncpy(lpBuffer, result, nBufferMax);
                    CPU_Regs.reg_eax.dword = Math.min(result.length(), nBufferMax);
                    if (LOG)
                        log("result=0x"+Integer.toString(CPU_Regs.reg_eax.dword, 16)+" "+result);
                    return;
                }
            }
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // int WINAPI MessageBox(HWND hWnd, LPCTSTR lpText, LPCTSTR lpCaption, UINT uType)
    private Callback.Handler MessageBoxA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.MessageBoxA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpText = CPU.CPU_Pop32();
            int lpCaption = CPU.CPU_Pop32();
            int uType = CPU.CPU_Pop32();
            String text = new LittleEndianFile(lpText).readCString();
            String caption = "";
            if (lpCaption != 0)
                caption = new LittleEndianFile(lpCaption).readCString();
            int type = JOptionPane.INFORMATION_MESSAGE;
            if ((uType & 0x00000040)!=0) // MB_ICONINFORMATION
                type = JOptionPane.INFORMATION_MESSAGE;
            else if ((uType & 0x00000030)!=0) // MB_ICONWARNING
                type = JOptionPane.WARNING_MESSAGE;
            else if ((uType & 0x00000020)!=0) // MB_ICONQUESTION
                type = JOptionPane.QUESTION_MESSAGE;
            else if ((uType & 0x00000010)!=0) // MB_ICONERROR
                type = JOptionPane.ERROR_MESSAGE;
            JOptionPane.showMessageDialog(null, text, caption, type);
            CPU_Regs.reg_eax.dword = 1; // IDOK
        }
    };

    // BOOL OffsetRect(LPRECT lprc, int dx, int dy)
    private Callback.Handler OffsetRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.OffsetRect";
        }
        public void onCall() {
            int lprc = CPU.CPU_Pop32();
            int dx = CPU.CPU_Pop32();
            int dy = CPU.CPU_Pop32();
            WinRect rect = new WinRect(lprc);
            rect.left+=dx;
            rect.right+=dx;
            rect.top+=dy;
            rect.bottom+=dy;
            rect.write(lprc);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
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

    // BOOL WINAPI PostMessage(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    private Callback.Handler PostMessageA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.PostMessageA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int Msg = CPU.CPU_Pop32();
            int wParam = CPU.CPU_Pop32();
            int lParam = CPU.CPU_Pop32();
            if (hWnd == 0)
                WinSystem.getCurrentThread().postMessage(hWnd, Msg, wParam, lParam);
            else if (hWnd == 0xFFFF) {
                Win.panic("Broadcast PostMessage not implemented yet");
            } else {
                WinObject object = WinSystem.getObject(hWnd);
                if (object == null || !(object instanceof WinWindow)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    return;
                }
                ((WinWindow)object).getThread().postMessage(hWnd, Msg, wParam, lParam);
            }
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
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

    // BOOL WINAPI ReleaseCapture(void)
    private Callback.Handler ReleaseCapture = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ReleaseCapture";
        }
        public void onCall() {
            WinSystem.setCapture(0);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
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

    // LRESULT WINAPI SendMessage(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    private Callback.Handler SendMessageA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SendMessageA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int Msg = CPU.CPU_Pop32();
            int wParam = CPU.CPU_Pop32();
            int lParam = CPU.CPU_Pop32();
            if (hWnd == 0xFFFF) {
                Win.panic("Broadcast SendMessage not implemented yet");
            }
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return;
            }
            // CPU_Regs.reg_eax.dword is assigned inside, since when this returns we may be executing in a new process
            ((WinWindow)object).getThread().sendMessage(hWnd, Msg, wParam, lParam);
        }
    };

    // HWND WINAPI SetCapture(HWND hWnd)
    private Callback.Handler SetCapture = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetCapture";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_WINDOW_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = WinSystem.getCapture();
                WinSystem.setCapture(hWnd);
            }
        }
    };

    // DWORD WINAPI SetClassLong(HWND hWnd, int nIndex, LONG dwNewLong)
    private Callback.Handler SetClassLongA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetClassLongA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int nIndex = CPU.CPU_Pop32();
            int dwNewLong = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = 0;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_WINDOW_HANDLE);
            } else {
                WinClass winClass = ((WinWindow)object).getWinClass();
                CPU_Regs.reg_eax.dword = winClass.setLong(nIndex, dwNewLong);
            }
        }
    };

    // HCURSOR WINAPI SetCursor(HCURSOR hCursor)
    private Callback.Handler SetCursor = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetCursor";
        }
        public void onCall() {
            int hCursor = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.setCursor(hCursor);
        }
    };

    // BOOL WINAPI SetCursorPos(int X, int Y)
    private Callback.Handler SetCursorPos = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "User32.SetCursorPos";
        }
        public int processReturn() {
            int X = CPU.CPU_Pop32();
            int Y = CPU.CPU_Pop32();
            WinMouse.currentPos.x = X;
            WinMouse.currentPos.y = Y;
            return WinAPI.TRUE;
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
                CPU_Regs.reg_eax.dword = ((WinWindow)object).setFocus();
            }
        }
    };

    // BOOL SetRect(LPRECT lprc, int xLeft, int yTop, int xRight, int yBottom)
    private Callback.Handler SetRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetRect";
        }
        public void onCall() {
            int lprc = CPU.CPU_Pop32();
            int xLeft = CPU.CPU_Pop32();
            int yTop = CPU.CPU_Pop32();
            int xRight = CPU.CPU_Pop32();
            int yBottom = CPU.CPU_Pop32();
            WinRect.write(lprc, xLeft, yTop, xRight, yBottom);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI SetMenuItemInfo(HMENU hMenu, UINT uItem, BOOL fByPosition, LPMENUITEMINFO lpmii)
    private Callback.Handler SetMenuItemInfoA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetMenuItemInfoA";
        }
        public void onCall() {
            int hMenu = CPU.CPU_Pop32();
            int uItem = CPU.CPU_Pop32();
            int fByPosition = CPU.CPU_Pop32();
            int lpmii = CPU.CPU_Pop32();
            if (hMenu == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                WinObject object = WinSystem.getObject(hMenu);
                if (object == null || !(object instanceof WinMenu)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                } else {
                    CPU_Regs.reg_eax.dword = ((WinMenu)object).setItemInfo(uItem, fByPosition, lpmii);
                }
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

    // BOOL WINAPI SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, UINT uFlags)
    private Callback.Handler SetWindowPos = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SetWindowPos";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int hWndInsertAfter = CPU.CPU_Pop32();
            int X = CPU.CPU_Pop32();
            int Y = CPU.CPU_Pop32();
            int cx = CPU.CPU_Pop32();
            int cy = CPU.CPU_Pop32();
            int uFlags = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                WinWindow window = (WinWindow)object;
                CPU_Regs.reg_eax.dword = window.setWindowPos(hWndInsertAfter, X, Y, cx, cy, uFlags);
            }
            if (LOG)
                log("hWnd="+hWnd+" hWndInsertAfter="+hWndInsertAfter+" X="+X+" Y="+Y+" cx="+cx+" cy="+cy+" uFlags=0x"+Integer.toString(uFlags, 16));
        }
    };

    // int WINAPI ShowCursor(BOOL bShow)
    private Callback.Handler ShowCursor = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ShowCursor";
        }
        public void onCall() {
            int bShow = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.showCursor(bShow!=0);
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

    // BOOL WINAPI SystemParametersInfo(UINT uiAction, UINT uiParam, PVOID pvParam, UINT fWinIni)
    private Callback.Handler SystemParametersInfoA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.SystemParametersInfoA";
        }
        public void onCall() {
            int uiAction = CPU.CPU_Pop32();
            int uiParam = CPU.CPU_Pop32();
            int pvParam = CPU.CPU_Pop32();
            int fWinIni = CPU.CPU_Pop32();
            switch (uiAction) {
                case 0x0010: // SPI_GETSCREENSAVEACTIVE
                    Memory.mem_writed(pvParam, WinAPI.FALSE);
                    break;
                case 0x0053: // SPI_GETLOWPOWERACTIVE
                    Memory.mem_writed(pvParam, WinAPI.FALSE);
                    break;
                default:
                    System.out.println("uiAction 0x"+Integer.toString(uiAction, 16)+" not supported yet");
                    notImplemented();
            }
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // int WINAPI TranslateAccelerator(HWND hWnd, HACCEL hAccTable, LPMSG lpMsg)
    private Callback.Handler TranslateAcceleratorA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.TranslateAcceleratorA";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int hAccTable = CPU.CPU_Pop32();
            int lpMsg = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = 0;
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
            defaultLog = false;
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

    // BOOL ValidateRect(HWND hWnd, const RECT *lpRect)
    private Callback.Handler ValidateRect = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.ValidateRect";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            if (hWnd == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
                System.out.println(getName()+" hWnd=NULL faked");
            } else {
                WinObject object = WinSystem.getObject(hWnd);
                if (object == null || !(object instanceof WinWindow)) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_WINDOW_HANDLE);
                } else {
                    CPU_Regs.reg_eax.dword = ((WinWindow)object).validateRect(lpRect);
                }
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
