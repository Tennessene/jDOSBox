package jdos.win.builtin.winmm;

import jdos.win.builtin.WinAPI;
import jdos.win.builtin.directx.Guid;

public class WAVEFORMATEXTENSIBLE extends WinAPI {
    public WAVEFORMATEXTENSIBLE(WAVEFORMATEX wfx, int address) {
        this.Format = wfx;
        if (wfx.wFormatTag == WAVE_FORMAT_EXTENSIBLE) {
            wSamplesPerBlock = readw(address+WAVEFORMATEX.SIZE);
            dwChannelMask = readw(address+WAVEFORMATEX.SIZE+2);
            SubFormat = new Guid(address+WAVEFORMATEX.SIZE+6);
        }
    }

    public WAVEFORMATEX Format;
    public int wSamplesPerBlock;
    public int dwChannelMask;
    public Guid SubFormat;
}
