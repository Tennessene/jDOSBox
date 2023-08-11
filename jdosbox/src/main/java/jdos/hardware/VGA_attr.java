package jdos.hardware;

import jdos.Dosbox;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class VGA_attr {
    public static void VGA_ATTR_SetPalette(/*Bit8u*/int index,/*Bit8u*/int val) {
        VGA.vga.attr.palette[index] = (short)val;
        if ((VGA.vga.attr.mode_control & 0x80)!=0) val = (val&0xf) | (VGA.vga.attr.color_select << 4);
        val &= 63;
        val |= (VGA.vga.attr.color_select & 0xc) << 4;
        if (Dosbox.machine== MachineType.MCH_EGA) {
            if ((VGA.vga.crtc.vertical_total | ((VGA.vga.crtc.overflow & 1) << 8)) == 260) {
                // check for intensity bit
                if ((val&0x10)!=0) val|=0x38;
                else {
                    val&=0x7;
                    // check for special brown
                    if (val==6) val=0x14;
                }
            }
        }
        VGA_dac.VGA_DAC_CombineColor(index,val);
    }

    private static IoHandler.IO_ReadHandler read_p3c0 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            // Wcharts, Win 3.11 & 95 SVGA
            /*Bitu*/int retval = VGA.vga.attr.index & 0x1f;
            if (VGA.vga.attr.disabled==0) retval |= 0x20;
            return retval;
        }
    };

    private static IoHandler.IO_WriteHandler write_p3c0 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if (!VGA.vga.internal.attrindex) {
                VGA.vga.attr.index=(short)(val & 0x1F);
                VGA.vga.internal.attrindex=true;
                if ((val & 0x20)!=0) VGA.vga.attr.disabled &= ~1;
                else VGA.vga.attr.disabled  |= 1;
                /*
                    0-4	Address of data register to write to port 3C0h or read from port 3C1h
                    5	If set screen output is enabled and the palette can not be modified,
                        if clear screen output is disabled and the palette can be modified.
                */
                return;
            } else {
                VGA.vga.internal.attrindex=false;
                switch (VGA.vga.attr.index) {
                    /* Palette */
                case 0x00:		case 0x01:		case 0x02:		case 0x03:
                case 0x04:		case 0x05:		case 0x06:		case 0x07:
                case 0x08:		case 0x09:		case 0x0a:		case 0x0b:
                case 0x0c:		case 0x0d:		case 0x0e:		case 0x0f:
                    if ((VGA.vga.attr.disabled & 0x1)!=0) VGA_ATTR_SetPalette(VGA.vga.attr.index,(/*Bit8u*/short)val);
                    /*
                        0-5	Index into the 256 color DAC table. May be modified by 3C0h index
                        10h and 14h.
                    */
                    break;
                case 0x10: { /* Mode Control Register */
                    if (!Dosbox.IS_VGA_ARCH()) val&=0x1f;	// not really correct, but should do it
                    /*Bitu*/int difference = VGA.vga.attr.mode_control^val;
                    VGA.vga.attr.mode_control=(short)val;

                    if ((difference & 0x80)!=0) {
                        for (/*Bit8u*/int i=0;i<0x10;i++)
                            VGA_ATTR_SetPalette(i,VGA.vga.attr.palette[i]);
                    }
                    if ((difference & 0x08)!=0)
                        VGA_draw.VGA_SetBlinking(val & 0x8);

                    if ((difference & 0x41)!=0)
                        VGA.VGA_DetermineMode();

                    if ((difference & 0x04)!=0) {
                        // recompute the panning value
                        if(VGA.vga.mode==VGA.M_TEXT) {
                            /*Bit8u*/int pan_reg = VGA.vga.attr.horizontal_pel_panning;
                            if (pan_reg > 7)
                                VGA.vga.config.pel_panning=0;
                            else if ((val & 0x4)!=0) // 9-dot wide characters
                                VGA.vga.config.pel_panning=(short)(pan_reg+1);
                            else // 8-dot characters
                                VGA.vga.config.pel_panning=(short)pan_reg;
                        }
                    }

                    /*
                        0	Graphics mode if set, Alphanumeric mode else.
                        1	Monochrome mode if set, color mode else.
                        2	9-bit wide characters if set.
                            The 9th bit of characters C0h-DFh will be the same as
                            the 8th bit. Otherwise it will be the background color.
                        3	If set Attribute bit 7 is blinking, else high intensity.
                        5	If set the PEL panning register (3C0h index 13h) is temporarily set
                            to 0 from when the line compare causes a wrap around until the next
                            vertical retrace when the register is automatically reloaded with
                            the old value, else the PEL panning register ignores line compares.
                        6	If set pixels are 8 bits wide. Used in 256 color modes.
                        7	If set bit 4-5 of the index into the DAC table are taken from port
                            3C0h index 14h bit 0-1, else the bits in the palette register are
                            used.
                    */
                    break;
                }
                case 0x11:	/* Overscan Color Register */
                    VGA.vga.attr.overscan_color=(/*Bit8u*/short)val;
                    /* 0-5  Color of screen border. Color is defined as in the palette registers. */
                    break;
                case 0x12:	/* Color Plane Enable Register */
                    /* Why disable colour planes? */
                    VGA.vga.attr.color_plane_enable=(/*Bit8u*/short)val;
                    /*
                        0	Bit plane 0 is enabled if set.
                        1	Bit plane 1 is enabled if set.
                        2	Bit plane 2 is enabled if set.
                        3	Bit plane 3 is enabled if set.
                        4-5	Video Status MUX. Diagnostics use only.
                            Two attribute bits appear on bits 4 and 5 of the Input Status
                            Register 1 (3dAh). 0: Bit 2/0, 1: Bit 5/4, 2: bit 3/1, 3: bit 7/6
                    */
                    break;
                case 0x13:	/* Horizontal PEL Panning Register */
                    VGA.vga.attr.horizontal_pel_panning=(short)(val & 0xF);
                    switch (VGA.vga.mode) {
                    case VGA.M_TEXT:
                        if ((val==0x7) && (Dosbox.svgaCard==SVGACards.SVGA_None)) VGA.vga.config.pel_panning=7;
                        if (val>0x7) VGA.vga.config.pel_panning=0;
                        else VGA.vga.config.pel_panning=(/*Bit8u*/short)(val+1);
                        break;
                    case VGA.M_VGA:
                    case VGA.M_LIN8:
                        VGA.vga.config.pel_panning=(short)((val & 0x7)/2);
                        break;
                    case VGA.M_LIN16:
                    default:
                        VGA.vga.config.pel_panning=(short)((val & 0x7));
                    }
                    if (Dosbox.machine==MachineType.MCH_EGA) {
				        // On the EGA panning can be programmed for every scanline:
				        VGA.vga.draw.panning = VGA.vga.config.pel_panning;
                    }
                    /*
                        0-3	Indicates number of pixels to shift the display left
                            Value  9bit textmode   256color mode   Other modes
                            0          1               0              0
                            1          2              n/a             1
                            2          3               1              2
                            3          4              n/a             3
                            4          5               2              4
                            5          6              n/a             5
                            6          7               3              6
                            7          8              n/a             7
                            8          0              n/a            n/a
                    */
                    break;
                case 0x14:	/* Color Select Register */
                    if (!Dosbox.IS_VGA_ARCH()) {
                        VGA.vga.attr.color_select=0;
                        break;
                    }
                    if ((VGA.vga.attr.color_select ^ val)!=0) {
                        VGA.vga.attr.color_select=(/*Bit8u*/short)val;
                        for (/*Bit8u*/short i=0;i<0x10;i++) {
                            VGA_ATTR_SetPalette(i,VGA.vga.attr.palette[i]);
                        }
                    }
                    /*
                        0-1	If 3C0h index 10h bit 7 is set these 2 bits are used as bits 4-5 of
                            the index into the DAC table.
                        2-3	These 2 bits are used as bit 6-7 of the index into the DAC table
                            except in 256 color mode.
                            Note: this register does not affect 256 color modes.
                    */
                    break;
                default:
                    if (VGA.svga.write_p3c0!=null) {
                        VGA.svga.write_p3c0.call(VGA.vga.attr.index, val, iolen);
                        break;
                    }
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:ATTR:Write to unkown Index "+Integer.toString(VGA.vga.attr.index,16));
                    break;
                }
            }
        }
    };

    private static IoHandler.IO_ReadHandler read_p3c1 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
        //	vga.internal.attrindex=false;
            switch (VGA.vga.attr.index) {
                    /* Palette */
            case 0x00:		case 0x01:		case 0x02:		case 0x03:
            case 0x04:		case 0x05:		case 0x06:		case 0x07:
            case 0x08:		case 0x09:		case 0x0a:		case 0x0b:
            case 0x0c:		case 0x0d:		case 0x0e:		case 0x0f:
                return VGA.vga.attr.palette[VGA.vga.attr.index];
            case 0x10: /* Mode Control Register */
                return VGA.vga.attr.mode_control;
            case 0x11:	/* Overscan Color Register */
                return VGA.vga.attr.overscan_color;
            case 0x12:	/* Color Plane Enable Register */
                return VGA.vga.attr.color_plane_enable;
            case 0x13:	/* Horizontal PEL Panning Register */
                return VGA.vga.attr.horizontal_pel_panning;
            case 0x14:	/* Color Select Register */
                return VGA.vga.attr.color_select;
            default:
                if (VGA.svga.read_p3c1!=null)
                    return VGA.svga.read_p3c1.call(VGA.vga.attr.index, iolen);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:ATTR:Read from unkown Index "+Integer.toString(VGA.vga.attr.index,16));
            }
            return 0;
        }
    };

    public static void VGA_SetupAttr() {
        if (Dosbox.IS_EGAVGA_ARCH()) {
            IoHandler.IO_RegisterWriteHandler(0x3c0,write_p3c0,IoHandler.IO_MB);
            if (Dosbox.IS_VGA_ARCH()) {
                IoHandler.IO_RegisterReadHandler(0x3c0,read_p3c0,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3c1,read_p3c1,IoHandler.IO_MB);
            }
        }
    }
    
}
