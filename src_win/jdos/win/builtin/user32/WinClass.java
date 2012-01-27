package jdos.win.builtin.user32;

import jdos.win.Win;
import jdos.win.builtin.gdi32.WinDC;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;
import jdos.win.utils.StringUtil;

import java.util.Hashtable;

public class WinClass extends WinObject {
    static public WinClass create() {
        int id = nextObjectId();
        if (id>0xFFFF)
            Win.panic("CLASS atom can not be greater than 0xFFFF");
        return new WinClass(id);
    }

    static public WinClass get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinClass))
            return null;
        return (WinClass)object;
    }

    // DWORD WINAPI GetClassLong(HWND hWnd, int nIndex)
    static public int GetClassLongA(int hWnd, int nIndex) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
         if (nIndex>=0) {
            Integer old = (Integer)window.winClass.extra.get(new Integer(nIndex));
            if (old != null)
                return old;
            return 0;
        }
        int result = 0;
        switch (nIndex) {
            case GCL_CBCLSEXTRA:
                return window.winClass.cbClsExtra;
            case GCL_CBWNDEXTRA:
                return window.winClass.cbWndExtra;
            case GCL_HBRBACKGROUND:
                return window.winClass.hbrBackground;
            case GCL_HCURSOR:
                return window.winClass.hCursor;
            case GCL_HICON:
                return window.winClass.hIcon;
            case GCL_HICONSM:
                return window.winClass.hIconSm;
            case GCL_HMODULE:
                return window.winClass.hInstance;
            case GCL_STYLE:
                return window.winClass.style;
            case GCL_WNDPROC:
                return window.winClass.eip;
            default:
                Win.panic("SetClassLong index=" + nIndex + " not implemented yet");
        }
        return 0;
    }
    // int WINAPI GetClassName(HWND hWnd, LPTSTR lpClassName, int nMaxCount)
    static public int GetClassNameA(int hWnd, int lpClassName, int nMaxCount) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        return StringUtil.strncpy(lpClassName, window.winClass.name, nMaxCount);
    }

    // ATOM WINAPI RegisterClass(const WNDCLASS *lpWndClass)
    static public int RegisterClassA(int lpWndClass) {
        WinClass c = WinClass.create();
        if (!c.load(lpWndClass)) {
            SetLastError(Error.ERROR_CLASS_ALREADY_EXISTS);
            c.close();
            return 0;
        }
        return c.handle;
    }

    // ATOM WINAPI RegisterClassEx(const WNDCLASSEX *lpwcx)
    static public int RegisterClassExA(int lpwcx) {
        WinClass c = WinClass.create();
        if (!c.loadEx(lpwcx)) {
            SetLastError(Error.ERROR_CLASS_ALREADY_EXISTS);
            c.close();
            return 0;
        }
        return c.handle;
    }

    // DWORD WINAPI SetClassLong(HWND hWnd, int nIndex, LONG dwNewLong)
    static public int SetClassLongA(int hWnd, int nIndex, int dwNewLong) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null)
            return 0;
        if (nIndex>=0) {
            Integer old = (Integer)window.winClass.extra.get(new Integer(nIndex));
            window.winClass.extra.put(new Integer(nIndex), new Integer(dwNewLong));
            if (old != null)
                return old.intValue();
            return 0;
        }
        int result = 0;
        switch (nIndex) {
            case GCL_CBCLSEXTRA:
                result = window.winClass.cbClsExtra;
                window.winClass.cbClsExtra = dwNewLong;
                break;
            case GCL_CBWNDEXTRA:
                result = window.winClass.cbWndExtra;
                window.winClass.cbWndExtra = dwNewLong;
                break;
            case GCL_HBRBACKGROUND:
                result = window.winClass.hbrBackground;
                window.winClass.hbrBackground = dwNewLong;
                break;
            case GCL_HCURSOR:
                result = window.winClass.hCursor;
                window.winClass.hCursor = dwNewLong;
                break;
            case GCL_HICON:
                result = window.winClass.hIcon;
                window.winClass.hIcon = dwNewLong;
                break;
            case GCL_HICONSM:
                result = window.winClass.hIconSm;
                window.winClass.hIconSm = dwNewLong;
                break;
            case GCL_HMODULE:
                result = window.winClass.hInstance;
                window.winClass.hInstance = dwNewLong;
                break;
            case GCL_STYLE:
                result = window.winClass.style;
                window.winClass.style = dwNewLong;
                break;
            case GCL_WNDPROC:
                result = window.winClass.eip;
                window.winClass.eip = dwNewLong;
                break;
            default:
                Win.panic("SetClassLong index=" + nIndex + " not implemented yet");
        }
        return result;
    }

    public WinClass(int id) {
        super(id);
        this.id = id;
    }

    /*
    typedef struct tagWNDCLASSEX {
      UINT      cbSize;
      UINT      style;
      WNDPROC   lpfnWndProc;
      int       cbClsExtra;
      int       cbWndExtra;
      HINSTANCE hInstance;
      HICON     hIcon;
      HCURSOR   hCursor;
      HBRUSH    hbrBackground;
      LPCTSTR   lpszMenuName;
      LPCTSTR   lpszClassName;
      HICON     hIconSm;
    }
    */
    public boolean loadEx(int address) {
        LittleEndianFile in = new LittleEndianFile(address);
        int cb = in.readInt();
        style = in.readInt();
        eip = in.readInt();
        cbClsExtra = in.readInt();
        cbWndExtra = in.readInt();
        hInstance = in.readInt();
        hIcon = in.readInt();
        hCursor = in.readInt();
        hbrBackground = in.readInt();
        pMenuName = in.readInt();
        className = new LittleEndianFile(in.readInt()).readCString();
        hIconSm = in.readInt();
        if (WinSystem.getCurrentProcess().classNames.containsKey(className)) {
            return false;
        }
        WinSystem.getCurrentProcess().classNames.put(className, this);
        return true;
    }

    /*
    typedef struct tagWNDCLASS {
      UINT      style;
      WNDPROC   lpfnWndProc;
      int       cbClsExtra;
      int       cbWndExtra;
      HINSTANCE hInstance;
      HICON     hIcon;
      HCURSOR   hCursor;
      HBRUSH    hbrBackground;
      LPCTSTR   lpszMenuName;
      LPCTSTR   lpszClassName;
    }
     */
    public boolean load(int address) {
        LittleEndianFile in = new LittleEndianFile(address);
        style = in.readInt();
        eip = in.readInt();
        cbClsExtra = in.readInt();
        cbWndExtra = in.readInt();
        hInstance = in.readInt();
        hIcon = in.readInt();
        hCursor = in.readInt();
        hbrBackground = in.readInt();
        pMenuName = in.readInt();
        className = new LittleEndianFile(in.readInt()).readCString();
        if (WinSystem.getCurrentProcess().classNames.containsKey(className)) {
            return false;
        }
        WinSystem.getCurrentProcess().classNames.put(className, this);
        return true;
    }

    public void onFree() {
        WinSystem.getCurrentProcess().classNames.remove(className);
    }

    public WinDC dc;
    public int id;
    public int style;
    public int eip;
    public int hInstance;
    public int hIcon;
    public int hCursor;
    public int hbrBackground;
    public String className;
    public int pMenuName;
    public int hIconSm;
    public int cbClsExtra;
    public int cbWndExtra;
    private Hashtable extra;
}
