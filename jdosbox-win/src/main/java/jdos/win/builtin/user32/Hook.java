package jdos.win.builtin.user32;

import jdos.cpu.CPU_Regs;
import jdos.win.Win;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.system.StaticData;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;

import java.util.Vector;

public class Hook extends WinObject {
    static public Hook create(int type, int threadId, int eip) {
        return new Hook(nextObjectId(), type, threadId, eip);
    }

    static public Hook get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof Hook))
            return null;
        return (Hook)object;
    }

    // LRESULT WINAPI CallNextHookEx(HHOOK hhk, int nCode, WPARAM wParam, LPARAM lParam)
    static public int CallNextHookEx(int hhk, int nCode, int wParam, int lParam) {
        if (StaticData.currentHookIndex+1<StaticData.currentHookChain.size()) {
            StaticData.currentHookIndex++;
            Hook hook = StaticData.currentHookChain.elementAt(StaticData.currentHookIndex);
            WinSystem.call(hook.eip, nCode, wParam, lParam);
            return CPU_Regs.reg_eax.dword;
        }
        return 0;
    }

    // HHOOK WINAPI SetWindowsHookEx(int idHook, HOOKPROC lpfn, HINSTANCE hMod, DWORD dwThreadId)
    static public int SetWindowsHookExA(int idHook, int lpfn, int hMod, int dwThreadId) {
        if (lpfn == 0) {
            SetLastError(Error.ERROR_INVALID_FILTER_PROC);
            return 0;
        }
        if (dwThreadId != 0) {
            if (idHook == WH_JOURNALRECORD || idHook == WH_JOURNALPLAYBACK || idHook == WH_KEYBOARD_LL || idHook == WH_MOUSE_LL || idHook == WH_SYSMSGFILTER) {
                /* these can only be global */
                SetLastError(ERROR_INVALID_PARAMETER);
                return 0;
            }
        } else {
            /* system-global hook */
            if (dwThreadId == WH_KEYBOARD_LL || dwThreadId == WH_MOUSE_LL) hMod = 0;
            else if (hMod==0) {
                SetLastError(ERROR_HOOK_NEEDS_HMOD);
                return 0;
            }
        }
        if (idHook < WH_MIN || idHook > WH_MAX) {
            SetLastError(ERROR_INVALID_PARAMETER);
            return 0;
        }
        WinThread thread = null;
        if (dwThreadId != 0) {
            thread = WinThread.get(dwThreadId);
            if (thread == null) {
                SetLastError(ERROR_INVALID_PARAMETER);
                return 0;
            }
        }
        if (dwThreadId == 0 || hMod !=0) {
            Win.panic("Kernel32.SetWindowsHookExA not implemented yet for other processes");
        }
        if (idHook == WH_KEYBOARD_LL || idHook == WH_MOUSE_LL) {
            Win.panic("Kernel32.SetWindowsHookExA does not support WH_KEYBOARD_LL or WH_MOUSE_LL yet");
        }
        Hook hook = create(idHook, dwThreadId, lpfn);
        Vector<Hook> hooks = StaticData.hooks.get(idHook);
        if (hooks == null) {
            hooks = new Vector<Hook>();
            StaticData.hooks.put(idHook, hooks);
        }
        hooks.add(hook);
        return hook.handle;
    }

    public Hook(int id, int type, int threadId, int eip) {
        super(id);
        this.type = type;
        this.threadId = threadId;
        this.eip = eip;
    }

    public int type;
    public int threadId;
    public int eip;

    static public int HOOK_CallHooks(int id, int code, int wparam, int lparam) {
        Vector hooks = StaticData.hooks.get(id);
        if (hooks != null && hooks.size()>0) {
            Hook hook = (Hook)hooks.elementAt(0);
            StaticData.currentHookChain = hooks;
            StaticData.currentHookIndex = 0;
            WinSystem.call(hook.eip, code, wparam, lparam);
            StaticData.currentHookChain = null;
            return CPU_Regs.reg_eax.dword;
        }
        return 0;
    }
}
