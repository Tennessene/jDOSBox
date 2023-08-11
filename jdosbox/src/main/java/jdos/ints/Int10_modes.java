package jdos.ints;

import jdos.Dosbox;
import jdos.hardware.*;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class Int10_modes {
    static public final int _EGA_HALF_CLOCK		=0x0001;
    static public final int  _EGA_LINE_DOUBLE	=0x0002;
    static public final int  _VGA_PIXEL_DOUBLE	=0x0004;

    static public final int  SEQ_REGS =0x05;
    static public final int  GFX_REGS =0x09;
    static public final int  ATT_REGS =0x15;

    public static Int10.VideoModeBlock ModeList_VGA[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde special flags */
        new Int10.VideoModeBlock( 0x000  , VGA.M_TEXT   ,360 ,400 ,40 ,25 ,9 ,16 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,360 ,400 ,40 ,25 ,9 ,16 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),
        new Int10.VideoModeBlock( 0x004  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x005  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x006  ,VGA.M_CGA2   ,640 ,200 ,80 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,100 ,449 ,80 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x007  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB0000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),

        new Int10.VideoModeBlock( 0x00D  ,VGA.M_EGA    ,320 ,200 ,40 ,25 ,8 ,8  ,8 ,0xA0000 ,0x2000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE	),
        new Int10.VideoModeBlock( 0x00E  ,VGA.M_EGA    ,640 ,200 ,80 ,25 ,8 ,8  ,4 ,0xA0000 ,0x4000 ,100 ,449 ,80 ,400 ,_EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x00F  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,100 ,449 ,80 ,350 ,0	),/*was EGA_2*/
        new Int10.VideoModeBlock( 0x010  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,100 ,449 ,80 ,350 ,0	),
        new Int10.VideoModeBlock( 0x011  ,VGA.M_EGA    ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 ,0	),/*was EGA_2 */
        new Int10.VideoModeBlock( 0x012  ,VGA.M_EGA    ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 ,0	),
        new Int10.VideoModeBlock( 0x013  ,VGA.M_VGA    ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x2000 ,100 ,449 ,80 ,400 ,0   ),

        new Int10.VideoModeBlock( 0x054  ,VGA.M_TEXT   ,1056,688, 132,43, 8, 16, 1 ,0xB8000 ,0x4000, 192, 800, 132,688, 0   ),
        new Int10.VideoModeBlock( 0x055  ,VGA.M_TEXT   ,1056,400, 132,25, 8, 16, 1 ,0xB8000 ,0x2000, 192, 449, 132,400, 0   ),

        /* Alias of mode 101 */
        new Int10.VideoModeBlock( 0x069  ,VGA.M_LIN8   ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,525 ,80 ,480 ,0	),
        /* Alias of mode 102 */
        new Int10.VideoModeBlock( 0x06A  ,VGA.M_LIN4   ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,128 ,663 ,100,600 ,0	),

        /* Follow vesa 1.2 for first 0x20 */
        new Int10.VideoModeBlock( 0x100  ,VGA.M_LIN8   ,640 ,400 ,80 ,25 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,449 ,80 ,400 ,0   ),
        new Int10.VideoModeBlock( 0x101  ,VGA.M_LIN8   ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,525 ,80 ,480 ,0	),
        new Int10.VideoModeBlock( 0x102  ,VGA.M_LIN4   ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,132 ,628 ,100,600 ,0	),
        new Int10.VideoModeBlock( 0x103  ,VGA.M_LIN8   ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,132 ,628 ,100,600 ,0	),
        new Int10.VideoModeBlock( 0x104  ,VGA.M_LIN4   ,1024,768 ,128,48 ,8 ,16 ,1 ,0xA0000 ,0x10000,168 ,806 ,128,768 ,0	),
        new Int10.VideoModeBlock( 0x105  ,VGA.M_LIN8   ,1024,768 ,128,48 ,8 ,16 ,1 ,0xA0000 ,0x10000,168 ,806 ,128,768 ,0	),
        new Int10.VideoModeBlock( 0x106  ,VGA.M_LIN4   ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,212 ,1066,160,1024,0	),
        new Int10.VideoModeBlock( 0x107  ,VGA.M_LIN8   ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,212 ,1066,160,1024,0	),

        /* VESA text modes */
        new Int10.VideoModeBlock( 0x108  ,VGA.M_TEXT   ,640 ,480,  80,60, 8,  8 ,2 ,0xB8000 ,0x4000, 100 ,525 ,80 ,480 ,0   ),
        new Int10.VideoModeBlock( 0x109  ,VGA.M_TEXT   ,1056,400, 132,25, 8, 16, 1 ,0xB8000 ,0x2000, 160, 449, 132,400, 0   ),
        new Int10.VideoModeBlock( 0x10A  ,VGA.M_TEXT   ,1056,688, 132,43, 8,  8, 1 ,0xB8000 ,0x4000, 160, 449, 132,344, 0   ),
        new Int10.VideoModeBlock( 0x10B  ,VGA.M_TEXT   ,1056,400, 132,50, 8,  8, 1 ,0xB8000 ,0x4000, 160, 449, 132,400, 0   ),
        new Int10.VideoModeBlock( 0x10C  ,VGA.M_TEXT   ,1056,480, 132,60, 8,  8, 2 ,0xB8000 ,0x4000, 160, 531, 132,480, 0   ),

    /* VESA higher color modes */
        new Int10.VideoModeBlock( 0x10D  ,VGA.M_LIN15  ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,449 ,40 ,400 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x10E  ,VGA.M_LIN16  ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,449 ,40 ,400 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x10F  ,VGA.M_LIN32  ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x10000,50  ,449 ,40 ,400 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x110  ,VGA.M_LIN15  ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,200 ,525 ,80,480 ,0   ),
        new Int10.VideoModeBlock( 0x111  ,VGA.M_LIN16  ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,200 ,525 ,80,480 ,0   ),
        new Int10.VideoModeBlock( 0x112  ,VGA.M_LIN32  ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,525 ,80 ,480 ,0   ),
        new Int10.VideoModeBlock( 0x113  ,VGA.M_LIN15  ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,264 ,628 ,100,600 ,0   ),
        new Int10.VideoModeBlock( 0x114  ,VGA.M_LIN16  ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,264 ,628 ,100,600 ,0   ),
        new Int10.VideoModeBlock( 0x115  ,VGA.M_LIN32  ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,132 ,628 ,100,600 ,0   ),

        new Int10.VideoModeBlock( 0x116  ,VGA.M_LIN15  ,1024,768 ,128,48 ,8 ,16 ,1 ,0xA0000 ,0x10000,336 ,806 ,128,768 ,0	),
        new Int10.VideoModeBlock( 0x117  ,VGA.M_LIN16  ,1024,768 ,128,48 ,8 ,16 ,1 ,0xA0000 ,0x10000,336 ,806 ,128,768 ,0	),
        new Int10.VideoModeBlock( 0x118  ,VGA.M_LIN32  ,1024,768 ,128,48 ,8 ,16 ,1 ,0xA0000 ,0x10000,168 ,806 ,128,768 ,0	),
        /* those should be interlaced but ok */
        //new Int10.VideoModeBlock( 0x119  ,VGA.M_LIN15  ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,424 ,1066,320,1024,0	),
        //new Int10.VideoModeBlock( 0x11A  ,VGA.M_LIN16  ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,424 ,1066,320,1024,0	),

        new Int10.VideoModeBlock( 0x150  ,VGA.M_LIN8   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,449 ,80 ,400 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x151  ,VGA.M_LIN8   ,320 ,240 ,40 ,30 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,525 ,80 ,480 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x152  ,VGA.M_LIN8   ,320 ,400 ,40 ,50 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,449 ,80 ,400 , _VGA_PIXEL_DOUBLE  ),
        new Int10.VideoModeBlock( 0x153  ,VGA.M_LIN8   ,320 ,480 ,40 ,60 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,525 ,80 ,480 , _VGA_PIXEL_DOUBLE  ),

        new Int10.VideoModeBlock( 0x160  ,VGA.M_LIN15  ,320 ,240 ,40 ,30 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,525 , 80 ,480 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x161  ,VGA.M_LIN15  ,320 ,400 ,40 ,50 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,449 , 80 ,400 , _VGA_PIXEL_DOUBLE  ),
        new Int10.VideoModeBlock( 0x162  ,VGA.M_LIN15  ,320 ,480 ,40 ,60 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,525 , 80 ,480 , _VGA_PIXEL_DOUBLE  ),
        new Int10.VideoModeBlock( 0x165  ,VGA.M_LIN15  ,640 ,400 ,80 ,25 ,8 ,16 ,1 ,0xA0000 ,0x10000,200 ,449 ,160 ,400 ,0   ),

        new Int10.VideoModeBlock( 0x170  ,VGA.M_LIN16  ,320 ,240 ,40 ,30 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,525 , 80 ,480 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x171  ,VGA.M_LIN16  ,320 ,400 ,40 ,50 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,449 , 80 ,400 , _VGA_PIXEL_DOUBLE  ),
        new Int10.VideoModeBlock( 0x172  ,VGA.M_LIN16  ,320 ,480 ,40 ,60 ,8 ,8  ,1 ,0xA0000 ,0x10000,100 ,525 , 80 ,480 , _VGA_PIXEL_DOUBLE  ),
        new Int10.VideoModeBlock( 0x175  ,VGA.M_LIN16  ,640 ,400 ,80 ,25 ,8 ,16 ,1 ,0xA0000 ,0x10000,200 ,449 ,160 ,400 ,0   ),

        new Int10.VideoModeBlock( 0x190  ,VGA.M_LIN32  ,320 ,240 ,40 ,30 ,8 ,8  ,1 ,0xA0000 ,0x10000, 50 ,525 ,40 ,480 , _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x191  ,VGA.M_LIN32  ,320 ,400 ,40 ,50 ,8 ,8  ,1 ,0xA0000 ,0x10000, 50 ,449 ,40 ,400 , _VGA_PIXEL_DOUBLE  ),
        new Int10.VideoModeBlock( 0x192  ,VGA.M_LIN32  ,320 ,480 ,40 ,60 ,8 ,8  ,1 ,0xA0000 ,0x10000, 50 ,525 ,40 ,480 , _VGA_PIXEL_DOUBLE  ),

        /* S3 specific modes */
        new Int10.VideoModeBlock( 0x207  ,VGA.M_LIN8	,1152,864,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,182 ,948 ,144,864 ,0	),
        new Int10.VideoModeBlock( 0x209  ,VGA.M_LIN15	,1152,864,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,364 ,948 ,288,864 ,0	),
        new Int10.VideoModeBlock( 0x20A  ,VGA.M_LIN16	,1152,864,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,364 ,948 ,288,864 ,0	),
        //new Int10.VideoModeBlock( 0x20B  ,VGA.M_LIN32	,1152,864,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,182 ,948 ,144,864 ,0	),
        new Int10.VideoModeBlock( 0x213  ,VGA.M_LIN32   ,640 ,400,80 ,25 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,449 ,80 ,400 ,0	),

        /* Some custom modes */
        //new Int10.VideoModeBlock( 0x220  ,VGA.M_LIN32  ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,212 ,1066,160,1024,0	),
        // A nice 16:9 mode
        new Int10.VideoModeBlock( 0x222  ,VGA.M_LIN8   ,848 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,132 ,525 ,106 ,480 ,0	),
        new Int10.VideoModeBlock( 0x223  ,VGA.M_LIN15  ,848 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,264 ,525 ,212 ,480 ,0  ),
        new Int10.VideoModeBlock( 0x224  ,VGA.M_LIN16  ,848 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,264 ,525 ,212 ,480 ,0  ),
        new Int10.VideoModeBlock( 0x225  ,VGA.M_LIN32  ,848 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,132 ,525 ,106 ,480 ,0  ),

        new Int10.VideoModeBlock(0xFFFF  ,VGA.M_ERROR  ,0   ,0   ,0  ,0  ,0 ,0  ,0 ,0x00000 ,0x0000 ,0   ,0   ,0  ,0   ,0 	),
    };

    private static Int10.VideoModeBlock ModeList_VGA_Text_200lines[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde special flags */
        new Int10.VideoModeBlock( 0x000  ,VGA.M_TEXT   ,320 ,200 ,40 ,25 ,8 , 8 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,320 ,200 ,40 ,25 ,8 , 8 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,640 ,200 ,80 ,25 ,8 , 8 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,_EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,640 ,200 ,80 ,25 ,8 , 8 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,_EGA_LINE_DOUBLE )
    };

    private static Int10.VideoModeBlock ModeList_VGA_Text_350lines[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde special flags */
        new Int10.VideoModeBlock( 0x000  ,VGA.M_TEXT   ,320 ,350 ,40 ,25 ,8 ,14 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,350 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,320 ,350 ,40 ,25 ,8 ,14 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,350 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,640 ,350 ,80 ,25 ,8 ,14 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,350 ,0	),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,640 ,350 ,80 ,25 ,8 ,14 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,350 ,0	)
    };

    private static Int10.VideoModeBlock ModeList_VGA_Tseng[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde special flags */
        new Int10.VideoModeBlock( 0x000  ,VGA.M_TEXT   ,360 ,400 ,40 ,25 ,9 ,16 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,360 ,400 ,40 ,25 ,9 ,16 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),
        new Int10.VideoModeBlock( 0x004  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x005  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x006  ,VGA.M_CGA2   ,640 ,200 ,80 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,100 ,449 ,80 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x007  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB0000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),

        new Int10.VideoModeBlock( 0x00D  ,VGA.M_EGA    ,320 ,200 ,40 ,25 ,8 ,8  ,8 ,0xA0000 ,0x2000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE	),
        new Int10.VideoModeBlock( 0x00E  ,VGA.M_EGA    ,640 ,200 ,80 ,25 ,8 ,8  ,4 ,0xA0000 ,0x4000 ,100 ,449 ,80 ,400 ,_EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x00F  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,100 ,449 ,80 ,350 ,0	),/*was EGA_2*/
        new Int10.VideoModeBlock( 0x010  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,100 ,449 ,80 ,350 ,0	),
        new Int10.VideoModeBlock( 0x011  ,VGA.M_EGA    ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 ,0	),/*was EGA_2 */
        new Int10.VideoModeBlock( 0x012  ,VGA.M_EGA    ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 ,0	),
        new Int10.VideoModeBlock( 0x013  ,VGA.M_VGA    ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x2000 ,100 ,449 ,80 ,400 ,0   ),

        new Int10.VideoModeBlock( 0x018  ,VGA.M_TEXT   ,1056 ,688, 132,44, 8, 8, 1 ,0xB0000 ,0x4000, 192, 800, 132, 704, 0 ),
        new Int10.VideoModeBlock( 0x019  ,VGA.M_TEXT   ,1056 ,400, 132,25, 8, 16,1 ,0xB0000 ,0x2000, 192, 449, 132, 400, 0 ),
        new Int10.VideoModeBlock( 0x01A  ,VGA.M_TEXT   ,1056 ,400, 132,28, 8, 16,1 ,0xB0000 ,0x2000, 192, 449, 132, 448, 0 ),
        new Int10.VideoModeBlock( 0x022  ,VGA.M_TEXT   ,1056 ,688, 132,44, 8, 8, 1 ,0xB8000 ,0x4000, 192, 800, 132, 704, 0 ),
        new Int10.VideoModeBlock( 0x023  ,VGA.M_TEXT   ,1056 ,400, 132,25, 8, 16,1 ,0xB8000 ,0x2000, 192, 449, 132, 400, 0 ),
        new Int10.VideoModeBlock( 0x024  ,VGA.M_TEXT   ,1056 ,400, 132,28, 8, 16,1 ,0xB8000 ,0x2000, 192, 449, 132, 448, 0 ),
        new Int10.VideoModeBlock( 0x025  ,VGA.M_LIN4   ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 , 0 ),
        new Int10.VideoModeBlock( 0x029  ,VGA.M_LIN4   ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0xA000, 128 ,663 ,100,600 , 0 ),
        new Int10.VideoModeBlock( 0x02D  ,VGA.M_LIN8   ,640 ,350 ,80 ,21 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,449 ,80 ,350 , 0 ),
        new Int10.VideoModeBlock( 0x02E  ,VGA.M_LIN8   ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,525 ,80 ,480 , 0 ),
        new Int10.VideoModeBlock( 0x02F  ,VGA.M_LIN8   ,640 ,400 ,80 ,25 ,8 ,16 ,1 ,0xA0000 ,0x10000,100 ,449 ,80 ,400 , 0 ),/* ET4000 only */
        new Int10.VideoModeBlock( 0x030  ,VGA.M_LIN8   ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0x10000,128 ,663 ,100,600 , 0 ),
        new Int10.VideoModeBlock( 0x036  ,VGA.M_LIN4   ,960 , 720,120,45 ,8 ,16 ,1 ,0xA0000 ,0xA000, 120 ,800 ,120,720 , 0 ),/* STB only */
        new Int10.VideoModeBlock( 0x037  ,VGA.M_LIN4   ,1024, 768,128,48 ,8 ,16 ,1 ,0xA0000 ,0xA000, 128 ,800 ,128,768 , 0 ),
        new Int10.VideoModeBlock( 0x038  ,VGA.M_LIN8   ,1024 ,768,128,48 ,8 ,16 ,1 ,0xA0000 ,0x10000,128 ,800 ,128,768 , 0 ),/* ET4000 only */
        new Int10.VideoModeBlock( 0x03D  ,VGA.M_LIN4   ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0xA000, 160 ,1152,160,1024, 0 ),/* newer ET4000 */
        new Int10.VideoModeBlock( 0x03E  ,VGA.M_LIN4   ,1280, 960,160,60 ,8 ,16 ,1 ,0xA0000 ,0xA000, 160 ,1024,160,960 , 0 ),/* Definicon only */
        new Int10.VideoModeBlock( 0x06A  ,VGA.M_LIN4   ,800 ,600 ,100,37 ,8 ,16 ,1 ,0xA0000 ,0xA000, 128 ,663 ,100,600 , 0 ),/* newer ET4000 */

        new Int10.VideoModeBlock(0xFFFF  ,VGA.M_ERROR  ,0   ,0   ,0  ,0  ,0 ,0  ,0 ,0x00000 ,0x0000 ,0   ,0   ,0  ,0   ,0 	),
    };

    private static Int10.VideoModeBlock ModeList_VGA_Paradise[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde special flags */
        new Int10.VideoModeBlock( 0x000  ,VGA.M_TEXT   ,360 ,400 ,40 ,25 ,9 ,16 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,360 ,400 ,40 ,25 ,9 ,16 ,8 ,0xB8000 ,0x0800 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB8000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),
        new Int10.VideoModeBlock( 0x004  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x005  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x006  ,VGA.M_CGA2   ,640 ,200 ,80 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,100 ,449 ,80 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x007  ,VGA.M_TEXT   ,720 ,400 ,80 ,25 ,9 ,16 ,8 ,0xB0000 ,0x1000 ,100 ,449 ,80 ,400 ,0	),

        new Int10.VideoModeBlock( 0x00D  ,VGA.M_EGA    ,320 ,200 ,40 ,25 ,8 ,8  ,8 ,0xA0000 ,0x2000 ,50  ,449 ,40 ,400 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE	),
        new Int10.VideoModeBlock( 0x00E  ,VGA.M_EGA    ,640 ,200 ,80 ,25 ,8 ,8  ,4 ,0xA0000 ,0x4000 ,100 ,449 ,80 ,400 ,_EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x00F  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,100 ,449 ,80 ,350 ,0	),/*was EGA_2*/
        new Int10.VideoModeBlock( 0x010  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,100 ,449 ,80 ,350 ,0	),
        new Int10.VideoModeBlock( 0x011  ,VGA.M_EGA    ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 ,0	),/*was EGA_2 */
        new Int10.VideoModeBlock( 0x012  ,VGA.M_EGA    ,640 ,480 ,80 ,30 ,8 ,16 ,1 ,0xA0000 ,0xA000 ,100 ,525 ,80 ,480 ,0	),
        new Int10.VideoModeBlock( 0x013  ,VGA.M_VGA    ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xA0000 ,0x2000 ,100 ,449 ,80 ,400 ,0   ),

        new Int10.VideoModeBlock( 0x054  ,VGA.M_TEXT   ,1056 ,688, 132,43, 8, 9, 1, 0xB0000, 0x4000, 192, 720, 132,688, 0 ),
        new Int10.VideoModeBlock( 0x055  ,VGA.M_TEXT   ,1056 ,400, 132,25, 8, 16,1, 0xB0000, 0x2000, 192, 449, 132,400, 0 ),
        new Int10.VideoModeBlock( 0x056  ,VGA.M_TEXT   ,1056 ,688, 132,43, 8, 9, 1, 0xB0000, 0x4000, 192, 720, 132,688, 0 ),
        new Int10.VideoModeBlock( 0x057  ,VGA.M_TEXT   ,1056 ,400, 132,25, 8, 16,1, 0xB0000, 0x2000, 192, 449, 132,400, 0 ),
        new Int10.VideoModeBlock( 0x058  ,VGA.M_LIN4   ,800 , 600, 100,37, 8, 16,1, 0xA0000, 0xA000, 128 ,663 ,100,600, 0 ),
        new Int10.VideoModeBlock( 0x05D  ,VGA.M_LIN4   ,1024, 768, 128,48 ,8, 16,1, 0xA0000, 0x10000,128 ,800 ,128,768 ,0 ), // documented only on C00 upwards
        new Int10.VideoModeBlock( 0x05E  ,VGA.M_LIN8   ,640 , 400, 80 ,25, 8, 16,1, 0xA0000, 0x10000,100 ,449 ,80 ,400, 0 ),
        new Int10.VideoModeBlock( 0x05F  ,VGA.M_LIN8   ,640 , 480, 80 ,30, 8, 16,1, 0xA0000, 0x10000,100 ,525 ,80 ,480, 0 ),

        new Int10.VideoModeBlock(0xFFFF  ,VGA.M_ERROR  ,0   ,0   ,0  ,0  ,0 ,0  ,0 ,0x00000 ,0x0000 ,0   ,0   ,0  ,0   ,0 	),
    };


    private static Int10.VideoModeBlock ModeList_EGA[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde special flags */
        new Int10.VideoModeBlock( 0x000  ,VGA.M_TEXT   ,320 ,350 ,40 ,25 ,8 ,14 ,8 ,0xB8000 ,0x0800 ,50  ,366 ,40 ,350 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,320 ,350 ,40 ,25 ,8 ,14 ,8 ,0xB8000 ,0x0800 ,50  ,366 ,40 ,350 ,_EGA_HALF_CLOCK	),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,640 ,350 ,80 ,25 ,8 ,14 ,8 ,0xB8000 ,0x1000 ,96  ,366 ,80 ,350 ,0	),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,640 ,350 ,80 ,25 ,8 ,14 ,8 ,0xB8000 ,0x1000 ,96  ,366 ,80 ,350 ,0	),
        new Int10.VideoModeBlock( 0x004  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,60  ,262 ,40 ,200 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x005  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,60  ,262 ,40 ,200 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x006  ,VGA.M_CGA2   ,640 ,200 ,80 ,25 ,8 ,8  ,1 ,0xB8000 ,0x4000 ,120 ,262 ,80 ,200 ,_EGA_LINE_DOUBLE),
        new Int10.VideoModeBlock( 0x007  ,VGA.M_TEXT   ,720 ,350 ,80 ,25 ,9 ,14 ,8 ,0xB0000 ,0x1000 ,120 ,440 ,80 ,350 ,0	),

        new Int10.VideoModeBlock( 0x00D  ,VGA.M_EGA    ,320 ,200 ,40 ,25 ,8 ,8  ,8 ,0xA0000 ,0x2000 ,60  ,262 ,40 ,200 ,_EGA_HALF_CLOCK	| _EGA_LINE_DOUBLE	),
        new Int10.VideoModeBlock( 0x00E  ,VGA.M_EGA    ,640 ,200 ,80 ,25 ,8 ,8  ,4 ,0xA0000 ,0x4000 ,120 ,262 ,80 ,200 ,_EGA_LINE_DOUBLE ),
        new Int10.VideoModeBlock( 0x00F  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,96  ,366 ,80 ,350 ,0	),/*was EGA_2*/
        new Int10.VideoModeBlock( 0x010  ,VGA.M_EGA    ,640 ,350 ,80 ,25 ,8 ,14 ,2 ,0xA0000 ,0x8000 ,96  ,366 ,80 ,350 ,0	),

        new Int10.VideoModeBlock(0xFFFF  ,VGA.M_ERROR  ,0   ,0   ,0  ,0  ,0 ,0  ,0 ,0x00000 ,0x0000 ,0   ,0   ,0  ,0   ,0 	),
    };

    private static Int10.VideoModeBlock ModeList_OTHER[]={
        /* mode  ,type     ,sw  ,sh  ,tw ,th ,cw,ch ,pt,pstart  ,plength,htot,vtot,hde,vde ,special flags */
        new Int10.VideoModeBlock( 0x000  ,VGA.M_TEXT   ,320 ,400 ,40 ,25 ,8 ,8  ,8 ,0xB8000 ,0x0800 ,56  ,31  ,40 ,25  ,0   ),
        new Int10.VideoModeBlock( 0x001  ,VGA.M_TEXT   ,320 ,400 ,40 ,25 ,8 ,8  ,8 ,0xB8000 ,0x0800 ,56  ,31  ,40 ,25  ,0	),
        new Int10.VideoModeBlock( 0x002  ,VGA.M_TEXT   ,640 ,400 ,80 ,25 ,8 ,8  ,4 ,0xB8000 ,0x1000 ,113 ,31  ,80 ,25  ,0	),
        new Int10.VideoModeBlock( 0x003  ,VGA.M_TEXT   ,640 ,400 ,80 ,25 ,8 ,8  ,4 ,0xB8000 ,0x1000 ,113 ,31  ,80 ,25  ,0	),
        new Int10.VideoModeBlock( 0x004  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,4 ,0xB8000 ,0x0800 ,56  ,127 ,40 ,100 ,0   ),
        new Int10.VideoModeBlock( 0x005  ,VGA.M_CGA4   ,320 ,200 ,40 ,25 ,8 ,8  ,4 ,0xB8000 ,0x0800 ,56  ,127 ,40 ,100 ,0   ),
        new Int10.VideoModeBlock( 0x006  ,VGA.M_CGA2   ,640 ,200 ,80 ,25 ,8 ,8  ,4 ,0xB8000 ,0x0800 ,56  ,127 ,40 ,100 ,0   ),
        new Int10.VideoModeBlock( 0x008  ,VGA.M_TANDY16,160 ,200 ,20 ,25 ,8 ,8  ,8 ,0xB8000 ,0x2000 ,56  ,127 ,40 ,100 ,0   ),
        new Int10.VideoModeBlock( 0x009  ,VGA.M_TANDY16,320 ,200 ,40 ,25 ,8 ,8  ,8 ,0xB8000 ,0x2000 ,113 ,63  ,80 ,50  ,0   ),
        new Int10.VideoModeBlock( 0x00A  ,VGA.M_CGA4   ,640 ,200 ,80 ,25 ,8 ,8  ,8 ,0xB8000 ,0x2000 ,113 ,63  ,80 ,50  ,0   ),
            //{ 0x00E  ,M_TANDY16,640 ,200 ,80 ,25 ,8 ,8  ,8 ,0xA0000 ,0x10000 ,113 ,256 ,80 ,200 ,0   },
        new Int10.VideoModeBlock(0xFFFF  ,VGA.M_ERROR  ,0   ,0   ,0  ,0  ,0 ,0  ,0 ,0x00000 ,0x0000 ,0   ,0   ,0  ,0   ,0 	),
    };

    private static Int10.VideoModeBlock Hercules_Mode=
        new Int10.VideoModeBlock(0x007  ,VGA.M_TEXT   ,640 ,400 ,80 ,25 ,8 ,14 ,1 ,0xB0000 ,0x1000 ,97 ,25  ,80 ,25  ,0	);

    private static byte[][] text_palette =
    {
      {0x00,0x00,0x00},{0x00,0x00,0x2a},{0x00,0x2a,0x00},{0x00,0x2a,0x2a},{0x2a,0x00,0x00},{0x2a,0x00,0x2a},{0x2a,0x2a,0x00},{0x2a,0x2a,0x2a},
      {0x00,0x00,0x15},{0x00,0x00,0x3f},{0x00,0x2a,0x15},{0x00,0x2a,0x3f},{0x2a,0x00,0x15},{0x2a,0x00,0x3f},{0x2a,0x2a,0x15},{0x2a,0x2a,0x3f},
      {0x00,0x15,0x00},{0x00,0x15,0x2a},{0x00,0x3f,0x00},{0x00,0x3f,0x2a},{0x2a,0x15,0x00},{0x2a,0x15,0x2a},{0x2a,0x3f,0x00},{0x2a,0x3f,0x2a},
      {0x00,0x15,0x15},{0x00,0x15,0x3f},{0x00,0x3f,0x15},{0x00,0x3f,0x3f},{0x2a,0x15,0x15},{0x2a,0x15,0x3f},{0x2a,0x3f,0x15},{0x2a,0x3f,0x3f},
      {0x15,0x00,0x00},{0x15,0x00,0x2a},{0x15,0x2a,0x00},{0x15,0x2a,0x2a},{0x3f,0x00,0x00},{0x3f,0x00,0x2a},{0x3f,0x2a,0x00},{0x3f,0x2a,0x2a},
      {0x15,0x00,0x15},{0x15,0x00,0x3f},{0x15,0x2a,0x15},{0x15,0x2a,0x3f},{0x3f,0x00,0x15},{0x3f,0x00,0x3f},{0x3f,0x2a,0x15},{0x3f,0x2a,0x3f},
      {0x15,0x15,0x00},{0x15,0x15,0x2a},{0x15,0x3f,0x00},{0x15,0x3f,0x2a},{0x3f,0x15,0x00},{0x3f,0x15,0x2a},{0x3f,0x3f,0x00},{0x3f,0x3f,0x2a},
      {0x15,0x15,0x15},{0x15,0x15,0x3f},{0x15,0x3f,0x15},{0x15,0x3f,0x3f},{0x3f,0x15,0x15},{0x3f,0x15,0x3f},{0x3f,0x3f,0x15},{0x3f,0x3f,0x3f}
    };

    private static byte[][] mtext_palette =
    {
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},
      {0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},
      {0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},
      {0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},
      {0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f}
    };

    private static byte[][] mtext_s3_palette =
    {
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},
      {0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},
      {0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},
      {0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},
      {0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},
      {0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},{0x2a,0x2a,0x2a},
      {0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f},{0x3f,0x3f,0x3f}
    };

    private static byte[][] ega_palette =
    {
      {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
      {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
      {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
      {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
      {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
      {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
      {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
      {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f}
    };

    private static byte[][] cga_palette =
    {
        {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
        {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
    };

    private static byte[][] cga_palette_2 =
    {
        {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
        {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
        {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
        {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
        {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
        {0x00,0x00,0x00}, {0x00,0x00,0x2a}, {0x00,0x2a,0x00}, {0x00,0x2a,0x2a}, {0x2a,0x00,0x00}, {0x2a,0x00,0x2a}, {0x2a,0x15,0x00}, {0x2a,0x2a,0x2a},
        {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
        {0x15,0x15,0x15}, {0x15,0x15,0x3f}, {0x15,0x3f,0x15}, {0x15,0x3f,0x3f}, {0x3f,0x15,0x15}, {0x3f,0x15,0x3f}, {0x3f,0x3f,0x15}, {0x3f,0x3f,0x3f},
    };

    private static byte[][] vga_palette =
    {
      {0x00,0x00,0x00},{0x00,0x00,0x2a},{0x00,0x2a,0x00},{0x00,0x2a,0x2a},{0x2a,0x00,0x00},{0x2a,0x00,0x2a},{0x2a,0x15,0x00},{0x2a,0x2a,0x2a},
      {0x15,0x15,0x15},{0x15,0x15,0x3f},{0x15,0x3f,0x15},{0x15,0x3f,0x3f},{0x3f,0x15,0x15},{0x3f,0x15,0x3f},{0x3f,0x3f,0x15},{0x3f,0x3f,0x3f},
      {0x00,0x00,0x00},{0x05,0x05,0x05},{0x08,0x08,0x08},{0x0b,0x0b,0x0b},{0x0e,0x0e,0x0e},{0x11,0x11,0x11},{0x14,0x14,0x14},{0x18,0x18,0x18},
      {0x1c,0x1c,0x1c},{0x20,0x20,0x20},{0x24,0x24,0x24},{0x28,0x28,0x28},{0x2d,0x2d,0x2d},{0x32,0x32,0x32},{0x38,0x38,0x38},{0x3f,0x3f,0x3f},
      {0x00,0x00,0x3f},{0x10,0x00,0x3f},{0x1f,0x00,0x3f},{0x2f,0x00,0x3f},{0x3f,0x00,0x3f},{0x3f,0x00,0x2f},{0x3f,0x00,0x1f},{0x3f,0x00,0x10},
      {0x3f,0x00,0x00},{0x3f,0x10,0x00},{0x3f,0x1f,0x00},{0x3f,0x2f,0x00},{0x3f,0x3f,0x00},{0x2f,0x3f,0x00},{0x1f,0x3f,0x00},{0x10,0x3f,0x00},
      {0x00,0x3f,0x00},{0x00,0x3f,0x10},{0x00,0x3f,0x1f},{0x00,0x3f,0x2f},{0x00,0x3f,0x3f},{0x00,0x2f,0x3f},{0x00,0x1f,0x3f},{0x00,0x10,0x3f},
      {0x1f,0x1f,0x3f},{0x27,0x1f,0x3f},{0x2f,0x1f,0x3f},{0x37,0x1f,0x3f},{0x3f,0x1f,0x3f},{0x3f,0x1f,0x37},{0x3f,0x1f,0x2f},{0x3f,0x1f,0x27},

      {0x3f,0x1f,0x1f},{0x3f,0x27,0x1f},{0x3f,0x2f,0x1f},{0x3f,0x37,0x1f},{0x3f,0x3f,0x1f},{0x37,0x3f,0x1f},{0x2f,0x3f,0x1f},{0x27,0x3f,0x1f},
      {0x1f,0x3f,0x1f},{0x1f,0x3f,0x27},{0x1f,0x3f,0x2f},{0x1f,0x3f,0x37},{0x1f,0x3f,0x3f},{0x1f,0x37,0x3f},{0x1f,0x2f,0x3f},{0x1f,0x27,0x3f},
      {0x2d,0x2d,0x3f},{0x31,0x2d,0x3f},{0x36,0x2d,0x3f},{0x3a,0x2d,0x3f},{0x3f,0x2d,0x3f},{0x3f,0x2d,0x3a},{0x3f,0x2d,0x36},{0x3f,0x2d,0x31},
      {0x3f,0x2d,0x2d},{0x3f,0x31,0x2d},{0x3f,0x36,0x2d},{0x3f,0x3a,0x2d},{0x3f,0x3f,0x2d},{0x3a,0x3f,0x2d},{0x36,0x3f,0x2d},{0x31,0x3f,0x2d},
      {0x2d,0x3f,0x2d},{0x2d,0x3f,0x31},{0x2d,0x3f,0x36},{0x2d,0x3f,0x3a},{0x2d,0x3f,0x3f},{0x2d,0x3a,0x3f},{0x2d,0x36,0x3f},{0x2d,0x31,0x3f},
      {0x00,0x00,0x1c},{0x07,0x00,0x1c},{0x0e,0x00,0x1c},{0x15,0x00,0x1c},{0x1c,0x00,0x1c},{0x1c,0x00,0x15},{0x1c,0x00,0x0e},{0x1c,0x00,0x07},
      {0x1c,0x00,0x00},{0x1c,0x07,0x00},{0x1c,0x0e,0x00},{0x1c,0x15,0x00},{0x1c,0x1c,0x00},{0x15,0x1c,0x00},{0x0e,0x1c,0x00},{0x07,0x1c,0x00},
      {0x00,0x1c,0x00},{0x00,0x1c,0x07},{0x00,0x1c,0x0e},{0x00,0x1c,0x15},{0x00,0x1c,0x1c},{0x00,0x15,0x1c},{0x00,0x0e,0x1c},{0x00,0x07,0x1c},

      {0x0e,0x0e,0x1c},{0x11,0x0e,0x1c},{0x15,0x0e,0x1c},{0x18,0x0e,0x1c},{0x1c,0x0e,0x1c},{0x1c,0x0e,0x18},{0x1c,0x0e,0x15},{0x1c,0x0e,0x11},
      {0x1c,0x0e,0x0e},{0x1c,0x11,0x0e},{0x1c,0x15,0x0e},{0x1c,0x18,0x0e},{0x1c,0x1c,0x0e},{0x18,0x1c,0x0e},{0x15,0x1c,0x0e},{0x11,0x1c,0x0e},
      {0x0e,0x1c,0x0e},{0x0e,0x1c,0x11},{0x0e,0x1c,0x15},{0x0e,0x1c,0x18},{0x0e,0x1c,0x1c},{0x0e,0x18,0x1c},{0x0e,0x15,0x1c},{0x0e,0x11,0x1c},
      {0x14,0x14,0x1c},{0x16,0x14,0x1c},{0x18,0x14,0x1c},{0x1a,0x14,0x1c},{0x1c,0x14,0x1c},{0x1c,0x14,0x1a},{0x1c,0x14,0x18},{0x1c,0x14,0x16},
      {0x1c,0x14,0x14},{0x1c,0x16,0x14},{0x1c,0x18,0x14},{0x1c,0x1a,0x14},{0x1c,0x1c,0x14},{0x1a,0x1c,0x14},{0x18,0x1c,0x14},{0x16,0x1c,0x14},
      {0x14,0x1c,0x14},{0x14,0x1c,0x16},{0x14,0x1c,0x18},{0x14,0x1c,0x1a},{0x14,0x1c,0x1c},{0x14,0x1a,0x1c},{0x14,0x18,0x1c},{0x14,0x16,0x1c},
      {0x00,0x00,0x10},{0x04,0x00,0x10},{0x08,0x00,0x10},{0x0c,0x00,0x10},{0x10,0x00,0x10},{0x10,0x00,0x0c},{0x10,0x00,0x08},{0x10,0x00,0x04},
      {0x10,0x00,0x00},{0x10,0x04,0x00},{0x10,0x08,0x00},{0x10,0x0c,0x00},{0x10,0x10,0x00},{0x0c,0x10,0x00},{0x08,0x10,0x00},{0x04,0x10,0x00},

      {0x00,0x10,0x00},{0x00,0x10,0x04},{0x00,0x10,0x08},{0x00,0x10,0x0c},{0x00,0x10,0x10},{0x00,0x0c,0x10},{0x00,0x08,0x10},{0x00,0x04,0x10},
      {0x08,0x08,0x10},{0x0a,0x08,0x10},{0x0c,0x08,0x10},{0x0e,0x08,0x10},{0x10,0x08,0x10},{0x10,0x08,0x0e},{0x10,0x08,0x0c},{0x10,0x08,0x0a},
      {0x10,0x08,0x08},{0x10,0x0a,0x08},{0x10,0x0c,0x08},{0x10,0x0e,0x08},{0x10,0x10,0x08},{0x0e,0x10,0x08},{0x0c,0x10,0x08},{0x0a,0x10,0x08},
      {0x08,0x10,0x08},{0x08,0x10,0x0a},{0x08,0x10,0x0c},{0x08,0x10,0x0e},{0x08,0x10,0x10},{0x08,0x0e,0x10},{0x08,0x0c,0x10},{0x08,0x0a,0x10},
      {0x0b,0x0b,0x10},{0x0c,0x0b,0x10},{0x0d,0x0b,0x10},{0x0f,0x0b,0x10},{0x10,0x0b,0x10},{0x10,0x0b,0x0f},{0x10,0x0b,0x0d},{0x10,0x0b,0x0c},
      {0x10,0x0b,0x0b},{0x10,0x0c,0x0b},{0x10,0x0d,0x0b},{0x10,0x0f,0x0b},{0x10,0x10,0x0b},{0x0f,0x10,0x0b},{0x0d,0x10,0x0b},{0x0c,0x10,0x0b},
      {0x0b,0x10,0x0b},{0x0b,0x10,0x0c},{0x0b,0x10,0x0d},{0x0b,0x10,0x0f},{0x0b,0x10,0x10},{0x0b,0x0f,0x10},{0x0b,0x0d,0x10},{0x0b,0x0c,0x10},
      {0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00},{0x00,0x00,0x00}
    };

    public static Int10.VideoModeBlock CurMode;

    private static boolean SetCurMode(Int10.VideoModeBlock modeblock[],/*Bit16u*/int mode) {
        /*Bitu*/int i=0;
        while (modeblock[i].mode!=0xffff) {
            if (modeblock[i].mode!=mode) i++;
            else {
                if ((!Int10.int10.vesa_oldvbe) || (ModeList_VGA[i].mode<0x120)) {
                    CurMode=modeblock[i];
                    return true;
                }
                return false;
            }
        }
        return false;
    }


    private static void FinishSetMode(boolean clearmem) {
        /* Clear video memory if needs be */
        if (clearmem) {
            switch (CurMode.type) {
            case VGA.M_CGA4:
            case VGA.M_CGA2:
            case VGA.M_TANDY16:
                for (/*Bit16u*/int ct=0;ct<16*1024;ct++) {
                    Memory.real_writew( 0xb800,ct*2,0x0000);
                }
                break;
            case VGA.M_TEXT: {
                /*Bit16u*/int seg = (CurMode.mode==7)?0xb000:0xb800;
                for (/*Bit16u*/int ct=0;ct<16*1024;ct++) Memory.real_writew(seg,ct*2,0x0720);
                break;
            }
            case VGA.M_EGA:
            case VGA.M_VGA:
            case VGA.M_LIN8:
            case VGA.M_LIN4:
            case VGA.M_LIN15:
            case VGA.M_LIN16:
            case VGA.M_LIN32:
                if (VGA.vga!=null) {
                    RAM.zeroset(VGA.vga.mem.linear, VGA.vga.vmemsize);
                    RAM.zeroset(VGA.vga.fastmem, Memory.videoCacheSize);
                }
            }
        }
        /* Setup the BIOS */
        if (CurMode.mode<128) Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MODE,CurMode.mode);
        else Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MODE,CurMode.mode-0x98);	//Looks like the s3 bios
        Memory.real_writew(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS,CurMode.twidth);
        Memory.real_writew(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE,CurMode.plength);
        Memory.real_writew(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTC_ADDRESS,((CurMode.mode==7 )|| (CurMode.mode==0x0f)) ? 0x3b4 : 0x3d4);
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_ROWS,(CurMode.theight-1));
        Memory.real_writew(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT,CurMode.cheight);
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_VIDEO_CTL,(0x60|(clearmem?0:0x80)));
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_SWITCHES,0x09);

        // this is an index into the dcc table:
        if (Dosbox.IS_VGA_ARCH()) Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_DCC_INDEX,0x0b);
        Memory.real_writed(Int10.BIOSMEM_SEG,Int10.BIOSMEM_VS_POINTER,Int10.int10.rom.video_save_pointers);

        // Set cursor shape
        if (CurMode.type==VGA.M_TEXT) {
            Int10_char.INT10_SetCursorShape((short)0x06,(short)07);
        }
        // Set cursor pos for page 0..7
        for (/*Bit8u*/int ct=0;ct<8;ct++) Int10_char.INT10_SetCursorPos((short)0,(short)0,(short)ct);
        // Set active page 0
        Int10_char.INT10_SetActivePage((short)0);
        /* Set some interrupt vectors */
        switch (CurMode.cheight) {
        case 8:Memory.RealSetVec(0x43,Int10.int10.rom.font_8_first);break;
        case 14:Memory.RealSetVec(0x43,Int10.int10.rom.font_14);break;
        case 16:Memory.RealSetVec(0x43,Int10.int10.rom.font_16);break;
        }
        /* Tell mouse resolution change */
        Mouse.Mouse_NewVideoMode();
    }

public static boolean INT10_SetVideoMode_OTHER(/*Bit16u*/int mode,boolean clearmem) {
	switch (Dosbox.machine) {
	case MachineType.MCH_CGA:
		if (mode>6) return false;
    case MachineType.MCH_TANDY:
	case MachineType.MCH_PCJR: //TANDY_ARCH_CASE:
		if (mode>0xa) return false;
		if (mode==7) mode=0; // PCJR defaults to 0 on illegal mode 7
		if (!SetCurMode(ModeList_OTHER,mode)) {
			if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Trying to set illegal mode "+Integer.toString(mode,16));
			return false;
		}
		break;
	case MachineType.MCH_HERC:
		// Only init the adapter if the equipment word is set to monochrome (Testdrive)
		if ((Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_INITIAL_MODE)&0x30)!=0x30) return false;
		CurMode=Hercules_Mode;
		mode=7; // in case the video parameter table is modified
		break;
	}
	if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_NORMAL,"Set Video Mode "+Integer.toString(mode,16));

	/* Setup the VGA to the correct mode */
//	VGA_SetMode(CurMode.type);
	/* Setup the CRTC */
	/*Bitu*/int crtc_base=Dosbox.machine== MachineType.MCH_HERC ? 0x3b4 : 0x3d4;
	//Horizontal total
	IO.IO_WriteW(crtc_base,0x00 | (CurMode.htotal) << 8);
	//Horizontal displayed
	IO.IO_WriteW(crtc_base,0x01 | (CurMode.hdispend) << 8);
	//Horizontal sync position
	IO.IO_WriteW(crtc_base,0x02 | (CurMode.hdispend+1) << 8);
	//Horizontal sync width, seems to be fixed to 0xa, for cga at least, hercules has 0xf
	IO.IO_WriteW(crtc_base,0x03 | (0xa) << 8);
	////Vertical total
	IO.IO_WriteW(crtc_base,0x04 | (CurMode.vtotal) << 8);
	//Vertical total adjust, 6 for cga,hercules,tandy
	IO.IO_WriteW(crtc_base,0x05 | (6) << 8);
	//Vertical displayed
	IO.IO_WriteW(crtc_base,0x06 | (CurMode.vdispend) << 8);
	//Vertical sync position
	IO.IO_WriteW(crtc_base,0x07 | (CurMode.vdispend + ((CurMode.vtotal - CurMode.vdispend)/2)-1) << 8);
	//Maximum scanline
	/*Bit8u*/short scanline,crtpage;
	scanline=8;
	switch(CurMode.type) {
	case VGA.M_TEXT:
		if (Dosbox.machine==MachineType.MCH_HERC) scanline=14;
		else scanline=8;
		break;
	case VGA.M_CGA2:
		scanline=2;
		break;
	case VGA.M_CGA4:
		if (CurMode.mode!=0xa) scanline=2;
		else scanline=4;
		break;
	case VGA.M_TANDY16:
		if (CurMode.mode!=0x9) scanline=2;
		else scanline=4;
		break;
	}
	IO.IO_WriteW(crtc_base,0x09 | (scanline-1) << 8);
	//Setup the CGA palette using VGA DAC palette
	for (/*Bit8u*/int ct=0;ct<16;ct++) VGA_dac.VGA_DAC_SetEntry(ct,cga_palette[ct][0],cga_palette[ct][1],cga_palette[ct][2]);
	//Setup the tandy palette
	for (/*Bit8u*/short ct=0;ct<16;ct++) VGA_dac.VGA_DAC_CombineColor(ct,ct);
	//Setup the special registers for each Dosbox.machine type
	final /*Bit8u*/byte[] mode_control_list={
		0x2c,0x28,0x2d,0x29,	//0-3
		0x2a,0x2e,0x1e,0x29,	//4-7
		0x2a,0x2b,0x3b			//8-a
	};
	final /*Bit8u*/byte[] mode_control_list_pcjr={
		0x0c,0x08,0x0d,0x09,	//0-3
		0x0a,0x0e,0x0e,0x09,	//4-7
		0x1a,0x1b,0x0b			//8-a
	};
	/*Bit8u*/short mode_control,color_select;
	switch (Dosbox.machine) {
	case MachineType.MCH_HERC:
		IO.IO_WriteB(0x3b8,0x28);	// TEXT mode and blinking characters

		VGA_other.Herc_Palette();
		VGA_dac.VGA_DAC_CombineColor((short)0,(short)0);
		VGA_dac.VGA_DAC_CombineColor((short)1,(short)7);

		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x29); // attribute controls blinking
		break;
	case MachineType.MCH_CGA:
		mode_control=mode_control_list[CurMode.mode];
		if (CurMode.mode == 0x6) color_select=0x3f;
		else color_select=0x30;
		IO.IO_WriteB(0x3d8,mode_control);
		IO.IO_WriteB(0x3d9,color_select);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,mode_control);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,color_select);
		break;
	case MachineType.MCH_TANDY:
		/* Init some registers */
		IO.IO_WriteB(0x3da,0x1);IO.IO_WriteB(0x3de,0xf);		//Palette mask always 0xf
		IO.IO_WriteB(0x3da,0x2);IO.IO_WriteB(0x3de,0x0);		//black border
		IO.IO_WriteB(0x3da,0x3);							//Tandy color overrides?
		switch (CurMode.mode) {
		case 0x8:
			IO.IO_WriteB(0x3de,0x14);break;
		case 0x9:
			IO.IO_WriteB(0x3de,0x14);break;
		case 0xa:
			IO.IO_WriteB(0x3de,0x0c);break;
		default:
			IO.IO_WriteB(0x3de,0x0);break;
		}
        // write palette
		for(int i = 0; i < 16; i++) {
			IO.IO_WriteB(0x3da,i+0x10);
			IO.IO_WriteB(0x3de,i);
		}
		//Clear extended mapping
		IO.IO_WriteB(0x3da,0x5);
		IO.IO_WriteB(0x3de,0x0);
		//Clear monitor mode
		IO.IO_WriteB(0x3da,0x8);
		IO.IO_WriteB(0x3de,0x0);
		crtpage=(CurMode.mode>=0x9) ? (short)0xf6 : (short)0x3f;
		IO.IO_WriteB(0x3df,crtpage);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTCPU_PAGE,crtpage);
		mode_control=mode_control_list[CurMode.mode];
		if (CurMode.mode == 0x6 || CurMode.mode==0xa) color_select=0x3f;
		else color_select=0x30;
		IO.IO_WriteB(0x3d8,mode_control);
		IO.IO_WriteB(0x3d9,color_select);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,mode_control);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,color_select);
		break;
	case MachineType.MCH_PCJR:
		/* Init some registers */
		IO.IO_ReadB(0x3da);
		IO.IO_WriteB(0x3da,0x1);IO.IO_WriteB(0x3da,0xf);		//Palette mask always 0xf
		IO.IO_WriteB(0x3da,0x2);IO.IO_WriteB(0x3da,0x0);		//black border
		IO.IO_WriteB(0x3da,0x3);
		if (CurMode.mode<=0x04) IO.IO_WriteB(0x3da,0x02);
		else if (CurMode.mode==0x06) IO.IO_WriteB(0x3da,0x08);
		else IO.IO_WriteB(0x3da,0x00);

		/* set CRT/Processor page register */
		if (CurMode.mode<0x04) crtpage=0x3f;
		else if (CurMode.mode>=0x09) crtpage=0xf6;
		else crtpage=0x7f;
		IO.IO_WriteB(0x3df,crtpage);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTCPU_PAGE,crtpage);

		mode_control=mode_control_list_pcjr[CurMode.mode];
		IO.IO_WriteB(0x3da,0x0);IO.IO_WriteB(0x3da,mode_control);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,mode_control);

		if (CurMode.mode == 0x6 || CurMode.mode==0xa) color_select=0x3f;
		else color_select=0x30;
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,color_select);
        Int10_pal.INT10_SetColorSelect((short)1);
		Int10_pal.INT10_SetBackgroundBorder((short)0);
		break;
	}

	/*RealPt*/int vparams = Memory.RealGetVec(0x1d);
	if ((vparams != Memory.RealMake(0xf000,0xf0a4)) && (mode < 8)) {
		// load crtc parameters from video params table
		/*Bit16u*/int crtc_block_index = 0;
		if (mode < 2) crtc_block_index = 0;
		else if (mode < 4) crtc_block_index = 1;
		else if (mode < 7) crtc_block_index = 2;
		else if (mode == 7) crtc_block_index = 3; // MDA mono mode; invalid for others
		else if (mode < 9) crtc_block_index = 2;
		else crtc_block_index = 3; // Tandy/PCjr modes

		// init CRTC registers
		for (/*Bit16u*/int i = 0; i < 16; i++)
			IO.IO_WriteW(crtc_base, i | (Memory.real_readb(Memory.RealSeg(vparams),
				Memory.RealOff(vparams) + i + crtc_block_index*16) << 8));
	}
	FinishSetMode(clearmem);
	return true;
}

