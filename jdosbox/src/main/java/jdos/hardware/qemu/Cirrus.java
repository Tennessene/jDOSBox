package jdos.hardware.qemu;

import jdos.cpu.Paging;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Section;

// Ported to Java by James Bryant
/*
 * QEMU Cirrus CLGD 54xx VGA Emulator.
 *
 * Copyright (c) 2004 Fabrice Bellard
 * Copyright (c) 2004 Makoto Suzuki (suzu)
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
/*
 * Reference: Finn Thogersons' VGADOC4b
 *   available at http://home.worldonline.dk/~finth/
 */
public class Cirrus extends VGA_header {
    static private final boolean DEBUG = false;
    static private final int VGA_PAGES = (128 / 4);
    static private final int VGA_PAGE_A0 = (0xA0000 / 4096);
    static private final int VGA_PAGE_B0 = (0xB0000 / 4096);
    static private final int VGA_PAGE_B8 = (0xB8000 / 4096);

    /* force some bits to zero */
    static final int[] sr_mask = new int[]{
            0x03,
            0x3d,
            0x0f,
            0x3f,
            0x0e,
            0x00,
            0x00,
            0xff,
    };

    static final int[] gr_mask = new int[]{
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

    /***************************************
     *
     *  definitions
     *
     ***************************************/

    // ID
    static private final int CIRRUS_ID_CLGD5422 = (0x23 << 2);
    static private final int CIRRUS_ID_CLGD5426 = (0x24 << 2);
    static private final int CIRRUS_ID_CLGD5424 = (0x25 << 2);
    static private final int CIRRUS_ID_CLGD5428 = (0x26 << 2);
    static private final int CIRRUS_ID_CLGD5430 = (0x28 << 2);
    static private final int CIRRUS_ID_CLGD5434 = (0x2A << 2);
    static private final int CIRRUS_ID_CLGD5436 = (0x2B << 2);
    static private final int CIRRUS_ID_CLGD5446 = (0x2E << 2);

    // sequencer 0x07
    static private final int CIRRUS_SR7_BPP_VGA = 0x00;
    static private final int CIRRUS_SR7_BPP_SVGA = 0x01;
    static private final int CIRRUS_SR7_BPP_MASK = 0x0e;
    static private final int CIRRUS_SR7_BPP_8 = 0x00;
    static private final int CIRRUS_SR7_BPP_16_DOUBLEVCLK = 0x02;
    static private final int CIRRUS_SR7_BPP_24 = 0x04;
    static private final int CIRRUS_SR7_BPP_16 = 0x06;
    static private final int CIRRUS_SR7_BPP_32 = 0x08;
    static private final int CIRRUS_SR7_ISAADDR_MASK = 0xe0;

    // sequencer 0x0f
    static private final int CIRRUS_MEMSIZE_512k = 0x08;
    static private final int CIRRUS_MEMSIZE_1M = 0x10;
    static private final int CIRRUS_MEMSIZE_2M = 0x18;
    static private final int CIRRUS_MEMFLAGS_BANKSWITCH = 0x80;    // bank switching is enabled.

    // sequencer 0x12
    static private final int CIRRUS_CURSOR_SHOW = 0x01;
    static private final int CIRRUS_CURSOR_HIDDENPEL = 0x02;
    static private final int CIRRUS_CURSOR_LARGE = 0x04;    // 64x64 if set, 32x32 if clear

    // sequencer 0x17
    static private final int CIRRUS_BUSTYPE_VLBFAST = 0x10;
    static private final int CIRRUS_BUSTYPE_PCI = 0x20;
    static private final int CIRRUS_BUSTYPE_VLBSLOW = 0x30;
    static private final int CIRRUS_BUSTYPE_ISA = 0x38;
    static private final int CIRRUS_MMIO_ENABLE = 0x04;
    static private final int CIRRUS_MMIO_USE_PCIADDR = 0x40;    // 0xb8000 if cleared.
    static private final int CIRRUS_MEMSIZEEXT_DOUBLE = 0x80;

    // control 0x0b
    static private final int CIRRUS_BANKING_DUAL = 0x01;
    static private final int CIRRUS_BANKING_GRANULARITY_16K = 0x20;    // set:16k, clear:4k

    // control 0x30
    static private final int CIRRUS_BLTMODE_BACKWARDS = 0x01;
    static private final int CIRRUS_BLTMODE_MEMSYSDEST = 0x02;
    static private final int CIRRUS_BLTMODE_MEMSYSSRC = 0x04;
    static private final int CIRRUS_BLTMODE_TRANSPARENTCOMP = 0x08;
    static private final int CIRRUS_BLTMODE_PATTERNCOPY = 0x40;
    static private final int CIRRUS_BLTMODE_COLOREXPAND = 0x80;
    static private final int CIRRUS_BLTMODE_PIXELWIDTHMASK = 0x30;
    static private final int CIRRUS_BLTMODE_PIXELWIDTH8 = 0x00;
    static private final int CIRRUS_BLTMODE_PIXELWIDTH16 = 0x10;
    static private final int CIRRUS_BLTMODE_PIXELWIDTH24 = 0x20;
    static private final int CIRRUS_BLTMODE_PIXELWIDTH32 = 0x30;

    // control 0x31
    static private final int CIRRUS_BLT_BUSY = 0x01;
    static private final int CIRRUS_BLT_START = 0x02;
    static private final int CIRRUS_BLT_RESET = 0x04;
    static private final int CIRRUS_BLT_FIFOUSED = 0x10;
    static private final int CIRRUS_BLT_AUTOSTART = 0x80;

    // control 0x32
    static private final int CIRRUS_ROP_0 = 0x00;
    static private final int CIRRUS_ROP_SRC_AND_DST = 0x05;
    static private final int CIRRUS_ROP_NOP = 0x06;
    static private final int CIRRUS_ROP_SRC_AND_NOTDST = 0x09;
    static private final int CIRRUS_ROP_NOTDST = 0x0b;
    static private final int CIRRUS_ROP_SRC = 0x0d;
    static private final int CIRRUS_ROP_1 = 0x0e;
    static private final int CIRRUS_ROP_NOTSRC_AND_DST = 0x50;
    static private final int CIRRUS_ROP_SRC_XOR_DST = 0x59;
    static private final int CIRRUS_ROP_SRC_OR_DST = 0x6d;
    static private final int CIRRUS_ROP_NOTSRC_OR_NOTDST = 0x90;
    static private final int CIRRUS_ROP_SRC_NOTXOR_DST = 0x95;
    static private final int CIRRUS_ROP_SRC_OR_NOTDST = 0xad;
    static private final int CIRRUS_ROP_NOTSRC = 0xd0;
    static private final int CIRRUS_ROP_NOTSRC_OR_DST = 0xd6;
    static private final int CIRRUS_ROP_NOTSRC_AND_NOTDST = 0xda;

    static private final int CIRRUS_ROP_NOP_INDEX = 2;
    static private final int CIRRUS_ROP_SRC_INDEX = 5;

    // control 0x33
    static private final int CIRRUS_BLTMODEEXT_SOLIDFILL = 0x04;
    static private final int CIRRUS_BLTMODEEXT_COLOREXPINV = 0x02;
    static private final int CIRRUS_BLTMODEEXT_DWORDGRANULARITY = 0x01;

    // memory-mapped IO
    static private final int CIRRUS_MMIO_BLTBGCOLOR = 0x00;    // dword
    static private final int CIRRUS_MMIO_BLTFGCOLOR = 0x04;    // dword
    static private final int CIRRUS_MMIO_BLTWIDTH = 0x08;    // word
    static private final int CIRRUS_MMIO_BLTHEIGHT = 0x0a;    // word
    static private final int CIRRUS_MMIO_BLTDESTPITCH = 0x0c;    // word
    static private final int CIRRUS_MMIO_BLTSRCPITCH = 0x0e;    // word
    static private final int CIRRUS_MMIO_BLTDESTADDR = 0x10;    // dword
    static private final int CIRRUS_MMIO_BLTSRCADDR = 0x14;    // dword
    static private final int CIRRUS_MMIO_BLTWRITEMASK = 0x17;    // byte
    static private final int CIRRUS_MMIO_BLTMODE = 0x18;    // byte
    static private final int CIRRUS_MMIO_BLTROP = 0x1a;    // byte
    static private final int CIRRUS_MMIO_BLTMODEEXT = 0x1b;    // byte
    static private final int CIRRUS_MMIO_BLTTRANSPARENTCOLOR = 0x1c;    // word?
    static private final int CIRRUS_MMIO_BLTTRANSPARENTCOLORMASK = 0x20;    // word?
    static private final int CIRRUS_MMIO_LINEARDRAW_START_X = 0x24;    // word
    static private final int CIRRUS_MMIO_LINEARDRAW_START_Y = 0x26;    // word
    static private final int CIRRUS_MMIO_LINEARDRAW_END_X = 0x28;    // word
    static private final int CIRRUS_MMIO_LINEARDRAW_END_Y = 0x2a;    // word
    static private final int CIRRUS_MMIO_LINEARDRAW_LINESTYLE_INC = 0x2c;    // byte
    static private final int CIRRUS_MMIO_LINEARDRAW_LINESTYLE_ROLLOVER = 0x2d;    // byte
    static private final int CIRRUS_MMIO_LINEARDRAW_LINESTYLE_MASK = 0x2e;    // byte
    static private final int CIRRUS_MMIO_LINEARDRAW_LINESTYLE_ACCUM = 0x2f;    // byte
    static private final int CIRRUS_MMIO_BRESENHAM_K1 = 0x30;    // word
    static private final int CIRRUS_MMIO_BRESENHAM_K3 = 0x32;    // word
    static private final int CIRRUS_MMIO_BRESENHAM_ERROR = 0x34;    // word
    static private final int CIRRUS_MMIO_BRESENHAM_DELTA_MAJOR = 0x36;    // word
    static private final int CIRRUS_MMIO_BRESENHAM_DIRECTION = 0x38;    // byte
    static private final int CIRRUS_MMIO_LINEDRAW_MODE = 0x39;    // byte
    static private final int CIRRUS_MMIO_BLTSTATUS = 0x40;    // byte

    static private final int CIRRUS_PNPMMIO_SIZE = 0x1000;

    static private boolean BLTUNSAFE(CirrusVGAState s) {
        return (s.cirrus_blt_height * Math.abs(s.cirrus_blt_dstpitch) + (s.cirrus_blt_dstaddr & s.cirrus_addr_mask) > s.vram_size) ||
                (s.cirrus_blt_height * Math.abs(s.cirrus_blt_srcpitch) + (s.cirrus_blt_srcaddr & s.cirrus_addr_mask) > s.vram_size);
    }

    static private interface cirrus_bitblt_rop_t {
        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight);
    }

    static private interface cirrus_fill_t {
        public void call(CirrusVGAState s, int dstPos, int dst_pitch, int width, int height);
    }

    static private final class CirrusVGAState extends VGACommonState {
        int get_bpp() {
            int ret = 8;

            if ((sr[0x07] & 0x01) != 0) {
            /* Cirrus SVGA */
                switch (sr[0x07] & CIRRUS_SR7_BPP_MASK) {
                    case CIRRUS_SR7_BPP_8:
                        ret = 8;
                        break;
                    case CIRRUS_SR7_BPP_16_DOUBLEVCLK:
                        ret = cirrus_get_bpp16_depth(this);
                        break;
                    case CIRRUS_SR7_BPP_24:
                        ret = 24;
                        break;
                    case CIRRUS_SR7_BPP_16:
                        ret = cirrus_get_bpp16_depth(this);
                        break;
                    case CIRRUS_SR7_BPP_32:
                        ret = 32;
                        break;
                    default:
                        if (DEBUG)
                            System.out.println("cirrus: unknown bpp - sr7=" + sr[0x7]);
                        ret = 8;
                        break;
                }
            } else {
            /* VGA */
                ret = 0;
            }

            return ret;
        }

        int get_resolutionCx() {
            return (cr[0x01] + 1) * 8;
        }

        int get_resolutionCy() {
            int height = cr[0x12] | ((cr[0x07] & 0x02) << 7) | ((cr[0x07] & 0x40) << 3);
            height = (height + 1);
            /* interlace support */
            if ((cr[0x1a] & 0x01) != 0)
                height = height * 2;
            return height;
        }

        //MemoryRegion cirrus_linear_io;
        //MemoryRegion cirrus_linear_bitblt_io;
        //MemoryRegion cirrus_mmio_io;
        //MemoryRegion pci_bar;
        boolean linear_vram;  /* vga.vram mapped over cirrus_linear_io */
        //MemoryRegion low_mem_container; /* container for 0xa0000-0xc0000 */
        //MemoryRegion low_mem;           /* always mapped, overridden by: */
        //MemoryRegion cirrus_bank[2];    /*   aliases at 0xa0000-0xb0000  */
        int cirrus_addr_mask;
        int linear_mmio_mask;
        int cirrus_shadow_gr0;
        int cirrus_shadow_gr1;
        int cirrus_hidden_dac_lockindex;
        int cirrus_hidden_dac_data;
        int[] cirrus_bank_base = new int[2];
        int[] cirrus_bank_limit = new int[2];
        int[] cirrus_hidden_palette = new int[48];
        int hw_cursor_x;
        int hw_cursor_y;
        int cirrus_blt_pixelwidth;
        int cirrus_blt_width;
        int cirrus_blt_height;
        int cirrus_blt_dstpitch;
        int cirrus_blt_srcpitch;
        int cirrus_blt_fgcol;
        int cirrus_blt_bgcol;
        int cirrus_blt_dstaddr;
        int cirrus_blt_srcaddr;
        int cirrus_blt_mode;
        int cirrus_blt_modeext;
        cirrus_bitblt_rop_t cirrus_rop;

        int cirrus_bltbufPos;
        int cirrus_srcptr;
        int cirrus_srcptr_end;
        int cirrus_srccounter;
        /* hwcursor display state */
        int last_hw_cursor_size;
        int last_hw_cursor_x;
        int last_hw_cursor_y;
        int last_hw_cursor_y_start;
        int last_hw_cursor_y_end;
        int real_vram_size; /* XXX: suppress that */
        int device_id;
        int bustype;
    }

//    typedef struct PCICirrusVGAState {
//        PCIDevice dev;
//        CirrusVGAState cirrus_vga;
//    } PCICirrusVGAState;
//
//    typedef struct ISACirrusVGAState {
//        ISADevice dev;
//        CirrusVGAState cirrus_vga;
//    } ISACirrusVGAState;

    static private final byte[] rop_to_index = new byte[256];

    /***************************************
     *
     *  raster operations
     *
     ***************************************/

