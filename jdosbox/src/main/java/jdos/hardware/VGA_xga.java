package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.misc.Log;
import jdos.util.StringHelper;

public class VGA_xga {
    static private int XGA_SCREEN_WIDTH() {
        return VGA.vga.s3.xga_screen_width;
    }
    static private int XGA_COLOR_MODE() {
        return VGA.vga.s3.xga_color_mode;
    }

    static private final boolean XGA_SHOW_COMMAND_TRACE = false;

    private static class XGAStatus {
        public static class scissorreg {
            /*Bit16u*/int x1, y1, x2, y2;
        }
        public scissorreg scissors = new scissorreg();

        /*Bit32u*/long readmask;
        /*Bit32u*/long writemask;

        /*Bit32u*/int forecolor;
        /*Bit32u*/int backcolor;

        /*Bitu*/int curcommand;

        /*Bit16u*/int foremix;
        /*Bit16u*/int backmix;

        /*Bit16u*/int curx, cury;
        /*Bit16u*/int destx, desty;

        /*Bit16u*/int ErrTerm;
        /*Bit16u*/int MIPcount;
        /*Bit16u*/int MAPcount;

        /*Bit16u*/int pix_cntl;
        /*Bit16u*/int control1;
        /*Bit16u*/int control2;
        /*Bit16u*/int read_sel;

        public static class XGA_WaitCmd {
            boolean newline;
            boolean wait;
            /*Bit16u*/int cmd;
            /*Bit16u*/int curx, cury;
            /*Bit16u*/int x1, y1, x2, y2, sizex, sizey;
            /*Bit32u*/long data; /* transient data passed by multiple calls */
            /*Bitu*/int datasize;
            /*Bitu*/int buswidth;
        }
        public XGA_WaitCmd waitcmd = new XGA_WaitCmd();

    }

    private static XGAStatus xga;

    private static void XGA_Write_Multifunc(/*Bitu*/int val, /*Bitu*/int len) {
        /*Bitu*/int regselect = val >>> 12;
        /*Bitu*/int dataval = val & 0xfff;
        switch(regselect) {
            case 0: // minor axis pixel count
                xga.MIPcount = dataval;
                break;
            case 1: // top scissors
                xga.scissors.y1 = dataval;
                break;
            case 2: // left
                xga.scissors.x1 = dataval;
                break;
            case 3: // bottom
                xga.scissors.y2 = dataval;
                break;
            case 4: // right
                xga.scissors.x2 = dataval;
                break;
            case 0xa: // data manip control
                xga.pix_cntl = dataval;
                break;
            case 0xd: // misc 2
                xga.control2 = dataval;
                break;
            case 0xe:
                xga.control1 = dataval;
                break;
            case 0xf:
                xga.read_sel = dataval;
                break;
            default:
                Log.log_msg("XGA: Unhandled multifunction command "+Integer.toString(regselect,16));
                break;
        }
    }

    private static /*Bitu*/int XGA_Read_Multifunc() {
        switch(xga.read_sel++) {
            case 0: return xga.MIPcount;
            case 1: return xga.scissors.y1;
            case 2: return xga.scissors.x1;
            case 3: return xga.scissors.y2;
            case 4: return xga.scissors.x2;
            case 5: return xga.pix_cntl;
            case 6: return xga.control1;
            case 7: return 0; // TODO
            case 8: return 0; // TODO
            case 9: return 0; // TODO
            case 10: return xga.control2;
            default: return 0;
        }
    }


    static private void XGA_DrawPoint(/*Bitu*/int x, /*Bitu*/int y, /*Bitu*/int c) {
        if((xga.curcommand & 0x1)==0) return;
        if((xga.curcommand & 0x10)==0) return;

        if(x < xga.scissors.x1) return;
        if(x > xga.scissors.x2) return;
        if(y < xga.scissors.y1) return;
        if(y > xga.scissors.y2) return;

        /*Bit32u*/int memaddr = (y * XGA_SCREEN_WIDTH()) + x;
        /* Need to zero out all unused bits in modes that have any (15-bit or "32"-bit -- the last
           one is actually 24-bit. Without this step there may be some graphics corruption (mainly,
           during windows dragging. */
        switch(XGA_COLOR_MODE()) {
            case VGA.M_LIN8:
                if ((memaddr >= VGA.vga.vmemsize)) break;
                RAM.writeb(VGA.vga.mem.linear+memaddr, (short)c);
                break;
            case VGA.M_LIN15:
                if ((memaddr*2 >= VGA.vga.vmemsize)) break;
                RAM.writew(VGA.vga.mem.linear+memaddr*2,c&0x7fff);
                break;
            case VGA.M_LIN16:
                if ((memaddr*2 >= VGA.vga.vmemsize)) break;
                RAM.writew(VGA.vga.mem.linear+memaddr*2,c&0xffff);
                break;
            case VGA.M_LIN32:
                if ((memaddr*4 >= VGA.vga.vmemsize)) break;
                RAM.writed(VGA.vga.mem.linear+memaddr*4,c);
                break;
            default:
                break;
        }

    }

    private static /*Bitu*/int XGA_GetPoint(/*Bitu*/int x, /*Bitu*/int y) {
        /*Bit32u*/int memaddr = (y * XGA_SCREEN_WIDTH()) + x;

        switch(XGA_COLOR_MODE()) {
        case VGA.M_LIN8:
            if ((memaddr >= VGA.vga.vmemsize)) break;
            return RAM.readb(VGA.vga.mem.linear+memaddr);
        case VGA.M_LIN15:
        case VGA.M_LIN16:
            if ((memaddr*2 >= VGA.vga.vmemsize)) break;
            return RAM.readw(VGA.vga.mem.linear+memaddr*2);
        case VGA.M_LIN32:
            if ((memaddr*4 >= VGA.vga.vmemsize)) break;
            return RAM.readd(VGA.vga.mem.linear+memaddr*4);
        default:
            break;
        }
        return 0;
    }


