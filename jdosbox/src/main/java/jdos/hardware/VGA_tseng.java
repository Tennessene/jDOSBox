package jdos.hardware;

import jdos.ints.Int10_modes;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_tseng {
    // Tseng ET4K data
    private static class SVGA_ET4K_DATA {
        public /*Bit8u*/boolean extensionsEnabled = true;

    // Stored exact values of some registers. Documentation only specifies some bits but hardware checks may
    // expect other bits to be preserved.
        public /*Bitu*/int store_3d4_31;
        public /*Bitu*/int store_3d4_32;
        public /*Bitu*/int store_3d4_33;
        public /*Bitu*/int store_3d4_34;
        public /*Bitu*/int store_3d4_35;
        public /*Bitu*/int store_3d4_36;
        public /*Bitu*/int store_3d4_37;
        public /*Bitu*/int store_3d4_3f;

        public /*Bitu*/int store_3c0_16;
        public /*Bitu*/int store_3c0_17;

        public /*Bitu*/int store_3c4_06;
        public /*Bitu*/int store_3c4_07;

        public /*Bitu*/int[] clockFreq=new int[16];
        public /*Bitu*/int biosMode;
    }

    static SVGA_ET4K_DATA et4k = new SVGA_ET4K_DATA();

//    #define STORE_ET4K(port, index) \
//        case 0x##index: \
//        et4k.store_##port##_##index = val; \
//            break;
//
//    #define RESTORE_ET4K(port, index) \
//        case 0x##index: \
//            return et4k.store_##port##_##index;

    // Tseng ET4K implementation
    static private VGA.tWritePort write_p3d5_et4k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            if(!et4k.extensionsEnabled && reg!=0x33)
                return;

            switch(reg) {
            /*
            3d4h index 31h (R/W):  General Purpose
            bit  0-3  Scratch pad
                 6-7  Clock Select bits 3-4. Bits 0-1 are in 3C2h/3CCh bits 2-3.
            */
            case 0x31:
                et4k.store_3d4_31 = val;
                break;

            // 3d4h index 32h - RAS/CAS Configuration (R/W)
            // No effect on emulation. Should not be written by software.
            case 0x32:
                et4k.store_3d4_32 = val;
                break;
            case 0x33:
                // 3d4 index 33h (R/W): Extended start Address
                // 0-1 Display Start Address bits 16-17
                // 2-3 Cursor start address bits 16-17
                // Used by standard Tseng ID scheme
                et4k.store_3d4_33 = val;
                VGA.vga.config.display_start = (VGA.vga.config.display_start & 0xffff) | ((val & 0x03)<<16);
                VGA.vga.config.cursor_start = (VGA.vga.config.cursor_start & 0xffff) | ((val & 0x0c)<<14);
                break;

            /*
            3d4h index 34h (R/W): 6845 Compatibility Control Register
            bit    0  Enable CS0 (alternate clock timing)
                   1  Clock Select bit 2.  Bits 0-1 in 3C2h bits 2-3, bits 3-4 are in 3d4h
                      index 31h bits 6-7
                   2  Tristate ET4000 bus and color outputs if set
                   3  Video Subsystem Enable Register at 46E8h if set, at 3C3h if clear.
                   4  Enable Translation ROM for reading CRTC and MISCOUT if set
                   5  Enable Translation ROM for writing CRTC and MISCOUT if set
                   6  Enable double scan in AT&T compatibility mode if set
                   7  Enable 6845 compatibility if set
            */
            // TODO: Bit 6 may have effect on emulation
            case 0x34:
                et4k.store_3d4_34 = val;
                break;
            case 0x35:
            /*
            3d4h index 35h (R/W): Overflow High
            bit    0  Vertical Blank Start Bit 10 (3d4h index 15h).
                   1  Vertical Total Bit 10 (3d4h index 6).
                   2  Vertical Display End Bit 10 (3d4h index 12h).
                   3  Vertical Sync Start Bit 10 (3d4h index 10h).
                   4  Line Compare Bit 10 (3d4h index 18h).
                   5  Gen-Lock Enabled if set (External sync)
                   6  (4000) Read/Modify/Write Enabled if set. Currently not implemented.
                   7  Vertical interlace if set. The Vertical timing registers are
                    programmed as if the mode was non-interlaced!!
            */
                et4k.store_3d4_35 = val;
                VGA.vga.config.line_compare = (VGA.vga.config.line_compare & 0x3ff) | ((val&0x10)<<6);
            // Abusing s3 ex_ver_overflow field. This is to be cleaned up later.
                {
                    /*Bit8u*/int s3val =
                        ((val & 0x01) << 2) | // vbstart
                        ((val & 0x02) >> 1) | // vtotal
                        ((val & 0x04) >> 1) | // vdispend
                        ((val & 0x08) << 1) | // vsyncstart (?)
                        ((val & 0x10) << 2); // linecomp
                    if (((s3val ^ VGA.vga.s3.ex_ver_overflow) & 0x3)!=0) {
                        VGA.vga.s3.ex_ver_overflow=(short)s3val;
                        VGA.VGA_StartResize();
                    } else VGA.vga.s3.ex_ver_overflow=(short)s3val;
                }
                break;

            // 3d4h index 36h - Video System Configuration 1 (R/W)
            // VGADOC provides a lot of info on this register, Ferraro has significantly less detail.
            // This is unlikely to be used by any games. Bit 4 switches chipset into linear mode -
            // that may be useful in some cases if there is any software actually using it.
            // TODO (not near future): support linear addressing
            case 0x36:
                et4k.store_3d4_34 = val;
                break;

            // 3d4h index 37 - Video System Configuration 2 (R/W)
            // Bits 0,1, and 3 provides information about memory size:
            // 0-1 Bus width (1: 8 bit, 2: 16 bit, 3: 32 bit)
            // 3   Size of RAM chips (0: 64Kx, 1: 256Kx)
            // Other bits have no effect on emulation.
            case 0x37:
                if (val != et4k.store_3d4_37) {
                    et4k.store_3d4_37 = val;
                    VGA.vga.vmemwrap = ((64*1024)<<((val&8)>>2))<<((val&3)-1);
                    VGA_memory.VGA_SetupHandlers();
                }
                break;

            case 0x3f:
            /*
            3d4h index 3Fh (R/W):
            bit    0  Bit 8 of the Horizontal Total (3d4h index 0)
                   2  Bit 8 of the Horizontal Blank Start (3d4h index 3)
                   4  Bit 8 of the Horizontal Retrace Start (3d4h index 4)
                   7  Bit 8 of the CRTC offset register (3d4h index 13h).
            */
            // The only unimplemented one is bit 7
                et4k.store_3d4_3f = val;
            // Abusing s3 ex_hor_overflow field which very similar. This is
            // to be cleaned up later
                if (((val ^ VGA.vga.s3.ex_hor_overflow) & 3)!=0) {
                    VGA.vga.s3.ex_hor_overflow=(short)(val&0x15);
                    VGA.VGA_StartResize();
                } else VGA.vga.s3.ex_hor_overflow=(short)(val&0x15);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:CRTC:ET4K:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3d5_et4k = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            if (!et4k.extensionsEnabled && reg!=0x33)
                return 0x0;
            switch(reg) {
                case 0x31:
                    return et4k.store_3d4_31;
                case 0x32:
                    return et4k.store_3d4_32;
                case 0x33:
                    return et4k.store_3d4_33;
                case 0x34:
                    return et4k.store_3d4_34;
                case 0x35:
                    return et4k.store_3d4_35;
                case 0x36:
                    return et4k.store_3d4_36;
                case 0x37:
                    return et4k.store_3d4_37;
                case 0x3f:
                    return et4k.store_3d4_3f;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:CRTC:ET4K:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    static private VGA.tWritePort write_p3c5_et4k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            switch(reg) {
            /*
            3C4h index  6  (R/W): TS State Control
            bit 1-2  Font Width Select in dots/character
                    If 3C4h index 4 bit 0 clear:
                        0: 9 dots, 1: 10 dots, 2: 12 dots, 3: 6 dots
                    If 3C4h index 5 bit 0 set:
                        0: 8 dots, 1: 11 dots, 2: 7 dots, 3: 16 dots
                    Only valid if 3d4h index 34h bit 3 set.
            */
            // TODO: Figure out if this has any practical use
            case 0x06:
                et4k.store_3c4_06 = val;
                break;
            // 3C4h index  7  (R/W): TS Auxiliary Mode
            // Unlikely to be used by games (things like ROM enable/disable and emulation of VGA vs EGA)
            case 0x07:
                et4k.store_3c4_07 = val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:SEQ:ET4K:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3c5_et4k = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            switch(reg) {
                case 0x06:
                    return et4k.store_3c4_06;
                case 0x07:
                    return et4k.store_3c4_07;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:SEQ:ET4K:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    /*
    3CDh (R/W): Segment Select
    bit 0-3  64k Write bank number (0..15)
        4-7  64k Read bank number (0..15)
    */
    static private IoHandler.IO_WriteHandler io_write_p3cd_et4k = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            write_p3cd_et4k.call(port, val, iolen);
        }
    };

    static private VGA.tWritePort write_p3cd_et4k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
           VGA.vga.svga.bank_write = (short)(val & 0x0f);
           VGA.vga.svga.bank_read = (short)((val>>4) & 0x0f);
           VGA_memory.VGA_SetupHandlers();
        }
    };

    static private IoHandler.IO_ReadHandler read_p3cd_et4k = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return (VGA.vga.svga.bank_read<<4)|VGA.vga.svga.bank_write;
        }
    };

    static private VGA.tWritePort write_p3c0_et4k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            switch(reg) {
            // 3c0 index 16h: ATC Miscellaneous
            // VGADOC provides a lot of information, Ferarro documents only two bits
            // and even those incompletely. The register is used as part of identification
            // scheme.
            // Unlikely to be used by any games but double timing may be useful.
            // TODO: Figure out if this has any practical use
            case 0x16:
                et4k.store_3c0_16 = val;
                break;
            /*
            3C0h index 17h (R/W):  Miscellaneous 1
            bit   7  If set protects the internal palette ram and redefines the attribute
                    bits as follows:
                    Monochrome:
                    bit 0-2  Select font 0-7
                            3  If set selects blinking
                            4  If set selects underline
                            5  If set prevents the character from being displayed
                            6  If set displays the character at half intensity
                            7  If set selects reverse video
                    Color:
                    bit 0-1  Selects font 0-3
                            2  Foreground Blue
                            3  Foreground Green
                            4  Foreground Red
                            5  Background Blue
                            6  Background Green
                            7  Background Red
            */
            // TODO: Figure out if this has any practical use
            case 0x17:
                et4k.store_3c0_17 = val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:ATTR:ET4K:Write to illegal index "+Integer.toString(reg, 16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3c1_et4k = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            switch(reg) {
                case 0x16:
                    return et4k.store_3c0_16;
                case 0x17:
                    return et4k.store_3c0_17;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:ATTR:ET4K:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    /*
    These ports are used but have little if any effect on emulation:
        3BFh (R/W): Hercules Compatibility Mode
        3CBh (R/W): PEL Address/Data Wd
        3CEh index 0Dh (R/W): Microsequencer Mode
        3CEh index 0Eh (R/W): Microsequencer Reset
        3d8h (R/W): Display Mode Control
        3DEh (W);  AT&T Mode Control Register
    */

    static private /*Bitu*/int get_clock_index_et4k() {
        // Ignoring bit 4, using "only" 16 frequencies. Looks like most implementations had only that
        return ((VGA.vga.misc_output>>2)&3) | ((et4k.store_3d4_34<<1)&4) | ((et4k.store_3d4_31>>3)&8);
    }

    static private void set_clock_index_et4k(/*Bitu*/int index) {
        // Shortwiring register reads/writes for simplicity
        IoHandler.IO_Write(0x3c2, (VGA.vga.misc_output&~0x0c)|((index&3)<<2));
        et4k.store_3d4_34 = (et4k.store_3d4_34&~0x02)|((index&4)>>1);
        et4k.store_3d4_31 = (et4k.store_3d4_31&~0xc0)|((index&8)<<3); // (index&0x18) if 32 clock frequencies are to be supported
    }

    private static VGA.tFinishSetMode FinishSetMode_ET4K = new VGA.tFinishSetMode() {
        public void call(/*Bitu*/int crtc_base, VGA.VGA_ModeExtraData modeData) {
            et4k.biosMode = modeData.modeNo;

            IoHandler.IO_Write(0x3cd, 0x00); // both banks to 0

            // Reinterpret hor_overflow. Curiously, three bits out of four are
            // in the same places. Input has hdispend (not supported), output
            // has CRTC offset (also not supported)
            /*Bit8u*/int et4k_hor_overflow =
                (modeData.hor_overflow & 0x01) |
                (modeData.hor_overflow & 0x04) |
                (modeData.hor_overflow & 0x10);
            IoHandler.IO_Write(crtc_base,0x3f);IoHandler.IO_Write(crtc_base+1,et4k_hor_overflow);

            // Reinterpret ver_overflow
            /*Bit8u*/int et4k_ver_overflow =
                ((modeData.ver_overflow & 0x01) << 1) | // vtotal10
                ((modeData.ver_overflow & 0x02) << 1) | // vdispend10
                ((modeData.ver_overflow & 0x04) >> 2) | // vbstart10
                ((modeData.ver_overflow & 0x10) >> 1) | // vretrace10 (tseng has vsync start?)
                ((modeData.ver_overflow & 0x40) >> 2);  // line_compare
            IoHandler.IO_Write(crtc_base,0x35);IoHandler.IO_Write(crtc_base+1,et4k_ver_overflow);

            // Clear remaining ext CRTC registers
            IoHandler.IO_Write(crtc_base,0x31);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x32);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x33);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x34);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x36);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x37);IoHandler.IO_Write(crtc_base+1,0x0c|(VGA.vga.vmemsize==1024*1024?3:VGA.vga.vmemsize==512*1024?2:1));
            // Clear ext SEQ
            IoHandler.IO_Write(0x3c4,0x06);IoHandler.IO_Write(0x3c5,0);
            IoHandler.IO_Write(0x3c4,0x07);IoHandler.IO_Write(0x3c5,0);
            // Clear ext ATTR
            IoHandler.IO_Write(0x3c0,0x16);IoHandler.IO_Write(0x3c0,0);
            IoHandler.IO_Write(0x3c0,0x17);IoHandler.IO_Write(0x3c0,0);

            // Select SVGA clock to get close to 60Hz (not particularly clean implementation)
            if (modeData.modeNo > 0x13) {
                /*Bitu*/int target = modeData.vtotal*8*modeData.htotal*60;
                /*Bitu*/int best = 1;
                /*Bits*/int dist = 100000000;
                for (/*Bitu*/int i=0; i<16; i++) {
                    /*Bits*/int cdiff=Math.abs((/*Bits*/int)(target-et4k.clockFreq[i]));
                    if (cdiff < dist) {
                        best = i;
                        dist = cdiff;
                    }
                }
                set_clock_index_et4k(best);
            }

            if(VGA.svga.determine_mode!=null)
                VGA.svga.determine_mode.call();

            // Verified (on real hardware and in a few games): Tseng ET4000 used chain4 implementation
            // different from standard VGA. It was also not limited to 64K in regular mode 13h.
            VGA.vga.config.compatible_chain4 = false;
            VGA.vga.vmemwrap = VGA.vga.vmemsize;

            VGA_memory.VGA_SetupHandlers();
        }
    };

    static private VGA.tDetermineMode DetermineMode_ET4K = new VGA.tDetermineMode() {
        public void call() {
            // Close replica from the base implementation. It will stay here
            // until I figure a way to either distinguish M_VGA and M_LIN8 or
            // merge them.
            if ((VGA.vga.attr.mode_control & 1)!=0) {
                if ((VGA.vga.gfx.mode & 0x40)!=0) VGA.VGA_SetMode((et4k.biosMode<=0x13)?VGA.M_VGA:VGA.M_LIN8); // Ugly...
                else if ((VGA.vga.gfx.mode & 0x20)!=0) VGA.VGA_SetMode(VGA.M_CGA4);
                else if ((VGA.vga.gfx.miscellaneous & 0x0c)==0x0c) VGA.VGA_SetMode(VGA.M_CGA2);
                else VGA.VGA_SetMode((et4k.biosMode<=0x13)?VGA.M_EGA:VGA.M_LIN4);
            } else {
                VGA.VGA_SetMode(VGA.M_TEXT);
            }
        }
    };

    static private VGA.tSetClock SetClock_ET4K = new VGA.tSetClock() {
        public void call(/*Bitu*/int which,/*Bitu*/int target) {
            et4k.clockFreq[which]=1000*target;
            VGA.VGA_StartResize();
        }
    };

    static private VGA.tGetClock GetClock_ET4K = new VGA.tGetClock() {
        public /*Bitu*/int call() {
            return et4k.clockFreq[get_clock_index_et4k()];
        }
    };

    static private VGA.tAcceptsMode AcceptsMode_ET4K = new VGA.tAcceptsMode() {
        public boolean call(/*Bitu*/int modeNo) {
            return Int10_modes.VideoModeMemSize(modeNo) < VGA.vga.vmemsize;
        //	return mode != 0x3d;
        }
    };

    static public void SVGA_Setup_TsengET4K() {
        VGA.svga.write_p3d5 = write_p3d5_et4k;
        VGA.svga.read_p3d5 = read_p3d5_et4k;
        VGA.svga.write_p3c5 = write_p3c5_et4k;
        VGA.svga.read_p3c5 = read_p3c5_et4k;
        VGA.svga.write_p3c0 = write_p3c0_et4k;
        VGA.svga.read_p3c1 = read_p3c1_et4k;

        VGA.svga.set_video_mode = FinishSetMode_ET4K;
        VGA.svga.determine_mode = DetermineMode_ET4K;
        VGA.svga.set_clock = SetClock_ET4K;
        VGA.svga.get_clock = GetClock_ET4K;
        VGA.svga.accepts_mode = AcceptsMode_ET4K;

        // From the depths of X86Config, probably inexact
        VGA.VGA_SetClock(0,VGA.CLK_25);
        VGA.VGA_SetClock(1,VGA.CLK_28);
        VGA.VGA_SetClock(2,32400);
        VGA.VGA_SetClock(3,35900);
        VGA.VGA_SetClock(4,39900);
        VGA.VGA_SetClock(5,44700);
        VGA.VGA_SetClock(6,31400);
        VGA.VGA_SetClock(7,37500);
        VGA.VGA_SetClock(8,50000);
        VGA.VGA_SetClock(9,56500);
        VGA.VGA_SetClock(10,64900);
        VGA.VGA_SetClock(11,71900);
        VGA.VGA_SetClock(12,79900);
        VGA.VGA_SetClock(13,89600);
        VGA.VGA_SetClock(14,62800);
        VGA.VGA_SetClock(15,74800);

        IoHandler.IO_RegisterReadHandler(0x3cd,read_p3cd_et4k,IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3cd,io_write_p3cd_et4k,IoHandler.IO_MB);

        // Default to 1M of VRAM
        if (VGA.vga.vmemsize == 0)
            VGA.vga.vmemsize = 1024*1024;

        if (VGA.vga.vmemsize < 512*1024)
            VGA.vga.vmemsize = 256*1024;
        else if (VGA.vga.vmemsize < 1024*1024)
            VGA.vga.vmemsize = 512*1024;
        else
            VGA.vga.vmemsize = 1024*1024;

        // Tseng ROM signature
        /*PhysPt*/int rom_base=(int)Memory.PhysMake(0xc000,0);
        Memory.phys_writeb(rom_base+0x0075,' ');
        Memory.phys_writeb(rom_base+0x0076,'T');
        Memory.phys_writeb(rom_base+0x0077,'s');
        Memory.phys_writeb(rom_base+0x0078,'e');
        Memory.phys_writeb(rom_base+0x0079,'n');
        Memory.phys_writeb(rom_base+0x007a,'g');
        Memory.phys_writeb(rom_base+0x007b,' ');
    }


    // Tseng ET3K implementation
    private static class SVGA_ET3K_DATA {
    // Stored exact values of some registers. Documentation only specifies some /*Bits*/int but hardware checks may
    // expect other /*Bits*/int to be preserved.
        public /*Bitu*/int store_3d4_1b;
        public /*Bitu*/int store_3d4_1c;
        public /*Bitu*/int store_3d4_1d;
        public /*Bitu*/int store_3d4_1e;
        public /*Bitu*/int store_3d4_1f;
        public /*Bitu*/int store_3d4_20;
        public /*Bitu*/int store_3d4_21;
        public /*Bitu*/int store_3d4_23; // note that 22 is missing
        public /*Bitu*/int store_3d4_24;
        public /*Bitu*/int store_3d4_25;

        public /*Bitu*/int store_3c0_16;
        public /*Bitu*/int store_3c0_17;

        public /*Bitu*/int store_3c4_06;
        public /*Bitu*/int store_3c4_07;

        public /*Bitu*/int[] clockFreq = new int[8];
        public /*Bitu*/int biosMode;
    }

    static private SVGA_ET3K_DATA et3k = new SVGA_ET3K_DATA();

