package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.fpu.FPU;
import jdos.hardware.Memory;
import jdos.util.MicroDouble;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

import java.util.Random;

public class Crtdll extends BuiltinModule {
    int _acmdln_dll;

    public Crtdll(Loader loader, int handle) {
        super(loader, "Crtdll.dll", handle);
        add(_CIpow);
        add(_ftol);
        add(__GetMainArgs);
        add(_initterm);
        add(rand);
        add(_strupr);
        add(toupper);
        _acmdln_dll = addData("_acmdln_dll", 4);
        Memory.mem_writed(_acmdln_dll, WinSystem.getCurrentProcess().getCommandLine());
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

    private Callback.Handler _ftol = new HandlerBase() {
        public java.lang.String getName() {
            return "Crtdll._ftol";
        }
        public void onCall() {
            // :TODO: is this right?
            long result = MicroDouble.longValue(FPU.fpu.regs[FPU.fpu.top].d);
            FPU.FPU_FPOP();
            CPU_Regs.reg_eax.dword = (int)result;
            CPU_Regs.reg_edx.dword = (int)(result >>> 32);
        }
    };

    // void __GetMainArgs(int * argc, char *** argv, char *** envp, int expand_wildcards)
    private Callback.Handler __GetMainArgs = new HandlerBase() {
        private int argv = 0;

        public java.lang.String getName() {
            return "Crtdll.__GetMainArgs";
        }
        public void onCall() {
            int argc = CPU.CPU_Peek32(0);
            int argv = CPU.CPU_Peek32(1);
            int envp = CPU.CPU_Peek32(2);
            int expand_wildcards = CPU.CPU_Peek32(3);
            Memory.mem_writed(argc, 1);
            if (this.argv == 0) {
                this.argv = WinSystem.getCurrentProcess().heap.alloc(4, false);
                Memory.mem_writed(this.argv, WinSystem.getCurrentProcess().getCommandLine());
            }
            Memory.mem_writed(argv, this.argv);
            Memory.mem_writed(envp, 0);
        }
    };

    // void __cdecl _initterm(PVFV *, PVFV *)
    private Callback.Handler _initterm = new HandlerBase() {
        public java.lang.String getName() {
            return "Crtdll._initterm";
        }
        public void onCall() {
            int start = CPU.CPU_Peek32(0);
            int end = CPU.CPU_Peek32(1);
            while (start<end) {
                int next = Memory.mem_readd(start);
                if (next != 0) {
                    System.out.println(getName()+" faked");
                }
                start+=4;
            }
        }
    };

    private static Random random = new Random();

    private Callback.Handler rand = new HandlerBase() {
        public java.lang.String getName() {
            return "Crtdll.rand";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = random.nextInt() & 0x7FFF;
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

    private Callback.Handler toupper = new HandlerBase() {
        public java.lang.String getName() {
            return "Crtdll.toupper";
        }
        public void onCall() {
            int c = CPU.CPU_Peek32(0);
            CPU_Regs.reg_eax.dword = (int)Character.toUpperCase((char)c);
        }
    };
}