    static private /*Bitu*/int XGA_GetMixResult(/*Bitu*/int mixmode, /*Bitu*/int srcval, /*Bitu*/int dstdata) {
        /*Bitu*/int destval = 0;
        switch(mixmode &  0xf) {
            case 0x00: /* not DST */
                destval = ~dstdata;
                break;
            case 0x01: /* 0 (false) */
                destval = 0;
                break;
            case 0x02: /* 1 (true) */
                destval = 0xffffffff;
                break;
            case 0x03: /* 2 DST */
                destval = dstdata;
                break;
            case 0x04: /* not SRC */
                destval = ~srcval;
                break;
            case 0x05: /* SRC xor DST */
                destval = srcval ^ dstdata;
                break;
            case 0x06: /* not (SRC xor DST) */
                destval = ~(srcval ^ dstdata);
                break;
            case 0x07: /* SRC */
                destval = srcval;
                break;
            case 0x08: /* not (SRC and DST) */
                destval = ~(srcval & dstdata);
                break;
            case 0x09: /* (not SRC) or DST */
                destval = (~srcval) | dstdata;
                break;
            case 0x0a: /* SRC or (not DST) */
                destval = srcval | (~dstdata);
                break;
            case 0x0b: /* SRC or DST */
                destval = srcval | dstdata;
                break;
            case 0x0c: /* SRC and DST */
                destval = srcval & dstdata;
                break;
            case 0x0d: /* SRC and (not DST) */
                destval = srcval & (~dstdata);
                break;
            case 0x0e: /* (not SRC) and DST */
                destval = (~srcval) & dstdata;
                break;
            case 0x0f: /* not (SRC or DST) */
                destval = ~(srcval | dstdata);
                break;
            default:
                Log.log_msg("XGA: GetMixResult: Unknown mix.  Shouldn't be able to get here!");
                break;
        }
        return destval;
    }

    static private void XGA_DrawLineVector(/*Bitu*/int val) {
        /*Bits*/int xat, yat;
        /*Bitu*/int srcval=0;
        /*Bitu*/int destval;
        /*Bitu*/int dstdata;
        /*Bits*/int i;

        /*Bits*/int dx, sx, sy;

        dx = xga.MAPcount;
        xat = xga.curx;
        yat = xga.cury;

        switch((val >> 5) & 0x7) {
            case 0x00: /* 0 degrees */
                sx = 1;
                sy = 0;
                break;
            case 0x01: /* 45 degrees */
                sx = 1;
                sy = -1;
                break;
            case 0x02: /* 90 degrees */
                sx = 0;
                sy = -1;
                break;
            case 0x03: /* 135 degrees */
                sx = -1;
                sy = -1;
                break;
            case 0x04: /* 180 degrees */
                sx = -1;
                sy = 0;
                break;
            case 0x05: /* 225 degrees */
                sx = -1;
                sy = 1;
                break;
            case 0x06: /* 270 degrees */
                sx = 0;
                sy = 1;
                break;
            case 0x07: /* 315 degrees */
                sx = 1;
                sy = 1;
                break;
            default:  // Should never get here
                sx = 0;
                sy = 0;
                break;
        }

        for (i=0;i<=dx;i++) {
            /*Bitu*/int mixmode = (xga.pix_cntl >> 6) & 0x3;
            switch (mixmode) {
                case 0x00: /* FOREMIX always used */
                    mixmode = xga.foremix;
                    switch((mixmode >> 5) & 0x03) {
                        case 0x00: /* Src is background color */
                            srcval = xga.backcolor;
                            break;
                        case 0x01: /* Src is foreground color */
                            srcval = xga.forecolor;
                            break;
                        case 0x02: /* Src is pixel data from PIX_TRANS register */
                            //srcval = tmpval;
                            //Log.log_msg("XGA: DrawRect: Wants data from PIX_TRANS register");
                            break;
                        case 0x03: /* Src is bitmap data */
                            Log.log_msg("XGA: DrawRect: Wants data from srcdata");
                            //srcval = srcdata;
                            break;
                        default:
                            Log.log_msg("XGA: DrawRect: Shouldn't be able to get here!");
                            break;
                    }
                    dstdata = XGA_GetPoint(xat,yat);

                    destval = XGA_GetMixResult(mixmode, srcval, dstdata);

                    XGA_DrawPoint(xat,yat, destval);
                    break;
                default:
                    Log.log_msg("XGA: DrawLine: Needs mixmode "+Integer.toString(mixmode,16));
                    break;
            }
            xat += sx;
            yat += sy;
        }

        xga.curx = xat-1;
        xga.cury = yat;
    }

