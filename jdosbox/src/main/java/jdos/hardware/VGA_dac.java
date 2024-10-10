package jdos.hardware;

import jdos.Dosbox;
import jdos.gui.Render;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class VGA_dac {
    /*
    3C6h (R/W):  PEL Mask
    bit 0-7  This register is anded with the palette index sent for each dot.
             Should be set to FFh.

    3C7h (R):  DAC State Register
    bit 0-1  0 indicates the DAC is in Write Mode and 3 indicates Read mode.

    3C7h (W):  PEL Address Read Mode
    bit 0-7  The PEL data register (0..255) to be read from 3C9h.
    Note: After reading the 3 bytes at 3C9h this register will increment,
          pointing to the next data register.

    3C8h (R/W):  PEL Address Write Mode
    bit 0-7  The PEL data register (0..255) to be written to 3C9h.
    Note: After writing the 3 bytes at 3C9h this register will increment, pointing
          to the next data register.

    3C9h (R/W):  PEL Data Register
    bit 0-5  Color value
    Note:  Each read or write of this register will cycle through first the
           registers for Red, Blue and Green, then increment the appropriate
           address register, thus the entire palette can be loaded by writing 0 to
           the PEL Address Write Mode register 3C8h and then writing all 768 bytes
           of the palette to this register.
    */

    private static final int DAC_READ = 0;
    private static final int DAC_WRITE = 1;

    static private void VGA_DAC_SendColor( /*Bitu*/int index, /*Bitu*/int src ) {
        final /*Bitu*/short red = VGA.vga.dac.rgb[src].red;
        final /*Bitu*/short green = VGA.vga.dac.rgb[src].green;
        final /*Bitu*/short blue = VGA.vga.dac.rgb[src].blue;
        //Set entry in 16bit output lookup table
        VGA.vga.dac.xlat16[index] = ((blue>>1)&0x1f) | (((green)&0x3f)<<5) | (((red>>1)&0x1f) << 11);

        Render.RENDER_SetPal(index, (red << 2) | ( red >> 4 ), (green << 2) | ( green >> 4 ), (blue << 2) | ( blue >> 4 ) );
    }

    static private void VGA_DAC_UpdateColor( /*Bitu*/int index ) {
        /*Bitu*/int maskIndex = index & VGA.vga.dac.pel_mask;
        VGA_DAC_SendColor( index, maskIndex );
    }

    static private IoHandler.IO_WriteHandler write_p3c6 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if ( VGA.vga.dac.pel_mask != val ) {
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:DCA:Pel Mask set to "+Integer.toString(val,16));
                VGA.vga.dac.pel_mask = (short)val;
                for ( /*Bitu*/int i = 0;i<256;i++)
                    VGA_DAC_UpdateColor( i );
            }
        }
    };


    static private IoHandler.IO_ReadHandler read_p3c6 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return VGA.vga.dac.pel_mask;
        }
    };


    static private IoHandler.IO_WriteHandler write_p3c7 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.dac.read_index=(short)(val & 0xFF);
            VGA.vga.dac.pel_index=0;
            VGA.vga.dac.state=DAC_READ;
            VGA.vga.dac.write_index= (short)((val + 1) & 0xFF);
        }
    };

    static private IoHandler.IO_ReadHandler read_p3c7 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            if (VGA.vga.dac.state==DAC_READ) return 0x3;
            else return 0x0;
        }
    };

    static private IoHandler.IO_WriteHandler write_p3c8 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.dac.write_index=(short)(val & 0xFF);
            VGA.vga.dac.pel_index=0;
            VGA.vga.dac.state=DAC_WRITE;
        }
    };

    static private IoHandler.IO_ReadHandler read_p3c8 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return VGA.vga.dac.write_index;
        }
    };

    static private IoHandler.IO_WriteHandler write_p3c9 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            val&=0x3f;
            switch (VGA.vga.dac.pel_index) {
            case 0:
                VGA.vga.dac.rgb[VGA.vga.dac.write_index].red=(short)val;
                VGA.vga.dac.pel_index=1;
                break;
            case 1:
                VGA.vga.dac.rgb[VGA.vga.dac.write_index].green=(short)val;
                VGA.vga.dac.pel_index=2;
                break;
            case 2:
                VGA.vga.dac.rgb[VGA.vga.dac.write_index].blue=(short)val;
                switch (VGA.vga.mode) {
                case VGA.M_VGA:
                case VGA.M_LIN8:
                    VGA_DAC_UpdateColor( VGA.vga.dac.write_index );
                    if (VGA.vga.dac.pel_mask != 0xff) {
                        /*Bitu*/int index = VGA.vga.dac.write_index;
                        if ( (index & VGA.vga.dac.pel_mask) == index ) {
                            for ( /*Bitu*/int i = index+1;i<256;i++)
                                if ( (i & VGA.vga.dac.pel_mask) == index )
                                    VGA_DAC_UpdateColor( i );
                        }
                    }
                    break;
                default:
                    /* Check for attributes and DAC entry link */
                    for (/*Bitu*/int i=0;i<16;i++) {
                        if (VGA.vga.dac.combine[i]==VGA.vga.dac.write_index) {
                            VGA_DAC_SendColor( i, VGA.vga.dac.write_index );
                        }
                    }
                }
                VGA.vga.dac.write_index++;VGA.vga.dac.write_index&=0xFF;
    //		VGA.vga.dac.read_index = VGA.vga.dac.write_index - 1;//disabled as it breaks Wari
                VGA.vga.dac.pel_index=0;
                break;
            default:
                Log.log(LogTypes.LOG_VGAGFX,LogSeverities.LOG_NORMAL,"VGA:DAC:Illegal Pel Index");			//If this can actually happen that will be the day
                break;
            }
        }
    };

    static private IoHandler.IO_ReadHandler read_p3c9 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bitu*/short ret;
            switch (VGA.vga.dac.pel_index) {
            case 0:
                ret=VGA.vga.dac.rgb[VGA.vga.dac.read_index].red;
                VGA.vga.dac.pel_index=1;
                break;
            case 1:
                ret=VGA.vga.dac.rgb[VGA.vga.dac.read_index].green;
                VGA.vga.dac.pel_index=2;
                break;
            case 2:
                ret=VGA.vga.dac.rgb[VGA.vga.dac.read_index].blue;
                VGA.vga.dac.read_index++;VGA.vga.dac.read_index&=0xFF;
                VGA.vga.dac.pel_index=0;
    //		VGA.vga.dac.write_index=VGA.vga.dac.read_index+1;//disabled as it breaks wari
                break;
            default:
                Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:DAC:Illegal Pel Index");			//If this can actually happen that will be the day
                ret=0;
                break;
            }
            return ret;
        }
    };

    public static void VGA_DAC_CombineColor(/*Bitu*/int attr,/*Bitu*/int pal) {
        /* Check if this is a new color */
        VGA.vga.dac.combine[attr]=(short)pal;
        switch (VGA.vga.mode) {
        case VGA.M_LIN8:
            break;
        case VGA.M_VGA:
            // used by copper demo; almost no video card seems to suport it
            if(!Dosbox.IS_VGA_ARCH() || (Dosbox.svgaCard!= SVGACards.SVGA_None)) break;
        default:
            VGA_DAC_SendColor( attr, pal );
        }
    }

    public static void VGA_DAC_SetEntry(/*Bitu*/int entry,/*Bitu*/int red,/*Bitu*/int green,/*Bitu*/int blue) {
        //Should only be called in machine != vga
        VGA.vga.dac.rgb[entry].red=(short)red;
        VGA.vga.dac.rgb[entry].green=(short)green;
        VGA.vga.dac.rgb[entry].blue=(short)blue;
        for (/*Bitu*/int i=0;i<16;i++)
            if (VGA.vga.dac.combine[i]==entry)
                VGA_DAC_SendColor( i, i );
    }

    static public void VGA_SetupDAC() {
        VGA.vga.dac.first_changed=256;
        VGA.vga.dac.bits=6;
        VGA.vga.dac.pel_mask=0xff;
        VGA.vga.dac.pel_index=0;
        VGA.vga.dac.state=DAC_READ;
        VGA.vga.dac.read_index=0;
        VGA.vga.dac.write_index=0;
        if (Dosbox.IS_VGA_ARCH()) {
            /* Setup the DAC IO port Handlers */
            IoHandler.IO_RegisterWriteHandler(0x3c6,write_p3c6,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3c6,read_p3c6,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3c7,write_p3c7,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3c7,read_p3c7,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3c8,write_p3c8,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3c8,read_p3c8,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3c9,write_p3c9,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x3c9,read_p3c9,IoHandler.IO_MB);
        } else if (Dosbox.machine== MachineType.MCH_EGA) {
            for (/*Bitu*/int i=0;i<64;i++) {
                if ((i&4)>0) VGA.vga.dac.rgb[i].red=0x2a;
                else VGA.vga.dac.rgb[i].red=0;
                if ((i&32)>0) VGA.vga.dac.rgb[i].red+=0x15;

                if ((i&2)>0) VGA.vga.dac.rgb[i].green=0x2a;
                else VGA.vga.dac.rgb[i].green=0;
                if ((i&16)>0) VGA.vga.dac.rgb[i].green+=0x15;

                if ((i&1)>0) VGA.vga.dac.rgb[i].blue=0x2a;
                else VGA.vga.dac.rgb[i].blue=0;
                if ((i&8)>0) VGA.vga.dac.rgb[i].blue+=0x15;
            }
        }
    }
}
