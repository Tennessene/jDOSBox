package jdos.win.builtin;

public class WinAPI {
    static final public int TRUE = 1;
    static final public int FALSE = 0;

    static final public int INVALID_HANDLE_VALUE = -1;

    /* Unicode char type flags */
    static final public int	CT_CTYPE1 =	0x0001;	/* usual ctype */
    static final public int	CT_CTYPE2 =	0x0002;	/* bidirectional layout info */
    static final public int CT_CTYPE3 =	0x0004;	/* textprocessing info */

    static final public int GENERIC_READ    = 0x80000000;
    static final public int GENERIC_WRITE   = 0x40000000;
    static final public int GENERIC_EXECUTE = 0x20000000;
    static final public int GENERIC_ALL     = 0x10000000;

    static final public String SYSTEM32_PATH = "C:\\Windows\\System32\\";
    static final public String WIN32_PATH = "C:\\WINDOWS";
}
