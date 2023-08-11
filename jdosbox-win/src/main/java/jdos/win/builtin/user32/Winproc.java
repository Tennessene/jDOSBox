package jdos.win.builtin.user32;

import jdos.cpu.CPU_Regs;
import jdos.win.system.WinSystem;

public class Winproc {
    // LRESULT WINAPI CallWindowProc(WNDPROC lpPrevWndFunc, HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
    static public int CallWindowProcA(int lpPrevWndFunc, int hWnd, int Msg, int wParam, int lParam) {
        /* Some window procedures modify register they shouldn't, or are not
        * properly declared stdcall; so we need a small assembly wrapper to
        * call them. */
        // :TODO: deal with this WINE comment, it looks like they push/pop ebx, esi, edi
        WinSystem.call(lpPrevWndFunc, hWnd, Msg, wParam, lParam);
        return CPU_Regs.reg_eax.dword;
    }
}