    static private void XGA_DrawLineBresenham(/*Bitu*/int val) {
        /*Bits*/int xat, yat;
        /*Bitu*/int srcval=0;
        /*Bitu*/int destval;
        /*Bitu*/int dstdata;
        /*Bits*/int i;
        boolean steep;

        /*Bits*/int dx, sx, dy, sy, e, dmajor, dminor,destxtmp;

        // Probably a lot easier way to do this, but this works.

        dminor = (/*Bits*/int)((/*Bit16s*/short)xga.desty);
        if((xga.desty&0x2000)!=0) dminor |= 0xffffe000;
        dminor >>= 1;

        destxtmp=(/*Bits*/int)((/*Bit16s*/short)xga.destx);
        if((xga.destx&0x2000)!=0) destxtmp |= 0xffffe000;


        dmajor = -(destxtmp - (dminor << 1)) >> 1;

        dx = dmajor;
        if(((val >> 5) & 0x1)!=0) {
            sx = 1;
        } else {
            sx = -1;
        }
        dy = dminor;
        if(((val >> 7) & 0x1)!=0) {
            sy = 1;
        } else {
            sy = -1;
        }
        e = (/*Bits*/int)((/*Bit16s*/short)xga.ErrTerm);
        if((xga.ErrTerm&0x2000)!=0) e |= 0xffffe000;
        xat = xga.curx;
        yat = xga.cury;

        if(((val >> 6) & 0x1)!=0) {
            steep = false;
            int t;
            t = xat;
            xat = yat;
            yat = t;

            t = sx;
            sx = sy;
            sy = t;
        } else {
            steep = true;
        }

        //Log.log_msg("XGA: Bresenham: ASC %d, LPDSC %d, sx %d, sy %d, err %d, steep %d, length %d, dmajor %d, dminor %d, xstart %d, ystart %d", dx, dy, sx, sy, e, steep, xga.MAPcount, dmajor, dminor,xat,yat);

        for (i=0;i<=xga.MAPcount;i++) {
                /*Bitu*/int mixmode = (xga.pix_cntl >> 6) & 0x3;
                switch (mixmode) {
                    case 0x00: /* FOREMIX always used */
                        mixmode = xga.foremix;
                        switch((mixmode >> 5) & 0x03) {
                            case 0x00: /* Src is background color */
                                srcval = xga.backcolor;
                                break;
                            case 0x01: /* Src is foreground color */
                                srcval = xga.forecolor;
                                break;
                            case 0x02: /* Src is pixel data from PIX_TRANS register */
                                //srcval = tmpval;
                                Log.log_msg("XGA: DrawRect: Wants data from PIX_TRANS register");
                                break;
                            case 0x03: /* Src is bitmap data */
                                Log.log_msg("XGA: DrawRect: Wants data from srcdata");
                                //srcval = srcdata;
                                break;
                            default:
                                Log.log_msg("XGA: DrawRect: Shouldn't be able to get here!");
                                break;
                        }

                        if(steep) {
                            dstdata = XGA_GetPoint(xat,yat);
                        } else {
                            dstdata = XGA_GetPoint(yat,xat);
                        }

                        destval = XGA_GetMixResult(mixmode, srcval, dstdata);

                        if(steep) {
                            XGA_DrawPoint(xat,yat, destval);
                        } else {
                            XGA_DrawPoint(yat,xat, destval);
                        }

                        break;
                    default:
                        Log.log_msg("XGA: DrawLine: Needs mixmode "+Integer.toString(mixmode,16));
                        break;
                }
                while (e > 0) {
                    yat += sy;
                    e -= (dx << 1);
                }
                xat += sx;
                e += (dy << 1);
        }

        if(steep) {
            xga.curx = xat;
            xga.cury = yat;
        } else {
            xga.curx = yat;
            xga.cury = xat;
        }
        //	}
        //}

    }

    private static void XGA_DrawRectangle(/*Bitu*/int val) {
        /*Bit32u*/long xat, yat;
        /*Bitu*/int srcval=0;
        /*Bitu*/int destval;
        /*Bitu*/int dstdata;

        /*Bits*/int srcx=0, srcy, dx, dy;

        dx = -1;
        dy = -1;

        if(((val >> 5) & 0x01) != 0) dx = 1;
        if(((val >> 7) & 0x01) != 0) dy = 1;

        srcy = xga.cury;

        for(yat=0;yat<=xga.MIPcount;yat++) {
            srcx = xga.curx;
            for(xat=0;xat<=xga.MAPcount;xat++) {
                /*Bitu*/int mixmode = (xga.pix_cntl >> 6) & 0x3;
                switch (mixmode) {
                    case 0x00: /* FOREMIX always used */
                        mixmode = xga.foremix;
                        switch((mixmode >> 5) & 0x03) {
                            case 0x00: /* Src is background color */
                                srcval = xga.backcolor;
                                break;
                            case 0x01: /* Src is foreground color */
                                srcval = xga.forecolor;
                                break;
                            case 0x02: /* Src is pixel data from PIX_TRANS register */
                                //srcval = tmpval;
                                Log.log_msg("XGA: DrawRect: Wants data from PIX_TRANS register");
                                break;
                            case 0x03: /* Src is bitmap data */
                                Log.log_msg("XGA: DrawRect: Wants data from srcdata");
                                //srcval = srcdata;
                                break;
                            default:
                                Log.log_msg("XGA: DrawRect: Shouldn't be able to get here!");
                                break;
                        }
                        dstdata = XGA_GetPoint(srcx,srcy);

                        destval = XGA_GetMixResult(mixmode, srcval, dstdata);

                        XGA_DrawPoint(srcx,srcy, destval);
                        break;
                    default:
                        Log.log_msg("XGA: DrawRect: Needs mixmode "+Integer.toString(mixmode,16));
                        break;
                }
                srcx += dx;
            }
            srcy += dy;
        }
        xga.curx = srcx;
        xga.cury = srcy;

        //Log.log_msg("XGA: Draw rect (%d, %d)-(%d, %d), %d", x1, y1, x2, y2, xga.forecolor);
    }