private static void att_text16(short[] att_data) {
    if (CurMode.mode==7) {
        att_data[0]=0x00;
        att_data[8]=0x10;
        for (int i=1; i<8; i++) {
            att_data[i]=0x08;
            att_data[i+8]=0x18;
        }
    } else {
        for (/*Bit8u*/short ct=0;ct<8;ct++) {
            att_data[ct]=ct;
            att_data[ct+8]=(short)(ct+0x38);
        }
        if (Dosbox.IS_VGA_ARCH()) att_data[0x06]=0x14;		//Odd Color 6 yellow/brown.
    }
}
public static boolean INT10_SetVideoMode(/*Bit16u*/int mode) {
	boolean clearmem=true;/*Bitu*/int i;
	if (mode>=0x100) {
		if ((mode & 0x4000)!=0 && Int10.int10.vesa_nolfb) return false;
		if ((mode & 0x8000)!=0) clearmem=false;
		mode&=0xfff;
	}
	if ((mode<0x100) && (mode & 0x80)!=0) {
		clearmem=false;
		mode-=0x80;
	}
	Int10.int10.vesa_setmode=0xffff;
	if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_NORMAL,"Set Video Mode "+Integer.toString(mode,16));
	if (!Dosbox.IS_EGAVGA_ARCH()) return INT10_SetVideoMode_OTHER(mode,clearmem);

	/* First read mode setup settings from bios area */
