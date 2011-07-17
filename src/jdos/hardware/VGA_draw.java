package jdos.hardware;

import jdos.Dosbox;
import jdos.gui.Render;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;
import jdos.util.IntPtr;
import jdos.util.Ptr;
import jdos.util.ShortPtr;
import jdos.util.StringHelper;

public class VGA_draw {
    static private final int VGA_PARTS = 4;

    static private interface VGA_Line_Handler {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line);
    }

    static private VGA_Line_Handler VGA_DrawLine;
    static private Ptr TempLine = new Ptr(1920 * 4);

    private static VGA_Line_Handler VGA_Draw_1BPP_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr base = new Ptr(VGA.vga.tandy.draw_base,  + ((line & VGA.vga.tandy.line_mask) << VGA.vga.tandy.line_shift));
            IntPtr draw = new IntPtr(TempLine);
            for (/*Bitu*/int x=VGA.vga.draw.blocks;x>0;x--, vidstart++) {
                /*Bitu*/int val = base.get((vidstart & (8 * 1024 -1)));
                draw.setInc(VGA.CGA_2_Table[val >> 4]);
                draw.setInc(VGA.CGA_2_Table[val & 0xf]);
            }
            return TempLine;
        }
    };

    private static VGA_Line_Handler VGA_Draw_2BPP_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr base = new Ptr(VGA.vga.tandy.draw_base, ((line & VGA.vga.tandy.line_mask) << VGA.vga.tandy.line_shift));
            IntPtr draw=new IntPtr(TempLine);
            for (/*Bitu*/int x=0;x<VGA.vga.draw.blocks;x++) {
                /*Bitu*/int val = base.get(vidstart & VGA.vga.tandy.addr_mask);
                vidstart++;
                draw.setInc(VGA.CGA_4_Table[val]);
            }
            return TempLine;
        }
    };

    private static VGA_Line_Handler VGA_Draw_2BPPHiRes_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr base = new Ptr(VGA.vga.tandy.draw_base, ((line & VGA.vga.tandy.line_mask) << VGA.vga.tandy.line_shift));
            IntPtr draw=new IntPtr(TempLine);
            for (/*Bitu*/int x=0;x<VGA.vga.draw.blocks;x++) {
                /*Bitu*/int val1 = base.get(vidstart & VGA.vga.tandy.addr_mask);
                ++vidstart;
                /*Bitu*/int val2 = base.get(vidstart & VGA.vga.tandy.addr_mask);
                ++vidstart;
                draw.setInc(VGA.CGA_4_HiRes_Table[(val1>>4)|(val2&0xf0)]);
                draw.setInc(VGA.CGA_4_HiRes_Table[(val1&0x0f)|((val2&0x0f)<<4)]);
            }
            return TempLine;
        }
    };

    private static /*Bitu*/int[] temp = new int[643];

    private static VGA_Line_Handler VGA_Draw_CGA16_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr base = new Ptr(VGA.vga.tandy.draw_base, ((line & VGA.vga.tandy.line_mask) << VGA.vga.tandy.line_shift));
            Ptr reader = new Ptr(base, vidstart);
            IntPtr draw=new IntPtr(TempLine);
            //Generate a temporary bitline to calculate the avarage
            //over bit-2  bit-1  bit  bit+1.
            //Combine this number with the current colour to get
            //an unigue index in the pallete. Or it with bit 7 as they are stored
            //in the upperpart to keep them from interfering the regular cga stuff

            for(/*Bitu*/int x = 0; x < 640; x++)
                temp[x+2] = (( reader.get((x>>3)) >> (7-(x&7)) )&1) << 4;
                //shift 4 as that is for the index.
            /*Bitu*/int i = 0,temp1,temp2,temp3,temp4;
            for (/*Bitu*/int x=0;x<VGA.vga.draw.blocks;x++) {
                /*Bitu*/int val1 = (int)reader.getInc();
                /*Bitu*/int val2 = val1&0xf;
                val1 >>= 4;

                temp1 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                temp2 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                temp3 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                temp4 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;

                draw.setInc(0x80808080|(temp1|val1) |
                          ((temp2|val1) << 8) |
                          ((temp3|val1) <<16) |
                          ((temp4|val1) <<24));
                temp1 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                temp2 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                temp3 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                temp4 = temp[i] + temp[i+1] + temp[i+2] + temp[i+3]; i++;
                draw.setInc(0x80808080|(temp1|val2) |
                          ((temp2|val2) << 8) |
                          ((temp3|val2) <<16) |
                          ((temp4|val2) <<24));
            }
            return TempLine;
        }
    };

    private static VGA_Line_Handler VGA_Draw_4BPP_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr base = new Ptr(VGA.vga.tandy.draw_base, ((line & VGA.vga.tandy.line_mask) << VGA.vga.tandy.line_shift));
            IntPtr draw=new IntPtr(TempLine);
            for (/*Bitu*/int x=0;x<VGA.vga.draw.blocks;x++) {
                /*Bitu*/int val1 = base.get(vidstart & VGA.vga.tandy.addr_mask);
                ++vidstart;
                /*Bitu*/int val2 = base.get(vidstart & VGA.vga.tandy.addr_mask);
                ++vidstart;
                draw.setInc((val1 & 0x0f) << 8  |
                        (val1 & 0xf0) >> 4  |
                        (val2 & 0x0f) << 24 |
                        (val2 & 0xf0) << 12);
            }
            return TempLine;
        }
    };

    private static VGA_Line_Handler VGA_Draw_4BPP_Line_Double = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr base = new Ptr(VGA.vga.tandy.draw_base, ((line & VGA.vga.tandy.line_mask) << VGA.vga.tandy.line_shift));
            IntPtr draw=new IntPtr(TempLine);
            for (/*Bitu*/int x=0;x<VGA.vga.draw.blocks;x++) {
                /*Bitu*/int val = base.get(vidstart & VGA.vga.tandy.addr_mask);
                ++vidstart;
                draw.setInc((val & 0xf0) >> 4  |
                        (val & 0xf0) << 4  |
                        (val & 0x0f) << 16 |
                        (val & 0x0f) << 24);
            }
            return TempLine;
        }
    };

    //#ifdef VGA_KEEP_CHANGES
    private static VGA_Line_Handler VGA_Draw_Changes_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            /*Bitu*/int checkMask = VGA.vga.changes.checkMask;
            Ptr map = new Ptr(VGA.vga.changes.map);
            /*Bitu*/int start = (vidstart >> VGA.VGA_CHANGE_SHIFT);
            /*Bitu*/int end = ((vidstart + VGA.vga.draw.line_length ) >> VGA.VGA_CHANGE_SHIFT);
            for (; start <= end;start++) {
                if ((map.get(start) & checkMask)!=0) {
                    /*Bitu*/int offset = vidstart & VGA.vga.draw.linear_mask;
                    if(VGA.vga.draw.linear_mask-offset < VGA.vga.draw.line_length)
                        Ptr.memcpy(new Ptr(VGA.vga.draw.linear_base,VGA.vga.draw.linear_mask+1), VGA.vga.draw.linear_base, VGA.vga.draw.line_length);
                    return new Ptr(VGA.vga.draw.linear_base, offset);
                }
            }
        //	memset( TempLine, 0x30, VGA.vga.changes.lineWidth );
        //	return TempLine;
            return null;
        }
    };

    private static VGA_Line_Handler VGA_Draw_Linear_Line = new VGA_Line_Handler() {
        private Ptr p = new Ptr();
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
        // There is guaranteed extra memory past the wrap boundary. So, instead of using temporary
        // storage just copy appropriate chunk from the beginning to the wrap boundary when needed.
            /*Bitu*/int offset = vidstart & VGA.vga.draw.linear_mask;
            if (VGA.vga.draw.linear_mask-offset < VGA.vga.draw.line_length)
                Ptr.memcpy(new Ptr(VGA.vga.draw.linear_base,VGA.vga.draw.linear_mask+1), VGA.vga.draw.linear_base, VGA.vga.draw.line_length);
            p.p = VGA.vga.draw.linear_base.p;
            p.off = offset+VGA.vga.draw.linear_base.off;
            p.size = p.p.length;
            return p;
        }
    };

    private static VGA_Line_Handler VGA_Draw_Xlat16_Linear_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            Ptr ret = new Ptr(VGA.vga.draw.linear_base, vidstart & VGA.vga.draw.linear_mask);
            ShortPtr temps = new ShortPtr(TempLine);
            for(/*Bitu*/int i = 0; i < VGA.vga.draw.line_length; i++) {
                temps.set(i,VGA.vga.dac.xlat16[ret.get(i)]);
            }
            return TempLine;
        }
    };

    //Test version, might as well keep it
    /* static Bit8u * VGA_Draw_Chain_Line(Bitu vidstart, Bitu line) {
        Bitu i = 0;
        for ( i = 0; i < VGA.vga.draw.width;i++ ) {
            Bitu addr = vidstart + i;
            TempLine[i] = VGA.vga.mem.linear[((addr&~3)<<2)+(addr&3)];
        }
        return TempLine;
    } */

    private static VGA_Line_Handler VGA_Draw_VGA_Line_HWMouse = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            if (VGA.svga.hardware_cursor_active==null || !VGA.svga.hardware_cursor_active.call())
                // HW Mouse not enabled, use the tried and true call
                return new Ptr(VGA.vga.mem.linear,vidstart);

            /*Bitu*/int lineat = (vidstart-(VGA.vga.config.real_start<<2)) / VGA.vga.draw.width;
            if ((VGA.vga.s3.hgc.posx >= VGA.vga.draw.width) ||
                (lineat < VGA.vga.s3.hgc.originy) ||
                (lineat > (VGA.vga.s3.hgc.originy + (63-VGA.vga.s3.hgc.posy))) ) {
                // the mouse cursor *pattern* is not on this line
                return new Ptr(VGA.vga.mem.linear,vidstart);
            } else {
                // Draw mouse cursor: cursor is a 64x64 pattern which is shifted (inside the
                // 64x64 mouse cursor space) to the right by posx pixels and up by posy pixels.
                // This is used when the mouse cursor partially leaves the screen.
                // It is arranged as bitmap of 16bits of bitA followed by 16bits of bitB, each
                // AB bits corresponding to a cursor pixel. The whole map is 8kB in size.
                Ptr.memcpy(TempLine, new Ptr(VGA.vga.mem.linear,vidstart), VGA.vga.draw.width);
                // the index of the bit inside the cursor bitmap we start at:
                /*Bitu*/int sourceStartBit = ((lineat - VGA.vga.s3.hgc.originy) + VGA.vga.s3.hgc.posy)*64 + VGA.vga.s3.hgc.posx;
                // convert to video memory addr and bit index
                // start adjusted to the pattern structure (thus shift address by 2 instead of 3)
                // Need to get rid of the third bit, so "/8 *2" becomes ">> 2 & ~1"
                /*Bitu*/int cursorMemStart = ((sourceStartBit >> 2)& ~1) + (((int)VGA.vga.s3.hgc.startaddr) << 10);
                /*Bitu*/int cursorStartBit = sourceStartBit & 0x7;
                // stay at the right position in the pattern
                if ((cursorMemStart & 0x2)!=0) cursorMemStart--;
                /*Bitu*/int cursorMemEnd = cursorMemStart + ((64-VGA.vga.s3.hgc.posx) >> 2);
                Ptr xat = new Ptr(TempLine,VGA.vga.s3.hgc.originx); // mouse data start pos. in scanline
                for (/*Bitu*/int m = cursorMemStart; m < cursorMemEnd;) {
                    // for each byte of cursor data
                    /*Bit8u*/int bitsA = VGA.vga.mem.linear.get(m);
                    /*Bit8u*/int bitsB = VGA.vga.mem.linear.get(m+2);
                    for (/*Bit8u*/int bit=(0x80 >> cursorStartBit); bit != 0; bit >>= 1) {
                        // for each bit
                        cursorStartBit=0; // only the first byte has some bits cut off
                        if ((bitsA & bit)!=0) {
                            if ((bitsB & bit)!=0) xat.setInc(xat.get() ^ 0xFF); // Invert screen data
                            //else Transparent
                        } else if ((bitsB & bit)!=0) {
                            xat.set(VGA.vga.s3.hgc.forestack.get()); // foreground color
                        } else {
                            xat.set(VGA.vga.s3.hgc.backstack.get());
                        }
                        xat.inc();
                    }
                    if ((m&1)!=0)
                        m+=3;
                    else
                        m++;
                }
                return TempLine;
            }
        }
    };

    private static VGA_Line_Handler VGA_Draw_LIN16_Line_HWMouse = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            if (VGA.svga.hardware_cursor_active==null || !VGA.svga.hardware_cursor_active.call())
                return new Ptr(VGA.vga.mem.linear,vidstart);

            /*Bitu*/int lineat = ((vidstart-(VGA.vga.config.real_start<<2)) >> 1) / VGA.vga.draw.width;
            if ((VGA.vga.s3.hgc.posx >= VGA.vga.draw.width) ||
                (lineat < VGA.vga.s3.hgc.originy) ||
                (lineat > (VGA.vga.s3.hgc.originy + (63-VGA.vga.s3.hgc.posy))) ) {
                return new Ptr(VGA.vga.mem.linear, vidstart);
            } else {
                Ptr.memcpy(TempLine, new Ptr(VGA.vga.mem.linear, vidstart), VGA.vga.draw.width*2);
                /*Bitu*/int sourceStartBit = ((lineat - VGA.vga.s3.hgc.originy) + VGA.vga.s3.hgc.posy)*64 + VGA.vga.s3.hgc.posx;
                /*Bitu*/int cursorMemStart = ((sourceStartBit >> 2)& ~1) + (((int)VGA.vga.s3.hgc.startaddr) << 10);
                /*Bitu*/int cursorStartBit = sourceStartBit & 0x7;
                if ((cursorMemStart & 0x2)!=0) cursorMemStart--;
                /*Bitu*/int cursorMemEnd = cursorMemStart + ((64-VGA.vga.s3.hgc.posx) >> 2);
                ShortPtr xat = new ShortPtr(TempLine,VGA.vga.s3.hgc.originx);
                for (/*Bitu*/int m = cursorMemStart; m < cursorMemEnd;) {
                    // for each byte of cursor data
                    /*Bit8u*/int bitsA = VGA.vga.mem.linear.get(m);
                    /*Bit8u*/int bitsB = VGA.vga.mem.linear.get(m+2);
                    for (/*Bit8u*/int bit=(0x80 >> cursorStartBit); bit != 0; bit >>= 1) {
                        // for each bit
                        cursorStartBit=0;
                        if ((bitsA & bit)!=0) {
                            // byte order doesn't matter here as all bits get flipped
                            if ((bitsB & bit)!=0) xat.set(xat.get() ^ ~0);
                            //else Transparent
                        } else if ((bitsB & bit)!=0) {
                            // Source as well as destination are Bit8u arrays,
                            // so this should work out endian-wise?
                            xat.set(VGA.vga.s3.hgc.forestack.readw(0));
                        } else {
                            xat.set(VGA.vga.s3.hgc.backstack.readw(0));
                        }
                        xat.inc();
                    }
                    if ((m&1)!=0)
                        m+=3;
                    else
                        m++;
                }
                return TempLine;
            }
        }
    };

    private static VGA_Line_Handler VGA_Draw_LIN32_Line_HWMouse = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            if (VGA.svga.hardware_cursor_active==null || !VGA.svga.hardware_cursor_active.call())
                return new Ptr(VGA.vga.mem.linear,vidstart);

            /*Bitu*/int lineat = ((vidstart-(VGA.vga.config.real_start<<2)) >> 2) / VGA.vga.draw.width;
            if ((VGA.vga.s3.hgc.posx >= VGA.vga.draw.width) ||
                (lineat < VGA.vga.s3.hgc.originy) ||
                (lineat > (VGA.vga.s3.hgc.originy + (63-VGA.vga.s3.hgc.posy))) ) {
                return new Ptr(VGA.vga.mem.linear,vidstart);
            } else {
                Ptr.memcpy(TempLine, new Ptr(VGA.vga.mem.linear, vidstart), VGA.vga.draw.width*4);
                /*Bitu*/int sourceStartBit = ((lineat - VGA.vga.s3.hgc.originy) + VGA.vga.s3.hgc.posy)*64 + VGA.vga.s3.hgc.posx;
                /*Bitu*/int cursorMemStart = ((sourceStartBit >> 2)& ~1) + (((int)VGA.vga.s3.hgc.startaddr) << 10);
                /*Bitu*/int cursorStartBit = sourceStartBit & 0x7;
                if ((cursorMemStart & 0x2)!=0) cursorMemStart--;
                /*Bitu*/int cursorMemEnd = cursorMemStart + ((64-VGA.vga.s3.hgc.posx) >> 2);
                IntPtr xat = new IntPtr(TempLine,VGA.vga.s3.hgc.originx);
                for (/*Bitu*/int m = cursorMemStart; m < cursorMemEnd;) {
                    // for each byte of cursor data
                    /*Bit8u*/int bitsA = VGA.vga.mem.linear.get(m);
                    /*Bit8u*/int bitsB = VGA.vga.mem.linear.get(m+2);
                    for (/*Bit8u*/int bit=(0x80 >> cursorStartBit); bit != 0; bit >>= 1) { // for each bit
                        cursorStartBit=0;
                        if ((bitsA & bit)!=0) {
                            if ((bitsB & bit)!=0) xat.set(xat.get() ^ ~0);
                            //else Transparent
                        } else if ((bitsB & bit)!=0) {
                            xat.set((int)new Ptr(VGA.vga.s3.hgc.forestack).readd(0));
                        } else {
                            xat.set((int)new Ptr(VGA.vga.s3.hgc.backstack).readd(0));
                        }
                        xat.inc();
                    }
                    if ((m&1)!=0)
                        m+=3;
                    else
                        m++;
                }
                return TempLine;
            }
        }
    };

    private static Ptr VGA_Text_Memwrap(/*Bitu*/int vidstart) {
        vidstart &= VGA.vga.draw.linear_mask;
        /*Bitu*/int line_end = 2 * VGA.vga.draw.blocks;
        if ((vidstart + line_end) > VGA.vga.draw.linear_mask) {
            // wrapping in this line
            /*Bitu*/int break_pos = (VGA.vga.draw.linear_mask - vidstart) + 1;
            // need a temporary storage - TempLine/2 is ok for a bit more than 132 columns
            Ptr t = new Ptr(TempLine, TempLine.size()/2);
            Ptr.memcpy(t, new Ptr(VGA.vga.tandy.draw_base, vidstart), break_pos);
            Ptr.memcpy(new Ptr(t, break_pos), new Ptr(VGA.vga.tandy.draw_base), line_end - break_pos);
            return t;
        } else return new Ptr(VGA.vga.tandy.draw_base, vidstart);
    }

    private static /*Bit32u*/int[] FontMask={0xffffffff,0x0};
    private static VGA_Line_Handler VGA_TEXT_Draw_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            /*Bits*/int font_addr;
            IntPtr draw=new IntPtr(TempLine);
            Ptr vidmem = VGA_Text_Memwrap(vidstart);
            for (/*Bitu*/int cx=0;cx<VGA.vga.draw.blocks;cx++) {
                /*Bitu*/int chr=vidmem.get(cx*2);
                /*Bitu*/int col=vidmem.get(cx*2+1);
                /*Bitu*/int font=VGA.vga.draw.font_tables[(col >> 3)&1].get(chr*32+line);
                /*Bit32u*/int mask1=VGA.TXT_Font_Table[font>>4] & FontMask[col >> 7];
                /*Bit32u*/int mask2=VGA.TXT_Font_Table[font&0xf] & FontMask[col >> 7];
                /*Bit32u*/int fg=VGA.TXT_FG_Table[col&0xf];
                /*Bit32u*/int bg=VGA.TXT_BG_Table[col>>4];
                draw.setInc((fg&mask1) | (bg&~mask1));
                draw.setInc((fg&mask2) | (bg&~mask2));
            }
            if (VGA.vga.draw.cursor.enabled && (VGA.vga.draw.cursor.count&0x8)!=0) {
                font_addr = (VGA.vga.draw.cursor.address-vidstart) >> 1;
                if (font_addr>=0 && font_addr<(/*Bits*/int)VGA.vga.draw.blocks) {
                    if (line>=VGA.vga.draw.cursor.sline && line<=VGA.vga.draw.cursor.eline) {
                        draw=new IntPtr(TempLine, font_addr*8);
                        /*Bit32u*/int att=VGA.TXT_FG_Table[VGA.vga.tandy.draw_base.get(VGA.vga.draw.cursor.address+1)&0xf];
                        draw.setInc(att);draw.setInc(att);
                    }
                }
            }
        //skip_cursor:
            return TempLine;
        }
    };

    private static VGA_Line_Handler VGA_TEXT_Herc_Draw_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            /*Bits*/int font_addr;
            IntPtr draw=new IntPtr(TempLine);
            Ptr vidmem = VGA_Text_Memwrap(vidstart);

            for (/*Bitu*/int cx=0;cx<VGA.vga.draw.blocks;cx++) {
                /*Bitu*/int chr=vidmem.get(cx*2);
                /*Bitu*/int attrib=vidmem.get(cx*2+1);
                if ((attrib&0x77)==0) {
                    // 00h, 80h, 08h, 88h produce black space
                    draw.setInc(0);
                    draw.setInc(0);
                } else {
                    /*Bit32u*/int bg, fg;
                    boolean underline=false;
                    if ((attrib&0x77)==0x70) {
                        bg = VGA.TXT_BG_Table[0x7];
                        if ((attrib&0x8)!=0) fg = VGA.TXT_FG_Table[0xf];
                        else fg = VGA.TXT_FG_Table[0x0];
                    } else {
                        if (((/*Bitu*/int)(VGA.vga.crtc.underline_location&0x1f)==line) && ((attrib&0x77)==0x1)) underline=true;
                        bg = VGA.TXT_BG_Table[0x0];
                        if ((attrib&0x8)!=0) fg = VGA.TXT_FG_Table[0xf];
                        else fg = VGA.TXT_FG_Table[0x7];
                    }
                    /*Bit32u*/int mask1, mask2;
                    if (underline) mask1 = mask2 = FontMask[attrib >> 7];
                    else {
                        /*Bitu*/int font=VGA.vga.draw.font_tables[0].get(chr*32+line);
                        mask1=VGA.TXT_Font_Table[font>>4] & FontMask[attrib >> 7]; // blinking
                        mask2=VGA.TXT_Font_Table[font&0xf] & FontMask[attrib >> 7];
                    }
                    draw.setInc((fg&mask1) | (bg&~mask1));
                    draw.setInc((fg&mask2) | (bg&~mask2));
                }
            }
            if (VGA.vga.draw.cursor.enabled && (VGA.vga.draw.cursor.count&0x8)==0) {
                font_addr = (VGA.vga.draw.cursor.address-vidstart) >> 1;
                if (font_addr>=0 && font_addr<(/*Bits*/int)VGA.vga.draw.blocks) {
                    if (line>=VGA.vga.draw.cursor.sline && line<=VGA.vga.draw.cursor.eline) {
                        draw=new IntPtr(TempLine, font_addr*8);
                        /*Bit8u*/int attr = VGA.vga.tandy.draw_base.get(VGA.vga.draw.cursor.address+1);
                        /*Bit32u*/int cg;
                        if ((attr&0x8)!=0) {
                            cg = VGA.TXT_FG_Table[0xf];
                        } else if ((attr&0x77)==0x70) {
                            cg = VGA.TXT_FG_Table[0x0];
                        } else {
                            cg = VGA.TXT_FG_Table[0x7];
                        }
                        draw.setInc(cg);draw.setInc(cg);
                    }
                }
            }
        //skip_cursor:
            return TempLine;
        }
    };

    private static VGA_Line_Handler VGA_TEXT_Xlat16_Draw_Line = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            /*Bits*/int font_addr;
            ShortPtr draw=new ShortPtr(TempLine);
            Ptr vidmem = VGA_Text_Memwrap(vidstart);
            for (/*Bitu*/int cx=0;cx<VGA.vga.draw.blocks;cx++) {
                /*Bitu*/int chr=vidmem.get(cx*2);
                /*Bitu*/int col=vidmem.get(cx*2+1);
                /*Bitu*/int font=VGA.vga.draw.font_tables[(col >> 3)&1].get(chr*32+line);
                /*Bit32u*/long mask1=VGA.TXT_Font_Table[font>>4] & FontMask[col >> 7];
                /*Bit32u*/long mask2=VGA.TXT_Font_Table[font&0xf] & FontMask[col >> 7];
                /*Bit32u*/long fg=VGA.TXT_FG_Table[col&0xf];
                /*Bit32u*/long bg=VGA.TXT_BG_Table[col>>4];

                mask1=(fg&mask1) | (bg&~mask1);
                mask2=(fg&mask2) | (bg&~mask2);

                for(int i = 0; i < 4; i++) {
                    draw.setInc(VGA.vga.dac.xlat16[(int)(mask1>>8*i)&0xff]);
                }
                for(int i = 0; i < 4; i++) {
                    draw.setInc(VGA.vga.dac.xlat16[(int)(mask2>>8*i)&0xff]);
                }
            }
            if (VGA.vga.draw.cursor.enabled && (VGA.vga.draw.cursor.count&0x8)!=0) {
                font_addr = (VGA.vga.draw.cursor.address-vidstart) >> 1;
                if (font_addr>=0 && font_addr<(/*Bits*/int)VGA.vga.draw.blocks) {
                    if (line>=VGA.vga.draw.cursor.sline && line<=VGA.vga.draw.cursor.eline) {
                        draw=new ShortPtr(TempLine, font_addr*16);
                        /*Bit8u*/int att=(int)(VGA.TXT_FG_Table[VGA.vga.tandy.draw_base.get(VGA.vga.draw.cursor.address+1)&0xf]&0xff);
                        for(int i = 0; i < 8; i++) {
                            draw.setInc(VGA.vga.dac.xlat16[att]);
                        }
                    }
                }
            }
        //skip_cursor:
            return TempLine;
        }
    };

    /*
    static Bit8u * VGA_TEXT_Draw_Line_9(Bitu vidstart, Bitu line) {
        Bits font_addr;
        Bit8u * draw=(Bit8u *)TempLine;
        boolean underline=(Bitu)(VGA.vga.crtc.underline_location&0x1f)==line;
        Bit8u pel_pan=(Bit8u)VGA.vga.draw.panning;
        if ((VGA.vga.attr.mode_control&0x20) && (VGA.vga.draw.lines_done>=VGA.vga.draw.split_line)) pel_pan=0;
        const Bit8u* vidmem = VGA_Text_Memwrap(vidstart);
        Bit8u chr=vidmem[0];
        Bit8u col=vidmem[1];
        Bit8u font=(VGA.vga.draw.font_tables[(col >> 3)&1][chr*32+line])<<pel_pan;
        if (underline && ((col&0x07) == 0x01)) font=0xff;
        Bit8u fg=col&0xf;
        Bit8u bg=(Bit8u)(VGA.TXT_BG_Table[col>>4]&0xff);
        Bitu draw_blocks=VGA.vga.draw.blocks;
        draw_blocks++;
        for (Bitu cx=1;cx<draw_blocks;cx++) {
            if (pel_pan) {
                chr=vidmem[cx*2];
                col=vidmem[cx*2+1];
                if (underline && ((col&0x07) == 0x01)) font|=0xff>>(8-pel_pan);
                else font|=VGA.vga.draw.font_tables[(col >> 3)&1][chr*32+line]>>(8-pel_pan);
                fg=col&0xf;
                bg=(Bit8u)(VGA.TXT_BG_Table[col>>4]&0xff);
            } else {
                chr=vidmem[(cx-1)*2];
                col=vidmem[(cx-1)*2+1];
                if (underline && ((col&0x07) == 0x01)) font=0xff;
                else font=VGA.vga.draw.font_tables[(col >> 3)&1][chr*32+line];
                fg=col&0xf;
                bg=(Bit8u)(VGA.TXT_BG_Table[col>>4]&0xff);
            }
            if (FontMask[col>>7]==0) font=0;
            *draw++=(font&0x80)?fg:bg;		*draw++=(font&0x40)?fg:bg;
            *draw++=(font&0x20)?fg:bg;		*draw++=(font&0x10)?fg:bg;
            *draw++=(font&0x08)?fg:bg;		*draw++=(font&0x04)?fg:bg;
            *draw++=(font&0x02)?fg:bg;
            Bit8u last=(font&0x01)?fg:bg;
            *draw++=last;
            *draw++=((VGA.vga.attr.mode_control&0x04) && ((chr<0xc0) || (chr>0xdf))) ? bg : last;
            if (pel_pan) {
                if (underline && ((col&0x07) == 0x01)) font=0xff;
                else font=(VGA.vga.draw.font_tables[(col >> 3)&1][chr*32+line])<<pel_pan;
            }
        }
        if (!VGA.vga.draw.cursor.enabled || !(VGA.vga.draw.cursor.count&0x8)) goto skip_cursor;
        font_addr = (VGA.vga.draw.cursor.address-vidstart) >> 1;
        if (font_addr>=0 && font_addr<(Bits)VGA.vga.draw.blocks) {
            if (line<VGA.vga.draw.cursor.sline) goto skip_cursor;
            if (line>VGA.vga.draw.cursor.eline) goto skip_cursor;
            draw=&TempLine[font_addr*9];
            Bit8u fg=VGA.vga.tandy.draw_base[VGA.vga.draw.cursor.address+1]&0xf;
            *draw++=fg;		*draw++=fg;		*draw++=fg;		*draw++=fg;
            *draw++=fg;		*draw++=fg;		*draw++=fg;		*draw++=fg;
        }
    skip_cursor:
        return TempLine;
    }
    */

    private static VGA_Line_Handler VGA_TEXT_Xlat16_Draw_Line_9 = new VGA_Line_Handler() {
        public Ptr call(/*Bitu*/int vidstart, /*Bitu*/int line) {
            /*Bits*/int font_addr;
            ShortPtr draw=new ShortPtr(TempLine);
            boolean underline=(/*Bitu*/int)(VGA.vga.crtc.underline_location&0x1f)==line;
            /*Bit8u*/int pel_pan=VGA.vga.draw.panning;
            if ((VGA.vga.attr.mode_control&0x20)!=0 && (VGA.vga.draw.lines_done>=VGA.vga.draw.split_line)) pel_pan=0;
            Ptr vidmem = VGA_Text_Memwrap(vidstart);
            /*Bit8u*/int chr=vidmem.get(0);
            /*Bit8u*/int col=vidmem.get(1);
            /*Bit8u*/int font=(VGA.vga.draw.font_tables[(col >> 3)&1].get(chr*32+line))<<pel_pan;
            if (underline && ((col&0x07) == 0x01)) font=0xff;
            /*Bit8u*/int fg=col&0xf;
            /*Bit8u*/int bg=(int)(VGA.TXT_BG_Table[col>>4]&0xff);
            /*Bitu*/int draw_blocks=VGA.vga.draw.blocks;
            draw_blocks++;
            for (/*Bitu*/int cx=1;cx<draw_blocks;cx++) {
                if (pel_pan!=0) {
                    chr=vidmem.get(cx*2);
                    col=vidmem.get(cx*2+1);
                    if (underline && ((col&0x07) == 0x01)) font|=0xff>>(8-pel_pan);
                    else font|=VGA.vga.draw.font_tables[(col >> 3)&1].get(chr*32+line)>>(8-pel_pan);
                    fg=col&0xf;
                    bg=(int)(VGA.TXT_BG_Table[col>>4]&0xff);
                } else {
                    chr=vidmem.get((cx-1)*2);
                    col=vidmem.get((cx-1)*2+1);
                    if (underline && ((col&0x07) == 0x01)) font=0xff;
                    else font=VGA.vga.draw.font_tables[(col >> 3)&1].get(chr*32+line);
                    fg=col&0xf;
                    bg=(int)(VGA.TXT_BG_Table[col>>4]&0xff);
                }
                if (FontMask[col>>7]==0) font=0;
                /*Bit8u*/int mask=0x80;
                for (int i = 0; i < 7; i++) {
                    draw.setInc(VGA.vga.dac.xlat16[(font & mask)!=0?fg:bg]);
                    mask>>=1;
                }
                /*Bit16u*/int lastval=VGA.vga.dac.xlat16[(font & mask)!=0?fg:bg];
                draw.setInc(lastval);
                draw.setInc((((VGA.vga.attr.mode_control&0x04)!=0 && ((chr<0xc0) || (chr>0xdf))) &&
                    !(underline && ((col&0x07) == 0x01))) ?
                    (VGA.vga.dac.xlat16[bg]) : lastval);
                if (pel_pan!=0) {
                    if (underline && ((col&0x07) == 0x01)) font=0xff;
                    else font=(VGA.vga.draw.font_tables[(col >> 3)&1].get(chr*32+line))<<pel_pan;
                }
            }
            if (VGA.vga.draw.cursor.enabled && (VGA.vga.draw.cursor.count&0x8)!=0) {
                font_addr = (VGA.vga.draw.cursor.address-vidstart) >> 1;
                if (font_addr>=0 && font_addr<(/*Bits*/int)VGA.vga.draw.blocks) {
                    if (line >= VGA.vga.draw.cursor.sline && line <= VGA.vga.draw.cursor.eline) {
                        draw=new ShortPtr(TempLine, font_addr*18);
                        /*Bit8u*/fg=VGA.vga.tandy.draw_base.get(VGA.vga.draw.cursor.address+1)&0xf;
                        for(int i = 0; i < 8; i++) {
                            draw.setInc(VGA.vga.dac.xlat16[fg]);
                        }
                        //if(underline && ((col&0x07) == 0x01))
                        //	*draw = VGA.vga.dac.xlat16[fg];
                    }
                }
            }
        //skip_cursor:
            return TempLine;
        }
    };

    private static void VGA_ChangesEnd() {
        if ( VGA.vga.changes.active ) {
    //		VGA.vga.changes.active = false;
            /*Bitu*/long end = VGA.vga.draw.address >> VGA.VGA_CHANGE_SHIFT;
            /*Bitu*/long total = 4 + end - VGA.vga.changes.start;
            /*Bit32u*/int clearMask = VGA.vga.changes.clearMask;
            total >>= 2;
            IntPtr clear = new IntPtr(VGA.vga.changes.map, (int)(VGA.vga.changes.start & ~3 ));
            while ( total-- !=0 ) {
                clear.setInc(clear.get() & clearMask);
            }
        }
    }


    private static void VGA_ProcessSplit() {
        // On the EGA the address is always reset to 0.
        if ((VGA.vga.attr.mode_control&0x20)!=0 || (Dosbox.machine== MachineType.MCH_EGA)) {
            VGA.vga.draw.address=0;
        } else {
            // In text mode only the characters are shifted by panning, not the address;
            // this is done in the text line draw function.
            VGA.vga.draw.address = VGA.vga.draw.byte_panning_shift*VGA.vga.draw.bytes_skip;
            if (!(VGA.vga.mode==VGA.M_TEXT)) VGA.vga.draw.address += VGA.vga.draw.panning;
        }
        VGA.vga.draw.address_line=0;
    }

    private static Pic.PIC_EventHandler VGA_DrawSingleLine = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            if (VGA.vga.attr.disabled!=0) {
                // draw blanked line (DoWhackaDo, Alien Carnage, TV sports Football)
                TempLine.clear();
                System.arraycopy(TempLine.p, 0, Render.render.src.outWrite, Render.render.src.outWriteOff, Render.render.src.outPitch);
            } else {
                Ptr data=VGA_DrawLine.call( VGA.vga.draw.address, VGA.vga.draw.address_line );
                System.arraycopy(data.p, data.off, Render.render.src.outWrite, Render.render.src.outWriteOff, Render.render.src.outPitch);
            }
            Render.render.src.outWriteOff+=Render.render.src.outPitch;
            VGA.vga.draw.address_line++;
            if (VGA.vga.draw.address_line>=VGA.vga.draw.address_line_total) {
                VGA.vga.draw.address_line=0;
                VGA.vga.draw.address+=VGA.vga.draw.address_add;
            }
            VGA.vga.draw.lines_done++;
            if (VGA.vga.draw.split_line==VGA.vga.draw.lines_done) VGA_ProcessSplit();
            if (VGA.vga.draw.lines_done < VGA.vga.draw.lines_total) {
                Pic.PIC_AddEvent(VGA_DrawSingleLine,(float)VGA.vga.draw.delay.htotal);
            } else Render.RENDER_EndUpdate(false);
        }
        public String toString() {
            return "VGA_DrawSingleLine";
        }
    };

    private static Pic.PIC_EventHandler VGA_DrawPart = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            while (val--!=0) {
                Ptr data=VGA_DrawLine.call( VGA.vga.draw.address, VGA.vga.draw.address_line );
                System.arraycopy(data.p, data.off, Render.render.src.outWrite, Render.render.src.outWriteOff, Render.render.src.outPitch);
                Render.render.src.outWriteOff+=Render.render.src.outPitch;
                VGA.vga.draw.address_line++;
                if (VGA.vga.draw.address_line>=VGA.vga.draw.address_line_total) {
                    VGA.vga.draw.address_line=0;
                    VGA.vga.draw.address+=VGA.vga.draw.address_add;
                }
                VGA.vga.draw.lines_done++;
                if (VGA.vga.draw.split_line==VGA.vga.draw.lines_done) {
                    if (VGA_memory.VGA_KEEP_CHANGES)
                        VGA_ChangesEnd( );
                    VGA_ProcessSplit();
                    if (VGA_memory.VGA_KEEP_CHANGES)
                        VGA.vga.changes.start = VGA.vga.draw.address >> VGA.VGA_CHANGE_SHIFT;
                }
            }
            if (--VGA.vga.draw.parts_left !=0) {
                Pic.PIC_AddEvent(VGA_DrawPart,(float)VGA.vga.draw.delay.parts,
                     (VGA.vga.draw.parts_left!=1) ? VGA.vga.draw.parts_lines  : (VGA.vga.draw.lines_total - VGA.vga.draw.lines_done));
            } else {
                if (VGA_memory.VGA_KEEP_CHANGES)
                    VGA_ChangesEnd();
                Render.RENDER_EndUpdate(false);
            }
        }
        public String toString() {
            return "VGA_DrawPart";
        }
    };

    public static void VGA_SetBlinking(/*Bitu*/int enabled) {
        /*Bitu*/int b;
        if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA, LogSeverities.LOG_NORMAL,"Blinking "+enabled);
        if (enabled!=0) {
            b=0;VGA.vga.draw.blinking=true; //used to -1 but blinking is unsigned
            VGA.vga.attr.mode_control|=0x08;
            VGA.vga.tandy.mode_control|=0x20;
        } else {
            b=8;VGA.vga.draw.blinking=false;
            VGA.vga.attr.mode_control&=~0x08;
            VGA.vga.tandy.mode_control&=~0x20;
        }
        for (/*Bitu*/int i=0;i<8;i++) VGA.TXT_BG_Table[i+8]=(b+i) | ((b+i) << 8)| ((b+i) <<16) | ((b+i) << 24);
    }

    //#ifdef VGA_KEEP_CHANGES
    private static void VGA_ChangesStart() {
        VGA.vga.changes.start = VGA.vga.draw.address >> VGA.VGA_CHANGE_SHIFT;
        VGA.vga.changes.last = VGA.vga.changes.start;
        if ( VGA.vga.changes.lastAddress != VGA.vga.draw.address ) {
    //		LOG_MSG("Address");
            VGA_DrawLine = VGA_Draw_Linear_Line;
            VGA.vga.changes.lastAddress = VGA.vga.draw.address;
        } else if ( Render.render.fullFrame ) {
    //		LOG_MSG("Full Frame");
            VGA_DrawLine = VGA_Draw_Linear_Line;
        } else {
    //		LOG_MSG("Changes");
            VGA_DrawLine = VGA_Draw_Changes_Line;
        }
        VGA.vga.changes.active = true;
        VGA.vga.changes.checkMask = VGA.vga.changes.writeMask;
        VGA.vga.changes.clearMask = ~( 0x01010101 << (VGA.vga.changes.frame & 7));
        VGA.vga.changes.frame++;
        VGA.vga.changes.writeMask = (short)(1 << (VGA.vga.changes.frame & 7));
    }
    //#endif

    private static Pic.PIC_EventHandler VGA_VertInterrupt = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            if ((!VGA.vga.draw.vret_triggered) && ((VGA.vga.crtc.vertical_retrace_end&0x30)==0x10)) {
                VGA.vga.draw.vret_triggered=true;
                if (Dosbox.machine==MachineType.MCH_EGA) Pic.PIC_ActivateIRQ(9);
            }
        }
        public String toString() {
            return "VGA_VertInterrupt";
        }
    };

    private static Pic.PIC_EventHandler VGA_Other_VertInterrupt = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            if (val!=0) Pic.PIC_ActivateIRQ(5);
            else Pic.PIC_DeActivateIRQ(5);
        }
        public String toString() {
            return "VGA_Other_VertInterrupt";
        }
    };

    private static Pic.PIC_EventHandler VGA_DisplayStartLatch = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            VGA.vga.config.real_start=VGA.vga.config.display_start & (VGA.vga.vmemwrap-1);
            VGA.vga.draw.bytes_skip = VGA.vga.config.bytes_skip;
        }
        public String toString() {
            return "VGA_DisplayStartLatch";
        }
    };

    private static Pic.PIC_EventHandler VGA_PanningLatch = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            VGA.vga.draw.panning = VGA.vga.config.pel_panning;
        }
        public String toString() {
            return "VGA_PanningLatch";
        }
    };

    static private Pic.PIC_EventHandler VGA_VerticalTimer = new Pic.PIC_EventHandler() {
        public String toString() {
            return "VGA_VerticalTimer";
        }
        public void call(/*Bitu*/int val) {
            VGA.vga.draw.delay.framestart = Pic.PIC_FullIndex();
            Pic.PIC_AddEvent( VGA_VerticalTimer, (float)VGA.vga.draw.delay.vtotal );

            switch(Dosbox.machine) {
            case MachineType.MCH_PCJR:
            case MachineType.MCH_TANDY:
                // PCJr: Vsync is directly connected to the IRQ controller
                // Some earlier Tandy models are said to have a vsync interrupt too
                Pic.PIC_AddEvent(VGA_Other_VertInterrupt, (float)VGA.vga.draw.delay.vrstart, 1);
                Pic.PIC_AddEvent(VGA_Other_VertInterrupt, (float)VGA.vga.draw.delay.vrend, 0);
                // fall-through
            case MachineType.MCH_CGA:
            case MachineType.MCH_HERC:
                // MC6845-powered graphics: Loading the display start latch happens somewhere
                // after vsync off and before first visible scanline, so probably here
                VGA_DisplayStartLatch.call(0);
                break;
            case MachineType.MCH_VGA:
            case MachineType.MCH_EGA:
                Pic.PIC_AddEvent(VGA_DisplayStartLatch, (float)VGA.vga.draw.delay.vrstart);
                Pic.PIC_AddEvent(VGA_PanningLatch, (float)VGA.vga.draw.delay.vrend);
                // EGA: 82c435 datasheet: interrupt happens at display end
                // VGA: checked with scope; however disabled by default by jumper on VGA boards
                // add a little amount of time to make sure the last drawpart has already fired
                Pic.PIC_AddEvent(VGA_VertInterrupt,(float)(VGA.vga.draw.delay.vdend + 0.005));
                break;
            default:
                Log.exit("This new machine needs implementation in VGA_VerticalTimer too.");
                break;
            }
            //Check if we can actually render, else skip the rest (frameskip)
            if (!Render.RENDER_StartUpdate())
                return;

            VGA.vga.draw.address_line = VGA.vga.config.hlines_skip;
            if (Dosbox.IS_EGAVGA_ARCH()) {
                VGA.vga.draw.split_line = (/*Bitu*/int)((VGA.vga.config.line_compare+1)/VGA.vga.draw.lines_scaled);
                if ((Dosbox.svgaCard== SVGACards.SVGA_S3Trio) && (VGA.vga.config.line_compare==0)) VGA.vga.draw.split_line=0;
                VGA.vga.draw.split_line -= VGA.vga.draw.vblank_skip;
            } else {
                VGA.vga.draw.split_line = 0x10000;	// don't care
            }
            VGA.vga.draw.address = VGA.vga.config.real_start;
            VGA.vga.draw.byte_panning_shift = 0;
            // go figure...
            if (Dosbox.machine==MachineType.MCH_EGA) VGA.vga.draw.split_line*=2;
        //	if (Dosbox.machine==MachineType.MCH_EGA) VGA.vga.draw.split_line = ((((VGA.vga.config.line_compare&0x5ff)+1)*2-1)/VGA.vga.draw.lines_scaled);
        //#ifdef VGA_KEEP_CHANGES
            boolean startaddr_changed=false;
        //#endif
            switch (VGA.vga.mode) {
            case VGA.M_EGA:
                if ((VGA.vga.crtc.mode_control&0x1)==0) VGA.vga.draw.linear_mask &= ~0x10000;
                else VGA.vga.draw.linear_mask |= 0x10000;
            case VGA.M_LIN4:
                VGA.vga.draw.byte_panning_shift = 8;
                VGA.vga.draw.address += VGA.vga.draw.bytes_skip;
                VGA.vga.draw.address *= VGA.vga.draw.byte_panning_shift;
                VGA.vga.draw.address += VGA.vga.draw.panning;
        //#ifdef VGA_KEEP_CHANGES
                startaddr_changed=true;
        //#endif
                break;
            case VGA.M_VGA:
                if(VGA.vga.config.compatible_chain4 && (VGA.vga.crtc.underline_location & 0x40)!=0) {
                    VGA.vga.draw.linear_base = VGA.vga.fastmem;
                    VGA.vga.draw.linear_mask = 0xffff;
                } else {
                    VGA.vga.draw.linear_base = VGA.vga.mem.linear;
                    VGA.vga.draw.linear_mask = VGA.vga.vmemwrap - 1;
                }
            case VGA.M_LIN8:
            case VGA.M_LIN15:
            case VGA.M_LIN16:
            case VGA.M_LIN32:
                VGA.vga.draw.byte_panning_shift = 4;
                VGA.vga.draw.address += VGA.vga.draw.bytes_skip;
                VGA.vga.draw.address *= VGA.vga.draw.byte_panning_shift;
                VGA.vga.draw.address += VGA.vga.draw.panning;
        //#ifdef VGA_KEEP_CHANGES
                startaddr_changed=true;
        //#endif
                break;
            case VGA.M_TEXT:
                VGA.vga.draw.byte_panning_shift = 2;
                VGA.vga.draw.address += VGA.vga.draw.bytes_skip;
                // fall-through
            case VGA.M_TANDY_TEXT:
            case VGA.M_HERC_TEXT:
                if (Dosbox.machine==MachineType.MCH_HERC) VGA.vga.draw.linear_mask = 0xfff; // 1 page
                else if (Dosbox.IS_EGAVGA_ARCH()) VGA.vga.draw.linear_mask = 0x7fff; // 8 pages
                else VGA.vga.draw.linear_mask = 0x3fff; // CGA, Tandy 4 pages
                VGA.vga.draw.cursor.address=VGA.vga.config.cursor_start*2;
                VGA.vga.draw.address *= 2;
                VGA.vga.draw.cursor.count++;
                /* check for blinking and blinking change delay */
                FontMask[1]=(VGA.vga.draw.blinking & (VGA.vga.draw.cursor.count >>> 4)!=0) ?
                    0 : 0xffffffff;
                break;
            case VGA.M_HERC_GFX:
                break;
            case VGA.M_CGA4:case VGA.M_CGA2:
                VGA.vga.draw.address=(VGA.vga.draw.address*2)&0x1fff;
                break;
            case VGA.M_CGA16:
            case VGA.M_TANDY2:case VGA.M_TANDY4:case VGA.M_TANDY16:
                VGA.vga.draw.address *= 2;
                break;
            default:
                break;
            }
            if (VGA.vga.draw.split_line==0) VGA_ProcessSplit();
            if (VGA_memory.VGA_KEEP_CHANGES)
                if (startaddr_changed) VGA_ChangesStart();

            // check if some lines at the top off the screen are blanked
            float draw_skip = 0.0f;
            if (VGA.vga.draw.vblank_skip!=0) {
                draw_skip = (float)(VGA.vga.draw.delay.htotal * VGA.vga.draw.vblank_skip);
                VGA.vga.draw.address += VGA.vga.draw.address_add * (VGA.vga.draw.vblank_skip/(VGA.vga.draw.address_line_total));
            }

            // add the draw event
            switch (VGA.vga.draw.mode) {
            case VGA.Drawmode.PART:
                if (VGA.vga.draw.parts_left!=0) {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL, "Parts left: "+VGA.vga.draw.parts_left );
                    Pic.PIC_RemoveEvents(VGA_DrawPart);
                    Render.RENDER_EndUpdate(true);
                }
                VGA.vga.draw.lines_done = 0;
                VGA.vga.draw.parts_left = VGA.vga.draw.parts_total;
                Pic.PIC_AddEvent(VGA_DrawPart,(float)VGA.vga.draw.delay.parts + draw_skip,VGA.vga.draw.parts_lines);
                break;
            case VGA.Drawmode.LINE:
                if (VGA.vga.draw.lines_done < VGA.vga.draw.lines_total) {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL, "Lines left: %d"+(VGA.vga.draw.lines_total-VGA.vga.draw.lines_done));
                    Pic.PIC_RemoveEvents(VGA_DrawSingleLine);
                    Render.RENDER_EndUpdate(true);
                }
                VGA.vga.draw.lines_done = 0;
                Pic.PIC_AddEvent(VGA_DrawSingleLine,(float)(VGA.vga.draw.delay.htotal/4.0 + draw_skip));
                break;
            //case EGALINE:
            }
        }
    };

    public static void VGA_CheckScanLength() {
        switch (VGA.vga.mode) {
        case VGA.M_EGA:
        case VGA.M_LIN4:
            VGA.vga.draw.address_add=VGA.vga.config.scan_len*16;
            break;
        case VGA.M_VGA:
        case VGA.M_LIN8:
        case VGA.M_LIN15:
        case VGA.M_LIN16:
        case VGA.M_LIN32:
            VGA.vga.draw.address_add=VGA.vga.config.scan_len*8;
            break;
        case VGA.M_TEXT:
            VGA.vga.draw.address_add=VGA.vga.config.scan_len*4;
            break;
        case VGA.M_CGA2:
        case VGA.M_CGA4:
        case VGA.M_CGA16:
            VGA.vga.draw.address_add=80;
            return;
        case VGA.M_TANDY2:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks/4;
            break;
        case VGA.M_TANDY4:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks;
            break;
        case VGA.M_TANDY16:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks;
            break;
        case VGA.M_TANDY_TEXT:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks*2;
            break;
        case VGA.M_HERC_TEXT:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks*2;
            break;
        case VGA.M_HERC_GFX:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks;
            break;
        default:
            VGA.vga.draw.address_add=VGA.vga.draw.blocks*8;
            break;
        }
    }

    public static void VGA_ActivateHardwareCursor() {
        boolean hwcursor_active=false;
        if (VGA.svga.hardware_cursor_active!=null) {
            if (VGA.svga.hardware_cursor_active.call()) hwcursor_active=true;
        }
        if (hwcursor_active) {
            switch(VGA.vga.mode) {
            case VGA.M_LIN32:
                VGA_DrawLine=VGA_Draw_LIN32_Line_HWMouse;
                break;
            case VGA.M_LIN15:
            case VGA.M_LIN16:
                VGA_DrawLine=VGA_Draw_LIN16_Line_HWMouse;
                break;
            default:
                VGA_DrawLine=VGA_Draw_VGA_Line_HWMouse;
            }
        } else {
            VGA_DrawLine=VGA_Draw_Linear_Line;
        }
    }

    public static Pic.PIC_EventHandler VGA_SetupDrawing = new Pic.PIC_EventHandler() {
        public String toString() {
            return "VGA_SetupDrawing";
        }
        public void call(/*Bitu*/int val) {
            if (VGA.vga.mode==VGA.M_ERROR) {
                Pic.PIC_RemoveEvents(VGA_VerticalTimer);
                Pic.PIC_RemoveEvents(VGA_PanningLatch);
                Pic.PIC_RemoveEvents(VGA_DisplayStartLatch);
                return;
            }
            // set the drawing mode
            switch (Dosbox.machine) {
            case MachineType.MCH_CGA:
            case MachineType.MCH_PCJR:
                VGA.vga.draw.mode = VGA.Drawmode.LINE;
                break;
            case MachineType.MCH_VGA:
                if (Dosbox.svgaCard==SVGACards.SVGA_None) {
                    VGA.vga.draw.mode = VGA.Drawmode.LINE;
                    break;
                }
                // fall-through
            default:
                VGA.vga.draw.mode = VGA.Drawmode.PART;
                break;
            }

            /* Calculate the FPS for this screen */
            double fps; /*Bitu*/int clock;
            /*Bitu*/int htotal, hdend, hbstart, hbend, hrstart, hrend;
            /*Bitu*/int vtotal, vdend, vbstart, vbend, vrstart, vrend;
            /*Bitu*/int vblank_skip;
            if (Dosbox.IS_EGAVGA_ARCH()) {
                htotal = VGA.vga.crtc.horizontal_total;
                hdend = VGA.vga.crtc.horizontal_display_end;
                hbend = VGA.vga.crtc.end_horizontal_blanking&0x1F;
                hbstart = VGA.vga.crtc.start_horizontal_blanking;
                hrstart = VGA.vga.crtc.start_horizontal_retrace;

                vtotal= VGA.vga.crtc.vertical_total | ((VGA.vga.crtc.overflow & 1) << 8);
                vdend = VGA.vga.crtc.vertical_display_end | ((VGA.vga.crtc.overflow & 2)<<7);
                vbstart = VGA.vga.crtc.start_vertical_blanking | ((VGA.vga.crtc.overflow & 0x08) << 5);
                vrstart = VGA.vga.crtc.vertical_retrace_start + ((VGA.vga.crtc.overflow & 0x04) << 6);

                if (Dosbox.IS_VGA_ARCH()) {
                    // additional bits only present on vga cards
                    htotal |= (VGA.vga.s3.ex_hor_overflow & 0x1) << 8;
                    htotal += 3;
                    hdend |= (VGA.vga.s3.ex_hor_overflow & 0x2) << 7;
                    hbend |= (VGA.vga.crtc.end_horizontal_retrace&0x80) >> 2;
                    hbstart |= (VGA.vga.s3.ex_hor_overflow & 0x4) << 6;
                    hrstart |= (VGA.vga.s3.ex_hor_overflow & 0x10) << 4;

                    vtotal |= (VGA.vga.crtc.overflow & 0x20) << 4;
                    vtotal |= (VGA.vga.s3.ex_ver_overflow & 0x1) << 10;
                    vdend |= (VGA.vga.crtc.overflow & 0x40) << 3;
                    vdend |= (VGA.vga.s3.ex_ver_overflow & 0x2) << 9;
                    vbstart |= (VGA.vga.crtc.maximum_scan_line & 0x20) << 4;
                    vbstart |= (VGA.vga.s3.ex_ver_overflow & 0x4) << 8;
                    vrstart |= ((VGA.vga.crtc.overflow & 0x80) << 2);
                    vrstart |= (VGA.vga.s3.ex_ver_overflow & 0x10) << 6;
                    vbend = VGA.vga.crtc.end_vertical_blanking & 0x7f;
                } else { // EGA
                    vbend = VGA.vga.crtc.end_vertical_blanking & 0x1f;
                }
                htotal += 2;
                vtotal += 2;
                hdend += 1;
                vdend += 1;
                vbstart += 1;

                hbend = hbstart + ((hbend - hbstart) & 0x3F);
                hrend = VGA.vga.crtc.end_horizontal_retrace & 0x1f;
                hrend = (hrend - hrstart) & 0x1f;

                if ( hrend==0 ) hrend = hrstart + 0x1f + 1;
                else hrend = hrstart + hrend;

                vrend = VGA.vga.crtc.vertical_retrace_end & 0xF;
                vrend = ( vrend - vrstart)&0xF;

                if ( vrend==0) vrend = vrstart + 0xf + 1;
                else vrend = vrstart + vrend;

                vbend = (vbend - vbstart) & 0x7f;
                if ( vbend==0) vbend = vbstart + 0x7f + 1;
                else vbend = vbstart + vbend;

                vbend++;

                if (VGA.svga.get_clock!=null) {
                    clock = VGA.svga.get_clock.call();
                } else {
                    switch ((VGA.vga.misc_output >> 2) & 3) {
                    case 0:
                        clock = (Dosbox.machine==MachineType.MCH_EGA) ? 14318180 : 25175000;
                        break;
                    case 1:
                    default:
                        clock = (Dosbox.machine==MachineType.MCH_EGA) ? 16257000 : 28322000;
                        break;
                    }
                }

                /* Check for 8 for 9 character clock mode */
                if ((VGA.vga.seq.clocking_mode & 1)!=0 ) clock/=8; else clock/=9;
                /* Check for pixel doubling, master clock/2 */
                if ((VGA.vga.seq.clocking_mode & 0x8)!=0) {
                    htotal*=2;
                }
                VGA.vga.draw.address_line_total=(VGA.vga.crtc.maximum_scan_line&0x1f)+1;
                if(Dosbox.IS_VGA_ARCH() && (Dosbox.svgaCard==SVGACards.SVGA_None) && (VGA.vga.mode==VGA.M_EGA || VGA.vga.mode==VGA.M_VGA)) {
                    // vgaonly; can't use with CGA because these use address_line for their
                    // own purposes.
                    // Set the low resolution modes to have as many lines as are scanned -
                    // Quite a few demos change the max_scanline register at display time
                    // to get SFX: Majic12 show, Magic circle, Copper, GBU, Party91
                    if((VGA.vga.crtc.maximum_scan_line&0x80)!=0) VGA.vga.draw.address_line_total*=2;
                    VGA.vga.draw.double_scan=false;
                }
                else if (Dosbox.IS_VGA_ARCH()) VGA.vga.draw.double_scan=(VGA.vga.crtc.maximum_scan_line&0x80)>0;
                else VGA.vga.draw.double_scan=(vtotal==262);
            } else {
                htotal = VGA.vga.other.htotal + 1;
                hdend = VGA.vga.other.hdend;
                hbstart = hdend;
                hbend = htotal;
                hrstart = VGA.vga.other.hsyncp;
                hrend = hrstart + VGA.vga.other.hsyncw;

                VGA.vga.draw.address_line_total = VGA.vga.other.max_scanline + 1;
                vtotal = VGA.vga.draw.address_line_total * (VGA.vga.other.vtotal+1)+VGA.vga.other.vadjust;
                vdend = VGA.vga.draw.address_line_total * VGA.vga.other.vdend;
                vrstart = VGA.vga.draw.address_line_total * VGA.vga.other.vsyncp;
                vrend = vrstart + 16; // vsync width is fixed to 16 lines on the MC6845 TODO Tandy
                vbstart = vdend;
                vbend = vtotal;
                VGA.vga.draw.double_scan=false;
                switch (Dosbox.machine) {
                case MachineType.MCH_CGA:
                case MachineType.MCH_TANDY:
                case MachineType.MCH_PCJR: //TANDY_ARCH_CASE:
                    clock=((VGA.vga.tandy.mode_control & 1)!=0 ? 14318180 : (14318180/2))/8;
                    break;
                case MachineType.MCH_HERC:
                    if ((VGA.vga.herc.mode_control & 0x2)!=0) clock=16000000/16;
                    else clock=16000000/8;
                    break;
                default:
                    clock = 14318180;
                    break;
                }
                VGA.vga.draw.delay.hdend = hdend*1000.0/clock; //in milliseconds
            }
            if (Config.C_DEBUG) {
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_NORMAL,"h total "+htotal+" end "+hdend+" blank ("+hbstart+"/"+hbend+") retrace ("+hrstart+"/"+hrend+")");
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_NORMAL,"v total "+vtotal+" end "+vdend+" blank ("+vbstart+"/"+vbend+") retrace ("+vrstart+"/"+vrend+")");
            }
            if (htotal==0) return;
            if (vtotal==0) return;

            // The screen refresh frequency
            fps=(double)clock/(vtotal*htotal);
            // Horizontal total (that's how long a line takes with whistles and bells)
            VGA.vga.draw.delay.htotal = htotal*1000.0/clock; //in milliseconds
            // Start and End of horizontal blanking
            VGA.vga.draw.delay.hblkstart = hbstart*1000.0/clock; //in milliseconds
            VGA.vga.draw.delay.hblkend = hbend*1000.0/clock;
            // Start and End of horizontal retrace
            VGA.vga.draw.delay.hrstart = hrstart*1000.0/clock;
            VGA.vga.draw.delay.hrend = hrend*1000.0/clock;
            // Start and End of vertical blanking
            VGA.vga.draw.delay.vblkstart = vbstart * VGA.vga.draw.delay.htotal;
            VGA.vga.draw.delay.vblkend = vbend * VGA.vga.draw.delay.htotal;
            // Start and End of vertical retrace pulse
            VGA.vga.draw.delay.vrstart = vrstart * VGA.vga.draw.delay.htotal;
            VGA.vga.draw.delay.vrend = vrend * VGA.vga.draw.delay.htotal;

            // Vertical blanking tricks
            vblank_skip = 0;
            if (Dosbox.IS_VGA_ARCH()) { // others need more investigation
                if (vbstart < vtotal) { // There will be no blanking at all otherwise
                    if (vbend > vtotal) {
                        // blanking wraps to the start of the screen
                        vblank_skip = vbend&0x7f;

                        // on blanking wrap to 0, the first line is not blanked
                        // this is used by the S3 BIOS and other S3 drivers in some SVGA modes
                        if((vbend&0x7f)==1) vblank_skip = 0;

                        // it might also cut some lines off the bottom
                        if(vbstart < vdend) {
                            vdend = vbstart;
                        }
                    if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_WARN,"Blanking wrap to line "+vblank_skip);
                    } else if (vbstart==1) {
                        // blanking is used to cut lines at the start of the screen
                        vblank_skip = vbend;
                    if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_WARN,"Upper "+vblank_skip+" lines of the screen blanked");
                    } else if (vbstart < vdend) {
                        if(vbend < vdend) {
                            // the game wants a black bar somewhere on the screen
                        if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_WARN,"Unsupported blanking: line "+vbstart+"-"+vbend);
                        } else {
                            // blanking is used to cut off some lines from the bottom
                            vdend = vbstart;
                        }
                    }
                    vdend -= vblank_skip;
                }
            }
            // Display end
            VGA.vga.draw.delay.vdend = vdend * VGA.vga.draw.delay.htotal;

            VGA.vga.draw.parts_total=VGA_PARTS;
            /*
              6  Horizontal Sync Polarity. Negative if set
              7  Vertical Sync Polarity. Negative if set
                 Bit 6-7 indicates the number of lines on the display:
                    1:  400, 2: 350, 3: 480
            */
            //Try to determine the pixel size, aspect correct is based around square pixels

            //Base pixel width around 100 clocks horizontal
            //For 9 pixel text modes this should be changed, but we don't support that anyway :)
            //Seems regular vga only listens to the 9 char pixel mode with character mode enabled
            double pwidth = (Dosbox.machine==MachineType.MCH_EGA) ? (114.0 / htotal) : (100.0 / htotal);
            //Base pixel height around vertical totals of modes that have 100 clocks horizontal
            //Different sync values gives different scaling of the whole vertical range
            //VGA monitor just seems to thighten or widen the whole vertical range
            double pheight;
            double target_total = (Dosbox.machine==MachineType.MCH_EGA) ? 262.0 : 449.0;
            /*Bitu*/int sync = VGA.vga.misc_output >> 6;
            switch ( sync ) {
            case 0:		// This is not defined in vga specs,
                        // Kiet, seems to be slightly less than 350 on my monitor
                //340 line mode, filled with 449 total
                pheight = (480.0 / 340.0) * ( target_total / vtotal );
                break;
            case 1:		//400 line mode, filled with 449 total
                pheight = (480.0 / 400.0) * ( target_total / vtotal );
                break;
            case 2:		//350 line mode, filled with 449 total
                //This mode seems to get regular 640x400 timing and goes for a loong retrace
                //Depends on the monitor to stretch the screen
                pheight = (480.0 / 350.0) * ( target_total / vtotal );
                break;
            case 3:		//480 line mode, filled with 525 total
            default:
                pheight = (480.0 / 480.0) * ( 525.0 / vtotal );
                break;
            }

            double aspect_ratio = pheight / pwidth;

            VGA.vga.draw.delay.parts = VGA.vga.draw.delay.vdend/VGA.vga.draw.parts_total;
            VGA.vga.draw.resizing=false;
            VGA.vga.draw.vret_triggered=false;

            //Check to prevent useless black areas
            if (hbstart<hdend) hdend=hbstart;
            if ((!Dosbox.IS_VGA_ARCH()) && (vbstart<vdend)) vdend=vbstart;


            /*Bitu*/int width=hdend;
            /*Bitu*/int height=vdend;
            boolean doubleheight=false;
            boolean doublewidth=false;

            //Set the bpp
            /*Bitu*/int bpp;
            switch (VGA.vga.mode) {
            case VGA.M_LIN15:
                bpp = 15;
                break;
            case VGA.M_LIN16:
                bpp = 16;
                break;
            case VGA.M_LIN32:
                bpp = 32;
                break;
            default:
                bpp = 8;
                break;
            }
            VGA.vga.draw.linear_base = VGA.vga.mem.linear;
            VGA.vga.draw.linear_mask = VGA.vga.vmemwrap - 1;
            switch (VGA.vga.mode) {
            case VGA.M_VGA:
                doublewidth=true;
                width<<=2;
                if ((Dosbox.IS_VGA_ARCH()) && (Dosbox.svgaCard==SVGACards.SVGA_None)) {
                    bpp=16;
                    VGA_DrawLine = VGA_Draw_Xlat16_Linear_Line;
                } else VGA_DrawLine = VGA_Draw_Linear_Line;
                break;
            case VGA.M_LIN8:
                if ((VGA.vga.crtc.mode_control & 0x8)!=0)
                    width >>=1;
                else if(Dosbox.svgaCard == SVGACards.SVGA_S3Trio && (VGA.vga.s3.reg_3a&0x10)==0) {
                    doublewidth=true;
                    width >>=1;
                }
                // fall-through
            case VGA.M_LIN32:
                width<<=3;
                if ((VGA.vga.crtc.mode_control & 0x8)!=0)
                    doublewidth = true;
                /* Use HW mouse cursor drawer if enabled */
                VGA_ActivateHardwareCursor();
                break;
            case VGA.M_LIN15:
            case VGA.M_LIN16:
                // 15/16 bpp modes double the horizontal values
                width<<=2;
                if ((VGA.vga.crtc.mode_control & 0x8)!=0 || (Dosbox.svgaCard == SVGACards.SVGA_S3Trio && (VGA.vga.s3.pll.cmd & 0x10)!=0))
                    doublewidth = true;
                /* Use HW mouse cursor drawer if enabled */
                VGA_ActivateHardwareCursor();
                break;
            case VGA.M_LIN4:
                doublewidth=(VGA.vga.seq.clocking_mode & 0x8) > 0;
                VGA.vga.draw.blocks = width;
                width<<=3;
                VGA_DrawLine=VGA_Draw_Linear_Line;
                VGA.vga.draw.linear_base = VGA.vga.fastmem;
                VGA.vga.draw.linear_mask = (VGA.vga.vmemwrap<<1) - 1;
                break;
            case VGA.M_EGA:
                doublewidth=(VGA.vga.seq.clocking_mode & 0x8) > 0;
                VGA.vga.draw.blocks = width;
                width<<=3;
                if ((Dosbox.IS_VGA_ARCH()) && (Dosbox.svgaCard==SVGACards.SVGA_None)) {
                    bpp=16;
                    VGA_DrawLine = VGA_Draw_Xlat16_Linear_Line;
                } else VGA_DrawLine=VGA_Draw_Linear_Line;

                VGA.vga.draw.linear_base = VGA.vga.fastmem;
                VGA.vga.draw.linear_mask = (VGA.vga.vmemwrap<<1) - 1;
                break;
            case VGA.M_CGA16:
                doubleheight=true;
                VGA.vga.draw.blocks=width*2;
                width<<=4;
                VGA_DrawLine=VGA_Draw_CGA16_Line;
                break;
            case VGA.M_CGA4:
                doublewidth=true;
                VGA.vga.draw.blocks=width*2;
                width<<=3;
                VGA_DrawLine=VGA_Draw_2BPP_Line;
                break;
            case VGA.M_CGA2:
                doubleheight=true;
                VGA.vga.draw.blocks=2*width;
                width<<=3;
                VGA_DrawLine=VGA_Draw_1BPP_Line;
                break;
            case VGA.M_TEXT:
                aspect_ratio=1.0;
                VGA.vga.draw.blocks=width;
                doublewidth=(VGA.vga.seq.clocking_mode & 0x8) > 0;
                if ((Dosbox.IS_VGA_ARCH()) && (Dosbox.svgaCard==SVGACards.SVGA_None) && (VGA.vga.seq.clocking_mode&0x01)==0) {
                    width*=9;				/* 9 bit wide text font */
                    VGA_DrawLine=VGA_TEXT_Xlat16_Draw_Line_9;
                    bpp=16;
        //			VGA_DrawLine=VGA_TEXT_Draw_Line_9;
                } else {
                    width<<=3;				/* 8 bit wide text font */
                    if ((Dosbox.IS_VGA_ARCH()) && (Dosbox.svgaCard==SVGACards.SVGA_None)) {
                        VGA_DrawLine=VGA_TEXT_Xlat16_Draw_Line;
                        bpp=16;
                    } else VGA_DrawLine=VGA_TEXT_Draw_Line;
                }
                break;
            case VGA.M_HERC_GFX:
                aspect_ratio=1.5;
                VGA.vga.draw.blocks=width*2;
                width*=16;
                VGA_DrawLine=VGA_Draw_1BPP_Line;
                break;
            case VGA.M_TANDY2:
                aspect_ratio=1.2;
                doubleheight=true;
                if (Dosbox.machine==MachineType.MCH_PCJR) doublewidth=(VGA.vga.tandy.gfx_control & 0x8)==0x00;
                else doublewidth=(VGA.vga.tandy.mode_control & 0x10)==0;
                VGA.vga.draw.blocks=width * (doublewidth ? 4:8);
                width=VGA.vga.draw.blocks*2;
                VGA_DrawLine=VGA_Draw_1BPP_Line;
                break;
            case VGA.M_TANDY4:
                aspect_ratio=1.2;
                doubleheight=true;
                if (Dosbox.machine==MachineType.MCH_TANDY) doublewidth=(VGA.vga.tandy.mode_control & 0x10)==0;
                else doublewidth=(VGA.vga.tandy.mode_control & 0x01)==0x00;
                VGA.vga.draw.blocks=width * 2;
                width=VGA.vga.draw.blocks*4;
                if ((Dosbox.machine==MachineType.MCH_TANDY && (VGA.vga.tandy.gfx_control & 0x8)!=0) ||
                    (Dosbox.machine==MachineType.MCH_PCJR && (VGA.vga.tandy.mode_control==0x0b)))
                    VGA_DrawLine=VGA_Draw_2BPPHiRes_Line;
                else VGA_DrawLine=VGA_Draw_2BPP_Line;
                break;
            case VGA.M_TANDY16:
                aspect_ratio=1.2;
                doubleheight=true;
                VGA.vga.draw.blocks=width*2;
                if ((VGA.vga.tandy.mode_control & 0x1)!=0) {
                    if (( Dosbox.machine==MachineType.MCH_TANDY ) && ( VGA.vga.tandy.mode_control & 0x10 )!=0) {
                        doublewidth = false;
                        VGA.vga.draw.blocks*=2;
                        width=VGA.vga.draw.blocks*2;
                    } else {
                        doublewidth = true;
                        width=VGA.vga.draw.blocks*2;
                    }
                    VGA_DrawLine=VGA_Draw_4BPP_Line;
                } else {
                    doublewidth=true;
                    width=VGA.vga.draw.blocks*4;
                    VGA_DrawLine=VGA_Draw_4BPP_Line_Double;
                }
                break;
            case VGA.M_TANDY_TEXT:
                doublewidth=(VGA.vga.tandy.mode_control & 0x1)==0;
                aspect_ratio=1;
                doubleheight=true;
                VGA.vga.draw.blocks=width;
                width<<=3;
                VGA_DrawLine=VGA_TEXT_Draw_Line;
                break;
            case VGA.M_HERC_TEXT:
                aspect_ratio=1;
                VGA.vga.draw.blocks=width;
                width<<=3;
                VGA_DrawLine=VGA_TEXT_Herc_Draw_Line;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_ERROR,"Unhandled VGA mode "+VGA.vga.mode+" while checking for resolution");
                break;
            }
            VGA_CheckScanLength();
            if (VGA.vga.draw.double_scan) {
                if (Dosbox.IS_VGA_ARCH()) {
                    VGA.vga.draw.vblank_skip /= 2;
                    height/=2;
                }
                doubleheight=true;
            }
            VGA.vga.draw.vblank_skip = vblank_skip;

            if(!(Dosbox.IS_VGA_ARCH() && (Dosbox.svgaCard==SVGACards.SVGA_None) && (VGA.vga.mode==VGA.M_EGA || VGA.vga.mode==VGA.M_VGA))) {
                //Only check for extra double height in vga modes
                //(line multiplying by address_line_total)
                if (!doubleheight && (VGA.vga.mode<VGA.M_TEXT) && (VGA.vga.draw.address_line_total & 1)==0) {
                    VGA.vga.draw.address_line_total/=2;
                    doubleheight=true;
                    height/=2;
                }
            }
            VGA.vga.draw.lines_total=height;
            VGA.vga.draw.parts_lines=VGA.vga.draw.lines_total/VGA.vga.draw.parts_total;
            VGA.vga.draw.line_length = width * ((bpp + 1) / 8);
            if (VGA_memory.VGA_KEEP_CHANGES) {
                VGA.vga.changes.active = false;
                VGA.vga.changes.frame = 0;
                VGA.vga.changes.writeMask = 1;
            }
            /*
               Cheap hack to just make all > 640x480 modes have 4:3 aspect ratio
            */
            if ( width >= 640 && height >= 480 ) {
                aspect_ratio = ((float)width / (float)height) * ( 3.0 / 4.0);
            }
        //	LOG_MSG("ht %d vt %d ratio %f", htotal, vtotal, aspect_ratio );

            // need to change the vertical timing?
            if (Math.abs(VGA.vga.draw.delay.vtotal - 1000.0 / fps) > 0.0001) {
                VGA.vga.draw.delay.vtotal = 1000.0 / fps;
                VGA_KillDrawing();
                Pic.PIC_RemoveEvents(VGA_Other_VertInterrupt);
                Pic.PIC_RemoveEvents(VGA_VerticalTimer);
                Pic.PIC_RemoveEvents(VGA_PanningLatch);
                Pic.PIC_RemoveEvents(VGA_DisplayStartLatch);
                VGA_VerticalTimer.call(0);
            }

            if (Config.C_DEBUG) {
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_NORMAL, StringHelper.sprintf("h total %2.5f (%3.2fkHz) blank(%02.5f/%02.5f) retrace(%02.5f/%02.5f)",
                    new Object[] {new Float(VGA.vga.draw.delay.htotal),new Float(1.0/VGA.vga.draw.delay.htotal),
                    new Float(VGA.vga.draw.delay.hblkstart),new Float(VGA.vga.draw.delay.hblkend),
                    new Float(VGA.vga.draw.delay.hrstart),new Float(VGA.vga.draw.delay.hrend)}));
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_NORMAL, StringHelper.sprintf("v total %2.5f (%3.2fHz) blank(%02.5f/%02.5f) retrace(%02.5f/%02.5f)",
                    new Object[] {new Float(VGA.vga.draw.delay.vtotal),new Float((1000.0/VGA.vga.draw.delay.vtotal)),
                    new Float(VGA.vga.draw.delay.vblkstart),new Float(VGA.vga.draw.delay.vblkend),
                    new Float(VGA.vga.draw.delay.vrstart),new Float(VGA.vga.draw.delay.vrend)}));
            }

            // need to resize the output window?
            if ((width != VGA.vga.draw.width) ||
                (height != VGA.vga.draw.height) ||
                (VGA.vga.draw.doublewidth != doublewidth) ||
                (VGA.vga.draw.doubleheight != doubleheight) ||
                (Math.abs(aspect_ratio - VGA.vga.draw.aspect_ratio) > 0.0001) ||
                (VGA.vga.draw.bpp != bpp)) {

                VGA_KillDrawing();

                VGA.vga.draw.width = width;
                VGA.vga.draw.height = height;
                VGA.vga.draw.doublewidth = doublewidth;
                VGA.vga.draw.doubleheight = doubleheight;
                VGA.vga.draw.aspect_ratio = aspect_ratio;
                VGA.vga.draw.bpp = bpp;
                if (doubleheight) VGA.vga.draw.lines_scaled=2;
                else VGA.vga.draw.lines_scaled=1;
                if (Config.C_DEBUG) {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_NORMAL,"Width "+width+", Height "+height+", fps "+fps);
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGA,LogSeverities.LOG_NORMAL,(doublewidth ? "double":"normal")+" width, "+(doubleheight ? "double":"normal")+" height aspect "+aspect_ratio);
                }
                Render.RENDER_SetSize(width,height,bpp,(float)fps,aspect_ratio,doublewidth,doubleheight);
            }
        }
    };

    private static void VGA_KillDrawing() {
        Pic.PIC_RemoveEvents(VGA_DrawPart);
        Pic.PIC_RemoveEvents(VGA_DrawSingleLine);
        VGA.vga.draw.parts_left = 0;
        VGA.vga.draw.lines_done = ~0;
        Render.RENDER_EndUpdate(true);
    }
    
}
