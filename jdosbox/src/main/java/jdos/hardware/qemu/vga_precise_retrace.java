package jdos.hardware.qemu;

public class vga_precise_retrace {
    public void reset() {
        ticks_per_char = 0;
        total_chars = 0;
        htotal = 0;
        hend = 0;
        vstart = 0;
        vend = 0;
        freq = 0;
    }
    long ticks_per_char;
    long total_chars;
    int htotal;
    int hstart;
    int hend;
    int vstart;
    int vend;
    int freq;
}