    static private final cirrus_bitblt_rop_t cirrus_bitblt_rop_nop = new cirrus_bitblt_rop_t() {
        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
        }
    };

    static private final cirrus_fill_t cirrus_bitblt_fill_nop = new cirrus_fill_t() {
        public void call(CirrusVGAState s, int dstPos, int dstpitch, int bltwidth, int bltheight) {
        }
    };

    static private interface ROP_OP {
        public void call8(CirrusVGAState s, int dstPos, int srcColor);

        public void call16(CirrusVGAState s, int dstPos, int srcColor);

        public void call32(CirrusVGAState s, int dstPos, int srcColor);
    }

    static private abstract class cirrus_bitblt_rop implements cirrus_bitblt_rop_t {
        public ROP_OP op;

        public cirrus_bitblt_rop(ROP_OP op) {
            this.op = op;
        }
    }

    static private abstract class cirrus_fill implements cirrus_fill_t {
        public ROP_OP op;

        public cirrus_fill(ROP_OP op) {
            this.op = op;
        }
    }

    static private final class cirrus_bitblt_rop_fwd extends cirrus_bitblt_rop {
        public cirrus_bitblt_rop_fwd(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int x, y;
            dstpitch -= bltwidth;
            srcpitch -= bltwidth;

            if (dstpitch < 0 || srcpitch < 0) {
                /* is 0 valid? srcpitch == 0 could be useful */
                return;
            }

            for (y = 0; y < bltheight; y++) {
                for (x = 0; x < bltwidth; x++) {
                    op.call8(s, dstPos, srcPos);
                    dstPos++;
                    srcPos++;
                }
                dstPos += dstpitch;
                srcPos += srcpitch;
            }
        }
    }

    static private final class cirrus_bitblt_rop_bkwd extends cirrus_bitblt_rop {
        public cirrus_bitblt_rop_bkwd(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int x, y;
            dstpitch += bltwidth;
            srcpitch += bltwidth;
            for (y = 0; y < bltheight; y++) {
                for (x = 0; x < bltwidth; x++) {
                    op.call8(s, dstPos, s.readb(srcPos));
                    dstPos--;
                    srcPos--;
                }
                dstPos += dstpitch;
                srcPos += srcpitch;
            }
        }
    }

    static private final class cirrus_bitblt_rop_fwd_transp_8 extends cirrus_bitblt_rop {
        public cirrus_bitblt_rop_fwd_transp_8(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int x, y;
            int p;
            dstpitch -= bltwidth;
            srcpitch -= bltwidth;
            for (y = 0; y < bltheight; y++) {
                for (x = 0; x < bltwidth; x++) {
                    p = s.readb(dstPos);
                    op.call8(s, dstPos, s.readb(srcPos));
                    if (s.readb(dstPos) == s.gr[0x34]) s.writeb(dstPos, p);
                    dstPos++;
                    srcPos++;
                }
                dstPos += dstpitch;
                srcPos += srcpitch;
            }
        }
    }

    static private final class cirrus_bitblt_rop_fwd_transp_16 extends cirrus_bitblt_rop {
        public cirrus_bitblt_rop_fwd_transp_16(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int x, y;
            int gr = s.gr[0x34] | (s.gr[0x35] << 8);
            dstpitch -= bltwidth;
            srcpitch -= bltwidth;
            for (y = 0; y < bltheight; y++) {
                for (x = 0; x < bltwidth; x += 2) {
                    int p = s.readw(dstPos);
                    op.call16(s, dstPos, s.readw(srcPos));
                    if (s.readw(dstPos) == gr) s.writew(dstPos, p);
                    dstPos += 2;
                    srcPos += 2;
                }
                dstPos += dstpitch;
                srcPos += srcpitch;
            }
        }
    }

    static private final class cirrus_bitblt_rop_bkwd_transp_8 extends cirrus_bitblt_rop {
        public cirrus_bitblt_rop_bkwd_transp_8(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int x, y;
            int p;
            dstpitch += bltwidth;
            srcpitch += bltwidth;
            for (y = 0; y < bltheight; y++) {
                for (x = 0; x < bltwidth; x++) {
                    p = s.readb(dstPos);
                    p = s.readb(dstPos);
                    if (s.readb(dstPos) == s.gr[0x34]) s.writeb(dstPos, p);
                    dstPos--;
                    srcPos--;
                }
                dstPos += dstpitch;
                srcPos += srcpitch;
            }
        }
    }

    static private final class cirrus_bitblt_rop_bkwd_transp_16 extends cirrus_bitblt_rop {
        public cirrus_bitblt_rop_bkwd_transp_16(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int x, y;
            int gr = s.gr[0x34] | (s.gr[0x35] << 8);
            dstpitch += bltwidth;
            srcpitch += bltwidth;
            for (y = 0; y < bltheight; y++) {
                for (x = 0; x < bltwidth; x += 2) {
                    int p = s.readw(dstPos);
                    op.call16(s, dstPos, s.readw(srcPos));
                    if (s.readw(dstPos) == gr) s.writew(dstPos, p);
                    dstPos -= 2;
                    srcPos -= 2;
                }
                dstPos += dstpitch;
                srcPos += srcpitch;
            }
        }
    }

    static private final class cirrus_patternfill_8 extends cirrus_bitblt_rop {
        public cirrus_patternfill_8(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, pattern_y, pattern_pitch, pattern_x;
            int src1Pos;
            int skipleft = (s.gr[0x2f] & 0x07);

            pattern_pitch = 8;
            pattern_y = s.cirrus_blt_srcaddr & 7;
            for (y = 0; y < bltheight; y++) {
                pattern_x = skipleft;
                dPos = dstPos + skipleft;
                src1Pos = srcPos + pattern_y * pattern_pitch;
                for (x = skipleft; x < bltwidth; x += 1) {
                    op.call8(s, dPos, s.readb(src1Pos + pattern_x));
                    pattern_x = (pattern_x + 1) & 7;
                    dPos += 1;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_patternfill_16 extends cirrus_bitblt_rop {
        public cirrus_patternfill_16(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, pattern_y, pattern_pitch, pattern_x;
            int src1Pos;
            int skipleft = (s.gr[0x2f] & 0x07) * 2;

            pattern_pitch = 16;
            pattern_y = s.cirrus_blt_srcaddr & 7;
            for (y = 0; y < bltheight; y++) {
                pattern_x = skipleft;
                dPos = dstPos + skipleft;
                src1Pos = srcPos + pattern_y * pattern_pitch;
                for (x = skipleft; x < bltwidth; x += 2) {
                    op.call16(s, dPos, s.readw(src1Pos + pattern_x));
                    pattern_x = (pattern_x + 2) & 15;
                    dPos += 2;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_patternfill_24 extends cirrus_bitblt_rop {
        public cirrus_patternfill_24(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, pattern_y, pattern_pitch, pattern_x;
            int src1Pos;
            int skipleft = s.gr[0x2f] & 0x1f;

            pattern_pitch = 32;
            pattern_y = s.cirrus_blt_srcaddr & 7;
            for (y = 0; y < bltheight; y++) {
                pattern_x = skipleft;
                dPos = dstPos + skipleft;
                src1Pos = srcPos + pattern_y * pattern_pitch;
                for (x = skipleft; x < bltwidth; x += 3) {
                    int src2 = src1Pos + pattern_x * 3;
                    pattern_x = (pattern_x + 1) & 7;
                    op.call8(s, dPos, s.readb(src2));
                    op.call8(s, dPos + 1, s.readb(src2 + 1));
                    op.call8(s, dPos + 2, s.readb(src2 + 2));
                    dPos += 3;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_patternfill_32 extends cirrus_bitblt_rop {
        public cirrus_patternfill_32(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, pattern_y, pattern_pitch, pattern_x;
            int src1Pos;
            int skipleft = (s.gr[0x2f] & 0x07) * 4;

            pattern_pitch = 32;
            pattern_y = s.cirrus_blt_srcaddr & 7;
            for (y = 0; y < bltheight; y++) {
                pattern_x = skipleft;
                dPos = dstPos + skipleft;
                src1Pos = srcPos + pattern_y * pattern_pitch;
                for (x = skipleft; x < bltwidth; x += 4) {
                    op.call32(s, dPos, s.readw(src1Pos + pattern_x));
                    pattern_x = (pattern_x + 4) & 31;
                    dPos += 4;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_transp_8 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_transp_8(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits, bits_xor;
            int col;
            int bitmask;
            int index;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }

            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++) ^ bits_xor;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x++) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++) ^ bits_xor;
                    }
                    index = (bits & bitmask);
                    if (index != 0) {
                        op.call8(s, dPos, col);
                    }
                    dPos++;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_transp_16 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_transp_16(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits, bits_xor;
            int col;
            int bitmask;
            int index;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 2;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }

            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++) ^ bits_xor;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 2) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++) ^ bits_xor;
                    }
                    index = (bits & bitmask);
                    if (index != 0) {
                        op.call16(s, dPos, col);
                    }
                    dPos += 2;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_transp_24 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_transp_24(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits, bits_xor;
            int col;
            int bitmask;
            int index;
            int dstskipleft = s.gr[0x2f] & 0x1f;
            int srcskipleft = dstskipleft / 3;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }

            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++) ^ bits_xor;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 3) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++) ^ bits_xor;
                    }
                    index = (bits & bitmask);
                    if (index != 0) {
                        op.call8(s, dPos, col & 0xFF);
                        op.call8(s, dPos + 1, (col >> 8) & 0xFF);
                        op.call8(s, dPos + 2, (col >> 16) & 0xFF);
                    }
                    dPos += 3;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_transp_32 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_transp_32(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits, bits_xor;
            int col;
            int bitmask;
            int index;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 4;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }

            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++) ^ bits_xor;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 4) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++) ^ bits_xor;
                    }
                    index = (bits & bitmask);
                    if (index != 0) {
                        op.call32(s, dPos, col);
                    }
                    dPos += 4;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_8 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_8(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits;
            int col;
            int bitmask;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++);
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x++) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++);
                    }
                    col = colors[(bits & bitmask) == 0 ? 0 : 1];
                    op.call8(s, dPos, col);
                    dPos++;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_16 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_16(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits;
            int col;
            int bitmask;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 2;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++);
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 2) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++);
                    }
                    col = colors[(bits & bitmask) == 0 ? 0 : 1];
                    op.call16(s, dPos, col);
                    dPos += 2;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_24 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_24(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits;
            int col;
            int bitmask;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 3;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++);
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 3) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++);
                    }
                    col = colors[(bits & bitmask) == 0 ? 0 : 1];
                    op.call8(s, dPos, col & 0xFF);
                    op.call8(s, dPos + 1, (col >> 8) & 0xFF);
                    op.call8(s, dPos + 2, (col >> 16) & 0xFF);
                    dPos += 3;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_32 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_32(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y;
            int bits;
            int col;
            int bitmask;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 4;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            for (y = 0; y < bltheight; y++) {
                bitmask = 0x80 >> srcskipleft;
                bits = s.readb(srcPos++);
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 4) {
                    if ((bitmask & 0xff) == 0) {
                        bitmask = 0x80;
                        bits = s.readb(srcPos++);
                    }
                    col = colors[(bits & bitmask) == 0 ? 0 : 1];
                    op.call32(s, dPos, col);
                    dPos += 32;
                    bitmask >>= 1;
                }
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_transp_8 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_transp_8(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits, bits_xor;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y) ^ bits_xor;
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x++) {
                    if (((bits >> bitpos) & 1) != 0) {
                        op.call8(s, dPos, col);
                    }
                    dPos++;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_transp_16 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_transp_16(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits, bits_xor;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 2;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y) ^ bits_xor;
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 2) {
                    if (((bits >> bitpos) & 1) != 0) {
                        op.call16(s, dPos, col);
                    }
                    dPos += 2;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_transp_24 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_transp_24(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits, bits_xor;
            int col;
            int dstskipleft = s.gr[0x2f] & 0x1f;
            int srcskipleft = dstskipleft / 3;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y) ^ bits_xor;
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 3) {
                    if (((bits >> bitpos) & 1) != 0) {
                        op.call8(s, dPos, col & 0xFF);
                        op.call8(s, dPos + 1, (col >> 8) & 0xFF);
                        op.call8(s, dPos + 2, (col >> 16) & 0xFF);
                    }
                    dPos += 3;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_transp_32 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_transp_32(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits, bits_xor;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 4;

            if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0) {
                bits_xor = 0xff;
                col = s.cirrus_blt_bgcol;
            } else {
                bits_xor = 0x00;
                col = s.cirrus_blt_fgcol;
            }
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y) ^ bits_xor;
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 4) {
                    if (((bits >> bitpos) & 1) != 0) {
                        op.call32(s, dPos, col);
                    }
                    dPos += 4;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_8 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_8(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y);
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x++) {
                    col = colors[(bits >> bitpos) & 1];
                    op.call8(s, dPos, col);
                    dPos++;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_16 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_16(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 2;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y);
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 2) {
                    col = colors[(bits >> bitpos) & 1];
                    op.call16(s, dPos, col);
                    dPos += 2;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_24 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_24(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 3;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y);
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 3) {
                    col = colors[(bits >> bitpos) & 1];
                    op.call8(s, dPos, col & 0xFF);
                    op.call8(s, dPos + 1, (col >> 8) & 0xFF);
                    op.call8(s, dPos + 2, (col >> 16) & 0xFF);
                    dPos += 3;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_colorexpand_pattern_32 extends cirrus_bitblt_rop {
        public cirrus_colorexpand_pattern_32(ROP_OP op) {
            super(op);
        }

        int[] colors = new int[2];

        public void call(CirrusVGAState s, int dstPos, int srcPos, int dstpitch, int srcpitch, int bltwidth, int bltheight) {
            int dPos;
            int x, y, bitpos, pattern_y;
            int bits;
            int col;
            int srcskipleft = s.gr[0x2f] & 0x07;
            int dstskipleft = srcskipleft * 4;

            colors[0] = s.cirrus_blt_bgcol;
            colors[1] = s.cirrus_blt_fgcol;
            pattern_y = s.cirrus_blt_srcaddr & 7;

            for (y = 0; y < bltheight; y++) {
                bits = s.readb(srcPos + pattern_y);
                bitpos = 7 - srcskipleft;
                dPos = dstPos + dstskipleft;
                for (x = dstskipleft; x < bltwidth; x += 4) {
                    col = colors[(bits >> bitpos) & 1];
                    op.call32(s, dPos, col);
                    dPos += 4;
                    bitpos = (bitpos - 1) & 7;
                }
                pattern_y = (pattern_y + 1) & 7;
                dstPos += dstpitch;
            }
        }
    }

    static private final class cirrus_fill_8 extends cirrus_fill {
        public cirrus_fill_8(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int dst_pitch, int width, int height) {
            int dPos, d1Pos;
            int col;
            int x, y;

            col = s.cirrus_blt_fgcol;
            d1Pos = dstPos;
            for (y = 0; y < height; y++) {
                dPos = d1Pos;
                for (x = 0; x < width; x++) {
                    op.call8(s, dPos, col);
                    dPos++;
                }
                d1Pos += dst_pitch;
            }
        }
    }

    static private final class cirrus_fill_16 extends cirrus_fill {
        public cirrus_fill_16(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int dst_pitch, int width, int height) {
            int dPos, d1Pos;
            int col;
            int x, y;

            col = s.cirrus_blt_fgcol;
            d1Pos = dstPos;
            for (y = 0; y < height; y++) {
                dPos = d1Pos;
                for (x = 0; x < width; x += 2) {
                    op.call16(s, dPos, col);
                    dPos += 2;
                }
                d1Pos += dst_pitch;
            }
        }
    }

    static private final class cirrus_fill_24 extends cirrus_fill {
        public cirrus_fill_24(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int dst_pitch, int width, int height) {
            int dPos, d1Pos;
            int col;
            int x, y;

            col = s.cirrus_blt_fgcol;
            d1Pos = dstPos;
            for (y = 0; y < height; y++) {
                dPos = d1Pos;
                for (x = 0; x < width; x += 3) {
                    op.call8(s, dPos, col & 0xFF);
                    op.call8(s, dPos + 1, (col >> 8) & 0xFF);
                    op.call8(s, dPos + 2, (col >> 16) & 0xFF);
                    dPos += 3;
                }
                d1Pos += dst_pitch;
            }
        }
    }

    static private final class cirrus_fill_32 extends cirrus_fill {
        public cirrus_fill_32(ROP_OP op) {
            super(op);
        }

        public void call(CirrusVGAState s, int dstPos, int dst_pitch, int width, int height) {
            int dPos, d1Pos;
            int col;
            int x, y;

            col = s.cirrus_blt_fgcol;
            d1Pos = dstPos;
            for (y = 0; y < height; y++) {
                dPos = d1Pos;
                for (x = 0; x < width; x += 4) {
                    op.call32(s, dPos, col);
                    dPos += 4;
                }
                d1Pos += dst_pitch;
            }
        }
    }

    static private final ROP_OP zero = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, 0);
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, 0);
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, 0);
        }
    };

    static private final ROP_OP src_and_dst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, s.readb(dstPos) & srcColor);
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, s.readw(dstPos) & srcColor);
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, s.readd(dstPos) & srcColor);
        }
    };

    static private final ROP_OP src_and_notdst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, srcColor & (~s.readb(dstPos)));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, srcColor & (~s.readw(dstPos)));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, srcColor & (~s.readd(dstPos)));
        }
    };

    static private final ROP_OP notdst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, ~s.readb(dstPos));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, ~s.readw(dstPos));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, ~s.readd(dstPos));
        }
    };

    static private final ROP_OP src = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, srcColor);
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, srcColor);
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, srcColor);
        }
    };

    static private final ROP_OP one = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, 0xFF);
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, 0xFFFF);
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, 0xFFFFFFFF);
        }
    };

    static private final ROP_OP notsrc_and_dst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, (~srcColor) & s.readb(dstPos));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, (~srcColor) & s.readw(dstPos));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, (~srcColor) & s.readd(dstPos));
        }
    };

    static private final ROP_OP src_xor_dst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, srcColor ^ s.readb(dstPos));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, srcColor ^ s.readw(dstPos));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, srcColor ^ s.readd(dstPos));
        }
    };

    static private final ROP_OP src_or_dst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, srcColor | s.readb(dstPos));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, srcColor | s.readw(dstPos));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, srcColor | s.readd(dstPos));
        }
    };

    static private final ROP_OP notsrc_or_notdst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, (~srcColor) | (~s.readb(dstPos)));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, (~srcColor) | (~s.readw(dstPos)));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, (~srcColor) | (~s.readd(dstPos)));
        }
    };

    static private final ROP_OP src_notxor_dst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, ~(srcColor ^ s.readb(dstPos)));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, ~(srcColor ^ s.readw(dstPos)));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, ~(srcColor ^ s.readd(dstPos)));
        }
    };

    static private final ROP_OP src_or_notdst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, srcColor | (~s.readb(dstPos)));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, srcColor | (~s.readw(dstPos)));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, srcColor | (~s.readd(dstPos)));
        }
    };

    static private final ROP_OP notsrc = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, ~srcColor);
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, ~srcColor);
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, ~srcColor);
        }
    };

    static private final ROP_OP notsrc_or_dst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, (~srcColor) | s.readb(dstPos));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, (~srcColor) | s.readw(dstPos));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, (~srcColor) | s.readd(dstPos));
        }
    };

    static private final ROP_OP notsrc_and_notdst = new ROP_OP() {
        public void call8(CirrusVGAState s, int dstPos, int srcColor) {
            s.writeb(dstPos, (~srcColor) | (~s.readb(dstPos)));
        }

        public void call16(CirrusVGAState s, int dstPos, int srcColor) {
            s.writew(dstPos, (~srcColor) | (~s.readw(dstPos)));
        }

        public void call32(CirrusVGAState s, int dstPos, int srcColor) {
            s.writed(dstPos, (~srcColor) | (~s.readd(dstPos)));
        }
    };

    static private final cirrus_bitblt_rop_t[] cirrus_fwd_rop = new cirrus_bitblt_rop_t[]{
            new cirrus_bitblt_rop_fwd(zero),
            new cirrus_bitblt_rop_fwd(src_and_dst),
            cirrus_bitblt_rop_nop,
            new cirrus_bitblt_rop_fwd(src_and_notdst),
            new cirrus_bitblt_rop_fwd(notdst),
            new cirrus_bitblt_rop_fwd(src),
            new cirrus_bitblt_rop_fwd(one),
            new cirrus_bitblt_rop_fwd(notsrc_and_dst),
            new cirrus_bitblt_rop_fwd(src_xor_dst),
            new cirrus_bitblt_rop_fwd(src_or_dst),
            new cirrus_bitblt_rop_fwd(notsrc_or_notdst),
            new cirrus_bitblt_rop_fwd(src_notxor_dst),
            new cirrus_bitblt_rop_fwd(src_or_notdst),
            new cirrus_bitblt_rop_fwd(notsrc),
            new cirrus_bitblt_rop_fwd(notsrc_or_dst),
            new cirrus_bitblt_rop_fwd(notsrc_and_notdst),
    };

    static private final cirrus_bitblt_rop_t[] cirrus_bkwd_rop = new cirrus_bitblt_rop_t[]{
            new cirrus_bitblt_rop_bkwd(zero),
            new cirrus_bitblt_rop_bkwd(src_and_dst),
            cirrus_bitblt_rop_nop,
            new cirrus_bitblt_rop_bkwd(src_and_notdst),
            new cirrus_bitblt_rop_bkwd(notdst),
            new cirrus_bitblt_rop_bkwd(src),
            new cirrus_bitblt_rop_bkwd(one),
            new cirrus_bitblt_rop_bkwd(notsrc_and_dst),
            new cirrus_bitblt_rop_bkwd(src_xor_dst),
            new cirrus_bitblt_rop_bkwd(src_or_dst),
            new cirrus_bitblt_rop_bkwd(notsrc_or_notdst),
            new cirrus_bitblt_rop_bkwd(src_notxor_dst),
            new cirrus_bitblt_rop_bkwd(src_or_notdst),
            new cirrus_bitblt_rop_bkwd(notsrc),
            new cirrus_bitblt_rop_bkwd(notsrc_or_dst),
            new cirrus_bitblt_rop_bkwd(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] TRANSP_ROP_fwd_transp(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[2];
        result[0] = new cirrus_bitblt_rop_fwd_transp_8(op);
        result[0] = new cirrus_bitblt_rop_fwd_transp_16(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_fwd_transp_rop = new cirrus_bitblt_rop_t[][]{
            TRANSP_ROP_fwd_transp(zero),
            TRANSP_ROP_fwd_transp(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            TRANSP_ROP_fwd_transp(src_and_notdst),
            TRANSP_ROP_fwd_transp(notdst),
            TRANSP_ROP_fwd_transp(src),
            TRANSP_ROP_fwd_transp(one),
            TRANSP_ROP_fwd_transp(notsrc_and_dst),
            TRANSP_ROP_fwd_transp(src_xor_dst),
            TRANSP_ROP_fwd_transp(src_or_dst),
            TRANSP_ROP_fwd_transp(notsrc_or_notdst),
            TRANSP_ROP_fwd_transp(src_notxor_dst),
            TRANSP_ROP_fwd_transp(src_or_notdst),
            TRANSP_ROP_fwd_transp(notsrc),
            TRANSP_ROP_fwd_transp(notsrc_or_dst),
            TRANSP_ROP_fwd_transp(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] TRANSP_ROP_bkwd_transp(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[2];
        result[0] = new cirrus_bitblt_rop_fwd_transp_8(op);
        result[0] = new cirrus_bitblt_rop_fwd_transp_16(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_bkwd_transp_rop = new cirrus_bitblt_rop_t[][]{
            TRANSP_ROP_bkwd_transp(zero),
            TRANSP_ROP_bkwd_transp(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            TRANSP_ROP_bkwd_transp(src_and_notdst),
            TRANSP_ROP_bkwd_transp(notdst),
            TRANSP_ROP_bkwd_transp(src),
            TRANSP_ROP_bkwd_transp(one),
            TRANSP_ROP_bkwd_transp(notsrc_and_dst),
            TRANSP_ROP_bkwd_transp(src_xor_dst),
            TRANSP_ROP_bkwd_transp(src_or_dst),
            TRANSP_ROP_bkwd_transp(notsrc_or_notdst),
            TRANSP_ROP_bkwd_transp(src_notxor_dst),
            TRANSP_ROP_bkwd_transp(src_or_notdst),
            TRANSP_ROP_bkwd_transp(notsrc),
            TRANSP_ROP_bkwd_transp(notsrc_or_dst),
            TRANSP_ROP_bkwd_transp(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] ROP2patternfill(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[4];
        result[0] = new cirrus_patternfill_8(op);
        result[1] = new cirrus_patternfill_16(op);
        result[2] = new cirrus_patternfill_24(op);
        result[3] = new cirrus_patternfill_32(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_patternfill = new cirrus_bitblt_rop_t[][]{
            ROP2patternfill(zero),
            ROP2patternfill(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            ROP2patternfill(src_and_notdst),
            ROP2patternfill(notdst),
            ROP2patternfill(src),
            ROP2patternfill(zero),
            ROP2patternfill(notsrc_and_dst),
            ROP2patternfill(src_xor_dst),
            ROP2patternfill(src_or_dst),
            ROP2patternfill(notsrc_or_notdst),
            ROP2patternfill(src_notxor_dst),
            ROP2patternfill(src_or_notdst),
            ROP2patternfill(notsrc),
            ROP2patternfill(notsrc_or_dst),
            ROP2patternfill(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] ROP2colorexpandtransp(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[4];
        result[0] = new cirrus_colorexpand_transp_8(op);
        result[1] = new cirrus_colorexpand_transp_16(op);
        result[2] = new cirrus_colorexpand_transp_24(op);
        result[3] = new cirrus_colorexpand_transp_32(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_colorexpand_transp = new cirrus_bitblt_rop_t[][]{
            ROP2colorexpandtransp(zero),
            ROP2colorexpandtransp(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            ROP2colorexpandtransp(src_and_notdst),
            ROP2colorexpandtransp(notdst),
            ROP2colorexpandtransp(src),
            ROP2colorexpandtransp(zero),
            ROP2colorexpandtransp(notsrc_and_dst),
            ROP2colorexpandtransp(src_xor_dst),
            ROP2colorexpandtransp(src_or_dst),
            ROP2colorexpandtransp(notsrc_or_notdst),
            ROP2colorexpandtransp(src_notxor_dst),
            ROP2colorexpandtransp(src_or_notdst),
            ROP2colorexpandtransp(notsrc),
            ROP2colorexpandtransp(notsrc_or_dst),
            ROP2colorexpandtransp(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] ROP2colorexpand(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[4];
        result[0] = new cirrus_colorexpand_8(op);
        result[1] = new cirrus_colorexpand_16(op);
        result[2] = new cirrus_colorexpand_24(op);
        result[3] = new cirrus_colorexpand_32(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_colorexpand = new cirrus_bitblt_rop_t[][]{
            ROP2colorexpand(zero),
            ROP2colorexpand(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            ROP2colorexpand(src_and_notdst),
            ROP2colorexpand(notdst),
            ROP2colorexpand(src),
            ROP2colorexpand(zero),
            ROP2colorexpand(notsrc_and_dst),
            ROP2colorexpand(src_xor_dst),
            ROP2colorexpand(src_or_dst),
            ROP2colorexpand(notsrc_or_notdst),
            ROP2colorexpand(src_notxor_dst),
            ROP2colorexpand(src_or_notdst),
            ROP2colorexpand(notsrc),
            ROP2colorexpand(notsrc_or_dst),
            ROP2colorexpand(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] ROP2colorexpandpatterntransp(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[4];
        result[0] = new cirrus_colorexpand_pattern_transp_8(op);
        result[1] = new cirrus_colorexpand_pattern_transp_16(op);
        result[2] = new cirrus_colorexpand_pattern_transp_24(op);
        result[3] = new cirrus_colorexpand_pattern_transp_32(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_colorexpand_pattern_transp = new cirrus_bitblt_rop_t[][]{
            ROP2colorexpandpatterntransp(zero),
            ROP2colorexpandpatterntransp(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            ROP2colorexpandpatterntransp(src_and_notdst),
            ROP2colorexpandpatterntransp(notdst),
            ROP2colorexpandpatterntransp(src),
            ROP2colorexpandpatterntransp(zero),
            ROP2colorexpandpatterntransp(notsrc_and_dst),
            ROP2colorexpandpatterntransp(src_xor_dst),
            ROP2colorexpandpatterntransp(src_or_dst),
            ROP2colorexpandpatterntransp(notsrc_or_notdst),
            ROP2colorexpandpatterntransp(src_notxor_dst),
            ROP2colorexpandpatterntransp(src_or_notdst),
            ROP2colorexpandpatterntransp(notsrc),
            ROP2colorexpandpatterntransp(notsrc_or_dst),
            ROP2colorexpandpatterntransp(notsrc_and_notdst),
    };

    static private cirrus_bitblt_rop_t[] ROP2colorexpandpattern(ROP_OP op) {
        cirrus_bitblt_rop_t[] result = new cirrus_bitblt_rop_t[4];
        result[0] = new cirrus_colorexpand_pattern_8(op);
        result[1] = new cirrus_colorexpand_pattern_16(op);
        result[2] = new cirrus_colorexpand_pattern_24(op);
        result[3] = new cirrus_colorexpand_pattern_32(op);
        return result;
    }

    static private final cirrus_bitblt_rop_t[][] cirrus_colorexpand_pattern = new cirrus_bitblt_rop_t[][]{
            ROP2colorexpandpatterntransp(zero),
            ROP2colorexpandpatterntransp(src_and_dst),
            new cirrus_bitblt_rop_t[]{cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop, cirrus_bitblt_rop_nop},
            ROP2colorexpandpatterntransp(src_and_notdst),
            ROP2colorexpandpatterntransp(notdst),
            ROP2colorexpandpatterntransp(src),
            ROP2colorexpandpatterntransp(zero),
            ROP2colorexpandpatterntransp(notsrc_and_dst),
            ROP2colorexpandpatterntransp(src_xor_dst),
            ROP2colorexpandpatterntransp(src_or_dst),
            ROP2colorexpandpatterntransp(notsrc_or_notdst),
            ROP2colorexpandpatterntransp(src_notxor_dst),
            ROP2colorexpandpatterntransp(src_or_notdst),
            ROP2colorexpandpatterntransp(notsrc),
            ROP2colorexpandpatterntransp(notsrc_or_dst),
            ROP2colorexpandpatterntransp(notsrc_and_notdst),
    };

    static private cirrus_fill_t[] ROP2fill(ROP_OP op) {
        cirrus_fill_t[] result = new cirrus_fill_t[4];
        result[0] = new cirrus_fill_8(op);
        result[1] = new cirrus_fill_16(op);
        result[2] = new cirrus_fill_24(op);
        result[3] = new cirrus_fill_32(op);
        return result;
    }

    static private final cirrus_fill_t[][] cirrus_fill = new cirrus_fill_t[][]{
            ROP2fill(zero),
            ROP2fill(src_and_dst),
            new cirrus_fill_t[]{cirrus_bitblt_fill_nop, cirrus_bitblt_fill_nop, cirrus_bitblt_fill_nop, cirrus_bitblt_fill_nop},
            ROP2fill(src_and_notdst),
            ROP2fill(notdst),
            ROP2fill(src),
            ROP2fill(zero),
            ROP2fill(notsrc_and_dst),
            ROP2fill(src_xor_dst),
            ROP2fill(src_or_dst),
            ROP2fill(notsrc_or_notdst),
            ROP2fill(src_notxor_dst),
            ROP2fill(src_or_notdst),
            ROP2fill(notsrc),
            ROP2fill(notsrc_or_dst),
            ROP2fill(notsrc_and_notdst),
    };

    static private void cirrus_bitblt_fgcol(CirrusVGAState s) {
        switch (s.cirrus_blt_pixelwidth) {
            case 1:
                s.cirrus_blt_fgcol = s.cirrus_shadow_gr1;
                break;
            case 2:
                s.cirrus_blt_fgcol = s.cirrus_shadow_gr1 | (s.gr[0x11] << 8);
                break;
            case 3:
                s.cirrus_blt_fgcol = s.cirrus_shadow_gr1 | (s.gr[0x11] << 8) | (s.gr[0x13] << 16);
                break;
            default:
            case 4:
                s.cirrus_blt_fgcol = s.cirrus_shadow_gr1 | (s.gr[0x11] << 8) | (s.gr[0x13] << 16) | (s.gr[0x15] << 24);
                break;
        }
    }

    static private void cirrus_bitblt_bgcol(CirrusVGAState s) {
        switch (s.cirrus_blt_pixelwidth) {
            case 1:
                s.cirrus_blt_bgcol = s.cirrus_shadow_gr0;
                break;
            case 2:
                s.cirrus_blt_bgcol = s.cirrus_shadow_gr0 | (s.gr[0x10] << 8);
                break;
            case 3:
                s.cirrus_blt_bgcol = s.cirrus_shadow_gr0 | (s.gr[0x10] << 8) | (s.gr[0x12] << 16);
                break;
            default:
            case 4:
                s.cirrus_blt_bgcol = s.cirrus_shadow_gr0 | (s.gr[0x10] << 8) | (s.gr[0x12] << 16) | (s.gr[0x14] << 24);
                break;
        }
    }

    static private void cirrus_invalidate_region(CirrusVGAState s, int off_begin, int off_pitch, int bytesperline, int lines) {
//        int y;
//        int off_cur;
//        int off_cur_end;
//
//        for (y = 0; y < lines; y++) {
//    	off_cur = off_begin;
//    	off_cur_end = (off_cur + bytesperline) & s.cirrus_addr_mask;
//        memory_region_set_dirty(&s.vram, off_cur, off_cur_end - off_cur);
//    	off_begin += off_pitch;
    }

    static private boolean cirrus_bitblt_common_patterncopy(CirrusVGAState s, int srcPos) {
        int dstPos;

        dstPos = s.vram_ptr + (s.cirrus_blt_dstaddr & s.cirrus_addr_mask);

        if (BLTUNSAFE(s))
            return false;

        s.cirrus_rop.call(s, dstPos, srcPos, s.cirrus_blt_dstpitch, 0, s.cirrus_blt_width, s.cirrus_blt_height);
        cirrus_invalidate_region(s, s.cirrus_blt_dstaddr, s.cirrus_blt_dstpitch, s.cirrus_blt_width, s.cirrus_blt_height);
        return true;
    }
    
    /* fill */

    static private int cirrus_bitblt_solidfill(CirrusVGAState s, int blt_rop) {
        cirrus_fill_t rop_func;

        if (BLTUNSAFE(s))
            return 0;
        rop_func = cirrus_fill[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
        rop_func.call(s, s.vram_ptr + (s.cirrus_blt_dstaddr & s.cirrus_addr_mask), s.cirrus_blt_dstpitch, s.cirrus_blt_width, s.cirrus_blt_height);
        cirrus_invalidate_region(s, s.cirrus_blt_dstaddr, s.cirrus_blt_dstpitch, s.cirrus_blt_width, s.cirrus_blt_height);
        cirrus_bitblt_reset(s);
        return 1;
    }

    /***************************************
     *
     *  bitblt (video-to-video)
     *
     ***************************************/

    static private boolean cirrus_bitblt_videotovideo_patterncopy(CirrusVGAState s) {
        return cirrus_bitblt_common_patterncopy(s, s.vram_ptr + ((s.cirrus_blt_srcaddr & ~7) & s.cirrus_addr_mask));
    }

    static private void cirrus_do_copy(CirrusVGAState s, int dst, int src, int w, int h) {
//        int sx = 0, sy = 0;
//        int dx = 0, dy = 0;
//        int depth = 0;
//        boolean notify = false;
    
        /* make sure to only copy if it's a plain copy ROP */
//        if (s.cirrus_rop == cirrus_fwd_rop[5] /* cirrus_bitblt_rop_fwd_src */ || s.cirrus_rop == cirrus_bkwd_rop[5] /* cirrus_bitblt_rop_bkwd_src */) {
//
//            int width = s.get_resolutionCx();
//            int height = s.get_resolutionCy();
//
//            depth = s.get_bpp() / 8;
//
//            /* extra x, y */
//            sx = (src % Math.abs(s.cirrus_blt_srcpitch)) / depth;
//            sy = (src / Math.abs(s.cirrus_blt_srcpitch));
//            dx = (dst % Math.abs(s.cirrus_blt_dstpitch)) / depth;
//            dy = (dst / Math.abs(s.cirrus_blt_dstpitch));
//
//            /* normalize width */
//            w /= depth;
//
//            /* if we're doing a backward copy, we have to adjust
//               our x/y to be the upper left corner (instead of the lower
//               right corner) */
//            if (s.cirrus_blt_dstpitch < 0) {
//                sx -= (s.cirrus_blt_width / depth) - 1;
//                dx -= (s.cirrus_blt_width / depth) - 1;
//                sy -= s.cirrus_blt_height - 1;
//                dy -= s.cirrus_blt_height - 1;
//            }
//
//            /* are we in the visible portion of memory? */
//            if (sx >= 0 && sy >= 0 && dx >= 0 && dy >= 0 &&
//                (sx + w) <= width && (sy + h) <= height &&
//                (dx + w) <= width && (dy + h) <= height) {
//                notify = true;
//            }
//        }
    
        /* we have to flush all pending changes so that the copy
           is generated at the appropriate moment in time */
//        if (notify)
//    	vga_hw_update();

        s.cirrus_rop.call(s, s.vram_ptr + (s.cirrus_blt_dstaddr & s.cirrus_addr_mask), s.vram_ptr + (s.cirrus_blt_srcaddr & s.cirrus_addr_mask), s.cirrus_blt_dstpitch, s.cirrus_blt_srcpitch, s.cirrus_blt_width, s.cirrus_blt_height);

//        if (notify)
//    	    qemu_console_copy(s.ds, sx, sy, dx, dy, s.cirrus_blt_width / depth, s.cirrus_blt_height);
    
        /* we don't have to notify the display that this portion has
           changed since qemu_console_copy implies this */

        cirrus_invalidate_region(s, s.cirrus_blt_dstaddr, s.cirrus_blt_dstpitch, s.cirrus_blt_width, s.cirrus_blt_height);
    }

    static private boolean cirrus_bitblt_videotovideo_copy(CirrusVGAState s) {
        if (BLTUNSAFE(s))
            return false;

        cirrus_do_copy(s, s.cirrus_blt_dstaddr - s.start_addr, s.cirrus_blt_srcaddr - s.start_addr, s.cirrus_blt_width, s.cirrus_blt_height);

        return true;
    }

    /***************************************
     *
     *  bitblt (cpu-to-video)
     *
     ***************************************/

    static private void cirrus_bitblt_cputovideo_next(CirrusVGAState s) {
        int copy_count;

        if (s.cirrus_srccounter > 0) {
            if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_PATTERNCOPY) != 0) {
                cirrus_bitblt_common_patterncopy(s, s.cirrus_bltbufPos);
                s.cirrus_srccounter = 0;
                cirrus_bitblt_reset(s);
            } else {
                /* at least one scan line */
                do {
                    s.cirrus_rop.call(s, s.vram_ptr + (s.cirrus_blt_dstaddr & s.cirrus_addr_mask), s.cirrus_bltbufPos, 0, 0, s.cirrus_blt_width, 1);
                    cirrus_invalidate_region(s, s.cirrus_blt_dstaddr, 0, s.cirrus_blt_width, 1);
                    s.cirrus_blt_dstaddr += s.cirrus_blt_dstpitch;
                    s.cirrus_srccounter -= s.cirrus_blt_srcpitch;
                    if (s.cirrus_srccounter <= 0) {
                        s.cirrus_srccounter = 0;
                        cirrus_bitblt_reset(s);
                        break;
                    }
                    /* more bytes than needed can be transferred because of
                       word alignment, so we keep them for the next line */
                    /* XXX: keep alignment to speed up transfer */
                    int end_ptr = s.cirrus_bltbufPos + s.cirrus_blt_srcpitch;
                    copy_count = s.cirrus_srcptr_end - end_ptr;
                    s.memmove(s.cirrus_bltbufPos, end_ptr, copy_count);
                    s.cirrus_srcptr = s.cirrus_bltbufPos + copy_count;
                    s.cirrus_srcptr_end = s.cirrus_bltbufPos + s.cirrus_blt_srcpitch;
                } while (s.cirrus_srcptr >= s.cirrus_srcptr_end);
            }
        }
    }

    /***************************************
     *
     *  bitblt wrapper
     *
     ***************************************/

    static private void cirrus_bitblt_reset(CirrusVGAState s) {
        boolean need_update;

        s.gr[0x31] &= ~(CIRRUS_BLT_START | CIRRUS_BLT_BUSY | CIRRUS_BLT_FIFOUSED);
        need_update = s.cirrus_srcptr != s.cirrus_bltbufPos || s.cirrus_srcptr_end != s.cirrus_bltbufPos;
        s.cirrus_srcptr = s.cirrus_bltbufPos;
        s.cirrus_srcptr_end = s.cirrus_bltbufPos;
        s.cirrus_srccounter = 0;
        if (!need_update)
            return;
        cirrus_update_memory_access(s);
    }

    static private boolean cirrus_bitblt_cputovideo(CirrusVGAState s) {
        int w;

        s.cirrus_blt_mode &= ~CIRRUS_BLTMODE_MEMSYSSRC;
        s.cirrus_srcptr = s.cirrus_bltbufPos;
        s.cirrus_srcptr_end = s.cirrus_bltbufPos;

        if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_PATTERNCOPY) != 0) {
            if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_COLOREXPAND) != 0) {
                s.cirrus_blt_srcpitch = 8;
            } else {
                /* XXX: check for 24 bpp */
                s.cirrus_blt_srcpitch = 8 * 8 * s.cirrus_blt_pixelwidth;
            }
            s.cirrus_srccounter = s.cirrus_blt_srcpitch;
        } else {
            if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_COLOREXPAND) != 0) {
                w = s.cirrus_blt_width / s.cirrus_blt_pixelwidth;
                if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_DWORDGRANULARITY) != 0)
                    s.cirrus_blt_srcpitch = ((w + 31) >> 5);
                else
                    s.cirrus_blt_srcpitch = ((w + 7) >> 3);
            } else {
                /* always align input size to 32 bits */
                s.cirrus_blt_srcpitch = (s.cirrus_blt_width + 3) & ~3;
            }
            s.cirrus_srccounter = s.cirrus_blt_srcpitch * s.cirrus_blt_height;
        }
        s.cirrus_srcptr = s.cirrus_bltbufPos;
        s.cirrus_srcptr_end = s.cirrus_bltbufPos + s.cirrus_blt_srcpitch;
        cirrus_update_memory_access(s);
        return true;
    }

    static private boolean warning = false;

    static private boolean cirrus_bitblt_videotocpu(CirrusVGAState s) {
        if (!warning) {
            System.out.println("cirrus: bitblt (video to cpu) is not implemented yet");
            warning = true;
        }
        return false;
    }

    static boolean cirrus_bitblt_videotovideo(CirrusVGAState s) {
        boolean ret;

        if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_PATTERNCOPY) != 0) {
            ret = cirrus_bitblt_videotovideo_patterncopy(s);
        } else {
            ret = cirrus_bitblt_videotovideo_copy(s);
        }
        if (ret)
            cirrus_bitblt_reset(s);
        return ret;
    }

    static private void cirrus_bitblt_start(CirrusVGAState s) {
        int blt_rop;

        s.gr[0x31] |= CIRRUS_BLT_BUSY;

        s.cirrus_blt_width = (s.gr[0x20] | (s.gr[0x21] << 8)) + 1;
        s.cirrus_blt_height = (s.gr[0x22] | (s.gr[0x23] << 8)) + 1;
        s.cirrus_blt_dstpitch = (s.gr[0x24] | (s.gr[0x25] << 8));
        s.cirrus_blt_srcpitch = (s.gr[0x26] | (s.gr[0x27] << 8));
        s.cirrus_blt_dstaddr =
                (s.gr[0x28] | (s.gr[0x29] << 8) | (s.gr[0x2a] << 16));
        s.cirrus_blt_srcaddr =
                (s.gr[0x2c] | (s.gr[0x2d] << 8) | (s.gr[0x2e] << 16));
        s.cirrus_blt_mode = s.gr[0x30];
        s.cirrus_blt_modeext = s.gr[0x33];
        blt_rop = s.gr[0x32];

//    #ifdef DEBUG_BITBLT
//        printf("rop=0x%02x mode=0x%02x modeext=0x%02x w=%d h=%d dpitch=%d spitch=%d daddr=0x%08x saddr=0x%08x writemask=0x%02x\n",
//               blt_rop,
//               s.cirrus_blt_mode,
//               s.cirrus_blt_modeext,
//               s.cirrus_blt_width,
//               s.cirrus_blt_height,
//               s.cirrus_blt_dstpitch,
//               s.cirrus_blt_srcpitch,
//               s.cirrus_blt_dstaddr,
//               s.cirrus_blt_srcaddr,
//               s.gr[0x2f]);
//    #endif

        switch (s.cirrus_blt_mode & CIRRUS_BLTMODE_PIXELWIDTHMASK) {
            case CIRRUS_BLTMODE_PIXELWIDTH8:
                s.cirrus_blt_pixelwidth = 1;
                break;
            case CIRRUS_BLTMODE_PIXELWIDTH16:
                s.cirrus_blt_pixelwidth = 2;
                break;
            case CIRRUS_BLTMODE_PIXELWIDTH24:
                s.cirrus_blt_pixelwidth = 3;
                break;
            case CIRRUS_BLTMODE_PIXELWIDTH32:
                s.cirrus_blt_pixelwidth = 4;
                break;
            default:
                //printf("cirrus: bitblt - pixel width is unknown\n");
                cirrus_bitblt_reset(s);
                return;
        }
        s.cirrus_blt_mode &= ~CIRRUS_BLTMODE_PIXELWIDTHMASK;

        if ((s.cirrus_blt_mode & (CIRRUS_BLTMODE_MEMSYSSRC | CIRRUS_BLTMODE_MEMSYSDEST)) == (CIRRUS_BLTMODE_MEMSYSSRC | CIRRUS_BLTMODE_MEMSYSDEST)) {
            // printf("cirrus: bitblt - memory-to-memory copy is requested\n");
            cirrus_bitblt_reset(s);
            return;
        }

        if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_SOLIDFILL) != 0 && (s.cirrus_blt_mode & (CIRRUS_BLTMODE_MEMSYSDEST | CIRRUS_BLTMODE_TRANSPARENTCOMP | CIRRUS_BLTMODE_PATTERNCOPY | CIRRUS_BLTMODE_COLOREXPAND)) == (CIRRUS_BLTMODE_PATTERNCOPY | CIRRUS_BLTMODE_COLOREXPAND)) {
            cirrus_bitblt_fgcol(s);
            cirrus_bitblt_solidfill(s, blt_rop);
        } else {
            if ((s.cirrus_blt_mode & (CIRRUS_BLTMODE_COLOREXPAND | CIRRUS_BLTMODE_PATTERNCOPY)) == CIRRUS_BLTMODE_COLOREXPAND) {
                if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_TRANSPARENTCOMP) != 0) {
                    if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0)
                        cirrus_bitblt_bgcol(s);
                    else
                        cirrus_bitblt_fgcol(s);
                    s.cirrus_rop = cirrus_colorexpand_transp[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                } else {
                    cirrus_bitblt_fgcol(s);
                    cirrus_bitblt_bgcol(s);
                    s.cirrus_rop = cirrus_colorexpand[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                }
            } else if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_PATTERNCOPY) != 0) {
                if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_COLOREXPAND) != 0) {
                    if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_TRANSPARENTCOMP) != 0) {
                        if ((s.cirrus_blt_modeext & CIRRUS_BLTMODEEXT_COLOREXPINV) != 0)
                            cirrus_bitblt_bgcol(s);
                        else
                            cirrus_bitblt_fgcol(s);
                        s.cirrus_rop = cirrus_colorexpand_pattern_transp[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                    } else {
                        cirrus_bitblt_fgcol(s);
                        cirrus_bitblt_bgcol(s);
                        s.cirrus_rop = cirrus_colorexpand_pattern[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                    }
                } else {
                    s.cirrus_rop = cirrus_patternfill[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                }
            } else {
                if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_TRANSPARENTCOMP) != 0) {
                    if (s.cirrus_blt_pixelwidth > 2) {
                        System.out.println("src transparent without colorexpand must be 8bpp or 16bpp");
                        cirrus_bitblt_reset(s);
                        return;
                    }
                    if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_BACKWARDS) != 0) {
                        s.cirrus_blt_dstpitch = -s.cirrus_blt_dstpitch;
                        s.cirrus_blt_srcpitch = -s.cirrus_blt_srcpitch;
                        s.cirrus_rop = cirrus_bkwd_transp_rop[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                    } else {
                        s.cirrus_rop = cirrus_fwd_transp_rop[rop_to_index[blt_rop]][s.cirrus_blt_pixelwidth - 1];
                    }
                } else {
                    if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_BACKWARDS) != 0) {
                        s.cirrus_blt_dstpitch = -s.cirrus_blt_dstpitch;
                        s.cirrus_blt_srcpitch = -s.cirrus_blt_srcpitch;
                        s.cirrus_rop = cirrus_bkwd_rop[rop_to_index[blt_rop]];
                    } else {
                        s.cirrus_rop = cirrus_fwd_rop[rop_to_index[blt_rop]];
                    }
                }
            }
            // setup bitblt engine.
            if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_MEMSYSSRC) != 0) {
                if (!cirrus_bitblt_cputovideo(s)) {
                    cirrus_bitblt_reset(s);
                }
            } else if ((s.cirrus_blt_mode & CIRRUS_BLTMODE_MEMSYSDEST) != 0) {
                if (!cirrus_bitblt_videotocpu(s)) {
                    cirrus_bitblt_reset(s);
                }
            } else {
                if (!cirrus_bitblt_videotovideo(s)) {
                    cirrus_bitblt_reset(s);
                }
            }
        }
    }

    static private void cirrus_write_bitblt(CirrusVGAState s, int reg_value) {
        int old_value;

        old_value = s.gr[0x31];
        s.gr[0x31] = reg_value;

        if (((old_value & CIRRUS_BLT_RESET) != 0) && ((reg_value & CIRRUS_BLT_RESET) == 0)) {
            cirrus_bitblt_reset(s);
        } else if (((old_value & CIRRUS_BLT_START) == 0) && ((reg_value & CIRRUS_BLT_START) != 0)) {
            cirrus_bitblt_start(s);
        }
    }


    /***************************************
     *
     *  basic parameters
     *
     ***************************************/

