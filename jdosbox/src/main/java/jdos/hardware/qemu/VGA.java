package jdos.hardware.qemu;

import jdos.Dosbox;
import jdos.cpu.Paging;
import jdos.gui.Main;
import jdos.gui.Render;
import jdos.hardware.*;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.SVGACards;

import java.util.Arrays;

// Ported to Java by James Bryant
/*
     * QEMU VGA Emulator.
     *
     * Copyright (c) 2003 Fabrice Bellard
     *
     * Permission is hereby granted, free of charge, to any person obtaining a copy
     * of this software and associated documentation files (the "Software"), to deal
     * in the Software without restriction, including without limitation the rights
     * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     * copies of the Software, and to permit persons to whom the Software is
     * furnished to do so, subject to the following conditions:
     *
     * The above copyright notice and this permission notice shall be included in
     * all copies or substantial portions of the Software.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
     * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
     * THE SOFTWARE.
     */
public class VGA extends VGA_header {
    static public final int ST01_V_RETRACE      = 0x08;
    static public final int ST01_DISP_ENABLE    = 0x01;
    static public final boolean CONFIG_BOCHS_VBE = true;

    static public final boolean DEBUG_VGA = false;
    static public final boolean DEBUG_VGA_MEM = false;
    static public final boolean DEBUG_VGA_REG = false;

    static public final boolean DEBUG_BOCHS_VBE = true;

    /* 16 state changes per vertical frame @60 Hz */
    static public final int VGA_TEXT_CURSOR_PERIOD_MS = (1000 * 2 * 16 / 60);

    /*
     * Video Graphics Array (VGA)
     *
     * Chipset docs for original IBM VGA:
     * http://www.mcamafia.de/pdf/ibm_vgaxga_trm2.pdf
     *
     * FreeVGA site:
     * http://www.osdever.net/FreeVGA/home.htm
     *
     * Standard VGA features and Bochs VBE extensions are implemented.
     */

    /* force some bits to zero */
    static public final int[] sr_mask = new int[] {
        0x03,
        0x3d,
        0x0f,
        0x3f,
        0x0e,
        0x00,
        0x00,
        0xff,
    };

    static public final int[] gr_mask = new int[] {
        0x0f, /* 0x00 */
        0x0f, /* 0x01 */
        0x0f, /* 0x02 */
        0x1f, /* 0x03 */
        0x03, /* 0x04 */
        0x7b, /* 0x05 */
        0x0f, /* 0x06 */
        0x0f, /* 0x07 */
        0xff, /* 0x08 */
        0x00, /* 0x09 */
        0x00, /* 0x0a */
        0x00, /* 0x0b */
        0x00, /* 0x0c */
        0x00, /* 0x0d */
        0x00, /* 0x0e */
        0x00, /* 0x0f */
    };

    static private int GET_PLANE(int data, int p) {return (((data) >> ((p) * 8)) & 0xff);}

    static public final int[] mask16 = new int[] {
        0x00000000,
        0x000000ff,
        0x0000ff00,
        0x0000ffff,
        0x00ff0000,
        0x00ff00ff,
        0x00ffff00,
        0x00ffffff,
        0xff000000,
        0xff0000ff,
        0xff00ff00,
        0xff00ffff,
        0xffff0000,
        0xffff00ff,
        0xffffff00,
        0xffffffff,
    };

    static public final int[] dmask16 = new int[] {
        0x00000000,
        0xff000000,
        0x00ff0000,
        0xffff0000,
        0x0000ff00,
        0xff00ff00,
        0x00ffff00,
        0xffffff00,
        0xff000000,
        0xff0000ff,
        0x00ff00ff,
        0xffff00ff,
        0x0000ffff,
        0xff00ffff,
        0x00ffffff,
        0xffffffff,
    };

    static public final int[] dmask4 = new int[] {
        0x00000000,        
        0xffff0000,
        0x0000ffff,
        0xffffffff,
    };

    static public final int[] expand4 = new int[256];
    static public final int[] expand2 = new int[256];
    static public final int[] expand4to8 = new int[16];

    static private class vga_dumb_update_retrace_info implements VGACommonState.vga_update_retrace_info_fn {
        public void call(VGACommonState s) {
        }
    }

    static private class vga_precise_update_retrace_info implements VGACommonState.vga_update_retrace_info_fn {
        public void call(VGACommonState s) {
            int htotal_chars;
            int hretr_start_char;
            int hretr_skew_chars;
            int hretr_end_char;

            int vtotal_lines;
            int vretr_start_line;
            int vretr_end_line;

            int dots;
            int clocking_mode;
            int clock_sel;
            final int clk_hz[] = {25175000, 28322000, 25175000, 25175000};
            long chars_per_sec;
            vga_precise_retrace r = s.retrace_info;

            htotal_chars = s.cr[VGA_CRTC_H_TOTAL] + 5;
            hretr_start_char = s.cr[VGA_CRTC_H_SYNC_START];
            hretr_skew_chars = (s.cr[VGA_CRTC_H_SYNC_END] >> 5) & 3;
            hretr_end_char = s.cr[VGA_CRTC_H_SYNC_END] & 0x1f;

            vtotal_lines = (s.cr[VGA_CRTC_V_TOTAL] | (((s.cr[VGA_CRTC_OVERFLOW] & 1) | ((s.cr[VGA_CRTC_OVERFLOW] >> 4) & 2)) << 8)) + 2;
            vretr_start_line = s.cr[VGA_CRTC_V_SYNC_START] | ((((s.cr[VGA_CRTC_OVERFLOW] >> 2) & 1) | ((s.cr[VGA_CRTC_OVERFLOW] >> 6) & 2)) << 8);
            vretr_end_line = s.cr[VGA_CRTC_V_SYNC_END] & 0xf;

            clocking_mode = (s.sr[VGA_SEQ_CLOCK_MODE] >> 3) & 1;
            clock_sel = (s.msr >> 2) & 3;
            dots = (s.msr & 1)!=0 ? 8 : 9;

            chars_per_sec = clk_hz[clock_sel] / dots;

            htotal_chars <<= clocking_mode;

            r.total_chars = vtotal_lines * htotal_chars;
            if (r.freq!=0) {
                r.ticks_per_char = Qemu.get_ticks_per_sec() / (r.total_chars * r.freq);
            } else {
                r.ticks_per_char = Qemu.get_ticks_per_sec() / chars_per_sec;
            }

            r.vstart = vretr_start_line;
            r.vend = r.vstart + vretr_end_line + 1;

            r.hstart = hretr_start_char + hretr_skew_chars;
            r.hend = r.hstart + hretr_end_char + 1;
            r.htotal = htotal_chars;
        }
    }

    static private class vga_precise_retrace_impl implements VGACommonState.Retrace {
        public int call(VGACommonState s) {
            vga_precise_retrace r = s.retrace_info;
            int val = s.st01 & ~(ST01_V_RETRACE | ST01_DISP_ENABLE);

            if (r.total_chars!=0) {
                int cur_line, cur_line_char, cur_char;
                long cur_tick;

                cur_tick = Qemu.qemu_get_clock_ns();

                cur_char = (int)((cur_tick / r.ticks_per_char) % r.total_chars);
                cur_line = cur_char / r.htotal;

                if (cur_line >= r.vstart && cur_line <= r.vend) {
                    val |= ST01_V_RETRACE | ST01_DISP_ENABLE;
                } else {
                    cur_line_char = cur_char % r.htotal;
                    if (cur_line_char >= r.hstart && cur_line_char <= r.hend) {
                        val |= ST01_DISP_ENABLE;
                    }
                }

                return val;
            } else {
                return s.st01 ^ (ST01_V_RETRACE | ST01_DISP_ENABLE);
            }
        }
    }

    static private class vga_dumb_retrace_impl implements VGACommonState.Retrace {
        public int call(VGACommonState s) {
            return s.st01 ^ (ST01_V_RETRACE | ST01_DISP_ENABLE);
        }
    }

    public static boolean vga_ioport_invalid(VGACommonState s, int addr)
    {
        if ((s.msr & VGA_MIS_COLOR)!=0) {
            /* Color */
            return (addr >= 0x3b0 && addr <= 0x3bf);
        } else {
            /* Monochrome */
            return (addr >= 0x3d0 && addr <= 0x3df);
        }
    }

    private static class vga_ioport_write implements IoHandler.IO_WriteHandler {
        VGACommonState s;
        public vga_ioport_write(VGACommonState s) {
            this.s = s;
        }

