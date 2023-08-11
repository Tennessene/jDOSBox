package jdos.win.builtin;

import jdos.cpu.CPU_Regs;
import jdos.fpu.FPU;
import jdos.hardware.Memory;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

import java.util.Random;

public class Crtdll extends BuiltinModule {
    int _acmdln_dll;

    public Crtdll(Loader loader, int handle) {
        super(loader, "Crtdll.dll", handle);
        add_cdecl(Crtdll.class, "_CIpow", new String[0]);
        add_cdecl(Crtdll.class, "_ftol", new String[0]);
        add_cdecl(Crtdll.class, "__GetMainArgs", new String[] {"(HEX)argc", "(HEX)argv", "(HEX)envp", "expand_wildcards"});
        add_cdecl(Crtdll.class, "_initterm", new String[] {"(HEX)start", "(HEX)end"});
        add_cdecl(Crtdll.class, "rand", new String[0]);
        add_cdecl(Crtdll.class, "_strupr", new String[] {"(STRING)str", "(STRING)result"});
        add_cdecl(Crtdll.class, "toupper", new String[] {"c"});
        _acmdln_dll = addData("_acmdln_dll", 4);
        Memory.mem_writed(_acmdln_dll, WinSystem.getCurrentProcess().getCommandLine());
    }

    // void __cdecl _CIpow()
    public static void _CIpow() {
        // use fpu stack
        // pop y
        // pop x
        double y = FPU.regs[FPU.top];
        FPU.FPU_FPOP();
        double x = FPU.regs[FPU.top];
        FPU.regs[FPU.top] = Math.pow(x, y);
        if (LOG)
            log(x+"^"+y+"="+ FPU.regs[FPU.top]);
    }

    public static void _ftol() {
        // :TODO: is this right?
        long result = (long) FPU.regs[FPU.top];
        if (LOG)
            log(FPU.regs[FPU.top]+" -> "+result);
        FPU.FPU_FPOP();
        CPU_Regs.reg_eax.dword = (int)result;
        CPU_Regs.reg_edx.dword = (int)(result >>> 32);
    }

    // void __GetMainArgs(int * argc, char *** argv, char *** envp, int expand_wildcards)
    public static void __GetMainArgs(int argc, int argv, int envp, int expand_wildcards) {
        Memory.mem_writed(argc, 1);
        int tmp_argv = WinSystem.getCurrentProcess().heap.alloc(4, false);  // :TODO: this will leak
        Memory.mem_writed(tmp_argv, WinSystem.getCurrentProcess().getCommandLine());
        Memory.mem_writed(argv, tmp_argv);
        Memory.mem_writed(envp, 0);
    }

    // void __cdecl _initterm(PVFV *, PVFV *)
    public static void _initterm(int start, int end) {
        while (start<end) {
            int next = Memory.mem_readd(start);
            if (next != 0) {
                System.out.println("Crtdll._initterm faked");
            }
            start+=4;
        }
    }

    private static Random random = new Random();

    public static int rand() {
        return random.nextInt() & 0x7FFF;
    }

    // char *_strupr(char *str)
    public static int _strupr(int str) {
        StringUtil._strupr(str);
        return str;
    }

    public static int toupper(int c) {
        return (int)Character.toUpperCase((char)c);
    }
}