//    static private void cirrus_get_offsets(VGACommonState s1,
//                                   uint32_t *pline_offset,
//                                   uint32_t *pstart_addr,
//                                   uint32_t *pline_compare)
//    {
//        CirrusVGAState * s = container_of(s1, CirrusVGAState, vga);
//        uint32_t start_addr, line_offset, line_compare;
//
//        line_offset = s.cr[0x13]
//    	| ((s.cr[0x1b] & 0x10) << 4);
//        line_offset <<= 3;
//        *pline_offset = line_offset;
//
//        start_addr = (s.cr[0x0c] << 8)
//    	| s.cr[0x0d]
//    	| ((s.cr[0x1b] & 0x01) << 16)
//    	| ((s.cr[0x1b] & 0x0c) << 15)
//    	| ((s.cr[0x1d] & 0x80) << 12);
//        *pstart_addr = start_addr;
//
//        line_compare = s.cr[0x18] |
//            ((s.cr[0x07] & 0x10) << 4) |
//            ((s.cr[0x09] & 0x40) << 3);
//        *pline_compare = line_compare;
//    }
    static private int cirrus_get_bpp16_depth(CirrusVGAState s) {
        int ret = 16;

        switch (s.cirrus_hidden_dac_data & 0xf) {
            case 0:
                ret = 15;
                break;			/* Sierra HiColor */
            case 1:
                ret = 16;
                break;			/* XGA HiColor */
            default:
                if (DEBUG)
                    System.out.println("cirrus: invalid DAC value " + (s.cirrus_hidden_dac_data & 0xf) + " in 16bpp");
                ret = 15;		/* XXX */
                break;
        }
        return ret;
    }

    static private class cirrus_get_bpp implements VGACommonState.get_func {
        public int call(VGACommonState c) {
            CirrusVGAState s = (CirrusVGAState) c;
            int ret = 8;

            if ((s.sr[0x07] & 0x01) != 0) {
        /* Cirrus SVGA */
                switch (s.sr[0x07] & CIRRUS_SR7_BPP_MASK) {
                    case CIRRUS_SR7_BPP_8:
                        ret = 8;
                        break;
                    case CIRRUS_SR7_BPP_16_DOUBLEVCLK:
                        ret = cirrus_get_bpp16_depth(s);
                        break;
                    case CIRRUS_SR7_BPP_24:
                        ret = 24;
                        break;
                    case CIRRUS_SR7_BPP_16:
                        ret = cirrus_get_bpp16_depth(s);
                        break;
                    case CIRRUS_SR7_BPP_32:
                        ret = 32;
                        break;
                    default:
//    #ifdef DEBUG_CIRRUS
//    	    printf("cirrus: unknown bpp - sr7=%x\n", s->vga.sr[0x7]);
//    #endif
                        ret = 8;
                        break;
                }
            } else {
    	/* VGA */
                ret = 0;
            }

            return ret;
        }
    }


    static private class cirrus_get_resolutionCy implements VGACommonState.get_func {
        public int call(VGACommonState s) {
            int height = s.cr[0x12] | ((s.cr[0x07] & 0x02) << 7) | ((s.cr[0x07] & 0x40) << 3);
            height = (height + 1);
            /* interlace support */
            if ((s.cr[0x1a] & 0x01) != 0)
                height = height * 2;
            return height;
        }
    }

    static private class cirrus_get_resolutionCx implements VGACommonState.get_func {
        public int call(VGACommonState s) {
            return (s.cr[0x01] + 1) * 8;
        }
    }

    /***************************************
     *
     * bank memory
     *
     ***************************************/

    static void cirrus_update_bank_ptr(CirrusVGAState s, int bank_index) {
        int offset;
        int limit;

        if ((s.gr[0x0b] & 0x01) != 0)	/* dual bank */
            offset = s.gr[0x09 + bank_index];
        else			/* single bank */
            offset = s.gr[0x09];

        if ((s.gr[0x0b] & 0x20) != 0)
            offset <<= 14;
        else
            offset <<= 12;

        if (s.real_vram_size <= offset)
            limit = 0;
        else
            limit = s.real_vram_size - offset;

        if (((s.gr[0x0b] & 0x01) == 0) && (bank_index != 0)) {
            if (limit > 0x8000) {
                offset += 0x8000;
                limit -= 0x8000;
            } else {
                limit = 0;
            }
        }

        if (limit > 0) {
            s.cirrus_bank_base[bank_index] = offset;
            s.cirrus_bank_limit[bank_index] = limit;
        } else {
            s.cirrus_bank_base[bank_index] = 0;
            s.cirrus_bank_limit[bank_index] = 0;
        }
    }

    /***************************************
     *
     *  I/O access between 0x3c4-0x3c5
     *
     ***************************************/

    static private int cirrus_vga_read_sr(CirrusVGAState s) {
        switch (s.sr_index) {
            case 0x00:            // Standard VGA
            case 0x01:            // Standard VGA
            case 0x02:            // Standard VGA
            case 0x03:            // Standard VGA
            case 0x04:            // Standard VGA
                return s.sr[s.sr_index];
            case 0x06:            // Unlock Cirrus extensions
                return s.sr[s.sr_index];
            case 0x10:
            case 0x30:
            case 0x50:
            case 0x70:            // Graphics Cursor X
            case 0x90:
            case 0xb0:
            case 0xd0:
            case 0xf0:            // Graphics Cursor X
                return s.sr[0x10];
            case 0x11:
            case 0x31:
            case 0x51:
            case 0x71:            // Graphics Cursor Y
            case 0x91:
            case 0xb1:
            case 0xd1:
            case 0xf1:            // Graphics Cursor Y
                return s.sr[0x11];
            case 0x05:            // ???
            case 0x07:            // Extended Sequencer Mode
            case 0x08:            // EEPROM Control
            case 0x09:            // Scratch Register 0
            case 0x0a:            // Scratch Register 1
            case 0x0b:            // VCLK 0
            case 0x0c:            // VCLK 1
            case 0x0d:            // VCLK 2
            case 0x0e:            // VCLK 3
            case 0x0f:            // DRAM Control
            case 0x12:            // Graphics Cursor Attribute
            case 0x13:            // Graphics Cursor Pattern Address
            case 0x14:            // Scratch Register 2
            case 0x15:            // Scratch Register 3
            case 0x16:            // Performance Tuning Register
            case 0x17:            // Configuration Readback and Extended Control
            case 0x18:            // Signature Generator Control
            case 0x19:            // Signal Generator Result
            case 0x1a:            // Signal Generator Result
            case 0x1b:            // VCLK 0 Denominator & Post
            case 0x1c:            // VCLK 1 Denominator & Post
            case 0x1d:            // VCLK 2 Denominator & Post
            case 0x1e:            // VCLK 3 Denominator & Post
            case 0x1f:            // BIOS Write Enable and MCLK select
                if (DEBUG)
                    System.out.println("cirrus: handled inport sr_index " + Integer.toHexString(s.sr_index));
                return s.sr[s.sr_index];
            default:
                if (DEBUG)
                    System.out.println("cirrus: inport sr_index " + Integer.toHexString(s.sr_index));
                return 0xff;
        }
    }

    static private void cirrus_vga_write_sr(CirrusVGAState s, int val) {
        switch (s.sr_index) {
            case 0x00:            // Standard VGA
            case 0x01:            // Standard VGA
            case 0x02:            // Standard VGA
            case 0x03:            // Standard VGA
            case 0x04:            // Standard VGA
                s.sr[s.sr_index] = val & sr_mask[s.sr_index];
                if (s.sr_index == 1)
                    s.update_retrace_info.call(s);
                break;
            case 0x06:            // Unlock Cirrus extensions
                val &= 0x17;
                if (val == 0x12) {
                    s.sr[s.sr_index] = 0x12;
                } else {
                    s.sr[s.sr_index] = 0x0f;
                }
                break;
            case 0x10:
            case 0x30:
            case 0x50:
            case 0x70:            // Graphics Cursor X
            case 0x90:
            case 0xb0:
            case 0xd0:
            case 0xf0:            // Graphics Cursor X
                s.sr[0x10] = val;
                s.hw_cursor_x = (val << 3) | (s.sr_index >> 5);
                break;
            case 0x11:
            case 0x31:
            case 0x51:
            case 0x71:            // Graphics Cursor Y
            case 0x91:
            case 0xb1:
            case 0xd1:
            case 0xf1:            // Graphics Cursor Y
                s.sr[0x11] = val;
                s.hw_cursor_y = (val << 3) | (s.sr_index >> 5);
                break;
            case 0x07:            // Extended Sequencer Mode
                cirrus_update_memory_access(s);
            case 0x08:            // EEPROM Control
            case 0x09:            // Scratch Register 0
            case 0x0a:            // Scratch Register 1
            case 0x0b:            // VCLK 0
            case 0x0c:            // VCLK 1
            case 0x0d:            // VCLK 2
            case 0x0e:            // VCLK 3
            case 0x0f:            // DRAM Control
            case 0x12:            // Graphics Cursor Attribute
            case 0x13:            // Graphics Cursor Pattern Address
            case 0x14:            // Scratch Register 2
            case 0x15:            // Scratch Register 3
            case 0x16:            // Performance Tuning Register
            case 0x18:            // Signature Generator Control
            case 0x19:            // Signature Generator Result
            case 0x1a:            // Signature Generator Result
            case 0x1b:            // VCLK 0 Denominator & Post
            case 0x1c:            // VCLK 1 Denominator & Post
            case 0x1d:            // VCLK 2 Denominator & Post
            case 0x1e:            // VCLK 3 Denominator & Post
            case 0x1f:            // BIOS Write Enable and MCLK select
                s.sr[s.sr_index] = val;
                if (DEBUG)
                    System.out.println("cirrus: handled outport sr_index " + Integer.toHexString(s.sr_index) + ", sr_value " + Integer.toHexString(val));
                break;
            case 0x17:            // Configuration Readback and Extended Control
                s.sr[s.sr_index] = (s.sr[s.sr_index] & 0x38) | (val & 0xc7);
                cirrus_update_memory_access(s);
                break;
            default:
                if (DEBUG)
                    System.out.println("cirrus: outport sr_index " + Integer.toHexString(s.sr_index) + ", sr_value " + Integer.toHexString(val));
                break;
        }
    }

    /***************************************
     *
     *  I/O access at 0x3c6
     *
     ***************************************/

    static private int cirrus_read_hidden_dac(CirrusVGAState s) {
        if (++s.cirrus_hidden_dac_lockindex == 5) {
            s.cirrus_hidden_dac_lockindex = 0;
            return s.cirrus_hidden_dac_data;
        }
        return 0xff;
    }

    static private void cirrus_write_hidden_dac(CirrusVGAState s, int reg_value) {
        if (s.cirrus_hidden_dac_lockindex == 4) {
            s.cirrus_hidden_dac_data = reg_value;
            if (DEBUG)
                System.out.println("cirrus: outport hidden DAC, value " + Integer.toHexString(reg_value));
        }
        s.cirrus_hidden_dac_lockindex = 0;
    }

    /***************************************
     *
     *  I/O access at 0x3c9
     *
     ***************************************/

    static private int cirrus_vga_read_palette(CirrusVGAState s) {
        int val;

        if ((s.sr[0x12] & CIRRUS_CURSOR_HIDDENPEL) != 0) {
            val = s.cirrus_hidden_palette[(s.dac_read_index & 0x0f) * 3 +
                    s.dac_sub_index];
        } else {
            val = s.palette[s.dac_read_index * 3 + s.dac_sub_index];
        }
        if (++s.dac_sub_index == 3) {
            s.dac_sub_index = 0;
            s.dac_read_index++;
        }
        return val;
    }

    static private void cirrus_vga_write_palette(CirrusVGAState s, int reg_value) {
        s.dac_cache[s.dac_sub_index] = reg_value;
        if (++s.dac_sub_index == 3) {
            if ((s.sr[0x12] & CIRRUS_CURSOR_HIDDENPEL) != 0) {
                System.arraycopy(s.dac_cache, 0, s.cirrus_hidden_palette, (s.dac_write_index & 0x0f) * 3, 3);
            } else {
                System.arraycopy(s.dac_cache, 0, s.palette, s.dac_write_index * 3, 3);
            }
            /* XXX update cursor */
            s.dac_sub_index = 0;
            s.dac_write_index++;
        }
    }

    /***************************************
     *
     *  I/O access between 0x3ce-0x3cf
     *
     ***************************************/

    static private int cirrus_vga_read_gr(CirrusVGAState s, int reg_index) {
        switch (reg_index) {
            case 0x00: // Standard VGA, BGCOLOR 0x000000ff
                return s.cirrus_shadow_gr0;
            case 0x01: // Standard VGA, FGCOLOR 0x000000ff
                return s.cirrus_shadow_gr1;
            case 0x02:            // Standard VGA
            case 0x03:            // Standard VGA
            case 0x04:            // Standard VGA
            case 0x06:            // Standard VGA
            case 0x07:            // Standard VGA
            case 0x08:            // Standard VGA
                return s.gr[s.gr_index];
            case 0x05:            // Standard VGA, Cirrus extended mode
            default:
                break;
        }

        if (reg_index < 0x3a) {
            return s.gr[reg_index];
        } else {
            if (DEBUG)
                System.out.println("cirrus: inport gr_index " + Integer.toHexString(reg_index));
            return 0xff;
        }
    }

    static private void cirrus_vga_write_gr(CirrusVGAState s, int reg_index, int reg_value) {
        switch (reg_index) {
            case 0x00:            // Standard VGA, BGCOLOR 0x000000ff
                s.gr[reg_index] = reg_value & gr_mask[reg_index];
                s.cirrus_shadow_gr0 = reg_value;
                break;
            case 0x01:            // Standard VGA, FGCOLOR 0x000000ff
                s.gr[reg_index] = reg_value & gr_mask[reg_index];
                s.cirrus_shadow_gr1 = reg_value;
                break;
            case 0x02:            // Standard VGA
            case 0x03:            // Standard VGA
            case 0x04:            // Standard VGA
            case 0x06:            // Standard VGA
            case 0x07:            // Standard VGA
            case 0x08:            // Standard VGA
                s.gr[reg_index] = reg_value & gr_mask[reg_index];
                break;
            case 0x05:            // Standard VGA, Cirrus extended mode
                s.gr[reg_index] = reg_value & 0x7f;
                cirrus_update_memory_access(s);
                break;
            case 0x09:            // bank offset #0
            case 0x0A:            // bank offset #1
                s.gr[reg_index] = reg_value;
                cirrus_update_bank_ptr(s, 0);
                cirrus_update_bank_ptr(s, 1);
                cirrus_update_memory_access(s);
                break;
            case 0x0B:
                s.gr[reg_index] = reg_value;
                cirrus_update_bank_ptr(s, 0);
                cirrus_update_bank_ptr(s, 1);
                cirrus_update_memory_access(s);
                break;
            case 0x10:            // BGCOLOR 0x0000ff00
            case 0x11:            // FGCOLOR 0x0000ff00
            case 0x12:            // BGCOLOR 0x00ff0000
            case 0x13:            // FGCOLOR 0x00ff0000
            case 0x14:            // BGCOLOR 0xff000000
            case 0x15:            // FGCOLOR 0xff000000
            case 0x20:            // BLT WIDTH 0x0000ff
            case 0x22:            // BLT HEIGHT 0x0000ff
            case 0x24:            // BLT DEST PITCH 0x0000ff
            case 0x26:            // BLT SRC PITCH 0x0000ff
            case 0x28:            // BLT DEST ADDR 0x0000ff
            case 0x29:            // BLT DEST ADDR 0x00ff00
            case 0x2c:            // BLT SRC ADDR 0x0000ff
            case 0x2d:            // BLT SRC ADDR 0x00ff00
            case 0x2f:                  // BLT WRITEMASK
            case 0x30:            // BLT MODE
            case 0x32:            // RASTER OP
            case 0x33:            // BLT MODEEXT
            case 0x34:            // BLT TRANSPARENT COLOR 0x00ff
            case 0x35:            // BLT TRANSPARENT COLOR 0xff00
            case 0x38:            // BLT TRANSPARENT COLOR MASK 0x00ff
            case 0x39:            // BLT TRANSPARENT COLOR MASK 0xff00
                s.gr[reg_index] = reg_value;
                break;
            case 0x21:            // BLT WIDTH 0x001f00
            case 0x23:            // BLT HEIGHT 0x001f00
            case 0x25:            // BLT DEST PITCH 0x001f00
            case 0x27:            // BLT SRC PITCH 0x001f00
                s.gr[reg_index] = reg_value & 0x1f;
                break;
            case 0x2a:            // BLT DEST ADDR 0x3f0000
                s.gr[reg_index] = reg_value & 0x3f;
            /* if auto start mode, starts bit blt now */
                if ((s.gr[0x31] & CIRRUS_BLT_AUTOSTART) != 0) {
                    cirrus_bitblt_start(s);
                }
                break;
            case 0x2e:            // BLT SRC ADDR 0x3f0000
                s.gr[reg_index] = reg_value & 0x3f;
                break;
            case 0x31:            // BLT STATUS/START
                cirrus_write_bitblt(s, reg_value);
                break;
            default:
                if (DEBUG)
                    System.out.println("cirrus: outport gr_index " + Integer.toHexString(reg_index) + ", gr_value " + Integer.toHexString(reg_value));
                break;
        }
    }

    /***************************************
     *
     *  I/O access between 0x3d4-0x3d5
     *
     ***************************************/

    static private int cirrus_vga_read_cr(CirrusVGAState s, int reg_index) {
        switch (reg_index) {
            case 0x00:            // Standard VGA
            case 0x01:            // Standard VGA
            case 0x02:            // Standard VGA
            case 0x03:            // Standard VGA
            case 0x04:            // Standard VGA
            case 0x05:            // Standard VGA
            case 0x06:            // Standard VGA
            case 0x07:            // Standard VGA
            case 0x08:            // Standard VGA
            case 0x09:            // Standard VGA
            case 0x0a:            // Standard VGA
            case 0x0b:            // Standard VGA
            case 0x0c:            // Standard VGA
            case 0x0d:            // Standard VGA
            case 0x0e:            // Standard VGA
            case 0x0f:            // Standard VGA
            case 0x10:            // Standard VGA
            case 0x11:            // Standard VGA
            case 0x12:            // Standard VGA
            case 0x13:            // Standard VGA
            case 0x14:            // Standard VGA
            case 0x15:            // Standard VGA
            case 0x16:            // Standard VGA
            case 0x17:            // Standard VGA
            case 0x18:            // Standard VGA
                return s.cr[s.cr_index];
            case 0x24:            // Attribute Controller Toggle Readback (R)
                return (s.ar_flip_flop << 7);
            case 0x19:            // Interlace End
            case 0x1a:            // Miscellaneous Control
            case 0x1b:            // Extended Display Control
            case 0x1c:            // Sync Adjust and Genlock
            case 0x1d:            // Overlay Extended Control
            case 0x22:            // Graphics Data Latches Readback (R)
            case 0x25:            // Part Status
            case 0x27:            // Part ID (R)
                return s.cr[s.cr_index];
            case 0x26:            // Attribute Controller Index Readback (R)
                return s.ar_index & 0x3f;
            default:
                if (DEBUG)
                    System.out.println("cirrus: inport cr_index " + Integer.toHexString(reg_index));
                return 0xff;
        }
    }

    static private void cirrus_vga_write_cr(CirrusVGAState s, int reg_value) {
        switch (s.cr_index) {
            case 0x00:            // Standard VGA
            case 0x01:            // Standard VGA
            case 0x02:            // Standard VGA
            case 0x03:            // Standard VGA
            case 0x04:            // Standard VGA
            case 0x05:            // Standard VGA
            case 0x06:            // Standard VGA
            case 0x07:            // Standard VGA
            case 0x08:            // Standard VGA
            case 0x09:            // Standard VGA
            case 0x0a:            // Standard VGA
            case 0x0b:            // Standard VGA
            case 0x0c:            // Standard VGA
            case 0x0d:            // Standard VGA
            case 0x0e:            // Standard VGA
            case 0x0f:            // Standard VGA
            case 0x10:            // Standard VGA
            case 0x11:            // Standard VGA
            case 0x12:            // Standard VGA
            case 0x13:            // Standard VGA
            case 0x14:            // Standard VGA
            case 0x15:            // Standard VGA
            case 0x16:            // Standard VGA
            case 0x17:            // Standard VGA
            case 0x18:            // Standard VGA
    	/* handle CR0-7 protection */
                if ((s.cr[0x11] & 0x80) != 0 && s.cr_index <= 7) {
    	    /* can always write bit 4 of CR7 */
                    if (s.cr_index == 7)
                        s.cr[7] = (s.cr[7] & ~0x10) | (reg_value & 0x10);
                    return;
                }
                s.cr[s.cr_index] = reg_value;
                switch (s.cr_index) {
                    case 0x00:
                    case 0x04:
                    case 0x05:
                    case 0x06:
                    case 0x07:
                    case 0x11:
                    case 0x17:
                        s.update_retrace_info.call(s);
                        break;
                }
                break;
            case 0x19:            // Interlace End
            case 0x1a:            // Miscellaneous Control
            case 0x1b:            // Extended Display Control
            case 0x1c:            // Sync Adjust and Genlock
            case 0x1d:            // Overlay Extended Control
                s.cr[s.cr_index] = reg_value;
                if (DEBUG)
                    System.out.println("cirrus: handled outport cr_index " + Integer.toHexString(s.cr_index) + ", cr_value " + Integer.toHexString(reg_value));
                break;
            case 0x22:            // Graphics Data Latches Readback (R)
            case 0x24:            // Attribute Controller Toggle Readback (R)
            case 0x26:            // Attribute Controller Index Readback (R)
            case 0x27:            // Part ID (R)
                break;
            case 0x25:            // Part Status
            default:
                if (DEBUG)
                    System.out.println("cirrus: outport cr_index " + Integer.toHexString(s.cr_index) + ", cr_value " + Integer.toHexString(reg_value));
                break;
        }
    }

    /***************************************
     *
     *  memory-mapped I/O (bitblt)
     *
     ***************************************/

    static private int cirrus_mmio_blt_read(CirrusVGAState s, int address) {
        int value = 0xff;

        switch (address) {
            case (CIRRUS_MMIO_BLTBGCOLOR + 0):
                value = cirrus_vga_read_gr(s, 0x00);
                break;
            case (CIRRUS_MMIO_BLTBGCOLOR + 1):
                value = cirrus_vga_read_gr(s, 0x10);
                break;
            case (CIRRUS_MMIO_BLTBGCOLOR + 2):
                value = cirrus_vga_read_gr(s, 0x12);
                break;
            case (CIRRUS_MMIO_BLTBGCOLOR + 3):
                value = cirrus_vga_read_gr(s, 0x14);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 0):
                value = cirrus_vga_read_gr(s, 0x01);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 1):
                value = cirrus_vga_read_gr(s, 0x11);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 2):
                value = cirrus_vga_read_gr(s, 0x13);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 3):
                value = cirrus_vga_read_gr(s, 0x15);
                break;
            case (CIRRUS_MMIO_BLTWIDTH + 0):
                value = cirrus_vga_read_gr(s, 0x20);
                break;
            case (CIRRUS_MMIO_BLTWIDTH + 1):
                value = cirrus_vga_read_gr(s, 0x21);
                break;
            case (CIRRUS_MMIO_BLTHEIGHT + 0):
                value = cirrus_vga_read_gr(s, 0x22);
                break;
            case (CIRRUS_MMIO_BLTHEIGHT + 1):
                value = cirrus_vga_read_gr(s, 0x23);
                break;
            case (CIRRUS_MMIO_BLTDESTPITCH + 0):
                value = cirrus_vga_read_gr(s, 0x24);
                break;
            case (CIRRUS_MMIO_BLTDESTPITCH + 1):
                value = cirrus_vga_read_gr(s, 0x25);
                break;
            case (CIRRUS_MMIO_BLTSRCPITCH + 0):
                value = cirrus_vga_read_gr(s, 0x26);
                break;
            case (CIRRUS_MMIO_BLTSRCPITCH + 1):
                value = cirrus_vga_read_gr(s, 0x27);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 0):
                value = cirrus_vga_read_gr(s, 0x28);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 1):
                value = cirrus_vga_read_gr(s, 0x29);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 2):
                value = cirrus_vga_read_gr(s, 0x2a);
                break;
            case (CIRRUS_MMIO_BLTSRCADDR + 0):
                value = cirrus_vga_read_gr(s, 0x2c);
                break;
            case (CIRRUS_MMIO_BLTSRCADDR + 1):
                value = cirrus_vga_read_gr(s, 0x2d);
                break;
            case (CIRRUS_MMIO_BLTSRCADDR + 2):
                value = cirrus_vga_read_gr(s, 0x2e);
                break;
            case CIRRUS_MMIO_BLTWRITEMASK:
                value = cirrus_vga_read_gr(s, 0x2f);
                break;
            case CIRRUS_MMIO_BLTMODE:
                value = cirrus_vga_read_gr(s, 0x30);
                break;
            case CIRRUS_MMIO_BLTROP:
                value = cirrus_vga_read_gr(s, 0x32);
                break;
            case CIRRUS_MMIO_BLTMODEEXT:
                value = cirrus_vga_read_gr(s, 0x33);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLOR + 0):
                value = cirrus_vga_read_gr(s, 0x34);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLOR + 1):
                value = cirrus_vga_read_gr(s, 0x35);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLORMASK + 0):
                value = cirrus_vga_read_gr(s, 0x38);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLORMASK + 1):
                value = cirrus_vga_read_gr(s, 0x39);
                break;
            case CIRRUS_MMIO_BLTSTATUS:
                value = cirrus_vga_read_gr(s, 0x31);
                break;
            default:
                if (DEBUG)
                    System.out.println("cirrus: mmio read - address 0x" + Integer.toHexString(address));
                break;
        }

        return value;
    }

    static void cirrus_mmio_blt_write(CirrusVGAState s, int address, int value) {
        switch (address) {
            case (CIRRUS_MMIO_BLTBGCOLOR + 0):
                cirrus_vga_write_gr(s, 0x00, value);
                break;
            case (CIRRUS_MMIO_BLTBGCOLOR + 1):
                cirrus_vga_write_gr(s, 0x10, value);
                break;
            case (CIRRUS_MMIO_BLTBGCOLOR + 2):
                cirrus_vga_write_gr(s, 0x12, value);
                break;
            case (CIRRUS_MMIO_BLTBGCOLOR + 3):
                cirrus_vga_write_gr(s, 0x14, value);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 0):
                cirrus_vga_write_gr(s, 0x01, value);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 1):
                cirrus_vga_write_gr(s, 0x11, value);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 2):
                cirrus_vga_write_gr(s, 0x13, value);
                break;
            case (CIRRUS_MMIO_BLTFGCOLOR + 3):
                cirrus_vga_write_gr(s, 0x15, value);
                break;
            case (CIRRUS_MMIO_BLTWIDTH + 0):
                cirrus_vga_write_gr(s, 0x20, value);
                break;
            case (CIRRUS_MMIO_BLTWIDTH + 1):
                cirrus_vga_write_gr(s, 0x21, value);
                break;
            case (CIRRUS_MMIO_BLTHEIGHT + 0):
                cirrus_vga_write_gr(s, 0x22, value);
                break;
            case (CIRRUS_MMIO_BLTHEIGHT + 1):
                cirrus_vga_write_gr(s, 0x23, value);
                break;
            case (CIRRUS_MMIO_BLTDESTPITCH + 0):
                cirrus_vga_write_gr(s, 0x24, value);
                break;
            case (CIRRUS_MMIO_BLTDESTPITCH + 1):
                cirrus_vga_write_gr(s, 0x25, value);
                break;
            case (CIRRUS_MMIO_BLTSRCPITCH + 0):
                cirrus_vga_write_gr(s, 0x26, value);
                break;
            case (CIRRUS_MMIO_BLTSRCPITCH + 1):
                cirrus_vga_write_gr(s, 0x27, value);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 0):
                cirrus_vga_write_gr(s, 0x28, value);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 1):
                cirrus_vga_write_gr(s, 0x29, value);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 2):
                cirrus_vga_write_gr(s, 0x2a, value);
                break;
            case (CIRRUS_MMIO_BLTDESTADDR + 3):
    	/* ignored */
                break;
            case (CIRRUS_MMIO_BLTSRCADDR + 0):
                cirrus_vga_write_gr(s, 0x2c, value);
                break;
            case (CIRRUS_MMIO_BLTSRCADDR + 1):
                cirrus_vga_write_gr(s, 0x2d, value);
                break;
            case (CIRRUS_MMIO_BLTSRCADDR + 2):
                cirrus_vga_write_gr(s, 0x2e, value);
                break;
            case CIRRUS_MMIO_BLTWRITEMASK:
                cirrus_vga_write_gr(s, 0x2f, value);
                break;
            case CIRRUS_MMIO_BLTMODE:
                cirrus_vga_write_gr(s, 0x30, value);
                break;
            case CIRRUS_MMIO_BLTROP:
                cirrus_vga_write_gr(s, 0x32, value);
                break;
            case CIRRUS_MMIO_BLTMODEEXT:
                cirrus_vga_write_gr(s, 0x33, value);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLOR + 0):
                cirrus_vga_write_gr(s, 0x34, value);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLOR + 1):
                cirrus_vga_write_gr(s, 0x35, value);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLORMASK + 0):
                cirrus_vga_write_gr(s, 0x38, value);
                break;
            case (CIRRUS_MMIO_BLTTRANSPARENTCOLORMASK + 1):
                cirrus_vga_write_gr(s, 0x39, value);
                break;
            case CIRRUS_MMIO_BLTSTATUS:
                cirrus_vga_write_gr(s, 0x31, value);
                break;
            default:
                if (DEBUG)
                    System.out.println("cirrus: mmio write - addr 0x" + Integer.toHexString(address) + " val 0x" + Integer.toHexString(value) + " (ignored)");
                break;
        }
    }

    /***************************************
     *
     *  write mode 4/5
     *
     ***************************************/

    static void cirrus_mem_writeb_mode4and5_8bpp(CirrusVGAState s, int mode, int offset, int mem_value) {
        int x;
        int val = mem_value;
        int dstPos;

        dstPos = s.vram_ptr + (offset &= s.cirrus_addr_mask);
        for (x = 0; x < 8; x++) {
            if ((val & 0x80) != 0) {
                s.writeb(dstPos, s.cirrus_shadow_gr1);
            } else if (mode == 5) {
                s.writeb(dstPos, s.cirrus_shadow_gr0);
            }
            val <<= 1;
            dstPos++;
        }
        //memory_region_set_dirty(&s.vram, offset, 8);
    }

    static void cirrus_mem_writeb_mode4and5_16bpp(CirrusVGAState s, int mode, int offset, int mem_value) {
        int x;
        int val = mem_value;
        int dstPos;

        dstPos = s.vram_ptr + (offset &= s.cirrus_addr_mask);
        for (x = 0; x < 8; x++) {
            if ((val & 0x80) != 0) {
                s.writeb(dstPos, s.cirrus_shadow_gr1);
                s.writeb(dstPos + 1, s.gr[0x11]);
            } else if (mode == 5) {
                s.writeb(dstPos, s.cirrus_shadow_gr0);
                s.writeb(dstPos + 1, s.gr[0x10]);
            }
            val <<= 1;
            dstPos += 2;
        }
        //memory_region_set_dirty(&s.vram, offset, 16);
    }

    /***************************************
     *
     *  memory access between 0xa0000-0xbffff
     *
     ***************************************/
    static private class cirrus_vga_mem extends Paging.PageHandler {
        public cirrus_vga_mem() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            CirrusVGAState s = cirrusVGAState;
            int bank_index;
            int bank_offset;
            int mode;

            if ((s.sr[0x07] & 0x01) == 0) {
                s.writeb(addr, val);
                return;
            }

            if (addr < 0x10000) {
                if (s.cirrus_srcptr != s.cirrus_srcptr_end) {
                /* bitblt */
                    s.writeb(s.cirrus_srcptr++, val);
                    if (s.cirrus_srcptr >= s.cirrus_srcptr_end) {
                        cirrus_bitblt_cputovideo_next(s);
                    }
                } else {
                /* video memory */
                    bank_index = addr >> 15;
                    bank_offset = addr & 0x7fff;
                    if (bank_offset < s.cirrus_bank_limit[bank_index]) {
                        bank_offset += s.cirrus_bank_base[bank_index];
                        if ((s.gr[0x0B] & 0x14) == 0x14) {
                            bank_offset <<= 4;
                        } else if ((s.gr[0x0B] & 0x02) != 0) {
                            bank_offset <<= 3;
                        }
                        bank_offset &= s.cirrus_addr_mask;
                        mode = s.gr[0x05] & 0x7;
                        if (mode < 4 || mode > 5 || ((s.gr[0x0B] & 0x4) == 0)) {
                            s.writeb(s.vram_ptr + bank_offset, val);
                            // memory_region_set_dirty(&s.vram, bank_offset, sizeof(mem_value));
                        } else {
                            if ((s.gr[0x0B] & 0x14) != 0x14) {
                                cirrus_mem_writeb_mode4and5_8bpp(s, mode, bank_offset, val);
                            } else {
                                cirrus_mem_writeb_mode4and5_16bpp(s, mode, bank_offset, val);
                            }
                        }
                    }
                }
            } else if (addr >= 0x18000 && addr < 0x18100) {
            /* memory-mapped I/O */
                if ((s.sr[0x17] & 0x44) == 0x04) {
                    cirrus_mmio_blt_write(s, addr & 0xff, val);
                }
            } else {
                if (DEBUG)
                    System.out.println("cirrus: mem_writeb " + Integer.toHexString(addr) + " value " + Integer.toHexString(val));
            }
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            CirrusVGAState s = cirrusVGAState;
            int bank_index;
            int bank_offset;
            int val;

            if ((s.sr[0x07] & 0x01) == 0) {
                return s.readb(addr);
            }

            if (addr < 0x10000) {
            /* XXX handle bitblt */
            /* video memory */
                bank_index = addr >> 15;
                bank_offset = addr & 0x7fff;
                if (bank_offset < s.cirrus_bank_limit[bank_index]) {
                    bank_offset += s.cirrus_bank_base[bank_index];
                    if ((s.gr[0x0B] & 0x14) == 0x14) {
                        bank_offset <<= 4;
                    } else if ((s.gr[0x0B] & 0x02) != 0) {
                        bank_offset <<= 3;
                    }
                    bank_offset &= s.cirrus_addr_mask;
                    val = s.readb(s.vram_ptr + bank_offset);
                } else
                    val = 0xff;
            } else if (addr >= 0x18000 && addr < 0x18100) {
            /* memory-mapped I/O */
                val = 0xff;
                if ((s.sr[0x17] & 0x44) == 0x04) {
                    val = cirrus_mmio_blt_read(s, addr & 0xff);
                }
            } else {
                val = 0xff;
                if (DEBUG)
                    System.out.println("cirrus: mem_readb " + Integer.toHexString(addr));
            }
            return val;
        }
    }

