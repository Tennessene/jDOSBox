package jdos.win.builtin.winmm;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.user32.WinWindow;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.WinMCI;
import jdos.win.system.WinMidi;
import jdos.win.system.WinSystem;
import jdos.win.utils.Ptr;

import java.io.File;

public class WinMM extends BuiltinModule {
    public WinMM(Loader loader, int handle) {
        super(loader, "WinMM.dll", handle);
        add(WinJoystick.class, "joyGetNumDevs", new String[0]);
        add(mciSendCommandA);
        add(Mci.class, "mciSendStringA", new String[] {"(STRING)lpszCommand", "(HEX)lpszReturnString", "cchReturn", "hwndCallback", "result", "01(STRING)lpszReturnString"});
        add(mixerGetNumDevs);
        add(Mmio.class, "mmioAdvance", new String[] {"hmmio", "(HEX)lpmmioinfo", "(HEX)wFlags"});
        add(Mmio.class, "mmioAscend", new String[] {"hmmio", "(HEX)lpck", "(HEX)wFlags"});
        add(Mmio.class, "mmioClose", new String[] {"hmmio", "(HEX)wFlags"});
        add(Mmio.class, "mmioDescend", new String[] {"hmmio", "(HEX)lpck", "(HEX)lpckParent", "(HEX)wFlags"});
        add(Mmio.class, "mmioGetInfo", new String[] {"hmmio", "(HEX)lpmmioinfo", "(HEX)wFlags"});
        add(Mmio.class, "mmioOpenA", new String[] {"(STRING)szFilename", "(HEX)lpmmioinfo", "(HEX)dwOpenFlags"});
        add(Mmio.class, "mmioRead", new String[] {"hmmio", "(HEX)pch", "cch"});
        add(Mmio.class, "mmioSeek", new String[] {"hmmio", "lOffset", "iOrigin"});
        add(Mmio.class, "mmioSetInfo", new String[] {"hmmio", "(HEX)lpmmioinfo", "(HEX)wFlags"});
        add(timeGetTime);
        add(Waveform.class, "waveOutClose", new String[] {"(HEX)hwo"});
        add(Waveform.class, "waveOutOpen", new String[] {"(HEX)lphWaveOut", "uDeviceID", "(HEX)pwfx", "(HEX)dwCallback", "dwCallbackInstance", "(HEX)fdwOpen"});
        add(Waveform.class, "waveOutPrepareHeader", new String[] {"(HEX)hwo", "(HEX)pwh", "cbwh"});
        add(Waveform.class, "waveOutReset", new String[] {"(HEX)hwo"});
        add(Waveform.class, "waveOutUnprepareHeader", new String[] {"(HEX)hwo", "(HEX)pwh", "cbwh"});
        add(Waveform.class, "waveOutWrite", new String[] {"(HEX)hwo", "(HEX)pwh", "cbwh"});
    }