//	Bit8u video_ctl=Memory.real_readb(Int10.BIOSMEM_SEG,BIOSMEM_VIDEO_CTL);
//	Bit8u vga_switches=Memory.real_readb(Int10.BIOSMEM_SEG,BIOSMEM_SWITCHES);
	/*Bit8u*/int modeset_ctl=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_MODESET_CTL);

	if (Dosbox.IS_VGA_ARCH()) {
		if (VGA.svga!=null && VGA.svga.accepts_mode!=null) {
			if (!VGA.svga.accepts_mode.call(mode)) return false;
		}

		switch(Dosbox.svgaCard) {
		case SVGACards.SVGA_TsengET4K:
		case SVGACards.SVGA_TsengET3K:
			if (!SetCurMode(ModeList_VGA_Tseng,mode)){
				if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"VGA:Trying to set illegal mode "+Integer.toString(mode,16));
				return false;
			}
			break;
		case SVGACards.SVGA_ParadisePVGA1A:
			if (!SetCurMode(ModeList_VGA_Paradise,mode)){
				if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"VGA:Trying to set illegal mode "+Integer.toString(mode,16));
				return false;
			}
			break;
		default:
			if (!SetCurMode(ModeList_VGA,mode)){
				if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"VGA:Trying to set illegal mode "+Integer.toString(mode,16));
				return false;
			}
		}
		// check for scanline backwards compatibility (VESA text modes??)
		if (CurMode.type==VGA.M_TEXT) {
			if ((modeset_ctl&0x90)==0x80) { // 200 lines emulation
				if (CurMode.mode <= 3) {
					CurMode = ModeList_VGA_Text_200lines[CurMode.mode];
				}
			} else if ((modeset_ctl&0x90)==0x00) { // 350 lines emulation
				if (CurMode.mode <= 3) {
					CurMode = ModeList_VGA_Text_350lines[CurMode.mode];
				}
			}
		}
	} else {
		if (!SetCurMode(ModeList_EGA,mode)){
			if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"EGA:Trying to set illegal mode "+Integer.toString(mode,16));
			return false;
		}
	}

	/* Setup the VGA to the correct mode */

	/*Bit16u*/int crtc_base;
	boolean mono_mode=(mode == 7) || (mode==0xf);
	if (mono_mode) crtc_base=0x3b4;
	else crtc_base=0x3d4;

	if (Dosbox.IS_VGA_ARCH() && (Dosbox.svgaCard == SVGACards.SVGA_S3Trio)) {
		// Disable MMIO here so we can read / write memory
		IoHandler.IO_Write(crtc_base,0x53);
		IoHandler.IO_Write(crtc_base+1,0x0);
	}

	/* Setup MISC Output Register */
	/*Bit8u*/int misc_output=0x2 | (mono_mode ? 0x0 : 0x1);

	if ((CurMode.type==VGA.M_TEXT) && (CurMode.cwidth==9)) {
		// 28MHz (16MHz EGA) clock for 9-pixel wide chars
		misc_output|=0x4;
	}

	switch (CurMode.vdispend) {
	case 400:
		misc_output|=0x60;
		break;
	case 480:
		misc_output|=0xe0;
		break;
	case 350:
		misc_output|=0xa0;
		break;
	default:
		misc_output|=0x60;
	}
	IoHandler.IO_Write(0x3c2,misc_output);		//Setup for 3b4 or 3d4

	/* Program Sequencer */
	/*Bit8u*/short[] seq_data=new short[SEQ_REGS];
	//memset(seq_data,0,SEQ_REGS);
	seq_data[1]|=0x01;	//8 dot fonts by default
	if ((CurMode.special & _EGA_HALF_CLOCK)!=0) seq_data[1]|=0x08; //Check for half clock
	if ((Dosbox.machine==MachineType.MCH_EGA) && (CurMode.special & _EGA_HALF_CLOCK)!=0) seq_data[1]|=0x02;
	seq_data[4]|=0x02;	//More than 64kb
	switch (CurMode.type) {
	case VGA.M_TEXT:
		if (CurMode.cwidth==9) seq_data[1] &= ~1;
		seq_data[2]|=0x3;				//Enable plane 0 and 1
		seq_data[4]|=0x01;				//Alpanumeric
		if (Dosbox.IS_VGA_ARCH()) seq_data[4]|=0x04;				//odd/even enabled
		break;
	case VGA.M_CGA2:
		seq_data[2]|=0xf;				//Enable plane 0
		if (Dosbox.machine==MachineType.MCH_EGA) seq_data[4]|=0x04;		//odd/even enabled
		break;
	case VGA.M_CGA4:
		if (Dosbox.machine==MachineType.MCH_EGA) seq_data[2]|=0x03;		//Enable plane 0 and 1
		break;
	case VGA.M_LIN4:
	case VGA.M_EGA:
		seq_data[2]|=0xf;				//Enable all planes for writing
		if (Dosbox.machine==MachineType.MCH_EGA) seq_data[4]|=0x04;		//odd/even enabled
		break;
	case VGA.M_LIN8:						//Seems to have the same reg layout from testing
	case VGA.M_LIN15:
	case VGA.M_LIN16:
	case VGA.M_LIN32:
	case VGA.M_VGA:
		seq_data[2]|=0xf;				//Enable all planes for writing
		seq_data[4]|=0xc;				//Graphics - odd/even - Chained
		break;
	}
	for (/*Bit8u*/short ct=0;ct<SEQ_REGS;ct++) {
		IoHandler.IO_Write(0x3c4,ct);
		IoHandler.IO_Write(0x3c5,seq_data[ct]);
	}
    if (VGA.vga != null)
	    VGA.vga.config.compatible_chain4 = true; // this may be changed by SVGA chipset emulation

	/* Program CRTC */
	/* First disable write protection */
	IoHandler.IO_Write(crtc_base,0x11);
	IoHandler.IO_Write(crtc_base+1,(IoHandler.IO_Read(crtc_base+1)&0x7f));
	/* Clear all the regs */
	for (/*Bit8u*/byte ct=0x0;ct<=0x18;ct++) {
		IoHandler.IO_Write(crtc_base,ct);IoHandler.IO_Write(crtc_base+1,0);
	}
	/*Bit8u*/short overflow=0;/*Bit8u*/short max_scanline=0;
	/*Bit8u*/short ver_overflow=0;/*Bit8u*/short hor_overflow=0;
	/* Horizontal Total */
	IoHandler.IO_Write(crtc_base,0x00);IoHandler.IO_Write(crtc_base+1,(CurMode.htotal-5));
	hor_overflow|=((CurMode.htotal-5) & 0x100) >> 8;
	/* Horizontal Display End */
	IoHandler.IO_Write(crtc_base,0x01);IoHandler.IO_Write(crtc_base+1,(CurMode.hdispend-1));
	hor_overflow|=((CurMode.hdispend-1) & 0x100) >> 7;
	/* Start horizontal Blanking */
	IoHandler.IO_Write(crtc_base,0x02);IoHandler.IO_Write(crtc_base+1,CurMode.hdispend);
	hor_overflow|=((CurMode.hdispend) & 0x100) >> 6;
	/* End horizontal Blanking */
	/*Bitu*/int blank_end=(CurMode.htotal-2) & 0x7f;
	IoHandler.IO_Write(crtc_base,0x03);IoHandler.IO_Write(crtc_base+1,(0x80|(blank_end & 0x1f)));

	/* Start Horizontal Retrace */
	/*Bitu*/int ret_start;
	if ((CurMode.special & _EGA_HALF_CLOCK)!=0 && (CurMode.type!=VGA.M_CGA2)) ret_start = (CurMode.hdispend+3);
	else if (CurMode.type==VGA.M_TEXT) ret_start = (CurMode.hdispend+5);
	else ret_start = (CurMode.hdispend+4);
	IoHandler.IO_Write(crtc_base,0x04);IoHandler.IO_Write(crtc_base+1,ret_start);
	hor_overflow|=(ret_start & 0x100) >> 4;

	/* End Horizontal Retrace */
	/*Bitu*/int ret_end;
	if ((CurMode.special & _EGA_HALF_CLOCK)!=0) {
		if (CurMode.type==VGA.M_CGA2) ret_end=0;	// mode 6
		else if ((CurMode.special & _EGA_LINE_DOUBLE)!=0) ret_end = (CurMode.htotal-18) & 0x1f;
		else ret_end = ((CurMode.htotal-18) & 0x1f) | 0x20; // mode 0&1 have 1 char sync delay
	} else if (CurMode.type==VGA.M_TEXT) ret_end = (CurMode.htotal-3) & 0x1f;
	else ret_end = (CurMode.htotal-4) & 0x1f;

	IoHandler.IO_Write(crtc_base,0x05);IoHandler.IO_Write(crtc_base+1,(ret_end | (blank_end & 0x20) << 2));

	/* Vertical Total */
	IoHandler.IO_Write(crtc_base,0x06);IoHandler.IO_Write(crtc_base+1,(CurMode.vtotal-2));
	overflow|=((CurMode.vtotal-2) & 0x100) >> 8;
	overflow|=((CurMode.vtotal-2) & 0x200) >> 4;
	ver_overflow|=((CurMode.vtotal-2) & 0x400) >> 10;

	/*Bitu*/int vretrace;
	if (Dosbox.IS_VGA_ARCH()) {
		switch (CurMode.vdispend) {
		case 400: vretrace=CurMode.vdispend+12;
				break;
		case 480: vretrace=CurMode.vdispend+10;
				break;
		case 350: vretrace=CurMode.vdispend+37;
				break;
		default: vretrace=CurMode.vdispend+12;
		}
	} else {
		switch (CurMode.vdispend) {
		case 350: vretrace=CurMode.vdispend;
				break;
		default: vretrace=CurMode.vdispend+24;
		}
	}

	/* Vertical Retrace Start */
	IoHandler.IO_Write(crtc_base,0x10);IoHandler.IO_Write(crtc_base+1,vretrace);
	overflow|=(vretrace & 0x100) >> 6;
	overflow|=(vretrace & 0x200) >> 2;
	ver_overflow|=(vretrace & 0x400) >> 6;

	/* Vertical Retrace End */
	IoHandler.IO_Write(crtc_base,0x11);IoHandler.IO_Write(crtc_base+1,((vretrace+2) & 0xF));

	/* Vertical Display End */
	IoHandler.IO_Write(crtc_base,0x12);IoHandler.IO_Write(crtc_base+1,CurMode.vdispend-1);
	overflow|=((CurMode.vdispend-1) & 0x100) >> 7;
	overflow|=((CurMode.vdispend-1) & 0x200) >> 3;
	ver_overflow|=((CurMode.vdispend-1) & 0x400) >> 9;

	/*Bitu*/int vblank_trim;
	if (Dosbox.IS_VGA_ARCH()) {
		switch (CurMode.vdispend) {
		case 400: vblank_trim=6;
				break;
		case 480: vblank_trim=7;
				break;
		case 350: vblank_trim=5;
				break;
		default: vblank_trim=8;
		}
	} else {
		switch (CurMode.vdispend) {
		case 350: vblank_trim=0;
				break;
		default: vblank_trim=23;
		}
	}

	/* Vertical Blank Start */
	IoHandler.IO_Write(crtc_base,0x15);IoHandler.IO_Write(crtc_base+1,(CurMode.vdispend+vblank_trim));
	overflow|=((CurMode.vdispend+vblank_trim) & 0x100) >> 5;
	max_scanline|=((CurMode.vdispend+vblank_trim) & 0x200) >> 4;
	ver_overflow|=((CurMode.vdispend+vblank_trim) & 0x400) >> 8;

	/* Vertical Blank End */
	IoHandler.IO_Write(crtc_base,0x16);IoHandler.IO_Write(crtc_base+1,(CurMode.vtotal-vblank_trim-2));

	/* Line Compare */
	/*Bitu*/int line_compare=(CurMode.vtotal < 1024) ? 1023 : 2047;
	IoHandler.IO_Write(crtc_base,0x18);IoHandler.IO_Write(crtc_base+1,(line_compare&0xff));
	overflow|=(line_compare & 0x100) >> 4;
	max_scanline|=(line_compare & 0x200) >> 3;
	ver_overflow|=(line_compare & 0x400) >> 4;
	/*Bit8u*/short underline=0;
	/* Maximum scanline / Underline Location */
	if ((CurMode.special & _EGA_LINE_DOUBLE)!=0) {
		if (Dosbox.machine!=MachineType.MCH_EGA) max_scanline|=0x80;
	}
	switch (CurMode.type) {
	case VGA.M_TEXT:
		max_scanline|=CurMode.cheight-1;
		underline=(short)(mono_mode ? 0x0f : 0x1f); // mode 7 uses a diff underline position
		break;
	case VGA.M_VGA:
		underline=0x40;
		max_scanline|=1;		//Vga doesn't use double line but this
		break;
	case VGA.M_LIN8:
	case VGA.M_LIN15:
	case VGA.M_LIN16:
	case VGA.M_LIN32:
		underline=0x60;			//Seems to enable the every 4th clock on my s3
		break;
	case VGA.M_CGA2:
	case VGA.M_CGA4:
		max_scanline|=1;
		break;
	}
	if (CurMode.vdispend==350) underline=0x0f;

	IoHandler.IO_Write(crtc_base,0x09);IoHandler.IO_Write(crtc_base+1,max_scanline);
	IoHandler.IO_Write(crtc_base,0x14);IoHandler.IO_Write(crtc_base+1,underline);

	/* OverFlow */
	IoHandler.IO_Write(crtc_base,0x07);IoHandler.IO_Write(crtc_base+1,overflow);

	if (Dosbox.svgaCard == SVGACards.SVGA_S3Trio) {
		/* Extended Horizontal Overflow */
		IoHandler.IO_Write(crtc_base,0x5d);IoHandler.IO_Write(crtc_base+1,hor_overflow);
		/* Extended Vertical Overflow */
		IoHandler.IO_Write(crtc_base,0x5e);IoHandler.IO_Write(crtc_base+1,ver_overflow);
	}

	/* Offset Register */
	/*Bitu*/int offset;
	switch (CurMode.type) {
	case VGA.M_LIN8:
		offset = CurMode.swidth/8;
		break;
	case VGA.M_LIN15:
	case VGA.M_LIN16:
		offset = 2 * CurMode.swidth/8;
		break;
	case VGA.M_LIN32:
		offset = 4 * CurMode.swidth/8;
		break;
	default:
		offset = CurMode.hdispend/2;
	}
	IoHandler.IO_Write(crtc_base,0x13);
	IoHandler.IO_Write(crtc_base + 1,(offset & 0xff));

	if (Dosbox.svgaCard == SVGACards.SVGA_S3Trio) {
		/* Extended System Control 2 Register  */
		/* This register actually has more bits but only use the extended offset ones */
		IoHandler.IO_Write(crtc_base,0x51);
		IoHandler.IO_Write(crtc_base + 1,((offset & 0x300) >> 4));
		/* Clear remaining bits of the display start */
		IoHandler.IO_Write(crtc_base,0x69);
		IoHandler.IO_Write(crtc_base + 1,0);
		/* Extended Vertical Overflow */
		IoHandler.IO_Write(crtc_base,0x5e);IoHandler.IO_Write(crtc_base+1,ver_overflow);
	}

	/* Mode Control */
	/*Bit8u*/short mode_control=0;

	switch (CurMode.type) {
	case VGA.M_CGA2:
		mode_control=0xc2; // 0x06 sets address wrap.
		break;
	case VGA.M_CGA4:
		mode_control=0xa2;
		break;
	case VGA.M_LIN4:
	case VGA.M_EGA:
		if (CurMode.mode==0x11) // 0x11 also sets address wrap.  thought maybe all 2 color modes did but 0x0f doesn't.
			mode_control=0xc3; // so.. 0x11 or 0x0f a one off?
		else {
			if (Dosbox.machine==MachineType.MCH_EGA) {
				if ((CurMode.special & _EGA_LINE_DOUBLE)!=0) mode_control=0xc3;
				else mode_control=0x8b;
			} else {
				mode_control=0xe3;
			}
		}
		break;
	case VGA.M_TEXT:
	case VGA.M_VGA:
	case VGA.M_LIN8:
	case VGA.M_LIN15:
	case VGA.M_LIN16:
	case VGA.M_LIN32:
		mode_control=0xa3;
		if ((CurMode.special & _VGA_PIXEL_DOUBLE)!=0)
			mode_control |= 0x08;
		break;
	}

	IoHandler.IO_Write(crtc_base,0x17);IoHandler.IO_Write(crtc_base+1,mode_control);
	/* Renable write protection */
	IoHandler.IO_Write(crtc_base,0x11);
	IoHandler.IO_Write(crtc_base+1,(IoHandler.IO_Read(crtc_base+1)|0x80));

	if (Dosbox.svgaCard == SVGACards.SVGA_S3Trio) {
		/* Setup the correct clock */
		if (CurMode.mode>=0x100) {
			misc_output|=0xef;		//Select clock 3
			/*Bitu*/int clock=CurMode.vtotal*8*CurMode.htotal*70;
			VGA.VGA_SetClock(3,clock/1000);
		}
		/*Bit8u*/short misc_control_2;
		/* Setup Pixel format */
		switch (CurMode.type) {
		case VGA.M_LIN8:
			misc_control_2=0x00;
			break;
		case VGA.M_LIN15:
			misc_control_2=0x30;
			break;
		case VGA.M_LIN16:
			misc_control_2=0x50;
			break;
		case VGA.M_LIN32:
			misc_control_2=0xd0;
			break;
		default:
			misc_control_2=0x0;
			break;
		}
		IO.IO_WriteB(crtc_base,0x67);IO.IO_WriteB(crtc_base+1,misc_control_2);
	}

	/* Write Misc Output */
	IoHandler.IO_Write(0x3c2,misc_output);
	/* Program Graphics controller */
	/*Bit8u*/short[] gfx_data = new short[GFX_REGS];
	//memset(gfx_data,0,GFX_REGS);
	gfx_data[0x7]=0xf;				/* Color don't care */
	gfx_data[0x8]=0xff;				/* BitMask */
	switch (CurMode.type) {
	case VGA.M_TEXT:
		gfx_data[0x5]|=0x10;		//Odd-Even Mode
		gfx_data[0x6]|=mono_mode ? 0x0a : 0x0e;		//Either b800 or b000
		break;
	case VGA.M_LIN8:
	case VGA.M_LIN15:
	case VGA.M_LIN16:
	case VGA.M_LIN32:
	case VGA.M_VGA:
		gfx_data[0x5]|=0x40;		//256 color mode
		gfx_data[0x6]|=0x05;		//graphics mode at 0xa000-affff
		break;
	case VGA.M_LIN4:
	case VGA.M_EGA:
		gfx_data[0x6]|=0x05;		//graphics mode at 0xa000-affff
		break;
	case VGA.M_CGA4:
		gfx_data[0x5]|=0x20;		//CGA mode
		gfx_data[0x6]|=0x0f;		//graphics mode at at 0xb800=0xbfff
		if (Dosbox.machine==MachineType.MCH_EGA) gfx_data[0x5]|=0x10;
		break;
	case VGA.M_CGA2:
		if (Dosbox.machine==MachineType.MCH_EGA) {
			gfx_data[0x6]|=0x0d;		//graphics mode at at 0xb800=0xbfff
		} else {
			gfx_data[0x6]|=0x0f;		//graphics mode at at 0xb800=0xbfff
		}
		break;
	}
	for (/*Bit8u*/short ct=0;ct<GFX_REGS;ct++) {
		IoHandler.IO_Write(0x3ce,ct);
		IoHandler.IO_Write(0x3cf,gfx_data[ct]);
	}
	/*Bit8u*/short[] att_data = new short[ATT_REGS];
	//memset(att_data,0,ATT_REGS);
	att_data[0x12]=0xf;				//Always have all color planes enabled
	/* Program Attribute Controller */
	switch (CurMode.type) {
	case VGA.M_EGA:
	case VGA.M_LIN4:
		att_data[0x10]=0x01;		//Color Graphics
		switch (CurMode.mode) {
		case 0x0f:
			att_data[0x10]|=0x0a;	//Monochrome
			att_data[0x01]=0x08;
			att_data[0x04]=0x18;
			att_data[0x05]=0x18;
			att_data[0x09]=0x08;
			att_data[0x0d]=0x18;
			break;
		case 0x11:
			for (i=1;i<16;i++) att_data[i]=0x3f;
			break;
		case 0x10:
		case 0x12:
            att_text16(att_data);
			break;
		default:
			if ( CurMode.type == VGA.M_LIN4 ) {
                att_text16(att_data);
                break;
            }
			for (/*Bit8u*/short ct=0;ct<8;ct++) {
				att_data[ct]=ct;
				att_data[ct+8]=(short)(ct+0x10);
			}
			break;
		}
		break;
	case VGA.M_TANDY16:
		att_data[0x10]=0x01;		//Color Graphics
		for (/*Bit8u*/short ct=0;ct<16;ct++) att_data[ct]=ct;
		break;
	case VGA.M_TEXT:
		if (CurMode.cwidth==9) {
			att_data[0x13]=0x08;	//Pel panning on 8, although we don't have 9 dot text mode
			att_data[0x10]=0x0C;	//Color Text with blinking, 9 Bit characters
		} else {
			att_data[0x13]=0x00;
			att_data[0x10]=0x08;	//Color Text with blinking, 8 Bit characters
		}
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,0x30);
        att_text16(att_data);
		break;
	case VGA.M_CGA2:
		att_data[0x10]=0x01;		//Color Graphics
		att_data[0]=0x0;
		for (i=1;i<0x10;i++) att_data[i]=0x17;
		att_data[0x12]=0x1;			//Only enable 1 plane
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,0x3f);
		break;
	case VGA.M_CGA4:
		att_data[0x10]=0x01;		//Color Graphics
		att_data[0]=0x0;
		att_data[1]=0x13;
		att_data[2]=0x15;
		att_data[3]=0x17;
		att_data[4]=0x02;
		att_data[5]=0x04;
		att_data[6]=0x06;
		att_data[7]=0x07;
		for (/*Bit8u*/short ct=0x8;ct<0x10;ct++)
			att_data[ct] = (short)(ct + 0x8);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAL,0x30);
		break;
	case VGA.M_VGA:
	case VGA.M_LIN8:
	case VGA.M_LIN15:
	case VGA.M_LIN16:
	case VGA.M_LIN32:
		for (/*Bit8u*/short ct=0;ct<16;ct++) att_data[ct]=ct;
		att_data[0x10]=0x41;		//Color Graphics 8-bit
		break;
	}
	IoHandler.IO_Read(mono_mode ? 0x3ba : 0x3da);
	if ((modeset_ctl & 8)==0) {
		for (/*Bit8u*/short ct=0;ct<ATT_REGS;ct++) {
			IoHandler.IO_Write(0x3c0,ct);
			IoHandler.IO_Write(0x3c0,att_data[ct]);
		}
        if (VGA.vga != null)
		    VGA.vga.config.pel_panning = 0;
		IoHandler.IO_Write(0x3c0,0x20); IoHandler.IO_Write(0x3c0,0x00); //Disable palette access
		IoHandler.IO_Write(0x3c6,0xff); //Reset Pelmask
		/* Setup the DAC */
		IoHandler.IO_Write(0x3c8,0);
		switch (CurMode.type) {
		case VGA.M_EGA:
			if (CurMode.mode>0xf) {
				//goto dac_text16;
                for (i=0;i<64;i++) {
                    IoHandler.IO_Write(0x3c9,text_palette[i][0]);
                    IoHandler.IO_Write(0x3c9,text_palette[i][1]);
                    IoHandler.IO_Write(0x3c9,text_palette[i][2]);
                }
			} else if (CurMode.mode==0xf) {
				for (i=0;i<64;i++) {
					IoHandler.IO_Write(0x3c9,mtext_s3_palette[i][0]);
					IoHandler.IO_Write(0x3c9,mtext_s3_palette[i][1]);
					IoHandler.IO_Write(0x3c9,mtext_s3_palette[i][2]);
				}
			} else {
				for (i=0;i<64;i++) {
					IoHandler.IO_Write(0x3c9,ega_palette[i][0]);
					IoHandler.IO_Write(0x3c9,ega_palette[i][1]);
					IoHandler.IO_Write(0x3c9,ega_palette[i][2]);
				}
			}
			break;
		case VGA.M_CGA2:
		case VGA.M_CGA4:
		case VGA.M_TANDY16:
			for (i=0;i<64;i++) {
				IoHandler.IO_Write(0x3c9,cga_palette_2[i][0]);
				IoHandler.IO_Write(0x3c9,cga_palette_2[i][1]);
				IoHandler.IO_Write(0x3c9,cga_palette_2[i][2]);
			}
			break;
		case VGA.M_TEXT:
			if (CurMode.mode==7) {
				if ((Dosbox.IS_VGA_ARCH()) && (Dosbox.svgaCard == SVGACards.SVGA_S3Trio)) {
					for (i=0;i<64;i++) {
						IoHandler.IO_Write(0x3c9,mtext_s3_palette[i][0]);
						IoHandler.IO_Write(0x3c9,mtext_s3_palette[i][1]);
						IoHandler.IO_Write(0x3c9,mtext_s3_palette[i][2]);
					}
				} else {
					for (i=0;i<64;i++) {
						IoHandler.IO_Write(0x3c9,mtext_palette[i][0]);
						IoHandler.IO_Write(0x3c9,mtext_palette[i][1]);
						IoHandler.IO_Write(0x3c9,mtext_palette[i][2]);
					}
				}
				break;
			} //FALLTHROUGH!!!!
		case VGA.M_LIN4: //Added for CAD Software
//dac_text16:
			for (i=0;i<64;i++) {
				IoHandler.IO_Write(0x3c9,text_palette[i][0]);
				IoHandler.IO_Write(0x3c9,text_palette[i][1]);
				IoHandler.IO_Write(0x3c9,text_palette[i][2]);
			}
			break;
		case VGA.M_VGA:
		case VGA.M_LIN8:
		case VGA.M_LIN15:
		case VGA.M_LIN16:
		case VGA.M_LIN32:
			for (i=0;i<256;i++) {
				IoHandler.IO_Write(0x3c9,vga_palette[i][0]);
				IoHandler.IO_Write(0x3c9,vga_palette[i][1]);
				IoHandler.IO_Write(0x3c9,vga_palette[i][2]);
			}
			break;
		}
		if (Dosbox.IS_VGA_ARCH()) {
			/* check if gray scale summing is enabled */
			if ((Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_MODESET_CTL) & 2)!=0) {
				Int10_pal.INT10_PerformGrayScaleSumming(0,256);
			}
		}
	} else {
		for (/*Bit8u*/short ct=0x10;ct<ATT_REGS;ct++) {
			if (ct==0x11) continue;	// skip overscan register
			IoHandler.IO_Write(0x3c0,ct);
			IoHandler.IO_Write(0x3c0,att_data[ct]);
		}
		VGA.vga.config.pel_panning = 0;
		IoHandler.IO_Write(0x3c0,0x20); //Disable palette access
	}
	/* Setup some special stuff for different modes */
	/*Bit8u*/int feature=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_INITIAL_MODE);
	switch (CurMode.type) {
	case VGA.M_CGA2:
		feature=(short)((feature&~0x30)|0x20);
		Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x1e);
		break;
	case VGA.M_CGA4:
		feature=(short)((feature&~0x30)|0x20);
		if (CurMode.mode==4) Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x2a);
		else if (CurMode.mode==5) Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x2e);
		else Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x2);
		break;
	case VGA.M_TANDY16:
		feature=(short)((feature&~0x30)|0x20);
		break;
	case VGA.M_TEXT:
		feature=(short)((feature&~0x30)|0x20);
		switch (CurMode.mode) {
		case 0:Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x2c);break;
		case 1:Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x28);break;
		case 2:Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x2d);break;
		case 3:
		case 7:Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_MSR,0x29);break;
		}
		break;
	case VGA.M_LIN4:
	case VGA.M_EGA:
	case VGA.M_VGA:
		feature=(short)((feature&~0x30));
		break;
	}
	// disabled, has to be set in bios.cpp exclusively
