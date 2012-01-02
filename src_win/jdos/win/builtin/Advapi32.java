package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.Error;

public class Advapi32 extends BuiltinModule {
    public Advapi32(Loader loader, int handle) {
        super(loader, "advapi32.dll", handle);
        add(RegOpenKeyExA);
    }

    // LONG WINAPI RegOpenKeyEx(HKEY hKey, LPCTSTR lpSubKey, DWORD ulOptions, REGSAM samDesired, PHKEY phkResult)
    private Callback.Handler RegOpenKeyExA = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.RegOpenKeyExA";
        }
        public void onCall() {
            int hKey = CPU.CPU_Pop32();
            int lpSubKey = CPU.CPU_Pop32();
            int ulOptions = CPU.CPU_Pop32();
            int samDesired = CPU.CPU_Pop32();
            int phkResult = CPU.CPU_Pop32();
            String name = new LittleEndianFile(lpSubKey).readCString();
            System.out.println("Registry not supported yet.  Tried to open "+name);
            CPU_Regs.reg_eax.dword = Error.ERROR_BAD_PATHNAME;
        }
    };
}
