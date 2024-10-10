package jdos.ints;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.hardware.VGA;
import jdos.hardware.VGA_draw;
import jdos.misc.setup.Config;
import jdos.util.IntRef;
import jdos.util.Ptr;

public class Int10_vesa {
    static public final int VESA_SUCCESS =          0x00;
    static private final int VESA_FAIL =             0x01;
    static private final int VESA_HW_UNSUPPORTED =   0x02;
    static private final int VESA_MODE_UNSUPPORTED = 0x03;
    // internal definition to pass to the caller
    static private final int VESA_UNIMPLEMENTED =    0xFF;

    static private class Callback {
        /*Bitu*/int setwindow;
        /*Bitu*/int pmStart;
        /*Bitu*/int pmWindow;
        /*Bitu*/int pmPalette;
    }
    static private Callback callback;

    static private final String string_oem="S3 Incorporated. Trio64\0";
    static private final String string_vendorname="DOSBox Development Team\0";
    static private final String string_productname="DOSBox - The DOS Emulator\0";
    static private final String string_productrev="DOSBox "+ Config.VERSION+"\0";

    static private class MODE_INFO {
        public void write(int addr) {
            byte[] data = new byte[256];
            Ptr p = new Ptr(data, 0);
            p.writew(0, ModeAttributes);
            p.writeb(2, WinAAttributes);
            p.writeb(3, WinBAttributes);
            p.writew(4, WinGranularity);
            p.writew(6, WinSize);
            p.writew(8, WinASegment);
            p.writew(10, WinBSegment);
            p.writed(12, (int)WinFuncPtr);
            p.writew(16, BytesPerScanLine);
            p.writew(18, XResolution);
            p.writew(20, YResolution);
            p.writeb(22, XCharSize);
            p.writeb(23, YCharSize);
            p.writeb(24, NumberOfPlanes);
            p.writeb(25, BitsPerPixel);
            p.writeb(26, NumberOfBanks);
            p.writeb(27, MemoryModel);
            p.writeb(28, BankSize);
            p.writeb(29, NumberOfImagePages);
            p.writeb(30, Reserved_page);
            p.writeb(31, RedMaskSize);
            p.writeb(32, RedMaskPos);
            p.writeb(33, GreenMaskSize);
            p.writeb(34, GreenMaskPos);
            p.writeb(35, BlueMaskSize);
            p.writeb(36, BlueMaskPos);
            p.writeb(37, ReservedMaskSize);
            p.writeb(38, ReservedMaskPos);
            p.writeb(39, DirectColorModeInfo);
            p.writed(40, (int)PhysBasePtr);
            p.writed(44, (int)OffScreenMemOffset);
            p.writew(48, OffScreenMemSize);
            System.arraycopy(Reserved, 0, data, 50, Reserved.length);
            Memory.MEM_BlockWrite(addr, data, data.length);
        }
        /*Bit16u*/int ModeAttributes;
        /*Bit8u*/short WinAAttributes;
        /*Bit8u*/short WinBAttributes;
        /*Bit16u*/int WinGranularity;
        /*Bit16u*/int WinSize;
        /*Bit16u*/int WinASegment;
        /*Bit16u*/int WinBSegment;
        /*Bit32u*/long WinFuncPtr;
        /*Bit16u*/int BytesPerScanLine;
        /*Bit16u*/int XResolution;
        /*Bit16u*/int YResolution;
        /*Bit8u*/short XCharSize;
        /*Bit8u*/short YCharSize;
        /*Bit8u*/short NumberOfPlanes;
        /*Bit8u*/short BitsPerPixel;
        /*Bit8u*/short NumberOfBanks;
        /*Bit8u*/short MemoryModel;
        /*Bit8u*/short BankSize;
        /*Bit8u*/short NumberOfImagePages;
        /*Bit8u*/short Reserved_page;
        /*Bit8u*/short RedMaskSize;
        /*Bit8u*/short RedMaskPos;
        /*Bit8u*/short GreenMaskSize;
        /*Bit8u*/short GreenMaskPos;
        /*Bit8u*/short BlueMaskSize;
        /*Bit8u*/short BlueMaskPos;
        /*Bit8u*/short ReservedMaskSize;
        /*Bit8u*/short ReservedMaskPos;
        /*Bit8u*/short DirectColorModeInfo;
        /*Bit32u*/long PhysBasePtr;
        /*Bit32u*/long OffScreenMemOffset;
        /*Bit16u*/int OffScreenMemSize;
        /*Bit8u*/byte[] Reserved = new byte[206];
    }

