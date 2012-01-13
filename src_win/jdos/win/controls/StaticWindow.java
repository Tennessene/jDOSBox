package jdos.win.controls;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.builtin.HandlerBase;
import jdos.win.kernel.WinCallback;
import jdos.win.utils.WinClass;
import jdos.win.utils.WinProcess;
import jdos.win.utils.WinSystem;
import jdos.win.utils.WinWindow;

public class StaticWindow {
    static public void registerClass(WinProcess process) {
        WinClass winClass = WinSystem.createClass();
        winClass.className = "STATIC";
        int cb = WinCallback.addCallback(SendMessage);
        winClass.eip = process.loader.registerFunction(cb);
        process.classNames.put(winClass.className, winClass);
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
        WinWindow window = (WinWindow)WinSystem.getObject(hWnd);
        return window.defWindowProc(msg, wParam, lParam);
    }
}
