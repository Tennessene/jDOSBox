package jdos.win.builtin.ddraw;

public class IDirectDraw {
    /*** IUnknown methods ***/
    // HRESULT QueryInterface(this, REFIID riid, void** ppvObject)
    // ULONG AddRef(this)
    // ULONG Release(this)
    /*** IDirectDraw methods ***/
    // HRESULT Compact(this)
    // HRESULT CreateClipper(this, DWORD dwFlags, LPDIRECTDRAWCLIPPER *lplpDDClipper, IUnknown *pUnkOuter)
    // HRESULT CreatePalette(this, DWORD dwFlags, LPPALETTEENTRY lpColorTable, LPDIRECTDRAWPALETTE *lplpDDPalette, IUnknown *pUnkOuter)
    // HRESULT CreateSurface(this, LPDDSURFACEDESC lpDDSurfaceDesc, LPDIRECTDRAWSURFACE *lplpDDSurface, IUnknown *pUnkOuter)
    // HRESULT DuplicateSurface(this, LPDIRECTDRAWSURFACE lpDDSurface, LPDIRECTDRAWSURFACE *lplpDupDDSurface)
    // HRESULT EnumDisplayModes(this, DWORD dwFlags, LPDDSURFACEDESC lpDDSurfaceDesc, LPVOID lpContext, LPDDENUMMODESCALLBACK lpEnumModesCallback)
    // HRESULT EnumSurfaces(this, DWORD dwFlags, LPDDSURFACEDESC lpDDSD, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpEnumSurfacesCallback)
    // HRESULT FlipToGDISurface(this)
    // HRESULT GetCaps(this, LPDDCAPS lpDDDriverCaps, LPDDCAPS lpDDHELCaps)
    // HRESULT GetDisplayMode(this, LPDDSURFACEDESC lpDDSurfaceDesc)
    // HRESULT GetFourCCCodes(this, LPDWORD lpNumCodes, LPDWORD lpCodes)
    // HRESULT GetGDISurface(this, LPDIRECTDRAWSURFACE *lplpGDIDDSurface)
    // HRESULT GetMonitorFrequency)(this, LPDWORD lpdwFrequency)
    // HRESULT GetScanLine(this, LPDWORD lpdwScanLine)
    // HRESULT GetVerticalBlankStatus(this, BOOL *lpbIsInVB)
    // HRESULT Initialize(this, GUID *lpGUID)
    // HRESULT RestoreDisplayMode(this)
    // HRESULT SetCooperativeLevel(this, HWND hWnd, DWORD dwFlags)
    // HRESULT SetDisplayMode(this, DWORD dwWidth, DWORD dwHeight, DWORD dwBPP)
    // HRESULT WaitForVerticalBlank(this, DWORD dwFlags, HANDLE hEvent)
}
