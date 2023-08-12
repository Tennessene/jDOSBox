package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.Paging;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.SVGACards;
import jdos.util.Ptr;

public class VGA {
    //Don't enable keeping changes and mapping lfb probably...
    public static final boolean VGA_LFB_MAPPED = true;
    //#define VGA_KEEP_CHANGES
    public static final int VGA_CHANGE_SHIFT = 9;

    public static final int M_CGA2 = 0;
    public static final int M_CGA4 = 1;
    public static final int M_EGA = 2;
    public static final int M_VGA = 3;
    public static final int M_LIN4 = 4;
    public static final int M_LIN8 = 5;
    public static final int M_LIN15 = 6;
    public static final int M_LIN16 = 7;
    public static final int M_LIN32 = 8;
    public static final int M_TEXT = 9;
    public static final int M_HERC_GFX = 10;
    public static final int M_HERC_TEXT = 11;
    public static final int M_CGA16 = 12;
    public static final int M_TANDY2 = 13;
    public static final int M_TANDY4 = 14;
    public static final int M_TANDY16 = 15;
    public static final int M_TANDY_TEXT = 16;
    public static final int M_ERROR = 17;


    public static final int CLK_25 = 25175;
    public static final int CLK_28 = 28322;

    public static final int MIN_VCO	= 180000;
    public static final int MAX_VCO = 360000;

    public static final int S3_CLOCK_REF = 14318;	/* KHz */
    public static int S3_CLOCK(int _M,int _N,int _R) {
        return ((S3_CLOCK_REF * ((_M) + 2)) / (((_N) + 2) * (1 << (_R))));
    }

    public static final int S3_MAX_CLOCK = 150000;	/* KHz */

    public static final int S3_XGA_1024		= 0x00;
    public static final int S3_XGA_1152		= 0x01;
    public static final int S3_XGA_640		= 0x40;
    public static final int S3_XGA_800		= 0x80;
    public static final int S3_XGA_1280		= 0xc0;
    public static final int S3_XGA_WMASK	= (S3_XGA_640|S3_XGA_800|S3_XGA_1024|S3_XGA_1152|S3_XGA_1280);

    public static final int S3_XGA_8BPP  = 0x00;
    public static final int S3_XGA_16BPP = 0x10;
    public static final int S3_XGA_32BPP = 0x30;
    public static final int S3_XGA_CMASK = (S3_XGA_8BPP|S3_XGA_16BPP|S3_XGA_32BPP);

    public static class VGA_Internal {
        public boolean attrindex;
    }

    public static class VGA_Config {
    /* Memory handlers */
        public /*Bitu*/int mh_mask;

    /* Video drawing */
        public /*Bitu*/int display_start;
        public /*Bitu*/int real_start;
        public boolean retrace;					/* A retrace is active */
        public /*Bitu*/int scan_len;
        public /*Bitu*/int cursor_start;

    /* Some other screen related variables */
        public /*Bitu*/int line_compare;
        public boolean chained;					/* Enable or Disabled Chain 4 Mode */
        public boolean compatible_chain4;

        /* Pixel Scrolling */
        public /*Bit8u*/short pel_panning;				/* Amount of pixels to skip when starting horizontal line */
        public /*Bit8u*/short hlines_skip;
        public /*Bit8u*/short bytes_skip;
        public /*Bit8u*/short addr_shift;

    /* Specific stuff memory write/read handling */

        public /*Bit8u*/short read_mode;
        public /*Bit8u*/short write_mode;
        public /*Bit8u*/short read_map_select;
        public /*Bit8u*/short color_dont_care;
        public /*Bit8u*/short color_compare;
        public /*Bit8u*/short data_rotate;
        public /*Bit8u*/short raster_op;

        public /*Bit32u*/int full_bit_mask;
        public /*Bit32u*/int full_map_mask;
        public /*Bit32u*/int full_not_map_mask;
        public /*Bit32u*/int full_set_reset;
        public /*Bit32u*/int full_not_enable_set_reset;
        public /*Bit32u*/int full_enable_set_reset;
        public /*Bit32u*/int full_enable_and_set_reset;
    }

