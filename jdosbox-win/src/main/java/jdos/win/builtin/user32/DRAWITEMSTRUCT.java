package jdos.win.builtin.user32;

import jdos.win.builtin.WinAPI;
import jdos.win.system.WinRect;

public class DRAWITEMSTRUCT extends WinAPI {
    public static final int SIZE = 48;

    public DRAWITEMSTRUCT() {
        rcItem = new WinRect();
    }

    public void write(int address) {
        writed(address, CtlType);address+=4;
        writed(address, CtlID);address+=4;
        writed(address, itemID);address+=4;
        writed(address, itemAction);address+=4;
        writed(address, itemState);address+=4;
        writed(address, hwndItem);address+=4;
        writed(address, hDC);address+=4;
        rcItem.write(address);address+=WinRect.SIZE;
        writed(address, itemData);address+=4;
    }
    public int allocTemp() {
        int result = getTempBuffer(SIZE);
        write(result);
        return result;
    }

    public int     CtlType;    /* Type of control (ODT_* flags from "winuser.h") */
    public int     CtlID;      /* Control ID */
    public int     itemID;     /* Menu item ID */
    public int     itemAction; /* Action to perform (ODA_* flags from "winuser.h") */
    public int     itemState;  /* Item state (ODS_* flags from "winuser.h") */
    public int     hwndItem;   /* Control window */
    public int     hDC;        /* Device context to draw to */
    public WinRect rcItem;     /* Position of the control in hDC */
    public int     itemData;   /* Extra data added by the application, if any */
}
