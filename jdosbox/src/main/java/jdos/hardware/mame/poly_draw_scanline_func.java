package jdos.hardware.mame;

public interface poly_draw_scanline_func {
    public void call(short[] dest, int destOffset, int scanline, poly_extent extent, poly_extra_data extradata, int threadid);
}
