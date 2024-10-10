package jdos.hardware.mame;

import jdos.cpu.Paging;
import jdos.gui.Render;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.hardware.VGA;
import jdos.hardware.VGA_draw;
import jdos.hardware.pci.PCI;
import jdos.hardware.pci.PCI_Device;
import jdos.hardware.pci.PCI_Memory_BAR;
import jdos.hardware.pci.PCI_PageHandler;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.IntRef;
import jdos.util.LongRef;

import java.util.Arrays;

// I ported this from the mame project, this is their license

/***************************************************************************

    voodoo.c

    3dfx Voodoo Graphics SST-1/2 emulator.

****************************************************************************

    Copyright Aaron Giles
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in
          the documentation and/or other materials provided with the
          distribution.
        * Neither the name 'MAME' nor the names of its contributors may be
          used to endorse or promote products derived from this software
          without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY AARON GILES ''AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL AARON GILES BE LIABLE FOR ANY DIRECT,
    INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
    STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
    IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.

 */

public class VoodooCommon extends PCI_Device {
    static private final int VOODOO_MEM = 0x60000000;

    static private final long ATTOSECONDS_PER_SECOND = 1000000000;// java only gets nano seconds

    static final int TYPE_VOODOO_1 = 0;
    static final int TYPE_VOODOO_2 = 1;
    static final int TYPE_VOODOO_BANSHEE = 2;
    static final int TYPE_VOODOO_3 = 3;

    static public long regAsUnsignedInt(int i) {return i & 0xFFFFFFFFl;}
    static public float regAsFloat(int i) {return Float.intBitsToFloat(i);}
    static public int setRegAsFloat(int i) {return Float.floatToRawIntBits((float)i);}
    static public int getRegB(int i) {return i & 0xFF;}
    static public int getRegG(int i) {return (i >> 8) & 0xFF;}
    static public int getRegR(int i) {return (i >> 16) & 0xFF;}
    static public int getRegA(int i) {return (i >> 24) & 0xFF;}
    static public int setRegRGBA(int r, int g, int b, int a) {return (b & 0xFF) | ((g & 0xFF) << 8) | ((r & 0xFF) << 16) | ((a & 0xFF) << 24);}
    static public int setRegA(int i, int a) {return (i & 0x00FFFFFF) | (a<<24);}

    /* enumeration describing reasons we might be stalled */
    static final int NOT_STALLED = 0;
    static final int STALLED_UNTIL_FIFO_LWM = 1;
    static final int STALLED_UNTIL_FIFO_EMPTY = 2;

    /* maximum number of TMUs */
    static final int MAX_TMU = 2;

    /* accumulate operations less than this number of clocks */
    static final int ACCUMULATE_THRESHOLD = 0;
    
    /* number of clocks to set up a triangle (just a guess) */
    static final int TRIANGLE_SETUP_CLOCKS = 100;
    
    /* maximum number of rasterizers */
    static final int MAX_RASTERIZERS = 1024;
    
    /* size of the rasterizer hash table */
    static final int RASTER_HASH_SIZE = 97;
    
    /* flags for the register access array */
    static final int REGISTER_READ         = 0x01;        /* reads are allowed */
    static final int REGISTER_WRITE        = 0x02;        /* writes are allowed */
    static final int REGISTER_PIPELINED    = 0x04;        /* writes are pipelined */
    static final int REGISTER_FIFO         = 0x08;        /* writes go to FIFO */
    static final int REGISTER_WRITETHRU    = 0x10;        /* writes are valid even for CMDFIFO */

    /* shorter combinations to make the table smaller */
    static final int REG_R                  = (REGISTER_READ);
    static final int REG_W                  = (REGISTER_WRITE);
    static final int REG_WT                 = (REGISTER_WRITE | REGISTER_WRITETHRU);
    static final int REG_RW                 = (REGISTER_READ | REGISTER_WRITE);
    static final int REG_RWT                = (REGISTER_READ | REGISTER_WRITE | REGISTER_WRITETHRU);
    static final int REG_RP                 = (REGISTER_READ | REGISTER_PIPELINED);
    static final int REG_WP                 = (REGISTER_WRITE | REGISTER_PIPELINED);
    static final int REG_RWP                = (REGISTER_READ | REGISTER_WRITE | REGISTER_PIPELINED);
    static final int REG_RWPT               = (REGISTER_READ | REGISTER_WRITE | REGISTER_PIPELINED | REGISTER_WRITETHRU);
    static final int REG_RF                 = (REGISTER_READ | REGISTER_FIFO);
    static final int REG_WF                 = (REGISTER_WRITE | REGISTER_FIFO);
    static final int REG_RWF                = (REGISTER_READ | REGISTER_WRITE | REGISTER_FIFO);
    static final int REG_RPF                = (REGISTER_READ | REGISTER_PIPELINED | REGISTER_FIFO);
    static final int REG_WPF                = (REGISTER_WRITE | REGISTER_PIPELINED | REGISTER_FIFO);
    static final int REG_RWPF               = (REGISTER_READ | REGISTER_WRITE | REGISTER_PIPELINED | REGISTER_FIFO);

    /* lookup bits is the log2 of the size of the reciprocal/log table */
    static final int RECIPLOG_LOOKUP_BITS   = 9;

    /* input precision is how many fraction bits the input value has; this is a 64-bit number */
    static final int RECIPLOG_INPUT_PREC    = 32;

    /* lookup precision is how many fraction bits each table entry contains */
    static final int RECIPLOG_LOOKUP_PREC   = 22;

    /* output precision is how many fraction bits the result should have */
    static final int RECIP_OUTPUT_PREC      = 15;
    static final int LOG_OUTPUT_PREC        = 8;

    static final public class voodoo_stats
    {
        public int               lastkey;                /* last key state */
        public boolean           display;                /* display stats? */
        public int               swaps;                  /* total swaps */
        public int               stalls;                 /* total stalls */
        public int               total_triangles;        /* total triangles */
        public int               total_pixels_in;        /* total pixels in */
        public int               total_pixels_out;       /* total pixels out */
        public int               total_chroma_fail;      /* total chroma fail */
        public int               total_zfunc_fail;       /* total z func fail */
        public int               total_afunc_fail;       /* total a func fail */
        public int               total_clipped;          /* total clipped */
        public int               total_stippled;         /* total stippled */
        public int               lfb_writes;             /* LFB writes */
        public int               lfb_reads;              /* LFB reads */
        public int               reg_writes;             /* register writes */
        public int               reg_reads;              /* register reads */
        public int               tex_writes;             /* texture writes */
        public int[]             texture_mode = new int[16];       /* 16 different texture modes */
        public int               render_override;        /* render override */
        public String            buffer;                 /* string */
    }


    static final class fifo_state
    {
        public fifo_state(short[] mem) {
            this.mem = mem;
        }
        public void write(int offset, int value) {
            mem_writed(mem, offset*2+base, value);
        }
        public int read(int offset) {

            return mem_readd(mem, offset*2+base);
        }

        short[]           mem;
    	int               base;                   /* base of the FIFO */
    	int               size;                   /* size of the FIFO */
        int               in;                     /* input pointer */
        int               out;                    /* output pointer */

    }


    static final class cmdfifo_info
    {
    	boolean             enable;                 /* enabled? */
        boolean             count_holes;            /* count holes? */
        int                 base;                   /* base address in framebuffer RAM */
        int                 end;                    /* end address in framebuffer RAM */
        int                 rdptr;                  /* current read pointer */
        int                 amin;                   /* minimum address */
        int                 amax;                   /* maximum address */
        int                 depth;                  /* current depth */
        int                 holes;                  /* number of holes */
    }

    static interface voodoo_stall_func {
        public void call(PCI_Device device, int state);
    }

    static final class pci_state
    {
    	fifo_state          fifo = new fifo_state(new short[64*4]);   /* PCI FIFO */
    	int                 init_enable;               /* initEnable value */
    	int                 stall_state;               /* state of the system if we're stalled */
    	voodoo_stall_func   stall_callback;            /* callback for stalling/unstalling */
    	boolean             op_pending;                /* true if an operation is pending */
    	long                op_end_time;               /* time when the pending operation ends */
    	//emu_timer *         continue_timer;            /* timer to use to continue processing */
    }


    static final class ncc_table
    {
    	boolean             dirty;                  /* is the texel lookup dirty? */
    	int                 reg;                    /* pointer to our registers */
    	int[]               ir = new int[4], ig = new int[4], ib = new int[4];    /* I values for R,G,B */
    	int[]               qr = new int[4], qg = new int[4], qb = new int[4];    /* Q values for R,G,B */
    	int[]               y = new int[16];                  /* Y values */
    	int[]               palette;                /* pointer to associated RGB palette */
    	int[]               palettea;               /* pointer to associated ARGB palette */
    	int[]               texel = new int[256];             /* texel lookup */
    }


    static final class tmu_shared_state
    {
    	int[]               rgb332 = new int[256];            /* RGB 3-3-2 lookup table */
        int[]               alpha8 = new int[256];            /* alpha 8-bit lookup table */
        int[]               int8 = new int[256];              /* intensity 8-bit lookup table */
        int[]               ai44 = new int[256];              /* alpha, intensity 4-4 lookup table */

        int[]               rgb565 = new int[65536];          /* RGB 5-6-5 lookup table */
        int[]               argb1555 = new int[65536];        /* ARGB 1-5-5-5 lookup table */
        int[]               argb4444 = new int[65536];        /* ARGB 4-4-4-4 lookup table */
    }


    static final class setup_vertex
    {
    	float               x, y;                   /* X, Y coordinates */
    	float               a, r, g, b;             /* A, R, G, B values */
    	float               z, wb;                  /* Z and broadcast W values */
    	float               w0, s0, t0;             /* W, S, T for TMU 0 */
    	float               w1, s1, t1;             /* W, S, T for TMU 1 */
    }

    static interface voodoo_vblank_func {
        public void call(PCI_Device device, int state);
    }

    static public final class fbi_state
    {
        public fbi_state() {
            for (int i=0;i<svert.length;i++) {
                svert[i] = new setup_vertex();
            }
            for (int i=0;i<cmdfifo.length;i++) {
                cmdfifo[i] = new cmdfifo_info();
            }
        }
        public short[]             ram;                    /* pointer to frame buffer RAM */
        public int                 mask;                   /* mask to apply to pointers */
        public int[]               rgboffs = new int[3];   /* word offset to 3 RGB buffers */
        public int                 auxoffs;                /* word offset to 1 aux buffer */

        public int                 frontbuf;               /* front buffer index */
        public int                 backbuf;                /* back buffer index */
        public int                 swaps_pending;          /* number of pending swaps */
        public boolean             video_changed;          /* did the frontbuffer video change? */

        public int                 yorigin;                /* Y origin subtract value */
        public int                 lfb_base;               /* base of LFB in memory */
        public int                 lfb_stride;             /* stride of LFB accesses in bits */

        public int                 width;                  /* width of current frame buffer */
        public int                 height;                 /* height of current frame buffer */
        public int                 xoffs;                  /* horizontal offset (back porch) */
        public int                 yoffs;                  /* vertical offset (back porch) */
        public int                 vsyncscan;              /* vertical sync scanline */
        public int                 rowpixels;              /* pixels per row */
        public int                 tile_width;             /* width of video tiles */
        public int                 tile_height;            /* height of video tiles */
        public int                 x_tiles;                /* number of tiles in the X direction */

        public boolean             vblank;                 /* VBLANK state */
        public int                 vblank_count;           /* number of VBLANKs since last swap */
        public boolean             vblank_swap_pending;    /* a swap is pending, waiting for a vblank */
        public int                 vblank_swap;            /* swap when we hit this count */
        public boolean             vblank_dont_swap;       /* don't actually swap when we hit this point */
        public voodoo_vblank_func  vblank_client;          /* client callback */

    	/* triangle setup info */
        public boolean             cheating_allowed;       /* allow cheating? */
        public int                 sign;                   /* triangle sign */
        public short               ax, ay;                 /* vertex A x,y (12.4) */
        public short               bx, by;                 /* vertex B x,y (12.4) */
        public short               cx, cy;                 /* vertex C x,y (12.4) */
        public int                 startr, startg, startb, starta; /* starting R,G,B,A (12.12) */
        public int                 startz;                 /* starting Z (20.12) */
        public long                startw;                 /* starting W (16.32) */
        public int                 drdx, dgdx, dbdx, dadx; /* delta R,G,B,A per X */
        public int                 dzdx;                   /* delta Z per X */
        public long                dwdx;                   /* delta W per X */
        public int                 drdy, dgdy, dbdy, dady; /* delta R,G,B,A per Y */
        public int                 dzdy;                   /* delta Z per Y */
        public long                dwdy;                   /* delta W per Y */

        public stats_block         lfb_stats = new stats_block();              /* LFB-access statistics */

        public int                 sverts;                 /* number of vertices ready */
        public setup_vertex[]      svert = new setup_vertex[3];               /* 3 setup vertices */

        public fifo_state          fifo = new fifo_state(null);                   /* framebuffer memory fifo */
        public cmdfifo_info[]      cmdfifo = new cmdfifo_info[2];             /* command FIFOs */

        public int[]               fogblend = new int[64];           /* 64-entry fog table */
        public int[]               fogdelta = new int[64];           /* 64-entry fog table */
        public int                 fogdelta_mask;          /* mask for for delta (0xff for V1, 0xfc for V2) */

        public int[]               pen = new int[65536];             /* mapping from pixels to pens */
        public int[]               clut = new int[512];              /* clut gamma data */
        public boolean             clut_dirty;             /* do we need to recompute? */
    }


    static final class dac_state
    {
    	int[]               reg = new int[8];       /* 8 registers */
    	int                 read_result;            /* pending read result */
    }

    ;

    static final class banshee_info
    {
    	int[]               io = new int[0x40];               /* I/O registers */
        int[]               agp = new int[0x80];              /* AGP registers */
    	byte[]              vga = new byte[0x20];              /* VGA registers */
        byte[]              crtc = new byte[0x27];             /* VGA CRTC registers */
        byte[]              seq = new byte[0x05];              /* VGA sequencer registers */
        byte[]              gc = new byte[0x05];               /* VGA graphics controller registers */
        byte[]              att =new byte[0x15];              /* VGA attribute registers */
    	byte                attff;                  /* VGA attribute flip-flop */

    	int[]               blt_regs = new int[0x20];         /* 2D Blitter registers */
        int                 blt_dst_base;
        int                 blt_dst_x;
        int                 blt_dst_y;
        int                 blt_dst_width;
        int                 blt_dst_height;
        int                 blt_dst_stride;
        int                 blt_dst_bpp;
        int                 blt_cmd;
        int                 blt_src_base;
        int                 blt_src_x;
        int                 blt_src_y;
        int                 blt_src_width;
        int                 blt_src_height;
        int                 blt_src_stride;
        int                 blt_src_bpp;
    }

    //screen_device *screen;              /* the screen we are acting on */
    //device_t *cpu;                  /* the CPU we interact with */
    public int                 type;                   /* type of system */
    public int                 chipmask;               /* mask for which chips are available */
    public int                 freq;                   /* operating frequency */
    public long                attoseconds_per_cycle;  /* attoseconds per cycle */
    public int                 extra_cycles;           /* extra cycles not yet accounted for */
    public int                 trigger;                /* trigger used for stalling */

    public int[]               reg = new int[0x400];             /* raw registers */
    public byte[]              regaccess;              /* register access array */
    public String[]            regnames;               /* register names array */
    public boolean             alt_regmap;             /* enable alternate register map? */

    public pci_state           pci = new pci_state();  /* PCI state */
    public dac_state           dac = new dac_state();                    /* DAC state */

    public fbi_state           fbi = new fbi_state();  /* FBI states */
    public tmu_state[]         tmu = new tmu_state[MAX_TMU];           /* TMU states */
    public tmu_shared_state    tmushare = new tmu_shared_state();               /* TMU shared state */
    public banshee_info        banshee = new banshee_info();                /* Banshee state */

    public Poly.poly_manager   poly = new Poly.poly_manager();                   /* polygon manager */
    public stats_block[]       thread_stats;           /* per-thread statistics */

    public voodoo_stats        stats = new voodoo_stats();                  /* internal statistics */

    public int                 last_status_pc;         /* PC of last status description (for logging) */
    public int                 last_status_value;      /* value of last status read (for logging) */

    public int                 next_rasterizer;        /* next rasterizer index */
    public raster_info[]       rasterizer = new raster_info[MAX_RASTERIZERS]; /* array of rasterizers */
    raster_info[]       raster_hash = new raster_info[RASTER_HASH_SIZE]; /* hash table of rasterizers */

    static final class rectangle {
        int min_x, max_x, min_y, max_y;
        public rectangle() {
        }
        public rectangle(int min_x, int max_x, int min_y, int max_y) {
            set(min_x, max_x, min_y, max_y);
        }

        public void set(int min_x, int max_x, int min_y, int max_y) {
            this.min_x = min_x;
            this.max_x = max_x;
            this.min_y = min_y;
            this.max_y = max_y;
        }
    }
    static final rectangle global_cliprect = new rectangle(-4096, 4095, -4096, 4095);

    /* fast dither lookup */
    static final public byte[] dither4_lookup = new byte[256*16*2];
    static final public byte[] dither2_lookup = new byte[256*16*2];
    static final public int[] voodoo_reciplog = new int[(2 << RECIPLOG_LOOKUP_BITS) + 2];

    static double LOGB2(double v) {
        return Math.log(v) / Math.log(2);
    }

    /*************************************
     *
     *  Macros for extracting pixels
     *
     *************************************/

    static private void EXTRACT_565_TO_888(int val, int[] a, int[] b, int[] c, int index) {
    	a[index] = ((val >> 8) & 0xf8) | ((val >> 13) & 0x07);
    	b[index] = ((val >> 3) & 0xfc) | ((val >> 9) & 0x03);
    	c[index] = ((val << 3) & 0xf8) | ((val >> 2) & 0x07);
    }

    static private void EXTRACT_x555_TO_888(int val, int[] a, int[] b, int[] c, int index) {
    	a[index] = ((val >> 7) & 0xf8) | ((val >> 12) & 0x07);
    	b[index] = ((val >> 2) & 0xf8) | ((val >> 7) & 0x07);
    	c[index] = ((val << 3) & 0xf8) | ((val >> 2) & 0x07);
    }

    static private void EXTRACT_555x_TO_888(int val, int[] a, int[] b, int[] c, int index) {
    	a[index] = ((val >> 8) & 0xf8) | ((val >> 13) & 0x07);
    	b[index] = ((val >> 3) & 0xf8) | ((val >> 8) & 0x07);
    	c[index] = ((val << 2) & 0xf8) | ((val >> 3) & 0x07);
    }

    static private void EXTRACT_1555_TO_8888(int val, int[] a, int[] b, int[] c, int[] d, int index) {
    	a[index] = (val >> 15) & 0xff;
    	EXTRACT_x555_TO_888(val, b, c, d, index);
    }

    static private void EXTRACT_5551_TO_8888(int val, int[] a, int[] b, int[] c, int[] d, int index) {
    	EXTRACT_555x_TO_888(val, a, b, c, index);
    	d[index] = (val & 0x0001)!=0 ? 0xff : 0x00;
    }

    static private void EXTRACT_x888_TO_888(int val, int[] a, int[] b, int[] c, int index){
    	a[index] = (val >> 16) & 0xff;
    	b[index] = (val >> 8) & 0xff;
    	c[index] = (val >> 0) & 0xff;
    }

    static private void EXTRACT_888x_TO_888(int val, int[] a, int[] b, int[] c, int index) {
    	a[index] = (val >> 24) & 0xff;
    	b[index] = (val >> 16) & 0xff;
    	c[index] = (val >> 8) & 0xff;
    }

    static private void EXTRACT_8888_TO_8888(int val, int[] a, int[] b, int[] c, int[] d, int index) {
    	a[index] = (val >> 24) & 0xff;
    	b[index] = (val >> 16) & 0xff;
    	c[index] = (val >> 8) & 0xff;
    	d[index] = (val >> 0) & 0xff;
    }

    static private void EXTRACT_4444_TO_8888(int val, int[] a, int[] b, int[] c, int[] d, int index) {
    	a[index] = ((val >> 8) & 0xf0) | ((val >> 12) & 0x0f);
    	b[index] = ((val >> 4) & 0xf0) | ((val >> 8) & 0x0f);
    	c[index] = ((val >> 0) & 0xf0) | ((val >> 4) & 0x0f);
    	d[index] = ((val << 4) & 0xf0) | ((val >> 0) & 0x0f);
    }

    static private void EXTRACT_332_TO_888(int val, int[] a, int[] b, int[] c, int index) {
    	a[index] = ((val >> 0) & 0xe0) | ((val >> 3) & 0x1c) | ((val >> 6) & 0x03);
    	b[index] = ((val << 3) & 0xe0) | ((val >> 0) & 0x1c) | ((val >> 3) & 0x03);
    	c[index] = ((val << 6) & 0xc0) | ((val << 4) & 0x30) | ((val << 2) & 0xc0) | ((val << 0) & 0x03);
    }
    
    static int CLAMPr(int val,int min,int max) {
        if (val < min) {
            val = min;
        } else if (val > max) {
            val = max;
        }
        return val;
    }

    static int CLAMPED_Zr(int ITERZ, int FBZCP) {
    	int RESULT = ITERZ >> 12;
    	if (!FBZCP_RGBZW_CLAMP(FBZCP))
    	{
    		RESULT &= 0xfffff;
    		if (RESULT == 0xfffff)
    			RESULT = 0;
    		else if (RESULT == 0x10000)
    			RESULT = 0xffff;
    		else
    			RESULT &= 0xffff;
            return RESULT;
    	}
    	else
    	{
    		return CLAMPr(RESULT, 0, 0xffff);
    	}
    }

    static int CLAMPED_Wr(long ITERW, int FBZCP) {
    	int RESULT = (short)((ITERW) >> 32);
    	if (!FBZCP_RGBZW_CLAMP(FBZCP))
    	{
    		RESULT &= 0xffff;
    		if (RESULT == 0xffff)
    			RESULT = 0;
    		else if (RESULT == 0x100)
    			RESULT = 0xff;
    		RESULT &= 0xff;
            return RESULT;
    	}
    	else
    	{
    		return CLAMPr(RESULT, 0, 0xff);
    	}
    }

    /*************************************
     *
     *  Dithering tables
     *
     *************************************/

    static public final byte[] dither_matrix_4x4 = new byte[]
    {
    		0,  8,  2, 10,
    	    12,  4, 14,  6,
    		3, 11,  1,  9,
    	    15,  7, 13,  5
    };

    static public final byte[] dither_matrix_2x2 = new byte[]
    {
    		2, 10,  2, 10,
    	    14,  6, 14,  6,
    		2, 10,  2, 10,
    	    14,  6, 14,  6
    };

    /*************************************
     *
     *  Dithering macros
     *
     *************************************/

    /* note that these equations and the dither matrixes have
       been confirmed to be exact matches to the real hardware */
    static private int DITHER_RB(int val, int dith) { return ((((val) << 1) - ((val) >> 4) + ((val) >> 7) + (dith)) >> 1);}
    static private int DITHER_G(int val, int dith) { return ((((val) << 2) - ((val) >> 4) + ((val) >> 6) + (dith)) >> 2);}

    static {
        /* create a table of precomputed 1/n and log2(n) values */
        /* n ranges from 1.0000 to 2.0000 */
        for (int val = 0; val <= (1 << RECIPLOG_LOOKUP_BITS); val++)
        {
            long value = (1l << RECIPLOG_LOOKUP_BITS) + val;
            voodoo_reciplog[val*2 + 0] = (int)((1l << (RECIPLOG_LOOKUP_PREC + RECIPLOG_LOOKUP_BITS)) / value);
            voodoo_reciplog[val*2 + 1] =  (int)(LOGB2((double)value / (double)(1 << RECIPLOG_LOOKUP_BITS)) * (double)(1 << RECIPLOG_LOOKUP_PREC));
        }

        /* create dithering tables */
        for (int val = 0; val < 256*16*2; val++)
        {
            int g = (val >> 0) & 1;
            int x = (val >> 1) & 3;
            int color = (val >> 3) & 0xff;
            int y = (val >> 11) & 3;

            if (g==0)
            {
                dither4_lookup[val] = (byte)(DITHER_RB(color, dither_matrix_4x4[y * 4 + x]) >> 3);
                dither2_lookup[val] = (byte)(DITHER_RB(color, dither_matrix_2x2[y * 4 + x]) >> 3);
            }
            else
            {
                dither4_lookup[val] = (byte)(DITHER_G(color, dither_matrix_4x4[y * 4 + x]) >> 2);
                dither2_lookup[val] = (byte)(DITHER_G(color, dither_matrix_2x2[y * 4 + x]) >> 2);
            }
        }
    }
    /*************************************
     *
     *  Register string table for debug
     *
     *************************************/
    static private final String[] voodoo_reg_name = new String[]
    {
    	/* 0x000 */
    	"status",       "{intrCtrl}",   "vertexAx",     "vertexAy",
    	"vertexBx",     "vertexBy",     "vertexCx",     "vertexCy",
    	"startR",       "startG",       "startB",       "startZ",
    	"startA",       "startS",       "startT",       "startW",
    	/* 0x040 */
    	"dRdX",         "dGdX",         "dBdX",         "dZdX",
    	"dAdX",         "dSdX",         "dTdX",         "dWdX",
    	"dRdY",         "dGdY",         "dBdY",         "dZdY",
    	"dAdY",         "dSdY",         "dTdY",         "dWdY",
    	/* 0x080 */
    	"triangleCMD",  "reserved084",  "fvertexAx",    "fvertexAy",
    	"fvertexBx",    "fvertexBy",    "fvertexCx",    "fvertexCy",
    	"fstartR",      "fstartG",      "fstartB",      "fstartZ",
    	"fstartA",      "fstartS",      "fstartT",      "fstartW",
    	/* 0x0c0 */
    	"fdRdX",        "fdGdX",        "fdBdX",        "fdZdX",
    	"fdAdX",        "fdSdX",        "fdTdX",        "fdWdX",
    	"fdRdY",        "fdGdY",        "fdBdY",        "fdZdY",
    	"fdAdY",        "fdSdY",        "fdTdY",        "fdWdY",
    	/* 0x100 */
    	"ftriangleCMD", "fbzColorPath", "fogMode",      "alphaMode",
    	"fbzMode",      "lfbMode",      "clipLeftRight","clipLowYHighY",
    	"nopCMD",       "fastfillCMD",  "swapbufferCMD","fogColor",
    	"zaColor",      "chromaKey",    "{chromaRange}","{userIntrCMD}",
    	/* 0x140 */
    	"stipple",      "color0",       "color1",       "fbiPixelsIn",
    	"fbiChromaFail","fbiZfuncFail", "fbiAfuncFail", "fbiPixelsOut",
    	"fogTable160",  "fogTable164",  "fogTable168",  "fogTable16c",
    	"fogTable170",  "fogTable174",  "fogTable178",  "fogTable17c",
    	/* 0x180 */
    	"fogTable180",  "fogTable184",  "fogTable188",  "fogTable18c",
    	"fogTable190",  "fogTable194",  "fogTable198",  "fogTable19c",
    	"fogTable1a0",  "fogTable1a4",  "fogTable1a8",  "fogTable1ac",
    	"fogTable1b0",  "fogTable1b4",  "fogTable1b8",  "fogTable1bc",
    	/* 0x1c0 */
    	"fogTable1c0",  "fogTable1c4",  "fogTable1c8",  "fogTable1cc",
    	"fogTable1d0",  "fogTable1d4",  "fogTable1d8",  "fogTable1dc",
    	"{cmdFifoBaseAddr}","{cmdFifoBump}","{cmdFifoRdPtr}","{cmdFifoAMin}",
    	"{cmdFifoAMax}","{cmdFifoDepth}","{cmdFifoHoles}","reserved1fc",
    	/* 0x200 */
    	"fbiInit4",     "vRetrace",     "backPorch",    "videoDimensions",
    	"fbiInit0",     "fbiInit1",     "fbiInit2",     "fbiInit3",
    	"hSync",        "vSync",        "clutData",     "dacData",
    	"maxRgbDelta",  "{hBorder}",    "{vBorder}",    "{borderColor}",
    	/* 0x240 */
    	"{hvRetrace}",  "{fbiInit5}",   "{fbiInit6}",   "{fbiInit7}",
    	"reserved250",  "reserved254",  "{fbiSwapHistory}","{fbiTrianglesOut}",
    	"{sSetupMode}", "{sVx}",        "{sVy}",        "{sARGB}",
    	"{sRed}",       "{sGreen}",     "{sBlue}",      "{sAlpha}",
    	/* 0x280 */
    	"{sVz}",        "{sWb}",        "{sWtmu0}",     "{sS/Wtmu0}",
    	"{sT/Wtmu0}",   "{sWtmu1}",     "{sS/Wtmu1}",   "{sT/Wtmu1}",
    	"{sDrawTriCMD}","{sBeginTriCMD}","reserved2a8", "reserved2ac",
    	"reserved2b0",  "reserved2b4",  "reserved2b8",  "reserved2bc",
    	/* 0x2c0 */
    	"{bltSrcBaseAddr}","{bltDstBaseAddr}","{bltXYStrides}","{bltSrcChromaRange}",
    	"{bltDstChromaRange}","{bltClipX}","{bltClipY}","reserved2dc",
    	"{bltSrcXY}",   "{bltDstXY}",   "{bltSize}",    "{bltRop}",
    	"{bltColor}",   "reserved2f4",  "{bltCommand}", "{bltData}",
    	/* 0x300 */
    	"textureMode",  "tLOD",         "tDetail",      "texBaseAddr",
    	"texBaseAddr_1","texBaseAddr_2","texBaseAddr_3_8","trexInit0",
    	"trexInit1",    "nccTable0.0",  "nccTable0.1",  "nccTable0.2",
    	"nccTable0.3",  "nccTable0.4",  "nccTable0.5",  "nccTable0.6",
    	/* 0x340 */
    	"nccTable0.7",  "nccTable0.8",  "nccTable0.9",  "nccTable0.A",
    	"nccTable0.B",  "nccTable1.0",  "nccTable1.1",  "nccTable1.2",
    	"nccTable1.3",  "nccTable1.4",  "nccTable1.5",  "nccTable1.6",
    	"nccTable1.7",  "nccTable1.8",  "nccTable1.9",  "nccTable1.A",
    	/* 0x380 */
    	"nccTable1.B"
    };


    static private final String[] banshee_reg_name = new String[]
    {
    	/* 0x000 */
    	"status",       "intrCtrl",     "vertexAx",     "vertexAy",
    	"vertexBx",     "vertexBy",     "vertexCx",     "vertexCy",
    	"startR",       "startG",       "startB",       "startZ",
    	"startA",       "startS",       "startT",       "startW",
    	/* 0x040 */
    	"dRdX",         "dGdX",         "dBdX",         "dZdX",
    	"dAdX",         "dSdX",         "dTdX",         "dWdX",
    	"dRdY",         "dGdY",         "dBdY",         "dZdY",
    	"dAdY",         "dSdY",         "dTdY",         "dWdY",
    	/* 0x080 */
    	"triangleCMD",  "reserved084",  "fvertexAx",    "fvertexAy",
    	"fvertexBx",    "fvertexBy",    "fvertexCx",    "fvertexCy",
    	"fstartR",      "fstartG",      "fstartB",      "fstartZ",
    	"fstartA",      "fstartS",      "fstartT",      "fstartW",
    	/* 0x0c0 */
    	"fdRdX",        "fdGdX",        "fdBdX",        "fdZdX",
    	"fdAdX",        "fdSdX",        "fdTdX",        "fdWdX",
    	"fdRdY",        "fdGdY",        "fdBdY",        "fdZdY",
    	"fdAdY",        "fdSdY",        "fdTdY",        "fdWdY",
    	/* 0x100 */
    	"ftriangleCMD", "fbzColorPath", "fogMode",      "alphaMode",
    	"fbzMode",      "lfbMode",      "clipLeftRight","clipLowYHighY",
    	"nopCMD",       "fastfillCMD",  "swapbufferCMD","fogColor",
    	"zaColor",      "chromaKey",    "chromaRange",  "userIntrCMD",
    	/* 0x140 */
    	"stipple",      "color0",       "color1",       "fbiPixelsIn",
    	"fbiChromaFail","fbiZfuncFail", "fbiAfuncFail", "fbiPixelsOut",
    	"fogTable160",  "fogTable164",  "fogTable168",  "fogTable16c",
    	"fogTable170",  "fogTable174",  "fogTable178",  "fogTable17c",
    	/* 0x180 */
    	"fogTable180",  "fogTable184",  "fogTable188",  "fogTable18c",
    	"fogTable190",  "fogTable194",  "fogTable198",  "fogTable19c",
    	"fogTable1a0",  "fogTable1a4",  "fogTable1a8",  "fogTable1ac",
    	"fogTable1b0",  "fogTable1b4",  "fogTable1b8",  "fogTable1bc",
    	/* 0x1c0 */
    	"fogTable1c0",  "fogTable1c4",  "fogTable1c8",  "fogTable1cc",
    	"fogTable1d0",  "fogTable1d4",  "fogTable1d8",  "fogTable1dc",
    	"reserved1e0",  "reserved1e4",  "reserved1e8",  "colBufferAddr",
    	"colBufferStride","auxBufferAddr","auxBufferStride","reserved1fc",
    	/* 0x200 */
    	"clipLeftRight1","clipTopBottom1","reserved208","reserved20c",
    	"reserved210",  "reserved214",  "reserved218",  "reserved21c",
    	"reserved220",  "reserved224",  "reserved228",  "reserved22c",
    	"reserved230",  "reserved234",  "reserved238",  "reserved23c",
    	/* 0x240 */
    	"reserved240",  "reserved244",  "reserved248",  "swapPending",
    	"leftOverlayBuf","rightOverlayBuf","fbiSwapHistory","fbiTrianglesOut",
    	"sSetupMode",   "sVx",          "sVy",          "sARGB",
    	"sRed",         "sGreen",       "sBlue",        "sAlpha",
    	/* 0x280 */
    	"sVz",          "sWb",          "sWtmu0",       "sS/Wtmu0",
    	"sT/Wtmu0",     "sWtmu1",       "sS/Wtmu1",     "sT/Wtmu1",
    	"sDrawTriCMD",  "sBeginTriCMD", "reserved2a8",  "reserved2ac",
    	"reserved2b0",  "reserved2b4",  "reserved2b8",  "reserved2bc",
    	/* 0x2c0 */
    	"reserved2c0",  "reserved2c4",  "reserved2c8",  "reserved2cc",
    	"reserved2d0",  "reserved2d4",  "reserved2d8",  "reserved2dc",
    	"reserved2e0",  "reserved2e4",  "reserved2e8",  "reserved2ec",
    	"reserved2f0",  "reserved2f4",  "reserved2f8",  "reserved2fc",
    	/* 0x300 */
    	"textureMode",  "tLOD",         "tDetail",      "texBaseAddr",
    	"texBaseAddr_1","texBaseAddr_2","texBaseAddr_3_8","reserved31c",
    	"trexInit1",    "nccTable0.0",  "nccTable0.1",  "nccTable0.2",
    	"nccTable0.3",  "nccTable0.4",  "nccTable0.5",  "nccTable0.6",
    	/* 0x340 */
    	"nccTable0.7",  "nccTable0.8",  "nccTable0.9",  "nccTable0.A",
    	"nccTable0.B",  "nccTable1.0",  "nccTable1.1",  "nccTable1.2",
    	"nccTable1.3",  "nccTable1.4",  "nccTable1.5",  "nccTable1.6",
    	"nccTable1.7",  "nccTable1.8",  "nccTable1.9",  "nccTable1.A",
    	/* 0x380 */
    	"nccTable1.B"
    };

    /*************************************
     *
     *  Voodoo Banshee I/O space registers
     *
     *************************************/
    
    /* 0x000 */
    static final public int io_status                       = (0x000/4);   /*  */
    static final public int io_pciInit0                     = (0x004/4);   /*  */
    static final public int io_sipMonitor                   = (0x008/4);   /*  */
    static final public int io_lfbMemoryConfig              = (0x00c/4);   /*  */
    static final public int io_miscInit0                    = (0x010/4);   /*  */
    static final public int io_miscInit1                    = (0x014/4);   /*  */
    static final public int io_dramInit0                    = (0x018/4);   /*  */
    static final public int io_dramInit1                    = (0x01c/4);   /*  */
    static final public int io_agpInit                      = (0x020/4);   /*  */
    static final public int io_tmuGbeInit                   = (0x024/4);   /*  */
    static final public int io_vgaInit0                     = (0x028/4);   /*  */
    static final public int io_vgaInit1                     = (0x02c/4);   /*  */
    static final public int io_dramCommand                  = (0x030/4);   /*  */
    static final public int io_dramData                     = (0x034/4);   /*  */
    
    /* 0x040 */
    static final public int io_pllCtrl0                     = (0x040/4);   /*  */
    static final public int io_pllCtrl1                     = (0x044/4);   /*  */
    static final public int io_pllCtrl2                     = (0x048/4);   /*  */
    static final public int io_dacMode                      = (0x04c/4);   /*  */
    static final public int io_dacAddr                      = (0x050/4);   /*  */
    static final public int io_dacData                      = (0x054/4);   /*  */
    static final public int io_rgbMaxDelta                  = (0x058/4);   /*  */
    static final public int io_vidProcCfg                   = (0x05c/4);   /*  */
    static final public int io_hwCurPatAddr                 = (0x060/4);   /*  */
    static final public int io_hwCurLoc                     = (0x064/4);   /*  */
    static final public int io_hwCurC0                      = (0x068/4);   /*  */
    static final public int io_hwCurC1                      = (0x06c/4);   /*  */
    static final public int io_vidInFormat                  = (0x070/4);   /*  */
    static final public int io_vidInStatus                  = (0x074/4);   /*  */
    static final public int io_vidSerialParallelPort        = (0x078/4);   /*  */
    static final public int io_vidInXDecimDeltas            = (0x07c/4);   /*  */
    
    /* 0x080 */
    static final public int io_vidInDecimInitErrs           = (0x080/4);   /*  */
    static final public int io_vidInYDecimDeltas            = (0x084/4);   /*  */
    static final public int io_vidPixelBufThold             = (0x088/4);   /*  */
    static final public int io_vidChromaMin                 = (0x08c/4);   /*  */
    static final public int io_vidChromaMax                 = (0x090/4);   /*  */
    static final public int io_vidCurrentLine               = (0x094/4);   /*  */
    static final public int io_vidScreenSize                = (0x098/4);   /*  */
    static final public int io_vidOverlayStartCoords        = (0x09c/4);   /*  */
    static final public int io_vidOverlayEndScreenCoord     = (0x0a0/4);   /*  */
    static final public int io_vidOverlayDudx               = (0x0a4/4);   /*  */
    static final public int io_vidOverlayDudxOffsetSrcWidth = (0x0a8/4);   /*  */
    static final public int io_vidOverlayDvdy               = (0x0ac/4);   /*  */
    static final public int io_vgab0                        = (0x0b0/4);   /*  */
    static final public int io_vgab4                        = (0x0b4/4);   /*  */
    static final public int io_vgab8                        = (0x0b8/4);   /*  */
    static final public int io_vgabc                        = (0x0bc/4);   /*  */
    
