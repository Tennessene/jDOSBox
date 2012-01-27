package jdos.win.builtin.directx;

import jdos.hardware.Memory;
import jdos.win.builtin.directx.ddraw.IDirectDraw;
import jdos.win.builtin.directx.ddraw.IDirectDraw7;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.utils.Error;

public class DDraw extends BuiltinModule {
    public DDraw(Loader loader, int handle) {
        super(loader, "DDraw.dll", handle);
        add(DDraw.class, "DirectDrawCreate", new String[]{"(GUID)lpGUID", "lplpDD", "pUnkOuter", "(HRESULT)result"});
        add(DDraw.class, "DirectDrawCreateEx", new String[] {"(GUID)lpGUID", "lplpDD", "(GUID)iid", "pUnkOuter", "(HRESULT)result"});
    }

    // HRESULT WINAPI DirectDrawCreate(GUID FAR* lpGUID, LPDIRECTDRAW FAR* lplpDD, IUnknown FAR* pUnkOuter)
    public static int DirectDrawCreate(int lpGUID, int lplpDD, int pUnkOuter) {
        Memory.mem_writed(lplpDD, IDirectDraw.create());
        return Error.S_OK;
    }

    // HRESULT WINAPI DirectDrawCreateEx(GUID FAR *lpGUID, LPVOID *lplpDD, REFIID iid, IUnknown FAR *pUnkOuter)
    public static int DirectDrawCreateEx(int lpGUID, int lplpDD, int iid, int pUnkOuter) {
        Memory.mem_writed(lplpDD, IDirectDraw7.create());
        return Error.S_OK;
    }
}