        public void call(/*Bitu*/int addr, /*Bitu*/int val, /*Bitu*/int len) {
            int index;

            /* check port range access depending on color/monochrome mode */
            if (vga_ioport_invalid(s, addr)) {
                return;
            }
            if (DEBUG_VGA)
                System.out.println("VGA: write addr=0x"+Integer.toHexString(addr)+" data=0x"+Integer.toHexString(val));

            switch(addr) {
            case VGA_ATT_W:
                if (s.ar_flip_flop == 0) {
                    val &= 0x3f;
                    s.ar_index = val;
                } else {
                    index = s.ar_index & 0x1f;
                    switch(index) {
                    case VGA_ATC_PALETTE0:case VGA_ATC_PALETTE1:case VGA_ATC_PALETTE2:case VGA_ATC_PALETTE3:case VGA_ATC_PALETTE4:case VGA_ATC_PALETTE5:case VGA_ATC_PALETTE6:case VGA_ATC_PALETTE7:
                    case VGA_ATC_PALETTE8:case VGA_ATC_PALETTE9:case VGA_ATC_PALETTEA:case VGA_ATC_PALETTEB:case VGA_ATC_PALETTEC:case VGA_ATC_PALETTED:case VGA_ATC_PALETTEE:case VGA_ATC_PALETTEF:
                        s.ar[index] = val & 0x3f;
                        break;
                    case VGA_ATC_MODE:
                        s.ar[index] = val & ~0x10;
                        break;
                    case VGA_ATC_OVERSCAN:
                        s.ar[index] = val;
                        break;
                    case VGA_ATC_PLANE_ENABLE:
                        s.ar[index] = val & ~0xc0;
                        break;
                    case VGA_ATC_PEL:
                        s.ar[index] = val & ~0xf0;
                        break;
                    case VGA_ATC_COLOR_PAGE:
                        s.ar[index] = val & ~0xf0;
                        break;
                    default:
                        break;
                    }
                }
                s.ar_flip_flop ^= 1;
                break;
            case VGA_MIS_W:
                s.msr = val & ~0x10;
                s.update_retrace_info.call(s);
                break;
            case VGA_SEQ_I:
                s.sr_index = val & 7;
                break;
            case VGA_SEQ_D:
                if (DEBUG_VGA_REG)
                    System.out.println("vga: write SR"+Integer.toHexString(s.sr_index)+" = 0x"+Integer.toHexString(val));
                s.sr[s.sr_index] = val & sr_mask[s.sr_index];
                if (s.sr_index == VGA_SEQ_CLOCK_MODE) {
                    s.update_retrace_info.call(s);
                }
                //vga_update_memory_access(s);
                break;
            case VGA_PEL_IR:
                s.dac_read_index = val;
                s.dac_sub_index = 0;
                s.dac_state = 3;
                break;
            case VGA_PEL_IW:
                s.dac_write_index = val;
                s.dac_sub_index = 0;
                s.dac_state = 0;
                break;
            case VGA_PEL_D:
                s.dac_cache[s.dac_sub_index] = val;
                if (++s.dac_sub_index == 3) {
                    System.arraycopy(s.dac_cache, 0, s.palette, s.dac_write_index * 3, 3);
                    s.dac_sub_index = 0;
                    s.dac_write_index++;
                }
                break;
            case VGA_GFX_I:
                s.gr_index = val & 0x0f;
                break;
            case VGA_GFX_D:
                if (DEBUG_VGA_REG)
                    System.out.println("vga: write GR"+Integer.toHexString(s.gr_index)+" = 0x"+Integer.toHexString(val));
                s.gr[s.gr_index] = val & gr_mask[s.gr_index];
                //vga_update_memory_access(s);
                break;
            case VGA_CRT_IM:
            case VGA_CRT_IC:
                s.cr_index = val;
                break;
            case VGA_CRT_DM:
            case VGA_CRT_DC:
                if (DEBUG_VGA_REG)
                    System.out.println("vga: write CR"+Integer.toHexString(s.cr_index)+" = 0x"+Integer.toHexString(val));
                /* handle CR0-7 protection */
                if ((s.cr[VGA_CRTC_V_SYNC_END] & VGA_CR11_LOCK_CR0_CR7)!=0 && s.cr_index <= VGA_CRTC_OVERFLOW) {
                    /* can always write bit 4 of CR7 */
                    if (s.cr_index == VGA_CRTC_OVERFLOW) {
                        s.cr[VGA_CRTC_OVERFLOW] = (s.cr[VGA_CRTC_OVERFLOW] & ~0x10) |
                            (val & 0x10);
                    }
                    return;
                }
                s.cr[s.cr_index] = val;

                switch(s.cr_index) {
                case VGA_CRTC_H_TOTAL:
                case VGA_CRTC_H_SYNC_START:
                case VGA_CRTC_H_SYNC_END:
                case VGA_CRTC_V_TOTAL:
                case VGA_CRTC_OVERFLOW:
                case VGA_CRTC_V_SYNC_END:
                case VGA_CRTC_MODE:
                    s.update_retrace_info.call(s);
                    break;
                }
                break;
            case VGA_IS1_RM:
            case VGA_IS1_RC:
                s.fcr = val & 0x10;
                break;
            }
        }
    }
    private static class vga_ioport_read implements IoHandler.IO_ReadHandler {
        VGACommonState s;
        public vga_ioport_read(VGACommonState s) {
            this.s = s;
        }
        public /*Bitu*/int call(/*Bitu*/int addr, /*Bitu*/int len) {
            int val, index;

            if (vga_ioport_invalid(s, addr)) {
                val = 0xff;
            } else {
                switch(addr) {
                case VGA_ATT_W:
                    if (s.ar_flip_flop == 0) {
                        val = s.ar_index;
                    } else {
                        val = 0;
                    }
                    break;
                case VGA_ATT_R:
                    index = s.ar_index & 0x1f;
                    if (index < VGA_ATT_C) {
                        val = s.ar[index];
                    } else {
                        val = 0;
                    }
                    break;
                case VGA_MIS_W:
                    val = s.st00;
                    break;
                case VGA_SEQ_I:
                    val = s.sr_index;
                    break;
                case VGA_SEQ_D:
                    val = s.sr[s.sr_index];
                    if (DEBUG_VGA_REG)
                        System.out.println("vga: read SR"+Integer.toHexString(s.sr_index)+" = 0x"+Integer.toHexString(val));
                    break;
                case VGA_PEL_IR:
                    val = s.dac_state;
                    break;
                case VGA_PEL_IW:
                    val = s.dac_write_index;
                    break;
                case VGA_PEL_D:
                    val = s.palette[s.dac_read_index * 3 + s.dac_sub_index];
                    if (++s.dac_sub_index == 3) {
                        s.dac_sub_index = 0;
                        s.dac_read_index++;
                    }
                    break;
                case VGA_FTC_R:
                    val = s.fcr;
                    break;
                case VGA_MIS_R:
                    val = s.msr;
                    break;
                case VGA_GFX_I:
                    val = s.gr_index;
                    break;
                case VGA_GFX_D:
                    val = s.gr[s.gr_index];
                    if (DEBUG_VGA_REG)
                        System.out.println("vga: read GR"+Integer.toHexString(s.gr_index)+" = 0x"+Integer.toHexString(val));
                    break;
                case VGA_CRT_IM:
                case VGA_CRT_IC:
                    val = s.cr_index;
                    break;
                case VGA_CRT_DM:
                case VGA_CRT_DC:
                    val = s.cr[s.cr_index];
                    if (DEBUG_VGA_REG)
                        System.out.println("vga: read CR"+Integer.toHexString(s.cr_index)+" = 0x"+Integer.toHexString(val));
                    break;
                case VGA_IS1_RM:
                case VGA_IS1_RC:
                    /* just toggle to fool polling */
                    val = s.st01 = s.retrace.call(s);
                    s.ar_flip_flop = 0;
                    break;
                default:
                    val = 0x00;
                    break;
                }
            }
            if (DEBUG_VGA)
                System.out.println("VGA: read addr=0x"+Integer.toHexString(addr)+" data=0x"+Integer.toHexString(val));
            return val;
        }
    }

    private static class vbe_ioport_read_index implements IoHandler.IO_ReadHandler {
        VGACommonState s;
        public vbe_ioport_read_index(VGACommonState s) {
            this.s = s;
        }
        public /*Bitu*/int call(/*Bitu*/int addr, /*Bitu*/int len) {
            int val;
            val = s.vbe_index;
            return val;
        }
    }

