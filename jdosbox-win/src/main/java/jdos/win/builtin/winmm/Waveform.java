package jdos.win.builtin.winmm;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.WinObject;
import jdos.win.utils.Ptr;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.Vector;

public class Waveform extends WinAPI {
    static final public int WAVECAPS_PITCH =            0x0001;   /* supports pitch control */
    static final public int WAVECAPS_PLAYBACKRATE =     0x0002;   /* supports playback rate control */
    static final public int WAVECAPS_VOLUME =           0x0004;   /* supports volume control */
    static final public int WAVECAPS_LRVOLUME =         0x0008;   /* separate left-right volume control */
    static final public int WAVECAPS_SYNC =             0x0010;	 /* driver is synchronous and playing is blocking */
    static final public int WAVECAPS_SAMPLEACCURATE =   0x0020;	 /* position is sample accurate */
    static final public int WAVECAPS_DIRECTSOUND =      0x0040;   /* ? */
    
    private static class WaveObject extends WinObject {
        static public WaveObject create(WAVEFORMATEX format) {
            return new WaveObject(nextObjectId(), format);
        }

        static public WaveObject get(int handle) {
            WinObject object = getObject(handle);
            if (object == null || !(object instanceof WaveObject))
                return null;
            return (WaveObject)object;
        }

        public WaveObject(int id, WAVEFORMATEX format) {
            super(id);
            thread = new WaveOutThread(format);
            thread.start();
        }
        public WaveOutThread thread;
    }

    private static class WaveOutThread extends Thread {
        public WaveOutThread(WAVEFORMATEX format) {
            this.format = format;
            open();
        }