    private static boolean XGA_CheckX() {
        boolean newline = false;
        if(!xga.waitcmd.newline) {

        if((xga.waitcmd.curx<2048) && xga.waitcmd.curx > (xga.waitcmd.x2)) {
            xga.waitcmd.curx = xga.waitcmd.x1;
            xga.waitcmd.cury++;
            xga.waitcmd.cury&=0x0fff;
            newline = true;
            xga.waitcmd.newline = true;
            if((xga.waitcmd.cury<2048)&&(xga.waitcmd.cury > xga.waitcmd.y2))
                xga.waitcmd.wait = false;
        } else if(xga.waitcmd.curx>=2048) {
            /*Bit16u*/int realx = 4096-xga.waitcmd.curx;
            if(xga.waitcmd.x2>2047) { // x end is negative too
                /*Bit16u*/int realxend=4096-xga.waitcmd.x2;
                if(realx==realxend) {
                    xga.waitcmd.curx = xga.waitcmd.x1;
                    xga.waitcmd.cury++;
                    xga.waitcmd.cury&=0x0fff;
                    newline = true;
                    xga.waitcmd.newline = true;
                    if((xga.waitcmd.cury<2048)&&(xga.waitcmd.cury > xga.waitcmd.y2))
                        xga.waitcmd.wait = false;
                }
            } else { // else overlapping
                if(realx==xga.waitcmd.x2) {
                    xga.waitcmd.curx = xga.waitcmd.x1;
                    xga.waitcmd.cury++;
                    xga.waitcmd.cury&=0x0fff;
                    newline = true;
                    xga.waitcmd.newline = true;
                    if((xga.waitcmd.cury<2048)&&(xga.waitcmd.cury > xga.waitcmd.y2))
                        xga.waitcmd.wait = false;
                    }
                }
            }
        } else {
            xga.waitcmd.newline = false;
        }
        return newline;
}

    private static void XGA_DrawWaitSub(/*Bitu*/int mixmode, /*Bitu*/int srcval) {
        /*Bitu*/int destval;
        /*Bitu*/int dstdata;
        dstdata = XGA_GetPoint(xga.waitcmd.curx, xga.waitcmd.cury);
        destval = XGA_GetMixResult(mixmode, srcval, dstdata);
        //Log.log_msg("XGA: DrawPattern: Mixmode: %x srcval: %x", mixmode, srcval);

        XGA_DrawPoint(xga.waitcmd.curx, xga.waitcmd.cury, destval);
        xga.waitcmd.curx++;
        xga.waitcmd.curx&=0x0fff;
        XGA_CheckX();
    }

    private static void XGA_DrawWait(/*Bitu*/int val, /*Bitu*/int len) {
        if(!xga.waitcmd.wait) return;
        /*Bitu*/int mixmode = (xga.pix_cntl >> 6) & 0x3;
        /*Bitu*/int srcval;
        switch(xga.waitcmd.cmd) {
            case 2: /* Rectangle */
                switch(mixmode) {
                    case 0x00: /* FOREMIX always used */
                        mixmode = xga.foremix;

    /*					switch((mixmode >> 5) & 0x03) {
                            case 0x00: // Src is background color
                                srcval = xga.backcolor;
                                break;
                            case 0x01: // Src is foreground color
                                srcval = xga.forecolor;
                                break;
                            case 0x02: // Src is pixel data from PIX_TRANS register
    */
                        if(((mixmode >> 5) & 0x03) != 0x2) {
                            // those cases don't seem to occur
                            Log.log_msg("XGA: unsupported drawwait operation");
                            break;
                        }
                        switch(xga.waitcmd.buswidth) {
                            case VGA.M_LIN8:		//  8 bit
                                XGA_DrawWaitSub(mixmode, val);
                                break;
                            case 0x20 | VGA.M_LIN8: // 16 bit
                                for(/*Bitu*/int i = 0; i < len; i++) {
                                    XGA_DrawWaitSub(mixmode, (val>>(8*i))&0xff);
                                    if(xga.waitcmd.newline) break;
                                }
                                break;
                            case 0x40 | VGA.M_LIN8: // 32 bit
                                for(int i = 0; i < 4; i++)
                                    XGA_DrawWaitSub(mixmode, (val>>(8*i))&0xff);
                                break;
                            case (0x20 | VGA.M_LIN32):
                                if(len!=4) { // Win 3.11 864 'hack?'
                                    if(xga.waitcmd.datasize == 0) {
                                        // set it up to wait for the next word
                                        xga.waitcmd.data = val;
                                        xga.waitcmd.datasize = 2;
                                        return;
                                    } else {
                                        srcval = (int)((val<<16)|xga.waitcmd.data);
                                        xga.waitcmd.data = 0;
                                        xga.waitcmd.datasize = 0;
                                        XGA_DrawWaitSub(mixmode, srcval);
                                    }
                                    break;
                                } // fall-through
                            case 0x40 | VGA.M_LIN32: // 32 bit
                                XGA_DrawWaitSub(mixmode, val);
                                break;
                            case 0x20 | VGA.M_LIN15: // 16 bit
                            case 0x20 | VGA.M_LIN16: // 16 bit
                                XGA_DrawWaitSub(mixmode, val);
                                break;
                            case 0x40 | VGA.M_LIN15: // 32 bit
                            case 0x40 | VGA.M_LIN16: // 32 bit
                                XGA_DrawWaitSub(mixmode, val&0xffff);
                                if(!xga.waitcmd.newline)
                                    XGA_DrawWaitSub(mixmode, val>>16);
                                break;
                            default:
                                // Let's hope they never show up ;)
                                Log.log_msg("XGA: unsupported bpp / datawidth combination "+
                                    Integer.toString(xga.waitcmd.buswidth,16));
                                break;
                        };
                        break;

                    case 0x02: // Data from PIX_TRANS selects the mix
                        /*Bitu*/int chunksize=0;
                        /*Bitu*/int chunks=0;
                        switch(xga.waitcmd.buswidth&0x60) {
                            case 0x0:
                                chunksize=8;
                                chunks=1;
                                break;
                            case 0x20: // 16 bit
                                chunksize=16;
                                if(len==4) chunks=2;
                                else chunks = 1;
                                break;
                            case 0x40: // 32 bit
                                chunksize=16;
                                if(len==4) chunks=2;
                                else chunks = 1;
                                break;
                            case 0x60: // undocumented guess (but works)
                                chunksize=8;
                                chunks=4;
                                break;
                        }

                        for(/*Bitu*/int k = 0; k < chunks; k++) { // chunks counter
                            xga.waitcmd.newline = false;
                            for(/*Bitu*/int n = 0; n < chunksize; n++) { // pixels
                                /*Bitu*/int mixmode1;

                                // This formula can rule the world ;)
                                /*Bitu*/int mask = 1 << ((((n&0xF8)+(8-(n&0x7)))-1)+chunksize*k);
                                if((val&mask)!=0) mixmode1 = xga.foremix;
                                else mixmode1 = xga.backmix;

                                switch((mixmode1 >> 5) & 0x03) {
                                    case 0x00: // Src is background color
                                        srcval = xga.backcolor;
                                        break;
                                    case 0x01: // Src is foreground color
                                        srcval = xga.forecolor;
                                        break;
                                    default:
                                        Log.log_msg("XGA: DrawBlitWait: Unsupported src "+
                                            Integer.toString((mixmode1 >> 5) & 0x03,16));
                                        srcval=0;
                                        break;
                                }
                                XGA_DrawWaitSub(mixmode1, srcval);

                                if((xga.waitcmd.cury<2048) &&
                                  (xga.waitcmd.cury >= xga.waitcmd.y2)) {
                                    xga.waitcmd.wait = false;
                                    k=1000; // no more chunks
                                    break;
                                }
                                // next chunk goes to next line
                                if(xga.waitcmd.newline) break;
                            } // pixels loop
                        } // chunks loop
                        break;

                    default:
                        Log.log_msg("XGA: DrawBlitWait: Unhandled mixmode: "+mixmode);
                        break;
                } // switch mixmode
                break;
            default:
                Log.log_msg("XGA: Unhandled draw command "+Integer.toString(xga.waitcmd.cmd,16));
                break;
        }
    }

