package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.StringUtil;

public class User32 extends BuiltinModule {
    public User32(int handle) {
        super("user32.dll", handle);
        add(wsprintfA);
    }

    // int __cdecl wsprintf(LPTSTR lpOut, LPCTSTR lpFmt, ...)
    private Callback.Handler wsprintfA = new HandlerBase() {
        public java.lang.String getName() {
            return "User32.wsprintfA";
        }
        public void onCall() {
            int out = CPU.CPU_Peek32(0);
            int in = CPU.CPU_Peek32(1);
            String format = new LittleEndianFile(in).readCString();
            String result = StringUtil.format(format, false, 2);
            StringUtil.strcpy(out, result);
            CPU_Regs.reg_eax.dword = result.length();
        }
    };
}
