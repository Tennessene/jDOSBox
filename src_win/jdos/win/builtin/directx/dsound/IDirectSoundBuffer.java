package jdos.win.builtin.directx.dsound;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.directx.DirectCallback;
import jdos.win.builtin.directx.ddraw.IUnknown;
import jdos.win.kernel.WinCallback;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.Hashtable;

public class IDirectSoundBuffer extends IUnknown {
    static final int VTABLE_SIZE = 18;

    final static int DSBSIZE_MIN = 4;
    final static int DSBSIZE_MAX = 0xFFFFFFF;

    final static int OFFSET_FLAGS = 0;
    final static int OFFSET_MEMORY = 4;
    final static int OFFSET_START_POS = 8;
    final static int OFFSET_END_POS = 12;
    final static int OFFSET_PARENT = 16;
    final static int OFFSET_DESC = 20;

    final static int OFFSET_DESC_WAV = OFFSET_DESC+16;
    static final int DATA_SIZE = OFFSET_DESC+DSBufferDesc.SIZE+WaveFormatEx.SIZE-4;

    final static int MEMORY_HEADER_SIZE = 4;
    final static int MEMORY_OFFSET_REF_COUNT = 0;

    static int getParent(int This) {
        return getData(This, OFFSET_PARENT);
    }
    static void setParent(int This, int parent) {
        setData(This, OFFSET_PARENT, parent);
    }

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

    static private Callback.Handler CleanUp = new DirectCallback() {
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
            PlayThread thread = threads.get(This);
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
        if ((desc.dwFlags & DSBufferDesc.DSBCAPS_PRIMARYBUFFER)==0 && (desc.dwBufferBytes<DSBSIZE_MIN || desc.dwBufferBytes>DSBSIZE_MAX)) {
            return Error.DDERR_INVALIDPARAMS;
        }
        int address = allocate(vtable, DATA_SIZE, cleanupCallback);
        setData(address, OFFSET_FLAGS, flags);
        Memory.mem_memcpy(address+OFFSET_DATA_START+OFFSET_DESC, lpcDSBufferDesc, DSBufferDesc.SIZE-4);
        if (desc.lpwfxFormat != null) {
            Memory.mem_memcpy(address+OFFSET_DATA_START+OFFSET_DESC_WAV, Memory.mem_readd(lpcDSBufferDesc+16), WaveFormatEx.SIZE);
        }
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
        int address = allocate(vtable, DATA_SIZE, cleanupCallback);
        Memory.mem_memcpy(address + OFFSET_DATA_START, This + OFFSET_DATA_START, DATA_SIZE);
        incrementMemoryRef(address);
        Memory.mem_writed(lplpDirectSoundBuffer, address);
        setParent(address, This);
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
            Memory.mem_writed(lpdwCurrentPlayCursor, 0);
            Memory.mem_writed(lpdwCurrentWriteCursor, 0);
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
            int status = DSBSTATUS_LOCSOFTWARE;
            PlayThread thread = threads.get(This);
            if (thread!=null) {
                if (thread.playing)
                    status |= DSBSTATUS_PLAYING;
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
            byte[] buffer = buffers.get(This);
            if (buffer == null) {
                int parent = getParent(This);
                if (parent != 0)
                    buffer = buffers.get(parent);
            }
            if (buffer == null) {
                buffer = getBuffer(This);
            }
            play(This, buffer);
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
            System.out.println(getName()+" faked");
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
            int dwAudioBytes2 = CPU.CPU_Pop32();
            setData(This, OFFSET_START_POS, getBits(This)-pvAudioPtr1);
            if (pvAudioPtr2 == 0 || dwAudioBytes2 == 0)
                setData(This, OFFSET_END_POS, dwAudioBytes1);
            else
                setData(This, OFFSET_END_POS, getBits(This)-pvAudioPtr2);

            getBuffer(This);
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

    private static Hashtable<Integer, PlayThread> threads = new Hashtable<Integer, PlayThread>();
    private static Hashtable<Integer, byte[]> buffers = new Hashtable<Integer, byte[]>();

    private static class PlayThread extends Thread {
        public PlayThread(WaveFormatEx format, int This) {
            this.format = format;
            this.This = This;
            open();
        }

        public boolean open() {
            try {
                AudioFormat af = new AudioFormat(format.nSamplesPerSec, format.wBitsPerSample, format.nChannels, false, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(af, 8192);
                line.start();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        WaveFormatEx format;
        SourceDataLine line;
        int This;
        boolean playing = false;
        final Object mutex = new Object();
        boolean bExit = false;
        byte[] buffer;

        public void run() {
            while (!bExit) {
                playing = true;

                line.write(buffer, 0, buffer.length);
                while (line.available() != line.getBufferSize()) {
                    try {Thread.sleep(25);} catch (Exception e) {}
                }
                synchronized (mutex) {
                    playing = false;
                    if (!bExit)
                        try {mutex.wait();} catch (Exception e){}
                }
            }
            line.stop();
            line.close();
            line = null;
            threads.remove(This);
        }
    }

    private static void play(int This, byte[] buffer) {
        PlayThread thread = threads.get(This);

        if (thread != null) {
            thread.buffer = buffer;
            synchronized (thread.mutex) {
                if (!thread.playing) {
                    thread.mutex.notify();
                }
            }
        } else {
            thread = new PlayThread(new WaveFormatEx(This + OFFSET_DESC_WAV + OFFSET_DATA_START), This);
            thread.buffer = buffer;
            threads.put(This, thread);
            thread.start();
        }
    }

    private static byte[] getBuffer(int This) {
        int size = getSize(This);
        int start = getBits(This);
        int posStart = getStart(This);
        int posEnd = getEnd(This);

        int len;
        if (posEnd<posStart)
            len=size - (posStart-posEnd);
        else
            len = posEnd - posStart;

        byte[] buffer = new byte[len];
        if (posEnd>posStart)
            Memory.mem_memcpy(buffer, 0, start+posStart, len);
        else {
            int part1Len = size  - (posStart-start);
            Memory.mem_memcpy(buffer, 0, start+posStart, part1Len);
            Memory.mem_memcpy(buffer, part1Len, start, len-part1Len);
        }
        buffers.put(This, buffer);
        return buffer;
    }
}
