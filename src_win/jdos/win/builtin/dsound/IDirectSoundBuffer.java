package jdos.win.builtin.dsound;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.ddraw.IUnknown;
import jdos.win.kernel.WinCallback;
import jdos.win.utils.Error;
import jdos.win.utils.WinSystem;

public class IDirectSoundBuffer extends IUnknown {
    static final int VTABLE_SIZE = 18;

    final static int DSBSIZE_MIN = 4;
    final static int DSBSIZE_MAX = 0xFFFFFFF;

    final static int OFFSET_FLAGS = 0;
    final static int OFFSET_MEMORY = 4;
    final static int OFFSET_START_POS = 8;
    final static int OFFSET_END_POS = 12;
    final static int OFFSET_DESC = 16;

    final static int OFFSET_DESC_WAV = OFFSET_DESC+16;
    static final int DATA_SIZE = OFFSET_DESC;

    final static int MEMORY_HEADER_SIZE = 4;
    final static int MEMORY_OFFSET_REF_COUNT = 0;

    static int getBits(int This) {
        return getData(This, OFFSET_MEMORY)+MEMORY_HEADER_SIZE;
    }

    static int getSize(int This) {
        return getData(This, OFFSET_DESC+8);
    }

    static int getStart(int This) {
        return getData(This, OFFSET_START_POS);
    }

    static int getEnd(int This) {
        return getData(This, OFFSET_END_POS);
    }

    static void incrementMemoryRef(int This) {
        int refAddress = getData(This, OFFSET_MEMORY)+MEMORY_OFFSET_REF_COUNT;
        int ref = Memory.mem_readd(refAddress);
        ref++;
        Memory.mem_writed(refAddress, ref);
    }

    static int decrementMemoryRef(int This) {
        int refAddress = getData(This, OFFSET_MEMORY)+MEMORY_OFFSET_REF_COUNT;
        int ref = Memory.mem_readd(refAddress);
        ref--;
        Memory.mem_writed(refAddress, ref);
        return ref;
    }

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

    static private Callback.Handler CleanUp = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.CleanUp";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int refCount = decrementMemoryRef(This);
            if (refCount == 0) {
                int memory = getData(This, OFFSET_MEMORY);
                WinSystem.getCurrentProcess().heap.free(memory);
            }
        }
    };

    public static int cleanupCallback;

    public static int create(int lplpDirectSoundBuffer, int lpcDSBufferDesc) {
        return create("IDirectSoundBuffer", lplpDirectSoundBuffer, lpcDSBufferDesc, 0);
    }

    public static int create(String name, int lplpDirectSoundBuffer, int lpcDSBufferDesc, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0) {
            vtable = createVTable();
            cleanupCallback = WinCallback.addCallback(CleanUp);
        }
        DSBufferDesc desc = new DSBufferDesc(lpcDSBufferDesc);
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER)==0 && (desc.dwBufferBytes<DSBSIZE_MIN || desc.dwBufferBytes>DSBSIZE_MAX)) {
            return Error.DDERR_INVALIDPARAMS;
        }
        int address = allocate(vtable, DATA_SIZE+DSBufferDesc.SIZE+WaveFormatEx.SIZE-4, cleanupCallback);
        setData(address, OFFSET_FLAGS, flags);
        Memory.mem_memcpy(address+OFFSET_DATA_START+OFFSET_DESC, lpcDSBufferDesc, DSBufferDesc.SIZE-4);
        if (desc.lpwfxFormat != null)
            Memory.mem_memcpy(address+OFFSET_DATA_START+OFFSET_DESC_WAV, Memory.mem_readd(lpcDSBufferDesc+16), WaveFormatEx.SIZE);
        Memory.mem_writed(lplpDirectSoundBuffer, address);
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER)!=0) {
            //Win.panic("Have not implemented direct sound primary buffer yet");
        }
        int data = WinSystem.getCurrentProcess().heap.alloc(desc.dwBufferBytes+MEMORY_HEADER_SIZE, false);
        setData(address, OFFSET_MEMORY, data);
        incrementMemoryRef(address);
        return Error.S_OK;
    }

    public static int duplicate(int This, int lplpDirectSoundBuffer) {
        int vtable = Memory.mem_readd(This);
        int address = allocate(vtable, DATA_SIZE+DSBufferDesc.SIZE, cleanupCallback);
        Memory.mem_memcpy(address+OFFSET_DATA_START+OFFSET_DESC, This+OFFSET_DATA_START+OFFSET_DESC, DSBufferDesc.SIZE);
        setData(address, OFFSET_MEMORY, getData(This, OFFSET_MEMORY));
        incrementMemoryRef(address);
        Memory.mem_writed(lplpDirectSoundBuffer, address);
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
        static public final int DSBSTATUS_PLAYING =     0x00000001;
        static public final int DSBSTATUS_BUFFERLOST =  0x00000002;
        static public final int DSBSTATUS_LOOPING =     0x00000004;
        static public final int DSBSTATUS_LOCHARDWARE = 0x00000008;
        static public final int DSBSTATUS_LOCSOFTWARE = 0x00000010;
        static public final int DSBSTATUS_TERMINATED =  0x00000020;

        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwStatus = CPU.CPU_Pop32();
            Memory.mem_writed(lpdwStatus, DSBSTATUS_LOCSOFTWARE);
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
        static final int DSBLOCK_FROMWRITECURSOR = 0x00000001;
        static final int DSBLOCK_ENTIREBUFFER = 0x00000002;

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
            if ((dwFlags & DSBLOCK_FROMWRITECURSOR)!=0)
                dwOffset = getEnd(This);
            else if ((dwFlags & DSBLOCK_ENTIREBUFFER)!=0)
                dwOffset = 0;
            int memory = getBits(This);
            int size = getSize(This);

            Memory.mem_writed(ppvAudioPtr1, memory+dwOffset);
            int length = size - dwOffset;
            if (length>dwBytes) {
                Memory.mem_writed(pdwAudioBytes1, dwBytes);
                if (ppvAudioPtr2 != 0)
                    Memory.mem_writed(ppvAudioPtr2, 0);
                if (pdwAudioBytes2 !=0 )
                    Memory.mem_writed(pdwAudioBytes2, 0);
            } else {
                Memory.mem_writed(pdwAudioBytes1, length);
                if (ppvAudioPtr2 != 0)
                    Memory.mem_writed(ppvAudioPtr2, memory);
                if (pdwAudioBytes2 !=0 )
                    Memory.mem_writed(pdwAudioBytes2, dwBytes-length);
            }
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Play(this, DWORD dwReserved1, DWORD dwReserved2, DWORD dwFlags)
    static private Callback.Handler Play = new HandlerBase() {
        static public final int DSBPLAY_LOOPING =               0x00000001;
        static public final int DSBPLAY_LOCHARDWARE =           0x00000002;
        static public final int DSBPLAY_LOCSOFTWARE =           0x00000004;
        static public final int DSBPLAY_TERMINATEBY_TIME =      0x00000008;
        static public final int DSBPLAY_TERMINATEBY_DISTANCE =  0x000000010;
        static public final int DSBPLAY_TERMINATEBY_PRIORITY =  0x000000020;

        public java.lang.String getName() {
            return "IDirectSoundBuffer.Play";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwReserved1 = CPU.CPU_Pop32();
            int dwReserved2 = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            Memory.mem_memcpy(This+OFFSET_DATA_START+OFFSET_DESC_WAV, lpcfxFormat, WaveFormatEx.SIZE);
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Restore(this)
    static private Callback.Handler Restore = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Restore";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };
}