    public static /*Bit8u*/short VESA_GetSVGAInformation(/*Bit16u*/int seg,/*Bit16u*/int off) {
        /* Fill 256 byte buffer with VESA information */
        /*PhysPt*/int buffer= Memory.PhysMake(seg,off);
        /*Bitu*/int i;
        boolean vbe2=false;/*Bit16u*/int vbe2_pos=256+off;
        /*Bitu*/int id=Memory.mem_readd(buffer);
        if (((id==0x56424532)||(id==0x32454256)) && (!Int10.int10.vesa_oldvbe)) vbe2=true;
        if (vbe2) {
            for (i=0;i<0x200;i++) Memory.mem_writeb(buffer+i,0);
        } else {
            for (i=0;i<0x100;i++) Memory.mem_writeb(buffer+i,0);
        }
        /* Fill common data */
        Memory.MEM_BlockWrite(buffer,"VESA",4);				//Identification
        if (!Int10.int10.vesa_oldvbe) Memory.mem_writew(buffer+0x04,0x200);	//Vesa version 2.0
        else Memory.mem_writew(buffer+0x04,0x102);						//Vesa version 1.2
        if (vbe2) {
            Memory.mem_writed(buffer+0x06,Memory.RealMake(seg,vbe2_pos));
            for (i=0;i<string_oem.length();i++) Memory.real_writeb(seg,vbe2_pos++,string_oem.charAt(i));
            Memory.mem_writew(buffer+0x14,0x200);					//VBE 2 software revision
            Memory.mem_writed(buffer+0x16,Memory.RealMake(seg,vbe2_pos));
            for (i=0;i<string_vendorname.length();i++) Memory.real_writeb(seg,vbe2_pos++,string_vendorname.charAt(i));
            Memory.mem_writed(buffer+0x1a,Memory.RealMake(seg,vbe2_pos));
            for (i=0;i<string_productname.length();i++) Memory.real_writeb(seg,vbe2_pos++,string_productname.charAt(i));
            Memory.mem_writed(buffer+0x1e,Memory.RealMake(seg,vbe2_pos));
            for (i=0;i<string_productrev.length();i++) Memory.real_writeb(seg,vbe2_pos++,string_productrev.charAt(i));
        } else {
            Memory.mem_writed(buffer+0x06,Int10.int10.rom.oemstring);	//Oemstring
        }
        Memory.mem_writed(buffer+0x0a,0x0);					//Capabilities and flags
        Memory.mem_writed(buffer+0x0e,Int10.int10.rom.vesa_modes);	//VESA Mode list
        Memory.mem_writew(buffer+0x12,(/*Bit16u*/int)(VGA.vga.vmemsize/(64*1024))); // memory size in 64kb blocks
        return VESA_SUCCESS;
    }

