package jdos.win.builtin.dsound;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.ddraw.IUnknown;
import jdos.win.utils.Error;

public class IDirectSoundBuffer extends IUnknown {
    static final int VTABLE_SIZE = 18;

    final static int DSBSIZE_MIN = 4;
    final static int DSBSIZE_MAX = 0xFFFFFFF;

    final static int OFFSET_FLAGS = 0;
    final static int OFFSET_DESC = 4;

    static final int DATA_SIZE = OFFSET_DESC;

    private static int createVTable() {
        int address = allocateVTable("IDirectSoundBuffer", VTABLE_SIZE);
        addIDirectSound(address);
        return address;
    }

    static int addIDirectSound(int address) {
        address = addIUnknown(address);
        address = add(address, GetCaps);
        address = add(address, GetCurrentPosition);
        address = add(address, GetFormat);
        address = add(address, GetVolume);
        address = add(address, GetPan);
        address = add(address, GetFrequency);
        address = add(address, GetStatus);
        address = add(address, Initialize);
        address = add(address, Lock);
        address = add(address, Play);
        address = add(address, SetCurrentPosition);
        address = add(address, SetFormat);
        address = add(address, SetVolume);
        address = add(address, SetPan);
        address = add(address, SetFrequency);
        address = add(address, Stop);
        address = add(address, Unlock);
        address = add(address, Restore);
        return address;
    }

    public static int create(int lplpDirectSoundBuffer, int lpcDSBufferDesc) {
        return create("IDirectSoundBuffer", lplpDirectSoundBuffer, lpcDSBufferDesc, 0);
    }

    public static int create(String name, int lplpDirectSoundBuffer, int lpcDSBufferDesc, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0)
            vtable = createVTable();
        DSBufferDesc desc = new DSBufferDesc(lpcDSBufferDesc);
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER)==0 && (desc.dwBufferBytes<DSBSIZE_MIN || desc.dwBufferBytes>DSBSIZE_MAX)) {
            return Error.DDERR_INVALIDPARAMS;
        }
        int address = allocate(vtable, DATA_SIZE+DSBufferDesc.SIZE, 0);
        setData(address, OFFSET_FLAGS, flags);
        Memory.mem_memcpy(address+OFFSET_DATA_START+OFFSET_DESC, lpcDSBufferDesc, DSBufferDesc.SIZE);
        Memory.mem_writed(lplpDirectSoundBuffer, address);
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER)!=0) {
            setData(address, OFFSET_DESC + 8, 256 * 1024); // :TODO: is 256kb a good audio buffer size?
        }
        return Error.S_OK;
    }

    static public int getFlags(int This) {
        return getData(This, OFFSET_DESC+4);
    }
    static public int getBufferBytes(int This) {
        return getData(This, OFFSET_DESC+8);
    }

    // HRESULT GetCaps(this, LPDSBCAPS lpDSBufferCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDSBufferCaps = CPU.CPU_Pop32();
            if (lpDSBufferCaps == 0 || Memory.mem_readd(lpDSBufferCaps)<DSBCaps.SIZE) {
                CPU_Regs.reg_eax.dword = Error.DDERR_INVALIDPARAMS;
                return;
            }
            DSBCaps.write(lpDSBufferCaps,  getFlags(This) | DSBufferDesc.DSBCAPS_LOCSOFTWARE, getBufferBytes(This), 0, 0);
        }
    };

    // HRESULT GetCurrentPosition(this, LPDWORD lpdwCurrentPlayCursor, LPDWORD lpdwCurrentWriteCursor)
    static private Callback.Handler GetCurrentPosition = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetCurrentPosition";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwCurrentPlayCursor = CPU.CPU_Pop32();
            int lpdwCurrentWriteCursor = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetFormat(this, LPWAVEFORMATEX lpwfxFormat, DWORD dwSizeAllocated, LPDWORD lpdwSizeWritten)
    static private Callback.Handler GetFormat = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetFormat";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpwfxFormat = CPU.CPU_Pop32();
            int dwSizeAllocated = CPU.CPU_Pop32();
            int lpdwSizeWritten = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetVolume(this, LPLONG lplVolume)
    static private Callback.Handler GetVolume = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetVolume";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplVolume = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetPan(this, LPLONG lplpan)
    static private Callback.Handler GetPan = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetPan";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpan = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetFrequency(this, LPDWORD lpdwFrequency)
    static private Callback.Handler GetFrequency = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetFrequency";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwFrequency = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetStatus(this, LPDWORD lpdwStatus)
    static private Callback.Handler GetStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwStatus = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, LPDIRECTSOUND lpDirectSound, LPCDSBUFFERDESC lpcDSBufferDesc)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDirectSound = CPU.CPU_Pop32();
            int lpcDSBufferDesc = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Lock(this, DWORD dwOffset, DWORD dwBytes, LPVOID *ppvAudioPtr1, LPDWORD pdwAudioBytes1, LPVOID *ppvAudioPtr2, LPDWORD pdwAudioBytes2, DWORD dwFlags)
    static private Callback.Handler Lock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Lock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwOffset = CPU.CPU_Pop32();
            int dwBytes = CPU.CPU_Pop32();
            int ppvAudioPtr1 = CPU.CPU_Pop32();
            int pdwAudioBytes1 = CPU.CPU_Pop32();
            int ppvAudioPtr2 = CPU.CPU_Pop32();
            int pdwAudioBytes2 = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Play(this, DWORD dwReserved1, DWORD dwReserved2, DWORD dwFlags)
    static private Callback.Handler Play = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Play";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwReserved1 = CPU.CPU_Pop32();
            int dwReserved2 = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetCurrentPosition(this, DWORD dwNewPosition)
    static private Callback.Handler SetCurrentPosition = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetCurrentPosition";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwNewPosition = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetFormat(this, LPCWAVEFORMATEX lpcfxFormat)
    static private Callback.Handler SetFormat = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetFormat";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpcfxFormat = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetVolume(this, LONG lVolume)
    static private Callback.Handler SetVolume = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetVolume";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lVolume = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetPan(this, LONG lPan)
    static private Callback.Handler SetPan = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetPan";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lPan = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetFrequency(this, DWORD dwFrequency)
    static private Callback.Handler SetFrequency = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetFrequency";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFrequency = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Stop(this)
    static private Callback.Handler Stop = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Stop";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Unlock(this, LPVOID pvAudioPtr1, DWORD dwAudioBytes1, LPVOID pvAudioPtr2, DWORD dwAudioPtr2)
    static private Callback.Handler Unlock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Unlock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int pvAudioPtr1 = CPU.CPU_Pop32();
            int dwAudioBytes1 = CPU.CPU_Pop32();
            int pvAudioPtr2 = CPU.CPU_Pop32();
            int dwAudioPtr2 = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Restore(this)
    static private Callback.Handler Restore = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Restore";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
