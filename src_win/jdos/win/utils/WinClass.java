package jdos.win.utils;

import jdos.win.loader.winpe.LittleEndianFile;

public class WinClass extends WinObject {
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
        int menu = in.readInt();
        if (menu>0)
            menuName = new LittleEndianFile(menu).readCString();
        className = new LittleEndianFile(in.readInt()).readCString();
        hIconSm = in.readInt();
        if (WinSystem.getCurrentProcess().classNames.containsKey(className)) {
            return false;
        }
        WinSystem.getCurrentProcess().classNames.put(className, this);
        if (hbrBackground == 5+1) { // COLOR_WINDOW
            hbrBackground = WinSystem.createBrush(0xFFFFFFFF).getHandle();
        }
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
        int cbClsExtra = in.readInt();
        int cbWndExtra = in.readInt();
        hInstance = in.readInt();
        hIcon = in.readInt();
        hCursor = in.readInt();
        hbrBackground = in.readInt();
        int menu = in.readInt();
        if (menu>0)
            menuName = new LittleEndianFile(menu).readCString();
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

    public int id;
    public int style;
    public int eip;
    public int hInstance;
    public int hIcon;
    public int hCursor;
    public int hbrBackground;
    public String className;
    public String menuName;
    public int hIconSm;
}