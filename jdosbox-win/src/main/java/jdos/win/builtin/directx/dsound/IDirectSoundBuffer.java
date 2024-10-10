package jdos.win.builtin.directx.dsound;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.ReturnHandlerBase;
import jdos.win.builtin.directx.DError;
import jdos.win.builtin.directx.DirectCallback;
import jdos.win.builtin.directx.ddraw.IUnknown;
import jdos.win.builtin.winmm.WAVEFORMATEX;
import jdos.win.builtin.winmm.WAVEFORMATEXTENSIBLE;
import jdos.win.kernel.WinCallback;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;
import jdos.win.utils.Ptr;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class IDirectSoundBuffer extends IUnknown {
    static final int VTABLE_SIZE = 18;

    final static int DSBSIZE_MIN = 4;
    final static int DSBSIZE_MAX = 0xFFFFFFF;

    final static int OFFSET_FLAGS = 0;
    final static int OFFSET_HANDLE = 4;
    final static int OFFSET_DESC = 8;

    final static int OFFSET_DESC_WAV = OFFSET_DESC + 16;
    static final int DATA_SIZE = OFFSET_DESC + DSBufferDesc.SIZE + WAVEFORMATEX.SIZE - 4;

    final static int MEMORY_HEADER_SIZE = 4;
    final static int MEMORY_OFFSET_REF_COUNT = 0;

    static private void incrementMemoryRef(int This) {
        Data data = Data.get(This);
        int refAddress = data.buffer + MEMORY_OFFSET_REF_COUNT;
        int ref = Memory.mem_readd(refAddress);
        ref++;
        Memory.mem_writed(refAddress, ref);
    }

    static private int decrementMemoryRef(int This) {
        Data data = Data.get(This);
        int refAddress = data.buffer + MEMORY_OFFSET_REF_COUNT;
        int ref = Memory.mem_readd(refAddress);
        ref--;
        Memory.mem_writed(refAddress, ref);
        return ref;
    }

    static public class Data extends WinObject {
        static public Data create(int This) {
            return new Data(nextObjectId(), This);
        }

        static public Data get(int This) {
            int handle = getData(This, OFFSET_HANDLE);
            return (Data) getObject(handle);
        }

        public int getTmpStart() {
            return ((int) ((long) startPos * tmp_buffer_len / buflen)) & ~3;
        }

        public int getTmpEnd() {
            return ((int) ((long) endPos * tmp_buffer_len / buflen)) & ~3;
        }

        public void play(boolean loop) {
            if (tmp_buffer == null)
                return;

            if (thread != null) {
                thread.loop = loop;
                synchronized (thread.mutex) {
                    if (!thread.playing) {
                        thread.mutex.notify();
                    }
                }
            } else {
                thread = new PlayThread(this);
                thread.loop = loop;
                thread.start();
            }
        }

        public void stop() {
            if (thread != null)
                thread.stop = true;
        }

        public PlayThread thread;
        public int This;
        public int startPos;
        public int endPos;
        public int parent;
        public int freq;
        public int nAvgBytesPerSec;
        public int buflen;
        public int freqAdjust;
        public int writelead;
        public int max_buffer_len;
        public int freqAcc;
        public int freqAccNext;
        public boolean freqneeded;
        public int tmp_buffer_len;
        public byte[] tmp_buffer;
        public int sec_mixpos;
        public int buf_mixpos;
        public int buffer;
        public DSConvert.bitsconvertfunc convert;
        public DSVOLUMEPAN volpan = new DSVOLUMEPAN();
        public boolean tmp_buffer_copied = false;

        public Data(int handle, int This) {
            super(handle);
            this.This = This;
        }

        public void copy(Data data) {
            this.This = data.This;
            this.startPos = data.startPos;
            this.endPos = data.endPos;
            this.parent = data.parent;
            this.freq = data.freq;
            this.nAvgBytesPerSec = data.nAvgBytesPerSec;
            this.buflen = data.buflen;
            this.freqAdjust = data.freqAdjust;
            this.writelead = data.writelead;
            this.max_buffer_len = data.max_buffer_len;
            this.freqAcc = data.freqAcc;
            this.freqAccNext = data.freqAccNext;
            this.freqneeded = data.freqneeded;
            this.tmp_buffer_len = data.tmp_buffer_len;
            this.tmp_buffer = data.tmp_buffer;
            this.sec_mixpos = data.sec_mixpos;
            this.buf_mixpos = data.buf_mixpos;
            this.buffer = data.buffer;
            this.convert = data.convert;
            this.volpan = new DSVOLUMEPAN(data.volpan);
            data.tmp_buffer_copied = true;
        }

        public int flags() {
            return getFlags(This);
        }

        public WAVEFORMATEX wfx() {
            return new WAVEFORMATEX(This + OFFSET_DATA_START + OFFSET_DESC_WAV);
        }

        public WAVEFORMATEXTENSIBLE wfxe() {
            return new WAVEFORMATEXTENSIBLE(wfx(), This + OFFSET_DATA_START + OFFSET_DESC_WAV);
        }
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

    static private Callback.Handler CleanUp = new DirectCallback() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.CleanUp";
        }

        public void onCall() {
            int This = CPU.CPU_Pop32();
            int refCount = decrementMemoryRef(This);
            Data data = Data.get(This);
            if (refCount == 0) {
                WinSystem.getCurrentProcess().heap.free(data.buffer);
            }
            PlayThread thread = data.thread;
            if (thread != null) {
                thread.bExit = true;
                synchronized (thread.mutex) {
                    thread.mutex.notify();
                }
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
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER) == 0 && (desc.dwBufferBytes < DSBSIZE_MIN || desc.dwBufferBytes > DSBSIZE_MAX)) {
            return Error.DDERR_INVALIDPARAMS;
        }
        int address = allocate(vtable, DATA_SIZE, cleanupCallback);
        setData(address, OFFSET_FLAGS, flags);
        Memory.mem_memcpy(address + OFFSET_DATA_START + OFFSET_DESC, lpcDSBufferDesc, DSBufferDesc.SIZE - 4);
        if (desc.lpwfxFormat != null) {
            Memory.mem_memcpy(address + OFFSET_DATA_START + OFFSET_DESC_WAV, Memory.mem_readd(lpcDSBufferDesc + 16), WAVEFORMATEX.SIZE);
        }
        Memory.mem_writed(lplpDirectSoundBuffer, address);
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER) != 0) {
            //Win.panic("Have not implemented direct sound primary buffer yet");
            desc.lpwfxFormat = new WAVEFORMATEX();
            desc.lpwfxFormat.write(address + OFFSET_DATA_START + OFFSET_DESC_WAV);
        }
        if (desc.lpwfxFormat.nSamplesPerSec == 0)
            desc.lpwfxFormat.nSamplesPerSec = desc.lpwfxFormat.nAvgBytesPerSec * 8 / desc.lpwfxFormat.wBitsPerSample / desc.lpwfxFormat.nChannels;
        Data d = Data.create(address);
        d.buffer = WinSystem.getCurrentProcess().heap.alloc(desc.dwBufferBytes + MEMORY_HEADER_SIZE, false);
        d.buflen = desc.dwBufferBytes;
        d.freq = desc.lpwfxFormat.nSamplesPerSec;
        d.freqAdjust = (int) (((long) d.freq << DSOUND_FREQSHIFT) / DSMixer.DEVICE_SAMPLE_RATE);
        d.nAvgBytesPerSec = d.freq * desc.lpwfxFormat.nBlockAlign;

        DSMixer.DSOUND_RecalcFormat(d);
        setData(address, OFFSET_HANDLE, d.handle);
        incrementMemoryRef(address);
        return Error.S_OK;
    }

    public static int duplicate(int This, int lplpDirectSoundBuffer) {
        int vtable = Memory.mem_readd(This);
        int address = allocate(vtable, DATA_SIZE, cleanupCallback);
        Memory.mem_memcpy(address + OFFSET_DATA_START, This + OFFSET_DATA_START, DATA_SIZE);

        incrementMemoryRef(This);
        Memory.mem_writed(lplpDirectSoundBuffer, address);
        Data fromData = Data.get(This);
        Data d = Data.create(address);
        d.copy(fromData);
        setData(address, OFFSET_HANDLE, d.handle);
        d.buf_mixpos = d.sec_mixpos = 0;
        d.parent = This;
        return Error.S_OK;
    }

    static public int getFlags(int This) {
        return getData(This, OFFSET_DESC + 4);
    }

    static public int getBufferBytes(int This) {
        return getData(This, OFFSET_DESC + 8);
    }

    // HRESULT GetCaps(this, LPDSBCAPS lpDSBufferCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetCaps";
        }

        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDSBufferCaps = CPU.CPU_Pop32();
            if (lpDSBufferCaps == 0 || Memory.mem_readd(lpDSBufferCaps) < DSBCaps.SIZE) {
                CPU_Regs.reg_eax.dword = Error.DDERR_INVALIDPARAMS;
                return;
            }
            DSBCaps.write(lpDSBufferCaps, getFlags(This) | DSBufferDesc.DSBCAPS_LOCSOFTWARE, getBufferBytes(This), 0, 0);
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            Data data = Data.get(This);
            Memory.mem_writed(lpdwCurrentPlayCursor, data.startPos);
            Memory.mem_writed(lpdwCurrentWriteCursor, data.endPos);
            CPU_Regs.reg_eax.dword = Error.S_OK;
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
            int f = readd(This + OFFSET_DATA_START + OFFSET_DESC_WAV + 4);
            if (f == 0)
                f = readd(This + OFFSET_DATA_START + OFFSET_DESC_WAV + 8);
            writed(lpdwFrequency, f);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT GetStatus(this, LPDWORD lpdwStatus)
    static private Callback.Handler GetStatus = new HandlerBase() {
        static public final int DSBSTATUS_PLAYING = 0x00000001;
        static public final int DSBSTATUS_BUFFERLOST = 0x00000002;
        static public final int DSBSTATUS_LOOPING = 0x00000004;
        static public final int DSBSTATUS_LOCHARDWARE = 0x00000008;
        static public final int DSBSTATUS_LOCSOFTWARE = 0x00000010;
        static public final int DSBSTATUS_TERMINATED = 0x00000020;

        public java.lang.String getName() {
            return "IDirectSoundBuffer.GetStatus";
        }

        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpdwStatus = CPU.CPU_Pop32();
            int status = 0;
            PlayThread thread = Data.get(This).thread;
            if (thread != null) {
                if (thread.playing) {
                    status |= DSBSTATUS_PLAYING;
                    if (thread.loop)
                        status |= DSBSTATUS_LOOPING;
                }
            }
            Memory.mem_writed(lpdwStatus, status);
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
            Data data = Data.get(This);
            if ((dwFlags & DSBLOCK_FROMWRITECURSOR) != 0)
                dwOffset = data.endPos;
            else if ((dwFlags & DSBLOCK_ENTIREBUFFER) != 0)
                dwOffset = 0;
            int memory = data.buffer;
            int size = data.buflen;

            Memory.mem_writed(ppvAudioPtr1, memory + dwOffset);
            int length = size - dwOffset;
            if (length > dwBytes) {
                Memory.mem_writed(pdwAudioBytes1, dwBytes);
                if (ppvAudioPtr2 != 0)
                    Memory.mem_writed(ppvAudioPtr2, 0);
                if (pdwAudioBytes2 != 0)
                    Memory.mem_writed(pdwAudioBytes2, 0);
            } else {
                Memory.mem_writed(pdwAudioBytes1, length);
                if (ppvAudioPtr2 != 0)
                    Memory.mem_writed(ppvAudioPtr2, memory);
                if (pdwAudioBytes2 != 0)
                    Memory.mem_writed(pdwAudioBytes2, dwBytes - length);
            }
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Play(this, DWORD dwReserved1, DWORD dwReserved2, DWORD dwFlags)
    static private Callback.Handler Play = new HandlerBase() {
        static public final int DSBPLAY_LOOPING = 0x00000001;
        static public final int DSBPLAY_LOCHARDWARE = 0x00000002;
        static public final int DSBPLAY_LOCSOFTWARE = 0x00000004;
        static public final int DSBPLAY_TERMINATEBY_TIME = 0x00000008;
        static public final int DSBPLAY_TERMINATEBY_DISTANCE = 0x000000010;
        static public final int DSBPLAY_TERMINATEBY_PRIORITY = 0x000000020;

        public java.lang.String getName() {
            return "IDirectSoundBuffer.Play";
        }

        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwReserved1 = CPU.CPU_Pop32();
            int dwReserved2 = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            Data data = Data.get(This);
            if (data.thread == null || !data.thread.playing)
                data.play((dwFlags & DSBPLAY_LOOPING) != 0);
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
            Data data = Data.get(This);
            data.startPos = dwNewPosition;
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
            Memory.mem_memcpy(This + OFFSET_DATA_START + OFFSET_DESC_WAV, lpcfxFormat, WAVEFORMATEX.SIZE);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT SetVolume(this, LONG lVolume)
    static private Callback.Handler SetVolume = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetVolume";
        }

        public int processReturn() {
            int This = CPU.CPU_Pop32();
            int vol = CPU.CPU_Pop32();
            if ((getFlags(This) & DSBufferDesc.DSBCAPS_CTRLVOLUME) == 0) {
                warn("IDirectSoundBuffer.SetVolume control unavailable: dwFlags = 0x" + Ptr.toString(getFlags(This)));
                return DError.DSERR_CONTROLUNAVAIL;
            }

            if ((vol > DSBVOLUME_MAX) || (vol < DSBVOLUME_MIN)) {
                warn("IDirectSoundBuffer.SetVolume invalid parameter: vol = " + vol);
                return DError.DSERR_INVALIDPARAM;
            }

            Data data = Data.get(This);
            int oldVol = data.volpan.lVolume;
            data.volpan.lVolume = vol;
            if (vol != oldVol) {
                DSMixer.DSOUND_RecalcVolPan(data.volpan);
                DSMixer.DSOUND_MixToTemporary(data, 0, data.buflen);
            }
            return Error.S_OK;
        }
    };

    // HRESULT SetPan(this, LONG lPan)
    static private Callback.Handler SetPan = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetPan";
        }

        public int processReturn() {
            int This = CPU.CPU_Pop32();
            int pan = CPU.CPU_Pop32();
            if ((pan > DSBPAN_RIGHT) || (pan < DSBPAN_LEFT)) {
                warn("IDirectSoundBuffer.SetPan invalid parameter: pan = " + pan);
                return DError.DSERR_INVALIDPARAM;
            }

            Data data = Data.get(This);
            int flags = data.flags();
            /* You cannot use both pan and 3D controls */
            if ((flags & DSBufferDesc.DSBCAPS_CTRLPAN) == 0 || (flags & DSBufferDesc.DSBCAPS_CTRL3D) != 0) {
                warn("IDirectSoundBuffer.SetPan control unavailable");
                return DError.DSERR_CONTROLUNAVAIL;
            }

            if (data.volpan.lPan != pan) {
                data.volpan.lPan = pan;
                DSMixer.DSOUND_RecalcVolPan(data.volpan);
                DSMixer.DSOUND_MixToTemporary(data, 0, data.buflen);
            }

            return Error.S_OK;
        }
    };

    // HRESULT SetFrequency(this, DWORD dwFrequency)
    static private Callback.Handler SetFrequency = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.SetFrequency";
        }

        public int processReturn() {
            int This = CPU.CPU_Pop32();
            int freq = CPU.CPU_Pop32();

            if (is_primary_buffer(This)) {
                log("IDirectSoundBuffer.SetFrequency not available for primary buffers.");
                return DError.DSERR_CONTROLUNAVAIL;
            }

            if ((getFlags(This) & DSBufferDesc.DSBCAPS_CTRLFREQUENCY) == 0) {
                log("IDirectSoundBuffer.SetFrequency control unavailable");
                return DError.DSERR_CONTROLUNAVAIL;
            }
            WAVEFORMATEX wfx = new WAVEFORMATEX(This + OFFSET_DESC_WAV + OFFSET_DATA_START);

            if (freq == 0) // DSBFREQUENCY_ORIGINAL
                freq = wfx.nSamplesPerSec;

            if ((freq < 100) || (freq > 200000)) {
                warn("IDirectSoundBuffer.SetFrequency invalid parameter: freq = " + freq);
                return DError.DSERR_INVALIDPARAM;
            }
            Data data = Data.get(This);
            int oldFreq = data.freq;
            if (freq != oldFreq) {
                synchronized (data) {
                    data.freq = freq;
                    data.freqAdjust = (int) (((long) freq << DSOUND_FREQSHIFT) / DSMixer.DEVICE_SAMPLE_RATE);
                    data.nAvgBytesPerSec = freq * wfx.nBlockAlign;
                    DSMixer.DSOUND_RecalcFormat(data);
                    DSMixer.DSOUND_MixToTemporary(data, 0, data.buflen);
                }
            }
            return Error.S_OK;
        }
    };

    // HRESULT Stop(this)
    static private Callback.Handler Stop = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Stop";
        }

        public void onCall() {
            int This = CPU.CPU_Pop32();
            Data data = Data.get(This);
            data.stop();
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT Unlock(this, LPVOID pvAudioPtr1, DWORD dwAudioBytes1, LPVOID pvAudioPtr2, DWORD dwAudioPtr2)
    static private Callback.Handler Unlock = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "IDirectSoundBuffer.Unlock";
        }

        public int processReturn() {
            int This = CPU.CPU_Pop32();
            int p1 = CPU.CPU_Pop32();
            int x1 = CPU.CPU_Pop32();
            int p2 = CPU.CPU_Pop32();
            int x2 = CPU.CPU_Pop32();
            Data data = Data.get(This);

            if ((p1 != 0 && p1 < data.buffer || p1 >= data.buffer + data.buflen) || (p2 != 0 && p2 < data.buffer || p2 >= data.buffer + data.buflen))
                return DError.DSERR_INVALIDPARAM;

            if (x1 != 0) {
                if (x1 + p1 - data.buffer > data.buflen)
                    return DError.DSERR_INVALIDPARAM;
                else
                    DSMixer.DSOUND_MixToTemporary(data, p1 - data.buffer, x1);
                data.endPos = p1 - data.buffer + x1;
            }
            if (x2 != 0) {
                DSMixer.DSOUND_MixToTemporary(data, 0, x2);
                data.endPos = x1;
            }
            return Error.S_OK;
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

    private static class PlayThread extends Thread {
        static private final int LINE_SIZE = 16384;
        public PlayThread(Data data) {
            this.format = data.wfx();
            this.data = data;
            open();
        }

        public boolean open() {
            try {
                AudioFormat af = new AudioFormat(DSMixer.DEVICE_SAMPLE_RATE, DSMixer.DEVICE_BITS_PER_SAMEPLE, DSMixer.DEVICE_CHANNELS, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(af, LINE_SIZE);
                line.start();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        WAVEFORMATEX format;
        SourceDataLine line;
        final Data data;
        boolean playing = false;
        final Object mutex = new Object();
        boolean bExit = false;
        boolean stop = false;
        boolean loop = false;

        private void play(byte[] buffer, int bufferLen, int start, int end) {
            int length = 8192;
            for (int i = start; i < end && !stop && data.tmp_buffer_len == bufferLen; i += 8192) {
                if (i + length >= end)
                    length = end - i;
                try {
                    line.write(buffer, i, length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                data.startPos = ((int) ((long) (i + length) * data.buflen / data.tmp_buffer_len) + 3) & ~3;
            }
        }

        public void run() {
            while (!bExit) {
                playing = true;
                do {
                    while (true) {
                        int start;
                        int end;
                        int prevEnd;
                        int prevStart;
                        byte[] buffer;
                        int bufferLen;

                        synchronized (data) {
                            start = data.getTmpStart();
                            end = data.getTmpEnd();
                            prevEnd = data.endPos;
                            prevStart = data.startPos;
                            buffer = data.tmp_buffer;
                            bufferLen = data.tmp_buffer_len;
                        }
                        if (end > start)
                            play(buffer, bufferLen, start, end);
                        else {
                            play(buffer, bufferLen, start, data.tmp_buffer_len);
                            if (!stop && buffer == data.tmp_buffer)
                                play(buffer, bufferLen, 0, end);
                        }
                        if (loop) {
                            data.startPos = prevStart;
                        }
                        if (prevEnd != data.endPos || bufferLen != data.tmp_buffer_len)
                            continue;
                        break;
                    }
                } while (loop && !stop);

                while (line.available() != LINE_SIZE) {
                    try {Thread.sleep(10);} catch (Exception e) {}
                }
                synchronized (mutex) {
                    playing = false;
                    stop = false;
                    if (!bExit) {
                        try {
                            mutex.wait();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            line.stop();
            line.close();
            line = null;
        }
    }

    static private boolean is_primary_buffer(int This) {
        return (getFlags(This) & DSBufferDesc.DSBCAPS_PRIMARYBUFFER) != 0 ? true : false;
    }
}