        public boolean open() {
            try {
                AudioFormat af = new AudioFormat(format.nSamplesPerSec, format.wBitsPerSample, format.nChannels, true, false);
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

        public void reset() {
            buffers.clear();
        }

        final Vector<WAVEHDR> buffers = new Vector<WAVEHDR>();
        WAVEFORMATEX format;
        boolean exit = false;
        SourceDataLine line;

        public void run() {
            while (!exit) {
                while (buffers.size()>0) {
                    WAVEHDR hdr = buffers.remove(0);
                    line.write(hdr.data, 0, hdr.data.length);
                    hdr.dwFlags &= ~WAVEHDR.WHDR_INQUEUE;
                    hdr.dwFlags |= WAVEHDR.WHDR_DONE;
                    hdr.writeFlags();
                }
                synchronized (buffers) {
                    if (buffers.size()==0 && !exit)
                        try {buffers.wait();} catch (Exception e){}
                }
            }
            line.stop();
            line.close();
            line = null;
        }
    }

    //MMRESULT waveOutClose(HWAVEOUT hwo)
    public static int waveOutClose(int hwo) {
        WaveObject obj = WaveObject.get(hwo);
        if (obj == null)
            return WinMM.MMSYSERR_INVALHANDLE;
        obj.thread.exit = true;
        synchronized(obj.thread.buffers) {
            obj.thread.buffers.notify();
        }
        try {obj.thread.join();} catch (Exception e) {}
        obj.close();
        return WinMM.MMSYSERR_NOERROR;
    }

    // MMRESULT waveOutGetDevCaps(UINT_PTR uDeviceID, LPWAVEOUTCAPS pwoc, UINT cbwoc)
    public static int waveOutGetDevCapsA(int uDeviceID, int pwoc, int cbwoc) {
        if (pwoc == 0)
            return WinMM.MMSYSERR_INVALPARAM;

        WAVEOUTCAPS mapper_caps = new WAVEOUTCAPS();
        mapper_caps.wMid = 0xFF;
        mapper_caps.wPid = 0xFF;
        mapper_caps.vDriverVersion = 0x00010001;
        mapper_caps.dwFormats = 0xFFFFFFFF;
        mapper_caps.wReserved1 = 0;
        mapper_caps.dwSupport = WAVECAPS_LRVOLUME | WAVECAPS_VOLUME |
            WAVECAPS_SAMPLEACCURATE;
        mapper_caps.wChannels = 2;
        mapper_caps.szPname = "Wine Sound Mapper";
        mapper_caps.write(pwoc);
        return WinMM.MMSYSERR_NOERROR;
    }

    //MMRESULT waveOutOpen(LPHWAVEOUT phwo, UINT_PTR uDeviceID, LPWAVEFORMATEX pwfx, DWORD_PTR dwCallback, DWORD_PTR dwCallbackInstance, DWORD fdwOpen)
    public static int waveOutOpen(int lphWaveOut, int uDeviceID, int pwfx, int dwCallback, int dwCallbackInstance, int fdwOpen) {
//        WINMM_OpenInfo info;
//        WINMM_CBInfo cb_info;
//
//        if(!WINMM_StartDevicesThread())
//            return MMSYSERR_ERROR;

        if(lphWaveOut==0 && (fdwOpen & WinMM.WAVE_FORMAT_QUERY)==0)
            return WinMM.MMSYSERR_INVALPARAM;

        int res = WinMM.WINMM_CheckCallback(dwCallback, fdwOpen, false);
        if(res != WinMM.MMSYSERR_NOERROR)
            return res;

        if (fdwOpen != 0)
            Win.panic("WinMM.waveOutOpen fdwOpen="+Ptr.toString(fdwOpen)+" not supported yet");
        if (uDeviceID != WinMM.WAVE_MAPPER)
            Win.panic("WinMM.waveOutOpen uDeviceID="+uDeviceID+" not supported yet");

        writed(lphWaveOut, WaveObject.create(new WAVEFORMATEX(pwfx)).handle);

//        info.format = (WAVEFORMATEX*)lpFormat;
//        info.callback = dwCallback;
//        info.cb_user = dwInstance;
//        info.req_device = uDeviceID;
//        info.flags = dwFlags;
//
//        res = SendMessageW(g_devices_hwnd, WinMM.WODM_OPEN, (DWORD_PTR)&info, 0);
//        if(res != WinMM.MMSYSERR_NOERROR)
//            return res;
//
//        if(lphWaveOut)
//            *lphWaveOut = (HWAVEOUT)info.handle;
//
//        cb_info.flags = HIWORD(fdwOpen & WinMM.CALLBACK_TYPEMASK);
//        cb_info.callback = dwCallback;
//        cb_info.user = dwInstance;
//        cb_info.hwave = info.handle;
//
//        WINMM_NotifyClient(&cb_info, WOM_OPEN, 0, 0);

        return res;
    }

    // MMRESULT waveOutPrepareHeader(HWAVEOUT hwo, LPWAVEHDR pwh, UINT cbwh)
    static public int waveOutPrepareHeader(int hwo, int pwh, int cbwh) {
        if (pwh==0 || cbwh<WAVEHDR.SIZE)
            return WinMM.MMSYSERR_INVALPARAM;

        WAVEHDR hdr = new WAVEHDR(pwh);
        if ((hdr.dwFlags & WAVEHDR.WHDR_INQUEUE)!=0)
            return WinMM.WAVERR_STILLPLAYING;

        WaveObject obj = WaveObject.get(hwo);
        if (obj == null)
            return WinMM.MMSYSERR_INVALHANDLE;

        hdr.dwFlags |= WAVEHDR.WHDR_PREPARED;
        hdr.dwFlags &= ~WAVEHDR.WHDR_DONE;
        hdr.reserved = pwh;
        hdr.writeFlags();
        return WinMM.MMSYSERR_NOERROR;
    }

    // MMRESULT waveOutReset(HWAVEOUT hwo)
    static public int waveOutReset(int hwo) {
        WaveObject obj = WaveObject.get(hwo);
        if (obj == null)
            return WinMM.MMSYSERR_INVALHANDLE;
        obj.thread.reset();
        return WinMM.MMSYSERR_NOERROR;
    }

    // MMRESULT waveOutUnprepareHeader(HWAVEOUT hwo, LPWAVEHDR pwh, UINT cbwh)
    static public int waveOutUnprepareHeader(int hwo, int pwh, int cbwh) {
        if (pwh==0 || cbwh<WAVEHDR.SIZE)
            return WinMM.MMSYSERR_INVALPARAM;

        WaveObject obj = WaveObject.get(hwo);
        if (obj == null)
            return WinMM.MMSYSERR_INVALHANDLE;

        WAVEHDR hdr = new WAVEHDR(pwh);
        if ((hdr.dwFlags & WAVEHDR.WHDR_INQUEUE)!=0)
            return WinMM.WAVERR_STILLPLAYING;
        hdr.dwFlags &= ~WAVEHDR.WHDR_PREPARED;
        hdr.dwFlags |= WAVEHDR.WHDR_DONE;
        hdr.writeFlags();
        return WinMM.MMSYSERR_NOERROR;
    }

    // MMRESULT waveOutWrite(HWAVEOUT hwo, LPWAVEHDR pwh, UINT cbwh)
    static public int waveOutWrite(int hwo, int pwh, int cbwh) {
        if (pwh==0 || cbwh<WAVEHDR.SIZE)
            return WinMM.MMSYSERR_INVALPARAM;

        WaveObject obj = WaveObject.get(hwo);
        if (obj == null)
            return WinMM.MMSYSERR_INVALHANDLE;

        WAVEHDR hdr = new WAVEHDR(pwh);
        if (hdr.lpData==0 || (hdr.dwFlags & WAVEHDR.WHDR_PREPARED)==0)
            return WinMM.WAVERR_UNPREPARED;

        if ((hdr.dwFlags & WAVEHDR.WHDR_INQUEUE)!=0)
            return WinMM.WAVERR_STILLPLAYING;

        hdr.dwFlags |= WAVEHDR.WHDR_INQUEUE;
        hdr.dwFlags &= ~WAVEHDR.WHDR_DONE;
        hdr.reserved = pwh;
        hdr.writeFlags();

        hdr.data = new byte[hdr.dwBufferLength];
        Memory.mem_memcpy(hdr.data, 0, hdr.lpData, hdr.dwBufferLength);

        if (pwh == 0xb0004afc) {
            int ii=0;
        }
        synchronized(obj.thread.buffers) {
            obj.thread.buffers.add(hdr);
            obj.thread.buffers.notify();
        }
        return WinMM.MMSYSERR_NOERROR;
    }
}