//	Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_INITIAL_MODE,feature);

	if (Dosbox.svgaCard == SVGACards.SVGA_S3Trio) {
		/* Setup the CPU Window */
		IoHandler.IO_Write(crtc_base,0x6a);
		IoHandler.IO_Write(crtc_base+1,0);
		/* Setup the linear frame buffer */
		IoHandler.IO_Write(crtc_base,0x59);
		IoHandler.IO_Write(crtc_base+1,(int)((Int10.S3_LFB_BASE >> 24)&0xff));
		IoHandler.IO_Write(crtc_base,0x5a);
		IoHandler.IO_Write(crtc_base+1,(int)((Int10.S3_LFB_BASE >> 16)&0xff));
		IoHandler.IO_Write(crtc_base,0x6b); // BIOS scratchpad
		IoHandler.IO_Write(crtc_base+1,(int)((Int10.S3_LFB_BASE >> 24)&0xff));

		/* Setup some remaining S3 registers */
		IoHandler.IO_Write(crtc_base,0x41); // BIOS scratchpad
		IoHandler.IO_Write(crtc_base+1,0x88);
		IoHandler.IO_Write(crtc_base,0x52); // extended BIOS scratchpad
		IoHandler.IO_Write(crtc_base+1,0x80);

		IoHandler.IO_Write(0x3c4,0x15);
		IoHandler.IO_Write(0x3c5,0x03);

		// Accellerator setup
		/*Bitu*/int reg_50=VGA.S3_XGA_8BPP;
		switch (CurMode.type) {
			case VGA.M_LIN15:
			case VGA.M_LIN16: reg_50|=VGA.S3_XGA_16BPP; break;
			case VGA.M_LIN32: reg_50|=VGA.S3_XGA_32BPP; break;
			default: break;
		}
		switch(CurMode.swidth) {
			case 640:  reg_50|=VGA.S3_XGA_640; break;
			case 800:  reg_50|=VGA.S3_XGA_800; break;
			case 1024: reg_50|=VGA.S3_XGA_1024; break;
			case 1152: reg_50|=VGA.S3_XGA_1152; break;
			case 1280: reg_50|=VGA.S3_XGA_1280; break;
			default: break;
		}
		IO.IO_WriteB(crtc_base,0x50); IO.IO_WriteB(crtc_base+1,reg_50);

		/*Bit8u*/short reg_31, reg_3a;
		switch (CurMode.type) {
			case VGA.M_LIN15:
			case VGA.M_LIN16:
			case VGA.M_LIN32:
				reg_3a=0x15;
				break;
			case VGA.M_LIN8:
				// S3VBE20 does it this way. The other double pixel bit does not
				// seem to have an effect on the Trio64.
				if((CurMode.special&_VGA_PIXEL_DOUBLE)!=0) reg_3a=0x5;
				else reg_3a=0x15;
				break;
			default:
				reg_3a=5;
				break;
		};

		switch (CurMode.type) {
		case VGA.M_LIN4: // <- Theres a discrepance with real hardware on this
		case VGA.M_LIN8:
		case VGA.M_LIN15:
		case VGA.M_LIN16:
		case VGA.M_LIN32:
			reg_31 = 9;
			break;
		default:
			reg_31 = 5;
			break;
		}
		IoHandler.IO_Write(crtc_base,0x3a);IoHandler.IO_Write(crtc_base+1,reg_3a);
		IoHandler.IO_Write(crtc_base,0x31);IoHandler.IO_Write(crtc_base+1,reg_31);	//Enable banked memory and 256k+ access
		IoHandler.IO_Write(crtc_base,0x58);IoHandler.IO_Write(crtc_base+1,0x3);		//Enable 8 mb of linear addressing

		IoHandler.IO_Write(crtc_base,0x38);IoHandler.IO_Write(crtc_base+1,0x48);	//Register lock 1
		IoHandler.IO_Write(crtc_base,0x39);IoHandler.IO_Write(crtc_base+1,0xa5);	//Register lock 2
	} else if (VGA.svga!=null && VGA.svga.set_video_mode!=null) {
		VGA.VGA_ModeExtraData modeData = new VGA.VGA_ModeExtraData();
		modeData.ver_overflow = ver_overflow;
		modeData.hor_overflow = hor_overflow;
		modeData.offset = offset;
		modeData.modeNo = CurMode.mode;
		modeData.htotal = CurMode.htotal;
		modeData.vtotal = CurMode.vtotal;
		VGA.svga.set_video_mode.call(crtc_base, modeData);
	}

	FinishSetMode(clearmem);

	/* Set vga attrib register into defined state */
	IoHandler.IO_Read(mono_mode ? 0x3ba : 0x3da);
	IoHandler.IO_Write(0x3c0,0x20);

	/* Load text mode font */
	if (CurMode.type==VGA.M_TEXT) {
		Int10_memory.INT10_ReloadFont();
	}
	return true;
}

