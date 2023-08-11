package jdos.ints;

import jdos.Dosbox;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.hardware.VGA;
import jdos.types.MachineType;
import jdos.types.SVGACards;
import jdos.util.ShortRef;

public class Int10_pal {
    static final int ACTL_MAX_REG = 0x14;

    static private void ResetACTL() {
        IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTC_ADDRESS) + 6);
    }

    static private void WriteTandyACTL(/*Bit8u*/short creg,/*Bit8u*/short val) {
        IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS,(byte)creg);
        if (Dosbox.machine== MachineType.MCH_TANDY) IoHandler.IO_Write(Int10.VGAREG_TDY_DATA,(byte)val);
        else IoHandler.IO_Write(Int10.VGAREG_PCJR_DATA,(byte)val);
    }

    static public void INT10_SetSinglePaletteRegister(/*Bit8u*/short reg,/*Bit8u*/short val) {
        switch (Dosbox.machine) {
        case MachineType.MCH_PCJR:
            reg&=0xf;
            IoHandler.IO_Read(Int10.VGAREG_TDY_RESET);
            WriteTandyACTL((short)(reg+0x10),val);
            IoHandler.IO_Write(0x3da,0x0); // palette back on
            break;
        case MachineType.MCH_TANDY:
            // TODO waits for vertical retrace
            switch(VGA.vga.mode) {
            case VGA.M_TANDY2:
                if (reg >= 0x10) break;
                else if (reg==1) reg = 0x1f;
                else reg |= 0x10;
                WriteTandyACTL((short)(reg+0x10),val);
                break;
            case VGA.M_TANDY4: {
                if (Int10_modes.CurMode.mode!=0x0a) {
                    // Palette values are kept constand by the BIOS.
                    // The four colors are mapped to special palette values by hardware.
                    // 3D8/3D9 registers influence this mapping. We need to figure out
                    // which entry is used for the requested color.
                    if (reg > 3) break;
                    if (reg != 0) { // 0 is assumed to be at 0
                        /*Bit8u*/int color_select=Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAL);
                        reg = (short)(reg*2+8); // Green Red Brown
                        if ((color_select& 0x20)!=0) reg++; // Cyan Magenta White
                    }
                    WriteTandyACTL((short)(reg+0x10),val);
                }
                // 4-color high resolution mode 0x0a isn't handled specially
                else WriteTandyACTL((short)(reg+0x10),val);
                break;
            }
            default:
                WriteTandyACTL((short)(reg+0x10),val);
                break;
            }
            IoHandler.IO_Write(0x3da, 0x0); // palette back on
            break;
        // EGAVGA_ARCH_CASE
        case MachineType.MCH_EGA:
        case MachineType.MCH_VGA:
            if (!Dosbox.IS_VGA_ARCH()) reg&=0x1f;
            if(reg<=ACTL_MAX_REG) {
                ResetACTL();
                IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)reg);
                IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,(byte)val);
            }
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)32);		//Enable output and protect palette
            break;
        }
    }


    public static void INT10_SetOverscanBorderColor(/*Bit8u*/short val) {
        switch (Dosbox.machine) {
        case MachineType.MCH_TANDY:
        case MachineType.MCH_PCJR: //TANDY_ARCH_CASE:
            IoHandler.IO_Read(Int10.VGAREG_TDY_RESET);
            WriteTandyACTL((short)0x02,val);
            break;
        // EGAVGA_ARCH_CASE
        case MachineType.MCH_EGA:
        case MachineType.MCH_VGA:
            ResetACTL();
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)0x11);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,(byte)val);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)32);		//Enable output and protect palette
            break;
        }
    }

    public static void INT10_SetAllPaletteRegisters(/*PhysPt*/int data) {
        switch (Dosbox.machine) {
        case MachineType.MCH_TANDY:
        case MachineType.MCH_PCJR: //TANDY_ARCH_CASE:
            IoHandler.IO_Read(Int10.VGAREG_TDY_RESET);
            // First the colors
            for(/*Bit8u*/short i=0;i<0x10;i++) {
                WriteTandyACTL((short)(i+0x10),(short)Memory.mem_readb(data));
                data++;
            }
            // Then the border
            WriteTandyACTL((short)0x02,(short)Memory.mem_readb(data));
            break;
        // EGAVGA_ARCH_CASE
        case MachineType.MCH_EGA:
        case MachineType.MCH_VGA:
            ResetACTL();
            // First the colors
            for(/*Bit8u*/short i=0;i<0x10;i++) {
                IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)i);
                IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,(byte)Memory.mem_readb(data));
                data++;
            }
            // Then the border
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)0x11);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,(byte)Memory.mem_readb(data));
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)32);		//Enable output and protect palette
            break;
        }
    }

    static public void INT10_ToggleBlinkingBit(/*Bit8u*/short state) {
        if(Dosbox.IS_VGA_ARCH()) {
            /*Bit8u*/short value;
        //	state&=0x01;
            if ((state>1) && (Dosbox.svgaCard== SVGACards.SVGA_S3Trio)) return;
            ResetACTL();

            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x10);
            value=IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA);
            if (state<=1) {
                value&=0xf7;
                value|=state<<3;
            }

            ResetACTL();
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x10);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,value);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,32);		//Enable output and protect palette

            if (state<=1) {
                /*Bit8u*/short msrval=(short)(Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR) & 0xdf);
                if (state!=0) msrval|=0x20;
                Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,msrval);
            }
        } else { // EGA
            // Usually it reads this from the mode list in ROM
            if (Int10_modes.CurMode.type!= VGA.M_TEXT) return;

            /*Bit8u*/short value = (short)((Int10_modes.CurMode.cwidth==9)? 0x4:0x0);
            if (state!=0) value |= 0x8;

            ResetACTL();
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x10);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,value);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x20);

            /*Bit8u*/short msrval=(short)(Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_MSR) & ~0x20);
            if (state!=0) msrval|=0x20;
            Memory.real_writeb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_MSR, msrval);
        }
    }

    public static short INT10_GetSinglePaletteRegister(short val, /*Bit8u*/short reg) {
        if(reg<=ACTL_MAX_REG) {
            ResetACTL();
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)(reg+32));
            val=IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,(byte)val);
        }
        return val;
    }

    public static short INT10_GetOverscanBorderColor() {
        ResetACTL();
        IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(byte)(0x11+32));
        short val=IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA);
        IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,(byte)val);
        return val;
    }

    public static void INT10_GetAllPaletteRegisters(/*PhysPt*/int data) {
        ResetACTL();
        // First the colors
        for(/*Bit8u*/short i=0;i<0x10;i++) {
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,i);
            Memory.mem_writeb(data,IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA));
            ResetACTL();
            data++;
        }
        // Then the border
        IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,(0x11+32));
        Memory.mem_writeb(data,IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA));
        ResetACTL();
    }

    public static void INT10_SetSingleDACRegister(/*Bit8u*/short index,/*Bit8u*/short red,/*Bit8u*/short green,/*Bit8u*/short blue) {
        IoHandler.IO_Write(Int10.VGAREG_DAC_WRITE_ADDRESS,index);
        if ((Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_MODESET_CTL)&0x06)==0) {
            IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,red);
            IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,green);
            IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,blue);
        } else {
            /* calculate clamped intensity, taken from VGABIOS */
            /*Bit32u*/int i=(( 77*red + 151*green + 28*blue ) + 0x80) >> 8;
            /*Bit8u*/short ic=(i>0x3f) ? 0x3f : ((/*Bit8u*/short)(i & 0xff));
            IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,ic);
            IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,ic);
            IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,ic);
        }

        IoHandler.IO_Write(Int10.VGAREG_DAC_WRITE_ADDRESS,index);
        IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,red);
        IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,green);
        IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,blue);
    }

    public static void INT10_GetSingleDACRegister(/*Bit8u*/short index,/*Bit8u*/ShortRef red,/*Bit8u*/ShortRef green,/*Bit8u*/ShortRef blue) {
        IoHandler.IO_Write(Int10.VGAREG_DAC_READ_ADDRESS,index);
        red.value=IoHandler.IO_Read(Int10.VGAREG_DAC_DATA);
        green.value=IoHandler.IO_Read(Int10.VGAREG_DAC_DATA);
        blue.value=IoHandler.IO_Read(Int10.VGAREG_DAC_DATA);
    }

    public static void INT10_SetDACBlock(/*Bit16u*/int index,/*Bit16u*/int count,/*PhysPt*/int data) {
        IoHandler.IO_Write(Int10.VGAREG_DAC_WRITE_ADDRESS,index);
        if ((Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_MODESET_CTL)&0x06)==0) {
            for (;count>0;count--) {
                IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,Memory.mem_readb(data++));
                IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,Memory.mem_readb(data++));
                IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,Memory.mem_readb(data++));
            }
        } else {
            for (;count>0;count--) {
                /*Bit8u*/int red=Memory.mem_readb(data++);
                /*Bit8u*/int green=Memory.mem_readb(data++);
                /*Bit8u*/int blue=Memory.mem_readb(data++);

                /* calculate clamped intensity, taken from VGABIOS */
                /*Bit32u*/int i=(( 77*red + 151*green + 28*blue ) + 0x80) >> 8;
                /*Bit8u*/short ic=(i>0x3f) ? 0x3f : ((/*Bit8u*/short)(i & 0xff));
                IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,ic);
                IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,ic);
                IoHandler.IO_Write(Int10.VGAREG_DAC_DATA,ic);
            }
        }
    }

    public static void INT10_GetDACBlock(/*Bit16u*/int index,/*Bit16u*/int count,/*PhysPt*/int data) {
        IoHandler.IO_Write(Int10.VGAREG_DAC_READ_ADDRESS,index);
        for (;count>0;count--) {
            Memory.mem_writeb(data++,IoHandler.IO_Read(Int10.VGAREG_DAC_DATA));
            Memory.mem_writeb(data++,IoHandler.IO_Read(Int10.VGAREG_DAC_DATA));
            Memory.mem_writeb(data++,IoHandler.IO_Read(Int10.VGAREG_DAC_DATA));
        }
    }

    public static void INT10_SelectDACPage(/*Bit8u*/short function,/*Bit8u*/short mode) {
        ResetACTL();
        IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x10);
        /*Bit8u*/short old10=IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA);
        if (function==0) {		//Select paging mode
            if (mode!=0) old10|=0x80;
            else old10&=0x7f;
            //IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x10);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,old10);
        } else {				//Select page
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,old10);
            if ((old10 & 0x80)==0) mode<<=2;
            mode&=0xf;
            IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x14);
            IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,mode);
        }
        IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,32);		//Enable output and protect palette
    }

    public static void INT10_GetDACPage(/*Bit8u*/ShortRef mode,/*Bit8u*/ShortRef page) {
        ResetACTL();
        IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x10);
        /*Bit8u*/short reg10=IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA);
        IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,reg10);
        mode.value=(short)(((reg10&0x80)!=0)?0x01:0x00);
        IoHandler.IO_Write(Int10.VGAREG_ACTL_ADDRESS,0x14);
        page.value=IoHandler.IO_Read(Int10.VGAREG_ACTL_READ_DATA);
        IoHandler.IO_Write(Int10.VGAREG_ACTL_WRITE_DATA,page.value);
        if(mode.value!=0) {
            page.value&=0xf;
        } else {
            page.value&=0xc;
            page.value>>=2;
        }
    }

    public static void INT10_SetPelMask(/*Bit8u*/short mask) {
        IoHandler.IO_Write(Int10.VGAREG_PEL_MASK,mask);
    }

    public static short INT10_GetPelMask() {
        return IoHandler.IO_Read(Int10.VGAREG_PEL_MASK);
    }

    public static void INT10_SetBackgroundBorder(/*Bit8u*/short val) {
        /*Bit8u*/int color_select=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL);
        color_select=(short)((color_select & 0xe0) | (val & 0x1f));
        Memory.real_writeb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAL, color_select);

        if (Dosbox.machine == MachineType.MCH_CGA || Dosbox.machine == MachineType.MCH_TANDY)
            IoHandler.IO_Write(0x3d9,color_select);
        else if (Dosbox.machine == MachineType.MCH_PCJR) {
            IoHandler.IO_Read(Int10.VGAREG_TDY_RESET); // reset the flipflop
            if (VGA.vga.mode!=VGA.M_TANDY_TEXT) {
                IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS, 0x10);
                IoHandler.IO_Write(Int10.VGAREG_PCJR_DATA, color_select&0xf);
            }
            IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS, 0x2); // border color
            IoHandler.IO_Write(Int10.VGAREG_PCJR_DATA, color_select&0xf);
        }
        else if (Dosbox.IS_EGAVGA_ARCH()) {
            val = (short)(((val << 1) & 0x10) | (val & 0x7));
            /* Always set the overscan color */
            INT10_SetSinglePaletteRegister( (short)0x11, val );
            /* Don't set any extra colors when in text mode */
            if (Int10_modes.CurMode.mode <= 3)
                return;
            INT10_SetSinglePaletteRegister( (short)0, val );
            val = (short)((color_select & 0x10) | 2 | ((color_select & 0x20) >> 5));
            INT10_SetSinglePaletteRegister( (short)1, val );
            val+=2;
            INT10_SetSinglePaletteRegister( (short)2, val );
            val+=2;
            INT10_SetSinglePaletteRegister((short) 3, val);
        }
    }

    public static void INT10_SetColorSelect(/*Bit8u*/short val) {
        /*Bit8u*/int temp=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL);
        temp=((temp & 0xdf) | ((val & 1)!=0 ? 0x20 : 0x0));
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,temp);
        if (Dosbox.machine == MachineType.MCH_CGA || Dosbox.machine == MachineType.MCH_TANDY)
            IoHandler.IO_Write(0x3d9,temp);
        else if (Dosbox.machine ==  MachineType.MCH_PCJR) {
            IoHandler.IO_Read(Int10.VGAREG_TDY_RESET); // reset the flipflop
            switch(VGA.vga.mode) {
            case VGA.M_TANDY2:
                IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS, 0x11);
                IoHandler.IO_Write(Int10.VGAREG_PCJR_DATA, ((val & 1) != 0) ? 0xf : 0);
                break;
            case VGA.M_TANDY4:
                final byte[] t4_table = new byte[]{0,2,4,6, 0,3,5,0xf};
                for(int i = 0x11; i < 0x14; i++) {
                    IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS, i);
                    IoHandler.IO_Write(Int10.VGAREG_PCJR_DATA, t4_table[(i - 0x10) + (((val & 1) != 0) ? 4 : 0)]);
                }
                break;
            default:
                // 16-color modes: always write the same palette
                for(int i = 0x11; i < 0x20; i++) {
                    IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS, i);
                    IoHandler.IO_Write(Int10.VGAREG_PCJR_DATA, i - 0x10);
                }
                break;
            }
            IoHandler.IO_Write(Int10.VGAREG_TDY_ADDRESS, 0); // enable palette
        } else if (Dosbox.IS_EGAVGA_ARCH()) {
            if (Int10_modes.CurMode.mode <= 3) //Maybe even skip the total function!
                return;
            val = (short)((temp & 0x10) | 2 | val);
            INT10_SetSinglePaletteRegister( (short)1, val );
            val+=2;
            INT10_SetSinglePaletteRegister( (short)2, val );
            val+=2;
            INT10_SetSinglePaletteRegister( (short)3, val );
        }
    }

    public static void INT10_PerformGrayScaleSumming(/*Bit16u*/int start_reg,/*Bit16u*/int count) {
        if (count>0x100) count=0x100;
        for (/*Bitu*/int ct=0; ct<count; ct++) {
            IoHandler.IO_Write(Int10.VGAREG_DAC_READ_ADDRESS,(start_reg+ct));
            /*Bit8u*/short red=IoHandler.IO_Read(Int10.VGAREG_DAC_DATA);
            /*Bit8u*/short green=IoHandler.IO_Read(Int10.VGAREG_DAC_DATA);
            /*Bit8u*/short blue=IoHandler.IO_Read(Int10.VGAREG_DAC_DATA);

            /* calculate clamped intensity, taken from VGABIOS */
            /*Bit32u*/int i=(( 77*red + 151*green + 28*blue ) + 0x80) >> 8;
            /*Bit8u*/short ic=(short)((i>0x3f) ? 0x3f : ((i & 0xff)));
            INT10_SetSingleDACRegister((short) (start_reg + ct), ic, ic, ic);
        }
    }
}
