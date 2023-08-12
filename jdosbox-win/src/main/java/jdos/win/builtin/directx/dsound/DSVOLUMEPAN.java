package jdos.win.builtin.directx.dsound;

public class DSVOLUMEPAN {
    public DSVOLUMEPAN() {

    }

    public DSVOLUMEPAN(DSVOLUMEPAN vol) {
        this.dwTotalLeftAmpFactor = vol.dwTotalLeftAmpFactor;
        this.dwTotalRightAmpFactor = vol.dwTotalRightAmpFactor;
        this.lVolume = vol.lVolume;
        this.dwVolAmpFactor = vol.dwVolAmpFactor;
        this.lPan = vol.lPan;
        this.dwPanLeftAmpFactor = vol.dwPanLeftAmpFactor;
        this.dwPanRightAmpFactor = vol.dwPanRightAmpFactor;
    }
    public int dwTotalLeftAmpFactor;
    public int dwTotalRightAmpFactor;
    public int lVolume;
    public int dwVolAmpFactor;
    public int lPan;
    public int dwPanLeftAmpFactor;
    public int dwPanRightAmpFactor;
}