    static final class Drawmode {
        static final public int PART = 0;
        static final public int LINE = 1;
        static final public int EGALINE = 2;
    }

    public static class VGA_Draw {
        public boolean resizing;
        public /*Bitu*/int width;
        public /*Bitu*/int height;
        public /*Bitu*/int blocks;
        public /*Bitu*/int address;
        public /*Bitu*/int panning;
        public /*Bitu*/int bytes_skip;
        public /*Bit8u*/int linear_base;
        public /*Bitu*/int linear_mask;
        public /*Bitu*/int address_add;
        public /*Bitu*/int line_length;
        public /*Bitu*/int address_line_total;
        public /*Bitu*/int address_line;
        public /*Bitu*/int lines_total;
        public /*Bitu*/int vblank_skip;
        public /*Bitu*/int lines_done;
        public /*Bitu*/int lines_scaled;
        public /*Bitu*/int split_line;
        public /*Bitu*/int parts_total;
        public /*Bitu*/int parts_lines;
        public /*Bitu*/int parts_left;
        public /*Bitu*/int byte_panning_shift;
        public static class Delay {
            double framestart;
            double vrstart, vrend;		// V-retrace
            double hrstart, hrend;		// H-retrace
            double hblkstart, hblkend;	// H-blanking
            double vblkstart, vblkend;	// V-Blanking
            double vdend, vtotal;
            double hdend, htotal;
            double parts;
        }
        public Delay delay = new Delay();
        public /*Bitu*/int bpp;
        public double aspect_ratio;
        public boolean double_scan;
        public boolean doublewidth,doubleheight;
        public /*Bit8u*/byte[] font = new byte[64*1024];
        public /*Bit8u*/Ptr[] font_tables = new Ptr[2];
        public /*Bitu*/boolean blinking;
        public boolean blink;
	    public boolean char9dot;
        public static class Cursor {
            /*Bitu*/int address;
            /*Bit8u*/short sline,eline;
            /*Bit8u*/short count,delay;
            /*Bit8u*/boolean enabled;
        }
        public Cursor cursor = new Cursor();
        public int mode;
        public boolean vret_triggered;
        public boolean vga_override;
    }

    public static class VGA_HWCURSOR {
        public /*Bit8u*/short curmode;
        public /*Bit16u*/int originx, originy;
        public /*Bit8u*/short fstackpos, bstackpos;
        public /*Bit8u*/Ptr forestack = new Ptr(4);
        public /*Bit8u*/Ptr backstack = new Ptr(4);
        public /*Bit16u*/int startaddr;
        public /*Bit8u*/short posx, posy;
        public /*Bit8u*/short[][] mc=new short[64][64];
    }

    public static class VGA_S3 {
        public VGA_S3() {
            for (int i=0;i<clk.length;i++) {
                clk[i] = new CLK();
            }
        }
        public /*Bit8u*/short reg_lock1;
        public /*Bit8u*/short reg_lock2;
        public /*Bit8u*/short reg_31;
        public /*Bit8u*/short reg_35;
        public /*Bit8u*/short reg_36; // RAM size
        public /*Bit8u*/short reg_3a; // 4/8/doublepixel bit in there
        public /*Bit8u*/short reg_40; // 8415/A functionality register
        public /*Bit8u*/short reg_41; // BIOS flags
        public /*Bit8u*/short reg_43;
        public /*Bit8u*/short reg_45; // Hardware graphics cursor
        public /*Bit8u*/short reg_50;
        public /*Bit8u*/short reg_51;
        public /*Bit8u*/short reg_52;
        public /*Bit8u*/short reg_55;
        public /*Bit8u*/short reg_58;
        public /*Bit8u*/short reg_6b; // LFB BIOS scratchpad
        public /*Bit8u*/short ex_hor_overflow;
        public /*Bit8u*/short ex_ver_overflow;
        public /*Bit16u*/int la_window;
        public /*Bit8u*/short misc_control_2;
        public /*Bit8u*/short ext_mem_ctrl;
        public /*Bitu*/int xga_screen_width;
        public int xga_color_mode;
        public static class CLK {
            /*Bit8u*/short r;
            /*Bit8u*/short n;
            /*Bit8u*/short m;
        }
        public CLK[] clk = new CLK[4];
        public CLK mclk = new CLK();