    static public /*Bit8u*/short VESA_GetSVGAModeInformation(/*Bit16u*/int mode,/*Bit16u*/int seg,/*Bit16u*/int off) {
        MODE_INFO minfo = new MODE_INFO();
        //memset(&minfo,0,sizeof(minfo));
        /*PhysPt*/int buf=Memory.PhysMake(seg,off);
        /*Bitu*/int pageSize;
        /*Bit8u*/short modeAttributes;
        /*Bitu*/int i=0;

        mode&=0x3fff;	// vbe2 compatible, ignore lfb and keep screen content bits
        if (mode<0x100) return 0x01;
        if (VGA.svga.accepts_mode!=null) {
            if (!VGA.svga.accepts_mode.call(mode)) return 0x01;
        }
        boolean foundit = false;
        while (Int10_modes.ModeList_VGA[i].mode!=0xffff) {
            if (mode==Int10_modes.ModeList_VGA[i].mode) {foundit=true;break;} else i++;
        }
        if (!foundit)
            return VESA_FAIL;
    //foundit:
        if ((Int10.int10.vesa_oldvbe) && (Int10_modes.ModeList_VGA[i].mode>=0x120)) return 0x01;
        Int10.VideoModeBlock mblock=Int10_modes.ModeList_VGA[i];
        switch (mblock.type) {
        case VGA.M_LIN4:
            pageSize = mblock.sheight * mblock.swidth/2;
            minfo.BytesPerScanLine=mblock.swidth/8;
            minfo.NumberOfPlanes=0x4;
            minfo.BitsPerPixel=4;
            minfo.MemoryModel=3;	//ega planar mode
            modeAttributes = 0x1b;	// Color, graphics, no linear buffer
            break;
        case VGA.M_LIN8:
            pageSize = mblock.sheight * mblock.swidth;
            minfo.BytesPerScanLine=mblock.swidth;
            minfo.NumberOfPlanes=0x1;
            minfo.BitsPerPixel=8;
            minfo.MemoryModel=4;		//packed pixel
            modeAttributes = 0x1b;	// Color, graphics
            if (!Int10.int10.vesa_nolfb) modeAttributes |= 0x80;	// linear framebuffer
            break;
        case VGA.M_LIN15:
            pageSize = mblock.sheight * mblock.swidth*2;
            minfo.BytesPerScanLine=mblock.swidth*2;
            minfo.NumberOfPlanes=0x1;
            minfo.BitsPerPixel=15;
            minfo.MemoryModel=6;	//HiColour
            minfo.RedMaskSize=5;
            minfo.RedMaskPos=10;
            minfo.GreenMaskSize=5;
            minfo.GreenMaskPos=5;
            minfo.BlueMaskSize=5;
            minfo.BlueMaskPos=0;
            minfo.ReservedMaskSize=0x01;
            minfo.ReservedMaskPos=0x0f;
            modeAttributes = 0x1b;	// Color, graphics
            if (!Int10.int10.vesa_nolfb) modeAttributes |= 0x80;	// linear framebuffer
            break;
        case VGA.M_LIN16:
            pageSize = mblock.sheight * mblock.swidth*2;
            minfo.BytesPerScanLine=mblock.swidth*2;
            minfo.NumberOfPlanes=0x1;
            minfo.BitsPerPixel=16;
            minfo.MemoryModel=6;	//HiColour
            minfo.RedMaskSize=5;
            minfo.RedMaskPos=11;
            minfo.GreenMaskSize=6;
            minfo.GreenMaskPos=5;
            minfo.BlueMaskSize=5;
            minfo.BlueMaskPos=0;
            modeAttributes = 0x1b;	// Color, graphics
            if (!Int10.int10.vesa_nolfb) modeAttributes |= 0x80;	// linear framebuffer
            break;
        case VGA.M_LIN32:
            pageSize = mblock.sheight * mblock.swidth*4;
            minfo.BytesPerScanLine=mblock.swidth*4;
            minfo.NumberOfPlanes=0x1;
            minfo.BitsPerPixel=32;
            minfo.MemoryModel=6;	//HiColour
            minfo.RedMaskSize=8;
            minfo.RedMaskPos=0x10;
            minfo.GreenMaskSize=0x8;
            minfo.GreenMaskPos=0x8;
            minfo.BlueMaskSize=0x8;
            minfo.BlueMaskPos=0x0;
            minfo.ReservedMaskSize=0x8;
            minfo.ReservedMaskPos=0x18;
            modeAttributes = 0x1b;	// Color, graphics
            if (!Int10.int10.vesa_nolfb) modeAttributes |= 0x80;	// linear framebuffer
            break;
        case VGA.M_TEXT:
            pageSize = 0;
            minfo.BytesPerScanLine = mblock.twidth * 2;
            minfo.NumberOfPlanes=0x4;
            minfo.BitsPerPixel=4;
            minfo.MemoryModel=0;	// text
            modeAttributes = 0x0f;	//Color, text, bios output
            break;
        default:
            return VESA_FAIL;
        }
        if ((pageSize & 0xFFFF)!=0) {
            // It is documented that many applications assume 64k-aligned page sizes
            // VBETEST is one of them
            pageSize += 0x10000;
            pageSize &= ~0xFFFF;
        }
        /*Bitu*/int pages = 0;
        if (pageSize > VGA.vga.vmemsize) {
            // mode not supported by current hardware configuration
            modeAttributes &= ~0x1;
        } else if (pageSize!=0) {
            pages = (VGA.vga.vmemsize / pageSize)-1;
        }
        minfo.NumberOfImagePages=(short)pages;
        minfo.ModeAttributes=modeAttributes;
        minfo.WinAAttributes=0x7;	// Exists/readable/writable

        if (mblock.type==VGA.M_TEXT) {
            minfo.WinGranularity=32;
            minfo.WinSize=32;
            minfo.WinASegment=0xb800;
            minfo.XResolution=mblock.swidth;
            minfo.YResolution=mblock.sheight;
        } else {
            minfo.WinGranularity=64;
            minfo.WinSize=64;
            minfo.WinASegment=0xa000;
            minfo.XResolution=mblock.swidth;
            minfo.YResolution=mblock.sheight;
        }
        minfo.WinFuncPtr=jdos.cpu.Callback.CALLBACK_RealPointer(callback.setwindow);
        minfo.NumberOfBanks=0x1;
        minfo.Reserved_page=0x1;
        minfo.XCharSize=(short)mblock.cwidth;
        minfo.YCharSize=(short)mblock.cheight;
        if (!Int10.int10.vesa_nolfb) minfo.PhysBasePtr=Int10.S3_LFB_BASE;

        minfo.write((int)buf);//MEM_BlockWrite(buf,&minfo,sizeof(MODE_INFO));
        return VESA_SUCCESS;
    }

