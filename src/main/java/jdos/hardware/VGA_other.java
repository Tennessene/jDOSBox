package jdos.hardware;

import jdos.Dosbox;
import jdos.gui.Mapper;
import jdos.gui.Render;
import jdos.ints.Int10_memory;
import jdos.misc.Log;
import jdos.sdl.JavaMapper;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.Ptr;

public class VGA_other {
    private static IoHandler.IO_WriteHandler write_crtc_index_other = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.other.index=(short)val;
        }
    };

    private static IoHandler.IO_ReadHandler read_crtc_index_other = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return VGA.vga.other.index;
        }
    };

    private static IoHandler.IO_WriteHandler write_crtc_data_other = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (VGA.vga.other.index) {
            case 0x00:		//Horizontal total
                if ((VGA.vga.other.htotal ^ val)!=0) VGA.VGA_StartResize();
                VGA.vga.other.htotal=(/*Bit8u*/short)val;
                break;
            case 0x01:		//Horizontal displayed chars
                if ((VGA.vga.other.hdend ^ val)!=0) VGA.VGA_StartResize();
                VGA.vga.other.hdend=(/*Bit8u*/short)val;
                break;
            case 0x02:		//Horizontal sync position
                VGA.vga.other.hsyncp=(/*Bit8u*/short)val;
                break;
            case 0x03:		//Horizontal sync width
                if (Dosbox.machine== MachineType.MCH_TANDY) VGA.vga.other.vsyncw=(/*Bit8u*/short)(val >> 4);
                else VGA.vga.other.vsyncw = 16; // The MC6845 has a fixed v-sync width of 16 lines
                VGA.vga.other.hsyncw=(/*Bit8u*/short)(val & 0xf);
                break;
            case 0x04:		//Vertical total
                if ((VGA.vga.other.vtotal ^ val)!=0) VGA.VGA_StartResize();
                VGA.vga.other.vtotal=(/*Bit8u*/short)val;
                break;
            case 0x05:		//Vertical display adjust
                if ((VGA.vga.other.vadjust ^ val)!=0) VGA.VGA_StartResize();
                VGA.vga.other.vadjust=(/*Bit8u*/short)val;
                break;
            case 0x06:		//Vertical rows
                if ((VGA.vga.other.vdend ^ val)!=0) VGA.VGA_StartResize();
                VGA.vga.other.vdend=(/*Bit8u*/short)val;
                break;
            case 0x07:		//Vertical sync position
                VGA.vga.other.vsyncp=(/*Bit8u*/short)val;
                break;
            case 0x09:		//Max scanline
                val &= 0x1f; // VGADOC says bit 0-3 but the MC6845 datasheet says bit 0-4
                 if ((VGA.vga.other.max_scanline ^ val)!=0) VGA.VGA_StartResize();
                VGA.vga.other.max_scanline=(/*Bit8u*/short)val;
                break;
            case 0x0A:	/* Cursor Start Register */
                VGA.vga.other.cursor_start = (/*Bit8u*/short)(val & 0x3f);
                VGA.vga.draw.cursor.sline = (/*Bit8u*/short)(val&0x1f);
                VGA.vga.draw.cursor.enabled = ((val & 0x60) != 0x20);
                break;
            case 0x0B:	/* Cursor End Register */
                VGA.vga.other.cursor_end = (/*Bit8u*/short)(val&0x1f);
                VGA.vga.draw.cursor.eline = (/*Bit8u*/short)(val&0x1f);
                break;
            case 0x0C:	/* Start Address High Register */
                // Bit 12 (depending on video mode) and 13 are actually masked too,
		        // but so far no need to implement it.
		        VGA.vga.config.display_start=(VGA.vga.config.display_start & 0x00FF) | ((val&0x3F) << 8);
                break;
            case 0x0D:	/* Start Address Low Register */
                VGA.vga.config.display_start=(VGA.vga.config.display_start & 0xFF00) | val;
                break;
            case 0x0E:	/*Cursor Location High Register */
                VGA.vga.config.cursor_start&=0x00ff;
                VGA.vga.config.cursor_start|=((/*Bit8u*/short)val) << 8;
                break;
            case 0x0F:	/* Cursor Location Low Register */
                VGA.vga.config.cursor_start&=0xff00;
                VGA.vga.config.cursor_start|=(/*Bit8u*/short)val;
                break;
            case 0x10:	/* Light Pen High */
                VGA.vga.other.lightpen &= 0xff;
                VGA.vga.other.lightpen |= (val & 0x3f)<<8;		// only 6 bits
                break;
            case 0x11:	/* Light Pen Low */
                VGA.vga.other.lightpen &= 0xff00;
                VGA.vga.other.lightpen |= (/*Bit8u*/short)val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"MC6845:Write "+Integer.toString(val, 16)+" to illegal index "+Integer.toString(VGA.vga.other.index,16));
            }
        }
    };

    private static IoHandler.IO_ReadHandler read_crtc_data_other = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            switch (VGA.vga.other.index) {
            case 0x00:		//Horizontal total
                return VGA.vga.other.htotal;
            case 0x01:		//Horizontal displayed chars
                return VGA.vga.other.hdend;
            case 0x02:		//Horizontal sync position
                return VGA.vga.other.hsyncp;
            case 0x03:		//Horizontal and vertical sync width
                if (Dosbox.machine==MachineType.MCH_TANDY)
                    return VGA.vga.other.hsyncw | (VGA.vga.other.vsyncw << 4);
                else return VGA.vga.other.hsyncw;
            case 0x04:		//Vertical total
                return VGA.vga.other.vtotal;
            case 0x05:		//Vertical display adjust
                return VGA.vga.other.vadjust;
            case 0x06:		//Vertical rows
                return VGA.vga.other.vdend;
            case 0x07:		//Vertical sync position
                return VGA.vga.other.vsyncp;
            case 0x09:		//Max scanline
                return VGA.vga.other.max_scanline;
            case 0x0A:	/* Cursor Start Register */
                return VGA.vga.other.cursor_start;
            case 0x0B:	/* Cursor End Register */
                return VGA.vga.other.cursor_end;
            case 0x0C:	/* Start Address High Register */
                return (/*Bit8u*/short)(VGA.vga.config.display_start >> 8);
            case 0x0D:	/* Start Address Low Register */
                return (/*Bit8u*/short)(VGA.vga.config.display_start & 0xff);
            case 0x0E:	/*Cursor Location High Register */
                return (/*Bit8u*/short)(VGA.vga.config.cursor_start >> 8);
            case 0x0F:	/* Cursor Location Low Register */
                return (/*Bit8u*/short)(VGA.vga.config.cursor_start & 0xff);
            case 0x10:	/* Light Pen High */
                return (/*Bit8u*/short)(VGA.vga.other.lightpen >> 8);
            case 0x11:	/* Light Pen Low */
                return (/*Bit8u*/short)(VGA.vga.other.lightpen & 0xff);
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"MC6845:Read from illegal index "+Integer.toString(VGA.vga.other.index,16));
            }
            return (/*Bitu*/int)(~0);
        }
    };

    private static IoHandler.IO_WriteHandler write_lightpen = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (port) {
            case 0x3db:	// Clear lightpen latch
                VGA.vga.other.lightpen_triggered = false;
                break;
            case 0x3dc:	// Preset lightpen latch
                if (!VGA.vga.other.lightpen_triggered) {
                    VGA.vga.other.lightpen_triggered = true; // TODO: this shows at port 3ba/3da bit 1

                    double timeInFrame = Pic.PIC_FullIndex()-VGA.vga.draw.delay.framestart;
                    double timeInLine = timeInFrame % VGA.vga.draw.delay.htotal;
                    int current_scanline = (int)(timeInFrame / VGA.vga.draw.delay.htotal);

                    VGA.vga.other.lightpen = ((VGA.vga.draw.address_add/2) * (current_scanline/2));
                    VGA.vga.other.lightpen += (int)((timeInLine / VGA.vga.draw.delay.hdend) * ((float)(VGA.vga.draw.address_add/2)));
                    VGA.vga.other.lightpen &= 0xFFFF;
                }
                break;
            }
        }
    };

    private static double hue_offset = 0.0;
    private static /*Bit8u*/short cga16_val = 0;
    private static /*Bit8u*/short herc_pal = 0;

    private static void cga16_color_select(/*Bit8u*/short val) {
        cga16_val = val;
        update_cga16_color();
    }

    private static void update_cga16_color() {
// Algorithm provided by NewRisingSun
// His/Her algorithm is more complex and gives better results than the one below
// However that algorithm doesn't fit in our vga pallette.
// Therefore a simple variant is used, but the colours are bit lighter.

// It uses an avarage over the bits to give smooth transitions from colour to colour
// This is represented by the j variable. The i variable gives the 16 colours
// The draw handler calculates the needed avarage and combines this with the colour
// to match an entry that is generated here.

        int baseR=0, baseG=0, baseB=0;
        double sinhue,coshue,hue,basehue = 50.0;
        double I,Q,Y,pixelI,pixelQ,R,G,B;
        /*Bitu*/int colorBit1,colorBit2,colorBit3,colorBit4,index;

        if ((cga16_val & 0x01)!=0) baseB += 0xa8;
        if ((cga16_val & 0x02)!=0) baseG += 0xa8;
        if ((cga16_val & 0x04)!=0) baseR += 0xa8;
        if ((cga16_val & 0x08)!=0) { baseR += 0x57; baseG += 0x57; baseB += 0x57; }
        if ((cga16_val & 0x20)!=0) basehue = 35.0;

        hue = (basehue + hue_offset)*0.017453239;
        sinhue = Math.sin(hue);
        coshue = Math.cos(hue);

        for(/*Bitu*/int i = 0; i < 16;i++) {
            for(/*Bitu*/int j = 0;j < 5;j++) {
                index = 0x80|(j << 4)|i; //use upperpart of vga pallette
                colorBit4 = (i&1)>>0;
                colorBit3 = (i&2)>>1;
                colorBit2 = (i&4)>>2;
                colorBit1 = (i&8)>>3;

                //calculate lookup table
                I = 0; Q = 0;
                I += (double) colorBit1;
                Q += (double) colorBit2;
                I -= (double) colorBit3;
                Q -= (double) colorBit4;
                Y  = (double) j / 4.0; //calculated avarage is over 4 bits

                pixelI = I * 1.0 / 3.0; //I* tvSaturnation / 3.0
                pixelQ = Q * 1.0 / 3.0; //Q* tvSaturnation / 3.0
                I = pixelI*coshue + pixelQ*sinhue;
                Q = pixelQ*coshue - pixelI*sinhue;

                R = Y + 0.956*I + 0.621*Q; if (R < 0.0) R = 0.0; if (R > 1.0) R = 1.0;
                G = Y - 0.272*I - 0.647*Q; if (G < 0.0) G = 0.0; if (G > 1.0) G = 1.0;
                B = Y - 1.105*I + 1.702*Q; if (B < 0.0) B = 0.0; if (B > 1.0) B = 1.0;

                Render.RENDER_SetPal(index,(short)(R*baseR),(short)(G*baseG),(short)(B*baseB));
            }
        }
    }

    private static Mapper.MAPPER_Handler IncreaseHue = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed)
                return;
            hue_offset += 5.0;
            update_cga16_color();
            Log.log_msg("Hue at "+hue_offset);
        }
    };

    private static Mapper.MAPPER_Handler DecreaseHue = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed)
                return;
            hue_offset -= 5.0;
            update_cga16_color();
            Log.log_msg("Hue at %f"+hue_offset);
        }
    };

    private static void write_cga_color_select(/*Bitu*/int val) {
        VGA.vga.tandy.color_select=(short)val;
        switch (VGA.vga.mode) {
        case  VGA.M_TANDY4:
            {
                /*Bit8u*/int base = (val & 0x10)!=0 ? 0x08 : 0;
                /*Bit8u*/short bg = (short)(val & 0xf);
                if ((VGA.vga.tandy.mode_control & 0x4)!=0)	// cyan red white
                    VGA.VGA_SetCGA4Table(bg, 3+base, 4+base, 7+base);
                else if ((val & 0x20)!=0)				// cyan magenta white
                    VGA.VGA_SetCGA4Table(bg, 3+base, 5+base, 7+base);
                else								// green red brown
                    VGA.VGA_SetCGA4Table(bg, 2+base, 4+base, 6+base);
                VGA.vga.tandy.border_color = bg;
                VGA.vga.attr.overscan_color = bg;
                break;
            }
        case VGA.M_TANDY2:
            VGA.VGA_SetCGA2Table((short)0,(short)(val & 0xf));
            VGA.vga.attr.overscan_color = 0;
            break;
        case VGA.M_CGA16:
            cga16_color_select((short)val);
            break;
        case VGA.M_TEXT:
            VGA.vga.tandy.border_color = (short)(val & 0xf);
		    VGA.vga.attr.overscan_color = 0;
            break;
        }
    }

    private static IoHandler.IO_WriteHandler write_cga = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (port) {
            case 0x3d8:
                VGA.vga.tandy.mode_control=(short)val;
                VGA.vga.attr.disabled = (byte)((val&0x8)!=0? 0: 1);
                if ((VGA.vga.tandy.mode_control & 0x2)!=0) {		// graphics mode
                    if ((VGA.vga.tandy.mode_control & 0x10)!=0) {// highres mode
                        if ((val & 0x4)==0) {				// burst on
                            VGA.VGA_SetMode(VGA.M_CGA16);		// composite ntsc 160x200 16 color mode
                        } else {
                            VGA.VGA_SetMode(VGA.M_TANDY2);
                        }
                    } else VGA.VGA_SetMode(VGA.M_TANDY4);		// lowres mode

                    write_cga_color_select(VGA.vga.tandy.color_select);
                } else {
                    VGA.VGA_SetMode(VGA.M_TANDY_TEXT);
                }
                VGA_draw.VGA_SetBlinking(val & 0x20);
                break;
            case 0x3d9: // color select
                write_cga_color_select(val);
                break;
            }
        }
    };

    private static void tandy_update_palette() {
        // TODO mask off bits if needed
        if (Dosbox.machine == MachineType.MCH_TANDY) {
            switch (VGA.vga.mode) {
            case VGA.M_TANDY2:
                VGA.VGA_SetCGA2Table(VGA.vga.attr.palette[0],
                    //vga.attr.palette[vga.tandy.color_select&0xf]);
                    VGA.vga.attr.palette[0xf]);
                //VGA_SetCGA2Table(vga.attr.palette[0xf],vga.attr.palette[0]);
                break;
            case VGA.M_TANDY4:
                if ((VGA.vga.tandy.gfx_control & 0x8)!=0) {
                    // 4-color high resolution - might be an idea to introduce M_TANDY4H
                    VGA.VGA_SetCGA4Table( // function sets both medium and highres 4color tables
                        VGA.vga.attr.palette[0], VGA.vga.attr.palette[1],
                        VGA.vga.attr.palette[2], VGA.vga.attr.palette[3]);
                } else {
                    int color_set = 0;
                    int r_mask = 0xf;
                    if ((VGA.vga.tandy.color_select & 0x10)!=0) color_set |= 8; // intensity
                    if ((VGA.vga.tandy.color_select & 0x20)!=0) color_set |= 1; // Cyan Mag. White
                    if ((VGA.vga.tandy.mode_control & 0x04)!=0) {			// Cyan Red White
                        color_set |= 1;
                        r_mask &= ~1;
                    }
                    VGA.VGA_SetCGA4Table(
                        VGA.vga.attr.palette[0],
                        VGA.vga.attr.palette[(2|color_set)& VGA.vga.tandy.palette_mask],
                        VGA.vga.attr.palette[(4|(color_set& r_mask))& VGA.vga.tandy.palette_mask],
                        VGA.vga.attr.palette[(6|color_set)& VGA.vga.tandy.palette_mask]);
                }
                break;
            default:
                break;
            }
        } else {
            // PCJr
            switch (VGA.vga.mode) {
            case VGA.M_TANDY2:
                VGA.VGA_SetCGA2Table(VGA.vga.attr.palette[0],VGA.vga.attr.palette[1]);
                break;
            case VGA.M_TANDY4:
                VGA.VGA_SetCGA4Table(
                    VGA.vga.attr.palette[0], VGA.vga.attr.palette[1],
                    VGA.vga.attr.palette[2], VGA.vga.attr.palette[3]);
                break;
            default:
                break;
            }
        }
    }

    private static void TANDY_FindMode() {
        if ((VGA.vga.tandy.mode_control & 0x2)!=0) {
            if ((VGA.vga.tandy.gfx_control & 0x10)!=0) {
                if (VGA.vga.mode==VGA.M_TANDY4) {
                    VGA.VGA_SetModeNow(VGA.M_TANDY16);
                } else VGA.VGA_SetMode(VGA.M_TANDY16);
            }
            else if ((VGA.vga.tandy.gfx_control & 0x08)!=0) {
                VGA.VGA_SetMode(VGA.M_TANDY4);
            }
            else if ((VGA.vga.tandy.mode_control & 0x10)!=0)
                VGA.VGA_SetMode(VGA.M_TANDY2);
            else {
                if (VGA.vga.mode==VGA.M_TANDY16) {
                    VGA.VGA_SetModeNow(VGA.M_TANDY4);
                } else VGA.VGA_SetMode(VGA.M_TANDY4);
            }
            tandy_update_palette();
        } else {
            VGA.VGA_SetMode(VGA.M_TANDY_TEXT);
        }
    }

    private static void PCJr_FindMode() {
        if ((VGA.vga.tandy.mode_control & 0x2)!=0) {
            if ((VGA.vga.tandy.mode_control & 0x10)!=0) {
                /* bit4 of mode control 1 signals 16 colour graphics mode */
                if (VGA.vga.mode==VGA.M_TANDY4) VGA.VGA_SetModeNow(VGA.M_TANDY16); // TODO lowres mode only
                else VGA.VGA_SetMode(VGA.M_TANDY16);
            } else if ((VGA.vga.tandy.gfx_control & 0x08)!=0) {
                /* bit3 of mode control 2 signals 2 colour graphics mode */
                VGA.VGA_SetMode(VGA.M_TANDY2);
            } else {
                /* otherwise some 4-colour graphics mode */
                if (VGA.vga.mode==VGA.M_TANDY16) VGA.VGA_SetModeNow(VGA.M_TANDY4);
                else VGA.VGA_SetMode(VGA.M_TANDY4);
            }
        } else {
            VGA.VGA_SetMode(VGA.M_TANDY_TEXT);
        }
    }

    private static void TandyCheckLineMask() {
        if ((VGA.vga.tandy.extended_ram & 1)!=0) {
            VGA.vga.tandy.line_mask = 0;
        } else if ((VGA.vga.tandy.mode_control & 0x2)!=0) {
            VGA.vga.tandy.line_mask |= 1;
        }
        if ( VGA.vga.tandy.line_mask!=0 ) {
            VGA.vga.tandy.line_shift = 13;
            VGA.vga.tandy.addr_mask = (1 << 13) - 1;
        } else {
            VGA.vga.tandy.addr_mask = (/*Bitu*/int)(~0);
            VGA.vga.tandy.line_shift = 0;
        }
    }

    private static void write_tandy_reg(/*Bit8u*/short val) {
        switch (VGA.vga.tandy.reg_index) {
        case 0x0:
            if (Dosbox.machine==MachineType.MCH_PCJR) {
                VGA.vga.tandy.mode_control=val;
                VGA_draw.VGA_SetBlinking(val & 0x20);
                PCJr_FindMode();
                if ((val&0x8)!=0) VGA.vga.attr.disabled &= ~1;
			    else VGA.vga.attr.disabled |= 1;
            } else {
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"Unhandled Write "+Integer.toString(val, 16)+" to tandy reg "+Integer.toString(VGA.vga.tandy.reg_index,16));
            }
            break;
        case 0x1:	/* Palette mask */
		    VGA.vga.tandy.palette_mask = val;
		    tandy_update_palette();
		    break;
        case 0x2:	/* Border color */
            VGA.vga.tandy.border_color=val;
            break;
        case 0x3:	/* More control */
            VGA.vga.tandy.gfx_control=val;
            if (Dosbox.machine==MachineType.MCH_TANDY) TANDY_FindMode();
            else PCJr_FindMode();
            break;
        case 0x5:	/* Extended ram page register */
            // Bit 0 enables extended ram
            // Bit 7 Switches clock, 0 -> cga 28.6 , 1 -> mono 32.5
            VGA.vga.tandy.extended_ram = val;
            //This is a bit of a hack to enable mapping video memory differently for highres mode
            TandyCheckLineMask();
            VGA_memory.VGA_SetupHandlers();
            break;
        default:
		    if ((VGA.vga.tandy.reg_index & 0xf0) == 0x10) { // color palette
                VGA.vga.attr.palette[VGA.vga.tandy.reg_index-0x10] = (short)(val&0xf);
                tandy_update_palette();
            } else
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL, "Unhandled Write "+Integer.toString(val, 16)+" to tandy reg "+Integer.toString(VGA.vga.tandy.reg_index,16));
            }
    }

    private static IoHandler.IO_WriteHandler write_tandy = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (port) {
            case 0x3d8:
                VGA.vga.tandy.mode_control=(/*Bit8u*/short)val;
                if ((val&0x8)!=0) VGA.vga.attr.disabled &= ~1;
		        else VGA.vga.attr.disabled |= 1;
                TandyCheckLineMask();
                VGA_draw.VGA_SetBlinking(val & 0x20);
                TANDY_FindMode();
                break;
            case 0x3d9:
                VGA.vga.tandy.color_select=(short)val;
                if (VGA.vga.mode==VGA.M_TANDY2) VGA.vga.attr.palette[0xf] = (short)(VGA.vga.tandy.color_select&0xf);
                else VGA.vga.attr.palette[0] = (short)(VGA.vga.tandy.color_select&0xf); // Pirates!
                tandy_update_palette();
                break;
            case 0x3da:
                VGA.vga.tandy.reg_index=(/*Bit8u*/short)val;
                //if (val&0x10) vga.attr.disabled |= 2;
		        //else vga.attr.disabled &= ~2;
                break;
    //	case 0x3dd:	//Extended ram page address register:
                //break;
            case 0x3de:
                write_tandy_reg((/*Bit8u*/short)val);
                break;
            case 0x3df:
                // CRT/processor page register
                // See the comments on the PCJr version of this register.
                // A difference to it is:
                // Bit 3-5: Processor page CPU_PG
                // The remapped range is 32kB instead of 16. Therefore CPU_PG bit 0
                // appears to be ORed with CPU A14 (to preserve some sort of
                // backwards compatibility?), resulting in odd pages being mapped
                // as 2x16kB. Implemeted in vga_memory.cpp Tandy handler.

                VGA.vga.tandy.line_mask = (/*Bit8u*/short)(val >> 6);
                VGA.vga.tandy.draw_bank = (short)(val & ((VGA.vga.tandy.line_mask&2)!=0 ? 0x6 : 0x7));
                VGA.vga.tandy.mem_bank = (short)((val >> 3) & 7);

                TandyCheckLineMask();
                VGA_memory.VGA_SetupHandlers();
                break;
            }
        }
    };

    private static IoHandler.IO_WriteHandler write_pcjr = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (port) {
            case 0x3da:
                if (VGA.vga.tandy.pcjr_flipflop) write_tandy_reg((/*Bit8u*/short)val);
                else {
                    VGA.vga.tandy.reg_index=(short)val;
                    if ((VGA.vga.tandy.reg_index & 0x10)!=0)
                        VGA.vga.attr.disabled |= 2;
                    else VGA.vga.attr.disabled &= ~2;
                }
                VGA.vga.tandy.pcjr_flipflop=!VGA.vga.tandy.pcjr_flipflop;
                break;
            case 0x3df:
                // CRT/processor page register

                // Bit 0-2: CRT page PG0-2
                // In one- and two bank modes, bit 0-2 select the 16kB memory
                // area of system RAM that is displayed on the screen.
                // In 4-banked modes, bit 1-2 select the 32kB memory area.
                // Bit 2 only has effect when the PCJR upgrade to 128k is installed.

                // Bit 3-5: Processor page CPU_PG
                // Selects the 16kB area of system RAM that is mapped to
                // the B8000h IBM PC video memory window. Since A14-A16 of the
                // processor are unconditionally replaced with these bits when
                // B8000h is accessed, the 16kB area is mapped to the 32kB
                // range twice in a row. (Scuba Venture writes across the boundary)

                // Bit 6-7: Video Address mode
                // 0: CRTC addresses A0-12 directly, accessing 8k characters
                //    (+8k attributes). Used in text modes (one bank).
                //    PG0-2 in effect. 16k range.
                // 1: CRTC A12 is replaced with CRTC RA0 (see max_scanline).
                //    This results in the even/odd scanline two bank system.
                //    PG0-2 in effect. 16k range.
                // 2: Documented as unused. CRTC addresses A0-12, PG0 is replaced
                //    with RA1. Looks like nonsense.
                //    PG1-2 in effect. 32k range which cannot be used completely.
                // 3: CRTC A12 is replaced with CRTC RA0, PG0 is replaced with
                //    CRTC RA1. This results in the 4-bank mode.
                //    PG1-2 in effect. 32k range.

                VGA.vga.tandy.line_mask = (/*Bit8u*/short)(val >> 6);
                VGA.vga.tandy.draw_bank = (short)(val & ((VGA.vga.tandy.line_mask&2)!=0 ? 0x6 : 0x7));
                VGA.vga.tandy.mem_bank = (short)((val >> 3) & 7);
                VGA.vga.tandy.draw_base = VGA.vga.tandy.draw_bank * 16 * 1024;
                VGA.vga.tandy.mem_base = VGA.vga.tandy.mem_bank * 16 * 1024;
                TandyCheckLineMask();
                VGA_memory.VGA_SetupHandlers();
                break;
            }
        }
    };

    private static Mapper.MAPPER_Handler CycleHercPal = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed) return;
            if (++herc_pal>2) herc_pal=0;
            Herc_Palette();
            VGA_dac.VGA_DAC_CombineColor(1,7);
        }
    };

    public static void Herc_Palette() {
        switch (herc_pal) {
        case 0:	// White
            VGA_dac.VGA_DAC_SetEntry(0x7,0x2a,0x2a,0x2a);
            VGA_dac.VGA_DAC_SetEntry(0xf,0x3f,0x3f,0x3f);
            break;
        case 1:	// Amber
            VGA_dac.VGA_DAC_SetEntry(0x7,0x34,0x20,0x00);
            VGA_dac.VGA_DAC_SetEntry(0xf,0x3f,0x34,0x00);
            break;
        case 2:	// Green
            VGA_dac.VGA_DAC_SetEntry(0x7,0x00,0x26,0x00);
            VGA_dac.VGA_DAC_SetEntry(0xf,0x00,0x3f,0x00);
            break;
        }
    }

    private static IoHandler.IO_WriteHandler write_hercules = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (port) {
            case 0x3b8: {
                // the protected bits can always be cleared but only be set if the
                // protection bits are set
                if ((VGA.vga.herc.mode_control&0x2)!=0) {
                    // already set
                    if ((val&0x2)==0) {
                        VGA.vga.herc.mode_control &= ~0x2;
                        VGA.VGA_SetMode(VGA.M_HERC_TEXT);
                    }
                } else {
                    // not set, can only set if protection bit is set
                    if ((val & 0x2)!=0 && (VGA.vga.herc.enable_bits & 0x1)!=0) {
                        VGA.vga.herc.mode_control |= 0x2;
                        VGA.VGA_SetMode(VGA.M_HERC_GFX);
                    }
                }
                if ((VGA.vga.herc.mode_control&0x80)!=0) {
                    if ((val&0x80)==0) {
                        VGA.vga.herc.mode_control &= ~0x80;
                        VGA.vga.tandy.draw_base = VGA.vga.mem.linear;
                    }
                } else {
                    if ((val & 0x80)!=0 && (VGA.vga.herc.enable_bits & 0x2)!=0) {
                        VGA.vga.herc.mode_control |= 0x80;
                        VGA.vga.tandy.draw_base = VGA.vga.mem.linear+ 32*1024;
                    }
                }
                VGA.vga.draw.blinking = (val&0x20)!=0;
                VGA.vga.herc.mode_control &= 0x82;
                VGA.vga.herc.mode_control |= val & ~0x82;
                break;
                }
            case 0x3bf:
                if (((VGA.vga.herc.enable_bits & 0xFF) ^ val)!=0) {
                    VGA.vga.herc.enable_bits=(byte)val;
                    // Bit 1 enables the upper 32k of video memory,
                    // so update the handlers
                    VGA_memory.VGA_SetupHandlers();
                }
                break;
            }
        }
    };

