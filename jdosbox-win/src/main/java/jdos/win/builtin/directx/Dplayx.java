package jdos.win.builtin.directx;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Dplayx extends BuiltinModule {
    public Dplayx(Loader loader, int handle) {
        super(loader, "Dplayx.dll", handle);
        add(DirectPlayCreate, 1);
        add(DirectPlayEnumerateA, 2);
        add(DirectPlayEnumerateW, 3);
        add(DirectPlayLobbyCreateA, 4);
        add(DirectPlayLobbyCreateW, 5);
    }

    // HRESULT WINAPI DirectPlayCreate( LPGUID lpGUID, LPDIRECTPLAY *lplpDP, IUnknown *pUnkOuter )
    private Callback.Handler DirectPlayCreate = new HandlerBase() {
        public String getName() {
            return "Dplayx.DirectPlayCreate";
        }
        public void onCall() {
            int lpGUID = CPU.CPU_Pop32();
            int lplpDP = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT WINAPI DirectPlayEnumerateA(LPDPENUMDPCALLBACKA lpEnumCallback, LPVOID lpContext);
    private Callback.Handler DirectPlayEnumerateA = new HandlerBase() {
        public String getName() {
            return "Dplayx.DirectPlayEnumerateA";
        }
        public void onCall() {
            int lpEnumCallback = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT WINAPI DirectPlayEnumerateW(LPDPENUMDPCALLBACKW lpEnumCallback, LPVOID lpContext);
    private Callback.Handler DirectPlayEnumerateW = new HandlerBase() {
        public String getName() {
            return "Dplayx.DirectPlayEnumerateW";
        }
        public void onCall() {
            int lpEnumCallback = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT WINAPI DirectPlayLobbyCreateA(LPGUID lpGUIDDSP, LPDIRECTPLAYLOBBYA *lplpDPL, IUnknown *lpUnk, LPVOID lpData, DWORD dwDataSize)
    private Callback.Handler DirectPlayLobbyCreateA = new HandlerBase() {
        public String getName() {
            return "Dplayx.DirectPlayLobbyCreateA";
        }
        public void onCall() {
            int lpGUIDDSP = CPU.CPU_Pop32();
            int lplpDPL = CPU.CPU_Pop32();
            int lpUnk = CPU.CPU_Pop32();
            int lpData = CPU.CPU_Pop32();
            int dwDataSize = CPU.CPU_Pop32();
            //Memory.mem_writed(lplpDPL, IDirectPlayLobby.create());
            //CPU_Regs.reg_eax.dword = jdos.win.utils.Error.S_OK;
            CPU_Regs.reg_eax.dword = DError.DPERR_UNAVAILABLE;
        }
    };

    // HRESULT WINAPI DirectPlayLobbyCreateW( LPGUID lpGUIDDSP, LPDIRECTPLAYLOBBY *lplpDPL, IUnknown *lpUnk, LPVOID lpData, DWORD dwDataSize)
    private Callback.Handler DirectPlayLobbyCreateW = new HandlerBase() {
        public String getName() {
            return "Dplayx.DirectPlayLobbyCreateW";
        }
        public void onCall() {
            int lpGUIDDSP = CPU.CPU_Pop32();
            int lplpDPL = CPU.CPU_Pop32();
            int lpUnk = CPU.CPU_Pop32();
            int lpData = CPU.CPU_Pop32();
            int dwDataSize = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
