package jdos.win.builtin.user32;

public class DialogInfo {
    int       hwndFocus;   /* Current control with focus */
    int       hUserFont;   /* Dialog font */
    int       hMenu;       /* Dialog menu */
    int       xBaseUnit;   /* Dialog units (depends on the font) */
    int       yBaseUnit;
    int       idResult;    /* EndDialog() result / default pushbutton ID */
    int       flags;       /* EndDialog() called for this dialog */
    boolean   endDialogCalled;

    public void close() {
    }
}