/* static Bitu read_hercules(Bitu port,Bitu iolen) {
	Log.log_msg("read from Herc port %x",port);
	return 0;
} */

    private static IoHandler.IO_ReadHandler read_herc_status = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            // 3BAh (R):  Status Register
            // bit   0  Horizontal sync
            //       1  Light pen status (only some cards)
            //       3  Video signal
            //     4-6	000: Hercules
            //			001: Hercules Plus
            //			101: Hercules InColor
            //			111: Unknown clone
            //       7  Vertical sync inverted

            double timeInFrame = Pic.PIC_FullIndex()-VGA.vga.draw.delay.framestart;
            /*Bit8u*/short retval=0x72; // Hercules ident; from a working card (Winbond W86855AF)
                            // Another known working card has 0x76 ("KeysoGood", full-length)
            if (timeInFrame < VGA.vga.draw.delay.vrstart ||
                timeInFrame > VGA.vga.draw.delay.vrend) retval |= 0x80;

            double timeInLine=timeInFrame % VGA.vga.draw.delay.htotal;
            if (timeInLine >= VGA.vga.draw.delay.hrstart &&
                timeInLine <= VGA.vga.draw.delay.hrend) retval |= 0x1;

            // 688 Attack sub checks bit 3 - as a workaround have the bit enabled
            // if no sync active (corresponds to a completely white screen)
            if ((retval&0x81)==0x80) retval |= 0x8;
            return retval;
        }
    };

    public static void VGA_SetupOther() {
        VGA.vga.tandy = new VGA.VGA_TANDY();
        VGA.vga.attr.disabled = 0;
        VGA.vga.config.bytes_skip=0;

        //Initialize values common for most Dosbox.machines, can be overwritten
        VGA.vga.tandy.draw_base = VGA.vga.mem.linear;
        VGA.vga.tandy.mem_base = VGA.vga.mem.linear;
        VGA.vga.tandy.addr_mask = 8*1024 - 1;
        VGA.vga.tandy.line_mask = 3;
        VGA.vga.tandy.line_shift = 13;

        if (Dosbox.machine==MachineType.MCH_CGA || Dosbox.IS_TANDY_ARCH()) {
            for (int i=0;i<256;i++)	System.arraycopy(Int10_memory.int10_font_08, i*8, VGA.vga.draw.font, i*32, 8);
            VGA.vga.draw.font_tables[0]=VGA.vga.draw.font_tables[1]=new Ptr(VGA.vga.draw.font,0);
        }
        if (Dosbox.machine==MachineType.MCH_CGA || Dosbox.IS_TANDY_ARCH() || Dosbox.machine==MachineType.MCH_HERC) {
            IoHandler.IO_RegisterWriteHandler(0x3db,write_lightpen,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3dc,write_lightpen,IoHandler.IO_MB);
        }
        if (Dosbox.machine==MachineType.MCH_HERC) {
            for (int i=0;i<256;i++)	System.arraycopy(Int10_memory.int10_font_14, i*14, VGA.vga.draw.font, i*32, 14);
            VGA.vga.draw.font_tables[0]=VGA.vga.draw.font_tables[1]=new Ptr(VGA.vga.draw.font,0);
            JavaMapper.MAPPER_AddHandler(CycleHercPal, Mapper.MapKeys.MK_f11, 0, "hercpal", "Herc Pal");
        }
        if (Dosbox.machine==MachineType.MCH_CGA) {
            IoHandler.IO_RegisterWriteHandler(0x3d8,write_cga,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3d9,write_cga,IoHandler.IO_MB);
            JavaMapper.MAPPER_AddHandler(IncreaseHue, Mapper.MapKeys.MK_f11, Mapper.MMOD2, "inchue", "Inc Hue");
            JavaMapper.MAPPER_AddHandler(DecreaseHue, Mapper.MapKeys.MK_f11, 0, "dechue", "Dec Hue");
        }
        if (Dosbox.machine==MachineType.MCH_TANDY) {
            write_tandy.call( 0x3df, 0x0, 0 );
            IoHandler.IO_RegisterWriteHandler(0x3d8,write_tandy,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3d9,write_tandy,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3da,write_tandy,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3de,write_tandy,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3df,write_tandy,IoHandler.IO_MB);
        }
        if (Dosbox.machine==MachineType.MCH_PCJR) {
            //write_pcjr will setup base address
            write_pcjr.call( 0x3df, 0x7 | (0x7 << 3), 0 );
            IoHandler.IO_RegisterWriteHandler(0x3da,write_pcjr,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3df,write_pcjr,IoHandler.IO_MB);
            // additional CRTC access documented
            IoHandler.IO_RegisterWriteHandler(0x3d0,write_crtc_index_other,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3d1,write_crtc_data_other,IoHandler.IO_MB);
        }
        if (Dosbox.machine==MachineType.MCH_HERC) {
            /*Bitu*/int base=0x3b0;
            for (/*Bitu*/int i = 0; i < 4; i++) {
                // The registers are repeated as the address is not decoded properly;
                // The official ports are 3b4, 3b5
                IoHandler.IO_RegisterWriteHandler(base+i*2,write_crtc_index_other,IoHandler.IO_MB);
                IoHandler.IO_RegisterWriteHandler(base+i*2+1,write_crtc_data_other,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(base+i*2,read_crtc_index_other,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(base+i*2+1,read_crtc_data_other,IoHandler.IO_MB);
            }
            VGA.vga.herc.enable_bits=0;
            VGA.vga.herc.mode_control=0xa; // first mode written will be text mode
            VGA.vga.crtc.underline_location = 13;
            IoHandler.IO_RegisterWriteHandler(0x3b8,write_hercules,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3bf,write_hercules,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3ba,read_herc_status,IoHandler.IO_MB);
        }
        if (Dosbox.machine==MachineType.MCH_CGA) {
            /*Bitu*/int base=0x3d0;
            for (/*Bitu*/int port_ct=0; port_ct<4; port_ct++) {
                IoHandler.IO_RegisterWriteHandler(base+port_ct*2,write_crtc_index_other,IoHandler.IO_MB);
                IoHandler.IO_RegisterWriteHandler(base+port_ct*2+1,write_crtc_data_other,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(base+port_ct*2,read_crtc_index_other,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(base+port_ct*2+1,read_crtc_data_other,IoHandler.IO_MB);
            }
        }
        if (Dosbox.IS_TANDY_ARCH()) {
            /*Bitu*/int base=0x3d4;
            IoHandler.IO_RegisterWriteHandler(base,write_crtc_index_other,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(base+1,write_crtc_data_other,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(base,read_crtc_index_other,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(base+1,read_crtc_data_other,IoHandler.IO_MB);
        }

    }

}
