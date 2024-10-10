package jdos.hardware;

import jdos.Dosbox;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class VGA_crtc {
    public static IoHandler.IO_WriteHandler vga_write_p3d4 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.crtc.index=(short)val;
        }
    };

    public static IoHandler.IO_ReadHandler vga_read_p3d4 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return VGA.vga.crtc.index;
        }
    };

    public static IoHandler.IO_WriteHandler vga_write_p3d5 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
        //	if (VGA.vga.crtc.index)>0x18) LOG_MSG("VGA CRCT write %X to reg %X",val,VGA.vga.crtc.index));
            switch(VGA.vga.crtc.index) {
            case 0x00:	/* Horizontal Total Register */
                if (VGA.vga.crtc.read_only) break;
                VGA.vga.crtc.horizontal_total=(short)val;
                /* 	0-7  Horizontal Total Character Clocks-5 */
                break;
            case 0x01:	/* Horizontal Display End Register */
                if (VGA.vga.crtc.read_only) break;
                if (val != (VGA.vga.crtc.horizontal_display_end & 0xFF)) {
                    VGA.vga.crtc.horizontal_display_end=(byte)val;
                    VGA.VGA_StartResize();
                }
                /* 	0-7  Number of Character Clocks Displayed -1 */
                break;
            case 0x02:	/* Start Horizontal Blanking Register */
                if (VGA.vga.crtc.read_only) break;
                VGA.vga.crtc.start_horizontal_blanking=(short)val;
                /*	0-7  The count at which Horizontal Blanking starts */
                break;
            case 0x03:	/* End Horizontal Blanking Register */
                if (VGA.vga.crtc.read_only) break;
                VGA.vga.crtc.end_horizontal_blanking=(short)val;
                /*
                    0-4	Horizontal Blanking ends when the last 6 bits of the character
                        counter equals this field. Bit 5 is at 3d4h index 5 bit 7.
                    5-6	Number of character clocks to delay start of display after Horizontal
                        Total has been reached.
                    7	Access to Vertical Retrace registers if set. If clear reads to 3d4h
                        index 10h and 11h access the Lightpen read back registers ??
                */
                break;
            case 0x04:	/* Start Horizontal Retrace Register */
                if (VGA.vga.crtc.read_only) break;
                VGA.vga.crtc.start_horizontal_retrace=(short)val;
                /*	0-7  Horizontal Retrace starts when the Character Counter reaches this value. */
                break;
            case 0x05:	/* End Horizontal Retrace Register */
                if (VGA.vga.crtc.read_only) break;
                VGA.vga.crtc.end_horizontal_retrace=(short)val;
                /*
                    0-4	Horizontal Retrace ends when the last 5 bits of the character counter
                        equals this value.
                    5-6	Number of character clocks to delay start of display after Horizontal
                        Retrace.
                    7	bit 5 of the End Horizontal Blanking count (See 3d4h index 3 bit 0-4)
                */
                break;
            case 0x06: /* Vertical Total Register */
                if (VGA.vga.crtc.read_only) break;
                if (val != VGA.vga.crtc.vertical_total) {
                    VGA.vga.crtc.vertical_total=(short)val;
                    VGA.VGA_StartResize();
                }
                /*	0-7	Lower 8 bits of the Vertical Total. Bit 8 is found in 3d4h index 7
                        bit 0. Bit 9 is found in 3d4h index 7 bit 5.
                    Note: For the VGA this value is the number of scan lines in the display -2.
                */
                break;
            case 0x07:	/* Overflow Register */
                //Line compare bit ignores read only */
                VGA.vga.config.line_compare=(VGA.vga.config.line_compare & 0x6ff) | (val & 0x10) << 4;
                if (VGA.vga.crtc.read_only) break;
                if ((((VGA.vga.crtc.overflow & 0xFF) ^ val) & 0xd6)!=0) {
                    VGA.vga.crtc.overflow=(byte)val;
                    VGA.VGA_StartResize();
                } else VGA.vga.crtc.overflow=(byte)val;
                /*
                    0  Bit 8 of Vertical Total (3d4h index 6)
                    1  Bit 8 of Vertical Display End (3d4h index 12h)
                    2  Bit 8 of Vertical Retrace Start (3d4h index 10h)
                    3  Bit 8 of Start Vertical Blanking (3d4h index 15h)
                    4  Bit 8 of Line Compare Register (3d4h index 18h)
                    5  Bit 9 of Vertical Total (3d4h index 6)
                    6  Bit 9 of Vertical Display End (3d4h index 12h)
                    7  Bit 9 of Vertical Retrace Start (3d4h index 10h)
                */
                break;
            case 0x08:	/* Preset Row Scan Register */
                VGA.vga.crtc.preset_row_scan=(short)val;
                VGA.vga.config.hlines_skip=(short)(val&31);
                if (Dosbox.IS_VGA_ARCH()) VGA.vga.config.bytes_skip=(short)((val>>5)&3);
                else VGA.vga.config.bytes_skip=0;
        //		LOG_DEBUG("Skip lines %d bytes %d",vga.config.hlines_skip,vga.config.bytes_skip);
                /*
                    0-4	Number of lines we have scrolled down in the first character row.
                        Provides Smooth Vertical Scrolling.b
                    5-6	Number of bytes to skip at the start of scanline. Provides Smooth
                        Horizontal Scrolling together with the Horizontal Panning Register
                        (3C0h index 13h).
                */
                break;
            case 0x09: /* Maximum Scan Line Register */
                if (Dosbox.IS_VGA_ARCH())
                    VGA.vga.config.line_compare=(VGA.vga.config.line_compare & 0x5ff)|(val&0x40)<<3;

                if (Dosbox.IS_VGA_ARCH() && (Dosbox.svgaCard== SVGACards.SVGA_None) && (VGA.vga.mode==VGA.M_EGA || VGA.vga.mode==VGA.M_VGA)) {
                    // in vgaonly mode we take special care of line repeats (excluding CGA modes)
                    if (((VGA.vga.crtc.maximum_scan_line ^ val) & 0x20)!=0) {
                        VGA.vga.crtc.maximum_scan_line=(byte)val;
                        VGA.VGA_StartResize();
                    } else {
                        VGA.vga.crtc.maximum_scan_line=(byte)val;
                    }
                    VGA.vga.draw.address_line_total = (val &0x1F) + 1;
                    if ((val&0x80)!=0) VGA.vga.draw.address_line_total*=2;
                } else {
                    if (((VGA.vga.crtc.maximum_scan_line ^ val) & 0xbf)!=0) {
                        VGA.vga.crtc.maximum_scan_line=(byte)val;
                        VGA.VGA_StartResize();
                    } else {
                        VGA.vga.crtc.maximum_scan_line=(byte)val;
                    }
                }
                /*
                    0-4	Number of scan lines in a character row -1. In graphics modes this is
                        the number of times (-1) the line is displayed before passing on to
                        the next line (0: normal, 1: double, 2: triple...).
                        This is independent of bit 7, except in CGA modes which seems to
                        require this field to be 1 and bit 7 to be set to work.
                    5	Bit 9 of Start Vertical Blanking
                    6	Bit 9 of Line Compare Register
                    7	Doubles each scan line if set. I.e. displays 200 lines on a 400 display.
                */
                break;
            case 0x0A:	/* Cursor Start Register */
                VGA.vga.crtc.cursor_start=(short)val;
                VGA.vga.draw.cursor.sline=(short)(val&0x1f);
                if (Dosbox.IS_VGA_ARCH()) VGA.vga.draw.cursor.enabled=(val&0x20)==0;
                else VGA.vga.draw.cursor.enabled=true;
                /*
                    0-4	First scanline of cursor within character.
                    5	Turns Cursor off if set
                */
                break;
            case 0x0B:	/* Cursor End Register */
                VGA.vga.crtc.cursor_end=(short)val;
                VGA.vga.draw.cursor.eline=(short)(val&0x1f);
                VGA.vga.draw.cursor.delay=(short)((val>>5)&0x3);

                /*
                    0-4	Last scanline of cursor within character
                    5-6	Delay of cursor data in character clocks.
                */
                break;
            case 0x0C:	/* Start Address High Register */
                VGA.vga.crtc.start_address_high=(short)val;
                VGA.vga.config.display_start=(VGA.vga.config.display_start & 0xFF00FF)| (val << 8);
                /* 0-7  Upper 8 bits of the start address of the display buffer */
                break;
            case 0x0D:	/* Start Address Low Register */
                VGA.vga.crtc.start_address_low=(short)val;
                VGA.vga.config.display_start=(VGA.vga.config.display_start & 0xFFFF00)| val;
                /*	0-7	Lower 8 bits of the start address of the display buffer */
                break;
            case 0x0E:	/*Cursor Location High Register */
                VGA.vga.crtc.cursor_location_high=(short)val;
                VGA.vga.config.cursor_start&=0xff00ff;
                VGA.vga.config.cursor_start|=val << 8;
                /*	0-7  Upper 8 bits of the address of the cursor */
                break;
            case 0x0F:	/* Cursor Location Low Register */
        //TODO update cursor on screen
                VGA.vga.crtc.cursor_location_low=(short)val;
                VGA.vga.config.cursor_start&=0xffff00;
                VGA.vga.config.cursor_start|=val;
                /*	0-7  Lower 8 bits of the address of the cursor */
                break;
            case 0x10:	/* Vertical Retrace Start Register */
                VGA.vga.crtc.vertical_retrace_start=(short)val;
                /*
                    0-7	Lower 8 bits of Vertical Retrace Start. Vertical Retrace starts when
                    the line counter reaches this value. Bit 8 is found in 3d4h index 7
                    bit 2. Bit 9 is found in 3d4h index 7 bit 7.
                */
                break;
            case 0x11:	/* Vertical Retrace End Register */
                VGA.vga.crtc.vertical_retrace_end=(short)val;

                if (Dosbox.IS_EGAVGA_ARCH() && (val & 0x10)==0) {
                    VGA.vga.draw.vret_triggered=false;
                    if (Dosbox.machine== MachineType.MCH_EGA) Pic.PIC_DeActivateIRQ(9);
                }
                if (Dosbox.IS_VGA_ARCH()) VGA.vga.crtc.read_only=(val & 128)>0;
                else VGA.vga.crtc.read_only=false;
                /*
                    0-3	Vertical Retrace ends when the last 4 bits of the line counter equals
                        this value.
                    4	if clear Clears pending Vertical Interrupts.
                    5	Vertical Interrupts (IRQ 2) disabled if set. Can usually be left
                        disabled, but some systems (including PS/2) require it to be enabled.
                    6	If set selects 5 refresh cycles per scanline rather than 3.
                    7	Disables writing to registers 0-7 if set 3d4h index 7 bit 4 is not
                        affected by this bit.
                */
                break;
            case 0x12:	/* Vertical Display End Register */
                if (val!=VGA.vga.crtc.vertical_display_end) {
                    if (Math.abs(val-VGA.vga.crtc.vertical_display_end)<3) {
                        // delay small vde changes a bit to avoid screen resizing
                        // if they are reverted in a short timeframe
                        Pic.PIC_RemoveEvents(VGA_draw.VGA_SetupDrawing);
                        VGA.vga.draw.resizing=false;
                        VGA.vga.crtc.vertical_display_end=(short)val;
                        VGA.VGA_StartResize(150);
                    } else {
                        VGA.vga.crtc.vertical_display_end=(short)val;
                        VGA.VGA_StartResize();
                    }
                }
                /*
                    0-7	Lower 8 bits of Vertical Display End. The display ends when the line
                        counter reaches this value. Bit 8 is found in 3d4h index 7 bit 1.
                    Bit 9 is found in 3d4h index 7 bit 6.
                */
                break;
            case 0x13:	/* Offset register */
                VGA.vga.crtc.offset=(byte)val;
                VGA.vga.config.scan_len&=0x300;
                VGA.vga.config.scan_len|=val;
                VGA_draw.VGA_CheckScanLength();
                /*
                    0-7	Number of bytes in a scanline / K. Where K is 2 for byte mode, 4 for
                        word mode and 8 for Double Word mode.
                */
                break;
            case 0x14:	/* Underline Location Register */
                VGA.vga.crtc.underline_location=(short)val;
                if (Dosbox.IS_VGA_ARCH()) {
                    //Byte,word,dword mode
                    if ((VGA.vga.crtc.underline_location & 0x20)!=0)
                        VGA.vga.config.addr_shift = 2;
                    else if ((VGA.vga.crtc. mode_control & 0x40)!=0)
                        VGA.vga.config.addr_shift = 0;
                    else
                        VGA.vga.config.addr_shift = 1;
                } else {
                    VGA.vga.config.addr_shift = 1;
                }
                /*
                    0-4	Position of underline within Character cell.
                    5	If set memory address is only changed every fourth character clock.
                    6	Double Word mode addressing if set
                */
                break;
            case 0x15:	/* Start Vertical Blank Register */
                if (val!=VGA.vga.crtc.start_vertical_blanking) {
                    VGA.vga.crtc.start_vertical_blanking=(short)val;
                    VGA.VGA_StartResize();
                }
                /*
                    0-7	Lower 8 bits of Vertical Blank Start. Vertical blanking starts when
                        the line counter reaches this value. Bit 8 is found in 3d4h index 7
                        bit 3.
                */
                break;
            case 0x16:	/*  End Vertical Blank Register */
                if (val!=VGA.vga.crtc.end_vertical_blanking) {
                    VGA.vga.crtc.end_vertical_blanking=(short)val;
                    VGA.VGA_StartResize();
                }
                /*
                    0-6	Vertical blanking stops when the lower 7 bits of the line counter
                        equals this field. Some SVGA chips uses all 8 bits!
                        IBM actually says bits 0-7.
                */
                break;
            case 0x17:	/* Mode Control Register */
                VGA.vga.crtc.mode_control=(short)val;
                VGA.vga.tandy.line_mask = (short)((~val) & 3);
                //Byte,word,dword mode
                if ((VGA.vga.crtc.underline_location & 0x20)!=0)
                    VGA.vga.config.addr_shift = 2;
                else if ((VGA.vga.crtc. mode_control & 0x40)!=0)
                    VGA.vga.config.addr_shift = 0;
                else
                    VGA.vga.config.addr_shift = 1;

                if ( VGA.vga.tandy.line_mask!=0 ) {
                    VGA.vga.tandy.line_shift = 13;
                    VGA.vga.tandy.addr_mask = (1 << 13) - 1;
                } else {
                    VGA.vga.tandy.addr_mask = ~0;
                    VGA.vga.tandy.line_shift = 0;
                }
                //Should we really need to do a determinemode here?
        //		VGA_DetermineMode();
                /*
                    0	If clear use CGA compatible memory addressing system
                        by substituting character row scan counter bit 0 for address bit 13,
                        thus creating 2 banks for even and odd scan lines.
                    1	If clear use Hercules compatible memory addressing system by
                        substituting character row scan counter bit 1 for address bit 14,
                        thus creating 4 banks.
                    2	If set increase scan line counter only every second line.
                    3	If set increase memory address counter only every other character clock.
                    5	When in Word Mode bit 15 is rotated to bit 0 if this bit is set else
                        bit 13 is rotated into bit 0.
                    6	If clear system is in word mode. Addresses are rotated 1 position up
                        bringing either bit 13 or 15 into bit 0.
                    7	Clearing this bit will reset the display system until the bit is set again.
                */
                break;
            case 0x18:	/* Line Compare Register */
                VGA.vga.crtc.line_compare=(short)val;
                VGA.vga.config.line_compare=(VGA.vga.config.line_compare & 0x700) | val;
                /*
                    0-7	Lower 8 bits of the Line Compare. When the Line counter reaches this
                        value, the display address wraps to 0. Provides Split Screen
                        facilities. Bit 8 is found in 3d4h index 7 bit 4.
                        Bit 9 is found in 3d4h index 9 bit 6.
                */
                break;
            default:
                if (VGA.svga.write_p3d5!=null) {
                    VGA.svga.write_p3d5.call(VGA.vga.crtc.index, val, iolen);
                } else {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:CRTC:Write to unknown index "+Integer.toString(VGA.vga.crtc.index,16));
                }
                break;
            }
        }
    };

    static public IoHandler.IO_ReadHandler vga_read_p3d5 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
        //	LOG_MSG("VGA CRCT read from reg %X",VGA.vga.crtc.index));
            switch(VGA.vga.crtc.index) {
            case 0x00:	/* Horizontal Total Register */
                return VGA.vga.crtc.horizontal_total;
            case 0x01:	/* Horizontal Display End Register */
                return VGA.vga.crtc.horizontal_display_end & 0xFF;
            case 0x02:	/* Start Horizontal Blanking Register */
                return VGA.vga.crtc.start_horizontal_blanking;
            case 0x03:	/* End Horizontal Blanking Register */
                return VGA.vga.crtc.end_horizontal_blanking;
            case 0x04:	/* Start Horizontal Retrace Register */
                return VGA.vga.crtc.start_horizontal_retrace;
            case 0x05:	/* End Horizontal Retrace Register */
                return VGA.vga.crtc.end_horizontal_retrace;
            case 0x06: /* Vertical Total Register */
                return VGA.vga.crtc.vertical_total;
            case 0x07:	/* Overflow Register */
                return VGA.vga.crtc.overflow & 0xFF;
            case 0x08:	/* Preset Row Scan Register */
                return VGA.vga.crtc.preset_row_scan;
            case 0x09: /* Maximum Scan Line Register */
                return VGA.vga.crtc.maximum_scan_line & 0xFF;
            case 0x0A:	/* Cursor Start Register */
                return VGA.vga.crtc.cursor_start;
            case 0x0B:	/* Cursor End Register */
                return VGA.vga.crtc.cursor_end;
            case 0x0C:	/* Start Address High Register */
                return VGA.vga.crtc.start_address_high;
            case 0x0D:	/* Start Address Low Register */
                return VGA.vga.crtc.start_address_low;
            case 0x0E:	/*Cursor Location High Register */
                return VGA.vga.crtc.cursor_location_high;
            case 0x0F:	/* Cursor Location Low Register */
                return VGA.vga.crtc.cursor_location_low;
            case 0x10:	/* Vertical Retrace Start Register */
                return VGA.vga.crtc.vertical_retrace_start;
            case 0x11:	/* Vertical Retrace End Register */
                return VGA.vga.crtc.vertical_retrace_end;
            case 0x12:	/* Vertical Display End Register */
                return VGA.vga.crtc.vertical_display_end;
            case 0x13:	/* Offset register */
                return VGA.vga.crtc.offset & 0xFF;
            case 0x14:	/* Underline Location Register */
                return VGA.vga.crtc.underline_location;
            case 0x15:	/* Start Vertical Blank Register */
                return VGA.vga.crtc.start_vertical_blanking;
            case 0x16:	/*  End Vertical Blank Register */
                return VGA.vga.crtc.end_vertical_blanking;
            case 0x17:	/* Mode Control Register */
                return VGA.vga.crtc.mode_control;
            case 0x18:	/* Line Compare Register */
                return VGA.vga.crtc.line_compare;
            default:
                if (VGA.svga.read_p3d5!=null) {
                    return VGA.svga.read_p3d5.call(VGA.vga.crtc.index, iolen);
                } else {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:CRTC:Read from unknown index "+Integer.toString(VGA.vga.crtc.index,16));
                    return 0x0;
                }
            }
        }
    };
}
