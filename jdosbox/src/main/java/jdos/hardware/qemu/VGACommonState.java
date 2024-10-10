package jdos.hardware.qemu;

import jdos.cpu.Paging;
import jdos.gui.Render;
import jdos.hardware.RAM;

public class VGACommonState {
    static public final int DISPLAY_ADDRESS = 0x60000000;

    public interface Retrace {
        public int call(VGACommonState s);
    }

    public interface vga_update_retrace_info_fn {
        public void call(VGACommonState s);
    }

    public interface rgb_to_pixel_dup_func {
        public int call(int r, int g, int b);
    }

    public interface get_func {
        public int call(VGACommonState s);
    }

    public interface cursor_draw_line_func {
        public void call(VGACommonState s, int d, int y);
    }

    public int readb(int offset) {
        if (offset>=DISPLAY_ADDRESS) {
            offset-=DISPLAY_ADDRESS;
            return (short)((Render.render.src.outWrite32[(offset >>> 2)] >>> ((offset & 0x3) << 3)) & 0xFF);
        }
        return RAM.readb(vram_ptr + offset);
    }
    public int readw(int offset) {
        if (offset>=DISPLAY_ADDRESS) {
            offset-=DISPLAY_ADDRESS;
            int rem = offset & 0x3;
            int[] local = Render.render.src.outWrite32;
            int index = (offset >>> 2);
            int val = local[index] >>> (rem << 3);
            if (rem == 3) {
              val |= local[index + 1] << 8;
            }
            return val & 0xFFFF;
        }
        return RAM.readw(vram_ptr + offset);
    }
    public int readd(int offset) {
        if (offset>=DISPLAY_ADDRESS) {
            offset-=DISPLAY_ADDRESS;
            int rem = (offset & 0x3);
            if (rem == 0) {
              return Render.render.src.outWrite32[offset >>> 2];
            }
            int off = rem << 3;
            int[] local = Render.render.src.outWrite32;
            int index = (offset >>> 2);
            return local[index] >>> off | local[index+1] << (32-off);
        }
        return RAM.readd(vram_ptr + offset);
    }
    public void writeb(int offset, int value) {
        if (offset>=DISPLAY_ADDRESS) {
            offset-=DISPLAY_ADDRESS;
            int off = (offset & 0x3) << 3;
            int[] local = Render.render.src.outWrite32;
            int mask = ~(0xFF << off);
            int index = (offset >>> 2);
            int val = local[index] & mask | (value & 0xFF) << off;
            local[index] = val;
        } else {
            RAM.writeb(vram_ptr + offset, value);
        }
    }
    public void writew(int offset, int value) {
        if (offset>=DISPLAY_ADDRESS) {
            offset-=DISPLAY_ADDRESS;
            int rem = (offset & 0x3);
            int[] local = Render.render.src.outWrite32;
            int index = (offset >>> 2);
            value&=0xFFFF;
            if (rem == 3) {
              local[index] = (local[index] & 0xFFFFFF | value << 24);
              index++;
              local[index] = (local[index] & 0xFFFFFF00 | value >>> 8);
            } else {
                int off = rem << 3;
              int mask = ~(0xFFFF << off);
              local[index] = (local[index] & mask | value << off);
            }
        } else {
            RAM.writew(vram_ptr + offset, value);
        }
    }
    public void writed(int offset, int value) {
        if (offset>=DISPLAY_ADDRESS) {
            offset-=DISPLAY_ADDRESS;
            int rem = (offset & 0x3);
            if (rem == 0) {
                try {
                    Render.render.src.outWrite32[offset >>> 2] = value;
                } catch (Exception e) {
                    e.printStackTrace();;
                }
            } else {
              int index = (offset >>> 2);
              int[] local = Render.render.src.outWrite32;
              int off = rem << 3;
              int mask = -1 << off;
              local[index] = (local[index] & ~mask) | (value << off);
              index++;
              local[index] = (local[index] & mask) | (value >>> (32-off));
            }
        } else {
            RAM.writed(vram_ptr + offset, value);
        }
    }
    public void memmove(int dst, int src, int len) {
        for (int i = 0; i < len; i++) {
            writeb(dst++, readb(src++));
        }
    }
    public void memset(int dst, int value, int len) {
        for (int i = 0; i < len; i++) {
            writeb(dst++, value);
        }
    }
    //MemoryRegion *legacy_address_space;
    Paging.PageHandler vga_mem;
    int vram_ptr;
    MemoryRegion vram;
    MemoryRegion vram_vbe;
    int vram_size;
    int vram_size_mb; /* property */
    int latch;
    MemoryRegion chain4_alias;
    int sr_index;
    int[] sr = new int[256];
    int gr_index;
    int[] gr = new int[256];
    int ar_index;
    int[] ar = new int[21];
    int ar_flip_flop;
    int cr_index;
    int[] cr = new int[256]; /* CRT registers */
    int msr; /* Misc Output Register */
    int fcr; /* Feature Control Register */
    int st00; /* status 0 */
    int st01; /* status 1 */
    int dac_state;
    int dac_sub_index;
    int dac_read_index;
    int dac_write_index;
    int[] dac_cache = new int[3]; /* used when writing */
    boolean dac_8bit;
    int[] palette = new int[768];
    int bank_offset;
    get_func get_bpp;
    get_func getLineOffset;
    get_func getStartAddress;
    get_func getLineCompare;

    get_func get_resolutionCx;
    get_func get_resolutionCy;
    // Bochs VBE
    int vbe_index;
    int[] vbe_regs = new int[VGA_header.VBE_DISPI_INDEX_NB];
    int vbe_start_addr;
    int vbe_line_offset;
    int vbe_bank_mask;
    int vbe_mapped;

    /* display refresh support */
    DisplayState ds = new DisplayState();
    int[] font_offsets = new int[2];
    int graphic_mode;
    int shift_control;
    int double_scan;
    int line_offset;
    int line_compare;
    int start_addr;
    int plane_updated;
    int last_line_offset;
    int last_cw, last_ch;
    int last_width, last_height; /* in chars or pixels */
    int last_scr_width, last_scr_height; /* in pixels */
    int last_depth; /* in bits */
    int cursor_start, cursor_end;
    boolean cursor_visible_phase;
    long cursor_blink_time;
    int cursor_offset;
    rgb_to_pixel_dup_func rgb_to_pixel;
    //vga_hw_update_ptr update;
    //vga_hw_invalidate_ptr invalidate;
    //vga_hw_screen_dump_ptr screen_dump;
    //vga_hw_text_update_ptr text_update;
    /* hardware mouse cursor support */
    int[] invalidated_y_table = new int[VGA_header.VGA_MAX_HEIGHT / 32];
    //void (*cursor_invalidate)(struct VGACommonState *s);
    cursor_draw_line_func cursor_draw_line;
    /* tell for each page if it has been updated since the last time */
    int[] last_palette = new int[256];
    int[] renderer_palette = new int[256];
    int[] last_ch_attr = new int[VGA_header.CH_ATTR_SIZE]; /* XXX: make it dynamic */
    /* retrace */
    Retrace retrace;
    vga_update_retrace_info_fn update_retrace_info;
    vga_precise_retrace retrace_info = new vga_precise_retrace();
}
