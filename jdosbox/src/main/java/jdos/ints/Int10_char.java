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

public class Int10_char {
    static void CGA2_CopyRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short rold,/*Bit8u*/short rnew,/*PhysPt*/int  base) {
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+((Int10_modes.CurMode.twidth*rnew)*(cheight/2)+cleft);
        /*PhysPt*/int  src=base+((Int10_modes.CurMode.twidth*rold)*(cheight/2)+cleft);
        /*Bitu*/int copy=(cright-cleft);
        /*Bitu*/int nextline=Int10_modes.CurMode.twidth;
        for (/*Bitu*/int i=0;i<cheight/2;i++) {
            Memory.MEM_BlockCopy(dest,src,copy);
            Memory.MEM_BlockCopy(dest+8*1024,src+8*1024,copy);
            dest+=nextline;src+=nextline;
        }
    }

    static void CGA4_CopyRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short rold,/*Bit8u*/short rnew,/*PhysPt*/int  base) {
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+((Int10_modes.CurMode.twidth*rnew)*(cheight/2)+cleft)*2;
        /*PhysPt*/int  src=base+((Int10_modes.CurMode.twidth*rold)*(cheight/2)+cleft)*2;
        /*Bitu*/int copy=(cright-cleft)*2;/*Bitu*/int nextline=Int10_modes.CurMode.twidth*2;
        for (/*Bitu*/int i=0;i<cheight/2;i++) {
            Memory.MEM_BlockCopy(dest,src,copy);
            Memory.MEM_BlockCopy(dest+8*1024,src+8*1024,copy);
            dest+=nextline;src+=nextline;
        }
    }

    static void TANDY16_CopyRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short rold,/*Bit8u*/short rnew,/*PhysPt*/int  base) {
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+((Int10_modes.CurMode.twidth*rnew)*(cheight/4)+cleft)*4;
        /*PhysPt*/int  src=base+((Int10_modes.CurMode.twidth*rold)*(cheight/4)+cleft)*4;
        /*Bitu*/int copy=(cright-cleft)*4;/*Bitu*/int nextline=Int10_modes.CurMode.twidth*4;
        for (/*Bitu*/int i=0;i<cheight/4;i++) {
            Memory.MEM_BlockCopy(dest,src,copy);
            Memory.MEM_BlockCopy(dest+8*1024,src+8*1024,copy);
            Memory.MEM_BlockCopy(dest+16*1024,src+16*1024,copy);
            Memory.MEM_BlockCopy(dest+24*1024,src+24*1024,copy);
            dest+=nextline;src+=nextline;
        }
    }

    static void EGA16_CopyRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short rold,/*Bit8u*/short rnew,/*PhysPt*/int  base) {
        /*PhysPt*/int  src,dest;/*Bitu*/int copy;
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        dest=base+(Int10_modes.CurMode.twidth*rnew)*cheight+cleft;
        src=base+(Int10_modes.CurMode.twidth*rold)*cheight+cleft;
        /*Bitu*/int nextline=Int10_modes.CurMode.twidth;
        /* Setup registers correctly */
        IoHandler.IO_Write(0x3ce,5);IoHandler.IO_Write(0x3cf,1);		/* Memory transfer mode */
        IoHandler.IO_Write(0x3c4,2);IoHandler.IO_Write(0x3c5,0xf);		/* Enable all Write planes */
        /* Do some copying */
        /*Bitu*/int rowsize=(cright-cleft);
        copy=cheight;
        for (;copy>0;copy--) {
            for (/*Bitu*/int x=0;x<rowsize;x++) Memory.mem_writeb(dest+x,Memory.mem_readb(src+x));
            dest+=nextline;src+=nextline;
        }
        /* Restore registers */
        IoHandler.IO_Write(0x3ce,5);
        IoHandler.IO_Write(0x3cf,0);		/* Normal transfer mode */
    }

    static void VGA_CopyRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short rold,/*Bit8u*/short rnew,/*PhysPt*/int  base) {
        /*PhysPt*/int  src,dest;/*Bitu*/int copy;
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        dest=base+8*((Int10_modes.CurMode.twidth*rnew)*cheight+cleft);
        src=base+8*((Int10_modes.CurMode.twidth*rold)*cheight+cleft);
        /*Bitu*/int nextline=8*Int10_modes.CurMode.twidth;
        /*Bitu*/int rowsize=8*(cright-cleft);
        copy=cheight;
        for (;copy>0;copy--) {
            for (/*Bitu*/int x=0;x<rowsize;x++) Memory.mem_writeb(dest+x,Memory.mem_readb(src+x));
            dest+=nextline;src+=nextline;
        }
    }

    static void TEXT_CopyRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short rold,/*Bit8u*/short rnew,/*PhysPt*/int  base) {
        /*PhysPt*/int  src,dest;
        src=base+(rold*Int10_modes.CurMode.twidth+cleft)*2;
        dest=base+(rnew*Int10_modes.CurMode.twidth+cleft)*2;
        Memory.MEM_BlockCopy(dest,src,(cright-cleft)*2);
    }

    static void CGA2_FillRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short row,/*PhysPt*/int  base,/*Bit8u*/short attr) {
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+((Int10_modes.CurMode.twidth*row)*(cheight/2)+cleft);
        /*Bitu*/int copy=(cright-cleft);
        /*Bitu*/int nextline=Int10_modes.CurMode.twidth;
        attr=(short)((attr & 0x3) | ((attr & 0x3) << 2) | ((attr & 0x3) << 4) | ((attr & 0x3) << 6));
        for (/*Bitu*/int i=0;i<cheight/2;i++) {
            for (/*Bitu*/int x=0;x<copy;x++) {
                Memory.mem_writeb(dest+x,attr);
                Memory.mem_writeb(dest+8*1024+x,attr);
            }
            dest+=nextline;
        }
    }

    static void CGA4_FillRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short row,/*PhysPt*/int  base,/*Bit8u*/short attr) {
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+((Int10_modes.CurMode.twidth*row)*(cheight/2)+cleft)*2;
        /*Bitu*/int copy=(cright-cleft)*2;/*Bitu*/int nextline=Int10_modes.CurMode.twidth*2;
        attr=(short)((attr & 0x3) | ((attr & 0x3) << 2) | ((attr & 0x3) << 4) | ((attr & 0x3) << 6));
        for (/*Bitu*/int i=0;i<cheight/2;i++) {
            for (/*Bitu*/int x=0;x<copy;x++) {
                Memory.mem_writeb(dest+x,attr);
                Memory.mem_writeb(dest+8*1024+x,attr);
            }
            dest+=nextline;
        }
    }

    static void TANDY16_FillRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short row,/*PhysPt*/int  base,/*Bit8u*/short attr) {
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+((Int10_modes.CurMode.twidth*row)*(cheight/4)+cleft)*4;
        /*Bitu*/int copy=(cright-cleft)*4;/*Bitu*/int nextline=Int10_modes.CurMode.twidth*4;
        attr=(short)((attr & 0xf) | (attr & 0xf) << 4);
        for (/*Bitu*/int i=0;i<cheight/4;i++) {
            for (/*Bitu*/int x=0;x<copy;x++) {
                Memory.mem_writeb(dest+x,attr);
                Memory.mem_writeb(dest+8*1024+x,attr);
                Memory.mem_writeb(dest+16*1024+x,attr);
                Memory.mem_writeb(dest+24*1024+x,attr);
            }
            dest+=nextline;
        }
    }

    static void EGA16_FillRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short row,/*PhysPt*/int  base,/*Bit8u*/short attr) {
        /* Set Bitmask / Color / Full Set Reset */
        IoHandler.IO_Write(0x3ce,0x8);IoHandler.IO_Write(0x3cf,0xff);
        IoHandler.IO_Write(0x3ce,0x0);IoHandler.IO_Write(0x3cf,attr);
        IoHandler.IO_Write(0x3ce,0x1);IoHandler.IO_Write(0x3cf,0xf);
        /* Write some bytes */
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+(Int10_modes.CurMode.twidth*row)*cheight+cleft;
        /*Bitu*/int nextline=Int10_modes.CurMode.twidth;
        /*Bitu*/int copy = cheight;/*Bitu*/int rowsize=(cright-cleft);
        for (;copy>0;copy--) {
            for (/*Bitu*/int x=0;x<rowsize;x++) Memory.mem_writeb(dest+x,0xff);
            dest+=nextline;
        }
        IoHandler.IO_Write(0x3cf,0);
    }

    static void VGA_FillRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short row,/*PhysPt*/int  base,/*Bit8u*/short attr) {
        /* Write some bytes */
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        /*PhysPt*/int  dest=base+8*((Int10_modes.CurMode.twidth*row)*cheight+cleft);
        /*Bitu*/int nextline=8*Int10_modes.CurMode.twidth;
        /*Bitu*/int copy = cheight;/*Bitu*/int rowsize=8*(cright-cleft);
        for (;copy>0;copy--) {
            for (/*Bitu*/int x=0;x<rowsize;x++) Memory.mem_writeb(dest+x,attr);
            dest+=nextline;
        }
    }

    static void TEXT_FillRow(/*Bit8u*/short cleft,/*Bit8u*/short cright,/*Bit8u*/short row,/*PhysPt*/int  base,/*Bit8u*/short attr) {
        /* Do some filing */
        /*PhysPt*/int  dest;
        dest=base+(row*Int10_modes.CurMode.twidth+cleft)*2;
        /*Bit16u*/int fill=(attr<<8)+' ';
        for (/*Bit8u*/short x=0;x<(cright-cleft);x++) {
            Memory.mem_writew(dest,fill);
            dest+=2;
        }
    }


    static public void INT10_ScrollWindow(/*Bit8u*/short rul,/*Bit8u*/short cul,/*Bit8u*/short rlr,/*Bit8u*/short clr,/*Bit8s*/byte nlines,/*Bit8u*/short attr,/*Bit8u*/short page) {
/* Do some range checking */
        if (Int10_modes.CurMode.type!= VGA.M_TEXT) page=0xff;
        /*BIOS_NCOLS*//*Bit16u*/int ncols=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS);
        /*BIOS_NROWS*//*Bit16u*/int nrows=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_ROWS)+1;
        if(rul>rlr) return;
        if(cul>clr) return;
        if(rlr>=nrows) rlr=(short)(nrows-1);
        if(clr>=ncols) clr=(short)(ncols-1);
        clr++;

        /* Get the correct page */
        if(page==0xFF) page=(short)Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE);
        /*PhysPt*/int  base=Int10_modes.CurMode.pstart+page*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE);

        if (Dosbox.machine==MachineType.MCH_PCJR) {
            if (Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_MODE) >= 9) {
                // PCJr cannot handle these modes at 0xb800
                // See INT10_PutPixel M_TANDY16
                /*Bitu*/int cpupage = (Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTCPU_PAGE) >> 3) & 0x7;

                base = cpupage << 14;
                base += page*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE);
            }
        }

        /* See how much lines need to be copied */
        /*Bit8u*/int start=0,end=0;/*Bits*/int next=0;
        /* Copy some lines */
        boolean gotofilling = false;
        if (nlines>0) {
            start=(rlr-nlines+1) & 0xFF;
            end=rul;
            next=-1;
        } else if (nlines<0) {
            start=(rul-nlines-1) & 0xFF;
            end=rlr;
            next=1;
        } else {
            nlines=(byte)(rlr-rul+1);
            //goto filling;
            gotofilling = true;
        }
        if (!gotofilling) {
            while (start!=end) {
                start=(start+next) & 0xFF;
                switch (Int10_modes.CurMode.type) {
                case VGA.M_TEXT:
                    TEXT_CopyRow(cul,clr,(short)start,(short)((start+nlines) & 0xFF),base);break;
                case VGA.M_CGA2:
                    CGA2_CopyRow(cul,clr,(short)start,(short)((start+nlines) & 0xFF),base);break;
                case VGA.M_CGA4:
                    CGA4_CopyRow(cul,clr,(short)start,(short)((start+nlines) & 0xFF),base);break;
                case VGA.M_TANDY16:
                    TANDY16_CopyRow(cul,clr,(short)start,(short)((start+nlines) & 0xFF),base);break;
                case VGA.M_EGA:
                    EGA16_CopyRow(cul,clr,(short)start,(short)((start+nlines) & 0xFF),base);break;
                case VGA.M_VGA:
                    VGA_CopyRow(cul,clr,(short)start,(short)((start+nlines) & 0xFF),base);break;
                case VGA.M_LIN4:
                    if ((Dosbox.machine== MachineType.MCH_VGA) && (Dosbox.svgaCard== SVGACards.SVGA_TsengET4K) &&
                            (Int10_modes.CurMode.swidth<=800)) {
                        // the ET4000 BIOS supports text output in 800x600 SVGA
                        EGA16_CopyRow(cul,clr,(short)start,(short)(start+nlines),base);break;
                    }
                    // fall-through
                default:
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR,"Unhandled mode "+Int10_modes.CurMode.type+" for scroll");
                }
            }
        }
        /* Fill some lines */
    //filling:
        if (nlines>0) {
            start=rul;
        } else {
            nlines=(byte)-nlines;
            start=rlr-nlines+1;
        }
        for (;nlines>0;nlines--) {
            switch (Int10_modes.CurMode.type) {
            case VGA.M_TEXT:
                TEXT_FillRow(cul,clr,(short)start,base,attr);break;
            case VGA.M_CGA2:
                CGA2_FillRow(cul,clr,(short)start,base,attr);break;
            case VGA.M_CGA4:
                CGA4_FillRow(cul,clr,(short)start,base,attr);break;
            case VGA.M_TANDY16:
                TANDY16_FillRow(cul,clr,(short)start,base,attr);break;
            case VGA.M_EGA:
                EGA16_FillRow(cul,clr,(short)start,base,attr);break;
            case VGA.M_VGA:
                VGA_FillRow(cul,clr,(short)start,base,attr);break;
            case VGA.M_LIN4:
                if ((Dosbox.machine==MachineType.MCH_VGA) && (Dosbox.svgaCard==SVGACards.SVGA_TsengET4K) &&
                        (Int10_modes.CurMode.swidth<=800)) {
                    EGA16_FillRow(cul,clr,(short)start,base,attr);break;
                }
                // fall-through
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"Unhandled mode "+Int10_modes.CurMode.type+" for scroll");
            }
            start++;
        }
    }

    public static void INT10_SetActivePage(/*Bit8u*/short page) {
        /*Bit16u*/int mem_address;
        if (page>7) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"INT10_SetActivePage page "+page);

        if (Dosbox.IS_EGAVGA_ARCH() && (Dosbox.svgaCard==SVGACards.SVGA_S3Trio)) page &= 7;

        mem_address=page*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE);
        /* Write the new page start */
        Memory.real_writew(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_START,mem_address);
        if (Dosbox.IS_EGAVGA_ARCH()) {
            if (Int10_modes.CurMode.mode<8) mem_address>>=1;
            // rare alternative: if (Int10_modes.CurMode.type==M_TEXT)  mem_address>>=1;
        } else {
            mem_address>>=1;
        }
        /* Write the new start address in vgahardware */
        /*Bit16u*/int base=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTC_ADDRESS);
        IoHandler.IO_Write(base,0x0c);
        IoHandler.IO_Write(base+1,(mem_address>>8));
        IoHandler.IO_Write(base,0x0d);
        IoHandler.IO_Write(base+1,mem_address);

        // And change the BIOS page
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE,page);
        /*Bit8u*/short cur_row=Int10.CURSOR_POS_ROW(page);
        /*Bit8u*/short cur_col=Int10.CURSOR_POS_COL(page);
        // Display the cursor, now the page is active
        INT10_SetCursorPos(cur_row,cur_col,page);
    }

    private static void dowrite(/*Bit8u*/short first,/*Bit8u*/short last) {
        /*Bit16u*/int base=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTC_ADDRESS);
        IoHandler.IO_Write(base,0xa);IoHandler.IO_Write(base+1,first);
        IoHandler.IO_Write(base,0xb);IoHandler.IO_Write(base+1,last);
    }

    public static void INT10_SetCursorShape(/*Bit8u*/short first,/*Bit8u*/short last) {
        Memory.real_writew(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURSOR_TYPE,last|(first<<8));
        if (Dosbox.machine==MachineType.MCH_CGA) {dowrite(first, last);return;}
        if (Dosbox.IS_TANDY_ARCH()) {dowrite(first, last);return;}
        /* Skip CGA cursor emulation if EGA/VGA system is active */
        if ((Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_VIDEO_CTL) & 0x8)==0) {
            /* Check for CGA type 01, invisible */
            if ((first & 0x60) == 0x20) {
                first=0x1e;
                last=0x00;
                {dowrite(first, last);return;}
            }
            /* Check if we need to convert CGA Bios cursor values */
            if ((Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_VIDEO_CTL) & 0x1)==0) { // set by int10 fun12 sub34
//			if (Int10_modes.CurMode.mode>0x3) goto dowrite;	//Only mode 0-3 are text modes on cga
                if ((first & 0xe0)!=0 || (last & 0xe0)!=0) {dowrite(first, last);return;}
                /*Bit8u*/short cheight=(short)(Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT)-1);
                /* Creative routine i based of the original ibmvga bios */

                if (last<first) {
                    if (last==0) {dowrite(first, last);return;}
                    first=last;
                    last=cheight;
                /* Test if this might be a cga style cursor set, if not don't do anything */
                } else if (((first | last)>=cheight) || !(last==(cheight-1)) || !(first==cheight) ) {
                    if (last<=3) {dowrite(first, last);return;}
                    if (first+2<last) {
                        if (first>2) {
                            first=(short)((cheight+1)/2);
                            last=cheight;
                        } else {
                            last=cheight;
                        }
                    } else {
                        first=(short)((first-last)+cheight);
                        last=cheight;

                        if (cheight>0xc) { // vgatest sets 15 15 2x where only one should be decremented to 14 14
                            first--;     // implementing int10 fun12 sub34 fixed this.
                            last--;
                        }
                    }
                }

            }
        }
        dowrite(first, last);
    }

    static public void INT10_SetCursorPos(/*Bit8u*/short row,/*Bit8u*/short col,/*Bit8u*/short page) {
        /*Bit16u*/int address;

        if (page>7) if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"INT10_SetCursorPos page "+page);
        // Bios cursor pos
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURSOR_POS+page*2,col);
        Memory.real_writeb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURSOR_POS+page*2+1,row);
        // Set the hardware cursor
        /*Bit8u*/int current=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE);
        if(page==current) {
            // Get the dimensions
            /*BIOS_NCOLS*//*Bit16u*/int ncols=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS);
            // Calculate the address knowing nbcols nbrows and page num
            // NOTE: Int10.BIOSMEM_CURRENT_START counts in colour/flag pairs
            address=(ncols*row)+col+Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_START)/2;
            // CRTC regs 0x0e and 0x0f
            /*Bit16u*/int base=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CRTC_ADDRESS);
            IoHandler.IO_Write(base,0x0e);
            IoHandler.IO_Write(base+1,(address>>8));
            IoHandler.IO_Write(base,0x0f);
            IoHandler.IO_Write(base+1,address);
        }
    }

    static public int ReadCharAttr(/*Bit16u*/int col,/*Bit16u*/int row,/*Bit8u*/short page) {
        /* Externally used by the mouse routine */
        /*PhysPt*/int  fontdata;
        /*Bitu*/int x,y;
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        boolean split_chr = false;
        switch (Int10_modes.CurMode.type) {
        case VGA.M_TEXT:
            {
                // Compute the address
                /*Bit16u*/int address=page*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE);
                address+=(row*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)+col)*2;
                // read the char
                /*PhysPt*/int  where = Int10_modes.CurMode.pstart+address;
                return Memory.mem_readw(where);
            }
        case VGA.M_CGA4:
        case VGA.M_CGA2:
        case VGA.M_TANDY16:
            split_chr = true;
            /* Fallthrough */
        default:		/* EGA/VGA don't have a split font-table */
            for(/*Bit16u*/int chr=0;chr <= 255 ;chr++) {
                if (!split_chr || (chr<128)) fontdata = Memory.Real2Phys(Memory.RealGetVec(0x43))+chr*cheight;
                else fontdata = Memory.Real2Phys(Memory.RealGetVec(0x1F))+(chr-128)*cheight;

                x=8*col;
                y=cheight*row;
                boolean error=false;
                for (/*Bit8u*/short h=0;h<cheight;h++) {
                    /*Bit8u*/short bitsel=128;
                    /*Bit8u*/int bitline=Memory.mem_readb(fontdata++);
                    /*Bit8u*/short res=0;
                    /*Bit8u*/short vidline=0;
                    /*Bit16u*/int tx=x;
                    while (bitsel!=0) {
                        //Construct bitline in memory
                        res = Int10_put_pixel.INT10_GetPixel(tx,y,page);
                        if(res!=0) vidline|=bitsel;
                        tx++;
                        bitsel>>=1;
                    }
                    y++;
                    if(bitline != vidline){
                        /* It's not character 'chr', move on to the next */
                        error = true;
                        break;
                    }
                }
                if(!error){
                    /* We found it */
                    return chr;
                }
            }
            Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"ReadChar didn't find character");
            return 0;
        }
    }

    public static int INT10_ReadCharAttr(/*Bit8u*/short page) {
        if(page==0xFF) page=(short)Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE);
        /*Bit8u*/short cur_row=Int10.CURSOR_POS_ROW(page);
        /*Bit8u*/short cur_col=Int10.CURSOR_POS_COL(page);
        return ReadCharAttr(cur_col,cur_row,page);
    }

    static private boolean warned_use = false;
    public static void WriteChar(/*Bit16u*/int col,/*Bit16u*/int row,/*Bit8u*/short page,/*Bit8u*/short chr,/*Bit8u*/short attr,boolean useattr) {
        /* Externally used by the mouse routine */
        /*RealPt*/int fontdata;
        /*Bitu*/int x,y;
        /*Bit8u*/int cheight = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CHAR_HEIGHT);
        switch (Int10_modes.CurMode.type) {
        case VGA.M_TEXT:
            {
                // Compute the address
                /*Bit16u*/int address=page*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_PAGE_SIZE);
                address+=(row*Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS)+col)*2;
                // Write the char
                /*PhysPt*/int  where = Int10_modes.CurMode.pstart+address;
                Memory.mem_writeb(where,chr);
                if (useattr) {
                    Memory.mem_writeb(where+1,attr);
                }
            }
            return;
        case VGA.M_CGA4:
        case VGA.M_CGA2:
        case VGA.M_TANDY16:
            if (chr<128)
                fontdata=Memory.RealGetVec(0x43);
            else {
                chr-=128;
                fontdata=Memory.RealGetVec(0x1f);
            }
            fontdata=Memory.RealMake(Memory.RealSeg(fontdata), Memory.RealOff(fontdata) + chr*cheight);
            break;
        default:
            fontdata=Memory.RealGetVec(0x43);
            fontdata=Memory.RealMake(Memory.RealSeg(fontdata), Memory.RealOff(fontdata) + chr*cheight);
            break;
        }

        if(!useattr) { //Set attribute(color) to a sensible value

            if(!warned_use){
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10,LogSeverities.LOG_ERROR,"writechar used without attribute in non-textmode "+String.valueOf((char)chr)+" "+Integer.toString(chr,16));
                warned_use = true;
            }
            switch(Int10_modes.CurMode.type) {
            case VGA.M_CGA4:
                attr = 0x3;
                break;
            case VGA.M_CGA2:
                attr = 0x1;
                break;
            case VGA.M_TANDY16:
            case VGA.M_EGA:
            default:
                attr = 0xf;
                break;
            }
        }

        //Some weird behavior of mode 6 (and 11)
        if ((Int10_modes.CurMode.mode == 0x6)/* || (Int10_modes.CurMode.mode==0x11)*/) attr = (short)((attr&0x80)|1);
        //(same fix for 11 fixes vgatest2, but it's not entirely correct according to wd)

        x=8*col;
        y=cheight*row;/*Bit8u*/short xor_mask=(short)((Int10_modes.CurMode.type == VGA.M_VGA) ? 0x0 : 0x80);
        //TODO Check for out of bounds
        if (Int10_modes.CurMode.type==VGA.M_EGA) {
            /* enable all planes for EGA modes (Ultima 1 colour bug) */
            /* might be put into INT10_PutPixel but different vga bios
               implementations have different opinions about this */
            IoHandler.IO_Write(0x3c4,0x2);IoHandler.IO_Write(0x3c5,0xf);
        }
        for (/*Bit8u*/short h=0;h<cheight;h++) {
            /*Bit8u*/short bitsel=128;
            /*Bit8u*/int bitline = Memory.mem_readb(Memory.Real2Phys( fontdata ));
            fontdata = Memory.RealMake( Memory.RealSeg( fontdata ), Memory.RealOff( fontdata ) + 1);
            /*Bit16u*/int tx=x;
            while (bitsel!=0) {
                if ((bitline&bitsel)!=0) Int10_put_pixel.INT10_PutPixel(tx,y,page,attr);
                else Int10_put_pixel.INT10_PutPixel(tx,y,page,(short)(attr & xor_mask));
                tx++;
                bitsel>>=1;
            }
            y++;
        }
    }

    public static void INT10_WriteChar(/*Bit8u*/short chr,/*Bit8u*/short attr,/*Bit8u*/short page,/*Bit16u*/int count,boolean showattr) {
        if (Int10_modes.CurMode.type!=VGA.M_TEXT) {
            showattr=true; //Use attr in graphics mode always
            switch (Dosbox.machine) {
                // EGAVGA_ARCH_CASE
                case MachineType.MCH_EGA:
                case MachineType.MCH_VGA:
                    page%=Int10_modes.CurMode.ptotal;
                    break;
                case MachineType.MCH_CGA:
                case MachineType.MCH_PCJR:
                    page=0;
                    break;
            }
        }

        /*Bit8u*/short cur_row=Int10.CURSOR_POS_ROW(page);
        /*Bit8u*/short cur_col=Int10.CURSOR_POS_COL(page);
        /*BIOS_NCOLS*//*Bit16u*/int ncols=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS);
        while (count>0) {
            WriteChar(cur_col,cur_row,page,chr,attr,showattr);
            count--;
            cur_col++;
            if(cur_col==ncols) {
                cur_col=0;
                cur_row++;
            }
        }
    }

    static void INT10_TeletypeOutputAttr(/*Bit8u*/short chr,/*Bit8u*/short attr,boolean useattr,/*Bit8u*/short page) {
        /*BIOS_NCOLS*//*Bit16u*/int ncols=Memory.real_readw(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_COLS);
        /*BIOS_NROWS*//*Bit16u*/int nrows=Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_ROWS)+1;
        /*Bit8u*/short cur_row=Int10.CURSOR_POS_ROW(page);
        /*Bit8u*/short cur_col=Int10.CURSOR_POS_COL(page);
        switch (chr) {
        case 7:
        //TODO BEEP
        break;
        case 8:
            if(cur_col>0) cur_col--;
            break;
        case '\r':
            cur_col=0;
            break;
        case '\n':
//		cur_col=0; //Seems to break an old chess game
            cur_row++;
            break;
        case '\t':
            do {
                INT10_TeletypeOutputAttr((short)' ',attr,useattr,page);
                cur_row=Int10.CURSOR_POS_ROW(page);
                cur_col=Int10.CURSOR_POS_COL(page);
            } while((cur_col%8) != 0);
            break;
        default:
            /* Draw the actual Character */
            WriteChar(cur_col,cur_row,page,chr,attr,useattr);
            cur_col++;
        }
        if(cur_col==ncols) {
            cur_col=0;
            cur_row++;
        }
        // Do we need to scroll ?
        if(cur_row==nrows) {
            //Fill with black on non-text modes and with 0x7 on textmode
            /*Bit8u*/short fill = (short)((Int10_modes.CurMode.type == VGA.M_TEXT)?0x7:0);
            INT10_ScrollWindow((short)0,(short)0,(short)(nrows-1),(short)(ncols-1),(byte)-1,fill,page);
            cur_row--;
        }
        // Set the cursor for the page
        INT10_SetCursorPos(cur_row,cur_col,page);
    }

    public static void INT10_TeletypeOutputAttr(/*Bit8u*/int chr,/*Bit8u*/int attr,boolean useattr) {
        INT10_TeletypeOutputAttr((short)chr,(short)attr,useattr,(short)Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE));
    }

    public static void INT10_TeletypeOutput(/*Bit8u*/int chr,/*Bit8u*/int attr) {
        INT10_TeletypeOutputAttr((short)chr,(short)attr,Int10_modes.CurMode.type!=VGA.M_TEXT);
    }

    public static void INT10_WriteString(/*Bit8u*/short row,/*Bit8u*/short col,/*sBit8u*/short flag,/*Bit8u*/short attr,/*PhysPt*/int string,/*Bit16u*/int count,/*Bit8u*/short page) {
        /*Bit8u*/short cur_row=Int10.CURSOR_POS_ROW(page);
        /*Bit8u*/short cur_col=Int10.CURSOR_POS_COL(page);

        // if row=0xff special case : use current cursor position
        if (row==0xff) {
            row=cur_row;
            col=cur_col;
        }
        INT10_SetCursorPos(row,col,page);
        while (count>0) {
            /*Bit8u*/short chr=(short)Memory.mem_readb(string);
            string++;
            if ((flag&2)!=0) {
                attr=(short)Memory.mem_readb(string);
                string++;
            }
            INT10_TeletypeOutputAttr(chr,attr,true,page);
            count--;
        }
        if ((flag&1)==0) {
            INT10_SetCursorPos(cur_row,cur_col,page);
        }
    }

}
