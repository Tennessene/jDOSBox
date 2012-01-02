package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.Console;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Gdi32 extends BuiltinModule {
    public Gdi32(Loader loader, int handle) {
        super(loader, "Gdi32.dll", handle);

        add(GdiSetBatchLimit);
        add(GetStockObject);
    }

    // DWORD GdiSetBatchLimit(DWORD dwLimit)
    private Callback.Handler GdiSetBatchLimit = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GdiSetBatchLimit";
        }
        public void onCall() {
            int dwLimit = CPU.CPU_Pop32();
            System.out.println("Faking "+getName());
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // HGDIOBJ GetStockObject(int fnObject)
    private Callback.Handler GetStockObject = new HandlerBase() {
        public java.lang.String getName() {
            return "Gdi32.GetStockObject";
        }
        public void onCall() {
            int fnObject = CPU.CPU_Pop32();
            switch (fnObject) {
                case 0: // WHITE_BRUSH
                case 1: // LTGRAY_BRUSH
                case 2: // GRAY_BRUSH
                case 3: // DKGRAY_BRUSH
                case 4: // BLACK_BRUSH
                case 5: // NULL_BRUSH
                case 6: // WHITE_PEN
                case 7: // BLACK_PEN
                case 8: // NULL_PEN
                case 10: // OEM_FIXED_FONT
                case 11: // ANSI_FIXED_FONT
                case 12: // ANSI_VAR_FONT
                case 13: // SYSTEM_FONT
                case 14: // DEVICE_DEFAULT_FONT
                case 15: // DEFAULT_PALETTE
                case 16: // SYSTEM_FIXED_FONT
                    Console.out("GetStockObject faked");
                    break;
                default:
                    Console.out("Unknown GetStockObject "+fnObject);
                    notImplemented();
            }
            CPU_Regs.reg_eax.dword = 1;
        }
    };
}
