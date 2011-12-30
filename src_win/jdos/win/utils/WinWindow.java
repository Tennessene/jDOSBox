package jdos.win.utils;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;

public class WinWindow extends WinObject {
    static public final int WM_CREATE =                      0x0001;
    static public final int WM_DESTROY =                     0x0002;
    static public final int WM_MOVE =                        0x0003;
    static public final int WM_SIZE =                        0x0005;
    static public final int WM_ACTIVATE =                    0x0006;
    static public final int WM_SETFOCUS =                    0x0007;
    static public final int WM_KILLFOCUS =                   0x0008;
    static public final int WM_ENABLE =                      0x000A;
    static public final int WM_SETREDRAW =                   0x000B;
    static public final int WM_SETTEXT =                     0x000C;
    static public final int WM_GETTEXT =                     0x000D;
    static public final int WM_GETTEXTLENGTH =               0x000E;
    static public final int WM_PAINT =                       0x000F;
    static public final int WM_CLOSE =                       0x0010;

    public WinWindow(int id, int dwExStyle, WinClass winClass, String name, int dwStyle, int x, int y, int cx, int cy, int hParent, int hMenu, int hInstance, int lpParam) {
        super(id);
        this.winClass = winClass;
    }

    public int sendMessage(int msg, int wParam, int lParam) {
        CPU.CPU_Push32(lParam);
        CPU.CPU_Push32(wParam);
        CPU.CPU_Push32(msg);
        CPU.CPU_Push32(handle);
        CPU.CPU_Push32(winClass.returnEip);
        int eip = CPU_Regs.reg_eip;
        CPU_Regs.reg_eip = winClass.eip;
        Dosbox.DOSBOX_RunMachine();
        CPU_Regs.reg_eip = eip;
        return CPU_Regs.reg_eax.dword;
    }

    public int defWindowProc(int msg, int wParam, int lParam) {
        return 0;
    }

    private WinClass winClass;
}
