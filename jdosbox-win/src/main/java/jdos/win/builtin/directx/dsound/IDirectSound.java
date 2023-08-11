package jdos.win.builtin.directx.dsound;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.directx.ddraw.IUnknown;
import jdos.win.utils.Error;

public class IDirectSound extends IUnknown {
    static final int VTABLE_SIZE = 8;

    static int OFFSET_FLAGS = 0;
    static final int DATA_SIZE = 4;


    private static int createVTable() {
        int address = allocateVTable("IDirectSound", VTABLE_SIZE);
        addIDirectSound(address);
        return address;
    }

    static int addIDirectSound(int address) {
        address = addIUnknown(address);
        address = add(address, CreateSoundBuffer);
        address = add(address, GetCaps);
        address = add(address, DuplicateSoundBuffer);
        address = add(address, SetCooperativeLevel);
        address = add(address, Compact);
        address = add(address, GetSpeakerConfig);
        address = add(address, SetSpeakerConfig);
        address = add(address, Initialize);
        return address;
    }

    public static int create() {
        return create("IDirectSound", 0);
    }

    public static int create(String name, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0)
            vtable = createVTable();
        int address = allocate(vtable, DATA_SIZE, 0);
        setData(address, OFFSET_FLAGS, flags);
        return address;
    }

    // HRESULT CreateSoundBuffer(this, LPCDSBUFFERDESC lpcDSBufferDesc, LPLPDIRECTSOUNDBUFFER lplpDirectSoundBuffer, IUnknown *pUnkOuter)
    static private Callback.Handler CreateSoundBuffer = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSound.CreateSoundBuffer";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpcDSBufferDesc = CPU.CPU_Pop32();
            int lplpDirectSoundBuffer = CPU.CPU_Pop32();
            int pUnkOuter = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = IDirectSoundBuffer.create(lplpDirectSoundBuffer, lpcDSBufferDesc);
        }
    };

    // HRESULT GetCaps(this, LPDSCAPS lpDSCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        static final int DSCAPS_PRIMARYMONO =       0x00000001;
        static final int DSCAPS_PRIMARYSTEREO =     0x00000002;
        static final int DSCAPS_PRIMARY8BIT =       0x00000004;
        static final int DSCAPS_PRIMARY16BIT =      0x00000008;
        static final int DSCAPS_CONTINUOUSRATE =    0x00000010;
        static final int DSCAPS_EMULDRIVER =        0x00000020;
        static final int DSCAPS_CERTIFIED =         0x00000040;
        static final int DSCAPS_SECONDARYMONO =     0x00000100;
        static final int DSCAPS_SECONDARYSTEREO =   0x00000200;
        static final int DSCAPS_SECONDARY8BIT =     0x00000400;
        static final int DSCAPS_SECONDARY16BIT =    0x00000800;

        public java.lang.String getName() {
            return "IDirectSound.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDSCaps = CPU.CPU_Pop32();
            DSCaps caps = new DSCaps(lpDSCaps);
            if (caps.dwSize != 96) {
                Win.panic(getName() + " unexpected size of caps structure");
            }
            caps.dwFlags = DSCAPS_PRIMARYMONO|DSCAPS_PRIMARYSTEREO|DSCAPS_PRIMARY8BIT|DSCAPS_PRIMARY16BIT|DSCAPS_CONTINUOUSRATE|DSCAPS_CERTIFIED|DSCAPS_SECONDARYMONO|DSCAPS_SECONDARYSTEREO|DSCAPS_SECONDARY8BIT|DSCAPS_SECONDARY16BIT;
            caps.dwMinSecondarySampleRate = 4000;
            caps.dwMaxSecondarySampleRate = 48000;
            caps.dwPrimaryBuffers = 1;
            caps.dwMaxHwMixingAllBuffers = 1;
            caps.dwMaxHwMixingStaticBuffers = 1;
            caps.dwMaxHwMixingStreamingBuffers = 1;
            caps.dwFreeHwMixingAllBuffers = 0;
            caps.dwFreeHwMixingStaticBuffers = 0;
            caps.dwFreeHwMixingStreamingBuffers = 0;
            caps.dwMaxHw3DAllBuffers = 0;
            caps.dwMaxHw3DStaticBuffers = 0;
            caps.dwMaxHw3DStreamingBuffers = 0;
            caps.dwFreeHw3DAllBuffers = 0;
            caps.dwFreeHw3DStaticBuffers = 0;
            caps.dwFreeHw3DStreamingBuffers = 0;
            caps.dwTotalHwMemBytes = 0;
            caps.dwFreeHwMemBytes = 0;
            caps.dwMaxContigFreeHwMemBytes = 0;
            caps.dwUnlockTransferRateHwBuffers = 0;
            caps.write(lpDSCaps);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT DuplicateSoundBuffer(this, LPDIRECTSOUNDBUFFER lpDsbOriginal, LPLPDIRECTSOUNDBUFFER lplpDsbDuplicate)
    static private Callback.Handler DuplicateSoundBuffer = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSound.DuplicateSoundBuffer";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDsbOriginal = CPU.CPU_Pop32();
            int lplpDsbDuplicate = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = IDirectSoundBuffer.duplicate(lpDsbOriginal, lplpDsbDuplicate);
        }
    };

    // HRESULT SetCooperativeLevel(this, HWND hwnd, DWORD dwLevel)
    static private Callback.Handler SetCooperativeLevel = new HandlerBase() {
        static final public int DSSCL_NORMAL =      1;
        static final public int DSSCL_PRIORITY =    2;
        static final public int DSSCL_EXCLUSIVE =   3;
        static final public int DSSCL_WRITEPRIMARY =4;

        public java.lang.String getName() {
            return "IDirectSound.SetCooperativeLevel";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hwnd = CPU.CPU_Pop32();
            int dwLevel = CPU.CPU_Pop32();
            System.out.println(getName()+" called with priority "+dwLevel);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Compact(this)
    static private Callback.Handler Compact = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSound.Compact";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetSpeakerConfig(this, LPDWORD lpdwSpeakerConfig)
    static private Callback.Handler GetSpeakerConfig = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSound.GetSpeakerConfig";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwSpeakerConfig = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetSpeakerConfig(this, DWORD dwSpeakerConfig)
    static private Callback.Handler SetSpeakerConfig = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSound.SetSpeakerConfig";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwSpeakerConfig = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Initialize(this, LPCGUID lpcGuid)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSound.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpcGuid = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}
