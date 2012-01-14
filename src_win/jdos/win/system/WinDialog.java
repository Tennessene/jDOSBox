package jdos.win.system;

public class WinDialog extends WinWindow {
    public WinDialog(int id, int hInstance, int hParent) {
        super(id, 0, null, null, 0, 0, 0, 0, 0, hParent, 0, hInstance, 0);
    }

    public int doModal(int lpTemplateName, int lpDialogFunc, int dwInitParam) {
        return 1; // Should be the result of EndDialog
    }
}