//    static const MemoryRegionOps cirrus_vga_mem_ops = {
//        .read = cirrus_vga_mem_read,
//        .write = cirrus_vga_mem_write,
//        .endianness = DEVICE_LITTLE_ENDIAN,
//        .impl = {
//            .min_access_size = 1,
//            .max_access_size = 1,
//        },
//    };

    /***************************************
     *
     *  hardware cursor
     *
     ***************************************/

    static private void invalidate_cursor1(CirrusVGAState s) {
//        if (s.last_hw_cursor_size) {
//            vga_invalidate_scanlines(&s.vga, s.last_hw_cursor_y + s.last_hw_cursor_y_start, s.last_hw_cursor_y + s.last_hw_cursor_y_end);
//        }
    }

    static private void cirrus_cursor_compute_yrange(CirrusVGAState s) {
        int src;
        int content;
        int y, y_min, y_max;

        src = s.vram_ptr + s.real_vram_size - 16 * 1024;
        if ((s.sr[0x12] & CIRRUS_CURSOR_LARGE) != 0) {
            src += (s.sr[0x13] & 0x3c) * 256;
            y_min = 64;
            y_max = -1;
            for (y = 0; y < 64; y++) {
                content = s.readd(src) | s.readd(src + 4) | s.readd(src + 8) | s.readd(src + 12);
                if (content != 0) {
                    if (y < y_min)
                        y_min = y;
                    if (y > y_max)
                        y_max = y;
                }
                src += 16;
            }
        } else {
            src += (s.sr[0x13] & 0x3f) * 256;
            y_min = 32;
            y_max = -1;
            for (y = 0; y < 32; y++) {
                content = s.readd(src) | s.readd(src + 128);
                if (content != 0) {
                    if (y < y_min)
                        y_min = y;
                    if (y > y_max)
                        y_max = y;
                }
                src += 4;
            }
        }
        if (y_min > y_max) {
            s.last_hw_cursor_y_start = 0;
            s.last_hw_cursor_y_end = 0;
        } else {
            s.last_hw_cursor_y_start = y_min;
            s.last_hw_cursor_y_end = y_max + 1;
        }
    }

    /* NOTE: we do not currently handle the cursor bitmap change, so we
       update the cursor only if it moves. */
    static private void cirrus_cursor_invalidate(CirrusVGAState s) {
        int size;

        if ((s.sr[0x12] & CIRRUS_CURSOR_SHOW) == 0) {
            size = 0;
        } else {
            if ((s.sr[0x12] & CIRRUS_CURSOR_LARGE) != 0)
                size = 64;
            else
                size = 32;
        }
        /* invalidate last cursor and new cursor if any change */
        if (s.last_hw_cursor_size != size ||
                s.last_hw_cursor_x != s.hw_cursor_x ||
                s.last_hw_cursor_y != s.hw_cursor_y) {

            invalidate_cursor1(s);

            s.last_hw_cursor_size = size;
            s.last_hw_cursor_x = s.hw_cursor_x;
            s.last_hw_cursor_y = s.hw_cursor_y;
            /* compute the real cursor min and max y */
            cirrus_cursor_compute_yrange(s);
            invalidate_cursor1(s);
        }
    }

    static private void vga_draw_cursor_line_8(CirrusVGAState s, int d1, int src1, int poffset, int w, int color0, int color1, int color_xor) {
        int plane0, plane1;
        int x, b0, b1;
        int d;

        d = d1;
        plane0 = src1;
        plane1 = src1 + poffset;
        for (x = 0; x < w; x++) {
            b0 = (s.readb(plane0 + (x >> 3)) >> (7 - (x & 7))) & 1;
            b1 = (s.readb(plane1 + (x >> 3)) >> (7 - (x & 7))) & 1;
            switch (b0 | (b1 << 1)) {
                case 0:
                    break;
                case 1:
                    s.writeb(d, s.readb(d) ^ color_xor);
                    break;
                case 2:
                    s.writeb(d, color0);
                    break;
                case 3:
                    s.writeb(d, color1);
                    break;
            }
            d++;
        }
    }

    static private void vga_draw_cursor_line_16(CirrusVGAState s, int d1, int src1, int poffset, int w, int color0, int color1, int color_xor) {
        int plane0, plane1;
        int x, b0, b1;
        int d;

        d = d1;
        plane0 = src1;
        plane1 = src1 + poffset;
        for (x = 0; x < w; x++) {
            b0 = (s.readb(plane0 + (x >> 3)) >> (7 - (x & 7))) & 1;
            b1 = (s.readb(plane1 + (x >> 3)) >> (7 - (x & 7))) & 1;
            switch (b0 | (b1 << 1)) {
                case 0:
                    break;
                case 1:
                    s.writew(d, s.readw(d) ^ color_xor);
                    break;
                case 2:
                    s.writew(d, color0);
                    break;
                case 3:
                    s.writew(d, color1);
                    break;
            }
            d += 2;
        }
    }

    static private void vga_draw_cursor_line_32(CirrusVGAState s, int d1, int src1, int poffset, int w, int color0, int color1, int color_xor) {
        int plane0, plane1;
        int x, b0, b1;
        int d;

        d = d1;
        plane0 = src1;
        plane1 = src1 + poffset;
        for (x = 0; x < w; x++) {
            b0 = (s.readb(plane0 + (x >> 3)) >> (7 - (x & 7))) & 1;
            b1 = (s.readb(plane1 + (x >> 3)) >> (7 - (x & 7))) & 1;
            switch (b0 | (b1 << 1)) {
                case 0:
                    break;
                case 1:
                    s.writed(d, s.readd(d) ^ color_xor);
                    break;
                case 2:
                    s.writed(d, color0);
                    break;
                case 3:
                    s.writed(d, color1);
                    break;
            }
            d += 4;
        }
    }

    static final private class cirrus_cursor_draw_line implements VGACommonState.cursor_draw_line_func {
        public void call(VGACommonState c, int d1, int scr_y) {
            CirrusVGAState s = (CirrusVGAState) c;
            int w, h, bpp, x1, x2, poffset;
            int color0, color1;
            int[] palette;
            int src;
            int content;

            if ((s.sr[0x12] & CIRRUS_CURSOR_SHOW) == 0)
                return;
            /* fast test to see if the cursor intersects with the scan line */
            if ((s.sr[0x12] & CIRRUS_CURSOR_LARGE) != 0) {
                h = 64;
            } else {
                h = 32;
            }
            if (scr_y < s.hw_cursor_y ||
                    scr_y >= (s.hw_cursor_y + h))
                return;

            src = s.vram_ptr + s.real_vram_size - 16 * 1024;
            if ((s.sr[0x12] & CIRRUS_CURSOR_LARGE) != 0) {
                src += (s.sr[0x13] & 0x3c) * 256;
                src += (scr_y - s.hw_cursor_y) * 16;
                poffset = 8;
                content = s.readd(src) | s.readd(src + 4) | s.readd(src + 8) | s.readd(src + 12);
            } else {
                src += (s.sr[0x13] & 0x3f) * 256;
                src += (scr_y - s.hw_cursor_y) * 4;
                poffset = 128;
                content = s.readd(src) | s.readd(src + 128);
            }
            /* if nothing to draw, no need to continue */
            if (content == 0)
                return;
            w = h;

            x1 = s.hw_cursor_x;
            if (x1 >= s.last_scr_width)
                return;
            x2 = s.hw_cursor_x + w;
            if (x2 > s.last_scr_width)
                x2 = s.last_scr_width;
            w = x2 - x1;
            palette = s.cirrus_hidden_palette;
            color0 = s.rgb_to_pixel.call(c6_to_8(palette[0x0 * 3]), c6_to_8(palette[0x0 * 3 + 1]), c6_to_8(palette[0x0 * 3 + 2]));
            color1 = s.rgb_to_pixel.call(c6_to_8(palette[0xf * 3]), c6_to_8(palette[0xf * 3 + 1]), c6_to_8(palette[0xf * 3 + 2]));
            bpp = ((s.ds.ds_get_bits_per_pixel() + 7) >> 3);
            d1 += x1 * bpp;
            switch (s.ds.ds_get_bits_per_pixel()) {
                default:
                    break;
                case 8:
                    vga_draw_cursor_line_8(s, d1, src, poffset, w, color0, color1, 0xff);
                    break;
                case 15:
                    vga_draw_cursor_line_16(s, d1, src, poffset, w, color0, color1, 0x7fff);
                    break;
                case 16:
                    vga_draw_cursor_line_16(s, d1, src, poffset, w, color0, color1, 0xffff);
                    break;
                case 32:
                    vga_draw_cursor_line_32(s, d1, src, poffset, w, color0, color1, 0xffffff);
                    break;
            }
        }
    }

    /***************************************
     *
     *  LFB memory access
     *
     ***************************************/

    static private class cirrus_linear extends Paging.PageHandler {
        public cirrus_linear() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            CirrusVGAState s = cirrusVGAState;
            int mode;

            addr &= s.cirrus_addr_mask;

            if (((s.sr[0x17] & 0x44) == 0x44) && ((addr & s.linear_mmio_mask) == s.linear_mmio_mask)) {
            /* memory-mapped I/O */
                cirrus_mmio_blt_write(s, addr & 0xff, val);
            } else if (s.cirrus_srcptr != s.cirrus_srcptr_end) {
            /* bitblt */
                s.writeb(s.cirrus_srcptr++, val);
                if (s.cirrus_srcptr >= s.cirrus_srcptr_end) {
                    cirrus_bitblt_cputovideo_next(s);
                }
            } else {
            /* video memory */
                if ((s.gr[0x0B] & 0x14) == 0x14) {
                    addr <<= 4;
                } else if ((s.gr[0x0B] & 0x02) != 0) {
                    addr <<= 3;
                }
                addr &= s.cirrus_addr_mask;

                mode = s.gr[0x05] & 0x7;
                if (mode < 4 || mode > 5 || ((s.gr[0x0B] & 0x4) == 0)) {
                    s.writeb(addr, val);
                    //memory_region_set_dirty( & s.vram, addr, 1);
                } else {
                    if ((s.gr[0x0B] & 0x14) != 0x14) {
                        cirrus_mem_writeb_mode4and5_8bpp(s, mode, addr, val);
                    } else {
                        cirrus_mem_writeb_mode4and5_16bpp(s, mode, addr, val);
                    }
                }
            }
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            CirrusVGAState s = cirrusVGAState;
            int ret;

            addr &= s.cirrus_addr_mask;

            if (((s.sr[0x17] & 0x44) == 0x44) &&
                    ((addr & s.linear_mmio_mask) == s.linear_mmio_mask)) {
            /* memory-mapped I/O */
                ret = cirrus_mmio_blt_read(s, addr & 0xff);
            } else if (false) {
            /* XXX handle bitblt */
                ret = 0xff;
            } else {
            /* video memory */
                if ((s.gr[0x0B] & 0x14) == 0x14) {
                    addr <<= 4;
                } else if ((s.gr[0x0B] & 0x02) != 0) {
                    addr <<= 3;
                }
                addr &= s.cirrus_addr_mask;
                ret = s.readb(addr);
            }

            return ret;
        }
    }

    /***************************************
     *
     *  system to screen memory access
     *
     ***************************************/

    static private class cirrus_linear_bitblt extends Paging.PageHandler {
        public cirrus_linear_bitblt() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            CirrusVGAState s = cirrusVGAState;

            if (s.cirrus_srcptr != s.cirrus_srcptr_end) {
                /* bitblt */
                s.writeb(s.cirrus_srcptr++, val);
                if (s.cirrus_srcptr >= s.cirrus_srcptr_end) {
                    cirrus_bitblt_cputovideo_next(s);
                }
            }
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            return 0xff;
        }
    }

    static private void map_linear_vram_bank(CirrusVGAState s, int bank) {
        boolean enabled = !(s.cirrus_srcptr != s.cirrus_srcptr_end) && (s.sr[0x07] & 0x01) != 0 && (s.gr[0x0B] & 0x14) != 0x14 && (s.gr[0x0B] & 0x02) == 0;
        if (enabled) {
            Memory.MEM_SetPageHandler(VGA_PAGE_A0, 16, low_mem);
            Memory.MEM_SetPageHandler(VGA_PAGE_B0, 16, low_mem);
        } else {
            Memory.MEM_ResetPageHandler(VGA_PAGE_A0, 16);
            Memory.MEM_ResetPageHandler(VGA_PAGE_B0, 16);
        }
        //memory_region_set_alias_offset(mr, s.cirrus_bank_base[bank]);
    }

    static private void map_linear_vram(CirrusVGAState s) {
//        if (s.bustype == CIRRUS_BUSTYPE_PCI && !s.linear_vram) {
//            s.linear_vram = true;
//            memory_region_add_subregion_overlap(&s.pci_bar, 0, &s.vram, 1);
//        }

        map_linear_vram_bank(s, 0);
        map_linear_vram_bank(s, 1);
    }

    static private void unmap_linear_vram(CirrusVGAState s) {
//        if (s.bustype == CIRRUS_BUSTYPE_PCI && s.linear_vram) {
//            s.linear_vram = false;
//            memory_region_del_subregion(&s.pci_bar, &s.vram);
//
        Memory.MEM_ResetPageHandler(VGA_PAGE_A0, 16);
        Memory.MEM_ResetPageHandler(VGA_PAGE_B0, 16);
    }

    /* Compute the memory access functions */
    static private void cirrus_update_memory_access(CirrusVGAState s) {
        int mode;

//        memory_region_transaction_begin();
        if ((s.sr[0x17] & 0x44) == 0x44) {
            unmap_linear_vram(s);
        } else if (s.cirrus_srcptr != s.cirrus_srcptr_end) {
            unmap_linear_vram(s);
        } else {
            if ((s.gr[0x0B] & 0x14) == 0x14) {
                unmap_linear_vram(s);
            } else if ((s.gr[0x0B] & 0x02) != 0) {
                unmap_linear_vram(s);
            } else {
                mode = s.gr[0x05] & 0x7;
                if (mode < 4 || mode > 5 || ((s.gr[0x0B] & 0x4) == 0)) {
                    map_linear_vram(s);
                } else {
                    unmap_linear_vram(s);
                }
            }
        }
//        memory_region_transaction_commit();
    }


    /* I/O ports */
    public static IoHandler.IO_ReadHandler cirrus_vga_ioport_read = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int addr, /*Bitu*/int len) {
            CirrusVGAState s = cirrusVGAState;
            int val, index;

            if (VGA.vga_ioport_invalid(s, addr)) {
                val = 0xff;
            } else {
                switch (addr) {
                    case 0x3c0:
                        if (s.ar_flip_flop == 0) {
                            val = s.ar_index;
                        } else {
                            val = 0;
                        }
                        break;
                    case 0x3c1:
                        index = s.ar_index & 0x1f;
                        if (index < 21)
                            val = s.ar[index];
                        else
                            val = 0;
                        break;
                    case 0x3c2:
                        val = s.st00;
                        break;
                    case 0x3c4:
                        val = s.sr_index;
                        break;
                    case 0x3c5:
                        val = cirrus_vga_read_sr(s);
                        break;
                    case 0x3c6:
                        val = cirrus_read_hidden_dac(s);
                        break;
                    case 0x3c7:
                        val = s.dac_state;
                        break;
                    case 0x3c8:
                        val = s.dac_write_index;
                        s.cirrus_hidden_dac_lockindex = 0;
                        break;
                    case 0x3c9:
                        val = cirrus_vga_read_palette(s);
                        break;
                    case 0x3ca:
                        val = s.fcr;
                        break;
                    case 0x3cc:
                        val = s.msr;
                        break;
                    case 0x3ce:
                        val = s.gr_index;
                        break;
                    case 0x3cf:
                        val = cirrus_vga_read_gr(s, s.gr_index);
//        #ifdef DEBUG_VGA_REG
//                printf("vga: read GR%x = 0x%02x\n", s.gr_index, val);
//        #endif
                        break;
                    case 0x3b4:
                    case 0x3d4:
                        val = s.cr_index;
                        break;
                    case 0x3b5:
                    case 0x3d5:
                        val = cirrus_vga_read_cr(s, s.cr_index);
//        #ifdef DEBUG_VGA_REG
//                printf("vga: read CR%x = 0x%02x\n", s.cr_index, val);
//        #endif
                        break;
                    case 0x3ba:
                    case 0x3da:
                /* just toggle to fool polling */
                        val = s.st01 = s.retrace.call(s);
                        s.ar_flip_flop = 0;
                        break;
                    default:
                        val = 0x00;
                        break;
                }
            }
//        #if defined(DEBUG_VGA)
//            printf("VGA: read addr=0x%04x data=0x%02x\n", addr, val);
//        #endif
            return val;
        }
    };

    public static IoHandler.IO_WriteHandler cirrus_vga_ioport_write = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int addr, /*Bitu*/int val, /*Bitu*/int len) {
            CirrusVGAState s = cirrusVGAState;
            int index;

            /* check port range access depending on color/monochrome mode */
            if (VGA.vga_ioport_invalid(s, addr)) {
                return;
            }
//        #ifdef DEBUG_VGA
//            printf("VGA: write addr=0x%04x data=0x%02x\n", addr, val);
//        #endif

            switch (addr) {
                case 0x3c0:
                    if (s.ar_flip_flop == 0) {
                        val &= 0x3f;
                        s.ar_index = val;
                    } else {
                        index = s.ar_index & 0x1f;
                        switch (index) {
                            case 0x00:
                            case 0x01:
                            case 0x02:
                            case 0x03:
                            case 0x04:
                            case 0x05:
                            case 0x06:
                            case 0x07:
                            case 0x08:
                            case 0x09:
                            case 0x0a:
                            case 0x0b:
                            case 0x0c:
                            case 0x0d:
                            case 0x0e:
                            case 0x0f:
                                s.ar[index] = val & 0x3f;
                                break;
                            case 0x10:
                                s.ar[index] = val & ~0x10;
                                break;
                            case 0x11:
                                s.ar[index] = val;
                                break;
                            case 0x12:
                                s.ar[index] = val & ~0xc0;
                                break;
                            case 0x13:
                                s.ar[index] = val & ~0xf0;
                                break;
                            case 0x14:
                                s.ar[index] = val & ~0xf0;
                                break;
                            default:
                                break;
                        }
                    }
                    s.ar_flip_flop ^= 1;
                    break;
                case 0x3c2:
                    s.msr = val & ~0x10;
                    s.update_retrace_info.call(s);
                    break;
                case 0x3c4:
                    s.sr_index = val;
                    break;
                case 0x3c5:
//        #ifdef DEBUG_VGA_REG
//            printf("vga: write SR%x = 0x%02x\n", s.sr_index, val);
//        #endif
                    cirrus_vga_write_sr(s, val);
                    break;
                case 0x3c6:
                    cirrus_write_hidden_dac(s, val);
                    break;
                case 0x3c7:
                    s.dac_read_index = val;
                    s.dac_sub_index = 0;
                    s.dac_state = 3;
                    break;
                case 0x3c8:
                    s.dac_write_index = val;
                    s.dac_sub_index = 0;
                    s.dac_state = 0;
                    break;
                case 0x3c9:
                    cirrus_vga_write_palette(s, val);
                    break;
                case 0x3ce:
                    s.gr_index = val;
                    break;
                case 0x3cf:
//        #ifdef DEBUG_VGA_REG
//            printf("vga: write GR%x = 0x%02x\n", s.gr_index, val);
//        #endif
                    cirrus_vga_write_gr(s, s.gr_index, val);
                    break;
                case 0x3b4:
                case 0x3d4:
                    s.cr_index = val;
                    break;
                case 0x3b5:
                case 0x3d5:
//        #ifdef DEBUG_VGA_REG
//            printf("vga: write CR%x = 0x%02x\n", s.cr_index, val);
//        #endif
                    cirrus_vga_write_cr(s, val);
                    break;
                case 0x3ba:
                case 0x3da:
                    s.fcr = val & 0x10;
                    break;
            }
        }
    };

    /***************************************
     *
     *  memory-mapped I/O access
     *
     ***************************************/
    static private class cirrus_mmio extends Paging.PageHandler {
        public cirrus_mmio() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            if (addr >= 0x100) {
                cirrus_mmio_blt_write(cirrusVGAState, addr - 0x100, val);
            } else {
                cirrus_vga_ioport_write.call(addr + 0x3c0, val, 1);
            }
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            if (addr >= 0x100) {
                return cirrus_mmio_blt_read(cirrusVGAState, addr - 0x100);
            } else {
                return cirrus_vga_ioport_read.call(addr + 0x3c0, 1);
            }
        }
    }

    /***************************************
     *
     *  initialize
     *
     ***************************************/

    static private void cirrus_reset(CirrusVGAState s) {
        VGA.vga_common_reset(s);
        unmap_linear_vram(s);
        s.sr[0x06] = 0x0f;
        if (s.device_id == CIRRUS_ID_CLGD5446) {
            /* 4MB 64 bit memory config, always PCI */
            s.sr[0x1F] = 0x2d;        // MemClock
            s.gr[0x18] = 0x0f;             // fastest memory configuration
            s.sr[0x0f] = 0x98;
            s.sr[0x17] = 0x20;
            s.sr[0x15] = 0x04; /* memory size, 3=2MB, 4=4MB */
        } else {
            s.sr[0x1F] = 0x22;        // MemClock
            s.sr[0x0F] = CIRRUS_MEMSIZE_2M;
            s.sr[0x17] = s.bustype;
            /* memory size, 3=2MB, 4=4MB */
            if (jdos.hardware.VGA.vga.vmemsize == 2 * 1024 * 1024)
                s.sr[0x15] = 0x03;
            else if (jdos.hardware.VGA.vga.vmemsize == 2 * 1024 * 1024)
                s.sr[0x15] = 0x04;
            else
                Log.exit("Cirrus video card needs to have 2 or 4 MB of RAM");
        }
        s.cr[0x27] = s.device_id;

        s.cirrus_hidden_dac_lockindex = 5;
        s.cirrus_hidden_dac_data = 0;
    }

    static private boolean inited = false;
    static private Paging.PageHandler low_mem = new cirrus_vga_mem();

    static private void cirrus_init_common(CirrusVGAState s, int device_id, boolean is_pci) {
        int i;

        if (!inited) {
            inited = true;
            for (i = 0; i < 256; i++)
                rop_to_index[i] = CIRRUS_ROP_NOP_INDEX; /* nop rop */
            rop_to_index[CIRRUS_ROP_0] = 0;
            rop_to_index[CIRRUS_ROP_SRC_AND_DST] = 1;
            rop_to_index[CIRRUS_ROP_NOP] = 2;
            rop_to_index[CIRRUS_ROP_SRC_AND_NOTDST] = 3;
            rop_to_index[CIRRUS_ROP_NOTDST] = 4;
            rop_to_index[CIRRUS_ROP_SRC] = 5;
            rop_to_index[CIRRUS_ROP_1] = 6;
            rop_to_index[CIRRUS_ROP_NOTSRC_AND_DST] = 7;
            rop_to_index[CIRRUS_ROP_SRC_XOR_DST] = 8;
            rop_to_index[CIRRUS_ROP_SRC_OR_DST] = 9;
            rop_to_index[CIRRUS_ROP_NOTSRC_OR_NOTDST] = 10;
            rop_to_index[CIRRUS_ROP_SRC_NOTXOR_DST] = 11;
            rop_to_index[CIRRUS_ROP_SRC_OR_NOTDST] = 12;
            rop_to_index[CIRRUS_ROP_NOTSRC] = 13;
            rop_to_index[CIRRUS_ROP_NOTSRC_OR_DST] = 14;
            rop_to_index[CIRRUS_ROP_NOTSRC_AND_NOTDST] = 15;
            s.device_id = device_id;
            if (is_pci)
                s.bustype = CIRRUS_BUSTYPE_PCI;
            else
                s.bustype = CIRRUS_BUSTYPE_ISA;
        }

        for (i = 0; i < 16; i++) {
            IoHandler.IO_RegisterWriteHandler(0x3c0 + i, cirrus_vga_ioport_write, IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3c0 + i, cirrus_vga_ioport_read, IoHandler.IO_MB);
        }
        IoHandler.IO_RegisterWriteHandler(0x3b4, cirrus_vga_ioport_write, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3b5, cirrus_vga_ioport_write, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3d4, cirrus_vga_ioport_write, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3d5, cirrus_vga_ioport_write, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3ba, cirrus_vga_ioport_write, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3da, cirrus_vga_ioport_write, IoHandler.IO_MB);

        IoHandler.IO_RegisterReadHandler(0x3b4, cirrus_vga_ioport_read, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3b5, cirrus_vga_ioport_read, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3d4, cirrus_vga_ioport_read, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3d5, cirrus_vga_ioport_read, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3ba, cirrus_vga_ioport_read, IoHandler.IO_MB);
        IoHandler.IO_RegisterReadHandler(0x3da, cirrus_vga_ioport_read, IoHandler.IO_MB);

        s.real_vram_size = jdos.hardware.VGA.vga.vmemsize; // (s.device_id == CIRRUS_ID_CLGD5446) ? 4096 * 1024 : 2048 * 1024;

        /* XXX: s.vram_size must be a power of two */
        s.cirrus_addr_mask = s.real_vram_size - 1;
        s.linear_mmio_mask = s.real_vram_size - 256;

        s.get_bpp = new cirrus_get_bpp();
        //s.get_offsets = cirrus_get_offsets;
        s.get_resolutionCx = new cirrus_get_resolutionCx();
        s.get_resolutionCy = new cirrus_get_resolutionCy();
        // s.cursor_invalidate = cirrus_cursor_invalidate;
        s.cursor_draw_line = new cirrus_cursor_draw_line();

        cirrus_reset(s);
        //qemu_register_reset(cirrus_reset, s);
    }

    /***************************************
     *
     *  ISA bus support
     *
     ***************************************/

    static int vga_initfn() {
        VGACommonState s = cirrusVGAState;

        s.vram_size_mb = jdos.hardware.VGA.vga.vmemsize >> 20;
        VGA.vga_common_init(s);
        cirrus_init_common(cirrusVGAState, CIRRUS_ID_CLGD5430, false);
        //s.ds = graphic_console_init(s.update, s.invalidate, s.screen_dump, s.text_update, s);
        Qemu.rom_add_vga("vgabios-cirrus.bin", true);
        /* XXX ISA-LFB support */
        /* FIXME not qdev yet */
        return 0;
    }