    private static void XGA_BlitRect(/*Bitu*/int val) {
        /*Bit32u*/long xat, yat;
        /*Bitu*/int srcdata;
        /*Bitu*/int dstdata;

        /*Bitu*/int srcval=0;
        /*Bitu*/int destval;

        /*Bits*/int srcx, srcy, tarx, tary, dx, dy;

        dx = -1;
        dy = -1;

        if(((val >> 5) & 0x01) != 0) dx = 1;
        if(((val >> 7) & 0x01) != 0) dy = 1;

        srcx = xga.curx;
        srcy = xga.cury;
        tarx = xga.destx;
        tary = xga.desty;

        /*Bitu*/int mixselect = (xga.pix_cntl >> 6) & 0x3;
        /*Bitu*/int mixmode = 0x67; /* Source is bitmap data, mix mode is src */
        switch(mixselect) {
            case 0x00: /* Foreground mix is always used */
                mixmode = xga.foremix;
                break;
            case 0x02: /* CPU Data determines mix used */
                Log.log_msg("XGA: DrawPattern: Mixselect data from PIX_TRANS register");
                break;
            case 0x03: /* Video memory determines mix */
                //Log.log_msg("XGA: Srcdata: %x, Forecolor %x, Backcolor %x, Foremix: %x Backmix: %x", srcdata, xga.forecolor, xga.backcolor, xga.foremix, xga.backmix);
                break;
            default:
                Log.log_msg("XGA: BlitRect: Unknown mix select register");
                break;
        }


        /* Copy source to video ram */
        for(yat=0;yat<=xga.MIPcount ;yat++) {
            srcx = xga.curx;
            tarx = xga.destx;

            for(xat=0;xat<=xga.MAPcount;xat++) {
                srcdata = XGA_GetPoint(srcx, srcy);
                dstdata = XGA_GetPoint(tarx, tary);

                if(mixselect == 0x3) {
                    if(srcdata == xga.forecolor) {
                        mixmode = xga.foremix;
                    } else {
                        if(srcdata == xga.backcolor) {
                            mixmode = xga.backmix;
                        } else {
                            /* Best guess otherwise */
                            mixmode = 0x67; /* Source is bitmap data, mix mode is src */
                        }
                    }
                }

                switch((mixmode >> 5) & 0x03) {
                    case 0x00: /* Src is background color */
                        srcval = xga.backcolor;
                        break;
                    case 0x01: /* Src is foreground color */
                        srcval = xga.forecolor;
                        break;
                    case 0x02: /* Src is pixel data from PIX_TRANS register */
                        Log.log_msg("XGA: DrawPattern: Wants data from PIX_TRANS register");
                        break;
                    case 0x03: /* Src is bitmap data */
                        srcval = srcdata;
                        break;
                    default:
                        Log.log_msg("XGA: DrawPattern: Shouldn't be able to get here!");
                        srcval = 0;
                        break;
                }

                destval = XGA_GetMixResult(mixmode, srcval, dstdata);
                //Log.log_msg("XGA: DrawPattern: Mixmode: %x Mixselect: %x", mixmode, mixselect);

                XGA_DrawPoint(tarx, tary, destval);

                srcx += dx;
                tarx += dx;
            }
            srcy += dy;
            tary += dy;
        }
    }

