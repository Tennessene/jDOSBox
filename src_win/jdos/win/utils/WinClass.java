package jdos.win.utils;

import jdos.cpu.Callback;
import jdos.win.kernel.WinCallback;
import jdos.win.loader.winpe.LittleEndianFile;

public class WinClass extends WinObject {
    private Callback.Handler WinProc = new Callback.Handler() {
        public String getName() {
            return "WinProc";
        }
        public int call() {
            return 1; // return from SendMessage
        }
    };

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
        int cbClsExtra = in.readInt();
        int cbWndExtra = in.readInt();
        hInstance = in.readInt();
        hIcon = in.readInt();
        hCursor = in.readInt();
        hbrBackground = in.readInt();
        className = new LittleEndianFile(in.readInt()).readCString();
        hIconSm = in.readInt();
        if (WinSystem.getCurrentProcess().classNames.containsKey(className)) {
            return false;
        }
        WinSystem.getCurrentProcess().classNames.put(className, this);

        // :TODO: this will leak
        int callback = WinCallback.addCallback(WinProc);
        returnEip =  WinSystem.getCurrentProcess().loader.registerFunction(callback);
        return true;
    }

    public void onFree() {
        WinSystem.getCurrentProcess().classNames.remove(className);
    }

    public int id;
    public int style;
    public int eip;
    public int returnEip;
    public int hInstance;
    public int hIcon;
    public int hCursor;
    public int hbrBackground;
    public String className;
    public int hIconSm;
}