        public static class PLL{
            /*Bit8u*/short lock;
            /*Bit8u*/short cmd;
        }
        public PLL pll = new PLL();
        public VGA_HWCURSOR hgc = new VGA_HWCURSOR();
    }

    public static class VGA_HERC {
        public /*Bit8u*/byte mode_control;
        public /*Bit8u*/byte enable_bits;
    }

    public static class VGA_OTHER {
        public /*Bit8u*/short index;
        public /*Bit8u*/short htotal;
        public /*Bit8u*/short hdend;
        public /*Bit8u*/short hsyncp;
        public /*Bit8u*/short hsyncw;
        public /*Bit8u*/short vtotal;
        public /*Bit8u*/short vdend;
        public /*Bit8u*/short vadjust;
        public /*Bit8u*/short vsyncp;
        public /*Bit8u*/short vsyncw;
        public /*Bit8u*/short max_scanline;
        public /*Bit16u*/int lightpen;
        boolean lightpen_triggered;
        public /*Bit8u*/short cursor_start;
        public /*Bit8u*/short cursor_end;
    }

    public static class VGA_TANDY {
        public /*Bit8u*/boolean pcjr_flipflop;
        public /*Bit8u*/short mode_control;
        public /*Bit8u*/short color_select;
        public /*Bit8u*/short disp_bank;
        public /*Bit8u*/short reg_index;
        public /*Bit8u*/short gfx_control;
        public /*Bit8u*/short palette_mask;
        public /*Bit8u*/short extended_ram;
        public /*Bit8u*/short border_color;
        public /*Bit8u*/short line_mask, line_shift;
        public /*Bit8u*/short draw_bank, mem_bank;
        public /*Bit8u*/int draw_base, mem_base;
        public /*Bitu*/int addr_mask;
    }

    public static class VGA_Seq {
        public /*Bit8u*/short index;
        public /*Bit8u*/short reset;
        public /*Bit8u*/byte clocking_mode;
        public /*Bit8u*/byte map_mask;
        public /*Bit8u*/short character_map_select;
        public /*Bit8u*/short memory_mode;
    }

    public static class VGA_Attr {
        public /*Bit8u*/short[] palette = new short[16];
        public /*Bit8u*/short mode_control;
        public /*Bit8u*/short horizontal_pel_panning;
        public /*Bit8u*/short overscan_color;
        public /*Bit8u*/short color_plane_enable;
        public /*Bit8u*/short color_select;
        public /*Bit8u*/short index;
        public /*Bit8u*/byte disabled; // Used for disabling the screen.
                        // Bit0: screen disabled by attribute controller index
                        // Bit1: screen disabled by sequencer index 1 bit 5
                        // These are put together in one variable for performance reasons:
                        // the line drawing function is called maybe 60*480=28800 times/s,
                        // and we only need to check one variable for zero this way.
    }

    public static class VGA_Crtc {
        public /*Bit8u*/short horizontal_total;
        public /*Bit8u*/byte horizontal_display_end;
        public /*Bit8u*/short start_horizontal_blanking;
        public /*Bit8u*/short end_horizontal_blanking;
        public /*Bit8u*/short start_horizontal_retrace;
        public /*Bit8u*/short end_horizontal_retrace;
        public /*Bit8u*/short vertical_total;
        public /*Bit8u*/byte overflow;
        public /*Bit8u*/short preset_row_scan;
        public /*Bit8u*/byte maximum_scan_line;
        public /*Bit8u*/short cursor_start;
        public /*Bit8u*/short cursor_end;
        public /*Bit8u*/short start_address_high;
        public /*Bit8u*/short start_address_low;
        public /*Bit8u*/short cursor_location_high;
        public /*Bit8u*/short cursor_location_low;
        public /*Bit8u*/short vertical_retrace_start;
        public /*Bit8u*/short vertical_retrace_end;
        public /*Bit8u*/short vertical_display_end;
        public /*Bit8u*/byte offset;
        public /*Bit8u*/short underline_location;
        public /*Bit8u*/short start_vertical_blanking;
        public /*Bit8u*/short end_vertical_blanking;
        public /*Bit8u*/short mode_control;
        public /*Bit8u*/short line_compare;