    static private void XGA_DrawPattern(/*Bitu*/int val) {
        /*Bitu*/int srcdata;
        /*Bitu*/int dstdata;

        /*Bitu*/int srcval=0;
        /*Bitu*/int destval;

        /*Bits*/int xat, yat, srcx, srcy, tarx, tary, dx, dy;

        dx = -1;
        dy = -1;

        if(((val >> 5) & 0x01) != 0) dx = 1;
        if(((val >> 7) & 0x01) != 0) dy = 1;

        srcx = xga.curx;
        srcy = xga.cury;

        tary = xga.desty;

        /*Bitu*/int mixselect = (xga.pix_cntl >> 6) & 0x3;
        /*Bitu*/int mixmode = 0x67; /* Source is bitmap data, mix mode is src */
        switch(mixselect) {
            case 0x00: /* Foreground mix is always used */
                mixmode = xga.foremix;
                break;
            case 0x02: /* CPU Data determines mix used */
                Log.log_msg("XGA: DrawPattern: Mixselect data from PIX_TRANS register");
                break;
            case 0x03: /* Video memory determines mix */
                //Log.log_msg("XGA: Pixctl: %x, Srcdata: %x, Forecolor %x, Backcolor %x, Foremix: %x Backmix: %x",xga.pix_cntl, srcdata, xga.forecolor, xga.backcolor, xga.foremix, xga.backmix);
                break;
            default:
                Log.log_msg("XGA: DrawPattern: Unknown mix select register");
                break;
        }

        for(yat=0;yat<=xga.MIPcount;yat++) {
            tarx = xga.destx;
            for(xat=0;xat<=xga.MAPcount;xat++) {

                srcdata = XGA_GetPoint(srcx + (tarx & 0x7), srcy + (tary & 0x7));
                //Log.log_msg("patternpoint (%3d/%3d)v%x",srcx + (tarx & 0x7), srcy + (tary & 0x7),srcdata);
                dstdata = XGA_GetPoint(tarx, tary);


                if(mixselect == 0x3) {
                    // TODO lots of guessing here but best results this way
                    /*if(srcdata == xga.forecolor)*/ mixmode = xga.foremix;
                    // else
                    if(srcdata == xga.backcolor || srcdata == 0)
                        mixmode = xga.backmix;
                }

                switch((mixmode >> 5) & 0x03) {
                    case 0x00: /* Src is background color */
                        srcval = xga.backcolor;
                        break;
                    case 0x01: /* Src is foreground color */
                        srcval = xga.forecolor;
                        break;
                    case 0x02: /* Src is pixel data from PIX_TRANS register */
                        Log.log_msg("XGA: DrawPattern: Wants data from PIX_TRANS register");
                        break;
                    case 0x03: /* Src is bitmap data */
                        srcval = srcdata;
                        break;
                    default:
                        Log.log_msg("XGA: DrawPattern: Shouldn't be able to get here!");
                        srcval = 0;
                        break;
                }

                destval = XGA_GetMixResult(mixmode, srcval, dstdata);

                XGA_DrawPoint(tarx, tary, destval);

                tarx += dx;
            }
            tary += dy;
        }
    }

    static private void XGA_DrawCmd(/*Bitu*/int val, /*Bitu*/int len) {
        /*Bit16u*/int cmd;
        cmd = val >> 13;
        //if (XGA_SHOW_COMMAND_TRACE)
            //Log.log_msg("XGA: Draw command %x", cmd);

        xga.curcommand = val;
        switch(cmd) {
            case 1: /* Draw line */
                if((val & 0x100) == 0) {
                    if((val & 0x8) == 0) {
                        if (XGA_SHOW_COMMAND_TRACE)
                            Log.log_msg("XGA: Drawing Bresenham line");
                        XGA_DrawLineBresenham(val);
                    } else {
                        if (XGA_SHOW_COMMAND_TRACE)
                            Log.log_msg("XGA: Drawing vector line");
                        XGA_DrawLineVector(val);
                    }
                } else {
                    Log.log_msg("XGA: Wants line drawn from PIX_TRANS register!");
                }
                break;
            case 2: /* Rectangle fill */
                if((val & 0x100) == 0) {
                    xga.waitcmd.wait = false;
                    if (XGA_SHOW_COMMAND_TRACE)
                        Log.log_msg(StringHelper.sprintf("XGA: Draw immediate rect: xy(%3d/%3d), len(%3d/%3d)", new Object[] {new Integer(xga.curx),new Integer(xga.cury),new Integer(xga.MAPcount),new Integer(xga.MIPcount)}));
                    XGA_DrawRectangle(val);

                } else {

                    xga.waitcmd.newline = true;
                    xga.waitcmd.wait = true;
                    xga.waitcmd.curx = xga.curx;
                    xga.waitcmd.cury = xga.cury;
                    xga.waitcmd.x1 = xga.curx;
                    xga.waitcmd.y1 = xga.cury;
                    xga.waitcmd.x2 = (/*Bit16u*/int)((xga.curx + xga.MAPcount)&0x0fff);
                    xga.waitcmd.y2 = (/*Bit16u*/int)((xga.cury + xga.MIPcount + 1)&0x0fff);
                    xga.waitcmd.sizex = xga.MAPcount;
                    xga.waitcmd.sizey = xga.MIPcount + 1;
                    xga.waitcmd.cmd = 2;
                    xga.waitcmd.buswidth = VGA.vga.mode | ((val&0x600) >> 4);
                    xga.waitcmd.data = 0;
                    xga.waitcmd.datasize = 0;

                    if (XGA_SHOW_COMMAND_TRACE)
                        Log.log_msg(StringHelper.sprintf("XGA: Draw wait rect, w/h(%3d/%3d), x/y1(%3d/%3d), x/y2(%3d/%3d), %4x",
                            new Object[] {new Integer(xga.MAPcount+1), new Integer(xga.MIPcount+1),new Integer(xga.curx),new Integer(xga.cury),
                            new Integer((xga.curx + xga.MAPcount)&0x0fff),
                            new Integer((xga.cury + xga.MIPcount + 1)&0x0fff),new Integer(val&0xffff)}));
                }
                break;
            case 6: /* BitBLT */
                if (XGA_SHOW_COMMAND_TRACE)
                    Log.log_msg("XGA: Blit Rect");
                XGA_BlitRect(val);
                break;
            case 7: /* Pattern fill */
                if (XGA_SHOW_COMMAND_TRACE)
                    Log.log_msg(StringHelper.sprintf("XGA: Pattern fill: src(%3d/%3d), dest(%3d/%3d), fill(%3d/%3d)",
                        new Object[]{new Integer(xga.curx),new Integer(xga.cury),new Integer(xga.destx),new Integer(xga.desty),new Integer(xga.MAPcount),new Integer(xga.MIPcount)}));
                XGA_DrawPattern(val);
                break;
            default:
                Log.log_msg("XGA: Unhandled draw command "+Integer.toString(cmd,16));
                break;
        }
    }

