package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.IO;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;
import jdos.util.IntRef;
import jdos.util.ShortRef;

public class Int10 {
    public static final int S3_LFB_BASE=		0xC0000000;

    public static final int BIOSMEM_SEG=		0x40;

    public static final int BIOSMEM_INITIAL_MODE=  0x10;
    public static final int BIOSMEM_CURRENT_MODE=  0x49;
    public static final int BIOSMEM_NB_COLS=       0x4A;
    public static final int BIOSMEM_PAGE_SIZE=     0x4C;
    public static final int BIOSMEM_CURRENT_START= 0x4E;
    public static final int BIOSMEM_CURSOR_POS=    0x50;
    public static final int BIOSMEM_CURSOR_TYPE=   0x60;
    public static final int BIOSMEM_CURRENT_PAGE=  0x62;
    public static final int BIOSMEM_CRTC_ADDRESS=  0x63;
    public static final int BIOSMEM_CURRENT_MSR=   0x65;
    public static final int BIOSMEM_CURRENT_PAL=   0x66;
    public static final int BIOSMEM_NB_ROWS=       0x84;
    public static final int BIOSMEM_CHAR_HEIGHT=   0x85;
    public static final int BIOSMEM_VIDEO_CTL=     0x87;
    public static final int BIOSMEM_SWITCHES=      0x88;
    public static final int BIOSMEM_MODESET_CTL=   0x89;
    public static final int BIOSMEM_DCC_INDEX=     0x8A;
    public static final int BIOSMEM_CRTCPU_PAGE=   0x8A;
    public static final int BIOSMEM_VS_POINTER=    0xA8;

        /*
     *
     * VGA registers
     *
     */
    public static final int VGAREG_ACTL_ADDRESS=            0x3c0;
    public static final int VGAREG_ACTL_WRITE_DATA=         0x3c0;
    public static final int VGAREG_ACTL_READ_DATA=          0x3c1;

    public static final int VGAREG_INPUT_STATUS=            0x3c2;
    public static final int VGAREG_WRITE_MISC_OUTPUT=       0x3c2;
    public static final int VGAREG_VIDEO_ENABLE=            0x3c3;
    public static final int VGAREG_SEQU_ADDRESS=            0x3c4;
    public static final int VGAREG_SEQU_DATA=               0x3c5;

    public static final int VGAREG_PEL_MASK=                0x3c6;
    public static final int VGAREG_DAC_STATE=               0x3c7;
    public static final int VGAREG_DAC_READ_ADDRESS=        0x3c7;
    public static final int VGAREG_DAC_WRITE_ADDRESS=       0x3c8;
    public static final int VGAREG_DAC_DATA=                0x3c9;

    public static final int VGAREG_READ_FEATURE_CTL=        0x3ca;
    public static final int VGAREG_READ_MISC_OUTPUT=        0x3cc;

    public static final int VGAREG_GRDC_ADDRESS=            0x3ce;
    public static final int VGAREG_GRDC_DATA=               0x3cf;

    public static final int VGAREG_MDA_CRTC_ADDRESS=        0x3b4;
    public static final int VGAREG_MDA_CRTC_DATA=           0x3b5;
    public static final int VGAREG_VGA_CRTC_ADDRESS=        0x3d4;
    public static final int VGAREG_VGA_CRTC_DATA=           0x3d5;

    public static final int VGAREG_MDA_WRITE_FEATURE_CTL=   0x3ba;
    public static final int VGAREG_VGA_WRITE_FEATURE_CTL=   0x3da;
    public static final int VGAREG_ACTL_RESET=              0x3da;
    public static final int VGAREG_TDY_RESET=               0x3da;
    public static final int VGAREG_TDY_ADDRESS=             0x3da;
    public static final int VGAREG_TDY_DATA=                0x3de;
    public static final int VGAREG_PCJR_DATA=               0x3da;

    public static final int VGAREG_MDA_MODECTL=             0x3b8;
    public static final int VGAREG_CGA_MODECTL=             0x3d8;
    public static final int VGAREG_CGA_PALETTE=             0x3d9;

    /* Video memory */
    public static final int VGAMEM_GRAPH= 0xA000;
    public static final int VGAMEM_CTEXT= 0xB800;
    public static final int VGAMEM_MTEXT= 0xB000;

    //#define BIOS_NCOLS Bit16u ncols=real_readw(BIOSMEM_SEG,BIOSMEM_NB_COLS);
    //#define BIOS_NROWS Bit16u nrows=(Bit16u)real_readb(BIOSMEM_SEG,BIOSMEM_NB_ROWS)+1;

    public static class VideoModeBlock {
        public VideoModeBlock(int mode, int type, int swidth, int sheight, int twidth, int theight, int cwidth, int cheight,
        int	ptotal,int pstart,int plength, int htotal, int vtotal, int hdispend, int vdispend, int special) {
            this.mode = mode;
            this.type = type;
            this.swidth = swidth;
            this.sheight = sheight;
            this.twidth = twidth;
            this.theight = theight;
            this.cwidth = cwidth;
            this.cheight = cheight;
            this.ptotal = ptotal;
            this.pstart = pstart;
            this.plength = plength;
            this.htotal = htotal;
            this.vtotal = vtotal;
            this.hdispend = hdispend;
            this.vdispend = vdispend;
            this.special = special;
        }
        /*Bit16u*/int	mode;
        int	type;
        /*Bitu*/int	swidth, sheight;
        /*Bitu*/int	twidth, theight;
        /*Bitu*/int	cwidth, cheight;
        /*Bitu*/int	ptotal,pstart,plength;

        /*Bitu*/int	htotal,vtotal;
        /*Bitu*/int	hdispend,vdispend;
        /*Bitu*/int	special;
    }

    public static class Int10Data {
        public static class Rom {
            /*RealPt*/int font_8_first;
            /*RealPt*/int font_8_second;
            /*RealPt*/int font_14;
            /*RealPt*/int font_16;
            /*RealPt*/int font_14_alternate;
            /*RealPt*/int font_16_alternate;
            /*RealPt*/int static_state;
            /*RealPt*/int video_save_pointers;
            /*RealPt*/int video_parameter_table;
            /*RealPt*/int video_save_pointer_table;
            /*RealPt*/int video_dcc_table;
            /*RealPt*/int oemstring;
            /*RealPt*/int vesa_modes;
            /*RealPt*/int pmode_interface;
            /*Bit16u*/int pmode_interface_size;
            /*Bit16u*/int pmode_interface_start;
            /*Bit16u*/int pmode_interface_window;
            /*Bit16u*/int pmode_interface_palette;
            /*Bit16u*/int used;
        }
        public Rom rom = new Rom();
        public /*Bit16u*/int vesa_setmode;
        public boolean vesa_nolfb;
        public boolean vesa_oldvbe;
    }