    private static class vbe_ioport_read_data implements IoHandler.IO_ReadHandler {
        VGACommonState s;
        public vbe_ioport_read_data(VGACommonState s) {
            this.s = s;
        }
        public /*Bitu*/int call(/*Bitu*/int addr, /*Bitu*/int len) {
            int val;

            if (s.vbe_index < VBE_DISPI_INDEX_NB) {
                if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_GETCAPS)!=0) {
                    switch(s.vbe_index) {
                        /* XXX: do not hardcode ? */
                    case VBE_DISPI_INDEX_XRES:
                        val = VBE_DISPI_MAX_XRES;
                        break;
                    case VBE_DISPI_INDEX_YRES:
                        val = VBE_DISPI_MAX_YRES;
                        break;
                    case VBE_DISPI_INDEX_BPP:
                        val = VBE_DISPI_MAX_BPP;
                        break;
                    default:
                        val = s.vbe_regs[s.vbe_index];
                        break;
                    }
                } else {
                    val = s.vbe_regs[s.vbe_index];
                }
            } else if (s.vbe_index == VBE_DISPI_INDEX_VIDEO_MEMORY_64K) {
                val = s.vram_size / (64 * 1024);
            } else {
                val = 0;
            }
            if (DEBUG_BOCHS_VBE)
                System.out.println("VBE: read index=0x"+Integer.toHexString(s.vbe_index)+" val=0x"+Integer.toHexString(val));
            return val;
        }
    }

    private static class vbe_ioport_write_index implements IoHandler.IO_WriteHandler {
        VGACommonState s;
        public vbe_ioport_write_index(VGACommonState s) {
            this.s = s;
        }
        public void call(/*Bitu*/int addr, /*Bitu*/int val, /*Bitu*/int len) {
            s.vbe_index = val;
        }
    }

    private static class vbe_ioport_write_data implements IoHandler.IO_WriteHandler {
        VGACommonState s;
        public vbe_ioport_write_data(VGACommonState s) {
            this.s = s;
        }
        public void call(/*Bitu*/int addr, /*Bitu*/int val, /*Bitu*/int len) {
            if (s.vbe_index <= VBE_DISPI_INDEX_NB) {
                if (DEBUG_BOCHS_VBE)
                    System.out.println("VBE: write index=0x"+Integer.toHexString(s.vbe_index)+" val=0x"+Integer.toHexString(val));
                switch(s.vbe_index) {
                case VBE_DISPI_INDEX_ID:
                    if (val == VBE_DISPI_ID0 ||
                        val == VBE_DISPI_ID1 ||
                        val == VBE_DISPI_ID2 ||
                        val == VBE_DISPI_ID3 ||
                        val == VBE_DISPI_ID4) {
                        s.vbe_regs[s.vbe_index] = val;
                    }
                    break;
                case VBE_DISPI_INDEX_XRES:
                    if ((val <= VBE_DISPI_MAX_XRES) && ((val & 7) == 0)) {
                        s.vbe_regs[s.vbe_index] = val;
                    }
                    break;
                case VBE_DISPI_INDEX_YRES:
                    if (val <= VBE_DISPI_MAX_YRES) {
                        s.vbe_regs[s.vbe_index] = val;
                    }
                    break;
                case VBE_DISPI_INDEX_BPP:
                    if (val == 0)
                        val = 8;
                    if (val == 4 || val == 8 || val == 15 ||
                        val == 16 || val == 24 || val == 32) {
                        s.vbe_regs[s.vbe_index] = val;
                    }
                    break;
                case VBE_DISPI_INDEX_BANK:
                    if (s.vbe_regs[VBE_DISPI_INDEX_BPP] == 4) {
                      val &= (s.vbe_bank_mask >> 2);
                    } else {
                      val &= s.vbe_bank_mask;
                    }
                    s.vbe_regs[s.vbe_index] = val;
                    s.bank_offset = (val << 16);
                    //vga_update_memory_access(s);
                    break;
                case VBE_DISPI_INDEX_ENABLE:
                    if ((val & VBE_DISPI_ENABLED)!=0 && (s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)==0) {
                        int h, shift_control;

                        s.vbe_regs[VBE_DISPI_INDEX_VIRT_WIDTH] =
                            s.vbe_regs[VBE_DISPI_INDEX_XRES];
                        s.vbe_regs[VBE_DISPI_INDEX_VIRT_HEIGHT] =
                            s.vbe_regs[VBE_DISPI_INDEX_YRES];
                        s.vbe_regs[VBE_DISPI_INDEX_X_OFFSET] = 0;
                        s.vbe_regs[VBE_DISPI_INDEX_Y_OFFSET] = 0;

                        if (s.vbe_regs[VBE_DISPI_INDEX_BPP] == 4)
                            s.vbe_line_offset = s.vbe_regs[VBE_DISPI_INDEX_XRES] >> 1;
                        else
                            s.vbe_line_offset = s.vbe_regs[VBE_DISPI_INDEX_XRES] *
                                ((s.vbe_regs[VBE_DISPI_INDEX_BPP] + 7) >> 3);
                        s.vbe_start_addr = 0;

                        /* clear the screen (should be done in BIOS) */
                        if ((val & VBE_DISPI_NOCLEARMEM)==0) {
                            s.memset(0, 0, s.vbe_regs[VBE_DISPI_INDEX_YRES] * s.vbe_line_offset);
                        }

                        /* we initialize the VGA graphic mode (should be done
                           in BIOS) */
                        /* graphic mode + memory map 1 */
                        s.gr[VGA_GFX_MISC] = (s.gr[VGA_GFX_MISC] & ~0x0c) | 0x04 |
                            VGA_GR06_GRAPHICS_MODE;
                        s.cr[VGA_CRTC_MODE] |= 3; /* no CGA modes */
                        s.cr[VGA_CRTC_OFFSET] = s.vbe_line_offset >> 3;
                        /* width */
                        s.cr[VGA_CRTC_H_DISP] =
                            (s.vbe_regs[VBE_DISPI_INDEX_XRES] >> 3) - 1;
                        /* height (only meaningful if < 1024) */
                        h = s.vbe_regs[VBE_DISPI_INDEX_YRES] - 1;
                        s.cr[VGA_CRTC_V_DISP_END] = h;
                        s.cr[VGA_CRTC_OVERFLOW] = (s.cr[VGA_CRTC_OVERFLOW] & ~0x42) |
                            ((h >> 7) & 0x02) | ((h >> 3) & 0x40);
                        /* line compare to 1023 */
                        s.cr[VGA_CRTC_LINE_COMPARE] = 0xff;
                        s.cr[VGA_CRTC_OVERFLOW] |= 0x10;
                        s.cr[VGA_CRTC_MAX_SCAN] |= 0x40;

                        if (s.vbe_regs[VBE_DISPI_INDEX_BPP] == 4) {
                            shift_control = 0;
                            s.sr[VGA_SEQ_CLOCK_MODE] &= ~8; /* no double line */
                        } else {
                            shift_control = 2;
                            /* set chain 4 mode */
                            s.sr[VGA_SEQ_MEMORY_MODE] |= VGA_SR04_CHN_4M;
                            /* activate all planes */
                            s.sr[VGA_SEQ_PLANE_WRITE] |= VGA_SR02_ALL_PLANES;
                        }
                        s.gr[VGA_GFX_MODE] = (s.gr[VGA_GFX_MODE] & ~0x60) |
                            (shift_control << 5);
                        s.cr[VGA_CRTC_MAX_SCAN] &= ~0x9f; /* no double scan */
                    } else {
                        /* XXX: the bios should do that */
                        s.bank_offset = 0;
                    }
                    s.dac_8bit = (val & VBE_DISPI_8BIT_DAC) > 0;
                    s.vbe_regs[s.vbe_index] = val;
                    //vga_update_memory_access(s);
                    break;
                case VBE_DISPI_INDEX_VIRT_WIDTH:
                    {
                        int w, h, line_offset;

                        if (val < s.vbe_regs[VBE_DISPI_INDEX_XRES])
                            return;
                        w = val;
                        if (s.vbe_regs[VBE_DISPI_INDEX_BPP] == 4)
                            line_offset = w >> 1;
                        else
                            line_offset = w * ((s.vbe_regs[VBE_DISPI_INDEX_BPP] + 7) >> 3);
                        h = s.vram_size / line_offset;
                        /* XXX: support weird bochs semantics ? */
                        if (h < s.vbe_regs[VBE_DISPI_INDEX_YRES])
                            return;
                        s.vbe_regs[VBE_DISPI_INDEX_VIRT_WIDTH] = w;
                        s.vbe_regs[VBE_DISPI_INDEX_VIRT_HEIGHT] = h;
                        s.vbe_line_offset = line_offset;
                    }
                    break;
                case VBE_DISPI_INDEX_X_OFFSET:
                case VBE_DISPI_INDEX_Y_OFFSET:
                    {
                        int x;
                        s.vbe_regs[s.vbe_index] = val;
                        s.vbe_start_addr = s.vbe_line_offset * s.vbe_regs[VBE_DISPI_INDEX_Y_OFFSET];
                        x = s.vbe_regs[VBE_DISPI_INDEX_X_OFFSET];
                        if (s.vbe_regs[VBE_DISPI_INDEX_BPP] == 4)
                            s.vbe_start_addr += x >> 1;
                        else
                            s.vbe_start_addr += x * ((s.vbe_regs[VBE_DISPI_INDEX_BPP] + 7) >> 3);
                        s.vbe_start_addr >>= 2;
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    static private class vga_mem extends Paging.PageHandler {
        VGACommonState s;
        public vga_mem(VGACommonState s) {
            this.s = s;
            flags=Paging.PFLAG_NOCODE;
        }

        public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
            return -1;
        }

        public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
            return -1;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            int memory_map_mode, plane, write_mode, b, func_select, mask;
            int write_mask, bit_mask=0, set_mask;

           if (DEBUG_VGA_MEM)
               System.out.println("vga: [0x" +Integer.toHexString(addr)+ "] = 0x"+Integer.toHexString(val));
           /* convert to VGA memory offset */
           memory_map_mode = (s.gr[VGA_GFX_MISC] >> 2) & 3;
           addr &= 0x1ffff;
           switch(memory_map_mode) {
           case 0:
               break;
           case 1:
               if (addr >= 0x10000)
                   return;
               addr += s.bank_offset;
               break;
           case 2:
               addr -= 0x10000;
               if (addr >= 0x8000)
                   return;
               break;
           default:
           case 3:
               addr -= 0x18000;
               if (addr >= 0x8000)
                   return;
               break;
           }

           if ((s.sr[VGA_SEQ_MEMORY_MODE] & VGA_SR04_CHN_4M)!=0) {
               /* chain 4 mode : simplest access */
               plane = addr & 3;
               mask = (1 << plane);
               if ((s.sr[VGA_SEQ_PLANE_WRITE] & mask)!=0) {
                   s.writeb(addr, val);
                    if (DEBUG_VGA_MEM)
                   System.out.println("vga: chain4: [0x" +Integer.toHexString(addr)+ "]");
                   s.plane_updated |= mask; /* only used to detect font change */
                   //memory_region_set_dirty(&s.vram, addr, 1);
               }
           } else if ((s.gr[VGA_GFX_MODE] & 0x10)!=0) {
               /* odd/even mode (aka text mode mapping) */
               plane = (s.gr[VGA_GFX_PLANE_READ] & 2) | (addr & 1);
               mask = (1 << plane);
               if ((s.sr[VGA_SEQ_PLANE_WRITE] & mask)!=0) {
                   addr = ((addr & ~1) << 1) | plane;
                   s.writeb(addr, val);
                   if (DEBUG_VGA_MEM)
                    System.out.println("vga: odd/even: [0x" +Integer.toHexString(addr)+ "]");
                   s.plane_updated |= mask; /* only used to detect font change */
                   //memory_region_set_dirty(&s.vram, addr, 1);
               }
           } else {
               boolean doWrite = false;
               /* standard VGA latched access */
               write_mode = s.gr[VGA_GFX_MODE] & 3;
               switch(write_mode) {
               default:
               case 0:
                   /* rotate */
                   b = s.gr[VGA_GFX_DATA_ROTATE] & 7;
                   val = ((val >> b) | (val << (8 - b))) & 0xff;
                   val |= val << 8;
                   val |= val << 16;

                   /* apply set/reset mask */
                   set_mask = mask16[s.gr[VGA_GFX_SR_ENABLE]];
                   val = (val & ~set_mask) |
                       (mask16[s.gr[VGA_GFX_SR_VALUE]] & set_mask);
                   bit_mask = s.gr[VGA_GFX_BIT_MASK];
                   break;
               case 1:
                   val = s.latch;
                   doWrite = true;
                   break;
               case 2:
                   val = mask16[val & 0x0f];
                   bit_mask = s.gr[VGA_GFX_BIT_MASK];
                   break;
               case 3:
                   /* rotate */
                   b = s.gr[VGA_GFX_DATA_ROTATE] & 7;
                   val = (val >> b) | (val << (8 - b));

                   bit_mask = s.gr[VGA_GFX_BIT_MASK] & val;
                   val = mask16[s.gr[VGA_GFX_SR_VALUE]];
                   break;
               }
               if (!doWrite) {
                   /* apply logical operation */
                   func_select = s.gr[VGA_GFX_DATA_ROTATE] >> 3;
                   switch(func_select) {
                   case 0:
                   default:
                       /* nothing to do */
                       break;
                   case 1:
                       /* and */
                       val &= s.latch;
                       break;
                   case 2:
                       /* or */
                       val |= s.latch;
                       break;
                   case 3:
                       /* xor */
                       val ^= s.latch;
                       break;
                   }

                   /* apply bit mask */
                   bit_mask |= bit_mask << 8;
                   bit_mask |= bit_mask << 16;
                   val = (val & bit_mask) | (s.latch & ~bit_mask);
               }
               /* mask data according to sr[2] */
               mask = s.sr[VGA_SEQ_PLANE_WRITE];
               s.plane_updated |= mask; /* only used to detect font change */
               write_mask = mask16[mask];
               s.writed(addr*4, (s.readd(addr*4) & ~write_mask) | (val & write_mask));
                if (DEBUG_VGA_MEM)
                    System.out.println("vga: latch: [0x" +Integer.toHexString(addr * 4)+ "] mask=0x"+Integer.toHexString(write_mask)+" val=0x"+Integer.toHexString(val));
               //memory_region_set_dirty(&s.vram, addr << 2, sizeof(uint32_t));
           }
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            int memory_map_mode, plane;
            int ret;

            /* convert to VGA memory offset */
            memory_map_mode = (s.gr[VGA_GFX_MISC] >> 2) & 3;
            addr &= 0x1ffff;
            switch(memory_map_mode) {
            case 0:
                break;
            case 1:
                if (addr >= 0x10000)
                    return 0xff;
                addr += s.bank_offset;
                break;
            case 2:
                addr -= 0x10000;
                if (addr >= 0x8000)
                    return 0xff;
                break;
            default:
            case 3:
                addr -= 0x18000;
                if (addr >= 0x8000)
                    return 0xff;
                break;
            }

            if ((s.sr[VGA_SEQ_MEMORY_MODE] & VGA_SR04_CHN_4M)!=0) {
                /* chain 4 mode : simplest access */
                ret = s.readb(addr);
            } else if ((s.gr[VGA_GFX_MODE] & 0x10)!=0) {
                /* odd/even mode (aka text mode mapping) */
                plane = (s.gr[VGA_GFX_PLANE_READ] & 2) | (addr & 1);
                ret = s.readb(((addr & ~1) << 1) | plane);
            } else {
                /* standard VGA latched access */
                s.latch = s.readd(addr*4);

                if ((s.gr[VGA_GFX_MODE] & 0x08)==0) {
                    /* read mode 0 */
                    plane = s.gr[VGA_GFX_PLANE_READ];
                    ret = GET_PLANE(s.latch, plane);
                } else {
                    /* read mode 1 */
                    ret = (s.latch ^ mask16[s.gr[VGA_GFX_COMPARE_VALUE]]) &
                        mask16[s.gr[VGA_GFX_COMPARE_MASK]];
                    ret |= ret >> 16;
                    ret |= ret >> 8;
                    ret = (~ret) & 0xff;
                }
            }
            return ret;
        }
    }

    static private void vga_draw_glyph_line_8(VGACommonState s, int d, int font_data, int xorcol, int bgcol) {
        s.writed(d, (dmask16[(font_data >> 4)] & xorcol) ^ bgcol);
        s.writed(d+4, (dmask16[(font_data >> 0) & 0xf] & xorcol) ^ bgcol);
    }

    static private void vga_draw_glyph_line_16(VGACommonState s, int d, int font_data, int xorcol, int bgcol) {
        s.writed(d, (dmask4[(font_data >> 6)] & xorcol) ^ bgcol);
        s.writed(d+4, (dmask4[(font_data >> 4) & 3] & xorcol) ^ bgcol);
        s.writed(d+8, (dmask4[(font_data >> 2) & 3] & xorcol) ^ bgcol);
        s.writed(d+12, (dmask4[(font_data >> 0) & 3] & xorcol) ^ bgcol);
    }

    static private void vga_draw_glyph_line_32(VGACommonState s, int d, int font_data, int xorcol, int bgcol) {
        s.writed(d, (-((font_data >> 7)) & xorcol) ^ bgcol);
        s.writed(d+4, (-((font_data >> 6) & 1) & xorcol) ^ bgcol);
        s.writed(d+8, (-((font_data >> 5) & 1) & xorcol) ^ bgcol);
        s.writed(d+12, (-((font_data >> 4) & 1) & xorcol) ^ bgcol);
        s.writed(d+16, (-((font_data >> 3) & 1) & xorcol) ^ bgcol);
        s.writed(d+20, (-((font_data >> 2) & 1) & xorcol) ^ bgcol);
        s.writed(d+24, (-((font_data >> 1) & 1) & xorcol) ^ bgcol);
        s.writed(d+28, (-((font_data >> 0) & 1) & xorcol) ^ bgcol);
    }

    static private interface vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol);
    }

    static private class vga_draw_glyph8_8 implements vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol) {
            int font_data, xorcol;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                vga_draw_glyph_line_8(s, d, font_data, xorcol, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph8_16 implements vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol) {
            int font_data, xorcol;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                vga_draw_glyph_line_16(s, d, font_data, xorcol, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph8_32 implements vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol) {
            int font_data, xorcol;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                vga_draw_glyph_line_32(s, d, font_data, xorcol, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph16_8 implements vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol) {
            int font_data, xorcol;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                vga_draw_glyph_line_8(s, d, expand4to8[font_data >> 4], xorcol, bgcol);
                vga_draw_glyph_line_8(s, d + 8, expand4to8[font_data & 0x0f], xorcol, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph16_16 implements vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol) {
            int font_data, xorcol;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                vga_draw_glyph_line_8(s, d, expand4to8[font_data >> 4], xorcol, bgcol);
                vga_draw_glyph_line_8(s, d + 16, expand4to8[font_data & 0x0f], xorcol, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph16_32 implements vga_draw_glyph8_func {
        public void call (VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol) {
            int font_data, xorcol;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                vga_draw_glyph_line_8(s, d, expand4to8[font_data >> 4], xorcol, bgcol);
                vga_draw_glyph_line_8(s, d + 32, expand4to8[font_data & 0x0f], xorcol, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private interface vga_draw_glyph9_func {
        public void call(VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol, boolean dup9);
    }

    static private class vga_draw_glyph9_8 implements vga_draw_glyph9_func {
        public void call(VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol, boolean dup9)
        {
            int font_data, xorcol, v;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                s.writed(d, (dmask16[(font_data >> 4)] & xorcol) ^ bgcol);
                v = (dmask16[(font_data >> 0) & 0xf] & xorcol) ^ bgcol;
                s.writed(d+4, v);
                if (dup9)
                    s.writeb(d+8, v >> 24);
                else
                    s.writeb(d+8, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph9_16 implements vga_draw_glyph9_func {
        public void call(VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol, boolean dup9) {
            int font_data, xorcol, v;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                s.writed(d, (dmask4[(font_data >> 6)] & xorcol) ^ bgcol);
                s.writed(d+4, (dmask4[(font_data >> 4) & 3] & xorcol) ^ bgcol);
                s.writed(d+8, (dmask4[(font_data >> 2) & 3] & xorcol) ^ bgcol);
                v = (dmask4[(font_data >> 0) & 3] & xorcol) ^ bgcol;
                s.writed(d+12, v);
                if (dup9)
                    s.writew(d+16, v >> 16);
                else
                    s.writew(d+16, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private class vga_draw_glyph9_32 implements vga_draw_glyph9_func {
        public void call(VGACommonState s, int d, int linesize, int font_ptr, int h, int fgcol, int bgcol, boolean dup9) {
            int font_data, xorcol, v;

            xorcol = bgcol ^ fgcol;
            do {
                font_data = s.readb(font_ptr);
                s.writed(d, (-((font_data >> 7)) & xorcol) ^ bgcol);
                s.writed(d+4, (-((font_data >> 6) & 1) & xorcol) ^ bgcol);
                s.writed(d+8, (-((font_data >> 5) & 1) & xorcol) ^ bgcol);
                s.writed(d+12, (-((font_data >> 4) & 1) & xorcol) ^ bgcol);
                s.writed(d+16, (-((font_data >> 3) & 1) & xorcol) ^ bgcol);
                s.writed(d+20, (-((font_data >> 2) & 1) & xorcol) ^ bgcol);
                s.writed(d+24, (-((font_data >> 1) & 1) & xorcol) ^ bgcol);
                v = (-((font_data >> 0) & 1) & xorcol) ^ bgcol;
                s.writed(d+28, v);
                if (dup9)
                    s.writed(d+32, v);
                else
                    s.writed(d+32, bgcol);
                font_ptr += 4;
                d += linesize;
                h--;
            } while (h!=0);
        }
    }

    static private interface vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width);
    }    
     
    static private final class vga_draw_line2_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int plane_mask, data, v;
            int x;

            plane_mask = mask16[s1.ar[VGA_ATC_PLANE_ENABLE] & 0xf];
            width >>= 3;
            for(x = 0; x < width; x++) {
                data = s1.readd(s);
                data &= plane_mask;
                v = expand2[GET_PLANE(data, 0)];
                v |= expand2[GET_PLANE(data, 2)] << 2;
                s1.writeb(d, (v >> 12) & 0xf);
                s1.writeb(d+1, (v >> 8) & 0xf);
                s1.writeb(d+2, (v >> 4) & 0xf);
                s1.writeb(d+3, (v >> 0) & 0xf);
    
                v = expand2[GET_PLANE(data, 1)];
                v |= expand2[GET_PLANE(data, 3)] << 2;
                s1.writeb(d+4, (v >> 12) & 0xf);
                s1.writeb(d+5, (v >> 8) & 0xf);
                s1.writeb(d+6, (v >> 4) & 0xf);
                s1.writeb(d+7, (v >> 0) & 0xf);
                d += 8;
                s += 4;
            }
        }
    }
        
    static private final class vga_draw_line2_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int plane_mask, data, v;
            int[] palette;
            int x;
    
            palette = s1.last_palette;
            plane_mask = mask16[s1.ar[VGA_ATC_PLANE_ENABLE] & 0xf];
            width >>= 3;
            for(x = 0; x < width; x++) {
                data = s1.readd(s);
                data &= plane_mask;
                v = expand2[GET_PLANE(data, 0)];
                v |= expand2[GET_PLANE(data, 2)] << 2;
                s1.writew(d, palette[v >> 12]);
                s1.writew(d+2, palette[(v >> 8) & 0xf]);
                s1.writew(d+4, palette[(v >> 4) & 0xf]);
                s1.writew(d+6, palette[(v >> 0) & 0xf]);
    
                v = expand2[GET_PLANE(data, 1)];
                v |= expand2[GET_PLANE(data, 3)] << 2;
                s1.writew(d+8, palette[v >> 12]);
                s1.writew(d+10, palette[(v >> 8) & 0xf]);
                s1.writew(d+12, palette[(v >> 4) & 0xf]);
                s1.writew(d+14, palette[(v >> 0) & 0xf]);
                d += 16;
                s += 4;
            }
        }
    }
        
    static private final class vga_draw_line2_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int plane_mask, data, v;
            int[] palette;
            int x;
    
            palette = s1.last_palette;
            plane_mask = mask16[s1.ar[VGA_ATC_PLANE_ENABLE] & 0xf];
            width >>= 3;
            for(x = 0; x < width; x++) {
                data = s1.readd(s);
                data &= plane_mask;
                v = expand2[GET_PLANE(data, 0)];
                v |= expand2[GET_PLANE(data, 2)] << 2;
                s1.writed(d, palette[v >> 12]);
                s1.writed(d+4, palette[(v >> 8) & 0xf]);
                s1.writed(d+8, palette[(v >> 4) & 0xf]);
                s1.writed(d+12, palette[(v >> 0) & 0xf]);
    
                v = expand2[GET_PLANE(data, 1)];
                v |= expand2[GET_PLANE(data, 3)] << 2;
                s1.writed(d+16, palette[v >> 12]);
                s1.writed(d+20, palette[(v >> 8) & 0xf]);
                s1.writed(d+24, palette[(v >> 4) & 0xf]);
                s1.writed(d+28, palette[(v >> 0) & 0xf]);
                d += 32;
                s += 4;
            }
        }
    }
        
    static private final class vga_draw_line4_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {        
            int plane_mask, data, v;
            int x;

            plane_mask = mask16[s1.ar[VGA_ATC_PLANE_ENABLE] & 0xf];
            width >>= 3;
            for(x = 0; x < width; x++) {
                data = s1.readd(s);
                data &= plane_mask;
                v = expand4[GET_PLANE(data, 0)];
                v |= expand4[GET_PLANE(data, 1)] << 1;
                v |= expand4[GET_PLANE(data, 2)] << 2;
                v |= expand4[GET_PLANE(data, 3)] << 3;
                s1.writeb(d, v >>> 28);
                s1.writeb(d+1, (v >> 24) & 0xf);
                s1.writeb(d+2, (v >> 20) & 0xf);
                s1.writeb(d+3, (v >> 16) & 0xf);
                s1.writeb(d+4, (v >> 12) & 0xf);
                s1.writeb(d+5, (v >> 8) & 0xf);
                s1.writeb(d+6, (v >> 4) & 0xf);
                s1.writeb(d+7, (v >> 0) & 0xf);
                d += 8;
                s += 4;
            }
        }
    }
        
    static private final class vga_draw_line4_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int plane_mask, data, v;
            int[] palette;
            int x;
    
            palette = s1.last_palette;
            plane_mask = mask16[s1.ar[VGA_ATC_PLANE_ENABLE] & 0xf];
            width >>= 3;
            for(x = 0; x < width; x++) {
                data = s1.readd(s);
                data &= plane_mask;
                v = expand4[GET_PLANE(data, 0)];
                v |= expand4[GET_PLANE(data, 1)] << 1;
                v |= expand4[GET_PLANE(data, 2)] << 2;
                v |= expand4[GET_PLANE(data, 3)] << 3;
                s1.writew(d, palette[v >> 28]);
                s1.writew(d+2, palette[(v >> 24) & 0xf]);
                s1.writew(d+4, palette[(v >> 20) & 0xf]);
                s1.writew(d+6, palette[(v >> 16) & 0xf]);
                s1.writew(d+8, palette[(v >> 12) & 0xf]);
                s1.writew(d+10, palette[(v >> 8) & 0xf]);
                s1.writew(d+12, palette[(v >> 4) & 0xf]);
                s1.writew(d+14, palette[(v >> 0) & 0xf]);
                d += 16;
                s += 4;
            }
        }
    }
     
    static private final class vga_draw_line4_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int plane_mask, data, v;
            int[] palette;
            int x;
    
            palette = s1.last_palette;
            plane_mask = mask16[s1.ar[VGA_ATC_PLANE_ENABLE] & 0xf];
            width >>= 3;
            for(x = 0; x < width; x++) {
                data = s1.readd(s);
                data &= plane_mask;
                v = expand4[GET_PLANE(data, 0)];
                v |= expand4[GET_PLANE(data, 1)] << 1;
                v |= expand4[GET_PLANE(data, 2)] << 2;
                v |= expand4[GET_PLANE(data, 3)] << 3;
                s1.writed(d, palette[v >>> 28]);
                s1.writed(d+4, palette[(v >> 24) & 0xf]);
                s1.writed(d+8, palette[(v >> 20) & 0xf]);
                s1.writed(d+12, palette[(v >> 16) & 0xf]);
                s1.writed(d+16, palette[(v >> 12) & 0xf]);
                s1.writed(d+20, palette[(v >> 8) & 0xf]);
                s1.writed(d+24, palette[(v >> 4) & 0xf]);
                s1.writed(d+28, palette[(v >> 0) & 0xf]);
                d += 32;
                s += 4;
            }
        }
    }

    static private final class vga_draw_line8_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int x;

            width >>= 3;
            for(x = 0; x < width; x++) {
                s1.writeb(d, s1.readb(s));
                s1.writeb(d+1, s1.readb(s+1));
                s1.writeb(d+2, s1.readb(s+2));
                s1.writeb(d+3, s1.readb(s+3));
                s1.writeb(d+4, s1.readb(s+4));
                s1.writeb(d+5, s1.readb(s+5));
                s1.writeb(d+6, s1.readb(s+6));
                s1.writeb(d+7, s1.readb(s+7));
                d += 8;
                s += 8;
            }
        }
    }
        
    static private final class vga_draw_line8_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int[] palette;
            int x;
    
            palette = s1.last_palette;
            width >>= 3;
            for(x = 0; x < width; x++) {
                s1.writew(d, palette[s1.readb(s)]);
                s1.writew(d+2, palette[s1.readb(s+1)]);
                s1.writew(d+4, palette[s1.readb(s+2)]);
                s1.writew(d+6, palette[s1.readb(s+3)]);
                s1.writew(d+8, palette[s1.readb(s+4)]);
                s1.writew(d+10, palette[s1.readb(s+5)]);
                s1.writew(d+12, palette[s1.readb(s+6)]);
                s1.writew(d+14, palette[s1.readb(s+7)]);
                d += 16;
                s += 8;
            }
        }
    }
        
    static private final class vga_draw_line8_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int[] palette;
            int x;
    
            palette = s1.last_palette;
            width >>= 3;
            for(x = 0; x < width; x++) {
                s1.writed(d, palette[s1.readb(s)]);
                s1.writed(d+4, palette[s1.readb(s+1)]);
                s1.writed(d+8, palette[s1.readb(s+2)]);
                s1.writed(d+12, palette[s1.readb(s+3)]);
                s1.writed(d+16, palette[s1.readb(s+4)]);
                s1.writed(d+20, palette[s1.readb(s+5)]);
                s1.writed(d+24, palette[s1.readb(s+6)]);
                s1.writed(d+28, palette[s1.readb(s+7)]);
                d += 32;
                s += 8;
            }
        }
    }

    static private final class vga_draw_line15_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writeb(d, PixelOps.rgb_to_pixel8(r, g, b));
                s += 2;
                d ++;
            } while (--w != 0);
        }
    }
      
    static private final class vga_draw_line15_15 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel15(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line15_15bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel15bgr(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line15_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel16(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line15_16bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel16bgr(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line15_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writed(d, PixelOps.rgb_to_pixel32(r, g, b));
                s += 2;
                d += 4;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line15_32bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 7) & 0xf8;
                g = (v >> 2) & 0xf8;
                b = (v << 3) & 0xf8;
                s1.writed(d, PixelOps.rgb_to_pixel32bgr(r, g, b));
                s += 2;
                d += 4;
            } while (--w != 0);
        }
    }
     
    static private final class vga_draw_line16_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writeb(d, PixelOps.rgb_to_pixel8(r, g, b));
                s += 2;
                d ++;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line16_15 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel15(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line16_15bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel15bgr(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line16_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel16(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line16_16bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writew(d, PixelOps.rgb_to_pixel16bgr(r, g, b));
                s += 2;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line16_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writed(d, PixelOps.rgb_to_pixel32(r, g, b));
                s += 2;
                d += 4;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line16_32bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int v, r, g, b;
    
            w = width;
            do {
                v = s1.readw(s);
                r = (v >> 8) & 0xf8;
                g = (v >> 3) & 0xfc;
                b = (v << 3) & 0xf8;
                s1.writed(d, PixelOps.rgb_to_pixel32bgr(r, g, b));
                s += 2;
                d += 4;
            } while (--w != 0);
        }
    }
     
    static private final class vga_draw_line24_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writeb(d, PixelOps.rgb_to_pixel8(r, g, b));
                s += 3;
                d ++;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line24_15 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel15(r, g, b));
                s += 3;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line24_15bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel15bgr(r, g, b));
                s += 3;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line24_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel16(r, g, b));
                s += 3;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line24_16bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel16bgr(r, g, b));
                s += 3;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line24_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writed(d, PixelOps.rgb_to_pixel32(r, g, b));
                s += 3;
                d += 4;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line24_32bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writed(d, PixelOps.rgb_to_pixel32bgr(r, g, b));
                s += 3;
                d += 4;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line32_8 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writeb(d, PixelOps.rgb_to_pixel8(r, g, b));
                s += 4;
                d ++;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line32_15 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel15(r, g, b));
                s += 4;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line32_15bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;

            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel15bgr(r, g, b));
                s += 4;
                d += 2;
            } while (--w != 0);
        }
    }

    static private final class vga_draw_line32_16 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel16(r, g, b));
                s += 4;
                d += 2;
            } while (--w != 0);
        }
    }

    static private final class vga_draw_line32_16bgr implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writew(d, PixelOps.rgb_to_pixel16bgr(r, g, b));
                s += 4;
                d += 2;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line32_32 implements vga_draw_line_func {
        public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writed(d, PixelOps.rgb_to_pixel32(r, g, b));
                s += 4;
                d += 4;
            } while (--w != 0);
        }
    }
        
    static private final class vga_draw_line32_32bgr implements vga_draw_line_func {
            public void call(VGACommonState s1, int d, int s, int width) {
            int w;
            int r, g, b;
    
            w = width;
            do {
                b = s1.readb(s);
                g = s1.readb(s+1);
                r = s1.readb(s+2);
                s1.writed(d, PixelOps.rgb_to_pixel32bgr(r, g, b));
                s += 4;
                d += 4;
            } while (--w != 0);
        }
    }
        
    static private class rgb_to_pixel8_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel8(r, g, b);
            col |= col << 8;
            col |= col << 16;
            return col;
        }
    }

    static private class rgb_to_pixel15_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel15(r, g, b);
            col |= col << 16;
            return col;
        }
    }

    static private class rgb_to_pixel15bgr_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel15bgr(r, g, b);
            col |= col << 16;
            return col;
        }
    }

    static private class rgb_to_pixel16_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel16(r, g, b);
            col |= col << 16;
            return col;
        }
    }

    static private class rgb_to_pixel16bgr_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel16bgr(r, g, b);
            col |= col << 16;
            return col;
        }
    }

    static private class rgb_to_pixel32_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel32(r, g, b);
            return col;
        }
    }

    static private class rgb_to_pixel32bgr_dup implements VGACommonState.rgb_to_pixel_dup_func {
        public int call(int r, int g, int b) {
            int col;
            col = PixelOps.rgb_to_pixel32bgr(r, g, b);
            return col;
        }
    }

    /* return true if the palette was modified */
    static boolean update_palette16(VGACommonState s)
    {
        boolean full_update = false;
        int i;
        int v, col;
        int[] palette;

        palette = s.last_palette;
        for(i = 0; i < 16; i++) {
            v = s.ar[i];
            if ((s.ar[VGA_ATC_MODE] & 0x80)!=0) {
                v = ((s.ar[VGA_ATC_COLOR_PAGE] & 0xf) << 4) | (v & 0xf);
            } else {
                v = ((s.ar[VGA_ATC_COLOR_PAGE] & 0xc) << 4) | (v & 0x3f);
            }
            v = v * 3;
            int r = c6_to_8(s.palette[v]);
            int g = c6_to_8(s.palette[v + 1]);
            int b = c6_to_8(s.palette[v + 2]);
            col = s.rgb_to_pixel.call(r, g, b);
            if (col != palette[i]) {
                full_update = true;
                palette[i] = col;
                s.renderer_palette[i] = Main.GFX_GetRGB(r, g, b);
            }
        }
        if (full_update) {
            Main.GFX_SetPalette(s.renderer_palette, 16);
        }
        return full_update;
    }

    /* return true if the palette was modified */
    static boolean update_palette256(VGACommonState s)
    {
        boolean full_update = false;
        int i;
        int v, col;
        int[] palette;

        palette = s.last_palette;
        v = 0;
        for(i = 0; i < 256; i++) {
            int r, g, b;
            if (s.dac_8bit) {
                r = s.palette[v];
                g = s.palette[v + 1];
                b = s.palette[v + 2];
            } else {
                r = c6_to_8(s.palette[v]);
                g = c6_to_8(s.palette[v + 1]);
                b = c6_to_8(s.palette[v + 2]);
            }
            col = s.rgb_to_pixel.call(r, g, b);
            if (col != palette[i]) {
                full_update = true;
                palette[i] = col;
                s.renderer_palette[i] = Main.GFX_GetRGB(r, g, b);
            }
            v += 3;
        }
        if (full_update) {
            Main.GFX_SetPalette(s.renderer_palette, 256);
        }
        return full_update;
    }

    static private class vga_get_line_offset implements VGACommonState.get_func {
        public int call(VGACommonState s)
        {
            if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)!=0) {
                return s.vbe_line_offset;
            } else {
                /* compute line_offset in bytes */
                int line_offset = s.cr[VGA_CRTC_OFFSET];
                line_offset <<= 3;
                return line_offset;
            }
        }
    }

    static private class vga_get_start_address implements VGACommonState.get_func {
        public int call(VGACommonState s)
        {
            if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)!=0) {
                return s.vbe_start_addr;
            } else {
                return s.cr[VGA_CRTC_START_LO] | (s.cr[VGA_CRTC_START_HI] << 8);
            }
        }
    }

    static private class vga_get_line_compare implements VGACommonState.get_func {
        public int call(VGACommonState s)
        {
            if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)!=0) {
                return 65535;
            } else {
                return s.cr[VGA_CRTC_LINE_COMPARE] | ((s.cr[VGA_CRTC_OVERFLOW] & 0x10) << 4) | ((s.cr[VGA_CRTC_MAX_SCAN] & 0x40) << 3);
            }
        }
    }

    /* update start_addr and line_offset. Return TRUE if modified */
    static private boolean update_basic_params(VGACommonState s)
    {
        boolean full_update = false;
        int start_addr = s.getStartAddress.call(s);
        int line_offset = s.getLineOffset.call(s);
        int line_compare = s.getLineCompare.call(s);

        if (line_offset != s.line_offset ||
            start_addr != s.start_addr ||
            line_compare != s.line_compare) {
            s.line_offset = line_offset;
            s.start_addr = start_addr;
            s.line_compare = line_compare;
            full_update = true;
        }
        return full_update;
    }

    static private final int NB_DEPTHS = 7;

    static private int get_depth_index(DisplayState s)
    {
        switch(s.ds_get_bits_per_pixel()) {
        default:
        case 8:
            return 0;
        case 15:
            return 1;
        case 16:
            return 2;
        case 32:
            if (s.is_surface_bgr())
                return 4;
            else
                return 3;
        }
    }

    static private final vga_draw_glyph8_func[] vga_draw_glyph8_table = new vga_draw_glyph8_func[] {
        new vga_draw_glyph8_8(),
        new vga_draw_glyph8_16(),
        new vga_draw_glyph8_16(),
        new vga_draw_glyph8_32(),
        new vga_draw_glyph8_32(),
        new vga_draw_glyph8_16(),
        new vga_draw_glyph8_16(),
    };

    static private final vga_draw_glyph8_func[] vga_draw_glyph16_table = new vga_draw_glyph8_func[] {
        new vga_draw_glyph16_8(),
        new vga_draw_glyph16_16(),
        new vga_draw_glyph16_16(),
        new vga_draw_glyph16_32(),
        new vga_draw_glyph16_32(),
        new vga_draw_glyph16_16(),
        new vga_draw_glyph16_16(),
    };

    static private final vga_draw_glyph9_func[] vga_draw_glyph9_table = new vga_draw_glyph9_func[] {
        new vga_draw_glyph9_8(),
        new vga_draw_glyph9_16(),
        new vga_draw_glyph9_16(),
        new vga_draw_glyph9_32(),
        new vga_draw_glyph9_32(),
        new vga_draw_glyph9_16(),
        new vga_draw_glyph9_16(),
    };

    static private int cursor_glyph;

    static int vga_get_text_height(VGACommonState s) {
        int height;
        if (s.cr[VGA_CRTC_V_TOTAL] == 100) {
            /* ugly hack for CGA 160x100x16 - explain me the logic */
            height = 100;
        } else {
            height = s.cr[VGA_CRTC_V_DISP_END] | ((s.cr[VGA_CRTC_OVERFLOW] & 0x02) << 7) | ((s.cr[VGA_CRTC_OVERFLOW] & 0x40) << 3);
            height = (height + 1) / vga_get_text_cheight(s);
        }
        return height;
    }

    static int vga_get_text_width(VGACommonState s) {
        return s.cr[VGA_CRTC_H_DISP] + 1;
    }
    static int vga_get_text_cheight(VGACommonState s) {
        return (s.cr[VGA_CRTC_MAX_SCAN] & 0x1f) + 1;
    }
    static int vga_get_text_cwidth(VGACommonState s) {
        int cwidth = 8;
        if ((s.sr[VGA_SEQ_CLOCK_MODE] & VGA_SR01_CHAR_CLK_8DOTS)==0) {
            cwidth = 9;
        }
        if ((s.sr[VGA_SEQ_CLOCK_MODE] & 0x08)!=0) {
            cwidth = 16; /* NOTE: no 18 pixel wide */
        }
        return cwidth;
    }

    static private final VGACommonState.rgb_to_pixel_dup_func[] rgb_to_pixel_dup_table = new VGACommonState.rgb_to_pixel_dup_func[] {
        new rgb_to_pixel8_dup(),
        new rgb_to_pixel15_dup(),
        new rgb_to_pixel16_dup(),
        new rgb_to_pixel32_dup(),
        new rgb_to_pixel32bgr_dup(),
        new rgb_to_pixel15bgr_dup(),
        new rgb_to_pixel16bgr_dup(),
    };

    /*
     * Text mode update
     * Missing:
     * - double scan
     * - double width
     * - underline
     * - flashing
     */
    static void vga_draw_text(VGACommonState s, boolean full_update)
    {
        int cx, cy, cheight, cw, ch, cattr, height, width, ch_attr;
        int cx_min, cx_max, linesize, x_incr, line, line1;
        int offset, fgcol, bgcol, v, cursor_offset;
        int d1, d, src, dest, cursor_ptr;
        int font_ptr;
        int[] font_base = new int[2];
        boolean dup9;
        int line_offset, depth_index;
        int[] palette;
        int ch_attr_ptr=0;
        vga_draw_glyph8_func vga_draw_glyph8;
        vga_draw_glyph9_func vga_draw_glyph9;
        long now = Qemu.qemu_get_clock_ms();

        /* compute font data address (in plane 2) */
        v = s.sr[VGA_SEQ_CHARACTER_MAP];
        offset = (((v >> 4) & 1) | ((v << 1) & 6)) * 8192 * 4 + 2;
        if (offset != s.font_offsets[0]) {
            s.font_offsets[0] = offset;
            full_update = true;
        }
        font_base[0] = offset;

        offset = (((v >> 5) & 1) | ((v >> 1) & 6)) * 8192 * 4 + 2;
        font_base[1] = offset;
        if (offset != s.font_offsets[1]) {
            s.font_offsets[1] = offset;
            full_update = true;
        }
        if ((s.plane_updated & (1 << 2))!=0 || s.chain4_alias!=null) {
            /* if the plane 2 was modified since the last display, it
               indicates the font may have been modified */
            s.plane_updated = 0;
            full_update = true;
        }
        full_update |= update_basic_params(s);

        line_offset = s.line_offset;

        width = vga_get_text_width(s);
        height = vga_get_text_height(s);
        cw = vga_get_text_cwidth(s);
        cheight = vga_get_text_cheight(s);

        if ((height * width) <= 1) {
            /* better than nothing: exit if transient size is too small */
            return;
        }
        if ((height * width) > CH_ATTR_SIZE) {
            /* better than nothing: exit if transient size is too big */
            return;
        }

        if (width != s.last_width || height != s.last_height || cw != s.last_cw || cheight != s.last_ch || s.last_depth!=0) {
            s.last_scr_width = width * cw;
            s.last_scr_height = height * cheight;
            s.last_depth = 0;
            s.last_width = width;
            s.last_height = height;
            s.last_ch = cheight;
            s.last_cw = cw;
            full_update = true;
            Render.RENDER_SetSize(s.last_scr_width, s.last_scr_height, 32, 70, (double)s.last_scr_width/s.last_scr_height, false, false); // don't use 8 bpp
            if (!Render.RENDER_StartUpdate())
                return;
        }
        s.rgb_to_pixel = rgb_to_pixel_dup_table[get_depth_index(s.ds)];
        full_update |= update_palette16(s);
        palette = s.last_palette;
        x_incr = cw * ((s.ds.ds_get_bits_per_pixel() + 7) >> 3);

        cursor_offset = ((s.cr[VGA_CRTC_CURSOR_HI] << 8) | s.cr[VGA_CRTC_CURSOR_LO]) - s.start_addr;
        if (cursor_offset != s.cursor_offset ||
            s.cr[VGA_CRTC_CURSOR_START] != s.cursor_start ||
            s.cr[VGA_CRTC_CURSOR_END] != s.cursor_end) {
          /* if the cursor position changed, we update the old and new
             chars */
            if (s.cursor_offset < CH_ATTR_SIZE)
                s.last_ch_attr[s.cursor_offset] = -1;
            if (cursor_offset < CH_ATTR_SIZE)
                s.last_ch_attr[cursor_offset] = -1;
            s.cursor_offset = cursor_offset;
            s.cursor_start = s.cr[VGA_CRTC_CURSOR_START];
            s.cursor_end = s.cr[VGA_CRTC_CURSOR_END];
        }
        cursor_ptr = (s.start_addr + cursor_offset) * 4;
        if (now >= s.cursor_blink_time) {
            s.cursor_blink_time = now + VGA_TEXT_CURSOR_PERIOD_MS / 2;
            s.cursor_visible_phase = !s.cursor_visible_phase;
        }

        depth_index = get_depth_index(s.ds);
        if (cw == 16)
            vga_draw_glyph8 = vga_draw_glyph16_table[depth_index];
        else
            vga_draw_glyph8 = vga_draw_glyph8_table[depth_index];
        vga_draw_glyph9 = vga_draw_glyph9_table[depth_index];

        dest = s.ds.ds_get_data();
        linesize = s.ds.ds_get_linesize();
        line = 0;
        offset = s.start_addr * 4;
        for(cy = 0; cy < height; cy++) {
            d1 = dest;
            src = offset;
            cx_min = width;
            cx_max = -1;
            for(cx = 0; cx < width; cx++) {
                ch_attr = s.readw(src);
                if (full_update || ch_attr != s.last_ch_attr[ch_attr_ptr] || src == cursor_ptr) {
                    if (cx < cx_min)
                        cx_min = cx;
                    if (cx > cx_max)
                        cx_max = cx;

                    s.last_ch_attr[ch_attr_ptr] = ch_attr;
                    ch = ch_attr & 0xff;
                    cattr = ch_attr >> 8;
                    font_ptr = font_base[(cattr >> 3) & 1];
                    font_ptr += 32 * 4 * ch;
                    bgcol = palette[cattr >> 4];
                    fgcol = palette[cattr & 0x0f];
                    if (cw != 9) {
                        vga_draw_glyph8.call(s, d1, linesize, font_ptr, cheight, fgcol, bgcol);
                    } else {
                        dup9 = false;
                        if (ch >= 0xb0 && ch <= 0xdf && (s.ar[VGA_ATC_MODE] & 0x04)!=0) {
                            dup9 = true;
                        }
                        vga_draw_glyph9.call(s, d1, linesize, font_ptr, cheight, fgcol, bgcol, dup9);
                    }
                    if (src == cursor_ptr && (s.cr[VGA_CRTC_CURSOR_START] & 0x20)==0 && s.cursor_visible_phase) {
                        int line_start, line_last, h;
                        /* draw the cursor */
                        line_start = s.cr[VGA_CRTC_CURSOR_START] & 0x1f;
                        line_last = s.cr[VGA_CRTC_CURSOR_END] & 0x1f;
                        /* XXX: check that */
                        if (line_last > cheight - 1)
                            line_last = cheight - 1;
                        if (line_last >= line_start && line_start < cheight) {
                            h = line_last - line_start + 1;
                            d = d1 + linesize * line_start;
                            if (cw != 9) {
                                vga_draw_glyph8.call(s, d, linesize, cursor_glyph, h, fgcol, bgcol);
                            } else {
                                vga_draw_glyph9.call(s, d, linesize, cursor_glyph, h, fgcol, bgcol, true);
                            }
                        }
                    }
                }
                d1 += x_incr;
                src += 4;
                ch_attr_ptr++;
            }
            if (cx_max != -1) {
                s.ds.dpy_update(cx_min * cw, cy * cheight, (cx_max - cx_min + 1) * cw, cheight);
            }
            dest += linesize * cheight;
            line1 = line + cheight;
            offset += line_offset;
            if (line < s.line_compare && line1 >= s.line_compare) {
                offset = 0;
            }
            line = line1;
        }
    }

    static private final int VGA_DRAW_LINE2 = 0;
    static private final int VGA_DRAW_LINE4 = 1;
    static private final int VGA_DRAW_LINE8 = 2;
    static private final int VGA_DRAW_LINE15 = 3;
    static private final int VGA_DRAW_LINE16 = 4;
    static private final int VGA_DRAW_LINE24 = 5;
    static private final int VGA_DRAW_LINE32 = 6;
    static private final int VGA_DRAW_LINE_NB = 7;

    static private final vga_draw_line_func[] vga_draw_line_table = new vga_draw_line_func[] {
        new vga_draw_line2_8(),
        new vga_draw_line2_16(),
        new vga_draw_line2_16(),
        new vga_draw_line2_32(),
        new vga_draw_line2_32(),
        new vga_draw_line2_16(),
        new vga_draw_line2_16(),

        new vga_draw_line4_8(),
        new vga_draw_line4_16(),
        new vga_draw_line4_16(),
        new vga_draw_line4_32(),
        new vga_draw_line4_32(),
        new vga_draw_line4_16(),
        new vga_draw_line4_16(),

        new vga_draw_line8_8(),
        new vga_draw_line8_16(),
        new vga_draw_line8_16(),
        new vga_draw_line8_32(),
        new vga_draw_line8_32(),
        new vga_draw_line8_16(),
        new vga_draw_line8_16(),

        new vga_draw_line15_8(),
        new vga_draw_line15_15(),
        new vga_draw_line15_16(),
        new vga_draw_line15_32(),
        new vga_draw_line15_32bgr(),
        new vga_draw_line15_15bgr(),
        new vga_draw_line15_16bgr(),

        new vga_draw_line16_8(),
        new vga_draw_line16_15(),
        new vga_draw_line16_16(),
        new vga_draw_line16_32(),
        new vga_draw_line16_32bgr(),
        new vga_draw_line16_15bgr(),
        new vga_draw_line16_16bgr(),

        new vga_draw_line24_8(),
        new vga_draw_line24_15(),
        new vga_draw_line24_16(),
        new vga_draw_line24_32(),
        new vga_draw_line24_32bgr(),
        new vga_draw_line24_15bgr(),
        new vga_draw_line24_16bgr(),

        new vga_draw_line32_8(),
        new vga_draw_line32_15(),
        new vga_draw_line32_16(),
        new vga_draw_line32_32(),
        new vga_draw_line32_32bgr(),
        new vga_draw_line32_15bgr(),
        new vga_draw_line32_16bgr()
    };

    static private class vga_get_bpp implements VGACommonState.get_func {
        public int call (VGACommonState s) {
            int ret;
            if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)!=0) {
                ret = s.vbe_regs[VBE_DISPI_INDEX_BPP];
            } else
            {
                ret = 0;
            }
            return ret;
        }
    }

    static private final class vga_get_resolutionCx implements VGACommonState.get_func {
        public int call(VGACommonState s) {
            if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)!=0) {
                return s.vbe_regs[VBE_DISPI_INDEX_XRES];
            } else {
                return (s.cr[VGA_CRTC_H_DISP] + 1) * 8;
            }
        }
    }
    static private final class vga_get_resolutionCy implements VGACommonState.get_func {
        public int call(VGACommonState s) {
            if ((s.vbe_regs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_ENABLED)!=0) {
                return s.vbe_regs[VBE_DISPI_INDEX_YRES];
            } else
            {
                return (s.cr[VGA_CRTC_V_DISP_END] | ((s.cr[VGA_CRTC_OVERFLOW] & 0x02) << 7) | ((s.cr[VGA_CRTC_OVERFLOW] & 0x40) << 3))+1;
            }
        }
    }

    void vga_invalidate_scanlines(VGACommonState s, int y1, int y2)
    {
        int y;
        if (y1 >= VGA_MAX_HEIGHT)
            return;
        if (y2 >= VGA_MAX_HEIGHT)
            y2 = VGA_MAX_HEIGHT;
        for(y = y1; y < y2; y++) {
            s.invalidated_y_table[y >> 5] |= 1 << (y & 0x1f);
        }
    }

    static void vga_sync_dirty_bitmap(VGACommonState s)
    {
        //memory_region_sync_dirty_bitmap(&s.vram);
    }

    void vga_dirty_log_start(VGACommonState s)
    {
        //memory_region_set_log(&s.vram, true, DIRTY_MEMORY_VGA);
    }

    void vga_dirty_log_stop(VGACommonState s)
    {
        //memory_region_set_log(&s.vram, false, DIRTY_MEMORY_VGA);
    }

    /*
     * graphic modes
     */
    static void vga_draw_graphic(VGACommonState s, boolean full_update)
    {
        int y1, y, linesize, y_start, double_scan, mask, depth;
        boolean update;
        int width, height, shift_control, line_offset, bwidth, bits;
        int page0, page1, page_min, page_max;
        int disp_width, multi_scan, multi_run;
        int d;
        int v, addr1, addr;
        vga_draw_line_func vga_draw_line;

        full_update |= update_basic_params(s);

        if (!full_update)
            vga_sync_dirty_bitmap(s);

        width = s.get_resolutionCx.call(s);
        height = s.get_resolutionCy.call(s);
        disp_width = width;

        shift_control = (s.gr[VGA_GFX_MODE] >> 5) & 3;
        double_scan = (s.cr[VGA_CRTC_MAX_SCAN] >> 7);
        if (shift_control != 1) {
            multi_scan = (((s.cr[VGA_CRTC_MAX_SCAN] & 0x1f) + 1) << double_scan) - 1;
        } else {
            /* in CGA modes, multi_scan is ignored */
            /* XXX: is it correct ? */
            multi_scan = double_scan;
        }
        multi_run = multi_scan;
        if (shift_control != s.shift_control ||
            double_scan != s.double_scan) {
            full_update = true;
            s.shift_control = shift_control;
            s.double_scan = double_scan;
        }

        if (shift_control == 0) {
            if ((s.sr[VGA_SEQ_CLOCK_MODE] & 8)!=0) {
                disp_width <<= 1;
            }
        } else if (shift_control == 1) {
            if ((s.sr[VGA_SEQ_CLOCK_MODE] & 8)!=0) {
                disp_width <<= 1;
            }
        } else if (s.get_bpp.call(s)==0) {
            width >>= 1;
        }

        depth = s.get_bpp.call(s);
        if (s.line_offset != s.last_line_offset || disp_width != s.last_width || width != s.last_scr_width || height != s.last_height || s.last_depth != depth) {
            s.last_scr_width = width;
            s.last_scr_height = height;
            s.last_width = disp_width;
            s.last_height = height;
            s.last_line_offset = s.line_offset;
            s.last_depth = depth;
            full_update = true;
            Render.RENDER_SetSize(width, height, 32, 70, (double)disp_width/height, disp_width>width, false);
            if (!Render.RENDER_StartUpdate())
                return;
        }

        s.rgb_to_pixel = rgb_to_pixel_dup_table[get_depth_index(s.ds)];

        if (shift_control == 0) {
            full_update |= update_palette16(s);
            v = VGA_DRAW_LINE4;
            bits = 4;
        } else if (shift_control == 1) {
            full_update |= update_palette16(s);
            v = VGA_DRAW_LINE2;
            bits = 4;
        } else {
            switch(s.get_bpp.call(s)) {
            default:
            case 0:
                full_update |= update_palette256(s);
                v = VGA_DRAW_LINE8;
                bits = 4;
                break;
            case 8:
                full_update |= update_palette256(s);
                v = VGA_DRAW_LINE8;
                bits = 8;
                break;
            case 15:
                v = VGA_DRAW_LINE15;
                bits = 16;
                break;
            case 16:
                v = VGA_DRAW_LINE16;
                bits = 16;
                break;
            case 24:
                v = VGA_DRAW_LINE24;
                bits = 24;
                break;
            case 32:
                v = VGA_DRAW_LINE32;
                bits = 32;
                break;
            }
        }
        vga_draw_line = vga_draw_line_table[v * NB_DEPTHS + get_depth_index(s.ds)];

        // if (!is_buffer_shared(s.ds.surface) && s.cursor_invalidate)
        //    s.cursor_invalidate(s);

        line_offset = s.line_offset;

        addr1 = (s.start_addr * 4);
        bwidth = (width * bits + 7) / 8;
        y_start = -1;
        page_min = -1;
        page_max = 0;
        d = s.ds.ds_get_data();
        linesize = s.ds.ds_get_linesize();
        y1 = 0;
        for(y = 0; y < height; y++) {
            addr = addr1;
            if ((s.cr[VGA_CRTC_MODE] & 1)==0) {
                int shift;
                /* CGA compatibility handling */
                shift = 14 + ((s.cr[VGA_CRTC_MODE] >> 6) & 1);
                addr = (addr & ~(1 << shift)) | ((y1 & 1) << shift);
            }
            if ((s.cr[VGA_CRTC_MODE] & 2)==0) {
                addr = (addr & ~0x8000) | ((y1 & 2) << 14);
            }
            update = full_update;
            page0 = addr;
            page1 = addr + bwidth - 1;
            update |= true;//memory_region_get_dirty(&s.vram, page0, page1 - page0, DIRTY_MEMORY_VGA);
            /* explicit invalidation for the hardware cursor */
            update |= ((s.invalidated_y_table[y >> 5] >> (y & 0x1f)) & 1)!=0;
            if (update) {
                if (y_start < 0)
                    y_start = y;
                if (page0 < page_min)
                    page_min = page0;
                if (page1 > page_max)
                    page_max = page1;
                //if (!(is_buffer_shared(s.ds.surface))) {
                    vga_draw_line.call(s, d, addr, width);
                    if (s.cursor_draw_line!=null)
                        s.cursor_draw_line.call(s, d, y);
                //}
            } else {
                if (y_start >= 0) {
                    /* flush to display */
                    s.ds.dpy_update(0, y_start, disp_width, y - y_start);
                    y_start = -1;
                }
            }
            if (multi_run==0) {
                mask = (s.cr[VGA_CRTC_MODE] & 3) ^ 3;
                if ((y1 & mask) == mask)
                    addr1 += line_offset;
                y1++;
                multi_run = multi_scan;
            } else {
                multi_run--;
            }
            /* line compare acts on the displayed lines */
            if (y == s.line_compare)
                addr1 = 0;
            d += linesize;
        }
        if (y_start >= 0) {
            /* flush to display */
            s.ds.dpy_update(0, y_start, disp_width, y - y_start);
        }
        /* reset modified pages */
        //if (page_max >= page_min) {
        //    memory_region_reset_dirty(&s.vram, page_min, page_max - page_min, DIRTY_MEMORY_VGA);
        //}
        Arrays.fill(s.invalidated_y_table, 0, 0, ((height + 31) >> 5) * 4-1);
    }

    static private void vga_draw_blank(VGACommonState s, boolean full_update)
    {
        int i, w, val;
        int d;

        if (!full_update)
            return;
        if (s.last_scr_width <= 0 || s.last_scr_height <= 0)
            return;

        s.rgb_to_pixel =
            rgb_to_pixel_dup_table[get_depth_index(s.ds)];
        if (s.ds.ds_get_bits_per_pixel() == 8)
            val = s.rgb_to_pixel.call(0, 0, 0);
        else
            val = 0;
        w = s.last_scr_width * ((s.ds.ds_get_bits_per_pixel() + 7) >> 3);
        d = s.ds.ds_get_data();
        for(i = 0; i < s.last_scr_height; i++) {
            s.memset(d, val, w);
            d += s.ds.ds_get_linesize();
        }
        s.ds.dpy_update(0, 0, s.last_scr_width, s.last_scr_height);
    }

    static private final int GMODE_TEXT     = 0;
    static private final int GMODE_GRAPH    = 1;
    static private final int GMODE_BLANK    = 2;

    public static Pic.PIC_EventHandler VGA_Draw = new Pic.PIC_EventHandler() {
        public String toString() {
            return "VGA_Draw";
        }
        public void call(/*Bitu*/int val) {
            Thread t = new Thread() {
                public void run() {
                    while (true) {
                        vga_update_display(vgaCommonState);
                        try {Thread.sleep(20);} catch (Exception e) {}
                    }
                }
            };
            t.start();
            //vga_update_display(vgaCommonState);
            //Pic.PIC_AddEvent(VGA_Draw,25);
        }
    };

    static void vga_update_display(VGACommonState s)
    {
        boolean full_update;
        int graphic_mode;

        // :TODO: must be a better place to put this
        if (!Render.render.active)
            Render.RENDER_SetSize(640, 400, 8, 70, 640/400, false, false);
        if (!Render.RENDER_StartUpdate())
            return;
        //qemu_flush_coalesced_mmio_buffer();

        full_update = true;
        if ((s.ar_index & 0x20)==0) {
            graphic_mode = GMODE_BLANK;
        } else {
            graphic_mode = s.gr[VGA_GFX_MISC] & VGA_GR06_GRAPHICS_MODE;
        }
        if (graphic_mode != s.graphic_mode) {
            s.graphic_mode = graphic_mode;
            s.cursor_blink_time = Qemu.qemu_get_clock_ms();
            full_update = true;
        }
        switch(graphic_mode) {
        case GMODE_TEXT:
            vga_draw_text(s, full_update);
            break;
        case GMODE_GRAPH:
            vga_draw_graphic(s, full_update);
            break;
        case GMODE_BLANK:
        default:
            vga_draw_blank(s, full_update);
            break;
        }
        Render.RENDER_EndUpdate(false);
    }

    /* force a full display refresh */
    static void vga_invalidate_display(VGACommonState s)
    {
        s.last_width = -1;
        s.last_height = -1;
    }

    static void vga_common_reset(VGACommonState s)
    {
        s.sr_index = 0;
        Arrays.fill(s.sr, 0);
        s.gr_index = 0;
        Arrays.fill(s.gr, 0);
        s.ar_index = 0;
        Arrays.fill(s.ar, 0);
        s.ar_flip_flop = 0;
        s.cr_index = 0;
        Arrays.fill(s.cr, 0);
        s.msr = 0;
        s.fcr = 0;
        s.st00 = 0;
        s.st01 = 0;
        s.dac_state = 0;
        s.dac_sub_index = 0;
        s.dac_read_index = 0;
        s.dac_write_index = 0;
        Arrays.fill(s.dac_cache, 0);
        s.dac_8bit = false;
        Arrays.fill(s.palette, 0);
        s.bank_offset = 0;
        s.vbe_index = 0;
        Arrays.fill(s.vbe_regs, 0);
        s.vbe_regs[VBE_DISPI_INDEX_ID] = VBE_DISPI_ID5;
        s.vbe_start_addr = 0;
        s.vbe_line_offset = 0;
        s.vbe_bank_mask = (s.vram_size >> 16) - 1;
        Arrays.fill(s.font_offsets, 0);
        s.graphic_mode = -1; /* force full update */
        s.shift_control = 0;
        s.double_scan = 0;
        s.line_offset = 0;
        s.line_compare = 0;
        s.start_addr = 0;
        s.plane_updated = 0;
        s.last_cw = 0;
        s.last_ch = 0;
        s.last_width = 0;
        s.last_height = 0;
        s.last_scr_width = 0;
        s.last_scr_height = 0;
        s.cursor_start = 0;
        s.cursor_end = 0;
        s.cursor_offset = 0;
        Arrays.fill(s.invalidated_y_table, 0);
        Arrays.fill(s.last_palette, 0);
        Arrays.fill(s.last_ch_attr, 0);
        switch (vga_retrace_method) {
        case VGA_RETRACE_DUMB:
            break;
        case VGA_RETRACE_PRECISE:
            s.retrace_info.reset();
            break;
        }
        //vga_update_memory_access(s);
    }

    static private void vga_reset(VGACommonState s)
    {
        vga_common_reset(s);
    }

    static private int TEXTMODE_X(int x, int width) {return ((x) % width);}
    static private int TEXTMODE_Y(int x, int width) {return ((x) / width);}
    static private int VMEM2CHTYPE(int v) {return ((v & 0xff0007ff) | ((v & 0x00000800) << 10) | ((v & 0x00007000) >> 1));}

    /* relay text rendering to the display driver
     * instead of doing a full vga_update_display() */
