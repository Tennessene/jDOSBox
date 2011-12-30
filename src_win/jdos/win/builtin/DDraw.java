package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class DDraw extends BuiltinModule {
    public DDraw(Loader loader, int handle) {
        super(loader, "DDraw.dll", handle);
        add(DirectDrawCreate);
    }

    // HRESULT WINAPI DirectDrawCreate(GUID FAR* lpGUID, LPDIRECTDRAW FAR* lplpDD, IUnknown FAR* pUnkOuter)
    private Callback.Handler DirectDrawCreate = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.DirectDrawCreate";
        }
        public void onCall() {
            int lpGUID = CPU.CPU_Pop32();
            int lplpDD = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
