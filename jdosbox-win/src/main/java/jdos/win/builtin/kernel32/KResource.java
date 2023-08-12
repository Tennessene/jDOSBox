package jdos.win.builtin.kernel32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;
import jdos.win.system.WinSystem;

public class KResource extends WinAPI {
    // BOOL WINAPI FreeResource(HGLOBAL hglbResource)
    static public int FreeResource(int hglbResource) {
        return TRUE;
    }

    // HRSRC WINAPI FindResource(HMODULE hModule, LPCTSTR lpName, LPCTSTR lpType)
    static public int FindResourceA(int hModule, int lpName, int lpType) {
        if (hModule == 0)
            hModule = WinSystem.getCurrentProcess().mainModule.getHandle();
        Module m = WinSystem.getCurrentProcess().loader.getModuleByHandle(hModule);
        if (m instanceof NativeModule) {
            NativeModule module = (NativeModule)m;
            return module.getAddressOfResource(lpType, lpName);
        } else {
            Win.panic("FindResourceA currently does not support loading a resource from a builtin module");
        }
        return 0;
    }

    // HGLOBAL WINAPI LoadResource(HMODULE hModule, HRSRC hResInfo)
    static public int LoadResource(int hModule, int hResInfo) {
        // Find resource just returns the address of it in memory
        return hResInfo;
    }

    // LPVOID WINAPI LockResource(HGLOBAL hResData)
    static public int LockResource(int hResData) {
        // Find/Load resource just returns the address of it in memory
        return hResData;
    }
}
