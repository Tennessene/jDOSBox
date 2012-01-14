package jdos.win.system;

abstract public class WinMCI extends WinObject {
    static public final int MCI_NOTIFY_SUCCESSFUL = 0x0001;
    static public final int MCI_NOTIFY_SUPERSEDED = 0x0002;
    static public final int MCI_NOTIFY_ABORTED =    0x0004;
    static public final int MCI_NOTIFY_FAILURE =    0x0008;

    protected int hWnd;

    public WinMCI(int id) {
        super(id);
    }

    abstract public void play(int from, int to, int hWndCallback, boolean wait);

    public void sendNotification(int reason) {
        WinObject object = WinSystem.getObject(hWnd);
        if (object != null && (object instanceof WinWindow)) {
            ((WinWindow)object).getThread().postMessage(hWnd, WinWindow.MM_MCINOTIFY, reason, handle);
        }
    }
}