//    static void vga_update_text(VGACommonState s, console_ch_t *chardata)
//    {
//        int graphic_mode, i, cursor_offset, cursor_visible;
//        int cw, cheight, width, height, size, c_min, c_max;
//        uint32_t *src;
//        console_ch_t *dst, val;
//        char msg_buffer[80];
//        int full_update = 0;
//
//        qemu_flush_coalesced_mmio_buffer();
//
//        if (!(s.ar_index & 0x20)) {
//            graphic_mode = GMODE_BLANK;
//        } else {
//            graphic_mode = s.gr[VGA_GFX_MISC] & VGA_GR06_GRAPHICS_MODE;
//        }
//        if (graphic_mode != s.graphic_mode) {
//            s.graphic_mode = graphic_mode;
//            full_update = 1;
//        }
//        if (s.last_width == -1) {
//            s.last_width = 0;
//            full_update = 1;
//        }
//
//        switch (graphic_mode) {
//        case GMODE_TEXT:
//            /* TODO: update palette */
//            full_update |= update_basic_params(s);
//
//            /* total width & height */
//            cheight = (s.cr[VGA_CRTC_MAX_SCAN] & 0x1f) + 1;
//            cw = 8;
//            if (!(s.sr[VGA_SEQ_CLOCK_MODE] & VGA_SR01_CHAR_CLK_8DOTS)) {
//                cw = 9;
//            }
//            if (s.sr[VGA_SEQ_CLOCK_MODE] & 0x08) {
//                cw = 16; /* NOTE: no 18 pixel wide */
//            }
//            width = (s.cr[VGA_CRTC_H_DISP] + 1);
//            if (s.cr[VGA_CRTC_V_TOTAL] == 100) {
//                /* ugly hack for CGA 160x100x16 - explain me the logic */
//                height = 100;
//            } else {
//                height = s.cr[VGA_CRTC_V_DISP_END] |
//                    ((s.cr[VGA_CRTC_OVERFLOW] & 0x02) << 7) |
//                    ((s.cr[VGA_CRTC_OVERFLOW] & 0x40) << 3);
//                height = (height + 1) / cheight;
//            }
//
//            size = (height * width);
//            if (size > CH_ATTR_SIZE) {
//                if (!full_update)
//                    return;
//
//                snprintf(msg_buffer, sizeof(msg_buffer), "%i x %i Text mode",
//                         width, height);
//                break;
//            }
//
//            if (width != s.last_width || height != s.last_height ||
//                cw != s.last_cw || cheight != s.last_ch) {
//                s.last_scr_width = width * cw;
//                s.last_scr_height = height * cheight;
//                s.ds.surface.width = width;
//                s.ds.surface.height = height;
//                dpy_resize(s.ds);
//                s.last_width = width;
//                s.last_height = height;
//                s.last_ch = cheight;
//                s.last_cw = cw;
//                full_update = 1;
//            }
//
//            /* Update "hardware" cursor */
//            cursor_offset = ((s.cr[VGA_CRTC_CURSOR_HI] << 8) |
//                             s.cr[VGA_CRTC_CURSOR_LO]) - s.start_addr;
//            if (cursor_offset != s.cursor_offset ||
//                s.cr[VGA_CRTC_CURSOR_START] != s.cursor_start ||
//                s.cr[VGA_CRTC_CURSOR_END] != s.cursor_end || full_update) {
//                cursor_visible = !(s.cr[VGA_CRTC_CURSOR_START] & 0x20);
//                if (cursor_visible && cursor_offset < size && cursor_offset >= 0)
//                    dpy_cursor(s.ds,
//                               TEXTMODE_X(cursor_offset),
//                               TEXTMODE_Y(cursor_offset));
//                else
//                    dpy_cursor(s.ds, -1, -1);
//                s.cursor_offset = cursor_offset;
//                s.cursor_start = s.cr[VGA_CRTC_CURSOR_START];
//                s.cursor_end = s.cr[VGA_CRTC_CURSOR_END];
//            }
//
//            src = (uint32_t *) s.vram_ptr + s.start_addr;
//            dst = chardata;
//
//            if (full_update) {
//                for (i = 0; i < size; src ++, dst ++, i ++)
//                    console_write_ch(dst, VMEM2CHTYPE(le32_to_cpu(*src)));
//
//                dpy_update(s.ds, 0, 0, width, height);
//            } else {
//                c_max = 0;
//
//                for (i = 0; i < size; src ++, dst ++, i ++) {
//                    console_write_ch(&val, VMEM2CHTYPE(le32_to_cpu(*src)));
//                    if (*dst != val) {
//                        *dst = val;
//                        c_max = i;
//                        break;
//                    }
//                }
//                c_min = i;
//                for (; i < size; src ++, dst ++, i ++) {
//                    console_write_ch(&val, VMEM2CHTYPE(le32_to_cpu(*src)));
//                    if (*dst != val) {
//                        *dst = val;
//                        c_max = i;
//                    }
//                }
//
//                if (c_min <= c_max) {
//                    i = TEXTMODE_Y(c_min);
//                    dpy_update(s.ds, 0, i, width, TEXTMODE_Y(c_max) - i + 1);
//                }
//            }
//
//            return;
//        case GMODE_GRAPH:
//            if (!full_update)
//                return;
//
//            s.get_resolution(s, &width, &height);
//            snprintf(msg_buffer, sizeof(msg_buffer), "%i x %i Graphic mode",
//                     width, height);
//            break;
//        case GMODE_BLANK:
//        default:
//            if (!full_update)
//                return;
//
//            snprintf(msg_buffer, sizeof(msg_buffer), "VGA Blank mode");
//            break;
//        }
//
//        /* Display a message */
//        s.last_width = 60;
//        s.last_height = height = 3;
//        dpy_cursor(s.ds, -1, -1);
//        s.ds.surface.width = s.last_width;
//        s.ds.surface.height = height;
//        dpy_resize(s.ds);
//
//        for (dst = chardata, i = 0; i < s.last_width * height; i ++)
//            console_write_ch(dst ++, ' ');
//
//        size = strlen(msg_buffer);
//        width = (s.last_width - size) / 2;
//        dst = chardata + s.last_width + width;
//        for (i = 0; i < size; i ++)
//            console_write_ch(dst ++, 0x00200100 | msg_buffer[i]);
//
//        dpy_update(s.ds, 0, 0, s.last_width, height);
//    }