//    #define STORE_ET3K(port, index) \
//        case 0x##index: \
//        et3k.store_##port##_##index = val; \
//            break;
//
//    #define RESTORE_ET3K(port, index) \
//        case 0x##index: \
//            return et3k.store_##port##_##index;

    static private VGA.tWritePort write_p3d5_et3k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            switch(reg) {
            // 3d4 index 1bh-21h: Hardware zoom control registers
            // I am not sure if there was a piece of software that used these.
            // Not implemented and will probably stay this way.
            case 0x1b:
                et3k.store_3d4_1b = val;
                break;
            case 0x1c:
                et3k.store_3d4_1c = val;
                break;
            case 0x1d:
                et3k.store_3d4_1d = val;
                break;
            case 0x1e:
                et3k.store_3d4_1e = val;
                break;
            case 0x1f:
                et3k.store_3d4_1f = val;
                break;
            case 0x20:
                et3k.store_3d4_20 = val;
                break;
            case 0x21:
                et3k.store_3d4_21 = val;
                break;
            case 0x23:
            /*
            3d4h index 23h (R/W): Extended start ET3000
            bit   0  Cursor start address bit 16
                  1  Display start address bit 16
                  2  Zoom start address bit 16
                  7  If set memory address 8 is output on the MBSL pin (allowing access to
                     1MB), if clear the blanking signal is output.
            */
            // Only /*Bits*/int 1 and 2 are supported. Bit 2 is related to hardware zoom, bit 7 is too obscure to be useful
                et3k.store_3d4_23 = val;
                VGA.vga.config.display_start = (VGA.vga.config.display_start & 0xffff) | ((val & 0x02)<<15);
                VGA.vga.config.cursor_start = (VGA.vga.config.cursor_start & 0xffff) | ((val & 0x01)<<16);
                break;


            /*
            3d4h index 24h (R/W): Compatibility Control
            bit   0  Enable Clock Translate if set
                1  Clock Select bit 2. Bits 0-1 are in 3C2h/3CCh.
                2  Enable tri-state for all output pins if set
                3  Enable input A8 of 1MB DRAMs from the INTL output if set
                4  Reserved
                5  Enable external ROM CRTC translation if set
                6  Enable Double Scan and Underline Attribute if set
                7  Enable 6845 compatibility if set.
            */
            // TODO: Some of these may be worth implementing.
            case 0x24:
                et3k.store_3d4_24 = val;
                break;
            case 0x25:
            /*
            3d4h index 25h (R/W): Overflow High
            bit   0  Vertical Blank Start bit 10
                  1  Vertical Total Start bit 10
                  2  Vertical Display End bit 10
                  3  Vertical Sync Start bit 10
                  4  Line Compare bit 10
                  5-6  Reserved
                  7  Vertical Interlace if set
            */
                et3k.store_3d4_25 = val;
                VGA.vga.config.line_compare = (VGA.vga.config.line_compare & 0x3ff) | ((val&0x10)<<6);
            // Abusing s3 ex_ver_overflow field. This is to be cleaned up later.
                {
                    /*Bit8u*/int s3val =
                        ((val & 0x01) << 2) | // vbstart
                        ((val & 0x02) >> 1) | // vtotal
                        ((val & 0x04) >> 1) | // vdispend
                        ((val & 0x08) << 1) | // vsyncstart (?)
                        ((val & 0x10) << 2); // linecomp
                    if (((s3val ^ VGA.vga.s3.ex_ver_overflow) & 0x3)!=0) {
                        VGA.vga.s3.ex_ver_overflow=(short)s3val;
                        VGA.VGA_StartResize();
                    } else VGA.vga.s3.ex_ver_overflow=(short)s3val;
                }
                break;

            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:CRTC:ET3K:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3d5_et3k = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            switch(reg) {
            case 0x1b:
                return et3k.store_3d4_1b;
            case 0x1c:
                return et3k.store_3d4_1c;
            case 0x1d:
                return et3k.store_3d4_1d;
            case 0x1e:
                return et3k.store_3d4_1e;
            case 0x1f:
                return et3k.store_3d4_1f;
            case 0x20:
                return et3k.store_3d4_20;
            case 0x21:
                return et3k.store_3d4_21;
            case 0x23:
                return et3k.store_3d4_23;
            case 0x24:
                return et3k.store_3d4_24;
            case 0x25:
                return et3k.store_3d4_25;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:CRTC:ET3K:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    static private VGA.tWritePort write_p3c5_et3k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            switch(reg) {
            // Both registers deal mostly with hardware zoom which is not implemented. Other /*Bits*/int
            // seem to be useless for emulation with the exception of index 7 bit 4 (font select)
            case 0x06:
                et3k.store_3c4_06 = val;
                break;
            case 0x07:
                et3k.store_3c4_07 = val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:SEQ:ET3K:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3c5_et3k = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            switch(reg) {
            case 0x06:
                return et3k.store_3c4_06;
            case 0x07:
                return et3k.store_3c4_07;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:SEQ:ET3K:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    /*
    3CDh (R/W): Segment Select
    bit 0-2  64k Write bank number
        3-5  64k Read bank number
        6-7  Segment Configuration.
               0  128K segments
               1   64K segments
               2  1M linear memory
    NOTES: 1M linear memory is not supported
    */
    static private IoHandler.IO_WriteHandler write_p3cd_et3k = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.svga.bank_write = (short)(val & 0x07);
            VGA.vga.svga.bank_read = (short)((val>>3) & 0x07);
            VGA.vga.svga.bank_size = (val&0x40)!=0?64*1024:128*1024;
            VGA_memory.VGA_SetupHandlers();
        }
    };

    static private IoHandler.IO_ReadHandler read_p3cd_et3k = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return (VGA.vga.svga.bank_read<<3)|VGA.vga.svga.bank_write|((VGA.vga.svga.bank_size==128*1024)?0:0x40);
        }
    };

    static private VGA.tWritePort write_p3c0_et3k = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
        // See ET4K notes.
            switch(reg) {
            case 0x16:
                et3k.store_3c0_16 = val;
                break;
            case 0x17:
                et3k.store_3c0_17 = val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:ATTR:ET3K:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3c1_et3k = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            switch(reg) {
            case 0x16:
                return et3k.store_3c0_16;
            case 0x17:
                return et3k.store_3c0_17;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:ATTR:ET3K:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    /*
    These ports are used but have little if any effect on emulation:
        3B8h (W):  Display Mode Control Register
        3BFh (R/W): Hercules Compatibility Mode
        3CBh (R/W): PEL Address/Data Wd
        3CEh index 0Dh (R/W): Microsequencer Mode
        3CEh index 0Eh (R/W): Microsequencer Reset
        3d8h (R/W): Display Mode Control
        3D9h (W): Color Select Register
        3dAh (W):  Feature Control Register
        3DEh (W);  AT&T Mode Control Register
    */

    static private /*Bitu*/int get_clock_index_et3k() {
        return ((VGA.vga.misc_output>>2)&3) | ((et3k.store_3d4_24<<1)&4);
    }

    static private void set_clock_index_et3k(/*Bitu*/int index) {
        // Shortwiring register reads/writes for simplicity
        IoHandler.IO_Write(0x3c2, (VGA.vga.misc_output&~0x0c)|((index&3)<<2));
        et3k.store_3d4_24 = (et3k.store_3d4_24&~0x02)|((index&4)>>1);
    }

    static private VGA.tFinishSetMode FinishSetMode_ET3K = new VGA.tFinishSetMode() {
        public void call(/*Bitu*/int crtc_base, VGA.VGA_ModeExtraData modeData) {
            et3k.biosMode = modeData.modeNo;

            IoHandler.IO_Write(0x3cd, 0x40); // both banks to 0, 64K bank size

            // Tseng ET3K does not have horizontal overflow /*Bits*/int
            // Reinterpret ver_overflow
            /*Bit8u*/int et4k_ver_overflow =
                ((modeData.ver_overflow & 0x01) << 1) | // vtotal10
                ((modeData.ver_overflow & 0x02) << 1) | // vdispend10
                ((modeData.ver_overflow & 0x04) >> 2) | // vbstart10
                ((modeData.ver_overflow & 0x10) >> 1) | // vretrace10 (tseng has vsync start?)
                ((modeData.ver_overflow & 0x40) >> 2);  // line_compare
            IoHandler.IO_Write(crtc_base,0x25);IoHandler.IO_Write(crtc_base+1,et4k_ver_overflow);

            // Clear remaining ext CRTC registers
            for (/*Bitu*/int i=0x16; i<=0x21; i++)
                IoHandler.IO_Write(crtc_base,i);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x23);IoHandler.IO_Write(crtc_base+1,0);
            IoHandler.IO_Write(crtc_base,0x24);IoHandler.IO_Write(crtc_base+1,0);
            // Clear ext SEQ
            IoHandler.IO_Write(0x3c4,0x06);IoHandler.IO_Write(0x3c5,0);
            IoHandler.IO_Write(0x3c4,0x07);IoHandler.IO_Write(0x3c5,0x40); // 0 in this register breaks WHATVGA
            // Clear ext ATTR
            IoHandler.IO_Write(0x3c0,0x16);IoHandler.IO_Write(0x3c0,0);
            IoHandler.IO_Write(0x3c0,0x17);IoHandler.IO_Write(0x3c0,0);

            // Select SVGA clock to get close to 60Hz (not particularly clean implementation)
            if (modeData.modeNo > 0x13) {
                /*Bitu*/int target = modeData.vtotal*8*modeData.htotal*60;
                /*Bitu*/int best = 1;
                /*Bits*/int dist = 100000000;
                for (/*Bitu*/int i=0; i<8; i++) {
                    /*Bits*/int cdiff = Math.abs((/*Bits*/int)(target-et3k.clockFreq[i]));
                    if (cdiff < dist) {
                        best = i;
                        dist = cdiff;
                    }
                }
                set_clock_index_et3k(best);
            }

            if (VGA.svga.determine_mode!=null)
                VGA.svga.determine_mode.call();

            // Verified on functioning (at last!) hardware: Tseng ET3000 is the same as ET4000 when
            // it comes to chain4 architecture
            VGA.vga.config.compatible_chain4 = false;
            VGA.vga.vmemwrap = VGA.vga.vmemsize;

            VGA_memory.VGA_SetupHandlers();
        }
    };

    static private VGA.tDetermineMode DetermineMode_ET3K = new VGA.tDetermineMode() {
        public void call() {
            // Close replica from the base implementation. It will stay here
            // until I figure a way to either distinguish M_VGA and M_LIN8 or
            // merge them.
            if ((VGA.vga.attr.mode_control & 1)!=0) {
                if ((VGA.vga.gfx.mode & 0x40)!=0) VGA.VGA_SetMode((et3k.biosMode<=0x13)?VGA.M_VGA:VGA.M_LIN8); // Ugly...
                else if ((VGA.vga.gfx.mode & 0x20)!=0) VGA.VGA_SetMode(VGA.M_CGA4);
                else if ((VGA.vga.gfx.miscellaneous & 0x0c)==0x0c) VGA.VGA_SetMode(VGA.M_CGA2);
                else VGA.VGA_SetMode((et3k.biosMode<=0x13)?VGA.M_EGA:VGA.M_LIN4);
            } else {
                VGA.VGA_SetMode(VGA.M_TEXT);
            }
        }
    };

    static private VGA.tSetClock SetClock_ET3K = new VGA.tSetClock() {
        public void call(/*Bitu*/int which,/*Bitu*/int target) {
            et3k.clockFreq[which]=1000*target;
            VGA.VGA_StartResize();
        }
    };

    static private VGA.tGetClock GetClock_ET3K = new VGA.tGetClock() {
        public /*Bitu*/int call() {
            return et3k.clockFreq[get_clock_index_et3k()];
        }
    };

    static private VGA.tAcceptsMode AcceptsMode_ET3K = new VGA.tAcceptsMode() {
        public boolean call(/*Bitu*/int mode) {
            return mode <= 0x37 && mode != 0x2f && Int10_modes.VideoModeMemSize(mode) < VGA.vga.vmemsize;
        }
    };

    static public void SVGA_Setup_TsengET3K() {
        VGA.svga.write_p3d5 = write_p3d5_et3k;
        VGA.svga.read_p3d5 = read_p3d5_et3k;
        VGA.svga.write_p3c5 = write_p3c5_et3k;
        VGA.svga.read_p3c5 = read_p3c5_et3k;
        VGA.svga.write_p3c0 = write_p3c0_et3k;
        VGA.svga.read_p3c1 = read_p3c1_et3k;

        VGA.svga.set_video_mode = FinishSetMode_ET3K;
        VGA.svga.determine_mode = DetermineMode_ET3K;
        VGA.svga.set_clock = SetClock_ET3K;
        VGA.svga.get_clock = GetClock_ET3K;
        VGA.svga.accepts_mode = AcceptsMode_ET3K;

        VGA.VGA_SetClock(0,VGA.CLK_25);
        VGA.VGA_SetClock(1,VGA.CLK_28);
        VGA.VGA_SetClock(2,32400);
        VGA.VGA_SetClock(3,35900);
        VGA.VGA_SetClock(4,39900);
        VGA.VGA_SetClock(5,44700);
        VGA.VGA_SetClock(6,31400);
        VGA.VGA_SetClock(7,37500);

        IoHandler.IO_RegisterReadHandler(0x3cd,read_p3cd_et3k,IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3cd,write_p3cd_et3k,IoHandler.IO_MB);

        VGA.vga.vmemsize = 512*1024; // Cannot figure how this was supposed to work on the real card

        // Tseng ROM signature
        /*PhysPt*/int rom_base=(int)Memory.PhysMake(0xc000,0);
        Memory.phys_writeb(rom_base+0x0075,' ');
        Memory.phys_writeb(rom_base+0x0076,'T');
        Memory.phys_writeb(rom_base+0x0077,'s');
        Memory.phys_writeb(rom_base+0x0078,'e');
        Memory.phys_writeb(rom_base+0x0079,'n');
        Memory.phys_writeb(rom_base+0x007a,'g');
        Memory.phys_writeb(rom_base+0x007b,' ');
    }

}