    public static /*Bit8u*/short VESA_SetSVGAMode(/*Bit16u*/int mode) {
        if (Int10_modes.INT10_SetVideoMode(mode)) {
            Int10.int10.vesa_setmode=mode&0x7fff;
            return VESA_SUCCESS;
        }
        return VESA_FAIL;
    }

    public static int VESA_GetSVGAMode() {
        if (Int10.int10.vesa_setmode!=0xffff) return Int10.int10.vesa_setmode;
        else return Int10_modes.CurMode.mode;
    }

    public static /*Bit8u*/short VESA_SetCPUWindow(/*Bit8u*/short window,/*Bit8u*/short address) {
        if (window!=0) return VESA_FAIL;
        if (((/*Bit32u*/long)(address)*64*1024<VGA.vga.vmemsize)) {
            IoHandler.IO_Write(0x3d4,0x6a);
            IoHandler.IO_Write(0x3d5,address);
            return VESA_SUCCESS;
        } else return VESA_FAIL;
    }

    static public int VESA_GetCPUWindow(/*Bit8u*/short window, CPU_Regs.Reg reg) {
        if (window!=0) return VESA_FAIL;
        IoHandler.IO_Write(0x3d4,0x6a);
        reg.word(IoHandler.IO_Read(0x3d5));
        return VESA_SUCCESS;
    }