//    static uint64_t vga_mem_read(void *opaque, target_phys_addr_t addr,
//                                 unsigned size)
//    {
//        VGACommonState *s = opaque;
//
//        return vga_mem_readb(s, addr);
//    }
//
//    static void vga_mem_write(void *opaque, target_phys_addr_t addr,
//                              uint64_t data, unsigned size)
//    {
//        VGACommonState *s = opaque;
//
//        return vga_mem_writeb(s, addr, data);
//    }

//    const MemoryRegionOps vga_mem_ops = {
//        .read = vga_mem_read,
//        .write = vga_mem_write,
//        .endianness = DEVICE_LITTLE_ENDIAN,
//        .impl = {
//            .min_access_size = 1,
//            .max_access_size = 1,
//        },
//    };

    static void vga_common_init(VGACommonState s)
    {
        int i, j, v, b;

        for(i = 0;i < 256; i++) {
            v = 0;
            for(j = 0; j < 8; j++) {
                v |= ((i >> j) & 1) << (j * 4);
            }
            expand4[i] = v;

            v = 0;
            for(j = 0; j < 4; j++) {
                v |= ((i >> (2 * j)) & 3) << (j * 4);
            }
            expand2[i] = v;
        }
        for(i = 0; i < 16; i++) {
            v = 0;
            for(j = 0; j < 4; j++) {
                b = ((i >> j) & 1);
                v |= b << (2 * j);
                v |= b << (2 * j + 1);
            }
            expand4to8[i] = v;
        }

        s.vram_size_mb = s.vram_size >> 20;

        //memory_region_init_ram(&s.vram, "vga.vram", s.vram_size);
        //vmstate_register_ram_global(&s.vram);
        //xen_register_framebuffer(&s.vram);
        //s.vram_ptr = memory_region_get_ram_ptr(&s.vram);
        s.get_bpp = new vga_get_bpp();
        s.getLineOffset = new vga_get_line_offset();
        s.getStartAddress = new vga_get_start_address();
        s.getLineCompare = new vga_get_line_compare();
        s.get_resolutionCx = new vga_get_resolutionCx();
        s.get_resolutionCy = new vga_get_resolutionCy();
        //s.update = vga_update_display;
        //s.invalidate = vga_invalidate_display;
        //s.screen_dump = vga_screen_dump;
        //s.text_update = vga_update_text;
        switch (vga_retrace_method) {
        case VGA_RETRACE_DUMB:
            s.retrace = new vga_dumb_retrace_impl();
            s.update_retrace_info = new vga_dumb_update_retrace_info();
            break;

        case VGA_RETRACE_PRECISE:
            s.retrace = new vga_precise_retrace_impl();
            s.update_retrace_info = new vga_precise_update_retrace_info();
            break;
        }
        //vga_dirty_log_start(s);
    }

    /* Used by both ISA and PCI */
    static void vga_init_io(VGACommonState s)
    {
        vga_ioport_write write = new vga_ioport_write(s);
        vga_ioport_read read = new vga_ioport_read(s);

        IoHandler.IO_RegisterWriteHandler(0x3b4, write, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3b4, read, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3b5, write, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3b5, read, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3ba, write, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3ba, read ,IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3d4, write, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3d4, read, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3d5, write, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3d5, read, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3da, write, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3da, read, IoHandler.IO_MB);
        for (int i=0;i<16;i++) {
            IoHandler.IO_RegisterWriteHandler(0x3c0+i, write, IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3c0+i, read, IoHandler.IO_MB);
        }
    }

    static void vga_init(VGACommonState s, int ramSize)
    {
        vga_init_io(s);
        s.vga_mem = new vga_mem(s);
        Memory.MEM_SetPageHandler( 0x0a0, 32, s.vga_mem );
        s.vram_ptr = Memory.allocate(ramSize);
        s.vram_size = ramSize;
        cursor_glyph = Memory.allocate(128);
        RAM.memset(cursor_glyph, 0xFF, 128);
        cursor_glyph-=s.vram_ptr; // cursor_glyph is relative to the beginning of vram
        Qemu.rom_add_vga("vgabios.bin", true);
    }

    static private class Bochs_LFB_Handler extends Paging.PageHandler {
        VGACommonState s;

        public Bochs_LFB_Handler(VGACommonState s) {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE|Paging.PFLAG_NOCODE;
            this.s = s;
        }
        public /*HostPt*/int GetHostReadPt( /*Bitu*/int phys_page ) {
            phys_page -= 0xE0000;
            return s.vram_ptr+phys_page*4096;
        }
        public /*HostPt*/int GetHostWritePt( /*Bitu*/int phys_page ) {
            phys_page -= 0xE0000;
            return s.vram_ptr+phys_page*4096;
        }
    }

    static void vga_init_vbe(VGACommonState s)
    {
        if (CONFIG_BOCHS_VBE) {
            VBE.initialized = true;
            VBE.handler = new Bochs_LFB_Handler(s);
            VBE.pageCount = s.vram_size >>> 12;
            s.vbe_mapped = 1;

            IoHandler.IO_RegisterWriteHandler(0x1ce, new vbe_ioport_write_index(s), IoHandler.IO_MA);
            IoHandler.IO_RegisterReadHandler(0x1ce, new vbe_ioport_read_index(s), IoHandler.IO_MA);
            IoHandler.IO_RegisterWriteHandler(0x1cf, new vbe_ioport_write_data(s), IoHandler.IO_MA);
            IoHandler.IO_RegisterReadHandler(0x1cf, new vbe_ioport_read_data(s), IoHandler.IO_MA);

            IoHandler.IO_RegisterWriteHandler(0xff80, new vbe_ioport_write_index(s), IoHandler.IO_MA);
            IoHandler.IO_RegisterReadHandler(0xff80, new vbe_ioport_read_index(s), IoHandler.IO_MA);
            IoHandler.IO_RegisterWriteHandler(0xff81, new vbe_ioport_write_data(s), IoHandler.IO_MA);
            IoHandler.IO_RegisterReadHandler(0xff81, new vbe_ioport_read_data(s), IoHandler.IO_MA);
        }
    }

    /********************************************************/
    /* vga screen dump */

