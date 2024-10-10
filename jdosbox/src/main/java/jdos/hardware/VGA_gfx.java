package jdos.hardware;

import jdos.Dosbox;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_gfx {
static boolean index9warned=false;

    private static IoHandler.IO_WriteHandler write_p3ce = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
	        VGA.vga.gfx.index=(short)(val & 0x0f);
        }
    };

    private static IoHandler.IO_ReadHandler read_p3ce = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
	        return VGA.vga.gfx.index;
        }
    };

    private static IoHandler.IO_WriteHandler write_p3cf = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (VGA.vga.gfx.index) {
            case 0:	/* Set/Reset Register */
                VGA.vga.gfx.set_reset=(short)(val & 0x0f);
                VGA.vga.config.full_set_reset=VGA.FillTable[val & 0x0f];
                VGA.vga.config.full_enable_and_set_reset=VGA.vga.config.full_set_reset &
                    VGA.vga.config.full_enable_set_reset;
                /*
                    0	If in Write Mode 0 and bit 0 of 3CEh index 1 is set a write to
                        display memory will set all the bits in plane 0 of the byte to this
                        bit, if the corresponding bit is set in the Map Mask Register (3CEh
                        index 8).
                    1	Same for plane 1 and bit 1 of 3CEh index 1.
                    2	Same for plane 2 and bit 2 of 3CEh index 1.
                    3	Same for plane 3 and bit 3 of 3CEh index 1.
                */
        //		LOG_DEBUG("Set Reset = %2X",val);
                break;
            case 1: /* Enable Set/Reset Register */
                VGA.vga.gfx.enable_set_reset=(short)(val & 0x0f);
                VGA.vga.config.full_enable_set_reset=VGA.FillTable[val & 0x0f];
                VGA.vga.config.full_not_enable_set_reset=~VGA.vga.config.full_enable_set_reset;
                VGA.vga.config.full_enable_and_set_reset=VGA.vga.config.full_set_reset &
                    VGA.vga.config.full_enable_set_reset;
        //		if (VGA.vga.gfx.enable_set_reset)) VGA.vga.config.mh_mask|=MH_SETRESET else VGA.vga.config.mh_mask&=~MH_SETRESET;
                break;
            case 2: /* Color Compare Register */
                VGA.vga.gfx.color_compare=(short)(val & 0x0f);
                /*
                    0-3	In Read Mode 1 each pixel at the address of the byte read is compared
                        to this color and the corresponding bit in the output set to 1 if
                        they match, 0 if not. The Color Don't Care Register (3CEh index 7)
                        can exclude bitplanes from the comparison.
                */
                VGA.vga.config.color_compare=(short)(val & 0xf);
        //		LOG_DEBUG("Color Compare = %2X",val);
                break;
            case 3: /* Data Rotate */
                VGA.vga.gfx.data_rotate=(short)val;
                VGA.vga.config.data_rotate=(short)(val & 7);
        //		if (val) VGA.vga.config.mh_mask|=MH_ROTATEOP else VGA.vga.config.mh_mask&=~MH_ROTATEOP;
                VGA.vga.config.raster_op=(short)((val>>3) & 3);
                /*
                    0-2	Number of positions to rotate data right before it is written to
                        display memory. Only active in Write Mode 0.
                    3-4	In Write Mode 2 this field controls the relation between the data
                        written from the CPU, the data latched from the previous read and the
                        data written to display memory:
                        0: CPU Data is written unmodified
                        1: CPU data is ANDed with the latched data
                        2: CPU data is ORed  with the latch data.
                        3: CPU data is XORed with the latched data.
                */
                break;
            case 4: /* Read Map Select Register */
                /*	0-1	number of the plane Read Mode 0 will read from */
                VGA.vga.gfx.read_map_select=(short)(val & 0x03);
                VGA.vga.config.read_map_select=(short)(val & 0x03);
        //		LOG_DEBUG("Read Map %2X",val);
                break;
            case 5: /* Mode Register */
                if (((VGA.vga.gfx.mode ^ val) & 0xf0)!=0) {
                    VGA.vga.gfx.mode=(byte)val;
                    VGA.VGA_DetermineMode();
                } else VGA.vga.gfx.mode=(byte)val;
                VGA.vga.config.write_mode=(short)(val & 3);
                VGA.vga.config.read_mode=(short)((val >> 3) & 1);
        //		LOG_DEBUG("Write Mode %d Read Mode %d val %d",VGA.vga.config.write_mode,VGA.vga.config.read_mode,val);
                /*
                    0-1	Write Mode: Controls how data from the CPU is transformed before
                        being written to display memory:
                        0:	Mode 0 works as a Read-Modify-Write operation.
                            First a read access loads the data latches of the VGA with the
                            value in video memory at the addressed location. Then a write
                            access will provide the destination address and the CPU data
                            byte. The data written is modified by the function code in the
                            Data Rotate register (3CEh index 3) as a function of the CPU
                            data and the latches, then data is rotated as specified by the
                            same register.
                        1:	Mode 1 is used for video to video transfers.
                            A read access will load the data latches with the contents of
                            the addressed byte of video memory. A write access will write
                            the contents of the latches to the addressed byte. Thus a single
                            MOVSB instruction can copy all pixels in the source address byte
                            to the destination address.
                        2:	Mode 2 writes a color to all pixels in the addressed byte of
                            video memory. Bit 0 of the CPU data is written to plane 0 et
                            cetera. Individual bits can be enabled or disabled through the
                            Bit Mask register (3CEh index 8).
                        3:	Mode 3 can be used to fill an area with a color and pattern. The
                            CPU data is rotated according to 3CEh index 3 bits 0-2 and anded
                            with the Bit Mask Register (3CEh index 8). For each bit in the
                            result the corresponding pixel is set to the color in the
                            Set/Reset Register (3CEh index 0 bits 0-3) if the bit is set and
                            to the contents of the processor latch if the bit is clear.
                    3	Read Mode
                        0:	Data is read from one of 4 bit planes depending on the Read Map
                            Select Register (3CEh index 4).
                        1:	Data returned is a comparison between the 8 pixels occupying the
                            read byte and the color in the Color Compare Register (3CEh
                            index 2). A bit is set if the color of the corresponding pixel
                            matches the register.
                    4	Enables Odd/Even mode if set (See 3C4h index 4 bit 2).
                    5	Enables CGA style 4 color pixels using even/odd bit pairs if set.
                    6	Enables 256 color mode if set.
                */
                break;
            case 6: /* Miscellaneous Register */
                if (((VGA.vga.gfx.miscellaneous ^ val) & 0x0c)!=0) {
                    VGA.vga.gfx.miscellaneous=(byte)val;
                    VGA.VGA_DetermineMode();
                } else VGA.vga.gfx.miscellaneous=(byte)val;
                VGA_memory.VGA_SetupHandlers();
                /*
                    0	Indicates Graphics Mode if set, Alphanumeric mode else.
                    1	Enables Odd/Even mode if set.
                    2-3	Memory Mapping:
                        0: use A000h-BFFFh
                        1: use A000h-AFFFh   VGA Graphics modes
                        2: use B000h-B7FFh   Monochrome modes
                        3: use B800h-BFFFh   CGA modes
                */
                break;
            case 7: /* Color Don't Care Register */
                VGA.vga.gfx.color_dont_care=(short)(val & 0x0f);
                /*
                    0	Ignore bit plane 0 in Read mode 1 if clear.
                    1	Ignore bit plane 1 in Read mode 1 if clear.
                    2	Ignore bit plane 2 in Read mode 1 if clear.
                    3	Ignore bit plane 3 in Read mode 1 if clear.
                */
                VGA.vga.config.color_dont_care=(short)(val & 0xf);
        //		LOG_DEBUG("Color don't care = %2X",val);
                break;
            case 8: /* Bit Mask Register */
                VGA.vga.gfx.bit_mask=(short)val;
                VGA.vga.config.full_bit_mask=VGA.ExpandTable[val];
        //		LOG_DEBUG("Bit mask %2X",val);
                /*
                    0-7	Each bit if set enables writing to the corresponding bit of a byte in
                        display memory.
                */
                break;
            default:
                if (VGA.svga.write_p3cf!=null) {
                    VGA.svga.write_p3cf.call(VGA.vga.gfx.index, val, iolen);
                    break;
                }
                if (VGA.vga.gfx.index == 9 && !index9warned) {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:3CF:Write "+Integer.toString(val, 16)+" to illegal index 9");
                    index9warned=true;
                    break;
                }
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:3CF:Write "+Integer.toString(val, 16)+" to illegal index "+Integer.toString(VGA.vga.gfx.index,16));
                break;
            }
        }
    };

    private static IoHandler.IO_ReadHandler read_p3cf = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            switch (VGA.vga.gfx.index) {
            case 0:	/* Set/Reset Register */
                return VGA.vga.gfx.set_reset;
            case 1: /* Enable Set/Reset Register */
                return VGA.vga.gfx.enable_set_reset;
            case 2: /* Color Compare Register */
                return VGA.vga.gfx.color_compare;
            case 3: /* Data Rotate */
                return VGA.vga.gfx.data_rotate;
            case 4: /* Read Map Select Register */
                return VGA.vga.gfx.read_map_select;
            case 5: /* Mode Register */
                return VGA.vga.gfx.mode;
            case 6: /* Miscellaneous Register */
                return VGA.vga.gfx.miscellaneous & 0xFF;
            case 7: /* Color Don't Care Register */
                return VGA.vga.gfx.color_dont_care;
            case 8: /* Bit Mask Register */
                return VGA.vga.gfx.bit_mask;
            default:
                if (VGA.svga.read_p3cf!=null)
                    return VGA.svga.read_p3cf.call(VGA.vga.gfx.index, iolen);
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"Reading from illegal index "+Integer.toString(VGA.vga.gfx.index, 16)+" in port "+Integer.toString(port,16));
                break;
            }
            return 0;	/* Compiler happy */
        }
    };

    static public void VGA_SetupGFX() {
        if (Dosbox.IS_EGAVGA_ARCH()) {
            IoHandler.IO_RegisterWriteHandler(0x3ce,write_p3ce,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3cf,write_p3cf,IoHandler.IO_MB);
            if (Dosbox.IS_VGA_ARCH()) {
                IoHandler.IO_RegisterReadHandler(0x3ce,read_p3ce,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3cf,read_p3cf,IoHandler.IO_MB);
            }
        }
    }
}