    /* 0x0c0 */
    static final public int io_vgac0                        = (0x0c0/4);   /*  */
    static final public int io_vgac4                        = (0x0c4/4);   /*  */
    static final public int io_vgac8                        = (0x0c8/4);   /*  */
    static final public int io_vgacc                        = (0x0cc/4);   /*  */
    static final public int io_vgad0                        = (0x0d0/4);   /*  */
    static final public int io_vgad4                        = (0x0d4/4);   /*  */
    static final public int io_vgad8                        = (0x0d8/4);   /*  */
    static final public int io_vgadc                        = (0x0dc/4);   /*  */
    static final public int io_vidOverlayDvdyOffset         = (0x0e0/4);   /*  */
    static final public int io_vidDesktopStartAddr          = (0x0e4/4);   /*  */
    static final public int io_vidDesktopOverlayStride      = (0x0e8/4);   /*  */
    static final public int io_vidInAddr0                   = (0x0ec/4);   /*  */
    static final public int io_vidInAddr1                   = (0x0f0/4);   /*  */
    static final public int io_vidInAddr2                   = (0x0f4/4);   /*  */
    static final public int io_vidInStride                  = (0x0f8/4);   /*  */
    static final public int io_vidCurrOverlayStartAddr      = (0x0fc/4);   /*  */
    
    /*************************************
     *
     *  Register constants
     *
     *************************************/
    
    /* Codes to the right:
        R = readable
        W = writeable
        P = pipelined
        F = goes to FIFO
    */
    
    /* 0x000 */
    static final public int status          = (0x000/4);   /* R  P  */
    static final public int intrCtrl        = (0x004/4);   /* RW P   -- Voodoo2/Banshee only */
    static final public int vertexAx        = (0x008/4);   /*  W PF */
    static final public int vertexAy        = (0x00c/4);   /*  W PF */
    static final public int vertexBx        = (0x010/4);   /*  W PF */
    static final public int vertexBy        = (0x014/4);   /*  W PF */
    static final public int vertexCx        = (0x018/4);   /*  W PF */
    static final public int vertexCy        = (0x01c/4);   /*  W PF */
    static final public int startR          = (0x020/4);   /*  W PF */
    static final public int startG          = (0x024/4);   /*  W PF */
    static final public int startB          = (0x028/4);   /*  W PF */
    static final public int startZ          = (0x02c/4);   /*  W PF */
    static final public int startA          = (0x030/4);   /*  W PF */
    static final public int startS          = (0x034/4);   /*  W PF */
    static final public int startT          = (0x038/4);   /*  W PF */
    static final public int startW          = (0x03c/4);   /*  W PF */
    
    /* 0x040 */
    static final public int dRdX            = (0x040/4);   /*  W PF */
    static final public int dGdX            = (0x044/4);   /*  W PF */
    static final public int dBdX            = (0x048/4);   /*  W PF */
    static final public int dZdX            = (0x04c/4);   /*  W PF */
    static final public int dAdX            = (0x050/4);   /*  W PF */
    static final public int dSdX            = (0x054/4);   /*  W PF */
    static final public int dTdX            = (0x058/4);   /*  W PF */
    static final public int dWdX            = (0x05c/4);   /*  W PF */
    static final public int dRdY            = (0x060/4);   /*  W PF */
    static final public int dGdY            = (0x064/4);   /*  W PF */
    static final public int dBdY            = (0x068/4);   /*  W PF */
    static final public int dZdY            = (0x06c/4);   /*  W PF */
    static final public int dAdY            = (0x070/4);   /*  W PF */
    static final public int dSdY            = (0x074/4);   /*  W PF */
    static final public int dTdY            = (0x078/4);   /*  W PF */
    static final public int dWdY            = (0x07c/4);   /*  W PF */
    
    /* 0x080 */
    static final public int triangleCMD     = (0x080/4);   /*  W PF */
    static final public int fvertexAx       = (0x088/4);   /*  W PF */
    static final public int fvertexAy       = (0x08c/4);   /*  W PF */
    static final public int fvertexBx       = (0x090/4);   /*  W PF */
    static final public int fvertexBy       = (0x094/4);   /*  W PF */
    static final public int fvertexCx       = (0x098/4);   /*  W PF */
    static final public int fvertexCy       = (0x09c/4);   /*  W PF */
    static final public int fstartR         = (0x0a0/4);   /*  W PF */
    static final public int fstartG         = (0x0a4/4);   /*  W PF */
    static final public int fstartB         = (0x0a8/4);   /*  W PF */
    static final public int fstartZ         = (0x0ac/4);   /*  W PF */
    static final public int fstartA         = (0x0b0/4);   /*  W PF */
    static final public int fstartS         = (0x0b4/4);   /*  W PF */
    static final public int fstartT         = (0x0b8/4);   /*  W PF */
    static final public int fstartW         = (0x0bc/4);   /*  W PF */
    
    /* 0x0c0 */
    static final public int fdRdX           = (0x0c0/4);   /*  W PF */
    static final public int fdGdX           = (0x0c4/4);   /*  W PF */
    static final public int fdBdX           = (0x0c8/4);   /*  W PF */
    static final public int fdZdX           = (0x0cc/4);   /*  W PF */
    static final public int fdAdX           = (0x0d0/4);   /*  W PF */
    static final public int fdSdX           = (0x0d4/4);   /*  W PF */
    static final public int fdTdX           = (0x0d8/4);   /*  W PF */
    static final public int fdWdX           = (0x0dc/4);   /*  W PF */
    static final public int fdRdY           = (0x0e0/4);   /*  W PF */
    static final public int fdGdY           = (0x0e4/4);   /*  W PF */
    static final public int fdBdY           = (0x0e8/4);   /*  W PF */
    static final public int fdZdY           = (0x0ec/4);   /*  W PF */
    static final public int fdAdY           = (0x0f0/4);   /*  W PF */
    static final public int fdSdY           = (0x0f4/4);   /*  W PF */
    static final public int fdTdY           = (0x0f8/4);   /*  W PF */
    static final public int fdWdY           = (0x0fc/4);   /*  W PF */
    
    /* 0x100 */
    static final public int ftriangleCMD    = (0x100/4);   /*  W PF */
    static final public int fbzColorPath    = (0x104/4);   /* RW PF */
    static final public int fogMode         = (0x108/4);   /* RW PF */
    static final public int alphaMode       = (0x10c/4);   /* RW PF */
    static final public int fbzMode         = (0x110/4);   /* RW  F */
    static final public int lfbMode         = (0x114/4);   /* RW  F */
    static final public int clipLeftRight   = (0x118/4);   /* RW  F */
    static final public int clipLowYHighY   = (0x11c/4);   /* RW  F */
    static final public int nopCMD          = (0x120/4);   /*  W  F */
    static final public int fastfillCMD     = (0x124/4);   /*  W  F */
    static final public int swapbufferCMD   = (0x128/4);   /*  W  F */
    static final public int fogColor        = (0x12c/4);   /*  W  F */
    static final public int zaColor         = (0x130/4);   /*  W  F */
    static final public int chromaKey       = (0x134/4);   /*  W  F */
    static final public int chromaRange     = (0x138/4);   /*  W  F  -- Voodoo2/Banshee only */
    static final public int userIntrCMD     = (0x13c/4);   /*  W  F  -- Voodoo2/Banshee only */
    
    /* 0x140 */
    static final public int stipple         = (0x140/4);   /* RW  F */
    static final public int color0          = (0x144/4);   /* RW  F */
    static final public int color1          = (0x148/4);   /* RW  F */
    static final public int fbiPixelsIn     = (0x14c/4);   /* R     */
    static final public int fbiChromaFail   = (0x150/4);   /* R     */
    static final public int fbiZfuncFail    = (0x154/4);   /* R     */
    static final public int fbiAfuncFail    = (0x158/4);   /* R     */
    static final public int fbiPixelsOut    = (0x15c/4);   /* R     */
    static final public int fogTable        = (0x160/4);   /*  W  F */
    
    /* 0x1c0 */
    static final public int cmdFifoBaseAddr = (0x1e0/4);   /* RW     -- Voodoo2 only */
    static final public int cmdFifoBump     = (0x1e4/4);   /* RW     -- Voodoo2 only */
    static final public int cmdFifoRdPtr    = (0x1e8/4);   /* RW     -- Voodoo2 only */
    static final public int cmdFifoAMin     = (0x1ec/4);   /* RW     -- Voodoo2 only */
    static final public int colBufferAddr   = (0x1ec/4);   /* RW     -- Banshee only */
    static final public int cmdFifoAMax     = (0x1f0/4);   /* RW     -- Voodoo2 only */
    static final public int colBufferStride = (0x1f0/4);   /* RW     -- Banshee only */
    static final public int cmdFifoDepth    = (0x1f4/4);   /* RW     -- Voodoo2 only */
    static final public int auxBufferAddr   = (0x1f4/4);   /* RW     -- Banshee only */
    static final public int cmdFifoHoles    = (0x1f8/4);   /* RW     -- Voodoo2 only */
    static final public int auxBufferStride = (0x1f8/4);   /* RW     -- Banshee only */
    