    public static /*Bit8u*/short VESA_SetPalette(/*PhysPt*/int data,/*Bitu*/int index,/*Bitu*/int count) {
//Structure is (vesa 3.0 doc): blue,green,red,alignment
        /*Bit8u*/int r,g,b;
        if (index>255) return VESA_FAIL;
        if (index+count>256) return VESA_FAIL;
        IoHandler.IO_Write(0x3c8,index);
        while (count!=0) {
            b = Memory.mem_readb(data++);
            g = Memory.mem_readb(data++);
            r = Memory.mem_readb(data++);
            data++;
            IoHandler.IO_Write(0x3c9,r);
            IoHandler.IO_Write(0x3c9,g);
            IoHandler.IO_Write(0x3c9,b);
            count--;
        }
        return VESA_SUCCESS;
    }


    static public /*Bit8u*/short VESA_GetPalette(/*PhysPt*/int data,/*Bitu*/int index,/*Bitu*/int count) {
        /*Bit8u*/short r,g,b;
        if (index>255) return VESA_FAIL;
        if (index+count>256) return VESA_FAIL;
        IoHandler.IO_Write(0x3c7,index);
        while (count!=0) {
            r = IoHandler.IO_Read(0x3c9);
            g = IoHandler.IO_Read(0x3c9);
            b = IoHandler.IO_Read(0x3c9);
            Memory.mem_writeb(data++,b);
            Memory.mem_writeb(data++,g);
            Memory.mem_writeb(data++,r);
            data++;
            count--;
        }
        return VESA_SUCCESS;
    }

    // maximum offset for the S3 Trio64 is 10 bits
    static private final int S3_MAX_OFFSET = 0x3ff;

    public static /*Bit8u*/short VESA_ScanLineLength(/*Bit8u*/short subcall,/*Bit16u*/int val, /*Bit16u*/IntRef bytes,/*Bit16u*/IntRef pixels,/*Bit16u*/IntRef lines) {
        // offset register: virtual scanline length
        /*Bitu*/int pixels_per_offset;
        /*Bitu*/int bytes_per_offset = 8;
        /*Bitu*/int vmemsize = VGA.vga.vmemsize;
        /*Bitu*/int new_offset = VGA.vga.config.scan_len;
        /*Bitu*/int screen_height = Int10_modes.CurMode.sheight;

        switch (Int10_modes.CurMode.type) {
        case VGA.M_TEXT:
            vmemsize = 0x8000;      // we have only the 32kB window here
            screen_height = Int10_modes.CurMode.theight;
            pixels_per_offset = 16; // two characters each 8 pixels wide
            bytes_per_offset = 4;   // 2 characters + 2 attributes
            break;
        case VGA.M_LIN4:
            pixels_per_offset = 16;
            break;
        case VGA.M_LIN8:
            pixels_per_offset = 8;
            break;
        case VGA.M_LIN15:
        case VGA.M_LIN16:
            pixels_per_offset = 4;
            break;
        case VGA.M_LIN32:
            pixels_per_offset = 2;
            break;
        default:
            return VESA_MODE_UNSUPPORTED;
        }
        switch (subcall) {
        case 0x00: // set scan length in pixels
            new_offset = val / pixels_per_offset;
            if ((val % pixels_per_offset) != 0) new_offset++;

            if (new_offset > S3_MAX_OFFSET)
                return VESA_HW_UNSUPPORTED; // scanline too long
            VGA.vga.config.scan_len = new_offset;
            VGA_draw.VGA_CheckScanLength();
            break;

        case 0x01: // get current scanline length
            // implemented at the end of this function
            break;

        case 0x02: // set scan length in bytes
            new_offset = val / bytes_per_offset;
            if ((val % bytes_per_offset)!=0) new_offset++;

            if (new_offset > S3_MAX_OFFSET)
                return VESA_HW_UNSUPPORTED; // scanline too long
            VGA.vga.config.scan_len = new_offset;
            VGA_draw.VGA_CheckScanLength();
            break;

        case 0x03: // get maximum scan line length
            // the smaller of either the hardware maximum scanline length or
            // the limit to get full y resolution of this mode
            new_offset = S3_MAX_OFFSET;
            if ((new_offset * bytes_per_offset * screen_height) > vmemsize)
                new_offset = vmemsize / (bytes_per_offset * screen_height);
            break;

        default:
            return VESA_UNIMPLEMENTED;
        }

        // set up the return values
        bytes.value = (new_offset * bytes_per_offset) & 0xFFFF;
        pixels.value = (new_offset * pixels_per_offset) & 0xFFFF;
        if (bytes.value == 0)
            // return failure on division by zero
            // some real VESA BIOS implementations may crash here
            return VESA_FAIL;

        lines.value = (vmemsize / bytes.value) & 0xFFFF;

        if (Int10_modes.CurMode.type==VGA.M_TEXT)
            lines.value *= Int10_modes.CurMode.cheight;

        return VESA_SUCCESS;
    }