        public /*Bit8u*/short index;
        public boolean read_only;
    }

    public static class VGA_Gfx {
        public /*Bit8u*/short index;
        public /*Bit8u*/short set_reset;
        public /*Bit8u*/short enable_set_reset;
        public /*Bit8u*/short color_compare;
        public /*Bit8u*/short data_rotate;
        public /*Bit8u*/short read_map_select;
        public /*Bit8u*/byte mode;
        public /*Bit8u*/byte miscellaneous;
        public /*Bit8u*/short color_dont_care;
        public /*Bit8u*/short bit_mask;
    }

    public static class RGBEntry  {
        public /*Bit8u*/short red;
        public /*Bit8u*/short green;
        public /*Bit8u*/short blue;
    }

    public static class VGA_Dac {
        public VGA_Dac() {
            for (int i=0;i<rgb.length;i++)
                rgb[i] = new RGBEntry();
        }
        public /*Bit8u*/short bits;						/* DAC bits, usually 6 or 8 */
        public /*Bit8u*/short pel_mask;
        public /*Bit8u*/short pel_index;
        public /*Bit8u*/short state;
        public /*Bit8u*/short write_index;
        public /*Bit8u*/short read_index;
        public /*Bitu*/int first_changed;
        public /*Bit8u*/short[] combine = new short[16];
        public RGBEntry[] rgb = new RGBEntry[0x100];
        public /*Bit16u*/int[] xlat16=new int[256];
    }

    public static class VGA_SVGA {
        public /*Bitu*/int	readStart, writeStart;
        public /*Bitu*/int	bankMask;
        public /*Bitu*/int	bank_read_full;
        public /*Bitu*/int	bank_write_full;
        public /*Bit8u*/short	bank_read;
        public /*Bit8u*/short	bank_write;
        public /*Bitu*/int	bank_size;
    }

    public static class VGA_Latch {
        public /*Bit32u*/int d;
        public /*Bit8u*/short b(int index) {
            switch (index) {
                case 0:
                    return (short)(d & 0xFF);
                case 1:
                    return (short)((d >> 8) & 0xFF);
                case 2:
                    return (short)((d >> 16) & 0xFF);
                case 3:
                    return (short)((d >> 24) & 0xFF);
            }
            return 0;
        }
    }

    public static class VGA_Memory {
        public /*Bit8u*/ int linear;
        public /*Bit8u*/int linear_orgptr;
    }

    public static class VGA_Changes {
        //Add a few more just to be safe
        public /*Bit8u*/Ptr	map; /* allocated dynamically: [(VGA_MEMORY >> VGA_CHANGE_SHIFT) + 32] */
        public /*Bit8u*/short	checkMask, frame, writeMask;
        public boolean	active;
        public /*Bit32u*/int  clearMask;
        public /*Bit32u*/int	start, last;
        public /*Bit32u*/int	lastAddress;
    }

    static public class VGA_LFB {
        public /*Bit32u*/int page;
        public /*Bit32u*/int addr;
        public /*Bit32u*/int mask;
        public Paging.PageHandler handler;
    }

    static public class VGA_Type {
        public int mode;								/* The mode the vga system is in */
        /*Bit8u*/short misc_output;
        public VGA_Draw draw = new VGA_Draw();
        public VGA_Config config = new VGA_Config();
        public VGA_Internal internal = new VGA_Internal();
    /* Internal module groups */
        public VGA_Seq seq = new VGA_Seq();
        public VGA_Attr attr = new VGA_Attr();
        public VGA_Crtc crtc = new VGA_Crtc();
        public VGA_Gfx gfx = new VGA_Gfx();
        public VGA_Dac dac = new VGA_Dac();
        public VGA_Latch latch = new VGA_Latch();
        public VGA_S3 s3 = new VGA_S3();
        public VGA_SVGA svga = new VGA_SVGA();
        public VGA_HERC herc = new VGA_HERC();
        public VGA_TANDY tandy = new VGA_TANDY();
        public VGA_OTHER other = new VGA_OTHER();
        public VGA_Memory mem = new VGA_Memory();
        public /*Bit32u*/int vmemwrap; /* this is assumed to be power of 2 */
        public /*Bit8u*/int fastmem;  /* memory for fast (usually 16-color) rendering, always twice as big as vmemsize */
        public /*Bit8u*/int fastmem_orgptr;
        public /*Bit32u*/int vmemsize;
        public VGA_Changes changes;
        public VGA_LFB lfb = new VGA_LFB();
    }

