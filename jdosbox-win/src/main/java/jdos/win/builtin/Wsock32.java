package jdos.win.builtin;

import jdos.win.Win;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.StringUtil;

public class Wsock32 extends BuiltinModule {
    public Wsock32(Loader loader, int handle) {
        super(loader, "Wsock32.dll", handle);
        add(Wsock32.class, "gethostbyname", new String[] {"(STRING)name", "(HEX)result"}, 52);
        add(Wsock32.class, "gethostname", new String[] {"(HEX)name", "namelen", "result", "00(STRING)name"}, 57);
        add(Wsock32.class, "WSACleanup", new String[0], 115);
        add(Wsock32.class, "WSAStartup", new String[] {"wVersionRequested", "(HEX)lpWSAData"}, 116);
    }

    // struct hostent* FAR gethostbyname(const char *name)
    public static int gethostbyname(int name) {
        Win.panic("gethostbyname not implemented yet");
        return 0;
    }

    // int gethostname(char *name, int namelen)
    public static int gethostname(int name, int namelen) {
        Win.panic("gethostname not implemented yet");
        return 0;
    }

    // int WSACleanup(void);
    public static int WSACleanup() {
        return 0;
    }

    // int WSAStartup(WORD wVersionRequested, LPWSADATA lpWSAData)
    public static int WSAStartup(int wVersionRequested, int lpWSAData) {
        if (LOBYTE(wVersionRequested) < 1)
        return WSAVERNOTSUPPORTED;

        if (lpWSAData==0) return WSAEINVAL;

        writew(lpWSAData, wVersionRequested); lpWSAData+=2;
        writew(lpWSAData, 0x0202); lpWSAData+=2;
        StringUtil.strcpy(lpWSAData, "WinSock 2.0"); lpWSAData+=4;
        StringUtil.strcpy(lpWSAData, "Running"); lpWSAData+=4;
        writew(lpWSAData, 128); lpWSAData+=2;
        writew(lpWSAData, 1024); lpWSAData+=2;
        return 0;
    }
    
    /*
     * All Windows Sockets error constants are biased by WSABASEERR from
     * the "normal". They are also defined in winerror.h.
     */
    static final public int WSABASEERR =                10000;
    /*
     * Windows Sockets definitions of regular Microsoft C error constants
     */
    static final public int WSAEINTR =                  WSABASEERR+4;
    static final public int WSAEBADF =                  WSABASEERR+9;
    static final public int WSAEACCES =                 WSABASEERR+13;
    static final public int WSAEFAULT =                 WSABASEERR+14;
    static final public int WSAEINVAL =                 WSABASEERR+22;
    static final public int WSAEMFILE =                 WSABASEERR+24;
    
    /*
     * Windows Sockets definitions of regular Berkeley error constants
     */
    static final public int WSAEWOULDBLOCK =            WSABASEERR+35;
    static final public int WSAEINPROGRESS =            WSABASEERR+36;
    static final public int WSAEALREADY =               WSABASEERR+37;
    static final public int WSAENOTSOCK =               WSABASEERR+38;
    static final public int WSAEDESTADDRREQ =           WSABASEERR+39;
    static final public int WSAEMSGSIZE =               WSABASEERR+40;
    static final public int WSAEPROTOTYPE =             WSABASEERR+41;
    static final public int WSAENOPROTOOPT =            WSABASEERR+42;
    static final public int WSAEPROTONOSUPPORT =        WSABASEERR+43;
    static final public int WSAESOCKTNOSUPPORT =        WSABASEERR+44;
    static final public int WSAEOPNOTSUPP =             WSABASEERR+45;
    static final public int WSAEPFNOSUPPORT =           WSABASEERR+46;
    static final public int WSAEAFNOSUPPORT =           WSABASEERR+47;
    static final public int WSAEADDRINUSE =             WSABASEERR+48;
    static final public int WSAEADDRNOTAVAIL =          WSABASEERR+49;
    static final public int WSAENETDOWN =               WSABASEERR+50;
    static final public int WSAENETUNREACH =            WSABASEERR+51;
    static final public int WSAENETRESET =              WSABASEERR+52;
    static final public int WSAECONNABORTED =           WSABASEERR+53;
    static final public int WSAECONNRESET =             WSABASEERR+54;
    static final public int WSAENOBUFS =                WSABASEERR+55;
    static final public int WSAEISCONN =                WSABASEERR+56;
    static final public int WSAENOTCONN =               WSABASEERR+57;
    static final public int WSAESHUTDOWN =              WSABASEERR+58;
    static final public int WSAETOOMANYREFS =           WSABASEERR+59;
    static final public int WSAETIMEDOUT =              WSABASEERR+60;
    static final public int WSAECONNREFUSED =           WSABASEERR+61;
    static final public int WSAELOOP =                  WSABASEERR+62;
    static final public int WSAENAMETOOLONG =           WSABASEERR+63;
    static final public int WSAEHOSTDOWN =              WSABASEERR+64;
    static final public int WSAEHOSTUNREACH =           WSABASEERR+65;
    static final public int WSAENOTEMPTY =              WSABASEERR+66;
    static final public int WSAEPROCLIM =               WSABASEERR+67;
    static final public int WSAEUSERS =                 WSABASEERR+68;
    static final public int WSAEDQUOT =                 WSABASEERR+69;
    static final public int WSAESTALE =                 WSABASEERR+70;
    static final public int WSAEREMOTE =                WSABASEERR+71;
    
    /*
     * Extended Windows Sockets error constant definitions
     */
    static final public int WSASYSNOTREADY =            WSABASEERR+91;
    static final public int WSAVERNOTSUPPORTED =        WSABASEERR+92;
    static final public int WSANOTINITIALISED =         WSABASEERR+93;
    static final public int WSAEDISCON =                WSABASEERR+101;
    static final public int WSAENOMORE =                WSABASEERR+102;
    static final public int WSAECANCELLED =             WSABASEERR+103;
    static final public int WSAEINVALIDPROCTABLE =      WSABASEERR+104;
    static final public int WSAEINVALIDPROVIDER =       WSABASEERR+105;
    static final public int WSAEPROVIDERFAILEDINIT =    WSABASEERR+106;
    static final public int WSASYSCALLFAILURE =         WSABASEERR+107;
    static final public int WSASERVICE_NOT_FOUND =      WSABASEERR+108;
    static final public int WSATYPE_NOT_FOUND =         WSABASEERR+109;
    static final public int WSA_E_NO_MORE =             WSABASEERR+110;
    static final public int WSA_E_CANCELLED =           WSABASEERR+111;
    static final public int WSAEREFUSED =               WSABASEERR+112;
}
