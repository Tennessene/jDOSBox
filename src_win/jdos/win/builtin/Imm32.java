package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.WinObject;
import jdos.win.utils.WinSystem;
import jdos.win.utils.WinWindow;

public class Imm32 extends BuiltinModule {
    public Imm32(Loader loader, int handle) {
        super(loader, "Imm32.dll", handle);
        add(ImmAssociateContext);
    }

    // HIMC ImmAssociateContext(HWND hWnd, HIMC hIMC)
    private Callback.Handler ImmAssociateContext = new HandlerBase() {
        public java.lang.String getName() {
            return "Imm32.ImmAssociateContext";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int hIMC = CPU.CPU_Pop32();
            WinObject object = WinSystem.getObject(hWnd);
            if (object == null || !(object instanceof WinWindow)) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                WinSystem.getCurrentThread().setLastError(jdos.win.utils.Error.ERROR_INVALID_WINDOW_HANDLE);
            } else {
                System.out.println(getName()+" faked");
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };
}