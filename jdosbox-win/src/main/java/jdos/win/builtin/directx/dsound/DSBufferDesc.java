package jdos.win.builtin.directx.dsound;

import jdos.hardware.Memory;
import jdos.win.builtin.winmm.WAVEFORMATEX;

public class DSBufferDesc {
    static public final int SIZE = 20;

    public final static int DSBCAPS_PRIMARYBUFFER =     0x00000001;
    public final static int DSBCAPS_STATIC =            0x00000002;
    public final static int DSBCAPS_LOCHARDWARE =       0x00000004;
    public final static int DSBCAPS_LOCSOFTWARE =       0x00000008;
    public final static int DSBCAPS_CTRL3D =            0x00000010;
    public final static int DSBCAPS_CTRLFREQUENCY =     0x00000020;
    public final static int DSBCAPS_CTRLPAN =           0x00000040;
    public final static int DSBCAPS_CTRLVOLUME =        0x00000080;
    public final static int DSBCAPS_CTRLDEFAULT =       0x000000E0;  /* Pan + volume + frequency. */
    public final static int DSBCAPS_CTRLPOSITIONNOTIFY= 0x00000100;
    public final static int DSBCAPS_CTRLFX =            0x00000200;
    public final static int DSBCAPS_CTRLALL =           0x000001F0;  /* All control capabilities */
    public final static int DSBCAPS_STICKYFOCUS =       0x00004000;
    public final static int DSBCAPS_GLOBALFOCUS =       0x00008000;
    public final static int DSBCAPS_GETCURRENTPOSITION2=0x00010000;  /* More accurate play cursor under emulation*/
    public final static int DSBCAPS_MUTE3DATMAXDISTANCE=0x00020000;
    public final static int DSBCAPS_LOCDEFER =          0x00040000;

    public DSBufferDesc(int address) {
        dwSize = Memory.mem_readd(address);address+=4;
        dwFlags = Memory.mem_readd(address);address+=4;
        dwBufferBytes = Memory.mem_readd(address);address+=4;
        dwReserved = Memory.mem_readd(address);address+=4;
        address = Memory.mem_readd(address);
        if (address != 0)
            lpwfxFormat = new WAVEFORMATEX(address);
    }

    public int dwSize;
    public int dwFlags;
    public int dwBufferBytes;
    public int dwReserved;
    public WAVEFORMATEX lpwfxFormat = null;
}