//    static void isa_cirrus_vga_class_init(ObjectClass *klass, void *data)
//    {
//        ISADeviceClass *k = ISA_DEVICE_CLASS(klass);
//        DeviceClass *dc = DEVICE_CLASS(klass);
//
//        dc.vmsd  = &vmstate_cirrus_vga;
//        k.init   = vga_initfn;
//    }

//    static TypeInfo isa_cirrus_vga_info = {
//        .name          = "isa-cirrus-vga",
//        .parent        = TYPE_ISA_DEVICE,
//        .instance_size = sizeof(ISACirrusVGAState),
//        .class_init = isa_cirrus_vga_class_init,
//    };
//
    /***************************************
     *
     *  PCI bus support
     *
     ***************************************/

//    static int pci_cirrus_vga_initfn(PCIDevice *dev)
//    {
//         PCICirrusVGAState *d = DO_UPCAST(PCICirrusVGAState, dev, dev);
//         CirrusVGAState *s = &d.cirrus_vga;
//         PCIDeviceClass *pc = PCI_DEVICE_GET_CLASS(dev);
//         int16_t device_id = pc.device_id;
//
//         /* setup VGA */
//         s.vram_size_mb = VGA_RAM_SIZE >> 20;
//         vga_common_init(&s.vga);
//         cirrus_init_common(s, device_id, 1, pci_address_space(dev));
//         s.ds = graphic_console_init(s.update, s.invalidate, s.screen_dump, s.text_update, &s.vga);
    
         /* setup PCI */