    public static Int10Data int10;

    public static /*Bit8u*/short CURSOR_POS_COL(/*Bit8u*/short page) {
        return (short)Memory.real_readb(BIOSMEM_SEG,BIOSMEM_CURSOR_POS+page*2);
    }

    public static /*Bit8u*/short CURSOR_POS_ROW(/*Bit8u*/short page) {
        return (short)Memory.real_readb(BIOSMEM_SEG,BIOSMEM_CURSOR_POS+page*2+1);
    }

    private static /*Bitu*/int call_10;
    static boolean warned_ff=false;

    static void graphics_chars() {
        switch (CPU_Regs.reg_ebx.low()) {
        case 0x00:Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_NB_ROWS,(short)(CPU_Regs.reg_edx.low()-1));break;
        case 0x01:Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_NB_ROWS,(short)13);break;
        case 0x03:Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_NB_ROWS,(short)42);break;
        case 0x02:
        default:Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_NB_ROWS,(short)24);break;
        }
    }

    static private Callback.Handler INT10_Handler = new Callback.Handler() {
        public String getName() {
            return "Int10.INT10_Handler 0x"+(CPU_Regs.reg_eax.high() & 0xFF);
        }
    public /*Bitu*/int call() {
        if (false) {
            switch ((short)(CPU_Regs.reg_eax.high() & 0xFF)) {
            case 0x02:
            case 0x03:
            case 0x09:
            case 0xc:
            case 0xd:
            case 0x0e:
            case 0x10:
            case 0x4f:

                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_NORMAL,"Function AX:"+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" , BX "+Integer.toString(CPU_Regs.reg_ebx.word(), 16)+" DX "+Integer.toString(CPU_Regs.reg_edx.word(), 16));
                break;
            }
        }
        short c = (short)(CPU_Regs.reg_eax.high() & 0xFF);
        //Debug.start(Debug.TYPE_INT10, c);
        switch (c) {
        case 0x00:								/* Set VideoMode */
            Int10_modes.INT10_SetVideoMode(CPU_Regs.reg_eax.low());
            break;
        case 0x01:								/* Set TextMode Cursor Shape */
            Int10_char.INT10_SetCursorShape((short)CPU_Regs.reg_ecx.high(),(short)CPU_Regs.reg_ecx.low());
            break;
        case 0x02:								/* Set Cursor Pos */
            Int10_char.INT10_SetCursorPos((short)CPU_Regs.reg_edx.high(),(short)CPU_Regs.reg_edx.low(),(short)CPU_Regs.reg_ebx.high());
            break;
        case 0x03:								/* get Cursor Pos and Cursor Shape*/
    //		reg_ah=0;
            CPU_Regs.reg_edx.low((CURSOR_POS_COL((short)CPU_Regs.reg_ebx.high()) & 0xFF));
            CPU_Regs.reg_edx.high((CURSOR_POS_ROW((short)CPU_Regs.reg_ebx.high()) & 0xFF));
            CPU_Regs.reg_ecx.word(Memory.real_readw(BIOSMEM_SEG,BIOSMEM_CURSOR_TYPE));
            break;
        case 0x04:								/* read light pen pos YEAH RIGHT */
            /* Light pen is not supported */
            CPU_Regs.reg_eax.word(0);
            break;
        case 0x05:								/* Set Active Page */
            if ((CPU_Regs.reg_eax.low() & 0x80)!=0 && Dosbox.IS_TANDY_ARCH()) {
                /*Bit8u*/int crtcpu=Memory.real_readb(BIOSMEM_SEG, BIOSMEM_CRTCPU_PAGE);
                switch ((int)CPU_Regs.reg_eax.low()) {
                case 0x80:
                    CPU_Regs.reg_ebx.high((crtcpu & 7));
                    CPU_Regs.reg_ebx.low(((crtcpu >> 3) & 0x7));
                    break;
                case 0x81:
                    crtcpu=(crtcpu & 0xc7) | ((CPU_Regs.reg_ebx.low() & 7) << 3);
                    break;
                case 0x82:
                    crtcpu=(crtcpu & 0xf8) | (CPU_Regs.reg_ebx.high() & 7);
                    break;
                case 0x83:
                    crtcpu=(crtcpu & 0xc0) | (CPU_Regs.reg_ebx.high() & 7) | ((CPU_Regs.reg_ebx.low() & 7) << 3);
                    break;
                }
                if (Dosbox.machine== MachineType.MCH_PCJR) {
                    /* always return graphics mapping, even for invalid values of AL */
                    CPU_Regs.reg_ebx.high((crtcpu & 7));
                    CPU_Regs.reg_ebx.low(((crtcpu >> 3) & 0x7));
                }
                IO.IO_WriteB(0x3df,crtcpu);
                Memory.real_writeb(BIOSMEM_SEG, BIOSMEM_CRTCPU_PAGE,crtcpu);
            }
            else Int10_char.INT10_SetActivePage((short)CPU_Regs.reg_eax.low());
            break;
        case 0x06:								/* Scroll Up */
            Int10_char.INT10_ScrollWindow((short)CPU_Regs.reg_ecx.high(),(short)CPU_Regs.reg_ecx.low(),(short)CPU_Regs.reg_edx.high(),(short)CPU_Regs.reg_edx.low(),(byte)-CPU_Regs.reg_eax.low(),(short)CPU_Regs.reg_ebx.high(),(short)0xFF);
            break;
        case 0x07:								/* Scroll Down */
            Int10_char.INT10_ScrollWindow((short)CPU_Regs.reg_ecx.high(),(short)CPU_Regs.reg_ecx.low(),(short)CPU_Regs.reg_edx.high(),(short)CPU_Regs.reg_edx.low(),(byte)CPU_Regs.reg_eax.low(),(short)CPU_Regs.reg_ebx.high(),(short)0xFF);
            break;
        case 0x08:								/* Read character & attribute at cursor */
            CPU_Regs.reg_eax.word(Int10_char.INT10_ReadCharAttr((short)CPU_Regs.reg_ebx.high()));
            break;
        case 0x09:								/* Write Character & Attribute at cursor CX times */
            if (Memory.real_readb(BIOSMEM_SEG,BIOSMEM_CURRENT_MODE)==0x11) {
                Int10_char.INT10_WriteChar((short)CPU_Regs.reg_eax.low(),(short)((CPU_Regs.reg_ebx.low() & 0x80)|0x3f),(short)CPU_Regs.reg_ebx.high(),CPU_Regs.reg_ecx.word(),true);
            } else {
                Int10_char.INT10_WriteChar((short)CPU_Regs.reg_eax.low(),(short)CPU_Regs.reg_ebx.low(),(short)CPU_Regs.reg_ebx.high(),CPU_Regs.reg_ecx.word(),true);
            }
            break;
        case 0x0A:								/* Write Character at cursor CX times */
            Int10_char.INT10_WriteChar((short)CPU_Regs.reg_eax.low(),(short)CPU_Regs.reg_ebx.low(),(short)CPU_Regs.reg_ebx.high(),CPU_Regs.reg_ecx.word(),false);
            break;
        case 0x0B:								/* Set Background/Border Colour & Set Palette*/
            switch (CPU_Regs.reg_ebx.high()) {
            case 0x00:		//Background/Border color
                Int10_pal.INT10_SetBackgroundBorder((short)CPU_Regs.reg_ebx.low());
                break;
            case 0x01:		//Set color Select
            default:
                Int10_pal.INT10_SetColorSelect((short)CPU_Regs.reg_ebx.low());
                break;
            }
            break;
        case 0x0C:								/* Write Graphics Pixel */
            Int10_put_pixel.INT10_PutPixel(CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word(),(short)CPU_Regs.reg_ebx.high(),(short)CPU_Regs.reg_eax.low());
            break;
        case 0x0D:								/* Read Graphics Pixel */
            CPU_Regs.reg_eax.low((Int10_put_pixel.INT10_GetPixel(CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word(),(short)CPU_Regs.reg_ebx.high()) & 0xFF));
            break;
        case 0x0E:								/* Teletype OutPut */
            Int10_char.INT10_TeletypeOutput(CPU_Regs.reg_eax.low(),CPU_Regs.reg_ebx.low());
            break;
        case 0x0F:								/* Get videomode */
            CPU_Regs.reg_ebx.high((Memory.real_readb(BIOSMEM_SEG,BIOSMEM_CURRENT_PAGE) & 0xFF));
            CPU_Regs.reg_eax.low(((Memory.real_readb(BIOSMEM_SEG,BIOSMEM_CURRENT_MODE)|(Memory.real_readb(BIOSMEM_SEG,BIOSMEM_VIDEO_CTL)&0x80)) & 0xFF));
            CPU_Regs.reg_eax.high((Memory.real_readw(BIOSMEM_SEG,BIOSMEM_NB_COLS) & 0xFF));
            break;
        case 0x10:								/* Palette functions */
            if (!Dosbox.IS_EGAVGA_ARCH() && (CPU_Regs.reg_eax.low()>0x02)) break;
		    else if (!Dosbox.IS_VGA_ARCH() && (CPU_Regs.reg_eax.low()>0x03)) break;
            switch (CPU_Regs.reg_eax.low()) {
            case 0x00:							/* SET SINGLE PALETTE REGISTER */
                Int10_pal.INT10_SetSinglePaletteRegister((short)CPU_Regs.reg_ebx.low(),(short)CPU_Regs.reg_ebx.high());
                break;
            case 0x01:							/* SET BORDER (OVERSCAN) COLOR*/
                Int10_pal.INT10_SetOverscanBorderColor((short)CPU_Regs.reg_ebx.high());
                break;
            case 0x02:							/* SET ALL PALETTE REGISTERS */
                Int10_pal.INT10_SetAllPaletteRegisters(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word());
                break;
            case 0x03:							/* TOGGLE INTENSITY/BLINKING BIT */
                Int10_pal.INT10_ToggleBlinkingBit((short)CPU_Regs.reg_ebx.low());
                break;
            case 0x07:							/* GET SINGLE PALETTE REGISTER */
                CPU_Regs.reg_ebx.high(((Int10_pal.INT10_GetSinglePaletteRegister((short)CPU_Regs.reg_ebx.high(), (short)CPU_Regs.reg_ebx.low()) & 0xFF)));
                break;
            case 0x08:							/* READ OVERSCAN (BORDER COLOR) REGISTER */
                CPU_Regs.reg_ebx.high((Int10_pal.INT10_GetOverscanBorderColor() & 0xFF)) ;
                break;
            case 0x09:							/* READ ALL PALETTE REGISTERS AND OVERSCAN REGISTER */
                Int10_pal.INT10_GetAllPaletteRegisters(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word());
                break;
            case 0x10:							/* SET INDIVIDUAL DAC REGISTER */
                Int10_pal.INT10_SetSingleDACRegister((short)CPU_Regs.reg_ebx.low(), (short)CPU_Regs.reg_edx.high(), (short)CPU_Regs.reg_ecx.high(), (short)CPU_Regs.reg_ecx.low());
                break;
            case 0x12:							/* SET BLOCK OF DAC REGISTERS */
                Int10_pal.INT10_SetDACBlock(CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word(),CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word());
                break;
            case 0x13:							/* SELECT VIDEO DAC COLOR PAGE */
                Int10_pal.INT10_SelectDACPage((short)CPU_Regs.reg_ebx.low(),(short)CPU_Regs.reg_ebx.high());
                break;
            case 0x15:							/* GET INDIVIDUAL DAC REGISTER */
            {
                ShortRef dh = new ShortRef();
                ShortRef ch = new ShortRef();
                ShortRef cl = new ShortRef();
                Int10_pal.INT10_GetSingleDACRegister((short)CPU_Regs.reg_ebx.low(), dh, ch, cl);
                CPU_Regs.reg_edx.high((dh.value & 0xFF));
                CPU_Regs.reg_ecx.high((ch.value & 0xFF));
                CPU_Regs.reg_ecx.low((cl.value & 0xFF));
            }
                break;
            case 0x17:							/* GET BLOCK OF DAC REGISTER */
                Int10_pal.INT10_GetDACBlock(CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word(),CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word());
                break;
            case 0x18:							/* undocumented - SET PEL MASK */
                Int10_pal.INT10_SetPelMask((short)CPU_Regs.reg_ebx.low());
                break;
            case 0x19:							/* undocumented - GET PEL MASK */
                CPU_Regs.reg_ebx.low((Int10_pal.INT10_GetPelMask() & 0xFF));
                CPU_Regs.reg_ebx.high(0);	// bx for get mask
                break;
            case 0x1A:							/* GET VIDEO DAC COLOR PAGE */
            {
                ShortRef bl = new ShortRef();
                ShortRef bh = new ShortRef();
                Int10_pal.INT10_GetDACPage(bl,bh);
                CPU_Regs.reg_ebx.low((bl.value & 0xFF));
                CPU_Regs.reg_ebx.high((bh.value & 0xFF));
                break;
            }
            case 0x1B:							/* PERFORM GRAY-SCALE SUMMING */
                Int10_pal.INT10_PerformGrayScaleSumming(CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word());
                break;
            //case 0xF0:							/* ET4000: SET HiColor GRAPHICS MODE */
            //case 0xF1:							/* ET4000: GET DAC TYPE */
            //case 0xF2:							/* ET4000: CHECK/SET HiColor MODE */
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function 10:Unhandled EGA/VGA Palette Function "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                break;
            }
            break;
        case 0x11:								/* Character generator functions */
            if (!Dosbox.IS_EGAVGA_ARCH())
                break;
            switch (CPU_Regs.reg_eax.low()) {
    /* Textmode calls */
            case 0x00:			/* Load user font */
            case 0x10:
                Int10_memory.INT10_LoadFont(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebp.word(),CPU_Regs.reg_eax.low()==0x10,CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word(),CPU_Regs.reg_ebx.low(),CPU_Regs.reg_ebx.high());
                break;
            case 0x01:			/* Load 8x14 font */
            case 0x11:
                Int10_memory.INT10_LoadFont(Memory.Real2Phys(int10.rom.font_14),CPU_Regs.reg_eax.low()==0x11,256,0,CPU_Regs.reg_ebx.low(),14);
                break;
            case 0x02:			/* Load 8x8 font */
            case 0x12:
                Int10_memory.INT10_LoadFont(Memory.Real2Phys(int10.rom.font_8_first),CPU_Regs.reg_eax.low()==0x12,256,0,CPU_Regs.reg_ebx.low(),8);
                break;
            case 0x03:			/* Set Block Specifier */
                IoHandler.IO_Write(0x3c4,0x3);IoHandler.IO_Write(0x3c5,CPU_Regs.reg_ebx.low());
                break;
            case 0x04:			/* Load 8x16 font */
            case 0x14:
                if (!Dosbox.IS_VGA_ARCH()) break;
                Int10_memory.INT10_LoadFont(Memory.Real2Phys(int10.rom.font_16),CPU_Regs.reg_eax.low()==0x14,256,0,CPU_Regs.reg_ebx.low(),16);
                break;
    /* Graphics mode calls */
            case 0x20:			/* Set User 8x8 Graphics characters */
                Memory.RealSetVec(0x1f,Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebp.word()));
                break;
            case 0x21:			/* Set user graphics characters */
                Memory.RealSetVec(0x43,Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebp.word()));
                Memory.real_writew(BIOSMEM_SEG,BIOSMEM_CHAR_HEIGHT,CPU_Regs.reg_ecx.word());
                graphics_chars();
                break;
            case 0x22:			/* Rom 8x14 set */
                Memory.RealSetVec(0x43,int10.rom.font_14);
                Memory.real_writew(BIOSMEM_SEG,BIOSMEM_CHAR_HEIGHT,14);
                graphics_chars();
                break;
            case 0x23:			/* Rom 8x8 double dot set */
                Memory.RealSetVec(0x43,int10.rom.font_8_first);
                Memory.real_writew(BIOSMEM_SEG,BIOSMEM_CHAR_HEIGHT,8);
                graphics_chars();
                break;
            case 0x24:			/* Rom 8x16 set */
                if (!Dosbox.IS_VGA_ARCH()) break;
                Memory.RealSetVec(0x43,int10.rom.font_16);
                Memory.real_writew(BIOSMEM_SEG,BIOSMEM_CHAR_HEIGHT,16);
                graphics_chars();
                break;
    /* General */
            case 0x30:/* Get Font Information */
                switch (CPU_Regs.reg_ebx.high()) {
                case 0x00:	/* interupt 0x1f vector */
                    {
                        /*RealPt*/int int_1f=Memory.RealGetVec(0x1f);
                        CPU_Regs.SegSet16ES(Memory.RealSeg(int_1f));
                        CPU_Regs.reg_ebp.word(Memory.RealOff(int_1f));
                    }
                    break;
                case 0x01:	/* interupt 0x43 vector */
                    {
                        /*RealPt*/int int_43=Memory.RealGetVec(0x43);
                        CPU_Regs.SegSet16ES(Memory.RealSeg(int_43));
                        CPU_Regs.reg_ebp.word(Memory.RealOff(int_43));
                    }
                    break;
                case 0x02:	/* font 8x14 */
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.font_14));
                    CPU_Regs.reg_ebp.word(Memory.RealOff(int10.rom.font_14));
                    break;
                case 0x03:	/* font 8x8 first 128 */
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.font_8_first));
                    CPU_Regs.reg_ebp.word(Memory.RealOff(int10.rom.font_8_first));
                    break;
                case 0x04:	/* font 8x8 second 128 */
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.font_8_second));
                    CPU_Regs.reg_ebp.word(Memory.RealOff(int10.rom.font_8_second));
                    break;
                case 0x05:	/* alpha alternate 9x14 */
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.font_14_alternate));
                    CPU_Regs.reg_ebp.word(Memory.RealOff(int10.rom.font_14_alternate));
                    break;
                case 0x06:	/* font 8x16 */
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.font_16));
                    CPU_Regs.reg_ebp.word(Memory.RealOff(int10.rom.font_16));
                    break;
                case 0x07:	/* alpha alternate 9x16 */
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.font_16_alternate));
                    CPU_Regs.reg_ebp.word(Memory.RealOff(int10.rom.font_16_alternate));
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function 11:30 Request for font "+Integer.toString(CPU_Regs.reg_ebx.high(),16));
                    break;
                }
                if ((CPU_Regs.reg_ebx.high()<=7) || (Dosbox.svgaCard== SVGACards.SVGA_TsengET4K)) {
                    CPU_Regs.reg_ecx.word(Memory.real_readw(BIOSMEM_SEG,BIOSMEM_CHAR_HEIGHT));
                    CPU_Regs.reg_edx.low(Memory.real_readb(BIOSMEM_SEG,BIOSMEM_NB_ROWS));
                }
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function 11:Unsupported character generator call "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                break;
            }
            break;
        case 0x12:								/* alternate function select */
            if (!Dosbox.IS_EGAVGA_ARCH())
                break;
            switch (CPU_Regs.reg_ebx.low()) {
            case 0x10:							/* Get EGA Information */
                CPU_Regs.reg_ebx.high((Memory.real_readw(BIOSMEM_SEG,BIOSMEM_CRTC_ADDRESS)==0x3B4)?(byte)1:(byte)0);
                CPU_Regs.reg_ebx.low(3);	//256 kb
                CPU_Regs.reg_ecx.low((Memory.real_readb(BIOSMEM_SEG,BIOSMEM_SWITCHES) & 0x0F));
                CPU_Regs.reg_ecx.high((Memory.real_readb(BIOSMEM_SEG,BIOSMEM_SWITCHES) >> 4));
                break;
            case 0x20:							/* Set alternate printscreen */
                break;
            case 0x30:							/* Select vertical resolution */
                {
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_WARN,"Function 12:Call "+Integer.toString(CPU_Regs.reg_ebx.low(), 16)+" (select vertical resolution)");
                    if (Dosbox.svgaCard != SVGACards.SVGA_None) {
                        if (CPU_Regs.reg_eax.low() > 2) {
                            CPU_Regs.reg_eax.low(0);		// invalid subfunction
                            break;
                        }
                    }
                    /*Bit8u*/int modeset_ctl = Memory.real_readb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL);
                    /*Bit8u*/int video_switches = Memory.real_readb(BIOSMEM_SEG,BIOSMEM_SWITCHES) & 0xf0;
                    switch(CPU_Regs.reg_eax.low()) {
                    case 0: // 200
                        modeset_ctl &= 0xef;
                        modeset_ctl |= 0x80;
                        video_switches |= 8;	// ega normal/cga emulation
                        break;
                    case 1: // 350
                        modeset_ctl &= 0x6f;
                        video_switches |= 9;	// ega enhanced
                        break;
                    case 2: // 400
                        modeset_ctl &= 0x6f;
                        modeset_ctl |= 0x10;	// use 400-line mode at next mode set
                        video_switches |= 9;	// ega enhanced
                        break;
                    default:
                        modeset_ctl &= 0xef;
                        video_switches |= 8;	// ega normal/cga emulation
                        break;
                    }
                    Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL,modeset_ctl);
                    Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_SWITCHES,video_switches);
                    CPU_Regs.reg_eax.low(0x12);	// success
                    break;
                }
            case 0x31:							/* Palette loading on modeset */
                {
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    if (Dosbox.svgaCard==SVGACards.SVGA_TsengET4K) CPU_Regs.reg_eax.low((CPU_Regs.reg_eax.low()&1));
                    if (CPU_Regs.reg_eax.low()>1) {
                        CPU_Regs.reg_eax.low(0);		//invalid subfunction
                        break;
                    }
                    /*Bit8u*/short temp = (short)(Memory.real_readb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL) & 0xf7);
                    if ((CPU_Regs.reg_eax.low() & 1)!=0) temp|=8;		// enable if al=0
                    Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL,temp);
                    CPU_Regs.reg_eax.low(0x12);
                    break;
                }
            case 0x32:							/* Video addressing */
                if (!Dosbox.IS_VGA_ARCH()) break;
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function 12:Call "+Integer.toString(CPU_Regs.reg_ebx.low(), 16)+" not handled");
                if (Dosbox.svgaCard==SVGACards.SVGA_TsengET4K) CPU_Regs.reg_eax.low((CPU_Regs.reg_eax.low() & 1));
                if (CPU_Regs.reg_eax.low()>1) CPU_Regs.reg_eax.low(0);		//invalid subfunction
                else CPU_Regs.reg_eax.low(0x12);			//fake a success call
                break;
            case 0x33: /* SWITCH GRAY-SCALE SUMMING */
                {
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    if (Dosbox.svgaCard==SVGACards.SVGA_TsengET4K) CPU_Regs.reg_eax.low((CPU_Regs.reg_eax.low() & 1));
                    if (CPU_Regs.reg_eax.low()>1) {
                        CPU_Regs.reg_eax.low(0);
                        break;
                    }
                    /*Bit8u*/short temp = (short)(Memory.real_readb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL) & 0xfd);
                    if ((CPU_Regs.reg_eax.low()&1)==0) temp|=2;		// enable if al=0
                    Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL,temp);
                    CPU_Regs.reg_eax.low(0x12);
                    break;
                }
            case 0x34: /* ALTERNATE FUNCTION SELECT (VGA) - CURSOR EMULATION */
                {
                    // bit 0: 0=enable, 1=disable
                    if (!Dosbox.IS_VGA_ARCH()) break;
                    if (Dosbox.svgaCard==SVGACards.SVGA_TsengET4K) CPU_Regs.reg_eax.low((CPU_Regs.reg_eax.low() & 1));
                    if (CPU_Regs.reg_eax.low()>1) {
                        CPU_Regs.reg_eax.low(0);
                        break;
                    }
                    /*Bit8u*/short temp = (short)(Memory.real_readb(BIOSMEM_SEG,BIOSMEM_VIDEO_CTL) & 0xfe);
                    Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_VIDEO_CTL,temp|CPU_Regs.reg_eax.low());
                    CPU_Regs.reg_eax.low(0x12);
                    break;
                }
            case 0x35:
                if (!Dosbox.IS_VGA_ARCH()) break;
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function 12:Call "+Integer.toString(CPU_Regs.reg_ebx.low(), 16)+" not handled");
                CPU_Regs.reg_eax.low(0x12);
                break;
            case 0x36: {						/* VGA Refresh control */
                if (!Dosbox.IS_VGA_ARCH()) break;
                if ((Dosbox.svgaCard==SVGACards.SVGA_S3Trio) && (CPU_Regs.reg_eax.low()>1)) {
                    CPU_Regs.reg_eax.low(0);
                    break;
                }
                IoHandler.IO_Write(0x3c4,0x1);
                /*Bit8u*/short clocking = IoHandler.IO_Read(0x3c5);

                if (CPU_Regs.reg_eax.low()==0) clocking &= ~0x20;
                else clocking |= 0x20;

                IoHandler.IO_Write(0x3c4,0x1);
                IoHandler.IO_Write(0x3c5,clocking);

                CPU_Regs.reg_eax.low(0x12); // success
                break;
            }
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function 12:Call "+Integer.toString(CPU_Regs.reg_ebx.low(), 16)+" not handled");
                if (Dosbox.machine!=MachineType.MCH_EGA) CPU_Regs.reg_eax.low(0);
                break;
            }
            break;
        case 0x13:								/* Write String */
            Int10_char.INT10_WriteString((short)CPU_Regs.reg_edx.high(),(short)CPU_Regs.reg_edx.low(),(short)CPU_Regs.reg_eax.low(),(short)CPU_Regs.reg_ebx.low(),CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebp.word(),CPU_Regs.reg_ecx.word(),(short)CPU_Regs.reg_ebx.high());
            break;
        case 0x1A:								/* Display Combination */
            if (!Dosbox.IS_VGA_ARCH()) break;
            if (CPU_Regs.reg_eax.low()==0) {	// get dcc
                // walk the tables...
                /*RealPt*/int vsavept=Memory.real_readd(BIOSMEM_SEG,BIOSMEM_VS_POINTER);
                /*RealPt*/int svstable=Memory.real_readd(Memory.RealSeg(vsavept),Memory.RealOff(vsavept)+0x10);
                if (svstable!=0) {
                    /*RealPt*/int dcctable=Memory.real_readd(Memory.RealSeg(svstable),Memory.RealOff(svstable)+0x02);
                    /*Bit8u*/int entries=Memory.real_readb(Memory.RealSeg(dcctable),Memory.RealOff(dcctable)+0x00);
                    /*Bit8u*/int idx=Memory.real_readb(BIOSMEM_SEG,BIOSMEM_DCC_INDEX);
                    // check if index within range
                    if (idx<entries) {
                        /*Bit16u*/int dccentry=Memory.real_readw(Memory.RealSeg(dcctable),Memory.RealOff(dcctable)+0x04+idx*2);
                        if ((dccentry&0xff)==0) CPU_Regs.reg_ebx.word((dccentry>>8));
                        else CPU_Regs.reg_ebx.word(dccentry);
                    } else CPU_Regs.reg_ebx.word((0xffff));
                } else CPU_Regs.reg_ebx.word(0xffff);
                CPU_Regs.reg_eax.word(0x1A);	// high part destroyed or zeroed depending on BIOS
            } else if (CPU_Regs.reg_eax.low()==1) {	// set dcc
                /*Bit8u*/short newidx=0xff;
                // walk the tables...
                /*RealPt*/int vsavept=Memory.real_readd(BIOSMEM_SEG,BIOSMEM_VS_POINTER);
                /*RealPt*/int svstable=Memory.real_readd(Memory.RealSeg(vsavept),Memory.RealOff(vsavept)+0x10);
                if (svstable!=0) {
                    /*RealPt*/int dcctable=Memory.real_readd(Memory.RealSeg(svstable),Memory.RealOff(svstable)+0x02);
                    /*Bit8u*/int entries=Memory.real_readb(Memory.RealSeg(dcctable),Memory.RealOff(dcctable)+0x00);
                    if (entries!=0) {
                        /*Bitu*/int ct;
                        /*Bit16u*/int swpidx=CPU_Regs.reg_ebx.high()|(CPU_Regs.reg_ebx.low()<<8);
                        // search the ddc index in the dcc table
                        for (ct=0; ct<entries; ct++) {
                            /*Bit16u*/int dccentry=Memory.real_readw(Memory.RealSeg(dcctable),Memory.RealOff(dcctable)+0x04+ct*2);
                            if ((dccentry==CPU_Regs.reg_ebx.word()) || (dccentry==swpidx)) {
                                newidx=(short)ct;
                                break;
                            }
                        }
                    }
                }

                Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_DCC_INDEX,newidx);
                CPU_Regs.reg_eax.word(0x1A);	// high part destroyed or zeroed depending on BIOS
            }
            break;
        case 0x1B:								/* functionality State Information */
            if (!Dosbox.IS_VGA_ARCH()) break;
            switch (CPU_Regs.reg_ebx.word()) {
            case 0x0000:
                Int10_misc.INT10_GetFuncStateInformation(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word());
                CPU_Regs.reg_eax.low(0x1B);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"1B:Unhandled call BX "+Integer.toString(CPU_Regs.reg_ebx.word(),16));
                CPU_Regs.reg_eax.low(0);
                break;
            }
            break;
        case 0x1C:	/* Video Save Area */
            if (!Dosbox.IS_VGA_ARCH()) break;
            switch (CPU_Regs.reg_eax.low()) {
                case 0: {
                    /*Bitu*/int ret=Int10_video_state.INT10_VideoState_GetSize(CPU_Regs.reg_ecx.word());
                    if (ret!=0) {
                        CPU_Regs.reg_eax.low(0x1c);
                        CPU_Regs.reg_ebx.word(ret);
                    } else CPU_Regs.reg_eax.low(0);
                    }
                    break;
                case 1:
                    if (Int10_video_state.INT10_VideoState_Save(CPU_Regs.reg_ecx.word(),Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word()))) CPU_Regs.reg_eax.low(0x1c);
                    else CPU_Regs.reg_eax.low(0);
                    break;
                case 2:
                    if (Int10_video_state.INT10_VideoState_Restore(CPU_Regs.reg_ecx.word(),Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word()))) CPU_Regs.reg_eax.low(0x1c);
                    else CPU_Regs.reg_eax.low(0);
                    break;
                default:
                    if (Dosbox.svgaCard==SVGACards.SVGA_TsengET4K) CPU_Regs.reg_eax.word(0);
                    else CPU_Regs.reg_eax.low(0);
                    break;
            }
            break;
        case 0x4f:								/* VESA Calls */
            if ((!Dosbox.IS_VGA_ARCH()) || (Dosbox.svgaCard!=SVGACards.SVGA_S3Trio)) break;
            switch ((short)(CPU_Regs.reg_eax.low() & 0xFF)) {
            case 0x00:							/* Get SVGA Information */
                CPU_Regs.reg_eax.low(0x4f);
                CPU_Regs.reg_eax.high(Int10_vesa.VESA_GetSVGAInformation((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()));
                break;
            case 0x01:							/* Get SVGA Mode Information */
                CPU_Regs.reg_eax.low(0x4f);
                CPU_Regs.reg_eax.high(Int10_vesa.VESA_GetSVGAModeInformation(CPU_Regs.reg_ecx.word(),(int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_edi.word()));
                break;
            case 0x02:							/* Set videomode */
                CPU_Regs.reg_eax.low(0x4f);
                CPU_Regs.reg_eax.high(Int10_vesa.VESA_SetSVGAMode(CPU_Regs.reg_ebx.word()));
                break;
            case 0x03:							/* Get videomode */
                CPU_Regs.reg_eax.low(0x4f);
                CPU_Regs.reg_ebx.word(Int10_vesa.VESA_GetSVGAMode());
                CPU_Regs.reg_eax.high(Int10_vesa.VESA_SUCCESS);
                break;
            case 0x04:							/* Save/restore state */
                CPU_Regs.reg_eax.low(0x4f);
                switch (CPU_Regs.reg_edx.low()) {
                    case 0: {
                        /*Bitu*/int ret=Int10_video_state.INT10_VideoState_GetSize(CPU_Regs.reg_ecx.word());
                        if (ret!= 0) {
                            CPU_Regs.reg_eax.high(0);
                            CPU_Regs.reg_ebx.word(ret);
                        } else CPU_Regs.reg_eax.high(1);
                    }
                    break;
                    case 1:
                        if (Int10_video_state.INT10_VideoState_Save(CPU_Regs.reg_ecx.word(),Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word()))) CPU_Regs.reg_eax.high(0);
                        else CPU_Regs.reg_eax.high(1);
                        break;
                    case 2:
                        if (Int10_video_state.INT10_VideoState_Restore(CPU_Regs.reg_ecx.word(),Memory.RealMake((int)CPU_Regs.reg_esVal.dword,CPU_Regs.reg_ebx.word()))) CPU_Regs.reg_eax.high(0);
                        else CPU_Regs.reg_eax.high(1);
                        break;
                    default:
                        CPU_Regs.reg_eax.high(1);
                        break;
                }
                break;
            case 0x05:
                if (CPU_Regs.reg_ebx.high()==0) {				/* Set CPU Window */
                    CPU_Regs.reg_eax.high(Int10_vesa.VESA_SetCPUWindow((short)CPU_Regs.reg_ebx.low(),(short)CPU_Regs.reg_edx.low()));
                    CPU_Regs.reg_eax.low(0x4f);
                } else if (CPU_Regs.reg_ebx.high() == 1) {		/* Get CPU Window */
                    CPU_Regs.reg_eax.high(Int10_vesa.VESA_GetCPUWindow((short)CPU_Regs.reg_ebx.low(), CPU_Regs.reg_edx));
                    CPU_Regs.reg_eax.low(0x4f);
                } else {
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Unhandled VESA Function "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_ebx.high(),16));
                    CPU_Regs.reg_eax.high(0x01);
                }
                break;
            case 0x06:
            {
                IntRef bx = new IntRef(CPU_Regs.reg_ebx.word());
                IntRef cx = new IntRef(CPU_Regs.reg_ecx.word());
                IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                CPU_Regs.reg_eax.low(0x4f);
                CPU_Regs.reg_eax.high(Int10_vesa.VESA_ScanLineLength((short)CPU_Regs.reg_ebx.low(),CPU_Regs.reg_ecx.word(),bx,cx,dx));
                CPU_Regs.reg_ebx.word(bx.value);
                CPU_Regs.reg_ecx.word(cx.value);
                CPU_Regs.reg_edx.word(dx.value);
            }
                break;
            case 0x07:
                switch ((short)(CPU_Regs.reg_ebx.low() & 0xFF)) {
                case 0x80:						/* Set Display Start during retrace ?? */
                case 0x00:						/* Set display Start */
                    CPU_Regs.reg_eax.low(0x4f);
                    CPU_Regs.reg_eax.high(Int10_vesa.VESA_SetDisplayStart(CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word()));
                    break;
                case 0x01:
                {
                    IntRef cx = new IntRef(CPU_Regs.reg_ecx.word());
                    IntRef dx = new IntRef(CPU_Regs.reg_edx.word());
                    CPU_Regs.reg_eax.low(0x4f);
                    CPU_Regs.reg_ebx.high(0x00);				//reserved
                    CPU_Regs.reg_eax.high(Int10_vesa.VESA_GetDisplayStart(cx,dx));
                    CPU_Regs.reg_ecx.word(cx.value);
                    CPU_Regs.reg_edx.word(dx.value);
                }
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Unhandled VESA Function "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_ebx.low(),16));
                    CPU_Regs.reg_eax.high(0x1);
                    break;
                }
                break;
            case 0x09:
                switch ((short)(CPU_Regs.reg_ebx.low() & 0xFF)) {
                case 0x80:						/* Set Palette during retrace */
                    //TODO
                case 0x00:						/* Set Palette */
                    CPU_Regs.reg_eax.high(Int10_vesa.VESA_SetPalette(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),CPU_Regs.reg_edx.word(),CPU_Regs.reg_ecx.word()));
                    CPU_Regs.reg_eax.low(0x4f);
                    break;
                case 0x01:						/* Get Palette */
                    CPU_Regs.reg_eax.high(Int10_vesa.VESA_GetPalette(CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edi.word(),CPU_Regs.reg_edx.word(),CPU_Regs.reg_ecx.word()));
                    CPU_Regs.reg_eax.low(0x4f);
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Unhandled VESA Function "+Integer.toString(CPU_Regs.reg_eax.low(), 16)+" Subfunction "+Integer.toString(CPU_Regs.reg_ebx.low(),16));
                    CPU_Regs.reg_eax.high(0x01);
                    break;
                }
                break;
            case 0x0a:							/* Get Pmode Interface */
                if (int10.vesa_oldvbe) {
                    CPU_Regs.reg_eax.word(0x014f);
                    break;
                }
                switch (CPU_Regs.reg_ebx.low()) {
                case 0x00:
                    CPU_Regs.reg_edi.dword=Memory.RealOff(int10.rom.pmode_interface);
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.pmode_interface));
                    CPU_Regs.reg_ecx.word(int10.rom.pmode_interface_size);
                    CPU_Regs.reg_eax.word(0x004f);
                    break;
                case 0x01:						/* Get code for "set window" */
                    CPU_Regs.reg_edi.dword=Memory.RealOff(int10.rom.pmode_interface)+int10.rom.pmode_interface_window;
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.pmode_interface));
                    CPU_Regs.reg_ecx.word(0x10);		//0x10 should be enough for the callbacks
                    CPU_Regs.reg_eax.word(0x004f);
                    break;
                case 0x02:						/* Get code for "set display start" */
                    CPU_Regs.reg_edi.dword=Memory.RealOff(int10.rom.pmode_interface)+int10.rom.pmode_interface_start;
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.pmode_interface));
                    CPU_Regs.reg_ecx.word(0x10);		//0x10 should be enough for the callbacks
                    CPU_Regs.reg_eax.word(0x004f);
                    break;
                case 0x03:						/* Get code for "set palette" */
                    CPU_Regs.reg_edi.dword=Memory.RealOff(int10.rom.pmode_interface)+int10.rom.pmode_interface_palette;
                    CPU_Regs.SegSet16ES(Memory.RealSeg(int10.rom.pmode_interface));
                    CPU_Regs.reg_ecx.word(0x10);		//0x10 should be enough for the callbacks
                    CPU_Regs.reg_eax.word(0x004f);
                    break;
                default:
                    CPU_Regs.reg_eax.word(0x014f);
                    break;
                }
                break;

            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Unhandled VESA Function "+Integer.toString(CPU_Regs.reg_eax.low(),16));
                CPU_Regs.reg_eax.low(0x0);
                break;
            }
            break;
        case 0xf0:
            CPU_Regs.reg_ebx.low(Int10_misc.INT10_EGA_RIL_ReadRegister((short)CPU_Regs.reg_ebx.low(), CPU_Regs.reg_edx.word()));
            break;
        case 0xf1:
            CPU_Regs.reg_ebx.low(Int10_misc.INT10_EGA_RIL_WriteRegister((short)CPU_Regs.reg_ebx.low(), (short)CPU_Regs.reg_ebx.high(), CPU_Regs.reg_edx.word()));
            break;
        case 0xf2:
            Int10_misc.INT10_EGA_RIL_ReadRegisterRange((short)CPU_Regs.reg_ecx.high(), (short)CPU_Regs.reg_ecx.low(), CPU_Regs.reg_edx.word(), CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebx.word());
            break;
        case 0xf3:
            Int10_misc.INT10_EGA_RIL_WriteRegisterRange((short)CPU_Regs.reg_ecx.high(), (short)CPU_Regs.reg_ecx.low(), CPU_Regs.reg_edx.word(), CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebx.word());
            break;
        case 0xf4:
            Int10_misc.INT10_EGA_RIL_ReadRegisterSet(CPU_Regs.reg_ecx.word(), CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebx.word());
            break;
        case 0xf5:
            Int10_misc.INT10_EGA_RIL_WriteRegisterSet(CPU_Regs.reg_ecx.word(), CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_ebx.word());
            break;
        case 0xfa: {
            /*RealPt*/int pt=Int10_misc.INT10_EGA_RIL_GetVersionPt();
            CPU_Regs.SegSet16ES(Memory.RealSeg(pt));
            CPU_Regs.reg_ebx.word(Memory.RealOff(pt));
            }
            break;
        case 0xff:
            if (!warned_ff) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_NORMAL,"INT10:FF:Weird NC call");
            warned_ff=true;
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Function "+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" not supported");
    //		CPU_Regs.reg_eax.low()=0x00;		//Successfull, breaks marriage
            break;
        }
        //Debug.stop(Debug.TYPE_INT10, c);
        return Callback.CBRET_NONE;
    }
    };

    private static void INT10_Seg40Init() {
        // the default char height
        Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_CHAR_HEIGHT,(short)16);
        // Clear the screen
        Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_VIDEO_CTL,(short)0x60);
        // Set the basic screen we have
        Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_SWITCHES,(short)0xF9);
        // Set the basic modeset options
        Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_MODESET_CTL,(short)0x51);
        // Set the  default MSR
        Memory.real_writeb(BIOSMEM_SEG,BIOSMEM_CURRENT_MSR,(short)0x09);
    }


    private static void INT10_InitVGA() {
    /* switch to color mode and enable CPU access 480 lines */
        IoHandler.IO_Write(0x3c2,(byte)0xc3);
        /* More than 64k */
        IoHandler.IO_Write(0x3c4,(byte)0x04);
        IoHandler.IO_Write(0x3c5,(byte)0x02);
    }

    private static void SetupTandyBios() {
        final byte[] TandyConfig = {
            0x21, 0x42, 0x49, 0x4f, 0x53, 0x20, 0x52, 0x4f, 0x4d, 0x20, 0x76, 0x65, 0x72,
            0x73, 0x69, 0x6f, 0x6e, 0x20, 0x30, 0x32, 0x2e, 0x30, 0x30, 0x2e, 0x30, 0x30,
            0x0d, 0x0a, 0x43, 0x6f, 0x6d, 0x70, 0x61, 0x74, 0x69, 0x62, 0x69, 0x6c, 0x69,
            0x74, 0x79, 0x20, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x0d, 0x0a,
            0x43, 0x6f, 0x70, 0x79, 0x72, 0x69, 0x67, 0x68, 0x74, 0x20, 0x28, 0x43, 0x29,
            0x20, 0x31, 0x39, 0x38, 0x34, 0x2c, 0x31, 0x39, 0x38, 0x35, 0x2c, 0x31, 0x39,
            0x38, 0x36, 0x2c, 0x31, 0x39, 0x38, 0x37, 0x0d, 0x0a, 0x50, 0x68, 0x6f, 0x65,
            0x6e, 0x69, 0x78, 0x20, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x20,
            0x41, 0x73, 0x73, 0x6f, 0x63, 0x69, 0x61, 0x74, 0x65, 0x73, 0x20, 0x4c, 0x74,
            0x64, 0x2e, 0x0d, 0x0a, 0x61, 0x6e, 0x64, 0x20, 0x54, 0x61, 0x6e, 0x64, 0x79
        };
        if (Dosbox.machine==MachineType.MCH_TANDY) {
            /*Bitu*/int i;
            for(i=0;i<130;i++) {
                Memory.phys_writeb(0xf0000+i+0xc000, TandyConfig[i]);
            }
        }
    }

    public static Section.SectionFunction INT10_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            int10 = null;
        }
    };

    public static Section.SectionFunction INT10_Init = new Section.SectionFunction() {
        public void call(Section section) {
            System.out.println("INT10_Init");
            // int10 = new Int10Data(); Happend in Dosbox.DOSBOX_RealInit
            INT10_InitVGA();
            if (Dosbox.IS_TANDY_ARCH()) SetupTandyBios();
            /* Setup the INT 10 vector */
            call_10=Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_10,INT10_Handler,Callback.CB_IRET,"Int 10 video");
            Memory.RealSetVec(0x10,Callback.CALLBACK_RealPointer(call_10));
            //Init the 0x40 segment and init the datastructures in the the video rom area
            Int10_memory.INT10_SetupRomMemory();
            INT10_Seg40Init();
            Int10_vesa.INT10_SetupVESA();
            Int10_memory.INT10_SetupRomMemoryChecksum();//SetupVesa modifies the rom as well.
            Int10_modes.INT10_SetVideoMode(0x3);
            section.AddDestroyFunction(INT10_Destroy,false);
        }
    };
}
