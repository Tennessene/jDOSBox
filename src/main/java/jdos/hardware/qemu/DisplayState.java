package jdos.hardware.qemu;

import jdos.gui.Render;

public class DisplayState {
    public int ds_get_bits_per_pixel() {
        return Render.render.src.bpp;
    }

    public boolean is_surface_bgr() {
        return false;
    }

    public void dpy_update(int x, int y, int cx, int cy) {
        int ii=0;
    }

    public int ds_get_linesize() {
        return Render.render.src.outPitch;
    }

    public int ds_get_data() {
        return 0x60000000;
    }
}
