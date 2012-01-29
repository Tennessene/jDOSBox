package jdos.win.controls;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.user32.DefWnd;
import jdos.win.builtin.user32.WinClass;
import jdos.win.kernel.WinCallback;
import jdos.win.system.WinProcess;

public class StaticWindow extends WinAPI {
    static public void registerClass(WinProcess process) {
        WinClass winClass = WinClass.create();
        winClass.className = "STATIC";
        int cb = WinCallback.addCallback(SendMessage);
        winClass.eip = process.loader.registerFunction(cb);
        process.classNames.put(winClass.className.toLowerCase(), winClass);
    }

    // LRESULT WINAPI SendMessage(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    static private Callback.Handler SendMessage = new HandlerBase() {
        public java.lang.String getName() {
            return "STATIC.sendMessage";
        }
        public void onCall() {
            int hWnd = CPU.CPU_Pop32();
            int Msg = CPU.CPU_Pop32();
            int wParam = CPU.CPU_Pop32();
            int lParam = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = defWindowProc(hWnd, Msg, wParam, lParam);
        }
    };

    static public int defWindowProc(int hWnd, int msg, int wParam, int lParam) {
        return DefWnd.DefWindowProcA(hWnd, msg, wParam, lParam);
    }
}