//    int ppm_save(const char *filename, struct DisplaySurface *ds)
//    {
//        FILE *f;
//        uint8_t *d, *d1;
//        uint32_t v;
//        int y, x;
//        uint8_t r, g, b;
//        int ret;
//        char *linebuf, *pbuf;
//
//        trace_ppm_save(filename, ds);
//        f = fopen(filename, "wb");
//        if (!f)
//            return -1;
//        fprintf(f, "P6\n%d %d\n%d\n",
//                ds.width, ds.height, 255);
//        linebuf = g_malloc(ds.width * 3);
//        d1 = ds.data;
//        for(y = 0; y < ds.height; y++) {
//            d = d1;
//            pbuf = linebuf;
//            for(x = 0; x < ds.width; x++) {
//                if (ds.pf.bits_per_pixel == 32)
//                    v = *(uint32_t *)d;
//                else
//                    v = (uint32_t) (*(uint16_t *)d);
//                /* Limited to 8 or fewer bits per channel: */
//                r = ((v >> ds.pf.rshift) & ds.pf.rmax) << (8 - ds.pf.rbits);
//                g = ((v >> ds.pf.gshift) & ds.pf.gmax) << (8 - ds.pf.gbits);
//                b = ((v >> ds.pf.bshift) & ds.pf.bmax) << (8 - ds.pf.bbits);
//                *pbuf++ = r;
//                *pbuf++ = g;
//                *pbuf++ = b;
//                d += ds.pf.bytes_per_pixel;
//            }
//            d1 += ds.linesize;
//            ret = fwrite(linebuf, 1, pbuf - linebuf, f);
//            (void)ret;
//        }
//        g_free(linebuf);
//        fclose(f);
//        return 0;
//    }

    /* save the vga display in a PPM image even if no display is
       available */
//    static void vga_screen_dump(void *opaque, const char *filename, bool cswitch)
//    {
//        VGACommonState *s = opaque;
//
//        if (cswitch) {
//            vga_invalidate_display(s);
//        }
//        vga_hw_update();
//        ppm_save(filename, s.ds.surface);
//    }
    static private VGACommonState vgaCommonState;

    public static Section.SectionFunction QEMU_VGA_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            if (Dosbox.svgaCard == SVGACards.SVGA_QEMU) {
                Section_prop section=(Section_prop)sec;
                vgaCommonState = new VGACommonState();
                vga_init(vgaCommonState, section.Get_int("vmemsize")*1024*1024);
                vga_init_vbe(vgaCommonState);
                vga_common_init(vgaCommonState);
                Pic.PIC_AddEvent(VGA_Draw,70/1000);
            }
        }
    };
}