    static public /*Bit8u*/short VESA_SetDisplayStart(/*Bit16u*/int x,/*Bit16u*/int y) {
        // TODO wait for retrace in case bl==0x80
        /*Bitu*/int pixels_per_offset;
        /*Bitu*/int panning_factor = 1;

        switch (Int10_modes.CurMode.type) {
        case VGA.M_TEXT:
        case VGA.M_LIN4:
            pixels_per_offset = 16;
            break;
        case VGA.M_LIN8:
            panning_factor = 2; // the panning register ignores bit0 in this mode
            pixels_per_offset = 8;
            break;
        case VGA.M_LIN15:
        case VGA.M_LIN16:
            panning_factor = 2; // this may be DOSBox specific
            pixels_per_offset = 4;
            break;
        case VGA.M_LIN32:
            pixels_per_offset = 2;
            break;
        default:
            return VESA_MODE_UNSUPPORTED;
        }
        // We would have to divide y by the character height for text modes and
        // write the remainder to the CRTC preset row scan register,
        // but VBE2 BIOSes that actually get that far also don't.
        // Only a VBE3 BIOS got it right.
        /*Bitu*/int virtual_screen_width = VGA.vga.config.scan_len * pixels_per_offset;
        /*Bitu*/int new_start_pixel = virtual_screen_width * y + x;
        /*Bitu*/int new_crtc_start = new_start_pixel / (pixels_per_offset/2);
        /*Bitu*/int new_panning = new_start_pixel % (pixels_per_offset/2);
        new_panning *= panning_factor;

        VGA.vga.config.display_start = new_crtc_start;

        // Setting the panning register is nice as it allows for super smooth
        // scrolling, but if we hit the retrace pulse there may be flicker as
        // panning and display start are latched at different times.

        IoHandler.IO_Read(0x3da);              // reset attribute flipflop
        IoHandler.IO_Write(0x3c0,0x13 | 0x20); // panning register, screen on
        IoHandler.IO_Write(0x3c0,new_panning);

        return VESA_SUCCESS;
    }

    public static /*Bit8u*/short VESA_GetDisplayStart(/*Bit16u*/IntRef x,/*Bit16u*/IntRef y) {
        /*Bitu*/int pixels_per_offset;
        /*Bitu*/int panning_factor = 1;

        switch (Int10_modes.CurMode.type) {
        case VGA.M_TEXT:
            pixels_per_offset = 16;
            break;
        case VGA.M_LIN4:
            pixels_per_offset = 16;
            break;
        case VGA.M_LIN8:
            panning_factor = 2;
            pixels_per_offset = 8;
            break;
        case VGA.M_LIN15:
        case VGA.M_LIN16:
            panning_factor = 2;
            pixels_per_offset = 4;
            break;
        case VGA.M_LIN32:
            pixels_per_offset = 2;
            break;
        default:
            return VESA_MODE_UNSUPPORTED;
        }

        IoHandler.IO_Read(0x3da);              // reset attribute flipflop
        IoHandler.IO_Write(0x3c0,0x13 | 0x20); // panning register, screen on
        /*Bit8u*/short panning = IoHandler.IO_Read(0x3c1);

        /*Bitu*/int virtual_screen_width = VGA.vga.config.scan_len * pixels_per_offset;
        /*Bitu*/int start_pixel = VGA.vga.config.display_start * (pixels_per_offset/2) + panning / panning_factor;

        y.value = start_pixel / virtual_screen_width;
        x.value = start_pixel % virtual_screen_width;
        return VESA_SUCCESS;
    }