    // :TODO: This code can use a lot of work
    // MCIERROR mciSendCommand(MCIDEVICEID IDDevice, UINT uMsg, DWORD_PTR fdwCommand, DWORD_PTR dwParam)
    private Callback.Handler mciSendCommandA = new HandlerBase() {
        static final private int MCI_OPEN_SHAREABLE =            0x00000100;
        static final private int MCI_OPEN_ELEMENT =              0x00000200;
        static final private int MCI_OPEN_ALIAS =                0x00000400;
        static final private int MCI_OPEN_ELEMENT_ID =           0x00000800;
        static final private int MCI_OPEN_TYPE_ID =              0x00001000;
        static final private int MCI_OPEN_TYPE =                 0x00002000;

        static final private int MCI_DEVTYPE_VCR =               513;
        static final private int MCI_DEVTYPE_VIDEODISC =         514;
        static final private int MCI_DEVTYPE_OVERLAY =           515;
        static final private int MCI_DEVTYPE_CD_AUDIO =          516;
        static final private int MCI_DEVTYPE_DAT =               517;
        static final private int MCI_DEVTYPE_SCANNER =           518;
        static final private int MCI_DEVTYPE_ANIMATION =         519;
        static final private int MCI_DEVTYPE_DIGITAL_VIDEO =     520;
        static final private int MCI_DEVTYPE_OTHER =             521;
        static final private int MCI_DEVTYPE_WAVEFORM_AUDIO =    522;
        static final private int MCI_DEVTYPE_SEQUENCER =         523;

        static final private int MCI_NOTIFY =                    0x00000001;
        static final private int MCI_WAIT =                      0x00000002;
        static final private int MCI_FROM =                      0x00000004;
        static final private int MCI_TO =                        0x00000008;
        static final private int MCI_TRACK =                     0x00000010;

        public java.lang.String getName() {
            return "WinMM.mciSendCommandA";
        }
        public void onCall() {
            int IDDevice = CPU.CPU_Pop32();
            int uMsg = CPU.CPU_Pop32();
            int fdwCommand = CPU.CPU_Pop32();
            int dwParam = CPU.CPU_Pop32();
            switch (uMsg) {
                case 0x803: // MCI_OPEN
                {
                    int callback = Memory.mem_readd(dwParam);
                    int deviceId = Memory.mem_readd(dwParam+4);
                    int lpstrDeviceType = Memory.mem_readd(dwParam+8);
                    int lpstrElementName = Memory.mem_readd(dwParam+12);
                    int lpstrAlias = Memory.mem_readd(dwParam+16);
                    boolean midi = false;

                    if ((fdwCommand & MCI_OPEN_TYPE)!=0) {
                        if ((fdwCommand & MCI_OPEN_TYPE_ID) != 0) {
                            int id = lpstrDeviceType & 0xFFFF;
                            int index = lpstrDeviceType >>> 16;
                            if (id == MCI_DEVTYPE_SEQUENCER)
                                midi = true;
                        } else {
                            String deviceType = new LittleEndianFile(lpstrDeviceType).readCString();
                            if (deviceType.equalsIgnoreCase("sequencer"))
                                midi = true;
                        }
                        if ((fdwCommand & MCI_OPEN_ELEMENT)!=0) {
                            if ((fdwCommand & MCI_OPEN_ELEMENT_ID)!=0) {

                            } else {
                                String elementName = new LittleEndianFile(lpstrElementName).readCString();
                                File file = WinSystem.getCurrentProcess().getFile(elementName);
                                if (file.exists()) {
                                    if (midi) {
                                        WinMidi winMidi = WinMidi.create();
                                        if (winMidi.setFile(file)) {
                                            Memory.mem_writed(dwParam + 4, winMidi.getHandle());
                                            CPU_Regs.reg_eax.dword = 0;
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
                case 0x804: // MCI_CLOSE
                {
                    WinMCI mci = WinMCI.getMCI(IDDevice);
                    if (mci == null) {

                    } else {
                        int dwCallback = 0;
                        if ((fdwCommand & MCI_NOTIFY)!=0)
                            dwCallback = Memory.mem_readd(dwParam);
                        mci.close(dwCallback, (fdwCommand & MCI_WAIT) != 0);
                        CPU_Regs.reg_eax.dword = 0;
                        mci.close();
                        return;
                    }
                }
                break;
                case 0x806: // MCI_PLAY
                {
                    WinMCI mci = WinMCI.getMCI(IDDevice);
                    if (mci == null) {

                    } else {
                        int dwCallback = 0;
                        int dwFrom = 0;
                        int dwTo = -1;

                        if ((fdwCommand & MCI_NOTIFY)!=0)
                            dwCallback = Memory.mem_readd(dwParam);
                        if ((fdwCommand & MCI_FROM)!=0)
                            dwFrom = Memory.mem_readd(dwParam+4);
                        if ((fdwCommand & MCI_TO)!=0)
                            dwTo = Memory.mem_readd(dwParam+8);
                        mci.play(dwFrom, dwTo, dwCallback, (fdwCommand & MCI_WAIT) != 0);
                        CPU_Regs.reg_eax.dword = 0;
                        return;
                    }
                }
                break;
                case 0x808: // MCI_STOP
                {
                    WinMCI mci = WinMCI.getMCI(IDDevice);
                    if (mci == null) {

                    } else {
                        int dwCallback = 0;
                        if ((fdwCommand & MCI_NOTIFY)!=0)
                            dwCallback = Memory.mem_readd(dwParam);
                        mci.stop(dwCallback, (fdwCommand & MCI_WAIT) != 0);
                        CPU_Regs.reg_eax.dword = 0;
                        return;
                    }
                }
                break;

            }
            Win.panic(getName()+" unhanded uMsg=0x"+ Ptr.toString(uMsg)+" fdwCommand=0x"+Ptr.toString(fdwCommand));
            CPU_Regs.reg_eax.dword = 1; // error
        }
    };

    // UINT mixerGetNumDevs(void)
    private Callback.Handler mixerGetNumDevs = new HandlerBase() {
        public java.lang.String getName() {
            return "WinMM.mixerGetNumDevs";
        }
        public void onCall() {
            // :TODO:
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // DWORD timeGetTime(void)
    private Callback.Handler timeGetTime = new HandlerBase() {
        public java.lang.String getName() {
            return "WinMM.timeGetTime";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getTickCount();
        }
    };

    public static int WINMM_CheckCallback(int dwCallback, int fdwOpen, boolean mixer) {
        switch (fdwOpen & CALLBACK_TYPEMASK) {
        case CALLBACK_NULL:     /* dwCallback need not be NULL */
            break;
        case CALLBACK_WINDOW:
            if (dwCallback!=0 && WinWindow.IsWindow(dwCallback)==0)
                return MMSYSERR_INVALPARAM;
            break;

        case CALLBACK_FUNCTION:
            /* a NULL cb is acceptable since w2k, MMSYSERR_INVALPARAM earlier */
            if (mixer)
                return MMSYSERR_INVALFLAG; /* since w2k, MMSYSERR_NOTSUPPORTED earlier */
            break;
        case CALLBACK_THREAD:
        case CALLBACK_EVENT:
            if (mixer) /* FIXME: mixer supports THREAD+EVENT since w2k */
                return MMSYSERR_NOTSUPPORTED; /* w9X */
            break;
        default:
            warn("Unknown callback type "+HIWORD(fdwOpen));
        }
        return MMSYSERR_NOERROR;
    }
}