    /* Support for modular SVGA implementation */
    /* Video mode extra data to be passed to FinishSetMode_SVGA().
       This structure will be in flux until all drivers (including S3)
       are properly separated. Right now it contains only three overflow
       fields in S3 format and relies on drivers re-interpreting those.
       For reference:
       ver_overflow:X|line_comp10|X|vretrace10|X|vbstart10|vdispend10|vtotal10
       hor_overflow:X|X|X|hretrace8|X|hblank8|hdispend8|htotal8
       offset is not currently used by drivers (useful only for S3 itself)
       It also contains basic int10 mode data - number, vtotal, htotal
       */
    public static class VGA_ModeExtraData {
        public /*Bit8u*/short ver_overflow;
        public /*Bit8u*/short hor_overflow;
        public /*Bitu*/int offset;
        public /*Bitu*/int modeNo;
        public /*Bitu*/int htotal;
        public /*Bitu*/int vtotal;
    }

    // Vector function prototypes
    public static interface tWritePort {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen);
    }
    public static interface tReadPort {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen);
    }
    public static interface tFinishSetMode {
        public void call(/*Bitu*/int crtc_base, VGA_ModeExtraData modeData);
    }
    public static interface tDetermineMode {
        public void call();
    }
    public static interface tSetClock {
        public void call(/*Bitu*/int which,/*Bitu*/int target);
    }
    public static interface tGetClock {
        public /*Bitu*/int call();
    }
    public static interface tHWCursorActive {
        public boolean call();
    }
    public static interface tAcceptsMode {
        public boolean call(/*Bitu*/int modeNo);
    }

    public static class SVGA_Driver {
        public tWritePort write_p3d5;
        public tReadPort read_p3d5;
        public tWritePort write_p3c5;
        public tReadPort read_p3c5;
        public tWritePort write_p3c0;
        public tReadPort read_p3c1;
        public tWritePort write_p3cf;
        public tReadPort read_p3cf;

        public tFinishSetMode set_video_mode;
        public tDetermineMode determine_mode;
        public tSetClock set_clock;
        public tGetClock get_clock;
        public tHWCursorActive hardware_cursor_active;
        public tAcceptsMode accepts_mode;
    }

    public static VGA_Type vga;
    public static SVGA_Driver svga;

    public static /*Bit32u*/int[] CGA_2_Table=new int[16];
    public static /*Bit32u*/int[] CGA_4_Table=new int[256];
    public static /*Bit32u*/int[] CGA_4_HiRes_Table=new int[256];
    private static /*Bit32u*/int[] CGA_16_Table=new int[256];
    public static /*Bit32u*/int[] TXT_Font_Table=new int[16];
    public static /*Bit32u*/int[] TXT_FG_Table=new int[16];
    public static /*Bit32u*/int[] TXT_BG_Table=new int[16];
    public static /*Bit32u*/int[] ExpandTable=new int[256];
    public static /*Bit32u*/int[][] Expand16Table=new int[4][16];
    public static /*Bit32u*/int[] FillTable=new int[16];
    private static /*Bit32u*/int[] ColorTable=new int[16];

    public static void VGA_SetModeNow(int mode) {
        if (vga.mode == mode) return;
        vga.mode=mode;
        VGA_memory.VGA_SetupHandlers();
        VGA_StartResize(0);
    }


    public static void VGA_SetMode(int mode) {
        if (vga.mode == mode) return;
        vga.mode=mode;
        VGA_memory.VGA_SetupHandlers();
        VGA_StartResize();
    }

    public static void VGA_DetermineMode() {
        if (svga.determine_mode!=null) {
            svga.determine_mode.call();
            return;
        }
        /* Test for VGA output active or direct color modes */
        switch (vga.s3.misc_control_2 >> 4) {
        case 0:
            if ((vga.attr.mode_control & 1)!=0) { // graphics mode
                if (Dosbox.IS_VGA_ARCH() && (vga.gfx.mode & 0x40)!=0) {
                    // access above 256k?
                    if ((vga.s3.reg_31 & 0x8)!=0) VGA_SetMode(M_LIN8);
                    else VGA_SetMode(M_VGA);
                }
                else if ((vga.gfx.mode & 0x20)!=0) VGA_SetMode(M_CGA4);
                else if ((vga.gfx.miscellaneous & 0x0c)==0x0c) VGA_SetMode(M_CGA2);
                else {
                    // access above 256k?
                    if ((vga.s3.reg_31 & 0x8)!=0) VGA_SetMode(M_LIN4);
                    else VGA_SetMode(M_EGA);
                }
            } else {
                VGA_SetMode(M_TEXT);
            }
            break;
        case 1:VGA_SetMode(M_LIN8);break;
        case 3:VGA_SetMode(M_LIN15);break;
        case 5:VGA_SetMode(M_LIN16);break;
        case 13:VGA_SetMode(M_LIN32);break;
        }
    }

    static public void VGA_StartResize() {
        VGA_StartResize(50);
    }
    
    static public void VGA_StartResize(/*Bitu*/int delay /*=50*/) {
        if (!vga.draw.resizing) {
            vga.draw.resizing=true;
            if (vga.mode==M_ERROR) delay = 5;
            /* Start a resize after delay (default 50 ms) */
            if (delay==0) VGA_draw.VGA_SetupDrawing.call(0);
            else Pic.PIC_AddEvent(VGA_draw.VGA_SetupDrawing,(float)delay);
        }
    }

   public static void VGA_SetClock(/*Bitu*/int which,/*Bitu*/int target) {
        if (svga.set_clock!=null) {
            svga.set_clock.call(which, target);
            return;
        }

        /*Bits*/int best_err=target;
        /*Bitu*/int best_m=1;
        /*Bitu*/int best_n=1;
        /*Bitu*/int n,r;
        /*Bits*/int m;

        for (r = 0; r <= 3; r++) {
            /*Bitu*/int f_vco = target * (1 << r);
            if (MIN_VCO <= f_vco && f_vco < MAX_VCO) break;
        }
        for (n=1;n<=31;n++) {
            m=(target * (n + 2) * (1 << r) + (S3_CLOCK_REF/2)) / S3_CLOCK_REF - 2;
            if (0 <= m && m <= 127)	{
                /*Bitu*/int temp_target = S3_CLOCK(m,n,r);
                /*Bits*/int err = target - temp_target;
                if (err < 0) err = -err;
                if (err < best_err) {
                    best_err = err;
                    best_m = m;
                    best_n = n;
                }
            }
        }
        /* Program the s3 clock chip */
        vga.s3.clk[which].m=(short)best_m;
        vga.s3.clk[which].r=(short)r;
        vga.s3.clk[which].n=(short)best_n;
        VGA_StartResize();
    }

    public static void VGA_SetCGA2Table(/*Bit8u*/int val0,/*Bit8u*/int val1) {
        /*Bit8u*/byte[] total={ (byte)val0,(byte)val1};
        for (/*Bitu*/int i=0;i<16;i++) {
            CGA_2_Table[i]=
                (total[(i >> 3) & 1] << 0  ) | (total[(i >> 2) & 1] << 8  ) |
                (total[(i >> 1) & 1] << 16 ) | (total[(i >> 0) & 1] << 24 );
        }
    }

    public static void VGA_SetCGA4Table(/*Bit8u*/int val0,/*Bit8u*/int val1,/*Bit8u*/int val2,/*Bit8u*/int val3) {
        /*Bit8u*/byte[] total={ (byte)val0,(byte)val1,(byte)val2,(byte)val3};
        for (/*Bitu*/int i=0;i<256;i++) {
            CGA_4_Table[i]=
                (total[(i >> 6) & 3] << 0  ) | (total[(i >> 4) & 3] << 8  ) |
                (total[(i >> 2) & 3] << 16 ) | (total[(i >> 0) & 3] << 24 );
            CGA_4_HiRes_Table[i]=
                (total[((i >> 3) & 1) | ((i >> 6) & 2)] << 0  ) | (total[((i >> 2) & 1) | ((i >> 5) & 2)] << 8  ) |
                (total[((i >> 1) & 1) | ((i >> 4) & 2)] << 16 ) | (total[((i >> 0) & 1) | ((i >> 3) & 2)] << 24 );
        }
    }

    private static void SVGA_Setup_Driver() {
        svga = new SVGA_Driver();

        switch(Dosbox.svgaCard) {
        case SVGACards.SVGA_S3Trio:
            VGA_s3.SVGA_Setup_S3Trio();
            break;
        case SVGACards.SVGA_TsengET4K:
            VGA_tseng.SVGA_Setup_TsengET4K();
            break;
        case SVGACards.SVGA_TsengET3K:
            VGA_tseng.SVGA_Setup_TsengET3K();
            break;
        case SVGACards.SVGA_ParadisePVGA1A:
            VGA_paradise.SVGA_Setup_ParadisePVGA1A();
            break;
        default:
            vga.vmemsize = vga.vmemwrap = 256*1024;
            break;
        }
    }

    public static Section.SectionFunction VGA_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            if (Dosbox.svgaCard >= SVGACards.SVGA_QEMU)
                return;
            Section_prop section=(Section_prop)sec;
            vga.draw.resizing=false;
            vga.mode=M_ERROR;			//For first init
            vga.vmemsize=section.Get_int("vmemsize")*1024*1024;
            SVGA_Setup_Driver();
            VGA_memory.VGA_SetupMemory.call(section);
            VGA_misc.VGA_SetupMisc();
            VGA_dac.VGA_SetupDAC();
            VGA_gfx.VGA_SetupGFX();
            VGA_seq.VGA_SetupSEQ();
            VGA_attr.VGA_SetupAttr();
            VGA_other.VGA_SetupOther();
            VGA_xga.VGA_SetupXGA();
            VGA_SetClock(0,CLK_25);
            VGA_SetClock(1,CLK_28);