    static private jdos.cpu.Callback.Handler VESA_SetWindow = new jdos.cpu.Callback.Handler() {
        public String getName() {
            return "Int10_vesa.VESA_SetWindow";
        }
        public /*Bitu*/int call() {
            if (CPU_Regs.reg_ebx.high()!=0) {CPU_Regs.reg_eax.high(VESA_GetCPUWindow((short)CPU_Regs.reg_ebx.low(), CPU_Regs.reg_edx));}
            else CPU_Regs.reg_eax.high(VESA_SetCPUWindow((short)CPU_Regs.reg_ebx.low(),(short)CPU_Regs.reg_edx.low()));
            CPU_Regs.reg_eax.low(0x4f);
            return 0;
        }
    };

    static private jdos.cpu.Callback.Handler VESA_PMSetWindow = new jdos.cpu.Callback.Handler() {
        public String getName() {
            return "Int10_vesa.VESA_PMSetWindow 0x"+Integer.toHexString(CPU_Regs.reg_edx.word());
        }
        public /*Bitu*/int call() {
            VESA_SetCPUWindow((short)0, (short)CPU_Regs.reg_edx.low());
            return 0;
        }
    };

    static private jdos.cpu.Callback.Handler VESA_PMSetPalette = new jdos.cpu.Callback.Handler() {
        public String getName() {
            return "Int10_vesa.VESA_PMSetPalette";
        }
        public /*Bitu*/int call() {
            VESA_SetPalette(CPU_Regs.reg_esPhys.dword +  CPU_Regs.reg_edi.dword, CPU_Regs.reg_edx.word(), CPU_Regs.reg_ecx.word() );
            return 0;
        }
    };

    static private jdos.cpu.Callback.Handler VESA_PMSetStart = new jdos.cpu.Callback.Handler() {
        public String getName() {
            return "Int10_vesa.VESA_PMSetStart 0x"+Integer.toHexString((CPU_Regs.reg_edx.word() << 16) | CPU_Regs.reg_ecx.word());
        }
        public /*Bitu*/int call() {
            // This function is from VBE2 and directly sets the VGA
	        // display start address.

	        // TODO wait for retrace in case bl==0x80
            /*Bit32u*/int start = (CPU_Regs.reg_edx.word() << 16) | CPU_Regs.reg_ecx.word();
            VGA.vga.config.display_start = start;
            return 0;
        }
    };