    static private long XGA_SetDualReg(/*Bit32u*/long reg, /*Bitu*/int val) {
        long result = reg;
        switch(XGA_COLOR_MODE()) {
        case VGA.M_LIN8:
            result = (val&0xff); break;
        case VGA.M_LIN15:
        case VGA.M_LIN16:
            result = (val&0xffff); break;
        case VGA.M_LIN32:
            if ((xga.control1 & 0x200)!=0)
                result = val;
            else if ((xga.control1 & 0x10)!=0)
                result = (result&0x0000ffff)|(val<<16);
            else
                result = (result&0xffff0000l)|(val&0x0000ffff);
            xga.control1 ^= 0x10;
            break;
        }
        return result;
    }

    static private /*Bitu*/int XGA_GetDualReg(/*Bit32u*/long reg) {
        switch(XGA_COLOR_MODE()) {
        case VGA.M_LIN8:
            return (int)(reg&0xff);
        case VGA.M_LIN15: case VGA.M_LIN16:
            return (/*Bit16u*/int)(reg&0xffff);
        case VGA.M_LIN32:
            if ((xga.control1 & 0x200)!=0) return (int)reg;
            xga.control1 ^= 0x10;
            if ((xga.control1 & 0x10)!=0) return (int)reg&0x0000ffff;
            else return (int)(reg>>>16);
        }
        return 0;
    }

