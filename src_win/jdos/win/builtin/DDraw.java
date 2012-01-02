package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.ddraw.IDirectDraw;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.Error;

public class DDraw extends BuiltinModule {
    public DDraw(Loader loader, int handle) {
        super(loader, "DDraw.dll", handle);
        add(DirectDrawCreate);
    }

    // HRESULT WINAPI DirectDrawCreate(GUID FAR* lpGUID, LPDIRECTDRAW FAR* lplpDD, IUnknown FAR* pUnkOuter)
    private Callback.Handler DirectDrawCreate = new HandlerBase() {
        public String getName() {
            return "DDraw.DirectDrawCreate";
        }
        public void onCall() {
            int lpGUID = CPU.CPU_Pop32();
            int lplpDD = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            Memory.mem_writed(lplpDD, IDirectDraw.create());
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };
}