//        memory_region_init(&s.pci_bar, "cirrus-pci-bar0", 0x2000000);
    
        /* XXX: add byte swapping apertures */
//        Memory.MEM_SetPageHandler(0, jdos.hardware.VGA.vga.vmemsize >> 12, new cirrus_linear());
//        memory_region_add_subregion(&s.pci_bar, 0, &s.cirrus_linear_io);

    //Memory.MEM_SetPageHandler(0x1000000, 0x400, new cirrus_linear_bitblt());
    //memory_region_add_subregion(&s.pci_bar, 0x1000000, &s.cirrus_linear_bitblt_io);
    
         /* setup memory space */
         /* memory #0 LFB */
         /* memory #1 memory-mapped I/O */
         /* XXX: s.vram_size must be a power of two */
//         pci_register_bar(&d.dev, 0, PCI_BASE_ADDRESS_MEM_PREFETCH, &s.pci_bar);
//         if (device_id == CIRRUS_ID_CLGD5446) {
//             Memory.MEM_SetPageHandler(0x1000000, CIRRUS_PNPMMIO_SIZE >> 12, new cirrus_mmio());
//             pci_register_bar(&d.dev, 1, 0, &s.cirrus_mmio_io);
//         }
//         return 0;
//    }

//    DeviceState *pci_cirrus_vga_init(PCIBus *bus)
//    {
//        return &pci_create_simple(bus, -1, "cirrus-vga").qdev;
//    }

//    static void cirrus_vga_class_init(ObjectClass *klass, void *data)
//    {
//        DeviceClass *dc = DEVICE_CLASS(klass);
//        PCIDeviceClass *k = PCI_DEVICE_CLASS(klass);
//
//        k.no_hotplug = 1;
//        k.init = pci_cirrus_vga_initfn;
//        k.romfile = VGABIOS_CIRRUS_FILENAME;
//        k.vendor_id = PCI_VENDOR_ID_CIRRUS;
//        k.device_id = CIRRUS_ID_CLGD5446;
//        k.class_id = PCI_CLASS_DISPLAY_VGA;
//        dc.desc = "Cirrus CLGD 54xx VGA";
//        dc.vmsd = &vmstate_pci_cirrus_vga;
//    }

    static private CirrusVGAState cirrusVGAState;
    public static Section.SectionFunction Cirrus_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            cirrusVGAState = new CirrusVGAState();
            cirrusVGAState.cirrus_bltbufPos = jdos.hardware.VGA.vga.vmemsize;
            vga_initfn();
        }
    };
}
