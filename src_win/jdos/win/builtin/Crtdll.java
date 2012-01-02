package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.fpu.FPU;
import jdos.util.MicroDouble;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.StringUtil;

public class Crtdll extends BuiltinModule {
    public Crtdll(Loader loader, int handle) {
        super(loader, "Crtdll.dll", handle);
        add(_CIpow);
        add(_strupr);
    }

    // void __cdecl _CIpow()
    private Callback.Handler _CIpow = new HandlerBase() {
        public java.lang.String getName() {
            return "Crtdll._CIpow";
        }
        public void onCall() {
            // use fpu stack
            // pop y
            // pop x
            long y = FPU.fpu.regs[FPU.fpu.top].d;
            FPU.FPU_FPOP();
            long x = FPU.fpu.regs[FPU.fpu.top].d;
            FPU.fpu.regs[FPU.fpu.top].d = MicroDouble.pow(x, y);
            System.out.println("Crtdll._CIpow "+MicroDouble.toString(x)+"^"+MicroDouble.toString(y)+"="+MicroDouble.toString(FPU.fpu.regs[FPU.fpu.top].d));
        }
    };

    // char *_strupr(char *str)
    private Callback.Handler _strupr = new HandlerBase() {
        public java.lang.String getName() {
            return "Crtdll._strupr";
        }
        public void onCall() {
            int str = CPU.CPU_Peek32(0);
            StringUtil._strupr(str);
            CPU_Regs.reg_eax.dword = str;
        }
    };
}
