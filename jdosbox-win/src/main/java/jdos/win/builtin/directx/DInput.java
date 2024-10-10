package jdos.win.builtin.directx;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.directx.dinput.IDirectInput;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class DInput  extends BuiltinModule {
    public DInput(Loader loader, int handle) {
        super(loader, "DInput.dll", handle);
        add(DirectInputCreateA);
    }

    // HRESULT WINAPI DirectInputCreateA(HINSTANCE hinst, DWORD dwVersion, LPDIRECTINPUTA *ppDI, LPUNKNOWN punkOuter);
    private Callback.Handler DirectInputCreateA = new HandlerBase() {
        public String getName() {
            return "DInput.DirectInputCreateA";
        }
        public void onCall() {
            int hinst = CPU.CPU_Pop32();
            int dwVersion = CPU.CPU_Pop32();
            int ppDI = CPU.CPU_Pop32();
            int punkOuter = CPU.CPU_Pop32();
            Memory.mem_writed(ppDI, IDirectInput.create());
            CPU_Regs.reg_eax.dword = jdos.win.utils.Error.S_OK;
        }
    };
}
