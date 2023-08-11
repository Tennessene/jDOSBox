package jdos.win.system;

import jdos.win.builtin.user32.WinWindow;

abstract public class WinMCI extends WinObject {
    static public final int MCI_NOTIFY_SUCCESSFUL = 0x0001;
    static public final int MCI_NOTIFY_SUPERSEDED = 0x0002;
    static public final int MCI_NOTIFY_ABORTED =    0x0004;
    static public final int MCI_NOTIFY_FAILURE =    0x0008;

    static public WinMCI getMCI(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinMCI))
            return null;
        return (WinMCI)object;
    }

    protected int hWnd;

    public WinMCI(int id) {
        super(id);
    }

    abstract public void play(int from, int to, int hWndCallback, boolean wait);
    abstract public void stop(int hWndCallback, boolean wait);
    abstract public void close(int hWndCallback, boolean wait);

    public void sendNotification(int reason) {
        WinWindow window = WinWindow.get(hWnd);
        if (window != null) {
            window.getThread().postMessage(hWnd, WinWindow.MM_MCINOTIFY, reason, handle);
        }
    }
}
