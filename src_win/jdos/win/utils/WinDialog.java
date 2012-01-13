package jdos.win.utils;

public class WinDialog extends WinWindow {
    public WinDialog(int id, int hInstance, int hParent) {
        super(id, hInstance, hParent);
    }

    public int doModal(int lpTemplateName, int lpDialogFunc, int dwInitParam) {
        return 1; // Should be the result of EndDialog
    }
}