static public /*Bitu*/int VideoModeMemSize(/*Bitu*/int mode) {
	if (!Dosbox.IS_VGA_ARCH())
		return 0;

	Int10.VideoModeBlock[] modelist = null;

	switch (Dosbox.svgaCard) {
	case SVGACards.SVGA_TsengET4K:
	case SVGACards.SVGA_TsengET3K:
		modelist = ModeList_VGA_Tseng;
		break;
	case SVGACards.SVGA_ParadisePVGA1A:
		modelist = ModeList_VGA_Paradise;
		break;
	default:
		modelist = ModeList_VGA;
		break;
	}

	Int10.VideoModeBlock vmodeBlock = null;
	/*Bitu*/int i=0;
	while (modelist[i].mode!=0xffff) {
		if (modelist[i].mode==mode) {
			vmodeBlock = modelist[i];
			break;
		}
		i++;
	}
	if (vmodeBlock==null)
        return 0;

	switch(vmodeBlock.type) {
	case VGA.M_LIN4:
		return vmodeBlock.swidth*vmodeBlock.sheight/2;
	case VGA.M_LIN8:
		return vmodeBlock.swidth*vmodeBlock.sheight;
	case VGA.M_LIN15: case VGA.M_LIN16:
		return vmodeBlock.swidth*vmodeBlock.sheight*2;
	case VGA.M_LIN32:
		return vmodeBlock.swidth*vmodeBlock.sheight*4;
	case VGA.M_TEXT:
		return vmodeBlock.twidth*vmodeBlock.theight*2;
	}
	// Return 0 for all other types, those always fit in memory
	return 0;
}

}