/* Generate tables */
            VGA_SetCGA2Table(0,1);
            VGA_SetCGA4Table(0,1,2,3);
            /*Bitu*/int i,j;
            for (i=0;i<256;i++) {
                ExpandTable[i]=i | (i << 8)| (i <<16) | (i << 24);
            }
            for (i=0;i<16;i++) {
                TXT_FG_Table[i]=i | (i << 8)| (i <<16) | (i << 24);
                TXT_BG_Table[i]=i | (i << 8)| (i <<16) | (i << 24);
                FillTable[i]=
                    ((i & 1)!=0 ? 0x000000ff : 0) |
                    ((i & 2)!=0 ? 0x0000ff00 : 0) |
                    ((i & 4)!=0 ? 0x00ff0000 : 0) |
                    ((i & 8)!=0 ? 0xff000000 : 0) ;
                TXT_Font_Table[i]=
                    ((i & 1)!=0 ? 0xff000000 : 0) |
                    ((i & 2)!=0 ? 0x00ff0000 : 0) |
                    ((i & 4)!=0 ? 0x0000ff00 : 0) |
                    ((i & 8)!=0 ? 0x000000ff : 0) ;
            }
            for (j=0;j<4;j++) {
                for (i=0;i<16;i++) {
                    Expand16Table[j][i] =
                        ((i & 1)!=0 ? 1 << (24 + j) : 0) |
                        ((i & 2)!=0 ? 1 << (16 + j) : 0) |
                        ((i & 4)!=0 ? 1 << (8 + j) : 0) |
                        ((i & 8)!=0 ? 1 << j : 0);
                }
            }

        }
    };

    static public void VGA_Init() {
        vga=new VGA_Type();
    }
}