    /* 0x200 */
    static final public int fbiInit4        = (0x200/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int clipLeftRight1  = (0x200/4);   /* RW     -- Banshee only */
    static final public int vRetrace        = (0x204/4);   /* R      -- Voodoo/Voodoo2 only */
    static final public int clipTopBottom1  = (0x204/4);   /* RW     -- Banshee only */
    static final public int backPorch       = (0x208/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int videoDimensions = (0x20c/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int fbiInit0        = (0x210/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int fbiInit1        = (0x214/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int fbiInit2        = (0x218/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int fbiInit3        = (0x21c/4);   /* RW     -- Voodoo/Voodoo2 only */
    static final public int hSync           = (0x220/4);   /*  W     -- Voodoo/Voodoo2 only */
    static final public int vSync           = (0x224/4);   /*  W     -- Voodoo/Voodoo2 only */
    static final public int clutData        = (0x228/4);   /*  W  F  -- Voodoo/Voodoo2 only */
    static final public int dacData         = (0x22c/4);   /*  W     -- Voodoo/Voodoo2 only */
    static final public int maxRgbDelta     = (0x230/4);   /*  W     -- Voodoo/Voodoo2 only */
    static final public int hBorder         = (0x234/4);   /*  W     -- Voodoo2 only */
    static final public int vBorder         = (0x238/4);   /*  W     -- Voodoo2 only */
    static final public int borderColor     = (0x23c/4);   /*  W     -- Voodoo2 only */
    
    /* 0x240 */
    static final public int hvRetrace       = (0x240/4);   /* R      -- Voodoo2 only */
    static final public int fbiInit5        = (0x244/4);   /* RW     -- Voodoo2 only */
    static final public int fbiInit6        = (0x248/4);   /* RW     -- Voodoo2 only */
    static final public int fbiInit7        = (0x24c/4);   /* RW     -- Voodoo2 only */
    static final public int swapPending     = (0x24c/4);   /*  W     -- Banshee only */
    static final public int leftOverlayBuf  = (0x250/4);   /*  W     -- Banshee only */
    static final public int rightOverlayBuf = (0x254/4);   /*  W     -- Banshee only */
    static final public int fbiSwapHistory  = (0x258/4);   /* R      -- Voodoo2/Banshee only */
    static final public int fbiTrianglesOut = (0x25c/4);   /* R      -- Voodoo2/Banshee only */
    static final public int sSetupMode      = (0x260/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sVx             = (0x264/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sVy             = (0x268/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sARGB           = (0x26c/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sRed            = (0x270/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sGreen          = (0x274/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sBlue           = (0x278/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sAlpha          = (0x27c/4);   /*  W PF  -- Voodoo2/Banshee only */
    
    /* 0x280 */
    static final public int sVz             = (0x280/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sWb             = (0x284/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sWtmu0          = (0x288/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sS_W0           = (0x28c/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sT_W0           = (0x290/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sWtmu1          = (0x294/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sS_Wtmu1        = (0x298/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sT_Wtmu1        = (0x29c/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sDrawTriCMD     = (0x2a0/4);   /*  W PF  -- Voodoo2/Banshee only */
    static final public int sBeginTriCMD    = (0x2a4/4);   /*  W PF  -- Voodoo2/Banshee only */
    
    /* 0x2c0 */
    static final public int bltSrcBaseAddr  = (0x2c0/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltDstBaseAddr  = (0x2c4/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltXYStrides    = (0x2c8/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltSrcChromaRange = (0x2cc/4); /* RW PF  -- Voodoo2 only */
    static final public int bltDstChromaRange = (0x2d0/4); /* RW PF  -- Voodoo2 only */
    static final public int bltClipX        = (0x2d4/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltClipY        = (0x2d8/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltSrcXY        = (0x2e0/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltDstXY        = (0x2e4/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltSize         = (0x2e8/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltRop          = (0x2ec/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltColor        = (0x2f0/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltCommand      = (0x2f8/4);   /* RW PF  -- Voodoo2 only */
    static final public int bltData         = (0x2fc/4);   /*  W PF  -- Voodoo2 only */
    
    /* 0x300 */
    static final public int textureMode     = (0x300/4);   /*  W PF */
    static final public int tLOD            = (0x304/4);   /*  W PF */
    static final public int tDetail         = (0x308/4);   /*  W PF */
    static final public int texBaseAddr     = (0x30c/4);   /*  W PF */
    static final public int texBaseAddr_1   = (0x310/4);   /*  W PF */
    static final public int texBaseAddr_2   = (0x314/4);   /*  W PF */
    static final public int texBaseAddr_3_8 = (0x318/4);   /*  W PF */
    static final public int trexInit0       = (0x31c/4);   /*  W  F  -- Voodoo/Voodoo2 only */
    static final public int trexInit1       = (0x320/4);   /*  W  F */
    static final public int nccTable        = (0x324/4);   /*  W  F */
    
    
    
    // 2D registers
    static final public int banshee2D_clip0Min          = (0x008/4);
    static final public int banshee2D_clip0Max          = (0x00c/4);
    static final public int banshee2D_dstBaseAddr       = (0x010/4);
    static final public int banshee2D_dstFormat         = (0x014/4);
    static final public int banshee2D_srcColorkeyMin    = (0x018/4);
    static final public int banshee2D_srcColorkeyMax    = (0x01c/4);
    static final public int banshee2D_dstColorkeyMin    = (0x020/4);
    static final public int banshee2D_dstColorkeyMax    = (0x024/4);
    static final public int banshee2D_bresError0        = (0x028/4);
    static final public int banshee2D_bresError1        = (0x02c/4);
    static final public int banshee2D_rop               = (0x030/4);
    static final public int banshee2D_srcBaseAddr       = (0x034/4);
    static final public int banshee2D_commandExtra      = (0x038/4);
    static final public int banshee2D_lineStipple       = (0x03c/4);
    static final public int banshee2D_lineStyle         = (0x040/4);
    static final public int banshee2D_pattern0Alias     = (0x044/4);
    static final public int banshee2D_pattern1Alias     = (0x048/4);
    static final public int banshee2D_clip1Min          = (0x04c/4);
    static final public int banshee2D_clip1Max          = (0x050/4);
    static final public int banshee2D_srcFormat         = (0x054/4);
    static final public int banshee2D_srcSize           = (0x058/4);
    static final public int banshee2D_srcXY             = (0x05c/4);
    static final public int banshee2D_colorBack         = (0x060/4);
    static final public int banshee2D_colorFore         = (0x064/4);
    static final public int banshee2D_dstSize           = (0x068/4);
    static final public int banshee2D_dstXY             = (0x06c/4);
    static final public int banshee2D_command           = (0x070/4);
        
    /*************************************
     *
     *  Alias map of the first 64
     *  registers when remapped
     *
     *************************************/

    static private final byte[] register_alias_map = new byte[]
    {
    	status,     0x004/4,    vertexAx,   vertexAy,
    	vertexBx,   vertexBy,   vertexCx,   vertexCy,
    	startR,     dRdX,       dRdY,       startG,
    	dGdX,       dGdY,       startB,     dBdX,
    	dBdY,       startZ,     dZdX,       dZdY,
    	startA,     dAdX,       dAdY,       startS,
    	dSdX,       dSdY,       startT,     dTdX,
    	dTdY,       startW,     dWdX,       dWdY,

    	triangleCMD,0x084/4,    fvertexAx,  fvertexAy,
    	fvertexBx,  fvertexBy,  fvertexCx,  fvertexCy,
    	fstartR,    fdRdX,      fdRdY,      fstartG,
    	fdGdX,      fdGdY,      fstartB,    fdBdX,
    	fdBdY,      fstartZ,    fdZdX,      fdZdY,
    	fstartA,    fdAdX,      fdAdY,      fstartS,
    	fdSdX,      fdSdY,      fstartT,    fdTdX,
    	fdTdY,      fstartW,    fdWdX,      fdWdY
    };



    /*************************************
     *
     *  Table of per-register access rights
     *
     *************************************/
    static final byte[] voodoo_register_access = new byte[]
    {
    	/* 0x000 */
    	REG_RP,     0,          REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x040 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x080 */
    	REG_WPF,    0,          REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x0c0 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x100 */
    	REG_WPF,    REG_RWPF,   REG_RWPF,   REG_RWPF,
    	REG_RWF,    REG_RWF,    REG_RWF,    REG_RWF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     0,          0,

    	/* 0x140 */
    	REG_RWF,    REG_RWF,    REG_RWF,    REG_R,
    	REG_R,      REG_R,      REG_R,      REG_R,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x180 */
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x1c0 */
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	0,          0,          0,          0,
    	0,          0,          0,          0,

    	/* 0x200 */
    	REG_RW,     REG_R,      REG_RW,     REG_RW,
    	REG_RW,     REG_RW,     REG_RW,     REG_RW,
    	REG_W,      REG_W,      REG_W,      REG_W,
    	REG_W,      0,          0,          0,

    	/* 0x240 */
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,

    	/* 0x280 */
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,

    	/* 0x2c0 */
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,

    	/* 0x300 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x340 */
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x380 */
    	REG_WF,     0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,

        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
    };

    static final byte[] voodoo2_register_access = new byte[]
    {
        /* 0x000 */
        REG_RP,     REG_RWPT,   REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

        /* 0x040 */
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

        /* 0x080 */
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

        /* 0x0c0 */
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

        /* 0x100 */
        REG_WPF,    REG_RWPF,   REG_RWPF,   REG_RWPF,
        REG_RWF,    REG_RWF,    REG_RWF,    REG_RWF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,

        /* 0x140 */
        REG_RWF,    REG_RWF,    REG_RWF,    REG_R,
        REG_R,      REG_R,      REG_R,      REG_R,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,

        /* 0x180 */
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,

        /* 0x1c0 */
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_RWT,    REG_RWT,    REG_RWT,    REG_RWT,
        REG_RWT,    REG_RWT,    REG_RWT,    REG_RW,

        /* 0x200 */
        REG_RWT,    REG_R,      REG_RWT,    REG_RWT,
        REG_RWT,    REG_RWT,    REG_RWT,    REG_RWT,
        REG_WT,     REG_WT,     REG_WF,     REG_WT,
        REG_WT,     REG_WT,     REG_WT,     REG_WT,

        /* 0x240 */
        REG_R,      REG_RWT,    REG_RWT,    REG_RWT,
        0,          0,          REG_R,      REG_R,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

        /* 0x280 */
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    0,          0,
        0,          0,          0,          0,

        /* 0x2c0 */
        REG_RWPF,   REG_RWPF,   REG_RWPF,   REG_RWPF,
        REG_RWPF,   REG_RWPF,   REG_RWPF,   REG_RWPF,
        REG_RWPF,   REG_RWPF,   REG_RWPF,   REG_RWPF,
        REG_RWPF,   REG_RWPF,   REG_RWPF,   REG_WPF,

        /* 0x300 */
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
        REG_WPF,    REG_WPF,    REG_WPF,    REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,

        /* 0x340 */
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,
        REG_WF,     REG_WF,     REG_WF,     REG_WF,

        /* 0x380 */
        REG_WF,     0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,

        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
    };

    static final private byte[] banshee_register_access = new byte[]
    {
    	/* 0x000 */
    	REG_RP,     REG_RWPT,   REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x040 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x080 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x0c0 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x100 */
    	REG_WPF,    REG_RWPF,   REG_RWPF,   REG_RWPF,
    	REG_RWF,    REG_RWF,    REG_RWF,    REG_RWF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x140 */
    	REG_RWF,    REG_RWF,    REG_RWF,    REG_R,
    	REG_R,      REG_R,      REG_R,      REG_R,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x180 */
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x1c0 */
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	0,          0,          0,          REG_RWF,
    	REG_RWF,    REG_RWF,    REG_RWF,    0,

    	/* 0x200 */
    	REG_RWF,    REG_RWF,    0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,

    	/* 0x240 */
    	0,          0,          0,          REG_WT,
    	REG_RWF,    REG_RWF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_R,      REG_R,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,

    	/* 0x280 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    0,          0,
    	0,          0,          0,          0,

    	/* 0x2c0 */
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,
    	0,          0,          0,          0,

    	/* 0x300 */
    	REG_WPF,    REG_WPF,    REG_WPF,    REG_WPF,
    	REG_WPF,    REG_WPF,    REG_WPF,    0,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x340 */
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,
    	REG_WF,     REG_WF,     REG_WF,     REG_WF,

    	/* 0x380 */
    	REG_WF,     0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,

        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
        0,          0,          0,          0,
    };

    static boolean INITEN_ENABLE_HW_INIT(int val) { return (((val) >> 0) & 1)!=0;}
    static boolean INITEN_ENABLE_PCI_FIFO(int val) { return (((val) >> 1) & 1)!=0;}
    static boolean INITEN_REMAP_INIT_TO_DAC(int val) { return (((val) >> 2) & 1)!=0;}
    static boolean INITEN_ENABLE_SNOOP0(int val) { return (((val) >> 4) & 1)!=0;}
    static boolean INITEN_SNOOP0_MEMORY_MATCH(int val) { return (((val) >> 5) & 1)!=0;}
    static boolean INITEN_SNOOP0_READWRITE_MATCH(int val) { return (((val) >> 6) & 1)!=0;}
    static boolean INITEN_ENABLE_SNOOP1(int val) { return (((val) >> 7) & 1)!=0;}
    static boolean INITEN_SNOOP1_MEMORY_MATCH(int val) { return (((val) >> 8) & 1)!=0;}
    static boolean INITEN_SNOOP1_READWRITE_MATCH(int val) { return (((val) >> 9) & 1)!=0;}
    static boolean INITEN_SLI_BUS_OWNER(int val) { return (((val) >> 10) & 1)!=0;}
    static boolean INITEN_SLI_ODD_EVEN(int val) { return (((val) >> 11) & 1)!=0;}
    static int INITEN_SECONDARY_REV_ID(int val) { return (((val) >> 12) & 0xf);}   /* voodoo 2 only */
    static int INITEN_MFCTR_FAB_ID(int val) { return (((val) >> 16) & 0xf);}   /* voodoo 2 only */
    static boolean INITEN_ENABLE_PCI_INTERRUPT(int val) { return (((val) >> 20) & 1)!=0;}     /* voodoo 2 only */
    static boolean INITEN_PCI_INTERRUPT_TIMEOUT(int val) { return (((val) >> 21) & 1)!=0;}     /* voodoo 2 only */
    static boolean INITEN_ENABLE_NAND_TREE_TEST(int val) { return (((val) >> 22) & 1)!=0;}     /* voodoo 2 only */
    static boolean INITEN_ENABLE_SLI_ADDRESS_SNOOP(int val) { return (((val) >> 23) & 1)!=0;}    /* voodoo 2 only */
    static int INITEN_SLI_SNOOP_ADDRESS(int val) { return (((val) >> 24) & 0xff);}  /* voodoo 2 only */

    static int FBZCP_CC_RGBSELECT(int val)             { return (((val) >> 0) & 3);}
    static int FBZCP_CC_ASELECT(int val)               { return (((val) >> 2) & 3);}
    static boolean FBZCP_CC_LOCALSELECT(int val)           { return (((val) >> 4) & 1)!=0;}
    static int FBZCP_CCA_LOCALSELECT(int val)          { return (((val) >> 5) & 3);}
    static boolean FBZCP_CC_LOCALSELECT_OVERRIDE(int val)  { return (((val) >> 7) & 1)!=0;}
    static boolean FBZCP_CC_ZERO_OTHER(int val)            { return (((val) >> 8) & 1)!=0;}
    static boolean FBZCP_CC_SUB_CLOCAL(int val)            { return (((val) >> 9) & 1)!=0;}
    static int FBZCP_CC_MSELECT(int val)               { return (((val) >> 10) & 7);}
    static boolean FBZCP_CC_REVERSE_BLEND(int val)         { return (((val) >> 13) & 1)!=0;}
    static int FBZCP_CC_ADD_ACLOCAL(int val)           { return (((val) >> 14) & 3);}
    static boolean FBZCP_CC_INVERT_OUTPUT(int val)         { return (((val) >> 16) & 1)!=0;}
    static boolean FBZCP_CCA_ZERO_OTHER(int val)           { return (((val) >> 17) & 1)!=0;}
    static boolean FBZCP_CCA_SUB_CLOCAL(int val)           { return (((val) >> 18) & 1)!=0;}
    static int FBZCP_CCA_MSELECT(int val)              { return (((val) >> 19) & 7);}
    static boolean FBZCP_CCA_REVERSE_BLEND(int val)        { return (((val) >> 22) & 1)!=0;}
    static int FBZCP_CCA_ADD_ACLOCAL(int val)          { return (((val) >> 23) & 3);}
    static boolean FBZCP_CCA_INVERT_OUTPUT(int val)        { return (((val) >> 25) & 1)!=0;}
    static boolean FBZCP_CCA_SUBPIXEL_ADJUST(int val)      { return (((val) >> 26) & 1)!=0;}
    static boolean FBZCP_TEXTURE_ENABLE(int val)           { return (((val) >> 27) & 1)!=0;}
    static boolean FBZCP_RGBZW_CLAMP(int val)              { return (((val) >> 28) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBZCP_ANTI_ALIAS(int val)               { return (((val) >> 29) & 1)!=0;}     /* voodoo 2 only */
    
    static boolean ALPHAMODE_ALPHATEST(int val)            { return (((val) >> 0) & 1)!=0;}
    static int ALPHAMODE_ALPHAFUNCTION(int val)        { return (((val) >> 1) & 7);}
    static boolean ALPHAMODE_ALPHABLEND(int val)           { return (((val) >> 4) & 1)!=0;}
    static boolean ALPHAMODE_ANTIALIAS(int val)            { return (((val) >> 5) & 1)!=0;}
    static int ALPHAMODE_SRCRGBBLEND(int val)          { return (((val) >> 8) & 15);}
    static int ALPHAMODE_DSTRGBBLEND(int val)          { return (((val) >> 12) & 15);}
    static int ALPHAMODE_SRCALPHABLEND(int val)        { return (((val) >> 16) & 15);}
    static int ALPHAMODE_DSTALPHABLEND(int val)        { return (((val) >> 20) & 15);}
    static int ALPHAMODE_ALPHAREF(int val)             { return (((val) >> 24) & 0xff);}
    
    static boolean FOGMODE_ENABLE_FOG(int val)             { return (((val) >> 0) & 1)!=0;}
    static boolean FOGMODE_FOG_ADD(int val)                { return (((val) >> 1) & 1)!=0;}
    static boolean FOGMODE_FOG_MULT(int val)               { return (((val) >> 2) & 1)!=0;}
    static int FOGMODE_FOG_ZALPHA(int val)             { return (((val) >> 3) & 3);}
    static boolean FOGMODE_FOG_CONSTANT(int val)           { return (((val) >> 5) & 1)!=0;}
    static boolean FOGMODE_FOG_DITHER(int val)             { return (((val) >> 6) & 1)!=0;}      /* voodoo 2 only */
    static boolean FOGMODE_FOG_ZONES(int val)              { return (((val) >> 7) & 1)!=0;}      /* voodoo 2 only */
    
    static boolean FBZMODE_ENABLE_CLIPPING(int val)        { return (((val) >> 0) & 1)!=0;}
    static boolean FBZMODE_ENABLE_CHROMAKEY(int val)       { return (((val) >> 1) & 1)!=0;}
    static boolean FBZMODE_ENABLE_STIPPLE(int val)         { return (((val) >> 2) & 1)!=0;}
    static boolean FBZMODE_WBUFFER_SELECT(int val)         { return (((val) >> 3) & 1)!=0;}
    static boolean FBZMODE_ENABLE_DEPTHBUF(int val)        { return (((val) >> 4) & 1)!=0;}
    static int FBZMODE_DEPTH_FUNCTION(int val)         { return (((val) >> 5) & 7);}
    static boolean FBZMODE_ENABLE_DITHERING(int val)       { return (((val) >> 8) & 1)!=0;}
    static boolean FBZMODE_RGB_BUFFER_MASK(int val)        { return (((val) >> 9) & 1)!=0;}
    static boolean FBZMODE_AUX_BUFFER_MASK(int val)        { return (((val) >> 10) & 1)!=0;}
    static boolean FBZMODE_DITHER_TYPE(int val)            { return (((val) >> 11) & 1)!=0;}
    static boolean FBZMODE_STIPPLE_PATTERN(int val)        { return (((val) >> 12) & 1)!=0;}
    static boolean FBZMODE_ENABLE_ALPHA_MASK(int val)      { return (((val) >> 13) & 1)!=0;}
    static int FBZMODE_DRAW_BUFFER(int val)            { return (((val) >> 14) & 3);}
    static boolean FBZMODE_ENABLE_DEPTH_BIAS(int val)      { return (((val) >> 16) & 1)!=0;}
    static boolean FBZMODE_Y_ORIGIN(int val)               { return (((val) >> 17) & 1)!=0;}
    static boolean FBZMODE_ENABLE_ALPHA_PLANES(int val)    { return (((val) >> 18) & 1)!=0;}
    static boolean FBZMODE_ALPHA_DITHER_SUBTRACT(int val)  { return (((val) >> 19) & 1)!=0;}
    static boolean FBZMODE_DEPTH_SOURCE_COMPARE(int val)   { return (((val) >> 20) & 1)!=0;}
    static boolean FBZMODE_DEPTH_FLOAT_SELECT(int val)     { return (((val) >> 21) & 1)!=0;}     /* voodoo 2 only */
    
    static int LFBMODE_WRITE_FORMAT(int val)           { return (((val) >> 0) & 0xf);}
    static int LFBMODE_WRITE_BUFFER_SELECT(int val)    { return (((val) >> 4) & 3);}
    static int LFBMODE_READ_BUFFER_SELECT(int val)     { return (((val) >> 6) & 3);}
    static boolean LFBMODE_ENABLE_PIXEL_PIPELINE(int val)  { return (((val) >> 8) & 1)!=0;}
    static int LFBMODE_RGBA_LANES(int val)             { return (((val) >> 9) & 3);}
    static boolean LFBMODE_WORD_SWAP_WRITES(int val)       { return (((val) >> 11) & 1)!=0;}
    static boolean LFBMODE_BYTE_SWIZZLE_WRITES(int val)    { return (((val) >> 12) & 1)!=0;}
    static boolean LFBMODE_Y_ORIGIN(int val)               { return (((val) >> 13) & 1)!=0;}
    static boolean LFBMODE_WRITE_W_SELECT(int val)         { return (((val) >> 14) & 1)!=0;}
    static boolean LFBMODE_WORD_SWAP_READS(int val)        { return (((val) >> 15) & 1)!=0;}
    static boolean LFBMODE_BYTE_SWIZZLE_READS(int val)     { return (((val) >> 16) & 1)!=0;}
    
    static int CHROMARANGE_BLUE_EXCLUSIVE(int val)     { return (((val) >> 24) & 1);}
    static int CHROMARANGE_GREEN_EXCLUSIVE(int val)    { return (((val) >> 25) & 1);}
    static int CHROMARANGE_RED_EXCLUSIVE(int val)      { return (((val) >> 26) & 1);}
    static boolean CHROMARANGE_UNION_MODE(int val)         { return (((val) >> 27) & 1)!=0;}
    static boolean CHROMARANGE_ENABLE(int val)             { return (((val) >> 28) & 1)!=0;}
    
    static boolean FBIINIT0_VGA_PASSTHRU(int val)          { return (((val) >> 0) & 1)!=0;}
    static boolean FBIINIT0_GRAPHICS_RESET(int val)        { return (((val) >> 1) & 1)!=0;}
    static boolean FBIINIT0_FIFO_RESET(int val)            { return (((val) >> 2) & 1)!=0;}
    static boolean FBIINIT0_SWIZZLE_REG_WRITES(int val)    { return (((val) >> 3) & 1)!=0;}
    static boolean FBIINIT0_STALL_PCIE_FOR_HWM(int val)    { return (((val) >> 4) & 1)!=0;}
    static int FBIINIT0_PCI_FIFO_LWM(int val)          { return (((val) >> 6) & 0x1f);}
    static boolean FBIINIT0_LFB_TO_MEMORY_FIFO(int val)    { return (((val) >> 11) & 1)!=0;}
    static boolean FBIINIT0_TEXMEM_TO_MEMORY_FIFO(int val) { return (((val) >> 12) & 1)!=0;}
    static boolean FBIINIT0_ENABLE_MEMORY_FIFO(int val)    { return (((val) >> 13) & 1)!=0;}
    static int FBIINIT0_MEMORY_FIFO_HWM(int val)       { return (((val) >> 14) & 0x7ff);}
    static int FBIINIT0_MEMORY_FIFO_BURST(int val)     { return (((val) >> 25) & 0x3f);}
    
    static boolean FBIINIT1_PCI_DEV_FUNCTION(int val)       { return (((val) >> 0) & 1)!=0;}
    static boolean FBIINIT1_PCI_WRITE_WAIT_STATES(int val)  { return (((val) >> 1) & 1)!=0;}
    static boolean FBIINIT1_MULTI_SST1(int val)             { return (((val) >> 2) & 1)!=0;}      /* not on voodoo 2 */
    static boolean FBIINIT1_ENABLE_LFB(int val)             { return (((val) >> 3) & 1)!=0;}
    static int FBIINIT1_X_VIDEO_TILES(int val)          { return (((val) >> 4) & 0xf);}
    static boolean FBIINIT1_VIDEO_TIMING_RESET(int val)     { return (((val) >> 8) & 1)!=0;}
    static boolean FBIINIT1_SOFTWARE_OVERRIDE(int val)      { return (((val) >> 9) & 1)!=0;}
    static boolean FBIINIT1_SOFTWARE_HSYNC(int val)         { return (((val) >> 10) & 1)!=0;}
    static boolean FBIINIT1_SOFTWARE_VSYNC(int val)         { return (((val) >> 11) & 1)!=0;}
    static boolean FBIINIT1_SOFTWARE_BLANK(int val)         { return (((val) >> 12) & 1)!=0;}
    static boolean FBIINIT1_DRIVE_VIDEO_TIMING(int val)     { return (((val) >> 13) & 1)!=0;}
    static boolean FBIINIT1_DRIVE_VIDEO_BLANK(int val)      { return (((val) >> 14) & 1)!=0;}
    static boolean FBIINIT1_DRIVE_VIDEO_SYNC(int val)       { return (((val) >> 15) & 1)!=0;}
    static boolean FBIINIT1_DRIVE_VIDEO_DCLK(int val)       { return (((val) >> 16) & 1)!=0;}
    static boolean FBIINIT1_VIDEO_TIMING_VCLK(int val)      { return (((val) >> 17) & 1)!=0;}
    static int FBIINIT1_VIDEO_CLK_2X_DELAY(int val)     { return (((val) >> 18) & 3);}
    static int FBIINIT1_VIDEO_TIMING_SOURCE(int val)    { return (((val) >> 20) & 3);}
    static boolean FBIINIT1_ENABLE_24BPP_OUTPUT(int val)    { return (((val) >> 22) & 1)!=0;}
    static boolean FBIINIT1_ENABLE_SLI(int val)             { return (((val) >> 23) & 1)!=0;}
    static boolean FBIINIT1_X_VIDEO_TILES_BIT5(int val)     { return (((val) >> 24) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT1_ENABLE_EDGE_FILTER(int val)     { return (((val) >> 25) & 1)!=0;}
    static boolean FBIINIT1_INVERT_VID_CLK_2X(int val)      { return (((val) >> 26) & 1)!=0;}
    static int FBIINIT1_VID_CLK_2X_SEL_DELAY(int val)   { return (((val) >> 27) & 3);}
    static int FBIINIT1_VID_CLK_DELAY(int val)          { return (((val) >> 29) & 3);}
    static boolean FBIINIT1_DISABLE_FAST_READAHEAD(int val) { return (((val) >> 31) & 1)!=0;}
    
    static boolean FBIINIT2_DISABLE_DITHER_SUB(int val)    { return (((val) >> 0) & 1)!=0;}
    static boolean FBIINIT2_DRAM_BANKING(int val)          { return (((val) >> 1) & 1)!=0;}
    static int FBIINIT2_ENABLE_TRIPLE_BUF(int val)     { return (((val) >> 4) & 1);}
    static boolean FBIINIT2_ENABLE_FAST_RAS_READ(int val)  { return (((val) >> 5) & 1)!=0;}
    static boolean FBIINIT2_ENABLE_GEN_DRAM_OE(int val)    { return (((val) >> 6) & 1)!=0;}
    static boolean FBIINIT2_ENABLE_FAST_READWRITE(int val) { return (((val) >> 7) & 1)!=0;}
    static boolean FBIINIT2_ENABLE_PASSTHRU_DITHER(int val) { return (((val) >> 8) & 1)!=0;}
    static int FBIINIT2_SWAP_BUFFER_ALGORITHM(int val) { return (((val) >> 9) & 3);}
    static int FBIINIT2_VIDEO_BUFFER_OFFSET(int val)   { return (((val) >> 11) & 0x1ff);}
    static boolean FBIINIT2_ENABLE_DRAM_BANKING(int val)   { return (((val) >> 20) & 1)!=0;}
    static boolean FBIINIT2_ENABLE_DRAM_READ_FIFO(int val) { return (((val) >> 21) & 1)!=0;}
    static boolean FBIINIT2_ENABLE_DRAM_REFRESH(int val)   { return (((val) >> 22) & 1)!=0;}
    static int FBIINIT2_REFRESH_LOAD_VALUE(int val)    { return (((val) >> 23) & 0x1ff);}
    
    static boolean FBIINIT3_TRI_REGISTER_REMAP(int val)    { return (((val) >> 0) & 1)!=0;}
    static int FBIINIT3_VIDEO_FIFO_THRESH(int val)     { return (((val) >> 1) & 0x1f);}
    static boolean FBIINIT3_DISABLE_TMUS(int val)          { return (((val) >> 6) & 1)!=0;}
    static int FBIINIT3_FBI_MEMORY_TYPE(int val)       { return (((val) >> 8) & 7);}
    static boolean FBIINIT3_VGA_PASS_RESET_VAL(int val)    { return (((val) >> 11) & 1)!=0;}
    static boolean FBIINIT3_HARDCODE_PCI_BASE(int val)     { return (((val) >> 12) & 1)!=0;}
    static int FBIINIT3_FBI2TREX_DELAY(int val)        { return (((val) >> 13) & 0xf);}
    static int FBIINIT3_TREX2FBI_DELAY(int val)        { return (((val) >> 17) & 0x1f);}
    static int FBIINIT3_YORIGIN_SUBTRACT(int val)      { return (((val) >> 22) & 0x3ff);}
    
    static boolean FBIINIT4_PCI_READ_WAITS(int val) { return (((val) >> 0) & 1)!=0;}
    static boolean FBIINIT4_ENABLE_LFB_READAHEAD(int val) { return (((val) >> 1) & 1)!=0;}
    static int FBIINIT4_MEMORY_FIFO_LWM(int val) { return (((val) >> 2) & 0x3f);}
    static int FBIINIT4_MEMORY_FIFO_START_ROW(int val) { return (((val) >> 8) & 0x3ff);}
    static int FBIINIT4_MEMORY_FIFO_STOP_ROW(int val) { return (((val) >> 18) & 0x3ff);}
    static int FBIINIT4_VIDEO_CLOCKING_DELAY(int val) { return (((val) >> 29) & 7);}     /* voodoo 2 only */
   
    static boolean FBIINIT5_DISABLE_PCI_STOP(int val)      { return (((val) >> 0) & 1)!=0;}      /* voodoo 2 only */
    static boolean FBIINIT5_PCI_SLAVE_SPEED(int val)       { return (((val) >> 1) & 1)!=0;}      /* voodoo 2 only */
    static boolean FBIINIT5_DAC_DATA_OUTPUT_WIDTH(int val) { return (((val) >> 2) & 1)!=0;}      /* voodoo 2 only */
    static boolean FBIINIT5_DAC_DATA_17_OUTPUT(int val)    { return (((val) >> 3) & 1)!=0;}      /* voodoo 2 only */
    static boolean FBIINIT5_DAC_DATA_18_OUTPUT(int val)    { return (((val) >> 4) & 1)!=0;}      /* voodoo 2 only */
    static int FBIINIT5_GENERIC_STRAPPING(int val)     { return (((val) >> 5) & 0xf);}    /* voodoo 2 only */
    static int FBIINIT5_BUFFER_ALLOCATION(int val)     { return (((val) >> 9) & 3);}      /* voodoo 2 only */
    static boolean FBIINIT5_DRIVE_VID_CLK_SLAVE(int val)   { return (((val) >> 11) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_DRIVE_DAC_DATA_16(int val)     { return (((val) >> 12) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_VCLK_INPUT_SELECT(int val)     { return (((val) >> 13) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_MULTI_CVG_DETECT(int val)      { return (((val) >> 14) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_SYNC_RETRACE_READS(int val)    { return (((val) >> 15) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_RHBORDER_COLOR(int val) { return (((val) >> 16) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_LHBORDER_COLOR(int val) { return (((val) >> 17) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_BVBORDER_COLOR(int val) { return (((val) >> 18) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_TVBORDER_COLOR(int val) { return (((val) >> 19) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_DOUBLE_HORIZ(int val)          { return (((val) >> 20) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_DOUBLE_VERT(int val)           { return (((val) >> 21) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_16BIT_GAMMA(int val)    { return (((val) >> 22) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_INVERT_DAC_HSYNC(int val)      { return (((val) >> 23) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_INVERT_DAC_VSYNC(int val)      { return (((val) >> 24) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_24BIT_DACDATA(int val)  { return (((val) >> 25) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_ENABLE_INTERLACING(int val)    { return (((val) >> 26) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT5_DAC_DATA_18_CONTROL(int val)   { return (((val) >> 27) & 1)!=0;}     /* voodoo 2 only */
    static int FBIINIT5_RASTERIZER_UNIT_MODE(int val)  { return (((val) >> 30) & 3);}     /* voodoo 2 only */
    
    static int FBIINIT6_WINDOW_ACTIVE_COUNTER(int val) { return (((val) >> 0) & 7);}      /* voodoo 2 only */
    static int FBIINIT6_WINDOW_DRAG_COUNTER(int val)   { return (((val) >> 3) & 0x1f);}   /* voodoo 2 only */
    static boolean FBIINIT6_SLI_SYNC_MASTER(int val)       { return (((val) >> 8) & 1)!=0;}      /* voodoo 2 only */
    static int FBIINIT6_DAC_DATA_22_OUTPUT(int val)    { return (((val) >> 9) & 3);}      /* voodoo 2 only */
    static int FBIINIT6_DAC_DATA_23_OUTPUT(int val)    { return (((val) >> 11) & 3);}     /* voodoo 2 only */
    static int FBIINIT6_SLI_SYNCIN_OUTPUT(int val)     { return (((val) >> 13) & 3);}     /* voodoo 2 only */
    static int FBIINIT6_SLI_SYNCOUT_OUTPUT(int val)    { return (((val) >> 15) & 3);}     /* voodoo 2 only */
    static int FBIINIT6_DAC_RD_OUTPUT(int val)         { return (((val) >> 17) & 3);}     /* voodoo 2 only */
    static int FBIINIT6_DAC_WR_OUTPUT(int val)         { return (((val) >> 19) & 3);}     /* voodoo 2 only */
    static int FBIINIT6_PCI_FIFO_LWM_RDY(int val)      { return (((val) >> 21) & 0x7f);}  /* voodoo 2 only */
    static int FBIINIT6_VGA_PASS_N_OUTPUT(int val)     { return (((val) >> 28) & 3);}     /* voodoo 2 only */
    static boolean FBIINIT6_X_VIDEO_TILES_BIT0(int val)    { return (((val) >> 30) & 1)!=0;}     /* voodoo 2 only */
    
    static int FBIINIT7_GENERIC_STRAPPING(int val) { return (((val) >> 0) & 0xff);}   /* voodoo 2 only */
    static boolean FBIINIT7_CMDFIFO_ENABLE(int val) { return (((val) >> 8) & 1)!=0;}      /* voodoo 2 only */
    static boolean FBIINIT7_CMDFIFO_MEMORY_STORE(int val) { return (((val) >> 9) & 1)!=0;}      /* voodoo 2 only */
    static boolean FBIINIT7_DISABLE_CMDFIFO_HOLES(int val) { return (((val) >> 10) & 1)!=0;}     /* voodoo 2 only */
    static int FBIINIT7_CMDFIFO_READ_THRESH(int val) { return (((val) >> 11) & 0x1f);}  /* voodoo 2 only */
    static boolean FBIINIT7_SYNC_CMDFIFO_WRITES(int val) { return (((val) >> 16) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT7_SYNC_CMDFIFO_READS(int val) { return (((val) >> 17) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT7_RESET_PCI_PACKER(int val) { return (((val) >> 18) & 1)!=0;}     /* voodoo 2 only */
    static boolean FBIINIT7_ENABLE_CHROMA_STUFF(int val) { return (((val) >> 19) & 1)!=0;}     /* voodoo 2 only */
    static int FBIINIT7_CMDFIFO_PCI_TIMEOUT(int val) { return (((val) >> 20) & 0x7f);}  /* voodoo 2 only */
    static boolean FBIINIT7_ENABLE_TEXTURE_BURST(int val) { return (((val) >> 27) & 1)!=0;}     /* voodoo 2 only */
    
    static boolean TEXMODE_ENABLE_PERSPECTIVE(int val)     { return (((val) >> 0) & 1)!=0;}
    static boolean TEXMODE_MINIFICATION_FILTER(int val)    { return (((val) >> 1) & 1)!=0;}
    static boolean TEXMODE_MAGNIFICATION_FILTER(int val)   { return (((val) >> 2) & 1)!=0;}
    static boolean TEXMODE_CLAMP_NEG_W(int val)            { return (((val) >> 3) & 1)!=0;}
    static boolean TEXMODE_ENABLE_LOD_DITHER(int val)      { return (((val) >> 4) & 1)!=0;}
    static int TEXMODE_NCC_TABLE_SELECT(int val)       { return (((val) >> 5) & 1);}
    static boolean TEXMODE_CLAMP_S(int val)                { return (((val) >> 6) & 1)!=0;}
    static boolean TEXMODE_CLAMP_T(int val)                { return (((val) >> 7) & 1)!=0;}
    static int TEXMODE_FORMAT(int val)                 { return (((val) >> 8) & 0xf);}
    static boolean TEXMODE_TC_ZERO_OTHER(int val)          { return (((val) >> 12) & 1)!=0;}
    static boolean TEXMODE_TC_SUB_CLOCAL(int val)          { return (((val) >> 13) & 1)!=0;}
    static int TEXMODE_TC_MSELECT(int val)             { return (((val) >> 14) & 7);}
    static boolean TEXMODE_TC_REVERSE_BLEND(int val)       { return (((val) >> 17) & 1)!=0;}
    static int TEXMODE_TC_ADD_ACLOCAL(int val)         { return (((val) >> 18) & 3);}
    static boolean TEXMODE_TC_INVERT_OUTPUT(int val)       { return (((val) >> 20) & 1)!=0;}
    static boolean TEXMODE_TCA_ZERO_OTHER(int val)         { return (((val) >> 21) & 1)!=0;}
    static boolean TEXMODE_TCA_SUB_CLOCAL(int val)         { return (((val) >> 22) & 1)!=0;}
    static int TEXMODE_TCA_MSELECT(int val)            { return (((val) >> 23) & 7);}
    static boolean TEXMODE_TCA_REVERSE_BLEND(int val)      { return (((val) >> 26) & 1)!=0;}
    static int TEXMODE_TCA_ADD_ACLOCAL(int val)        { return (((val) >> 27) & 3);}
    static boolean TEXMODE_TCA_INVERT_OUTPUT(int val)      { return (((val) >> 29) & 1)!=0;}
    static boolean TEXMODE_TRILINEAR(int val)              { return (((val) >> 30) & 1)!=0;}
    static boolean TEXMODE_SEQ_8_DOWNLD(int val)           { return (((val) >> 31) & 1)!=0;}
    
    static int TEXLOD_LODMIN(int val)                  { return (((val) >> 0) & 0x3f);}
    static int TEXLOD_LODMAX(int val)                  { return (((val) >> 6) & 0x3f);}
    static int TEXLOD_LODBIAS(int val)                 { return (((val) >> 12) & 0x3f);}
    static boolean TEXLOD_LOD_ODD(int val)                 { return (((val) >> 18) & 1)!=0;}
    static boolean TEXLOD_LOD_TSPLIT(int val)              { return (((val) >> 19) & 1)!=0;}
    static boolean TEXLOD_LOD_S_IS_WIDER(int val)          { return (((val) >> 20) & 1)!=0;}
    static int TEXLOD_LOD_ASPECT(int val)              { return (((val) >> 21) & 3);}
    static boolean TEXLOD_LOD_ZEROFRAC(int val)            { return (((val) >> 23) & 1)!=0;}
    static boolean TEXLOD_TMULTIBASEADDR(int val)          { return (((val) >> 24) & 1)!=0;}
    static boolean TEXLOD_TDATA_SWIZZLE(int val)           { return (((val) >> 25) & 1)!=0;}
    static boolean TEXLOD_TDATA_SWAP(int val)              { return (((val) >> 26) & 1)!=0;}
    static boolean TEXLOD_TDIRECT_WRITE(int val)           { return (((val) >> 27) & 1)!=0;}     /* Voodoo 2 only */
    
    static int TEXDETAIL_DETAIL_MAX(int val)           { return (((val) >> 0) & 0xff);}
    static int TEXDETAIL_DETAIL_BIAS(int val)          { return (((val) >> 8) & 0x3f);}
    static int TEXDETAIL_DETAIL_SCALE(int val)         { return (((val) >> 14) & 7);}
    static boolean TEXDETAIL_RGB_MIN_FILTER(int val)       { return (((val) >> 17) & 1)!=0;}     /* Voodoo 2 only */
    static boolean TEXDETAIL_RGB_MAG_FILTER(int val)       { return (((val) >> 18) & 1)!=0;}     /* Voodoo 2 only */
    static boolean TEXDETAIL_ALPHA_MIN_FILTER(int val)     { return (((val) >> 19) & 1)!=0;}     /* Voodoo 2 only */
    static boolean TEXDETAIL_ALPHA_MAG_FILTER(int val)     { return (((val) >> 20) & 1)!=0;}     /* Voodoo 2 only */
    static boolean TEXDETAIL_SEPARATE_RGBA_FILTER(int val) { return (((val) >> 21) & 1)!=0;}     /* Voodoo 2 only */
    
    static private int FLIPENDIAN_INT32(int x) {
        return (x << 24) | (x >>> 24) | ((x & 0x0000ff00) << 8) | ((x & 0x00ff0000) >> 8);
    }

    /*************************************
     *
     *  Computes a fast 16.16 reciprocal
     *  of a 16.32 value; used for
     *  computing 1/w in the rasterizer.
     *
     *  Since it is trivial to also
     *  compute log2(1/w) = -log2(w) at
     *  the same time, we do that as well
     *  to 16.8 precision for LOD
     *  calculations.
     *
     *************************************/

    private int fast_reciplog(long value, IntRef log2)
    {
    	int temp, recip, rlog;
        int interp;
    	int tablePos;
    	boolean neg = false;
    	int lz, exp = 0;

    	/* always work with unsigned numbers */
    	if (value < 0)
    	{
    		value = -value;
    		neg = true;
    	}

    	/* if we've spilled out of 32 bits, push it down under 32 */
    	if ((value & 0xffff00000000l)!=0)
    	{
    		temp = (int)(value >> 16);
    		exp -= 16;
    	}
    	else
    		temp = (int)value;

    	/* if the resulting value is 0, the reciprocal is infinite */
    	if (temp == 0)
    	{
    		log2.value = 1000 << LOG_OUTPUT_PREC;
    		return neg ? 0x80000000 : 0x7fffffff;
    	}

    	/* determine how many leading zeros in the value and shift it up high */
    	lz = Integer.numberOfLeadingZeros(temp);
    	temp <<= lz;
    	exp += lz;

    	/* compute a pointer to the table entries we want */
    	/* math is a bit funny here because we shift one less than we need to in order */
    	/* to account for the fact that there are two UINT32's per table entry */
    	tablePos = (temp >>> (31 - RECIPLOG_LOOKUP_BITS - 1)) & ((2 << RECIPLOG_LOOKUP_BITS) - 2);

    	/* compute the interpolation value */
    	interp = (temp >>> (31 - RECIPLOG_LOOKUP_BITS - 8)) & 0xff;

    	/* do a linear interpolatation between the two nearest table values */
    	/* for both the log and the reciprocal */
    	rlog = (voodoo_reciplog[tablePos+1] * (0x100 - interp) + voodoo_reciplog[tablePos+3] * interp) >>> 8;
    	recip = (voodoo_reciplog[tablePos] * (0x100 - interp) + voodoo_reciplog[tablePos+2] * interp) >>> 8;

    	/* the log result is the fractional part of the log; round it to the output precision */
    	rlog = (rlog + (1 << (RECIPLOG_LOOKUP_PREC - LOG_OUTPUT_PREC - 1))) >> (RECIPLOG_LOOKUP_PREC - LOG_OUTPUT_PREC);

    	/* the exponent is the non-fractional part of the log; normally, we would subtract it from rlog */
    	/* but since we want the log(1/value) = -log(value), we subtract rlog from the exponent */
    	log2.value = ((exp - (31 - RECIPLOG_INPUT_PREC)) << LOG_OUTPUT_PREC) - rlog;

    	/* adjust the exponent to account for all the reciprocal-related parameters to arrive at a final shift amount */
    	exp += (RECIP_OUTPUT_PREC - RECIPLOG_LOOKUP_PREC) - (31 - RECIPLOG_INPUT_PREC);

    	/* shift by the exponent */
    	if (exp < 0)
    		recip >>>= -exp;
    	else
    		recip <<= exp;

    	/* on the way out, apply the original sign to the reciprocal */
    	return (neg && recip>0) ? -recip : recip;
    }

    static private int float_to_int32(int data, int fixedbits)
    {
    	int exponent = ((data >> 23) & 0xff) - 127 - 23 + fixedbits;
    	int result = (data & 0x7fffff) | 0x800000;
    	if (exponent < 0)
    	{
    		if (exponent > -32)
    			result >>= -exponent;
    		else
    			result = 0;
    	}
    	else
    	{
    		if (exponent < 32)
    			result <<= exponent;
    		else
    			result = 0x7fffffff;
    	}
    	if ((data & 0x80000000)!=0)
    		result = -result;
    	return result;
    }

    private static long float_to_long(int data, int fixedbits)
    {
    	int exponent = ((data >> 23) & 0xff) - 127 - 23 + fixedbits;
    	long result = (data & 0x7fffff) | 0x800000;
    	if (exponent < 0)
    	{
    		if (exponent > -64)
    			result >>= -exponent;
    		else
    			result = 0;
    	}
    	else
    	{
    		if (exponent < 64)
    			result <<= exponent;
    		else
    			result = 0x7fffffffffffffffl;
    	}
    	if ((data & 0x80000000)!=0)
    		result = -result;
    	return result;
    }

    /*************************************
     *
     *  Rasterizer inlines
     *
     *************************************/

    static private int normalize_color_path(int eff_color_path)
    {
    	/* ignore the subpixel adjust and texture enable flags */
    	eff_color_path &= ~((1 << 26) | (1 << 27));

    	return eff_color_path;
    }


    static private int normalize_alpha_mode(int eff_alpha_mode)
    {
    	/* always ignore alpha ref value */
    	eff_alpha_mode &= ~(0xff << 24);

    	/* if not doing alpha testing, ignore the alpha function and ref value */
    	if (!ALPHAMODE_ALPHATEST(eff_alpha_mode))
    		eff_alpha_mode &= ~(7 << 1);

    	/* if not doing alpha blending, ignore the source and dest blending factors */
    	if (!ALPHAMODE_ALPHABLEND(eff_alpha_mode))
    		eff_alpha_mode &= ~((15 << 8) | (15 << 12) | (15 << 16) | (15 << 20));

    	return eff_alpha_mode;
    }


    private static int normalize_fog_mode(int eff_fog_mode)
    {
    	/* if not doing fogging, ignore all the other fog bits */
    	if (!FOGMODE_ENABLE_FOG(eff_fog_mode))
    		eff_fog_mode = 0;

    	return eff_fog_mode;
    }


    private static int normalize_fbz_mode(int eff_fbz_mode)
    {
    	/* ignore the draw buffer */
    	eff_fbz_mode &= ~(3 << 14);

    	return eff_fbz_mode;
    }


    private static int normalize_tex_mode(int eff_tex_mode)
    {
    	/* ignore the NCC table and seq_8_downld flags */
    	eff_tex_mode &= ~((1 << 5) | (1 << 31));

    	/* classify texture formats into 3 format categories */
    	if (TEXMODE_FORMAT(eff_tex_mode) < 8)
    		eff_tex_mode = (eff_tex_mode & ~(0xf << 8)) | (0 << 8);
    	else if (TEXMODE_FORMAT(eff_tex_mode) >= 10 && TEXMODE_FORMAT(eff_tex_mode) <= 12)
    		eff_tex_mode = (eff_tex_mode & ~(0xf << 8)) | (10 << 8);
    	else
    		eff_tex_mode = (eff_tex_mode & ~(0xf << 8)) | (8 << 8);

    	return eff_tex_mode;
    }


    static private int compute_raster_hash(raster_info info)
    {
    	int hash;

    	/* make a hash */
    	hash = info.eff_color_path;
    	hash = (hash << 1) | (hash >> 31);
    	hash ^= info.eff_fbz_mode;
    	hash = (hash << 1) | (hash >> 31);
    	hash ^= info.eff_alpha_mode;
    	hash = (hash << 1) | (hash >> 31);
    	hash ^= info.eff_fog_mode;
        if (info.eff_tex_mode_0!=-1) {
    	    hash = (hash << 1) | (hash >> 31);
    	    hash ^= info.eff_tex_mode_0;
        }
        if (info.eff_tex_mode_1!=-1) {
    	    hash = (hash << 1) | (hash >> 31);
    	    hash ^= info.eff_tex_mode_1;
        }

    	return (int)((hash & 0xFFFFFFFFl) % RASTER_HASH_SIZE);
    }

    /*************************************
     *
     *  Inline FIFO management
     *
     *************************************/
    
    static private void fifo_reset(fifo_state f)
    {
        f.in = f.out = 0;
    }
    
    
    static private void fifo_add(fifo_state f, int data)
    {
        int next_in;
    
        /* compute the value of 'in' after we add this item */
        next_in = f.in + 1;
        if (next_in >= f.size)
            next_in = 0;
    
        /* as long as it's not equal to the output pointer, we can do it */
        if (next_in != f.out)
        {
            f.write(f.in, data);
            f.in = next_in;
        }
    }
    
    
    static private int fifo_remove(fifo_state f)
    {
        int data = 0xffffffff;
    
        /* as long as we have data, we can do it */
        if (f.out != f.in)
        {
            int next_out;
    
            /* fetch the data */
            data = f.read(f.out);
    
            /* advance the output pointer */
            next_out = f.out + 1;
            if (next_out >= f.size)
                next_out = 0;
            f.out = next_out;
        }
        return data;
    }
    
    
    static private int fifo_peek(fifo_state f)
    {
        return f.read(f.out);
    }
    
    
    static private boolean fifo_empty(fifo_state f)
    {
        return (f.in == f.out);
    }
    
    
    static private boolean fifo_full(fifo_state f)
    {
        return (f.in + 1 == f.out || (f.in == f.size - 1 && f.out == 0));
    }
    
    
    static private int fifo_items(fifo_state f)
    {
        int items = f.in - f.out;
        if (items < 0)
            items += f.size;
        return items;
    }
    
    
    static private int fifo_space(fifo_state f)
    {
        int items = f.in - f.out;
        if (items < 0)
            items += f.size;
        return f.size - 1 - items;
    }
    
    /*************************************
     *
     *  Statistics management
     *
     *************************************/
    
    private void accumulate_statistics(stats_block stats)
    {
    	/* apply internal voodoo statistics */
    	reg[fbiPixelsIn] += stats.pixels_in;
    	reg[fbiPixelsOut] += stats.pixels_out;
    	reg[fbiChromaFail] += stats.chroma_fail;
    	reg[fbiZfuncFail] += stats.zfunc_fail;
    	reg[fbiAfuncFail] += stats.afunc_fail;
    
    	/* apply emulation statistics */
    	this.stats.total_pixels_in += stats.pixels_in;
        this.stats.total_pixels_out += stats.pixels_out;
        this.stats.total_chroma_fail += stats.chroma_fail;
        this.stats.total_zfunc_fail += stats.zfunc_fail;
        this.stats.total_afunc_fail += stats.afunc_fail;
        this.stats.total_clipped += stats.clip_fail;
        this.stats.total_stippled += stats.stipple_count;
    }
    
    
    private void update_statistics(boolean accumulate)
    {
    	int threadnum;
    
    	/* accumulate/reset statistics from all units */
    	for (threadnum = 0; threadnum < Poly.WORK_MAX_THREADS; threadnum++)
    	{
    		if (accumulate)
    			accumulate_statistics(thread_stats[threadnum]);
            thread_stats[threadnum].clear();
    	}
    
    	/* accumulate/reset statistics from the LFB */
    	if (accumulate)
    		accumulate_statistics(fbi.lfb_stats);
        fbi.lfb_stats.clear();
    }
    
    /*************************************
     *
     *  VBLANK management
     *
     *************************************/
    
    private void swap_buffers()
    {
    	int count;
    
    	//if (LOG_VBLANK_SWAP) logerror("--- swap_buffers @ %d\n", screen.vpos());
    
    	/* force a partial update */
    	//screen.update_partial(screen.vpos());
    	fbi.video_changed = true;
    
    	/* keep a history of swap intervals */
    	count = fbi.vblank_count;
    	if (count > 15)
    		count = 15;
    	reg[fbiSwapHistory] = (reg[fbiSwapHistory] << 4) | count;
    
    	/* rotate the buffers */
    	if (type <= TYPE_VOODOO_2)
    	{
    		if (type < TYPE_VOODOO_2 || !fbi.vblank_dont_swap)
    		{
    			if (fbi.rgboffs[2] == ~0)
    			{
    				fbi.frontbuf = 1 - fbi.frontbuf;
    				fbi.backbuf = 1 - fbi.frontbuf;
    			}
    			else
    			{
    				fbi.frontbuf = (fbi.frontbuf + 1) % 3;
    				fbi.backbuf = (fbi.frontbuf + 1) % 3;
    			}
    		}
    	}
    	else
    		fbi.rgboffs[0] = reg[leftOverlayBuf] & fbi.mask & ~0x0f;
    
    	/* decrement the pending count and reset our state */
    	if (fbi.swaps_pending!=0)
    		fbi.swaps_pending--;
    	fbi.vblank_count = 0;
    	fbi.vblank_swap_pending = false;
    
    	/* reset the last_op_time to now and start processing the next command */
    	if (pci.op_pending)
    	{
    		pci.op_end_time = System.nanoTime();
    		flush_fifos();
    	}
    
    	/* we may be able to unstall now */
//    	if (pci.stall_state != NOT_STALLED)
//    		check_stalled_cpu(v, device.machine().time());
    
    	/* periodically log rasterizer info */
    	stats.swaps++;
    	//if (LOG_RASTERIZERS && stats.swaps % 100 == 0)
    	//	dump_rasterizer_stats(v);
    
    	/* update the statistics (debug) */
//    	if (stats.display)
//    	{
//    		const rectangle &visible_area = screen.visible_area();
//    		int screen_area = visible_area.width() * visible_area.height();
//    		char *statsptr = stats.buffer;
//    		int pixelcount;
//    		int i;
//
//    		update_statistics(v, TRUE);
//    		pixelcount = stats.total_pixels_out;
//
//    		statsptr += sprintf(statsptr, "Swap:%6d\n", stats.swaps);
//    		statsptr += sprintf(statsptr, "Hist:%08X\n", reg[fbiSwapHistory].u);
//    		statsptr += sprintf(statsptr, "Stal:%6d\n", stats.stalls);
//    		statsptr += sprintf(statsptr, "Rend:%6d%%\n", pixelcount * 100 / screen_area);
//    		statsptr += sprintf(statsptr, "Poly:%6d\n", stats.total_triangles);
//    		statsptr += sprintf(statsptr, "PxIn:%6d\n", stats.total_pixels_in);
//    		statsptr += sprintf(statsptr, "POut:%6d\n", stats.total_pixels_out);
//    		statsptr += sprintf(statsptr, "Clip:%6d\n", stats.total_clipped);
//    		statsptr += sprintf(statsptr, "Stip:%6d\n", stats.total_stippled);
//    		statsptr += sprintf(statsptr, "Chro:%6d\n", stats.total_chroma_fail);
//    		statsptr += sprintf(statsptr, "ZFun:%6d\n", stats.total_zfunc_fail);
//    		statsptr += sprintf(statsptr, "AFun:%6d\n", stats.total_afunc_fail);
//    		statsptr += sprintf(statsptr, "RegW:%6d\n", stats.reg_writes);
//    		statsptr += sprintf(statsptr, "RegR:%6d\n", stats.reg_reads);
//    		statsptr += sprintf(statsptr, "LFBW:%6d\n", stats.lfb_writes);
//    		statsptr += sprintf(statsptr, "LFBR:%6d\n", stats.lfb_reads);
//    		statsptr += sprintf(statsptr, "TexW:%6d\n", stats.tex_writes);
//    		statsptr += sprintf(statsptr, "TexM:");
//    		for (i = 0; i < 16; i++)
//    			if (stats.texture_mode[i])
//    				*statsptr++ = "0123456789ABCDEF"[i];
//    		*statsptr = 0;
//    	}
    
    	/* update statistics */
    	stats.stalls = 0;
    	stats.total_triangles = 0;
    	stats.total_pixels_in = 0;
    	stats.total_pixels_out = 0;
    	stats.total_chroma_fail = 0;
    	stats.total_zfunc_fail = 0;
    	stats.total_afunc_fail = 0;
    	stats.total_clipped = 0;
    	stats.total_stippled = 0;
    	stats.reg_writes = 0;
    	stats.reg_reads = 0;
    	stats.lfb_writes = 0;
    	stats.lfb_reads = 0;
    	stats.tex_writes = 0;
    	Arrays.fill(stats.texture_mode, 0);
    }

    void adjust_vblank_timer()
    {

    }

    public void vblank_callback() {
        //if (LOG_VBLANK_SWAP) logerror("--- vblank start\n");

        /* flush the pipes */
        if (pci.op_pending)
        {
            //if (LOG_VBLANK_SWAP) logerror("---- vblank flush begin\n");
            flush_fifos();
            //if (LOG_VBLANK_SWAP) logerror("---- vblank flush end\n");
        }

        /* increment the count */
        fbi.vblank_count++;
        if (fbi.vblank_count > 250)
            fbi.vblank_count = 250;
        //if (LOG_VBLANK_SWAP) logerror("---- vblank count = %d", fbi.vblank_count);
        //if (fbi.vblank_swap_pending)
        //    if (LOG_VBLANK_SWAP) logerror(" (target=%d)", fbi.vblank_swap);
        //if (LOG_VBLANK_SWAP) logerror("\n");

        /* if we're past the swap count, do the swap */
        if (fbi.vblank_swap_pending && fbi.vblank_count >= fbi.vblank_swap)
            swap_buffers();

        /* set internal state and call the client */
        fbi.vblank = true;

        // TODO: Vblank IRQ enable is VOODOO3 only?
        if (type >= TYPE_VOODOO_3)
        {
            if ((reg[intrCtrl] & 0x4)!=0)       // call IRQ handler if VSYNC interrupt (rising) is enabled
            {
                reg[intrCtrl] |= 0x100;        // VSYNC int (rising) active

                if (fbi.vblank_client != null)
                    fbi.vblank_client.call(VoodooCommon.this, 1);
            }
        }
        else
        {
            if (fbi.vblank_client != null)
                fbi.vblank_client.call(VoodooCommon.this, 1);
        }
    }

    void vblank_off_callback() {
        //if (LOG_VBLANK_SWAP) logerror("--- vblank end\n");

        /* set internal state and call the client */
        fbi.vblank = false;

        // TODO: Vblank IRQ enable is VOODOO3 only?
        if (type >= TYPE_VOODOO_3)
        {
            if ((reg[intrCtrl] & 0x8)!=0)       // call IRQ handler if VSYNC interrupt (falling) is enabled
            {
                reg[intrCtrl] |= 0x200;        // VSYNC int (falling) active

                if (fbi.vblank_client != null)
                    fbi.vblank_client.call(VoodooCommon.this, 0);

            }
        }
        else
        {
            if (fbi.vblank_client != null)
                fbi.vblank_client.call(VoodooCommon.this, 0);
        }

        /* go to the end of the next frame */
        adjust_vblank_timer();
    }

    
    /*************************************
     *
     *  Chip reset
     *
     *************************************/
    
    private void reset_counters()
    {
    	update_statistics(false);
    	reg[fbiPixelsIn] = 0;
    	reg[fbiChromaFail] = 0;
    	reg[fbiZfuncFail] = 0;
    	reg[fbiAfuncFail] = 0;
    	reg[fbiPixelsOut] = 0;
    }
    
    
    private void soft_reset()
    {
    	reset_counters();
    	reg[fbiTrianglesOut] = 0;
    	fifo_reset(fbi.fifo);
    	fifo_reset(pci.fifo);
    }
    
    
    
    /*************************************
     *
     *  Recompute video memory layout
     *
     *************************************/
    
    private void recompute_video_memory()
    {
    	int buffer_pages = FBIINIT2_VIDEO_BUFFER_OFFSET(reg[fbiInit2]);
        int fifo_start_page = FBIINIT4_MEMORY_FIFO_START_ROW(reg[fbiInit4]);
        int fifo_last_page = FBIINIT4_MEMORY_FIFO_STOP_ROW(reg[fbiInit4]);
        int memory_config;
    	int buf;
    
    	/* memory config is determined differently between V1 and V2 */
    	memory_config = FBIINIT2_ENABLE_TRIPLE_BUF(reg[fbiInit2]);
    	if (type == TYPE_VOODOO_2 && memory_config==0)
    		memory_config = FBIINIT5_BUFFER_ALLOCATION(reg[fbiInit5]);
    
    	/* tiles are 64x16/32; x_tiles specifies how many half-tiles */
    	fbi.tile_width = (type == TYPE_VOODOO_1) ? 64 : 32;
    	fbi.tile_height = (type == TYPE_VOODOO_1) ? 16 : 32;
    	fbi.x_tiles = FBIINIT1_X_VIDEO_TILES(reg[fbiInit1]);
    	if (type == TYPE_VOODOO_2)
    	{
    		fbi.x_tiles = (fbi.x_tiles << 1) |
    						((FBIINIT1_X_VIDEO_TILES_BIT5(reg[fbiInit1])?1:0) << 5) |
    						(FBIINIT6_X_VIDEO_TILES_BIT0(reg[fbiInit6])?1:0);
    	}
    	fbi.rowpixels = fbi.tile_width * fbi.x_tiles;
    
    //  logerror("VOODOO.%d.VIDMEM: buffer_pages=%X  fifo=%X-%X  tiles=%X  rowpix=%d\n", index, buffer_pages, fifo_start_page, fifo_last_page, fbi.x_tiles, fbi.rowpixels);
    
    	/* first RGB buffer always starts at 0 */
    	fbi.rgboffs[0] = 0;
    
    	/* second RGB buffer starts immediately afterwards */
    	fbi.rgboffs[1] = buffer_pages * 0x1000;
    
    	/* remaining buffers are based on the config */
    	switch (memory_config)
    	{
    		case 3: /* reserved */
    			Log.exit("VOODOO."+this.pci_id+".ERROR:Unexpected memory configuration in recompute_video_memory!");
    
    		case 0: /* 2 color buffers, 1 aux buffer */
    			fbi.rgboffs[2] = ~0;
    			fbi.auxoffs = 2 * buffer_pages * 0x1000;
    			break;
    
    		case 1: /* 3 color buffers, 0 aux buffers */
    			fbi.rgboffs[2] = 2 * buffer_pages * 0x1000;
    			fbi.auxoffs = -1;
    			break;
    
    		case 2: /* 3 color buffers, 1 aux buffers */
    			fbi.rgboffs[2] = 2 * buffer_pages * 0x1000;
    			fbi.auxoffs = 3 * buffer_pages * 0x1000;
    			break;
    	}
    	/* clamp the RGB buffers to video memory */
    	for (buf = 0; buf < 3; buf++)
    		if (fbi.rgboffs[buf] != ~0 && fbi.rgboffs[buf] > fbi.mask)
    			fbi.rgboffs[buf] = fbi.mask;
    
    	/* clamp the aux buffer to video memory */
    	if (fbi.auxoffs != -1 && fbi.auxoffs > fbi.mask)
    		fbi.auxoffs = fbi.mask;
    
    /*  mame_printf_debug("rgb[0] = %08X   rgb[1] = %08X   rgb[2] = %08X   aux = %08X\n",
                fbi.rgboffs[0], fbi.rgboffs[1], fbi.rgboffs[2], fbi.auxoffs);*/
    
    	/* compute the memory FIFO location and size */
    	if (fifo_last_page > fbi.mask / 0x1000)
    		fifo_last_page = fbi.mask / 0x1000;
    
    	/* is it valid and enabled? */
    	if (fifo_start_page <= fifo_last_page && FBIINIT0_ENABLE_MEMORY_FIFO(reg[fbiInit0]))
    	{
            fbi.fifo.mem = fbi.ram;
    		fbi.fifo.base = fifo_start_page * 0x1000 / 2;
    		fbi.fifo.size = (fifo_last_page + 1 - fifo_start_page) * 0x1000 / 4;
    		if (fbi.fifo.size > 65536*2)
    			fbi.fifo.size = 65536*2;
    	}
    
    	/* if not, disable the FIFO */
    	else
    	{
    		fbi.fifo.base = -1;
    		fbi.fifo.size = 0;
    	}
    
    	/* reset the FIFO */
    	fifo_reset(fbi.fifo);
    
    	/* reset our front/back buffers if they are out of range */
    	if (fbi.rgboffs[2] == ~0)
    	{
    		if (fbi.frontbuf == 2)
    			fbi.frontbuf = 0;
    		if (fbi.backbuf == 2)
    			fbi.backbuf = 0;
    	}
    }
    
    
    
    /*************************************
     *
     *  NCC table management
     *
     *************************************/
    
    private void ncc_table_write(ncc_table n, int regnum, int data)
    {
    	/* I/Q entries reference the plaette if the high bit is set */
    	if (regnum >= 4 && (data & 0x80000000)!=0 && n.palette!=null)
    	{
    		int index = ((data >> 23) & 0xfe) | (regnum & 1);
    
    		/* set the ARGB for this palette index */
    		n.palette[index] = 0xff000000 | data;
    
    		/* if we have an ARGB palette as well, compute its value */
    		if (n.palettea!=null)
    		{
    			int a = ((data >> 16) & 0xfc) | ((data >> 22) & 0x03);
    			int r = ((data >> 10) & 0xfc) | ((data >> 16) & 0x03);
    			int g = ((data >>  4) & 0xfc) | ((data >> 10) & 0x03);
    			int b = ((data <<  2) & 0xfc) | ((data >>  4) & 0x03);
    			n.palettea[index] = MAKE_ARGB(a, r, g, b);
    		}
    
    		/* this doesn't dirty the table or go to the registers, so bail */
    		return;
    	}
    
    	/* if the register matches, don't update */
    	if (data == reg[n.reg+regnum])
    		return;
    	reg[n.reg+regnum] = data;
    
    	/* first four entries are packed Y values */
    	if (regnum < 4)
    	{
    		regnum *= 4;
    		n.y[regnum+0] = (data >>  0) & 0xff;
    		n.y[regnum+1] = (data >>  8) & 0xff;
    		n.y[regnum+2] = (data >> 16) & 0xff;
    		n.y[regnum+3] = (data >> 24) & 0xff;
    	}
    
    	/* the second four entries are the I RGB values */
    	else if (regnum < 8)
    	{
    		regnum &= 3;
    		n.ir[regnum] = (data <<  5) >> 23; // intentional signed shift
    		n.ig[regnum] = (data << 14) >> 23; // intentional signed shift
    		n.ib[regnum] = (data << 23) >> 23; // intentional signed shift
    	}
    
    	/* the final four entries are the Q RGB values */
    	else
    	{
    		regnum &= 3;
    		n.qr[regnum] = (data <<  5) >> 23; // intentional signed shift
    		n.qg[regnum] = (data << 14) >> 23; // intentional signed shift
    		n.qb[regnum] = (data << 23) >> 23; // intentional signed shift
    	}
    
    	/* mark the table dirty */
    	n.dirty = true;
    }
    
    
    private void ncc_table_update(ncc_table n)
    {
    	int r, g, b, i;
    
    	/* generte all 256 possibilities */
    	for (i = 0; i < 256; i++)
    	{
    		int vi = (i >> 2) & 0x03;
    		int vq = (i >> 0) & 0x03;
    
    		/* start with the intensity */
    		r = g = b = n.y[(i >> 4) & 0x0f];
    
    		/* add the coloring */
    		r += n.ir[vi] + n.qr[vq];
    		g += n.ig[vi] + n.qg[vq];
    		b += n.ib[vi] + n.qb[vq];
    
    		/* clamp */
    		r=CLAMPr(r, 0, 255);
    		g=CLAMPr(g, 0, 255);
    		b=CLAMPr(b, 0, 255);
    
    		/* fill in the table */
    		n.texel[i] = MAKE_ARGB(0xff, r, g, b);
    	}
    
    	/* no longer dirty */
    	n.dirty = false;
    }

    /*************************************
     *
     *  Faux DAC implementation
     *
     *************************************/

    private void dacdata_w(dac_state d, int regnum, int data)
    {
    	d.reg[regnum] = data;
    }


    private void dacdata_r(dac_state d, int regnum)
    {
    	int result = 0xff;

    	/* switch off the DAC register requested */
    	switch (regnum)
    	{
    		case 5:
    			/* this is just to make startup happy */
    			switch (d.reg[7])
    			{
    				case 0x01:  result = 0x55; break;
    				case 0x07:  result = 0x71; break;
    				case 0x0b:  result = 0x79; break;
    			}
    			break;

    		default:
    			result = d.reg[regnum];
    			break;
    	}

    	/* remember the read result; it is fetched elsewhere */
    	d.read_result = result;
    }

    /*************************************
     *
     *  Texuture parameter computation
     *
     *************************************/
    
    private void recompute_texture_params(tmu_state t)
    {
    	int bppscale;
    	int base;
    	int lod;
    
    	/* extract LOD parameters */
    	t.lodmin = TEXLOD_LODMIN(reg[t.reg+tLOD]) << 6;
    	t.lodmax = TEXLOD_LODMAX(reg[t.reg+tLOD]) << 6;
    	t.lodbias = (byte)((byte)(TEXLOD_LODBIAS(reg[t.reg+tLOD]) << 2) << 4);
    
    	/* determine which LODs are present */
    	t.lodmask = 0x1ff;
    	if (TEXLOD_LOD_TSPLIT(reg[t.reg+tLOD]))
    	{
    		if (!TEXLOD_LOD_ODD(reg[t.reg+tLOD]))
    			t.lodmask = 0x155;
    		else
    			t.lodmask = 0x0aa;
    	}
    
    	/* determine base texture width/height */
    	t.wmask = t.hmask = 0xff;
    	if (TEXLOD_LOD_S_IS_WIDER(reg[t.reg+tLOD]))
    		t.hmask >>= TEXLOD_LOD_ASPECT(reg[t.reg+tLOD]);
    	else
    		t.wmask >>= TEXLOD_LOD_ASPECT(reg[t.reg+tLOD]);
    
    	/* determine the bpp of the texture */
    	bppscale = TEXMODE_FORMAT(reg[t.reg+textureMode]) >> 3;
    
    	/* start with the base of LOD 0 */
    	if (t.texaddr_shift == 0 && (reg[t.reg+texBaseAddr] & 1)!=0)
    		Log.log_msg("Tiled texture\n");
    	base = (reg[t.reg+texBaseAddr] & t.texaddr_mask) << t.texaddr_shift;
    	t.lodoffset[0] = base & t.mask;
    
    	/* LODs 1-3 are different depending on whether we are in multitex mode */
    	/* Several Voodoo 2 games leave the upper bits of TLOD == 0xff, meaning we think */
    	/* they want multitex mode when they really don't -- disable for now */
    	// Enable for Voodoo 3 or Viper breaks - VL.
    	if (TEXLOD_TMULTIBASEADDR(reg[t.reg+tLOD]))
    	{
    		base = (reg[t.reg+texBaseAddr_1] & t.texaddr_mask) << t.texaddr_shift;
    		t.lodoffset[1] = base & t.mask;
    		base = (reg[t.reg+texBaseAddr_2] & t.texaddr_mask) << t.texaddr_shift;
    		t.lodoffset[2] = base & t.mask;
    		base = (reg[t.reg+texBaseAddr_3_8] & t.texaddr_mask) << t.texaddr_shift;
    		t.lodoffset[3] = base & t.mask;
    	}
    	else
    	{
    		if ((t.lodmask & (1 << 0))!=0)
    			base += (((t.wmask >> 0) + 1) * ((t.hmask >> 0) + 1)) << bppscale;
    		t.lodoffset[1] = base & t.mask;
    		if ((t.lodmask & (1 << 1))!=0)
    			base += (((t.wmask >> 1) + 1) * ((t.hmask >> 1) + 1)) << bppscale;
    		t.lodoffset[2] = base & t.mask;
    		if ((t.lodmask & (1 << 2))!=0)
    			base += (((t.wmask >> 2) + 1) * ((t.hmask >> 2) + 1)) << bppscale;
    		t.lodoffset[3] = base & t.mask;
    	}
    
    	/* remaining LODs make sense */
    	for (lod = 4; lod <= 8; lod++)
    	{
    		if ((t.lodmask & (1 << (lod - 1)))!=0)
    		{
    			int size = ((t.wmask >> (lod - 1)) + 1) * ((t.hmask >> (lod - 1)) + 1);
    			if (size < 4) size = 4;
    			base += size << bppscale;
    		}
    		t.lodoffset[lod] = base & t.mask;
    	}
    
    	/* set the NCC lookup appropriately */
    	t.texel[1] = t.texel[9] = t.ncc[TEXMODE_NCC_TABLE_SELECT(reg[t.reg+textureMode])].texel;
    
    	/* pick the lookup table */
    	t.lookup = t.texel[TEXMODE_FORMAT(reg[t.reg+textureMode])];
    
    	/* compute the detail parameters */
    	t.detailmax = TEXDETAIL_DETAIL_MAX(reg[t.reg+tDetail]);
    	t.detailbias = (byte)((byte)(TEXDETAIL_DETAIL_BIAS(reg[t.reg+tDetail]) << 2) << 6);
    	t.detailscale = TEXDETAIL_DETAIL_SCALE(reg[t.reg+tDetail]);
    
    	/* no longer dirty */
    	t.regdirty = false;
    
    	/* check for separate RGBA filtering */
    	if (TEXDETAIL_SEPARATE_RGBA_FILTER(reg[t.reg+tDetail]))
    		Log.exit("Separate RGBA filters!\n");
    }
    

    IntRef tmp_lodbase = new IntRef(0);
    private int prepare_tmu(tmu_state t)
    {
    	long texdx, texdy;
    
    	/* if the texture parameters are dirty, update them */
    	if (t.regdirty)
    	{
    		recompute_texture_params(t);
    
    		/* ensure that the NCC tables are up to date */
    		if ((TEXMODE_FORMAT(reg[t.reg+textureMode]) & 7) == 1)
    		{
    			ncc_table n = t.ncc[TEXMODE_NCC_TABLE_SELECT(reg[t.reg+textureMode])];
    			t.texel[1] = t.texel[9] = n.texel;
    			if (n.dirty)
    				ncc_table_update(n);
    		}
    	}
    
    	/* compute (ds^2 + dt^2) in both X and Y as 28.36 numbers */
    	texdx = (t.dsdx >> 14) * (t.dsdx >> 14) + (t.dtdx >> 14) * (t.dtdx >> 14);
    	texdy = (t.dsdy >> 14) * (t.dsdy >> 14) + (t.dtdy >> 14) * (t.dtdy >> 14);
    
    	/* pick whichever is larger and shift off some high bits . 28.20 */
    	if (texdx < texdy)
    		texdx = texdy;
    	texdx >>= 16;
    
    	/* use our fast reciprocal/log on this value; it expects input as a */
    	/* 16.32 number, and returns the log of the reciprocal, so we have to */
    	/* adjust the result: negative to get the log of the original value */
    	/* plus 12 to account for the extra exponent, and divided by 2 to */
    	/* get the log of the square root of texdx */
    	fast_reciplog(texdx, tmp_lodbase);
    	return (-tmp_lodbase.value + (12 << 8)) / 2;
    }
    
    /*************************************
     *
     *  Command FIFO depth computation
     *
     *************************************/
    
    private int cmdfifo_compute_expected_depth(cmdfifo_info f)
    {
    	int command = mem_readd(fbi.ram, f.rdptr);
    	int i, count = 0;
    
    	/* low 3 bits specify the packet type */
    	switch (command & 7)
    	{
    		/*
    		    Packet type 0: 1 or 2 words
    
    		      Word  Bits
    		        0  31:29 = reserved
    		        0  28:6  = Address [24:2]
    		        0   5:3  = Function (0 = NOP, 1 = JSR, 2 = RET, 3 = JMP LOCAL, 4 = JMP AGP)
    		        0   2:0  = Packet type (0)
    		        1  31:11 = reserved (JMP AGP only)
    		        1  10:0  = Address [35:25]
    		*/
    		case 0:
    			if (((command >> 3) & 7) == 4)
    				return 2;
    			return 1;
    
    		/*
    		    Packet type 1: 1 + N words
    
    		      Word  Bits
    		        0  31:16 = Number of words
    		        0    15  = Increment?
    		        0  14:3  = Register base
    		        0   2:0  = Packet type (1)
    		        1  31:0  = Data word
    		*/
    		case 1:
    			return 1 + (command >> 16);
    
    		/*
    		    Packet type 2: 1 + N words
    
    		      Word  Bits
    		        0  31:3  = 2D Register mask
    		        0   2:0  = Packet type (2)
    		        1  31:0  = Data word
    		*/
    		case 2:
    			for (i = 3; i <= 31; i++)
    				if ((command & (1 << i))!=0) count++;
    			return 1 + count;
    
    		/*
    		    Packet type 3: 1 + N words
    
    		      Word  Bits
    		        0  31:29 = Number of dummy entries following the data
    		        0   28   = Packed color data?
    		        0   25   = Disable ping pong sign correction (0=normal, 1=disable)
    		        0   24   = Culling sign (0=positive, 1=negative)
    		        0   23   = Enable culling (0=disable, 1=enable)
    		        0   22   = Strip mode (0=strip, 1=fan)
    		        0   17   = Setup S1 and T1
    		        0   16   = Setup W1
    		        0   15   = Setup S0 and T0
    		        0   14   = Setup W0
    		        0   13   = Setup Wb
    		        0   12   = Setup Z
    		        0   11   = Setup Alpha
    		        0   10   = Setup RGB
    		        0   9:6  = Number of vertices
    		        0   5:3  = Command (0=Independent tris, 1=Start new strip, 2=Continue strip)
    		        0   2:0  = Packet type (3)
    		        1  31:0  = Data word
    		*/
    		case 3:
    			count = 2;      /* X/Y */
    			if ((command & (1 << 28))!=0)
    			{
    				if ((command & (3 << 10))!=0) count++;       /* ARGB */
    			}
    			else
    			{
    				if ((command & (1 << 10))!=0) count += 3;    /* RGB */
    				if ((command & (1 << 11))!=0) count++;       /* A */
    			}
    			if ((command & (1 << 12))!=0) count++;           /* Z */
    			if ((command & (1 << 13))!=0) count++;           /* Wb */
    			if ((command & (1 << 14))!=0) count++;           /* W0 */
    			if ((command & (1 << 15))!=0) count += 2;        /* S0/T0 */
    			if ((command & (1 << 16))!=0) count++;           /* W1 */
    			if ((command & (1 << 17))!=0) count += 2;        /* S1/T1 */
    			count *= (command >> 6) & 15;               /* numverts */
    			return 1 + count + (command >> 29);
    
    		/*
    		    Packet type 4: 1 + N words
    
    		      Word  Bits
    		        0  31:29 = Number of dummy entries following the data
    		        0  28:15 = General register mask
    		        0  14:3  = Register base
    		        0   2:0  = Packet type (4)
    		        1  31:0  = Data word
    		*/
    		case 4:
    			for (i = 15; i <= 28; i++)
    				if ((command & (1 << i))!=0) count++;
    			return 1 + count + (command >> 29);
    
    		/*
    		    Packet type 5: 2 + N words
    
    		      Word  Bits
    		        0  31:30 = Space (0,1=reserved, 2=LFB, 3=texture)
    		        0  29:26 = Byte disable W2
    		        0  25:22 = Byte disable WN
    		        0  21:3  = Num words
    		        0   2:0  = Packet type (5)
    		        1  31:30 = Reserved
    		        1  29:0  = Base address [24:0]
    		        2  31:0  = Data word
    		*/
    		case 5:
    			return 2 + ((command >> 3) & 0x7ffff);
    
    		default:
    			Log.log_msg("UNKNOWN PACKET TYPE " + (command & 7));
    			return 1;
    	}
    }
    
    
    
    /*************************************
     *
     *  Command FIFO execution
     *
     *************************************/
    static public int mem_readw(byte[] p, int off) {
        return (p[off] & 0xFF) | ((p[off+1] & 0xFF) << 8);
    }
    static private int mem_readd(byte[] p, int off) {
        return (p[off] & 0xFF) | ((p[off+1] & 0xFF) << 8) | ((p[off+2] & 0xFF) << 16) | ((p[off+3] & 0xFF) << 24);
    }
    static private int mem_readd(short[] p, int off) {
        return (p[off] & 0xFFFF) | ((p[off+1] & 0xFFFF) << 16);
    }
    static private void mem_writed(byte[] p, int off, int val) {
        p[off]=(byte)(val);
        p[off+1]=(byte)((val >> 8));
        p[off+2]=(byte)((val >> 16));
        p[off+3]=(byte)((val >> 24));
    }
    static private void mem_writed(short[] p, int off, int val) {
        p[off]=(short)(val);
        p[off+1]=(short)((val >> 16));
    }
    static private void mem_writew(byte[] p, int off, int val) {
        p[off]=(byte)(val);
        p[off+1]=(byte)((val >> 8));
    }

    private int cmdfifo_execute(cmdfifo_info f)
    {
    	int srcPos = f.rdptr;
    	int command = mem_readd(fbi.ram, srcPos);srcPos+=4;
    	int count, inc, code, i;
    	setup_vertex svert = new setup_vertex();
    	int target;
    	int cycles = 0;
    
    	switch (command & 7)
    	{
    		/*
    		    Packet type 0: 1 or 2 words
    
    		      Word  Bits
    		        0  31:29 = reserved
    		        0  28:6  = Address [24:2]
    		        0   5:3  = Function (0 = NOP, 1 = JSR, 2 = RET, 3 = JMP LOCAL, 4 = JMP AGP)
    		        0   2:0  = Packet type (0)
    		        1  31:11 = reserved (JMP AGP only)
    		        1  10:0  = Address [35:25]
    		*/
    		case 0:
    
    			/* extract parameters */
    			target = (command >> 4) & 0x1fffffc;
    
    			/* switch off of the specific command */
    			switch ((command >> 3) & 7)
    			{
    				case 0:     /* NOP */
    					//if (LOG_CMDFIFO) logerror("  NOP\n");
    					break;
    
    				case 1:     /* JSR */
    					//if (LOG_CMDFIFO) logerror("  JSR $%06X\n", target);
    					//mame_printf_debug("JSR in CMDFIFO!\n");
    					srcPos = target;
    					break;
    
    				case 2:     /* RET */
    					//if (LOG_CMDFIFO) logerror("  RET $%06X\n", target);
    					Log.exit("RET in CMDFIFO!\n");
    					break;
    
    				case 3:     /* JMP LOCAL FRAME BUFFER */
    					//if (LOG_CMDFIFO) logerror("  JMP LOCAL FRAMEBUF $%06X\n", target);
    					srcPos = target;
    					break;
    
    				case 4:     /* JMP AGP */
    					//if (LOG_CMDFIFO) logerror("  JMP AGP $%06X\n", target);
    					Log.exit("JMP AGP in CMDFIFO!\n");
    					srcPos = target;
    					break;
    
    				default:
    					//mame_printf_debug("INVALID JUMP COMMAND!\n");
    					Log.exit("  INVALID JUMP COMMAND");
    					break;
    			}
    			break;
    
    		/*
    		    Packet type 1: 1 + N words
    
    		      Word  Bits
    		        0  31:16 = Number of words
    		        0    15  = Increment?
    		        0  14:3  = Register base
    		        0   2:0  = Packet type (1)
    		        1  31:0  = Data word
    		*/
    		case 1:
    
    			/* extract parameters */
    			count = command >> 16;
    			inc = (command >> 15) & 1;
    			target = (command >> 3) & 0xfff;
    
    			//if (LOG_CMDFIFO) logerror("  PACKET TYPE 1: count=%d inc=%d reg=%04X\n", count, inc, target);
    
    			if (type >= TYPE_VOODOO_BANSHEE && (target & 0x800)!=0)
    			{
    				//  Banshee/Voodoo3 2D register writes
    
    				/* loop over all registers and write them one at a time */
    				for (i = 0; i < count; i++, target += inc)
    				{
    					cycles += banshee_2d_w(target & 0xff, mem_readd(fbi.ram, srcPos));
    					//logerror("    2d reg: %03x = %08X\n", target & 0x7ff, *src);
    					srcPos+=4;
    				}
    			}
    			else
    			{
    				/* loop over all registers and write them one at a time */
    				for (i = 0; i < count; i++, target += inc) {
    					cycles += register_w(target, mem_readd(fbi.ram, srcPos));
                        srcPos+=4;
                    }
    			}
    			break;
    
    		/*
    		    Packet type 2: 1 + N words
    
    		      Word  Bits
    		        0  31:3  = 2D Register mask
    		        0   2:0  = Packet type (2)
    		        1  31:0  = Data word
    		*/
    		case 2:
    			//if (LOG_CMDFIFO) logerror("  PACKET TYPE 2: mask=%X\n", (command >> 3) & 0x1ffffff);
    
    			/* loop over all registers and write them one at a time */
    			for (i = 3; i <= 31; i++)
    				if ((command & (1 << i))!=0) {
    					cycles += register_w(bltSrcBaseAddr + (i - 3), mem_readd(fbi.ram, srcPos));
                        srcPos+=4;
                    }
    			break;
    
    		/*
    		    Packet type 3: 1 + N words
    
    		      Word  Bits
    		        0  31:29 = Number of dummy entries following the data
    		        0   28   = Packed color data?
    		        0   25   = Disable ping pong sign correction (0=normal, 1=disable)
    		        0   24   = Culling sign (0=positive, 1=negative)
    		        0   23   = Enable culling (0=disable, 1=enable)
    		        0   22   = Strip mode (0=strip, 1=fan)
    		        0   17   = Setup S1 and T1
    		        0   16   = Setup W1
    		        0   15   = Setup S0 and T0
    		        0   14   = Setup W0
    		        0   13   = Setup Wb
    		        0   12   = Setup Z
    		        0   11   = Setup Alpha
    		        0   10   = Setup RGB
    		        0   9:6  = Number of vertices
    		        0   5:3  = Command (0=Independent tris, 1=Start new strip, 2=Continue strip)
    		        0   2:0  = Packet type (3)
    		        1  31:0  = Data word
    		*/
    		case 3:
    
    			/* extract parameters */
    			count = (command >> 6) & 15;
    			code = (command >> 3) & 7;
    
    			//if (LOG_CMDFIFO) logerror("  PACKET TYPE 3: count=%d code=%d mask=%03X smode=%02X pc=%d\n", count, code, (command >> 10) & 0xfff, (command >> 22) & 0x3f, (command >> 28) & 1);
    
    			/* copy relevant bits into the setup mode register */
    			reg[sSetupMode] = ((command >> 10) & 0xff) | ((command >> 6) & 0xf0000);
    
    			/* loop over triangles */
    			for (i = 0; i < count; i++)
    			{
    				/* always extract X/Y */
    				svert.x = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				svert.y = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    
    				/* load ARGB values if packed */
    				if ((command & (1 << 28))!=0)
    				{
    					if ((command & (3 << 10))!=0)
    					{
    						int argb = mem_readd(fbi.ram, srcPos);srcPos+=4;
    						if ((command & (1 << 10))!=0)
    						{
    							svert.r = RGB_RED(argb);
    							svert.g = RGB_GREEN(argb);
    							svert.b = RGB_BLUE(argb);
    						}
    						if ((command & (1 << 11))!=0)
    							svert.a = RGB_ALPHA(argb);
    					}
    				}
    
    				/* load ARGB values if not packed */
    				else
    				{
    					if ((command & (1 << 10))!=0)
    					{
    						svert.r = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    						svert.g = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    						svert.b = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    					}
    					if ((command & (1 << 11))!=0)
    						svert.a = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				}
    
    				/* load Z and Wb values */
    				if ((command & (1 << 12))!=0)
    					svert.z = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				if ((command & (1 << 13))!=0)
    					svert.wb = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    
    				/* load W0, S0, T0 values */
    				if ((command & (1 << 14))!=0)
    					svert.w0 = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				if ((command & (1 << 15))!=0)
    				{
    					svert.s0 = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    					svert.t0 = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				}
    
    				/* load W1, S1, T1 values */
    				if ((command & (1 << 16))!=0)
    					svert.w1 = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				if ((command & (1 << 17))!=0)
    				{
    					svert.s1 = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    					svert.t1 = Float.intBitsToFloat(mem_readd(fbi.ram, srcPos));srcPos+=4;
    				}
    
    				/* if we're starting a new strip, or if this is the first of a set of verts */
    				/* for a series of individual triangles, initialize all the verts */
    				if ((code == 1 && i == 0) || (code == 0 && i % 3 == 0))
    				{
    					fbi.sverts = 1;
    					fbi.svert[0] = fbi.svert[1] = fbi.svert[2] = svert;
    				}
    
    				/* otherwise, add this to the list */
    				else
    				{
    					/* for strip mode, shuffle vertex 1 down to 0 */
    					if ((command & (1 << 22))==0)
    						fbi.svert[0] = fbi.svert[1];
    
    					/* copy 2 down to 1 and add our new one regardless */
    					fbi.svert[1] = fbi.svert[2];
    					fbi.svert[2] = svert;
    
    					/* if we have enough, draw */
    					if (++fbi.sverts >= 3)
    						cycles += setup_and_draw_triangle();
    				}
    			}
    
    			/* account for the extra dummy words */
    			srcPos += 4*(command >> 29);
    			break;
    
    		/*
    		    Packet type 4: 1 + N words
    
    		      Word  Bits
    		        0  31:29 = Number of dummy entries following the data
    		        0  28:15 = General register mask
    		        0  14:3  = Register base
    		        0   2:0  = Packet type (4)
    		        1  31:0  = Data word
    		*/
    		case 4:
    
    			/* extract parameters */
    			target = (command >> 3) & 0xfff;
    
    			//if (LOG_CMDFIFO) logerror("  PACKET TYPE 4: mask=%X reg=%04X pad=%d\n", (command >> 15) & 0x3fff, target, command >> 29);
    
    			if (type >= TYPE_VOODOO_BANSHEE && (target & 0x800)!=0)
    			{
    				//  Banshee/Voodoo3 2D register writes
    
    				/* loop over all registers and write them one at a time */
    				for (i = 15; i <= 28; i++)
    				{
    					if ((command & (1 << i))!=0)
    					{
    						cycles += banshee_2d_w(target & 0xff, mem_readd(fbi.ram, srcPos));
    						//logerror("    2d reg: %03x = %08X\n", target & 0x7ff, *src);
    						srcPos+=4;
    					}
    				}
    			}
    			else
    			{
    				/* loop over all registers and write them one at a time */
    				for (i = 15; i <= 28; i++)
    					if ((command & (1 << i))!=0) {
    						cycles += register_w(target + (i - 15), mem_readd(fbi.ram, srcPos));
                            srcPos+=4;
                        }
    			}
    
    			/* account for the extra dummy words */
    			srcPos += 4*(command >> 29);
    			break;
    
    		/*
    		    Packet type 5: 2 + N words
    
    		      Word  Bits
    		        0  31:30 = Space (0,1=reserved, 2=LFB, 3=texture)
    		        0  29:26 = Byte disable W2
    		        0  25:22 = Byte disable WN
    		        0  21:3  = Num words
    		        0   2:0  = Packet type (5)
    		        1  31:30 = Reserved
    		        1  29:0  = Base address [24:0]
    		        2  31:0  = Data word
    		*/
    		case 5:
    
    			/* extract parameters */
    			count = (command >> 3) & 0x7ffff;
    			target = mem_readd(fbi.ram, srcPos) / 4;
                srcPos+=4;
    
    			/* handle LFB writes */
    			switch (command >> 30)
    			{
    				case 0:     // Linear FB
    				{
    					//if (LOG_CMDFIFO) logerror("  PACKET TYPE 5: FB count=%d dest=%08X bd2=%X bdN=%X\n", count, target, (command >> 26) & 15, (command >> 22) & 15);
    
    					int addr = target * 4;
    					for (i=0; i < count; i++)
    					{
    						int data = mem_readd(fbi.ram, srcPos);srcPos+=4;
    
    						fbi.ram[addr + 0] = (byte)(data);
    						fbi.ram[addr + 1] = (byte)(data >> 8);
    						fbi.ram[addr + 2] = (byte)(data >> 16);
    						fbi.ram[addr + 3] = (byte)(data >> 24);
    
    						addr += 4;
    					}
    					break;
    				}
    				case 2:     // 3D LFB
    				{
    					//if (LOG_CMDFIFO) logerror("  PACKET TYPE 5: 3D LFB count=%d dest=%08X bd2=%X bdN=%X\n", count, target, (command >> 26) & 15, (command >> 22) & 15);
    
    					/* loop over words */
    					for (i = 0; i < count; i++) {
    						cycles += lfb_w(target++, mem_readd(fbi.ram, srcPos), 0xffffffff, false);
                            srcPos+=4;
                        }
    					break;
    				}
    
    				case 1:     // Planar YUV
    				{
    					// TODO
    
    					/* just update the pointers for now */
    					for (i = 0; i < count; i++)
    					{
    						target++;
    						srcPos+=4;
    					}
    
    					break;
    				}
    
    				case 3:     // Texture Port
    				{
    					//if (LOG_CMDFIFO) logerror("  PACKET TYPE 5: textureRAM count=%d dest=%08X bd2=%X bdN=%X\n", count, target, (command >> 26) & 15, (command >> 22) & 15);
    
    					/* loop over words */
    					for (i = 0; i < count; i++) {
    						cycles += texture_w(target++, mem_readd(fbi.ram, srcPos));
                            srcPos+=4;
                        }
    
    					break;
    				}
    			}
    
    			break;
    
    		default:
    			//fprintf(stderr, "PACKET TYPE %d\n", command & 7);
    			break;
    	}
    
    	/* by default just update the read pointer past all the data we consumed */
    	f.rdptr = srcPos;
    	return cycles;
    }
    
    
    
    /*************************************
     *
     *  Handle execution if we're ready
     *
     *************************************/
    
    private int cmdfifo_execute_if_ready(cmdfifo_info f)
    {
    	int needed_depth;
    	int cycles;
    
    	/* all CMDFIFO commands need at least one word */
    	if (f.depth == 0)
    		return -1;
    
    	/* see if we have enough for the current command */
    	needed_depth = cmdfifo_compute_expected_depth(f);
    	if (f.depth < needed_depth)
    		return -1;
    
    	/* execute */
    	cycles = cmdfifo_execute(f);
    	f.depth -= needed_depth;
    	return cycles;
    }
    
    
    
    /*************************************
     *
     *  Handle writes to the CMD FIFO
     *
     *************************************/
    
    private void cmdfifo_w(cmdfifo_info f, int offset, int data)
    {
    	int addr = f.base + offset * 4;
    
    	//if (LOG_CMDFIFO_VERBOSE) logerror("CMDFIFO_w(%04X) = %08X\n", offset, data);
    
    	/* write the data */
    	if (addr < f.end)
            mem_writed(fbi.ram, addr, data);
    
    	/* count holes? */
    	if (f.count_holes)
    	{
    		/* in-order, no holes */
    		if (f.holes == 0 && addr == f.amin + 4)
    		{
    			f.amin = f.amax = addr;
    			f.depth++;
    		}
    
    		/* out-of-order, below the minimum */
    		else if (addr < f.amin)
    		{
//    			if (f.holes != 0)
//    				logerror("Unexpected CMDFIFO: AMin=%08X AMax=%08X Holes=%d WroteTo:%08X\n",
//    						f.amin, f.amax, f.holes, addr);
    			f.amin = f.amax = addr;
    			f.depth++;
    		}
    
    		/* out-of-order, but within the min-max range */
    		else if (addr < f.amax)
    		{
    			f.holes--;
    			if (f.holes == 0)
    			{
    				f.depth += (f.amax - f.amin) / 4;
    				f.amin = f.amax;
    			}
    		}
    
    		/* out-of-order, bumping max */
    		else
    		{
    			f.holes += (addr - f.amax) / 4 - 1;
    			f.amax = addr;
    		}
    	}
    
    	/* execute if we can */
    	if (!pci.op_pending)
    	{
    		int cycles = cmdfifo_execute_if_ready(f);
    		if (cycles > 0)
    		{
    			pci.op_pending = true;
    			pci.op_end_time = System.nanoTime() + cycles * attoseconds_per_cycle;
    
//    			if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:direct write start at %d.%08X%08X end at %d.%08X%08X\n", index,
//    				device.machine().time().seconds, (UINT32)(device.machine().time().attoseconds >> 32), (UINT32)device.machine().time().attoseconds,
//    				pci.op_end_time.seconds, (UINT32)(pci.op_end_time.attoseconds >> 32), (UINT32)pci.op_end_time.attoseconds);
    		}
    	}
    }
    
    /*************************************
     *
     *  Flush data from the FIFOs
     *
     *************************************/
    static boolean in_flush = false;
    
    private void flush_fifos()
    {
        long current_time = System.nanoTime();

        /* check for recursive calls */
        if (in_flush)
            return;
        in_flush = true;

        if (!pci.op_pending) Log.exit("flush_fifos called with no pending operation");

//    	if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:flush_fifos start -- pending=%d.%08X%08X cur=%d.%08X%08X\n", index,
//    		pci.op_end_time.seconds, (UINT32)(pci.op_end_time.attoseconds >> 32), (UINT32)pci.op_end_time.attoseconds,
//    		current_time.seconds, (UINT32)(current_time.attoseconds >> 32), (UINT32)current_time.attoseconds);

        /* loop while we still have cycles to burn */
        while (pci.op_end_time <= current_time)
        {
            int extra_cycles = 0;
            int cycles;

            /* loop over 0-cycle stuff; this constitutes the bulk of our writes */
            do
            {
                fifo_state fifo;
                int address;
                int data;

                /* we might be in CMDFIFO mode */
                if (fbi.cmdfifo[0].enable)
                {
                    /* if we don't have anything to execute, we're done for now */
                    cycles = cmdfifo_execute_if_ready(fbi.cmdfifo[0]);
                    if (cycles == -1)
                    {
                        pci.op_pending = false;
                        in_flush = false;
                        //if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:flush_fifos end -- CMDFIFO empty\n", index);
                        return;
                    }
                }
                else if (fbi.cmdfifo[1].enable)
                {
                    /* if we don't have anything to execute, we're done for now */
                    cycles = cmdfifo_execute_if_ready(fbi.cmdfifo[1]);
                    if (cycles == -1)
                    {
                        pci.op_pending = false;
                        in_flush = false;
                        //if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:flush_fifos end -- CMDFIFO empty\n", index);
                        return;
                    }
                }

                /* else we are in standard PCI/memory FIFO mode */
                else
                {
                    /* choose which FIFO to read from */
                    if (!fifo_empty(fbi.fifo))
                        fifo = fbi.fifo;
                    else if (!fifo_empty(pci.fifo))
                        fifo = pci.fifo;
                    else
                    {
                        pci.op_pending = false;
                        in_flush = false;
                        //if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:flush_fifos end -- FIFOs empty\n", index);
                        return;
                    }

                    /* extract address and data */
                    address = fifo_remove(fifo);
                    data = fifo_remove(fifo);

                    /* target the appropriate location */
                    if ((address & (0xc00000/4)) == 0)
                        cycles = register_w(address, data);
                    else if ((address & (0x800000/4))!=0)
                        cycles = texture_w(address, data);
                    else
                    {
                        int mem_mask = 0xffffffff;

                        /* compute mem_mask */
                        if ((address & 0x80000000)!=0)
                            mem_mask &= 0x0000ffff;
                        if ((address & 0x40000000)!=0)
                            mem_mask &= 0xffff0000;
                        address &= 0xffffff;

                        cycles = lfb_w(address, data, mem_mask, false);
                    }
                }

                /* accumulate smaller operations */
                if (cycles < ACCUMULATE_THRESHOLD)
                {
                    extra_cycles += cycles;
                    cycles = 0;
                }
            }
            while (cycles == 0);

            /* account for extra cycles */
            cycles += extra_cycles;

            /* account for those cycles */
            pci.op_end_time += (cycles * attoseconds_per_cycle);

//    		if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:update -- pending=%d.%08X%08X cur=%d.%08X%08X\n", index,
//    			pci.op_end_time.seconds, (UINT32)(pci.op_end_time.attoseconds >> 32), (UINT32)pci.op_end_time.attoseconds,
//    			current_time.seconds, (UINT32)(current_time.attoseconds >> 32), (UINT32)current_time.attoseconds);
        }

//    	if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:flush_fifos end -- pending command complete at %d.%08X%08X\n", index,
//    		pci.op_end_time.seconds, (UINT32)(pci.op_end_time.attoseconds >> 32), (UINT32)pci.op_end_time.attoseconds);

        in_flush = false;
    }

    private int banshee_2d_w(int offset, int data) {
        Log.exit("banshee_2d_w not implemented");
        return 0;
    }
    private static int count = 0;
    private int register_w(int offset, int data) {
        int origdata = data;
        int cycles = 0;
        long data64;
        int regnum;
        int chips;
    
        /* statistics */
        stats.reg_writes++;
    
        /* determine which chips we are addressing */
        chips = (offset >> 8) & 0xf;
        if (chips == 0)
            chips = 0xf;
        chips &= chipmask;
    
        /* the first 64 registers can be aliased differently */
        if ((offset & 0x800c0) == 0x80000 && alt_regmap)
            regnum = register_alias_map[offset & 0x3f];
        else
            regnum = offset & 0xff;
    
        /* first make sure this register is readable */
        if ((regaccess[regnum] & REGISTER_WRITE)==0)
        {
            Log.log_msg("VOODOO."+this.pci_id+".ERROR:Invalid attempt to write "+regnames[regnum]);
            return 0;
        }
        //System.out.print(count + " write " + regnames[regnum] + " " + data);
        count++;
        /* switch off the register */
        switch (regnum)
        {
            /* Vertex data is 12.4 formatted fixed point */
            case fvertexAx:
                data = float_to_int32(data, 4);
            case vertexAx:
                if ((chips & 1)!=0) fbi.ax = (short)data;
                break;
    
            case fvertexAy:
                data = float_to_int32(data, 4);
            case vertexAy:
                if ((chips & 1)!=0) fbi.ay = (short)data;
                break;
    
            case fvertexBx:
                data = float_to_int32(data, 4);
            case vertexBx:
                if ((chips & 1)!=0) fbi.bx = (short)data;
                break;
    
            case fvertexBy:
                data = float_to_int32(data, 4);
            case vertexBy:
                if ((chips & 1)!=0) fbi.by = (short)data;
                break;
    
            case fvertexCx:
                data = float_to_int32(data, 4);
            case vertexCx:
                if ((chips & 1)!=0) fbi.cx = (short)data;
                break;
    
            case fvertexCy:
                data = float_to_int32(data, 4);
            case vertexCy:
                if ((chips & 1)!=0) fbi.cy = (short)data;
                break;
    
            /* RGB data is 12.12 formatted fixed point */
            case fstartR:
                data = float_to_int32(data, 12);
            case startR:
                if ((chips & 1)!=0) fbi.startr = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fstartG:
                data = float_to_int32(data, 12);
            case startG:
                if ((chips & 1)!=0) fbi.startg = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fstartB:
                data = float_to_int32(data, 12);
            case startB:
                if ((chips & 1)!=0) fbi.startb = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fstartA:
                data = float_to_int32(data, 12);
            case startA:
                if ((chips & 1)!=0) fbi.starta = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdRdX:
                data = float_to_int32(data, 12);
            case dRdX:
                if ((chips & 1)!=0) fbi.drdx = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdGdX:
                data = float_to_int32(data, 12);
            case dGdX:
                if ((chips & 1)!=0) fbi.dgdx = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdBdX:
                data = float_to_int32(data, 12);
            case dBdX:
                if ((chips & 1)!=0) fbi.dbdx = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdAdX:
                data = float_to_int32(data, 12);
            case dAdX:
                if ((chips & 1)!=0) fbi.dadx = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdRdY:
                data = float_to_int32(data, 12);
            case dRdY:
                if ((chips & 1)!=0) fbi.drdy = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdGdY:
                data = float_to_int32(data, 12);
            case dGdY:
                if ((chips & 1)!=0) fbi.dgdy = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdBdY:
                data = float_to_int32(data, 12);
            case dBdY:
                if ((chips & 1)!=0) fbi.dbdy = (data << 8) >> 8; // intentional signed shift
                break;
    
            case fdAdY:
                data = float_to_int32(data, 12);
            case dAdY:
                if ((chips & 1)!=0) fbi.dady = (data << 8) >> 8; // intentional signed shift
                break;
    
            /* Z data is 20.12 formatted fixed point */
            case fstartZ:
                data = float_to_int32(data, 12);
            case startZ:
                if ((chips & 1)!=0) fbi.startz = data;
                break;
    
            case fdZdX:
                data = float_to_int32(data, 12);
            case dZdX:
                if ((chips & 1)!=0) fbi.dzdx = data;
                break;
    
            case fdZdY:
                data = float_to_int32(data, 12);
            case dZdY:
                if ((chips & 1)!=0) fbi.dzdy = data;
                break;
    
            /* S,T data is 14.18 formatted fixed point, converted to 16.32 internally */
            case fstartS:
                data64 = float_to_long(data, 32);
                if ((chips & 2)!=0) tmu[0].starts = data64;
                if ((chips & 4)!=0) tmu[1].starts = data64;
                break;
            case startS:
                if ((chips & 2)!=0) tmu[0].starts = (long)data << 14;
                if ((chips & 4)!=0) tmu[1].starts = (long)data << 14;
                break;
    
            case fstartT:
                data64 = float_to_long(data, 32);
                if ((chips & 2)!=0) tmu[0].startt = data64;
                if ((chips & 4)!=0) tmu[1].startt = data64;
                break;
            case startT:
                if ((chips & 2)!=0) tmu[0].startt = (long)data << 14;
                if ((chips & 4)!=0) tmu[1].startt = (long)data << 14;
                break;
    
            case fdSdX:
                data64 = float_to_long(data, 32);
                if ((chips & 2)!=0) tmu[0].dsdx = data64;
                if ((chips & 4)!=0) tmu[1].dsdx = data64;
                break;
            case dSdX:
                if ((chips & 2)!=0) tmu[0].dsdx = (long)data << 14;
                if ((chips & 4)!=0) tmu[1].dsdx = (long)data << 14;
                break;
    
            case fdTdX:
                data64 = float_to_long(data, 32);
                if ((chips & 2)!=0) tmu[0].dtdx = data64;
                if ((chips & 4)!=0) tmu[1].dtdx = data64;
                break;
            case dTdX:
                if ((chips & 2)!=0) tmu[0].dtdx = (long)data << 14;
                if ((chips & 4)!=0) tmu[1].dtdx = (long)data << 14;
                break;
    
            case fdSdY:
                data64 = float_to_long(data, 32);
                if ((chips & 2)!=0) tmu[0].dsdy = data64;
                if ((chips & 4)!=0) tmu[1].dsdy = data64;
                break;
            case dSdY:
                if ((chips & 2)!=0) tmu[0].dsdy = (long)data << 14;
                if ((chips & 4)!=0) tmu[1].dsdy = (long)data << 14;
                break;
    
            case fdTdY:
                data64 = float_to_long(data, 32);
                if ((chips & 2)!=0) tmu[0].dtdy = data64;
                if ((chips & 4)!=0) tmu[1].dtdy = data64;
                break;
            case dTdY:
                if ((chips & 2)!=0) tmu[0].dtdy = (long)data << 14;
                if ((chips & 4)!=0) tmu[1].dtdy = (long)data << 14;
                break;
    
            /* W data is 2.30 formatted fixed point, converted to 16.32 internally */
            case fstartW:
                data64 = float_to_long(data, 32);
                if ((chips & 1)!=0) fbi.startw = data64;
                if ((chips & 2)!=0) tmu[0].startw = data64;
                if ((chips & 4)!=0) tmu[1].startw = data64;
                break;
            case startW:
                if ((chips & 1)!=0) fbi.startw = (long)data << 2;
                if ((chips & 2)!=0) tmu[0].startw = (long)data << 2;
                if ((chips & 4)!=0) tmu[1].startw = (long)data << 2;
                break;
    
            case fdWdX:
                data64 = float_to_long(data, 32);
                if ((chips & 1)!=0) fbi.dwdx = data64;
                if ((chips & 2)!=0) tmu[0].dwdx = data64;
                if ((chips & 4)!=0) tmu[1].dwdx = data64;
                break;
            case dWdX:
                if ((chips & 1)!=0) fbi.dwdx = (long)data << 2;
                if ((chips & 2)!=0) tmu[0].dwdx = (long)data << 2;
                if ((chips & 4)!=0) tmu[1].dwdx = (long)data << 2;
                break;
    
            case fdWdY:
                data64 = float_to_long(data, 32);
                if ((chips & 1)!=0) fbi.dwdy = data64;
                if ((chips & 2)!=0) tmu[0].dwdy = data64;
                if ((chips & 4)!=0) tmu[1].dwdy = data64;
                break;
            case dWdY:
                if ((chips & 1)!=0) fbi.dwdy = (long)data << 2;
                if ((chips & 2)!=0) tmu[0].dwdy = (long)data << 2;
                if ((chips & 4)!=0) tmu[1].dwdy = (long)data << 2;
                break;
    
            /* setup bits */
            case sARGB:
                if ((chips & 1)!=0)
                {
                    reg[sAlpha] = setRegAsFloat(RGB_ALPHA(data));
                    reg[sRed] = setRegAsFloat(RGB_RED(data));
                    reg[sGreen] = setRegAsFloat(RGB_GREEN(data));
                    reg[sBlue] = setRegAsFloat(RGB_BLUE(data));
                }
                break;
    
            /* mask off invalid bits for different cards */
            case fbzColorPath:
                Poly.poly_wait(poly, regnames[regnum]);
                if (type < TYPE_VOODOO_2)
                    data &= 0x0fffffff;
                if ((chips & 1)!=0) reg[fbzColorPath] = data;
                break;
    
            case fbzMode:
                Poly.poly_wait(poly, regnames[regnum]);
                if (type < TYPE_VOODOO_2)
                    data &= 0x001fffff;
                if ((chips & 1)!=0) reg[fbzMode] = data;
                break;
    
            case fogMode:
                Poly.poly_wait(poly, regnames[regnum]);
                if (type < TYPE_VOODOO_2)
                    data &= 0x0000003f;
                if ((chips & 1)!=0) reg[fogMode] = data;
                break;
    
            /* triangle drawing */
            case triangleCMD:
                fbi.cheating_allowed = (fbi.ax != 0 || fbi.ay != 0 || fbi.bx > 50 || fbi.by != 0 || fbi.cx != 0 || fbi.cy > 50);
                fbi.sign = data;
                cycles = triangle();
                break;
    
            case ftriangleCMD:
                fbi.cheating_allowed = true;
                fbi.sign = data;
                cycles = triangle();
                break;
    
            case sBeginTriCMD:
                cycles = begin_triangle();
                break;
    
            case sDrawTriCMD:
                cycles = draw_triangle();
                break;
    
            /* other commands */
            case nopCMD:
                Poly.poly_wait(poly, regnames[regnum]);
                if ((data & 1)!=0)
                    reset_counters();
                if ((data & 2)!=0)
                    reg[fbiTrianglesOut] = 0;
                break;
    
            case fastfillCMD:
                cycles = fastfill();
                break;
    
            case swapbufferCMD:
                Poly.poly_wait(poly, regnames[regnum]);
                cycles = swapbuffer(data);
                break;
    
            case userIntrCMD:
                Poly.poly_wait(poly, regnames[regnum]);
                //fatalerror("userIntrCMD\n");
    
                reg[intrCtrl] |= 0x1800;
                reg[intrCtrl] &= ~0x80000000;
    
                // TODO: rename vblank_client for less confusion?
                if (fbi.vblank_client != null)
                    fbi.vblank_client.call(this, 1);
                break;
    
            /* gamma table access -- Voodoo/Voodoo2 only */
            case clutData:
                if (type <= TYPE_VOODOO_2 && (chips & 1)!=0)
                {
                    Poly.poly_wait(poly, regnames[regnum]);
                    if (!FBIINIT1_VIDEO_TIMING_RESET(reg[fbiInit1]))
                    {
                        int index = data >>> 24;
                        if (index <= 32)
                        {
                            fbi.clut[index] = data;
                            fbi.clut_dirty = true;
                        }
                    }
                    else
                        Log.log_msg("clutData ignored because video timing reset = 1");
                }
                break;
    
            /* external DAC access -- Voodoo/Voodoo2 only */
            case dacData:
                if (type <= TYPE_VOODOO_2 && (chips & 1)!=0)
                {
                    Poly.poly_wait(poly, regnames[regnum]);
                    if ((data & 0x800)==0)
                        dacdata_w(dac, (data >> 8) & 7, data & 0xff);
                    else
                        dacdata_r(dac, (data >> 8) & 7);
                }
                break;
    
            /* vertical sync rate -- Voodoo/Voodoo2 only */
            case hSync:
            case vSync:
            case backPorch:
            case videoDimensions:
                if (type <= TYPE_VOODOO_2 && (chips & 1)!=0)
                {
                    Poly.poly_wait(poly, regnames[regnum]);
                    reg[regnum] = data;
                    if (reg[hSync] != 0 && reg[vSync] != 0 && reg[videoDimensions] != 0)
                    {
                        int hvis, vvis, htotal, vtotal, hbp, vbp;
//                        attoseconds_t refresh = screen.frame_period().attoseconds;
//                        attoseconds_t stdperiod, medperiod, vgaperiod;
//                        attoseconds_t stddiff, meddiff, vgadiff;
                        rectangle visarea = new rectangle();
    
                        if (type == TYPE_VOODOO_2)
                        {
                            htotal = ((reg[hSync] >>> 16) & 0x7ff) + 1 + (reg[hSync] & 0x1ff) + 1;
                            vtotal = ((reg[vSync] >>> 16) & 0x1fff) + (reg[vSync] & 0x1fff);
                            hvis = (reg[videoDimensions] & 0x7ff );
                            vvis = (reg[videoDimensions] >>> 16) & 0x7ff;
                            hbp = (reg[backPorch] & 0x1ff) + 2;
                            vbp = (reg[backPorch] >>> 16) & 0x1ff;
                        }
                        else
                        {
                            htotal = ((reg[hSync] >>> 16) & 0x3ff) + 1 + (reg[hSync] & 0xff) + 1;
                            vtotal = ((reg[vSync] >>> 16) & 0xfff) + (reg[vSync] & 0xfff);
                            hvis = reg[videoDimensions] & 0x3ff;
                            vvis = (reg[videoDimensions] >>> 16) & 0x3ff;
                            hbp = (reg[backPorch] & 0xff) + 2;
                            vbp = (reg[backPorch] >>> 16) & 0xff;
                        }
    
                        /* create a new visarea */
                        visarea.set(hbp, hbp + hvis - 1, vbp, vbp + vvis - 1);
    
                        /* keep within bounds */
                        visarea.max_x = Math.min(visarea.max_x, htotal - 1);
                        visarea.max_y = Math.min(visarea.max_y, vtotal - 1);
    
                        /* compute the new period for standard res, medium res, and VGA res */
//                        stdperiod = HZ_TO_ATTOSECONDS(15750) * vtotal;
//                        medperiod = HZ_TO_ATTOSECONDS(25000) * vtotal;
//                        vgaperiod = HZ_TO_ATTOSECONDS(31500) * vtotal;
    
                        /* compute a diff against the current refresh period */
//                        stddiff = stdperiod - refresh;
//                        if (stddiff < 0) stddiff = -stddiff;
//                        meddiff = medperiod - refresh;
//                        if (meddiff < 0) meddiff = -meddiff;
//                        vgadiff = vgaperiod - refresh;
//                        if (vgadiff < 0) vgadiff = -vgadiff;
    
//                        mame_printf_debug("hSync=%08X  vSync=%08X  backPorch=%08X  videoDimensions=%08X\n",
//                            reg[hSync].u, reg[vSync].u, reg[backPorch].u, reg[videoDimensions].u);
//                        mame_printf_debug("Horiz: %d-%d (%d total)  Vert: %d-%d (%d total) -- ", visarea.min_x, visarea.max_x, htotal, visarea.min_y, visarea.max_y, vtotal);
    
                        /* configure the screen based on which one matches the closest */
//                        if (stddiff < meddiff && stddiff < vgadiff)
//                        {
//                            screen.configure(htotal, vtotal, visarea, stdperiod);
//                            mame_printf_debug("Standard resolution, %f Hz\n", ATTOSECONDS_TO_HZ(stdperiod));
//                        }
//                        else if (meddiff < vgadiff)
//                        {
//                            screen.configure(htotal, vtotal, visarea, medperiod);
//                            mame_printf_debug("Medium resolution, %f Hz\n", ATTOSECONDS_TO_HZ(medperiod));
//                        }
//                        else
//                        {
//                            screen.configure(htotal, vtotal, visarea, vgaperiod);
//                            mame_printf_debug("VGA resolution, %f Hz\n", ATTOSECONDS_TO_HZ(vgaperiod));
//                        }
                        /* configure the new framebuffer info */
                        fbi.width = hvis;
                        fbi.height = vvis;
                        fbi.xoffs = hbp;
                        fbi.yoffs = vbp;
                        fbi.vsyncscan = (reg[vSync] >> 16) & 0xfff;

                        /* recompute the time of VBLANK */
                        adjust_vblank_timer();
    
                        /* if changing dimensions, update video memory layout */
                        if (regnum == videoDimensions)
                            recompute_video_memory();
                        Voodoo_UpdateScreenStart();
                    }
                }
                break;
    
            /* fbiInit0 can only be written if initEnable says we can -- Voodoo/Voodoo2 only */
            case fbiInit0:
                Poly.poly_wait(poly, regnames[regnum]);
                Voodoo_Output_Enable((data&1)!=0);
                if (type <= TYPE_VOODOO_2 && (chips & 1)!=0 && INITEN_ENABLE_HW_INIT(pci.init_enable))
                {
                    reg[fbiInit0] = data;
                    if (FBIINIT0_GRAPHICS_RESET(data))
                        soft_reset();
                    if (FBIINIT0_FIFO_RESET(data))
                        fifo_reset(pci.fifo);
                    recompute_video_memory();
                }
                break;
    
            /* fbiInit5-7 are Voodoo 2-only; ignore them on anything else */
            case fbiInit5:
            case fbiInit6:
                if (type < TYPE_VOODOO_2)
                    break;
                /* else fall through... */
    
            /* fbiInitX can only be written if initEnable says we can -- Voodoo/Voodoo2 only */
            /* most of these affect memory layout, so always recompute that when done */
            case fbiInit1:
            case fbiInit2:
            case fbiInit4:
                Poly.poly_wait(poly, regnames[regnum]);
                if (type <= TYPE_VOODOO_2 && (chips & 1)!=0 && INITEN_ENABLE_HW_INIT(pci.init_enable))
                {
                    reg[regnum] = data;
                    recompute_video_memory();
                    fbi.video_changed = true;
                }
                break;
    
            case fbiInit3:
                Poly.poly_wait(poly, regnames[regnum]);
                if (type <= TYPE_VOODOO_2 && (chips & 1)!=0 && INITEN_ENABLE_HW_INIT(pci.init_enable))
                {
                    reg[regnum] = data;
                    alt_regmap = FBIINIT3_TRI_REGISTER_REMAP(data);
                    fbi.yorigin = FBIINIT3_YORIGIN_SUBTRACT(reg[fbiInit3]);
                    recompute_video_memory();
                }
                break;
    
            case fbiInit7:
    /*      case swapPending: -- Banshee */
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0 && INITEN_ENABLE_HW_INIT(pci.init_enable))
                {
                    Poly.poly_wait(poly, regnames[regnum]);
                    reg[regnum] = data;
                    fbi.cmdfifo[0].enable = FBIINIT7_CMDFIFO_ENABLE(data);
                    fbi.cmdfifo[0].count_holes = !FBIINIT7_DISABLE_CMDFIFO_HOLES(data);
                }
                else if (type >= TYPE_VOODOO_BANSHEE)
                    fbi.swaps_pending++;
                break;
    
            /* cmdFifo -- Voodoo2 only */
            case cmdFifoBaseAddr:
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                {
                    Poly.poly_wait(poly, regnames[regnum]);
                    reg[regnum] = data;
                    fbi.cmdfifo[0].base = (data & 0x3ff) << 12;
                    fbi.cmdfifo[0].end = (((data >> 16) & 0x3ff) + 1) << 12;
                }
                break;
    
            case cmdFifoBump:
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                    Log.exit("cmdFifoBump");
                break;
    
            case cmdFifoRdPtr:
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                    fbi.cmdfifo[0].rdptr = data;
                break;
    
            case cmdFifoAMin:
    /*      case colBufferAddr: -- Banshee */
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                    fbi.cmdfifo[0].amin = data;
                else if (type >= TYPE_VOODOO_BANSHEE && (chips & 1)!=0)
                    fbi.rgboffs[1] = data & fbi.mask & ~0x0f;
                break;
    
            case cmdFifoAMax:
    /*      case colBufferStride: -- Banshee */
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                    fbi.cmdfifo[0].amax = data;
                else if (type >= TYPE_VOODOO_BANSHEE && (chips & 1)!=0)
                {
                    if ((data & 0x8000)!=0)
                        fbi.rowpixels = (data & 0x7f) << 6;
                    else
                        fbi.rowpixels = (data & 0x3fff) >> 1;
                }
                break;
    
            case cmdFifoDepth:
    /*      case auxBufferAddr: -- Banshee */
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                    fbi.cmdfifo[0].depth = data;
                else if (type >= TYPE_VOODOO_BANSHEE && (chips & 1)!=0)
                    fbi.auxoffs = data & fbi.mask & ~0x0f;
                break;
    
            case cmdFifoHoles:
    /*      case auxBufferStride: -- Banshee */
                if (type == TYPE_VOODOO_2 && (chips & 1)!=0)
                    fbi.cmdfifo[0].holes = data;
                else if (type >= TYPE_VOODOO_BANSHEE && (chips & 1)!=0)
                {
                    int rowpixels;
    
                    if ((data & 0x8000)!=0)
                        rowpixels = (data & 0x7f) << 6;
                    else
                        rowpixels = (data & 0x3fff) >> 1;
                    if (fbi.rowpixels != rowpixels)
                        Log.exit("aux buffer stride differs from color buffer stride");
                }
                break;
    
            /* nccTable entries are processed and expanded immediately */
            case nccTable+0:
            case nccTable+1:
            case nccTable+2:
            case nccTable+3:
            case nccTable+4:
            case nccTable+5:
            case nccTable+6:
            case nccTable+7:
            case nccTable+8:
            case nccTable+9:
            case nccTable+10:
            case nccTable+11:
                Poly.poly_wait(poly, regnames[regnum]);
                if ((chips & 2)!=0) ncc_table_write(tmu[0].ncc[0], regnum - nccTable, data);
                if ((chips & 4)!=0) ncc_table_write(tmu[1].ncc[0], regnum - nccTable, data);
                break;
    
            case nccTable+12:
            case nccTable+13:
            case nccTable+14:
            case nccTable+15:
            case nccTable+16:
            case nccTable+17:
            case nccTable+18:
            case nccTable+19:
            case nccTable+20:
            case nccTable+21:
            case nccTable+22:
            case nccTable+23:
                Poly.poly_wait(poly, regnames[regnum]);
                if ((chips & 2)!=0) ncc_table_write(tmu[0].ncc[1], regnum - (nccTable+12), data);
                if ((chips & 4)!=0) ncc_table_write(tmu[1].ncc[1], regnum - (nccTable+12), data);
                break;
    
            /* fogTable entries are processed and expanded immediately */
            case fogTable+0:
            case fogTable+1:
            case fogTable+2:
            case fogTable+3:
            case fogTable+4:
            case fogTable+5:
            case fogTable+6:
            case fogTable+7:
            case fogTable+8:
            case fogTable+9:
            case fogTable+10:
            case fogTable+11:
            case fogTable+12:
            case fogTable+13:
            case fogTable+14:
            case fogTable+15:
            case fogTable+16:
            case fogTable+17:
            case fogTable+18:
            case fogTable+19:
            case fogTable+20:
            case fogTable+21:
            case fogTable+22:
            case fogTable+23:
            case fogTable+24:
            case fogTable+25:
            case fogTable+26:
            case fogTable+27:
            case fogTable+28:
            case fogTable+29:
            case fogTable+30:
            case fogTable+31:
                Poly.poly_wait(poly, regnames[regnum]);
                if ((chips & 1)!=0)
                {
                    int base = 2 * (regnum - fogTable);
                    fbi.fogdelta[base + 0] = (data >> 0) & 0xff;
                    fbi.fogblend[base + 0] = (data >> 8) & 0xff;
                    fbi.fogdelta[base + 1] = (data >> 16) & 0xff;
                    fbi.fogblend[base + 1] = (data >> 24) & 0xff;
                }
                break;
    
            /* texture modifications cause us to recompute everything */
            case textureMode:
            case tLOD:
            case tDetail:
            case texBaseAddr:
            case texBaseAddr_1:
            case texBaseAddr_2:
            case texBaseAddr_3_8:
                Poly.poly_wait(poly, regnames[regnum]);
                if ((chips & 2)!=0)
                {
                    reg[tmu[0].reg+regnum] = data;
                    tmu[0].regdirty = true;
                }
                if ((chips & 4)!=0)
                {
                    reg[tmu[1].reg+regnum] = data;
                    tmu[1].regdirty = true;
                }
                break;
    
            /* these registers are referenced in the renderer; we must wait for pending work before changing */
            case chromaRange:
            case chromaKey:
            case alphaMode:
            case fogColor:
            case stipple:
            case zaColor:
            case color1:
            case color0:
            case clipLowYHighY:
            case clipLeftRight:
                Poly.poly_wait(poly, regnames[regnum]);
                /* fall through to default implementation */
    
            /* by default, just feed the data to the chips */
            default:
                if ((chips & 1)!=0) reg[0x000 + regnum] = data;
                if ((chips & 2)!=0) reg[0x100 + regnum] = data;
                if ((chips & 4)!=0) reg[0x200 + regnum] = data;
                if ((chips & 4)!=0) reg[0x300 + regnum] = data;
                break;
        }
    
//        if (LOG_REGISTERS)
//        {
//            if (regnum < fvertexAx || regnum > fdWdY)
//                logerror("VOODOO.%d.REG:%s(%d) write = %08X\n", index, (regnum < 0x384/4) ? regnames[regnum] : "oob", chips, origdata);
//            else
//                logerror("VOODOO.%d.REG:%s(%d) write = %f\n", index, (regnum < 0x384/4) ? regnames[regnum] : "oob", chips, u2f(origdata));
//        }
        //System.out.println(" cycles="+cycles);
        return cycles;
    }

    /*************************************
     *
     *  Voodoo LFB writes
     *
     *************************************/

    /* flags for LFB writes */
    static final private int LFB_RGB_PRESENT        = 1;
    static final private int LFB_ALPHA_PRESENT      = 2;
    static final private int LFB_DEPTH_PRESENT      = 4;
    static final private int LFB_DEPTH_PRESENT_MSW  = 8;

    final int[] tmp_sr=new int[2], tmp_sg=new int[2], tmp_sb=new int[2], tmp_sa=new int[2], tmp_sw=new int[2];
    private int lfb_w(int offset, int data, int mem_mask, boolean forcefront) {
        int destPos, depthPos;
        int destmax, depthmax;
        int x, y, scry, mask;
        int pix, destbuf;

        /* statistics */
        stats.lfb_writes++;

        /* byte swizzling */
        if (LFBMODE_BYTE_SWIZZLE_WRITES(reg[lfbMode]))
        {
            data = FLIPENDIAN_INT32(data);
            mem_mask = FLIPENDIAN_INT32(mem_mask);
        }

        /* word swapping */
        if (LFBMODE_WORD_SWAP_WRITES(reg[lfbMode]))
        {
            data = (data << 16) | (data >> 16);
            mem_mask = (mem_mask << 16) | (mem_mask >> 16);
        }

        /* extract default depth and alpha values */
        tmp_sw[0] = tmp_sw[1] = reg[zaColor] & 0xffff;
        tmp_sa[0] = tmp_sa[1] = reg[zaColor] >> 24;

        /* first extract A,R,G,B from the data */
        switch (LFBMODE_WRITE_FORMAT(reg[lfbMode]) + 16 * LFBMODE_RGBA_LANES(reg[lfbMode]))
        {
            case 16*0 + 0:      /* ARGB, 16-bit RGB 5-6-5 */
            case 16*2 + 0:      /* RGBA, 16-bit RGB 5-6-5 */
                EXTRACT_565_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                EXTRACT_565_TO_888(data >> 16, tmp_sr, tmp_sg, tmp_sb, 1);
                mask = LFB_RGB_PRESENT | (LFB_RGB_PRESENT << 4);
                offset <<= 1;
                break;
            case 16*1 + 0:      /* ABGR, 16-bit RGB 5-6-5 */
            case 16*3 + 0:      /* BGRA, 16-bit RGB 5-6-5 */
                EXTRACT_565_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                EXTRACT_565_TO_888(data >> 16, tmp_sb, tmp_sg, tmp_sr, 1);
                mask = LFB_RGB_PRESENT | (LFB_RGB_PRESENT << 4);
                offset <<= 1;
                break;

            case 16*0 + 1:      /* ARGB, 16-bit RGB x-5-5-5 */
                EXTRACT_x555_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                EXTRACT_x555_TO_888(data >> 16, tmp_sr, tmp_sg, tmp_sb, 1);
                mask = LFB_RGB_PRESENT | (LFB_RGB_PRESENT << 4);
                offset <<= 1;
                break;
            case 16*1 + 1:      /* ABGR, 16-bit RGB x-5-5-5 */
                EXTRACT_x555_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                EXTRACT_x555_TO_888(data >> 16, tmp_sb, tmp_sg, tmp_sr, 1);
                mask = LFB_RGB_PRESENT | (LFB_RGB_PRESENT << 4);
                offset <<= 1;
                break;
            case 16*2 + 1:      /* RGBA, 16-bit RGB x-5-5-5 */
                EXTRACT_555x_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                EXTRACT_555x_TO_888(data >> 16, tmp_sr, tmp_sg, tmp_sb, 1);
                mask = LFB_RGB_PRESENT | (LFB_RGB_PRESENT << 4);
                offset <<= 1;
                break;
            case 16*3 + 1:      /* BGRA, 16-bit RGB x-5-5-5 */
                EXTRACT_555x_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                EXTRACT_555x_TO_888(data >> 16, tmp_sb, tmp_sg, tmp_sr, 1);
                mask = LFB_RGB_PRESENT | (LFB_RGB_PRESENT << 4);
                offset <<= 1;
                break;

            case 16*0 + 2:      /* ARGB, 16-bit ARGB 1-5-5-5 */
                EXTRACT_1555_TO_8888(data, tmp_sa, tmp_sr, tmp_sg, tmp_sb, 0);
                EXTRACT_1555_TO_8888(data >> 16, tmp_sa, tmp_sr, tmp_sg, tmp_sb, 1);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | ((LFB_RGB_PRESENT | LFB_ALPHA_PRESENT) << 4);
                offset <<= 1;
                break;
            case 16*1 + 2:      /* ABGR, 16-bit ARGB 1-5-5-5 */
                EXTRACT_1555_TO_8888(data, tmp_sa, tmp_sb, tmp_sg, tmp_sr, 0);
                EXTRACT_1555_TO_8888(data >> 16, tmp_sa, tmp_sb, tmp_sg, tmp_sr, 1);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | ((LFB_RGB_PRESENT | LFB_ALPHA_PRESENT) << 4);
                offset <<= 1;
                break;
            case 16*2 + 2:      /* RGBA, 16-bit ARGB 1-5-5-5 */
                EXTRACT_5551_TO_8888(data, tmp_sr, tmp_sg, tmp_sb, tmp_sa, 0);
                EXTRACT_5551_TO_8888(data >> 16, tmp_sr, tmp_sg, tmp_sb, tmp_sa, 1);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | ((LFB_RGB_PRESENT | LFB_ALPHA_PRESENT) << 4);
                offset <<= 1;
                break;
            case 16*3 + 2:      /* BGRA, 16-bit ARGB 1-5-5-5 */
                EXTRACT_5551_TO_8888(data, tmp_sb, tmp_sg, tmp_sr, tmp_sa, 0);
                EXTRACT_5551_TO_8888(data >> 16, tmp_sb, tmp_sg, tmp_sr, tmp_sa, 1);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | ((LFB_RGB_PRESENT | LFB_ALPHA_PRESENT) << 4);
                offset <<= 1;
                break;

            case 16*0 + 4:      /* ARGB, 32-bit RGB x-8-8-8 */
                EXTRACT_x888_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT;
                break;
            case 16*1 + 4:      /* ABGR, 32-bit RGB x-8-8-8 */
                EXTRACT_x888_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT;
                break;
            case 16*2 + 4:      /* RGBA, 32-bit RGB x-8-8-8 */
                EXTRACT_888x_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT;
                break;
            case 16*3 + 4:      /* BGRA, 32-bit RGB x-8-8-8 */
                EXTRACT_888x_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT;
                break;

            case 16*0 + 5:      /* ARGB, 32-bit ARGB 8-8-8-8 */
                EXTRACT_8888_TO_8888(data, tmp_sa, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT;
                break;
            case 16*1 + 5:      /* ABGR, 32-bit ARGB 8-8-8-8 */
                EXTRACT_8888_TO_8888(data, tmp_sa, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT;
                break;
            case 16*2 + 5:      /* RGBA, 32-bit ARGB 8-8-8-8 */
                EXTRACT_8888_TO_8888(data, tmp_sr, tmp_sg, tmp_sb, tmp_sa, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT;
                break;
            case 16*3 + 5:      /* BGRA, 32-bit ARGB 8-8-8-8 */
                EXTRACT_8888_TO_8888(data, tmp_sb, tmp_sg, tmp_sr, tmp_sa, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT;
                break;

            case 16*0 + 12:     /* ARGB, 32-bit depth+RGB 5-6-5 */
            case 16*2 + 12:     /* RGBA, 32-bit depth+RGB 5-6-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_565_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*1 + 12:     /* ABGR, 32-bit depth+RGB 5-6-5 */
            case 16*3 + 12:     /* BGRA, 32-bit depth+RGB 5-6-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_565_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;

            case 16*0 + 13:     /* ARGB, 32-bit depth+RGB x-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_x555_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*1 + 13:     /* ABGR, 32-bit depth+RGB x-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_x555_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*2 + 13:     /* RGBA, 32-bit depth+RGB x-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_555x_TO_888(data, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*3 + 13:     /* BGRA, 32-bit depth+RGB x-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_555x_TO_888(data, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;

            case 16*0 + 14:     /* ARGB, 32-bit depth+ARGB 1-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_1555_TO_8888(data, tmp_sa, tmp_sr, tmp_sg, tmp_sb, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*1 + 14:     /* ABGR, 32-bit depth+ARGB 1-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_1555_TO_8888(data, tmp_sa, tmp_sb, tmp_sg, tmp_sr, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*2 + 14:     /* RGBA, 32-bit depth+ARGB 1-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_5551_TO_8888(data, tmp_sr, tmp_sg, tmp_sb, tmp_sa, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;
            case 16*3 + 14:     /* BGRA, 32-bit depth+ARGB 1-5-5-5 */
                tmp_sw[0] = data >> 16;
                EXTRACT_5551_TO_8888(data, tmp_sb, tmp_sg, tmp_sr, tmp_sa, 0);
                mask = LFB_RGB_PRESENT | LFB_ALPHA_PRESENT | LFB_DEPTH_PRESENT_MSW;
                break;

            case 16*0 + 15:     /* ARGB, 16-bit depth */
            case 16*1 + 15:     /* ARGB, 16-bit depth */
            case 16*2 + 15:     /* ARGB, 16-bit depth */
            case 16*3 + 15:     /* ARGB, 16-bit depth */
                tmp_sw[0] = data & 0xffff;
                tmp_sw[1] = data >> 16;
                mask = LFB_DEPTH_PRESENT | (LFB_DEPTH_PRESENT << 4);
                offset <<= 1;
                break;

            default:            /* reserved */
                return 0;
        }

        /* compute X,Y */
        x = (offset << 0) & ((1 << fbi.lfb_stride) - 1);
        y = (offset >> fbi.lfb_stride) & ((1 << fbi.lfb_stride) - 1);

        /* adjust the mask based on which half of the data is written */
        if ((mem_mask & 0x0000ffff) == 0)
            mask &= ~(0x0f - LFB_DEPTH_PRESENT_MSW);
        if ((mem_mask & 0xffff0000) == 0)
            mask &= ~(0xf0 + LFB_DEPTH_PRESENT_MSW);

        /* select the target buffer */
        destbuf = (type >= TYPE_VOODOO_BANSHEE) ? (forcefront?0:1) : LFBMODE_WRITE_BUFFER_SELECT(reg[lfbMode]);
        switch (destbuf)
        {
            case 0:         /* front buffer */
                destPos = fbi.rgboffs[fbi.frontbuf] / 2;
                destmax = (fbi.mask + 1 - fbi.rgboffs[fbi.frontbuf]) / 2;
                fbi.video_changed = true;
                break;

            case 1:         /* back buffer */
                destPos = fbi.rgboffs[fbi.backbuf] / 2;
                destmax = (fbi.mask + 1 - fbi.rgboffs[fbi.backbuf]) / 2;
                break;

            default:        /* reserved */
                return 0;
        }
        depthPos = fbi.auxoffs / 2;
        depthmax = (fbi.mask + 1 - fbi.auxoffs) / 2;

        /* wait for any outstanding work to finish */
        Poly.poly_wait(poly, "LFB Write");

        /* simple case: no pipeline */
        if (!LFBMODE_ENABLE_PIXEL_PIPELINE(reg[lfbMode]))
        {
            byte[] dither_lookup = null;
            int dither_lookupPos = 0;
            int bufoffs;

            //if (LOG_LFB) logerror("VOODOO.%d.LFB:write raw mode %X (%d,%d) = %08X & %08X\n", index, LFBMODE_WRITE_FORMAT(reg[lfbMode].u), x, y, data, mem_mask);

            /* determine the screen Y */
            scry = y;
            if (LFBMODE_Y_ORIGIN(reg[lfbMode]))
                scry = (fbi.yorigin - y) & 0x3ff;

            /* advance pointers to the proper row */
            bufoffs = scry * fbi.rowpixels + x;

            /* compute dithering */
            //COMPUTE_DITHER_POINTERS_NO_DITHER_VAR(reg[fbzMode].u, y);
            if (FBZMODE_ENABLE_DITHERING(reg[fbzMode]))
            {
                if (!FBZMODE_DITHER_TYPE(reg[fbzMode]))
                {
                    dither_lookup = dither4_lookup;
                    dither_lookupPos = (y & 3) << 11;
                }
                else
                {
                    dither_lookup = dither2_lookup;
                    dither_lookupPos = (y & 3) << 11;
                }
            }

            /* loop over up to two pixels */
            for (pix = 0; mask!=0; pix++)
            {
                /* make sure we care about this pixel */
                if ((mask & 0x0f)!=0)
                {
                    /* write to the RGB buffer */
                    if ((mask & LFB_RGB_PRESENT)!=0 && bufoffs < destmax)
                    {
                        /* apply dithering and write to the screen */
                        //APPLY_DITHER(reg[fbzMode].u, x, dither_lookup, sr[pix], sg[pix], sb[pix]);
                        if (FBZMODE_ENABLE_DITHERING(reg[fbzMode]))
                        {
                            /* look up the dither value from the appropriate matrix */
                            int dither_offset = dither_lookupPos + ((x & 3) << 1);
                            /* apply dithering to R,G,B */
                            tmp_sr[pix] = dither_lookup[(tmp_sr[pix] << 3) + 0 + dither_offset];
                            tmp_sg[pix] = dither_lookup[(tmp_sg[pix] << 3) + 1 + dither_offset];
                            tmp_sb[pix] = dither_lookup[(tmp_sb[pix] << 3) + 0 + dither_offset];
                        }
                        else
                        {
                            tmp_sr[pix] >>= 3;
                            tmp_sg[pix] >>= 2;
                            tmp_sb[pix] >>= 3;
                        }
                        fbi.ram[destPos+bufoffs] = (short)((tmp_sr[pix] << 11) | (tmp_sg[pix] << 5) | tmp_sb[pix]);
                    }

                    /* make sure we have an aux buffer to write to */
                    if (bufoffs < depthmax)
                    {
                        /* write to the alpha buffer */
                        if ((mask & LFB_ALPHA_PRESENT)!=0 && FBZMODE_ENABLE_ALPHA_PLANES(reg[fbzMode]))
                            fbi.ram[depthPos+bufoffs] = (short)tmp_sa[pix];

                        /* write to the depth buffer */
                        if ((mask & (LFB_DEPTH_PRESENT | LFB_DEPTH_PRESENT_MSW))!=0 && !FBZMODE_ENABLE_ALPHA_PLANES(reg[fbzMode]))
                            fbi.ram[depthPos+bufoffs] = (short)tmp_sw[pix];
                    }

                    /* track pixel writes to the frame buffer regardless of mask */
                    reg[fbiPixelsOut]++;
                }

                /* advance our pointers */
                bufoffs++;
                x++;
                mask >>= 4;
            }
        }

        /* tricky case: run the full pixel pipeline on the pixel */
        else
        {
            // DECLARE_DITHER_POINTERS;
            byte[] dither_lookup = null;
            int dither_lookupPos = 0;
            byte[] dither4 = null;
            int dither4Pos = 0;
            byte[] dither = null;
            int ditherPos=0;

            //if (LOG_LFB) logerror("VOODOO.%d.LFB:write pipelined mode %X (%d,%d) = %08X & %08X\n", index, LFBMODE_WRITE_FORMAT(reg[lfbMode].u), x, y, data, mem_mask);

            /* determine the screen Y */
            scry = y;
            if (FBZMODE_Y_ORIGIN(reg[fbzMode]))
                scry = (fbi.yorigin - y) & 0x3ff;

            /* advance pointers to the proper row */
            destPos += (scry * fbi.rowpixels);
            depthPos += (scry * fbi.rowpixels);

            /* compute dithering */
            // COMPUTE_DITHER_POINTERS(reg[fbzMode].u, y);
            if (FBZMODE_ENABLE_DITHERING(reg[fbzMode]))
            {
                dither4 = dither_matrix_4x4;
                dither4Pos = (y & 3) * 4;
                if (!FBZMODE_DITHER_TYPE(reg[fbzMode]))
                {
                    dither = dither4;
                    ditherPos = dither4Pos;
                    dither_lookup = dither4_lookup;
                    dither_lookupPos = (y & 3) << 11;
                }
                else
                {
                    dither = dither_matrix_2x2;
                    ditherPos = (y & 3) * 4;
                    dither_lookup = dither2_lookup;
                    dither_lookupPos = (y & 3) << 11;
                }
            }

            /* loop over up to two pixels */
            for (pix = 0; mask!=0; pix++)
            {
                /* make sure we care about this pixel */
                if ((mask & 0x0f)!=0)
                {
                    final stats_block stats = fbi.lfb_stats;
                    LongRef iterw = new LongRef(tmp_sw[pix] << (30-16));
                    IntRef iterz = new IntRef(tmp_sw[pix] << 12);
                    int color;

                    /* apply clipping */
                    if (FBZMODE_ENABLE_CLIPPING(reg[fbzMode]))
                    {
                        if (x < ((reg[clipLeftRight] >> 16) & 0x3ff) ||
                            x >= (reg[clipLeftRight] & 0x3ff) ||
                            scry < ((reg[clipLowYHighY] >> 16) & 0x3ff) ||
                            scry >= (reg[clipLowYHighY] & 0x3ff))
                        {
                            stats.pixels_in++;
                            stats.clip_fail++;

                            x++;
                            mask >>= 4;
                            continue;
                        }
                    }
                    final int pi = pix;
                    PIXEL_PIPELINE_CALLBACK callback = new PIXEL_PIPELINE_CALLBACK() {
                        public boolean call(IntRef iterargb, IntRef result) {
                            iterargb.value = reg[zaColor];

                            /* use the RGBA we stashed above */
                            result.value = setRegRGBA(tmp_sr[pi], tmp_sg[pi], tmp_sb[pi], tmp_sa[pi]);

                            /* apply chroma key, alpha mask, and alpha testing */
                            if (!APPLY_CHROMAKEY(stats, reg[fbzMode], result.value))
                                return false;
                            if (!APPLY_ALPHAMASK(stats, reg[fbzMode], tmp_sa[pi]))
                                return false;
                            if (!APPLY_ALPHATEST(stats, reg[alphaMode], tmp_sa[pi]))
                                return false;
                            return true;
                        }
                    };

                    PIXEL_PIPELINE(stats, x, y, reg[fbzColorPath], reg[fbzMode], reg[alphaMode], reg[fogMode], iterz, iterw, dither, ditherPos, dither4, dither4Pos, dither_lookup, dither_lookupPos, fbi.ram, destPos, fbi.ram, depthPos, callback);
                }
                /* advance our pointers */
                x++;
                mask >>= 4;
            }
        }

        return 0;
    }

    private int texture_w(int offset, int data) {
        int tmunum = (offset >> 19) & 0x03;
        tmu_state t;
    
        /* statistics */
        stats.tex_writes++;
    
        /* point to the right TMU */
        if ((chipmask & (2 << tmunum))==0)
            return 0;
        t = tmu[tmunum];

        if (TEXLOD_TDIRECT_WRITE(reg[t.reg+tLOD]))
            Log.exit("Texture direct write!");
    
        /* wait for any outstanding work to finish */
        Poly.poly_wait(poly, "Texture write");
    
        /* update texture info if dirty */
        if (t.regdirty)
            recompute_texture_params(t);
    
        /* swizzle the data */
        if (TEXLOD_TDATA_SWIZZLE(reg[t.reg+tLOD]))
            data = FLIPENDIAN_INT32(data);
        if (TEXLOD_TDATA_SWAP(reg[t.reg+tLOD]))
            data = (data >> 16) | (data << 16);
    
        /* 8-bit texture case */
        if (TEXMODE_FORMAT(reg[t.reg+textureMode]) < 8)
        {
            int lod, tt, ts;
            int tbaseaddr;
    
            /* extract info */
            if (type <= TYPE_VOODOO_2)
            {
                lod = (offset >> 15) & 0x0f;
                tt = (offset >> 7) & 0xff;
    
                /* old code has a bit about how this is broken in gauntleg unless we always look at TMU0 */
                if (TEXMODE_SEQ_8_DOWNLD(reg[tmu[0].reg/*t.reg*/+textureMode]))
                    ts = (offset << 2) & 0xfc;
                else
                    ts = (offset << 1) & 0xfc;
    
                /* validate parameters */
                if (lod > 8)
                    return 0;
    
                /* compute the base address */
                tbaseaddr = t.lodoffset[lod];
                tbaseaddr += tt * ((t.wmask >> lod) + 1) + ts;
    
                //if (LOG_TEXTURE_RAM) logerror("Texture 8-bit w: lod=%d s=%d t=%d data=%08X\n", lod, ts, tt, data);
            }
            else
            {
                tbaseaddr = t.lodoffset[0] + offset*4;
    
                //if (LOG_TEXTURE_RAM) logerror("Texture 16-bit w: offset=%X data=%08X\n", offset*4, data);
            }
    
            /* write the four bytes in little-endian order */
            tbaseaddr &= t.mask;
            mem_writed(t.ram, tbaseaddr, data);
        }
    
        /* 16-bit texture case */
        else
        {
            int lod, tt, ts;
            int tbaseaddr;
    
            /* extract info */
            if (type <= TYPE_VOODOO_2)
            {
                lod = (offset >> 15) & 0x0f;
                tt = (offset >> 7) & 0xff;
                ts = (offset << 1) & 0xfe;
    
                /* validate parameters */
                if (lod > 8)
                    return 0;
    
                /* compute the base address */
                tbaseaddr = t.lodoffset[lod];
                tbaseaddr += 2 * (tt * ((t.wmask >> lod) + 1) + ts);
    
                //if (LOG_TEXTURE_RAM) logerror("Texture 16-bit w: lod=%d s=%d t=%d data=%08X\n", lod, ts, tt, data);
            }
            else
            {
                tbaseaddr = t.lodoffset[0] + offset*4;
    
                //if (LOG_TEXTURE_RAM) logerror("Texture 16-bit w: offset=%X data=%08X\n", offset*4, data);
            }
    
            /* write the two words in little-endian order */
            tbaseaddr &= t.mask;
            mem_writed(t.ram, tbaseaddr, data);
        }
    
        return 0;
    }

    private void stall_cpu(int state) {
        stats.stalls++;
    }

    /*************************************
     *
     *  Handle a write to the Voodoo
     *  memory space
     *
     *************************************/

    private void voodoo_w(int offset, int data, int mem_mask)
    {
        boolean stall = false;

        //g_profiler.start(PROFILER_USER1);

        /* should not be getting accesses while stalled */
        if (pci.stall_state != NOT_STALLED)
            Log.log_msg("voodoo_w while stalled!");

        /* if we have something pending, flush the FIFOs up to the current time */
        if (pci.op_pending)
            flush_fifos();

        /* special handling for registers */
        if ((offset & 0xc00000/4) == 0)
        {
            byte access;

            /* some special stuff for Voodoo 2 */
            if (type >= TYPE_VOODOO_2)
            {
                /* we might be in CMDFIFO mode */
                if (FBIINIT7_CMDFIFO_ENABLE(reg[fbiInit7]))
                {
                    /* if bit 21 is set, we're writing to the FIFO */
                    if ((offset & 0x200000/4)!=0)
                    {
                        /* check for byte swizzling (bit 18) */
                        if ((offset & 0x40000/4)!=0)
                            data = FLIPENDIAN_INT32(data);
                        cmdfifo_w(fbi.cmdfifo[0], offset & 0xffff, data);
                        //g_profiler.stop();
                        return;
                    }

                    /* we're a register access; but only certain ones are allowed */
                    access = regaccess[offset & 0xff];
                    if ((access & REGISTER_WRITETHRU)==0)
                    {
                        /* track swap buffers regardless */
                        if ((offset & 0xff) == swapbufferCMD)
                            fbi.swaps_pending++;

                        Log.log_msg("Ignoring write to "+regnames[offset & 0xff]+" in CMDFIFO mode");
                        //g_profiler.stop();
                        return;
                    }
                }

                /* if not, we might be byte swizzled (bit 20) */
                else if ((offset & 0x100000/4)!=0)
                    data = FLIPENDIAN_INT32(data);
            }

            /* check the access behavior; note that the table works even if the */
            /* alternate mapping is used */
            access = regaccess[offset & 0xff];

            /* ignore if writes aren't allowed */
            if ((access & REGISTER_WRITE)==0)
            {
                //g_profiler.stop();
                return;
            }

            /* if this is a non-FIFO command, let it go to the FIFO, but stall until it completes */
            if ((access & REGISTER_FIFO)==0)
                stall = true;

            /* track swap buffers */
            if ((offset & 0xff) == swapbufferCMD)
                fbi.swaps_pending++;
        }

        /* if we don't have anything pending, or if FIFOs are disabled, just execute */
        if (!pci.op_pending || !INITEN_ENABLE_PCI_FIFO(pci.init_enable))
        {
            int cycles;

            /* target the appropriate location */
            if ((offset & (0xc00000/4)) == 0)
                cycles = register_w(offset, data);
            else if ((offset & (0x800000/4))!=0)
                cycles = texture_w(offset, data);
            else
                cycles = lfb_w(offset, data, mem_mask, false);

            /* if we ended up with cycles, mark the operation pending */
            if (cycles!=0)
            {
                pci.op_pending = true;
                pci.op_end_time = System.nanoTime() + cycles * attoseconds_per_cycle;

//                if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:direct write start at %d.%08X%08X end at %d.%08X%08X\n", index,
//                    device.machine().time().seconds, (UINT32)(device.machine().time().attoseconds >> 32), (UINT32)device.machine().time().attoseconds,
//                    pci.op_end_time.seconds, (UINT32)(pci.op_end_time.attoseconds >> 32), (UINT32)pci.op_end_time.attoseconds);
            }
            //g_profiler.stop();
            return;
        }

        /* modify the offset based on the mem_mask */
        if (mem_mask != 0xffffffff)
        {
            if ((mem_mask & 0xffff0000)==0)
                offset |= 0x80000000;
            if ((mem_mask & 0x0000ffff) == 0)
                offset |= 0x40000000;
        }

        /* if there's room in the PCI FIFO, add there */
        //if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:voodoo_w adding to PCI FIFO @ %08X=%08X\n", index, offset, data);
        if (!fifo_full(pci.fifo))
        {
            fifo_add(pci.fifo, offset);
            fifo_add(pci.fifo, data);
        }
        else
            Log.exit("Voodoo PCI FIFO full");

        /* handle flushing to the memory FIFO */
        if (FBIINIT0_ENABLE_MEMORY_FIFO(reg[fbiInit0]) &&
            fifo_space(pci.fifo) <= 2 * FBIINIT4_MEMORY_FIFO_LWM(reg[fbiInit4]))
        {
            byte[] valid = new byte[4];

            /* determine which types of data can go to the memory FIFO */
            valid[0] = 1;
            valid[1] = (byte)(FBIINIT0_LFB_TO_MEMORY_FIFO(reg[fbiInit0])?1:0);
            valid[2] = valid[3] = (byte)(FBIINIT0_TEXMEM_TO_MEMORY_FIFO(reg[fbiInit0])?1:0);

            /* flush everything we can */
            //if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:voodoo_w moving PCI FIFO to memory FIFO\n", index);
            while (!fifo_empty(pci.fifo) && valid[(fifo_peek(pci.fifo) >> 22) & 3]!=0)
            {
                fifo_add(fbi.fifo, fifo_remove(pci.fifo));
                fifo_add(fbi.fifo, fifo_remove(pci.fifo));
            }

            /* if we're above the HWM as a result, stall */
            if (FBIINIT0_STALL_PCIE_FOR_HWM(reg[fbiInit0]) &&
                fifo_items(fbi.fifo) >= 2 * 32 * FBIINIT0_MEMORY_FIFO_HWM(reg[fbiInit0]))
            {
                //if (LOG_FIFO) logerror("VOODOO.%d.FIFO:voodoo_w hit memory FIFO HWM -- stalling\n", index);
                stall_cpu(STALLED_UNTIL_FIFO_LWM);
            }
        }

        /* if we're at the LWM for the PCI FIFO, stall */
        if (FBIINIT0_STALL_PCIE_FOR_HWM(reg[fbiInit0]) &&
            fifo_space(pci.fifo) <= 2 * FBIINIT0_PCI_FIFO_LWM(reg[fbiInit0]))
        {
            //if (LOG_FIFO) logerror("VOODOO.%d.FIFO:voodoo_w hit PCI FIFO free LWM -- stalling\n", index);
            stall_cpu(STALLED_UNTIL_FIFO_LWM);
        }

        /* if we weren't ready, and this is a non-FIFO access, stall until the FIFOs are clear */
        if (stall)
        {
            //if (LOG_FIFO_VERBOSE) logerror("VOODOO.%d.FIFO:voodoo_w wrote non-FIFO register -- stalling until clear\n", index);
            stall_cpu(STALLED_UNTIL_FIFO_EMPTY);
        }

        //g_profiler.stop();
    }



    /*************************************
     *
     *  Handle a register read
     *
     *************************************/

    private void eat_cycles(int amount) {
        //CPU.CPU_Cycles-=amount;
    }

    private int register_r(int offset)
    {
        int regnum = offset & 0xff;
        int result;

        /* statistics */
        stats.reg_reads++;

        /* first make sure this register is readable */
        if ((regaccess[regnum] & REGISTER_READ)==0)
        {
            //logerror("VOODOO.%d.ERROR:Invalid attempt to read %s\n", index, regnum < 225 ? regnames[regnum] : "unknown register");
            return 0xffffffff;
        }

        /* default result is the FBI register value */
        result = reg[regnum];

        /* some registers are dynamic; compute them */
        switch (regnum)
        {
            case status:

                /* start with a blank slate */
                result = 0;

                /* bits 5:0 are the PCI FIFO free space */
                if (fifo_empty(pci.fifo))
                    result |= 0x3f << 0;
                else
                {
                    int temp = fifo_space(pci.fifo)/2;
                    if (temp > 0x3f)
                        temp = 0x3f;
                    result |= temp << 0;
                }

                /* bit 6 is the vertical retrace */
                result |= (fbi.vblank?1:0) << 6;

                /* bit 7 is FBI graphics engine busy */
                if (pci.op_pending)
                    result |= 1 << 7;

                /* bit 8 is TREX busy */
                if (pci.op_pending)
                    result |= 1 << 8;

                /* bit 9 is overall busy */
                if (pci.op_pending)
                    result |= 1 << 9;

                /* Banshee is different starting here */
                if (type < TYPE_VOODOO_BANSHEE)
                {
                    /* bits 11:10 specifies which buffer is visible */
                    result |= fbi.frontbuf << 10;

                    /* bits 27:12 indicate memory FIFO freespace */
                    if (!FBIINIT0_ENABLE_MEMORY_FIFO(reg[fbiInit0]) || fifo_empty(fbi.fifo))
                        result |= 0xffff << 12;
                    else
                    {
                        int temp = fifo_space(fbi.fifo)/2;
                        if (temp > 0xffff)
                            temp = 0xffff;
                        result |= temp << 12;
                    }
                }
                else
                {
                    /* bit 10 is 2D busy */

                    /* bit 11 is cmd FIFO 0 busy */
                    if (fbi.cmdfifo[0].enable && fbi.cmdfifo[0].depth > 0)
                        result |= 1 << 11;

                    /* bit 12 is cmd FIFO 1 busy */
                    if (fbi.cmdfifo[1].enable && fbi.cmdfifo[1].depth > 0)
                        result |= 1 << 12;
                }

                /* bits 30:28 are the number of pending swaps */
                if (fbi.swaps_pending > 7)
                    result |= 7 << 28;
                else
                    result |= fbi.swaps_pending << 28;

                /* bit 31 is not used */

                /* eat some cycles since people like polling here */
                eat_cycles(1000);
                break;

            /* bit 2 of the initEnable register maps this to dacRead */
            case fbiInit2:
                if (INITEN_REMAP_INIT_TO_DAC(pci.init_enable))
                    result = dac.read_result;
                break;

            /* return the current scanline for now */
            case vRetrace:

                /* eat some cycles since people like polling here */
                eat_cycles(10);
                result = VGA.vga.draw.lines_done;  // :TODO: no clue if this is right
                break;

            /* reserved area in the TMU read by the Vegas startup sequence */
            case hvRetrace:
                result = 0x200 << 16;   /* should be between 0x7b and 0x267 */
                result |= 0x80;         /* should be between 0x17 and 0x103 */
                break;

            /* cmdFifo -- Voodoo2 only */
            case cmdFifoRdPtr:
                result = fbi.cmdfifo[0].rdptr;

                /* eat some cycles since people like polling here */
                eat_cycles(1000);
                break;

            case cmdFifoAMin:
                result = fbi.cmdfifo[0].amin;
                break;

            case cmdFifoAMax:
                result = fbi.cmdfifo[0].amax;
                break;

            case cmdFifoDepth:
                result = fbi.cmdfifo[0].depth;
                break;

            case cmdFifoHoles:
                result = fbi.cmdfifo[0].holes;
                break;

            /* all counters are 24-bit only */
            case fbiPixelsIn:
            case fbiChromaFail:
            case fbiZfuncFail:
            case fbiAfuncFail:
            case fbiPixelsOut:
                update_statistics(true);
            case fbiTrianglesOut:
                result = reg[regnum] & 0xffffff;
                break;
        }

//        if (LOG_REGISTERS)
//        {
//            int logit = TRUE;
//
//            /* don't log multiple identical status reads from the same address */
//            if (regnum == status)
//            {
//                offs_t pc = cpu.safe_pc();
//                if (pc == last_status_pc && result == last_status_value)
//                    logit = FALSE;
//                last_status_pc = pc;
//                last_status_value = result;
//            }
//            if (regnum == cmdFifoRdPtr)
//                logit = FALSE;
//
//            if (logit)
//                logerror("VOODOO.%d.REG:%s read = %08X\n", index, regnames[regnum], result);
//        }
        //if (regnum>0)
            //System.out.println(count+" read "+regnames[regnum]+" result="+result);
        count++;
        return result;
    }



    /*************************************
     *
     *  Handle an LFB read
     *
     *************************************/

    private int lfb_r(int offset, boolean forcefront)
    {
        int bufferPos;
        int bufmax;
        int bufoffs;
        int data;
        int x, y, scry, destbuf;

        /* statistics */
        stats.lfb_reads++;

        /* compute X,Y */
        x = (offset << 1) & 0x3fe;
        y = (offset >> 9) & 0x3ff;

        /* select the target buffer */
        destbuf = (type >= TYPE_VOODOO_BANSHEE) ? (forcefront?0:1) : LFBMODE_READ_BUFFER_SELECT(reg[lfbMode]);
        switch (destbuf)
        {
            case 0:         /* front buffer */
                bufferPos = fbi.rgboffs[fbi.frontbuf] / 2;
                bufmax = (fbi.mask + 1 - fbi.rgboffs[fbi.frontbuf]) / 2;
                break;

            case 1:         /* back buffer */
                bufferPos = fbi.rgboffs[fbi.backbuf] / 2;
                bufmax = (fbi.mask + 1 - fbi.rgboffs[fbi.backbuf]) / 2;
                break;

            case 2:         /* aux buffer */
                if (fbi.auxoffs == -1)
                    return 0xffffffff;
                bufferPos = fbi.auxoffs / 2;
                bufmax = (fbi.mask + 1 - fbi.auxoffs) / 2;
                break;

            default:        /* reserved */
                return 0xffffffff;
        }

        /* determine the screen Y */
        scry = y;
        if (LFBMODE_Y_ORIGIN(reg[lfbMode]))
            scry = (fbi.yorigin - y) & 0x3ff;

        /* advance pointers to the proper row */
        bufoffs = scry * fbi.rowpixels + x;
        if (bufoffs >= bufmax)
            return 0xffffffff;

        /* wait for any outstanding work to finish */
        Poly.poly_wait(poly, "LFB read");

        /* compute the data */
        data = mem_readd(fbi.ram, bufferPos+bufoffs);

        /* word swapping */
        if (LFBMODE_WORD_SWAP_READS(reg[lfbMode]))
            data = (data << 16) | (data >> 16);

        /* byte swizzling */
        if (LFBMODE_BYTE_SWIZZLE_READS(reg[lfbMode]))
            data = FLIPENDIAN_INT32(data);

        //if (LOG_LFB) logerror("VOODOO.%d.LFB:read (%d,%d) = %08X\n", index, x, y, data);
        return data;
    }
    
    /***************************************************************************
        COMMAND HANDLERS
    ***************************************************************************/
    
    /*-------------------------------------------------
        fastfill - execute the 'fastfill'
        command
    -------------------------------------------------*/
    private int fastfill()
    {
    	int sx = (reg[clipLeftRight] >>> 16) & 0x3ff;
    	int ex = (reg[clipLeftRight] >>> 0) & 0x3ff;
    	int sy = (reg[clipLowYHighY] >>> 16) & 0x3ff;
    	int ey = (reg[clipLowYHighY] >>> 0) & 0x3ff;
    	poly_extent[] extents = poly_extent.create(64);
    	int[] dithermatrix = new int[16];
    	int drawbufPos = 0;
    	int pixels = 0;
    	int extnum, x, y;
    
    	/* if we're not clearing either, take no time */
    	if (!FBZMODE_RGB_BUFFER_MASK(reg[fbzMode]) && !FBZMODE_AUX_BUFFER_MASK(reg[fbzMode]))
    		return 0;
    
    	/* are we clearing the RGB buffer? */
    	if (FBZMODE_RGB_BUFFER_MASK(reg[fbzMode]))
    	{
    		/* determine the draw buffer */
    		int destbuf = (type >= TYPE_VOODOO_BANSHEE) ? 1 : FBZMODE_DRAW_BUFFER(reg[fbzMode]);
    		switch (destbuf)
    		{
    			case 0:     /* front buffer */
                    drawbufPos = fbi.rgboffs[fbi.frontbuf] / 2;
    				break;
    
    			case 1:     /* back buffer */
                    drawbufPos = fbi.rgboffs[fbi.backbuf] / 2;
    				break;
    
    			default:    /* reserved */
    				break;
    		}
    
    		/* determine the dither pattern */
    		for (y = 0; y < 4; y++)
    		{
    			//DECLARE_DITHER_POINTERS_NO_DITHER_VAR;
                byte[] dither_lookup = null;
                int dither_lookupPos = 0;

    			// COMPUTE_DITHER_POINTERS_NO_DITHER_VAR(reg[fbzMode], y);
                if (FBZMODE_ENABLE_DITHERING(reg[fbzMode]))
                {
                    if (!FBZMODE_DITHER_TYPE(reg[fbzMode]))
                    {
                        dither_lookup = dither4_lookup;
                        dither_lookupPos = (y & 3) << 11;
                    }
                    else
                    {
                        dither_lookup = dither2_lookup;
                        dither_lookupPos = (y & 3) << 11;
                    }
                }

    			for (x = 0; x < 4; x++)
    			{
    				int r = getRegR(reg[color1]);
    				int g = getRegG(reg[color1]);
    				int b = getRegB(reg[color1]);
    
    				// APPLY_DITHER(reg[fbzMode], x, dither_lookup, r, g, b);
                    /* apply dithering */
                    if (FBZMODE_ENABLE_DITHERING(reg[fbzMode]))
                    {
                        /* look up the dither value from the appropriate matrix */
                        int dithPos = dither_lookupPos + ((x & 3) << 1);

                        /* apply dithering to R,G,B */
                        r = dither_lookup[dithPos+(r << 3) + 0];
                        g = dither_lookup[dithPos+(g << 3) + 1];
                        b = dither_lookup[dithPos+(b << 3) + 0];
                    }
                    else
                    {
                        r >>>= 3;
                        g >>>= 2;
                        b >>>= 3;
                    }

    				dithermatrix[y*4 + x] = (r << 11) | (g << 5) | b;
    			}
    		}
    	}
    
    	/* fill in a block of extents */
    	extents[0].startx = sx;
    	extents[0].stopx = ex;
    	for (extnum = 1; extnum < extents.length; extnum++) {
    		extents[extnum].startx = sx;
            extents[extnum].stopx = ex;
        }
    
    	/* iterate over blocks of extents */
    	for (y = sy; y < ey; y += extents.length)
    	{
    		poly_extra_data extra = Poly.allocate_poly_extra_data(poly);
    		int count = Math.min(ey - y, extents.length);
    
    		extra.state = this;
    		extra.dither = dithermatrix;
    
    		pixels += Poly.poly_render_triangle_custom(poly, fbi.ram, drawbufPos, global_cliprect, raster_fastfill, y, count, extents, extra);
    	}
    
    	/* 2 pixels per clock */
    	return pixels / 2;
    }
    
    
    /*-------------------------------------------------
        swapbuffer - execute the 'swapbuffer'
        command
    -------------------------------------------------*/
    
    private int swapbuffer(int data)
    {
    	/* set the don't swap value for Voodoo 2 */
    	fbi.vblank_swap_pending = true;
    	fbi.vblank_swap = (data >> 1) & 0xff;
    	fbi.vblank_dont_swap = ((data >> 9) & 1)!=0;
    
    	/* if we're not syncing to the retrace, process the command immediately */
    	if ((data & 1)==0)
    	{
    		swap_buffers();
    		return 0;
    	}
    
    	/* determine how many cycles to wait; we deliberately overshoot here because */
    	/* the final count gets updated on the VBLANK */
    	return (fbi.vblank_swap + 1) * freq / 30;
    }
    
    
    /*-------------------------------------------------
        triangle - execute the 'triangle'
        command
    -------------------------------------------------*/
    private int mul_32x32_shift(int a, int b, int shift) {
        long tmp = (long)a * (long)b;
        tmp >>>= shift;
        return (int)tmp;
    }

    private int triangle()
    {
    	int texcount = 0;
    	int drawbufPos;
    	int destbuf;
    	int pixels;
    
    	//g_profiler.start(PROFILER_USER2);
    
    	/* determine the number of TMUs involved */
    	texcount = 0;
    	if (!FBIINIT3_DISABLE_TMUS(reg[fbiInit3]) && FBZCP_TEXTURE_ENABLE(reg[fbzColorPath]))
    	{
    		texcount = 1;
    		if ((chipmask & 0x04)!=0)
    			texcount = 2;
    	}
    
    	/* perform subpixel adjustments */
    	if (FBZCP_CCA_SUBPIXEL_ADJUST(reg[fbzColorPath]))
    	{
    		int dx = 8 - (fbi.ax & 15);
            int dy = 8 - (fbi.ay & 15);
    
    		/* adjust iterated R,G,B,A and W/Z */
    		fbi.startr += (dy * fbi.drdy + dx * fbi.drdx) >> 4;
    		fbi.startg += (dy * fbi.dgdy + dx * fbi.dgdx) >> 4;
    		fbi.startb += (dy * fbi.dbdy + dx * fbi.dbdx) >> 4;
    		fbi.starta += (dy * fbi.dady + dx * fbi.dadx) >> 4;
    		fbi.startw += (dy * fbi.dwdy + dx * fbi.dwdx) >> 4;
    		fbi.startz += mul_32x32_shift(dy, fbi.dzdy, 4) + mul_32x32_shift(dx, fbi.dzdx, 4);
    
    		/* adjust iterated W/S/T for TMU 0 */
    		if (texcount >= 1)
    		{
    			tmu[0].startw += (dy * tmu[0].dwdy + dx * tmu[0].dwdx) >> 4;
    			tmu[0].starts += (dy * tmu[0].dsdy + dx * tmu[0].dsdx) >> 4;
    			tmu[0].startt += (dy * tmu[0].dtdy + dx * tmu[0].dtdx) >> 4;
    
    			/* adjust iterated W/S/T for TMU 1 */
    			if (texcount >= 2)
    			{
    				tmu[1].startw += (dy * tmu[1].dwdy + dx * tmu[1].dwdx) >> 4;
    				tmu[1].starts += (dy * tmu[1].dsdy + dx * tmu[1].dsdx) >> 4;
    				tmu[1].startt += (dy * tmu[1].dtdy + dx * tmu[1].dtdx) >> 4;
    			}
    		}
    	}
    
    	/* wait for any outstanding work to finish */
    //  poly_wait(poly, "triangle");
    
    	/* determine the draw buffer */
    	destbuf = (type >= TYPE_VOODOO_BANSHEE) ? 1 : FBZMODE_DRAW_BUFFER(reg[fbzMode]);
    	switch (destbuf)
    	{
    		case 0:     /* front buffer */
    			drawbufPos = fbi.rgboffs[fbi.frontbuf] / 2;
    			fbi.video_changed = true;
    			break;
    
    		case 1:     /* back buffer */
                drawbufPos = fbi.rgboffs[fbi.backbuf] / 2;
    			break;
    
    		default:    /* reserved */
    			return TRIANGLE_SETUP_CLOCKS;
    	}
    
    	/* find a rasterizer that matches our current state */
    	pixels = triangle_create_work_item(drawbufPos, texcount);
    
    	/* update stats */
    	reg[fbiTrianglesOut]++;
    
    	/* update stats */
    	stats.total_triangles++;
    
    	//g_profiler.stop();
    
    	/* 1 pixel per clock, plus some setup time */
    	//if (LOG_REGISTERS) logerror("cycles = %d\n", TRIANGLE_SETUP_CLOCKS + pixels);
    	return TRIANGLE_SETUP_CLOCKS + pixels;
    }
    
    
    /*-------------------------------------------------
        begin_triangle - execute the 'beginTri'
        command
    -------------------------------------------------*/
    
    private int begin_triangle()
    {
    	setup_vertex sv = fbi.svert[2];
    
    	/* extract all the data from registers */
    	sv.x = Float.intBitsToFloat(reg[sVx]);
    	sv.y = Float.intBitsToFloat(reg[sVy]);
    	sv.wb = Float.intBitsToFloat(reg[sWb]);
    	sv.w0 = Float.intBitsToFloat(reg[sWtmu0]);
    	sv.s0 = Float.intBitsToFloat(reg[sS_W0]);
    	sv.t0 = Float.intBitsToFloat(reg[sT_W0]);
    	sv.w1 = Float.intBitsToFloat(reg[sWtmu1]);
    	sv.s1 = Float.intBitsToFloat(reg[sS_Wtmu1]);
    	sv.t1 = Float.intBitsToFloat(reg[sT_Wtmu1]);
    	sv.a = Float.intBitsToFloat(reg[sAlpha]);
    	sv.r = Float.intBitsToFloat(reg[sRed]);
    	sv.g = Float.intBitsToFloat(reg[sGreen]);
    	sv.b = Float.intBitsToFloat(reg[sBlue]);
    
    	/* spread it across all three verts and reset the count */
    	fbi.svert[0] = fbi.svert[1] = fbi.svert[2];
    	fbi.sverts = 1;
    
    	return 0;
    }
    
    
    /*-------------------------------------------------
        draw_triangle - execute the 'DrawTri'
        command
    -------------------------------------------------*/
    
    private int draw_triangle()
    {
    	setup_vertex sv = fbi.svert[2];
    	int cycles = 0;
    
    	/* for strip mode, shuffle vertex 1 down to 0 */
    	if ((reg[sSetupMode] & (1 << 16))==0)
    		fbi.svert[0] = fbi.svert[1];
    
    	/* copy 2 down to 1 regardless */
    	fbi.svert[1] = fbi.svert[2];
    
    	/* extract all the data from registers */
    	sv.x = Float.intBitsToFloat(reg[sVx]);
    	sv.y = Float.intBitsToFloat(reg[sVy]);
    	sv.wb = Float.intBitsToFloat(reg[sWb]);
    	sv.w0 = Float.intBitsToFloat(reg[sWtmu0]);
    	sv.s0 = Float.intBitsToFloat(reg[sS_W0]);
    	sv.t0 = Float.intBitsToFloat(reg[sT_W0]);
    	sv.w1 = Float.intBitsToFloat(reg[sWtmu1]);
    	sv.s1 = Float.intBitsToFloat(reg[sS_Wtmu1]);
    	sv.t1 = Float.intBitsToFloat(reg[sT_Wtmu1]);
    	sv.a = Float.intBitsToFloat(reg[sAlpha]);
    	sv.r = Float.intBitsToFloat(reg[sRed]);
    	sv.g = Float.intBitsToFloat(reg[sGreen]);
    	sv.b = Float.intBitsToFloat(reg[sBlue]);
    
    	/* if we have enough verts, go ahead and draw */
    	if (++fbi.sverts >= 3)
    		cycles = setup_and_draw_triangle();
    
    	return cycles;
    }
    
    
    
    /***************************************************************************
        TRIANGLE HELPERS
    ***************************************************************************/
    
    /*-------------------------------------------------
        setup_and_draw_triangle - process the setup
        parameters and render the triangle
    -------------------------------------------------*/
    
    private int setup_and_draw_triangle()
    {
    	float dx1, dy1, dx2, dy2;
    	float divisor, tdiv;
    
    	/* grab the X/Ys at least */
    	fbi.ax = (short)(fbi.svert[0].x * 16.0);
    	fbi.ay = (short)(fbi.svert[0].y * 16.0);
    	fbi.bx = (short)(fbi.svert[1].x * 16.0);
    	fbi.by = (short)(fbi.svert[1].y * 16.0);
    	fbi.cx = (short)(fbi.svert[2].x * 16.0);
    	fbi.cy = (short)(fbi.svert[2].y * 16.0);
    
    	/* compute the divisor */
    	divisor = 1.0f / ((fbi.svert[0].x - fbi.svert[1].x) * (fbi.svert[0].y - fbi.svert[2].y) -
    						(fbi.svert[0].x - fbi.svert[2].x) * (fbi.svert[0].y - fbi.svert[1].y));
    
    	/* backface culling */
    	if ((reg[sSetupMode] & 0x20000)!=0)
    	{
    		int culling_sign = (reg[sSetupMode] >> 18) & 1;
    		int divisor_sign = (divisor < 0)?1:0;
    
    		/* if doing strips and ping pong is enabled, apply the ping pong */
    		if ((reg[sSetupMode] & 0x90000) == 0x00000)
    			culling_sign ^= (fbi.sverts - 3) & 1;
    
    		/* if our sign matches the culling sign, we're done for */
    		if (divisor_sign == culling_sign)
    			return TRIANGLE_SETUP_CLOCKS;
    	}
    
    	/* compute the dx/dy values */
    	dx1 = fbi.svert[0].y - fbi.svert[2].y;
    	dx2 = fbi.svert[0].y - fbi.svert[1].y;
    	dy1 = fbi.svert[0].x - fbi.svert[1].x;
    	dy2 = fbi.svert[0].x - fbi.svert[2].x;
    
    	/* set up R,G,B */
    	tdiv = divisor * 4096.0f;
    	if ((reg[sSetupMode] & (1 << 0))!=0)
    	{
    		fbi.startr = (int)(fbi.svert[0].r * 4096.0f);
    		fbi.drdx = (int)(((fbi.svert[0].r - fbi.svert[1].r) * dx1 - (fbi.svert[0].r - fbi.svert[2].r) * dx2) * tdiv);
    		fbi.drdy = (int)(((fbi.svert[0].r - fbi.svert[2].r) * dy1 - (fbi.svert[0].r - fbi.svert[1].r) * dy2) * tdiv);
    		fbi.startg = (int)(fbi.svert[0].g * 4096.0f);
    		fbi.dgdx = (int)(((fbi.svert[0].g - fbi.svert[1].g) * dx1 - (fbi.svert[0].g - fbi.svert[2].g) * dx2) * tdiv);
    		fbi.dgdy = (int)(((fbi.svert[0].g - fbi.svert[2].g) * dy1 - (fbi.svert[0].g - fbi.svert[1].g) * dy2) * tdiv);
    		fbi.startb = (int)(fbi.svert[0].b * 4096.0f);
    		fbi.dbdx = (int)(((fbi.svert[0].b - fbi.svert[1].b) * dx1 - (fbi.svert[0].b - fbi.svert[2].b) * dx2) * tdiv);
    		fbi.dbdy = (int)(((fbi.svert[0].b - fbi.svert[2].b) * dy1 - (fbi.svert[0].b - fbi.svert[1].b) * dy2) * tdiv);
    	}
    
    	/* set up alpha */
    	if ((reg[sSetupMode] & (1 << 1))!=0)
    	{
    		fbi.starta = (int)(fbi.svert[0].a * 4096.0);
    		fbi.dadx = (int)(((fbi.svert[0].a - fbi.svert[1].a) * dx1 - (fbi.svert[0].a - fbi.svert[2].a) * dx2) * tdiv);
    		fbi.dady = (int)(((fbi.svert[0].a - fbi.svert[2].a) * dy1 - (fbi.svert[0].a - fbi.svert[1].a) * dy2) * tdiv);
    	}
    
    	/* set up Z */
    	if ((reg[sSetupMode] & (1 << 2))!=0)
    	{
    		fbi.startz = (int)(fbi.svert[0].z * 4096.0);
    		fbi.dzdx = (int)(((fbi.svert[0].z - fbi.svert[1].z) * dx1 - (fbi.svert[0].z - fbi.svert[2].z) * dx2) * tdiv);
    		fbi.dzdy = (int)(((fbi.svert[0].z - fbi.svert[2].z) * dy1 - (fbi.svert[0].z - fbi.svert[1].z) * dy2) * tdiv);
    	}
    
    	/* set up Wb */
    	tdiv = divisor * 65536.0f * 65536.0f;
    	if ((reg[sSetupMode] & (1 << 3))!=0)
    	{
    		fbi.startw = tmu[0].startw = tmu[1].startw = (long)(fbi.svert[0].wb * 65536.0f * 65536.0f);
    		fbi.dwdx = tmu[0].dwdx = tmu[1].dwdx = (long) (((fbi.svert[0].wb - fbi.svert[1].wb) * dx1 - (fbi.svert[0].wb - fbi.svert[2].wb) * dx2) * tdiv);
    		fbi.dwdy = tmu[0].dwdy = tmu[1].dwdy = (long) (((fbi.svert[0].wb - fbi.svert[2].wb) * dy1 - (fbi.svert[0].wb - fbi.svert[1].wb) * dy2) * tdiv);
    	}
    
    	/* set up W0 */
    	if ((reg[sSetupMode] & (1 << 4))!=0)
    	{
    		tmu[0].startw = tmu[1].startw = (long)(fbi.svert[0].w0 * 65536.0f * 65536.0f);
    		tmu[0].dwdx = tmu[1].dwdx = (long) (((fbi.svert[0].w0 - fbi.svert[1].w0) * dx1 - (fbi.svert[0].w0 - fbi.svert[2].w0) * dx2) * tdiv);
    		tmu[0].dwdy = tmu[1].dwdy = (long) (((fbi.svert[0].w0 - fbi.svert[2].w0) * dy1 - (fbi.svert[0].w0 - fbi.svert[1].w0) * dy2) * tdiv);
    	}
    
    	/* set up S0,T0 */
    	if ((reg[sSetupMode] & (1 << 5))!=0)
    	{
    		tmu[0].starts = tmu[1].starts = (long)(fbi.svert[0].s0 * 65536.0f * 65536.0f);
    		tmu[0].dsdx = tmu[1].dsdx = (long) (((fbi.svert[0].s0 - fbi.svert[1].s0) * dx1 - (fbi.svert[0].s0 - fbi.svert[2].s0) * dx2) * tdiv);
    		tmu[0].dsdy = tmu[1].dsdy = (long) (((fbi.svert[0].s0 - fbi.svert[2].s0) * dy1 - (fbi.svert[0].s0 - fbi.svert[1].s0) * dy2) * tdiv);
    		tmu[0].startt = tmu[1].startt = (long)(fbi.svert[0].t0 * 65536.0f * 65536.0f);
    		tmu[0].dtdx = tmu[1].dtdx = (long) (((fbi.svert[0].t0 - fbi.svert[1].t0) * dx1 - (fbi.svert[0].t0 - fbi.svert[2].t0) * dx2) * tdiv);
    		tmu[0].dtdy = tmu[1].dtdy = (long) (((fbi.svert[0].t0 - fbi.svert[2].t0) * dy1 - (fbi.svert[0].t0 - fbi.svert[1].t0) * dy2) * tdiv);
    	}
    
    	/* set up W1 */
    	if ((reg[sSetupMode] & (1 << 6))!=0)
    	{
    		tmu[1].startw = (long)(fbi.svert[0].w1 * 65536.0f * 65536.0f);
    		tmu[1].dwdx = (long) (((fbi.svert[0].w1 - fbi.svert[1].w1) * dx1 - (fbi.svert[0].w1 - fbi.svert[2].w1) * dx2) * tdiv);
    		tmu[1].dwdy = (long) (((fbi.svert[0].w1 - fbi.svert[2].w1) * dy1 - (fbi.svert[0].w1 - fbi.svert[1].w1) * dy2) * tdiv);
    	}
    
    	/* set up S1,T1 */
    	if ((reg[sSetupMode] & (1 << 7))!=0)
    	{
    		tmu[1].starts = (long)(fbi.svert[0].s1 * 65536.0f * 65536.0f);
    		tmu[1].dsdx = (long) (((fbi.svert[0].s1 - fbi.svert[1].s1) * dx1 - (fbi.svert[0].s1 - fbi.svert[2].s1) * dx2) * tdiv);
    		tmu[1].dsdy = (long) (((fbi.svert[0].s1 - fbi.svert[2].s1) * dy1 - (fbi.svert[0].s1 - fbi.svert[1].s1) * dy2) * tdiv);
    		tmu[1].startt = (long)(fbi.svert[0].t1 * 65536.0f * 65536.0f);
    		tmu[1].dtdx = (long) (((fbi.svert[0].t1 - fbi.svert[1].t1) * dx1 - (fbi.svert[0].t1 - fbi.svert[2].t1) * dx2) * tdiv);
    		tmu[1].dtdy = (long) (((fbi.svert[0].t1 - fbi.svert[2].t1) * dy1 - (fbi.svert[0].t1 - fbi.svert[1].t1) * dy2) * tdiv);
    	}
    
    	/* draw the triangle */
    	fbi.cheating_allowed = true;
    	return triangle();
    }
    
    
    /*-------------------------------------------------
        triangle_create_work_item - finish triangle
        setup and create the work item
    -------------------------------------------------*/
    
    private int triangle_create_work_item(int drawbufPos, int texcount)
    {
    	poly_extra_data extra = Poly.allocate_poly_extra_data(poly);
    	raster_info info = find_rasterizer(texcount);
    	Poly.poly_vertex[] vert = Poly.poly_vertex.create(3);
    
    	/* fill in the vertex data */
    	vert[0].x = (float)fbi.ax * (1.0f / 16.0f);
    	vert[0].y = (float)fbi.ay * (1.0f / 16.0f);
    	vert[1].x = (float)fbi.bx * (1.0f / 16.0f);
    	vert[1].y = (float)fbi.by * (1.0f / 16.0f);
    	vert[2].x = (float)fbi.cx * (1.0f / 16.0f);
    	vert[2].y = (float)fbi.cy * (1.0f / 16.0f);
    
    	/* fill in the extra data */
    	extra.state = this;
    	extra.info = info;
    
    	/* fill in triangle parameters */
    	extra.ax = fbi.ax;
    	extra.ay = fbi.ay;
    	extra.startr = fbi.startr;
    	extra.startg = fbi.startg;
    	extra.startb = fbi.startb;
    	extra.starta = fbi.starta;
    	extra.startz = fbi.startz;
    	extra.startw = fbi.startw;
    	extra.drdx = fbi.drdx;
    	extra.dgdx = fbi.dgdx;
    	extra.dbdx = fbi.dbdx;
    	extra.dadx = fbi.dadx;
    	extra.dzdx = fbi.dzdx;
    	extra.dwdx = fbi.dwdx;
    	extra.drdy = fbi.drdy;
    	extra.dgdy = fbi.dgdy;
    	extra.dbdy = fbi.dbdy;
    	extra.dady = fbi.dady;
    	extra.dzdy = fbi.dzdy;
    	extra.dwdy = fbi.dwdy;
    
    	/* fill in texture 0 parameters */
    	if (texcount > 0)
    	{
    		extra.starts0 = tmu[0].starts;
    		extra.startt0 = tmu[0].startt;
    		extra.startw0 = tmu[0].startw;
    		extra.ds0dx = tmu[0].dsdx;
    		extra.dt0dx = tmu[0].dtdx;
    		extra.dw0dx = tmu[0].dwdx;
    		extra.ds0dy = tmu[0].dsdy;
    		extra.dt0dy = tmu[0].dtdy;
    		extra.dw0dy = tmu[0].dwdy;
    		extra.lodbase0 = prepare_tmu(tmu[0]);
    		stats.texture_mode[TEXMODE_FORMAT(reg[tmu[0].reg+textureMode])]++;
    
    		/* fill in texture 1 parameters */
    		if (texcount > 1)
    		{
    			extra.starts1 = tmu[1].starts;
    			extra.startt1 = tmu[1].startt;
    			extra.startw1 = tmu[1].startw;
    			extra.ds1dx = tmu[1].dsdx;
    			extra.dt1dx = tmu[1].dtdx;
    			extra.dw1dx = tmu[1].dwdx;
    			extra.ds1dy = tmu[1].dsdy;
    			extra.dt1dy = tmu[1].dtdy;
    			extra.dw1dy = tmu[1].dwdy;
    			extra.lodbase1 = prepare_tmu(tmu[1]);
    			stats.texture_mode[TEXMODE_FORMAT(reg[tmu[1].reg+textureMode])]++;
    		}
    	}
    
    	/* farm the rasterization out to other threads */
    	info.polys++;
    	return Poly.poly_render_triangle(poly, fbi.ram, drawbufPos, global_cliprect, info.callback, 0, vert[0], vert[1], vert[2], extra);
    }

    /***************************************************************************
        RASTERIZER MANAGEMENT
    ***************************************************************************/

    /*-------------------------------------------------
        add_rasterizer - add a rasterizer to our
        hash table
    -------------------------------------------------*/

    public raster_info add_rasterizer(raster_info cinfo)
    {
    	raster_info info = rasterizer[next_rasterizer++];
    	int hash = compute_raster_hash(cinfo);

    	//assert_always(next_rasterizer <= MAX_RASTERIZERS, "Out of space for new rasterizers!");

    	/* make a copy of the info */
    	info.copy(cinfo);

    	/* fill in the data */
    	info.hits = 0;
    	info.polys = 0;

    	/* hook us into the hash table */
    	info.next = raster_hash[hash];
    	raster_hash[hash] = info;

//    	if (LOG_RASTERIZERS)
//    		printf("Adding rasterizer @ %p : %08X %08X %08X %08X %08X %08X (hash=%d)\n",
//    				info.callback,
//    				info.eff_color_path, info.eff_alpha_mode, info.eff_fog_mode, info.eff_fbz_mode,
//    				info.eff_tex_mode_0, info.eff_tex_mode_1, hash);

    	return info;
    }


    /*-------------------------------------------------
        find_rasterizer - find a rasterizer that
        matches  our current parameters and return
        it, creating a new one if necessary
    -------------------------------------------------*/
    private raster_info find_rasterizer(int texcount)
    {
    	raster_info info, prev = null;
    	raster_info curinfo = new raster_info();
    	int hash;

    	/* build an info struct with all the parameters */
    	curinfo.eff_color_path = normalize_color_path(reg[fbzColorPath]);
    	curinfo.eff_alpha_mode = normalize_alpha_mode(reg[alphaMode]);
    	curinfo.eff_fog_mode = normalize_fog_mode(reg[fogMode]);
    	curinfo.eff_fbz_mode = normalize_fbz_mode(reg[fbzMode]);
    	curinfo.eff_tex_mode_0 = (texcount >= 1) ? normalize_tex_mode(reg[tmu[0].reg+textureMode]) : 0xffffffff;
    	curinfo.eff_tex_mode_1 = (texcount >= 2) ? normalize_tex_mode(reg[tmu[1].reg+textureMode]) : 0xffffffff;

    	/* compute the hash */
    	hash = compute_raster_hash(curinfo);

    	/* find the appropriate hash entry */
    	for (info = raster_hash[hash]; info!=null; prev = info, info = info.next)
    		if (info.eff_color_path == curinfo.eff_color_path &&
    			info.eff_alpha_mode == curinfo.eff_alpha_mode &&
    			info.eff_fog_mode == curinfo.eff_fog_mode &&
    			info.eff_fbz_mode == curinfo.eff_fbz_mode &&
    			info.eff_tex_mode_0 == curinfo.eff_tex_mode_0 &&
    			info.eff_tex_mode_1 == curinfo.eff_tex_mode_1)
    		{
    			/* got it, move us to the head of the list */
    			if (prev!=null)
    			{
    				prev.next = info.next;
    				info.next = raster_hash[hash];
    				raster_hash[hash] = info;
    			}

    			/* return the result */
    			return info;
    		}

    	/* generate a new one using the generic entry */
    	curinfo.callback = (texcount == 0) ? raster_generic_0tmu : (texcount == 1) ? raster_generic_1tmu : raster_generic_2tmu;
    	curinfo.is_generic = true;
    	curinfo.display = 0;
    	curinfo.polys = 0;
    	curinfo.hits = 0;
    	curinfo.next = null;

        RasterizerCompiler.compile(curinfo, texcount, reg[fbzColorPath], reg[alphaMode], reg[fogMode], reg[fbzMode], reg[tmu[0].reg + textureMode], reg[tmu[1].reg + textureMode]);
    	return add_rasterizer(curinfo);
    }

    /***************************************************************************
       GENERIC RASTERIZERS
   ***************************************************************************/
   
   /*-------------------------------------------------
       raster_fastfill - per-scanline
       implementation of the 'fastfill' command
   -------------------------------------------------*/
    static final poly_draw_scanline_func raster_fastfill = new poly_draw_scanline_func() {
        public void call(short[] dest, int destOffset, int y, poly_extent extent, poly_extra_data extra, int threadid)
        {
            VoodooCommon v = extra.state;
            stats_block stats = v.thread_stats[threadid];
            int startx = extent.startx;
            int stopx = extent.stopx;
            int scry, x;
           
            /* determine the screen Y */
            scry = y;
            if (FBZMODE_Y_ORIGIN(v.reg[fbzMode]))
                scry = (v.fbi.yorigin - y) & 0x3ff;
           
            /* fill this RGB row */
            if (FBZMODE_RGB_BUFFER_MASK(v.reg[fbzMode]))
            {
                int ditherrowPos = (y & 3) * 4;
                int destPos = scry * v.fbi.rowpixels;
           
                for (x = startx; x < stopx; x++)
                    dest[destOffset+destPos+x] = (short)extra.dither[ditherrowPos+(x & 3)];
                stats.pixels_out += stopx - startx;
            }
           
            /* fill this dest buffer row */
            if (FBZMODE_AUX_BUFFER_MASK(v.reg[fbzMode]) && v.fbi.auxoffs != -1)
            {
                short color = (short)v.reg[zaColor];
                int destPos = v.fbi.auxoffs / 2 + scry * v.fbi.rowpixels;
           
                for (x = startx; x < stopx; x++)
                    v.fbi.ram[destPos+x]= color;
            }
        }
    };

    /*************************************
     *
     *  Rasterizer generator macro
     *
     *************************************/
    
    static final class RASTERIZER implements poly_draw_scanline_func {
        final public String name;
        final int TMUS;
        
        public RASTERIZER(String name, int TMUS) {
            this.name = name;
            this.TMUS = TMUS;
        }
        
        public void call(short[] destbase, int destbasePos, int y, poly_extent extent, final poly_extra_data extra, int threadid) {
            final VoodooCommon v = extra.state;
            final stats_block stats = v.thread_stats[threadid];
            // DECLARE_DITHER_POINTERS;
            byte[] dither_lookup = null;
            int dither_lookupPos = 0;
            byte[] dither4 = null;
            int dither4Pos = 0;
            byte[] dither = null;
            int ditherPos=0;
            final int FBZCOLORPATH=v.reg[fbzColorPath];
            final int FBZMODE=v.reg[fbzMode];
            final int ALPHAMODE=v.reg[alphaMode];
            final int FOGMODE=v.reg[fogMode];
            final int TEXMODE0=v.reg[v.tmu[0].reg+textureMode];
            final int TEXMODE1=v.reg[v.tmu[1].reg+textureMode];

            int startx = extent.startx;
            int stopx = extent.stopx;
            final IntRef iterz = new IntRef(0);
            final LongRef iterw = new LongRef(0);
            long iterw0 = 0, iterw1 = 0;
            long iters0 = 0, iters1 = 0;
            long itert0 = 0, itert1 = 0;
            int depthPos;
            int destPos;
            int dx, dy;
            int scry;
            int x;

            /* determine the screen Y */
            scry = y;
            if (FBZMODE_Y_ORIGIN(FBZMODE))
                scry = (v.fbi.yorigin - y) & 0x3ff;

            /* compute dithering */
            //COMPUTE_DITHER_POINTERS(FBZMODE, y);
            if (FBZMODE_ENABLE_DITHERING(FBZMODE))
            {
                dither4 = dither_matrix_4x4;
                dither4Pos = (y & 3) * 4;
                if (!FBZMODE_DITHER_TYPE(FBZMODE))
                {
                    dither = dither4;
                    ditherPos = dither4Pos;
                    dither_lookup = dither4_lookup;
                    dither_lookupPos = (y & 3) << 11;
                }
                else
                {
                    dither = dither_matrix_2x2;
                    ditherPos = (y & 3) * 4;
                    dither_lookup = dither2_lookup;
                    dither_lookupPos = (y & 3) << 11;
                }
            }

            /* apply clipping */
            if (FBZMODE_ENABLE_CLIPPING(FBZMODE))
            {
                int tempclip;

                /* Y clipping buys us the whole scanline */
                if (scry < ((v.reg[clipLowYHighY] >> 16) & 0x3ff) ||
                    scry >= (v.reg[clipLowYHighY] & 0x3ff))
                {
                    stats.pixels_in += stopx - startx;
                    stats.clip_fail += stopx - startx;
                    return;
                }

                /* X clipping */
                tempclip = (v.reg[clipLeftRight] >> 16) & 0x3ff;
                if (startx < tempclip)
                {
                    stats.pixels_in += tempclip - startx;
                    v.stats.total_clipped += tempclip - startx;
                    startx = tempclip;
                }
                tempclip = v.reg[clipLeftRight] & 0x3ff;
                if (stopx >= tempclip)
                {
                    stats.pixels_in += stopx - tempclip;
                    v.stats.total_clipped += stopx - tempclip;
                    stopx = tempclip - 1;
                }
            }

            /* get pointers to the target buffer and depth buffer */
            destPos = destbasePos+scry * v.fbi.rowpixels;
            depthPos = (v.fbi.auxoffs != -1) ? (v.fbi.auxoffs / 2 + scry * v.fbi.rowpixels) : -1;

            /* compute the starting parameters */
            dx = startx - (extra.ax >> 4);
            dy = y - (extra.ay >> 4);
            final IntRef iterr = new IntRef(extra.startr + dy * extra.drdy + dx * extra.drdx);
            final IntRef iterg = new IntRef(extra.startg + dy * extra.dgdy + dx * extra.dgdx);
            final IntRef iterb = new IntRef(extra.startb + dy * extra.dbdy + dx * extra.dbdx);
            final IntRef itera = new IntRef(extra.starta + dy * extra.dady + dx * extra.dadx);
            iterz.value = extra.startz + dy * extra.dzdy + dx * extra.dzdx;
            iterw.value = extra.startw + dy * extra.dwdy + dx * extra.dwdx;
            if (TMUS >= 1)
            {
                iterw0 = extra.startw0 + dy * extra.dw0dy +   dx * extra.dw0dx;
                iters0 = extra.starts0 + dy * extra.ds0dy + dx * extra.ds0dx;
                itert0 = extra.startt0 + dy * extra.dt0dy + dx * extra.dt0dx;
            }
            if (TMUS >= 2)
            {
                iterw1 = extra.startw1 + dy * extra.dw1dy +   dx * extra.dw1dx;
                iters1 = extra.starts1 + dy * extra.ds1dy + dx * extra.ds1dx;
                itert1 = extra.startt1 + dy * extra.dt1dy + dx * extra.dt1dx;
            }

            /* loop in X */
            for (x = startx; x < stopx; x++)
            {
                final IntRef texel = new IntRef(0);
                final int XX = x;
                final long ITERS0 = iters0;
                final long ITERT0 = itert0;
                final long ITERW0 = iterw0;
                final long ITERS1 = iters1;
                final long ITERT1 = itert1;
                final long ITERW1 = iterw1;
                final byte[] DITHER4 = dither4;
                final int DITHER4POS = dither4Pos;

                PIXEL_PIPELINE_CALLBACK callback = new PIXEL_PIPELINE_CALLBACK() {
                    public boolean call(IntRef iterargb, IntRef result) {
                        /* run the texture pipeline on TMU1 to produce a value in texel */
                        /* note that they set LOD min to 8 to "disable" a TMU */
                        if (TMUS >= 2 && v.tmu[1].lodmin < (8 << 8))
                            texel.value = v.TEXTURE_PIPELINE(v.tmu[1], XX, DITHER4, DITHER4POS, TEXMODE1, texel.value, v.tmu[1].lookup, extra.lodbase1, ITERS1, ITERT1, ITERW1);

                        /* run the texture pipeline on TMU0 to produce a final */
                        /* result in texel */
                        /* note that they set LOD min to 8 to "disable" a TMU */
                        if (TMUS >= 1 && v.tmu[0].lodmin < (8 << 8)) {
                            if (((v.reg[v.tmu[0].reg+trexInit1] >> 18) & 1)==0)
                                texel.value = v.TEXTURE_PIPELINE(v.tmu[0], XX, DITHER4, DITHER4POS, TEXMODE0, texel.value, v.tmu[0].lookup, extra.lodbase0, ITERS0, ITERT0, ITERW0);
                            else
                                texel.value = 64;
                        }

                        /* colorpath pipeline selects source colors and does blending */
                        // CLAMPED_ARGB(iterr, iterg, iterb, itera, v.reg[FBZCOLORPATH], iterargb);
                        int r = iterr.value >> 12;
                        int g = iterg.value >> 12;
                        int b = iterb.value >> 12;
                        int a = itera.value >> 12;

                        {
                            int ir;
                            int ig;
                            int ib;
                            int ia;

                            if (!FBZCP_RGBZW_CLAMP(FBZCOLORPATH))
                            {
                                r &= 0xfff;
                                ir = r;
                                if (r == 0xfff)
                                    ir = 0;
                                else if (r == 0x100)
                                    ir = 0xff;

                                g &= 0xfff;
                                ig = g;
                                if (g == 0xfff)
                                    ig = 0;
                                else if (g == 0x100)
                                    ig = 0xff;

                                b &= 0xfff;
                                ib = b;
                                if (b == 0xfff)
                                    ib = 0;
                                else if (b == 0x100)
                                    ib = 0xff;

                                a &= 0xfff;
                                ia = a;
                                if (a == 0xfff)
                                    ia = 0;
                                else if (a == 0x100)
                                    ia = 0xff;
                            }
                            else
                            {
                                ir = (r < 0) ? 0 : (r > 0xff) ? 0xff : r;
                                ig = (g < 0) ? 0 : (g > 0xff) ? 0xff : g;
                                ib = (b < 0) ? 0 : (b > 0xff) ? 0xff : b;
                                ia = (a < 0) ? 0 : (a > 0xff) ? 0xff : a;
                            }
                            iterargb.value = setRegRGBA(ir, ig, ib, ia);
                        }

                        // COLORPATH_PIPELINE(v, stats, v.reg[FBZCOLORPATH], v.reg[FBZMODE], v.reg[ALPHAMODE], texel, iterz, iterw, iterargb);
                        // #define COLORPATH_PIPELINE(VV, STATS, FBZCOLORPATH, FBZMODE, ALPHAMODE, TEXELARGB, ITERZ, ITERW, ITERARGB) \
                        int blendr, blendg, blendb, blenda;
                        int c_other;
                        int c_local;

                        /* compute c_other */
                        switch (FBZCP_CC_RGBSELECT(FBZCOLORPATH))
                        {
                            case 0:     /* iterated RGB */
                                c_other = iterargb.value;
                                break;
                            case 1:     /* texture RGB */
                                c_other = texel.value;
                                break;
                            case 2:     /* color1 RGB */
                                c_other = v.reg[color1];
                                break;
                            default:    /* reserved */
                                c_other = 0;
                                break;
                        }

                        /* handle chroma key */
                        if (!v.APPLY_CHROMAKEY(stats, FBZMODE, c_other))
                            return false;

                        /* compute a_other */
                        switch (FBZCP_CC_ASELECT(FBZCOLORPATH))
                        {
                            case 0:     /* iterated alpha */
                                c_other = setRegA(c_other, getRegA(iterargb.value));
                                break;

                            case 1:     /* texture alpha */
                                c_other = setRegA(c_other, getRegA(texel.value));
                                break;

                            case 2:     /* color1 alpha */
                                c_other = setRegA(c_other, getRegA(v.reg[color1]));
                                break;

                            default:    /* reserved */
                                c_other = setRegA(c_other, 0);
                                break;
                        }

                        /* handle alpha mask */
                        if (!v.APPLY_ALPHAMASK(stats, FBZMODE, getRegA(c_other)))
                            return false;

                        /* handle alpha test */
                        if (!v.APPLY_ALPHATEST(stats, ALPHAMODE, getRegA(c_other)))
                            return false;

                        /* compute c_local */
                        if (!FBZCP_CC_LOCALSELECT_OVERRIDE(FBZCOLORPATH))
                        {
                            if (!FBZCP_CC_LOCALSELECT(FBZCOLORPATH))    /* iterated RGB */
                                c_local = iterargb.value;
                            else                                            /* color0 RGB */
                                c_local = v.reg[color0];
                        }
                        else
                        {
                            if ((getRegA(texel.value) & 0x80)==0)                  /* iterated RGB */
                                c_local = iterargb.value;
                            else                                            /* color0 RGB */
                                c_local = v.reg[color0];
                        }

                        /* compute a_local */
                        switch (FBZCP_CCA_LOCALSELECT(FBZCOLORPATH))
                        {
                            default:
                            case 0:     /* iterated alpha */
                                c_local = setRegA(c_local, getRegA(iterargb.value));
                                break;

                            case 1:     /* color0 alpha */
                                c_local = setRegA(c_local, getRegA(v.reg[color0]));
                                break;

                            case 2:     /* clamped iterated Z[27:20] */
                            {
                                int temp = CLAMPED_Zr(iterz.value, FBZCOLORPATH);
                                c_local = setRegA(c_local, temp & 0xFF);
                                break;
                            }

                            case 3:     /* clamped iterated W[39:32] */
                            {
                                int temp = CLAMPED_Wr(iterw.value, FBZCOLORPATH);           /* Voodoo 2 only */
                                c_local = setRegA(c_local, temp & 0xFF);
                                break;
                            }
                        }

                        /* select zero or c_other */
                        if (!FBZCP_CC_ZERO_OTHER(FBZCOLORPATH))
                        {
                            r = getRegR(c_other);
                            g = getRegG(c_other);
                            b = getRegB(c_other);
                        }
                        else
                            r = g = b = 0;

                        /* select zero or a_other */
                        if (!FBZCP_CCA_ZERO_OTHER(FBZCOLORPATH))
                            a = getRegA(c_other);
                        else
                            a = 0;

                        /* subtract c_local */
                        if (FBZCP_CC_SUB_CLOCAL(FBZCOLORPATH))
                        {
                            r -= getRegR(c_local);
                            g -= getRegG(c_local);
                            b -= getRegB(c_local);
                        }

                        /* subtract a_local */
                        if (FBZCP_CCA_SUB_CLOCAL(FBZCOLORPATH))
                            a -= getRegA(c_local);

                        /* blend RGB */
                        switch (FBZCP_CC_MSELECT(FBZCOLORPATH))
                        {
                            default:    /* reserved */
                            case 0:     /* 0 */
                                blendr = blendg = blendb = 0;
                                break;

                            case 1:     /* c_local */
                                blendr = getRegR(c_local);
                                blendg = getRegG(c_local);
                                blendb = getRegB(c_local);
                                break;

                            case 2:     /* a_other */
                                blendr = blendg = blendb = getRegA(c_other);
                                break;

                            case 3:     /* a_local */
                                blendr = blendg = blendb = getRegA(c_local);
                                break;

                            case 4:     /* texture alpha */
                                blendr = blendg = blendb = getRegA(texel.value);
                                break;

                            case 5:     /* texture RGB (Voodoo 2 only) */
                                blendr = getRegR(texel.value);
                                blendg = getRegG(texel.value);
                                blendb = getRegB(texel.value);
                                break;
                        }

                        /* blend alpha */
                        switch (FBZCP_CCA_MSELECT(FBZCOLORPATH))
                        {
                            default:    /* reserved */
                            case 0:     /* 0 */
                                blenda = 0;
                                break;

                            case 1:     /* a_local */
                                blenda = getRegA(c_local);
                                break;

                            case 2:     /* a_other */
                                blenda = getRegA(c_other);
                                break;

                            case 3:     /* a_local */
                                blenda = getRegA(c_local);
                                break;

                            case 4:     /* texture alpha */
                                blenda = getRegA(texel.value);
                                break;
                        }

                        /* reverse the RGB blend */
                        if (!FBZCP_CC_REVERSE_BLEND(FBZCOLORPATH))
                        {
                            blendr ^= 0xff;
                            blendg ^= 0xff;
                            blendb ^= 0xff;
                        }

                        /* reverse the alpha blend */
                        if (!FBZCP_CCA_REVERSE_BLEND(FBZCOLORPATH))
                            blenda ^= 0xff;

                        /* do the blend */
                        r = (r * (blendr + 1)) >> 8;
                        g = (g * (blendg + 1)) >> 8;
                        b = (b * (blendb + 1)) >> 8;
                        a = (a * (blenda + 1)) >> 8;

                        /* add clocal or alocal to RGB */
                        switch (FBZCP_CC_ADD_ACLOCAL(FBZCOLORPATH))
                        {
                            case 3:     /* reserved */
                            case 0:     /* nothing */
                                break;

                            case 1:     /* add c_local */
                                r += getRegR(c_local);
                                g += getRegG(c_local);
                                b += getRegB(c_local);
                                break;

                            case 2:     /* add_alocal */
                                r += getRegA(c_local);
                                g += getRegA(c_local);
                                b += getRegA(c_local);
                                break;
                        }

                        /* add clocal or alocal to alpha */
                        if (FBZCP_CCA_ADD_ACLOCAL(FBZCOLORPATH)!=0)
                            a += getRegA(c_local);

                        /* clamp */
                        r = CLAMPr(r, 0x00, 0xff);
                        g = CLAMPr(g, 0x00, 0xff);
                        b = CLAMPr(b, 0x00, 0xff);
                        a = CLAMPr(a, 0x00, 0xff);

                        /* invert */
                        if (FBZCP_CC_INVERT_OUTPUT(FBZCOLORPATH))
                        {
                            r ^= 0xff;
                            g ^= 0xff;
                            b ^= 0xff;
                        }
                        if (FBZCP_CCA_INVERT_OUTPUT(FBZCOLORPATH))
                            a ^= 0xff;
                        result.value = setRegRGBA(r, g, b, a);
                        return true;
                    }
                };
                v.PIXEL_PIPELINE(stats, x, y, FBZCOLORPATH, FBZMODE, ALPHAMODE, FOGMODE, iterz, iterw, dither, ditherPos, dither4, dither4Pos, dither_lookup, dither_lookupPos, destbase, destPos, v.fbi.ram, depthPos, callback);

                /* update the iterated parameters */
                iterr.value += extra.drdx;
                iterg.value += extra.dgdx;
                iterb.value += extra.dbdx;
                itera.value += extra.dadx;
                iterz.value += extra.dzdx;
                iterw.value += extra.dwdx;
                if (TMUS >= 1)
                {
                    iterw0 += extra.dw0dx;
                    iters0 += extra.ds0dx;
                    itert0 += extra.dt0dx;
                }
                if (TMUS >= 2)
                {
                    iterw1 += extra.dw1dx;
                    iters1 += extra.ds1dx;
                    itert1 += extra.dt1dx;
                }
            }
        }
    }
    
    /*-------------------------------------------------
        generic_0tmu - generic rasterizer for 0 TMUs
    -------------------------------------------------*/
    static final private RASTERIZER raster_generic_0tmu = new RASTERIZER("generic_0tmu", 0);

    /*-------------------------------------------------
        generic_1tmu - generic rasterizer for 1 TMU
    -------------------------------------------------*/
    static final private RASTERIZER raster_generic_1tmu = new RASTERIZER("generic_1tmu", 1);

    /*-------------------------------------------------
        generic_2tmu - generic rasterizer for 2 TMUs
    -------------------------------------------------*/
    static final private RASTERIZER raster_generic_2tmu = new RASTERIZER("generic_2tmu", 2);


    /*************************************
     *
     *  Common initialization
     *
     *************************************/

    static private int MAKE_ARGB(int a, int r,int g, int b)  { return (((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));}
    static private int MAKE_RGB(int r, int g, int b) { return MAKE_ARGB(255,r,g,b); }
    static private int RGB_ALPHA(int rgb) { return (((rgb) >> 24) & 0xff);}
    static private int RGB_RED(int rgb) { return (((rgb) >> 16) & 0xff);}
    static private int RGB_GREEN(int rgb) { return (((rgb) >> 8) & 0xff);}
    static private int RGB_BLUE(int rgb) { return ((rgb) & 0xff);}

    static private int pal5bit(int bits)
    {
    	bits &= 0x1f;
    	return (bits << 3) | (bits >> 2);
    }

    static private final class voodoo_draw {
    	int width;
        int height;
        int bpp;
    	float vfreq;
        VoodooCommon v;
    	double frame_start;
    	boolean doublewidth;
        boolean doubleheight;

        boolean clock_enabled;
        boolean output_on;
        boolean override_on;
        boolean screen_update_pending;
    }

    static voodoo_draw vdraw = new voodoo_draw();

    static final private Pic.PIC_EventHandler Voodoo_VerticalBlankTimer = new Pic.PIC_EventHandler() {
            public void call(int val) {
                vdraw.v.vblank_callback();
                Pic.PIC_AddEvent( Voodoo_VerticalTimer, vdraw.vfreq*5/100);
            }
    };

    static final private Pic.PIC_EventHandler Voodoo_VerticalTimer = new Pic.PIC_EventHandler() {
        public void call(int val) {
            if (vdraw.screen_update_pending)
                return;
            vdraw.v.vblank_off_callback();

            vdraw.frame_start = Pic.PIC_FullIndex();
            Pic.PIC_AddEvent( Voodoo_VerticalBlankTimer, vdraw.vfreq*95/100);

            if (!Render.RENDER_StartUpdate()) return; // frameskip

            // draw all lines at once
            for (int y=0;y<vdraw.v.fbi.height;y++) {
                int inOffset = vdraw.v.fbi.rgboffs[vdraw.v.fbi.frontbuf] / 2 + y*vdraw.v.fbi.rowpixels;
                int outOffset = Render.render.src.outPitch * y / 2;
                System.arraycopy(vdraw.v.fbi.ram, inOffset, Render.render.src.outWrite16, outOffset, vdraw.v.fbi.width);
            }
            Render.RENDER_EndUpdate(false);
        }
    };

    static void Voodoo_Output_Enable(boolean enabled) {
    	if (vdraw.output_on != enabled) {
    		vdraw.output_on = enabled;
    		Voodoo_UpdateScreenStart();
    	}
    }

    static void Voodoo_PCI_Enable(boolean enable) {
        if (enable != vdraw.clock_enabled) {
    	    vdraw.clock_enabled=enable;
    	    Voodoo_UpdateScreenStart();
        }
    }

    static final private Pic.PIC_EventHandler Voodoo_UpdateScreen = new Pic.PIC_EventHandler() {
        public void call(int val) {
            vdraw.screen_update_pending = false;
            // abort drawing
            Render.RENDER_EndUpdate(true);

            if ((!vdraw.clock_enabled || !vdraw.output_on)&& vdraw.override_on) {
                // switching off
                Pic.PIC_RemoveEvents(Voodoo_VerticalTimer);
                Pic.PIC_RemoveEvents(Voodoo_VerticalBlankTimer);
                VGA_draw.VGA_SetOverride(false);
                vdraw.override_on=false;
            }

            if ((vdraw.clock_enabled && vdraw.output_on)) {
                // switching on
                Pic.PIC_RemoveEvents(Voodoo_VerticalTimer); // shouldn't be needed

                // TODO proper implementation of refresh rates and timings
                vdraw.vfreq = 1000.0f/60.0f;
                if (!vdraw.override_on) {
                    VGA_draw.VGA_SetOverride(true);
                    vdraw.override_on=true;
                }
                vdraw.height=vdraw.v.fbi.height;
                Log.log_msg("Voodoo output "+(vdraw.v.fbi.width+1)+"x"+vdraw.v.fbi.height);

                Render.RENDER_SetSize(vdraw.v.fbi.width+1, vdraw.v.fbi.height, 16, vdraw.vfreq, 4.0/3.0, false, false);
                Voodoo_VerticalTimer.call(0);
            }
        }
    };

    static void Voodoo_UpdateScreenStart() {
    	if (!vdraw.screen_update_pending) {
    		vdraw.screen_update_pending=true;
    		Pic.PIC_AddEvent(Voodoo_UpdateScreen, 100.0f);
    	}
    }

    private void voodoo_set_init_enable(int newval)
    {
    	pci.init_enable = newval;
//    	if (LOG_REGISTERS)
//    		logerror("VOODOO.%d.REG:initEnable write = %08X\n", index, newval);
    }

    private void init_fbi(short[] memory, int fbmem)
    {
    	int pen;
    
    	/* allocate frame buffer RAM and set pointers */
    	fbi.ram = memory;
    	fbi.mask = fbmem - 1;
    	fbi.rgboffs[0] = fbi.rgboffs[1] = fbi.rgboffs[2] = 0;
    	fbi.auxoffs = -1;
    
    	/* default to 0x0 */
    	fbi.frontbuf = 0;
    	fbi.backbuf = 1;
    	fbi.width = 512;
    	fbi.height = 384;
    
    	/* init the pens */
    	fbi.clut_dirty = true;
    	if (type <= TYPE_VOODOO_2)
    	{
    		for (pen = 0; pen < 32; pen++)
    			fbi.clut[pen] = MAKE_ARGB(pen, pal5bit(pen), pal5bit(pen), pal5bit(pen));
    		fbi.clut[32] = MAKE_ARGB(32,0xff,0xff,0xff);
    	}
    	else
    	{
    		for (pen = 0; pen < 512; pen++)
    			fbi.clut[pen] = MAKE_RGB(pen,pen,pen);
    	}

    	fbi.vblank = false;
    
    	/* initialize the memory FIFO */
    	fbi.fifo.base = -1;
    	fbi.fifo.size = fbi.fifo.in = fbi.fifo.out = 0;
    
    	/* set the fog delta mask */
    	fbi.fogdelta_mask = (type < TYPE_VOODOO_2) ? 0xff : 0xfc;
    }
    
    
    private void init_tmu_shared()
    {
    	int val;
    
    	/* build static 8-bit texel tables */
    	for (val = 0; val < 256; val++)
    	{
    		int r, g, b, a;
    
    		/* 8-bit RGB (3-3-2) */
    		// EXTRACT_332_TO_888(val, r, g, b);
            r = (((val) >> 0) & 0xe0) | (((val) >> 3) & 0x1c) | (((val) >> 6) & 0x03);
            g = (((val) << 3) & 0xe0) | (((val) >> 0) & 0x1c) | (((val) >> 3) & 0x03);
            b = (((val) << 6) & 0xc0) | (((val) << 4) & 0x30) | (((val) << 2) & 0xc0) | (((val) << 0) & 0x03);

    		tmushare.rgb332[val] = MAKE_ARGB(0xff, r, g, b);
    
    		/* 8-bit alpha */
    		tmushare.alpha8[val] = MAKE_ARGB(val, val, val, val);
    
    		/* 8-bit intensity */
    		tmushare.int8[val] = MAKE_ARGB(0xff, val, val, val);
    
    		/* 8-bit alpha, intensity */
    		a = ((val >> 0) & 0xf0) | ((val >> 4) & 0x0f);
    		r = ((val << 4) & 0xf0) | ((val << 0) & 0x0f);
    		tmushare.ai44[val] = MAKE_ARGB(a, r, r, r);
    	}
    
    	/* build static 16-bit texel tables */
    	for (val = 0; val < 65536; val++)
    	{
    		int r, g, b, a;
    
    		/* table 10 = 16-bit RGB (5-6-5) */
    		// EXTRACT_565_TO_888(val, r, g, b);
            r = (((val) >> 8) & 0xf8) | (((val) >> 13) & 0x07);
            g = (((val) >> 3) & 0xfc) | (((val) >> 9) & 0x03);
            b = (((val) << 3) & 0xf8) | (((val) >> 2) & 0x07);

    		tmushare.rgb565[val] = MAKE_ARGB(0xff, r, g, b);
    
    		/* table 11 = 16 ARGB (1-5-5-5) */
    		// EXTRACT_1555_TO_8888(val, a, r, g, b);
            a = (((short)(val)) >> 15) & 0xff;
            r = (((val) >> 7) & 0xf8) | (((val) >> 12) & 0x07);
            g = (((val) >> 2) & 0xf8) | (((val) >> 7) & 0x07);
            b = (((val) << 3) & 0xf8) | (((val) >> 2) & 0x07);

    		tmushare.argb1555[val] = MAKE_ARGB(a, r, g, b);
    
    		/* table 12 = 16-bit ARGB (4-4-4-4) */
    		//EXTRACT_4444_TO_8888(val, a, r, g, b);
            a = (((val) >> 8) & 0xf0) | (((val) >> 12) & 0x0f);
            r = (((val) >> 4) & 0xf0) | (((val) >> 8) & 0x0f);
            g = (((val) >> 0) & 0xf0) | (((val) >> 4) & 0x0f);
            b = (((val) << 4) & 0xf0) | (((val) >> 0) & 0x0f);
    		tmushare.argb4444[val] = MAKE_ARGB(a, r, g, b);
    	}
    }
    
    
    private void init_tmu(tmu_state t, int reg, byte[] memory, int tmem)
    {
    	/* allocate texture RAM */
    	t.ram = memory;
    	t.mask = tmem - 1;
    	t.reg = reg;
    	t.regdirty = true;
    	t.bilinear_mask = (type >= TYPE_VOODOO_2) ? 0xff : 0xf0;
    
    	/* mark the NCC tables dirty and configure their registers */
    	t.ncc[0].dirty = t.ncc[1].dirty = true;
    	t.ncc[0].reg = t.reg+nccTable+0;
    	t.ncc[1].reg = t.reg+nccTable+12;
    
    	/* create pointers to all the tables */
    	t.texel[0] = tmushare.rgb332;
    	t.texel[1] = t.ncc[0].texel;
    	t.texel[2] = tmushare.alpha8;
    	t.texel[3] = tmushare.int8;
    	t.texel[4] = tmushare.ai44;
    	t.texel[5] = t.palette;
    	t.texel[6] = (type >= TYPE_VOODOO_2) ? t.palettea : null;
    	t.texel[7] = null;
    	t.texel[8] = tmushare.rgb332;
    	t.texel[9] = t.ncc[0].texel;
    	t.texel[10] = tmushare.rgb565;
    	t.texel[11] = tmushare.argb1555;
    	t.texel[12] = tmushare.argb4444;
    	t.texel[13] = tmushare.int8;
    	t.texel[14] = t.palette;
    	t.texel[15] = null;
    	t.lookup = t.texel[0];
    
    	/* attach the palette to NCC table 0 */
    	t.ncc[0].palette = t.palette;
    	if (type >= TYPE_VOODOO_2)
    		t.ncc[0].palettea = t.palettea;
    
    	/* set up texture address calculations */
    	if (type <= TYPE_VOODOO_2)
    	{
    		t.texaddr_mask = 0x0fffff;
    		t.texaddr_shift = 3;
    	}
    	else
    	{
    		t.texaddr_mask = 0xfffff0;
    		t.texaddr_shift = 0;
    	}
    }
    
    private void common_start_voodoo(int type, int configFbmem, int configTmumem0, int configTmumem1, voodoo_stall_func configStall, voodoo_vblank_func configVblank)
    {
    	raster_info info;
    	short[] fbmem;
        byte[][] tmumem = new byte[2][];
    	int tmumem0;
    	int val;

    	/* store a pointer back to the device */
    	this.type = type;

    	/* copy config data */
    	freq = 33000000;
    	fbi.vblank_client = configVblank;
    	pci.stall_callback = configStall;

    	/* create a multiprocessor work queue */
    	poly = Poly.poly_alloc(64, 0, poly_extra_data.create(64));
    	thread_stats = stats_block.create(Poly.WORK_MAX_THREADS);

    	/* configure type-specific values */
    	switch (type)
    	{
    		case TYPE_VOODOO_1:
    			regaccess = voodoo_register_access;
    			regnames = voodoo_reg_name;
    			alt_regmap = false;
    			fbi.lfb_stride = 10;
    			break;

    		case TYPE_VOODOO_2:
    			regaccess = voodoo2_register_access;
    			regnames = voodoo_reg_name;
    			alt_regmap = false;
    			fbi.lfb_stride = 10;
    			break;

    		case TYPE_VOODOO_BANSHEE:
    			regaccess = banshee_register_access;
    			regnames = banshee_reg_name;
    			alt_regmap = true;
    			fbi.lfb_stride = 11;
    			break;

    		case TYPE_VOODOO_3:
    			regaccess = banshee_register_access;
    			regnames = banshee_reg_name;
    			alt_regmap = true;
    			fbi.lfb_stride = 11;
    			break;

    		default:
    			Log.exit("Unsupported voodoo card in voodoo_start!");
    			break;
    	}

    	chipmask = 0x01;
    	attoseconds_per_cycle = ATTOSECONDS_PER_SECOND / freq;
    	trigger = 51324 + pci_id;

//        add_rasterizer(new Logo.Rast1());
//        add_rasterizer(new Logo.Rast2());
//        add_rasterizer(new Logo.Rast3());
//        add_rasterizer(new Logo.Rast4());
    	/* build the rasterizer table */
    	//for (info = predef_raster_table; info.callback; info++)
    	//	add_rasterizer(v, info);

    	/* set up the PCI FIFO */
    	pci.fifo.base = 0; //pci.fifo_mem;
    	pci.fifo.size = 64*2;
    	pci.fifo.in = pci.fifo.out = 0;
    	pci.stall_state = NOT_STALLED;
    	//pci.continue_timer = device.machine().scheduler().timer_alloc(FUNC(stall_cpu_callback), v);

    	/* allocate memory */
    	tmumem0 = configTmumem0;
    	if (type <= TYPE_VOODOO_2)
    	{
    		/* separate FB/TMU memory */
    		fbmem = new short[configFbmem << 19];
    		tmumem[0] = new byte[configTmumem0 << 20];
    		tmumem[1] = (configTmumem1 != 0) ? new byte[configTmumem1 << 20] : null;
    	}
    	else
    	{
    		/* shared memory */
            Log.exit("Voodoo shared memory not implemented yet");
            fbmem = new short[configFbmem << 19];
    		tmumem[0] = tmumem[1] = new byte[configFbmem << 20];
    		tmumem0 = configFbmem;
    	}

    	/* set up frame buffer */
    	init_fbi(fbmem, configFbmem << 20);

    	/* build shared TMU tables */
    	init_tmu_shared();

    	/* set up the TMUs */
    	init_tmu(tmu[0], 0x100, tmumem[0], tmumem0 << 20);
    	chipmask |= 0x02;
    	if (configTmumem1 != 0 || type == TYPE_VOODOO_3)
    	{
    		init_tmu(tmu[1], 0x200, tmumem[1], configTmumem1 << 20);
    		chipmask |= 0x04;
    	}

    	/* initialize some registers */
        Arrays.fill(reg, 0);
    	pci.init_enable = 0;
    	reg[fbiInit0] = (1 << 4) | (0x10 << 6);
    	reg[fbiInit1] = (1 << 1) | (1 << 8) | (1 << 12) | (2 << 20);
    	reg[fbiInit2] = (1 << 6) | (0x100 << 23);
    	reg[fbiInit3] = (2 << 13) | (0xf << 17);
    	reg[fbiInit4] = (1 << 0);

    	/* initialize banshee registers */
    	Arrays.fill(banshee.io, 0);
    	banshee.io[io_pciInit0] = 0x01800040;
    	banshee.io[io_sipMonitor] = 0x40000000;
    	banshee.io[io_lfbMemoryConfig] = 0x000a2200;
    	banshee.io[io_dramInit0] = 0x00579d29;
    	banshee.io[io_dramInit0] |= 0x08000000;      // Konami Viper expects 16MBit SGRAMs
    	banshee.io[io_dramInit1] = 0x00f02200;
    	banshee.io[io_tmuGbeInit] = 0x00000bfb;

    	/* do a soft reset to reset everything else */
    	//soft_reset(v);
    }

    /*************************************
     *
     *  Chroma keying macro
     *
     *************************************/
    public boolean APPLY_CHROMAKEY(stats_block STATS, int FBZMODE, int COLOR) {
        if (FBZMODE_ENABLE_CHROMAKEY(FBZMODE))
        {
            /* non-range version */
            if (!CHROMARANGE_ENABLE(reg[chromaRange]))
            {
                if (((COLOR ^ reg[chromaKey]) & 0xffffff) == 0)
                {
                    STATS.chroma_fail++;
                    return false;
                }
            }

            /* tricky range version */
            else
            {
                int low, high, test;
                int results = 0;

                /* check blue */
                low = getRegB(reg[chromaKey]);
                high = getRegB(reg[chromaRange]);
                test = getRegB(COLOR);
                results = (test >= low && test <= high)?1:0;
                results ^= CHROMARANGE_BLUE_EXCLUSIVE(reg[chromaRange]);
                results <<= 1;

                /* check green */
                low = getRegG(reg[chromaKey]);
                high = getRegG(reg[chromaRange]);
                test = getRegG(COLOR);
                results |= (test >= low && test <= high)?1:0;
                results ^= CHROMARANGE_GREEN_EXCLUSIVE(reg[chromaRange]);
                results <<= 1;

                /* check red */
                low = getRegR(reg[chromaKey]);
                high = getRegR(reg[chromaRange]);
                test = getRegR(COLOR);
                results |= (test >= low && test <= high)?1:0;
                results ^= CHROMARANGE_RED_EXCLUSIVE(reg[chromaRange]);

                /* final result */
                if (CHROMARANGE_UNION_MODE(reg[chromaRange]))
                {
                    if (results != 0)
                    {
                        STATS.chroma_fail++;
                        return false;
                    }
                }
                else
                {
                    if (results == 7)
                    {
                        STATS.chroma_fail++;
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /*************************************
     *
     *  Alpha masking macro
     *
     *************************************/
    public boolean APPLY_ALPHAMASK(stats_block STATS, int FBZMODE, int AA) {
        if (FBZMODE_ENABLE_ALPHA_MASK(FBZMODE))
        {
            if (((AA) & 1) == 0)
            {
                STATS.afunc_fail++;
                return false;
            }
        }
        return true;
    }

    /*************************************
     *
     *  Alpha testing macro
     *
     *************************************/
    public boolean APPLY_ALPHATEST(stats_block STATS, int ALPHAMODE, int AA) {
        if (ALPHAMODE_ALPHATEST(ALPHAMODE))
        {
            int alpharef = getRegA(reg[alphaMode]);
            switch (ALPHAMODE_ALPHAFUNCTION(ALPHAMODE))
            {
                case 0:     /* alphaOP = never */
                    STATS.afunc_fail++;
                    return false;
                case 1:     /* alphaOP = less than */
                    if ((AA) >= alpharef)
                    {
                        STATS.afunc_fail++;
                        return false;
                    }
                    break;
                case 2:     /* alphaOP = equal */
                    if ((AA) != alpharef)
                    {
                        STATS.afunc_fail++;
                        return false;
                    }
                    break;
                case 3:     /* alphaOP = less than or equal */
                    if ((AA) > alpharef)
                    {
                        STATS.afunc_fail++;
                        return false;
                    }
                    break;
                case 4:     /* alphaOP = greater than */
                    if ((AA) <= alpharef)
                    {
                        STATS.afunc_fail++;
                        return false;
                    }
                    break;
                case 5:     /* alphaOP = not equal */
                    if ((AA) == alpharef)
                    {
                        STATS.afunc_fail++;
                        return false;
                    }
                    break;
                case 6:     /* alphaOP = greater than or equal */
                    if ((AA) < alpharef)
                    {
                        STATS.afunc_fail++;
                        return false;
                    }
                    break;
                case 7:     /* alphaOP = always */
                    break;
            }
        }
        return true;
    }

    /*-------------------------------------------------
        rgba_bilinear_filter - bilinear filter between
        four pixel values; this code is derived from
        code provided by Michael Herf
    -------------------------------------------------*/

    static public int rgba_bilinear_filter(int rgb00, int rgb01, int rgb10, int rgb11, int u, int v)
    {
        int ag0, ag1, rb0, rb1;

    	rb0 = (rgb00 & 0x00ff00ff) + ((((rgb01 & 0x00ff00ff) - (rgb00 & 0x00ff00ff)) * u) >> 8);
    	rb1 = (rgb10 & 0x00ff00ff) + ((((rgb11 & 0x00ff00ff) - (rgb10 & 0x00ff00ff)) * u) >> 8);
    	rgb00 >>= 8;
    	rgb01 >>= 8;
    	rgb10 >>= 8;
    	rgb11 >>= 8;
    	ag0 = (rgb00 & 0x00ff00ff) + ((((rgb01 & 0x00ff00ff) - (rgb00 & 0x00ff00ff)) * u) >> 8);
    	ag1 = (rgb10 & 0x00ff00ff) + ((((rgb11 & 0x00ff00ff) - (rgb10 & 0x00ff00ff)) * u) >> 8);

    	rb0 = (rb0 & 0x00ff00ff) + ((((rb1 & 0x00ff00ff) - (rb0 & 0x00ff00ff)) * v) >> 8);
    	ag0 = (ag0 & 0x00ff00ff) + ((((ag1 & 0x00ff00ff) - (ag0 & 0x00ff00ff)) * v) >> 8);

    	return ((ag0 << 8) & 0xff00ff00) | (rb0 & 0x00ff00ff);
    }

    /*************************************
     *
     *  Texture pipeline macro
     *
     *************************************/
    private int TEXTURE_PIPELINE(tmu_state TT, int XX, byte[] DITHER4s, int DITHER4POS, int TEXMODE, int COTHER, int[] LOOKUP, int LODBASE, long ITERS, long ITERT, long ITERW) {
    	int blendr, blendg, blendb, blenda;
        int tr, tg, tb, ta;
        int oow, s, t, lod, ilod;
        int smax, tmax;
        int texbase;
        int c_local;

    	/* determine the S/T/LOD values for this texture */
    	if (TEXMODE_ENABLE_PERSPECTIVE(TEXMODE))
    	{
            IntRef tmp = new IntRef(0);
    		oow = fast_reciplog(ITERW, tmp);
            lod = tmp.value;
    		s = (int)(((long)oow * ITERS) >>> 29);
    		t = (int)(((long)oow * ITERT) >> 29);
    		lod += LODBASE;
    	}
    	else
    	{
    		s = (int)(ITERS >> 14);
    		t = (int)(ITERT >> 14);
    		lod = LODBASE;
    	}

    	/* clamp W */
    	if (TEXMODE_CLAMP_NEG_W(TEXMODE) && (ITERW) < 0)
    		s = t = 0;

    	/* clamp the LOD */
    	lod += TT.lodbias;
    	if (TEXMODE_ENABLE_LOD_DITHER(TEXMODE))
    		lod += DITHER4s[DITHER4POS+(XX & 3)] << 4;
    	if (lod < TT.lodmin)
    		lod = TT.lodmin;
    	if (lod > TT.lodmax)
    		lod = TT.lodmax;

    	/* now the LOD is in range; if we don't own this LOD, take the next one */
    	ilod = lod >> 8;
    	if (((TT.lodmask >> ilod) & 1)==0)
    		ilod++;

    	/* fetch the texture base */
    	texbase = TT.lodoffset[ilod];

    	/* compute the maximum s and t values at this LOD */
    	smax = TT.wmask >> ilod;
    	tmax = TT.hmask >> ilod;

    	/* determine whether we are point-sampled or bilinear */
    	if ((lod == TT.lodmin && !TEXMODE_MAGNIFICATION_FILTER(TEXMODE)) ||
    		(lod != TT.lodmin && !TEXMODE_MINIFICATION_FILTER(TEXMODE)))
    	{
    		/* point sampled */

    		int texel0;

    		/* adjust S/T for the LOD and strip off the fractions */
    		s >>= ilod + 18;
    		t >>= ilod + 18;

    		/* clamp/wrap S/T if necessary */
    		if (TEXMODE_CLAMP_S(TEXMODE))
    			s = CLAMPr(s, 0, smax);
    		if (TEXMODE_CLAMP_T(TEXMODE))
    			t = CLAMPr(t, 0, tmax);
    		s &= smax;
    		t &= tmax;
    		t *= smax + 1;

    		/* fetch texel data */
    		if (TEXMODE_FORMAT(TEXMODE) < 8)
    		{
    			texel0 = TT.ram[(texbase + t + s) & TT.mask] & 0xFF;
    			c_local = LOOKUP[texel0];
    		}
    		else
    		{
    			texel0 = mem_readw(TT.ram, (texbase + 2*(t + s)) & TT.mask);
    			if (TEXMODE_FORMAT(TEXMODE) >= 10 && TEXMODE_FORMAT(TEXMODE) <= 12) {
    				c_local = LOOKUP[texel0];
                } else
    				c_local = (LOOKUP[texel0 & 0xff] & 0xffffff) | ((texel0 & 0xff00) << 16);
    		}
    	}
    	else
    	{
    		/* bilinear filtered */

    		int texel0, texel1, texel2, texel3;
            int sfrac, tfrac;
            int s1, t1;

    		/* adjust S/T for the LOD and strip off all but the low 8 bits of */
    		/* the fraction */
    		s >>= ilod + 10;
    		t >>= ilod + 10;

    		/* also subtract 1/2 texel so that (0.5,0.5) = a full (0,0) texel */
    		s -= 0x80;
    		t -= 0x80;

    		/* extract the fractions */
    		sfrac = s & TT.bilinear_mask;
    		tfrac = t & TT.bilinear_mask;

    		/* now toss the rest */
    		s >>= 8;
    		t >>= 8;
    		s1 = s + 1;
    		t1 = t + 1;

    		/* clamp/wrap S/T if necessary */
    		if (TEXMODE_CLAMP_S(TEXMODE))
    		{
    			s = CLAMPr(s, 0, smax);
    			s1 = CLAMPr(s1, 0, smax);
    		}
    		if (TEXMODE_CLAMP_T(TEXMODE))
    		{
    			t = CLAMPr(t, 0, tmax);
    			t1 = CLAMPr(t1, 0, tmax);
    		}
    		s &= smax;
    		s1 &= smax;
    		t &= tmax;
    		t1 &= tmax;
    		t *= smax + 1;
    		t1 *= smax + 1;

    		/* fetch texel data */
    		if (TEXMODE_FORMAT(TEXMODE) < 8)
    		{
    			texel0 = TT.ram[(texbase + t + s) & TT.mask] & 0xFF;
    			texel1 = TT.ram[(texbase + t + s1) & TT.mask] & 0xFF;
    			texel2 = TT.ram[(texbase + t1 + s) & TT.mask] & 0xFF;
    			texel3 = TT.ram[(texbase + t1 + s1) & TT.mask] & 0xFF;
    			texel0 = (LOOKUP)[texel0];
    			texel1 = (LOOKUP)[texel1];
    			texel2 = (LOOKUP)[texel2];
    			texel3 = (LOOKUP)[texel3];
    		}
    		else
    		{
    			texel0 = mem_readw(TT.ram, (texbase + 2*(t + s)) & TT.mask);
    			texel1 = mem_readw(TT.ram, (texbase + 2*(t + s1)) & TT.mask);
    			texel2 = mem_readw(TT.ram, (texbase + 2*(t1 + s)) & TT.mask);
    			texel3 = mem_readw(TT.ram, (texbase + 2*(t1 + s1)) & TT.mask);
    			if (TEXMODE_FORMAT(TEXMODE) >= 10 && TEXMODE_FORMAT(TEXMODE) <= 12)
    			{
    				texel0 = (LOOKUP)[texel0];
    				texel1 = (LOOKUP)[texel1];
    				texel2 = (LOOKUP)[texel2];
    				texel3 = (LOOKUP)[texel3];
    			}
    			else
    			{
    				texel0 = ((LOOKUP)[texel0 & 0xff] & 0xffffff) | ((texel0 & 0xff00) << 16);
    				texel1 = ((LOOKUP)[texel1 & 0xff] & 0xffffff) | ((texel1 & 0xff00) << 16);
    				texel2 = ((LOOKUP)[texel2 & 0xff] & 0xffffff) | ((texel2 & 0xff00) << 16);
    				texel3 = ((LOOKUP)[texel3 & 0xff] & 0xffffff) | ((texel3 & 0xff00) << 16);
    			}
    		}

    		/* weigh in each texel */
    		c_local = rgba_bilinear_filter(texel0, texel1, texel2, texel3, sfrac, tfrac);
    	}

    	/* select zero/other for RGB */
    	if (!TEXMODE_TC_ZERO_OTHER(TEXMODE))
    	{
    		tr = getRegR(COTHER);
    		tg = getRegG(COTHER);
    		tb = getRegB(COTHER);
    	}
    	else
    		tr = tg = tb = 0;

    	/* select zero/other for alpha */
    	if (!TEXMODE_TCA_ZERO_OTHER(TEXMODE))
    		ta = getRegA(COTHER);
    	else
    		ta = 0;

    	/* potentially subtract c_local */
    	if (TEXMODE_TC_SUB_CLOCAL(TEXMODE))
    	{
    		tr -= getRegR(c_local);
    		tg -= getRegG(c_local);
    		tb -= getRegB(c_local);
    	}
    	if (TEXMODE_TCA_SUB_CLOCAL(TEXMODE))
    		ta -= getRegA(c_local);

    	/* blend RGB */
    	switch (TEXMODE_TC_MSELECT(TEXMODE))
    	{
    		default:    /* reserved */
    		case 0:     /* zero */
    			blendr = blendg = blendb = 0;
    			break;

    		case 1:     /* c_local */
    			blendr = getRegR(c_local);
    			blendg = getRegG(c_local);
    			blendb = getRegB(c_local);
    			break;

    		case 2:     /* a_other */
    			blendr = blendg = blendb = getRegA(COTHER);
    			break;

    		case 3:     /* a_local */
    			blendr = blendg = blendb = getRegA(c_local);
    			break;

    		case 4:     /* LOD (detail factor) */
    			if (TT.detailbias <= lod)
    				blendr = blendg = blendb = 0;
    			else
    			{
    				blendr = (((TT.detailbias - lod) << TT.detailscale) >> 8);
    				if (blendr > TT.detailmax)
    					blendr = TT.detailmax;
    				blendg = blendb = blendr;
    			}
    			break;

    		case 5:     /* LOD fraction */
    			blendr = blendg = blendb = lod & 0xff;
    			break;
    	}

    	/* blend alpha */
    	switch (TEXMODE_TCA_MSELECT(TEXMODE))
    	{
    		default:    /* reserved */
    		case 0:     /* zero */
    			blenda = 0;
    			break;

    		case 1:     /* c_local */
    			blenda = getRegA(c_local);
    			break;

    		case 2:     /* a_other */
    			blenda = getRegA(COTHER);
    			break;

    		case 3:     /* a_local */
    			blenda = getRegA(c_local);
    			break;

    		case 4:     /* LOD (detail factor) */
    			if (TT.detailbias <= lod)
    				blenda = 0;
    			else
    			{
    				blenda = (((TT.detailbias - lod) << TT.detailscale) >> 8);
    				if (blenda > TT.detailmax)
    					blenda = TT.detailmax;
    			}
    			break;

    		case 5:     /* LOD fraction */
    			blenda = lod & 0xff;
    			break;
    	}

    	/* reverse the RGB blend */
    	if (!TEXMODE_TC_REVERSE_BLEND(TEXMODE))
    	{
    		blendr ^= 0xff;
    		blendg ^= 0xff;
    		blendb ^= 0xff;
    	}

    	/* reverse the alpha blend */
    	if (!TEXMODE_TCA_REVERSE_BLEND(TEXMODE))
    		blenda ^= 0xff;

    	/* do the blend */
    	tr = (tr * (blendr + 1)) >> 8;
    	tg = (tg * (blendg + 1)) >> 8;
    	tb = (tb * (blendb + 1)) >> 8;
    	ta = (ta * (blenda + 1)) >> 8;

    	/* add clocal or alocal to RGB */
    	switch (TEXMODE_TC_ADD_ACLOCAL(TEXMODE))
    	{
    		case 3:     /* reserved */
    		case 0:     /* nothing */
    			break;

    		case 1:     /* add c_local */
    			tr += getRegR(c_local);
    			tg += getRegG(c_local);
    			tb += getRegB(c_local);
    			break;

    		case 2:     /* add_alocal */
    			tr += getRegA(c_local);
    			tg += getRegA(c_local);
    			tb += getRegA(c_local);
    			break;
    	}

    	/* add clocal or alocal to alpha */
    	if (TEXMODE_TCA_ADD_ACLOCAL(TEXMODE)!=0)
    		ta += getRegA(c_local);

    	/* clamp */
    	int rr = (tr < 0) ? 0 : (tr > 0xff) ? 0xff : tr;
    	int rg = (tg < 0) ? 0 : (tg > 0xff) ? 0xff : tg;
    	int rb = (tb < 0) ? 0 : (tb > 0xff) ? 0xff : tb;
    	int ra = (ta < 0) ? 0 : (ta > 0xff) ? 0xff : ta;
        int RESULT = setRegRGBA(rr, rg, rb, ra);

    	/* invert */
    	if (TEXMODE_TC_INVERT_OUTPUT(TEXMODE))
    		RESULT ^= 0x00ffffff;
    	if (TEXMODE_TCA_INVERT_OUTPUT(TEXMODE))
    		RESULT = setRegA(RESULT, getRegA(RESULT) ^ 0xff);
        return RESULT;
    }

    private interface PIXEL_PIPELINE_CALLBACK {
        public boolean call(IntRef iterargb, IntRef result);
    }

    private void PIXEL_PIPELINE(stats_block STATS, int XX, int YY, int FBZCOLORPATH, int FBZMODE, int ALPHAMODE, int FOGMODE, IntRef ITERZ, LongRef ITERW, byte[] DITHERs, int DITHERPOS, byte[] DITHER4s, int DITHER4POS, byte[] DITHER_LOOKUPs, int DITHER_LOOKUPPOS, short[] destbase, final int destPos, short[] depthbase, final int depthPos, PIXEL_PIPELINE_CALLBACK callback) {
    	int depthval, wfloat;

    	STATS.pixels_in++;

    	/* apply clipping */
    	/* note that for perf reasons, we assume the caller has done clipping */

    	/* handle stippling */
    	if (FBZMODE_ENABLE_STIPPLE(FBZMODE))
    	{
    		/* rotate mode */
    		if (!FBZMODE_STIPPLE_PATTERN(FBZMODE))
    		{
    			reg[stipple] = (reg[stipple] << 1) | (reg[stipple] >> 31);
    			if ((reg[stipple] & 0x80000000) == 0)
    			{
    				stats.total_stippled++;
    				return; //goto skipdrawdepth;
    			}
    		}

    		/* pattern mode */
    		else
    		{
    			int stipple_index = (((YY) & 3) << 3) | (~(XX) & 7);
    			if (((reg[stipple] >> stipple_index) & 1) == 0)
    			{
    				stats.total_stippled++;
    				return; //goto skipdrawdepth;
    			}
    		}
    	}

    	/* compute "floating point" W value (used for depth and fog) */
    	if ((ITERW.value & 0xffff00000000l)!=0)
    		wfloat = 0x0000;
    	else
    	{
    		int temp = (int)ITERW.value;
    		if ((temp & 0xffff0000) == 0)
    			wfloat = 0xffff;
    		else
    		{
    			int exp = Integer.numberOfLeadingZeros(temp);
    			wfloat = ((exp << 12) | ((~temp >> (19 - exp)) & 0xfff)) + 1;
    		}
    	}

    	/* compute depth value (W or Z) for this pixel */
    	if (!FBZMODE_WBUFFER_SELECT(FBZMODE))
            depthval = CLAMPED_Zr(ITERZ.value, FBZCOLORPATH);
    	else if (!FBZMODE_DEPTH_FLOAT_SELECT(FBZMODE))
    		depthval = wfloat;
    	else
    	{
    		if ((ITERZ.value & 0xf0000000)!=0)
    			depthval = 0x0000;
    		else
    		{
    			int temp = ITERZ.value << 4;
    			if ((temp & 0xffff0000) == 0)
    				depthval = 0xffff;
    			else
    			{
    				int exp = Integer.numberOfLeadingZeros(temp);
    				depthval = ((exp << 12) | ((~temp >> (19 - exp)) & 0xfff)) + 1;
    			}
    		}
    	}

    	/* add the bias */
    	if (FBZMODE_ENABLE_DEPTH_BIAS(FBZMODE))
    	{
    		depthval += (short)reg[zaColor];
    		depthval = CLAMPr(depthval, 0, 0xffff);
    	}

    	/* handle depth buffer testing */
    	if (FBZMODE_ENABLE_DEPTHBUF(FBZMODE))
    	{
    		int depthsource;

    		/* the source depth is either the iterated W/Z+bias or a */
    		/* constant value */
    		if (!FBZMODE_DEPTH_SOURCE_COMPARE(FBZMODE))
    			depthsource = depthval;
    		else
    			depthsource = reg[zaColor] & 0xFFFF;

    		/* test against the depth buffer */
    		switch (FBZMODE_DEPTH_FUNCTION(FBZMODE))
    		{
    			case 0:     /* depthOP = never */
    				STATS.zfunc_fail++;
    				return; //goto skipdrawdepth;
    			case 1:     /* depthOP = less than */
    				if (depthsource >= (depthbase[depthPos+XX] & 0xFFFF))
    				{
    					STATS.zfunc_fail++;
    					return; //goto skipdrawdepth;
    				}
    				break;
    			case 2:     /* depthOP = equal */
    				if (depthsource != (depthbase[depthPos+XX] & 0xFFFF))
    				{
    					STATS.zfunc_fail++;
                        return; //goto skipdrawdepth;
    				}
    				break;
    			case 3:     /* depthOP = less than or equal */
    				if (depthsource > (depthbase[depthPos+XX] & 0xFFFF))
    				{
    					STATS.zfunc_fail++;
                        return; //goto skipdrawdepth;
    				}
    				break;
    			case 4:     /* depthOP = greater than */
    				if (depthsource <= (depthbase[depthPos+XX] & 0xFFFF))
    				{
    					STATS.zfunc_fail++;
                        return; //goto skipdrawdepth;
    				}
    				break;
    			case 5:     /* depthOP = not equal */
    				if (depthsource == (depthbase[depthPos+XX] & 0xFFFF))
    				{
    					STATS.zfunc_fail++;
                        return; //goto skipdrawdepth;
    				}
    				break;
    			case 6:     /* depthOP = greater than or equal */
    				if (depthsource < (depthbase[depthPos+XX] & 0xFFFF))
    				{
    					STATS.zfunc_fail++;
                        return; //goto skipdrawdepth;
    				}
    				break;
    			case 7:     /* depthOP = always */
    				break;
    		}
    	}
        IntRef ITERAXXX = new IntRef(0);
        IntRef color = new IntRef(0);
        if (!callback.call(ITERAXXX, color))
            return;
        int r = getRegR(color.value);
        int g = getRegG(color.value);
        int b = getRegB(color.value);
        int a = getRegA(color.value);

        /* perform fogging */
        int prefogr = r;
        int prefogg = g;
        int prefogb = b;

        // APPLY_FOGGING(VV, FOGMODE, FBZCOLORPATH, XX, DITHER4, r, g, b, ITERZ, ITERW, ITERAXXX);
        if (FOGMODE_ENABLE_FOG(FOGMODE)) {
            int fogcolor = reg[fogColor];
            int fr, fg, fb;

            /* constant fog bypasses everything else */
            if (FOGMODE_FOG_CONSTANT(FOGMODE)) {
                fr = getRegR(fogcolor);
                fg = getRegG(fogcolor);
                fb = getRegB(fogcolor);
            }
            /* non-constant fog comes from several sources */
            else {
                int fogblend = 0;

                /* if fog_add is zero, we start with the fog color */
                if (!FOGMODE_FOG_ADD(FOGMODE)) {
                    fr = getRegR(fogcolor);
                    fg = getRegG(fogcolor);
                    fb = getRegB(fogcolor);
                } else {
                    fr = fg = fb = 0;
                }

                /* if fog_mult is zero, we subtract the incoming color */
                if (!FOGMODE_FOG_MULT(FOGMODE)) {
                    fr -= r;
                    fg -= g;
                    fb -= b;
                }

                /* fog blending mode */
                switch (FOGMODE_FOG_ZALPHA(FOGMODE)) {
                    case 0:     /* fog table */ {
                        int delta = fbi.fogdelta[wfloat >> 10];
                        int deltaval;

                        /* perform the multiply against lower 8 bits of wfloat */
                        deltaval = (delta & fbi.fogdelta_mask) * ((wfloat >> 2) & 0xff);

                        /* fog zones allow for negating this value */
                        if (FOGMODE_FOG_ZONES(FOGMODE) && (delta & 2) != 0)
                            deltaval = -deltaval;
                        deltaval >>= 6;

                        /* apply dither */
                        if (FOGMODE_FOG_DITHER(FOGMODE))
                            deltaval += DITHER4s[DITHER4POS + (XX & 3)];
                        deltaval >>= 4;

                        /* add to the blending factor */
                        fogblend = fbi.fogblend[wfloat >> 10] + deltaval;
                        break;
                    }
                    case 1:     /* iterated A */
                        fogblend = getRegA(ITERAXXX.value);
                        break;
                    case 2:     /* iterated Z */
                        fogblend = CLAMPED_Zr(ITERZ.value, FBZCOLORPATH);
                        fogblend >>= 8;
                        break;
                    case 3:     /* iterated W - Voodoo 2 only */
                        fogblend = CLAMPED_Wr(ITERW.value, FBZCOLORPATH);
                        break;
                }

                /* perform the blend */
                fogblend++;
                fr = (fr * fogblend) >> 8;
                fg = (fg * fogblend) >> 8;
                fb = (fb * fogblend) >> 8;
            }

            /* if fog_mult is 0, we add this to the original color */
            if (!FOGMODE_FOG_MULT(FOGMODE)) {
                r += fr;
                g += fg;
                b += fb;
            }
            /* otherwise this just becomes the new color */
            else {
                r = fr;
                g = fg;
                b = fb;
            }

            /* clamp */
            r = CLAMPr(r, 0x00, 0xff);
            g = CLAMPr(g, 0x00, 0xff);
            b = CLAMPr(b, 0x00, 0xff);
        }

        /* perform alpha blending */
        if (ALPHAMODE_ALPHABLEND(ALPHAMODE))
        {
            int dpix = destbase[destPos+XX] & 0xFFFF;
            int dr = (dpix >> 8) & 0xf8;
            int dg = (dpix >> 3) & 0xfc;
            int db = (dpix << 3) & 0xf8;
            int da = FBZMODE_ENABLE_ALPHA_PLANES(FBZMODE) ? (destbase[destPos+XX] & 0xFFFF) : 0xff;
            int sr = r;
            int sb = b;
            int sg = g;
            int sa = a;
            int ta;

            /* apply dither subtraction */
            if (FBZMODE_ALPHA_DITHER_SUBTRACT(FBZMODE))
            {
                /* look up the dither value from the appropriate matrix */
                int dith = DITHERs[DITHERPOS+(XX & 3)];

                /* subtract the dither value */
                dr = ((dr << 1) + 15 - dith) >> 1;
                dg = ((dg << 2) + 15 - dith) >> 2;
                db = ((db << 1) + 15 - dith) >> 1;
            }

            /* compute source portion */
            switch (ALPHAMODE_SRCRGBBLEND(ALPHAMODE))
            {
                default:    /* reserved */
                case 0:     /* AZERO */
                    r = g = b = 0;
                    break;
                case 1:     /* ASRC_ALPHA */
                    r = (sr * (sa + 1)) >> 8;
                    g = (sg * (sa + 1)) >> 8;
                    b = (sb * (sa + 1)) >> 8;
                    break;
                case 2:     /* A_COLOR */
                    r = (sr * (dr + 1)) >> 8;
                    g = (sg * (dg + 1)) >> 8;
                    b = (sb * (db + 1)) >> 8;
                    break;
                case 3:     /* ADST_ALPHA */
                    r = (sr * (da + 1)) >> 8;
                    g = (sg * (da + 1)) >> 8;
                    b = (sb * (da + 1)) >> 8;
                    break;
                case 4:     /* AONE */
                    break;
                case 5:     /* AOMSRC_ALPHA */
                    r = (sr * (0x100 - sa)) >> 8;
                    g = (sg * (0x100 - sa)) >> 8;
                    b = (sb * (0x100 - sa)) >> 8;
                    break;
                case 6:     /* AOM_COLOR */
                    r = (sr * (0x100 - dr)) >> 8;
                    g = (sg * (0x100 - dg)) >> 8;
                    b = (sb * (0x100 - db)) >> 8;
                    break;
                case 7:     /* AOMDST_ALPHA */
                    r = (sr * (0x100 - da)) >> 8;
                    g = (sg * (0x100 - da)) >> 8;
                    b = (sb * (0x100 - da)) >> 8;
                    break;
                case 15:    /* ASATURATE */
                    ta = (sa < (0x100 - da)) ? sa : (0x100 - da);
                    r = (sr * (ta + 1)) >> 8;
                    g = (sg * (ta + 1)) >> 8;
                    b = (sb * (ta + 1)) >> 8;
                    break;
            }

            /* add in dest portion */
            switch (ALPHAMODE_DSTRGBBLEND(ALPHAMODE))
            {
                default:    /* reserved */
                case 0:     /* AZERO */
                    break;
                case 1:     /* ASRC_ALPHA */
                    r += (dr * (sa + 1)) >> 8;
                    g += (dg * (sa + 1)) >> 8;
                    b += (db * (sa + 1)) >> 8;
                    break;
                case 2:     /* A_COLOR */
                    r += (dr * (sr + 1)) >> 8;
                    g += (dg * (sg + 1)) >> 8;
                    b += (db * (sb + 1)) >> 8;
                    break;
                case 3:     /* ADST_ALPHA */
                    r += (dr * (da + 1)) >> 8;
                    g += (dg * (da + 1)) >> 8;
                    b += (db * (da + 1)) >> 8;
                    break;
                case 4:     /* AONE */
                    r += dr;
                    g += dg;
                    b += db;
                    break;
                case 5:     /* AOMSRC_ALPHA */
                    r += (dr * (0x100 - sa)) >> 8;
                    g += (dg * (0x100 - sa)) >> 8;
                    b += (db * (0x100 - sa)) >> 8;
                    break;
                case 6:     /* AOM_COLOR */
                    r += (dr * (0x100 - sr)) >> 8;
                    g += (dg * (0x100 - sg)) >> 8;
                    b += (db * (0x100 - sb)) >> 8;
                    break;
                case 7:     /* AOMDST_ALPHA */
                    r += (dr * (0x100 - da)) >> 8;
                    g += (dg * (0x100 - da)) >> 8;
                    b += (db * (0x100 - da)) >> 8;
                    break;
                case 15:    /* A_COLORBEFOREFOG */
                    r += (dr * (prefogr + 1)) >> 8;
                    g += (dg * (prefogg + 1)) >> 8;
                    b += (db * (prefogb + 1)) >> 8;
                    break;
            }
            /* blend the source alpha */
            a = 0;
            if (ALPHAMODE_SRCALPHABLEND(ALPHAMODE) == 4)
                a = sa;
            /* blend the dest alpha */
            if (ALPHAMODE_DSTALPHABLEND(ALPHAMODE) == 4)
                a += da;
            /* clamp */
            r = CLAMPr(r, 0x00, 0xff);
            g = CLAMPr(g, 0x00, 0xff);
            b = CLAMPr(b, 0x00, 0xff);
            a = CLAMPr(a, 0x00, 0xff);
        }

        /* write to framebuffer */
        if (FBZMODE_RGB_BUFFER_MASK(FBZMODE))
        {
            /* apply dithering */
            //APPLY_DITHER(FBZMODE, XX, DITHER_LOOKUP, r, g, b);
            if (FBZMODE_ENABLE_DITHERING(FBZMODE))
            {
                /* look up the dither value from the appropriate matrix */
                int dithPos = DITHER_LOOKUPPOS + ((XX & 3) << 1);

                /* apply dithering to R,G,B */
                r = DITHER_LOOKUPs[dithPos+(r << 3) + 0];
                g = DITHER_LOOKUPs[dithPos+(g << 3) + 1];
                b = DITHER_LOOKUPs[dithPos+(b << 3) + 0];
            }
            else
            {
                r >>>= 3;
                g >>>= 2;
                b >>>= 3;
            }
            destbase[destPos+XX] = (short)((r << 11) | (g << 5) | b);
        }

        /* write to aux buffer */
        if (depthPos!=-1 && FBZMODE_AUX_BUFFER_MASK(FBZMODE))
        {
            if (!FBZMODE_ENABLE_ALPHA_PLANES(FBZMODE))
                depthbase[depthPos+XX] = (short)depthval;
            else
                depthbase[depthPos+XX] = (short)a;
        }

        /* track pixel writes to the frame buffer regardless of mask */
        STATS.pixels_out++;
    }

    public VoodooCommon(int deviceId, int fbRAM, int tmuRAM0, int tmuRAM1, int type) {
        super(0x121A, deviceId);
        setBAR(new PCI_Memory_BAR(new VoodooPageHandler(VOODOO_MEM >> 12, (VOODOO_MEM >> 12) + ((fbRAM+tmuRAM0+tmuRAM1+4) << 8))), 0); // 4 is for REGS
        PCI.pci_interface.RegisterPCIDevice(this, -1);
        for (int i=0;i<tmu.length;i++) {
            tmu[i] = new tmu_state();
        }
        for (int i=0;i<rasterizer.length;i++) {
            rasterizer[i] = new raster_info();
        }
        common_start_voodoo(type, fbRAM, tmuRAM0, tmuRAM1, null, null);
        vdraw.v = this;
    }

    public int ParseReadRegister(int regnum) {
        return regnum;
    }

    public boolean OverrideReadRegister(int regnum, IntRef rval, IntRef rval_mask) {
        return false;
    }

    public int ParseWriteRegister(int regnum, int value) {
        if (regnum == 0x40) {
            voodoo_set_init_enable(value);
        } else if (regnum == 0xc0) {
            Voodoo_PCI_Enable(true);
        } else if (regnum == 0xe0) {
            Voodoo_PCI_Enable(false);
        }
        return value;
    }

    public boolean InitializeRegisters(byte[] registers) {
        registers[0x09] = 0x00;
        // header type
        registers[0x0e] = 0x00;
        // command reg
        registers[0x04] = 0x02;
        // revision
        registers[0x08] = 0x02;
        // class
        registers[0x0b] = 0x04; // Multi-media
        // subclass
        registers[0x0a] = 0x00; // Video

        // memBaseAddr: size is 16MB,
        registers[0x10] = 0;
        registers[0x11] = 0;
        registers[0x12] = 0;
        registers[0x13] = (VOODOO_MEM >> 24) & 0xFF;
        return true;
    }
    
    /*************************************
     *
     *  Handle a read from the Voodoo
     *  memory space
     *
     *************************************/

    private class VoodooPageHandler extends PCI_PageHandler {
        public VoodooPageHandler(int start_page, int stop_page) {
            this.start_page = start_page;
            this.stop_page = stop_page;
            this.flags = Paging.PFLAG_NOCODE;
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            //Log.exit("No byte handler for read from " + Long.toString(addr, 16));
            return -1;
        }

        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            int address = addr;
            if ((address & 2)!=0)
                return readd(address-2) >>> 16;
            return readd(addr) & 0xFFFF;
        }

        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr);
            int offset = (addr>>2)&0x3FFFFF;
            /* if we have something pending, flush the FIFOs up to the current time */
            if (pci.op_pending)
                flush_fifos();

            /* target the appropriate location */
            if ((offset & (0xc00000/4))==0)
                return register_r(offset);
            else if ((offset & (0x800000/4))==0)
                return lfb_r(offset, false);
            return -1;
        }

        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            Log.exit("No byte handler for write to " + Long.toString(addr, 16));
        }

        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr);
            if ((addr & 2)==0)
                voodoo_w((addr>>2)&0x3FFFFF,val,0x0000ffff);
            else
                voodoo_w((addr>>2)&0x3FFFFF,val << 16 ,0xffff0000);
        }

        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr);
            voodoo_w((addr>>2)&0x3FFFFF,val,0xffffffff);
        }
    }

    public static VoodooCommon voodoo;

    public static Section.SectionFunction Voodoo_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            voodoo=null;
        }
    };

    public static Section.SectionFunction Voodoo_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            if (PCI.pci_interface != null) {
                Section_prop section = (Section_prop)sec;
                String type = section.Get_string("type");
                String fb = section.Get_string("framebuffer");
                String tm = section.Get_string("texturememory");
                if (type.equals("voodoo1")) {
                    voodoo = new VoodooCommon(0x0001, Integer.parseInt(fb), Integer.parseInt(tm), section.Get_bool("singletmu")?0:Integer.parseInt(tm), TYPE_VOODOO_1);
                } else if (type.equals("voodoo2")) {
                    voodoo = new VoodooCommon(0x0002, Integer.parseInt(fb), Integer.parseInt(tm), Integer.parseInt(tm), TYPE_VOODOO_2);
                }
                RasterizerCompiler.load();
                sec.AddDestroyFunction(Voodoo_ShutDown,false);
            }
        }
    };
}
