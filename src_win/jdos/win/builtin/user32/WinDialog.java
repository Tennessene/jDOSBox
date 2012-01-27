package jdos.win.builtin.user32;

public class WinDialog {
    static public int doModal(WinWindow dlg, int lpTemplateName, int lpDialogFunc, int dwInitParam) {
        return 1; // Should be the result of EndDialog
    }
}