    public static IoHandler.IO_WriteHandler XGA_Write = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int len) {
        //	Log.log_msg("XGA: Write to port %x, val %8x, len %x", port,val, len);

            switch(port) {
                case 0x8100:// drawing control: row (low word), column (high word)
                            // "CUR_X" and "CUR_Y" (see PORT 82E8h,PORT 86E8h)
                    xga.cury = val & 0x0fff;
                    if(len==4) xga.curx = (val>>16)&0x0fff;
                    break;
                case 0x8102:
                    xga.curx = val& 0x0fff;
                    break;

                case 0x8108:// DWORD drawing control: destination Y and axial step
                            // constant (low word), destination X and axial step
                            // constant (high word) (see PORT 8AE8h,PORT 8EE8h)
                    xga.desty = val&0x3FFF;
                    if(len==4) xga.destx = (val>>16)&0x3fff;
                    break;
                case 0x810a:
                    xga.destx = val&0x3fff;
                    break;
                case 0x8110: // WORD error term (see PORT 92E8h)
                    xga.ErrTerm = val&0x3FFF;
                    break;

                case 0x8120: // packed MMIO: DWORD background color (see PORT A2E8h)
                    xga.backcolor = val;
                    break;
                case 0x8124: // packed MMIO: DWORD foreground color (see PORT A6E8h)
                    xga.forecolor = val;
                    break;
                case 0x8128: // DWORD	write mask (see PORT AAE8h)
                    xga.writemask = val;
                    break;
                case 0x812C: // DWORD	read mask (see PORT AEE8h)
                    xga.readmask = val;
                    break;
                case 0x8134: // packed MMIO: DWORD	background mix (low word) and
                             // foreground mix (high word)	(see PORT B6E8h,PORT BAE8h)
                    xga.backmix = val&0xFFFF;
                    if(len==4) xga.foremix = (val>>16);
                    break;
                case 0x8136:
                    xga.foremix = val;
                    break;
                case 0x8138:// DWORD top scissors (low word) and left scissors (high
                            // word) (see PORT BEE8h,#P1047)
                    xga.scissors.y1=val&0x0fff;
                    if(len==4) xga.scissors.x1 = (val>>16)&0x0fff;
                    break;
                case 0x813a:
                    xga.scissors.x1 = val&0x0fff;
                    break;
                case 0x813C:// DWORD bottom scissors (low word) and right scissors
                            // (high word) (see PORT BEE8h,#P1047)
                    xga.scissors.y2=val&0x0fff;
                    if(len==4) xga.scissors.x2 = (val>>16)&0x0fff;
                    break;
                case 0x813e:
                    xga.scissors.x2 = val&0x0fff;
                    break;

                case 0x8140:// DWORD data manipulation control (low word) and
                            // miscellaneous 2 (high word) (see PORT BEE8h,#P1047)
                    xga.pix_cntl=val&0xFFFF;
                    if(len==4) xga.control2=(val>>16)&0x0fff;
                    break;
                case 0x8144:// DWORD miscellaneous (low word) and read register select
                            // (high word)(see PORT BEE8h,#P1047)
                    xga.control1=val&0xffff;
                    if(len==4)xga.read_sel=(val>>16)&0x7;
                    break;
                case 0x8148:// DWORD minor axis pixel count (low word) and major axis
                            // pixel count (high word) (see PORT BEE8h,#P1047,PORT 96E8h)
                    xga.MIPcount = val&0x0fff;
                    if(len==4) xga.MAPcount = (val>>16)&0x0fff;
                    break;
                case 0x814a:
                    xga.MAPcount = val&0x0fff;
                    break;
                case 0x92e8:
                    xga.ErrTerm = val&0x3FFF;
                    break;
                case 0x96e8:
                    xga.MAPcount = val&0x0fff;
                    break;
                case 0x9ae8:
                case 0x8118: // Trio64V+ packed MMIO
                    XGA_DrawCmd(val, len);
                    break;
                case 0xa2e8:
                    xga.backcolor = (int)XGA_SetDualReg(xga.backcolor, val);
                    break;
                case 0xa6e8:
                    xga.forecolor = (int)XGA_SetDualReg(xga.forecolor, val);
                    break;
                case 0xaae8:
                    xga.writemask = (int)XGA_SetDualReg(xga.writemask, val);
                    break;
                case 0xaee8:
                    xga.readmask = (int)XGA_SetDualReg(xga.readmask, val);
                    break;
                case 0x82e8:
                    xga.cury = val&0x0fff;
                    break;
                case 0x86e8:
                    xga.curx = val&0x0fff;
                    break;
                case 0x8ae8:
                    xga.desty = val&0x3fff;
                    break;
                case 0x8ee8:
                    xga.destx = val&0x3fff;
                    break;
                case 0xb2e8:
                    Log.log_msg("COLOR_CMP not implemented");
                    break;
                case 0xb6e8:
                    xga.backmix = val;
                    break;
                case 0xbae8:
                    xga.foremix = val;
                    break;
                case 0xbee8:
                    XGA_Write_Multifunc(val, len);
                    break;
                case 0xe2e8:
                    xga.waitcmd.newline = false;
                    XGA_DrawWait(val, len);
                    break;
                case 0x83d4:
                    if(len==1) VGA_crtc.vga_write_p3d4.call(0,val,1);
                    else if(len==2) {
                        VGA_crtc.vga_write_p3d4.call(0,val&0xff,1);
                        VGA_crtc.vga_write_p3d5.call(0,val>>8,1);
                    }
                    else Log.exit("unimplemented XGA MMIO");
                    break;
                case 0x83d5:
                    if(len==1) VGA_crtc.vga_write_p3d5.call(0,val,1);
                    else Log.exit("unimplemented XGA MMIO");
                    break;
                default:
                    if(port <= 0x4000) {
                        //Log.log_msg("XGA: Wrote to port %4x with %08x, len %x", port, val, len);
                        xga.waitcmd.newline = false;
                        XGA_DrawWait(val, len);

                    }
                    else Log.log_msg("XGA: Wrote to port "+Integer.toString(port, 16)+" with "+Integer.toString(val, 16)+", len "+Integer.toString(len));
                    break;
            }
        }
    };

    public static IoHandler.IO_ReadHandler XGA_Read = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int len) {
            switch(port) {
                case 0x8118:
                case 0x9ae8:
                    return 0x400; // nothing busy
                case 0x81ec: // S3 video data processor
                    return 0x00007000;
                case 0x83da:
                    {
                        /*Bits*/int delaycyc = CPU.CPU_CycleMax/5000;
                        if((CPU.CPU_Cycles < 3*delaycyc)) delaycyc = 0;
                        CPU.CPU_Cycles -= delaycyc;
                        CPU.CPU_IODelayRemoved += delaycyc;
                        return VGA_misc.vga_read_p3da.call(0,0);
                    }
                case 0x83d4:
                    if(len==1) return VGA_crtc.vga_read_p3d4.call(0,0);
                    else Log.exit("unimplemented XGA MMIO");
                    break;
                case 0x83d5:
                    if(len==1) return VGA_crtc.vga_read_p3d5.call(0,0);
                    else Log.exit("unimplemented XGA MMIO");
                    break;
                case 0x9ae9:
                    if(xga.waitcmd.wait) return 0x4;
                    else return 0x0;
                case 0xbee8:
                    return XGA_Read_Multifunc();
                case 0xa2e8:
                    return XGA_GetDualReg(xga.backcolor);
                case 0xa6e8:
                    return XGA_GetDualReg(xga.forecolor);
                case 0xaae8:
                    return XGA_GetDualReg(xga.writemask);
                case 0xaee8:
                    return XGA_GetDualReg(xga.readmask);
                default:
                    //Log.log_msg("XGA: Read from port %x, len %x", port, len);
                    break;
            }
            return 0xffffffff;
        }
    };

    static public void VGA_SetupXGA() {
        if (!Dosbox.IS_VGA_ARCH()) return;

        xga = new XGAStatus();

        xga.scissors.y1 = 0;
        xga.scissors.x1 = 0;
        xga.scissors.y2 = 0xFFF;
        xga.scissors.x2 = 0xFFF;

        IoHandler.IO_RegisterWriteHandler(0x42e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x42e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x46e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x4ae8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x82e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x82e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x82e9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x82e9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x86e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x86e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x86e9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x86e9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x8ae8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x8ae8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x8ee8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x8ee8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x8ee9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x8ee9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x92e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x92e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x92e9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x92e9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x96e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x96e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x96e9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x96e9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x9ae8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x9ae8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x9ae9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x9ae9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0x9ee8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x9ee8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0x9ee9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0x9ee9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xa2e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xa2e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xa6e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xa6e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0xa6e9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xa6e9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xaae8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xaae8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0xaae9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xaae9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xaee8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xaee8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0xaee9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xaee9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xb2e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xb2e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0xb2e9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xb2e9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xb6e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xb6e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xbee8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xbee8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0xbee9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xbee9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xbae8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xbae8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterWriteHandler(0xbae9,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xbae9,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xe2e8,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xe2e8,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xe2e0,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xe2e0,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);

        IoHandler.IO_RegisterWriteHandler(0xe2ea,XGA_Write,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
        IoHandler.IO_RegisterReadHandler(0xe2ea,XGA_Read,IoHandler.IO_MB | IoHandler.IO_MW | IoHandler.IO_MD);
    }
}
