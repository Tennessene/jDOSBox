package jdos.ints;

import jdos.Dosbox;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.hardware.VGA;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class Int10_put_pixel {
    static private short[] cga_masks={0x3f,0xcf,0xf3,0xfc};
    static private short[] cga_masks2={0x7f,0xbf,0xdf,0xef,0xf7,0xfb,0xfd,0xfe};

    static private boolean putpixelwarned = false;
    public static void INT10_PutPixel(/*Bit16u*/int x,/*Bit16u*/int y,/*Bit8u*/short page,/*Bit8u*/short color) {

        switch (Int10_modes.CurMode.type) {
        case VGA.M_CGA4:
            {
                    if (Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MODE)<=5) {
                        /*Bit16u*/int off=(y>>1)*80+(x>>2);
                        if ((y&1)!=0) off+=8*1024;

                        /*Bit8u*/short old=Memory.real_readb(0xb800,off);
                        if ((color & 0x80)!=0) {
                            color&=3;
                            old^=color << (2*(3-(x&3)));
                        } else {
                            old=(short)((old&cga_masks[x&3])|((color&3) << (2*(3-(x&3)))));
                        }
                        Memory.real_writeb(0xb800,off,old);
                    } else {
                        /*Bit16u*/int off=(y>>2)*160+((x>>2)&(~1));
                        off+=(8*1024) * (y & 3);

                        /*Bit16u*/int old=Memory.real_readw(0xb800,off);
                        if ((color & 0x80)!=0) {
                            old^=(color&1) << (7-(x&7));
                            old^=((color&2)>>1) << ((7-(x&7))+8);
                        } else {
                            old=(old&(~(0x101<<(7-(x&7))))) | ((color&1) << (7-(x&7))) | (((color&2)>>1) << ((7-(x&7))+8));
                        }
                        Memory.real_writew(0xb800,off,old);
                    }
            }
            break;
        case VGA.M_CGA2:
            {
                    /*Bit16u*/int off=(y>>1)*80+(x>>3);
                    if ((y&1)!=0) off+=8*1024;
                    /*Bit8u*/short old=Memory.real_readb(0xb800,off);
                    if ((color & 0x80)!=0) {
                        color&=1;
                        old^=color << ((7-(x&7)));
                    } else {
                        old=(short)((old&cga_masks2[x&7])|((color&1) << ((7-(x&7)))));
                    }
                    Memory.real_writeb(0xb800,off,old);
            }
            break;
        case VGA.M_TANDY16:
            {
                IoHandler.IO_Write(0x3d4,(byte)0x09);
                /*Bit8u*/short scanlines_m1=IoHandler.IO_Read(0x3d5);
                /*Bit16u*/int off=(y>>((scanlines_m1==1)?1:2))*(Int10_modes.CurMode.swidth>>1)+(x>>1);
                off+=(8*1024) * (y & scanlines_m1);
                /*Bit8u*/short old=Memory.real_readb(0xb800,off);
                /*Bit8u*/short[] p=new short[2];
                p[1] = (short)((old >> 4) & 0xf);
                p[0] = (short)(old & 0xf);
                /*Bitu*/int ind = 1-(x & 0x1);

                if ((color & 0x80)!=0) {
                    p[ind]^=(color & 0x7f);
                } else {
                    p[ind]=color;
                }
                old = (short)((p[1] << 4) | p[0]);
                Memory.real_writeb(0xb800,off,old);
            }
            break;
        case VGA.M_LIN4:
            if ((Dosbox.machine!= MachineType.MCH_VGA) || (Dosbox.svgaCard!= SVGACards.SVGA_TsengET4K) ||
                    (Int10_modes.CurMode.swidth>800)) {
                // the ET4000 BIOS supports text output in 800x600 SVGA (Gateway 2)
                // putpixel warining?
                break;
            }
        case VGA.M_EGA:
            {
                /* Set the correct bitmask for the pixel position */
                IoHandler.IO_Write(0x3ce,0x8);/*Bit8u*/short mask=(short)(128>>(x&7));IoHandler.IO_Write(0x3cf,mask);
                /* Set the color to set/reset register */
                IoHandler.IO_Write(0x3ce,0x0);IoHandler.IO_Write(0x3cf,color);
                /* Enable all the set/resets */
                IoHandler.IO_Write(0x3ce,0x1);IoHandler.IO_Write(0x3cf,0xf);
                /* test for xorring */
                if ((color & 0x80)!=0) { IoHandler.IO_Write(0x3ce,0x3);IoHandler.IO_Write(0x3cf,0x18); }
                //Perhaps also set mode 1
                /* Calculate where the pixel is in video memory */
                if (Int10_modes.CurMode.plength!=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE))
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR,"PutPixel_EGA_p: "+Integer.toString(Int10_modes.CurMode.plength, 16)+"!="+Integer.toString(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE),16));
                if (Int10_modes.CurMode.swidth!=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8)
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"PutPixel_EGA_w: "+Integer.toString(Int10_modes.CurMode.swidth, 16)+"!="+Integer.toString(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8, 16));
                /*PhysPt*/int off=0xa0000+Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE)*page+
                    ((y*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8+x)>>3);
                /* Bitmask and set/reset should do the rest */
                Memory.mem_readb(off);
                Memory.mem_writeb(off,0xff);
                /* Restore bitmask */
                IoHandler.IO_Write(0x3ce,0x8);IoHandler.IO_Write(0x3cf,0xff);
                IoHandler.IO_Write(0x3ce,0x1);IoHandler.IO_Write(0x3cf,0);
                /* Restore write operating if changed */
                if ((color & 0x80)!=0) { IoHandler.IO_Write(0x3ce,0x3);IoHandler.IO_Write(0x3cf,0x0); }
                break;
            }

        case VGA.M_VGA:
            Memory.mem_writeb(Memory.PhysMake(0xa000,y*320+x),color);
            break;
        case VGA.M_LIN8: {
                if (Int10_modes.CurMode.swidth!=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8)
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"PutPixel_VGA_w: "+Integer.toString(Int10_modes.CurMode.swidth, 16)+"!="+Integer.toString(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8,16));
                /*PhysPt*/int off=Int10.S3_LFB_BASE+y*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8+x;
                Memory.mem_writeb(off,color);
                break;
            }
        default:
            if(!putpixelwarned) {
                putpixelwarned = true;
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"PutPixel unhandled mode type "+Int10_modes.CurMode.type);
            }
            break;
        }
    }

    public static short INT10_GetPixel(/*Bit16u*/int x,/*Bit16u*/int y,/*Bit8u*/short page) {
        int color = 0;
        switch (Int10_modes.CurMode.type) {
        case VGA.M_CGA4:
            {
                /*Bit16u*/int off=(y>>1)*80+(x>>2);
                if ((y&1)!=0) off+=8*1024;
                /*Bit8u*/short val=Memory.real_readb(0xb800,off);
                color=(val>>(((3-(x&3)))*2)) & 3 ;
            }
            break;
        case VGA.M_CGA2:
            {
                /*Bit16u*/int off=(y>>1)*80+(x>>3);
                if ((y&1)!=0) off+=8*1024;
                /*Bit8u*/short val=Memory.real_readb(0xb800,off);
                color=(val>>(((7-(x&7))))) & 1 ;
            }
            break;
        case VGA.M_EGA:
            {
                /* Calculate where the pixel is in video memory */
                if (Int10_modes.CurMode.plength!=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE))
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"GetPixel_EGA_p: "+Integer.toString(Int10_modes.CurMode.plength, 16)+"!="+Integer.toString(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE),16));
                if (Int10_modes.CurMode.swidth!=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8)
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"GetPixel_EGA_w: "+Integer.toString(Int10_modes.CurMode.swidth, 16)+"!="+Integer.toString(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8,16));
                /*PhysPt*/int off=0xa0000+Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE)*page+
                    ((y*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8+x)>>3);
                /*Bitu*/int shift=7-(x & 7);
                /* Set the read map */
                color=0;
                IoHandler.IO_Write(0x3ce,0x4);IoHandler.IO_Write(0x3cf,0);
                color|=((Memory.mem_readb(off)>>shift) & 1) << 0;
                IoHandler.IO_Write(0x3ce,0x4);IoHandler.IO_Write(0x3cf,1);
                color|=((Memory.mem_readb(off)>>shift) & 1) << 1;
                IoHandler.IO_Write(0x3ce,0x4);IoHandler.IO_Write(0x3cf,2);
                color|=((Memory.mem_readb(off)>>shift) & 1) << 2;
                IoHandler.IO_Write(0x3ce,0x4);IoHandler.IO_Write(0x3cf,3);
                color|=((Memory.mem_readb(off)>>shift) & 1) << 3;
                break;
            }
        case VGA.M_VGA:
            color=Memory.mem_readb(Memory.PhysMake(0xa000,320*y+x));
            break;
        case VGA.M_LIN8: {
                if (Int10_modes.CurMode.swidth!=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8)
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"GetPixel_VGA_w: "+Integer.toString(Int10_modes.CurMode.swidth, 16)+"!="+Integer.toString(Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8,16));
                /*PhysPt*/int off=Int10.S3_LFB_BASE+y*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)*8+x;
                color = Memory.mem_readb(off);
                break;
            }
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"GetPixel unhandled mode type "+Int10_modes.CurMode.type);
            break;
        }
        return (short)color;
    }
}