    static public void INT10_SetupVESA() {
        callback = new Callback();
        /* Put the mode list somewhere in memory */
        /*Bitu*/int i;
        i=0;
        Int10.int10.rom.vesa_modes=Memory.RealMake(0xc000,Int10.int10.rom.used);
        //TODO Maybe add normal vga modes too, but only seems to complicate things
        while (Int10_modes.ModeList_VGA[i].mode!=0xffff) {
            boolean canuse_mode=false;
            if (VGA.svga==null || VGA.svga.accepts_mode==null) canuse_mode=true;
            else {
                if (VGA.svga.accepts_mode.call(Int10_modes.ModeList_VGA[i].mode)) canuse_mode=true;
            }
            if (Int10_modes.ModeList_VGA[i].mode>=0x100 && canuse_mode) {
                if ((!Int10.int10.vesa_oldvbe) || (Int10_modes.ModeList_VGA[i].mode<0x120)) {
                    Memory.phys_writew((int)Memory.PhysMake(0xc000,Int10.int10.rom.used),Int10_modes.ModeList_VGA[i].mode);
                    Int10.int10.rom.used+=2;
                }
            }
            i++;
        }
        Memory.phys_writew((int)Memory.PhysMake(0xc000,Int10.int10.rom.used),0xffff);
        Int10.int10.rom.used+=2;
        Int10.int10.rom.oemstring=Memory.RealMake(0xc000,Int10.int10.rom.used);
        Memory.phys_writes(0xc0000+Int10.int10.rom.used, string_oem);
        Int10.int10.rom.used+=string_oem.length();
//        switch (svgaCard) {  //:TODO: wtf
//        case SVGA_S3Trio:
//            break;
//        }
        callback.setwindow=jdos.cpu.Callback.CALLBACK_Allocate();
        callback.pmPalette=jdos.cpu.Callback.CALLBACK_Allocate();
        callback.pmStart=jdos.cpu.Callback.CALLBACK_Allocate();
        jdos.cpu.Callback.CALLBACK_Setup(callback.setwindow,VESA_SetWindow,jdos.cpu.Callback.CB_RETF, "VESA Real Set Window");
        /* Prepare the pmode interface */
        Int10.int10.rom.pmode_interface=Memory.RealMake(0xc000,Int10.int10.rom.used);
        Int10.int10.rom.used += 8;		//Skip the byte later used for offsets
        /* PM Set Window call */
        Int10.int10.rom.pmode_interface_window = Int10.int10.rom.used - Memory.RealOff( Int10.int10.rom.pmode_interface );
        Memory.phys_writew( (int)Memory.Real2Phys(Int10.int10.rom.pmode_interface) + 0, Int10.int10.rom.pmode_interface_window );
        callback.pmWindow=jdos.cpu.Callback.CALLBACK_Allocate();
        Int10.int10.rom.used += (/*Bit16u*/int)jdos.cpu.Callback.CALLBACK_Setup(callback.pmWindow, VESA_PMSetWindow, jdos.cpu.Callback.CB_RETN, Memory.PhysMake(0xc000,Int10.int10.rom.used), "VESA PM Set Window");
        /* PM Set start call */
        Int10.int10.rom.pmode_interface_start = Int10.int10.rom.used - Memory.RealOff( Int10.int10.rom.pmode_interface );
        Memory.phys_writew( (int)Memory.Real2Phys(Int10.int10.rom.pmode_interface) + 2, Int10.int10.rom.pmode_interface_start);
        callback.pmStart=jdos.cpu.Callback.CALLBACK_Allocate();
        Int10.int10.rom.used += (/*Bit16u*/int)jdos.cpu.Callback.CALLBACK_Setup(callback.pmStart, VESA_PMSetStart, jdos.cpu.Callback.CB_RETN, Memory.PhysMake(0xc000,Int10.int10.rom.used), "VESA PM Set Start");
        /* PM Set Palette call */
        Int10.int10.rom.pmode_interface_palette = Int10.int10.rom.used - Memory.RealOff( Int10.int10.rom.pmode_interface );
        Memory.phys_writew( (int)Memory.Real2Phys(Int10.int10.rom.pmode_interface) + 4, Int10.int10.rom.pmode_interface_palette);
        callback.pmPalette=jdos.cpu.Callback.CALLBACK_Allocate();
        Int10.int10.rom.used += (/*Bit16u*/int)jdos.cpu.Callback.CALLBACK_Setup(callback.pmPalette, VESA_PMSetPalette, jdos.cpu.Callback.CB_RETN, Memory.PhysMake(0xc000,Int10.int10.rom.used), "VESA PM Set Palette");
        /* Finalize the size and clear the required ports pointer */
        Memory.phys_writew( (int)Memory.Real2Phys(Int10.int10.rom.pmode_interface) + 6, 0);
        Int10.int10.rom.pmode_interface_size=Int10.int10.rom.used - Memory.RealOff( Int10.int10.rom.pmode_interface );
    }

}
